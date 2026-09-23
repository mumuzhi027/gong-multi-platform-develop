package main

import (
	"cached_proxy/gongai"
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"os"
	"path/filepath"
	"strconv"
	"strings"
	"sync"
	"time"
)

type gongCampusTools struct{}

func toolFailure(code gongai.ErrorCode, message string, retryable bool) gongai.ToolResult {
	return gongai.ToolResult{OK: false, Code: code, UserMessage: message, Retryable: retryable}
}

func (gongCampusTools) ExecuteRead(_ context.Context, currentUser, capability string, args map[string]any) gongai.ToolResult {
	switch capability {
	case "academic.schedule.query":
		data, err := StudentCourseService.GetInfo(currentUser)
		if err != nil || data == nil {
			return toolFailure(gongai.CodeUpstreamTimeout, "课表正在从教务系统更新，请稍后再试。", true)
		}
		items := make([]gongai.CardItem, 0, len(data.Courses))
		for _, course := range data.Courses {
			items = append(items, gongai.CardItem{
				Title: course.Name, Subtitle: course.Classroom,
				Meta: map[string]string{"教师": course.Teacher, "周次": course.Weeks, "星期": course.Day, "节次": fmt.Sprintf("第%d节起，共%d节", course.StartTime, course.Duration)},
			})
		}
		return gongai.ToolResult{OK: true, Code: gongai.CodeOK, ProviderMeta: gongai.ProviderMeta{Provider: "xtu_academic"}, Blocks: []gongai.ChatBlock{{Type: "schedule_card", Title: "我的课表", Items: items}}}

	case "academic.grade.query":
		data, err := StudentMajorScoreService.GetInfo(currentUser)
		if err != nil || data == nil {
			return toolFailure(gongai.CodeUpstreamTimeout, "成绩正在从教务系统更新，请稍后再试。", true)
		}
		failedOnly, _ := args["failed_only"].(bool)
		items := make([]gongai.CardItem, 0, len(data.Scores))
		failedCount := 0
		for _, score := range data.Scores {
			failed := isFailedScore(score.Score)
			if failed {
				failedCount++
			}
			if failedOnly && !failed {
				continue
			}
			items = append(items, gongai.CardItem{Title: score.Name, Subtitle: score.Score, Meta: map[string]string{"学分": score.Credit, "类别": score.Type}})
		}
		title := "成绩概览"
		if failedOnly {
			title = fmt.Sprintf("不及格课程：%d 门", failedCount)
		}
		return gongai.ToolResult{OK: true, Code: gongai.CodeOK, ProviderMeta: gongai.ProviderMeta{Provider: "xtu_academic"}, Blocks: []gongai.ChatBlock{{Type: "grade_card", Title: title, Items: items}}}

	case "academic.exam.query":
		data, err := StudentExamService.GetInfo(currentUser)
		if err != nil || data == nil {
			return toolFailure(gongai.CodeUpstreamTimeout, "考试安排正在从教务系统更新，请稍后再试。", true)
		}
		items := make([]gongai.CardItem, 0, len(data.Exams))
		for _, exam := range data.Exams {
			items = append(items, gongai.CardItem{Title: exam.Name, Subtitle: exam.Location, Meta: map[string]string{"开始": exam.StartTime, "结束": exam.EndTime, "类型": exam.Type}})
		}
		return gongai.ToolResult{OK: true, Code: gongai.CodeOK, ProviderMeta: gongai.ProviderMeta{Provider: "xtu_academic"}, Blocks: []gongai.ChatBlock{{Type: "exam_card", Title: "考试安排", Items: items}}}

	case "room.search":
		if mode, _ := args["mode"].(string); mode != "empty_classroom" {
			return toolFailure(gongai.CodeNotSupported, "场地预约的上游字段仍需在服务器完成一次真实页面校验；为避免错误预约，本版本暂不执行该查询。", false)
		}
		data, err := TodayClassroomService.GetInfo(currentUser)
		if err != nil || data == nil {
			return toolFailure(gongai.CodeUpstreamTimeout, "空教室数据正在更新，请稍后再试。", true)
		}
		items := make([]gongai.CardItem, 0)
		for building, rooms := range data.Classrooms {
			for _, room := range rooms {
				items = append(items, gongai.CardItem{Title: room.Name, Subtitle: building, Meta: map[string]string{"占用情况": strings.Join(room.Status, "、")}})
				if len(items) >= 40 {
					break
				}
			}
			if len(items) >= 40 {
				break
			}
		}
		return gongai.ToolResult{OK: true, Code: gongai.CodeOK, ProviderMeta: gongai.ProviderMeta{Provider: "xtu_academic"}, Blocks: []gongai.ChatBlock{{Type: "room_candidate_card", Title: "今日空教室", Items: items}}}

	case "leave.status":
		return toolFailure(gongai.CodeProviderSessionExpired, "课外培养平台的独立登录态尚未建立，请先在部署端完成 Session Vault 的 SSO 接入配置。", false)
	default:
		return toolFailure(gongai.CodeNotSupported, "当前能力不在拱拱AI Beta 范围内。", false)
	}
}

func isFailedScore(value string) bool {
	value = strings.TrimSpace(value)
	if strings.Contains(value, "不及格") || strings.Contains(value, "不通过") {
		return true
	}
	number, err := strconv.ParseFloat(value, 64)
	return err == nil && number < 60
}

func (gongCampusTools) ExecuteWrite(_ context.Context, _ string, capability string, _ map[string]any) gongai.ToolResult {
	switch capability {
	case "leave.apply", "leave.withdraw":
		return toolFailure(gongai.CodeProviderSessionExpired, "未建立当前用户的课外培养平台会话，因此没有向上游提交任何内容。", false)
	case "room.reserve":
		return toolFailure(gongai.CodeNotSupported, "场地预约提交参数尚未经过真实上游校验，因此没有执行提交。", false)
	default:
		return toolFailure(gongai.CodeNotSupported, "该写操作不在 Beta 范围内。", false)
	}
}

type userRate struct {
	window time.Time
	count  int
}

type gongAIHandler struct {
	TokenService
	orchestrator *gongai.Orchestrator
	mu           sync.Mutex
	rates        map[string]userRate
}

func newGongAIHandler() *gongAIHandler {
	_ = os.MkdirAll(ActivityDataDir, 0700)
	auditFile, err := os.OpenFile(filepath.Join(ActivityDataDir, "gong-ai-audit.jsonl"), os.O_CREATE|os.O_APPEND|os.O_WRONLY, 0600)
	var audit *gongai.AuditWriter
	if err == nil {
		audit = gongai.NewAuditWriter(auditFile)
	}
	return &gongAIHandler{
		orchestrator: gongai.NewOrchestrator(gongai.NewDeepSeekGatewayFromEnv(), gongCampusTools{}, audit),
		rates:        make(map[string]userRate),
	}
}

func (h *gongAIHandler) allow(userID string) bool {
	h.mu.Lock()
	defer h.mu.Unlock()
	now := time.Now()
	entry := h.rates[userID]
	if entry.window.IsZero() || now.Sub(entry.window) >= time.Minute {
		entry = userRate{window: now}
	}
	if entry.count >= 30 {
		h.rates[userID] = entry
		return false
	}
	entry.count++
	h.rates[userID] = entry
	return true
}

func writeAIResponse(w http.ResponseWriter, status int, code gongai.ErrorCode, data any, message string) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(map[string]any{"code": code, "message": message, "data": data})
}

func (h *gongAIHandler) currentUser(w http.ResponseWriter, r *http.Request) (string, bool) {
	account := h.checkToken(w, r)
	if account == nil {
		return "", false
	}
	userID := account.AccountID()
	if !h.allow(userID) {
		writeAIResponse(w, http.StatusTooManyRequests, gongai.CodeRateLimited, nil, "请求过于频繁，请稍后再试")
		return "", false
	}
	return userID, true
}

func (h *gongAIHandler) chat(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
		return
	}
	userID, ok := h.currentUser(w, r)
	if !ok {
		return
	}
	r.Body = http.MaxBytesReader(w, r.Body, 64<<10)
	decoder := json.NewDecoder(r.Body)
	decoder.DisallowUnknownFields()
	var request gongai.ChatRequest
	if err := decoder.Decode(&request); err != nil {
		writeAIResponse(w, http.StatusBadRequest, gongai.CodeValidationFailed, nil, "请求格式不正确")
		return
	}
	response, code, err := h.orchestrator.Chat(r.Context(), userID, request)
	if err != nil {
		writeAIResponse(w, http.StatusBadRequest, code, nil, err.Error())
		return
	}
	writeAIResponse(w, http.StatusOK, code, response, "success")
}

func (h *gongAIHandler) action(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
		return
	}
	userID, ok := h.currentUser(w, r)
	if !ok {
		return
	}
	path := strings.TrimPrefix(r.URL.Path, "/api/gong-ai/actions/")
	parts := strings.Split(strings.Trim(path, "/"), "/")
	if len(parts) != 2 || parts[0] == "" {
		writeAIResponse(w, http.StatusNotFound, gongai.CodeValidationFailed, nil, "操作不存在")
		return
	}
	switch parts[1] {
	case "confirm":
		response, code, err := h.orchestrator.Confirm(r.Context(), userID, parts[0])
		if err != nil {
			writeAIResponse(w, http.StatusConflict, code, nil, "确认失败或操作已处理")
			return
		}
		writeAIResponse(w, http.StatusOK, code, response, "success")
	case "cancel":
		code := h.orchestrator.Cancel(userID, parts[0])
		status := http.StatusOK
		if code != gongai.CodeOK {
			status = http.StatusConflict
		}
		writeAIResponse(w, status, code, map[string]bool{"cancelled": code == gongai.CodeOK}, "success")
	default:
		writeAIResponse(w, http.StatusNotFound, gongai.CodeValidationFailed, nil, "操作不存在")
	}
}

func (h *gongAIHandler) conversation(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodDelete {
		http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
		return
	}
	userID, ok := h.currentUser(w, r)
	if !ok {
		return
	}
	id := strings.Trim(strings.TrimPrefix(r.URL.Path, "/api/gong-ai/conversations/"), "/")
	deleted := h.orchestrator.ClearConversation(userID, id)
	if !deleted {
		writeAIResponse(w, http.StatusNotFound, gongai.CodePermissionDenied, nil, "对话不存在")
		return
	}
	writeAIResponse(w, http.StatusOK, gongai.CodeOK, map[string]bool{"deleted": true}, "success")
}

func RegisterGongAIRoutes(mux *http.ServeMux) {
	handler := newGongAIHandler()
	onlyWhenEnabled := func(next http.HandlerFunc) http.HandlerFunc {
		return func(w http.ResponseWriter, r *http.Request) {
			if !readPublicFeatures().GongAIVisible { http.Error(w, "拱拱AI暂未开放", http.StatusServiceUnavailable); return }
			next(w, r)
		}
	}
	mux.HandleFunc("/api/gong-ai/chat", onlyWhenEnabled(handler.chat))
	mux.HandleFunc("/api/gong-ai/actions/", onlyWhenEnabled(handler.action))
	mux.HandleFunc("/api/gong-ai/conversations/", onlyWhenEnabled(handler.conversation))
}

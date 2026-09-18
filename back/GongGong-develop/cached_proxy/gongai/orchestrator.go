package gongai

import (
	"context"
	"errors"
	"fmt"
	"math"
	"regexp"
	"strconv"
	"strings"
	"time"
)

var (
	mainlandPhonePattern = regexp.MustCompile(`^1[3-9][0-9]{9}$`)
	phoneInTextPattern   = regexp.MustCompile(`(?:^|[^0-9])(1[3-9][0-9]{9})(?:$|[^0-9])`)
)

type CampusTools interface {
	ExecuteRead(ctx context.Context, currentUser, capability string, args map[string]any) ToolResult
	ExecuteWrite(ctx context.Context, currentUser, capability string, args map[string]any) ToolResult
}

type Orchestrator struct {
	registry      *Registry
	model         ModelGateway
	tools         CampusTools
	actions       *PendingActionStore
	drafts        *DraftStore
	conversations *ConversationStore
	privacy       PrivacyGateway
	audit         *AuditWriter
	attachments   *AttachmentStore
}

func NewOrchestrator(model ModelGateway, tools CampusTools, audit *AuditWriter) *Orchestrator {
	return &Orchestrator{
		registry: NewRegistry(), model: model, tools: tools, actions: NewPendingActionStore(), drafts: NewDraftStore(),
		conversations: NewConversationStore(), privacy: PrivacyGateway{}, audit: audit, attachments: NewAttachmentStore(),
	}
}

func fallbackDecision(message string) AgentDecision {
	text := strings.ToLower(strings.TrimSpace(message))
	switch {
	case strings.Contains(text, "请假") && (strings.Contains(text, "状态") || strings.Contains(text, "进度") || strings.Contains(text, "审批")):
		return AgentDecision{Type: "tool", Capability: "leave.status", Args: map[string]any{}}
	case strings.Contains(text, "请假"):
		return AgentDecision{Type: "tool", Capability: "leave.apply", Args: map[string]any{"raw_request": message}}
	case strings.Contains(text, "预约") && (strings.Contains(text, "教室") || strings.Contains(text, "场地")):
		return AgentDecision{Type: "tool", Capability: "room.search", Args: map[string]any{"raw_request": message}}
	case strings.Contains(text, "空教室"):
		return AgentDecision{Type: "tool", Capability: "room.search", Args: map[string]any{"mode": "empty_classroom", "raw_request": message}}
	case strings.Contains(text, "成绩") || strings.Contains(text, "挂科") || strings.Contains(text, "绩点") || strings.Contains(text, "gpa") || strings.Contains(text, "排名"):
		return AgentDecision{Type: "tool", Capability: "academic.grade.query", Args: map[string]any{"failed_only": strings.Contains(text, "挂科")}}
	case strings.Contains(text, "考试"):
		return AgentDecision{Type: "tool", Capability: "academic.exam.query", Args: map[string]any{"raw_request": message}}
	case strings.Contains(text, "课") || strings.Contains(text, "课表"):
		return AgentDecision{Type: "tool", Capability: "academic.schedule.query", Args: map[string]any{"raw_request": message}}
	default:
		return AgentDecision{Type: "reply", Reply: "我目前可以查询课表、成绩、考试和空教室，也可以协助准备请假或场地预约。你可以直接说想办什么。"}
	}
}

func (o *Orchestrator) Chat(ctx context.Context, currentUser string, request ChatRequest) (ChatResponse, ErrorCode, error) {
	if strings.TrimSpace(currentUser) == "" {
		return ChatResponse{}, CodeAuthRequired, errors.New("current user is required")
	}
	message := o.privacy.SanitizeText(request.Message)
	if message == "" {
		return ChatResponse{}, CodeValidationFailed, errors.New("message is empty")
	}
	conversationID, err := o.conversations.Resolve(request.ConversationID, currentUser)
	if err != nil {
		return ChatResponse{}, CodePermissionDenied, err
	}
	serverArgs := map[string]any{}
	if match := phoneInTextPattern.FindStringSubmatch(message); len(match) == 2 {
		serverArgs["phone"] = match[1]
		message = strings.Replace(message, match[1], "[已提供手机号]", 1)
	}
	// Any new message after a preview is a modification or a new intent. The old
	// immutable preview must no longer be confirmable.
	o.actions.InvalidateConversation(currentUser, conversationID)
	input := ModelInput{Message: message, Capabilities: o.registry.PublicModelCatalog(), Context: o.conversations.History(conversationID, currentUser)}
	decision, modelErr := o.model.Decide(ctx, input)
	if modelErr != nil {
		decision = fallbackDecision(message)
	}
	if decision.Type == "reply" {
		text := o.privacy.SanitizeText(decision.Reply)
		if text == "" {
			fallback := fallbackDecision(message)
			if fallback.Type == "reply" {
				text = fallback.Reply
			} else {
				text = "我没能可靠理解这次请求，请换一种更具体的说法再试。"
			}
		}
		response := ChatResponse{ConversationID: conversationID, WorkflowState: "IDLE", Blocks: []ChatBlock{{Type: "text", Text: text}}}
		o.conversations.Append(conversationID, currentUser, ConversationMessage{"user", message}, ConversationMessage{"assistant", text})
		return response, CodeOK, nil
	}
	capability, ok := o.registry.Get(decision.Capability)
	if !ok {
		return ChatResponse{}, CodeValidationFailed, errors.New("model selected an unknown capability")
	}
	if decision.Args == nil {
		decision.Args = make(map[string]any)
	}
	for name, value := range serverArgs {
		decision.Args[name] = value
	}
	if capability.ReadOnly {
		result := o.tools.ExecuteRead(ctx, currentUser, capability.ID, decision.Args)
		if !result.OK {
			block := ChatBlock{Type: "warning", Text: result.UserMessage}
			if block.Text == "" {
				block.Text = "暂时无法完成这个查询，请稍后重试。"
			}
			return ChatResponse{ConversationID: conversationID, WorkflowState: "FAILED", Blocks: []ChatBlock{block}}, result.Code, nil
		}
		response := ChatResponse{ConversationID: conversationID, WorkflowState: "SUCCEEDED", Blocks: result.Blocks}
		o.conversations.Append(conversationID, currentUser, ConversationMessage{"user", message}, ConversationMessage{"assistant", summarizeBlocks(result.Blocks)})
		return response, CodeOK, nil
	}
	for _, attachment := range request.Attachments {
		stored, attachmentErr := o.attachments.Get(currentUser, attachment.ID)
		if attachmentErr != nil || stored.Purpose != attachment.Purpose || stored.MediaType != attachment.MediaType {
			return ChatResponse{}, CodePermissionDenied, errors.New("attachment does not belong to current user")
		}
	}
	draft := o.drafts.Merge(currentUser, conversationID, capability.ID, decision.Args, request.Attachments)
	params, preview, missing := buildWritePreview(capability.ID, draft.Params, draft.Attachments)
	if len(missing) > 0 {
		text := "还需要补充：" + strings.Join(missing, "、")
		o.conversations.Append(conversationID, currentUser, ConversationMessage{"user", message}, ConversationMessage{"assistant", text})
		return ChatResponse{ConversationID: conversationID, WorkflowState: "COLLECTING", Blocks: []ChatBlock{{Type: "text", Text: text}}}, CodeMissingRequiredField, nil
	}
	o.drafts.Delete(currentUser, conversationID)
	action, err := o.actions.Create(currentUser, conversationID, capability, params, preview)
	if err != nil {
		return ChatResponse{}, CodeInternalError, err
	}
	o.audit.Write(currentUser, action.ActionID, capability.ID, capability.Provider, "PREVIEW_CREATED")
	block := ChatBlock{Type: "action_preview", Title: preview.Title, Fields: preview.Fields, Warnings: preview.Warnings,
		ActionID: action.ActionID, ConfirmText: preview.ConfirmText, CancelText: preview.CancelText, ExpiresAt: action.ExpiresAt.Format(time.RFC3339)}
	return ChatResponse{ConversationID: conversationID, WorkflowState: "AWAITING_CONFIRMATION", Blocks: []ChatBlock{block}}, CodeConfirmationRequired, nil
}

// RegisterAttachment is intentionally unavailable to the model. A provider
// adapter calls it only after an authenticated, owner-bound upload succeeds.
func (o *Orchestrator) RegisterAttachment(currentUser, purpose, mediaType, provider string, encryptedReference []byte) (AttachmentRef, error) {
	item, err := o.attachments.Put(currentUser, purpose, mediaType, provider, encryptedReference)
	if err != nil {
		return AttachmentRef{}, err
	}
	return AttachmentRef{ID: item.ID, Purpose: purpose, MediaType: mediaType}, nil
}

func summarizeBlocks(blocks []ChatBlock) string {
	if len(blocks) == 0 {
		return "查询完成"
	}
	if blocks[0].Title != "" {
		return blocks[0].Title
	}
	return blocks[0].Text
}

func buildWritePreview(capability string, args map[string]any, attachments []AttachmentRef) (map[string]any, ActionPreview, []string) {
	if args == nil {
		args = map[string]any{}
	}
	params := make(map[string]any, len(args)+1)
	for key, value := range args {
		params[key] = value
	}
	params["attachment_refs"] = attachments
	preview := ActionPreview{ConfirmText: "确认提交", CancelText: "取消"}
	missing := []string{}
	require := func(key, label string) string {
		value := strings.TrimSpace(fmt.Sprint(params[key]))
		if value == "" || value == "<nil>" {
			missing = append(missing, label)
			return ""
		}
		return value
	}
	switch capability {
	case "leave.apply":
		preview.Title = "请假申请"
		for _, item := range []struct{ key, label string }{{"reason", "请假原因"}, {"start_time", "开始时间"}, {"end_time", "结束时间"}, {"leave_days", "请假天数"}, {"leave_class_hours", "请假课时"}, {"leave_campus", "是否离校"}, {"phone", "本人电话"}} {
			value := require(item.key, item.label)
			if value != "" {
				preview.Fields = append(preview.Fields, PreviewField{Label: item.label, Value: map[string]string{"本人电话": MaskPhone(value)}[item.label], Sensitive: item.label == "本人电话"})
				if item.label != "本人电话" {
					preview.Fields[len(preview.Fields)-1].Value = value
				}
			}
		}
		if fmt.Sprint(params["leave_campus"]) == "true" {
			require("destination", "离校地点")
		}
		if value := strings.TrimSpace(fmt.Sprint(params["phone"])); value != "" && value != "<nil>" && !mainlandPhonePattern.MatchString(value) {
			missing = append(missing, "有效的本人电话")
		}
		if value, err := strconv.ParseFloat(strings.TrimSpace(fmt.Sprint(params["leave_days"])), 64); err != nil || value < 0.5 || value > 30 || math.Mod(value*2, 1) != 0 {
			missing = append(missing, "0.5 至 30 天且按 0.5 递增的请假天数")
		}
		if value, err := strconv.Atoi(strings.TrimSpace(fmt.Sprint(params["leave_class_hours"]))); err != nil || value < 1 || value > 200 {
			missing = append(missing, "1 至 200 的整数请假课时")
		}
		if len(attachments) == 0 {
			preview.Warnings = append(preview.Warnings, "当前未附班主任沟通截图。系统可能允许提交，但按实际办理经验，缺少该材料可能导致申请被驳回。")
		}
	case "room.reserve":
		preview.Title = "场地预约"
		for _, item := range []struct{ key, label string }{{"room_id", "场地"}, {"start_time", "开始时间"}, {"end_time", "结束时间"}, {"reason", "借用理由"}, {"phone", "联系电话"}, {"attendees", "使用人数"}} {
			value := require(item.key, item.label)
			if value != "" {
				if item.label == "联系电话" {
					value = MaskPhone(value)
				}
				preview.Fields = append(preview.Fields, PreviewField{Label: item.label, Value: value, Sensitive: item.label == "联系电话"})
			}
		}
		if value := strings.TrimSpace(fmt.Sprint(params["phone"])); value != "" && value != "<nil>" && !mainlandPhonePattern.MatchString(value) {
			missing = append(missing, "有效的联系电话")
		}
		if value, err := strconv.Atoi(strings.TrimSpace(fmt.Sprint(params["attendees"]))); err != nil || value < 1 {
			missing = append(missing, "大于 0 的使用人数")
		}
	default:
		missing = append(missing, "当前操作不在 Beta 范围")
	}
	return params, preview, missing
}

func (o *Orchestrator) Confirm(ctx context.Context, currentUser, actionID string) (ConfirmResponse, ErrorCode, error) {
	action, ready, code, err := o.actions.Confirm(actionID, currentUser)
	if err != nil {
		return ConfirmResponse{}, code, err
	}
	o.audit.Write(currentUser, actionID, action.Capability, action.Provider, "USER_CONFIRMED")
	if !ready {
		return ConfirmResponse{Block: ChatBlock{
			Type: "action_preview", Title: "请再次确认", Text: "你没有上传班主任沟通截图。继续提交可能被驳回；请再次点击确认后才会真正提交。",
			Fields: action.Preview.Fields, Warnings: action.Preview.Warnings, ActionID: actionID,
			ConfirmText: "仍然提交", CancelText: "取消", ExpiresAt: action.ExpiresAt.Format(time.RFC3339), Status: ActionAwaitingConfirmation,
		}}, CodeConfirmationRequired, nil
	}
	o.audit.Write(currentUser, actionID, action.Capability, action.Provider, "EXECUTE_STARTED")
	result := o.tools.ExecuteWrite(ctx, currentUser, action.Capability, action.Params)
	o.audit.Write(currentUser, actionID, action.Capability, action.Provider, "EXECUTE_FINISHED")
	if !result.OK {
		o.actions.SetStatus(actionID, ActionFailed)
		return ConfirmResponse{Block: ChatBlock{Type: "warning", Text: result.UserMessage, ActionID: actionID, Status: "FAILED"}}, result.Code, nil
	}
	o.actions.SetStatus(actionID, ActionVerifying)
	// ExecuteWrite is contractually required to return OK only after provider re-query verification.
	o.actions.SetStatus(actionID, ActionSucceeded)
	o.audit.Write(currentUser, actionID, action.Capability, action.Provider, "VERIFY_SUCCEEDED")
	summary := result.UserMessage
	if summary == "" {
		summary = "操作已提交并完成上游状态核验。"
	}
	return ConfirmResponse{Block: ChatBlock{Type: "action_receipt", Title: "办理完成", Text: summary, ActionID: actionID, Status: "SUCCESS"}}, CodeOK, nil
}

func (o *Orchestrator) Cancel(currentUser, actionID string) ErrorCode {
	return o.actions.Cancel(actionID, currentUser)
}
func (o *Orchestrator) ClearConversation(currentUser, conversationID string) bool {
	deleted := o.conversations.Delete(conversationID, currentUser)
	if deleted {
		o.drafts.Delete(currentUser, conversationID)
	}
	return deleted
}

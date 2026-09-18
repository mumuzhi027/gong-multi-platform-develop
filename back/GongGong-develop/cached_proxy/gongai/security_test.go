package gongai

import (
	"context"
	"io"
	"strings"
	"testing"
	"time"
)

type testModel struct{ decision AgentDecision }

func (m testModel) Decide(context.Context, ModelInput) (AgentDecision, error) { return m.decision, nil }

type captureModel struct{ input ModelInput }

func (m *captureModel) Decide(_ context.Context, input ModelInput) (AgentDecision, error) {
	m.input = input
	return AgentDecision{Type: "reply", Reply: "ok"}, nil
}

type testTools struct{ writes int }

func (t *testTools) ExecuteRead(context.Context, string, string, map[string]any) ToolResult {
	return ToolResult{OK: true, Code: CodeOK, Blocks: []ChatBlock{{Type: "text", Text: "ok"}}}
}

func completeLeaveArgs() map[string]any {
	return map[string]any{
		"reason": "身体不适", "start_time": "2026-09-18 08:00", "end_time": "2026-09-18 18:00",
		"leave_days": "1", "leave_class_hours": "4", "leave_campus": false, "phone": "13800000000",
	}
}
func (t *testTools) ExecuteWrite(context.Context, string, string, map[string]any) ToolResult {
	t.writes++
	return ToolResult{OK: true, Code: CodeOK, UserMessage: "verified"}
}

func TestPendingActionIsOwnerBoundAndSingleUse(t *testing.T) {
	store := NewPendingActionStore()
	capability := Capability{ID: "leave.apply", Provider: "xtu_extracurricular", ReadOnly: false}
	action, err := store.Create("user-a", "conversation-a", capability, map[string]any{"reason": "test"}, ActionPreview{Title: "preview"})
	if err != nil {
		t.Fatal(err)
	}
	if _, code, _ := store.BeginExecution(action.ActionID, "user-b"); code != CodePermissionDenied {
		t.Fatalf("cross-user confirm code=%s", code)
	}
	if _, code, err := store.BeginExecution(action.ActionID, "user-a"); err != nil || code != CodeOK {
		t.Fatalf("owner confirm failed: %v %s", err, code)
	}
	if _, code, _ := store.BeginExecution(action.ActionID, "user-a"); code != CodeUpstreamConflict {
		t.Fatalf("replay code=%s", code)
	}
}

func TestSessionVaultSeparatesUsers(t *testing.T) {
	vault, err := NewSessionVault("test-only-master-key-with-enough-entropy")
	if err != nil {
		t.Fatal(err)
	}
	if err := vault.Put("user-a", ProviderSession{Provider: "extra", AccessToken: "token-a", ExpiresAt: time.Now().Add(time.Hour)}); err != nil {
		t.Fatal(err)
	}
	if _, err := vault.Get("user-b", "extra"); err == nil {
		t.Fatal("user-b accessed user-a session")
	}
	session, err := vault.Get("user-a", "extra")
	if err != nil || session.AccessToken != "token-a" {
		t.Fatalf("owner session mismatch: %#v %v", session, err)
	}
}

func TestAttachmentStoreSeparatesUsers(t *testing.T) {
	store := NewAttachmentStore()
	item, err := store.Put("user-a", "leave_evidence", "image/jpeg", "extra", []byte("encrypted"))
	if err != nil {
		t.Fatal(err)
	}
	if _, err := store.Get("user-b", item.ID); err == nil {
		t.Fatal("user-b accessed user-a attachment")
	}
}

func TestModelInputRejectsCredentials(t *testing.T) {
	err := (PrivacyGateway{}).ValidateModelInput(map[string]string{"ACCESS_TOKEN": "secret"})
	if err == nil {
		t.Fatal("credential-like model input was accepted")
	}
}

func TestPhoneIsRemovedBeforeModel(t *testing.T) {
	model := &captureModel{}
	orchestrator := NewOrchestrator(model, &testTools{}, NewAuditWriter(io.Discard))
	_, _, err := orchestrator.Chat(context.Background(), "user-a", ChatRequest{Message: "我的电话是13800000000"})
	if err != nil {
		t.Fatal(err)
	}
	if strings.Contains(model.input.Message, "13800000000") {
		t.Fatalf("phone reached model: %q", model.input.Message)
	}
}

func TestOrchestratorCannotConfirmAnotherUsersAction(t *testing.T) {
	tools := &testTools{}
	orchestrator := NewOrchestrator(testModel{AgentDecision{Type: "tool", Capability: "academic.schedule.query"}}, tools, NewAuditWriter(io.Discard))
	// The lower-level store test covers pending ownership. This verifies conversation ownership too.
	response, _, err := orchestrator.Chat(context.Background(), "user-a", ChatRequest{Message: "课表"})
	if err != nil {
		t.Fatal(err)
	}
	if _, _, err := orchestrator.Chat(context.Background(), "user-b", ChatRequest{ConversationID: response.ConversationID, Message: "课表"}); err == nil {
		t.Fatal("cross-user conversation access succeeded")
	}
}

func TestWriteDraftMergesAcrossTurns(t *testing.T) {
	tools := &testTools{}
	model := &sequenceModel{decisions: []AgentDecision{
		{Type: "tool", Capability: "leave.apply", Args: map[string]any{"reason": "身体不适"}},
		{Type: "tool", Capability: "leave.apply", Args: map[string]any{"start_time": "2026-09-18 08:00", "end_time": "2026-09-18 18:00", "leave_days": "1", "leave_class_hours": "4", "leave_campus": false, "phone": "13800000000"}},
	}}
	orchestrator := NewOrchestrator(model, tools, NewAuditWriter(io.Discard))
	first, code, err := orchestrator.Chat(context.Background(), "user-a", ChatRequest{Message: "请假，身体不适"})
	if err != nil || code != CodeMissingRequiredField || first.WorkflowState != "COLLECTING" {
		t.Fatalf("first turn failed: %#v %s %v", first, code, err)
	}
	second, code, err := orchestrator.Chat(context.Background(), "user-a", ChatRequest{ConversationID: first.ConversationID, Message: "补充时间和电话"})
	if err != nil || code != CodeConfirmationRequired || second.WorkflowState != "AWAITING_CONFIRMATION" {
		t.Fatalf("second turn failed: %#v %s %v", second, code, err)
	}
}

type sequenceModel struct {
	decisions []AgentDecision
	next      int
}

func (m *sequenceModel) Decide(context.Context, ModelInput) (AgentDecision, error) {
	decision := m.decisions[m.next]
	m.next++
	return decision, nil
}

func TestMissingLeaveEvidenceRequiresTwoConfirmations(t *testing.T) {
	tools := &testTools{}
	orchestrator := NewOrchestrator(testModel{AgentDecision{Type: "tool", Capability: "leave.apply", Args: completeLeaveArgs()}}, tools, NewAuditWriter(io.Discard))
	response, _, err := orchestrator.Chat(context.Background(), "user-a", ChatRequest{Message: "请假"})
	if err != nil || len(response.Blocks) != 1 {
		t.Fatalf("preview failed: %#v %v", response, err)
	}
	actionID := response.Blocks[0].ActionID
	first, code, err := orchestrator.Confirm(context.Background(), "user-a", actionID)
	if err != nil || code != CodeConfirmationRequired || first.Block.ConfirmText != "仍然提交" || tools.writes != 0 {
		t.Fatalf("first confirm should not write: %#v %s %v writes=%d", first, code, err, tools.writes)
	}
	_, code, err = orchestrator.Confirm(context.Background(), "user-a", actionID)
	if err != nil || code != CodeOK || tools.writes != 1 {
		t.Fatalf("second confirm failed: %s %v writes=%d", code, err, tools.writes)
	}
	if _, code, _ = orchestrator.Confirm(context.Background(), "user-a", actionID); code != CodeUpstreamConflict || tools.writes != 1 {
		t.Fatalf("replay was not blocked: %s writes=%d", code, tools.writes)
	}
}

func TestNewMessageInvalidatesOldPreview(t *testing.T) {
	tools := &testTools{}
	orchestrator := NewOrchestrator(testModel{AgentDecision{Type: "tool", Capability: "leave.apply", Args: completeLeaveArgs()}}, tools, NewAuditWriter(io.Discard))
	first, _, err := orchestrator.Chat(context.Background(), "user-a", ChatRequest{Message: "请假"})
	if err != nil {
		t.Fatal(err)
	}
	actionID := first.Blocks[0].ActionID
	_, _, err = orchestrator.Chat(context.Background(), "user-a", ChatRequest{ConversationID: first.ConversationID, Message: "时间改一下"})
	if err != nil {
		t.Fatal(err)
	}
	if _, code, _ := orchestrator.Confirm(context.Background(), "user-a", actionID); code != CodeUpstreamConflict {
		t.Fatalf("old preview remained confirmable: %s", code)
	}
}

func TestForgedAttachmentReferenceIsRejected(t *testing.T) {
	orchestrator := NewOrchestrator(testModel{AgentDecision{Type: "tool", Capability: "leave.apply", Args: completeLeaveArgs()}}, &testTools{}, NewAuditWriter(io.Discard))
	_, code, err := orchestrator.Chat(context.Background(), "user-a", ChatRequest{
		Message: "请假", Attachments: []AttachmentRef{{ID: "forged", Purpose: "leave_evidence", MediaType: "image/jpeg"}},
	})
	if err == nil || code != CodePermissionDenied {
		t.Fatalf("forged attachment accepted: %s %v", code, err)
	}
}

func TestAttachmentReferenceIsOwnerBound(t *testing.T) {
	orchestrator := NewOrchestrator(testModel{AgentDecision{Type: "tool", Capability: "leave.apply", Args: completeLeaveArgs()}}, &testTools{}, NewAuditWriter(io.Discard))
	attachment, err := orchestrator.RegisterAttachment("user-a", "leave_evidence", "image/jpeg", "extra", []byte("encrypted-reference"))
	if err != nil {
		t.Fatal(err)
	}
	_, code, err := orchestrator.Chat(context.Background(), "user-b", ChatRequest{Message: "请假", Attachments: []AttachmentRef{attachment}})
	if err == nil || code != CodePermissionDenied {
		t.Fatalf("cross-user attachment accepted: %s %v", code, err)
	}
}

package gongai

import (
	"crypto/rand"
	"crypto/sha256"
	"encoding/hex"
	"encoding/json"
	"errors"
	"fmt"
	"strings"
	"sync"
	"time"
)

const (
	ActionAwaitingConfirmation = "AWAITING_CONFIRMATION"
	ActionExecuting            = "EXECUTING"
	ActionVerifying            = "VERIFYING"
	ActionSucceeded            = "SUCCEEDED"
	ActionFailed               = "FAILED"
	ActionCancelled            = "CANCELLED"
	ActionExpired              = "EXPIRED"
)

type PendingActionStore struct {
	mu      sync.Mutex
	actions map[string]*PendingAction
}

func NewPendingActionStore() *PendingActionStore {
	return &PendingActionStore{actions: make(map[string]*PendingAction)}
}

func randomID() (string, error) {
	data := make([]byte, 18)
	if _, err := rand.Read(data); err != nil {
		return "", err
	}
	return hex.EncodeToString(data), nil
}

func paramsHash(params map[string]any) (string, error) {
	data, err := json.Marshal(params)
	if err != nil {
		return "", err
	}
	hash := sha256.Sum256(data)
	return hex.EncodeToString(hash[:]), nil
}

func (s *PendingActionStore) Create(userID, conversationID string, capability Capability, params map[string]any, preview ActionPreview) (*PendingAction, error) {
	id, err := randomID()
	if err != nil {
		return nil, err
	}
	hash, err := paramsHash(params)
	if err != nil {
		return nil, err
	}
	now := time.Now().UTC()
	requiredConfirmations := 1
	if len(preview.Warnings) > 0 {
		requiredConfirmations = 2
	}
	action := &PendingAction{
		ActionID: id, UserID: userID, ConversationID: conversationID, Capability: capability.ID, Provider: capability.Provider,
		Params: params, ParamsHash: hash, Preview: preview, Status: ActionAwaitingConfirmation,
		CreatedAt: now, ExpiresAt: now.Add(15 * time.Minute), RequiredConfirmations: requiredConfirmations,
	}
	s.mu.Lock()
	s.actions[id] = action
	s.mu.Unlock()
	copy := *action
	return &copy, nil
}

func (s *PendingActionStore) InvalidateConversation(userID, conversationID string) {
	s.mu.Lock()
	defer s.mu.Unlock()
	for _, action := range s.actions {
		if action.UserID == userID && action.ConversationID == conversationID && action.Status == ActionAwaitingConfirmation {
			action.Status = ActionCancelled
		}
	}
}

func (s *PendingActionStore) Confirm(actionID, userID string) (*PendingAction, bool, ErrorCode, error) {
	s.mu.Lock()
	defer s.mu.Unlock()
	action, ok := s.actions[actionID]
	if !ok || action.UserID != userID {
		return nil, false, CodePermissionDenied, errors.New("action not found for current user")
	}
	if time.Now().UTC().After(action.ExpiresAt) {
		action.Status = ActionExpired
		return nil, false, CodeConfirmationExpired, errors.New("action expired")
	}
	if action.Status != ActionAwaitingConfirmation {
		return nil, false, CodeUpstreamConflict, errors.New("action was already consumed")
	}
	hash, err := paramsHash(action.Params)
	if err != nil || hash != action.ParamsHash {
		return nil, false, CodeConfirmationMismatch, errors.New("action parameters changed")
	}
	action.ConfirmationCount++
	if action.ConfirmationCount < action.RequiredConfirmations {
		copy := *action
		return &copy, false, CodeConfirmationRequired, nil
	}
	action.Status = ActionExecuting
	copy := *action
	return &copy, true, CodeOK, nil
}

func (s *PendingActionStore) BeginExecution(actionID, userID string) (*PendingAction, ErrorCode, error) {
	action, ready, code, err := s.Confirm(actionID, userID)
	if err != nil {
		return nil, code, err
	}
	if !ready {
		return nil, code, errors.New("additional confirmation required")
	}
	return action, code, nil
}

func (s *PendingActionStore) SetStatus(actionID, status string) {
	s.mu.Lock()
	defer s.mu.Unlock()
	if action := s.actions[actionID]; action != nil {
		action.Status = status
	}
}

func (s *PendingActionStore) Cancel(actionID, userID string) ErrorCode {
	s.mu.Lock()
	defer s.mu.Unlock()
	action, ok := s.actions[actionID]
	if !ok || action.UserID != userID {
		return CodePermissionDenied
	}
	if action.Status != ActionAwaitingConfirmation {
		return CodeUpstreamConflict
	}
	action.Status = ActionCancelled
	return CodeOK
}

type ConversationMessage struct {
	Role string
	Text string
}

type conversation struct {
	Owner     string
	Messages  []ConversationMessage
	UpdatedAt time.Time
}

type ConversationStore struct {
	mu    sync.Mutex
	items map[string]*conversation
}

type WriteDraft struct {
	Capability  string
	Params      map[string]any
	Attachments []AttachmentRef
}

type DraftStore struct {
	mu    sync.Mutex
	items map[string]WriteDraft
}

func NewDraftStore() *DraftStore { return &DraftStore{items: make(map[string]WriteDraft)} }

func draftKey(userID, conversationID string) string { return userID + "\x00" + conversationID }

func (s *DraftStore) Merge(userID, conversationID, capability string, params map[string]any, attachments []AttachmentRef) WriteDraft {
	s.mu.Lock()
	defer s.mu.Unlock()
	key := draftKey(userID, conversationID)
	draft := s.items[key]
	if draft.Capability != capability {
		draft = WriteDraft{Capability: capability, Params: make(map[string]any)}
	}
	for name, value := range params {
		if value != nil && strings.TrimSpace(fmt.Sprint(value)) != "" {
			draft.Params[name] = value
		}
	}
	if len(attachments) > 0 {
		draft.Attachments = append([]AttachmentRef(nil), attachments...)
	}
	s.items[key] = draft
	return draft
}

func (s *DraftStore) Delete(userID, conversationID string) {
	s.mu.Lock()
	defer s.mu.Unlock()
	delete(s.items, draftKey(userID, conversationID))
}

func NewConversationStore() *ConversationStore {
	return &ConversationStore{items: make(map[string]*conversation)}
}

func (s *ConversationStore) Resolve(id, userID string) (string, error) {
	s.mu.Lock()
	defer s.mu.Unlock()
	if id == "" {
		var err error
		id, err = randomID()
		if err != nil {
			return "", err
		}
	}
	item, ok := s.items[id]
	if ok && item.Owner != userID {
		return "", errors.New("conversation belongs to another user")
	}
	if !ok {
		s.items[id] = &conversation{Owner: userID, UpdatedAt: time.Now().UTC()}
	}
	return id, nil
}

func (s *ConversationStore) History(id, userID string) []ConversationMessage {
	s.mu.Lock()
	defer s.mu.Unlock()
	item := s.items[id]
	if item == nil || item.Owner != userID {
		return nil
	}
	start := 0
	if len(item.Messages) > 12 {
		start = len(item.Messages) - 12
	}
	return append([]ConversationMessage(nil), item.Messages[start:]...)
}

func (s *ConversationStore) Append(id, userID string, messages ...ConversationMessage) {
	s.mu.Lock()
	defer s.mu.Unlock()
	item := s.items[id]
	if item == nil || item.Owner != userID {
		return
	}
	item.Messages = append(item.Messages, messages...)
	if len(item.Messages) > 20 {
		item.Messages = append([]ConversationMessage(nil), item.Messages[len(item.Messages)-20:]...)
	}
	item.UpdatedAt = time.Now().UTC()
}

func (s *ConversationStore) Delete(id, userID string) bool {
	s.mu.Lock()
	defer s.mu.Unlock()
	item := s.items[id]
	if item == nil || item.Owner != userID {
		return false
	}
	delete(s.items, id)
	return true
}

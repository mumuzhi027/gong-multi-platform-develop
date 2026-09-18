package gongai

import (
	"encoding/json"
	"io"
	"sync"
	"time"
)

type AuditEvent struct {
	UserInternalID string `json:"userInternalId"`
	ActionID       string `json:"actionId"`
	Capability     string `json:"capability"`
	Provider       string `json:"provider"`
	Event          string `json:"event"`
	Timestamp      string `json:"timestamp"`
}

type AuditWriter struct {
	mu sync.Mutex
	w  io.Writer
}

func NewAuditWriter(w io.Writer) *AuditWriter { return &AuditWriter{w: w} }

func (a *AuditWriter) Write(userID, actionID, capability, provider, event string) {
	if a == nil || a.w == nil {
		return
	}
	// Use a non-reversible short digest rather than a student number.
	digest, _ := paramsHash(map[string]any{"user": userID})
	if len(digest) > 16 {
		digest = digest[:16]
	}
	record := AuditEvent{digest, actionID, capability, provider, event, time.Now().UTC().Format(time.RFC3339)}
	a.mu.Lock()
	defer a.mu.Unlock()
	_ = json.NewEncoder(a.w).Encode(record)
}

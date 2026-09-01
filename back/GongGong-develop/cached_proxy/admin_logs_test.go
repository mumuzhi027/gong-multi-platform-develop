package main

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"path/filepath"
	"strings"
	"testing"
)

func TestRequestLoggingAndAdminAccess(t *testing.T) {
	store, err := newRequestLogStore(filepath.Join(t.TempDir(), "logs"), 10)
	if err != nil {
		t.Fatal(err)
	}
	wrapped := requestLoggingMiddleware(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		markRequestAudit(w, "login", "202405190101")
		http.Error(w, "upstream login failed", http.StatusBadGateway)
	}), store)

	response := httptest.NewRecorder()
	wrapped.ServeHTTP(response, httptest.NewRequest(http.MethodPost, "/login?username=secret", strings.NewReader("password=secret")))
	entries := store.list(10, "error", "login", "202405190101")
	if len(entries) != 1 {
		t.Fatalf("entries = %d, want 1", len(entries))
	}
	if entries[0].Path != "/login" || strings.Contains(entries[0].Path, "secret") {
		t.Fatalf("unsafe path logged: %q", entries[0].Path)
	}
	if entries[0].Status != http.StatusBadGateway || entries[0].Message != "upstream login failed" {
		t.Fatalf("unexpected entry: %#v", entries[0])
	}
	if entries[0].Event != "login" || entries[0].Account != "202405190101" {
		t.Fatalf("login audit fields missing: %#v", entries[0])
	}

	handler := &activityHandler{requestLogs: store, adminToken: "test-token", adminOrigin: "*"}
	adminRequest := httptest.NewRequest(http.MethodGet, "/admin/logs?level=error", nil)
	adminRequest.Header.Set("Authorization", "Bearer test-token")
	adminResponse := httptest.NewRecorder()
	handler.adminLogs(adminResponse, adminRequest)
	if adminResponse.Code != http.StatusOK {
		t.Fatalf("admin status = %d, body = %s", adminResponse.Code, adminResponse.Body.String())
	}
	var result struct {
		Items []RequestLogEntry `json:"items"`
		Count int               `json:"count"`
	}
	if err := json.NewDecoder(adminResponse.Body).Decode(&result); err != nil {
		t.Fatal(err)
	}
	if result.Count != 1 || len(result.Items) != 1 {
		t.Fatalf("unexpected admin result: %#v", result)
	}
}

func TestAdminLogsRequiresToken(t *testing.T) {
	store, err := newRequestLogStore(filepath.Join(t.TempDir(), "logs"), 10)
	if err != nil {
		t.Fatal(err)
	}
	handler := &activityHandler{requestLogs: store, adminToken: "test-token", adminOrigin: "*"}
	response := httptest.NewRecorder()
	handler.adminLogs(response, httptest.NewRequest(http.MethodGet, "/admin/logs", nil))
	if response.Code != http.StatusUnauthorized {
		t.Fatalf("status = %d, want %d", response.Code, http.StatusUnauthorized)
	}
}

func TestApplicationErrorResponseIsLogged(t *testing.T) {
	store, err := newRequestLogStore(filepath.Join(t.TempDir(), "logs"), 10)
	if err != nil {
		t.Fatal(err)
	}
	wrapped := requestLoggingMiddleware(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		writeJSON(w, http.StatusNonAuthoritativeInfo, map[string]any{"code": 0, "message": "cache updating"})
	}), store)
	wrapped.ServeHTTP(httptest.NewRecorder(), httptest.NewRequest(http.MethodGet, "/courses", nil))
	entries := store.list(10, "warn", "", "cache updating")
	if len(entries) != 1 || entries[0].Message != "cache updating" {
		t.Fatalf("unexpected application error log: %#v", entries)
	}
}

package main

import (
	"bytes"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"path/filepath"
	"testing"
)

func TestActivityCRUDAndPublicVisibility(t *testing.T) {
	store, err := newActivityStore(filepath.Join(t.TempDir(), "activities"))
	if err != nil {
		t.Fatal(err)
	}
	handler := &activityHandler{store: store, adminToken: "test-token", adminOrigin: "*"}

	payload := Activity{
		Title: "测试活动", StartDate: "2026.09.01", EndDate: "2026.09.02", Published: true,
	}
	body, _ := json.Marshal(payload)
	request := httptest.NewRequest(http.MethodPost, "/admin/activities", bytes.NewReader(body))
	request.Header.Set("Authorization", "Bearer test-token")
	response := httptest.NewRecorder()
	handler.adminActivities(response, request)
	if response.Code != http.StatusCreated {
		t.Fatalf("create status = %d, body = %s", response.Code, response.Body.String())
	}

	publicResponse := httptest.NewRecorder()
	handler.publicActivities(publicResponse, httptest.NewRequest(http.MethodGet, "/activities", nil))
	if publicResponse.Code != http.StatusOK {
		t.Fatalf("public status = %d", publicResponse.Code)
	}
	var payloadResponse struct {
		Data []Activity `json:"data"`
	}
	if err := json.NewDecoder(publicResponse.Body).Decode(&payloadResponse); err != nil {
		t.Fatal(err)
	}
	activities := payloadResponse.Data
	if len(activities) != 1 || activities[0].Title != payload.Title {
		t.Fatalf("unexpected activities: %#v", activities)
	}
}

func TestActivityAdminRequiresToken(t *testing.T) {
	store, err := newActivityStore(filepath.Join(t.TempDir(), "activities"))
	if err != nil {
		t.Fatal(err)
	}
	handler := &activityHandler{store: store, adminToken: "test-token", adminOrigin: "*"}
	response := httptest.NewRecorder()
	handler.adminActivities(response, httptest.NewRequest(http.MethodGet, "/admin/activities", nil))
	if response.Code != http.StatusUnauthorized {
		t.Fatalf("status = %d, want %d", response.Code, http.StatusUnauthorized)
	}
}

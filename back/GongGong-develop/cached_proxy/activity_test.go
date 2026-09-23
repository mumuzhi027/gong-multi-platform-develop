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
	accounts, token := testActivityAccounts(t)
	handler := &activityHandler{store: store, accounts: accounts, adminOrigin: "*"}

	payload := Activity{
		Title: "测试活动", StartDate: "2026.09.01", EndDate: "2026.09.02", Published: true,
	}
	body, _ := json.Marshal(payload)
	request := httptest.NewRequest(http.MethodPost, "/admin/activities", bytes.NewReader(body))
	request.Header.Set("Authorization", "Bearer "+token)
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
	accounts, _ := testActivityAccounts(t)
	handler := &activityHandler{store: store, accounts: accounts, adminOrigin: "*"}
	response := httptest.NewRecorder()
	handler.adminActivities(response, httptest.NewRequest(http.MethodGet, "/admin/activities", nil))
	if response.Code != http.StatusUnauthorized {
		t.Fatalf("status = %d, want %d", response.Code, http.StatusUnauthorized)
	}
}

func testActivityAccounts(t *testing.T) (*activityAccountStore, string) {
	t.Helper()
	t.Setenv("ACTIVITY_SUPERADMIN_PASSWORD", "strong-test-password")
	accounts, err := newActivityAccountStore(t.TempDir())
	if err != nil { t.Fatal(err) }
	token, _, ok := accounts.login(superActivityAccount, "strong-test-password")
	if !ok { t.Fatal("super admin could not log in") }
	return accounts, token
}

func TestOrdinaryAccountCannotChangeOtherActivityOrPriority(t *testing.T) {
	store, err := newActivityStore(filepath.Join(t.TempDir(), "activities"))
	if err != nil { t.Fatal(err) }
	accounts, _ := testActivityAccounts(t)
	if _, err := accounts.put("三翼", "ordinary-password-1"); err != nil { t.Fatal(err) }
	if _, err := accounts.put("学生会", "ordinary-password-2"); err != nil { t.Fatal(err) }
	token, _, ok := accounts.login("三翼", "ordinary-password-1")
	if !ok { t.Fatal("ordinary account could not log in") }
	handler := &activityHandler{store: store, accounts: accounts}
	payload := Activity{Title: "活动", StartDate: "2026-09-01", EndDate: "2026-09-02", Featured: true, SortOrder: -10, Organizer: "伪造组织", Published: true}
	postBody, _ := json.Marshal(payload)
	post := httptest.NewRequest(http.MethodPost, "/admin/activities", bytes.NewReader(postBody))
	post.Header.Set("Authorization", "Bearer "+token)
	response := httptest.NewRecorder()
	handler.adminActivities(response, post)
	if response.Code != http.StatusCreated { t.Fatalf("create: %d %s", response.Code, response.Body.String()) }
	var created Activity
	if err := json.NewDecoder(response.Body).Decode(&created); err != nil { t.Fatal(err) }
	if created.OwnerID != "三翼" || created.Organizer != "三翼" || created.Featured || created.SortOrder != 0 { t.Fatalf("bad ordinary defaults: %+v", created) }
	otherToken, _, _ := accounts.login("学生会", "ordinary-password-2")
	put := httptest.NewRequest(http.MethodPut, "/admin/activities/"+created.ID, bytes.NewReader(postBody))
	put.Header.Set("Authorization", "Bearer "+otherToken)
	response = httptest.NewRecorder()
	handler.adminActivity(response, put)
	if response.Code != http.StatusForbidden { t.Fatalf("cross-account edit status = %d", response.Code) }
	deleteRequest := httptest.NewRequest(http.MethodDelete, "/admin/activities/"+created.ID, nil)
	deleteRequest.Header.Set("Authorization", "Bearer "+otherToken)
	response = httptest.NewRecorder()
	handler.adminActivity(response, deleteRequest)
	if response.Code != http.StatusForbidden { t.Fatalf("cross-account delete status = %d", response.Code) }
}

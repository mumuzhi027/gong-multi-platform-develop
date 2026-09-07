package main

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"os"
	"path/filepath"
	"testing"
	"time"
)

func TestUserStatsRecordsSuccessfulLoginsByRange(t *testing.T) {
	store, err := newUserStatsStore(filepath.Join(t.TempDir(), "stats"))
	if err != nil {
		t.Fatal(err)
	}
	location := time.Local
	loginTimes := []struct {
		account string
		at      time.Time
	}{
		{"202405190101", time.Date(2026, 8, 30, 10, 0, 0, 0, location)},
		{"202405190101", time.Date(2026, 8, 31, 11, 0, 0, 0, location)},
		{"202405190102", time.Date(2026, 8, 31, 12, 0, 0, 0, location)},
	}
	for _, login := range loginTimes {
		if err := store.recordSuccessfulLogin(login.account, login.at); err != nil {
			t.Fatal(err)
		}
	}
	from := time.Date(2026, 8, 31, 0, 0, 0, 0, location)
	to := from.AddDate(0, 0, 1)
	snapshot := store.snapshot(from, to)
	if snapshot.TotalUsers != 2 || snapshot.AccumulatedBeforeFrom != 1 || snapshot.NewUsers != 1 {
		t.Fatalf("unexpected accumulated stats: %#v", snapshot)
	}
	if snapshot.ActiveUsers != 2 || snapshot.LoginCount != 2 {
		t.Fatalf("unexpected range stats: %#v", snapshot)
	}
	if len(snapshot.Daily) != 1 || snapshot.Daily[0].ActiveUsers != 2 {
		t.Fatalf("unexpected daily stats: %#v", snapshot.Daily)
	}
}

func TestUserStatsPersistsAndRecoversUserList(t *testing.T) {
	dataDir := filepath.Join(t.TempDir(), "stats")
	store, err := newUserStatsStore(dataDir)
	if err != nil {
		t.Fatal(err)
	}
	loginAt := time.Date(2026, 9, 1, 12, 0, 0, 0, time.Local)
	if err := store.recordSuccessfulLogin("202405190101", loginAt); err != nil {
		t.Fatal(err)
	}
	if err := store.recordSuccessfulLogin("202405190102", loginAt.Add(time.Hour)); err != nil {
		t.Fatal(err)
	}

	listFile, err := os.Open(filepath.Join(dataDir, "user-list.json"))
	if err != nil {
		t.Fatal(err)
	}
	var list persistedUserList
	if err := json.NewDecoder(listFile).Decode(&list); err != nil {
		listFile.Close()
		t.Fatal(err)
	}
	if err := listFile.Close(); err != nil {
		t.Fatal(err)
	}
	if list.Count != 2 || len(list.Users) != 2 || list.Users[0].Account != "202405190101" {
		t.Fatalf("unexpected persisted user list: %#v", list)
	}

	if err := os.Remove(filepath.Join(dataDir, "user-stats.json")); err != nil {
		t.Fatal(err)
	}
	recovered, err := newUserStatsStore(dataDir)
	if err != nil {
		t.Fatal(err)
	}
	from := time.Date(2026, 9, 1, 0, 0, 0, 0, time.Local)
	snapshot := recovered.snapshot(from, from.AddDate(0, 0, 1))
	if snapshot.TotalUsers != 2 {
		t.Fatalf("expected two recovered users, got %#v", snapshot)
	}
}

func TestAdminUserStatsRequiresAuthorization(t *testing.T) {
	store, err := newUserStatsStore(filepath.Join(t.TempDir(), "stats"))
	if err != nil {
		t.Fatal(err)
	}
	handler := &activityHandler{userStats: store, adminToken: "test-token", adminOrigin: "*"}
	response := httptest.NewRecorder()
	handler.adminUserStats(response, httptest.NewRequest(http.MethodGet, "/admin/user-stats", nil))
	if response.Code != http.StatusUnauthorized {
		t.Fatalf("expected unauthorized, got %d", response.Code)
	}
}

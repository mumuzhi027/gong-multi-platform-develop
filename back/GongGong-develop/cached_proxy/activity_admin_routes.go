package main

import (
	"encoding/json"
	"errors"
	"net/http"
	"os"
	"sort"
	"strings"
	"time"
)

func (h *activityHandler) adminSession(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Cache-Control", "no-store")
	if r.Method == http.MethodOptions { w.WriteHeader(http.StatusNoContent); return }
	if r.Method == http.MethodGet {
		account, ok := h.authorize(w, r)
		if ok { writeJSON(w, http.StatusOK, account.public()) }
		return
	}
	if r.Method == http.MethodDelete {
		if _, ok := h.authorize(w, r); !ok { return }
		h.accounts.logout(r)
		w.WriteHeader(http.StatusNoContent)
		return
	}
	if r.Method != http.MethodPost { http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed); return }
	r.Body = http.MaxBytesReader(w, r.Body, 4096)
	var input struct { Name string `json:"name"`; Password string `json:"password"` }
	if json.NewDecoder(r.Body).Decode(&input) != nil { http.Error(w, "Invalid credentials", http.StatusBadRequest); return }
	token, account, ok := h.accounts.login(strings.TrimSpace(input.Name), input.Password)
	if !ok { http.Error(w, "Unauthorized", http.StatusUnauthorized); return }
	writeJSON(w, http.StatusOK, map[string]any{"token": token, "account": account})
}

func (h *activityHandler) adminAccounts(w http.ResponseWriter, r *http.Request) {
	if r.Method == http.MethodOptions { w.WriteHeader(http.StatusNoContent); return }
	actor, ok := h.authorize(w, r)
	if !ok { return }
	if !actor.Super { http.Error(w, "Forbidden", http.StatusForbidden); return }
	switch r.Method {
	case http.MethodGet:
		items := h.accounts.list()
		sort.Slice(items, func(i, j int) bool { return items[i].Name < items[j].Name })
		writeJSON(w, http.StatusOK, items)
	case http.MethodPost:
		name, password, ok := readActivityAccountPayload(w, r)
		if !ok { return }
		if _, exists := h.accounts.find(name); exists { http.Error(w, "Account already exists", http.StatusConflict); return }
		created, err := h.accounts.put(name, password)
		if err != nil { http.Error(w, err.Error(), http.StatusBadRequest); return }
		h.logAccountOperation("account.create", actor, name)
		writeJSON(w, http.StatusCreated, created)
	default:
		http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
	}
}

func (h *activityHandler) adminAccount(w http.ResponseWriter, r *http.Request) {
	if r.Method == http.MethodOptions { w.WriteHeader(http.StatusNoContent); return }
	actor, ok := h.authorize(w, r)
	if !ok { return }
	if !actor.Super { http.Error(w, "Forbidden", http.StatusForbidden); return }
	name := strings.TrimPrefix(r.URL.Path, "/admin/accounts/")
	if name == "" || strings.Contains(name, "/") { http.Error(w, "Invalid account", http.StatusBadRequest); return }
	if _, exists := h.accounts.find(name); !exists { http.Error(w, "Account not found", http.StatusNotFound); return }
	switch r.Method {
	case http.MethodPut:
		inputName, password, ok := readActivityAccountPayload(w, r)
		if !ok { return }
		if inputName != name { http.Error(w, "Account name is immutable", http.StatusBadRequest); return }
		updated, err := h.accounts.put(name, password)
		if err != nil { http.Error(w, err.Error(), http.StatusBadRequest); return }
		h.logAccountOperation("account.update", actor, name)
		writeJSON(w, http.StatusOK, updated)
	case http.MethodDelete:
		if h.store.countOwned(name) > 0 { http.Error(w, "Transfer or delete this account's activities first", http.StatusConflict); return }
		if err := h.accounts.delete(name); err != nil {
			if errors.Is(err, os.ErrNotExist) { http.Error(w, "Account not found", http.StatusNotFound) } else { http.Error(w, err.Error(), http.StatusBadRequest) }
			return
		}
		h.logAccountOperation("account.delete", actor, name)
		w.WriteHeader(http.StatusNoContent)
	default:
		http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
	}
}

func readActivityAccountPayload(w http.ResponseWriter, r *http.Request) (string, string, bool) {
	r.Body = http.MaxBytesReader(w, r.Body, 4096)
	var payload struct { Name string `json:"name"`; Password string `json:"password"` }
	if json.NewDecoder(r.Body).Decode(&payload) != nil { http.Error(w, "Invalid account payload", http.StatusBadRequest); return "", "", false }
	return strings.TrimSpace(payload.Name), payload.Password, true
}

func (h *activityHandler) logActivityOperation(event string, actor activityAccount, activity Activity) {
	if h.operationLogs != nil { h.operationLogs.append(RequestLogEntry{
		Timestamp: time.Now().UTC(), Level: "info", Event: event, Account: actor.Name,
		Method: "AUDIT", Path: "/admin/activities/" + activity.ID, Status: 200,
		Message: activity.Title,
	}) }
}

func (h *activityHandler) logAccountOperation(event string, actor activityAccount, name string) {
	if h.operationLogs != nil { h.operationLogs.append(RequestLogEntry{
		Timestamp: time.Now().UTC(), Level: "info", Event: event, Account: actor.Name,
		Method: "AUDIT", Path: "/admin/accounts", Status: 200, Message: name,
	}) }
}

func (h *activityHandler) adminOperationLogs(w http.ResponseWriter, r *http.Request) {
	if r.Method == http.MethodOptions { w.WriteHeader(http.StatusNoContent); return }
	if _, ok := h.authorize(w, r); !ok { return }
	if r.Method != http.MethodGet { http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed); return }
	if h.operationLogs == nil { writeJSON(w, http.StatusOK, map[string]any{"items": []RequestLogEntry{}}); return }
	all := h.operationLogs.list(500, "", "", "")
	items := make([]RequestLogEntry, 0)
	for _, entry := range all { if strings.HasPrefix(entry.Event, "activity.") || strings.HasPrefix(entry.Event, "account.") || strings.HasPrefix(entry.Event, "feature.") { items = append(items, entry) } }
	writeJSON(w, http.StatusOK, map[string]any{"items": items})
}

func (h *activityHandler) publicActivityOrganizers(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet { http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed); return }
	registered := make(map[string]bool)
	for _, account := range h.accounts.list() { registered[account.ID] = true }
	current, past := make(map[string]bool), make(map[string]bool)
	now := time.Now()
	today := now.Format("2006-01-02")
	for _, activity := range h.store.list(false) {
		if !activityVisibleToPublic(activity, now) { continue }
		if !registered[activity.Organizer] { continue }
		if strings.ReplaceAll(activity.EndDate, ".", "-") < today { past[activity.Organizer] = true } else { current[activity.Organizer] = true }
	}
	currentList, pastList := make([]string, 0, len(current)), make([]string, 0, len(past))
	for name := range current { currentList = append(currentList, name) }
	for name := range past { pastList = append(pastList, name) }
	sort.Strings(currentList); sort.Strings(pastList)
	writeJSON(w, http.StatusOK, map[string]any{"code": 1, "message": "success", "data": map[string]any{"current": currentList, "past": pastList}})
}

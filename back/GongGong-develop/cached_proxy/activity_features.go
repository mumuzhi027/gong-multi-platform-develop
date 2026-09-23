package main

import (
	"encoding/json"
	"net/http"
	"os"
	"path/filepath"
)

type publicFeatures struct {
	GongAIVisible bool `json:"gongAiVisible"`
}

func readPublicFeatures() publicFeatures {
	data, err := os.ReadFile(filepath.Join(ActivityDataDir, "public-features.json"))
	if err != nil { return publicFeatures{} }
	var features publicFeatures
	if json.Unmarshal(data, &features) != nil { return publicFeatures{} }
	return features
}

func (h *activityHandler) publicFeatures(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet { http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed); return }
	writeJSON(w, http.StatusOK, map[string]any{"code": 1, "message": "success", "data": readPublicFeatures()})
}

func (h *activityHandler) adminFeatures(w http.ResponseWriter, r *http.Request) {
	if r.Method == http.MethodOptions { w.WriteHeader(http.StatusNoContent); return }
	actor, ok := h.authorize(w, r)
	if !ok { return }
	if !actor.Super { http.Error(w, "Forbidden", http.StatusForbidden); return }
	if r.Method == http.MethodGet { writeJSON(w, http.StatusOK, readPublicFeatures()); return }
	if r.Method != http.MethodPut { http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed); return }
	r.Body = http.MaxBytesReader(w, r.Body, 2048)
	var features publicFeatures
	if json.NewDecoder(r.Body).Decode(&features) != nil { http.Error(w, "Invalid feature config", http.StatusBadRequest); return }
	if err := writeActivityJSONFile(filepath.Join(ActivityDataDir, "public-features.json"), features); err != nil {
		http.Error(w, "Unable to save feature config", http.StatusInternalServerError); return
	}
	h.logAccountOperation("feature.update", actor, "gongAiVisible")
	writeJSON(w, http.StatusOK, features)
}

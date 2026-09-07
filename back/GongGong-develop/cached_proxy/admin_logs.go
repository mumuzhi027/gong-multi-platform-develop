package main

import (
	"bufio"
	"encoding/json"
	"errors"
	"net/http"
	"os"
	"path/filepath"
	"strconv"
	"strings"
	"sync"
	"time"
)

const (
	defaultRequestLogCapacity = 5000
	maxRequestLogFileSize     = 25 << 20
	maxLoggedErrorLength      = 600
	maxInspectedResponseSize  = 4096
)

type RequestLogEntry struct {
	Timestamp  time.Time `json:"timestamp"`
	Level      string    `json:"level"`
	Event      string    `json:"event,omitempty"`
	Account    string    `json:"account,omitempty"`
	Method     string    `json:"method"`
	Path       string    `json:"path"`
	Status     int       `json:"status"`
	DurationMS int64     `json:"durationMs"`
	Message    string    `json:"message,omitempty"`
}

type requestLogStore struct {
	mu       sync.RWMutex
	filePath string
	capacity int
	items    []RequestLogEntry
}

func newRequestLogStore(dataDir string, capacity int) (*requestLogStore, error) {
	if capacity <= 0 {
		capacity = defaultRequestLogCapacity
	}
	if err := os.MkdirAll(dataDir, 0755); err != nil {
		return nil, err
	}
	store := &requestLogStore{
		filePath: filepath.Join(dataDir, "request-logs.jsonl"),
		capacity: capacity,
		items:    make([]RequestLogEntry, 0, capacity),
	}
	if err := store.load(); err != nil {
		return nil, err
	}
	return store, nil
}

func (s *requestLogStore) load() error {
	file, err := os.Open(s.filePath)
	if errors.Is(err, os.ErrNotExist) {
		return nil
	}
	if err != nil {
		return err
	}
	defer file.Close()

	scanner := bufio.NewScanner(file)
	scanner.Buffer(make([]byte, 4096), 1<<20)
	for scanner.Scan() {
		var entry RequestLogEntry
		if json.Unmarshal(scanner.Bytes(), &entry) == nil {
			s.items = append(s.items, entry)
			if len(s.items) > s.capacity {
				s.items = s.items[len(s.items)-s.capacity:]
			}
		}
	}
	return scanner.Err()
}

func (s *requestLogStore) append(entry RequestLogEntry) {
	s.mu.Lock()
	defer s.mu.Unlock()

	s.items = append(s.items, entry)
	if len(s.items) > s.capacity {
		s.items = append([]RequestLogEntry(nil), s.items[len(s.items)-s.capacity:]...)
	}

	file, err := os.OpenFile(s.filePath, os.O_CREATE|os.O_APPEND|os.O_WRONLY, 0640)
	if err == nil {
		_ = json.NewEncoder(file).Encode(entry)
		_ = file.Close()
	}
	if info, err := os.Stat(s.filePath); err == nil && info.Size() > maxRequestLogFileSize {
		_ = s.rewriteLocked()
	}
}

func (s *requestLogStore) list(limit int, level string, event string, query string) []RequestLogEntry {
	s.mu.RLock()
	defer s.mu.RUnlock()

	if limit <= 0 || limit > s.capacity {
		limit = 200
	}
	level = strings.ToLower(strings.TrimSpace(level))
	event = strings.ToLower(strings.TrimSpace(event))
	query = strings.ToLower(strings.TrimSpace(query))
	result := make([]RequestLogEntry, 0, limit)
	for index := len(s.items) - 1; index >= 0 && len(result) < limit; index-- {
		entry := s.items[index]
		if level != "" && entry.Level != level {
			continue
		}
		if event != "" && entry.Event != event {
			continue
		}
		if query != "" {
			haystack := strings.ToLower(entry.Event + " " + entry.Account + " " + entry.Method + " " + entry.Path + " " + entry.Message + " " + strconv.Itoa(entry.Status))
			if !strings.Contains(haystack, query) {
				continue
			}
		}
		result = append(result, entry)
	}
	return result
}

func (s *requestLogStore) clear() error {
	s.mu.Lock()
	defer s.mu.Unlock()
	s.items = s.items[:0]
	if err := os.Remove(s.filePath); err != nil && !errors.Is(err, os.ErrNotExist) {
		return err
	}
	return nil
}

func (s *requestLogStore) rewriteLocked() error {
	temp, err := os.CreateTemp(filepath.Dir(s.filePath), "request-logs-*.jsonl")
	if err != nil {
		return err
	}
	tempPath := temp.Name()
	defer os.Remove(tempPath)

	encoder := json.NewEncoder(temp)
	for _, entry := range s.items {
		if err := encoder.Encode(entry); err != nil {
			temp.Close()
			return err
		}
	}
	if err := temp.Close(); err != nil {
		return err
	}
	if err := os.Chmod(tempPath, 0640); err != nil {
		return err
	}
	return os.Rename(tempPath, s.filePath)
}

type requestLogResponseWriter struct {
	http.ResponseWriter
	status  int
	message strings.Builder
	event   string
	account string
}

func (w *requestLogResponseWriter) SetRequestAudit(event string, account string) {
	w.event = strings.ToLower(strings.TrimSpace(event))
	w.account = sanitizeAuditAccount(account)
}

func (w *requestLogResponseWriter) WriteHeader(status int) {
	if w.status == 0 {
		w.status = status
	}
	w.ResponseWriter.WriteHeader(status)
}

func (w *requestLogResponseWriter) Write(body []byte) (int, error) {
	if w.status == 0 {
		w.status = http.StatusOK
	}
	if w.message.Len() < maxInspectedResponseSize {
		remaining := maxInspectedResponseSize - w.message.Len()
		if len(body) < remaining {
			remaining = len(body)
		}
		_, _ = w.message.Write(body[:remaining])
	}
	return w.ResponseWriter.Write(body)
}

func requestLoggingMiddleware(next http.Handler, store *requestLogStore) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if store == nil || r.URL.Path == "/admin/logs" || r.Method == http.MethodOptions {
			next.ServeHTTP(w, r)
			return
		}

		started := time.Now()
		writer := &requestLogResponseWriter{ResponseWriter: w}
		next.ServeHTTP(writer, r)
		if writer.status == 0 {
			writer.status = http.StatusOK
		}

		message := extractLogMessage(writer.status, writer.message.String())
		level := "info"
		if writer.status >= 500 {
			level = "error"
		} else if writer.status >= 400 || message != "" {
			level = "warn"
		}
		store.append(RequestLogEntry{
			Timestamp:  time.Now().UTC(),
			Level:      level,
			Event:      writer.event,
			Account:    writer.account,
			Method:     r.Method,
			Path:       r.URL.Path,
			Status:     writer.status,
			DurationMS: time.Since(started).Milliseconds(),
			Message:    message,
		})
	})
}

func markRequestAudit(w http.ResponseWriter, event string, account string) {
	if marker, ok := w.(interface {
		SetRequestAudit(string, string)
	}); ok {
		marker.SetRequestAudit(event, account)
	}
}

func sanitizeAuditAccount(account string) string {
	account = strings.Join(strings.Fields(account), "")
	if len(account) > 64 {
		account = account[:64]
	}
	return account
}

func extractLogMessage(status int, responseBody string) string {
	if status >= 400 {
		return sanitizeLogMessage(responseBody)
	}
	var response struct {
		Code    *int   `json:"code"`
		Message string `json:"message"`
	}
	if json.Unmarshal([]byte(responseBody), &response) == nil && response.Code != nil && *response.Code == 0 {
		return sanitizeLogMessage(response.Message)
	}
	return ""
}

func sanitizeLogMessage(message string) string {
	message = strings.Join(strings.Fields(message), " ")
	if len(message) > maxLoggedErrorLength {
		message = message[:maxLoggedErrorLength]
	}
	return message
}

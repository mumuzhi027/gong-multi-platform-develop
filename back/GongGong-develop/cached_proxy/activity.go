package main

import (
	"bytes"
	"crypto/rand"
	"encoding/binary"
	"encoding/hex"
	"encoding/json"
	"errors"
	"fmt"
	"image"
	_ "image/jpeg"
	_ "image/png"
	"io"
	"net/http"
	"os"
	"path/filepath"
	"sort"
	"strconv"
	"strings"
	"sync"
	"time"
)

const (
	maxActivityBodySize = 1 << 20
	maxPosterUploadSize = 8 << 20
	maxStoredImageSize  = 2 << 20
	maxActivityImages   = 9
	maxImageDimension   = 4096
	maxImagePixels      = 16_000_000
	activityRevealDays  = 15
)

var errActivityForbidden = errors.New("activity belongs to another account")

type Activity struct {
	ID             string    `json:"id"`
	Title          string    `json:"title"`
	Organizer      string    `json:"organizer"`
	Participation  string    `json:"participation"`
	Location       string    `json:"location"`
	StartDate      string    `json:"startDate"`
	EndDate        string    `json:"endDate"`
	RegistrationDeadline string `json:"registrationDeadline,omitempty"`
	PosterTitle    string    `json:"posterTitle"`
	PosterSubtitle string    `json:"posterSubtitle"`
	PosterURL      string    `json:"posterUrl"`
	Images         []string  `json:"images"`
	Description    string    `json:"description"`
	Featured       bool      `json:"featured"`
	Published      bool      `json:"published"`
	SortOrder      int       `json:"sortOrder"`
	FilterTags     []string  `json:"filterTags"`
	OwnerID        string    `json:"ownerId,omitempty"`
	CreatedAt      time.Time `json:"createdAt"`
	UpdatedAt      time.Time `json:"updatedAt"`
}

type activityStore struct {
	mu       sync.RWMutex
	filePath string
	items    map[string]Activity
}

func newActivityStore(dataDir string) (*activityStore, error) {
	if err := os.MkdirAll(filepath.Join(dataDir, "posters"), 0755); err != nil {
		return nil, fmt.Errorf("create activity data directory: %w", err)
	}

	store := &activityStore{
		filePath: filepath.Join(dataDir, "activities.json"),
		items:    make(map[string]Activity),
	}
	if err := store.load(); err != nil {
		return nil, err
	}
	return store, nil
}

func (s *activityStore) load() error {
	file, err := os.Open(s.filePath)
	if errors.Is(err, os.ErrNotExist) {
		return nil
	}
	if err != nil {
		return fmt.Errorf("open activities: %w", err)
	}
	defer file.Close()

	var activities []Activity
	if err := json.NewDecoder(file).Decode(&activities); err != nil {
		return fmt.Errorf("decode activities: %w", err)
	}
	for _, activity := range activities {
		s.items[activity.ID] = activity
	}
	return nil
}

func (s *activityStore) list(includeUnpublished bool) []Activity {
	s.mu.RLock()
	defer s.mu.RUnlock()

	activities := make([]Activity, 0, len(s.items))
	for _, activity := range s.items {
		if includeUnpublished || activity.Published {
			// Older records may not contain these optional arrays. Encoding a nil
			// slice as null breaks clients whose schema expects a non-null list.
			if activity.Images == nil {
				activity.Images = []string{}
			}
			if activity.FilterTags == nil {
				activity.FilterTags = []string{}
			}
			activities = append(activities, activity)
		}
	}
	sort.SliceStable(activities, func(i, j int) bool {
		left, right := activities[i], activities[j]
		if left.Featured != right.Featured { return left.Featured }
		if left.SortOrder != right.SortOrder { return left.SortOrder < right.SortOrder }
		leftDate, rightDate := left.RegistrationDeadline, right.RegistrationDeadline
		if leftDate == "" { leftDate = left.StartDate }
		if rightDate == "" { rightDate = right.StartDate }
		if leftDate != rightDate { return leftDate < rightDate }
		return activities[i].CreatedAt.Before(activities[j].CreatedAt)
	})
	return activities
}

func (s *activityStore) create(activity Activity) (Activity, error) {
	s.mu.Lock()
	defer s.mu.Unlock()

	now := time.Now().UTC()
	activity.ID = randomID()
	activity.CreatedAt = now
	activity.UpdatedAt = now
	s.items[activity.ID] = activity
	if err := s.writeLocked(); err != nil {
		delete(s.items, activity.ID)
		return Activity{}, err
	}
	return activity, nil
}

func (s *activityStore) get(id string) (Activity, bool) {
	s.mu.RLock()
	defer s.mu.RUnlock()
	a, ok := s.items[id]
	return a, ok
}

func (s *activityStore) countOwned(owner string) int {
	s.mu.RLock()
	defer s.mu.RUnlock()
	count := 0
	for _, activity := range s.items { if activity.OwnerID == owner { count++ } }
	return count
}

func (s *activityStore) update(id string, activity Activity, actor activityAccount) (Activity, error) {
	s.mu.Lock()
	defer s.mu.Unlock()

	current, ok := s.items[id]
	if !ok {
		return Activity{}, os.ErrNotExist
	}
	if !actor.Super && current.OwnerID != actor.ID { return Activity{}, errActivityForbidden }
	activity.ID = id
	activity.OwnerID = current.OwnerID
	if !actor.Super { activity.Organizer = actor.Name; activity.Featured = current.Featured; activity.SortOrder = current.SortOrder }
	activity.CreatedAt = current.CreatedAt
	activity.UpdatedAt = time.Now().UTC()
	s.items[id] = activity
	if err := s.writeLocked(); err != nil {
		s.items[id] = current
		return Activity{}, err
	}
	return activity, nil
}

func (s *activityStore) delete(id string, actor activityAccount) (bool, error) {
	s.mu.Lock()
	defer s.mu.Unlock()

	current, ok := s.items[id]
	if !ok {
		return false, nil
	}
	if !actor.Super && current.OwnerID != actor.ID { return false, errActivityForbidden }
	delete(s.items, id)
	if err := s.writeLocked(); err != nil {
		s.items[id] = current
		return false, err
	}
	return true, nil
}

func (s *activityStore) writeLocked() error {
	activities := make([]Activity, 0, len(s.items))
	for _, activity := range s.items {
		activities = append(activities, activity)
	}

	temp, err := os.CreateTemp(filepath.Dir(s.filePath), "activities-*.json")
	if err != nil {
		return err
	}
	tempPath := temp.Name()
	defer os.Remove(tempPath)

	encoder := json.NewEncoder(temp)
	encoder.SetIndent("", "  ")
	if err := encoder.Encode(activities); err != nil {
		temp.Close()
		return err
	}
	if err := temp.Sync(); err != nil {
		temp.Close()
		return err
	}
	if err := temp.Close(); err != nil {
		return err
	}
	return os.Rename(tempPath, s.filePath)
}

type activityHandler struct {
	store       *activityStore
	requestLogs *requestLogStore
	operationLogs *requestLogStore
	userStats   *userStatsStore
	dataDir     string
	accounts    *activityAccountStore
	adminOrigin string
	filterPath  string
	filterMu    sync.RWMutex
}

func RegisterActivityRoutes(mux *http.ServeMux, requestLogs *requestLogStore, userStats *userStatsStore) {
	store, err := newActivityStore(ActivityDataDir)
	if err != nil {
		panic(err)
	}
	accounts, err := newActivityAccountStore(ActivityDataDir)
	if err != nil { panic(err) }
	operationLogs, err := newRequestLogStore(filepath.Join(ActivityDataDir, "operations"), defaultRequestLogCapacity)
	if err != nil { panic(err) }
	handler := &activityHandler{
		store:       store,
		requestLogs: requestLogs,
		operationLogs: operationLogs,
		userStats:   userStats,
		dataDir:     ActivityDataDir,
		accounts:    accounts,
		adminOrigin: ActivityAdminOrigin,
		filterPath:  filepath.Join(ActivityDataDir, "activity-filters.json"),
	}

	mux.HandleFunc("/activities", handler.publicActivities)
	mux.HandleFunc("/activity-organizers", handler.publicActivityOrganizers)
	mux.HandleFunc("/public-features", handler.publicFeatures)
	mux.HandleFunc("/admin/features", handler.withCORS(handler.adminFeatures))
	mux.HandleFunc("/activity-filters", handler.publicActivityFilters)
	mux.HandleFunc("/admin/session", handler.withCORS(handler.adminSession))
	mux.HandleFunc("/admin/accounts", handler.withCORS(handler.adminAccounts))
	mux.HandleFunc("/admin/accounts/", handler.withCORS(handler.adminAccount))
	mux.HandleFunc("/admin/operation-logs", handler.withCORS(handler.adminOperationLogs))
	mux.HandleFunc("/admin/activity-filters", handler.withCORS(handler.adminActivityFilters))
	mux.HandleFunc("/admin/activities", handler.withCORS(handler.adminActivities))
	mux.HandleFunc("/admin/activities/", handler.withCORS(handler.adminActivity))
	mux.HandleFunc("/admin/activity-posters", handler.withCORS(handler.uploadPoster))
	mux.HandleFunc("/admin/logs", handler.withCORS(handler.adminLogs))
	mux.HandleFunc("/admin/user-stats", handler.withCORS(handler.adminUserStats))
	posterFiles := http.StripPrefix(
		"/activity-posters/",
		http.FileServer(http.Dir(filepath.Join(ActivityDataDir, "posters"))),
	)
	mux.Handle("/activity-posters/", http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Cache-Control", "public, max-age=2592000, immutable")
		posterFiles.ServeHTTP(w, r)
	}))
}

func (h *activityHandler) readActivityFilters() ([]string, error) {
	h.filterMu.RLock()
	defer h.filterMu.RUnlock()
	data, err := os.ReadFile(h.filterPath)
	if errors.Is(err, os.ErrNotExist) {
		return []string{}, nil
	}
	if err != nil {
		return nil, err
	}
	var filters []string
	if err := json.Unmarshal(data, &filters); err != nil {
		return nil, err
	}
	return filters, nil
}

func normalizeActivityFilters(filters []string) []string {
	result, seen := make([]string, 0, len(filters)), map[string]bool{}
	for _, item := range filters {
		item = strings.TrimSpace(item)
		if item != "" && len([]rune(item)) <= 20 && !seen[item] {
			seen[item] = true
			result = append(result, item)
		}
		if len(result) == 12 {
			break
		}
	}
	return result
}

func (h *activityHandler) publicActivityFilters(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
		return
	}
	filters, err := h.readActivityFilters()
	if err != nil {
		http.Error(w, "Unable to read activity filters", http.StatusInternalServerError)
		return
	}
	writeJSON(w, http.StatusOK, map[string]any{"code": 1, "message": "success", "data": filters})
}

func (h *activityHandler) adminActivityFilters(w http.ResponseWriter, r *http.Request) {
	if r.Method == http.MethodOptions {
		w.WriteHeader(http.StatusNoContent)
		return
	}
	actor, ok := h.authorize(w, r)
	if !ok { return }
	switch r.Method {
	case http.MethodGet:
		filters, err := h.readActivityFilters()
		if err != nil {
			http.Error(w, "Unable to read activity filters", http.StatusInternalServerError)
			return
		}
		writeJSON(w, http.StatusOK, filters)
	case http.MethodPut:
		if !actor.Super { http.Error(w, "Forbidden", http.StatusForbidden); return }
		r.Body = http.MaxBytesReader(w, r.Body, maxActivityBodySize)
		var payload struct {
			Filters []string `json:"filters"`
		}
		if err := json.NewDecoder(r.Body).Decode(&payload); err != nil {
			http.Error(w, "Invalid filter payload", http.StatusBadRequest)
			return
		}
		filters := normalizeActivityFilters(payload.Filters)
		data, _ := json.MarshalIndent(filters, "", "  ")
		h.filterMu.Lock()
		err := os.WriteFile(h.filterPath, append(data, '\n'), 0640)
		h.filterMu.Unlock()
		if err != nil {
			http.Error(w, "Unable to save activity filters", http.StatusInternalServerError)
			return
		}
		writeJSON(w, http.StatusOK, filters)
	default:
		http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
	}
}

func (h *activityHandler) adminUserStats(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Cache-Control", "no-store")
	if r.Method == http.MethodOptions {
		w.WriteHeader(http.StatusNoContent)
		return
	}
	actor, ok := h.authorize(w, r)
	if !ok { return }
	if !actor.Super { http.Error(w, "Forbidden", http.StatusForbidden); return }
	if r.Method != http.MethodGet {
		http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
		return
	}
	if h.userStats == nil {
		http.Error(w, "User statistics are not available", http.StatusServiceUnavailable)
		return
	}
	from, to, err := parseUserStatsRange(r, h.userStats.trackingStartedAt())
	if err != nil {
		http.Error(w, "Dates must use YYYY-MM-DD and form a valid range", http.StatusBadRequest)
		return
	}
	writeJSON(w, http.StatusOK, h.userStats.snapshot(from, to))
}

func (h *activityHandler) adminLogs(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Cache-Control", "no-store")
	if r.Method == http.MethodOptions {
		w.WriteHeader(http.StatusNoContent)
		return
	}
	actor, ok := h.authorize(w, r)
	if !ok { return }
	if !actor.Super { http.Error(w, "Forbidden", http.StatusForbidden); return }
	if h.requestLogs == nil {
		http.Error(w, "Request logs are not available", http.StatusServiceUnavailable)
		return
	}

	switch r.Method {
	case http.MethodGet:
		limit, _ := strconv.Atoi(r.URL.Query().Get("limit"))
		items := h.requestLogs.list(limit, r.URL.Query().Get("level"), r.URL.Query().Get("event"), r.URL.Query().Get("query"))
		writeJSON(w, http.StatusOK, map[string]any{"items": items, "count": len(items)})
	case http.MethodDelete:
		if err := h.requestLogs.clear(); err != nil {
			http.Error(w, "Unable to clear request logs", http.StatusInternalServerError)
			return
		}
		w.WriteHeader(http.StatusNoContent)
	default:
		http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
	}
}

func (h *activityHandler) publicActivities(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
		return
	}
	allActivities := h.store.list(false)
	activities := make([]Activity, 0, len(allActivities))
	now := time.Now()
	for _, activity := range allActivities {
		if activityVisibleToPublic(activity, now) {
			activities = append(activities, activity)
		}
	}
	if limit, err := strconv.Atoi(r.URL.Query().Get("limit")); err == nil && limit > 0 && limit < len(activities) {
		activities = activities[:limit]
	}
	writeJSON(w, http.StatusOK, map[string]any{
		"code":    1,
		"message": "success",
		"data":    activities,
	})
}

// Published activities stay available to administrators immediately, but are
// exposed to app clients only during the 15 days leading up to registration
// close. Older records without a registration deadline use the activity start
// date so they remain compatible with the new visibility rule.
func activityVisibleToPublic(activity Activity, now time.Time) bool {
	deadline := strings.TrimSpace(activity.RegistrationDeadline)
	if deadline == "" { deadline = strings.TrimSpace(activity.StartDate) }
	deadline = strings.ReplaceAll(deadline, ".", "-")
	parsed, err := time.ParseInLocation("2006-01-02", deadline, now.Location())
	if err != nil { return true }
	today := time.Date(now.Year(), now.Month(), now.Day(), 0, 0, 0, 0, now.Location())
	return !today.Before(parsed.AddDate(0, 0, -activityRevealDays))
}

func (h *activityHandler) adminActivities(w http.ResponseWriter, r *http.Request) {
	if r.Method == http.MethodOptions {
		w.WriteHeader(http.StatusNoContent)
		return
	}
	actor, ok := h.authorize(w, r)
	if !ok { return }

	switch r.Method {
	case http.MethodGet:
		h.logAccountOperation("activity.read", actor, "全部活动")
		writeJSON(w, http.StatusOK, h.store.list(true))
	case http.MethodPost:
		activity, ok := decodeActivity(w, r)
		if !ok {
			return
		}
		activity.OwnerID = actor.ID
		if !actor.Super {
			activity.Organizer = actor.Name
			activity.Featured = false
			activity.SortOrder = 0
		}
		created, err := h.store.create(activity)
		if err != nil {
			http.Error(w, "Unable to save activity", http.StatusInternalServerError)
			return
		}
		h.logActivityOperation("activity.create", actor, created)
		writeJSON(w, http.StatusCreated, created)
	default:
		http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
	}
}

func (h *activityHandler) adminActivity(w http.ResponseWriter, r *http.Request) {
	if r.Method == http.MethodOptions {
		w.WriteHeader(http.StatusNoContent)
		return
	}
	actor, ok := h.authorize(w, r)
	if !ok { return }
	id := strings.TrimPrefix(r.URL.Path, "/admin/activities/")
	if id == "" || strings.Contains(id, "/") {
		http.Error(w, "Invalid activity id", http.StatusBadRequest)
		return
	}

	switch r.Method {
	case http.MethodPut:
		activity, ok := decodeActivity(w, r)
		if !ok {
			return
		}
		updated, err := h.store.update(id, activity, actor)
		if errors.Is(err, errActivityForbidden) { http.Error(w, "Forbidden", http.StatusForbidden); return }
		if errors.Is(err, os.ErrNotExist) {
			http.Error(w, "Activity not found", http.StatusNotFound)
			return
		}
		if err != nil {
			http.Error(w, "Unable to save activity", http.StatusInternalServerError)
			return
		}
		h.logActivityOperation("activity.update", actor, updated)
		writeJSON(w, http.StatusOK, updated)
	case http.MethodDelete:
		current, _ := h.store.get(id)
		deleted, err := h.store.delete(id, actor)
		if errors.Is(err, errActivityForbidden) { http.Error(w, "Forbidden", http.StatusForbidden); return }
		if err != nil {
			http.Error(w, "Unable to delete activity", http.StatusInternalServerError)
			return
		}
		if !deleted {
			http.Error(w, "Activity not found", http.StatusNotFound)
			return
		}
		h.logActivityOperation("activity.delete", actor, current)
		w.WriteHeader(http.StatusNoContent)
	default:
		http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
	}
}

func (h *activityHandler) uploadPoster(w http.ResponseWriter, r *http.Request) {
	if r.Method == http.MethodOptions {
		w.WriteHeader(http.StatusNoContent)
		return
	}
	if _, ok := h.authorize(w, r); !ok { return }
	if r.Method != http.MethodPost {
		http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
		return
	}

	r.Body = http.MaxBytesReader(w, r.Body, maxPosterUploadSize)
	if err := r.ParseMultipartForm(maxPosterUploadSize); err != nil {
		http.Error(w, "Poster must be smaller than 8 MB", http.StatusRequestEntityTooLarge)
		return
	}
	if r.MultipartForm != nil {
		defer r.MultipartForm.RemoveAll()
	}
	file, _, err := r.FormFile("poster")
	if err != nil {
		http.Error(w, "Missing poster file", http.StatusBadRequest)
		return
	}
	defer file.Close()

	data, err := io.ReadAll(io.LimitReader(file, maxStoredImageSize+1))
	if err != nil {
		http.Error(w, "Unable to read poster", http.StatusBadRequest)
		return
	}
	if len(data) > maxStoredImageSize {
		http.Error(w, "Compressed image must be smaller than 2 MB", http.StatusRequestEntityTooLarge)
		return
	}
	if len(data) == 0 {
		http.Error(w, "Image file is empty", http.StatusBadRequest)
		return
	}
	contentType := http.DetectContentType(data)
	extension, ok := posterExtensions[contentType]
	if !ok {
		http.Error(w, "Only PNG, JPEG and WebP posters are supported", http.StatusUnsupportedMediaType)
		return
	}
	width, height, err := activityImageDimensions(data, contentType)
	if err != nil {
		http.Error(w, "Unable to decode image dimensions", http.StatusBadRequest)
		return
	}
	if width > maxImageDimension || height > maxImageDimension || width*height > maxImagePixels {
		http.Error(w, "Image dimensions are too large", http.StatusRequestEntityTooLarge)
		return
	}

	name := randomID() + extension
	path := filepath.Join(h.dataDir, "posters", name)
	destination, err := os.OpenFile(path, os.O_CREATE|os.O_EXCL|os.O_WRONLY, 0644)
	if err != nil {
		http.Error(w, "Unable to save poster", http.StatusInternalServerError)
		return
	}
	defer destination.Close()
	if _, err := destination.Write(data); err != nil {
		os.Remove(path)
		http.Error(w, "Unable to save poster", http.StatusInternalServerError)
		return
	}

	writeJSON(w, http.StatusCreated, map[string]string{"url": "/activity-posters/" + name})
}

var posterExtensions = map[string]string{
	"image/png":  ".png",
	"image/jpeg": ".jpg",
	"image/webp": ".webp",
}

func activityImageDimensions(data []byte, contentType string) (int, int, error) {
	if contentType != "image/webp" {
		config, _, err := image.DecodeConfig(bytes.NewReader(data))
		if err != nil {
			return 0, 0, err
		}
		return config.Width, config.Height, nil
	}
	return webPDimensions(data)
}

func webPDimensions(data []byte) (int, int, error) {
	if len(data) < 30 || string(data[:4]) != "RIFF" || string(data[8:12]) != "WEBP" {
		return 0, 0, errors.New("invalid webp header")
	}
	for offset := 12; offset+8 <= len(data); {
		chunkType := string(data[offset : offset+4])
		chunkSize := int(binary.LittleEndian.Uint32(data[offset+4 : offset+8]))
		payload := offset + 8
		if chunkSize < 0 || payload+chunkSize > len(data) {
			return 0, 0, errors.New("invalid webp chunk")
		}
		switch chunkType {
		case "VP8X":
			if chunkSize >= 10 {
				width := 1 + int(data[payload+4]) + int(data[payload+5])<<8 + int(data[payload+6])<<16
				height := 1 + int(data[payload+7]) + int(data[payload+8])<<8 + int(data[payload+9])<<16
				return width, height, nil
			}
		case "VP8L":
			if chunkSize >= 5 && data[payload] == 0x2f {
				bits := binary.LittleEndian.Uint32(data[payload+1 : payload+5])
				return int(bits&0x3fff) + 1, int((bits>>14)&0x3fff) + 1, nil
			}
		case "VP8 ":
			if chunkSize >= 10 && bytes.Equal(data[payload+3:payload+6], []byte{0x9d, 0x01, 0x2a}) {
				width := int(binary.LittleEndian.Uint16(data[payload+6:payload+8]) & 0x3fff)
				height := int(binary.LittleEndian.Uint16(data[payload+8:payload+10]) & 0x3fff)
				return width, height, nil
			}
		}
		offset = payload + chunkSize + chunkSize%2
	}
	return 0, 0, errors.New("webp dimensions not found")
}

func decodeActivity(w http.ResponseWriter, r *http.Request) (Activity, bool) {
	r.Body = http.MaxBytesReader(w, r.Body, maxActivityBodySize)
	var activity Activity
	decoder := json.NewDecoder(r.Body)
	decoder.DisallowUnknownFields()
	if err := decoder.Decode(&activity); err != nil {
		http.Error(w, "Invalid activity payload", http.StatusBadRequest)
		return Activity{}, false
	}
	activity.Title = strings.TrimSpace(activity.Title)
	activity.StartDate = strings.TrimSpace(activity.StartDate)
	activity.EndDate = strings.TrimSpace(activity.EndDate)
	activity.RegistrationDeadline = strings.TrimSpace(activity.RegistrationDeadline)
	if activity.Title == "" || activity.StartDate == "" || activity.EndDate == "" {
		http.Error(w, "title, startDate and endDate are required", http.StatusBadRequest)
		return Activity{}, false
	}
	if activity.EndDate < activity.StartDate {
		http.Error(w, "endDate cannot be earlier than startDate", http.StatusBadRequest)
		return Activity{}, false
	}
	if activity.RegistrationDeadline != "" && len(activity.RegistrationDeadline) != 10 {
		http.Error(w, "registrationDeadline must use YYYY-MM-DD", http.StatusBadRequest)
		return Activity{}, false
	}
	if len(activity.Images) > maxActivityImages {
		http.Error(w, "an activity can contain at most 9 images", http.StatusBadRequest)
		return Activity{}, false
	}
	activity.FilterTags = normalizeActivityFilters(activity.FilterTags)
	if activity.PosterTitle == "" {
		activity.PosterTitle = activity.Title
	}
	return activity, true
}

func (h *activityHandler) authorize(w http.ResponseWriter, r *http.Request) (activityAccount, bool) {
	if h.accounts != nil {
		if account, ok := h.accounts.authenticate(r); ok { return account, true }
	}
	http.Error(w, "Unauthorized", http.StatusUnauthorized)
	return activityAccount{}, false
}

func (h *activityHandler) withCORS(next http.HandlerFunc) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Access-Control-Allow-Origin", h.adminOrigin)
		w.Header().Set("Access-Control-Allow-Headers", "Authorization, Content-Type")
		w.Header().Set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
		next(w, r)
	}
}

func writeJSON(w http.ResponseWriter, status int, value any) {
	w.Header().Set("Content-Type", "application/json; charset=utf-8")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(value)
}

func randomID() string {
	buffer := make([]byte, 12)
	if _, err := rand.Read(buffer); err != nil {
		return strconv.FormatInt(time.Now().UnixNano(), 36)
	}
	return hex.EncodeToString(buffer)
}

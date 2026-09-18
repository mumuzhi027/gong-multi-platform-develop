package main

import (
	"encoding/json"
	"errors"
	"net/http"
	"os"
	"path/filepath"
	"sort"
	"strings"
	"sync"
	"time"
)

const userStatsDateLayout = "2006-01-02"

type userUsageRecord struct {
	FirstSeen  time.Time      `json:"firstSeen"`
	LastSeen   time.Time      `json:"lastSeen"`
	LoginCount int64          `json:"loginCount"`
	Daily      map[string]int `json:"daily"`
}

type persistedUserStats struct {
	TrackingStartedAt time.Time                  `json:"trackingStartedAt"`
	Users             map[string]userUsageRecord `json:"users"`
}

type persistedUserList struct {
	UpdatedAt time.Time       `json:"updatedAt"`
	Count     int             `json:"count"`
	Users     []userListEntry `json:"users"`
}

type userListEntry struct {
	Account    string    `json:"account"`
	FirstSeen  time.Time `json:"firstSeen"`
	LastSeen   time.Time `json:"lastSeen"`
	LoginCount int64     `json:"loginCount"`
}

type userStatsStore struct {
	mu           sync.RWMutex
	filePath     string
	userListPath string
	data         persistedUserStats
}

type userStatsDay struct {
	Date        string `json:"date"`
	NewUsers    int    `json:"newUsers"`
	ActiveUsers int    `json:"activeUsers"`
	LoginCount  int64  `json:"loginCount"`
}

type userStatsSnapshot struct {
	From                  time.Time      `json:"from"`
	To                    time.Time      `json:"to"`
	TrackingStartedAt     time.Time      `json:"trackingStartedAt"`
	TotalUsers            int            `json:"totalUsers"`
	AccumulatedBeforeFrom int            `json:"accumulatedBeforeFrom"`
	NewUsers              int            `json:"newUsers"`
	ActiveUsers           int            `json:"activeUsers"`
	LoginCount            int64          `json:"loginCount"`
	Daily                 []userStatsDay `json:"daily"`
}

func newUserStatsStore(dataDir string) (*userStatsStore, error) {
	if err := os.MkdirAll(dataDir, 0755); err != nil {
		return nil, err
	}
	store := &userStatsStore{
		filePath:     filepath.Join(dataDir, "user-stats.json"),
		userListPath: filepath.Join(dataDir, "user-list.json"),
		data: persistedUserStats{
			TrackingStartedAt: time.Now(),
			Users:             make(map[string]userUsageRecord),
		},
	}
	if err := store.load(); err != nil {
		return nil, err
	}
	// Refresh both persistent files on startup so the readable user list is
	// always present and can recover the accumulated count if the main file fails.
	if err := store.writeLocked(); err != nil {
		return nil, err
	}
	return store, nil
}

func (s *userStatsStore) load() error {
	file, err := os.Open(s.filePath)
	if errors.Is(err, os.ErrNotExist) {
		if recoveryErr := s.loadUserList(); errors.Is(recoveryErr, os.ErrNotExist) {
			return nil
		} else {
			return recoveryErr
		}
	}
	if err != nil {
		return err
	}
	decodeErr := json.NewDecoder(file).Decode(&s.data)
	closeErr := file.Close()
	if decodeErr != nil {
		if recoveryErr := s.loadUserList(); recoveryErr == nil {
			return s.writeLocked()
		}
		return decodeErr
	}
	if closeErr != nil {
		return closeErr
	}
	if s.data.Users == nil {
		s.data.Users = make(map[string]userUsageRecord)
	}
	if s.data.TrackingStartedAt.IsZero() {
		s.data.TrackingStartedAt = time.Now()
	}
	return nil
}

func (s *userStatsStore) loadUserList() error {
	file, err := os.Open(s.userListPath)
	if err != nil {
		return err
	}
	defer file.Close()
	var persisted persistedUserList
	if err := json.NewDecoder(file).Decode(&persisted); err != nil {
		return err
	}
	users := make(map[string]userUsageRecord, len(persisted.Users))
	var trackingStartedAt time.Time
	for _, item := range persisted.Users {
		account := sanitizeAuditAccount(item.Account)
		if account == "" {
			continue
		}
		users[account] = userUsageRecord{
			FirstSeen:  item.FirstSeen,
			LastSeen:   item.LastSeen,
			LoginCount: item.LoginCount,
			Daily:      make(map[string]int),
		}
		if trackingStartedAt.IsZero() || (!item.FirstSeen.IsZero() && item.FirstSeen.Before(trackingStartedAt)) {
			trackingStartedAt = item.FirstSeen
		}
	}
	if trackingStartedAt.IsZero() {
		trackingStartedAt = time.Now()
	}
	s.data = persistedUserStats{TrackingStartedAt: trackingStartedAt, Users: users}
	return nil
}

func (s *userStatsStore) recordSuccessfulLogin(account string, at time.Time) error {
	account = sanitizeAuditAccount(account)
	if account == "" {
		return nil
	}
	at = at.In(time.Local)
	s.mu.Lock()
	defer s.mu.Unlock()
	record, exists := s.data.Users[account]
	if !exists {
		record.FirstSeen = at
		record.Daily = make(map[string]int)
	}
	if record.Daily == nil {
		record.Daily = make(map[string]int)
	}
	record.LastSeen = at
	record.LoginCount++
	record.Daily[at.Format(userStatsDateLayout)]++
	s.data.Users[account] = record
	return s.writeLocked()
}

func (s *userStatsStore) writeLocked() error {
	if err := writeJSONAtomically(s.filePath, "user-stats-*.json", s.data); err != nil {
		return err
	}
	return writeJSONAtomically(s.userListPath, "user-list-*.json", s.userListLocked())
}

func (s *userStatsStore) userListLocked() persistedUserList {
	users := make([]userListEntry, 0, len(s.data.Users))
	for account, record := range s.data.Users {
		users = append(users, userListEntry{
			Account:    account,
			FirstSeen:  record.FirstSeen,
			LastSeen:   record.LastSeen,
			LoginCount: record.LoginCount,
		})
	}
	sort.Slice(users, func(i, j int) bool { return users[i].Account < users[j].Account })
	return persistedUserList{UpdatedAt: time.Now(), Count: len(users), Users: users}
}

func writeJSONAtomically(path, pattern string, value any) error {
	temp, err := os.CreateTemp(filepath.Dir(path), pattern)
	if err != nil {
		return err
	}
	tempPath := temp.Name()
	defer os.Remove(tempPath)
	encoder := json.NewEncoder(temp)
	encoder.SetIndent("", "  ")
	if err := encoder.Encode(value); err != nil {
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
	if err := os.Chmod(tempPath, 0640); err != nil {
		return err
	}
	return os.Rename(tempPath, path)
}

func (s *userStatsStore) snapshot(from, to time.Time) userStatsSnapshot {
	s.mu.RLock()
	defer s.mu.RUnlock()
	days := make(map[string]*userStatsDay)
	active := make(map[string]struct{})
	result := userStatsSnapshot{From: from, To: to, TrackingStartedAt: s.data.TrackingStartedAt}
	for account, record := range s.data.Users {
		if record.FirstSeen.Before(to) {
			result.TotalUsers++
		}
		if record.FirstSeen.Before(from) {
			result.AccumulatedBeforeFrom++
		} else if record.FirstSeen.Before(to) {
			result.NewUsers++
			day := record.FirstSeen.Format(userStatsDateLayout)
			entry := ensureUserStatsDay(days, day)
			entry.NewUsers++
		}
		for day, count := range record.Daily {
			dayTime, err := time.ParseInLocation(userStatsDateLayout, day, time.Local)
			if err != nil || dayTime.Before(from) || !dayTime.Before(to) {
				continue
			}
			active[account] = struct{}{}
			entry := ensureUserStatsDay(days, day)
			entry.ActiveUsers++
			entry.LoginCount += int64(count)
			result.LoginCount += int64(count)
		}
	}
	result.ActiveUsers = len(active)
	for _, entry := range days {
		result.Daily = append(result.Daily, *entry)
	}
	sort.Slice(result.Daily, func(i, j int) bool { return result.Daily[i].Date < result.Daily[j].Date })
	return result
}

func (s *userStatsStore) trackingStartedAt() time.Time {
	s.mu.RLock()
	defer s.mu.RUnlock()
	return s.data.TrackingStartedAt
}

func ensureUserStatsDay(days map[string]*userStatsDay, day string) *userStatsDay {
	if days[day] == nil {
		days[day] = &userStatsDay{Date: day}
	}
	return days[day]
}

func parseUserStatsRange(r *http.Request, startedAt time.Time) (time.Time, time.Time, error) {
	now := time.Now()
	startedAt = startedAt.In(time.Local)
	from := time.Date(startedAt.Year(), startedAt.Month(), startedAt.Day(), 0, 0, 0, 0, time.Local)
	to := now
	if raw := strings.TrimSpace(r.URL.Query().Get("from")); raw != "" {
		parsed, err := time.ParseInLocation(userStatsDateLayout, raw, time.Local)
		if err != nil {
			return time.Time{}, time.Time{}, err
		}
		from = parsed
	}
	if raw := strings.TrimSpace(r.URL.Query().Get("to")); raw != "" {
		parsed, err := time.ParseInLocation(userStatsDateLayout, raw, time.Local)
		if err != nil {
			return time.Time{}, time.Time{}, err
		}
		to = parsed.AddDate(0, 0, 1)
	}
	if !from.Before(to) {
		return time.Time{}, time.Time{}, errors.New("from must be earlier than or equal to to")
	}
	return from, to, nil
}

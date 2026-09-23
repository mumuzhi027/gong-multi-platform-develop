package main

import (
	"crypto/hmac"
	"crypto/rand"
	"crypto/sha256"
	"crypto/subtle"
	"encoding/hex"
	"encoding/json"
	"errors"
	"net/http"
	"os"
	"path/filepath"
	"strings"
	"sync"
	"time"
)

const superActivityAccount = "三翼青融媒体中心"

type activityAccount struct {
	ID           string    `json:"id"`
	Name         string    `json:"name"`
	PasswordSalt string    `json:"passwordSalt,omitempty"`
	PasswordHash string    `json:"passwordHash,omitempty"`
	Super        bool      `json:"super"`
	CreatedAt    time.Time `json:"createdAt"`
}

type activityAccountPublic struct {
	ID        string    `json:"id"`
	Name      string    `json:"name"`
	Super     bool      `json:"super"`
	CreatedAt time.Time `json:"createdAt"`
}

func (a activityAccount) public() activityAccountPublic {
	return activityAccountPublic{a.ID, a.Name, a.Super, a.CreatedAt}
}

type activitySession struct {
	accountID string
	expires   time.Time
}

type activityAccountStore struct {
	mu       sync.Mutex
	path     string
	accounts map[string]activityAccount
	sessions map[string]activitySession
}

func newActivityAccountStore(dataDir string) (*activityAccountStore, error) {
	s := &activityAccountStore{
		path: filepath.Join(dataDir, "admin-accounts.json"),
		accounts: make(map[string]activityAccount),
		sessions: make(map[string]activitySession),
	}
	data, err := os.ReadFile(s.path)
	if err == nil {
		var accounts []activityAccount
		if err = json.Unmarshal(data, &accounts); err != nil { return nil, err }
		for _, account := range accounts { s.accounts[account.ID] = account }
	} else if !errors.Is(err, os.ErrNotExist) { return nil, err }
	// Administrator credentials are injected via the deployment environment.
	// The environment is applied on every start, so changing a password in .env
	// and restarting the proxy immediately rotates that account's password.
	password := os.Getenv("ACTIVITY_SUPERADMIN_PASSWORD")
	if password == "" { password = os.Getenv("ACTIVITY_ADMIN_TOKEN") }
	if password == "" { return nil, errors.New("ACTIVITY_SUPERADMIN_PASSWORD is required") }
	superName := strings.TrimSpace(os.Getenv("ACTIVITY_SUPERADMIN_NAME"))
	if superName == "" { superName = superActivityAccount }
	if err := validateActivityAccountCredentials(superName, password); err != nil {
		return nil, errors.New("invalid ACTIVITY_SUPERADMIN_NAME or ACTIVITY_SUPERADMIN_PASSWORD")
	}
	configured := map[string]string{superName: password}
	if raw := strings.TrimSpace(os.Getenv("ACTIVITY_ADMIN_ACCOUNTS_JSON")); raw != "" {
		var accounts map[string]string
		if err := json.Unmarshal([]byte(raw), &accounts); err != nil {
			return nil, errors.New("ACTIVITY_ADMIN_ACCOUNTS_JSON must be a JSON object of account names to passwords")
		}
		for name, accountPassword := range accounts {
			name = strings.TrimSpace(name)
			if name == superName {
				return nil, errors.New("configure the super administrator with ACTIVITY_SUPERADMIN_PASSWORD")
			}
			if err := validateActivityAccountCredentials(name, accountPassword); err != nil {
				return nil, errors.New("invalid account in ACTIVITY_ADMIN_ACCOUNTS_JSON: " + name)
			}
			configured[name] = accountPassword
		}
	}
	for name, account := range s.accounts {
		account.Super = false
		s.accounts[name] = account
	}
	for name, accountPassword := range configured {
		account, exists := s.accounts[name]
		if !exists {
			account = activityAccount{ID: name, Name: name, CreatedAt: time.Now().UTC()}
		}
		account.Super = name == superName
		account.PasswordSalt, account.PasswordHash = activityPassword(accountPassword)
		s.accounts[name] = account
	}
	if err := s.writeLocked(); err != nil { return nil, err }
	return s, nil
}

func validateActivityAccountCredentials(name, password string) error {
	if name == "" || strings.ContainsAny(name, "/\\\r\n") || len([]rune(name)) > 40 || len(password) < 10 || len(password) > 256 {
		return errors.New("invalid account name or password")
	}
	return nil
}

func activityPassword(password string) (string, string) {
	salt := make([]byte, 24)
	if _, err := rand.Read(salt); err != nil { panic(err) }
	return hex.EncodeToString(salt), hex.EncodeToString(activityPasswordHash(password, salt))
}

// PBKDF2-HMAC-SHA256, 240k iterations. Passwords are never stored as plaintext.
func activityPasswordHash(password string, salt []byte) []byte {
	block := append(append([]byte(nil), salt...), 0, 0, 0, 1)
	mac := hmac.New(sha256.New, []byte(password))
	mac.Write(block)
	u := mac.Sum(nil)
	result := append([]byte(nil), u...)
	for i := 1; i < 240000; i++ {
		mac.Reset()
		mac.Write(u)
		u = mac.Sum(nil)
		for j := range result { result[j] ^= u[j] }
	}
	return result
}

func (a activityAccount) verify(password string) bool {
	salt, err := hex.DecodeString(a.PasswordSalt)
	if err != nil { return false }
	want, err := hex.DecodeString(a.PasswordHash)
	if err != nil { return false }
	got := activityPasswordHash(password, salt)
	return len(got) == len(want) && subtle.ConstantTimeCompare(got, want) == 1
}

func (s *activityAccountStore) login(name, password string) (string, activityAccountPublic, bool) {
	s.mu.Lock()
	defer s.mu.Unlock()
	a, ok := s.accounts[name]
	if !ok || !a.verify(password) { return "", activityAccountPublic{}, false }
	raw := randomID() + randomID()
	digest := sha256.Sum256([]byte(raw))
	s.sessions[hex.EncodeToString(digest[:])] = activitySession{a.ID, time.Now().Add(24 * time.Hour)}
	return raw, a.public(), true
}

func (s *activityAccountStore) authenticate(r *http.Request) (activityAccount, bool) {
	authorization := r.Header.Get("Authorization")
	if !strings.HasPrefix(authorization, "Bearer ") { return activityAccount{}, false }
	raw := strings.TrimPrefix(authorization, "Bearer ")
	digest := sha256.Sum256([]byte(raw))
	s.mu.Lock()
	defer s.mu.Unlock()
	session, ok := s.sessions[hex.EncodeToString(digest[:])]
	if !ok || time.Now().After(session.expires) { return activityAccount{}, false }
	a, ok := s.accounts[session.accountID]
	return a, ok
}

func (s *activityAccountStore) logout(r *http.Request) {
	raw := strings.TrimPrefix(r.Header.Get("Authorization"), "Bearer ")
	digest := sha256.Sum256([]byte(raw))
	s.mu.Lock()
	delete(s.sessions, hex.EncodeToString(digest[:]))
	s.mu.Unlock()
}

func (s *activityAccountStore) list() []activityAccountPublic {
	s.mu.Lock()
	defer s.mu.Unlock()
	result := make([]activityAccountPublic, 0, len(s.accounts))
	for _, account := range s.accounts { result = append(result, account.public()) }
	return result
}

func (s *activityAccountStore) find(name string) (activityAccountPublic, bool) {
	s.mu.Lock()
	defer s.mu.Unlock()
	account, ok := s.accounts[name]
	return account.public(), ok
}

func (s *activityAccountStore) put(name, password string) (activityAccountPublic, error) {
	name = strings.TrimSpace(name)
	if err := validateActivityAccountCredentials(name, password); err != nil { return activityAccountPublic{}, err }
	s.mu.Lock()
	defer s.mu.Unlock()
	old, exists := s.accounts[name]
	if !exists { old = activityAccount{ID: name, Name: name, CreatedAt: time.Now().UTC()} }
	previous := old
	old.PasswordSalt, old.PasswordHash = activityPassword(password)
	s.accounts[name] = old
	if err := s.writeLocked(); err != nil {
		if exists { s.accounts[name] = previous } else { delete(s.accounts, name) }
		return activityAccountPublic{}, err
	}
	for token, session := range s.sessions { if session.accountID == name { delete(s.sessions, token) } }
	return old.public(), nil
}

func (s *activityAccountStore) delete(name string) error {
	s.mu.Lock()
	defer s.mu.Unlock()
	old, ok := s.accounts[name]
	if !ok { return os.ErrNotExist }
	if old.Super { return errors.New("cannot delete super account") }
	delete(s.accounts, name)
	if err := s.writeLocked(); err != nil { s.accounts[name] = old; return err }
	for token, session := range s.sessions { if session.accountID == name { delete(s.sessions, token) } }
	return nil
}

func (s *activityAccountStore) writeLocked() error {
	items := make([]activityAccount, 0, len(s.accounts))
	for _, account := range s.accounts { items = append(items, account) }
	return writeActivityJSONFile(s.path, items)
}

func writeActivityJSONFile(path string, value any) error {
	file, err := os.CreateTemp(filepath.Dir(path), "activity-admin-*.json")
	if err != nil { return err }
	defer os.Remove(file.Name())
	if err := file.Chmod(0600); err != nil { file.Close(); return err }
	if err := json.NewEncoder(file).Encode(value); err != nil { file.Close(); return err }
	if err := file.Sync(); err != nil { file.Close(); return err }
	if err := file.Close(); err != nil { return err }
	return os.Rename(file.Name(), path)
}

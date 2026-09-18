package gongai

import (
	"crypto/aes"
	"crypto/cipher"
	"crypto/rand"
	"crypto/sha256"
	"encoding/base64"
	"errors"
	"io"
	"sync"
	"time"
)

type ProviderSession struct {
	Provider     string    `json:"provider"`
	AccessToken  string    `json:"access_token"`
	RefreshToken string    `json:"refresh_token,omitempty"`
	ExpiresAt    time.Time `json:"expires_at"`
}

type encryptedSession struct {
	nonce      []byte
	ciphertext []byte
	expiresAt  time.Time
}

type SessionVault struct {
	mu       sync.Mutex
	aead     cipher.AEAD
	sessions map[string]encryptedSession
}

func NewSessionVault(masterKey string) (*SessionVault, error) {
	if masterKey == "" {
		return nil, errors.New("SESSION_VAULT_MASTER_KEY is required")
	}
	decoded, err := base64.StdEncoding.DecodeString(masterKey)
	if err != nil || len(decoded) < 32 {
		hash := sha256.Sum256([]byte(masterKey))
		decoded = hash[:]
	}
	block, err := aes.NewCipher(decoded[:32])
	if err != nil {
		return nil, err
	}
	aead, err := cipher.NewGCM(block)
	if err != nil {
		return nil, err
	}
	return &SessionVault{aead: aead, sessions: make(map[string]encryptedSession)}, nil
}

func vaultKey(userID, provider string) string { return userID + "\x00" + provider }

func (v *SessionVault) Put(userID string, session ProviderSession) error {
	if userID == "" || session.Provider == "" || session.AccessToken == "" {
		return errors.New("invalid provider session")
	}
	nonce := make([]byte, v.aead.NonceSize())
	if _, err := io.ReadFull(rand.Reader, nonce); err != nil {
		return err
	}
	plain := []byte(session.AccessToken + "\x00" + session.RefreshToken)
	aad := []byte(vaultKey(userID, session.Provider))
	ciphertext := v.aead.Seal(nil, nonce, plain, aad)
	v.mu.Lock()
	v.sessions[vaultKey(userID, session.Provider)] = encryptedSession{nonce: nonce, ciphertext: ciphertext, expiresAt: session.ExpiresAt}
	v.mu.Unlock()
	return nil
}

func (v *SessionVault) Get(userID, provider string) (ProviderSession, error) {
	key := vaultKey(userID, provider)
	v.mu.Lock()
	stored, ok := v.sessions[key]
	if ok && !stored.expiresAt.IsZero() && time.Now().UTC().After(stored.expiresAt) {
		delete(v.sessions, key)
		ok = false
	}
	v.mu.Unlock()
	if !ok {
		return ProviderSession{}, errors.New("provider session not found or expired")
	}
	plain, err := v.aead.Open(nil, stored.nonce, stored.ciphertext, []byte(key))
	if err != nil {
		return ProviderSession{}, err
	}
	parts := splitNUL(string(plain))
	return ProviderSession{Provider: provider, AccessToken: parts[0], RefreshToken: parts[1], ExpiresAt: stored.expiresAt}, nil
}

func splitNUL(value string) [2]string {
	for i := range value {
		if value[i] == 0 {
			return [2]string{value[:i], value[i+1:]}
		}
	}
	return [2]string{value, ""}
}

func (v *SessionVault) RevokeAll(userID string) {
	v.mu.Lock()
	defer v.mu.Unlock()
	prefix := userID + "\x00"
	for key := range v.sessions {
		if len(key) >= len(prefix) && key[:len(prefix)] == prefix {
			delete(v.sessions, key)
		}
	}
}

type StoredAttachment struct {
	ID                 string
	OwnerUserID        string
	Purpose            string
	MediaType          string
	UpstreamProvider   string
	EncryptedReference []byte
	Uploaded           bool
	CreatedAt          time.Time
}

type AttachmentStore struct {
	mu    sync.Mutex
	items map[string]StoredAttachment
}

func NewAttachmentStore() *AttachmentStore {
	return &AttachmentStore{items: make(map[string]StoredAttachment)}
}

func (s *AttachmentStore) Put(owner, purpose, mediaType, provider string, encryptedRef []byte) (StoredAttachment, error) {
	id, err := randomID()
	if err != nil {
		return StoredAttachment{}, err
	}
	item := StoredAttachment{ID: id, OwnerUserID: owner, Purpose: purpose, MediaType: mediaType, UpstreamProvider: provider, EncryptedReference: append([]byte(nil), encryptedRef...), Uploaded: len(encryptedRef) > 0, CreatedAt: time.Now().UTC()}
	s.mu.Lock()
	s.items[id] = item
	s.mu.Unlock()
	return item, nil
}

func (s *AttachmentStore) Get(owner, id string) (StoredAttachment, error) {
	s.mu.Lock()
	defer s.mu.Unlock()
	item, ok := s.items[id]
	if !ok || item.OwnerUserID != owner {
		return StoredAttachment{}, errors.New("attachment not found for current user")
	}
	return item, nil
}

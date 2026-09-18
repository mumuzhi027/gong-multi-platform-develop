package gongai

import "time"

type ErrorCode string

const (
	CodeOK                          ErrorCode = "OK"
	CodeAuthRequired                ErrorCode = "AUTH_REQUIRED"
	CodeProviderSessionExpired      ErrorCode = "PROVIDER_SESSION_EXPIRED"
	CodeValidationFailed            ErrorCode = "VALIDATION_FAILED"
	CodeMissingRequiredField        ErrorCode = "MISSING_REQUIRED_FIELD"
	CodePracticalRequirementMissing ErrorCode = "PRACTICAL_REQUIREMENT_MISSING"
	CodeConfirmationRequired        ErrorCode = "CONFIRMATION_REQUIRED"
	CodeConfirmationExpired         ErrorCode = "CONFIRMATION_EXPIRED"
	CodeConfirmationMismatch        ErrorCode = "CONFIRMATION_MISMATCH"
	CodeUpstreamRejected            ErrorCode = "UPSTREAM_REJECTED"
	CodeUpstreamConflict            ErrorCode = "UPSTREAM_CONFLICT"
	CodeUpstreamTimeout             ErrorCode = "UPSTREAM_TIMEOUT"
	CodeVerifyFailed                ErrorCode = "VERIFY_FAILED"
	CodeRateLimited                 ErrorCode = "RATE_LIMITED"
	CodePermissionDenied            ErrorCode = "PERMISSION_DENIED"
	CodeNotSupported                ErrorCode = "NOT_SUPPORTED"
	CodeInternalError               ErrorCode = "INTERNAL_ERROR"
)

type ChatRequest struct {
	ConversationID string          `json:"conversationId"`
	Message        string          `json:"message"`
	Attachments    []AttachmentRef `json:"attachments,omitempty"`
}

type AttachmentRef struct {
	ID        string `json:"id"`
	Purpose   string `json:"purpose"`
	MediaType string `json:"mediaType"`
}

type ChatResponse struct {
	ConversationID string      `json:"conversationId"`
	Blocks         []ChatBlock `json:"blocks"`
	WorkflowState  string      `json:"workflowState"`
}

type ChatBlock struct {
	Type        string         `json:"type"`
	Text        string         `json:"text,omitempty"`
	Title       string         `json:"title,omitempty"`
	Items       []CardItem     `json:"items,omitempty"`
	Fields      []PreviewField `json:"fields,omitempty"`
	Warnings    []string       `json:"warnings,omitempty"`
	ActionID    string         `json:"actionId,omitempty"`
	ConfirmText string         `json:"confirmText,omitempty"`
	CancelText  string         `json:"cancelText,omitempty"`
	ExpiresAt   string         `json:"expiresAt,omitempty"`
	Status      string         `json:"status,omitempty"`
}

type CardItem struct {
	Title    string            `json:"title"`
	Subtitle string            `json:"subtitle,omitempty"`
	Meta     map[string]string `json:"meta,omitempty"`
}

type PreviewField struct {
	Label     string `json:"label"`
	Value     string `json:"value"`
	Sensitive bool   `json:"sensitive,omitempty"`
}

type ToolResult struct {
	OK           bool         `json:"ok"`
	Code         ErrorCode    `json:"code"`
	Data         any          `json:"data,omitempty"`
	UserMessage  string       `json:"userMessage,omitempty"`
	Retryable    bool         `json:"retryable,omitempty"`
	ProviderMeta ProviderMeta `json:"providerMeta"`
	Blocks       []ChatBlock  `json:"-"`
}

type ProviderMeta struct {
	Provider       string `json:"provider"`
	UpstreamStatus string `json:"upstreamStatus,omitempty"`
}

type Capability struct {
	ID           string
	Provider     string
	Risk         string
	ReadOnly     bool
	Confirmation string
	Privacy      PrivacyLevel
}

type PrivacyLevel string

const (
	PrivacyPublic     PrivacyLevel = "L0"
	PrivacyPersonal   PrivacyLevel = "L1"
	PrivacySensitive  PrivacyLevel = "L2"
	PrivacyCredential PrivacyLevel = "L3"
)

type ActionPreview struct {
	Title       string         `json:"title"`
	Fields      []PreviewField `json:"fields"`
	Warnings    []string       `json:"warnings"`
	ConfirmText string         `json:"confirmText"`
	CancelText  string         `json:"cancelText"`
}

type PendingAction struct {
	ActionID              string         `json:"actionId"`
	ConversationID        string         `json:"-"`
	UserID                string         `json:"-"`
	Capability            string         `json:"capability"`
	Provider              string         `json:"provider"`
	Params                map[string]any `json:"-"`
	ParamsHash            string         `json:"-"`
	Preview               ActionPreview  `json:"preview"`
	Status                string         `json:"status"`
	ExpiresAt             time.Time      `json:"expiresAt"`
	CreatedAt             time.Time      `json:"createdAt"`
	RequiredConfirmations int            `json:"-"`
	ConfirmationCount     int            `json:"-"`
}

type ActionReceipt struct {
	ActionID         string `json:"actionId"`
	Capability       string `json:"capability"`
	Status           string `json:"status"`
	Summary          string `json:"summary"`
	UpstreamEntityID string `json:"upstreamEntityId,omitempty"`
	VerifiedAt       string `json:"verifiedAt"`
}

type ConfirmResponse struct {
	Block ChatBlock `json:"block"`
}

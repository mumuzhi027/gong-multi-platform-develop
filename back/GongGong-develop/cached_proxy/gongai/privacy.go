package gongai

import (
	"encoding/json"
	"errors"
	"regexp"
	"strings"
)

var credentialPattern = regexp.MustCompile(`(?i)(password|cookie|authorization|access[_-]?token|refresh[_-]?token|jsessionid|csrf)`)

type PrivacyGateway struct{}

func (PrivacyGateway) ValidateModelInput(value any) error {
	data, err := json.Marshal(value)
	if err != nil {
		return err
	}
	if credentialPattern.Match(data) {
		return errors.New("credential-like data is forbidden in model input")
	}
	return nil
}

func (PrivacyGateway) SanitizeText(value string) string {
	if credentialPattern.MatchString(value) {
		return "[敏感凭据已移除]"
	}
	return strings.TrimSpace(value)
}

func MaskPhone(value string) string {
	runes := []rune(strings.TrimSpace(value))
	if len(runes) < 7 {
		return "****"
	}
	return string(runes[:3]) + "****" + string(runes[len(runes)-4:])
}

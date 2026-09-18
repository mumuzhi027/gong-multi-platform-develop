package gongai

type Registry struct {
	items map[string]Capability
}

func NewRegistry() *Registry {
	capabilities := []Capability{
		{ID: "academic.schedule.query", Provider: "xtu_academic", Risk: "low", ReadOnly: true, Privacy: PrivacyPersonal},
		{ID: "academic.grade.query", Provider: "xtu_academic", Risk: "medium", ReadOnly: true, Privacy: PrivacySensitive},
		{ID: "academic.exam.query", Provider: "xtu_academic", Risk: "low", ReadOnly: true, Privacy: PrivacyPersonal},
		{ID: "room.search", Provider: "xtu_academic", Risk: "low", ReadOnly: true, Privacy: PrivacyPublic},
		{ID: "room.reserve", Provider: "xtu_academic", Risk: "high", ReadOnly: false, Confirmation: "explicit", Privacy: PrivacySensitive},
		{ID: "leave.apply", Provider: "xtu_extracurricular", Risk: "high", ReadOnly: false, Confirmation: "explicit", Privacy: PrivacySensitive},
		{ID: "leave.status", Provider: "xtu_extracurricular", Risk: "medium", ReadOnly: true, Privacy: PrivacySensitive},
		{ID: "leave.withdraw", Provider: "xtu_extracurricular", Risk: "high", ReadOnly: false, Confirmation: "explicit", Privacy: PrivacySensitive},
	}
	items := make(map[string]Capability, len(capabilities))
	for _, capability := range capabilities {
		items[capability.ID] = capability
	}
	return &Registry{items: items}
}

func (r *Registry) Get(id string) (Capability, bool) {
	capability, ok := r.items[id]
	return capability, ok
}

func (r *Registry) PublicModelCatalog() []map[string]string {
	result := make([]map[string]string, 0, len(r.items))
	for _, capability := range r.items {
		result = append(result, map[string]string{
			"id":       capability.ID,
			"readOnly": map[bool]string{true: "true", false: "false"}[capability.ReadOnly],
		})
	}
	return result
}

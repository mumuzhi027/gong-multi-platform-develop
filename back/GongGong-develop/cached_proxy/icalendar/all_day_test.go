package icalendar

import (
	"strings"
	"testing"
	"time"
)

func TestAllDayEventPreservesDateWithoutInventingClockTime(t *testing.T) {
	event := IcsEvent{}
	event.SetAllDay(true)
	event.SetStart(time.Date(2026, 10, 26, 0, 0, 0, 0, time.Local))
	event.SetEnd(time.Date(2026, 10, 27, 0, 0, 0, 0, time.Local))
	ics := event.ToIcs(nil)
	if !strings.Contains(ics, "DTSTART;VALUE=DATE:20261026") || !strings.Contains(ics, "DTEND;VALUE=DATE:20261027") {
		t.Fatalf("Missing date-only bounds: %s", ics)
	}
}

package main

import (
	"cached_proxy/feign"
	"strings"
	"testing"
)

func TestExperimentalCalendarUnknownTwelfthPeriod(t *testing.T) {
	calendar := &feign.TeachingCalendar{Start: "2026-08-03", Weeks: 20}
	course := feign.Course{Name: "模拟电子技术实验", IsExperiment: true, ExperimentProject: "晶体管实验", ExperimentGroup: "分组1", Day: "Monday", Weeks: "13", StartTime: 11, Duration: 2}
	ics := CoursesConvertCalendar(&feign.CourseList{Courses: []feign.Course{course}}, calendar).ToIcs(nil)
	for _, expected := range []string{"SUMMARY:【实验课】 模拟电子技术实验", "DTSTART;VALUE=DATE:20261026", "DTEND;VALUE=DATE:20261027", "第11-12节", "晶体管实验", "分组1"} {
		if !strings.Contains(ics, expected) {
			t.Fatalf("missing %q in calendar: %s", expected, ics)
		}
	}
	if strings.Contains(ics, "BEGIN:VALARM") {
		t.Fatal("unknown clock time must not create a timed reminder")
	}

	course.StartTime, course.Duration = 1, 2
	ics = CoursesConvertCalendar(&feign.CourseList{Courses: []feign.Course{course}}, calendar).ToIcs(nil)
	if strings.Contains(ics, "DTSTART;VALUE=DATE") || !strings.Contains(ics, "BEGIN:VALARM") {
		t.Fatal("known periods must retain their clock times and reminders")
	}
}

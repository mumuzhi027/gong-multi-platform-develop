package feign

import (
	"reflect"
	"testing"
)

func TestParseCourseWeeks(t *testing.T) {
	tests := []struct {
		value string
		want  []int
	}{
		{value: "1-4", want: []int{1, 2, 3, 4}},
		{value: "1-5周(单),8-12周(双),13周", want: []int{1, 3, 5, 8, 10, 12, 13}},
		{value: "2、4，6", want: []int{2, 4, 6}},
	}

	for _, test := range tests {
		if got := parseCourseWeeks(test.value); !reflect.DeepEqual(got, test.want) {
			t.Fatalf("parseCourseWeeks(%q) = %v, want %v", test.value, got, test.want)
		}
	}
}

func TestExpandCourseWeeks(t *testing.T) {
	list := &CourseList{Courses: []Course{{Name: "课程", Weeks: "2-4", Day: "Friday"}}}
	got := ExpandCourseWeeks(list)

	if len(got.Courses) != 3 {
		t.Fatalf("expanded course count = %d, want 3", len(got.Courses))
	}
	for index, want := range []string{"2", "3", "4"} {
		if got.Courses[index].Weeks != want {
			t.Fatalf("course %d week = %q, want %q", index, got.Courses[index].Weeks, want)
		}
	}
	if list.Courses[0].Weeks != "2-4" {
		t.Fatal("ExpandCourseWeeks mutated its input")
	}
}

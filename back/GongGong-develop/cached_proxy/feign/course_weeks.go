package feign

import (
	"regexp"
	"sort"
	"strconv"
	"strings"
)

var courseWeekNumberPattern = regexp.MustCompile(`\d+`)

// ExpandCourseWeeks returns one course record per teaching week. Released app
// versions persist deletions by comparing the complete course record, including
// Weeks. Returning a range such as "1-16" therefore makes deleting one visible
// occurrence hide the whole semester. Single-week records preserve the API
// contract while making old clients treat a deletion as one occurrence.
func ExpandCourseWeeks(list *CourseList) *CourseList {
	if list == nil {
		return nil
	}

	expanded := &CourseList{Courses: make([]Course, 0, len(list.Courses))}
	for _, course := range list.Courses {
		weeks := parseCourseWeeks(course.Weeks)
		if len(weeks) == 0 {
			expanded.Courses = append(expanded.Courses, course)
			continue
		}
		for _, week := range weeks {
			occurrence := course
			occurrence.Weeks = strconv.Itoa(week)
			expanded.Courses = append(expanded.Courses, occurrence)
		}
	}
	return expanded
}

func parseCourseWeeks(value string) []int {
	normalized := strings.NewReplacer(
		"，", ",", "、", ",", "；", ",", ";", ",",
		"～", "-", "~", "-", "周", "",
	).Replace(value)
	seen := make(map[int]struct{})

	for _, rawPart := range strings.Split(normalized, ",") {
		part := strings.TrimSpace(rawPart)
		if part == "" {
			continue
		}
		numberTexts := courseWeekNumberPattern.FindAllString(part, -1)
		if len(numberTexts) == 0 {
			continue
		}
		numbers := make([]int, 0, len(numberTexts))
		for _, text := range numberTexts {
			number, err := strconv.Atoi(text)
			if err == nil && number > 0 {
				numbers = append(numbers, number)
			}
		}
		if len(numbers) == 0 {
			continue
		}

		candidates := numbers
		if strings.Contains(part, "-") && len(numbers) >= 2 {
			start, end := numbers[0], numbers[1]
			if start > end {
				start, end = end, start
			}
			candidates = make([]int, 0, end-start+1)
			for week := start; week <= end; week++ {
				candidates = append(candidates, week)
			}
		}

		for _, week := range candidates {
			if strings.Contains(part, "单") && week%2 == 0 {
				continue
			}
			if strings.Contains(part, "双") && week%2 != 0 {
				continue
			}
			seen[week] = struct{}{}
		}
	}

	weeks := make([]int, 0, len(seen))
	for week := range seen {
		weeks = append(weeks, week)
	}
	sort.Ints(weeks)
	return weeks
}

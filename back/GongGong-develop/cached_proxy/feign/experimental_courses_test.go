package feign

import (
	"encoding/json"
	"testing"
)

func TestExperimentalCourseMetadataSurvivesProxyAndWeekExpansion(t *testing.T) {
	var list CourseList
	err := json.Unmarshal([]byte(`{"courses":[{"name":"数据结构与算法","day":"Tuesday","weeks":"9-10","start_time":7,"duration":2,"is_experiment":true,"experiment_id":"lab-1","experiment_project":"二叉树寻找","experiment_group":"分组2"}]}`), &list)
	if err != nil {
		t.Fatal(err)
	}
	expanded := ExpandCourseWeeks(&list)
	if len(expanded.Courses) != 2 {
		t.Fatal("Expected two week occurrences")
	}
	for _, course := range expanded.Courses {
		if !course.IsExperiment || course.ExperimentID != "lab-1" || course.ExperimentProject != "二叉树寻找" || course.ExperimentGroup != "分组2" {
			t.Fatalf("Lost lab metadata: %+v", course)
		}
	}
}

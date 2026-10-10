export function isExperimentalCourse(course) {
  return Boolean(course?.is_experiment || course?.isExperiment)
}

export function courseSlots(courses, periodCount = 11) {
  const slots = []
  for (let period = 1; period <= periodCount; period++) {
    const active = courses.filter(course => period >= Number(course.start_time) && period < Number(course.start_time) + Number(course.duration))
      .sort((a,b) => Number(isExperimentalCourse(a)) - Number(isExperimentalCourse(b)) || Number(a.start_time) - Number(b.start_time) || a.name.localeCompare(b.name))
    if (!active.length) continue
    const last = slots[slots.length - 1]
    const boundary = period === 5 || period === 9
    if (!boundary && last && last.start_time + last.duration === period && last.courses.length === active.length && last.courses.every((course,index) => course === active[index])) {
      last.duration++
    } else slots.push({ start_time: period, duration: 1, courses: active })
  }
  return slots
}

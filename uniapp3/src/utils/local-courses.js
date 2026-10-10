import { parseWeeks } from './date'

export function courseIdentity(course) {
  const identity = [course.day, course.name, course.teacher || '', course.classroom || '', Number(course.start_time), Number(course.duration)]
  if (course.is_experiment || course.isExperiment) identity.push('experiment', course.experiment_id || course.experimentId || '', course.experiment_project || course.experimentProject || '')
  return JSON.stringify(identity)
}

export function coursesOverlap(a, b) {
  return Number(a.start_time) < Number(b.start_time) + Number(b.duration) &&
    Number(b.start_time) < Number(a.start_time) + Number(a.duration)
}

export function mergeLocalCourses(remote, custom, deleted) {
  const result = {}
  for (const day of new Set([...Object.keys(remote || {}), ...custom.map((course) => course.day)])) {
    const originals = (remote?.[day] || []).flatMap((course) => {
      const withDay = { ...course, day }
      const removed = deleted.filter((entry) => entry.identity === courseIdentity(withDay)).map((entry) => entry.week)
      if (!removed.length) return [withDay]
      const weeks = parseWeeks(course.weeks)
      const active = weeks.size ? [...weeks] : Array.from({ length: 60 }, (_, index) => index + 1)
      const remaining = active.filter((week) => !removed.includes(week))
      return remaining.length ? [{ ...withDay, weeks: remaining.join(',') }] : []
    })
    result[day] = [...originals, ...custom.filter((course) => course.day === day)].sort((a, b) => a.start_time - b.start_time)
  }
  return result
}

export function validateCustomCourse(value) {
  const { startWeek, endWeek, start, end } = value
  return Boolean(value.name?.trim()) && [startWeek, endWeek, start, end].every(Number.isInteger) &&
    startWeek >= 1 && endWeek >= startWeek && endWeek <= 60 && start >= 1 && end >= start && end <= 11
}

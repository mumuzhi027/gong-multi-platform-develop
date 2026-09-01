import { parseWeeks } from './date'

export const DAYS = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday']
export const DAY_LABELS = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']

const DAY_ALIASES = {
  Monday: 'Monday', Tuesday: 'Tuesday', Wednesday: 'Wednesday', Thursday: 'Thursday',
  Friday: 'Friday', Saturday: 'Saturday', Sunday: 'Sunday',
  星期一: 'Monday', 星期二: 'Tuesday', 星期三: 'Wednesday', 星期四: 'Thursday',
  星期五: 'Friday', 星期六: 'Saturday', 星期日: 'Sunday',
  周一: 'Monday', 周二: 'Tuesday', 周三: 'Wednesday', 周四: 'Thursday',
  周五: 'Friday', 周六: 'Saturday', 周日: 'Sunday',
}

const COLORS = ['#FFA040', '#FF714D', '#FF7700', '#FF4000', '#55E6FF', '#88BCFF', '#F3A6FF', '#FFBDBD', '#CFB6FE', '#FFE188']
const SUMMER_STARTS = ['08:00', '08:55', '10:10', '11:05', '14:30', '15:25', '16:40', '17:35', '19:30', '20:25', '21:20']
const WINTER_STARTS = ['08:00', '08:55', '10:10', '11:05', '14:00', '14:55', '16:10', '17:05', '19:00', '19:55', '20:50']

function javaHash(value = '') {
  let hash = 0
  for (let index = 0; index < value.length; index += 1) hash = ((hash * 31) + value.charCodeAt(index)) | 0
  return Math.abs(hash)
}

export function courseColor(name = '') {
  return COLORS[javaHash(String(name)) % COLORS.length]
}

export function normalizeCourseMap(payload) {
  if (!payload) return Object.fromEntries(DAYS.map((day) => [day, []]))
  if (payload.data && typeof payload.data === 'object') return normalizeCourseMap(payload.data)
  // The /courses endpoint returns { courses: CourseElem[] }.
  // Android's CourseRepository explicitly extracts this property before
  // grouping by weekday, so the mini-program must do the same.
  if (Array.isArray(payload.courses)) return normalizeCourseMap(payload.courses)
  const result = Object.fromEntries(DAYS.map((day) => [day, []]))
  if (Array.isArray(payload)) {
    payload.forEach((course) => {
      const day = DAY_ALIASES[course?.day]
      if (!day) return
      result[day].push({
        ...course,
        start_time: course.start_time ?? course.startTime,
      })
    })
  } else {
    Object.entries(payload).forEach(([key, list]) => {
      const day = DAY_ALIASES[key]
      if (day && Array.isArray(list)) result[day].push(...list)
    })
  }
  DAYS.forEach((day) => result[day].sort((a, b) => Number(a.start_time) - Number(b.start_time)))
  return result
}

export function courseMapSize(courseMap) {
  return DAYS.reduce((total, day) => total + (Array.isArray(courseMap?.[day]) ? courseMap[day].length : 0), 0)
}

export function coursesForWeek(courseMap, week) {
  return DAYS.reduce((result, day) => {
    result[day] = (courseMap?.[day] || []).filter((course) => {
      const weeks = parseWeeks(course.weeks)
      return weeks.size === 0 || weeks.has(Number(week))
    })
    return result
  }, {})
}

export function todayCourses(courseMap, week, date = new Date()) {
  return coursesForWeek(courseMap, week)[DAYS[(date.getDay() + 6) % 7]] || []
}

export function courseTimes(course, date = new Date()) {
  const starts = date.getMonth() + 1 >= 5 && date.getMonth() + 1 <= 9 ? SUMMER_STARTS : WINTER_STARTS
  const start = starts[Math.max(0, Number(course?.start_time || 1) - 1)] || '--:--'
  if (start === '--:--') return [start, start]
  const [hour, minute] = start.split(':').map(Number)
  const duration = Math.max(1, Number(course?.duration || 1))
  const total = hour * 60 + minute + duration * 45 + (duration - 1) * 10
  return [start, `${String(Math.floor(total / 60) % 24).padStart(2, '0')}:${String(total % 60).padStart(2, '0')}`]
}

export function courseState(course, date = new Date()) {
  const [start, end] = courseTimes(course, date)
  const minutes = date.getHours() * 60 + date.getMinutes()
  const toMinutes = (value) => value.split(':').map(Number).reduce((sum, item, index) => sum + item * (index ? 1 : 60), 0)
  if (minutes > toMinutes(end)) return 'finished'
  if (minutes < toMinutes(start)) return 'upcoming'
  return 'during'
}

export function stateColor(state) {
  return { finished: '#2FB8BE', during: '#F8931D', upcoming: '#EF5C66' }[state] || '#EF5C66'
}

export function periodStarts(date = new Date()) {
  return date.getMonth() + 1 >= 5 && date.getMonth() + 1 <= 9 ? SUMMER_STARTS : WINTER_STARTS
}

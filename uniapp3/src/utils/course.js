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
// 试行表中的“其他教学区域”，不再按旧夏/冬季作息切换。
const REGULAR_STARTS = ['08:00', '08:55', '10:20', '11:15', '14:00', '14:55', '16:20', '17:15', '19:15', '20:10', '21:05']
// 课程按教室使用错峰时间；周课表默认常规作息，可手动切换查看。
const STAGGERED_STARTS = ['08:10', '09:05', '10:30', '11:25', '14:10', '15:05', '16:30', '17:25', '19:15', '20:10', '21:05']

export function isStaggeredCourse(course) {
  const location = String(course?.classroom || '').replace(/\s/g, '')
  return ['逸夫楼', '信息科技大楼', '信息科技楼'].some((building) => location.includes(building))
}

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
    const active = (courseMap?.[day] || []).filter((course) => {
      const weeks = parseWeeks(course.weeks)
      return weeks.size === 0 || weeks.has(Number(week))
    })
    const custom = active.filter((course) => course.isCustom)
    result[day] = active.filter((course) => course.isCustom || !custom.some((item) =>
      Number(course.start_time) < Number(item.start_time) + Number(item.duration) &&
      Number(item.start_time) < Number(course.start_time) + Number(course.duration)))
    return result
  }, {})
}

export function todayCourses(courseMap, week, date = new Date()) {
  const courses = coursesForWeek(courseMap, week)[DAYS[(date.getDay() + 6) % 7]] || []
  const occupied = new Set()
  return [...courses].sort((a,b) => Number(Boolean(b.isCustom)) - Number(Boolean(a.isCustom)) ||
    Number(Boolean(b.is_experiment || b.isExperiment)) - Number(Boolean(a.is_experiment || a.isExperiment)) || Number(a.start_time) - Number(b.start_time))
    .filter(course => {
      const slots = Array.from({length:Number(course.duration)},(_,index)=>Number(course.start_time)+index)
      if(slots.some(period=>occupied.has(period))) return false
      slots.forEach(period=>occupied.add(period))
      return true
    }).sort((a,b)=>Number(a.start_time)-Number(b.start_time))
}

export function courseTimes(course, date = new Date()) {
  const starts = periodStarts(date, isStaggeredCourse(course))
  const firstPeriod = Number(course?.start_time || 1)
  const start = starts[Math.max(0, firstPeriod - 1)] || `第${firstPeriod}节`
  if (start === '--:--') return [start, start]
  const duration = Math.max(1, Number(course?.duration || 1))
  const lastStart = starts[Number(course?.start_time || 1) + duration - 2]
  if (!lastStart) return [start, `第${firstPeriod + duration - 1}节`]
  const [lastHour, lastMinute] = lastStart.split(':').map(Number)
  const end = lastHour * 60 + lastMinute + 45
  return [start, `${String(Math.floor(end / 60)).padStart(2, '0')}:${String(end % 60).padStart(2, '0')}`]
}

export function courseState(course, date = new Date()) {
  const [start, end] = courseTimes(course, date)
  const minutes = date.getHours() * 60 + date.getMinutes()
  const toMinutes = (value) => value.split(':').map(Number).reduce((sum, item, index) => sum + item * (index ? 1 : 60), 0)
  if (start.includes('节')) return 'upcoming'
  if (end.includes('节')) return minutes < toMinutes(start) ? 'upcoming' : 'during'
  if (minutes > toMinutes(end)) return 'finished'
  if (minutes < toMinutes(start)) return 'upcoming'
  return 'during'
}

export function stateColor(state) {
  return { finished: '#2FB8BE', during: '#F8931D', upcoming: '#EF5C66' }[state] || '#EF5C66'
}

export function periodStarts(date = new Date(), staggered = false) {
  return staggered ? STAGGERED_STARTS : REGULAR_STARTS
}

const WEEKDAYS = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']

export function formatDate(date = new Date()) {
  const pad = (value) => String(value).padStart(2, '0')
  return `${date.getFullYear()}.${pad(date.getMonth() + 1)}.${pad(date.getDate())}`
}

export function weekdayName(date = new Date()) {
  return WEEKDAYS[date.getDay()]
}

export function calculateWeek(calendar, date = new Date()) {
  if (!calendar?.start) return 0
  const start = new Date(`${calendar.start}T00:00:00`)
  const current = new Date(date.getFullYear(), date.getMonth(), date.getDate())
  const days = Math.trunc((current - start) / 86400000)
  return Math.max(0, Math.trunc(days / 7) + 1)
}

export function parseWeeks(value = '') {
  const result = new Set()
  String(value)
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
    .forEach((part) => {
      const [from, to] = part.split('-').map(Number)
      if (Number.isFinite(from) && Number.isFinite(to)) {
        for (let week = from; week <= to; week += 1) result.add(week)
      } else if (Number.isFinite(from)) {
        result.add(from)
      }
    })
  return result
}

export function academicTermLabel(term) {
  const value = Number(term)
  if (!value) return '未知学期'
  const year = Math.ceil(value / 2)
  const names = ['零', '一', '二', '三', '四', '五', '六']
  return `大${names[year] || year}${value % 2 ? '上' : '下'}`
}

export function addDays(date, amount) {
  const result = new Date(date)
  result.setDate(result.getDate() + amount)
  return result
}

export function weekDates(calendar, week) {
  if (!calendar?.start) return Array.from({ length: 7 }, (_, index) => addDays(new Date(), index - ((new Date().getDay() + 6) % 7)))
  const start = new Date(`${calendar.start}T00:00:00`)
  return Array.from({ length: 7 }, (_, index) => addDays(start, (Number(week) - 1) * 7 + index))
}

export function dateLabel(offset = 0) {
  const date = new Date()
  date.setDate(date.getDate() + offset)
  return `${date.getMonth() + 1}月${date.getDate()}日`
}

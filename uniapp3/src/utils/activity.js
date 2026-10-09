import { API_BASE_URL, NATIVE_API_BASE_URL } from '../config/env'

export function normalizeActivityDate(value) {
  return String(value || '').trim().replace(/\./g, '-')
}

export function activityDate(value) {
  const normalized = normalizeActivityDate(value)
  if (!/^\d{4}-\d{2}-\d{2}$/.test(normalized)) return null
  const [year, month, day] = normalized.split('-').map(Number)
  return new Date(year, month - 1, day)
}

export function isPastActivity(activity, now = new Date()) {
  const end = activityDate(activity?.endDate)
  if (!end) return false
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  return end < today
}

export function activityDurationDays(activity) {
  const start = activityDate(activity?.startDate)
  const end = activityDate(activity?.endDate)
  if (!start || !end || end < start) return null
  return Math.round((end - start) / 86400000) + 1
}

export function activityDurationLabel(activity) {
  const days = activityDurationDays(activity)
  return days ? `共 ${days} 天` : '时间待定'
}

export function formatActivityRange(activity) {
  const start = normalizeActivityDate(activity?.startDate)
  const end = normalizeActivityDate(activity?.endDate)
  if (!start) return '时间待定'
  return !end || start === end ? start : `${start} - ${end}`
}

export function activityImageUrl(url) {
  if (!url) return ''
  const value = String(url).trim()
  if (!value) return ''
  // #ifdef H5
  // Existing poster records can contain absolute URLs for the HTTP app backend.
  // Route those through the same HTTPS origin as the PWA as well.
  const backendOrigin = (import.meta.env.VITE_BACKEND_PUBLIC_ORIGIN || NATIVE_API_BASE_URL).replace(/\/$/, '')
  if (value.startsWith(`${backendOrigin}/`)) return `${API_BASE_URL}${value.slice(backendOrigin.length)}`
  if (value.startsWith(`${API_BASE_URL}/`)) return value
  // #endif
  if (/^https?:\/\//i.test(value)) return value
  if (value.startsWith('//')) return `https:${value}`
  return `${API_BASE_URL.replace(/\/$/, '')}/${value.replace(/^\/+/, '')}`
}

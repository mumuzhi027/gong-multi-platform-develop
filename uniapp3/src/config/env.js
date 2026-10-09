export const NATIVE_API_BASE_URL = 'http://101.43.27.78:8000'
let apiBaseUrl = NATIVE_API_BASE_URL
let webBaseUrl = `${NATIVE_API_BASE_URL}/`
// #ifdef H5
apiBaseUrl = (import.meta.env.VITE_H5_API_BASE_URL || '/api').replace(/\/$/, '')
webBaseUrl = `${(import.meta.env.VITE_H5_WEB_BASE_URL || '/web').replace(/\/$/, '')}/`
// #endif
export const API_BASE_URL = apiBaseUrl
export const WEB_BASE_URL = webBaseUrl
export const APP_VERSION = '1.0.0'
export const WECHAT_SUBSCRIBE_TEMPLATE_IDS = []

export const STORAGE_KEYS = {
  token: 'gong_access_token',
  offline: 'gong_offline_mode',
  profile: 'gong_user_profile',
  username: 'gong_username',
  courseMap: 'gong_course_map',
  calendar: 'gong_calendar',
  exams: 'gong_exams',
  todayClassroom: 'gong_classroom_today',
  tomorrowClassroom: 'gong_classroom_tomorrow',
  academic: 'gong_academic',
  activities: 'gong_activities',
}

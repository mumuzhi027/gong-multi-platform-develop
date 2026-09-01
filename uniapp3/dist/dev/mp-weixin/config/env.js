"use strict";
const API_BASE_URL = "http://101.43.27.78:8000";
const APP_VERSION = "1.0.0";
const WECHAT_SUBSCRIBE_TEMPLATE_IDS = [];
const STORAGE_KEYS = {
  token: "gong_access_token",
  offline: "gong_offline_mode",
  profile: "gong_user_profile",
  username: "gong_username",
  courseMap: "gong_course_map",
  calendar: "gong_calendar",
  exams: "gong_exams",
  todayClassroom: "gong_classroom_today",
  tomorrowClassroom: "gong_classroom_tomorrow",
  academic: "gong_academic"
};
exports.API_BASE_URL = API_BASE_URL;
exports.APP_VERSION = APP_VERSION;
exports.STORAGE_KEYS = STORAGE_KEYS;
exports.WECHAT_SUBSCRIBE_TEMPLATE_IDS = WECHAT_SUBSCRIBE_TEMPLATE_IDS;
//# sourceMappingURL=../../.sourcemap/mp-weixin/config/env.js.map

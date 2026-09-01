"use strict";
const api_request = require("./request.js");
async function login(username, password) {
  return api_request.unwrap(await api_request.formRequest("/login", { username, password }));
}
const campusApi = {
  profile: async () => api_request.unwrap(await api_request.request({ url: "/info" })),
  courses: async () => api_request.unwrap(await api_request.request({ url: "/courses" })),
  exams: async () => api_request.unwrap(await api_request.request({ url: "/exams" })),
  calendar: async () => api_request.unwrap(await api_request.request({ url: "/calendar" })),
  classroomToday: async () => api_request.unwrap(await api_request.request({ url: "/classroom/today" })),
  classroomTomorrow: async () => api_request.unwrap(await api_request.request({ url: "/classroom/tomorrow" })),
  scores: async (refresh = false) => api_request.unwrap(await api_request.request({ url: "/scores", data: { refresh } })),
  minorScores: async (refresh = false) => api_request.unwrap(await api_request.request({ url: "/minor/scores", data: { refresh } })),
  rank: async (refresh = false) => api_request.unwrap(await api_request.request({ url: "/rank", data: { refresh } })),
  compulsoryRank: async (refresh = false) => api_request.unwrap(await api_request.request({ url: "/compulsory/rank", data: { refresh } }))
};
exports.campusApi = campusApi;
exports.login = login;
//# sourceMappingURL=../../.sourcemap/mp-weixin/api/index.js.map

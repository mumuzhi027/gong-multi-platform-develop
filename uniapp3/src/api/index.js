import { formRequest, request, unwrap } from './request'

export async function login(username, password) {
  return unwrap(await formRequest('/login', { username, password }))
}

export const campusApi = {
  profile: async () => unwrap(await request({ url: '/info' })),
  courses: async () => unwrap(await request({ url: '/courses' })),
  exams: async () => unwrap(await request({ url: '/exams' })),
  calendar: async () => unwrap(await request({ url: '/calendar' })),
  classroomToday: async () => unwrap(await request({ url: '/classroom/today' })),
  classroomTomorrow: async () => unwrap(await request({ url: '/classroom/tomorrow' })),
  scores: async (refresh = false) => unwrap(await request({ url: '/scores', data: { refresh } })),
  minorScores: async (refresh = false) => unwrap(await request({ url: '/minor/scores', data: { refresh } })),
  rank: async (refresh = false) => unwrap(await request({ url: '/rank', data: { refresh } })),
  compulsoryRank: async (refresh = false) =>
    unwrap(await request({ url: '/compulsory/rank', data: { refresh } })),
}

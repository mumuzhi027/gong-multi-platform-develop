export const ALL_STAGGERED_TIME_GROUPS = 0b111111
export const COURSE_TIME_MODES = ['混合', '错峰', '常规']

export function nextCourseTimeMode(mode) {
  return COURSE_TIME_MODES[(COURSE_TIME_MODES.indexOf(mode) + 1) % COURSE_TIME_MODES.length]
}

export function cycleCourseTimeDisplay(state) {
  const mode = nextCourseTimeMode(state.mode)
  return { mode, staggeredGroups: mode === '错峰' ? ALL_STAGGERED_TIME_GROUPS : 0 }
}

export function toggleCourseTimeGroup(state, group) {
  if (!Number.isInteger(group) || group < 0 || group > 5) return state
  return { mode: '混合', staggeredGroups: state.staggeredGroups ^ (1 << group) }
}

export function courseDisplayAlpha(mode, staggered) {
  return (mode === '错峰' && !staggered) || (mode === '常规' && staggered) ? 0.25 : 1
}

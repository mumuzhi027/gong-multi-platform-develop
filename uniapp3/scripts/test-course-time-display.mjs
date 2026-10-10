import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const source = await readFile(new URL('../src/utils/courseTimeDisplay.js', import.meta.url), 'utf8')
const { ALL_STAGGERED_TIME_GROUPS, COURSE_TIME_MODES, cycleCourseTimeDisplay, toggleCourseTimeGroup, courseDisplayAlpha } =
  await import(`data:text/javascript;base64,${Buffer.from(source).toString('base64')}`)

let state = { mode: '混合', staggeredGroups: 0 }
for (let round = 0; round < 2; round++) {
  for (const [mode, mask, regularAlpha, staggeredAlpha] of [
    ['混合', 0, 1, 1], ['错峰', ALL_STAGGERED_TIME_GROUPS, 0.25, 1], ['常规', 0, 1, 0.25],
  ]) {
    assert.deepEqual(state, { mode, staggeredGroups: mask })
    assert.equal(courseDisplayAlpha(state.mode, false), regularAlpha)
    assert.equal(courseDisplayAlpha(state.mode, true), staggeredAlpha)
    state = cycleCourseTimeDisplay(state)
  }
}
for (const mode of COURSE_TIME_MODES) {
  for (const mask of [0, 1, 31, ALL_STAGGERED_TIME_GROUPS]) {
    for (let group = 0; group < 6; group++) {
      const mixed = toggleCourseTimeGroup({ mode, staggeredGroups: mask }, group)
      assert.deepEqual(mixed, { mode: '混合', staggeredGroups: mask ^ (1 << group) })
      assert.equal(courseDisplayAlpha(mixed.mode, false), 1)
      assert.equal(courseDisplayAlpha(mixed.mode, true), 1)
      assert.deepEqual(cycleCourseTimeDisplay(mixed), { mode: '错峰', staggeredGroups: ALL_STAGGERED_TIME_GROUPS })
    }
  }
}
console.log('Course display tests passed: mode cycle, course opacity, every time group, uniform-mask mixed states, and cycle restart.')

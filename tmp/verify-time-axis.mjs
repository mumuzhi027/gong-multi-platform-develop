import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import { createRequire } from 'node:module'
import vm from 'node:vm'

const require = createRequire(new URL('../uniapp3/package.json', import.meta.url))
const { ref, computed, nextTick } = require('vue')
const { parse } = require('@vue/compiler-sfc')
const { baseParse } = require('@vue/compiler-dom')
const savedPreferences = new Map()
globalThis.uni = {
  getStorageSync: (key) => savedPreferences.has(key) ? savedPreferences.get(key) : '',
  setStorageSync: (key, value) => savedPreferences.set(key, value),
  removeStorageSync: (key) => savedPreferences.delete(key),
}
const storageSource = await readFile(new URL('../uniapp3/src/utils/storage.js', import.meta.url), 'utf8')
const { storage } = await import(`data:text/javascript;base64,${Buffer.from(storageSource).toString('base64')}`)
const dateSource = await readFile(new URL('../uniapp3/src/utils/date.js', import.meta.url), 'utf8')
const dateUrl = `data:text/javascript;base64,${Buffer.from(dateSource).toString('base64')}`
const date = await import(dateUrl)
const courseSource = (await readFile(new URL('../uniapp3/src/utils/course.js', import.meta.url), 'utf8')).replace("'./date'", JSON.stringify(dateUrl))
const course = await import(`data:text/javascript;base64,${Buffer.from(courseSource).toString('base64')}`)
const source = await readFile(new URL('../uniapp3/src/pages/course/index.vue', import.meta.url), 'utf8')
const { descriptor, errors } = parse(source)
assert.deepEqual(errors, [])
baseParse(descriptor.template.content)
const baseContext = {
  ...date, ...course, ref, computed, nextTick, Date, storage,
  onLoad: () => {}, onPullDownRefresh: () => {},
  useAuthStore: () => ({ authenticated: true, offline: true }),
  useCampusStore: () => ({ calendar: { start: '2026-08-31', weeks: 20 }, courseMap: {} }),
}
function reopenPage() {
  const context = { ...baseContext }
  vm.runInNewContext(descriptor.scriptSetup.content.replace(/^import .*$/gm, '') +
    '\nglobalThis.axis = { timeGroups, timeMode, staggeredTimeGroups, currentWeek, weekSlides, isStaggeredGroup, toggleTimeGroup, toggleAllTimes, timeGroupLabel };', context)
  return context.axis
}
const axis = reopenPage()
const periods = JSON.parse(JSON.stringify(axis.timeGroups.map((group) => group.periods)))
assert.deepEqual(periods, [[0,1],[2,3],[4,5],[6,7],[8,9],[10]])
assert.equal(axis.timeMode.value, '常规')
const winterSlide = { starts: course.periodStarts(new Date(2026, 9, 7)) }
const summerSlide = { starts: course.periodStarts(new Date(2026, 6, 7)) }
const initial = JSON.stringify(winterSlide)
axis.toggleTimeGroup(0)
assert.equal(axis.timeMode.value, '混合')
assert.equal(axis.isStaggeredGroup(1), false)
assert.match(axis.timeGroupLabel(winterSlide, axis.timeGroups[0]), /08:10至09:50/)
assert.match(axis.timeGroupLabel(winterSlide, axis.timeGroups[1]), /10:20至12:00/)
axis.toggleTimeGroup(0)
assert.equal(axis.timeMode.value, '常规')
assert.match(axis.timeGroupLabel(winterSlide, axis.timeGroups[0]), /08:00至09:40/)
for (const group of axis.timeGroups) {
  axis.toggleTimeGroup(group.index)
  assert.equal(axis.isStaggeredGroup(group.index), true)
}
assert.equal(axis.timeMode.value, '错峰')
assert.match(axis.timeGroupLabel(winterSlide, axis.timeGroups[5]), /第11节.*21:05至21:50/)
axis.toggleAllTimes()
assert.equal(axis.staggeredTimeGroups.value, 0)
axis.toggleTimeGroup(2)
axis.toggleAllTimes()
assert.equal(axis.staggeredTimeGroups.value, 63)
assert.match(axis.timeGroupLabel(winterSlide, axis.timeGroups[2]), /14:10至15:50/)
assert.match(axis.timeGroupLabel(summerSlide, axis.timeGroups[2]), /14:10至15:50/)
axis.currentWeek.value = 6
assert.equal(axis.timeMode.value, '错峰')
axis.toggleAllTimes()
assert.match(axis.timeGroupLabel(summerSlide, axis.timeGroups[2]), /14:00至15:40/)
assert.equal(JSON.stringify(winterSlide), initial)
axis.toggleTimeGroup(0)
axis.toggleTimeGroup(3)
let restored = reopenPage()
assert.equal(restored.staggeredTimeGroups.value, 9)
assert.equal(restored.timeMode.value, '混合')
assert.equal(restored.isStaggeredGroup(0), true)
assert.equal(restored.isStaggeredGroup(3), true)
assert.equal(restored.isStaggeredGroup(1), false)
restored.toggleAllTimes()
restored = reopenPage()
assert.equal(restored.staggeredTimeGroups.value, 63)
assert.equal(restored.timeMode.value, '错峰')
restored.toggleAllTimes()
assert.equal(reopenPage().timeMode.value, '常规')
for (const invalid of ['bad data', -1, 64, 1.5]) {
  savedPreferences.set('gong_course_time_axis', invalid)
  assert.equal(reopenPage().staggeredTimeGroups.value, 0)
}
console.log('Time axis checks passed: pair/all toggles, updated trial times, reopening restores mixed/staggered/regular modes, and invalid saved data resets safely.')

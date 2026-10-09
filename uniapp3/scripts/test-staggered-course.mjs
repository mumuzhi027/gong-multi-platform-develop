import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

// Load the same source modules as Vite, resolving the extensionless date import for Node.
const dateSource = await readFile(new URL('../src/utils/date.js', import.meta.url), 'utf8')
const dateUrl = `data:text/javascript;base64,${Buffer.from(dateSource).toString('base64')}`
const source = (await readFile(new URL('../src/utils/course.js', import.meta.url), 'utf8'))
  .replace("'./date'", JSON.stringify(dateUrl))
const { courseTimes, courseState, isStaggeredCourse, periodStarts } =
  await import(`data:text/javascript;base64,${Buffer.from(source).toString('base64')}`)

const lesson = (classroom = '逸夫楼101', start_time = 1, duration = 2) => ({ classroom, start_time, duration })
const winter = new Date(2026, 9, 7, 8, 0)
const summer = new Date(2026, 6, 7, 8, 0)
const expected = [
  ['08:10', '08:55'], ['09:05', '09:50'], ['10:30', '11:15'], ['11:25', '12:10'],
  ['14:10', '14:55'], ['15:05', '15:50'], ['16:30', '17:15'], ['17:25', '18:10'],
  ['19:15', '20:00'], ['20:10', '20:55'], ['21:05', '21:50'],
]
const regular = [
  ['08:00', '08:45'], ['08:55', '09:40'], ['10:20', '11:05'], ['11:15', '12:00'],
  ['14:00', '14:45'], ['14:55', '15:40'], ['16:20', '17:05'], ['17:15', '18:00'],
  ['19:15', '20:00'], ['20:10', '20:55'], ['21:05', '21:50'],
]
for (const room of ['逸夫楼101', '信息科技大楼A302', '信息科技楼302', ' 信息科技 大楼 302 ']) {
  assert.ok(isStaggeredCourse(lesson(room)))
  for (const date of [winter, summer]) {
    expected.forEach((times, index) => assert.deepEqual(courseTimes(lesson(room, index + 1, 1), date), times))
  }
}
for (const room of ['', '第三教学楼101', '信息楼101', '逸夫图书馆']) {
  assert.equal(isStaggeredCourse(lesson(room)), false)
  for (const date of [winter, summer, new Date(2026, 0, 7)]) {
    regular.forEach((times, index) => assert.deepEqual(courseTimes(lesson(room, index + 1, 1), date), times))
  }
}
assert.deepEqual(courseTimes(lesson('逸夫楼101', 1, 4), winter), ['08:10', '12:10'])
assert.deepEqual(courseTimes(lesson('信息科技大楼302', 6, 2), winter), ['15:05', '17:15'])
for (const [hour, minute, state] of [[8, 9, 'upcoming'], [8, 10, 'during'], [9, 50, 'during'], [9, 51, 'finished']]) {
  assert.equal(courseState(lesson(), new Date(2026, 9, 7, hour, minute)), state)
}
assert.deepEqual(courseTimes(lesson('第三教学楼101', 5), winter), ['14:00', '15:40'])
assert.deepEqual(courseTimes(lesson('第三教学楼101', 5), summer), ['14:00', '15:40'])
assert.deepEqual(courseTimes(lesson('第三教学楼101', 1, 4), winter), ['08:00', '12:00'])
assert.deepEqual(courseTimes(lesson('第三教学楼101', 6, 2), winter), ['14:55', '17:05'])
for (const [hour, minute, state] of [[10, 19, 'upcoming'], [10, 20, 'during'], [12, 0, 'during'], [12, 1, 'finished']]) {
  assert.equal(courseState(lesson('第三教学楼101', 3, 2), new Date(2026, 9, 7, hour, minute)), state)
}
for (const date of [winter, summer]) {
  assert.deepEqual(periodStarts(date), regular.map(([start]) => start))
  assert.deepEqual(periodStarts(date, true), expected.map(([start]) => start))
}
// 查看错峰作息后，默认时间轴仍返回试行表中的常规时间。
assert.equal(periodStarts(winter)[4], '14:00')
assert.equal(periodStarts(summer)[4], '14:00')
// Native home, timetable and Android widget share this table; verify both platforms' data agree.
const kotlinSource = await readFile(new URL('../../composeApp/src/commonMain/kotlin/com/sky31/gongmultiplatform/util/courseUtils.kt', import.meta.url), 'utf8')
for (const [name, times] of [['regularStartTime', regular], ['staggeredStartTime', expected]]) {
  const table = kotlinSource.match(new RegExp(`private val ${name} = listOf\\(([\\s\\S]*?)\\n\\)`))?.[1]
  assert.ok(table, `${name} exists in Kotlin`)
  const starts = [...table.matchAll(/CustomTime\((\d+),\s*(\d+)\)/g)].map(([, hour, minute]) => `${hour.padStart(2, '0')}:${minute.padStart(2, '0')}`)
  assert.deepEqual(starts, times.map(([start]) => start))
}
console.log('Trial timetable tests passed: both platforms, both teaching areas, all 11 periods, long breaks, progress boundaries, and selectable weekly times.')

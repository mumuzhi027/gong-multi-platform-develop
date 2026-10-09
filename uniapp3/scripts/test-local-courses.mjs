import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
const moduleUrl = (code) => `data:text/javascript;base64,${Buffer.from(code).toString('base64')}`
const date = moduleUrl(readFileSync(new URL('../src/utils/date.js', import.meta.url), 'utf8'))
const source = readFileSync(new URL('../src/utils/local-courses.js', import.meta.url), 'utf8').replace("'./date'", JSON.stringify(date))
const { courseIdentity, mergeLocalCourses, validateCustomCourse } = await import(moduleUrl(source))
const utilSource = readFileSync(new URL('../src/utils/course.js', import.meta.url), 'utf8').replace("'./date'", JSON.stringify(date))
const { coursesForWeek } = await import(moduleUrl(utilSource))
const original = { day: 'Monday', name: '原始课程', teacher: '老师', classroom: '逸夫楼', start_time: 1, duration: 2, weeks: '1-16' }
const remote = { Monday: [original] }
const deleted = [{ identity: courseIdentity(original), week: 6, course: original }]
const map = mergeLocalCourses(remote, [], deleted)
assert.equal(coursesForWeek(map, 6).Monday.length, 0)
assert.equal(coursesForWeek(map, 5).Monday.length, 1)
assert.equal(coursesForWeek(map, 7).Monday.length, 1)
assert.equal(original.weeks, '1-16', 'Deleting one occurrence must preserve the remote definition')
const custom = { ...original, name: '自定义课程', isCustom: true, weeks: '6' }
assert.equal(coursesForWeek(mergeLocalCourses(remote, [custom], []), 6).Monday[0].name, '自定义课程')
assert.equal(coursesForWeek(mergeLocalCourses(remote, [custom], []), 7).Monday[0].name, '原始课程')
const valid = { name: '课程', startWeek: 1, endWeek: 60, start: 1, end: 11 }
assert.ok(validateCustomCourse(valid))
for (const invalid of [{ name: ' ' }, { endWeek: 61 }, { startWeek: 0 }, { start: 0 }, { end: 12 }, { start: 4, end: 3 }, { startWeek: 6, endWeek: 5 }]) {
  assert.ok(!validateCustomCourse({ ...valid, ...invalid }))
}
console.log('Local course checks passed: one-week deletion, remote preservation, custom course priority, week isolation and editor validation.')

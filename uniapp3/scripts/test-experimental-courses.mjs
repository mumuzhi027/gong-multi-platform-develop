import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
const moduleUrl = source => `data:text/javascript;base64,${Buffer.from(source).toString('base64')}`
const date = moduleUrl(readFileSync(new URL('../src/utils/date.js', import.meta.url), 'utf8'))
const courseModule = readFileSync(new URL('../src/utils/course.js', import.meta.url), 'utf8').replace("'./date'", JSON.stringify(date))
const { coursesForWeek, courseTimes, courseState, todayCourses } = await import(moduleUrl(courseModule))
const { courseSlots } = await import(moduleUrl(readFileSync(new URL('../src/utils/courseSlots.js', import.meta.url), 'utf8')))
const localModule = readFileSync(new URL('../src/utils/local-courses.js', import.meta.url), 'utf8').replace("'./date'", JSON.stringify(date))
const { courseIdentity, mergeLocalCourses } = await import(moduleUrl(localModule))
const ordinary = { name:'数据结构与算法',classroom:'工科楼南楼607',teacher:'老师',day:'Tuesday',weeks:'1-20',start_time:7,duration:2 }
const lab = { ...ordinary,weeks:'10',is_experiment:true,experiment_id:'lab-1',experiment_project:'二叉树寻找',experiment_group:'分组2' }
const courses = coursesForWeek({Tuesday:[lab,ordinary]},10).Tuesday
assert.deepEqual(courseSlots(courses)[0].courses,[ordinary,lab])
assert.deepEqual(coursesForWeek({Tuesday:[lab,ordinary]},9).Tuesday,[ordinary])
assert.deepEqual(todayCourses({Tuesday:[ordinary,lab]},10,new Date(2026,9,6)),[lab])
const longer = { ...ordinary,start_time:5,duration:4 }
assert.deepEqual(courseSlots([longer,lab]).map(slot=>[slot.start_time,slot.duration,slot.courses]),[[5,2,[longer]],[7,2,[longer,lab]]])
assert.deepEqual(coursesForWeek({Tuesday:[lab,ordinary,{...ordinary,isCustom:true}]},10).Tuesday,[{...ordinary,isCustom:true}])
assert.notEqual(courseIdentity(lab),courseIdentity(ordinary))
const deleted = mergeLocalCourses({Tuesday:[ordinary,lab]},[],[{identity:courseIdentity(lab),week:10,course:lab}])
assert.deepEqual(coursesForWeek(deleted,10).Tuesday,[ordinary])
const lateLab = { ...lab,start_time:9,duration:4 }
assert.deepEqual(courseTimes(lateLab),['19:15','第12节'])
assert.equal(courseState(lateLab,new Date(2026,9,10,20,0)),'during')
assert.equal(courseSlots([lateLab],12)[0].duration,4)
assert.equal(courseSlots([lateLab],11)[0].duration,3)
console.log('Experimental course checks passed: week isolation, badges metadata, exact/partial conflicts, custom priority, lab-only deletion, and period 12 labels.')

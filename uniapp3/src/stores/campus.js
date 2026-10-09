import { defineStore } from 'pinia'
import { campusApi } from '../api'
import { STORAGE_KEYS } from '../config/env'
import { normalizeCourseMap } from '../utils/course'
import { storage } from '../utils/storage'
import { courseIdentity, coursesOverlap, mergeLocalCourses, validateCustomCourse } from '../utils/local-courses'
import { parseWeeks } from '../utils/date'

export const useCampusStore = defineStore('campus', {
  state: () => ({
    remoteCourseMap: storage.get(STORAGE_KEYS.courseMap, {}),
    customCourses: storage.get('gong_custom_courses', []),
    deletedCourses: storage.get('gong_deleted_courses', []),
    calendar: storage.get(STORAGE_KEYS.calendar, null),
    exams: storage.get(STORAGE_KEYS.exams, []),
    activities: storage.get(STORAGE_KEYS.activities, []),
    todayClassroom: storage.get(STORAGE_KEYS.todayClassroom, null),
    tomorrowClassroom: storage.get(STORAGE_KEYS.tomorrowClassroom, null),
    academic: storage.get(STORAGE_KEYS.academic, {
      major: null,
      minor: null,
      rank: null,
      compulsoryRank: null,
    }),
    loading: {},
    errors: {},
  }),
  getters: {
    courseMap: (state) => mergeLocalCourses(state.remoteCourseMap, state.customCourses, state.deletedCourses),
  },
  actions: {
    saveCustomCourse(value, origin = null) {
      if (!validateCustomCourse(value)) throw new Error('请填写课程名称，周次为1–60，节次为1–11，结束不得早于开始')
      const course = {
        id: origin?.id || `custom-${Date.now()}-${Math.random().toString(36).slice(2)}`,
        name: value.name.trim(), teacher: value.teacher.trim(), classroom: value.classroom.trim(),
        day: value.day, weeks: value.startWeek === value.endWeek ? String(value.startWeek) : `${value.startWeek}-${value.endWeek}`,
        start_time: value.start, duration: value.end - value.start + 1, isCustom: true,
      }
      const next = [...this.customCourses.filter((item) => item.id !== course.id), course]
      storage.set('gong_custom_courses', next)
      this.customCourses = next
    },
    deleteCustomCourse(course) {
      const next = this.customCourses.filter((item) => item.id !== course.id)
      storage.set('gong_custom_courses', next)
      this.customCourses = next
    },
    deleteRemoteCourse(course, week) {
      const identity = courseIdentity(course)
      if (this.deletedCourses.some((entry) => entry.identity === identity && entry.week === week)) return
      const next = [...this.deletedCourses, { identity, week, course }]
      storage.set('gong_deleted_courses', next)
      this.deletedCourses = next
    },
    restoreCourses(week) {
      const restoring = this.deletedCourses.filter((entry) => entry.week === week)
      const custom = this.customCourses.filter((course) => !parseWeeks(course.weeks).has(week) ||
        !restoring.some((entry) => entry.course.day === course.day && coursesOverlap(entry.course, course)))
      const deleted = this.deletedCourses.filter((entry) => entry.week !== week)
      storage.set('gong_custom_courses', custom)
      storage.set('gong_deleted_courses', deleted)
      this.customCourses = custom
      this.deletedCourses = deleted
      return restoring.length
    },
    setLoading(name, value) {
      this.loading[name] = value
    },
    setError(name, error) {
      this.errors[name] = error?.authExpired ? '' : (error?.message || '')
    },
    async loadHome() {
      this.setLoading('home', true)
      this.setError('home', null)
      try {
        const [courses, calendar, exams] = await Promise.all([
          campusApi.courses(),
          campusApi.calendar(),
          campusApi.exams(),
        ])
        this.remoteCourseMap = normalizeCourseMap(courses)
        this.calendar = calendar
        this.exams = Array.isArray(exams) ? exams : exams?.exams || []
        storage.set(STORAGE_KEYS.courseMap, this.remoteCourseMap)
        storage.set(STORAGE_KEYS.calendar, this.calendar)
        storage.set(STORAGE_KEYS.exams, this.exams)
      } catch (error) {
        this.setError('home', error)
        throw error
      } finally {
        this.setLoading('home', false)
      }
    },
    async loadActivities() {
      this.setLoading('activities', true)
      this.setError('activities', null)
      try {
        const activities = await campusApi.activities()
        this.activities = Array.isArray(activities) ? activities : []
        storage.set(STORAGE_KEYS.activities, this.activities)
        return this.activities
      } catch (error) {
        this.setError('activities', error)
        throw error
      } finally {
        this.setLoading('activities', false)
      }
    },
    async loadClassrooms() {
      this.setLoading('classroom', true)
      this.setError('classroom', null)
      try {
        const [today, tomorrow] = await Promise.all([
          campusApi.classroomToday(),
          campusApi.classroomTomorrow(),
        ])
        this.todayClassroom = today
        this.tomorrowClassroom = tomorrow
        storage.set(STORAGE_KEYS.todayClassroom, today)
        storage.set(STORAGE_KEYS.tomorrowClassroom, tomorrow)
      } catch (error) {
        this.setError('classroom', error)
        throw error
      } finally {
        this.setLoading('classroom', false)
      }
    },
    async loadAcademic(refresh = false) {
      this.setLoading('academic', true)
      this.setError('academic', null)
      try {
        const [major, minor, rank, compulsoryRank] = await Promise.all([
          campusApi.scores(refresh),
          campusApi.minorScores(refresh),
          campusApi.rank(refresh),
          campusApi.compulsoryRank(refresh),
        ])
        this.academic = { major, minor, rank, compulsoryRank }
        storage.set(STORAGE_KEYS.academic, this.academic)
      } catch (error) {
        this.setError('academic', error)
        throw error
      } finally {
        this.setLoading('academic', false)
      }
    },
  },
})

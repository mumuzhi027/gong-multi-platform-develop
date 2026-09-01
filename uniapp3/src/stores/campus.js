import { defineStore } from 'pinia'
import { campusApi } from '../api'
import { STORAGE_KEYS } from '../config/env'
import { normalizeCourseMap } from '../utils/course'
import { storage } from '../utils/storage'

export const useCampusStore = defineStore('campus', {
  state: () => ({
    courseMap: storage.get(STORAGE_KEYS.courseMap, {}),
    calendar: storage.get(STORAGE_KEYS.calendar, null),
    exams: storage.get(STORAGE_KEYS.exams, []),
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
  actions: {
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
        this.courseMap = normalizeCourseMap(courses)
        this.calendar = calendar
        this.exams = Array.isArray(exams) ? exams : exams?.exams || []
        storage.set(STORAGE_KEYS.courseMap, this.courseMap)
        storage.set(STORAGE_KEYS.calendar, this.calendar)
        storage.set(STORAGE_KEYS.exams, this.exams)
      } catch (error) {
        this.setError('home', error)
        throw error
      } finally {
        this.setLoading('home', false)
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

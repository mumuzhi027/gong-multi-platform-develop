"use strict";
const common_vendor = require("../common/vendor.js");
const api_index = require("../api/index.js");
const config_env = require("../config/env.js");
const utils_course = require("../utils/course.js");
const utils_storage = require("../utils/storage.js");
const useCampusStore = common_vendor.defineStore("campus", {
  state: () => ({
    courseMap: utils_storage.storage.get(config_env.STORAGE_KEYS.courseMap, {}),
    calendar: utils_storage.storage.get(config_env.STORAGE_KEYS.calendar, null),
    exams: utils_storage.storage.get(config_env.STORAGE_KEYS.exams, []),
    todayClassroom: utils_storage.storage.get(config_env.STORAGE_KEYS.todayClassroom, null),
    tomorrowClassroom: utils_storage.storage.get(config_env.STORAGE_KEYS.tomorrowClassroom, null),
    academic: utils_storage.storage.get(config_env.STORAGE_KEYS.academic, {
      major: null,
      minor: null,
      rank: null,
      compulsoryRank: null
    }),
    loading: {},
    errors: {}
  }),
  actions: {
    setLoading(name, value) {
      this.loading[name] = value;
    },
    setError(name, error) {
      this.errors[name] = (error == null ? void 0 : error.authExpired) ? "" : (error == null ? void 0 : error.message) || "";
    },
    async loadHome() {
      this.setLoading("home", true);
      this.setError("home", null);
      try {
        const [courses, calendar, exams] = await Promise.all([
          api_index.campusApi.courses(),
          api_index.campusApi.calendar(),
          api_index.campusApi.exams()
        ]);
        this.courseMap = utils_course.normalizeCourseMap(courses);
        this.calendar = calendar;
        this.exams = Array.isArray(exams) ? exams : (exams == null ? void 0 : exams.exams) || [];
        utils_storage.storage.set(config_env.STORAGE_KEYS.courseMap, this.courseMap);
        utils_storage.storage.set(config_env.STORAGE_KEYS.calendar, this.calendar);
        utils_storage.storage.set(config_env.STORAGE_KEYS.exams, this.exams);
      } catch (error) {
        this.setError("home", error);
        throw error;
      } finally {
        this.setLoading("home", false);
      }
    },
    async loadClassrooms() {
      this.setLoading("classroom", true);
      this.setError("classroom", null);
      try {
        const [today, tomorrow] = await Promise.all([
          api_index.campusApi.classroomToday(),
          api_index.campusApi.classroomTomorrow()
        ]);
        this.todayClassroom = today;
        this.tomorrowClassroom = tomorrow;
        utils_storage.storage.set(config_env.STORAGE_KEYS.todayClassroom, today);
        utils_storage.storage.set(config_env.STORAGE_KEYS.tomorrowClassroom, tomorrow);
      } catch (error) {
        this.setError("classroom", error);
        throw error;
      } finally {
        this.setLoading("classroom", false);
      }
    },
    async loadAcademic(refresh = false) {
      this.setLoading("academic", true);
      this.setError("academic", null);
      try {
        const [major, minor, rank, compulsoryRank] = await Promise.all([
          api_index.campusApi.scores(refresh),
          api_index.campusApi.minorScores(refresh),
          api_index.campusApi.rank(refresh),
          api_index.campusApi.compulsoryRank(refresh)
        ]);
        this.academic = { major, minor, rank, compulsoryRank };
        utils_storage.storage.set(config_env.STORAGE_KEYS.academic, this.academic);
      } catch (error) {
        this.setError("academic", error);
        throw error;
      } finally {
        this.setLoading("academic", false);
      }
    }
  }
});
exports.useCampusStore = useCampusStore;
//# sourceMappingURL=../../.sourcemap/mp-weixin/stores/campus.js.map

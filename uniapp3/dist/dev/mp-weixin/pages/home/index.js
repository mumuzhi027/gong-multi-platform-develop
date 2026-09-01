"use strict";
const common_vendor = require("../../common/vendor.js");
const common_assets = require("../../common/assets.js");
const stores_auth = require("../../stores/auth.js");
const stores_campus = require("../../stores/campus.js");
const utils_date = require("../../utils/date.js");
const utils_course = require("../../utils/course.js");
if (!Math) {
  (AppNav + SideDrawer + CourseSheet)();
}
const AppNav = () => "../../components/AppNav.js";
const SideDrawer = () => "../../components/SideDrawer.js";
const CourseSheet = () => "../../components/CourseSheet.js";
const _sfc_main = {
  __name: "index",
  setup(__props) {
    const auth = stores_auth.useAuthStore();
    const campus = stores_campus.useCampusStore();
    const drawerVisible = common_vendor.ref(false);
    const selectedCourse = common_vendor.ref(null);
    const now = common_vendor.ref(/* @__PURE__ */ new Date());
    const week = common_vendor.computed(() => utils_date.calculateWeek(campus.calendar, now.value));
    const courses = common_vendor.computed(() => utils_course.todayCourses(campus.courseMap, week.value, now.value));
    const finished = common_vendor.computed(() => courses.value.filter((item) => utils_course.courseState(item, now.value) === "finished").length);
    const remaining = common_vendor.computed(() => Math.max(0, courses.value.length - finished.value));
    const progress = common_vendor.computed(() => courses.value.length ? finished.value / courses.value.length : 1);
    function parseDate(value) {
      if (!value)
        return null;
      const result = new Date(String(value).replace(" ", "T"));
      return Number.isNaN(result.getTime()) ? null : result;
    }
    const exams = common_vendor.computed(() => [...campus.exams || []].sort((a, b) => {
      var _a, _b, _c, _d;
      const aEnd = ((_a = parseDate(a.end_time)) == null ? void 0 : _a.getTime()) || Infinity;
      const bEnd = ((_b = parseDate(b.end_time)) == null ? void 0 : _b.getTime()) || Infinity;
      const aDone = aEnd <= now.value.getTime() ? 1 : 0;
      const bDone = bEnd <= now.value.getTime() ? 1 : 0;
      return aDone - bDone || (((_c = parseDate(a.start_time)) == null ? void 0 : _c.getTime()) || Infinity) - (((_d = parseDate(b.start_time)) == null ? void 0 : _d.getTime()) || Infinity);
    }));
    common_vendor.onLoad(() => {
      if (!auth.authenticated)
        return common_vendor.index.reLaunch({ url: "/pages/login/index" });
      refresh();
    });
    common_vendor.onShow(() => {
      now.value = /* @__PURE__ */ new Date();
    });
    common_vendor.onPullDownRefresh(async () => {
      await refresh(true);
      common_vendor.index.stopPullDownRefresh();
    });
    async function refresh(showToast = false) {
      if (auth.offline)
        return showToast && common_vendor.index.showToast({ title: "当前为离线模式", icon: "none" });
      try {
        await Promise.all([campus.loadHome(), auth.loadProfile().catch(() => null)]);
      } catch (error) {
        if (showToast && !error.authExpired)
          common_vendor.index.showToast({ title: error.message, icon: "none" });
      }
    }
    function examInfo(exam) {
      const start = parseDate(exam.start_time);
      const end = parseDate(exam.end_time);
      let countdown = "";
      if (start) {
        const today = new Date(now.value.getFullYear(), now.value.getMonth(), now.value.getDate());
        const examDay = new Date(start.getFullYear(), start.getMonth(), start.getDate());
        const days = Math.round((examDay - today) / 864e5);
        countdown = end && now.value >= end || days < 0 ? "已结束" : days === 0 ? "今天" : String(days);
      }
      const pad = (v) => String(v).padStart(2, "0");
      return {
        date: start ? `${start.getMonth() + 1}月${start.getDate()}日` : "",
        countdown,
        time: start && end ? `${pad(start.getHours())}:${pad(start.getMinutes())}-${pad(end.getHours())}:${pad(end.getMinutes())}` : exam.start_time || "无时间安排"
      };
    }
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_assets._imports_1$2,
        b: common_vendor.o(($event) => drawerVisible.value = true, "0f"),
        c: common_vendor.t(common_vendor.unref(utils_date.weekdayName)(now.value)),
        d: common_vendor.t(week.value),
        e: common_vendor.t(common_vendor.unref(utils_date.formatDate)(now.value)),
        f: common_vendor.t(courses.value.length),
        g: `${progress.value * 360}deg`,
        h: common_vendor.t(remaining.value),
        i: common_assets._imports_1,
        j: common_vendor.unref(campus).loading.home ? 1 : "",
        k: common_vendor.o(($event) => refresh(true), "4e"),
        l: common_assets._imports_1$1,
        m: common_vendor.o(($event) => common_vendor.index.navigateTo({
          url: "/pages/course/index"
        }), "f3"),
        n: common_vendor.unref(campus).errors.home && !courses.value.length
      }, common_vendor.unref(campus).errors.home && !courses.value.length ? {
        o: common_assets._imports_1,
        p: common_vendor.o(($event) => refresh(true), "cd")
      } : !courses.value.length ? {
        r: common_assets._imports_1$1,
        s: common_vendor.o(($event) => common_vendor.index.navigateTo({
          url: "/pages/course/index"
        }), "ef")
      } : {
        t: common_vendor.f(courses.value, (course, k0, i0) => {
          return {
            a: common_vendor.t(common_vendor.unref(utils_course.courseTimes)(course, now.value)[0]),
            b: common_vendor.t(common_vendor.unref(utils_course.courseTimes)(course, now.value)[1]),
            c: common_vendor.unref(utils_course.stateColor)(common_vendor.unref(utils_course.courseState)(course, now.value)),
            d: common_vendor.unref(utils_course.stateColor)(common_vendor.unref(utils_course.courseState)(course, now.value)),
            e: common_vendor.t(course.name),
            f: common_vendor.t(course.classroom),
            g: `${course.name}-${course.start_time}`,
            h: common_vendor.o(($event) => selectedCourse.value = course, `${course.name}-${course.start_time}`)
          };
        })
      }, {
        q: !courses.value.length,
        v: common_assets._imports_1,
        w: common_vendor.unref(campus).loading.home ? 1 : "",
        x: common_vendor.o(($event) => refresh(true), "c4"),
        y: !exams.value.length
      }, !exams.value.length ? {
        z: common_assets._imports_3,
        A: common_vendor.o(($event) => common_vendor.index.redirectTo({
          url: "/pages/academic/index"
        }), "91")
      } : {
        B: common_vendor.f(exams.value, (exam, k0, i0) => {
          return common_vendor.e({
            a: common_vendor.t(exam.name),
            b: common_vendor.t(examInfo(exam).date),
            c: common_vendor.t(examInfo(exam).countdown),
            d: /^\d+$/.test(examInfo(exam).countdown)
          }, /^\d+$/.test(examInfo(exam).countdown) ? {} : {}, {
            e: common_vendor.t(exam.location || "无地点安排"),
            f: common_vendor.t(examInfo(exam).time),
            g: `${exam.name}-${exam.start_time}`
          });
        }),
        C: common_assets._imports_2,
        D: common_assets._imports_3$1
      }, {
        E: common_vendor.p({
          current: "home"
        }),
        F: common_vendor.o(($event) => drawerVisible.value = false, "c6"),
        G: common_vendor.p({
          visible: drawerVisible.value
        }),
        H: common_vendor.o(($event) => selectedCourse.value = null, "aa"),
        I: common_vendor.p({
          course: selectedCourse.value
        })
      });
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-2c5296db"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/home/index.js.map

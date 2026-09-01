"use strict";
const common_vendor = require("../../common/vendor.js");
const stores_auth = require("../../stores/auth.js");
const stores_campus = require("../../stores/campus.js");
const utils_date = require("../../utils/date.js");
const utils_course = require("../../utils/course.js");
if (!Math) {
  (AppHeader + StateView + CourseSheet + AuthorizationDialog)();
}
const AppHeader = () => "../../components/AppHeader.js";
const StateView = () => "../../components/StateView.js";
const CourseSheet = () => "../../components/CourseSheet.js";
const AuthorizationDialog = () => "../../components/AuthorizationDialog.js";
const _sfc_main = {
  __name: "index",
  setup(__props) {
    const auth = stores_auth.useAuthStore();
    const campus = stores_campus.useCampusStore();
    const selectedCourse = common_vendor.ref(null);
    const currentWeek = common_vendor.ref(1);
    const showWeeks = common_vendor.ref(false);
    const swiperCurrent = common_vendor.ref(1);
    const swiperDuration = common_vendor.ref(280);
    const resettingSwiper = common_vendor.ref(false);
    const weeks = common_vendor.computed(() => {
      var _a;
      return Number(((_a = campus.calendar) == null ? void 0 : _a.weeks) || 20);
    });
    const courseCount = common_vendor.computed(() => utils_course.courseMapSize(campus.courseMap));
    const realWeek = common_vendor.computed(() => utils_date.calculateWeek(campus.calendar));
    const weekSlides = common_vendor.computed(() => [-1, 0, 1].map((offset, slot) => {
      var _a;
      const week = Math.min(weeks.value, Math.max(1, currentWeek.value + offset));
      const dates = utils_date.weekDates(campus.calendar, week);
      return {
        slot,
        week,
        dates,
        month: ((_a = dates[0]) == null ? void 0 : _a.getMonth()) + 1 || "",
        starts: utils_course.periodStarts(dates[0] || /* @__PURE__ */ new Date()),
        courses: utils_course.coursesForWeek(campus.courseMap, week)
      };
    }));
    common_vendor.onLoad(async () => {
      if (!auth.authenticated)
        return common_vendor.index.reLaunch({ url: "/pages/login/index" });
      if ((!courseCount.value || !campus.calendar) && !auth.offline)
        await campus.loadHome().catch(() => null);
      currentWeek.value = realWeek.value || 1;
    });
    common_vendor.onPullDownRefresh(async () => {
      if (!auth.offline)
        await campus.loadHome().catch(() => null);
      common_vendor.index.stopPullDownRefresh();
    });
    function endTime(start) {
      const [hour, minute] = start.split(":").map(Number);
      const total = hour * 60 + minute + 45;
      return `${String(Math.floor(total / 60)).padStart(2, "0")}:${String(total % 60).padStart(2, "0")}`;
    }
    function dateText(date) {
      return `${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
    }
    function isToday(date) {
      const now = /* @__PURE__ */ new Date();
      return date.toDateString() === now.toDateString();
    }
    function offsetFor(period) {
      return (period - 1) * 86 + (period >= 5 ? 20 : 0) + (period >= 9 ? 20 : 0);
    }
    function blockStyle(course) {
      var _a, _b;
      const duration = Math.max(1, Number(course.duration || 1));
      const length = Math.max(((_a = course.name) == null ? void 0 : _a.length) || 0, ((_b = course.classroom) == null ? void 0 : _b.length) || 0);
      const font = duration >= 3 ? 23 : duration === 2 ? 21 : length >= 11 ? 18 : length >= 8 ? 19 : 21;
      return { top: `${offsetFor(Number(course.start_time || 1)) + 6}rpx`, height: `${duration * 86 - 12}rpx`, backgroundColor: utils_course.courseColor(course.name), "--font": `${font}rpx` };
    }
    function selectWeek(value) {
      currentWeek.value = value;
      showWeeks.value = false;
    }
    async function onSwiperChange(event) {
      const page = Number(event.detail.current);
      if (resettingSwiper.value || page === 1)
        return;
      const target = Math.min(weeks.value, Math.max(1, currentWeek.value + (page > 1 ? 1 : -1)));
      if (target === currentWeek.value) {
        swiperDuration.value = 180;
        swiperCurrent.value = 1;
        return;
      }
      resettingSwiper.value = true;
      currentWeek.value = target;
      swiperDuration.value = 0;
      await common_vendor.nextTick$1();
      swiperCurrent.value = 1;
      setTimeout(() => {
        swiperDuration.value = 280;
        resettingSwiper.value = false;
      }, 40);
    }
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_vendor.p({
          back: true
        }),
        b: common_vendor.t(currentWeek.value),
        c: common_vendor.t(currentWeek.value === realWeek.value ? "本周" : "非本周"),
        d: currentWeek.value === realWeek.value ? 1 : "",
        e: common_vendor.o(($event) => showWeeks.value = !showWeeks.value, "1c"),
        f: showWeeks.value
      }, showWeeks.value ? {
        g: common_vendor.f(weeks.value, (value, k0, i0) => {
          return {
            a: common_vendor.t(value),
            b: `week-${value}`,
            c: value,
            d: common_vendor.n({
              active: value === currentWeek.value
            }),
            e: common_vendor.o(($event) => selectWeek(value), value)
          };
        }),
        h: `week-${currentWeek.value}`
      } : {}, {
        i: common_vendor.f(weekSlides.value, (slide, k0, i0) => {
          return {
            a: common_vendor.t(slide.month),
            b: common_vendor.f(common_vendor.unref(utils_course.DAYS), (day, index, i1) => {
              return {
                a: common_vendor.t(common_vendor.unref(utils_course.DAY_LABELS)[index]),
                b: common_vendor.t(dateText(slide.dates[index])),
                c: day,
                d: common_vendor.n({
                  today: isToday(slide.dates[index])
                })
              };
            }),
            c: common_vendor.f(slide.starts, (start, index, i1) => {
              return common_vendor.e({
                a: index === 4 || index === 8
              }, index === 4 || index === 8 ? {} : {}, {
                b: common_vendor.t(start),
                c: common_vendor.t(endTime(start)),
                d: start
              });
            }),
            d: common_vendor.f(common_vendor.unref(utils_course.DAYS), (day, index, i1) => {
              return {
                a: common_vendor.f(11, (period, k2, i2) => {
                  return common_vendor.e({
                    a: period === 5 || period === 9
                  }, period === 5 || period === 9 ? {} : {}, {
                    b: period
                  });
                }),
                b: common_vendor.f(slide.courses[day], (course, k2, i2) => {
                  return common_vendor.e({
                    a: common_vendor.t(course.name),
                    b: course.classroom
                  }, course.classroom ? {
                    c: common_vendor.t(course.classroom)
                  } : {}, {
                    d: `${course.name}-${course.start_time}`,
                    e: common_vendor.s(blockStyle(course)),
                    f: common_vendor.o(($event) => selectedCourse.value = course, `${course.name}-${course.start_time}`)
                  });
                }),
                c: day,
                d: common_vendor.n({
                  today: isToday(slide.dates[index])
                })
              };
            }),
            e: slide.slot
          };
        }),
        j: swiperCurrent.value,
        k: swiperDuration.value,
        l: common_vendor.o(onSwiperChange, "00"),
        m: common_vendor.o(common_vendor.unref(campus).loadHome, "6a"),
        n: common_vendor.p({
          loading: common_vendor.unref(campus).loading.home && !courseCount.value,
          error: common_vendor.unref(campus).errors.home,
          empty: !courseCount.value,
          ["empty-text"]: "暂无课程数据"
        }),
        o: common_vendor.o(($event) => selectedCourse.value = null, "df"),
        p: common_vendor.p({
          course: selectedCourse.value
        })
      });
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-c0ec5cf0"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/course/index.js.map

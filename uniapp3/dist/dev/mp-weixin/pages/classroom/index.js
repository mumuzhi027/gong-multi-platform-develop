"use strict";
const common_vendor = require("../../common/vendor.js");
const stores_auth = require("../../stores/auth.js");
const stores_campus = require("../../stores/campus.js");
const utils_date = require("../../utils/date.js");
if (!Math) {
  (StateView + AppNav)();
}
const AppNav = () => "../../components/AppNav.js";
const StateView = () => "../../components/StateView.js";
const _sfc_main = {
  __name: "index",
  setup(__props) {
    const auth = stores_auth.useAuthStore();
    const campus = stores_campus.useCampusStore();
    const dayIndex = common_vendor.ref(0);
    const location = common_vendor.ref("");
    const selectedPeriods = common_vendor.ref([]);
    const touchStartX = common_vendor.ref(0);
    const periodLabels = ["1-2", "3-4", "5-6", "7-8", "9-11"];
    function sourceFor(index) {
      return index === 0 ? campus.todayClassroom : campus.tomorrowClassroom;
    }
    function mapFor(index) {
      const source = sourceFor(index);
      const value = (source == null ? void 0 : source.classrooms) || source || {};
      return value && typeof value === "object" && !Array.isArray(value) ? value : {};
    }
    const classroomMap = common_vendor.computed(() => mapFor(dayIndex.value));
    const locations = common_vendor.computed(() => Object.keys(classroomMap.value).filter((key) => Array.isArray(classroomMap.value[key])));
    const filteredRooms = common_vendor.computed(() => {
      const rooms = classroomMap.value[location.value];
      if (!Array.isArray(rooms))
        return [];
      return rooms.filter((room) => {
        const status = Array.isArray(room == null ? void 0 : room.status) ? room.status : [];
        return selectedPeriods.value.every((period) => status[period] === "空");
      });
    });
    common_vendor.watch(locations, (values) => {
      if (!values.includes(location.value))
        location.value = values[0] || "";
    }, { immediate: true });
    common_vendor.onLoad(async () => {
      if (!auth.authenticated)
        return common_vendor.index.reLaunch({ url: "/pages/login/index" });
      if (!campus.todayClassroom && !auth.offline)
        await refresh();
    });
    common_vendor.onPullDownRefresh(async () => {
      await refresh();
      common_vendor.index.stopPullDownRefresh();
    });
    async function refresh() {
      if (auth.offline)
        return common_vendor.index.showToast({ title: "离线模式下无法刷新", icon: "none" });
      await campus.loadClassrooms().catch((error) => {
        if (!error.authExpired)
          common_vendor.index.showToast({ title: error.message, icon: "none" });
      });
    }
    function togglePeriod(index) {
      selectedPeriods.value = selectedPeriods.value.includes(index) ? selectedPeriods.value.filter((value) => value !== index) : [...selectedPeriods.value, index];
    }
    function switchDay(index) {
      if (index === dayIndex.value)
        return;
      dayIndex.value = index;
      location.value = locations.value[0] || "";
    }
    function onTouchStart(event) {
      var _a, _b;
      touchStartX.value = ((_b = (_a = event.changedTouches) == null ? void 0 : _a[0]) == null ? void 0 : _b.clientX) || 0;
    }
    function onTouchEnd(event) {
      var _a, _b;
      const endX = ((_b = (_a = event.changedTouches) == null ? void 0 : _a[0]) == null ? void 0 : _b.clientX) || 0;
      const distance = endX - touchStartX.value;
      if (distance < -60)
        switchDay(1);
      if (distance > 60)
        switchDay(0);
    }
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_vendor.t(common_vendor.unref(utils_date.dateLabel)(dayIndex.value)),
        b: common_vendor.t(dayIndex.value === 0 ? "今天" : "明天"),
        c: common_vendor.o(($event) => switchDay(0), "e8"),
        d: common_vendor.o(($event) => switchDay(1), "ce"),
        e: common_vendor.n({
          right: dayIndex.value === 1
        }),
        f: common_vendor.f(locations.value, (item, k0, i0) => {
          return {
            a: common_vendor.t(item),
            b: item,
            c: common_vendor.n({
              active: item === location.value
            }),
            d: common_vendor.o(($event) => location.value = item, item)
          };
        }),
        g: common_vendor.f(periodLabels, (label, index, i0) => {
          return {
            a: common_vendor.t(label),
            b: label,
            c: common_vendor.n({
              active: selectedPeriods.value.includes(index)
            }),
            d: common_vendor.o(($event) => togglePeriod(index), label)
          };
        }),
        h: common_vendor.f(filteredRooms.value, (room, roomIndex, i0) => {
          return {
            a: common_vendor.t(room.name),
            b: common_vendor.f(room.status, (status, index, i1) => {
              return {
                a: index,
                b: common_vendor.n(status === "空" ? "free" : "busy")
              };
            }),
            c: `${room.name}-${roomIndex}`
          };
        }),
        i: !filteredRooms.value.length
      }, !filteredRooms.value.length ? {} : {}, {
        j: common_vendor.o(onTouchStart, "f7"),
        k: common_vendor.o(onTouchEnd, "18"),
        l: common_vendor.o(refresh, "cd"),
        m: common_vendor.p({
          loading: common_vendor.unref(campus).loading.classroom && !locations.value.length,
          error: common_vendor.unref(campus).errors.classroom,
          empty: !locations.value.length,
          ["empty-text"]: "当前暂无空教室数据"
        }),
        n: common_vendor.p({
          current: "classroom"
        })
      });
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-efa48afc"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/classroom/index.js.map

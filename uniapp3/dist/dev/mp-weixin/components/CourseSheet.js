"use strict";
const common_vendor = require("../common/vendor.js");
const common_assets = require("../common/assets.js");
const _sfc_main = {
  __name: "CourseSheet",
  props: { course: { type: Object, default: null } },
  emits: ["close"],
  setup(__props, { emit: __emit }) {
    const emit = __emit;
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: __props.course
      }, __props.course ? {
        b: common_vendor.t(__props.course.name),
        c: common_assets._imports_0$5,
        d: common_vendor.t(__props.course.teacher || "教师待定"),
        e: common_assets._imports_1$1,
        f: common_vendor.t(__props.course.weeks || "-"),
        g: common_assets._imports_2,
        h: common_vendor.t(__props.course.classroom || "地点待定"),
        i: common_assets._imports_3$1,
        j: common_vendor.t(__props.course.start_time),
        k: common_vendor.t(Number(__props.course.start_time) + Number(__props.course.duration || 1) - 1),
        l: common_vendor.o(() => {
        }, "76"),
        m: common_vendor.o(($event) => emit("close"), "65")
      } : {});
    };
  }
};
const Component = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-ac568c0a"]]);
wx.createComponent(Component);
//# sourceMappingURL=../../.sourcemap/mp-weixin/components/CourseSheet.js.map

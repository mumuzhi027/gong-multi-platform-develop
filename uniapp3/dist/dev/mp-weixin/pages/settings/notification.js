"use strict";
const common_vendor = require("../../common/vendor.js");
const _sfc_main = {
  __name: "notification",
  setup(__props) {
    const course = common_vendor.ref(common_vendor.index.getStorageSync("gong_course_reminder") !== false);
    const exam = common_vendor.ref(common_vendor.index.getStorageSync("gong_exam_reminder") !== false);
    function change(name, event) {
      const value = event.detail.value;
      if (name === "course")
        course.value = value;
      else
        exam.value = value;
      common_vendor.index.setStorageSync(name === "course" ? "gong_course_reminder" : "gong_exam_reminder", value);
    }
    return (_ctx, _cache) => {
      return {
        a: course.value,
        b: common_vendor.o(($event) => change("course", $event), "6b"),
        c: exam.value,
        d: common_vendor.o(($event) => change("exam", $event), "31")
      };
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-0fb49f47"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/settings/notification.js.map

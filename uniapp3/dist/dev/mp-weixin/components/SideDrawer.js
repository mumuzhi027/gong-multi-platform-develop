"use strict";
const common_vendor = require("../common/vendor.js");
const common_assets = require("../common/assets.js");
const stores_auth = require("../stores/auth.js");
const _sfc_main = {
  __name: "SideDrawer",
  props: { visible: Boolean },
  emits: ["close"],
  setup(__props, { emit: __emit }) {
    const emit = __emit;
    const auth = stores_auth.useAuthStore();
    const name = common_vendor.computed(() => {
      var _a;
      return ((_a = auth.profile) == null ? void 0 : _a.name) || (auth.offline ? "离线用户" : "同学");
    });
    const major = common_vendor.computed(() => {
      var _a, _b;
      return ((_a = auth.profile) == null ? void 0 : _a.major) || ((_b = auth.profile) == null ? void 0 : _b.major_name) || "";
    });
    const studentId = common_vendor.computed(() => {
      var _a, _b, _c;
      return ((_a = auth.profile) == null ? void 0 : _a.student_id) || ((_b = auth.profile) == null ? void 0 : _b.studentID) || ((_c = auth.profile) == null ? void 0 : _c.studentId) || "";
    });
    function open(url) {
      emit("close");
      common_vendor.index.navigateTo({ url });
    }
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: __props.visible
      }, __props.visible ? {
        b: common_vendor.o(($event) => emit("close"), "a5"),
        c: common_vendor.t(name.value),
        d: common_vendor.t(major.value || "专业信息暂无"),
        e: common_vendor.t(studentId.value || "学号信息暂无"),
        f: common_assets._imports_0$4,
        g: common_vendor.o(($event) => open("/pages/settings/index"), "3f"),
        h: common_assets._imports_1$4,
        i: common_vendor.o(($event) => open("/pages/about/index"), "83"),
        j: common_vendor.o((...args) => common_vendor.unref(auth).logout && common_vendor.unref(auth).logout(...args), "7f")
      } : {});
    };
  }
};
const Component = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-ce0f97bc"]]);
wx.createComponent(Component);
//# sourceMappingURL=../../.sourcemap/mp-weixin/components/SideDrawer.js.map

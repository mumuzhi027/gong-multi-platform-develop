"use strict";
const common_vendor = require("../common/vendor.js");
const common_assets = require("../common/assets.js");
const _sfc_main = {
  __name: "AppHeader",
  props: {
    title: { type: String, default: "" },
    back: { type: Boolean, default: false }
  },
  emits: ["menu"],
  setup(__props, { emit: __emit }) {
    const emit = __emit;
    function goBack() {
      const pages = getCurrentPages();
      if (pages.length > 1)
        common_vendor.index.navigateBack();
      else
        common_vendor.index.redirectTo({ url: "/pages/home/index" });
    }
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: __props.back
      }, __props.back ? {
        b: common_assets._imports_0$6,
        c: common_vendor.o(goBack, "71")
      } : {
        d: common_assets._imports_1$2,
        e: common_vendor.o(($event) => emit("menu"), "a3")
      }, {
        f: __props.title
      }, __props.title ? {
        g: common_vendor.t(__props.title)
      } : {});
    };
  }
};
const Component = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-48a13218"]]);
wx.createComponent(Component);
//# sourceMappingURL=../../.sourcemap/mp-weixin/components/AppHeader.js.map

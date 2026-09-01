"use strict";
const common_vendor = require("../common/vendor.js");
const common_assets = require("../common/assets.js");
const _sfc_main = {
  __name: "StateView",
  props: {
    loading: Boolean,
    error: { type: String, default: "" },
    empty: Boolean,
    emptyText: { type: String, default: "暂无数据" }
  },
  emits: ["retry"],
  setup(__props, { emit: __emit }) {
    const emit = __emit;
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: __props.loading
      }, __props.loading ? {} : __props.error ? {
        c: common_assets._imports_0$7,
        d: common_vendor.t(__props.error),
        e: common_vendor.o(($event) => emit("retry"), "69")
      } : __props.empty ? {
        g: common_assets._imports_1$5,
        h: common_vendor.t(__props.emptyText)
      } : {}, {
        b: __props.error,
        f: __props.empty
      });
    };
  }
};
const Component = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-5e0bb669"]]);
wx.createComponent(Component);
//# sourceMappingURL=../../.sourcemap/mp-weixin/components/StateView.js.map

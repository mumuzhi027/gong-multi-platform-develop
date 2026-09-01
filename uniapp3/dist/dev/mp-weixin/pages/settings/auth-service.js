"use strict";
const common_vendor = require("../../common/vendor.js");
const _sfc_main = {
  __name: "auth-service",
  setup(__props) {
    const enabled = common_vendor.ref(common_vendor.index.getStorageSync("gong_reauthentication") !== false);
    function change(event) {
      enabled.value = event.detail.value;
      common_vendor.index.setStorageSync("gong_reauthentication", enabled.value);
    }
    return (_ctx, _cache) => {
      return {
        a: enabled.value,
        b: common_vendor.o(change, "8b")
      };
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-4c734b27"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/settings/auth-service.js.map

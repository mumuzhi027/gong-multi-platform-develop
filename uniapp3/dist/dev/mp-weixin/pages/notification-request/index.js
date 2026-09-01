"use strict";
const common_vendor = require("../../common/vendor.js");
const common_assets = require("../../common/assets.js");
const config_env = require("../../config/env.js");
const _sfc_main = {
  __name: "index",
  setup(__props) {
    function enter() {
      common_vendor.index.reLaunch({ url: "/pages/home/index" });
    }
    function requestNotification() {
      if (!config_env.WECHAT_SUBSCRIBE_TEMPLATE_IDS.length) {
        common_vendor.index.showModal({ title: "开启通知", content: "当前小程序尚未配置订阅消息模板，可稍后在设置中开启。", showCancel: false, success: enter });
        return;
      }
      common_vendor.index.requestSubscribeMessage({ tmplIds: config_env.WECHAT_SUBSCRIBE_TEMPLATE_IDS, complete: enter });
    }
    return (_ctx, _cache) => {
      return {
        a: common_assets._imports_0$1,
        b: common_vendor.o(requestNotification, "10"),
        c: common_vendor.o(enter, "f2")
      };
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-3caad637"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/notification-request/index.js.map

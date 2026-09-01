"use strict";
const common_vendor = require("../../common/vendor.js");
const common_assets = require("../../common/assets.js");
const config_env = require("../../config/env.js");
const _sfc_main = {
  __name: "index",
  setup(__props) {
    const menus = [
      { icon: "privacy_policy", title: "隐私政策", desc: "查看我们如何收集、使用、存储和保护您的个人信息", type: "privacyPolicy" },
      { icon: "permissions", title: "权限申请与使用情况说明", desc: "查看我们申请的权限及其使用情况", type: "permissionsRequest" },
      { icon: "user_clause", title: "用户协议", desc: "使用拱拱之前，请务必仔细阅读并同意我们的用户协议", type: "userClause" }
    ];
    function open(item) {
      common_vendor.index.navigateTo({ url: `/pages/clause/index?type=${item.type}&title=${encodeURIComponent(item.title)}` });
    }
    return (_ctx, _cache) => {
      return {
        a: common_assets._imports_0$3,
        b: common_vendor.t(common_vendor.unref(config_env.APP_VERSION)),
        c: common_vendor.f(menus, (item, k0, i0) => {
          return {
            a: `/static/icons/${item.icon}.png`,
            b: common_vendor.t(item.title),
            c: common_vendor.t(item.desc),
            d: item.type,
            e: common_vendor.o(($event) => open(item), item.type)
          };
        })
      };
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-6b4e7e2d"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/about/index.js.map

"use strict";
const common_vendor = require("../../common/vendor.js");
const common_assets = require("../../common/assets.js");
const _sfc_main = {
  __name: "index",
  setup(__props) {
    function checkUpdate() {
      if (!common_vendor.index.canIUse("getUpdateManager"))
        return common_vendor.index.showToast({ title: "当前微信版本不支持更新检查", icon: "none" });
      const manager = common_vendor.index.getUpdateManager();
      manager.onCheckForUpdate(({ hasUpdate }) => {
        if (!hasUpdate)
          common_vendor.index.showToast({ title: "已是最新版本", icon: "success" });
      });
      manager.onUpdateReady(() => common_vendor.index.showModal({ title: "更新提示", content: "新版本已准备好，是否立即重启？", success: ({ confirm }) => confirm && manager.applyUpdate() }));
      manager.onUpdateFailed(() => common_vendor.index.showToast({ title: "更新下载失败", icon: "none" }));
    }
    return (_ctx, _cache) => {
      return {
        a: common_assets._imports_0$2,
        b: common_vendor.o(($event) => common_vendor.index.navigateTo({
          url: "/pages/settings/notification"
        }), "4b"),
        c: common_assets._imports_1$3,
        d: common_vendor.o(($event) => common_vendor.index.navigateTo({
          url: "/pages/settings/auth-service"
        }), "11"),
        e: common_assets._imports_2$1,
        f: common_vendor.o(checkUpdate, "bd")
      };
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-b4180827"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/settings/index.js.map

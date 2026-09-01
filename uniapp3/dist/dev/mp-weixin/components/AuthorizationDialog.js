"use strict";
const common_vendor = require("../common/vendor.js");
const utils_authExpiry = require("../utils/auth-expiry.js");
const stores_auth = require("../stores/auth.js");
const _sfc_main = {
  __name: "AuthorizationDialog",
  setup(__props) {
    const auth = stores_auth.useAuthStore();
    const password = common_vendor.ref("");
    const passwordVisible = common_vendor.ref(false);
    const localError = common_vendor.ref("");
    const username = common_vendor.computed(() => {
      var _a, _b;
      return auth.username || ((_a = auth.profile) == null ? void 0 : _a.student_id) || ((_b = auth.profile) == null ? void 0 : _b.studentID) || "当前账号";
    });
    common_vendor.watch(() => utils_authExpiry.authExpiry.visible, (visible) => {
      if (!visible)
        return;
      if (common_vendor.index.getStorageSync("gong_reauthentication") === false) {
        auth.logout();
        return;
      }
      auth.markExpired();
      password.value = "";
      localError.value = "";
    }, { immediate: true });
    async function verify() {
      localError.value = "";
      try {
        await auth.reauthenticate(password.value);
        password.value = "";
        common_vendor.index.showToast({ title: "验证成功", icon: "success" });
        setTimeout(() => common_vendor.index.startPullDownRefresh(), 250);
      } catch (error) {
        localError.value = error.message || "验证失败";
      }
    }
    function switchAccount() {
      auth.logout();
    }
    function enterOffline() {
      auth.enterOffline();
      password.value = "";
      common_vendor.index.showToast({ title: "已进入离线模式", icon: "none" });
    }
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_vendor.unref(utils_authExpiry.authExpiry).visible
      }, common_vendor.unref(utils_authExpiry.authExpiry).visible ? {
        b: common_vendor.t(username.value),
        c: !passwordVisible.value,
        d: common_vendor.o(verify, "23"),
        e: password.value,
        f: common_vendor.o(($event) => password.value = $event.detail.value, "cc"),
        g: passwordVisible.value ? "/static/icons/password_visible.png" : "/static/icons/password_invisible.png",
        h: common_vendor.o(($event) => passwordVisible.value = !passwordVisible.value, "05"),
        i: common_vendor.unref(auth).reauthLoading,
        j: common_vendor.unref(auth).reauthLoading,
        k: common_vendor.o(verify, "81"),
        l: common_vendor.t(localError.value || common_vendor.unref(auth).reauthError),
        m: common_vendor.o(switchAccount, "e2"),
        n: common_vendor.o(enterOffline, "f6")
      } : {});
    };
  }
};
const Component = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-ff2bde40"]]);
wx.createComponent(Component);
//# sourceMappingURL=../../.sourcemap/mp-weixin/components/AuthorizationDialog.js.map

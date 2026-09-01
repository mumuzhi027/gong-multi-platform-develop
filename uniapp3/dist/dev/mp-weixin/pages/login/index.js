"use strict";
const common_vendor = require("../../common/vendor.js");
const common_assets = require("../../common/assets.js");
const stores_auth = require("../../stores/auth.js");
const _sfc_main = {
  __name: "index",
  setup(__props) {
    const auth = stores_auth.useAuthStore();
    const username = common_vendor.ref("");
    const password = common_vendor.ref("");
    const passwordVisible = common_vendor.ref(false);
    const error = common_vendor.ref("");
    const canSubmit = common_vendor.computed(() => username.value.trim() && password.value);
    common_vendor.onLoad(() => {
      if (auth.authenticated)
        common_vendor.index.reLaunch({ url: "/pages/home/index" });
    });
    async function submit() {
      if (!canSubmit.value || auth.loading)
        return;
      error.value = "";
      try {
        await auth.login(username.value.trim(), password.value);
        common_vendor.index.reLaunch({ url: "/pages/notification-request/index" });
      } catch (reason) {
        error.value = reason.message || "登录失败";
        setTimeout(() => {
          error.value = "";
        }, 2800);
      }
    }
    function enterOffline() {
      auth.enterOffline();
      common_vendor.index.reLaunch({ url: "/pages/home/index" });
    }
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: error.value
      }, error.value ? {
        b: common_vendor.t(error.value)
      } : {}, {
        c: common_assets._imports_0,
        d: username.value,
        e: common_vendor.o(($event) => username.value = $event.detail.value, "39"),
        f: !passwordVisible.value,
        g: common_vendor.o(submit, "13"),
        h: password.value,
        i: common_vendor.o(($event) => password.value = $event.detail.value, "1a"),
        j: passwordVisible.value ? "/static/icons/password_visible.png" : "/static/icons/password_invisible.png",
        k: common_vendor.o(($event) => passwordVisible.value = !passwordVisible.value, "67"),
        l: !canSubmit.value ? 1 : "",
        m: common_vendor.unref(auth).loading,
        n: !canSubmit.value || common_vendor.unref(auth).loading,
        o: common_vendor.o(submit, "09"),
        p: common_vendor.o(enterOffline, "eb")
      });
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-45258083"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/login/index.js.map

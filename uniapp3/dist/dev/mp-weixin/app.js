"use strict";
Object.defineProperty(exports, Symbol.toStringTag, { value: "Module" });
const common_vendor = require("./common/vendor.js");
if (!Math) {
  "./pages/login/index.js";
  "./pages/home/index.js";
  "./pages/notification-request/index.js";
  "./pages/course/index.js";
  "./pages/classroom/index.js";
  "./pages/academic/index.js";
  "./pages/settings/index.js";
  "./pages/settings/notification.js";
  "./pages/settings/auth-service.js";
  "./pages/about/index.js";
  "./pages/clause/index.js";
}
const _sfc_main = {
  onLaunch() {
    const savedTheme = common_vendor.index.getStorageSync("gong_theme") || "light";
    common_vendor.index.setStorageSync("gong_theme", savedTheme);
  }
};
function createApp() {
  const app = common_vendor.createSSRApp(_sfc_main);
  app.use(common_vendor.createPinia());
  return { app };
}
createApp().app.mount("#app");
exports.createApp = createApp;
//# sourceMappingURL=../.sourcemap/mp-weixin/app.js.map

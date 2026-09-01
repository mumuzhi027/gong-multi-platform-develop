"use strict";
const common_vendor = require("../common/vendor.js");
const authExpiry = common_vendor.reactive({
  visible: false,
  sequence: 0
});
function notifyAuthExpired() {
  authExpiry.sequence += 1;
  authExpiry.visible = true;
}
function clearAuthExpired() {
  authExpiry.visible = false;
}
exports.authExpiry = authExpiry;
exports.clearAuthExpired = clearAuthExpired;
exports.notifyAuthExpired = notifyAuthExpired;
//# sourceMappingURL=../../.sourcemap/mp-weixin/utils/auth-expiry.js.map

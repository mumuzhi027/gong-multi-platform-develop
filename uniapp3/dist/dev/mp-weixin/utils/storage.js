"use strict";
const common_vendor = require("../common/vendor.js");
const storage = {
  get(key, fallback = null) {
    try {
      const value = common_vendor.index.getStorageSync(key);
      return value === "" || value === void 0 ? fallback : value;
    } catch {
      return fallback;
    }
  },
  set(key, value) {
    common_vendor.index.setStorageSync(key, value);
  },
  remove(key) {
    common_vendor.index.removeStorageSync(key);
  }
};
exports.storage = storage;
//# sourceMappingURL=../../.sourcemap/mp-weixin/utils/storage.js.map

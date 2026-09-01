"use strict";
const common_vendor = require("../common/vendor.js");
const config_env = require("../config/env.js");
const utils_storage = require("../utils/storage.js");
const utils_authExpiry = require("../utils/auth-expiry.js");
function messageFrom(data, fallback) {
  return (data == null ? void 0 : data.detail) || (data == null ? void 0 : data.message) || (data == null ? void 0 : data.msg) || fallback;
}
function request(options) {
  const token = utils_storage.storage.get(config_env.STORAGE_KEYS.token, "");
  const headers = {
    "content-type": "application/json",
    ...options.header || {}
  };
  if (token && options.auth !== false)
    headers.Authorization = `Bearer ${token}`;
  return new Promise((resolve, reject) => {
    common_vendor.index.request({
      url: `${config_env.API_BASE_URL}${options.url}`,
      method: options.method || "GET",
      data: options.data,
      header: headers,
      timeout: 15e3,
      success(response) {
        if (response.statusCode >= 200 && response.statusCode < 300) {
          resolve(response.data);
          return;
        }
        if (response.statusCode === 401) {
          if (options.auth !== false) {
            utils_storage.storage.remove(config_env.STORAGE_KEYS.token);
            utils_authExpiry.notifyAuthExpired();
            const error = new Error("");
            error.authExpired = true;
            reject(error);
          } else {
            reject(new Error(messageFrom(response.data, "用户名或密码错误")));
          }
          return;
        }
        reject(new Error(messageFrom(response.data, `请求失败（${response.statusCode}）`)));
      },
      fail(error) {
        var _a;
        reject(new Error(((_a = error.errMsg) == null ? void 0 : _a.replace("request:fail", "").trim()) || "网络连接失败"));
      }
    });
  });
}
function formRequest(url, data) {
  return request({
    url,
    method: "POST",
    data,
    auth: false,
    header: { "content-type": "application/x-www-form-urlencoded" }
  });
}
function unwrap(payload) {
  return (payload == null ? void 0 : payload.data) ?? payload;
}
exports.formRequest = formRequest;
exports.request = request;
exports.unwrap = unwrap;
//# sourceMappingURL=../../.sourcemap/mp-weixin/api/request.js.map

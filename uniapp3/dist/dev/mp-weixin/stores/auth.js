"use strict";
const common_vendor = require("../common/vendor.js");
const api_index = require("../api/index.js");
const config_env = require("../config/env.js");
const utils_storage = require("../utils/storage.js");
const utils_authExpiry = require("../utils/auth-expiry.js");
const useAuthStore = common_vendor.defineStore("auth", {
  state: () => ({
    token: utils_storage.storage.get(config_env.STORAGE_KEYS.token, ""),
    offline: Boolean(utils_storage.storage.get(config_env.STORAGE_KEYS.offline, false)),
    profile: utils_storage.storage.get(config_env.STORAGE_KEYS.profile, null),
    username: utils_storage.storage.get(config_env.STORAGE_KEYS.username, ""),
    loading: false,
    reauthLoading: false,
    reauthError: ""
  }),
  getters: {
    authenticated: (state) => Boolean(state.token) || state.offline
  },
  actions: {
    async login(username, password) {
      this.loading = true;
      try {
        const result = await api_index.login(username, password);
        const token = (result == null ? void 0 : result.access_token) || (result == null ? void 0 : result.accessToken) || (result == null ? void 0 : result.token);
        if (!token)
          throw new Error("服务器未返回有效登录凭证");
        this.token = token;
        this.username = username;
        this.offline = false;
        utils_storage.storage.set(config_env.STORAGE_KEYS.token, token);
        utils_storage.storage.set(config_env.STORAGE_KEYS.username, username);
        utils_storage.storage.set(config_env.STORAGE_KEYS.offline, false);
        await this.loadProfile().catch(() => null);
      } finally {
        this.loading = false;
      }
    },
    enterOffline() {
      this.offline = true;
      this.token = "";
      utils_storage.storage.remove(config_env.STORAGE_KEYS.token);
      utils_storage.storage.set(config_env.STORAGE_KEYS.offline, true);
      utils_authExpiry.clearAuthExpired();
    },
    markExpired() {
      this.token = "";
      this.reauthError = "";
      utils_storage.storage.remove(config_env.STORAGE_KEYS.token);
    },
    async reauthenticate(password) {
      var _a, _b;
      const username = this.username || ((_a = this.profile) == null ? void 0 : _a.student_id) || ((_b = this.profile) == null ? void 0 : _b.studentID) || "";
      if (!username)
        throw new Error("未找到原登录账号，请切换账号重新登录");
      if (!password)
        throw new Error("请输入密码");
      this.reauthLoading = true;
      this.reauthError = "";
      try {
        const result = await api_index.login(username, password);
        const token = (result == null ? void 0 : result.access_token) || (result == null ? void 0 : result.accessToken) || (result == null ? void 0 : result.token);
        if (!token)
          throw new Error("服务器未返回有效登录凭证");
        this.token = token;
        this.offline = false;
        utils_storage.storage.set(config_env.STORAGE_KEYS.token, token);
        utils_storage.storage.set(config_env.STORAGE_KEYS.offline, false);
        utils_authExpiry.clearAuthExpired();
        await this.loadProfile().catch(() => null);
      } catch (error) {
        this.reauthError = error.message || "验证失败";
        throw error;
      } finally {
        this.reauthLoading = false;
      }
    },
    async loadProfile() {
      if (this.offline)
        return this.profile;
      this.profile = await api_index.campusApi.profile();
      utils_storage.storage.set(config_env.STORAGE_KEYS.profile, this.profile);
      return this.profile;
    },
    logout() {
      this.token = "";
      this.offline = false;
      this.username = "";
      utils_authExpiry.clearAuthExpired();
      utils_storage.storage.remove(config_env.STORAGE_KEYS.token);
      utils_storage.storage.remove(config_env.STORAGE_KEYS.offline);
      utils_storage.storage.remove(config_env.STORAGE_KEYS.username);
      common_vendor.index.reLaunch({ url: "/pages/login/index" });
    }
  }
});
exports.useAuthStore = useAuthStore;
//# sourceMappingURL=../../.sourcemap/mp-weixin/stores/auth.js.map

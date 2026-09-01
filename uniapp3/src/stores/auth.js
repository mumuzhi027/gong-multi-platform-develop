import { defineStore } from 'pinia'
import { login as loginApi, campusApi } from '../api'
import { STORAGE_KEYS } from '../config/env'
import { storage } from '../utils/storage'
import { clearAuthExpired } from '../utils/auth-expiry'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: storage.get(STORAGE_KEYS.token, ''),
    offline: Boolean(storage.get(STORAGE_KEYS.offline, false)),
    profile: storage.get(STORAGE_KEYS.profile, null),
    username: storage.get(STORAGE_KEYS.username, ''),
    loading: false,
    reauthLoading: false,
    reauthError: '',
  }),
  getters: {
    authenticated: (state) => Boolean(state.token) || state.offline,
  },
  actions: {
    async login(username, password) {
      this.loading = true
      try {
        const result = await loginApi(username, password)
        const token = result?.access_token || result?.accessToken || result?.token
        if (!token) throw new Error('服务器未返回有效登录凭证')
        this.token = token
        this.username = username
        this.offline = false
        storage.set(STORAGE_KEYS.token, token)
        storage.set(STORAGE_KEYS.username, username)
        storage.set(STORAGE_KEYS.offline, false)
        await this.loadProfile().catch(() => null)
      } finally {
        this.loading = false
      }
    },
    enterOffline() {
      this.offline = true
      this.token = ''
      storage.remove(STORAGE_KEYS.token)
      storage.set(STORAGE_KEYS.offline, true)
      clearAuthExpired()
    },
    markExpired() {
      this.token = ''
      this.reauthError = ''
      storage.remove(STORAGE_KEYS.token)
    },
    async reauthenticate(password) {
      const username = this.username || this.profile?.student_id || this.profile?.studentID || ''
      if (!username) throw new Error('未找到原登录账号，请切换账号重新登录')
      if (!password) throw new Error('请输入密码')
      this.reauthLoading = true
      this.reauthError = ''
      try {
        const result = await loginApi(username, password)
        const token = result?.access_token || result?.accessToken || result?.token
        if (!token) throw new Error('服务器未返回有效登录凭证')
        this.token = token
        this.offline = false
        storage.set(STORAGE_KEYS.token, token)
        storage.set(STORAGE_KEYS.offline, false)
        clearAuthExpired()
        await this.loadProfile().catch(() => null)
      } catch (error) {
        this.reauthError = error.message || '验证失败'
        throw error
      } finally {
        this.reauthLoading = false
      }
    },
    async loadProfile() {
      if (this.offline) return this.profile
      this.profile = await campusApi.profile()
      storage.set(STORAGE_KEYS.profile, this.profile)
      return this.profile
    },
    logout() {
      this.token = ''
      this.offline = false
      this.username = ''
      clearAuthExpired()
      storage.remove(STORAGE_KEYS.token)
      storage.remove(STORAGE_KEYS.offline)
      storage.remove(STORAGE_KEYS.username)
      uni.reLaunch({ url: '/pages/login/index' })
    },
  },
})

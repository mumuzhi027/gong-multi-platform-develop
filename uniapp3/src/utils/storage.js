export const storage = {
  get(key, fallback = null) {
    try {
      const value = uni.getStorageSync(key)
      return value === '' || value === undefined ? fallback : value
    } catch {
      return fallback
    }
  },
  set(key, value) {
    uni.setStorageSync(key, value)
  },
  remove(key) {
    uni.removeStorageSync(key)
  },
}

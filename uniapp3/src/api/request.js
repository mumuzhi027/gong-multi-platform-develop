import { API_BASE_URL, STORAGE_KEYS } from '../config/env'
import { storage } from '../utils/storage'
import { notifyAuthExpired } from '../utils/auth-expiry'

function messageFrom(data, fallback) {
  return data?.detail || data?.message || data?.msg || fallback
}

export function request(options) {
  const token = storage.get(STORAGE_KEYS.token, '')
  const headers = {
    'content-type': 'application/json',
    ...(options.header || {}),
  }
  if (token && options.auth !== false) headers.Authorization = `Bearer ${token}`

  return new Promise((resolve, reject) => {
    uni.request({
      url: `${API_BASE_URL}${options.url}`,
      method: options.method || 'GET',
      data: options.data,
      header: headers,
      timeout: 15000,
      success(response) {
        if (response.statusCode >= 200 && response.statusCode < 300) {
          resolve(response.data)
          return
        }
        if (response.statusCode === 401) {
          if (options.auth !== false) {
            storage.remove(STORAGE_KEYS.token)
            notifyAuthExpired()
            const error = new Error('')
            error.authExpired = true
            reject(error)
          } else {
            reject(new Error(messageFrom(response.data, '登录校验未通过，请核对输入或稍后重试')))
          }
          return
        }
        reject(new Error(messageFrom(response.data, `请求失败（${response.statusCode}）`)))
      },
      fail(error) {
        reject(new Error(error.errMsg?.replace('request:fail', '').trim() || '网络连接失败'))
      },
    })
  })
}

export function formRequest(url, data) {
  return request({
    url,
    method: 'POST',
    data,
    auth: false,
    header: { 'content-type': 'application/x-www-form-urlencoded' },
  })
}

export function unwrap(payload) {
  return payload?.data ?? payload
}

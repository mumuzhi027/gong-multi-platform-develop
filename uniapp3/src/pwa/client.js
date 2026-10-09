import { reactive } from 'vue'
import { rememberMainLayout, showLayoutFeedback } from './layout-feedback'
import { syncIosViewport } from './ios-viewport'

export { rememberMainLayout, showLayoutFeedback }

export const pwaState = reactive({ installed: false, installable: false, updating: false })
let registrationPromise
let installPrompt
let reloadOnActivation = false
let lastPromptedWorker

export function isIos() {
  return /iPad|iPhone|iPod/.test(navigator.userAgent) ||
    (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1)
}

function offerUpdate(registration) {
  const worker = registration.waiting
  if (!worker || worker === lastPromptedWorker || !navigator.serviceWorker.controller) return
  lastPromptedWorker = worker
  uni.showModal({
    title: '更新提示', content: '新版本已准备好，是否立即重启？',
    confirmText: '立即更新', cancelText: '以后再说',
    success: ({ confirm }) => {
      if (!confirm) return
      reloadOnActivation = true
      worker.postMessage({ type: 'APPLY_UPDATE' })
    },
  })
}

export function initPwa() {
  if (registrationPromise) return registrationPromise
  const displayMode = window.matchMedia('(display-mode: standalone)')
  const syncMode = () => {
    pwaState.installed = displayMode.matches || navigator.standalone === true
    document.documentElement.toggleAttribute('data-ios-standalone', isIos() && pwaState.installed)
    const guard = document.querySelector('.pwa-status-background')
    if (isIos() && pwaState.installed && !guard) {
      const element = document.createElement('div')
      element.className = 'pwa-status-background'
      element.setAttribute('aria-hidden', 'true')
      document.body.prepend(element)
    } else if (!isIos() || !pwaState.installed) guard?.remove()
    syncIosViewport(isIos() && pwaState.installed)
  }
  syncMode()
  displayMode.addEventListener?.('change', syncMode)
  let viewportFrame
  const scheduleViewportSync = () => {
    cancelAnimationFrame(viewportFrame)
    viewportFrame = requestAnimationFrame(() => {
      syncIosViewport(isIos() && pwaState.installed)
      rememberMainLayout()
    })
  }
  window.addEventListener('resize', scheduleViewportSync)
  window.addEventListener('orientationchange', scheduleViewportSync)
  window.addEventListener('pageshow', scheduleViewportSync)
  window.visualViewport?.addEventListener('resize', scheduleViewportSync)
  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible') scheduleViewportSync()
  })
  window.addEventListener('beforeinstallprompt', (event) => {
    event.preventDefault()
    installPrompt = event
    pwaState.installable = true
  })
  window.addEventListener('appinstalled', () => {
    installPrompt = null
    pwaState.installable = false
    pwaState.installed = true
  })
  if (!import.meta.env.PROD || !window.isSecureContext || !('serviceWorker' in navigator)) {
    registrationPromise = Promise.resolve(null)
    return registrationPromise
  }
  navigator.serviceWorker.addEventListener('controllerchange', () => {
    if (reloadOnActivation) window.location.reload()
  })
  registrationPromise = navigator.serviceWorker.register('/sw.js', { scope: '/', updateViaCache: 'none' })
    .then((registration) => {
      offerUpdate(registration)
      registration.addEventListener('updatefound', () => {
        const worker = registration.installing
        worker?.addEventListener('statechange', () => {
          if (worker.state === 'installed') offerUpdate(registration)
        })
      })
      document.addEventListener('visibilitychange', () => {
        if (document.visibilityState === 'visible' && navigator.onLine) registration.update().catch(() => {})
      })
      return registration
    }).catch(() => null)
  return registrationPromise
}

export async function installPwa() {
  if (pwaState.installed) return
  if (installPrompt) {
    const prompt = installPrompt
    installPrompt = null
    pwaState.installable = false
    await prompt.prompt()
    await prompt.userChoice
    return
  }
  uni.showModal({
    title: '添加到主屏幕', showCancel: false,
    content: isIos()
      ? '在 Safari 中打开拱拱，点分享按钮，选择“添加到主屏幕”。如果出现“作为 Web App 打开”，请保持开启。以后从桌面的拱拱图标进入即可。'
      : '打开浏览器菜单，选择“安装应用”或“添加到主屏幕”，以后从桌面的拱拱图标进入即可。',
  })
}

export async function checkPwaUpdate() {
  if (pwaState.updating) return
  pwaState.updating = true
  try {
    if (!navigator.onLine) throw new Error('当前离线，请联网后检查更新')
    const registration = await initPwa()
    if (!registration) throw new Error('请从已部署的 HTTPS 地址检查更新')
    lastPromptedWorker = null
    await registration.update()
    const worker = registration.installing
    if (worker) {
      await new Promise((resolve, reject) => {
        const changed = () => {
          if (worker.state === 'installed' || worker.state === 'activated') {
            worker.removeEventListener('statechange', changed)
            resolve()
          } else if (worker.state === 'redundant') {
            worker.removeEventListener('statechange', changed)
            reject(new Error('更新下载失败，请稍后重试'))
          }
        }
        worker.addEventListener('statechange', changed)
        changed()
      })
    }
    if (registration.waiting) {
      offerUpdate(registration)
    } else uni.showToast({ title: '当前为最新版本', icon: 'success' })
  } catch (error) {
    uni.showToast({ title: error.message || '检查更新失败', icon: 'none' })
  } finally { pwaState.updating = false }
}

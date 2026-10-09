// Deliberately restricted to display geometry: no account data, page text,
// storage reads, API calls or automatic uploads.
import { iosViewportState } from './ios-viewport'
const REVISION = '20261009-ios3'
let lastMainLayout = null

function rect(element) {
  if (!element) return null
  const r = element.getBoundingClientRect()
  return Object.fromEntries(['top', 'bottom', 'left', 'right', 'width', 'height'].map(key => [key, Math.round(r[key] * 100) / 100]))
}

export function rememberMainLayout() {
  const nav = Array.from(document.querySelectorAll('.app-nav')).find(element => {
    const box = element.getBoundingClientRect()
    return box.width > 0 && box.height > 0
  })
  if (!nav) return
  const page = nav.closest('.page-with-nav')
  const style = getComputedStyle(nav)
  lastMainLayout = {
    page: page?.className,
    viewport: { width: innerWidth, height: innerHeight, screenWidth: screen.width, screenHeight: screen.height },
    visualViewport: window.visualViewport ? { height: visualViewport.height, width: visualViewport.width, offsetTop: visualViewport.offsetTop, scale: visualViewport.scale } : null,
    nav: rect(nav),
    pageRect: rect(page),
    navPosition: style.position,
    navPadding: style.paddingBottom,
    iosStandalone: document.documentElement.hasAttribute('data-ios-standalone'),
    statusBarInset: page ? getComputedStyle(page).getPropertyValue('--status-bar-height').trim() : null,
    viewportRecovery: iosViewportState(),
  }
}

async function workerVersion() {
  const worker = navigator.serviceWorker?.controller
  if (!worker) return null
  return new Promise(resolve => {
    const channel = new MessageChannel()
    const finish = version => { clearTimeout(timer); channel.port1.close(); resolve(version) }
    const timer = setTimeout(() => finish(null), 1500)
    channel.port1.onmessage = event => finish(event.data?.version || null)
    worker.postMessage({ type: 'GET_VERSION' }, [channel.port2])
  })
}

export async function collectLayoutFeedback() {
  rememberMainLayout()
  const probe = document.createElement('div')
  probe.style.cssText = 'position:fixed;bottom:0;left:0;width:1px;height:1px;visibility:hidden;pointer-events:none;padding-top:env(safe-area-inset-top,0px);padding-bottom:env(safe-area-inset-bottom,0px);box-sizing:content-box'
  document.body.append(probe)
  const style = getComputedStyle(probe)
  const safeArea = { top: style.paddingTop, bottom: style.paddingBottom, fixedProbe: rect(probe) }
  probe.remove()
  return {
    revision: REVISION,
    activeWorker: await workerVersion(),
    userAgent: navigator.userAgent,
    standalone: navigator.standalone === true || matchMedia('(display-mode: standalone)').matches,
    statusBarStyle: document.querySelector('meta[name="apple-mobile-web-app-status-bar-style"]')?.content,
    safeArea,
    mainLayout: lastMainLayout,
  }
}

export async function showLayoutFeedback() {
  const info = await collectLayoutFeedback()
  const layout = info.mainLayout
  uni.showModal({
    title: '界面显示反馈',
    content: `界面版本：${info.revision}\n桌面模式：${info.standalone ? '是' : '否'}\n视口高度：${layout?.viewport.height ?? '未记录'}\n页面高度：${layout?.pageRect?.height ?? '未记录'}\n导航底部：${layout?.nav.bottom ?? '未记录'}\n导航定位：${layout?.navPosition ?? '未记录'}\n底部留白：${layout?.navPadding ?? '未记录'}\n只包含设备和显示信息。复制后发给维护者。`,
    confirmText: '复制信息',
    cancelText: '关闭',
    success: ({ confirm }) => { if (confirm) uni.setClipboardData({ data: JSON.stringify(info, null, 2) }) },
  })
}

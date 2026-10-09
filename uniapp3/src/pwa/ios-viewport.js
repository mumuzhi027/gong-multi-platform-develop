// Recover only the measured missing top inset, not arbitrary small viewports.
// Keyboard, zoom, browser chrome and multitasking can all legitimately reduce it.
let viewportState = null

export function viewportCorrection(metrics, enabled, browserMajor = 0) {
  const gap = metrics.screenHeight - metrics.dvh
  if (!enabled || metrics.top <= 0 || gap <= 0 || gap > 110 ||
      Math.abs(gap - metrics.top) > 1 ||
      Math.abs(metrics.screenWidth - metrics.viewportWidth) > 1 ||
      Math.abs(metrics.scale - 1) > 0.01 ||
      Math.abs(metrics.visualHeight - metrics.dvh) > 1) return null
  return { height: metrics.screenHeight, topClearance: browserMajor >= 27 ? 16 : 0 }
}

export function measureViewport() {
  const probe = document.createElement('div')
  probe.style.cssText = 'position:fixed;top:0;left:0;width:0;visibility:hidden;pointer-events:none;box-sizing:content-box;padding-top:env(safe-area-inset-top,0px);padding-bottom:env(safe-area-inset-bottom,0px)'
  document.body.append(probe)
  const height = unit => {
    probe.style.height = unit
    return parseFloat(getComputedStyle(probe).height)
  }
  const vh = height('100vh')
  const dvh = CSS.supports('height','100dvh') ? height('100dvh') : vh
  const lvh = CSS.supports('height','100lvh') ? height('100lvh') : vh
  const style = getComputedStyle(probe)
  const metrics = {
    top: parseFloat(style.paddingTop), bottom: parseFloat(style.paddingBottom),
    vh, dvh, lvh, screenHeight: screen.height, screenWidth: screen.width,
    viewportWidth: innerWidth, visualHeight: window.visualViewport?.height ?? innerHeight,
    scale: window.visualViewport?.scale ?? 1,
  }
  probe.remove()
  return metrics
}

export function syncIosViewport(enabled) {
  const root = document.documentElement
  const metrics = measureViewport()
  // Retain the last stable layout while a keyboard or pinch changes the visual viewport.
  if (enabled && (Math.abs(metrics.scale - 1) > 0.01 || metrics.visualHeight < metrics.dvh - 100)) return
  const major = Number(navigator.userAgent.match(/Version\/(\d+)/)?.[1] || 0)
  const correction = viewportCorrection(metrics, enabled, major)
  root.toggleAttribute('data-ios-viewport-recovered', Boolean(correction))
  if (correction) {
    root.style.setProperty('--ios-app-height', `${correction.height}px`)
    root.style.setProperty('--ios-top-edge-clearance', `${correction.topClearance}px`)
  } else {
    root.style.removeProperty('--ios-app-height')
    root.style.removeProperty('--ios-top-edge-clearance')
  }
  viewportState = { metrics, correction }
}

export function iosViewportState() { return viewportState }

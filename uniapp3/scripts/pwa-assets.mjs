import { readFileSync } from 'node:fs'
import { Resvg } from '@resvg/resvg-js'

// Reuse the app's artwork; keep the pig inside the maskable safe zone.
export function pwaAssets() {
  const logo = readFileSync(new URL('../src/static/icons/gonggong_logo.svg', import.meta.url), 'utf8')
  const artwork = logo.replace(/<svg[^>]*>/, '').replace(/<\/svg>\s*$/, '')
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><rect width="100" height="100" fill="#f1f1f1"/>${artwork}</svg>`
  return [
    ['apple-touch-icon.png', 180], ['icon-192.png', 192], ['icon-512.png', 512],
  ].map(([name, size]) => ({
    name: `pwa/${name}`,
    data: new Resvg(svg, { fitTo: { mode: 'width', value: size } }).render().asPng(),
  }))
}

export const webManifest = {
  id: '/', name: '拱拱', short_name: '拱拱',
  description: '湘大学业与校园信息助手', lang: 'zh-CN',
  start_url: '/#/pages/login/index', scope: '/', display: 'standalone',
  background_color: '#f1f1f1', theme_color: '#f1f1f1',
  icons: [192, 512].map((size) => ({
    src: `/pwa/icon-${size}.png`, sizes: `${size}x${size}`,
    type: 'image/png', purpose: 'any maskable',
  })),
}

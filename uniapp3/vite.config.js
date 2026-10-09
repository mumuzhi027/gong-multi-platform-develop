import { defineConfig, loadEnv } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'
import { gongPwa } from './scripts/pwa-plugin.mjs'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const backend = env.GONG_API_PROXY_TARGET || 'http://101.43.27.78:8000'
  return {
    plugins: [uni(), ...(process.env.UNI_PLATFORM === 'h5' ? [gongPwa()] : [])],
    server: {
      host: '0.0.0.0',
      proxy: {
        '/api': { target: backend, changeOrigin: true, rewrite: (path) => path.replace(/^\/api/, '') },
        '/web': { target: env.GONG_WEB_PROXY_TARGET || backend, changeOrigin: true, rewrite: (path) => path.replace(/^\/web/, '') },
      },
    },
  }
})

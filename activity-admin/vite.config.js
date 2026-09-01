import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  build: {
    // Keep classic max-width media queries for older Android/Quark kernels.
    target: 'es2018',
    cssTarget: 'chrome61',
    cssMinify: false,
  },
  server: {
    port: 4174,
  },
})

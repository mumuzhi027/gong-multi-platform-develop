/* Build injects a version and an explicit list of public app files. */
const CACHE_PREFIX = 'gong-pwa-shell-'
const CACHE_NAME = `${CACHE_PREFIX}__GONG_VERSION__`
const FILES = __GONG_PRECACHE__
const FILE_URLS = new Set(FILES.map((path) => new URL(path, self.location.origin).href))

self.addEventListener('install', (event) => {
  event.waitUntil((async () => {
    try {
      const cache = await caches.open(CACHE_NAME)
      await cache.addAll(FILES.map((path) => new Request(path, { cache: 'reload' })))
      // Existing clients keep their complete old shell until the user accepts an update.
      if (!self.registration.active) await self.skipWaiting()
    } catch (error) {
      await caches.delete(CACHE_NAME)
      throw error
    }
  })())
})

self.addEventListener('activate', (event) => {
  event.waitUntil((async () => {
    for (const key of await caches.keys()) {
      if (key.startsWith(CACHE_PREFIX) && key !== CACHE_NAME) await caches.delete(key)
    }
    await self.clients.claim()
  })())
})

self.addEventListener('message', (event) => {
  if (event.data?.type === 'APPLY_UPDATE') event.waitUntil(self.skipWaiting())
  if (event.data?.type === 'GET_VERSION') event.ports?.[0]?.postMessage({ version: CACHE_NAME.slice(CACHE_PREFIX.length) })
})

self.addEventListener('fetch', (event) => {
  const request = event.request
  const url = new URL(request.url)
  if (request.method !== 'GET' || url.origin !== self.location.origin) return
  // The router uses hashes. Never intercept API, legal pages or remote images.
  const shell = request.mode === 'navigate' && (url.pathname === '/' || url.pathname === '/index.html')
  const key = shell ? `${url.origin}/index.html` : `${url.origin}${url.pathname}`
  if (!FILE_URLS.has(key)) return
  event.respondWith((async () => {
    const cache = await caches.open(CACHE_NAME)
    const cached = await cache.match(key)
    return cached || fetch(request)
  })())
})

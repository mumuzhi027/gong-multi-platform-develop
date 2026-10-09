import assert from 'node:assert/strict'
import { readFileSync, existsSync } from 'node:fs'
import vm from 'node:vm'

const root = new URL('../dist/build/h5/', import.meta.url)
const source = readFileSync(new URL('sw.js', root), 'utf8')
const manifest = JSON.parse(readFileSync(new URL('manifest.webmanifest', root), 'utf8'))
assert.equal(manifest.display, 'standalone')
assert.equal(manifest.scope, '/')
assert.equal(manifest.start_url, '/#/pages/login/index')
for (const icon of manifest.icons) {
  const png = readFileSync(new URL(icon.src.slice(1), root))
  const size = Number(icon.sizes.split('x')[0])
  assert.equal(png.readUInt32BE(16), size)
  assert.equal(png.readUInt32BE(20), size)
}
const html = readFileSync(new URL('index.html', root), 'utf8')
assert.match(html, /viewport-fit=cover/)
assert.match(html, /apple-mobile-web-app-capable/)

function harness({ active = false, failingInstall = false } = {}) {
  const handlers = {}
  const store = new Map([['unrelated-cache', new Map()], ['gong-pwa-shell-old', new Map()]])
  const calls = { skipped: 0, claimed: 0, network: 0 }
  const context = {
    URL, Set,
    Request: class { constructor(url) { this.url = new URL(url, 'https://gong.test').href } },
    self: {
      location: { origin: 'https://gong.test' }, registration: { active },
      addEventListener: (name, handler) => { handlers[name] = handler },
      skipWaiting: async () => { calls.skipped++ },
      clients: { claim: async () => { calls.claimed++ } },
    },
    caches: {
      keys: async () => [...store.keys()], delete: async (name) => store.delete(name),
      open: async (name) => {
        if (!store.has(name)) store.set(name, new Map())
        const cache = store.get(name)
        return {
          addAll: async (requests) => {
            if (failingInstall) throw new Error('Network error')
            for (const request of requests) cache.set(request.url, { url: request.url, cached: true })
          },
          match: async (key) => cache.get(key),
        }
      },
    },
    fetch: async () => { calls.network++; throw new Error('Offline') },
  }
  vm.runInNewContext(source, context)
  vm.runInNewContext('globalThis.precache = FILES; globalThis.cacheName = CACHE_NAME;', context)
  return {
    context, store, calls,
    async event(name, data = {}) {
      let result
      handlers[name]({ ...data, waitUntil: (promise) => { result = promise }, respondWith: (promise) => { result = promise } })
      return result && await result
    },
  }
}

const fresh = harness()
for (const file of fresh.context.precache) assert.ok(existsSync(new URL(file.slice(1), root)), file)
for (const page of ['home', 'course', 'academic', 'activity', 'classroom']) {
  assert.ok(fresh.context.precache.some((name) => name.includes(`pages-${page}-index`) && name.endsWith('.js')))
}
await fresh.event('install')
assert.equal(fresh.calls.skipped, 1)
await fresh.event('activate')
assert.equal(fresh.calls.claimed, 1)
assert.ok(fresh.store.has('unrelated-cache'))
assert.ok(!fresh.store.has('gong-pwa-shell-old'))
let reportedVersion
await fresh.event('message', { data: { type: 'GET_VERSION' }, ports: [{ postMessage: data => { reportedVersion = data.version } }] })
assert.equal(reportedVersion, fresh.context.cacheName.slice('gong-pwa-shell-'.length))
assert.ok((await fresh.event('fetch', { request: { url: 'https://gong.test/?source=homescreen', method: 'GET', mode: 'navigate' } })).cached)
const lazyPage = fresh.context.precache.find((name) => name.includes('pages-academic-index') && name.endsWith('.js'))
assert.ok((await fresh.event('fetch', { request: { url: `https://gong.test${lazyPage}`, method: 'GET', mode: 'cors' } })).cached)
for (const request of [
  { url: 'https://gong.test/api/info', method: 'GET', mode: 'cors' },
  { url: 'https://gong.test/api/courses', method: 'GET', mode: 'navigate' },
  { url: 'https://gong.test/api/login', method: 'POST', mode: 'cors' },
  { url: 'https://gong.test/web/', method: 'GET', mode: 'navigate' },
  { url: 'https://other.test/index.html', method: 'GET', mode: 'navigate' },
]) assert.equal(await fresh.event('fetch', { request }), undefined)
assert.equal(fresh.calls.network, 0)
const update = harness({ active: true })
await update.event('install')
assert.equal(update.calls.skipped, 0)
assert.ok(update.store.has('gong-pwa-shell-old'))
await update.event('message', { data: { type: 'APPLY_UPDATE' } })
assert.equal(update.calls.skipped, 1)
const failure = harness({ active: true, failingInstall: true })
await assert.rejects(failure.event('install'), /Network error/)
assert.ok(failure.store.has('gong-pwa-shell-old'))
assert.ok(!failure.store.has(failure.context.cacheName))
console.log('PWA checks passed: icons, full offline shell, lazy pages, API bypass, cache isolation, user-controlled update, failed-update recovery.')

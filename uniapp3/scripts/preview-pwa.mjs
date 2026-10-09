import http from 'node:http'
import https from 'node:https'
import { createReadStream, existsSync, statSync } from 'node:fs'
import { resolve, extname, sep } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = fileURLToPath(new URL('../dist/build/h5/', import.meta.url))
const port = Number(process.env.PORT || 4173)
const types = { '.html': 'text/html; charset=utf-8', '.js': 'application/javascript', '.css': 'text/css', '.webmanifest': 'application/manifest+json', '.png': 'image/png', '.svg': 'image/svg+xml' }
if (!existsSync(resolve(root, 'sw.js'))) throw new Error('先运行 npm run build:h5')

const server = http.createServer((request, response) => {
  const url = new URL(request.url, 'http://localhost')
  const route = ['/api/', '/web/'].find((prefix) => url.pathname.startsWith(prefix))
  if (route) {
    const backend = route === '/api/' ? process.env.GONG_API_PROXY_TARGET : process.env.GONG_WEB_PROXY_TARGET
    const target = new URL(url.pathname.slice(route.length - 1) + url.search, backend || 'http://101.43.27.78:8000')
    const proxy = (target.protocol === 'https:' ? https : http).request(target, {
      method: request.method, headers: { ...request.headers, host: target.host },
    }, (upstream) => {
      response.writeHead(upstream.statusCode, { ...upstream.headers, 'cache-control': 'no-store' })
      upstream.pipe(response)
    })
    proxy.setTimeout(15000, () => proxy.destroy(new Error('Upstream timed out')))
    proxy.on('error', () => {
      if (!response.headersSent) response.writeHead(502, { 'content-type': 'application/json' })
      response.end(JSON.stringify({ detail: '接口暂时不可用' }))
    })
    request.pipe(proxy)
    return
  }
  let pathname
  try { pathname = decodeURIComponent(url.pathname) } catch { response.writeHead(400).end(); return }
  const filename = resolve(root, `.${pathname === '/' ? '/index.html' : pathname}`)
  if (!filename.startsWith(root.endsWith(sep) ? root : `${root}${sep}`) || !existsSync(filename) || !statSync(filename).isFile()) {
    response.writeHead(404).end('Not found')
    return
  }
  response.writeHead(200, {
    'content-type': types[extname(filename)] || 'application/octet-stream',
    'cache-control': 'no-store',
  })
  createReadStream(filename).pipe(response)
})
server.listen(port, '127.0.0.1', () => console.log(`拱拱 PWA 预览：http://localhost:${port}（iPhone 正式安装需要 HTTPS 域名）`))

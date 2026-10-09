import { createHash } from 'node:crypto'
import { mkdirSync, readFileSync, readdirSync, writeFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { pwaAssets, webManifest } from './pwa-assets.mjs'

function filesIn(directory, prefix = '') {
  return readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
    const name = `${prefix}${entry.name}`
    return entry.isDirectory()
      ? filesIn(resolve(directory, entry.name), `${name}/`)
      : [name]
  })
}

export function gongPwa() {
  let output
  return {
    name: 'gong-pwa',
    configResolved(config) { output = resolve(config.root, config.build.outDir) },
    configureServer(server) {
      const assets = pwaAssets()
      server.middlewares.use((req, res, next) => {
        if (req.url === '/manifest.webmanifest') {
          res.setHeader('Content-Type', 'application/manifest+json')
          res.end(JSON.stringify(webManifest))
        } else {
          const asset = assets.find((item) => `/${item.name}` === req.url)
          if (!asset) return next()
          res.setHeader('Content-Type', 'image/png')
          res.end(asset.data)
        }
      })
    },
    closeBundle() {
      mkdirSync(resolve(output, 'pwa'), { recursive: true })
      for (const asset of pwaAssets()) writeFileSync(resolve(output, asset.name), asset.data)
      writeFileSync(resolve(output, 'manifest.webmanifest'), JSON.stringify(webManifest, null, 2))
      const files = filesIn(output)
        .filter((name) => name !== 'sw.js' && /\.(?:html|js|css|png|svg|webmanifest|woff2?|ttf)$/.test(name))
        .sort()
      const hash = createHash('sha256')
      for (const name of files) hash.update(name).update(readFileSync(resolve(output, name)))
      const version = hash.digest('hex').slice(0, 16)
      const source = readFileSync(new URL('../src/pwa/service-worker.js', import.meta.url), 'utf8')
        .replace('__GONG_VERSION__', version)
        .replace('__GONG_PRECACHE__', JSON.stringify(files.map((name) => `/${name}`)))
      writeFileSync(resolve(output, 'sw.js'), source)
      console.log(`PWA: ${files.length} app resources, version ${version}`)
    },
  }
}

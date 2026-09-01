import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { Resvg } from '@resvg/resvg-js'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const iconDir = path.join(root, 'src', 'static', 'icons')

for (const name of fs.readdirSync(iconDir).filter((item) => item.endsWith('.svg'))) {
  const input = fs.readFileSync(path.join(iconDir, name), 'utf8')
  const png = new Resvg(input, {
    background: 'rgba(0,0,0,0)',
    fitTo: { mode: 'width', value: 96 },
  }).render().asPng()
  fs.writeFileSync(path.join(iconDir, name.replace(/\.svg$/, '.png')), png)
}

const sourceDir = path.join(root, 'src')
function replaceVueReferences(directory) {
  for (const entry of fs.readdirSync(directory, { withFileTypes: true })) {
    const target = path.join(directory, entry.name)
    if (entry.isDirectory()) replaceVueReferences(target)
    else if (entry.name.endsWith('.vue')) {
      const before = fs.readFileSync(target, 'utf8')
      const after = before.replace(/(\/static\/icons\/[^'"`]+)\.svg/g, '$1.png')
      if (after !== before) fs.writeFileSync(target, after, 'utf8')
    }
  }
}
replaceVueReferences(sourceDir)
console.log(`Rendered ${fs.readdirSync(iconDir).filter((item) => item.endsWith('.png')).length} mini-program PNG icons.`)

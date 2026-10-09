import fs from 'node:fs'
import path from 'node:path'
import assert from 'node:assert/strict'
import { fileURLToPath } from 'node:url'
import { baseParse } from '@vue/compiler-dom'
import { Resvg } from '@resvg/resvg-js'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const nativeDir = path.resolve(root, '../composeApp/src/commonMain/composeResources/drawable')
const iconDir = path.join(root, 'src/static/icons')
const check = process.argv.includes('--check')
const parse = xml => baseParse(xml.replace(/<\?xml[^?]*\?>/g, ''))
// Compose uses these original colors (Image or Icon with Color.Unspecified).
const originalColors = new Set(['notification_logo', 'gonggong_logo', 'compose-multiplatform', 'password_visible', 'password_invisible'])
const elements = node => (node.children || []).filter(child => child.type === 1)
const attributes = node => Object.fromEntries(node.props.map(prop => {
  assert.equal(prop.type, 6, `Unexpected XML attribute in ${node.tag}`)
  return [prop.name.replace(/^android:/, ''), prop.value?.content]
}))
const escape = value => String(value).replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/</g, '&lt;')
const attrs = values => Object.entries(values).filter(([, value]) => value !== undefined).map(([key, value]) => `${key}="${escape(value)}"`).join(' ')

function color(value, tint) {
  if (!value) return 'none' // VectorDrawable defaults to transparent, SVG defaults to black.
  assert.match(value, /^#(?:[\da-f]{3}|[\da-f]{6}|[\da-f]{8})$/i, `Unsupported native color ${value}`)
  let alpha = 1
  if (value.length === 9) {
    alpha = parseInt(value.slice(1, 3), 16) / 255
    value = `#${value.slice(3)}`
  }
  if (!alpha) return 'none'
  const result = tint || value
  if (alpha === 1) return result
  const rgb = result.slice(1).match(/../g).map(part => parseInt(part, 16))
  return `rgba(${rgb.join(',')},${alpha})`
}

export function vectorToSvg(xml, tint) {
  const vector = parse(xml).children.find(node => node.type === 1 && node.tag === 'vector')
  assert.ok(vector, 'Expected a native vector')
  const viewport = attributes(vector)
  const definitions = []
  const paths = elements(vector).map((node, index) => {
    assert.equal(node.tag, 'path', `Unsupported vector element ${node.tag}; add an explicit conversion before using it`)
    const source = attributes(node)
    const supported = new Set(['pathData','fillColor','strokeColor','strokeWidth','strokeLineCap','strokeLineJoin'])
    for (const key of Object.keys(source)) assert.ok(supported.has(key), `Unsupported native path attribute ${key}; extend conversion instead of silently losing it`)
    const fill = elements(node).find(child => child.tag === 'aapt:attr' && attributes(child).name === 'android:fillColor')
    let fillColor = color(source.fillColor, tint)
    if (fill) {
      const gradient = elements(fill)[0]
      assert.equal(gradient.tag, 'gradient')
      const props = attributes(gradient)
      assert.ok(['linear', 'radial'].includes(props.type), `Unsupported gradient ${props.type}`)
      const id = `native-gradient-${index}`
      const tag = props.type === 'radial' ? 'radialGradient' : 'linearGradient'
      const coords = props.type === 'radial'
        ? { cx: props.centerX, cy: props.centerY, r: props.gradientRadius }
        : { x1: props.startX, y1: props.startY, x2: props.endX, y2: props.endY }
      const stops = elements(gradient).map(item => {
        assert.equal(item.tag, 'item')
        const stop = attributes(item)
        return `<stop ${attrs({ offset: stop.offset, 'stop-color': color(stop.color, tint) })}/>`
      }).join('')
      definitions.push(`<${tag} ${attrs({ id, gradientUnits: 'userSpaceOnUse', ...coords })}>${stops}</${tag}>`)
      fillColor = `url(#${id})`
    }
    return `  <path ${attrs({
      d: source.pathData, fill: fillColor,
      stroke: source.strokeColor ? color(source.strokeColor, tint) : undefined,
      'stroke-width': source.strokeWidth, 'stroke-linecap': source.strokeLineCap,
      'stroke-linejoin': source.strokeLineJoin,
    })}/>`
  })
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${viewport.viewportWidth} ${viewport.viewportHeight}">\n${definitions.length ? `  <defs>${definitions.join('')}</defs>\n` : ''}${paths.join('\n')}\n</svg>\n`
}

function output(file, content) {
  const target = path.join(iconDir, file)
  const identical = fs.existsSync(target) && fs.readFileSync(target).equals(Buffer.from(content))
  if (check) assert.ok(identical, `Outdated asset ${file}; run npm run assets:sync`)
  else if (!identical) {
    const temporary = `${target}.tmp`
    fs.writeFileSync(temporary, content)
    fs.renameSync(temporary, target)
  }
}

const nativeVectors = fs.readdirSync(nativeDir).filter(name => name.endsWith('.xml'))
for (const file of nativeVectors) {
  const name = file.replace(/\.xml$/, '')
  const xml = fs.readFileSync(path.join(nativeDir, file), 'utf8')
  const vector = parse(xml).children.find(node => node.type === 1 && node.tag === 'vector')
  const props = attributes(vector)
  for (const [suffix, tint] of [['', '#000000'], ['-active', '#85cdfa']]) {
    // Never flatten an illustration into a monochrome icon.
    const svg = vectorToSvg(xml, originalColors.has(name) ? undefined : tint)
    output(`${name}${suffix}.svg`, svg)
    // Legacy raster fallback: enough physical pixels for 3x displays at the largest UI size.
    const width = name === 'notification_logo' ? 900 : Math.max(96, parseFloat(props.width) * 3)
    output(`${name}${suffix}.png`, new Resvg(svg, { fitTo: { mode: 'width', value: width } }).render().asPng())
  }
}

const imageDir = path.join(root, 'src/static/images')
for (const file of fs.readdirSync(nativeDir).filter(name => /\.(png|jpe?g)$/i.test(name))) {
  const native = fs.readFileSync(path.join(nativeDir, file))
  const target = path.join(imageDir, file)
  if (check) assert.ok(fs.existsSync(target) && fs.readFileSync(target).equals(native), `Raster differs from the App: ${file}`)
  else if (!fs.existsSync(target) || !fs.readFileSync(target).equals(native)) fs.writeFileSync(target, native)
}
console.log(`${check ? 'Verified' : 'Synchronized'} ${nativeVectors.length} native vectors, color/stroke/gradient preservation and original raster assets.`)

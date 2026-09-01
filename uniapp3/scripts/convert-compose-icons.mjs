import fs from 'node:fs'
import path from 'node:path'

const projectRoot = path.resolve(import.meta.dirname, '..')
const sourceDir = path.resolve(projectRoot, '..', 'composeApp', 'src', 'commonMain', 'composeResources', 'drawable')
const outputDir = path.resolve(projectRoot, 'src', 'static', 'icons')
const multiColorIcons = new Set(['gonggong_logo'])

fs.mkdirSync(outputDir, { recursive: true })

function attribute(source, name) {
  const match = source.match(new RegExp(`android:${name}="([^"]+)"`))
  return match?.[1]
}

function colorAttributes(rawColor, property, forceColor) {
  if (forceColor) return `${property}="${forceColor}"`
  if (!rawColor) return property === 'fill' ? 'fill="none"' : ''

  const normalized = /^#[0-9a-f]{8}$/i.test(rawColor) && rawColor.slice(1, 3).toUpperCase() === 'FF'
    ? `#${rawColor.slice(3)}`
    : rawColor
  if (/^#00[0-9a-f]{6}$/i.test(rawColor)) return `${property}="none"`
  if (/^#[0-9a-f]{8}$/i.test(rawColor)) {
    const alpha = Number.parseInt(rawColor.slice(1, 3), 16) / 255
    return `${property}="#${rawColor.slice(3)}" ${property}-opacity="${alpha.toFixed(3)}"`
  }
  return `${property}="${normalized}"`
}

function convert(filePath, forcedColor = null) {
  const xml = fs.readFileSync(filePath, 'utf8')
  const vectorTag = xml.match(/<vector\b[^>]*>/)?.[0] || ''
  const viewportWidth = attribute(vectorTag, 'viewportWidth') || '24'
  const viewportHeight = attribute(vectorTag, 'viewportHeight') || '24'
  const paths = [...xml.matchAll(/<path\b[^>]*\/?\s*>/g)].map(([tag]) => {
    const d = attribute(tag, 'pathData') || ''
    const fill = colorAttributes(attribute(tag, 'fillColor'), 'fill', forcedColor)
    const strokeRaw = attribute(tag, 'strokeColor')
    const stroke = strokeRaw ? colorAttributes(strokeRaw, 'stroke', forcedColor) : ''
    const strokeWidth = attribute(tag, 'strokeWidth')
    const lineCap = attribute(tag, 'strokeLineCap')
    const lineJoin = attribute(tag, 'strokeLineJoin')
    return `  <path d="${d}" ${fill}${stroke ? ` ${stroke}` : ''}${strokeWidth ? ` stroke-width="${strokeWidth}"` : ''}${lineCap ? ` stroke-linecap="${lineCap}"` : ''}${lineJoin ? ` stroke-linejoin="${lineJoin}"` : ''}/>`
  })

  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${viewportWidth} ${viewportHeight}">\n${paths.join('\n')}\n</svg>\n`
}

for (const filename of fs.readdirSync(sourceDir)) {
  if (!filename.endsWith('.xml')) continue
  const name = path.basename(filename, '.xml')
  const source = path.join(sourceDir, filename)
  const preserveColors = multiColorIcons.has(name)
  fs.writeFileSync(
    path.join(outputDir, `${name}.svg`),
    convert(source, preserveColors ? null : '#292929'),
    'utf8',
  )
  if (!preserveColors) {
    fs.writeFileSync(path.join(outputDir, `${name}-active.svg`), convert(source, '#49B7F6'), 'utf8')
  }
}

console.log(`Converted Compose drawables to ${outputDir}`)

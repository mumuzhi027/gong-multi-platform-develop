import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
const source = readFileSync(new URL('../src/pwa/ios-viewport.js',import.meta.url),'utf8')
const { viewportCorrection } = await import(`data:text/javascript;base64,${Buffer.from(source).toString('base64')}`)

const reported = {screenHeight:874,screenWidth:402,viewportWidth:402,dvh:812,visualHeight:812,scale:1,top:62}
assert.deepEqual(viewportCorrection(reported,true,27),{height:874,topClearance:16})
assert.deepEqual(viewportCorrection(reported,true,18),{height:874,topClearance:0})
assert.equal(viewportCorrection(reported,false,27),null)
for (const change of [
  {dvh:874,visualHeight:874}, // Already covers the screen: don't add the inset twice.
  {visualHeight:500}, // Keyboard overlay.
  {dvh:500,visualHeight:500}, // Resized keyboard viewport.
  {scale:1.5}, // Pinch zoom.
  {viewportWidth:300}, // Multitasking / a smaller window.
  {top:0}, // OS already excludes the status bar from the content area.
  {screenHeight:900}, // A deficit that is not the top inset.
  {screenHeight:402,screenWidth:874,viewportWidth:874,dvh:402,visualHeight:402,top:0},
]) assert.equal(viewportCorrection({...reported,...change},true,27),null,JSON.stringify(change))
console.log('iOS viewport checks passed: reported 812/874/62 geometry, normal fullscreen, older Safari, keyboard, zoom, split window and rotation.')

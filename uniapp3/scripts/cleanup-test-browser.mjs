import { realpathSync, lstatSync, rmSync } from 'node:fs'
import { basename, dirname } from 'node:path'
import { setTimeout as delay } from 'node:timers/promises'

// Close only this script's browser; never touch the user's normal Edge profile.
export async function cleanupTestBrowser({ browser, socket, send, profile, artifacts }) {
  if (socket?.readyState === 1) {
    await Promise.race([send('Browser.close').catch(() => {}), delay(1500)])
  }
  socket?.close()
  if (browser.exitCode === null) browser.kill()
  for (let attempt = 0; browser.exitCode === null && attempt < 10; attempt++) await delay(100)
  try {
    const root = realpathSync(artifacts)
    const target = realpathSync(profile)
    if (dirname(target).toLowerCase() !== root.toLowerCase() ||
        !/^browser-\d+$/.test(basename(target)) || lstatSync(profile).isSymbolicLink()) {
      throw new Error('Refusing cleanup outside the disposable test profile directory')
    }
    rmSync(target, { recursive: true, force: true, maxRetries: 4, retryDelay: 250 })
  } catch (error) {
    // Preserve the test result if Windows still holds a file; report leftovers.
    console.warn(`Temporary browser profile cleanup: ${error.message}`)
  }
}

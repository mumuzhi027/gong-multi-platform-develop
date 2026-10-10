import assert from 'node:assert/strict'
import { spawn } from 'node:child_process'
import { mkdirSync, existsSync, readFileSync, writeFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import { setTimeout as delay } from 'node:timers/promises'
import { cleanupTestBrowser } from './cleanup-test-browser.mjs'

// Exercise real Vue inputs and uni.request, but intercept every API request.
// Dummy credentials never reach either the public server or school service.
const artifacts = fileURLToPath(new URL('../../tmp/pwa-check/', import.meta.url))
mkdirSync(artifacts, { recursive: true })
const profile = resolve(artifacts, `browser-${Date.now()}`)
mkdirSync(profile)
const browser = spawn(process.env.PWA_BROWSER || 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe', [
  '--headless=new', '--disable-gpu', '--no-sandbox', '--no-first-run', '--no-default-browser-check',
  '--remote-debugging-port=0', `--user-data-dir=${profile}`, 'about:blank',
], { windowsHide: true, stdio: 'ignore' })
let socket, sequence = 0
const pending = new Map()
const captured = []
const failures = []
let responseStatus = 401
function send(method, params = {}) {
  const id = ++sequence
  return new Promise((resolve, reject) => {
    const timeout = setTimeout(() => reject(new Error(`CDP timeout: ${method}`)), 20000)
    pending.set(id, { resolve, reject, timeout })
    socket.send(JSON.stringify({ id, method, params }))
  })
}
async function evaluate(expression) {
  const result = await send('Runtime.evaluate', { expression, awaitPromise: true, returnByValue: true })
  if (result.exceptionDetails) throw new Error(result.exceptionDetails.exception?.description || result.exceptionDetails.text)
  return result.result.value
}
async function until(fn, label) {
  for (let i = 0; i < 150; i++) {
    if (await fn()) return
    await delay(100)
  }
  throw new Error(`Timed out: ${label}`)
}
try {
  const portFile = resolve(profile, 'DevToolsActivePort')
  await until(() => existsSync(portFile), 'browser startup')
  const port = readFileSync(portFile, 'utf8').split('\n')[0].trim()
  const tabs = await (await fetch(`http://127.0.0.1:${port}/json`)).json()
  socket = new WebSocket(tabs.find(tab => tab.type === 'page').webSocketDebuggerUrl)
  await new Promise(resolve => socket.addEventListener('open', resolve))
  socket.addEventListener('message', ({ data }) => {
    const message = JSON.parse(data)
    if (message.method === 'Runtime.exceptionThrown') failures.push(message.params.exceptionDetails.text)
    if (message.method === 'Fetch.requestPaused') {
      const request = message.params.request
      const login = new URL(request.url).pathname === '/api/login'
      if (login) captured.push(request)
      const body = login ? (responseStatus === 200 ? JSON.stringify({ access_token: 'browser-fixture-token' }) : 'Unauthorized\n') : '{}'
      send('Fetch.fulfillRequest', {
        requestId: message.params.requestId, responseCode: login ? responseStatus : 200,
        responseHeaders: [{ name: 'Content-Type', value: responseStatus === 200 || !login ? 'application/json' : 'text/plain; charset=utf-8' }],
        body: Buffer.from(body).toString('base64'),
      }).catch(error => failures.push(error.message))
    }
    const task = pending.get(message.id)
    if (!task) return
    clearTimeout(task.timeout)
    pending.delete(message.id)
    message.error ? task.reject(new Error(JSON.stringify(message.error))) : task.resolve(message.result)
  })
  await send('Page.enable')
  await send('Runtime.enable')
  await send('Network.enable')
  await send('Fetch.enable', { patterns: [{ urlPattern: '*/api/*' }] })
  await send('Emulation.setDeviceMetricsOverride', { width: 393, height: 852, deviceScaleFactor: 1, mobile: true })
  await send('Network.setUserAgentOverride', { userAgent: 'Mozilla/5.0 (iPhone; CPU iPhone OS 18_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.5 Mobile/15E148 Safari/604.1' })
  await send('Page.navigate', { url: process.env.PWA_LOGIN_TEST_ORIGIN || 'http://127.0.0.1:4173/' })
  await until(() => evaluate('Boolean(document.querySelector(".login-button"))'), 'login page')
  const samples = [
    { username: '20260000000', password: 'Example123!', action: 'tap' },
    { username: '20260000000', password: 'A+b&c=d%20 #?/中文', action: 'tap' },
    { username: '20260000000', password: 'Enter+&%Final!', action: 'enter' },
    { username: '20261111111', password: 'Autofill+Final!', action: 'autofill' },
  ]
  const checks = []
  for (const sample of samples) {
    await evaluate(`(() => {const fields=document.querySelectorAll('.form input');for(const [index,value] of ${JSON.stringify([sample.username, sample.password])}.entries()){fields[index].value=value;if(${JSON.stringify(sample.action)}!=='autofill'){fields[index].dispatchEvent(new Event('input',{bubbles:true}));fields[index].dispatchEvent(new Event('change',{bubbles:true}));}}return true})()`)
    await until(() => evaluate('!document.querySelector(".login-button").hasAttribute("disabled")'), 'submit enabled')
    const previous = captured.length
    await evaluate(sample.action === 'enter'
      ? 'document.querySelectorAll(".form input")[1].dispatchEvent(new KeyboardEvent("keyup",{key:"Enter",bubbles:true})); true'
      : 'document.querySelector(".login-button").click(); true')
    await until(() => captured.length > previous, 'intercepted login')
    await until(() => evaluate('Boolean(document.querySelector(".alert"))'), 'login rejection shown')
    const request = captured.at(-1)
    const form = Object.fromEntries(new URLSearchParams(request.postData))
    assert.equal(request.method, 'POST')
    assert.deepEqual(form, { username: sample.username, password: sample.password })
    assert.ok(Object.entries(request.headers).some(([key,value])=>key.toLowerCase() === 'content-type' && value.startsWith('application/x-www-form-urlencoded')))
    assert.ok(!Object.keys(request.headers).some(key => key.toLowerCase() === 'authorization'))
    checks.push({ action: sample.action, fields: Object.keys(form), credentialsPreserved: true, formEncoded: true, noStaleToken: true })
  }
  responseStatus = 200
  const previous = captured.length
  await evaluate('document.querySelector(".login-button").click(); true')
  await until(() => captured.length > previous, 'successful login request')
  await until(() => evaluate('Boolean(document.querySelector(".notice-request"))'), 'successful login navigation')
  assert.equal(failures.length, 0, JSON.stringify(failures))
  writeFileSync(resolve(artifacts, 'login-result.json'), JSON.stringify({ origin: process.env.PWA_LOGIN_TEST_ORIGIN || 'http://127.0.0.1:4173/', checks, successNavigation: true, failures }, null, 2))
  console.log('Login browser checks passed: real input-to-request encoding, special characters preserved, no old bearer token, mocked rejection and success navigation. No real login attempted.')
} finally {
  await cleanupTestBrowser({ browser, socket, send, profile, artifacts })
  for (const task of pending.values()) clearTimeout(task.timeout)
}

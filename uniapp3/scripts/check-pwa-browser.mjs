import assert from 'node:assert/strict'
import { spawn } from 'node:child_process'
import { mkdirSync, readFileSync, existsSync, writeFileSync, readdirSync } from 'node:fs'
import { resolve } from 'node:path'
import { setTimeout as delay } from 'node:timers/promises'
import { fileURLToPath } from 'node:url'

// A disposable Edge profile and the built-in DevTools protocol avoid extra dependencies.
const artifactDir = fileURLToPath(new URL('../../tmp/pwa-check/', import.meta.url))
mkdirSync(artifactDir, { recursive: true })
const profile = resolve(artifactDir, `browser-${Date.now()}`)
mkdirSync(profile)
const browser = spawn(process.env.PWA_BROWSER || 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe', [
  '--headless=new', '--disable-gpu', '--no-sandbox', '--no-first-run', '--no-default-browser-check',
  '--remote-debugging-port=0', `--user-data-dir=${profile}`, 'about:blank',
], { windowsHide: true, stdio: 'ignore' })
let socket
const pending = new Map()
let sequence = 0
const exceptions = []
const imageFixtures = {
  'wide.svg': '<svg xmlns="http://www.w3.org/2000/svg" width="1200" height="600"><rect width="1200" height="600" fill="#317981"/><rect x="300" width="600" height="600" fill="#df9259"/></svg>',
  'tall.svg': '<svg xmlns="http://www.w3.org/2000/svg" width="600" height="1200"><rect width="600" height="1200" fill="#317981"/><rect y="300" width="600" height="600" fill="#df9259"/></svg>',
}
async function poll(fn, label) {
  for (let i = 0; i < 150; i++) {
    const result = await fn()
    if (result) return result
    await delay(100)
  }
  throw new Error(`Timed out: ${label}`)
}
function send(method, params = {}) {
  const id = ++sequence
  return new Promise((resolve, reject) => {
    const timeout = setTimeout(() => { pending.delete(id); reject(new Error(`CDP timeout: ${method}`)) }, 20000)
    pending.set(id, { resolve, reject, timeout, method, expression:params.expression?.slice(0,180) })
    socket.send(JSON.stringify({ id, method, params }))
  })
}
async function evaluate(expression) {
  const result = await send('Runtime.evaluate', { expression, awaitPromise: true, returnByValue: true })
  if (result.exceptionDetails) throw new Error(result.exceptionDetails.text + ' ' + result.exceptionDetails.exception?.description)
  return result.result.value
}
async function waitFor(expression) {
  return poll(async () => {
    try { return await evaluate(expression) }
    catch (error) {
      // Reload can destroy the old execution context before the new one is ready.
      if (/Inspected target navigated or closed|Execution context was destroyed|Cannot find context/.test(error.message)) return false
      throw error
    }
  }, expression)
}
async function setViewport(metrics) {
  await send('Emulation.setDeviceMetricsOverride',metrics)
  // Switching between desktop and mobile emulation can retain the previous zoom.
  await send('Emulation.resetPageScaleFactor')
  await send('Emulation.setPageScaleFactor',{pageScaleFactor:1})
}

async function screenshot(name) {
  const result = await send('Page.captureScreenshot', { format: 'png', captureBeyondViewport: false })
  writeFileSync(resolve(artifactDir, `${name}.png`), Buffer.from(result.data, 'base64'))
}
async function navigate(page) {
  await send('Page.navigate', { url: `http://127.0.0.1:4173/#/pages/${page}` })
}
async function reload() {
  const marker = String(Math.random())
  await evaluate(`window.__pwaReloadMarker=${JSON.stringify(marker)}; true`)
  await send('Page.reload')
  await waitFor(`window.__pwaReloadMarker !== ${JSON.stringify(marker)} && document.readyState !== 'loading'`)
}
try {
  const portFile = resolve(profile, 'DevToolsActivePort')
  await poll(() => existsSync(portFile), 'Edge startup')
  const port = readFileSync(portFile, 'utf8').split('\n')[0].trim()
  const tabs = await (await fetch(`http://127.0.0.1:${port}/json`)).json()
  socket = new WebSocket(tabs.find((tab) => tab.type === 'page').webSocketDebuggerUrl)
  await new Promise((resolve, reject) => { socket.addEventListener('open', resolve); socket.addEventListener('error', reject) })
  socket.addEventListener('message', ({ data }) => {
    const message = JSON.parse(data)
    if (message.method === 'Fetch.requestPaused') {
      const name = message.params.request.url.split('/').pop()
      const body = imageFixtures[name]
      send('Fetch.fulfillRequest', {
        requestId: message.params.requestId, responseCode: body ? 200 : 404,
        responseHeaders: [{ name: 'Content-Type', value: 'image/svg+xml' }],
        body: Buffer.from(body || 'Missing image').toString('base64'),
      }).catch(error => exceptions.push({ interception: error.message }))
    }
    if (message.method === 'Runtime.exceptionThrown') exceptions.push(message.params.exceptionDetails)
    const task = pending.get(message.id)
    if (!task) return
    clearTimeout(task.timeout)
    pending.delete(message.id)
    if (message.error) task.reject(new Error(`${task.method}: ${JSON.stringify(message.error)} ${task.expression || ''}`))
    else task.resolve(message.result)
  })
  await send('Page.enable')
  await send('Runtime.enable')
  await send('Network.enable')
  await setViewport({ width: 393, height: 852, deviceScaleFactor: 1, mobile: true })
  await send('Emulation.setTouchEmulationEnabled', { enabled: true })
  await send('Network.setUserAgentOverride', { userAgent: 'Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 Version/18.0 Mobile/15E148 Safari/604.1' })
  await navigate('login/index')
  await waitFor('Boolean(document.querySelector(".offline"))')
  await waitFor('Boolean(navigator.serviceWorker.controller)')
  assert.ok(await evaluate('Boolean(document.querySelector(".install-entry"))'))
  const iconDirectory = fileURLToPath(new URL('../src/static/icons/', import.meta.url))
  const staticAssets = readdirSync(iconDirectory).filter(name => name.endsWith('.svg')).map(name => `/static/icons/${name}`)
  const imageDirectory=fileURLToPath(new URL('../src/static/images/',import.meta.url))
  staticAssets.push(...readdirSync(imageDirectory).filter(name=>name.endsWith('.png')).map(name=>`/static/images/${name}`))
  // Decode every icon in a real browser, including assets on unvisited pages.
  const assetFailures = await evaluate(`(async()=>{const failed=[];for(const url of ${JSON.stringify(staticAssets)}) {const image=new Image(); image.src=url;try{await image.decode();if(!image.naturalWidth)failed.push(url)}catch{failed.push(url)}}return failed})()`)
  assert.deepEqual(assetFailures, [])
  await navigate('notification-request/index')
  await waitFor('Boolean(document.querySelector(".notice-content img")?.naturalWidth)')
  const notificationArtwork = await evaluate(`(async()=>{
    const image=document.querySelector('.notice-content img');await image.decode();
    const canvas=document.createElement('canvas');canvas.width=900;canvas.height=360;
    const ctx=canvas.getContext('2d');ctx.drawImage(image,0,0,900,360);
    const pixel=(x,y)=>Array.from(ctx.getImageData(Math.round(x*9),Math.round(y*9),1,1).data);
    const rect=image.closest('uni-image').getBoundingClientRect();
    return {width:rect.width,height:rect.height,source:image.src,pixels:[pixel(10,9),pixel(8,11),pixel(5,15),pixel(8,15.2),pixel(20,22.5)]};
  })()`)
  assert.equal(notificationArtwork.width, 300)
  assert.equal(notificationArtwork.height, 120)
  assert.ok(notificationArtwork.source.endsWith('notification_logo.svg'))
  assert.deepEqual(notificationArtwork.pixels, [[0,0,0,255],[35,35,35,255],[71,71,71,255],[255,210,220,255],[255,255,255,255]])
  await screenshot('notification-request')
  await setViewport({ width:393,height:852,deviceScaleFactor:3,mobile:true })
  await screenshot('notification-request-3x')
  await navigate('login/index')
  await waitFor('Boolean(document.querySelector(".offline"))')
  await screenshot('login')
  const viewports = []
  for (const [width, height] of [[320,568],[375,667],[393,852],[430,932],[460,820],[768,1024],[1280,800],[393,400]]) {
    await setViewport({ width, height, deviceScaleFactor: 1, mobile: width < 768 })
    await waitFor(`innerWidth === ${width} && innerHeight === ${height}`)
    await delay(100)
    const metrics = await evaluate(`(() => {
      const rect = selector => {const r=document.querySelector(selector).getBoundingClientRect();return {left:r.left,right:r.right,top:r.top,bottom:r.bottom,width:r.width,height:r.height}};
      return {width:innerWidth,height:innerHeight,page:rect('.login-page'),logo:rect('.login-logo'),field:rect('.input-wrap'),button:rect('.login-button'),offline:rect('.offline'),install:rect('.install-entry'),font:getComputedStyle(document.querySelector('.input')).fontSize,fontFamily:getComputedStyle(document.querySelector('input')).fontFamily,scrollWidth:document.documentElement.scrollWidth};
    })()`)
    assert.ok(Math.abs(metrics.logo.width - 200) < 1, JSON.stringify(metrics))
    assert.ok(Math.abs(metrics.field.height - 45) < 1, JSON.stringify(metrics))
    assert.ok(Math.abs(metrics.button.width - 170) < 1 && Math.abs(metrics.button.height - 40) < 1, JSON.stringify(metrics))
    assert.equal(metrics.font, '16px')
    assert.ok(metrics.fontFamily.includes('Microsoft YaHei'), 'Inputs must inherit the app font')
    assert.ok(metrics.page.width <= 480 && Math.abs(metrics.page.left - (width - metrics.page.width)/2) < 1, JSON.stringify(metrics))
    assert.ok(metrics.install.top > metrics.offline.bottom + 8, 'Install action overlaps login controls')
    assert.ok(metrics.scrollWidth <= width, 'Horizontal overflow')
    await screenshot(`login-${width}x${height}`)
    viewports.push(metrics)
  }
  await setViewport({width:393,height:852,deviceScaleFactor:1,mobile:true})
  await delay(100)
  // Cached fixtures exercise offline app data without touching real user accounts.
  await evaluate('document.querySelector(".offline").click(); true')
  await waitFor('Boolean(document.querySelector(".home-page"))')
  await screenshot('home')
  await evaluate(`(() => {
    const put=(key,data)=>localStorage.setItem(key,JSON.stringify({type:typeof data,data}));
    put('gong_calendar',{start:'2026-08-31',weeks:20});
    put('gong_course_map',Object.fromEntries(['Monday','Tuesday','Wednesday','Thursday','Friday','Saturday','Sunday'].map(day=>[day,[
      {day,name:'数据结构',teacher:'测试老师',classroom:'逸夫楼101',weeks:'1-20',start_time:1,duration:2},
      {day,name:'大学英语',teacher:'测试老师',classroom:'三教201',weeks:'1-20',start_time:5,duration:2}
    ]])));
    put('gong_exams',[{name:'数据结构',start_time:'2026-11-09 09:00:00',end_time:'2026-11-09 11:00:00',location:'逸夫楼101'}]);
    put('gong_academic',{major:{gpa:3.8,average_score:90,total_credit:[150,80],compulsory_credit:[100,60],scores:[{name:'数据结构',type:'必修',score:90,credit:3,term:5}]}});
    for(const key of ['gong_classroom_today','gong_classroom_tomorrow']) put(key,{'逸夫楼':Array.from({length:20},(_,index)=>({name:String(101+index),status:['空','空','满','空','空']}))});
    return true;
  })()`)
    await reload()
  await waitFor('Boolean(document.querySelector(".home-page.has-exams .course-name"))')
  await waitFor('Array.from(document.querySelectorAll(".app-nav img")).every(image=>image.naturalWidth > 0 && image.src.endsWith(".svg"))')
  await screenshot('home-with-data')
  const layout = await evaluate(`(() => {
    const rect = selector => { const r=document.querySelector(selector).getBoundingClientRect(); return {top:r.top,bottom:r.bottom,height:r.height} };
    return {nav:rect('.app-nav'),body:rect('.body'),exam:rect('.exam-area'),height:innerHeight,width:innerWidth,scroll:document.documentElement.scrollWidth};
  })()`)
  assert.ok(layout.exam.bottom <= layout.nav.top + 2, JSON.stringify(layout))
  assert.ok(layout.scroll <= layout.width, 'Horizontal page overflow')
  // Exercise real CSS env() values, including iOS reporting an absent toolbar.
  // This checks geometry; it does not emulate WebKit's system status-bar blur.
  const safeAreaLayouts = []
  const standaloneScript = await send('Page.addScriptToEvaluateOnNewDocument', {
    source: 'Object.defineProperty(navigator,"standalone",{get:()=>true,configurable:true})',
  })
  await reload()
  await waitFor('Boolean(document.querySelector(".home-page.has-exams .course-name")) && document.documentElement.hasAttribute("data-ios-standalone")')
  assert.equal(await evaluate('document.querySelector("meta[name=apple-mobile-web-app-status-bar-style]").content'), 'default')
  for (const scenario of [
    { name:'no-home-indicator', top:0, bottom:0, width:393, height:852 },
    { name:'iphone-portrait', top:62, bottom:34, width:402, height:874 },
    { name:'iphone-extra-toolbar', top:62, bottom:109, width:402, height:874 },
    { name:'iphone-landscape', top:0, bottom:21, width:874, height:402 },
    { name:'reported-ios27-viewport', top:62, bottom:34, width:402, height:812, screenHeight:874, expectedHeight:874, topClearance:16 },
  ]) {
    const expectedHeight = scenario.expectedHeight || scenario.height
    await send('Network.setUserAgentOverride', {userAgent:scenario.screenHeight
      ? 'Mozilla/5.0 (iPhone; CPU iPhone OS 18_7 like Mac OS X) AppleWebKit/605.1.15 Version/27.0 Mobile/15E148 Safari/604.1'
      : 'Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 Version/18.0 Mobile/15E148 Safari/604.1'})
    await setViewport({width:scenario.width,height:scenario.height,screenWidth:scenario.width,screenHeight:scenario.screenHeight || scenario.height,deviceScaleFactor:3,mobile:true})
    await send('Emulation.setSafeAreaInsetsOverride', { insets: {top:scenario.top,bottom:scenario.bottom,left:0,right:0} })
    for (const page of ['home','classroom','academic','activity']) {
      await navigate(`${page}/index`)
      await waitFor(`Boolean(document.querySelector('.${page}-page .app-nav'))`)
      await delay(120)
      const metrics = await evaluate(`(() => {
        const nav=document.querySelector('.app-nav');const box=nav.getBoundingClientRect();
        const item=nav.querySelector('.nav-item').getBoundingClientRect();
        const content=document.querySelector('.body,.room-list,.academic-scroll');
        const head=document.querySelector('.main-info,.date-title,.top-tabs>uni-view:not(.tab-indicator),.page-title');
        return {height:innerHeight,nav:{top:box.top,bottom:box.bottom,height:box.height},
          itemBottom:item.bottom,padding:parseFloat(getComputedStyle(nav).paddingBottom),
          headTop:head.getBoundingClientRect().top,contentBottom:content?.getBoundingClientRect().bottom};
      })()`)
      assert.ok(Math.abs(metrics.nav.bottom-expectedHeight)<1, JSON.stringify({scenario,page,metrics}))
      assert.equal(metrics.nav.height, 75 + Math.min(scenario.bottom,34))
      assert.equal(metrics.padding, Math.min(scenario.bottom,34))
      assert.ok(metrics.itemBottom<=expectedHeight-metrics.padding, JSON.stringify({scenario,page,metrics}))
      assert.ok(metrics.headTop>=scenario.top+(scenario.topClearance || 0), JSON.stringify({scenario,page,metrics}))
      if (metrics.contentBottom) assert.ok(metrics.contentBottom<=metrics.nav.top+2, JSON.stringify({scenario,page,metrics}))
      safeAreaLayouts.push({scenario:scenario.name,page,...metrics})
      assert.equal(await evaluate('getComputedStyle(document.querySelector(".app-nav")).position'), 'absolute')
      // Model a shorter fixed containing block while the app page still fills
      // its viewport. The footer must use the app page, not that ancestor.
      const moved = await evaluate(`(() => {
        const wrapper=document.querySelector('uni-page-wrapper');
        wrapper.style.transform='translateZ(0)';wrapper.style.height='calc(100% - 62px)';
        const nav=document.querySelector('.app-nav');const before=nav.getBoundingClientRect().bottom;
        nav.style.position='fixed';const oldBottom=nav.getBoundingClientRect().bottom;
        nav.style.removeProperty('position');
        wrapper.style.removeProperty('transform');wrapper.style.removeProperty('height');
        return {before,oldBottom};
      })()`)
      assert.ok(Math.abs(moved.before-expectedHeight)<1, JSON.stringify({scenario,page,moved}))
      assert.ok(moved.oldBottom<expectedHeight-50, JSON.stringify({scenario,page,moved}))
      if (scenario.name==='iphone-extra-toolbar') await screenshot(`ios-safe-area-${page}`)
    }
  }
  // Regular iOS browser tabs must keep their own toolbar inset; don't cap them.
  await setViewport({width:402,height:874,deviceScaleFactor:3,mobile:true})
  const originalActivities = await evaluate('localStorage.getItem("gong_activities")')
  await evaluate(`localStorage.setItem('gong_activities',JSON.stringify({type:'object',data:Array.from({length:20},(_,i)=>({id:'ios-scroll-'+i,title:'滚动活动 '+i,startDate:'2026-10-01',endDate:'2099-01-01'}))})); true`)
  await navigate('activity/index')
  await reload()
  await waitFor('document.querySelectorAll(".activity-card").length === 20')
  const activityScroll = await evaluate(`(()=>{const content=document.querySelector('.activity-content');const nav=document.querySelector('.app-nav');const before=nav.getBoundingClientRect().bottom;content.scrollTop=content.scrollHeight;return {before,after:nav.getBoundingClientRect().bottom,scroll:content.scrollTop,overflow:getComputedStyle(content).overflowY}})()`)
  assert.ok(activityScroll.scroll>100)
  assert.equal(activityScroll.overflow,'auto')
  assert.equal(activityScroll.after,874)
  assert.equal(activityScroll.before,activityScroll.after)
  await evaluate(`localStorage.${originalActivities === null ? 'removeItem' : 'setItem'}('gong_activities'${originalActivities === null ? '' : ','+JSON.stringify(originalActivities)}); true`)
  await navigate('home/index')
  await reload()
  await waitFor('Boolean(document.querySelector(".home-page .app-nav"))')
  await delay(150)
  assert.ok(await evaluate('Boolean(document.querySelector(".pwa-status-background"))'))
  await evaluate('document.querySelector(".menu-button").click(); true')
  await waitFor('Boolean(document.querySelector(".drawer .menus"))')
  await evaluate('document.querySelector(".drawer .menus uni-view").click(); true')
  await waitFor('Boolean(document.querySelector(".layout-feedback"))')
  await evaluate('Object.defineProperty(navigator.clipboard,"writeText",{value:async data=>{window.__layoutFeedback=JSON.parse(data)},configurable:true}); document.querySelector(".layout-feedback").click(); true')
  await waitFor('document.querySelector(".uni-modal__bd")?.textContent.includes("20261009-ios3")')
  await evaluate('Array.from(document.querySelectorAll(".uni-modal__btn")).find(item=>item.textContent.includes("复制信息")).click(); true')
  await waitFor('Boolean(window.__layoutFeedback)')
  const feedback = await evaluate('window.__layoutFeedback')
  assert.equal(feedback.revision,'20261009-ios3')
  assert.equal(feedback.mainLayout.navPosition,'absolute')
  assert.equal(feedback.mainLayout.nav.bottom,874)
  assert.equal(feedback.mainLayout.iosStandalone,true)
  assert.match(feedback.activeWorker,/^[0-9a-f]{16}$/)
  assert.ok(!JSON.stringify(feedback).includes('browser-fixture-token'))
  writeFileSync(resolve(artifactDir,'layout-feedback.json'),JSON.stringify(feedback,null,2))
  await send('Page.removeScriptToEvaluateOnNewDocument', {identifier:standaloneScript.identifier})
  await send('Emulation.setSafeAreaInsetsOverride', {insets:{top:0,bottom:21,left:0,right:0}})
  await setViewport({width:393,height:852,deviceScaleFactor:1,mobile:true})
  await navigate('home/index')
  await reload()
  await waitFor('Boolean(document.querySelector(".home-page")) && !document.documentElement.hasAttribute("data-ios-standalone")')
  assert.equal(await evaluate('parseFloat(getComputedStyle(document.querySelector(".app-nav")).paddingBottom)'),21)
  await send('Emulation.setSafeAreaInsetsOverride', {insets:{top:0,bottom:109,left:0,right:0}})
  assert.equal(await evaluate('document.querySelector(".app-nav").getBoundingClientRect().height'),184)
  const androidScript = await send('Page.addScriptToEvaluateOnNewDocument', {
    source: 'Object.defineProperty(navigator,"standalone",{get:()=>true,configurable:true})',
  })
  await send('Network.setUserAgentOverride', {userAgent:'Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/140.0.0.0 Mobile Safari/537.36'})
  await reload()
  await waitFor('Boolean(document.querySelector(".home-page")) && !document.documentElement.hasAttribute("data-ios-standalone")')
  assert.equal(await evaluate('document.querySelector(".app-nav").getBoundingClientRect().height'),184)
  await send('Page.removeScriptToEvaluateOnNewDocument', {identifier:androidScript.identifier})
  await send('Network.setUserAgentOverride', {userAgent:'Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 Version/18.0 Mobile/15E148 Safari/604.1'})
  await send('Emulation.setSafeAreaInsetsOverride', {insets:{top:0,bottom:0,left:0,right:0}})
  for (const width of [320,460,768,1280]) {
    await setViewport({width,height:852,deviceScaleFactor:1,mobile:width<768})
    await delay(100)
    const metrics=await evaluate(`(() => {const page=document.querySelector('.home-page').getBoundingClientRect();const nav=document.querySelector('.app-nav').getBoundingClientRect();const exam=document.querySelector('.exam-area').getBoundingClientRect();return {page:{left:page.left,right:page.right,width:page.width},nav:{left:nav.left,right:nav.right,width:nav.width,top:nav.top},examBottom:exam.bottom};})()`)
    assert.ok(Math.abs(metrics.nav.left-metrics.page.left)<1 && Math.abs(metrics.nav.right-metrics.page.right)<1,JSON.stringify(metrics))
    assert.ok(metrics.examBottom<=metrics.nav.top+2,JSON.stringify(metrics))
    await screenshot(`home-${width}`)
  }
  await setViewport({width:393,height:852,deviceScaleFactor:1,mobile:true})
  await delay(100)
  await send('Fetch.enable', {patterns:[{urlPattern:'*__image_check__*'}]})
  await evaluate(`(() => {
    const activities=[
      {id:'image-wide',title:'横图检查',posterTitle:'活动海报',posterSubtitle:'保持比例，居中裁切',startDate:'2026-10-09',endDate:'2099-12-31',posterUrl:'http://101.43.27.78:8000/__image_check__/wide.svg',images:['/__image_check__/wide.svg','/__image_check__/tall.svg','/__image_check__/missing.svg']},
      {id:'image-tall',title:'竖图检查',posterTitle:'竖版海报',startDate:'2026-10-09',endDate:'2099-12-31',posterUrl:'/__image_check__/tall.svg'},
      {id:'image-missing',title:'坏图检查',posterTitle:'失败时保留可读标题',startDate:'2026-10-09',endDate:'2099-12-31',posterUrl:'/__image_check__/missing.svg'},
      {id:'image-past',title:'往期带图活动',posterTitle:'归档海报',startDate:'2020-10-09',endDate:'2020-10-10',posterUrl:'/__image_check__/wide.svg'},
    ];
    localStorage.setItem('gong_activities',JSON.stringify({type:'object',data:activities}));return true;
  })()`)
  await reload()
  await waitFor('Boolean(document.querySelector(".home-poster-swiper .with-image img")?.naturalWidth)')
  const posterLayout=await evaluate(`(()=>{const rect=selector=>{const r=document.querySelector(selector).getBoundingClientRect();return {width:r.width,height:r.height,top:r.top,bottom:r.bottom}};return {width:innerWidth,height:innerHeight,scale:visualViewport.scale,viewportWidth:visualViewport.width,page:rect('.home-page'),nav:rect('.app-nav'),font:getComputedStyle(document.querySelector('.title')).fontSize,rpx:getComputedStyle(document.documentElement).getPropertyValue('--rpx')}})()`)
  assert.equal(posterLayout.scale,1,JSON.stringify(posterLayout))
  assert.equal(posterLayout.font,'20px')
  await screenshot('home-with-posters')
  await navigate('activity/index')
  await waitFor('document.querySelectorAll(".activity-thumb uni-image").length === 3')
  await evaluate('window.scrollTo(0,document.body.scrollHeight); true')
  await waitFor('document.querySelectorAll(".activity-poster.with-image").length >= 2')
  await waitFor('document.querySelectorAll(".activity-thumb .thumb-fallback").length === 1')
  assert.equal(await evaluate('document.querySelectorAll(".activity-poster .poster-shade").length'),2)
  assert.equal(await evaluate('document.querySelectorAll(".activity-poster")[2].classList.contains("with-image")'),false)
  await evaluate('window.scrollTo(0,0); true')
  await screenshot('activity-with-posters')
  await evaluate('document.querySelectorAll(".period-switch > uni-view")[1].click(); true')
  await waitFor('document.querySelectorAll(".activity-card").length === 1 && document.querySelector(".activity-card").textContent.includes("往期带图活动")')
  await waitFor('Boolean(document.querySelector(".activity-thumb img")?.naturalWidth)')
  await screenshot('activity-past-with-image')
  await navigate('activity/detail?id=image-wide')
  await waitFor('Boolean(document.querySelector(".hero.with-image img")?.naturalWidth)')
  await evaluate('window.scrollTo(0,document.querySelector(".image-card").offsetTop); true')
  await waitFor('document.querySelectorAll(".image-card img")[1]?.naturalWidth > 0 && Boolean(document.querySelector(".image-fallback"))')
  const contentImageMetrics=await evaluate(`Array.from(document.querySelectorAll('.image-card img')).slice(0,2).map(image=>{const rect=image.closest('uni-image').getBoundingClientRect();return {width:rect.width,height:rect.height,ratio:image.naturalWidth/image.naturalHeight,background:getComputedStyle(image.closest('uni-image').querySelector('div')).backgroundSize}})`)
  for(const metrics of contentImageMetrics) assert.ok(Math.abs(metrics.width/metrics.height-metrics.ratio)<.01,JSON.stringify(metrics))
  await screenshot('activity-content-images')
  await evaluate('document.querySelector(".image-card uni-image").click(); true')
  await waitFor('Boolean(document.querySelector("#u-a-p > div"))')
  await screenshot('activity-image-preview')
  await evaluate('document.querySelector("#u-a-p > div").click(); true')
  await waitFor('!document.querySelector("#u-a-p > div")')
  // Navigate away also dismisses uni-app's image preview.
  await send('Page.navigate',{url:'http://127.0.0.1:4173/#/pages/activity/detail?id=image-missing'})
  await reload()
  await waitFor('Boolean(document.querySelector(".hero-title"))')
  assert.equal(await evaluate('Boolean(document.querySelector(".hero.with-image,.hero-shade"))'),false)
  await screenshot('activity-failed-poster')
  await send('Fetch.disable')
  await navigate('home/index')
  await waitFor('Boolean(document.querySelector(".home-page"))')
  await send('Network.emulateNetworkConditions', { offline: true, latency: 0, downloadThroughput: 0, uploadThroughput: 0 })
  await reload()
  await waitFor('Boolean(document.querySelector(".home-page"))')
  for (const [page, selector] of [['course/index','.course-page'], ['academic/index','.academic-page'], ['classroom/index','.classroom-page'], ['activity/index','.activity-page'], ['settings/index','.settings'], ['about/index','.about-page']]) {
    await navigate(page)
    await waitFor(`Boolean(document.querySelector(${JSON.stringify(selector)}))`)
    if (page === 'academic/index') {
      await evaluate('document.querySelectorAll(".top-tabs > uni-view")[1].click(); true')
      await waitFor('Boolean(document.querySelector(".score-list"))')
      const metrics = await evaluate(`(() => {const list=document.querySelector('.score-list').getBoundingClientRect();const nav=document.querySelector('.app-nav').getBoundingClientRect();const tab=document.querySelector('.tab-indicator').getBoundingClientRect();const page=document.querySelector('.academic-page').getBoundingClientRect();return {bottom:list.bottom,navTop:nav.top,tabCenter:tab.left+tab.width/2,expectedCenter:page.left+page.width*.75};})()`)
      assert.ok(metrics.bottom <= metrics.navTop + 1, JSON.stringify(metrics))
      assert.ok(Math.abs(metrics.tabCenter-metrics.expectedCenter) < 1, JSON.stringify(metrics))
    }
    if (page === 'classroom/index') {
      await evaluate('document.querySelectorAll(".day-tabs > uni-view")[1].click(); true')
      await waitFor('Boolean(document.querySelector(".room-list"))')
      const metrics = await evaluate(`(() => {const list=document.querySelector('.room-list').getBoundingClientRect();const nav=document.querySelector('.app-nav').getBoundingClientRect();const tab=document.querySelector('.day-indicator').getBoundingClientRect();const page=document.querySelector('.classroom-page').getBoundingClientRect();return {bottom:list.bottom,navTop:nav.top,tabCenter:tab.left+tab.width/2,expectedCenter:page.left+page.width*.75};})()`)
      assert.ok(metrics.bottom <= metrics.navTop + 1, JSON.stringify(metrics))
      assert.ok(Math.abs(metrics.tabCenter-metrics.expectedCenter) < 1, JSON.stringify(metrics))
    }
    await screenshot(page.replace('/', '-'))
  }
  await navigate('course/index')
  await waitFor('Boolean(document.querySelector(".course-block"))')
  await evaluate('document.querySelectorAll(".table-body")[1].querySelectorAll(".grid-cell")[2].click(); true')
  await waitFor('Boolean(document.querySelector(".editor-dialog"))')
  await screenshot('course-editor')
  await evaluate(`(() => {
    const fields=document.querySelectorAll('.editor-dialog input');
    fields[0].value='测试自定义课程'; fields[0].dispatchEvent(new Event('input',{bubbles:true}));
    return true;
  })()`)
  await waitFor('!document.querySelector(".editor-actions .save").hasAttribute("disabled")')
  await evaluate('document.querySelector(".editor-actions .save").click(); true')
  await waitFor('!document.querySelector(".editor-dialog")')
  await waitFor('Boolean(Array.from(document.querySelectorAll(".course-block")).find(e=>e.textContent.includes("测试自定义课程")))')
  await reload()
  await waitFor('Boolean(Array.from(document.querySelectorAll(".course-block")).find(e=>e.textContent.includes("测试自定义课程")))')
  await evaluate('Array.from(document.querySelectorAll(".course-block")).find(e=>e.textContent.includes("测试自定义课程")).click(); true')
  await waitFor('Boolean(document.querySelector(".editor-actions .delete"))')
  await evaluate('document.querySelector(".editor-actions .delete").click(); true')
  await waitFor('!document.querySelector(".editor-dialog")')
  await waitFor('document.querySelector(".table-body").textContent.includes("数据结构")')
  const cachedUrls = await evaluate(`(async()=>{ const urls=[]; for(const name of await caches.keys()) { for(const request of await (await caches.open(name)).keys()) urls.push(request.url) } return urls })()`)
  assert.ok(!cachedUrls.some((url) => url.includes('/api/')))
  assert.equal(exceptions.length, 0, JSON.stringify(exceptions))
  writeFileSync(resolve(artifactDir, 'result.json'), JSON.stringify({ layout, safeAreaLayouts, viewports, notificationArtwork, posterLayout, decodedAssets:staticAssets.length, contentImageMetrics, offlinePages: 6, cachedFiles: cachedUrls.length, exceptions }, null, 2))
  console.log(`Browser checks passed: iPhone-size layout, 20 safe-area layouts including reported iOS27 viewport and Android/browser isolation, install entry, real worker activation, offline restart and 6 lazy routes, native artwork colors/3x rendering, all SVG/raster decodes, landscape/portrait/broken posters and original image preview, course create/reopen/delete, no API cache. Screenshots: ${artifactDir}`)
} catch (error) {
  if (socket?.readyState === 1) {
    await screenshot('failure').catch(() => {})
    console.error(await evaluate(`({url:location.href,text:document.body.textContent.slice(-1200),thumbs:document.querySelectorAll('.activity-thumb').length,images:document.querySelectorAll('.activity-thumb img').length})`).catch(() => null))
  }
  throw error
} finally {
  socket?.close()
  browser.kill()
  for (const task of pending.values()) clearTimeout(task.timeout)
}

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'

const defaultForm = () => ({
  id: '',
  title: '',
  organizer: '',
  participation: '',
  location: '',
  startDate: '',
  endDate: '',
  posterTitle: '',
  posterSubtitle: '',
  posterUrl: '',
  images: [],
  description: '',
  featured: false,
  published: true,
  sortOrder: 0,
  filterTags: [],
})

const inferredApiBase = window.location.hostname === 'localhost' ? 'http://localhost:8000' : `${window.location.origin}/api`
const apiBase = ref(localStorage.getItem('gong_activity_api') || inferredApiBase)
const adminToken = ref(sessionStorage.getItem('gong_activity_token') || '')
const activities = ref([])
const activityFilters = ref([])
const filterDraft = ref('')
const activeSection = ref('activities')
const logs = ref([])
const logsLoading = ref(false)
const userStatsLoading = ref(false)
const userStats = ref(null)
const statsFrom = ref('')
const statsTo = ref(localDateKey())
const logLevel = ref('')
const logEvent = ref('login')
const logQuery = ref('')
const loading = ref(false)
const saving = ref(false)
const uploading = ref(false)
const connected = ref(false)
const editorOpen = ref(false)
const error = ref('')
const notice = ref('')
const form = reactive(defaultForm())
const todayKey = ref(localDateKey())
let dateRefreshTimer

const allowedImageTypes = new Set(['image/png', 'image/jpeg', 'image/webp'])
const maxOriginalImageBytes = 8 * 1024 * 1024
const maxCompressedImageBytes = 1024 * 1024
const maxContentImageCount = 9

const currentActivities = computed(() => activities.value.filter((item) => !isPastActivity(item)))
const pastActivities = computed(() => activities.value
  .filter(isPastActivity)
  .sort((left, right) => normalizedDate(right.endDate).localeCompare(normalizedDate(left.endDate))))
const activityGroups = computed(() => [
  { key: 'current', title: '近期活动', subtitle: '未结束的活动，正常展示在客户端活动页', items: currentActivities.value },
  { key: 'past', title: '往期活动', subtitle: '结束日期已过，系统自动归档', items: pastActivities.value },
])
const loginLogCount = computed(() => logs.value.filter((item) => item.event === 'login').length)
const failedLoginCount = computed(() => logs.value.filter((item) => item.event === 'login' && (item.status >= 400 || item.message)).length)
const formDurationLabel = computed(() => activityDurationLabel(form))

function localDateKey(date = new Date()) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function normalizedDate(value) {
  return String(value || '').trim().replaceAll('.', '-')
}

function isPastActivity(activity) {
  const endDate = normalizedDate(activity.endDate)
  return /^\d{4}-\d{2}-\d{2}$/.test(endDate) && endDate < todayKey.value
}

function activityDurationDays(activity) {
  const start = normalizedDate(activity.startDate)
  const end = normalizedDate(activity.endDate)
  if (!/^\d{4}-\d{2}-\d{2}$/.test(start) || !/^\d{4}-\d{2}-\d{2}$/.test(end)) return null
  const days = Math.round((Date.parse(`${end}T00:00:00Z`) - Date.parse(`${start}T00:00:00Z`)) / 86400000) + 1
  return days > 0 ? days : null
}

function activityDurationLabel(activity) {
  const days = activityDurationDays(activity)
  return days ? `持续 ${days} 天` : '请选择有效的开始和结束日期'
}

function endpoint(path) {
  return `${apiBase.value.replace(/\/$/, '')}${path}`
}

function absolutePoster(url) {
  if (!url) return ''
  if (/^https?:\/\//.test(url)) return url
  return endpoint(url.startsWith('/') ? url : `/${url}`)
}

function openImage(url) {
  const imageUrl = absolutePoster(url)
  if (imageUrl) window.open(imageUrl, '_blank', 'noopener,noreferrer')
}

function validateImageFile(file) {
  if (!allowedImageTypes.has(file.type)) throw new Error('仅支持 PNG、JPEG、WebP 图片')
  if (file.size > maxOriginalImageBytes) throw new Error(`“${file.name}”超过 8 MB，请先缩小后上传`)
}

function canvasToWebP(canvas, quality) {
  return new Promise((resolve, reject) => {
    canvas.toBlob(
      (blob) => blob ? resolve(blob) : reject(new Error('浏览器无法压缩这张图片')),
      'image/webp',
      quality,
    )
  })
}

async function decodeImage(file) {
  if ('createImageBitmap' in window) return createImageBitmap(file)
  const objectUrl = URL.createObjectURL(file)
  try {
    const image = new Image()
    image.src = objectUrl
    await image.decode()
    return image
  } catch (reason) {
    throw new Error(`无法读取图片“${file.name}”`)
  } finally {
    URL.revokeObjectURL(objectUrl)
  }
}

async function compressImage(file, maxDimension) {
  validateImageFile(file)
  const source = await decodeImage(file)
  const sourceWidth = source.width
  const sourceHeight = source.height
  if (!sourceWidth || !sourceHeight) throw new Error(`无法读取图片“${file.name}”的尺寸`)

  const initialScale = Math.min(1, maxDimension / Math.max(sourceWidth, sourceHeight))
  let width = Math.max(1, Math.round(sourceWidth * initialScale))
  let height = Math.max(1, Math.round(sourceHeight * initialScale))
  const canvas = document.createElement('canvas')
  const context = canvas.getContext('2d', { alpha: true })
  if (!context) throw new Error('当前浏览器不支持图片压缩')

  let compressed
  const qualities = [0.84, 0.76, 0.68, 0.60]
  for (let resizeRound = 0; resizeRound < 4; resizeRound += 1) {
    canvas.width = width
    canvas.height = height
    context.clearRect(0, 0, width, height)
    context.drawImage(source, 0, 0, width, height)
    for (const quality of qualities) {
      compressed = await canvasToWebP(canvas, quality)
      if (compressed.size <= maxCompressedImageBytes) break
    }
    if (compressed.size <= maxCompressedImageBytes) break
    width = Math.max(1, Math.round(width * 0.82))
    height = Math.max(1, Math.round(height * 0.82))
  }
  source.close?.()
  if (!compressed || compressed.size > maxCompressedImageBytes) {
    throw new Error(`“${file.name}”压缩后仍超过 1 MB，请换一张尺寸更小的图片`)
  }

  const safeName = file.name.replace(/\.[^.]+$/, '') || 'activity-image'
  return new File([compressed], `${safeName}.webp`, { type: 'image/webp' })
}

async function request(path, options = {}) {
  const headers = new Headers(options.headers || {})
  headers.set('Authorization', `Bearer ${adminToken.value}`)
  if (options.body && !(options.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json')
  }
  const response = await fetch(endpoint(path), { ...options, headers })
  if (!response.ok) {
    const message = (await response.text()).trim()
    throw new Error(message || `请求失败（${response.status}）`)
  }
  if (response.status === 204) return null
  return response.json()
}

async function connect() {
  error.value = ''
  loading.value = true
  try {
    localStorage.setItem('gong_activity_api', apiBase.value.replace(/\/$/, ''))
    sessionStorage.setItem('gong_activity_token', adminToken.value)
    activities.value = await request('/admin/activities')
    try {
      activityFilters.value = await request('/admin/activity-filters')
    } catch {
      activityFilters.value = []
    }
    connected.value = true
  } catch (reason) {
    connected.value = false
    error.value = reason.message || '无法连接活动服务'
  } finally {
    loading.value = false
  }
}

async function refresh() {
  loading.value = true
  error.value = ''
  try {
    activities.value = await request('/admin/activities')
  } catch (reason) {
    error.value = reason.message
  } finally {
    loading.value = false
  }
}

function addActivityFilter() {
  const value = filterDraft.value.trim()
  if (value && !activityFilters.value.includes(value) && activityFilters.value.length < 12) {
    activityFilters.value = [...activityFilters.value, value]
  }
  filterDraft.value = ''
}

async function saveActivityFilters() {
  error.value = ''
  try {
    activityFilters.value = await request('/admin/activity-filters', {
      method: 'PUT', body: JSON.stringify({ filters: activityFilters.value }),
    })
    notice.value = '活动筛选项已保存'
  } catch (reason) { error.value = reason.message }
}

async function loadLogs() {
  logsLoading.value = true
  error.value = ''
  try {
    const params = new URLSearchParams({ limit: '300' })
    if (logLevel.value) params.set('level', logLevel.value)
    if (logEvent.value) params.set('event', logEvent.value)
    if (logQuery.value.trim()) params.set('query', logQuery.value.trim())
    const result = await request(`/admin/logs?${params}`)
    logs.value = result.items || []
  } catch (reason) {
    error.value = reason.message || '无法读取后端日志'
  } finally {
    logsLoading.value = false
  }
}

async function loadUserStats() {
  userStatsLoading.value = true
  error.value = ''
  try {
    const params = new URLSearchParams()
    if (statsFrom.value) params.set('from', statsFrom.value)
    if (statsTo.value) params.set('to', statsTo.value)
    const suffix = params.toString() ? `?${params}` : ''
    userStats.value = await request(`/admin/user-stats${suffix}`)
  } catch (reason) {
    error.value = reason.message || '无法读取用户累计统计'
  } finally {
    userStatsLoading.value = false
  }
}

function formatStatsDate(timestamp) {
  if (!timestamp) return '—'
  return new Intl.DateTimeFormat('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date(timestamp))
}

async function switchSection(section) {
  activeSection.value = section
  error.value = ''
  notice.value = ''
  if (section === 'logs') await Promise.all([loadLogs(), loadUserStats()])
}

async function clearLogs() {
  if (!window.confirm('确定清空当前保存的后端请求日志吗？')) return
  logsLoading.value = true
  error.value = ''
  try {
    await request('/admin/logs', { method: 'DELETE' })
    logs.value = []
    notice.value = '后端日志已清空'
  } catch (reason) {
    error.value = reason.message || '无法清空日志'
  } finally {
    logsLoading.value = false
  }
}

function formatLogTime(timestamp) {
  if (!timestamp) return '—'
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false,
  }).format(new Date(timestamp))
}

function logMessage(entry) {
  const translations = {
    'Unauthorized': '账号或密码错误',
    'Account Not Initialized': '账号尚未初始化',
    'Account Locked': '账号已锁定',
    'Service Unavailable': '学校统一认证服务暂时不可用',
    'Internal Server Error': '后端内部错误',
  }
  if (entry.event === 'login') {
    if (entry.status < 400 && !entry.message) return '登录成功'
    return `登录失败：${translations[entry.message] || entry.message || `HTTP ${entry.status}`}`
  }
  return entry.message || '请求正常完成'
}

function openCreate() {
  Object.assign(form, defaultForm())
  editorOpen.value = true
}

function openEdit(activity) {
  Object.assign(form, defaultForm(), activity)
  form.images = [...(activity.images || [])]
  form.filterTags = [...(activity.filterTags || [])]
  editorOpen.value = true
}

async function saveActivity() {
  error.value = ''
  notice.value = ''
  saving.value = true
  try {
    const payload = { ...form, sortOrder: Number(form.sortOrder) || 0 }
    delete payload.id
    delete payload.createdAt
    delete payload.updatedAt
    if (form.id) {
      await request(`/admin/activities/${form.id}`, {
        method: 'PUT',
        body: JSON.stringify(payload),
      })
    } else {
      await request('/admin/activities', {
        method: 'POST',
        body: JSON.stringify(payload),
      })
    }
    editorOpen.value = false
    notice.value = form.id ? '活动已更新' : '活动已创建'
    await refresh()
  } catch (reason) {
    error.value = reason.message
  } finally {
    saving.value = false
  }
}

async function deleteActivity(activity) {
  if (!window.confirm(`确定删除“${activity.title}”吗？此操作无法撤销。`)) return
  error.value = ''
  try {
    await request(`/admin/activities/${activity.id}`, { method: 'DELETE' })
    notice.value = '活动已删除'
    await refresh()
  } catch (reason) {
    error.value = reason.message
  }
}

async function uploadPoster(event) {
  const file = event.target.files?.[0]
  if (!file) return
  uploading.value = true
  error.value = ''
  try {
    const compressedFile = await compressImage(file, 1600)
    const body = new FormData()
    body.append('poster', compressedFile)
    const result = await request('/admin/activity-posters', { method: 'POST', body })
    form.posterUrl = result.url
    notice.value = `海报已压缩并上传（${Math.ceil(compressedFile.size / 1024)} KB）`
  } catch (reason) {
    error.value = reason.message
  } finally {
    uploading.value = false
    event.target.value = ''
  }
}

async function uploadContentImages(event) {
  const files = [...(event.target.files || [])]
  if (!files.length) return
  uploading.value = true
  error.value = ''
  try {
    const remainingCount = maxContentImageCount - (form.images?.length || 0)
    if (remainingCount <= 0 || files.length > remainingCount) {
      throw new Error(`每个活动最多上传 ${maxContentImageCount} 张内容图片，当前还可添加 ${Math.max(0, remainingCount)} 张`)
    }
    let compressedBytes = 0
    for (const file of files) {
      const compressedFile = await compressImage(file, 1920)
      const body = new FormData()
      body.append('poster', compressedFile)
      const result = await request('/admin/activity-posters', { method: 'POST', body })
      form.images = [...(form.images || []), result.url]
      compressedBytes += compressedFile.size
    }
    notice.value = `${files.length} 张活动图片已压缩并上传（共 ${Math.ceil(compressedBytes / 1024)} KB）`
  } catch (reason) {
    error.value = reason.message
  } finally {
    uploading.value = false
    event.target.value = ''
  }
}

function removeContentImage(index) {
  form.images = form.images.filter((_, imageIndex) => imageIndex !== index)
}

function removePoster() {
  form.posterUrl = ''
  notice.value = '海报已移除，保存活动后生效'
}

function signOut() {
  sessionStorage.removeItem('gong_activity_token')
  adminToken.value = ''
  connected.value = false
  activities.value = []
  logs.value = []
  activeSection.value = 'activities'
}

onMounted(() => {
  if (adminToken.value) connect()
  dateRefreshTimer = window.setInterval(() => { todayKey.value = localDateKey() }, 60_000)
})

onUnmounted(() => window.clearInterval(dateRefreshTimer))
</script>

<template>
  <main v-if="!connected" class="login-shell">
    <section class="login-card">
      <div class="brand-mark">拱</div>
      <p class="eyebrow">GONG ACTIVITY</p>
      <h1>活动管理台</h1>
      <p class="login-copy">管理活动内容、展示顺序与首页推荐。</p>

      <label>
        <span>活动服务地址</span>
        <input v-model.trim="apiBase" placeholder="http://localhost:8000" />
      </label>
      <label>
        <span>管理员令牌</span>
        <input v-model="adminToken" type="password" placeholder="ACTIVITY_ADMIN_TOKEN" @keyup.enter="connect" />
      </label>
      <p v-if="error" class="message error">{{ error }}</p>
      <button class="primary wide" :disabled="loading || !apiBase || !adminToken" @click="connect">
        {{ loading ? '连接中…' : '进入管理台' }}
      </button>
    </section>
  </main>

  <div v-else class="dashboard">
    <aside class="sidebar">
      <div>
        <div class="sidebar-brand"><span>拱</span> 活动管理</div>
        <nav>
          <button class="nav-link" :class="{ active: activeSection === 'activities' }" @click="switchSection('activities')">活动内容</button>
          <button class="nav-link" :class="{ active: activeSection === 'logs' }" @click="switchSection('logs')">后端日志</button>
          <a :href="endpoint('/activities')" target="_blank">公开接口 ↗</a>
        </nav>
      </div>
      <button class="text-button" @click="signOut">退出管理台</button>
    </aside>

    <main class="content">
      <template v-if="activeSection === 'activities'">
      <header class="topbar">
        <div>
          <p class="eyebrow">CONTENT CENTER</p>
          <h1>校园活动</h1>
          <p>活动会同步展示在拱拱首页和活动页。</p>
        </div>
        <button class="primary" @click="openCreate">＋ 新建活动</button>
      </header>

      <section class="stats">
        <article><strong>{{ activities.length }}</strong><span>全部活动</span></article>
        <article><strong>{{ currentActivities.length }}</strong><span>近期活动</span></article>
        <article><strong>{{ pastActivities.length }}</strong><span>往期活动</span></article>
      </section>

      <p v-if="error" class="message error">{{ error }}</p>
      <p v-if="notice" class="message success">{{ notice }}</p>

      <section class="panel">
        <div class="panel-head">
          <div><h2>客户端筛选行</h2><span>这里的项目会显示在近期/往期下方；活动未选择项目时默认出现在全部筛选中。</span></div>
          <button class="secondary" @click="saveActivityFilters">保存筛选行</button>
        </div>
        <div class="filter-editor">
          <span v-for="(item, index) in activityFilters" :key="item" class="tag published">
            {{ item }} <button type="button" class="tag-remove" @click="activityFilters.splice(index, 1)">×</button>
          </span>
          <input v-model="filterDraft" maxlength="20" placeholder="例如：讲座、比赛、招新" @keyup.enter="addActivityFilter" />
          <button class="secondary" type="button" @click="addActivityFilter">添加</button>
        </div>
      </section>

      <section v-for="group in activityGroups" :key="group.key" class="panel activity-group-panel">
        <div class="panel-head">
          <div>
            <h2>{{ group.title }}</h2>
            <span>{{ group.subtitle }}</span>
          </div>
          <button v-if="group.key === 'current'" class="secondary" :disabled="loading" @click="refresh">{{ loading ? '刷新中…' : '刷新' }}</button>
        </div>

        <div v-if="!group.items.length && !loading" class="empty-state compact">
          <div>{{ group.key === 'current' ? '暂无近期活动' : '暂无往期活动' }}</div>
          <p>{{ group.key === 'current' ? '新建活动后会展示在这里。' : '活动结束后会自动归档到这里。' }}</p>
          <button v-if="group.key === 'current'" class="primary" @click="openCreate">新建活动</button>
        </div>

        <div v-else class="activity-list">
          <article v-for="activity in group.items" :key="activity.id" class="activity-row" :class="{ 'past-activity': group.key === 'past' }">
            <div class="poster-thumb" :class="{ 'image-openable': activity.posterUrl }" :style="activity.posterUrl ? { backgroundImage: `url(${absolutePoster(activity.posterUrl)})` } : {}" :title="activity.posterUrl ? '点击查看大图' : ''" @click="openImage(activity.posterUrl)">
              <span v-if="!activity.posterUrl">{{ activity.posterTitle?.slice(0, 2) || '活动' }}</span>
            </div>
            <div class="activity-main">
              <div class="title-line">
                <h3>{{ activity.title }}</h3>
                <span v-if="activity.featured" class="tag feature">首页推荐</span>
                <span class="tag" :class="activity.published ? 'published' : 'draft'">
                  {{ activity.published ? '已发布' : '草稿' }}
                </span>
                <span v-if="group.key === 'past'" class="tag ended">已结束</span>
              </div>
              <p>{{ activity.organizer || '未填写主办方' }} · {{ activity.location || '未填写地点' }}</p>
              <p class="date">{{ activity.startDate }} — {{ activity.endDate }} · {{ activityDurationLabel(activity) }}</p>
            </div>
            <div class="row-actions">
              <button class="secondary" @click="openEdit(activity)">编辑</button>
              <button class="danger" @click="deleteActivity(activity)">删除</button>
            </div>
          </article>
        </div>
      </section>
      </template>

      <template v-else>
        <header class="topbar">
          <div>
            <p class="eyebrow">SERVICE OBSERVABILITY</p>
            <h1>后端日志</h1>
            <p>查看客户端请求状态、耗时及可公开的错误原因。</p>
          </div>
          <button class="primary" :disabled="logsLoading" @click="loadLogs">{{ logsLoading ? '读取中…' : '刷新日志' }}</button>
        </header>

        <section class="panel user-stats-panel">
          <div class="panel-head user-stats-head">
            <div>
              <h2>用户累计</h2>
              <span>只统计登录成功的独立账号；从 {{ formatStatsDate(userStats?.trackingStartedAt) }} 开始自动记录</span>
            </div>
            <div class="stats-range">
              <label><span>从</span><input v-model="statsFrom" type="date" /></label>
              <label><span>到</span><input v-model="statsTo" type="date" /></label>
              <button class="secondary" :disabled="userStatsLoading" @click="loadUserStats">{{ userStatsLoading ? '统计中…' : '查询' }}</button>
            </div>
          </div>
          <div class="stats user-stats-grid">
            <article><strong>{{ userStats?.totalUsers ?? '—' }}</strong><span>累计用户</span></article>
            <article><strong>{{ userStats?.newUsers ?? '—' }}</strong><span>区间新增</span></article>
            <article><strong>{{ userStats?.activeUsers ?? '—' }}</strong><span>区间登录用户</span></article>
            <article><strong>{{ userStats?.loginCount ?? '—' }}</strong><span>区间登录次数</span></article>
          </div>
        </section>

        <section class="stats log-summary-stats">
          <article><strong>{{ logs.length }}</strong><span>当前日志</span></article>
          <article><strong>{{ loginLogCount }}</strong><span>日志内登录</span></article>
          <article><strong>{{ failedLoginCount }}</strong><span>日志内失败</span></article>
        </section>

        <p v-if="error" class="message error">{{ error }}</p>
        <p v-if="notice" class="message success">{{ notice }}</p>

        <section class="panel logs-panel">
          <div class="panel-head log-tools">
            <div>
              <h2>请求记录</h2>
              <span>后端持续保存最近 5000 条（无需打开管理端），当前查询最多返回 300 条</span>
            </div>
            <div class="log-filters">
              <select v-model="logEvent" @change="loadLogs">
                <option value="login">账号登录</option>
                <option value="">全部请求</option>
              </select>
              <select v-model="logLevel" @change="loadLogs">
                <option value="">全部级别</option>
                <option value="warn">仅 4xx</option>
                <option value="error">仅 5xx</option>
                <option value="info">仅正常请求</option>
              </select>
              <input v-model="logQuery" placeholder="搜索路径、状态码或错误" @keyup.enter="loadLogs" />
              <button class="secondary" @click="loadLogs">查询</button>
              <button class="danger" @click="clearLogs">清空</button>
            </div>
          </div>

          <div v-if="!logs.length && !logsLoading" class="empty-state">
            <div>暂无匹配日志</div>
            <p>部署新后端并产生客户端请求后，记录会显示在这里。</p>
          </div>
          <div v-else class="log-list">
            <div class="log-row log-header">
              <span>时间</span><span>级别</span><span>账号</span><span>请求</span><span>状态</span><span>耗时</span><span>结果 / 原因</span>
            </div>
            <article v-for="(entry, index) in logs" :key="`${entry.timestamp}-${index}`" class="log-row">
              <time>{{ formatLogTime(entry.timestamp) }}</time>
              <span class="log-level" :class="entry.level">{{ entry.level.toUpperCase() }}</span>
              <strong class="log-account">{{ entry.account || '—' }}</strong>
              <strong class="log-request"><b>{{ entry.method }}</b> {{ entry.path }}</strong>
              <span class="log-status" :class="entry.level">{{ entry.status }}</span>
              <span class="log-duration">{{ entry.durationMs }} ms</span>
              <p :class="{ muted: !entry.message && entry.event !== 'login' }">{{ logMessage(entry) }}</p>
            </article>
          </div>
        </section>
      </template>
    </main>

    <div v-if="editorOpen" class="modal-backdrop" @click.self="editorOpen = false">
      <form class="editor" @submit.prevent="saveActivity">
        <div class="editor-head">
          <div>
            <p class="eyebrow">ACTIVITY EDITOR</p>
            <h2>{{ form.id ? '编辑活动' : '新建活动' }}</h2>
          </div>
          <button type="button" class="icon-button" @click="editorOpen = false">×</button>
        </div>

        <div class="form-grid">
          <label class="full"><span>活动名称 *</span><input v-model.trim="form.title" required /></label>
          <label><span>主办方</span><input v-model.trim="form.organizer" /></label>
          <label><span>参与方式</span><input v-model.trim="form.participation" /></label>
          <label><span>活动地点</span><input v-model.trim="form.location" /></label>
          <label><span>排序值</span><input v-model.number="form.sortOrder" type="number" /></label>
          <label><span>开始日期 *</span><input v-model="form.startDate" type="date" required /></label>
          <label><span>结束日期 *</span><input v-model="form.endDate" type="date" required /></label>
          <div class="full duration-hint">
            <strong>{{ formDurationLabel }}</strong>
            <span>结束日期当天仍属于近期活动，次日自动转入往期活动。</span>
          </div>
          <label><span>海报主标题</span><input v-model.trim="form.posterTitle" placeholder="显示在海报图片上" /></label>
          <label><span>海报副标题</span><input v-model.trim="form.posterSubtitle" placeholder="显示在主标题下方" /></label>
          <label class="full"><span>活动介绍</span><textarea v-model.trim="form.description" rows="4" /></label>
          <div class="full">
            <span class="field-label">发布到哪个筛选项</span>
            <p class="field-help">不选择时默认在所有筛选项下显示。</p>
            <div class="filter-editor">
              <label v-for="item in activityFilters" :key="item" class="switch-row">
                <input v-model="form.filterTags" type="checkbox" :value="item" /><span>{{ item }}</span>
              </label>
              <span v-if="!activityFilters.length">请先在活动列表顶部添加筛选项</span>
            </div>
          </div>

          <div class="full poster-field">
            <div>
              <span class="field-label">活动海报</span>
              <p>支持 PNG、JPEG、WebP，原图最大 8 MB；上传时自动压缩为 WebP（最长边 1600px、最大 1 MB）。</p>
            </div>
            <div v-if="form.posterUrl" class="poster-preview image-openable" :style="{ backgroundImage: `url(${absolutePoster(form.posterUrl)})` }" title="点击查看大图" @click="openImage(form.posterUrl)" />
            <div class="poster-actions">
              <label class="upload-button">
                {{ uploading ? '上传中…' : (form.posterUrl ? '更换海报' : '上传海报') }}
                <input type="file" accept="image/png,image/jpeg,image/webp" :disabled="uploading" @change="uploadPoster" />
              </label>
              <button v-if="form.posterUrl" type="button" class="danger" @click="removePoster">删除海报</button>
            </div>
          </div>

          <div class="full content-images-field">
            <div class="content-images-head">
              <div>
                <span class="field-label">活动内容图片</span>
                <p>最多 9 张，原图单张最大 8 MB；自动压缩为 WebP（最长边 1920px、最大 1 MB）。仅展示在详情页，不会替代海报。</p>
              </div>
              <label class="upload-button">
                {{ uploading ? '上传中…' : '添加图片' }}
                <input type="file" multiple accept="image/png,image/jpeg,image/webp" :disabled="uploading" @change="uploadContentImages" />
              </label>
            </div>
            <div v-if="form.images?.length" class="content-image-grid">
              <div v-for="(image, index) in form.images" :key="`${image}-${index}`" class="content-image-card">
                <div class="content-image-preview image-openable" :style="{ backgroundImage: `url(${absolutePoster(image)})` }" title="点击查看大图" @click="openImage(image)">
                  <span v-if="index === 0">卡片背景</span>
                </div>
                <button type="button" class="danger" @click="removeContentImage(index)">移除</button>
              </div>
            </div>
          </div>

          <label class="switch-row"><input v-model="form.published" type="checkbox" /><span>发布到客户端</span></label>
          <label class="switch-row"><input v-model="form.featured" type="checkbox" /><span>设为首页推荐</span></label>
        </div>

        <div class="editor-actions">
          <button type="button" class="secondary" @click="editorOpen = false">取消</button>
          <button type="submit" class="primary" :disabled="saving || uploading">
            {{ saving ? '保存中…' : '保存活动' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

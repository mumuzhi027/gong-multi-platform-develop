<script setup>
import { computed, ref, watch } from 'vue'
import { onLoad, onPullDownRefresh } from '@dcloudio/uni-app'
import AppNav from '../../components/AppNav.vue'
import StateView from '../../components/StateView.vue'
import { useAuthStore } from '../../stores/auth'
import { useCampusStore } from '../../stores/campus'
import { academicTermLabel } from '../../utils/date'

const auth = useAuthStore()
const campus = useCampusStore()
const tab = ref('overview')
const scoreType = ref('major')
const statisticsType = ref('all')
const selectedTerm = ref(null)

const scoreData = computed(() => campus.academic?.[scoreType.value] || null)
const majorScore = computed(() => campus.academic?.major || null)
const rankData = computed(() => statisticsType.value === 'all' ? campus.academic?.rank : campus.academic?.compulsoryRank)
const scoreMap = computed(() => (scoreData.value?.scores || []).reduce((map, score) => {
  map[score.term] ||= []
  map[score.term].push(score)
  return map
}, {}))
const terms = computed(() => Object.keys(scoreMap.value).map(Number).sort((a, b) => b - a))
const currentScores = computed(() => scoreMap.value?.[selectedTerm.value] || [])
const scoreGroups = computed(() => [
  ['必修', currentScores.value.filter((item) => item.type === '必修')],
  ['选修', currentScores.value.filter((item) => item.type === '选修')],
  ['跨学科选修', currentScores.value.filter((item) => item.type === '跨学科选修')],
].filter(([, list]) => list.length))

function field(source, snake, camel) { return source?.[snake] ?? source?.[camel] }
function valueOrDash(value) { return value !== undefined && value !== null && String(value).trim() && String(value) !== '0' ? value : '-' }
function rankOrDash(value) { return Number(value) > 0 ? value : '-' }
function creditText(values) { return Array.isArray(values) ? `${values[1] || 0}/${values[0] || 0}` : '-/-' }
function creditPercent(values) {
  if (!Array.isArray(values) || !Number(values[0])) return '0%'
  return `${Math.min(100, Number(values[1] || 0) / Number(values[0]) * 100)}%`
}

const overviewRows = computed(() => {
  const data = scoreData.value
  if (scoreType.value === 'minor') return [
    { label: '绩点', value: valueOrDash(data?.gpa) },
    { label: '总学分', credits: field(data, 'total_credit', 'totalCredit') },
    { label: '平均成绩', value: valueOrDash(field(data, 'average_score', 'averageScore')) },
  ]
  return [
    { label: '绩点', value: valueOrDash(data?.gpa) },
    { label: '平均成绩', value: valueOrDash(field(data, 'average_score', 'averageScore')) },
    { label: '总学分', credits: field(data, 'total_credit', 'totalCredit') },
    { label: '必修学分', credits: field(data, 'compulsory_credit', 'compulsoryCredit') },
    { label: '选修学分', credits: field(data, 'elective_credit', 'electiveCredit') },
    { label: '跨学科选修学分', credits: field(data, 'cross_course_credit', 'crossCourseCredit') },
  ]
})

function numericScore(value) {
  const number = Number(value)
  if (Number.isFinite(number)) return number
  return { 优: 95, 优秀: 95, 良: 93, 良好: 93, 中: 73, 中等: 73, 及格: 62, 合格: 62, 不及格: 30, 不合格: 30 }[String(value)]
}
const weightedAverage = computed(() => {
  let total = 0
  let credits = 0
  ;(majorScore.value?.scores || []).filter((item) => item.type === '必修').forEach((item) => {
    const score = numericScore(item.score)
    const credit = Number(item.credit)
    if (Number.isFinite(score) && Number.isFinite(credit)) { total += score * credit; credits += credit }
  })
  return credits ? (total / credits).toFixed(2) : '-'
})
function weightedFor(scores) {
  let total = 0
  let credits = 0
  scores.forEach((item) => {
    const score = numericScore(item.score)
    const credit = Number(item.credit)
    if (Number.isFinite(score) && Number.isFinite(credit) && credit > 0) { total += score * credit; credits += credit }
  })
  return credits ? (total / credits).toFixed(2) : '-'
}
const statisticsRows = computed(() => {
  const rank = rankData.value
  const rows = [
    ['平均学分绩点', valueOrDash(rank?.gpa)],
    ['平均成绩', valueOrDash(field(rank, 'average_score', 'averageScore'))],
    ['班级排名', rankOrDash(field(rank, 'class_rank', 'classRank'))],
    ['年级专业排名', rankOrDash(field(rank, 'major_rank', 'majorRank'))],
  ]
  if (statisticsType.value === 'required') rows.push(
    ['加权平均分', weightedAverage.value],
    ['加权平均成绩排名', rankOrDash(field(rank, 'weighted_average_rank', 'weightedAverageRank'))],
  )
  return rows
})

watch([terms, scoreType], () => { selectedTerm.value = terms.value[0] || null }, { immediate: true })
onLoad(async () => {
  if (!auth.authenticated) return uni.reLaunch({ url: '/pages/login/index' })
  if (!campus.academic?.major && !auth.offline) await refresh(false)
})
onPullDownRefresh(async () => { await refresh(true); uni.stopPullDownRefresh() })
async function refresh(force = false) {
  if (auth.offline) return uni.showToast({ title: '离线模式下无法刷新', icon: 'none' })
  await campus.loadAcademic(force).catch((error) => { if (!error.authExpired) uni.showToast({ title: error.message, icon: 'none' }) })
}
</script>

<template>
  <view class="page page-with-nav academic-page">
    <view class="top-tabs">
      <view :class="{ active: tab === 'overview' }" @tap="tab = 'overview'">学业总览</view>
      <view :class="{ active: tab === 'scores' }" @tap="tab = 'scores'">成绩表单</view>
      <view class="tab-indicator" :class="{ right: tab === 'scores' }" />
    </view>

    <StateView :loading="campus.loading.academic && !campus.academic?.major" :error="campus.errors.academic" :empty="!campus.academic?.major && !campus.academic?.minor" empty-text="暂无成绩数据" @retry="refresh(true)">
      <scroll-view v-if="tab === 'overview'" class="academic-scroll" scroll-y>
        <view class="summary panel">
          <view class="card-heading large">总览</view>
          <scroll-view class="summary-content" scroll-y>
            <view v-for="row in overviewRows" :key="row.label" :class="['academic-row',{credit:row.credits}]">
              <view class="row-line"><text class="bullet"/><text>{{ row.label }}</text><text class="academic-value">{{ row.credits ? creditText(row.credits) : row.value }}</text></view>
              <view v-if="row.credits" class="progress"><view :style="{width:creditPercent(row.credits)}"/></view>
            </view>
          </scroll-view>
          <view class="major-switch"><view :class="{active:scoreType==='major'}" @tap="scoreType='major'">主修</view><view :class="{active:scoreType==='minor'}" @tap="scoreType='minor'">辅修</view></view>
        </view>

        <view class="english panel">
          <view class="card-heading">英语</view>
          <view class="english-row">
            <view class="academic-row"><view class="row-line"><text class="bullet"/><text>CET4</text><text class="academic-value">{{ valueOrDash(majorScore?.cet4) }}</text></view></view>
            <view class="academic-row"><view class="row-line"><text class="bullet"/><text>CET6</text><text class="academic-value">{{ valueOrDash(majorScore?.cet6) }}</text></view></view>
          </view>
        </view>

        <view class="statistics panel">
          <view class="card-heading">统计</view>
          <view class="statistics-switch"><view :class="{active:statisticsType==='required'}" @tap="statisticsType='required'">必修</view><view :class="{active:statisticsType==='all'}" @tap="statisticsType='all'">必+选</view></view>
          <scroll-view class="statistics-list" scroll-y>
            <view v-for="row in statisticsRows" :key="row[0]" class="academic-row"><view class="row-line"><text class="bullet"/><text>{{ row[0] }}</text><text class="academic-value">{{ row[1] }}</text></view></view>
          </scroll-view>
        </view>
      </scroll-view>

      <view v-else class="scores-panel">
        <view class="score-type"><view :class="{active:scoreType==='major'}" @tap="scoreType='major'">主修</view><view :class="{active:scoreType==='minor'}" @tap="scoreType='minor'">辅修</view></view>
        <scroll-view class="term-scroll" scroll-x :show-scrollbar="false"><view class="term-list"><view v-for="term in terms" :key="term" :class="['term-chip',{active:term===selectedTerm}]" @tap="selectedTerm=term">{{ academicTermLabel(term) }}</view></view></scroll-view>
        <scroll-view class="score-list" scroll-y>
          <view v-if="selectedTerm" class="score-term-title">{{ academicTermLabel(selectedTerm) }}</view>
          <view v-for="group in scoreGroups" :key="group[0]" class="score-group">
            <view class="score-group-title">{{ group[0] }}</view>
            <view class="score-table-head"><text class="subject">名称</text><text>分数</text><text>学分</text></view>
            <view v-for="score in group[1]" :key="`${group[0]}-${score.name}`" class="score-row"><text class="subject score-name">{{ score.name }}</text><text class="score-value">{{ score.score }}</text><text class="score-value">{{ score.credit }}</text></view>
            <view v-if="group[0] === '必修'" class="weighted-row"><text class="subject">加权平均分</text><text>{{ weightedFor(group[1]) }}</text><text /></view>
          </view>
          <view v-if="!currentScores.length" class="empty">该学期暂无成绩</view>
        </scroll-view>
      </view>
    </StateView>
    <AppNav current="academic" />
  </view>
</template>

<style scoped lang="scss">
.academic-page{height:100vh;overflow:hidden}.top-tabs{position:relative;display:flex;height:calc(var(--status-bar-height) + 78rpx);padding-top:var(--status-bar-height);align-items:center}.top-tabs>view:not(.tab-indicator){flex:1;color:#515151;font-size:30rpx;text-align:center}.top-tabs>view.active{font-weight:500}.tab-indicator{position:absolute;bottom:0;left:calc(25% - 100rpx);width:200rpx;height:20rpx;border-radius:10rpx;background:var(--primary);transition:transform .25s}.tab-indicator.right{transform:translateX(375rpx)}.academic-scroll,.scores-panel{height:calc(100vh - var(--status-bar-height) - 204rpx - env(safe-area-inset-bottom))}.academic-scroll{padding:48rpx 20rpx 30rpx}.panel{overflow:hidden;border-radius:30rpx;background:var(--surface)}.summary{display:flex;height:670rpx;padding-top:30rpx;flex-direction:column}.card-heading{padding:0 38rpx;font-size:40rpx;font-weight:800;letter-spacing:10rpx}.card-heading.large{font-size:46rpx}.summary-content{min-height:0;padding:10rpx 0;flex:1}.academic-row{margin:6rpx 20rpx 10rpx;padding:13rpx 20rpx;border-radius:16rpx;background:var(--background);font-size:30rpx}.academic-row.credit{padding-bottom:20rpx}.row-line{display:flex;width:100%;align-items:center}.row-line>text:not(.bullet){white-space:nowrap}.bullet{width:10rpx;height:10rpx;margin:0 16rpx 0 4rpx;border-radius:50%;background:var(--primary)}.academic-value{margin-left:auto;color:var(--primary);font-size:31rpx;font-weight:700;text-align:right}.progress{height:10rpx;margin:12rpx 30rpx 0;overflow:hidden;border-radius:10rpx;background:var(--surface)}.progress view{height:100%;border-radius:10rpx;background:var(--primary)}.major-switch{display:flex;border-top:4rpx solid var(--background)}.major-switch view{padding:11rpx;flex:1;font-size:28rpx;text-align:center}.major-switch .active{background:var(--primary)}.english{min-height:206rpx;margin-top:30rpx;padding:28rpx 0 24rpx}.english-row{display:flex;margin-top:18rpx}.english-row>.academic-row{min-width:0;margin-right:10rpx;margin-left:20rpx;flex:1}.english-row>.academic-row+ .academic-row{margin-right:20rpx;margin-left:0}.statistics{height:440rpx;margin-top:30rpx;padding:28rpx 0 20rpx}.statistics-switch{display:flex;margin:18rpx 20rpx 0;padding:4rpx;border-radius:40rpx;background:var(--background)}.statistics-switch view{padding:18rpx 12rpx;flex:1;border-radius:36rpx;font-size:27rpx;font-weight:700;text-align:center}.statistics-switch .active{background:var(--primary)}.statistics-list{height:270rpx;margin-top:12rpx}.scores-panel{padding:25rpx 20rpx 0}.score-type{display:flex;padding-bottom:20rpx;justify-content:space-evenly}.score-type view{padding:4rpx 48rpx;border-radius:40rpx;font-size:32rpx}.score-type .active,.term-chip.active{background:var(--primary);font-weight:400}.term-scroll{height:92rpx;white-space:nowrap}.term-list{display:inline-flex;padding-bottom:20rpx;gap:16rpx}.term-chip{padding:14rpx 28rpx;border-radius:32rpx;background:var(--surface);font-size:28rpx}.score-list{height:calc(100% - 112rpx);padding:28rpx 30rpx;border-radius:30rpx;background:var(--surface)}.score-term-title{font-size:48rpx;font-weight:800;letter-spacing:6rpx}.score-group{margin-top:28rpx}.score-group-title{font-size:40rpx;font-weight:800;letter-spacing:10rpx}.score-table-head,.score-row,.weighted-row{display:flex;width:100%;align-items:center}.score-table-head{padding:12rpx 0;color:#515151;font-size:34rpx;font-weight:700}.score-row{padding:8rpx 0;font-size:30rpx}.score-table-head text:not(.subject),.score-row>text:not(.subject),.weighted-row>text:not(.subject){width:20%;text-align:center}.subject{width:60%;min-width:0}.score-name{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.score-value{color:var(--primary);font-size:31rpx;font-weight:700}.weighted-row{margin-top:12rpx;padding:16rpx 0 8rpx;border-top:1rpx solid rgba(0,0,0,.12);font-size:31rpx;font-weight:700}.weighted-row>text:nth-child(2){color:var(--primary);font-weight:800}
</style>

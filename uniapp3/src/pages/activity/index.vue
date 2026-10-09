<script setup>
import { computed, ref } from 'vue'
import { onLoad, onPullDownRefresh } from '@dcloudio/uni-app'
import AppNav from '../../components/AppNav.vue'
import ActivityPoster from '../../components/ActivityPoster.vue'
import { useImageState } from '../../utils/image-state'
import { useAuthStore } from '../../stores/auth'
import { useCampusStore } from '../../stores/campus'
import {
  activityDurationLabel,
  activityImageUrl,
  formatActivityRange,
  isPastActivity,
  normalizeActivityDate,
} from '../../utils/activity'

const auth = useAuthStore()
const campus = useCampusStore()
const images = useImageState()
const selectedPeriod = ref('current')
const currentActivities = computed(() => campus.activities.filter((item) => !isPastActivity(item)))
const pastActivities = computed(() => campus.activities
  .filter(item => isPastActivity(item))
  .sort((left, right) => normalizeActivityDate(right.endDate).localeCompare(normalizeActivityDate(left.endDate))))
const displayedActivities = computed(() => selectedPeriod.value === 'current' ? currentActivities.value : pastActivities.value)
const posters = computed(() => currentActivities.value.slice(0, 3))

onLoad(() => {
  if (!auth.authenticated) return uni.reLaunch({ url: '/pages/login/index' })
  refresh()
})

onPullDownRefresh(async () => {
  await refresh(true)
  uni.stopPullDownRefresh()
})

async function refresh(showToast = false) {
  if (auth.offline) {
    if (showToast) uni.showToast({ title: '当前为离线模式', icon: 'none' })
    return
  }
  try {
    await campus.loadActivities()
    if (showToast) uni.showToast({ title: '活动已更新', icon: 'none' })
  } catch (error) {
    if (!error.authExpired) uni.showToast({ title: error.message || '活动加载失败', icon: 'none' })
  }
}

function openActivity(activity) {
  uni.navigateTo({ url: `/pages/activity/detail?id=${encodeURIComponent(activity.id)}` })
}
</script>

<template>
  <view class="page page-with-nav activity-page">
    <view class="activity-content">
    <view class="activity-head safe-top">
      <text class="page-title">校园活动</text>
      <text class="page-subtitle">发现湘大正在发生的新鲜事</text>
    </view>

    <swiper v-if="posters.length" class="poster-swiper" circular autoplay :interval="3000" indicator-dots indicator-color="rgba(0,0,0,.45)" indicator-active-color="#000000">
      <swiper-item v-for="activity in posters" :key="activity.id">
        <ActivityPoster :activity="activity" @open="openActivity" />
      </swiper-item>
    </swiper>

    <view class="section-head">
      <view>
        <text class="section-title">活动</text>
        <text v-if="campus.errors.activities" class="section-hint">{{ campus.errors.activities }}</text>
      </view>
      <text class="refresh" @tap="refresh(true)">{{ campus.loading.activities ? '更新中…' : '刷新' }}</text>
    </view>

    <view class="period-switch">
      <view :class="{ selected: selectedPeriod === 'current' }" @tap="selectedPeriod = 'current'">近期活动</view>
      <view :class="{ selected: selectedPeriod === 'past' }" @tap="selectedPeriod = 'past'">往期活动</view>
    </view>

    <view v-if="displayedActivities.length" class="activity-list">
      <view v-for="activity in displayedActivities" :key="`${selectedPeriod}-${activity.id}`" class="activity-card" @tap="openActivity(activity)">
        <view class="activity-thumb">
          <image v-if="activity.posterUrl" :key="activityImageUrl(activity.posterUrl)" :src="activityImageUrl(activity.posterUrl)" mode="aspectFill" lazy-load
            @load="images.load(activityImageUrl(activity.posterUrl))" @error="images.error(activityImageUrl(activity.posterUrl))" />
          <text v-if="!images.loaded(activityImageUrl(activity.posterUrl))" class="thumb-fallback">{{ (activity.posterTitle || activity.title || '活动').slice(0, 2) }}</text>
        </view>
        <view class="activity-info">
          <view class="activity-name-line">
            <text class="activity-name">{{ activity.title }}</text>
            <text v-if="selectedPeriod === 'past'" class="ended">已结束</text>
          </view>
          <text class="activity-meta">{{ activity.participation || '请关注活动后续通知' }}</text>
          <text class="activity-meta">{{ activity.location || '地点待定' }}</text>
          <text class="activity-date">{{ formatActivityRange(activity) }}</text>
          <text class="activity-duration">{{ activityDurationLabel(activity) }}</text>
        </view>
      </view>
    </view>
    <view v-else class="empty">
      <text>{{ selectedPeriod === 'current' ? '暂无近期活动' : '暂无往期活动' }}</text>
      <text v-if="selectedPeriod === 'past'" class="empty-hint">活动结束后会自动归档到这里</text>
    </view>

    </view>
    <AppNav current="activity" />
  </view>
</template>

<style scoped lang="scss">
.activity-page{padding-right:24rpx;padding-left:24rpx}.activity-head{display:flex;padding-right:6rpx;padding-bottom:24rpx;padding-left:6rpx;flex-direction:column}.page-title{font-size:50rpx;font-weight:850;letter-spacing:2rpx}.page-subtitle{margin-top:10rpx;color:var(--muted);font-size:28rpx}.poster-swiper{height:420rpx}.section-head{display:flex;min-height:92rpx;align-items:center;justify-content:space-between}.section-head>view{display:flex;flex-direction:column}.section-title{font-size:38rpx;font-weight:850}.section-hint{max-width:480rpx;margin-top:4rpx;overflow:hidden;color:#b83b48;font-size:22rpx;text-overflow:ellipsis;white-space:nowrap}.refresh{padding:16rpx;color:var(--primary-strong);font-size:27rpx;font-weight:650}.period-switch{display:flex;margin-bottom:18rpx;padding:4rpx;border-radius:40rpx;background:var(--surface)}.period-switch view{display:flex;height:66rpx;flex:1;align-items:center;justify-content:center;border-radius:36rpx;font-size:27rpx;font-weight:700}.period-switch .selected{background:var(--primary)}.activity-list{display:grid;gap:16rpx}.activity-card{display:flex;min-height:192rpx;padding:20rpx;border-radius:30rpx;background:var(--card);box-shadow:0 8rpx 28rpx rgba(57,98,123,.045)}.activity-thumb{position:relative;display:flex;width:142rpx;height:142rpx;flex:0 0 auto;overflow:hidden;align-items:center;justify-content:center;border-radius:22rpx;background:var(--card);color:#17384a;font-size:30rpx;font-weight:850}.activity-thumb image{position:absolute;inset:0;width:100%;height:100%}.thumb-fallback{position:relative}.activity-info{display:flex;min-width:0;padding-left:24rpx;flex:1;flex-direction:column}.activity-name-line{display:flex;min-width:0;align-items:center;gap:10rpx}.activity-name{min-width:0;overflow:hidden;flex:1;font-size:31rpx;font-weight:800;text-overflow:ellipsis;white-space:nowrap}.ended{padding:4rpx 10rpx;border-radius:14rpx;color:#7a5b34;background:#f6eee3;font-size:20rpx;font-weight:750}.activity-meta{margin-top:4rpx;overflow:hidden;color:var(--muted);font-size:24rpx;text-overflow:ellipsis;white-space:nowrap}.activity-date{margin-top:6rpx;font-size:23rpx;font-weight:700}.activity-duration{margin-top:2rpx;color:var(--muted);font-size:21rpx}.empty{min-height:320rpx;font-size:30rpx;font-weight:700}.empty-hint{margin-top:12rpx;font-size:23rpx;font-weight:400}
</style>

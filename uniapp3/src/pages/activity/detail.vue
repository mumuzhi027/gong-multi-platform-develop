<script setup>
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import AppNav from '../../components/AppNav.vue'
import { useAuthStore } from '../../stores/auth'
import { useCampusStore } from '../../stores/campus'
import { activityDurationLabel, activityImageUrl, formatActivityRange } from '../../utils/activity'

const auth = useAuthStore()
const campus = useCampusStore()
const activityId = ref('')
const activity = computed(() => campus.activities.find((item) => String(item.id) === activityId.value))
const contentImages = computed(() => (activity.value?.images || []).map(activityImageUrl).filter(Boolean))

onLoad(async (options) => {
  if (!auth.authenticated) return uni.reLaunch({ url: '/pages/login/index' })
  activityId.value = decodeURIComponent(options?.id || '')
  if (!campus.activities.length && !auth.offline) {
    try { await campus.loadActivities() }
    catch (error) { if (!error.authExpired) uni.showToast({ title: error.message || '活动加载失败', icon: 'none' }) }
  }
})

function goBack() {
  const pages = getCurrentPages()
  if (pages.length > 1) uni.navigateBack()
  else uni.redirectTo({ url: '/pages/activity/index' })
}

function previewPoster() {
  const url = activityImageUrl(activity.value?.posterUrl)
  if (url) uni.previewImage({ current: url, urls: [url] })
}

function previewContent(index) {
  if (contentImages.value.length) uni.previewImage({ current: contentImages.value[index], urls: contentImages.value })
}
</script>

<template>
  <view class="page page-with-nav detail-page">
    <view class="detail-head safe-top">
      <view class="back" @tap="goBack">‹</view>
      <text>活动详情</text>
    </view>

    <view v-if="activity" class="detail-content">
      <view class="hero" :class="{ 'with-image': activity.posterUrl }" @tap="previewPoster">
        <image v-if="activity.posterUrl" :src="activityImageUrl(activity.posterUrl)" mode="aspectFill" lazy-load />
        <view v-if="activity.posterUrl" class="hero-shade" />
        <view class="hero-copy">
          <text class="hero-title">{{ activity.posterTitle || activity.title }}</text>
          <text v-if="activity.posterSubtitle || activity.organizer" class="hero-subtitle">{{ activity.posterSubtitle || activity.organizer }}</text>
        </view>
      </view>

      <view class="info-card card">
        <view v-if="activity.organizer" class="info-row"><text>主办方</text><text>{{ activity.organizer }}</text></view>
        <view class="info-row"><text>活动时间</text><text>{{ formatActivityRange(activity) }}</text></view>
        <view class="info-row"><text>持续时间</text><text>{{ activityDurationLabel(activity) }}</text></view>
        <view class="info-row"><text>活动地点</text><text>{{ activity.location || '待定' }}</text></view>
      </view>

      <text class="block-title">参与方式</text>
      <view class="text-card card">{{ activity.participation || '请关注活动后续通知' }}</view>

      <text class="block-title">活动介绍</text>
      <view class="text-card card">{{ activity.description || '暂无更多活动介绍' }}</view>

      <template v-if="contentImages.length">
        <text class="block-title">活动图片</text>
        <view class="image-card card">
          <image v-for="(image, index) in contentImages" :key="`${image}-${index}`" :src="image" mode="widthFix" lazy-load @tap="previewContent(index)" />
        </view>
      </template>
    </view>

    <view v-else class="empty">
      <text>活动不存在或已下架</text>
      <button @tap="goBack">返回活动页</button>
    </view>
    <AppNav current="activity" />
  </view>
</template>

<style scoped lang="scss">
.detail-page{padding:0 24rpx}.detail-head{display:flex;height:116rpx;align-items:center;font-size:38rpx;font-weight:850}.back{display:flex;width:72rpx;height:72rpx;margin-right:8rpx;align-items:center;justify-content:center;font-family:Arial,sans-serif;font-size:72rpx;font-weight:300;line-height:1}.detail-content{padding-bottom:18rpx}.hero{position:relative;height:410rpx;overflow:hidden;border-radius:34rpx;background:var(--card);box-shadow:0 10rpx 32rpx rgba(57,98,123,.05)}.hero image,.hero-shade{position:absolute;width:100%;height:100%}.hero-shade{background:linear-gradient(180deg,transparent 34%,rgba(0,0,0,.58))}.hero-copy{position:absolute;right:34rpx;bottom:34rpx;left:34rpx;display:flex;flex-direction:column}.hero-title{color:#17384a;font-size:48rpx;font-weight:850;line-height:1.18}.with-image .hero-title{color:#fff;text-shadow:0 2rpx 14rpx rgba(0,0,0,.35)}.hero-subtitle{margin-top:8rpx;color:var(--muted);font-size:27rpx}.with-image .hero-subtitle{color:rgba(255,255,255,.9)}.info-card{display:grid;gap:22rpx;margin-top:24rpx;padding:30rpx}.info-row{display:grid;grid-template-columns:170rpx minmax(0,1fr);gap:12rpx;font-size:28rpx;line-height:1.5}.info-row>text:first-child{color:var(--muted)}.info-row>text:last-child{overflow-wrap:anywhere;font-weight:650}.block-title{display:block;margin:40rpx 4rpx 16rpx;font-size:38rpx;font-weight:850}.text-card{padding:30rpx;font-size:29rpx;line-height:1.75;overflow-wrap:anywhere}.image-card{display:grid;gap:16rpx;padding:16rpx}.image-card image{width:100%;overflow:hidden;border-radius:24rpx;background:var(--surface)}.empty button{width:260rpx;height:72rpx;margin-top:22rpx;border-radius:38rpx;background:var(--primary);font-size:26rpx;line-height:72rpx}
</style>

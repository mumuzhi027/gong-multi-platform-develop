<script setup>
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import AppNav from '../../components/AppNav.vue'
import { useImageState } from '../../utils/image-state'
import { useAuthStore } from '../../stores/auth'
import { useCampusStore } from '../../stores/campus'
import { activityDurationLabel, activityImageUrl, formatActivityRange } from '../../utils/activity'

const auth = useAuthStore()
const campus = useCampusStore()
const activityId = ref('')
const activity = computed(() => campus.activities.find((item) => String(item.id) === activityId.value))
const contentImages = computed(() => (activity.value?.images || []).map(activityImageUrl).filter(Boolean))
const posterUrl = computed(() => activityImageUrl(activity.value?.posterUrl))
const images = useImageState()

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
  if (images.loaded(url)) uni.previewImage({ current: url, urls: [url] })
}

function previewContent(index) {
  const urls = contentImages.value.filter(images.loaded)
  if (images.loaded(contentImages.value[index])) uni.previewImage({ current: contentImages.value[index], urls })
}
</script>

<template>
  <view class="page page-with-nav detail-page">
    <view class="detail-head safe-top">
      <view class="back" @tap="goBack"><image src="/static/icons/baseline_arrow_back_24.svg" mode="aspectFit" /></view>
      <text>活动详情</text>
    </view>

    <view v-if="activity" class="detail-content">
      <view class="hero" :class="{ 'with-image': images.loaded(posterUrl) }" @tap="previewPoster">
        <image v-if="posterUrl" :key="posterUrl" :src="posterUrl" mode="aspectFill"
          @load="images.load(posterUrl)" @error="images.error(posterUrl)" />
        <view v-if="images.loaded(posterUrl)" class="hero-shade" />
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
          <view v-for="(image, index) in contentImages" :key="`${image}-${index}`" class="content-image">
            <image v-show="!images.failed(image)" :src="image" mode="widthFix" lazy-load
              :aria-label="`${activity.title} 活动图片 ${index + 1}`" @load="images.load(image)" @error="images.error(image)" @tap="previewContent(index)" />
            <text v-if="images.failed(image)" class="image-fallback">图片暂时无法加载</text>
          </view>
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
.detail-page{padding:0 24rpx}
.detail-head{display:flex;height:116rpx;align-items:center;font-size:40rpx;font-weight:800}
.back{display:flex;width:76rpx;height:76rpx;margin-right:16rpx;align-items:center;justify-content:center}
.back image{width:44rpx;height:44rpx}
.detail-content{padding-bottom:36rpx}
.hero{position:relative;height:460rpx;overflow:hidden;border-radius:40rpx;background:var(--card)}
.hero image,.hero-shade{position:absolute;inset:0;width:100%;height:100%}
.hero-shade{background:linear-gradient(180deg,transparent,rgba(0,0,0,.42))}
.hero-copy{position:absolute;right:40rpx;bottom:40rpx;left:40rpx;display:flex;flex-direction:column}
.hero-title{color:var(--text);font-size:50rpx;font-weight:800;line-height:1.2;overflow-wrap:anywhere}
.with-image .hero-title{color:#fff}
.hero-subtitle{margin-top:10rpx;color:var(--muted);font-size:28rpx;line-height:40rpx}
.with-image .hero-subtitle{color:rgba(255,255,255,.88)}
.info-card{display:grid;gap:24rpx;margin-top:28rpx;padding:36rpx;border-radius:36rpx;box-shadow:none}
.info-row{display:grid;grid-template-columns:170rpx minmax(0,1fr);gap:12rpx;font-size:28rpx;line-height:40rpx}
.info-row>text:first-child{color:var(--muted)}
.info-row>text:last-child{overflow-wrap:anywhere}
.block-title{display:block;margin:44rpx 0 20rpx;font-size:40rpx;font-weight:800;line-height:56rpx}
.text-card{padding:36rpx;border-radius:36rpx;box-shadow:none;font-size:32rpx;line-height:52rpx;overflow-wrap:anywhere}
.image-card{display:grid;gap:20rpx;padding:20rpx;border-radius:36rpx;box-shadow:none}
.content-image{min-width:0}
.image-card image{display:block;width:100%;overflow:hidden;border-radius:28rpx;background:var(--surface)}
.image-fallback{display:flex;min-height:120rpx;align-items:center;justify-content:center;border-radius:28rpx;color:var(--muted);background:var(--surface);font-size:26rpx}
.empty button{width:260rpx;height:72rpx;margin-top:22rpx;border-radius:38rpx;background:var(--primary);font-size:26rpx;line-height:72rpx}
</style>

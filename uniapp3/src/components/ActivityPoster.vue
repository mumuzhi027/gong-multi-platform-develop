<script setup>
import { computed, ref, watch } from 'vue'
import { activityImageUrl } from '../utils/activity'

const props = defineProps({ activity: { type: Object, required: true } })
const emit = defineEmits(['open'])
const imageUrl = computed(() => activityImageUrl(props.activity.posterUrl))
const loaded = ref(false)
watch(imageUrl, () => { loaded.value = false })
</script>

<template>
  <view class="activity-poster" :class="{ 'with-image': loaded }" @tap="emit('open', activity)">
    <image v-if="imageUrl" :key="imageUrl" class="poster-image" :src="imageUrl" mode="aspectFill"
      :aria-label="activity.title" @load="loaded = true" @error="loaded = false" />
    <view v-if="loaded" class="poster-shade" />
    <view class="poster-copy">
      <text class="poster-kind">{{ activity.title }}</text>
      <text class="poster-title">{{ activity.posterTitle || activity.title }}</text>
      <text v-if="activity.posterSubtitle || activity.organizer" class="poster-subtitle">{{ activity.posterSubtitle || activity.organizer }}</text>
    </view>
    <text class="poster-date">{{ activity.startDate }}</text>
  </view>
</template>

<style scoped lang="scss">
.activity-poster{position:relative;width:100%;height:100%;overflow:hidden;border-radius:32rpx;background:var(--card)}
.poster-image,.poster-shade{position:absolute;inset:0;width:100%;height:100%}
.poster-shade{background:linear-gradient(180deg,rgba(0,0,0,.04),rgba(0,0,0,.52))}
.poster-copy{position:absolute;right:48rpx;bottom:36rpx;left:48rpx;display:flex;flex-direction:column}
.poster-kind{margin-bottom:12rpx;color:rgba(0,0,0,.7);font-size:24rpx;font-weight:600;line-height:32rpx;letter-spacing:4rpx}
.poster-title{color:#132e3d;font-size:50rpx;font-weight:800;line-height:60rpx}
.poster-subtitle{color:rgba(19,46,61,.8);font-size:26rpx;line-height:36rpx}
.poster-kind,.poster-title,.poster-subtitle{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.poster-date{position:absolute;top:36rpx;right:48rpx;color:rgba(19,46,61,.7);font-size:22rpx;line-height:30rpx}
.with-image .poster-kind{color:rgba(255,255,255,.86)}
.with-image .poster-title{color:#fff}
.with-image .poster-subtitle,.with-image .poster-date{color:rgba(255,255,255,.9)}
</style>

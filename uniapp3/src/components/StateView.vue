<script setup>
defineProps({
  loading: Boolean,
  error: { type: String, default: '' },
  empty: Boolean,
  emptyText: { type: String, default: '暂无数据' },
})
const emit = defineEmits(['retry'])
</script>

<template>
  <view v-if="loading" class="state">
    <view class="spinner"></view>
    <text>正在加载</text>
  </view>
  <view v-else-if="error" class="state">
    <image class="state-image" src="/static/icons/error.png" mode="aspectFit" />
    <text>{{ error }}</text>
    <button class="retry" @tap="emit('retry')">重新加载</button>
  </view>
  <view v-else-if="empty" class="state">
    <image class="state-image" src="/static/icons/course-active.png" mode="aspectFit" />
    <text>{{ emptyText }}</text>
  </view>
  <slot v-else />
</template>

<style scoped lang="scss">
.state {
  display: flex;
  min-height: 320rpx;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 20rpx;
  color: var(--muted);
  font-size: 26rpx;
  text-align: center;
}

.spinner {
  width: 40rpx;
  height: 40rpx;
  border: 5rpx solid rgba(73, 183, 246, 0.22);
  border-top-color: var(--primary-strong);
  border-radius: 50%;
  animation: rotate 0.75s linear infinite;
}

.state-icon {
  display: flex;
  width: 60rpx;
  height: 60rpx;
  align-items: center;
  justify-content: center;
  border: 4rpx solid #df756f;
  border-radius: 50%;
  color: #df756f;
  font-weight: 800;
}

.state-icon.calm {
  border-color: var(--primary-strong);
  color: var(--primary-strong);
}

.state-image { width: 64rpx; height: 64rpx; }

.retry {
  margin: 4rpx 0 0;
  padding: 0 36rpx;
  border-radius: 30rpx;
  color: #17384a;
  background: var(--primary);
  font-size: 24rpx;
  line-height: 60rpx;
}

@keyframes rotate {
  to { transform: rotate(360deg); }
}
</style>

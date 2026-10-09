<script setup>
defineProps({
  title: { type: String, default: '' },
  back: { type: Boolean, default: false },
})

const emit = defineEmits(['menu'])

function goBack() {
  const pages = getCurrentPages()
  if (pages.length > 1) uni.navigateBack()
  else uni.redirectTo({ url: '/pages/home/index' })
}
</script>

<template>
  <view class="header safe-top" :class="{ 'with-title': title }">
    <view v-if="back" class="header-button" @tap="goBack">
      <image class="header-icon back-icon" src="/static/icons/baseline_arrow_back_ios_new_24.svg" mode="aspectFit" />
    </view>
    <view v-else class="header-button" @tap="emit('menu')">
      <image class="header-icon" src="/static/icons/menu_icon.svg" mode="aspectFit" />
    </view>
    <text v-if="title" class="header-title">{{ title }}</text>
    <view class="header-spacer"></view>
  </view>
</template>

<style scoped lang="scss">
.header {
  display: flex;
  min-height: 92rpx;
  padding-right: 24rpx;
  padding-left: 24rpx;
  align-items: center;
  justify-content: space-between;
}

.header-button,
.header-spacer {
  display: flex;
  width: 64rpx;
  height: 64rpx;
  align-items: center;
  justify-content: center;
  font-size: 60rpx;
  line-height: 1;
}

.header-icon { width: 46rpx; height: 46rpx; }
.back-icon { width: 40rpx; height: 40rpx; }

.header-title {
  font-size: 34rpx;
  font-weight: 700;
}

.header.with-title {
  height: calc(var(--status-bar-height) + 120rpx);
  padding-top: var(--status-bar-height);
  border-bottom: 2rpx solid var(--surface);
}
.with-title .header-title { font-size: 40rpx; font-weight: 800; }
</style>

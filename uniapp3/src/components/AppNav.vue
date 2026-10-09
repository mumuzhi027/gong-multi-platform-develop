<script setup>
import AuthorizationDialog from './AuthorizationDialog.vue'
// #ifdef H5
import { onMounted, onBeforeUnmount } from 'vue'
import { rememberMainLayout } from '../pwa/client'
const captureLayout = () => requestAnimationFrame(rememberMainLayout)
onMounted(() => {
  captureLayout()
  window.addEventListener('resize', captureLayout)
  window.visualViewport?.addEventListener('resize', captureLayout)
})
onBeforeUnmount(() => {
  rememberMainLayout()
  window.removeEventListener('resize', captureLayout)
  window.visualViewport?.removeEventListener('resize', captureLayout)
})
// #endif
const props = defineProps({
  current: { type: String, required: true },
})

const items = [
  { key: 'home', label: '首页', icon: 'home', url: '/pages/home/index' },
  { key: 'classroom', label: '空教室', icon: 'classroom', url: '/pages/classroom/index' },
  { key: 'activity', label: '活动', icon: 'activity', url: '/pages/activity/index' },
  { key: 'academic', label: '成绩单', icon: 'grade', url: '/pages/academic/index' },
]

function navigate(item) {
  if (item.key !== props.current) uni.redirectTo({ url: item.url })
}
</script>

<template>
  <view class="app-nav">
    <view
      v-for="item in items"
      :key="item.key"
      class="nav-item"
      :class="{ active: item.key === current }"
      @tap="navigate(item)"
    >
      <image
        class="nav-icon"
        :src="`/static/icons/${item.icon}${item.key === current ? '-active' : ''}.svg`"
        mode="aspectFit"
      />
      <text>{{ item.label }}</text>
    </view>
  </view>
  <AuthorizationDialog />
</template>

<style scoped lang="scss">
.app-nav {
  position: fixed;
  z-index: 60;
  right: 0;
  bottom: 0;
  left: 0;
  display: flex;
  height: calc(126rpx + var(--safe-area-bottom));
  padding: 10rpx 24rpx var(--safe-area-bottom);
  align-items: flex-start;
  justify-content: space-between;
  border-top: 1rpx solid var(--line);
  background: rgba(248, 248, 248, 0.96);
}

.nav-item {
  display: flex;
  width: 96rpx;
  height: 96rpx;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #363636;
  font-size: 22rpx;
  transition: transform 0.2s ease;
}

.nav-item.active {
  color: var(--primary);
}

.nav-icon {
  width: 38rpx;
  height: 38rpx;
  margin-bottom: 4rpx;
}

/* #ifdef H5 */
.app-nav {
  height: var(--nav-height);
  left: var(--window-left, 0px);
  right: var(--window-right, 0px);
  padding: 0 0 var(--safe-area-bottom);
  align-items: center;
  justify-content: space-evenly;
  border-top-color: var(--surface);
  background: var(--background);
}
.nav-item { width: 100rpx; height: 100rpx; color: var(--text); font-size: 24rpx; }
.nav-item.active { color: var(--primary); }
.nav-icon { width: 40rpx; height: 40rpx; margin-bottom: 0; }
/* #endif */
</style>

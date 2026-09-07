<script setup>
import AuthorizationDialog from './AuthorizationDialog.vue'
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
        :src="`/static/icons/${item.icon}${item.key === current ? '-active' : ''}.png`"
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
  height: calc(126rpx + env(safe-area-inset-bottom));
  padding: 10rpx 24rpx env(safe-area-inset-bottom);
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
</style>

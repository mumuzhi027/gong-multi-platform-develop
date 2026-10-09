<script setup>
import { computed } from 'vue'
import { useAuthStore } from '../stores/auth'
defineProps({ visible: Boolean })
const emit = defineEmits(['close'])
const auth = useAuthStore()
const name = computed(() => auth.profile?.name || (auth.offline ? '离线用户' : '同学'))
const major = computed(() => auth.profile?.major || auth.profile?.major_name || '')
const studentId = computed(() => auth.profile?.student_id || auth.profile?.studentID || auth.profile?.studentId || '')
function open(url){ emit('close'); uni.navigateTo({url}) }
</script>
<template>
  <view v-if="visible" class="host"><view class="mask" @tap="emit('close')"/><view class="drawer">
    <view class="user-card"><text class="name">{{ name }}</text><text>{{ major || '专业信息暂无' }}</text><text>{{ studentId || '学号信息暂无' }}</text></view>
    <view class="menus"><view @tap="open('/pages/settings/index')"><image src="/static/icons/settings.svg"/><text>设置</text></view><view @tap="open('/pages/about/index')"><image src="/static/icons/about.svg"/><text>关于</text></view></view>
    <button class="logout" @tap="auth.logout">退出登录</button>
  </view></view>
</template>
<style scoped lang="scss">
/* #ifdef H5 */
.host,.mask{left:var(--window-left,0px)!important;right:var(--window-right,0px)!important}
/* #endif */
.host,.mask{position:fixed;z-index:90;inset:0}.mask{background:rgba(0,0,0,.3)}.drawer{position:absolute;z-index:91;top:0;bottom:0;left:0;width:75%;padding:calc(var(--status-bar-height) + 10rpx) 16rpx calc(30rpx + env(safe-area-inset-bottom));border-radius:0 30rpx 30rpx 0;background:var(--background);animation:slide .2s ease-out}.user-card{display:flex;margin:0 0 20rpx;padding:16rpx 20rpx;flex-direction:column;border-radius:20rpx;color:#000;background:var(--primary);font-size:28rpx;line-height:1.45}.user-card .name{font-size:40rpx;font-weight:800}.menus{margin:0}.menus view{display:flex;height:84rpx;padding:2rpx 0;align-items:center;font-size:34rpx;font-weight:800}.menus image{width:48rpx;height:48rpx;margin-right:20rpx}.logout{position:absolute;right:20rpx;bottom:calc(30rpx + env(safe-area-inset-bottom));left:20rpx;height:70rpx;border-radius:20rpx;color:#000;background:var(--primary);font-size:34rpx;font-weight:800;line-height:70rpx}@keyframes slide{from{transform:translateX(-100%)}to{transform:none}}
</style>

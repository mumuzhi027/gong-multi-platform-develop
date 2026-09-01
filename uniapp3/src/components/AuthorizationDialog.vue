<script setup>
import { computed, ref, watch } from 'vue'
import { authExpiry } from '../utils/auth-expiry'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const password = ref('')
const passwordVisible = ref(false)
const localError = ref('')
const username = computed(() => auth.username || auth.profile?.student_id || auth.profile?.studentID || '当前账号')

watch(() => authExpiry.visible, (visible) => {
  if (!visible) return
  if (uni.getStorageSync('gong_reauthentication') === false) {
    auth.logout()
    return
  }
  auth.markExpired()
  password.value = ''
  localError.value = ''
}, { immediate: true })

async function verify() {
  localError.value = ''
  try {
    await auth.reauthenticate(password.value)
    password.value = ''
    uni.showToast({ title: '验证成功', icon: 'success' })
    setTimeout(() => uni.startPullDownRefresh(), 250)
  } catch (error) {
    localError.value = error.message || '验证失败'
  }
}

function switchAccount() {
  auth.logout()
}

function enterOffline() {
  auth.enterOffline()
  password.value = ''
  uni.showToast({ title: '已进入离线模式', icon: 'none' })
}
</script>

<template>
  <view v-if="authExpiry.visible" class="auth-dialog-host">
    <view class="auth-dialog-mask" />
    <view class="auth-dialog">
      <text class="dialog-title">重新验证</text>
      <text class="dialog-user">{{ username }}</text>
      <view class="password-box">
        <input v-model="password" :password="!passwordVisible" placeholder="密码" confirm-type="done" @confirm="verify" />
        <view class="password-eye" @tap="passwordVisible = !passwordVisible">
          <image :src="passwordVisible ? '/static/icons/password_visible.png' : '/static/icons/password_invisible.png'" mode="aspectFit" />
        </view>
      </view>
      <button class="verify-button" :loading="auth.reauthLoading" :disabled="auth.reauthLoading" @tap="verify">验证</button>
      <view class="error-row">
        <text class="error-text">{{ localError || auth.reauthError }}</text>
        <text class="switch-account" @tap="switchAccount">切换账号</text>
      </view>
      <text class="offline-link" @tap="enterOffline">暂时离线模式</text>
    </view>
  </view>
</template>

<style scoped lang="scss">
.auth-dialog-host,.auth-dialog-mask{position:fixed;z-index:1000;inset:0}.auth-dialog-host{display:flex;align-items:center;justify-content:center}.auth-dialog-mask{background:rgba(0,0,0,.34)}.auth-dialog{position:relative;z-index:1001;display:flex;width:80%;padding:40rpx 32rpx;flex-direction:column;align-items:center;border-radius:28rpx;background:#fff}.dialog-title{margin-bottom:60rpx;font-size:40rpx;font-weight:800}.dialog-user{margin-bottom:40rpx;font-size:52rpx;font-weight:800}.password-box{display:flex;width:100%;height:72rpx;padding:0 28rpx;align-items:center;border:2rpx solid #969696;border-radius:60rpx}.password-box input{height:68rpx;flex:1;font-size:32rpx}.password-eye{display:flex;width:44rpx;height:44rpx;align-items:center;justify-content:center}.password-eye image{width:30rpx;height:30rpx;opacity:.55}.verify-button{width:100%;height:60rpx;margin-top:60rpx;border-radius:20rpx;color:#000;background:var(--primary);font-size:28rpx;line-height:60rpx}.error-row{display:flex;width:100%;min-height:48rpx;margin-top:20rpx;align-items:flex-start}.error-text{min-width:0;flex:1;color:#e00000;font-size:26rpx}.switch-account,.offline-link{color:var(--primary-strong);font-size:28rpx;text-decoration:underline}.switch-account{margin-left:16rpx;white-space:nowrap}.offline-link{margin-top:36rpx}
</style>

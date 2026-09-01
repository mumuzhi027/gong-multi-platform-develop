<script setup>
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useAuthStore } from '../../stores/auth'

const auth = useAuthStore()
const username = ref('')
const password = ref('')
const passwordVisible = ref(false)
const error = ref('')
const helpVisible = ref(false)
const canSubmit = computed(() => username.value.trim() && password.value)
const portalUrl = 'https://portal2020.xtu.edu.cn/'

onLoad(() => {
  if (auth.authenticated) uni.reLaunch({ url: '/pages/home/index' })
})

async function submit() {
  if (!canSubmit.value || auth.loading) return
  error.value = ''
  try {
    await auth.login(username.value.trim(), password.value)
    uni.reLaunch({ url: '/pages/notification-request/index' })
  } catch (reason) {
    error.value = reason.message || '登录失败'
    setTimeout(() => {
      error.value = ''
    }, 2800)
  }
}

function enterOffline() {
  auth.enterOffline()
  uni.reLaunch({ url: '/pages/home/index' })
}

function copyPortalUrl() {
  uni.setClipboardData({
    data: portalUrl,
    success: () => uni.showToast({ title: '门户地址已复制', icon: 'success' }),
  })
}
</script>

<template>
  <view class="page login-page safe-top">
    <view class="alert-wrap">
      <view v-if="error" class="alert">{{ error }}</view>
    </view>

    <view class="brand">
      <image class="login-logo" src="/static/images/login_logo.png" mode="widthFix" />
    </view>

    <view class="form">
      <view class="input-wrap">
        <input
          v-model="username"
          class="input"
          placeholder="用户名"
          placeholder-class="placeholder"
          confirm-type="next"
        />
      </view>
      <view class="input-wrap">
        <input
          v-model="password"
          class="input"
          placeholder="密码"
          placeholder-class="placeholder"
          :password="!passwordVisible"
          confirm-type="done"
          @confirm="submit"
        />
        <view class="eye" @tap="passwordVisible = !passwordVisible">
          <image
            :src="passwordVisible ? '/static/icons/password_visible.png' : '/static/icons/password_invisible.png'"
            mode="aspectFit"
          />
        </view>
      </view>
    </view>

    <view class="help-entry" @tap="helpVisible = true">登不进去？点我看看</view>

    <button
      class="login-button"
      :class="{ disabled: !canSubmit }"
      :loading="auth.loading"
      :disabled="!canSubmit || auth.loading"
      @tap="submit"
    >
      登录
    </button>
    <view class="offline" @tap="enterOffline">离线模式</view>

    <view v-if="helpVisible" class="help-mask" @tap="helpVisible = false" @touchmove.stop>
      <view class="help-dialog" @tap.stop>
        <view class="help-title">登录常见问题</view>
        <scroll-view class="help-content" scroll-y>
          <view class="help-item">
            <view class="help-question">账号或密码不对</view>
            <view class="help-answer">请使用学校信息门户的账号和密码。建议先在信息门户确认能够正常登录，再回到拱拱重试。</view>
            <button class="help-action" @tap="copyPortalUrl">复制信息门户地址</button>
          </view>
          <view class="help-item">
            <view class="help-question">提示“未知错误”</view>
            <view class="help-answer">短时间尝试次数过多时，信息门户可能会暂时禁用账号。请停止反复尝试，等待 24 小时后再登录。</view>
          </view>
          <view class="help-item">
            <view class="help-question">提示“该账户未初始化”</view>
            <view class="help-answer">通常是密码过于简单。请先登录信息门户修改为更安全的密码，再使用新密码登录拱拱。</view>
          </view>
          <view class="help-item">
            <view class="help-question">提示“axios error”或网络超时</view>
            <view class="help-answer">请切换网络后重试，并从微信重新进入小程序。若微信提示有新版本，请更新后再登录；仍失败时可稍后重试或先使用离线模式。</view>
          </view>
        </scroll-view>
        <button class="help-close" @tap="helpVisible = false">我知道了</button>
      </view>
    </view>
  </view>
</template>

<style scoped lang="scss">
.login-page {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.alert-wrap {
  height: 74rpx;
  margin-top: 20rpx;
}

.alert {
  max-width: 610rpx;
  padding: 13rpx 42rpx;
  overflow: hidden;
  border-radius: 40rpx;
  color: #17384a;
  background: var(--primary);
  font-size: 24rpx;
  text-overflow: ellipsis;
  white-space: nowrap;
  animation: drop 0.24s ease-out;
}

.brand {
  display: flex;
  margin-top: 100rpx;
  margin-bottom: 72rpx;
  flex-direction: column;
  align-items: center;
}

.login-logo { width: 260rpx; }

.form {
  width: 70%;
}

.input-wrap {
  display: flex;
  height: 82rpx;
  margin-bottom: 30rpx;
  padding: 0 28rpx;
  align-items: center;
  border: 2rpx solid #969696;
  border-radius: 50rpx;
}

.input {
  height: 78rpx;
  flex: 1;
  color: var(--text);
  font-size: 30rpx;
}

.placeholder { color: #888; }
.eye { display: flex; width: 60rpx; align-items: center; justify-content: center; }
.eye image { width: 28rpx; height: 28rpx; opacity: 0.48; }

.login-button {
  width: 320rpx;
  height: 76rpx;
  margin-top: 64rpx;
  border-radius: 50rpx;
  color: #17384a;
  background: var(--primary);
  font-size: 28rpx;
  font-weight: 500;
  line-height: 76rpx;
}

.help-entry {
  margin-top: 30rpx;
  color: var(--primary-strong);
  font-size: 28rpx;
  font-weight: 600;
  text-decoration: underline;
}

.help-mask {
  position: fixed;
  z-index: 1000;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  display: flex;
  padding: 48rpx;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.48);
}

.help-dialog {
  width: 100%;
  max-width: 660rpx;
  padding: 38rpx 34rpx 28rpx;
  border-radius: 30rpx;
  background: var(--card);
  box-shadow: 0 20rpx 60rpx rgba(0, 0, 0, 0.18);
}

.help-title {
  margin-bottom: 24rpx;
  font-size: 38rpx;
  font-weight: 800;
}

.help-content {
  height: min(720rpx, 58vh);
}

.help-item {
  padding-bottom: 28rpx;
}

.help-question {
  margin-bottom: 8rpx;
  font-size: 30rpx;
  font-weight: 700;
}

.help-answer {
  color: var(--muted);
  font-size: 27rpx;
  line-height: 1.65;
}

.help-action {
  width: fit-content;
  height: 62rpx;
  margin: 18rpx 0 0;
  padding: 0 28rpx;
  border-radius: 32rpx;
  color: #17384a;
  background: var(--primary);
  font-size: 25rpx;
  line-height: 62rpx;
}

.help-close {
  height: 70rpx;
  margin-top: 20rpx;
  border-radius: 36rpx;
  color: #17384a;
  background: var(--primary);
  font-size: 28rpx;
  font-weight: 600;
  line-height: 70rpx;
}

.login-button.disabled {
  color: #171717;
  background: var(--surface);
}

.offline {
  margin-top: 34rpx;
  color: var(--primary-strong);
  font-size: 34rpx;
  font-weight: 800;
  text-decoration: underline;
}

@keyframes drop {
  from { opacity: 0; transform: translateY(-20rpx); }
  to { opacity: 1; transform: translateY(0); }
}
</style>

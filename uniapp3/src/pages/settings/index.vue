<script setup>
import AppHeader from '../../components/AppHeader.vue'
// #ifdef H5
import { checkPwaUpdate, installPwa, pwaState, showLayoutFeedback } from '../../pwa/client'
// #endif
function checkUpdate(){
  // #ifdef MP-WEIXIN
  if (!uni.canIUse('getUpdateManager')) return uni.showToast({title:'当前微信版本不支持更新检查',icon:'none'})
  const manager=uni.getUpdateManager(); manager.onCheckForUpdate(({hasUpdate})=>{ if(!hasUpdate) uni.showToast({title:'已是最新版本',icon:'success'}) }); manager.onUpdateReady(()=>uni.showModal({title:'更新提示',content:'新版本已准备好，是否立即重启？',success:({confirm})=>confirm&&manager.applyUpdate()})); manager.onUpdateFailed(()=>uni.showToast({title:'更新下载失败',icon:'none'}))
  // #endif
  // #ifdef H5
  checkPwaUpdate()
  // #endif
  // #ifndef MP-WEIXIN
  // #ifndef H5
  uni.showToast({title:'已是最新版本',icon:'success'})
  // #endif
  // #endif
}
</script>
<template><AppHeader back title="设置"/><view class="page sub-page settings"><view @tap="uni.navigateTo({url:'/pages/settings/notification'})"><image src="/static/icons/notification.svg"/>通知管理</view><view @tap="uni.navigateTo({url:'/pages/settings/auth-service'})"><image src="/static/icons/authentication.svg"/>认证服务</view><view @tap="checkUpdate"><image src="/static/icons/update.svg"/>检查更新</view>
<!-- #ifdef H5 -->
<view v-if="!pwaState.installed" @tap="installPwa"><image src="/static/icons/gonggong_logo.svg"/>添加到主屏幕</view>
<view class="layout-feedback" @tap="showLayoutFeedback"><image src="/static/icons/about.svg"/>界面显示反馈</view>
<!-- #endif -->
</view></template>
<style scoped lang="scss">.settings{padding:32rpx 30rpx}.settings>view{display:flex;height:76rpx;align-items:center;font-size:36rpx;font-weight:800}.settings image{width:48rpx;height:48rpx;margin-right:24rpx}</style>

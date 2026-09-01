<script setup>
function checkUpdate(){
  // #ifdef MP-WEIXIN
  if (!uni.canIUse('getUpdateManager')) return uni.showToast({title:'当前微信版本不支持更新检查',icon:'none'})
  const manager=uni.getUpdateManager(); manager.onCheckForUpdate(({hasUpdate})=>{ if(!hasUpdate) uni.showToast({title:'已是最新版本',icon:'success'}) }); manager.onUpdateReady(()=>uni.showModal({title:'更新提示',content:'新版本已准备好，是否立即重启？',success:({confirm})=>confirm&&manager.applyUpdate()})); manager.onUpdateFailed(()=>uni.showToast({title:'更新下载失败',icon:'none'}))
  // #endif
  // #ifndef MP-WEIXIN
  uni.showToast({title:'已是最新版本',icon:'success'})
  // #endif
}
</script>
<template><view class="page settings"><view @tap="uni.navigateTo({url:'/pages/settings/notification'})"><image src="/static/icons/notification.png"/>通知管理</view><view @tap="uni.navigateTo({url:'/pages/settings/auth-service'})"><image src="/static/icons/authentication.png"/>认证服务</view><view @tap="checkUpdate"><image src="/static/icons/update.png"/>检查更新</view></view></template>
<style scoped lang="scss">.settings{padding:32rpx 30rpx}.settings>view{display:flex;height:76rpx;align-items:center;font-size:36rpx;font-weight:800}.settings image{width:48rpx;height:48rpx;margin-right:24rpx}</style>

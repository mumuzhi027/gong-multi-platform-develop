<script setup>
import { WECHAT_SUBSCRIBE_TEMPLATE_IDS } from '../../config/env'
function enter(){uni.reLaunch({url:'/pages/home/index'})}
function requestNotification(){
  // #ifdef MP-WEIXIN
  if(!WECHAT_SUBSCRIBE_TEMPLATE_IDS.length){
    uni.showModal({title:'开启通知',content:'当前小程序尚未配置订阅消息模板，可稍后在设置中开启。',showCancel:false,success:enter})
    return
  }
  uni.requestSubscribeMessage({tmplIds:WECHAT_SUBSCRIBE_TEMPLATE_IDS,complete:enter})
  // #endif
  // #ifndef MP-WEIXIN
  // #ifdef H5
  uni.showModal({title:'通知服务尚未开通',content:'当前网页版尚未接入后台通知服务，开启后将可以接收课程和考试提醒。',showCancel:false,success:enter})
  return
  // #endif
  enter()
  // #endif
}
</script>
<template><view class="page notice-request"><view class="notice-content"><image src="/static/icons/notification_logo.svg" mode="aspectFit"/><text class="notice-title">开启通知</text><text class="notice-desc">接收课程、考试的通知及更多内容。</text><button class="enable" @tap="requestNotification">开启通知</button><button class="later" @tap="enter">以后再说</button><text class="notice-foot">在"设置"中随时管理通知类别。</text></view></view></template>
<style scoped lang="scss">
.notice-request{display:flex;padding:var(--status-bar-height) 0 var(--safe-area-bottom);align-items:center;justify-content:center}
.notice-content{display:flex;width:100%;padding:24rpx 16rpx;flex-direction:column;align-items:center;color:var(--text)}
.notice-content image{width:600rpx;max-width:100%;height:240rpx;flex-shrink:0}
.notice-title{margin-top:100rpx;font-size:40rpx;font-weight:800;line-height:56rpx}
.notice-desc{margin-top:40rpx;font-size:28rpx;line-height:40rpx;text-align:center}
.enable,.later{min-height:0;margin-right:0;margin-left:0;padding:20rpx 60rpx;border-radius:100rpx;font-size:28rpx;line-height:40rpx}
.enable{margin-top:40rpx;color:#fff;background:#000}
.later{margin-top:20rpx;color:var(--text);background:transparent}
.notice-foot{margin-top:200rpx;font-size:24rpx;line-height:32rpx;text-align:center}
</style>

<script setup>
import { APP_VERSION } from '../../config/env'
const menus = [
  { icon:'privacy_policy',title:'隐私政策',desc:'查看我们如何收集、使用、存储和保护您的个人信息',type:'privacyPolicy' },
  { icon:'permissions',title:'权限申请与使用情况说明',desc:'查看我们申请的权限及其使用情况',type:'permissionsRequest' },
  { icon:'user_clause',title:'用户协议',desc:'使用拱拱之前，请务必仔细阅读并同意我们的用户协议',type:'userClause' },
]
function open(item){uni.navigateTo({url:`/pages/clause/index?type=${item.type}&title=${encodeURIComponent(item.title)}`})}
</script>
<template><view class="page about-page"><view class="logo-glow"><image src="/static/icons/gonggong_logo.png" mode="aspectFit"/></view><text class="app-name">拱拱</text><text class="app-version">版本: {{ APP_VERSION }}</text><view class="about-spacer"/><view class="about-menu"><view v-for="item in menus" :key="item.type" class="menu-item" @tap="open(item)"><view class="menu-title"><image :src="`/static/icons/${item.icon}.png`" mode="aspectFit"/><text>{{ item.title }}</text></view><text class="menu-desc">{{ item.desc }}</text></view></view></view></template>
<style scoped lang="scss">.about-page{display:flex;padding-top:40rpx;flex-direction:column;align-items:center}.logo-glow{display:flex;width:320rpx;height:320rpx;align-items:center;justify-content:center;border-radius:50%;background:radial-gradient(circle,#ffd2dc 0%,rgba(255,210,220,.12) 66%,transparent 72%)}.logo-glow image{width:320rpx;height:320rpx}.app-name{font-size:50rpx;font-weight:800}.app-version{font-size:24rpx}.about-spacer{height:320rpx}.about-menu{width:calc(100% - 40rpx);padding:16rpx 40rpx;border-radius:24rpx;background:var(--surface)}.menu-item{padding:20rpx 0;border-bottom:4rpx solid var(--background)}.menu-item:last-child{border:0}.menu-title{display:flex;align-items:center;gap:16rpx}.menu-title image{width:40rpx;height:40rpx}.menu-title text{font-size:34rpx;font-weight:800}.menu-desc{display:block;margin-top:16rpx;font-size:24rpx;line-height:1.4}</style>

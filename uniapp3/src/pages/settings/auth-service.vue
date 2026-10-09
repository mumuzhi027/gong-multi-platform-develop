<script setup>
import AppHeader from '../../components/AppHeader.vue'
import { ref } from 'vue'
const enabled=ref(uni.getStorageSync('gong_reauthentication') !== false)
function change(event){enabled.value=event.detail.value;uni.setStorageSync('gong_reauthentication',enabled.value)}
</script>
<template><AppHeader back title="认证服务"/><view class="page sub-page config-page"><view class="config-row"><view><text>重新认证</text><small>当请求返回未授权状态时，弹出认证模块。</small></view><switch :checked="enabled" color="#85cdfa" @change="change"/></view></view></template>
<style scoped lang="scss">.config-page{padding:30rpx}.config-row{display:flex;min-height:112rpx;padding:12rpx 8rpx;align-items:center;justify-content:space-between}.config-row>view{display:flex;flex-direction:column}.config-row text{font-size:36rpx;font-weight:800}.config-row small{margin-top:8rpx;color:#515151;font-size:24rpx}</style>

<script setup>
import AppHeader from '../../components/AppHeader.vue'
import { ref } from 'vue'
const course=ref(uni.getStorageSync('gong_course_reminder') !== false)
const exam=ref(uni.getStorageSync('gong_exam_reminder') !== false)
function change(name,event){ const value=event.detail.value; if(name==='course') course.value=value; else exam.value=value; uni.setStorageSync(name==='course'?'gong_course_reminder':'gong_exam_reminder',value) }
</script>
<template><AppHeader back title="通知管理"/><view class="page sub-page config-page"><view class="config-row"><view><text>上课通知</text><small>上课前10分钟发送上课提醒</small></view><switch :checked="course" color="#85cdfa" @change="change('course',$event)"/></view><view class="config-row"><view><text>考试通知</text><small>考试前7天发送考试提醒</small></view><switch :checked="exam" color="#85cdfa" @change="change('exam',$event)"/></view><!-- #ifdef H5 -->
<view class="notification-note">当前网页版仅保存提醒偏好，尚未接入后台通知服务。</view>
<!-- #endif -->
</view></template>
<style scoped>.notification-note{margin-top:24px;color:var(--muted);font-size:14px;line-height:1.6}</style>
<style scoped lang="scss">.config-page{padding:30rpx}.config-row{display:flex;min-height:112rpx;padding:12rpx 8rpx;align-items:center;justify-content:space-between}.config-row>view{display:flex;flex-direction:column}.config-row text{font-size:36rpx;font-weight:800}.config-row small{margin-top:8rpx;color:#515151;font-size:24rpx}</style>

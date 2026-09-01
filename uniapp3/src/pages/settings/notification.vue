<script setup>
import { ref } from 'vue'
const course=ref(uni.getStorageSync('gong_course_reminder') !== false)
const exam=ref(uni.getStorageSync('gong_exam_reminder') !== false)
function change(name,event){ const value=event.detail.value; if(name==='course') course.value=value; else exam.value=value; uni.setStorageSync(name==='course'?'gong_course_reminder':'gong_exam_reminder',value) }
</script>
<template><view class="page config-page"><view class="config-row"><view><text>上课通知</text><small>上课前10分钟发送上课提醒</small></view><switch :checked="course" color="#85cdfa" @change="change('course',$event)"/></view><view class="config-row"><view><text>考试通知</text><small>考试前7天发送考试提醒</small></view><switch :checked="exam" color="#85cdfa" @change="change('exam',$event)"/></view></view></template>
<style scoped lang="scss">.config-page{padding:30rpx}.config-row{display:flex;min-height:112rpx;padding:12rpx 8rpx;align-items:center;justify-content:space-between}.config-row>view{display:flex;flex-direction:column}.config-row text{font-size:36rpx;font-weight:800}.config-row small{margin-top:8rpx;color:#515151;font-size:24rpx}</style>

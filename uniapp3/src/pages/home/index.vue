<script setup>
import { computed, ref } from 'vue'
import { onLoad, onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import AppNav from '../../components/AppNav.vue'
import SideDrawer from '../../components/SideDrawer.vue'
import CourseSheet from '../../components/CourseSheet.vue'
import { useAuthStore } from '../../stores/auth'
import { useCampusStore } from '../../stores/campus'
import { calculateWeek, formatDate, weekdayName } from '../../utils/date'
import { courseState, courseTimes, stateColor, todayCourses } from '../../utils/course'
import { activityImageUrl, isPastActivity } from '../../utils/activity'

const auth = useAuthStore()
const campus = useCampusStore()
const drawerVisible = ref(false)
const selectedCourse = ref(null)
const now = ref(new Date())
const week = computed(() => calculateWeek(campus.calendar, now.value))
const courses = computed(() => todayCourses(campus.courseMap, week.value, now.value))
const finished = computed(() => courses.value.filter((item) => courseState(item, now.value) === 'finished').length)
const remaining = computed(() => Math.max(0, courses.value.length - finished.value))
const progress = computed(() => courses.value.length ? finished.value / courses.value.length : 1)
const recentActivities = computed(() => campus.activities.filter((item) => !isPastActivity(item)).slice(0, 3))

function parseDate(value) {
  if (!value) return null
  const result = new Date(String(value).replace(' ', 'T'))
  return Number.isNaN(result.getTime()) ? null : result
}

const exams = computed(() => [...(campus.exams || [])].sort((a, b) => {
  const aEnd = parseDate(a.end_time)?.getTime() || Infinity
  const bEnd = parseDate(b.end_time)?.getTime() || Infinity
  const aDone = aEnd <= now.value.getTime() ? 1 : 0
  const bDone = bEnd <= now.value.getTime() ? 1 : 0
  return aDone - bDone || (parseDate(a.start_time)?.getTime() || Infinity) - (parseDate(b.start_time)?.getTime() || Infinity)
}))

onLoad(() => {
  if (!auth.authenticated) return uni.reLaunch({ url: '/pages/login/index' })
  refresh()
})
onShow(() => { now.value = new Date() })
onPullDownRefresh(async () => { await refresh(true); uni.stopPullDownRefresh() })

async function refresh(showToast = false) {
  if (auth.offline) return showToast && uni.showToast({ title: '当前为离线模式', icon: 'none' })
  try { await Promise.all([campus.loadHome(), campus.loadActivities(), auth.loadProfile().catch(() => null)]) }
  catch (error) { if (showToast && !error.authExpired) uni.showToast({ title: error.message, icon: 'none' }) }
}

function examInfo(exam) {
  const start = parseDate(exam.start_time)
  const end = parseDate(exam.end_time)
  let countdown = ''
  if (start) {
    const today = new Date(now.value.getFullYear(), now.value.getMonth(), now.value.getDate())
    const examDay = new Date(start.getFullYear(), start.getMonth(), start.getDate())
    const days = Math.round((examDay - today) / 86400000)
    countdown = end && now.value >= end || days < 0 ? '已结束' : days === 0 ? '今天' : String(days)
  }
  const pad = (v) => String(v).padStart(2, '0')
  return {
    date: start ? `${start.getMonth() + 1}月${start.getDate()}日` : '',
    countdown,
    time: start && end ? `${pad(start.getHours())}:${pad(start.getMinutes())}-${pad(end.getHours())}:${pad(end.getMinutes())}` : exam.start_time || '无时间安排',
  }
}

function openActivity(activity) {
  uni.navigateTo({ url: `/pages/activity/detail?id=${encodeURIComponent(activity.id)}` })
}
</script>

<template>
  <view class="page page-with-nav home-page">
    <view class="safe-head">
      <view class="menu-button" @tap="drawerVisible = true"><image src="/static/icons/menu_icon.png" mode="aspectFit" /></view>
      <view class="main-info">
        <view class="info-column">
          <view class="info-top">
            <view class="weekday">{{ weekdayName(now) }}</view>
            <view class="week-text">第<text>{{ week }}</text>周</view>
          </view>
          <view class="date">{{ formatDate(now) }}</view>
        </view>
      </view>
    </view>

    <scroll-view class="body" scroll-y enhanced :show-scrollbar="false">
      <view class="home-section">
        <view class="course-head">
          <text class="title">今日课程</text>
          <view class="progress-ring" :style="{ '--progress': `${progress * 360}deg` }"><view>{{ courses.length }}</view></view>
          <view class="remain">还剩 <text>{{ remaining }}</text> 节课</view>
          <view class="spacer" />
          <view class="icon-button" :class="{ spinning: campus.loading.home }" @tap="refresh(true)"><image src="/static/icons/baseline_refresh_24.png" /></view>
          <view class="icon-button" @tap="uni.navigateTo({ url: '/pages/course/index' })"><image src="/static/icons/course.png" /></view>
        </view>
        <view class="course-area">
          <view v-if="campus.errors.home && !courses.length" class="overlay-retry" @tap="refresh(true)">
            <image src="/static/icons/baseline_refresh_24.png" /><text>重新加载</text>
          </view>
          <view v-else-if="!courses.length" class="empty-action">
            <text>今日无课</text>
            <button @tap="uni.navigateTo({ url: '/pages/course/index' })"><image src="/static/icons/course.png" />课程表</button>
          </view>
          <scroll-view v-else class="today-list" scroll-y>
            <view v-for="course in courses" :key="`${course.name}-${course.start_time}`" class="today-row" @tap="selectedCourse = course">
              <view class="time">{{ courseTimes(course, now)[0] }}<text>-</text>{{ courseTimes(course, now)[1] }}</view>
              <view class="timeline"><view class="line" /><view class="dot" :style="{ background: stateColor(courseState(course, now)) }" /></view>
              <view class="course-card">
                <view class="state-bar" :style="{ background: stateColor(courseState(course, now)) }" />
                <view><text class="course-name">{{ course.name }}</text><text class="course-room">{{ course.classroom }}</text></view>
              </view>
            </view>
          </scroll-view>
        </view>
      </view>

      <view class="divider" />
      <view class="home-section activity-overview">
        <view class="activity-overview-head">
          <text class="title">校园活动</text>
          <view class="spacer" />
          <text class="more" @tap="uni.redirectTo({ url: '/pages/activity/index' })">查看更多  ›</text>
        </view>
        <swiper v-if="recentActivities.length" class="home-poster-swiper" circular autoplay :interval="4200" indicator-dots indicator-color="rgba(255,255,255,.55)" indicator-active-color="#85cdfa">
          <swiper-item v-for="activity in recentActivities" :key="activity.id">
            <view class="home-poster" :class="{ 'with-image': activity.posterUrl }" @tap="openActivity(activity)">
              <image v-if="activity.posterUrl" :src="activityImageUrl(activity.posterUrl)" mode="aspectFill" lazy-load />
              <view v-if="activity.posterUrl" class="home-poster-shade" />
              <view class="home-poster-copy">
                <text class="home-poster-kind">{{ activity.title }}</text>
                <text class="home-poster-title">{{ activity.posterTitle || activity.title }}</text>
                <text v-if="activity.posterSubtitle || activity.organizer" class="home-poster-subtitle">{{ activity.posterSubtitle || activity.organizer }}</text>
              </view>
              <text class="home-poster-date">{{ activity.startDate }}</text>
            </view>
          </swiper-item>
        </swiper>
        <view v-else class="activity-empty" @tap="uni.redirectTo({ url: '/pages/activity/index' })">暂无近期活动</view>
      </view>

      <view class="divider" />
      <view class="home-section">
        <view class="exam-head"><text class="title">考试安排</text><view class="spacer"/><view class="icon-button" :class="{ spinning: campus.loading.home }" @tap="refresh(true)"><image src="/static/icons/baseline_refresh_24.png" /></view></view>
        <view class="exam-area">
          <view v-if="!exams.length" class="empty-action"><text>暂无考试</text><button @tap="uni.redirectTo({ url: '/pages/academic/index' })"><image src="/static/icons/grade.png" />成绩单</button></view>
          <scroll-view v-else scroll-y class="exam-scroll">
            <view v-for="exam in exams" :key="`${exam.name}-${exam.start_time}`" class="exam-card">
              <view class="exam-top"><text class="exam-name">{{ exam.name }}</text><text class="exam-date">{{ examInfo(exam).date }}</text><view class="countdown"><text>{{ examInfo(exam).countdown }}</text><small v-if="/^\d+$/.test(examInfo(exam).countdown)">天</small></view></view>
              <view class="exam-bottom"><view><image src="/static/icons/baseline_location_on_24.png" />{{ exam.location || '无地点安排' }}</view><view><image src="/static/icons/baseline_access_time_filled_24.png" />{{ examInfo(exam).time }}</view></view>
            </view>
          </scroll-view>
        </view>
      </view>
    </scroll-view>
    <AppNav current="home" />
    <SideDrawer :visible="drawerVisible" @close="drawerVisible = false" />
    <CourseSheet :course="selectedCourse" @close="selectedCourse = null" />
  </view>
</template>

<style scoped lang="scss">
.home-page{height:100vh;overflow:hidden}.safe-head{display:flex;padding:calc(var(--status-bar-height) + 8rpx) 20rpx 10rpx;align-items:center}.menu-button{display:flex;width:66rpx;height:66rpx;align-items:center;justify-content:center}.menu-button image{width:56rpx;height:56rpx}.main-info{display:flex;height:152rpx;margin-left:2rpx;padding:28rpx;flex:1;border-radius:20rpx;background:var(--primary)}.info-column{display:flex;height:96rpx;margin-left:28rpx;flex-direction:column;justify-content:space-between}.info-top{display:flex;height:58rpx;align-items:center}.weekday{font-size:42rpx;font-weight:800}.week-text{margin-left:58rpx;font-size:27rpx}.week-text text{margin:0 10rpx;font-size:48rpx;font-weight:800}.date{font-size:30rpx}.body{height:calc(100vh - var(--status-bar-height) - 296rpx);padding:0 20rpx}.home-section{display:flex;min-height:0;flex-direction:column}.home-section:first-child{height:650rpx}.course-head,.exam-head{display:flex;min-height:100rpx;align-items:center}.title{font-size:40rpx;font-weight:800}.progress-ring{display:flex;width:84rpx;height:84rpx;margin:0 32rpx;align-items:center;justify-content:center;border-radius:50%;background:conic-gradient(var(--primary) var(--progress),var(--surface) 0)}.progress-ring view{display:flex;width:44rpx;height:44rpx;align-items:center;justify-content:center;border-radius:50%;background:var(--background);font-size:46rpx;font-weight:700}.remain{color:#515151;font-size:27rpx}.remain text{margin:0 8rpx;color:#171717;font-size:34rpx;font-weight:800}.spacer{flex:1}.icon-button{display:flex;width:76rpx;height:76rpx;align-items:center;justify-content:center}.icon-button image{width:40rpx;height:40rpx}.spinning image{animation:spin 1s linear infinite}.course-area,.exam-area{position:relative;min-height:0;flex:1}.exam-area{height:500rpx;overflow:hidden;border-radius:10rpx;background:var(--surface)}.today-list,.exam-scroll{height:100%}.today-row{display:flex;height:160rpx;align-items:center}.time{display:flex;width:200rpx;justify-content:space-evenly;font-size:27rpx}.timeline{position:relative;width:24rpx;height:100%}.line{position:absolute;top:0;bottom:0;left:11rpx;width:2rpx;background:var(--surface)}.dot{position:absolute;top:72rpx;left:4rpx;width:16rpx;height:16rpx;border-radius:50%}.course-card{display:flex;height:140rpx;margin-left:10rpx;padding:20rpx 30rpx;flex:1;align-items:center;border-radius:30rpx;background:#fff}.state-bar{width:8rpx;height:50%;margin-right:20rpx;border-radius:10rpx}.course-card>view:last-child{display:flex;min-width:0;flex-direction:column}.course-name{overflow:hidden;font-size:30rpx;font-weight:600;letter-spacing:2rpx;text-overflow:ellipsis;white-space:nowrap}.course-room{margin-top:10rpx;color:#515151;font-size:27rpx}.divider{height:2rpx;background:var(--surface)}.exam-head{min-height:100rpx}.exam-card{margin:10rpx;padding:20rpx 30rpx;border-radius:30rpx;background:#fff}.exam-top{display:flex;align-items:center}.exam-name{width:42%;overflow:hidden;font-size:34rpx;font-weight:600;text-overflow:ellipsis;white-space:nowrap}.exam-date{width:22%;color:#515151;font-size:24rpx;font-weight:600}.countdown{display:flex;flex:1;align-items:center;justify-content:flex-end}.countdown text{color:var(--primary-strong);font-size:38rpx;font-weight:600}.countdown small{margin-left:18rpx;font-size:26rpx}.exam-bottom{display:flex;margin-top:12rpx}.exam-bottom view{display:flex;min-width:0;flex:1;align-items:center;overflow:hidden;color:#515151;font-size:23rpx;white-space:nowrap}.exam-bottom image{width:32rpx;height:32rpx;margin-right:14rpx}.activity-overview{height:430rpx}.activity-overview-head{display:flex;height:100rpx;align-items:center}.more{padding:18rpx 4rpx;color:var(--muted);font-size:24rpx}.home-poster-swiper{height:320rpx}.home-poster{position:relative;height:294rpx;overflow:hidden;border-radius:28rpx;background:var(--card)}.home-poster>image,.home-poster-shade{position:absolute;width:100%;height:100%}.home-poster-shade{background:linear-gradient(180deg,rgba(0,0,0,.02),rgba(0,0,0,.56))}.home-poster-copy{position:absolute;right:30rpx;bottom:26rpx;left:30rpx;display:flex;flex-direction:column}.home-poster-kind{margin-bottom:20rpx;color:rgba(19,46,61,.72);font-size:22rpx;font-weight:650}.with-image .home-poster-kind{color:rgba(255,255,255,.86)}.home-poster-title{color:#132e3d;font-size:40rpx;font-weight:850;line-height:1.12}.with-image .home-poster-title{color:#fff}.home-poster-subtitle{margin-top:5rpx;color:var(--muted);font-size:23rpx}.with-image .home-poster-subtitle{color:rgba(255,255,255,.9)}.home-poster-date{position:absolute;top:22rpx;right:24rpx;color:rgba(19,46,61,.68);font-size:20rpx}.with-image .home-poster-date{color:rgba(255,255,255,.88)}.activity-empty{display:flex;height:294rpx;align-items:center;justify-content:center;border-radius:28rpx;color:var(--muted);background:var(--card);font-size:28rpx}.empty-action,.overlay-retry{display:flex;height:100%;flex-direction:column;align-items:center;justify-content:center;font-size:40rpx;font-weight:800}.empty-action button{display:flex;width:auto;height:76rpx;min-height:0;margin-top:18rpx;padding:0 48rpx;align-items:center;border-radius:40rpx;color:#000;background:var(--primary);font-size:27rpx;font-weight:500;line-height:76rpx}.empty-action button image{width:38rpx;height:38rpx;margin-right:14rpx}.overlay-retry image{width:80rpx;height:80rpx;margin-bottom:12rpx}@keyframes spin{to{transform:rotate(360deg)}}
</style>

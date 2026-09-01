<script setup>
import { computed, nextTick, ref } from 'vue'
import { onLoad, onPullDownRefresh } from '@dcloudio/uni-app'
import AppHeader from '../../components/AppHeader.vue'
import StateView from '../../components/StateView.vue'
import CourseSheet from '../../components/CourseSheet.vue'
import AuthorizationDialog from '../../components/AuthorizationDialog.vue'
import { useAuthStore } from '../../stores/auth'
import { useCampusStore } from '../../stores/campus'
import { calculateWeek, weekDates } from '../../utils/date'
import { courseColor, courseMapSize, coursesForWeek, DAYS, DAY_LABELS, periodStarts } from '../../utils/course'

const auth = useAuthStore()
const campus = useCampusStore()
const selectedCourse = ref(null)
const currentWeek = ref(1)
const showWeeks = ref(false)
const swiperCurrent = ref(1)
const swiperDuration = ref(280)
const resettingSwiper = ref(false)
const weeks = computed(() => Number(campus.calendar?.weeks || 20))
const courseCount = computed(() => courseMapSize(campus.courseMap))
const realWeek = computed(() => calculateWeek(campus.calendar))
const weekSlides = computed(() => [-1, 0, 1].map((offset, slot) => {
  const week = Math.min(weeks.value, Math.max(1, currentWeek.value + offset))
  const dates = weekDates(campus.calendar, week)
  return {
    slot,
    week,
    dates,
    month: dates[0]?.getMonth() + 1 || '',
    starts: periodStarts(dates[0] || new Date()),
    courses: coursesForWeek(campus.courseMap, week),
  }
}))

onLoad(async () => {
  if (!auth.authenticated) return uni.reLaunch({ url: '/pages/login/index' })
  if ((!courseCount.value || !campus.calendar) && !auth.offline) await campus.loadHome().catch(() => null)
  currentWeek.value = realWeek.value || 1
})
onPullDownRefresh(async () => { if (!auth.offline) await campus.loadHome().catch(() => null); uni.stopPullDownRefresh() })

function endTime(start) {
  const [hour, minute] = start.split(':').map(Number)
  const total = hour * 60 + minute + 45
  return `${String(Math.floor(total / 60)).padStart(2, '0')}:${String(total % 60).padStart(2, '0')}`
}
function dateText(date) { return `${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}` }
function isToday(date) { const now = new Date(); return date.toDateString() === now.toDateString() }
function offsetFor(period) { return (period - 1) * 86 + (period >= 5 ? 20 : 0) + (period >= 9 ? 20 : 0) }
function blockStyle(course) {
  const duration = Math.max(1, Number(course.duration || 1))
  const length = Math.max(course.name?.length || 0, course.classroom?.length || 0)
  const font = duration >= 3 ? 23 : duration === 2 ? 21 : length >= 11 ? 18 : length >= 8 ? 19 : 21
  return { top: `${offsetFor(Number(course.start_time || 1)) + 6}rpx`, height: `${duration * 86 - 12}rpx`, backgroundColor: courseColor(course.name), '--font': `${font}rpx` }
}
function selectWeek(value) { currentWeek.value = value; showWeeks.value = false }
async function onSwiperChange(event) {
  const page = Number(event.detail.current)
  if (resettingSwiper.value || page === 1) return
  const target = Math.min(weeks.value, Math.max(1, currentWeek.value + (page > 1 ? 1 : -1)))
  if (target === currentWeek.value) {
    swiperDuration.value = 180
    swiperCurrent.value = 1
    return
  }
  resettingSwiper.value = true
  currentWeek.value = target
  swiperDuration.value = 0
  await nextTick()
  swiperCurrent.value = 1
  setTimeout(() => {
    swiperDuration.value = 280
    resettingSwiper.value = false
  }, 40)
}
</script>

<template>
  <view class="page course-page">
    <AppHeader back />
    <view class="week-title" @tap="showWeeks = !showWeeks"><text>第{{ currentWeek }}周</text><small :class="{ current: currentWeek === realWeek }">{{ currentWeek === realWeek ? '本周' : '非本周' }}</small></view>
    <scroll-view v-if="showWeeks" class="week-picker" scroll-x :scroll-into-view="`week-${currentWeek}`">
      <view class="week-picker-inner"><view v-for="value in weeks" :id="`week-${value}`" :key="value" :class="['week-chip',{active:value===currentWeek}]" @tap="selectWeek(value)">{{ value }}</view></view>
    </scroll-view>
    <StateView :loading="campus.loading.home && !courseCount" :error="campus.errors.home" :empty="!courseCount" empty-text="暂无课程数据" @retry="campus.loadHome">
      <swiper class="week-swiper" :current="swiperCurrent" :duration="swiperDuration" @change="onSwiperChange">
        <swiper-item v-for="slide in weekSlides" :key="slide.slot">
          <view class="table-head">
            <view class="month-head">{{ slide.month }}</view>
            <view v-for="(day,index) in DAYS" :key="day" :class="['day-head',{today:isToday(slide.dates[index])}]"><text>{{ DAY_LABELS[index] }}</text><small>{{ dateText(slide.dates[index]) }}</small></view>
          </view>
          <scroll-view class="table-scroll" scroll-y>
            <view class="table-body">
              <view class="time-column">
                <template v-for="(start,index) in slide.starts" :key="start">
                  <view v-if="index===4 || index===8" class="section-gap" />
                  <view class="time-cell"><text>{{ start }}</text><small>{{ endTime(start) }}</small></view>
                </template>
              </view>
              <view v-for="(day,index) in DAYS" :key="day" :class="['day-column',{today:isToday(slide.dates[index])}]">
                <template v-for="period in 11" :key="period"><view v-if="period===5 || period===9" class="section-gap"/><view class="grid-cell" /></template>
                <view v-for="course in slide.courses[day]" :key="`${course.name}-${course.start_time}`" class="course-block" :style="blockStyle(course)" @tap="selectedCourse=course"><text>{{ course.name }}</text><small v-if="course.classroom">{{ course.classroom }}</small></view>
              </view>
            </view>
          </scroll-view>
        </swiper-item>
      </swiper>
    </StateView>
    <CourseSheet :course="selectedCourse" @close="selectedCourse=null" />
    <AuthorizationDialog />
  </view>
</template>

<style scoped lang="scss">
.course-page{height:100vh;overflow:hidden}.week-title{position:absolute;z-index:4;top:calc(var(--status-bar-height) + 18rpx);right:160rpx;left:160rpx;display:flex;flex-direction:column;align-items:center}.week-title text{font-size:30rpx;font-weight:700}.week-title small{margin-top:3rpx;padding:3rpx 16rpx;border-radius:20rpx;color:#515151;background:#e3e3e3;font-size:18rpx}.week-title small.current{color:#255a76;background:rgba(133,205,250,.3)}.week-picker{position:absolute;z-index:10;top:calc(var(--status-bar-height) + 94rpx);width:100%;height:78rpx;background:var(--background);white-space:nowrap}.week-picker-inner{display:inline-flex;padding:10rpx 24rpx;gap:12rpx}.week-chip{display:flex;width:56rpx;height:56rpx;align-items:center;justify-content:center;border-radius:50%;font-size:24rpx}.week-chip.active{background:var(--primary);font-weight:800}.week-swiper{height:calc(100vh - var(--status-bar-height) - 92rpx)}.table-head{display:flex;height:94rpx;padding:0 8rpx}.month-head{display:flex;width:84rpx;align-items:center;justify-content:center;font-size:28rpx;font-weight:700}.day-head{display:flex;min-width:0;margin:3rpx 1rpx;flex:1;flex-direction:column;align-items:center;justify-content:center;border-radius:28rpx}.day-head.today,.day-column.today{background:rgba(133,205,250,.18)}.day-head text{font-size:25rpx;font-weight:600}.day-head small{margin-top:5rpx;color:#515151;font-size:18rpx}.day-head.today text,.day-head.today small{color:#2387ba}.table-scroll{height:calc(100vh - var(--status-bar-height) - 194rpx)}.table-body{display:flex;min-height:986rpx;padding:0 8rpx 20rpx}.time-column{width:84rpx}.time-cell{display:flex;height:86rpx;flex-direction:column;align-items:center;justify-content:center}.time-cell text,.time-cell small{font-size:17rpx;line-height:1.25}.time-cell small{color:#777}.section-gap{height:20rpx;border-radius:10rpx;background:var(--primary)}.day-column{position:relative;min-width:0;margin:0 1rpx;flex:1;border-radius:28rpx}.day-column .section-gap{background:transparent}.grid-cell{height:86rpx}.course-block{position:absolute;right:4rpx;left:4rpx;display:flex;padding:7rpx 5rpx;overflow:hidden;flex-direction:column;align-items:center;justify-content:center;border-radius:8rpx;text-align:center}.course-block text{display:-webkit-box;overflow:hidden;font-size:var(--font);font-weight:600;line-height:1.12;-webkit-box-orient:vertical;-webkit-line-clamp:4}.course-block small{display:-webkit-box;margin-top:7rpx;overflow:hidden;font-size:calc(var(--font) - 2rpx);line-height:1.1;-webkit-box-orient:vertical;-webkit-line-clamp:3}
</style>

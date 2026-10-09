<script setup>
import { computed, nextTick, ref } from 'vue'
import { onLoad, onPullDownRefresh } from '@dcloudio/uni-app'
import AppHeader from '../../components/AppHeader.vue'
import StateView from '../../components/StateView.vue'
import CourseSheet from '../../components/CourseSheet.vue'
import CourseFlowBorder from '../../components/CourseFlowBorder.vue'
import AuthorizationDialog from '../../components/AuthorizationDialog.vue'
import CourseEditor from '../../components/CourseEditor.vue'
import { useAuthStore } from '../../stores/auth'
import { useCampusStore } from '../../stores/campus'
import { calculateWeek, weekDates } from '../../utils/date'
import { storage } from '../../utils/storage'
import { courseColor, courseMapSize, coursesForWeek, DAYS, DAY_LABELS, periodStarts, isStaggeredCourse } from '../../utils/course'

const auth = useAuthStore()
const campus = useCampusStore()
const selectedCourse = ref(null)
const editor = ref(null)
const currentWeek = ref(1)
const showWeeks = ref(false)
const swiperCurrent = ref(1)
const swiperDuration = ref(280)
const resettingSwiper = ref(false)
// 只控制时间轴查看方式，课程时间、详情和提醒仍按教室计算。
const allStaggeredTimeGroups = 0b111111
const timeAxisStorageKey = 'gong_course_time_axis'
const storedTimeGroups = Number(storage.get(timeAxisStorageKey, 0))
const staggeredTimeGroups = ref(Number.isInteger(storedTimeGroups) && storedTimeGroups >= 0 && storedTimeGroups <= allStaggeredTimeGroups ? storedTimeGroups : 0)
const timeGroups = Array.from({ length: 6 }, (_, index) => ({
  index,
  periods: [index * 2, index * 2 + 1].filter((period) => period < 11),
}))
const staggeredStarts = periodStarts(new Date(), true)
const timeMode = computed(() => staggeredTimeGroups.value === 0 ? '常规'
  : staggeredTimeGroups.value === allStaggeredTimeGroups ? '错峰' : '混合')
const nextTimeMode = computed(() => staggeredTimeGroups.value === allStaggeredTimeGroups ? '常规' : '错峰')
const weeks = computed(() => Number(campus.calendar?.weeks || 20))
const courseCount = computed(() => courseMapSize(campus.courseMap))
const calendarValid = computed(() => Number(campus.calendar?.weeks) > 0 && !Number.isNaN(new Date(`${campus.calendar?.start}T00:00:00`).getTime()))
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
function isStaggeredGroup(group) { return (staggeredTimeGroups.value & (1 << group)) !== 0 }
function saveTimeGroups(value) {
  staggeredTimeGroups.value = value & allStaggeredTimeGroups
  storage.set(timeAxisStorageKey, staggeredTimeGroups.value)
}
function toggleTimeGroup(group) { saveTimeGroups(staggeredTimeGroups.value ^ (1 << group)) }
function toggleAllTimes() {
  saveTimeGroups(staggeredTimeGroups.value === allStaggeredTimeGroups ? 0 : allStaggeredTimeGroups)
}
function timeGroupLabel(slide, group) {
  const starts = isStaggeredGroup(group.index) ? staggeredStarts : slide.starts
  const first = group.periods[0]
  const last = group.periods[group.periods.length - 1]
  const periods = first === last ? `${first + 1}` : `${first + 1}–${last + 1}`
  return `第${periods}节，${isStaggeredGroup(group.index) ? '错峰' : '常规'}时段，${starts[first]}至${endTime(starts[last])}，点击切换`
}
function dateText(date) { return `${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}` }
function isToday(date) { const now = new Date(); return date.toDateString() === now.toDateString() }
function offsetFor(period) { return (period - 1) * 86 + (period >= 5 ? 20 : 0) + (period >= 9 ? 20 : 0) }
function blockStyle(course) {
  const duration = Math.max(1, Number(course.duration || 1))
  const length = Math.max(course.name?.length || 0, course.classroom?.length || 0)
  const font = duration >= 3 ? 23 : duration === 2 ? 21 : length >= 11 ? 18 : length >= 8 ? 19 : 21
  // #ifdef H5
  const start = Number(course.start_time || 1)
  const boundary = start <= 4 ? 4 : start <= 8 ? 8 : 11
  const visibleDuration = Math.min(duration, boundary - start + 1)
  const gap = (start >= 5 ? 20 : 0) + (start >= 9 ? 20 : 0)
  return {
    top: `calc((100% - 40rpx) / 11 * ${start - 1} + ${gap + 6}rpx)`,
    height: `calc((100% - 40rpx) / 11 * ${visibleDuration} - 12rpx)`,
    backgroundColor: courseColor(course.name), '--course-fill': courseColor(course.name), '--font': `${font}rpx`,
  }
  // #endif
  return { top: `${offsetFor(Number(course.start_time || 1)) + 6}rpx`, height: `${duration * 86 - 12}rpx`, backgroundColor: courseColor(course.name), '--course-fill': courseColor(course.name), '--font': `${font}rpx` }
}
function openSlot(day, week, start) { editor.value = { day, week, start, course: null } }
function openCourse(course, week) {
  if (course.isCustom) editor.value = { day: course.day, week, start: course.start_time, course }
  else selectedCourse.value = course
}
function saveCourse(value) {
  try { campus.saveCustomCourse(value, editor.value?.course); editor.value = null }
  catch (error) { uni.showToast({ title: error.message, icon: 'none' }) }
}
function deleteCustom(course) { campus.deleteCustomCourse(course); editor.value = null }
function deleteRemote(course) { campus.deleteRemoteCourse(course, currentWeek.value); selectedCourse.value = null }
function restoreWeek() {
  uni.showModal({
    title: `恢复第${currentWeek.value}周课表`,
    content: '将恢复本周被删除的原始课程。用户新增课程会保留；若新增课程与待恢复的原始课程节次重叠，则以原始课程为准。',
    confirmText: '确认恢复', success: ({ confirm }) => {
      if (!confirm) return
      const count = campus.restoreCourses(currentWeek.value)
      uni.showToast({ title: count ? `已恢复第${currentWeek.value}周原课表内容` : `第${currentWeek.value}周没有需要恢复的课程`, icon: 'none' })
    },
  })
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
    <view class="restore-week" @tap="restoreWeek">恢复本周</view>
    <view class="week-title" @tap="showWeeks = !showWeeks"><text>第{{ currentWeek }}周</text><small :class="{ current: currentWeek === realWeek }">{{ currentWeek === realWeek ? '本周' : '非本周' }}</small></view>
    <scroll-view v-if="showWeeks" class="week-picker" scroll-x :scroll-into-view="`week-${currentWeek}`">
      <view class="week-picker-inner"><view v-for="value in weeks" :id="`week-${value}`" :key="value" :class="['week-chip',{active:value===currentWeek}]" @tap="selectWeek(value)">{{ value }}</view></view>
    </scroll-view>
    <StateView :loading="campus.loading.home && !calendarValid" :error="calendarValid ? '' : campus.errors.home" :empty="!calendarValid" empty-text="暂无校历数据，无法显示课表" @retry="campus.loadHome">
      <swiper class="week-swiper" :current="swiperCurrent" :duration="swiperDuration" @change="onSwiperChange">
        <swiper-item v-for="slide in weekSlides" :key="slide.slot">
          <view class="table-head">
            <view class="month-head time-mode-toggle" :class="{ 'has-staggered': staggeredTimeGroups !== 0 }"
              role="button" :aria-label="`${timeMode}时段，点击整列切换为${nextTimeMode}时段`" @tap.stop="toggleAllTimes">
              <text v-for="label in ['常规','错峰','混合']" :key="label" class="time-mode-label" :class="{ active: timeMode === label }" :aria-hidden="timeMode !== label">{{ label }}</text>
              <small>{{ slide.month }}月</small>
            </view>
            <view v-for="(day,index) in DAYS" :key="day" :class="['day-head',{today:isToday(slide.dates[index])}]"><text>{{ DAY_LABELS[index] }}</text><small>{{ dateText(slide.dates[index]) }}</small></view>
          </view>
          <scroll-view class="table-scroll" scroll-y>
            <view class="table-body">
              <view class="time-column">
                <template v-for="group in timeGroups" :key="group.index">
                  <view v-if="group.index===2 || group.index===4" class="section-gap" />
                  <view class="time-pair" :class="{ 'is-staggered': isStaggeredGroup(group.index) }"
                    role="button" :aria-label="timeGroupLabel(slide, group)" @tap.stop="toggleTimeGroup(group.index)">
                    <view v-for="period in group.periods" :key="period" class="time-cell">
                      <view class="time-values regular-values" :aria-hidden="isStaggeredGroup(group.index)"><text>{{ slide.starts[period] }}</text><small>{{ endTime(slide.starts[period]) }}</small></view>
                      <view class="time-values staggered-values" :aria-hidden="!isStaggeredGroup(group.index)"><text>{{ staggeredStarts[period] }}</text><small>{{ endTime(staggeredStarts[period]) }}</small></view>
                    </view>
                  </view>
                </template>
              </view>
              <view v-for="(day,index) in DAYS" :key="day" :class="['day-column',{today:isToday(slide.dates[index])}]">
                <template v-for="period in 11" :key="period"><view v-if="period===5 || period===9" class="section-gap"/><view class="grid-cell" @tap="openSlot(day, slide.week, period)">＋</view></template>
                <view v-for="course in slide.courses[day]" :key="`${course.name}-${course.start_time}`" class="course-block" :style="blockStyle(course)" @tap="openCourse(course, slide.week)">
                  <template v-if="isStaggeredCourse(course)"><CourseFlowBorder :diameter="Math.max(240, Number(course.duration || 1) * 130)" /><view class="flow-surface" /></template>
                  <text>{{ course.name }}</text><small v-if="course.classroom">{{ course.classroom }}</small>
                </view>
              </view>
            </view>
          </scroll-view>
        </swiper-item>
      </swiper>
    </StateView>
    <CourseSheet :course="selectedCourse" removable @delete="deleteRemote" @close="selectedCourse=null" />
    <CourseEditor v-if="editor" :key="`${editor.day}-${editor.week}-${editor.start}`" :editor="editor" @save="saveCourse" @delete="deleteCustom" @close="editor=null"/>
    <AuthorizationDialog />
  </view>
</template>

<style scoped lang="scss">
.restore-week{position:absolute;z-index:5;top:calc(var(--status-bar-height) + 26rpx);right:24rpx;color:#24617e;font-size:28rpx}
.grid-cell{display:flex;align-items:center;justify-content:center;color:rgba(81,81,81,.28);font-size:24rpx}
/* #ifdef H5 */
.week-swiper{height:calc(var(--app-height) - var(--status-bar-height) - 92rpx - var(--safe-area-bottom))!important}
.table-scroll{height:calc(var(--app-height) - var(--status-bar-height) - 186rpx - var(--safe-area-bottom))!important}
.table-scroll :deep(.uni-scroll-view-content){height:100%}
.table-body{height:100%;min-height:0!important;padding-bottom:0!important}
.time-column,.day-column{display:flex;height:100%;flex-direction:column}
.time-pair{min-height:0;flex:2;display:flex;flex-direction:column}
.time-pair:last-child{flex:1}
.time-cell{height:auto!important;min-height:0;flex:1}
.grid-cell{height:auto!important;min-height:0;flex:1}
.section-gap{flex:0 0 20rpx}
/* #endif */
.course-page {
  --axis-active-background: #cee9fc;
  --axis-active-text: #135379;
}
.month-head.time-mode-toggle {
  position: relative;
  flex-shrink: 0;
  border-radius: 8rpx;
  background: var(--surface);
  transition: background-color 260ms ease, color 260ms ease;
}
.time-mode-label {
  position: absolute;
  top: 16rpx;
  right: 0;
  left: 0;
  opacity: 0;
  text-align: center;
  font-size: 22rpx;
  line-height: 1.2;
  transition: opacity 240ms ease;
}
.time-mode-label.active { opacity: 1; }
.time-mode-toggle > small {
  position: absolute;
  right: 0;
  bottom: 13rpx;
  left: 0;
  text-align: center;
  font-size: 18rpx;
  line-height: 1.2;
}
.time-pair {
  overflow: hidden;
  border-radius: 8rpx;
  background: var(--surface);
  transition: background-color 260ms ease;
}
.time-pair .time-cell { position: relative; }
.time-values {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: space-evenly;
  pointer-events: none;
  transition: opacity 240ms ease, transform 240ms ease;
}
.staggered-values { opacity: 0; transform: translateY(4rpx); }
.time-pair.is-staggered, .time-mode-toggle.has-staggered {
  background: var(--axis-active-background);
  color: var(--axis-active-text);
}
.time-pair.is-staggered .time-cell small { color: var(--axis-active-text); }
.is-staggered .regular-values { opacity: 0; transform: translateY(-4rpx); }
.is-staggered .staggered-values { opacity: 1; transform: translateY(0); }
@media (prefers-color-scheme: dark) {
  .course-page {
    --axis-active-background: #163e58;
    --axis-active-text: #b6e4ff;
  }
}
@media (prefers-reduced-motion: reduce) {
  .time-values, .time-mode-label, .time-pair, .month-head.time-mode-toggle { transition: none; }
}
.flow-surface{position:absolute;inset:6rpx;border-radius:inherit;background:var(--course-fill,var(--background));box-shadow:0 0 0 1rpx var(--course-flow-separator,#fff);pointer-events:none}.course-block>text,.course-block>small{position:relative;z-index:1}
.course-page{height:var(--app-height, 100vh);overflow:hidden}.week-title{position:absolute;z-index:4;top:calc(var(--status-bar-height) + 18rpx);right:160rpx;left:160rpx;display:flex;flex-direction:column;align-items:center}.week-title text{font-size:30rpx;font-weight:700}.week-title small{margin-top:3rpx;padding:3rpx 16rpx;border-radius:20rpx;color:#515151;background:#e3e3e3;font-size:18rpx}.week-title small.current{color:#255a76;background:rgba(133,205,250,.3)}.week-picker{position:absolute;z-index:10;top:calc(var(--status-bar-height) + 94rpx);width:100%;height:78rpx;background:var(--background);white-space:nowrap}.week-picker-inner{display:inline-flex;padding:10rpx 24rpx;gap:12rpx}.week-chip{display:flex;width:56rpx;height:56rpx;align-items:center;justify-content:center;border-radius:50%;font-size:24rpx}.week-chip.active{background:var(--primary);font-weight:800}.week-swiper{height:calc(var(--app-height, 100vh) - var(--status-bar-height) - 92rpx)}.table-head{display:flex;height:94rpx;padding:0 8rpx}.month-head{display:flex;width:84rpx;align-items:center;justify-content:center;font-size:28rpx;font-weight:700}.day-head{display:flex;min-width:0;margin:3rpx 1rpx;flex:1;flex-direction:column;align-items:center;justify-content:center;border-radius:28rpx}.day-head.today,.day-column.today{background:rgba(133,205,250,.18)}.day-head text{font-size:25rpx;font-weight:600}.day-head small{margin-top:5rpx;color:#515151;font-size:18rpx}.day-head.today text,.day-head.today small{color:#2387ba}.table-scroll{height:calc(var(--app-height, 100vh) - var(--status-bar-height) - 194rpx)}.table-body{display:flex;min-height:986rpx;padding:0 8rpx 20rpx}.time-column{width:84rpx}.time-cell{display:flex;height:86rpx;flex-direction:column;align-items:center;justify-content:center}.time-cell text,.time-cell small{font-size:17rpx;line-height:1.25}.time-cell small{color:#777}.section-gap{height:20rpx;border-radius:10rpx;background:var(--primary)}.day-column{position:relative;min-width:0;margin:0 1rpx;flex:1;border-radius:28rpx}.day-column .section-gap{background:transparent}.grid-cell{height:86rpx}.course-block{position:absolute;right:4rpx;left:4rpx;display:flex;padding:7rpx 5rpx;overflow:hidden;flex-direction:column;align-items:center;justify-content:center;border-radius:8rpx;text-align:center}.course-block text{display:-webkit-box;overflow:hidden;font-size:var(--font);font-weight:600;line-height:1.12;-webkit-box-orient:vertical;-webkit-line-clamp:4}.course-block small{display:-webkit-box;margin-top:7rpx;overflow:hidden;font-size:calc(var(--font) - 2rpx);line-height:1.1;-webkit-box-orient:vertical;-webkit-line-clamp:3}
</style>

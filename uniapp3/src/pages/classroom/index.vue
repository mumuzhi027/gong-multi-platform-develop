<script setup>
import { computed, ref, watch } from 'vue'
import { onLoad, onPullDownRefresh } from '@dcloudio/uni-app'
import AppNav from '../../components/AppNav.vue'
import StateView from '../../components/StateView.vue'
import { useAuthStore } from '../../stores/auth'
import { useCampusStore } from '../../stores/campus'
import { dateLabel } from '../../utils/date'

const auth = useAuthStore()
const campus = useCampusStore()
const dayIndex = ref(0)
const location = ref('')
const selectedPeriods = ref([])
const touchStartX = ref(0)
const periodLabels = ['1-2', '3-4', '5-6', '7-8', '9-11']

function sourceFor(index) {
  return index === 0 ? campus.todayClassroom : campus.tomorrowClassroom
}

function mapFor(index) {
  const source = sourceFor(index)
  const value = source?.classrooms || source || {}
  return value && typeof value === 'object' && !Array.isArray(value) ? value : {}
}

const classroomMap = computed(() => mapFor(dayIndex.value))
const locations = computed(() => Object.keys(classroomMap.value).filter((key) => Array.isArray(classroomMap.value[key])))
const filteredRooms = computed(() => {
  const rooms = classroomMap.value[location.value]
  if (!Array.isArray(rooms)) return []
  return rooms.filter((room) => {
    const status = Array.isArray(room?.status) ? room.status : []
    return selectedPeriods.value.every((period) => status[period] === '空')
  })
})

watch(locations, (values) => {
  if (!values.includes(location.value)) location.value = values[0] || ''
}, { immediate: true })

onLoad(async () => {
  if (!auth.authenticated) return uni.reLaunch({ url: '/pages/login/index' })
  if (!campus.todayClassroom && !auth.offline) await refresh()
})

onPullDownRefresh(async () => {
  await refresh()
  uni.stopPullDownRefresh()
})

async function refresh() {
  if (auth.offline) return uni.showToast({ title: '离线模式下无法刷新', icon: 'none' })
  await campus.loadClassrooms().catch((error) => {
    if (!error.authExpired) uni.showToast({ title: error.message, icon: 'none' })
  })
}

function togglePeriod(index) {
  selectedPeriods.value = selectedPeriods.value.includes(index)
    ? selectedPeriods.value.filter((value) => value !== index)
    : [...selectedPeriods.value, index]
}

function switchDay(index) {
  if (index === dayIndex.value) return
  dayIndex.value = index
  location.value = locations.value[0] || ''
}

function onTouchStart(event) {
  touchStartX.value = event.changedTouches?.[0]?.clientX || 0
}

function onTouchEnd(event) {
  const endX = event.changedTouches?.[0]?.clientX || 0
  const distance = endX - touchStartX.value
  if (distance < -60) switchDay(1)
  if (distance > 60) switchDay(0)
}
</script>

<template>
  <view class="page page-with-nav classroom-page">
    <view class="date-row">
      <text class="date-title">{{ dateLabel(dayIndex) }}</text>
      <text class="day-label">{{ dayIndex === 0 ? '今天' : '明天' }}</text>
    </view>
    <view class="day-tabs">
      <view @tap="switchDay(0)">今天</view>
      <view @tap="switchDay(1)">明天</view>
      <view :class="['day-indicator', { right: dayIndex === 1 }]" />
    </view>
    <StateView
      :loading="campus.loading.classroom && !locations.length"
      :error="campus.errors.classroom"
      :empty="!locations.length"
      empty-text="当前暂无空教室数据"
      @retry="refresh"
    >
      <scroll-view class="location-scroll" scroll-x :show-scrollbar="false">
        <view class="location-list">
          <view
            v-for="item in locations"
            :key="item"
            :class="['location-chip', { active: item === location }]"
            @tap="location = item"
          >{{ item }}</view>
        </view>
      </scroll-view>
      <view class="filter-row">
        <text class="room-heading">教室</text>
        <view class="period-filters">
          <view
            v-for="(label, index) in periodLabels"
            :key="label"
            :class="['period-chip', { active: selectedPeriods.includes(index) }]"
            @tap="togglePeriod(index)"
          >{{ label }}</view>
        </view>
      </view>
      <scroll-view
        class="room-list"
        scroll-y
        :scroll-top="0"
        @touchstart="onTouchStart"
        @touchend="onTouchEnd"
      >
        <view
          v-for="(room, roomIndex) in filteredRooms"
          :key="`${room.name}-${roomIndex}`"
          class="room-row"
        >
          <view class="room-name"><text>{{ room.name }}</text></view>
          <view class="status-cell">
            <view class="status-track" />
            <view class="segments">
              <view
                v-for="(status, index) in room.status"
                :key="index"
                :class="['status-segment', status === '空' ? 'free' : 'busy']"
              />
            </view>
          </view>
        </view>
        <view v-if="!filteredRooms.length" class="empty">没有满足筛选条件的教室</view>
      </scroll-view>
    </StateView>
    <AppNav current="classroom" />
  </view>
</template>

<style scoped lang="scss">
/* #ifdef H5 */
.room-list{height:calc(var(--app-height) - var(--status-bar-height) - 300rpx - var(--nav-height))!important}
/* #endif */
.classroom-page { height: var(--app-height, 100vh); overflow: hidden; padding-top: var(--status-bar-height); }
.date-row { display: flex; height: 66rpx; padding: 0 40rpx 10rpx; align-items: flex-end; }
.date-title { font-size: 46rpx; font-weight: 800; line-height: 1.2; }
.day-label { margin-left: 20rpx; color: #515151; font-size: 31rpx; font-weight: 800; line-height: 1.2; }
.day-tabs { position: relative; display: flex; height: 74rpx; }
.day-tabs > view:not(.day-indicator) { padding: 8rpx 0; flex: 1; color: #515151; font-size: 30rpx; text-align: center; }
.day-indicator { position: absolute; bottom: 0; left: calc(25% - 100rpx); width: 200rpx; height: 16rpx; border-radius: 10rpx; background: var(--primary); transition: transform .25s; }
.day-indicator.right{left:calc(75% - 100rpx);transform:none}
.location-scroll { width: 100%; height: 80rpx; margin-top: 20rpx; white-space: nowrap; }
.location-list { display: inline-flex; padding: 10rpx 40rpx; }
.location-chip { min-width: 142rpx; padding: 4rpx 24rpx; border-radius: 40rpx; color: #171717; font-size: 29rpx; font-weight: 800; text-align: center; }
.location-chip.active { background: var(--primary); }
.filter-row { display: flex; height: 60rpx; padding: 10rpx 30rpx 0; align-items: center; }
.room-heading { width: 137rpx; color: #515151; font-size: 27rpx; }
.period-filters { display: flex; padding-left: 16rpx; flex: 1; }
.period-chip { display: flex; min-width: 0; height: 42rpx; margin: 0 10rpx; flex: 1; align-items: center; justify-content: center; border-radius: 40rpx; color: #515151; font-size: 25rpx; }
.period-chip.active { color: #171717; background: var(--primary); }
.room-list { height: calc(var(--app-height, 100vh) - var(--status-bar-height) - 406rpx - var(--safe-area-bottom)); padding: 10rpx 30rpx; }
.room-row { display: flex; height: 74rpx; padding: 4rpx 0; align-items: center; }
.room-name { width: 137rpx; padding-right: 4rpx; overflow: hidden; color: #171717; font-size: 27rpx; white-space: nowrap; }
.room-name text { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.status-cell { position: relative; height: 66rpx; padding-left: 16rpx; flex: 1; }
.status-track { position: absolute; top: 0; right: 24rpx; bottom: 0; left: 40rpx; border-radius: 40rpx; background: var(--surface); }
.segments { position: absolute; inset: 0; display: flex; align-items: stretch; }
.status-segment { margin: 0 4rpx; flex: 1; }
.free { background: #34e800; }
.busy { background: #e82e00; }
</style>

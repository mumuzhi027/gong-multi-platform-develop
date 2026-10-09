<script setup>
defineProps({ course: { type: Object, default: null }, removable: Boolean })
const emit = defineEmits(['close', 'delete'])
</script>

<template>
  <view v-if="course" class="sheet-host" @tap="emit('close')">
    <view class="sheet" @tap.stop>
      <view class="handle"></view>
      <view class="sheet-title">{{ course.name }}</view>
      <view class="detail-grid">
        <view><image class="detail-icon" src="/static/icons/teacher.svg" mode="aspectFit" />{{ course.teacher || '教师待定' }}</view>
        <view><image class="detail-icon" src="/static/icons/course.svg" mode="aspectFit" />{{ course.weeks || '-' }}周</view>
        <view><image class="detail-icon" src="/static/icons/baseline_location_on_24.svg" mode="aspectFit" />{{ course.classroom || '地点待定' }}</view>
        <view>
          <image class="detail-icon" src="/static/icons/baseline_access_time_filled_24.svg" mode="aspectFit" />
          {{ course.start_time }}-{{ Number(course.start_time) + Number(course.duration || 1) - 1 }}节
        </view>
      </view>
      <view v-if="removable" class="sheet-actions"><button @tap="emit('delete', course)">从课表删除</button></view>
    </view>
  </view>
</template>

<style scoped lang="scss">
.sheet-host {
  position: fixed;
  z-index: 100;
  inset: 0;
  display: flex;
  align-items: flex-end;
  background: rgba(0, 0, 0, 0.3);
}

.sheet {
  width: 100%;
  padding: 16rpx 30rpx calc(20rpx + var(--safe-area-bottom));
  border-radius: 36rpx 36rpx 0 0;
  background: var(--card);
}

.handle {
  width: 80rpx;
  height: 8rpx;
  margin: 0 auto 30rpx;
  border-radius: 10rpx;
  background: #c9c9c9;
}

.sheet-title {
  margin-bottom: 16rpx;
  font-size: 40rpx;
  font-weight: 800;
}

.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16rpx 20rpx;
  color: #171717;
  font-size: 30rpx;
}

.detail-grid view { display: flex; align-items: center; }

.detail-icon {
  width: 40rpx;
  height: 40rpx;
  margin-right: 20rpx;
}
.sheet-actions{display:flex;margin-top:16rpx;justify-content:flex-end}.sheet-actions button{margin:0;padding:16rpx 24rpx;color:#ba1a1a;background:transparent;font-size:28rpx;line-height:1.3}
</style>

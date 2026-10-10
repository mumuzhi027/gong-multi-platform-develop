<script setup>
import { computed, ref, watch } from 'vue'
import CourseFlowBorder from './CourseFlowBorder.vue'
import { courseColor, isStaggeredCourse } from '../utils/course'
import { isExperimentalCourse } from '../utils/courseSlots'
import { courseDisplayAlpha } from '../utils/courseTimeDisplay'

const props = defineProps({ slot: { type: Object, required: true }, timeMode: { type: String, default: '混合' } })
const emit = defineEmits(['open'])
const selected = ref(0)
watch(() => props.slot.courses, () => { selected.value = 0 })
const course = computed(() => props.slot.courses[selected.value] || props.slot.courses[0])
const experimental = computed(() => isExperimentalCourse(course.value))
const hasExperiment = computed(() => props.slot.courses.some(isExperimentalCourse))
const conflict = computed(() => hasExperiment.value && props.slot.courses.length > 1)
const font = computed(() => {
  const length = Math.max(course.value.name.length, (course.value.classroom || '').length)
  return props.slot.duration >= 3 ? 23 : props.slot.duration === 2 ? 21 : length >= 11 ? 18 : length >= 8 ? 19 : 21
})
function open() {
  if (conflict.value && !experimental.value) selected.value = props.slot.courses.findIndex(isExperimentalCourse)
  else emit('open', course.value)
}
function cycle() { selected.value = (selected.value + 1) % props.slot.courses.length }
</script>

<template>
  <view class="course-block" :style="{ opacity: courseDisplayAlpha(timeMode, isStaggeredCourse(course)), '--font': `${font}rpx` }"
    role="button" :aria-label="`${course.name}${experimental ? '，实验课' : ''}${conflict && !experimental ? '，点击切换实验课' : '，点击查看详情'}`" @tap="open" @longpress="emit('open', course)">
    <Transition name="course-swap">
      <view :key="selected" class="course-face" :style="{ backgroundColor: courseColor(course.name), '--course-fill': courseColor(course.name) }">
        <template v-if="isStaggeredCourse(course)"><CourseFlowBorder :diameter="Math.max(240, slot.duration * 130)"/><view class="flow-surface"/></template>
        <view v-if="conflict && !experimental" class="conflict-sheen" aria-hidden="true"/>
        <view class="course-copy"><text>{{ course.name }}</text><small v-if="course.classroom">{{ course.classroom }}</small></view>
        <view v-if="experimental" class="experiment-label" :class="{ switchable: conflict }"
          :role="conflict ? 'button' : undefined" :aria-label="conflict ? '实验课，点击切换同一时段课程' : '实验课'" @tap.stop="conflict ? cycle() : emit('open', course)">实验课</view>
      </view>
    </Transition>
  </view>
</template>

<style scoped lang="scss">
.course-block{position:absolute;z-index:1;right:4rpx;left:4rpx;overflow:hidden;border-radius:8rpx;transition:opacity 320ms cubic-bezier(.4,0,.2,1)}
.course-face{position:absolute;inset:0;display:flex;flex-direction:column;padding:7rpx 5rpx;border-radius:inherit;text-align:center;overflow:hidden}
.course-copy{position:relative;z-index:1;flex:1;min-height:0;display:flex;flex-direction:column;align-items:center;justify-content:center}
.course-copy text{display:-webkit-box;overflow:hidden;font-size:var(--font);font-weight:600;line-height:1.12;-webkit-box-orient:vertical;-webkit-line-clamp:4}
.course-copy small{display:-webkit-box;margin-top:7rpx;overflow:hidden;font-size:calc(var(--font) - 2rpx);line-height:1.1;-webkit-box-orient:vertical;-webkit-line-clamp:3}
.experiment-label{position:relative;z-index:1;flex-shrink:0;align-self:center;margin-top:5rpx;padding:2rpx 8rpx;border:1rpx solid rgba(23,23,23,.14);border-radius:6rpx;background:rgba(255,255,255,.42);color:rgba(23,23,23,.85);font-size:16rpx;font-weight:500;letter-spacing:.4rpx;line-height:1.4;white-space:nowrap}
.flow-surface{position:absolute;inset:6rpx;border-radius:inherit;background:var(--course-fill);box-shadow:0 0 0 1rpx var(--course-flow-separator,#fff);pointer-events:none}
.conflict-sheen{position:absolute;z-index:1;inset:0;pointer-events:none;background:linear-gradient(135deg,transparent 44%,rgba(255,255,255,.16) 50%,transparent 56%);background-size:400% 400%;animation:conflict-sheen 5s linear infinite}
@keyframes conflict-sheen{0%{background-position:100% 100%}40%,100%{background-position:0% 0%}}
.course-swap-enter-active,.course-swap-leave-active{transition:opacity 320ms cubic-bezier(.4,0,.2,1)}
.course-swap-enter-from,.course-swap-leave-to{opacity:0}
@media(prefers-reduced-motion:reduce){.course-block,.course-swap-enter-active,.course-swap-leave-active{transition:none}.conflict-sheen{animation:none;display:none}}
</style>

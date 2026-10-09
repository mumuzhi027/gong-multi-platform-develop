<script setup>
import { computed, reactive } from 'vue'
import { parseWeeks } from '../utils/date'
import { validateCustomCourse } from '../utils/local-courses'
const props = defineProps({ editor: { type: Object, required: true } })
const emit = defineEmits(['close', 'save', 'delete'])
const course = props.editor.course
const weeks = [...parseWeeks(course?.weeks)].sort((a, b) => a - b)
const form = reactive({
  name: course?.name || '', teacher: course?.teacher || '', classroom: course?.classroom || '',
  day: props.editor.day, startWeek: String(weeks[0] || props.editor.week),
  endWeek: String(weeks.at(-1) || props.editor.week),
  start: String(course?.start_time || props.editor.start),
  end: String(course ? Number(course.start_time) + Number(course.duration) - 1 : props.editor.start),
})
const value = computed(() => ({ ...form, startWeek: Number(form.startWeek), endWeek: Number(form.endWeek), start: Number(form.start), end: Number(form.end) }))
const valid = computed(() => validateCustomCourse(value.value))
</script>
<template>
  <view class="editor-mask" @tap="emit('close')" @touchmove.stop>
    <view class="editor-dialog" @tap.stop role="dialog" aria-modal="true" :aria-label="course ? '编辑课程' : '自主添加课程'">
      <text class="editor-title">{{ course ? '编辑课程' : '自主添加课程' }}</text>
      <scroll-view scroll-y class="editor-fields">
        <label class="editor-field">课程名称<input v-model="form.name" placeholder="课程名称" /></label>
        <label class="editor-field">老师<input v-model="form.teacher" placeholder="老师" /></label>
        <label class="editor-field">上课地点<input v-model="form.classroom" placeholder="上课地点" /></label>
        <view class="editor-pair"><label class="editor-field">开始周次<input v-model="form.startWeek" type="number" /></label><label class="editor-field">结束周次<input v-model="form.endWeek" type="number" /></label></view>
        <view class="editor-pair"><label class="editor-field">开始节次<input v-model="form.start" type="number" /></label><label class="editor-field">结束节次<input v-model="form.end" type="number" /></label></view>
      </scroll-view>
      <view class="editor-actions">
        <button v-if="course" class="delete" @tap="emit('delete', course)">删除</button>
        <button @tap="emit('close')">取消</button>
        <button class="save" :disabled="!valid" @tap="valid && emit('save', value)">保存</button>
      </view>
    </view>
  </view>
</template>
<style scoped lang="scss">
.editor-mask{position:fixed;z-index:120;inset:0;display:flex;padding:calc(env(safe-area-inset-top) + 40rpx) 48rpx calc(var(--safe-area-bottom) + 40rpx);align-items:center;justify-content:center;background:rgba(0,0,0,.4)}
.editor-dialog{width:100%;max-width:660rpx;max-height:100%;padding:48rpx;border-radius:56rpx;background:var(--background)}
.editor-title{display:block;margin-bottom:32rpx;font-size:48rpx;font-weight:500}
.editor-fields{max-height:60vh;height:610rpx}.editor-field{display:block;min-width:0;margin-bottom:16rpx;color:var(--muted);font-size:24rpx}.editor-field input{height:82rpx;margin-top:6rpx;padding:0 20rpx;border:2rpx solid #79747e;border-radius:8rpx;color:var(--text);font-size:32rpx}.editor-pair{display:flex;gap:16rpx}.editor-pair .editor-field{flex:1}
.editor-actions{display:flex;margin-top:32rpx;justify-content:flex-end;gap:12rpx}.editor-actions button{min-height:0;margin:0;padding:16rpx 24rpx;border-radius:40rpx;color:#24617e;background:transparent;font-size:28rpx;line-height:1.3}.editor-actions .delete{margin-right:auto;color:#ba1a1a}.editor-actions .save{background:var(--primary)}.editor-actions .save[disabled]{opacity:.4}
</style>

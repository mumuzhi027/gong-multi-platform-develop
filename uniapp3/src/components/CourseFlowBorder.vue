<script setup>
defineProps({ diameter: { type: Number, default: 1000 } })
</script>

<template>
  <view class="course-flow-border" aria-hidden="true" :style="{ '--flow-diameter': `${diameter}rpx` }">
    <view class="course-flow-gradient" />
  </view>
</template>

<style scoped lang="scss">
.course-flow-border {
  position: absolute;
  inset: 0;
  overflow: hidden;
  border-radius: inherit;
  pointer-events: none;
}
.course-flow-gradient {
  position: absolute;
  top: 50%;
  left: 50%;
  width: var(--flow-diameter);
  height: var(--flow-diameter);
  border-radius: 50%;
  background: conic-gradient(
    var(--course-flow-track, #d5deed) 0deg 20deg,
    var(--course-flow-start, #004fff) 48deg 100deg,
    var(--course-flow-track, #d5deed) 110deg 200deg,
    var(--course-flow-middle, #d50079) 228deg 256deg,
    var(--course-flow-track, #d5deed) 266deg 360deg
  );
  animation: course-border-flow 6s linear infinite;
}
@keyframes course-border-flow {
  from { transform: translate(-50%, -50%) rotate(0deg); }
  to { transform: translate(-50%, -50%) rotate(360deg); }
}
@media (prefers-reduced-motion: reduce) {
  .course-flow-gradient { animation: none; transform: translate(-50%, -50%); }
}
</style>

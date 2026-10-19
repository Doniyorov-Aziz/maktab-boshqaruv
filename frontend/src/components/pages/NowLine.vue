<template>
  <div
    v-if="pos !== null"
    class="now-line"
    :class="`now-line--${direction}`"
    :style="direction === 'vertical' ? { left: `${pos}%` } : { top: `${pos}%` }"
    data-now-line
    role="presentation"
  >
    <span class="now-line__label">{{ label }}</span>
  </div>
</template>

<script setup>
// The red "now" line over a time scale (the day's first start to its last end). It moves
// every minute; outside the scale it is not drawn. vertical: time runs left to right.
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { tashkentNow } from '@/utils/tashkent'

const props = defineProps({
  // minutes since midnight
  from: { type: Number, required: true },
  to: { type: Number, required: true },
  direction: { type: String, default: 'vertical' },
  // a fixed time instead of the clock ("o'sha paytda")
  minutes: { type: Number, default: null }
})

const now = ref(tashkentNow().minutes)
let timer = null
onMounted(() => {
  timer = setInterval(() => (now.value = tashkentNow().minutes), 60_000)
})
onBeforeUnmount(() => clearInterval(timer))

const at = computed(() => (props.minutes !== null ? props.minutes : now.value))
const pos = computed(() => {
  if (props.to <= props.from || at.value < props.from || at.value > props.to)
    return null
  return ((at.value - props.from) / (props.to - props.from)) * 100
})
const label = computed(() => {
  const m = Math.floor(at.value)
  return `${String(Math.floor(m / 60)).padStart(2, '0')}:${String(m % 60).padStart(2, '0')}`
})
</script>

<style scoped>
.now-line {
  position: absolute;
  z-index: 3;
  pointer-events: none;
  background: var(--now-line);
  transition:
    left var(--dur) linear,
    top var(--dur) linear;
}

.now-line--vertical {
  top: 0;
  bottom: 0;
  width: var(--hairline);
  box-shadow: 0 0 0 var(--hairline)
    color-mix(in srgb, var(--now-line) 25%, transparent);
}

.now-line--horizontal {
  left: 0;
  right: 0;
  height: var(--hairline);
}

.now-line__label {
  position: absolute;
  top: calc(var(--space-4) * -1);
  left: 50%;
  transform: translateX(-50%);
  padding: 0 var(--space-1);
  border-radius: var(--radius-sm);
  background: var(--now-line);
  color: white;
  font-size: var(--text-xs);
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  line-height: 1.4;
}

.now-line--horizontal .now-line__label {
  top: 50%;
  left: 0;
  transform: translateY(-50%);
}

@media (prefers-reduced-motion: reduce) {
  .now-line {
    transition: none;
  }
}
</style>

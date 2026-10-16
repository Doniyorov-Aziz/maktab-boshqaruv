<template>
  <span class="score" :class="`score--${tone}`" :title="title">
    <svg viewBox="0 0 44 44" aria-hidden="true">
      <circle cx="22" cy="22" r="18" class="score__track" />
      <circle
        cx="22"
        cy="22"
        r="18"
        class="score__arc"
        :style="{
          strokeDasharray: C,
          strokeDashoffset: grown ? C * (1 - clamped / 100) : C
        }"
        transform="rotate(-90 22 22)"
      />
    </svg>
    <b class="score__num">{{ value }}</b>
  </span>
</template>

<script setup>
// A 0–100 score in a small ring: red from 60, amber below. Fills in when it appears.
import { computed, ref, onMounted } from 'vue'
import { reducedMotion } from '@/composables/useCountUp'

const props = defineProps({
  value: { type: Number, required: true },
  title: { type: String, default: '' }
})
const C = 2 * Math.PI * 18
const clamped = computed(() => Math.max(0, Math.min(100, props.value)))
const tone = computed(() => (props.value >= 60 ? 'high' : 'mid'))
const grown = ref(reducedMotion())
onMounted(() => {
  if (!grown.value)
    requestAnimationFrame(() =>
      requestAnimationFrame(() => (grown.value = true))
    )
})
</script>

<style scoped>
.score {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: var(--ring-score);
  height: var(--ring-score);
  flex-shrink: 0;
}

.score svg {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.score__track {
  fill: none;
  stroke: var(--ring-track);
  stroke-width: 4;
}

.score__arc {
  fill: none;
  stroke-width: 4;
  stroke-linecap: round;
  transition: stroke-dashoffset var(--dur-ring) var(--ease-out);
}

.score--high .score__arc {
  stroke: var(--st-absent);
}

.score--mid .score__arc {
  stroke: var(--gr-3);
}

.score__num {
  font-size: var(--text-xs);
  font-variant-numeric: tabular-nums;
}

.score--high .score__num {
  color: var(--st-absent);
}

@media (prefers-reduced-motion: reduce) {
  .score__arc {
    transition: none;
  }
}
</style>

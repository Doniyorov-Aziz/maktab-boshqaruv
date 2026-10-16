<template>
  <figure class="mring" :class="`mring--${size}`" data-ring="multi">
    <svg viewBox="0 0 200 200" role="img" :aria-label="ariaLabel">
      <g v-for="(r, i) in drawn" :key="r.key" transform="rotate(-90 100 100)">
        <circle
          cx="100"
          cy="100"
          :r="r.radius"
          class="mring__track"
          :style="{ strokeWidth: r.width }"
        />
        <circle
          v-if="r.value !== null"
          cx="100"
          cy="100"
          :r="r.radius"
          class="mring__arc"
          :style="{
            stroke: r.color,
            strokeWidth: r.width,
            strokeDasharray: r.circ,
            strokeDashoffset: grown ? r.offset : r.circ,
            transitionDelay: `${i * 70}ms`
          }"
        >
          <title>{{ r.label }}: {{ r.value }}%</title>
        </circle>
      </g>
    </svg>
    <figcaption class="mring__center">
      <slot />
    </figcaption>
  </figure>
</template>

<script setup>
// Concentric progress rings in the Apple Fitness manner: an optional thick outer ring
// (the main number) and thin rings inside it, each 0–100 in its own colour. They fill
// over 800 ms when they appear (at once with prefers-reduced-motion). A ring without a
// value shows only its track.
import { computed, ref, onMounted } from 'vue'
import { reducedMotion } from '@/composables/useCountUp'

const props = defineProps({
  // the thick outer ring: { key, label, value, color } or null
  main: { type: Object, default: null },
  // thin rings, outside in: [{ key, label, value, color }]
  rings: { type: Array, default: () => [] },
  // 'hero' (180 px) or 'mid' (110 px)
  size: { type: String, default: 'hero' }
})

const grown = ref(reducedMotion())
onMounted(() => {
  if (!grown.value)
    requestAnimationFrame(() =>
      requestAnimationFrame(() => (grown.value = true))
    )
})

const clamp = v => Math.max(0, Math.min(100, v))

const drawn = computed(() => {
  const out = []
  let radius = 90
  const add = (r, width) => {
    const circ = 2 * Math.PI * radius
    const v =
      typeof r.value === 'number' && Number.isFinite(r.value)
        ? clamp(r.value)
        : null
    out.push({
      ...r,
      value: v,
      radius,
      width,
      circ,
      offset: v === null ? circ : circ * (1 - v / 100)
    })
  }
  if (props.main) {
    radius = 88
    add(props.main, 14)
    radius -= 18
  }
  const thin = props.main ? 7 : 12
  const step = props.main ? 10 : 16
  for (const r of props.rings) {
    add(r, thin)
    radius -= step
  }
  return out
})

const ariaLabel = computed(() =>
  [props.main, ...props.rings]
    .filter(Boolean)
    .map(r => `${r.label} ${r.value ?? '—'}%`)
    .join(', ')
)
</script>

<style scoped>
.mring {
  position: relative;
  margin: 0;
  flex-shrink: 0;
  width: var(--ring-hero);
  height: var(--ring-hero);
}

.mring--mid {
  width: var(--ring-mid);
  height: var(--ring-mid);
}

.mring svg {
  width: 100%;
  height: 100%;
  overflow: visible;
}

.mring__track {
  fill: none;
  stroke: var(--ring-track);
}

.mring__arc {
  fill: none;
  stroke-linecap: round;
  transition: stroke-dashoffset var(--dur-ring) var(--ease-out);
}

.mring__center {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  pointer-events: none;
}

@media (prefers-reduced-motion: reduce) {
  .mring__arc {
    transition: none;
  }
}
</style>

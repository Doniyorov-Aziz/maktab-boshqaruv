<template>
  <svg
    class="sparkline"
    :viewBox="`0 0 ${W} ${H}`"
    preserveAspectRatio="none"
    role="img"
    :aria-label="label"
  >
    <path
      v-if="area"
      :d="area"
      class="sparkline__area"
      :style="{ fill: fillColor }"
    />
    <path
      v-for="(seg, i) in segments"
      :key="i"
      :d="seg"
      class="sparkline__line"
      :style="{ stroke: color }"
      vector-effect="non-scaling-stroke"
    />
    <circle
      v-if="last"
      :cx="last[0]"
      :cy="last[1]"
      r="2.5"
      class="sparkline__dot"
      :style="{ fill: color }"
      vector-effect="non-scaling-stroke"
    />
  </svg>
</template>

<script setup>
// A small trend line (SVG, no chart library). Days without data (null) leave a gap
// instead of dropping to zero; with fewer than two numbers nothing is drawn. Every
// coordinate is a finite number — no NaN ever reaches the path.
import { computed } from 'vue'

const props = defineProps({
  values: { type: Array, default: () => [] },
  color: { type: String, default: 'var(--chart-primary)' },
  fill: { type: Boolean, default: true },
  // fixed scale (e.g. 0–100 for percentages); otherwise min/max of the data
  min: { type: Number, default: null },
  max: { type: Number, default: null },
  label: { type: String, default: '' }
})

const W = 120
const H = 36
const PAD = 3

const nums = computed(() =>
  props.values.map(v =>
    typeof v === 'number' && Number.isFinite(v) ? v : null
  )
)

const scale = computed(() => {
  const present = nums.value.filter(v => v !== null)
  if (present.length < 2) return null
  let lo = props.min ?? Math.min(...present)
  let hi = props.max ?? Math.max(...present)
  if (hi === lo) {
    lo -= 1
    hi += 1
  }
  const n = nums.value.length
  return {
    x: i => (n === 1 ? W / 2 : PAD + (i * (W - 2 * PAD)) / (n - 1)),
    y: v => H - PAD - ((v - lo) / (hi - lo)) * (H - 2 * PAD)
  }
})

const points = computed(() => {
  if (!scale.value) return []
  return nums.value.map((v, i) =>
    v === null ? null : [scale.value.x(i), scale.value.y(v)]
  )
})

/** Runs of consecutive points; a null splits the line. */
const runs = computed(() => {
  const out = []
  let cur = []
  for (const p of points.value) {
    if (p) cur.push(p)
    else if (cur.length) {
      out.push(cur)
      cur = []
    }
  }
  if (cur.length) out.push(cur)
  return out
})

const fmt = n => n.toFixed(2)
const segments = computed(() =>
  runs.value
    .filter(r => r.length >= 2)
    .map(r =>
      r.map((p, i) => `${i ? 'L' : 'M'}${fmt(p[0])},${fmt(p[1])}`).join(' ')
    )
)
const area = computed(() => {
  if (!props.fill) return null
  const longest = runs.value.reduce((a, r) => (r.length > a.length ? r : a), [])
  if (longest.length < 2) return null
  const line = longest
    .map((p, i) => `${i ? 'L' : 'M'}${fmt(p[0])},${fmt(p[1])}`)
    .join(' ')
  return `${line} L${fmt(longest.at(-1)[0])},${H} L${fmt(longest[0][0])},${H} Z`
})
const last = computed(() => {
  const ps = points.value.filter(Boolean)
  return ps.length >= 2 ? ps.at(-1) : null
})
const fillColor = computed(() =>
  props.color === 'var(--chart-primary)'
    ? 'var(--chart-primary-fill)'
    : `color-mix(in srgb, ${props.color} 14%, transparent)`
)
</script>

<style scoped>
.sparkline {
  display: block;
  width: 100%;
  height: var(--spark-h);
  overflow: visible;
}

.sparkline__line {
  fill: none;
  stroke-width: 1.75;
  stroke-linejoin: round;
  stroke-linecap: round;
}

.sparkline__area {
  stroke: none;
}
</style>

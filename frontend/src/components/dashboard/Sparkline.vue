<template>
  <svg
    class="sparkline"
    :class="{ 'sparkline--lg': large }"
    :viewBox="`0 0 ${W} ${H}`"
    preserveAspectRatio="none"
    role="img"
    :aria-label="label"
  >
    <defs>
      <linearGradient :id="gradId" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0%" :style="{ stopColor: color, stopOpacity: 0.28 }" />
        <stop offset="100%" :style="{ stopColor: color, stopOpacity: 0 }" />
      </linearGradient>
    </defs>
    <path
      v-if="area"
      :d="area"
      class="sparkline__area"
      :style="{ fill: `url(#${gradId})` }"
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
// A small trend line (SVG, no chart library): a smooth monotone curve with a soft
// gradient under it. Days without data (null) leave a gap instead of dropping to
// zero; with fewer than two numbers nothing is drawn. Every coordinate is a finite
// number — no NaN ever reaches the path.
import { computed, useId } from 'vue'

const props = defineProps({
  values: { type: Array, default: () => [] },
  color: { type: String, default: 'var(--chart-primary)' },
  fill: { type: Boolean, default: true },
  // fixed scale (e.g. 0–100 for percentages); otherwise min/max of the data
  min: { type: Number, default: null },
  max: { type: Number, default: null },
  label: { type: String, default: '' },
  large: { type: Boolean, default: false }
})

// each sparkline's gradient needs its own id on the page
const gradId = `spark-${useId()}`

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
  if (props.min === null && props.max === null) {
    // a little room above and below so the curve never touches the edges
    const pad = (hi - lo) * 0.15
    lo -= pad
    hi += pad
  }
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

/** A monotone cubic curve through the points (Fritsch–Carlson): smooth, no overshoot. */
function curve(r) {
  if (r.length < 3)
    return r
      .map((p, i) => `${i ? 'L' : 'M'}${fmt(p[0])},${fmt(p[1])}`)
      .join(' ')
  const n = r.length
  const dx = [],
    dy = [],
    m = []
  for (let i = 0; i < n - 1; i++) {
    dx.push(r[i + 1][0] - r[i][0])
    dy.push(r[i + 1][1] - r[i][1])
    m.push(dx[i] === 0 ? 0 : dy[i] / dx[i])
  }
  const t = [m[0]]
  for (let i = 1; i < n - 1; i++)
    t.push(m[i - 1] * m[i] <= 0 ? 0 : (m[i - 1] + m[i]) / 2)
  t.push(m[n - 2])
  for (let i = 0; i < n - 1; i++) {
    if (m[i] === 0) {
      t[i] = 0
      t[i + 1] = 0
      continue
    }
    const a = t[i] / m[i]
    const b = t[i + 1] / m[i]
    const s = a * a + b * b
    if (s > 9) {
      const k = 3 / Math.sqrt(s)
      t[i] = k * a * m[i]
      t[i + 1] = k * b * m[i]
    }
  }
  let d = `M${fmt(r[0][0])},${fmt(r[0][1])}`
  for (let i = 0; i < n - 1; i++) {
    const h = dx[i] / 3
    d += ` C${fmt(r[i][0] + h)},${fmt(r[i][1] + t[i] * h)} ${fmt(r[i + 1][0] - h)},${fmt(r[i + 1][1] - t[i + 1] * h)} ${fmt(r[i + 1][0])},${fmt(r[i + 1][1])}`
  }
  return d
}

const segments = computed(() =>
  runs.value.filter(r => r.length >= 2).map(curve)
)

const area = computed(() => {
  if (!props.fill) return null
  const longest = runs.value.reduce((a, r) => (r.length > a.length ? r : a), [])
  if (longest.length < 2) return null
  return `${curve(longest)} L${fmt(longest.at(-1)[0])},${H} L${fmt(longest[0][0])},${H} Z`
})

const last = computed(() => {
  const ps = points.value.filter(Boolean)
  return ps.length >= 2 ? ps.at(-1) : null
})
</script>

<style scoped>
.sparkline {
  display: block;
  width: 100%;
  height: var(--spark-h);
  overflow: visible;
}

.sparkline--lg {
  height: var(--spark-h-lg);
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

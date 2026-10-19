<template>
  <div class="yl" data-year-line>
    <div class="yl__bar" role="img" :aria-label="aria">
      <span
        v-for="s in quarters"
        :key="s.label"
        class="yl__seg"
        :style="{
          left: `${pos(s.from)}%`,
          width: `${len(s.from, s.to)}%`,
          '--c': `var(--year-quarter-${s.n})`
        }"
        :title="`${s.label}: ${fmt(s.from)} – ${fmt(s.to)}`"
        :data-segment="s.label"
      >
        <span class="yl__seg-label">{{ s.label }}</span>
      </span>
      <span
        v-for="v in vacations"
        :key="v.label + v.from"
        class="yl__vac"
        :style="{
          left: `${pos(v.from)}%`,
          width: `${Math.max(len(v.from, v.to), 0.6)}%`
        }"
        :title="`${v.label}: ${fmt(v.from)} – ${fmt(v.to)}`"
        data-segment="vacation"
      />
      <span class="yl__done" :style="{ width: `${pos(today)}%` }" />
      <span
        class="yl__here"
        :style="{ left: `${pos(today)}%` }"
        data-you-are-here
      >
        <span class="yl__here-label">Siz shu yerdasiz</span>
      </span>
    </div>
    <div class="yl__months">
      <span
        v-for="m in months"
        :key="m.key"
        :style="{ left: `${pos(m.date)}%` }"
        >{{ m.label }}</span
      >
    </div>
    <div class="yl__note">
      <template v-if="quarter">
        <b
          >{{ ROMAN[quarter] }} chorak:
          {{ done === null ? '—' : `${Math.round(done)}%` }} o'tdi</b
        >
        · {{ left }} ish kuni qoldi
      </template>
      <template v-else-if="onVacation"><b>Ta'til</b> — dam oling!</template>
      <span class="yl__legend">
        <span class="yl__key yl__key--q" /> choraklar
        <span class="yl__key yl__key--v" /> ta'tillar
      </span>
    </div>
  </div>
</template>

<script setup>
// The school year as one line: the four quarters in their colours, vacations striped on
// top, the part gone shaded, and a "you are here" marker. Under it: how much of the
// quarter is gone and how many school days are left.
import { computed } from 'vue'
import { MONTHS, daysBetween, parse } from './calendar'

const props = defineProps({
  // the calendar overview: yearStart, yearEnd, today, segments, quarter, quarterDone, …
  data: { type: Object, required: true }
})

const ROMAN = { 1: 'I', 2: 'II', 3: 'III', 4: 'IV' }
const total = computed(() =>
  Math.max(1, daysBetween(props.data.yearStart, props.data.yearEnd) + 1)
)
const pos = d =>
  Math.max(
    0,
    Math.min(100, (daysBetween(props.data.yearStart, d) / total.value) * 100)
  )
const len = (a, b) => ((daysBetween(a, b) + 1) / total.value) * 100
const fmt = d => {
  const x = parse(d)
  return `${x.getDate()}-${MONTHS[x.getMonth()].slice(0, 3).toLowerCase()}`
}

const quarters = computed(() =>
  props.data.segments
    .filter(s => s.kind === 'QUARTER')
    .map(s => ({ ...s, n: Number(s.label[0]) }))
)
const vacations = computed(() =>
  props.data.segments.filter(s => s.kind === 'VACATION')
)
const today = computed(() => props.data.today)
const quarter = computed(() => props.data.quarter)
const done = computed(() => props.data.quarterDone)
const left = computed(() => props.data.quarterDaysLeft)
const onVacation = computed(() => props.data.onVacation)
const months = computed(() => {
  const out = []
  const d = parse(props.data.yearStart)
  d.setDate(1)
  while (d <= parse(props.data.yearEnd)) {
    const key = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-01`
    if (key >= props.data.yearStart)
      out.push({ key, date: key, label: MONTHS[d.getMonth()].slice(0, 3) })
    d.setMonth(d.getMonth() + 1)
  }
  return out
})
const aria = computed(
  () =>
    `O'quv yili ${props.data.yearStart} – ${props.data.yearEnd}` +
    (quarter.value
      ? `, ${quarter.value}-chorak, ${Math.round(done.value || 0)}% o'tdi`
      : '')
)
</script>

<style scoped>
.yl {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  padding-top: var(--space-5);
}

.yl__bar {
  position: relative;
  height: var(--year-h);
  border-radius: var(--radius-pill);
  background: var(--tile-none);
}

.yl__seg {
  position: absolute;
  top: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: color-mix(in srgb, var(--c) 70%, transparent);
  border-right: var(--hairline) solid var(--card-bg);
  overflow: hidden;
}

.yl__seg:first-child {
  border-radius: var(--radius-pill) 0 0 var(--radius-pill);
}

.yl__seg:nth-child(4) {
  border-radius: 0 var(--radius-pill) var(--radius-pill) 0;
}

.yl__seg-label {
  font-size: var(--text-xs);
  font-weight: 700;
  color: white;
  white-space: nowrap;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.25);
}

.yl__vac {
  position: absolute;
  top: calc(var(--space-1) * -1);
  bottom: calc(var(--space-1) * -1);
  border-radius: var(--radius-sm);
  background: repeating-linear-gradient(
    135deg,
    var(--year-vacation) 0 4px,
    color-mix(in srgb, var(--year-vacation) 55%, var(--card-bg)) 4px 8px
  );
  box-shadow: 0 0 0 var(--hairline) var(--card-bg);
}

.yl__done {
  position: absolute;
  inset: 0 auto 0 0;
  border-radius: var(--radius-pill) 0 0 var(--radius-pill);
  background: color-mix(in srgb, var(--card-bg) 45%, transparent);
  pointer-events: none;
}

.yl__here {
  position: absolute;
  top: calc(var(--space-2) * -1);
  bottom: calc(var(--space-2) * -1);
  width: var(--accent-bar);
  margin-left: calc(var(--accent-bar) / -2);
  border-radius: var(--radius-pill);
  background: var(--text-primary);
  box-shadow: 0 0 0 var(--hairline) var(--card-bg);
}

.yl__here-label {
  position: absolute;
  bottom: calc(100% + var(--space-1));
  left: 50%;
  transform: translateX(-50%);
  padding: 0 var(--space-2);
  border-radius: var(--radius-pill);
  background: var(--text-primary);
  color: var(--card-bg);
  font-size: var(--text-xs);
  font-weight: 700;
  white-space: nowrap;
}

.yl__months {
  position: relative;
  height: var(--space-4);
}

.yl__months span {
  position: absolute;
  top: 0;
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
}

.yl__note {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-sm);
  color: var(--brand-text-muted);
}

.yl__note b {
  color: var(--text-primary);
}

.yl__legend {
  margin-left: auto;
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  font-size: var(--text-xs);
}

.yl__key {
  display: inline-block;
  width: var(--space-4);
  height: var(--bar-h);
  border-radius: var(--radius-pill);
  margin-left: var(--space-2);
}

.yl__key--q {
  background: linear-gradient(
    90deg,
    var(--year-quarter-1),
    var(--year-quarter-2),
    var(--year-quarter-3),
    var(--year-quarter-4)
  );
}

.yl__key--v {
  background: var(--year-vacation);
}

@media (max-width: 599px) {
  .yl__months span:nth-child(even) {
    display: none;
  }
}
</style>

<template>
  <div class="yv" data-year-view>
    <button
      v-for="m in months"
      :key="m.key"
      type="button"
      class="yv__month dash-card"
      :data-month="m.key"
      @click="emit('month', m)"
    >
      <h4>{{ MONTHS[m.month] }} <small>{{ m.year }}</small></h4>
      <div class="yv__grid">
        <span v-for="d in DAYS_SHORT" :key="d" class="yv__wd">{{ d[0] }}</span>
        <template v-for="week in m.weeks" :key="week[0]">
          <span
            v-for="(date, k) in week"
            :key="date"
            class="yv__day"
            :class="{
              'yv__day--other': Number(date.slice(5, 7)) !== m.month + 1,
              'yv__day--today': date === today,
              'yv__day--sun': k === 6
            }"
            :style="dot(date)"
            :title="titles(date)"
            :data-day-events="count(date) || undefined"
            >{{ Number(date.slice(8)) }}</span
          >
        </template>
      </div>
    </button>
  </div>
</template>

<script setup>
// The school year as 12 small months (September first). A day with events has a dot in
// the first event's colour (a vacation tints the day); today has a ring. A month opens
// the month view.
import { computed } from 'vue'
import { DAYS_SHORT, EVENT_TYPES, MONTHS, monthWeeks } from './calendar'

const props = defineProps({
  // the first month shown (YYYY-MM-DD of any day in it)
  start: { type: String, required: true },
  events: { type: Array, required: true },
  today: { type: String, required: true }
})
const emit = defineEmits(['month'])

const months = computed(() => {
  const out = []
  let y = Number(props.start.slice(0, 4))
  let m = Number(props.start.slice(5, 7)) - 1
  for (let k = 0; k < 12; k++) {
    out.push({ key: `${y}-${String(m + 1).padStart(2, '0')}`, year: y, month: m, weeks: monthWeeks(y, m) })
    m++
    if (m > 11) {
      m = 0
      y++
    }
  }
  return out
})

const on = date => props.events.filter(e => e.startDate <= date && e.endDate >= date)
const count = date => on(date).length
function dot(date) {
  const list = on(date)
  if (!list.length) return undefined
  const vac = list.find(e => e.type === 'VACATION')
  const c = EVENT_TYPES[(vac || list[0]).type]?.color
  return vac ? { '--c': c, '--tint': '22%' } : { '--c': c, '--tint': '0%', '--dot': '1' }
}
function titles(date) {
  return on(date)
    .map(e => e.title)
    .join(', ')
}
</script>

<style scoped>
.yv {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(var(--grid-min-sm), 1fr));
  gap: var(--space-3);
}

.yv__month {
  padding: var(--space-3);
  border: 1px solid var(--dash-edge);
  text-align: left;
  font: inherit;
  cursor: pointer;
}

.yv__month h4 {
  margin: 0 0 var(--space-2);
  font-size: var(--text-sm);
  font-weight: 700;
}

.yv__month h4 small {
  color: var(--brand-text-muted);
  font-weight: 500;
}

.yv__grid {
  display: grid;
  grid-template-columns: repeat(7, var(--mini-cell));
  justify-content: space-between;
  gap: var(--hairline);
}

.yv__wd {
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
  text-align: center;
}

.yv__day {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: var(--mini-cell);
  border-radius: var(--radius-pill);
  font-size: var(--text-xs);
  font-variant-numeric: tabular-nums;
  background: color-mix(in srgb, var(--c, transparent) var(--tint, 0%), transparent);
}

.yv__day[style*='--dot']::after {
  content: '';
  position: absolute;
  bottom: var(--hairline);
  left: 50%;
  width: var(--space-1);
  height: var(--space-1);
  margin-left: calc(var(--space-1) / -2);
  border-radius: var(--radius-pill);
  background: var(--c);
}

.yv__day--other {
  visibility: hidden;
}

.yv__day--sun {
  color: var(--st-absent);
}

.yv__day--today {
  box-shadow: 0 0 0 2px var(--c-primary);
  color: var(--c-primary);
  font-weight: 700;
}
</style>

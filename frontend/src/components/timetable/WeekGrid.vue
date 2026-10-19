<template>
  <div
    class="wg"
    :class="{ 'wg--dragging': !!dragging }"
    :style="gridStyle"
    data-week-grid
    role="grid"
    :aria-label="label"
  >
    <div class="wg__corner">Dars</div>
    <div
      v-for="day in days"
      :key="day"
      class="wg__day"
      :class="{ 'wg__day--today': day === today }"
      role="columnheader"
      :data-day="day"
    >
      <span>{{ day }}</span>
      <span v-if="day === today" class="wg__today">Bugun</span>
    </div>

    <template v-for="row in rows" :key="row.key">
      <template v-if="row.kind === 'period'">
        <div class="wg__period" role="rowheader">
          <b>{{ row.period.n }}</b>
          <span>{{ hm(row.period.start) }}–{{ hm(row.period.end) }}</span>
        </div>
        <div
          v-for="day in days"
          :key="day + row.key"
          class="wg__cell"
          :class="cellClass(day, row.period)"
          :title="dragReason(day, row.period) || undefined"
          :data-cell="`${day}|${row.period.n}`"
          role="gridcell"
          @dragover="onDragOver($event, day, row.period)"
          @drop="onDrop($event, day, row.period)"
        >
          <LessonCard
            v-for="l in cell(day, row.period)"
            :key="l.id"
            :lesson="l"
            :mode="mode"
            :now-minutes="day === today ? nowMinutes : null"
            :dim="focusSubject !== null && l.subjectId !== focusSubject"
            :clash="cell(day, row.period).length > 1"
            :draggable="editable"
            :state="day === today ? states[l.id] || null : null"
            :substitute="day === today ? subs[l.id] || null : null"
            @open="emit('open', $event)"
            @dragstart="dragging = $event"
            @dragend="dragging = null"
          />
          <button
            v-if="editable && !dragging && !cell(day, row.period).length"
            type="button"
            class="wg__add"
            :aria-label="`${day}, ${row.period.n}-dars: dars qo'shish`"
            data-add-lesson
            @click="emit('add', { day, period: row.period })"
          >
            <q-icon name="add" />
          </button>
          <span
            v-if="dragging && dragReason(day, row.period)"
            class="wg__why"
            data-drop-reason
            >{{ dragReason(day, row.period) }}</span
          >
        </div>
      </template>
      <div v-else class="wg__break" :class="{ 'wg__break--long': row.long }">
        <span>☕ {{ row.minutes }} daq</span>
      </div>
    </template>
  </div>
</template>

<script setup>
// The week on one screen: columns are the days (today marked "Bugun"), rows are the
// lessons with thin break rows between them ("☕ 10 daq"). Rows share the height the page
// gives the grid, so the whole week fits without scrolling. In edit mode a lesson can be
// dragged: while it moves, every cell says whether it may go there — a cell where its
// teacher, room or class is busy turns red and says why, and refuses the drop. An empty
// cell offers "+" on hover.
import { computed, ref } from 'vue'
import LessonCard from './LessonCard.vue'
import { breaks, conflictReason, hm } from './timetable'

const props = defineProps({
  // the lessons this grid shows (one class / teacher / room)
  lessons: { type: Array, required: true },
  // every lesson of the school, for the drag check
  all: { type: Array, required: true },
  periods: { type: Array, required: true },
  days: { type: Array, required: true },
  today: { type: String, default: null },
  nowMinutes: { type: Number, default: null },
  mode: { type: String, default: 'class' },
  focusSubject: { type: Number, default: null },
  editable: { type: Boolean, default: false },
  states: { type: Object, default: () => ({}) },
  subs: { type: Object, default: () => ({}) },
  label: { type: String, default: 'Haftalik jadval' }
})
const emit = defineEmits(['open', 'move', 'add'])

const dragging = ref(null)

const rows = computed(() => {
  const gaps = new Map(breaks(props.periods).map(b => [b.after, b]))
  const out = []
  for (const p of props.periods) {
    out.push({ kind: 'period', key: `p${p.n}`, period: p })
    const g = gaps.get(p.n)
    if (g)
      out.push({ kind: 'break', key: `b${p.n}`, minutes: g.minutes, long: g.long })
  }
  return out
})

const gridStyle = computed(() => ({
  gridTemplateColumns: `var(--tt-period-w) repeat(${props.days.length}, minmax(0, 1fr))`,
  gridTemplateRows: [
    'var(--tt-head-h)',
    ...rows.value.map(r =>
      r.kind === 'period'
        ? 'minmax(var(--tt-row-min), 1fr)'
        : 'var(--tt-break-h)'
    )
  ].join(' ')
}))

const byCell = computed(() => {
  const m = new Map()
  for (const l of props.lessons) {
    const k = `${l.weekday}|${hm(l.start)}`
    if (!m.has(k)) m.set(k, [])
    m.get(k).push(l)
  }
  return m
})

function cell(day, period) {
  return byCell.value.get(`${day}|${hm(period.start)}`) || []
}

// the reason a dragged lesson may not land here (cached per drag)
const reasons = computed(() => {
  const m = new Map()
  if (!dragging.value) return m
  for (const day of props.days)
    for (const p of props.periods)
      m.set(`${day}|${p.n}`, conflictReason(props.all, dragging.value, day, p))
  return m
})

function dragReason(day, period) {
  return reasons.value.get(`${day}|${period.n}`) || null
}

function isOwnCell(day, period) {
  return (
    dragging.value &&
    dragging.value.weekday === day &&
    hm(dragging.value.start) === hm(period.start)
  )
}

function cellClass(day, period) {
  return {
    'wg__cell--today': day === props.today,
    'wg__cell--bad': !!dragging.value && !!dragReason(day, period),
    'wg__cell--ok':
      !!dragging.value && !dragReason(day, period) && !isOwnCell(day, period)
  }
}

function onDragOver(e, day, period) {
  if (!dragging.value || dragReason(day, period) || isOwnCell(day, period))
    return
  e.preventDefault()
  e.dataTransfer.dropEffect = 'move'
}

function onDrop(e, day, period) {
  e.preventDefault()
  const l = dragging.value
  dragging.value = null
  if (!l || conflictReason(props.all, l, day, period) || isOwnCell(day, period))
    return
  emit('move', { lesson: l, day, period })
}
</script>

<style scoped>
.wg {
  display: grid;
  gap: var(--space-1);
  height: 100%;
  min-height: 0;
}

.wg__corner,
.wg__day {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-xs);
  font-weight: 700;
  color: var(--brand-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.wg__day {
  justify-content: center;
  border-radius: var(--radius-sm);
}

.wg__day--today {
  color: var(--c-primary);
  background: color-mix(in srgb, var(--c-primary) 10%, transparent);
}

.wg__today {
  padding: 0 var(--space-2);
  border-radius: var(--radius-pill);
  background: var(--c-primary);
  color: white;
  font-size: var(--text-xs);
  letter-spacing: 0;
  text-transform: none;
}

.wg__period {
  display: flex;
  flex-direction: column;
  justify-content: center;
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
  font-variant-numeric: tabular-nums;
}

.wg__period b {
  color: var(--text-primary);
  font-size: var(--text-sm);
}

.wg__cell {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: var(--hairline);
  min-height: 0;
  border-radius: var(--radius-sm);
  background: var(--surface-2);
  transition: background var(--dur-fast) var(--ease-out);
}

.wg__cell > * {
  flex: 1 1 0;
  min-height: 0;
}

.wg__cell--today {
  background: color-mix(in srgb, var(--c-primary) 6%, var(--surface-2));
}

.wg__cell--ok {
  background: color-mix(in srgb, var(--st-present) 14%, transparent);
  outline: 1px dashed var(--st-present);
}

.wg__cell--bad {
  background: color-mix(in srgb, var(--st-absent) 14%, transparent);
  outline: 1px solid var(--st-absent);
  cursor: not-allowed;
}

.wg__why {
  position: absolute;
  inset: auto var(--space-1) var(--space-1);
  flex: none;
  padding: 0 var(--space-1);
  border-radius: var(--radius-sm);
  background: var(--st-absent);
  color: white;
  font-size: var(--text-xs);
  line-height: 1.4;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  pointer-events: none;
}

.wg__add {
  flex: 1 1 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px dashed transparent;
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--brand-text-muted);
  opacity: 0;
  cursor: pointer;
  transition: opacity var(--dur-fast) var(--ease-out);
}

.wg__cell:hover .wg__add,
.wg__add:focus-visible {
  opacity: 1;
  border-color: var(--c-border-control);
}

.wg__break {
  grid-column: 1 / -1;
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
  line-height: 1;
}

.wg__break::after {
  content: '';
  flex: 1 1 auto;
  border-top: 1px dashed var(--dash-edge);
}

.wg__break--long {
  color: var(--st-late);
  font-weight: 600;
}

@media (prefers-reduced-motion: reduce) {
  .wg__cell,
  .wg__add {
    transition: none;
  }
}
</style>

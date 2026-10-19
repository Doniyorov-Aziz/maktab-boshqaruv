<template>
  <div class="mv" data-month-view>
    <div class="mv__head">
      <span
        v-for="(d, k) in DAYS_SHORT"
        :key="d"
        :class="{ mv__sun: k === 6 }"
        >{{ d }}</span
      >
    </div>
    <div
      v-for="(week, w) in weeks"
      :key="week[0]"
      class="mv__week"
      :style="{
        gridTemplateRows: `var(--space-6) repeat(${layout[w].lanes}, var(--legend-chip-h)) minmax(0, 1fr)`
      }"
      @dragover="onDragOver($event)"
      @drop="onDrop($event, week)"
    >
      <!-- the day cells, behind everything -->
      <div
        v-for="(date, k) in week"
        :key="date"
        class="mv__day"
        :class="dayClass(date, k)"
        :style="{ gridColumn: k + 1 }"
        :data-date="date"
        @click="onDayClick(date, $event)"
      >
        <span class="mv__num" @click.stop="emit('day', date)">{{
          Number(date.slice(8))
        }}</span>
        <span
          v-if="heat(date)"
          class="mv__dot"
          :class="`mv__dot--${heat(date)}`"
          :title="`Davomat ${attendance[date].toFixed(1)}%`"
          :data-attendance="attendance[date]"
        />
      </div>

      <!-- multi-day events: one band across their days -->
      <div
        v-for="b in layout[w].bands"
        :key="b.event.id + week[0]"
        class="mv__band"
        :class="{
          'mv__band--cut-start': b.cutStart,
          'mv__band--cut-end': b.cutEnd
        }"
        :style="{
          gridColumn: `${b.col + 1} / span ${b.span}`,
          gridRow: b.lane + 2,
          '--c': EVENT_TYPES[b.event.type]?.color
        }"
        :draggable="editable"
        :title="title(b.event)"
        :data-event="b.event.id"
        data-band
        @dragstart="onDragStart($event, b.event, 'move')"
        @click.stop="emit('open', b.event)"
      >
        <q-icon :name="EVENT_TYPES[b.event.type]?.icon" />
        <span class="mv__band-text">{{ b.event.title }}</span>
        <span
          v-if="editable && !b.cutEnd"
          class="mv__grip"
          draggable="true"
          title="Cho'zish: chetidan sudrang"
          data-band-grip
          @dragstart.stop="onDragStart($event, b.event, 'resize')"
          @click.stop
        />
      </div>

      <!-- one-day events: pills, the rest behind "+N" -->
      <div
        v-for="(date, k) in week"
        :key="'p' + date"
        class="mv__pills"
        :style="{ gridColumn: k + 1, gridRow: layout[w].lanes + 2 }"
      >
        <button
          v-for="e in shown(date)"
          :key="e.id"
          type="button"
          class="mv__pill"
          :style="{ '--c': EVENT_TYPES[e.type]?.color }"
          :draggable="editable"
          :title="title(e)"
          :data-event="e.id"
          @dragstart="onDragStart($event, e, 'move')"
          @click.stop="emit('open', e)"
        >
          <span v-if="e.startTime" class="mv__time">{{
            e.startTime.slice(0, 5)
          }}</span>
          {{ e.title }}
        </button>
        <button
          v-if="hidden(date).length"
          type="button"
          class="mv__more"
          data-more
          @click.stop
        >
          +{{ hidden(date).length }}
          <q-popup-proxy class="mv__pop">
            <div class="mv__pop-list">
              <b>{{ Number(date.slice(8)) }}-sana</b>
              <button
                v-for="e in hidden(date)"
                :key="e.id"
                type="button"
                class="mv__pill"
                :style="{ '--c': EVENT_TYPES[e.type]?.color }"
                @click="emit('open', e)"
              >
                {{ e.title }}
              </button>
            </div>
          </q-popup-proxy>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
// A month as a grid, Monday first. Today has a ring in the brand colour; Sundays and
// holidays are striped (the holiday's own pill says its name). A vacation (any event over several days)
// is one band across its days. One-day events are pills in their type's colour; when more
// do not fit, "+2" opens the rest. A past school day shows its attendance as a small dot
// (the home page's colour steps). Editors drag an event to another day, stretch a band
// from its right edge, and click an empty day to add one.
import { computed } from 'vue'
import {
  DAYS_SHORT,
  EVENT_TYPES,
  dayEvents,
  daysOff,
  heatStep,
  monthWeeks,
  weekBands
} from './calendar'

const props = defineProps({
  year: { type: Number, required: true },
  month: { type: Number, required: true },
  events: { type: Array, required: true },
  // date → attendance %
  attendance: { type: Object, default: () => ({}) },
  today: { type: String, required: true },
  editable: { type: Boolean, default: false },
  maxPills: { type: Number, default: 2 }
})
const emit = defineEmits(['open', 'day', 'create', 'move', 'resize'])

const weeks = computed(() => monthWeeks(props.year, props.month))
const layout = computed(() => weeks.value.map(w => weekBands(w, props.events)))
const inMonth = date => Number(date.slice(5, 7)) === props.month + 1

function shown(date) {
  return dayEvents(date, props.events).slice(0, props.maxPills)
}
function hidden(date) {
  return dayEvents(date, props.events).slice(props.maxPills)
}
function heat(date) {
  if (date > props.today) return null
  return heatStep(props.attendance[date])
}
function dayClass(date, k) {
  return {
    'mv__day--other': !inMonth(date),
    'mv__day--today': date === props.today,
    'mv__day--off': k === 6 || daysOff(date, props.events).length > 0,
    'mv__day--past': date < props.today
  }
}
function title(e) {
  const t = EVENT_TYPES[e.type]?.label || ''
  const time = e.startTime
    ? ` · ${e.startTime.slice(0, 5)}–${(e.endTime || '').slice(0, 5)}`
    : ''
  return `${e.title} — ${t}${time}`
}

function onDayClick(date, ev) {
  if (ev.target !== ev.currentTarget) return
  if (props.editable) emit('create', date)
  else emit('day', date)
}

// ---------------------------------------------------------------- drag and drop
let drag = null
function onDragStart(ev, event, kind) {
  if (!props.editable) return
  drag = { event, kind }
  ev.dataTransfer.effectAllowed = 'move'
  ev.dataTransfer.setData('text/plain', `${kind}:${event.id}`)
}
function onDragOver(ev) {
  if (!drag) return
  ev.preventDefault()
  ev.dataTransfer.dropEffect = 'move'
}
function onDrop(ev, week) {
  ev.preventDefault()
  if (!drag) return
  const rect = ev.currentTarget.getBoundingClientRect()
  const k = Math.max(
    0,
    Math.min(6, Math.floor(((ev.clientX - rect.left) / rect.width) * 7))
  )
  const date = week[k]
  const { event, kind } = drag
  drag = null
  if (kind === 'resize') {
    if (date >= event.startDate && date !== event.endDate)
      emit('resize', { event, endDate: date })
  } else if (date !== event.startDate) {
    emit('move', { event, startDate: date })
  }
}
</script>

<style scoped>
.mv {
  display: flex;
  flex-direction: column;
  gap: var(--hairline);
  min-width: 0;
}

.mv__head {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  font-size: var(--text-xs);
  font-weight: 700;
  color: var(--brand-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.04em;
  padding: 0 var(--space-2) var(--space-1);
}

.mv__sun {
  color: var(--st-absent);
}

.mv__week {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  column-gap: var(--hairline);
  min-height: var(--month-cell-h);
}

.mv__day {
  grid-row: 1 / -1;
  position: relative;
  display: flex;
  align-items: flex-start;
  gap: var(--space-1);
  padding: var(--space-1) var(--space-2);
  border-radius: var(--radius-sm);
  background: var(--surface-2);
  cursor: pointer;
  transition: background var(--dur-fast) var(--ease-out);
}

.mv__day:hover {
  background: color-mix(in srgb, var(--c-primary) 6%, var(--surface-2));
}

.mv__day--other {
  opacity: 0.45;
}

.mv__day--off {
  background: repeating-linear-gradient(
    135deg,
    var(--surface-2) 0 6px,
    var(--tile-stripe) 6px 7px
  );
}

.mv__num {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: var(--space-6);
  height: var(--space-6);
  border-radius: var(--radius-pill);
  font-size: var(--text-sm);
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.mv__num:hover {
  background: var(--card-bg);
}

.mv__day--today .mv__num {
  box-shadow: 0 0 0 2px var(--c-primary);
  color: var(--c-primary);
  background: color-mix(in srgb, var(--c-primary) 10%, var(--card-bg));
}

.mv__day--today {
  background: color-mix(in srgb, var(--c-primary) 8%, var(--surface-2));
}

.mv__dot {
  position: absolute;
  top: var(--space-2);
  right: var(--space-2);
  width: var(--dot);
  height: var(--dot);
  border-radius: var(--radius-pill);
}

.mv__dot--1 {
  background: var(--heat-1);
}

.mv__dot--2 {
  background: var(--heat-2);
}

.mv__dot--3 {
  background: var(--heat-3);
}

.mv__dot--4 {
  background: var(--heat-4);
}

.mv__band {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: var(--space-1);
  margin: var(--hairline) var(--space-1);
  padding: 0 var(--space-2);
  border-radius: var(--radius-sm);
  background: color-mix(in srgb, var(--c) 85%, transparent);
  color: white;
  font-size: var(--text-xs);
  font-weight: 700;
  white-space: nowrap;
  overflow: hidden;
  cursor: pointer;
}

.mv__band--cut-start {
  margin-left: 0;
  border-top-left-radius: 0;
  border-bottom-left-radius: 0;
}

.mv__band--cut-end {
  margin-right: 0;
  border-top-right-radius: 0;
  border-bottom-right-radius: 0;
}

.mv__band-text {
  flex: 1 1 auto;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
}

.mv__grip {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  width: var(--space-2);
  cursor: ew-resize;
  background: rgba(255, 255, 255, 0.25);
}

.mv__pills {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  gap: var(--hairline);
  padding: 0 var(--space-1) var(--space-1);
  min-width: 0;
  pointer-events: none;
}

.mv__pills > * {
  pointer-events: auto;
}

.mv__pill {
  display: block;
  width: 100%;
  padding: 0 var(--space-2);
  border: 0;
  border-left: var(--accent-bar) solid var(--c);
  border-radius: var(--radius-sm);
  background: color-mix(in srgb, var(--c) 16%, var(--card-bg));
  color: var(--text-primary);
  font: inherit;
  font-size: var(--text-xs);
  font-weight: 600;
  line-height: 1.7;
  text-align: left;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  cursor: pointer;
}

.mv__pill:hover {
  background: color-mix(in srgb, var(--c) 26%, var(--card-bg));
}

.mv__time {
  color: var(--brand-text-muted);
  font-variant-numeric: tabular-nums;
}

.mv__more {
  align-self: flex-start;
  padding: 0 var(--space-2);
  border: 0;
  border-radius: var(--radius-pill);
  background: var(--card-bg);
  color: var(--c-primary);
  font: inherit;
  font-size: var(--text-xs);
  font-weight: 700;
  cursor: pointer;
}

.mv__pop-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  padding: var(--space-3);
  min-width: var(--field-w-sm);
}

@media (max-width: 599px) {
  .mv__week {
    min-height: var(--day-min-h-sm);
  }

  .mv__pill,
  .mv__band-text {
    font-size: 0;
  }

  .mv__pill {
    height: var(--bar-h);
    padding: 0;
  }
}
</style>

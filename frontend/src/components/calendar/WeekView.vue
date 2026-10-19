<template>
  <div class="wv" data-week-view>
    <div
      v-for="(date, k) in days"
      :key="date"
      class="wv__col"
      :class="{
        'wv__col--today': date === today,
        'wv__col--off': k === 6 || daysOff(date, events).length
      }"
      :data-date="date"
    >
      <button type="button" class="wv__head" @click="emit('day', date)">
        <span>{{ DAYS_SHORT[k] }}</span>
        <b>{{ Number(date.slice(8)) }}</b>
        <small v-if="date === today">Bugun</small>
      </button>
      <div class="wv__list">
        <button
          v-for="e in on(date)"
          :key="e.id"
          type="button"
          class="wv__ev"
          :style="{ '--c': EVENT_TYPES[e.type]?.color }"
          :data-event="e.id"
          @click="emit('open', e)"
        >
          <span class="wv__type">
            <q-icon :name="EVENT_TYPES[e.type]?.icon" />
            {{ e.startTime ? `${e.startTime.slice(0, 5)}–${(e.endTime || '').slice(0, 5)}` : 'kun bo\'yi' }}
          </span>
          <b>{{ e.title }}</b>
          <small v-if="e.roomNumber || e.location">📍 {{ e.roomNumber ? roomLabel(e.roomNumber) : e.location }}</small>
        </button>
        <button
          v-if="editable"
          type="button"
          class="wv__add"
          :aria-label="`${date}: tadbir qo'shish`"
          @click="emit('create', date)"
        >
          <q-icon name="add" />
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
// One week: a column per day (today marked), each event as a card with its time and place;
// multi-day events show on every day they cover. "+" adds one (editors).
import { computed } from 'vue'
import { DAYS_SHORT, EVENT_TYPES, addDays, daysOff } from './calendar'
import { roomLabel } from '@/components/timetable/timetable'

const props = defineProps({
  // the Monday of the week
  monday: { type: String, required: true },
  events: { type: Array, required: true },
  today: { type: String, required: true },
  editable: { type: Boolean, default: false }
})
const emit = defineEmits(['open', 'day', 'create'])

const days = computed(() => Array.from({ length: 7 }, (_, k) => addDays(props.monday, k)))
function on(date) {
  return props.events
    .filter(e => e.startDate <= date && e.endDate >= date)
    .sort((a, b) => (a.startTime || '').localeCompare(b.startTime || ''))
}
</script>

<style scoped>
.wv {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: var(--space-2);
  min-height: var(--panel-min-h);
}

.wv__col {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  padding: var(--space-2);
  border-radius: var(--radius-md);
  background: var(--surface-2);
  min-width: 0;
}

.wv__col--today {
  background: color-mix(in srgb, var(--c-primary) 8%, var(--surface-2));
  box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--c-primary) 40%, transparent);
}

.wv__col--off {
  background: repeating-linear-gradient(135deg, var(--surface-2) 0 6px, var(--tile-stripe) 6px 7px);
}

.wv__head {
  display: flex;
  align-items: baseline;
  gap: var(--space-2);
  border: 0;
  background: transparent;
  color: var(--brand-text-muted);
  font: inherit;
  font-size: var(--text-xs);
  font-weight: 700;
  text-transform: uppercase;
  cursor: pointer;
}

.wv__head b {
  font-size: var(--text-lg);
  color: var(--text-primary);
}

.wv__head small {
  color: var(--c-primary);
  text-transform: none;
}

.wv__list {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.wv__ev {
  display: flex;
  flex-direction: column;
  gap: var(--hairline);
  padding: var(--space-2);
  border: 0;
  border-left: var(--accent-bar) solid var(--c);
  border-radius: var(--radius-sm);
  background: color-mix(in srgb, var(--c) 12%, var(--card-bg));
  color: var(--text-primary);
  font: inherit;
  text-align: left;
  cursor: pointer;
}

.wv__ev:hover {
  background: color-mix(in srgb, var(--c) 22%, var(--card-bg));
}

.wv__type {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  font-size: var(--text-xs);
  color: var(--c);
  font-weight: 700;
}

.wv__ev b {
  font-size: var(--text-sm);
  line-height: 1.3;
}

.wv__ev small {
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
}

.wv__add {
  border: 1px dashed var(--c-border-control);
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--brand-text-muted);
  opacity: 0;
  cursor: pointer;
}

.wv__col:hover .wv__add,
.wv__add:focus-visible {
  opacity: 1;
}

@media (max-width: 1023px) {
  .wv {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 599px) {
  .wv {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>

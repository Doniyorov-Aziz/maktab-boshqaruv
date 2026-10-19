<template>
  <div class="ag" data-agenda-view>
    <EmptyState
      v-if="!groups.length"
      icon="event_available"
      title="Oldinda tadbir yo'q"
      :action="editable ? 'Tadbir qo\'shish' : ''"
      @action="emit('create', today)"
    />
    <section v-for="g in groups" :key="g.key" class="ag__month">
      <h4>{{ g.label }}</h4>
      <button
        v-for="e in g.events"
        :key="e.id"
        type="button"
        class="ag__row"
        :style="{ '--c': EVENT_TYPES[e.type]?.color }"
        :data-event="e.id"
        @click="emit('open', e)"
      >
        <span class="ag__date">
          <b>{{ Number(e.startDate.slice(8)) }}</b>
          <small>{{ MONTHS[Number(e.startDate.slice(5, 7)) - 1].slice(0, 3) }}</small>
        </span>
        <span class="ag__main">
          <b>{{ e.title }}</b>
          <small>
            <span class="ag__type"><q-icon :name="EVENT_TYPES[e.type]?.icon" /> {{ EVENT_TYPES[e.type]?.label }}</span>
            <template v-if="e.endDate !== e.startDate"> · {{ daysBetween(e.startDate, e.endDate) + 1 }} kun</template>
            <template v-if="e.startTime"> · {{ e.startTime.slice(0, 5) }}–{{ (e.endTime || '').slice(0, 5) }}</template>
            <template v-if="e.roomNumber || e.location"> · 📍 {{ e.roomNumber ? roomLabel(e.roomNumber) : e.location }}</template>
            <template v-if="e.notifyParents"> · 🔔 ota-onalarga</template>
          </small>
        </span>
        <span class="ag__left">{{ countdown(daysBetween(today, e.startDate)) }}</span>
      </button>
    </section>
  </div>
</template>

<script setup>
// What is coming, as a list: grouped by month, each event with its date block, type, length,
// time, place, whether parents are told, and how many days are left.
import { computed } from 'vue'
import EmptyState from '@/components/pages/EmptyState.vue'
import { EVENT_TYPES, MONTHS, countdown, daysBetween } from './calendar'
import { roomLabel } from '@/components/timetable/timetable'

const props = defineProps({
  events: { type: Array, required: true },
  today: { type: String, required: true },
  editable: { type: Boolean, default: false }
})
const emit = defineEmits(['open', 'create'])

const groups = computed(() => {
  const m = new Map()
  for (const e of [...props.events]
    .filter(x => x.endDate >= props.today)
    .sort((a, b) => a.startDate.localeCompare(b.startDate))) {
    const key = e.startDate.slice(0, 7)
    if (!m.has(key))
      m.set(key, {
        key,
        label: `${MONTHS[Number(key.slice(5)) - 1]} ${key.slice(0, 4)}`,
        events: []
      })
    m.get(key).events.push(e)
  }
  return [...m.values()]
})
</script>

<style scoped>
.ag {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.ag__month h4 {
  margin: 0 0 var(--space-2);
  font-size: var(--text-xs);
  font-weight: 700;
  color: var(--brand-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.ag__row {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  width: 100%;
  padding: var(--space-2);
  border: 0;
  border-radius: var(--radius-md);
  background: transparent;
  color: inherit;
  font: inherit;
  text-align: left;
  cursor: pointer;
}

.ag__row:hover {
  background: var(--surface-2);
}

.ag__date {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: var(--tile-lg);
  height: var(--tile-lg);
  flex-shrink: 0;
  border-radius: var(--radius-md);
  border: 1px solid color-mix(in srgb, var(--c) 45%, transparent);
  background: color-mix(in srgb, var(--c) 12%, transparent);
  color: var(--c);
  line-height: 1.05;
}

.ag__date b {
  font-size: var(--text-md);
}

.ag__date small {
  font-size: var(--text-xs);
  text-transform: uppercase;
}

.ag__main {
  flex: 1 1 auto;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.ag__main b {
  font-size: var(--text-sm);
}

.ag__main small {
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
}

.ag__type {
  color: var(--c);
  font-weight: 600;
}

.ag__left {
  flex-shrink: 0;
  font-size: var(--text-xs);
  font-weight: 700;
  color: var(--text-primary);
}
</style>

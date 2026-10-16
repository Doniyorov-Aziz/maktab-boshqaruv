<template>
  <router-link
    :to="to"
    class="heat"
    :class="`heat--${level}`"
    :data-class-id="tile.classId"
    :aria-label="ariaLabel"
  >
    <span class="heat__name">{{ tile.name }}</span>
    <span class="heat__value">{{ valueText }}</span>
    <span v-if="auto" class="heat__auto" aria-hidden="true">🤖</span>
    <q-tooltip
      anchor="top middle"
      self="bottom middle"
      :offset="[0, 6]"
      class="heat-tip"
    >
      <div class="text-weight-bold">{{ tile.name }} · {{ valueText }}</div>
      <div v-if="tile.marked">
        Keldi {{ tile.present }} · Kechikdi {{ tile.late }} · Kelmadi
        {{ tile.absent }} · Sababli {{ tile.excused }}
      </div>
      <div v-else-if="tile.hasLessons">Davomat hali olinmagan</div>
      <div v-else>Bugun darsi yo'q</div>
      <div v-if="tile.teacher">Sinf rahbari: {{ tile.teacher }}</div>
      <div v-if="tile.takenBy"
        >Oldi: {{ tile.takenBy }}, {{ formatClock(tile.takenAt) }}</div
      >
      <div v-if="tile.source"
        >Manba: {{ SOURCE[tile.source] || tile.source }}</div
      >
    </q-tooltip>
  </router-link>
</template>

<script setup>
// One class on the attendance map: name and %, coloured by the school's thresholds
// (≥ good dark green, ≥ ok green, ≥ warn yellow, below red), grey "⏳" when nobody
// took it yet, 🤖 in the corner when automatic marks are part of it.
import { computed } from 'vue'
import { formatPercent, formatClock } from '@/utils/format'

const props = defineProps({
  tile: { type: Object, required: true },
  thresholds: { type: Object, required: true },
  month: { type: String, required: true }
})

const SOURCE = {
  MANUAL: "Qo'lda",
  AUTO_CARRY: 'Avtomatik (oldingi darsdan)',
  DEMO_SIM: 'DEMO simulyatsiya',
  DEVICE: 'Qurilma',
  MIXED: "Qo'lda + avtomatik"
}

const level = computed(() => {
  const r = props.tile.rate
  if (r === null || r === undefined)
    return props.tile.hasLessons ? 'none' : 'off'
  if (r >= props.thresholds.good) return '1'
  if (r >= props.thresholds.ok) return '2'
  if (r >= props.thresholds.warn) return '3'
  return '4'
})
const valueText = computed(() =>
  props.tile.rate === null || props.tile.rate === undefined
    ? props.tile.hasLessons
      ? '⏳'
      : '—'
    : formatPercent(props.tile.rate)
)
const auto = computed(() =>
  ['AUTO_CARRY', 'DEMO_SIM', 'MIXED'].includes(props.tile.source)
)
const to = computed(() => ({
  path: '/attendance/calendar',
  query: {
    grade: props.tile.grade,
    class: props.tile.classId,
    month: props.month
  }
}))
const ariaLabel = computed(
  () =>
    `${props.tile.name}: ${valueText.value === '⏳' ? 'olinmagan' : valueText.value}`
)
</script>

<style scoped>
.heat {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  width: var(--heat-w);
  height: var(--heat-h);
  border-radius: var(--radius-sm);
  text-decoration: none;
  font-variant-numeric: tabular-nums;
  transition: transform var(--dur-fast) var(--ease-out);
}

.heat:hover,
.heat:focus-visible {
  transform: translateY(calc(var(--hairline) * -2));
  outline: none;
  box-shadow: 0 0 0 var(--space-1)
    color-mix(in srgb, var(--chart-primary) 35%, transparent);
}

.heat__name {
  font-size: var(--text-xs);
  font-weight: 700;
  line-height: 1.2;
}

.heat__value {
  font-size: var(--text-xs);
  line-height: 1.2;
}

.heat__auto {
  position: absolute;
  top: calc(var(--space-1) * -1);
  right: calc(var(--space-1) * -1);
  font-size: var(--text-xs);
  line-height: 1;
}

.heat--1 {
  background: var(--heat-1);
  color: var(--heat-1-text);
}
.heat--2 {
  background: var(--heat-2);
  color: var(--heat-2-text);
}
.heat--3 {
  background: var(--heat-3);
  color: var(--heat-3-text);
}
.heat--4 {
  background: var(--heat-4);
  color: var(--heat-4-text);
}
.heat--none {
  background: var(--heat-none);
  color: var(--heat-none-text);
}
.heat--off {
  background: transparent;
  color: var(--heat-none-text);
  border: var(--hairline) dashed var(--c-border-control);
}

@media (prefers-reduced-motion: reduce) {
  .heat {
    transition: none;
  }
}
</style>

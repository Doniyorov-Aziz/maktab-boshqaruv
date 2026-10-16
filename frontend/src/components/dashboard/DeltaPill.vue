<template>
  <span
    v-if="text"
    :class="['delta-pill', `delta-pill--${tone}`]"
    :title="title"
  >
    <span aria-hidden="true">{{ arrow }}</span> {{ text }}
  </span>
</template>

<script setup>
// ▲ +2.1% (green) / ▼ −1.4% (red). Whether "up" is good depends on the measure:
// more attendance is good, more absences is not — higherIsBetter says which.
import { computed } from 'vue'
import { formatDelta } from '@/utils/format'

const props = defineProps({
  value: { type: Number, default: null },
  higherIsBetter: { type: Boolean, default: true },
  percent: { type: Boolean, default: false },
  digits: { type: Number, default: 1 },
  title: { type: String, default: '' }
})

const text = computed(() =>
  props.value === null || props.value === undefined
    ? ''
    : formatDelta(props.value, { percent: props.percent, digits: props.digits })
)
const arrow = computed(() =>
  props.value > 0 ? '▲' : props.value < 0 ? '▼' : '•'
)
const tone = computed(() => {
  if (!props.value) return 'flat'
  const good = props.value > 0 === props.higherIsBetter
  return good ? 'good' : 'bad'
})
</script>

<style scoped>
.delta-pill {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  padding: 0 var(--space-2);
  border-radius: var(--radius-sm);
  font-size: var(--text-xs);
  font-weight: 600;
  line-height: 1.8;
  white-space: nowrap;
}

.delta-pill--good {
  color: var(--st-present);
  background: color-mix(in srgb, var(--st-present) 14%, transparent);
}

.delta-pill--bad {
  color: var(--st-absent);
  background: color-mix(in srgb, var(--st-absent) 14%, transparent);
}

.delta-pill--flat {
  color: var(--brand-text-muted);
  background: var(--surface-2);
}
</style>

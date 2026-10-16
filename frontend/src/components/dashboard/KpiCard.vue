<template>
  <component
    :is="to ? 'router-link' : 'div'"
    :to="to || undefined"
    class="kpi brand-card"
    :class="{ 'kpi--link': !!to }"
    :data-kpi="kpiKey"
  >
    <div class="kpi__top">
      <div class="kpi__name">
        <IconTile :icon="icon" size="40" :color="tileBg" :text-color="color" />
        <span>{{ label }}</span>
      </div>
      <DeltaPill
        :value="delta"
        :higher-is-better="higherIsBetter"
        :percent="deltaPercent"
        :digits="deltaDigits"
        :title="deltaTitle"
      />
    </div>
    <div v-if="loading && value === undefined" class="kpi__value">
      <q-skeleton type="text" width="60%" />
    </div>
    <div v-else class="kpi__value" :class="{ 'kpi__value--none': emptyValue }">
      {{ display }}
    </div>
    <div class="kpi__note">
      <slot name="note">{{ note }}</slot>
    </div>
    <Sparkline
      class="kpi__spark"
      :values="spark || []"
      :color="color"
      :min="sparkMin"
      :max="sparkMax"
      :label="`${label}: oxirgi 14 ish kuni`"
    />
  </component>
</template>

<script setup>
// One KPI: tile + name, the change (pill), a big number, a note and a 14-school-day
// sparkline. Clicking goes to the page behind the number (to).
import { computed } from 'vue'
import IconTile from '@/components/app/IconTile.vue'
import DeltaPill from './DeltaPill.vue'
import Sparkline from './Sparkline.vue'

const props = defineProps({
  kpiKey: { type: String, default: '' },
  label: { type: String, required: true },
  icon: { type: String, required: true },
  color: { type: String, default: 'var(--chart-primary)' },
  value: { type: [Number, String], default: undefined },
  display: { type: String, default: '' },
  emptyValue: { type: Boolean, default: false },
  note: { type: String, default: '' },
  delta: { type: Number, default: null },
  higherIsBetter: { type: Boolean, default: true },
  deltaPercent: { type: Boolean, default: false },
  deltaDigits: { type: Number, default: 1 },
  deltaTitle: { type: String, default: '' },
  spark: { type: Array, default: null },
  sparkMin: { type: Number, default: null },
  sparkMax: { type: Number, default: null },
  to: { type: [String, Object], default: null },
  loading: { type: Boolean, default: false }
})

const tileBg = computed(
  () => `color-mix(in srgb, ${props.color} 14%, transparent)`
)
</script>

<style scoped>
.kpi {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  padding: var(--space-4);
  min-width: 0;
  height: 100%;
  color: inherit;
  text-decoration: none;
}

.kpi--link {
  cursor: pointer;
  transition:
    box-shadow var(--dur) var(--ease-out),
    transform var(--dur) var(--ease-out);
}

.kpi--link:hover {
  box-shadow: var(--shadow-card-hover);
}

.kpi--link:focus-visible {
  outline: var(--border-focus);
  outline-offset: var(--hairline);
}

.kpi__top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-2);
}

.kpi__name {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--brand-text-muted);
  min-width: 0;
}

.kpi__value {
  font-size: var(--kpi-value);
  font-weight: 700;
  line-height: 1.1;
  letter-spacing: -0.02em;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.kpi__value--none {
  font-size: var(--text-lg);
  color: var(--brand-text-muted);
  font-weight: 600;
  line-height: calc(var(--kpi-value) * 1.1);
}

.kpi__note {
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
  min-height: calc(var(--text-xs) * 1.5);
}

.kpi__spark {
  margin-top: auto;
}

@media (prefers-reduced-motion: reduce) {
  .kpi--link {
    transition: none;
  }
}
</style>

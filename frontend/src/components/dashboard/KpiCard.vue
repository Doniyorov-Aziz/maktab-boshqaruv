<template>
  <component
    :is="to ? 'router-link' : 'div'"
    :to="to || undefined"
    class="kpi dash-card"
    :class="{
      'kpi--link': !!to || clickable,
      'kpi--compact': compact,
      'kpi--mini': mini
    }"
    :data-kpi="kpiKey"
    :aria-busy="loading"
    :role="clickable && !to ? 'button' : undefined"
    :tabindex="clickable && !to ? 0 : undefined"
    @click="clickable && !to && emit('click')"
    @keydown.enter="clickable && !to && emit('click')"
  >
    <template v-if="loading && value === undefined">
      <div class="kpi__head">
        <div class="skel kpi__skel-icon" />
        <div class="skel kpi__skel-name" />
      </div>
      <div class="skel kpi__skel-value" />
      <div class="skel kpi__skel-spark" />
    </template>
    <template v-else>
      <div class="kpi__head">
        <IconTile
          v-if="!mini"
          :icon="icon"
          size="32"
          :color="`color-mix(in srgb, ${color} 14%, transparent)`"
          :text-color="color"
        />
        <span class="kpi__name">{{ label }}</span>
        <DeltaPill
          class="kpi__delta"
          :value="delta"
          :higher-is-better="higherIsBetter"
          :percent="deltaPercent"
          :digits="deltaDigits"
          :title="deltaTitle"
        />
      </div>
      <div
        class="kpi__value"
        :class="{ 'kpi__value--empty': emptyValue }"
        :style="valueColor ? { color: valueColor } : null"
      >
        <span v-if="emptyValue" class="kpi__empty">{{ emptyText }}</span>
        <CountUp v-else :value="value" :format="format" />
      </div>
      <div v-if="!mini" class="kpi__note">
        <slot name="note">{{ note }}</slot>
      </div>
      <Sparkline
        class="kpi__spark"
        :large="!compact && !mini"
        :values="spark || []"
        :color="color"
        :min="sparkMin"
        :max="sparkMax"
        :label="sparkLabel || `${label}: oxirgi 14 ish kuni`"
      />
    </template>
  </component>
</template>

<script setup>
// One hero number: name with its icon tile, the change (▲/▼), the big value counting
// up, a note, and a smooth 14 school-day sparkline. The whole card links to the page
// (with its filter) the number comes from.
import IconTile from '@/components/app/IconTile.vue'
import DeltaPill from './DeltaPill.vue'
import Sparkline from './Sparkline.vue'
import CountUp from './CountUp.vue'

defineProps({
  kpiKey: { type: String, required: true },
  label: { type: String, required: true },
  icon: { type: String, required: true },
  color: { type: String, default: 'var(--chart-primary)' },
  value: { type: Number, default: undefined },
  format: { type: Function, default: v => String(Math.round(v)) },
  emptyValue: { type: Boolean, default: false },
  emptyText: { type: String, default: '—' },
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
  loading: { type: Boolean, default: false },
  // a card that opens something on the page (a list in a side panel) rather than a link
  clickable: { type: Boolean, default: false },
  // a lower card (smaller number and sparkline) for pages that need the room below
  compact: { type: Boolean, default: false },
  // the smallest: no icon and no note, a tile inside another card (journal panel)
  mini: { type: Boolean, default: false },
  sparkLabel: { type: String, default: '' },
  valueColor: { type: String, default: '' }
})
const emit = defineEmits(['click'])
</script>

<style scoped>
.kpi {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  padding: var(--space-4) var(--space-5) var(--space-3);
  color: inherit;
  text-decoration: none;
  overflow: hidden;
}

.kpi--link {
  cursor: pointer;
}

.kpi--link:focus-visible {
  outline: var(--border-focus);
  outline-offset: var(--hairline);
}

.kpi__head {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  min-width: 0;
}

.kpi__name {
  flex: 1 1 auto;
  min-width: 0;
  font-size: var(--text-sm);
  font-weight: 500;
  color: var(--brand-text-muted);
  line-height: 1.25;
}

.kpi__delta {
  flex-shrink: 0;
}

.kpi__value {
  font-size: var(--kpi-value-xl);
  margin-top: var(--space-1);
}

.kpi__value--empty {
  font-size: var(--text-xl);
}

.kpi__empty {
  color: var(--brand-text-muted);
  font-weight: 600;
  letter-spacing: -0.01em;
}

.kpi__note {
  min-height: calc(var(--text-xs) * 1.5);
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
}

.kpi__spark {
  margin-top: auto;
}

.kpi--compact {
  gap: var(--space-1);
  padding: var(--space-3) var(--space-4) var(--space-2);
}

.kpi--compact .kpi__value {
  font-size: var(--kpi-value-md);
  margin-top: 0;
}

.kpi--compact .kpi__note {
  min-height: 0;
}

.kpi--mini {
  gap: var(--hairline);
  padding: var(--space-2) var(--space-3);
  border: 0;
  border-radius: var(--radius-md);
  background: var(--surface-2);
  box-shadow: none;
}

.kpi--mini::before {
  display: none;
}

.kpi--mini:hover {
  transform: none;
  box-shadow: none;
}

.kpi--mini .kpi__name {
  font-size: var(--text-xs);
  white-space: nowrap;
}

.kpi--mini .kpi__value {
  font-size: var(--text-lg);
  font-weight: 700;
  margin-top: 0;
  line-height: 1.2;
}

.kpi--mini .kpi__spark {
  --spark-h: var(--space-5);
}

.kpi__skel-icon {
  width: var(--tile-sm);
  height: var(--tile-sm);
}

.kpi__skel-name {
  flex: 1 1 auto;
  height: var(--space-4);
}

.kpi__skel-value {
  width: 55%;
  height: var(--kpi-value-xl);
  margin-top: var(--space-1);
}

.kpi__skel-spark {
  height: var(--spark-h-lg);
  margin-top: auto;
}
</style>

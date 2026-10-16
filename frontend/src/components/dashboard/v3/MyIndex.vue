<template>
  <ChartCard
    title="Shaxsiy indeksim"
    :subtitle="data ? `Oxirgi ${data.days} ish kuni` : ''"
    :as-of="asOf"
    :loading="loading"
    :error="error"
    :data="data"
    :empty="!!data && data.index === null"
    skeleton="ring"
    data-widget="my-index"
    @retry="reload"
  >
    <template #title-extra>
      <q-icon name="info" class="mi__info" tabindex="0" aria-label="Formula">
        <q-tooltip max-width="300px">
          Indeks = darslaringizdagi davomat × 50% + jurnalingiz to'liqligi ×
          50%. Jurnal to'liqligi: har bir o'tgan darsda davomat kiritilgan va
          kamida bitta baho qo'yilgan bo'lsa — 100%.
        </q-tooltip>
      </q-icon>
    </template>
    <div v-if="data" class="mi">
      <MultiRing :main="main" :rings="rings">
        <CountUp class="mi__index" :value="data.index" />
        <span class="mi__label">{{ data.label }}</span>
      </MultiRing>
      <ul class="mi__legend">
        <li v-for="r in rings" :key="r.key">
          <span class="mi__dot" :style="{ background: r.color }" />
          <span class="mi__name">{{ r.label }}</span>
          <b>{{ formatPercent(r.value) }}</b>
        </li>
        <li class="mi__delta">
          <DeltaPill :value="data.delta" :digits="0" />
          <span v-if="data.delta !== null">o'tgan haftaga nisbatan</span>
        </li>
      </ul>
    </div>
  </ChartCard>
</template>

<script setup>
// 9 — the teacher's own index: attendance in their lessons and how complete their
// journal is, half each, against the week before.
import { computed } from 'vue'
import ChartCard from '../ChartCard.vue'
import MultiRing from '../MultiRing.vue'
import CountUp from '../CountUp.vue'
import DeltaPill from '../DeltaPill.vue'
import { useWidget } from '@/composables/useWidget'
import { formatPercent } from '@/utils/format'

const { data, asOf, loading, error, reload } = useWidget('my-index')

const main = computed(() => {
  const i = data.value?.index ?? null
  const color =
    i === null
      ? 'var(--st-unmarked)'
      : i >= 85
        ? 'var(--st-present)'
        : i >= 70
          ? 'var(--gr-3)'
          : 'var(--st-absent)'
  return { key: 'index', label: 'Indeks', value: i, color }
})
const rings = computed(() => [
  {
    key: 'attendance',
    label: 'Darslarimdagi davomat',
    value: data.value?.attendance ?? null,
    color: 'var(--ring-attendance)'
  },
  {
    key: 'journal',
    label: "Jurnal to'liqligi",
    value: data.value?.journal ?? null,
    color: 'var(--ring-journal)'
  }
])
</script>

<style scoped>
.mi {
  display: flex;
  align-items: center;
  gap: var(--space-5);
  flex-wrap: wrap;
}

.mi__index {
  font-size: var(--kpi-value-xl);
}

.mi__label {
  margin-top: var(--space-1);
  font-size: var(--text-xs);
  font-weight: 600;
  color: var(--brand-text-muted);
}

.mi__legend {
  list-style: none;
  margin: 0;
  padding: 0;
  flex: 1 1 12rem;
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  font-size: var(--text-sm);
}

.mi__legend li {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.mi__dot {
  width: var(--legend-dot);
  height: var(--legend-dot);
  border-radius: var(--radius-pill);
}

.mi__name {
  flex: 1 1 auto;
  color: var(--brand-text-muted);
}

.mi__delta {
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
}

.mi__info {
  font-size: var(--text-md);
  color: var(--brand-text-muted);
  cursor: help;
}
</style>

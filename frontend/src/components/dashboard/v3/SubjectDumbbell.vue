<template>
  <ChartCard
    title="Fanlar: o'zlashtirish va sifat"
    subtitle="Eng past sifat tepada"
    :as-of="asOf"
    :loading="loading"
    :error="error"
    :data="data"
    :empty="!!data && !rows.length"
    :skeleton-rows="6"
    :tabs="tabs"
    :tab="tab"
    scroll
    data-widget="subjects"
    @update:tab="v => (tab = v)"
    @retry="reload"
  >
    <div class="db__legend">
      <span><i class="db__dot db__dot--m" />O'zlashtirish (3–5)</span>
      <span><i class="db__dot db__dot--q" />Sifat (4–5)</span>
    </div>
    <ul class="db">
      <li v-for="r in rows" :key="r.subjectId" class="db__row">
        <span class="db__name" :title="r.subject">{{ r.subject }}</span>
        <span class="db__track" :title="`${r.total} ta baho`">
          <span class="db__axis" />
          <span
            class="db__bar"
            :style="{
              left: `${r.quality}%`,
              width: `${Math.max(0, r.mastery - r.quality)}%`
            }"
          />
          <span class="db__pt db__pt--m" :style="{ left: `${r.mastery}%` }" />
          <span class="db__pt db__pt--q" :style="{ left: `${r.quality}%` }" />
        </span>
        <span class="db__vals">
          <b>{{ formatPercent(r.quality) }}</b>
          <small>{{ formatPercent(r.mastery) }}</small>
        </span>
      </li>
    </ul>
    <div class="db__scale" aria-hidden="true">
      <span />
      <span class="db__scale-axis"><i>0</i><i>50%</i><i>100%</i></span>
      <span />
    </div>
  </ChartCard>
</template>

<script setup>
// 6.7 — every subject on one 0–100 % axis: mastery (faint dot) and quality (bright dot)
// joined by a line, so the gap between "passed" and "good" is visible at a glance. The
// lowest quality is on top; the period is this quarter or this month.
import { computed, ref } from 'vue'
import ChartCard from '../ChartCard.vue'
import { useWidget } from '@/composables/useWidget'
import { formatPercent } from '@/utils/format'

const tabs = [
  { label: 'Chorak', value: 'quarter' },
  { label: 'Oy', value: 'month' }
]
const tab = ref('quarter')
const params = computed(() => ({ period: tab.value }))
const { data, asOf, loading, error, reload } = useWidget('subject-quality', {
  params
})

const rows = computed(() =>
  (data.value || []).filter(r => r.mastery !== null && r.quality !== null)
)
</script>

<style scoped>
.db__legend {
  display: flex;
  gap: var(--space-4);
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
  margin-bottom: var(--space-2);
}

.db__legend span {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
}

.db__dot {
  display: inline-block;
  width: var(--dumbbell-dot);
  height: var(--dumbbell-dot);
  border-radius: var(--radius-pill);
}

.db__dot--m {
  background: color-mix(in srgb, var(--ring-quality) 35%, transparent);
  border: 1px solid var(--ring-quality);
}

.db__dot--q {
  background: var(--ring-quality);
}

.db {
  list-style: none;
  margin: 0;
  padding: 0;
}

.db__row,
.db__scale {
  display: grid;
  grid-template-columns: minmax(0, 9rem) minmax(0, 1fr) 4.5rem;
  align-items: center;
  gap: var(--space-3);
}

.db__row {
  height: var(--dumbbell-h);
  margin-bottom: var(--space-2);
}

.db__name {
  font-size: var(--text-sm);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.db__track {
  position: relative;
  height: 100%;
}

.db__axis {
  position: absolute;
  left: 0;
  right: 0;
  top: 50%;
  height: var(--hairline);
  background: var(--ring-track);
}

.db__bar {
  position: absolute;
  top: 50%;
  height: var(--accent-bar);
  transform: translateY(-50%);
  background: color-mix(in srgb, var(--ring-quality) 45%, transparent);
  border-radius: var(--radius-pill);
}

.db__pt {
  position: absolute;
  top: 50%;
  width: var(--dumbbell-dot);
  height: var(--dumbbell-dot);
  border-radius: var(--radius-pill);
  transform: translate(-50%, -50%);
}

.db__pt--m {
  background: color-mix(in srgb, var(--ring-quality) 35%, var(--card-bg));
  border: 1px solid var(--ring-quality);
}

.db__pt--q {
  background: var(--ring-quality);
  box-shadow: 0 0 0 var(--accent-bar)
    color-mix(in srgb, var(--ring-quality) 18%, transparent);
}

.db__vals {
  display: flex;
  align-items: baseline;
  justify-content: flex-end;
  gap: var(--space-1);
  font-variant-numeric: tabular-nums;
}

.db__vals b {
  font-size: var(--text-sm);
  font-weight: 600;
}

.db__vals small {
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
}

.db__scale-axis {
  display: flex;
  justify-content: space-between;
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
  font-style: normal;
}

.db__scale-axis i {
  font-style: normal;
}
</style>

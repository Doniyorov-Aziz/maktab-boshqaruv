<template>
  <ChartCard
    title="Jurnal to'liqligi"
    :subtitle="subtitle"
    :as-of="asOf"
    :loading="loading"
    :error="error"
    :data="data"
    :empty="!!data && !data.teachers.length"
    :skeleton-rows="5"
    data-widget="journal"
    @retry="reload"
  >
    <template #title-extra>
      <q-icon
        name="info"
        class="jc__info"
        tabindex="0"
        aria-label="Qanday hisoblanadi"
      >
        <q-tooltip max-width="320px">
          Har bir o'tgan dars uchun: davomat kiritilganmi va kamida bitta baho
          qo'yilganmi. To'liqlik = (davomatli darslar + bahoga ega darslar) / (2
          × darslar). Avtomatik ko'chirilgan davomat hisoblanmaydi.
        </q-tooltip>
      </q-icon>
    </template>
    <div v-if="data" class="jc">
      <div class="jc__school">
        Maktab bo'yicha
        <b class="dash-num">{{ formatPercent(data.school) }}</b>
      </div>
      <ul class="jc__list">
        <li v-for="t in lowest" :key="t.employeeId">
          <component
            :is="readonly ? 'div' : 'router-link'"
            :to="readonly ? undefined : `/profiles/teacher/${t.employeeId}`"
            class="jc__row"
          >
            <span class="jc__name" :title="t.name">{{ t.name }}</span>
            <span
              class="jc__bullet"
              :title="`davomat ${formatPercent(t.attendedPct)} · baho ${formatPercent(t.gradedPct)} · ${t.lessons} dars`"
            >
              <span class="jc__target" />
              <span
                :class="['jc__fill', `jc__fill--${tone(t.completeness)}`]"
                :style="{ width: `${t.completeness ?? 0}%` }"
              />
            </span>
            <b class="jc__val">{{ formatPercent(t.completeness) }}</b>
          </component>
        </li>
      </ul>
      <div v-if="data.teachers.length > lowest.length" class="jc__more">
        Yana {{ data.teachers.length - lowest.length }} o'qituvchi —
        {{ fullCount }} tasida 95%+
      </div>
    </div>
  </ChartCard>
</template>

<script setup>
// 6.10 — whose journal is least complete over the last 7 school days: the five lowest
// teachers as bullet bars against a 95 % mark.
import { computed } from 'vue'
import ChartCard from '../ChartCard.vue'
import { useWidget } from '@/composables/useWidget'
import { formatPercent } from '@/utils/format'

defineProps({ readonly: { type: Boolean, default: false } })
const { data, asOf, loading, error, reload } = useWidget('journal')

const subtitle = computed(() =>
  data.value ? `Oxirgi ${data.value.days} ish kuni · eng past 5` : ''
)
const lowest = computed(() => (data.value?.teachers || []).slice(0, 5))
const fullCount = computed(
  () =>
    (data.value?.teachers || []).filter(t => (t.completeness ?? 0) >= 95).length
)
const tone = v =>
  v === null || v === undefined
    ? 'none'
    : v >= 95
      ? 'good'
      : v >= 80
        ? 'warn'
        : 'bad'
</script>

<style scoped>
.jc {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.jc__school {
  font-size: var(--text-sm);
  color: var(--brand-text-muted);
  display: flex;
  align-items: baseline;
  gap: var(--space-2);
}

.jc__school b {
  font-size: var(--text-lg);
  color: var(--text-primary);
}

.jc__list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.jc__row {
  display: grid;
  grid-template-columns: minmax(0, 10rem) minmax(0, 1fr) 3.5rem;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-1) var(--space-2);
  margin: 0 calc(var(--space-2) * -1);
  border-radius: var(--radius-sm);
  color: inherit;
  text-decoration: none;
}

a.jc__row:hover {
  background: var(--surface-2);
}

.jc__name {
  font-size: var(--text-sm);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.jc__bullet {
  position: relative;
  height: var(--bullet-h);
  border-radius: var(--radius-pill);
  background: var(--ring-track);
}

.jc__fill {
  position: absolute;
  inset: 0 auto 0 0;
  border-radius: var(--radius-pill);
  transition: width var(--dur-ring) var(--ease-out);
}

.jc__fill--good {
  background: var(--st-present);
}

.jc__fill--warn {
  background: var(--gr-3);
}

.jc__fill--bad {
  background: var(--st-absent);
}

.jc__target {
  position: absolute;
  left: 95%;
  top: calc(var(--space-1) * -1);
  bottom: calc(var(--space-1) * -1);
  width: var(--hairline);
  background: var(--chart-target);
  z-index: 1;
}

.jc__val {
  text-align: right;
  font-size: var(--text-sm);
  font-variant-numeric: tabular-nums;
}

.jc__more {
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
}

.jc__info {
  font-size: var(--text-md);
  color: var(--brand-text-muted);
  cursor: help;
}

@media (prefers-reduced-motion: reduce) {
  .jc__fill {
    transition: none;
  }
}
</style>

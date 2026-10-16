<template>
  <ChartCard
    title="Maktab salomatlik indeksi"
    :subtitle="data ? `Oxirgi ${data.days} ish kuni` : ''"
    :as-of="asOf"
    :loading="loading"
    :error="error"
    :data="data"
    :empty="!!data && data.index === null"
    skeleton="ring"
    data-widget="health"
    @retry="reload"
  >
    <template #title-extra>
      <q-icon
        name="info"
        class="health__info"
        tabindex="0"
        aria-label="Formula"
      >
        <q-tooltip class="health__tip" max-width="320px">
          <b>Indeks (0–100)</b> = davomat × {{ w.attendance }}% + sifat ×
          {{ w.quality }}% + jurnal to'liqligi × {{ w.journal }}% + ota-onalar
          Telegramda × {{ w.telegram }}%.<br />
          Har biri 0–100. Ma'lumoti yo'q qism hisobga olinmaydi, qolganlar
          og'irligi bo'yicha qayta taqsimlanadi. Og'irliklar — Maktab
          sozlamalarida.
        </q-tooltip>
      </q-icon>
    </template>

    <div v-if="data" class="health">
      <MultiRing :main="main" :rings="rings">
        <CountUp class="health__index" :value="data.index" />
        <span :class="['health__label', `health__label--${tone}`]">{{
          data.label
        }}</span>
      </MultiRing>
      <ul class="health__legend">
        <li v-for="r in rings" :key="r.key">
          <span class="health__dot" :style="{ background: r.color }" />
          <span class="health__name">{{ r.label }}</span>
          <span class="health__val">{{ formatPercent(r.value) }}</span>
        </li>
      </ul>
    </div>

    <template v-if="data && data.index !== null" #footer>
      <span class="health__foot">
        <DeltaPill
          :value="data.delta"
          :digits="0"
          title="Oldingi 7 ish kuniga nisbatan"
        />
        <span v-if="data.delta !== null">o'tgan haftaga nisbatan</span>
        <span v-else>o'tgan hafta uchun ma'lumot yo'q</span>
      </span>
    </template>
  </ChartCard>
</template>

<script setup>
// 6.1 — one number for "how is the school doing": attendance, quality, journal
// completeness and parents on Telegram, weighted (formula in the tooltip, weights in
// the school settings). The four parts are the thin rings inside the index ring.
import { computed } from 'vue'
import ChartCard from '../ChartCard.vue'
import MultiRing from '../MultiRing.vue'
import CountUp from '../CountUp.vue'
import DeltaPill from '../DeltaPill.vue'
import { useWidget } from '@/composables/useWidget'
import { formatPercent } from '@/utils/format'

const { data, asOf, loading, error, reload } = useWidget('health')

const w = computed(
  () =>
    data.value?.weights || {
      attendance: 40,
      quality: 30,
      journal: 20,
      telegram: 10
    }
)
const tone = computed(() => {
  const i = data.value?.index
  if (i === null || i === undefined) return 'none'
  return i >= 85 ? 'good' : i >= 70 ? 'ok' : 'bad'
})
const TONE_COLOR = {
  good: 'var(--st-present)',
  ok: 'var(--gr-3)',
  bad: 'var(--st-absent)',
  none: 'var(--st-unmarked)'
}
const main = computed(() => ({
  key: 'index',
  label: 'Indeks',
  value: data.value?.index ?? null,
  color: TONE_COLOR[tone.value]
}))
const rings = computed(() => {
  const p = data.value?.parts || {}
  return [
    {
      key: 'attendance',
      label: 'Davomat',
      value: p.attendance ?? null,
      color: 'var(--ring-attendance)'
    },
    {
      key: 'quality',
      label: 'Sifat',
      value: p.quality ?? null,
      color: 'var(--ring-quality)'
    },
    {
      key: 'journal',
      label: "Jurnal to'liqligi",
      value: p.journal ?? null,
      color: 'var(--ring-journal)'
    },
    {
      key: 'telegram',
      label: 'Ota-onalar Telegramda',
      value: p.telegram ?? null,
      color: 'var(--ring-telegram)'
    }
  ]
})
</script>

<style scoped>
.health {
  display: flex;
  align-items: center;
  gap: var(--space-5);
  height: 100%;
}

.health__index {
  font-size: var(--kpi-value-xl);
}

.health__label {
  margin-top: var(--space-1);
  font-size: var(--text-xs);
  font-weight: 600;
}

.health__label--good {
  color: var(--st-present);
}

.health__label--ok {
  color: var(--gr-3);
}

.health__label--bad {
  color: var(--st-absent);
}

.health__legend {
  max-width: var(--menu-w-lg);
  list-style: none;
  margin: 0;
  padding: 0;
  flex: 1 1 0;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.health__legend li {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-sm);
}

.health__dot {
  width: var(--legend-dot);
  height: var(--legend-dot);
  border-radius: var(--radius-pill);
  flex-shrink: 0;
}

.health__name {
  flex: 1 1 auto;
  min-width: 0;
  color: var(--brand-text-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.health__val {
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.health__info {
  font-size: var(--text-md);
  color: var(--brand-text-muted);
  cursor: help;
}

.health__foot {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
}

@media (max-width: 599px) {
  .health {
    flex-direction: column;
    align-items: stretch;
  }

  .health :deep(.mring) {
    align-self: center;
  }
}
</style>

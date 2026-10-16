<template>
  <div class="kpi-grid" data-widget="kpis">
    <div v-if="error && !data" class="kpi-grid__error dash-card" role="alert">
      {{ error }}
      <AppButton
        variant="secondary"
        size="sm"
        icon="refresh"
        label="Qayta urinish"
        @click="reload"
      />
    </div>
    <template v-else>
      <KpiCard
        v-for="k in cards"
        :key="k.kpiKey"
        v-bind="k"
        :loading="loading && !data"
      />
    </template>
  </div>
</template>

<script setup>
// 6.1 — the four numbers a head of school looks at first, 2 × 2: attendance today,
// absent today, mastery and quality over the chosen period. Each links to the page
// (with its filter) the number comes from.
import { computed } from 'vue'
import AppButton from '@/components/app/AppButton.vue'
import KpiCard from '../KpiCard.vue'
import { useWidget } from '@/composables/useWidget'
import { formatPercent, formatNumber } from '@/utils/format'
import { usePeriod, periodNote } from './period'

const props = defineProps({ readonly: { type: Boolean, default: false } })
const period = usePeriod()
const params = computed(() => ({ period: period.value }))
const { data, loading, error, reload } = useWidget('kpis', { params })

const today = new Date()
const month = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}`

const META = {
  attendance: {
    label: 'Bugungi davomat',
    icon: 'fact_check',
    color: 'var(--chart-primary)',
    percent: true,
    to: { path: '/attendance/calendar', query: { month } }
  },
  absent: {
    label: 'Bugun kelmaganlar',
    icon: 'person_off',
    color: 'var(--st-absent)',
    to: { path: '/attendance/calendar', query: { month } }
  },
  mastery: {
    label: "O'zlashtirish",
    icon: 'school',
    color: 'var(--ring-journal)',
    percent: true,
    to: { path: '/gradebook' }
  },
  quality: {
    label: 'Sifat',
    icon: 'workspace_premium',
    color: 'var(--ring-quality)',
    percent: true,
    to: { path: '/gradebook' }
  }
}

function note(k) {
  const e = k.extra || {}
  if (k.key === 'attendance') {
    if (e.schoolDay === false) return "Bugun dars yo'q"
    const auto = e.autoClasses ? ` · 🤖 ${e.autoClasses}` : ''
    return `Olingan: ${e.classesTaken ?? 0}/${e.classesTotal ?? 0} sinf${auto}`
  }
  if (k.key === 'absent') {
    if (e.excused === null || e.excused === undefined) return ''
    return `shundan sababli ${formatNumber(e.excused)}${e.late ? ` · kechikkan ${formatNumber(e.late)}` : ''}`
  }
  return `${periodNote(e.period, e.quarter)} · ${formatNumber(e.grades)} baho`
}

const cards = computed(() =>
  (data.value || Object.keys(META).map(key => ({ key }))).map(k => {
    const m = META[k.key]
    const none = k.value === null || k.value === undefined
    return {
      kpiKey: k.key,
      label: m.label,
      icon: m.icon,
      color: m.color,
      value: data.value ? (none ? null : k.value) : undefined,
      format: m.percent ? formatPercent : v => formatNumber(Math.round(v)),
      emptyValue: !!data.value && none,
      emptyText: k.key === 'attendance' ? 'Hali olinmagan' : '—',
      note: data.value ? note(k) : '',
      delta: k.delta ?? null,
      higherIsBetter: k.higherIsBetter ?? true,
      deltaPercent: !!m.percent,
      deltaDigits: m.percent ? 1 : 0,
      deltaTitle:
        k.key === 'mastery' || k.key === 'quality'
          ? 'Oxirgi 7 ish kuni oldingi 7 kunga nisbatan'
          : 'Oldingi ish kuniga nisbatan',
      spark: k.spark || [],
      sparkMin: null,
      sparkMax: null,
      to: props.readonly ? null : m.to
    }
  })
)
</script>

<style scoped>
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-4);
  height: 100%;
}

.kpi-grid__error {
  grid-column: 1 / -1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  padding: var(--space-4);
  color: var(--brand-text-muted);
}

@media (min-width: 1024px) and (max-width: 1439px) {
  .kpi-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 599px) {
  .kpi-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>

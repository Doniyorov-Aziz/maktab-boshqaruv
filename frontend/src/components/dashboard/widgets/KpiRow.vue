<template>
  <div class="kpi-row">
    <div v-if="error && !data" class="kpi-row__error brand-card" role="alert">
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
        :key="k.key"
        v-bind="k"
        :kpi-key="k.key"
        :loading="loading"
      >
        <template v-if="k.key === 'attendance' && k.extra" #note>
          <span v-if="!k.extra.schoolDay">Bugun dars yo'q</span>
          <template v-else>
            Olingan: {{ k.extra.classesTaken }}/{{ k.extra.classesTotal }} sinf
            <span v-if="k.extra.autoClasses">
              · 🤖 {{ k.extra.autoClasses }}</span
            >
            <q-linear-progress
              :value="
                k.extra.classesTotal
                  ? k.extra.classesTaken / k.extra.classesTotal
                  : 0
              "
              class="kpi-row__progress"
              color="primary"
              track-color="grey-3"
              rounded
            />
          </template>
        </template>
      </KpiCard>
    </template>
  </div>
</template>

<script setup>
// 6.1 — the six numbers a head of school looks at first. Each card links to the page
// (with its filter) where the number comes from.
import { computed } from 'vue'
import AppButton from '@/components/app/AppButton.vue'
import KpiCard from '../KpiCard.vue'
import { useWidget } from '@/composables/useWidget'
import { formatPercent, formatNumber } from '@/utils/format'

const props = defineProps({ readonly: { type: Boolean, default: false } })
const { data, loading, error, reload } = useWidget('kpis')

const today = new Date()
const month = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}`

const META = {
  attendance: {
    label: 'Bugungi davomat',
    icon: 'fact_check',
    color: 'var(--st-present)',
    percent: true,
    to: { path: '/attendance/calendar', query: { month } }
  },
  absent: {
    label: 'Bugun kelmaganlar',
    icon: 'person_off',
    color: 'var(--st-absent)',
    to: { path: '/attendance/calendar', query: { month } }
  },
  late: {
    label: 'Kechikkanlar',
    icon: 'schedule',
    color: 'var(--st-late)',
    to: { path: '/attendance/calendar', query: { month } }
  },
  mastery: {
    label: "O'zlashtirish",
    icon: 'school',
    color: 'var(--gr-4)',
    percent: true,
    to: { path: '/gradebook' }
  },
  quality: {
    label: 'Sifat',
    icon: 'workspace_premium',
    color: 'var(--gr-5)',
    percent: true,
    to: { path: '/gradebook' }
  },
  appeals: {
    label: 'Javob kutayotgan murojaatlar',
    icon: 'forum',
    color: 'var(--chart-primary)',
    to: { path: '/parent-messages' }
  }
}

const cards = computed(() =>
  (data.value || Object.keys(META).map(key => ({ key }))).map(k => {
    const m = META[k.key]
    const v = k.value
    const none = v === null || v === undefined
    let note = ''
    if (k.key === 'absent' && k.extra)
      note =
        k.extra.excused !== null && k.extra.excused !== undefined
          ? `Sababli: ${k.extra.excused}`
          : ''
    if (k.key === 'late' && k.extra)
      note = k.extra.avgMinutes ? `O'rtacha ${k.extra.avgMinutes} daqiqa` : ''
    if ((k.key === 'mastery' || k.key === 'quality') && k.extra)
      note = `${k.extra.quarter}-chorak · ${formatNumber(k.extra.grades)} ta baho · ${k.key === 'mastery' ? '3, 4, 5' : '4, 5'} / jami`
    if (k.key === 'appeals' && k.extra)
      note = k.extra.overdue
        ? `🔴 ${k.extra.overdue} tasi ${k.extra.overdueHours} soatdan oshgan`
        : `${k.extra.overdueHours} soatdan oshgani yo'q`
    return {
      key: k.key,
      label: m.label,
      icon: m.icon,
      color: m.color,
      value: v,
      emptyValue: none,
      display: none
        ? k.key === 'attendance'
          ? '— Hali olinmagan'
          : '—'
        : m.percent
          ? formatPercent(v)
          : formatNumber(v),
      note,
      extra: k.extra,
      delta: k.delta,
      higherIsBetter: k.higherIsBetter ?? true,
      deltaPercent: !!m.percent,
      deltaDigits: m.percent ? 1 : 0,
      deltaTitle:
        k.key === 'mastery' || k.key === 'quality'
          ? 'Oxirgi 7 ish kuni oldingi 7 kunga nisbatan'
          : 'Oldingi ish kuniga nisbatan',
      spark: k.spark,
      sparkMin: m.percent ? 0 : null,
      sparkMax: m.percent ? 100 : null,
      to: props.readonly ? null : m.to
    }
  })
)
</script>

<style scoped>
.kpi-row {
  display: contents;
}

.kpi-row__error {
  grid-column: 1 / -1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  padding: var(--space-4);
  color: var(--brand-text-muted);
}

.kpi-row__progress {
  margin-top: var(--space-1);
  height: var(--space-1);
  border-radius: var(--radius-pill);
}
</style>

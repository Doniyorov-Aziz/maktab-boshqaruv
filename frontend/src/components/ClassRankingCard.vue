<template>
  <div class="brand-card q-pa-md full-height column class-ranking-card">
    <div class="row items-center justify-between q-mb-xs no-wrap">
      <div class="row items-center q-gutter-xs ranking-title">
        <span class="text-subtitle1 text-weight-semibold ellipsis"
          >Sinflar reytingi — {{ metric.title }}, oxirgi 30 kun</span
        >
        <q-icon name="info" size="16px" class="muted-text cursor-help">
          <q-tooltip max-width="260px" class="text-caption">{{
            metric.tooltip
          }}</q-tooltip>
        </q-icon>
      </div>
    </div>

    <div class="row items-center justify-between q-mb-sm">
      <q-btn-toggle
        v-model="activeTab"
        dense
        no-caps
        unelevated
        toggle-color="primary"
        color="white"
        text-color="grey-7"
        class="ranking-toggle"
        :options="[
          { label: 'Davomat', value: 'attendance' },
          { label: 'O\'rtacha baho', value: 'grade' },
          { label: 'Umumiy', value: 'overall' }
        ]"
      />
      <span v-if="domain" class="text-caption muted-text scale-label"
        >Shkala: {{ formatScaleValue(domain.lo) }}–{{
          formatScaleValue(domain.hi)
        }}{{ metric.unit }}</span
      >
    </div>

    <q-skeleton v-if="loading" type="rect" height="220px" />
    <div v-else-if="!rankings.length" class="muted-text text-body2 q-py-md"
      >Ma'lumot yo'q</div
    >
    <div v-else class="col ranking-rows">
      <div class="ranking-group-label" v-if="bottomRows.length"
        >Eng yaxshi 5 ta</div
      >
      <ranking-row
        v-for="row in topRows"
        :key="row.schoolClassId"
        :row="row"
        @click="goClass(row.schoolClassId)"
      />
      <div v-if="bottomRows.length" class="ranking-separator">
        <q-separator />
        <span class="ranking-separator__label">Eng past 5 ta</span>
        <q-separator />
      </div>
      <ranking-row
        v-for="row in bottomRows"
        :key="row.schoolClassId"
        :row="row"
        @click="goClass(row.schoolClassId)"
      />
    </div>

    <div v-if="!loading && sortedFull.length > 10" class="text-center q-mt-sm">
      <q-btn
        flat
        dense
        no-caps
        color="primary"
        :label="`Barcha ${sortedFull.length} sinfni ko'rish`"
        @click="allDialogOpen = true"
      />
    </div>

    <q-dialog v-model="allDialogOpen">
      <q-card style="width: 100%; max-width: 560px; border-radius: 16px">
        <q-card-section class="row items-center justify-between q-pb-none">
          <div class="text-subtitle1 text-weight-bold"
            >Barcha sinflar reytingi</div
          >
          <q-btn flat round dense icon="close" v-close-popup />
        </q-card-section>
        <q-card-section class="q-pb-none">
          <div class="row items-center justify-between q-gutter-sm">
            <q-btn-toggle
              v-model="activeTab"
              dense
              no-caps
              unelevated
              toggle-color="primary"
              color="white"
              text-color="grey-7"
              :options="[
                { label: 'Davomat', value: 'attendance' },
                { label: 'O\'rtacha baho', value: 'grade' },
                { label: 'Umumiy', value: 'overall' }
              ]"
            />
            <span v-if="domain" class="text-caption muted-text scale-label"
              >Shkala: {{ formatScaleValue(domain.lo) }}–{{
                formatScaleValue(domain.hi)
              }}{{ metric.unit }}</span
            >
          </div>
        </q-card-section>
        <q-card-section style="max-height: 60vh; overflow-y: auto">
          <ranking-row
            v-for="row in sortedFull"
            :key="row.schoolClassId"
            :row="row"
            @click="
              () => {
                allDialogOpen = false
                goClass(row.schoolClassId)
              }
            "
          />
        </q-card-section>
      </q-card>
    </q-dialog>
  </div>
</template>

<script setup>
import { ref, computed, h, defineComponent } from 'vue'
import { useRouter } from 'vue-router'

const props = defineProps({
  rankings: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false }
})

const router = useRouter()
const activeTab = ref('attendance')
const allDialogOpen = ref(false)

const METRICS = {
  attendance: {
    key: 'attendanceRate',
    deltaKey: 'attendanceRateDelta',
    title: 'davomat',
    unit: '%',
    hardMin: 0,
    hardMax: 100,
    roundStep: 1,
    tooltip:
      "Davomat foizi: darsda qatnashgan o'quvchilar / jami o'quvchi-darslar × 100%, oxirgi 30 kun.",
    format: v => `${v.toFixed(1)}%`,
    deltaFormat: v => `${v > 0 ? '+' : ''}${v.toFixed(1)}`
  },
  grade: {
    key: 'averageGrade',
    deltaKey: 'averageGradeDelta',
    title: "o'rtacha baho",
    unit: '',
    hardMin: 2,
    hardMax: 5,
    roundStep: 0.1,
    tooltip:
      "O'rtacha baho: oxirgi 30 kunda qo'yilgan barcha baholarning o'rtachasi (2–5 ballik tizimda).",
    format: v => v.toFixed(2),
    deltaFormat: v => `${v > 0 ? '+' : ''}${v.toFixed(2)}`
  },
  overall: {
    key: 'overallScore',
    deltaKey: 'overallScoreDelta',
    title: 'umumiy ball',
    unit: '',
    hardMin: 0,
    hardMax: 100,
    roundStep: 1,
    tooltip:
      "Umumiy ball = Davomat% × 0.5 + normallashtirilgan o'rtacha baho ((baho−2)/3×100) × 0.5.",
    format: v => v.toFixed(1),
    deltaFormat: v => `${v > 0 ? '+' : ''}${v.toFixed(1)}`
  }
}

const metric = computed(() => METRICS[activeTab.value])

function metricValue(row) {
  return row[metric.value.key]
}
function metricDelta(row) {
  return row[metric.value.deltaKey]
}

const sortedRaw = computed(() =>
  [...props.rankings].sort((a, b) => {
    const va = metricValue(a)
    const vb = metricValue(b)
    if (va == null && vb == null) return 0
    if (va == null) return 1
    if (vb == null) return -1
    return vb - va
  })
)

const schoolAverage = computed(() => {
  const vals = sortedRaw.value.map(metricValue).filter(v => v != null)
  if (!vals.length) return null
  return vals.reduce((a, b) => a + b, 0) / vals.length
})

const domain = computed(() => {
  const m = metric.value
  const vals = sortedRaw.value.map(metricValue).filter(v => v != null)
  if (!vals.length) return { lo: m.hardMin, hi: m.hardMax }
  let lo = Math.min(...vals)
  let hi = Math.max(...vals)
  if (lo === hi) {
    lo -= m.roundStep * 5
    hi += m.roundStep * 5
  }
  const pad = (hi - lo) * 0.15
  lo = Math.max(m.hardMin, lo - pad)
  hi = Math.min(m.hardMax, hi + pad)
  const step = m.roundStep
  lo = Math.floor(lo / step) * step
  hi = Math.ceil(hi / step) * step
  return { lo, hi }
})

function formatScaleValue(v) {
  return metric.value.key === 'averageGrade' ? v.toFixed(1) : Math.round(v)
}

function barColor(value) {
  if (value == null || schoolAverage.value == null) return 'var(--brand-border)'
  const d = domain.value
  const band = Math.max((d.hi - d.lo) * 0.04, 0.001)
  const avg = schoolAverage.value
  if (value > avg + band) return '#22c55e'
  if (value >= avg - band) return '#3b82f6'
  if (value >= avg - band * 3) return '#f97316'
  return '#ef4444'
}

function pct(value) {
  if (value == null) return 0
  const d = domain.value
  const range = d.hi - d.lo || 1
  return Math.min(100, Math.max(0, ((value - d.lo) / range) * 100))
}

function medal(rank) {
  if (rank === 1) return '\u{1F947}'
  if (rank === 2) return '\u{1F948}'
  if (rank === 3) return '\u{1F949}'
  return `#${rank}`
}

const sortedFull = computed(() => {
  const m = metric.value
  const avgPct = schoolAverage.value != null ? pct(schoolAverage.value) : null
  return sortedRaw.value.map((row, i) => {
    const value = metricValue(row)
    const deltaVal = metricDelta(row)
    return {
      schoolClassId: row.schoolClassId,
      className: row.className,
      homeroomTeacherName: row.homeroomTeacherName,
      rank: i + 1,
      medal: medal(i + 1),
      valueLabel: value == null ? '—' : m.format(value),
      deltaLabel: deltaVal == null ? null : m.deltaFormat(deltaVal),
      deltaUp: deltaVal != null && deltaVal > 0,
      deltaDown: deltaVal != null && deltaVal < 0,
      barPct: pct(value),
      avgPct,
      barColor: barColor(value)
    }
  })
})

const topRows = computed(() =>
  sortedFull.value.length > 10 ? sortedFull.value.slice(0, 5) : sortedFull.value
)
const bottomRows = computed(() =>
  sortedFull.value.length > 10 ? sortedFull.value.slice(-5) : []
)

function goClass(id) {
  router.push(`/profiles/class/${id}`)
}

const RankingRow = defineComponent({
  props: { row: { type: Object, required: true } },
  emits: ['click'],
  setup(props, { emit }) {
    return () =>
      h('div', { class: 'ranking-row', onClick: () => emit('click') }, [
        h('div', { class: 'ranking-row__rank' }, props.row.medal),
        h('div', { class: 'ranking-row__name' }, [
          h(
            'div',
            { class: 'ellipsis text-weight-medium' },
            props.row.className
          ),
          h(
            'div',
            { class: 'ellipsis text-caption muted-text' },
            props.row.homeroomTeacherName || '—'
          )
        ]),
        h('div', { class: 'ranking-row__bar' }, [
          h('div', { class: 'ranking-row__bar-track' }, [
            h('div', {
              class: 'ranking-row__bar-fill',
              style: {
                width: `${props.row.barPct}%`,
                background: props.row.barColor
              }
            }),
            props.row.avgPct != null
              ? h('div', {
                  class: 'ranking-row__avg-line',
                  style: { left: `${props.row.avgPct}%` }
                })
              : null
          ])
        ]),
        h('div', { class: 'ranking-row__value' }, [
          h(
            'span',
            { class: 'text-weight-bold tabular-nums' },
            props.row.valueLabel
          ),
          props.row.deltaLabel
            ? h(
                'span',
                {
                  class: [
                    'ranking-row__delta',
                    props.row.deltaUp
                      ? 'text-positive'
                      : props.row.deltaDown
                        ? 'text-negative'
                        : 'muted-text'
                  ]
                },
                `${props.row.deltaUp ? '↑' : props.row.deltaDown ? '↓' : ''}${props.row.deltaLabel.replace('+', '').replace('-', '')}`
              )
            : null
        ])
      ])
  }
})
</script>

<script>
export default { name: 'ClassRankingCard' }
</script>

<style scoped>
.muted-text {
  color: var(--brand-text-muted);
}

.ranking-toggle :deep(.q-btn) {
  font-size: var(--text-xs);
  min-height: 28px;
  padding: 0 10px;
}

.scale-label {
  white-space: nowrap;
}

.ranking-rows {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 2px;
}

.ranking-group-label {
  font-size: var(--text-xs);
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: var(--brand-text-muted);
  padding: 2px 8px 4px;
}

.ranking-separator {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 4px;
}

.ranking-separator__label {
  font-size: var(--text-xs);
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: var(--brand-text-muted);
  white-space: nowrap;
}

:deep(.ranking-row) {
  display: grid;
  grid-template-columns: 30px 1fr 110px 78px;
  align-items: center;
  gap: 10px;
  padding: 6px 8px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  font-size: var(--text-sm);
}

:deep(.ranking-row:hover) {
  background: rgba(79, 70, 229, 0.08);
}

:deep(.ranking-row__rank) {
  text-align: center;
  font-size: 15px;
}

:deep(.ranking-row__name) {
  min-width: 0;
}

:deep(.ranking-row__bar-track) {
  position: relative;
  height: 6px;
  border-radius: 999px;
  background: var(--brand-border);
  overflow: visible;
}

:deep(.ranking-row__bar-fill) {
  position: absolute;
  inset: 0 auto 0 0;
  height: 100%;
  border-radius: 999px;
  transition: width 0.3s ease;
}

:deep(.ranking-row__avg-line) {
  position: absolute;
  top: -3px;
  bottom: -3px;
  width: 2px;
  background: var(--brand-text-muted);
  opacity: 0.6;
}

:deep(.ranking-row__value) {
  text-align: right;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  line-height: 1.25;
}

:deep(.ranking-row__delta) {
  font-size: var(--text-xs);
  font-weight: 600;
}
</style>

<template>
  <ChartCard
    title="Dinamika — oxirgi 30 ish kuni"
    :subtitle="subtitle"
    :as-of="asOf"
    :loading="loading"
    :error="error"
    :data="data"
    :empty="!!data && !hasPoints"
    skeleton="chart"
    :tabs="tabs"
    :tab="tab"
    data-widget="dynamics"
    @update:tab="v => (tab = v)"
    @retry="reload"
  >
    <div v-if="tab === 'both'" class="dyn__legend">
      <span><i class="dyn__key dyn__key--att" />Davomat</span>
      <span><i class="dyn__key dyn__key--q" />Sifat</span>
      <span
        ><i class="dyn__key dyn__key--target" />Maqsad {{ data?.target }}%</span
      >
    </div>
    <div class="dyn">
      <Line
        :key="themeKey + tab"
        :data="chartData"
        :options="options"
        :plugins="[targetLine]"
      />
    </div>
    <template v-if="lowest && tab === 'both'" #footer>
      Eng past kun: <b class="dyn__b">{{ lowest.label }}</b> — davomat
      {{ formatPercent(lowest.attendance) }}, {{ formatNumber(lowest.absent) }}
      kishi kelmagan
    </template>
  </ChartCard>
</template>

<script setup>
// 6.5 — the last 30 school days: attendance (brand line with a gradient under it) and
// quality (green, no fill) together, the target as a dashed line and the lowest day
// marked; or the number of absent students; or the number of grades. Monotone curves:
// no made-up waves between the days.
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Line } from 'vue-chartjs'
import {
  Chart,
  LineElement,
  PointElement,
  LinearScale,
  CategoryScale,
  Filler,
  Tooltip
} from 'chart.js'
import ChartCard from '../ChartCard.vue'
import { useWidget } from '@/composables/useWidget'
import { useChartTheme, alpha } from '@/charts/chartTheme'
import { formatPercent, formatNumber } from '@/utils/format'

Chart.register(
  LineElement,
  PointElement,
  LinearScale,
  CategoryScale,
  Filler,
  Tooltip
)

const props = defineProps({ readonly: { type: Boolean, default: false } })
const router = useRouter()
const { data, asOf, loading, error, reload } = useWidget('dynamics', {
  params: { days: 30 }
})
const { themeKey, colors } = useChartTheme()

const tabs = [
  { label: 'Davomat va sifat', value: 'both' },
  { label: 'Kelmaganlar soni', value: 'absent' },
  { label: 'Baholar soni', value: 'grades' }
]
const tab = ref('both')

const MONTHS = [
  'yan',
  'fev',
  'mar',
  'apr',
  'may',
  'iyun',
  'iyul',
  'avg',
  'sen',
  'okt',
  'noy',
  'dek'
]
const dayLabel = iso => {
  const [, m, d] = iso.split('-').map(Number)
  return `${d}-${MONTHS[m - 1]}`
}

const points = computed(() => data.value?.points || [])
const hasPoints = computed(() =>
  points.value.some(p => p.attendance !== null || p.grades !== null)
)
const subtitle = computed(() =>
  data.value ? `Yakshanba va bayramlarsiz · maqsad ${data.value.target}%` : ''
)

const lowest = computed(() => {
  const withData = points.value.filter(p => p.attendance !== null)
  if (!withData.length) return null
  const low = withData.reduce((a, p) => (p.attendance < a.attendance ? p : a))
  return { ...low, label: dayLabel(low.date) }
})

// a vertical gradient under the attendance line: brand at the top, nothing at the bottom
function gradient(ctx, color) {
  const { chart } = ctx
  const area = chart.chartArea
  if (!area) return alpha(color, 0.12)
  const g = chart.ctx.createLinearGradient(0, area.top, 0, area.bottom)
  g.addColorStop(0, alpha(color, 0.28))
  g.addColorStop(1, alpha(color, 0))
  return g
}

const chartData = computed(() => {
  const c = colors.value
  const labels = points.value.map(p => dayLabel(p.date))
  if (tab.value === 'both') {
    const lowIdx = lowest.value
      ? points.value.findIndex(p => p.date === lowest.value.date)
      : -1
    return {
      labels,
      datasets: [
        {
          label: 'Davomat',
          data: points.value.map(p => p.attendance),
          borderColor: c.primary,
          backgroundColor: ctx => gradient(ctx, c.primary),
          fill: true,
          borderWidth: 2.25,
          cubicInterpolationMode: 'monotone',
          spanGaps: false,
          pointRadius: points.value.map((_, i) => (i === lowIdx ? 5 : 0)),
          pointBackgroundColor: points.value.map((_, i) =>
            i === lowIdx ? c.absent : c.primary
          ),
          pointBorderColor: c.card,
          pointBorderWidth: 2,
          pointHoverRadius: 5,
          yAxisID: 'y'
        },
        {
          label: 'Sifat',
          data: points.value.map(p => p.quality),
          borderColor: c.ringQuality,
          backgroundColor: 'transparent',
          fill: false,
          borderWidth: 2,
          cubicInterpolationMode: 'monotone',
          spanGaps: false,
          pointRadius: 0,
          pointHoverRadius: 5,
          yAxisID: 'y'
        }
      ]
    }
  }
  const absent = tab.value === 'absent'
  const color = absent ? c.absent : c.primary
  return {
    labels,
    datasets: [
      {
        label: absent ? 'Kelmaganlar' : 'Baholar',
        data: points.value.map(p => (absent ? p.absent : p.grades)),
        borderColor: color,
        backgroundColor: ctx => gradient(ctx, color),
        fill: true,
        borderWidth: 2,
        cubicInterpolationMode: 'monotone',
        spanGaps: false,
        pointRadius: 0,
        pointHoverRadius: 5
      }
    ]
  }
})

const yRange = computed(() => {
  if (tab.value !== 'both') return { min: 0 }
  const vals = points.value
    .flatMap(p => [p.attendance, p.quality])
    .filter(v => v !== null)
  const target = data.value?.target ?? 95
  const lo = vals.length ? Math.min(...vals, target) : target
  return { min: Math.max(0, Math.floor((lo - 3) / 5) * 5), max: 100 }
})

const options = computed(() => ({
  // a gradient fill cannot be tweened: animate positions only
  animations: { colors: false },
  layout: { padding: { top: 8, right: 8, left: 0, bottom: 0 } },
  interaction: { mode: 'index', intersect: false },
  scales: {
    x: {
      grid: { display: false },
      ticks: { maxRotation: 0, autoSkip: true, maxTicksLimit: 8 }
    },
    y: {
      ...yRange.value,
      ticks: {
        callback: v => (tab.value === 'both' ? `${v}%` : v),
        maxTicksLimit: 6
      }
    }
  },
  plugins: {
    tooltip: {
      callbacks: {
        title: items => {
          const p = points.value[items[0].dataIndex]
          return p ? p.date.split('-').reverse().join('.') : ''
        },
        label: item => {
          const p = points.value[item.dataIndex]
          if (!p) return ''
          if (tab.value === 'both')
            return item.datasetIndex === 0
              ? `Davomat: ${formatPercent(p.attendance)}`
              : `Sifat: ${formatPercent(p.quality)}`
          return tab.value === 'absent'
            ? `Kelmagan: ${formatNumber(p.absent)}`
            : `Baholar: ${formatNumber(p.grades)}`
        }
      }
    }
  },
  onClick: (_, els) => {
    if (props.readonly || !els.length) return
    const p = points.value[els[0].index]
    if (p)
      router.push({
        path: '/attendance/calendar',
        query: { month: p.date.slice(0, 7) }
      })
  },
  onHover: (e, els) => {
    if (e.native?.target)
      e.native.target.style.cursor =
        els.length && !props.readonly ? 'pointer' : 'default'
  }
}))

// the target as a dashed line with its label (first tab only)
const targetLine = {
  id: 'targetLine',
  afterDatasetsDraw(chart) {
    if (tab.value !== 'both' || !data.value) return
    const y = chart.scales.y
    const target = data.value.target
    if (target < y.min || target > y.max) return
    const yy = y.getPixelForValue(target)
    const { left, right } = chart.chartArea
    const ctx = chart.ctx
    ctx.save()
    ctx.strokeStyle = colors.value.target
    ctx.setLineDash([5, 4])
    ctx.lineWidth = 1
    ctx.beginPath()
    ctx.moveTo(left, yy)
    ctx.lineTo(right, yy)
    ctx.stroke()
    ctx.setLineDash([])
    ctx.fillStyle = colors.value.text
    ctx.font = '600 11px Inter, sans-serif'
    ctx.textAlign = 'right'
    ctx.fillText(`Maqsad ${target}%`, right, yy - 5)
    // the lowest day, written by its point
    if (lowest.value) {
      const i = points.value.findIndex(p => p.date === lowest.value.date)
      const el = chart.getDatasetMeta(0).data[i]
      if (el) {
        ctx.fillStyle = colors.value.absent
        ctx.textAlign = el.x > (left + right) / 2 ? 'right' : 'left'
        ctx.fillText(
          `${lowest.value.label}: ${formatPercent(lowest.value.attendance)}`,
          el.x + (ctx.textAlign === 'left' ? 8 : -8),
          el.y + 16
        )
      }
    }
    ctx.restore()
  }
}
</script>

<style scoped>
.dyn {
  position: relative;
  width: 100%;
  height: var(--dyn-h);
}

.dyn__legend {
  display: flex;
  gap: var(--space-4);
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
  margin-bottom: var(--space-2);
}

.dyn__legend span {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
}

.dyn__key {
  display: inline-block;
  width: var(--space-4);
  height: var(--accent-bar);
  border-radius: var(--radius-pill);
}

.dyn__key--att {
  background: var(--chart-primary);
}

.dyn__key--q {
  background: var(--ring-quality);
}

.dyn__key--target {
  background: repeating-linear-gradient(
    90deg,
    var(--chart-target) 0 var(--space-1),
    transparent var(--space-1) var(--space-2)
  );
}

.dyn__b {
  color: var(--text-primary);
}
</style>

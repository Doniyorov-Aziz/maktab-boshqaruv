<template>
  <ChartCard
    title="Davomat qanday o'zgaryapti?"
    :subtitle="subtitle"
    :as-of="asOf"
    :loading="loading"
    :error="error"
    :data="data"
    :empty="!!data && !hasPoints"
    empty-text="Hali davomat ma'lumoti yo'q"
    :tabs="tabs"
    :tab="tab"
    @update:tab="v => (tab = v)"
    @retry="reload"
  >
    <div class="trend app-chart">
      <Line
        :key="themeKey + tab"
        :data="chartData"
        :options="options"
        :plugins="[targetLine]"
      />
    </div>
    <template v-if="lowest" #footer>
      Eng past kun: <b>{{ lowest.label }}</b> —
      {{ formatPercent(lowest.rate) }}, {{ lowest.absent }} kishi kelmagan
    </template>
  </ChartCard>
</template>

<script setup>
// 6.5 — attendance over the last 30 school days (no Sundays, no holidays). The y axis
// starts just under the lowest day and ends at 100; the dashed line is the school's
// target. Clicking a point opens that month's calendar.
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
import { useChartTheme } from '@/charts/chartTheme'
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
const { data, asOf, loading, error, reload } = useWidget('attendance-trend', {
  params: { days: 30 }
})
const { themeKey, colors } = useChartTheme()

const tabs = [
  { label: 'Davomat %', value: 'rate' },
  { label: 'Kelmaganlar soni', value: 'absent' }
]
const tab = ref('rate')

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
const hasPoints = computed(() => points.value.some(p => p.rate !== null))
const subtitle = computed(() =>
  data.value
    ? `Oxirgi ${points.value.length} ish kuni · maqsad ${data.value.target}%`
    : ''
)
const lowest = computed(() => {
  const withData = points.value.filter(p => p.rate !== null)
  if (!withData.length) return null
  const low = withData.reduce((a, p) => (p.rate < a.rate ? p : a))
  return { ...low, label: dayLabel(low.date) }
})

const values = computed(() =>
  points.value.map(p => (tab.value === 'rate' ? p.rate : p.absent))
)
const chartData = computed(() => {
  const c = colors.value
  const lowIdx = lowest.value
    ? points.value.findIndex(p => p.date === lowest.value.date)
    : -1
  return {
    labels: points.value.map(p => dayLabel(p.date)),
    datasets: [
      {
        data: values.value,
        borderColor: tab.value === 'rate' ? c.primary : c.absent,
        backgroundColor:
          tab.value === 'rate'
            ? c.primaryFill
            : `color-mix(in srgb, ${c.absent} 14%, transparent)`,
        fill: true,
        borderWidth: 2,
        cubicInterpolationMode: 'monotone',
        tension: 0.2,
        spanGaps: false,
        pointRadius: points.value.map((_, i) =>
          i === lowIdx && tab.value === 'rate' ? 5 : 2
        ),
        pointBackgroundColor: points.value.map((_, i) =>
          i === lowIdx && tab.value === 'rate' ? c.absent : c.primary
        ),
        pointHoverRadius: 6
      }
    ]
  }
})

const yRange = computed(() => {
  if (tab.value !== 'rate') return { min: 0 }
  const rates = points.value.map(p => p.rate).filter(v => v !== null)
  const target = data.value?.target ?? 95
  const lo = rates.length ? Math.min(...rates, target) : target
  return { min: Math.max(0, Math.floor(lo - 2)), max: 100 }
})

const options = computed(() => ({
  layout: { padding: { top: 8, right: 12, left: 4, bottom: 0 } },
  interaction: { mode: 'index', intersect: false },
  scales: {
    x: {
      grid: { display: false },
      ticks: { maxRotation: 0, autoSkip: true, maxTicksLimit: 10 }
    },
    y: {
      ...yRange.value,
      ticks: { callback: v => (tab.value === 'rate' ? `${v}%` : v) }
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
          if (!p || p.rate === null) return 'Davomat olinmagan'
          return [
            `Davomat: ${formatPercent(p.rate)}`,
            `Kelmagan: ${formatNumber(p.absent)}`,
            `Belgilangan: ${formatNumber(p.marked)}`
          ]
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

// the target as a dashed line with its label (rate tab only)
const targetLine = {
  id: 'targetLine',
  afterDatasetsDraw(chart) {
    if (tab.value !== 'rate' || !data.value) return
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
    ctx.fillText(`Maqsad ${target}%`, right, yy - 4)
    ctx.restore()
  }
}
</script>

<style scoped>
.trend {
  overflow: hidden;
}
</style>

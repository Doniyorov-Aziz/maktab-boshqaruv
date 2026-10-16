<template>
  <ChartCard
    title="Sinflar xaritasi: davomat × sifat"
    :subtitle="subtitle"
    :as-of="asOf"
    :loading="loading"
    :error="error"
    :data="data"
    :empty="!!data && !points.length"
    skeleton="chart"
    data-widget="class-map"
    @retry="reload"
  >
    <div class="bubbles" :data-points="points.length">
      <Bubble
        :key="themeKey"
        :data="chartData"
        :options="options"
        :plugins="[quadrants, names]"
      />
    </div>
    <template v-if="data" #footer>
      <span class="bubbles__foot">
        Punktir — maktab o'rtachasi: davomat
        <b>{{ formatPercent(data.avgAttendance) }}</b
        >, sifat <b>{{ formatPercent(data.avgQuality) }}</b> · pufakcha o'lchami
        — o'quvchilar soni
      </span>
    </template>
  </ChartCard>
</template>

<script setup>
// 6.3 — every class as a bubble: x = attendance over 30 days, y = this quarter's
// quality, size = students. The school averages split the plane into four corners:
// stars (↗), attendance needs work (↖), quality needs work (↘), both (↙). Colour
// follows the corner; hover tells the class teacher and the three numbers; a click
// opens the class.
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { Bubble } from 'vue-chartjs'
import {
  Chart,
  BubbleController,
  PointElement,
  LinearScale,
  Tooltip
} from 'chart.js'
import ChartCard from '../ChartCard.vue'
import { useWidget } from '@/composables/useWidget'
import { useChartTheme, alpha } from '@/charts/chartTheme'
import { formatPercent, formatNumber } from '@/utils/format'

Chart.register(BubbleController, PointElement, LinearScale, Tooltip)

const props = defineProps({ readonly: { type: Boolean, default: false } })
const router = useRouter()
const { data, asOf, loading, error, reload } = useWidget('class-map')
const { themeKey, colors } = useChartTheme()

const points = computed(() =>
  (data.value?.classes || []).filter(
    c => c.attendance !== null && c.quality !== null
  )
)
const subtitle = computed(() =>
  data.value ? `Davomat — ${data.value.days} kun, sifat — shu chorak` : ''
)

const avgX = computed(() => data.value?.avgAttendance ?? 0)
const avgY = computed(() => data.value?.avgQuality ?? 0)

function corner(c) {
  const right = c.attendance >= avgX.value
  const top = c.quality >= avgY.value
  if (right && top) return 'star'
  if (!right && !top) return 'both'
  return right ? 'quality' : 'attendance'
}

function color(c) {
  const k = corner(c)
  const p = colors.value
  return k === 'star' ? p.present : k === 'both' ? p.absent : p.warn
}

// bubble radius from the class size: 7–18 px
const radius = computed(() => {
  const sizes = points.value.map(c => c.students)
  const lo = Math.min(...sizes, 0)
  const hi = Math.max(...sizes, 1)
  return n => 7 + (hi === lo ? 0.5 : (n - lo) / (hi - lo)) * 11
})

const chartData = computed(() => ({
  datasets: [
    {
      data: points.value.map(c => ({
        x: c.attendance,
        y: c.quality,
        r: radius.value(c.students)
      })),
      backgroundColor: points.value.map(c => alpha(color(c), 0.28)),
      borderColor: points.value.map(c => color(c)),
      borderWidth: 1.5,
      hoverBorderWidth: 2.5,
      hoverBackgroundColor: points.value.map(c => alpha(color(c), 0.5))
    }
  ]
}))

// the axes fit the data (with a margin) and say so in their titles
const range = computed(() => {
  const xs = points.value.map(c => c.attendance)
  const ys = points.value.map(c => c.quality)
  const xMin = xs.length
    ? Math.max(0, Math.floor(Math.min(...xs, avgX.value) - 2))
    : 80
  const yMin = ys.length
    ? Math.max(0, Math.floor(Math.min(...ys, avgY.value) - 6))
    : 40
  const yMax = ys.length
    ? Math.min(100, Math.ceil(Math.max(...ys, avgY.value) + 6))
    : 100
  return { xMin, xMax: 100, yMin, yMax }
})

const options = computed(() => ({
  // a gradient fill cannot be tweened: animate positions only
  animations: { colors: false },
  layout: { padding: { top: 8, right: 16, left: 4, bottom: 0 } },
  scales: {
    x: {
      min: range.value.xMin,
      max: range.value.xMax,
      title: {
        display: true,
        text: `Davomat, % (${range.value.xMin}–${range.value.xMax})`,
        color: colors.value.text
      },
      ticks: { callback: v => `${v}%` },
      grid: { color: colors.value.grid }
    },
    y: {
      min: range.value.yMin,
      max: range.value.yMax,
      title: {
        display: true,
        text: `Sifat, % (${range.value.yMin}–${range.value.yMax})`,
        color: colors.value.text
      },
      ticks: { callback: v => `${v}%` }
    }
  },
  plugins: {
    tooltip: {
      displayColors: false,
      callbacks: {
        title: items => {
          const c = points.value[items[0].dataIndex]
          return c ? `${c.name} sinf` : ''
        },
        label: item => {
          const c = points.value[item.dataIndex]
          if (!c) return ''
          return [
            `Rahbar: ${c.teacher || '—'}`,
            `Davomat: ${formatPercent(c.attendance)}`,
            `Sifat: ${formatPercent(c.quality)}`,
            `O'zlashtirish: ${formatPercent(c.mastery)}`,
            `O'quvchilar: ${formatNumber(c.students)}`
          ]
        }
      }
    }
  },
  onClick: (_, els) => {
    if (props.readonly || !els.length) return
    const c = points.value[els[0].index]
    if (c) router.push(`/profiles/class/${c.classId}`)
  },
  onHover: (e, els) => {
    if (e.native?.target)
      e.native.target.style.cursor =
        els.length && !props.readonly ? 'pointer' : 'default'
  }
}))

// dashed average lines and the four corner names, faint, behind the bubbles
const quadrants = {
  id: 'quadrants',
  beforeDatasetsDraw(chart) {
    if (!data.value || !points.value.length) return
    const { ctx, chartArea: a, scales } = chart
    const x = scales.x.getPixelForValue(avgX.value)
    const y = scales.y.getPixelForValue(avgY.value)
    ctx.save()
    ctx.strokeStyle = colors.value.quadrantLine
    ctx.setLineDash([4, 4])
    ctx.lineWidth = 1
    ctx.beginPath()
    if (x > a.left && x < a.right) {
      ctx.moveTo(x, a.top)
      ctx.lineTo(x, a.bottom)
    }
    if (y > a.top && y < a.bottom) {
      ctx.moveTo(a.left, y)
      ctx.lineTo(a.right, y)
    }
    ctx.stroke()
    ctx.setLineDash([])
    ctx.fillStyle = colors.value.quadrantLabel
    ctx.font = '600 11px Inter, sans-serif'
    const pad = 8
    ctx.textBaseline = 'top'
    ctx.textAlign = 'right'
    ctx.fillText('↗ Yulduzlar', a.right - pad, a.top + pad)
    ctx.textAlign = 'left'
    ctx.fillText("↖ Davomatga e'tibor", a.left + pad, a.top + pad)
    ctx.textBaseline = 'bottom'
    ctx.textAlign = 'right'
    ctx.fillText("↘ Sifatga e'tibor", a.right - pad, a.bottom - pad)
    ctx.textAlign = 'left'
    ctx.fillText("↙ Jiddiy e'tibor kerak", a.left + pad, a.bottom - pad)
    ctx.restore()
  }
}

// the class name beside each bubble — biggest classes first; a name that would cover
// another name or bubble is left out (hover still tells it)
const names = {
  id: 'bubbleNames',
  afterDatasetsDraw(chart) {
    const meta = chart.getDatasetMeta(0)
    if (!meta?.data) return
    const { ctx, chartArea: area } = chart
    ctx.save()
    ctx.font = '600 10px Inter, sans-serif'
    ctx.fillStyle = colors.value.textPrimary
    ctx.textAlign = 'left'
    ctx.textBaseline = 'middle'
    const circles = meta.data.map(el => ({
      x: el.x,
      y: el.y,
      r: el.options?.radius ?? 8
    }))
    const taken = []
    const hits = (a, b) =>
      a.x < b.x + b.w && a.x + a.w > b.x && a.y < b.y + b.h && a.y + a.h > b.y
    const order = meta.data
      .map((_, i) => i)
      .sort((i, j) => circles[j].r - circles[i].r)
    for (const i of order) {
      const c = points.value[i]
      if (!c) continue
      const el = circles[i]
      const w = ctx.measureText(c.name).width
      const box = { x: el.x + el.r + 3, y: el.y - 6, w, h: 12 }
      if (box.x + w > area.right) box.x = el.x - el.r - 3 - w
      const overBubble = circles.some(
        (o, k) =>
          k !== i &&
          box.x < o.x + o.r &&
          box.x + w > o.x - o.r &&
          box.y < o.y + o.r &&
          box.y + box.h > o.y - o.r
      )
      if (overBubble || taken.some(t => hits(box, t))) continue
      taken.push(box)
      ctx.fillText(c.name, box.x, el.y)
    }
    ctx.restore()
  }
}
</script>

<style scoped>
.bubbles {
  position: relative;
  width: 100%;
  height: var(--bubble-h);
}

.bubbles__foot b {
  color: var(--text-primary);
  font-weight: 600;
}
</style>

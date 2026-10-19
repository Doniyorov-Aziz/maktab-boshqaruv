<template>
  <q-dialog
    :model-value="modelValue"
    :maximized="$q.screen.lt.md"
    @update:model-value="v => emit('update:modelValue', v)"
  >
    <q-card class="app-dialog app-dialog--xl cmp" data-compare>
      <q-card-section class="row items-center q-pb-none">
        <div class="text-h6">Solishtirish</div>
        <span v-for="(c, i) in data || []" :key="c.id" class="cmp__name" :style="{ '--c': colors[i] }">
          {{ c.name }}
        </span>
        <q-space />
        <AppButton variant="ghost" icon="close" aria-label="Yopish" v-close-popup />
      </q-card-section>
      <q-card-section v-if="!data" class="cmp__skel">
        <div v-for="i in 3" :key="i" class="skel cmp__skel-row" />
      </q-card-section>
      <q-card-section v-else class="cmp__body">
        <div class="cmp__metrics">
          <div v-for="m in METRICS" :key="m.key" class="cmp__metric" :data-metric="m.key">
            <small>{{ m.label }}</small>
            <div v-for="(c, i) in data" :key="c.id" class="cmp__row">
              <span class="cmp__label">{{ c.name }}</span>
              <span class="cmp__bar"
                ><span :style="{ width: `${c[m.key] || 0}%`, background: colors[i] }"
              /></span>
              <b :class="{ 'cmp__best': best(m.key) === c.id }">{{ formatPercent(c[m.key]) }}</b>
            </div>
          </div>
        </div>
        <div class="cmp__charts">
          <figure class="cmp__chart">
            <figcaption>Fanlar bo'yicha sifat (shu chorak)</figcaption>
            <div class="cmp__canvas">
              <Bar :key="'b' + themeKey" :data="subjectsData" :options="barOptions" />
            </div>
          </figure>
          <figure class="cmp__chart">
            <figcaption>Davomat dinamikasi — oxirgi {{ data[0].daily.length }} ish kuni</figcaption>
            <div class="cmp__canvas">
              <Line :key="'l' + themeKey" :data="dailyData" :options="lineOptions" />
            </div>
          </figure>
        </div>
      </q-card-section>
    </q-card>
  </q-dialog>
</template>

<script setup>
// Two or three classes side by side: attendance, mastery and quality as bars (the best
// one bold), quality by subject as grouped bars, and 20 school days of attendance as one
// line each — every class keeps its colour throughout.
import { computed, ref, watch } from 'vue'
import { useQuasar } from 'quasar'
import { Bar, Line } from 'vue-chartjs'
import {
  BarElement,
  CategoryScale,
  Chart,
  Legend,
  LineElement,
  LinearScale,
  PointElement,
  Tooltip
} from 'chart.js'
import AppButton from '@/components/app/AppButton.vue'
import { api } from '@/boot/axios'
import { useSchoolStore } from '@/stores/school'
import { token, useChartTheme } from '@/charts/chartTheme'
import { formatPercent } from '@/utils/format'

Chart.register(BarElement, CategoryScale, LinearScale, LineElement, PointElement, Tooltip, Legend)

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  ids: { type: Array, default: () => [] }
})
const emit = defineEmits(['update:modelValue'])
const $q = useQuasar()
const school = useSchoolStore()
const { themeKey } = useChartTheme()

const METRICS = [
  { key: 'attendance', label: 'Davomat (30 kun)' },
  { key: 'mastery', label: "O'zlashtirish (chorak)" },
  { key: 'quality', label: 'Sifat (chorak)' }
]
const TOKENS = ['--subj-1', '--subj-3', '--subj-4']

const data = ref(null)
// re-read when the theme flips (themeKey changes)
const colors = computed(() => {
  void themeKey.value
  return TOKENS.map(t => token(t))
})

watch(
  () => [props.modelValue, props.ids.join(',')],
  async ([open]) => {
    data.value = null
    if (!open || props.ids.length < 2) return
    const res = await api.get('/api/pages/classes/compare', {
      params: { schoolId: school.activeSchoolId, ids: props.ids.join(',') }
    })
    data.value = res.data
  },
  { immediate: true }
)

function best(key) {
  let top = null
  for (const c of data.value || []) if (c[key] !== null && (!top || c[key] > top[key])) top = c
  return top?.id
}

const subjectsData = computed(() => {
  const names = [...new Set((data.value || []).flatMap(c => c.subjects.map(s => s.subject)))].sort()
  return {
    labels: names,
    datasets: (data.value || []).map((c, i) => ({
      label: c.name,
      data: names.map(n => c.subjects.find(s => s.subject === n)?.quality ?? null),
      backgroundColor: colors.value[i],
      borderRadius: 4,
      maxBarThickness: 18
    }))
  }
})
const dailyData = computed(() => {
  const days = data.value?.[0]?.daily || []
  return {
    labels: days.map(d => `${Number(d.date.slice(8))}.${d.date.slice(5, 7)}`),
    datasets: (data.value || []).map((c, i) => ({
      label: c.name,
      data: c.daily.map(d => d.rate),
      borderColor: colors.value[i],
      backgroundColor: colors.value[i],
      borderWidth: 2,
      pointRadius: 0,
      pointHoverRadius: 4,
      tension: 0.3,
      cubicInterpolationMode: 'monotone',
      spanGaps: true
    }))
  }
})
const legend = { display: true, position: 'bottom', labels: { boxWidth: 10, boxHeight: 10 } }
const barOptions = computed(() => ({
  plugins: { legend, tooltip: { callbacks: { label: i => `${i.dataset.label}: ${formatPercent(i.raw)}` } } },
  scales: { y: { min: 0, max: 100, ticks: { callback: v => `${v}%` } } }
}))
const lineOptions = computed(() => ({
  interaction: { mode: 'index', intersect: false },
  plugins: { legend, tooltip: { callbacks: { label: i => `${i.dataset.label}: ${formatPercent(i.raw)}` } } },
  scales: { y: { suggestedMin: 80, max: 100, ticks: { callback: v => `${v}%` } } }
}))
</script>

<style scoped>
.cmp__name {
  margin-left: var(--space-3);
  padding: 0 var(--space-2);
  border-radius: var(--radius-pill);
  background: color-mix(in srgb, var(--c) 16%, transparent);
  color: var(--c);
  font-weight: 700;
}

.cmp__body {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}

.cmp__metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--space-4);
}

.cmp__metric small {
  display: block;
  margin-bottom: var(--space-2);
  font-size: var(--text-xs);
  font-weight: 700;
  color: var(--brand-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.cmp__row {
  display: grid;
  grid-template-columns: var(--tile-md) minmax(0, 1fr) var(--space-8);
  gap: var(--space-2);
  align-items: center;
  margin-bottom: var(--space-1);
  font-size: var(--text-sm);
}

.cmp__label {
  font-weight: 700;
}

.cmp__bar {
  height: var(--bar-thin);
  border-radius: var(--radius-pill);
  background: var(--tile-none);
  overflow: hidden;
}

.cmp__bar span {
  display: block;
  height: 100%;
  border-radius: var(--radius-pill);
}

.cmp__row b {
  text-align: right;
  font-variant-numeric: tabular-nums;
  font-weight: 500;
}

.cmp__row b.cmp__best {
  font-weight: 800;
}

.cmp__charts {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-4);
}

.cmp__chart {
  margin: 0;
}

.cmp__chart figcaption {
  margin-bottom: var(--space-2);
  font-size: var(--text-sm);
  font-weight: 600;
}

.cmp__canvas {
  position: relative;
  height: var(--chart-h);
}

.cmp__skel {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.cmp__skel-row {
  height: var(--space-8);
}

@media (max-width: 1023px) {
  .cmp__metrics,
  .cmp__charts {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>

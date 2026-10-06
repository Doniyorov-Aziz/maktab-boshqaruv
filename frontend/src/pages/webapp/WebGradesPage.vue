<template>
  <div>
    <div v-if="error" class="wa-card">
      <WaState
        kind="error"
        :text="t('error')"
        :retry-text="t('retry')"
        @retry="retry"
      />
    </div>
    <template v-else-if="!data">
      <div class="stats"
        ><div v-for="i in 3" :key="i" class="wa-card"
          ><q-skeleton height="52px" /></div
      ></div>
      <div class="wa-card"><q-skeleton height="200px" /></div>
    </template>
    <div
      v-else-if="!data.subjects.length && !data.recent.length"
      class="wa-card"
    >
      <WaState :text="t('no_grades')" />
    </div>
    <template v-else>
      <div class="stats">
        <div class="wa-card stat">
          <div class="stat-value" :style="{ color: averageColor(overall) }">{{
            overall.toFixed(2)
          }}</div>
          <div class="wa-hint">{{ t('overall_avg') }}</div>
        </div>
        <div class="wa-card stat">
          <div class="stat-value">{{ totalCount }}</div>
          <div class="wa-hint">{{ t('grades_count') }}</div>
        </div>
        <div class="wa-card stat">
          <div class="stat-best ellipsis">
            🏆 {{ best ? best.subject : '—' }}
          </div>
          <div class="wa-hint">{{ t('best_subject') }}</div>
        </div>
      </div>

      <div class="wa-card">
        <div class="wa-card-title">📈 {{ t('trend_30') }}</div>
        <WaState v-if="data.trend.length < 2" :text="t('trend_empty')" />
        <div v-else class="chart-box">
          <Line :data="lineData" :options="lineOptions" />
        </div>
      </div>

      <div v-if="data.subjects.length" class="wa-card">
        <div class="wa-card-title">👥 {{ t('class_compare') }}</div>
        <div class="legend wa-hint">
          <span><i class="sw sw-child" /> {{ t('child_avg') }}</span>
          <span><i class="sw sw-class" /> {{ t('class_avg') }}</span>
        </div>
        <div v-for="a in data.subjects" :key="a.subjectId" class="cmp">
          <button
            class="cmp-head"
            :class="{ open: openSubject === a.subjectId }"
            @click="toggle(a.subjectId)"
          >
            <span class="col ellipsis"
              >{{ subjectIcon(a.subject) }} {{ a.subject }}</span
            >
            <span class="trend" :class="a.trend">{{ arrow(a.trend) }}</span>
            <span class="avg" :style="{ color: averageColor(a.average) }">{{
              a.average.toFixed(1)
            }}</span>
          </button>
          <div class="cmp-track">
            <div
              class="cmp-child"
              :style="{
                width: pct(a.average),
                background: averageColor(a.average)
              }"
            />
            <div
              v-if="classAvg(a) != null"
              class="cmp-class"
              :style="{ left: pct(classAvg(a)) }"
              :title="classAvg(a).toFixed(2)"
            />
          </div>
          <div v-if="classAvg(a) != null" class="cmp-note wa-hint">
            {{ t('class_avg') }}: {{ classAvg(a).toFixed(1) }}
            <b :class="a.average >= classAvg(a) ? 'up' : 'down'">
              {{ a.average >= classAvg(a) ? '▲' : '▼' }}
              {{ Math.abs(a.average - classAvg(a)).toFixed(1) }}
            </b>
          </div>
          <div v-if="openSubject === a.subjectId" class="detail">
            <q-skeleton v-if="!detail" type="text" />
            <template v-else>
              <div class="wa-hint q-mb-xs"
                >{{ t('teacher') }}: {{ detail.teacher || '—' }}</div
              >
              <div class="chips">
                <span
                  v-for="g in detail.grades"
                  :key="g.id"
                  class="chip"
                  :style="{ background: scoreColor(g.score) }"
                  :title="fullDate(state.lang, g.date)"
                  >{{ g.score }}</span
                >
              </div>
            </template>
          </div>
        </div>
        <div class="wa-hint privacy">🔒 {{ t('compare_note') }}</div>
      </div>

      <div class="wa-card">
        <div class="wa-card-title">{{ t('recent') }}</div>
        <WaState v-if="!data.recent.length" :text="t('no_grades')" />
        <div v-for="g in data.recent.slice(0, 10)" :key="g.id" class="wa-row">
          <div class="wa-score" :style="{ background: scoreColor(g.score) }">{{
            g.score
          }}</div>
          <div class="col">
            <div class="text-weight-semibold"
              >{{ subjectIcon(g.subject) }} {{ g.subject }}</div
            >
            <div class="wa-hint">
              {{ shortDate(state.lang, g.date) }} · {{ t('grade_' + g.type) }}
              <template v-if="g.comment"> · «{{ g.comment }}»</template>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useQuasar } from 'quasar'
import { Line } from 'vue-chartjs'
import {
  Chart as ChartJS,
  Tooltip,
  LineElement,
  PointElement,
  CategoryScale,
  LinearScale,
  Filler
} from 'chart.js'
import { parentApi } from '@/webapp/api'
import { state, t } from '@/webapp/state'
import { fullDate, shortDate } from '@/webapp/i18n'
import { subjectIcon, scoreColor, averageColor } from '@/webapp/icons'
import { hapticSelect } from '@/webapp/telegram'
import { useLoad } from '@/webapp/useLoad'
import WaState from '@/components/webapp/WaState.vue'

ChartJS.register(
  Tooltip,
  LineElement,
  PointElement,
  CategoryScale,
  LinearScale,
  Filler
)

const $q = useQuasar()
const openSubject = ref(null)
const detail = ref(null)

const { data, error, retry } = useLoad(
  async () =>
    (await parentApi.get(`/api/parent/students/${state.childId}/grades`)).data
)

const arrow = trend => ({ UP: '↑', DOWN: '↓', FLAT: '→' })[trend] || ''
const pct = v => Math.max(0, Math.min(100, (v / 5) * 100)) + '%'
const classAvg = a => data.value?.classAverages?.[a.subjectId] ?? null

const totalCount = computed(() =>
  data.value.subjects.reduce((n, a) => n + a.count, 0)
)
const overall = computed(() => {
  const subjects = data.value.subjects
  const n = totalCount.value
  return n ? subjects.reduce((s, a) => s + a.average * a.count, 0) / n : 0
})
const best = computed(() =>
  data.value.subjects.reduce(
    (b, a) => (!b || a.average > b.average ? a : b),
    null
  )
)

const lineData = computed(() => ({
  labels: data.value.trend.map(p => shortDate(state.lang, p.date)),
  datasets: [
    {
      data: data.value.trend.map(p => p.average),
      borderColor: '#6366f1',
      borderWidth: 3,
      tension: 0.35,
      cubicInterpolationMode: 'monotone',
      pointRadius: 3.5,
      pointBackgroundColor: data.value.trend.map(p => averageColor(p.average)),
      pointBorderWidth: 0,
      fill: true,
      backgroundColor: ctx => {
        const { chart } = ctx
        if (!chart.chartArea) return 'rgba(99,102,241,0.15)'
        const g = chart.ctx.createLinearGradient(
          0,
          chart.chartArea.top,
          0,
          chart.chartArea.bottom
        )
        g.addColorStop(0, 'rgba(99,102,241,0.35)')
        g.addColorStop(1, 'rgba(99,102,241,0)')
        return g
      }
    }
  ]
}))

const lineOptions = computed(() => {
  const text = $q.dark.isActive ? '#cbd5e1' : '#475569'
  return {
    responsive: true,
    maintainAspectRatio: false,
    animation: { duration: 500 },
    plugins: {
      legend: { display: false },
      tooltip: { callbacks: { label: c => ' ' + Number(c.raw).toFixed(2) } }
    },
    scales: {
      y: {
        min: 1,
        max: 5,
        ticks: { stepSize: 1, color: text },
        grid: { color: 'rgba(148,163,184,0.18)' }
      },
      x: {
        ticks: {
          color: text,
          maxRotation: 0,
          autoSkip: true,
          maxTicksLimit: 5,
          font: { size: 11 }
        },
        grid: { display: false }
      }
    }
  }
})

async function toggle(subjectId) {
  hapticSelect()
  if (openSubject.value === subjectId) {
    openSubject.value = null
    detail.value = null
    return
  }
  openSubject.value = subjectId
  detail.value = null
  detail.value = (
    await parentApi.get(
      `/api/parent/students/${state.childId}/grades/${subjectId}`
    )
  ).data
}
</script>

<style scoped>
.stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.stat {
  padding: 12px 10px;
  text-align: center;
  min-width: 0;
}

.stat-value {
  font-size: 24px;
  font-weight: 800;
  line-height: 1.2;
}

.stat-best {
  font-size: 15px;
  font-weight: 800;
  line-height: 1.25;
  padding: 4px 0 6px;
}

.stat-emoji {
  font-size: 22px;
}

.ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chart-box {
  height: 190px;
}

.legend {
  display: flex;
  gap: 16px;
  margin-bottom: 6px;
}

.sw {
  display: inline-block;
  vertical-align: middle;
  margin-right: 4px;
}

.sw-child {
  width: 14px;
  height: 8px;
  border-radius: 4px;
  background: var(--wa-accent);
}

.sw-class {
  width: 3px;
  height: 12px;
  border-radius: 2px;
  background: var(--wa-text);
}

.cmp {
  padding: 8px 0;
  border-bottom: 1px solid var(--wa-border);
}

.cmp:last-of-type {
  border-bottom: 0;
}

.cmp-head {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 0;
  background: transparent;
  border: 0;
  color: var(--wa-text);
  font: inherit;
  font-weight: 600;
  text-align: left;
  cursor: pointer;
  min-width: 0;
}

.cmp-head.open {
  color: var(--wa-accent);
}

.cmp-track {
  position: relative;
  height: 10px;
  border-radius: 99px;
  background: var(--wa-border);
  margin-top: 6px;
}

.cmp-child {
  height: 100%;
  border-radius: 99px;
  transition: width 0.6s ease;
}

.cmp-class {
  position: absolute;
  top: -3px;
  width: 3px;
  height: 16px;
  margin-left: -1.5px;
  border-radius: 2px;
  background: var(--wa-text);
}

.cmp-note {
  margin-top: 4px;
  font-size: 12px;
}

.cmp-note .up {
  color: var(--wa-good);
}

.cmp-note .down {
  color: var(--wa-warn);
}

.avg {
  font-weight: 800;
  width: 32px;
  text-align: right;
}

.trend {
  font-weight: 800;
}

.trend.UP {
  color: var(--wa-good);
}

.trend.DOWN {
  color: var(--wa-bad);
}

.trend.FLAT {
  color: var(--wa-hint);
}

.detail {
  padding-top: 8px;
}

.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
}

.chip {
  width: 26px;
  height: 26px;
  border-radius: 8px;
  color: #fff;
  font-weight: 800;
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.privacy {
  margin-top: 10px;
  font-size: 12px;
}
</style>

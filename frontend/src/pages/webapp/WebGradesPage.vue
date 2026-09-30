<template>
  <div>
    <div v-if="!data" class="wa-card"><q-skeleton height="240px" /></div>
    <template v-else>
      <div class="wa-card">
        <div class="wa-card-title">{{ t('averages') }}</div>
        <div v-if="!data.subjects.length" class="wa-empty">{{
          t('no_grades')
        }}</div>
        <div
          v-else
          class="chart-box"
          :style="{ height: Math.max(160, data.subjects.length * 42) + 'px' }"
        >
          <Bar :data="chartData" :options="chartOptions" />
        </div>
      </div>

      <div v-if="data.subjects.length" class="wa-card">
        <button
          v-for="a in data.subjects"
          :key="a.subjectId"
          class="subject-row"
          :class="{ open: openSubject === a.subjectId }"
          @click="toggle(a.subjectId)"
        >
          <span class="col text-left"
            >{{ subjectIcon(a.subject) }} {{ a.subject }}</span
          >
          <span class="trend" :class="a.trend">{{ arrow(a.trend) }}</span>
          <span class="avg" :style="{ color: averageColor(a.average) }">{{
            a.average.toFixed(1)
          }}</span>
        </button>
        <div v-if="detail" class="detail">
          <div class="wa-hint q-mb-xs">
            {{ t('teacher') }}: {{ detail.teacher || '—' }} ·
            {{ t('average') }}: <b>{{ detail.average.toFixed(2) }}</b>
          </div>
          <div v-for="g in detail.grades" :key="g.id" class="wa-row">
            <div
              class="wa-score"
              :style="{ background: scoreColor(g.score) }"
              >{{ g.score }}</div
            >
            <div class="col">
              <div>{{ fullDate(state.lang, g.date) }}</div>
              <div class="wa-hint"
                >{{ t('grade_' + g.type)
                }}<template v-if="g.comment">
                  · «{{ g.comment }}»</template
                ></div
              >
            </div>
          </div>
        </div>
      </div>

      <div class="wa-card">
        <div class="wa-card-title">{{ t('recent') }}</div>
        <div v-if="!data.recent.length" class="wa-empty">{{
          t('no_grades')
        }}</div>
        <div v-for="g in data.recent.slice(0, 12)" :key="g.id" class="wa-row">
          <div class="wa-score" :style="{ background: scoreColor(g.score) }">{{
            g.score
          }}</div>
          <div class="col">
            <div class="text-weight-semibold"
              >{{ subjectIcon(g.subject) }} {{ g.subject }}</div
            >
            <div class="wa-hint"
              >{{ shortDate(state.lang, g.date) }} ·
              {{ t('grade_' + g.type) }}</div
            >
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useQuasar } from 'quasar'
import { Bar } from 'vue-chartjs'
import {
  Chart as ChartJS,
  Tooltip,
  BarElement,
  CategoryScale,
  LinearScale
} from 'chart.js'
import { parentApi } from '@/webapp/api'
import { state, t } from '@/webapp/state'
import { fullDate, shortDate } from '@/webapp/i18n'
import { subjectIcon, scoreColor, averageColor } from '@/webapp/icons'
import { haptic } from '@/webapp/telegram'

ChartJS.register(Tooltip, BarElement, CategoryScale, LinearScale)

const $q = useQuasar()
const data = ref(null)
const openSubject = ref(null)
const detail = ref(null)

const arrow = trend => ({ UP: '↑', DOWN: '↓', FLAT: '→' })[trend] || ''

const chartData = computed(() => ({
  labels: data.value.subjects.map(a => a.subject),
  datasets: [
    {
      data: data.value.subjects.map(a => a.average),
      backgroundColor: data.value.subjects.map(a => averageColor(a.average)),
      borderRadius: 8,
      barThickness: 18
    }
  ]
}))

const chartOptions = computed(() => {
  const text = $q.dark.isActive ? '#e2e8f0' : '#334155'
  return {
    indexAxis: 'y',
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { display: false } },
    scales: {
      x: {
        min: 0,
        max: 5,
        ticks: { stepSize: 1, color: text },
        grid: { color: 'rgba(148,163,184,0.18)' }
      },
      y: {
        ticks: { color: text, font: { size: 12 } },
        grid: { display: false }
      }
    }
  }
})

async function toggle(subjectId) {
  haptic()
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

onMounted(async () => {
  data.value = (
    await parentApi.get(`/api/parent/students/${state.childId}/grades`)
  ).data
})
</script>

<style scoped>
.subject-row {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 10px 2px;
  background: transparent;
  border: 0;
  border-bottom: 1px solid var(--wa-border);
  color: var(--wa-text);
  font: inherit;
  cursor: pointer;
}

.subject-row.open {
  color: var(--wa-accent);
  font-weight: 700;
}

.avg {
  font-weight: 800;
  width: 34px;
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
  padding: 10px 0 0;
}
</style>

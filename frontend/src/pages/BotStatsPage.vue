<template>
  <page-layout
    icon="insights"
    color="#8b5cf6"
    title="Bot statistikasi"
    :subtitle="`Ota-onalar boti qanchalik ishlatilyapti · ${schoolStore.activeSchoolName || ''}`"
  >
    <template #actions>
      <q-btn
        flat
        round
        dense
        icon="refresh"
        aria-label="Yangilash"
        @click="load"
      >
        <q-tooltip>Yangilash</q-tooltip>
      </q-btn>
    </template>

    <div class="row q-col-gutter-md q-mb-md">
      <div v-for="k in kpis" :key="k.label" class="col-6 col-md-3 col-xl">
        <div class="brand-card stat-card">
          <div class="row items-center no-wrap">
            <div
              class="stat-card__icon q-mr-md gt-xs"
              :style="{ background: `${k.color}1f`, color: k.color }"
            >
              <q-icon :name="k.icon" size="24px" />
            </div>
            <div class="col" style="min-width: 0">
              <div class="text-h5 text-weight-bold tabular-nums">
                <q-skeleton v-if="!stats" type="text" width="48px" />
                <template v-else>{{ k.value }}</template>
              </div>
              <div class="text-caption muted-text kpi-label">{{ k.label }}</div>
              <div
                v-if="stats && k.hint"
                class="text-caption muted-text kpi-label"
                >{{ k.hint }}</div
              >
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="row q-col-gutter-md q-mb-md">
      <div class="col-12 col-lg-7">
        <div class="brand-card q-pa-md chart-card">
          <div class="row items-center q-mb-sm">
            <div class="text-subtitle1 text-weight-semibold"
              >Xabarlar (30 kun)</div
            >
            <q-space />
            <span v-if="stats" class="text-caption muted-text tabular-nums">
              {{ stats.messages30 }} navbatga ·
              {{ stats.delivered30 }} yetkazildi · {{ stats.failed30 }} xato
            </span>
          </div>
          <div class="chart-box">
            <q-skeleton v-if="!stats" type="rect" class="full-height" />
            <Line v-else :data="sentChartData" :options="sentChartOptions" />
          </div>
        </div>
      </div>
      <div class="col-12 col-lg-5">
        <div class="brand-card q-pa-md chart-card">
          <div class="text-subtitle1 text-weight-semibold q-mb-sm"
            >Eng ko'p ochiladigan bo'limlar (30 kun)</div
          >
          <div
            v-if="stats && !stats.sections.length"
            class="muted-text q-py-xl text-center"
          >
            Hali hech kim botni ochmagan
          </div>
          <div v-else class="chart-box">
            <q-skeleton v-if="!stats" type="rect" class="full-height" />
            <Bar
              v-else
              :data="sectionChartData"
              :options="sectionChartOptions"
            />
          </div>
        </div>
      </div>
    </div>

    <div class="brand-card q-pa-md q-mb-md">
      <div class="row items-center q-mb-sm">
        <div class="text-subtitle1 text-weight-semibold"
          >Sinflar bo'yicha botga ulanish (%)</div
        >
        <q-space />
        <span class="text-caption muted-text"
          >Ulanish foizi past sinflar birinchi</span
        >
      </div>
      <div class="chart-box">
        <q-skeleton v-if="!stats" type="rect" class="full-height" />
        <Bar v-else :data="classChartData" :options="classChartOptions" />
      </div>
    </div>

    <div class="brand-card overflow-hidden">
      <div class="q-pa-md row items-center">
        <div class="text-subtitle1 text-weight-semibold"
          >Sinflar bo'yicha ulangan ota-onalar</div
        >
        <q-space />
        <span class="text-caption muted-text"
          >Past foizli sinflarga QR varaqlarni tarqating</span
        >
      </div>
      <q-table
        :rows="stats?.classes || []"
        :columns="columns"
        row-key="classId"
        :loading="!stats"
        flat
        dense
        class="brand-table"
        :pagination="{ rowsPerPage: 0, sortBy: 'percent', descending: false }"
        hide-pagination
      >
        <template v-slot:body-cell-className="props">
          <q-td :props="props">
            <router-link
              :to="`/profiles/class/${props.row.classId}`"
              class="class-link"
            >
              {{ props.row.className }}</router-link
            >
          </q-td>
        </template>
        <template v-slot:body-cell-percent="props">
          <q-td :props="props" style="min-width: 220px">
            <div class="row items-center no-wrap q-gutter-x-sm">
              <q-linear-progress
                :value="(props.row.percent || 0) / 100"
                :color="coverageColor(props.row.percent)"
                :track-color="$q.dark.isActive ? 'grey-9' : 'grey-3'"
                rounded
                size="10px"
                class="col"
              />
              <span class="tabular-nums text-weight-semibold percent-label"
                >{{ props.row.percent ?? 0 }}%</span
              >
            </div>
          </q-td>
        </template>
        <template v-slot:body-cell-actions="props">
          <q-td :props="props" class="text-right">
            <q-btn
              flat
              dense
              no-caps
              size="sm"
              icon="qr_code_2"
              label="QR varaq"
              color="primary"
              :to="`/print/class-qr/${props.row.classId}`"
            />
          </q-td>
        </template>
      </q-table>
    </div>
  </page-layout>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useQuasar } from 'quasar'
import { Bar, Line } from 'vue-chartjs'
import {
  Chart as ChartJS,
  Tooltip,
  Legend,
  BarElement,
  LineElement,
  PointElement,
  CategoryScale,
  LinearScale
} from 'chart.js'
import { api } from '@/boot/axios'
import PageLayout from '@/components/PageLayout.vue'
import { useSchoolStore } from '@/stores/school'
import { formatShortDate } from '@/utils/date'
import { botSections } from '@/utils/telegram'

ChartJS.register(
  Tooltip,
  Legend,
  BarElement,
  LineElement,
  PointElement,
  CategoryScale,
  LinearScale
)

function replyTime(minutes) {
  if (minutes == null) return '—'
  if (minutes < 1) return '1 daqiqadan kam'
  if (minutes < 60) return `${Math.round(minutes)} daq`
  const h = Math.floor(minutes / 60)
  const m = Math.round(minutes % 60)
  if (h < 24) return m ? `${h} soat ${m} daq` : `${h} soat`
  return `${Math.round(h / 24)} kun`
}

const $q = useQuasar()
const schoolStore = useSchoolStore()
const stats = ref(null)

const kpis = computed(() => {
  const s = stats.value || {}
  return [
    {
      label: 'Ulangan ota-onalar',
      value: s.linkedPercent != null ? `${s.linkedPercent}%` : '—',
      hint: `${s.linkedStudents ?? 0} / ${s.totalStudents ?? 0} o'quvchi`,
      icon: 'family_restroom',
      color: '#229ed9'
    },
    {
      label: 'Obunachilar',
      value: s.parentCount ?? 0,
      hint: 'Telegram chatlar',
      icon: 'send',
      color: '#8b5cf6'
    },
    {
      label: 'Faol (7 kun)',
      value: s.active7 ?? 0,
      hint: 'botni ochgan ota-onalar',
      icon: 'bolt',
      color: '#10b981'
    },
    {
      label: 'Faol (30 kun)',
      value: s.active30 ?? 0,
      hint: null,
      icon: 'trending_up',
      color: '#0ea5e9'
    },
    {
      label: 'Yangi murojaatlar',
      value: s.newMessages ?? 0,
      hint: null,
      icon: 'forum',
      color: '#f59e0b'
    },
    {
      label: 'Murojaatlar (30 kun)',
      value: s.appeals30 ?? 0,
      hint: `o'rtacha javob: ${replyTime(s.avgReplyMinutes)}`,
      icon: 'mark_chat_unread',
      color: '#ec4899'
    },
    {
      label: 'Kutilayotgan arizalar',
      value: s.pendingAbsences ?? 0,
      hint: null,
      icon: 'medical_information',
      color: '#ef4444'
    }
  ]
})

const columns = [
  {
    name: 'className',
    label: 'Sinf',
    field: 'className',
    align: 'left',
    sortable: true
  },
  {
    name: 'students',
    label: "O'quvchilar",
    field: 'students',
    align: 'right',
    sortable: true
  },
  {
    name: 'linked',
    label: 'Ulangan',
    field: 'linked',
    align: 'right',
    sortable: true
  },
  {
    name: 'percent',
    label: 'Qamrov',
    field: 'percent',
    align: 'left',
    sortable: true
  },
  { name: 'actions', label: '', field: 'classId', align: 'right' }
]

function coverageColor(p) {
  if (p == null || p < 30) return 'negative'
  if (p < 70) return 'warning'
  return 'positive'
}

const textColor = computed(() => ($q.dark.isActive ? '#f1f5f9' : '#334155'))
const gridColor = computed(() =>
  $q.dark.isActive ? 'rgba(148,163,184,0.16)' : 'rgba(148,163,184,0.18)'
)

const sentChartData = computed(() => {
  const days = stats.value?.messagesPerDay || []
  const line = (label, key, color, dash) => ({
    label,
    data: days.map(d => d[key]),
    borderColor: color,
    backgroundColor: color,
    borderDash: dash || [],
    tension: 0.3,
    pointRadius: 2,
    borderWidth: 2
  })
  return {
    labels: days.map(d => formatShortDate(d.date)),
    datasets: [
      line("Navbatga qo'yilgan", 'queued', '#8b5cf6', [6, 4]),
      line('Yetkazilgan', 'delivered', '#10b981'),
      line('Xato', 'failed', '#ef4444')
    ]
  }
})

const sentChartOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  interaction: { mode: 'index', intersect: false },
  plugins: {
    legend: {
      position: 'bottom',
      labels: {
        color: textColor.value,
        font: { family: 'Inter' },
        boxWidth: 12
      }
    }
  },
  scales: {
    x: {
      ticks: {
        color: textColor.value,
        font: { family: 'Inter' },
        maxTicksLimit: 10
      },
      grid: { display: false }
    },
    y: {
      beginAtZero: true,
      ticks: {
        color: textColor.value,
        font: { family: 'Inter' },
        precision: 0
      },
      grid: { color: gridColor.value }
    }
  }
}))

const coverageHex = p =>
  p == null || p < 30 ? '#ef4444' : p < 70 ? '#f59e0b' : '#10b981'

const classChartData = computed(() => {
  const rows = stats.value?.classes || []
  return {
    labels: rows.map(c => c.className),
    datasets: [
      {
        label: 'Ulangan, %',
        data: rows.map(c => c.percent ?? 0),
        backgroundColor: rows.map(c => coverageHex(c.percent)),
        borderRadius: 6,
        maxBarThickness: 28
      }
    ]
  }
})

const classChartOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { display: false },
    tooltip: {
      callbacks: {
        label: ctx => {
          const c = stats.value.classes[ctx.dataIndex]
          return ` ${c.percent ?? 0}% · ${c.linked}/${c.students} o'quvchi`
        }
      }
    }
  },
  scales: {
    x: {
      ticks: { color: textColor.value, font: { family: 'Inter' } },
      grid: { display: false }
    },
    y: {
      beginAtZero: true,
      max: 100,
      ticks: {
        color: textColor.value,
        font: { family: 'Inter' },
        callback: v => `${v}%`
      },
      grid: { color: gridColor.value }
    }
  }
}))

const sectionChartData = computed(() => {
  const top = (stats.value?.sections || []).slice(0, 8)
  return {
    labels: top.map(s => botSections[s.section] || s.section),
    datasets: [
      {
        label: 'Ochilgan',
        data: top.map(s => s.count),
        backgroundColor: '#8b5cf6',
        borderRadius: 6,
        barThickness: 16
      }
    ]
  }
})

const sectionChartOptions = computed(() => ({
  indexAxis: 'y',
  responsive: true,
  maintainAspectRatio: false,
  plugins: { legend: { display: false } },
  scales: {
    x: {
      beginAtZero: true,
      ticks: {
        color: textColor.value,
        font: { family: 'Inter' },
        precision: 0
      },
      grid: { color: gridColor.value }
    },
    y: {
      ticks: { color: textColor.value, font: { family: 'Inter' } },
      grid: { display: false }
    }
  }
}))

async function load() {
  if (!schoolStore.activeSchoolId) return
  stats.value = null
  try {
    stats.value = (
      await api.get('/api/bot/stats', {
        params: { schoolId: schoolStore.activeSchoolId }
      })
    ).data
  } catch (error) {
    $q.notify({
      type: 'negative',
      message: error.friendlyMessage || 'Xatolik yuz berdi'
    })
  }
}

watch(() => schoolStore.activeSchoolId, load, { immediate: true })
</script>

<style scoped>
.muted-text {
  color: var(--brand-text-muted);
}

.kpi-label {
  line-height: 1.3;
}

.chart-card {
  height: 100%;
}

.chart-box {
  height: 280px;
}

.class-link {
  color: var(--text-primary);
  font-weight: 600;
  text-decoration: none;
}

.class-link:hover {
  color: var(--color-brand);
}

.percent-label {
  width: 48px;
  text-align: right;
}
</style>

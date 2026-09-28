<template>
  <q-page class="q-pa-md q-pa-lg-md dashboard-page">
    <div class="row items-center justify-between q-mb-lg">
      <div>
        <div class="text-h5 text-weight-bold"
          >Xush kelibsiz, {{ authStore.username }} 👋</div
        >
        <div class="text-body2 muted-text q-mt-xs">
          Tizimning umumiy holati va asosiy ko'rsatkichlar
        </div>
      </div>
      <q-badge :color="roleColor" outline class="q-px-sm q-py-xs">
        {{ authStore.role }}
      </q-badge>
    </div>

    <div class="row q-col-gutter-md q-mb-lg">
      <div
        v-for="(stat, index) in primaryStats"
        :key="stat.key"
        class="col-6 col-sm-4 col-md-3 stat-card-enter"
        :style="{ animationDelay: `${index * 60}ms` }"
      >
        <div class="stat-card brand-card full-height">
          <div class="row items-center justify-between q-mb-sm">
            <div class="stat-card__icon" :style="{ background: stat.bg }">
              <q-icon :name="stat.icon" :color="stat.color" size="24px" />
            </div>
            <q-icon
              v-if="!loading"
              name="north_east"
              size="16px"
              class="muted-text cursor-pointer"
              @click="router.push(`/app/${stat.key}`)"
            />
          </div>
          <div class="text-h4 text-weight-bold">
            <q-skeleton v-if="loading" type="text" width="50px" />
            <template v-else>{{ stat.count }}</template>
          </div>
          <div class="text-caption muted-text">{{ stat.title }}</div>
        </div>
      </div>
    </div>

    <div class="row q-col-gutter-md items-stretch">
      <div class="col-12 col-md-7">
        <div class="brand-card q-pa-md full-height">
          <div class="text-subtitle1 text-weight-semibold q-mb-md">
            Modullar bo'yicha yozuvlar soni
          </div>
          <div :style="{ height: chartHeight + 'px' }">
            <q-skeleton v-if="loading" type="rect" class="full-height" />
            <Bar
              v-else
              :data="chartData"
              :options="chartOptions"
              :plugins="[valueLabelPlugin]"
            />
          </div>
        </div>
      </div>

      <div class="col-12 col-md-5">
        <div class="brand-card q-pa-md full-height column">
          <div class="text-subtitle1 text-weight-semibold q-mb-md"
            >Tezkor kirish</div
          >
          <div
            class="row q-col-gutter-sm quick-access-scroll"
            :style="{ maxHeight: chartHeight + 'px' }"
          >
            <div v-for="item in quickLinks" :key="item.key" class="col-6">
              <div class="quick-tile" @click="router.push(`/app/${item.key}`)">
                <div class="row items-start justify-between no-wrap">
                  <div
                    class="quick-tile__icon"
                    :style="{ background: item.bg }"
                  >
                    <q-icon :name="item.icon" :color="item.color" size="20px" />
                  </div>
                  <q-btn
                    v-if="item.canCreate"
                    flat
                    dense
                    round
                    size="sm"
                    icon="add"
                    color="primary"
                    class="quick-tile__add"
                    @click.stop="onQuickAdd(item.key)"
                  >
                    <q-tooltip>Yangi qo'shish</q-tooltip>
                  </q-btn>
                </div>
                <div class="text-body2 text-weight-medium q-mt-sm ellipsis">{{
                  item.title
                }}</div>
                <div class="text-caption muted-text">
                  <q-skeleton v-if="loading" type="text" width="40px" />
                  <template v-else
                    >{{ counts[item.key] ?? 0 }} ta yozuv</template
                  >
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </q-page>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useQuasar } from 'quasar'
import { Bar } from 'vue-chartjs'
import {
  Chart as ChartJS,
  Title,
  Tooltip,
  BarElement,
  CategoryScale,
  LinearScale
} from 'chart.js'
import { api } from '@/boot/axios'
import { useAuthStore } from '@/stores/auth'
import { modules } from '@/config/modules'

ChartJS.register(Title, Tooltip, BarElement, CategoryScale, LinearScale)

const router = useRouter()
const $q = useQuasar()
const authStore = useAuthStore()
const loading = ref(true)
const counts = ref({})

const roleColor = computed(() => {
  if (authStore.role === 'ADMIN') return 'negative'
  if (authStore.role === 'EDITOR') return 'primary'
  return 'grey-7'
})

const visibleModules = computed(() =>
  modules.filter(m => !m.adminOnly || authStore.isAdmin)
)

const cardPalette = [
  { color: 'primary', bg: 'rgba(79, 70, 229, 0.12)' },
  { color: 'secondary', bg: 'rgba(14, 165, 233, 0.12)' },
  { color: 'positive', bg: 'rgba(16, 185, 129, 0.12)' },
  { color: 'warning', bg: 'rgba(245, 158, 11, 0.12)' }
]

const canCreate = m => (m.adminOnly ? authStore.isAdmin : authStore.isEditor)

const primaryStats = computed(() =>
  visibleModules.value.map((m, i) => ({
    key: m.key,
    title: m.title,
    icon: m.icon,
    count: counts.value[m.key] ?? 0,
    ...cardPalette[i % cardPalette.length]
  }))
)

const quickLinks = computed(() =>
  visibleModules.value.map((m, i) => ({
    key: m.key,
    title: m.title,
    icon: m.icon,
    canCreate: canCreate(m),
    ...cardPalette[i % cardPalette.length]
  }))
)

function onQuickAdd(key) {
  router.push(`/app/${key}?create=1`)
}

const sortedStats = computed(() =>
  [...visibleModules.value]
    .map(m => ({ title: m.title, count: counts.value[m.key] ?? 0 }))
    .sort((a, b) => b.count - a.count)
)

const chartHeight = computed(() => Math.max(320, sortedStats.value.length * 34))

const chartData = computed(() => ({
  labels: sortedStats.value.map(s => s.title),
  datasets: [
    {
      label: 'Yozuvlar soni',
      backgroundColor: '#4f46e5',
      borderRadius: 6,
      barThickness: 16,
      data: sortedStats.value.map(s => s.count)
    }
  ]
}))

const chartOptions = computed(() => {
  const textColor = $q.dark.isActive ? '#f1f5f9' : '#334155'
  const gridColor = $q.dark.isActive
    ? 'rgba(148, 163, 184, 0.16)'
    : 'rgba(148, 163, 184, 0.18)'
  return {
    indexAxis: 'y',
    responsive: true,
    maintainAspectRatio: false,
    layout: { padding: { right: 34 } },
    plugins: {
      legend: { display: false },
      tooltip: {
        backgroundColor: '#1a1b23',
        padding: 10,
        cornerRadius: 8,
        titleFont: { family: 'Inter', weight: '600' },
        bodyFont: { family: 'Inter' }
      }
    },
    scales: {
      x: {
        beginAtZero: true,
        ticks: { precision: 0, color: textColor, font: { family: 'Inter' } },
        grid: { color: gridColor }
      },
      y: {
        ticks: { color: textColor, font: { family: 'Inter' } },
        grid: { display: false }
      }
    }
  }
})

const valueLabelPlugin = {
  id: 'valueLabels',
  afterDatasetsDraw(chart) {
    const { ctx } = chart
    const meta = chart.getDatasetMeta(0)
    const color = $q.dark.isActive ? '#f1f5f9' : '#0f172a'
    ctx.save()
    ctx.fillStyle = color
    ctx.font = "600 12px 'Inter'"
    ctx.textBaseline = 'middle'
    meta.data.forEach((bar, index) => {
      const value = chart.data.datasets[0].data[index]
      ctx.fillText(String(value), bar.x + 8, bar.y)
    })
    ctx.restore()
  }
}

onMounted(async () => {
  const results = await Promise.all(
    visibleModules.value.map(m =>
      api
        .get(m.endpoint, { params: { size: 1 } })
        .then(res => [m.key, res.data.totalElements])
        .catch(() => [m.key, 0])
    )
  )
  counts.value = Object.fromEntries(results)
  loading.value = false
})
</script>

<style scoped>
.dashboard-page {
  max-width: 1280px;
  margin: 0 auto;
}

.muted-text {
  color: var(--brand-text-muted);
}

.stat-card-enter {
  animation: statCardEnter 0.35s ease both;
}

@keyframes statCardEnter {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.quick-access-scroll {
  overflow-y: auto;
  align-content: flex-start;
  padding-right: 2px;
}

.quick-tile {
  position: relative;
  height: 100%;
  border: 1px solid var(--brand-border);
  border-radius: 14px;
  padding: 12px;
  cursor: pointer;
  transition:
    transform 0.15s ease,
    box-shadow 0.15s ease,
    border-color 0.15s ease;
}

.quick-tile:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.1);
  border-color: var(--q-primary);
}

.quick-tile__icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.quick-tile__add {
  margin: -6px -6px 0 0;
}
</style>

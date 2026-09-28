<template>
  <q-page class="q-pa-md q-pa-lg-md dashboard-page">
    <div class="row items-center justify-between q-mb-lg">
      <div>
        <div class="text-h5 text-weight-bold"
          >Xush kelibsiz, {{ authStore.username }} 👋</div
        >
        <div class="text-body2 text-grey-6 q-mt-xs">
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
        <div class="stat-card brand-card bg-white full-height">
          <div class="row items-center justify-between q-mb-sm">
            <div class="stat-card__icon" :style="{ background: stat.bg }">
              <q-icon :name="stat.icon" :color="stat.color" size="24px" />
            </div>
            <q-icon
              v-if="!loading"
              name="north_east"
              size="16px"
              color="grey-5"
              class="cursor-pointer"
              @click="router.push(`/app/${stat.key}`)"
            />
          </div>
          <div class="text-h4 text-weight-bold">
            <q-skeleton v-if="loading" type="text" width="50px" />
            <template v-else>{{ stat.count }}</template>
          </div>
          <div class="text-caption text-grey-6">{{ stat.title }}</div>
        </div>
      </div>
    </div>

    <div class="row q-col-gutter-md">
      <div class="col-12 col-md-7">
        <div class="brand-card bg-white q-pa-md full-height">
          <div class="text-subtitle1 text-weight-semibold q-mb-md">
            Modullar bo'yicha yozuvlar soni
          </div>
          <div style="height: 300px">
            <q-skeleton v-if="loading" type="rect" class="full-height" />
            <Bar v-else :data="chartData" :options="chartOptions" />
          </div>
        </div>
      </div>

      <div class="col-12 col-md-5">
        <div class="brand-card bg-white q-pa-md full-height">
          <div class="text-subtitle1 text-weight-semibold q-mb-md"
            >Tezkor kirish</div
          >
          <q-list separator>
            <q-item
              v-for="item in quickLinks"
              :key="item.key"
              clickable
              v-ripple
              class="rounded-borders"
              @click="router.push(`/app/${item.key}`)"
            >
              <q-item-section avatar>
                <q-avatar
                  :color="item.color"
                  text-color="white"
                  :icon="item.icon"
                  size="36px"
                />
              </q-item-section>
              <q-item-section>
                <q-item-label class="text-weight-medium">{{
                  item.title
                }}</q-item-label>
                <q-item-label caption>{{ item.group }}</q-item-label>
              </q-item-section>
              <q-item-section side>
                <q-icon name="chevron_right" color="grey-5" />
              </q-item-section>
            </q-item>
          </q-list>
        </div>
      </div>
    </div>
  </q-page>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
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
  visibleModules.value.slice(0, 6).map((m, i) => ({
    key: m.key,
    title: m.title,
    group: m.group,
    icon: m.icon,
    color: cardPalette[i % cardPalette.length].color
  }))
)

const chartData = computed(() => ({
  labels: visibleModules.value.map(m => m.title),
  datasets: [
    {
      label: 'Yozuvlar soni',
      backgroundColor: '#4f46e5',
      borderRadius: 6,
      data: visibleModules.value.map(m => counts.value[m.key] ?? 0)
    }
  ]
}))

const chartOptions = {
  responsive: true,
  maintainAspectRatio: false,
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
    x: { grid: { display: false }, ticks: { font: { family: 'Inter' } } },
    y: {
      beginAtZero: true,
      ticks: { precision: 0, font: { family: 'Inter' } },
      grid: { color: 'rgba(148, 163, 184, 0.15)' }
    }
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
</style>

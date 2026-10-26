<template>
  <page-layout
    icon="travel_explore"
    title="Tashriflar"
    subtitle="Ommaviy demo havolasi orqali kirganlar · xodimlar kirishi va botlar hisobga olinmaydi"
  >
    <template #actions>
      <AppButton
        variant="secondary"
        icon="refresh"
        label="Yangilash"
        :loading="loading"
        @click="load"
      />
    </template>

    <div class="visits" data-testid="visits-page">
      <div v-if="error && !stats" class="visits__error dash-card" role="alert">
        {{ error }}
        <AppButton
          variant="secondary"
          size="sm"
          icon="refresh"
          label="Qayta urinish"
          @click="load"
        />
      </div>

      <div class="visits__kpis">
        <KpiCard
          v-for="k in kpis"
          :key="k.kpiKey"
          v-bind="k"
          :loading="loading && !stats"
        />
      </div>

      <ChartCard
        class="visits__days"
        title="30 kunlik tashriflar"
        subtitle="Kun bo'yicha tashriflar (sessiyalar) va noyob tashrifchilar"
        :loading="loading"
        :data="stats"
        :empty="!!stats && !stats.visits"
        empty-text="Hali tashrif yo'q — havolani ulashing"
        skeleton="chart"
      >
        <div class="visits__chart">
          <Bar :key="themeKey" :data="daysData" :options="daysOptions" />
        </div>
      </ChartCard>

      <ChartCard
        class="visits__sources"
        title="Manbalar"
        subtitle="Havola qayerdan ochilgan (?ref=)"
        :loading="loading"
        :data="stats"
        :empty="!!stats && !stats.sources.length"
        skeleton="ring"
      >
        <div class="visits__donut" data-testid="visits-sources">
          <div class="visits__donut-chart">
            <Doughnut
              :key="themeKey"
              :data="sourcesData"
              :options="donutOptions"
            />
          </div>
          <ul class="visits__legend">
            <li v-for="(s, i) in stats?.sources || []" :key="s.key">
              <span
                class="visits__dot"
                :style="{ background: COLORS[i % COLORS.length] }"
              />
              <span class="visits__legend-name">{{ sourceName(s.key) }}</span>
              <span class="visits__legend-num">{{ s.count }}</span>
            </li>
          </ul>
        </div>
      </ChartCard>

      <ChartCard
        class="visits__devices"
        title="Qurilmalar"
        subtitle="Kompyuter / telefon / planshet · brauzerlar"
        :loading="loading"
        :data="stats"
        :empty="!!stats && !stats.devices.length"
      >
        <div class="visits__bars">
          <div v-for="d in deviceRows" :key="d.key" class="visits__bar-row">
            <q-icon :name="d.icon" size="20px" class="visits__bar-icon" />
            <span class="visits__bar-name">{{ d.name }}</span>
            <div class="visits__bar-track">
              <div
                class="visits__bar-fill"
                :style="{ width: d.percent + '%' }"
              />
            </div>
            <span class="visits__bar-num">{{ d.count }}</span>
          </div>
          <div v-if="stats?.browsers?.length" class="visits__browsers">
            <q-chip
              v-for="b in stats.browsers"
              :key="b.key"
              dense
              square
              class="visits__chip"
              >{{ b.key }} · {{ b.count }}</q-chip
            >
          </div>
        </div>
      </ChartCard>

      <ChartCard
        class="visits__pages"
        title="Eng ko'p ko'rilgan sahifalar"
        subtitle="Oxirgi 90 kun"
        :loading="loading"
        :data="stats"
        :empty="!!stats && !stats.topPages.length"
        skeleton="chart"
      >
        <div class="visits__chart" :style="{ height: pagesHeight }">
          <Bar :key="themeKey" :data="pagesData" :options="pagesOptions" />
        </div>
      </ChartCard>

      <section class="visits__share dash-card" data-testid="visits-share">
        <h3 class="visits__h3">Ulashish havolalari</h3>
        <p class="visits__muted">
          Havola orqali kirgan har kim "Mehmon" bo'ladi: hamma sahifani ko'radi,
          hech narsani o'zgartira olmaydi. Manba (ref) tashriflarda ko'rinadi.
        </p>
        <div v-for="l in links" :key="l.ref" class="visits__link">
          <span class="visits__link-name">{{ sourceName(l.ref) }}</span>
          <code class="visits__link-url">{{ l.url }}</code>
          <AppButton
            variant="ghost"
            size="sm"
            icon="content_copy"
            :aria-label="`${sourceName(l.ref)} havolasini nusxalash`"
            @click="copy(l.url)"
          >
            <q-tooltip>Nusxalash</q-tooltip>
          </AppButton>
        </div>
        <div class="visits__custom">
          <AppInput
            v-model="customRef"
            label="O'z manbangiz (masalan: instagram, guruh1)"
            class="visits__custom-input"
            maxlength="40"
          />
          <AppButton
            variant="secondary"
            icon="content_copy"
            label="Havolani nusxalash"
            :disable="!cleanCustom"
            @click="copy(linkFor(cleanCustom))"
          />
        </div>
        <code v-if="cleanCustom" class="visits__link-url">{{
          linkFor(cleanCustom)
        }}</code>
      </section>

      <section class="visits__recent dash-card">
        <h3 class="visits__h3">Oxirgi tashriflar</h3>
        <q-table
          flat
          dense
          :rows="stats?.recent || []"
          :columns="columns"
          row-key="id"
          :loading="loading && !stats"
          :pagination="{ rowsPerPage: 20 }"
          no-data-label="Hali tashrif yo'q"
          class="visits__table"
        >
          <template #body="props">
            <q-tr
              :props="props"
              class="visits__row"
              @click="props.expand = !props.expand"
            >
              <q-td key="firstSeen" :props="props">{{
                when(props.row.firstSeen)
              }}</q-td>
              <q-td key="ref" :props="props">{{
                sourceName(props.row.ref)
              }}</q-td>
              <q-td key="device" :props="props">
                <q-icon :name="deviceIcon(props.row.device)" size="18px" />
                {{ props.row.browser }}
              </q-td>
              <q-td key="duration" :props="props">{{
                duration(props.row.durationSeconds)
              }}</q-td>
              <q-td key="pageCount" :props="props">
                {{ props.row.pageCount }}
                <q-icon
                  :name="props.expand ? 'expand_less' : 'expand_more'"
                  size="18px"
                />
              </q-td>
            </q-tr>
            <q-tr v-if="props.expand" :props="props" class="visits__trail-row">
              <q-td colspan="100%">
                <ol class="visits__trail">
                  <li v-for="(p, i) in props.row.pages" :key="i">
                    <span class="visits__trail-time">{{ p.time }}</span>
                    {{ pageName(p.path) }}
                    <span class="visits__muted">{{ p.path }}</span>
                  </li>
                </ol>
              </q-td>
            </q-tr>
          </template>
        </q-table>
      </section>
    </div>
  </page-layout>
</template>

<script setup>
// "Tashriflar": who opened the public demo link (#/demo?ref=…). Unique visitors (one browser =
// one visitor), visits (a new one after 30 minutes without activity), how long they looked,
// where the link came from, what they opened and on which device; and the links to share.
import { computed, onMounted, ref } from 'vue'
import { copyToClipboard, useQuasar } from 'quasar'
import { Bar, Doughnut } from 'vue-chartjs'
import {
  Chart as ChartJS,
  ArcElement,
  BarElement,
  CategoryScale,
  LinearScale,
  Tooltip
} from 'chart.js'
import PageLayout from '@/components/PageLayout.vue'
import AppButton from '@/components/app/AppButton.vue'
import AppInput from '@/components/app/AppInput.vue'
import KpiCard from '@/components/dashboard/KpiCard.vue'
import ChartCard from '@/components/dashboard/ChartCard.vue'
import { useChartTheme, alpha } from '@/charts/chartTheme'
import { getModule } from '@/config/modules'
import { api } from '@/boot/axios'

ChartJS.register(ArcElement, BarElement, CategoryScale, LinearScale, Tooltip)

const $q = useQuasar()
const { themeKey, colors } = useChartTheme()
const stats = ref(null)
const loading = ref(false)
const error = ref(null)

async function load() {
  loading.value = true
  error.value = null
  try {
    const res = await api.get('/api/demo/visits/stats')
    stats.value = res.data
  } catch (e) {
    error.value = e.friendlyMessage || "Ma'lumot yuklanmadi"
  } finally {
    loading.value = false
  }
}
onMounted(load)

// ------------------------------------------------------------------ numbers

function duration(sec) {
  const s = Math.max(0, Math.round(sec || 0))
  if (s < 60) return `${s} s`
  const m = Math.floor(s / 60)
  if (m < 60) return `${m} daq ${s % 60} s`
  return `${Math.floor(m / 60)} soat ${m % 60} daq`
}

const kpis = computed(() => {
  const s = stats.value
  const visitorsSpark = (s?.perDay || []).map(d => d.visitors)
  const visitsSpark = (s?.perDay || []).map(d => d.visits)
  return [
    {
      kpiKey: 'visitors',
      label: 'Jami noyob tashrifchilar',
      icon: 'groups',
      color: 'var(--chart-primary)',
      value: s ? s.visitors : undefined,
      format: v => `${Math.round(v)} kishi`,
      note: s ? `${s.visits} ta tashrif (sessiya)` : '',
      spark: visitorsSpark,
      sparkLabel: 'Noyob tashrifchilar: oxirgi 30 kun'
    },
    {
      kpiKey: 'today',
      label: 'Bugun',
      icon: 'today',
      color: 'var(--st-present)',
      value: s ? s.today : undefined,
      format: v => `${Math.round(v)} kishi`,
      note: 'Bugun faol bo‘lganlar',
      spark: visitsSpark,
      sparkLabel: 'Tashriflar: oxirgi 30 kun'
    },
    {
      kpiKey: 'week',
      label: 'Shu hafta',
      icon: 'date_range',
      color: 'var(--ring-journal)',
      value: s ? s.week : undefined,
      format: v => `${Math.round(v)} kishi`,
      note: 'Dushanbadan beri',
      spark: visitorsSpark.slice(-7)
    },
    {
      kpiKey: 'avg',
      label: "O'rtacha ko'rish vaqti",
      icon: 'timer',
      color: 'var(--ring-quality)',
      value: s ? s.avgDurationSeconds : undefined,
      format: duration,
      note: 'Bir tashrifda',
      spark: []
    }
  ]
})

// ------------------------------------------------------------------ charts

const COLORS = [
  '#0a66c2', // LinkedIn
  '#229ed9', // Telegram
  '#64748b', // direct
  '#22c55e',
  '#f59e0b',
  '#a855f7',
  '#ef4444',
  '#14b8a6'
]

const SOURCE_NAMES = {
  linkedin: 'LinkedIn',
  telegram: 'Telegram',
  direct: "To'g'ridan-to'g'ri",
  login: 'Kirish sahifasi'
}
function sourceName(key) {
  return SOURCE_NAMES[key] || key
}

function dayLabel(iso) {
  const [, m, d] = iso.split('-')
  return `${Number(d)}.${m}`
}

const daysData = computed(() => {
  const rows = stats.value?.perDay || []
  return {
    labels: rows.map(r => dayLabel(r.date)),
    datasets: [
      {
        label: 'Tashriflar',
        data: rows.map(r => r.visits),
        backgroundColor: alpha(colors.value.primary, 0.85),
        borderRadius: 4,
        maxBarThickness: 18
      },
      {
        label: 'Noyob tashrifchilar',
        data: rows.map(r => r.visitors),
        backgroundColor: alpha(colors.value.secondary, 0.7),
        borderRadius: 4,
        maxBarThickness: 18
      }
    ]
  }
})
const daysOptions = {
  plugins: { tooltip: { mode: 'index', intersect: false } },
  scales: {
    x: { grid: { display: false } },
    y: { beginAtZero: true, ticks: { precision: 0 } }
  }
}

const sourcesData = computed(() => {
  const rows = stats.value?.sources || []
  return {
    labels: rows.map(r => sourceName(r.key)),
    datasets: [
      {
        data: rows.map(r => r.count),
        backgroundColor: rows.map((_, i) => COLORS[i % COLORS.length]),
        borderColor: colors.value.card,
        borderWidth: 2
      }
    ]
  }
})
const donutOptions = { cutout: '62%' }

const PAGE_NAMES = {
  '/': 'Bosh sahifa',
  '/attendance': 'Davomat olish',
  '/attendance/calendar': 'Davomat kalendari',
  '/gradebook': 'Baholar jurnali',
  '/timetable': 'Dars jadvali',
  '/calendar': 'Taqvim',
  '/classes': 'Sinflar',
  '/rooms': 'Xonalar',
  '/telegram-parents': 'Ota-onalar (Telegram)',
  '/bot-stats': 'Bot statistikasi',
  '/bot-settings': 'Bot sozlamalari',
  '/activity': 'Faollik jurnali',
  '/school-settings': 'Maktab sozlamalari',
  '/library/books': 'Kitoblar',
  '/library/categories': "Kutubxona bo'limlari",
  '/library/authors': 'Mualliflar',
  '/library/permissions': 'Ruxsatnomalar',
  '/library/stats': 'Mutolaa statistikasi',
  '/locked': 'Yopiq bo‘lim'
}
function pageName(path) {
  if (PAGE_NAMES[path]) return PAGE_NAMES[path]
  const app = /^\/app\/([^/]+)/.exec(path)
  if (app) return getModule(app[1])?.title || app[1]
  if (path.startsWith('/classes/')) return 'Sinf sahifasi'
  if (path.startsWith('/profiles/student/')) return "O'quvchi profili"
  if (path.startsWith('/profiles/teacher/')) return "O'qituvchi profili"
  if (path.startsWith('/profiles/class/')) return 'Sinf profili'
  return path
}

const pagesData = computed(() => {
  const rows = stats.value?.topPages || []
  return {
    labels: rows.map(r => pageName(r.key)),
    datasets: [
      {
        label: "Ko'rishlar",
        data: rows.map(r => r.count),
        backgroundColor: alpha(colors.value.primary, 0.85),
        borderRadius: 4,
        maxBarThickness: 16
      }
    ]
  }
})
// one bar per page, so the card is as tall as its list
const pagesHeight = computed(
  () => `${Math.max(140, (stats.value?.topPages?.length || 0) * 34 + 40)}px`
)
const pagesOptions = {
  indexAxis: 'y',
  scales: {
    x: { beginAtZero: true, ticks: { precision: 0 } },
    y: { grid: { display: false } }
  }
}

const DEVICES = {
  DESKTOP: { name: 'Kompyuter', icon: 'computer' },
  MOBILE: { name: 'Telefon', icon: 'smartphone' },
  TABLET: { name: 'Planshet', icon: 'tablet_mac' }
}
function deviceIcon(d) {
  return DEVICES[d]?.icon || 'devices'
}
const deviceRows = computed(() => {
  const rows = stats.value?.devices || []
  const total = rows.reduce((s, r) => s + r.count, 0) || 1
  return rows.map(r => ({
    key: r.key,
    name: DEVICES[r.key]?.name || r.key,
    icon: deviceIcon(r.key),
    count: r.count,
    percent: Math.round((r.count / total) * 100)
  }))
})

// ------------------------------------------------------------------ recent visits

const columns = [
  { name: 'firstSeen', label: 'Vaqt', field: 'firstSeen', align: 'left' },
  { name: 'ref', label: 'Manba', field: 'ref', align: 'left' },
  { name: 'device', label: 'Qurilma', field: 'device', align: 'left' },
  {
    name: 'duration',
    label: 'Davomiylik',
    field: 'durationSeconds',
    align: 'left'
  },
  { name: 'pageCount', label: 'Sahifalar', field: 'pageCount', align: 'left' }
]

function when(iso) {
  if (!iso) return '—'
  const [date, time] = String(iso).split('T')
  const [, m, d] = date.split('-')
  return `${Number(d)}.${m} ${time.slice(0, 5)}`
}

// ------------------------------------------------------------------ share links

function linkFor(ref) {
  return `${window.location.origin}${window.location.pathname}#/demo?ref=${encodeURIComponent(ref)}`
}
const links = computed(() =>
  ['linkedin', 'telegram', 'direct'].map(ref => ({ ref, url: linkFor(ref) }))
)
const customRef = ref('')
const cleanCustom = computed(() =>
  customRef.value
    .toLowerCase()
    .replace(/[^a-z0-9_-]/g, '')
    .slice(0, 40)
)

async function copy(text) {
  try {
    await copyToClipboard(text)
    $q.notify({ message: 'Havola nusxalandi', icon: 'content_copy' })
  } catch {
    $q.notify({ message: text, timeout: 8000 })
  }
}
</script>

<style scoped>
.visits {
  display: grid;
  align-items: start;
  grid-template-columns: repeat(12, minmax(0, 1fr));
  gap: var(--space-4);
}

.visits__error {
  grid-column: 1 / -1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  padding: var(--space-4);
}

.visits__kpis {
  grid-column: 1 / -1;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--space-4);
}

.visits__days {
  grid-column: span 8;
}

.visits__sources {
  grid-column: span 4;
}

.visits__devices {
  grid-column: span 5;
}

.visits__pages {
  grid-column: span 7;
}

.visits__share,
.visits__recent {
  grid-column: 1 / -1;
  padding: var(--space-4) var(--space-5);
}

.visits__chart {
  position: relative;
  height: var(--chart-h);
}

.visits__donut {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  min-height: var(--chart-h);
}

.visits__donut-chart {
  position: relative;
  flex: 0 0 160px;
  width: 160px;
  height: 160px;
}

.visits__legend {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  min-width: 0;
  flex: 1 1 auto;
}

.visits__legend li {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-sm);
}

.visits__dot {
  width: var(--legend-dot);
  height: var(--legend-dot);
  border-radius: var(--radius-pill);
  flex: none;
}

.visits__legend-name {
  flex: 1 1 auto;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.visits__legend-num,
.visits__bar-num {
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.visits__bars {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.visits__bar-row {
  display: grid;
  grid-template-columns: 24px 96px minmax(0, 1fr) auto;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-sm);
}

.visits__bar-icon {
  color: var(--brand-text-muted);
}

.visits__bar-track {
  height: var(--bar-h);
  border-radius: var(--radius-pill);
  background: var(--ring-track);
  overflow: hidden;
}

.visits__bar-fill {
  height: 100%;
  border-radius: inherit;
  background: var(--chart-primary);
}

.visits__browsers {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-1);
}

.visits__chip {
  margin: 0;
  background: var(--surface-2);
  color: var(--text-primary);
  font-size: var(--text-xs);
}

.visits__h3 {
  margin: 0 0 var(--space-2);
  font-size: var(--text-md);
  font-weight: 700;
}

.visits__muted {
  color: var(--brand-text-muted);
  font-size: var(--text-sm);
}

.visits__share p {
  margin: 0 0 var(--space-3);
}

.visits__link {
  display: grid;
  grid-template-columns: 140px minmax(0, 1fr) auto;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-1) 0;
  border-bottom: 1px solid var(--brand-border);
}

.visits__link-name {
  font-weight: 600;
  font-size: var(--text-sm);
}

.visits__link-url {
  display: block;
  font-size: var(--text-xs);
  overflow-wrap: anywhere;
  color: var(--brand-text-muted);
}

.visits__custom {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  gap: var(--space-2);
  margin: var(--space-3) 0 var(--space-2);
}

.visits__custom-input {
  flex: 1 1 260px;
}

.visits__row {
  cursor: pointer;
}

.visits__trail {
  margin: 0;
  padding: var(--space-2) 0 var(--space-2) var(--space-5);
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  font-size: var(--text-sm);
  white-space: normal;
}

.visits__trail-time {
  font-variant-numeric: tabular-nums;
  color: var(--brand-text-muted);
  margin-right: var(--space-2);
}

@media (max-width: 1023px) {
  .visits__kpis {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .visits__days,
  .visits__sources,
  .visits__devices,
  .visits__pages {
    grid-column: 1 / -1;
  }
}

@media (max-width: 599px) {
  .visits__kpis {
    grid-template-columns: minmax(0, 1fr);
  }

  .visits__donut {
    flex-direction: column;
  }

  .visits__link {
    grid-template-columns: minmax(0, 1fr) auto;
  }

  .visits__link-name {
    grid-column: 1 / -1;
  }
}
</style>

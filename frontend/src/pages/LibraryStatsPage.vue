<template>
  <page-layout
    icon="auto_stories"
    color="#8b5cf6"
    title="Mutolaa statistikasi"
    :subtitle="`Ota-onalar botda qaysi kitoblarni o'qiyapti · ${schoolStore.activeSchoolName || ''}`"
  >
    <template #actions>
      <AppButton
        variant="ghost"
        icon="refresh"
        aria-label="Yangilash"
        @click="reloadAll"
      >
        <q-tooltip>Yangilash</q-tooltip>
      </AppButton>
    </template>

    <q-banner dense rounded class="privacy-banner q-mb-md">
      <template #avatar><q-icon name="lock" color="primary" /></template>
      Bu sahifa faqat administratorga ko'rinadi: ota-onalarning telefon
      raqamlari va kim nimani o'qiyotgani maxfiy ma'lumot.
    </q-banner>

    <!-- KPI -->
    <div class="row q-col-gutter-md q-mb-md">
      <div v-for="k in kpis" :key="k.label" class="col-6 col-md-3">
        <div class="brand-card stat-card">
          <div class="row items-center no-wrap">
            <IconTile
              :icon="k.icon"
              size="48"
              :color="`${k.color}1f`"
              :text-color="k.color"
              class="q-mr-md gt-xs"
            />
            <div class="col" style="min-width: 0">
              <div class="text-h5 text-weight-bold tabular-nums ellipsis">
                <q-skeleton v-if="!overview" type="text" width="48px" />
                <template v-else>{{ k.value }}</template>
              </div>
              <div class="text-caption muted-text">{{ k.label }}</div>
              <div v-if="overview && k.hint" class="text-caption muted-text">
                {{ k.hint }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- charts -->
    <div class="row q-col-gutter-md q-mb-md">
      <div class="col-12 col-lg-6">
        <div class="brand-card q-pa-md chart-card">
          <div class="text-subtitle1 text-weight-semibold q-mb-sm">
            Yuklashlar — oxirgi 30 kun
          </div>
          <div class="chart-box">
            <q-skeleton v-if="!overview" height="100%" />
            <Line v-else :data="dailyData" :options="lineOptions" />
          </div>
        </div>
      </div>
      <div class="col-12 col-md-6 col-lg-3">
        <div class="brand-card q-pa-md chart-card">
          <div class="text-subtitle1 text-weight-semibold q-mb-sm"
            >Bo'limlar bo'yicha</div
          >
          <div class="chart-box">
            <q-skeleton v-if="!overview" height="100%" />
            <div
              v-else-if="!overview.byCategory.length"
              class="empty-chart muted-text text-caption"
            >
              30 kunda yuklash yo'q
            </div>
            <Doughnut v-else :data="categoryData" :options="doughnutOptions" />
          </div>
        </div>
      </div>
      <div class="col-12 col-md-6 col-lg-3">
        <div class="brand-card q-pa-md chart-card">
          <div class="text-subtitle1 text-weight-semibold q-mb-sm">
            Sinflar: o'qiyotgan ota-onalar
          </div>
          <div class="chart-box">
            <q-skeleton v-if="!overview" height="100%" />
            <Bar v-else :data="classData" :options="barOptions" />
          </div>
        </div>
      </div>
    </div>

    <div class="row q-col-gutter-md q-mb-md">
      <!-- TOP-10 -->
      <div class="col-12 col-lg-5">
        <div class="brand-card q-pa-md full-height">
          <div class="text-subtitle1 text-weight-semibold q-mb-sm"
            >TOP-10 kitob</div
          >
          <div
            v-if="overview && !overview.top.length"
            class="muted-text text-caption"
          >
            Hali kitob yuklab olinmagan.
          </div>
          <q-list v-else dense>
            <q-item
              v-for="(b, i) in overview?.top || []"
              :key="b.id"
              class="q-px-none"
            >
              <q-item-section avatar class="top-rank tabular-nums">{{
                i + 1
              }}</q-item-section>
              <q-item-section avatar>
                <book-cover
                  :book-id="b.id"
                  :has-cover="b.hasCover"
                  :title="b.title"
                  width="36px"
                  height="48px"
                />
              </q-item-section>
              <q-item-section>
                <q-item-label class="ellipsis text-weight-semibold">{{
                  b.title
                }}</q-item-label>
                <q-item-label caption class="ellipsis">{{
                  b.author || '—'
                }}</q-item-label>
              </q-item-section>
              <q-item-section side class="text-right">
                <div class="tabular-nums text-weight-semibold">
                  <q-icon name="download" size="14px" /> {{ b.downloads }}
                </div>
                <div class="text-caption muted-text tabular-nums">
                  👍 {{ b.likePercent == null ? '—' : b.likePercent + '%' }}
                </div>
              </q-item-section>
            </q-item>
          </q-list>
        </div>
      </div>
      <!-- class report -->
      <div class="col-12 col-lg-7">
        <div class="brand-card full-height">
          <div class="text-subtitle1 text-weight-semibold q-pa-md q-pb-none">
            Sinflar bo'yicha hisobot
          </div>
          <q-table
            flat
            dense
            :rows="overview?.byClass || []"
            :columns="classColumns"
            row-key="classId"
            :loading="!overview"
            :pagination="{
              rowsPerPage: 8,
              sortBy: 'percent',
              descending: true
            }"
            :rows-per-page-options="[8, 16, 0]"
            no-data-label="Sinf yo'q"
          >
            <template #body-cell-percent="p">
              <q-td :props="p">
                <div
                  class="row items-center no-wrap q-gutter-xs"
                  style="min-width: 120px"
                >
                  <q-linear-progress
                    :value="p.row.percent / 100"
                    rounded
                    size="8px"
                    :color="
                      p.row.percent >= 50
                        ? 'positive'
                        : p.row.percent >= 20
                          ? 'warning'
                          : 'negative'
                    "
                    class="col"
                  />
                  <span class="tabular-nums text-caption"
                    >{{ p.row.percent }}%</span
                  >
                </div>
              </q-td>
            </template>
          </q-table>
        </div>
      </div>
    </div>

    <!-- journal -->
    <div class="brand-card q-pa-md">
      <div class="row items-center q-mb-sm">
        <div class="text-subtitle1 text-weight-semibold"
          >Yuklashlar jurnali</div
        >
        <q-space />
        <AppButton
          color="positive"
          icon="file_download"
          label="Excel"
          :loading="exporting"
          @click="exportExcel"
        />
      </div>
      <div class="row q-col-gutter-sm q-mb-sm">
        <div class="col-6 col-md-2">
          <AppDateInput v-model="filters.from" label="Dan" />
        </div>
        <div class="col-6 col-md-2">
          <AppDateInput v-model="filters.to" label="Gacha" />
        </div>
        <div class="col-12 col-sm-6 col-md-2">
          <AppSelect
            v-model="filters.categoryId"
            clearable
            emit-value
            map-options
            :options="categoryOptions"
            label="Bo'lim"
          />
        </div>
        <div class="col-12 col-sm-6 col-md-3">
          <AppSelect
            v-model="filters.bookId"
            clearable
            emit-value
            map-options
            use-input
            input-debounce="300"
            :options="bookOptions"
            label="Kitob"
            @filter="filterBooks"
          />
        </div>
        <div class="col-12 col-md-3">
          <AppInput
            v-model="filters.q"
            clearable
            debounce="400"
            label="Ota-ona (ism yoki telefon)"
          >
            <template #prepend><q-icon name="person_search" /></template>
          </AppInput>
        </div>
      </div>
      <div class="row items-center q-mb-sm q-gutter-sm">
        <class-picker
          :classes="classes"
          :model-value="filters.classId"
          @update:model-value="v => (filters.classId = v)"
        />
        <AppButton
          variant="ghost"
          v-if="filters.classId || hasFilters"
          icon="filter_alt_off"
          label="Filtrlarni tozalash"
          @click="clearFilters"
        />
      </div>
      <q-table
        v-model:pagination="journalPagination"
        flat
        :rows="journal"
        :columns="journalColumns"
        row-key="id"
        :loading="journalLoading"
        :rows-per-page-options="[20, 50, 100]"
        no-data-label="Yuklash topilmadi"
        @request="onJournalRequest"
        @row-click="(_, row) => openParent(row)"
      >
        <template #body-cell-parent="p">
          <q-td :props="p" class="cursor-pointer">
            <div class="text-weight-semibold text-primary">{{
              p.row.parentName || '—'
            }}</div>
            <div class="text-caption muted-text">{{
              p.row.parentPhone || ''
            }}</div>
          </q-td>
        </template>
        <template #body-cell-children="p">
          <q-td :props="p" style="white-space: normal; min-width: 180px">
            {{ p.row.children || '—' }}
          </q-td>
        </template>
        <template #body-cell-book="p">
          <q-td :props="p" style="white-space: normal; min-width: 180px">
            <div class="text-weight-semibold">{{ p.row.bookTitle }}</div>
            <div class="text-caption muted-text">{{
              p.row.bookAuthor || ''
            }}</div>
          </q-td>
        </template>
        <template #body-cell-source="p">
          <q-td :props="p">
            <q-chip
              dense
              square
              :icon="source(p.row.source).icon"
              :style="{
                background: source(p.row.source).color + '1f',
                color: source(p.row.source).color
              }"
              :label="source(p.row.source).label"
            />
          </q-td>
        </template>
      </q-table>
    </div>

    <!-- one parent -->
    <q-dialog v-model="parentOpen" position="right" full-height>
      <q-card class="parent-panel column no-wrap">
        <q-card-section class="row items-center q-pb-sm">
          <IconTile
            icon="person"
            size="40"
            color="var(--q-primary)"
            class="q-mr-sm"
          />
          <div class="col" style="min-width: 0">
            <div class="text-subtitle1 text-weight-bold ellipsis">
              {{ parent?.name || 'Ota-ona' }}
            </div>
            <div class="text-caption muted-text ellipsis">
              {{ parent?.phone || ''
              }}{{ parent?.telegramName ? ' · ' + parent.telegramName : '' }}
            </div>
          </div>
          <AppButton
            variant="ghost"
            icon="close"
            aria-label="Yopish"
            v-close-popup
          />
        </q-card-section>
        <q-separator />
        <q-card-section v-if="parentLoading" class="col flex flex-center">
          <q-spinner size="36px" color="primary" />
        </q-card-section>
        <q-card-section v-else-if="parent" class="col scroll">
          <div class="text-caption text-weight-semibold q-mb-xs"
            >Farzandlari</div
          >
          <div class="q-mb-md">
            <q-chip
              v-for="c in parent.children"
              :key="c.name"
              dense
              icon="face"
              :label="`${c.name} (${c.className})`"
            />
            <span v-if="!parent.children.length" class="muted-text text-caption"
              >—</span
            >
          </div>
          <div class="text-caption text-weight-semibold q-mb-xs"
            >Sevimli bo'limlari</div
          >
          <div class="q-mb-md">
            <q-chip
              v-for="c in parent.favoriteCategories"
              :key="c.label"
              dense
              color="indigo-1"
              text-color="indigo-9"
              :label="`${c.label} · ${c.count}`"
            />
            <span
              v-if="!parent.favoriteCategories.length"
              class="muted-text text-caption"
              >—</span
            >
          </div>
          <div class="row items-center q-mb-xs">
            <div class="text-caption text-weight-semibold">
              Yuklagan kitoblari ({{ parent.downloads.length }})
            </div>
            <q-space />
            <span class="text-caption muted-text"
              >❤️ saqlagan: {{ parent.savedBooks }}</span
            >
          </div>
          <q-list dense separator>
            <q-item
              v-for="(d, i) in parent.downloads"
              :key="i"
              class="q-px-none"
            >
              <q-item-section>
                <q-item-label class="text-weight-semibold">{{
                  d.title
                }}</q-item-label>
                <q-item-label caption>
                  {{ d.author || '—' }} · {{ d.category || "Bo'limsiz" }}
                </q-item-label>
              </q-item-section>
              <q-item-section side class="text-right">
                <div class="text-caption tabular-nums">{{
                  formatTime(d.downloadedAt)
                }}</div>
                <div class="text-caption muted-text">{{
                  source(d.source).label
                }}</div>
              </q-item-section>
            </q-item>
          </q-list>
        </q-card-section>
      </q-card>
    </q-dialog>
  </page-layout>
</template>

<script setup>
import IconTile from '@/components/app/IconTile.vue'
import AppButton from '@/components/app/AppButton.vue'
import AppInput from '@/components/app/AppInput.vue'
import AppSelect from '@/components/app/AppSelect.vue'
import AppDateInput from '@/components/app/AppDateInput.vue'
import { ref, reactive, computed, watch } from 'vue'
import { useQuasar } from 'quasar'
import { Line, Bar, Doughnut } from 'vue-chartjs'
import {
  Chart as ChartJS,
  Tooltip,
  Legend,
  BarElement,
  LineElement,
  PointElement,
  ArcElement,
  CategoryScale,
  LinearScale,
  Filler
} from 'chart.js'
import { api } from '@/boot/axios'
import PageLayout from '@/components/PageLayout.vue'
import BookCover from '@/components/BookCover.vue'
import ClassPicker from '@/components/ClassPicker.vue'
import { useSchoolStore } from '@/stores/school'
import { formatShortDate } from '@/utils/date'
import { downloadSources } from '@/utils/library'

ChartJS.register(
  Tooltip,
  Legend,
  BarElement,
  LineElement,
  PointElement,
  ArcElement,
  CategoryScale,
  LinearScale,
  Filler
)

const COLORS = [
  '#6366f1',
  '#0ea5e9',
  '#10b981',
  '#f59e0b',
  '#ef4444',
  '#8b5cf6',
  '#ec4899',
  '#14b8a6',
  '#64748b',
  '#84cc16'
]

const $q = useQuasar()
const schoolStore = useSchoolStore()
const overview = ref(null)
const classes = ref([])
const categories = ref([])
const journal = ref([])
const journalLoading = ref(false)
const exporting = ref(false)
const journalPagination = ref({ page: 1, rowsPerPage: 20, rowsNumber: 0 })
const filters = reactive({
  from: '',
  to: '',
  classId: null,
  categoryId: null,
  bookId: null,
  q: ''
})
const bookOptions = ref([])

const hasFilters = computed(
  () =>
    !!(
      filters.from ||
      filters.to ||
      filters.categoryId ||
      filters.bookId ||
      filters.q
    )
)

function notifyError(error) {
  $q.notify({
    type: 'negative',
    message: error.friendlyMessage || 'Xatolik yuz berdi'
  })
}

function source(code) {
  return (
    downloadSources[code] || {
      label: code || '—',
      icon: 'help',
      color: '#64748b'
    }
  )
}

function formatTime(value) {
  if (!value) return ''
  const d = new Date(value)
  const pad = n => String(n).padStart(2, '0')
  return `${pad(d.getDate())}.${pad(d.getMonth() + 1)}.${d.getFullYear()} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

const kpis = computed(() => {
  const k = overview.value?.kpi || {}
  return [
    {
      label: 'Jami kitoblar',
      value: k.totalBooks ?? 0,
      hint: 'botda faol',
      icon: 'menu_book',
      color: '#6366f1'
    },
    {
      label: 'Shu oyda yuklashlar',
      value: k.monthDownloads ?? 0,
      hint: null,
      icon: 'download',
      color: '#0ea5e9'
    },
    {
      label: "Kitob o'qiyotgan ota-onalar",
      value: k.readingParents ?? 0,
      hint: `${k.readingPercent ?? 0}% · ulangan ${k.linkedParents ?? 0} tadan (30 kun)`,
      icon: 'family_restroom',
      color: '#10b981'
    },
    {
      label: 'Eng faol sinf',
      value: k.topClass || '—',
      hint: k.topClass ? `${k.topClassReaders} ota-ona o'qiyapti` : null,
      icon: 'emoji_events',
      color: '#f59e0b'
    }
  ]
})

const textColor = computed(() => ($q.dark.isActive ? '#f1f5f9' : '#334155'))
const gridColor = computed(() =>
  $q.dark.isActive ? 'rgba(148,163,184,0.16)' : 'rgba(148,163,184,0.2)'
)

const dailyData = computed(() => {
  const days = overview.value?.daily || []
  return {
    labels: days.map(d => formatShortDate(d.date)),
    datasets: [
      {
        label: 'Yuklashlar',
        data: days.map(d => d.count),
        borderColor: '#6366f1',
        backgroundColor: 'rgba(99,102,241,0.15)',
        fill: true,
        tension: 0.3,
        pointRadius: 2,
        borderWidth: 2
      }
    ]
  }
})

const categoryData = computed(() => {
  const rows = overview.value?.byCategory || []
  return {
    labels: rows.map(r => r.label),
    datasets: [
      {
        data: rows.map(r => r.count),
        backgroundColor: rows.map((_, i) => COLORS[i % COLORS.length]),
        borderWidth: 0
      }
    ]
  }
})

const classData = computed(() => {
  const rows = (overview.value?.byClass || []).filter(r => r.linkedParents > 0)
  return {
    labels: rows.map(r => r.className),
    datasets: [
      {
        label: "O'qiyotgan ota-onalar, %",
        data: rows.map(r => r.percent),
        backgroundColor: '#10b981',
        borderRadius: 4
      }
    ]
  }
})

const scales = computed(() => ({
  x: {
    ticks: { color: textColor.value, maxRotation: 0, autoSkip: true },
    grid: { display: false }
  },
  y: {
    beginAtZero: true,
    ticks: { color: textColor.value, precision: 0 },
    grid: { color: gridColor.value }
  }
}))

const lineOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  plugins: { legend: { display: false } },
  scales: scales.value
}))

const barOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  plugins: { legend: { display: false } },
  scales: { ...scales.value, y: { ...scales.value.y, max: 100 } }
}))

const doughnutOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  cutout: '62%',
  plugins: {
    legend: {
      position: 'bottom',
      labels: { color: textColor.value, boxWidth: 10, font: { size: 11 } }
    }
  }
}))

const classColumns = [
  {
    name: 'className',
    label: 'Sinf',
    field: 'className',
    align: 'left',
    sortable: true,
    sort: (a, b, ra, rb) => ra.grade - rb.grade || a.localeCompare(b)
  },
  {
    name: 'students',
    label: "O'quvchi",
    field: 'students',
    align: 'right',
    sortable: true
  },
  {
    name: 'linked',
    label: 'Ulangan ota-ona',
    field: 'linkedParents',
    align: 'right',
    sortable: true
  },
  {
    name: 'readers',
    label: "O'qiyotgan",
    field: 'readingParents',
    align: 'right',
    sortable: true
  },
  {
    name: 'percent',
    label: 'Faollik',
    field: 'percent',
    align: 'left',
    sortable: true
  }
]

const journalColumns = [
  {
    name: 'time',
    label: 'Sana-vaqt',
    field: r => formatTime(r.downloadedAt),
    align: 'left'
  },
  { name: 'parent', label: 'Ota-ona', field: 'parentName', align: 'left' },
  { name: 'children', label: 'Farzandi', field: 'children', align: 'left' },
  {
    name: 'classes',
    label: 'Sinfi',
    field: r => r.classes || '—',
    align: 'left'
  },
  { name: 'book', label: 'Kitob', field: 'bookTitle', align: 'left' },
  {
    name: 'category',
    label: "Bo'lim",
    field: r => r.category || '—',
    align: 'left'
  },
  { name: 'source', label: 'Manba', field: 'source', align: 'left' }
]

const categoryOptions = computed(() =>
  categories.value.map(c => ({
    value: c.id,
    label: `${c.emoji ? c.emoji + ' ' : ''}${c.nameUz}`
  }))
)

function params() {
  return {
    schoolId: schoolStore.activeSchoolId,
    from: filters.from || undefined,
    to: filters.to || undefined,
    classId: filters.classId || undefined,
    categoryId: filters.categoryId || undefined,
    bookId: filters.bookId || undefined,
    q: filters.q || undefined
  }
}

async function loadOverview() {
  overview.value = null
  try {
    const res = await api.get('/api/library/stats', {
      params: { schoolId: schoolStore.activeSchoolId }
    })
    overview.value = res.data
  } catch (e) {
    notifyError(e)
  }
}

async function loadLists() {
  try {
    const [c, cats] = await Promise.all([
      api.get('/api/school-classes', {
        params: { schoolId: schoolStore.activeSchoolId, size: 300 }
      }),
      api.get('/api/library/categories', {
        params: { schoolId: schoolStore.activeSchoolId }
      })
    ])
    classes.value = c.data.content
    categories.value = cats.data
  } catch (e) {
    notifyError(e)
  }
}

async function loadJournal() {
  journalLoading.value = true
  try {
    const res = await api.get('/api/library/stats/journal', {
      params: {
        ...params(),
        page: journalPagination.value.page - 1,
        size: journalPagination.value.rowsPerPage
      }
    })
    journal.value = res.data.items
    journalPagination.value = {
      ...journalPagination.value,
      rowsNumber: res.data.total
    }
  } catch (e) {
    notifyError(e)
  } finally {
    journalLoading.value = false
  }
}

function onJournalRequest({ pagination }) {
  journalPagination.value = {
    ...journalPagination.value,
    page: pagination.page,
    rowsPerPage: pagination.rowsPerPage || 20
  }
  loadJournal()
}

async function filterBooks(text, update) {
  try {
    const res = await api.get('/api/library/books', {
      params: {
        schoolId: schoolStore.activeSchoolId,
        q: text || undefined,
        size: 20
      },
      background: true
    })
    update(() => {
      bookOptions.value = res.data.items.map(b => ({
        value: b.id,
        label: b.title
      }))
    })
  } catch {
    update(() => {})
  }
}

function clearFilters() {
  Object.assign(filters, {
    from: '',
    to: '',
    classId: null,
    categoryId: null,
    bookId: null,
    q: ''
  })
}

async function exportExcel() {
  exporting.value = true
  try {
    const res = await api.get('/api/library/stats/journal/export', {
      params: params(),
      responseType: 'blob'
    })
    const url = URL.createObjectURL(res.data)
    const a = document.createElement('a')
    a.href = url
    a.download = `mutolaa-jurnali-${new Date().toISOString().slice(0, 10)}.xlsx`
    document.body.appendChild(a)
    a.click()
    a.remove()
    setTimeout(() => URL.revokeObjectURL(url), 10_000)
  } catch (e) {
    notifyError(e)
  } finally {
    exporting.value = false
  }
}

// ------------------------------------------------------------------ one parent

const parentOpen = ref(false)
const parentLoading = ref(false)
const parent = ref(null)

async function openParent(row) {
  parent.value = {
    name: row.parentName,
    phone: row.parentPhone,
    telegramName: row.telegramName
  }
  parentOpen.value = true
  parentLoading.value = true
  try {
    const res = await api.get(`/api/library/stats/parents/${row.chatId}`, {
      params: { schoolId: schoolStore.activeSchoolId }
    })
    parent.value = res.data
  } catch (e) {
    notifyError(e)
    parentOpen.value = false
  } finally {
    parentLoading.value = false
  }
}

function reloadAll() {
  loadOverview()
  loadJournal()
}

watch(
  () => ({ ...filters }),
  () => {
    journalPagination.value = { ...journalPagination.value, page: 1 }
    loadJournal()
  },
  { deep: true }
)

watch(
  () => schoolStore.activeSchoolId,
  id => {
    if (!id) return
    clearFilters()
    loadLists()
    reloadAll()
  },
  { immediate: true }
)
</script>

<style scoped>
.chart-card {
  height: 100%;
}
.chart-box {
  position: relative;
  height: var(--chart-h);
}
.empty-chart {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.top-rank {
  min-width: 24px;
  font-weight: 700;
  color: var(--brand-text-muted);
}
.parent-panel {
  width: 440px;
  max-width: 100vw;
}
.privacy-banner {
  background: rgba(99, 102, 241, 0.08);
  border: 1px solid rgba(99, 102, 241, 0.25);
}
.muted-text {
  color: var(--brand-text-muted);
}
</style>

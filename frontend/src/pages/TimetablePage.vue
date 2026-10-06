<template>
  <page-layout
    icon="calendar_view_week"
    color="#a855f7"
    title="Dars jadvali"
    :subtitle="subtitle"
  >
    <template v-slot:actions>
      <q-btn-toggle
        v-model="filterType"
        dense
        no-caps
        unelevated
        toggle-color="primary"
        color="grey-3"
        text-color="grey-9"
        class="tt-mode no-print"
        :options="[
          { value: 'class', label: 'Sinf' },
          { value: 'teacher', label: 'O\'qituvchi' },
          { value: 'room', label: 'Xona' }
        ]"
        @update:model-value="onFilterTypeChange"
      />
      <q-btn-dropdown
        split
        unelevated
        no-caps
        color="primary"
        icon="picture_as_pdf"
        label="PDF yuklab olish"
        class="no-print"
        :loading="pdfLoading"
        :disable="filterType !== 'class' || !filterValue"
        @click="downloadPdf(false)"
      >
        <q-list dense style="min-width: 220px">
          <q-item v-close-popup clickable @click="downloadPdf(false)">
            <q-item-section avatar
              ><q-icon name="description"
            /></q-item-section>
            <q-item-section>{{ currentLabel }} sinf</q-item-section>
          </q-item>
          <q-item v-close-popup clickable @click="downloadPdf(true)">
            <q-item-section avatar
              ><q-icon name="library_books"
            /></q-item-section>
            <q-item-section>Barcha sinflar (bitta fayl)</q-item-section>
          </q-item>
        </q-list>
      </q-btn-dropdown>
      <q-btn
        flat
        round
        dense
        icon="print"
        color="grey-7"
        class="no-print gt-xs"
        @click="printPage"
        ><q-tooltip>Chop etish (Ctrl+P)</q-tooltip></q-btn
      >
      <q-btn
        flat
        round
        dense
        icon="add"
        color="primary"
        class="no-print"
        @click="router.push('/app/lesson-slots?create=1')"
        ><q-tooltip>Dars qo'shish</q-tooltip></q-btn
      >
      <q-btn
        flat
        round
        dense
        icon="list"
        color="grey-7"
        class="no-print"
        @click="router.push('/app/lesson-slots')"
        ><q-tooltip>Ro'yxat ko'rinishi</q-tooltip></q-btn
      >
    </template>

    <!-- class tabs (1-A, 1-B, 2-A…) in one row; teacher/room — a select -->
    <div class="tt-picker no-print">
      <q-tabs
        v-if="filterType === 'class'"
        v-model="filterValue"
        dense
        no-caps
        inline-label
        outside-arrows
        mobile-arrows
        active-color="primary"
        indicator-color="primary"
        align="left"
        class="tt-tabs"
        @update:model-value="onPick"
      >
        <q-tab
          v-for="o in filterOptions"
          :key="o.value"
          :name="o.value"
          :label="o.label"
        />
      </q-tabs>
      <q-select
        v-else
        v-model="filterValue"
        :options="filterOptions"
        option-value="value"
        option-label="label"
        emit-value
        map-options
        outlined
        dense
        use-input
        input-debounce="0"
        :label="filterType === 'teacher' ? 'O\'qituvchi' : 'Xona'"
        style="max-width: 320px"
        @update:model-value="onPick"
        @filter="filterSelect"
      />
    </div>

    <!-- desktop/tablet: the whole week on one screen -->
    <div
      v-if="!isPhone"
      ref="gridBox"
      class="tt-box brand-card"
      :style="{ height: gridHeight ? gridHeight + 'px' : undefined }"
    >
      <div v-if="loading" class="tt-loading">
        <q-skeleton type="rect" class="full-height" />
      </div>
      <div
        v-else-if="!periods.length"
        class="tt-empty column flex-center muted-text"
      >
        <q-icon name="event_busy" size="44px" class="q-mb-sm" />
        <div>Bu {{ modeNoun }} uchun dars jadvali kiritilmagan</div>
      </div>
      <div
        v-else
        class="tt-grid"
        :style="{
          gridTemplateRows: `34px repeat(${periods.length}, minmax(0, 1fr))`
        }"
      >
        <div class="tt-corner">Dars</div>
        <div
          v-for="day in weekdays"
          :key="day"
          class="tt-day"
          :class="{ 'tt-day--today': day === todayName }"
        >
          {{ day }}
        </div>
        <template v-for="(p, i) in periods" :key="p.start">
          <div class="tt-period">
            <b>{{ i + 1 }}</b>
            <span>{{ p.start }}–{{ p.end }}</span>
          </div>
          <div
            v-for="day in weekdays"
            :key="day + p.start"
            class="tt-cell"
            :class="{ 'tt-cell--today': day === todayName }"
          >
            <div
              v-for="e in cell(day, p.start)"
              :key="e.lessonSlotId"
              class="tt-lesson"
              :class="{ 'tt-lesson--now': isLive(day, e) }"
              :style="{ '--c': subjectColor(e.subjectName) }"
              @click="openDetail(e)"
            >
              <div class="tt-subject">{{ e.subjectName }}</div>
              <div class="tt-meta">{{ metaLine(e) }}</div>
              <q-tooltip anchor="top middle" self="bottom middle" :delay="300">
                <b>{{ e.subjectName }}</b
                ><br />
                {{ e.teacherName }} · {{ roomLabel(e.roomNumber) }}<br />
                {{ e.className }} · {{ e.startTime.slice(0, 5) }}–{{
                  e.endTime.slice(0, 5)
                }}
              </q-tooltip>
            </div>
          </div>
        </template>
      </div>
    </div>

    <!-- phone: days as tabs, one day at a time -->
    <template v-else>
      <q-tabs
        v-model="phoneDay"
        dense
        no-caps
        mobile-arrows
        outside-arrows
        active-color="primary"
        class="q-mb-sm"
      >
        <q-tab
          v-for="day in weekdays"
          :key="day"
          :name="day"
          :label="day.slice(0, 3)"
        />
      </q-tabs>
      <div class="brand-card q-pa-sm">
        <q-skeleton v-if="loading" type="rect" height="240px" />
        <div v-else-if="!dayList.length" class="q-pa-lg text-center muted-text">
          Bu kunda dars yo'q
        </div>
        <div
          v-for="e in dayList"
          :key="e.lessonSlotId"
          class="tt-phone-row"
          :style="{ '--c': subjectColor(e.subjectName) }"
          @click="openDetail(e)"
        >
          <div class="tt-phone-time">
            {{ e.startTime.slice(0, 5) }}<br /><span>{{
              e.endTime.slice(0, 5)
            }}</span>
          </div>
          <div class="col">
            <div class="text-weight-bold">{{ e.subjectName }}</div>
            <div class="text-caption muted-text">{{ metaLine(e) }}</div>
          </div>
        </div>
      </div>
    </template>

    <q-dialog v-model="detailOpen">
      <q-card
        v-if="detailEntry"
        style="width: 100%; max-width: 380px; border-radius: 18px"
      >
        <q-card-section class="row items-center q-pb-none">
          <q-avatar
            :style="{ background: subjectColor(detailEntry.subjectName) }"
            text-color="white"
            size="40px"
            icon="menu_book"
            class="q-mr-sm"
          />
          <div class="text-h6">{{ detailEntry.subjectName }}</div>
          <q-space />
          <q-btn flat round dense icon="close" v-close-popup />
        </q-card-section>
        <q-card-section class="q-gutter-sm">
          <div class="row items-center q-gutter-xs">
            <q-icon name="groups" size="18px" class="muted-text" />
            {{ detailEntry.className }}
          </div>
          <div class="row items-center q-gutter-xs">
            <q-icon name="person" size="18px" class="muted-text" />
            {{ detailEntry.teacherName }}
          </div>
          <div class="row items-center q-gutter-xs">
            <q-icon name="meeting_room" size="18px" class="muted-text" />
            {{ roomLabel(detailEntry.roomNumber) }}
          </div>
          <div class="row items-center q-gutter-xs">
            <q-icon name="schedule" size="18px" class="muted-text" />
            {{ detailEntry.weekday }} ·
            {{ detailEntry.startTime.slice(0, 5) }}–{{
              detailEntry.endTime.slice(0, 5)
            }}
          </div>
        </q-card-section>
        <q-card-actions align="right" class="q-pa-md">
          <q-btn
            flat
            no-caps
            color="primary"
            label="Tahrirlash"
            @click="router.push('/app/lesson-slots')"
          />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </page-layout>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useQuasar } from 'quasar'
import { api } from '@/boot/axios'
import PageLayout from '@/components/PageLayout.vue'
import { useSchoolStore } from '@/stores/school'

const route = useRoute()
const router = useRouter()
const $q = useQuasar()
const schoolStore = useSchoolStore()

const weekdays = [
  'Dushanba',
  'Seshanba',
  'Chorshanba',
  'Payshanba',
  'Juma',
  'Shanba'
]
const todayName = [null, ...weekdays][new Date().getDay()] || null
const QUERY_KEY = { class: 'class', teacher: 'teacher', room: 'room' }

const filterType = ref(
  route.query.teacher ? 'teacher' : route.query.room ? 'room' : 'class'
)
const filterValue = ref(null)
const filterOptions = ref([])
const allOptions = ref([])
const entries = ref([])
const loading = ref(false)
const pdfLoading = ref(false)
const isPhone = computed(() => $q.screen.lt.sm)
const phoneDay = ref(todayName || 'Dushanba')

const currentLabel = computed(
  () => allOptions.value.find(o => o.value === filterValue.value)?.label || ''
)
const modeNoun = computed(
  () =>
    ({ class: 'sinf', teacher: "o'qituvchi", room: 'xona' })[filterType.value]
)
const subtitle = computed(() =>
  [schoolStore.activeSchoolName, currentLabel.value && `${currentLabel.value}`]
    .filter(Boolean)
    .join(' · ')
)

// lesson rows = distinct start times, numbered 1..N, each with its end time
const periods = computed(() => {
  const map = new Map()
  for (const e of entries.value) {
    const s = e.startTime.slice(0, 5)
    const end = e.endTime.slice(0, 5)
    if (!map.has(s) || map.get(s) < end) map.set(s, end)
  }
  return [...map.entries()]
    .sort(([a], [b]) => a.localeCompare(b))
    .map(([start, end]) => ({ start, end }))
})

const byCell = computed(() => {
  const map = new Map()
  for (const e of entries.value) {
    const k = `${e.weekday}|${e.startTime.slice(0, 5)}`
    if (!map.has(k)) map.set(k, [])
    map.get(k).push(e)
  }
  return map
})

function cell(day, start) {
  return byCell.value.get(`${day}|${start}`) || []
}

const dayList = computed(() =>
  entries.value
    .filter(e => e.weekday === phoneDay.value)
    .sort((a, b) => a.startTime.localeCompare(b.startTime))
)

function metaLine(e) {
  const parts = []
  if (filterType.value !== 'teacher')
    parts.push(e.teacherLastName || e.teacherName)
  if (filterType.value !== 'class') parts.push(e.className)
  if (filterType.value !== 'room') parts.push(roomLabel(e.roomNumber))
  return parts.filter(Boolean).join(' · ')
}

// "201" → "201-xona", but a named room ("Katta sport zali") stays as it is
function roomLabel(room) {
  if (!room) return ''
  return /^\d/.test(room) ? `${room}-xona` : room
}

const now = ref(new Date())
let clock = null

function isLive(day, e) {
  if (day !== todayName) return false
  const m = now.value.getHours() * 60 + now.value.getMinutes()
  const toMin = t => {
    const [h, mm] = t.split(':').map(Number)
    return h * 60 + mm
  }
  return m >= toMin(e.startTime) && m < toMin(e.endTime)
}

const subjectPalette = [
  '#4f46e5',
  '#0ea5e9',
  '#10b981',
  '#f59e0b',
  '#ec4899',
  '#8b5cf6',
  '#14b8a6',
  '#ef4444',
  '#3b82f6',
  '#22c55e',
  '#eab308',
  '#f43f5e'
]
// same hash as the PDF (TimetablePdfService#subjectColor) — one colour per subject everywhere
function subjectColor(name) {
  let hash = 0
  for (const ch of name || '')
    hash = (hash * 31 + ch.charCodeAt(0)) % subjectPalette.length
  return subjectPalette[Math.abs(hash) % subjectPalette.length]
}

const detailOpen = ref(false)
const detailEntry = ref(null)
function openDetail(entry) {
  detailEntry.value = entry
  detailOpen.value = true
}

// --- one screen, no page scroll: the grid takes exactly the height left below it ---
const gridBox = ref(null)
const gridHeight = ref(0)

function fitGrid() {
  if (!gridBox.value || isPhone.value) return
  const top = gridBox.value.getBoundingClientRect().top + window.scrollY
  // q-page bottom padding (16px) + a hair so rounding never adds a scrollbar
  gridHeight.value = Math.max(320, Math.floor(window.innerHeight - top - 18))
}

let resizeTimer = null
function onResize() {
  clearTimeout(resizeTimer)
  resizeTimer = setTimeout(fitGrid, 80)
}

onMounted(() => {
  window.addEventListener('resize', onResize)
  clock = setInterval(() => (now.value = new Date()), 30000)
  nextTick(fitGrid)
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  clearInterval(clock)
  clearTimeout(resizeTimer)
})
watch([entries, isPhone, filterOptions], () => nextTick(fitGrid))

// --- picking a class / teacher / room (kept in the URL: ?class=12) ---
function filterSelect(val, update) {
  update(() => {
    const q = (val || '').toLowerCase()
    filterOptions.value = q
      ? allOptions.value.filter(o => o.label.toLowerCase().includes(q))
      : allOptions.value
  })
}

async function onFilterTypeChange() {
  filterValue.value = null
  entries.value = []
  await loadFilterOptions(null)
}

function onPick(val) {
  router.replace({ query: { [QUERY_KEY[filterType.value]]: val } })
  loadTimetable()
}

async function loadFilterOptions(preferred) {
  if (!schoolStore.activeSchoolId) return
  const schoolId = schoolStore.activeSchoolId
  if (filterType.value === 'class') {
    const res = await api.get('/api/school-classes', {
      params: { schoolId, size: 200, sort: 'gradeNumber,asc' }
    })
    allOptions.value = res.data.content
      .slice()
      .sort(
        (a, b) =>
          a.gradeNumber - b.gradeNumber ||
          String(a.sectionLetter).localeCompare(String(b.sectionLetter))
      )
      .map(c => ({ value: c.id, label: `${c.gradeNumber}-${c.sectionLetter}` }))
  } else if (filterType.value === 'teacher') {
    const res = await api.get('/api/employees', {
      params: { schoolId, size: 500 }
    })
    allOptions.value = res.data.content.map(e => ({
      value: e.id,
      label: e.fullName
    }))
  } else {
    const res = await api.get('/api/rooms', { params: { schoolId, size: 500 } })
    allOptions.value = res.data.content.map(r => ({
      value: r.id,
      label: r.roomNumber
    }))
  }
  filterOptions.value = allOptions.value
  const wanted = Number(preferred)
  const pick = allOptions.value.some(o => o.value === wanted)
    ? wanted
    : allOptions.value[0]?.value
  if (pick != null) {
    filterValue.value = pick
    onPick(pick)
  }
}

async function loadTimetable() {
  if (!filterValue.value) return
  loading.value = true
  try {
    const params = { schoolId: schoolStore.activeSchoolId }
    if (filterType.value === 'class') params.schoolClassId = filterValue.value
    else if (filterType.value === 'teacher')
      params.employeeId = filterValue.value
    else params.roomId = filterValue.value
    const res = await api.get('/api/lesson-slots/timetable', { params })
    entries.value = res.data
  } finally {
    loading.value = false
  }
}

// --- PDF (A4 landscape, made by the backend) and plain printing ---
async function downloadPdf(all) {
  pdfLoading.value = true
  try {
    const params = { schoolId: schoolStore.activeSchoolId }
    if (!all) params.schoolClassId = filterValue.value
    const res = await api.get('/api/lesson-slots/timetable.pdf', {
      params,
      responseType: 'blob',
      timeout: 60000
    })
    const url = URL.createObjectURL(res.data)
    const a = document.createElement('a')
    a.href = url
    a.download = all
      ? 'dars-jadvali-barcha-sinflar.pdf'
      : `dars-jadvali-${currentLabel.value || 'sinf'}.pdf`
    document.body.appendChild(a)
    a.click()
    a.remove()
    setTimeout(() => URL.revokeObjectURL(url), 1000)
  } catch {
    $q.notify({ type: 'negative', message: "PDF tayyorlab bo'lmadi" })
  } finally {
    pdfLoading.value = false
  }
}

function printPage() {
  window.print()
}

const preferred =
  route.query.class || route.query.teacher || route.query.room || null
loadFilterOptions(preferred)
watch(
  () => schoolStore.activeSchoolId,
  () => loadFilterOptions(null)
)
</script>

<style scoped>
.muted-text {
  color: var(--brand-text-muted);
}

.tt-mode :deep(.q-btn) {
  padding: 4px 12px;
}

.tt-picker {
  margin: -8px 0 10px;
}

.tt-tabs {
  border-bottom: 1px solid var(--brand-border);
}

.tt-tabs :deep(.q-tab) {
  min-height: 34px;
  padding: 0 14px;
  font-weight: 600;
}

.tt-box {
  overflow: hidden;
  padding: 0;
}

.tt-loading,
.tt-empty {
  height: 100%;
  padding: 12px;
}

.tt-grid {
  display: grid;
  grid-template-columns: 92px repeat(6, minmax(0, 1fr));
  height: 100%;
}

.tt-corner,
.tt-day {
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 13px;
  border-bottom: 2px solid var(--brand-border);
  background: var(--surface-2);
}

.tt-corner {
  color: var(--brand-text-muted);
  font-size: 12px;
}

.tt-day--today {
  color: var(--q-primary);
  background: rgba(79, 70, 229, 0.09);
}

.tt-period {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid var(--brand-border);
  border-right: 1px solid var(--brand-border);
  line-height: 1.2;
  min-height: 0;
}

.tt-period b {
  font-size: 15px;
}

.tt-period span {
  font-size: 11px;
  color: var(--brand-text-muted);
  white-space: nowrap;
}

.tt-cell {
  border-bottom: 1px solid var(--brand-border);
  border-right: 1px solid var(--brand-border);
  padding: 3px;
  min-height: 0;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
  overflow: hidden;
}

.tt-cell:last-child {
  border-right: 0;
}

.tt-cell--today {
  background: rgba(79, 70, 229, 0.04);
}

.tt-lesson {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 2px 8px;
  border-radius: 7px;
  border-left: 4px solid var(--c);
  background: color-mix(in srgb, var(--c) 13%, transparent);
  cursor: pointer;
  overflow: hidden;
  transition: background 0.15s ease;
}

.tt-lesson:hover {
  background: color-mix(in srgb, var(--c) 22%, transparent);
}

.tt-lesson--now {
  box-shadow: inset 0 0 0 2px var(--c);
  background: color-mix(in srgb, var(--c) 24%, transparent);
}

.tt-subject {
  font-weight: 700;
  /* grows with the screen: 13px on 1366, ~16px on 1920 */
  font-size: clamp(13px, 0.85vw, 17px);
  line-height: 1.2;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tt-meta {
  font-size: clamp(11px, 0.68vw, 14px);
  line-height: 1.25;
  color: var(--brand-text-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tt-phone-row {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 10px 8px;
  border-left: 4px solid var(--c);
  border-radius: 8px;
  margin-bottom: 6px;
  background: color-mix(in srgb, var(--c) 10%, transparent);
}

.tt-phone-time {
  width: 48px;
  font-weight: 700;
  font-size: 13px;
  line-height: 1.2;
  text-align: right;
}

.tt-phone-time span {
  font-weight: 400;
  color: var(--brand-text-muted);
}

/* below 1024px the grid keeps a readable width and scrolls sideways */
@media (max-width: 1023px) {
  .tt-box {
    overflow-x: auto;
  }
  .tt-grid {
    min-width: 860px;
  }
}
</style>

<style>
/* Ctrl+P: the timetable alone, A4 landscape, on one page */
@media print {
  @page {
    size: A4 landscape;
    margin: 10mm;
  }
  .q-header,
  .q-loading-bar,
  .q-drawer,
  .q-footer,
  .no-print,
  .q-tabs {
    display: none !important;
  }
  .q-page-container {
    padding: 0 !important;
  }
  .q-page {
    padding: 0 !important;
    min-height: 0 !important;
  }
  .tt-box {
    height: 180mm !important;
    box-shadow: none !important;
    border: 1px solid #cbd5e1;
    overflow: visible !important;
  }
  .tt-grid {
    min-width: 0 !important;
  }
  .tt-lesson,
  .tt-day,
  .tt-corner {
    -webkit-print-color-adjust: exact;
    print-color-adjust: exact;
  }
}
</style>

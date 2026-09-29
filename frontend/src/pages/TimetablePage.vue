<template>
  <page-layout
    icon="calendar_view_week"
    color="#a855f7"
    title="Dars jadvali"
    :subtitle="schoolStore.activeSchoolName"
  >
    <template v-slot:actions>
      <q-btn
        outline
        no-caps
        color="primary"
        icon="add"
        label="Dars qo'shish"
        @click="router.push('/app/lesson-slots?create=1')"
      />
      <q-btn
        outline
        no-caps
        color="grey-7"
        icon="print"
        label="PDF"
        class="gt-xs"
        @click="printDialogOpen = true"
      />
      <q-btn
        outline
        no-caps
        color="primary"
        icon="list"
        label="Ro'yxat ko'rinishi"
        @click="router.push('/app/lesson-slots')"
      />
    </template>

    <div class="brand-card q-pa-md q-mb-md">
      <div class="row q-col-gutter-md items-end">
        <div class="col-12 col-sm-3">
          <q-select
            v-model="filterType"
            :options="[
              { value: 'class', label: 'Sinf bo\'yicha' },
              { value: 'teacher', label: 'O\'qituvchi bo\'yicha' },
              { value: 'room', label: 'Xona bo\'yicha' }
            ]"
            option-value="value"
            option-label="label"
            emit-value
            map-options
            outlined
            dense
            label="Filtr turi"
            @update:model-value="onFilterTypeChange"
          />
        </div>
        <div class="col-12 col-sm-4">
          <q-select
            v-model="filterValue"
            :options="filterOptions"
            option-value="value"
            option-label="label"
            emit-value
            map-options
            outlined
            dense
            :label="filterLabel"
            @update:model-value="loadTimetable"
          />
        </div>
      </div>
    </div>

    <div v-if="loading" class="brand-card q-pa-md">
      <q-skeleton type="rect" height="400px" />
    </div>

    <div v-else class="brand-card q-pa-sm overflow-hidden">
      <div ref="tableWrapperRef" class="table-scroll table-scroll--relative">
        <div
          v-if="liveLine.visible"
          class="live-time-line"
          :style="{
            top: liveLine.top + 'px',
            left: liveLine.left + 'px',
            width: liveLine.width + 'px'
          }"
        >
          <span class="live-time-line__badge">{{ liveClock }}</span>
        </div>
        <table class="timetable-grid">
          <thead>
            <tr>
              <th class="time-col"></th>
              <th
                v-for="day in weekdays"
                :key="day"
                class="day-col"
                :class="{ 'day-col--today': day === todayName }"
              >
                {{ day }}
              </th>
            </tr>
          </thead>
          <tbody>
            <template v-for="(slot, idx) in timeSlots" :key="slot">
              <tr>
                <td class="time-col">{{ slot }}</td>
                <td
                  v-for="day in weekdays"
                  :key="day"
                  :ref="el => day === todayName && setTodayCellRef(idx, el)"
                  class="cell"
                  :class="{ 'cell--today': day === todayName }"
                >
                  <div
                    v-for="entry in cellEntries(day, slot)"
                    :key="entry.lessonSlotId"
                    class="lesson-chip"
                    :class="{ 'lesson-chip--live': isLive(day, entry) }"
                    :style="{ background: subjectColor(entry.subjectName) }"
                    @click="openDetail(entry)"
                  >
                    <div class="text-weight-bold ellipsis">{{
                      entry.subjectName
                    }}</div>
                    <div class="ellipsis" v-if="filterType !== 'class'">{{
                      entry.className
                    }}</div>
                    <div class="ellipsis" v-if="filterType !== 'teacher'">{{
                      entry.teacherName
                    }}</div>
                    <div class="ellipsis" v-if="filterType !== 'room'"
                      >{{ entry.roomNumber }}-xona</div
                    >
                  </div>
                  <div
                    v-if="!cellEntries(day, slot).length"
                    class="cell-add"
                    @click="router.push('/app/lesson-slots?create=1')"
                  >
                    <q-icon name="add" size="16px" />
                  </div>
                </td>
              </tr>
              <tr v-if="breakLabel(idx)" class="break-row">
                <td class="time-col"></td>
                <td :colspan="weekdays.length" class="break-cell">{{
                  breakLabel(idx)
                }}</td>
              </tr>
            </template>
          </tbody>
        </table>
      </div>
    </div>

    <div v-if="!loading && entries.length" class="row q-gutter-sm q-mt-md">
      <div v-for="s in subjectLegend" :key="s.name" class="legend-chip">
        <span class="legend-dot" :style="{ background: s.color }" />
        {{ s.name }}
      </div>
    </div>

    <q-dialog v-model="printDialogOpen">
      <q-card style="width: 100%; max-width: 380px; border-radius: 18px">
        <q-card-section class="row items-center q-pb-none">
          <div class="text-h6">PDF sifatida chiqarish</div>
          <q-space />
          <q-btn flat round dense icon="close" v-close-popup />
        </q-card-section>
        <q-card-section class="q-gutter-md">
          <q-option-group
            v-model="printScope"
            :options="[
              { label: 'Joriy ko\'rinish (hozir ekranda)', value: 'current' },
              {
                label: 'Barcha sinflar (har biri alohida sahifada)',
                value: 'all',
                disable: filterType !== 'class'
              }
            ]"
            color="primary"
          />
          <div v-if="filterType !== 'class'" class="text-caption muted-text">
            "Barcha sinflar" faqat "Sinf bo'yicha" filtrida ishlaydi
          </div>
        </q-card-section>
        <q-card-actions align="right" class="q-pa-md">
          <q-btn
            flat
            no-caps
            label="Bekor qilish"
            color="grey-7"
            v-close-popup
          />
          <q-btn
            color="primary"
            no-caps
            unelevated
            label="Chop etish"
            :loading="printPreparing"
            @click="confirmPrint"
          />
        </q-card-actions>
      </q-card>
    </q-dialog>

    <div v-if="printAllMode" class="print-all-only">
      <div
        v-for="cls in printAllData"
        :key="cls.classId"
        class="print-class-page"
      >
        <h2>{{ cls.className }} — dars jadvali</h2>
        <table class="print-all-table">
          <thead>
            <tr>
              <th>Kun</th>
              <th>Vaqt</th>
              <th>Fan</th>
              <th>O'qituvchi</th>
              <th>Xona</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(e, i) in cls.entries" :key="i">
              <td>{{ e.weekday }}</td>
              <td>{{ e.startTime.slice(0, 5) }}–{{ e.endTime.slice(0, 5) }}</td>
              <td>{{ e.subjectName }}</td>
              <td>{{ e.teacherName }}</td>
              <td>{{ e.roomNumber }}-xona</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

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
            {{ detailEntry.roomNumber }}-xona
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
import { useRouter } from 'vue-router'
import { api } from '@/boot/axios'
import PageLayout from '@/components/PageLayout.vue'
import { useSchoolStore } from '@/stores/school'

const router = useRouter()
const schoolStore = useSchoolStore()

const weekdays = [
  'Dushanba',
  'Seshanba',
  'Chorshanba',
  'Payshanba',
  'Juma',
  'Shanba'
]
const dayIndex = {
  1: 'Dushanba',
  2: 'Seshanba',
  3: 'Chorshanba',
  4: 'Payshanba',
  5: 'Juma',
  6: 'Shanba',
  0: null
}
const todayName = dayIndex[new Date().getDay()]

const filterType = ref('class')
const filterValue = ref(null)
const filterOptions = ref([])
const entries = ref([])
const loading = ref(false)

const filterLabel = computed(() => {
  if (filterType.value === 'class') return 'Sinf'
  if (filterType.value === 'teacher') return "O'qituvchi"
  return 'Xona'
})

const timeSlots = computed(() => {
  const set = new Set(entries.value.map(e => e.startTime.slice(0, 5)))
  return [...set].sort()
})

const periodEndByStart = computed(() => {
  const map = new Map()
  for (const e of entries.value) {
    map.set(e.startTime.slice(0, 5), e.endTime.slice(0, 5))
  }
  return map
})

function breakLabel(idx) {
  const current = timeSlots.value[idx]
  const next = timeSlots.value[idx + 1]
  if (!next) return ''
  const end = periodEndByStart.value.get(current)
  if (!end || end >= next) return ''
  const toMin = t => {
    const [h, m] = t.split(':').map(Number)
    return h * 60 + m
  }
  const gap = toMin(next) - toMin(end)
  return gap > 0 ? `Tanaffus · ${gap} daq` : ''
}

const subjectLegend = computed(() => {
  const names = [...new Set(entries.value.map(e => e.subjectName))].sort()
  return names.map(name => ({ name, color: subjectColor(name) }))
})

const detailOpen = ref(false)
const detailEntry = ref(null)
function openDetail(entry) {
  detailEntry.value = entry
  detailOpen.value = true
}

const printDialogOpen = ref(false)
const printScope = ref('current')
const printPreparing = ref(false)
const printAllMode = ref(false)
const printAllData = ref([])

async function confirmPrint() {
  printDialogOpen.value = false
  if (printScope.value === 'current') {
    window.print()
    return
  }

  printPreparing.value = true
  try {
    const classes = filterOptions.value
    const results = []
    for (const cls of classes) {
      const res = await api.get('/api/lesson-slots/timetable', {
        params: {
          schoolId: schoolStore.activeSchoolId,
          schoolClassId: cls.value
        }
      })
      results.push({
        classId: cls.value,
        className: cls.label,
        entries: [...res.data].sort(
          (a, b) =>
            a.weekday.localeCompare(b.weekday) ||
            a.startTime.localeCompare(b.startTime)
        )
      })
    }
    printAllData.value = results
    printAllMode.value = true
    await nextTick()
    window.print()
    printAllMode.value = false
  } finally {
    printPreparing.value = false
  }
}

// --- live "now" time line, drawn over today's column only ---
const tableWrapperRef = ref(null)
const todayCellRefs = ref({})
const liveLine = ref({ visible: false, top: 0, left: 0, width: 0 })
const liveClock = ref('')
let liveTimer = null

function setTodayCellRef(idx, el) {
  if (el) todayCellRefs.value[idx] = el
}

function nowMinutes() {
  const now = new Date()
  return now.getHours() * 60 + now.getMinutes() + now.getSeconds() / 60
}

function toMin(t) {
  const [h, m] = t.split(':').map(Number)
  return h * 60 + m
}

function recomputeLiveLine() {
  const now = new Date()
  liveClock.value = now.toTimeString().slice(0, 5)

  if (!todayName || !tableWrapperRef.value) {
    liveLine.value = { ...liveLine.value, visible: false }
    return
  }
  const wrapperRect = tableWrapperRef.value.getBoundingClientRect()
  const nowMin = nowMinutes()
  const slots = timeSlots.value

  for (let i = 0; i < slots.length; i++) {
    const start = toMin(slots[i])
    const end = toMin(periodEndByStart.value.get(slots[i]) || slots[i])
    const cell = todayCellRefs.value[i]
    if (!cell) continue
    const rect = cell.getBoundingClientRect()

    if (nowMin >= start && nowMin < end) {
      const ratio = (nowMin - start) / (end - start || 1)
      const top =
        rect.top -
        wrapperRect.top +
        tableWrapperRef.value.scrollTop +
        rect.height * ratio
      setLine(true, top, rect, wrapperRect)
      return
    }

    const next = slots[i + 1]
    if (next) {
      const nextStart = toMin(next)
      if (nowMin >= end && nowMin < nextStart) {
        const nextCell = todayCellRefs.value[i + 1]
        if (nextCell) {
          const nextRect = nextCell.getBoundingClientRect()
          const ratio = (nowMin - end) / (nextStart - end || 1)
          const bottomOfThis =
            rect.top -
            wrapperRect.top +
            tableWrapperRef.value.scrollTop +
            rect.height
          const topOfNext =
            nextRect.top - wrapperRect.top + tableWrapperRef.value.scrollTop
          const top = bottomOfThis + (topOfNext - bottomOfThis) * ratio
          setLine(true, top, rect, wrapperRect)
          return
        }
      }
    }
  }
  liveLine.value = { ...liveLine.value, visible: false }
}

function setLine(visible, top, rect, wrapperRect) {
  liveLine.value = {
    visible,
    top,
    left: rect.left - wrapperRect.left + tableWrapperRef.value.scrollLeft,
    width: rect.width
  }
}

function startLiveTimer() {
  stopLiveTimer()
  nextTick(recomputeLiveLine)
  liveTimer = setInterval(recomputeLiveLine, 30000)
}
function stopLiveTimer() {
  if (liveTimer) clearInterval(liveTimer)
  liveTimer = null
}

onMounted(startLiveTimer)
onBeforeUnmount(stopLiveTimer)
watch(entries, () => nextTick(recomputeLiveLine))

function cellEntries(day, slot) {
  return entries.value.filter(
    e => e.weekday === day && e.startTime.slice(0, 5) === slot
  )
}

function isLive(day, entry) {
  if (day !== todayName) return false
  const now = new Date()
  const [sh, sm] = entry.startTime.split(':').map(Number)
  const [eh, em] = entry.endTime.split(':').map(Number)
  const nowMin = now.getHours() * 60 + now.getMinutes()
  return nowMin >= sh * 60 + sm && nowMin < eh * 60 + em
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
function subjectColor(name) {
  let hash = 0
  for (const ch of name || '')
    hash = (hash * 31 + ch.charCodeAt(0)) % subjectPalette.length
  return subjectPalette[Math.abs(hash) % subjectPalette.length]
}

async function onFilterTypeChange() {
  filterValue.value = null
  entries.value = []
  await loadFilterOptions()
}

async function loadFilterOptions() {
  if (!schoolStore.activeSchoolId) return
  if (filterType.value === 'class') {
    const res = await api.get('/api/school-classes', {
      params: {
        schoolId: schoolStore.activeSchoolId,
        size: 100,
        sort: 'gradeNumber,asc'
      }
    })
    filterOptions.value = res.data.content.map(c => ({
      value: c.id,
      label: `${c.gradeNumber}-${c.sectionLetter}`
    }))
  } else if (filterType.value === 'teacher') {
    const res = await api.get('/api/employees', {
      params: { schoolId: schoolStore.activeSchoolId, size: 200 }
    })
    filterOptions.value = res.data.content.map(e => ({
      value: e.id,
      label: e.fullName
    }))
  } else {
    const res = await api.get('/api/rooms', {
      params: { schoolId: schoolStore.activeSchoolId, size: 200 }
    })
    filterOptions.value = res.data.content.map(r => ({
      value: r.id,
      label: r.roomNumber
    }))
  }
  if (filterOptions.value.length) {
    filterValue.value = filterOptions.value[0].value
    loadTimetable()
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

loadFilterOptions()
</script>

<style scoped>
.muted-text {
  color: var(--brand-text-muted);
}

.table-scroll {
  overflow-x: auto;
}

.table-scroll--relative {
  position: relative;
}

.live-time-line {
  position: absolute;
  height: 2px;
  background: var(--color-danger);
  z-index: 2;
  pointer-events: none;
}

.live-time-line::before {
  content: '';
  position: absolute;
  left: -4px;
  top: -3px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--color-danger);
}

.live-time-line__badge {
  position: absolute;
  right: 0;
  top: -18px;
  font-size: 10px;
  font-weight: 700;
  color: white;
  background: var(--color-danger);
  padding: 1px 5px;
  border-radius: 4px;
}

.timetable-grid {
  border-collapse: collapse;
  width: 100%;
  min-width: 900px;
  table-layout: fixed;
}

.time-col {
  width: 76px;
  font-size: 13px;
  font-weight: 600;
  color: var(--brand-text-muted);
  text-align: center;
  vertical-align: top;
  padding-top: 12px;
}

.day-col {
  text-align: center;
  padding: 12px 4px;
  font-size: 15px;
  font-weight: 700;
  border-bottom: 2px solid var(--brand-border);
}

.day-col--today {
  color: var(--q-primary);
}

.cell {
  border: 1px solid var(--brand-border);
  vertical-align: top;
  padding: 5px;
  height: 84px;
}

.cell--today {
  background: rgba(79, 70, 229, 0.03);
}

.cell-add {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  min-height: 40px;
  color: var(--brand-text-muted);
  opacity: 0;
  cursor: pointer;
  border-radius: 8px;
  transition: opacity 0.15s ease;
}

.cell:hover .cell-add {
  opacity: 1;
  background: rgba(79, 70, 229, 0.06);
}

.break-row .break-cell {
  text-align: center;
  font-size: 11px;
  color: var(--brand-text-muted);
  background: var(--surface-2);
  padding: 3px;
  border: 1px solid var(--brand-border);
}

.lesson-chip {
  border-radius: 8px;
  padding: 5px 7px;
  color: white;
  font-size: 12px;
  margin-bottom: 3px;
  line-height: 1.35;
  cursor: pointer;
}

.lesson-chip--live {
  box-shadow:
    0 0 0 2px white,
    0 0 0 4px var(--q-primary);
}

.legend-chip {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 999px;
  border: 1px solid var(--brand-border);
}

.legend-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
}
</style>

<style>
.print-all-only {
  display: none;
}

@media print {
  body * {
    visibility: hidden;
  }

  .timetable-grid,
  .timetable-grid * {
    visibility: visible;
  }
  .timetable-grid {
    position: absolute;
    left: 0;
    top: 0;
    min-width: 0;
  }

  .print-all-only,
  .print-all-only * {
    visibility: visible;
  }
  .print-all-only {
    display: block;
    position: absolute;
    left: 0;
    top: 0;
    width: 100%;
  }
  /* when printing all classes, the regular single-view grid must stay hidden */
  body:has(.print-all-only) .timetable-grid,
  body:has(.print-all-only) .timetable-grid * {
    visibility: hidden !important;
  }
  .print-class-page {
    page-break-after: always;
    padding: 16px;
  }
  .print-all-table {
    width: 100%;
    border-collapse: collapse;
  }
  .print-all-table th,
  .print-all-table td {
    border: 1px solid #ccc;
    padding: 6px 8px;
    text-align: left;
    font-size: 12px;
  }
}
</style>

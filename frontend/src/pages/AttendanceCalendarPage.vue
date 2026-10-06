<template>
  <page-layout
    icon="calendar_month"
    color="#ef4444"
    title="Davomat kalendari"
    :subtitle="subtitle"
  >
    <template v-slot:actions>
      <div class="ac-month row items-center no-wrap">
        <q-btn flat round dense icon="chevron_left" @click="shiftMonth(-1)" />
        <div class="ac-month__title">{{ monthTitle }}</div>
        <q-btn
          flat
          round
          dense
          icon="chevron_right"
          :disable="isCurrentMonth"
          @click="shiftMonth(1)"
        />
      </div>
      <q-btn
        outline
        no-caps
        color="primary"
        icon="fact_check"
        label="Dars bo'yicha belgilash"
        @click="router.push('/attendance')"
      />
    </template>

    <!-- parallel first (1…11), then its letters (1-A, 1-B) -->
    <div class="brand-card q-pa-sm q-mb-md">
      <div class="row items-center q-gutter-xs">
        <span class="ac-label">Sinf:</span>
        <q-btn
          v-for="g in grades"
          :key="g"
          :label="String(g)"
          dense
          unelevated
          no-caps
          class="ac-grade"
          :color="g === grade ? 'primary' : 'grey-3'"
          :text-color="g === grade ? 'white' : 'grey-9'"
          @click="pickGrade(g)"
        />
      </div>
      <div v-if="letters.length" class="row items-center q-gutter-xs q-mt-xs">
        <span class="ac-label"></span>
        <q-chip
          v-for="c in letters"
          :key="c.id"
          clickable
          :outline="c.id !== classId"
          :color="c.id === classId ? 'primary' : undefined"
          :text-color="c.id === classId ? 'white' : undefined"
          class="ac-letter"
          @click="pickClass(c.id)"
          >{{ c.gradeNumber }}-{{ c.sectionLetter }}</q-chip
        >
      </div>
    </div>

    <div v-if="loading && !data" class="brand-card q-pa-md">
      <q-skeleton type="rect" height="360px" />
    </div>
    <div
      v-else-if="!classId"
      class="brand-card q-pa-xl column flex-center muted-text"
    >
      <q-icon name="groups" size="44px" class="q-mb-sm" />
      Sinfni tanlang
    </div>

    <div v-else-if="data" class="row q-col-gutter-md no-wrap-md">
      <div class="col-12 col-md">
        <div class="brand-card ac-card">
          <div class="ac-scroll">
            <table class="ac-table">
              <thead>
                <tr>
                  <th class="ac-name-col">O'quvchi</th>
                  <th
                    v-for="d in data.days"
                    :key="d.date"
                    class="ac-day"
                    :class="dayClass(d)"
                    :title="d.holiday || d.weekday"
                    @click="pickDay(d)"
                  >
                    <div class="ac-day__num">{{ Number(d.date.slice(8)) }}</div>
                    <div class="ac-day__wd">{{ d.weekday.slice(0, 2) }}</div>
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="s in data.students" :key="s.studentId">
                  <td class="ac-name-col">
                    <div class="ac-student ellipsis">{{ s.fullName }}</div>
                    <div class="ac-parent ellipsis">
                      {{ s.guardianName ? 'Ota-ona: ' + s.guardianName : '—' }}
                    </div>
                  </td>
                  <td
                    v-for="d in data.days"
                    :key="d.date"
                    class="ac-cell"
                    :class="dayClass(d)"
                    @click="openCell($event, s, d)"
                  >
                    <span
                      class="ac-dot"
                      :class="
                        'ac-dot--' + (cellOf(s, d.date)?.status || 'NONE')
                      "
                      :title="cellTitle(s, d)"
                    />
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div class="ac-footer row items-center q-gutter-md">
            <span v-for="l in legend" :key="l.key" class="ac-legend">
              <span class="ac-dot" :class="'ac-dot--' + l.key" />{{ l.label }}
            </span>
            <q-space />
            <span class="text-weight-bold">
              {{ data.className }} — oylik davomat:
              <span :style="{ color: rateColor(data.rate) }">{{
                data.rate == null ? '—' : data.rate + '%'
              }}</span>
            </span>
          </div>
        </div>
      </div>

      <!-- the selected day: who was absent, late, excused -->
      <div class="col-12 col-md-auto">
        <div class="brand-card ac-side q-pa-md">
          <template v-if="selectedDay">
            <div class="text-subtitle1 text-weight-bold">
              {{ formatDate(selectedDay.date) }} · {{ selectedDay.weekday }}
            </div>
            <div v-if="selectedDay.holiday" class="muted-text q-mt-sm">
              🎉 {{ selectedDay.holiday }} — dars yo'q
            </div>
            <div v-else-if="selectedDay.sunday" class="muted-text q-mt-sm">
              Yakshanba — dars yo'q
            </div>
            <template v-else>
              <div class="row q-col-gutter-xs q-mt-sm text-center">
                <div v-for="k in summaryKeys" :key="k.key" class="col-3">
                  <div class="ac-count" :class="'ac-count--' + k.key">
                    {{ daySummary[k.key].length }}
                  </div>
                  <div class="text-caption muted-text">{{ k.short }}</div>
                </div>
              </div>
              <div
                v-for="k in summaryKeys.filter(x => x.key !== 'PRESENT')"
                :key="'l' + k.key"
                class="q-mt-md"
              >
                <div class="ac-list-title">
                  <span class="ac-dot" :class="'ac-dot--' + k.key" />{{
                    k.label
                  }}
                  ({{ daySummary[k.key].length }})
                </div>
                <div
                  v-if="!daySummary[k.key].length"
                  class="text-caption muted-text q-pl-md"
                  >—</div
                >
                <div
                  v-for="item in daySummary[k.key]"
                  :key="item.studentId"
                  class="ac-list-item"
                >
                  <div class="text-weight-medium">{{ item.fullName }}</div>
                  <div class="text-caption muted-text">
                    {{ item.guardianName || 'Ota-ona kiritilmagan' }}
                    <template v-if="item.guardianPhone">
                      · {{ item.guardianPhone }}</template
                    >
                  </div>
                  <div v-if="item.comment" class="text-caption ac-reason">
                    «{{ item.comment }}»
                  </div>
                </div>
              </div>
              <div
                v-if="daySummary.NONE.length"
                class="text-caption muted-text q-mt-md"
              >
                {{ daySummary.NONE.length }} ta o'quvchi belgilanmagan
              </div>
            </template>
          </template>
          <div v-else class="muted-text text-center q-py-lg">
            <q-icon name="touch_app" size="32px" /><br />
            Kun ustiga bosing — shu kun xulosasi chiqadi
          </div>
        </div>
      </div>
    </div>

    <!-- one shared quick menu for all cells -->
    <q-menu
      v-model="menuOpen"
      :target="menuTarget"
      no-parent-event
      anchor="bottom middle"
      self="top middle"
    >
      <q-list v-if="menuCell" dense style="min-width: 220px">
        <q-item-label header class="q-pb-xs">
          {{ menuCell.student.fullName }} ·
          {{ formatDate(menuCell.day.date) }}
        </q-item-label>
        <q-item
          v-for="opt in statusOptions"
          :key="opt.key"
          clickable
          :active="
            (cellOf(menuCell.student, menuCell.day.date)?.status || null) ===
            opt.value
          "
          @click="choose(opt.value)"
        >
          <q-item-section avatar style="min-width: 28px">
            <span class="ac-dot" :class="'ac-dot--' + opt.key" />
          </q-item-section>
          <q-item-section>{{ opt.label }}</q-item-section>
        </q-item>
        <q-item>
          <q-input
            v-model="menuComment"
            dense
            outlined
            class="full-width"
            label="Sabab / izoh (ixtiyoriy)"
            maxlength="300"
            @keyup.enter="choose(pendingStatus)"
          />
        </q-item>
      </q-list>
    </q-menu>
  </page-layout>
</template>

<script setup>
import { ref, shallowRef, computed, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useQuasar } from 'quasar'
import { api } from '@/boot/axios'
import PageLayout from '@/components/PageLayout.vue'
import { useSchoolStore } from '@/stores/school'
import { formatDate } from '@/utils/date'

const route = useRoute()
const router = useRouter()
const $q = useQuasar()
const schoolStore = useSchoolStore()

const MONTHS = [
  'Yanvar',
  'Fevral',
  'Mart',
  'Aprel',
  'May',
  'Iyun',
  'Iyul',
  'Avgust',
  'Sentabr',
  'Oktabr',
  'Noyabr',
  'Dekabr'
]
const legend = [
  { key: 'PRESENT', label: 'Keldi' },
  { key: 'LATE', label: 'Kechikdi' },
  { key: 'ABSENT', label: 'Kelmadi' },
  { key: 'EXCUSED', label: 'Sababli' },
  { key: 'NONE', label: 'Belgilanmagan' }
]
const statusOptions = [
  { key: 'PRESENT', value: 'PRESENT', label: 'Keldi' },
  { key: 'LATE', value: 'LATE', label: 'Kechikdi' },
  { key: 'ABSENT', value: 'ABSENT', label: 'Kelmadi' },
  { key: 'EXCUSED', value: 'EXCUSED', label: 'Sababli' },
  { key: 'NONE', value: null, label: 'Belgilanmagan (tozalash)' }
]
const summaryKeys = [
  { key: 'PRESENT', label: 'Keldi', short: 'Keldi' },
  { key: 'ABSENT', label: 'Kelmaganlar', short: 'Kelmadi' },
  { key: 'LATE', label: 'Kechikkanlar', short: 'Kechikdi' },
  { key: 'EXCUSED', label: 'Sabablilar', short: 'Sababli' }
]

const todayIso = (() => {
  const t = new Date()
  return `${t.getFullYear()}-${String(t.getMonth() + 1).padStart(2, '0')}-${String(t.getDate()).padStart(2, '0')}`
})()

const classes = ref([])
const grade = ref(Number(route.query.grade) || null)
const classId = ref(Number(route.query.class) || null)
const month = ref(route.query.month || todayIso.slice(0, 7))
const data = ref(null)
const loading = ref(false)
const selectedDay = ref(null)

const grades = computed(() =>
  [...new Set(classes.value.map(c => c.gradeNumber))].sort((a, b) => a - b)
)
const letters = computed(() =>
  classes.value
    .filter(c => c.gradeNumber === grade.value)
    .sort((a, b) => String(a.sectionLetter).localeCompare(b.sectionLetter))
)
const monthTitle = computed(() => {
  const [y, m] = month.value.split('-').map(Number)
  return `${MONTHS[m - 1]} ${y}`
})
const isCurrentMonth = computed(() => month.value >= todayIso.slice(0, 7))
const subtitle = computed(() =>
  [schoolStore.activeSchoolName, data.value?.className]
    .filter(Boolean)
    .join(' · ')
)

function syncUrl() {
  router.replace({
    query: {
      grade: grade.value || undefined,
      class: classId.value || undefined,
      month: month.value
    }
  })
}

async function loadClasses() {
  if (!schoolStore.activeSchoolId) return
  const res = await api.get('/api/school-classes', {
    params: { schoolId: schoolStore.activeSchoolId, size: 300 }
  })
  classes.value = res.data.content
  if (classId.value && !classes.value.some(c => c.id === classId.value))
    classId.value = null
  if (!classId.value && classes.value.length) {
    const first = [...classes.value].sort(
      (a, b) =>
        a.gradeNumber - b.gradeNumber ||
        String(a.sectionLetter).localeCompare(b.sectionLetter)
    )[0]
    classId.value = first.id
  }
  const current = classes.value.find(c => c.id === classId.value)
  if (current) grade.value = current.gradeNumber
  syncUrl()
  loadMonth()
}

async function loadMonth() {
  if (!classId.value) return
  loading.value = true
  try {
    const res = await api.get('/api/attendance/calendar', {
      params: { schoolClassId: classId.value, month: month.value }
    })
    data.value = res.data
    // keep the chosen day if it is in this month, otherwise today (or nothing)
    const keep = selectedDay.value?.date
    selectedDay.value =
      res.data.days.find(d => d.date === keep) ||
      res.data.days.find(d => d.date === todayIso) ||
      null
  } catch (e) {
    $q.notify({ type: 'negative', message: e.friendlyMessage || 'Xatolik' })
  } finally {
    loading.value = false
  }
}

function pickGrade(g) {
  grade.value = g
  const first = letters.value[0]
  if (first) pickClass(first.id)
}

function pickClass(id) {
  classId.value = id
  data.value = null
  syncUrl()
  loadMonth()
}

function shiftMonth(delta) {
  const [y, m] = month.value.split('-').map(Number)
  const d = new Date(y, m - 1 + delta, 1)
  month.value = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
  syncUrl()
  loadMonth()
}

function pickDay(d) {
  selectedDay.value = d
}

function cellOf(student, date) {
  return student.cells?.[date] || null
}

function cellTitle(s, d) {
  if (d.sunday) return 'Yakshanba'
  if (d.holiday) return d.holiday
  const c = cellOf(s, d.date)
  const label = legend.find(l => l.key === (c?.status || 'NONE'))?.label
  return c?.comment ? `${label}: ${c.comment}` : label
}

function dayClass(d) {
  return {
    'ac-off': d.sunday || !!d.holiday || d.lessons === 0,
    'ac-today': d.date === todayIso,
    'ac-selected': selectedDay.value?.date === d.date,
    'ac-future': d.date > todayIso
  }
}

const daySummary = computed(() => {
  const out = { PRESENT: [], LATE: [], ABSENT: [], EXCUSED: [], NONE: [] }
  if (!data.value || !selectedDay.value) return out
  for (const s of data.value.students) {
    const c = cellOf(s, selectedDay.value.date)
    out[c?.status || 'NONE'].push({ ...s, comment: c?.comment })
  }
  return out
})

function rateColor(rate) {
  if (rate == null) return 'inherit'
  if (rate >= 90) return '#10b981'
  if (rate >= 75) return '#f59e0b'
  return '#ef4444'
}

// --- quick change of a cell: one shared menu, optimistic update ---
const menuOpen = ref(false)
// shallowRef: a DOM element must not be wrapped in a reactive proxy (QMenu measures it)
const menuTarget = shallowRef(false)
const menuCell = ref(null)
const menuComment = ref('')
const pendingStatus = ref('PRESENT')

function openCell(event, student, day) {
  if (day.sunday || day.holiday || day.lessons === 0 || day.date > todayIso)
    return
  selectedDay.value = day
  menuCell.value = { student, day }
  menuComment.value = cellOf(student, day.date)?.comment || ''
  pendingStatus.value = cellOf(student, day.date)?.status || 'PRESENT'
  menuOpen.value = false
  menuTarget.value = event.currentTarget
  nextTick(() => (menuOpen.value = true))
}

async function choose(status) {
  const { student, day } = menuCell.value
  const before = cellOf(student, day.date)
  const comment = menuComment.value.trim() || null
  menuOpen.value = false
  // optimistic: show it now, roll back if the server says no
  student.cells = {
    ...student.cells,
    [day.date]: status ? { status, comment } : undefined
  }
  try {
    const res = await api.put('/api/attendance/day', {
      studentId: student.studentId,
      date: day.date,
      status,
      comment
    })
    student.cells = {
      ...student.cells,
      [day.date]: res.data.status ? res.data : undefined
    }
    recount()
  } catch (e) {
    student.cells = { ...student.cells, [day.date]: before || undefined }
    $q.notify({
      type: 'negative',
      message: e.friendlyMessage || "Saqlab bo'lmadi"
    })
  }
}

// the class's monthly % after a change — the server's exact (per-lesson) figure, fetched quietly
async function recount() {
  try {
    const res = await api.get('/api/attendance/calendar', {
      params: { schoolClassId: classId.value, month: month.value }
    })
    if (data.value && res.data.classId === data.value.classId)
      data.value.rate = res.data.rate
  } catch {
    // the cell itself is saved; the percentage refreshes on the next load
  }
}

loadClasses()
watch(
  () => schoolStore.activeSchoolId,
  () => {
    classId.value = null
    loadClasses()
  }
)
</script>

<style scoped>
.muted-text {
  color: var(--brand-text-muted);
}

.ac-month {
  gap: 4px;
}

.ac-month__title {
  min-width: 128px;
  text-align: center;
  font-weight: 700;
  font-size: 16px;
}

.ac-label {
  width: 40px;
  font-weight: 600;
  color: var(--brand-text-muted);
}

.ac-grade {
  min-width: 38px;
  font-weight: 700;
}

.ac-letter {
  font-weight: 600;
}

.ac-card {
  overflow: hidden;
  padding: 0;
}

.ac-scroll {
  overflow: auto;
  max-height: calc(100vh - 330px);
  min-height: 240px;
}

.ac-table {
  border-collapse: separate;
  border-spacing: 0;
  width: 100%;
}

.ac-table th,
.ac-table td {
  border-bottom: 1px solid var(--brand-border);
}

.ac-table thead th {
  position: sticky;
  top: 0;
  z-index: 2;
  background: var(--card-bg);
}

.ac-name-col {
  position: sticky;
  left: 0;
  z-index: 1;
  background: var(--card-bg);
  min-width: 170px;
  max-width: 170px;
  text-align: left;
  padding: 6px 8px;
  border-right: 1px solid var(--brand-border);
}

thead .ac-name-col {
  z-index: 3;
  font-size: 12px;
  color: var(--brand-text-muted);
}

.ac-student {
  font-weight: 600;
  font-size: 13px;
}

.ac-parent {
  font-size: 11px;
  color: var(--brand-text-muted);
}

/* 31 days × 23px + names fit next to the day panel at 1440px */
.ac-day {
  min-width: 23px;
  width: 23px;
  padding: 4px 0;
  text-align: center;
  cursor: pointer;
  user-select: none;
}

.ac-day__num {
  font-weight: 700;
  font-size: 13px;
}

.ac-day__wd {
  font-size: 10px;
  color: var(--brand-text-muted);
}

.ac-cell {
  text-align: center;
  cursor: pointer;
  padding: 0;
  height: 40px;
}

.ac-cell:hover {
  background: rgba(79, 70, 229, 0.07);
}

.ac-off {
  opacity: 0.35;
  cursor: default;
}

.ac-future {
  opacity: 0.55;
}

.ac-today .ac-day__num {
  color: var(--q-primary);
}

.ac-selected {
  background: rgba(79, 70, 229, 0.12) !important;
}

thead .ac-selected {
  box-shadow: inset 0 -3px 0 var(--q-primary);
}

.ac-dot {
  display: inline-block;
  width: 13px;
  height: 13px;
  border-radius: 50%;
  vertical-align: middle;
}

.ac-dot--PRESENT {
  background: #10b981;
}

.ac-dot--LATE {
  background: #f59e0b;
}

.ac-dot--ABSENT {
  background: #ef4444;
}

.ac-dot--EXCUSED {
  background: #3b82f6;
}

.ac-dot--NONE {
  background: transparent;
  border: 2px solid #cbd5e1;
  width: 11px;
  height: 11px;
}

.ac-day__num,
.ac-day__wd {
  letter-spacing: -0.3px;
}

.ac-footer {
  padding: 10px 14px;
  border-top: 1px solid var(--brand-border);
  font-size: 13px;
}

.ac-legend {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.ac-side {
  width: 250px;
  max-height: calc(100vh - 250px);
  overflow: auto;
}

.ac-count {
  font-size: 22px;
  font-weight: 800;
}

.ac-count--PRESENT {
  color: #10b981;
}

.ac-count--ABSENT {
  color: #ef4444;
}

.ac-count--LATE {
  color: #f59e0b;
}

.ac-count--EXCUSED {
  color: #3b82f6;
}

.ac-list-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 700;
  margin-bottom: 4px;
}

.ac-list-item {
  padding: 6px 0 6px 20px;
  border-bottom: 1px dashed var(--brand-border);
}

.ac-reason {
  font-style: italic;
}

@media (max-width: 1023px) {
  .ac-side {
    width: 100%;
    max-height: none;
  }
}

@media (min-width: 1024px) {
  .no-wrap-md {
    flex-wrap: nowrap;
  }
}
</style>

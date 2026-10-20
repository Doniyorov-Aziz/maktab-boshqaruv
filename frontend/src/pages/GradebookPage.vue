<template>
  <page-layout
    icon="grid_on"
    color="#f59e0b"
    title="Baholar jurnali"
    :subtitle="subtitle"
  >
    <template v-slot:actions>
      <div class="gbp__month" role="group" aria-label="Oy">
        <AppButton
          variant="ghost"
          size="sm"
          icon="chevron_left"
          aria-label="Oldingi oy"
          data-month-prev
          @click="moveMonth(-1)"
        />
        <span class="gbp__month-title" data-month-title>{{
          monthTitle(month)
        }}</span>
        <AppButton
          variant="ghost"
          size="sm"
          icon="chevron_right"
          aria-label="Keyingi oy"
          data-month-next
          @click="moveMonth(1)"
        />
      </div>
      <AppButton
        variant="secondary"
        size="sm"
        icon="today"
        label="Bugun"
        data-today
        @click="goToday"
      />
      <AppButton
        variant="secondary"
        size="sm"
        icon="download"
        icon-right="arrow_drop_down"
        label="Eksport"
        :disable="!journal"
        data-export
      >
        <q-menu anchor="bottom right" self="top right">
          <q-list dense class="gbp__export">
            <q-item
              v-close-popup
              clickable
              data-export-xlsx
              @click="exportFile('xlsx')"
            >
              <q-item-section avatar
                ><q-icon name="table_view" color="positive"
              /></q-item-section>
              <q-item-section>Excel (.xlsx)</q-item-section>
            </q-item>
            <q-item
              v-close-popup
              clickable
              data-export-pdf
              @click="exportFile('pdf')"
            >
              <q-item-section avatar
                ><q-icon name="picture_as_pdf" color="negative"
              /></q-item-section>
              <q-item-section>PDF</q-item-section>
            </q-item>
          </q-list>
        </q-menu>
      </AppButton>
    </template>

    <div class="gbp">
      <!-- class + subject -->
      <section class="gbp__bar dash-card" data-journal-controls>
        <ClassPicker
          :model-value="classId"
          compact
          :classes="classes"
          class="gbp__classes"
          @update:model-value="pickClass"
        />
        <div
          v-if="subjects.length"
          class="gbp__subjects"
          role="group"
          aria-label="Fan"
        >
          <SubjectChip
            v-for="s in subjects"
            :key="s.id"
            :subject-id="s.id"
            :name="s.name"
            :chosen-color="subjectColors[s.id]"
            :active="s.id === subjectId"
            :dim="!!subjectId && s.id !== subjectId"
            clickable
            @toggle="pickSubject"
          />
        </div>
        <q-skeleton
          v-else-if="loadingOverview"
          type="rect"
          class="gbp__skel-chips"
        />
      </section>

      <EmptyState
        v-if="noStudents"
        icon="group_off"
        title="Sinfda o'quvchi yo'q"
        text="Avval sinfga o'quvchilar qo'shing — keyin baho qo'yiladi."
        data-empty="students"
      />
      <EmptyState
        v-else-if="overview && !subjects.length"
        icon="menu_book"
        title="Bu sinfga fan biriktirilmagan"
        text="Dars jadvaliga shu sinf uchun darslar qo'shilgach, fanlar shu yerda chiqadi."
        data-empty="subjects"
      />

      <template v-else-if="subjectId">
        <JournalPanel
          :overview="overview"
          :subject-id="subjectId"
          :teacher="teacher"
          :current="monthStats"
          :previous="previous"
          :series="series"
          :now-minutes="now.minutes"
          :can-grade="canGrade"
          @grade-lesson="gradeLesson"
          @go-today="goToday"
        />

        <section class="gbp__table dash-card" data-journal-table>
          <div class="gbp__tools">
            <div class="gbp__legend" aria-label="Belgilar">
              <span
                v-for="v in GRADE_VALUES"
                :key="v"
                class="gbp__lg"
                :style="{ '--g': GRADE_COLOR[v] }"
                >{{ v }}</span
              >
              <span class="gbp__lg gbp__lg--exam">N</span
              ><small>nazorat / chorak</small>
              <span class="gbp__lg gbp__lg--hw">U</span
              ><small>uy vazifasi</small> <span class="gbp__lg-dot" /><small
                >demo</small
              >
            </div>
            <span v-if="canGrade" class="gbp__keys gt-sm">
              <kbd>2</kbd>–<kbd>5</kbd> baho va pastga · <kbd>Enter</kbd> oyna ·
              <kbd>Del</kbd> o'chirish · <kbd>Shift</kbd>+strelka blok
            </span>
            <q-toggle
              v-model="heat"
              dense
              label="Issiqlik xaritasi"
              class="gbp__heat"
              data-heat-toggle
            />
          </div>

          <div v-if="loading && !journal" class="gbp__skel">
            <q-skeleton
              v-for="i in 10"
              :key="i"
              type="rect"
              class="gbp__skel-row"
            />
          </div>
          <GradeTable
            v-else-if="journal"
            ref="table"
            :days="journal.days"
            :students="journal.students"
            :cells="cells"
            :row-stats="rowStats"
            :day-avg="dayAvg"
            :total="monthStats"
            :now-date="now.date"
            :can-grade="canGrade"
            :heat="heat"
            :pulse-date="pulseDate"
            :open-cell="pop ? { r: pop.r, c: pop.c } : null"
            :label="`${className} · ${subjectName}: baholar`"
            @quick="onQuick"
            @open="onOpen"
            @remove="onRemove"
            @column="onColumn"
            @student="openStudent"
          />
        </section>
      </template>
    </div>

    <GradePopover
      :pop="pop"
      @choose="onChoose"
      @remove="removeGrade"
      @close="closePop"
    />
    <ColumnDrawer
      v-model="column.open"
      :day="column.day"
      :subject-name="subjectName"
      :students="journal?.students || []"
      :existing="column.existing"
      :attendance="column.attendance"
      :saving="column.saving"
      @save="saveColumn"
    />
    <StudentDrawer
      v-model="drawer.open"
      :student="drawer.student"
      :card="drawer.card"
      :subject-name="subjectName"
    />
  </page-layout>
</template>

<script setup>
// The grade journal: a class (compact picker) and its subjects as chips (the subject going on
// now, else the one chosen last time, else the first), a band with both teachers, today's
// lesson and the month's four numbers against last month, then the month as a table built for
// speed — see GradeTable for the keys. Grades are saved one by one at once (optimistic), a
// deletion waits 5 s for "Bekor qilish". VIEWER sees everything and changes nothing.
import {
  ref,
  reactive,
  computed,
  watch,
  nextTick,
  onBeforeUnmount,
  markRaw
} from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useQuasar } from 'quasar'
import { api } from '@/boot/axios'
import PageLayout from '@/components/PageLayout.vue'
import AppButton from '@/components/app/AppButton.vue'
import ClassPicker from '@/components/ClassPicker.vue'
import SubjectChip from '@/components/pages/SubjectChip.vue'
import EmptyState from '@/components/pages/EmptyState.vue'
import JournalPanel from '@/components/journal/JournalPanel.vue'
import GradeTable from '@/components/journal/GradeTable.vue'
import GradePopover from '@/components/journal/GradePopover.vue'
import ColumnDrawer from '@/components/journal/ColumnDrawer.vue'
import StudentDrawer from '@/components/journal/StudentDrawer.vue'
import {
  GRADE_VALUES,
  GRADE_COLOR,
  stats,
  trend,
  runningSeries,
  blockedReason,
  monthTitle,
  shiftMonth,
  pickSubject as autoSubject
} from '@/components/journal/journal'
import { useSchoolStore } from '@/stores/school'
import { useAuthStore } from '@/stores/auth'
import { tashkentNow } from '@/utils/tashkent'

const LAST_CLASS = 'journal.classId'
const LAST_SUBJECT = 'journal.subjectId'
const UNDO_MS = 5000

const $q = useQuasar()
const route = useRoute()
const router = useRouter()
const schoolStore = useSchoolStore()
const authStore = useAuthStore()
const canGrade = computed(() => authStore.isEditor)

const now = ref(tashkentNow())
const ticker = setInterval(() => (now.value = tashkentNow()), 30000)

const classes = ref([])
const classId = ref(Number(route.query.class) || null)
const subjectId = ref(Number(route.query.subject) || null)
const month = ref(
  /^\d{4}-\d{2}$/.test(route.query.month || '')
    ? route.query.month
    : now.value.month
)
const overview = ref(null)
const loadingOverview = ref(false)
const journal = ref(null)
const loading = ref(false)
const previous = ref(null)
const subjectColors = ref({})
const heat = ref(false)
const table = ref(null)

// ---------------------------------------------------------------- names

const subjects = computed(() => overview.value?.subjects || [])
const selectedClass = computed(() =>
  classes.value.find(c => c.id === classId.value)
)
const className = computed(
  () =>
    overview.value?.className ||
    (selectedClass.value
      ? `${selectedClass.value.gradeNumber}-${selectedClass.value.sectionLetter}`
      : '')
)
const subjectName = computed(
  () => subjects.value.find(s => s.id === subjectId.value)?.name || ''
)
const noStudents = computed(() => selectedClass.value?.studentCount === 0)
const teacher = computed(
  () =>
    journal.value?.subjectTeacher ||
    subjects.value.find(s => s.id === subjectId.value)?.teacher ||
    null
)
const subtitle = computed(() =>
  [className.value, subjectName.value, monthTitle(month.value)]
    .filter(Boolean)
    .join(' · ')
)

// ---------------------------------------------------------------- loading

function remember(key, value) {
  try {
    localStorage.setItem(key, String(value))
  } catch {
    // private window: nothing is remembered
  }
}

function recall(key) {
  try {
    return Number(localStorage.getItem(key)) || null
  } catch {
    return null
  }
}

function syncUrl() {
  router.replace({
    query: {
      class: classId.value || undefined,
      subject: subjectId.value || undefined,
      month: month.value
    }
  })
}

async function loadClasses() {
  if (!schoolStore.activeSchoolId) return
  const res = await api.get('/api/school-classes', {
    params: {
      schoolId: schoolStore.activeSchoolId,
      size: 300,
      sort: 'gradeNumber,asc'
    }
  })
  classes.value = res.data.content || []
  if (classId.value && !classes.value.some(c => c.id === classId.value)) {
    classId.value = null
    subjectId.value = null
  }
}

async function loadSubjectColors() {
  try {
    const res = await api.get('/api/subjects', {
      background: true,
      params: { schoolId: schoolStore.activeSchoolId, size: 500 }
    })
    subjectColors.value = Object.fromEntries(
      (res.data.content || []).map(s => [s.id, s.color])
    )
  } catch {
    subjectColors.value = {}
  }
}

async function loadOverview() {
  if (!classId.value) return
  const id = classId.value
  loadingOverview.value = true
  try {
    const res = await api.get(`/api/classes/${id}/overview`)
    if (id !== classId.value) return
    overview.value = res.data
    const has = sid => res.data.subjects.some(s => s.id === sid)
    if (!subjectId.value || !has(subjectId.value))
      subjectId.value = autoSubject(
        res.data,
        now.value.date,
        now.value.minutes,
        recall(LAST_SUBJECT)
      )
  } catch (e) {
    if (id === classId.value) {
      overview.value = null
      notifyError(e)
    }
  } finally {
    loadingOverview.value = false
  }
}

async function loadJournal() {
  flushDeletes()
  pop.value = null
  if (!classId.value || !subjectId.value) {
    journal.value = null
    return
  }
  const key = `${classId.value}|${subjectId.value}|${month.value}`
  const same = () =>
    key === `${classId.value}|${subjectId.value}|${month.value}`
  loading.value = true
  loadSummary(key, same)
  try {
    const res = await api.get('/api/grades/journal', {
      params: {
        classId: classId.value,
        subjectId: subjectId.value,
        month: month.value
      }
    })
    if (!same()) return
    journal.value = res.data
    await nextTick()
    table.value?.scrollToDate(pulseDate.value || now.value.date)
  } catch (e) {
    notifyError(e)
  } finally {
    loading.value = false
  }
}

async function loadSummary(key, same) {
  previous.value = null
  try {
    const res = await api.get('/api/pages/journal/summary', {
      background: true,
      params: {
        classId: classId.value,
        subjectId: subjectId.value,
        month: month.value
      }
    })
    if (same()) previous.value = res.data.previous
  } catch {
    // the deltas stay empty
  }
}

async function pickClass(id) {
  if (!id || id === classId.value) return
  classId.value = id
  remember(LAST_CLASS, id)
  overview.value = null
  journal.value = null
  subjectId.value = null
  await loadOverview()
  syncUrl()
  await loadJournal()
}

async function pickSubject(id) {
  if (id === subjectId.value) return
  subjectId.value = id
  remember(LAST_SUBJECT, id)
  syncUrl()
  await loadJournal()
}

function moveMonth(delta) {
  month.value = shiftMonth(month.value, delta)
  syncUrl()
  loadJournal()
}

async function goToday() {
  pulse(now.value.date)
  if (month.value !== now.value.month) {
    month.value = now.value.month
    syncUrl()
    await loadJournal()
  } else table.value?.scrollToDate(now.value.date)
  focusToday()
}

/** "Shu darsga baho qo'yish": the lesson's subject, this month, today's column, first student. */
async function gradeLesson(lesson) {
  const changed =
    subjectId.value !== lesson.subjectId || month.value !== now.value.month
  subjectId.value = lesson.subjectId
  month.value = now.value.month
  pulse(now.value.date)
  syncUrl()
  if (changed) await loadJournal()
  else table.value?.scrollToDate(now.value.date)
  focusToday()
}

function focusToday() {
  nextTick(() => {
    const ci = journal.value?.days.findIndex(d => d.date === now.value.date)
    if (ci >= 0 && journal.value.students.length) table.value?.focusCell(0, ci)
  })
}

const pulseDate = ref(null)
let pulseTimer = null
function pulse(date) {
  pulseDate.value = null
  clearTimeout(pulseTimer)
  requestAnimationFrame(() => {
    pulseDate.value = date
    pulseTimer = setTimeout(() => (pulseDate.value = null), 2200)
  })
}

// ---------------------------------------------------------------- numbers

const cells = computed(() => {
  const map = {}
  for (const g of journal.value?.grades || [])
    (map[g.studentId + '|' + g.date] ||= []).push(g)
  return map
})
const monthStats = computed(() =>
  stats((journal.value?.grades || []).map(g => g.value))
)
const series = computed(() => runningSeries(journal.value?.grades || []))
const rowStats = computed(() => {
  const by = {}
  for (const g of journal.value?.grades || []) (by[g.studentId] ||= []).push(g)
  const out = {}
  for (const [id, list] of Object.entries(by))
    out[id] = { ...stats(list.map(g => g.value)), trend: trend(list) }
  return out
})
const dayAvg = computed(() => {
  const by = {}
  for (const g of journal.value?.grades || []) (by[g.date] ||= []).push(g.value)
  return Object.fromEntries(
    Object.entries(by).map(([d, v]) => [
      d,
      v.reduce((a, b) => a + b, 0) / v.length
    ])
  )
})

// ---------------------------------------------------------------- grading

function tellBlocked(d) {
  $q.notify({
    message: blockedReason(d),
    color: 'grey-9',
    textColor: 'white',
    timeout: 3000,
    position: 'top'
  })
}

let tmpId = 0
/** Puts a grade in at once and saves it; false when the server refused (the chip goes away). */
async function createGrade(
  studentId,
  date,
  value,
  type = 'CURRENT',
  comment = ''
) {
  const temp = reactive({
    id: `tmp-${++tmpId}`,
    studentId,
    date,
    value,
    type,
    comment: comment || null,
    createdBy: null,
    createdAt: null,
    source: 'MANUAL'
  })
  journal.value.grades.push(temp)
  try {
    const res = await api.post(
      '/api/grades',
      {
        studentId,
        subjectId: subjectId.value,
        gradeDate: date,
        score: value,
        type,
        comment: comment || null
      },
      { background: true }
    )
    Object.assign(temp, {
      id: res.data.id,
      createdBy: res.data.createdBy,
      createdAt: res.data.createdAt
    })
    return true
  } catch (e) {
    if (journal.value)
      journal.value.grades = journal.value.grades.filter(g => g !== temp)
    notifyError(e)
    return false
  }
}

async function updateGrade(g, value, type, comment) {
  const before = {
    value: g.value,
    type: g.type,
    comment: g.comment,
    source: g.source
  }
  Object.assign(g, { value, type, comment: comment || null })
  try {
    await api.put(
      `/api/grades/${g.id}`,
      {
        studentId: g.studentId,
        subjectId: subjectId.value,
        gradeDate: g.date,
        score: value,
        type,
        comment: comment || null
      },
      { background: true }
    )
    // a staff edit makes a demo grade the teacher's own
    g.source = 'MANUAL'
  } catch (e) {
    Object.assign(g, before)
    notifyError(e)
  }
}

/** 2–5 on one cell or a block: every gradable cell of it gets the grade. */
function onQuick({ cells: list, value }) {
  let blocked = null
  for (const { r, c } of list) {
    const d = journal.value.days[c]
    if (blockedReason(d)) {
      blocked ||= d
      continue
    }
    createGrade(journal.value.students[r].id, d.date, value)
  }
  if (blocked && list.length === 1) tellBlocked(blocked)
}

// ---- the cell window

const pop = ref(null)
const dayAttendance = {}

async function attendanceOf(date) {
  if (dayAttendance[`${subjectId.value}|${date}`])
    return dayAttendance[`${subjectId.value}|${date}`]
  try {
    const res = await api.get('/api/pages/journal/day-attendance', {
      background: true,
      params: { classId: classId.value, subjectId: subjectId.value, date }
    })
    dayAttendance[`${subjectId.value}|${date}`] = res.data || {}
  } catch {
    return {}
  }
  return dayAttendance[`${subjectId.value}|${date}`]
}

async function onOpen({ r, c, el, gradeId }) {
  const d = journal.value.days[c]
  const s = journal.value.students[r]
  const grade = gradeId
    ? (cells.value[s.id + '|' + d.date] || []).find(
        g => String(g.id) === gradeId
      )
    : null
  if (!grade && blockedReason(d)) return tellBlocked(d)
  if (!canGrade.value) return
  // markRaw: the menu measures the real element, not a reactive proxy of it
  pop.value = { r, c, el: markRaw(el), student: s, day: d, grade, absent: null }
  const att = await attendanceOf(d.date)
  const st = att[s.id]
  if (
    pop.value?.student.id === s.id &&
    pop.value.day.date === d.date &&
    (st === 'ABSENT' || st === 'EXCUSED')
  )
    pop.value = { ...pop.value, absent: st }
}

function closePop() {
  const had = pop.value
  pop.value = null
  if (had) table.value?.focusCell(had.r, had.c, { scroll: false })
}

function onChoose({ value, type, comment }) {
  const p = pop.value
  if (!p) return
  pop.value = null
  if (p.grade) updateGrade(p.grade, value, type, comment)
  else createGrade(p.student.id, p.day.date, value, type, comment)
  table.value?.focusCell(
    Math.min(journal.value.students.length - 1, p.r + 1),
    p.c
  )
}

// ---- deleting (5 s to undo)

const pendingDeletes = new Map()

function onRemove({ r, c }) {
  const d = journal.value.days[c]
  const list = cells.value[journal.value.students[r].id + '|' + d.date]
  if (list?.length) removeGrade(list[list.length - 1])
}

function removeGrade(g) {
  pop.value = null
  const list = journal.value.grades
  const index = list.indexOf(g)
  if (index < 0) return
  list.splice(index, 1)
  if (String(g.id).startsWith('tmp-')) return
  pendingDeletes.set(
    g.id,
    setTimeout(() => sendDelete(g.id), UNDO_MS)
  )
  $q.notify({
    message: `${g.value} bahosi o'chirildi`,
    icon: 'delete_outline',
    color: 'grey-9',
    textColor: 'white',
    timeout: UNDO_MS,
    progress: true,
    attrs: { 'data-undo-toast': '' },
    actions: [
      {
        label: 'Bekor qilish',
        color: 'amber',
        noCaps: true,
        attrs: { 'data-undo': '' },
        handler: () => {
          clearTimeout(pendingDeletes.get(g.id))
          pendingDeletes.delete(g.id)
          if (journal.value) journal.value.grades.push(g)
        }
      }
    ]
  })
}

async function sendDelete(id) {
  pendingDeletes.delete(id)
  try {
    await api.delete(`/api/grades/${id}`, { background: true })
  } catch (e) {
    notifyError(e)
    loadJournal()
  }
}

function flushDeletes() {
  for (const [id, timer] of pendingDeletes) {
    clearTimeout(timer)
    sendDelete(id)
  }
}

// ---- a whole column

const column = reactive({
  open: false,
  day: null,
  existing: {},
  attendance: {},
  saving: false
})

async function onColumn(c) {
  const d = journal.value.days[c]
  if (blockedReason(d)) return tellBlocked(d)
  if (!canGrade.value) return
  const existing = {}
  for (const s of journal.value.students)
    existing[s.id] = cells.value[s.id + '|' + d.date] || []
  Object.assign(column, { open: true, day: d, existing, attendance: {} })
  column.attendance = await attendanceOf(d.date)
}

async function saveColumn({ entries, type }) {
  column.saving = true
  const results = await Promise.all(
    entries.map(e => createGrade(e.studentId, column.day.date, e.value, type))
  )
  column.saving = false
  const ok = results.filter(Boolean).length
  if (ok) {
    $q.notify({
      type: 'positive',
      message: `${ok} ta baho saqlandi`,
      icon: 'check_circle'
    })
    pulse(column.day.date)
    column.open = false
  }
}

// ---- a student

const drawer = reactive({ open: false, student: null, card: null })

async function openStudent(s) {
  Object.assign(drawer, { open: true, student: s, card: null })
  try {
    const res = await api.get('/api/pages/journal/student', {
      params: { studentId: s.id, subjectId: subjectId.value }
    })
    if (drawer.student?.id === s.id) drawer.card = res.data
  } catch (e) {
    notifyError(e)
  }
}

// ---------------------------------------------------------------- export

async function exportFile(kind) {
  try {
    const res = await api.get(`/api/pages/journal/export.${kind}`, {
      params: {
        classId: classId.value,
        subjectId: subjectId.value,
        month: month.value
      },
      responseType: 'blob'
    })
    const url = URL.createObjectURL(res.data)
    const a = document.createElement('a')
    a.href = url
    a.download =
      `jurnal-${className.value}-${subjectName.value}-${month.value}.${kind}`.replace(
        /\s+/g,
        '_'
      )
    a.click()
    setTimeout(() => URL.revokeObjectURL(url), 1000)
  } catch (e) {
    notifyError(e)
  }
}

function notifyError(e) {
  $q.notify({
    type: 'negative',
    message: e?.friendlyMessage || 'Xatolik yuz berdi'
  })
}

// ---------------------------------------------------------------- opening

/**
 * Which class and subject open: the link (?class&subject&month), else the teacher's lesson going
 * on now, else the class chosen last time, else the first class with students; the subject by
 * pickSubject (the lesson now / the last one / the first).
 */
async function start() {
  await loadClasses().catch(notifyError)
  loadSubjectColors()
  if (!classId.value) {
    try {
      const res = await api.get('/api/me/current-lesson', { background: true })
      if (res.status === 200 && res.data?.classId) {
        classId.value = res.data.classId
        subjectId.value = res.data.subjectId
        pulse(now.value.date)
      }
    } catch {
      // no lesson of one's own now
    }
  }
  if (!classId.value) {
    const usable = c => c.studentCount == null || c.studentCount > 0
    const last = recall(LAST_CLASS)
    classId.value =
      (
        classes.value.find(c => c.id === last && usable(c)) ||
        classes.value.find(usable)
      )?.id ?? null
  }
  if (!classId.value) return
  await loadOverview()
  syncUrl()
  await loadJournal()
  if (pulseDate.value) focusToday()
}

watch(
  () => schoolStore.activeSchoolId,
  (id, old) => {
    if (!id) return
    if (old && old !== id) {
      flushDeletes()
      classId.value = null
      subjectId.value = null
      overview.value = null
      journal.value = null
    }
    start()
  },
  { immediate: true }
)

onBeforeUnmount(() => {
  clearInterval(ticker)
  clearTimeout(pulseTimer)
  flushDeletes()
})
</script>

<style scoped>
.gbp {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.gbp__bar,
.gbp__table {
  padding: var(--space-3) var(--space-4);
}

.gbp__bar:hover,
.gbp__table:hover {
  transform: none;
}

.gbp__bar {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.gbp__subjects {
  display: flex;
  flex-wrap: nowrap;
  gap: var(--space-2);
  overflow-x: auto;
  scrollbar-width: thin;
  padding-bottom: var(--hairline);
}

.gbp__subjects > * {
  flex: none;
}

.gbp__month {
  display: flex;
  align-items: center;
  gap: var(--space-1);
}

.gbp__month-title {
  min-width: var(--space-8);
  padding: 0 var(--space-2);
  font-weight: 700;
  text-align: center;
  white-space: nowrap;
}

.gbp__export {
  min-width: var(--field-w-sm);
}

.gbp__table {
  padding: 0;
  overflow: hidden;
}

.gbp__tools {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--space-4);
  padding: var(--space-2) var(--space-4);
  border-bottom: 1px solid var(--dash-edge);
}

.gbp__legend {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
}

.gbp__legend small {
  margin-right: var(--space-2);
}

.gbp__lg {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: var(--avatar-xs);
  height: var(--avatar-xs);
  border-radius: var(--radius-sm);
  background: color-mix(in srgb, var(--g) 18%, transparent);
  color: var(--g);
  font-weight: 800;
}

.gbp__lg--exam {
  --g: var(--brand-text-muted);
  margin-left: var(--space-2);
  box-shadow: inset 0 0 0 2px var(--g);
}

.gbp__lg--hw {
  --g: var(--brand-text-muted);
  outline: var(--hairline) dashed var(--g);
  outline-offset: calc(var(--hairline) * -3);
}

.gbp__lg-dot {
  width: var(--space-1);
  height: var(--space-1);
  border-radius: var(--radius-pill);
  background: var(--chart-primary);
}

.gbp__keys {
  margin-left: auto;
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
}

.gbp__heat {
  font-size: var(--text-sm);
}

kbd {
  display: inline-block;
  min-width: var(--space-5);
  padding: 0 var(--space-1);
  border: 1px solid var(--c-border-control);
  border-radius: var(--radius-sm);
  font: inherit;
  font-size: var(--text-xs);
  text-align: center;
}

.gbp__skel {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  padding: var(--space-3);
}

.gbp__skel-row {
  height: var(--gb-row-h);
  border-radius: var(--radius-sm);
}

.gbp__skel-chips {
  height: var(--control-h-sm);
  border-radius: var(--radius-pill);
}

@media (max-width: 599px) {
  .gbp__tools {
    gap: var(--space-2);
  }

  .gbp__legend small {
    display: none;
  }
}
</style>

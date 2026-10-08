<template>
  <page-layout
    icon="grid_on"
    color="#f59e0b"
    title="Baholar jurnali"
    :subtitle="subtitle"
  >
    <template v-slot:actions>
      <q-btn
        v-if="canGrade"
        unelevated
        no-caps
        color="primary"
        icon="playlist_add_check"
        label="Baho qo'yish"
        :disable="!journal"
        @click="openBulk"
      >
        <q-tooltip>Bir kunga butun sinfga tez baho qo'yish</q-tooltip>
      </q-btn>
    </template>

    <!-- 1. class, subject, month -->
    <div class="brand-card q-pa-sm q-mb-md">
      <class-picker
        :classes="classes"
        :model-value="classId"
        @update:model-value="pickClass"
      />
      <div class="row items-center q-col-gutter-sm q-mt-xs jr-filters">
        <div class="col-12 col-sm-6 col-md-4 row items-center no-wrap">
          <span class="jr-label">Fan:</span>
          <q-select
            v-model="subjectId"
            class="col"
            dense
            outlined
            emit-value
            map-options
            options-dense
            :options="subjectOptions"
            :disable="!subjectOptions.length"
            placeholder="Fanni tanlang"
            @update:model-value="onSubjectChange"
          />
        </div>
        <div v-if="subjectTeacher" class="col-auto jr-teacher">
          O'qituvchi: <b>{{ shortName(subjectTeacher.fullName) }}</b>
        </div>
        <q-space />
        <div class="col-auto row items-center no-wrap jr-month">
          <q-btn
            flat
            round
            dense
            icon="chevron_left"
            aria-label="Oldingi oy"
            @click="shiftMonth(-1)"
          />
          <div class="jr-month-title">{{ monthTitle }}</div>
          <q-btn
            flat
            round
            dense
            icon="chevron_right"
            aria-label="Keyingi oy"
            :disable="month >= now.month"
            @click="shiftMonth(1)"
          />
          <q-btn
            outline
            dense
            no-caps
            color="primary"
            label="Bugun"
            class="q-ml-sm q-px-sm"
            @click="goToday"
          />
        </div>
      </div>
    </div>

    <div v-if="autoLesson" class="jr-auto q-mb-sm">
      <q-icon name="school" size="18px" class="q-mr-xs" />
      Sizning hozirgi darsingiz:
      <b>{{ autoLesson.className }} · {{ autoLesson.subjectName }}</b>
    </div>

    <!-- 2. class panel -->
    <!-- the panel could not be loaded: say so, offer a retry — the journal below still works -->
    <div
      v-if="classId && overviewError"
      class="brand-card q-pa-md q-mb-md row items-center no-wrap jr-panel-error"
      role="alert"
    >
      <q-icon
        name="error_outline"
        size="22px"
        class="q-mr-sm"
        color="negative"
      />
      <span class="col">Sinf ma'lumotini yuklab bo'lmadi</span>
      <q-btn
        flat
        dense
        no-caps
        color="primary"
        icon="refresh"
        label="Qayta urinish"
        :loading="overviewRetrying"
        @click="retryOverview"
      />
    </div>
    <class-overview-panel
      v-else-if="classId"
      class="q-mb-md"
      :overview="overview"
      :now-minutes="now.minutes"
      :subject-id="subjectId"
      :can-grade="canGrade"
      :is-admin="authStore.isAdmin"
      @pick-subject="onSubjectChange"
      @grade-lesson="gradeLesson"
    />

    <!-- 5. empty state -->
    <div
      v-if="!classId || !subjectId"
      class="brand-card q-pa-xl column flex-center text-center jr-empty"
    >
      <div class="jr-empty-art" aria-hidden="true">📒</div>
      <div class="text-h6 q-mt-sm">Sinf va fanni tanlang</div>
      <div class="jr-muted">
        Jurnal oyning har bir kuni bo'yicha ochiladi: bo'sh katakdagi «+» ni
        bosib baho qo'ying.
      </div>
    </div>

    <template v-else>
      <!-- statistics -->
      <div class="row items-center q-gutter-sm q-mb-sm jr-stats">
        <q-chip dense icon="groups" class="jr-chip"
          >{{ journal ? journal.students.length : '—' }} o'quvchi</q-chip
        >
        <q-chip dense icon="grade" class="jr-chip"
          >{{ journal ? journal.grades.length : '—' }} ta baho shu oyda</q-chip
        >
        <q-chip dense icon="functions" class="jr-chip"
          >Sinf o'rtachasi:
          <b class="q-ml-xs" :class="avgClass(classAverage)">{{
            fmtAvg(classAverage)
          }}</b></q-chip
        >
        <q-space />
        <span v-if="canGrade" class="jr-hint gt-sm"
          >Katakka bosing yoki strelkalar bilan yuring · 2–5 baho qo'yadi ·
          Delete o'chiradi</span
        >
      </div>

      <div v-if="!journal" class="brand-card q-pa-md">
        <q-skeleton height="38px" class="q-mb-sm" />
        <q-skeleton v-for="i in 8" :key="i" height="30px" class="q-mb-xs" />
      </div>

      <!-- 3. the journal -->
      <div
        v-else
        ref="scrollEl"
        class="brand-card jr-scroll"
        :class="{ 'jr-stale': loading }"
      >
        <table
          ref="tableEl"
          class="jr-table"
          :class="{ 'jr-readonly': !canGrade }"
          @click="onTableClick"
          @keydown="onKeydown"
        >
          <thead>
            <tr>
              <th class="jr-no jr-sticky-l">№</th>
              <th class="jr-name jr-sticky-l2">O'quvchi</th>
              <th
                v-for="(d, ci) in journal.days"
                :key="d.date"
                class="jr-day"
                :class="dayClass(d)"
                :data-col="ci"
                :title="dayTitle(d)"
              >
                <div class="jr-day-date">{{ dm(d.date) }}</div>
                <div class="jr-day-wd">
                  {{ d.weekday
                  }}<span
                    v-if="d.hasLesson"
                    class="jr-dot"
                    aria-label="dars bor"
                    >•</span
                  >
                </div>
                <div v-if="d.date === now.date" class="jr-today-tag">Bugun</div>
              </th>
              <th class="jr-avg jr-sticky-r">O'rtacha</th>
              <th class="jr-count">Soni</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(s, ri) in journal.students" :key="s.id">
              <td class="jr-no jr-sticky-l">{{ ri + 1 }}</td>
              <td class="jr-name jr-sticky-l2" :title="s.fullName">
                {{ s.fullName }}
              </td>
              <td
                v-for="(d, ci) in journal.days"
                :key="d.date"
                class="jr-cell"
                :class="cellClass(d, ri, ci)"
                :data-row="ri"
                :data-col="ci"
                :tabindex="focus.r === ri && focus.c === ci ? 0 : -1"
              >
                <template v-if="cells[s.id + '|' + d.date]">
                  <!-- at most two chips side by side; the rest as "+N" (all of them in its tooltip) -->
                  <span
                    v-for="g in cells[s.id + '|' + d.date].slice(0, 2)"
                    :key="g.id"
                    class="jr-grade"
                    :class="'jr-grade--' + g.value"
                    :data-grade="g.id"
                    :title="gradeTitle(g)"
                    >{{ g.value }}</span
                  ><span
                    v-if="cells[s.id + '|' + d.date].length > 2"
                    class="jr-more"
                    :title="
                      cells[s.id + '|' + d.date].map(g => g.value).join(', ')
                    "
                    >+{{ cells[s.id + '|' + d.date].length - 2 }}</span
                  >
                </template>
                <span v-else-if="canGrade && isGradable(d)" class="jr-plus"
                  >+</span
                >
              </td>
              <td class="jr-avg jr-sticky-r">
                <span :class="avgClass(studentAvg[s.id])">{{
                  fmtAvg(studentAvg[s.id])
                }}</span>
              </td>
              <td class="jr-count">{{ studentCount[s.id] || 0 }}</td>
            </tr>
            <tr v-if="!journal.students.length">
              <td :colspan="journal.days.length + 4" class="jr-muted q-pa-md">
                Bu sinfda o'quvchi yo'q
              </td>
            </tr>
          </tbody>
          <tfoot v-if="journal.students.length">
            <tr>
              <td class="jr-no jr-sticky-l"></td>
              <td class="jr-name jr-sticky-l2 jr-foot-label"
                >Sinf o'rtachasi</td
              >
              <td
                v-for="d in journal.days"
                :key="d.date"
                class="jr-foot"
                :class="dayClass(d)"
              >
                <span
                  v-if="dayAvg[d.date] != null"
                  :class="avgClass(dayAvg[d.date])"
                  >{{ dayAvg[d.date].toFixed(1) }}</span
                >
              </td>
              <td class="jr-avg jr-sticky-r">
                <b :class="avgClass(classAverage)">{{
                  fmtAvg(classAverage)
                }}</b>
              </td>
              <td class="jr-count">{{ journal.grades.length }}</td>
            </tr>
          </tfoot>
        </table>
      </div>
    </template>

    <!-- the one popover for every cell -->
    <div
      v-if="pop"
      ref="popEl"
      class="jr-pop"
      role="dialog"
      aria-label="Baho"
      :style="{ top: pop.y + 'px', left: pop.x + 'px' }"
      @keydown.esc.stop="closePop"
    >
      <div class="jr-pop-head">
        <b>{{ pop.student.fullName }}</b>
        <span class="jr-muted"> · {{ dm(pop.day.date) }}</span>
      </div>
      <div class="row no-wrap q-gutter-x-xs q-my-sm">
        <button
          v-for="v in [2, 3, 4, 5]"
          :key="v"
          type="button"
          class="jr-pop-btn"
          :class="[
            'jr-grade--' + v,
            { 'jr-pop-btn--on': pop.grade && pop.grade.value === v }
          ]"
          :aria-label="`Baho ${v}`"
          @click="choose(v)"
        >
          {{ v }}
        </button>
      </div>
      <q-btn-toggle
        v-model="pop.type"
        dense
        no-caps
        unelevated
        spread
        size="sm"
        toggle-color="primary"
        class="q-mb-sm"
        :options="typeOptions"
      />
      <q-input
        v-model="pop.comment"
        dense
        outlined
        maxlength="200"
        placeholder="Izoh (ixtiyoriy)"
        @keydown.enter.prevent="pop.grade && choose(pop.grade.value)"
      />
      <div class="row items-center q-mt-sm">
        <q-btn
          v-if="pop.grade"
          flat
          dense
          no-caps
          color="negative"
          icon="delete_outline"
          label="O'chirish"
          @click="removeGrade(pop.grade)"
        />
        <q-space />
        <q-btn flat dense no-caps label="Yopish" @click="closePop" />
      </div>
    </div>

    <!-- quick grades for the whole class on one day -->
    <q-dialog v-model="bulk.open">
      <q-card class="jr-bulk">
        <q-card-section class="row items-center q-pb-sm">
          <div class="text-h6">Baho qo'yish · {{ overview?.className }}</div>
          <q-space />
          <q-btn flat round dense icon="close" v-close-popup />
        </q-card-section>
        <q-card-section class="row q-col-gutter-sm q-pt-none">
          <div class="col-12 col-sm-6">
            <date-field v-model="bulk.date" label="Sana" no-sundays no-future />
          </div>
          <div class="col-12 col-sm-6">
            <q-btn-toggle
              v-model="bulk.type"
              no-caps
              unelevated
              spread
              toggle-color="primary"
              :options="typeOptions"
            />
          </div>
        </q-card-section>
        <q-separator />
        <div class="jr-bulk-list scroll">
          <div
            v-for="(s, i) in journal?.students || []"
            :key="s.id"
            class="row items-center no-wrap q-px-md q-py-xs jr-bulk-row"
          >
            <span class="jr-bulk-no">{{ i + 1 }}</span>
            <span class="col ellipsis">{{ s.fullName }}</span>
            <button
              v-for="v in [2, 3, 4, 5]"
              :key="v"
              type="button"
              class="jr-pop-btn jr-pop-btn--sm"
              :class="[
                'jr-grade--' + v,
                { 'jr-pop-btn--on': bulk.values[s.id] === v }
              ]"
              @click="bulk.values[s.id] = bulk.values[s.id] === v ? null : v"
            >
              {{ v }}
            </button>
          </div>
        </div>
        <q-card-actions align="right" class="q-pa-md">
          <span class="jr-muted q-mr-auto"
            >{{ bulkCount }} ta o'quvchiga baho</span
          >
          <q-btn flat no-caps label="Bekor qilish" v-close-popup />
          <q-btn
            unelevated
            no-caps
            color="primary"
            icon="save"
            label="Saqlash"
            :disable="!bulkCount || !bulk.date"
            :loading="bulk.saving"
            @click="saveBulk"
          />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </page-layout>
</template>

<script setup>
import {
  ref,
  reactive,
  computed,
  watch,
  nextTick,
  onMounted,
  onBeforeUnmount
} from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useQuasar } from 'quasar'
import { api } from '@/boot/axios'
import PageLayout from '@/components/PageLayout.vue'
import ClassPicker from '@/components/ClassPicker.vue'
import ClassOverviewPanel from '@/components/ClassOverviewPanel.vue'
import DateField from '@/components/DateField.vue'
import { useSchoolStore } from '@/stores/school'
import { useAuthStore } from '@/stores/auth'
import { monthName } from '@/utils/date'
import { tashkentNow, shortName } from '@/utils/tashkent'

const $q = useQuasar()
const route = useRoute()
const router = useRouter()
const schoolStore = useSchoolStore()
const authStore = useAuthStore()

const SUNDAY_TEXT =
  "😴 Uzr, bu kun yakshanba — maktab ishlamaydi. Baho qo'yib bo'lmaydi."
const FUTURE_TEXT = "⏳ Kelajak sanaga baho qo'yib bo'lmaydi."
const typeOptions = [
  { label: 'Joriy', value: 'CURRENT' },
  { label: 'Nazorat', value: 'EXAM' },
  { label: 'Chorak', value: 'QUARTERLY' }
]
const typeLabel = Object.fromEntries(typeOptions.map(o => [o.value, o.label]))

const canGrade = computed(() => authStore.isEditor)

// ------------------------------------------------------------------ time
const now = ref(tashkentNow())
const ticker = setInterval(() => (now.value = tashkentNow()), 30000)

// ------------------------------------------------------------------ selection
const classes = ref([])
const classId = ref(Number(route.query.class) || null)
const subjectId = ref(Number(route.query.subject) || null)
const month = ref(
  /^\d{4}-\d{2}$/.test(route.query.month || '')
    ? route.query.month
    : now.value.month
)
const overview = ref(null)
const journal = ref(null)
const loading = ref(false)
const autoLesson = ref(null)
const highlightDate = ref(null)

const overviewError = ref(false)
const fallbackSubjects = ref([])
const subjectOptions = computed(() =>
  (
    overview.value?.subjects ||
    (overviewError.value ? fallbackSubjects.value : [])
  ).map(s => ({ value: s.id, label: s.name }))
)
const subjectTeacher = computed(() => {
  if (journal.value?.subjectTeacher) return journal.value.subjectTeacher
  return overview.value?.subjects?.find(s => s.id === subjectId.value)?.teacher
})
const monthTitle = computed(() => {
  const [y, m] = month.value.split('-').map(Number)
  return `${monthName(m - 1)} ${y}`
})
const subtitle = computed(() =>
  [
    schoolStore.activeSchoolName,
    overview.value?.className,
    subjectOptions.value.find(o => o.value === subjectId.value)?.label
  ]
    .filter(Boolean)
    .join(' · ')
)

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
    params: { schoolId: schoolStore.activeSchoolId, size: 300 }
  })
  classes.value = res.data.content
  if (classId.value && !classes.value.some(c => c.id === classId.value)) {
    classId.value = null
    subjectId.value = null
  }
}

async function loadOverview() {
  if (!classId.value) return
  const id = classId.value
  overviewError.value = false
  try {
    const res = await api.get(`/api/classes/${id}/overview`)
    if (id !== classId.value) return
    overview.value = res.data
    // a subject this class does not have is dropped
    if (
      subjectId.value &&
      !res.data.subjects.some(s => s.id === subjectId.value)
    ) {
      subjectId.value = null
      syncUrl()
    }
  } catch {
    if (id !== classId.value) return
    // the panel shows an error with a retry; subjects still come from the school's active list
    overview.value = null
    overviewError.value = true
    await loadFallbackSubjects()
  }
}

/** Without the class overview: every active subject of the school. */
async function loadFallbackSubjects() {
  if (fallbackSubjects.value.length || !schoolStore.activeSchoolId) return
  try {
    const res = await api.get('/api/subjects', {
      background: true,
      params: { schoolId: schoolStore.activeSchoolId, size: 500, sort: 'name' }
    })
    fallbackSubjects.value = res.data.content.map(s => ({
      id: s.id,
      name: s.name
    }))
  } catch {
    fallbackSubjects.value = []
  }
}

const overviewRetrying = ref(false)
async function retryOverview() {
  overviewRetrying.value = true
  await loadOverview()
  overviewRetrying.value = false
}

async function loadJournal() {
  flushDeletes()
  if (!classId.value || !subjectId.value) {
    journal.value = null
    return
  }
  const key = `${classId.value}|${subjectId.value}|${month.value}`
  loading.value = true
  try {
    const res = await api.get('/api/grades/journal', {
      params: {
        classId: classId.value,
        subjectId: subjectId.value,
        month: month.value
      }
    })
    if (key !== `${classId.value}|${subjectId.value}|${month.value}`) return
    journal.value = res.data
    await nextTick()
    scrollToDate(highlightDate.value || now.value.date)
  } catch (e) {
    notifyError(e)
  } finally {
    loading.value = false
  }
}

function pickClass(id) {
  if (id === classId.value) return
  classId.value = id
  overview.value = null
  overviewError.value = false
  journal.value = null
  autoLesson.value = null
  syncUrl()
  loadOverview().then(loadJournal)
}

function onSubjectChange(id) {
  subjectId.value = id
  syncUrl()
  loadJournal()
}

function shiftMonth(delta) {
  const [y, m] = month.value.split('-').map(Number)
  const d = new Date(y, m - 1 + delta, 1)
  month.value = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
  syncUrl()
  loadJournal()
}

function goToday() {
  highlightDate.value = now.value.date
  if (month.value !== now.value.month) {
    month.value = now.value.month
    syncUrl()
    loadJournal()
  } else {
    scrollToDate(now.value.date)
  }
}

/** "Shu darsga baho qo'yish": subject of the lesson, this month, today's column lit up. */
async function gradeLesson(lesson) {
  highlightDate.value = now.value.date
  pulse()
  const changed =
    subjectId.value !== lesson.subjectId || month.value !== now.value.month
  subjectId.value = lesson.subjectId
  month.value = now.value.month
  syncUrl()
  if (changed) await loadJournal()
  else scrollToDate(now.value.date)
  focusTodayFirst()
}

const pulseOn = ref(false)
let pulseTimer = null
function pulse() {
  pulseOn.value = false
  clearTimeout(pulseTimer)
  requestAnimationFrame(() => {
    pulseOn.value = true
    pulseTimer = setTimeout(() => (pulseOn.value = false), 2000)
  })
}

// ------------------------------------------------------------------ table data
const cells = computed(() => {
  const map = {}
  for (const g of journal.value?.grades || []) {
    ;(map[g.studentId + '|' + g.date] ||= []).push(g)
  }
  return map
})

const studentAvg = computed(() => avgBy(g => g.studentId))
const dayAvg = computed(() => avgBy(g => g.date))
const studentCount = computed(() => {
  const c = {}
  for (const g of journal.value?.grades || [])
    c[g.studentId] = (c[g.studentId] || 0) + 1
  return c
})
const classAverage = computed(() => {
  const gs = journal.value?.grades || []
  return gs.length ? gs.reduce((a, g) => a + g.value, 0) / gs.length : null
})

function avgBy(key) {
  const sum = {}
  const cnt = {}
  for (const g of journal.value?.grades || []) {
    const k = key(g)
    sum[k] = (sum[k] || 0) + g.value
    cnt[k] = (cnt[k] || 0) + 1
  }
  const out = {}
  for (const k of Object.keys(sum)) out[k] = sum[k] / cnt[k]
  return out
}

function fmtAvg(v) {
  return v == null ? '—' : v.toFixed(2)
}

function avgClass(v) {
  if (v == null) return 'jr-avg-none'
  if (v >= 4.5) return 'jr-avg-5'
  if (v >= 3.5) return 'jr-avg-4'
  if (v >= 2.5) return 'jr-avg-3'
  return 'jr-avg-2'
}

function dm(date) {
  return `${date.slice(8, 10)}.${date.slice(5, 7)}`
}

const isGradable = d => !d.isSunday && !d.isHoliday && !d.isFuture

function dayClass(d) {
  return {
    'jr-sunday': d.isSunday,
    'jr-holiday': d.isHoliday && !d.isSunday,
    'jr-future': d.isFuture && !d.isSunday,
    'jr-today': d.date === now.value.date,
    'jr-lesson': d.hasLesson,
    'jr-pulse': pulseOn.value && d.date === highlightDate.value
  }
}

function cellClass(d, ri, ci) {
  return [
    dayClass(d),
    {
      'jr-focus': focus.r === ri && focus.c === ci,
      'jr-open': pop.value?.r === ri && pop.value?.c === ci
    }
  ]
}

function dayTitle(d) {
  if (d.isSunday) return 'Yakshanba — dam olish kuni'
  if (d.isHoliday) return `Bayram: ${d.holidayName}`
  if (d.hasLesson) return `${dm(d.date)} — shu fandan dars bor`
  return dm(d.date)
}

function gradeTitle(g) {
  const parts = [`${g.value} · ${typeLabel[g.type] || ''}`]
  if (g.createdBy) parts.push(`Qo'ydi: ${g.createdBy}`)
  if (g.createdAt) {
    const t = new Date(g.createdAt)
    parts.push(
      `${String(t.getDate()).padStart(2, '0')}.${String(t.getMonth() + 1).padStart(2, '0')} ${String(t.getHours()).padStart(2, '0')}:${String(t.getMinutes()).padStart(2, '0')}`
    )
  }
  if (g.comment) parts.push(`Izoh: ${g.comment}`)
  return parts.join('\n')
}

/** Why a day cannot take a grade (null when it can). */
function blockedReason(d) {
  if (d.isSunday) return SUNDAY_TEXT
  if (d.isHoliday) return `🎉 Bu kun bayram — dam olish kuni: ${d.holidayName}`
  if (d.isFuture) return FUTURE_TEXT
  return null
}

function tellBlocked(d) {
  $q.notify({
    message: blockedReason(d),
    color: 'grey-9',
    textColor: 'white',
    timeout: 3000,
    position: 'top'
  })
}

// ------------------------------------------------------------------ scroll & focus
const scrollEl = ref(null)
const tableEl = ref(null)
const focus = reactive({ r: 0, c: 0 })

function scrollToDate(date) {
  const el = scrollEl.value
  if (!el || !journal.value) return
  const ci = journal.value.days.findIndex(d => d.date === date)
  if (ci < 0) return
  const th = el.querySelector(`th[data-col="${ci}"]`)
  if (!th) return
  const stickyLeft = 236
  const target =
    th.offsetLeft - stickyLeft - (el.clientWidth - stickyLeft - 140) / 2
  el.scrollLeft = Math.max(0, target)
  if (focus.c === 0 || !journal.value.days[focus.c]) focus.c = ci
}

function focusTodayFirst() {
  nextTick(() => {
    const ci = journal.value?.days.findIndex(d => d.date === now.value.date)
    if (ci >= 0 && journal.value.students.length) focusCell(0, ci)
  })
}

function focusCell(r, c) {
  focus.r = r
  focus.c = c
  nextTick(() =>
    tableEl.value
      ?.querySelector(`td[data-row="${r}"][data-col="${c}"]`)
      ?.focus({ preventScroll: false })
  )
}

// ------------------------------------------------------------------ popover & grading
const pop = ref(null)
const popEl = ref(null)

function onTableClick(e) {
  const th = e.target.closest('th[data-col]')
  if (th) {
    const d = journal.value.days[Number(th.dataset.col)]
    if (blockedReason(d)) tellBlocked(d)
    return
  }
  const td = e.target.closest('td[data-row]')
  if (!td) return
  const r = Number(td.dataset.row)
  const c = Number(td.dataset.col)
  focus.r = r
  focus.c = c
  const d = journal.value.days[c]
  const gradeEl = e.target.closest('[data-grade]')
  if (gradeEl && canGrade.value) {
    const g = cells.value[journal.value.students[r].id + '|' + d.date].find(
      x => String(x.id) === gradeEl.dataset.grade
    )
    openPop(r, c, td, g)
    return
  }
  if (blockedReason(d)) {
    tellBlocked(d)
    return
  }
  if (canGrade.value) openPop(r, c, td, null)
}

function openPop(r, c, td, grade) {
  const rect = td.getBoundingClientRect()
  const w = 240
  const h = 210
  let x = rect.left + rect.width / 2 - w / 2
  x = Math.min(Math.max(8, x), window.innerWidth - w - 8)
  let y = rect.bottom + 6
  if (y + h > window.innerHeight - 8) y = rect.top - h - 6
  pop.value = {
    r,
    c,
    x,
    y: Math.max(8, y),
    student: journal.value.students[r],
    day: journal.value.days[c],
    grade,
    type: grade?.type || 'CURRENT',
    comment: grade?.comment || ''
  }
}

function closePop() {
  const had = pop.value
  pop.value = null
  if (had) focusCell(had.r, had.c)
}

function onDocDown(e) {
  if (!pop.value) return
  if (popEl.value?.contains(e.target)) return
  if (e.target.closest?.('.q-menu, .q-notification')) return
  if (e.target.closest?.('td[data-row]')) return // the table click opens the next one
  pop.value = null
}

function choose(value) {
  const p = pop.value
  if (!p) return
  pop.value = null
  if (p.grade) updateGrade(p.grade, value, p.type, p.comment)
  else createGrade(p.student.id, p.day.date, value, p.type, p.comment)
  focusCell(p.r, p.c)
}

let tmpId = 0
async function createGrade(
  studentId,
  date,
  value,
  type = 'CURRENT',
  comment = ''
) {
  const temp = {
    id: `tmp-${++tmpId}`,
    studentId,
    date,
    value,
    type,
    comment: comment || null,
    createdBy: null,
    createdAt: null
  }
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
  } catch (e) {
    journal.value.grades = journal.value.grades.filter(g => g !== temp)
    notifyError(e)
  }
}

async function updateGrade(g, value, type, comment) {
  const before = { value: g.value, type: g.type, comment: g.comment }
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
  } catch (e) {
    Object.assign(g, before)
    notifyError(e)
  }
}

// deletes wait 5 s for "Bekor qilish"; leaving the page or the month sends them at once
const pendingDeletes = new Map()

function removeGrade(g) {
  pop.value = null
  const list = journal.value.grades
  const index = list.indexOf(g)
  if (index < 0) return
  list.splice(index, 1)
  if (String(g.id).startsWith('tmp-')) return
  const timer = setTimeout(() => sendDelete(g.id), 5000)
  pendingDeletes.set(g.id, timer)
  $q.notify({
    message: `${g.value} bahosi o'chirildi`,
    icon: 'delete_outline',
    color: 'grey-9',
    textColor: 'white',
    timeout: 5000,
    actions: [
      {
        label: 'Bekor qilish',
        color: 'amber',
        noCaps: true,
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

// ------------------------------------------------------------------ keyboard
function onKeydown(e) {
  if (!journal.value || pop.value) return
  const td = e.target.closest?.('td[data-row]')
  if (!td) return
  const rows = journal.value.students.length
  const cols = journal.value.days.length
  let { r, c } = { r: Number(td.dataset.row), c: Number(td.dataset.col) }
  const move = {
    ArrowUp: [-1, 0],
    ArrowDown: [1, 0],
    ArrowLeft: [0, -1],
    ArrowRight: [0, 1]
  }[e.key]
  if (move) {
    e.preventDefault()
    r = Math.min(rows - 1, Math.max(0, r + move[0]))
    c = Math.min(cols - 1, Math.max(0, c + move[1]))
    focusCell(r, c)
    return
  }
  const d = journal.value.days[c]
  const s = journal.value.students[r]
  if (['2', '3', '4', '5'].includes(e.key)) {
    e.preventDefault()
    if (!canGrade.value) return
    if (blockedReason(d)) return tellBlocked(d)
    createGrade(s.id, d.date, Number(e.key))
    return
  }
  if (e.key === 'Delete' || e.key === 'Backspace') {
    const list = cells.value[s.id + '|' + d.date]
    if (canGrade.value && list?.length) {
      e.preventDefault()
      removeGrade(list[list.length - 1])
    }
    return
  }
  if (e.key === 'Enter' || e.key === ' ') {
    e.preventDefault()
    td.click()
  }
}

// ------------------------------------------------------------------ bulk dialog
const bulk = reactive({
  open: false,
  date: '',
  type: 'CURRENT',
  values: {},
  saving: false
})
const bulkCount = computed(
  () => Object.values(bulk.values).filter(Boolean).length
)

function openBulk() {
  const t = new Date(now.value.date + 'T12:00:00')
  if (t.getDay() === 0) t.setDate(t.getDate() - 1)
  bulk.date = `${t.getFullYear()}-${String(t.getMonth() + 1).padStart(2, '0')}-${String(t.getDate()).padStart(2, '0')}`
  bulk.type = 'CURRENT'
  bulk.values = {}
  bulk.open = true
}

async function saveBulk() {
  bulk.saving = true
  const entries = Object.entries(bulk.values).filter(([, v]) => v)
  const results = await Promise.allSettled(
    entries.map(([studentId, score]) =>
      api.post(
        '/api/grades',
        {
          studentId: Number(studentId),
          subjectId: subjectId.value,
          gradeDate: bulk.date,
          score,
          type: bulk.type
        },
        { background: true }
      )
    )
  )
  bulk.saving = false
  const failed = results.filter(r => r.status === 'rejected')
  if (failed.length) notifyError(failed[0].reason)
  const ok = results.length - failed.length
  if (ok) {
    $q.notify({ type: 'positive', message: `${ok} ta baho saqlandi` })
    bulk.open = false
    highlightDate.value = bulk.date
    if (bulk.date.slice(0, 7) !== month.value) {
      month.value = bulk.date.slice(0, 7)
      syncUrl()
    }
    loadJournal()
  }
}

// ------------------------------------------------------------------ teacher's current lesson
async function autoPickLesson() {
  try {
    const res = await api.get('/api/me/current-lesson', { background: true })
    if (res.status !== 200 || !res.data?.classId) return
    // the user picked something meanwhile — their choice wins
    if (route.query.class || route.query.subject) return
    autoLesson.value = res.data
    classId.value = res.data.classId
    subjectId.value = res.data.subjectId
    month.value = now.value.month
    highlightDate.value = now.value.date
    syncUrl()
    await loadOverview()
    await loadJournal()
    pulse()
    focusTodayFirst()
  } catch {
    // no lesson info — the page simply waits for a choice
  }
}

function notifyError(e) {
  $q.notify({
    type: 'negative',
    message: e?.friendlyMessage || e?.response?.data || 'Xatolik yuz berdi'
  })
}

// ------------------------------------------------------------------ wiring
watch(
  () => schoolStore.activeSchoolId,
  async (id, old) => {
    if (!id) return
    if (old && old !== id) {
      classId.value = null
      subjectId.value = null
      overview.value = null
      overviewError.value = false
      fallbackSubjects.value = []
      journal.value = null
    }
    await loadClasses().catch(notifyError)
    if (classId.value) {
      await loadOverview()
      await loadJournal()
    } else if (!route.query.class && !route.query.subject) {
      autoPickLesson()
    }
  },
  { immediate: true }
)

onMounted(() => document.addEventListener('mousedown', onDocDown, true))
onBeforeUnmount(() => {
  clearInterval(ticker)
  clearTimeout(pulseTimer)
  document.removeEventListener('mousedown', onDocDown, true)
  flushDeletes()
})
</script>

<style scoped>
.jr-panel-error {
  color: var(--text-primary);
  border-color: color-mix(
    in srgb,
    var(--brand-border) 50%,
    var(--color-danger)
  );
}

.jr-label {
  width: 40px;
  flex: none;
  font-weight: 600;
  color: var(--brand-text-muted);
}

.jr-teacher {
  color: var(--brand-text-muted);
  font-size: var(--text-sm, 13px);
}

.jr-month-title {
  min-width: 128px;
  text-align: center;
  font-weight: 700;
  color: var(--text-primary);
}

.jr-auto {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(16, 185, 129, 0.12);
  color: var(--text-primary);
  font-size: var(--text-sm, 13px);
}

.jr-muted {
  color: var(--brand-text-muted);
}

.jr-empty-art {
  font-size: 56px;
  line-height: 1;
}

.jr-chip {
  background: var(--surface-2);
  color: var(--text-primary);
  border: 1px solid var(--brand-border);
}

.jr-hint {
  font-size: var(--text-xs, 12px);
  color: var(--brand-text-muted);
}

/* ---------------------------------------------------------------- table */
.jr-scroll {
  overflow: auto;
  max-height: calc(100vh - 230px);
  min-height: 260px;
  padding: 0;
  transition: opacity 0.15s;
}

.jr-stale {
  opacity: 0.55;
  pointer-events: none;
}

.jr-table {
  border-collapse: separate;
  border-spacing: 0;
  font-size: 13px;
  width: max-content;
  min-width: 100%;
}

.jr-table th,
.jr-table td {
  border-right: 1px solid var(--brand-border);
  border-bottom: 1px solid var(--brand-border);
  padding: 0;
  height: 34px;
  background: var(--card-bg);
}

.jr-table thead th {
  position: sticky;
  top: 0;
  z-index: 3;
  height: 52px;
  font-weight: 600;
  color: var(--brand-text-muted);
  background: var(--card-bg);
}

.jr-table tfoot td {
  position: sticky;
  bottom: 0;
  z-index: 2;
  background: var(--surface-2);
  font-weight: 600;
  border-top: 1px solid var(--brand-border);
}

.jr-no {
  width: 36px;
  min-width: 36px;
  text-align: center;
  color: var(--brand-text-muted);
}

.jr-name {
  width: 200px;
  min-width: 200px;
  max-width: 200px;
  text-align: left;
  padding: 0 10px !important;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: var(--text-primary);
  font-weight: 500;
}

thead .jr-name {
  font-weight: 600;
  color: var(--brand-text-muted);
}

.jr-sticky-l {
  position: sticky;
  left: 0;
  z-index: 2;
}

.jr-sticky-l2 {
  position: sticky;
  left: 36px;
  z-index: 2;
  box-shadow: 6px 0 8px -8px rgba(15, 23, 42, 0.35);
}

.jr-sticky-r {
  position: sticky;
  right: 52px;
  z-index: 2;
  box-shadow: -6px 0 8px -8px rgba(15, 23, 42, 0.35);
}

.jr-table thead .jr-sticky-l,
.jr-table thead .jr-sticky-l2,
.jr-table thead .jr-sticky-r,
.jr-table tfoot .jr-sticky-l,
.jr-table tfoot .jr-sticky-l2,
.jr-table tfoot .jr-sticky-r {
  z-index: 4;
}

.jr-count {
  position: sticky;
  right: 0;
  z-index: 2;
  width: 52px;
  min-width: 52px;
  text-align: center;
  color: var(--brand-text-muted);
}

.jr-table thead .jr-count,
.jr-table tfoot .jr-count {
  z-index: 4;
}

.jr-avg {
  width: 64px;
  min-width: 64px;
  text-align: center;
  font-weight: 700;
}

.jr-day,
.jr-cell,
.jr-foot {
  width: 44px;
  min-width: 44px;
  max-width: 44px;
  text-align: center;
}

.jr-day {
  cursor: default;
  line-height: 1.15;
}

.jr-day-date {
  color: var(--text-primary);
  font-weight: 600;
  font-size: 12px;
}

.jr-day-wd {
  font-size: 11px;
  font-weight: 500;
}

.jr-dot {
  color: var(--color-brand);
  font-weight: 900;
  margin-left: 1px;
}

.jr-today-tag {
  display: inline-block;
  margin-top: 1px;
  padding: 0 4px;
  border-radius: 4px;
  font-size: 9px;
  font-weight: 700;
  text-transform: uppercase;
  background: var(--color-brand);
  color: #fff;
}

/* zebra + whole-row hover (keeps the student's name in view) */
.jr-table tbody tr:nth-child(even) td {
  background: color-mix(in srgb, var(--card-bg) 97%, var(--text-primary));
}

.jr-table tbody tr:hover td {
  background: color-mix(in srgb, var(--card-bg) 90%, var(--color-brand));
}

.jr-cell {
  cursor: pointer;
  outline: none;
  position: relative;
}

.jr-readonly .jr-cell {
  cursor: default;
}

.jr-plus {
  color: var(--brand-text-muted);
  opacity: 0.45;
  font-size: 15px;
  font-weight: 600;
  transition:
    opacity 0.12s,
    color 0.12s;
}

.jr-cell:hover .jr-plus,
.jr-focus .jr-plus {
  opacity: 1;
  color: var(--color-brand);
}

.jr-table tbody td.jr-cell:hover {
  background: color-mix(in srgb, var(--card-bg) 80%, var(--color-brand));
}

.jr-focus,
.jr-open {
  box-shadow: inset 0 0 0 2px var(--color-brand);
}

.jr-grade {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 22px;
  height: 22px;
  padding: 0 4px;
  border-radius: 6px;
  font-weight: 700;
  font-size: 13px;
  color: #fff;
  margin: 0 1px;
  cursor: pointer;
}

.jr-cell {
  white-space: nowrap;
}

/* two grades on one day: both a little smaller so they fit side by side */
.jr-grade:has(+ .jr-grade),
.jr-grade + .jr-grade {
  min-width: 16px;
  height: 18px;
  padding: 0 2px;
  font-size: 11px;
  margin: 0;
}

.jr-more {
  font-size: 10px;
  font-weight: 700;
  color: var(--brand-text-muted);
  margin-left: 1px;
}

.jr-grade--5 {
  background: var(--color-success);
}

.jr-grade--4 {
  background: var(--color-info);
}

.jr-grade--3 {
  background: var(--color-warning);
  color: #1f2937;
}

.jr-grade--2 {
  background: var(--color-danger);
}

/* today, Sunday, holiday, future */
.jr-table .jr-today {
  background: color-mix(
    in srgb,
    var(--card-bg) 88%,
    var(--color-brand)
  ) !important;
}

.jr-table thead th.jr-today {
  color: var(--color-brand);
}

.jr-table .jr-sunday,
.jr-table .jr-holiday {
  background: repeating-linear-gradient(
    -45deg,
    var(--surface-2),
    var(--surface-2) 4px,
    color-mix(in srgb, var(--surface-2) 70%, var(--brand-text-muted)) 4px,
    color-mix(in srgb, var(--surface-2) 70%, var(--brand-text-muted)) 5px
  ) !important;
  cursor: not-allowed;
}

.jr-table .jr-future {
  opacity: 0.5;
  cursor: not-allowed;
}

.jr-table .jr-pulse {
  animation: jr-pulse 0.66s ease-in-out 3;
}

@keyframes jr-pulse {
  50% {
    background: color-mix(in srgb, var(--card-bg) 55%, var(--color-brand));
  }
}

.jr-foot-label {
  color: var(--brand-text-muted) !important;
  font-weight: 600 !important;
}

.jr-avg-5 {
  color: var(--color-success);
}

.jr-avg-4 {
  color: var(--color-info);
}

.jr-avg-3 {
  color: #d97706;
}

.jr-avg-2 {
  color: var(--color-danger);
}

.jr-avg-none {
  color: var(--brand-text-muted);
}

/* ---------------------------------------------------------------- popover */
.jr-pop {
  position: fixed;
  z-index: 6000;
  width: 240px;
  padding: 12px;
  border-radius: 12px;
  background: var(--card-bg);
  color: var(--text-primary);
  border: 1px solid var(--brand-border);
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.22);
}

.jr-pop-head {
  font-size: 13px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.jr-pop-btn {
  flex: 1;
  height: 42px;
  border: none;
  border-radius: 10px;
  font: inherit;
  font-size: 18px;
  font-weight: 800;
  color: #fff;
  cursor: pointer;
  opacity: 0.85;
  transition:
    transform 0.08s,
    opacity 0.08s;
}

.jr-pop-btn:hover {
  opacity: 1;
  transform: translateY(-1px);
}

.jr-pop-btn--on {
  opacity: 1;
  box-shadow:
    0 0 0 3px var(--card-bg),
    0 0 0 5px var(--text-primary);
}

.jr-pop-btn--sm {
  flex: none;
  width: 36px;
  height: 32px;
  font-size: 15px;
  margin-left: 4px;
  opacity: 0.35;
}

.jr-pop-btn--sm.jr-pop-btn--on {
  opacity: 1;
}

.jr-bulk {
  width: 100%;
  max-width: 560px;
  border-radius: var(--radius-lg);
}

.jr-bulk-list {
  max-height: 55vh;
}

.jr-bulk-row:nth-child(even) {
  background: var(--surface-2);
}

.jr-bulk-no {
  width: 28px;
  color: var(--brand-text-muted);
}

@media (max-width: 767px) {
  .jr-name {
    width: 130px;
    min-width: 130px;
    max-width: 130px;
  }

  /* on a phone only the names stay put; average and count scroll with the days */
  .jr-table .jr-sticky-r,
  .jr-table .jr-count {
    position: static;
    box-shadow: none;
  }

  .jr-scroll {
    max-height: none;
  }
}
</style>

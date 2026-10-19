<template>
  <page-layout
    icon="fact_check"
    color="#ef4444"
    title="Davomat olish"
    :subtitle="subtitle"
  >
    <template v-slot:actions>
      <AppButton
        variant="secondary"
        icon="calendar_month"
        label="Oylik kalendar"
        @click="$router.push('/attendance/calendar')"
      />
    </template>

    <div class="att">
      <!-- date + class -->
      <section class="att__bar dash-card" data-att-controls>
        <div class="att__date" role="group" aria-label="Sana">
          <AppButton
            variant="ghost"
            size="sm"
            icon="chevron_left"
            aria-label="Oldingi o'quv kuni"
            data-date-prev
            @click="step(-1)"
          />
          <button type="button" class="att__date-label" data-date-label>
            <q-icon name="event" />
            {{ shortDayLabel(date) }}
            <q-popup-proxy
              cover
              transition-show="scale"
              transition-hide="scale"
            >
              <q-date
                :model-value="date"
                mask="YYYY-MM-DD"
                :options="d => allowedDay(d)"
                first-day-of-week="1"
                @update:model-value="v => v && setDate(v)"
              />
            </q-popup-proxy>
          </button>
          <AppButton
            variant="ghost"
            size="sm"
            icon="chevron_right"
            aria-label="Keyingi o'quv kuni"
            :disable="date >= openingDay(now.date)"
            data-date-next
            @click="step(1)"
          />
          <AppButton
            variant="secondary"
            size="sm"
            label="Bugun"
            :disable="date === openingDay(now.date)"
            data-date-today
            @click="setDate(openingDay(now.date))"
          />
        </div>
        <ClassPicker
          v-model="classId"
          compact
          class="att__classes"
          :classes="classes"
          :states="day?.classStates || {}"
        />
      </section>

      <!-- the day's lessons -->
      <section v-if="loadingDay && !day" class="dash-card att__card">
        <q-skeleton type="rect" class="att__skel-timeline" />
      </section>
      <section
        v-else-if="day && day.lessons.length && !noStudents"
        class="dash-card att__card att__timeline"
      >
        <LessonTimeline
          :model-value="lessonId"
          :lessons="day.lessons"
          :is-today="date === now.date"
          :now-minutes="now.minutes"
          @update:model-value="pickLesson"
        />
      </section>

      <!-- empty states -->
      <EmptyState
        v-if="day && day.dayOff"
        icon="bedtime"
        title="😴 Dam olish kuni"
        :text="`${day.weekday} — darslar yo'q. Oldingi o'quv kunini ochish mumkin.`"
        action="Oldingi o'quv kuni"
        action-icon="chevron_left"
        data-empty="dayoff"
        @action="step(-1)"
      />
      <EmptyState
        v-else-if="noStudents"
        icon="group_off"
        title="Sinfda o'quvchi yo'q"
        text="Avval sinfga o'quvchilar qo'shing — keyin davomat olinadi."
        data-empty="students"
      />
      <EmptyState
        v-else-if="day && !loadingDay && !day.lessons.length"
        icon="event_busy"
        title="Bu kuni dars yo'q"
        :text="`${selectedClassName} sinfining ${day.weekday.toLowerCase()} kungi jadvalida dars yo'q.`"
        data-empty="lessons"
      />

      <template v-else-if="lessonId">
        <div
          v-if="loadingRoster && !roster"
          class="dash-card att__card att__skel"
        >
          <q-skeleton type="rect" class="att__skel-summary" />
          <q-skeleton
            v-for="i in 8"
            :key="i"
            type="rect"
            class="att__skel-row"
          />
        </div>

        <template v-else-if="roster">
          <AttendanceSummary
            v-model:query="query"
            :c="c"
            :title="lessonTitle"
            :subtitle="lessonSubtitle"
            :demo="!!(day?.demo || roster.demo)"
            :notify="notifyCount"
            :auto="autoCount"
            :can-edit="canEdit"
            :confirming="confirming"
            @all-present="allPresent"
            @confirm-auto="confirmAutomatic"
          />

          <section class="dash-card att__roster" data-roster>
            <div class="att__roster-head">
              <SegTabs
                v-model="view"
                label="Ko'rinish"
                :options="[
                  { label: 'Ro\'yxat', value: 'list', icon: 'view_list' },
                  { label: 'Kartochkalar', value: 'cards', icon: 'grid_view' }
                ]"
              />
              <span v-if="selection.size > 1" class="att__sel" data-selection>
                {{ selection.size }} ta tanlandi — 1–4 bilan belgilang
                <AppButton
                  variant="ghost"
                  size="sm"
                  icon="close"
                  aria-label="Tanlovni bekor qilish"
                  @click="clearSelection"
                />
              </span>
              <span v-else-if="canEdit" class="att__keys gt-sm">
                <kbd>↑</kbd><kbd>↓</kbd> qator · <kbd>1</kbd>–<kbd>4</kbd> holat
                · <kbd>Enter</kbd> keyingi · <kbd>Shift</kbd>+bosish oraliq
              </span>
            </div>

            <div v-if="!shown.length" class="att__nothing"
              >"{{ query }}" bo'yicha o'quvchi topilmadi</div
            >

            <div
              v-else-if="view === 'list'"
              ref="listEl"
              class="att__list"
              role="grid"
              :aria-label="`${lessonTitle}: o'quvchilar`"
              tabindex="0"
              data-roster-list
              @keydown="onKeydown"
            >
              <RosterRow
                v-for="(r, i) in shown"
                :key="r.studentId"
                :row="r"
                :n="i + 1"
                :active="i === active"
                :selected="selection.has(r.studentId)"
                :readonly="!canEdit"
                :dirty="!!dirty[r.studentId]"
                @focus-row="e => onRowClick(i, e)"
                @pick="(v, el) => pick(r, v, el)"
              />
            </div>

            <RosterCards
              v-else
              :rows="shown"
              :readonly="!canEdit"
              @pick="(r, v, el) => pick(r, v, el)"
            />
          </section>

          <!-- save state, always in sight -->
          <div
            v-if="canEdit"
            class="att__savebar"
            :class="`att__savebar--${saveState}`"
            data-savebar
            role="status"
          >
            <template v-if="saveState === 'saving'">
              <q-spinner size="1em" /> Saqlanmoqda…
            </template>
            <template v-else-if="saveState === 'error'">
              <q-icon name="error" /> Xato — saqlanmadi
              <AppButton
                size="sm"
                variant="danger"
                label="Qayta urinish"
                icon="refresh"
                data-retry
                @click="flush"
              />
            </template>
            <template v-else-if="pendingCount">
              <q-icon name="edit" /> {{ pendingCount }} ta o'zgarish — avtomatik
              saqlanadi
              <AppButton
                size="sm"
                label="Hozir saqlash"
                icon="save"
                data-save-now
                @click="flush"
              />
            </template>
            <template v-else-if="savedAt">
              <q-icon name="check_circle" /> Saqlandi ✓ {{ savedAt }}
            </template>
            <template v-else>
              <q-icon name="cloud_done" /> O'zgarishlar avtomatik saqlanadi
            </template>
          </div>
        </template>
      </template>
    </div>

    <MarkPopover
      :key="`${pop.row?.studentId}|${pop.kind}`"
      v-model="pop.open"
      :target="pop.el"
      :kind="pop.kind"
      :late-minutes="pop.row?.lateMinutes ?? null"
      :comment="pop.row?.comment || ''"
      @apply="applyPopover"
    />
  </page-layout>
</template>

<script setup>
// Taking attendance: a day (◀ ▶, no Sundays, nothing after today) and a class (the picker's dots
// say where today's attendance is taken), the class's lessons of THAT weekday on a line, the
// chosen lesson's summary and its students — as a list (keyboard, Shift ranges) or as cards
// (tap to cycle). Every change is saved by itself 1.5 s later; only the changed rows are sent,
// so automatic marks nobody touched stay automatic and nothing manual is overwritten unasked.
import {
  ref,
  reactive,
  computed,
  watch,
  onBeforeUnmount,
  nextTick,
  markRaw
} from 'vue'
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'
import { useQuasar } from 'quasar'
import { api } from '@/boot/axios'
import PageLayout from '@/components/PageLayout.vue'
import AppButton from '@/components/app/AppButton.vue'
import ClassPicker from '@/components/ClassPicker.vue'
import EmptyState from '@/components/pages/EmptyState.vue'
import SegTabs from '@/components/dashboard/SegTabs.vue'
import LessonTimeline from '@/components/attendance/LessonTimeline.vue'
import AttendanceSummary from '@/components/attendance/AttendanceSummary.vue'
import RosterRow from '@/components/attendance/RosterRow.vue'
import RosterCards from '@/components/attendance/RosterCards.vue'
import MarkPopover from '@/components/attendance/MarkPopover.vue'
import {
  BY_KEY,
  counts,
  dayLabel,
  shortDayLabel,
  stepSchoolDay,
  openingDay
} from '@/components/attendance/attendance'
import { hm, minutes, roomLabel } from '@/components/timetable/timetable'
import { confirmUndoToast } from '@/composables/useUndoToast'
import { useSchoolStore } from '@/stores/school'
import { useAuthStore } from '@/stores/auth'
import { tashkentNow } from '@/utils/tashkent'

const AUTOSAVE_MS = 1500
const LAST_CLASS = 'attendance.classId'

const $q = useQuasar()
const route = useRoute()
const router = useRouter()
const schoolStore = useSchoolStore()
const authStore = useAuthStore()
const canEdit = computed(() => authStore.isEditor)

const now = ref(tashkentNow())
const ticker = setInterval(() => (now.value = tashkentNow()), 30000)

const date = ref(openingDay(now.value.date))
const classes = ref([])
const classId = ref(null)
const day = ref(null)
const lessonId = ref(null)
const roster = ref(null)
const loadingDay = ref(false)
const loadingRoster = ref(false)
const confirming = ref(false)
const query = ref('')
const view = ref($q.screen.lt.sm ? 'cards' : 'list')

// ---------------------------------------------------------------- header

const selectedClass = computed(() =>
  classes.value.find(c => c.id === classId.value)
)
const selectedClassName = computed(() =>
  selectedClass.value
    ? `${selectedClass.value.gradeNumber}-${selectedClass.value.sectionLetter}`
    : ''
)
const noStudents = computed(() => selectedClass.value?.studentCount === 0)
const subtitle = computed(() =>
  [schoolStore.activeSchoolName, selectedClassName.value, dayLabel(date.value)]
    .filter(Boolean)
    .join(' · ')
)
const lesson = computed(
  () => day.value?.lessons.find(l => l.lessonSlotId === lessonId.value) || null
)
const lessonTitle = computed(() =>
  lesson.value ? `${lesson.value.n}-dars · ${lesson.value.subject}` : ''
)
const lessonSubtitle = computed(() => {
  const l = lesson.value
  if (!l) return ''
  return [`${hm(l.start)}–${hm(l.end)}`, l.teacher, l.room && roomLabel(l.room)]
    .filter(Boolean)
    .join(' · ')
})

// ---------------------------------------------------------------- date

function allowedDay(d) {
  const iso = d.replaceAll('/', '-')
  return iso <= now.value.date && new Date(`${iso}T12:00:00`).getDay() !== 0
}

function step(delta) {
  setDate(stepSchoolDay(date.value, delta, now.value.date))
}

async function setDate(d) {
  if (d === date.value) return
  await flush()
  date.value = d
  lessonId.value = null
  await loadDay()
}

// ---------------------------------------------------------------- loading

async function loadClasses() {
  if (!schoolStore.activeSchoolId) return
  try {
    const res = await api.get('/api/school-classes', {
      params: {
        schoolId: schoolStore.activeSchoolId,
        size: 100,
        sort: 'gradeNumber,asc'
      }
    })
    classes.value = res.data.content || []
  } catch {
    classes.value = []
  }
}

/** The class's lessons on the chosen date (its weekday only) and every class's state for the picker. */
async function loadDay({ keepLesson = false, silent = false } = {}) {
  if (!classId.value) return
  if (!silent) loadingDay.value = true
  const forClass = classId.value
  const forDate = date.value
  try {
    const res = await api.get('/api/pages/attendance/day', {
      params: {
        schoolId: schoolStore.activeSchoolId,
        classId: forClass,
        date: forDate
      },
      background: silent
    })
    if (forClass !== classId.value || forDate !== date.value) return
    day.value = res.data
    const ids = res.data.lessons.map(l => l.lessonSlotId)
    if (!keepLesson || !ids.includes(lessonId.value)) {
      const next = ids.includes(lessonId.value)
        ? lessonId.value
        : autoLesson(res.data.lessons)
      if (next !== lessonId.value || !roster.value) {
        lessonId.value = next
        await loadRoster()
      }
    }
  } catch (e) {
    if (!silent) {
      day.value = null
      $q.notify({
        type: 'negative',
        message: e.friendlyMessage || "Darslarni yuklab bo'lmadi"
      })
    }
  } finally {
    loadingDay.value = false
  }
}

/** Today: the lesson going on now or the last one that started; another day: the first not taken. */
function autoLesson(lessons) {
  if (!lessons.length) return null
  if (date.value === now.value.date) {
    const started = lessons.filter(l => minutes(l.start) <= now.value.minutes)
    return (started.length ? started[started.length - 1] : lessons[0])
      .lessonSlotId
  }
  return (
    lessons.find(l => l.state === 'NONE' || l.state === 'PARTIAL') || lessons[0]
  ).lessonSlotId
}

async function loadRoster() {
  roster.value = null
  clearSelection()
  active.value = 0
  if (!lessonId.value) return
  loadingRoster.value = true
  const forLesson = lessonId.value
  try {
    const res = await api.get('/api/pages/attendance/roster', {
      params: { lessonSlotId: forLesson, date: date.value }
    })
    if (forLesson === lessonId.value) roster.value = res.data
  } catch (e) {
    $q.notify({
      type: 'negative',
      message: e.friendlyMessage || "Ro'yxatni yuklab bo'lmadi"
    })
  } finally {
    loadingRoster.value = false
  }
}

async function pickLesson(id) {
  if (id === lessonId.value) return
  await flush()
  lessonId.value = id
  syncUrl()
  await loadRoster()
}

watch(classId, async (id, old) => {
  if (!id || id === old) return
  if (old) await flush()
  try {
    localStorage.setItem(LAST_CLASS, String(id))
  } catch {
    // private window: the class is not remembered
  }
  if (old) lessonId.value = null
  roster.value = null
  await loadDay()
})

watch([lessonId, date], () => syncUrl())

function syncUrl() {
  if (!lessonId.value) return
  const q = { lessonSlotId: String(lessonId.value), date: date.value }
  if (
    route.query.lessonSlotId !== q.lessonSlotId ||
    route.query.date !== q.date
  )
    router.replace({ query: { ...route.query, ...q } })
}

// ---------------------------------------------------------------- the list

const rows = computed(() => roster.value?.rows || [])
const shown = computed(() => {
  const q = query.value.trim().toLowerCase()
  return q
    ? rows.value.filter(r => r.name.toLowerCase().includes(q))
    : rows.value
})
const c = computed(() => counts(rows.value.map(r => r.status)))
const isAuto = r => r.source === 'AUTO_CARRY' || r.source === 'DEMO_SIM'
const autoCount = computed(
  () => rows.value.filter(r => isAuto(r) && !dirty[r.studentId]).length
)
// parents hear about the absent and late ones once their row is saved
const notifyCount = computed(
  () =>
    rows.value.filter(
      r =>
        dirty[r.studentId] &&
        r.telegram &&
        (r.status === 'ABSENT' || r.status === 'LATE')
    ).length
)

const listEl = ref(null)
const active = ref(0)
const selection = reactive(new Set())
let anchor = 0

function clearSelection() {
  selection.clear()
}

function onRowClick(i, e) {
  if (e.target.closest('[data-status-segment]')) return
  if (e.shiftKey) {
    const [a, b] = [Math.min(anchor, i), Math.max(anchor, i)]
    selection.clear()
    for (let k = a; k <= b; k++) selection.add(shown.value[k].studentId)
  } else if (e.ctrlKey || e.metaKey) {
    const id = shown.value[i].studentId
    if (selection.has(id)) selection.delete(id)
    else selection.add(id)
    anchor = i
  } else {
    selection.clear()
    anchor = i
  }
  active.value = i
  listEl.value?.focus({ preventScroll: true })
}

function moveTo(i, extend = false) {
  const max = shown.value.length - 1
  const next = Math.max(0, Math.min(max, i))
  if (extend) {
    selection.add(shown.value[active.value].studentId)
    selection.add(shown.value[next].studentId)
  }
  active.value = next
  nextTick(() =>
    listEl.value
      ?.querySelector(`[data-roster-row="${shown.value[next]?.studentId}"]`)
      ?.scrollIntoView({ block: 'nearest' })
  )
}

function onKeydown(e) {
  if (e.target !== listEl.value) return
  if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
    e.preventDefault()
    moveTo(active.value + (e.key === 'ArrowDown' ? 1 : -1), e.shiftKey)
  } else if (e.key === 'Enter') {
    e.preventDefault()
    selection.clear()
    moveTo(active.value + 1)
  } else if (e.key === 'Escape') {
    selection.clear()
  } else if (
    BY_KEY[e.key] &&
    canEdit.value &&
    !e.ctrlKey &&
    !e.metaKey &&
    !e.altKey
  ) {
    e.preventDefault()
    const r = shown.value[active.value]
    if (r)
      setStatus(
        selection.size && selection.has(r.studentId)
          ? [...selection]
          : [r.studentId],
        BY_KEY[e.key]
      )
  }
}

// ---------------------------------------------------------------- marking

const dirty = reactive({})
const pendingCount = computed(() => Object.keys(dirty).length)

/** The same status for these students; Kechikdi / Sababli keep their minutes / reason. */
function setStatus(ids, status) {
  for (const id of ids) {
    const r = rows.value.find(x => x.studentId === id)
    if (!r || r.status === status) continue
    r.status = status
    if (status !== 'LATE') r.lateMinutes = null
    if (status !== 'EXCUSED') r.comment = null
    touch(r)
  }
}

/** A click on a status: several selected rows take it together; Kechikdi / Sababli ask for details. */
function pick(r, status, el) {
  if (!canEdit.value) return
  const ids =
    selection.size > 1 && selection.has(r.studentId)
      ? [...selection]
      : [r.studentId]
  setStatus(ids, status)
  if (ids.length === 1 && (status === 'LATE' || status === 'EXCUSED')) {
    pop.row = r
    pop.kind = status
    pop.el = markRaw(el)
    pop.open = true
  }
}

const pop = reactive({ open: false, el: null, kind: 'LATE', row: null })

function applyPopover(patch) {
  if (!pop.row) return
  Object.assign(pop.row, patch)
  touch(pop.row)
}

function allPresent() {
  const before = rows.value.map(r => ({
    r,
    status: r.status,
    lateMinutes: r.lateMinutes,
    comment: r.comment
  }))
  const changed = before.filter(b => b.status !== 'PRESENT')
  if (!changed.length) return
  setStatus(
    changed.map(b => b.r.studentId),
    'PRESENT'
  )
  confirmUndoToast(
    `${changed.length} ta o'quvchi "Keldi" deb belgilandi`,
    () => {
      for (const b of changed) {
        Object.assign(b.r, {
          status: b.status,
          lateMinutes: b.lateMinutes,
          comment: b.comment
        })
        touch(b.r)
      }
    }
  )
}

// ---------------------------------------------------------------- autosave

const saveState = ref('idle')
const savedAt = ref('')
let timer = null
let inFlight = null

function touch(r) {
  dirty[r.studentId] = (dirty[r.studentId] || 0) + 1
  if (saveState.value === 'saved') saveState.value = 'idle'
  clearTimeout(timer)
  timer = setTimeout(flush, AUTOSAVE_MS)
}

/** Sends the changed rows now; true when nothing is left unsaved. */
async function flush() {
  clearTimeout(timer)
  if (inFlight) await inFlight
  const ids = Object.keys(dirty).map(Number)
  if (!ids.length || !roster.value) return saveState.value !== 'error'
  const sent = Object.fromEntries(ids.map(id => [id, dirty[id]]))
  const byId = new Map(rows.value.map(r => [r.studentId, r]))
  const entries = ids
    .map(id => byId.get(id))
    .filter(r => r?.status)
    .map(r => ({
      studentId: r.studentId,
      status: r.status,
      comment: r.comment || null,
      lateMinutes: r.lateMinutes ?? null
    }))
  saveState.value = 'saving'
  inFlight = api
    .post(
      '/api/pages/attendance/save',
      {
        lessonSlotId: roster.value.lessonSlotId,
        date: roster.value.date,
        entries
      },
      {
        params: { schoolId: schoolStore.activeSchoolId },
        background: true
      }
    )
    .then(res => {
      for (const id of ids) {
        // a row changed again while this was on its way stays dirty for the next save
        if (dirty[id] === sent[id]) delete dirty[id]
        const r = byId.get(id)
        if (r) r.source = 'MANUAL'
      }
      const at = res.data?.savedAt ? new Date(res.data.savedAt) : new Date()
      savedAt.value = at.toLocaleTimeString('uz-UZ', {
        hour: '2-digit',
        minute: '2-digit',
        timeZone: 'Asia/Tashkent'
      })
      saveState.value = 'saved'
      loadDay({ keepLesson: true, silent: true })
      return true
    })
    .catch(e => {
      saveState.value = 'error'
      $q.notify({
        type: 'negative',
        message: e.friendlyMessage || 'Davomat saqlanmadi',
        icon: 'error'
      })
      return false
    })
  const ok = await inFlight
  inFlight = null
  return ok && !pendingCount.value
}

/** "Avtomatikni tasdiqlash": this lesson's automatic marks become the user's own. */
async function confirmAutomatic() {
  await flush()
  confirming.value = true
  try {
    const res = await api.post('/api/attendance/confirm', null, {
      params: { lessonSlotId: lessonId.value, date: date.value }
    })
    $q.notify({
      type: 'positive',
      message: `${res.data.confirmed} ta belgi tasdiqlandi`,
      icon: 'check_circle'
    })
    await loadRoster()
    loadDay({ keepLesson: true, silent: true })
  } catch (e) {
    $q.notify({
      type: 'negative',
      message: e.friendlyMessage || 'Xatolik yuz berdi'
    })
  } finally {
    confirming.value = false
  }
}

// ---------------------------------------------------------------- leaving with unsaved marks

function onBeforeUnload(e) {
  if (!pendingCount.value && saveState.value !== 'saving') return
  flush()
  e.preventDefault()
  e.returnValue = ''
}
window.addEventListener('beforeunload', onBeforeUnload)

onBeforeRouteLeave(async () => {
  if (!pendingCount.value) return true
  if (await flush()) return true
  return new Promise(resolve =>
    $q
      .dialog({
        title: 'Davomat saqlanmadi',
        message: "O'zgarishlar saqlanmadi. Baribir chiqasizmi?",
        cancel: { label: 'Qolish', flat: true, noCaps: true },
        ok: { label: 'Chiqish', color: 'negative', noCaps: true },
        persistent: true
      })
      .onOk(() => resolve(true))
      .onCancel(() => resolve(false))
  )
})

onBeforeUnmount(() => {
  clearInterval(ticker)
  window.removeEventListener('beforeunload', onBeforeUnload)
  if (pendingCount.value) flush()
})

// ---------------------------------------------------------------- opening

/**
 * Which class opens: a link (/attendance?lessonSlotId=12&date=…), else the teacher's lesson going
 * on now, else the class chosen last time, else the first class with students.
 */
async function start() {
  await loadClasses()
  const usable = c => c.studentCount == null || c.studentCount > 0
  const fromLink = Number(route.query.lessonSlotId)
  if (
    /^\d{4}-\d{2}-\d{2}$/.test(route.query.date || '') &&
    route.query.date <= now.value.date
  )
    date.value = route.query.date
  if (fromLink) {
    try {
      const res = await api.get(`/api/lesson-slots/${fromLink}`)
      if (res.data?.schoolClassId) {
        lessonId.value = fromLink
        classId.value = res.data.schoolClassId
        return
      }
    } catch {
      // the lesson is gone or belongs to another school: open as usual
    }
  }
  if (authStore.employeeId && date.value === now.value.date) {
    try {
      const res = await api.get('/api/attendance/today-lessons', {
        params: {
          schoolId: schoolStore.activeSchoolId,
          employeeId: authStore.employeeId
        },
        background: true
      })
      const mine = (res.data || []).find(l => l.status === 'HOZIR')
      if (mine) {
        classId.value = mine.schoolClassId
        return
      }
    } catch {
      // no lessons of one's own: fall through
    }
  }
  let last = null
  try {
    last = Number(localStorage.getItem(LAST_CLASS))
  } catch {
    // private window
  }
  const remembered = classes.value.find(c => c.id === last && usable(c))
  classId.value =
    (remembered || classes.value.find(usable) || classes.value[0])?.id ?? null
}

watch(
  () => schoolStore.activeSchoolId,
  async () => {
    await flush()
    classId.value = null
    day.value = null
    roster.value = null
    lessonId.value = null
    start()
  }
)

start()
</script>

<style scoped>
.att {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  padding-bottom: var(--space-8);
}

.att__card {
  padding: var(--space-3) var(--space-4);
}

.att__card:hover,
.att__bar:hover,
.att__roster:hover {
  transform: none;
}

.att__bar {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-3) var(--space-4);
}

.att__date {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  flex: none;
}

.att__date-label {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  height: var(--control-h-sm);
  min-width: var(--field-w-sm);
  justify-content: center;
  padding: 0 var(--space-3);
  border: 1px solid var(--c-border-control);
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--text-primary);
  font: inherit;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  cursor: pointer;
}

.att__date-label:hover {
  border-color: var(--c-primary);
}

.att__classes {
  flex: 1 1 auto;
  min-width: 0;
}

.att__timeline {
  padding-top: 0;
}

.att__roster {
  padding: 0;
  overflow: hidden;
}

.att__roster-head {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--space-3);
  padding: var(--space-3) var(--space-4);
  border-bottom: 1px solid var(--dash-edge);
}

.att__keys,
.att__sel {
  margin-left: auto;
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
}

.att__sel {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  font-weight: 700;
  color: var(--c-primary);
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

.att__list {
  outline: none;
}

.att__list:focus-visible {
  box-shadow: inset 0 0 0 2px
    color-mix(in srgb, var(--c-primary) 30%, transparent);
}

.att__nothing {
  padding: var(--space-6);
  text-align: center;
  color: var(--brand-text-muted);
}

.att__savebar {
  position: sticky;
  bottom: var(--space-3);
  z-index: 5;
  display: flex;
  align-items: center;
  gap: var(--space-2);
  align-self: center;
  padding: var(--space-2) var(--space-4);
  border: 1px solid var(--dash-edge);
  border-radius: var(--radius-pill);
  background: var(--card-bg);
  box-shadow: var(--dash-shadow-hover);
  font-size: var(--text-sm);
  font-weight: 600;
}

.att__savebar--saved .q-icon {
  color: var(--st-present);
}

.att__savebar--error {
  border-color: var(--st-absent);
  color: var(--st-absent);
}

.att__skel {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.att__skel-timeline {
  height: calc(var(--control-h) * 2);
  border-radius: var(--radius-md);
}

.att__skel-summary {
  height: calc(var(--control-h) * 3);
  border-radius: var(--radius-md);
}

.att__skel-row {
  height: var(--control-h);
  border-radius: var(--radius-sm);
}

@media (max-width: 1023px) {
  .att__bar {
    flex-direction: column;
    align-items: stretch;
  }

  .att__date {
    justify-content: space-between;
  }

  .att__date-label {
    flex: 1 1 auto;
    min-width: 0;
  }
}
</style>

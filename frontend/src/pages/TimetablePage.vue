<template>
  <page-layout
    icon="calendar_view_week"
    color="var(--subj-1)"
    title="Dars jadvali"
    :subtitle="subtitle"
    class="ttp"
  >
    <template #actions>
      <q-btn-dropdown
        split
        unelevated
        no-caps
        color="primary"
        icon="picture_as_pdf"
        label="PDF"
        class="no-print"
        :loading="pdfLoading"
        :disable="view !== 'class' || !classId"
        @click="downloadPdf(false)"
      >
        <q-list dense>
          <q-item v-close-popup clickable @click="downloadPdf(false)">
            <q-item-section avatar><q-icon name="description" /></q-item-section>
            <q-item-section>{{ currentName || 'Sinf' }} — A4, bitta sahifa</q-item-section>
          </q-item>
          <q-item v-close-popup clickable @click="downloadPdf(true)">
            <q-item-section avatar><q-icon name="library_books" /></q-item-section>
            <q-item-section>Barcha sinflar (har sinf alohida sahifada)</q-item-section>
          </q-item>
        </q-list>
      </q-btn-dropdown>
      <AppButton
        variant="ghost"
        icon="print"
        class="no-print gt-xs"
        aria-label="Chop etish"
        @click="printPage"
        ><q-tooltip>Chop etish (Ctrl+P)</q-tooltip></AppButton
      >
      <AppButton
        variant="ghost"
        icon="list"
        class="no-print"
        aria-label="Ro'yxat ko'rinishi"
        to="/app/lesson-slots"
        ><q-tooltip>Ro'yxat ko'rinishi</q-tooltip></AppButton
      >
    </template>

    <!-- hero -->
    <section class="ttp__hero no-print" data-tt-hero>
      <KpiCard
        kpi-key="lessons"
        label="Haftalik darslar"
        icon="event_note"
        color="var(--chart-primary)"
        compact
        :loading="kpis.loading.value"
        :value="k?.lessons"
        :note="k ? `6 kun · ${week?.classes.length || 0} sinf` : ''"
        :spark="k?.lessonsByDay"
        style="--i: 0"
      />
      <KpiCard
        kpi-key="load"
        label="O'qituvchi yuklamasi"
        icon="person_pin"
        color="var(--chart-secondary)"
        compact
        clickable
        :loading="kpis.loading.value"
        :value="k?.teacherHours ?? undefined"
        :empty-value="!!k && k.teacherHours === null"
        :format="v => `${v.toFixed(1)} soat`"
        :note="k ? `o'rtacha, haftasiga · ${k.teachers} o'qituvchi` : ''"
        :spark="k?.teacherHoursByDay"
        style="--i: 1"
        @click="setView('load')"
      />
      <KpiCard
        kpi-key="rooms"
        label="Xonalar bandligi"
        icon="meeting_room"
        color="var(--room-busy)"
        compact
        :loading="kpis.loading.value"
        :value="k?.roomUse ?? undefined"
        :empty-value="!!k && k.roomUse === null"
        :format="v => `${v.toFixed(1)}%`"
        :note="k ? `${k.rooms} ta faol xona` : ''"
        :spark="k?.roomUseByDay"
        :spark-min="0"
        :spark-max="100"
        to="/rooms"
        style="--i: 2"
      />
      <KpiCard
        kpi-key="conflicts"
        label="Konfliktlar"
        :icon="conflicts.length ? 'report' : 'verified'"
        :color="conflicts.length ? 'var(--st-absent)' : 'var(--st-present)'"
        compact
        clickable
        :loading="kpis.loading.value"
        :value="k ? conflicts.length : undefined"
        :note="
          k
            ? conflicts.length
              ? 'bosing — ro\'yxat va yechim'
              : 'hammasi joyida'
            : ''
        "
        :spark="null"
        :class="['ttp__conflicts', conflicts.length ? 'is-bad' : 'is-ok']"
        style="--i: 3"
        @click="conflictsOpen = true"
      />
    </section>

    <!-- views and the picker -->
    <div class="ttp__bar no-print">
      <SegTabs
        :model-value="view"
        :options="VIEWS"
        label="Ko'rinish"
        data-tt-views
        @update:model-value="setView"
      />
      <AppSelect
        v-if="view === 'class'"
        :model-value="classId"
        class="ttp__pick"
        dense
        label="Sinf"
        :options="classOptions"
        emit-value
        map-options
        data-tt-pick="class"
        @update:model-value="v => setQuery({ class: v })"
      />
      <AppSelect
        v-else-if="view === 'teacher'"
        :model-value="teacherId"
        class="ttp__pick"
        dense
        use-input
        input-debounce="0"
        label="O'qituvchi"
        :options="teacherOptions"
        emit-value
        map-options
        data-tt-pick="teacher"
        @filter="filterTeachers"
        @update:model-value="v => setQuery({ teacher: v })"
      />
      <AppSelect
        v-else-if="view === 'room'"
        :model-value="roomId"
        class="ttp__pick"
        dense
        label="Xona"
        :options="roomOptions"
        emit-value
        map-options
        data-tt-pick="room"
        @update:model-value="v => setQuery({ room: v })"
      />
      <SegTabs
        v-if="isPhone && gridView"
        v-model="phoneDay"
        :options="phoneDays"
        label="Kun"
      />
      <span v-if="view === 'teacher' && teacherInfo" class="ttp__info">
        <b>{{ teacherInfo.hours.toFixed(1) }}</b> soat/hafta ·
        <b>{{ teacherInfo.lessons }}</b> dars ·
        <span v-if="teacherInfo.windows.length" class="ttp__windows"
          >bo'sh oynalar:
          <span v-for="w in teacherInfo.windows" :key="w.day" class="ttp__window"
            >{{ w.day.slice(0, 2) }} {{ w.periods.join(', ') }}-dars</span
          ></span
        >
        <span v-else class="ttp__ok">bo'sh oyna yo'q</span>
      </span>
      <span v-if="view === 'room' && roomUse !== null" class="ttp__info">
        bandlik <b>{{ roomUse }}%</b>
      </span>
      <span v-if="canEdit && gridView" class="ttp__hint gt-sm">
        <q-icon name="open_with" /> darsni sudrab ko'chiring · bo'sh katakda «+»
      </span>
    </div>

    <!-- the view -->
    <section
      ref="box"
      class="ttp__box dash-card"
      :class="{ 'ttp__box--grid': gridView && !isPhone }"
      :style="gridView && !isPhone && boxHeight ? { height: `${boxHeight}px` } : undefined"
      data-tt-box
    >
      <div v-if="weekLoading && !week" class="ttp__skel">
        <div v-for="i in 7" :key="i" class="skel ttp__skel-row" />
      </div>
      <EmptyState
        v-else-if="weekError && !week"
        icon="cloud_off"
        title="Jadvalni yuklab bo'lmadi"
        :text="weekError"
        action="Qayta urinish"
        action-icon="refresh"
        @action="loadWeek"
      />
      <template v-else-if="week">
        <template v-if="gridView">
          <EmptyState
            v-if="!viewLessons.length"
            icon="event_busy"
            :title="`Bu ${NOUN[view]} uchun jadval kiritilmagan`"
            :action="canEdit && view === 'class' ? 'Dars qo\'shish' : ''"
            @action="openAdd({ day: 'Dushanba', period: week.periods[0] })"
          />
          <WeekGrid
            v-else
            :lessons="viewLessons"
            :all="week.lessons"
            :periods="week.periods"
            :days="isPhone ? [phoneDay] : WEEKDAYS"
            :today="today"
            :now-minutes="nowMinutes"
            :mode="view"
            :focus-subject="focusSubject"
            :editable="canEdit && !isPhone"
            :states="todayStates"
            :subs="todaySubs"
            :label="`${currentName} — haftalik jadval`"
            @open="openLesson"
            @move="moveLesson"
            @add="openAdd"
          />
        </template>
        <template v-else-if="view === 'today'">
          <div class="ttp__today-head">
            <SegTabs v-model="todayDay" :options="todayOptions" label="Kun" />
            <span class="ttp__info">
              {{ todayDay === today ? 'bugun' : 'boshqa kun' }} ·
              {{ dayLessons.length }} dars ·
              <span title="Davomat olingan">✓ {{ stateCount.MANUAL }}</span>
              <span title="Avtomatik">🤖 {{ stateCount.AUTO }}</span>
              <span title="Olinmagan">⏳ {{ stateCount.NONE }}</span>
            </span>
          </div>
          <EmptyState
            v-if="!dayLessons.length"
            icon="weekend"
            :title="today ? 'Bu kunda dars yo\'q' : 'Bugun yakshanba — darslar yo\'q'"
          />
          <TodayTimeline
            v-else
            :lessons="dayLessons"
            :classes="week.classes"
            :periods="week.periods"
            :is-today="todayDay === today"
            :now-minutes="nowMinutes"
            :focus-subject="focusSubject"
            :states="todayDay === today ? todayStates : {}"
            :subs="todayDay === today ? todaySubs : {}"
            @open="openLesson"
          />
        </template>
        <TeacherLoad
          v-else-if="view === 'load'"
          :lessons="week.lessons"
          @pick="id => setView('teacher', { teacher: id })"
        />
      </template>
    </section>

    <!-- subject legend: a chip focuses one subject -->
    <div v-if="week && legend.length && view !== 'load'" class="ttp__legend no-print" data-tt-legend>
      <SubjectChip
        v-for="s in legend"
        :key="s.id"
        :subject-id="s.id"
        :name="s.name"
        :chosen-color="s.color"
        :count="s.count"
        clickable
        :active="focusSubject === s.id"
        :dim="focusSubject !== null && focusSubject !== s.id"
        @toggle="id => (focusSubject = focusSubject === id ? null : id)"
      />
    </div>

    <SideDrawer
      v-model="conflictsOpen"
      title="Konfliktlar"
      :subtitle="conflicts.length ? `${conflicts.length} ta — bosing, dars ochiladi` : 'Hammasi joyida'"
      icon="report"
      :color="conflicts.length ? 'var(--st-absent)' : 'var(--st-present)'"
    >
      <EmptyState
        v-if="!conflicts.length"
        kind="ok"
        title="Konflikt yo'q"
        text="Hech bir o'qituvchi, xona yoki sinf bir vaqtda ikki joyda emas"
      />
      <SignalRow
        v-for="(c, i) in conflicts"
        :key="i"
        :icon="CONFLICT_ICON[c.kind]"
        tone="bad"
        :title="c.text"
        :subtitle="`${c.weekday}, ${hm(c.start)}–${hm(c.end)}`"
        clickable
        data-conflict
        @click="openConflict(c)"
      />
    </SideDrawer>

    <LessonDrawer
      v-model="lessonOpen"
      :lesson="lesson"
      :periods="week?.periods || []"
      :phone="lessonPhone"
      :can-edit="canEdit"
      @changed="reloadToday"
    />

    <QuickAddDialog
      v-if="week"
      v-model="addOpen"
      :slot="addSlot"
      :fixed="addFixed"
      :week="week"
      @saved="loadWeek"
    />
  </page-layout>
</template>

<script setup>
// "Dars jadvali": the hero (lessons a week, teachers' load, room use, conflicts — 0 is
// green, more is red and opens the list) and five views kept in the URL: a class's week
// (on one screen), a teacher's week (hours and empty "windows"), a room's week, today for
// the whole school (a red line at the present minute) and the teachers' load. A lesson
// opens its side panel (attendance, journal, substitute); editors drag lessons — every
// cell checks itself live — and add one with "+". The legend focuses one subject.
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useQuasar } from 'quasar'
import PageLayout from '@/components/PageLayout.vue'
import AppButton from '@/components/app/AppButton.vue'
import AppSelect from '@/components/app/AppSelect.vue'
import KpiCard from '@/components/dashboard/KpiCard.vue'
import SegTabs from '@/components/dashboard/SegTabs.vue'
import SideDrawer from '@/components/dashboard/SideDrawer.vue'
import EmptyState from '@/components/pages/EmptyState.vue'
import SignalRow from '@/components/pages/SignalRow.vue'
import SubjectChip from '@/components/pages/SubjectChip.vue'
import WeekGrid from '@/components/timetable/WeekGrid.vue'
import TodayTimeline from '@/components/timetable/TodayTimeline.vue'
import TeacherLoad from '@/components/timetable/TeacherLoad.vue'
import LessonDrawer from '@/components/timetable/LessonDrawer.vue'
import QuickAddDialog from '@/components/timetable/QuickAddDialog.vue'
import {
  WEEKDAYS,
  WEEKDAY_SHORT,
  hm,
  minutes,
  roomLabel,
  teacherHours,
  teacherWindows,
  todayWeekday
} from '@/components/timetable/timetable'
import { confirmUndoToast } from '@/composables/useUndoToast'
import { useWidget } from '@/composables/useWidget'
import { api } from '@/boot/axios'
import { useAuthStore } from '@/stores/auth'
import { useSchoolStore } from '@/stores/school'
import { tashkentNow } from '@/utils/tashkent'

const route = useRoute()
const router = useRouter()
const $q = useQuasar()
const auth = useAuthStore()
const school = useSchoolStore()

const VIEWS = [
  { value: 'class', label: 'Sinf' },
  { value: 'teacher', label: "O'qituvchi" },
  { value: 'room', label: 'Xona' },
  { value: 'today', label: 'Bugun' },
  { value: 'load', label: 'Yuklama' }
]
const NOUN = { class: 'sinf', teacher: "o'qituvchi", room: 'xona' }
const CONFLICT_ICON = { TEACHER: 'person', ROOM: 'meeting_room', CLASS: 'groups' }

const canEdit = computed(() => auth.isEditor)
const isPhone = computed(() => $q.screen.lt.sm)

// ---------------------------------------------------------------- the URL is the state
const view = computed(() =>
  VIEWS.some(v => v.value === route.query.view) ? route.query.view : 'class'
)
const gridView = computed(() => ['class', 'teacher', 'room'].includes(view.value))
const classId = computed(() => Number(route.query.class) || defaultClass.value)
const teacherId = computed(() => Number(route.query.teacher) || defaultTeacher.value)
const roomId = computed(() => Number(route.query.room) || defaultRoom.value)

function setQuery(patch) {
  router.replace({ query: { ...route.query, ...patch } })
}
function setView(v, extra = {}) {
  setQuery({ view: v, ...extra })
}

// ---------------------------------------------------------------- data
const week = ref(null)
const weekLoading = ref(false)
const weekError = ref(null)

async function loadWeek() {
  if (!school.activeSchoolId) return
  weekLoading.value = true
  try {
    const res = await api.get('/api/pages/timetable/week', {
      params: { schoolId: school.activeSchoolId }
    })
    week.value = res.data
    weekError.value = null
    kpis.reload()
  } catch (e) {
    weekError.value = e.friendlyMessage || "Yuklab bo'lmadi"
  } finally {
    weekLoading.value = false
    nextTick(fit)
  }
}

const kpis = useWidget('/api/pages/timetable/kpis')
const k = computed(() => kpis.data.value)
const conflicts = computed(() => k.value?.conflicts || [])

const todayDate = ref(tashkentNow().date)
const todayInfo = useWidget('/api/pages/timetable/day', {
  params: computed(() => ({ date: todayDate.value }))
})
const todayStates = computed(() => todayInfo.data.value?.states || {})
const todaySubs = computed(() => {
  const m = {}
  for (const s of todayInfo.data.value?.substitutions || []) m[s.lessonSlotId] = s.substitute
  return m
})
function reloadToday() {
  todayInfo.reload()
}

const today = ref(todayWeekday())
const nowMinutes = ref(tashkentNow().minutes)
let clock = null
onMounted(() => {
  loadWeek()
  clock = setInterval(() => {
    const n = tashkentNow()
    nowMinutes.value = n.minutes
    if (n.date !== todayDate.value) {
      todayDate.value = n.date
      today.value = todayWeekday()
    }
  }, 30_000)
  window.addEventListener('resize', onResize)
})
onBeforeUnmount(() => {
  clearInterval(clock)
  window.removeEventListener('resize', onResize)
})
watch(
  () => school.activeSchoolId,
  () => loadWeek()
)

// ---------------------------------------------------------------- pickers
const defaultClass = computed(() => week.value?.classes[0]?.id || null)
const defaultTeacher = computed(() => {
  const withLessons = new Set((week.value?.lessons || []).map(l => l.teacherId))
  return week.value?.teachers.find(t => withLessons.has(t.id))?.id || null
})
const defaultRoom = computed(() => {
  const used = new Set((week.value?.lessons || []).map(l => l.roomId))
  return week.value?.rooms.find(r => used.has(r.id))?.id || null
})
const classOptions = computed(() =>
  (week.value?.classes || []).map(c => ({ value: c.id, label: c.name }))
)
const teacherQuery = ref('')
const teacherOptions = computed(() => {
  const withLessons = new Set((week.value?.lessons || []).map(l => l.teacherId))
  const q = teacherQuery.value.toLowerCase()
  return (week.value?.teachers || [])
    .filter(t => withLessons.has(t.id) && (!q || t.name.toLowerCase().includes(q)))
    .map(t => ({ value: t.id, label: t.name }))
})
function filterTeachers(val, update) {
  update(() => (teacherQuery.value = val || ''))
}
const roomOptions = computed(() =>
  (week.value?.rooms || []).map(r => ({
    value: r.id,
    label: `${roomLabel(r.number)} · ${r.building}`
  }))
)

const currentName = computed(() => {
  if (!week.value) return ''
  if (view.value === 'class')
    return week.value.classes.find(c => c.id === classId.value)?.name || ''
  if (view.value === 'teacher')
    return week.value.teachers.find(t => t.id === teacherId.value)?.name || ''
  if (view.value === 'room')
    return roomLabel(week.value.rooms.find(r => r.id === roomId.value)?.number)
  return view.value === 'today' ? 'Bugun — butun maktab' : "O'qituvchilar yuklamasi"
})
const subtitle = computed(() =>
  [school.activeSchoolName, currentName.value].filter(Boolean).join(' · ')
)

const viewLessons = computed(() => {
  const all = week.value?.lessons || []
  if (view.value === 'class') return all.filter(l => l.classId === classId.value)
  if (view.value === 'teacher') return all.filter(l => l.teacherId === teacherId.value)
  if (view.value === 'room') return all.filter(l => l.roomId === roomId.value)
  return all
})

const teacherInfo = computed(() => {
  if (view.value !== 'teacher' || !week.value) return null
  const mine = viewLessons.value
  return {
    hours: teacherHours(mine).get(teacherId.value) || 0,
    lessons: mine.length,
    windows: teacherWindows(mine, week.value.periods)
  }
})
const roomUse = computed(() => {
  if (view.value !== 'room' || !week.value?.periods.length) return null
  const cells = new Set(viewLessons.value.map(l => `${l.weekday}|${hm(l.start)}`))
  return Math.round((cells.size / (week.value.periods.length * 6)) * 1000) / 10
})

// phone: one day at a time
const phoneDay = ref(todayWeekday() || 'Dushanba')
const phoneDays = WEEKDAYS.map((d, i) => ({ value: d, label: WEEKDAY_SHORT[i] }))

// today's view
const todayDay = ref(todayWeekday() || 'Dushanba')
const todayOptions = WEEKDAYS.map((d, i) => ({
  value: d,
  label: d === todayWeekday() ? 'Bugun' : WEEKDAY_SHORT[i]
}))
const dayLessons = computed(() =>
  (week.value?.lessons || []).filter(l => l.weekday === todayDay.value)
)
const stateCount = computed(() => {
  const c = { MANUAL: 0, AUTO: 0, NONE: 0 }
  if (todayDay.value !== today.value) return c
  for (const l of dayLessons.value) c[todayStates.value[l.id] || 'NONE']++
  return c
})

// legend: the subjects of what is on screen, with how many lessons
const focusSubject = ref(null)
const legend = computed(() => {
  const list = view.value === 'today' ? dayLessons.value : viewLessons.value
  const m = new Map()
  for (const l of list) {
    if (!m.has(l.subjectId))
      m.set(l.subjectId, { id: l.subjectId, name: l.subject, color: l.subjectColor, count: 0 })
    m.get(l.subjectId).count++
  }
  return [...m.values()].sort((a, b) => b.count - a.count || a.name.localeCompare(b.name))
})
watch(view, () => (focusSubject.value = null))

// ---------------------------------------------------------------- the grid fills the screen
const box = ref(null)
const boxHeight = ref(0)
function fit() {
  if (!box.value || isPhone.value || !gridView.value) return
  const top = box.value.getBoundingClientRect().top + window.scrollY
  const legendH = 44
  boxHeight.value = Math.max(360, Math.floor(window.innerHeight - top - legendH - 16))
}
let resizeTimer = null
function onResize() {
  clearTimeout(resizeTimer)
  resizeTimer = setTimeout(fit, 80)
}
watch([view, isPhone, () => week.value?.periods.length], () => nextTick(fit))

// ---------------------------------------------------------------- a lesson
const lessonOpen = ref(false)
const lesson = ref(null)
function openLesson(l) {
  lesson.value = l
  lessonOpen.value = true
}
const lessonPhone = computed(
  () => week.value?.teachers.find(t => t.id === lesson.value?.teacherId)?.phone || null
)
function openConflict(c) {
  const l = week.value?.lessons.find(x => x.id === c.lessonIds[0])
  conflictsOpen.value = false
  if (l) openLesson(l)
}
const conflictsOpen = ref(false)

// ---------------------------------------------------------------- moving a lesson
function body(l, weekday, start, end) {
  return {
    schoolClassId: l.classId,
    subjectId: l.subjectId,
    employeeId: l.teacherId,
    roomId: l.roomId,
    weekday,
    startTime: `${hm(start)}:00`,
    endTime: `${hm(end)}:00`
  }
}

async function moveLesson({ lesson: l, day, period }) {
  const before = { weekday: l.weekday, start: l.start, end: l.end }
  const local = week.value.lessons.find(x => x.id === l.id)
  Object.assign(local, { weekday: day, start: period.start, end: period.end })
  try {
    await api.put(`/api/lesson-slots/${l.id}`, body(l, day, period.start, period.end))
    kpis.reload()
    confirmUndoToast(
      `${l.subject} (${l.className}) → ${day}, ${period.n}-dars`,
      async () => {
        await api.put(`/api/lesson-slots/${l.id}`, body(l, before.weekday, before.start, before.end))
        Object.assign(local, before)
        kpis.reload()
      }
    )
  } catch (e) {
    Object.assign(local, before)
    $q.notify({ type: 'negative', message: e.friendlyMessage || "Ko'chirib bo'lmadi" })
  }
}

// ---------------------------------------------------------------- adding a lesson
const addOpen = ref(false)
const addSlot = ref(null)
const addFixed = computed(() =>
  view.value === 'class'
    ? { classId: classId.value }
    : view.value === 'teacher'
      ? { teacherId: teacherId.value }
      : view.value === 'room'
        ? { roomId: roomId.value }
        : {}
)
function openAdd(slot) {
  addSlot.value = slot
  addOpen.value = true
}

// ---------------------------------------------------------------- PDF and print
const pdfLoading = ref(false)
async function downloadPdf(all) {
  pdfLoading.value = true
  try {
    const params = { schoolId: school.activeSchoolId }
    if (!all) params.schoolClassId = classId.value
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
      : `dars-jadvali-${currentName.value || 'sinf'}.pdf`
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
</script>

<style scoped>
.ttp :deep(.page-layout__body) {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.ttp {
  --page-gap: var(--space-4);
}

.ttp__hero {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--space-3);
}

.ttp__conflicts.is-bad :deep(.kpi__value) {
  color: var(--st-absent);
}

.ttp__conflicts.is-ok :deep(.kpi__value) {
  color: var(--st-present);
}

.ttp__bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-3);
}

.ttp__pick {
  width: var(--field-w);
}

.ttp__info {
  display: inline-flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
}

.ttp__info b {
  color: var(--text-primary);
}

.ttp__window {
  margin-left: var(--space-1);
  padding: 0 var(--space-2);
  border-radius: var(--radius-pill);
  background: color-mix(in srgb, var(--st-late) 14%, transparent);
  color: var(--st-late);
  font-weight: 600;
}

.ttp__ok {
  color: var(--st-present);
  font-weight: 600;
}

.ttp__hint {
  margin-left: auto;
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
}

.ttp__box {
  padding: var(--space-3);
  min-width: 0;
}

.ttp__box:hover {
  transform: none;
}

.ttp__box--grid {
  overflow: hidden;
}

.ttp__skel {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.ttp__skel-row {
  height: var(--tt-row-min);
}

.ttp__today-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-3);
  margin-bottom: var(--space-3);
}

.ttp__legend {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

@media (max-width: 1023px) {
  .ttp__hero {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 599px) {
  .ttp__pick {
    width: 100%;
  }
}

@media print {
  .ttp__box {
    height: 180mm !important;
    box-shadow: none !important;
  }
}
</style>

<style>
/* printing the timetable: only the grid, A4 landscape */
@media print {
  @page {
    size: A4 landscape;
    margin: 10mm;
  }

  .q-header,
  .q-drawer,
  .q-footer,
  .q-loading-bar,
  .no-print {
    display: none !important;
  }

  .q-page-container {
    padding: 0 !important;
  }
}
</style>

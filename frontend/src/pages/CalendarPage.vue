<template>
  <page-layout
    icon="event_available"
    color="var(--st-present)"
    title="Taqvim"
    :subtitle="subtitle"
    class="cal"
  >
    <template #actions>
      <AppButton
        variant="secondary"
        icon="ios_share"
        label="Kalendarga qo'shish"
        data-cal-export
        @click="feedOpen = true"
      />
      <AppButton
        v-if="canEdit"
        icon="add"
        label="Tadbir"
        data-cal-add
        @click="openCreate(today)"
      />
    </template>

    <!-- hero: the year line and four numbers -->
    <section class="cal__hero">
      <div class="cal__year dash-card" data-cal-hero>
        <div class="cal__year-head">
          <b>O'quv yili</b>
          <small v-if="ov">{{ fmtDate(ov.yearStart) }} – {{ fmtDate(ov.yearEnd) }}</small>
        </div>
        <div v-if="!ov" class="skel cal__year-skel" />
        <YearLine v-else :data="ov" />
      </div>
      <div class="cal__kpis">
        <KpiCard
          kpi-key="next-event"
          label="Keyingi tadbir"
          icon="event_upcoming"
          color="var(--subj-1)"
          compact
          clickable
          :loading="overview.loading.value"
          :value="ov?.nextEvent?.daysLeft ?? undefined"
          :empty-value="!!ov && !ov.nextEvent"
          empty-text="Yo'q"
          :format="v => countdown(Math.round(v))"
          :note="ov?.nextEvent?.title || ''"
          :spark="null"
          @click="ov?.nextEvent && openById(ov.nextEvent.id)"
        />
        <KpiCard
          kpi-key="month-events"
          label="Shu oy tadbirlari"
          icon="date_range"
          color="var(--chart-secondary)"
          compact
          :loading="overview.loading.value"
          :value="ov?.monthEvents ?? undefined"
          note="o'quv yili bo'yicha oylar"
          :spark="ov?.eventsByMonth"
        />
        <KpiCard
          kpi-key="next-exam"
          label="Keyingi imtihon"
          icon="quiz"
          color="var(--st-absent)"
          compact
          clickable
          :loading="overview.loading.value"
          :value="ov?.nextExam?.daysLeft ?? undefined"
          :empty-value="!!ov && !ov.nextExam"
          empty-text="Rejada yo'q"
          :format="v => countdown(Math.round(v))"
          :note="ov?.nextExam?.title || ''"
          :spark="null"
          @click="ov?.nextExam && openById(ov.nextExam.id)"
        />
        <KpiCard
          kpi-key="vacation"
          label="Ta'tilgacha"
          icon="beach_access"
          color="var(--year-vacation)"
          compact
          :loading="overview.loading.value"
          :value="ov && !ov.onVacation ? ov.nextVacation?.daysLeft ?? undefined : undefined"
          :empty-value="!!ov && (ov.onVacation || !ov.nextVacation)"
          :empty-text="ov?.onVacation ? 'Ta\'tilda 🎉' : 'Rejada yo\'q'"
          :format="v => `${Math.round(v)} kun`"
          :note="ov?.nextVacation?.title || ''"
          :spark="null"
        />
      </div>
    </section>

    <!-- controls -->
    <div class="cal__bar">
      <SegTabs :model-value="view" :options="VIEWS" label="Ko'rinish" data-cal-views @update:model-value="setView" />
      <div v-if="view !== 'agenda'" class="cal__nav">
        <AppButton variant="ghost" icon="chevron_left" aria-label="Oldingi" @click="shift(-1)" />
        <b class="cal__period" data-cal-period>{{ periodLabel }}</b>
        <AppButton variant="ghost" icon="chevron_right" aria-label="Keyingi" @click="shift(1)" />
        <AppButton variant="ghost" size="sm" label="Bugun" @click="goToday" />
      </div>
      <div class="cal__filters" role="group" aria-label="Tadbir turlari">
        <button
          v-for="t in TYPE_ORDER"
          :key="t"
          type="button"
          class="cal__chip"
          :class="{ 'cal__chip--off': hiddenTypes.has(t) }"
          :style="{ '--c': EVENT_TYPES[t].color }"
          :aria-pressed="!hiddenTypes.has(t)"
          :data-type-filter="t"
          @click="toggleType(t)"
        >
          <q-icon :name="EVENT_TYPES[t].icon" /> {{ EVENT_TYPES[t].label }}
          <b>{{ typeCounts[t] || 0 }}</b>
        </button>
      </div>
    </div>

    <section class="cal__box dash-card" data-cal-box>
      <div v-if="loading && !events.length" class="cal__skel">
        <div v-for="i in 5" :key="i" class="skel cal__skel-row" />
      </div>
      <EmptyState
        v-else-if="error && !events.length"
        icon="cloud_off"
        title="Taqvimni yuklab bo'lmadi"
        :text="error"
        action="Qayta urinish"
        action-icon="refresh"
        @action="loadEvents"
      />
      <MonthView
        v-else-if="view === 'month'"
        :year="cursor.year"
        :month="cursor.month"
        :events="shown"
        :attendance="attendance"
        :today="today"
        :editable="canEdit"
        @open="openEvent"
        @day="openDay"
        @create="openCreate"
        @move="moveEvent"
        @resize="resizeEvent"
      />
      <WeekView
        v-else-if="view === 'week'"
        :monday="monday"
        :events="shown"
        :today="today"
        :editable="canEdit"
        @open="openEvent"
        @day="openDay"
        @create="openCreate"
      />
      <AgendaView
        v-else-if="view === 'agenda'"
        :events="shown"
        :today="today"
        :editable="canEdit"
        @open="openEvent"
        @create="openCreate"
      />
      <YearView
        v-else
        :start="yearStart"
        :events="shown"
        :today="today"
        @month="m => setView('month', { month: m.key })"
      />
    </section>

    <div class="cal__legend">
      <span v-for="s in [1, 2, 3, 4]" :key="s" class="cal__heat">
        <span :class="`cal__dot cal__dot--${s}`" />{{ HEAT_LABELS[s - 1] }}
      </span>
      <span class="cal__legend-note">— o'tgan ish kunlari davomati</span>
    </div>

    <EventDialog
      v-model="editOpen"
      :event="editing"
      :rooms="rooms"
      :classes="classes"
      :can-delete="auth.isAdmin"
      @saved="afterSave"
    />
    <DayDrawer
      v-model="dayOpen"
      :date="dayDate"
      :editable="canEdit"
      @open="e => ((dayOpen = false), openEvent(e))"
      @create="d => ((dayOpen = false), openCreate(d))"
    />
    <FeedDialog v-model="feedOpen" :admin="auth.isAdmin" />
  </page-layout>
</template>

<script setup>
// "Taqvim": the school year as one line (quarters, vacations, "you are here") and four
// numbers (the next event counting down, this month, the next exam, days to the vacation);
// four views — month, week, list, year — and type filters. Editors click an empty day to
// add an event, drag one to another day (5 s to undo), stretch a vacation from its edge.
// A day opens its summary. The calendar can be taken to Google / Apple as .ics.
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useQuasar } from 'quasar'
import PageLayout from '@/components/PageLayout.vue'
import AppButton from '@/components/app/AppButton.vue'
import KpiCard from '@/components/dashboard/KpiCard.vue'
import SegTabs from '@/components/dashboard/SegTabs.vue'
import EmptyState from '@/components/pages/EmptyState.vue'
import YearLine from '@/components/calendar/YearLine.vue'
import MonthView from '@/components/calendar/MonthView.vue'
import WeekView from '@/components/calendar/WeekView.vue'
import AgendaView from '@/components/calendar/AgendaView.vue'
import YearView from '@/components/calendar/YearView.vue'
import EventDialog from '@/components/calendar/EventDialog.vue'
import DayDrawer from '@/components/calendar/DayDrawer.vue'
import FeedDialog from '@/components/calendar/FeedDialog.vue'
import {
  EVENT_TYPES,
  MONTHS,
  TYPE_ORDER,
  addDays,
  countdown,
  monthWeeks,
  moved,
  parse,
  weekdayIndex
} from '@/components/calendar/calendar'
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
  { value: 'month', label: 'Oy' },
  { value: 'week', label: 'Hafta' },
  { value: 'agenda', label: "Ro'yxat" },
  { value: 'year', label: 'Yil' }
]
const HEAT_LABELS = ['≥ 95%', '90–95%', '85–90%', '< 85%']
const canEdit = computed(() => auth.isEditor)
const today = ref(tashkentNow().date)

// ---------------------------------------------------------------- the URL is the state
const view = computed(() => (VIEWS.some(v => v.value === route.query.view) ? route.query.view : 'month'))
const anchor = computed(() =>
  /^\d{4}-\d{2}$/.test(route.query.month || '') ? `${route.query.month}-01` : today.value
)
const cursor = computed(() => ({
  year: Number(anchor.value.slice(0, 4)),
  month: Number(anchor.value.slice(5, 7)) - 1
}))
const weekAnchor = computed(() =>
  /^\d{4}-\d{2}-\d{2}$/.test(route.query.week || '') ? route.query.week : today.value
)
const monday = computed(() => addDays(weekAnchor.value, -weekdayIndex(weekAnchor.value)))
const yearStart = computed(() => {
  const y = cursor.value.month >= 8 ? cursor.value.year : cursor.value.year - 1
  return `${y}-09-01`
})

function setView(v, extra = {}) {
  router.replace({ query: { ...route.query, view: v, ...extra } })
}
function shift(n) {
  if (view.value === 'month') {
    const d = new Date(cursor.value.year, cursor.value.month + n, 1)
    setView('month', { month: `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}` })
  } else if (view.value === 'week') {
    setView('week', { week: addDays(monday.value, 7 * n) })
  } else {
    const y = Number(yearStart.value.slice(0, 4)) + n
    setView('year', { month: `${y}-09` })
  }
}
function goToday() {
  router.replace({ query: { view: view.value } })
}

const periodLabel = computed(() => {
  if (view.value === 'month') return `${MONTHS[cursor.value.month]} ${cursor.value.year}`
  if (view.value === 'week') {
    const s = parse(monday.value)
    const e = parse(addDays(monday.value, 6))
    return `${s.getDate()} ${MONTHS[s.getMonth()].slice(0, 3)} – ${e.getDate()} ${MONTHS[e.getMonth()].slice(0, 3)}`
  }
  const y = Number(yearStart.value.slice(0, 4))
  return `${y}–${y + 1} o'quv yili`
})
const subtitle = computed(() => [school.activeSchoolName, periodLabel.value].filter(Boolean).join(' · '))

// ---------------------------------------------------------------- data
const overview = useWidget('/api/pages/calendar/overview')
const ov = computed(() => overview.data.value)

// the range on screen (with a margin for weeks that cross months)
const range = computed(() => {
  if (view.value === 'month') {
    const w = monthWeeks(cursor.value.year, cursor.value.month)
    return { from: w[0][0], to: w[w.length - 1][6] }
  }
  if (view.value === 'week') return { from: monday.value, to: addDays(monday.value, 6) }
  if (view.value === 'agenda') return { from: today.value, to: addDays(today.value, 180) }
  const y = Number(yearStart.value.slice(0, 4))
  return { from: `${y}-09-01`, to: `${y + 1}-08-31` }
})

const events = ref([])
const loading = ref(false)
const error = ref(null)
const attendance = ref({})

async function loadEvents() {
  if (!school.activeSchoolId) return
  loading.value = true
  try {
    const res = await api.get('/api/calendar-events/range', {
      params: { schoolId: school.activeSchoolId, ...range.value }
    })
    events.value = res.data
    error.value = null
  } catch (e) {
    error.value = e.friendlyMessage || "Yuklab bo'lmadi"
  } finally {
    loading.value = false
  }
  if (view.value === 'month') {
    try {
      const res = await api.get('/api/pages/calendar/attendance', {
        params: { schoolId: school.activeSchoolId, ...range.value }
      })
      attendance.value = Object.fromEntries(res.data.filter(d => d.rate !== null).map(d => [d.date, d.rate]))
    } catch {
      attendance.value = {}
    }
  }
}
onMounted(loadEvents)
watch([range, () => school.activeSchoolId], loadEvents)

// rooms and classes for the event form
const rooms = ref([])
const classes = ref([])
async function loadRefs() {
  if (!canEdit.value || !school.activeSchoolId) return
  const res = await api.get('/api/pages/timetable/week', { params: { schoolId: school.activeSchoolId } })
  rooms.value = res.data.rooms
  classes.value = res.data.classes
}
onMounted(loadRefs)

// type filters
const hiddenTypes = ref(new Set())
function toggleType(t) {
  const s = new Set(hiddenTypes.value)
  s.has(t) ? s.delete(t) : s.add(t)
  hiddenTypes.value = s
}
const shown = computed(() => events.value.filter(e => !hiddenTypes.value.has(e.type)))
const typeCounts = computed(() => {
  const c = {}
  for (const e of events.value) c[e.type] = (c[e.type] || 0) + 1
  return c
})

// ---------------------------------------------------------------- events
const editOpen = ref(false)
const editing = ref(null)
function openEvent(e) {
  if (!canEdit.value) {
    openDay(e.startDate)
    return
  }
  editing.value = e
  editOpen.value = true
}
async function openById(id) {
  const e = events.value.find(x => x.id === id)
  if (e) return openEvent(e)
  const res = await api.get('/api/calendar-events/upcoming', { params: { schoolId: school.activeSchoolId, limit: 50 } })
  const found = res.data.find(x => x.id === id)
  if (found) openEvent(found)
}
function openCreate(date) {
  if (!canEdit.value) return
  editing.value = { startDate: date, endDate: date }
  editOpen.value = true
}
function afterSave() {
  loadEvents()
  overview.reload()
}

const dayOpen = ref(false)
const dayDate = ref(null)
function openDay(date) {
  dayDate.value = date
  dayOpen.value = true
}

function body(e, patch) {
  return {
    schoolId: school.activeSchoolId,
    title: e.title,
    description: e.description,
    type: e.type,
    startDate: e.startDate,
    endDate: e.endDate,
    startTime: e.startTime,
    endTime: e.endTime,
    roomId: e.roomId,
    location: e.location,
    classIds: e.classIds || [],
    notifyParents: e.notifyParents,
    ...patch
  }
}

async function change(e, patch, message) {
  const before = { startDate: e.startDate, endDate: e.endDate }
  const local = events.value.find(x => x.id === e.id)
  Object.assign(local, patch)
  try {
    await api.put(`/api/calendar-events/${e.id}`, body(e, patch))
    overview.reload()
    confirmUndoToast(message, async () => {
      await api.put(`/api/calendar-events/${e.id}`, body(e, before))
      Object.assign(local, before)
      overview.reload()
    })
  } catch (err) {
    Object.assign(local, before)
    $q.notify({ type: 'negative', message: err.friendlyMessage || "O'zgartirib bo'lmadi" })
  }
}

function moveEvent({ event, startDate }) {
  change(event, moved(event, startDate), `«${event.title}» → ${fmtDate(startDate)}`)
}
function resizeEvent({ event, endDate }) {
  change(event, { endDate }, `«${event.title}» ${fmtDate(endDate)} gacha`)
}

function fmtDate(s) {
  if (!s) return ''
  const d = parse(s)
  return `${d.getDate()}-${MONTHS[d.getMonth()].slice(0, 3).toLowerCase()}`
}

const feedOpen = ref(false)
</script>

<style scoped>
.cal :deep(.page-layout__body) {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.cal__hero {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(0, 2fr);
  gap: var(--space-3);
}

.cal__year {
  padding: var(--space-4) var(--space-5);
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.cal__year-head {
  display: flex;
  align-items: baseline;
  gap: var(--space-2);
}

.cal__year-head small {
  color: var(--brand-text-muted);
}

.cal__year-skel {
  height: var(--space-8);
}

.cal__kpis {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-3);
}

.cal__bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-3);
}

.cal__nav {
  display: flex;
  align-items: center;
  gap: var(--space-1);
}

.cal__period {
  min-width: var(--field-w-sm);
  text-align: center;
  font-size: var(--text-md);
}

.cal__filters {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-1);
  margin-left: auto;
}

.cal__chip {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  padding: var(--space-1) var(--space-2);
  border: 1px solid color-mix(in srgb, var(--c) 40%, transparent);
  border-radius: var(--radius-pill);
  background: color-mix(in srgb, var(--c) 10%, transparent);
  color: var(--text-primary);
  font: inherit;
  font-size: var(--text-xs);
  font-weight: 600;
  cursor: pointer;
}

.cal__chip .q-icon {
  color: var(--c);
}

.cal__chip b {
  color: var(--brand-text-muted);
}

.cal__chip--off {
  opacity: 0.4;
  background: transparent;
  text-decoration: line-through;
}

.cal__box {
  padding: var(--space-3);
  min-width: 0;
}

.cal__box:hover {
  transform: none;
}

.cal__skel {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.cal__skel-row {
  height: var(--month-cell-h);
}

.cal__legend {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-3);
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
}

.cal__heat {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
}

.cal__dot {
  width: var(--dot);
  height: var(--dot);
  border-radius: var(--radius-pill);
}

.cal__dot--1 {
  background: var(--heat-1);
}

.cal__dot--2 {
  background: var(--heat-2);
}

.cal__dot--3 {
  background: var(--heat-3);
}

.cal__dot--4 {
  background: var(--heat-4);
}

@media (max-width: 1279px) {
  .cal__hero {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 599px) {
  .cal__kpis {
    grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  }

  .cal__filters {
    margin-left: 0;
  }
}
</style>

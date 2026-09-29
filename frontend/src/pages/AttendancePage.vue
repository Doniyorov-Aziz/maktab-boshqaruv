<template>
  <page-layout
    icon="fact_check"
    color="#ef4444"
    title="Davomat olish"
    :subtitle="schoolStore.activeSchoolName"
  >
    <div v-if="todayLessons.length || myEmployeeId" class="q-mb-md">
      <div class="row items-center justify-between q-mb-sm">
        <div class="text-subtitle2 text-weight-semibold">Bugungi darslar</div>
        <q-btn-toggle
          v-if="myEmployeeId"
          v-model="onlyMyLessons"
          dense
          no-caps
          size="sm"
          toggle-color="primary"
          :options="[
            { label: 'Mening darslarim', value: true },
            { label: 'Barchasi', value: false }
          ]"
          @update:model-value="loadTodayLessons"
        />
      </div>
      <div v-if="!todayLessons.length" class="text-caption muted-text q-pb-sm"
        >Bugun dars yo'q</div
      >
      <div v-else class="today-lessons-scroll">
        <div
          v-for="l in todayLessons"
          :key="l.lessonSlotId"
          class="today-lesson-card"
          :class="{
            'today-lesson-card--active': selectedLessonId === l.lessonSlotId,
            'today-lesson-card--live': l.status === 'HOZIR'
          }"
          @click="selectTodayLesson(l)"
        >
          <div class="row items-center justify-between no-wrap">
            <span class="text-weight-bold">{{ l.className }}</span>
            <q-icon
              v-if="l.attendanceTaken"
              name="check_circle"
              color="positive"
              size="18px"
            />
            <q-badge v-else-if="l.status === 'HOZIR'" color="positive" rounded
              >hozir</q-badge
            >
          </div>
          <div class="text-caption ellipsis">{{ l.subjectName }}</div>
          <div class="text-caption muted-text"
            >{{ l.startTime.slice(0, 5) }}–{{ l.endTime.slice(0, 5) }}</div
          >
        </div>
      </div>
    </div>

    <div class="brand-card q-pa-md q-mb-md">
      <div class="row q-col-gutter-md items-end">
        <div class="col-12 col-sm-3">
          <q-select
            v-model="selectedClassId"
            :options="classOptions"
            option-value="value"
            option-label="label"
            emit-value
            map-options
            outlined
            dense
            label="Sinf"
            @update:model-value="onClassChange"
          />
        </div>
        <div class="col-12 col-sm-4">
          <q-select
            v-model="selectedLessonId"
            :options="lessonOptions"
            option-value="value"
            option-label="label"
            emit-value
            map-options
            outlined
            dense
            label="Dars"
            :disable="!selectedClassId"
            @update:model-value="loadRoster"
          />
        </div>
        <div class="col-12 col-sm-3">
          <date-field
            v-model="selectedDate"
            label="Sana"
            @update:model-value="loadRoster"
          />
        </div>
        <div class="col-12 col-sm-2">
          <q-btn
            color="primary"
            unelevated
            no-caps
            class="full-width mark-all-btn"
            icon="done_all"
            label="Hammasi keldi"
            :disable="!roster.length"
            @click="markAllPresent"
          />
        </div>
      </div>
    </div>

    <div v-if="loadingRoster" class="brand-card q-pa-md">
      <q-skeleton
        v-for="i in 6"
        :key="i"
        type="text"
        height="40px"
        class="q-mb-sm"
      />
    </div>

    <div
      v-else-if="!selectedLessonId"
      class="brand-card q-pa-xl column flex-center muted-text"
    >
      <q-icon name="fact_check" size="48px" class="q-mb-sm" />
      <div class="text-subtitle2"
        >Davomatni ko'rish uchun sinf va darsni tanlang</div
      >
    </div>

    <div v-else class="brand-card overflow-hidden">
      <div class="row items-center justify-between live-count-bar">
        <div class="text-body1">
          <span class="text-weight-bold text-positive">{{ presentCount }}</span>
          keldi ·
          <span class="text-weight-bold text-negative">{{ absentCount }}</span>
          kelmadi ·
          <span class="text-weight-bold text-warning">{{ lateCount }}</span>
          kechikdi ·
          <span class="text-weight-bold">{{ excusedCount }}</span> sababli
        </div>
        <div class="text-caption muted-text gt-xs"
          >1=Keldi · 2=Kelmadi · 3=Kechikdi · 4=Sababli · ↑↓ o'tish</div
        >
      </div>
      <q-separator />
      <q-list separator>
        <transition-group name="roster-row">
          <q-item
            v-for="(entry, idx) in roster"
            :key="entry.studentId"
            :ref="el => (rowRefs[idx] = el)"
            class="roster-item"
            tabindex="0"
            @keydown="onRowKeydown($event, idx)"
          >
            <q-item-section avatar>
              <q-avatar
                size="36px"
                :style="{ background: avatarColor(entry.studentName) }"
                text-color="white"
              >
                {{ initials(entry.studentName) }}
              </q-avatar>
            </q-item-section>
            <q-item-section>
              <q-item-label class="text-weight-medium"
                >{{ idx + 1 }}. {{ entry.studentName }}</q-item-label
              >
            </q-item-section>
            <q-item-section side>
              <q-btn-toggle
                v-model="entry.status"
                dense
                no-caps
                spread
                toggle-color="primary"
                :options="statusOptions"
                class="status-toggle"
              />
            </q-item-section>
          </q-item>
        </transition-group>
      </q-list>

      <div class="row justify-end items-center q-pa-md">
        <q-btn
          color="primary"
          unelevated
          no-caps
          icon="save"
          label="Saqlash"
          :loading="saving"
          @click="saveAttendance"
        />
      </div>
    </div>
  </page-layout>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useQuasar } from 'quasar'
import { api } from '@/boot/axios'
import PageLayout from '@/components/PageLayout.vue'
import DateField from '@/components/DateField.vue'
import { useSchoolStore } from '@/stores/school'
import { useAuthStore } from '@/stores/auth'
import { todayStr } from '@/utils/date'

const $q = useQuasar()
const schoolStore = useSchoolStore()
const authStore = useAuthStore()
const myEmployeeId = computed(() => authStore.employeeId)
const onlyMyLessons = ref(true)

const classOptions = ref([])
const lessonOptions = ref([])
const selectedClassId = ref(null)
const selectedLessonId = ref(null)
const selectedDate = ref(todayStr())
const roster = ref([])
const loadingRoster = ref(false)
const saving = ref(false)
const todayLessons = ref([])
const rowRefs = ref([])

const statusOptions = [
  { label: 'Keldi', value: 'PRESENT' },
  { label: 'Kelmadi', value: 'ABSENT' },
  { label: 'Kechikdi', value: 'LATE' },
  { label: 'Sababli', value: 'EXCUSED' }
]

const statusByKey = {
  1: 'PRESENT',
  2: 'ABSENT',
  3: 'LATE',
  4: 'EXCUSED'
}

function onRowKeydown(evt, idx) {
  if (statusByKey[evt.key]) {
    roster.value[idx].status = statusByKey[evt.key]
    evt.preventDefault()
    return
  }
  if (evt.key === 'ArrowDown') {
    evt.preventDefault()
    focusRow(idx + 1)
  } else if (evt.key === 'ArrowUp') {
    evt.preventDefault()
    focusRow(idx - 1)
  }
}

function focusRow(idx) {
  const el = rowRefs.value[idx]
  const domEl = el?.$el || el
  domEl?.focus?.()
}

async function loadTodayLessons() {
  if (!schoolStore.activeSchoolId) return
  try {
    const params = { schoolId: schoolStore.activeSchoolId }
    if (myEmployeeId.value && onlyMyLessons.value) {
      params.employeeId = myEmployeeId.value
    }
    const res = await api.get('/api/attendance/today-lessons', { params })
    todayLessons.value = res.data
  } catch {
    todayLessons.value = []
  }
}

function selectTodayLesson(lesson) {
  selectedClassId.value = lesson.schoolClassId
  selectedLessonId.value = lesson.lessonSlotId
  onClassChange(true)
}

const presentCount = computed(
  () => roster.value.filter(r => r.status === 'PRESENT').length
)
const absentCount = computed(
  () => roster.value.filter(r => r.status === 'ABSENT').length
)
const lateCount = computed(
  () => roster.value.filter(r => r.status === 'LATE').length
)
const excusedCount = computed(
  () => roster.value.filter(r => r.status === 'EXCUSED').length
)

function initials(name) {
  return (name || '')
    .split(' ')
    .map(p => p.charAt(0))
    .join('')
    .slice(0, 2)
    .toUpperCase()
}

const palette = [
  '#4f46e5',
  '#0ea5e9',
  '#10b981',
  '#f59e0b',
  '#ec4899',
  '#8b5cf6',
  '#14b8a6'
]
function avatarColor(name) {
  let hash = 0
  for (const ch of name || '')
    hash = (hash * 31 + ch.charCodeAt(0)) % palette.length
  return palette[Math.abs(hash) % palette.length]
}

async function loadClasses() {
  if (!schoolStore.activeSchoolId) return
  try {
    const response = await api.get('/api/school-classes', {
      params: {
        schoolId: schoolStore.activeSchoolId,
        size: 100,
        sort: 'gradeNumber,asc'
      }
    })
    classOptions.value = response.data.content.map(c => ({
      value: c.id,
      label: `${c.gradeNumber}-${c.sectionLetter}`
    }))
  } catch {
    classOptions.value = []
  }
}

async function onClassChange(keepLesson) {
  const preserveLessonId = keepLesson ? selectedLessonId.value : null
  if (!keepLesson) selectedLessonId.value = null
  roster.value = []
  if (!selectedClassId.value) {
    lessonOptions.value = []
    return
  }
  try {
    const response = await api.get('/api/lesson-slots/timetable', {
      params: {
        schoolId: schoolStore.activeSchoolId,
        schoolClassId: selectedClassId.value
      }
    })
    lessonOptions.value = response.data
      .sort(
        (a, b) =>
          a.weekday.localeCompare(b.weekday) ||
          a.startTime.localeCompare(b.startTime)
      )
      .map(l => ({
        value: l.lessonSlotId,
        label: `${l.weekday} · ${l.startTime.slice(0, 5)}-${l.endTime.slice(0, 5)} · ${l.subjectName} (${l.teacherName})`
      }))
    if (preserveLessonId) {
      selectedLessonId.value = preserveLessonId
      loadRoster()
    }
  } catch {
    lessonOptions.value = []
  }
}

async function loadRoster() {
  if (!selectedLessonId.value || !selectedDate.value) {
    roster.value = []
    return
  }
  loadingRoster.value = true
  try {
    const response = await api.get('/api/attendance/roster', {
      params: { lessonSlotId: selectedLessonId.value, date: selectedDate.value }
    })
    roster.value = response.data.map(r => ({
      ...r,
      status: r.status || 'PRESENT'
    }))
  } catch (error) {
    $q.notify({
      type: 'negative',
      message: error.response?.data || 'Xatolik yuz berdi'
    })
  } finally {
    loadingRoster.value = false
  }
}

function markAllPresent() {
  roster.value = roster.value.map(r => ({ ...r, status: 'PRESENT' }))
}

async function saveAttendance() {
  saving.value = true
  try {
    await api.post('/api/attendance/bulk', {
      lessonSlotId: selectedLessonId.value,
      recordDate: selectedDate.value,
      entries: roster.value.map(r => ({
        studentId: r.studentId,
        status: r.status,
        comment: r.comment || null
      }))
    })
    $q.notify({
      type: 'positive',
      message: 'Davomat saqlandi',
      icon: 'check_circle'
    })
    loadTodayLessons()
  } catch (error) {
    $q.notify({
      type: 'negative',
      message: error.response?.data || 'Xatolik yuz berdi'
    })
  } finally {
    saving.value = false
  }
}

watch(
  () => schoolStore.activeSchoolId,
  () => {
    selectedClassId.value = null
    selectedLessonId.value = null
    roster.value = []
    loadClasses()
    loadTodayLessons()
  }
)

loadClasses()
loadTodayLessons()
</script>

<style scoped>
.muted-text {
  color: var(--brand-text-muted);
}

.today-lessons-scroll {
  display: flex;
  gap: 10px;
  overflow-x: auto;
  padding-bottom: 4px;
}

.today-lesson-card {
  flex: 0 0 auto;
  min-width: 140px;
  border: 1px solid var(--brand-border);
  border-radius: var(--radius-md);
  padding: 8px 10px;
  cursor: pointer;
  background: var(--card-bg);
  transition:
    border-color 0.15s ease,
    background 0.15s ease;
}

.today-lesson-card:hover {
  border-color: var(--q-primary);
}

.today-lesson-card--active {
  border-color: var(--q-primary);
  background: rgba(79, 70, 229, 0.08);
}

.today-lesson-card--live {
  border-color: var(--color-success);
}

.mark-all-btn {
  font-weight: 600;
}

.live-count-bar {
  padding: 12px 16px;
}

.roster-item {
  outline: none;
}

.roster-item:focus-visible {
  background: rgba(79, 70, 229, 0.08);
}

.status-toggle {
  border: 1px solid var(--brand-border);
}

.roster-row-enter-active {
  transition: all 0.2s ease;
}

.roster-row-enter-from {
  opacity: 0;
  transform: translateY(6px);
}

@media (prefers-reduced-motion: reduce) {
  .roster-row-enter-active {
    transition: none;
  }
}
</style>

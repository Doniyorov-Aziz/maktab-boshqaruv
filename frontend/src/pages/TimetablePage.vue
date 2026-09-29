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
      <div class="table-scroll">
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
            <tr v-for="slot in timeSlots" :key="slot">
              <td class="time-col">{{ slot }}</td>
              <td v-for="day in weekdays" :key="day" class="cell">
                <div
                  v-for="entry in cellEntries(day, slot)"
                  :key="entry.lessonSlotId"
                  class="lesson-chip"
                  :class="{ 'lesson-chip--live': isLive(day, entry) }"
                  :style="{ background: subjectColor(entry.subjectName) }"
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
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </page-layout>
</template>

<script setup>
import { ref, computed } from 'vue'
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

.timetable-grid {
  border-collapse: collapse;
  width: 100%;
  table-layout: fixed;
}

.time-col {
  width: 70px;
  font-size: 12px;
  color: var(--brand-text-muted);
  text-align: center;
  vertical-align: top;
  padding-top: 10px;
}

.day-col {
  width: 150px;
  text-align: center;
  padding: 10px 4px;
  font-weight: 700;
  border-bottom: 2px solid var(--brand-border);
}

.day-col--today {
  color: var(--q-primary);
}

.cell {
  border: 1px solid var(--brand-border);
  vertical-align: top;
  padding: 4px;
  height: 70px;
}

.lesson-chip {
  border-radius: 8px;
  padding: 4px 6px;
  color: white;
  font-size: 11px;
  margin-bottom: 3px;
  line-height: 1.3;
}

.lesson-chip--live {
  box-shadow:
    0 0 0 2px white,
    0 0 0 4px var(--q-primary);
}
</style>

<template>
  <page-layout
    icon="event_available"
    color="#22c55e"
    title="Tadbirlar taqvimi"
    :subtitle="schoolStore.activeSchoolName"
  >
    <template v-slot:actions>
      <q-btn-toggle
        v-model="viewMode"
        dense
        no-caps
        toggle-color="primary"
        :options="[
          { label: 'Oylik', value: 'month' },
          { label: 'Haftalik', value: 'week' }
        ]"
      />
      <q-btn flat round dense icon="chevron_left" @click="shift(-1)" />
      <q-btn flat no-caps dense label="Bugun" @click="goToday" />
      <div class="text-subtitle1 text-weight-medium range-label">{{
        rangeLabel
      }}</div>
      <q-btn flat round dense icon="chevron_right" @click="shift(1)" />
      <q-btn
        v-if="canCreate"
        color="primary"
        unelevated
        no-caps
        icon="add"
        label="Tadbir"
        @click="openCreateDialog"
      />
    </template>

    <div class="row items-center q-gutter-sm q-mb-md legend-row">
      <div
        v-for="t in eventTypes"
        :key="t.value"
        class="legend-chip"
        :class="{ 'legend-chip--off': !activeTypes.has(t.value) }"
        @click="toggleType(t.value)"
      >
        <span class="legend-dot" :style="{ background: t.color }" />
        {{ t.label }}
      </div>
    </div>

    <div v-if="loading" class="brand-card q-pa-md">
      <q-skeleton type="rect" height="400px" />
    </div>

    <div v-else class="brand-card q-pa-sm">
      <div
        class="calendar-grid"
        :class="{ 'calendar-grid--week': viewMode === 'week' }"
      >
        <div
          v-for="(d, i) in weekdayLabels"
          :key="d"
          class="day-header"
          :class="{ 'day-header--weekend': i === 6 }"
          >{{ d }}</div
        >
        <div
          v-for="(cell, i) in cells"
          :key="cell.dateStr"
          class="day-cell"
          :class="{
            'day-cell--muted': !cell.inRange,
            'day-cell--today': cell.isToday,
            'day-cell--weekend': i % 7 === 6
          }"
          @click="onCellClick(cell)"
        >
          <div class="row items-center justify-between no-wrap">
            <div class="day-number">{{ cell.day }}</div>
            <div v-if="cell.isToday" class="live-clock">{{ liveClock }}</div>
          </div>
          <div
            v-for="ev in cell.events.slice(0, 3)"
            :key="ev.id"
            class="event-chip"
            :style="{ background: typeColor(ev.type) }"
            @click.stop="openEditDialog(ev)"
          >
            {{ ev.title }}
          </div>
          <div
            v-if="cell.events.length > 3"
            class="event-more"
            @click.stop="openDayDetail(cell)"
          >
            +{{ cell.events.length - 3 }}
          </div>
        </div>
      </div>
    </div>

    <q-dialog v-model="dayDetailOpen">
      <q-card style="width: 100%; max-width: 420px; border-radius: 18px">
        <q-card-section class="row items-center q-pb-none">
          <div class="text-h6">{{
            dayDetailDate && formatDate(dayDetailDate)
          }}</div>
          <q-space />
          <q-btn flat round dense icon="close" v-close-popup />
        </q-card-section>
        <q-list separator class="q-mt-sm">
          <q-item
            v-for="ev in dayDetailEvents"
            :key="ev.id"
            clickable
            v-close-popup
            @click="openEditDialog(ev)"
          >
            <q-item-section avatar>
              <q-avatar
                :style="{ background: typeColor(ev.type) }"
                text-color="white"
                size="32px"
              >
                <q-icon name="event" size="16px" />
              </q-avatar>
            </q-item-section>
            <q-item-section>
              <q-item-label>{{ ev.title }}</q-item-label>
              <q-item-label caption>{{ typeLabel(ev.type) }}</q-item-label>
            </q-item-section>
          </q-item>
        </q-list>
      </q-card>
    </q-dialog>

    <q-dialog v-model="dialogOpen" persistent>
      <q-card style="width: 100%; max-width: 460px; border-radius: 18px">
        <q-card-section class="row items-center q-pb-none">
          <div class="text-h6">{{
            editingId ? 'Tadbirni tahrirlash' : 'Yangi tadbir'
          }}</div>
          <q-space />
          <q-btn flat round dense icon="close" v-close-popup />
        </q-card-section>
        <q-form @submit.prevent="saveEvent">
          <q-card-section class="q-gutter-md q-pt-md">
            <q-input
              v-model="form.title"
              label="Nomi"
              outlined
              dense
              :rules="[v => !!v || 'Majburiy']"
            />
            <q-input
              v-model="form.description"
              label="Tavsif"
              type="textarea"
              autogrow
              outlined
              dense
            />
            <q-select
              v-model="form.type"
              :options="[
                { value: 'HOLIDAY', label: 'Bayram' },
                { value: 'EXAM', label: 'Imtihon' },
                { value: 'PARENT_MEETING', label: 'Ota-onalar yig\'ilishi' },
                { value: 'VACATION', label: 'Ta\'til' },
                { value: 'OTHER', label: 'Boshqa' }
              ]"
              option-value="value"
              option-label="label"
              emit-value
              map-options
              outlined
              dense
              label="Turi"
            />
            <date-field
              v-model="form.startDate"
              label="Boshlanish"
              :rules="[v => !!v || 'Majburiy']"
            />
            <date-field
              v-model="form.endDate"
              label="Tugash"
              :rules="[v => !!v || 'Majburiy']"
            />
            <q-banner
              v-if="formError"
              class="bg-red-1 text-negative rounded-borders"
              dense
              >{{ formError }}</q-banner
            >
          </q-card-section>
          <q-card-actions align="right" class="q-pa-md">
            <q-btn
              v-if="editingId"
              flat
              no-caps
              color="negative"
              label="O'chirish"
              :loading="deleting"
              @click="deleteEvent"
            />
            <q-space />
            <q-btn
              flat
              label="Bekor qilish"
              no-caps
              color="grey-7"
              v-close-popup
            />
            <q-btn
              type="submit"
              color="primary"
              label="Saqlash"
              no-caps
              unelevated
              :loading="saving"
            />
          </q-card-actions>
        </q-form>
      </q-card>
    </q-dialog>
  </page-layout>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useQuasar } from 'quasar'
import { api } from '@/boot/axios'
import PageLayout from '@/components/PageLayout.vue'
import DateField from '@/components/DateField.vue'
import { useAuthStore } from '@/stores/auth'
import { useSchoolStore } from '@/stores/school'
import { toLocalDateStr, todayStr, monthName, formatDate } from '@/utils/date'

const $q = useQuasar()
const authStore = useAuthStore()
const schoolStore = useSchoolStore()

const canCreate = computed(() => authStore.isEditor)
const viewMode = ref('month')
const anchorDate = ref(new Date())
const events = ref([])
const loading = ref(false)
const dialogOpen = ref(false)
const editingId = ref(null)
const saving = ref(false)
const formError = ref('')
const form = ref({
  title: '',
  description: '',
  type: 'OTHER',
  startDate: '',
  endDate: ''
})

const dayDetailOpen = ref(false)
const dayDetailDate = ref('')
const dayDetailEvents = ref([])

const eventTypes = [
  { value: 'HOLIDAY', label: 'Bayram', color: '#f59e0b' },
  { value: 'EXAM', label: 'Imtihon', color: '#ef4444' },
  { value: 'PARENT_MEETING', label: "Yig'ilish", color: '#3b82f6' },
  { value: 'VACATION', label: "Ta'til", color: '#10b981' },
  { value: 'OTHER', label: 'Boshqa', color: '#8b5cf6' }
]
const activeTypes = ref(new Set(eventTypes.map(t => t.value)))
function toggleType(t) {
  const next = new Set(activeTypes.value)
  if (next.has(t)) next.delete(t)
  else next.add(t)
  activeTypes.value = next
}

const weekdayLabels = ['Dush', 'Sesh', 'Chor', 'Pay', 'Juma', 'Shan', 'Yak']

const rangeStart = computed(() => {
  const d = new Date(anchorDate.value)
  if (viewMode.value === 'month') {
    const first = new Date(d.getFullYear(), d.getMonth(), 1)
    const dow = (first.getDay() + 6) % 7
    first.setDate(first.getDate() - dow)
    return first
  }
  const dow = (d.getDay() + 6) % 7
  const monday = new Date(d)
  monday.setDate(d.getDate() - dow)
  return monday
})

const rangeEnd = computed(() => {
  const start = new Date(rangeStart.value)
  const days = viewMode.value === 'month' ? 41 : 6
  const end = new Date(start)
  end.setDate(start.getDate() + days)
  return end
})

const rangeLabel = computed(() => {
  const d = anchorDate.value
  return `${monthName(d.getMonth())} ${d.getFullYear()}`
})

const cells = computed(() => {
  const result = []
  const start = new Date(rangeStart.value)
  const end = new Date(rangeEnd.value)
  const today = todayStr()
  const currentMonth = anchorDate.value.getMonth()
  for (let d = new Date(start); d <= end; d.setDate(d.getDate() + 1)) {
    const dateStr = toLocalDateStr(d)
    result.push({
      dateStr,
      day: d.getDate(),
      inRange: viewMode.value === 'week' || d.getMonth() === currentMonth,
      isToday: dateStr === today,
      events: events.value.filter(
        ev =>
          dateStr >= ev.startDate &&
          dateStr <= ev.endDate &&
          activeTypes.value.has(ev.type)
      )
    })
  }
  return result
})

const typeColors = {
  HOLIDAY: '#f59e0b',
  EXAM: '#ef4444',
  PARENT_MEETING: '#3b82f6',
  VACATION: '#10b981',
  OTHER: '#8b5cf6'
}
function typeColor(t) {
  return typeColors[t] || '#8b5cf6'
}
function typeLabel(t) {
  return eventTypes.find(x => x.value === t)?.label || 'Boshqa'
}

function shift(dir) {
  const d = new Date(anchorDate.value)
  if (viewMode.value === 'month') d.setMonth(d.getMonth() + dir)
  else d.setDate(d.getDate() + dir * 7)
  anchorDate.value = d
}

function goToday() {
  anchorDate.value = new Date()
}

function openDayDetail(cell) {
  dayDetailDate.value = cell.dateStr
  dayDetailEvents.value = cell.events
  dayDetailOpen.value = true
}

function onCellClick(cell) {
  if (!canCreate.value || cell.events.length) return
  editingId.value = null
  formError.value = ''
  form.value = {
    title: '',
    description: '',
    type: 'OTHER',
    startDate: cell.dateStr,
    endDate: cell.dateStr
  }
  dialogOpen.value = true
}

async function loadEvents() {
  if (!schoolStore.activeSchoolId) return
  loading.value = true
  try {
    const response = await api.get('/api/calendar-events/range', {
      params: {
        schoolId: schoolStore.activeSchoolId,
        from: toLocalDateStr(rangeStart.value),
        to: toLocalDateStr(rangeEnd.value)
      }
    })
    events.value = response.data
  } finally {
    loading.value = false
  }
}

function openCreateDialog() {
  editingId.value = null
  formError.value = ''
  form.value = {
    title: '',
    description: '',
    type: 'OTHER',
    startDate: toLocalDateStr(new Date()),
    endDate: toLocalDateStr(new Date())
  }
  dialogOpen.value = true
}

function openEditDialog(ev) {
  if (!canCreate.value) return
  editingId.value = ev.id
  formError.value = ''
  form.value = {
    title: ev.title,
    description: ev.description || '',
    type: ev.type,
    startDate: ev.startDate,
    endDate: ev.endDate
  }
  dialogOpen.value = true
}

async function saveEvent() {
  if (saving.value) return
  saving.value = true
  formError.value = ''
  try {
    const payload = { ...form.value, schoolId: schoolStore.activeSchoolId }
    if (editingId.value) {
      await api.put(`/api/calendar-events/${editingId.value}`, payload)
    } else {
      await api.post('/api/calendar-events', payload)
    }
    dialogOpen.value = false
    $q.notify({ type: 'positive', message: 'Saqlandi', icon: 'check_circle' })
    loadEvents()
  } catch (error) {
    formError.value = error.response?.data || 'Xatolik yuz berdi'
  } finally {
    saving.value = false
  }
}

const deleting = ref(false)

async function deleteEvent() {
  if (deleting.value) return // a second click while the first is on its way
  deleting.value = true
  try {
    await api.delete(`/api/calendar-events/${editingId.value}`)
    dialogOpen.value = false
    $q.notify({ type: 'positive', message: "O'chirildi", icon: 'check_circle' })
    loadEvents()
  } catch (error) {
    $q.notify({
      type: 'negative',
      message: error.response?.data || 'Xatolik yuz berdi'
    })
  } finally {
    deleting.value = false
  }
}

watch([viewMode, anchorDate], loadEvents)
watch(() => schoolStore.activeSchoolId, loadEvents)
loadEvents()

const liveClock = ref('')
let clockTimer = null
function tickClock() {
  liveClock.value = new Date().toTimeString().slice(0, 5)
}
onMounted(() => {
  tickClock()
  clockTimer = setInterval(tickClock, 30000)
})
onBeforeUnmount(() => {
  if (clockTimer) clearInterval(clockTimer)
})
</script>

<style scoped>
.muted-text {
  color: var(--brand-text-muted);
}

.range-label {
  min-width: 130px;
  text-align: center;
}

.legend-row {
  flex-wrap: wrap;
}

.legend-chip {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 999px;
  border: 1px solid var(--brand-border);
  cursor: pointer;
  user-select: none;
  transition: opacity 0.15s ease;
}

.legend-chip--off {
  opacity: 0.4;
}

.legend-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
}

.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 4px;
  min-width: 0;
}

.day-header {
  text-align: center;
  font-weight: 700;
  font-size: 12px;
  color: var(--brand-text-muted);
  padding: 6px 0;
}

.day-header--weekend {
  color: var(--color-danger);
}

.day-cell {
  min-height: 90px;
  min-width: 0;
  border: 1px solid var(--brand-border);
  border-radius: 8px;
  padding: 4px;
  cursor: pointer;
  overflow: hidden;
}

.calendar-grid--week .day-cell {
  min-height: 140px;
}

.day-cell--muted {
  opacity: 0.4;
}

.day-cell--weekend {
  background: rgba(239, 68, 68, 0.04);
}

.day-cell--today {
  border-color: var(--q-primary);
  box-shadow: inset 0 0 0 1px var(--q-primary);
}

.day-number {
  font-size: 12px;
  color: var(--brand-text-muted);
  margin-bottom: 3px;
}

.live-clock {
  font-size: 10px;
  font-weight: 700;
  color: white;
  background: var(--q-primary);
  padding: 1px 5px;
  border-radius: 4px;
  margin-bottom: 3px;
  font-variant-numeric: tabular-nums;
}

.event-chip {
  font-size: 11px;
  color: white;
  border-radius: 6px;
  padding: 2px 5px;
  margin-bottom: 2px;
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.event-more {
  font-size: 11px;
  color: var(--brand-text-muted);
  font-weight: 600;
  cursor: pointer;
  padding: 1px 5px;
}

.event-more:hover {
  color: var(--q-primary);
}

@media (max-width: 599px) {
  .day-cell {
    min-height: 60px;
  }

  .day-header {
    font-size: 10px;
  }
}
</style>

<template>
  <page-layout
    icon="handshake"
    color="#ec4899"
    title="Ota-onalar bilan uchrashuvlar"
    subtitle="O'qituvchi bo'sh vaqtlarini belgilaydi — ota-onalar botdagi «➕ Yana → 📅 Uchrashuv» bo'limida yoziladi"
  >
    <template #actions>
      <AppButton
        v-if="authStore.isEditor && (authStore.isAdmin || authStore.employeeId)"
        icon="add"
        label="Bo'sh vaqt qo'shish"
        @click="openBulk"
      />
    </template>

    <div class="row q-col-gutter-sm items-center">
      <div
        class="col-12 col-sm-5 col-md-4"
        v-if="authStore.isAdmin || !authStore.employeeId"
      >
        <AppSelect
          v-model="employeeId"
          :options="employees"
          label="O'qituvchi"
          emit-value
          map-options
          clearable
          use-input
          input-debounce="0"
          @filter="filterEmployees"
          @update:model-value="load"
        />
      </div>
      <div class="col-auto">
        <q-btn-toggle
          v-model="range"
          no-caps
          unelevated
          rounded
          toggle-color="primary"
          :options="[
            { label: '2 hafta', value: 14 },
            { label: '1 oy', value: 31 }
          ]"
          @update:model-value="load"
        />
      </div>
      <div class="col-auto">
        <q-toggle v-model="bookedOnly" label="Faqat band qilinganlar" />
      </div>
    </div>

    <div v-if="loading" class="row justify-center q-pa-lg">
      <q-spinner size="32px" color="primary" />
    </div>
    <div
      v-else-if="!days.length"
      class="brand-card column flex-center q-py-xl muted-text"
    >
      <q-icon name="event_busy" size="48px" class="q-mb-sm" />
      <div class="text-subtitle2">Uchrashuv vaqtlari yo'q</div>
      <div class="text-caption">
        «Bo'sh vaqt qo'shish» tugmasi bilan qabul vaqtlarini belgilang
      </div>
    </div>

    <div v-for="d in days" :key="d.date" class="day-group">
      <div class="text-weight-bold">
        {{ weekdayName(parseLocalDate(d.date)) }}, {{ formatDate(d.date) }}
        <span class="text-caption muted-text q-ml-sm"
          >{{ d.items.filter(s => s.booked).length }} band /
          {{ d.items.length }}</span
        >
      </div>
      <div class="row q-col-gutter-sm">
        <div
          v-for="s in d.items"
          :key="s.id"
          class="col-12 col-sm-6 col-md-4 col-lg-3"
        >
          <div class="brand-card slot" :class="{ 'slot--booked': s.booked }">
            <div class="row items-center no-wrap">
              <div class="text-weight-bold tabular-nums">
                {{ hm(s.startTime) }}–{{ hm(s.endTime) }}
              </div>
              <q-space />
              <q-badge
                :color="s.booked ? 'pink' : 'grey-5'"
                :label="s.booked ? 'Band' : 'Bo\'sh'"
              />
            </div>
            <div class="text-caption muted-text">
              {{ s.employeeName }}<span v-if="s.place"> · {{ s.place }}</span>
            </div>
            <template v-if="s.booked">
              <div class="q-mt-xs">
                <router-link
                  v-if="s.studentId"
                  :to="`/profiles/student/${s.studentId}`"
                  class="text-weight-medium link"
                  >{{ s.studentName }}</router-link
                >
                <span class="text-caption muted-text">
                  · {{ s.className }}</span
                >
              </div>
              <div class="text-caption">
                {{ s.parentName || 'Ota-ona' }}
                <span v-if="s.parentPhone"> · {{ s.parentPhone }}</span>
              </div>
              <div v-if="s.topic" class="text-caption topic">
                «{{ s.topic }}»
              </div>
            </template>
            <div class="row q-mt-sm" v-if="canEdit(s)">
              <AppButton
                v-if="s.booked"
                variant="ghost"
                size="sm"
                icon="event_busy"
                label="Bekor qilish"
                color="negative"
                @click="cancel(s)"
              />
              <q-space />
              <AppButton
                variant="ghost"
                size="sm"
                icon="delete"
                aria-label="O'chirish"
                @click="remove(s)"
              />
            </div>
          </div>
        </div>
      </div>
    </div>

    <q-dialog v-model="bulkOpen" persistent>
      <q-card class="dialog-card" style="width: 520px; max-width: 95vw">
        <q-card-section class="row items-center q-pb-none">
          <div class="text-h6">Bo'sh vaqt qo'shish</div>
          <q-space />
          <AppButton variant="ghost" icon="close" v-close-popup />
        </q-card-section>
        <q-card-section class="q-gutter-y-sm">
          <AppSelect
            v-if="authStore.isAdmin"
            v-model="bulk.employeeId"
            :options="employees"
            label="O'qituvchi *"
            emit-value
            map-options
            use-input
            input-debounce="0"
            @filter="filterEmployees"
          />
          <AppDateInput
            v-model="bulk.date"
            label="Kun *"
            no-sundays
            :min="today"
          />
          <div class="row q-col-gutter-sm">
            <div class="col-6">
              <AppTimeInput v-model="bulk.from" label="Boshlanish *" />
            </div>
            <div class="col-6">
              <AppTimeInput v-model="bulk.to" label="Tugash *" />
            </div>
          </div>
          <div class="row q-col-gutter-sm">
            <div class="col-6">
              <AppSelect
                v-model="bulk.minutes"
                :options="
                  [10, 15, 20, 30, 45, 60].map(v => ({
                    label: `${v} daqiqa`,
                    value: v
                  }))
                "
                label="Har bir uchrashuv"
                emit-value
                map-options
              />
            </div>
            <div class="col-6">
              <AppSelect
                v-model="bulk.gapMinutes"
                :options="
                  [0, 5, 10, 15].map(v => ({
                    label: v ? `${v} daqiqa` : 'Tanaffussiz',
                    value: v
                  }))
                "
                label="Oraliq"
                emit-value
                map-options
              />
            </div>
          </div>
          <AppInput v-model="bulk.place" label="Joyi (xona)" maxlength="80" />
          <AppSelect
            v-model="bulk.weeks"
            :options="
              [1, 2, 3, 4, 6, 8].map(v => ({
                label:
                  v === 1 ? 'Faqat shu kuni' : `${v} hafta davomida har hafta`,
                value: v
              }))
            "
            label="Takrorlash"
            emit-value
            map-options
          />
          <div class="text-caption muted-text" v-if="preview">
            {{ preview }}
          </div>
        </q-card-section>
        <q-card-actions align="right" class="q-px-md q-pb-md">
          <AppButton variant="ghost" label="Bekor qilish" v-close-popup />
          <AppButton
            label="Qo'shish"
            :loading="saving"
            :disable="!bulk.employeeId || !bulk.date || !bulk.from || !bulk.to"
            @click="saveBulk"
          />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </page-layout>
</template>

<script setup>
import AppButton from '@/components/app/AppButton.vue'
import AppInput from '@/components/app/AppInput.vue'
import AppSelect from '@/components/app/AppSelect.vue'
import AppDateInput from '@/components/app/AppDateInput.vue'
import AppTimeInput from '@/components/app/AppTimeInput.vue'
import PageLayout from '@/components/PageLayout.vue'
import { computed, onMounted, ref, watch } from 'vue'
import { useQuasar } from 'quasar'
import { api } from '@/boot/axios'
import { useSchoolStore } from '@/stores/school'
import { useAuthStore } from '@/stores/auth'
import {
  formatDate,
  parseLocalDate,
  toLocalDateStr,
  todayStr,
  weekdayName
} from '@/utils/date'
import { errorText, hm, loadEmployees } from '@/utils/modules'

const $q = useQuasar()
const schoolStore = useSchoolStore()
const authStore = useAuthStore()
const today = todayStr()

const employeeId = ref(authStore.isAdmin ? null : authStore.employeeId)
const employees = ref([])
const allEmployees = ref([])
const range = ref(14)
const bookedOnly = ref(false)
const slots = ref([])
const loading = ref(false)
const bulkOpen = ref(false)
const saving = ref(false)
const bulk = ref({})

const days = computed(() => {
  const map = new Map()
  for (const s of slots.value) {
    if (bookedOnly.value && !s.booked) continue
    if (!map.has(s.date)) map.set(s.date, [])
    map.get(s.date).push(s)
  }
  return [...map.entries()].map(([date, items]) => ({ date, items }))
})

const preview = computed(() => {
  const b = bulk.value
  if (!b.from || !b.to || !b.minutes) return ''
  const [fh, fm] = b.from.split(':').map(Number)
  const [th, tm] = b.to.split(':').map(Number)
  const total = th * 60 + tm - (fh * 60 + fm)
  if (total <= 0) return "Tugash vaqti boshlanishdan keyin bo'lsin"
  const n = Math.floor(
    (total + (b.gapMinutes || 0)) / (b.minutes + (b.gapMinutes || 0))
  )
  return `${n} ta uchrashuv vaqti${b.weeks > 1 ? ` × ${b.weeks} hafta` : ''}`
})

function canEdit(s) {
  return (
    authStore.isEditor &&
    (authStore.isAdmin || authStore.employeeId === s.employeeId)
  )
}

function filterEmployees(val, update) {
  update(() => {
    const q = val.toLowerCase()
    employees.value = allEmployees.value.filter(e =>
      e.label.toLowerCase().includes(q)
    )
  })
}

async function loadRefs() {
  if (!schoolStore.activeSchoolId) return
  allEmployees.value = await loadEmployees(schoolStore.activeSchoolId)
  employees.value = allEmployees.value
}

async function load() {
  if (!schoolStore.activeSchoolId) return
  loading.value = true
  try {
    const to = parseLocalDate(today)
    to.setDate(to.getDate() + range.value)
    slots.value = (
      await api.get('/api/meeting-slots', {
        params: {
          schoolId: schoolStore.activeSchoolId,
          employeeId: employeeId.value || undefined,
          from: today,
          to: toLocalDateStr(to)
        }
      })
    ).data
  } catch (e) {
    $q.notify({ type: 'negative', message: errorText(e) })
  } finally {
    loading.value = false
  }
}

function openBulk() {
  bulk.value = {
    employeeId: authStore.isAdmin ? employeeId.value : authStore.employeeId,
    date: '',
    from: '15:00',
    to: '17:00',
    minutes: 15,
    gapMinutes: 0,
    place: '',
    weeks: 1
  }
  bulkOpen.value = true
}

async function saveBulk() {
  saving.value = true
  try {
    const res = await api.post('/api/meeting-slots/bulk', {
      ...bulk.value,
      place: bulk.value.place || null
    })
    $q.notify({
      type: 'positive',
      message: `${res.data.length} ta bo'sh vaqt qo'shildi`
    })
    bulkOpen.value = false
    load()
  } catch (e) {
    $q.notify({ type: 'negative', message: errorText(e) })
  } finally {
    saving.value = false
  }
}

function cancel(s) {
  $q.dialog({
    title: 'Uchrashuvni bekor qilish',
    message: `${s.studentName} ota-onasiga botda xabar boradi, vaqt yana bo'sh bo'ladi.`,
    cancel: { flat: true, label: 'Yopish', noCaps: true },
    ok: {
      label: 'Bekor qilish',
      color: 'negative',
      unelevated: true,
      noCaps: true
    }
  }).onOk(async () => {
    try {
      await api.post(`/api/meeting-slots/${s.id}/cancel`)
      load()
    } catch (e) {
      $q.notify({ type: 'negative', message: errorText(e) })
    }
  })
}

function remove(s) {
  $q.dialog({
    title: "Vaqtni o'chirish",
    message: s.booked
      ? "Bu vaqt band — o'chirilsa, ota-onaga bekor qilingani haqida xabar boradi."
      : `${formatDate(s.date)} ${hm(s.startTime)} o'chirilsinmi?`,
    cancel: { flat: true, label: 'Bekor qilish', noCaps: true },
    ok: {
      label: "O'chirish",
      color: 'negative',
      unelevated: true,
      noCaps: true
    }
  }).onOk(async () => {
    try {
      await api.delete(`/api/meeting-slots/${s.id}`)
      load()
    } catch (e) {
      $q.notify({ type: 'negative', message: errorText(e) })
    }
  })
}

watch(
  () => schoolStore.activeSchoolId,
  () => {
    loadRefs()
    load()
  }
)
onMounted(() => {
  loadRefs()
  load()
})
</script>

<style scoped>
.day-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.slot {
  padding: 10px 12px;
  height: 100%;
}
.slot--booked {
  border-left: 3px solid #ec4899;
}
.topic {
  font-style: italic;
}
.link {
  color: inherit;
  text-decoration: none;
}
</style>

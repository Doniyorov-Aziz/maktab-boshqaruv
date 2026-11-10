<template>
  <page-layout
    icon="quiz"
    color="#ef4444"
    title="Imtihonlar va nazorat ishlari"
    subtitle="Ota-onalarga bir kun oldin botda eslatma boradi; natijalar jurnalga «Imtihon» bahosi bo'lib yoziladi"
  >
    <template #actions>
      <q-btn-toggle
        v-model="period"
        no-caps
        unelevated
        rounded
        toggle-color="primary"
        :options="[
          { label: 'Oldinda', value: 'ahead' },
          { label: 'O‘tgan', value: 'past' },
          { label: 'Hammasi', value: 'all' }
        ]"
        @update:model-value="load"
      />
      <AppButton
        v-if="authStore.isEditor"
        icon="add"
        label="Rejalashtirish"
        @click="openCreate"
      />
    </template>

    <div class="row q-col-gutter-sm">
      <div class="col-12 col-sm-4 col-md-3">
        <AppSelect
          v-model="classId"
          :options="classes"
          label="Sinf"
          emit-value
          map-options
          clearable
          @update:model-value="load"
        />
      </div>
      <div class="col-12 col-sm-4 col-md-3">
        <AppSelect
          v-model="subjectId"
          :options="subjects"
          label="Fan"
          emit-value
          map-options
          clearable
          @update:model-value="load"
        />
      </div>
    </div>

    <div v-if="loading" class="row justify-center q-pa-lg">
      <q-spinner size="32px" color="primary" />
    </div>
    <div
      v-else-if="!groups.length"
      class="brand-card column flex-center q-py-xl muted-text"
    >
      <q-icon name="event_available" size="48px" class="q-mb-sm" />
      <div class="text-subtitle2">Imtihonlar yo'q</div>
    </div>
    <div v-for="g in groups" :key="g.date" class="day-group">
      <div class="day-title text-weight-bold">
        {{ dayTitle(g.date) }}
        <span class="text-caption muted-text q-ml-sm">{{
          formatDate(g.date)
        }}</span>
      </div>
      <div class="row q-col-gutter-sm">
        <div v-for="x in g.items" :key="x.id" class="col-12 col-md-6 col-lg-4">
          <div class="brand-card exam-card">
            <div class="row items-start no-wrap q-gutter-x-sm">
              <q-avatar
                size="38px"
                :style="{
                  background: `${examKinds[x.kind].color}1f`,
                  color: examKinds[x.kind].color
                }"
              >
                <q-icon :name="examKinds[x.kind].icon" size="20px" />
              </q-avatar>
              <div class="col">
                <div class="text-weight-bold">
                  {{ x.subjectName }} · {{ x.className }}
                </div>
                <div class="text-caption">
                  {{ examKinds[x.kind].label
                  }}<span v-if="x.title"> — {{ x.title }}</span>
                </div>
                <div class="text-caption muted-text">
                  <span v-if="x.startTime"
                    ><q-icon name="schedule" /> {{ hm(x.startTime) }}</span
                  >
                  <span v-if="x.place" class="q-ml-sm"
                    ><q-icon name="place" /> {{ x.place }}</span
                  >
                  <span v-if="x.employeeName" class="q-ml-sm"
                    ><q-icon name="person" /> {{ x.employeeName }}</span
                  >
                </div>
                <div class="q-mt-xs">
                  <q-badge
                    v-if="!x.notifyParents"
                    outline
                    color="grey"
                    label="Eslatma yo'q"
                  />
                </div>
              </div>
            </div>
            <div class="row items-center q-mt-sm q-gutter-x-xs">
              <AppButton
                variant="secondary"
                size="sm"
                icon="grading"
                :label="
                  x.examDate <= today ? 'Natijalar' : 'Natijalar (kun kelganda)'
                "
                :disable="x.examDate > today"
                @click="openResults(x)"
              />
              <q-space />
              <template v-if="authStore.isEditor">
                <AppButton
                  variant="ghost"
                  size="sm"
                  icon="edit"
                  aria-label="Tahrirlash"
                  @click="openEdit(x)"
                />
                <AppButton
                  variant="ghost"
                  size="sm"
                  icon="delete"
                  color="negative"
                  aria-label="O'chirish"
                  @click="remove(x)"
                />
              </template>
            </div>
          </div>
        </div>
      </div>
    </div>

    <q-dialog v-model="dialogOpen" persistent>
      <q-card class="dialog-card" style="width: 560px; max-width: 95vw">
        <q-card-section class="row items-center q-pb-none">
          <div class="text-h6">
            {{
              form.id ? 'Tahrirlash' : 'Imtihon / nazorat ishini rejalashtirish'
            }}
          </div>
          <q-space />
          <AppButton variant="ghost" icon="close" v-close-popup />
        </q-card-section>
        <q-card-section class="q-gutter-y-sm">
          <div class="row q-col-gutter-sm">
            <div class="col-6">
              <AppSelect
                v-model="form.schoolClassId"
                :options="classes"
                label="Sinf *"
                emit-value
                map-options
              />
            </div>
            <div class="col-6">
              <AppSelect
                v-model="form.subjectId"
                :options="subjects"
                label="Fan *"
                emit-value
                map-options
              />
            </div>
          </div>
          <AppSelect
            v-model="form.kind"
            :options="toOptions(examKinds)"
            label="Turi *"
            emit-value
            map-options
          />
          <AppInput
            v-model="form.title"
            label="Nomi (ixtiyoriy)"
            hint="Masalan: «1-chorak yakuniy nazorat ishi»"
            maxlength="200"
          />
          <div class="row q-col-gutter-sm">
            <div class="col-6">
              <AppDateInput v-model="form.examDate" label="Sana *" no-sundays />
            </div>
            <div class="col-6">
              <AppTimeInput
                v-model="form.startTime"
                label="Vaqti"
                :rules="[]"
              />
            </div>
          </div>
          <AppInput v-model="form.place" label="Joyi (xona)" maxlength="60" />
          <AppInput
            v-model="form.description"
            type="textarea"
            autogrow
            label="Izoh (mavzular, kerakli narsalar)"
            maxlength="2000"
          />
          <q-checkbox
            v-model="form.notifyParents"
            label="Ota-onalarga bir kun oldin eslatma yuborish"
          />
          <q-banner
            v-if="sameDay.length"
            dense
            rounded
            class="text-warning q-mt-sm"
          >
            <template #avatar
              ><q-icon name="warning" color="warning"
            /></template>
            Shu kuni bu sinfda yana: {{ sameDay.join(', ') }}
          </q-banner>
        </q-card-section>
        <q-card-actions align="right" class="q-px-md q-pb-md">
          <AppButton variant="ghost" label="Bekor qilish" v-close-popup />
          <AppButton
            label="Saqlash"
            :loading="saving"
            :disable="
              !form.schoolClassId ||
              !form.subjectId ||
              !form.kind ||
              !form.examDate
            "
            @click="save"
          />
        </q-card-actions>
      </q-card>
    </q-dialog>

    <q-dialog v-model="resultsOpen" persistent>
      <q-card class="dialog-card" style="width: 640px; max-width: 96vw">
        <q-card-section class="row items-center q-pb-none" v-if="results">
          <div>
            <div class="text-h6">Natijalar</div>
            <div class="text-caption muted-text">
              {{ results.exam.subjectName }} · {{ results.exam.className }} ·
              {{ examKinds[results.exam.kind].label }} ·
              {{ formatDate(results.exam.examDate) }}
            </div>
          </div>
          <q-space />
          <AppButton variant="ghost" icon="close" v-close-popup />
        </q-card-section>
        <q-card-section v-if="results" class="results-list">
          <div
            v-for="r in results.rows"
            :key="r.studentId"
            class="row items-center no-wrap result-row"
          >
            <div class="col ellipsis">{{ r.name }}</div>
            <div class="row no-wrap q-gutter-x-xs">
              <AppButton
                v-for="n in [1, 2, 3, 4, 5]"
                :key="n"
                size="sm"
                :variant="scores[r.studentId] === n ? 'primary' : 'ghost'"
                :label="n"
                :disable="!authStore.isEditor"
                class="score-btn"
                @click="
                  scores[r.studentId] = scores[r.studentId] === n ? null : n
                "
              />
            </div>
          </div>
        </q-card-section>
        <q-card-actions align="between" class="q-px-md q-pb-md" v-if="results">
          <div class="text-caption muted-text">
            {{ filled }} / {{ results.rows.length }} ta natija
            <span v-if="average"> · o'rtacha {{ average }}</span>
          </div>
          <div>
            <AppButton variant="ghost" label="Yopish" v-close-popup />
            <AppButton
              v-if="authStore.isEditor"
              label="Saqlash"
              :loading="saving"
              @click="saveResults"
            />
          </div>
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
  relativeDay,
  todayStr,
  weekdayName,
  parseLocalDate
} from '@/utils/date'
import {
  errorText,
  examKinds,
  hm,
  loadClasses,
  loadSubjects,
  toOptions
} from '@/utils/modules'

const $q = useQuasar()
const schoolStore = useSchoolStore()
const authStore = useAuthStore()
const today = todayStr()

const period = ref('ahead')
const classId = ref(null)
const subjectId = ref(null)
const classes = ref([])
const subjects = ref([])
const exams = ref([])
const loading = ref(false)
const dialogOpen = ref(false)
const saving = ref(false)
const form = ref({})
const sameDay = ref([])
const resultsOpen = ref(false)
const results = ref(null)
const scores = ref({})

const groups = computed(() => {
  const map = new Map()
  for (const x of exams.value) {
    if (!map.has(x.examDate)) map.set(x.examDate, [])
    map.get(x.examDate).push(x)
  }
  return [...map.entries()].map(([date, items]) => ({ date, items }))
})

const filled = computed(
  () => Object.values(scores.value).filter(v => v != null).length
)
const average = computed(() => {
  const v = Object.values(scores.value).filter(x => x != null)
  return v.length ? (v.reduce((a, b) => a + b, 0) / v.length).toFixed(2) : null
})

function dayTitle(d) {
  return `${weekdayName(parseLocalDate(d))} · ${relativeDay(d)}`
}

async function loadRefs() {
  const id = schoolStore.activeSchoolId
  if (!id) return
  const [c, s] = await Promise.all([loadClasses(id), loadSubjects(id)])
  classes.value = c
  subjects.value = s
}

async function load() {
  if (!schoolStore.activeSchoolId) return
  loading.value = true
  try {
    const params = {
      schoolId: schoolStore.activeSchoolId,
      schoolClassId: classId.value || undefined,
      subjectId: subjectId.value || undefined,
      size: 300,
      sort:
        period.value === 'past'
          ? ['examDate,desc', 'id,desc']
          : ['examDate,asc', 'startTime,asc']
    }
    if (period.value === 'ahead') params.from = today
    if (period.value === 'past') params.to = today
    const res = await api.get('/api/exams', {
      params,
      paramsSerializer: { indexes: null }
    })
    exams.value = res.data.content
  } catch (e) {
    $q.notify({ type: 'negative', message: errorText(e) })
  } finally {
    loading.value = false
  }
}

function openCreate() {
  form.value = {
    schoolClassId: classId.value,
    subjectId: subjectId.value,
    kind: 'CONTROL',
    title: '',
    examDate: '',
    startTime: '',
    place: '',
    description: '',
    notifyParents: true
  }
  sameDay.value = []
  dialogOpen.value = true
}

function openEdit(x) {
  form.value = { ...x, startTime: hm(x.startTime) }
  sameDay.value = x.sameDay || []
  dialogOpen.value = true
}

async function save() {
  saving.value = true
  try {
    const body = {
      schoolClassId: form.value.schoolClassId,
      subjectId: form.value.subjectId,
      employeeId: form.value.employeeId || null,
      kind: form.value.kind,
      title: form.value.title || null,
      examDate: form.value.examDate,
      startTime: form.value.startTime || null,
      place: form.value.place || null,
      description: form.value.description || null,
      notifyParents: form.value.notifyParents
    }
    const res = form.value.id
      ? await api.put(`/api/exams/${form.value.id}`, body)
      : await api.post('/api/exams', body)
    if (res.data.sameDay?.length) {
      $q.notify({
        type: 'warning',
        message: `Diqqat: shu kuni bu sinfda yana ${res.data.sameDay.join(', ')}`
      })
    } else {
      $q.notify({ type: 'positive', message: 'Saqlandi' })
    }
    dialogOpen.value = false
    load()
  } catch (e) {
    $q.notify({ type: 'negative', message: errorText(e) })
  } finally {
    saving.value = false
  }
}

function remove(x) {
  $q.dialog({
    title: "O'chirish",
    message: `${x.subjectName} (${x.className}) — ${formatDate(x.examDate)} o'chirilsinmi? Kiritilgan natijalar jurnalda qoladi.`,
    cancel: { flat: true, label: 'Bekor qilish', noCaps: true },
    ok: {
      label: "O'chirish",
      color: 'negative',
      unelevated: true,
      noCaps: true
    }
  }).onOk(async () => {
    try {
      await api.delete(`/api/exams/${x.id}`)
      load()
    } catch (e) {
      $q.notify({ type: 'negative', message: errorText(e) })
    }
  })
}

async function openResults(x) {
  try {
    results.value = (await api.get(`/api/exams/${x.id}/results`)).data
    const s = {}
    for (const r of results.value.rows) s[r.studentId] = r.score
    scores.value = s
    resultsOpen.value = true
  } catch (e) {
    $q.notify({ type: 'negative', message: errorText(e) })
  }
}

async function saveResults() {
  saving.value = true
  try {
    const body = results.value.rows.map(r => ({
      studentId: r.studentId,
      score: scores.value[r.studentId] ?? null
    }))
    results.value = (
      await api.put(`/api/exams/${results.value.exam.id}/results`, body)
    ).data
    $q.notify({ type: 'positive', message: 'Natijalar saqlandi' })
    resultsOpen.value = false
  } catch (e) {
    $q.notify({ type: 'negative', message: errorText(e) })
  } finally {
    saving.value = false
  }
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
.day-title {
  font-size: 15px;
}
.exam-card {
  padding: 12px 14px;
  height: 100%;
}
.results-list {
  max-height: 60vh;
  overflow: auto;
}
.result-row {
  padding: 4px 0;
  border-bottom: 1px solid var(--brand-border);
}
.score-btn {
  min-width: 34px;
}
</style>

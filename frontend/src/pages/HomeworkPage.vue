<template>
  <page-layout
    icon="assignment"
    color="#0ea5e9"
    title="Uy vazifalari"
    :subtitle="`Berilgan vazifalar ota-onalar botida «📝 Uy vazifasi» bo'limida ko'rinadi · ${schoolStore.activeSchoolName || ''}`"
  >
    <template #actions>
      <q-btn-toggle
        v-model="period"
        no-caps
        unelevated
        rounded
        toggle-color="primary"
        :options="[
          { label: 'Joriy', value: 'current' },
          { label: 'Hammasi', value: 'all' }
        ]"
        @update:model-value="reload"
      />
      <AppButton
        v-if="authStore.isEditor"
        icon="add"
        label="Vazifa berish"
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
          @update:model-value="reload"
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
          @update:model-value="reload"
        />
      </div>
      <div class="col-12 col-sm-4 col-md-3" v-if="authStore.employeeId">
        <q-toggle
          v-model="mineOnly"
          label="Faqat men bergan"
          @update:model-value="reload"
        />
      </div>
    </div>

    <div class="brand-card overflow-hidden">
      <q-table
        :rows="rows"
        :columns="columns"
        row-key="id"
        :loading="loading"
        v-model:pagination="pagination"
        @request="onRequest"
        flat
        class="brand-table"
        :rows-per-page-options="[10, 20, 50]"
      >
        <template #no-data>
          <div class="full-width column flex-center q-py-xl muted-text">
            <q-icon name="assignment_turned_in" size="48px" class="q-mb-sm" />
            <div class="text-subtitle2">Vazifalar yo'q</div>
          </div>
        </template>
        <template #body-cell-task="props">
          <q-td :props="props" class="task-cell">
            <div class="row items-center no-wrap q-gutter-x-sm">
              <span
                class="subject-dot"
                :style="{
                  background: subjectColor(
                    props.row.subjectId,
                    props.row.subjectColor
                  )
                }"
              />
              <div>
                <div class="text-weight-medium">{{ props.row.title }}</div>
                <div class="text-caption muted-text">
                  {{ props.row.subjectName }} · {{ props.row.className }} sinf
                  <span v-if="props.row.employeeName">
                    · {{ props.row.employeeName }}</span
                  >
                </div>
                <div
                  v-if="props.row.description"
                  class="text-caption description"
                >
                  {{ props.row.description }}
                </div>
              </div>
            </div>
          </q-td>
        </template>
        <template #body-cell-due="props">
          <q-td :props="props" class="tabular-nums">
            <span :class="dueClass(props.row.dueDate)">{{
              formatDate(props.row.dueDate)
            }}</span>
            <div class="text-caption muted-text">
              berilgan: {{ formatDate(props.row.assignedDate) }}
            </div>
          </q-td>
        </template>
        <template #body-cell-file="props">
          <q-td :props="props">
            <AppButton
              v-if="props.row.fileName"
              variant="ghost"
              size="sm"
              icon="attach_file"
              :label="props.row.fileName"
              class="file-btn"
              @click="download(props.row)"
            >
              <q-tooltip>{{ fileSize(props.row.fileSize) }}</q-tooltip>
            </AppButton>
            <span v-else class="muted-text">—</span>
          </q-td>
        </template>
        <template #body-cell-sent="props">
          <q-td :props="props">
            <q-badge
              v-if="props.row.notifiedAt"
              color="positive"
              outline
              label="Yuborilgan"
            />
            <AppButton
              v-else-if="authStore.isEditor"
              variant="secondary"
              size="sm"
              icon="send"
              label="Yuborish"
              :loading="busy === props.row.id"
              @click="notify(props.row)"
            />
            <span v-else class="muted-text">—</span>
          </q-td>
        </template>
        <template #body-cell-actions="props">
          <q-td :props="props" class="text-right">
            <template v-if="authStore.isEditor">
              <AppButton
                variant="ghost"
                size="sm"
                icon="edit"
                aria-label="Tahrirlash"
                @click="openEdit(props.row)"
              />
              <AppButton
                variant="ghost"
                size="sm"
                icon="delete"
                color="negative"
                aria-label="O'chirish"
                @click="remove(props.row)"
              />
            </template>
          </q-td>
        </template>
      </q-table>
    </div>

    <q-dialog v-model="dialogOpen" persistent>
      <q-card class="dialog-card" style="width: 560px; max-width: 95vw">
        <q-card-section class="row items-center q-pb-none">
          <div class="text-h6">
            {{ form.id ? 'Vazifani tahrirlash' : 'Uy vazifasi berish' }}
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
            v-if="authStore.isAdmin"
            v-model="form.employeeId"
            :options="employees"
            label="O'qituvchi"
            emit-value
            map-options
            clearable
            use-input
            input-debounce="0"
            @filter="filterEmployees"
          />
          <AppInput v-model="form.title" label="Vazifa *" maxlength="200" />
          <AppInput
            v-model="form.description"
            type="textarea"
            autogrow
            label="Batafsil (ixtiyoriy)"
            maxlength="4000"
          />
          <div class="row q-col-gutter-sm">
            <div class="col-6">
              <AppDateInput v-model="form.assignedDate" label="Berilgan kun" />
            </div>
            <div class="col-6">
              <AppDateInput
                v-model="form.dueDate"
                label="Topshirish kuni *"
                :min="form.assignedDate"
                no-sundays
              />
            </div>
          </div>
          <q-file
            v-model="file"
            outlined
            dense
            clearable
            :label="
              form.fileName
                ? `Fayl: ${form.fileName} (almashtirish)`
                : 'Fayl biriktirish (ixtiyoriy, 20 MB gacha)'
            "
            accept=".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.jpg,.jpeg,.png,.webp,.zip"
            max-file-size="20971520"
            @rejected="onRejected"
          >
            <template #prepend><q-icon name="attach_file" /></template>
          </q-file>
          <q-checkbox
            v-if="!form.id"
            v-model="notifyParents"
            label="Ota-onalarga botda xabar yuborish"
          />
        </q-card-section>
        <q-card-actions align="right" class="q-px-md q-pb-md">
          <AppButton variant="ghost" label="Bekor qilish" v-close-popup />
          <AppButton
            label="Saqlash"
            :loading="saving"
            :disable="!canSave"
            @click="save"
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
import PageLayout from '@/components/PageLayout.vue'
import { computed, onMounted, ref, watch } from 'vue'
import { useQuasar } from 'quasar'
import { api } from '@/boot/axios'
import { useSchoolStore } from '@/stores/school'
import { useAuthStore } from '@/stores/auth'
import { formatDate, todayStr } from '@/utils/date'
import { subjectColor } from '@/utils/subjectColor'
import {
  errorText,
  fileSize,
  loadClasses,
  loadEmployees,
  loadSubjects,
  saveFile
} from '@/utils/modules'

const $q = useQuasar()
const schoolStore = useSchoolStore()
const authStore = useAuthStore()

const rows = ref([])
const loading = ref(false)
const pagination = ref({ page: 1, rowsPerPage: 20, rowsNumber: 0 })
const period = ref('current')
const classId = ref(null)
const subjectId = ref(null)
const mineOnly = ref(false)
const classes = ref([])
const subjects = ref([])
const employees = ref([])
const allEmployees = ref([])
const busy = ref(null)

const dialogOpen = ref(false)
const saving = ref(false)
const form = ref({})
const file = ref(null)
const notifyParents = ref(true)

const columns = [
  { name: 'task', label: 'Vazifa', field: 'title', align: 'left' },
  { name: 'due', label: 'Topshirish', field: 'dueDate', align: 'left' },
  { name: 'file', label: 'Fayl', field: 'fileName', align: 'left' },
  { name: 'sent', label: 'Ota-onalarga', field: 'notifiedAt', align: 'left' },
  { name: 'actions', label: '', field: 'id', align: 'right' }
]

const canSave = computed(
  () =>
    form.value.schoolClassId &&
    form.value.subjectId &&
    form.value.title?.trim() &&
    form.value.dueDate
)

function dueClass(d) {
  const t = todayStr()
  if (d < t) return 'muted-text'
  if (d === t) return 'text-negative text-weight-bold'
  return 'text-weight-medium'
}

async function loadRefs() {
  const id = schoolStore.activeSchoolId
  if (!id) return
  const [c, s] = await Promise.all([loadClasses(id), loadSubjects(id)])
  classes.value = c
  subjects.value = s
  if (authStore.isAdmin) {
    allEmployees.value = await loadEmployees(id)
    employees.value = allEmployees.value
  }
}

function filterEmployees(val, update) {
  update(() => {
    const q = val.toLowerCase()
    employees.value = allEmployees.value.filter(e =>
      e.label.toLowerCase().includes(q)
    )
  })
}

async function load() {
  if (!schoolStore.activeSchoolId) return
  loading.value = true
  try {
    const { page, rowsPerPage } = pagination.value
    const res = await api.get('/api/homework', {
      params: {
        schoolId: schoolStore.activeSchoolId,
        schoolClassId: classId.value || undefined,
        subjectId: subjectId.value || undefined,
        employeeId:
          mineOnly.value && authStore.employeeId
            ? authStore.employeeId
            : undefined,
        from: period.value === 'current' ? todayStr() : undefined,
        page: page - 1,
        size: rowsPerPage,
        sort:
          period.value === 'current'
            ? ['dueDate,asc', 'id,asc']
            : ['dueDate,desc', 'id,desc']
      },
      paramsSerializer: { indexes: null }
    })
    rows.value = res.data.content
    pagination.value.rowsNumber = res.data.totalElements
  } catch (e) {
    $q.notify({ type: 'negative', message: errorText(e) })
  } finally {
    loading.value = false
  }
}

function reload() {
  pagination.value.page = 1
  load()
}

function onRequest(p) {
  pagination.value = { ...pagination.value, ...p.pagination }
  load()
}

function openCreate() {
  form.value = {
    schoolClassId: classId.value,
    subjectId: subjectId.value,
    employeeId: null,
    title: '',
    description: '',
    assignedDate: todayStr(),
    dueDate: ''
  }
  file.value = null
  notifyParents.value = true
  dialogOpen.value = true
}

function openEdit(row) {
  form.value = { ...row }
  file.value = null
  dialogOpen.value = true
}

function onRejected() {
  $q.notify({
    type: 'negative',
    message: 'Fayl 20 MB dan katta yoki bu turdagi fayl qabul qilinmaydi'
  })
}

async function save() {
  saving.value = true
  try {
    const body = {
      schoolClassId: form.value.schoolClassId,
      subjectId: form.value.subjectId,
      employeeId: form.value.employeeId || null,
      assignedDate: form.value.assignedDate || null,
      dueDate: form.value.dueDate,
      title: form.value.title,
      description: form.value.description || null,
      notifyParents: false
    }
    const res = form.value.id
      ? await api.put(`/api/homework/${form.value.id}`, body)
      : await api.post('/api/homework', body)
    const id = res.data.id
    if (file.value) {
      const fd = new FormData()
      fd.append('file', file.value)
      await api.post(`/api/homework/${id}/file`, fd)
    }
    let sent = 0
    if (!form.value.id && notifyParents.value) {
      sent = (await api.post(`/api/homework/${id}/notify`)).data.queued
    }
    $q.notify({
      type: 'positive',
      message: form.value.id
        ? 'Vazifa saqlandi'
        : notifyParents.value
          ? `Vazifa berildi, ${sent} ta ota-onaga xabar ketdi`
          : 'Vazifa berildi'
    })
    dialogOpen.value = false
    load()
  } catch (e) {
    $q.notify({ type: 'negative', message: errorText(e) })
  } finally {
    saving.value = false
  }
}

async function notify(row) {
  busy.value = row.id
  try {
    const res = await api.post(`/api/homework/${row.id}/notify`)
    $q.notify({
      type: 'positive',
      message: `${res.data.queued} ta ota-onaga xabar yuborildi`
    })
    load()
  } catch (e) {
    $q.notify({ type: 'negative', message: errorText(e) })
  } finally {
    busy.value = null
  }
}

function remove(row) {
  $q.dialog({
    title: "Vazifani o'chirish",
    message: `«${row.title}» o'chirilsinmi? Ota-onalar botida ham ko'rinmay qoladi.`,
    cancel: { flat: true, label: 'Bekor qilish', noCaps: true },
    ok: {
      label: "O'chirish",
      color: 'negative',
      unelevated: true,
      noCaps: true
    }
  }).onOk(async () => {
    try {
      await api.delete(`/api/homework/${row.id}`)
      load()
    } catch (e) {
      $q.notify({ type: 'negative', message: errorText(e) })
    }
  })
}

async function download(row) {
  try {
    await saveFile(`/api/homework/${row.id}/file`, null, row.fileName)
  } catch (e) {
    $q.notify({
      type: 'negative',
      message: errorText(e, "Faylni olib bo'lmadi")
    })
  }
}

watch(
  () => schoolStore.activeSchoolId,
  () => {
    loadRefs()
    reload()
  }
)

onMounted(() => {
  loadRefs()
  load()
})
</script>

<style scoped>
.subject-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex: none;
}
.task-cell {
  white-space: normal;
  max-width: 520px;
}
.description {
  white-space: pre-line;
  max-height: 3.6em;
  overflow: hidden;
}
.file-btn {
  max-width: 220px;
}
</style>

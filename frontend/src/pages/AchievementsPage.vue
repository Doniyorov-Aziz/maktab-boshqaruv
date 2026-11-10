<template>
  <page-layout
    icon="emoji_events"
    color="#f59e0b"
    title="O'quvchilar yutuqlari"
    subtitle="Olimpiadalar, tanlovlar, sport musobaqalari va sertifikatlar — o'quvchi profilida va ota-onalar botida ko'rinadi"
  >
    <template #actions>
      <AppButton
        v-if="authStore.isEditor"
        icon="add"
        label="Yutuq qo'shish"
        @click="openCreate"
      />
    </template>

    <div v-if="stats" class="row q-col-gutter-md">
      <div class="col-6 col-md-3">
        <div class="brand-card stat">
          <div class="stat__value">{{ stats.total }}</div>
          <div class="stat__label">jami yutuq</div>
        </div>
      </div>
      <div class="col-6 col-md-3">
        <div class="brand-card stat">
          <div class="stat__value text-warning">{{ stats.prizes }}</div>
          <div class="stat__label">sovrinli o'rin</div>
        </div>
      </div>
      <div class="col-12 col-md-6">
        <div class="brand-card stat">
          <div class="stat__label q-mb-xs">Darajalar bo'yicha</div>
          <div class="row q-gutter-xs">
            <q-chip
              v-for="c in stats.byLevel"
              :key="c.key"
              dense
              square
              :style="{
                background: `${achievementLevels[c.key]?.color}1f`,
                color: achievementLevels[c.key]?.color
              }"
            >
              {{ achievementLevels[c.key]?.label }}:
              <b class="q-ml-xs">{{ c.count }}</b>
            </q-chip>
            <span v-if="!stats.byLevel.length" class="muted-text">—</span>
          </div>
        </div>
      </div>
    </div>

    <div class="row q-col-gutter-sm">
      <div class="col-12 col-sm-3">
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
      <div class="col-6 col-sm-3">
        <AppSelect
          v-model="category"
          :options="toOptions(achievementCategories)"
          label="Turi"
          emit-value
          map-options
          clearable
          @update:model-value="reload"
        />
      </div>
      <div class="col-6 col-sm-3">
        <AppSelect
          v-model="level"
          :options="toOptions(achievementLevels)"
          label="Darajasi"
          emit-value
          map-options
          clearable
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
        :rows-per-page-options="[20, 50]"
      >
        <template #no-data>
          <div class="full-width column flex-center q-py-xl muted-text">
            <q-icon name="emoji_events" size="48px" class="q-mb-sm" />
            <div class="text-subtitle2">Yutuqlar hali kiritilmagan</div>
          </div>
        </template>
        <template #body-cell-title="props">
          <q-td :props="props" class="title-cell">
            <div class="row items-center no-wrap q-gutter-x-sm">
              <span class="medal">{{ medal(props.row.place) }}</span>
              <div>
                <div class="text-weight-medium">{{ props.row.title }}</div>
                <div class="text-caption muted-text">
                  <q-icon
                    :name="achievementCategories[props.row.category].icon"
                  />
                  {{ achievementCategories[props.row.category].label }}
                  <span v-if="props.row.subjectName">
                    · {{ props.row.subjectName }}</span
                  >
                </div>
              </div>
            </div>
          </q-td>
        </template>
        <template #body-cell-student="props">
          <q-td :props="props">
            <router-link
              :to="`/profiles/student/${props.row.studentId}`"
              class="link text-weight-medium"
              >{{ props.row.studentName }}</router-link
            >
            <div class="text-caption muted-text">{{ props.row.className }}</div>
          </q-td>
        </template>
        <template #body-cell-level="props">
          <q-td :props="props">
            <q-badge
              :style="{ background: achievementLevels[props.row.level].color }"
              :label="achievementLevels[props.row.level].label"
            />
            <span v-if="props.row.place" class="q-ml-xs text-weight-bold"
              >{{ props.row.place }}-o'rin</span
            >
          </q-td>
        </template>
        <template #body-cell-file="props">
          <q-td :props="props">
            <AppButton
              v-if="props.row.fileName"
              variant="ghost"
              size="sm"
              icon="workspace_premium"
              aria-label="Diplom"
              @click="download(props.row)"
            >
              <q-tooltip>{{ props.row.fileName }}</q-tooltip>
            </AppButton>
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

    <div v-if="stats?.top.length" class="brand-card q-pa-md">
      <div class="text-subtitle1 text-weight-bold q-mb-sm">
        Eng faol o'quvchilar
      </div>
      <div class="row q-col-gutter-sm">
        <div
          v-for="(t, i) in stats.top"
          :key="t.studentId"
          class="col-12 col-sm-6 col-md-4"
        >
          <router-link
            :to="`/profiles/student/${t.studentId}`"
            class="top-row link"
          >
            <span class="rank">{{ i + 1 }}</span>
            <span class="col ellipsis">{{ t.name }} · {{ t.className }}</span>
            <span class="text-caption muted-text"
              >{{ t.count }} ta<span v-if="t.prizes">
                · 🏆 {{ t.prizes }}</span
              ></span
            >
          </router-link>
        </div>
      </div>
    </div>

    <q-dialog v-model="dialogOpen" persistent>
      <q-card class="dialog-card" style="width: 560px; max-width: 95vw">
        <q-card-section class="row items-center q-pb-none">
          <div class="text-h6">{{
            form.id ? 'Yutuqni tahrirlash' : "Yutuq qo'shish"
          }}</div>
          <q-space />
          <AppButton variant="ghost" icon="close" v-close-popup />
        </q-card-section>
        <q-card-section class="q-gutter-y-sm">
          <AppSelect
            v-model="form.studentId"
            :options="studentOptions"
            label="O'quvchi *"
            emit-value
            map-options
            use-input
            input-debounce="200"
            @filter="filterStudents"
          />
          <AppInput
            v-model="form.title"
            label="Nomi *"
            hint="Masalan: «Matematika fan olimpiadasi (tuman bosqichi)»"
            maxlength="200"
          />
          <div class="row q-col-gutter-sm">
            <div class="col-6">
              <AppSelect
                v-model="form.category"
                :options="toOptions(achievementCategories)"
                label="Turi *"
                emit-value
                map-options
              />
            </div>
            <div class="col-6">
              <AppSelect
                v-model="form.level"
                :options="toOptions(achievementLevels)"
                label="Darajasi *"
                emit-value
                map-options
              />
            </div>
          </div>
          <div class="row q-col-gutter-sm">
            <div class="col-6">
              <AppSelect
                v-model="form.place"
                :options="[
                  { label: '🥇 1-o\'rin', value: 1 },
                  { label: '🥈 2-o\'rin', value: 2 },
                  { label: '🥉 3-o\'rin', value: 3 },
                  { label: '🎖 Ishtirokchi / sertifikat', value: null }
                ]"
                label="O'rin"
                emit-value
                map-options
              />
            </div>
            <div class="col-6">
              <AppDateInput
                v-model="form.achievedOn"
                label="Sana *"
                no-future
              />
            </div>
          </div>
          <AppSelect
            v-model="form.subjectId"
            :options="subjects"
            label="Fan (olimpiada bo'lsa)"
            emit-value
            map-options
            clearable
          />
          <AppInput
            v-model="form.description"
            type="textarea"
            autogrow
            label="Izoh"
            maxlength="1000"
          />
          <q-file
            v-model="file"
            outlined
            dense
            clearable
            :label="
              form.fileName
                ? `Diplom: ${form.fileName} (almashtirish)`
                : 'Diplom / sertifikat (rasm yoki PDF)'
            "
            accept=".pdf,.jpg,.jpeg,.png,.webp"
            max-file-size="20971520"
          >
            <template #prepend><q-icon name="workspace_premium" /></template>
          </q-file>
          <q-checkbox
            v-if="!form.id"
            v-model="notifyParents"
            label="Ota-onalarga botda tabrik yuborish"
          />
        </q-card-section>
        <q-card-actions align="right" class="q-px-md q-pb-md">
          <AppButton variant="ghost" label="Bekor qilish" v-close-popup />
          <AppButton
            label="Saqlash"
            :loading="saving"
            :disable="
              !form.studentId ||
              !form.title?.trim() ||
              !form.category ||
              !form.level ||
              !form.achievedOn
            "
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
import { onMounted, ref, watch } from 'vue'
import { useQuasar } from 'quasar'
import { api } from '@/boot/axios'
import { useSchoolStore } from '@/stores/school'
import { useAuthStore } from '@/stores/auth'
import { formatDate, todayStr } from '@/utils/date'
import {
  achievementCategories,
  achievementLevels,
  errorText,
  loadClasses,
  loadStudents,
  loadSubjects,
  saveFile,
  toOptions
} from '@/utils/modules'

const $q = useQuasar()
const schoolStore = useSchoolStore()
const authStore = useAuthStore()

const rows = ref([])
const stats = ref(null)
const loading = ref(false)
const pagination = ref({ page: 1, rowsPerPage: 20, rowsNumber: 0 })
const classes = ref([])
const subjects = ref([])
const classId = ref(null)
const category = ref(null)
const level = ref(null)
const dialogOpen = ref(false)
const saving = ref(false)
const form = ref({})
const file = ref(null)
const notifyParents = ref(true)
const allStudents = ref([])
const studentOptions = ref([])

const columns = [
  { name: 'title', label: 'Yutuq', field: 'title', align: 'left' },
  { name: 'student', label: "O'quvchi", field: 'studentName', align: 'left' },
  { name: 'level', label: 'Daraja', field: 'level', align: 'left' },
  {
    name: 'date',
    label: 'Sana',
    field: 'achievedOn',
    align: 'left',
    sortable: true,
    format: formatDate
  },
  { name: 'file', label: 'Diplom', field: 'fileName', align: 'center' },
  { name: 'actions', label: '', field: 'id', align: 'right' }
]

function medal(place) {
  return place === 1 ? '🥇' : place === 2 ? '🥈' : place === 3 ? '🥉' : '🎖'
}

async function loadRefs() {
  const id = schoolStore.activeSchoolId
  if (!id) return
  allStudents.value = []
  const [c, s] = await Promise.all([loadClasses(id), loadSubjects(id)])
  classes.value = c
  subjects.value = s
}

async function load() {
  if (!schoolStore.activeSchoolId) return
  loading.value = true
  try {
    const { page, rowsPerPage } = pagination.value
    const [list, st] = await Promise.all([
      api.get('/api/achievements', {
        params: {
          schoolId: schoolStore.activeSchoolId,
          schoolClassId: classId.value || undefined,
          category: category.value || undefined,
          level: level.value || undefined,
          page: page - 1,
          size: rowsPerPage
        }
      }),
      api.get('/api/achievements/stats', {
        params: { schoolId: schoolStore.activeSchoolId }
      })
    ])
    rows.value = list.data.content
    pagination.value.rowsNumber = list.data.totalElements
    stats.value = st.data
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

async function ensureStudents(selectedId) {
  if (!allStudents.value.length) {
    allStudents.value = (await loadStudents(schoolStore.activeSchoolId)).map(
      s => ({
        ...s,
        label: `${s.label} · ${s.className || ''}`
      })
    )
  }
  studentOptions.value = allStudents.value.slice(0, 50)
  if (selectedId && !studentOptions.value.some(s => s.value === selectedId)) {
    const found = allStudents.value.find(s => s.value === selectedId)
    if (found) studentOptions.value.unshift(found)
  }
}

function filterStudents(val, update) {
  update(() => {
    const q = val.toLowerCase()
    studentOptions.value = allStudents.value
      .filter(s => s.label.toLowerCase().includes(q))
      .slice(0, 50)
  })
}

async function openCreate() {
  await ensureStudents()
  form.value = {
    studentId: null,
    title: '',
    category: 'OLYMPIAD',
    level: 'SCHOOL',
    place: 1,
    achievedOn: todayStr(),
    subjectId: null,
    description: ''
  }
  file.value = null
  notifyParents.value = true
  dialogOpen.value = true
}

async function openEdit(row) {
  await ensureStudents(row.studentId)
  form.value = { ...row }
  file.value = null
  dialogOpen.value = true
}

async function save() {
  saving.value = true
  try {
    const body = {
      studentId: form.value.studentId,
      subjectId: form.value.subjectId || null,
      title: form.value.title,
      category: form.value.category,
      level: form.value.level,
      place: form.value.place ?? null,
      achievedOn: form.value.achievedOn,
      description: form.value.description || null,
      notifyParents: !form.value.id && notifyParents.value
    }
    const res = form.value.id
      ? await api.put(`/api/achievements/${form.value.id}`, body)
      : await api.post('/api/achievements', body)
    if (file.value) {
      const fd = new FormData()
      fd.append('file', file.value)
      await api.post(`/api/achievements/${res.data.id}/file`, fd)
    }
    $q.notify({ type: 'positive', message: 'Saqlandi 🏆' })
    dialogOpen.value = false
    load()
  } catch (e) {
    $q.notify({ type: 'negative', message: errorText(e) })
  } finally {
    saving.value = false
  }
}

function remove(row) {
  $q.dialog({
    title: "Yutuqni o'chirish",
    message: `«${row.title}» (${row.studentName}) o'chirilsinmi?`,
    cancel: { flat: true, label: 'Bekor qilish', noCaps: true },
    ok: {
      label: "O'chirish",
      color: 'negative',
      unelevated: true,
      noCaps: true
    }
  }).onOk(async () => {
    try {
      await api.delete(`/api/achievements/${row.id}`)
      load()
    } catch (e) {
      $q.notify({ type: 'negative', message: errorText(e) })
    }
  })
}

async function download(row) {
  try {
    await saveFile(`/api/achievements/${row.id}/file`, null, row.fileName)
  } catch (e) {
    $q.notify({ type: 'negative', message: errorText(e) })
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
.stat {
  padding: 14px 16px;
  height: 100%;
}
.stat__value {
  font-size: 26px;
  font-weight: 800;
}
.stat__label {
  font-size: 12px;
  opacity: 0.7;
}
.title-cell {
  white-space: normal;
  max-width: 420px;
}
.medal {
  font-size: 22px;
}
.link {
  color: inherit;
  text-decoration: none;
}
.top-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  border-radius: 8px;
  background: var(--surface-2);
}
.rank {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 12px;
  background: #f59e0b33;
}
</style>

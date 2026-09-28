<template>
  <q-page class="q-pa-md crud-page" v-if="module">
    <div class="row items-center q-mb-lg q-col-gutter-md">
      <div class="col-auto">
        <q-avatar
          size="48px"
          class="brand-gradient"
          text-color="white"
          :icon="module.icon"
        />
      </div>
      <div class="col">
        <div class="text-h5 text-weight-bold">{{ module.title }}</div>
        <div class="text-caption muted-text">
          <q-skeleton v-if="loading" type="text" width="90px" />
          <template v-else>Jami {{ pagination.rowsNumber }} ta yozuv</template>
        </div>
      </div>
      <div class="col-auto">
        <q-btn
          v-if="canCreate"
          color="primary"
          icon="add"
          label="Yangi qo'shish"
          unelevated
          no-caps
          class="q-px-md"
          @click="openCreateDialog"
        />
      </div>
    </div>

    <div class="brand-card overflow-hidden">
      <q-table
        :rows="rows"
        :columns="tableColumns"
        row-key="id"
        :loading="loading"
        v-model:pagination="pagination"
        @request="onRequest"
        binary-state-sort
        flat
        class="brand-table"
        :rows-per-page-options="[10, 20, 50]"
      >
        <template v-slot:loading>
          <q-inner-loading showing color="primary" />
        </template>

        <template v-slot:no-data>
          <div class="full-width column flex-center q-py-xl muted-text">
            <q-icon name="inbox" size="48px" class="q-mb-sm" />
            <div class="text-subtitle2">Hozircha ma'lumot yo'q</div>
            <div class="text-caption q-mb-md">
              Boshlash uchun birinchi yozuvni qo'shing
            </div>
            <q-btn
              v-if="canCreate"
              outline
              color="primary"
              icon="add"
              label="Yangi qo'shish"
              no-caps
              @click="openCreateDialog"
            />
          </div>
        </template>

        <template v-slot:body-cell-actions="props">
          <q-td :props="props" class="q-gutter-x-xs">
            <q-btn
              v-if="canEdit"
              flat
              dense
              round
              size="sm"
              icon="edit"
              color="primary"
              @click="openEditDialog(props.row)"
            >
              <q-tooltip>Tahrirlash</q-tooltip>
            </q-btn>
            <q-btn
              v-if="canDelete"
              flat
              dense
              round
              size="sm"
              icon="delete_outline"
              color="negative"
              @click="confirmDelete(props.row)"
            >
              <q-tooltip>O'chirish</q-tooltip>
            </q-btn>
          </q-td>
        </template>
      </q-table>
    </div>

    <q-dialog v-model="dialogOpen" persistent>
      <q-card style="width: 100%; max-width: 480px; border-radius: 18px">
        <q-card-section class="row items-center q-pb-none">
          <q-avatar
            size="40px"
            :color="isEditing ? 'primary' : 'positive'"
            text-color="white"
            :icon="isEditing ? 'edit' : 'add'"
            class="q-mr-sm"
          />
          <div class="text-h6">
            {{ isEditing ? 'Tahrirlash' : "Yangi qo'shish" }}
          </div>
          <q-space />
          <q-btn flat round dense icon="close" v-close-popup />
        </q-card-section>

        <q-form @submit.prevent="onSave">
          <q-card-section class="q-gutter-md q-pt-md">
            <template v-for="field in module.fields" :key="field.key">
              <q-select
                v-if="field.type === 'select'"
                v-model="formModel[field.key]"
                :label="field.label"
                :options="fieldOptions[field.key] || []"
                option-value="value"
                option-label="label"
                emit-value
                map-options
                outlined
                dense
                :rules="fieldRules(field)"
              />
              <q-input
                v-else
                v-model="formModel[field.key]"
                :label="field.label"
                :type="inputType(field)"
                :hint="field.hint"
                outlined
                dense
                :rules="fieldRules(field)"
              />
            </template>

            <q-banner
              v-if="formError"
              class="bg-red-1 text-negative rounded-borders"
              dense
            >
              <template v-slot:avatar>
                <q-icon name="error_outline" color="negative" />
              </template>
              {{ formError }}
            </q-banner>
          </q-card-section>

          <q-card-actions align="right" class="q-pa-md">
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
              class="q-px-md"
              :loading="saving"
            />
          </q-card-actions>
        </q-form>
      </q-card>
    </q-dialog>
  </q-page>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useQuasar } from 'quasar'
import { api } from '@/boot/axios'
import { useAuthStore } from '@/stores/auth'
import { getModule } from '@/config/modules'

const route = useRoute()
const router = useRouter()
const $q = useQuasar()
const authStore = useAuthStore()

const module = computed(() => getModule(route.params.moduleKey))

const rows = ref([])
const loading = ref(false)
const pagination = ref({
  sortBy: 'id',
  descending: false,
  page: 1,
  rowsPerPage: 10,
  rowsNumber: 0
})

const tableColumns = computed(() => [
  ...module.value.columns,
  { name: 'actions', label: '', field: 'actions', align: 'right' }
])

const canCreate = computed(() =>
  module.value.adminOnly ? authStore.isAdmin : authStore.isEditor
)
const canEdit = canCreate
const canDelete = computed(() => authStore.isAdmin)

async function fetchRows() {
  if (!module.value) return
  loading.value = true
  try {
    const { page, rowsPerPage, sortBy, descending } = pagination.value
    const params = {
      page: page - 1,
      size: rowsPerPage
    }
    if (sortBy) {
      params.sort = `${sortBy},${descending ? 'desc' : 'asc'}`
    }
    const response = await api.get(module.value.endpoint, { params })
    rows.value = response.data.content
    pagination.value.rowsNumber = response.data.totalElements
  } catch (error) {
    $q.notify({ type: 'negative', message: extractError(error) })
  } finally {
    loading.value = false
  }
}

function onRequest(requestProp) {
  pagination.value = requestProp.pagination
  fetchRows()
}

watch(
  () => route.params.moduleKey,
  async () => {
    pagination.value = {
      sortBy: 'id',
      descending: false,
      page: 1,
      rowsPerPage: 10,
      rowsNumber: 0
    }
    await fetchRows()
    if (route.query.create === '1' && canCreate.value) {
      router.replace({ query: {} })
      openCreateDialog()
    }
  },
  { immediate: true }
)

const dialogOpen = ref(false)
const isEditing = ref(false)
const editingId = ref(null)
const formModel = reactive({})
const formError = ref('')
const saving = ref(false)
const fieldOptions = reactive({})

function inputType(field) {
  if (field.type === 'password') return 'password'
  if (field.type === 'number') return 'number'
  if (field.type === 'date') return 'date'
  if (field.type === 'time') return 'time'
  return 'text'
}

function fieldRules(field) {
  const rules = []
  const isRequired =
    field.required && !(isEditing.value && field.requiredOnCreateOnly)
  if (isRequired) {
    rules.push(
      val =>
        (val !== null && val !== undefined && val !== '') || 'Majburiy maydon'
    )
  }
  return rules
}

async function loadFieldOptions() {
  for (const field of module.value.fields) {
    if (field.type !== 'select') continue

    if (field.options) {
      fieldOptions[field.key] = field.options.map(o => ({ label: o, value: o }))
      continue
    }

    if (field.optionsEndpoint) {
      try {
        const response = await api.get(field.optionsEndpoint, {
          params: { size: 1000 }
        })
        const items = response.data.content
        fieldOptions[field.key] = items.map(item => ({
          value: item[field.optionValue],
          label:
            typeof field.optionLabel === 'function'
              ? field.optionLabel(item)
              : item[field.optionLabel]
        }))
      } catch {
        fieldOptions[field.key] = []
      }
    }
  }
}

async function openCreateDialog() {
  isEditing.value = false
  editingId.value = null
  formError.value = ''
  Object.keys(formModel).forEach(k => delete formModel[k])
  await loadFieldOptions()
  dialogOpen.value = true
}

async function openEditDialog(row) {
  isEditing.value = true
  editingId.value = row.id
  formError.value = ''
  await loadFieldOptions()
  module.value.fields.forEach(field => {
    formModel[field.key] = field.type === 'password' ? '' : row[field.key]
  })
  dialogOpen.value = true
}

async function onSave() {
  saving.value = true
  formError.value = ''
  try {
    const payload = { ...formModel }
    if (module.value.key === 'users' && isEditing.value && !payload.password) {
      delete payload.password
    }

    if (isEditing.value) {
      await api.put(`${module.value.endpoint}/${editingId.value}`, payload)
    } else {
      await api.post(module.value.endpoint, payload)
    }

    dialogOpen.value = false
    $q.notify({
      type: 'positive',
      message: 'Muvaffaqiyatli saqlandi',
      icon: 'check_circle'
    })
    fetchRows()
  } catch (error) {
    formError.value = extractError(error)
  } finally {
    saving.value = false
  }
}

function confirmDelete(row) {
  $q.dialog({
    title: "O'chirish",
    message:
      "Haqiqatan ham o'chirmoqchimisiz? Bu amalni ortga qaytarib bo'lmaydi.",
    cancel: {
      flat: true,
      label: 'Bekor qilish',
      color: 'grey-7',
      noCaps: true
    },
    ok: {
      label: "O'chirish",
      color: 'negative',
      unelevated: true,
      noCaps: true
    },
    persistent: true
  }).onOk(async () => {
    try {
      await api.delete(`${module.value.endpoint}/${row.id}`)
      $q.notify({
        type: 'positive',
        message: "Muvaffaqiyatli o'chirildi",
        icon: 'check_circle'
      })
      fetchRows()
    } catch (error) {
      $q.notify({ type: 'negative', message: extractError(error) })
    }
  })
}

function extractError(error) {
  return error.response?.data || 'Xatolik yuz berdi'
}
</script>

<style scoped>
.crud-page {
  max-width: 1280px;
  margin: 0 auto;
}

.muted-text {
  color: var(--brand-text-muted);
}
</style>

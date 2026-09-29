<template>
  <page-layout v-if="module" :icon="module.icon" :color="module.color">
    <template v-slot:header>
      <div class="col-auto">
        <q-avatar
          size="48px"
          text-color="white"
          :icon="module.icon"
          :style="{ background: module.color || 'var(--brand-gradient)' }"
        />
      </div>
      <div class="col page-title-col">
        <div class="text-h5 text-weight-bold ellipsis">{{ module.title }}</div>
        <div
          v-if="module.schoolScoped && schoolStore.activeSchoolName"
          class="text-caption muted-text ellipsis"
        >
          {{ schoolStore.activeSchoolName }}
        </div>
        <div class="text-caption muted-text">
          <q-skeleton v-if="loading" type="text" width="90px" />
          <template v-else>Jami {{ pagination.rowsNumber }} ta yozuv</template>
        </div>
      </div>
      <div class="col-auto row items-center q-gutter-sm no-wrap">
        <q-input
          v-model="searchQuery"
          dense
          outlined
          clearable
          debounce="250"
          placeholder="Qidirish..."
          class="search-input"
          @update:model-value="onSearchChange"
        >
          <template v-slot:prepend>
            <q-icon name="search" />
          </template>
        </q-input>
        <q-btn
          flat
          dense
          round
          icon="file_download"
          color="grey-7"
          @click="exportCsv"
        >
          <q-tooltip>CSV eksport</q-tooltip>
        </q-btn>
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
    </template>

    <q-banner
      v-if="module.schoolScoped && !schoolStore.activeSchoolId"
      class="bg-orange-1 text-orange-9 rounded-borders q-mb-md"
      dense
    >
      <template v-slot:avatar>
        <q-icon name="info" color="warning" />
      </template>
      Ma'lumotlarni ko'rish uchun avval maktab tanlang
    </q-banner>

    <div v-else class="brand-card overflow-hidden">
      <q-table
        :rows="displayRows"
        :columns="tableColumns"
        row-key="id"
        :loading="loading"
        v-model:pagination="pagination"
        @request="onRequest"
        binary-state-sort
        flat
        class="brand-table"
        :class="{ 'row-clickable': !!module.rowLink }"
        :rows-per-page-options="[10, 20, 50]"
        @row-click="onRowClick"
      >
        <template v-slot:loading>
          <q-inner-loading showing color="primary" />
        </template>

        <template v-slot:no-data>
          <div class="full-width column flex-center q-py-xl muted-text">
            <q-icon name="inbox" size="48px" class="q-mb-sm" />
            <div class="text-subtitle2">{{
              searchQuery ? 'Hech narsa topilmadi' : "Hozircha ma'lumot yo'q"
            }}</div>
            <div class="text-caption q-mb-md">{{
              searchQuery
                ? "Boshqa kalit so'z bilan qidirib ko'ring"
                : "Boshlash uchun birinchi yozuvni qo'shing"
            }}</div>
            <q-btn
              v-if="canCreate && !searchQuery"
              outline
              color="primary"
              icon="add"
              label="Yangi qo'shish"
              no-caps
              @click="openCreateDialog"
            />
          </div>
        </template>

        <template
          v-for="col in linkColumns"
          :key="'body-cell-' + col.name"
          v-slot:[`body-cell-${col.name}`]="props"
        >
          <q-td :props="props">
            <span
              class="link-cell"
              @click.stop="goToProfile(col.link, props.row)"
              >{{ props.value }}</span
            >
          </q-td>
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
              @click.stop="openEditDialog(props.row)"
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
              @click.stop="confirmDelete(props.row)"
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
            <q-banner
              v-if="module.fields.some(f => f.autoSchool)"
              class="bg-blue-1 text-primary rounded-borders"
              dense
            >
              <template v-slot:avatar>
                <q-icon name="school" color="primary" />
              </template>
              Maktab: {{ schoolStore.activeSchoolName || '—' }}
            </q-banner>
            <template v-for="field in module.fields" :key="field.key">
              <q-select
                v-if="!field.autoSchool && field.type === 'select'"
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
                v-else-if="!field.autoSchool && field.type === 'textarea'"
                v-model="formModel[field.key]"
                :label="field.label"
                type="textarea"
                autogrow
                outlined
                dense
                :rules="fieldRules(field)"
              />
              <date-field
                v-else-if="!field.autoSchool && field.type === 'date'"
                v-model="formModel[field.key]"
                :label="field.label"
                :rules="fieldRules(field)"
              />
              <q-input
                v-else-if="!field.autoSchool"
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
  </page-layout>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useQuasar } from 'quasar'
import { api } from '@/boot/axios'
import { useAuthStore } from '@/stores/auth'
import { useSchoolStore } from '@/stores/school'
import { getModule } from '@/config/modules'
import PageLayout from '@/components/PageLayout.vue'
import DateField from '@/components/DateField.vue'

const route = useRoute()
const router = useRouter()
const $q = useQuasar()
const authStore = useAuthStore()
const schoolStore = useSchoolStore()

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

const searchQuery = ref('')
const searchCache = ref(null)
const searchLoading = ref(false)

const tableColumns = computed(() => [
  ...module.value.columns,
  { name: 'actions', label: '', field: 'actions', align: 'right' }
])

const linkColumns = computed(() => module.value.columns.filter(c => c.link))

const displayRows = computed(() => {
  if (!searchQuery.value) return rows.value
  const q = searchQuery.value.toLowerCase()
  const source = searchCache.value || []
  return source.filter(row =>
    module.value.columns.some(col => {
      if (typeof col.field !== 'string') return false
      const v = row[col.field]
      return v != null && String(v).toLowerCase().includes(q)
    })
  )
})

const canCreate = computed(() =>
  module.value.adminOnly ? authStore.isAdmin : authStore.isEditor
)
const canEdit = canCreate
const canDelete = computed(() => authStore.isAdmin)

async function fetchRows() {
  if (!module.value) return
  if (module.value.schoolScoped && !schoolStore.activeSchoolId) {
    rows.value = []
    pagination.value.rowsNumber = 0
    return
  }
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
    if (module.value.schoolScoped) {
      params.schoolId = schoolStore.activeSchoolId
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

async function fetchAllForSearch() {
  if (module.value.schoolScoped && !schoolStore.activeSchoolId) return
  searchLoading.value = true
  try {
    const params = { page: 0, size: 3000 }
    if (module.value.schoolScoped) {
      params.schoolId = schoolStore.activeSchoolId
    }
    const response = await api.get(module.value.endpoint, { params })
    searchCache.value = response.data.content
  } catch {
    searchCache.value = []
  } finally {
    searchLoading.value = false
  }
}

function onSearchChange(val) {
  if (val && !searchCache.value) {
    fetchAllForSearch()
  }
}

function onRequest(requestProp) {
  pagination.value = requestProp.pagination
  fetchRows()
}

function onRowClick(evt, row) {
  if (!module.value.rowLink) return
  if (module.value.key === 'schools') {
    schoolStore.setActiveSchool(row.id, row.name)
    router.push('/')
    return
  }
  router.push(`/profiles/${module.value.rowLink}/${row.id}`)
}

function goToProfile(link, row) {
  const id = row[link.idField]
  if (id != null) {
    router.push(`/profiles/${link.type}/${id}`)
  }
}

async function exportCsv() {
  if (searchQuery.value) {
    doExportCsv(displayRows.value)
    return
  }
  if (!searchCache.value) {
    $q.notify({
      type: 'info',
      message: 'Eksport uchun tayyorlanmoqda...',
      timeout: 800
    })
    await fetchAllForSearch()
  }
  doExportCsv(searchCache.value || rows.value)
}

function doExportCsv(source) {
  const cols = module.value.columns
  const header = cols.map(c => c.label).join(',')
  const lines = source.map(row =>
    cols
      .map(c => {
        const v = typeof c.field === 'string' ? row[c.field] : ''
        const s = v == null ? '' : String(v).replace(/"/g, '""')
        return `"${s}"`
      })
      .join(',')
  )
  const csv = [header, ...lines].join('\n')
  const blob = new Blob(['﻿' + csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${module.value.key}.csv`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
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
    searchQuery.value = ''
    searchCache.value = null
    await fetchRows()
    if (route.query.create === '1' && canCreate.value) {
      router.replace({ query: {} })
      openCreateDialog()
    }
  },
  { immediate: true }
)

watch(
  () => schoolStore.activeSchoolId,
  () => {
    if (!module.value?.schoolScoped) return
    pagination.value.page = 1
    searchCache.value = null
    fetchRows()
    if (searchQuery.value) fetchAllForSearch()
  }
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
    if (field.autoSchool || field.type !== 'select') continue

    if (field.options) {
      fieldOptions[field.key] = field.options.map(o => ({ label: o, value: o }))
      continue
    }

    if (field.optionsEndpoint) {
      try {
        const params = { size: 1000 }
        if (field.schoolScoped) {
          params.schoolId = schoolStore.activeSchoolId
        }
        const response = await api.get(field.optionsEndpoint, { params })
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
  module.value.fields.forEach(field => {
    if (field.autoSchool) {
      formModel[field.key] = schoolStore.activeSchoolId
    }
  })
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
    if (module.value.key === 'schools') {
      await schoolStore.fetchSchools(api)
    }
    searchCache.value = null
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
      if (module.value.key === 'schools') {
        await schoolStore.fetchSchools(api)
      }
      searchCache.value = null
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
.muted-text {
  color: var(--brand-text-muted);
}

.page-title-col {
  min-width: 0;
}

.search-input {
  width: 200px;
}

.link-cell {
  color: var(--q-primary);
  cursor: pointer;
  font-weight: 600;
}

.link-cell:hover {
  text-decoration: underline;
}

:deep(.row-clickable tbody tr) {
  cursor: pointer;
}

:deep(.row-clickable tbody tr:hover) {
  background: rgba(79, 70, 229, 0.06);
}

@media (max-width: 599px) {
  .search-input {
    width: 140px;
  }
}
</style>

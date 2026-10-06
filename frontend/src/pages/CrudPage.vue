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
          no-caps
          icon="file_download"
          label="Excel"
          color="grey-7"
          @click="exportCsv"
        />
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

    <template v-else>
      <div
        v-if="module.filters?.length || module.toggles?.length"
        class="row items-center q-gutter-sm q-mb-md"
      >
        <q-select
          v-for="f in module.filters || []"
          :key="f.key"
          v-model="activeFilters[f.key]"
          :options="filterOptionsFor(f)"
          option-value="value"
          option-label="label"
          emit-value
          map-options
          dense
          outlined
          clearable
          :label="f.label"
          :placeholder="f.placeholder"
          style="min-width: 180px"
          @update:model-value="onFilterChange"
        />
        <q-toggle
          v-for="tg in module.toggles || []"
          :key="tg.key"
          v-model="activeFilters[tg.key]"
          :label="tg.label"
          :true-value="true"
          :false-value="null"
          dense
          @update:model-value="onFilterChange"
        />
      </div>

      <div
        v-if="module.viewType === 'cards' && loading"
        class="row q-col-gutter-md"
      >
        <div v-for="i in 6" :key="i" class="col-12 col-sm-6 col-md-4">
          <q-skeleton type="rect" height="160px" class="rounded-borders" />
        </div>
      </div>

      <template v-else-if="module.viewType === 'cards' && displayRows.length">
        <div class="row q-col-gutter-md">
          <div
            v-for="row in displayRows"
            :key="row.id"
            class="col-12 col-sm-6 col-md-4"
          >
            <div
              class="brand-card q-pa-md full-height entity-card"
              :class="{ 'entity-card--link': cardIsLink }"
              :tabindex="cardIsLink ? 0 : undefined"
              @click="onCardClick(row)"
              @keydown.enter="onCardClick(row)"
            >
              <div class="row items-start justify-between no-wrap">
                <div
                  v-if="module.key === 'school-classes'"
                  class="row items-center q-gutter-sm no-wrap"
                >
                  <div class="class-card__name" :style="{ color: module.color }"
                    >{{ row.gradeNumber }}-{{ row.sectionLetter }}</div
                  >
                </div>
                <div v-else class="row items-center q-gutter-sm no-wrap">
                  <q-avatar
                    :icon="module.icon"
                    :style="{ background: module.color }"
                    text-color="white"
                    size="40px"
                  />
                  <div class="text-weight-bold text-subtitle1 ellipsis">{{
                    row.name
                  }}</div>
                </div>
                <q-badge
                  v-if="
                    module.key === 'schools' &&
                    row.id === schoolStore.activeSchoolId
                  "
                  color="positive"
                  >Faol</q-badge
                >
                <div class="row no-wrap">
                  <q-btn
                    v-if="canEdit"
                    flat
                    dense
                    round
                    size="sm"
                    icon="edit"
                    color="primary"
                    @click.stop="openEditDialog(row)"
                    @keydown.enter.stop
                  />
                  <q-btn
                    v-if="canDelete"
                    flat
                    dense
                    round
                    size="sm"
                    icon="delete_outline"
                    color="negative"
                    @click.stop="confirmDelete(row)"
                    @keydown.enter.stop
                  />
                </div>
              </div>

              <template v-if="module.key === 'schools'">
                <div class="text-caption muted-text q-mt-xs ellipsis">{{
                  row.address
                }}</div>
                <div class="row q-mt-md q-col-gutter-sm text-center">
                  <div class="col-4">
                    <div class="text-h6 text-weight-bold">{{
                      row.studentCount ?? 0
                    }}</div>
                    <div class="text-caption muted-text">O'quvchilar</div>
                  </div>
                  <div class="col-4">
                    <div class="text-h6 text-weight-bold">{{
                      row.teacherCount ?? 0
                    }}</div>
                    <div class="text-caption muted-text">O'qituvchilar</div>
                  </div>
                  <div class="col-4">
                    <div class="text-h6 text-weight-bold">{{
                      row.classCount ?? 0
                    }}</div>
                    <div class="text-caption muted-text">Sinflar</div>
                  </div>
                </div>
                <div class="row items-center q-mt-md text-caption text-primary">
                  <q-icon name="login" size="16px" class="q-mr-xs" />
                  {{
                    row.id === schoolStore.activeSchoolId
                      ? 'Joriy maktab — bosing, bosh sahifa ochiladi'
                      : "Bosing — shu maktabga o'tish"
                  }}
                </div>
              </template>

              <template v-else-if="module.key === 'school-classes'">
                <div class="text-caption muted-text q-mt-xs ellipsis">
                  <q-icon name="person" size="14px" />
                  {{ row.classTeacherName || 'Sinf rahbari tayinlanmagan' }}
                </div>
                <div class="row q-mt-md q-col-gutter-sm text-center">
                  <div class="col-6">
                    <div class="text-h6 text-weight-bold">{{
                      row.studentCount ?? 0
                    }}</div>
                    <div class="text-caption muted-text">O'quvchi</div>
                  </div>
                  <div class="col-6">
                    <div class="text-h6 text-weight-bold">{{
                      row.maxStudents ?? '—'
                    }}</div>
                    <div class="text-caption muted-text">Maks.</div>
                  </div>
                </div>
                <div class="text-caption muted-text q-mt-sm ellipsis">{{
                  row.academicYearTitle
                }}</div>
              </template>

              <template v-else-if="module.key === 'buildings'">
                <div class="text-caption muted-text q-mt-xs">{{
                  row.schoolName
                }}</div>
                <div class="row q-mt-md q-col-gutter-sm text-center">
                  <div class="col-4">
                    <div class="text-h6 text-weight-bold">{{
                      row.floorCount ?? 1
                    }}</div>
                    <div class="text-caption muted-text">Qavat</div>
                  </div>
                  <div class="col-4">
                    <div class="text-h6 text-weight-bold">{{
                      row.roomCount ?? 0
                    }}</div>
                    <div class="text-caption muted-text">Xona</div>
                  </div>
                  <div class="col-4">
                    <div class="text-h6 text-weight-bold"
                      >{{ row.occupiedPercentage ?? 0 }}%</div
                    >
                    <div class="text-caption muted-text">Band</div>
                  </div>
                </div>
              </template>
            </div>
          </div>
        </div>

        <div
          v-if="!searchQuery && !filtersActive"
          class="row items-center justify-end q-mt-md q-gutter-sm"
        >
          <div class="text-caption muted-text">{{ cardPageLabel }}</div>
          <q-btn
            flat
            dense
            round
            icon="chevron_left"
            :disable="pagination.page <= 1"
            @click="changeCardPage(-1)"
          />
          <q-btn
            flat
            dense
            round
            icon="chevron_right"
            :disable="
              pagination.page * pagination.rowsPerPage >= pagination.rowsNumber
            "
            @click="changeCardPage(1)"
          />
        </div>
      </template>

      <div
        v-else-if="module.viewType === 'cards'"
        class="brand-card q-pa-xl column flex-center muted-text"
      >
        <q-icon name="inbox" size="48px" class="q-mb-sm" />
        <div class="text-subtitle2">{{
          searchQuery ? 'Hech narsa topilmadi' : "Hozircha ma'lumot yo'q"
        }}</div>
        <q-btn
          v-if="canCreate && !searchQuery"
          outline
          color="primary"
          icon="add"
          label="Yangi qo'shish"
          no-caps
          class="q-mt-md"
          @click="openCreateDialog"
        />
      </div>

      <!-- first load: skeleton rows instead of an empty table flashing "Hozircha ma'lumot yo'q" -->
      <div
        v-else-if="loading && !rows.length"
        class="brand-card q-pa-md column q-gutter-sm"
      >
        <q-skeleton type="text" width="40%" height="28px" />
        <q-skeleton
          v-for="i in pagination.rowsPerPage"
          :key="i"
          type="rect"
          height="34px"
        />
      </div>

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
          class="brand-table sticky-table"
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

          <template
            v-if="firstNonLinkColumn"
            v-slot:[`body-cell-${firstNonLinkColumn}`]="props"
          >
            <q-td :props="props" class="text-weight-bold">{{
              props.value
            }}</q-td>
          </template>

          <template
            v-for="col in badgeColumns"
            :key="'badge-' + col.name"
            v-slot:[`body-cell-${col.name}`]="props"
          >
            <q-td :props="props">
              <q-badge
                :style="{
                  background: col.badgeColors[props.value] || '#64748b'
                }"
                class="q-px-sm q-py-2xs"
                >{{ col.badgeLabels?.[props.value] || props.value }}</q-badge
              >
              <div
                v-if="col.badgeHint && col.badgeHint(props.row)"
                class="text-caption muted-text badge-hint"
                >{{ col.badgeHint(props.row) }}</div
              >
            </q-td>
          </template>

          <template
            v-if="module.key === 'rooms'"
            v-slot:body-cell-currentStatus="props"
          >
            <q-td :props="props">
              <span
                class="link-cell"
                @click.stop="openRoomOccupancy(props.row)"
                >{{ props.value }}</span
              >
            </q-td>
          </template>

          <template v-slot:body-cell-actions="props">
            <q-td :props="props" class="row-actions">
              <q-btn
                v-if="
                  canEdit &&
                  module.key === 'subjects' &&
                  props.row.active === false
                "
                flat
                dense
                round
                size="sm"
                icon="restore"
                color="positive"
                :loading="busyRowId === props.row.id"
                @click.stop="reactivate(props.row)"
              >
                <q-tooltip>Qayta faollashtirish</q-tooltip>
              </q-btn>
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
                v-if="canDelete && props.row.active !== false"
                flat
                dense
                round
                size="sm"
                :icon="module.softDelete ? 'block' : 'delete_outline'"
                color="negative"
                :loading="busyRowId === props.row.id"
                @click.stop="confirmDelete(props.row)"
              >
                <q-tooltip>{{
                  module.softDelete ? 'Nofaol qilish' : "O'chirish"
                }}</q-tooltip>
              </q-btn>
            </q-td>
          </template>
        </q-table>
      </div>
    </template>

    <q-dialog v-model="roomOccupancyOpen">
      <q-card style="width: 100%; max-width: 420px; border-radius: 18px">
        <q-card-section class="row items-center q-pb-none">
          <div class="text-h6"
            >{{ roomOccupancyRoom?.roomNumber }}-xona — bugungi jadval</div
          >
          <q-space />
          <q-btn flat round dense icon="close" v-close-popup />
        </q-card-section>
        <q-card-section>
          <div v-if="roomOccupancyLoading" class="column q-gutter-sm">
            <q-skeleton v-for="i in 4" :key="i" type="text" height="36px" />
          </div>
          <div
            v-else-if="!roomOccupancySlots.length"
            class="text-body2 muted-text q-py-md text-center"
          >
            Bugun bu xonada dars yo'q
          </div>
          <q-list v-else separator>
            <q-item
              v-for="(slot, i) in roomOccupancySlots"
              :key="i"
              :class="{ 'occupancy-slot--current': slot.current }"
            >
              <q-item-section avatar>
                <q-icon
                  :name="slot.current ? 'radio_button_checked' : 'schedule'"
                  :color="slot.current ? 'positive' : 'grey-6'"
                />
              </q-item-section>
              <q-item-section>
                <q-item-label
                  >{{ slot.startTime.slice(0, 5) }}–{{
                    slot.endTime.slice(0, 5)
                  }}
                  · {{ slot.className }} · {{ slot.subjectName }}</q-item-label
                >
                <q-item-label caption>{{ slot.teacherName }}</q-item-label>
              </q-item-section>
            </q-item>
          </q-list>
        </q-card-section>
      </q-card>
    </q-dialog>

    <q-dialog
      v-model="dialogOpen"
      persistent
      position="right"
      full-height
      maximized-mobile
    >
      <q-card
        style="width: 420px; max-width: 100vw"
        class="form-drawer column full-height no-wrap"
      >
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

        <q-form
          @submit.prevent="onSave"
          class="column full-height no-wrap"
          style="min-height: 0"
        >
          <q-card-section class="q-gutter-md q-pt-md col scroll">
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
            <template v-for="field in formFields" :key="field.key">
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
                :hint="field.hint"
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
const activeFilters = reactive({})
const filterOptionsCache = reactive({})

const visibleColumns = computed(() =>
  module.value.columns.filter(c => c.name !== 'id')
)

const tableColumns = computed(() => [
  ...visibleColumns.value,
  { name: 'actions', label: '', field: 'actions', align: 'right' }
])

const firstNonLinkColumn = computed(() => {
  const first = visibleColumns.value[0]
  return first && !first.link ? first.name : null
})

const linkColumns = computed(() => module.value.columns.filter(c => c.link))
const badgeColumns = computed(() =>
  module.value.columns.filter(c => c.badgeColors)
)

const filtersActive = computed(() =>
  Object.values(activeFilters).some(v => v != null && v !== '')
)

const displayRows = computed(() => {
  if (!searchQuery.value) return rows.value
  const q = searchQuery.value.toLowerCase()
  const source = searchCache.value || []
  return source.filter(
    row =>
      (module.value.searchText &&
        module.value.searchText(row).toLowerCase().includes(q)) ||
      module.value.columns.some(col => {
        if (typeof col.field !== 'string') return false
        const v = row[col.field]
        return v != null && String(v).toLowerCase().includes(q)
      })
  )
})

function filterOptionsFor(f) {
  if (f.options) return f.options
  return filterOptionsCache[f.key] || []
}

async function loadFilterOptionsFor(f) {
  if (!f.optionsEndpoint || filterOptionsCache[f.key]) return
  try {
    const params = { size: 1000 }
    if (f.schoolScoped) params.schoolId = schoolStore.activeSchoolId
    const response = await api.get(f.optionsEndpoint, { params })
    const options = response.data.content.map(item => ({
      value: item[f.optionValue],
      label:
        typeof f.optionLabel === 'function'
          ? f.optionLabel(item)
          : item[f.optionLabel]
    }))
    // "2-A" before "10-A"
    if (f.sortOptions)
      options.sort((a, b) =>
        String(a.label).localeCompare(String(b.label), undefined, {
          numeric: true
        })
      )
    filterOptionsCache[f.key] = options
  } catch {
    filterOptionsCache[f.key] = []
  }
}

function onFilterChange() {
  pagination.value.page = 1
  fetchRows()
  if (searchQuery.value) {
    searchCache.value = null
    fetchAllForSearch()
  }
}

const cardPageLabel = computed(() => {
  const { page, rowsPerPage, rowsNumber } = pagination.value
  if (!rowsNumber) return ''
  const start = (page - 1) * rowsPerPage + 1
  const end = Math.min(page * rowsPerPage, rowsNumber)
  return `${start}-${end} / ${rowsNumber}`
})

function changeCardPage(dir) {
  pagination.value.page += dir
  fetchRows()
}

const canCreate = computed(() =>
  module.value.adminOnly ? authStore.isAdmin : authStore.isEditor
)
const canEdit = canCreate
const canDelete = computed(() => authStore.isAdmin)

function filterParams() {
  const params = {}
  if (module.value.schoolScoped) {
    params.schoolId = schoolStore.activeSchoolId
  }
  for (const [key, val] of Object.entries(activeFilters)) {
    if (val != null && val !== '') params[key] = val
  }
  return params
}

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
      ...filterParams(),
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

async function fetchAllForSearch() {
  if (module.value.schoolScoped && !schoolStore.activeSchoolId) return
  searchLoading.value = true
  try {
    const params = { ...filterParams(), page: 0, size: 3000 }
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
    switchToSchool(row)
    return
  }
  router.push(`/profiles/${module.value.rowLink}/${row.id}`)
}

const cardIsLink = computed(() =>
  ['schools', 'school-classes'].includes(module.value.key)
)

function onCardClick(row) {
  if (module.value.key === 'schools') switchToSchool(row)
  else if (module.value.key === 'school-classes')
    router.push(`/profiles/class/${row.id}`)
}

function switchToSchool(row) {
  schoolStore.setActiveSchool(row.id, row.name)
  router.push('/')
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
      sortBy: module.value?.defaultSort || 'id',
      descending: !!module.value?.defaultDescending,
      page: 1,
      rowsPerPage: module.value?.viewType === 'cards' ? 12 : 10,
      rowsNumber: 0
    }
    searchQuery.value = ''
    searchCache.value = null
    Object.keys(activeFilters).forEach(k => delete activeFilters[k])
    Object.keys(filterOptionsCache).forEach(k => delete filterOptionsCache[k])
    if (module.value?.filters) {
      module.value.filters.forEach(loadFilterOptionsFor)
    }
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

const roomOccupancyOpen = ref(false)
const roomOccupancyRoom = ref(null)
const roomOccupancySlots = ref([])
const roomOccupancyLoading = ref(false)

async function openRoomOccupancy(row) {
  roomOccupancyRoom.value = row
  roomOccupancyOpen.value = true
  roomOccupancyLoading.value = true
  try {
    const res = await api.get(`/api/rooms/${row.id}/occupancy`)
    roomOccupancySlots.value = res.data
  } catch {
    roomOccupancySlots.value = []
  } finally {
    roomOccupancyLoading.value = false
  }
}

const dialogOpen = ref(false)
const isEditing = ref(false)
const editingId = ref(null)
const formModel = reactive({})
const formError = ref('')
const saving = ref(false)
const fieldOptions = reactive({})

// a field with showIf(form) is shown (and validated, and sent) only when it applies
const formFields = computed(() =>
  module.value.fields.filter(f => !f.showIf || f.showIf(formModel))
)

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
      fieldOptions[field.key] = field.options.map(o =>
        typeof o === 'object' ? o : { label: o, value: o }
      )
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
  if (saving.value) return // Enter + click must not create two rows
  saving.value = true
  formError.value = ''
  try {
    const payload = { ...formModel }
    for (const f of module.value.fields) {
      if (f.showIf && !f.showIf(formModel)) payload[f.key] = null
    }
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

const busyRowId = ref(null)

async function reactivate(row) {
  busyRowId.value = row.id
  try {
    await api.put(`${module.value.endpoint}/${row.id}/activate`)
    $q.notify({ type: 'positive', message: 'Qayta faollashtirildi' })
    searchCache.value = null
    fetchRows()
  } catch (error) {
    $q.notify({ type: 'negative', message: extractError(error) })
  } finally {
    busyRowId.value = null
  }
}

function confirmDelete(row) {
  const soft = module.value.softDelete
  $q.dialog({
    title: soft ? 'Nofaol qilish' : "O'chirish",
    message: soft
      ? "Fan nofaol bo'ladi: yangi baho va jadvalda tanlanmaydi, eski baholarda nomi saqlanadi. Keyin qayta faollashtirish mumkin."
      : "Haqiqatan ham o'chirmoqchimisiz? Bu amalni ortga qaytarib bo'lmaydi.",
    cancel: {
      flat: true,
      label: 'Bekor qilish',
      color: 'grey-7',
      noCaps: true
    },
    ok: {
      label: soft ? 'Nofaol qilish' : "O'chirish",
      color: 'negative',
      unelevated: true,
      noCaps: true
    },
    persistent: true
  }).onOk(async () => {
    if (busyRowId.value === row.id) return // a second click while the first is on its way
    busyRowId.value = row.id
    try {
      await api.delete(`${module.value.endpoint}/${row.id}`)
      $q.notify({
        type: 'positive',
        message: soft ? 'Nofaol qilindi' : "Muvaffaqiyatli o'chirildi",
        icon: 'check_circle'
      })
      if (module.value.key === 'schools') {
        await schoolStore.fetchSchools(api)
      }
      searchCache.value = null
      fetchRows()
    } catch (error) {
      $q.notify({ type: 'negative', message: extractError(error) })
    } finally {
      busyRowId.value = null
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

/* edit/delete are always visible; the column sticks to the right edge even when
   the table scrolls sideways, and the header row sticks to the top */
.row-actions {
  white-space: nowrap;
}

:deep(.sticky-table .q-table__middle) {
  max-height: calc(100vh - 260px);
}

:deep(.sticky-table thead tr th) {
  position: sticky;
  top: 0;
  z-index: 2;
  background: var(--card-bg);
}

:deep(.sticky-table tbody td:last-child),
:deep(.sticky-table thead th:last-child) {
  position: sticky;
  right: 0;
  background: var(--card-bg);
  box-shadow: -8px 0 10px -10px rgba(15, 23, 42, 0.35);
}

:deep(.sticky-table thead th:last-child) {
  z-index: 3;
}

:deep(.sticky-table tbody tr:hover td:last-child) {
  background: color-mix(in srgb, var(--card-bg) 94%, #4f46e5);
}

/* school / class cards: the whole card opens it */
.entity-card--link {
  cursor: pointer;
}

.entity-card--link:hover {
  transform: translateY(-2px);
  border-color: color-mix(in srgb, var(--q-primary) 45%, var(--brand-border));
}

.class-card__name {
  font-size: 30px;
  font-weight: 800;
  line-height: 1;
}

.form-drawer {
  border-radius: 0;
}

.badge-hint {
  margin-top: 2px;
  white-space: nowrap;
}

.entity-card {
  transition:
    box-shadow 0.15s ease,
    transform 0.15s ease;
}

.entity-card:hover {
  box-shadow: var(--shadow-card-hover);
}

.occupancy-slot--current {
  background: rgba(34, 197, 94, 0.08);
  border-radius: var(--radius-sm);
}

@media (max-width: 599px) {
  .search-input {
    width: 140px;
  }
}
</style>

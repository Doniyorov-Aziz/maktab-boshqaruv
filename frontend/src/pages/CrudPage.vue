<template>
  <page-layout
    v-if="module"
    :icon="module.icon"
    :color="module.color"
    :title="module.title"
  >
    <template #subtitle>
      <span class="crud-sub" data-crud-subtitle>
        <template v-if="module.schoolScoped && schoolStore.activeSchoolName"
          >{{ schoolStore.activeSchoolName }} ·
        </template>
        <q-skeleton
          v-if="loading && !loaded"
          type="text"
          width="60px"
          class="crud-sub__skel"
        />
        <template v-else>{{ total }} ta</template>
      </span>
    </template>
    <template #actions>
      <AppInput
        ref="searchEl"
        v-model="search"
        clearable
        debounce="300"
        placeholder="Qidirish…"
        class="crud-search"
        aria-label="Qidirish"
        data-crud-search
        @update:model-value="onSearch"
      >
        <template #prepend><q-icon name="search" /></template>
        <template #append>
          <kbd v-if="!search" class="crud-kbd" title="Qidiruvga o'tish">/</kbd>
        </template>
      </AppInput>
      <AppButton
        variant="secondary"
        icon="file_download"
        label="Eksport"
        data-export
      >
        <q-menu anchor="bottom right" self="top right" auto-close>
          <q-list dense class="crud-menu">
            <q-item clickable data-export-xlsx @click="exportAll('xlsx')">
              <q-item-section avatar
                ><q-icon name="table_view"
              /></q-item-section>
              <q-item-section>Excel (.xlsx)</q-item-section>
            </q-item>
            <q-item clickable data-export-pdf @click="exportAll('pdf')">
              <q-item-section avatar
                ><q-icon name="picture_as_pdf"
              /></q-item-section>
              <q-item-section>PDF</q-item-section>
            </q-item>
          </q-list>
        </q-menu>
      </AppButton>
      <AppButton
        v-if="canCreate"
        icon="add"
        label="Yangi qo'shish"
        data-create
        @click="openCreate"
      />
    </template>

    <q-banner
      v-if="module.schoolScoped && !schoolStore.activeSchoolId"
      class="crud-banner rounded-borders"
      dense
    >
      <template #avatar><q-icon name="info" /></template>
      Ma'lumotlarni ko'rish uchun avval maktab tanlang
    </q-banner>

    <div v-else class="crud">
      <!-- 1.2 hero numbers (from the module's hero config) -->
      <div v-if="module.hero && hero" class="crud-hero" data-crud-hero>
        <KpiCard
          v-for="(k, i) in module.hero.cards"
          :key="k.key"
          compact
          :kpi-key="k.key"
          :label="k.label"
          :icon="k.icon"
          :color="k.color"
          :value="k.value(hero)"
          :format="k.format"
          :empty-value="k.value(hero) == null"
          :delta="k.delta?.(hero) ?? null"
          :spark="k.spark?.(hero) || []"
          :style="{ '--i': i }"
        />
      </div>

      <!-- 1.3 filters, 1.4 view, density, columns -->
      <section
        v-if="hasFilters || canCards || view === 'table'"
        class="crud-bar"
        :class="hasFilters || canCards ? 'dash-card' : 'crud-bar--plain'"
        data-crud-bar
      >
        <CrudFilters
          v-if="hasFilters"
          class="crud-bar__filters"
          :filters="module.filters || []"
          :toggles="module.toggles || []"
          :values="state.filters"
          :options="filterOptions"
          :facets="facets"
          :classes="classes"
          @change="onFilter"
          @clear="clearFilters"
        />
        <div class="crud-bar__tools">
          <SegTabs
            v-if="canCards"
            :model-value="view"
            label="Ko'rinish"
            :options="VIEWS"
            data-view-switch
            @update:model-value="setView"
          />
          <template v-if="view === 'table'">
            <SegTabs
              :model-value="density"
              label="Zichlik"
              :options="DENSITIES"
              data-density
              @update:model-value="setDensity"
            />
            <AppButton
              variant="ghost"
              size="sm"
              icon="view_column"
              aria-label="Ustunlar"
              data-columns
            >
              <q-tooltip>Ustunlar</q-tooltip>
              <q-menu anchor="bottom right" self="top right">
                <q-list dense class="crud-menu" data-columns-menu>
                  <q-item-label header>Ko'rinadigan ustunlar</q-item-label>
                  <q-item
                    v-for="c in allColumns"
                    :key="c.name"
                    clickable
                    :data-column-toggle="c.name"
                    @click="toggleColumn(c.name)"
                  >
                    <q-item-section avatar>
                      <q-icon
                        :name="
                          shown.includes(c.name)
                            ? 'check_box'
                            : 'check_box_outline_blank'
                        "
                        :color="shown.includes(c.name) ? 'primary' : undefined"
                      />
                    </q-item-section>
                    <q-item-section>{{ c.label }}</q-item-section>
                  </q-item>
                  <q-separator />
                  <q-item clickable @click="resetColumns">
                    <q-item-section avatar
                      ><q-icon name="restart_alt"
                    /></q-item-section>
                    <q-item-section>Asl holiga</q-item-section>
                  </q-item>
                </q-list>
              </q-menu>
            </AppButton>
          </template>
        </div>
      </section>

      <!-- 1.9 states -->
      <section v-if="error" class="dash-card crud-state" data-crud-error>
        <EmptyState
          icon="error_outline"
          title="Ma'lumotni yuklab bo'lmadi"
          :text="error"
          action="Qayta urinish"
          action-icon="refresh"
          @action="fetchRows"
        />
      </section>

      <section
        v-else-if="loading && !loaded"
        class="dash-card crud-skel"
        data-crud-skeleton
      >
        <div v-if="view === 'cards'" class="crud-skel__cards">
          <div v-for="i in 6" :key="i" class="skel crud-skel__card" />
        </div>
        <template v-else>
          <div class="skel crud-skel__head" />
          <div v-for="i in 8" :key="i" class="skel crud-skel__row" />
        </template>
      </section>

      <section
        v-else-if="!rows.length"
        class="dash-card crud-state"
        data-crud-empty
      >
        <EmptyState
          v-if="searchOrFilter"
          icon="filter_alt_off"
          title="Hech narsa topilmadi"
          text="Qidiruv yoki filtr bo'yicha natija yo'q."
          action="Filtrni tozalash"
          action-icon="close"
          data-empty="filtered"
          @action="clearAll"
        />
        <EmptyState
          v-else
          :icon="module.icon"
          :title="`${module.title}: hozircha yozuv yo'q`"
          :text="
            canCreate ? 'Birinchisini qo\'shing — shu yerda ko\'rinadi.' : ''
          "
          :action="canCreate ? 'Qo\'shish' : ''"
          action-icon="add"
          data-empty="none"
          @action="openCreate"
        />
      </section>

      <template v-else>
        <!-- 1.6 cards -->
        <CrudCards
          v-if="view === 'cards'"
          :rows="rows"
          :card="module.cards"
          :icon="module.icon"
          :color="module.color"
          :can-edit="canEdit && !module.readOnlyCards"
          :can-delete="canDelete && !module.readOnlyCards"
          :soft-delete="!!module.softDelete"
          @open="openRow"
          @edit="openEdit"
          @delete="confirmDelete"
        />
        <!-- 1.5 table -->
        <section v-else class="dash-card crud-table-card">
          <CrudTable
            :columns="tableColumns"
            :rows="rows"
            :loading="loading"
            selectable
            :selected="selected"
            :sort-by="state.sortBy"
            :descending="state.descending"
            :density="density"
            :can-edit="canEdit"
            :can-delete="canDelete"
            :soft-delete="!!module.softDelete"
            :extra-actions="extraActions"
            :today="today"
            :label="module.title"
            :title-of="titleOf"
            @sort="onSort"
            @row="openRow"
            @edit="openEdit"
            @delete="confirmDelete"
            @action="onAction"
            @select="onSelect"
            @select-all="onSelectAll"
            @profile="goToProfile"
          />
        </section>

        <!-- pager -->
        <nav class="crud-pager" aria-label="Sahifalar" data-crud-pager>
          <span class="crud-pager__size">
            Sahifada
            <AppSelect
              :model-value="state.size"
              :options="PAGE_SIZES"
              dense
              options-dense
              class="crud-pager__select"
              aria-label="Sahifadagi qatorlar"
              data-page-size
              @update:model-value="setSize"
            />
          </span>
          <span class="crud-pager__range tnum" data-page-range>{{
            range
          }}</span>
          <AppButton
            variant="ghost"
            size="sm"
            icon="first_page"
            aria-label="Birinchi sahifa"
            :disable="state.page <= 1"
            @click="setPage(1)"
          />
          <AppButton
            variant="ghost"
            size="sm"
            icon="chevron_left"
            aria-label="Oldingi sahifa"
            :disable="state.page <= 1"
            data-page-prev
            @click="setPage(state.page - 1)"
          />
          <span class="crud-pager__page tnum"
            >{{ state.page }} / {{ pages }}</span
          >
          <AppButton
            variant="ghost"
            size="sm"
            icon="chevron_right"
            aria-label="Keyingi sahifa"
            :disable="state.page >= pages"
            data-page-next
            @click="setPage(state.page + 1)"
          />
          <AppButton
            variant="ghost"
            size="sm"
            icon="last_page"
            aria-label="Oxirgi sahifa"
            :disable="state.page >= pages"
            @click="setPage(pages)"
          />
        </nav>
      </template>
    </div>

    <!-- bulk actions -->
    <transition name="crud-bulk">
      <div
        v-if="selected.size"
        class="crud-bulk"
        role="toolbar"
        aria-label="Tanlanganlar"
        data-bulk-bar
      >
        <b class="tnum">{{ selected.size }} ta tanlandi</b>
        <AppButton
          variant="secondary"
          size="sm"
          icon="table_view"
          label="Excel"
          data-bulk-xlsx
          @click="exportSelected('xlsx')"
        />
        <AppButton
          variant="secondary"
          size="sm"
          icon="picture_as_pdf"
          label="PDF"
          @click="exportSelected('pdf')"
        />
        <AppButton
          v-for="a in module.bulkActions || []"
          :key="a.key"
          variant="secondary"
          size="sm"
          :icon="a.icon"
          :label="a.label"
          @click="a.run({ ids: [...selected], api, reload: fetchRows, $q })"
        />
        <AppButton
          v-if="canDelete"
          variant="danger"
          size="sm"
          :icon="module.softDelete ? 'block' : 'delete_outline'"
          :label="module.softDelete ? 'Nofaol qilish' : 'O\'chirish'"
          data-bulk-delete
          @click="deleteSelected"
        />
        <AppButton
          variant="ghost"
          size="sm"
          icon="close"
          aria-label="Tanlovni bekor qilish"
          data-bulk-clear
          @click="selected.clear()"
        />
      </div>
    </transition>

    <!-- 1.5 / 1.7: one panel for reading and editing a record -->
    <SideDrawer
      :model-value="panel.open"
      persistent
      :wide="panel.mode !== 'view'"
      :title="panelTitle"
      :subtitle="panel.mode === 'view' ? '' : module.title"
      :icon="
        panel.mode === 'create'
          ? 'add'
          : panel.mode === 'edit'
            ? 'edit'
            : module.icon
      "
      :color="panel.mode === 'view' ? module.color : 'var(--c-primary)'"
      @update:model-value="v => !v && closePanel()"
    >
      <CrudDetail
        v-if="panel.mode === 'view' && panel.row"
        :module="module"
        :row="panel.row"
        :options="fieldOptions"
        :today="today"
        @profile="goToProfile"
      />
      <CrudForm
        v-else-if="panel.mode !== 'view'"
        ref="formEl"
        form-id="crud-form"
        :fields="formFields"
        :model="formModel"
        :options-for="optionsFor"
        :editing="panel.mode === 'edit'"
        :error="formError"
        :school-name="
          module.fields.some(f => f.autoSchool)
            ? schoolStore.activeSchoolName || ''
            : ''
        "
        @submit="onSave"
        @parent-change="clearOrphanChildren"
      />
      <template #footer>
        <template v-if="panel.mode === 'view'">
          <AppButton
            v-if="module.profileLink && panel.row"
            variant="ghost"
            icon="open_in_new"
            label="Profil"
            data-open-profile
            @click="goToProfile(module.profileLink, panel.row)"
          />
          <q-space />
          <AppButton
            v-if="canDelete && panel.row?.active !== false"
            variant="ghost"
            color="negative"
            :icon="module.softDelete ? 'block' : 'delete_outline'"
            :label="module.softDelete ? 'Nofaol qilish' : 'O\'chirish'"
            @click="confirmDelete(panel.row)"
          />
          <AppButton
            v-if="canEdit"
            icon="edit"
            label="Tahrirlash"
            data-detail-edit
            @click="openEdit(panel.row)"
          />
        </template>
        <template v-else>
          <span v-if="dirty" class="crud-dirty" data-dirty
            >● Saqlanmagan o'zgarishlar</span
          >
          <q-space />
          <AppButton
            variant="ghost"
            label="Bekor qilish"
            color="grey-7"
            data-form-cancel
            @click="closePanel"
          />
          <!-- outside the <form> (the footer stays put), tied to it by id -->
          <AppButton
            type="submit"
            form="crud-form"
            label="Saqlash"
            class="q-px-md"
            :loading="saving"
            data-form-save
          />
        </template>
      </template>
    </SideDrawer>

    <q-dialog v-model="roomOccupancyOpen">
      <q-card class="app-dialog app-dialog--sm">
        <q-card-section class="row items-center q-pb-none">
          <div class="text-h6"
            >{{ roomOccupancyRoom?.roomNumber }}-xona — bugungi jadval</div
          >
          <q-space />
          <AppButton variant="ghost" icon="close" v-close-popup />
        </q-card-section>
        <q-card-section>
          <div v-if="roomOccupancyLoading" class="column q-gutter-sm">
            <q-skeleton v-for="i in 4" :key="i" type="text" height="36px" />
          </div>
          <div
            v-else-if="!roomOccupancySlots.length"
            class="text-body2 crud-muted q-py-md text-center"
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
  </page-layout>
</template>

<script setup>
// Every list page of the app (config/modules.js decides what each one shows): a header with the
// count, search ("/" jumps into it), Excel / PDF export and "Yangi qo'shish"; optional hero
// numbers; filters as chips with their numbers and the class picker; table or cards; a table with
// typed cells, sort, density, column choice, check boxes and a bulk bar, and a pager (25 a page by
// default). A row opens the record in the side panel, and the same panel edits it (a wide form in
// sections). View, search, filters, sort and page live in the URL; density and columns in this
// browser. The server pages, searches and filters where the module says so (serverSearch);
// otherwise the whole list is read once and searched here.
import { ref, reactive, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'
import { useQuasar } from 'quasar'
import { api } from '@/boot/axios'
import { useAuthStore } from '@/stores/auth'
import { useSchoolStore } from '@/stores/school'
import { getModule } from '@/config/modules'
import PageLayout from '@/components/PageLayout.vue'
import AppButton from '@/components/app/AppButton.vue'
import AppInput from '@/components/app/AppInput.vue'
import AppSelect from '@/components/app/AppSelect.vue'
import SideDrawer from '@/components/dashboard/SideDrawer.vue'
import SegTabs from '@/components/dashboard/SegTabs.vue'
import KpiCard from '@/components/dashboard/KpiCard.vue'
import EmptyState from '@/components/pages/EmptyState.vue'
import CrudFilters from '@/components/crud/CrudFilters.vue'
import CrudTable from '@/components/crud/CrudTable.vue'
import CrudCards from '@/components/crud/CrudCards.vue'
import CrudDetail from '@/components/crud/CrudDetail.vue'
import CrudForm from '@/components/crud/CrudForm.vue'
import {
  PAGE_SIZES,
  cellText,
  rangeLabel,
  readQuery,
  writeQuery,
  todayIso,
  visibleColumnNames,
  maskPhone
} from '@/components/crud/crud'

const route = useRoute()
const router = useRouter()
const $q = useQuasar()
const authStore = useAuthStore()
const schoolStore = useSchoolStore()

const VIEWS = [
  { label: 'Jadval', value: 'table' },
  { label: 'Kartochkalar', value: 'cards' }
]
const DENSITIES = [
  { label: 'Qulay', value: 'comfortable' },
  { label: 'Ixcham', value: 'compact' }
]

const module = computed(() => getModule(route.params.moduleKey))
const today = todayIso()

// ------------------------------------------------------------------ page state (URL)
const filterKeys = computed(() => [
  ...(module.value?.filters || []).map(f => f.key),
  ...(module.value?.toggles || []).map(t => t.key)
])
// a phone opens on cards where the module has them (a wide table is hard to read there)
const defaults = computed(() => ({
  view:
    module.value?.viewType === 'cards' ||
    (module.value?.cards && $q.screen.lt.sm)
      ? 'cards'
      : 'table',
  sort: module.value?.defaultSort
    ? `${module.value.defaultSort},${module.value.defaultDescending ? 'desc' : 'asc'}`
    : '',
  size: 25
}))
const state = reactive({
  view: 'table',
  q: '',
  page: 1,
  size: 25,
  sortBy: null,
  descending: false,
  filters: {}
})
const search = ref('')
const canCards = computed(() => !!module.value?.cards)
const view = computed(() => (canCards.value ? state.view : 'table'))

function syncUrl() {
  const keep = {}
  for (const k of ['create', 'edit'])
    if (route.query[k] != null) keep[k] = route.query[k]
  const q = { ...writeQuery(state, defaults.value), ...keep }
  if (JSON.stringify(q) !== JSON.stringify(route.query))
    router.replace({ query: q })
}

function readUrl() {
  const s = readQuery(route.query, filterKeys.value, defaults.value)
  Object.assign(state, s)
  search.value = s.q
}

// ------------------------------------------------------------------ rows
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const loaded = ref(false)
const error = ref('')
const allCache = ref(null) // the whole list, for modules searched in the browser
let requestNo = 0

const serverSide = computed(() => !!module.value?.serverSearch)
const searchOrFilter = computed(
  () =>
    !!state.q || Object.values(state.filters).some(v => v != null && v !== '')
)
const pages = computed(() => Math.max(1, Math.ceil(total.value / state.size)))
const range = computed(() => rangeLabel(state.page, state.size, total.value))

function baseParams() {
  const params = {}
  if (module.value.schoolScoped) params.schoolId = schoolStore.activeSchoolId
  if (module.value.schoolParam) params.schoolId = schoolStore.activeSchoolId
  for (const [k, v] of Object.entries(state.filters))
    if (v != null && v !== '') params[k] = v
  return params
}

function localSearch(list) {
  const q = state.q.toLowerCase().trim()
  if (!q) return list
  return list.filter(
    row =>
      (module.value.searchText &&
        module.value.searchText(row).toLowerCase().includes(q)) ||
      module.value.columns.some(col => {
        const v = cellText(col, row, today)
        return v && v.toLowerCase().includes(q)
      })
  )
}

async function fetchRows() {
  if (!module.value) return
  if (module.value.schoolScoped && !schoolStore.activeSchoolId) {
    rows.value = []
    total.value = 0
    return
  }
  const no = ++requestNo
  loading.value = true
  error.value = ''
  try {
    if (serverSide.value || !state.q) {
      const params = { ...baseParams(), page: state.page - 1, size: state.size }
      if (serverSide.value && state.q) params.q = state.q
      if (state.sortBy)
        params.sort = `${state.sortBy},${state.descending ? 'desc' : 'asc'}`
      const res = await api.get(module.value.endpoint, { params })
      if (no !== requestNo) return
      rows.value = res.data.content
      total.value = res.data.totalElements
    } else {
      if (!allCache.value) allCache.value = await fetchAll()
      if (no !== requestNo) return
      const found = localSearch(allCache.value)
      total.value = found.length
      rows.value = found.slice(
        (state.page - 1) * state.size,
        state.page * state.size
      )
    }
    // a page past the end (a filter shrank the list): go to the last one
    if (!rows.value.length && total.value && state.page > 1) {
      state.page = Math.max(1, Math.ceil(total.value / state.size))
      syncUrl()
      return fetchRows()
    }
    loaded.value = true
  } catch (e) {
    if (no === requestNo) error.value = extractError(e)
  } finally {
    if (no === requestNo) loading.value = false
  }
}

/** Every row of the current filters (and search, server side) — for search here and the export. */
async function fetchAll() {
  const params = { ...baseParams(), page: 0, size: 5000 }
  if (serverSide.value && state.q) params.q = state.q
  if (state.sortBy)
    params.sort = `${state.sortBy},${state.descending ? 'desc' : 'asc'}`
  const res = await api.get(module.value.endpoint, { params })
  return res.data.content
}

// ------------------------------------------------------------------ filters, facets, classes
const filterOptions = reactive({})
const facets = ref({})
const classes = ref([])
const hero = ref(null)
const hasFilters = computed(
  () => !!(module.value?.filters?.length || module.value?.toggles?.length)
)

async function loadFilterData() {
  const m = module.value
  for (const f of m.filters || []) {
    if (f.picker === 'class') {
      try {
        const res = await api.get('/api/school-classes', {
          params: {
            schoolId: schoolStore.activeSchoolId,
            size: 500,
            sort: 'gradeNumber,asc'
          }
        })
        classes.value = res.data.content
      } catch {
        classes.value = []
      }
      continue
    }
    if (f.options || !f.optionsEndpoint) continue
    try {
      const params = { size: 1000 }
      if (f.schoolScoped) params.schoolId = schoolStore.activeSchoolId
      const res = await api.get(f.optionsEndpoint, { params })
      const items = Array.isArray(res.data) ? res.data : res.data.content
      const opts = items.map(item => ({
        value: String(item[f.optionValue]),
        label:
          typeof f.optionLabel === 'function'
            ? f.optionLabel(item)
            : item[f.optionLabel]
      }))
      if (f.sortOptions)
        opts.sort((a, b) =>
          String(a.label).localeCompare(String(b.label), undefined, {
            numeric: true
          })
        )
      filterOptions[f.key] = opts
    } catch {
      filterOptions[f.key] = []
    }
  }
  await loadFacets()
}

async function loadFacets() {
  if (!module.value.facetsEndpoint) return
  try {
    const res = await api.get(module.value.facetsEndpoint, {
      params: { schoolId: schoolStore.activeSchoolId }
    })
    facets.value = res.data || {}
  } catch {
    facets.value = {}
  }
}

async function loadHero() {
  const h = module.value.hero
  if (!h) return
  try {
    const res = await api.get(h.endpoint, {
      params: { schoolId: schoolStore.activeSchoolId }
    })
    hero.value = res.data
  } catch {
    hero.value = null
  }
}

function onFilter({ key, value }) {
  state.filters = { ...state.filters, [key]: value }
  if (value == null) delete state.filters[key]
  state.page = 1
  reload()
}

function clearFilters() {
  state.filters = {}
  state.page = 1
  reload()
}

function clearAll() {
  search.value = ''
  state.q = ''
  clearFilters()
}

function onSearch(v) {
  state.q = (v || '').trim()
  state.page = 1
  reload()
}

// the whole list read for a search here holds for one set of filters and one sort
let cacheKey = ''
function reload() {
  selected.clear()
  const key = JSON.stringify([state.filters, state.sortBy, state.descending])
  if (serverSide.value || key !== cacheKey) allCache.value = null
  cacheKey = key
  syncUrl()
  fetchRows()
}

function onSort(key) {
  if (state.sortBy === key) state.descending = !state.descending
  else {
    state.sortBy = key
    state.descending = false
  }
  state.page = 1
  reload()
}

function setPage(p) {
  state.page = Math.min(Math.max(1, p), pages.value)
  syncUrl()
  fetchRows()
}

function setSize(s) {
  state.size = Number(s)
  state.page = 1
  syncUrl()
  fetchRows()
}

function setView(v) {
  state.view = v
  syncUrl()
}

// ------------------------------------------------------------------ columns and density (this browser)
const storageKey = k => `crud.${k}.${module.value.key}`
const readStore = (k, d) => {
  try {
    const v = localStorage.getItem(storageKey(k))
    return v == null ? d : JSON.parse(v)
  } catch {
    return d
  }
}
const writeStore = (k, v) => {
  try {
    localStorage.setItem(storageKey(k), JSON.stringify(v))
  } catch {
    // private mode: the choice lasts for this visit only
  }
}
const density = ref('comfortable')
const shown = ref([])
const allColumns = computed(() =>
  module.value.columns.filter(c => c.name !== 'id' && !c.detailOnly)
)
const tableColumns = computed(() =>
  allColumns.value.filter(c => shown.value.includes(c.name))
)

function setDensity(v) {
  density.value = v
  writeStore('density', v)
}

function toggleColumn(name) {
  const next = shown.value.includes(name)
    ? shown.value.filter(n => n !== name)
    : [...shown.value, name]
  if (!next.length) return // at least one column stays
  // keep the module's order
  shown.value = allColumns.value.map(c => c.name).filter(n => next.includes(n))
  writeStore('columns', shown.value)
}

function resetColumns() {
  try {
    localStorage.removeItem(storageKey('columns'))
  } catch {
    // nothing stored
  }
  shown.value = visibleColumnNames(allColumns.value, null)
}

// ------------------------------------------------------------------ selection and bulk
const selected = reactive(new Set())
let lastIndex = -1

function onSelect({ row, on, shift }) {
  const i = rows.value.indexOf(row)
  if (shift && lastIndex >= 0) {
    const [a, b] = [Math.min(i, lastIndex), Math.max(i, lastIndex)]
    for (const r of rows.value.slice(a, b + 1)) mark(r.id, on)
  } else mark(row.id, on)
  lastIndex = i
}

function mark(id, on) {
  if (on) selected.add(id)
  else selected.delete(id)
}

function onSelectAll(on) {
  for (const r of rows.value) mark(r.id, on)
}

async function deleteSelected() {
  const ids = [...selected]
  const soft = module.value.softDelete
  $q.dialog({
    title: soft ? 'Nofaol qilish' : "O'chirish",
    message: `${ids.length} ta yozuv ${soft ? 'nofaol qilinadi' : "o'chiriladi"}. ${soft ? '' : "Ortga qaytarib bo'lmaydi."}`,
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
    const failed = []
    for (const id of ids) {
      try {
        await api.delete(`${module.value.endpoint}/${id}`)
        selected.delete(id)
      } catch (e) {
        failed.push(extractError(e))
      }
    }
    if (failed.length)
      $q.notify({
        type: 'warning',
        message: `${ids.length - failed.length} ta bajarildi, ${failed.length} tasi yo'q: ${failed[0]}`,
        timeout: 6000
      })
    else
      $q.notify({
        type: 'positive',
        message: `${ids.length} ta bajarildi`,
        icon: 'check_circle'
      })
    afterChange()
  })
}

// ------------------------------------------------------------------ export
async function download(rowsToExport, format) {
  const cols = view.value === 'table' ? tableColumns.value : allColumns.value
  const body = {
    title: module.value.title,
    subtitle: [
      schoolStore.activeSchoolName,
      `${rowsToExport.length} ta`,
      today.split('-').reverse().join('.')
    ]
      .filter(Boolean)
      .join(' · '),
    columns: cols.map(c => ({
      label: c.label,
      type: c.type === 'phone' ? 'phone' : 'text'
    })),
    rows: rowsToExport.map(r => cols.map(c => cellText(c, r, today)))
  }
  const res = await api.post('/api/export/table', body, {
    params: { format },
    responseType: 'blob'
  })
  const url = URL.createObjectURL(res.data)
  const a = document.createElement('a')
  a.href = url
  a.download = `${module.value.key}-${today}.${format}`
  document.body.appendChild(a)
  a.click()
  a.remove()
  URL.revokeObjectURL(url)
}

async function exportAll(format) {
  try {
    $q.notify({
      type: 'info',
      message: 'Eksport tayyorlanmoqda…',
      timeout: 900
    })
    const all =
      serverSide.value || !state.q
        ? await fetchAll()
        : localSearch(allCache.value || (await fetchAll()))
    await download(all, format)
  } catch (e) {
    $q.notify({ type: 'negative', message: extractError(e) })
  }
}

async function exportSelected(format) {
  try {
    const ids = new Set(selected)
    let list = rows.value.filter(r => ids.has(r.id))
    if (list.length < ids.size)
      list = (await fetchAll()).filter(r => ids.has(r.id))
    await download(list, format)
  } catch (e) {
    $q.notify({ type: 'negative', message: extractError(e) })
  }
}

// ------------------------------------------------------------------ permissions
const canCreate = computed(() =>
  module.value.adminOnly ? authStore.isAdmin : authStore.isEditor
)
const canEdit = canCreate
const canDelete = computed(() => authStore.isAdmin)

// ------------------------------------------------------------------ the side panel
const panel = reactive({ open: false, mode: 'view', row: null })
const formEl = ref(null)
const formModel = reactive({})
const formError = ref('')
const saving = ref(false)
const fieldOptions = reactive({})
let snapshot = ''

const titleOf = row =>
  module.value.titleOf?.(row) ??
  String(
    row.fullName ??
      row.name ??
      row.title ??
      row.roomNumber ??
      row.username ??
      row.id
  )
const panelTitle = computed(() => {
  if (panel.mode === 'create') return "Yangi qo'shish"
  if (panel.mode === 'edit')
    return `Tahrirlash${panel.row ? ' — ' + titleOf(panel.row) : ''}`
  // reading: the record's name is big in the panel itself; the header names the list
  return module.value.title
})
const formFields = computed(() =>
  module.value.fields.filter(
    f => !f.autoSchool && (!f.showIf || f.showIf(formModel))
  )
)
const dirty = computed(
  () => panel.mode !== 'view' && JSON.stringify(formModel) !== snapshot
)

function openRow(row) {
  if (module.value.key === 'schools') {
    schoolStore.setActiveSchool(row.id, row.name)
    router.push('/')
    return
  }
  if (module.value.rowLink) {
    router.push(`/profiles/${module.value.rowLink}/${row.id}`)
    return
  }
  panel.row = row
  panel.mode = 'view'
  panel.open = true
  loadFieldOptions()
}

async function openCreate() {
  formError.value = ''
  Object.keys(formModel).forEach(k => delete formModel[k])
  for (const f of module.value.fields) {
    if (f.autoSchool) formModel[f.key] = schoolStore.activeSchoolId
    else if (f.type === 'phone') formModel[f.key] = '+998'
  }
  await loadFieldOptions()
  snapshot = JSON.stringify(formModel)
  panel.row = null
  panel.mode = 'create'
  panel.open = true
}

async function openEdit(row) {
  formError.value = ''
  await loadFieldOptions()
  Object.keys(formModel).forEach(k => delete formModel[k])
  for (const f of module.value.fields) {
    if (f.type === 'password') formModel[f.key] = ''
    else if (f.type === 'phone')
      formModel[f.key] = row[f.key] ? maskPhone(row[f.key]) : '+998'
    else formModel[f.key] = row[f.key]
  }
  snapshot = JSON.stringify(formModel)
  panel.row = row
  panel.mode = 'edit'
  panel.open = true
}

function closePanel() {
  if (!dirty.value) {
    panel.open = false
    return
  }
  $q.dialog({
    title: "Saqlanmagan o'zgarishlar",
    message: 'Kiritganlaringiz saqlanmaydi. Chiqasizmi?',
    cancel: { flat: true, label: 'Qolish', color: 'grey-7', noCaps: true },
    ok: { label: 'Chiqish', color: 'negative', unelevated: true, noCaps: true },
    persistent: true
  }).onOk(() => {
    snapshot = JSON.stringify(formModel)
    panel.open = false
  })
}

// a select with parentKey (tuman → hudud) offers only the options of the chosen parent
function optionsFor(field) {
  const all = fieldOptions[field.key] || []
  if (!field.parentKey) return all
  const parent = formModel[field.parentKey]
  return parent == null ? [] : all.filter(o => o.parent === parent)
}

// choosing another hudud drops a tuman that is not in it
function clearOrphanChildren(parentField) {
  for (const child of module.value.fields) {
    if (child.parentKey !== parentField.key) continue
    const value = formModel[child.key]
    if (value != null && !optionsFor(child).some(o => o.value === value))
      formModel[child.key] = null
  }
}

async function loadFieldOptions() {
  for (const field of module.value.fields) {
    if (field.autoSchool || field.type !== 'select' || fieldOptions[field.key])
      continue
    if (field.options) {
      fieldOptions[field.key] = field.options.map(o =>
        typeof o === 'object' ? o : { label: o, value: o }
      )
      continue
    }
    if (!field.optionsEndpoint) continue
    try {
      const params = { size: 1000 }
      if (field.schoolScoped) params.schoolId = schoolStore.activeSchoolId
      const response = await api.get(field.optionsEndpoint, { params })
      // paged lists come as { content }, reference lists (regions, districts) as a plain array
      const items = Array.isArray(response.data)
        ? response.data
        : response.data.content
      fieldOptions[field.key] = items.map(item => ({
        value: item[field.optionValue],
        label:
          typeof field.optionLabel === 'function'
            ? field.optionLabel(item)
            : item[field.optionLabel],
        parent: field.parentKey ? item[field.parentValue] : undefined
      }))
    } catch {
      fieldOptions[field.key] = []
    }
  }
}

async function onSave() {
  if (saving.value) return // Enter + click must not create two rows
  saving.value = true
  formError.value = ''
  try {
    const payload = { ...formModel }
    for (const f of module.value.fields) {
      if (f.showIf && !f.showIf(formModel)) payload[f.key] = null
      // a phone is stored as +998901234567 (the shown spaces are for people)
      if (f.type === 'phone') {
        const d = String(payload[f.key] || '').replace(/\D/g, '')
        payload[f.key] = d.length > 3 ? `+${d}` : null
      }
    }
    if (
      module.value.key === 'users' &&
      panel.mode === 'edit' &&
      !payload.password
    )
      delete payload.password
    let saved
    if (panel.mode === 'edit')
      saved = await api.put(`${module.value.endpoint}/${panel.row.id}`, payload)
    else saved = await api.post(module.value.endpoint, payload)
    snapshot = JSON.stringify(formModel)
    $q.notify({
      type: 'positive',
      message: 'Muvaffaqiyatli saqlandi',
      icon: 'check_circle'
    })
    if (module.value.key === 'schools') await schoolStore.fetchSchools(api)
    // after an edit the panel shows the saved record
    if (
      panel.mode === 'edit' &&
      saved?.data &&
      typeof saved.data === 'object'
    ) {
      panel.row = { ...panel.row, ...saved.data }
      panel.mode = 'view'
    } else panel.open = false
    afterChange()
  } catch (e) {
    formError.value = extractError(e)
  } finally {
    saving.value = false
  }
}

function afterChange() {
  allCache.value = null
  fetchRows()
  loadFacets()
}

// ------------------------------------------------------------------ delete (1.8)
const busyRowId = ref(null)

function confirmDelete(row) {
  const m = module.value
  const soft = m.softDelete
  const impact = m.deleteImpact?.(row) || null
  if (impact?.blocked) {
    $q.dialog({
      title: "O'chirib bo'lmaydi",
      message: impact.message,
      ok: {
        label: 'Tushunarli',
        color: 'primary',
        unelevated: true,
        noCaps: true
      }
    })
    return
  }
  const deactivate =
    m.deactivate && m.deactivate.applies(row) ? m.deactivate : null
  const message = [
    impact?.message,
    soft
      ? "Nofaol bo'ladi: yangi baho va jadvalda tanlanmaydi, eski yozuvlarda nomi saqlanadi. Keyin qayta faollashtirish mumkin."
      : "Haqiqatan ham o'chirmoqchimisiz? Bu amalni ortga qaytarib bo'lmaydi.",
    deactivate
      ? `O'chirish o'rniga «${deactivate.label}» — ma'lumot saqlanib qoladi.`
      : ''
  ]
    .filter(Boolean)
    .join('\n\n')
  const dlg = $q.dialog({
    title: `${soft ? 'Nofaol qilish' : "O'chirish"}: ${titleOf(row)}`,
    message,
    style: 'white-space: pre-line',
    options: deactivate
      ? {
          type: 'radio',
          model: 'deactivate',
          items: [
            { label: deactivate.label, value: 'deactivate' },
            { label: "Butunlay o'chirish", value: 'delete' }
          ]
        }
      : undefined,
    cancel: {
      flat: true,
      label: 'Bekor qilish',
      color: 'grey-7',
      noCaps: true
    },
    ok: {
      label: soft ? 'Nofaol qilish' : 'Davom etish',
      color: 'negative',
      unelevated: true,
      noCaps: true
    },
    persistent: true
  })
  dlg.onOk(async choice => {
    if (busyRowId.value === row.id) return // a second click while the first is on its way
    busyRowId.value = row.id
    try {
      if (deactivate && choice === 'deactivate') {
        await api.put(`${m.endpoint}/${row.id}`, deactivate.payload(row))
        $q.notify({
          type: 'positive',
          message: `${deactivate.label} — saqlandi`,
          icon: 'check_circle'
        })
      } else {
        await api.delete(`${m.endpoint}/${row.id}`)
        $q.notify({
          type: 'positive',
          message: soft ? 'Nofaol qilindi' : "Muvaffaqiyatli o'chirildi",
          icon: 'check_circle'
        })
      }
      if (m.key === 'schools') await schoolStore.fetchSchools(api)
      selected.delete(row.id)
      if (panel.row?.id === row.id) panel.open = false
      afterChange()
    } catch (e) {
      // 409: the backend says why (e.g. "Bu lavozimda 12 ta xodim bor")
      $q.dialog({
        title: e.response?.status === 409 ? "O'chirib bo'lmaydi" : 'Xatolik',
        message: extractError(e),
        ok: {
          label: 'Tushunarli',
          color: 'primary',
          unelevated: true,
          noCaps: true
        }
      })
    } finally {
      busyRowId.value = null
    }
  })
}

// ------------------------------------------------------------------ module row actions
function extraActions(row) {
  const out = []
  if (module.value.key === 'subjects' && row.active === false && canEdit.value)
    out.push({ key: 'restore', label: 'Qayta faollashtirish', icon: 'restore' })
  if (module.value.key === 'rooms')
    out.push({ key: 'occupancy', label: 'Bugungi jadval', icon: 'schedule' })
  for (const a of module.value.rowActions || [])
    if (!a.show || a.show(row)) out.push(a)
  return out
}

async function onAction({ key, row }) {
  if (key === 'delete') return confirmDelete(row)
  if (key === 'occupancy') return openRoomOccupancy(row)
  if (key === 'restore') {
    try {
      await api.put(`${module.value.endpoint}/${row.id}/activate`)
      $q.notify({ type: 'positive', message: 'Qayta faollashtirildi' })
      afterChange()
    } catch (e) {
      $q.notify({ type: 'negative', message: extractError(e) })
    }
    return
  }
  const own = (module.value.rowActions || []).find(a => a.key === key)
  if (own) own.run({ row, api, router, reload: afterChange, $q })
}

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

function goToProfile(link, row) {
  const id = row[link.idField]
  if (id != null) router.push(`/profiles/${link.type}/${id}`)
}

function extractError(e) {
  const d = e?.response?.data
  if (typeof d === 'string' && d) return d
  if (d && typeof d === 'object' && d.message) return d.message
  return e?.message || 'Xatolik yuz berdi'
}

// ------------------------------------------------------------------ lifecycle
async function enterModule() {
  if (!module.value) return
  panel.open = false
  selected.clear()
  rows.value = []
  total.value = 0
  loaded.value = false
  allCache.value = null
  facets.value = {}
  hero.value = null
  classes.value = []
  Object.keys(filterOptions).forEach(k => delete filterOptions[k])
  Object.keys(fieldOptions).forEach(k => delete fieldOptions[k])
  readUrl()
  cacheKey = JSON.stringify([state.filters, state.sortBy, state.descending])
  density.value = readStore('density', 'comfortable')
  shown.value = visibleColumnNames(allColumns.value, readStore('columns', null))
  if (!module.value.schoolScoped || schoolStore.activeSchoolId) {
    loadFilterData()
    loadHero()
  }
  await fetchRows()
  if (route.query.create === '1' && canCreate.value) {
    router.replace({ query: writeQuery(state, defaults.value) })
    openCreate()
  }
  // ?edit=<id> opens that record's form (e.g. "Biriktirish" from the journal's class panel)
  if (route.query.edit && canEdit.value) {
    const id = Number(route.query.edit)
    router.replace({ query: writeQuery(state, defaults.value) })
    try {
      const res = await api.get(`${module.value.endpoint}/${id}`)
      openEdit(res.data)
    } catch {
      // the record is gone or not ours — just show the list
    }
  }
}

watch(() => route.params.moduleKey, enterModule, { immediate: true })

// back / forward in the browser: take the state from the URL again
watch(
  () => route.query,
  q => {
    if (!module.value || route.params.moduleKey !== module.value.key) return
    const now = JSON.stringify(writeQuery(state, defaults.value))
    const next = JSON.stringify(
      writeQuery(readQuery(q, filterKeys.value, defaults.value), defaults.value)
    )
    if (now === next) return
    readUrl()
    allCache.value = null
    fetchRows()
  }
)

watch(
  () => schoolStore.activeSchoolId,
  () => {
    if (!module.value?.schoolScoped) return
    state.page = 1
    state.filters = {}
    syncUrl()
    enterModule()
  }
)

// "/" puts the cursor into the search (not while typing somewhere)
const searchEl = ref(null)
function onKey(e) {
  if (e.key !== '/' || e.ctrlKey || e.metaKey || e.altKey) return
  const t = e.target
  if (
    t &&
    (t.isContentEditable || ['INPUT', 'TEXTAREA', 'SELECT'].includes(t.tagName))
  )
    return
  if (panel.open) return
  e.preventDefault()
  searchEl.value?.focus()
}
onMounted(() => window.addEventListener('keydown', onKey))
onBeforeUnmount(() => window.removeEventListener('keydown', onKey))

// leaving the page with an unsaved form asks first
onBeforeRouteLeave(() => {
  if (!dirty.value || !panel.open) return true
  return window.confirm("Saqlanmagan o'zgarishlar bor. Sahifadan chiqasizmi?")
})
</script>

<style scoped>
.crud {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  padding-bottom: var(--space-8);
}

.crud-sub {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
}

.crud-sub__skel {
  display: inline-block;
}

.crud-search {
  width: var(--field-w-sm);
}

.crud-kbd {
  display: inline-block;
  min-width: var(--space-5);
  padding: 0 var(--space-1);
  border: var(--hairline) solid var(--brand-border);
  border-radius: var(--radius-sm);
  color: var(--brand-text-muted);
  font: inherit;
  font-size: var(--text-xs);
  text-align: center;
}

.crud-menu {
  min-width: calc(var(--space-8) * 4);
}

.crud-banner {
  background: color-mix(in srgb, var(--gr-3) 14%, transparent);
  color: var(--text-primary);
}

.crud-muted {
  color: var(--brand-text-muted);
}

.crud-hero {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(220px, 100%), 1fr));
  gap: var(--space-4);
}

.crud-bar {
  display: flex;
  flex-wrap: nowrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-3);
  padding: var(--space-3) var(--space-4);
}

.crud-bar:hover {
  transform: none;
}

.crud-bar--plain {
  justify-content: flex-end;
  padding: 0;
}

.crud-bar__filters {
  flex: 1 1 0;
  min-width: 0;
}

.crud-bar__tools {
  display: flex;
  flex: none;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-2);
  margin-left: auto;
}

.crud-table-card {
  overflow: hidden;
}

.crud-table-card:hover {
  transform: none;
}

.crud-state:hover,
.crud-skel:hover {
  transform: none;
}

.crud-skel {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  padding: var(--space-4);
}

.crud-skel__head {
  height: var(--control-h);
}

.crud-skel__row {
  height: var(--crud-row-h);
}

.crud-skel__cards {
  display: grid;
  grid-template-columns: repeat(
    auto-fill,
    minmax(min(var(--crud-card-min), 100%), 1fr)
  );
  gap: var(--space-4);
}

.crud-skel__card {
  height: calc(var(--crud-row-h) * 3);
  border-radius: var(--radius-lg);
}

.crud-pager {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: var(--space-1) var(--space-2);
  color: var(--brand-text-muted);
  font-size: var(--text-sm);
}

.crud-pager__size {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  margin-right: auto;
}

.crud-pager__select {
  width: calc(var(--space-8) * 1.75);
}

.crud-pager__range,
.crud-pager__page {
  color: var(--text-primary);
  font-weight: 600;
}

.tnum {
  font-variant-numeric: tabular-nums;
}

.crud-bulk {
  position: fixed;
  left: 50%;
  bottom: var(--space-5);
  z-index: 2000;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-2);
  max-width: calc(100vw - var(--space-6));
  padding: var(--space-2) var(--space-3) var(--space-2) var(--space-4);
  border: var(--hairline) solid var(--dash-edge);
  border-radius: var(--radius-lg);
  background: var(--card-bg);
  box-shadow: var(--dash-shadow-hover);
  transform: translateX(-50%);
}

.crud-bulk-enter-active,
.crud-bulk-leave-active {
  transition:
    opacity var(--dur-fast) var(--ease-out),
    transform var(--dur-fast) var(--ease-out);
}

.crud-bulk-enter-from,
.crud-bulk-leave-to {
  opacity: 0;
  transform: translate(-50%, var(--space-3));
}

.crud-dirty {
  color: var(--st-late);
  font-size: var(--text-xs);
  font-weight: 600;
}

.occupancy-slot--current {
  background: color-mix(in srgb, var(--st-present) 8%, transparent);
  border-radius: var(--radius-sm);
}

@media (max-width: 599px) {
  .crud-search {
    width: 100%;
  }

  .crud-bar {
    flex-wrap: wrap;
  }

  .crud-bar__tools {
    margin-left: 0;
  }

  .crud-pager__size {
    width: 100%;
  }
}
</style>

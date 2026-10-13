<template>
  <page-layout
    icon="local_library"
    color="#6366f1"
    title="Kutubxona"
    :subtitle="`Ota-onalar botidagi «📚 Kitob mutolaasi» kitoblari · ${schoolStore.activeSchoolName || ''}`"
  >
    <template #actions>
      <q-btn-toggle
        v-model="view"
        dense
        unelevated
        no-caps
        toggle-color="primary"
        :options="[
          { value: 'grid', icon: 'grid_view', slot: 'grid' },
          { value: 'table', icon: 'table_rows', slot: 'table' }
        ]"
        aria-label="Ko'rinish"
      >
        <template #grid><q-tooltip>Kartochkalar</q-tooltip></template>
        <template #table><q-tooltip>Jadval</q-tooltip></template>
      </q-btn-toggle>
      <q-btn
        outline
        color="primary"
        no-caps
        icon="drive_folder_upload"
        label="Ommaviy yuklash"
        @click="openBulk"
      />
      <q-btn
        unelevated
        color="primary"
        no-caps
        icon="add"
        label="Kitob qo'shish"
        @click="openCreate"
      />
    </template>

    <!-- filters -->
    <div class="brand-card q-pa-md q-mb-md">
      <div class="row q-col-gutter-sm items-center">
        <div class="col-12 col-md-4">
          <q-input
            v-model="filters.q"
            dense
            outlined
            clearable
            debounce="350"
            placeholder="Nomi yoki muallif bo'yicha qidirish"
            aria-label="Qidirish"
          >
            <template #prepend><q-icon name="search" /></template>
          </q-input>
        </div>
        <div class="col-6 col-md-2">
          <q-select
            v-model="filters.categoryId"
            dense
            outlined
            clearable
            emit-value
            map-options
            :options="categoryOptions"
            label="Bo'lim"
          />
        </div>
        <div class="col-6 col-md-2">
          <q-select
            v-model="filters.language"
            dense
            outlined
            clearable
            emit-value
            map-options
            :options="languages"
            label="Til"
          />
        </div>
        <div class="col-6 col-md-2">
          <q-select
            v-model="filters.ageGroup"
            dense
            outlined
            clearable
            emit-value
            map-options
            :options="ageGroups"
            label="Yosh toifasi"
          />
        </div>
        <div class="col-6 col-md-2">
          <q-select
            v-model="filters.legalBasis"
            dense
            outlined
            clearable
            emit-value
            map-options
            :options="legalBases"
            label="Huquqiy asos"
          />
        </div>
      </div>
      <div class="row items-center q-mt-sm q-gutter-sm">
        <q-btn-toggle
          v-model="filters.status"
          dense
          unelevated
          no-caps
          rounded
          toggle-color="primary"
          :options="statusOptions"
        />
        <q-space />
        <span class="text-caption muted-text tabular-nums"
          >Jami: {{ total }} ta</span
        >
      </div>
    </div>

    <!-- grid -->
    <template v-if="view === 'grid'">
      <div v-if="loading && !books.length" class="row q-col-gutter-md">
        <div v-for="n in 8" :key="n" class="col-6 col-sm-4 col-md-3 col-xl-2">
          <q-skeleton height="300px" class="rounded-borders" />
        </div>
      </div>
      <div v-else-if="!books.length" class="brand-card q-pa-xl text-center">
        <q-icon name="local_library" size="56px" color="grey-5" />
        <div class="text-subtitle1 q-mt-sm">Kitob topilmadi</div>
        <div class="text-caption muted-text q-mb-md">
          Filtrni o'zgartiring yoki birinchi kitobni qo'shing.
        </div>
        <q-btn
          unelevated
          color="primary"
          no-caps
          icon="add"
          label="Kitob qo'shish"
          @click="openCreate"
        />
      </div>
      <div v-else class="row q-col-gutter-md">
        <div
          v-for="b in books"
          :key="b.id"
          class="col-6 col-sm-4 col-md-3 col-xl-2"
        >
          <div
            class="brand-card book-card cursor-pointer"
            :class="{ 'book-card--off': !b.active || b.draft }"
            role="button"
            tabindex="0"
            :aria-label="b.title"
            @click="openEdit(b)"
            @keyup.enter="openEdit(b)"
          >
            <div class="book-card__cover">
              <book-cover
                :book-id="b.id"
                :has-cover="b.hasCover"
                :title="b.title"
                :version="coverVersion(b)"
              />
              <q-badge
                v-if="b.draft"
                color="orange"
                class="book-card__badge"
                label="Qoralama"
              />
              <q-badge
                v-else-if="!b.active"
                color="grey-7"
                class="book-card__badge"
                label="Nofaol"
              />
              <q-badge
                v-else-if="b.featured"
                color="amber-8"
                class="book-card__badge"
                label="⭐ Tavsiya"
              />
            </div>
            <div class="q-pa-sm">
              <div class="book-card__title" :title="b.title">{{ b.title }}</div>
              <div class="text-caption muted-text ellipsis">
                {{ b.author || "Muallif ko'rsatilmagan" }}
              </div>
              <div class="row items-center no-wrap q-mt-xs text-caption">
                <span class="ellipsis col">
                  {{ b.categoryEmoji }} {{ b.categoryName || "Bo'limsiz" }}
                </span>
                <span class="tabular-nums muted-text q-ml-xs">
                  <q-icon name="download" size="14px" />
                  {{ b.downloadCount }}
                </span>
              </div>
            </div>
            <q-btn
              flat
              round
              dense
              size="sm"
              icon="more_vert"
              class="book-card__menu"
              aria-label="Amallar"
              @click.stop
            >
              <q-menu auto-close>
                <q-list dense style="min-width: 180px">
                  <q-item clickable @click="viewPdf(b)">
                    <q-item-section avatar
                      ><q-icon name="picture_as_pdf"
                    /></q-item-section>
                    <q-item-section>PDF ni ko'rish</q-item-section>
                  </q-item>
                  <q-item clickable @click="openEdit(b)">
                    <q-item-section avatar
                      ><q-icon name="edit"
                    /></q-item-section>
                    <q-item-section>Tahrirlash</q-item-section>
                  </q-item>
                  <q-item
                    v-if="b.active"
                    clickable
                    class="text-negative"
                    @click="deactivate(b)"
                  >
                    <q-item-section avatar
                      ><q-icon name="visibility_off"
                    /></q-item-section>
                    <q-item-section>Nofaol qilish</q-item-section>
                  </q-item>
                </q-list>
              </q-menu>
            </q-btn>
          </div>
        </div>
      </div>
      <div v-if="pages > 1" class="row justify-center q-mt-md">
        <q-pagination
          v-model="page"
          :max="pages"
          :max-pages="7"
          direction-links
          boundary-links
        />
      </div>
    </template>

    <!-- table -->
    <div v-else class="brand-card">
      <q-table
        v-model:pagination="tablePagination"
        flat
        :rows="books"
        :columns="columns"
        row-key="id"
        :loading="loading"
        :rows-per-page-options="[12, 24, 48, 96]"
        no-data-label="Kitob topilmadi"
        @request="onTableRequest"
        @row-click="(_, row) => openEdit(row)"
      >
        <template #body-cell-cover="p">
          <q-td :props="p" style="width: 56px">
            <book-cover
              :book-id="p.row.id"
              :has-cover="p.row.hasCover"
              :title="p.row.title"
              :version="coverVersion(p.row)"
              width="40px"
              height="54px"
            />
          </q-td>
        </template>
        <template #body-cell-title="p">
          <q-td :props="p">
            <div class="text-weight-semibold">{{ p.row.title }}</div>
            <div class="text-caption muted-text">{{ p.row.author || '—' }}</div>
          </q-td>
        </template>
        <template #body-cell-status="p">
          <q-td :props="p">
            <q-badge v-if="p.row.draft" color="orange" label="Qoralama" />
            <q-badge v-else-if="!p.row.active" color="grey-7" label="Nofaol" />
            <q-badge v-else color="positive" label="Faol" />
            <q-badge
              v-if="p.row.featured"
              color="amber-8"
              class="q-ml-xs"
              label="⭐"
            />
          </q-td>
        </template>
        <template #body-cell-actions="p">
          <q-td :props="p" auto-width>
            <q-btn
              flat
              round
              dense
              icon="picture_as_pdf"
              aria-label="PDF ni ko'rish"
              @click.stop="viewPdf(p.row)"
            >
              <q-tooltip>PDF ni ko'rish</q-tooltip>
            </q-btn>
            <q-btn
              v-if="p.row.active"
              flat
              round
              dense
              color="negative"
              icon="visibility_off"
              aria-label="Nofaol qilish"
              @click.stop="deactivate(p.row)"
            >
              <q-tooltip>Nofaol qilish</q-tooltip>
            </q-btn>
          </q-td>
        </template>
      </q-table>
    </div>

    <!-- create / edit -->
    <q-dialog v-model="formOpen" persistent>
      <q-card class="book-form">
        <q-card-section class="row items-center q-pb-none">
          <q-icon
            :name="form.id ? 'edit' : 'add_circle'"
            color="primary"
            size="24px"
            class="q-mr-sm"
          />
          <div class="text-h6">
            {{ form.id ? 'Kitobni tahrirlash' : 'Yangi kitob' }}
          </div>
          <q-badge
            v-if="form.draft"
            color="orange"
            class="q-ml-sm"
            label="Qoralama — saqlansa faol bo'ladi"
          />
          <q-space />
          <q-btn
            flat
            round
            dense
            icon="close"
            aria-label="Yopish"
            v-close-popup
          />
        </q-card-section>

        <q-card-section>
          <q-banner dense rounded class="legal-banner q-mb-md">
            <template #avatar><q-icon name="gavel" color="warning" /></template>
            <b>{{ LEGAL_WARNING }}</b>
            Bot kitoblarni faqat shu yerda yuklangan fayllardan beradi —
            internetdan hech narsa olinmaydi.
          </q-banner>

          <div class="row q-col-gutter-md">
            <!-- files -->
            <div class="col-12 col-md-4">
              <div class="text-caption text-weight-semibold q-mb-xs">
                PDF fayl {{ form.id ? '' : '*' }}
              </div>
              <div
                class="drop-zone"
                :class="{
                  'drop-zone--over': dragPdf,
                  'drop-zone--ok': pdfFile
                }"
                role="button"
                tabindex="0"
                aria-label="PDF faylni tanlash"
                @click="pdfInput.click()"
                @keyup.enter="pdfInput.click()"
                @dragover.prevent="dragPdf = true"
                @dragleave.prevent="dragPdf = false"
                @drop.prevent="onPdfDrop"
              >
                <q-icon
                  :name="pdfFile ? 'task' : 'picture_as_pdf'"
                  size="36px"
                  :color="pdfFile ? 'positive' : 'red-5'"
                />
                <div v-if="pdfFile" class="text-caption ellipsis full-width">
                  {{ pdfFile.name }} · {{ formatSize(pdfFile.size) }}
                </div>
                <div v-else class="text-caption">
                  PDF ni shu yerga tashlang yoki bosing
                  <div class="muted-text"
                    >faqat PDF, {{ MAX_PDF_MB }} MB gacha</div
                  >
                </div>
                <div
                  v-if="form.id && !pdfFile"
                  class="text-caption muted-text q-mt-xs"
                >
                  Hozirgi: {{ formatSize(form.pdfSize) }}
                </div>
              </div>
              <input
                ref="pdfInput"
                type="file"
                accept="application/pdf,.pdf"
                class="hidden"
                @change="e => pickPdf(e.target.files[0])"
              />
              <q-btn
                v-if="form.id"
                flat
                dense
                no-caps
                size="sm"
                icon="open_in_new"
                label="Hozirgi PDF ni ko'rish"
                class="q-mt-xs"
                @click="viewPdf(form)"
              />
              <q-banner
                v-if="duplicates.length"
                dense
                rounded
                class="bg-orange-1 text-orange-10 q-mt-sm"
              >
                <template #avatar
                  ><q-icon name="content_copy" color="orange"
                /></template>
                Bu PDF allaqachon yuklangan:
                <div v-for="d in duplicates" :key="d.id">
                  • «{{ d.title }}»{{ d.active ? '' : ' (nofaol)' }}
                </div>
              </q-banner>

              <div class="text-caption text-weight-semibold q-mt-md q-mb-xs">
                Muqova (ixtiyoriy)
              </div>
              <div
                class="drop-zone drop-zone--cover"
                :class="{ 'drop-zone--over': dragCover }"
                role="button"
                tabindex="0"
                aria-label="Muqovani tanlash"
                @click="coverInput.click()"
                @keyup.enter="coverInput.click()"
                @dragover.prevent="dragCover = true"
                @dragleave.prevent="dragCover = false"
                @drop.prevent="onCoverDrop"
              >
                <img
                  v-if="coverPreview"
                  :src="coverPreview"
                  alt="Muqova"
                  class="cover-preview"
                />
                <book-cover
                  v-else-if="form.id && form.hasCover && !removeCover"
                  :book-id="form.id"
                  :has-cover="true"
                  :title="form.title"
                  :version="coverVersion(form)"
                  width="96px"
                  height="128px"
                />
                <template v-else>
                  <q-icon name="image" size="30px" color="grey-6" />
                  <div class="text-caption">
                    JPG/PNG, {{ MAX_COVER_MB }} MB gacha
                    <div class="muted-text">
                      Bo'lmasa — PDF ning 1-sahifasidan avtomatik
                    </div>
                  </div>
                </template>
              </div>
              <input
                ref="coverInput"
                type="file"
                accept="image/jpeg,image/png"
                class="hidden"
                @change="e => pickCover(e.target.files[0])"
              />
              <q-checkbox
                v-if="form.id && form.hasCover && !coverFile"
                v-model="removeCover"
                dense
                class="q-mt-xs text-caption"
                label="Muqovani PDF dan qayta chizish"
              />
            </div>

            <!-- fields -->
            <div class="col-12 col-md-8">
              <div class="row q-col-gutter-sm">
                <div class="col-12">
                  <q-input
                    v-model="form.title"
                    dense
                    outlined
                    label="Kitob nomi *"
                    maxlength="255"
                    :rules="[
                      v => !!(v && v.trim()) || 'Nomi kiritilishi shart'
                    ]"
                    lazy-rules
                  />
                </div>
                <div class="col-12 col-sm-7">
                  <q-input
                    v-model="form.author"
                    dense
                    outlined
                    label="Muallif"
                    maxlength="255"
                  />
                </div>
                <div class="col-12 col-sm-5">
                  <q-select
                    v-model="form.categoryId"
                    dense
                    outlined
                    clearable
                    emit-value
                    map-options
                    :options="categoryOptions"
                    label="Bo'lim"
                  />
                </div>
                <div class="col-6 col-sm-3">
                  <q-select
                    v-model="form.language"
                    dense
                    outlined
                    emit-value
                    map-options
                    :options="languages"
                    label="Til"
                  />
                </div>
                <div class="col-6 col-sm-3">
                  <q-select
                    v-model="form.ageGroup"
                    dense
                    outlined
                    emit-value
                    map-options
                    :options="ageGroups"
                    label="Yosh toifasi"
                  />
                </div>
                <div class="col-6 col-sm-3">
                  <q-input
                    v-model.number="form.pages"
                    dense
                    outlined
                    type="number"
                    min="1"
                    label="Bet soni"
                    hint="Bo'sh bo'lsa — PDF dan"
                  />
                </div>
                <div class="col-6 col-sm-3">
                  <q-input
                    v-model.number="form.year"
                    dense
                    outlined
                    type="number"
                    min="1000"
                    max="2100"
                    label="Yili"
                  />
                </div>
                <div class="col-12">
                  <q-input
                    v-model="form.description"
                    dense
                    outlined
                    autogrow
                    type="textarea"
                    maxlength="2000"
                    counter
                    label="Qisqa annotatsiya"
                    hint="Botda 600 belgigacha ko'rsatiladi"
                  />
                </div>
                <div class="col-12 col-sm-6">
                  <q-select
                    v-model="form.legalBasis"
                    dense
                    outlined
                    emit-value
                    map-options
                    :options="legalBases"
                    label="Huquqiy asos *"
                    :rules="[v => !!v || 'Huquqiy asos tanlanishi shart']"
                    lazy-rules
                  >
                    <template #option="scope">
                      <q-item v-bind="scope.itemProps">
                        <q-item-section>
                          <q-item-label>{{ scope.opt.label }}</q-item-label>
                          <q-item-label v-if="scope.opt.hint" caption>{{
                            scope.opt.hint
                          }}</q-item-label>
                        </q-item-section>
                      </q-item>
                    </template>
                  </q-select>
                </div>
                <div class="col-12 col-sm-6">
                  <q-input
                    v-model="form.legalNote"
                    dense
                    outlined
                    maxlength="500"
                    label="Izoh (masalan, ruxsat xati raqami)"
                  />
                </div>
                <div class="col-12 row q-gutter-md">
                  <q-toggle
                    v-model="form.active"
                    label="Faol (botda ko'rinadi)"
                  />
                  <q-toggle
                    v-model="form.featured"
                    color="amber-8"
                    label="⭐ Maktab tavsiya qiladi"
                  />
                </div>
              </div>
            </div>
          </div>
        </q-card-section>

        <q-card-actions align="right" class="q-px-md q-pb-md">
          <q-btn
            v-if="form.id && form.active"
            flat
            no-caps
            color="negative"
            icon="visibility_off"
            label="Nofaol qilish"
            @click="deactivate(form, true)"
          />
          <q-space />
          <q-btn flat no-caps label="Bekor qilish" v-close-popup />
          <q-btn
            unelevated
            no-caps
            color="primary"
            icon="save"
            label="Saqlash"
            :loading="saving"
            @click="save"
          />
        </q-card-actions>
      </q-card>
    </q-dialog>

    <!-- bulk upload -->
    <q-dialog v-model="bulkOpen" :persistent="bulkRunning">
      <q-card class="bulk-form">
        <q-card-section class="row items-center q-pb-none">
          <q-icon
            name="drive_folder_upload"
            color="primary"
            size="24px"
            class="q-mr-sm"
          />
          <div class="text-h6">Ommaviy yuklash</div>
          <q-space />
          <q-btn
            flat
            round
            dense
            icon="close"
            aria-label="Yopish"
            :disable="bulkRunning"
            v-close-popup
          />
        </q-card-section>
        <q-card-section>
          <q-banner dense rounded class="legal-banner q-mb-md">
            <template #avatar><q-icon name="gavel" color="warning" /></template>
            <b>{{ LEGAL_WARNING }}</b>
            Har bir PDF uchun qoralama kartochka yaratiladi (nomi fayl nomidan)
            — keyin to'ldirib saqlaysiz. Qoralamalar botda ko'rinmaydi.
          </q-banner>
          <div class="row q-col-gutter-sm">
            <div class="col-12 col-sm-6">
              <q-select
                v-model="bulk.legalBasis"
                dense
                outlined
                emit-value
                map-options
                :options="legalBases"
                label="Huquqiy asos (hammasiga) *"
              />
            </div>
            <div class="col-12 col-sm-6">
              <q-input
                v-model="bulk.legalNote"
                dense
                outlined
                maxlength="500"
                label="Izoh"
              />
            </div>
            <div class="col-12 col-sm-4">
              <q-select
                v-model="bulk.categoryId"
                dense
                outlined
                clearable
                emit-value
                map-options
                :options="categoryOptions"
                label="Bo'lim"
              />
            </div>
            <div class="col-6 col-sm-4">
              <q-select
                v-model="bulk.language"
                dense
                outlined
                emit-value
                map-options
                :options="languages"
                label="Til"
              />
            </div>
            <div class="col-6 col-sm-4">
              <q-select
                v-model="bulk.ageGroup"
                dense
                outlined
                emit-value
                map-options
                :options="ageGroups"
                label="Yosh toifasi"
              />
            </div>
          </div>
          <div
            class="drop-zone q-mt-md"
            :class="{ 'drop-zone--over': dragBulk }"
            role="button"
            tabindex="0"
            aria-label="PDF fayllarni tanlash"
            @click="bulkInput.click()"
            @keyup.enter="bulkInput.click()"
            @dragover.prevent="dragBulk = true"
            @dragleave.prevent="dragBulk = false"
            @drop.prevent="onBulkDrop"
          >
            <q-icon name="library_add" size="36px" color="primary" />
            <div class="text-caption">
              Bir nechta PDF ni shu yerga tashlang yoki bosing
            </div>
          </div>
          <input
            ref="bulkInput"
            type="file"
            multiple
            accept="application/pdf,.pdf"
            class="hidden"
            @change="e => addBulk(e.target.files)"
          />
          <q-list
            v-if="bulkFiles.length"
            dense
            separator
            class="q-mt-sm bulk-list"
          >
            <q-item v-for="(f, i) in bulkFiles" :key="i">
              <q-item-section avatar>
                <q-spinner
                  v-if="f.state === 'uploading'"
                  size="18px"
                  color="primary"
                />
                <q-icon
                  v-else
                  :name="bulkIcon(f.state)"
                  :color="bulkColor(f.state)"
                />
              </q-item-section>
              <q-item-section>
                <q-item-label class="ellipsis">{{ f.file.name }}</q-item-label>
                <q-item-label caption>
                  {{ formatSize(f.file.size) }}
                  <span v-if="f.error" class="text-negative">
                    · {{ f.error }}</span
                  >
                </q-item-label>
              </q-item-section>
              <q-item-section v-if="f.state === 'waiting' && !bulkRunning" side>
                <q-btn
                  flat
                  round
                  dense
                  size="sm"
                  icon="close"
                  aria-label="Olib tashlash"
                  @click="bulkFiles.splice(i, 1)"
                />
              </q-item-section>
            </q-item>
          </q-list>
        </q-card-section>
        <q-card-actions align="right" class="q-px-md q-pb-md">
          <q-btn
            flat
            no-caps
            label="Yopish"
            :disable="bulkRunning"
            v-close-popup
          />
          <q-btn
            unelevated
            no-caps
            color="primary"
            icon="cloud_upload"
            :label="`Yuklash (${bulkWaiting})`"
            :loading="bulkRunning"
            :disable="!bulkWaiting || !bulk.legalBasis"
            @click="runBulk"
          />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </page-layout>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { useQuasar } from 'quasar'
import { api } from '@/boot/axios'
import PageLayout from '@/components/PageLayout.vue'
import BookCover from '@/components/BookCover.vue'
import { useSchoolStore } from '@/stores/school'
import {
  legalBases,
  ageGroups,
  languages,
  labelOf,
  formatSize,
  sha256Hex,
  looksLikePdf,
  LEGAL_WARNING
} from '@/utils/library'

const MAX_PDF_MB = 50
const MAX_COVER_MB = 2

const $q = useQuasar()
const schoolStore = useSchoolStore()

const view = ref(readView())
const books = ref([])
const total = ref(0)
const loading = ref(false)
const page = ref(1)
const pageSize = ref(24)
const categories = ref([])
const filters = reactive({
  q: '',
  categoryId: null,
  language: null,
  ageGroup: null,
  legalBasis: null,
  status: 'all'
})

const statusOptions = [
  { value: 'all', label: 'Hammasi' },
  { value: 'active', label: 'Faol' },
  { value: 'inactive', label: 'Nofaol' },
  { value: 'draft', label: 'Qoralama' }
]

const categoryOptions = computed(() =>
  categories.value.map(c => ({
    value: c.id,
    label: `${c.emoji ? c.emoji + ' ' : ''}${c.nameUz}${c.active ? '' : ' (yashirin)'}`
  }))
)

const pages = computed(() =>
  Math.max(1, Math.ceil(total.value / pageSize.value))
)

const columns = [
  { name: 'cover', label: '', field: 'id', align: 'left' },
  { name: 'title', label: 'Kitob', field: 'title', align: 'left' },
  {
    name: 'category',
    label: "Bo'lim",
    field: r =>
      r.categoryName ? `${r.categoryEmoji || ''} ${r.categoryName}` : '—',
    align: 'left'
  },
  {
    name: 'language',
    label: 'Til',
    field: r => labelOf(languages, r.language),
    align: 'left'
  },
  {
    name: 'age',
    label: 'Yosh',
    field: r => labelOf(ageGroups, r.ageGroup),
    align: 'left'
  },
  {
    name: 'legal',
    label: 'Huquqiy asos',
    field: r => labelOf(legalBases, r.legalBasis),
    align: 'left'
  },
  {
    name: 'downloads',
    label: 'Yuklashlar',
    field: 'downloadCount',
    align: 'right'
  },
  { name: 'status', label: 'Holat', field: 'active', align: 'left' },
  { name: 'actions', label: '', field: 'id', align: 'right' }
]

const tablePagination = ref({ page: 1, rowsPerPage: 24, rowsNumber: 0 })

// bumped after every save: a replaced cover is fetched again instead of the cached one
const coverEpoch = ref(0)
function coverVersion(b) {
  return `${b.pdfSha256}:${b.hasCover}:${coverEpoch.value}`
}

function readView() {
  try {
    return localStorage.getItem('libraryView') === 'table' ? 'table' : 'grid'
  } catch {
    return 'grid'
  }
}

watch(view, v => {
  try {
    localStorage.setItem('libraryView', v)
  } catch {
    // remembered for this session only
  }
})

function notifyError(error) {
  $q.notify({
    type: 'negative',
    message: error.friendlyMessage || 'Xatolik yuz berdi'
  })
}

async function loadCategories() {
  if (!schoolStore.activeSchoolId) return
  try {
    const res = await api.get('/api/library/categories', {
      params: { schoolId: schoolStore.activeSchoolId }
    })
    categories.value = res.data
  } catch (e) {
    notifyError(e)
  }
}

async function load() {
  if (!schoolStore.activeSchoolId) return
  loading.value = true
  try {
    const res = await api.get('/api/library/books', {
      params: {
        schoolId: schoolStore.activeSchoolId,
        q: filters.q || undefined,
        categoryId: filters.categoryId || undefined,
        language: filters.language || undefined,
        ageGroup: filters.ageGroup || undefined,
        legalBasis: filters.legalBasis || undefined,
        status: filters.status,
        page: page.value - 1,
        size: pageSize.value
      }
    })
    books.value = res.data.items
    total.value = res.data.total
    tablePagination.value = {
      ...tablePagination.value,
      page: page.value,
      rowsPerPage: pageSize.value,
      rowsNumber: res.data.total
    }
  } catch (e) {
    notifyError(e)
  } finally {
    loading.value = false
  }
}

function onTableRequest({ pagination }) {
  pageSize.value = pagination.rowsPerPage || 24
  if (page.value === pagination.page) load()
  else page.value = pagination.page
}

watch(
  () => ({ ...filters }),
  () => {
    if (page.value === 1) load()
    else page.value = 1
  },
  { deep: true }
)
watch(page, load)
watch(
  () => schoolStore.activeSchoolId,
  id => {
    if (!id) return
    page.value = 1
    loadCategories()
    load()
  },
  { immediate: true }
)

// ------------------------------------------------------------------ PDF

async function viewPdf(book) {
  // opened right away (still inside the click) so the browser does not block the tab
  const tab = window.open('', '_blank')
  try {
    const res = await api.get(`/api/library/books/${book.id}/pdf`, {
      responseType: 'blob'
    })
    const url = URL.createObjectURL(
      new Blob([res.data], { type: 'application/pdf' })
    )
    if (tab) tab.location.href = url
    else window.location.href = url
    setTimeout(() => URL.revokeObjectURL(url), 60_000)
  } catch (e) {
    if (tab) tab.close()
    notifyError(e)
  }
}

function deactivate(book, fromForm = false) {
  $q.dialog({
    title: 'Nofaol qilish',
    message: `«${book.title}» botda ko'rinmay qoladi. Yuklash tarixi saqlanadi. Davom etamizmi?`,
    cancel: { flat: true, noCaps: true, label: 'Bekor qilish' },
    ok: {
      color: 'negative',
      unelevated: true,
      noCaps: true,
      label: 'Nofaol qilish'
    }
  }).onOk(async () => {
    try {
      await api.delete(`/api/library/books/${book.id}`)
      $q.notify({ type: 'positive', message: 'Kitob nofaol qilindi' })
      if (fromForm) formOpen.value = false
      load()
    } catch (e) {
      notifyError(e)
    }
  })
}

// ------------------------------------------------------------------ form

const formOpen = ref(false)
const saving = ref(false)
const form = reactive({})
const pdfFile = ref(null)
const pdfSha = ref('')
const coverFile = ref(null)
const coverPreview = ref('')
const removeCover = ref(false)
const duplicates = ref([])
const dragPdf = ref(false)
const dragCover = ref(false)
const pdfInput = ref(null)
const coverInput = ref(null)

function resetFiles() {
  pdfFile.value = null
  pdfSha.value = ''
  coverFile.value = null
  if (coverPreview.value) URL.revokeObjectURL(coverPreview.value)
  coverPreview.value = ''
  removeCover.value = false
  duplicates.value = []
}

function openCreate() {
  resetFiles()
  Object.keys(form).forEach(k => delete form[k])
  Object.assign(form, {
    id: null,
    title: '',
    author: '',
    categoryId: filters.categoryId || null,
    language: 'uz',
    ageGroup: 'KATTALAR',
    pages: null,
    year: null,
    description: '',
    legalBasis: null,
    legalNote: '',
    active: true,
    featured: false,
    draft: false
  })
  formOpen.value = true
}

function openEdit(book) {
  resetFiles()
  Object.keys(form).forEach(k => delete form[k])
  Object.assign(form, { ...book, active: book.draft ? true : book.active })
  formOpen.value = true
}

async function pickPdf(file) {
  dragPdf.value = false
  if (pdfInput.value) pdfInput.value.value = ''
  if (!file) return
  if (file.size > MAX_PDF_MB * 1024 * 1024) {
    $q.notify({
      type: 'negative',
      message: `PDF juda katta: ko'pi bilan ${MAX_PDF_MB} MB`
    })
    return
  }
  if (!(await looksLikePdf(file))) {
    $q.notify({
      type: 'negative',
      message: 'Fayl PDF emas (faqat haqiqiy PDF qabul qilinadi)'
    })
    return
  }
  pdfFile.value = file
  if (!form.title)
    form.title = file.name
      .replace(/\.pdf$/i, '')
      .replace(/_+/g, ' ')
      .trim()
  duplicates.value = []
  try {
    pdfSha.value = await sha256Hex(file)
    const res = await api.get('/api/library/books/duplicates', {
      params: { schoolId: schoolStore.activeSchoolId, sha256: pdfSha.value },
      background: true
    })
    duplicates.value = res.data.filter(d => d.id !== form.id)
  } catch {
    // the server checks again on save
  }
}

function onPdfDrop(e) {
  pickPdf(e.dataTransfer.files[0])
}

function pickCover(file) {
  dragCover.value = false
  if (coverInput.value) coverInput.value.value = ''
  if (!file) return
  if (!['image/jpeg', 'image/png'].includes(file.type)) {
    $q.notify({
      type: 'negative',
      message: "Muqova faqat JPG yoki PNG bo'lishi mumkin"
    })
    return
  }
  if (file.size > MAX_COVER_MB * 1024 * 1024) {
    $q.notify({
      type: 'negative',
      message: `Muqova juda katta: ko'pi bilan ${MAX_COVER_MB} MB`
    })
    return
  }
  coverFile.value = file
  if (coverPreview.value) URL.revokeObjectURL(coverPreview.value)
  coverPreview.value = URL.createObjectURL(file)
  removeCover.value = false
}

function onCoverDrop(e) {
  pickCover(e.dataTransfer.files[0])
}

function payload() {
  return {
    title: (form.title || '').trim(),
    author: form.author || null,
    categoryId: form.categoryId || null,
    description: form.description || null,
    language: form.language || 'uz',
    ageGroup: form.ageGroup || 'KATTALAR',
    pages: form.pages || null,
    year: form.year || null,
    legalBasis: form.legalBasis,
    legalNote: form.legalNote || null,
    active: !!form.active,
    featured: !!form.featured
  }
}

async function save(allowDuplicate = false) {
  if (!form.title || !form.title.trim()) {
    $q.notify({ type: 'warning', message: 'Kitob nomini kiriting' })
    return
  }
  if (!form.legalBasis) {
    $q.notify({
      type: 'warning',
      message: `Huquqiy asosni tanlang. ${LEGAL_WARNING}`
    })
    return
  }
  if (!form.id && !pdfFile.value) {
    $q.notify({ type: 'warning', message: 'PDF faylni tanlang' })
    return
  }
  if (duplicates.value.length && allowDuplicate !== true) {
    $q.dialog({
      title: 'Bu PDF allaqachon bor',
      message: `Xuddi shu fayl «${duplicates.value[0].title}» kitobida yuklangan. Baribir saqlaymizmi?`,
      cancel: { flat: true, noCaps: true, label: "Yo'q" },
      ok: { unelevated: true, noCaps: true, label: 'Ha, saqlash' }
    }).onOk(() => save(true))
    return
  }
  const data = new FormData()
  data.append(
    'book',
    new Blob([JSON.stringify(payload())], { type: 'application/json' })
  )
  if (pdfFile.value) data.append('pdf', pdfFile.value)
  if (coverFile.value) data.append('cover', coverFile.value)
  saving.value = true
  try {
    const params = { allowDuplicate: allowDuplicate === true }
    if (form.id) {
      await api.put(`/api/library/books/${form.id}`, data, {
        params: { ...params, removeCover: removeCover.value }
      })
    } else {
      await api.post('/api/library/books', data, {
        params: { ...params, schoolId: schoolStore.activeSchoolId }
      })
    }
    $q.notify({
      type: 'positive',
      message: form.id ? 'Kitob saqlandi' : "Kitob qo'shildi"
    })
    formOpen.value = false
    coverEpoch.value++
    loadCategories()
    load()
  } catch (e) {
    if (e.response?.status === 409 && allowDuplicate !== true) {
      $q.dialog({
        title: 'Bu PDF allaqachon bor',
        message: `${e.friendlyMessage}. Baribir saqlaymizmi?`,
        cancel: { flat: true, noCaps: true, label: "Yo'q" },
        ok: { unelevated: true, noCaps: true, label: 'Ha, saqlash' }
      }).onOk(() => save(true))
    } else {
      notifyError(e)
    }
  } finally {
    saving.value = false
  }
}

// ------------------------------------------------------------------ bulk

const bulkOpen = ref(false)
const bulkRunning = ref(false)
const bulkFiles = ref([])
const dragBulk = ref(false)
const bulkInput = ref(null)
const bulk = reactive({
  legalBasis: null,
  legalNote: '',
  categoryId: null,
  language: 'uz',
  ageGroup: 'KATTALAR'
})
const bulkWaiting = computed(
  () => bulkFiles.value.filter(f => f.state === 'waiting').length
)

function openBulk() {
  bulkFiles.value = []
  bulkOpen.value = true
}

function addBulk(list) {
  dragBulk.value = false
  for (const file of Array.from(list || [])) {
    if (!/\.pdf$/i.test(file.name) && file.type !== 'application/pdf') {
      bulkFiles.value.push({ file, state: 'error', error: 'PDF emas' })
    } else if (file.size > MAX_PDF_MB * 1024 * 1024) {
      bulkFiles.value.push({
        file,
        state: 'error',
        error: `${MAX_PDF_MB} MB dan katta`
      })
    } else {
      bulkFiles.value.push({ file, state: 'waiting', error: '' })
    }
  }
  if (bulkInput.value) bulkInput.value.value = ''
}

function onBulkDrop(e) {
  addBulk(e.dataTransfer.files)
}

function bulkIcon(state) {
  return (
    { waiting: 'schedule', done: 'check_circle', error: 'error' }[state] ||
    'schedule'
  )
}

function bulkColor(state) {
  return (
    { waiting: 'grey-6', done: 'positive', error: 'negative' }[state] ||
    'grey-6'
  )
}

async function runBulk() {
  bulkRunning.value = true
  let done = 0
  // one file per request, one after another: 50 MB files never pile up in one request
  for (const item of bulkFiles.value) {
    if (item.state !== 'waiting') continue
    if (!(await looksLikePdf(item.file))) {
      item.state = 'error'
      item.error = 'PDF emas'
      continue
    }
    item.state = 'uploading'
    const data = new FormData()
    data.append('pdf', item.file)
    try {
      await api.post('/api/library/books/drafts', data, {
        params: {
          schoolId: schoolStore.activeSchoolId,
          legalBasis: bulk.legalBasis,
          legalNote: bulk.legalNote || undefined,
          categoryId: bulk.categoryId || undefined,
          language: bulk.language,
          ageGroup: bulk.ageGroup
        }
      })
      item.state = 'done'
      done++
    } catch (e) {
      item.state = 'error'
      item.error = e.friendlyMessage || 'Xatolik'
    }
  }
  bulkRunning.value = false
  if (done) {
    $q.notify({
      type: 'positive',
      message: `${done} ta qoralama yaratildi — to'ldirib saqlang`
    })
    filters.status = 'draft'
    load()
  }
}
</script>

<style scoped>
.book-card {
  position: relative;
  overflow: hidden;
  height: 100%;
  transition:
    transform 0.15s ease,
    box-shadow 0.15s ease;
}
.book-card:hover,
.book-card:focus-visible {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.12);
}
.book-card--off .book-card__cover {
  opacity: 0.55;
}
.book-card__cover {
  position: relative;
  aspect-ratio: 3 / 4;
  padding: 8px 8px 0;
}
.book-card__badge {
  position: absolute;
  top: 14px;
  left: 14px;
}
.book-card__menu {
  position: absolute;
  top: 10px;
  right: 10px;
  background: rgba(15, 23, 42, 0.45);
  color: #fff;
}
.book-card__title {
  font-weight: 600;
  line-height: 1.25;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 2.5em;
}
.book-form {
  width: 920px;
  max-width: 96vw;
}
.bulk-form {
  width: 640px;
  max-width: 96vw;
}
.legal-banner {
  background: rgba(245, 158, 11, 0.12);
  border: 1px solid rgba(245, 158, 11, 0.35);
}
.drop-zone {
  border: 2px dashed rgba(148, 163, 184, 0.6);
  border-radius: 12px;
  padding: 16px 10px;
  min-height: 120px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  text-align: center;
  cursor: pointer;
  transition:
    border-color 0.15s ease,
    background 0.15s ease;
}
.drop-zone--cover {
  min-height: 150px;
}
.drop-zone--over {
  border-color: var(--q-primary);
  background: rgba(99, 102, 241, 0.08);
}
.drop-zone--ok {
  border-style: solid;
  border-color: var(--q-positive);
}
.cover-preview {
  max-height: 128px;
  max-width: 100%;
  border-radius: 8px;
}
.bulk-list {
  max-height: 260px;
  overflow-y: auto;
}
.muted-text {
  color: var(--brand-text-muted);
}
.hidden {
  display: none;
}
</style>

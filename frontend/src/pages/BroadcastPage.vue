<template>
  <page-layout
    icon="campaign"
    color="#ec4899"
    title="Ota-onalarga xabar"
    :subtitle="`Telegram bot orqali xabar, rasm va video · ${schoolStore.activeSchoolName || ''}`"
  >
    <div class="row q-col-gutter-md">
      <div class="col-12 col-lg-7">
        <div class="brand-card q-pa-md">
          <div class="text-subtitle1 text-weight-semibold q-mb-sm"
            >Yangi xabar</div
          >

          <div class="text-caption muted-text q-mb-xs">Kimga?</div>
          <q-btn-toggle
            v-model="audience"
            no-caps
            unelevated
            rounded
            toggle-color="primary"
            class="q-mb-md audience-toggle"
            :options="[
              { label: 'Butun maktab', value: 'ALL', icon: 'domain' },
              { label: 'Tanlangan sinflar', value: 'CLASSES', icon: 'groups' },
              {
                label: 'Tanlangan ota-onalar',
                value: 'PARENTS',
                icon: 'person_search'
              }
            ]"
            @update:model-value="resetPreview"
          />

          <q-select
            v-if="audience === 'CLASSES'"
            v-model="classIds"
            :options="classOptions"
            emit-value
            map-options
            multiple
            use-chips
            outlined
            dense
            label="Sinflar"
            class="q-mb-md"
            @update:model-value="resetPreview"
          />

          <!-- choose parents from the list of linked ones -->
          <div v-if="audience === 'PARENTS'" class="parents-picker q-mb-md">
            <div class="row q-col-gutter-sm q-pa-sm">
              <div class="col-12 col-sm-7">
                <q-input
                  v-model="parentQuery"
                  dense
                  outlined
                  clearable
                  debounce="300"
                  placeholder="Ota-ona yoki farzand ismi"
                  @update:model-value="loadChoices"
                >
                  <template #prepend><q-icon name="search" /></template>
                </q-input>
              </div>
              <div class="col-12 col-sm-5">
                <q-select
                  v-model="parentClass"
                  dense
                  outlined
                  clearable
                  emit-value
                  map-options
                  label="Sinf"
                  :options="classOptions"
                  @update:model-value="loadChoices"
                />
              </div>
            </div>
            <q-separator />
            <div class="row items-center q-px-sm q-py-xs text-caption">
              <q-checkbox
                dense
                :model-value="allChoicesPicked"
                :disable="!choices.length"
                label="Barchasini belgilash"
                @update:model-value="toggleAllChoices"
              />
              <q-space />
              <span class="text-weight-semibold"
                >Tanlandi: {{ linkIds.length }}</span
              >
              <q-btn
                v-if="linkIds.length"
                flat
                dense
                no-caps
                size="sm"
                label="Tozalash"
                class="q-ml-sm"
                @click="clearPicked"
              />
            </div>
            <q-separator />
            <q-list dense class="choices scroll">
              <q-item v-if="loadingChoices">
                <q-item-section><q-skeleton type="text" /></q-item-section>
              </q-item>
              <q-item
                v-else-if="!choices.length"
                class="muted-text text-caption"
              >
                <q-item-section>Botga ulangan ota-ona topilmadi</q-item-section>
              </q-item>
              <q-item
                v-for="c in choices"
                :key="c.linkId"
                tag="label"
                clickable
              >
                <q-item-section side>
                  <q-checkbox
                    v-model="linkIds"
                    :val="c.linkId"
                    dense
                    @update:model-value="resetPreview"
                  />
                </q-item-section>
                <q-item-section>
                  <q-item-label
                    >{{ c.parentName || 'Ota-ona'
                    }}<span v-if="c.username" class="muted-text">
                      @{{ c.username }}</span
                    ></q-item-label
                  >
                  <q-item-label caption
                    >{{ c.studentName }} · {{ c.className }}</q-item-label
                  >
                </q-item-section>
              </q-item>
            </q-list>
          </div>

          <q-input
            v-model="text"
            type="textarea"
            outlined
            autogrow
            counter
            maxlength="3500"
            label="Xabar matni"
            placeholder="Masalan: Hurmatli ota-onalar! 7-oktabr kuni soat 18:00 da umumiy yig'ilish bo'lib o'tadi."
            input-style="min-height: 120px"
            @update:model-value="resetPreview"
          />

          <!-- attachments -->
          <div
            class="dropzone q-mt-md"
            :class="{ 'dropzone--over': dragOver }"
            @dragover.prevent="dragOver = true"
            @dragleave.prevent="dragOver = false"
            @drop.prevent="onDrop"
          >
            <div class="row items-center q-gutter-sm">
              <q-btn
                outline
                no-caps
                color="primary"
                icon="attach_file"
                label="Rasm, video yoki fayl"
                :disable="files.length >= MAX_FILES"
                @click="fileInput.click()"
              />
              <span class="text-caption muted-text"
                >yoki shu yerga tashlang · ko'pi bilan {{ MAX_FILES }} ta, har
                biri 20 MB gacha</span
              >
              <input
                ref="fileInput"
                type="file"
                multiple
                hidden
                accept="image/jpeg,image/png,image/webp,video/mp4,video/quicktime,audio/mpeg,audio/mp4,audio/ogg,.pdf,.doc,.docx,.xls,.xlsx,.txt"
                @change="e => addFiles(e.target.files, e)"
              />
            </div>
            <div v-if="files.length" class="thumbs q-mt-sm">
              <div v-for="(f, i) in files" :key="f.key" class="thumb">
                <img v-if="f.kind === 'PHOTO'" :src="f.url" alt="" />
                <video
                  v-else-if="f.kind === 'VIDEO'"
                  :src="f.url"
                  muted
                  preload="metadata"
                />
                <div v-else class="thumb-file column flex-center">
                  <q-icon
                    :name="f.kind === 'AUDIO' ? 'audiotrack' : 'description'"
                    size="28px"
                  />
                </div>
                <div class="thumb-name ellipsis">{{ f.file.name }}</div>
                <div class="thumb-size">{{ size(f.file.size) }}</div>
                <q-btn
                  round
                  dense
                  size="xs"
                  icon="close"
                  class="thumb-remove"
                  :aria-label="`${f.file.name} ni olib tashlash`"
                  @click="removeFile(i)"
                />
              </div>
            </div>
          </div>

          <div class="row q-gutter-sm q-mt-md">
            <q-btn
              outline
              no-caps
              color="primary"
              icon="visibility"
              label="Oldindan ko'rish"
              :disable="!canSend"
              :loading="previewing"
              @click="doPreview"
            />
            <q-btn
              unelevated
              no-caps
              color="primary"
              icon="send"
              label="Yuborish"
              :disable="!preview || !preview.recipientCount"
              :loading="sending"
              @click="confirmSend"
            />
          </div>
        </div>
      </div>

      <div class="col-12 col-lg-5">
        <div class="brand-card q-pa-md preview-card">
          <div class="row items-center q-mb-sm">
            <q-icon name="smartphone" size="20px" class="q-mr-sm" />
            <div class="text-subtitle1 text-weight-semibold"
              >Ota-ona ko'radigan ko'rinish</div
            >
          </div>
          <div
            v-if="!preview"
            class="muted-text text-body2 q-py-lg text-center"
          >
            Matnni yozib (yoki fayl biriktirib), «Oldindan ko'rish»ni bosing
          </div>
          <template v-else>
            <div class="tg-chat column q-gutter-y-sm">
              <!-- album(s) and files arrive first, then the text -->
              <div v-if="visualFiles.length" class="tg-bubble tg-media">
                <div
                  class="album"
                  :class="`album--${Math.min(visualFiles.length, 4)}`"
                >
                  <div
                    v-for="f in visualFiles.slice(0, 10)"
                    :key="f.key"
                    class="album-cell"
                  >
                    <img v-if="f.kind === 'PHOTO'" :src="f.url" alt="" />
                    <video v-else :src="f.url" muted preload="metadata" />
                  </div>
                </div>
                <!-- a single file carries the text as its caption -->
                <!-- eslint-disable-next-line vue/no-v-html -->
                <div
                  v-if="captionInside"
                  class="tg-caption"
                  v-html="telegramHtmlToSafe(preview.html)"
                />
              </div>
              <div
                v-for="f in otherFiles"
                :key="f.key"
                class="tg-bubble row items-center no-wrap q-gutter-x-sm"
              >
                <q-icon
                  :name="f.kind === 'AUDIO' ? 'audiotrack' : 'description'"
                  size="28px"
                  color="primary"
                />
                <div style="min-width: 0">
                  <div class="ellipsis text-weight-medium">
                    {{ f.file.name }}
                  </div>
                  <div class="text-caption muted-text">
                    {{ size(f.file.size) }}
                  </div>
                </div>
              </div>
              <!-- eslint-disable-next-line vue/no-v-html -->
              <div
                v-if="!captionInside"
                class="tg-bubble"
                v-html="telegramHtmlToSafe(preview.html)"
              />
            </div>
            <div class="row items-center q-mt-md recipients">
              <q-icon
                name="people"
                size="20px"
                class="q-mr-sm"
                color="primary"
              />
              <div>
                <b>{{ preview.recipientCount }}</b> ta ota-onaga yuboriladi
                <div
                  v-if="preview.classNames?.length"
                  class="text-caption muted-text"
                >
                  {{ preview.classNames.join(', ') }} sinflar
                </div>
              </div>
            </div>
            <div
              v-if="!preview.recipientCount"
              class="text-caption text-warning q-mt-sm"
            >
              Tanlangan auditoriyada botga ulangan ota-ona yo'q.
            </div>
          </template>
        </div>
      </div>
    </div>

    <!-- history -->
    <div class="brand-card overflow-hidden q-mt-md">
      <div class="q-pa-md row items-center q-col-gutter-sm">
        <div class="col-12 col-md text-subtitle1 text-weight-semibold"
          >Yuborilgan xabarlar</div
        >
        <div class="col-12 col-sm-4 col-md-3">
          <q-input
            v-model="historyQuery"
            dense
            outlined
            clearable
            debounce="400"
            placeholder="Matn bo'yicha qidirish"
            @update:model-value="loadHistory"
          >
            <template #prepend><q-icon name="search" /></template>
          </q-input>
        </div>
        <div class="col-6 col-sm-4 col-md-2">
          <date-field
            v-model="historyFrom"
            label="Sanadan"
            @update:model-value="loadHistory"
          />
        </div>
        <div class="col-6 col-sm-4 col-md-2">
          <date-field
            v-model="historyTo"
            label="Sanagacha"
            @update:model-value="loadHistory"
          />
        </div>
      </div>
      <q-table
        :rows="history"
        :columns="columns"
        row-key="id"
        :loading="loadingHistory && !history.length"
        flat
        class="brand-table history-table"
        :pagination="{ rowsPerPage: 10 }"
        @row-click="(_, row) => openRecipients(row)"
      >
        <template v-slot:no-data>
          <div class="full-width column flex-center q-py-lg muted-text">
            <q-icon name="outbox" size="40px" class="q-mb-sm" />
            Hali xabar yuborilmagan
          </div>
        </template>
        <template v-slot:body-cell-text="props">
          <q-td :props="props" class="text-cell">
            <div class="row items-center no-wrap q-gutter-x-xs">
              <q-icon
                v-if="props.row.attachments?.length"
                name="perm_media"
                size="16px"
                color="primary"
              >
                <q-tooltip
                  >{{ props.row.attachments.length }} ta fayl:
                  {{
                    props.row.attachments.map(a => a.name).join(', ')
                  }}</q-tooltip
                >
              </q-icon>
              <span class="ellipsis-2">{{ props.row.text || '📎' }}</span>
            </div>
            <q-tooltip v-if="props.row.text" max-width="420px">{{
              props.row.text
            }}</q-tooltip>
          </q-td>
        </template>
        <template v-slot:body-cell-audience="props">
          <q-td :props="props">
            {{ audienceLabel(props.row) }}
          </q-td>
        </template>
        <template v-slot:body-cell-result="props">
          <q-td :props="props" style="min-width: 240px">
            <div class="row items-center q-gutter-x-sm text-caption">
              <span class="text-weight-semibold tabular-nums"
                >{{ props.row.sent }}/{{
                  props.row.recipientCount
                }}
                yuborildi</span
              >
              <span v-if="props.row.pending" class="text-warning"
                >⏳ {{ props.row.pending }}</span
              >
              <span v-if="props.row.failed" class="text-negative"
                >✕ {{ props.row.failed }}</span
              >
              <span v-if="props.row.skipped" class="muted-text"
                >⊘ {{ props.row.skipped }}</span
              >
            </div>
            <q-linear-progress
              :value="
                props.row.recipientCount
                  ? props.row.sent / props.row.recipientCount
                  : 0
              "
              :color="props.row.pending ? 'primary' : 'positive'"
              :track-color="$q.dark.isActive ? 'grey-9' : 'grey-4'"
              :indeterminate="false"
              rounded
              size="6px"
              class="q-mt-xs"
            />
          </q-td>
        </template>
        <template v-slot:body-cell-open="props">
          <q-td :props="props" class="text-right">
            <q-btn
              flat
              dense
              no-caps
              no-wrap
              size="sm"
              color="primary"
              icon="groups"
              label="Oluvchilar"
              @click.stop="openRecipients(props.row)"
            />
          </q-td>
        </template>
      </q-table>
    </div>

    <!-- recipients of one message -->
    <q-dialog v-model="recipientsOpen">
      <q-card class="recipients-card">
        <q-card-section class="row items-center q-pb-sm">
          <div class="col" style="min-width: 0">
            <div class="text-h6">Oluvchilar</div>
            <div v-if="current" class="text-caption muted-text ellipsis">
              {{ formatDateTime(current.createdAt) }} ·
              {{ audienceLabel(current) }} ·
              {{ current.text || current.attachments.length + ' ta fayl' }}
            </div>
          </div>
          <q-btn flat round dense icon="close" v-close-popup />
        </q-card-section>
        <q-card-section class="row q-col-gutter-sm q-pt-none">
          <div class="col-12 col-sm-5">
            <q-input
              v-model="recipientQuery"
              dense
              outlined
              clearable
              debounce="300"
              placeholder="O'quvchi ismi"
              @update:model-value="loadRecipients(1)"
            >
              <template #prepend><q-icon name="search" /></template>
            </q-input>
          </div>
          <div class="col-6 col-sm-3">
            <q-select
              v-model="recipientClass"
              dense
              outlined
              clearable
              emit-value
              map-options
              label="Sinf"
              :options="classOptions"
              @update:model-value="loadRecipients(1)"
            />
          </div>
          <div class="col-6 col-sm-4">
            <q-select
              v-model="recipientStatus"
              dense
              outlined
              clearable
              emit-value
              map-options
              label="Holat"
              :options="statusOptions"
              @update:model-value="loadRecipients(1)"
            />
          </div>
        </q-card-section>
        <q-table
          :rows="recipients"
          :columns="recipientColumns"
          row-key="id"
          flat
          dense
          class="brand-table"
          :loading="loadingRecipients"
          v-model:pagination="recipientPagination"
          :rows-per-page-options="[20, 50, 100]"
          @request="p => loadRecipients(p.pagination.page, p.pagination)"
        >
          <template v-slot:body-cell-status="props">
            <q-td :props="props">
              <q-badge
                :color="statuses[props.row.status]?.color"
                :label="statuses[props.row.status]?.label"
                class="status-badge"
              />
              <q-tooltip v-if="props.row.error">{{
                props.row.error
              }}</q-tooltip>
            </q-td>
          </template>
          <template v-slot:no-data>
            <div class="full-width text-center muted-text q-py-md">
              Oluvchi topilmadi
            </div>
          </template>
        </q-table>
      </q-card>
    </q-dialog>
  </page-layout>
</template>

<script setup>
import { ref, computed, watch, onBeforeUnmount } from 'vue'
import { useQuasar } from 'quasar'
import { api } from '@/boot/axios'
import PageLayout from '@/components/PageLayout.vue'
import DateField from '@/components/DateField.vue'
import { useSchoolStore } from '@/stores/school'
import { formatDateTime, telegramHtmlToSafe } from '@/utils/telegram'

const $q = useQuasar()
const schoolStore = useSchoolStore()

const MAX_FILES = 10
const MAX_BYTES = 20 * 1024 * 1024
const MAX_CAPTION = 1024
const ALLOWED_EXT = [
  'jpg',
  'jpeg',
  'png',
  'webp',
  'mp4',
  'mov',
  'mp3',
  'm4a',
  'ogg',
  'pdf',
  'doc',
  'docx',
  'xls',
  'xlsx',
  'txt'
]

const statuses = {
  SENT: { label: 'Yetkazildi', color: 'positive' },
  PENDING: { label: 'Navbatda', color: 'warning' },
  FAILED: { label: 'Xato', color: 'negative' },
  SKIPPED: { label: "O'tkazib yuborildi", color: 'grey-7' }
}
const statusOptions = Object.entries(statuses).map(([value, s]) => ({
  value,
  label: s.label
}))

// ------------------------------------------------------------------- form
const audience = ref('ALL')
const classIds = ref([])
const classOptions = ref([])
const text = ref('')
const preview = ref(null)
const previewing = ref(false)
const sending = ref(false)

const linkIds = ref([])
const choices = ref([])
const parentQuery = ref('')
const parentClass = ref(null)
const loadingChoices = ref(false)

const files = ref([])
const fileInput = ref(null)
const dragOver = ref(false)
let fileKey = 0

const canSend = computed(
  () =>
    (text.value.trim().length > 0 || files.value.length > 0) &&
    (audience.value === 'ALL' ||
      (audience.value === 'CLASSES' && classIds.value.length > 0) ||
      (audience.value === 'PARENTS' && linkIds.value.length > 0))
)

const visualFiles = computed(() =>
  files.value.filter(f => f.kind === 'PHOTO' || f.kind === 'VIDEO')
)
const otherFiles = computed(() =>
  files.value.filter(f => f.kind !== 'PHOTO' && f.kind !== 'VIDEO')
)
// Telegram: one file → the text is its caption (if it fits); otherwise album, then the text
const captionInside = computed(
  () =>
    files.value.length === 1 &&
    visualFiles.value.length === 1 &&
    plainLength(preview.value?.html) <= MAX_CAPTION
)

function plainLength(html) {
  if (!html) return 0
  const div = document.createElement('div')
  div.innerHTML = html
  return div.textContent.length
}

function kindOf(file) {
  const t = (file.type || '').toLowerCase()
  const n = file.name.toLowerCase()
  if (t.startsWith('image/') && !n.endsWith('.svg'))
    return file.size > 10 * 1024 * 1024 ? 'DOCUMENT' : 'PHOTO'
  if (t.startsWith('video/')) return 'VIDEO'
  if (t.startsWith('audio/')) return 'AUDIO'
  return 'DOCUMENT'
}

function addFiles(list, event) {
  for (const file of list) {
    const ext = file.name.split('.').pop().toLowerCase()
    if (files.value.length >= MAX_FILES) {
      $q.notify({
        type: 'warning',
        message: `Ko'pi bilan ${MAX_FILES} ta fayl`
      })
      break
    }
    if (!ALLOWED_EXT.includes(ext)) {
      $q.notify({
        type: 'warning',
        message: `${file.name}: bu turdagi faylni yuborib bo'lmaydi`
      })
      continue
    }
    if (file.size > MAX_BYTES) {
      $q.notify({ type: 'warning', message: `${file.name}: 20 MB dan katta` })
      continue
    }
    files.value.push({
      key: ++fileKey,
      file,
      kind: kindOf(file),
      url: URL.createObjectURL(file)
    })
  }
  if (event?.target) event.target.value = ''
  resetPreview()
}

function onDrop(e) {
  dragOver.value = false
  addFiles(e.dataTransfer.files)
}

function removeFile(i) {
  URL.revokeObjectURL(files.value[i].url)
  files.value.splice(i, 1)
  resetPreview()
}

function clearFiles() {
  for (const f of files.value) URL.revokeObjectURL(f.url)
  files.value = []
}

function size(bytes) {
  if (bytes < 1024 * 1024) return `${Math.max(1, Math.round(bytes / 1024))} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

function body() {
  return {
    schoolId: schoolStore.activeSchoolId,
    text: text.value.trim(),
    audience: audience.value,
    classIds: audience.value === 'CLASSES' ? classIds.value : null,
    linkIds: audience.value === 'PARENTS' ? linkIds.value : null
  }
}

function resetPreview() {
  preview.value = null
}

async function doPreview() {
  previewing.value = true
  try {
    preview.value = (await api.post('/api/broadcasts/preview', body())).data
  } catch (error) {
    notifyError(error)
  } finally {
    previewing.value = false
  }
}

function confirmSend() {
  const n = preview.value.recipientCount
  $q.dialog({
    title: 'Xabarni yuborish',
    message: `Xabar${files.value.length ? ` (${files.value.length} ta fayl bilan)` : ''} ${n} ta ota-onaga Telegram orqali yuboriladi. Yuborilgan xabarni qaytarib bo'lmaydi.`,
    cancel: {
      flat: true,
      label: 'Bekor qilish',
      color: 'grey-7',
      noCaps: true
    },
    ok: { unelevated: true, label: 'Yuborish', color: 'primary', noCaps: true }
  }).onOk(async () => {
    sending.value = true
    try {
      const form = new FormData()
      form.append(
        'data',
        new Blob([JSON.stringify(body())], { type: 'application/json' })
      )
      for (const f of files.value) form.append('files', f.file)
      await api.post('/api/broadcasts', form)
      $q.notify({
        type: 'positive',
        icon: 'send',
        message: "Xabar navbatga qo'yildi va yuborilmoqda"
      })
      text.value = ''
      clearFiles()
      linkIds.value = []
      preview.value = null
      loadHistory()
    } catch (error) {
      notifyError(error)
    } finally {
      sending.value = false
    }
  })
}

// ------------------------------------------------------------------- parents picker
const allChoicesPicked = computed(
  () =>
    choices.value.length > 0 &&
    choices.value.every(c => linkIds.value.includes(c.linkId))
)

function toggleAllChoices(on) {
  const ids = choices.value.map(c => c.linkId)
  linkIds.value = on
    ? [...new Set([...linkIds.value, ...ids])]
    : linkIds.value.filter(id => !ids.includes(id))
  resetPreview()
}

function clearPicked() {
  linkIds.value = []
  resetPreview()
}

async function loadChoices() {
  loadingChoices.value = true
  try {
    choices.value = (
      await api.get('/api/telegram/parents/choices', {
        background: true,
        params: {
          schoolId: schoolStore.activeSchoolId,
          classId: parentClass.value || undefined,
          q: parentQuery.value?.trim() || undefined
        }
      })
    ).data
  } catch (error) {
    notifyError(error)
  } finally {
    loadingChoices.value = false
  }
}

watch(audience, a => {
  if (a === 'PARENTS' && !choices.value.length) loadChoices()
})

// ------------------------------------------------------------------- history
const history = ref([])
const loadingHistory = ref(false)
const historyQuery = ref('')
const historyFrom = ref('')
const historyTo = ref('')

const columns = [
  {
    name: 'createdAt',
    label: 'Sana',
    field: 'createdAt',
    align: 'left',
    format: v => formatDateTime(v)
  },
  { name: 'text', label: 'Xabar', field: 'text', align: 'left' },
  { name: 'audience', label: 'Kimga', field: 'audience', align: 'left' },
  { name: 'result', label: 'Natija', field: 'sent', align: 'left' },
  { name: 'createdBy', label: 'Yuborgan', field: 'createdBy', align: 'left' },
  { name: 'open', label: '', field: 'id', align: 'right' }
]

function audienceLabel(row) {
  if (row.audience === 'ALL') return 'Butun maktab'
  if (row.audience === 'PARENTS')
    return `${row.chosenParents} ta tanlangan ota-ona`
  return row.classNames.join(', ')
}

async function loadHistory({ quiet = false } = {}) {
  if (!quiet) loadingHistory.value = true
  try {
    history.value = (
      await api.get('/api/broadcasts', {
        background: quiet,
        params: {
          schoolId: schoolStore.activeSchoolId,
          size: 50,
          q: historyQuery.value?.trim() || undefined,
          from: historyFrom.value || undefined,
          to: historyTo.value || undefined
        }
      })
    ).data.content
  } catch (error) {
    if (!quiet) notifyError(error)
  } finally {
    loadingHistory.value = false
  }
}

// while something is still being delivered, refresh the counters ("1250/3000")
const progressTimer = setInterval(() => {
  if (!document.hidden && history.value.some(b => b.pending > 0))
    loadHistory({ quiet: true })
}, 3000)
onBeforeUnmount(() => {
  clearInterval(progressTimer)
  clearFiles()
})

// ------------------------------------------------------------------- recipients
const recipientsOpen = ref(false)
const current = ref(null)
const recipients = ref([])
const loadingRecipients = ref(false)
const recipientQuery = ref('')
const recipientClass = ref(null)
const recipientStatus = ref(null)
const recipientPagination = ref({ page: 1, rowsPerPage: 20, rowsNumber: 0 })

const recipientColumns = [
  {
    name: 'studentName',
    label: "O'quvchi",
    field: 'studentName',
    align: 'left'
  },
  { name: 'className', label: 'Sinf', field: 'className', align: 'left' },
  {
    name: 'parentName',
    label: 'Ota-ona (Telegram)',
    field: 'parentName',
    align: 'left',
    format: v => v || '—'
  },
  { name: 'status', label: 'Holat', field: 'status', align: 'left' },
  {
    name: 'time',
    label: 'Vaqt',
    field: r => r.sentAt || r.queuedAt,
    align: 'left',
    format: v => formatDateTime(v)
  }
]

function openRecipients(row) {
  current.value = row
  recipientQuery.value = ''
  recipientClass.value = null
  recipientStatus.value = null
  recipients.value = []
  recipientsOpen.value = true
  loadRecipients(1)
}

async function loadRecipients(page, pagination) {
  if (pagination) recipientPagination.value = { ...pagination }
  recipientPagination.value.page = page
  loadingRecipients.value = true
  try {
    const res = await api.get(
      `/api/broadcasts/${current.value.id}/recipients`,
      {
        params: {
          page: page - 1,
          size: recipientPagination.value.rowsPerPage,
          q: recipientQuery.value?.trim() || undefined,
          classId: recipientClass.value || undefined,
          status: recipientStatus.value || undefined
        }
      }
    )
    recipients.value = res.data.content
    recipientPagination.value.rowsNumber = res.data.totalElements
  } catch (error) {
    notifyError(error)
  } finally {
    loadingRecipients.value = false
  }
}

// ------------------------------------------------------------------- shared
async function loadClasses() {
  const res = await api.get('/api/school-classes', {
    params: {
      schoolId: schoolStore.activeSchoolId,
      size: 200,
      sort: 'gradeNumber,asc'
    }
  })
  classOptions.value = res.data.content
    .map(c => ({
      value: c.id,
      label: `${c.gradeNumber}-${c.sectionLetter}`,
      g: c.gradeNumber,
      s: c.sectionLetter
    }))
    .sort((a, b) => a.g - b.g || a.s.localeCompare(b.s))
}

function notifyError(error) {
  $q.notify({
    type: 'negative',
    message: error.friendlyMessage || 'Xatolik yuz berdi'
  })
}

watch(
  () => schoolStore.activeSchoolId,
  id => {
    if (!id) return
    preview.value = null
    classIds.value = []
    linkIds.value = []
    choices.value = []
    loadClasses()
    loadHistory()
    if (audience.value === 'PARENTS') loadChoices()
  },
  { immediate: true }
)
</script>

<style scoped>
.muted-text {
  color: var(--brand-text-muted);
}

.preview-card {
  height: 100%;
}

.audience-toggle {
  flex-wrap: wrap;
}

.parents-picker {
  border: 1px solid var(--brand-border);
  border-radius: var(--radius-md);
}

.choices {
  max-height: 260px;
}

.dropzone {
  border: 1.5px dashed var(--brand-border);
  border-radius: var(--radius-md);
  padding: 12px;
  transition:
    border-color 0.15s,
    background 0.15s;
}

.dropzone--over {
  border-color: var(--color-brand);
  background: rgba(79, 70, 229, 0.06);
}

.thumbs {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(110px, 1fr));
  gap: 8px;
}

.thumb {
  position: relative;
  border: 1px solid var(--brand-border);
  border-radius: 10px;
  overflow: hidden;
  background: var(--surface-2);
}

.thumb img,
.thumb video,
.thumb-file {
  width: 100%;
  height: 76px;
  object-fit: cover;
  display: block;
}

.thumb-name,
.thumb-size {
  font-size: var(--text-xs);
  padding: 0 6px;
}

.thumb-size {
  color: var(--brand-text-muted);
  padding-bottom: 4px;
}

.thumb-remove {
  position: absolute;
  top: 4px;
  right: 4px;
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
}

/* Telegram-like chat backdrop around the preview bubble. */
.tg-chat {
  background: var(--surface-2);
  border: 1px solid var(--brand-border);
  border-radius: var(--radius-md);
  padding: 16px;
}

.tg-bubble {
  background: var(--card-bg);
  border: 1px solid var(--brand-border);
  border-radius: 14px 14px 14px 4px;
  padding: 10px 14px;
  white-space: pre-wrap;
  line-height: 1.45;
  font-size: var(--text-base);
  max-width: 440px;
  box-shadow: var(--shadow-card);
}

.tg-media {
  padding: 4px;
}

.tg-caption {
  padding: 6px 10px 4px;
}

.album {
  display: grid;
  gap: 3px;
  border-radius: 10px;
  overflow: hidden;
}

.album--2,
.album--3,
.album--4 {
  grid-template-columns: 1fr 1fr;
}

.album--3 .album-cell:first-child {
  grid-column: span 2;
}

.album-cell {
  aspect-ratio: 4 / 3;
  background: rgba(0, 0, 0, 0.06);
}

.album--1 .album-cell {
  aspect-ratio: auto;
}

.album-cell img,
.album-cell video {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.recipients {
  font-size: var(--text-md);
}

.history-table :deep(tbody tr) {
  cursor: pointer;
}

.text-cell {
  max-width: 360px;
  white-space: normal;
}

.ellipsis-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.recipients-card {
  width: 100%;
  max-width: 860px;
  border-radius: var(--radius-lg);
}

.status-badge {
  padding: 3px 7px;
  border-radius: 6px;
  font-weight: 600;
}
</style>

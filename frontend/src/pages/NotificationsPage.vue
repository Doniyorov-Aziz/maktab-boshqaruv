<template>
  <page-layout
    icon="notifications_active"
    color="#229ed9"
    title="Xabarnomalar"
    :subtitle="`Ota-onalarga Telegram xabarlari · ${schoolStore.activeSchoolName || ''}`"
  >
    <template #actions>
      <q-chip
        v-if="status"
        square
        :color="botModes[status.mode]?.color"
        text-color="white"
        :icon="botModes[status.mode]?.icon"
      >
        {{ botModes[status.mode]?.label }}
        <template v-if="status.botUsername && status.mode === 'LIVE'">
          · @{{ status.botUsername }}</template
        >
      </q-chip>
      <q-btn
        flat
        round
        dense
        icon="refresh"
        aria-label="Yangilash"
        @click="loadAll"
      >
        <q-tooltip>Yangilash</q-tooltip>
      </q-btn>
    </template>

    <!-- Bot not configured: warning + setup steps -->
    <div
      v-if="status && !status.active"
      class="brand-card q-pa-md q-mb-md setup-card"
    >
      <div class="row items-start no-wrap">
        <q-icon name="warning" color="warning" size="28px" class="q-mr-md" />
        <div class="col">
          <div class="text-subtitle1 text-weight-semibold">Bot sozlanmagan</div>
          <div class="text-body2 muted-text q-mb-sm">
            Xabarnomalar navbatga yoziladi, lekin
            <b>{{ notificationStatuses.SKIPPED.label }}</b> holatida qoladi —
            ota-onalarga hech narsa yuborilmaydi.
            <template v-if="status.mode === 'NO_TOKEN'">
              TELEGRAM_ENABLED yoqilgan, ammo token berilmagan.</template
            >
          </div>
          <ol class="setup-steps">
            <li>
              Telegram'da <b>@BotFather</b> → <code>/newbot</code> → bot nomi va
              username kiriting, bergan <b>token</b>ni nusxalang.
            </li>
            <li>
              Serverdagi
              <code>src/main/resources/application-local.properties</code>
              fayliga yozing:
              <pre class="setup-code">
telegram.enabled=true
telegram.bot-token=&lt;token&gt;
telegram.bot-username=&lt;bot_username&gt;</pre>
              (yoki <code>TELEGRAM_ENABLED</code>,
              <code>TELEGRAM_BOT_TOKEN</code>,
              <code>TELEGRAM_BOT_USERNAME</code> muhit o'zgaruvchilari). Tokenni
              hech qachon kodga yoki git'ga yozmang.
            </li>
            <li
              >Backend'ni qayta ishga tushiring — bu yerda «Ulangan»
              chiqadi.</li
            >
          </ol>
          <div class="text-caption muted-text"
            >Batafsil: docs/15-telegram-bot.md — «Administrator uchun
            yo'riqnoma»</div
          >
        </div>
      </div>
    </div>

    <q-banner
      v-else-if="status?.mode === 'MOCK'"
      rounded
      dense
      class="brand-card q-mb-md"
    >
      <template v-slot:avatar>
        <q-icon name="science" color="info" />
      </template>
      Sinov (mock) rejimi: xabarlar Telegram'ga yuborilmaydi, faqat server
      logiga va bazaga yoziladi.
    </q-banner>

    <q-banner
      v-if="status?.lastError"
      rounded
      dense
      class="brand-card q-mb-md error-banner"
    >
      <template v-slot:avatar>
        <q-icon name="error" color="negative" />
      </template>
      {{ status.lastError }}
    </q-banner>

    <!-- KPI row -->
    <div class="row q-col-gutter-md q-mb-md">
      <div v-for="k in kpis" :key="k.label" class="col-6 col-md-3">
        <div class="brand-card stat-card">
          <div class="row items-center no-wrap">
            <div
              class="stat-card__icon q-mr-md"
              :style="{ background: `${k.color}1f`, color: k.color }"
            >
              <q-icon :name="k.icon" size="24px" />
            </div>
            <div class="col" style="min-width: 0">
              <div class="text-h5 text-weight-bold tabular-nums">
                <q-skeleton v-if="!stats" type="text" width="48px" />
                <template v-else>{{ k.value }}</template>
              </div>
              <div class="text-caption muted-text ellipsis">{{ k.label }}</div>
              <div
                v-if="stats && k.hint"
                class="text-caption muted-text ellipsis"
              >
                {{ k.hint }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="row q-col-gutter-md">
      <!-- Log table -->
      <div class="col-12 col-lg-8 col-xl-9">
        <div class="brand-card q-pa-md q-mb-md">
          <div class="row q-col-gutter-sm items-end">
            <div class="col-12 col-sm-6 col-md-3">
              <q-select
                v-model="filters.type"
                :options="typeOptions"
                emit-value
                map-options
                clearable
                outlined
                dense
                label="Turi"
                @update:model-value="reload"
              />
            </div>
            <div class="col-12 col-sm-6 col-md-3">
              <q-select
                v-model="filters.status"
                :options="statusOptions"
                emit-value
                map-options
                clearable
                outlined
                dense
                label="Holati"
                @update:model-value="reload"
              />
            </div>
            <div class="col-6 col-md-2">
              <date-field
                v-model="filters.from"
                label="Dan"
                @update:model-value="reload"
              />
            </div>
            <div class="col-6 col-md-2">
              <date-field
                v-model="filters.to"
                label="Gacha"
                @update:model-value="reload"
              />
            </div>
            <div class="col-12 col-md-2">
              <q-btn
                flat
                no-caps
                class="full-width"
                icon="filter_alt_off"
                label="Tozalash"
                :disable="!hasFilters"
                @click="clearFilters"
              />
            </div>
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
            <template v-slot:loading>
              <q-inner-loading showing color="primary" />
            </template>

            <template v-slot:no-data>
              <div class="full-width column flex-center q-py-xl muted-text">
                <q-icon name="mark_email_read" size="48px" class="q-mb-sm" />
                <div class="text-subtitle2">Hozircha xabarnomalar yo'q</div>
                <div class="text-caption">
                  Davomat, baho yoki e'lon saqlanganda ota-onalarga xabar shu
                  yerda paydo bo'ladi.
                </div>
              </div>
            </template>

            <template v-slot:body-cell-createdAt="props">
              <q-td :props="props" class="tabular-nums">
                {{ formatDateTime(props.row.createdAt) }}
                <div
                  v-if="
                    props.row.status === 'PENDING' &&
                    isFuture(props.row.scheduledAt)
                  "
                  class="text-caption muted-text"
                >
                  <q-icon name="bedtime" size="12px" />
                  {{ formatDateTime(props.row.scheduledAt).slice(11) }} da
                </div>
              </q-td>
            </template>

            <template v-slot:body-cell-type="props">
              <q-td :props="props">
                <span
                  class="type-pill"
                  :style="{
                    color: notificationTypes[props.row.type]?.color,
                    background: `${notificationTypes[props.row.type]?.color}1a`
                  }"
                >
                  <q-icon
                    :name="notificationTypes[props.row.type]?.icon"
                    size="14px"
                  />
                  {{ notificationTypes[props.row.type]?.label }}
                </span>
              </q-td>
            </template>

            <template v-slot:body-cell-student="props">
              <q-td :props="props">
                <router-link
                  :to="`/profiles/student/${props.row.studentId}`"
                  class="student-link"
                  >{{ props.row.studentName }}</router-link
                >
                <div class="text-caption muted-text">{{
                  props.row.className
                }}</div>
              </q-td>
            </template>

            <template v-slot:body-cell-status="props">
              <q-td :props="props">
                <q-badge
                  :color="notificationStatuses[props.row.status]?.color"
                  class="status-badge"
                >
                  <q-icon
                    :name="notificationStatuses[props.row.status]?.icon"
                    size="12px"
                    class="q-mr-xs"
                  />
                  {{ notificationStatuses[props.row.status]?.label }}
                </q-badge>
              </q-td>
            </template>

            <template v-slot:body-cell-lastError="props">
              <q-td :props="props" class="error-cell">
                <span v-if="props.row.lastError" class="ellipsis block">
                  {{ props.row.lastError }}
                  <q-tooltip max-width="360px">{{
                    props.row.lastError
                  }}</q-tooltip>
                </span>
                <span v-else class="muted-text">—</span>
              </q-td>
            </template>

            <template v-slot:body-cell-actions="props">
              <q-td :props="props" class="text-right no-wrap">
                <q-btn
                  flat
                  round
                  dense
                  size="sm"
                  icon="visibility"
                  aria-label="Matnni ko'rish"
                  @click="preview = props.row"
                >
                  <q-tooltip>Matnni ko'rish</q-tooltip>
                </q-btn>
                <q-btn
                  v-if="props.row.status === 'FAILED'"
                  flat
                  dense
                  no-caps
                  size="sm"
                  color="primary"
                  icon="replay"
                  label="Qayta yuborish"
                  :loading="retrying === props.row.id"
                  @click="retry(props.row)"
                />
              </q-td>
            </template>
          </q-table>
        </div>
      </div>

      <!-- Settings -->
      <div class="col-12 col-lg-4 col-xl-3">
        <div class="brand-card q-pa-md">
          <div class="row items-center q-mb-sm">
            <q-icon name="tune" size="20px" class="q-mr-sm" />
            <div class="text-subtitle1 text-weight-semibold">Sozlamalar</div>
          </div>
          <div class="text-caption muted-text q-mb-md">
            Maktab bo'yicha qaysi xabarlar yuborilishi.
            <template v-if="!authStore.isAdmin">
              Faqat administrator o'zgartira oladi.</template
            >
          </div>

          <template v-if="settings">
            <q-list dense class="q-mb-md">
              <q-item
                v-for="t in settingToggles"
                :key="t.key"
                tag="label"
                class="q-px-none"
              >
                <q-item-section avatar>
                  <q-icon :name="t.icon" :style="{ color: t.color }" />
                </q-item-section>
                <q-item-section>
                  <q-item-label>{{ t.label }}</q-item-label>
                  <q-item-label caption>{{ t.caption }}</q-item-label>
                </q-item-section>
                <q-item-section side>
                  <q-toggle
                    v-model="settings[t.key]"
                    color="primary"
                    :disable="!authStore.isAdmin"
                  />
                </q-item-section>
              </q-item>
            </q-list>

            <q-separator class="q-mb-md" />

            <q-toggle
              v-model="settings.quietHoursEnabled"
              label="Tinch soatlar"
              color="primary"
              :disable="!authStore.isAdmin"
            />
            <div class="text-caption muted-text q-mb-sm">
              Bu oraliqda yaratilgan xabarlar tugash vaqtida yuboriladi
              (Toshkent vaqti).
            </div>
            <div class="row q-col-gutter-sm q-mb-md">
              <div class="col-6">
                <q-input
                  v-model="settings.quietHoursStart"
                  type="time"
                  outlined
                  dense
                  label="Boshlanishi"
                  :disable="!authStore.isAdmin || !settings.quietHoursEnabled"
                />
              </div>
              <div class="col-6">
                <q-input
                  v-model="settings.quietHoursEnd"
                  type="time"
                  outlined
                  dense
                  label="Tugashi"
                  :disable="!authStore.isAdmin || !settings.quietHoursEnabled"
                />
              </div>
            </div>

            <q-btn
              v-if="authStore.isAdmin"
              unelevated
              no-caps
              color="primary"
              class="full-width"
              icon="save"
              label="Saqlash"
              :loading="savingSettings"
              @click="saveSettings"
            />
          </template>
          <div v-else class="column q-gutter-sm">
            <q-skeleton v-for="i in 4" :key="i" type="text" height="36px" />
          </div>
        </div>
      </div>
    </div>

    <q-dialog v-model="previewOpen">
      <q-card class="preview-card">
        <q-card-section class="row items-center q-pb-none">
          <div class="text-h6">Xabar matni</div>
          <q-space />
          <q-btn flat round dense icon="close" v-close-popup />
        </q-card-section>
        <q-card-section v-if="preview">
          <div class="text-caption muted-text q-mb-sm">
            {{ notificationTypes[preview.type]?.label }} ·
            {{ preview.studentName }} · {{ formatDateTime(preview.createdAt) }}
          </div>
          <!-- eslint-disable-next-line vue/no-v-html -->
          <div class="tg-bubble" v-html="telegramHtmlToSafe(preview.text)" />
          <div v-if="preview.sentAt" class="text-caption muted-text q-mt-sm">
            Yuborildi: {{ formatDateTime(preview.sentAt) }} ·
            {{ preview.attempts }} urinish
          </div>
        </q-card-section>
      </q-card>
    </q-dialog>
  </page-layout>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useQuasar } from 'quasar'
import { api } from '@/boot/axios'
import PageLayout from '@/components/PageLayout.vue'
import DateField from '@/components/DateField.vue'
import { useSchoolStore } from '@/stores/school'
import { useAuthStore } from '@/stores/auth'
import {
  notificationTypes,
  notificationStatuses,
  botModes,
  formatDateTime,
  telegramHtmlToSafe
} from '@/utils/telegram'

const $q = useQuasar()
const schoolStore = useSchoolStore()
const authStore = useAuthStore()

const status = ref(null)
const stats = ref(null)
const settings = ref(null)
const savingSettings = ref(false)
const rows = ref([])
const loading = ref(false)
const retrying = ref(null)
const preview = ref(null)
const previewOpen = computed({
  get: () => !!preview.value,
  set: v => {
    if (!v) preview.value = null
  }
})

const filters = ref({ type: null, status: null, from: '', to: '' })
const pagination = ref({ page: 1, rowsPerPage: 20, rowsNumber: 0 })

const typeOptions = Object.entries(notificationTypes).map(([value, t]) => ({
  value,
  label: t.label
}))
const statusOptions = Object.entries(notificationStatuses).map(
  ([value, s]) => ({ value, label: s.label })
)

const hasFilters = computed(
  () =>
    !!(
      filters.value.type ||
      filters.value.status ||
      filters.value.from ||
      filters.value.to
    )
)

const columns = [
  { name: 'createdAt', label: 'Vaqt', field: 'createdAt', align: 'left' },
  { name: 'type', label: 'Turi', field: 'type', align: 'left' },
  { name: 'student', label: "O'quvchi", field: 'studentName', align: 'left' },
  { name: 'status', label: 'Holati', field: 'status', align: 'left' },
  { name: 'lastError', label: 'Xato', field: 'lastError', align: 'left' },
  { name: 'actions', label: '', field: 'id', align: 'right' }
]

const settingToggles = [
  {
    key: 'attendanceEnabled',
    label: 'Davomat',
    caption: 'Kelmadi / kechikdi',
    icon: 'fact_check',
    color: '#ef4444'
  },
  {
    key: 'gradeEnabled',
    label: 'Baholar',
    caption: "Yangi yoki o'zgargan baho",
    icon: 'grade',
    color: '#0ea5e9'
  },
  {
    key: 'announcementEnabled',
    label: "E'lonlar",
    caption: "Maktab va sinf e'lonlari",
    icon: 'campaign',
    color: '#10b981'
  }
]

const kpis = computed(() => {
  const s = stats.value || {}
  return [
    {
      label: 'Bugun yuborilgan',
      value: s.sentToday ?? 0,
      icon: 'done_all',
      color: '#10b981',
      hint: s.pending ? `${s.pending} ta navbatda` : null
    },
    {
      label: 'Bugungi xatolar',
      value: s.failedToday ?? 0,
      icon: 'error_outline',
      color: '#ef4444',
      hint: s.failedTotal ? `Jami ${s.failedTotal} ta xato` : null
    },
    {
      label: 'Ulangan ota-onalar',
      value: s.linkedPercent != null ? `${s.linkedPercent}%` : '—',
      icon: 'family_restroom',
      color: '#229ed9',
      hint: `${s.linkedStudents ?? 0} / ${s.totalStudents ?? 0} o'quvchi`
    },
    {
      label: 'Telegram obunachilar',
      value: s.parentCount ?? 0,
      icon: 'send',
      color: '#8b5cf6',
      hint: s.skippedToday ? `Bugun ${s.skippedToday} ta o'tkazildi` : null
    }
  ]
})

function notifyError(error) {
  $q.notify({
    type: 'negative',
    message: error.friendlyMessage || 'Xatolik yuz berdi'
  })
}

function isFuture(value) {
  return value && new Date(value) > new Date()
}

async function loadStatus() {
  try {
    status.value = (await api.get('/api/telegram/status')).data
  } catch (error) {
    notifyError(error)
  }
}

async function loadStats() {
  if (!schoolStore.activeSchoolId) return
  try {
    stats.value = (
      await api.get('/api/notifications/stats', {
        params: { schoolId: schoolStore.activeSchoolId }
      })
    ).data
  } catch (error) {
    notifyError(error)
  }
}

async function loadSettings() {
  if (!schoolStore.activeSchoolId) return
  try {
    const data = (
      await api.get('/api/notifications/settings', {
        params: { schoolId: schoolStore.activeSchoolId }
      })
    ).data
    settings.value = {
      ...data,
      quietHoursStart: data.quietHoursStart.slice(0, 5),
      quietHoursEnd: data.quietHoursEnd.slice(0, 5)
    }
  } catch (error) {
    notifyError(error)
  }
}

async function fetchRows() {
  if (!schoolStore.activeSchoolId) return
  loading.value = true
  try {
    const { page, rowsPerPage } = pagination.value
    const response = await api.get('/api/notifications', {
      params: {
        schoolId: schoolStore.activeSchoolId,
        type: filters.value.type || undefined,
        status: filters.value.status || undefined,
        from: filters.value.from || undefined,
        to: filters.value.to || undefined,
        page: page - 1,
        size: rowsPerPage
      }
    })
    rows.value = response.data.content
    pagination.value.rowsNumber = response.data.totalElements
  } catch (error) {
    notifyError(error)
  } finally {
    loading.value = false
  }
}

function onRequest(requestProp) {
  pagination.value = requestProp.pagination
  fetchRows()
}

function reload() {
  pagination.value.page = 1
  fetchRows()
}

function clearFilters() {
  filters.value = { type: null, status: null, from: '', to: '' }
  reload()
}

async function retry(row) {
  retrying.value = row.id
  try {
    await api.post(`/api/notifications/${row.id}/retry`)
    $q.notify({
      type: 'positive',
      message: "Xabar qayta navbatga qo'yildi",
      icon: 'replay'
    })
    fetchRows()
    loadStats()
  } catch (error) {
    notifyError(error)
  } finally {
    retrying.value = null
  }
}

async function saveSettings() {
  savingSettings.value = true
  try {
    await api.put('/api/notifications/settings', settings.value, {
      params: { schoolId: schoolStore.activeSchoolId }
    })
    $q.notify({ type: 'positive', message: 'Saqlandi', icon: 'check_circle' })
  } catch (error) {
    notifyError(error)
  } finally {
    savingSettings.value = false
  }
}

function loadAll() {
  loadStatus()
  loadStats()
  loadSettings()
  fetchRows()
}

watch(() => schoolStore.activeSchoolId, loadAll, { immediate: true })
</script>

<style scoped>
.muted-text {
  color: var(--brand-text-muted);
}

.setup-card {
  border-color: rgba(245, 158, 11, 0.45);
}

.setup-steps {
  margin: 0 0 8px;
  padding-left: 20px;
  line-height: 1.6;
}

.setup-steps code,
.setup-code {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: var(--text-xs);
  background: var(--surface-2);
  border: 1px solid var(--brand-border);
  border-radius: 6px;
  padding: 1px 6px;
}

.setup-code {
  display: block;
  margin: 6px 0;
  padding: 8px 10px;
  white-space: pre;
  overflow-x: auto;
}

.error-banner {
  border-color: rgba(239, 68, 68, 0.45);
}

.type-pill {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: var(--text-xs);
  font-weight: 600;
  white-space: nowrap;
}

.status-badge {
  padding: 4px 8px;
  border-radius: 6px;
  font-weight: 600;
}

.student-link {
  color: var(--text-primary);
  font-weight: 600;
  text-decoration: none;
}

.student-link:hover {
  color: var(--color-brand);
  text-decoration: underline;
}

.error-cell {
  max-width: 240px;
}

.preview-card {
  width: 100%;
  max-width: 460px;
  border-radius: var(--radius-lg);
}

/* Mimics a Telegram chat bubble so staff see roughly what parents see. */
.tg-bubble {
  background: var(--surface-2);
  border: 1px solid var(--brand-border);
  border-radius: 14px 14px 14px 4px;
  padding: 10px 14px;
  white-space: pre-wrap;
  line-height: 1.45;
  font-size: var(--text-base);
}
</style>

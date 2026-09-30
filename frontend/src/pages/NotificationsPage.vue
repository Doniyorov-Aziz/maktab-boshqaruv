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
              class="stat-card__icon q-mr-md gt-xs"
              :style="{ background: `${k.color}1f`, color: k.color }"
            >
              <q-icon :name="k.icon" size="24px" />
            </div>
            <div class="col" style="min-width: 0">
              <div class="text-h5 text-weight-bold tabular-nums">
                <q-skeleton v-if="!stats" type="text" width="48px" />
                <template v-else>{{ k.value }}</template>
              </div>
              <div class="text-caption muted-text kpi-label">{{ k.label }}</div>
              <div
                v-if="stats && k.hint"
                class="text-caption muted-text kpi-label"
              >
                {{ k.hint }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Settings: one compact full-width strip so the log table below keeps the full width -->
    <div class="brand-card q-pa-md q-mb-md">
      <div class="row items-center q-col-gutter-md">
        <div class="col-12 col-lg-auto">
          <div class="row items-center no-wrap">
            <q-icon name="tune" size="22px" class="q-mr-sm" />
            <div>
              <div class="text-subtitle1 text-weight-semibold">Sozlamalar</div>
              <div class="text-caption muted-text">
                Maktab bo'yicha qaysi xabarlar yuborilishi<template
                  v-if="!authStore.isAdmin"
                >
                  · faqat administrator o'zgartira oladi</template
                >
              </div>
            </div>
          </div>
        </div>

        <template v-if="settings">
          <div class="col-12 col-md">
            <div class="row items-center q-gutter-x-md q-gutter-y-xs">
              <q-toggle
                v-for="t in settingToggles"
                :key="t.key"
                v-model="settings[t.key]"
                color="primary"
                :disable="!authStore.isAdmin"
              >
                <q-icon
                  :name="t.icon"
                  :style="{ color: t.color }"
                  size="18px"
                />
                {{ t.label }}
                <q-tooltip>{{ t.caption }}</q-tooltip>
              </q-toggle>
              <q-toggle
                v-model="settings.quietHoursEnabled"
                color="primary"
                :disable="!authStore.isAdmin"
              >
                <q-icon name="bedtime" color="indigo-4" size="18px" />
                Tinch soatlar
                <q-tooltip
                  >Bu oraliqda yaratilgan xabarlar tugash vaqtida yuboriladi
                  (Toshkent vaqti)</q-tooltip
                >
              </q-toggle>
            </div>
          </div>
          <div class="col-12 col-sm-auto">
            <div class="row items-center no-wrap q-gutter-sm">
              <q-input
                v-for="f in quietFields"
                :key="f.key"
                v-model="settings[f.key]"
                outlined
                dense
                mask="time"
                :rules="['time']"
                hide-bottom-space
                :label="f.label"
                class="time-input"
                :disable="!authStore.isAdmin || !settings.quietHoursEnabled"
              >
                <template v-slot:append>
                  <q-icon name="schedule" class="cursor-pointer">
                    <q-popup-proxy
                      cover
                      transition-show="scale"
                      transition-hide="scale"
                    >
                      <q-time v-model="settings[f.key]" format24h />
                    </q-popup-proxy>
                  </q-icon>
                </template>
              </q-input>
            </div>
          </div>
          <div v-if="authStore.isAdmin" class="col-12 col-sm-auto">
            <q-btn
              unelevated
              no-caps
              color="primary"
              icon="save"
              label="Saqlash"
              :loading="savingSettings"
              @click="saveSettings"
            />
          </div>
        </template>
        <div v-else class="col">
          <q-skeleton type="QInput" />
        </div>
      </div>
    </div>

    <div>
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
              <span v-if="props.row.lastError" class="error-text">
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

const quietFields = [
  { key: 'quietHoursStart', label: 'Boshlanishi' },
  { key: 'quietHoursEnd', label: 'Tugashi' }
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

.error-text {
  display: block;
  max-width: 280px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.time-input {
  width: 128px;
}

.kpi-label {
  line-height: 1.3;
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

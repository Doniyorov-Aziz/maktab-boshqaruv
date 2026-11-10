<template>
  <page-layout
    icon="payments"
    color="#16a34a"
    title="To'lovlar"
    :subtitle="`Xususiy maktab uchun oylik to'lovlar va qarzdorlik · ${schoolStore.activeSchoolName || ''}`"
  >
    <template #actions>
      <q-btn-toggle
        v-model="tab"
        no-caps
        unelevated
        rounded
        toggle-color="primary"
        :options="tabs"
      />
      <AppButton
        v-if="settings.enabled"
        icon="add"
        label="To'lov qabul qilish"
        @click="openPayment()"
      />
    </template>

    <q-banner
      v-if="loaded && !settings.enabled"
      rounded
      class="brand-card info-banner"
    >
      <template #avatar><q-icon name="info" color="info" /></template>
      To'lovlar bo'limi o'chirilgan. Davlat maktabida to'lov olinmaydi — xususiy
      maktab bo'lsa, administrator «Sozlamalar»da yoqadi.
    </q-banner>

    <!-- Qarzdorlar -->
    <template v-if="tab === 'debtors' && settings.enabled">
      <div class="row q-col-gutter-sm items-center">
        <div class="col-12 col-sm-4 col-md-3">
          <AppSelect
            v-model="classId"
            :options="classes"
            label="Sinf"
            emit-value
            map-options
            clearable
            @update:model-value="loadSummary"
          />
        </div>
        <div class="col-12 col-sm-4 col-md-3">
          <AppSelect
            v-model="month"
            :options="monthOptions"
            label="Oy (yig'ilgan summa uchun)"
            emit-value
            map-options
            @update:model-value="loadSummary"
          />
        </div>
        <q-space />
        <AppButton
          variant="secondary"
          icon="notifications_active"
          label="Qarzdorlarga eslatma"
          :disable="!summary?.rows.length"
          @click="remind()"
        />
      </div>

      <div v-if="summary" class="row q-col-gutter-md">
        <div class="col-6 col-md-3">
          <div class="brand-card stat">
            <div class="stat__value">{{ money(summary.expected) }}</div>
            <div class="stat__label"
              >{{ monthLabel(summary.month) }} — kutilgan</div
            >
          </div>
        </div>
        <div class="col-6 col-md-3">
          <div class="brand-card stat">
            <div class="stat__value text-positive">
              {{ money(summary.collected) }}
            </div>
            <div class="stat__label">yig'ilgan ({{ collectedPercent }})</div>
          </div>
        </div>
        <div class="col-6 col-md-3">
          <div class="brand-card stat">
            <div class="stat__value text-negative">
              {{ money(summary.debt) }}
            </div>
            <div class="stat__label">umumiy qarzdorlik</div>
          </div>
        </div>
        <div class="col-6 col-md-3">
          <div class="brand-card stat">
            <div class="stat__value">
              {{ summary.debtors }} / {{ summary.students }}
            </div>
            <div class="stat__label">qarzdor o'quvchi</div>
          </div>
        </div>
      </div>

      <div class="brand-card overflow-hidden">
        <q-table
          :rows="summary?.rows || []"
          :columns="debtorColumns"
          row-key="studentId"
          :loading="loading"
          flat
          class="brand-table"
          :pagination="{ rowsPerPage: 20 }"
        >
          <template #no-data>
            <div class="full-width column flex-center q-py-xl muted-text">
              <q-icon name="verified" size="48px" class="q-mb-sm" />
              <div class="text-subtitle2">Qarzdorlar yo'q 🎉</div>
            </div>
          </template>
          <template #body-cell-name="props">
            <q-td :props="props">
              <a
                class="link text-weight-medium"
                @click="openAccount(props.row.studentId)"
                >{{ props.row.name }}</a
              >
              <div class="text-caption muted-text">
                {{ props.row.className }} ·
                {{ props.row.guardianName || '—' }}
                <span v-if="props.row.guardianPhone">
                  · {{ props.row.guardianPhone }}</span
                >
              </div>
            </q-td>
          </template>
          <template #body-cell-telegram="props">
            <q-td :props="props">
              <q-icon
                :name="props.row.telegram ? 'telegram' : 'block'"
                :color="props.row.telegram ? 'info' : 'grey-5'"
                size="18px"
              >
                <q-tooltip>{{
                  props.row.telegram ? 'Botga ulangan' : 'Botga ulanmagan'
                }}</q-tooltip>
              </q-icon>
            </q-td>
          </template>
          <template #body-cell-actions="props">
            <q-td :props="props" class="text-right">
              <AppButton
                variant="ghost"
                size="sm"
                icon="add_card"
                aria-label="To'lov"
                @click="openPayment(props.row)"
              >
                <q-tooltip>To'lov qabul qilish</q-tooltip>
              </AppButton>
              <AppButton
                v-if="props.row.telegram"
                variant="ghost"
                size="sm"
                icon="notifications"
                aria-label="Eslatma"
                @click="remind([props.row.studentId])"
              >
                <q-tooltip>Ota-onaga eslatma</q-tooltip>
              </AppButton>
            </q-td>
          </template>
        </q-table>
      </div>
    </template>

    <!-- To'lovlar ro'yxati -->
    <template v-if="tab === 'payments' && settings.enabled">
      <div class="row q-col-gutter-sm">
        <div class="col-12 col-sm-4 col-md-3">
          <AppSelect
            v-model="classId"
            :options="classes"
            label="Sinf"
            emit-value
            map-options
            clearable
            @update:model-value="reloadPayments"
          />
        </div>
        <div class="col-12 col-sm-4 col-md-3">
          <AppSelect
            v-model="listMonth"
            :options="monthOptions"
            label="Qaysi oy uchun"
            emit-value
            map-options
            clearable
            @update:model-value="reloadPayments"
          />
        </div>
      </div>
      <div class="brand-card overflow-hidden">
        <q-table
          :rows="payments"
          :columns="paymentColumns"
          row-key="id"
          :loading="loading"
          v-model:pagination="pagination"
          @request="onRequest"
          flat
          class="brand-table"
          :rows-per-page-options="[20, 50, 100]"
        >
          <template #body-cell-name="props">
            <q-td :props="props">
              <a
                class="link text-weight-medium"
                @click="openAccount(props.row.studentId)"
                >{{ props.row.studentName }}</a
              >
              <div class="text-caption muted-text">{{
                props.row.className
              }}</div>
            </q-td>
          </template>
          <template #body-cell-actions="props">
            <q-td :props="props" class="text-right">
              <AppButton
                variant="ghost"
                size="sm"
                icon="edit"
                aria-label="Tahrirlash"
                @click="openPayment(null, props.row)"
              />
              <AppButton
                v-if="authStore.isAdmin"
                variant="ghost"
                size="sm"
                icon="delete"
                color="negative"
                aria-label="O'chirish"
                @click="removePayment(props.row)"
              />
            </q-td>
          </template>
        </q-table>
      </div>
    </template>

    <!-- Sozlamalar -->
    <div
      v-if="tab === 'settings' && authStore.isAdmin"
      class="brand-card q-pa-md settings-card"
    >
      <div class="text-subtitle1 text-weight-bold q-mb-sm">
        To'lov sozlamalari
      </div>
      <q-toggle
        v-model="settingsForm.enabled"
        label="To'lovlar bo'limi yoqilgan (xususiy maktab)"
      />
      <div class="row q-col-gutter-sm q-mt-sm">
        <div class="col-12 col-sm-6">
          <AppInput
            v-model.number="settingsForm.monthlyFee"
            type="number"
            label="Oylik to'lov (so'm)"
            min="0"
          />
        </div>
        <div class="col-12 col-sm-6">
          <AppInput
            v-model.number="settingsForm.dueDay"
            type="number"
            label="To'lov muddati (oyning nechanchi kuni)"
            min="1"
            max="28"
          />
        </div>
      </div>
      <q-toggle
        v-model="settingsForm.remindParents"
        label="Muddat o'tgan kuni ertasiga qarzdorlarning ota-onalariga botda eslatma"
      />
      <AppInput
        v-model="settingsForm.payInfo"
        type="textarea"
        autogrow
        label="Qanday to'lash mumkin (botda ko'rinadi)"
        hint="Masalan: «Click/Payme: 8600 1234 5678 9012, Aliyev A. Kassa: 1-qavat, 9:00–17:00»"
        maxlength="500"
        class="q-mt-sm"
      />
      <div class="row justify-end q-mt-md">
        <AppButton label="Saqlash" :loading="saving" @click="saveSettings" />
      </div>
    </div>

    <!-- O'quvchi hisobi -->
    <q-dialog v-model="accountOpen">
      <q-card class="dialog-card" style="width: 640px; max-width: 96vw">
        <q-card-section v-if="account" class="row items-center q-pb-none">
          <div>
            <div class="text-h6">{{ account.name }}</div>
            <div class="text-caption muted-text">
              {{ account.className }} · oylik {{ money(account.monthlyFee) }}
              <span v-if="account.ownFee"
                >(alohida{{
                  account.feeNote ? `: ${account.feeNote}` : ''
                }})</span
              >
            </div>
          </div>
          <q-space />
          <AppButton variant="ghost" icon="close" v-close-popup />
        </q-card-section>
        <q-card-section v-if="account">
          <div class="months">
            <div
              v-for="m in account.months"
              :key="m.month"
              class="month"
              :style="{ borderColor: monthStatuses[m.status].color }"
            >
              <div class="text-weight-bold">{{ monthLabel(m.month) }}</div>
              <div
                class="text-caption"
                :style="{ color: monthStatuses[m.status].color }"
              >
                <q-icon :name="monthStatuses[m.status].icon" />
                {{ monthStatuses[m.status].label }}
              </div>
              <div class="text-caption muted-text tabular-nums">
                {{ money(m.paid) }} / {{ money(m.fee) }}
              </div>
            </div>
          </div>
          <div class="row q-mt-md text-body2">
            <div class="col">
              To'langan: <b>{{ money(account.paid) }}</b>
            </div>
            <div class="col text-right">
              Qarzdorlik:
              <b :class="account.debt ? 'text-negative' : 'text-positive'">{{
                money(account.debt)
              }}</b>
            </div>
          </div>
          <q-separator class="q-my-md" />
          <div class="text-subtitle2 q-mb-xs">To'lovlar tarixi</div>
          <div v-if="!account.payments.length" class="muted-text text-caption">
            To'lovlar yo'q
          </div>
          <div
            v-for="p in account.payments"
            :key="p.id"
            class="row items-center text-body2 history-row"
          >
            <div class="col">
              {{ formatDate(p.paidOn) }} · {{ monthLabel(p.month) }} uchun
              <span class="muted-text"
                >· {{ paymentMethods[p.method]
                }}{{ p.note ? ` · ${p.note}` : '' }}</span
              >
            </div>
            <div class="text-weight-bold tabular-nums">{{
              money(p.amount)
            }}</div>
          </div>
          <template v-if="authStore.isAdmin">
            <q-separator class="q-my-md" />
            <div class="text-subtitle2 q-mb-xs">Alohida oylik to'lov</div>
            <div class="row q-col-gutter-sm items-center">
              <div class="col-5">
                <AppInput
                  v-model.number="feeForm.monthlyFee"
                  type="number"
                  label="So'm (0 = ozod)"
                  min="0"
                />
              </div>
              <div class="col-5">
                <AppInput v-model="feeForm.note" label="Sabab (imtiyoz…)" />
              </div>
              <div class="col-2 column q-gutter-xs">
                <AppButton size="sm" label="Saqlash" @click="saveFee(false)" />
                <AppButton
                  v-if="account.ownFee"
                  size="sm"
                  variant="ghost"
                  label="Umumiy"
                  @click="saveFee(true)"
                />
              </div>
            </div>
          </template>
        </q-card-section>
        <q-card-actions align="right" class="q-px-md q-pb-md" v-if="account">
          <AppButton
            icon="add_card"
            label="To'lov qabul qilish"
            @click="
              openPayment({
                studentId: account.studentId,
                name: account.name,
                className: account.className
              })
            "
          />
        </q-card-actions>
      </q-card>
    </q-dialog>

    <!-- To'lov formasi -->
    <q-dialog v-model="paymentOpen" persistent>
      <q-card class="dialog-card" style="width: 480px; max-width: 95vw">
        <q-card-section class="row items-center q-pb-none">
          <div class="text-h6">
            {{ payForm.id ? "To'lovni tahrirlash" : "To'lov qabul qilish" }}
          </div>
          <q-space />
          <AppButton variant="ghost" icon="close" v-close-popup />
        </q-card-section>
        <q-card-section class="q-gutter-y-sm">
          <AppSelect
            v-model="payForm.studentId"
            :options="studentOptions"
            label="O'quvchi *"
            emit-value
            map-options
            use-input
            input-debounce="200"
            @filter="filterStudents"
          />
          <div class="row q-col-gutter-sm">
            <div class="col-6">
              <AppSelect
                v-model="payForm.month"
                :options="monthOptions"
                label="Qaysi oy uchun *"
                emit-value
                map-options
              />
            </div>
            <div class="col-6">
              <AppInput
                v-model.number="payForm.amount"
                type="number"
                label="Summa (so'm) *"
                min="1"
              />
            </div>
          </div>
          <div class="row q-col-gutter-sm">
            <div class="col-6">
              <AppDateInput
                v-model="payForm.paidOn"
                label="To'langan kun"
                no-future
              />
            </div>
            <div class="col-6">
              <AppSelect
                v-model="payForm.method"
                :options="toOptions(paymentMethods)"
                label="Usul"
                emit-value
                map-options
              />
            </div>
          </div>
          <AppInput
            v-model="payForm.note"
            label="Izoh (chek raqami…)"
            maxlength="200"
          />
        </q-card-section>
        <q-card-actions align="right" class="q-px-md q-pb-md">
          <AppButton variant="ghost" label="Bekor qilish" v-close-popup />
          <AppButton
            label="Saqlash"
            :loading="saving"
            :disable="
              !payForm.studentId || !payForm.month || !(payForm.amount > 0)
            "
            @click="savePayment"
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
import {
  errorText,
  loadClasses,
  loadStudents,
  money,
  monthLabel,
  monthStatuses,
  paymentMethods,
  toOptions
} from '@/utils/modules'

const $q = useQuasar()
const schoolStore = useSchoolStore()
const authStore = useAuthStore()

const tab = ref('debtors')
const loaded = ref(false)
const settings = ref({ enabled: false })
const settingsForm = ref({})
const classes = ref([])
const classId = ref(null)
const thisMonth = todayStr().slice(0, 7)
const month = ref(thisMonth)
const listMonth = ref(null)
const summary = ref(null)
const payments = ref([])
const pagination = ref({ page: 1, rowsPerPage: 20, rowsNumber: 0 })
const loading = ref(false)
const saving = ref(false)
const accountOpen = ref(false)
const account = ref(null)
const feeForm = ref({})
const paymentOpen = ref(false)
const payForm = ref({})
const allStudents = ref([])
const studentOptions = ref([])

const tabs = computed(() => [
  ...(settings.value.enabled
    ? [
        { label: 'Qarzdorlar', value: 'debtors' },
        { label: "To'lovlar", value: 'payments' }
      ]
    : []),
  ...(authStore.isAdmin ? [{ label: 'Sozlamalar', value: 'settings' }] : [])
])

// the school year's months (from September) up to the next month, newest first
const monthOptions = computed(() => {
  const out = []
  const [y, m] = thisMonth.split('-').map(Number)
  let cy = m >= 9 ? y : y - 1
  let cm = 9
  const lastY = m === 12 ? y + 1 : y
  const lastM = m === 12 ? 1 : m + 1
  while (cy < lastY || (cy === lastY && cm <= lastM)) {
    const ym = `${cy}-${String(cm).padStart(2, '0')}`
    out.push({ label: monthLabel(ym), value: ym })
    cm++
    if (cm > 12) {
      cm = 1
      cy++
    }
  }
  return out.reverse()
})

// a small share is "<1%", never a misleading "0%"
const collectedPercent = computed(() => {
  const s = summary.value
  if (!s?.expected) return '0%'
  const p = (100 * s.collected) / s.expected
  return s.collected > 0 && p < 1 ? '<1%' : `${Math.round(p)}%`
})

const debtorColumns = [
  {
    name: 'name',
    label: "O'quvchi",
    field: 'name',
    align: 'left',
    sortable: true
  },
  {
    name: 'debt',
    label: 'Qarzdorlik',
    field: 'debt',
    align: 'right',
    sortable: true,
    format: money,
    classes: 'text-negative text-weight-bold tabular-nums'
  },
  {
    name: 'months',
    label: 'Oylar',
    field: 'monthsUnpaid',
    align: 'right',
    sortable: true
  },
  {
    name: 'oldest',
    label: 'Eng eski',
    field: 'oldestUnpaid',
    align: 'left',
    format: monthLabel
  },
  {
    name: 'last',
    label: "So'nggi to'lov",
    field: 'lastPaidOn',
    align: 'left',
    format: v => (v ? formatDate(v) : '—')
  },
  { name: 'telegram', label: 'Bot', field: 'telegram', align: 'center' },
  { name: 'actions', label: '', field: 'studentId', align: 'right' }
]

const paymentColumns = [
  { name: 'name', label: "O'quvchi", field: 'studentName', align: 'left' },
  {
    name: 'month',
    label: 'Oy',
    field: 'month',
    align: 'left',
    format: monthLabel
  },
  {
    name: 'amount',
    label: 'Summa',
    field: 'amount',
    align: 'right',
    format: money,
    classes: 'text-weight-bold tabular-nums'
  },
  {
    name: 'paidOn',
    label: "To'langan",
    field: 'paidOn',
    align: 'left',
    format: formatDate
  },
  {
    name: 'method',
    label: 'Usul',
    field: 'method',
    align: 'left',
    format: v => paymentMethods[v]
  },
  { name: 'note', label: 'Izoh', field: 'note', align: 'left' },
  { name: 'by', label: 'Qabul qildi', field: 'createdBy', align: 'left' },
  { name: 'actions', label: '', field: 'id', align: 'right' }
]

async function loadSettings() {
  if (!schoolStore.activeSchoolId) return
  try {
    settings.value = (
      await api.get('/api/payments/settings', {
        params: { schoolId: schoolStore.activeSchoolId }
      })
    ).data
    settingsForm.value = { ...settings.value }
    if (!settings.value.enabled) tab.value = 'settings'
    loaded.value = true
  } catch (e) {
    $q.notify({ type: 'negative', message: errorText(e) })
  }
}

async function loadSummary() {
  if (!settings.value.enabled) return
  loading.value = true
  try {
    summary.value = (
      await api.get('/api/payments/summary', {
        params: {
          schoolId: schoolStore.activeSchoolId,
          schoolClassId: classId.value || undefined,
          month: month.value
        }
      })
    ).data
  } catch (e) {
    $q.notify({ type: 'negative', message: errorText(e) })
  } finally {
    loading.value = false
  }
}

async function loadPayments() {
  if (!settings.value.enabled) return
  loading.value = true
  try {
    const { page, rowsPerPage } = pagination.value
    const res = await api.get('/api/payments', {
      params: {
        schoolId: schoolStore.activeSchoolId,
        schoolClassId: classId.value || undefined,
        month: listMonth.value || undefined,
        page: page - 1,
        size: rowsPerPage
      }
    })
    payments.value = res.data.content
    pagination.value.rowsNumber = res.data.totalElements
  } catch (e) {
    $q.notify({ type: 'negative', message: errorText(e) })
  } finally {
    loading.value = false
  }
}

function reloadPayments() {
  pagination.value.page = 1
  loadPayments()
}

function onRequest(p) {
  pagination.value = { ...pagination.value, ...p.pagination }
  loadPayments()
}

async function saveSettings() {
  saving.value = true
  try {
    settings.value = (
      await api.put('/api/payments/settings', settingsForm.value, {
        params: { schoolId: schoolStore.activeSchoolId }
      })
    ).data
    $q.notify({ type: 'positive', message: 'Sozlamalar saqlandi' })
    if (settings.value.enabled) {
      tab.value = 'debtors'
      loadSummary()
    }
  } catch (e) {
    $q.notify({ type: 'negative', message: errorText(e) })
  } finally {
    saving.value = false
  }
}

async function openAccount(studentId) {
  try {
    account.value = (await api.get(`/api/payments/student/${studentId}`)).data
    feeForm.value = {
      monthlyFee: account.value.ownFee ? account.value.monthlyFee : null,
      note: account.value.feeNote || ''
    }
    accountOpen.value = true
  } catch (e) {
    $q.notify({ type: 'negative', message: errorText(e) })
  }
}

async function saveFee(reset) {
  try {
    account.value = (
      await api.put('/api/payments/fee', {
        studentId: account.value.studentId,
        monthlyFee: reset ? null : feeForm.value.monthlyFee,
        note: feeForm.value.note || null
      })
    ).data
    $q.notify({ type: 'positive', message: 'Saqlandi' })
    loadSummary()
  } catch (e) {
    $q.notify({ type: 'negative', message: errorText(e) })
  }
}

async function ensureStudents() {
  if (!allStudents.value.length) {
    allStudents.value = (await loadStudents(schoolStore.activeSchoolId)).map(
      s => ({
        ...s,
        label: `${s.label} · ${s.className || ''}`
      })
    )
  }
  studentOptions.value = allStudents.value.slice(0, 50)
}

function filterStudents(val, update) {
  update(() => {
    const q = val.toLowerCase()
    studentOptions.value = allStudents.value
      .filter(s => s.label.toLowerCase().includes(q))
      .slice(0, 50)
  })
}

async function openPayment(debtor, existing) {
  await ensureStudents()
  if (existing) {
    payForm.value = { ...existing }
  } else {
    payForm.value = {
      studentId: debtor?.studentId || null,
      month: debtor?.oldestUnpaid || thisMonth,
      amount: settings.value.monthlyFee || null,
      paidOn: todayStr(),
      method: 'CASH',
      note: ''
    }
  }
  const id = payForm.value.studentId
  if (id && !studentOptions.value.some(s => s.value === id)) {
    const found = allStudents.value.find(s => s.value === id)
    if (found) studentOptions.value = [found, ...studentOptions.value]
  }
  paymentOpen.value = true
}

async function savePayment() {
  saving.value = true
  try {
    const body = {
      studentId: payForm.value.studentId,
      month: payForm.value.month,
      amount: payForm.value.amount,
      paidOn: payForm.value.paidOn || null,
      method: payForm.value.method,
      note: payForm.value.note || null
    }
    if (payForm.value.id)
      await api.put(`/api/payments/${payForm.value.id}`, body)
    else await api.post('/api/payments', body)
    $q.notify({
      type: 'positive',
      message: `${money(body.amount)} qabul qilindi`
    })
    paymentOpen.value = false
    loadSummary()
    loadPayments()
    if (accountOpen.value && account.value?.studentId === body.studentId) {
      openAccount(body.studentId)
    }
  } catch (e) {
    $q.notify({ type: 'negative', message: errorText(e) })
  } finally {
    saving.value = false
  }
}

function removePayment(p) {
  $q.dialog({
    title: "To'lovni o'chirish",
    message: `${p.studentName} — ${money(p.amount)} (${monthLabel(p.month)}) o'chirilsinmi?`,
    cancel: { flat: true, label: 'Bekor qilish', noCaps: true },
    ok: {
      label: "O'chirish",
      color: 'negative',
      unelevated: true,
      noCaps: true
    }
  }).onOk(async () => {
    try {
      await api.delete(`/api/payments/${p.id}`)
      loadPayments()
      loadSummary()
    } catch (e) {
      $q.notify({ type: 'negative', message: errorText(e) })
    }
  })
}

function remind(studentIds) {
  $q.dialog({
    title: 'Eslatma yuborish',
    message: studentIds
      ? 'Ota-onaga botda qarzdorlik haqida eslatma boradi.'
      : `${summary.value.rows.length} ta qarzdor o'quvchining botga ulangan ota-onalariga eslatma boradi (bir kunda bir marta).`,
    cancel: { flat: true, label: 'Bekor qilish', noCaps: true },
    ok: { label: 'Yuborish', color: 'primary', unelevated: true, noCaps: true }
  }).onOk(async () => {
    try {
      const res = await api.post('/api/payments/remind', {
        schoolId: schoolStore.activeSchoolId,
        studentIds: studentIds || []
      })
      $q.notify({
        type: 'positive',
        message: `${res.data.queued} ta eslatma navbatga qo'shildi`
      })
    } catch (e) {
      $q.notify({ type: 'negative', message: errorText(e) })
    }
  })
}

watch(tab, t => {
  if (t === 'debtors') loadSummary()
  if (t === 'payments') loadPayments()
})

async function init() {
  allStudents.value = []
  if (!schoolStore.activeSchoolId) return
  classes.value = await loadClasses(schoolStore.activeSchoolId)
  await loadSettings()
  if (settings.value.enabled) loadSummary()
}

watch(() => schoolStore.activeSchoolId, init)
onMounted(init)
</script>

<style scoped>
.stat {
  padding: 14px 16px;
}
.stat__value {
  font-size: 22px;
  font-weight: 800;
}
.stat__label {
  font-size: 12px;
  opacity: 0.7;
}
.link {
  cursor: pointer;
  color: inherit;
}
.settings-card {
  max-width: 720px;
}
.months {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(130px, 1fr));
  gap: 8px;
}
.month {
  border: 1px solid;
  border-left-width: 4px;
  border-radius: 8px;
  padding: 6px 10px;
}
.history-row {
  padding: 4px 0;
  border-bottom: 1px solid var(--brand-border);
}
</style>

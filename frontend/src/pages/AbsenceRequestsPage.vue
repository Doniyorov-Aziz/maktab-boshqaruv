<template>
  <page-layout
    icon="medical_information"
    color="#f59e0b"
    title="Sababli arizalar"
    :subtitle="`Ota-onalar botdan yuborgan arizalar · ${schoolStore.activeSchoolName || ''}`"
  >
    <template #actions>
      <q-btn-toggle
        v-model="filter"
        no-caps
        unelevated
        rounded
        toggle-color="primary"
        :options="[
          {
            label: `Yangi${pendingCount ? ' · ' + pendingCount : ''}`,
            value: 'PENDING'
          },
          { label: 'Tasdiqlangan', value: 'APPROVED' },
          { label: 'Rad etilgan', value: 'REJECTED' },
          { label: 'Hammasi', value: 'ALL' }
        ]"
        @update:model-value="reload"
      />
      <q-btn
        flat
        round
        dense
        icon="refresh"
        aria-label="Yangilash"
        @click="reload"
      >
        <q-tooltip>Yangilash</q-tooltip>
      </q-btn>
    </template>

    <q-banner rounded dense class="brand-card q-mb-md info-banner">
      <template v-slot:avatar>
        <q-icon name="info" color="info" />
      </template>
      Tasdiqlangan arizaning barcha kunlaridagi darslar davomatda avtomatik
      <b>«Sababli»</b> deb belgilanadi (bayram va ta'til kunlaridan tashqari),
      ota-onaga esa botda javob boradi.
    </q-banner>

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
            <q-icon name="task_alt" size="48px" class="q-mb-sm" />
            <div class="text-subtitle2">
              {{
                filter === 'PENDING'
                  ? "Ko'rib chiqilmagan ariza yo'q 🎉"
                  : "Arizalar yo'q"
              }}
            </div>
          </div>
        </template>

        <template v-slot:body-cell-student="props">
          <q-td :props="props">
            <router-link
              :to="`/profiles/student/${props.row.studentId}`"
              class="student-link"
              >{{ props.row.studentName }}</router-link
            >
            <div class="text-caption muted-text">
              {{ props.row.className }} sinf ·
              {{ props.row.parentName || 'Ota-ona' }}
            </div>
          </q-td>
        </template>

        <template v-slot:body-cell-dates="props">
          <q-td :props="props" class="tabular-nums">
            {{ formatDate(props.row.dateFrom) }} –
            {{ formatDate(props.row.dateTo) }}
            <div class="text-caption muted-text">{{ days(props.row) }} kun</div>
          </q-td>
        </template>

        <template v-slot:body-cell-reason="props">
          <q-td :props="props">
            <span
              class="reason-pill"
              :style="{
                color: absenceReasons[props.row.reason]?.color,
                background: `${absenceReasons[props.row.reason]?.color}1a`
              }"
            >
              <q-icon
                :name="absenceReasons[props.row.reason]?.icon"
                size="14px"
              />
              {{ absenceReasons[props.row.reason]?.label }}
            </span>
            <div v-if="props.row.comment" class="text-caption comment-text">
              «{{ props.row.comment }}»
            </div>
          </q-td>
        </template>

        <template v-slot:body-cell-photo="props">
          <q-td :props="props">
            <q-btn
              v-if="props.row.hasPhoto"
              flat
              dense
              round
              icon="image"
              color="primary"
              aria-label="Rasmni ko'rish"
              @click="
                photo = {
                  url: `/api/absence-requests/${props.row.id}/photo`,
                  title: `${props.row.studentName} — ma'lumotnoma`
                }
              "
            >
              <q-tooltip>Ma'lumotnoma rasmi</q-tooltip>
            </q-btn>
            <span v-else class="muted-text">—</span>
          </q-td>
        </template>

        <template v-slot:body-cell-status="props">
          <q-td :props="props">
            <q-badge
              :color="absenceStatuses[props.row.status]?.color"
              class="status-badge"
            >
              <q-icon
                :name="absenceStatuses[props.row.status]?.icon"
                size="12px"
                class="q-mr-xs"
              />
              {{ absenceStatuses[props.row.status]?.label }}
            </q-badge>
            <div
              v-if="props.row.decisionNote"
              class="text-caption muted-text q-mt-xs"
            >
              {{ props.row.decisionNote }}
            </div>
            <div
              v-else-if="
                props.row.status === 'APPROVED' &&
                props.row.excusedLessons != null
              "
              class="text-caption muted-text q-mt-xs"
            >
              {{ props.row.excusedLessons }} ta dars «sababli»
            </div>
          </q-td>
        </template>

        <template v-slot:body-cell-actions="props">
          <q-td :props="props" class="text-right no-wrap">
            <template v-if="props.row.status === 'PENDING'">
              <q-btn
                unelevated
                dense
                no-caps
                color="positive"
                icon="check"
                label="Tasdiqlash"
                class="q-mr-xs action-btn"
                :loading="busy === props.row.id"
                @click="approve(props.row)"
              />
              <q-btn
                outline
                dense
                no-caps
                color="negative"
                icon="close"
                label="Rad etish"
                class="action-btn"
                @click="openReject(props.row)"
              />
            </template>
            <span v-else class="text-caption muted-text">
              {{ props.row.decidedBy }} ·
              {{ formatDateTime(props.row.decidedAt) }}
            </span>
          </q-td>
        </template>
      </q-table>
    </div>

    <q-dialog v-model="rejectOpen">
      <q-card class="dialog-card">
        <q-card-section class="row items-center q-pb-none">
          <div class="text-h6">Arizani rad etish</div>
          <q-space />
          <q-btn flat round dense icon="close" v-close-popup />
        </q-card-section>
        <q-card-section v-if="rejecting">
          <div class="text-body2 q-mb-md">
            <b>{{ rejecting.studentName }}</b> ({{ rejecting.className }}),
            {{ formatDate(rejecting.dateFrom) }} –
            {{ formatDate(rejecting.dateTo) }}
          </div>
          <q-input
            v-model="rejectReason"
            type="textarea"
            outlined
            autogrow
            autofocus
            label="Sabab (ota-onaga ko'rsatiladi)"
            hint="Masalan: «Ma'lumotnoma talab qilinadi, iltimos, sinf rahbariga olib keling»"
          />
        </q-card-section>
        <q-card-actions align="right" class="q-px-md q-pb-md">
          <q-btn
            flat
            no-caps
            label="Bekor qilish"
            color="grey-7"
            v-close-popup
          />
          <q-btn
            unelevated
            no-caps
            color="negative"
            label="Rad etish"
            :disable="!rejectReason.trim()"
            :loading="busy === rejecting?.id"
            @click="reject"
          />
        </q-card-actions>
      </q-card>
    </q-dialog>

    <auth-image-dialog
      :url="photo?.url || ''"
      :title="photo?.title || 'Rasm'"
      @close="photo = null"
    />
  </page-layout>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useQuasar } from 'quasar'
import { api } from '@/boot/axios'
import PageLayout from '@/components/PageLayout.vue'
import AuthImageDialog from '@/components/AuthImageDialog.vue'
import { useSchoolStore } from '@/stores/school'
import { formatDate, parseLocalDate } from '@/utils/date'
import {
  absenceReasons,
  absenceStatuses,
  formatDateTime
} from '@/utils/telegram'

const $q = useQuasar()
const schoolStore = useSchoolStore()

const filter = ref('PENDING')
const rows = ref([])
const loading = ref(false)
const pendingCount = ref(0)
const pagination = ref({ page: 1, rowsPerPage: 10, rowsNumber: 0 })
const busy = ref(null)
const photo = ref(null)
const rejectOpen = ref(false)
const rejecting = ref(null)
const rejectReason = ref('')

const columns = [
  { name: 'student', label: "O'quvchi", field: 'studentName', align: 'left' },
  { name: 'dates', label: 'Kunlar', field: 'dateFrom', align: 'left' },
  { name: 'reason', label: 'Sabab', field: 'reason', align: 'left' },
  { name: 'photo', label: 'Rasm', field: 'hasPhoto', align: 'center' },
  {
    name: 'created',
    label: 'Yuborilgan',
    field: 'createdAt',
    align: 'left',
    format: v => formatDateTime(v)
  },
  { name: 'status', label: 'Holati', field: 'status', align: 'left' },
  { name: 'actions', label: '', field: 'id', align: 'right' }
]

function days(r) {
  return (
    Math.round(
      (parseLocalDate(r.dateTo) - parseLocalDate(r.dateFrom)) / 86400000
    ) + 1
  )
}

async function load() {
  if (!schoolStore.activeSchoolId) return
  loading.value = true
  try {
    const { page, rowsPerPage } = pagination.value
    const [list, count] = await Promise.all([
      api.get('/api/absence-requests', {
        params: {
          schoolId: schoolStore.activeSchoolId,
          status: filter.value === 'ALL' ? undefined : filter.value,
          page: page - 1,
          size: rowsPerPage
        }
      }),
      api.get('/api/absence-requests/count-pending', {
        params: { schoolId: schoolStore.activeSchoolId }
      })
    ])
    rows.value = list.data.content
    pagination.value.rowsNumber = list.data.totalElements
    pendingCount.value = count.data
  } catch (error) {
    $q.notify({
      type: 'negative',
      message: error.friendlyMessage || 'Xatolik yuz berdi'
    })
  } finally {
    loading.value = false
  }
}

function onRequest(req) {
  pagination.value = req.pagination
  load()
}

function reload() {
  pagination.value.page = 1
  load()
}

function approve(row) {
  $q.dialog({
    title: 'Arizani tasdiqlash',
    message: `${row.studentName}: ${formatDate(row.dateFrom)} – ${formatDate(row.dateTo)} kunlaridagi darslar «Sababli» deb belgilanadi va ota-onaga xabar boradi. Davom etasizmi?`,
    cancel: {
      flat: true,
      label: 'Bekor qilish',
      color: 'grey-7',
      noCaps: true
    },
    ok: {
      unelevated: true,
      label: 'Tasdiqlash',
      color: 'positive',
      noCaps: true
    }
  }).onOk(async () => {
    busy.value = row.id
    try {
      const res = await api.post(`/api/absence-requests/${row.id}/approve`)
      $q.notify({
        type: 'positive',
        icon: 'task_alt',
        message: `Tasdiqlandi — ${res.data.excusedLessons} ta dars «sababli» deb belgilandi`
      })
      load()
    } catch (error) {
      $q.notify({
        type: 'negative',
        message: error.friendlyMessage || 'Xatolik yuz berdi'
      })
    } finally {
      busy.value = null
    }
  })
}

function openReject(row) {
  rejecting.value = row
  rejectReason.value = ''
  rejectOpen.value = true
}

async function reject() {
  busy.value = rejecting.value.id
  try {
    await api.post(`/api/absence-requests/${rejecting.value.id}/reject`, {
      text: rejectReason.value.trim()
    })
    rejectOpen.value = false
    $q.notify({
      type: 'positive',
      message: 'Ariza rad etildi, ota-onaga sababi yuborildi'
    })
    load()
  } catch (error) {
    $q.notify({
      type: 'negative',
      message: error.friendlyMessage || 'Xatolik yuz berdi'
    })
  } finally {
    busy.value = null
  }
}

watch(() => schoolStore.activeSchoolId, reload, { immediate: true })
</script>

<style scoped>
.muted-text {
  color: var(--brand-text-muted);
}

.info-banner {
  border-color: rgba(14, 165, 233, 0.35);
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

.reason-pill {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: var(--text-xs);
  font-weight: 600;
  white-space: nowrap;
}

.comment-text {
  max-width: 260px;
  white-space: normal;
  color: var(--text-primary);
  margin-top: 4px;
}

.status-badge {
  padding: 4px 8px;
  border-radius: 6px;
  font-weight: 600;
}

.action-btn {
  padding: 2px 10px;
}

.dialog-card {
  width: 100%;
  max-width: 520px;
  border-radius: var(--radius-lg);
}
</style>

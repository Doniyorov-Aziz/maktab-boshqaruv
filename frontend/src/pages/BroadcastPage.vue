<template>
  <page-layout
    icon="campaign"
    color="#ec4899"
    title="Ota-onalarga xabar"
    :subtitle="`Telegram bot orqali umumiy xabar · ${schoolStore.activeSchoolName || ''}`"
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
            class="q-mb-md"
            :options="[
              { label: 'Butun maktab', value: 'ALL', icon: 'domain' },
              { label: 'Tanlangan sinflar', value: 'CLASSES', icon: 'groups' }
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

          <q-input
            v-model="text"
            type="textarea"
            outlined
            autogrow
            counter
            maxlength="3500"
            label="Xabar matni"
            placeholder="Masalan: Hurmatli ota-onalar! 7-oktabr kuni soat 18:00 da umumiy yig'ilish bo'lib o'tadi."
            input-style="min-height: 140px"
            @update:model-value="resetPreview"
          />

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
            Matnni yozib, «Oldindan ko'rish»ni bosing
          </div>
          <template v-else>
            <div class="tg-chat">
              <!-- eslint-disable-next-line vue/no-v-html -->
              <div
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

    <div class="brand-card overflow-hidden q-mt-md">
      <div class="q-pa-md text-subtitle1 text-weight-semibold"
        >Yuborilgan xabarlar</div
      >
      <q-table
        :rows="history"
        :columns="columns"
        row-key="id"
        :loading="loadingHistory"
        flat
        class="brand-table"
        :pagination="{ rowsPerPage: 10 }"
      >
        <template v-slot:no-data>
          <div class="full-width column flex-center q-py-lg muted-text">
            <q-icon name="outbox" size="40px" class="q-mb-sm" />
            Hali umumiy xabar yuborilmagan
          </div>
        </template>
        <template v-slot:body-cell-text="props">
          <q-td :props="props" class="text-cell">
            <span class="ellipsis-2">{{ props.row.text }}</span>
            <q-tooltip max-width="420px">{{ props.row.text }}</q-tooltip>
          </q-td>
        </template>
        <template v-slot:body-cell-audience="props">
          <q-td :props="props">
            {{
              props.row.audience === 'ALL'
                ? 'Butun maktab'
                : props.row.classNames.join(', ')
            }}
          </q-td>
        </template>
        <template v-slot:body-cell-result="props">
          <q-td :props="props" style="min-width: 220px">
            <div class="row items-center q-gutter-x-sm text-caption">
              <span class="text-positive">✓ {{ props.row.sent }}</span>
              <span v-if="props.row.pending" class="text-warning"
                >⏳ {{ props.row.pending }}</span
              >
              <span v-if="props.row.failed" class="text-negative"
                >✕ {{ props.row.failed }}</span
              >
              <span v-if="props.row.skipped" class="muted-text"
                >⊘ {{ props.row.skipped }}</span
              >
              <span class="muted-text">/ {{ props.row.recipientCount }}</span>
            </div>
            <q-linear-progress
              :value="
                props.row.recipientCount
                  ? props.row.sent / props.row.recipientCount
                  : 0
              "
              color="positive"
              track-color="grey-4"
              rounded
              size="6px"
              class="q-mt-xs"
            />
          </q-td>
        </template>
      </q-table>
    </div>
  </page-layout>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useQuasar } from 'quasar'
import { api } from '@/boot/axios'
import PageLayout from '@/components/PageLayout.vue'
import { useSchoolStore } from '@/stores/school'
import { formatDateTime, telegramHtmlToSafe } from '@/utils/telegram'

const $q = useQuasar()
const schoolStore = useSchoolStore()

const audience = ref('ALL')
const classIds = ref([])
const classOptions = ref([])
const text = ref('')
const preview = ref(null)
const previewing = ref(false)
const sending = ref(false)
const history = ref([])
const loadingHistory = ref(false)

const canSend = computed(
  () =>
    text.value.trim().length > 0 &&
    (audience.value === 'ALL' || classIds.value.length > 0)
)

const columns = [
  {
    name: 'createdAt',
    label: 'Sana',
    field: 'createdAt',
    align: 'left',
    format: v => formatDateTime(v)
  },
  { name: 'text', label: 'Matn', field: 'text', align: 'left' },
  { name: 'audience', label: 'Kimga', field: 'audience', align: 'left' },
  { name: 'result', label: 'Natija', field: 'sent', align: 'left' },
  { name: 'createdBy', label: 'Yuborgan', field: 'createdBy', align: 'left' }
]

function body() {
  return {
    schoolId: schoolStore.activeSchoolId,
    text: text.value.trim(),
    audience: audience.value,
    classIds: audience.value === 'CLASSES' ? classIds.value : null
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
    $q.notify({
      type: 'negative',
      message: error.friendlyMessage || 'Xatolik yuz berdi'
    })
  } finally {
    previewing.value = false
  }
}

function confirmSend() {
  $q.dialog({
    title: 'Xabarni yuborish',
    message: `Xabar ${preview.value.recipientCount} ta ota-onaga Telegram orqali yuboriladi. Yuborilgan xabarni qaytarib bo'lmaydi.`,
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
      await api.post('/api/broadcasts', body())
      $q.notify({
        type: 'positive',
        icon: 'send',
        message: "Xabar navbatga qo'yildi va yuborilmoqda"
      })
      text.value = ''
      preview.value = null
      loadHistory()
    } catch (error) {
      $q.notify({
        type: 'negative',
        message: error.friendlyMessage || 'Xatolik yuz berdi'
      })
    } finally {
      sending.value = false
    }
  })
}

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

async function loadHistory() {
  loadingHistory.value = true
  try {
    history.value = (
      await api.get('/api/broadcasts', {
        params: { schoolId: schoolStore.activeSchoolId, size: 50 }
      })
    ).data.content
  } catch (error) {
    $q.notify({
      type: 'negative',
      message: error.friendlyMessage || 'Xatolik yuz berdi'
    })
  } finally {
    loadingHistory.value = false
  }
}

watch(
  () => schoolStore.activeSchoolId,
  id => {
    if (!id) return
    preview.value = null
    classIds.value = []
    loadClasses()
    loadHistory()
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

.recipients {
  font-size: var(--text-md);
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
</style>

<template>
  <div class="brand-card q-pa-md telegram-card">
    <div class="row items-center no-wrap q-mb-md">
      <q-avatar size="40px" class="tg-avatar" text-color="white" icon="send" />
      <div class="col q-ml-sm">
        <div class="text-subtitle1 text-weight-semibold">Telegram</div>
        <div class="text-caption muted-text">
          <template v-if="loading">Yuklanmoqda…</template>
          <template v-else-if="info"
            >{{ info.linkedCount }} ta ota-ona ulangan</template
          >
        </div>
      </div>
      <q-chip
        v-if="status"
        dense
        square
        :color="botModes[status.mode]?.color"
        text-color="white"
        :icon="botModes[status.mode]?.icon"
        class="gt-xs"
        >{{ botModes[status.mode]?.label }}</q-chip
      >
    </div>

    <div v-if="loading" class="row q-col-gutter-md">
      <div class="col-auto"><q-skeleton width="176px" height="176px" /></div>
      <div class="col">
        <q-skeleton type="text" width="60%" />
        <q-skeleton type="QInput" class="q-mt-sm" />
      </div>
    </div>

    <template v-else-if="info">
      <q-banner
        v-if="status && !status.active"
        dense
        rounded
        class="tg-banner tg-banner--warning q-mb-md"
      >
        <template v-slot:avatar>
          <q-icon name="warning" color="warning" />
        </template>
        Bot sozlanmagan — ota-onalar ulanishi mumkin, lekin xabarlar
        yuborilmaydi.
        <router-link to="/notifications" class="tg-link"
          >Sozlash yo'riqnomasi</router-link
        >
      </q-banner>

      <div class="row q-col-gutter-lg items-start">
        <div class="col-12 col-sm-auto column items-center">
          <qr-code
            :value="info.deepLink || ''"
            :size="160"
            :alt="`${info.studentName} uchun QR kod`"
          />
          <div class="text-caption muted-text q-mt-xs text-center"
            >Ota-ona telefon kamerasi bilan skanerlaydi</div
          >
        </div>

        <div class="col">
          <div class="text-caption muted-text q-mb-xs">Bog'lash kodi</div>
          <div class="row items-center q-gutter-sm q-mb-md">
            <code class="link-code">{{ info.linkCode }}</code>
            <q-btn
              flat
              dense
              round
              size="sm"
              icon="content_copy"
              aria-label="Kodni nusxalash"
              @click="copy(info.linkCode, 'Kod nusxalandi')"
            >
              <q-tooltip>Kodni nusxalash</q-tooltip>
            </q-btn>
          </div>

          <div class="text-caption muted-text q-mb-xs">Havola (deep link)</div>
          <q-input
            v-if="info.deepLink"
            :model-value="info.deepLink"
            readonly
            outlined
            dense
            class="q-mb-md"
          >
            <template v-slot:append>
              <q-btn
                flat
                dense
                round
                icon="content_copy"
                aria-label="Havolani nusxalash"
                @click="copy(info.deepLink, 'Havola nusxalandi')"
              >
                <q-tooltip>Havolani nusxalash</q-tooltip>
              </q-btn>
            </template>
          </q-input>
          <div v-else class="text-caption text-warning q-mb-md">
            TELEGRAM_BOT_USERNAME sozlanmagan — havola va QR kod yaratib
            bo'lmaydi.
          </div>

          <q-btn
            outline
            no-caps
            color="primary"
            icon="autorenew"
            label="Kodni yangilash"
            :loading="regenerating"
            @click="confirmRegenerate"
          />
          <div class="text-caption muted-text q-mt-xs">
            Eski kod darhol bekor bo'ladi, ulangan ota-onalar saqlanadi.
          </div>
        </div>
      </div>

      <q-separator class="q-my-md" />

      <div class="text-subtitle2 text-weight-semibold q-mb-sm"
        >Ulangan ota-onalar</div
      >
      <div v-if="!info.links.length" class="muted-text text-body2">
        Hali hech bir ota-ona ulanmagan. QR kodni ota-onaga bering yoki ular
        botda /start bosib telefon raqamini ulashsin.
      </div>
      <q-list v-else separator class="parent-list">
        <q-item v-for="l in info.links" :key="l.id" class="q-px-none">
          <q-item-section avatar>
            <q-avatar size="34px" class="tg-avatar" text-color="white">{{
              (l.firstName || '?').charAt(0).toUpperCase()
            }}</q-avatar>
          </q-item-section>
          <q-item-section>
            <q-item-label>{{ l.firstName || "Noma'lum" }}</q-item-label>
            <q-item-label caption>
              <span v-if="l.telegramUsername">@{{ l.telegramUsername }} · </span
              >Ulangan: {{ formatDate(l.linkedAt?.slice(0, 10)) }}
            </q-item-label>
          </q-item-section>
          <q-item-section side>
            <q-btn
              flat
              dense
              no-caps
              color="negative"
              icon="link_off"
              label="Uzish"
              @click="confirmUnlink(l)"
            />
          </q-item-section>
        </q-item>
      </q-list>
    </template>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useQuasar, copyToClipboard } from 'quasar'
import { api } from '@/boot/axios'
import QrCode from '@/components/QrCode.vue'
import { formatDate } from '@/utils/date'
import { botModes } from '@/utils/telegram'

const props = defineProps({
  studentId: { type: [Number, String], required: true }
})

const $q = useQuasar()
const info = ref(null)
const status = ref(null)
const loading = ref(true)
const regenerating = ref(false)

function notifyError(error) {
  $q.notify({
    type: 'negative',
    message: error.friendlyMessage || 'Xatolik yuz berdi'
  })
}

async function load() {
  loading.value = true
  try {
    const [studentRes, statusRes] = await Promise.all([
      api.get(`/api/telegram/students/${props.studentId}`),
      api.get('/api/telegram/status')
    ])
    info.value = studentRes.data
    status.value = statusRes.data
  } catch (error) {
    notifyError(error)
  } finally {
    loading.value = false
  }
}

async function copy(text, message) {
  try {
    await copyToClipboard(text)
    $q.notify({ type: 'positive', message, icon: 'content_copy' })
  } catch {
    $q.notify({ type: 'negative', message: "Nusxalab bo'lmadi" })
  }
}

function confirmRegenerate() {
  $q.dialog({
    title: 'Kodni yangilash',
    message:
      'Yangi kod yaratiladi va eski QR kod/havola ishlamay qoladi. Allaqachon ulangan ota-onalar uzilmaydi. Davom etasizmi?',
    cancel: {
      flat: true,
      label: 'Bekor qilish',
      color: 'grey-7',
      noCaps: true
    },
    ok: { unelevated: true, label: 'Yangilash', color: 'primary', noCaps: true }
  }).onOk(async () => {
    regenerating.value = true
    try {
      const response = await api.post(
        `/api/telegram/students/${props.studentId}/regenerate-code`
      )
      info.value = response.data
      $q.notify({
        type: 'positive',
        message: 'Yangi kod yaratildi',
        icon: 'check_circle'
      })
    } catch (error) {
      notifyError(error)
    } finally {
      regenerating.value = false
    }
  })
}

function confirmUnlink(link) {
  $q.dialog({
    title: 'Ota-onani uzish',
    message: `${link.firstName || 'Bu ota-ona'} endi bu o'quvchi haqida xabar olmaydi. Davom etasizmi?`,
    cancel: {
      flat: true,
      label: 'Bekor qilish',
      color: 'grey-7',
      noCaps: true
    },
    ok: { unelevated: true, label: 'Uzish', color: 'negative', noCaps: true }
  }).onOk(async () => {
    try {
      await api.delete(`/api/telegram/links/${link.id}`)
      $q.notify({
        type: 'positive',
        message: "Bog'lanish uzildi",
        icon: 'link_off'
      })
      load()
    } catch (error) {
      notifyError(error)
    }
  })
}

watch(() => props.studentId, load, { immediate: true })
</script>

<style scoped>
.muted-text {
  color: var(--brand-text-muted);
}

/* Telegram's brand blue — used only as the avatar accent of this card. */
.tg-avatar {
  background: #229ed9;
}

.link-code {
  font-family: 'JetBrains Mono', ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: var(--text-md);
  letter-spacing: 0.08em;
  padding: 6px 12px;
  border-radius: var(--radius-sm);
  background: var(--surface-2);
  border: 1px solid var(--brand-border);
  color: var(--text-primary);
  user-select: all;
}

.tg-banner {
  border: 1px solid var(--brand-border);
  background: var(--surface-2);
  color: var(--text-primary);
}

.tg-banner--warning {
  border-color: rgba(245, 158, 11, 0.45);
}

.tg-link {
  color: var(--color-brand);
  font-weight: 600;
  margin-left: 4px;
}

.parent-list :deep(.q-item) {
  min-height: 52px;
}
</style>

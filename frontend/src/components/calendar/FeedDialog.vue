<template>
  <q-dialog
    :model-value="modelValue"
    @update:model-value="v => emit('update:modelValue', v)"
  >
    <q-card class="app-dialog app-dialog--md fd" data-feed-dialog>
      <q-card-section class="row items-center q-pb-none">
        <div class="text-h6">Kalendarga qo'shish</div>
        <q-space />
        <AppButton
          variant="ghost"
          icon="close"
          aria-label="Yopish"
          v-close-popup
        />
      </q-card-section>
      <q-card-section class="fd__body">
        <div class="fd__block">
          <b>.ics fayl</b>
          <p
            >Bir martalik nusxa: Google, Apple yoki Outlook kalendariga import
            qiling.</p
          >
          <AppButton
            icon="download"
            label="taqvim.ics yuklab olish"
            :loading="downloading"
            data-ics-download
            @click="download"
          />
        </div>
        <div class="fd__block">
          <b>Obuna havolasi</b>
          <p>
            Kalendar o'zi yangilanib turadi. Havola maxfiy — kimda bo'lsa,
            tadbirlarni ko'radi. Kerak bo'lmasa, bekor qiling.
          </p>
          <template v-if="admin">
            <div v-for="f in feeds" :key="f.id" class="fd__feed" data-feed>
              <code>{{ link(f) }}</code>
              <AppButton
                variant="ghost"
                size="sm"
                icon="content_copy"
                aria-label="Nusxa olish"
                @click="copy(f)"
              />
              <AppButton
                variant="ghost"
                size="sm"
                icon="link_off"
                color="negative"
                aria-label="Bekor qilish"
                data-feed-revoke
                @click="revoke(f)"
              />
            </div>
            <AppButton
              variant="secondary"
              icon="add_link"
              label="Yangi havola"
              :loading="creating"
              data-feed-create
              @click="create"
            />
          </template>
          <p v-else class="fd__note"
            >Obuna havolasini faqat administrator yaratadi.</p
          >
        </div>
      </q-card-section>
    </q-card>
  </q-dialog>
</template>

<script setup>
// The calendar outside the app: a one-time .ics file, and (admin) secret subscription links
// that Google / Apple calendars refresh by themselves — each can be revoked.
import { ref, watch } from 'vue'
import { copyToClipboard, useQuasar } from 'quasar'
import AppButton from '@/components/app/AppButton.vue'
import { api } from '@/boot/axios'
import { useSchoolStore } from '@/stores/school'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  admin: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue'])
const $q = useQuasar()
const school = useSchoolStore()

const feeds = ref([])
const downloading = ref(false)
const creating = ref(false)

watch(
  () => props.modelValue,
  async open => {
    if (!open || !props.admin) return
    const res = await api.get('/api/pages/calendar/feeds', {
      params: { schoolId: school.activeSchoolId }
    })
    feeds.value = res.data
  }
)

function link(f) {
  const base = (api.defaults.baseURL || window.location.origin).replace(
    /\/$/,
    ''
  )
  return `${base}/api/public/calendar/${f.token}.ics`
}

async function download() {
  downloading.value = true
  try {
    const res = await api.get('/api/pages/calendar/export.ics', {
      params: { schoolId: school.activeSchoolId },
      responseType: 'blob'
    })
    const url = URL.createObjectURL(res.data)
    const a = document.createElement('a')
    a.href = url
    a.download = 'taqvim.ics'
    document.body.appendChild(a)
    a.click()
    a.remove()
    setTimeout(() => URL.revokeObjectURL(url), 1000)
  } finally {
    downloading.value = false
  }
}

async function create() {
  creating.value = true
  try {
    const res = await api.post('/api/pages/calendar/feeds', null, {
      params: { schoolId: school.activeSchoolId }
    })
    feeds.value = [res.data, ...feeds.value]
    copy(res.data)
  } finally {
    creating.value = false
  }
}

async function revoke(f) {
  await api.delete(`/api/pages/calendar/feeds/${f.id}`, {
    params: { schoolId: school.activeSchoolId }
  })
  feeds.value = feeds.value.filter(x => x.id !== f.id)
  $q.notify({ type: 'info', message: 'Havola bekor qilindi' })
}

async function copy(f) {
  try {
    await copyToClipboard(link(f))
    $q.notify({ type: 'positive', message: 'Havola nusxalandi' })
  } catch {
    // the link is on screen to copy by hand
  }
}
</script>

<style scoped>
.fd__body {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.fd__block {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: var(--space-2);
}

.fd__block p {
  margin: 0;
  font-size: var(--text-sm);
  color: var(--brand-text-muted);
}

.fd__feed {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  width: 100%;
}

.fd__feed code {
  flex: 1 1 auto;
  min-width: 0;
  padding: var(--space-1) var(--space-2);
  border-radius: var(--radius-sm);
  background: var(--surface-2);
  font-size: var(--text-xs);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.fd__note {
  font-style: italic;
}
</style>

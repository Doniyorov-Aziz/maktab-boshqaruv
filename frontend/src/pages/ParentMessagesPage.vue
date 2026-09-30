<template>
  <page-layout
    icon="forum"
    color="#0ea5e9"
    title="Murojaatlar"
    :subtitle="`Ota-onalar botdan yozgan xabarlar · ${schoolStore.activeSchoolName || ''}`"
  >
    <template #actions>
      <q-btn-toggle
        v-model="filter"
        no-caps
        unelevated
        rounded
        toggle-color="primary"
        class="filter-toggle"
        :options="[
          { label: `Yangi${newCount ? ' · ' + newCount : ''}`, value: 'NEW' },
          { label: 'Javob berilgan', value: 'ANSWERED' },
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

    <div v-if="loading && !items.length" class="column q-gutter-md">
      <q-skeleton v-for="i in 3" :key="i" height="140px" class="brand-card" />
    </div>

    <div
      v-else-if="!items.length"
      class="brand-card q-pa-xl column flex-center muted-text empty-state"
    >
      <q-icon name="mark_chat_read" size="56px" class="q-mb-sm" />
      <div class="text-subtitle1">
        {{
          filter === 'NEW'
            ? "Yangi murojaat yo'q — hammasiga javob berilgan 🎉"
            : "Hozircha murojaat yo'q"
        }}
      </div>
      <div class="text-caption">
        Ota-onalar botdagi «💬 Maktabga yozish» bo'limidan yozishadi.
      </div>
    </div>

    <div v-else class="column q-gutter-md">
      <div
        v-for="m in items"
        :key="m.id"
        class="brand-card q-pa-md message-card"
      >
        <div class="row items-start no-wrap q-col-gutter-md">
          <div class="col-auto">
            <q-avatar size="44px" class="tg-avatar" text-color="white">{{
              initial(m.parentName)
            }}</q-avatar>
          </div>
          <div class="col" style="min-width: 0">
            <div class="row items-center q-gutter-x-sm q-mb-xs">
              <span class="text-weight-semibold">{{
                m.parentName || 'Ota-ona'
              }}</span>
              <span v-if="m.parentUsername" class="muted-text text-caption"
                >@{{ m.parentUsername }}</span
              >
              <q-chip
                dense
                square
                class="meta-chip"
                :icon="recipients[m.recipient]?.icon"
              >
                {{ recipients[m.recipient]?.label }}
              </q-chip>
              <q-space />
              <q-badge
                :color="m.status === 'NEW' ? 'warning' : 'positive'"
                class="status-badge"
              >
                <q-icon
                  :name="m.status === 'NEW' ? 'mark_email_unread' : 'done_all'"
                  size="12px"
                  class="q-mr-xs"
                />
                {{ m.status === 'NEW' ? 'Yangi' : 'Javob berilgan' }}
              </q-badge>
            </div>
            <div class="text-caption muted-text q-mb-sm">
              <router-link
                :to="`/profiles/student/${m.studentId}`"
                class="student-link"
              >
                {{ m.studentName }}</router-link
              >
              · {{ m.className }} sinf · {{ formatDateTime(m.createdAt) }}
            </div>
            <div class="bubble bubble--in">{{ m.text }}</div>
            <q-btn
              v-if="m.hasPhoto"
              flat
              dense
              no-caps
              size="sm"
              icon="image"
              label="Rasmni ko'rish"
              color="primary"
              class="q-mt-xs"
              @click="
                photo = {
                  url: `/api/parent-messages/${m.id}/photo`,
                  title: m.studentName
                }
              "
            />
            <div v-if="m.replyText" class="bubble bubble--out q-mt-sm">
              {{ m.replyText }}
              <div class="bubble-meta">
                <q-icon name="done_all" size="12px" />
                {{ m.repliedBy || 'Maktab' }} ·
                {{ formatDateTime(m.repliedAt) }}
              </div>
            </div>
            <div v-else class="q-mt-sm">
              <q-btn
                unelevated
                no-caps
                color="primary"
                icon="reply"
                label="Javob yozish"
                @click="openReply(m)"
              />
            </div>
          </div>
        </div>
      </div>

      <div v-if="totalPages > 1" class="row justify-center">
        <q-pagination
          v-model="page"
          :max="totalPages"
          direction-links
          boundary-numbers
          color="primary"
          @update:model-value="load"
        />
      </div>
    </div>

    <q-dialog v-model="replyOpen">
      <q-card class="reply-card">
        <q-card-section class="row items-center q-pb-none">
          <div class="text-h6">Javob yozish</div>
          <q-space />
          <q-btn flat round dense icon="close" v-close-popup />
        </q-card-section>
        <q-card-section v-if="replyTo">
          <div class="text-caption muted-text q-mb-xs">
            {{ replyTo.parentName || 'Ota-ona' }} · {{ replyTo.studentName }},
            {{ replyTo.className }}
          </div>
          <div class="bubble bubble--in q-mb-md">{{ replyTo.text }}</div>
          <q-input
            v-model="replyText"
            type="textarea"
            outlined
            autogrow
            autofocus
            counter
            maxlength="3000"
            label="Javobingiz"
            hint="Javob ota-onaga Telegram bot orqali, uning tilida sarlavha bilan yuboriladi"
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
            color="primary"
            icon="send"
            label="Yuborish"
            :loading="sending"
            :disable="!replyText.trim()"
            @click="sendReply"
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
import { ref, computed, watch } from 'vue'
import { useQuasar } from 'quasar'
import { api } from '@/boot/axios'
import PageLayout from '@/components/PageLayout.vue'
import AuthImageDialog from '@/components/AuthImageDialog.vue'
import { useSchoolStore } from '@/stores/school'
import { recipients, formatDateTime } from '@/utils/telegram'

const $q = useQuasar()
const schoolStore = useSchoolStore()

const PAGE_SIZE = 10
const filter = ref('NEW')
const items = ref([])
const loading = ref(false)
const page = ref(1)
const total = ref(0)
const newCount = ref(0)
const totalPages = computed(() =>
  Math.max(1, Math.ceil(total.value / PAGE_SIZE))
)

const replyOpen = ref(false)
const replyTo = ref(null)
const replyText = ref('')
const sending = ref(false)
const photo = ref(null)

function initial(name) {
  return (name || '?').charAt(0).toUpperCase()
}

async function load() {
  if (!schoolStore.activeSchoolId) return
  loading.value = true
  try {
    const params = {
      schoolId: schoolStore.activeSchoolId,
      page: page.value - 1,
      size: PAGE_SIZE,
      status: filter.value === 'ALL' ? undefined : filter.value
    }
    const [list, count] = await Promise.all([
      api.get('/api/parent-messages', { params }),
      api.get('/api/parent-messages/count-new', {
        params: { schoolId: schoolStore.activeSchoolId }
      })
    ])
    items.value = list.data.content
    total.value = list.data.totalElements
    newCount.value = count.data
  } catch (error) {
    $q.notify({
      type: 'negative',
      message: error.friendlyMessage || 'Xatolik yuz berdi'
    })
  } finally {
    loading.value = false
  }
}

function reload() {
  page.value = 1
  load()
}

function openReply(m) {
  replyTo.value = m
  replyText.value = ''
  replyOpen.value = true
}

async function sendReply() {
  sending.value = true
  try {
    await api.post(`/api/parent-messages/${replyTo.value.id}/reply`, {
      text: replyText.value.trim()
    })
    replyOpen.value = false
    $q.notify({
      type: 'positive',
      message: 'Javob ota-onaga yuborildi',
      icon: 'send'
    })
    load()
  } catch (error) {
    $q.notify({
      type: 'negative',
      message: error.friendlyMessage || 'Xatolik yuz berdi'
    })
  } finally {
    sending.value = false
  }
}

watch(() => schoolStore.activeSchoolId, reload, { immediate: true })
</script>

<style scoped>
.muted-text {
  color: var(--brand-text-muted);
}

.empty-state {
  text-align: center;
}

.tg-avatar {
  background: #229ed9;
}

.meta-chip {
  background: var(--surface-2);
  color: var(--text-primary);
  border: 1px solid var(--brand-border);
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

/* Chat-style bubbles: parent on the left, school's reply on the right. */
.bubble {
  white-space: pre-wrap;
  line-height: 1.45;
  padding: 10px 14px;
  border-radius: 14px;
  max-width: 720px;
  font-size: var(--text-base);
}

.bubble--in {
  background: var(--surface-2);
  border: 1px solid var(--brand-border);
  border-bottom-left-radius: 4px;
}

.bubble--out {
  margin-left: auto;
  background: rgba(79, 70, 229, 0.1);
  border: 1px solid rgba(79, 70, 229, 0.25);
  border-bottom-right-radius: 4px;
}

.bubble-meta {
  margin-top: 4px;
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
  text-align: right;
}

.reply-card {
  width: 100%;
  max-width: 560px;
  border-radius: var(--radius-lg);
}

.filter-toggle :deep(.q-btn) {
  padding: 4px 14px;
}
</style>

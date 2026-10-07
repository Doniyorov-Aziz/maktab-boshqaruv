<template>
  <page-layout
    icon="forum"
    color="#0ea5e9"
    title="Murojaatlar"
    :subtitle="`Ota-onalarning ma'muriyatga xatlari · ${schoolStore.activeSchoolName || ''}`"
  >
    <template #actions>
      <q-btn
        v-if="authStore.isEditor"
        unelevated
        no-caps
        color="primary"
        icon="add"
        label="Murojaat qo'shish"
        @click="openManual"
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

    <div class="appeals" :class="{ 'appeals--chat': selectedId }">
      <!-- ---------------------------------------------------------------- list -->
      <div class="brand-card list-pane column no-wrap">
        <div class="q-pa-sm column q-gutter-sm filters">
          <q-input
            v-model="q"
            dense
            outlined
            clearable
            debounce="350"
            placeholder="Ism yoki #raqam"
            @update:model-value="reload"
          >
            <template #prepend><q-icon name="search" /></template>
          </q-input>
          <div class="row q-col-gutter-xs">
            <div class="col-6">
              <q-select
                v-model="status"
                dense
                outlined
                emit-value
                map-options
                clearable
                label="Holat"
                :options="statusOptions"
                @update:model-value="reload"
              />
            </div>
            <div class="col-6">
              <q-select
                v-model="target"
                dense
                outlined
                emit-value
                map-options
                clearable
                label="Kimga"
                :options="targetOptions"
                @update:model-value="reload"
              />
            </div>
            <div class="col-6">
              <q-select
                v-model="classId"
                dense
                outlined
                emit-value
                map-options
                clearable
                label="Sinf"
                :options="classOptions"
                @update:model-value="reload"
              />
            </div>
            <div class="col-6">
              <date-field
                v-model="from"
                label="Sanadan"
                @update:model-value="reload"
              />
            </div>
          </div>
        </div>
        <q-separator />

        <div v-if="loading && !items.length" class="q-pa-sm column q-gutter-sm">
          <q-skeleton v-for="i in 6" :key="i" height="62px" />
        </div>
        <div
          v-else-if="!items.length"
          class="q-pa-xl column flex-center muted-text text-center col"
        >
          <q-icon name="mark_chat_read" size="48px" class="q-mb-sm" />
          <div>Murojaat topilmadi</div>
          <div class="text-caption">
            Ota-onalar botdagi «✉️ Ma'muriyatga xat» bo'limidan yozishadi.
          </div>
        </div>
        <q-list v-else separator class="col scroll">
          <q-item
            v-for="a in items"
            :key="a.id"
            clickable
            :active="a.id === selectedId"
            active-class="row-active"
            @click="select(a.id)"
          >
            <q-item-section avatar>
              <q-avatar size="40px" class="tg-avatar" text-color="white">{{
                initial(a.parentName)
              }}</q-avatar>
            </q-item-section>
            <q-item-section>
              <q-item-label class="row items-center no-wrap">
                <span class="ellipsis text-weight-semibold">{{
                  a.parentName || 'Ota-ona'
                }}</span>
                <q-space />
                <span class="text-caption muted-text q-ml-sm">{{
                  shortTime(a.lastMessageAt)
                }}</span>
              </q-item-label>
              <q-item-label caption class="ellipsis">
                <q-icon
                  :name="a.target === 'CLASS_TEACHER' ? 'school' : 'apartment'"
                  size="13px"
                />
                {{ targets[a.target]?.label }} · #{{ a.id }} ·
                {{ a.studentName }} · {{ a.className }}
              </q-item-label>
              <q-item-label caption class="row items-center no-wrap">
                <span class="ellipsis col">{{ a.lastPreview || '📎' }}</span>
                <q-badge
                  v-if="a.unread"
                  rounded
                  color="primary"
                  :label="a.unread"
                  class="q-ml-xs"
                />
                <q-badge
                  :color="statuses[a.status]?.color"
                  :label="statuses[a.status]?.label"
                  class="q-ml-xs status-badge"
                />
              </q-item-label>
            </q-item-section>
          </q-item>
        </q-list>
        <div v-if="totalPages > 1" class="row justify-center q-pa-xs">
          <q-pagination
            v-model="page"
            :max="totalPages"
            :max-pages="5"
            direction-links
            boundary-numbers
            size="sm"
            color="primary"
            @update:model-value="load"
          />
        </div>
      </div>

      <!-- ---------------------------------------------------------------- chat -->
      <div class="brand-card chat-pane column no-wrap">
        <div
          v-if="!selectedId"
          class="col column flex-center muted-text text-center q-pa-xl"
        >
          <q-icon name="forum" size="56px" class="q-mb-sm" />
          <div>Chap tomondan murojaatni tanlang</div>
        </div>
        <template v-else>
          <div class="chat-head row items-center no-wrap q-pa-sm q-gutter-x-sm">
            <q-btn
              flat
              round
              dense
              icon="arrow_back"
              class="back-btn"
              aria-label="Orqaga"
              @click="selectedId = null"
            />
            <template v-if="chat">
              <q-avatar size="40px" class="tg-avatar" text-color="white">{{
                initial(chat.appeal.parentName)
              }}</q-avatar>
              <div class="col" style="min-width: 0">
                <div class="text-weight-semibold ellipsis">
                  {{ chat.appeal.parentName || 'Ota-ona' }}
                  <span
                    v-if="chat.appeal.parentUsername"
                    class="muted-text text-caption"
                    >@{{ chat.appeal.parentUsername }}</span
                  >
                </div>
                <div class="text-caption muted-text ellipsis">
                  #{{ chat.appeal.id }} ·
                  <router-link
                    :to="`/profiles/student/${chat.appeal.studentId}`"
                    class="student-link"
                    >{{ chat.appeal.studentName }}</router-link
                  >
                  · {{ chat.appeal.className }} sinf ·
                  {{ targets[chat.appeal.target]?.label }}
                  <template v-if="chat.guardianPhone">
                    · {{ chat.guardianPhone }}</template
                  >
                  <template v-if="chat.appeal.source === 'MANUAL'">
                    · qo'lda kiritilgan</template
                  >
                </div>
              </div>
              <q-badge
                :color="statuses[chat.appeal.status]?.color"
                :label="statuses[chat.appeal.status]?.label"
                class="status-badge"
              />
              <q-btn
                v-if="chat.canReply && chat.appeal.status !== 'CLOSED'"
                flat
                dense
                no-caps
                icon="task_alt"
                label="Yopish"
                :loading="closing"
                @click="closeAppeal"
              />
            </template>
          </div>
          <q-separator />

          <div
            ref="scrollEl"
            class="col scroll chat-body q-pa-md"
            @scroll.passive="onChatScroll"
            @load.capture="keepAtBottom"
            @loadedmetadata.capture="keepAtBottom"
          >
            <div v-if="!chat" class="column q-gutter-md">
              <q-skeleton
                v-for="i in 4"
                :key="i"
                height="48px"
                :width="i % 2 ? '60%' : '45%'"
                :class="{ 'self-end': !(i % 2) }"
              />
            </div>
            <template v-else>
              <div
                v-for="g in groups"
                :key="g.key"
                class="bubble-row"
                :class="g.direction === 'OUT' ? 'bubble-row--out' : ''"
              >
                <div
                  class="bubble"
                  :class="g.direction === 'OUT' ? 'bubble--out' : 'bubble--in'"
                >
                  <!-- album: a grid of photos / videos -->
                  <div
                    v-if="g.album"
                    class="album"
                    :class="`album--${Math.min(g.items.length, 4)}`"
                  >
                    <div v-for="m in g.items" :key="m.id" class="album-cell">
                      <img
                        v-if="m.kind === 'PHOTO' && m.url"
                        :src="fullUrl(m.url)"
                        alt="Rasm"
                        @click="lightbox = fullUrl(m.url)"
                      />
                      <video
                        v-else-if="m.kind === 'VIDEO' && m.url"
                        :src="fullUrl(m.url)"
                        controls
                        preload="metadata"
                      />
                      <div v-else class="file-wait column flex-center">
                        <q-spinner v-if="m.fileState === 'PENDING'" />
                        <q-icon v-else name="broken_image" size="28px" />
                      </div>
                    </div>
                  </div>

                  <template v-for="m in g.album ? [] : g.items" :key="m.id">
                    <div v-if="m.kind !== 'TEXT'" class="media">
                      <div
                        v-if="!m.url"
                        class="file-card row items-center no-wrap q-gutter-x-sm"
                      >
                        <q-spinner
                          v-if="m.fileState === 'PENDING'"
                          size="20px"
                        />
                        <q-icon v-else name="error_outline" size="20px" />
                        <span class="text-caption">{{
                          m.fileState === 'PENDING'
                            ? 'Fayl yuklanmoqda…'
                            : "Faylni olib bo'lmadi"
                        }}</span>
                      </div>
                      <img
                        v-else-if="m.kind === 'PHOTO'"
                        :src="fullUrl(m.url)"
                        alt="Rasm"
                        class="photo"
                        @click="lightbox = fullUrl(m.url)"
                      />
                      <div
                        v-else-if="
                          m.kind === 'VIDEO' || m.kind === 'VIDEO_NOTE'
                        "
                      >
                        <video
                          :src="fullUrl(m.url)"
                          controls
                          preload="metadata"
                          class="video"
                          :class="{ 'video--round': m.kind === 'VIDEO_NOTE' }"
                        />
                        <div class="media-meta">
                          {{ duration(m.duration) }} · {{ size(m.fileSize) }} ·
                          <a :href="fullUrl(m.url, true)">Yuklab olish</a>
                        </div>
                      </div>
                      <div v-else-if="m.kind === 'VOICE' || m.kind === 'AUDIO'">
                        <div class="row items-center no-wrap q-gutter-x-xs">
                          <q-icon
                            :name="m.kind === 'VOICE' ? 'mic' : 'audiotrack'"
                            size="20px"
                          />
                          <audio
                            :src="fullUrl(m.url)"
                            controls
                            preload="metadata"
                            class="audio"
                          />
                        </div>
                        <div class="media-meta">
                          {{ m.fileName ? m.fileName + ' · ' : ''
                          }}{{ duration(m.duration) }} ·
                          <a :href="fullUrl(m.url, true)">Yuklab olish</a>
                        </div>
                      </div>
                      <a
                        v-else
                        :href="fullUrl(m.url, true)"
                        class="file-card row items-center no-wrap q-gutter-x-sm"
                      >
                        <q-icon
                          :name="fileIcon(m.mimeType)"
                          size="32px"
                          color="primary"
                        />
                        <div class="col" style="min-width: 0">
                          <div class="ellipsis text-weight-medium">
                            {{ m.fileName || 'Hujjat' }}
                          </div>
                          <div class="text-caption muted-text">
                            {{ size(m.fileSize) }}
                          </div>
                        </div>
                        <q-icon name="download" size="20px" />
                      </a>
                    </div>
                    <div v-if="m.text" class="bubble-text">{{ m.text }}</div>
                  </template>
                  <div v-if="g.album && g.caption" class="bubble-text q-mt-xs">
                    {{ g.caption }}
                  </div>
                  <div class="bubble-meta">
                    <template v-if="g.direction === 'OUT'"
                      ><q-icon name="done_all" size="12px" />
                      {{ g.sentBy || 'Maktab' }} ·
                    </template>
                    {{ formatDateTime(g.createdAt) }}
                  </div>
                </div>
              </div>
            </template>
          </div>

          <!-- reply box -->
          <div
            v-if="chat && chat.canReply && chat.appeal.status !== 'CLOSED'"
            class="reply-box q-pa-sm"
          >
            <div v-if="files.length" class="row q-gutter-xs q-mb-xs">
              <q-chip
                v-for="(f, i) in files"
                :key="i"
                removable
                dense
                icon="attach_file"
                :label="`${f.name} · ${size(f.size)}`"
                @remove="files.splice(i, 1)"
              />
            </div>
            <div class="row items-end no-wrap q-gutter-x-sm">
              <q-btn
                flat
                round
                dense
                icon="attach_file"
                aria-label="Fayl biriktirish"
                @click="fileInput.click()"
              >
                <q-tooltip
                  >Rasm, video, audio yoki hujjat (20 MB gacha)</q-tooltip
                >
              </q-btn>
              <input
                ref="fileInput"
                type="file"
                multiple
                hidden
                accept="image/jpeg,image/png,image/webp,video/mp4,video/quicktime,audio/mpeg,audio/ogg,audio/mp4,.pdf,.doc,.docx,.xls,.xlsx,.txt"
                @change="addFiles"
              />
              <q-input
                v-model="replyText"
                class="col"
                type="textarea"
                outlined
                dense
                autogrow
                maxlength="3500"
                placeholder="Javob yozing… (Ctrl+Enter — yuborish)"
                input-style="max-height: 140px"
                @keydown.ctrl.enter="sendReply"
              />
              <q-btn
                unelevated
                round
                color="primary"
                icon="send"
                aria-label="Yuborish"
                :loading="sending"
                :disable="!replyText.trim() && !files.length"
                @click="sendReply"
              />
            </div>
          </div>
          <div
            v-else-if="chat && chat.appeal.status === 'CLOSED'"
            class="q-pa-sm text-center text-caption muted-text"
          >
            Murojaat yopilgan. Ota-ona yana yozsa, u qayta ochiladi.
          </div>
        </template>
      </div>
    </div>

    <!-- lightbox -->
    <q-dialog :model-value="!!lightbox" @update:model-value="lightbox = null">
      <div class="lightbox column items-center">
        <img :src="lightbox" alt="Rasm" />
        <q-btn
          flat
          round
          icon="close"
          color="white"
          class="lightbox-close"
          aria-label="Yopish"
          v-close-popup
        />
      </div>
    </q-dialog>

    <!-- 6.2: an appeal received by phone / in person, with its real date and time -->
    <q-dialog v-model="manualOpen">
      <q-card class="manual-card">
        <q-card-section class="row items-center q-pb-none">
          <div class="text-h6">Murojaat qo'shish</div>
          <q-space />
          <q-btn flat round dense icon="close" v-close-popup />
        </q-card-section>
        <q-form @submit.prevent="saveManual">
          <q-card-section class="column q-gutter-md">
            <q-select
              v-model="manual.student"
              outlined
              use-input
              input-debounce="300"
              label="O'quvchi *"
              :options="studentOptions"
              option-label="label"
              :rules="[v => !!v || 'O\'quvchini tanlang']"
              @filter="findStudents"
            >
              <template #no-option>
                <q-item
                  ><q-item-section class="text-grey"
                    >Ism yozib qidiring</q-item-section
                  ></q-item
                >
              </template>
            </q-select>
            <q-input
              v-model="manual.parentName"
              outlined
              maxlength="120"
              label="Murojaatchi (ota-ona)"
            />
            <q-btn-toggle
              v-model="manual.target"
              no-caps
              unelevated
              spread
              toggle-color="primary"
              :options="targetOptions"
            />
            <div class="row q-col-gutter-sm">
              <div class="col-7">
                <date-field
                  v-model="manual.date"
                  label="Sana *"
                  no-future
                  :rules="[v => !!v || 'Sanani tanlang']"
                />
              </div>
              <div class="col-5">
                <q-input
                  v-model="manual.time"
                  outlined
                  label="Vaqt *"
                  mask="##:##"
                  :rules="[
                    v => /^([01]\d|2[0-3]):[0-5]\d$/.test(v || '') || 'SS:DD'
                  ]"
                >
                  <template #append>
                    <q-icon name="schedule" class="cursor-pointer">
                      <q-popup-proxy cover>
                        <q-time v-model="manual.time" format24h>
                          <div class="row justify-end">
                            <q-btn
                              v-close-popup
                              flat
                              label="OK"
                              color="primary"
                            />
                          </div>
                        </q-time>
                      </q-popup-proxy>
                    </q-icon>
                  </template>
                </q-input>
              </div>
            </div>
            <q-input
              v-model="manual.text"
              outlined
              type="textarea"
              autogrow
              counter
              maxlength="4000"
              label="Murojaat matni *"
              :rules="[v => !!(v && v.trim()) || 'Matnni yozing']"
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
              type="submit"
              color="primary"
              icon="save"
              label="Saqlash"
              :loading="savingManual"
            />
          </q-card-actions>
        </q-form>
      </q-card>
    </q-dialog>
  </page-layout>
</template>

<script setup>
import { ref, computed, watch, nextTick, onBeforeUnmount } from 'vue'
import { useQuasar } from 'quasar'
import { useRoute, useRouter } from 'vue-router'
import { api } from '@/boot/axios'
import PageLayout from '@/components/PageLayout.vue'
import DateField from '@/components/DateField.vue'
import { useSchoolStore } from '@/stores/school'
import { useAuthStore } from '@/stores/auth'
import { formatDateTime } from '@/utils/telegram'

const $q = useQuasar()
const route = useRoute()
const router = useRouter()
const schoolStore = useSchoolStore()
const authStore = useAuthStore()

const statuses = {
  NEW: { label: 'Yangi', color: 'warning' },
  SEEN: { label: "Ko'rildi", color: 'info' },
  ANSWERED: { label: 'Javob berildi', color: 'positive' },
  CLOSED: { label: 'Yopilgan', color: 'grey-7' }
}
const targets = {
  CLASS_TEACHER: { label: 'Sinf rahbariga' },
  ADMINISTRATION: { label: "Ma'muriyatga" }
}
const statusOptions = Object.entries(statuses).map(([value, s]) => ({
  value,
  label: s.label
}))
const targetOptions = Object.entries(targets).map(([value, t]) => ({
  value,
  label: t.label
}))

// ------------------------------------------------------------------- list
const PAGE_SIZE = 30
const q = ref('')
const status = ref(null)
const target = ref(null)
const classId = ref(null)
const from = ref('')
const items = ref([])
const loading = ref(false)
const page = ref(1)
const total = ref(0)
const totalPages = computed(() =>
  Math.max(1, Math.ceil(total.value / PAGE_SIZE))
)
const classOptions = ref([])

async function loadClasses() {
  const res = await api.get('/api/school-classes', {
    params: { schoolId: schoolStore.activeSchoolId, size: 300 }
  })
  classOptions.value = res.data.content.map(c => ({
    value: c.id,
    label: c.name
  }))
}

async function load({ quiet = false } = {}) {
  if (!schoolStore.activeSchoolId) return
  if (!quiet) loading.value = true
  try {
    const res = await api.get('/api/appeals', {
      background: quiet,
      params: {
        schoolId: schoolStore.activeSchoolId,
        status: status.value || undefined,
        target: target.value || undefined,
        classId: classId.value || undefined,
        from: from.value || undefined,
        q: q.value?.trim() || undefined,
        page: page.value - 1,
        size: PAGE_SIZE,
        sort: 'lastMessageAt,desc'
      }
    })
    items.value = res.data.content
    total.value = res.data.totalElements
  } catch (error) {
    if (!quiet) notifyError(error)
  } finally {
    loading.value = false
  }
}

function reload() {
  page.value = 1
  load()
}

// ------------------------------------------------------------------- chat
const selectedId = ref(route.query.id ? Number(route.query.id) : null)
const chat = ref(null)
const scrollEl = ref(null)
const lightbox = ref(null)
const closing = ref(false)

function select(id) {
  selectedId.value = id
}

async function loadChat({ quiet = false } = {}) {
  const id = selectedId.value
  if (!id) return
  if (!quiet) chat.value = null
  try {
    const res = await api.get(`/api/appeals/${id}`, { background: quiet })
    if (id !== selectedId.value) return
    const grew =
      !chat.value || res.data.messages.length !== chat.value.messages.length
    chat.value = res.data
    // opening it marks it read: reflect that in the list without a reload
    const row = items.value.find(a => a.id === id)
    if (row) Object.assign(row, res.data.appeal)
    if (grew) scrollDown()
  } catch (error) {
    if (!quiet) {
      notifyError(error)
      selectedId.value = null
    }
  }
}

// Photos and videos get their height only once loaded: while the reader is at the
// bottom, stay there so the newest message is never left half hidden.
let atBottom = true

function scrollDown() {
  atBottom = true
  nextTick(() => {
    if (scrollEl.value) scrollEl.value.scrollTop = scrollEl.value.scrollHeight
  })
}

function onChatScroll() {
  const el = scrollEl.value
  if (el) atBottom = el.scrollHeight - el.scrollTop - el.clientHeight < 40
}

function keepAtBottom() {
  if (atBottom && scrollEl.value)
    scrollEl.value.scrollTop = scrollEl.value.scrollHeight
}

/** Messages of one album (same media group, same side) share a bubble. */
const groups = computed(() => {
  const out = []
  for (const m of chat.value?.messages || []) {
    const last = out[out.length - 1]
    if (
      m.mediaGroupId &&
      last?.mediaGroupId === m.mediaGroupId &&
      last.direction === m.direction
    ) {
      last.items.push(m)
      if (m.text && !last.caption) last.caption = m.text
      continue
    }
    out.push({
      key: m.id,
      direction: m.direction,
      mediaGroupId: m.mediaGroupId,
      sentBy: m.sentBy,
      createdAt: m.createdAt,
      caption: m.mediaGroupId ? m.text : null,
      items: [m]
    })
  }
  for (const g of out) g.album = g.items.length > 1
  return out
})

async function closeAppeal() {
  closing.value = true
  try {
    await api.post(`/api/appeals/${selectedId.value}/close`)
    await loadChat({ quiet: true })
    $q.notify({ type: 'positive', message: 'Murojaat yopildi' })
  } catch (error) {
    notifyError(error)
  } finally {
    closing.value = false
  }
}

// ------------------------------------------------------------------- reply
const MAX_BYTES = 20 * 1024 * 1024
const replyText = ref('')
const files = ref([])
const fileInput = ref(null)
const sending = ref(false)

function addFiles(e) {
  for (const f of e.target.files) {
    if (f.size > MAX_BYTES) {
      $q.notify({ type: 'warning', message: `${f.name}: 20 MB dan katta` })
    } else if (files.value.length >= 10) {
      $q.notify({ type: 'warning', message: "Ko'pi bilan 10 ta fayl" })
      break
    } else {
      files.value.push(f)
    }
  }
  e.target.value = ''
}

async function sendReply() {
  if (sending.value || (!replyText.value.trim() && !files.value.length)) return
  sending.value = true
  try {
    const form = new FormData()
    if (replyText.value.trim()) form.append('text', replyText.value.trim())
    for (const f of files.value) form.append('files', f)
    const res = await api.post(`/api/appeals/${selectedId.value}/reply`, form)
    chat.value = res.data
    const row = items.value.find(a => a.id === selectedId.value)
    if (row) Object.assign(row, res.data.appeal)
    replyText.value = ''
    files.value = []
    scrollDown()
    $q.notify({
      type: 'positive',
      icon: 'send',
      message: 'Javob ota-onaga Telegram orqali yuborildi'
    })
  } catch (error) {
    notifyError(error)
  } finally {
    sending.value = false
  }
}

// ------------------------------------------------------------------- 6.2 manual appeal
const manualOpen = ref(false)
const savingManual = ref(false)
const manual = ref({})
const studentOptions = ref([])

function pad(n) {
  return String(n).padStart(2, '0')
}

function openManual() {
  const now = new Date()
  manual.value = {
    student: null,
    parentName: '',
    target: 'ADMINISTRATION',
    date: `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`,
    time: `${pad(now.getHours())}:${pad(now.getMinutes())}`,
    text: ''
  }
  manualOpen.value = true
}

async function findStudents(val, update, abort) {
  if (!val || val.length < 2) {
    update(() => {
      studentOptions.value = []
    })
    return
  }
  try {
    const res = await api.get('/api/students', {
      background: true,
      params: { schoolId: schoolStore.activeSchoolId, q: val, size: 20 }
    })
    update(() => {
      studentOptions.value = res.data.content.map(s => ({
        value: s.id,
        label: `${s.lastName} ${s.firstName} · ${s.className || ''}`,
        guardianName: s.guardianName
      }))
    })
  } catch {
    abort()
  }
}

watch(
  () => manual.value.student,
  s => {
    if (s?.guardianName && !manual.value.parentName)
      manual.value.parentName = s.guardianName
  }
)

async function saveManual() {
  const m = manual.value
  const at = new Date(`${m.date}T${m.time}:00`)
  if (at > new Date()) {
    $q.notify({
      type: 'warning',
      message: "Kelajakdagi vaqtni tanlab bo'lmaydi"
    })
    return
  }
  savingManual.value = true
  try {
    const res = await api.post('/api/appeals', {
      studentId: m.student.value,
      target: m.target,
      text: m.text.trim(),
      parentName: m.parentName?.trim() || null,
      at: `${m.date}T${m.time}:00`
    })
    manualOpen.value = false
    $q.notify({
      type: 'positive',
      message: `Murojaat #${res.data.id} saqlandi`
    })
    await load()
    selectedId.value = res.data.id
  } catch (error) {
    notifyError(error)
  } finally {
    savingManual.value = false
  }
}

// ------------------------------------------------------------------- helpers
function fullUrl(url, download = false) {
  return api.defaults.baseURL + url + (download ? '&download=true' : '')
}

function initial(name) {
  return (name || '?').charAt(0).toUpperCase()
}

function shortTime(value) {
  if (!value) return ''
  const d = new Date(value)
  const now = new Date()
  if (d.toDateString() === now.toDateString())
    return `${pad(d.getHours())}:${pad(d.getMinutes())}`
  return `${pad(d.getDate())}.${pad(d.getMonth() + 1)}`
}

function duration(sec) {
  if (sec == null) return '—'
  return `${Math.floor(sec / 60)}:${pad(sec % 60)}`
}

function size(bytes) {
  if (bytes == null) return ''
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

function fileIcon(mime) {
  if (!mime) return 'description'
  if (mime.includes('pdf')) return 'picture_as_pdf'
  if (mime.includes('sheet') || mime.includes('excel')) return 'table_chart'
  if (mime.includes('word')) return 'article'
  return 'description'
}

function notifyError(error) {
  $q.notify({
    type: 'negative',
    message: error.friendlyMessage || 'Xatolik yuz berdi'
  })
}

// ------------------------------------------------------------------- wiring
watch(selectedId, id => {
  router.replace({ query: { ...route.query, id: id || undefined } })
  replyText.value = ''
  files.value = []
  chat.value = null
  if (id) loadChat()
})

watch(
  () => schoolStore.activeSchoolId,
  id => {
    if (!id) return
    loadClasses().catch(() => {})
    reload()
    if (selectedId.value) loadChat()
  },
  { immediate: true }
)

// new parent messages show up without a manual refresh
const timer = setInterval(() => {
  if (document.hidden) return
  load({ quiet: true })
  if (selectedId.value && chat.value) loadChat({ quiet: true })
}, 30000)
onBeforeUnmount(() => clearInterval(timer))
</script>

<style scoped>
.muted-text {
  color: var(--brand-text-muted);
}

.appeals {
  display: grid;
  grid-template-columns: 360px 1fr;
  gap: 16px;
  height: calc(100vh - 190px);
  min-height: 480px;
}

.list-pane,
.chat-pane {
  min-height: 0;
  overflow: hidden;
}

.back-btn {
  display: none;
}

@media (max-width: 900px) {
  .appeals {
    grid-template-columns: 1fr;
    height: calc(100vh - 170px);
  }
  .appeals--chat .list-pane,
  .appeals:not(.appeals--chat) .chat-pane {
    display: none;
  }
  .back-btn {
    display: inline-flex;
  }
}

.row-active {
  background: rgba(79, 70, 229, 0.1);
}

.tg-avatar {
  background: #229ed9;
}

.status-badge {
  padding: 3px 7px;
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

.chat-body {
  background: var(--surface-2);
}

.bubble-row {
  display: flex;
  margin-bottom: 10px;
}

.bubble-row--out {
  justify-content: flex-end;
}

.bubble {
  max-width: min(520px, 80%);
  padding: 8px 12px;
  border-radius: 14px;
  line-height: 1.45;
  font-size: var(--text-base);
}

.bubble--in {
  background: var(--card-bg);
  color: var(--text-primary);
  border: 1px solid var(--brand-border);
  border-bottom-left-radius: 4px;
}

.bubble--out {
  background: rgba(79, 70, 229, 0.12);
  border: 1px solid rgba(79, 70, 229, 0.28);
  border-bottom-right-radius: 4px;
}

.bubble-text {
  white-space: pre-wrap;
  word-break: break-word;
}

.bubble-meta {
  margin-top: 4px;
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
  text-align: right;
}

.media + .bubble-text,
.media + .media {
  margin-top: 6px;
}

.photo {
  display: block;
  max-width: 100%;
  max-height: 320px;
  border-radius: 10px;
  cursor: zoom-in;
}

.video {
  display: block;
  max-width: 100%;
  max-height: 320px;
  border-radius: 10px;
  background: #000;
}

.video--round {
  width: 240px;
  height: 240px;
  border-radius: 50%;
  object-fit: cover;
}

.audio {
  width: 280px;
  max-width: 100%;
  height: 40px;
}

.media-meta {
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
  margin-top: 2px;
}

.media-meta a {
  color: var(--color-brand);
  font-weight: 600;
  text-decoration: none;
}

.body--dark .media-meta a {
  color: #a5b4fc;
}

.file-card {
  padding: 8px 10px;
  border-radius: 10px;
  border: 1px solid var(--brand-border);
  color: inherit;
  text-decoration: none;
  min-width: 220px;
}

.file-card:hover {
  background: rgba(79, 70, 229, 0.06);
}

.album {
  display: grid;
  gap: 3px;
  width: 360px;
  max-width: 100%;
  border-radius: 10px;
  overflow: hidden;
}

.album--2 {
  grid-template-columns: 1fr 1fr;
}

.album--3 {
  grid-template-columns: 1fr 1fr;
}

.album--3 .album-cell:first-child {
  grid-column: span 2;
}

.album--4 {
  grid-template-columns: 1fr 1fr;
}

.album-cell {
  aspect-ratio: 4 / 3;
  background: rgba(0, 0, 0, 0.06);
}

.album-cell img,
.album-cell video {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  cursor: zoom-in;
}

.file-wait {
  height: 100%;
}

.reply-box {
  border-top: 1px solid var(--brand-border);
}

.lightbox {
  position: relative;
  max-width: 92vw;
  max-height: 92vh;
}

.lightbox img {
  max-width: 92vw;
  max-height: 92vh;
  border-radius: 8px;
}

.lightbox-close {
  position: absolute;
  top: 6px;
  right: 6px;
  background: rgba(0, 0, 0, 0.45);
}

.manual-card {
  width: 100%;
  max-width: 560px;
  border-radius: var(--radius-lg);
}
</style>

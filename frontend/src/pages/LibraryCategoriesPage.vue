<template>
  <page-layout
    icon="category"
    color="#0ea5e9"
    title="Kutubxona bo'limlari"
    :subtitle="`Botdagi tartib — sudrab o'zgartiring · ${schoolStore.activeSchoolName || ''}`"
  >
    <template #actions>
      <AppButton icon="add" label="Bo'lim qo'shish" @click="openForm()" />
    </template>

    <div class="brand-card">
      <q-list separator>
        <q-item v-if="loading && !items.length">
          <q-item-section><q-skeleton type="text" /></q-item-section>
        </q-item>
        <q-item
          v-for="(c, i) in items"
          :key="c.id"
          class="cat-row"
          :class="{
            'cat-row--drag': dragIndex === i,
            'cat-row--over': overIndex === i,
            'cat-row--off': !c.active
          }"
          draggable="true"
          @dragstart="onDragStart(i, $event)"
          @dragover.prevent="overIndex = i"
          @dragleave="overIndex = null"
          @drop.prevent="onDrop(i)"
          @dragend="dragIndex = overIndex = null"
        >
          <q-item-section avatar class="cat-row__handle">
            <q-icon name="drag_indicator" color="grey-6" aria-hidden="true" />
          </q-item-section>
          <q-item-section avatar>
            <div class="cat-emoji">{{ c.emoji || '📁' }}</div>
          </q-item-section>
          <q-item-section>
            <q-item-label class="text-weight-semibold">{{
              c.nameUz
            }}</q-item-label>
            <q-item-label caption>{{ c.nameRu || '—' }}</q-item-label>
            <q-item-label caption class="lt-sm tabular-nums">
              {{ c.bookCount }} ta kitob{{ c.active ? '' : ' · yashirin' }}
            </q-item-label>
          </q-item-section>
          <!-- phone: one menu instead of four buttons -->
          <q-item-section side class="lt-sm">
            <AppButton
              variant="ghost"
              icon="more_vert"
              :aria-label="`${c.nameUz} — amallar`"
            >
              <q-menu auto-close>
                <q-list dense style="min-width: 170px">
                  <q-item
                    clickable
                    :disable="i === 0 || saving"
                    @click="move(i, -1)"
                  >
                    <q-item-section avatar
                      ><q-icon name="arrow_upward"
                    /></q-item-section>
                    <q-item-section>Yuqoriga</q-item-section>
                  </q-item>
                  <q-item
                    clickable
                    :disable="i === items.length - 1 || saving"
                    @click="move(i, 1)"
                  >
                    <q-item-section avatar
                      ><q-icon name="arrow_downward"
                    /></q-item-section>
                    <q-item-section>Pastga</q-item-section>
                  </q-item>
                  <q-item clickable @click="openForm(c)">
                    <q-item-section avatar
                      ><q-icon name="edit"
                    /></q-item-section>
                    <q-item-section>Tahrirlash</q-item-section>
                  </q-item>
                  <q-item clickable class="text-negative" @click="remove(c)">
                    <q-item-section avatar
                      ><q-icon name="delete"
                    /></q-item-section>
                    <q-item-section>O'chirish</q-item-section>
                  </q-item>
                </q-list>
              </q-menu>
            </AppButton>
          </q-item-section>
          <q-item-section side class="gt-xs">
            <div class="row items-center no-wrap q-gutter-xs">
              <q-badge
                outline
                color="primary"
                class="tabular-nums"
                :label="`${c.bookCount} ta kitob`"
              />
              <q-badge v-if="!c.active" color="grey-7" label="Yashirin" />
              <AppButton
                variant="ghost"
                icon="arrow_upward"
                :disable="i === 0 || saving"
                :aria-label="`${c.nameUz} — yuqoriga`"
                @click="move(i, -1)"
              />
              <AppButton
                variant="ghost"
                icon="arrow_downward"
                :disable="i === items.length - 1 || saving"
                :aria-label="`${c.nameUz} — pastga`"
                @click="move(i, 1)"
              />
              <AppButton
                variant="ghost"
                icon="edit"
                :aria-label="`${c.nameUz} — tahrirlash`"
                @click="openForm(c)"
              />
              <AppButton
                variant="ghost"
                color="negative"
                icon="delete"
                :aria-label="`${c.nameUz} — o'chirish`"
                @click="remove(c)"
              />
            </div>
          </q-item-section>
        </q-item>
      </q-list>
    </div>
    <div class="text-caption muted-text q-mt-sm">
      Tartib botdagi «📚 Kitob mutolaasi» sahifasida xuddi shunday ko'rinadi.
      Kitobi bor bo'lim o'chirilmaydi — yashiriladi.
    </div>

    <q-dialog v-model="formOpen">
      <q-card class="app-dialog app-dialog--md">
        <q-card-section class="row items-center q-pb-none">
          <div class="text-h6">{{
            form.id ? "Bo'limni tahrirlash" : "Yangi bo'lim"
          }}</div>
          <q-space />
          <AppButton
            variant="ghost"
            icon="close"
            aria-label="Yopish"
            v-close-popup
          />
        </q-card-section>
        <q-card-section class="q-gutter-sm">
          <div class="text-caption text-weight-semibold">Emoji</div>
          <div class="row q-gutter-xs">
            <AppButton
              variant="ghost"
              v-for="e in EMOJIS"
              :key="e"
              class="emoji-btn"
              :class="{ 'emoji-btn--on': form.emoji === e }"
              :aria-label="`Emoji ${e}`"
              :aria-pressed="form.emoji === e"
              @click="form.emoji = e"
              :label="e"
            />
          </div>
          <AppInput
            v-model="form.emoji"
            maxlength="16"
            label="Yoki o'zingiz yozing"
            style="max-width: var(--field-w-sm)"
          />
          <AppInput
            v-model="form.nameUz"
            maxlength="80"
            label="Nomi (o'zbekcha) *"
          />
          <AppInput
            v-model="form.nameRu"
            maxlength="80"
            label="Nomi (ruscha)"
          />
          <q-toggle v-model="form.active" label="Botda ko'rinadi" />
        </q-card-section>
        <q-card-actions align="right" class="q-px-md q-pb-md">
          <AppButton variant="ghost" label="Bekor qilish" v-close-popup />
          <AppButton
            icon="save"
            label="Saqlash"
            :loading="saving"
            @click="save"
          />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </page-layout>
</template>

<script setup>
import AppButton from '@/components/app/AppButton.vue'
import AppInput from '@/components/app/AppInput.vue'
import { ref, reactive, watch } from 'vue'
import { useQuasar } from 'quasar'
import { api } from '@/boot/axios'
import PageLayout from '@/components/PageLayout.vue'
import { useSchoolStore } from '@/stores/school'

const EMOJIS = [
  '🕵️',
  '📖',
  '🏛',
  '👨‍👩‍👧',
  '🧒',
  '🧠',
  '📜',
  '💼',
  '🇬🇧',
  '🇷🇺',
  '🔬',
  '🎨',
  '🌍',
  '💡',
  '❤️',
  '⚽',
  '🎵',
  '🧪'
]

const $q = useQuasar()
const schoolStore = useSchoolStore()
const items = ref([])
const loading = ref(false)
const saving = ref(false)
const formOpen = ref(false)
const form = reactive({
  id: null,
  emoji: '',
  nameUz: '',
  nameRu: '',
  active: true
})
const dragIndex = ref(null)
const overIndex = ref(null)

function notifyError(error) {
  $q.notify({
    type: 'negative',
    message: error.friendlyMessage || 'Xatolik yuz berdi'
  })
}

async function load() {
  if (!schoolStore.activeSchoolId) return
  loading.value = true
  try {
    const res = await api.get('/api/library/categories', {
      params: { schoolId: schoolStore.activeSchoolId }
    })
    items.value = res.data
  } catch (e) {
    notifyError(e)
  } finally {
    loading.value = false
  }
}

function openForm(c) {
  Object.assign(
    form,
    c
      ? {
          id: c.id,
          emoji: c.emoji || '',
          nameUz: c.nameUz,
          nameRu: c.nameRu || '',
          active: c.active
        }
      : { id: null, emoji: '📖', nameUz: '', nameRu: '', active: true }
  )
  formOpen.value = true
}

async function save() {
  if (!form.nameUz.trim()) {
    $q.notify({ type: 'warning', message: "Bo'lim nomini kiriting" })
    return
  }
  saving.value = true
  const body = {
    nameUz: form.nameUz,
    nameRu: form.nameRu || null,
    emoji: form.emoji || null,
    active: form.active
  }
  try {
    if (form.id) await api.put(`/api/library/categories/${form.id}`, body)
    else
      await api.post('/api/library/categories', body, {
        params: { schoolId: schoolStore.activeSchoolId }
      })
    formOpen.value = false
    $q.notify({ type: 'positive', message: "Bo'lim saqlandi" })
    load()
  } catch (e) {
    notifyError(e)
  } finally {
    saving.value = false
  }
}

function remove(c) {
  $q.dialog({
    title: "Bo'limni o'chirish",
    message: c.bookCount
      ? `«${c.nameUz}» da ${c.bookCount} ta kitob bor — bo'lim o'chirilmaydi, botda yashiriladi.`
      : `«${c.nameUz}» o'chirilsinmi?`,
    cancel: { flat: true, noCaps: true, label: 'Bekor qilish' },
    ok: {
      color: 'negative',
      unelevated: true,
      noCaps: true,
      label: c.bookCount ? 'Yashirish' : "O'chirish"
    }
  }).onOk(async () => {
    try {
      const res = await api.delete(`/api/library/categories/${c.id}`)
      $q.notify({
        type: 'positive',
        message: res.data.deleted ? "Bo'lim o'chirildi" : "Bo'lim yashirildi"
      })
      load()
    } catch (e) {
      notifyError(e)
    }
  })
}

function onDragStart(i, event) {
  dragIndex.value = i
  event.dataTransfer.effectAllowed = 'move'
  event.dataTransfer.setData('text/plain', String(i))
}

function onDrop(i) {
  const from = dragIndex.value
  dragIndex.value = overIndex.value = null
  if (from == null || from === i) return
  const list = [...items.value]
  const [moved] = list.splice(from, 1)
  list.splice(i, 0, moved)
  saveOrder(list)
}

function move(i, delta) {
  const list = [...items.value]
  const [moved] = list.splice(i, 1)
  list.splice(i + delta, 0, moved)
  saveOrder(list)
}

async function saveOrder(list) {
  const previous = items.value
  items.value = list
  saving.value = true
  try {
    const res = await api.put(
      '/api/library/categories/order',
      { ids: list.map(c => c.id) },
      { params: { schoolId: schoolStore.activeSchoolId } }
    )
    items.value = res.data
  } catch (e) {
    items.value = previous
    notifyError(e)
  } finally {
    saving.value = false
  }
}

watch(() => schoolStore.activeSchoolId, load, { immediate: true })
</script>

<style scoped>
.cat-row {
  cursor: grab;
  transition: background 0.12s ease;
}
.cat-row--drag {
  opacity: 0.45;
}
.cat-row--over {
  background: rgba(99, 102, 241, 0.1);
}
.cat-row--off {
  opacity: 0.6;
}
.cat-emoji {
  font-size: 26px;
  width: 40px;
  text-align: center;
}
.emoji-btn {
  font-size: 20px;
  min-width: 40px;
  border-radius: var(--radius-md);
}
.emoji-btn--on {
  background: rgba(99, 102, 241, 0.16);
  outline: 2px solid var(--q-primary);
}
.muted-text {
  color: var(--brand-text-muted);
}
@media (max-width: 599px) {
  .cat-row__handle {
    display: none;
  }
}
</style>

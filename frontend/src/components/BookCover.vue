<template>
  <div class="book-cover" :style="{ width, height }">
    <img v-if="src" :src="src" :alt="title" class="book-cover__img" />
    <div v-else class="book-cover__empty" :style="{ background: tint }">
      <q-spinner v-if="loading" size="20px" color="white" />
      <template v-else>
        <q-icon name="menu_book" size="28px" />
        <div class="book-cover__title">{{ title }}</div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onBeforeUnmount } from 'vue'
import { api } from '@/boot/axios'

// Covers are served only to a signed-in user (JWT + school check), so they are fetched as
// blobs instead of plain <img src>. Each one is kept for the session (one request per cover).
const cache = new Map()

const props = defineProps({
  bookId: { type: Number, required: true },
  hasCover: { type: Boolean, default: false },
  title: { type: String, default: '' },
  /** changes when the cover is replaced, so the cached image is not reused */
  version: { type: String, default: '' },
  width: { type: String, default: '100%' },
  height: { type: String, default: '100%' }
})

const src = ref('')
const loading = ref(false)
let alive = true

const PALETTE = [
  '#4f46e5',
  '#0ea5e9',
  '#10b981',
  '#f59e0b',
  '#ef4444',
  '#8b5cf6',
  '#ec4899',
  '#14b8a6'
]
const tint = computed(() => PALETTE[props.bookId % PALETTE.length])

async function load() {
  src.value = ''
  if (!props.hasCover) return
  const key = `${props.bookId}:${props.version}`
  if (cache.has(key)) {
    src.value = await cache.get(key)
    return
  }
  loading.value = true
  const pending = api
    .get(`/api/library/books/${props.bookId}/cover`, {
      responseType: 'blob',
      background: true
    })
    .then(res => URL.createObjectURL(res.data))
    .catch(() => '')
  cache.set(key, pending)
  const url = await pending
  if (!url) cache.delete(key)
  if (alive) {
    src.value = url
    loading.value = false
  }
}

watch(() => [props.bookId, props.hasCover, props.version], load, {
  immediate: true
})
onBeforeUnmount(() => {
  alive = false
})
</script>

<style scoped>
.book-cover {
  border-radius: 10px;
  overflow: hidden;
  flex-shrink: 0;
  background: var(--brand-surface-2, rgba(148, 163, 184, 0.12));
}
.book-cover__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.book-cover__empty {
  width: 100%;
  height: 100%;
  color: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px;
  text-align: center;
}
.book-cover__title {
  font-size: 11px;
  font-weight: 600;
  line-height: 1.2;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>

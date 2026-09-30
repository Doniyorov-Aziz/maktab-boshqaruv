<template>
  <q-dialog :model-value="!!url" @update:model-value="close">
    <q-card class="image-card">
      <q-card-section class="row items-center q-pb-sm">
        <div class="text-subtitle1 text-weight-semibold">{{ title }}</div>
        <q-space />
        <q-btn
          flat
          round
          dense
          icon="close"
          aria-label="Yopish"
          v-close-popup
        />
      </q-card-section>
      <q-card-section class="q-pt-none text-center">
        <q-spinner v-if="loading" size="36px" color="primary" class="q-my-xl" />
        <div v-else-if="failed" class="muted-text q-my-xl">
          <q-icon name="broken_image" size="40px" />
          <div>Rasmni yuklab bo'lmadi</div>
        </div>
        <img v-else :src="objectUrl" :alt="title" class="preview-img" />
      </q-card-section>
    </q-card>
  </q-dialog>
</template>

<script setup>
import { ref, watch, onBeforeUnmount } from 'vue'
import { api } from '@/boot/axios'

// Photos parents send live in Telegram; the backend streams them only with a
// valid admin token, so the image is fetched as a blob instead of an <img src>.
const props = defineProps({
  url: { type: String, default: '' },
  title: { type: String, default: 'Rasm' }
})
const emit = defineEmits(['close'])

const objectUrl = ref('')
const loading = ref(false)
const failed = ref(false)

function release() {
  if (objectUrl.value) URL.revokeObjectURL(objectUrl.value)
  objectUrl.value = ''
}

watch(
  () => props.url,
  async url => {
    release()
    if (!url) return
    loading.value = true
    failed.value = false
    try {
      const res = await api.get(url, { responseType: 'blob' })
      objectUrl.value = URL.createObjectURL(res.data)
    } catch {
      failed.value = true
    } finally {
      loading.value = false
    }
  },
  { immediate: true }
)

function close() {
  emit('close')
}

onBeforeUnmount(release)
</script>

<style scoped>
.image-card {
  width: 100%;
  max-width: 640px;
  border-radius: var(--radius-lg);
}

.preview-img {
  max-width: 100%;
  max-height: 70vh;
  border-radius: var(--radius-md);
  border: 1px solid var(--brand-border);
}

.muted-text {
  color: var(--brand-text-muted);
}
</style>

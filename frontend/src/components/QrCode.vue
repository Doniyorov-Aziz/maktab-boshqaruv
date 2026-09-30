<template>
  <div class="qr-box" :style="{ width: `${size}px`, height: `${size}px` }">
    <img v-if="src" :src="src" :width="size" :height="size" :alt="alt" />
    <q-icon v-else name="qr_code_2" :size="`${size / 2}px`" class="qr-empty" />
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import QRCode from 'qrcode'

const props = defineProps({
  value: { type: String, default: '' },
  size: { type: Number, default: 160 },
  alt: { type: String, default: 'QR kod' }
})

const src = ref('')

watch(
  () => [props.value, props.size],
  async () => {
    if (!props.value) {
      src.value = ''
      return
    }
    // Rendered at 2x for crisp printing; margin 1 keeps the quiet zone small
    // because the white box below already provides padding.
    src.value = await QRCode.toDataURL(props.value, {
      width: props.size * 2,
      margin: 1,
      errorCorrectionLevel: 'M'
    })
  },
  { immediate: true }
)
</script>

<style scoped>
/* A QR code must stay dark-on-white to scan reliably, so this box keeps a
   white background in dark mode too instead of following --card-bg. */
.qr-box {
  background: #ffffff;
  border-radius: var(--radius-sm);
  padding: 8px;
  box-sizing: content-box;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--brand-border);
}

.qr-box img {
  display: block;
  width: 100%;
  height: 100%;
}

.qr-empty {
  color: #cbd5e1;
}
</style>

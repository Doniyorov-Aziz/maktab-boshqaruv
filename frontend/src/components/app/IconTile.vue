<template>
  <div
    :class="['icon-tile', `icon-tile--${size}`]"
    :style="{ background: bg, color: fg }"
    aria-hidden="true"
  >
    <q-icon :name="icon" />
  </div>
</template>

<script setup>
// A coloured square with an icon — page headers, stat cards, list rows.
// Three sizes only: 32 / 40 / 48 px; radius --radius-sm (48: --radius-md).
//   <IconTile icon="school" size="48" color="var(--brand-gradient)" />
//   <IconTile icon="event" size="32" color="rgba(16, 185, 129, 0.12)" text-color="var(--color-success)" />
import { computed } from 'vue'

const props = defineProps({
  icon: { type: String, required: true },
  size: { type: [String, Number], default: '40', validator: v => ['32', '40', '48'].includes(String(v)) },
  // background: any CSS colour or gradient; default is the brand gradient
  color: { type: String, default: '' },
  textColor: { type: String, default: '' }
})

const bg = computed(() => props.color || 'var(--brand-gradient)')
const fg = computed(() => props.textColor || 'white')
</script>

<style scoped>
.icon-tile {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  border-radius: var(--radius-sm);
}

.icon-tile--32 {
  width: var(--tile-sm);
  height: var(--tile-sm);
  font-size: var(--text-md);
}

.icon-tile--40 {
  width: var(--tile-md);
  height: var(--tile-md);
  font-size: var(--text-lg);
}

.icon-tile--48 {
  width: var(--tile-lg);
  height: var(--tile-lg);
  border-radius: var(--radius-md);
  font-size: var(--text-xl);
}
</style>

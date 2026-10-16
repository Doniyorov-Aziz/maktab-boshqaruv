<template>
  <span class="risk-avatar" :style="{ background: bg }" aria-hidden="true">{{
    initials
  }}</span>
</template>

<script setup>
// A person's initials on a colour of their own: the same person always gets the same
// one of five quiet tones, picked from the id (not at random).
import { computed } from 'vue'

const props = defineProps({
  name: { type: String, required: true },
  id: { type: Number, default: 0 }
})

const initials = computed(() =>
  props.name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map(p => p[0].toUpperCase())
    .join('')
)
const bg = computed(() => `var(--avatar-${(Math.abs(props.id) % 5) + 1})`)
</script>

<style scoped>
.risk-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: var(--avatar-sm);
  height: var(--avatar-sm);
  border-radius: var(--radius-pill);
  color: white;
  font-size: var(--text-xs);
  font-weight: 700;
  letter-spacing: 0.02em;
}
</style>

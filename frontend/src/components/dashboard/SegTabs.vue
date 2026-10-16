<template>
  <div class="seg" role="tablist" :aria-label="label">
    <button
      v-for="t in options"
      :key="t.value"
      type="button"
      role="tab"
      :aria-selected="t.value === modelValue"
      :class="['seg__btn', { 'seg__btn--on': t.value === modelValue }]"
      @click="emit('update:modelValue', t.value)"
    >
      {{ t.label }}
    </button>
  </div>
</template>

<script setup>
// Quiet segmented tabs (the active one lifted) — widget tabs and the page's period.
defineProps({
  modelValue: { type: String, default: null },
  options: { type: Array, required: true },
  label: { type: String, default: '' }
})
const emit = defineEmits(['update:modelValue'])
</script>

<style scoped>
.seg {
  display: inline-flex;
  padding: var(--hairline);
  gap: var(--hairline);
  border-radius: var(--radius-sm);
  background: var(--surface-2);
  border: 1px solid var(--dash-edge);
}

.seg__btn {
  border: 0;
  background: transparent;
  color: var(--brand-text-muted);
  font: inherit;
  font-size: var(--text-xs);
  font-weight: 600;
  padding: var(--space-1) var(--space-3);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition:
    background var(--dur-fast) var(--ease-out),
    color var(--dur-fast) var(--ease-out);
}

.seg__btn:hover {
  color: var(--text-primary);
}

.seg__btn--on {
  background: var(--card-bg);
  color: var(--text-primary);
  box-shadow: var(--dash-shadow);
}

.seg__btn:focus-visible {
  outline: var(--border-focus);
  outline-offset: var(--hairline);
}
</style>

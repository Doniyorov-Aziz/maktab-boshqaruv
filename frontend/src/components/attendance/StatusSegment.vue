<template>
  <div
    class="seg4"
    role="radiogroup"
    :aria-label="`${name}: davomat`"
    data-status-segment
  >
    <button
      v-for="s in STATUSES"
      :key="s.value"
      type="button"
      role="radio"
      class="seg4__btn"
      :class="{ 'seg4__btn--on': modelValue === s.value }"
      :style="{ '--c': s.color }"
      :aria-checked="modelValue === s.value"
      :aria-label="s.label"
      :title="`${s.label} (${s.key})`"
      :disabled="readonly"
      :data-status="s.value"
      tabindex="-1"
      @click.stop="emit('pick', s.value, $event.currentTarget)"
    >
      <q-icon :name="s.icon" class="seg4__icon" />
      <span class="seg4__label">{{ s.label }}</span>
    </button>
  </div>
</template>

<script setup>
// The four statuses as four separate buttons with a gap between them: the chosen one filled in
// its own colour with white text, the others see-through with muted text and a light tint on
// hover. Under 1024 px only the icons (the name is in the tooltip).
import { STATUSES } from './attendance'

defineProps({
  modelValue: { type: String, default: null },
  name: { type: String, default: '' },
  readonly: { type: Boolean, default: false }
})
const emit = defineEmits(['pick'])
</script>

<style scoped>
.seg4 {
  display: inline-flex;
  gap: var(--space-1);
  flex-shrink: 0;
}

.seg4__btn {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  height: var(--control-h-sm);
  padding: 0 var(--space-3);
  border: 1px solid color-mix(in srgb, var(--c) 30%, var(--dash-edge));
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--brand-text-muted);
  font: inherit;
  font-size: var(--text-xs);
  font-weight: 600;
  cursor: pointer;
  transition:
    background var(--dur-fast) var(--ease-out),
    color var(--dur-fast) var(--ease-out),
    border-color var(--dur-fast) var(--ease-out);
}

.seg4__btn:hover:not(:disabled) {
  background: color-mix(in srgb, var(--c) 12%, transparent);
  color: var(--text-primary);
}

.seg4__btn--on,
.seg4__btn--on:hover:not(:disabled) {
  background: var(--c);
  border-color: var(--c);
  color: white;
}

.seg4__btn:disabled {
  cursor: default;
}

.seg4__btn:disabled:not(.seg4__btn--on) {
  opacity: 0.45;
}

.seg4__icon {
  font-size: var(--text-md);
}

@media (max-width: 1023px) {
  .seg4__label {
    display: none;
  }

  .seg4__btn {
    width: var(--control-h-sm);
    justify-content: center;
    padding: 0;
  }
}

@media (prefers-reduced-motion: reduce) {
  .seg4__btn {
    transition: none;
  }
}
</style>

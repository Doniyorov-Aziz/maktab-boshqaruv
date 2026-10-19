<template>
  <div class="empty" :class="`empty--${kind}`" :data-empty-state="kind">
    <span class="empty__icon" aria-hidden="true">
      <q-icon :name="kind === 'ok' ? 'check_circle' : icon" />
    </span>
    <div class="empty__title">{{ title }}</div>
    <div v-if="text" class="empty__text">{{ text }}</div>
    <AppButton
      v-if="action"
      class="empty__action"
      variant="secondary"
      size="sm"
      :icon="actionIcon"
      :label="action"
      @click="emit('action')"
    />
    <slot />
  </div>
</template>

<script setup>
// "Nothing here" said kindly: a neutral icon, one line, and the next step as a button.
// The green check is only for kind="ok" — nothing to show because nothing is wrong.
import AppButton from '@/components/app/AppButton.vue'

defineProps({
  title: { type: String, required: true },
  text: { type: String, default: '' },
  icon: { type: String, default: 'inbox' },
  // 'nodata' (neutral) or 'ok' (no problem)
  kind: { type: String, default: 'nodata' },
  action: { type: String, default: '' },
  actionIcon: { type: String, default: 'add' }
})
const emit = defineEmits(['action'])
</script>

<style scoped>
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  padding: var(--space-7) var(--space-4);
  text-align: center;
  color: var(--brand-text-muted);
}

.empty__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: var(--tile-lg);
  height: var(--tile-lg);
  border-radius: var(--radius-pill);
  background: var(--surface-2);
  font-size: var(--text-xl);
}

.empty--ok .empty__icon {
  color: var(--st-present);
  background: color-mix(in srgb, var(--st-present) 12%, transparent);
}

.empty__title {
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--text-primary);
}

.empty__text {
  font-size: var(--text-xs);
  max-width: var(--field-w-lg);
}

.empty__action {
  margin-top: var(--space-2);
}
</style>

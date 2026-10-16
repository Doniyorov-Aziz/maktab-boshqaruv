<template>
  <q-dialog
    :model-value="modelValue"
    position="right"
    full-height
    @update:model-value="v => emit('update:modelValue', v)"
  >
    <section
      class="side-panel"
      role="dialog"
      :aria-label="title"
      data-side-panel
    >
      <header class="side-panel__head">
        <IconTile
          v-if="icon"
          :icon="icon"
          size="32"
          :color="`color-mix(in srgb, ${color} 14%, transparent)`"
          :text-color="color"
        />
        <div class="side-panel__titles">
          <h3>{{ title }}</h3>
          <small v-if="subtitle">{{ subtitle }}</small>
        </div>
        <AppButton
          v-close-popup
          variant="ghost"
          size="sm"
          icon="close"
          aria-label="Yopish"
        />
      </header>
      <div class="side-panel__body">
        <slot />
      </div>
    </section>
  </q-dialog>
</template>

<script setup>
// A panel that slides in from the right with a full list and its actions (the home
// page's signals, "Hammasi"). Esc or the cross closes it.
import AppButton from '@/components/app/AppButton.vue'
import IconTile from '@/components/app/IconTile.vue'

defineProps({
  modelValue: { type: Boolean, default: false },
  title: { type: String, required: true },
  subtitle: { type: String, default: '' },
  icon: { type: String, default: '' },
  color: { type: String, default: 'var(--chart-primary)' }
})
const emit = defineEmits(['update:modelValue'])
</script>

<style scoped>
.side-panel {
  width: min(var(--side-panel-w), 100vw);
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--card-bg);
  color: var(--text-primary);
  border-left: 1px solid var(--dash-edge);
  box-shadow: var(--dash-shadow-hover);
}

.side-panel__head {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-4) var(--space-5);
  border-bottom: 1px solid var(--dash-edge);
}

.side-panel__titles {
  flex: 1 1 auto;
  min-width: 0;
}

.side-panel__titles h3 {
  margin: 0;
  font-size: var(--text-md);
  font-weight: 600;
  line-height: 1.3;
}

.side-panel__titles small {
  color: var(--brand-text-muted);
  font-size: var(--text-xs);
}

.side-panel__body {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  padding: var(--space-3) var(--space-5) var(--space-5);
}
</style>

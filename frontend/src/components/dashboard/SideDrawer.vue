<template>
  <q-dialog
    :model-value="modelValue"
    position="right"
    full-height
    :persistent="persistent"
    :maximized="$q.screen.lt.sm"
    transition-show="slide-left"
    transition-hide="slide-right"
    @update:model-value="v => emit('update:modelValue', v)"
    @show="onShow"
  >
    <!-- a <div>: Quasar gives pointer events back only to the dialog's direct <div> children -->
    <div
      class="side-drawer"
      :class="{ 'side-drawer--wide': wide }"
      role="dialog"
      aria-modal="true"
      :aria-label="title"
      data-side-panel
    >
      <header class="side-drawer__head">
        <IconTile
          v-if="icon"
          :icon="icon"
          size="32"
          :color="`color-mix(in srgb, ${color} 14%, transparent)`"
          :text-color="color"
        />
        <div class="side-drawer__titles">
          <h3>{{ title }}</h3>
          <small v-if="subtitle" data-drawer-subtitle>{{ subtitle }}</small>
        </div>
        <AppButton
          v-close-popup
          variant="ghost"
          size="sm"
          icon="close"
          aria-label="Yopish"
          data-drawer-close
        />
      </header>
      <div
        v-if="$slots.toolbar"
        class="side-drawer__toolbar"
        data-drawer-toolbar
      >
        <slot name="toolbar" />
      </div>
      <div
        ref="body"
        class="side-drawer__body"
        tabindex="0"
        data-drawer-body
        :aria-label="`${title}: ro'yxat`"
      >
        <slot :scroll-el="body" />
      </div>
      <footer v-if="$slots.footer" class="side-drawer__foot">
        <slot name="footer" />
      </footer>
    </div>
  </q-dialog>
</template>

<script setup>
// The one panel that slides in from the right (full screen on a phone): a header, an
// optional sticky toolbar (filters), the list — the only part that scrolls — and an
// optional footer. The list takes the keyboard focus when the panel opens (PageDown,
// PageUp and the arrows scroll it), Esc closes the panel and the focus goes back to the
// button that opened it. Scrolling inside never moves the page behind.
//   <SideDrawer v-model="open" title="…"><template #toolbar>…</template>
//     <template #default="{ scrollEl }">…</template></SideDrawer>
import { ref } from 'vue'
import { useQuasar } from 'quasar'
import AppButton from '@/components/app/AppButton.vue'
import IconTile from '@/components/app/IconTile.vue'

defineProps({
  modelValue: { type: Boolean, default: false },
  title: { type: String, required: true },
  subtitle: { type: String, default: '' },
  icon: { type: String, default: '' },
  color: { type: String, default: 'var(--chart-primary)' },
  // a form: neither a click outside nor Esc closes it, only the cross or its buttons
  persistent: { type: Boolean, default: false },
  // a form with two columns: 560 px instead of 440
  wide: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue'])
const $q = useQuasar()
const body = ref(null)

function onShow() {
  body.value?.focus({ preventScroll: true })
}

defineExpose({ body })
</script>

<style scoped>
.side-drawer {
  /* a direct div of the dialog gets pointer events back; say it once more for safety */
  pointer-events: all;
  width: min(var(--side-panel-w), 100vw);
  height: 100vh;
  height: 100dvh;
  max-height: 100dvh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--card-bg);
  color: var(--text-primary);
  border-left: 1px solid var(--dash-edge);
  box-shadow: var(--dash-shadow-hover);
}

.side-drawer--wide {
  width: min(var(--side-panel-w-wide), 100vw);
}

.side-drawer__head {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-4) var(--space-5);
  border-bottom: 1px solid var(--dash-edge);
}

.side-drawer__titles {
  flex: 1 1 auto;
  min-width: 0;
}

.side-drawer__titles h3 {
  margin: 0;
  font-size: var(--text-md);
  font-weight: 600;
  line-height: 1.3;
}

.side-drawer__titles small {
  color: var(--brand-text-muted);
  font-size: var(--text-xs);
}

.side-drawer__toolbar {
  flex: 0 0 auto;
  padding: var(--space-3) var(--space-5);
  border-bottom: 1px solid var(--dash-edge);
  background: var(--card-bg);
}

.side-drawer__body {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
  -webkit-overflow-scrolling: touch;
  touch-action: pan-y;
  padding: var(--space-2) var(--space-5) var(--space-5);
  scrollbar-width: thin;
  scrollbar-color: var(--scrollbar-thumb) transparent;
}

.side-drawer__body:focus {
  outline: none;
}

.side-drawer__body:focus-visible {
  box-shadow: inset 0 0 0 var(--hairline) var(--c-primary);
}

.side-drawer__body::-webkit-scrollbar {
  width: var(--scrollbar-w);
}

.side-drawer__body::-webkit-scrollbar-thumb {
  background: var(--scrollbar-thumb);
  border-radius: var(--radius-pill);
}

.side-drawer__body::-webkit-scrollbar-thumb:hover {
  background: var(--scrollbar-thumb-hover);
}

.side-drawer__foot {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  padding: var(--space-3) var(--space-5);
  border-top: 1px solid var(--dash-edge);
  background: var(--card-bg);
}

@media (max-width: 599px) {
  .side-drawer {
    width: 100vw;
    border-left: 0;
  }
}
</style>

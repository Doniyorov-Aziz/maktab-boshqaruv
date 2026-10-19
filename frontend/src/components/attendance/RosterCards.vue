<template>
  <div class="rc" data-roster-cards>
    <button
      v-for="r in rows"
      :key="r.studentId"
      type="button"
      class="rc__card"
      :class="
        r.status ? `rc__card--${r.status.toLowerCase()}` : 'rc__card--none'
      "
      :style="{
        '--c': r.status ? STATUS[r.status].color : 'var(--st-unmarked)'
      }"
      :disabled="readonly"
      :aria-label="`${r.name}: ${r.status ? STATUS[r.status].label : 'belgilanmagan'}`"
      :data-card="r.studentId"
      :data-row-status="r.status || 'NONE'"
      @click="onClick(r, $event)"
      @contextmenu.prevent="openMenu(r, $event)"
      @pointerdown="startPress(r, $event)"
      @pointerup="cancelPress"
      @pointerleave="cancelPress"
      @pointercancel="cancelPress"
    >
      <span class="rc__top">
        <AvatarInitials :name="r.name" :id="r.studentId" size="sm" />
        <span v-if="isAuto(r)" class="rc__auto" title="Avtomatik">🤖</span>
      </span>
      <span class="rc__name">{{ r.name }}</span>
      <span class="rc__status">
        <q-icon :name="r.status ? STATUS[r.status].icon : 'help_outline'" />
        {{ r.status ? STATUS[r.status].label : 'Belgilanmagan' }}
        <template v-if="r.status === 'LATE' && r.lateMinutes">
          · {{ r.lateMinutes }} daq</template
        >
      </span>
    </button>

    <q-menu
      v-model="menu.open"
      :target="menu.el || false"
      no-parent-event
      anchor="bottom middle"
      self="top middle"
    >
      <q-list dense class="rc__menu" data-card-menu>
        <q-item
          v-for="s in STATUSES"
          :key="s.value"
          v-close-popup
          clickable
          :data-menu-status="s.value"
          @click="emit('pick', menu.row, s.value, menu.el)"
        >
          <q-item-section avatar
            ><q-icon :name="s.icon" :style="{ color: s.color }"
          /></q-item-section>
          <q-item-section>{{ s.label }}</q-item-section>
          <q-item-section side class="rc__key">{{ s.key }}</q-item-section>
        </q-item>
      </q-list>
    </q-menu>
  </div>
</template>

<script setup>
// The cards view (phones open on it): a card per student coloured by status; a tap moves it
// Keldi → Kelmadi → Kechikdi → Sababli, a long press or right click opens all four.
import { reactive } from 'vue'
import AvatarInitials from '@/components/pages/AvatarInitials.vue'
import { STATUS, STATUSES, nextStatus } from './attendance'

defineProps({
  rows: { type: Array, required: true },
  readonly: { type: Boolean, default: false }
})
const emit = defineEmits(['pick'])

const LONG_PRESS_MS = 500
const menu = reactive({ open: false, el: null, row: null })
let timer = null
let pressed = false

const isAuto = r => r.source === 'AUTO_CARRY' || r.source === 'DEMO_SIM'

function onClick(r, e) {
  // a long press already opened the menu: the click that follows it does nothing
  if (pressed) {
    pressed = false
    return
  }
  emit('pick', r, nextStatus(r.status), e.currentTarget)
}

function openMenu(r, e) {
  menu.row = r
  menu.el = e.currentTarget
  menu.open = true
}

function startPress(r, e) {
  if (e.pointerType === 'mouse') return
  const el = e.currentTarget
  cancelPress()
  timer = setTimeout(() => {
    pressed = true
    openMenu(r, { currentTarget: el })
  }, LONG_PRESS_MS)
}

function cancelPress() {
  clearTimeout(timer)
  timer = null
}
</script>

<style scoped>
.rc {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(var(--att-card-w), 1fr));
  gap: var(--space-3);
  padding: var(--space-4);
}

.rc__card {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: var(--space-1);
  min-height: var(--att-card-h);
  padding: var(--space-3);
  border: 1px solid color-mix(in srgb, var(--c) 35%, var(--dash-edge));
  border-top: var(--row-stripe) solid var(--c);
  border-radius: var(--radius-md);
  background: color-mix(in srgb, var(--c) 8%, var(--card-bg));
  color: var(--text-primary);
  font: inherit;
  text-align: left;
  cursor: pointer;
  user-select: none;
  -webkit-touch-callout: none;
  transition:
    background var(--dur-fast) var(--ease-out),
    transform var(--dur-fast) var(--ease-out);
}

.rc__card:hover:not(:disabled) {
  background: color-mix(in srgb, var(--c) 14%, var(--card-bg));
}

.rc__card:active:not(:disabled) {
  transform: scale(0.98);
}

.rc__card:focus-visible {
  outline: var(--border-focus);
  outline-offset: var(--hairline);
}

.rc__card:disabled {
  cursor: default;
}

.rc__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.rc__auto {
  font-size: var(--text-xs);
}

.rc__name {
  font-size: var(--text-sm);
  font-weight: 600;
  line-height: 1.25;
}

.rc__status {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  margin-top: auto;
  font-size: var(--text-xs);
  font-weight: 700;
  color: var(--c);
}

.rc__menu {
  min-width: var(--field-w-sm);
}

.rc__key {
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
}

@media (max-width: 599px) {
  .rc {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: var(--space-2);
    padding: var(--space-3);
  }
}

@media (prefers-reduced-motion: reduce) {
  .rc__card {
    transition: none;
  }
}
</style>

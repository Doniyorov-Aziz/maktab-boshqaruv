<template>
  <!-- the demo visitor: a button that changes data stays visible but is off; the wrapper
       carries the tooltip (a disabled <button> gets no hover events) -->
  <span
    v-if="guestOff"
    class="app-btn-guest"
    :class="{ 'app-btn-guest--block': block }"
    data-guest-disabled
  >
    <q-btn
      v-bind="$attrs"
      :class="[
        'app-btn',
        `app-btn--${variant}`,
        `app-btn--${size}`,
        { 'app-btn--icon': iconOnly, 'app-btn--block': block }
      ]"
      :data-size="size"
      no-caps
      :unelevated="variant === 'primary' || variant === 'danger'"
      :outline="variant === 'secondary'"
      :flat="variant === 'ghost'"
      :color="color || COLORS[variant]"
      :text-color="textColor"
      :icon="icon || undefined"
      :icon-right="iconRight || undefined"
      :label="label || undefined"
      disable
      :to="undefined"
    >
      <slot />
    </q-btn>
    <q-tooltip>{{ GUEST_READ_ONLY }}</q-tooltip>
  </span>
  <q-btn
    v-else
    v-bind="$attrs"
    :class="[
      'app-btn',
      `app-btn--${variant}`,
      `app-btn--${size}`,
      { 'app-btn--icon': iconOnly, 'app-btn--block': block }
    ]"
    :data-size="size"
    no-caps
    :unelevated="variant === 'primary' || variant === 'danger'"
    :outline="variant === 'secondary'"
    :flat="variant === 'ghost'"
    :color="color || COLORS[variant]"
    :text-color="textColor"
    :icon="icon || undefined"
    :icon-right="iconRight || undefined"
    :label="label || undefined"
  >
    <slot />
  </q-btn>
</template>

<script setup>
// The one button of the app: height --control-h (sm: --control-h-sm), radius
// --radius-md (sm: --radius-sm), no caps, weight 600, 8px between icon and text.
//   <AppButton label="Saqlash" icon="save" />                 primary
//   <AppButton variant="secondary" label="Bekor qilish" />     outlined
//   <AppButton variant="ghost" icon="edit" size="sm" />       icon-only, square
//   <AppButton variant="danger" label="O'chirish" />
// Everything else (to, type, loading, disable, @click, q-menu/q-tooltip in the
// default slot) goes straight to q-btn.
// For the demo visitor (role GUEST) a button that changes data (by its icon / label, see
// utils/guest.js; guest="block" / guest="allow" decides by hand) is shown disabled.
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { GUEST_READ_ONLY, isWriteAction } from '@/utils/guest'

defineOptions({ inheritAttrs: false })

const props = defineProps({
  variant: {
    type: String,
    default: 'primary',
    validator: v => ['primary', 'secondary', 'ghost', 'danger'].includes(v)
  },
  size: {
    type: String,
    default: 'md',
    validator: v => ['md', 'sm'].includes(v)
  },
  label: { type: [String, Number], default: '' },
  icon: { type: String, default: '' },
  iconRight: { type: String, default: '' },
  // a colour other than the variant's own (e.g. "positive"); the shape stays the same
  color: { type: String, default: '' },
  textColor: { type: String, default: undefined },
  block: { type: Boolean, default: false },
  // square like an icon button, for content in the slot (an avatar)
  square: { type: Boolean, default: false },
  // the demo visitor: 'auto' (by icon / label), 'block' (always off) or 'allow' (never off)
  guest: {
    type: String,
    default: 'auto',
    validator: v => ['auto', 'block', 'allow'].includes(v)
  }
})

const COLORS = {
  primary: 'primary',
  secondary: 'primary',
  ghost: undefined,
  danger: 'negative'
}

const auth = useAuthStore()

const guestOff = computed(
  () =>
    auth.isGuest &&
    props.guest !== 'allow' &&
    (props.guest === 'block' || isWriteAction(props.icon, props.label))
)

// an icon without a label is a square button
const iconOnly = computed(
  () =>
    props.square ||
    (!props.label && props.label !== 0 && !!props.icon && !props.iconRight)
)
</script>

<style scoped>
.app-btn-guest {
  display: inline-flex;
  cursor: not-allowed;
}

.app-btn-guest--block {
  display: flex;
  width: 100%;
}
</style>

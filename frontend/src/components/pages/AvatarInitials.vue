<template>
  <span
    class="avatar"
    :class="[`avatar--${size}`, { 'avatar--square': square }]"
    :style="{ background: bg }"
    :title="title || undefined"
    aria-hidden="true"
    >{{ text || letters }}</span
  >
</template>

<script setup>
// A person's (or a class's) initials on a colour of their own: the same id always gets
// the same one of five quiet tones. gradient: a class avatar ("5-A") on a soft gradient.
import { computed } from 'vue'

const props = defineProps({
  name: { type: String, default: '' },
  id: { type: Number, default: 0 },
  // xs 20 (lesson cards) / sm 32 / md 40 / lg 48 / xl 56 (class cards)
  size: { type: String, default: 'sm' },
  // shown instead of the initials ("5-A")
  text: { type: String, default: '' },
  gradient: { type: Boolean, default: false },
  square: { type: Boolean, default: false },
  title: { type: String, default: '' }
})

const letters = computed(() =>
  props.name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map(p => p[0].toUpperCase())
    .join('')
)
const tone = computed(() => `var(--avatar-${(Math.abs(props.id) % 5) + 1})`)
const bg = computed(() =>
  props.gradient
    ? `linear-gradient(135deg, ${tone.value}, color-mix(in srgb, ${tone.value} 55%, var(--chart-secondary)))`
    : tone.value
)
</script>

<style scoped>
.avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  border-radius: var(--radius-pill);
  color: white;
  font-weight: 700;
  letter-spacing: 0.02em;
  font-variant-numeric: tabular-nums;
}

.avatar--square {
  border-radius: var(--radius-md);
}

.avatar--xs {
  width: var(--avatar-xs);
  height: var(--avatar-xs);
  font-size: calc(var(--text-xs) * 0.85);
}

.avatar--sm {
  width: var(--avatar-sm);
  height: var(--avatar-sm);
  font-size: var(--text-xs);
}

.avatar--md {
  width: var(--avatar-md);
  height: var(--avatar-md);
  font-size: var(--text-sm);
}

.avatar--lg {
  width: var(--avatar-lg);
  height: var(--avatar-lg);
  font-size: var(--text-md);
}

.avatar--xl {
  width: var(--class-avatar);
  height: var(--class-avatar);
  font-size: var(--text-lg);
}
</style>

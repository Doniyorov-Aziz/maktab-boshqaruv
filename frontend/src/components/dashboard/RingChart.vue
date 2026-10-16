<template>
  <figure class="ring" :class="{ 'ring--sm': small }">
    <svg viewBox="0 0 120 120" role="img" :aria-label="ariaLabel">
      <circle cx="60" cy="60" :r="R" class="ring__track" />
      <circle
        v-for="s in arcs"
        :key="s.key"
        cx="60"
        cy="60"
        :r="R"
        class="ring__arc"
        :style="{ stroke: s.color }"
        :stroke-dasharray="`${s.len} ${C - s.len}`"
        :stroke-dashoffset="-s.offset"
        transform="rotate(-90 60 60)"
      >
        <title>{{ s.label }}: {{ s.value }}</title>
      </circle>
    </svg>
    <figcaption class="ring__center">
      <div class="ring__value">{{ center }}</div>
      <div v-if="caption" class="ring__caption">{{ caption }}</div>
    </figcaption>
  </figure>
</template>

<script setup>
// A ring (donut) of parts — e.g. present / late / absent / excused / unmarked — with
// the main number in the middle. SVG, no chart library; a part of 0 is not drawn.
import { computed } from 'vue'

const props = defineProps({
  // [{ key, label, value, color }]
  parts: { type: Array, default: () => [] },
  center: { type: String, default: '' },
  caption: { type: String, default: '' },
  small: { type: Boolean, default: false }
})

const R = 48
const C = 2 * Math.PI * R
const GAP = 1.5

const total = computed(() =>
  props.parts.reduce((a, p) => a + (p.value || 0), 0)
)
const arcs = computed(() => {
  if (!total.value) return []
  let offset = 0
  return props.parts
    .filter(p => p.value > 0)
    .map(p => {
      const full = (p.value / total.value) * C
      const len = Math.max(full - GAP, 0.5)
      const arc = { ...p, len, offset }
      offset += full
      return arc
    })
})
const ariaLabel = computed(() =>
  props.parts.map(p => `${p.label} ${p.value}`).join(', ')
)
</script>

<style scoped>
.ring {
  position: relative;
  margin: 0;
  width: var(--ring-size);
  height: var(--ring-size);
  flex-shrink: 0;
}

.ring--sm {
  width: var(--ring-size-sm);
  height: var(--ring-size-sm);
}

.ring svg {
  width: 100%;
  height: 100%;
}

.ring__track {
  fill: none;
  stroke: var(--surface-2);
  stroke-width: 12;
}

.ring__arc {
  fill: none;
  stroke-width: 12;
  transition: stroke-dasharray var(--dur) var(--ease-out);
}

.ring__center {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.ring__value {
  font-size: var(--text-xl);
  font-weight: 700;
  line-height: 1.1;
}

.ring--sm .ring__value {
  font-size: var(--text-md);
}

.ring__caption {
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
}

@media (prefers-reduced-motion: reduce) {
  .ring__arc {
    transition: none;
  }
}
</style>

<template>
  <figure
    class="ring"
    :class="{ 'ring--sm': small, 'ring--mid': mid }"
    data-ring="parts"
  >
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
        :stroke-dasharray="`${grown ? s.len : 0} ${C}`"
        :stroke-dashoffset="-(grown ? s.offset : 0)"
        transform="rotate(-90 60 60)"
      >
        <title>{{ s.label }}: {{ s.value }}</title>
      </circle>
    </svg>
    <figcaption class="ring__center">
      <!-- a page may put its own centre (e.g. a counting-up number) -->
      <slot>
        <div class="ring__value">{{ center }}</div>
        <div v-if="caption" class="ring__caption">{{ caption }}</div>
      </slot>
    </figcaption>
  </figure>
</template>

<script setup>
// A ring (donut) of parts — e.g. present / late / absent / excused / unmarked — with
// the main number in the middle. SVG, no chart library; a part of 0 is not drawn. The
// parts grow in over 800 ms when the ring appears (at once with prefers-reduced-motion).
import { computed, ref, onMounted } from 'vue'
import { reducedMotion } from '@/composables/useCountUp'

const props = defineProps({
  // [{ key, label, value, color }]
  parts: { type: Array, default: () => [] },
  center: { type: String, default: '' },
  caption: { type: String, default: '' },
  small: { type: Boolean, default: false },
  mid: { type: Boolean, default: false }
})

const grown = ref(reducedMotion())
onMounted(() => {
  if (!grown.value)
    requestAnimationFrame(() =>
      requestAnimationFrame(() => (grown.value = true))
    )
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

.ring--mid {
  width: var(--ring-mid);
  height: var(--ring-mid);
}

.ring svg {
  width: 100%;
  height: 100%;
}

.ring__track {
  fill: none;
  stroke: var(--ring-track);
  stroke-width: 10;
}

.ring__arc {
  fill: none;
  stroke-width: 10;
  transition:
    stroke-dasharray var(--dur-ring) var(--ease-out),
    stroke-dashoffset var(--dur-ring) var(--ease-out);
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

.ring--sm .ring__value,
.ring--mid .ring__value {
  font-size: var(--text-md);
  letter-spacing: -0.02em;
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

.ring__value {
  font-variant-numeric: tabular-nums;
}
</style>

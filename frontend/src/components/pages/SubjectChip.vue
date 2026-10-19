<template>
  <component
    :is="clickable ? 'button' : 'span'"
    :type="clickable ? 'button' : undefined"
    class="subj"
    :class="{ 'subj--on': active, 'subj--dim': dim, 'subj--click': clickable }"
    :style="{ '--c': color }"
    :aria-pressed="clickable ? active : undefined"
    :data-subject="subjectId"
    @click="clickable && emit('toggle', subjectId)"
  >
    <span class="subj__dot" />
    <span class="subj__name">{{ name }}</span>
    <b v-if="count !== null" class="subj__count">{{ count }}</b>
  </component>
</template>

<script setup>
// A subject as a small pill in its own colour (legend, lists). Clickable in the timetable
// legend: the chosen subject stays bright, the others dim.
import { computed } from 'vue'
import { subjectColor } from '@/utils/subjectColor'

const props = defineProps({
  subjectId: { type: Number, required: true },
  name: { type: String, required: true },
  chosenColor: { type: String, default: null },
  count: { type: Number, default: null },
  active: { type: Boolean, default: false },
  dim: { type: Boolean, default: false },
  clickable: { type: Boolean, default: false }
})
const emit = defineEmits(['toggle'])
const color = computed(() => subjectColor(props.subjectId, props.chosenColor))
</script>

<style scoped>
.subj {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  min-height: var(--legend-chip-h);
  padding: 0 var(--space-3) 0 var(--space-2);
  border-radius: var(--radius-pill);
  border: 1px solid color-mix(in srgb, var(--c) 30%, transparent);
  background: color-mix(in srgb, var(--c) 8%, transparent);
  color: var(--text-primary);
  font: inherit;
  font-size: var(--text-xs);
  font-weight: 600;
  white-space: nowrap;
  transition:
    opacity var(--dur-fast) var(--ease-out),
    background var(--dur-fast) var(--ease-out);
}

.subj--click {
  cursor: pointer;
}

.subj--click:hover,
.subj--on {
  background: color-mix(in srgb, var(--c) 20%, transparent);
  border-color: var(--c);
}

.subj--dim {
  opacity: 0.4;
}

.subj:focus-visible {
  outline: var(--border-focus);
  outline-offset: var(--hairline);
}

.subj__dot {
  width: var(--legend-dot);
  height: var(--legend-dot);
  border-radius: var(--radius-pill);
  background: var(--c);
}

.subj__count {
  color: var(--brand-text-muted);
  font-variant-numeric: tabular-nums;
}
</style>

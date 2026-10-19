<template>
  <span
    class="heat"
    :class="{ 'heat--zero': !value }"
    :style="{ '--a': `${share}%` }"
    :title="title"
    :data-heat="value"
    >{{ value || '' }}</span
  >
</template>

<script setup>
// One cell of a heat map: the brand colour, stronger for a bigger value (0 is an empty
// track). The number is in the cell, the full meaning in its title.
import { computed } from 'vue'

const props = defineProps({
  value: { type: Number, default: 0 },
  max: { type: Number, default: 1 },
  title: { type: String, default: '' }
})
const share = computed(() =>
  !props.value ? 0 : Math.round(12 + (props.value / Math.max(props.max, 1)) * 70)
)
</script>

<style scoped>
.heat {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  min-width: var(--heat-cell);
  height: var(--heat-cell);
  border-radius: var(--radius-sm);
  background: color-mix(in srgb, var(--chart-primary) var(--a), transparent);
  color: var(--text-primary);
  font-size: var(--text-xs);
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.heat--zero {
  background: var(--tile-none);
}
</style>

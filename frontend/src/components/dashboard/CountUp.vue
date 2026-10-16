<template>
  <span class="dash-num">{{ text }}</span>
</template>

<script setup>
// A number that counts up to its value (useCountUp); format turns it into text.
import { computed, toRef } from 'vue'
import { useCountUp } from '@/composables/useCountUp'

const props = defineProps({
  value: { type: Number, default: null },
  format: { type: Function, default: v => String(Math.round(v)) },
  empty: { type: String, default: '—' }
})
const shown = useCountUp(toRef(props, 'value'))
const text = computed(() =>
  shown.value === null ||
  shown.value === undefined ||
  !Number.isFinite(shown.value)
    ? props.empty
    : props.format(shown.value)
)
</script>

<template>
  <q-input
    ref="inner"
    v-bind="$attrs"
    class="app-field app-field--clickable"
    :model-value="displayValue"
    outlined
    dense
    readonly
    :label="label"
    :rules="rules"
    :disable="disable"
    :clearable="clearable && !!modelValue"
    @clear="emit('update:modelValue', '')"
  >
    <template #append>
      <q-icon name="event" class="cursor-pointer" />
    </template>
    <q-popup-proxy
      v-if="!disable"
      ref="popupRef"
      cover
      transition-show="scale"
      transition-hide="scale"
    >
      <q-date
        :model-value="modelValue"
        mask="YYYY-MM-DD"
        today-btn
        :options="options"
        @update:model-value="onPick"
      />
    </q-popup-proxy>
  </q-input>
</template>

<script setup>
// The date field of the app: shows dd.MM.yyyy, keeps "YYYY-MM-DD" in v-model,
// opens the calendar on a click anywhere in the field (icon on the right),
// and has the same solid frame as every other field — read-only is not dashed.
//   <AppDateInput v-model="form.birthDate" label="Tug'ilgan sana" no-future />
import { computed, ref } from 'vue'
import { formatDate } from '@/utils/date'

defineOptions({ inheritAttrs: false })

const props = defineProps({
  modelValue: { type: String, default: '' },
  label: { type: String, default: 'Sana' },
  rules: { type: Array, default: () => [] },
  disable: { type: Boolean, default: false },
  clearable: { type: Boolean, default: false },
  // school days only (Sundays greyed out) and/or nothing after today — used for grades
  noSundays: { type: Boolean, default: false },
  noFuture: { type: Boolean, default: false },
  // extra limits: "YYYY-MM-DD" bounds, inclusive
  min: { type: String, default: '' },
  max: { type: String, default: '' }
})
const emit = defineEmits(['update:modelValue'])

const pad = n => String(n).padStart(2, '0')
const slashed = s => (s ? s.replace(/-/g, '/') : '')

// q-date passes "YYYY/MM/DD"
const options = computed(() => {
  if (!props.noSundays && !props.noFuture && !props.min && !props.max) return undefined
  const t = new Date()
  const today = `${t.getFullYear()}/${pad(t.getMonth() + 1)}/${pad(t.getDate())}`
  const min = slashed(props.min)
  const max = slashed(props.max)
  return d => {
    if (props.noFuture && d > today) return false
    if (min && d < min) return false
    if (max && d > max) return false
    if (props.noSundays) {
      const [y, m, day] = d.split('/').map(Number)
      if (new Date(y, m - 1, day).getDay() === 0) return false
    }
    return true
  }
})

const inner = ref(null)
const popupRef = ref(null)
const displayValue = computed(() => (props.modelValue ? formatDate(props.modelValue) : ''))

function onPick(val) {
  emit('update:modelValue', val || '')
  popupRef.value?.hide()
}

defineExpose({
  validate: v => inner.value?.validate(v),
  resetValidation: () => inner.value?.resetValidation()
})
</script>

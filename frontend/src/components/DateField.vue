<template>
  <q-input
    :model-value="displayValue"
    outlined
    dense
    :label="label"
    :rules="rules"
    :disable="disable"
    readonly
  >
    <template v-slot:append>
      <q-icon name="event" class="cursor-pointer">
        <q-popup-proxy
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
      </q-icon>
    </template>
  </q-input>
</template>

<script setup>
import { computed, ref } from 'vue'
import { formatDate } from '@/utils/date'

const props = defineProps({
  modelValue: { type: String, default: '' },
  label: { type: String, default: 'Sana' },
  rules: { type: Array, default: () => [] },
  disable: { type: Boolean, default: false },
  // school days only (Sundays greyed out) and/or nothing after today — used for grades
  noSundays: { type: Boolean, default: false },
  noFuture: { type: Boolean, default: false }
})

// q-date passes "YYYY/MM/DD"
const options = computed(() => {
  if (!props.noSundays && !props.noFuture) return undefined
  const t = new Date()
  const today = `${t.getFullYear()}/${String(t.getMonth() + 1).padStart(2, '0')}/${String(t.getDate()).padStart(2, '0')}`
  return d => {
    if (props.noFuture && d > today) return false
    if (props.noSundays) {
      const [y, m, day] = d.split('/').map(Number)
      if (new Date(y, m - 1, day).getDay() === 0) return false
    }
    return true
  }
})
const emit = defineEmits(['update:modelValue'])

const popupRef = ref(null)
const displayValue = computed(() => formatDate(props.modelValue))

function onPick(val) {
  emit('update:modelValue', val)
  popupRef.value?.hide()
}
</script>

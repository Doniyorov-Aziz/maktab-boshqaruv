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
  disable: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue'])

const popupRef = ref(null)
const displayValue = computed(() => formatDate(props.modelValue))

function onPick(val) {
  emit('update:modelValue', val)
  popupRef.value?.hide()
}
</script>

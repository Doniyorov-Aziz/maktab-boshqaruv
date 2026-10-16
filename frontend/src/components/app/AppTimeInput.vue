<template>
  <q-input
    ref="inner"
    v-bind="$attrs"
    class="app-field"
    :model-value="modelValue"
    outlined
    dense
    mask="time"
    :label="label"
    :rules="rules"
    :disable="disable"
    @update:model-value="v => emit('update:modelValue', v || '')"
  >
    <template #append>
      <q-icon name="schedule" class="cursor-pointer">
        <q-popup-proxy
          v-if="!disable"
          ref="popupRef"
          cover
          transition-show="scale"
          transition-hide="scale"
        >
          <q-time
            :model-value="modelValue"
            format24h
            @update:model-value="v => emit('update:modelValue', v || '')"
          >
            <div class="row justify-end">
              <AppButton v-close-popup variant="ghost" size="sm" label="OK" />
            </div>
          </q-time>
        </q-popup-proxy>
      </q-icon>
    </template>
  </q-input>
</template>

<script setup>
// 24-hour time field ("HH:mm"): type it, or pick it from the clock behind the
// icon (the browser's own picker follows the OS and is often 12-hour). Same
// frame as every other field.
//   <AppTimeInput v-model="form.reportTime" label="Hisobot vaqti" />
import { ref } from 'vue'
import AppButton from './AppButton.vue'

defineOptions({ inheritAttrs: false })

defineProps({
  modelValue: { type: String, default: '' },
  label: { type: String, default: '' },
  rules: { type: Array, default: () => ['time'] },
  disable: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue'])

const inner = ref(null)
const popupRef = ref(null)
defineExpose({
  validate: v => inner.value?.validate(v),
  resetValidation: () => inner.value?.resetValidation()
})
</script>

<template>
  <q-select
    ref="inner"
    v-bind="$attrs"
    class="app-field"
    outlined
    dense
    options-dense
  >
    <template v-for="(_, name) in $slots" #[name]="scope">
      <slot :name="name" v-bind="scope || {}" />
    </template>
  </q-select>
</template>

<script setup>
// The select of the app: same frame as AppInput (outlined, label inside,
// --control-h, --radius-md). All q-select props, events and slots pass through:
// <AppSelect v-model="classId" :options="classes" label="Sinf" emit-value map-options />.
import { ref } from 'vue'

defineOptions({ inheritAttrs: false })

const inner = ref(null)
defineExpose({
  focus: () => inner.value?.focus(),
  validate: v => inner.value?.validate(v),
  resetValidation: () => inner.value?.resetValidation(),
  showPopup: () => inner.value?.showPopup(),
  hidePopup: () => inner.value?.hidePopup(),
  updateInputValue: (v, noFilter) => inner.value?.updateInputValue(v, noFilter)
})
</script>

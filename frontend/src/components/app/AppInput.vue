<template>
  <q-input
    ref="inner"
    v-bind="$attrs"
    :class="['app-field', { 'app-field--multiline': multiline }]"
    outlined
    dense
  >
    <template v-for="(_, name) in $slots" #[name]="scope">
      <slot :name="name" v-bind="scope || {}" />
    </template>
  </q-input>
</template>

<script setup>
// The text field of the app: outlined, label inside (floating), height
// --control-h, radius --radius-md. Every q-input prop, event and slot passes
// through: <AppInput v-model="name" label="Ism" :rules="[required]" />.
// type="textarea" / autogrow keep their natural height.
import { computed, ref, useAttrs } from 'vue'

defineOptions({ inheritAttrs: false })

const attrs = useAttrs()
const multiline = computed(
  () => attrs.type === 'textarea' || attrs.autogrow === '' || !!attrs.autogrow
)

const inner = ref(null)
// the parts of q-input callers use through a ref (focus, validation)
defineExpose({
  focus: () => inner.value?.focus(),
  blur: () => inner.value?.blur(),
  select: () => inner.value?.select(),
  validate: v => inner.value?.validate(v),
  resetValidation: () => inner.value?.resetValidation(),
  get nativeEl() {
    return inner.value?.nativeEl
  }
})
</script>

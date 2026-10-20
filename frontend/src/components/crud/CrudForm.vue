<template>
  <q-form
    ref="formEl"
    :id="formId"
    class="cform"
    data-crud-form
    greedy
    @submit.prevent="emit('submit')"
  >
    <div v-if="schoolName" class="cform__school">
      <q-icon name="school" /> Maktab: <b>{{ schoolName }}</b>
    </div>
    <section
      v-for="s in sections"
      :key="s.title"
      class="cform__section"
      :data-section="s.title"
    >
      <h4 v-if="s.title" class="cform__title">{{ s.title }}</h4>
      <div class="cform__grid">
        <div
          v-for="field in s.fields"
          :key="field.key"
          class="cform__cell"
          :class="{ 'cform__cell--full': isFull(field) }"
          :data-field="field.key"
        >
          <AppSelect
            v-if="field.type === 'select'"
            v-model="model[field.key]"
            :label="label(field)"
            :multiple="!!field.multiple"
            :use-chips="!!field.multiple"
            :options="optionsFor(field)"
            option-value="value"
            option-label="label"
            emit-value
            map-options
            :hint="field.hint"
            :rules="rules(field)"
            lazy-rules
            @update:model-value="emit('parent-change', field)"
          />
          <AppInput
            v-else-if="field.type === 'textarea'"
            v-model="model[field.key]"
            :label="label(field)"
            type="textarea"
            autogrow
            :hint="field.hint"
            :rules="rules(field)"
            lazy-rules
          />
          <AppDateInput
            v-else-if="field.type === 'date'"
            v-model="model[field.key]"
            :label="label(field)"
            :rules="rules(field)"
          />
          <AppInput
            v-else-if="field.type === 'phone'"
            :model-value="model[field.key]"
            :label="label(field)"
            type="tel"
            inputmode="tel"
            placeholder="+998 __ ___ __ __"
            :hint="field.hint"
            :rules="rules(field)"
            lazy-rules
            data-phone-input
            @update:model-value="v => (model[field.key] = maskPhone(v))"
          >
            <template #prepend><q-icon name="call" /></template>
          </AppInput>
          <div
            v-else-if="field.type === 'color'"
            class="cform__pick"
            role="radiogroup"
            :aria-label="field.label"
            data-color-picker
          >
            <span class="cform__pick-label">{{ field.label }}</span>
            <div class="cform__swatches">
              <button
                type="button"
                class="cform__swatch cform__swatch--auto"
                :class="{ 'cform__swatch--on': !model[field.key] }"
                role="radio"
                :aria-checked="!model[field.key]"
                aria-label="Avtomatik"
                data-swatch=""
                @click="model[field.key] = ''"
              >
                <q-icon name="auto_awesome" />
                <q-tooltip>Avtomatik</q-tooltip>
              </button>
              <button
                v-for="c in field.palette || []"
                :key="c.value"
                type="button"
                class="cform__swatch"
                :class="{ 'cform__swatch--on': model[field.key] === c.value }"
                :style="{ '--c': c.value }"
                role="radio"
                :aria-checked="model[field.key] === c.value"
                :aria-label="c.label"
                :data-swatch="c.value"
                @click="model[field.key] = c.value"
              >
                <q-icon v-if="model[field.key] === c.value" name="check" />
                <q-tooltip>{{ c.label }}</q-tooltip>
              </button>
            </div>
          </div>
          <div
            v-else-if="field.type === 'icon'"
            class="cform__pick"
            role="radiogroup"
            :aria-label="field.label"
            data-icon-picker
          >
            <span class="cform__pick-label"
              >{{ field.label
              }}<small v-if="!model[field.key] && field.suggest">
                — avtomatik: <q-icon :name="field.suggest(model)"
              /></small>
            </span>
            <div class="cform__icons">
              <button
                v-for="ic in field.icons || []"
                :key="ic"
                type="button"
                class="cform__icon"
                :class="{
                  'cform__icon--on': model[field.key] === ic,
                  'cform__icon--suggested':
                    !model[field.key] && field.suggest?.(model) === ic
                }"
                role="radio"
                :aria-checked="model[field.key] === ic"
                :aria-label="ic"
                :data-icon="ic"
                @click="model[field.key] = model[field.key] === ic ? '' : ic"
              >
                <q-icon :name="ic" />
              </button>
            </div>
          </div>
          <AppInput
            v-else
            v-model="model[field.key]"
            :label="label(field)"
            :type="inputType(field)"
            :hint="field.hint"
            :rules="rules(field)"
            lazy-rules
          />
        </div>
      </div>
    </section>
    <q-banner
      v-if="error"
      class="cform__error rounded-borders"
      dense
      data-form-error
    >
      <template #avatar><q-icon name="error_outline" /></template>
      {{ error }}
    </q-banner>
  </q-form>
</template>

<script setup>
// The create / edit form of a list, inside the wide side panel: fields grouped under their
// section titles (field.section), two columns (field.full spans both; a textarea always does),
// errors under a field once it was left (lazy rules) and all of them on save, a phone field typed
// into "+998 90 111 22 33". The page owns the model, the save and the unsaved-changes warning.
import { computed, ref } from 'vue'
import AppInput from '@/components/app/AppInput.vue'
import AppSelect from '@/components/app/AppSelect.vue'
import AppDateInput from '@/components/app/AppDateInput.vue'
import { maskPhone, phoneComplete } from './crud'

const props = defineProps({
  formId: { type: String, required: true },
  fields: { type: Array, required: true },
  model: { type: Object, required: true },
  optionsFor: { type: Function, required: true },
  editing: { type: Boolean, default: false },
  error: { type: String, default: '' },
  schoolName: { type: String, default: '' }
})
const emit = defineEmits(['submit', 'parent-change'])
const formEl = ref(null)

/** Sections in the order their first field comes; fields without one go under "Asosiy". */
const sections = computed(() => {
  const out = []
  const byTitle = {}
  for (const f of props.fields) {
    const title = f.section || "Asosiy ma'lumot"
    if (!byTitle[title]) {
      byTitle[title] = { title, fields: [] }
      out.push(byTitle[title])
    }
    byTitle[title].fields.push(f)
  }
  return out.length === 1 && !props.fields.some(f => f.section)
    ? [{ title: '', fields: out[0].fields }]
    : out
})

const isFull = f =>
  f.full ||
  f.type === 'textarea' ||
  f.multiple ||
  ['color', 'icon'].includes(f.type)

function required(field) {
  return !!field.required && !(props.editing && field.requiredOnCreateOnly)
}

function label(field) {
  return required(field) ? `${field.label} *` : field.label
}

function rules(field) {
  const out = []
  if (required(field))
    out.push(
      v =>
        (v !== null &&
          v !== undefined &&
          v !== '' &&
          !(field.type === 'phone' && v === '+998') &&
          !(Array.isArray(v) && !v.length)) ||
        'Majburiy maydon'
    )
  if (field.type === 'phone')
    out.push(
      v =>
        !v ||
        v === '+998' ||
        phoneComplete(v) ||
        "Telefon to'liq emas: +998 90 123 45 67"
    )
  if (field.type === 'number')
    out.push(
      v =>
        v === null ||
        v === '' ||
        v === undefined ||
        !isNaN(Number(v)) ||
        'Raqam kiriting'
    )
  for (const r of field.rules || []) out.push(r)
  return out
}

function inputType(field) {
  return (
    { password: 'password', number: 'number', time: 'time' }[field.type] ||
    'text'
  )
}

defineExpose({ validate: () => formEl.value?.validate() })
</script>

<style scoped>
.cform {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
  padding-top: var(--space-2);
}

.cform__school {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-md);
  background: color-mix(in srgb, var(--c-primary) 8%, transparent);
  color: var(--c-primary);
  font-size: var(--text-sm);
}

.cform__section {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.cform__title {
  margin: 0;
  font-size: var(--text-xs);
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--brand-text-muted);
}

.cform__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-1) var(--space-3);
}

.cform__cell--full {
  grid-column: 1 / -1;
}

@media (max-width: 599px) {
  .cform__grid {
    grid-template-columns: minmax(0, 1fr);
  }
}

.cform__pick {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  padding: var(--space-1) 0 var(--space-3);
}

.cform__pick-label {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  font-size: var(--text-sm);
  color: var(--brand-text-muted);
}

.cform__swatches,
.cform__icons {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.cform__swatch {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: var(--control-h-sm);
  height: var(--control-h-sm);
  border: 2px solid transparent;
  border-radius: var(--radius-pill);
  background: var(--c);
  color: white;
  cursor: pointer;
  transition: transform var(--dur-fast) var(--ease-out);
}

.cform__swatch:hover {
  transform: scale(1.08);
}

.cform__swatch--auto {
  --c: var(--surface-2);
  border-color: var(--brand-border);
  color: var(--brand-text-muted);
}

.cform__swatch--on {
  box-shadow:
    0 0 0 2px var(--card-bg),
    0 0 0 4px var(--c);
}

.cform__swatch--auto.cform__swatch--on {
  box-shadow:
    0 0 0 2px var(--card-bg),
    0 0 0 4px var(--c-primary);
  color: var(--c-primary);
}

.cform__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: var(--control-h);
  height: var(--control-h);
  border: 1px solid var(--c-border-control);
  border-radius: var(--radius-md);
  background: transparent;
  color: var(--text-primary);
  font-size: var(--text-lg);
  cursor: pointer;
}

.cform__icon:hover {
  border-color: var(--c-primary);
  color: var(--c-primary);
}

.cform__icon--suggested {
  border-style: dashed;
  border-color: var(--c-primary);
}

.cform__icon--on {
  border-color: var(--c-primary);
  background: var(--c-primary);
  color: white;
}

.cform__error {
  background: color-mix(in srgb, var(--st-absent) 10%, transparent);
  color: var(--st-absent);
}
</style>

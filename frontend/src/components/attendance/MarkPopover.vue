<template>
  <!-- mounted only with its button: the menu reads its anchor when it is created -->
  <q-menu
    v-if="modelValue && target"
    model-value
    :target="target"
    no-parent-event
    anchor="bottom middle"
    self="top middle"
    :offset="[0, 6]"
    class="mp"
    @update:model-value="v => emit('update:modelValue', v)"
  >
    <div class="mp__body" :data-mark-popover="kind">
      <template v-if="kind === 'LATE'">
        <div class="mp__title"
          ><q-icon name="schedule" /> Necha daqiqa kechikdi?</div
        >
        <div class="mp__grid">
          <button
            v-for="m in LATE_MINUTES"
            :key="m"
            v-close-popup
            type="button"
            class="mp__opt"
            :class="{ 'mp__opt--on': m === lateMinutes }"
            :data-late="m"
            @click="emit('apply', { lateMinutes: m })"
          >
            {{ m === 20 ? '20+' : m }} daq
          </button>
        </div>
      </template>
      <template v-else>
        <div class="mp__title"><q-icon name="description" /> Sabab</div>
        <div class="mp__grid">
          <button
            v-for="r in REASONS"
            :key="r"
            type="button"
            class="mp__opt"
            :class="{ 'mp__opt--on': r === reason }"
            :data-reason="r"
            @click="reason = r"
          >
            {{ r }}
          </button>
        </div>
        <AppInput
          v-model="note"
          dense
          autofocus
          placeholder="Izoh (ixtiyoriy)"
          aria-label="Izoh"
          data-reason-note
          @keydown.enter="apply"
        />
        <AppButton
          v-close-popup
          label="Saqlash"
          icon="check"
          size="sm"
          data-reason-save
          @click="apply"
        />
      </template>
    </div>
  </q-menu>
</template>

<script setup>
// A small window next to the status a user just chose: for "Kechikdi" how many minutes
// (5 / 10 / 15 / 20+), for "Sababli" the reason (illness / family / other) and a note.
// The reason goes into the record's comment as "Kasallik — izoh".
import { ref, watch } from 'vue'
import AppButton from '@/components/app/AppButton.vue'
import AppInput from '@/components/app/AppInput.vue'

const LATE_MINUTES = [5, 10, 15, 20]
const REASONS = ['Kasallik', 'Oilaviy sabab', 'Boshqa']

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  target: { type: Object, default: null },
  kind: { type: String, default: 'LATE' },
  lateMinutes: { type: Number, default: null },
  comment: { type: String, default: '' }
})
const emit = defineEmits(['update:modelValue', 'apply'])

const reason = ref(REASONS[0])
const note = ref('')

watch(
  () => [props.modelValue, props.comment],
  () => {
    if (!props.modelValue) return
    const [r, ...rest] = String(props.comment || '').split(' — ')
    reason.value = REASONS.includes(r) ? r : REASONS[0]
    note.value = REASONS.includes(r) ? rest.join(' — ') : props.comment || ''
  },
  { immediate: true }
)

function apply() {
  const text = note.value.trim()
  emit('apply', { comment: text ? `${reason.value} — ${text}` : reason.value })
  emit('update:modelValue', false)
}
</script>

<style scoped>
.mp__body {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  padding: var(--space-3);
  min-width: var(--field-w-sm);
}

.mp__title {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  font-size: var(--text-sm);
  font-weight: 700;
}

.mp__grid {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-1);
}

.mp__opt {
  height: var(--control-h-sm);
  padding: 0 var(--space-3);
  border: 1px solid var(--c-border-control);
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--text-primary);
  font: inherit;
  font-size: var(--text-sm);
  font-weight: 600;
  cursor: pointer;
}

.mp__opt:hover {
  border-color: var(--c-primary);
}

.mp__opt--on {
  background: var(--c-primary);
  border-color: var(--c-primary);
  color: white;
}
</style>

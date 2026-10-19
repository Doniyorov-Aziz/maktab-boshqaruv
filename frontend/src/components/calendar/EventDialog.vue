<template>
  <q-dialog
    :model-value="modelValue"
    @update:model-value="v => emit('update:modelValue', v)"
  >
    <q-card class="app-dialog app-dialog--md ed" data-event-dialog>
      <q-card-section class="row items-center q-pb-none">
        <IconTile
          :icon="EVENT_TYPES[form.type]?.icon || 'event'"
          size="40"
          :color="`color-mix(in srgb, ${EVENT_TYPES[form.type]?.color} 16%, transparent)`"
          :text-color="EVENT_TYPES[form.type]?.color"
          class="q-mr-sm"
        />
        <div class="text-h6">{{ event?.id ? 'Tadbirni tahrirlash' : 'Yangi tadbir' }}</div>
        <q-space />
        <AppButton variant="ghost" icon="close" aria-label="Yopish" v-close-popup />
      </q-card-section>

      <q-card-section class="ed__form">
        <AppInput
          v-model="form.title"
          label="Nomi"
          autofocus
          data-ed-title
          class="ed__wide"
        />
        <div class="ed__types ed__wide" role="radiogroup" aria-label="Turi">
          <button
            v-for="t in TYPE_ORDER"
            :key="t"
            type="button"
            class="ed__type"
            :class="{ 'ed__type--on': form.type === t }"
            :style="{ '--c': EVENT_TYPES[t].color }"
            :aria-pressed="form.type === t"
            :data-type="t"
            @click="form.type = t"
          >
            <q-icon :name="EVENT_TYPES[t].icon" /> {{ EVENT_TYPES[t].label }}
          </button>
        </div>
        <AppDateInput v-model="form.startDate" label="Boshlanishi" />
        <AppDateInput v-model="form.endDate" label="Tugashi" :min="form.startDate" />
        <q-toggle v-model="form.timed" label="Vaqti bor" class="ed__wide" />
        <template v-if="form.timed">
          <AppTimeInput v-model="form.startTime" label="Boshlanish vaqti" />
          <AppTimeInput v-model="form.endTime" label="Tugash vaqti" />
        </template>
        <AppSelect
          v-model="form.roomId"
          label="Xona (band qilinadi)"
          :options="roomOptions"
          emit-value
          map-options
          clearable
          hint="Xona bandligi tekshiriladi"
        />
        <AppInput v-model="form.location" label="Yoki joy (hovli, stadion…)" />
        <div class="ed__wide ed__aud">
          <span class="ed__label">Kimlar uchun</span>
          <SegTabs v-model="audience" :options="AUDIENCE" label="Kimlar uchun" />
          <AppSelect
            v-if="audience === 'classes'"
            v-model="form.classIds"
            label="Sinflar"
            :options="classOptions"
            emit-value
            map-options
            multiple
            use-chips
            class="ed__classes"
          />
        </div>
        <q-toggle
          v-model="form.notifyParents"
          class="ed__wide"
          data-ed-notify
          label="Ota-onalarga Telegram orqali xabar berish (hozir va 1 kun oldin)"
        />
        <template v-if="!event?.id">
          <AppSelect
            v-model="form.recurrence"
            label="Takrorlanishi"
            :options="RECURRENCE"
            emit-value
            map-options
          />
          <AppDateInput
            v-if="form.recurrence !== 'NONE'"
            v-model="form.recurrenceUntil"
            label="Qachongacha"
            :min="form.startDate"
          />
        </template>
        <AppInput
          v-model="form.description"
          label="Izoh"
          type="textarea"
          autogrow
          class="ed__wide"
        />
        <q-banner v-if="error" class="ed__error ed__wide" dense data-ed-error>
          <template #avatar><q-icon name="error_outline" /></template>
          {{ error }}
        </q-banner>
      </q-card-section>

      <q-card-actions class="q-pa-md">
        <AppButton
          v-if="event?.id && canDelete"
          variant="danger"
          icon="delete"
          label="O'chirish"
          :loading="deleting"
          @click="remove"
        />
        <q-space />
        <AppButton variant="ghost" label="Bekor qilish" v-close-popup />
        <AppButton
          icon="check"
          label="Saqlash"
          :disable="!ready"
          :loading="saving"
          data-ed-save
          @click="save"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup>
// A calendar event: name, type (colour and icon), dates, an optional time, a room (booked,
// a busy room is refused with the reason) or another place, for whom (the whole school or
// some classes), "tell the parents" on Telegram, and — when new — a repetition.
import { computed, reactive, ref, watch } from 'vue'
import { useQuasar } from 'quasar'
import IconTile from '@/components/app/IconTile.vue'
import AppButton from '@/components/app/AppButton.vue'
import AppInput from '@/components/app/AppInput.vue'
import AppSelect from '@/components/app/AppSelect.vue'
import AppDateInput from '@/components/app/AppDateInput.vue'
import AppTimeInput from '@/components/app/AppTimeInput.vue'
import SegTabs from '@/components/dashboard/SegTabs.vue'
import { api } from '@/boot/axios'
import { useSchoolStore } from '@/stores/school'
import { EVENT_TYPES, TYPE_ORDER } from './calendar'
import { roomLabel } from '@/components/timetable/timetable'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  // an event to edit, or { startDate } for a new one
  event: { type: Object, default: null },
  rooms: { type: Array, default: () => [] },
  classes: { type: Array, default: () => [] },
  canDelete: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue', 'saved'])
const $q = useQuasar()
const school = useSchoolStore()

const AUDIENCE = [
  { value: 'all', label: 'Butun maktab' },
  { value: 'classes', label: 'Sinflar' }
]
const RECURRENCE = [
  { value: 'NONE', label: 'Takrorlanmaydi' },
  { value: 'WEEKLY', label: 'Har hafta' },
  { value: 'MONTHLY', label: 'Har oy' }
]

const form = reactive({})
const audience = ref('all')
const error = ref('')
const saving = ref(false)
const deleting = ref(false)

watch(
  () => [props.modelValue, props.event],
  ([open]) => {
    if (!open) return
    const e = props.event || {}
    Object.assign(form, {
      title: e.title || '',
      type: e.type || 'SCHOOL_EVENT',
      startDate: e.startDate || '',
      endDate: e.endDate || e.startDate || '',
      timed: !!e.startTime,
      startTime: (e.startTime || '').slice(0, 5),
      endTime: (e.endTime || '').slice(0, 5),
      roomId: e.roomId || null,
      location: e.location || '',
      classIds: e.classIds || [],
      notifyParents: !!e.notifyParents,
      description: e.description || '',
      recurrence: 'NONE',
      recurrenceUntil: ''
    })
    audience.value = e.classIds?.length ? 'classes' : 'all'
    error.value = ''
  },
  { immediate: true }
)
watch(
  () => form.startDate,
  v => {
    if (v && (!form.endDate || form.endDate < v)) form.endDate = v
  }
)

const roomOptions = computed(() =>
  props.rooms
    .filter(r => r.status !== 'REPAIR')
    .map(r => ({ value: r.id, label: `${roomLabel(r.number)} · ${r.building}` }))
)
const classOptions = computed(() => props.classes.map(c => ({ value: c.id, label: c.name })))
const ready = computed(
  () =>
    form.title?.trim() &&
    form.startDate &&
    form.endDate &&
    (!form.timed || (form.startTime && form.endTime)) &&
    (form.recurrence === 'NONE' || form.recurrenceUntil)
)

function payload() {
  return {
    schoolId: school.activeSchoolId,
    title: form.title.trim(),
    type: form.type,
    startDate: form.startDate,
    endDate: form.endDate,
    startTime: form.timed ? `${form.startTime}:00` : null,
    endTime: form.timed ? `${form.endTime}:00` : null,
    roomId: form.roomId || null,
    location: form.location || null,
    classIds: audience.value === 'classes' ? form.classIds : [],
    notifyParents: form.notifyParents,
    description: form.description || null,
    recurrence: props.event?.id ? undefined : form.recurrence,
    recurrenceUntil: form.recurrence !== 'NONE' ? form.recurrenceUntil : null
  }
}

async function save() {
  saving.value = true
  error.value = ''
  try {
    if (props.event?.id) await api.put(`/api/calendar-events/${props.event.id}`, payload())
    else await api.post('/api/calendar-events', payload())
    $q.notify({
      type: 'positive',
      message: form.notifyParents && !props.event?.id ? 'Saqlandi · ota-onalarga xabar navbatda' : 'Saqlandi'
    })
    emit('update:modelValue', false)
    emit('saved')
  } catch (e) {
    error.value = e.friendlyMessage || "Saqlab bo'lmadi"
  } finally {
    saving.value = false
  }
}

async function remove() {
  deleting.value = true
  try {
    await api.delete(`/api/calendar-events/${props.event.id}`)
    emit('update:modelValue', false)
    emit('saved')
  } catch (e) {
    error.value = e.friendlyMessage || "O'chirib bo'lmadi"
  } finally {
    deleting.value = false
  }
}
</script>

<style scoped>
.ed__form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-3);
}

.ed__wide {
  grid-column: 1 / -1;
}

.ed__types {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-1);
}

.ed__type {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  padding: var(--space-1) var(--space-3);
  border: 1px solid color-mix(in srgb, var(--c) 35%, transparent);
  border-radius: var(--radius-pill);
  background: transparent;
  color: var(--text-primary);
  font: inherit;
  font-size: var(--text-xs);
  font-weight: 600;
  cursor: pointer;
}

.ed__type .q-icon {
  color: var(--c);
}

.ed__type--on {
  background: color-mix(in srgb, var(--c) 18%, transparent);
  border-color: var(--c);
}

.ed__aud {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-3);
}

.ed__label {
  font-size: var(--text-sm);
  color: var(--brand-text-muted);
}

.ed__classes {
  flex: 1 1 var(--field-w);
}

.ed__error {
  border-radius: var(--radius-md);
  background: color-mix(in srgb, var(--st-absent) 12%, transparent);
  color: var(--st-absent);
}

@media (max-width: 599px) {
  .ed__form {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>

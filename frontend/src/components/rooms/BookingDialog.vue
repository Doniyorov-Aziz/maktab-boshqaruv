<template>
  <q-dialog
    :model-value="modelValue"
    @update:model-value="v => emit('update:modelValue', v)"
  >
    <q-card class="app-dialog app-dialog--sm" data-booking-dialog>
      <q-card-section class="row items-center q-pb-none">
        <div class="text-h6"
          >Band qilish · {{ room ? roomLabel(room.number) : '' }}</div
        >
        <q-space />
        <AppButton
          variant="ghost"
          icon="close"
          aria-label="Yopish"
          v-close-popup
        />
      </q-card-section>
      <q-card-section class="bd">
        <AppDateInput
          v-model="form.date"
          label="Sana"
          :min="today"
          class="bd__wide"
        />
        <AppTimeInput v-model="form.start" label="Boshlanishi" />
        <AppTimeInput v-model="form.end" label="Tugashi" />
        <AppInput
          v-model="form.purpose"
          label="Maqsad (yig'ilish, qo'shimcha dars…)"
          class="bd__wide"
          data-booking-purpose
        />
        <AppInput v-model="form.responsible" label="Mas'ul" class="bd__wide" />
        <q-banner
          v-if="error"
          dense
          class="bd__error bd__wide"
          data-booking-error
        >
          <template #avatar><q-icon name="error_outline" /></template>
          {{ error }}
        </q-banner>
      </q-card-section>
      <q-card-actions align="right" class="q-pa-md">
        <AppButton variant="ghost" label="Bekor qilish" v-close-popup />
        <AppButton
          icon="event_available"
          label="Band qilish"
          :disable="!ready"
          :loading="saving"
          data-booking-save
          @click="save"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup>
// A one-off booking of a room: date, time, what for and who answers for it. A lesson or
// another booking at that time is refused with the reason (409).
import { computed, reactive, ref, watch } from 'vue'
import { useQuasar } from 'quasar'
import AppButton from '@/components/app/AppButton.vue'
import AppInput from '@/components/app/AppInput.vue'
import AppDateInput from '@/components/app/AppDateInput.vue'
import AppTimeInput from '@/components/app/AppTimeInput.vue'
import { api } from '@/boot/axios'
import { useSchoolStore } from '@/stores/school'
import { roomLabel } from '@/components/timetable/timetable'
import { tashkentNow } from '@/utils/tashkent'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  room: { type: Object, default: null },
  // { date, start, end } to start from (the finder's choice)
  preset: { type: Object, default: null }
})
const emit = defineEmits(['update:modelValue', 'saved'])
const $q = useQuasar()
const school = useSchoolStore()
const today = tashkentNow().date

const form = reactive({
  date: '',
  start: '',
  end: '',
  purpose: '',
  responsible: ''
})
const error = ref('')
const saving = ref(false)
watch(
  () => props.modelValue,
  open => {
    if (!open) return
    Object.assign(form, {
      date: props.preset?.date || today,
      start: (props.preset?.start || '').slice(0, 5),
      end: (props.preset?.end || '').slice(0, 5),
      purpose: '',
      responsible: ''
    })
    error.value = ''
  }
)
const ready = computed(
  () => form.date && form.start && form.end && form.purpose.trim()
)

async function save() {
  saving.value = true
  error.value = ''
  try {
    await api.post(
      '/api/pages/rooms/bookings',
      {
        roomId: props.room.id,
        date: form.date,
        startTime: `${form.start}:00`,
        endTime: `${form.end}:00`,
        purpose: form.purpose.trim(),
        responsible: form.responsible || null
      },
      { params: { schoolId: school.activeSchoolId } }
    )
    $q.notify({
      type: 'positive',
      message: `${roomLabel(props.room.number)} band qilindi`
    })
    emit('update:modelValue', false)
    emit('saved')
  } catch (e) {
    error.value = e.friendlyMessage || "Band qilib bo'lmadi"
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.bd {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-3);
}

.bd__wide {
  grid-column: 1 / -1;
}

.bd__error {
  border-radius: var(--radius-md);
  background: color-mix(in srgb, var(--st-absent) 12%, transparent);
  color: var(--st-absent);
}
</style>

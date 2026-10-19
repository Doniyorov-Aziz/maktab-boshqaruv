<template>
  <q-dialog
    :model-value="modelValue"
    @update:model-value="v => emit('update:modelValue', v)"
  >
    <q-card class="app-dialog app-dialog--sm qa" data-quick-add>
      <q-card-section class="row items-center q-pb-none">
        <div class="text-h6">Dars qo'shish</div>
        <q-space />
        <AppButton variant="ghost" icon="close" aria-label="Yopish" v-close-popup />
      </q-card-section>
      <q-card-section v-if="slot" class="qa__when">
        {{ slot.day }}, {{ slot.period.n }}-dars · {{ hm(slot.period.start) }}–{{
          hm(slot.period.end)
        }}
      </q-card-section>
      <q-card-section class="q-gutter-md">
        <AppSelect
          v-if="!fixed.classId"
          v-model="form.classId"
          label="Sinf"
          :options="classOptions"
          emit-value
          map-options
        />
        <AppSelect
          v-model="form.subjectId"
          label="Fan"
          :options="subjectOptions"
          emit-value
          map-options
          data-qa-subject
        />
        <AppSelect
          v-if="!fixed.teacherId"
          v-model="form.teacherId"
          label="O'qituvchi (faqat bo'shlari)"
          :options="teacherOptions"
          emit-value
          map-options
          data-qa-teacher
        />
        <AppSelect
          v-if="!fixed.roomId"
          v-model="form.roomId"
          label="Xona (faqat bo'shlari)"
          :options="roomOptions"
          emit-value
          map-options
          data-qa-room
        />
      </q-card-section>
      <q-card-actions align="right" class="q-pa-md">
        <AppButton variant="ghost" label="Bekor qilish" v-close-popup />
        <AppButton
          label="Qo'shish"
          icon="add"
          :disable="!ready"
          :loading="saving"
          data-qa-save
          @click="save"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup>
// "+" on an empty cell: a lesson in a few clicks. Only the teachers and rooms that are free
// at that time are offered (and the backend checks again).
import { computed, reactive, ref, watch } from 'vue'
import { useQuasar } from 'quasar'
import AppButton from '@/components/app/AppButton.vue'
import AppSelect from '@/components/app/AppSelect.vue'
import { api } from '@/boot/axios'
import { busy, hm, roomLabel } from './timetable'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  // { day, period }
  slot: { type: Object, default: null },
  // the view's own entity: { classId } / { teacherId } / { roomId }
  fixed: { type: Object, default: () => ({}) },
  week: { type: Object, required: true }
})
const emit = defineEmits(['update:modelValue', 'saved'])
const $q = useQuasar()

const form = reactive({ classId: null, subjectId: null, teacherId: null, roomId: null })
const saving = ref(false)

watch(
  () => props.modelValue,
  open => {
    if (!open) return
    form.classId = props.fixed.classId || null
    form.teacherId = props.fixed.teacherId || null
    form.roomId = props.fixed.roomId || null
    form.subjectId = null
  }
)

const taken = computed(() =>
  props.slot
    ? busy(props.week.lessons, props.slot.day, props.slot.period.start, props.slot.period.end)
    : { teachers: new Set(), rooms: new Set(), classes: new Set() }
)
const classOptions = computed(() =>
  props.week.classes
    .filter(c => !taken.value.classes.has(c.id))
    .map(c => ({ value: c.id, label: c.name }))
)
const subjectOptions = computed(() =>
  props.week.subjects
    .filter(s => s.active)
    .map(s => ({ value: s.id, label: s.name }))
)
const teacherOptions = computed(() =>
  props.week.teachers
    .filter(t => !taken.value.teachers.has(t.id))
    .map(t => ({ value: t.id, label: t.name }))
)
const roomOptions = computed(() =>
  props.week.rooms
    .filter(r => r.status !== 'REPAIR' && !taken.value.rooms.has(r.id))
    .map(r => ({ value: r.id, label: `${roomLabel(r.number)} · ${r.building}` }))
)
const ready = computed(
  () => form.classId && form.subjectId && form.teacherId && form.roomId
)

async function save() {
  saving.value = true
  try {
    await api.post('/api/lesson-slots', {
      schoolClassId: form.classId,
      subjectId: form.subjectId,
      employeeId: form.teacherId,
      roomId: form.roomId,
      weekday: props.slot.day,
      startTime: hm(props.slot.period.start) + ':00',
      endTime: hm(props.slot.period.end) + ':00'
    })
    $q.notify({ type: 'positive', message: "Dars qo'shildi" })
    emit('update:modelValue', false)
    emit('saved')
  } catch (e) {
    $q.notify({ type: 'negative', message: e.friendlyMessage || 'Xatolik' })
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.qa__when {
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--brand-text-muted);
}
</style>

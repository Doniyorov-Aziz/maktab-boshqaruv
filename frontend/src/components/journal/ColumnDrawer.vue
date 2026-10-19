<template>
  <SideDrawer
    :model-value="modelValue"
    title="Butun sinfga baho"
    :subtitle="day ? `${dayLabel(day.date)} · ${subjectName}` : ''"
    icon="playlist_add_check"
    persistent
    @update:model-value="v => emit('update:modelValue', v)"
  >
    <template #toolbar>
      <div class="cd__tools">
        <SegTabs v-model="type" label="Baho turi" :options="TYPES" />
        <span class="cd__count" data-column-count
          >{{ chosen }} / {{ students.length }} tanlandi</span
        >
      </div>
      <div class="cd__fill">
        <span>Belgilanmaganlarga:</span>
        <button
          v-for="v in GRADE_VALUES"
          :key="v"
          type="button"
          class="cd__mini"
          :style="{ '--g': GRADE_COLOR[v] }"
          :data-fill="v"
          @click="fillRest(v)"
        >
          {{ v }}
        </button>
        <button
          type="button"
          class="cd__clear"
          data-fill-clear
          @click="values = {}"
          >Tozalash</button
        >
      </div>
    </template>

    <div class="cd__list" data-column-list>
      <div
        v-for="s in students"
        :key="s.id"
        class="cd__row"
        :class="{ 'cd__row--absent': absent(s.id) }"
        :data-column-student="s.id"
      >
        <AvatarInitials :name="s.fullName" :id="s.id" size="xs" />
        <div class="cd__who">
          <span class="ellipsis">{{ s.fullName }}</span>
          <small v-if="absent(s.id)" class="cd__tag" data-absent>
            {{ attendance[s.id] === 'EXCUSED' ? "Sababli yo'q" : 'Kelmagan' }}
          </small>
          <small v-else-if="(existing[s.id] || []).length" class="cd__has">
            bor: {{ existing[s.id].map(g => g.value).join(', ') }}
          </small>
        </div>
        <div class="cd__btns" role="group" :aria-label="`${s.fullName}: baho`">
          <button
            v-for="v in GRADE_VALUES"
            :key="v"
            type="button"
            class="cd__btn"
            :class="{ 'cd__btn--on': values[s.id] === v }"
            :style="{ '--g': GRADE_COLOR[v] }"
            :aria-pressed="values[s.id] === v"
            :data-value="v"
            @click="toggle(s.id, v)"
          >
            {{ v }}
          </button>
        </div>
      </div>
    </div>

    <template #footer>
      <AppButton
        variant="secondary"
        label="Bekor qilish"
        @click="emit('update:modelValue', false)"
      />
      <AppButton
        icon="save"
        :label="`${chosen} ta bahoni saqlash`"
        :disable="!chosen"
        :loading="saving"
        data-column-save
        @click="save"
      />
    </template>
  </SideDrawer>
</template>

<script setup>
// "Butun sinfga baho": a click on a day's header opens the class for that day — one row a
// student with 2–5 buttons, the type for all of them, a quick "the rest get 5". Students who
// were absent that day are faded and labelled (they can still be graded).
import { ref, computed, watch } from 'vue'
import SideDrawer from '@/components/dashboard/SideDrawer.vue'
import SegTabs from '@/components/dashboard/SegTabs.vue'
import AppButton from '@/components/app/AppButton.vue'
import AvatarInitials from '@/components/pages/AvatarInitials.vue'
import { GRADE_VALUES, GRADE_COLOR, TYPES } from './journal'
import { dayLabel } from '@/components/attendance/attendance'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  day: { type: Object, default: null },
  subjectName: { type: String, default: '' },
  students: { type: Array, required: true },
  // studentId → grades of that day
  existing: { type: Object, default: () => ({}) },
  // studentId → status of that day (from /api/pages/journal/day-attendance)
  attendance: { type: Object, default: () => ({}) },
  saving: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue', 'save'])

const type = ref('CURRENT')
const values = ref({})
watch(
  () => props.modelValue,
  open => {
    if (!open) return
    type.value = 'CURRENT'
    values.value = {}
  }
)

const chosen = computed(
  () => Object.values(values.value).filter(Boolean).length
)
const absent = id =>
  props.attendance[id] === 'ABSENT' || props.attendance[id] === 'EXCUSED'

function toggle(id, v) {
  values.value = { ...values.value, [id]: values.value[id] === v ? null : v }
}

/** Everyone present without a grade yet (here or already in the journal) gets v. */
function fillRest(v) {
  const next = { ...values.value }
  for (const s of props.students)
    if (!next[s.id] && !absent(s.id) && !(props.existing[s.id] || []).length)
      next[s.id] = v
  values.value = next
}

function save() {
  const entries = Object.entries(values.value)
    .filter(([, v]) => v)
    .map(([id, v]) => ({ studentId: Number(id), value: v }))
  emit('save', { entries, type: type.value })
}
</script>

<style scoped>
.cd__tools,
.cd__fill {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.cd__fill {
  margin-top: var(--space-2);
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
}

.cd__count {
  margin-left: auto;
  font-size: var(--text-xs);
  font-weight: 700;
}

.cd__mini,
.cd__clear {
  height: var(--control-h-sm);
  min-width: var(--control-h-sm);
  border: 1px solid var(--c-border-control);
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--g, var(--text-primary));
  font: inherit;
  font-weight: 800;
  cursor: pointer;
}

.cd__clear {
  padding: 0 var(--space-2);
  font-weight: 600;
}

.cd__list {
  display: flex;
  flex-direction: column;
}

.cd__row {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) 0;
  border-bottom: 1px solid var(--dash-edge);
}

.cd__row--absent .cd__who,
.cd__row--absent :deep(.avatar) {
  opacity: 0.5;
}

.cd__who {
  display: flex;
  flex-direction: column;
  flex: 1 1 auto;
  min-width: 0;
  font-size: var(--text-sm);
  font-weight: 600;
}

.cd__tag {
  color: var(--st-absent);
  font-weight: 700;
}

.cd__has {
  color: var(--brand-text-muted);
  font-weight: 500;
}

.cd__btns {
  display: flex;
  gap: var(--space-1);
}

.cd__btn {
  width: var(--control-h-sm);
  height: var(--control-h-sm);
  border: 1px solid color-mix(in srgb, var(--g) 35%, transparent);
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--g);
  font: inherit;
  font-weight: 800;
  cursor: pointer;
}

.cd__btn:hover {
  background: color-mix(in srgb, var(--g) 14%, transparent);
}

.cd__btn--on,
.cd__btn--on:hover {
  background: var(--g);
  color: white;
}
</style>

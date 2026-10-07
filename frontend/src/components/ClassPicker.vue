<template>
  <!-- parallel first (1…11), then its letters (1-A, 1-B) — shared by attendance and the journal -->
  <div class="class-picker">
    <div class="row items-center no-wrap cp-row">
      <span class="cp-label">Sinf:</span>
      <div class="row items-center q-gutter-xs cp-scroll">
        <q-btn
          v-for="g in GRADES"
          :key="g"
          :label="String(g)"
          dense
          unelevated
          no-caps
          class="cp-grade"
          :class="{ 'cp-grade--on': g === grade }"
          :disable="!usable(g)"
          :aria-pressed="g === grade"
          :aria-label="`${g}-sinflar`"
          @click="pickGrade(g)"
        >
          <q-tooltip v-if="!usable(g)"
            >{{ g }}-sinflarda o'quvchi yo'q</q-tooltip
          >
        </q-btn>
      </div>
    </div>
    <div v-if="letters.length" class="row items-center no-wrap cp-row q-mt-xs">
      <span class="cp-label"></span>
      <div class="row items-center q-gutter-xs cp-scroll">
        <button
          v-for="c in letters"
          :key="c.id"
          type="button"
          class="cp-letter"
          :class="{ 'cp-letter--on': c.id === modelValue }"
          :disabled="!hasStudents(c)"
          :aria-pressed="c.id === modelValue"
          :title="hasStudents(c) ? '' : 'Sinfda o\'quvchi yo\'q'"
          @click="emit('update:modelValue', c.id)"
        >
          {{ c.gradeNumber }}-{{ c.sectionLetter }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({
  /** /api/school-classes rows: { id, gradeNumber, sectionLetter, studentCount } */
  classes: { type: Array, default: () => [] },
  modelValue: { type: Number, default: null }
})
const emit = defineEmits(['update:modelValue', 'update:grade'])

const GRADES = Array.from({ length: 11 }, (_, i) => i + 1)
const grade = ref(null)

// a class counts when it has students (older API rows without the count count as usable)
const hasStudents = c => c.studentCount == null || c.studentCount > 0

const byGrade = computed(() => {
  const map = {}
  for (const c of props.classes) (map[c.gradeNumber] ||= []).push(c)
  for (const list of Object.values(map))
    list.sort((a, b) => String(a.sectionLetter).localeCompare(b.sectionLetter))
  return map
})
const letters = computed(() => byGrade.value[grade.value] || [])

function usable(g) {
  return (byGrade.value[g] || []).some(hasStudents)
}

function pickGrade(g) {
  grade.value = g
  emit('update:grade', g)
  const first = letters.value.find(hasStudents)
  if (first && first.id !== props.modelValue)
    emit('update:modelValue', first.id)
}

// the selected class decides which parallel is open
watch(
  () => [props.modelValue, props.classes],
  () => {
    const current = props.classes.find(c => c.id === props.modelValue)
    if (current && current.gradeNumber !== grade.value) {
      grade.value = current.gradeNumber
      emit('update:grade', grade.value)
    }
  },
  { immediate: true }
)
</script>

<style scoped>
.cp-row {
  min-width: 0;
}

.cp-scroll {
  flex-wrap: wrap;
  min-width: 0;
}

.cp-label {
  width: 40px;
  flex: none;
  font-weight: 600;
  color: var(--brand-text-muted);
}

.cp-grade {
  min-width: 38px;
  font-weight: 700;
  background: var(--surface-2);
  color: var(--text-primary);
  border: 1px solid var(--brand-border);
}

.cp-grade--on {
  background: var(--color-brand);
  color: #fff;
  border-color: var(--color-brand);
}

.cp-letter {
  font: inherit;
  font-weight: 600;
  font-size: var(--text-sm, 13px);
  padding: 4px 14px;
  border-radius: 999px;
  border: 1.5px solid var(--color-brand);
  background: transparent;
  color: var(--color-brand);
  cursor: pointer;
  transition:
    background 0.15s,
    color 0.15s;
}

.body--dark .cp-letter {
  color: #a5b4fc;
  border-color: #818cf8;
}

.cp-letter:hover:not(:disabled) {
  background: rgba(79, 70, 229, 0.08);
}

.cp-letter--on,
.body--dark .cp-letter--on {
  background: var(--color-brand);
  border-color: var(--color-brand);
  color: #fff;
}

.cp-letter:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.cp-letter:focus-visible {
  outline: 2px solid var(--color-brand);
  outline-offset: 2px;
}
</style>

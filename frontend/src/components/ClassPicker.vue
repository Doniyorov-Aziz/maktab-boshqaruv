<template>
  <!-- parallel first (1…11), then its letters (1-A, 1-B) — shared by attendance, the journal and others -->
  <div class="cp" :class="{ 'cp--compact': compact }" data-class-picker>
    <div class="cp-row">
      <span v-if="!compact" class="cp-label">Sinf:</span>
      <div class="cp-list" role="group" aria-label="Parallel">
        <button
          v-if="allowAll"
          type="button"
          class="cp-grade cp-all"
          :class="{ 'cp-grade--on': modelValue == null }"
          :aria-pressed="modelValue == null"
          data-class-all
          @click="pickAll"
        >
          Barchasi
        </button>
        <button
          v-for="g in GRADES"
          :key="g"
          type="button"
          class="cp-grade"
          :class="{ 'cp-grade--on': g === grade }"
          :disabled="!usable(g)"
          :aria-pressed="g === grade"
          :aria-label="`${g}-sinflar`"
          :title="usable(g) ? '' : `${g}-sinflarda o'quvchi yo'q`"
          :data-grade="g"
          @click="pickGrade(g)"
        >
          {{ g }}
          <span
            v-if="gradeState(g)"
            class="cp-dot"
            :class="`cp-dot--${gradeState(g)}`"
          />
        </button>
      </div>
      <span
        v-if="compact && letters.length"
        class="cp-sep"
        aria-hidden="true"
      />
      <div
        v-if="compact && letters.length"
        class="cp-list"
        role="group"
        aria-label="Sinf"
      >
        <button
          v-for="c in letters"
          :key="c.id"
          type="button"
          class="cp-letter"
          :class="{ 'cp-letter--on': c.id === modelValue }"
          :disabled="!hasStudents(c)"
          :aria-pressed="c.id === modelValue"
          :title="hasStudents(c) ? stateTitle(c.id) : 'Sinfda o\'quvchi yo\'q'"
          :data-class="c.id"
          @click="emit('update:modelValue', c.id)"
        >
          {{ c.gradeNumber }}-{{ c.sectionLetter }}
          <span
            v-if="states[c.id]"
            class="cp-dot"
            :class="`cp-dot--${states[c.id]}`"
          />
        </button>
      </div>
    </div>
    <div v-if="!compact && letters.length" class="cp-row">
      <span class="cp-label"></span>
      <div class="cp-list" role="group" aria-label="Sinf">
        <button
          v-for="c in letters"
          :key="c.id"
          type="button"
          class="cp-letter"
          :class="{ 'cp-letter--on': c.id === modelValue }"
          :disabled="!hasStudents(c)"
          :aria-pressed="c.id === modelValue"
          :title="hasStudents(c) ? stateTitle(c.id) : 'Sinfda o\'quvchi yo\'q'"
          :data-class="c.id"
          @click="emit('update:modelValue', c.id)"
        >
          {{ c.gradeNumber }}-{{ c.sectionLetter }}
          <span
            v-if="states[c.id]"
            class="cp-dot"
            :class="`cp-dot--${states[c.id]}`"
          />
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
// The class picker: the chosen parallel and class are filled in the brand colour, the others
// are see-through with a faint edge, a parallel without students is faded and cannot be
// pressed. compact: parallels and letters in one row. states (classId → FULL / PARTIAL /
// NONE): a small dot — today's attendance taken (green), partly (amber), not yet (grey).
import { ref, computed, watch } from 'vue'

const props = defineProps({
  /** /api/school-classes rows: { id, gradeNumber, sectionLetter, studentCount } */
  classes: { type: Array, default: () => [] },
  modelValue: { type: Number, default: null },
  compact: { type: Boolean, default: false },
  states: { type: Object, default: () => ({}) },
  // a filter: a "Barchasi" button first (no class chosen = every class)
  allowAll: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue', 'update:grade'])

const GRADES = Array.from({ length: 11 }, (_, i) => i + 1)
const grade = ref(null)
const STATE_TITLE = {
  FULL: 'Davomat olingan',
  PARTIAL: 'Davomat qisman olingan',
  NONE: 'Davomat olinmagan'
}

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

/** A parallel's dot: the worst state of its classes (only when states are given). */
function gradeState(g) {
  const list = (byGrade.value[g] || [])
    .map(c => props.states[c.id])
    .filter(Boolean)
  if (!list.length) return null
  if (list.every(s => s === 'FULL')) return 'FULL'
  if (list.some(s => s !== 'NONE')) return 'PARTIAL'
  return 'NONE'
}

function stateTitle(id) {
  return STATE_TITLE[props.states[id]] || ''
}

function pickAll() {
  grade.value = null
  emit('update:grade', null)
  if (props.modelValue != null) emit('update:modelValue', null)
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
    if (props.allowAll && props.modelValue == null) grade.value = null
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
.cp {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  min-width: 0;
}

.cp-row {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  min-width: 0;
}

.cp--compact .cp-row {
  flex-wrap: nowrap;
  overflow-x: auto;
  scrollbar-width: thin;
  /* room for the status dots, which sit over the corner */
  padding: var(--space-1) var(--space-1) var(--hairline);
}

.cp-list {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-1);
  min-width: 0;
}

.cp--compact .cp-list {
  flex-wrap: nowrap;
}

.cp-label {
  width: var(--space-7);
  flex: none;
  font-weight: 600;
  color: var(--brand-text-muted);
}

.cp-sep {
  flex: none;
  width: var(--hairline);
  height: var(--space-5);
  background: var(--dash-edge);
}

.cp-grade,
.cp-letter {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: none;
  min-width: var(--control-h-sm);
  height: var(--control-h-sm);
  padding: 0 var(--space-3);
  border: 1px solid var(--c-border-control);
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--text-primary);
  font: inherit;
  font-size: var(--text-sm);
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  cursor: pointer;
  transition:
    background var(--dur-fast) var(--ease-out),
    border-color var(--dur-fast) var(--ease-out),
    color var(--dur-fast) var(--ease-out);
}

.cp-grade {
  padding: 0 var(--space-2);
}

.cp-grade.cp-all {
  padding: 0 var(--space-3);
}

.cp-grade:hover:not(:disabled),
.cp-letter:hover:not(:disabled) {
  border-color: var(--c-primary);
  background: color-mix(in srgb, var(--c-primary) 8%, transparent);
}

.cp-grade--on,
.cp-letter--on,
.cp-grade--on:hover:not(:disabled),
.cp-letter--on:hover:not(:disabled) {
  background: var(--c-primary);
  border-color: var(--c-primary);
  color: white;
}

.cp-grade:disabled,
.cp-letter:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

.cp-grade:focus-visible,
.cp-letter:focus-visible {
  outline: var(--border-focus);
  outline-offset: var(--hairline);
}

.cp-dot {
  position: absolute;
  top: calc(var(--space-1) * -1);
  right: calc(var(--space-1) * -1);
  width: var(--dot);
  height: var(--dot);
  border-radius: var(--radius-pill);
  box-shadow: 0 0 0 2px var(--card-bg);
}

.cp-dot--FULL {
  background: var(--st-present);
}

.cp-dot--PARTIAL {
  background: var(--gr-3);
}

.cp-dot--NONE {
  background: var(--st-unmarked);
}

@media (prefers-reduced-motion: reduce) {
  .cp-grade,
  .cp-letter {
    transition: none;
  }
}
</style>

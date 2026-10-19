<template>
  <!-- mounted only with its cell: the menu reads its anchor when it is created -->
  <q-menu
    v-if="pop"
    :key="`${pop.r}|${pop.c}|${pop.grade?.id || ''}`"
    model-value
    :target="pop.el"
    no-parent-event
    anchor="bottom middle"
    self="top middle"
    :offset="[0, 6]"
    @update:model-value="v => !v && emit('close')"
    @keydown="onKey"
  >
    <div v-if="pop" class="gp" data-grade-popover>
      <div class="gp__head">
        <AvatarInitials
          :name="pop.student.fullName"
          :id="pop.student.id"
          size="xs"
        />
        <b class="ellipsis">{{ pop.student.fullName }}</b>
        <span class="gp__date">{{ dateLabel }}</span>
      </div>
      <div v-if="pop.absent" class="gp__absent" data-absent-hint>
        <q-icon name="event_busy" /> Bu kuni darsda
        {{ pop.absent === 'EXCUSED' ? 'sababli ' : '' }}yo'q edi
      </div>
      <div class="gp__grades" role="group" aria-label="Baho">
        <button
          v-for="v in GRADE_VALUES"
          :key="v"
          type="button"
          class="gp__grade"
          :class="{ 'gp__grade--on': pop.grade?.value === v }"
          :style="{ '--g': GRADE_COLOR[v] }"
          :data-pick="v"
          @click="choose(v)"
        >
          {{ v }}
        </button>
      </div>
      <SegTabs
        v-model="type"
        label="Baho turi"
        :options="TYPES"
        class="gp__type"
      />
      <AppInput
        v-model="comment"
        dense
        placeholder="Izoh (ixtiyoriy)"
        aria-label="Izoh"
        data-grade-comment
      />
      <div class="gp__foot">
        <span class="gp__hint"
          ><kbd>2</kbd>–<kbd>5</kbd> tanlash · <kbd>Esc</kbd> yopish</span
        >
        <AppButton
          v-if="pop.grade"
          variant="danger"
          size="sm"
          icon="delete_outline"
          label="O'chirish"
          data-grade-delete
          @click="emit('remove', pop.grade)"
        />
      </div>
      <div v-if="pop.grade?.createdBy" class="gp__meta">
        Qo'ydi: {{ pop.grade.createdBy
        }}<template v-if="pop.grade.source === 'DEMO_SIM'"> · 🤖 demo</template>
      </div>
    </div>
  </q-menu>
</template>

<script setup>
// The window of one cell: four big grade buttons (2–5, or the keys), the type (current / test /
// term) and a comment; an existing grade can be changed or deleted. When attendance says the
// student was not at that day's lesson, it says so (the grade can still be given).
import { ref, watch, computed } from 'vue'
import AvatarInitials from '@/components/pages/AvatarInitials.vue'
import SegTabs from '@/components/dashboard/SegTabs.vue'
import AppButton from '@/components/app/AppButton.vue'
import AppInput from '@/components/app/AppInput.vue'
import { GRADE_VALUES, GRADE_COLOR, TYPES } from './journal'
import { dayLabel } from '@/components/attendance/attendance'

const props = defineProps({
  // { el, student, day, grade, absent }
  pop: { type: Object, default: null }
})
const emit = defineEmits(['choose', 'remove', 'close'])

const type = ref('CURRENT')
const comment = ref('')
watch(
  () => props.pop,
  p => {
    type.value = p?.grade?.type || 'CURRENT'
    comment.value = p?.grade?.comment || ''
  },
  { immediate: true }
)
const dateLabel = computed(() =>
  props.pop ? dayLabel(props.pop.day.date) : ''
)

function choose(v) {
  emit('choose', { value: v, type: type.value, comment: comment.value.trim() })
}

function onKey(e) {
  if (e.target.tagName === 'INPUT') {
    if (e.key === 'Enter' && props.pop?.grade) choose(props.pop.grade.value)
    return
  }
  if (['2', '3', '4', '5'].includes(e.key)) {
    e.preventDefault()
    choose(Number(e.key))
  }
}
</script>

<style scoped>
.gp {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  width: var(--field-w-lg);
  padding: var(--space-4);
}

.gp__head {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  min-width: 0;
  font-size: var(--text-sm);
}

.gp__date {
  margin-left: auto;
  flex: none;
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
}

.gp__absent {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  padding: var(--space-1) var(--space-2);
  border-radius: var(--radius-sm);
  background: color-mix(in srgb, var(--st-absent) 12%, transparent);
  color: var(--st-absent);
  font-size: var(--text-xs);
  font-weight: 600;
}

.gp__grades {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-2);
}

.gp__grade {
  height: var(--gb-grade-btn);
  border: 1px solid color-mix(in srgb, var(--g) 40%, transparent);
  border-radius: var(--radius-md);
  background: color-mix(in srgb, var(--g) 14%, transparent);
  color: var(--g);
  font: inherit;
  font-size: var(--text-xl);
  font-weight: 800;
  cursor: pointer;
  transition:
    background var(--dur-fast) var(--ease-out),
    transform var(--dur-fast) var(--ease-out);
}

.gp__grade:hover,
.gp__grade:focus-visible {
  background: color-mix(in srgb, var(--g) 26%, transparent);
  outline: none;
  transform: translateY(calc(var(--hairline) * -1));
}

.gp__grade--on {
  background: var(--g);
  color: white;
}

.gp__type {
  align-self: flex-start;
}

.gp__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);
}

.gp__hint,
.gp__meta {
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
}

kbd {
  display: inline-block;
  min-width: var(--space-4);
  padding: 0 var(--hairline);
  border: 1px solid var(--c-border-control);
  border-radius: var(--radius-sm);
  font: inherit;
  text-align: center;
}

@media (prefers-reduced-motion: reduce) {
  .gp__grade {
    transition: none;
  }
}
</style>

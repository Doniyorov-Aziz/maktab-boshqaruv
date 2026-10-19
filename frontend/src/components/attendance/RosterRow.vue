<template>
  <div
    class="rr"
    :class="[
      row.status ? `rr--${row.status.toLowerCase()}` : 'rr--none',
      { 'rr--active': active, 'rr--selected': selected }
    ]"
    role="row"
    :aria-selected="selected"
    :data-roster-row="row.studentId"
    :data-row-status="row.status || 'NONE'"
    @click="emit('focus-row', $event)"
  >
    <span class="rr__n">{{ n }}</span>
    <AvatarInitials :name="row.name" :id="row.studentId" size="sm" />
    <div class="rr__who">
      <div class="rr__name">
        <span class="ellipsis">{{ row.name }}</span>
        <span
          v-if="auto"
          class="rr__auto"
          :title="autoTitle"
          :aria-label="autoTitle"
          data-auto-dot
          >🤖</span
        >
        <span
          v-if="row.status === 'LATE' && row.lateMinutes"
          class="rr__late-min"
        >
          {{ row.lateMinutes >= 20 ? '20+' : row.lateMinutes }} daq
        </span>
        <span
          v-if="row.status === 'EXCUSED' && row.comment"
          class="rr__reason ellipsis"
        >
          {{ row.comment }}
        </span>
      </div>
      <div class="rr__guardian ellipsis">
        <template v-if="row.guardianName">{{
          guardianShort(row.guardianName, row.guardianRelation)
        }}</template>
        <template v-if="row.guardianPhone">
          · {{ formatPhone(row.guardianPhone) }}</template
        >
        <q-icon
          v-if="row.telegram"
          name="send"
          class="rr__tg"
          title="Telegramga ulangan"
        />
      </div>
    </div>
    <div class="rr__warn">
      <span
        v-if="row.absentStreak >= 2"
        class="rr__chip rr__chip--absent"
        data-warn="streak"
      >
        <q-icon name="warning" /> {{ row.absentStreak }} kun ketma-ket kelmagan
      </span>
      <span
        v-if="row.lateThisWeek >= 2"
        class="rr__chip rr__chip--late"
        data-warn="late"
      >
        <q-icon name="schedule" /> Bu hafta {{ row.lateThisWeek }} marta
        kechikdi
      </span>
    </div>
    <div
      class="rr__hist"
      :aria-label="histLabel"
      :title="histLabel"
      data-history
    >
      <span
        v-for="h in row.history"
        :key="h.date"
        class="rr__dot"
        :style="{
          background: h.status ? STATUS[h.status].color : 'var(--tile-none)'
        }"
      />
    </div>
    <StatusSegment
      :model-value="row.status"
      :name="row.name"
      :readonly="readonly"
      @pick="(v, el) => emit('pick', v, el)"
    />
  </div>
</template>

<script setup>
// One student in the list: number, avatar, name with the parent and phone under it, warnings
// (absent several days in a row, late again this week), the last ten school days as dots and
// the four status buttons. A 3 px stripe on the left and a light tint show the status at a glance.
import { computed } from 'vue'
import AvatarInitials from '@/components/pages/AvatarInitials.vue'
import StatusSegment from './StatusSegment.vue'
import { STATUS } from './attendance'
import { guardianShort } from '@/utils/guardian'
import { formatPhone } from '@/utils/tashkent'

const props = defineProps({
  row: { type: Object, required: true },
  n: { type: Number, required: true },
  active: { type: Boolean, default: false },
  selected: { type: Boolean, default: false },
  readonly: { type: Boolean, default: false },
  // the row has a change not saved yet: the 🤖 mark goes away at once
  dirty: { type: Boolean, default: false }
})
const emit = defineEmits(['pick', 'focus-row'])

const auto = computed(
  () =>
    !props.dirty &&
    (props.row.source === 'AUTO_CARRY' || props.row.source === 'DEMO_SIM')
)
const autoTitle = computed(() => {
  const at = props.row.autoAt
    ? `, ${String(props.row.autoAt).split('T')[1].slice(0, 5)}`
    : ''
  return `Avtomatik: ${props.row.autoNote || "oldingi darsdan ko'chirildi"}${at}`
})
const histLabel = computed(() => {
  const h = props.row.history || []
  const absent = h.filter(x => x.status === 'ABSENT').length
  const late = h.filter(x => x.status === 'LATE').length
  return `Oxirgi ${h.length} kun: ${absent} kelmagan, ${late} kechikkan`
})
</script>

<style scoped>
.rr {
  --stripe: transparent;
  display: grid;
  grid-template-columns: var(--space-6) auto minmax(0, 1fr) auto auto auto;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-2) var(--space-4) var(--space-2) var(--space-3);
  border-left: var(--row-stripe) solid var(--stripe);
  border-bottom: 1px solid var(--dash-edge);
  cursor: default;
  user-select: none;
  transition: background var(--dur-fast) var(--ease-out);
}

.rr--present {
  --stripe: var(--st-present);
}

.rr--absent {
  --stripe: var(--st-absent);
  background: color-mix(in srgb, var(--st-absent) 7%, transparent);
}

.rr--late {
  --stripe: var(--st-late);
  background: color-mix(in srgb, var(--st-late) 7%, transparent);
}

.rr--excused {
  --stripe: var(--st-excused);
}

.rr--none {
  --stripe: var(--st-unmarked);
}

.rr:hover {
  background: color-mix(in srgb, var(--c-primary) 4%, transparent);
}

.rr--active {
  box-shadow: inset 0 0 0 2px
    color-mix(in srgb, var(--c-primary) 55%, transparent);
}

.rr--selected {
  background: color-mix(in srgb, var(--c-primary) 12%, transparent);
}

.rr__n {
  font-size: var(--text-xs);
  font-weight: 700;
  color: var(--brand-text-muted);
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.rr__who {
  min-width: 0;
}

.rr__name {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  min-width: 0;
  font-weight: 600;
}

.rr__auto {
  flex: none;
  font-size: var(--text-xs);
  cursor: help;
}

.rr__late-min,
.rr__reason {
  flex: none;
  max-width: var(--field-w-sm);
  padding: 0 var(--space-2);
  border-radius: var(--radius-pill);
  font-size: var(--text-xs);
  font-weight: 600;
}

.rr__late-min {
  background: color-mix(in srgb, var(--st-late) 14%, transparent);
  color: var(--st-late);
}

.rr__reason {
  flex: 0 1 auto;
  background: color-mix(in srgb, var(--st-excused) 14%, transparent);
  color: var(--st-excused);
}

.rr__guardian {
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
  font-variant-numeric: tabular-nums;
}

.rr__tg {
  margin-left: var(--space-1);
  color: var(--c-primary);
}

.rr__warn {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: var(--space-1);
}

.rr__chip {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  padding: var(--hairline) var(--space-2);
  border-radius: var(--radius-pill);
  font-size: var(--text-xs);
  font-weight: 600;
  white-space: nowrap;
}

.rr__chip--absent {
  background: color-mix(in srgb, var(--st-absent) 14%, transparent);
  color: var(--st-absent);
}

.rr__chip--late {
  background: color-mix(in srgb, var(--st-late) 14%, transparent);
  color: var(--st-late);
}

.rr__hist {
  display: flex;
  gap: var(--space-1);
}

.rr__dot {
  width: var(--hist-dot);
  height: var(--hist-dot);
  border-radius: var(--radius-pill);
}

@media (max-width: 1365px) {
  .rr__warn .rr__chip {
    max-width: var(--field-w-sm);
    overflow: hidden;
    text-overflow: ellipsis;
  }
}

@media (max-width: 1023px) {
  .rr {
    grid-template-columns: var(--space-5) auto minmax(0, 1fr) auto;
  }

  .rr__warn {
    grid-column: 3 / -1;
    grid-row: 2;
    justify-content: flex-start;
  }

  .rr__warn:empty {
    display: none;
  }

  .rr__hist {
    display: none;
  }
}

@media (prefers-reduced-motion: reduce) {
  .rr {
    transition: none;
  }
}
</style>

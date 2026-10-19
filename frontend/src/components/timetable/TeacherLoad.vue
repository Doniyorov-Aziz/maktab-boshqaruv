<template>
  <div class="load" data-teacher-load>
    <div class="load__head">
      <span class="load__name">O'qituvchi</span>
      <span v-for="d in WEEKDAY_SHORT" :key="d" class="load__day">{{ d }}</span>
      <span class="load__hours">Soat / hafta</span>
    </div>
    <button
      v-for="t in rows"
      :key="t.id"
      type="button"
      class="load__row"
      :data-teacher="t.id"
      @click="emit('pick', t.id)"
    >
      <span class="load__name">
        <AvatarInitials size="xs" :name="t.name" :id="t.id" />
        <span>{{ t.short }}</span>
      </span>
      <HeatTile
        v-for="(n, k) in t.days"
        :key="k"
        :value="n"
        :max="maxDay"
        :title="`${t.name}, ${WEEKDAYS[k]}: ${n} ta dars`"
      />
      <span class="load__hours">
        <span class="load__bar"
          ><span :style="{ width: `${(t.hours / maxHours) * 100}%` }"
        /></span>
        <b>{{ t.hours.toFixed(1) }}</b>
      </span>
    </button>
  </div>
</template>

<script setup>
// Teachers' load: a heat map (a row per teacher, a column per weekday, the colour = how
// many lessons) and the week's hours as a bar, the busiest first. A row opens the
// teacher's own week.
import { computed } from 'vue'
import HeatTile from '@/components/pages/HeatTile.vue'
import AvatarInitials from '@/components/pages/AvatarInitials.vue'
import { WEEKDAYS, WEEKDAY_SHORT, minutes } from './timetable'

const props = defineProps({
  lessons: { type: Array, required: true }
})
const emit = defineEmits(['pick'])

const rows = computed(() => {
  const m = new Map()
  for (const l of props.lessons) {
    if (!m.has(l.teacherId))
      m.set(l.teacherId, {
        id: l.teacherId,
        name: l.teacher,
        short: l.teacherShort,
        days: [0, 0, 0, 0, 0, 0],
        hours: 0
      })
    const t = m.get(l.teacherId)
    const d = WEEKDAYS.indexOf(l.weekday)
    if (d >= 0) t.days[d]++
    t.hours += (minutes(l.end) - minutes(l.start)) / 60
  }
  return [...m.values()].sort(
    (a, b) => b.hours - a.hours || a.name.localeCompare(b.name)
  )
})
const maxDay = computed(() =>
  Math.max(1, ...rows.value.flatMap(r => r.days))
)
const maxHours = computed(() => Math.max(1, ...rows.value.map(r => r.hours)))
</script>

<style scoped>
.load {
  display: flex;
  flex-direction: column;
  gap: var(--hairline);
  min-width: 0;
}

.load__head,
.load__row {
  display: grid;
  grid-template-columns: var(--heat-name-w) repeat(6, minmax(var(--heat-cell), 1fr)) minmax(
      var(--field-w-sm),
      1.5fr
    );
  gap: var(--space-1);
  align-items: center;
}

.load__head {
  font-size: var(--text-xs);
  font-weight: 700;
  color: var(--brand-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.04em;
  padding: 0 var(--space-2) var(--space-1);
}

.load__day {
  text-align: center;
}

.load__row {
  padding: var(--hairline) var(--space-2);
  border: 0;
  border-radius: var(--radius-sm);
  background: transparent;
  color: inherit;
  font: inherit;
  text-align: left;
  cursor: pointer;
}

.load__row:hover,
.load__row:focus-visible {
  background: var(--surface-2);
  outline: none;
}

.load__name {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  min-width: 0;
  font-size: var(--text-sm);
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.load__hours {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-xs);
  font-variant-numeric: tabular-nums;
}

.load__bar {
  flex: 1 1 auto;
  height: var(--bar-thin);
  border-radius: var(--radius-pill);
  background: var(--tile-none);
  overflow: hidden;
}

.load__bar span {
  display: block;
  height: 100%;
  border-radius: var(--radius-pill);
  background: linear-gradient(90deg, var(--chart-primary), var(--chart-secondary));
}

@media (max-width: 599px) {
  .load__head,
  .load__row {
    grid-template-columns: minmax(0, 1fr) repeat(6, var(--heat-cell)) var(--tile-md);
  }

  .load__bar {
    display: none;
  }
}
</style>

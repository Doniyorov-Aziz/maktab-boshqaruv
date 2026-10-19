<template>
  <div class="tl" data-today-timeline>
    <div class="tl__scale">
      <span class="tl__name" />
      <div class="tl__ticks">
        <span
          v-for="p in periods"
          :key="p.n"
          class="tl__tick"
          :style="{ left: `${pos(p.start)}%`, width: `${len(p.start, p.end)}%` }"
          >{{ p.n }} · {{ hm(p.start) }}</span
        >
      </div>
    </div>
    <div class="tl__body">
      <div v-for="c in classes" :key="c.id" class="tl__row" :data-class-row="c.id">
        <span class="tl__name">{{ c.name }}</span>
        <div class="tl__track">
          <button
            v-for="l in byClass.get(c.id) || []"
            :key="l.id"
            type="button"
            class="tl__chip"
            :class="{
              'tl__chip--past': isToday && nowMinutes >= minutes(l.end),
              'tl__chip--now':
                isToday &&
                nowMinutes >= minutes(l.start) &&
                nowMinutes < minutes(l.end),
              'tl__chip--dim': focusSubject !== null && l.subjectId !== focusSubject
            }"
            :style="{
              left: `${pos(l.start)}%`,
              width: `${len(l.start, l.end)}%`,
              '--c': subjectColor(l.subjectId, l.subjectColor)
            }"
            :title="`${l.subject} · ${l.teacher} · ${roomLabel(l.room)} · ${hm(l.start)}–${hm(l.end)}`"
            :data-lesson="l.id"
            @click="emit('open', l)"
          >
            <span class="tl__subject">{{ l.subject }}</span>
            <span v-if="states[l.id]" class="tl__state">{{
              STATE_ICON[states[l.id]]
            }}</span>
            <span v-if="subs[l.id]" class="tl__sub" title="O'rinbosar">⇄</span>
          </button>
        </div>
      </div>
      <div class="tl__now-layer">
        <span class="tl__name" />
        <div class="tl__now-track">
          <NowLine v-if="isToday" :from="from" :to="to" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
// The whole school's day: a row per class, time running left to right (the day's bell
// periods on the scale), each lesson a chip in its subject's colour placed by its time.
// Today a red "now" line crosses every row and moves each minute; lessons that are over
// fade. ✓ / 🤖 / ⏳ say whether attendance was taken, ⇄ marks a substitute.
import { computed } from 'vue'
import NowLine from '@/components/pages/NowLine.vue'
import { subjectColor } from '@/utils/subjectColor'
import { hm, minutes, roomLabel } from './timetable'

const props = defineProps({
  lessons: { type: Array, required: true },
  classes: { type: Array, required: true },
  periods: { type: Array, required: true },
  isToday: { type: Boolean, default: false },
  nowMinutes: { type: Number, default: 0 },
  focusSubject: { type: Number, default: null },
  states: { type: Object, default: () => ({}) },
  subs: { type: Object, default: () => ({}) }
})
const emit = defineEmits(['open'])

const STATE_ICON = { MANUAL: '✓', AUTO: '🤖', NONE: '⏳' }

const from = computed(() =>
  props.periods.length ? minutes(props.periods[0].start) : 480
)
const to = computed(() =>
  props.periods.length
    ? Math.max(...props.periods.map(p => minutes(p.end)))
    : 840
)
const span = computed(() => Math.max(1, to.value - from.value))
const pos = t => ((minutes(t) - from.value) / span.value) * 100
const len = (a, b) => ((minutes(b) - minutes(a)) / span.value) * 100

const byClass = computed(() => {
  const m = new Map()
  for (const l of props.lessons) {
    if (!m.has(l.classId)) m.set(l.classId, [])
    m.get(l.classId).push(l)
  }
  return m
})
</script>

<style scoped>
.tl {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  min-width: 0;
}

.tl__scale,
.tl__row,
.tl__now-layer {
  display: grid;
  grid-template-columns: var(--tl-name-w) minmax(0, 1fr);
  gap: var(--space-2);
  align-items: center;
}

.tl__ticks {
  position: relative;
  height: var(--space-5);
}

.tl__tick {
  position: absolute;
  top: 0;
  padding-left: var(--space-1);
  border-left: 1px solid var(--dash-edge);
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
  white-space: nowrap;
  overflow: hidden;
  font-variant-numeric: tabular-nums;
}

.tl__body {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: var(--hairline);
}

.tl__name {
  font-size: var(--text-xs);
  font-weight: 700;
  color: var(--text-primary);
  font-variant-numeric: tabular-nums;
}

.tl__track {
  position: relative;
  height: var(--tl-row-h);
  border-radius: var(--radius-sm);
  background: var(--surface-2);
}

.tl__chip {
  position: absolute;
  top: var(--hairline);
  bottom: var(--hairline);
  display: flex;
  align-items: center;
  gap: var(--space-1);
  min-width: 0;
  padding: 0 var(--space-1) 0 var(--space-2);
  border: 0;
  border-left: var(--tt-stripe) solid var(--c);
  border-radius: var(--radius-sm);
  background: color-mix(in srgb, var(--c) var(--lesson-tint-hover), var(--card-bg));
  color: var(--text-primary);
  font: inherit;
  font-size: var(--text-xs);
  font-weight: 600;
  text-align: left;
  overflow: hidden;
  cursor: pointer;
  transition: opacity var(--dur-fast) var(--ease-out);
}

.tl__chip:hover,
.tl__chip:focus-visible {
  outline: none;
  box-shadow: var(--dash-shadow);
}

.tl__chip--past {
  opacity: 0.5;
}

.tl__chip--now {
  box-shadow:
    0 0 0 2px var(--c),
    0 0 14px var(--now-glow);
  z-index: 1;
}

.tl__chip--dim {
  opacity: 0.18;
}

.tl__subject {
  flex: 1 1 auto;
  min-width: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tl__sub {
  color: var(--st-late);
  font-weight: 800;
}

.tl__now-layer {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.tl__now-track {
  position: relative;
  height: 100%;
}

@media (prefers-reduced-motion: reduce) {
  .tl__chip {
    transition: none;
  }
}
</style>

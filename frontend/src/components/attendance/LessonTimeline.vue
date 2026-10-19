<template>
  <div ref="root" class="ltl" data-lesson-timeline>
    <div
      class="ltl__track"
      :style="{ minWidth: `calc(${lessons.length} * var(--ltl-min))` }"
    >
      <button
        v-for="l in lessons"
        :key="l.lessonSlotId"
        type="button"
        class="ltl__seg"
        :class="[
          `ltl__seg--${l.state.toLowerCase()}`,
          {
            'ltl__seg--on': l.lessonSlotId === modelValue,
            'ltl__seg--now': isNow(l),
            'ltl__seg--later': isLater(l)
          }
        ]"
        :style="{
          left: `${pos(l.start)}%`,
          width: `${len(l.start, l.end)}%`,
          '--c': subjectColor(l.subjectId, l.subjectColor)
        }"
        :aria-pressed="l.lessonSlotId === modelValue"
        :aria-label="`${l.n}-dars, ${l.subject}, ${hm(l.start)}–${hm(l.end)}, ${STATE[l.state].label}`"
        :data-timeline-lesson="l.lessonSlotId"
        :data-state="l.state"
        @click="emit('update:modelValue', l.lessonSlotId)"
      >
        <span class="ltl__top">
          <b class="ltl__n">{{ l.n }}</b>
          <span class="ltl__subject">{{ l.subject }}</span>
          <span class="ltl__state" :title="STATE[l.state].label">{{
            STATE[l.state].icon
          }}</span>
        </span>
        <span class="ltl__meta"
          >{{ hm(l.start) }}–{{ hm(l.end) }} · {{ l.teacherShort }}</span
        >
        <span v-if="isNow(l)" class="ltl__now"
          >Hozir · {{ left(l) }} daq qoldi</span
        >
      </button>
      <NowLine v-if="isToday" :from="from" :to="to" :minutes="nowMinutes" />
    </div>
  </div>
</template>

<script setup>
// A class's lessons of the day on a line, each as wide as it is long, breaks as thin gaps:
// number, subject in its colour, time and teacher; ✓ taken by hand, 🤖 automatic, ⏳ not yet.
// The lesson going on now glows and says how long is left; later ones are faded. Today a
// red line marks the present minute.
import { computed, ref, watch, nextTick, onMounted } from 'vue'
import NowLine from '@/components/pages/NowLine.vue'
import { subjectColor } from '@/utils/subjectColor'
import { hm, minutes } from '@/components/timetable/timetable'

const props = defineProps({
  lessons: { type: Array, required: true },
  modelValue: { type: Number, default: null },
  isToday: { type: Boolean, default: false },
  nowMinutes: { type: Number, default: 0 }
})
const emit = defineEmits(['update:modelValue'])

const STATE = {
  MANUAL: { icon: '✓', label: "Qo'lda olingan" },
  PARTIAL: { icon: '◐', label: 'Qisman olingan' },
  AUTO: { icon: '🤖', label: 'Avtomatik' },
  NONE: { icon: '⏳', label: 'Olinmagan' }
}

const from = computed(() =>
  props.lessons.length ? minutes(props.lessons[0].start) : 480
)
const to = computed(() =>
  props.lessons.length
    ? Math.max(...props.lessons.map(l => minutes(l.end)))
    : 840
)
const span = computed(() => Math.max(1, to.value - from.value))
const pos = t => ((minutes(t) - from.value) / span.value) * 100
const len = (a, b) => ((minutes(b) - minutes(a)) / span.value) * 100

const isNow = l =>
  props.isToday &&
  props.nowMinutes >= minutes(l.start) &&
  props.nowMinutes < minutes(l.end)
const isLater = l => props.isToday && props.nowMinutes < minutes(l.start)
// the chosen lesson is always in sight on a narrow screen
const root = ref(null)
function reveal() {
  nextTick(() => {
    const el = root.value?.querySelector('.ltl__seg--on')
    const box = root.value
    if (!el || !box || box.scrollWidth <= box.clientWidth) return
    box.scrollLeft = el.offsetLeft - (box.clientWidth - el.offsetWidth) / 2
  })
}
watch(() => props.modelValue, reveal)
onMounted(reveal)

const left = l => Math.max(1, Math.ceil(minutes(l.end) - props.nowMinutes))
</script>

<style scoped>
.ltl {
  --ltl-min: 150px;
  overflow-x: auto;
  scrollbar-width: thin;
  padding: var(--space-4) 0 var(--space-1);
}

.ltl__track {
  position: relative;
  height: calc(var(--control-h) * 2);
}

.ltl__seg {
  position: absolute;
  top: 0;
  bottom: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: var(--hairline);
  min-width: 0;
  padding: var(--space-1) var(--space-2) var(--space-1) var(--space-3);
  border: 1px solid color-mix(in srgb, var(--c) 25%, var(--dash-edge));
  border-left: var(--tt-stripe) solid var(--c);
  border-radius: var(--radius-md);
  background: color-mix(in srgb, var(--c) var(--lesson-tint), var(--card-bg));
  color: var(--text-primary);
  font: inherit;
  text-align: left;
  cursor: pointer;
  overflow: hidden;
  transition:
    box-shadow var(--dur-fast) var(--ease-out),
    opacity var(--dur-fast) var(--ease-out),
    transform var(--dur-fast) var(--ease-out);
}

.ltl__seg:hover,
.ltl__seg:focus-visible {
  box-shadow: var(--dash-shadow);
  outline: none;
}

.ltl__seg--on {
  background: color-mix(
    in srgb,
    var(--c) var(--lesson-tint-hover),
    var(--card-bg)
  );
  box-shadow:
    0 0 0 2px var(--c-primary),
    var(--dash-shadow);
}

.ltl__seg--now {
  box-shadow:
    0 0 0 2px var(--c),
    0 0 18px var(--now-glow);
}

.ltl__seg--on.ltl__seg--now {
  box-shadow:
    0 0 0 2px var(--c-primary),
    0 0 18px var(--now-glow);
}

.ltl__seg--later {
  opacity: 0.55;
}

.ltl__top {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  min-width: 0;
}

.ltl__n {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: none;
  width: var(--avatar-xs);
  height: var(--avatar-xs);
  border-radius: var(--radius-pill);
  background: var(--c);
  color: white;
  font-size: var(--text-xs);
}

.ltl__subject {
  flex: 1 1 auto;
  min-width: 0;
  font-size: var(--text-sm);
  font-weight: 700;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ltl__state {
  flex: none;
  font-size: var(--text-sm);
}

.ltl__seg--manual .ltl__state {
  color: var(--st-present);
  font-weight: 800;
}

.ltl__seg--none .ltl__state {
  color: var(--st-late);
}

.ltl__seg--partial .ltl__state {
  color: var(--gr-3);
}

.ltl__meta {
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-variant-numeric: tabular-nums;
}

.ltl__now {
  font-size: var(--text-xs);
  font-weight: 700;
  color: var(--c);
  white-space: nowrap;
}

@media (prefers-reduced-motion: reduce) {
  .ltl__seg {
    transition: none;
  }
}
</style>

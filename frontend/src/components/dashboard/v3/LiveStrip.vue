<template>
  <section class="live dash-card" data-widget="live" :aria-busy="loading">
    <header class="live__head">
      <span
        class="live__pulse"
        :class="{ 'live__pulse--on': lesson }"
        aria-hidden="true"
      />
      <h3 class="live__title">{{ headline }}</h3>
      <span v-if="asOf" class="live__asof"
        >{{ formatClock(asOf) }} holatiga</span
      >
      <router-link v-if="!readonly" to="/timetable" class="live__link"
        >Dars jadvali</router-link
      >
    </header>
    <div
      v-if="lesson"
      class="live__progress"
      role="progressbar"
      :aria-valuenow="progress"
      aria-valuemin="0"
      aria-valuemax="100"
    >
      <span :style="{ width: `${progress}%` }" />
    </div>
    <div v-if="loading && !data" class="live__chips">
      <span v-for="i in 6" :key="i" class="skel live__skel" />
    </div>
    <div v-else-if="error && !data" class="live__note" role="alert">{{
      error
    }}</div>
    <div v-else-if="lesson && data.lessons.length" class="live__chips">
      <component
        :is="readonly ? 'span' : 'router-link'"
        v-for="l in data.lessons"
        :key="l.lessonSlotId"
        :to="
          readonly
            ? undefined
            : { path: '/attendance', query: { lessonSlotId: l.lessonSlotId } }
        "
        :class="['lchip', `lchip--${l.state}`]"
        :data-state="l.state"
      >
        <span class="lchip__dot" :aria-label="STATE[l.state]">{{
          MARK[l.state]
        }}</span>
        <b>{{ l.className }}</b>
        <span class="lchip__sep">·</span>
        <span>{{ l.subject }}</span>
        <span class="lchip__sep">·</span>
        <span class="lchip__who">{{ surname(l.teacher) }}</span>
        <q-tooltip anchor="top middle" self="bottom middle" :offset="[0, 6]">
          {{ l.className }} · {{ l.subject }} · {{ l.teacher }}
          <span v-if="l.room"> · {{ roomLabel(l.room) }}</span
          ><br />
          {{ STATE[l.state] }} · {{ l.marked }}/{{ l.students }} belgilangan
        </q-tooltip>
      </component>
    </div>
    <div v-else-if="data" class="live__note">{{ note }}</div>
  </section>
</template>

<script setup>
// 6.9 — the lessons going on now, as a ribbon of chips: class · subject · teacher and
// a status dot (✓ taken, 🤖 automatic, ⏳ not yet). Not taken first. The period and the
// time left are said once, in the header, with a thin progress bar; a break and the
// end of the day have their own line.
import { computed } from 'vue'
import { useWidget } from '@/composables/useWidget'
import { formatClock, roomLabel, formatNumber } from '@/utils/format'

defineProps({ readonly: { type: Boolean, default: false } })
const { data, asOf, loading, error } = useWidget('live-lessons')

const STATE = {
  MANUAL: "Davomat qo'lda olingan",
  AUTO: 'Avtomatik (🤖)',
  NONE: 'Davomat olinmagan'
}
const MARK = { MANUAL: '✓', AUTO: '🤖', NONE: '⏳' }
const hm = t => (t ? String(t).slice(0, 5) : '')
const surname = t => (t ? t.split(' ').slice(-1)[0] : '')

const lesson = computed(() => data.value?.phase === 'LESSON')

const progress = computed(() => {
  const d = data.value
  if (!lesson.value || !d.start || !d.end) return 0
  const toMin = t => {
    const [h, m] = String(t).split(':').map(Number)
    return h * 60 + m
  }
  const total = toMin(d.end) - toMin(d.start)
  const left = d.minutesLeft ?? 0
  return total > 0
    ? Math.max(0, Math.min(100, Math.round(((total - left) * 100) / total)))
    : 0
})

const headline = computed(() => {
  const d = data.value
  if (!d) return 'Hozirgi darslar'
  if (d.phase === 'LESSON')
    return `${d.period}-dars · ${hm(d.start)}–${hm(d.end)} · tugashiga ${d.minutesLeft} daq`
  if (d.phase === 'BREAK')
    return `Tanaffus · ${d.nextPeriod}-dars ${hm(d.nextStart)} da (${d.minutesLeft} daq)`
  if (d.phase === 'BEFORE')
    return `Darslar hali boshlanmagan · 1-dars ${hm(d.nextStart)} da`
  if (d.phase === 'AFTER') return 'Bugungi darslar tugadi'
  if (d.phase === 'DAY_OFF') return 'Bugun dam olish kuni'
  return "Bugun dars yo'q"
})

const note = computed(() => {
  const d = data.value
  if (d?.phase === 'AFTER' && d.summary) {
    const s = d.summary
    return `${formatNumber(s.lessons)} dars: ✓ ${formatNumber(s.taken)} qo'lda · 🤖 ${formatNumber(s.auto)} avtomatik · ⏳ ${formatNumber(s.missing)} olinmagan`
  }
  if (d?.phase === 'BREAK') return 'Hozir dars yo’q — tanaffus.'
  if (d?.phase === 'BEFORE')
    return 'Birinchi dars boshlanganda bu yerda sinflar ko’rinadi.'
  return 'Hozir dars yo’q.'
})
</script>

<style scoped>
.live {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  padding: var(--space-4) var(--space-5);
}

.live__head {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  flex-wrap: wrap;
}

.live__pulse {
  width: var(--legend-dot);
  height: var(--legend-dot);
  border-radius: var(--radius-pill);
  background: var(--st-unmarked);
}

.live__pulse--on {
  background: var(--st-present);
  box-shadow: 0 0 0 0 color-mix(in srgb, var(--st-present) 50%, transparent);
  animation: live-pulse 2s ease-out infinite;
}

@keyframes live-pulse {
  to {
    box-shadow: 0 0 0 var(--space-2) transparent;
  }
}

.live__title {
  margin: 0;
  font-size: var(--text-base);
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.live__asof {
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
}

.live__link {
  margin-left: auto;
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--q-primary);
  text-decoration: none;
}

.live__progress {
  height: var(--accent-bar);
  border-radius: var(--radius-pill);
  background: var(--ring-track);
  overflow: hidden;
}

.live__progress span {
  display: block;
  height: 100%;
  border-radius: var(--radius-pill);
  background: var(--chart-primary);
  transition: width var(--dur-ring) var(--ease-out);
}

.live__chips {
  display: flex;
  gap: var(--space-2);
  overflow-x: auto;
  padding-bottom: var(--space-1);
  scrollbar-width: thin;
}

.lchip {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  flex-shrink: 0;
  padding: var(--space-1) var(--space-3);
  border: 1px solid var(--dash-edge);
  border-radius: var(--radius-pill);
  background: var(--surface-2);
  color: inherit;
  font-size: var(--text-xs);
  text-decoration: none;
  white-space: nowrap;
  transition: border-color var(--dur-fast) var(--ease-out);
}

a.lchip:hover {
  border-color: var(--chart-primary);
}

.lchip--NONE {
  border-color: color-mix(in srgb, var(--st-absent) 45%, transparent);
  background: color-mix(in srgb, var(--st-absent) 8%, transparent);
}

.lchip__dot {
  font-size: var(--text-xs);
}

.lchip--MANUAL .lchip__dot {
  color: var(--st-present);
  font-weight: 700;
}

.lchip__sep {
  color: var(--brand-text-muted);
}

.lchip__who {
  color: var(--brand-text-muted);
}

.live__skel {
  width: calc(var(--space-8) * 4);
  height: var(--control-h-sm);
  border-radius: var(--radius-pill);
  flex-shrink: 0;
}

.live__note {
  font-size: var(--text-sm);
  color: var(--brand-text-muted);
}

@media (prefers-reduced-motion: reduce) {
  .live__pulse--on {
    animation: none;
  }

  .live__progress span {
    transition: none;
  }
}
</style>

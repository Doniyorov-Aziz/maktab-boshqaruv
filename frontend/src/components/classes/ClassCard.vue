<template>
  <article
    class="cc dash-card"
    :class="{ 'cc--picked': picked }"
    :data-class-card="c.id"
    role="link"
    tabindex="0"
    @click="emit('open', c)"
    @keydown.enter="emit('open', c)"
  >
    <header class="cc__head">
      <AvatarInitials
        size="xl"
        square
        gradient
        :id="c.grade"
        :text="c.name"
        :title="`${c.name} sinf`"
      />
      <div class="cc__who">
        <span class="cc__teacher">
          <AvatarInitials
            v-if="c.teacher"
            size="xs"
            :name="c.teacher"
            :id="c.teacherId || 0"
          />
          {{ c.teacher || 'Rahbar tayinlanmagan' }}
        </span>
        <span class="cc__meta">
          <b>{{ c.students }}</b> o'quvchi
          <template v-if="c.room"> · {{ roomLabel(c.room) }}</template>
        </span>
      </div>
      <span
        v-if="c.risk"
        class="cc__risk"
        :title="`${c.risk} o'quvchi xavf ostida`"
        data-risk-badge
      >
        <q-icon name="warning" /> {{ c.risk }}
      </span>
      <q-checkbox
        :model-value="picked"
        dense
        class="cc__pick"
        :aria-label="`${c.name} — solishtirish uchun belgilash`"
        data-compare-pick
        @click.stop
        @update:model-value="emit('pick', c)"
      />
    </header>

    <div class="cc__nums">
      <div
        class="cc__today"
        :title="
          c.today === null
            ? 'Bugun davomat hali olinmagan'
            : `Bugun ${c.today}%`
        "
      >
        <svg viewBox="0 0 44 44" aria-hidden="true">
          <circle cx="22" cy="22" r="18" class="cc__track" />
          <circle
            v-if="arc"
            cx="22"
            cy="22"
            r="18"
            pathLength="100"
            class="cc__arc"
            :style="{
              strokeDasharray: arc.dasharray,
              strokeLinecap: arc.cap,
              stroke: todayColor
            }"
            transform="rotate(-90 22 22)"
          />
        </svg>
        <span>{{ c.today === null ? '—' : Math.round(c.today) }}</span>
        <small>bugun</small>
      </div>
      <div class="cc__stat">
        <small>Sifat</small>
        <b>{{ formatPercent(c.quality) }}</b>
        <DeltaPill
          :value="c.qualityDelta"
          title="Oxirgi 30 kun, oldingi 30 kunga nisbatan"
        />
      </div>
      <div class="cc__stat">
        <small>O'zlashtirish</small>
        <b>{{ formatPercent(c.mastery) }}</b>
        <DeltaPill
          :value="c.masteryDelta"
          title="Oxirgi 30 kun, oldingi 30 kunga nisbatan"
        />
      </div>
    </div>

    <div
      class="cc__tg"
      :title="`Ota-onalar Telegramda: ${formatPercent(c.telegram)}`"
    >
      <span><q-icon name="send" /> Telegram</span>
      <span class="cc__bar"
        ><span :style="{ width: `${c.telegram || 0}%` }"
      /></span>
      <b>{{ formatPercent(c.telegram) }}</b>
    </div>

    <div class="cc__spark">
      <Sparkline
        :values="c.spark"
        :min="60"
        :max="100"
        :label="`${c.name}: davomat, ${c.spark.length} ish kuni`"
      />
      <small
        >davomat · {{ c.spark.length }} ish kuni · o'rtacha
        {{ formatPercent(c.attendance) }}</small
      >
    </div>
  </article>
</template>

<script setup>
// One class as a card: its name on a gradient tile, the class teacher, students and home
// room, today's attendance as a small ring, quality and mastery with their change, parents
// on Telegram, 20 school days of attendance, and a red badge when students are at risk.
// The card opens the class; the box picks it for "Solishtirish".
import { computed } from 'vue'
import AvatarInitials from '@/components/pages/AvatarInitials.vue'
import DeltaPill from '@/components/dashboard/DeltaPill.vue'
import Sparkline from '@/components/dashboard/Sparkline.vue'
import { ringArc } from '@/components/dashboard/ringArc'
import { roomLabel } from '@/components/timetable/timetable'
import { formatPercent } from '@/utils/format'

const props = defineProps({
  c: { type: Object, required: true },
  picked: { type: Boolean, default: false }
})
const emit = defineEmits(['open', 'pick'])

const arc = computed(() => ringArc(props.c.today))
const todayColor = computed(() => {
  const v = props.c.today
  if (v === null) return 'var(--st-unmarked)'
  return v >= 95
    ? 'var(--heat-1)'
    : v >= 90
      ? 'var(--st-present)'
      : v >= 85
        ? 'var(--gr-3)'
        : 'var(--st-absent)'
})
</script>

<style scoped>
.cc {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  padding: var(--space-4);
  cursor: pointer;
}

.cc:focus-visible {
  outline: var(--border-focus);
  outline-offset: var(--hairline);
}

.cc--picked {
  box-shadow:
    0 0 0 2px var(--c-primary),
    var(--dash-shadow-hover);
}

.cc__head {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.cc__who {
  flex: 1 1 auto;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: var(--hairline);
}

.cc__teacher {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  font-size: var(--text-sm);
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.cc__meta {
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
}

.cc__meta b {
  color: var(--text-primary);
}

.cc__risk {
  display: inline-flex;
  align-items: center;
  gap: var(--hairline);
  padding: 0 var(--space-2);
  border-radius: var(--radius-pill);
  background: color-mix(in srgb, var(--st-absent) 15%, transparent);
  color: var(--st-absent);
  font-size: var(--text-xs);
  font-weight: 700;
}

.cc__pick {
  align-self: flex-start;
}

.cc__nums {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) minmax(0, 1fr);
  gap: var(--space-3);
  align-items: center;
}

.cc__today {
  position: relative;
  width: var(--ring-score);
  height: var(--ring-score);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.cc__today svg {
  position: absolute;
  inset: 0;
}

.cc__track {
  fill: none;
  stroke: var(--ring-track);
  stroke-width: 4;
}

.cc__arc {
  fill: none;
  stroke-width: 4;
}

.cc__today span {
  font-size: var(--text-sm);
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  line-height: 1;
}

.cc__today small {
  font-size: calc(var(--text-xs) * 0.8);
  color: var(--brand-text-muted);
}

.cc__stat {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: var(--hairline);
}

.cc__stat small {
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
}

.cc__stat b {
  font-size: var(--text-lg);
  font-variant-numeric: tabular-nums;
  line-height: 1.1;
}

.cc__tg {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: var(--space-2);
  align-items: center;
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
}

.cc__tg b {
  color: var(--text-primary);
}

.cc__bar {
  height: var(--bar-h);
  border-radius: var(--radius-pill);
  background: var(--tile-none);
  overflow: hidden;
}

.cc__bar span {
  display: block;
  height: 100%;
  border-radius: var(--radius-pill);
  background: var(--chart-secondary);
}

.cc__spark small {
  display: block;
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
}
</style>

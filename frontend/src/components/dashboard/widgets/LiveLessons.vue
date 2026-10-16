<template>
  <ChartCard
    title="Hozir ketayotgan darslar"
    :subtitle="headline"
    :as-of="asOf"
    :loading="loading"
    :error="error"
    :data="data"
    @retry="reload"
  >
    <template #tools>
      <router-link to="/timetable" class="live__link">Dars jadvali</router-link>
    </template>
    <div v-if="data" class="live">
      <template v-if="data.phase === 'LESSON'">
        <div class="live__bar" data-testid="live-remaining">
          <span
            ><b>{{ data.period }}-dars</b> · {{ hm(data.start) }}–{{
              hm(data.end)
            }}
            · tugashiga {{ data.minutesLeft }} daq</span
          >
          <q-linear-progress
            :value="progress"
            color="primary"
            track-color="grey-3"
            rounded
            class="live__progress"
          />
        </div>
        <div class="live__grid">
          <component
            :is="readonly ? 'div' : 'router-link'"
            v-for="l in data.lessons"
            :key="l.lessonSlotId"
            :to="
              readonly
                ? undefined
                : {
                    path: '/attendance',
                    query: { lessonSlotId: l.lessonSlotId }
                  }
            "
            class="live__card"
            :data-state="l.state"
          >
            <div class="live__top">
              <b>{{ l.className }}</b>
              <span :class="['live__state', `live__state--${l.state}`]">{{
                STATE[l.state]
              }}</span>
            </div>
            <div class="live__subject ellipsis">{{ l.subject }}</div>
            <div class="live__meta ellipsis"
              >{{ l.teacher }} · {{ roomLabel(l.room) }}</div
            >
          </component>
        </div>
      </template>
      <div v-else class="live__state-line">
        <template v-if="data.phase === 'BREAK'"
          >☕ Tanaffus · keyingi: {{ data.nextPeriod }}-dars
          {{ hm(data.nextStart) }} ({{ data.minutesLeft }} daq)</template
        >
        <template v-else-if="data.phase === 'BEFORE'"
          >🌅 Darslar hali boshlanmagan · 1-dars
          {{ hm(data.nextStart) }}</template
        >
        <template v-else-if="data.phase === 'AFTER'">
          🏁 Bugungi darslar tugadi · {{ data.summary.lessons }} dars: ✓
          {{ data.summary.taken }} qo'lda · 🤖 {{ data.summary.auto }} avtomatik
          · ⏳ {{ data.summary.missing }} olinmagan
        </template>
        <template v-else-if="data.phase === 'DAY_OFF'"
          >🌿 Bugun dam olish kuni</template
        >
        <template v-else>Bugun dars yo'q</template>
      </div>
    </div>
  </ChartCard>
</template>

<script setup>
// 6.4 — the period now, said once in the header, and every lesson in it with whether
// attendance was taken (✓ by hand / 🤖 automatically / ⏳ not yet) — not yet first.
import { computed } from 'vue'
import ChartCard from '../ChartCard.vue'
import { useWidget } from '@/composables/useWidget'
import { roomLabel } from '@/utils/format'

defineProps({ readonly: { type: Boolean, default: false } })
const { data, asOf, loading, error, reload } = useWidget('live-lessons')

const STATE = { MANUAL: "✓ qo'lda", AUTO: '🤖 avtomatik', NONE: '⏳ olinmagan' }
const hm = t => (t ? String(t).slice(0, 5) : '')
const mins = t => {
  const [h, m] = String(t).split(':').map(Number)
  return h * 60 + m
}
const progress = computed(() => {
  const d = data.value
  if (!d || d.phase !== 'LESSON') return 0
  const total = mins(d.end) - mins(d.start)
  return total > 0
    ? Math.min(1, Math.max(0, (total - d.minutesLeft) / total))
    : 0
})
const headline = computed(() => {
  const d = data.value
  if (!d || d.phase !== 'LESSON') return ''
  const none = d.lessons.filter(l => l.state === 'NONE').length
  return `${d.lessons.length} sinf darsda${none ? ` · ⏳ ${none} tasida davomat olinmagan` : ''}`
})
</script>

<style scoped>
.live__link {
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--q-primary);
  text-decoration: none;
}

.live__bar {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  font-size: var(--text-sm);
  margin-bottom: var(--space-3);
}

.live__progress {
  height: var(--space-1);
  border-radius: var(--radius-pill);
}

.live__grid {
  display: grid;
  grid-template-columns: repeat(
    auto-fill,
    minmax(min(200px, 100%), 1fr)
  ); /* ui-allow: card width from the spec */
  gap: var(--space-2);
}

.live__card {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  padding: var(--space-3);
  border: var(--border);
  border-radius: var(--radius-md);
  background: var(--card-bg);
  color: inherit;
  text-decoration: none;
  min-width: 0;
}

a.live__card:hover {
  border-color: var(--q-primary);
}

.live__card[data-state='NONE'] {
  border-color: color-mix(in srgb, var(--st-absent) 45%, transparent);
}

.live__top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--space-2);
}

.live__state {
  font-size: var(--text-xs);
  font-weight: 600;
  border-radius: var(--radius-sm);
  padding: 0 var(--space-2);
  line-height: 1.8;
  white-space: nowrap;
}

.live__state--MANUAL {
  color: var(--st-present);
  background: color-mix(in srgb, var(--st-present) 14%, transparent);
}

.live__state--AUTO {
  color: var(--st-excused);
  background: color-mix(in srgb, var(--st-excused) 14%, transparent);
}

.live__state--NONE {
  color: var(--st-absent);
  background: color-mix(in srgb, var(--st-absent) 14%, transparent);
}

.live__subject {
  font-size: var(--text-sm);
}

.live__meta {
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
}

.live__state-line {
  font-size: var(--text-sm);
  padding: var(--space-3) 0;
}
</style>

<template>
  <section class="today dash-card" data-widget="today" :aria-busy="loading">
    <div v-if="loading && !data" class="today__skel">
      <div class="skel today__skel-ring" />
      <div class="skel today__skel-grid" />
    </div>
    <div v-else-if="error && !data" class="today__error" role="alert">
      {{ error }}
      <AppButton
        variant="secondary"
        size="sm"
        icon="refresh"
        label="Qayta urinish"
        @click="reload"
      />
    </div>
    <template v-else-if="data">
      <div class="today__left">
        <RingChart
          mid
          :parts="ringParts"
          :center="data.rate === null ? '—' : formatPercent(data.rate)"
          caption="keldi"
        />
        <div class="today__facts">
          <div class="today__title">
            Bugungi davomat
            <span class="today__asof">{{ formatClock(asOf) }} holatiga</span>
          </div>
          <ul class="today__stats">
            <li v-for="s in stats" :key="s.key">
              <q-icon :name="s.icon" :style="{ color: s.color }" />
              <b class="dash-num">{{ formatNumber(s.value) }}</b>
              <span>{{ s.label }}</span>
            </li>
          </ul>
          <div class="today__sources">
            Qo'lda {{ formatNumber(data.sources.manual) }} · Avtomatik
            {{ formatNumber(data.sources.auto) }}
            <span v-if="data.ring.unmarked">
              · olinmagan {{ formatNumber(data.ring.unmarked) }}</span
            >
          </div>
        </div>
      </div>

      <div class="today__right">
        <div class="today__grid" :style="{ '--rows': rows }">
          <div v-for="g in 11" :key="g" class="today__col">
            <span class="today__grade">{{ g }}</span>
            <component
              :is="readonly ? 'div' : 'router-link'"
              v-for="t in byGrade[g] || []"
              :key="t.classId"
              :to="readonly ? undefined : tileLink(t)"
              :class="['tile', `tile--${tone(t)}`]"
              :data-class-id="t.classId"
              :data-state="t.state"
            >
              <span class="tile__letter">{{ t.letter }}</span>
              <span class="tile__val">{{ tileText(t) }}</span>
              <q-tooltip
                class="today__tip"
                anchor="top middle"
                self="bottom middle"
                :offset="[0, 6]"
              >
                <b>{{ t.name }}</b>
                <span v-if="t.teacher"> · {{ t.teacher }}</span>
                <br />
                <template v-if="t.marked">
                  Keldi {{ t.present }} · kechikdi {{ t.late }} · kelmadi
                  {{ t.absent }} · sababli {{ t.excused }}<br />
                  {{ t.marked }}/{{ t.students }} belgilangan<span
                    v-if="t.source && t.source !== 'MANUAL'"
                  >
                    · 🤖 avtomatik</span
                  >
                </template>
                <template v-else-if="t.state === 'NO_LESSONS'"
                  >Bugun darsi yo'q</template
                >
                <template v-else>Davomat hali olinmagan</template>
              </q-tooltip>
            </component>
          </div>
        </div>
        <div class="today__legend">
          <span
            ><i class="legend-dot legend-dot--good" />≥
            {{ data.thresholds.good }}%</span
          >
          <span
            ><i class="legend-dot legend-dot--ok" />{{ data.thresholds.ok }}–{{
              data.thresholds.good
            }}</span
          >
          <span
            ><i class="legend-dot legend-dot--warn" />{{
              data.thresholds.warn
            }}–{{ data.thresholds.ok }}</span
          >
          <span
            ><i class="legend-dot legend-dot--bad" />&lt;
            {{ data.thresholds.warn }}</span
          >
          <span><i class="legend-dot legend-dot--partial" />qisman</span>
          <span><i class="legend-dot legend-dot--none" />olinmagan</span>
        </div>
      </div>
    </template>
  </section>
</template>

<script setup>
// 6.2 — today's attendance in one strip (≤ 180 px): the ring and four counts on the
// left, every class on the right — columns are grades 1…11, letters stacked. A soft
// tint says how the class is doing (< 85 % gets a red edge too); a striped tile is not
// taken yet, a grey one is partial (under half marked: "12/25", no colour verdict).
// The tooltip shows only on hover; a click opens the class in the attendance calendar.
import { computed } from 'vue'
import AppButton from '@/components/app/AppButton.vue'
import RingChart from '../RingChart.vue'
import { useWidget } from '@/composables/useWidget'
import { formatPercent, formatNumber, formatClock } from '@/utils/format'

defineProps({ readonly: { type: Boolean, default: false } })
const { data, asOf, loading, error, reload } = useWidget('today-map')

const month = (() => {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
})()

const ringParts = computed(() => {
  const r = data.value?.ring || {}
  return [
    {
      key: 'present',
      label: 'Keldi',
      value: r.present || 0,
      color: 'var(--st-present)'
    },
    {
      key: 'late',
      label: 'Kechikdi',
      value: r.late || 0,
      color: 'var(--st-late)'
    },
    {
      key: 'absent',
      label: 'Kelmadi',
      value: r.absent || 0,
      color: 'var(--st-absent)'
    },
    {
      key: 'excused',
      label: 'Sababli',
      value: r.excused || 0,
      color: 'var(--st-excused)'
    },
    {
      key: 'unmarked',
      label: 'Olinmagan',
      value: r.unmarked || 0,
      color: 'var(--ring-track)'
    }
  ]
})

const stats = computed(() => {
  const r = data.value?.ring || {}
  return [
    {
      key: 'present',
      icon: 'check_circle',
      label: 'keldi',
      value: r.present,
      color: 'var(--st-present)'
    },
    {
      key: 'late',
      icon: 'schedule',
      label: 'kechikdi',
      value: r.late,
      color: 'var(--st-late)'
    },
    {
      key: 'absent',
      icon: 'cancel',
      label: 'kelmadi',
      value: r.absent,
      color: 'var(--st-absent)'
    },
    {
      key: 'excused',
      icon: 'description',
      label: 'sababli',
      value: r.excused,
      color: 'var(--st-excused)'
    }
  ]
})

const byGrade = computed(() => {
  const m = {}
  for (const t of data.value?.classes || []) {
    if (t.grade < 1 || t.grade > 11) continue
    ;(m[t.grade] ||= []).push(t)
  }
  return m
})
const rows = computed(() =>
  Math.max(1, ...Object.values(byGrade.value).map(a => a.length))
)

function tone(t) {
  if (t.state === 'NO_LESSONS') return 'off'
  if (t.state === 'NONE') return 'none'
  if (t.state === 'PARTIAL' || t.rate === null) return 'partial'
  const th = data.value.thresholds
  if (t.rate >= th.good) return 'good'
  if (t.rate >= th.ok) return 'ok'
  if (t.rate >= th.warn) return 'warn'
  return 'bad'
}

function tileText(t) {
  if (t.state === 'NO_LESSONS') return '—'
  if (t.state === 'NONE') return '⏳'
  if (t.state === 'PARTIAL') return `${t.marked}/${t.students}`
  return t.rate === null ? '—' : `${Math.round(t.rate)}%`
}

const tileLink = t => ({
  path: '/attendance/calendar',
  query: { month, class: t.classId }
})
</script>

<style scoped>
.today {
  display: flex;
  gap: var(--space-5);
  padding: var(--space-4) var(--space-5);
  max-height: var(--today-max-h);
  min-height: 0;
  overflow: hidden;
}

.today__left {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  flex-shrink: 0;
}

.today__facts {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  min-width: 0;
}

.today__title {
  font-size: var(--text-base);
  font-weight: 600;
  display: flex;
  align-items: baseline;
  gap: var(--space-2);
  white-space: nowrap;
}

.today__asof {
  font-size: var(--text-xs);
  font-weight: 400;
  color: var(--brand-text-muted);
}

.today__stats {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  gap: var(--space-4);
  font-size: var(--text-sm);
  white-space: nowrap;
}

.today__stats li {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
}

.today__stats span {
  color: var(--brand-text-muted);
}

.today__sources {
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
}

.today__right {
  flex: 1 1 0;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  height: var(--ring-mid);
}

.today__grid {
  flex: 1 1 0;
  min-height: 0;
  display: grid;
  grid-template-columns: repeat(11, minmax(0, 1fr));
  gap: var(--today-tile-gap) var(--space-1);
}

.today__col {
  container-type: inline-size;
  display: grid;
  grid-template-rows: auto repeat(var(--rows), minmax(0, 1fr));
  gap: var(--today-tile-gap);
  min-width: 0;
}

.today__grade {
  font-size: var(--text-xs);
  font-weight: 600;
  color: var(--brand-text-muted);
  text-align: center;
  line-height: 1;
}

.tile {
  --tone: var(--st-unmarked);
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-1);
  min-height: 0;
  border-radius: var(--radius-sm);
  background: color-mix(in srgb, var(--tone) 16%, transparent);
  color: color-mix(in srgb, var(--tone) 80%, var(--text-primary));
  text-decoration: none;
  overflow: hidden;
  white-space: nowrap;
  transition:
    transform var(--dur-fast) var(--ease-out),
    background var(--dur-fast) var(--ease-out);
}

a.tile:hover {
  background: color-mix(in srgb, var(--tone) 26%, transparent);
}

a.tile:focus-visible {
  outline: var(--border-focus);
  outline-offset: var(--hairline);
}

.tile__letter {
  font-size: var(--text-sm);
  font-weight: 700;
}

.tile__val {
  font-size: var(--text-xs);
  font-weight: 500;
  font-variant-numeric: tabular-nums;
  opacity: 0.9;
}

.tile--good {
  --tone: var(--st-present);
}

.tile--ok {
  --tone: color-mix(in srgb, var(--st-present) 55%, var(--gr-3));
}

.tile--warn {
  --tone: var(--gr-3);
}

.tile--bad {
  --tone: var(--st-absent);
  box-shadow: inset var(--accent-bar) 0 0 var(--st-absent);
}

.tile--partial {
  background: var(--tile-none);
  color: var(--brand-text-muted);
}

.tile--none {
  background: repeating-linear-gradient(
    135deg,
    var(--tile-none) 0 var(--space-1),
    var(--tile-stripe) var(--space-1) var(--space-2)
  );
  color: var(--brand-text-muted);
}

.tile--off {
  background: transparent;
  border: 1px dashed var(--dash-edge);
  color: var(--brand-text-muted);
  opacity: 0.6;
}

.today__legend {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-1) var(--space-3);
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
}

.today__legend span {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
}

.legend-dot {
  display: inline-block;
  width: var(--legend-dot);
  height: var(--legend-dot);
  border-radius: var(--radius-pill);
}

.legend-dot--good {
  background: var(--st-present);
}

.legend-dot--ok {
  background: color-mix(in srgb, var(--st-present) 55%, var(--gr-3));
}

.legend-dot--warn {
  background: var(--gr-3);
}

.legend-dot--bad {
  background: var(--st-absent);
}

.legend-dot--partial {
  background: var(--st-unmarked);
}

.legend-dot--none {
  background: repeating-linear-gradient(
    135deg,
    var(--tile-stripe) 0 var(--hairline),
    transparent var(--hairline) var(--space-1)
  );
  border: 1px solid var(--tile-stripe);
}

.today__skel {
  display: flex;
  gap: var(--space-5);
  width: 100%;
}

.today__skel-ring {
  width: var(--ring-mid);
  height: var(--ring-mid);
  border-radius: var(--radius-pill);
  flex-shrink: 0;
}

.today__skel-grid {
  flex: 1 1 auto;
  height: var(--ring-mid);
  border-radius: var(--radius-md);
}

.today__error {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  width: 100%;
  color: var(--brand-text-muted);
}

/* a narrow column shows the letter only (the tooltip still has the number) */
@container (width < 52px) {
  .tile__val {
    display: none;
  }
}

/* narrow: ring and counts above the grid; the strip may grow */
@media (max-width: 1279px) {
  .today {
    flex-direction: column;
    max-height: none;
  }

  .today__right {
    height: auto;
  }

  .today__grid {
    min-height: calc(var(--today-tile-h) * 3);
  }
}

@media (max-width: 599px) {
  .today__left {
    flex-direction: column;
    align-items: flex-start;
  }

  .today__stats {
    flex-wrap: wrap;
    gap: var(--space-2) var(--space-3);
  }

  .tile__val {
    display: none;
  }
}
</style>

<template>
  <ChartCard
    title="Baholar taqsimoti"
    subtitle="Shu hafta va o'tgan hafta"
    :as-of="asOf"
    :loading="loading"
    :error="error"
    :data="data"
    :empty="!!data && !data.some(w => w.total)"
    data-widget="grades"
    @retry="reload"
  >
    <div v-if="data" class="gs">
      <div v-for="w in data" :key="w.label" class="gs__week">
        <div class="gs__label">
          <span>{{ w.label }}</span>
          <small>{{ formatNumber(w.total) }} baho</small>
        </div>
        <div class="gs__bar" role="img" :aria-label="aria(w)">
          <template v-if="w.total">
            <span
              v-for="s in [5, 4, 3, 2]"
              :key="s"
              :class="['gs__seg', `gs__seg--${s}`]"
              :style="{ flexGrow: w.counts[s] || 0 }"
              :title="`«${s}»: ${w.counts[s]} (${share(w, s)})`"
            >
              <em v-if="(w.counts[s] || 0) / w.total >= 0.07">{{
                share(w, s)
              }}</em>
            </span>
          </template>
          <span v-else class="gs__none">Hali baho yo'q</span>
        </div>
      </div>
      <div class="gs__keys">
        <span v-for="s in [5, 4, 3, 2]" :key="s"
          ><i :class="['gs__key', `gs__seg--${s}`]" />«{{ s }}»</span
        >
      </div>
      <dl class="gs__stats">
        <div v-for="m in stats" :key="m.key">
          <dt>{{ m.label }}</dt>
          <dd>
            <b class="dash-num">{{ m.text }}</b>
            <DeltaPill
              :value="m.delta"
              :higher-is-better="m.higherIsBetter"
              :percent="m.percent"
              :digits="m.digits"
              title="O'tgan haftaga nisbatan"
            />
          </dd>
        </div>
      </dl>
    </div>
  </ChartCard>
</template>

<script setup>
// 6.8 — how this week's grades split into 5 / 4 / 3 / 2 against last week's (100 %
// bars), and four small numbers with their change: grades given, average, share of
// fives, share of twos.
import { computed } from 'vue'
import ChartCard from '../ChartCard.vue'
import DeltaPill from '../DeltaPill.vue'
import { useWidget } from '@/composables/useWidget'
import { formatNumber, formatPercent } from '@/utils/format'

const { data, asOf, loading, error, reload } = useWidget('grade-distribution')

const share = (w, s) => `${Math.round(((w.counts[s] || 0) * 100) / w.total)}%`
const aria = w => [5, 4, 3, 2].map(s => `${s}: ${w.counts[s] || 0}`).join(', ')

const diff = (a, b, digits) =>
  a === null || a === undefined || b === null || b === undefined
    ? null
    : Math.round((a - b) * 10 ** digits) / 10 ** digits

const stats = computed(() => {
  const [now, before] = data.value || [{}, {}]
  return [
    {
      key: 'total',
      label: 'Jami baholar',
      text: formatNumber(now.total),
      delta: null,
      digits: 0,
      higherIsBetter: true
    },
    {
      key: 'avg',
      label: "O'rtacha",
      text:
        now.average === null || now.average === undefined
          ? '—'
          : now.average.toFixed(2),
      delta: diff(now.average, before.average, 1),
      digits: 1,
      higherIsBetter: true
    },
    {
      key: 'five',
      label: '«5» ulushi',
      text: formatPercent(now.share5),
      delta: diff(now.share5, before.share5, 1),
      percent: true,
      digits: 1,
      higherIsBetter: true
    },
    {
      key: 'two',
      label: '«2» ulushi',
      text: formatPercent(now.share2),
      delta: diff(now.share2, before.share2, 1),
      percent: true,
      digits: 1,
      higherIsBetter: false
    }
  ]
})
</script>

<style scoped>
.gs {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.gs__week {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.gs__label {
  display: flex;
  justify-content: space-between;
  font-size: var(--text-sm);
  font-weight: 500;
}

.gs__label small {
  color: var(--brand-text-muted);
  font-size: var(--text-xs);
}

.gs__bar {
  display: flex;
  height: var(--stack-h);
  border-radius: var(--radius-sm);
  overflow: hidden;
  background: var(--ring-track);
  gap: var(--hairline);
}

.gs__seg {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 0;
  flex-basis: 0;
  transition: flex-grow var(--dur-ring) var(--ease-out);
}

.gs__seg em {
  font-style: normal;
  font-size: var(--text-xs);
  font-weight: 700;
  color: white;
  font-variant-numeric: tabular-nums;
}

.gs__seg--5 {
  background: var(--gr-5);
}

.gs__seg--4 {
  background: var(--gr-4);
}

.gs__seg--3 {
  background: var(--gr-3);
}

.gs__seg--2 {
  background: var(--gr-2);
}

.gs__none {
  margin: auto;
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
}

.gs__keys {
  display: flex;
  gap: var(--space-3);
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
}

.gs__keys span {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
}

.gs__key {
  display: inline-block;
  width: var(--legend-dot);
  height: var(--legend-dot);
  border-radius: var(--radius-pill);
}

.gs__stats {
  margin: 0;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--space-3);
  padding-top: var(--space-3);
  border-top: 1px solid var(--dash-edge);
}

.gs__stats dt {
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
  white-space: nowrap;
}

.gs__stats dd {
  margin: var(--space-1) 0 0;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: var(--space-1);
}

.gs__stats b {
  font-size: var(--text-lg);
}

@media (max-width: 599px) {
  .gs__stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>

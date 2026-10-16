<template>
  <ChartCard
    title="Baholar qanday taqsimlangan?"
    subtitle="Shu hafta va o'tgan hafta"
    :as-of="asOf"
    :loading="loading"
    :error="error"
    :data="data"
    :empty="!!data && data.every(w => !w.total)"
    empty-text="Bu ikki haftada baho qo'yilmagan"
    @retry="reload"
  >
    <div class="gd">
      <div v-for="w in data || []" :key="w.label" class="gd__week">
        <div class="gd__label">{{ w.label }}</div>
        <div class="gd__bar" role="img" :aria-label="aria(w)">
          <span
            v-for="s in SCORES"
            :key="s"
            v-show="w.counts[s]"
            class="gd__seg"
            :style="{
              width: `${share(w, s)}%`,
              background: `var(--gr-${s})`,
              color: s === 3 ? 'var(--heat-3-text)' : 'white'
            }"
            :title="`${s}: ${w.counts[s]} ta (${formatPercent(share(w, s))})`"
          >
            <template v-if="share(w, s) > 5"
              >{{ Math.round(share(w, s)) }}%</template
            >
          </span>
          <span v-if="!w.total" class="gd__none">baho yo'q</span>
        </div>
        <div class="gd__total">Jami: {{ formatNumber(w.total) }} ta baho</div>
      </div>
      <div class="gd__legend" aria-hidden="true">
        <span v-for="s in SCORES" :key="s"
          ><i :style="{ background: `var(--gr-${s})` }" /> {{ s }}</span
        >
      </div>
    </div>
  </ChartCard>
</template>

<script setup>
// 6.8 — the share of 5 / 4 / 3 / 2 in this week's and last week's grades (100 % bars).
import ChartCard from '../ChartCard.vue'
import { useWidget } from '@/composables/useWidget'
import { formatPercent, formatNumber } from '@/utils/format'

const { data, asOf, loading, error, reload } = useWidget('grade-distribution')
const SCORES = [5, 4, 3, 2]
const share = (w, s) => (w.total ? (w.counts[s] * 100) / w.total : 0)
const aria = w => SCORES.map(s => `${s}: ${w.counts[s]}`).join(', ')
</script>

<style scoped>
.gd {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.gd__label {
  font-size: var(--text-sm);
  font-weight: 600;
  margin-bottom: var(--space-1);
}

.gd__bar {
  display: flex;
  height: var(--control-h-sm);
  border-radius: var(--radius-sm);
  overflow: hidden;
  background: var(--surface-2);
}

.gd__seg {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--text-xs);
  font-weight: 700;
  min-width: 0;
  overflow: hidden;
}

.gd__none {
  margin: auto;
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
}

.gd__total {
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
  margin-top: var(--space-1);
}

.gd__legend {
  display: flex;
  gap: var(--space-3);
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
}

.gd__legend i {
  display: inline-block;
  width: var(--space-3);
  height: var(--space-3);
  border-radius: var(--radius-sm);
  vertical-align: middle;
  margin-right: var(--space-1);
}
</style>

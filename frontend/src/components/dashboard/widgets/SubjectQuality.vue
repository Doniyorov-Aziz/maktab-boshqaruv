<template>
  <ChartCard
    title="Qaysi fanlarda o'zlashtirish va sifat past?"
    :subtitle="subtitle"
    :as-of="asOf"
    :loading="loading"
    :error="error"
    :data="data"
    :empty="!!data && !data.length"
    empty-text="Shu chorakda baho qo'yilmagan"
    scroll
    @retry="reload"
  >
    <div
      class="sq"
      role="table"
      aria-label="Fanlar bo'yicha o'zlashtirish va sifat"
    >
      <div class="sq__legend" aria-hidden="true">
        <span><i class="sq__swatch sq__swatch--m" /> O'zlashtirish (3–5)</span>
        <span><i class="sq__swatch sq__swatch--q" /> Sifat (4–5)</span>
      </div>
      <div
        v-for="s in data || []"
        :key="s.subjectId"
        class="sq__row"
        role="row"
      >
        <div
          class="sq__name ellipsis"
          role="cell"
          :title="`${s.subject} · ${s.total} ta baho`"
          >{{ s.subject }}</div
        >
        <div class="sq__bars" role="cell">
          <div class="sq__track">
            <span
              class="sq__fill sq__fill--m"
              :style="{ width: `${s.mastery ?? 0}%` }"
            />
            <span class="sq__val">{{ formatPercent(s.mastery) }}</span>
          </div>
          <div class="sq__track">
            <span
              class="sq__fill sq__fill--q"
              :style="{ width: `${s.quality ?? 0}%` }"
            />
            <span class="sq__val">{{ formatPercent(s.quality) }}</span>
          </div>
        </div>
      </div>
      <div class="sq__axis" aria-hidden="true"
        ><span>0</span><span>50</span><span>100%</span></div
      >
    </div>
  </ChartCard>
</template>

<script setup>
// 6.7 — every subject this quarter: mastery (3–5) and quality (4–5) side by side on a
// 0–100 % axis, lowest quality first.
import { computed } from 'vue'
import ChartCard from '../ChartCard.vue'
import { useWidget } from '@/composables/useWidget'
import { formatPercent } from '@/utils/format'

const { data, asOf, loading, error, reload } = useWidget('subject-quality')
const subtitle = computed(() => {
  const m = new Date().getMonth() + 1
  const q = m === 9 || m === 10 ? 1 : m === 11 || m === 12 ? 2 : m <= 3 ? 3 : 4
  return `${q}-chorak · eng past sifat tepada`
})
</script>

<style scoped>
.sq__legend {
  display: flex;
  gap: var(--space-4);
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
  margin-bottom: var(--space-2);
}

.sq__swatch {
  display: inline-block;
  width: var(--space-3);
  height: var(--space-2);
  border-radius: var(--radius-sm);
  margin-right: var(--space-1);
  vertical-align: middle;
}

.sq__swatch--m,
.sq__fill--m {
  background: var(--gr-4);
}

.sq__swatch--q,
.sq__fill--q {
  background: var(--gr-5);
}

.sq__row {
  display: grid;
  grid-template-columns: minmax(0, 0.9fr) minmax(0, 2fr);
  gap: var(--space-3);
  align-items: center;
  padding: var(--space-1) 0;
}

.sq__name {
  font-size: var(--text-sm);
}

.sq__bars {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.sq__track {
  position: relative;
  margin: var(--space-1) 0;
  height: var(--bar-thin);
  background: var(--surface-2);
  border-radius: var(--radius-sm);
  overflow: visible;
  margin-right: calc(var(--space-7) + var(--space-2));
}

.sq__fill {
  position: absolute;
  inset: 0 auto 0 0;
  border-radius: var(--radius-sm);
}

.sq__val {
  position: absolute;
  left: calc(100% + var(--space-2));
  top: 50%;
  transform: translateY(-50%);
  font-size: var(--text-xs);
  font-weight: 600;
  line-height: 1;
  white-space: nowrap;
}

.sq__axis {
  display: flex;
  justify-content: space-between;
  margin-left: calc(31% + var(--space-3));
  margin-right: calc(var(--space-7) + var(--space-2));
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
  padding-top: var(--space-1);
}
</style>

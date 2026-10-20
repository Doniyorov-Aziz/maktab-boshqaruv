<template>
  <ChartCard
    v-if="rows.length >= 2"
    title="Maktablarni solishtirish"
    subtitle="Oxirgi 30 kun"
    :data="rows"
    data-schools-compare
  >
    <div class="cmp">
      <div
        v-for="m in METRICS"
        :key="m.key"
        class="cmp__group"
        :data-metric="m.key"
      >
        <span class="cmp__metric">
          <q-icon :name="m.icon" />
          {{ m.label }}
        </span>
        <div class="cmp__bars">
          <div v-for="s in schools" :key="s.id" class="cmp__row">
            <span class="cmp__track">
              <i
                class="cmp__bar"
                :style="{
                  width: `${Math.max(0, Math.min(100, s[m.key] ?? 0))}%`,
                  background: s.color
                }"
              />
            </span>
            <b class="cmp__val tnum">{{ formatPercent(s[m.key]) }}</b>
            <q-tooltip
              >{{ s.name }} · {{ m.label }}:
              {{ formatPercent(s[m.key]) }}</q-tooltip
            >
          </div>
        </div>
      </div>
    </div>
    <template #footer>
      <span class="cmp__legend">
        <span v-for="s in schools" :key="s.id"
          ><i :style="{ background: s.color }" />{{ s.name }}</span
        >
      </span>
    </template>
  </ChartCard>
</template>

<script setup>
// Two schools or more: attendance (mean of 30 school days), quality (4–5), mastery (3–5) and
// parents in Telegram side by side — grouped bars, one colour per school (its card's colour).
import { computed } from 'vue'
import ChartCard from '@/components/dashboard/ChartCard.vue'
import { formatPercent } from '@/utils/format'
import { schoolColor } from './SchoolCard.vue'

const props = defineProps({ rows: { type: Array, required: true } })

const METRICS = [
  { key: 'attendance', label: 'Davomat', icon: 'how_to_reg' },
  { key: 'quality', label: 'Sifat', icon: 'workspace_premium' },
  { key: 'mastery', label: "O'zlashtirish", icon: 'school' },
  { key: 'telegram', label: 'Telegram', icon: 'send' }
]

const mean = xs => {
  const v = (xs || []).filter(x => x != null)
  return v.length
    ? Math.round((v.reduce((a, b) => a + b, 0) / v.length) * 10) / 10
    : null
}

const schools = computed(() =>
  props.rows.map(r => ({
    id: r.id,
    name: r.name,
    color: schoolColor(r),
    attendance: mean(r.card?.run),
    quality: r.card?.quality ?? null,
    mastery: r.card?.mastery ?? null,
    telegram: r.card?.telegram ?? null
  }))
)
</script>

<style scoped>
.cmp {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(260px, 100%), 1fr));
  gap: var(--space-4) var(--space-6);
}

.cmp__group {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.cmp__metric {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  color: var(--brand-text-muted);
  font-size: var(--text-sm);
  font-weight: 600;
}

.cmp__bars {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.cmp__row {
  display: grid;
  grid-template-columns: 1fr auto;
  align-items: center;
  gap: var(--space-2);
}

.cmp__track {
  height: var(--space-3);
  overflow: hidden;
  border-radius: var(--radius-pill);
  background: var(--ring-track);
}

.cmp__bar {
  display: block;
  height: 100%;
  border-radius: var(--radius-pill);
  transition: width var(--dur-slow, 0.6s) var(--ease-out);
}

.cmp__val {
  min-width: 52px;
  font-size: var(--text-sm);
  text-align: right;
}

.cmp__legend {
  display: inline-flex;
  flex-wrap: wrap;
  gap: var(--space-3);
}

.cmp__legend span {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
}

.cmp__legend i {
  display: inline-block;
  width: var(--space-3);
  height: var(--space-2);
  border-radius: 2px;
}
</style>

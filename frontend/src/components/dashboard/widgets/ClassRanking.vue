<template>
  <ChartCard
    title="Qaysi sinflar orqada, qaysilari oldinda?"
    subtitle="Shu oy · o'tgan oyga nisbatan"
    :as-of="asOf"
    :loading="loading"
    :error="error"
    :data="data"
    :empty="!!data && !ranked.length"
    empty-text="Shu oy uchun ma'lumot yo'q"
    :tabs="tabs"
    :tab="tab"
    scroll
    @update:tab="v => (tab = v)"
    @retry="reload"
  >
    <div class="rank">
      <section>
        <h4 class="rank__h">Eng past {{ bottom.length }}</h4>
        <RankRow
          v-for="r in bottom"
          :key="r.classId"
          :row="r"
          :metric="tab"
          :readonly="readonly"
        />
      </section>
      <section v-if="top.length">
        <h4 class="rank__h">Eng yuqori {{ top.length }}</h4>
        <RankRow
          v-for="r in top"
          :key="r.classId"
          :row="r"
          :metric="tab"
          :readonly="readonly"
        />
      </section>
    </div>
    <template #footer>
      <router-link v-if="!readonly" to="/app/school-classes" class="rank__all"
        >Barcha sinflar ({{ ranked.length }})</router-link
      >
      <span v-else>{{ ranked.length }} sinf</span>
    </template>
  </ChartCard>
</template>

<script setup>
// 6.6 — this month's classes by attendance, mastery or quality: the five lowest first
// (that is where help is needed), then the five highest.
import { computed, ref, h } from 'vue'
import { RouterLink } from 'vue-router'
import ChartCard from '../ChartCard.vue'
import DeltaPill from '../DeltaPill.vue'
import { useWidget } from '@/composables/useWidget'
import { formatPercent } from '@/utils/format'

defineProps({ readonly: { type: Boolean, default: false } })
const { data, asOf, loading, error, reload } = useWidget('class-ranking')

const tabs = [
  { label: 'Davomat', value: 'attendance' },
  { label: "O'zlashtirish", value: 'mastery' },
  { label: 'Sifat', value: 'quality' }
]
const tab = ref('attendance')

const ranked = computed(() =>
  (data.value || [])
    .filter(r => r[tab.value] !== null && r[tab.value] !== undefined)
    .sort((a, b) => b[tab.value] - a[tab.value])
    .map((r, i) => ({ ...r, place: i + 1 }))
)
const bottom = computed(() => ranked.value.slice(-5).reverse())
const top = computed(() =>
  ranked.value.length > 5 ? ranked.value.slice(0, 5) : []
)

const MEDAL = { 1: '🥇', 2: '🥈', 3: '🥉' }

/** One row: place (medal for the top 3), class, class teacher, a 0–100 bar with the value, the change. */
const RankRow = props => {
  const r = props.row
  const v = r[props.metric]
  const d = r[`${props.metric}Delta`]
  const color =
    props.metric === 'attendance'
      ? 'var(--st-present)'
      : props.metric === 'mastery'
        ? 'var(--gr-4)'
        : 'var(--gr-5)'
  const inner = [
    h('span', { class: 'rank__place' }, MEDAL[r.place] || `#${r.place}`),
    h('span', { class: 'rank__who' }, [
      h('b', r.name),
      h(
        'span',
        { class: 'rank__teacher ellipsis' },
        r.teacher || 'Sinf rahbari yo‘q'
      )
    ]),
    h(
      'span',
      {
        class: 'rank__bar',
        role: 'img',
        'aria-label': `${r.name}: ${formatPercent(v)}`
      },
      [
        h('span', {
          class: 'rank__fill',
          style: {
            width: `${Math.max(0, Math.min(100, v))}%`,
            background: color
          }
        }),
        h('span', { class: 'rank__value' }, formatPercent(v))
      ]
    ),
    h(DeltaPill, { value: d, higherIsBetter: true, percent: true })
  ]
  return props.readonly
    ? h('div', { class: 'rank__row' }, inner)
    : h(
        RouterLink,
        { class: 'rank__row', to: `/profiles/class/${r.classId}` },
        () => inner
      )
}
RankRow.props = ['row', 'metric', 'readonly']
</script>

<style scoped>
.rank {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.rank__h {
  margin: 0 0 var(--space-1);
  font-size: var(--text-xs);
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--brand-text-muted);
  font-weight: 600;
}

.rank :deep(.rank__row) {
  display: grid;
  grid-template-columns: var(--space-7) minmax(0, 1fr) minmax(0, 1.4fr) auto;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-1) 0;
  color: inherit;
  text-decoration: none;
  font-size: var(--text-sm);
}

.rank :deep(a.rank__row:hover b) {
  color: var(--q-primary);
}

.rank :deep(.rank__place) {
  text-align: center;
  font-weight: 700;
  color: var(--brand-text-muted);
}

.rank :deep(.rank__who) {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.rank :deep(.rank__teacher) {
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
}

.rank :deep(.rank__bar) {
  position: relative;
  height: var(--space-5);
  background: var(--surface-2);
  border-radius: var(--radius-sm);
  overflow: hidden;
}

.rank :deep(.rank__fill) {
  position: absolute;
  inset: 0 auto 0 0;
  border-radius: var(--radius-sm);
  opacity: 0.85;
}

.rank :deep(.rank__value) {
  position: absolute;
  right: var(--space-2);
  top: 50%;
  transform: translateY(-50%);
  font-size: var(--text-xs);
  font-weight: 700;
  color: var(--text-primary);
}

.rank__all {
  font-weight: 600;
  color: var(--q-primary);
  text-decoration: none;
}
</style>

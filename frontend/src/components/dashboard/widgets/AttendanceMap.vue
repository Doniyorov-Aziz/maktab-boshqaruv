<template>
  <ChartCard
    title="Bugun qaysi sinflarda davomat yaxshi, qayerda olinmagan?"
    :as-of="asOf"
    :loading="loading"
    :error="error"
    :data="data"
    @retry="reload"
  >
    <div v-if="data" class="amap">
      <div class="amap__summary">
        <RingChart
          :parts="ringParts"
          :center="data.rate === null ? '—' : formatPercent(data.rate)"
          :caption="data.rate === null ? 'hali olinmagan' : 'kelganlar'"
        />
        <ul class="amap__legend">
          <li v-for="p in ringParts" :key="p.key">
            <span
              class="amap__dot"
              :style="{ background: p.color }"
              aria-hidden="true"
            />
            {{ p.label }} <b>{{ formatNumber(p.value) }}</b>
          </li>
        </ul>
      </div>
      <div class="amap__grades">
        <div v-for="g in grades" :key="g.grade" class="amap__row">
          <div class="amap__grade">{{ g.grade }}</div>
          <div class="amap__tiles">
            <HeatTile
              v-for="t in g.tiles"
              :key="t.classId"
              :tile="t"
              :thresholds="data.thresholds"
              :month="month"
            />
          </div>
        </div>
      </div>
    </div>
    <template v-if="data" #footer>
      <div class="amap__foot">
        <span class="amap__scale">
          <span class="amap__chip heat-1">≥ {{ data.thresholds.good }}%</span>
          <span class="amap__chip heat-2"
            >{{ data.thresholds.ok }}–{{ data.thresholds.good }}</span
          >
          <span class="amap__chip heat-3"
            >{{ data.thresholds.warn }}–{{ data.thresholds.ok }}</span
          >
          <span class="amap__chip heat-4">&lt; {{ data.thresholds.warn }}</span>
          <span class="amap__chip heat-none">⏳ olinmagan</span>
        </span>
        <span class="amap__sources">
          Qo'lda {{ formatNumber(manual) }} · Avtomatik
          {{ formatNumber(data.sources.AUTO_CARRY) }}
          <template v-if="data.sources.DEMO_SIM">
            ·
            <b class="amap__demo"
              >🧪 Demo {{ formatNumber(data.sources.DEMO_SIM) }} ({{
                demoShare
              }})</b
            ></template
          >
          <template v-if="data.sources.DEVICE">
            · Qurilma {{ formatNumber(data.sources.DEVICE) }}</template
          >
        </span>
      </div>
    </template>
  </ChartCard>
</template>

<script setup>
// 6.2 — every class as a tile, grouped by grade; the ring shows the whole school.
import { computed } from 'vue'
import ChartCard from '../ChartCard.vue'
import RingChart from '../RingChart.vue'
import HeatTile from '../HeatTile.vue'
import { useWidget } from '@/composables/useWidget'
import { formatPercent, formatNumber } from '@/utils/format'

const { data, asOf, loading, error, reload } = useWidget('today-map')

const today = new Date()
const month = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}`

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
      color: 'var(--st-unmarked)'
    }
  ]
})

const grades = computed(() => {
  const map = new Map()
  for (const t of data.value?.classes || []) {
    if (!map.has(t.grade)) map.set(t.grade, [])
    map.get(t.grade).push(t)
  }
  return [...map.entries()]
    .sort((a, b) => a[0] - b[0])
    .map(([grade, tiles]) => ({ grade, tiles }))
})

const manual = computed(() => data.value?.sources.MANUAL || 0)
// what part of today's marks the demo simulator made (shown so nobody mistakes it for real data)
const demoShare = computed(() => {
  const src = data.value?.sources || {}
  const total = Object.values(src).reduce((a, b) => a + b, 0)
  return total ? formatPercent((src.DEMO_SIM * 100) / total) : '—'
})
</script>

<style scoped>
.amap {
  display: flex;
  gap: var(--space-5);
  align-items: flex-start;
  flex-wrap: wrap;
}

.amap__summary {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-3);
}

.amap__legend {
  list-style: none;
  margin: 0;
  padding: 0;
  font-size: var(--text-xs);
  display: grid;
  gap: var(--space-1);
}

.amap__legend b {
  font-weight: 700;
  margin-left: var(--space-1);
}

.amap__dot {
  display: inline-block;
  width: var(--dot);
  height: var(--dot);
  border-radius: var(--radius-pill);
  margin-right: var(--space-1);
}

.amap__grades {
  flex: 1 1 0;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.amap__row {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.amap__grade {
  width: var(--space-6);
  flex-shrink: 0;
  font-size: var(--text-sm);
  font-weight: 700;
  color: var(--brand-text-muted);
  text-align: right;
}

.amap__tiles {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.amap__foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--space-2);
  color: var(--brand-text-muted);
  font-size: var(--text-xs);
}

.amap__demo {
  color: var(--gr-3);
}

.amap__scale {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-1);
}

.amap__chip {
  padding: 0 var(--space-2);
  border-radius: var(--radius-sm);
  line-height: 1.8;
  font-weight: 600;
}

.heat-1 {
  background: var(--heat-1);
  color: var(--heat-1-text);
}
.heat-2 {
  background: var(--heat-2);
  color: var(--heat-2-text);
}
.heat-3 {
  background: var(--heat-3);
  color: var(--heat-3-text);
}
.heat-4 {
  background: var(--heat-4);
  color: var(--heat-4-text);
}
.heat-none {
  background: var(--heat-none);
  color: var(--heat-none-text);
}

@media (max-width: 599px) {
  .amap {
    flex-direction: column;
    align-items: stretch;
  }

  .amap__summary {
    flex-direction: row;
    justify-content: center;
  }
}
</style>

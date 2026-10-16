<template>
  <ChartCard
    title="Ota-onalar bot bilan bog'langanmi?"
    :as-of="asOf"
    :loading="loading"
    :error="error"
    :data="data"
    @retry="reload"
  >
    <div v-if="data" class="pt">
      <component
        :is="readonly ? 'div' : 'router-link'"
        :to="readonly ? undefined : '/telegram-parents'"
        class="pt__ring"
      >
        <RingChart
          small
          :parts="[
            {
              key: 'linked',
              label: 'Ulangan',
              value: data.linked,
              color: 'var(--chart-secondary)'
            },
            {
              key: 'not',
              label: 'Ulanmagan',
              value: Math.max(0, data.students - data.linked),
              color: 'var(--st-unmarked)'
            }
          ]"
          :center="formatPercent(data.linkedPct)"
          caption="ulangan"
        />
      </component>
      <dl class="pt__facts">
        <div>
          <dt>Ulangan o'quvchilar</dt>
          <dd
            >{{ formatNumber(data.linked) }} /
            {{ formatNumber(data.students) }}</dd
          >
        </div>
        <div>
          <dt>Shu hafta yuborilgan</dt>
          <dd>{{ formatNumber(data.sentWeek) }}</dd>
        </div>
        <div>
          <dt>Yetkazilgan</dt>
          <dd>{{ formatPercent(data.deliveredPct) }}</dd>
        </div>
        <div v-if="data.bookDownloadsWeek !== null">
          <dt>Kitob yuklashlar (hafta)</dt>
          <dd>{{ formatNumber(data.bookDownloadsWeek) }}</dd>
        </div>
      </dl>
    </div>
  </ChartCard>
</template>

<script setup>
// 6.10 — how many families the school can reach through the bot, and whether messages arrive.
import ChartCard from '../ChartCard.vue'
import RingChart from '../RingChart.vue'
import { useWidget } from '@/composables/useWidget'
import { formatPercent, formatNumber } from '@/utils/format'

defineProps({ readonly: { type: Boolean, default: false } })
const { data, asOf, loading, error, reload } = useWidget('parents-telegram')
</script>

<style scoped>
.pt {
  display: flex;
  gap: var(--space-4);
  align-items: center;
  flex-wrap: wrap;
}

.pt__ring {
  color: inherit;
  text-decoration: none;
}

.pt__facts {
  flex: 1 1 0;
  min-width: 0;
  margin: 0;
  display: grid;
  gap: var(--space-2);
}

.pt__facts div {
  display: flex;
  justify-content: space-between;
  gap: var(--space-2);
  font-size: var(--text-sm);
}

.pt__facts dt {
  color: var(--brand-text-muted);
}

.pt__facts dd {
  margin: 0;
  font-weight: 700;
}
</style>

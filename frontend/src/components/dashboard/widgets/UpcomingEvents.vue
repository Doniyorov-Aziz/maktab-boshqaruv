<template>
  <ChartCard
    title="Yaqin tadbirlar"
    :as-of="asOf"
    :loading="loading"
    :error="error"
    :data="data"
    :empty="!!data && !data.length"
    empty-text="Yaqin kunlarda tadbir yo'q"
    @retry="reload"
  >
    <ul class="ev">
      <li v-for="e in data || []" :key="e.id">
        <component
          :is="readonly ? 'div' : 'router-link'"
          :to="readonly ? undefined : '/calendar'"
          class="ev__row"
        >
          <span
            class="ev__date"
            :style="{
              borderColor: TYPE[e.type]?.color,
              color: TYPE[e.type]?.color
            }"
          >
            <b>{{ Number(e.startDate.slice(8, 10)) }}</b>
            <small>{{ MONTHS[Number(e.startDate.slice(5, 7)) - 1] }}</small>
          </span>
          <span class="ev__text">
            <span class="ev__title ellipsis">{{ e.title }}</span>
            <span class="ev__when">{{
              e.daysLeft === 0 ? 'bugun' : `${e.daysLeft} kundan keyin`
            }}</span>
          </span>
          <span
            class="ev__chip"
            :style="{
              color: TYPE[e.type]?.color,
              background: `color-mix(in srgb, ${TYPE[e.type]?.color} 14%, transparent)`
            }"
          >
            {{ TYPE[e.type]?.label || e.type }}
          </span>
        </component>
      </li>
    </ul>
  </ChartCard>
</template>

<script setup>
// 6.11 — the next five events (date square, name, how soon, type).
import ChartCard from '../ChartCard.vue'
import { useWidget } from '@/composables/useWidget'

defineProps({ readonly: { type: Boolean, default: false } })
const { data, asOf, loading, error, reload } = useWidget('events')
const MONTHS = [
  'yan',
  'fev',
  'mar',
  'apr',
  'may',
  'iyun',
  'iyul',
  'avg',
  'sen',
  'okt',
  'noy',
  'dek'
]
const TYPE = {
  HOLIDAY: { label: 'Bayram', color: 'var(--st-late)' },
  EXAM: { label: 'Imtihon', color: 'var(--st-absent)' },
  PARENT_MEETING: {
    label: "Ota-onalar yig'ilishi",
    color: 'var(--st-excused)'
  },
  VACATION: { label: "Ta'til", color: 'var(--st-present)' },
  OTHER: { label: 'Boshqa', color: 'var(--chart-primary)' }
}
</script>

<style scoped>
.ev {
  list-style: none;
  margin: 0;
  padding: 0;
}

.ev__row {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-2) 0;
  border-bottom: var(--border);
  color: inherit;
  text-decoration: none;
}

.ev li:last-child .ev__row {
  border-bottom: 0;
}

.ev__date {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: var(--tile-md);
  height: var(--tile-md);
  border: var(--hairline) solid;
  border-radius: var(--radius-sm);
  flex-shrink: 0;
  line-height: 1;
}

.ev__date b {
  font-size: var(--text-md);
}

.ev__date small {
  font-size: var(--text-xs);
  text-transform: uppercase;
}

.ev__text {
  flex: 1 1 0;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.ev__title {
  font-size: var(--text-sm);
  font-weight: 600;
}

.ev__when {
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
}

.ev__chip {
  font-size: var(--text-xs);
  font-weight: 600;
  border-radius: var(--radius-sm);
  padding: 0 var(--space-2);
  line-height: 1.8;
  white-space: nowrap;
}
</style>

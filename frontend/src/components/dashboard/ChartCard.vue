<template>
  <section class="chart-card dash-card" :aria-busy="loading">
    <header class="chart-card__head">
      <div class="chart-card__titles">
        <h3 class="chart-card__title">
          {{ title }}
          <slot name="title-extra" />
        </h3>
        <div v-if="subtitle || asOf" class="chart-card__sub">
          <span v-if="subtitle">{{ subtitle }}</span>
          <span v-if="subtitle && asOf"> · </span>
          <span v-if="asOf" class="chart-card__asof"
            >{{ formatClock(asOf) }} holatiga</span
          >
        </div>
      </div>
      <div class="chart-card__tools">
        <SegTabs
          v-if="tabs?.length"
          :model-value="tab"
          :options="tabs"
          :label="title"
          @update:model-value="v => emit('update:tab', v)"
        />
        <slot name="tools" />
      </div>
    </header>

    <div
      class="chart-card__body"
      :class="{ 'chart-card__body--scroll': scroll }"
    >
      <div
        v-if="loading && !hasData"
        :class="['chart-card__skeleton', `chart-card__skeleton--${skeleton}`]"
      >
        <template v-if="skeleton === 'chart'">
          <div class="skel chart-card__skel-chart" />
        </template>
        <template v-else-if="skeleton === 'ring'">
          <div class="skel chart-card__skel-ring" />
          <div class="chart-card__skel-lines">
            <div v-for="i in 4" :key="i" class="skel chart-card__skel-line" />
          </div>
        </template>
        <template v-else>
          <div
            v-for="i in skeletonRows"
            :key="i"
            class="skel chart-card__skel-line"
          />
        </template>
      </div>
      <div v-else-if="error && !hasData" class="chart-card__state" role="alert">
        <q-icon name="cloud_off" class="chart-card__state-icon" />
        <div>{{ error }}</div>
        <AppButton
          variant="secondary"
          size="sm"
          icon="refresh"
          label="Qayta urinish"
          @click="emit('retry')"
        />
      </div>
      <div v-else-if="empty" class="chart-card__state" :data-empty="emptyKind">
        <!-- a green check only when there truly is no problem; no data is neutral -->
        <q-icon
          :name="emptyKind === 'ok' ? 'check_circle' : 'hourglass_empty'"
          :class="[
            'chart-card__state-icon',
            `chart-card__state-icon--${emptyKind}`
          ]"
        />
        <div>{{ shownEmptyText }}</div>
      </div>
      <slot v-else />
    </div>

    <footer v-if="$slots.footer" class="chart-card__foot">
      <slot name="footer" />
    </footer>
  </section>
</template>

<script setup>
// The frame of every home-page widget: a title, when the numbers are from ("09:42
// holatiga"), optional tabs, and the loading (shimmer skeleton in the card's shape),
// error and empty states. "No data yet" is neutral; the green check is only for
// emptyKind="ok" (a list that is empty because nothing is wrong). The body clips its
// content, so a chart can never spill out of the card.
import { computed } from 'vue'
import AppButton from '@/components/app/AppButton.vue'
import SegTabs from './SegTabs.vue'
import { formatClock } from '@/utils/format'
import { useSystemInfo } from '@/composables/useSystemInfo'

const props = defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' },
  asOf: { type: String, default: null },
  loading: { type: Boolean, default: false },
  error: { type: String, default: null },
  empty: { type: Boolean, default: false },
  // 'nodata' (neutral) or 'ok' (nothing is wrong)
  emptyKind: { type: String, default: 'nodata' },
  emptyText: { type: String, default: '' },
  data: { type: null, default: null },
  tabs: { type: Array, default: null },
  tab: { type: String, default: null },
  scroll: { type: Boolean, default: false },
  skeleton: { type: String, default: 'rows' },
  skeletonRows: { type: Number, default: 4 }
})
const emit = defineEmits(['retry', 'update:tab'])
const hasData = computed(() => props.data !== null && props.data !== undefined)
const system = useSystemInfo()
const shownEmptyText = computed(() => {
  if (props.emptyText) return props.emptyText
  if (props.emptyKind === 'ok') return 'Hammasi joyida'
  return system.value.demoSimulator
    ? "Hali ma'lumot yo'q — demo to'ldirilmoqda"
    : "Hali ma'lumot yo'q"
})
</script>

<style scoped>
.chart-card {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: var(--space-4) var(--space-5) var(--space-5);
  gap: var(--space-3);
}

.chart-card__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-3);
  flex-wrap: wrap;
}

.chart-card__titles {
  min-width: 0;
}

.chart-card__title {
  margin: 0;
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-base);
  font-weight: 600;
  line-height: 1.35;
  letter-spacing: -0.01em;
}

.chart-card__sub {
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
  margin-top: var(--space-1);
}

.chart-card__tools {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  flex-wrap: wrap;
}

.chart-card__body {
  flex: 1 1 auto;
  min-height: 0;
  min-width: 0;
  overflow: hidden;
  position: relative;
}

.chart-card__body--scroll {
  overflow-y: auto;
  max-height: var(--widget-body-max-h);
}

@media (min-width: 1024px) {
  .chart-card__body--scroll {
    flex: 1 1 0;
    max-height: none;
    min-height: calc(var(--control-h) * 5);
  }
}

.chart-card__skeleton {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  height: 100%;
}

.chart-card__skeleton--ring {
  flex-direction: row;
  align-items: center;
  gap: var(--space-4);
}

.chart-card__skel-line {
  height: var(--control-h-sm);
}

.chart-card__skel-lines {
  flex: 1 1 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.chart-card__skel-chart {
  flex: 1 1 auto;
  min-height: var(--chart-h);
  border-radius: var(--radius-md);
}

.chart-card__skel-ring {
  width: var(--ring-mid);
  height: var(--ring-mid);
  border-radius: var(--radius-pill);
  flex-shrink: 0;
}

.chart-card__state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  min-height: calc(var(--control-h) * 3);
  height: 100%;
  color: var(--brand-text-muted);
  font-size: var(--text-sm);
  text-align: center;
}

.chart-card__state-icon {
  font-size: var(--text-xl);
  opacity: 0.7;
}

.chart-card__state-icon--ok {
  color: var(--st-present);
  opacity: 1;
}

.chart-card__foot {
  border-top: 1px solid var(--dash-edge);
  padding-top: var(--space-3);
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
}
</style>

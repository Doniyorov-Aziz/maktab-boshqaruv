<template>
  <section class="chart-card brand-card" :aria-busy="loading">
    <header class="chart-card__head">
      <div class="chart-card__titles">
        <h3 class="chart-card__title">{{ title }}</h3>
        <div v-if="subtitle || asOf" class="chart-card__sub">
          <span v-if="subtitle">{{ subtitle }}</span>
          <span v-if="subtitle && asOf"> · </span>
          <span v-if="asOf" class="chart-card__asof"
            >{{ formatClock(asOf) }} holatiga</span
          >
        </div>
      </div>
      <div class="chart-card__tools">
        <q-btn-toggle
          v-if="tabs?.length"
          :model-value="tab"
          :options="tabs"
          no-caps
          unelevated
          toggle-color="primary"
          text-color="grey-7"
          class="app-toggle--sm"
          @update:model-value="v => emit('update:tab', v)"
        />
        <slot name="tools" />
      </div>
    </header>

    <div
      class="chart-card__body"
      :class="{ 'chart-card__body--scroll': scroll }"
    >
      <div v-if="loading && !hasData" class="chart-card__skeleton">
        <q-skeleton
          v-for="i in skeletonRows"
          :key="i"
          type="rect"
          class="chart-card__skeleton-row"
        />
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
      <div v-else-if="empty" class="chart-card__state">
        <div class="chart-card__ok">✅</div>
        <div>{{ emptyText }}</div>
      </div>
      <slot v-else />
    </div>

    <footer v-if="$slots.footer" class="chart-card__foot">
      <slot name="footer" />
    </footer>
  </section>
</template>

<script setup>
// The frame of every home-page widget: a title (a question or a clear name), when the
// numbers are from ("09:42 holatiga"), optional tabs, and the loading / error / empty states.
// The body clips its content (overflow hidden), so a chart can never spill out of the card.
import { computed } from 'vue'
import AppButton from '@/components/app/AppButton.vue'
import { formatClock } from '@/utils/format'

const props = defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' },
  asOf: { type: String, default: null },
  loading: { type: Boolean, default: false },
  error: { type: String, default: null },
  empty: { type: Boolean, default: false },
  emptyText: { type: String, default: 'Hammasi joyida' },
  data: { type: null, default: null },
  tabs: { type: Array, default: null },
  tab: { type: String, default: null },
  scroll: { type: Boolean, default: false },
  skeletonRows: { type: Number, default: 4 }
})
const emit = defineEmits(['retry', 'update:tab'])
const hasData = computed(() => props.data !== null && props.data !== undefined)
</script>

<style scoped>
.chart-card {
  display: flex;
  flex-direction: column;
  min-width: 0;
  height: 100%;
  overflow: hidden;
  padding: var(--space-4);
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
  font-size: var(--text-md);
  font-weight: 600;
  line-height: 1.3;
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

/* on a desktop a long list does not make its row taller: it takes the height the row's
   other widgets give it (rows are equal height) and scrolls inside */
@media (min-width: 1024px) {
  .chart-card__body--scroll {
    flex: 1 1 0;
    max-height: none;
    min-height: calc(var(--control-h) * 6);
  }
}

.chart-card__skeleton {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.chart-card__skeleton-row {
  height: var(--control-h-sm);
  border-radius: var(--radius-sm);
}

.chart-card__state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  min-height: calc(var(--control-h) * 3);
  color: var(--brand-text-muted);
  text-align: center;
}

.chart-card__state-icon {
  font-size: var(--text-xl);
}

.chart-card__ok {
  font-size: var(--text-lg);
}

.chart-card__foot {
  border-top: var(--border);
  padding-top: var(--space-3);
  font-size: var(--text-sm);
}
</style>

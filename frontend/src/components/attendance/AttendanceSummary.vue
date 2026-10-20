<template>
  <section class="as dash-card" data-attendance-summary>
    <RingChart :parts="parts" mid class="as__ring">
      <CountUp
        class="as__rate dash-num"
        :value="rate"
        :format="v => `${Math.round(v)}%`"
      />
      <small>keldi</small>
    </RingChart>

    <div class="as__mid">
      <div class="as__lesson">
        <b>{{ title }}</b>
        <span v-if="subtitle" class="dash-muted">{{ subtitle }}</span>
      </div>
      <div class="as__chips">
        <span
          v-for="s in STATUSES"
          :key="s.value"
          class="as__chip"
          :style="{ '--c': s.color }"
          :data-count="s.value"
        >
          <q-icon :name="s.icon" />
          {{ s.label }}
          <b>{{ c[s.value] }}</b>
        </span>
      </div>
      <div class="as__progress" :title="`${c.marked}/${c.total} belgilandi`">
        <span class="as__bar">
          <span
            v-for="s in STATUSES"
            :key="s.value"
            :style="{
              width: `${c.total ? (c[s.value] / c.total) * 100 : 0}%`,
              background: s.color
            }"
          />
        </span>
        <span class="as__marked" data-marked
          >{{ c.marked }}/{{ c.total }} belgilandi</span
        >
      </div>
      <div class="as__notify" data-notify>
        <q-icon name="send" />
        <template v-if="demo">demo — xabar yuborilmaydi</template>
        <template v-else-if="notify"
          >Saqlangach {{ notify }} ta ota-onaga xabar boradi</template
        >
        <template v-else
          >Ota-onalarga xabar ketmaydi (kelmagan yoki kechikkan yo'q)</template
        >
      </div>
    </div>

    <div class="as__actions">
      <AppInput
        :model-value="query"
        dense
        clearable
        class="as__search"
        placeholder="Qidirish"
        aria-label="O'quvchini qidirish"
        @update:model-value="v => emit('update:query', v || '')"
      >
        <template #prepend><q-icon name="search" /></template>
      </AppInput>
      <AppButton
        v-if="canEdit"
        icon="done_all"
        label="Hammasi keldi"
        data-all-present
        @click="emit('all-present')"
      />
      <AppButton
        v-if="canEdit && auto"
        variant="secondary"
        icon="smart_toy"
        :label="`Avtomatikni tasdiqlash (${auto})`"
        :loading="confirming"
        data-confirm-auto
        @click="emit('confirm-auto')"
      />
    </div>
  </section>
</template>

<script setup>
// The chosen lesson at a glance: a ring with who came (present + late, excused left out — the
// home page's rule), the four counters, how many are marked, whether parents will be told,
// and the quick actions.
import { computed } from 'vue'
import RingChart from '@/components/dashboard/RingChart.vue'
import CountUp from '@/components/dashboard/CountUp.vue'
import AppButton from '@/components/app/AppButton.vue'
import AppInput from '@/components/app/AppInput.vue'
import { STATUSES, presentRate } from './attendance'

const props = defineProps({
  // counts(): { PRESENT, ABSENT, LATE, EXCUSED, marked, total }
  c: { type: Object, required: true },
  title: { type: String, default: '' },
  subtitle: { type: String, default: '' },
  demo: { type: Boolean, default: false },
  notify: { type: Number, default: 0 },
  auto: { type: Number, default: 0 },
  canEdit: { type: Boolean, default: false },
  confirming: { type: Boolean, default: false },
  query: { type: String, default: '' }
})
const emit = defineEmits(['all-present', 'confirm-auto', 'update:query'])

const rate = computed(() => presentRate(props.c))
// the ring is the lesson itself: every status as its part, the not-yet-marked in grey
const parts = computed(() => [
  ...STATUSES.map(st => ({
    key: st.value,
    label: st.label,
    value: props.c[st.value],
    color: st.color
  })),
  {
    key: 'UNMARKED',
    label: 'Belgilanmagan',
    value: Math.max(props.c.total - props.c.marked, 0),
    color: 'var(--st-unmarked)'
  }
])
</script>

<style scoped>
.as {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: var(--space-5);
  padding: var(--space-4) var(--space-5);
}

.as:hover {
  transform: none;
}

.as__rate {
  font-size: var(--text-xl);
}

.as__ring small {
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
}

.as__mid {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  min-width: 0;
}

.as__lesson {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: var(--space-2);
  font-size: var(--text-md);
}

.as__lesson span {
  font-size: var(--text-sm);
}

.as__chips {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.as__chip {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  padding: var(--space-1) var(--space-3);
  border-radius: var(--radius-pill);
  background: color-mix(in srgb, var(--c) 12%, transparent);
  color: var(--text-primary);
  font-size: var(--text-sm);
  font-weight: 600;
}

.as__chip .q-icon {
  color: var(--c);
}

.as__chip b {
  font-variant-numeric: tabular-nums;
  color: var(--c);
}

.as__progress {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.as__bar {
  flex: 1 1 auto;
  display: flex;
  height: var(--bar-h);
  border-radius: var(--radius-pill);
  background: var(--tile-none);
  overflow: hidden;
}

.as__bar span {
  height: 100%;
  transition: width var(--dur) var(--ease-out);
}

.as__marked {
  font-size: var(--text-xs);
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.as__notify {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
}

.as__actions {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: var(--space-2);
  min-width: var(--field-w-sm);
}

@media (max-width: 1023px) {
  .as {
    grid-template-columns: auto minmax(0, 1fr);
  }

  .as__actions {
    grid-column: 1 / -1;
    flex-direction: row;
    flex-wrap: wrap;
  }

  .as__search {
    flex: 1 1 var(--field-w-sm);
  }
}

@media (max-width: 599px) {
  .as {
    grid-template-columns: minmax(0, 1fr);
    justify-items: center;
    gap: var(--space-3);
    padding: var(--space-4);
  }

  .as__mid,
  .as__actions {
    width: 100%;
  }
}

@media (prefers-reduced-motion: reduce) {
  .as__bar span {
    transition: none;
  }
}
</style>

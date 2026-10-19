<template>
  <ChartCard
    :title="title"
    subtitle="Xavf balli 0–100"
    :as-of="asOf"
    :loading="loading"
    :error="error"
    :data="data"
    :empty="!!data && !data.rows.length"
    empty-kind="ok"
    empty-text="Xavf ostidagi o'quvchi yo'q"
    :skeleton-rows="6"
    data-widget="risk"
    @retry="reload"
  >
    <template #title-extra>
      <q-icon name="info" class="risk__info" tabindex="0" aria-label="Formula">
        <q-tooltip max-width="320px">
          Xavf balli = kelmaslik (30 kunda; 6+ kun = 35) + oxirgi 2 hafta
          o'rtacha bahosi (4.0 dan past; 2.5 va past = 30) + pasayish (oldingi 4
          haftaga nisbatan; 1 ball = 20) + «2»lar (30 kunda; 4+ = 15). Ro'yxatda
          40 va undan yuqorisi.
        </q-tooltip>
      </q-icon>
    </template>
    <ul v-if="data" class="risk">
      <li v-for="r in data.rows" :key="r.studentId">
        <component
          :is="readonly ? 'div' : 'router-link'"
          :to="readonly ? undefined : `/profiles/student/${r.studentId}`"
          class="risk__row"
          data-risk-row
        >
          <RiskAvatar :name="r.name" :id="r.studentId" />
          <span class="risk__who">
            <b>{{ r.name }}</b>
            <small>{{ r.className }}</small>
          </span>
          <span class="risk__chips">
            <span v-if="r.absentDays" class="chip chip--abs"
              >{{ r.absentDays }} kun kelmagan</span
            >
            <span v-if="avgChip(r)" class="chip chip--avg">{{
              avgChip(r)
            }}</span>
            <span v-else-if="r.twos" class="chip chip--avg"
              >{{ r.twos }} ta «2»</span
            >
          </span>
          <ScoreRing :value="r.score" :title="`Xavf balli ${r.score}`" />
        </component>
      </li>
    </ul>
    <template v-if="data && data.total > data.rows.length" #footer>
      <button type="button" class="risk__more" data-risk-all @click="openAll">
        Hammasi ({{ data.total }})
        <q-icon name="chevron_right" />
      </button>
      <RiskDrawer
        v-model="allOpen"
        :title="title"
        :source="source"
        :readonly="readonly"
      />
    </template>
  </ChartCard>
</template>

<script setup>
// 6.4 — the students who most need someone now, by risk score (DashboardMetrics.risk on
// the server): absence, a low and falling average, twos. A row opens the student.
import ChartCard from '../ChartCard.vue'
import RiskAvatar from '../RiskAvatar.vue'
import ScoreRing from '../ScoreRing.vue'
import RiskDrawer from './RiskDrawer.vue'
import { ref } from 'vue'
import { useWidget } from '@/composables/useWidget'

const props = defineProps({
  readonly: { type: Boolean, default: false },
  // 'risk' (school) or 'my-risk' (the teacher's classes)
  source: { type: String, default: 'risk' },
  title: { type: String, default: "Xavf ostidagi o'quvchilar" }
})
const { data, asOf, loading, error, reload } = useWidget(props.source, {
  params: { limit: 6 }
})

// "Hammasi": the whole list with its filters in the side panel
const allOpen = ref(false)
function openAll() {
  allOpen.value = true
}

const one = v => (v === null || v === undefined ? null : v.toFixed(1))
function avgChip(r) {
  if (r.avgNow === null || r.avgNow === undefined) return null
  if (
    r.avgBefore !== null &&
    r.avgBefore !== undefined &&
    r.avgBefore - r.avgNow >= 0.3
  )
    return `o'rtacha ${one(r.avgBefore)} → ${one(r.avgNow)}`
  return `o'rtacha ${one(r.avgNow)}`
}
</script>

<style scoped>
.risk {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
}

.risk__row {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-2);
  margin: 0 calc(var(--space-2) * -1);
  border-radius: var(--radius-md);
  color: inherit;
  text-decoration: none;
  transition: background var(--dur-fast) var(--ease-out);
}

a.risk__row:hover {
  background: var(--surface-2);
}

.risk__who {
  display: flex;
  flex-direction: column;
  min-width: 0;
  flex: 1 1 auto;
}

.risk__who b {
  font-size: var(--text-sm);
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.risk__who small {
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
}

.risk__chips {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: var(--space-1);
  flex-shrink: 0;
}

.chip {
  font-size: var(--text-xs);
  font-weight: 600;
  line-height: 1.6;
  padding: 0 var(--space-2);
  border-radius: var(--radius-pill);
  white-space: nowrap;
}

.chip--abs {
  color: var(--st-absent);
  background: color-mix(in srgb, var(--st-absent) 12%, transparent);
}

.chip--avg {
  color: var(--gr-3);
  background: color-mix(in srgb, var(--gr-3) 14%, transparent);
}

.risk__info {
  font-size: var(--text-md);
  color: var(--brand-text-muted);
  cursor: help;
}

.risk__more {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  border: 0;
  padding: 0;
  background: transparent;
  font: inherit;
  font-weight: 600;
  color: var(--q-primary);
  cursor: pointer;
}
</style>

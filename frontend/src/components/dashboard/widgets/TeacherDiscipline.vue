<template>
  <ChartCard
    title="Qaysi o'qituvchilar jurnalni to'ldirmayapti?"
    subtitle="Oxirgi 7 ish kuni · faqat qo'lda va qurilmadan olingan davomat"
    :as-of="asOf"
    :loading="loading"
    :error="error"
    :data="data"
    :empty="!!data && !data.length"
    empty-text="Bu davrda dars bo'lmagan"
    scroll
    @retry="reload"
  >
    <div class="td">
      <div class="td__legend" aria-hidden="true">
        <span><i class="td__sw td__sw--a" /> Davomat olingan</span>
        <span><i class="td__sw td__sw--g" /> Baho qo'yilgan</span>
      </div>
      <component
        :is="readonly ? 'div' : 'router-link'"
        v-for="t in shown"
        :key="t.employeeId"
        :to="readonly ? undefined : `/profiles/teacher/${t.employeeId}`"
        class="td__row"
      >
        <div class="td__name ellipsis"
          >{{ t.name
          }}<span class="td__lessons"> · {{ t.lessons }} dars</span></div
        >
        <div class="td__bullets">
          <div class="td__track" :title="`Davomat: ${t.attended}/${t.lessons}`">
            <span
              class="td__fill td__fill--a"
              :style="{ width: `${t.attendedPct ?? 0}%` }"
            />
            <span class="td__val">{{ formatPercent(t.attendedPct) }}</span>
          </div>
          <div class="td__track" :title="`Baho: ${t.graded}/${t.lessons}`">
            <span
              class="td__fill td__fill--g"
              :style="{ width: `${t.gradedPct ?? 0}%` }"
            />
            <span class="td__val">{{ formatPercent(t.gradedPct) }}</span>
          </div>
        </div>
      </component>
    </div>
    <template v-if="(data || []).length > 7" #footer>
      <a href="#" class="td__all" @click.prevent="all = !all">{{
        all ? 'Kamroq' : `Hammasi (${data.length})`
      }}</a>
    </template>
  </ChartCard>
</template>

<script setup>
// 6.9 — per teacher, the share of their lessons with attendance taken by a person or a
// device and with at least one grade; lowest first.
import { computed, ref } from 'vue'
import ChartCard from '../ChartCard.vue'
import { useWidget } from '@/composables/useWidget'
import { formatPercent } from '@/utils/format'

defineProps({ readonly: { type: Boolean, default: false } })
const { data, asOf, loading, error, reload } = useWidget('teacher-discipline')
const all = ref(false)
const shown = computed(() =>
  all.value ? data.value || [] : (data.value || []).slice(0, 7)
)
</script>

<style scoped>
.td__legend {
  display: flex;
  gap: var(--space-4);
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
  margin-bottom: var(--space-2);
}

.td__sw {
  display: inline-block;
  width: var(--space-3);
  height: var(--space-2);
  border-radius: var(--radius-sm);
  margin-right: var(--space-1);
  vertical-align: middle;
}

.td__sw--a,
.td__fill--a {
  background: var(--st-present);
}

.td__sw--g,
.td__fill--g {
  background: var(--gr-4);
}

.td__row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1.3fr);
  gap: var(--space-3);
  align-items: center;
  padding: var(--space-1) 0;
  color: inherit;
  text-decoration: none;
  font-size: var(--text-sm);
}

a.td__row:hover .td__name {
  color: var(--q-primary);
}

.td__lessons {
  color: var(--brand-text-muted);
  font-size: var(--text-xs);
}

.td__bullets {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.td__track {
  position: relative;
  margin: var(--space-1) 0;
  height: var(--bar-thin);
  background: var(--surface-2);
  border-radius: var(--radius-sm);
  margin-right: calc(var(--space-7) + var(--space-2));
}

.td__fill {
  position: absolute;
  inset: 0 auto 0 0;
  border-radius: var(--radius-sm);
}

.td__val {
  position: absolute;
  left: calc(100% + var(--space-2));
  top: 50%;
  transform: translateY(-50%);
  font-size: var(--text-xs);
  font-weight: 600;
  line-height: 1;
  white-space: nowrap;
}

.td__all {
  font-weight: 600;
  color: var(--q-primary);
  text-decoration: none;
}
</style>

<template>
  <ChartCard
    title="E'tibor talab qiladi"
    :as-of="asOf"
    :loading="loading"
    :error="error"
    :data="data"
    :empty="!!data && !data.length"
    :skeleton-rows="5"
    data-widget="signals"
    @retry="reload"
  >
    <ul v-if="data" class="signals">
      <li v-for="s in rows" :key="s.kind">
        <button
          type="button"
          :class="['signal', { 'signal--zero': !s.count }]"
          :data-signal="s.kind"
          :disabled="!s.count"
          @click="open(s)"
        >
          <span
            class="signal__icon"
            :style="{
              color: s.color,
              background: `color-mix(in srgb, ${s.color} 12%, transparent)`
            }"
          >
            <q-icon :name="s.icon" />
          </span>
          <span class="signal__name">{{ s.title }}</span>
          <span v-if="s.count" class="signal__count dash-num">{{
            formatNumber(s.count)
          }}</span>
          <span v-else class="signal__ok" aria-label="Muammo yo'q">
            <q-icon name="check" />
          </span>
          <q-icon v-if="s.count" name="chevron_right" class="signal__arrow" />
        </button>
      </li>
    </ul>

    <template v-if="data && data.length" #footer>
      Raqamli qator bosilsa — to'liq ro'yxat va amallar ochiladi
    </template>

    <SidePanel
      v-model="panelOpen"
      :title="current?.title || ''"
      :subtitle="current ? `${formatNumber(current.count)} ta` : ''"
      :icon="current?.icon"
      :color="current?.color"
    >
      <ul v-if="current" class="panel-list">
        <li v-for="(it, i) in current.items" :key="i" class="panel-item">
          <div class="panel-item__main">
            <b>{{ it.title }}</b>
            <small v-if="it.subtitle">{{ it.subtitle }}</small>
          </div>
          <span v-if="it.detail" class="panel-item__detail">{{
            it.detail
          }}</span>
          <template v-if="!readonly">
            <AppButton
              v-if="it.ref && it.ref.phone"
              variant="ghost"
              size="sm"
              icon="call"
              :href="`tel:${it.ref.phone}`"
              aria-label="Qo'ng'iroq"
            />
            <AppButton
              v-if="action(it)"
              variant="ghost"
              size="sm"
              class="panel-item__action"
              :label="action(it).label"
              :to="action(it).to"
              data-action
            />
          </template>
        </li>
      </ul>
      <p
        v-if="current && current.count > current.items.length"
        class="panel-more"
      >
        Yana {{ current.count - current.items.length }} ta — eng muhim
        {{ current.items.length }} tasi ko'rsatildi
      </p>
    </SidePanel>
  </ChartCard>
</template>

<script setup>
// 6.6 — five signals, not a long list: lessons without attendance, students absent 3+
// days, appeals unanswered for a day, falling grades, teachers with an incomplete
// journal. A zero row is faint with a check; a row with a number opens the side panel
// with the full list and small actions ("Davomat olish", "Ochish").
import { computed, ref } from 'vue'
import ChartCard from '../ChartCard.vue'
import SidePanel from '../SidePanel.vue'
import AppButton from '@/components/app/AppButton.vue'
import { useWidget } from '@/composables/useWidget'
import { formatNumber } from '@/utils/format'

defineProps({ readonly: { type: Boolean, default: false } })
const { data, asOf, loading, error, reload } = useWidget('signals')

const META = {
  UNMARKED_LESSON: { icon: 'event_busy', color: 'var(--st-absent)' },
  ABSENT_STREAK: { icon: 'person_off', color: 'var(--st-absent)' },
  APPEAL_OVERDUE: { icon: 'forum', color: 'var(--st-late)' },
  GRADE_DROP: { icon: 'trending_down', color: 'var(--gr-3)' },
  TEACHER_DISCIPLINE: { icon: 'menu_book', color: 'var(--ring-journal)' }
}

const rows = computed(() =>
  (data.value || []).map(s => ({
    ...s,
    ...(META[s.kind] || { icon: 'flag', color: 'var(--st-unmarked)' })
  }))
)

const panelOpen = ref(false)
const current = ref(null)
function open(s) {
  if (!s.count) return
  current.value = s
  panelOpen.value = true
}

function action(it) {
  const r = it.ref || {}
  switch (it.kind) {
    case 'UNMARKED_LESSON':
      return {
        label: 'Davomat olish',
        to: {
          path: '/attendance',
          query: { lessonSlotId: r.lessonSlotId, date: r.date }
        }
      }
    case 'ABSENT_STREAK':
    case 'GRADE_DROP':
      return r.studentId
        ? { label: 'Ochish', to: `/profiles/student/${r.studentId}` }
        : null
    case 'APPEAL_OVERDUE':
      return {
        label: 'Ochish',
        to: { path: '/parent-messages', query: { id: r.appealId } }
      }
    case 'TEACHER_DISCIPLINE':
      return r.employeeId
        ? { label: 'Ochish', to: `/profiles/teacher/${r.employeeId}` }
        : null
    default:
      return null
  }
}
</script>

<style scoped>
.signals {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.signal {
  width: 100%;
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-2);
  border: 0;
  border-radius: var(--radius-md);
  background: transparent;
  color: inherit;
  font: inherit;
  text-align: left;
  cursor: pointer;
  transition: background var(--dur-fast) var(--ease-out);
}

.signal:hover:not(:disabled) {
  background: var(--surface-2);
}

.signal:focus-visible {
  outline: var(--border-focus);
  outline-offset: calc(var(--hairline) * -1);
}

.signal--zero {
  cursor: default;
  opacity: 0.55;
}

.signal__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: var(--tile-sm);
  height: var(--tile-sm);
  border-radius: var(--radius-sm);
  font-size: var(--text-md);
  flex-shrink: 0;
}

.signal__name {
  flex: 1 1 auto;
  min-width: 0;
  font-size: var(--text-sm);
  font-weight: 500;
}

.signal__count {
  font-size: var(--text-lg);
}

.signal__ok {
  color: var(--st-present);
  font-size: var(--text-md);
  display: inline-flex;
}

.signal__arrow {
  color: var(--brand-text-muted);
  font-size: var(--text-lg);
}

.panel-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.panel-item {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-3) 0;
  border-bottom: 1px solid var(--dash-edge);
}

.panel-item:last-child {
  border-bottom: 0;
}

.panel-item__main {
  flex: 1 1 auto;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.panel-item__main b {
  font-size: var(--text-sm);
  font-weight: 600;
}

.panel-item__main small {
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
}

.panel-item__detail {
  font-size: var(--text-xs);
  font-weight: 600;
  color: var(--brand-text-muted);
  white-space: nowrap;
}

.panel-more {
  margin: var(--space-3) 0 0;
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
}
</style>

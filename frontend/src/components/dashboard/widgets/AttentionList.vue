<template>
  <ChartCard
    :title="title"
    :as-of="asOf"
    :loading="loading"
    :error="error"
    :data="data"
    :empty="!!data && !open.length"
    empty-text="Hammasi joyida"
    scroll
    @retry="reload"
  >
    <div class="att">
      <section
        v-for="g in open"
        :key="g.kind"
        class="att__group"
        :data-kind="g.kind"
      >
        <div class="att__group-head">
          <span
            :class="['att__level', `att__level--${g.level}`]"
            aria-hidden="true"
          />
          <span class="att__group-title">{{ g.title }}</span>
          <span class="att__count">{{ g.total }}</span>
        </div>
        <ul class="att__items">
          <li v-for="(it, i) in g.items" :key="i" class="att__item">
            <component
              :is="rowLink(it) ? 'router-link' : 'div'"
              :to="rowLink(it) || undefined"
              class="att__text"
            >
              <div class="att__title ellipsis">{{ it.title }}</div>
              <div class="att__sub ellipsis">
                <span v-if="it.subtitle">{{ it.subtitle }}</span>
                <span v-if="it.subtitle && it.detail"> · </span>
                <b v-if="it.detail">{{ it.detail }}</b>
                <a
                  v-if="it.ref?.phone && !readonly"
                  :href="`tel:${it.ref.phone}`"
                  class="att__phone"
                  @click.stop
                >
                  · {{ it.ref.phone }}</a
                >
              </div>
            </component>
            <AppButton
              v-if="!readonly && action(it)"
              size="sm"
              variant="secondary"
              :label="action(it).label"
              :to="action(it).to"
              class="att__action"
            />
          </li>
        </ul>
        <router-link
          v-if="g.total > g.items.length && more(g)"
          :to="more(g)"
          class="att__more"
        >
          Hammasini ko'rish ({{ g.total }})
        </router-link>
      </section>
    </div>
  </ChartCard>
</template>

<script setup>
// 6.3 — what needs someone today, most urgent first, each row with what to do about it.
import { computed } from 'vue'
import ChartCard from '../ChartCard.vue'
import AppButton from '@/components/app/AppButton.vue'
import { useWidget } from '@/composables/useWidget'

const props = defineProps({
  readonly: { type: Boolean, default: false },
  // "my-attention" for a teacher (their classes only)
  source: { type: String, default: 'attention' },
  title: { type: String, default: "E'tibor talab qiladi" }
})
const { data, asOf, loading, error, reload } = useWidget(props.source)

const open = computed(() => (data.value || []).filter(g => g.total > 0))

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
      return {
        label: 'Ota-onaga yozish',
        to: `/profiles/student/${r.studentId}`
      }
    case 'APPEAL_OVERDUE':
      return {
        label: 'Ochish',
        to: { path: '/parent-messages', query: { id: r.appealId } }
      }
    case 'GRADE_DROP':
      return {
        label: 'Jurnal',
        to: {
          path: '/gradebook',
          query: { class: r.classId, subject: r.subjectId }
        }
      }
    case 'TEACHER_DISCIPLINE':
      return { label: 'Ko‘rish', to: `/profiles/teacher/${r.employeeId}` }
    case 'TELEGRAM_LOW':
      return {
        label: 'Ulanmaganlar',
        to: {
          path: '/telegram-parents',
          query: { class: r.classId, status: 'NOT_LINKED' }
        }
      }
    case 'PERMISSION_EXPIRED':
    case 'PERMISSION_ENDING':
      return { label: 'Ochish', to: '/library/permissions' }
    default:
      return null
  }
}

function rowLink(it) {
  if (props.readonly) return null
  if (it.ref?.studentId) return `/profiles/student/${it.ref.studentId}`
  return null
}

function more(g) {
  switch (g.kind) {
    case 'APPEAL_OVERDUE':
      return '/parent-messages'
    case 'TELEGRAM_LOW':
      return { path: '/telegram-parents', query: { status: 'NOT_LINKED' } }
    case 'UNMARKED_LESSON':
      return '/attendance'
    case 'PERMISSION_EXPIRED':
    case 'PERMISSION_ENDING':
    case 'LIBRARY_PERMISSION':
      return '/library/permissions'
    default:
      return null
  }
}
</script>

<style scoped>
.att {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.att__group-head {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-sm);
  font-weight: 600;
  margin-bottom: var(--space-1);
}

.att__level {
  width: var(--dot);
  height: var(--dot);
  border-radius: var(--radius-pill);
  flex-shrink: 0;
}

.att__level--red {
  background: var(--st-absent);
}
.att__level--orange {
  background: var(--st-late);
}
.att__level--yellow {
  background: var(--gr-3);
}

.att__group-title {
  flex: 1 1 0;
  min-width: 0;
}

.att__count {
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
  background: var(--surface-2);
  border-radius: var(--radius-sm);
  padding: 0 var(--space-2);
  line-height: 1.8;
}

.att__items {
  list-style: none;
  margin: 0;
  padding: 0;
}

.att__item {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) 0;
  border-bottom: var(--border);
}

.att__item:last-child {
  border-bottom: 0;
}

.att__text {
  flex: 1 1 0;
  min-width: 0;
  color: inherit;
  text-decoration: none;
}

a.att__text:hover .att__title {
  color: var(--q-primary);
}

.att__title {
  font-size: var(--text-sm);
  font-weight: 600;
}

.att__sub {
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
}

.att__phone {
  color: inherit;
}

.att__action {
  flex-shrink: 0;
}

.att__more {
  display: inline-block;
  margin-top: var(--space-1);
  font-size: var(--text-xs);
  font-weight: 600;
  color: var(--q-primary);
  text-decoration: none;
}
</style>

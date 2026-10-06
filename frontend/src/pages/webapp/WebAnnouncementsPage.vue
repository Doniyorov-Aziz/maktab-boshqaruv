<template>
  <div>
    <div class="wa-segment">
      <button :class="{ active: tab === 'ann' }" @click="select('ann')">
        📢 {{ t('tab_announcements') }}
        <span v-if="unread" class="count">{{ unread }}</span>
      </button>
      <button :class="{ active: tab === 'events' }" @click="select('events')">
        📅 {{ t('tab_events') }}
      </button>
    </div>

    <div v-if="error" class="wa-card">
      <WaState
        kind="error"
        :text="t('error')"
        :retry-text="t('retry')"
        @retry="retry"
      />
    </div>
    <div v-else-if="!data" class="wa-card"
      ><q-skeleton v-for="i in 4" :key="i" type="text"
    /></div>

    <template v-else-if="tab === 'ann'">
      <div v-if="!data.announcements.length" class="wa-card">
        <WaState :text="t('no_announcements')" />
      </div>
      <div
        v-for="a in data.announcements"
        :key="a.id"
        class="wa-card ann"
        :class="{ important: a.important }"
        @click="toggle(a.id)"
      >
        <div class="row items-center q-gutter-x-xs q-mb-xs">
          <span v-if="a.unread" class="wa-pill new">🆕 {{ t('new') }}</span>
          <span v-if="a.important" class="wa-pill imp"
            >🔴 {{ t('important') }}</span
          >
          <q-space />
          <span class="wa-hint">{{
            a.date ? shortDate(state.lang, a.date) : ''
          }}</span>
        </div>
        <div class="text-weight-bold title">{{ a.title }}</div>
        <div v-if="a.classLabel" class="wa-hint"
          >{{ a.classLabel }} {{ t('for_class') }}</div
        >
        <div class="content" :class="{ clamp: open !== a.id }">{{
          a.content
        }}</div>
        <div v-if="a.deadline" class="deadline">
          ⏰ <b>{{ t('deadline') }}:</b> {{ fullDate(state.lang, a.deadline) }}
        </div>
      </div>
    </template>

    <template v-else>
      <div v-if="!data.events.length" class="wa-card">
        <WaState :text="t('no_events')" />
      </div>
      <div
        v-for="e in data.events"
        :key="e.id"
        class="wa-card event"
        :class="`ev-${e.type}`"
      >
        <div class="ev-date">
          <b>{{ Number(e.startDate.slice(8)) }}</b>
          <span>{{ month(e.startDate) }}</span>
        </div>
        <div class="col">
          <div class="text-weight-bold"
            >{{ eventIcon(e.type) }} {{ e.title }}</div
          >
          <div class="wa-hint">
            {{ dayTitle(state.lang, e.startDate) }}
            <template v-if="e.endDate && e.endDate !== e.startDate">
              – {{ shortDate(state.lang, e.endDate) }}</template
            >
            · {{ when(e.startDate) }}
          </div>
          <div v-if="e.description" class="ev-desc">{{ e.description }}</div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { parentApi } from '@/webapp/api'
import { state, t } from '@/webapp/state'
import { fullDate, shortDate, dayTitle } from '@/webapp/i18n'
import { eventIcon } from '@/webapp/icons'
import { haptic, hapticSelect } from '@/webapp/telegram'
import { useLoad } from '@/webapp/useLoad'
import { isoDate } from '@/webapp/time'
import WaState from '@/components/webapp/WaState.vue'

const tab = ref('ann')
const open = ref(null)

const { data, error, retry } = useLoad(async () => {
  const id = state.childId
  const [announcements, events] = await Promise.all([
    parentApi.get(`/api/parent/students/${id}/announcements`),
    parentApi.get(`/api/parent/students/${id}/events`)
  ])
  return { announcements: announcements.data, events: events.data }
})

const unread = computed(
  () => data.value?.announcements.filter(a => a.unread).length || 0
)

function select(next) {
  if (tab.value === next) return
  hapticSelect()
  tab.value = next
}

function toggle(id) {
  haptic()
  open.value = open.value === id ? null : id
}

const month = date => shortDate(state.lang, date).replace(/^\d+[-\s]/, '')

function when(date) {
  const today = new Date(isoDate(new Date()) + 'T00:00:00')
  const d = Math.round((new Date(date + 'T00:00:00') - today) / 86400000)
  if (d <= 0) return t('today')
  if (d === 1) return t('tomorrow')
  return t('in_days', { d })
}
</script>

<style scoped>
.wa-segment button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.count {
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 99px;
  background: var(--wa-bad);
  color: #fff;
  font-size: 11px;
  line-height: 18px;
}

.ann {
  cursor: pointer;
}

.ann.important {
  border-left: 4px solid var(--wa-bad);
}

.title {
  font-size: 16px;
}

.content {
  margin-top: 6px;
  white-space: pre-wrap;
  line-height: 1.45;
}

.clamp {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.deadline {
  margin-top: 8px;
  font-size: 14px;
}

.new {
  background: rgba(79, 70, 229, 0.14);
  color: var(--wa-accent);
}

.imp {
  background: rgba(239, 68, 68, 0.14);
  color: var(--wa-bad);
}

.event {
  display: flex;
  gap: 14px;
  align-items: flex-start;
}

.ev-date {
  width: 52px;
  flex-shrink: 0;
  border-radius: 12px;
  background: color-mix(in srgb, var(--wa-accent) 12%, transparent);
  color: var(--wa-accent);
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 6px 0;
  line-height: 1.1;
}

.ev-date b {
  font-size: 20px;
}

.ev-date span {
  font-size: 11px;
  font-weight: 600;
}

.ev-PARENT_MEETING .ev-date {
  background: rgba(236, 72, 153, 0.12);
  color: #db2777;
}

.ev-HOLIDAY .ev-date,
.ev-VACATION .ev-date {
  background: rgba(16, 185, 129, 0.12);
  color: #059669;
}

.ev-EXAM .ev-date {
  background: rgba(245, 158, 11, 0.14);
  color: #d97706;
}

.ev-desc {
  margin-top: 4px;
  font-size: 14px;
  line-height: 1.4;
}
</style>

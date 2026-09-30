<template>
  <div>
    <div class="wa-card month-nav">
      <button class="nav-btn" aria-label="prev" @click="shift(-1)">‹</button>
      <div class="month-title">{{ monthTitle(state.lang, year, month) }}</div>
      <button
        class="nav-btn"
        aria-label="next"
        :disabled="isCurrentMonth"
        @click="shift(1)"
        >›</button
      >
    </div>

    <div v-if="!data" class="wa-card"><q-skeleton height="280px" /></div>
    <template v-else>
      <div class="wa-card summary">
        <div
          class="ring"
          :style="{
            '--p': (s.rate || 0) * 3.6 + 'deg',
            '--c': rateColor(s.rate)
          }"
        >
          <div class="ring-inner">
            <div class="ring-value">{{
              s.rate == null ? '—' : fmt(s.rate) + '%'
            }}</div>
            <div class="wa-hint">{{ t('rate') }}</div>
          </div>
        </div>
        <div class="counts">
          <div
            ><span class="dot" style="background: var(--wa-good)" />{{
              t('present')
            }}: <b>{{ s.present }}</b></div
          >
          <div
            ><span class="dot" style="background: var(--wa-warn)" />{{
              t('late')
            }}: <b>{{ s.late }}</b></div
          >
          <div
            ><span class="dot" style="background: var(--wa-bad)" />{{
              t('absent')
            }}: <b>{{ s.absent }}</b></div
          >
          <div
            ><span class="dot" style="background: var(--wa-info)" />{{
              t('excused')
            }}: <b>{{ s.excused }}</b></div
          >
          <div v-if="s.classRate != null" class="wa-hint q-mt-xs"
            >{{ t('class_avg') }}: {{ fmt(s.classRate) }}%</div
          >
        </div>
      </div>

      <div class="wa-card">
        <div class="grid head">
          <div v-for="i in 7" :key="i" class="wa-hint">{{
            dayShort(state.lang, i - 1)
          }}</div>
        </div>
        <div class="grid">
          <div v-for="n in lead" :key="'l' + n" />
          <button
            v-for="d in s.days"
            :key="d.date"
            class="day"
            :class="[
              `d-${d.status}`,
              { selected: d.date === selected, today: d.date === todayStr }
            ]"
            @click="pick(d.date)"
          >
            {{ Number(d.date.slice(8)) }}
          </button>
        </div>
      </div>

      <div v-if="selected" class="wa-card">
        <div class="wa-card-title">{{ fullDate(state.lang, selected) }}</div>
        <div v-if="selectedIncidents.length">
          <div
            v-for="i in selectedIncidents"
            :key="i.lessonNumber"
            class="wa-row"
          >
            <span
              class="wa-pill"
              :class="i.status === 'ABSENT' ? 'bad' : 'warn'"
            >
              {{
                i.status === 'ABSENT' ? '🔴 ' + t('absent') : '🟡 ' + t('late')
              }}
            </span>
            <div class="col"
              >{{ i.lessonNumber }}-{{ t('lesson') }} ·
              {{ subjectIcon(i.subject) }} {{ i.subject }}</div
            >
          </div>
        </div>
        <div v-else class="wa-empty">{{
          selectedStatus === 'NONE'
            ? t('nothing_marked')
            : selectedStatus === 'EXCUSED'
              ? '🔵 ' + t('excused')
              : t('all_present')
        }}</div>
      </div>

      <div v-if="data.bySubject.length" class="wa-card">
        <div class="wa-card-title">{{ t('by_subject') }}</div>
        <div v-for="b in data.bySubject" :key="b.subject" class="wa-row">
          <div class="col">{{ subjectIcon(b.subject) }} {{ b.subject }}</div>
          <span v-if="b.absent" class="wa-pill bad">🔴 {{ b.absent }}</span>
          <span v-if="b.late" class="wa-pill warn">🟡 {{ b.late }}</span>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { parentApi } from '@/webapp/api'
import { state, t } from '@/webapp/state'
import { monthTitle, dayShort, fullDate } from '@/webapp/i18n'
import { subjectIcon } from '@/webapp/icons'
import { haptic } from '@/webapp/telegram'
import { todayStr as localToday } from '@/utils/date'

const todayStr = localToday()
const now = new Date()
const year = ref(now.getFullYear())
const month = ref(now.getMonth() + 1)
const data = ref(null)
const selected = ref(null)

const s = computed(() => data.value.summary)
const isCurrentMonth = computed(
  () => year.value === now.getFullYear() && month.value === now.getMonth() + 1
)
const lead = computed(() => {
  if (!data.value) return 0
  const [y, m, d] = s.value.from.split('-').map(Number)
  return (new Date(y, m - 1, d).getDay() + 6) % 7
})
const selectedIncidents = computed(() =>
  (data.value?.incidents || []).filter(i => i.date === selected.value)
)
const selectedStatus = computed(
  () => s.value.days.find(d => d.date === selected.value)?.status
)

function fmt(v) {
  return Number.isInteger(v) ? v : v.toFixed(1)
}

function rateColor(rate) {
  if (rate == null) return 'var(--wa-hint)'
  if (rate >= 90) return 'var(--wa-good)'
  if (rate >= 75) return 'var(--wa-warn)'
  return 'var(--wa-bad)'
}

function pick(date) {
  haptic()
  selected.value = date
}

async function load() {
  data.value = null
  const key = `${year.value}-${String(month.value).padStart(2, '0')}`
  data.value = (
    await parentApi.get(`/api/parent/students/${state.childId}/attendance`, {
      params: { month: key }
    })
  ).data
  selected.value = isCurrentMonth.value ? todayStr : null
}

function shift(delta) {
  haptic()
  const d = new Date(year.value, month.value - 1 + delta, 1)
  year.value = d.getFullYear()
  month.value = d.getMonth() + 1
  load()
}

onMounted(load)
</script>

<style scoped>
.month-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 10px;
}

.month-title {
  font-weight: 700;
  font-size: 17px;
}

.nav-btn {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  border: 1px solid var(--wa-border);
  background: transparent;
  color: var(--wa-text);
  font-size: 22px;
  cursor: pointer;
}

.nav-btn:disabled {
  opacity: 0.35;
}

.summary {
  display: flex;
  align-items: center;
  gap: 18px;
}

/* Conic-gradient ring = attendance percentage. */
.ring {
  width: 104px;
  height: 104px;
  border-radius: 50%;
  background: conic-gradient(var(--c) var(--p), rgba(148, 163, 184, 0.2) 0);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.ring-inner {
  width: 82px;
  height: 82px;
  border-radius: 50%;
  background: var(--wa-card);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.ring-value {
  font-weight: 800;
  font-size: 20px;
}

.counts {
  display: flex;
  flex-direction: column;
  gap: 3px;
  font-size: 14px;
}

.dot {
  display: inline-block;
  width: 9px;
  height: 9px;
  border-radius: 50%;
  margin-right: 6px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 6px;
}

.grid.head {
  text-align: center;
  margin-bottom: 6px;
  font-weight: 600;
}

.day {
  aspect-ratio: 1;
  border-radius: 10px;
  border: 2px solid transparent;
  font: inherit;
  font-weight: 700;
  cursor: pointer;
  background: rgba(148, 163, 184, 0.15);
  color: var(--wa-hint);
}

.d-PRESENT {
  background: rgba(16, 185, 129, 0.18);
  color: #059669;
}

.d-LATE {
  background: rgba(245, 158, 11, 0.2);
  color: #b45309;
}

.d-ABSENT {
  background: rgba(239, 68, 68, 0.18);
  color: #dc2626;
}

.d-EXCUSED {
  background: rgba(59, 130, 246, 0.18);
  color: #2563eb;
}

.day.today {
  border-color: var(--wa-accent);
}

.day.selected {
  box-shadow:
    0 0 0 2px var(--wa-card),
    0 0 0 4px var(--wa-accent);
}

.bad {
  background: rgba(239, 68, 68, 0.15);
  color: var(--wa-bad);
}

.warn {
  background: rgba(245, 158, 11, 0.18);
  color: #b45309;
}
</style>

<template>
  <div>
    <div v-if="error" class="wa-card">
      <WaState
        kind="error"
        :text="t('error')"
        :retry-text="t('retry')"
        @retry="retry"
      />
    </div>
    <template v-else>
      <div class="days">
        <button
          v-for="(d, i) in data || placeholders"
          :key="i"
          class="day"
          :class="{ active: i === selected, today: d && d.date === todayIso }"
          @click="select(i)"
        >
          <span class="day-name">{{ dayShort(state.lang, i) }}</span>
          <span class="day-num">{{ d ? Number(d.date.slice(8)) : '·' }}</span>
          <span
            v-if="d && d.lessons.length && !d.holidayTitle"
            class="day-dots"
          >
            <i v-for="n in Math.min(d.lessons.length, 6)" :key="n" />
          </span>
        </button>
      </div>

      <div v-if="!data" class="wa-card"
        ><q-skeleton v-for="i in 6" :key="i" type="text" height="34px"
      /></div>
      <div v-else-if="day" class="wa-card">
        <div class="wa-card-title">
          {{ dayTitle(state.lang, day.date) }},
          {{ shortDate(state.lang, day.date) }}
          <span v-if="day.date === todayIso" class="wa-pill today-pill">{{
            t('today')
          }}</span>
        </div>
        <div v-if="day.holidayTitle" class="wa-empty"
          >🎉 {{ day.holidayTitle }} — {{ t('holiday') }}</div
        >
        <WaState
          v-else-if="!day.lessons.length"
          :text="day.weekend ? t('weekend') : t('no_schedule')"
        />
        <template v-for="l in day.lessons" :key="l.number">
          <div v-if="l.breakBeforeMinutes >= 5" class="break"
            >☕ {{ l.breakBeforeMinutes }} {{ t('break_min') }}</div
          >
          <div
            class="lesson"
            :class="{ now: current === l.number }"
            :style="{ '--subject': subjectColor(l.subject) }"
          >
            <div class="lesson-time">
              <b>{{ time(l.start) }}</b>
              <span>{{ time(l.end) }}</span>
            </div>
            <div class="lesson-body">
              <div class="lesson-subject">
                <span class="lesson-emoji">{{ subjectIcon(l.subject) }}</span>
                {{ l.subject }}
              </div>
              <div class="wa-hint">
                {{ l.number }}-{{ t('lesson') }} · {{ l.teacher }}
                <template v-if="l.room">
                  · {{ l.room }} {{ t('room') }}</template
                >
              </div>
              <div v-if="current === l.number" class="lesson-bar">
                <div :style="{ width: Math.round(progress * 100) + '%' }" />
              </div>
            </div>
            <span v-if="current === l.number" class="wa-pill now-pill"
              >▶ {{ t('now') }}</span
            >
          </div>
        </template>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { parentApi } from '@/webapp/api'
import { state, t } from '@/webapp/state'
import { dayTitle, shortDate, dayShort } from '@/webapp/i18n'
import { subjectIcon, subjectColor } from '@/webapp/icons'
import { hapticSelect } from '@/webapp/telegram'
import { useLoad } from '@/webapp/useLoad'
import { useNow, dayPhase, isoDate } from '@/webapp/time'
import WaState from '@/components/webapp/WaState.vue'

const placeholders = [null, null, null, null, null, null]
const time = v => (v || '').slice(0, 5)
const now = useNow()
const todayIso = computed(() => isoDate(now.value))
const selected = ref(0)

const { data, error, retry } = useLoad(
  async () =>
    (
      await parentApi.get(`/api/parent/students/${state.childId}/schedule`, {
        params: { day: 'week' }
      })
    ).data
)

// Open on today's tab (Monday on Sunday).
watch(
  data,
  (days, before) => {
    if (!days || before) return
    const i = days.findIndex(d => d.date === todayIso.value)
    selected.value = i >= 0 ? i : 0
  },
  { immediate: true }
)

const day = computed(() => data.value?.[selected.value] || null)

const livePhase = computed(() =>
  day.value && day.value.date === todayIso.value && !day.value.holidayTitle
    ? dayPhase(day.value.lessons, now.value)
    : null
)
const current = computed(() => livePhase.value?.current?.number ?? null)
const progress = computed(() => livePhase.value?.progress ?? 0)

function select(i) {
  if (i === selected.value) return
  hapticSelect()
  selected.value = i
}
</script>

<style scoped>
.days {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 6px;
  margin-bottom: 12px;
}

.day {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 8px 0 6px;
  border-radius: 14px;
  border: 1px solid var(--wa-border);
  background: var(--wa-card);
  color: var(--wa-text);
  font: inherit;
  cursor: pointer;
  transition:
    background 0.2s,
    transform 0.15s;
}

.day:active {
  transform: scale(0.96);
}

.day-name {
  font-size: 12px;
  color: var(--wa-hint);
  font-weight: 600;
}

.day-num {
  font-size: 17px;
  font-weight: 800;
}

.day.today {
  border-color: var(--wa-accent);
}

.day.today .day-name {
  color: var(--wa-accent);
}

.day.active {
  background: var(--wa-accent);
  border-color: var(--wa-accent);
  color: var(--wa-accent-text);
}

.day.active .day-name {
  color: var(--wa-accent-text);
  opacity: 0.85;
}

.day-dots {
  display: flex;
  gap: 2px;
  height: 4px;
}

.day-dots i {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: currentColor;
  opacity: 0.45;
}

.today-pill {
  margin-left: 6px;
  background: color-mix(in srgb, var(--wa-accent) 14%, transparent);
  color: var(--wa-accent);
  vertical-align: middle;
}

.break {
  font-size: 12px;
  color: var(--wa-hint);
  padding: 2px 0 2px 62px;
}

.lesson {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 10px 10px 0;
  border-radius: 12px;
  position: relative;
}

.lesson-time {
  width: 50px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  font-size: 13px;
  line-height: 1.25;
}

.lesson-time span {
  color: var(--wa-hint);
  font-size: 12px;
}

.lesson-body {
  flex: 1;
  min-width: 0;
  padding-left: 12px;
  border-left: 4px solid var(--subject);
}

.lesson-subject {
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 6px;
}

.lesson-emoji {
  width: 26px;
  height: 26px;
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: color-mix(in srgb, var(--subject) 16%, transparent);
  font-size: 15px;
}

.lesson.now {
  background: rgba(16, 185, 129, 0.1);
  box-shadow: inset 0 0 0 1.5px rgba(16, 185, 129, 0.5);
}

.lesson-bar {
  height: 5px;
  border-radius: 99px;
  background: var(--wa-border);
  margin-top: 6px;
  overflow: hidden;
}

.lesson-bar div {
  height: 100%;
  background: var(--wa-good);
  border-radius: 99px;
}

.now-pill {
  background: rgba(16, 185, 129, 0.15);
  color: var(--wa-good);
}
</style>

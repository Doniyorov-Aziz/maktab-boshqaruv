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
    <template v-else-if="!data">
      <div class="wa-card hero-skeleton"
        ><q-skeleton type="text" width="40%" /><q-skeleton
          type="text"
          width="70%"
          height="34px" /><q-skeleton type="QChip"
      /></div>
      <div class="wa-card"><q-skeleton height="70px" /></div>
      <div class="wa-card"
        ><q-skeleton v-for="i in 3" :key="i" type="text"
      /></div>
    </template>
    <template v-else>
      <div class="wa-card hero">
        <div class="hero-date">{{ fullDate(state.lang, data.date) }}</div>
        <div class="hero-name">{{ data.child.fullName }}</div>
        <div class="hero-sub">
          {{ data.child.className }} {{ t('class') }}
          <template v-if="data.child.classTeacher">
            · {{ t('class_teacher') }}: {{ data.child.classTeacher }}</template
          >
        </div>
        <div class="hero-status" :class="`st-${data.attendanceState}`">
          <span class="hero-status-label">{{ t('today_att') }}:</span>
          {{ statusText }}
        </div>
      </div>

      <div v-if="phase" class="wa-card live" :class="`live--${phase.phase}`">
        <template v-if="phase.phase === 'lesson'">
          <div class="live-top">
            <span class="live-dot" />
            <span class="live-label">{{
              t('now_lesson', { n: phase.current.number })
            }}</span>
            <q-space />
            <span class="wa-hint">{{
              t('minutes_left', { m: phase.left })
            }}</span>
          </div>
          <div class="live-subject">
            {{ subjectIcon(phase.current.subject) }} {{ phase.current.subject }}
          </div>
          <div class="wa-hint">
            {{ time(phase.current.start) }}–{{ time(phase.current.end) }} ·
            {{ phase.current.teacher }}
            <template v-if="phase.current.room">
              · {{ phase.current.room }} {{ t('room') }}</template
            >
          </div>
        </template>
        <template v-else-if="phase.phase === 'break'">
          <div class="live-top">
            <span class="live-label">☕ {{ t('break_now') }}</span>
            <q-space />
            <span class="wa-hint">{{
              t('minutes_left', { m: phase.left })
            }}</span>
          </div>
        </template>
        <template v-else-if="phase.phase === 'before'">
          <div class="live-top">
            <span class="live-label"
              >🌅
              {{ t('first_lesson_at', { time: time(phase.next.start) }) }}</span
            >
          </div>
        </template>
        <div v-else class="live-label">{{ t('lessons_over') }}</div>

        <div
          v-if="phase.phase === 'lesson' || phase.phase === 'break'"
          class="bar"
        >
          <div
            class="bar-fill"
            :style="{ width: Math.round(phase.progress * 100) + '%' }"
          />
        </div>
        <div
          v-if="phase.next && phase.phase !== 'before'"
          class="live-next wa-hint"
        >
          {{ t('next_up') }}:
          <b>{{ subjectIcon(phase.next.subject) }} {{ phase.next.subject }}</b>
          · {{ time(phase.next.start) }}
        </div>
      </div>

      <div class="tiles">
        <div class="wa-card tile">
          <div class="tile-value">{{ data.gradesToday.length }}</div>
          <div class="wa-hint">{{ t('grades_today') }}</div>
        </div>
        <div class="wa-card tile">
          <div class="tile-value">{{ data.newAnnouncements }}</div>
          <div class="wa-hint">{{ t('new_announcements') }}</div>
        </div>
      </div>

      <div class="wa-card">
        <div class="card-head">
          <div class="wa-card-title">{{ t('recent') }}</div>
          <router-link to="/webapp/grades" class="see-all"
            >{{ t('see_all') }} ›</router-link
          >
        </div>
        <WaState v-if="!lastGrades.length" :text="t('no_grades')" />
        <div v-for="g in lastGrades" :key="g.id" class="wa-row">
          <div class="wa-score" :style="{ background: scoreColor(g.score) }">{{
            g.score
          }}</div>
          <div class="col">
            <div class="text-weight-semibold"
              >{{ subjectIcon(g.subject) }} {{ g.subject }}</div
            >
            <div class="wa-hint">
              {{ shortDate(state.lang, g.date) }} · {{ t('grade_' + g.type) }}
              <template v-if="g.comment"> · «{{ g.comment }}»</template>
            </div>
          </div>
        </div>
      </div>

      <div class="wa-card">
        <div class="wa-card-title">{{ t('lessons_today') }}</div>
        <div v-if="data.schedule.holidayTitle" class="wa-empty"
          >🎉 {{ data.schedule.holidayTitle }} — {{ t('holiday') }}</div
        >
        <WaState
          v-else-if="!data.schedule.lessons.length"
          :text="t('no_lessons')"
        />
        <div
          v-for="l in data.schedule.lessons"
          :key="l.number"
          class="wa-row lesson"
          :class="{ now: phase?.current?.number === l.number, done: isDone(l) }"
        >
          <div class="num" :style="{ background: subjectColor(l.subject) }">{{
            l.number
          }}</div>
          <div class="col">
            <div class="text-weight-semibold"
              >{{ subjectIcon(l.subject) }} {{ l.subject }}</div
            >
            <div class="wa-hint"
              >{{ time(l.start) }}–{{ time(l.end) }} · {{ l.teacher }}</div
            >
          </div>
          <span
            v-if="phase?.current?.number === l.number"
            class="wa-pill now-pill"
            >▶ {{ t('now') }}</span
          >
        </div>
      </div>

      <div v-if="data.nextEvent" class="wa-card event">
        <div class="wa-hint">{{ t('next_event') }}</div>
        <div class="text-weight-semibold"
          >{{ eventIcon(data.nextEvent.type) }} {{ data.nextEvent.title }}</div
        >
        <div class="wa-hint">{{
          fullDate(state.lang, data.nextEvent.startDate)
        }}</div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { parentApi } from '@/webapp/api'
import { state, t } from '@/webapp/state'
import { fullDate, shortDate } from '@/webapp/i18n'
import {
  subjectIcon,
  subjectColor,
  eventIcon,
  scoreColor
} from '@/webapp/icons'
import { useLoad } from '@/webapp/useLoad'
import { useNow, dayPhase, isoDate, minutesOf } from '@/webapp/time'
import WaState from '@/components/webapp/WaState.vue'

const time = v => (v || '').slice(0, 5)
const now = useNow()

const { data, error, retry } = useLoad(async () => {
  const id = state.childId
  const [today, grades] = await Promise.all([
    parentApi.get(`/api/parent/students/${id}/today`),
    parentApi.get(`/api/parent/students/${id}/grades`)
  ])
  return { ...today.data, recent: grades.data.recent || [] }
})

const lastGrades = computed(() => (data.value?.recent || []).slice(0, 3))

// The live card only makes sense when the device's day is the school's day.
const phase = computed(() => {
  const d = data.value
  if (!d || d.schedule.holidayTitle || isoDate(now.value) !== d.date)
    return null
  return dayPhase(d.schedule.lessons, now.value)
})

function isDone(lesson) {
  if (!phase.value) return false
  const minute = now.value.getHours() * 60 + now.value.getMinutes()
  return minute >= minutesOf(lesson.end)
}

const statusText = computed(() => {
  const s = data.value?.attendanceState
  if (!s || s === 'NONE') return '⏳ ' + t('att_none')
  const icons = { PRESENT: '✅', LATE: '🟡', ABSENT: '🔴', EXCUSED: '🔵' }
  return `${icons[s] || ''} ${t('att_' + s)}`
})
</script>

<style scoped>
.hero {
  background:
    radial-gradient(
      circle at 92% 8%,
      rgba(255, 255, 255, 0.18) 0 60px,
      transparent 61px
    ),
    linear-gradient(135deg, #4f46e5 0%, #6d28d9 100%);
  color: #fff;
  border: 0;
  padding: 16px 18px;
}

.hero-date {
  font-size: 13px;
  opacity: 0.85;
}

.hero-name {
  font-size: 22px;
  font-weight: 800;
  margin: 2px 0;
}

.hero-sub {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.85);
}

.hero-status {
  display: inline-block;
  margin-top: 12px;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.18);
  font-weight: 600;
  font-size: 14px;
}

.hero-status-label {
  opacity: 0.8;
  font-weight: 500;
}

.hero-status.st-ABSENT {
  background: rgba(239, 68, 68, 0.85);
}

.hero-status.st-LATE {
  background: rgba(245, 158, 11, 0.85);
}

.live {
  border-left: 4px solid var(--wa-good);
}

.live--break,
.live--before {
  border-left-color: var(--wa-warn);
}

.live--over {
  border-left-color: var(--wa-hint);
}

.live-top {
  display: flex;
  align-items: center;
  gap: 8px;
}

.live-label {
  font-weight: 700;
}

.live-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: var(--wa-good);
  box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.6);
  animation: pulse 1.8s infinite;
}

@keyframes pulse {
  70% {
    box-shadow: 0 0 0 8px rgba(16, 185, 129, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(16, 185, 129, 0);
  }
}

.live-subject {
  font-size: 18px;
  font-weight: 800;
  margin: 6px 0 2px;
}

.bar {
  height: 8px;
  border-radius: 99px;
  background: var(--wa-border);
  overflow: hidden;
  margin-top: 10px;
}

.bar-fill {
  height: 100%;
  border-radius: 99px;
  background: linear-gradient(90deg, #10b981, #34d399);
  transition: width 0.6s ease;
}

.live--break .bar-fill {
  background: linear-gradient(90deg, #f59e0b, #fbbf24);
}

.live-next {
  margin-top: 8px;
}

.tiles {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.tile-value {
  font-size: 26px;
  font-weight: 800;
}

.card-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}

.see-all {
  color: var(--wa-link);
  text-decoration: none;
  font-size: 14px;
  font-weight: 600;
}

.num {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  flex-shrink: 0;
}

.lesson.done {
  opacity: 0.55;
}

.now {
  background: rgba(16, 185, 129, 0.1);
  border-radius: 10px;
  padding-left: 6px;
  padding-right: 6px;
}

.now-pill {
  background: rgba(16, 185, 129, 0.15);
  color: var(--wa-good);
}

.event {
  border-left: 4px solid var(--wa-accent);
}
</style>

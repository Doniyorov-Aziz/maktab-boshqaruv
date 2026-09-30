<template>
  <div>
    <div v-if="!data" class="wa-card"
      ><q-skeleton v-for="i in 4" :key="i" type="text"
    /></div>
    <template v-else>
      <div class="wa-card hero">
        <div class="hero-date">{{ fullDate(state.lang, data.date) }}</div>
        <div class="hero-name">{{ data.child.fullName }}</div>
        <div class="wa-hint">
          {{ data.child.className }} {{ t('class') }}
          <template v-if="data.child.classTeacher">
            · {{ t('class_teacher') }}: {{ data.child.classTeacher }}</template
          >
        </div>
        <div class="hero-status" :class="`st-${data.attendanceState}`">
          {{ statusText }}
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
        <div class="wa-card-title">{{ t('lessons_today') }}</div>
        <div v-if="data.schedule.holidayTitle" class="wa-empty"
          >🎉 {{ data.schedule.holidayTitle }} — {{ t('holiday') }}</div
        >
        <div v-else-if="!data.schedule.lessons.length" class="wa-empty">{{
          t('no_lessons')
        }}</div>
        <div
          v-for="l in data.schedule.lessons"
          :key="l.number"
          class="wa-row"
          :class="{ now: l.now }"
        >
          <div class="num">{{ l.number }}</div>
          <div class="col">
            <div class="text-weight-semibold"
              >{{ icon(l.subject) }} {{ l.subject }}</div
            >
            <div class="wa-hint"
              >{{ time(l.start) }}–{{ time(l.end) }} · {{ l.teacher }}</div
            >
          </div>
          <span v-if="l.now" class="wa-pill now-pill">▶ {{ t('now') }}</span>
        </div>
      </div>

      <div v-if="data.gradesToday.length" class="wa-card">
        <div class="wa-card-title">{{ t('grades_today') }}</div>
        <div v-for="g in data.gradesToday" :key="g.id" class="wa-row">
          <div class="wa-score" :style="{ background: scoreColor(g.score) }">{{
            g.score
          }}</div>
          <div class="col">
            <div class="text-weight-semibold">{{ g.subject }}</div>
            <div class="wa-hint"
              >{{ t('grade_' + g.type)
              }}<template v-if="g.comment"> · «{{ g.comment }}»</template></div
            >
          </div>
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
import { ref, computed, onMounted } from 'vue'
import { parentApi } from '@/webapp/api'
import { state, t } from '@/webapp/state'
import { fullDate } from '@/webapp/i18n'
import { subjectIcon, eventIcon, scoreColor } from '@/webapp/icons'

const data = ref(null)
const icon = subjectIcon
const time = v => (v || '').slice(0, 5)

const statusText = computed(() => {
  const s = data.value?.attendanceState
  if (!s || s === 'NONE') return '⏳ ' + t('att_none')
  const icons = { PRESENT: '✅', LATE: '🟡', ABSENT: '🔴', EXCUSED: '🔵' }
  return `${icons[s] || ''} ${t('att_' + s)}`
})

onMounted(async () => {
  data.value = (
    await parentApi.get(`/api/parent/students/${state.childId}/today`)
  ).data
})
</script>

<style scoped>
.hero {
  background: linear-gradient(135deg, #4f46e5 0%, #6d28d9 100%);
  color: #fff;
  border: 0;
}

.hero .wa-hint {
  color: rgba(255, 255, 255, 0.8);
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

.hero-status {
  display: inline-block;
  margin-top: 10px;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.18);
  font-weight: 600;
  font-size: 14px;
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

.num {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: rgba(79, 70, 229, 0.12);
  color: var(--wa-accent);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  flex-shrink: 0;
}

.now {
  background: rgba(16, 185, 129, 0.08);
  border-radius: 10px;
}

.now-pill {
  background: rgba(16, 185, 129, 0.15);
  color: var(--wa-good);
}

.event {
  border-left: 4px solid var(--wa-accent);
}
</style>

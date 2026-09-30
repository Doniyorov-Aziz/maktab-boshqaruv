<template>
  <div>
    <div class="wa-segment">
      <button
        v-for="d in ['today', 'tomorrow', 'week']"
        :key="d"
        :class="{ active: day === d }"
        @click="select(d)"
      >
        {{ t(d) }}
      </button>
    </div>

    <div v-if="!days" class="wa-card"
      ><q-skeleton v-for="i in 5" :key="i" type="text"
    /></div>
    <div v-for="d in days || []" :key="d.date" class="wa-card">
      <div class="wa-card-title"
        >{{ dayTitle(state.lang, d.date) }},
        {{ shortDate(state.lang, d.date) }}</div
      >
      <div v-if="d.holidayTitle" class="wa-empty"
        >🎉 {{ d.holidayTitle }} — {{ t('holiday') }}</div
      >
      <div v-else-if="!d.lessons.length" class="wa-empty">{{
        d.weekend ? t('weekend') : t('no_schedule')
      }}</div>
      <template v-for="l in d.lessons" :key="l.number">
        <div v-if="l.breakBeforeMinutes >= 5 && day !== 'week'" class="break"
          >☕ {{ l.breakBeforeMinutes }} {{ t('break_min') }}</div
        >
        <div class="wa-row" :class="{ now: l.now }">
          <div class="num">{{ l.number }}</div>
          <div class="col">
            <div class="text-weight-semibold"
              >{{ subjectIcon(l.subject) }} {{ l.subject }}</div
            >
            <div class="wa-hint">
              {{ time(l.start) }}–{{ time(l.end) }}
              <template v-if="day !== 'week'">
                · {{ l.teacher }} · {{ l.room }} {{ t('room') }}</template
              >
            </div>
          </div>
          <span v-if="l.now" class="wa-pill now-pill">▶ {{ t('now') }}</span>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { parentApi } from '@/webapp/api'
import { state, t } from '@/webapp/state'
import { dayTitle, shortDate } from '@/webapp/i18n'
import { subjectIcon } from '@/webapp/icons'
import { haptic } from '@/webapp/telegram'

const day = ref('today')
const days = ref(null)
const time = v => (v || '').slice(0, 5)

async function load() {
  days.value = null
  days.value = (
    await parentApi.get(`/api/parent/students/${state.childId}/schedule`, {
      params: { day: day.value }
    })
  ).data
}

function select(d) {
  haptic()
  day.value = d
  load()
}

onMounted(load)
</script>

<style scoped>
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

.break {
  font-size: 12px;
  color: var(--wa-hint);
  padding: 2px 0 2px 38px;
}

.now {
  background: rgba(16, 185, 129, 0.08);
  border-radius: 10px;
}

.now-pill {
  background: rgba(16, 185, 129, 0.15);
  color: var(--wa-good);
}
</style>

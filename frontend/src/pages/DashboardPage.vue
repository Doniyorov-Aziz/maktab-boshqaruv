<template>
  <page-layout icon="insights" :title="title">
    <template #subtitle>
      {{ todayLabel }} · {{ quarterLabel }}
      <template v-if="schoolStore.activeSchoolName">
        · {{ schoolStore.activeSchoolName }}</template
      >
    </template>
    <template v-if="home && home.kind !== 'TEACHER'" #actions>
      <router-link
        v-if="visitSummary"
        to="/visits"
        class="dash-visits"
        data-testid="demo-visits-widget"
      >
        👀 Demo tashriflari: {{ visitSummary.visitors }} kishi · bugun
        {{ visitSummary.today }}
      </router-link>
      <span class="dash-period">
        <span class="dash-period__label">Baholar davri</span>
        <SegTabs v-model="period" :options="PERIODS" label="Baholar davri" />
      </span>
    </template>

    <div v-if="!home" class="dash dash-loading" aria-busy="true">
      <div v-for="i in 6" :key="i" class="skel dash-loading__tile" />
    </div>

    <!-- head of school / director, and the read-only viewer -->
    <div
      v-else-if="home.kind !== 'TEACHER'"
      class="dash"
      :class="{ 'dash--readonly': readonly }"
      data-dash="school"
    >
      <div class="dash__health" style="--i: 0"><HealthHero /></div>
      <div class="dash__kpis" style="--i: 1">
        <KpiGrid :readonly="readonly" />
      </div>
      <div class="dash__today" style="--i: 2">
        <TodayStrip :readonly="readonly" />
      </div>
      <div class="dash__bubbles" style="--i: 3">
        <ClassBubbles :readonly="readonly" />
      </div>
      <div class="dash__risk" style="--i: 4">
        <RiskStudents :readonly="readonly" />
      </div>
      <div class="dash__dynamics" style="--i: 5">
        <Dynamics :readonly="readonly" />
      </div>
      <div class="dash__signals" style="--i: 6">
        <Signals :readonly="readonly" />
      </div>
      <div class="dash__subjects" style="--i: 7"><SubjectDumbbell /></div>
      <div class="dash__grades" style="--i: 8"><GradeStack /></div>
      <div class="dash__live" style="--i: 9">
        <LiveStrip :readonly="readonly" />
      </div>
      <div class="dash__parents" style="--i: 10">
        <ParentsTelegram :readonly="readonly" />
      </div>
      <div class="dash__events" style="--i: 11">
        <UpcomingEvents :readonly="readonly" />
      </div>
      <div class="dash__journal" style="--i: 12">
        <JournalCompleteness :readonly="readonly" />
      </div>
    </div>

    <!-- teacher -->
    <div
      v-else
      class="dash dash--teacher"
      :class="{ 'dash--no-class': !home.ledClassId }"
      data-dash="teacher"
    >
      <div class="dash__myindex" style="--i: 0"><MyIndex /></div>
      <div class="dash__mylessons" style="--i: 1"><MyLessons /></div>
      <div class="dash__myrisk" style="--i: 2">
        <RiskStudents
          source="my-risk"
          title="Sinflarimdagi xavf ostidagi o'quvchilar"
        />
      </div>
      <div v-if="home.ledClassId" class="dash__myclass" style="--i: 3">
        <MyClass />
      </div>
      <div v-if="home.ledClassId" class="dash__myappeals" style="--i: 4">
        <MyAppeals />
      </div>
    </div>
  </page-layout>
</template>

<script setup>
// The home page — a decision centre. Which page a user gets (school / teacher / read-only)
// comes from the server (/api/dashboard/home); every widget loads, fails and refreshes on
// its own (useWidget). A 12-column grid of equal-height rows: hero (index + four numbers),
// today's strip, then pairs, the live ribbon and the bottom row; one column under 1024 px.
import { computed, ref, watch, onMounted } from 'vue'
import PageLayout from '@/components/PageLayout.vue'
import SegTabs from '@/components/dashboard/SegTabs.vue'
import HealthHero from '@/components/dashboard/v3/HealthHero.vue'
import KpiGrid from '@/components/dashboard/v3/KpiGrid.vue'
import TodayStrip from '@/components/dashboard/v3/TodayStrip.vue'
import ClassBubbles from '@/components/dashboard/v3/ClassBubbles.vue'
import RiskStudents from '@/components/dashboard/v3/RiskStudents.vue'
import Dynamics from '@/components/dashboard/v3/Dynamics.vue'
import Signals from '@/components/dashboard/v3/Signals.vue'
import SubjectDumbbell from '@/components/dashboard/v3/SubjectDumbbell.vue'
import GradeStack from '@/components/dashboard/v3/GradeStack.vue'
import LiveStrip from '@/components/dashboard/v3/LiveStrip.vue'
import JournalCompleteness from '@/components/dashboard/v3/JournalCompleteness.vue'
import MyIndex from '@/components/dashboard/v3/MyIndex.vue'
import ParentsTelegram from '@/components/dashboard/widgets/ParentsTelegram.vue'
import UpcomingEvents from '@/components/dashboard/widgets/UpcomingEvents.vue'
import {
  MyLessons,
  MyAppeals,
  MyClass
} from '@/components/dashboard/widgets/TeacherWidgets'
import { PERIODS, usePeriod } from '@/components/dashboard/v3/period'
import { api } from '@/boot/axios'
import { useAuthStore } from '@/stores/auth'
import { useSchoolStore } from '@/stores/school'
import { useSystemInfo } from '@/composables/useSystemInfo'

const authStore = useAuthStore()
const schoolStore = useSchoolStore()
const home = ref(null)
const readonly = computed(() => home.value?.kind === 'VIEWER')
const period = usePeriod()

async function loadHome() {
  if (!schoolStore.activeSchoolId) return
  try {
    const res = await api.get('/api/dashboard/home', {
      params: { schoolId: schoolStore.activeSchoolId }
    })
    home.value = res.data
  } catch {
    // the school page still works without knowing the role exactly
    home.value = { kind: authStore.role === 'VIEWER' ? 'VIEWER' : 'ADMIN' }
  }
}
onMounted(loadHome)
watch(
  () => schoolStore.activeSchoolId,
  () => {
    home.value = null
    loadHome()
  }
)

const title = computed(() =>
  authStore.isGuest
    ? 'Xush kelibsiz! Demo rejimi'
    : `Xush kelibsiz, ${authStore.username}`
)

// the public demo's visitors, for the real administrator while the demo link works
const systemInfo = useSystemInfo()
const visitSummary = ref(null)
watch(
  () => systemInfo.value.guestLogin,
  async on => {
    if (!on || !authStore.isRealAdmin) return
    try {
      const res = await api.get('/api/demo/visits/summary', {
        background: true
      })
      visitSummary.value = res.data
    } catch {
      visitSummary.value = null
    }
  },
  { immediate: true }
)
const MONTHS = [
  'yanvar',
  'fevral',
  'mart',
  'aprel',
  'may',
  'iyun',
  'iyul',
  'avgust',
  'sentabr',
  'oktabr',
  'noyabr',
  'dekabr'
]
const DAYS = [
  'Yakshanba',
  'Dushanba',
  'Seshanba',
  'Chorshanba',
  'Payshanba',
  'Juma',
  'Shanba'
]
const now = new Date()
const todayLabel = `${DAYS[now.getDay()]}, ${now.getDate()}-${MONTHS[now.getMonth()]}`
const quarterLabel = (() => {
  const m = now.getMonth() + 1
  const q =
    m === 9 || m === 10
      ? 'I'
      : m === 11 || m === 12
        ? 'II'
        : m <= 3
          ? 'III'
          : 'IV'
  return `${q} chorak`
})()
</script>

<style scoped>
.dash-visits {
  display: inline-flex;
  align-items: center;
  min-height: var(--control-h-sm);
  padding: 0 var(--space-3);
  border-radius: var(--radius-pill);
  border: 1px solid var(--brand-border);
  background: var(--card-bg);
  color: var(--text-primary);
  font-size: var(--text-sm);
  font-weight: 600;
  text-decoration: none;
  white-space: nowrap;
}

.dash-visits:hover {
  border-color: var(--color-info);
}

.dash {
  position: relative;
  isolation: isolate;
  display: grid;
  grid-template-columns: repeat(12, minmax(0, 1fr));
  gap: var(--space-4);
  align-items: stretch;
}

/* the hero glow: a very soft brand light behind the top of the page */
.dash::before {
  content: '';
  position: absolute;
  z-index: -1;
  left: calc(var(--page-pad) * -1);
  right: calc(var(--page-pad) * -1);
  top: calc(var(--page-gap) * -2);
  height: var(--glow-h);
  pointer-events: none;
  background:
    radial-gradient(
      ellipse 50% 70% at 22% 0%,
      var(--dash-glow),
      transparent 70%
    ),
    radial-gradient(
      ellipse 40% 60% at 82% 10%,
      var(--dash-glow-2),
      transparent 70%
    );
}

.dash > div {
  min-width: 0;
  grid-column: span 12;
}

/* 1024–1439: the hero stacks (index, then four numbers in a row); pairs are 7 + 5 */
@media (min-width: 1024px) {
  .dash__bubbles,
  .dash__dynamics,
  .dash__subjects {
    grid-column: span 7 !important;
  }

  .dash__risk,
  .dash__signals,
  .dash__grades {
    grid-column: span 5 !important;
  }

  .dash__parents,
  .dash__events {
    grid-column: span 6 !important;
  }

  .dash--teacher .dash__myindex {
    grid-column: span 5;
  }

  .dash--teacher .dash__mylessons {
    grid-column: span 7;
  }

  .dash--teacher .dash__myrisk,
  .dash--teacher .dash__myclass {
    grid-column: span 6;
  }

  .dash--teacher.dash--no-class .dash__myrisk {
    grid-column: span 12;
  }
}

/* 1440+: hero 4 + 8, pairs 8 + 4 and 6 + 6, bottom 3 + 3 + 6 */
@media (min-width: 1440px) {
  .dash__health {
    grid-column: span 4 !important;
  }

  .dash__kpis {
    grid-column: span 8 !important;
  }

  .dash__bubbles,
  .dash__dynamics {
    grid-column: span 8 !important;
  }

  .dash__risk,
  .dash__signals {
    grid-column: span 4 !important;
  }

  .dash__subjects,
  .dash__grades {
    grid-column: span 6 !important;
  }

  .dash__parents,
  .dash__events {
    grid-column: span 3 !important;
  }

  .dash__journal {
    grid-column: span 6 !important;
  }

  .dash--teacher .dash__myindex {
    grid-column: span 4;
  }

  .dash--teacher .dash__mylessons {
    grid-column: span 8;
  }
}

.dash-loading__tile {
  grid-column: span 12;
  height: calc(var(--control-h) * 4);
  border-radius: var(--radius-lg);
}

@media (min-width: 1024px) {
  .dash-loading__tile {
    grid-column: span 4;
  }
}

.dash-period {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
}

.dash-period__label {
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
}

@media (max-width: 599px) {
  .dash-period__label {
    display: none;
  }
}
</style>

<style>
/* teacher widgets (render components in TeacherWidgets.js) */
.tw__list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.tw__row {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) 0;
  border-bottom: var(--border);
  font-size: var(--text-sm);
}

.tw__row:last-child {
  border-bottom: 0;
}

.tw__row--now {
  background: color-mix(in srgb, var(--chart-primary) 8%, transparent);
  border-radius: var(--radius-sm);
  padding-left: var(--space-2);
  padding-right: var(--space-2);
}

.tw__time,
.tw__main {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.tw__time {
  width: calc(var(--space-8) + var(--space-5));
  flex-shrink: 0;
}

.tw__main {
  flex: 1 1 0;
}

.tw__time small,
.tw__main small {
  color: var(--brand-text-muted);
  font-size: var(--text-xs);
}

.tw__state {
  font-size: var(--text-xs);
  font-weight: 600;
  border-radius: var(--radius-sm);
  padding: 0 var(--space-2);
  line-height: 1.8;
  white-space: nowrap;
}

.tw__state--MANUAL {
  color: var(--st-present);
  background: color-mix(in srgb, var(--st-present) 14%, transparent);
}

.tw__state--AUTO {
  color: var(--st-excused);
  background: color-mix(in srgb, var(--st-excused) 14%, transparent);
}

.tw__state--NONE {
  color: var(--st-absent);
  background: color-mix(in srgb, var(--st-absent) 14%, transparent);
}

.tw__class {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.tw__facts {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-3);
}

.tw__fact {
  display: flex;
  flex-direction: column;
}

.tw__fact small {
  color: var(--brand-text-muted);
  font-size: var(--text-xs);
}

.tw__fact b {
  font-size: var(--text-lg);
}

.tw__link {
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--q-primary);
  text-decoration: none;
}
</style>

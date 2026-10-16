<template>
  <page-layout icon="insights" :title="title">
    <template #subtitle>
      {{ todayLabel }} · {{ quarterLabel }}
      <template v-if="schoolStore.activeSchoolName">
        · {{ schoolStore.activeSchoolName }}</template
      >
    </template>

    <div v-if="!home" class="dash-loading">
      <q-skeleton
        v-for="i in 6"
        :key="i"
        type="rect"
        class="dash-loading__tile"
      />
    </div>

    <!-- head of school / director, and the read-only viewer -->
    <div
      v-else-if="home.kind !== 'TEACHER'"
      class="dash"
      :class="{ 'dash--readonly': readonly }"
      data-dash="school"
    >
      <KpiRow :readonly="readonly" />
      <div class="dash__map"><AttendanceMap /></div>
      <div class="dash__attention"><AttentionList :readonly="readonly" /></div>
      <div class="dash__live"><LiveLessons :readonly="readonly" /></div>
      <div class="dash__trend"><AttendanceTrend :readonly="readonly" /></div>
      <div class="dash__ranking"><ClassRanking :readonly="readonly" /></div>
      <div class="dash__subjects"><SubjectQuality /></div>
      <div class="dash__grades"><GradeDistribution /></div>
      <div class="dash__discipline"
        ><TeacherDiscipline :readonly="readonly"
      /></div>
      <div class="dash__parents"><ParentsTelegram :readonly="readonly" /></div>
      <div class="dash__events"><UpcomingEvents :readonly="readonly" /></div>
    </div>

    <!-- teacher -->
    <div
      v-else
      class="dash dash--teacher"
      :class="{ 'dash--no-class': !home.ledClassId }"
      data-dash="teacher"
    >
      <div v-if="home.ledClassId" class="dash__myclass"><MyClass /></div>
      <div class="dash__mylessons"><MyLessons /></div>
      <div class="dash__mymissing"><MyMissing /></div>
      <div class="dash__mystudents">
        <AttentionList
          source="my-attention"
          title="Sinflarimdagi e'tibor talab qiladigan o'quvchilar"
        />
      </div>
      <div v-if="home.ledClassId" class="dash__myappeals"><MyAppeals /></div>
      <div v-if="home.ledClassId" class="dash__mylive"><LiveLessons /></div>
    </div>
  </page-layout>
</template>

<script setup>
// The home page — a decision centre. Which page a user gets (school / teacher / read-only)
// comes from the server (/api/dashboard/home); every widget loads, fails and refreshes on
// its own (useWidget), and the layout is a 12-column grid of equal-height rows.
import { computed, ref, watch, onMounted } from 'vue'
import PageLayout from '@/components/PageLayout.vue'
import KpiRow from '@/components/dashboard/widgets/KpiRow.vue'
import AttendanceMap from '@/components/dashboard/widgets/AttendanceMap.vue'
import AttentionList from '@/components/dashboard/widgets/AttentionList.vue'
import LiveLessons from '@/components/dashboard/widgets/LiveLessons.vue'
import AttendanceTrend from '@/components/dashboard/widgets/AttendanceTrend.vue'
import ClassRanking from '@/components/dashboard/widgets/ClassRanking.vue'
import SubjectQuality from '@/components/dashboard/widgets/SubjectQuality.vue'
import GradeDistribution from '@/components/dashboard/widgets/GradeDistribution.vue'
import TeacherDiscipline from '@/components/dashboard/widgets/TeacherDiscipline.vue'
import ParentsTelegram from '@/components/dashboard/widgets/ParentsTelegram.vue'
import UpcomingEvents from '@/components/dashboard/widgets/UpcomingEvents.vue'
import {
  MyLessons,
  MyMissing,
  MyAppeals,
  MyClass
} from '@/components/dashboard/widgets/TeacherWidgets'
import { api } from '@/boot/axios'
import { useAuthStore } from '@/stores/auth'
import { useSchoolStore } from '@/stores/school'

const authStore = useAuthStore()
const schoolStore = useSchoolStore()
const home = ref(null)
const readonly = computed(() => home.value?.kind === 'VIEWER')

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

const title = computed(() => `Xush kelibsiz, ${authStore.username}`)
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
.dash {
  display: grid;
  grid-template-columns: repeat(12, minmax(0, 1fr));
  gap: var(--space-4);
  align-items: stretch;
}

.dash > div {
  min-width: 0;
}

/* phones and tablets: one column, KPI two in a row */
.dash :deep(.kpi) {
  grid-column: span 6;
}

.dash > div {
  grid-column: span 12;
}

@media (min-width: 1024px) {
  .dash :deep(.kpi) {
    grid-column: span 4;
  }

  .dash__ranking,
  .dash__subjects,
  .dash__grades,
  .dash__discipline,
  .dash__parents,
  .dash__events {
    grid-column: span 6 !important;
  }

  .dash--teacher .dash__mymissing,
  .dash--teacher .dash__mystudents,
  .dash--teacher .dash__myappeals,
  .dash--teacher .dash__mylive {
    grid-column: span 6;
  }

  .dash--teacher .dash__myclass {
    grid-column: span 5;
  }

  .dash--teacher .dash__mylessons {
    grid-column: span 7;
  }

  .dash--teacher.dash--no-class .dash__mylessons {
    grid-column: span 12;
  }
}

@media (min-width: 1440px) {
  .dash :deep(.kpi) {
    grid-column: span 2;
  }

  .dash__map {
    grid-column: span 8 !important;
  }

  .dash__attention {
    grid-column: span 4 !important;
  }

  .dash__trend {
    grid-column: span 8 !important;
  }

  .dash__ranking {
    grid-column: span 4 !important;
  }

  .dash__subjects {
    grid-column: span 7 !important;
  }

  .dash__grades {
    grid-column: span 5 !important;
  }

  .dash__discipline {
    grid-column: span 6 !important;
  }

  .dash__parents,
  .dash__events {
    grid-column: span 3 !important;
  }
}

.dash-loading {
  display: grid;
  grid-template-columns: repeat(
    auto-fill,
    minmax(min(var(--grid-min-sm), 100%), 1fr)
  );
  gap: var(--space-4);
}

.dash-loading__tile {
  height: calc(var(--control-h) * 4);
  border-radius: var(--radius-lg);
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

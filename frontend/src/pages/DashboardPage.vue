<template>
  <page-layout>
    <template v-slot:header>
      <div class="col-auto">
        <q-avatar
          size="48px"
          text-color="white"
          icon="waving_hand"
          style="background: var(--brand-gradient)"
        />
      </div>
      <div class="col page-layout__title-col">
        <div class="text-h5 text-weight-bold ellipsis"
          >Xush kelibsiz, {{ authStore.username }}</div
        >
        <div class="text-caption muted-text ellipsis">
          {{ todayLabel }} · {{ quarterLabel }}
          <template v-if="schoolStore.activeSchoolName">
            · {{ schoolStore.activeSchoolName }}</template
          >
        </div>
      </div>
      <div class="col-auto row items-center q-gutter-sm no-wrap">
        <q-badge :color="roleColor" outline class="q-px-sm q-py-xs">{{
          authStore.role
        }}</q-badge>
      </div>
    </template>

    <div class="row q-col-gutter-md q-mb-md">
      <div class="col-6 col-md-3 kpi-enter" style="animation-delay: 0ms">
        <div class="brand-card q-pa-md full-height kpi-card">
          <div class="row items-center justify-between">
            <div class="kpi-icon" style="background: rgba(16, 185, 129, 0.12)">
              <q-icon name="fact_check" color="positive" size="22px" />
            </div>
            <trend-badge :value="summary?.attendanceRateDelta" suffix="%" />
          </div>
          <div class="text-h4 text-weight-bold q-mt-sm">
            <q-skeleton v-if="loading" type="text" width="60px" />
            <count-up
              v-else
              :value="summary?.todayAttendanceRate ?? 0"
              suffix="%"
            />
          </div>
          <div class="text-caption muted-text">Bugungi davomat</div>
          <div v-if="!loading && attendanceInProgress" class="q-mt-sm">
            <div class="text-caption muted-text"
              >Davomat olinmoqda: {{ summary.classesWithAttendanceToday }}/{{
                summary.totalClasses
              }}
              sinf</div
            >
            <q-linear-progress
              :value="attendanceProgressRatio"
              color="positive"
              rounded
              size="6px"
              class="q-mt-xs"
            />
          </div>
          <sparkline
            v-else-if="!loading"
            :data="summary?.attendanceTrend14"
            color="#10b981"
            suffix="%"
          />
        </div>
      </div>

      <div class="col-6 col-md-3 kpi-enter" style="animation-delay: 60ms">
        <div
          class="brand-card q-pa-md full-height kpi-card cursor-pointer"
          @click="openAbsentees"
        >
          <div class="row items-center justify-between">
            <div class="kpi-icon" style="background: rgba(239, 68, 68, 0.12)">
              <q-icon name="person_off" color="negative" size="22px" />
            </div>
            <trend-badge :value="summary?.absentCountDelta" invert />
          </div>
          <div class="text-h4 text-weight-bold q-mt-sm">
            <q-skeleton v-if="loading" type="text" width="60px" />
            <count-up v-else :value="summary?.todayAbsentCount ?? 0" />
          </div>
          <div class="text-caption muted-text">Bugun kelmagan</div>
          <sparkline
            v-if="!loading"
            :data="summary?.absentTrend14"
            color="#ef4444"
          />
        </div>
      </div>

      <div class="col-6 col-md-3 kpi-enter" style="animation-delay: 120ms">
        <div class="brand-card q-pa-md full-height kpi-card">
          <div class="row items-center justify-between">
            <div class="kpi-icon" style="background: rgba(234, 179, 8, 0.14)">
              <q-icon name="grade" color="warning" size="22px" />
            </div>
            <trend-badge :value="summary?.averageGradeDelta" />
          </div>
          <div class="text-h4 text-weight-bold q-mt-sm">
            <q-skeleton v-if="loading" type="text" width="60px" />
            <count-up
              v-else
              :value="summary?.averageGrade ?? 0"
              :decimals="2"
            />
          </div>
          <div class="text-caption muted-text">O'rtacha baho</div>
          <sparkline
            v-if="!loading"
            :data="summary?.gradeTrend14"
            color="#eab308"
          />
        </div>
      </div>

      <div class="col-6 col-md-3 kpi-enter" style="animation-delay: 180ms">
        <div class="brand-card q-pa-md full-height kpi-card">
          <div class="row items-center justify-between">
            <div class="kpi-icon" style="background: rgba(79, 70, 229, 0.12)">
              <q-icon name="groups" color="primary" size="22px" />
            </div>
          </div>
          <div class="text-h4 text-weight-bold q-mt-sm">
            <q-skeleton v-if="loading" type="text" width="80px" />
            <template v-else>
              <count-up :value="summary?.totalStudents ?? 0" /> /
              <count-up :value="summary?.totalTeachers ?? 0" />
            </template>
          </div>
          <div class="text-caption muted-text">O'quvchilar / o'qituvchilar</div>
        </div>
      </div>
    </div>

    <div class="brand-card q-pa-md q-mb-md">
      <div class="row items-center justify-between q-mb-sm">
        <div>
          <div class="text-subtitle1 text-weight-semibold"
            >Hozir ketayotgan darslar</div
          >
          <div v-if="!loading" class="text-caption muted-text">
            {{ liveLessonsHeadline }}
          </div>
        </div>
        <q-btn
          flat
          dense
          no-caps
          color="primary"
          label="Jadvalni ko'rish"
          @click="router.push('/timetable')"
        />
      </div>
      <q-skeleton v-if="loading" type="rect" height="70px" />
      <div
        v-else-if="!liveLessons.length"
        class="muted-text text-body2 q-py-md"
      >
        Hozir hech qanday dars ketayotgani yo'q
      </div>
      <div v-else class="live-lessons-table">
        <div class="live-lessons-row live-lessons-row--head">
          <div class="ll-col-status" />
          <div>Sinf</div>
          <div>Fan</div>
          <div class="gt-xs">O'qituvchi</div>
          <div class="gt-xs">Xona</div>
          <div class="ll-col-progress">Qolgan vaqt</div>
        </div>
        <div
          v-for="l in visibleLiveLessons"
          :key="l.lessonSlotId + l.status"
          class="live-lessons-row"
        >
          <div class="ll-col-status">
            <q-badge
              :color="l.status === 'HOZIR' ? 'positive' : 'grey-6'"
              rounded
            />
          </div>
          <div class="text-weight-medium">{{ l.className }}</div>
          <div class="ellipsis">{{ l.subjectName }}</div>
          <div class="ellipsis gt-xs muted-text">{{ l.teacherName }}</div>
          <div class="gt-xs muted-text">{{ l.roomNumber }}-xona</div>
          <div class="ll-col-progress">
            <template v-if="l.status === 'HOZIR'">
              <q-linear-progress
                :value="lessonProgress(l) / 100"
                color="positive"
                rounded
                size="6px"
                class="q-mb-2xs"
              />
              <span class="text-caption muted-text"
                >{{ l.minutesUntilOrRemaining }} daq qoldi</span
              >
            </template>
            <span v-else class="text-caption muted-text"
              >{{ l.minutesUntilOrRemaining }} daqdan keyin</span
            >
          </div>
        </div>
      </div>
      <div v-if="liveLessons.length > 8" class="text-center q-mt-sm">
        <q-btn
          flat
          dense
          no-caps
          color="primary"
          :label="showAllLessons ? 'Kamroq ko\'rsatish' : 'Hammasini ko\'rish'"
          @click="showAllLessons = !showAllLessons"
        />
      </div>
    </div>

    <div class="row q-col-gutter-md q-mb-md items-stretch">
      <div class="col-12 col-md-7">
        <div class="brand-card q-pa-md full-height column">
          <div class="text-subtitle1 text-weight-semibold q-mb-md"
            >Oxirgi 30 kunlik davomat</div
          >
          <div class="col chart-fill">
            <q-skeleton v-if="loading" type="rect" class="full-height" />
            <Line
              v-else
              :data="attendanceChartData"
              :options="attendanceChartOptions"
            />
          </div>
        </div>
      </div>
      <div class="col-12 col-md-5">
        <class-ranking-card :rankings="rankings" :loading="loading" />
      </div>
    </div>

    <div class="row q-col-gutter-md q-mb-md items-stretch">
      <div class="col-12 col-md-6">
        <div class="brand-card q-pa-md full-height">
          <div class="text-subtitle1 text-weight-semibold q-mb-md"
            >E'tibor talab qiladi</div
          >
          <div v-if="!attentionGroups.length" class="muted-text text-body2"
            >Diqqat talab qiladigan holatlar yo'q</div
          >
          <q-list v-else separator>
            <template v-for="g in attentionGroups" :key="g.type">
              <q-item clickable @click="toggleAttentionGroup(g.type)">
                <q-item-section avatar>
                  <q-icon :name="g.icon" :color="g.color" />
                </q-item-section>
                <q-item-section>{{ g.label }}</q-item-section>
                <q-item-section side class="row items-center q-gutter-xs">
                  <q-badge color="grey-7" outline>{{ g.items.length }}</q-badge>
                  <q-icon
                    :name="
                      expandedAttentionGroups.has(g.type)
                        ? 'expand_less'
                        : 'expand_more'
                    "
                  />
                </q-item-section>
              </q-item>
              <q-slide-transition>
                <div v-if="expandedAttentionGroups.has(g.type)">
                  <q-item
                    v-for="(item, i) in g.items"
                    :key="i"
                    clickable
                    dense
                    class="attention-subitem"
                    @click="goAttention(item)"
                  >
                    <q-item-section>{{ item.description }}</q-item-section>
                  </q-item>
                </div>
              </q-slide-transition>
            </template>
          </q-list>
        </div>
      </div>
      <div class="col-12 col-md-6">
        <div class="brand-card q-pa-md full-height">
          <div class="text-subtitle1 text-weight-semibold q-mb-md"
            >Fanlar bo'yicha o'rtacha baho</div
          >
          <div
            v-if="!subjectAveragesSorted.length"
            class="muted-text text-body2"
            >Ma'lumot yo'q</div
          >
          <div v-else :style="{ height: subjectChartHeight + 'px' }">
            <q-skeleton v-if="loading" type="rect" class="full-height" />
            <Bar
              v-else
              :data="subjectChartData"
              :options="subjectChartOptions"
            />
          </div>
        </div>
      </div>
    </div>

    <div class="row q-col-gutter-md q-mb-md items-stretch">
      <div class="col-12 col-md-6">
        <div class="brand-card q-pa-md full-height">
          <div class="text-subtitle1 text-weight-semibold q-mb-md"
            >Yaqinlashayotgan tadbirlar</div
          >
          <div v-if="!upcomingEvents.length" class="muted-text text-body2"
            >Tadbirlar yo'q</div
          >
          <q-list v-else separator>
            <q-item v-for="e in upcomingEvents" :key="e.id">
              <q-item-section avatar>
                <div
                  class="event-date-block"
                  :style="{ borderColor: eventTypeColor(e.type) }"
                >
                  <div class="event-date-day">{{ eventDay(e.startDate) }}</div>
                  <div class="event-date-month">{{
                    eventMonthShort(e.startDate)
                  }}</div>
                </div>
              </q-item-section>
              <q-item-section>
                <q-item-label>{{ e.title }}</q-item-label>
                <q-item-label caption>{{
                  relativeDay(e.startDate)
                }}</q-item-label>
              </q-item-section>
              <q-item-section side>
                <q-badge
                  :style="{ background: eventTypeColor(e.type) }"
                  text-color="white"
                  >{{ eventTypeLabel(e.type) }}</q-badge
                >
              </q-item-section>
            </q-item>
          </q-list>
        </div>
      </div>
      <div class="col-12 col-md-6">
        <div class="brand-card q-pa-md full-height">
          <div class="text-subtitle1 text-weight-semibold q-mb-md"
            >So'nggi e'lonlar</div
          >
          <div v-if="!latestAnnouncements.length" class="muted-text text-body2"
            >E'lonlar yo'q</div
          >
          <q-list v-else separator>
            <q-item
              v-for="a in latestAnnouncements"
              :key="a.id"
              clickable
              @click="toggleAnnouncement(a.id)"
            >
              <q-item-section avatar>
                <q-icon
                  name="campaign"
                  :color="a.priority === 'HIGH' ? 'negative' : 'primary'"
                />
              </q-item-section>
              <q-item-section>
                <q-item-label>{{ a.title }}</q-item-label>
                <q-item-label
                  caption
                  :class="expandedAnnouncements.has(a.id) ? '' : 'ellipsis'"
                  >{{ a.content }}</q-item-label
                >
                <div class="row items-center q-gutter-xs q-mt-2xs">
                  <q-badge v-if="a.priority === 'HIGH'" color="negative" outline
                    >Muhim</q-badge
                  >
                  <span class="text-caption muted-text">{{
                    formatDate(a.createdDate?.slice(0, 10))
                  }}</span>
                </div>
              </q-item-section>
            </q-item>
          </q-list>
        </div>
      </div>
    </div>

    <div class="brand-card q-pa-md">
      <div class="text-subtitle1 text-weight-semibold q-mb-md"
        >So'nggi harakatlar</div
      >
      <div v-if="!activity.length" class="muted-text text-body2"
        >Harakatlar yo'q</div
      >
      <q-list v-else separator>
        <q-item v-for="(a, i) in activity" :key="i">
          <q-item-section avatar>
            <q-icon :name="a.icon" :color="activityColor(a.icon)" />
          </q-item-section>
          <q-item-section>
            <div>{{ a.description }}</div>
            <div v-if="a.actorUsername" class="text-caption muted-text"
              >{{ a.actorUsername }} tomonidan</div
            >
          </q-item-section>
          <q-item-section side class="text-caption muted-text tabular-nums">{{
            formatActivityTime(a.timestamp)
          }}</q-item-section>
        </q-item>
      </q-list>
    </div>

    <q-dialog v-model="absenteesDialogOpen">
      <q-card style="width: 100%; max-width: 480px; border-radius: 16px">
        <q-card-section class="row items-center justify-between">
          <div class="text-subtitle1 text-weight-bold">Bugun kelmaganlar</div>
          <q-badge color="negative">{{ absentees.length }}</q-badge>
        </q-card-section>
        <q-separator />
        <q-list
          v-if="absentees.length"
          separator
          style="max-height: 420px; overflow-y: auto"
        >
          <q-item
            v-for="(a, i) in absentees"
            :key="a.studentId + '-' + i"
            clickable
            v-close-popup
            @click="router.push(`/profiles/student/${a.studentId}`)"
          >
            <q-item-section>
              <q-item-label>{{ a.studentName }}</q-item-label>
              <q-item-label caption
                >{{ a.className }} · {{ a.subjectName }}</q-item-label
              >
            </q-item-section>
          </q-item>
        </q-list>
        <q-card-section v-else class="muted-text text-body2">
          Bugun hech kim yo'q deb qayd etilmagan
        </q-card-section>
      </q-card>
    </q-dialog>
  </page-layout>
</template>

<script setup>
import { ref, computed, onMounted, watch, h, defineComponent } from 'vue'
import { useRouter } from 'vue-router'
import { useQuasar, QIcon } from 'quasar'
import { Bar, Line } from 'vue-chartjs'
import {
  Chart as ChartJS,
  Title,
  Tooltip,
  Filler,
  BarElement,
  LineElement,
  PointElement,
  CategoryScale,
  LinearScale
} from 'chart.js'
import { api } from '@/boot/axios'
import { useAuthStore } from '@/stores/auth'
import { useSchoolStore } from '@/stores/school'
import PageLayout from '@/components/PageLayout.vue'
import ClassRankingCard from '@/components/ClassRankingCard.vue'
import {
  formatDate,
  formatShortDate,
  relativeDay,
  formatActivityTime,
  parseLocalDate,
  monthName
} from '@/utils/date'

ChartJS.register(
  Title,
  Tooltip,
  Filler,
  BarElement,
  LineElement,
  PointElement,
  CategoryScale,
  LinearScale
)

const router = useRouter()
const $q = useQuasar()
const authStore = useAuthStore()
const schoolStore = useSchoolStore()

const loading = ref(true)
const summary = ref(null)
const liveLessons = ref([])
const liveLessonsSummary = ref(null)
const attendanceTrend = ref([])
const rankings = ref([])
const subjectAverages = ref([])
const attention = ref([])
const upcomingEvents = ref([])
const latestAnnouncements = ref([])
const activity = ref([])

const showAllLessons = ref(false)
const absenteesDialogOpen = ref(false)
const absentees = ref([])
const expandedAttentionGroups = ref(new Set())
const expandedAnnouncements = ref(new Set())

const roleColor = computed(() => {
  if (authStore.role === 'ADMIN') return 'grey-7'
  if (authStore.role === 'EDITOR') return 'primary'
  return 'grey-7'
})

const todayLabel = computed(() => {
  const d = new Date()
  return `${weekdayLabel(d)}, ${d.getDate()}-${monthName(d.getMonth()).toLowerCase()}`
})

function weekdayLabel(d) {
  const days = [
    'Yakshanba',
    'Dushanba',
    'Seshanba',
    'Chorshanba',
    'Payshanba',
    'Juma',
    'Shanba'
  ]
  return days[d.getDay()]
}

const quarterLabel = computed(() => {
  const m = new Date().getMonth() + 1
  if (m >= 9 || m === 1) return 'I chorak'
  if (m >= 2 && m <= 3) return 'III chorak'
  if (m >= 4 && m <= 5) return 'IV chorak'
  return 'II chorak'
})

// --- attendance KPI ---
const attendanceInProgress = computed(() => {
  const s = summary.value
  return (
    !!s && s.totalClasses > 0 && s.classesWithAttendanceToday < s.totalClasses
  )
})
const attendanceProgressRatio = computed(() => {
  const s = summary.value
  if (!s || !s.totalClasses) return 0
  return s.classesWithAttendanceToday / s.totalClasses
})

async function openAbsentees() {
  absenteesDialogOpen.value = true
  if (!schoolStore.activeSchoolId) return
  try {
    const res = await api.get('/api/dashboard/absentees-today', {
      params: { schoolId: schoolStore.activeSchoolId }
    })
    absentees.value = res.data
  } catch {
    absentees.value = []
  }
}

// --- live lessons ---
const visibleLiveLessons = computed(() =>
  showAllLessons.value ? liveLessons.value : liveLessons.value.slice(0, 8)
)

const liveLessonsHeadline = computed(() => {
  const s = liveLessonsSummary.value
  if (!s) return ''
  const parts = [s.periodLabel]
  if (s.classesInSession) parts.push(`${s.classesInSession} sinf darsda`)
  if (s.transitionLabel && s.minutesToTransition != null) {
    parts.push(`${s.transitionLabel} ${s.minutesToTransition} daq`)
  }
  return parts.join(' · ')
})

function toMinutes(t) {
  const [hh, mm] = t.split(':').map(Number)
  return hh * 60 + mm
}
function lessonProgress(l) {
  if (l.status !== 'HOZIR') return 0
  const duration = toMinutes(l.endTime) - toMinutes(l.startTime)
  if (duration <= 0) return 0
  const elapsed = duration - l.minutesUntilOrRemaining
  return Math.min(100, Math.max(0, Math.round((elapsed / duration) * 100)))
}

// --- attention panel ---
const ATTENTION_META = {
  MISSING_ATTENDANCE: {
    label: 'Davomat olinmagan sinflar',
    icon: 'fact_check',
    color: 'grey-7'
  },
  CONSECUTIVE_ABSENCE: {
    label: "Surunkali kelmayotgan o'quvchilar",
    icon: 'event_busy',
    color: 'negative'
  },
  LOW_GRADE: {
    label: "Past baholi o'quvchilar",
    icon: 'trending_down',
    color: 'warning'
  }
}
const attentionGroups = computed(() => {
  const order = ['MISSING_ATTENDANCE', 'CONSECUTIVE_ABSENCE', 'LOW_GRADE']
  const groups = []
  for (const type of order) {
    const items = attention.value.filter(a => a.type === type)
    if (items.length) {
      groups.push({ type, items, ...ATTENTION_META[type] })
    }
  }
  return groups
})

function toggleAttentionGroup(type) {
  const next = new Set(expandedAttentionGroups.value)
  if (next.has(type)) next.delete(type)
  else next.add(type)
  expandedAttentionGroups.value = next
}

function goAttention(item) {
  if (item.linkModule === 'students')
    router.push(`/profiles/student/${item.linkId}`)
  else if (item.linkModule === 'attendance-take') router.push('/attendance')
}

function toggleAnnouncement(id) {
  const next = new Set(expandedAnnouncements.value)
  if (next.has(id)) next.delete(id)
  else next.add(id)
  expandedAnnouncements.value = next
}

// --- events ---
const eventTypeColors = {
  HOLIDAY: '#f59e0b',
  EXAM: '#ef4444',
  PARENT_MEETING: '#3b82f6',
  VACATION: '#10b981',
  OTHER: '#8b5cf6'
}
const eventTypeLabels = {
  HOLIDAY: 'Bayram',
  EXAM: 'Imtihon',
  PARENT_MEETING: "Ota-onalar yig'ilishi",
  VACATION: "Ta'til",
  OTHER: 'Boshqa'
}
function eventTypeColor(t) {
  return eventTypeColors[t] || '#8b5cf6'
}
function eventTypeLabel(t) {
  return eventTypeLabels[t] || 'Tadbir'
}
function eventDay(dateStr) {
  return parseLocalDate(dateStr).getDate()
}
function eventMonthShort(dateStr) {
  return monthName(parseLocalDate(dateStr).getMonth()).slice(0, 3).toLowerCase()
}

// --- activity feed ---
function activityColor(icon) {
  return (
    {
      fact_check: 'positive',
      grade: 'warning',
      person_add: 'info',
      campaign: 'primary',
      star: 'positive',
      warning: 'negative'
    }[icon] || 'grey-7'
  )
}

// --- charts ---
const attendanceAverage = computed(() => {
  const vals = attendanceTrend.value.map(p => p.rate).filter(v => v != null)
  if (!vals.length) return null
  return Math.round((vals.reduce((a, b) => a + b, 0) / vals.length) * 10) / 10
})

const attendanceChartData = computed(() => ({
  labels: attendanceTrend.value.map(p => formatShortDate(p.date)),
  datasets: [
    {
      label: 'Davomat %',
      data: attendanceTrend.value.map(p => p.rate),
      borderColor: '#4f46e5',
      backgroundColor: 'rgba(79,70,229,0.14)',
      fill: true,
      tension: 0.35,
      pointRadius: 0,
      spanGaps: true
    },
    {
      label: "O'rtacha",
      data: attendanceTrend.value.map(() => attendanceAverage.value),
      borderColor: '#94a3b8',
      borderDash: [6, 4],
      borderWidth: 1.5,
      pointRadius: 0,
      fill: false,
      spanGaps: true
    }
  ]
}))
const attendanceChartOptions = computed(() => {
  const textColor = $q.dark.isActive ? '#f1f5f9' : '#334155'
  const gridColor = $q.dark.isActive
    ? 'rgba(148,163,184,0.16)'
    : 'rgba(148,163,184,0.18)'
  return {
    responsive: true,
    maintainAspectRatio: false,
    interaction: { mode: 'index', intersect: false },
    plugins: {
      legend: { display: false },
      tooltip: {
        callbacks: {
          label: ctx =>
            ctx.raw == null ? '' : `${ctx.dataset.label}: ${ctx.raw}%`
        }
      }
    },
    scales: {
      x: {
        ticks: {
          color: textColor,
          font: { family: 'Inter' },
          maxTicksLimit: 10
        },
        grid: { display: false }
      },
      y: {
        min: 80,
        max: 100,
        ticks: { color: textColor, font: { family: 'Inter' } },
        grid: { color: gridColor }
      }
    }
  }
})

const subjectAveragesSorted = computed(() =>
  [...subjectAverages.value].sort(
    (a, b) => (b.averageScore ?? 0) - (a.averageScore ?? 0)
  )
)
const subjectChartHeight = computed(() =>
  Math.max(220, subjectAveragesSorted.value.length * 38)
)

function scoreColor(score) {
  const s = Math.max(3, Math.min(5, score ?? 3))
  const t = (s - 3) / 2
  const c1 = [249, 115, 22]
  const c2 = [34, 197, 94]
  const rgb = c1.map((c, i) => Math.round(c + (c2[i] - c) * t))
  return `rgb(${rgb.join(',')})`
}

// Chart.js can fail to draw bars at all when the value axis is truncated
// (min > 0) and the implicit zero-base sits outside the visible range, so
// the data is shifted into a 0..2 scale instead and the ticks/tooltip are
// relabeled back to the real 3..5 score.
const subjectChartData = computed(() => ({
  labels: subjectAveragesSorted.value.map(s => s.subjectName),
  datasets: [
    {
      label: "O'rtacha baho",
      data: subjectAveragesSorted.value.map(s => (s.averageScore ?? 3) - 3),
      backgroundColor: subjectAveragesSorted.value.map(s =>
        scoreColor(s.averageScore)
      ),
      borderRadius: 6,
      barThickness: 16
    }
  ]
}))
const subjectChartOptions = computed(() => {
  const textColor = $q.dark.isActive ? '#f1f5f9' : '#334155'
  const gridColor = $q.dark.isActive
    ? 'rgba(148,163,184,0.16)'
    : 'rgba(148,163,184,0.18)'
  return {
    indexAxis: 'y',
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: false },
      tooltip: {
        callbacks: {
          label: ctx => `${ctx.dataset.label}: ${(ctx.raw + 3).toFixed(2)}`
        }
      }
    },
    scales: {
      x: {
        min: 0,
        max: 2,
        ticks: {
          color: textColor,
          font: { family: 'Inter' },
          callback: v => (v + 3).toFixed(1)
        },
        grid: { color: gridColor }
      },
      y: {
        ticks: { color: textColor, font: { family: 'Inter' } },
        grid: { display: false }
      }
    }
  }
})

const CountUp = defineComponent({
  props: {
    value: { type: Number, default: 0 },
    decimals: { type: Number, default: 0 },
    suffix: { type: String, default: '' }
  },
  setup(props) {
    const display = ref(0)
    watch(
      () => props.value,
      newVal => {
        const start = display.value
        const startTime = performance.now()
        const duration = 600
        function step(now) {
          const t = Math.min(1, (now - startTime) / duration)
          display.value = start + (newVal - start) * (1 - Math.pow(1 - t, 3))
          if (t < 1) requestAnimationFrame(step)
        }
        if (window.matchMedia?.('(prefers-reduced-motion: reduce)').matches) {
          display.value = newVal
        } else {
          requestAnimationFrame(step)
        }
      },
      { immediate: true }
    )
    return () =>
      h('span', `${display.value.toFixed(props.decimals)}${props.suffix}`)
  }
})

const TrendBadge = defineComponent({
  props: {
    value: { type: Number, default: null },
    suffix: { type: String, default: '' },
    invert: { type: Boolean, default: false }
  },
  setup(props) {
    return () => {
      if (props.value == null) return null
      let up = props.value > 0
      if (props.invert) up = !up
      const color = props.value === 0 ? 'grey-6' : up ? 'positive' : 'negative'
      const icon =
        props.value === 0
          ? 'remove'
          : props.value > 0
            ? 'north_east'
            : 'south_east'
      return h('div', { class: `row items-center trend-badge text-${color}` }, [
        h(QIcon, { name: icon, size: '14px' }),
        h(
          'span',
          { class: 'text-caption text-weight-bold q-ml-2xs' },
          `${Math.abs(props.value)}${props.suffix}`
        )
      ])
    }
  }
})

const Sparkline = defineComponent({
  props: {
    data: { type: Array, default: () => [] },
    color: { type: String, default: '#4f46e5' },
    suffix: { type: String, default: '' }
  },
  setup(props) {
    return () => {
      const raw = props.data || []
      const values = raw.filter(v => v != null)
      if (values.length < 2) return h('div', { style: 'height:28px' })
      const max = Math.max(...values, 1)
      const min = Math.min(...values, 0)
      const range = max - min || 1
      const w = 100
      const hgt = 28
      const step = raw.length > 1 ? w / (raw.length - 1) : 0
      let d = ''
      let drawing = false
      const points = []
      raw.forEach((v, i) => {
        if (v == null) {
          drawing = false
          return
        }
        const x = i * step
        const y = hgt - ((v - min) / range) * hgt
        d += `${drawing ? ' L ' : ' M '}${x} ${y}`
        drawing = true
        points.push(
          h(
            'circle',
            {
              cx: x,
              cy: y,
              r: 2.4,
              fill: props.color,
              class: 'sparkline-point'
            },
            [h('title', {}, `${v}${props.suffix}`)]
          )
        )
      })
      return h('svg', { viewBox: `0 0 ${w} ${hgt}`, class: 'sparkline-svg' }, [
        h('path', {
          d: d.trim(),
          fill: 'none',
          stroke: props.color,
          'stroke-width': 2,
          'vector-effect': 'non-scaling-stroke'
        }),
        ...points
      ])
    }
  }
})

async function loadAll() {
  if (!schoolStore.activeSchoolId) {
    loading.value = false
    return
  }
  loading.value = true
  const schoolId = schoolStore.activeSchoolId
  try {
    const [overview, events, ann] = await Promise.all([
      api.get('/api/dashboard/overview', {
        params: { schoolId, trendDays: 30, activityLimit: 8 }
      }),
      api.get('/api/calendar-events/upcoming', {
        params: { schoolId, limit: 5 }
      }),
      api.get('/api/announcements/latest', { params: { schoolId, limit: 5 } })
    ])
    summary.value = overview.data.summary
    liveLessons.value = overview.data.liveLessons
    liveLessonsSummary.value = overview.data.liveLessonsSummary
    attendanceTrend.value = overview.data.attendanceTrend
    rankings.value = overview.data.classRankings
    subjectAverages.value = overview.data.subjectAverages
    attention.value = overview.data.attention
    activity.value = overview.data.activity
    upcomingEvents.value = events.data
    latestAnnouncements.value = ann.data
  } catch (error) {
    $q.notify({
      type: 'negative',
      message:
        error.friendlyMessage || "Dashboard ma'lumotlarini yuklab bo'lmadi"
    })
  } finally {
    loading.value = false
  }
}

onMounted(loadAll)
watch(() => schoolStore.activeSchoolId, loadAll)
</script>

<style scoped>
.muted-text {
  color: var(--brand-text-muted);
}

.kpi-card {
  position: relative;
}

.kpi-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
}

.kpi-enter {
  animation: kpiEnter 0.35s ease both;
}

@keyframes kpiEnter {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (prefers-reduced-motion: reduce) {
  .kpi-enter {
    animation: none;
  }
}

.sparkline-svg {
  width: 100%;
  height: 28px;
  margin-top: 6px;
}

.sparkline-point {
  cursor: default;
}

.trend-badge {
  gap: 2px;
}

.chart-fill {
  min-height: 260px;
}

.live-lessons-table {
  display: flex;
  flex-direction: column;
}

.live-lessons-row {
  display: grid;
  grid-template-columns: 14px 1fr 1fr 1fr 100px 140px;
  gap: 10px;
  align-items: center;
  padding: 8px 4px;
  border-bottom: 1px solid var(--brand-border);
  font-size: var(--text-sm);
}

@media (max-width: 599px) {
  .live-lessons-row {
    grid-template-columns: 14px 1fr 1fr 140px;
  }
}

.live-lessons-row--head {
  font-size: var(--text-xs);
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: var(--brand-text-muted);
  border-bottom: 1px solid var(--brand-border);
}

.ll-col-status {
  display: flex;
  align-items: center;
}

.ll-col-progress {
  min-width: 120px;
}

.attention-subitem {
  padding-left: 56px;
  min-height: 36px;
}

.event-date-block {
  width: 44px;
  text-align: center;
  border: 1.5px solid;
  border-radius: var(--radius-sm);
  padding: 2px 0;
  line-height: 1.1;
}

.event-date-day {
  font-size: var(--text-lg);
  font-weight: 700;
}

.event-date-month {
  font-size: 10px;
  text-transform: uppercase;
  color: var(--brand-text-muted);
}
</style>

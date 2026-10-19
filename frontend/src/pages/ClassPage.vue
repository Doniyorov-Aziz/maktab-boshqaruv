<template>
  <page-layout no-header class="cpg">
    <div v-if="!d && !error" class="cpg__skel">
      <div class="skel cpg__skel-hero" />
      <div class="skel cpg__skel-body" />
    </div>
    <EmptyState
      v-else-if="error && !d"
      icon="cloud_off"
      title="Sinfni yuklab bo'lmadi"
      :text="error"
      action="Qayta urinish"
      action-icon="refresh"
      @action="load"
    />
    <template v-else>
      <!-- hero -->
      <section class="cpg__hero dash-card" data-class-hero>
        <div class="cpg__id">
          <AppButton variant="ghost" icon="arrow_back" aria-label="Sinflar" to="/classes" />
          <AvatarInitials size="xl" square gradient :id="c.grade" :text="c.name" />
          <div class="cpg__titles">
            <h1>{{ c.name }} sinf</h1>
            <div class="cpg__facts">
              <span v-if="c.teacher"
                ><q-icon name="person" /> {{ c.teacher }}
                <a v-if="d.teacherPhone" :href="`tel:${d.teacherPhone}`">{{ formatPhone(d.teacherPhone) }}</a></span
              >
              <span v-if="c.room"><q-icon name="meeting_room" /> {{ roomLabel(c.room) }}</span>
              <span><q-icon name="groups" /> {{ c.students }} o'quvchi</span>
            </div>
          </div>
          <div class="cpg__actions">
            <AppButton icon="fact_check" label="Davomat olish" to="/attendance" />
            <AppButton variant="secondary" icon="grade" label="Jurnal" :to="{ path: '/gradebook', query: { class: c.id } }" />
            <AppButton
              variant="secondary"
              icon="send"
              label="Ota-onalarga xabar"
              :to="auth.isAdmin ? { path: '/broadcasts', query: { class: c.id } } : { path: '/telegram-parents', query: { class: c.id } }"
            />
          </div>
        </div>
        <div class="cpg__kpis">
          <KpiCard
            kpi-key="today"
            label="Bugungi davomat"
            icon="fact_check"
            color="var(--chart-primary)"
            compact
            :value="c.today ?? undefined"
            :empty-value="c.today === null"
            empty-text="Olinmagan"
            :format="v => `${v.toFixed(1)}%`"
            :note="`30 kun: ${formatPercent(c.attendance)}`"
            :spark="c.spark"
            :spark-min="60"
            :spark-max="100"
          />
          <KpiCard
            kpi-key="quality"
            label="Sifat"
            icon="workspace_premium"
            color="var(--ring-quality)"
            compact
            :value="c.quality ?? undefined"
            :empty-value="c.quality === null"
            :format="v => `${v.toFixed(1)}%`"
            :delta="c.qualityDelta"
            :note="`shu chorak · ${c.grades} baho`"
            :spark="null"
          />
          <KpiCard
            kpi-key="mastery"
            label="O'zlashtirish"
            icon="school"
            color="var(--chart-secondary)"
            compact
            :value="c.mastery ?? undefined"
            :empty-value="c.mastery === null"
            :format="v => `${v.toFixed(1)}%`"
            :delta="c.masteryDelta"
            note="3–5 baholar ulushi"
            :spark="null"
          />
          <KpiCard
            kpi-key="telegram"
            label="Telegram"
            icon="send"
            color="var(--ring-telegram)"
            compact
            :value="c.telegram ?? undefined"
            :empty-value="c.telegram === null"
            :format="v => `${v.toFixed(1)}%`"
            :note="`${parents.linked} / ${c.students} oila ulangan`"
            :spark="null"
          />
        </div>
      </section>

      <q-tabs
        :model-value="tab"
        dense
        no-caps
        align="left"
        active-color="primary"
        indicator-color="primary"
        class="cpg__tabs"
        data-class-tabs
        @update:model-value="setTab"
      >
        <q-tab v-for="t in TABS" :key="t.value" :name="t.value" :label="t.label" :data-tab="t.value" />
      </q-tabs>

      <!-- Umumiy -->
      <div v-if="tab === 'overview'" class="cpg__overview">
        <ChartCard title="Davomat dinamikasi" :subtitle="`oxirgi ${d.dynamics.length} ish kuni`" :data="d" :empty="!d.dynamics.some(p => p.rate !== null)">
          <div class="cpg__line">
            <Line :key="themeKey" :data="lineData" :options="lineOptions" />
          </div>
        </ChartCard>
        <ChartCard title="Baholar taqsimoti" subtitle="shu chorak" :data="d" :empty="!gradeTotal">
          <div class="cpg__stack">
            <span
              v-for="s in [5, 4, 3, 2]"
              :key="s"
              :style="{ width: `${(d.gradeCounts[s] / gradeTotal) * 100}%`, background: `var(--gr-${s})` }"
              :title="`«${s}»: ${d.gradeCounts[s]}`"
              >{{ d.gradeCounts[s] / gradeTotal >= 0.08 ? `${Math.round((d.gradeCounts[s] / gradeTotal) * 100)}%` : '' }}</span
            >
          </div>
          <div class="cpg__stack-legend">
            <span v-for="s in [5, 4, 3, 2]" :key="s"><i :style="{ background: `var(--gr-${s})` }" />«{{ s }}» {{ d.gradeCounts[s] }}</span>
          </div>
        </ChartCard>
        <ChartCard title="Fanlar: o'zlashtirish va sifat" subtitle="shu chorak" :data="d" :empty="!d.subjects.length">
          <ul class="cpg__dumb">
            <li v-for="s in d.subjects" :key="s.subjectId">
              <SubjectChip :subject-id="s.subjectId" :name="s.subject" :chosen-color="s.color" />
              <span class="cpg__dumb-track">
                <span
                  class="cpg__dumb-line"
                  :style="{ left: `${s.quality || 0}%`, width: `${Math.max(0, (s.mastery || 0) - (s.quality || 0))}%` }"
                />
                <span class="cpg__dumb-dot cpg__dumb-dot--q" :style="{ left: `${s.quality || 0}%` }" :title="`Sifat ${formatPercent(s.quality)}`" />
                <span class="cpg__dumb-dot cpg__dumb-dot--m" :style="{ left: `${s.mastery || 0}%` }" :title="`O'zlashtirish ${formatPercent(s.mastery)}`" />
              </span>
              <small>{{ formatPercent(s.quality) }} · {{ formatPercent(s.mastery) }}</small>
            </li>
          </ul>
        </ChartCard>
        <ChartCard
          title="Xavf ostidagi o'quvchilar"
          :subtitle="`${d.risk.length} o'quvchi`"
          :data="d"
          :empty="!d.risk.length"
          empty-kind="ok"
          empty-text="Xavf ostida o'quvchi yo'q"
        >
          <RiskRow v-for="(r, i) in d.risk.slice(0, 8)" :key="r.studentId" :row="r" :index="i" @open="openStudent" />
        </ChartCard>
      </div>

      <!-- O'quvchilar -->
      <div v-else-if="tab === 'students'" class="cpg__card dash-card" data-tab-body="students">
        <table class="cpg__table">
          <thead>
            <tr>
              <th>O'quvchi</th>
              <th>Ota-ona</th>
              <th class="num">Davomat (30)</th>
              <th class="num">O'rtacha</th>
              <th class="num">Xavf</th>
              <th class="num">Telegram</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="s in d.students" :key="s.id" :data-student="s.id" @click="openStudent({ studentId: s.id })">
              <td class="cpg__person"><AvatarInitials size="sm" :name="s.name" :id="s.id" /> {{ s.name }}</td>
              <td>{{ s.guardianName || '—' }}</td>
              <td class="num" :class="{ 'cpg__bad': s.attendance !== null && s.attendance < 85 }">{{ formatPercent(s.attendance) }}</td>
              <td class="num">{{ s.average === null ? '—' : s.average.toFixed(2) }}</td>
              <td class="num"><ScoreRing v-if="s.risk !== null" :value="s.risk" :title="`Xavf balli ${s.risk}`" /><span v-else>—</span></td>
              <td class="num">{{ s.telegram ? '✓' : '✗' }}</td>
            </tr>
          </tbody>
        </table>
        <EmptyState v-if="!d.students.length" icon="person_add" title="Sinfda o'quvchi yo'q" />
      </div>

      <!-- Jadval -->
      <div v-else-if="tab === 'schedule'" class="cpg__card cpg__schedule dash-card" data-tab-body="schedule">
        <EmptyState v-if="week && !classLessons.length" icon="event_busy" title="Bu sinf uchun jadval kiritilmagan" />
        <WeekGrid
          v-else-if="week"
          :lessons="classLessons"
          :all="week.lessons"
          :periods="week.periods"
          :days="WEEKDAYS"
          :today="todayWd"
          :now-minutes="nowMin"
          mode="class"
          @open="l => $router.push({ path: '/timetable', query: { class: c.id } })"
        />
        <div v-else class="skel cpg__skel-body" />
      </div>

      <!-- Davomat -->
      <div v-else-if="tab === 'attendance'" class="cpg__card dash-card" data-tab-body="attendance">
        <div class="cpg__month-nav">
          <AppButton variant="ghost" icon="chevron_left" aria-label="Oldingi oy" @click="shiftMonth(-1)" />
          <b>{{ MONTHS[monthCursor.getMonth()] }} {{ monthCursor.getFullYear() }}</b>
          <AppButton variant="ghost" icon="chevron_right" aria-label="Keyingi oy" @click="shiftMonth(1)" />
        </div>
        <div class="cpg__month">
          <span v-for="dn in DAYS_SHORT" :key="dn" class="cpg__wd">{{ dn }}</span>
          <template v-for="w in monthGrid" :key="w[0]">
            <span
              v-for="date in w"
              :key="date"
              class="cpg__mday"
              :class="[{ 'cpg__mday--other': date.slice(5, 7) !== monthKey.slice(5, 7) }, monthHeat(date)]"
              :title="monthRates[date] !== undefined ? `${monthRates[date]}%` : ''"
              ><b>{{ Number(date.slice(8)) }}</b
              ><small v-if="monthRates[date] !== undefined && monthRates[date] !== null">{{ Math.round(monthRates[date]) }}%</small></span
            >
          </template>
        </div>
      </div>

      <!-- Ota-onalar -->
      <div v-else-if="tab === 'parents'" class="cpg__card dash-card" data-tab-body="parents">
        <p class="cpg__lead">
          Telegramga ulangan: <b>{{ parents.linked }}</b> / {{ d.students.length }} ·
          telefon yozilgan: <b>{{ parents.phones }}</b>
        </p>
        <table class="cpg__table">
          <thead>
            <tr>
              <th>Ota-ona</th>
              <th>Farzandi</th>
              <th>Telefon</th>
              <th class="num">Telegram</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="s in d.students" :key="s.id">
              <td>{{ s.guardianName || '—' }}<small v-if="s.relation"> · {{ RELATION[s.relation] || s.relation }}</small></td>
              <td>{{ s.name }}</td>
              <td>
                <a v-if="s.guardianPhone" :href="`tel:${s.guardianPhone}`">{{ formatPhone(s.guardianPhone) }}</a>
                <span v-else>—</span>
              </td>
              <td class="num" :class="s.telegram ? 'cpg__ok' : 'cpg__bad'">{{ s.telegram ? '✓ ulangan' : '✗ ulanmagan' }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Xulq -->
      <div v-else class="cpg__card dash-card" data-tab-body="behavior">
        <p class="cpg__lead">
          <span class="cpg__ok">👍 {{ behaviorCount.REWARD }} rag'bat</span> ·
          <span class="cpg__bad">⚠ {{ behaviorCount.WARNING }} ogohlantirish</span>
        </p>
        <EmptyState v-if="!d.behavior.length" icon="sentiment_satisfied" title="Xulq yozuvlari yo'q" />
        <SignalRow
          v-for="b in d.behavior"
          :key="b.id"
          :icon="b.type === 'REWARD' ? 'thumb_up' : 'warning'"
          :tone="b.type === 'REWARD' ? 'ok' : 'warn'"
          :title="`${b.student} — ${b.description || (b.type === 'REWARD' ? 'Rag\'bat' : 'Ogohlantirish')}`"
          :subtitle="`${b.date}${b.by ? ' · ' + b.by : ''}`"
        />
      </div>
    </template>
  </page-layout>
</template>

<script setup>
// One class in full: the hero (name, class teacher with phone, room, size; today's
// attendance, quality, mastery, parents on Telegram) and tabs kept in the URL — Umumiy
// (attendance trend, grades, subjects, students at risk), O'quvchilar, Jadval (the class's
// week), Davomat (a month), Ota-onalar and Xulq — with Davomat olish / Jurnal / Ota-onalarga
// xabar.
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Line } from 'vue-chartjs'
import { CategoryScale, Chart, Filler, LineElement, LinearScale, PointElement, Tooltip } from 'chart.js'
import PageLayout from '@/components/PageLayout.vue'
import AppButton from '@/components/app/AppButton.vue'
import ChartCard from '@/components/dashboard/ChartCard.vue'
import KpiCard from '@/components/dashboard/KpiCard.vue'
import ScoreRing from '@/components/dashboard/ScoreRing.vue'
import RiskRow from '@/components/dashboard/v3/RiskRow.vue'
import AvatarInitials from '@/components/pages/AvatarInitials.vue'
import EmptyState from '@/components/pages/EmptyState.vue'
import SignalRow from '@/components/pages/SignalRow.vue'
import SubjectChip from '@/components/pages/SubjectChip.vue'
import WeekGrid from '@/components/timetable/WeekGrid.vue'
import { WEEKDAYS, roomLabel, todayWeekday } from '@/components/timetable/timetable'
import { DAYS_SHORT, MONTHS, heatStep, iso, monthWeeks } from '@/components/calendar/calendar'
import { api } from '@/boot/axios'
import { useAuthStore } from '@/stores/auth'
import { useSchoolStore } from '@/stores/school'
import { useChartTheme, alpha } from '@/charts/chartTheme'
import { formatPercent } from '@/utils/format'
import { formatPhone, tashkentNow } from '@/utils/tashkent'

Chart.register(LineElement, PointElement, LinearScale, CategoryScale, Filler, Tooltip)

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const school = useSchoolStore()
const { themeKey, colors } = useChartTheme()

const TABS = [
  { value: 'overview', label: 'Umumiy' },
  { value: 'students', label: "O'quvchilar" },
  { value: 'schedule', label: 'Jadval' },
  { value: 'attendance', label: 'Davomat' },
  { value: 'parents', label: 'Ota-onalar' },
  { value: 'behavior', label: 'Xulq' }
]
const RELATION = { OTASI: 'otasi', ONASI: 'onasi', BOSHQA: 'boshqa' }

const id = computed(() => Number(route.params.id))
const tab = computed(() => (TABS.some(t => t.value === route.query.tab) ? route.query.tab : 'overview'))
function setTab(t) {
  router.replace({ query: { ...route.query, tab: t === 'overview' ? undefined : t } })
}

const d = ref(null)
const error = ref(null)
async function load() {
  if (!school.activeSchoolId || !id.value) return
  try {
    const res = await api.get(`/api/pages/classes/${id.value}`, { params: { schoolId: school.activeSchoolId } })
    d.value = res.data
    error.value = null
  } catch (e) {
    error.value = e.friendlyMessage || "Yuklab bo'lmadi"
  }
}
onMounted(load)
watch([id, () => school.activeSchoolId], () => {
  d.value = null
  load()
})
const c = computed(() => d.value?.card || {})

function openStudent(r) {
  router.push(`/profiles/student/${r.studentId}`)
}

// overview
const lineData = computed(() => ({
  labels: (d.value?.dynamics || []).map(p => `${Number(p.date.slice(8))}.${p.date.slice(5, 7)}`),
  datasets: [
    {
      data: (d.value?.dynamics || []).map(p => p.rate),
      borderColor: colors.value.primary,
      backgroundColor: alpha(colors.value.primary, 0.12),
      fill: true,
      borderWidth: 2,
      pointRadius: 0,
      pointHoverRadius: 4,
      tension: 0.3,
      cubicInterpolationMode: 'monotone',
      spanGaps: true
    }
  ]
}))
const lineOptions = {
  interaction: { mode: 'index', intersect: false },
  plugins: { tooltip: { callbacks: { label: i => `Davomat: ${formatPercent(i.raw)}` } } },
  scales: { y: { suggestedMin: 80, max: 100, ticks: { callback: v => `${v}%` } } }
}
const gradeTotal = computed(() => Object.values(d.value?.gradeCounts || {}).reduce((a, b) => a + b, 0))

// schedule
const week = ref(null)
const classLessons = computed(() => (week.value?.lessons || []).filter(l => l.classId === id.value))
const todayWd = todayWeekday()
const nowMin = tashkentNow().minutes
watch(
  tab,
  async t => {
    if (t === 'schedule' && !week.value) {
      const res = await api.get('/api/pages/timetable/week', { params: { schoolId: school.activeSchoolId } })
      week.value = res.data
    }
    if (t === 'attendance') loadMonth()
  },
  { immediate: true }
)

// attendance month
const monthCursor = ref(new Date(`${tashkentNow().date.slice(0, 7)}-01T12:00:00`))
const monthKey = computed(() => iso(monthCursor.value))
const monthGrid = computed(() => monthWeeks(monthCursor.value.getFullYear(), monthCursor.value.getMonth()))
const monthRates = ref({})
async function loadMonth() {
  const res = await api.get(`/api/pages/classes/${id.value}/month`, {
    params: { schoolId: school.activeSchoolId, date: monthKey.value }
  })
  monthRates.value = Object.fromEntries(res.data.map(p => [p.date, p.rate]))
}
function shiftMonth(n) {
  const x = new Date(monthCursor.value)
  x.setMonth(x.getMonth() + n)
  monthCursor.value = x
  loadMonth()
}
function monthHeat(date) {
  const s = heatStep(monthRates.value[date])
  return s ? `cpg__heat-${s}` : ''
}

// parents and behaviour
const parents = computed(() => ({
  linked: (d.value?.students || []).filter(s => s.telegram).length,
  phones: (d.value?.students || []).filter(s => s.guardianPhone).length
}))
const behaviorCount = computed(() => {
  const n = { REWARD: 0, WARNING: 0 }
  for (const b of d.value?.behavior || []) n[b.type] = (n[b.type] || 0) + 1
  return n
})
</script>

<style scoped>
.cpg :deep(.page-layout__body) {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.cpg__hero {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  padding: var(--space-5);
  background:
    radial-gradient(circle at 0 0, var(--dash-glow), transparent 60%),
    var(--card-bg);
}

.cpg__hero:hover {
  transform: none;
}

.cpg__id {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-4);
}

.cpg__titles {
  flex: 1 1 auto;
  min-width: 0;
}

.cpg__titles h1 {
  margin: 0;
  font-size: var(--text-xl);
  font-weight: 800;
  line-height: 1.2;
}

.cpg__facts {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-4);
  margin-top: var(--space-1);
  font-size: var(--text-sm);
  color: var(--brand-text-muted);
}

.cpg__facts a {
  margin-left: var(--space-1);
  color: var(--c-primary);
}

.cpg__actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.cpg__kpis {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--space-3);
}

.cpg__tabs {
  border-bottom: 1px solid var(--dash-edge);
}

.cpg__overview {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-3);
}

.cpg__line {
  position: relative;
  height: var(--chart-h);
}

.cpg__stack {
  display: flex;
  height: var(--stack-h);
  border-radius: var(--radius-sm);
  overflow: hidden;
  margin-top: var(--space-4);
}

.cpg__stack span {
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: var(--text-xs);
  font-weight: 700;
}

.cpg__stack-legend {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
  margin-top: var(--space-3);
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
}

.cpg__stack-legend i {
  display: inline-block;
  width: var(--legend-dot);
  height: var(--legend-dot);
  margin-right: var(--space-1);
  border-radius: var(--radius-pill);
}

.cpg__dumb {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.cpg__dumb li {
  display: grid;
  grid-template-columns: var(--name-col-sm) minmax(0, 1fr) auto;
  gap: var(--space-3);
  align-items: center;
}

.cpg__dumb small {
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
  font-variant-numeric: tabular-nums;
}

.cpg__dumb-track {
  position: relative;
  height: var(--dumbbell-h);
}

.cpg__dumb-track::before {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  top: 50%;
  border-top: 1px dashed var(--dash-edge);
}

.cpg__dumb-line {
  position: absolute;
  top: calc(50% - var(--hairline));
  height: var(--accent-bar);
  background: color-mix(in srgb, var(--ring-quality) 50%, transparent);
}

.cpg__dumb-dot {
  position: absolute;
  top: calc(50% - var(--dumbbell-dot) / 2);
  width: var(--dumbbell-dot);
  height: var(--dumbbell-dot);
  margin-left: calc(var(--dumbbell-dot) / -2);
  border-radius: var(--radius-pill);
}

.cpg__dumb-dot--q {
  background: var(--ring-quality);
}

.cpg__dumb-dot--m {
  background: var(--card-bg);
  border: var(--accent-bar) solid var(--ring-quality);
}

.cpg__card {
  padding: var(--space-4);
  overflow-x: auto;
}

.cpg__card:hover {
  transform: none;
}

.cpg__schedule {
  height: calc(var(--panel-min-h) + var(--space-8));
}

.cpg__table {
  width: 100%;
  border-collapse: collapse;
  font-size: var(--text-sm);
}

.cpg__table th {
  padding: var(--space-2);
  font-size: var(--text-xs);
  font-weight: 700;
  color: var(--brand-text-muted);
  text-align: left;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.cpg__table td {
  padding: var(--space-2);
  border-top: 1px solid var(--dash-edge);
}

.cpg__table tbody tr {
  cursor: pointer;
}

.cpg__table tbody tr:hover {
  background: var(--surface-2);
}

.cpg__table .num {
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.cpg__person {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-weight: 600;
}

.cpg__ok {
  color: var(--st-present);
  font-weight: 600;
}

.cpg__bad {
  color: var(--st-absent);
  font-weight: 600;
}

.cpg__lead {
  margin: 0 0 var(--space-3);
  font-size: var(--text-sm);
  color: var(--brand-text-muted);
}

.cpg__month-nav {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-bottom: var(--space-3);
}

.cpg__month {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: var(--space-1);
}

.cpg__wd {
  font-size: var(--text-xs);
  font-weight: 700;
  color: var(--brand-text-muted);
  text-align: center;
}

.cpg__mday {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: var(--day-min-h-sm);
  border-radius: var(--radius-sm);
  background: var(--surface-2);
  font-size: var(--text-sm);
}

.cpg__mday small {
  font-size: var(--text-xs);
  font-weight: 700;
}

.cpg__mday--other {
  visibility: hidden;
}

.cpg__heat-1 {
  background: var(--heat-1);
  color: var(--heat-1-text);
}

.cpg__heat-2 {
  background: var(--heat-2);
  color: var(--heat-2-text);
}

.cpg__heat-3 {
  background: var(--heat-3);
  color: var(--heat-3-text);
}

.cpg__heat-4 {
  background: var(--heat-4);
  color: var(--heat-4-text);
}

.cpg__skel {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.cpg__skel-hero {
  height: calc(var(--space-8) * 5);
  border-radius: var(--radius-lg);
}

.cpg__skel-body {
  height: var(--panel-min-h);
  border-radius: var(--radius-lg);
}

@media (max-width: 1023px) {
  .cpg__overview {
    grid-template-columns: minmax(0, 1fr);
  }

  .cpg__kpis {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>

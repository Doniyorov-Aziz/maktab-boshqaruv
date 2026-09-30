<template>
  <page-layout no-header>
    <div v-if="loading" class="brand-card q-pa-xl">
      <q-skeleton type="QAvatar" size="80px" class="q-mb-md" />
      <q-skeleton type="text" width="200px" />
      <q-skeleton type="text" width="140px" />
    </div>

    <template v-else-if="profile">
      <q-btn
        flat
        dense
        no-caps
        icon="arrow_back"
        label="Orqaga"
        class="q-mb-sm"
        @click="router.back()"
      />

      <div class="brand-card q-pa-md q-mb-md profile-header">
        <div class="row items-center q-col-gutter-md">
          <div class="col-auto">
            <q-avatar
              size="72px"
              text-color="white"
              :style="{ background: avatarColor(profile.fullName) }"
              class="text-h5"
            >
              {{ initials(profile.fullName) }}
            </q-avatar>
          </div>
          <div class="col">
            <div class="text-h5 text-weight-bold">{{ profile.fullName }}</div>
            <div class="text-body2 muted-text">
              {{ profile.className }} sinf · {{ profile.age }} yosh ·
              {{ formatDate(profile.birthDate) }}
            </div>
            <div
              v-if="profile.guardianName"
              class="text-caption muted-text q-mt-xs"
            >
              <q-icon name="family_restroom" size="14px" />
              {{ profile.guardianName }}
              <template v-if="profile.guardianPhone">
                · {{ profile.guardianPhone }}</template
              >
            </div>
          </div>
        </div>
      </div>

      <q-tabs
        v-model="tab"
        dense
        class="brand-card q-mb-md tabs-bar"
        active-color="primary"
        indicator-color="primary"
        align="left"
      >
        <q-tab name="overview" label="Umumiy" />
        <q-tab name="grades" label="Baholar" />
        <q-tab name="attendance" label="Davomat" />
        <q-tab name="behavior" label="Xulq" />
        <q-tab name="schedule" label="Dars jadvali" />
      </q-tabs>

      <q-tab-panels v-model="tab" animated class="bg-transparent">
        <q-tab-panel name="overview" class="q-pa-none">
          <div class="row q-col-gutter-md">
            <div class="col-6 col-sm-3">
              <div class="brand-card q-pa-md kpi-card">
                <div class="text-h4 text-weight-bold">{{
                  profile.attendanceRate != null
                    ? profile.attendanceRate + '%'
                    : '—'
                }}</div>
                <div class="text-caption muted-text">Davomat (90 kun)</div>
              </div>
            </div>
            <div class="col-6 col-sm-3">
              <div class="brand-card q-pa-md kpi-card">
                <div class="text-h4 text-weight-bold">{{
                  profile.averageGrade ?? '—'
                }}</div>
                <div class="text-caption muted-text">O'rtacha baho</div>
              </div>
            </div>
            <div class="col-6 col-sm-3">
              <div class="brand-card q-pa-md kpi-card">
                <div class="text-h4 text-weight-bold text-positive">{{
                  profile.rewardCount
                }}</div>
                <div class="text-caption muted-text">Rag'batlar</div>
              </div>
            </div>
            <div class="col-6 col-sm-3">
              <div class="brand-card q-pa-md kpi-card">
                <div class="text-h4 text-weight-bold text-negative">{{
                  profile.warningCount
                }}</div>
                <div class="text-caption muted-text">Ogohlantirishlar</div>
              </div>
            </div>
          </div>

          <!-- Link codes let the holder subscribe to this child's data, so the
               block is only shown to staff who can manage it (ADMIN/EDITOR). -->
          <telegram-student-card
            v-if="authStore.isEditor"
            :student-id="profile.id"
            class="q-mt-md"
          />
        </q-tab-panel>

        <q-tab-panel name="grades" class="q-pa-none">
          <div class="brand-card q-pa-md">
            <div class="text-subtitle1 text-weight-semibold q-mb-md"
              >Fanlar bo'yicha o'rtacha baholar</div
            >
            <div v-if="!profile.subjectAverages.length" class="muted-text"
              >Hali baholar yo'q</div
            >
            <div v-else class="q-gutter-sm">
              <div
                v-for="s in profile.subjectAverages"
                :key="s.subjectId"
                class="row items-center subject-row"
              >
                <div class="col-4 col-sm-3 ellipsis">{{ s.subjectName }}</div>
                <div class="col-6 col-sm-8">
                  <q-linear-progress
                    :value="(s.averageScore || 0) / 5"
                    size="14px"
                    rounded
                    :color="gradeColor(s.averageScore)"
                    track-color="grey-3"
                  />
                </div>
                <div class="col-2 col-sm-1 text-right text-weight-bold">{{
                  s.averageScore
                }}</div>
              </div>
            </div>
          </div>
        </q-tab-panel>

        <q-tab-panel name="attendance" class="q-pa-none">
          <div class="brand-card q-pa-md">
            <div class="text-subtitle1 text-weight-semibold q-mb-md"
              >Oxirgi 90 kun davomati</div
            >
            <div class="heatmap">
              <div
                v-for="(week, wi) in heatmapWeeks"
                :key="wi"
                class="heatmap-col"
              >
                <div
                  v-for="day in week"
                  :key="day.date"
                  class="heatmap-cell"
                  :class="`hm-${day.status}`"
                >
                  <q-tooltip
                    >{{ day.date }} — {{ statusLabel(day.status) }}</q-tooltip
                  >
                </div>
              </div>
            </div>
            <div
              class="row items-center q-gutter-xs q-mt-md text-caption muted-text"
            >
              <span>Kam</span>
              <div class="heatmap-cell hm-NONE" />
              <div class="heatmap-cell hm-ABSENT" />
              <div class="heatmap-cell hm-PARTIAL" />
              <div class="heatmap-cell hm-LATE" />
              <div class="heatmap-cell hm-PRESENT" />
              <span>Ko'p</span>
            </div>
          </div>
        </q-tab-panel>

        <q-tab-panel name="behavior" class="q-pa-none">
          <div class="brand-card q-pa-md">
            <q-list v-if="profile.behaviorRecords.length" separator>
              <q-item v-for="b in profile.behaviorRecords" :key="b.id">
                <q-item-section avatar>
                  <q-icon
                    :name="b.type === 'REWARD' ? 'star' : 'warning'"
                    :color="b.type === 'REWARD' ? 'positive' : 'negative'"
                  />
                </q-item-section>
                <q-item-section>
                  <q-item-label>{{ b.description }}</q-item-label>
                  <q-item-label caption>{{
                    formatDate(b.recordDate)
                  }}</q-item-label>
                </q-item-section>
              </q-item>
            </q-list>
            <div v-else class="muted-text">Xulq yozuvlari yo'q</div>
          </div>
        </q-tab-panel>

        <q-tab-panel name="schedule" class="q-pa-none">
          <div class="brand-card q-pa-md">
            <q-list v-if="profile.weeklySchedule.length" separator>
              <q-item v-for="l in sortedSchedule" :key="l.lessonSlotId">
                <q-item-section avatar>
                  <q-badge
                    :style="{ background: subjectColor(l.subjectName) }"
                    >{{ l.weekday.slice(0, 3) }}</q-badge
                  >
                </q-item-section>
                <q-item-section>
                  <q-item-label
                    >{{ l.subjectName }} · {{ l.teacherName }}</q-item-label
                  >
                  <q-item-label caption
                    >{{ l.startTime.slice(0, 5) }}–{{ l.endTime.slice(0, 5) }} ·
                    {{ l.roomNumber }}-xona</q-item-label
                  >
                </q-item-section>
              </q-item>
            </q-list>
            <div v-else class="muted-text">Dars jadvali topilmadi</div>
          </div>
        </q-tab-panel>
      </q-tab-panels>
    </template>
  </page-layout>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '@/boot/axios'
import PageLayout from '@/components/PageLayout.vue'
import TelegramStudentCard from '@/components/TelegramStudentCard.vue'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const profile = ref(null)
const loading = ref(true)
const tab = ref('overview')

const weekdayOrder = {
  Dushanba: 0,
  Seshanba: 1,
  Chorshanba: 2,
  Payshanba: 3,
  Juma: 4,
  Shanba: 5
}
const sortedSchedule = computed(() =>
  [...(profile.value?.weeklySchedule || [])].sort(
    (a, b) =>
      weekdayOrder[a.weekday] - weekdayOrder[b.weekday] ||
      a.startTime.localeCompare(b.startTime)
  )
)

const heatmapWeeks = computed(() => {
  const days = profile.value?.attendanceHeatmap || []
  const weeks = []
  for (let i = 0; i < days.length; i += 7) {
    weeks.push(days.slice(i, i + 7))
  }
  return weeks
})

function statusLabel(s) {
  return (
    {
      PRESENT: 'Keldi',
      LATE: 'Kechikdi',
      PARTIAL: 'Qisman',
      ABSENT: 'Kelmadi',
      NONE: "Ma'lumot yo'q"
    }[s] || s
  )
}

function initials(name) {
  return (name || '')
    .split(' ')
    .map(p => p.charAt(0))
    .join('')
    .slice(0, 2)
    .toUpperCase()
}

const palette = [
  '#4f46e5',
  '#0ea5e9',
  '#10b981',
  '#f59e0b',
  '#ec4899',
  '#8b5cf6',
  '#14b8a6'
]
function avatarColor(name) {
  let hash = 0
  for (const ch of name || '')
    hash = (hash * 31 + ch.charCodeAt(0)) % palette.length
  return palette[Math.abs(hash) % palette.length]
}
function subjectColor(name) {
  return avatarColor(name)
}

function gradeColor(score) {
  if (score >= 4.5) return 'positive'
  if (score >= 3.5) return 'primary'
  if (score >= 2.5) return 'warning'
  return 'negative'
}

function formatDate(d) {
  if (!d) return ''
  const [y, m, day] = d.split('-')
  return `${day}.${m}.${y}`
}

async function load() {
  loading.value = true
  try {
    const response = await api.get(`/api/profiles/students/${route.params.id}`)
    profile.value = response.data
  } finally {
    loading.value = false
  }
}

load()
</script>

<style scoped>
.muted-text {
  color: var(--brand-text-muted);
}

.tabs-bar {
  padding: 4px 8px;
}

.kpi-card {
  text-align: center;
}

.subject-row {
  padding: 4px 0;
}

.heatmap {
  display: flex;
  gap: 3px;
  overflow-x: auto;
}

.heatmap-col {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.heatmap-cell {
  width: 12px;
  height: 12px;
  border-radius: 3px;
}

.hm-NONE {
  background: rgba(148, 163, 184, 0.2);
}

.hm-ABSENT {
  background: #ef4444;
}

.hm-PARTIAL {
  background: #f59e0b;
}

.hm-LATE {
  background: #eab308;
}

.hm-PRESENT {
  background: #10b981;
}
</style>

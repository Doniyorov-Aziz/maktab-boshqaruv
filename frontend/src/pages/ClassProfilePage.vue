<template>
  <page-layout no-header>
    <div v-if="loading" class="brand-card q-pa-xl">
      <q-skeleton type="text" width="200px" />
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
              size="64px"
              color="primary"
              text-color="white"
              icon="groups"
            />
          </div>
          <div class="col">
            <div class="text-h5 text-weight-bold"
              >{{ profile.className }} sinf</div
            >
            <div class="text-body2 muted-text">{{
              profile.academicYearTitle
            }}</div>
            <div v-if="profile.classTeacherName" class="text-caption q-mt-xs">
              <q-icon name="badge" size="14px" /> Sinf rahbari:
              {{ profile.classTeacherName }}
            </div>
          </div>
        </div>
      </div>

      <div class="row q-col-gutter-md q-mb-md">
        <div class="col-6 col-sm-3">
          <div class="brand-card q-pa-md kpi-card">
            <div class="text-h4 text-weight-bold">{{
              profile.studentCount
            }}</div>
            <div class="text-caption muted-text">O'quvchilar</div>
          </div>
        </div>
        <div class="col-6 col-sm-3">
          <div class="brand-card q-pa-md kpi-card">
            <div class="text-h4 text-weight-bold">{{
              profile.attendanceRate != null
                ? profile.attendanceRate + '%'
                : '—'
            }}</div>
            <div class="text-caption muted-text">Davomat (30 kun)</div>
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
      </div>

      <q-tabs
        v-model="tab"
        dense
        class="brand-card q-mb-md tabs-bar"
        active-color="primary"
        indicator-color="primary"
        align="left"
      >
        <q-tab name="students" label="O'quvchilar" />
        <q-tab name="schedule" label="Dars jadvali" />
      </q-tabs>

      <q-tab-panels v-model="tab" animated class="bg-transparent">
        <q-tab-panel name="students" class="q-pa-none">
          <div class="brand-card overflow-hidden">
            <q-list separator>
              <q-item
                v-for="s in profile.students"
                :key="s.id"
                clickable
                @click="router.push(`/profiles/student/${s.id}`)"
              >
                <q-item-section avatar>
                  <q-avatar
                    size="34px"
                    text-color="white"
                    :style="{ background: avatarColor(s.fullName) }"
                  >
                    {{ initials(s.fullName) }}
                  </q-avatar>
                </q-item-section>
                <q-item-section>
                  <q-item-label>{{ s.fullName }}</q-item-label>
                </q-item-section>
              </q-item>
            </q-list>
          </div>
        </q-tab-panel>

        <q-tab-panel name="schedule" class="q-pa-none">
          <div class="brand-card q-pa-md">
            <q-list separator>
              <q-item v-for="l in sortedSchedule" :key="l.lessonSlotId">
                <q-item-section avatar>
                  <q-badge
                    :style="{ background: avatarColor(l.subjectName) }"
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

const route = useRoute()
const router = useRouter()

const profile = ref(null)
const loading = ref(true)
const tab = ref('students')

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

async function load() {
  loading.value = true
  try {
    const response = await api.get(`/api/profiles/classes/${route.params.id}`)
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
</style>

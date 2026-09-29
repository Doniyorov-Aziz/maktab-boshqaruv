<template>
  <page-layout no-header>
    <div v-if="loading" class="brand-card q-pa-xl">
      <q-skeleton type="QAvatar" size="80px" class="q-mb-md" />
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
              {{ profile.positionTitle }} · {{ profile.phone }}
            </div>
            <div v-if="profile.homeroomClassName" class="text-caption q-mt-xs">
              <q-badge color="primary" outline
                >{{ profile.homeroomClassName }} sinf rahbari</q-badge
              >
            </div>
          </div>
        </div>
      </div>

      <div class="row q-col-gutter-md q-mb-md">
        <div class="col-6 col-sm-3">
          <div class="brand-card q-pa-md kpi-card">
            <div class="text-h4 text-weight-bold">{{
              profile.weeklyLoadCount
            }}</div>
            <div class="text-caption muted-text">Haftalik soat</div>
          </div>
        </div>
        <div class="col-6 col-sm-3">
          <div class="brand-card q-pa-md kpi-card">
            <div class="text-h4 text-weight-bold">{{
              profile.subjects.length
            }}</div>
            <div class="text-caption muted-text">Fanlar</div>
          </div>
        </div>
        <div class="col-6 col-sm-3">
          <div class="brand-card q-pa-md kpi-card">
            <div class="text-h4 text-weight-bold">{{
              profile.classes.length
            }}</div>
            <div class="text-caption muted-text">Sinflar</div>
          </div>
        </div>
        <div class="col-6 col-sm-3">
          <div class="brand-card q-pa-md kpi-card">
            <div class="text-h4 text-weight-bold">{{
              profile.todayLessons.length
            }}</div>
            <div class="text-caption muted-text">Bugungi darslar</div>
          </div>
        </div>
      </div>

      <div class="row q-col-gutter-md">
        <div class="col-12 col-md-5">
          <div class="brand-card q-pa-md">
            <div class="text-subtitle1 text-weight-semibold q-mb-sm"
              >Bugungi darslar</div
            >
            <q-list v-if="sortedToday.length" separator>
              <q-item v-for="l in sortedToday" :key="l.lessonSlotId">
                <q-item-section>
                  <q-item-label
                    >{{ l.subjectName }} · {{ l.className }}</q-item-label
                  >
                  <q-item-label caption
                    >{{ l.startTime.slice(0, 5) }}–{{ l.endTime.slice(0, 5) }} ·
                    {{ l.roomNumber }}-xona</q-item-label
                  >
                </q-item-section>
              </q-item>
            </q-list>
            <div v-else class="muted-text">Bugun dars yo'q</div>
          </div>
        </div>
        <div class="col-12 col-md-7">
          <div class="brand-card q-pa-md">
            <div class="text-subtitle1 text-weight-semibold q-mb-sm"
              >Haftalik jadval</div
            >
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
                    >{{ l.subjectName }} · {{ l.className }}</q-item-label
                  >
                  <q-item-label caption
                    >{{ l.startTime.slice(0, 5) }}–{{ l.endTime.slice(0, 5) }} ·
                    {{ l.roomNumber }}-xona</q-item-label
                  >
                </q-item-section>
              </q-item>
            </q-list>
          </div>
        </div>
      </div>
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
const sortedToday = computed(() =>
  [...(profile.value?.todayLessons || [])].sort((a, b) =>
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
    const response = await api.get(`/api/profiles/teachers/${route.params.id}`)
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

.kpi-card {
  text-align: center;
}
</style>

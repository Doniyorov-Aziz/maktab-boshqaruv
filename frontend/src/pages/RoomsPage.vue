<template>
  <page-layout
    icon="meeting_room"
    color="var(--room-busy)"
    title="Xonalar"
    :subtitle="subtitle"
    class="rmp"
  >
    <template #actions>
      <AppButton
        icon="travel_explore"
        label="Bo'sh xona topish"
        data-open-finder
        @click="finderOpen = true"
      />
      <AppButton
        v-if="auth.isEditor"
        variant="secondary"
        icon="tune"
        label="Boshqarish"
        to="/app/rooms"
      />
    </template>

    <section class="rmp__hero" data-rooms-hero>
      <KpiCard
        kpi-key="total"
        compact
        label="Jami xonalar"
        icon="meeting_room"
        color="var(--chart-secondary)"
        :loading="!live"
        :value="k?.total"
        :note="k ? `${k.active} faol · ${buildingsCount} bino` : ''"
        :spark="null"
      />
      <KpiCard
        kpi-key="busy"
        compact
        label="Hozir band"
        icon="groups"
        color="var(--room-busy)"
        :loading="!live"
        :value="k?.busyPct ?? undefined"
        :empty-value="!!k && k.busyPct === null"
        :format="v => `${v.toFixed(0)}%`"
        :note="k ? `${k.busy} / ${k.active} xona · ${timeLabel}` : ''"
        :spark="busySpark"
        :spark-min="0"
        :spark-max="100"
      />
      <KpiCard
        kpi-key="week"
        compact
        label="Haftalik bandlik"
        icon="date_range"
        color="var(--chart-primary)"
        :loading="!live"
        :value="k?.weekUse ?? undefined"
        :empty-value="!!k && k.weekUse === null"
        :format="v => `${v.toFixed(1)}%`"
        note="xona × dars × 6 kun"
        :spark="null"
      />
      <KpiCard
        kpi-key="repair"
        compact
        label="Ta'mirda"
        icon="construction"
        :color="k?.repair ? 'var(--st-late)' : 'var(--st-present)'"
        clickable
        :loading="!live"
        :value="k?.repair"
        :note="
          k ? (k.repair ? 'bosing — ko\'rsatish' : 'hammasi ishlayapti') : ''
        "
        :spark="null"
        @click="stateFilter = stateFilter === 'REPAIR' ? null : 'REPAIR'"
      />
    </section>

    <!-- when: now, or a day and a lesson -->
    <div class="rmp__bar">
      <SegTabs
        :model-value="mode"
        :options="MODES"
        label="Vaqt"
        data-rooms-mode
        @update:model-value="setMode"
      />
      <template v-if="mode === 'pick'">
        <SegTabs v-model="pickDay" :options="DAY_OPTIONS" label="Kun" />
        <div v-if="live?.periods.length" class="rmp__slider">
          <q-slider
            v-model="pickPeriod"
            :min="1"
            :max="live.periods.length"
            :step="1"
            markers
            snap
            label
            :label-value="`${pickPeriod}-dars · ${hmP(pickPeriod)}`"
            data-rooms-slider
          />
        </div>
      </template>
      <span class="rmp__clock" data-rooms-clock>
        <span class="rmp__pulse" v-if="mode === 'now'" /> {{ timeLabel }}
        <small v-if="mode === 'now'">· har daqiqa yangilanadi</small>
      </span>
      <div class="rmp__states" role="group" aria-label="Holat">
        <button
          v-for="(s, key) in STATES"
          :key="key"
          type="button"
          class="rmp__state"
          :class="{ 'rmp__state--on': stateFilter === key }"
          :style="{ '--c': s.color }"
          :aria-pressed="stateFilter === key"
          :data-state-filter="key"
          @click="stateFilter = stateFilter === key ? null : key"
        >
          <span class="rmp__sdot" /> {{ s.label }} <b>{{ counts[key] || 0 }}</b>
        </button>
      </div>
    </div>

    <!-- the live map -->
    <section class="rmp__map" data-rooms-map>
      <div v-if="!live && !error" class="rmp__skel">
        <div v-for="i in 3" :key="i" class="skel rmp__skel-row" />
      </div>
      <EmptyState
        v-else-if="error && !live"
        icon="cloud_off"
        title="Xonalarni yuklab bo'lmadi"
        :text="error"
        action="Qayta urinish"
        action-icon="refresh"
        @action="load"
      />
      <EmptyState
        v-else-if="live && !live.buildings.length"
        icon="meeting_room"
        title="Hali xona kiritilmagan"
        :action="auth.isEditor ? 'Xona qo\'shish' : ''"
        @action="$router.push('/app/rooms?create=1')"
      />
      <div
        v-for="b in shownBuildings"
        :key="b.id"
        class="rmp__building dash-card"
        :data-building="b.id"
      >
        <h3><q-icon name="apartment" /> {{ b.name }}</h3>
        <div
          v-for="f in b.floors"
          :key="String(f.floor)"
          class="rmp__floor"
          :data-floor="f.floor"
        >
          <span class="rmp__flabel">{{
            f.floor === null ? '—' : `${f.floor}-qavat`
          }}</span>
          <div class="rmp__tiles">
            <RoomTile
              v-for="r in f.rooms"
              :key="r.room.id"
              :r="r"
              @open="openRoom"
            />
          </div>
        </div>
      </div>
    </section>

    <!-- analytics -->
    <section class="rmp__analytics">
      <ChartCard
        title="Eng band 5 xona"
        subtitle="haftalik bandlik"
        :loading="analytics.loading.value"
        :data="an"
        :empty="!!an && !an.busiest.length"
      >
        <ul class="rmp__bars">
          <li v-for="u in an?.busiest || []" :key="u.id">
            <span>{{ roomLabel(u.number) }}</span>
            <span class="rmp__track"
              ><span
                :style="{
                  width: `${u.use || 0}%`,
                  background: 'var(--room-busy)'
                }"
            /></span>
            <b>{{ formatPercent(u.use) }}</b>
          </li>
        </ul>
      </ChartCard>
      <ChartCard
        title="Eng kam ishlatiladigan 5 xona"
        subtitle="bo'sh turgan joylar"
        :loading="analytics.loading.value"
        :data="an"
        :empty="!!an && !an.quietest.length"
      >
        <ul class="rmp__bars">
          <li v-for="u in an?.quietest || []" :key="u.id">
            <span>{{ roomLabel(u.number) }}</span>
            <span class="rmp__track"
              ><span
                :style="{
                  width: `${u.use || 0}%`,
                  background: 'var(--room-free)'
                }"
            /></span>
            <b>{{ formatPercent(u.use) }}</b>
          </li>
        </ul>
      </ChartCard>
      <ChartCard
        title="Tur bo'yicha bandlik"
        subtitle="o'rtacha"
        :loading="analytics.loading.value"
        :data="an"
        :empty="!!an && !an.byType.length"
      >
        <ul class="rmp__bars">
          <li v-for="t in an?.byType || []" :key="t.type">
            <span
              ><q-icon :name="ROOM_TYPES[t.type]?.icon" />
              {{ ROOM_TYPES[t.type]?.label || t.type }}
              <small>({{ t.rooms }})</small></span
            >
            <span class="rmp__track"
              ><span
                :style="{
                  width: `${t.use || 0}%`,
                  background: 'var(--chart-secondary)'
                }"
            /></span>
            <b>{{ formatPercent(t.use) }}</b>
          </li>
        </ul>
      </ChartCard>
    </section>

    <RoomFinder
      v-model="finderOpen"
      :periods="live?.periods || []"
      :can-edit="auth.isEditor"
      @book="openBooking"
    />
    <RoomDrawer
      v-model="roomOpen"
      :room="room"
      :can-edit="auth.isEditor"
      @book="r => openBooking({ room: r })"
      @changed="load"
    />
    <BookingDialog
      v-model="bookingOpen"
      :room="bookingRoom"
      :preset="bookingPreset"
      @saved="afterBooking"
    />
  </page-layout>
</template>

<script setup>
// "Xonalar": the hero (rooms, busy now, weekly use, under repair) and the live map — every
// building, a row per floor (top floor first, rooms in number order), each room busy with a
// lesson until when, booked, free until the next lesson, or under repair. It refreshes every
// minute; "Tanlash" shows any day and lesson instead. "Bo'sh xona topish" finds and books a
// free room; a room opens its week and bookings. Analytics: the busiest and quietest rooms.
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PageLayout from '@/components/PageLayout.vue'
import AppButton from '@/components/app/AppButton.vue'
import ChartCard from '@/components/dashboard/ChartCard.vue'
import KpiCard from '@/components/dashboard/KpiCard.vue'
import SegTabs from '@/components/dashboard/SegTabs.vue'
import EmptyState from '@/components/pages/EmptyState.vue'
import RoomTile from '@/components/rooms/RoomTile.vue'
import RoomFinder from '@/components/rooms/RoomFinder.vue'
import RoomDrawer from '@/components/rooms/RoomDrawer.vue'
import BookingDialog from '@/components/rooms/BookingDialog.vue'
import { ROOM_TYPES, STATES, hm } from '@/components/rooms/rooms'
import {
  WEEKDAYS,
  WEEKDAY_SHORT,
  nextDateOf,
  roomLabel,
  todayWeekday
} from '@/components/timetable/timetable'
import { useWidget } from '@/composables/useWidget'
import { api } from '@/boot/axios'
import { useAuthStore } from '@/stores/auth'
import { useSchoolStore } from '@/stores/school'
import { formatPercent } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const school = useSchoolStore()

const MODES = [
  { value: 'now', label: 'Hozir' },
  { value: 'pick', label: 'Tanlash' }
]
const DAY_OPTIONS = WEEKDAYS.map((d, i) => ({
  value: d,
  label: WEEKDAY_SHORT[i]
}))

const mode = computed(() => (route.query.at === 'pick' ? 'pick' : 'now'))
function setMode(m) {
  router.replace({ query: { ...route.query, at: m === 'now' ? undefined : m } })
}
const pickDay = ref(todayWeekday() || 'Dushanba')
const pickPeriod = ref(1)

const live = ref(null)
const error = ref(null)
const busySpark = ref([])

const params = computed(() => {
  if (mode.value === 'now' || !live.value?.periods.length) return {}
  const p =
    live.value.periods[
      Math.min(pickPeriod.value, live.value.periods.length) - 1
    ]
  // the middle of the lesson
  const [h1, m1] = hm(p.start).split(':').map(Number)
  const [h2, m2] = hm(p.end).split(':').map(Number)
  const mid = Math.round((h1 * 60 + m1 + h2 * 60 + m2) / 2)
  return {
    date: nextDateOf(pickDay.value),
    time: `${String(Math.floor(mid / 60)).padStart(2, '0')}:${String(mid % 60).padStart(2, '0')}`
  }
})

async function load() {
  if (!school.activeSchoolId) return
  try {
    const res = await api.get('/api/pages/rooms/live', {
      params: { schoolId: school.activeSchoolId, ...params.value },
      background: !!live.value
    })
    live.value = res.data
    error.value = null
    if (mode.value === 'now' && res.data.kpis.busyPct !== null)
      busySpark.value = [...busySpark.value, res.data.kpis.busyPct].slice(-20)
  } catch (e) {
    error.value = e.friendlyMessage || "Yuklab bo'lmadi"
  }
}
let timer = null
onMounted(() => {
  load()
  timer = setInterval(() => {
    if (mode.value === 'now' && document.visibilityState === 'visible') load()
  }, 60_000)
})
onBeforeUnmount(() => clearInterval(timer))
watch([mode, pickDay, pickPeriod, () => school.activeSchoolId], load)

const analytics = useWidget('/api/pages/rooms/analytics')
const an = computed(() => analytics.data.value)

const k = computed(() => live.value?.kpis)
const buildingsCount = computed(() => live.value?.buildings.length || 0)
const timeLabel = computed(() => {
  if (!live.value) return ''
  const t = hm(live.value.time)
  if (mode.value === 'now') return `hozir ${t}`
  return `${live.value.weekday || ''}, ${pickPeriod.value}-dars (${t})`
})
function hmP(n) {
  const p = live.value?.periods[n - 1]
  return p ? `${hm(p.start)}–${hm(p.end)}` : ''
}
const subtitle = computed(() =>
  [school.activeSchoolName, live.value ? `${live.value.kpis.total} xona` : '']
    .filter(Boolean)
    .join(' · ')
)

const stateFilter = ref(null)
const counts = computed(() => {
  const c = {}
  for (const b of live.value?.buildings || [])
    for (const f of b.floors)
      for (const r of f.rooms) c[r.state] = (c[r.state] || 0) + 1
  return c
})
const shownBuildings = computed(() =>
  (live.value?.buildings || [])
    .map(b => ({
      ...b,
      floors: b.floors
        .map(f => ({
          ...f,
          rooms: f.rooms.filter(
            r => !stateFilter.value || r.state === stateFilter.value
          )
        }))
        .filter(f => f.rooms.length)
    }))
    .filter(b => b.floors.length)
)

const roomOpen = ref(false)
const room = ref(null)
function openRoom(r) {
  room.value = { ...r.room, number: r.room.number }
  roomOpen.value = true
}

const finderOpen = ref(false)
const bookingOpen = ref(false)
const bookingRoom = ref(null)
const bookingPreset = ref(null)
function openBooking({ room: r, preset = null }) {
  bookingRoom.value = r
  bookingPreset.value = preset
  bookingOpen.value = true
}
function afterBooking() {
  load()
  analytics.reload()
  if (roomOpen.value) roomOpen.value = false
}
</script>

<style scoped>
.rmp :deep(.page-layout__body) {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.rmp__hero {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--space-3);
}

.rmp__bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-3);
}

.rmp__slider {
  width: var(--field-w-lg);
  padding: 0 var(--space-3);
}

.rmp__clock {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  font-size: var(--text-sm);
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.rmp__clock small {
  font-weight: 500;
  color: var(--brand-text-muted);
}

.rmp__pulse {
  width: var(--dot);
  height: var(--dot);
  border-radius: var(--radius-pill);
  background: var(--now-line);
  animation: rmp-pulse 1.6s ease-in-out infinite;
}

@keyframes rmp-pulse {
  50% {
    opacity: 0.3;
  }
}

.rmp__states {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-1);
  margin-left: auto;
}

.rmp__state {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  padding: var(--space-1) var(--space-2);
  border: 1px solid color-mix(in srgb, var(--c) 35%, transparent);
  border-radius: var(--radius-pill);
  background: transparent;
  color: var(--text-primary);
  font: inherit;
  font-size: var(--text-xs);
  font-weight: 600;
  cursor: pointer;
}

.rmp__state--on {
  background: color-mix(in srgb, var(--c) 16%, transparent);
  border-color: var(--c);
}

.rmp__sdot {
  width: var(--dot);
  height: var(--dot);
  border-radius: var(--radius-pill);
  background: var(--c);
}

.rmp__state b {
  color: var(--brand-text-muted);
}

.rmp__map {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.rmp__building {
  padding: var(--space-4) var(--space-5);
}

.rmp__building:hover {
  transform: none;
}

.rmp__building h3 {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin: 0 0 var(--space-3);
  font-size: var(--text-md);
  font-weight: 700;
}

.rmp__floor {
  display: grid;
  grid-template-columns: var(--tl-name-w) minmax(0, 1fr);
  gap: var(--space-3);
  align-items: start;
  padding: var(--space-2) 0;
  border-top: 1px dashed var(--dash-edge);
}

.rmp__floor:first-of-type {
  border-top: 0;
}

.rmp__flabel {
  padding-top: var(--space-2);
  font-size: var(--text-xs);
  font-weight: 700;
  color: var(--brand-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.rmp__tiles {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.rmp__analytics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--space-3);
}

.rmp__bars {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.rmp__bars li {
  display: grid;
  grid-template-columns: var(--name-col-sm) minmax(0, 1fr) var(--space-8);
  gap: var(--space-2);
  align-items: center;
  font-size: var(--text-sm);
}

.rmp__bars li span:first-child {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.rmp__bars b {
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.rmp__track {
  height: var(--bar-thin);
  border-radius: var(--radius-pill);
  background: var(--tile-none);
  overflow: hidden;
}

.rmp__track span {
  display: block;
  height: 100%;
  border-radius: var(--radius-pill);
}

.rmp__skel {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.rmp__skel-row {
  height: calc(var(--room-tile-h) * 2);
  border-radius: var(--radius-lg);
}

@media (max-width: 1023px) {
  .rmp__hero {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .rmp__analytics {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 599px) {
  .rmp__floor {
    grid-template-columns: minmax(0, 1fr);
  }

  .rmp__states {
    margin-left: 0;
  }

  .rmp__slider {
    width: 100%;
  }
}

@media (prefers-reduced-motion: reduce) {
  .rmp__pulse {
    animation: none;
  }
}
</style>

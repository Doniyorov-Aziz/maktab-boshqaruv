<template>
  <SideDrawer
    :model-value="modelValue"
    :title="room ? roomLabel(room.number) : ''"
    :subtitle="
      room
        ? `${ROOM_TYPES[room.type]?.label} · ${room.building}${room.floor ? ` · ${room.floor}-qavat` : ''}`
        : ''
    "
    :icon="room ? ROOM_TYPES[room.type]?.icon : 'meeting_room'"
    color="var(--room-busy)"
    @update:model-value="v => emit('update:modelValue', v)"
  >
    <div v-if="!d" class="rd__skel">
      <div v-for="i in 5" :key="i" class="skel rd__skel-line" />
    </div>
    <div v-else class="rd" data-room-drawer>
      <div class="rd__facts">
        <span class="rd__fact"
          ><q-icon name="person" /> {{ d.room.capacity }} o'rin</span
        >
        <span
          class="rd__fact"
          :class="{ 'rd__fact--repair': d.room.status === 'REPAIR' }"
        >
          <q-icon
            :name="d.room.status === 'REPAIR' ? 'construction' : 'check_circle'"
          />
          {{ d.room.status === 'REPAIR' ? "Ta'mirda" : 'Faol' }}
        </span>
        <span v-for="t in d.room.equipment" :key="t" class="rd__fact">
          <q-icon :name="EQUIPMENT[t]?.icon" /> {{ EQUIPMENT[t]?.label || t }}
        </span>
        <span v-if="d.room.computers" class="rd__fact"
          ><q-icon name="computer" /> {{ d.room.computers }} kompyuter</span
        >
      </div>

      <section>
        <h4>
          Haftalik bandlik <b>{{ d.use === null ? '—' : `${d.use}%` }}</b>
        </h4>
        <div
          class="rd__heat"
          :style="{
            gridTemplateColumns: `var(--space-6) repeat(${d.periods.length}, minmax(0, 1fr))`
          }"
          data-room-heat
        >
          <span />
          <span v-for="p in d.periods" :key="p.n" class="rd__ph">{{
            p.n
          }}</span>
          <template v-for="(row, k) in d.week" :key="k">
            <span class="rd__dh">{{ WEEKDAY_SHORT[k] }}</span>
            <span
              v-for="(cell, j) in row"
              :key="j"
              class="rd__cell"
              :class="{ 'rd__cell--on': cell }"
              :style="
                cell
                  ? { '--c': subjectColor(cell.subjectId, cell.subjectColor) }
                  : undefined
              "
              :title="
                cell
                  ? `${WEEKDAYS[k]}, ${j + 1}-dars: ${cell.className} · ${cell.subject} · ${cell.teacher}`
                  : `${WEEKDAYS[k]}, ${j + 1}-dars: bo'sh`
              "
              >{{ cell ? cell.className : '' }}</span
            >
          </template>
        </div>
      </section>

      <section v-if="d.classes.length">
        <h4>Shu xonada dars o'tadigan sinflar</h4>
        <div class="rd__chips">
          <router-link
            v-for="c in d.classes"
            :key="c.id"
            :to="`/classes/${c.id}`"
            class="rd__chip"
            >{{ c.name }} <b>{{ c.lessons }}</b></router-link
          >
        </div>
      </section>
      <section v-if="d.teachers.length">
        <h4>O'qituvchilar</h4>
        <div class="rd__chips">
          <span v-for="t in d.teachers" :key="t.id" class="rd__chip"
            >{{ t.name }} <b>{{ t.lessons }}</b></span
          >
        </div>
      </section>

      <section>
        <h4>Yaqin bandlar (30 kun)</h4>
        <EmptyState
          v-if="!d.bookings.length"
          icon="event_available"
          title="Band qilinmagan"
        />
        <div
          v-for="b in d.bookings"
          :key="b.id"
          class="rd__booking"
          data-booking
        >
          <span class="rd__bdate">
            <b>{{ Number(b.date.slice(8)) }}.{{ b.date.slice(5, 7) }}</b>
            <small>{{ hm(b.startTime) }}–{{ hm(b.endTime) }}</small>
          </span>
          <span class="rd__bwhat"
            ><b>{{ b.purpose }}</b
            ><small
              >{{ b.responsible || b.createdBy || ''
              }}{{ b.calendarEventId ? ' · taqvimdan' : '' }}</small
            ></span
          >
          <AppButton
            v-if="canEdit && !b.calendarEventId"
            variant="ghost"
            size="sm"
            icon="delete"
            aria-label="Bandni bekor qilish"
            @click="cancel(b)"
          />
        </div>
      </section>

      <div class="rd__actions">
        <AppButton
          v-if="canEdit && d.room.status !== 'REPAIR'"
          icon="event_available"
          label="Band qilish"
          data-room-book
          @click="emit('book', d.room)"
        />
        <AppButton
          variant="secondary"
          icon="calendar_view_week"
          label="Jadvalda ochish"
          :to="{ path: '/timetable', query: { view: 'room', room: d.room.id } }"
        />
      </div>
    </div>
  </SideDrawer>
</template>

<script setup>
// One room: seats, status, equipment, its week as a heat map (days × lessons, each lesson
// in its subject's colour with the class), how busy it is, which classes and teachers use
// it, and the bookings of the next 30 days (cancel / book).
import { ref, watch } from 'vue'
import { useQuasar } from 'quasar'
import SideDrawer from '@/components/dashboard/SideDrawer.vue'
import AppButton from '@/components/app/AppButton.vue'
import EmptyState from '@/components/pages/EmptyState.vue'
import { api } from '@/boot/axios'
import { useSchoolStore } from '@/stores/school'
import { subjectColor } from '@/utils/subjectColor'
import {
  WEEKDAYS,
  WEEKDAY_SHORT,
  roomLabel
} from '@/components/timetable/timetable'
import { EQUIPMENT, ROOM_TYPES, hm } from './rooms'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  // RoomInfo from the live map
  room: { type: Object, default: null },
  canEdit: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue', 'book', 'changed'])
const $q = useQuasar()
const school = useSchoolStore()

const d = ref(null)
async function load() {
  d.value = null
  if (!props.modelValue || !props.room) return
  const res = await api.get(`/api/pages/rooms/${props.room.id}/detail`, {
    params: { schoolId: school.activeSchoolId }
  })
  d.value = res.data
}
watch(() => [props.modelValue, props.room?.id], load, { immediate: true })
defineExpose({ load })

async function cancel(b) {
  try {
    await api.delete(`/api/pages/rooms/bookings/${b.id}`, {
      params: { schoolId: school.activeSchoolId }
    })
    $q.notify({ type: 'info', message: 'Band qilish bekor qilindi' })
    await load()
    emit('changed')
  } catch (e) {
    $q.notify({ type: 'negative', message: e.friendlyMessage || 'Xatolik' })
  }
}
</script>

<style scoped>
.rd {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  padding-top: var(--space-3);
}

.rd h4 {
  margin: 0 0 var(--space-2);
  font-size: var(--text-sm);
  font-weight: 700;
}

.rd h4 b {
  margin-left: var(--space-1);
  color: var(--room-busy);
}

.rd__facts,
.rd__chips {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-1);
}

.rd__fact,
.rd__chip {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  padding: var(--space-1) var(--space-2);
  border-radius: var(--radius-pill);
  background: var(--surface-2);
  color: var(--text-primary);
  font-size: var(--text-xs);
  font-weight: 600;
  text-decoration: none;
}

.rd__chip b {
  color: var(--brand-text-muted);
}

.rd__fact--repair {
  color: var(--st-late);
}

.rd__heat {
  display: grid;
  gap: var(--hairline);
}

.rd__ph,
.rd__dh {
  font-size: var(--text-xs);
  font-weight: 700;
  color: var(--brand-text-muted);
  text-align: center;
}

.rd__cell {
  display: flex;
  align-items: center;
  justify-content: center;
  height: var(--heat-cell);
  border-radius: var(--radius-sm);
  background: var(--tile-none);
  font-size: calc(var(--text-xs) * 0.85);
  font-weight: 700;
  overflow: hidden;
}

.rd__cell--on {
  background: color-mix(in srgb, var(--c) 55%, var(--card-bg));
  color: var(--text-primary);
}

.rd__booking {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-2) 0;
  border-bottom: 1px solid var(--dash-edge);
}

.rd__bdate,
.rd__bwhat {
  display: flex;
  flex-direction: column;
}

.rd__bwhat {
  flex: 1 1 auto;
  min-width: 0;
}

.rd__bdate small,
.rd__bwhat small {
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
}

.rd__actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.rd__skel {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  padding-top: var(--space-3);
}

.rd__skel-line {
  height: var(--control-h);
}
</style>

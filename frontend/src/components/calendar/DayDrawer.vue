<template>
  <SideDrawer
    :model-value="modelValue"
    :title="title"
    :subtitle="subtitle"
    icon="today"
    color="var(--c-primary)"
    @update:model-value="v => emit('update:modelValue', v)"
  >
    <div v-if="!day" class="dd__skel">
      <div v-for="i in 5" :key="i" class="skel dd__skel-line" />
    </div>
    <div v-else class="dd" data-day-drawer>
      <div class="dd__stats">
        <div>
          <small>Darslar</small>
          <b>{{ day.schoolDay ? day.lessons : '—' }}</b>
        </div>
        <div>
          <small>Davomat</small>
          <b :class="heatClass">{{ day.attendance === null ? '—' : `${day.attendance.toFixed(1)}%` }}</b>
          <small v-if="day.absent !== null">{{ day.absent }} kelmagan</small>
        </div>
        <div>
          <small>Baholar</small>
          <b>{{ day.grades }}</b>
        </div>
      </div>
      <p v-if="!day.schoolDay" class="dd__off">
        {{ day.weekday ? 'Dam olish kuni (bayram yoki ta\'til)' : 'Yakshanba' }}
      </p>

      <section>
        <h4>Tadbirlar</h4>
        <EmptyState
          v-if="!day.events.length"
          icon="event_available"
          title="Bu kunda tadbir yo'q"
          :action="editable ? 'Tadbir qo\'shish' : ''"
          @action="emit('create', date)"
        />
        <button
          v-for="e in day.events"
          :key="e.id"
          type="button"
          class="dd__ev"
          :style="{ '--c': EVENT_TYPES[e.type]?.color }"
          @click="emit('open', e)"
        >
          <q-icon :name="EVENT_TYPES[e.type]?.icon" />
          <span>
            <b>{{ e.title }}</b>
            <small
              >{{ EVENT_TYPES[e.type]?.label
              }}<template v-if="e.startTime">
                · {{ e.startTime.slice(0, 5) }}–{{ (e.endTime || '').slice(0, 5) }}</template
              ><template v-if="e.roomNumber || e.location">
                · 📍 {{ e.roomNumber ? roomLabel(e.roomNumber) : e.location }}</template
              ></small
            >
          </span>
        </button>
      </section>

      <section v-if="day.birthdays.length">
        <h4>🎂 Tug'ilgan kunlar</h4>
        <router-link
          v-for="b in day.birthdays"
          :key="b.studentId"
          :to="`/profiles/student/${b.studentId}`"
          class="dd__bday"
        >
          <AvatarInitials size="sm" :name="b.name" :id="b.studentId" />
          <span
            ><b>{{ b.name }}</b> <small>{{ b.className }} · {{ b.age }} yosh</small></span
          >
        </router-link>
      </section>

      <div class="dd__actions">
        <AppButton
          v-if="editable"
          icon="add"
          label="Tadbir qo'shish"
          size="sm"
          @click="emit('create', date)"
        />
        <AppButton
          v-if="day.schoolDay"
          variant="secondary"
          icon="calendar_view_week"
          label="Shu kun jadvali"
          size="sm"
          :to="{ path: '/timetable', query: { view: 'today' } }"
        />
        <AppButton
          v-if="day.attendance !== null"
          variant="secondary"
          icon="fact_check"
          label="Davomat kalendari"
          size="sm"
          :to="{ path: '/attendance/calendar', query: { month: date.slice(0, 7) } }"
        />
      </div>
    </div>
  </SideDrawer>
</template>

<script setup>
// One day: how many lessons, attendance and grades if it is past, its events, and whose
// birthday it is.
import { computed, ref, watch } from 'vue'
import SideDrawer from '@/components/dashboard/SideDrawer.vue'
import AppButton from '@/components/app/AppButton.vue'
import AvatarInitials from '@/components/pages/AvatarInitials.vue'
import EmptyState from '@/components/pages/EmptyState.vue'
import { api } from '@/boot/axios'
import { useSchoolStore } from '@/stores/school'
import { EVENT_TYPES, MONTHS, heatStep, parse } from './calendar'
import { roomLabel } from '@/components/timetable/timetable'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  date: { type: String, default: null },
  editable: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue', 'open', 'create'])
const school = useSchoolStore()

const day = ref(null)
watch(
  () => [props.modelValue, props.date],
  async ([open]) => {
    day.value = null
    if (!open || !props.date) return
    const res = await api.get('/api/pages/calendar/day', {
      params: { schoolId: school.activeSchoolId, date: props.date }
    })
    day.value = res.data
  },
  { immediate: true }
)

const WEEKDAYS = ['Yakshanba', 'Dushanba', 'Seshanba', 'Chorshanba', 'Payshanba', 'Juma', 'Shanba']
const title = computed(() => {
  if (!props.date) return ''
  const d = parse(props.date)
  return `${d.getDate()}-${MONTHS[d.getMonth()].toLowerCase()}`
})
const subtitle = computed(() => (props.date ? WEEKDAYS[parse(props.date).getDay()] : ''))
const heatClass = computed(() => {
  const s = heatStep(day.value?.attendance)
  return s ? `dd__heat-${s}` : ''
})
</script>

<style scoped>
.dd {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  padding-top: var(--space-3);
}

.dd__stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--space-2);
}

.dd__stats > div {
  display: flex;
  flex-direction: column;
  padding: var(--space-3);
  border-radius: var(--radius-md);
  background: var(--surface-2);
}

.dd__stats small {
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
}

.dd__stats b {
  font-size: var(--text-xl);
  font-variant-numeric: tabular-nums;
}

.dd__heat-1 {
  color: var(--heat-1);
}

.dd__heat-3 {
  color: var(--st-late);
}

.dd__heat-4 {
  color: var(--st-absent);
}

.dd__off {
  margin: 0;
  font-size: var(--text-sm);
  color: var(--subj-4);
  font-weight: 600;
}

.dd h4 {
  margin: 0 0 var(--space-2);
  font-size: var(--text-sm);
  font-weight: 700;
}

.dd__ev,
.dd__bday {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  width: 100%;
  padding: var(--space-2);
  border: 0;
  border-radius: var(--radius-md);
  background: transparent;
  color: inherit;
  font: inherit;
  text-align: left;
  text-decoration: none;
  cursor: pointer;
}

.dd__ev:hover,
.dd__bday:hover {
  background: var(--surface-2);
}

.dd__ev .q-icon {
  color: var(--c);
  font-size: var(--text-lg);
}

.dd__ev span,
.dd__bday span {
  display: flex;
  flex-direction: column;
}

.dd__ev small,
.dd__bday small {
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
}

.dd__actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.dd__skel {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  padding-top: var(--space-3);
}

.dd__skel-line {
  height: var(--control-h);
}
</style>

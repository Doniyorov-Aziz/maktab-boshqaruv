<template>
  <SideDrawer
    :model-value="modelValue"
    title="Bo'sh xona topish"
    :subtitle="result ? `${result.rooms.length} ta mos xona · ${hm(result.start)}–${hm(result.end)}` : 'Kun, dars va talablar'"
    icon="travel_explore"
    color="var(--room-free)"
    @update:model-value="v => emit('update:modelValue', v)"
  >
    <template #toolbar>
      <div class="rf__form" data-finder-form>
        <AppDateInput v-model="date" label="Kun" :min="today" no-sundays />
        <div class="rf__periods" role="group" aria-label="Darslar">
          <button
            v-for="p in periods"
            :key="p.n"
            type="button"
            class="rf__period"
            :class="{ 'rf__period--on': chosen.includes(p.n) }"
            :aria-pressed="chosen.includes(p.n)"
            :title="`${hm(p.start)}–${hm(p.end)}`"
            :data-period="p.n"
            @click="togglePeriod(p.n)"
          >
            {{ p.n }}
          </button>
        </div>
        <div class="rf__row">
          <AppInput v-model.number="minCapacity" type="number" dense label="Kamida o'rin" class="rf__cap" />
          <AppSelect
            v-model="type"
            dense
            clearable
            label="Turi"
            :options="typeOptions"
            emit-value
            map-options
            class="rf__type"
          />
        </div>
        <div class="rf__equip" role="group" aria-label="Jihozlar">
          <button
            v-for="(e, key) in EQUIPMENT"
            :key="key"
            type="button"
            class="rf__tag"
            :class="{ 'rf__tag--on': equipment.includes(key) }"
            :aria-pressed="equipment.includes(key)"
            @click="toggleEquip(key)"
          >
            <q-icon :name="e.icon" /> {{ e.label }}
          </button>
        </div>
      </div>
    </template>

    <div v-if="loading" class="rf__skel">
      <div v-for="i in 5" :key="i" class="skel rf__skel-line" />
    </div>
    <EmptyState
      v-else-if="result && !result.rooms.length"
      icon="search_off"
      title="Bu talablarga mos bo'sh xona yo'q"
      text="Boshqa dars yoki kamroq talab tanlab ko'ring"
    />
    <ul v-else-if="result" class="rf__list" data-finder-results>
      <li v-for="m in result.rooms" :key="m.room.id" :data-finder-room="m.room.id">
        <span class="rf__score" :style="{ '--p': `${m.score}%` }" :title="`Moslik ${m.score}%`">{{ m.score }}</span>
        <span class="rf__info">
          <b>{{ roomLabel(m.room.number) }}</b>
          <small>
            <q-icon :name="ROOM_TYPES[m.room.type]?.icon" /> {{ ROOM_TYPES[m.room.type]?.label }} ·
            {{ m.room.capacity }} o'rin · {{ m.room.building }}
          </small>
          <span class="rf__tags">
            <span v-for="t in m.matched" :key="t" class="rf__ok">✓ {{ EQUIPMENT[t]?.label }}</span>
            <span v-for="t in m.missing" :key="t" class="rf__miss">✗ {{ EQUIPMENT[t]?.label }}</span>
          </span>
        </span>
        <AppButton
          v-if="canEdit"
          size="sm"
          label="Band qilish"
          data-finder-book
          @click="emit('book', { room: m.room, preset: { date: result.date, start: result.start, end: result.end } })"
        />
      </li>
    </ul>
  </SideDrawer>
</template>

<script setup>
// "Bo'sh xona topish": a day, one or more lessons, at least so many seats, a type and the
// equipment wanted → the rooms free in all those lessons (timetable and bookings counted),
// the best fit first, each with what it has and lacks, and "Band qilish".
import { ref, watch } from 'vue'
import SideDrawer from '@/components/dashboard/SideDrawer.vue'
import AppButton from '@/components/app/AppButton.vue'
import AppInput from '@/components/app/AppInput.vue'
import AppSelect from '@/components/app/AppSelect.vue'
import AppDateInput from '@/components/app/AppDateInput.vue'
import EmptyState from '@/components/pages/EmptyState.vue'
import { api } from '@/boot/axios'
import { useSchoolStore } from '@/stores/school'
import { roomLabel } from '@/components/timetable/timetable'
import { tashkentNow } from '@/utils/tashkent'
import { EQUIPMENT, ROOM_TYPES, hm } from './rooms'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  periods: { type: Array, default: () => [] },
  canEdit: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue', 'book'])
const school = useSchoolStore()
const today = tashkentNow().date

const date = ref(today)
const chosen = ref([])
const minCapacity = ref(null)
const type = ref(null)
const equipment = ref([])
const result = ref(null)
const loading = ref(false)

const typeOptions = Object.entries(ROOM_TYPES).map(([value, t]) => ({ value, label: t.label }))

function togglePeriod(n) {
  chosen.value = chosen.value.includes(n) ? chosen.value.filter(x => x !== n) : [...chosen.value, n].sort((a, b) => a - b)
}
function toggleEquip(k) {
  equipment.value = equipment.value.includes(k) ? equipment.value.filter(x => x !== k) : [...equipment.value, k]
}

let timer = null
async function search() {
  if (!props.modelValue || !date.value) return
  if (!chosen.value.length) {
    result.value = null
    return
  }
  loading.value = true
  try {
    const res = await api.get('/api/pages/rooms/find', {
      params: {
        schoolId: school.activeSchoolId,
        date: date.value,
        periods: chosen.value.join(','),
        minCapacity: minCapacity.value || undefined,
        type: type.value || undefined,
        equipment: equipment.value.join(',') || undefined
      }
    })
    result.value = res.data
  } catch {
    result.value = { rooms: [], start: '', end: '' }
  } finally {
    loading.value = false
  }
}
watch([date, chosen, minCapacity, type, equipment, () => props.modelValue], () => {
  clearTimeout(timer)
  timer = setTimeout(search, 250)
})
watch(
  () => props.modelValue,
  open => {
    if (open && !chosen.value.length && props.periods.length) chosen.value = [props.periods[0].n]
  }
)
defineExpose({ search })
</script>

<style scoped>
.rf__form {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.rf__periods,
.rf__equip {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-1);
}

.rf__period {
  min-width: var(--control-h-sm);
  height: var(--control-h-sm);
  border: 1px solid var(--c-border-control);
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--text-primary);
  font: inherit;
  font-weight: 700;
  cursor: pointer;
}

.rf__period--on {
  background: var(--c-primary);
  border-color: var(--c-primary);
  color: white;
}

.rf__row {
  display: flex;
  gap: var(--space-2);
}

.rf__cap {
  flex: 0 0 var(--field-w-sm);
}

.rf__type {
  flex: 1 1 auto;
}

.rf__tag {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  padding: var(--space-1) var(--space-2);
  border: 1px solid var(--c-border-control);
  border-radius: var(--radius-pill);
  background: transparent;
  color: var(--text-primary);
  font: inherit;
  font-size: var(--text-xs);
  cursor: pointer;
}

.rf__tag--on {
  border-color: var(--room-free);
  background: color-mix(in srgb, var(--room-free) 14%, transparent);
}

.rf__list {
  list-style: none;
  margin: 0;
  padding: var(--space-2) 0 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.rf__list li {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-2);
  border-radius: var(--radius-md);
}

.rf__list li:hover {
  background: var(--surface-2);
}

.rf__score {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: var(--tile-md);
  height: var(--tile-md);
  border-radius: var(--radius-pill);
  background: conic-gradient(var(--room-free) var(--p), var(--tile-none) 0);
  font-size: var(--text-xs);
  font-weight: 800;
  position: relative;
}

.rf__score::before {
  content: '';
  position: absolute;
  inset: var(--accent-bar);
  border-radius: var(--radius-pill);
  background: var(--card-bg);
}

.rf__score {
  isolation: isolate;
}

.rf__score::before {
  z-index: -1;
}

.rf__info {
  flex: 1 1 auto;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.rf__info small {
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
}

.rf__tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-1);
  font-size: var(--text-xs);
}

.rf__ok {
  color: var(--st-present);
}

.rf__miss {
  color: var(--st-absent);
}

.rf__skel {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  padding-top: var(--space-2);
}

.rf__skel-line {
  height: var(--control-h);
}
</style>

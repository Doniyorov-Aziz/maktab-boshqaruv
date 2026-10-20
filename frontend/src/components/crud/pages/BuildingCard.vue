<template>
  <HeroCard
    :color="color"
    icon="apartment"
    :title="row.name"
    :subtitle="subtitle"
    :primary="{ label: 'Xonalar', icon: 'meeting_room' }"
    :menu="menu"
    :data-building-card="row.id"
    @open="toRooms"
    @primary="toRooms"
    @menu="k => emit(k)"
  >
    <template #meta>
      <!-- the building: floors as bands (top floor up), rooms as cells -->
      <figure v-if="floors.length" class="bc__draw" data-building-svg>
        <svg
          :viewBox="`0 0 ${W} ${height}`"
          preserveAspectRatio="xMidYMid meet"
          role="img"
          :aria-label="`${row.name}: ${floors.length} qavat, ${roomCount} xona`"
        >
          <path
            :d="`M ${X0 - 6} ${ROOF} L ${W / 2} 2 L ${W - 2} ${ROOF} Z`"
            class="bc__roof"
          />
          <g
            v-for="(f, fi) in drawn"
            :key="f.floor"
            :transform="`translate(0 ${ROOF + fi * (FH + GAP)})`"
          >
            <rect
              :x="X0 - 6"
              y="-2"
              :width="W - X0 + 4"
              :height="FH + 4"
              rx="4"
              class="bc__floor"
            />
            <text x="2" :y="FH / 2 + 4" class="bc__floor-no">
              {{ f.floor }}
            </text>
            <g
              v-for="(r, ri) in f.rooms"
              :key="r.id"
              :transform="`translate(${X0 + ri * f.cell} 1)`"
            >
              <rect
                :width="Math.max(f.cell - 3, 2)"
                :height="FH - 2"
                rx="3"
                :class="`bc__room bc__room--${r.status.toLowerCase()}`"
                :data-room-cell="r.status"
              >
                <title>{{ roomTitle(r) }}</title>
              </rect>
              <!-- the room's weekly share of lesson slots, as a bar along its foot -->
              <rect
                v-if="r.weekly"
                x="2"
                :y="FH - 6"
                :width="Math.max(((f.cell - 7) * r.weekly) / 100, 1)"
                height="2"
                rx="1"
                class="bc__week"
              />
              <text
                v-if="f.cell >= 20 && /^\d+$/.test(r.number)"
                :x="(f.cell - 3) / 2"
                :y="FH / 2 - 2"
                class="bc__num"
              >
                {{ r.number }}
              </text>
            </g>
          </g>
        </svg>
        <figcaption class="bc__legend">
          <span><i class="bc__dot bc__dot--busy" />Band (hozir)</span>
          <span><i class="bc__dot bc__dot--free" />Bo'sh</span>
          <span v-if="st.repairRooms"
            ><i class="bc__dot bc__dot--repair" />Ta'mirda</span
          >
          <span><i class="bc__dot bc__dot--week" />Haftalik bandlik</span>
          <span v-if="!st.busyNow" class="bc__quiet">Hozir dars yo'q</span>
        </figcaption>
      </figure>
      <div v-else class="bc__empty">Binoda hali xona yo'q</div>
    </template>

    <template #kpis>
      <HeroKpi
        kpi-key="floors"
        icon="layers"
        :value="formatNumber(st.floorCount || row.floorCount || 0)"
        label="Qavatlar"
        :color="color"
      />
      <HeroKpi
        kpi-key="rooms"
        icon="meeting_room"
        :value="formatNumber(roomCount)"
        label="Xonalar"
        color="var(--chart-primary)"
      />
      <HeroKpi
        kpi-key="capacity"
        icon="event_seat"
        :value="formatNumber(st.capacity ?? 0)"
        label="Umumiy sig'im"
        color="var(--gr-4)"
      />
      <HeroKpi
        kpi-key="weekly"
        icon="date_range"
        :value="formatPercent(weekly)"
        label="Haftalik bandlik"
        :ring="weekly"
        color="var(--c-primary)"
        :hint="
          st.totalSlots
            ? `${st.busySlots} / ${st.totalSlots} dars sloti band (haftada)`
            : 'Dars jadvali yo\'q'
        "
      />
      <HeroKpi
        kpi-key="now"
        icon="schedule"
        :value="`${st.busyNow ?? 0} / ${st.activeRooms ?? roomCount}`"
        label="Hozir band"
        color="var(--st-present)"
      />
    </template>

    <template #bottom>
      <div class="bc__types" data-room-types>
        <button
          v-for="t in types"
          :key="t.type"
          type="button"
          class="bc__type"
          :style="{ '--t': t.color }"
          :data-room-type="t.type"
          @click.stop="toRooms(t.type)"
        >
          {{ t.label }} <b class="tnum">{{ t.count }}</b>
        </button>
      </div>
    </template>
  </HeroCard>
</template>

<script setup>
// Binolar, up to six buildings: the building drawn (floors as bands, the top one up; each room a
// cell — busy now in the brand colour, free green, under repair grey, a tooltip with its weekly
// share), floors, rooms, seats, weekly occupancy (busy lesson slots of the week / all slots of its
// working rooms) and rooms busy now, the room types as chips. A click opens Xonalar for this
// building (a chip — for this building and type).
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import HeroCard from '../HeroCard.vue'
import HeroKpi from '../HeroKpi.vue'
import { roomTypeLabels, roomTypeColors } from '@/config/roomTypes'
import { formatNumber, formatPercent } from '@/utils/format'
import { SUBJECT_PALETTE } from '@/utils/subjectColor'

const props = defineProps({
  row: { type: Object, required: true },
  canEdit: { type: Boolean, default: false },
  canDelete: { type: Boolean, default: false }
})
const emit = defineEmits(['open', 'edit', 'delete'])
const router = useRouter()

const st = computed(() => props.row.stats || {})
const floors = computed(() => st.value.floors || [])
const roomCount = computed(() => Number(props.row.roomCount ?? 0))
const weekly = computed(
  () => st.value.weeklyOccupancy ?? props.row.occupiedPercentage ?? null
)
const color = computed(
  () => SUBJECT_PALETTE[((((Number(props.row.id) || 0) * 7) % 12) + 12) % 12]
)
const subtitle = computed(() => {
  const parts = []
  if (props.row.schoolName) parts.push(props.row.schoolName)
  if (st.value.repairRooms)
    parts.push(`${st.value.repairRooms} ta xona ta'mirda`)
  return parts.join(' · ')
})

const types = computed(() =>
  Object.entries(st.value.roomTypes || {})
    .map(([type, count]) => ({
      type,
      count,
      label: roomTypeLabels[type] || type,
      color: roomTypeColors[type] || 'var(--c-primary)'
    }))
    .sort((a, b) => b.count - a.count)
)

// ---- the drawing: a fixed 320-wide box, 26 units a floor; cells share the width of their floor
const W = 320
const X0 = 18
const FH = 26
const GAP = 4
const ROOF = 16
const drawn = computed(() =>
  [...floors.value]
    .sort((a, b) => b.floor - a.floor)
    .map(f => ({ ...f, cell: (W - X0 - 4) / Math.max(f.rooms.length, 1) }))
)
const height = computed(() => ROOF + drawn.value.length * (FH + GAP) + 2)

const STATUS = { BUSY: 'band (hozir dars)', FREE: "bo'sh", REPAIR: "ta'mirda" }
function roomTitle(r) {
  const parts = [
    `${r.number}`,
    roomTypeLabels[r.type] || r.type,
    STATUS[r.status] || r.status,
    `${r.capacity} o'rin`
  ]
  if (r.weekly != null) parts.push(`haftalik ${formatPercent(r.weekly)}`)
  return parts.join(' · ')
}

const menu = computed(() => [
  ...(props.canEdit
    ? [{ key: 'edit', label: 'Tahrirlash', icon: 'edit' }]
    : []),
  ...(props.canDelete
    ? [
        {
          key: 'delete',
          label: "O'chirish",
          icon: 'delete_outline',
          danger: true
        }
      ]
    : [])
])

function toRooms(type) {
  const query = { buildingId: String(props.row.id) }
  if (typeof type === 'string') query.type = type
  router.push({ path: '/app/rooms', query })
}
</script>

<style scoped>
.bc__draw {
  margin: var(--space-2) 0 0;
}

.bc__draw svg {
  display: block;
  width: 100%;
  max-height: 200px;
}

.bc__roof {
  fill: color-mix(in srgb, var(--text-primary) 8%, transparent);
}

.bc__floor {
  fill: var(--surface-2);
  stroke: var(--dash-edge);
}

.bc__floor-no {
  fill: var(--brand-text-muted);
  font-size: 11px;
  font-weight: 700;
}

.bc__week {
  fill: color-mix(in srgb, var(--text-primary) 55%, transparent);
}

.bc__num {
  fill: white;
  font-size: 7px;
  font-weight: 700;
  text-anchor: middle;
  dominant-baseline: middle;
  pointer-events: none;
}

.bc__room--busy {
  fill: var(--c-primary);
}

.bc__room--free {
  fill: color-mix(in srgb, var(--st-present) 70%, var(--card-bg));
}

.bc__room--repair {
  fill: color-mix(in srgb, var(--st-unmarked) 55%, var(--card-bg));
}

.bc__legend {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
  margin-top: var(--space-1);
  color: var(--brand-text-muted);
  font-size: var(--text-xs);
}

.bc__legend span {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
}

.bc__dot {
  display: inline-block;
  width: var(--space-2);
  height: var(--space-2);
  border-radius: 2px;
}

.bc__dot--busy {
  background: var(--c-primary);
}

.bc__dot--free {
  background: color-mix(in srgb, var(--st-present) 70%, var(--card-bg));
}

.bc__dot--week {
  height: 2px;
  background: color-mix(in srgb, var(--text-primary) 55%, transparent);
}

.bc__quiet {
  margin-left: auto;
  font-style: italic;
}

.bc__dot--repair {
  background: color-mix(in srgb, var(--st-unmarked) 55%, var(--card-bg));
}

.bc__empty {
  color: var(--brand-text-muted);
  font-size: var(--text-sm);
}

.bc__types {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-1);
}

.bc__type {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  padding: var(--hairline) var(--space-2);
  border: var(--hairline) solid color-mix(in srgb, var(--t) 30%, transparent);
  border-radius: var(--radius-pill);
  background: color-mix(in srgb, var(--t) 10%, transparent);
  color: var(--text-primary);
  font-size: var(--text-xs);
  cursor: pointer;
}

.bc__type b {
  color: var(--t);
}

.bc__type:hover {
  background: color-mix(in srgb, var(--t) 18%, transparent);
}
</style>

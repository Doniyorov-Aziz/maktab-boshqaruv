<template>
  <HeroCard
    :color="color"
    :initials="initials"
    :title="row.name"
    :subtitle="row.address"
    :badge="
      isCurrent
        ? { label: 'Joriy', color: 'var(--st-present)', glow: true }
        : null
    "
    :primary="
      isCurrent
        ? {
            label: 'Bosh sahifaga',
            icon: 'space_dashboard',
            variant: 'primary'
          }
        : { label: 'Shu maktabga o\'tish', icon: 'login' }
    "
    :menu="menu"
    :data-school-card="row.id"
    @open="emit('open')"
    @primary="onPrimary"
    @menu="k => emit(k)"
  >
    <template #meta>
      <div v-if="card?.director" class="sc__director" data-director>
        <AvatarInitials
          :name="card.director.name"
          :id="card.director.id"
          size="sm"
          gradient
        />
        <div class="sc__who">
          <b>{{ card.director.name }}</b>
          <small>Direktor</small>
        </div>
        <a
          v-if="card.director.phone"
          class="sc__phone tnum"
          :href="`tel:${card.director.phone}`"
          @click.stop
          >{{ maskPhone(card.director.phone) }}</a
        >
      </div>
      <div v-else class="sc__director sc__director--none">
        <q-icon name="person_off" /> Direktor biriktirilmagan
      </div>
    </template>

    <template #kpis>
      <HeroKpi
        kpi-key="students"
        icon="groups"
        :value="formatNumber(row.studentCount ?? 0)"
        label="O'quvchilar"
        color="var(--chart-primary)"
      />
      <HeroKpi
        kpi-key="teachers"
        icon="badge"
        :value="formatNumber(card?.teachers ?? row.teacherCount ?? 0)"
        label="O'qituvchilar"
        color="var(--ring-journal)"
      />
      <HeroKpi
        kpi-key="classes"
        icon="class"
        :value="formatNumber(row.classCount ?? 0)"
        label="Sinflar"
        color="var(--gr-4)"
      />
      <HeroKpi
        kpi-key="today"
        icon="how_to_reg"
        :value="formatPercent(card?.today)"
        :label="
          card?.todayNote && card.todayNote !== 'bugun'
            ? `Davomat (${card.todayNote})`
            : 'Bugungi davomat'
        "
        :ring="card?.today ?? null"
        color="var(--st-present)"
      />
      <HeroKpi
        kpi-key="quality"
        icon="workspace_premium"
        :value="formatPercent(card?.quality)"
        label="Sifat (30 kun)"
        color="var(--gr-5)"
        hint="4 va 5 baholar ulushi, oxirgi 30 kun"
      />
      <HeroKpi
        kpi-key="telegram"
        icon="send"
        :value="formatPercent(card?.telegram)"
        label="Ota-onalar Telegramda"
        color="var(--tg-blue, #229ed9)"
      />
    </template>

    <template #bottom>
      <div class="sc__run" data-school-run>
        <span class="sc__run-label">Davomat · 30 ish kuni</span>
        <Sparkline
          v-if="hasRun"
          :values="card.run"
          :min="sparkRange(card.run).min"
          :max="sparkRange(card.run).max"
          :color="color"
          label="30 kunlik davomat"
          class="sc__spark"
        />
        <span v-else class="sc__run-none">Hali ma'lumot yo'q</span>
      </div>
    </template>
  </HeroCard>
</template>

<script setup>
// Maktablar, up to six schools: a banner in the school's colour (its own or one of the palette
// by id), its initials, name, address and director; students, teachers, classes, today's
// attendance (a small ring), quality and parents in Telegram; 30 school days of attendance. The
// current school says "Joriy" and leads to the home page; another one is switched to.
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import HeroCard from '../HeroCard.vue'
import HeroKpi from '../HeroKpi.vue'
import Sparkline from '@/components/dashboard/Sparkline.vue'
import AvatarInitials from '@/components/pages/AvatarInitials.vue'
import { useSchoolStore } from '@/stores/school'
import { formatNumber, formatPercent, sparkRange } from '@/utils/format'
import { maskPhone } from '../crud'
import { SUBJECT_PALETTE } from '@/utils/subjectColor'

const props = defineProps({
  row: { type: Object, required: true },
  canEdit: { type: Boolean, default: false },
  canDelete: { type: Boolean, default: false }
})
const emit = defineEmits(['open', 'edit', 'delete'])
const router = useRouter()
const schoolStore = useSchoolStore()

const card = computed(() => props.row.card || null)
const color = computed(() => schoolColor(props.row))
const isCurrent = computed(
  () => String(props.row.id) === String(schoolStore.activeSchoolId)
)
const hasRun = computed(
  () => (card.value?.run || []).filter(v => v != null).length > 1
)
// "110-maktab" → 110, "1-maktab" → 1M, otherwise the first letters of two words
const initials = computed(() => {
  const name = String(props.row.name || '')
  const num = name.match(/\d+/)
  if (num) return num[0].length >= 3 ? num[0] : `${num[0]}M`
  return name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map(w => w[0].toUpperCase())
    .join('')
})

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

function onPrimary() {
  if (isCurrent.value) router.push('/')
  else schoolStore.setActiveSchool(props.row.id, props.row.name)
}
</script>

<script>
/** The school's colour: its own, else one of the palette by its id. */
export function schoolColor(row) {
  if (row?.color) return row.color
  const n = (((Number(row?.id) || 0) % 12) + 12) % 12
  return SUBJECT_PALETTE[(n * 5) % 12]
}
</script>

<style scoped>
.sc__director {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  min-width: 0;
  margin-top: var(--space-1);
}

.sc__director--none {
  color: var(--brand-text-muted);
  font-size: var(--text-sm);
}

.sc__who {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-width: 0;
  line-height: 1.2;
}

.sc__who b {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: var(--text-sm);
}

.sc__who small {
  color: var(--brand-text-muted);
  font-size: var(--text-xs);
}

.sc__phone {
  flex: none;
  color: var(--c-primary);
  font-size: var(--text-sm);
  font-weight: 600;
  text-decoration: none;
}

.sc__run {
  display: grid;
  grid-template-columns: auto 1fr;
  align-items: center;
  gap: var(--space-3);
}

.sc__run-label {
  color: var(--brand-text-muted);
  font-size: var(--text-xs);
  white-space: nowrap;
}

.sc__spark {
  width: 100%;
  height: var(--space-7);
}

.sc__run-none {
  color: var(--brand-text-muted);
  font-size: var(--text-xs);
}
</style>

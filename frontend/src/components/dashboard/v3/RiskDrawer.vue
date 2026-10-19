<template>
  <SideDrawer
    :model-value="modelValue"
    :title="title"
    :subtitle="subtitle"
    icon="warning"
    color="var(--st-absent)"
    @update:model-value="v => emit('update:modelValue', v)"
  >
    <template #toolbar>
      <div class="rd-tools">
        <div class="rd-tools__row">
          <AppInput
            v-model="query"
            class="rd-tools__search"
            dense
            clearable
            placeholder="Ism bo'yicha qidirish"
            aria-label="Ism bo'yicha qidirish"
            data-risk-search
          >
            <template #prepend><q-icon name="search" /></template>
          </AppInput>
          <AppSelect
            v-model="classId"
            class="rd-tools__class"
            dense
            clearable
            :options="classOptions"
            emit-value
            map-options
            label="Sinf"
            data-risk-class
          />
        </div>
        <div class="rd-tools__row rd-tools__row--wrap">
          <SegTabs v-model="sort" :options="SORTS" label="Saralash" />
          <div class="rd-levels" role="group" aria-label="Xavf darajasi">
            <button
              v-for="l in LEVELS"
              :key="l.value"
              type="button"
              :class="[
                'rd-level',
                `rd-level--${l.value}`,
                { 'rd-level--on': level === l.value }
              ]"
              :aria-pressed="level === l.value"
              :data-risk-level="l.value"
              @click="level = level === l.value ? null : l.value"
            >
              {{ l.label }} <b>{{ levelCounts[l.value] }}</b>
            </button>
          </div>
        </div>
        <div class="rd-count" data-risk-count aria-live="polite">
          {{ formatNumber(total) }} tadan {{ formatNumber(filtered.length) }}
          tasi
        </div>
      </div>
    </template>

    <template #default="{ scrollEl }">
      <div v-if="!rows" class="rd-skel">
        <div v-for="i in 10" :key="i" class="skel rd-skel__line" />
      </div>
      <div v-else-if="!filtered.length" class="rd-empty" data-risk-empty>
        <q-icon name="search_off" class="rd-empty__icon" />
        <div>Bu filtr bo'yicha o'quvchi topilmadi</div>
        <AppButton
          variant="secondary"
          size="sm"
          icon="filter_alt_off"
          label="Filtrni tozalash"
          @click="clearFilters"
        />
      </div>
      <q-virtual-scroll
        v-else-if="filtered.length > 50 && scrollEl"
        :items="filtered"
        :scroll-target="scrollEl"
        :virtual-scroll-item-size="ROW_H"
        :virtual-scroll-slice-size="30"
      >
        <template #default="{ item, index }">
          <RiskRow
            :key="item.studentId"
            :row="item"
            :index="index"
            :readonly="readonly"
            @open="openStudent"
          />
        </template>
      </q-virtual-scroll>
      <template v-else>
        <RiskRow
          v-for="(r, i) in filtered"
          :key="r.studentId"
          :row="r"
          :index="i"
          :readonly="readonly"
          @open="openStudent"
        />
      </template>
    </template>

    <template #footer>
      <span class="rd-foot-note"
        >Xavf balli: kelmaslik, baho, pasayish, «2»lar</span
      >
      <AppButton
        v-if="source === 'risk' && !readonly"
        variant="secondary"
        size="sm"
        icon="download"
        label="Excel ga yuklab olish"
        :loading="exporting"
        data-risk-export
        @click="exportXlsx"
      />
    </template>
  </SideDrawer>
</template>

<script setup>
// The whole "Xavf ostidagi o'quvchilar" list in the side panel: search (by name, 250 ms
// after typing stops), one class, the order (risk score, days absent, how far the average
// fell) and the level (high ≥ 70, middle 50–69, low under 50) with how many of each. Over 50
// rows the list is virtual, so 500+ stay smooth. A row opens the student; on hover it
// offers a call to the guardian, the class's Telegram parents and the profile. "Excel"
// downloads exactly what the filter shows.
import { computed, ref, watch, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import SideDrawer from '../SideDrawer.vue'
import SegTabs from '../SegTabs.vue'
import RiskRow from './RiskRow.vue'
import AppInput from '@/components/app/AppInput.vue'
import AppSelect from '@/components/app/AppSelect.vue'
import AppButton from '@/components/app/AppButton.vue'
import { api } from '@/boot/axios'
import { useSchoolStore } from '@/stores/school'
import { formatNumber } from '@/utils/format'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  title: { type: String, required: true },
  source: { type: String, default: 'risk' },
  readonly: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue'])
const router = useRouter()
const school = useSchoolStore()

const ROW_H = 64
const SORTS = [
  { label: 'Xavf balli', value: 'score' },
  { label: 'Kelmagan kunlar', value: 'absent' },
  { label: 'Baho pasayishi', value: 'drop' }
]
const LEVELS = [
  { label: 'Yuqori', value: 'high' },
  { label: "O'rta", value: 'mid' },
  { label: 'Past', value: 'low' }
]
const levelOf = score => (score >= 70 ? 'high' : score >= 50 ? 'mid' : 'low')
const drop = r =>
  r.avgBefore === null ||
  r.avgBefore === undefined ||
  r.avgNow === null ||
  r.avgNow === undefined
    ? 0
    : r.avgBefore - r.avgNow

const rows = ref(null)
const total = computed(() => rows.value?.length || 0)

async function load() {
  rows.value = null
  try {
    const res = await api.get(`/api/dashboard/${props.source}`, {
      params: { schoolId: school.activeSchoolId, limit: 2000 }
    })
    rows.value = res.data.data.rows
  } catch {
    rows.value = []
  }
}
watch(
  () => props.modelValue,
  open => open && load(),
  { immediate: true }
)

// search: applied 250 ms after the last key
const query = ref('')
const q = ref('')
let timer = null
watch(query, v => {
  clearTimeout(timer)
  timer = setTimeout(() => (q.value = (v || '').trim().toLowerCase()), 250)
})
onBeforeUnmount(() => clearTimeout(timer))

const classId = ref(null)
const sort = ref('score')
const level = ref(null)

const classOptions = computed(() => {
  const seen = new Map()
  for (const r of rows.value || []) seen.set(r.classId, r.className)
  return [...seen.entries()]
    .sort((a, b) => a[1].localeCompare(b[1], 'uz', { numeric: true }))
    .map(([value, label]) => ({ value, label }))
})

// name and class first; the level chips count within that
const base = computed(() =>
  (rows.value || []).filter(
    r =>
      (!q.value || r.name.toLowerCase().includes(q.value)) &&
      (!classId.value || r.classId === classId.value)
  )
)
const levelCounts = computed(() => {
  const c = { high: 0, mid: 0, low: 0 }
  for (const r of base.value) c[levelOf(r.score)]++
  return c
})
const filtered = computed(() => {
  const list = base.value.filter(
    r => !level.value || levelOf(r.score) === level.value
  )
  const by = {
    score: (a, b) => b.score - a.score,
    absent: (a, b) => b.absentDays - a.absentDays,
    drop: (a, b) => drop(b) - drop(a)
  }[sort.value]
  return [...list].sort((a, b) => by(a, b) || a.name.localeCompare(b.name))
})

const subtitle = computed(() =>
  rows.value
    ? `${formatNumber(total.value)} o'quvchi · xavf balli bo'yicha`
    : ''
)

function clearFilters() {
  query.value = ''
  q.value = ''
  classId.value = null
  level.value = null
  sort.value = 'score'
}

function openStudent(r) {
  if (props.readonly) return
  router.push(`/profiles/student/${r.studentId}`)
}

const exporting = ref(false)
async function exportXlsx() {
  exporting.value = true
  try {
    const res = await api.get('/api/dashboard/risk/export', {
      params: {
        schoolId: school.activeSchoolId,
        q: q.value || undefined,
        classId: classId.value || undefined,
        level: level.value || undefined,
        sort: sort.value
      },
      responseType: 'blob'
    })
    const url = URL.createObjectURL(res.data)
    const a = document.createElement('a')
    a.href = url
    a.download = 'xavf-ostidagi-oquvchilar.xlsx'
    a.click()
    URL.revokeObjectURL(url)
  } finally {
    exporting.value = false
  }
}
</script>

<style scoped>
.rd-tools {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.rd-tools__row {
  display: flex;
  gap: var(--space-2);
  align-items: center;
}

.rd-tools__row--wrap {
  flex-wrap: wrap;
  justify-content: space-between;
}

.rd-tools__search {
  flex: 1 1 auto;
  min-width: 0;
}

.rd-tools__class {
  flex: 0 0 calc(var(--field-w-sm) * 0.6);
}

.rd-levels {
  display: inline-flex;
  gap: var(--space-1);
}

.rd-level {
  --tone: var(--gr-3);
  border: 1px solid color-mix(in srgb, var(--tone) 35%, transparent);
  background: transparent;
  color: var(--tone);
  font: inherit;
  font-size: var(--text-xs);
  font-weight: 600;
  padding: var(--space-1) var(--space-2);
  border-radius: var(--radius-pill);
  cursor: pointer;
}

.rd-level b {
  font-variant-numeric: tabular-nums;
}

.rd-level--high {
  --tone: var(--st-absent);
}

.rd-level--mid {
  --tone: var(--st-late);
}

.rd-level--on {
  background: color-mix(in srgb, var(--tone) 16%, transparent);
  border-color: var(--tone);
}

.rd-level:focus-visible {
  outline: var(--border-focus);
  outline-offset: var(--hairline);
}

.rd-count {
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
}

.rd-skel {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  padding-top: var(--space-2);
}

.rd-skel__line {
  height: var(--control-h);
}

.rd-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-7) 0;
  color: var(--brand-text-muted);
  font-size: var(--text-sm);
  text-align: center;
}

.rd-empty__icon {
  font-size: var(--text-xl);
}

.rd-foot-note {
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
}

@media (max-width: 599px) {
  .rd-foot-note {
    display: none;
  }
}
</style>

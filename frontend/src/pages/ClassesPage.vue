<template>
  <page-layout
    icon="groups"
    color="var(--subj-1)"
    title="Sinflar"
    :subtitle="subtitle"
    class="clp"
  >
    <template #actions>
      <AppButton
        v-if="auth.isEditor"
        variant="secondary"
        icon="tune"
        label="Boshqarish"
        to="/app/school-classes"
      />
    </template>

    <section class="clp__hero" data-classes-hero>
      <KpiCard
        kpi-key="classes"
        compact
        label="Sinflar"
        icon="groups"
        color="var(--subj-1)"
        :loading="loading"
        :value="o ? o.classes.length : undefined"
        :note="o ? `${grades.length} ta parallel` : ''"
        :spark="null"
        style="--i: 0"
      />
      <KpiCard
        kpi-key="students"
        compact
        label="Jami o'quvchilar"
        icon="school"
        color="var(--chart-secondary)"
        :loading="loading"
        :value="o?.students"
        :format="formatNumber"
        :note="o ? `o'rtacha sinf: ${o.avgSize ?? '—'} o'quvchi` : ''"
        :spark="null"
        style="--i: 1"
      />
      <KpiCard
        kpi-key="size"
        compact
        label="O'rtacha sinf hajmi"
        icon="person"
        color="var(--subj-7)"
        :loading="loading"
        :value="o?.avgSize ?? undefined"
        :format="v => v.toFixed(1)"
        :note="sizeNote"
        :spark="null"
        style="--i: 2"
      />
      <KpiCard
        kpi-key="today"
        compact
        label="Bugungi davomat"
        icon="fact_check"
        color="var(--chart-primary)"
        :loading="loading"
        :value="o?.today ?? undefined"
        :empty-value="!!o && o.today === null"
        empty-text="Olinmagan"
        :format="v => `${v.toFixed(1)}%`"
        note="maktab bo'yicha · 20 ish kuni"
        :spark="o?.todaySpark"
        to="/attendance/calendar"
        style="--i: 3"
      />
      <KpiCard
        kpi-key="best"
        compact
        label="Eng yaxshi sinf"
        icon="emoji_events"
        color="var(--subj-4)"
        clickable
        :loading="loading"
        :value="o?.best?.quality ?? undefined"
        :empty-value="!!o && !o.best"
        :format="v => `${v.toFixed(1)}%`"
        :note="
          o?.best ? `${o.best.name} · sifat bo'yicha` : 'baholar yetarli emas'
        "
        :spark="null"
        style="--i: 4"
        @click="o?.best && open({ id: o.best.id })"
      />
    </section>

    <div class="clp__bar">
      <AppInput
        v-model="query"
        dense
        clearable
        class="clp__search"
        placeholder="Sinf yoki rahbar"
        aria-label="Qidirish"
        data-classes-search
      >
        <template #prepend><q-icon name="search" /></template>
      </AppInput>
      <AppSelect
        v-model="grade"
        dense
        clearable
        class="clp__grade"
        label="Parallel"
        :options="grades.map(g => ({ value: g, label: `${g}-sinflar` }))"
        emit-value
        map-options
        data-classes-grade
      />
      <SegTabs
        v-model="sort"
        :options="SORTS"
        label="Saralash"
        data-classes-sort
      />
      <SegTabs
        v-model="layout"
        :options="LAYOUTS"
        label="Ko'rinish"
        class="q-ml-auto"
      />
    </div>

    <div v-if="loading && !o" class="clp__grid">
      <div v-for="i in 8" :key="i" class="skel clp__skel" />
    </div>
    <EmptyState
      v-else-if="error && !o"
      icon="cloud_off"
      title="Sinflarni yuklab bo'lmadi"
      :text="error"
      action="Qayta urinish"
      action-icon="refresh"
      @action="reload"
    />
    <EmptyState
      v-else-if="o && !filtered.length"
      icon="search_off"
      :title="
        o.classes.length
          ? 'Bu filtr bo\'yicha sinf topilmadi'
          : 'Hali sinf yo\'q'
      "
      :action="
        o.classes.length
          ? 'Filtrni tozalash'
          : auth.isEditor
            ? 'Sinf qo\'shish'
            : ''
      "
      :action-icon="o.classes.length ? 'filter_alt_off' : 'add'"
      @action="
        o.classes.length
          ? clear()
          : $router.push('/app/school-classes?create=1')
      "
    />
    <div v-else-if="layout === 'cards'" class="clp__groups">
      <section
        v-for="g in groups"
        :key="g.grade"
        class="clp__group"
        :data-grade-group="g.grade"
      >
        <h3>
          {{ g.grade }}-sinflar
          <small>{{ g.items.length }} sinf · {{ g.students }} o'quvchi</small>
        </h3>
        <div class="clp__grid">
          <ClassCard
            v-for="(c, i) in g.items"
            :key="c.id"
            :c="c"
            :picked="picked.includes(c.id)"
            :style="{ '--i': i }"
            @open="open"
            @pick="pick"
          />
        </div>
      </section>
    </div>
    <div v-else class="clp__table dash-card" data-classes-table>
      <table>
        <thead>
          <tr>
            <th />
            <th>Sinf</th>
            <th>Rahbar</th>
            <th class="num">O'quvchi</th>
            <th class="num">Bugun</th>
            <th class="num">Davomat (30)</th>
            <th class="num">Sifat</th>
            <th class="num">O'zlashtirish</th>
            <th class="num">Telegram</th>
            <th class="num">Xavf</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="c in filtered"
            :key="c.id"
            :data-class-row="c.id"
            @click="open(c)"
          >
            <td @click.stop>
              <q-checkbox
                :model-value="picked.includes(c.id)"
                dense
                @update:model-value="pick(c)"
              />
            </td>
            <td
              ><b>{{ c.name }}</b></td
            >
            <td>{{ c.teacher || '—' }}</td>
            <td class="num">{{ c.students }}</td>
            <td class="num">{{ formatPercent(c.today) }}</td>
            <td class="num">{{ formatPercent(c.attendance) }}</td>
            <td class="num"
              >{{ formatPercent(c.quality) }}
              <DeltaPill :value="c.qualityDelta"
            /></td>
            <td class="num">{{ formatPercent(c.mastery) }}</td>
            <td class="num">{{ formatPercent(c.telegram) }}</td>
            <td class="num" :class="{ clp__bad: c.risk }">{{
              c.risk || '—'
            }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <transition name="clp-pop">
      <div v-if="picked.length" class="clp__pickbar dash-card" data-compare-bar>
        <span
          ><b>{{ picked.length }}</b> ta sinf tanlandi
          <small v-if="picked.length < 2"
            >(yana bittasini belgilang)</small
          ></span
        >
        <AppButton
          icon="compare_arrows"
          label="Solishtirish"
          :disable="picked.length < 2"
          data-compare-open
          @click="compareOpen = true"
        />
        <AppButton
          variant="ghost"
          icon="close"
          aria-label="Tozalash"
          @click="picked = []"
        />
      </div>
    </transition>

    <ClassCompare v-model="compareOpen" :ids="picked" />
  </page-layout>
</template>

<script setup>
// "Sinflar": the school's classes as cards grouped by grade ("5-sinflar"), with the hero
// (classes, students, average size, today's attendance, the best class), search, a grade
// filter, sorting (name / attendance / quality / risk), cards or a table, and up to three
// classes picked for "Solishtirish". A card opens the class page.
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PageLayout from '@/components/PageLayout.vue'
import AppButton from '@/components/app/AppButton.vue'
import AppInput from '@/components/app/AppInput.vue'
import AppSelect from '@/components/app/AppSelect.vue'
import KpiCard from '@/components/dashboard/KpiCard.vue'
import SegTabs from '@/components/dashboard/SegTabs.vue'
import DeltaPill from '@/components/dashboard/DeltaPill.vue'
import EmptyState from '@/components/pages/EmptyState.vue'
import ClassCard from '@/components/classes/ClassCard.vue'
import ClassCompare from '@/components/classes/ClassCompare.vue'
import { useWidget } from '@/composables/useWidget'
import { useAuthStore } from '@/stores/auth'
import { useSchoolStore } from '@/stores/school'
import { formatNumber, formatPercent } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const school = useSchoolStore()

const SORTS = [
  { value: 'name', label: 'Nom' },
  { value: 'attendance', label: 'Davomat' },
  { value: 'quality', label: 'Sifat' },
  { value: 'risk', label: 'Xavf' }
]
const LAYOUTS = [
  { value: 'cards', label: 'Karta' },
  { value: 'table', label: 'Jadval' }
]

const { data, loading, error, reload } = useWidget(
  '/api/pages/classes/overview'
)
const o = computed(() => data.value)

const query = ref(route.query.q || '')
const grade = ref(Number(route.query.grade) || null)
const sort = ref(
  SORTS.some(s => s.value === route.query.sort) ? route.query.sort : 'name'
)
const layout = ref(route.query.layout === 'table' ? 'table' : 'cards')
watch([query, grade, sort, layout], () =>
  router.replace({
    query: {
      q: query.value || undefined,
      grade: grade.value || undefined,
      sort: sort.value === 'name' ? undefined : sort.value,
      layout: layout.value === 'cards' ? undefined : layout.value
    }
  })
)
function clear() {
  query.value = ''
  grade.value = null
}

const grades = computed(() =>
  [...new Set((o.value?.classes || []).map(c => c.grade))].sort((a, b) => a - b)
)
const sizeNote = computed(() => {
  const list = o.value?.classes || []
  if (!list.length) return ''
  const sizes = list.map(c => c.students)
  return `eng kichik ${Math.min(...sizes)} · eng katta ${Math.max(...sizes)}`
})
const subtitle = computed(() =>
  [school.activeSchoolName, o.value ? `${o.value.classes.length} sinf` : '']
    .filter(Boolean)
    .join(' · ')
)

const BY = {
  name: (a, b) => a.grade - b.grade || a.name.localeCompare(b.name, 'uz'),
  attendance: (a, b) => (b.attendance ?? -1) - (a.attendance ?? -1),
  quality: (a, b) => (b.quality ?? -1) - (a.quality ?? -1),
  risk: (a, b) => b.risk - a.risk
}
const filtered = computed(() => {
  const q = (query.value || '').trim().toLowerCase().replace(/\s+/g, '-')
  return (o.value?.classes || [])
    .filter(c => !grade.value || c.grade === grade.value)
    .filter(
      c =>
        !q ||
        c.name.toLowerCase().includes(q) ||
        (c.teacher || '')
          .toLowerCase()
          .includes((query.value || '').trim().toLowerCase())
    )
    .sort((a, b) => BY[sort.value](a, b) || a.name.localeCompare(b.name, 'uz'))
})
// by grade; inside a grade the chosen order
const groups = computed(() => {
  const m = new Map()
  for (const c of filtered.value) {
    if (!m.has(c.grade))
      m.set(c.grade, { grade: c.grade, items: [], students: 0 })
    const g = m.get(c.grade)
    g.items.push(c)
    g.students += c.students
  }
  return [...m.values()].sort((a, b) => a.grade - b.grade)
})

function open(c) {
  router.push(`/classes/${c.id}`)
}

const picked = ref([])
function pick(c) {
  if (picked.value.includes(c.id))
    picked.value = picked.value.filter(x => x !== c.id)
  else if (picked.value.length < 3) picked.value = [...picked.value, c.id]
}
const compareOpen = ref(false)
</script>

<style scoped>
.clp :deep(.page-layout__body) {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.clp__hero {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: var(--space-3);
}

.clp__bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-3);
}

.clp__search {
  width: var(--field-w);
}

.clp__grade {
  width: var(--field-w-sm);
}

.clp__groups {
  display: grid;
  grid-template-columns: repeat(
    auto-fill,
    minmax(calc(var(--grid-min) * 2 + var(--space-3)), 1fr)
  );
  gap: var(--space-5) var(--space-5);
}

.clp__group h3 {
  margin: 0 0 var(--space-3);
  font-size: var(--text-md);
  font-weight: 700;
}

.clp__group h3 small {
  margin-left: var(--space-2);
  font-size: var(--text-xs);
  font-weight: 500;
  color: var(--brand-text-muted);
}

.clp__grid {
  display: grid;
  grid-template-columns: repeat(
    auto-fill,
    minmax(min(var(--grid-min), 100%), 1fr)
  );
  gap: var(--space-3);
}

.clp__skel {
  height: calc(var(--space-8) * 5);
  border-radius: var(--radius-lg);
}

.clp__table {
  padding: var(--space-2);
  overflow-x: auto;
}

.clp__table:hover {
  transform: none;
}

.clp__table table {
  width: 100%;
  border-collapse: collapse;
  font-size: var(--text-sm);
}

.clp__table th {
  padding: var(--space-2);
  font-size: var(--text-xs);
  font-weight: 700;
  color: var(--brand-text-muted);
  text-align: left;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  white-space: nowrap;
}

.clp__table td {
  padding: var(--space-2);
  border-top: 1px solid var(--dash-edge);
  white-space: nowrap;
}

.clp__table tbody tr {
  cursor: pointer;
}

.clp__table tbody tr:hover {
  background: var(--surface-2);
}

.clp__table .num {
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.clp__bad {
  color: var(--st-absent);
  font-weight: 700;
}

.clp__pickbar {
  position: sticky;
  bottom: var(--space-4);
  z-index: 5;
  align-self: center;
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-2) var(--space-2) var(--space-2) var(--space-4);
  border-radius: var(--radius-pill);
  box-shadow: var(--dash-shadow-hover);
}

.clp__pickbar small {
  color: var(--brand-text-muted);
}

.clp-pop-enter-active,
.clp-pop-leave-active {
  transition:
    opacity var(--dur) var(--ease-out),
    transform var(--dur) var(--ease-out);
}

.clp-pop-enter-from,
.clp-pop-leave-to {
  opacity: 0;
  transform: translateY(var(--space-4));
}

@media (max-width: 1439px) {
  .clp__hero {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 767px) {
  .clp__groups {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 599px) {
  .clp__hero {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .clp__search,
  .clp__grade {
    width: 100%;
  }
}

@media (prefers-reduced-motion: reduce) {
  .clp-pop-enter-active,
  .clp-pop-leave-active {
    transition: none;
  }
}
</style>

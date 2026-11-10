<template>
  <page-layout
    icon="work_history"
    color="#0f766e"
    title="O'qituvchilar ish yuklamasi"
    subtitle="Dars jadvali bo'yicha: haftalik soatlar va tanlangan davrda o'tilgan darslar (yakshanba, bayram va ta'til kunlari hisobga olinmaydi)"
  >
    <template #actions>
      <q-btn-toggle
        v-model="preset"
        no-caps
        unelevated
        rounded
        toggle-color="primary"
        :options="[
          { label: 'Shu hafta', value: 'week' },
          { label: 'Shu oy', value: 'month' },
          { label: 'O‘tgan oy', value: 'prev' },
          { label: 'Boshqa', value: 'custom' }
        ]"
        @update:model-value="applyPreset"
      />
      <AppButton
        variant="secondary"
        icon="download"
        label="Excel"
        :disable="!report?.rows.length"
        @click="exportXlsx"
      />
    </template>

    <div v-if="preset === 'custom'" class="row q-col-gutter-sm">
      <div class="col-6 col-sm-3">
        <AppDateInput
          v-model="from"
          label="Boshlanish"
          @update:model-value="load"
        />
      </div>
      <div class="col-6 col-sm-3">
        <AppDateInput
          v-model="to"
          label="Tugash"
          :min="from"
          @update:model-value="load"
        />
      </div>
    </div>

    <div v-if="report" class="row q-col-gutter-md">
      <div class="col-6 col-md-3">
        <div class="brand-card stat">
          <div class="stat__value">{{ report.rows.length }}</div>
          <div class="stat__label">o'qituvchi</div>
        </div>
      </div>
      <div class="col-6 col-md-3">
        <div class="brand-card stat">
          <div class="stat__value">{{ report.totalWeekly }}</div>
          <div class="stat__label">haftalik dars soati (jami)</div>
        </div>
      </div>
      <div class="col-6 col-md-3">
        <div class="brand-card stat">
          <div class="stat__value">{{ report.schoolDays }}</div>
          <div class="stat__label">o'quv kuni davrda</div>
        </div>
      </div>
      <div class="col-6 col-md-3">
        <div class="brand-card stat">
          <div class="stat__value">{{ report.totalHeld }}</div>
          <div class="stat__label">o'tilgan dars (jami)</div>
        </div>
      </div>
    </div>

    <div class="brand-card overflow-hidden">
      <q-table
        :rows="report?.rows || []"
        :columns="columns"
        row-key="employeeId"
        :loading="loading"
        flat
        class="brand-table"
        :pagination="{ rowsPerPage: 0 }"
        hide-pagination
        :filter="search"
      >
        <template #top>
          <AppInput
            v-model="search"
            dense
            clearable
            placeholder="Qidirish…"
            style="width: 260px"
          >
            <template #prepend><q-icon name="search" /></template>
          </AppInput>
          <q-space />
          <div class="text-caption muted-text" v-if="report">
            {{ formatDate(report.from) }} – {{ formatDate(report.to) }}
            <span v-if="report.countedTo !== report.to">
              (bugungacha: {{ formatDate(report.countedTo) }})</span
            >
          </div>
        </template>
        <template #body-cell-name="props">
          <q-td :props="props">
            <router-link
              :to="`/profiles/teacher/${props.row.employeeId}`"
              class="text-weight-medium link"
              >{{ props.row.name }}</router-link
            >
            <div class="text-caption muted-text">
              {{ props.row.position || '—' }}
            </div>
          </q-td>
        </template>
        <template #body-cell-weekly="props">
          <q-td :props="props">
            <div class="row items-center no-wrap q-gutter-x-sm">
              <span class="text-weight-bold tabular-nums">{{
                props.row.weeklyLessons
              }}</span>
              <q-linear-progress
                :value="Math.min(1, props.row.weeklyLessons / maxWeekly)"
                rounded
                size="6px"
                :color="props.row.weeklyLessons > 30 ? 'negative' : 'primary'"
                style="width: 70px"
              />
            </div>
          </q-td>
        </template>
        <template #body-cell-loads="props">
          <q-td :props="props" class="loads-cell">
            <q-chip
              v-for="l in props.row.loads"
              :key="l.className + l.subject"
              dense
              square
              size="sm"
              class="load-chip"
            >
              {{ l.className }} · {{ l.subject }}
              <b class="q-ml-xs">{{ l.weekly }}</b>
            </q-chip>
            <span v-if="!props.row.loads.length" class="muted-text">—</span>
          </q-td>
        </template>
        <template #body-cell-subs="props">
          <q-td :props="props" class="tabular-nums">
            <span v-if="props.row.substitutedAway" class="text-negative"
              >−{{ props.row.substitutedAway }}</span
            >
            <span
              v-if="props.row.substitutedAway && props.row.coveredForOthers"
            >
              /
            </span>
            <span v-if="props.row.coveredForOthers" class="text-positive"
              >+{{ props.row.coveredForOthers }}</span
            >
            <span
              v-if="!props.row.substitutedAway && !props.row.coveredForOthers"
              class="muted-text"
              >—</span
            >
          </q-td>
        </template>
      </q-table>
    </div>
  </page-layout>
</template>

<script setup>
import AppButton from '@/components/app/AppButton.vue'
import AppInput from '@/components/app/AppInput.vue'
import AppDateInput from '@/components/app/AppDateInput.vue'
import PageLayout from '@/components/PageLayout.vue'
import { computed, onMounted, ref, watch } from 'vue'
import { useQuasar } from 'quasar'
import { api } from '@/boot/axios'
import { useSchoolStore } from '@/stores/school'
import { formatDate, toLocalDateStr, todayStr } from '@/utils/date'
import { errorText } from '@/utils/modules'

const $q = useQuasar()
const schoolStore = useSchoolStore()

const preset = ref('month')
const from = ref('')
const to = ref('')
const report = ref(null)
const loading = ref(false)
const search = ref('')

const columns = [
  {
    name: 'name',
    label: "O'qituvchi",
    field: 'name',
    align: 'left',
    sortable: true
  },
  {
    name: 'weekly',
    label: 'Haftalik soat',
    field: 'weeklyLessons',
    align: 'left',
    sortable: true
  },
  {
    name: 'loads',
    label: 'Sinf · fan · soat',
    field: r => r.loads.map(l => `${l.className} ${l.subject}`).join(' '),
    align: 'left'
  },
  {
    name: 'planned',
    label: "Jadval bo'yicha",
    field: 'planned',
    align: 'right',
    sortable: true
  },
  {
    name: 'subs',
    label: "O'rinbosarlik",
    field: 'coveredForOthers',
    align: 'right'
  },
  {
    name: 'total',
    label: "O'tilgan dars",
    field: 'total',
    align: 'right',
    sortable: true,
    classes: 'text-weight-bold tabular-nums'
  }
]

const maxWeekly = computed(() =>
  Math.max(1, ...(report.value?.rows || []).map(r => r.weeklyLessons))
)

function applyPreset() {
  const t = new Date(`${todayStr()}T00:00:00`)
  if (preset.value === 'week') {
    const monday = new Date(t)
    monday.setDate(t.getDate() - ((t.getDay() + 6) % 7))
    const saturday = new Date(monday)
    saturday.setDate(monday.getDate() + 5)
    from.value = toLocalDateStr(monday)
    to.value = toLocalDateStr(saturday)
  } else if (preset.value === 'month') {
    from.value = toLocalDateStr(new Date(t.getFullYear(), t.getMonth(), 1))
    to.value = toLocalDateStr(new Date(t.getFullYear(), t.getMonth() + 1, 0))
  } else if (preset.value === 'prev') {
    from.value = toLocalDateStr(new Date(t.getFullYear(), t.getMonth() - 1, 1))
    to.value = toLocalDateStr(new Date(t.getFullYear(), t.getMonth(), 0))
  }
  load()
}

async function load() {
  if (!schoolStore.activeSchoolId || !from.value || !to.value) return
  loading.value = true
  try {
    report.value = (
      await api.get('/api/workload', {
        params: {
          schoolId: schoolStore.activeSchoolId,
          from: from.value,
          to: to.value
        }
      })
    ).data
  } catch (e) {
    $q.notify({ type: 'negative', message: errorText(e) })
  } finally {
    loading.value = false
  }
}

async function exportXlsx() {
  const r = report.value
  const body = {
    title: "O'qituvchilar ish yuklamasi",
    subtitle: [
      schoolStore.activeSchoolName,
      `${formatDate(r.from)} – ${formatDate(r.to)}`,
      `${r.schoolDays} o'quv kuni`
    ]
      .filter(Boolean)
      .join(' · '),
    columns: [
      "O'qituvchi",
      'Lavozim',
      'Haftalik soat',
      'Sinf va fanlar',
      "Jadval bo'yicha",
      "Uning o'rniga boshqasi",
      "Boshqaning o'rniga",
      "O'tilgan dars"
    ].map(label => ({ label, type: 'text' })),
    rows: r.rows.map(x => [
      x.name,
      x.position || '',
      String(x.weeklyLessons),
      x.loads.map(l => `${l.className} ${l.subject} (${l.weekly})`).join(', '),
      String(x.planned),
      String(x.substitutedAway),
      String(x.coveredForOthers),
      String(x.total)
    ])
  }
  try {
    const res = await api.post('/api/export/table', body, {
      params: { format: 'xlsx' },
      responseType: 'blob'
    })
    const url = URL.createObjectURL(res.data)
    const a = document.createElement('a')
    a.href = url
    a.download = `ish-yuklamasi-${r.from}-${r.to}.xlsx`
    document.body.appendChild(a)
    a.click()
    a.remove()
    setTimeout(() => URL.revokeObjectURL(url), 1000)
  } catch (e) {
    $q.notify({ type: 'negative', message: errorText(e) })
  }
}

watch(() => schoolStore.activeSchoolId, load)
onMounted(applyPreset)
</script>

<style scoped>
.stat {
  padding: 14px 16px;
}
.stat__value {
  font-size: 26px;
  font-weight: 800;
}
.stat__label {
  font-size: 12px;
  opacity: 0.7;
}
.loads-cell {
  white-space: normal;
  max-width: 460px;
}
.load-chip {
  margin: 2px 4px 2px 0;
}
.link {
  color: inherit;
  text-decoration: none;
}
</style>

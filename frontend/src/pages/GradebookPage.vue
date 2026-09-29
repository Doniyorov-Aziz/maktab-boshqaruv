<template>
  <page-layout
    icon="grade"
    color="#eab308"
    title="Baholar jurnali"
    :subtitle="schoolStore.activeSchoolName"
  >
    <div class="brand-card q-pa-md q-mb-md">
      <div class="row q-col-gutter-md items-end">
        <div class="col-12 col-sm-3">
          <q-select
            v-model="selectedClassId"
            :options="classOptions"
            option-value="value"
            option-label="label"
            emit-value
            map-options
            outlined
            dense
            label="Sinf"
            @update:model-value="loadGradebook"
          />
        </div>
        <div class="col-12 col-sm-3">
          <q-select
            v-model="selectedSubjectId"
            :options="subjectOptions"
            option-value="value"
            option-label="label"
            emit-value
            map-options
            outlined
            dense
            label="Fan"
            @update:model-value="loadGradebook"
          />
        </div>
        <div class="col-6 col-sm-2">
          <date-field
            v-model="fromDate"
            label="Dan"
            @update:model-value="loadGradebook"
          />
        </div>
        <div class="col-6 col-sm-2">
          <date-field
            v-model="toDate"
            label="Gacha"
            @update:model-value="loadGradebook"
          />
        </div>
        <div class="col-12 col-sm-2">
          <q-btn
            color="primary"
            unelevated
            no-caps
            class="full-width"
            icon="add"
            label="Baho qo'yish"
            :disable="!selectedClassId || !selectedSubjectId"
            @click="openGradeDialog(null, null)"
          />
        </div>
      </div>
    </div>

    <div v-if="loading" class="brand-card q-pa-md">
      <q-skeleton
        v-for="i in 6"
        :key="i"
        type="text"
        height="40px"
        class="q-mb-sm"
      />
    </div>

    <div
      v-else-if="!selectedClassId || !selectedSubjectId"
      class="brand-card q-pa-xl column flex-center muted-text"
    >
      <q-icon name="grade" size="48px" class="q-mb-sm" />
      <div class="text-subtitle2"
        >Jurnalni ko'rish uchun sinf va fanni tanlang</div
      >
    </div>

    <div v-else class="brand-card overflow-hidden">
      <div class="table-scroll">
        <table class="gradebook-table">
          <thead>
            <tr>
              <th class="sticky-col student-col">O'quvchi</th>
              <th v-for="d in dateColumns" :key="d" class="date-col">{{
                formatShortDate(d)
              }}</th>
              <th class="avg-col">O'rtacha</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="student in students" :key="student.studentId">
              <td class="sticky-col student-col">{{ student.studentName }}</td>
              <td
                v-for="d in dateColumns"
                :key="d"
                class="date-col grade-cell"
                @click="onCellClick(student, d)"
              >
                <span
                  v-if="gradeFor(student.studentId, d)"
                  class="grade-chip"
                  :class="scoreClass(gradeFor(student.studentId, d).score)"
                >
                  {{ gradeFor(student.studentId, d).score }}
                </span>
                <span v-else class="grade-chip grade-chip--empty">+</span>
              </td>
              <td class="avg-col">
                <span
                  v-if="student.average != null"
                  class="avg-badge"
                  :class="scoreClass(Math.round(student.average))"
                >
                  {{ student.average.toFixed(2) }}
                </span>
                <span v-else class="muted-text">—</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div
      v-if="!loading && selectedClassId && selectedSubjectId && grades.length"
      class="brand-card q-pa-md q-mt-md"
    >
      <div class="text-subtitle1 text-weight-semibold q-mb-md"
        >Sinf bo'yicha baholar taqsimoti</div
      >
      <div style="height: 200px">
        <Bar
          :data="distributionChartData"
          :options="distributionChartOptions"
        />
      </div>
    </div>

    <q-dialog v-model="dialogOpen" persistent>
      <q-card style="width: 100%; max-width: 420px; border-radius: 18px">
        <q-card-section class="row items-center q-pb-none">
          <div class="text-h6">{{
            editingGradeId ? 'Bahoni tahrirlash' : "Baho qo'yish"
          }}</div>
          <q-space />
          <q-btn flat round dense icon="close" v-close-popup />
        </q-card-section>
        <q-form @submit.prevent="saveGrade">
          <q-card-section class="q-gutter-md q-pt-md">
            <q-select
              v-model="gradeForm.studentId"
              :options="
                students.map(s => ({
                  value: s.studentId,
                  label: s.studentName
                }))
              "
              option-value="value"
              option-label="label"
              emit-value
              map-options
              outlined
              dense
              label="O'quvchi"
              :rules="[v => !!v || 'Majburiy']"
            />
            <date-field
              v-model="gradeForm.gradeDate"
              label="Sana"
              :rules="[v => !!v || 'Majburiy']"
            />
            <div>
              <div class="text-caption muted-text q-mb-xs">Baho</div>
              <div class="row q-gutter-sm">
                <q-btn
                  v-for="s in [2, 3, 4, 5]"
                  :key="s"
                  :label="String(s)"
                  round
                  unelevated
                  class="score-pick-btn"
                  :style="{
                    background:
                      gradeForm.score === s ? scoreColor(s) : 'transparent',
                    color: gradeForm.score === s ? 'white' : scoreColor(s),
                    border: '2px solid ' + scoreColor(s)
                  }"
                  @click="gradeForm.score = s"
                />
              </div>
            </div>
            <q-select
              v-model="gradeForm.type"
              :options="[
                { value: 'CURRENT', label: 'Joriy' },
                { value: 'EXAM', label: 'Nazorat' },
                { value: 'QUARTERLY', label: 'Chorak' }
              ]"
              option-value="value"
              option-label="label"
              emit-value
              map-options
              outlined
              dense
              label="Turi"
            />
            <q-input v-model="gradeForm.comment" outlined dense label="Izoh" />
            <q-banner
              v-if="formError"
              class="bg-red-1 text-negative rounded-borders"
              dense
              >{{ formError }}</q-banner
            >
          </q-card-section>
          <q-card-actions align="right" class="q-pa-md">
            <q-btn
              v-if="editingGradeId"
              flat
              no-caps
              color="negative"
              label="O'chirish"
              @click="deleteGrade"
            />
            <q-space />
            <q-btn
              flat
              label="Bekor qilish"
              no-caps
              color="grey-7"
              v-close-popup
            />
            <q-btn
              type="submit"
              color="primary"
              label="Saqlash"
              no-caps
              unelevated
              :loading="saving"
            />
          </q-card-actions>
        </q-form>
      </q-card>
    </q-dialog>
  </page-layout>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useQuasar } from 'quasar'
import { Bar } from 'vue-chartjs'
import {
  Chart as ChartJS,
  Title,
  Tooltip,
  BarElement,
  CategoryScale,
  LinearScale
} from 'chart.js'
import { api } from '@/boot/axios'
import PageLayout from '@/components/PageLayout.vue'
import DateField from '@/components/DateField.vue'
import { useSchoolStore } from '@/stores/school'
import { toLocalDateStr } from '@/utils/date'

ChartJS.register(Title, Tooltip, BarElement, CategoryScale, LinearScale)

const $q = useQuasar()
const schoolStore = useSchoolStore()

const classOptions = ref([])
const subjectOptions = ref([])
const selectedClassId = ref(null)
const selectedSubjectId = ref(null)

const today = new Date()
const sixtyDaysAgo = new Date(today.getTime() - 59 * 86400000)
const fromDate = ref(toLocalDateStr(sixtyDaysAgo))
const toDate = ref(toLocalDateStr(today))

const students = ref([])
const grades = ref([])
const loading = ref(false)
const saving = ref(false)
const dialogOpen = ref(false)
const editingGradeId = ref(null)
const formError = ref('')
const gradeForm = ref({
  studentId: null,
  gradeDate: '',
  score: null,
  type: 'CURRENT',
  comment: ''
})

const dateColumns = computed(() => {
  const set = new Set(grades.value.map(g => g.gradeDate))
  return [...set].sort()
})

function gradeFor(studentId, date) {
  return grades.value.find(
    g => g.studentId === studentId && g.gradeDate === date
  )
}

function formatShortDate(d) {
  const [, m, day] = d.split('-')
  return `${day}.${m}`
}

const scoreColors = { 5: '#10b981', 4: '#3b82f6', 3: '#f59e0b', 2: '#ef4444' }
function scoreColor(score) {
  return scoreColors[Math.round(score)] || scoreColors[2]
}

function scoreClass(score) {
  if (score >= 5) return 'score-5'
  if (score >= 4) return 'score-4'
  if (score >= 3) return 'score-3'
  return 'score-2'
}

async function loadClasses() {
  if (!schoolStore.activeSchoolId) return
  const response = await api.get('/api/school-classes', {
    params: {
      schoolId: schoolStore.activeSchoolId,
      size: 100,
      sort: 'gradeNumber,asc'
    }
  })
  classOptions.value = response.data.content.map(c => ({
    value: c.id,
    label: `${c.gradeNumber}-${c.sectionLetter}`
  }))
  if (!selectedClassId.value && classOptions.value.length) {
    selectedClassId.value = classOptions.value[0].value
  }
}

async function loadSubjects() {
  if (!schoolStore.activeSchoolId) return
  const response = await api.get('/api/subjects', {
    params: { schoolId: schoolStore.activeSchoolId, size: 100 }
  })
  subjectOptions.value = response.data.content.map(s => ({
    value: s.id,
    label: s.name
  }))
  if (!selectedSubjectId.value && subjectOptions.value.length) {
    selectedSubjectId.value = subjectOptions.value[0].value
  }
}

async function loadGradebook() {
  if (!selectedClassId.value || !selectedSubjectId.value) return
  loading.value = true
  try {
    const response = await api.get('/api/grades/gradebook', {
      params: {
        schoolClassId: selectedClassId.value,
        subjectId: selectedSubjectId.value,
        from: fromDate.value,
        to: toDate.value
      }
    })
    students.value = response.data.students
    grades.value = response.data.grades
  } catch (error) {
    $q.notify({
      type: 'negative',
      message: error.response?.data || 'Xatolik yuz berdi'
    })
  } finally {
    loading.value = false
  }
}

const distributionChartData = computed(() => {
  const counts = { 2: 0, 3: 0, 4: 0, 5: 0 }
  for (const g of grades.value) {
    if (counts[g.score] != null) counts[g.score]++
  }
  return {
    labels: ['2', '3', '4', '5'],
    datasets: [
      {
        label: 'Baholar soni',
        data: [counts[2], counts[3], counts[4], counts[5]],
        backgroundColor: [
          scoreColors[2],
          scoreColors[3],
          scoreColors[4],
          scoreColors[5]
        ],
        borderRadius: 6,
        barThickness: 40
      }
    ]
  }
})
const distributionChartOptions = computed(() => {
  const textColor = $q.dark.isActive ? '#f1f5f9' : '#334155'
  const gridColor = $q.dark.isActive
    ? 'rgba(148,163,184,0.16)'
    : 'rgba(148,163,184,0.18)'
  return {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { display: false } },
    scales: {
      x: { ticks: { color: textColor }, grid: { display: false } },
      y: {
        beginAtZero: true,
        ticks: { color: textColor, precision: 0 },
        grid: { color: gridColor }
      }
    }
  }
})

function onCellClick(student, date) {
  const existing = gradeFor(student.studentId, date)
  openGradeDialog(existing, student.studentId, date)
}

function openGradeDialog(existingGrade, studentId, date) {
  formError.value = ''
  if (existingGrade) {
    editingGradeId.value = existingGrade.id
    gradeForm.value = {
      studentId: existingGrade.studentId,
      gradeDate: existingGrade.gradeDate,
      score: existingGrade.score,
      type: existingGrade.type,
      comment: existingGrade.comment || ''
    }
  } else {
    editingGradeId.value = null
    gradeForm.value = {
      studentId: studentId || null,
      gradeDate: date || toDate.value,
      score: null,
      type: 'CURRENT',
      comment: ''
    }
  }
  dialogOpen.value = true
}

async function saveGrade() {
  saving.value = true
  formError.value = ''
  try {
    const payload = {
      studentId: gradeForm.value.studentId,
      subjectId: selectedSubjectId.value,
      gradeDate: gradeForm.value.gradeDate,
      score: gradeForm.value.score,
      type: gradeForm.value.type,
      comment: gradeForm.value.comment
    }
    if (editingGradeId.value) {
      await api.put(`/api/grades/${editingGradeId.value}`, payload)
    } else {
      await api.post('/api/grades', payload)
    }
    dialogOpen.value = false
    $q.notify({ type: 'positive', message: 'Saqlandi', icon: 'check_circle' })
    loadGradebook()
  } catch (error) {
    formError.value = error.response?.data || 'Xatolik yuz berdi'
  } finally {
    saving.value = false
  }
}

async function deleteGrade() {
  try {
    await api.delete(`/api/grades/${editingGradeId.value}`)
    dialogOpen.value = false
    $q.notify({ type: 'positive', message: "O'chirildi", icon: 'check_circle' })
    loadGradebook()
  } catch (error) {
    $q.notify({
      type: 'negative',
      message: error.response?.data || 'Xatolik yuz berdi'
    })
  }
}

async function loadInitial() {
  await Promise.all([loadClasses(), loadSubjects()])
  if (selectedClassId.value && selectedSubjectId.value) {
    loadGradebook()
  }
}
loadInitial()
</script>

<style scoped>
.muted-text {
  color: var(--brand-text-muted);
}

.table-scroll {
  overflow-x: auto;
}

.gradebook-table {
  border-collapse: collapse;
  width: 100%;
  font-size: 13px;
}

.gradebook-table th,
.gradebook-table td {
  padding: 8px 10px;
  border-bottom: 1px solid var(--brand-border);
  text-align: center;
  white-space: nowrap;
}

.student-col {
  text-align: left;
  min-width: 160px;
}

.sticky-col {
  position: sticky;
  left: 0;
  background: var(--card-bg);
  z-index: 1;
}

.date-col {
  min-width: 46px;
}

.avg-col {
  min-width: 70px;
  font-weight: 700;
}

.grade-cell {
  cursor: pointer;
}

.grade-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: 8px;
  font-weight: 700;
  color: white;
}

.grade-chip--empty {
  background: transparent;
  color: var(--brand-text-muted);
  opacity: 0;
  transition: opacity 0.15s ease;
}

.grade-cell:hover .grade-chip--empty {
  opacity: 1;
  background: rgba(79, 70, 229, 0.12);
  color: var(--q-primary);
}

.avg-badge {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 999px;
  color: white;
  font-weight: 700;
}

.score-5 {
  background: #10b981;
}

.score-4 {
  background: #3b82f6;
}

.score-3 {
  background: #f59e0b;
}

.score-2 {
  background: #ef4444;
}
</style>

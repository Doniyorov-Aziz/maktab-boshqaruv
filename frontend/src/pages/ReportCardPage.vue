<template>
  <page-layout
    icon="workspace_premium"
    color="#6366f1"
    title="Chorak tabeli"
    subtitle="Chorak bahosi chorakdagi baholardan taklif qilinadi (4.5 → 5); tasdiqlangani jurnalga «Chorak» bahosi bo'lib yoziladi"
  >
    <template #actions>
      <AppButton
        variant="secondary"
        icon="picture_as_pdf"
        label="Sinf tabeli (PDF)"
        :disable="!card"
        :loading="pdfLoading"
        @click="pdf()"
      />
      <AppButton
        v-if="authStore.isEditor"
        variant="secondary"
        icon="send"
        label="Ota-onalarga yuborish"
        :disable="!card || !card.stored"
        @click="send"
      />
      <AppButton
        v-if="authStore.isEditor"
        icon="done_all"
        label="Chorak baholarini qo'yish"
        :disable="!card || !card.missing"
        :loading="finalizing"
        @click="finalize"
      />
    </template>

    <div class="row q-col-gutter-sm items-center">
      <div class="col-12 col-sm-4 col-md-3">
        <AppSelect
          v-model="classId"
          :options="classes"
          label="Sinf"
          emit-value
          map-options
          @update:model-value="onClass"
        />
      </div>
      <div class="col-12 col-sm-8 col-md-9" v-if="terms.length">
        <q-btn-toggle
          v-model="termId"
          no-caps
          unelevated
          rounded
          toggle-color="primary"
          :options="terms.map(t => ({ label: t.name, value: t.id }))"
          @update:model-value="load"
        />
      </div>
    </div>

    <q-banner v-if="error" rounded class="brand-card info-banner">
      <template #avatar><q-icon name="info" color="warning" /></template>
      {{ error }}
    </q-banner>

    <div v-if="card" class="row q-col-gutter-md">
      <div class="col-6 col-md-3">
        <div class="brand-card stat">
          <div class="stat__value">{{ card.rows.length }}</div>
          <div class="stat__label">o'quvchi</div>
        </div>
      </div>
      <div class="col-6 col-md-3">
        <div class="brand-card stat">
          <div class="stat__value">{{ card.subjects.length }}</div>
          <div class="stat__label">fan</div>
        </div>
      </div>
      <div class="col-6 col-md-3">
        <div class="brand-card stat">
          <div class="stat__value text-positive">{{ card.stored }}</div>
          <div class="stat__label">qo'yilgan chorak bahosi</div>
        </div>
      </div>
      <div class="col-6 col-md-3">
        <div class="brand-card stat">
          <div class="stat__value text-warning">{{ card.missing }}</div>
          <div class="stat__label">tasdiqlanmagan taklif</div>
        </div>
      </div>
    </div>

    <div v-if="card" class="brand-card overflow-auto tabel-wrap">
      <table class="tabel">
        <thead>
          <tr>
            <th class="sticky-col">O'quvchi</th>
            <th v-for="s in card.subjects" :key="s.id" class="subject-th">
              <div class="subject-name" :title="s.name">{{ s.name }}</div>
            </th>
            <th>O'rtacha</th>
            <th>Davomat</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="r in card.rows" :key="r.studentId">
            <td class="sticky-col name-cell">
              <router-link :to="`/profiles/student/${r.studentId}`">{{
                r.name
              }}</router-link>
            </td>
            <td
              v-for="(c, i) in r.cells"
              :key="i"
              class="grade-cell"
              :class="cellClass(c)"
              @click="authStore.isEditor && openCell(r, c)"
            >
              <span v-if="c.quarterly != null" class="stored">{{
                c.quarterly
              }}</span>
              <span v-else-if="c.suggested != null" class="suggested">{{
                c.suggested
              }}</span>
              <span v-else class="muted-text">·</span>
              <q-tooltip v-if="c.count">
                O'rtacha {{ c.average }} · {{ c.count }} ta baho
                <span v-if="c.count < 3"> (kam baho)</span>
                <span v-if="c.quarterly != null && c.quarterly !== c.suggested">
                  · taklif: {{ c.suggested }}</span
                >
              </q-tooltip>
            </td>
            <td class="tabular-nums text-weight-medium">
              {{ r.average ?? '—' }}
            </td>
            <td class="tabular-nums">
              <span v-if="r.attendancePercent != null"
                >{{ r.attendancePercent }}%</span
              >
              <span v-else class="muted-text">—</span>
              <div v-if="r.absent" class="text-caption text-negative">
                {{ r.absent }} dars qoldirgan
              </div>
            </td>
            <td>
              <AppButton
                variant="ghost"
                size="sm"
                icon="picture_as_pdf"
                aria-label="Tabel PDF"
                @click="pdf(r)"
              >
                <q-tooltip>O'quvchi tabeli (PDF)</q-tooltip>
              </AppButton>
            </td>
          </tr>
        </tbody>
      </table>
      <div class="legend text-caption muted-text q-pa-sm">
        <span class="stored">5</span> — qo'yilgan chorak bahosi ·
        <span class="suggested">5</span> — taklif (hali qo'yilmagan) · katakni
        bosib bahoni o'zgartirish mumkin
      </div>
    </div>

    <q-dialog v-model="cellOpen">
      <q-card class="dialog-card" style="width: 360px">
        <q-card-section v-if="editing">
          <div class="text-subtitle1 text-weight-bold">
            {{ editing.row.name }}
          </div>
          <div class="text-caption muted-text q-mb-md">
            {{ subjectName(editing.cell.subjectId) }} · {{ card.term.name }}
            <span v-if="editing.cell.count">
              · o'rtacha {{ editing.cell.average }} ({{ editing.cell.count }} ta
              baho)</span
            >
          </div>
          <div class="row q-gutter-sm justify-center">
            <AppButton
              v-for="n in [1, 2, 3, 4, 5]"
              :key="n"
              :variant="editing.cell.quarterly === n ? 'primary' : 'secondary'"
              :label="n"
              class="score-btn"
              @click="setCell(n)"
            />
          </div>
        </q-card-section>
        <q-card-actions align="between" class="q-px-md q-pb-md">
          <AppButton
            variant="ghost"
            color="negative"
            label="Bahoni olib tashlash"
            :disable="editing?.cell.quarterly == null"
            @click="setCell(null)"
          />
          <AppButton variant="ghost" label="Yopish" v-close-popup />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </page-layout>
</template>

<script setup>
import AppButton from '@/components/app/AppButton.vue'
import AppSelect from '@/components/app/AppSelect.vue'
import PageLayout from '@/components/PageLayout.vue'
import { onMounted, ref, watch } from 'vue'
import { useQuasar } from 'quasar'
import { api } from '@/boot/axios'
import { useSchoolStore } from '@/stores/school'
import { useAuthStore } from '@/stores/auth'
import { errorText, loadClasses, saveFile } from '@/utils/modules'

const $q = useQuasar()
const schoolStore = useSchoolStore()
const authStore = useAuthStore()

const classes = ref([])
const classId = ref(null)
const terms = ref([])
const termId = ref(null)
const card = ref(null)
const error = ref('')
const finalizing = ref(false)
const pdfLoading = ref(false)
const cellOpen = ref(false)
const editing = ref(null)

function cellClass(c) {
  if (c.quarterly != null) return `g${c.quarterly}`
  if (c.suggested != null) return `g${c.suggested} pending`
  return ''
}

function subjectName(id) {
  return card.value?.subjects.find(s => s.id === id)?.name || ''
}

async function loadRefs() {
  if (!schoolStore.activeSchoolId) return
  classes.value = await loadClasses(schoolStore.activeSchoolId)
  if (!classId.value && classes.value.length) {
    classId.value = classes.value[0].value
    onClass()
  }
}

async function onClass() {
  card.value = null
  terms.value = []
  termId.value = null
  error.value = ''
  if (!classId.value) return
  try {
    terms.value = (
      await api.get('/api/report-cards/terms', {
        params: { classId: classId.value }
      })
    ).data
  } catch (e) {
    error.value = errorText(e)
  }
  load()
}

async function load() {
  if (!classId.value) return
  error.value = ''
  try {
    card.value = (
      await api.get('/api/report-cards', {
        params: { classId: classId.value, termId: termId.value || undefined }
      })
    ).data
    termId.value = card.value.term.id
  } catch (e) {
    card.value = null
    error.value = errorText(e)
  }
}

function finalize() {
  $q.dialog({
    title: "Chorak baholarini qo'yish",
    message: `${card.value.missing} ta taklif chorak bahosi sifatida jurnalga yoziladi. Avval qo'yilgan baholar o'zgarmaydi.`,
    cancel: { flat: true, label: 'Bekor qilish', noCaps: true },
    ok: { label: "Qo'yish", color: 'primary', unelevated: true, noCaps: true }
  }).onOk(async () => {
    finalizing.value = true
    try {
      const res = await api.post('/api/report-cards/finalize', {
        classId: classId.value,
        termId: termId.value
      })
      $q.notify({
        type: 'positive',
        message: `${res.data.saved} ta chorak bahosi qo'yildi`
      })
      load()
    } catch (e) {
      $q.notify({ type: 'negative', message: errorText(e) })
    } finally {
      finalizing.value = false
    }
  })
}

function openCell(row, cell) {
  editing.value = { row, cell }
  cellOpen.value = true
}

async function setCell(score) {
  try {
    card.value = (
      await api.put('/api/report-cards/cell', {
        classId: classId.value,
        termId: termId.value,
        studentId: editing.value.row.studentId,
        subjectId: editing.value.cell.subjectId,
        score
      })
    ).data
    cellOpen.value = false
  } catch (e) {
    $q.notify({ type: 'negative', message: errorText(e) })
  }
}

async function pdf(row) {
  pdfLoading.value = !row
  try {
    const cls = classes.value.find(c => c.value === classId.value)?.label || ''
    await saveFile(
      '/api/report-cards/pdf',
      {
        classId: classId.value,
        termId: termId.value,
        studentId: row?.studentId
      },
      row ? `tabel-${row.name}.pdf` : `tabel-${cls}.pdf`
    )
  } catch (e) {
    $q.notify({
      type: 'negative',
      message: errorText(e, "PDF tayyorlab bo'lmadi")
    })
  } finally {
    pdfLoading.value = false
  }
}

function send() {
  $q.dialog({
    title: 'Ota-onalarga yuborish',
    message:
      "Har bir o'quvchining tabeli (PDF) Telegram botga ulangan ota-onasiga yuboriladi.",
    cancel: { flat: true, label: 'Bekor qilish', noCaps: true },
    ok: { label: 'Yuborish', color: 'primary', unelevated: true, noCaps: true }
  }).onOk(async () => {
    try {
      const res = await api.post('/api/report-cards/send', {
        classId: classId.value,
        termId: termId.value
      })
      $q.notify({
        type: 'positive',
        message: `${res.data.queued} ta xabar navbatga qo'shildi`
      })
    } catch (e) {
      $q.notify({ type: 'negative', message: errorText(e) })
    }
  })
}

watch(
  () => schoolStore.activeSchoolId,
  () => {
    classId.value = null
    loadRefs()
  }
)

onMounted(loadRefs)
</script>

<style scoped>
.tabel-wrap {
  max-height: calc(100vh - 320px);
}
.tabel {
  border-collapse: separate;
  border-spacing: 0;
  width: 100%;
  font-size: 13px;
}
.tabel th,
.tabel td {
  border-bottom: 1px solid var(--brand-border);
  padding: 6px 8px;
  text-align: center;
  white-space: nowrap;
}
.tabel thead th {
  position: sticky;
  top: 0;
  background: var(--card-bg);
  z-index: 2;
  font-weight: 600;
}
.sticky-col {
  position: sticky;
  left: 0;
  background: var(--card-bg);
  z-index: 1;
  text-align: left !important;
}
thead .sticky-col {
  z-index: 3;
}
.name-cell a {
  color: inherit;
  text-decoration: none;
}
.subject-th {
  min-width: 48px;
}
.subject-name {
  max-width: 90px;
  overflow: hidden;
  text-overflow: ellipsis;
}
.grade-cell {
  cursor: pointer;
  font-weight: 700;
  min-width: 42px;
}
.stored {
  font-weight: 800;
}
.suggested {
  font-weight: 600;
  opacity: 0.55;
  border-bottom: 1px dashed currentColor;
}
.g5 {
  color: #059669;
}
.g4 {
  color: #2563eb;
}
.g3 {
  color: #d97706;
}
.g2,
.g1 {
  color: #dc2626;
}
.pending {
  background: rgba(245, 158, 11, 0.06);
}
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
.score-btn {
  width: 48px;
}
.legend .stored,
.legend .suggested {
  color: inherit;
}
</style>

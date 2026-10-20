<template>
  <div class="yd" data-year-detail>
    <div class="yd__hero">
      <IconTile icon="event" size="48" color="var(--c-primary)" />
      <div>
        <h2>{{ row.title }}</h2>
        <small class="tnum"
          >{{ formatDay(row.startDate) }} — {{ formatDay(row.endDate) }}</small
        >
      </div>
    </div>

    <section>
      <h4>Choraklar va ta'tillar</h4>
      <p class="yd__muted">
        Har biri o'quv yili ichida, bir-biriga tegmasdan. Bosh sahifa va
        jurnaldagi «shu chorak» ko'rsatkichlari shu sanalardan olinadi.
      </p>
      <div v-if="loading" class="skel yd__skel" />
      <div v-else class="yd__terms" data-terms>
        <div
          v-for="(t, k) in terms"
          :key="k"
          class="yd__term"
          :class="`yd__term--${t.kind.toLowerCase()}`"
          :data-term-row="k"
        >
          <AppInput
            v-model="t.name"
            dense
            label="Nomi"
            :readonly="!canEdit"
            class="yd__name"
          />
          <AppSelect
            v-model="t.kind"
            dense
            label="Turi"
            :options="KINDS"
            emit-value
            map-options
            :readonly="!canEdit"
            class="yd__kind"
          />
          <AppDateInput
            v-model="t.startDate"
            label="Boshlanish"
            :readonly="!canEdit"
          />
          <AppDateInput
            v-model="t.endDate"
            label="Tugash"
            :readonly="!canEdit"
          />
          <AppButton
            v-if="canEdit"
            variant="ghost"
            size="sm"
            icon="close"
            aria-label="Olib tashlash"
            @click="terms.splice(k, 1)"
          />
        </div>
        <EmptyState
          v-if="!terms.length"
          icon="date_range"
          title="Choraklar kiritilmagan"
          :text="
            canEdit ? 'Odatiy taqvimni qo\'ying yoki bittalab qo\'shing.' : ''
          "
        />
      </div>
      <div v-if="canEdit" class="yd__add">
        <AppButton
          variant="secondary"
          size="sm"
          icon="add"
          label="Chorak"
          data-add-quarter
          @click="add('QUARTER')"
        />
        <AppButton
          variant="secondary"
          size="sm"
          icon="add"
          label="Ta'til"
          @click="add('VACATION')"
        />
        <AppButton
          variant="ghost"
          size="sm"
          icon="auto_fix_high"
          label="Odatiy taqvim"
          data-usual-terms
          @click="usual"
        />
      </div>
      <div v-if="error" class="yd__error" data-terms-error>{{ error }}</div>
      <div v-if="canEdit" class="yd__save">
        <AppButton
          icon="save"
          label="Choraklarni saqlash"
          :loading="saving"
          data-save-terms
          @click="save"
        />
      </div>
    </section>
  </div>
</template>

<script setup>
// One school year in the side panel: its quarters and vacations, edited as a list and saved at
// once (PUT /api/academic-years/{id}/terms — 409 when one leaves the year or two overlap, nothing
// saved then). "Odatiy taqvim" fills Uzbekistan's usual four quarters and three vacations.
import { ref, watch } from 'vue'
import { useQuasar } from 'quasar'
import { api } from '@/boot/axios'
import AppButton from '@/components/app/AppButton.vue'
import AppInput from '@/components/app/AppInput.vue'
import AppSelect from '@/components/app/AppSelect.vue'
import AppDateInput from '@/components/app/AppDateInput.vue'
import IconTile from '@/components/app/IconTile.vue'
import EmptyState from '@/components/pages/EmptyState.vue'
import { formatDay } from '../crud'

const props = defineProps({
  row: { type: Object, required: true },
  canEdit: { type: Boolean, default: false }
})
const emit = defineEmits(['reload'])
const $q = useQuasar()

const KINDS = [
  { value: 'QUARTER', label: 'Chorak' },
  { value: 'VACATION', label: "Ta'til" }
]
const terms = ref([])
const loading = ref(false)
const saving = ref(false)
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    terms.value = (
      await api.get(`/api/academic-years/${props.row.id}/terms`)
    ).data
  } catch {
    terms.value = []
  } finally {
    loading.value = false
  }
}
watch(() => props.row.id, load, { immediate: true })

function add(kind) {
  const last = terms.value.at(-1)
  const start = last?.endDate ? nextDay(last.endDate) : props.row.startDate
  const n = terms.value.filter(t => t.kind === 'QUARTER').length + 1
  terms.value.push({
    name: kind === 'QUARTER' ? `${n}-chorak` : "Ta'til",
    kind,
    startDate: start,
    endDate: start
  })
}

function nextDay(iso) {
  const d = new Date(`${iso}T00:00:00Z`)
  d.setUTCDate(d.getUTCDate() + 1)
  return d.toISOString().slice(0, 10)
}

// the usual calendar of the year this one starts in
function usual() {
  const y = Number(String(props.row.startDate).slice(0, 4))
  const t = (name, kind, a, b) => ({ name, kind, startDate: a, endDate: b })
  terms.value = [
    t('1-chorak', 'QUARTER', `${y}-09-02`, `${y}-11-02`),
    t("Kuzgi ta'til", 'VACATION', `${y}-11-03`, `${y}-11-09`),
    t('2-chorak', 'QUARTER', `${y}-11-10`, `${y}-12-28`),
    t("Qishki ta'til", 'VACATION', `${y}-12-29`, `${y + 1}-01-11`),
    t('3-chorak', 'QUARTER', `${y + 1}-01-12`, `${y + 1}-03-20`),
    t("Bahorgi ta'til", 'VACATION', `${y + 1}-03-21`, `${y + 1}-03-29`),
    t('4-chorak', 'QUARTER', `${y + 1}-03-30`, `${y + 1}-05-25`)
  ].filter(
    x => x.startDate >= props.row.startDate && x.endDate <= props.row.endDate
  )
}

async function save() {
  saving.value = true
  error.value = ''
  try {
    terms.value = (
      await api.put(`/api/academic-years/${props.row.id}/terms`, terms.value)
    ).data
    $q.notify({
      type: 'positive',
      message: 'Choraklar saqlandi',
      icon: 'check_circle'
    })
    emit('reload')
  } catch (e) {
    error.value = e.response?.data || "Saqlab bo'lmadi"
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.tnum {
  font-variant-numeric: tabular-nums;
}

.yd {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}

.yd__hero {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.yd__hero h2 {
  margin: 0;
  font-size: var(--text-lg);
  font-weight: 800;
}

.yd__hero small,
.yd__muted {
  color: var(--brand-text-muted);
}

h4 {
  margin: 0 0 var(--space-1);
  font-size: var(--text-xs);
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--brand-text-muted);
}

.yd__muted {
  margin: 0 0 var(--space-3);
  font-size: var(--text-sm);
}

.yd__skel {
  height: calc(var(--crud-row-h) * 4);
  border-radius: var(--radius-md);
}

.yd__terms {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.yd__term {
  display: grid;
  grid-template-columns:
    minmax(0, 1.4fr) minmax(0, 1fr) minmax(0, 1.1fr)
    minmax(0, 1.1fr) auto;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2);
  border-left: var(--row-stripe) solid var(--c-primary);
  border-radius: var(--radius-md);
  background: var(--surface-2);
}

.yd__term--vacation {
  border-left-color: var(--st-late);
}

.yd__add,
.yd__save {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  margin-top: var(--space-3);
}

.yd__save {
  justify-content: flex-end;
}

.yd__error {
  margin-top: var(--space-2);
  color: var(--st-absent);
  font-size: var(--text-sm);
}

@media (max-width: 599px) {
  .yd__term {
    grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  }
}
</style>

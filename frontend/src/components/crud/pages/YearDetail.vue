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
      <!-- the year as a bar: drag a term's edge (or focus it and use ← →) to move its date -->
      <div
        v-else-if="terms.length"
        ref="barEl"
        class="yd__bar"
        :class="{ 'yd__bar--dragging': drag }"
        data-terms-bar
      >
        <div
          v-for="(t, k) in terms"
          :key="k"
          class="yd__seg"
          :class="`yd__seg--${t.kind.toLowerCase()}`"
          :style="segStyle(t)"
          :data-seg="k"
        >
          <span class="yd__seg-name">{{ t.name }}</span>
          <template v-if="canEdit && t.startDate && t.endDate">
            <button
              type="button"
              class="yd__handle yd__handle--start"
              :aria-label="`${t.name}: boshlanishi ${formatDay(t.startDate)} (← → bir kun)`"
              data-handle="start"
              @pointerdown.prevent="startDrag($event, k, 'startDate')"
              @keydown="nudge($event, k, 'startDate')"
            />
            <button
              type="button"
              class="yd__handle yd__handle--end"
              :aria-label="`${t.name}: tugashi ${formatDay(t.endDate)} (← → bir kun)`"
              data-handle="end"
              @pointerdown.prevent="startDrag($event, k, 'endDate')"
              @keydown="nudge($event, k, 'endDate')"
            />
          </template>
          <q-tooltip
            >{{ t.name }}: {{ formatDay(t.startDate) }} —
            {{ formatDay(t.endDate) }}</q-tooltip
          >
        </div>
      </div>
      <div v-if="terms.length && !loading" class="yd__bar-ends tnum">
        <span>{{ formatDay(row.startDate) }}</span>
        <span v-if="drag" class="yd__drag-note" data-drag-note
          >{{ terms[drag.k].name }}:
          {{ formatDay(terms[drag.k][drag.edge]) }}</span
        >
        <span>{{ formatDay(row.endDate) }}</span>
      </div>
      <div v-if="!loading" class="yd__terms" data-terms>
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
            class="yd__start"
          />
          <AppDateInput
            v-model="t.endDate"
            label="Tugash"
            :readonly="!canEdit"
            class="yd__end"
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
      <!-- nothing to change until the year's terms are in (a late answer would wipe the edits) -->
      <div v-if="canEdit" class="yd__add">
        <AppButton
          variant="secondary"
          size="sm"
          icon="add"
          label="Chorak"
          data-add-quarter
          :disable="loading"
          @click="add('QUARTER')"
        />
        <AppButton
          variant="secondary"
          size="sm"
          icon="add"
          label="Ta'til"
          :disable="loading"
          @click="add('VACATION')"
        />
        <AppButton
          variant="ghost"
          size="sm"
          icon="auto_fix_high"
          label="Odatiy taqvim"
          data-usual-terms
          :disable="loading"
          @click="usual"
        />
      </div>
      <!-- the server's rules checked as you type: saving waits until the list is right -->
      <ul
        v-if="canEdit && problems.length"
        class="yd__problems"
        data-terms-problems
      >
        <li v-for="p in problems" :key="p">
          <q-icon name="error_outline" /> {{ p }}
        </li>
      </ul>
      <div v-if="error" class="yd__error" data-terms-error>{{ error }}</div>
      <div v-if="canEdit" class="yd__save">
        <AppButton
          icon="save"
          label="Choraklarni saqlash"
          :loading="saving"
          data-save-terms
          :disable="loading || problems.length > 0"
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
import { computed, ref, watch } from 'vue'
import { useQuasar } from 'quasar'
import { api } from '@/boot/axios'
import AppButton from '@/components/app/AppButton.vue'
import AppInput from '@/components/app/AppInput.vue'
import AppSelect from '@/components/app/AppSelect.vue'
import AppDateInput from '@/components/app/AppDateInput.vue'
import IconTile from '@/components/app/IconTile.vue'
import EmptyState from '@/components/pages/EmptyState.vue'
import { formatDay } from '../crud'
import {
  along,
  daysBetween,
  isoPlus,
  moveEdge,
  termProblems,
  quarterIndex,
  QUARTER_COLORS
} from './years'

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
const problems = computed(() => termProblems(terms.value, props.row))

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

// ---- dragging an edge on the bar
const barEl = ref(null)
const drag = ref(null)

function segStyle(t) {
  if (!t.startDate || !t.endDate) return { display: 'none' }
  const left = along(props.row.startDate, props.row.endDate, t.startDate)
  const right = along(props.row.startDate, props.row.endDate, t.endDate)
  const q = quarterIndex(terms.value)[terms.value.indexOf(t)]
  return {
    left: `${left}%`,
    width: `${Math.max(right - left, 0.8)}%`,
    '--q': q == null ? null : QUARTER_COLORS[q % QUARTER_COLORS.length]
  }
}

/** The date under the pointer: the bar spans the year, one day per step. */
function dateAt(clientX) {
  const box = barEl.value.getBoundingClientRect()
  const f = Math.max(0, Math.min(1, (clientX - box.left) / box.width))
  return isoPlus(
    props.row.startDate,
    Math.round(f * daysBetween(props.row.startDate, props.row.endDate))
  )
}

function setEdge(k, edge, iso) {
  terms.value[k][edge] = moveEdge(terms.value, k, edge, iso, props.row)
}

function startDrag(e, k, edge) {
  drag.value = { k, edge }
  const move = ev => setEdge(k, edge, dateAt(ev.clientX))
  const up = () => {
    drag.value = null
    window.removeEventListener('pointermove', move)
    window.removeEventListener('pointerup', up)
  }
  window.addEventListener('pointermove', move)
  window.addEventListener('pointerup', up)
}

function nudge(e, k, edge) {
  const step = { ArrowLeft: -1, ArrowRight: 1 }[e.key]
  if (!step) return
  e.preventDefault()
  setEdge(k, edge, isoPlus(terms.value[k][edge], e.shiftKey ? step * 7 : step))
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

.yd__bar {
  position: relative;
  height: var(--space-8);
  margin-bottom: var(--space-1);
  border-radius: var(--radius-md);
  background: var(--ring-track);
  touch-action: none;
  user-select: none;
}

.yd__bar--dragging {
  cursor: ew-resize;
}

.yd__seg {
  position: absolute;
  top: var(--space-1);
  bottom: var(--space-1);
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-sm);
  font-size: var(--text-xs);
  font-weight: 700;
  white-space: nowrap;
}

.yd__seg--quarter {
  background: var(--q, var(--c-primary));
  color: white;
}

.yd__seg--vacation {
  background: repeating-linear-gradient(
    135deg,
    color-mix(in srgb, var(--st-late) 45%, transparent) 0 var(--space-1),
    color-mix(in srgb, var(--st-late) 25%, transparent) var(--space-1)
      var(--space-2)
  );
  color: var(--text-primary);
}

.yd__seg-name {
  overflow: hidden;
  text-overflow: ellipsis;
  padding: 0 var(--space-2);
}

.yd__handle {
  position: absolute;
  top: 0;
  bottom: 0;
  width: var(--space-2);
  padding: 0;
  border: 0;
  border-radius: var(--radius-sm);
  background: color-mix(in srgb, var(--text-primary) 35%, transparent);
  cursor: ew-resize;
  opacity: 0;
  transition: opacity var(--dur-fast) var(--ease-out);
}

.yd__handle--start {
  left: 0;
}

.yd__handle--end {
  right: 0;
}

.yd__seg:hover .yd__handle,
.yd__handle:focus-visible,
.yd__bar--dragging .yd__handle {
  opacity: 1;
}

.yd__handle:focus-visible {
  outline: var(--border-focus);
}

@media (hover: none) {
  .yd__handle {
    opacity: 1;
  }
}

.yd__bar-ends {
  display: flex;
  justify-content: space-between;
  margin-bottom: var(--space-3);
  color: var(--brand-text-muted);
  font-size: var(--text-xs);
}

.yd__drag-note {
  color: var(--c-primary);
  font-weight: 700;
}

.yd__terms {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.yd__term {
  /* two floors: name and kind, then the two dates — every field wide enough to read */
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr) auto;
  grid-template-areas:
    'name kind del'
    'start end del';
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2);
  border-left: var(--row-stripe) solid var(--c-primary);
  border-radius: var(--radius-md);
  background: var(--surface-2);
}

.yd__name {
  grid-area: name;
}

.yd__kind {
  grid-area: kind;
}

.yd__start {
  grid-area: start;
}

.yd__end {
  grid-area: end;
}

.yd__term > .app-btn,
.yd__term > :deep(.q-btn) {
  grid-area: del;
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

.yd__problems {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  margin: var(--space-2) 0 0;
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-sm);
  background: color-mix(in srgb, var(--st-absent) 8%, transparent);
  color: var(--st-absent);
  font-size: var(--text-sm);
  list-style: none;
}

.yd__error {
  margin-top: var(--space-2);
  color: var(--st-absent);
  font-size: var(--text-sm);
}
</style>

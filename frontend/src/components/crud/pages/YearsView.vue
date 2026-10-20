<template>
  <div class="yv" data-years>
    <div v-if="canEdit" class="yv__bar">
      <AppButton
        variant="secondary"
        icon="event_repeat"
        label="Keyingi yilni yaratish"
        data-next-year
        @click="openNext"
      />
    </div>

    <div v-if="loading && !cards.length" class="yv__list">
      <div v-for="i in 2" :key="i" class="skel yv__skel" />
    </div>
    <EmptyState
      v-else-if="!cards.length"
      icon="event"
      title="O'quv yili hali yo'q"
      text="Birinchi yilni qo'shing yoki odatiy taqvim bo'yicha yarating."
      :action="canEdit ? 'Yaratish' : ''"
      action-icon="event_repeat"
      @action="openNext"
    />

    <div v-else class="yv__list">
      <article
        v-for="(y, i) in cards"
        :key="y.id"
        class="yv__card dash-card"
        :class="`yv__card--${y.status.toLowerCase()}`"
        :style="{ '--i': i, '--tone': STATUS[y.status].tone }"
        :data-year-id="y.id"
        :data-status="y.status"
      >
        <header class="yv__head">
          <div class="yv__titles">
            <h3 class="yv__title">{{ y.title }}</h3>
            <span class="yv__dates tnum"
              >{{ formatDay(y.startDate) }} — {{ formatDay(y.endDate) }}</span
            >
          </div>
          <span class="yv__badge" data-year-status>
            <i class="yv__badge-dot" />{{ STATUS[y.status].label }}
          </span>
          <q-space />
          <AppButton
            v-if="canEdit && !y.current"
            variant="ghost"
            size="sm"
            icon="star_outline"
            label="Joriy yil qilib belgilash"
            data-make-current
            @click="makeCurrent(y)"
          />
          <AppButton
            variant="ghost"
            size="sm"
            icon="date_range"
            :label="canEdit ? 'Choraklar va ta\'tillar' : 'Choraklar'"
            data-open-terms
            @click="emit('open', y)"
          />
          <AppButton
            v-if="canEdit"
            variant="ghost"
            size="sm"
            icon="edit"
            aria-label="Tahrirlash"
            data-action="edit"
            @click="emit('edit', y)"
          />
          <AppButton
            v-if="canDelete"
            variant="ghost"
            size="sm"
            icon="delete_outline"
            aria-label="O'chirish"
            data-action="delete"
            @click="emit('delete', y)"
          />
        </header>

        <!-- the year as a bar: quarters and vacations by their dates, the part gone, today -->
        <div class="yv__track" role="img" :aria-label="progressLine(y, today)">
          <div
            v-if="y.status !== 'PLANNED'"
            class="yv__passed"
            :style="{ width: `${along(y.startDate, y.endDate, today)}%` }"
          />
          <div
            v-for="(t, k) in y.terms"
            :key="t.id || k"
            class="yv__seg"
            :class="`yv__seg--${t.kind.toLowerCase()}`"
            :style="{
              left: `${segment(y, t).left}%`,
              width: `${segment(y, t).width}%`,
              '--k': k
            }"
            :data-term="t.name"
          >
            <span class="yv__seg-name">{{ t.name }}</span>
            <q-tooltip
              >{{ t.name }}: {{ formatDay(t.startDate) }} —
              {{ formatDay(t.endDate) }}</q-tooltip
            >
          </div>
          <div
            v-if="today >= y.startDate && today <= y.endDate"
            class="yv__today"
            :style="{ left: `${along(y.startDate, y.endDate, today)}%` }"
            data-today-marker
          >
            <span>Bugun</span>
          </div>
        </div>
        <div class="yv__line">
          <span data-year-progress>{{ progressLine(y, today) }}</span>
          <span v-if="termOn(y.terms, today)" class="yv__now"
            >Hozir: <b>{{ termOn(y.terms, today).name }}</b></span
          >
          <span v-else-if="!y.terms.length" class="yv__now yv__muted"
            >Choraklar kiritilmagan</span
          >
        </div>

        <div class="yv__stats">
          <div class="yv__stat">
            <b class="tnum">{{ y.workDays }}</b
            ><small>ish kuni</small>
          </div>
          <div class="yv__stat">
            <b class="tnum">{{ y.students }}</b
            ><small>o'quvchi · {{ y.classes }} sinf</small>
          </div>
          <div class="yv__stat">
            <b class="tnum" :style="{ color: pctTone(y.attendance, 95, 85) }">{{
              pct(y.attendance)
            }}</b
            ><small
              >davomat{{ y.status === 'FINISHED' ? ' (yakuniy)' : '' }}</small
            >
          </div>
          <div class="yv__stat">
            <b class="tnum" :style="{ color: pctTone(y.quality, 60, 40) }">{{
              pct(y.quality)
            }}</b
            ><small
              >sifat{{ y.status === 'FINISHED' ? ' (yakuniy)' : '' }}</small
            >
          </div>
        </div>
      </article>
    </div>

    <!-- "Keyingi yilni yaratish": the proposal, editable, then created with its terms -->
    <q-dialog v-model="nextOpen">
      <div class="yv__dialog dash-card" data-next-dialog>
        <h3>Keyingi o'quv yili</h3>
        <p class="yv__muted">
          Sanalar va choraklar oxirgi yildan bir yil keyinga surildi (yil
          bo'lmasa — odatiy taqvim). O'zgartirish mumkin.
        </p>
        <template v-if="next">
          <AppInput v-model="next.title" label="Sarlavha" data-next-title />
          <div class="yv__dates-row">
            <AppDateInput v-model="next.startDate" label="Boshlanish" />
            <AppDateInput v-model="next.endDate" label="Tugash" />
          </div>
          <ul class="yv__terms">
            <li v-for="(t, k) in next.terms" :key="k">
              <i :class="`yv__dot yv__dot--${t.kind.toLowerCase()}`" />
              <b>{{ t.name }}</b>
              <span class="tnum"
                >{{ formatDay(t.startDate) }} — {{ formatDay(t.endDate) }}</span
              >
            </li>
          </ul>
          <div v-if="nextError" class="yv__error">{{ nextError }}</div>
        </template>
        <div class="yv__dialog-foot">
          <AppButton variant="ghost" label="Bekor qilish" v-close-popup />
          <AppButton
            icon="check"
            label="Yaratish"
            :loading="creating"
            data-next-create
            @click="createNext"
          />
        </div>
      </div>
    </q-dialog>
  </div>
</template>

<script setup>
// O'quv yillari as cards, the newest first: the year's status (the current one glows), its dates,
// a bar of its quarters and vacations by date with the part already gone and a "Bugun" marker,
// "62% o'tdi · 74 ish kuni qoldi", and its numbers (school days, students, attendance, quality).
// "Joriy yil qilib belgilash" (one per school), the terms in the side panel, and "Keyingi yilni
// yaratish" from the last year's dates one year on.
import { ref, onMounted, watch } from 'vue'
import { useQuasar } from 'quasar'
import { api } from '@/boot/axios'
import AppButton from '@/components/app/AppButton.vue'
import AppInput from '@/components/app/AppInput.vue'
import AppDateInput from '@/components/app/AppDateInput.vue'
import EmptyState from '@/components/pages/EmptyState.vue'
import { formatDay } from '../crud'
import { along, segment, STATUS, progressLine, termOn } from './years'

const props = defineProps({
  schoolId: { type: Number, default: null },
  canEdit: { type: Boolean, default: false },
  canDelete: { type: Boolean, default: false },
  today: { type: String, required: true }
})
const emit = defineEmits(['open', 'edit', 'delete', 'create'])
const $q = useQuasar()

const cards = ref([])
const loading = ref(false)

async function reload() {
  if (!props.schoolId) return
  loading.value = true
  try {
    cards.value = (
      await api.get('/api/pages/lists/years', {
        params: { schoolId: props.schoolId }
      })
    ).data
  } catch (e) {
    $q.notify({
      type: 'negative',
      message: e.response?.data || "Yuklab bo'lmadi"
    })
  } finally {
    loading.value = false
  }
}
onMounted(reload)
watch(() => props.schoolId, reload)
defineExpose({ reload })

const pct = v => (v == null ? '—' : `${Math.round(v)}%`)
const pctTone = (v, good, warn) =>
  v == null
    ? 'var(--brand-text-muted)'
    : v >= good
      ? 'var(--st-present)'
      : v >= warn
        ? 'var(--gr-3)'
        : 'var(--st-absent)'

async function makeCurrent(y) {
  try {
    await api.post(`/api/academic-years/${y.id}/current`)
    $q.notify({ type: 'positive', message: `«${y.title}» — joriy yil` })
    reload()
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data || 'Xatolik' })
  }
}

const nextOpen = ref(false)
const next = ref(null)
const nextError = ref('')
const creating = ref(false)

async function openNext() {
  nextError.value = ''
  next.value = null
  nextOpen.value = true
  try {
    next.value = (
      await api.get('/api/academic-years/next', {
        params: { schoolId: props.schoolId }
      })
    ).data
  } catch (e) {
    nextError.value = e.response?.data || "Taklifni olib bo'lmadi"
  }
}

async function createNext() {
  creating.value = true
  nextError.value = ''
  try {
    await api.post('/api/academic-years/next', next.value, {
      params: { schoolId: props.schoolId }
    })
    nextOpen.value = false
    $q.notify({ type: 'positive', message: `«${next.value.title}» yaratildi` })
    reload()
  } catch (e) {
    nextError.value = e.response?.data || "Yaratib bo'lmadi"
  } finally {
    creating.value = false
  }
}
</script>

<style scoped>
.tnum {
  font-variant-numeric: tabular-nums;
}

.yv {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.yv__bar {
  display: flex;
  justify-content: flex-end;
}

.yv__list {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.yv__skel {
  height: calc(var(--crud-row-h) * 4);
  border-radius: var(--radius-lg);
}

.yv__card {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  padding: var(--space-5);
}

.yv__card:hover {
  transform: none;
}

/* the current year glows in the brand colour */
.yv__card--current {
  border-color: color-mix(in srgb, var(--c-primary) 45%, transparent);
  box-shadow:
    var(--dash-shadow),
    0 0 0 var(--hairline) color-mix(in srgb, var(--c-primary) 30%, transparent),
    0 var(--space-3) var(--space-7) calc(var(--space-3) * -1)
      color-mix(in srgb, var(--c-primary) 40%, transparent);
}

.yv__card--finished {
  opacity: 0.88;
}

.yv__head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-2) var(--space-3);
}

.yv__titles {
  display: flex;
  flex-direction: column;
}

.yv__title {
  margin: 0;
  font-size: var(--text-lg);
  font-weight: 800;
  letter-spacing: -0.01em;
}

.yv__dates {
  color: var(--brand-text-muted);
  font-size: var(--text-sm);
}

.yv__badge {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  padding: var(--hairline) var(--space-2);
  border-radius: var(--radius-pill);
  background: color-mix(in srgb, var(--tone) 14%, transparent);
  color: var(--tone);
  font-size: var(--text-xs);
  font-weight: 700;
}

.yv__badge-dot {
  width: var(--space-2);
  height: var(--space-2);
  border-radius: var(--radius-pill);
  background: var(--tone);
}

.yv__card--current .yv__badge-dot {
  box-shadow: 0 0 0 var(--space-1)
    color-mix(in srgb, var(--tone) 25%, transparent);
}

.yv__track {
  position: relative;
  height: var(--space-7);
  margin-top: var(--space-4);
  border-radius: var(--radius-md);
  background: var(--ring-track);
}

.yv__passed {
  position: absolute;
  inset: 0 auto 0 0;
  border-radius: var(--radius-md) 0 0 var(--radius-md);
  background: color-mix(in srgb, var(--c-primary) 10%, transparent);
}

.yv__seg {
  position: absolute;
  top: var(--space-1);
  bottom: var(--space-1);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border-radius: var(--radius-sm);
  font-size: var(--text-xs);
  font-weight: 700;
  white-space: nowrap;
}

.yv__seg--quarter {
  background: color-mix(
    in srgb,
    var(--c-primary) calc(70% - var(--k) * 6%),
    var(--card-bg)
  );
  color: white;
}

.yv__seg--vacation {
  background: repeating-linear-gradient(
    135deg,
    color-mix(in srgb, var(--st-late) 45%, transparent) 0 var(--space-1),
    color-mix(in srgb, var(--st-late) 25%, transparent) var(--space-1)
      var(--space-2)
  );
  color: var(--text-primary);
}

.yv__seg-name {
  overflow: hidden;
  text-overflow: ellipsis;
  padding: 0 var(--space-1);
}

.yv__today {
  position: absolute;
  top: calc(var(--space-4) * -1);
  bottom: calc(var(--space-1) * -1);
  width: 2px;
  margin-left: -1px;
  background: var(--st-absent);
  pointer-events: none;
}

.yv__today span {
  position: absolute;
  top: 0;
  left: 50%;
  transform: translate(-50%, -60%);
  padding: 0 var(--space-1);
  border-radius: var(--radius-sm);
  background: var(--st-absent);
  color: white;
  font-size: var(--text-xs);
  font-weight: 700;
  white-space: nowrap;
}

.yv__line {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: var(--space-2);
  font-size: var(--text-sm);
  font-weight: 600;
}

.yv__now {
  color: var(--brand-text-muted);
  font-weight: 500;
}

.yv__muted {
  color: var(--brand-text-muted);
}

.yv__stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--space-3);
  padding-top: var(--space-3);
  border-top: var(--hairline) solid var(--dash-edge);
}

.yv__stat {
  display: flex;
  flex-direction: column;
}

.yv__stat b {
  font-size: var(--text-lg);
  font-weight: 800;
}

.yv__stat small {
  color: var(--brand-text-muted);
}

.yv__dialog {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  width: min(var(--side-panel-w-wide), 92vw);
  padding: var(--space-5);
}

.yv__dialog h3 {
  margin: 0;
  font-size: var(--text-lg);
}

.yv__dialog p {
  margin: 0;
  font-size: var(--text-sm);
}

.yv__dates-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-3);
}

.yv__terms {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  margin: 0;
  padding: 0;
  list-style: none;
  font-size: var(--text-sm);
}

.yv__terms li {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.yv__terms li span {
  margin-left: auto;
  color: var(--brand-text-muted);
}

.yv__dot {
  width: var(--space-2);
  height: var(--space-2);
  border-radius: var(--radius-pill);
}

.yv__dot--quarter {
  background: var(--c-primary);
}

.yv__dot--vacation {
  background: var(--st-late);
}

.yv__error {
  color: var(--st-absent);
  font-size: var(--text-sm);
}

.yv__dialog-foot {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-2);
}

@media (max-width: 599px) {
  .yv__stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .yv__seg-name {
    display: none;
  }
}
</style>

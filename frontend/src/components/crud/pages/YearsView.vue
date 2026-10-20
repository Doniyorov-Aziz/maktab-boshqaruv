<template>
  <div class="yv" data-years>
    <!-- no year holds today: say so and offer that year, its dates and terms filled in -->
    <section
      v-if="missing"
      class="yv__missing dash-card"
      role="alert"
      data-missing-year
    >
      <span class="yv__missing-icon"><q-icon name="event_busy" /></span>
      <div class="yv__missing-text">
        <b>Joriy o'quv yili kiritilmagan</b>
        <span
          >Bugun ({{ formatDay(today) }}) hech qaysi o'quv yiliga tushmaydi —
          davomat va baholar yilga bog'lanmaydi.</span
        >
      </div>
      <AppButton
        v-if="canEdit"
        icon="add"
        :label="`${missing.title} ni yaratish`"
        data-create-missing
        @click="openNext(missing)"
      />
    </section>

    <div v-if="loading && !cards.length" class="yv__list">
      <div class="skel yv__skel yv__skel--hero" />
      <div v-for="i in 2" :key="i" class="skel yv__skel" />
    </div>
    <EmptyState
      v-else-if="!cards.length"
      icon="event"
      title="O'quv yili hali yo'q"
      text="Birinchi yilni qo'shing yoki odatiy taqvim bo'yicha yarating."
      :action="canEdit ? 'Yaratish' : ''"
      action-icon="event_repeat"
      @action="openNext()"
    />

    <template v-else>
      <!-- the current year: a full-width hero -->
      <article
        v-for="y in current"
        :key="y.id"
        class="yh dash-card"
        :data-year-id="y.id"
        data-status="CURRENT"
        data-year-hero
      >
        <header class="yh__head">
          <IconTile icon="event_available" size="48" />
          <div class="yh__titles">
            <h2 class="yh__title" data-year-title>{{ y.title }}</h2>
            <span class="yh__dates tnum"
              >{{ formatDay(y.startDate) }} — {{ formatDay(y.endDate) }}</span
            >
          </div>
          <span class="yh__badge" data-year-status
            ><i />{{ STATUS.CURRENT.label }}</span
          >
          <q-space />
          <AppButton
            icon="date_range"
            :label="canEdit ? 'Choraklar va ta\'tillar' : 'Choraklar'"
            data-open-terms
            @click="emit('open', y)"
          />
          <AppButton
            v-if="menuFor(y).length"
            variant="ghost"
            icon="more_horiz"
            aria-label="Boshqa amallar"
            data-year-menu
          >
            <YearMenu :items="menuFor(y)" @pick="k => act(k, y)" />
          </AppButton>
        </header>

        <!-- 40px bar: quarters in their colours, vacations hatched, the part gone dark, today -->
        <div
          class="yh__track"
          role="img"
          :aria-label="termLine(y.terms, today) || progressLine(y, today)"
          data-year-bar
        >
          <div
            v-for="(t, k) in y.terms"
            :key="t.id || k"
            class="yh__seg"
            :class="`yh__seg--${t.kind.toLowerCase()}`"
            :style="{
              left: `${segment(y, t).left}%`,
              width: `${segment(y, t).width}%`,
              '--q': quarterColor(y, k)
            }"
            :data-term="t.name"
          >
            <span class="yh__seg-name">{{ t.name }}</span>
            <q-tooltip
              >{{ t.name }}: {{ formatDay(t.startDate) }} —
              {{ formatDay(t.endDate) }}</q-tooltip
            >
          </div>
          <div
            class="yh__future"
            :style="{ left: `${along(y.startDate, y.endDate, today)}%` }"
          />
          <div
            class="yh__today"
            :style="{ left: `${along(y.startDate, y.endDate, today)}%` }"
            data-today-marker
          >
            <span>Bugun</span>
          </div>
        </div>
        <div class="yh__line">
          <span v-if="termLine(y.terms, today)" data-term-line>
            <q-icon name="flag" /> {{ termLine(y.terms, today) }}
          </span>
          <span v-else class="yv__muted">Choraklar kiritilmagan</span>
          <span class="tnum" data-year-progress
            >Yil: <b>{{ y.dayProgress }}%</b> o'tdi</span
          >
        </div>

        <div class="yh__kpis">
          <div class="yh__kpi" data-kpi="days">
            <small>Ish kunlari</small>
            <b class="dash-num"
              >{{ y.passedDays }}<em> / {{ y.workDays }}</em></b
            >
            <span class="yh__bar"
              ><i :style="{ width: `${y.progress ?? 0}%` }"
            /></span>
            <small class="yh__note">{{ y.leftDays }} ish kuni qoldi</small>
          </div>
          <div class="yh__kpi" data-kpi="people">
            <small>O'quvchilar va sinflar</small>
            <b class="dash-num">{{ formatNumber(y.students) }}</b>
            <small class="yh__note">{{ y.classes }} ta sinf</small>
          </div>
          <template v-if="y.hasData">
            <div class="yh__kpi" data-kpi="attendance">
              <small>Davomat</small>
              <b
                class="dash-num"
                :style="{ color: pctTone(y.attendance, 95, 85) }"
                data-year-attendance
                >{{ formatPercent(y.attendance) }}</b
              >
              <Sparkline
                :values="y.attendanceRun"
                :min="sparkRange(y.attendanceRun).min"
                :max="sparkRange(y.attendanceRun).max"
                color="var(--st-present)"
                label="Davomat haftalar bo'yicha"
              />
            </div>
            <div class="yh__kpi" data-kpi="quality">
              <small>Sifat (4–5)</small>
              <b
                class="dash-num"
                :style="{ color: pctTone(y.quality, 60, 40) }"
                data-year-quality
                >{{ formatPercent(y.quality) }}</b
              >
              <Sparkline
                :values="y.qualityRun"
                :min="sparkRange(y.qualityRun).min"
                :max="sparkRange(y.qualityRun).max"
                color="var(--gr-5)"
                label="Sifat haftalar bo'yicha"
              />
            </div>
            <div class="yh__kpi" data-kpi="mastery">
              <small>O'zlashtirish (3–5)</small>
              <b
                class="dash-num"
                :style="{ color: pctTone(y.mastery, 85, 70) }"
                >{{ formatPercent(y.mastery) }}</b
              >
              <span class="yh__bar yh__bar--ok"
                ><i :style="{ width: `${y.mastery ?? 0}%` }"
              /></span>
            </div>
          </template>
          <div v-else class="yh__kpi yh__kpi--none" data-no-data>
            <q-icon name="insights" />
            Bu yil bo'yicha ma'lumot yo'q
          </div>
        </div>
      </article>

      <!-- planned years: how many days until they start -->
      <div v-if="planned.length" class="yv__planned">
        <article
          v-for="y in planned"
          :key="y.id"
          class="yp dash-card"
          :data-year-id="y.id"
          :data-status="y.status"
          tabindex="0"
          @click="emit('open', y)"
          @keydown.enter.self="emit('open', y)"
        >
          <span class="yp__count">
            <b class="dash-num">{{ y.daysToStart }}</b>
            <small>kun</small>
          </span>
          <div class="yp__text">
            <b>{{ y.title }}</b>
            <span>Boshlanishiga {{ y.daysToStart }} kun</span>
            <small class="tnum"
              >{{ formatDay(y.startDate) }} — {{ formatDay(y.endDate) }} ·
              {{
                y.terms.length
                  ? `${y.terms.filter(t => t.kind === 'QUARTER').length} chorak`
                  : 'choraklar kiritilmagan'
              }}</small
            >
          </div>
          <span class="yv__badge" :style="{ '--tone': STATUS.PLANNED.tone }"
            ><i />{{ STATUS.PLANNED.label }}</span
          >
          <AppButton
            v-if="menuFor(y).length"
            variant="ghost"
            size="sm"
            icon="more_horiz"
            aria-label="Boshqa amallar"
            data-year-menu
            @click.stop
          >
            <YearMenu :items="menuFor(y)" @pick="k => act(k, y)" />
          </AppButton>
        </article>
      </div>

      <!-- finished years (and any other): compact rows, a click opens the panel -->
      <section v-if="past.length" class="yv__past dash-card" data-past-years>
        <h3 class="yv__past-title">O'tgan o'quv yillari</h3>
        <div
          v-for="y in past"
          :key="y.id"
          class="yr"
          :data-year-id="y.id"
          :data-status="y.status"
          tabindex="0"
          role="button"
          @click="emit('open', y)"
          @keydown.enter.self="emit('open', y)"
        >
          <span class="yr__dot" :style="{ '--tone': STATUS[y.status].tone }" />
          <div class="yr__name">
            <b>{{ y.title }}</b>
            <small class="tnum"
              >{{ formatDay(y.startDate) }} — {{ formatDay(y.endDate) }}</small
            >
          </div>
          <template v-if="y.hasData">
            <span class="yr__num" data-year-attendance>
              <b
                class="tnum"
                :style="{ color: pctTone(y.attendance, 95, 85) }"
                >{{ formatPercent(y.attendance) }}</b
              >
              <small>davomat</small>
            </span>
            <span class="yr__num">
              <b class="tnum" :style="{ color: pctTone(y.quality, 60, 40) }">{{
                formatPercent(y.quality)
              }}</b>
              <small>sifat</small>
            </span>
          </template>
          <span v-else class="yr__none">Bu yil bo'yicha ma'lumot yo'q</span>
          <span class="yr__num">
            <b class="tnum">{{
              y.students || y.hasData ? formatNumber(y.students) : '—'
            }}</b>
            <small>o'quvchi</small>
          </span>
          <span class="yv__badge" :style="{ '--tone': STATUS[y.status].tone }"
            ><i />{{ STATUS[y.status].label }}</span
          >
          <AppButton
            v-if="menuFor(y).length"
            variant="ghost"
            size="sm"
            icon="more_horiz"
            aria-label="Boshqa amallar"
            data-year-menu
            @click.stop
          >
            <YearMenu :items="menuFor(y)" @pick="k => act(k, y)" />
          </AppButton>
        </div>
      </section>
    </template>

    <!-- "Keyingi yilni yaratish" / the missing year: the proposal, editable, then created with its terms -->
    <q-dialog v-model="nextOpen">
      <div class="yv__dialog dash-card" data-next-dialog>
        <h3>{{ nextIsMissing ? "Joriy o'quv yili" : "Keyingi o'quv yili" }}</h3>
        <p class="yv__muted">
          {{
            nextIsMissing
              ? "Bugungi sanani qamrab oluvchi yil: odatiy taqvim (2-sentabr — 31-may), choraklar va ta'tillar to'ldirilgan. O'zgartirish mumkin."
              : "Sanalar va choraklar oxirgi yildan bir yil keyinga surildi (yil bo'lmasa — odatiy taqvim). O'zgartirish mumkin."
          }}
        </p>
        <template v-if="next">
          <AppInput v-model="next.title" label="Sarlavha" data-next-title />
          <div class="yv__dates-row">
            <AppDateInput v-model="next.startDate" label="Boshlanish" />
            <AppDateInput v-model="next.endDate" label="Tugash" />
          </div>
          <ul class="yv__terms">
            <li v-for="(t, k) in next.terms" :key="k">
              <i :class="`yv__tdot yv__tdot--${t.kind.toLowerCase()}`" />
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
// O'quv yillari like the home page. The current year (today inside its dates) is a full-width
// hero: name, dates, "Joriy"; a 40px bar of its quarters (each its colour, the name inside) and
// vacations (hatched), the part gone dark and the rest light, a "Bugun" marker; under it
// "1-chorak · 68% o'tdi · kuzgi ta'tilgacha 12 kun" and "Yil: 15% o'tdi"; school days (gone /
// all), students and classes, attendance and quality (weekly runs), mastery — or "Bu yil
// bo'yicha ma'lumot yo'q". A planned year says how many days until it starts; finished ones are
// compact rows (final attendance, quality, students) that open the panel. Every other action is
// in "⋯". With no year of today a card at the top offers to create it.
import { computed, ref, onMounted, watch, h } from 'vue'
import { useQuasar, QList, QItem, QItemSection, QIcon, QMenu } from 'quasar'
import { api } from '@/boot/axios'
import AppButton from '@/components/app/AppButton.vue'
import AppInput from '@/components/app/AppInput.vue'
import AppDateInput from '@/components/app/AppDateInput.vue'
import IconTile from '@/components/app/IconTile.vue'
import EmptyState from '@/components/pages/EmptyState.vue'
import Sparkline from '@/components/dashboard/Sparkline.vue'
import { formatDay } from '../crud'
import { formatNumber, formatPercent, sparkRange } from '@/utils/format'
import {
  along,
  segment,
  STATUS,
  progressLine,
  termLine,
  quarterIndex,
  QUARTER_COLORS
} from './years'

const props = defineProps({
  schoolId: { type: Number, default: null },
  canEdit: { type: Boolean, default: false },
  canDelete: { type: Boolean, default: false },
  today: { type: String, required: true }
})
const emit = defineEmits(['open', 'edit', 'delete', 'create'])
const $q = useQuasar()

const cards = ref([])
const missing = ref(null)
const loading = ref(false)

async function reload() {
  if (!props.schoolId) return
  loading.value = true
  try {
    const data = (
      await api.get('/api/pages/lists/years-page', {
        params: { schoolId: props.schoolId }
      })
    ).data
    cards.value = data.years || []
    missing.value = data.missing || null
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
defineExpose({ reload, openNext: () => openNext() })

const current = computed(() => cards.value.filter(y => y.status === 'CURRENT'))
const planned = computed(() =>
  cards.value
    .filter(y => y.status === 'PLANNED')
    .sort((a, b) => a.startDate.localeCompare(b.startDate))
)
const past = computed(() =>
  cards.value.filter(y => y.status !== 'CURRENT' && y.status !== 'PLANNED')
)

function quarterColor(y, k) {
  const q = quarterIndex(y.terms)[k]
  return q == null ? null : QUARTER_COLORS[q % QUARTER_COLORS.length]
}

const pctTone = (v, good, warn) =>
  v == null
    ? 'var(--brand-text-muted)'
    : v >= good
      ? 'var(--st-present)'
      : v >= warn
        ? 'var(--gr-3)'
        : 'var(--st-absent)'

// ---- the "⋯" menu: everything but the main action
function menuFor(y) {
  const items = []
  if (y.status !== 'CURRENT')
    items.push({
      key: 'terms',
      label: props.canEdit ? "Choraklar va ta'tillar" : 'Choraklar',
      icon: 'date_range'
    })
  if (props.canEdit) {
    items.push({ key: 'edit', label: 'Tahrirlash', icon: 'edit' })
    if (!y.current && y.status !== 'CURRENT')
      items.push({
        key: 'current',
        label: 'Joriy yil qilib belgilash',
        icon: 'star_outline'
      })
  }
  if (props.canDelete)
    items.push({
      key: 'delete',
      label: "O'chirish",
      icon: 'delete_outline',
      danger: true
    })
  return items
}

function act(key, y) {
  if (key === 'terms') emit('open', y)
  else if (key === 'edit') emit('edit', y)
  else if (key === 'delete') emit('delete', y)
  else if (key === 'current') makeCurrent(y)
}

// a small render component: the menu under a "⋯" button
const YearMenu = {
  props: { items: { type: Array, required: true } },
  emits: ['pick'],
  setup(p, { emit: e }) {
    return () =>
      h(
        QMenu,
        { anchor: 'bottom right', self: 'top right', autoClose: true },
        () =>
          h(QList, { dense: true, class: 'yv__menu' }, () =>
            p.items.map(m =>
              h(
                QItem,
                {
                  clickable: true,
                  class: m.danger ? 'yv__menu--danger' : '',
                  'data-menu-item': m.key,
                  onClick: () => e('pick', m.key)
                },
                () => [
                  h(QItemSection, { avatar: true }, () =>
                    h(QIcon, { name: m.icon })
                  ),
                  h(QItemSection, null, () => m.label)
                ]
              )
            )
          )
      )
  }
}

async function makeCurrent(y) {
  try {
    await api.post(`/api/academic-years/${y.id}/current`)
    $q.notify({ type: 'positive', message: `«${y.title}» — joriy yil` })
    reload()
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data || 'Xatolik' })
  }
}

// ---- creating a year: the next one, or the missing year of today
const nextOpen = ref(false)
const next = ref(null)
const nextError = ref('')
const nextIsMissing = ref(false)
const creating = ref(false)

async function openNext(proposal) {
  nextError.value = ''
  next.value = null
  nextIsMissing.value = !!proposal
  nextOpen.value = true
  if (proposal) {
    next.value = JSON.parse(JSON.stringify(proposal))
    return
  }
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

.yv__list {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.yv__skel {
  height: calc(var(--crud-row-h) * 2);
  border-radius: var(--radius-lg);
}

.yv__skel--hero {
  height: calc(var(--crud-row-h) * 6);
}

.yv__muted {
  color: var(--brand-text-muted);
}

/* ---- the missing year */
.yv__missing {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-4) var(--space-5);
  border-color: color-mix(in srgb, var(--st-late) 45%, transparent);
  background: color-mix(in srgb, var(--st-late) 8%, var(--card-bg));
}

.yv__missing-icon {
  display: inline-flex;
  flex: none;
  align-items: center;
  justify-content: center;
  width: var(--tile-lg);
  height: var(--tile-lg);
  border-radius: var(--radius-md);
  background: color-mix(in srgb, var(--st-late) 18%, transparent);
  color: var(--st-late);
  font-size: var(--text-xl);
}

.yv__missing-text {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  gap: var(--hairline);
  min-width: 0;
}

.yv__missing-text b {
  font-size: var(--text-md);
}

.yv__missing-text span {
  color: var(--brand-text-muted);
  font-size: var(--text-sm);
}

/* ---- the current year */
.yh {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  padding: var(--space-5) var(--space-6);
  border-color: color-mix(in srgb, var(--c-primary) 30%, transparent);
  background:
    radial-gradient(
      90% 120% at 100% 0%,
      color-mix(in srgb, var(--c-primary) 10%, transparent),
      transparent 60%
    ),
    var(--card-bg);
}

.yh__head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-3);
}

.yh__titles {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.yh__title {
  margin: 0;
  font-size: var(--text-xl);
  font-weight: 800;
  line-height: 1.2;
}

.yh__dates {
  color: var(--brand-text-muted);
  font-size: var(--text-sm);
}

.yh__badge {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  padding: var(--hairline) var(--space-3);
  border-radius: var(--radius-pill);
  background: color-mix(in srgb, var(--c-primary) 14%, transparent);
  box-shadow: 0 0 var(--space-4)
    color-mix(in srgb, var(--c-primary) 35%, transparent);
  color: var(--c-primary);
  font-size: var(--text-xs);
  font-weight: 800;
}

.yh__badge i,
.yv__badge i {
  width: var(--space-2);
  height: var(--space-2);
  border-radius: 50%;
  background: currentColor;
}

.yh__track {
  position: relative;
  height: 40px;
  margin-top: var(--space-4);
  border-radius: var(--radius-md);
  background: var(--ring-track);
}

.yh__seg {
  position: absolute;
  top: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 0;
  overflow: hidden;
  border-radius: var(--radius-sm);
  font-size: var(--text-xs);
  font-weight: 700;
  white-space: nowrap;
}

.yh__seg--quarter {
  background: var(--q);
  color: white;
}

.yh__seg--vacation {
  background: repeating-linear-gradient(
    135deg,
    color-mix(in srgb, var(--st-late) 50%, transparent) 0 var(--space-1),
    color-mix(in srgb, var(--st-late) 22%, transparent) var(--space-1)
      var(--space-2)
  );
  color: var(--text-primary);
}

.yh__seg-name {
  overflow: hidden;
  padding: 0 var(--space-1);
  text-overflow: ellipsis;
}

/* the part still to come is lighter */
.yh__future {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  border-radius: 0 var(--radius-md) var(--radius-md) 0;
  background: color-mix(in srgb, var(--card-bg) 55%, transparent);
  pointer-events: none;
}

.yh__today {
  position: absolute;
  top: calc(var(--space-4) * -1);
  bottom: calc(var(--space-1) * -1);
  width: 2px;
  margin-left: -1px;
  background: var(--st-absent);
  pointer-events: none;
}

.yh__today span {
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

.yh__line {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: var(--space-2) var(--space-4);
  font-size: var(--text-sm);
}

.yh__line span {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
}

.yh__kpis {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(180px, 100%), 1fr));
  gap: var(--space-3);
  padding-top: var(--space-4);
  border-top: var(--hairline) solid var(--dash-edge);
}

.yh__kpi {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  min-width: 0;
  padding: var(--space-3) var(--space-4);
  border-radius: var(--radius-md);
  background: var(--surface-2);
}

.yh__kpi > small {
  color: var(--brand-text-muted);
  font-size: var(--text-xs);
  font-weight: 600;
}

.yh__kpi b {
  font-size: var(--text-xl);
  font-weight: 800;
  line-height: 1.1;
}

.yh__kpi b em {
  color: var(--brand-text-muted);
  font-size: var(--text-sm);
  font-style: normal;
  font-weight: 600;
}

.yh__kpi--none {
  flex-direction: row;
  grid-column: span 3;
  align-items: center;
  justify-content: center;
  color: var(--brand-text-muted);
  font-size: var(--text-sm);
}

.yh__note {
  color: var(--brand-text-muted);
  font-size: var(--text-xs);
}

.yh__bar {
  display: block;
  height: var(--space-2);
  overflow: hidden;
  border-radius: var(--radius-pill);
  background: var(--ring-track);
}

.yh__bar i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--c-primary);
}

.yh__bar--ok i {
  background: var(--st-present);
}

/* ---- planned */
.yv__planned {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(380px, 100%), 1fr));
  gap: var(--space-4);
}

.yp {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-4) var(--space-5);
  cursor: pointer;
}

.yp__count {
  display: flex;
  flex: none;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: var(--space-8);
  height: var(--space-8);
  border-radius: var(--radius-md);
  background: color-mix(in srgb, var(--st-excused) 14%, transparent);
  color: var(--st-excused);
  line-height: 1;
}

.yp__count b {
  font-size: var(--text-lg);
}

.yp__count small {
  font-size: var(--text-xs);
}

.yp__text {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-width: 0;
}

.yp__text span {
  font-size: var(--text-sm);
}

.yp__text small {
  color: var(--brand-text-muted);
  font-size: var(--text-xs);
}

/* ---- past rows */
.yv__past {
  display: flex;
  flex-direction: column;
  padding: var(--space-3) var(--space-4);
}

.yv__past-title {
  margin: 0 0 var(--space-2);
  color: var(--brand-text-muted);
  font-size: var(--text-sm);
  font-weight: 700;
}

.yr {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  min-height: var(--crud-row-h);
  padding: var(--space-2) var(--space-2);
  border-top: var(--hairline) solid var(--dash-edge);
  border-radius: var(--radius-sm);
  cursor: pointer;
}

.yr:hover {
  background: var(--surface-2);
}

.yr:focus-visible {
  outline: var(--border-focus);
}

.yr__dot {
  flex: none;
  width: var(--space-2);
  height: var(--space-2);
  border-radius: 50%;
  background: var(--tone);
}

.yr__name {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-width: 0;
}

.yr__name small {
  color: var(--brand-text-muted);
  font-size: var(--text-xs);
}

.yr__num {
  display: flex;
  flex: none;
  flex-direction: column;
  align-items: flex-end;
  min-width: 64px;
}

.yr__num small {
  color: var(--brand-text-muted);
  font-size: var(--text-xs);
}

.yr__none {
  flex: none;
  color: var(--brand-text-muted);
  font-size: var(--text-xs);
}

.yv__badge {
  display: inline-flex;
  flex: none;
  align-items: center;
  gap: var(--space-1);
  padding: var(--hairline) var(--space-2);
  border-radius: var(--radius-pill);
  background: color-mix(in srgb, var(--tone) 14%, transparent);
  color: var(--tone);
  font-size: var(--text-xs);
  font-weight: 700;
}

@media (max-width: 700px) {
  .yh {
    padding: var(--space-4);
  }

  .yh__kpis {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .yh__kpi--none {
    grid-column: 1 / -1;
  }

  /* a past year on a phone: the name, then its numbers on one line */
  .yr {
    flex-wrap: wrap;
    gap: var(--space-1) var(--space-3);
  }

  .yr__name {
    flex-basis: calc(100% - var(--space-6));
  }

  .yr__num {
    align-items: flex-start;
    min-width: 0;
  }

  .yr__none {
    flex: 1 1 auto;
  }

  .yv__missing {
    flex-wrap: wrap;
  }
}

/* ---- the create dialog */
.yv__dialog {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  width: min(520px, 94vw);
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
  grid-template-columns: 1fr 1fr;
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
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: var(--space-2);
}

.yv__tdot {
  width: var(--space-2);
  height: var(--space-2);
  border-radius: 50%;
}

.yv__tdot--quarter {
  background: var(--c-primary);
}

.yv__tdot--vacation {
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
</style>

<style>
/* the "⋯" menu lives in a portal, outside this component's scope */
.yv__menu {
  min-width: 220px;
}

.yv__menu--danger {
  color: var(--st-absent);
}
</style>

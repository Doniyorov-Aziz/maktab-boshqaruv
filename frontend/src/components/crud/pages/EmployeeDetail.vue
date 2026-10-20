<template>
  <div class="emd" data-employee-detail>
    <div class="emd__hero">
      <AvatarInitials :name="row.fullName" :id="row.id" size="xl" gradient />
      <div class="emd__titles">
        <h2>{{ row.fullName }}</h2>
        <small>{{ row.positionTitle }}</small>
        <span
          class="emd__status"
          :style="{ '--c': status.color }"
          data-employee-status
          ><i />{{ status.text }}</span
        >
      </div>
    </div>

    <div class="emd__contact">
      <a
        v-if="row.phone"
        class="emd__phone tnum"
        :href="`tel:${String(row.phone).replace(/[^\d+]/g, '')}`"
        data-employee-phone
        ><q-icon name="call" /> {{ formatPhone(row.phone) }}</a
      >
      <AppButton
        v-if="row.phone"
        variant="ghost"
        size="sm"
        icon="content_copy"
        aria-label="Nusxalash"
        @click="copy"
      />
    </div>

    <div class="emd__kpis">
      <div class="emd__kpi">
        <small>Yuklama</small>
        <b class="tnum"
          >{{ extra ? extra.weeklyLessons : '…' }}<em> soat / hafta</em></b
        >
      </div>
      <div class="emd__kpi">
        <small>Jurnal (7 ish kuni)</small>
        <b class="tnum" :style="{ color: journalTone(extra?.journal7) }">{{
          extra?.journal7 == null ? '—' : `${Math.round(extra.journal7)}%`
        }}</b>
      </div>
      <div class="emd__kpi">
        <small>Sinflar</small>
        <b class="tnum">{{ classes.length }}</b>
      </div>
    </div>

    <section v-if="extra?.journal?.some(v => v != null)">
      <h4>Jurnal to'liqligi — oxirgi 10 ish kuni</h4>
      <Sparkline
        class="emd__spark"
        :values="extra.journal"
        :min="0"
        :max="100"
        large
        color="var(--ring-journal)"
        label="Jurnal to'liqligi dinamikasi"
        data-journal-spark
      />
    </section>

    <section v-if="extra">
      <h4>Yuklama xaritasi (kunlar × darslar)</h4>
      <div
        class="emd__heat"
        :style="{ '--n': extra.lessonsPerDay }"
        role="img"
        :aria-label="`Haftasiga ${extra.weeklyLessons} dars`"
        data-load-heat
      >
        <span />
        <span
          v-for="n in extra.lessonsPerDay"
          :key="'h' + n"
          class="emd__heat-h"
          >{{ n }}</span
        >
        <template v-for="(day, d) in extra.weekdays" :key="day">
          <span class="emd__heat-day">{{ day.slice(0, 2) }}</span>
          <span
            v-for="(v, k) in extra.heat[d]"
            :key="k"
            class="emd__cell"
            :class="{ 'emd__cell--on': v > 0, 'emd__cell--twice': v > 1 }"
            :title="`${day}, ${k + 1}-dars: ${v ? 'dars bor' : 'bo‘sh'}`"
          />
        </template>
      </div>
    </section>

    <section>
      <h4>Bu haftalik dars jadvali</h4>
      <div v-if="!profile" class="skel emd__skel" />
      <p v-else-if="!week.length" class="emd__muted">Darslari yo'q.</p>
      <div v-else class="emd__week" data-week-grid>
        <div v-for="d in week" :key="d.day" class="emd__col">
          <b>{{ d.day }}</b>
          <div
            v-for="l in d.lessons"
            :key="l.lessonSlotId"
            class="emd__lesson"
            :style="{ '--c': subjectColor(l.subjectId, l.subjectColor) }"
          >
            <span class="tnum">{{ String(l.startTime).slice(0, 5) }}</span>
            <b>{{ l.className }}</b>
            <small>{{ l.subjectName }}</small>
          </div>
        </div>
      </div>
    </section>

    <section v-if="subjects.length || classes.length">
      <h4>Fanlar va sinflar</h4>
      <div class="emd__chips">
        <span
          v-for="s in subjects"
          :key="s"
          class="emd__chip emd__chip--subject"
          >{{ s }}</span
        >
      </div>
      <div class="emd__chips">
        <span v-for="c in classes" :key="c" class="emd__chip">{{ c }}</span>
      </div>
    </section>

    <div class="emd__actions">
      <AppButton
        variant="secondary"
        icon="calendar_view_week"
        label="Jadvalni ko'rish"
        data-open-timetable
        @click="
          router.push({
            path: '/timetable',
            query: { view: 'teacher', teacher: row.id }
          })
        "
      />
      <AppButton
        v-if="canEdit"
        icon="edit"
        label="Tahrirlash"
        @click="emit('edit', row)"
      />
    </div>
  </div>
</template>

<script setup>
// One employee in the side panel: contact (call, copy), status and leave dates, lessons a week,
// journal completeness and its 10-day run, the week as a heat map (days × lesson numbers), this
// week's lessons by day, subjects and classes, and the way to the timetable.
import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useQuasar, copyToClipboard } from 'quasar'
import { api } from '@/boot/axios'
import AppButton from '@/components/app/AppButton.vue'
import AvatarInitials from '@/components/pages/AvatarInitials.vue'
import Sparkline from '@/components/dashboard/Sparkline.vue'
import { subjectColor } from '@/utils/subjectColor'
import { formatPhone } from '@/utils/tashkent'
import { formatDay, listItems } from '../crud'

const props = defineProps({
  row: { type: Object, required: true },
  canEdit: { type: Boolean, default: false }
})
const emit = defineEmits(['edit'])
const router = useRouter()
const $q = useQuasar()

const DAYS = [
  'Dushanba',
  'Seshanba',
  'Chorshanba',
  'Payshanba',
  'Juma',
  'Shanba'
]
const extra = ref(null)
const profile = ref(null)
watch(
  () => props.row.id,
  async id => {
    extra.value = null
    profile.value = null
    const [e, p] = await Promise.allSettled([
      api.get(`/api/pages/lists/employees/${id}/extra`),
      api.get(`/api/profiles/teachers/${id}`)
    ])
    extra.value = e.status === 'fulfilled' ? e.value.data : null
    profile.value =
      p.status === 'fulfilled' ? p.value.data : { weeklySchedule: [] }
  },
  { immediate: true }
)

const subjects = computed(() => listItems(props.row.subjects))
const classes = computed(() => listItems(props.row.classes))
const status = computed(() => {
  const r = props.row
  if (r.status === 'ON_LEAVE')
    return {
      text:
        r.leaveFrom || r.leaveTo
          ? `Ta'tilda · ${formatDay(r.leaveFrom) || '…'} — ${formatDay(r.leaveTo) || '…'}`
          : "Ta'tilda",
      color: 'var(--st-late)'
    }
  if (r.status === 'DISMISSED')
    return { text: 'Ishdan ketgan', color: 'var(--st-unmarked)' }
  return { text: 'Ishda', color: 'var(--st-present)' }
})
const week = computed(() =>
  DAYS.map(day => ({
    day,
    lessons: (profile.value?.weeklySchedule || [])
      .filter(l => l.weekday === day)
      .sort((a, b) => String(a.startTime).localeCompare(String(b.startTime)))
  })).filter(d => d.lessons.length)
)

const journalTone = v =>
  v == null
    ? 'var(--brand-text-muted)'
    : v >= 80
      ? 'var(--st-present)'
      : v >= 50
        ? 'var(--gr-3)'
        : 'var(--st-absent)'

async function copy() {
  try {
    await copyToClipboard(formatPhone(props.row.phone))
    $q.notify({ type: 'positive', message: 'Nusxalandi ✓', timeout: 1200 })
  } catch {
    $q.notify({ type: 'warning', message: "Nusxalab bo'lmadi" })
  }
}
</script>

<style scoped>
.tnum {
  font-variant-numeric: tabular-nums;
}

.emd {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}

.emd__hero {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}

.emd__titles {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: var(--hairline);
}

.emd__titles h2 {
  margin: 0;
  font-size: var(--text-lg);
  font-weight: 800;
}

.emd__titles small,
.emd__muted {
  color: var(--brand-text-muted);
}

.emd__status {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  margin-top: var(--space-1);
  padding: var(--hairline) var(--space-2);
  border-radius: var(--radius-pill);
  background: color-mix(in srgb, var(--c) 14%, transparent);
  color: var(--c);
  font-size: var(--text-xs);
  font-weight: 700;
}

.emd__status i {
  width: var(--space-1);
  height: var(--space-1);
  border-radius: var(--radius-pill);
  background: var(--c);
}

.emd__contact {
  display: flex;
  align-items: center;
  gap: var(--space-1);
}

.emd__phone {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  color: var(--c-primary);
  font-weight: 700;
  text-decoration: none;
}

.emd__kpis {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--space-2);
}

.emd__kpi {
  display: flex;
  flex-direction: column;
  padding: var(--space-3);
  border-radius: var(--radius-md);
  background: var(--surface-2);
}

.emd__kpi small {
  color: var(--brand-text-muted);
  font-size: var(--text-xs);
}

.emd__kpi b {
  font-size: var(--text-lg);
  font-weight: 800;
}

.emd__kpi em {
  color: var(--brand-text-muted);
  font-size: var(--text-xs);
  font-style: normal;
  font-weight: 500;
}

h4 {
  margin: 0 0 var(--space-2);
  font-size: var(--text-xs);
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--brand-text-muted);
}

.emd__heat {
  display: grid;
  grid-template-columns: var(--space-6) repeat(var(--n), minmax(0, 1fr));
  gap: var(--space-1);
  font-size: var(--text-xs);
}

.emd__heat-h,
.emd__heat-day {
  color: var(--brand-text-muted);
  text-align: center;
}

.emd__heat-day {
  text-align: left;
}

.emd__cell {
  height: var(--space-5);
  border-radius: var(--radius-sm);
  background: var(--tile-none);
}

.emd__cell--on {
  background: color-mix(in srgb, var(--chart-primary) 70%, transparent);
}

.emd__cell--twice {
  background: var(--st-absent);
}

.emd__skel {
  height: calc(var(--crud-row-h) * 3);
  border-radius: var(--radius-md);
}

.emd__week {
  display: grid;
  grid-auto-flow: column;
  grid-auto-columns: minmax(var(--space-8), 1fr);
  gap: var(--space-2);
  overflow-x: auto;
  scrollbar-width: thin;
}

.emd__col {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  min-width: 0;
  font-size: var(--text-xs);
}

.emd__col > b {
  color: var(--brand-text-muted);
}

.emd__lesson {
  display: flex;
  flex-direction: column;
  padding: var(--space-1) var(--space-2);
  border-left: var(--row-stripe) solid var(--c);
  border-radius: var(--radius-sm);
  background: color-mix(in srgb, var(--c) 10%, transparent);
  min-width: 0;
}

.emd__lesson small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--brand-text-muted);
}

.emd__chips {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-1);
  margin-bottom: var(--space-2);
}

.emd__chip {
  padding: var(--hairline) var(--space-2);
  border: var(--hairline) solid var(--brand-border);
  border-radius: var(--radius-pill);
  background: var(--surface-2);
  font-size: var(--text-xs);
  font-weight: 600;
}

.emd__chip--subject {
  border-color: color-mix(in srgb, var(--c-primary) 30%, transparent);
  color: var(--c-primary);
}

.emd__actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}
</style>

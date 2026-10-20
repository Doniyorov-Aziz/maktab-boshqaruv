<template>
  <div class="std" data-student-detail>
    <div class="std__hero">
      <AvatarInitials :name="row.fullName" :id="row.id" size="xl" gradient />
      <div class="std__titles">
        <h2>{{ row.fullName }}</h2>
        <span class="std__chips">
          <span class="std__chip">{{ row.className }} sinf</span>
          <span v-if="age != null" class="std__chip">{{ age }} yosh</span>
          <span
            v-if="extra?.telegram"
            class="std__chip std__chip--tg"
            title="Ota-ona Telegram botga ulangan"
            ><q-icon name="send" /> Telegram</span
          >
        </span>
      </div>
    </div>

    <div class="std__kpis">
      <div class="std__kpi">
        <small>Davomat (30 kun)</small>
        <b :style="{ color: pctTone(row.attendance30) }">{{
          pct(row.attendance30)
        }}</b>
      </div>
      <div class="std__kpi">
        <small>O'rtacha baho</small>
        <b :style="{ color: avgColor(row.average30) }"
          >{{ row.average30 == null ? '—' : row.average30.toFixed(2) }}
          <span v-if="trend" :class="`std__trend std__trend--${trend}`">{{
            trend === 'up' ? '↗' : '↘'
          }}</span></b
        >
      </div>
      <div class="std__kpi">
        <small>Xavf balli</small>
        <b :style="{ color: riskTone }"
          >{{ extra?.risk ?? '—' }}<em v-if="extra?.risk != null"> / 100</em></b
        >
      </div>
    </div>

    <section>
      <h4>Oxirgi 30 kun davomati</h4>
      <div v-if="!extra" class="skel std__skel-dots" />
      <div v-else class="std__dots" data-attendance-dots>
        <span
          v-for="d in extra.days"
          :key="d.date"
          class="std__dot"
          :class="`std__dot--${d.status.toLowerCase()}`"
          :data-status="d.status"
        >
          <q-tooltip
            >{{ formatDay(d.date) }} · {{ STATUS_LABEL[d.status] }}</q-tooltip
          >
        </span>
      </div>
      <div class="std__legend">
        <span v-for="(label, k) in LEGEND" :key="k"
          ><i :class="`std__dot std__dot--${k.toLowerCase()}`" />{{
            label
          }}</span
        >
      </div>
    </section>

    <section v-if="extra?.risk != null">
      <h4>Xavf sabablari</h4>
      <p v-if="!extra.risk" class="std__muted" data-risk-none>
        Xavf belgilari yo'q: kelmagan kun, past va pasayayotgan baho, «2» lar
        yo'q.
      </p>
      <ul v-else class="std__reasons" data-risk-reasons>
        <li v-if="extra.parts.absence">
          <q-icon name="event_busy" /> 30 kunda {{ extra.absentDays }} kun
          kelmagan
          <b>+{{ extra.parts.absence }}</b>
        </li>
        <li v-if="extra.parts.average">
          <q-icon name="trending_down" /> O'rtacha baho past ({{
            extra.avgNow?.toFixed(2) ?? '—'
          }}) <b>+{{ extra.parts.average }}</b>
        </li>
        <li v-if="extra.parts.trend">
          <q-icon name="south_east" /> Baholar pasaymoqda ({{
            extra.avgBefore?.toFixed(2) ?? '—'
          }}
          → {{ extra.avgNow?.toFixed(2) ?? '—' }})
          <b>+{{ extra.parts.trend }}</b>
        </li>
        <li v-if="extra.parts.twos">
          <q-icon name="looks_two" /> 30 kunda {{ extra.twos }} ta «2»
          <b>+{{ extra.parts.twos }}</b>
        </li>
      </ul>
    </section>

    <section>
      <h4>Fanlar bo'yicha o'rtacha</h4>
      <div v-if="!profile" class="skel std__skel-bars" />
      <p v-else-if="!subjects.length" class="std__muted">Hali baho yo'q.</p>
      <div
        v-for="s in subjects"
        :key="s.subjectId"
        class="std__bar-row"
        :data-subject-avg="s.subjectId"
      >
        <span class="std__subj">{{ s.subjectName }}</span>
        <span class="std__bar"
          ><i
            :style="{
              width: `${((s.averageScore - 1) / 4) * 100}%`,
              background: avgColor(s.averageScore)
            }"
        /></span>
        <b class="tnum" :style="{ color: avgColor(s.averageScore) }">{{
          s.averageScore.toFixed(2)
        }}</b>
      </div>
    </section>

    <section>
      <h4>Ota-ona</h4>
      <div class="std__guardian">
        <AvatarInitials
          :name="row.guardianName || '?'"
          :id="row.id + 7"
          size="md"
        />
        <div class="std__who">
          <b>{{ row.guardianName || 'Kiritilmagan' }}</b>
          <small>{{ relation }}</small>
          <a
            v-if="row.guardianPhone"
            class="std__phone tnum"
            :href="`tel:${String(row.guardianPhone).replace(/[^\d+]/g, '')}`"
            data-guardian-phone
            >{{ formatPhone(row.guardianPhone) }}</a
          >
        </div>
      </div>
    </section>

    <section>
      <h4>Shaxsiy ma'lumot</h4>
      <dl class="std__list">
        <dt>Tug'ilgan sana</dt>
        <dd>{{ formatDay(row.birthDate) || '—' }}</dd>
        <dt>Tug'ilgan joyi</dt>
        <dd>{{ row.birthPlace || '—' }}</dd>
        <dt>Yashash manzili</dt>
        <dd data-detail="fullAddress">{{ row.fullAddress || '—' }}</dd>
      </dl>
    </section>

    <section v-if="profile?.behaviorRecords?.length">
      <h4>Xulq yozuvlari</h4>
      <div
        v-for="b in profile.behaviorRecords.slice(0, 6)"
        :key="b.id"
        class="std__behavior"
        :class="`std__behavior--${String(b.type).toLowerCase()}`"
      >
        <q-icon
          :name="b.type === 'REWARD' ? 'emoji_events' : 'warning_amber'"
        />
        <span>{{ b.description }}</span>
        <small class="tnum">{{ formatDay(b.recordDate) }}</small>
      </div>
    </section>

    <div class="std__actions">
      <AppButton
        variant="secondary"
        icon="send"
        label="Ota-onaga xabar"
        data-message-parent
        @click="
          router.push({
            path: '/broadcasts',
            query: { students: String(row.id) }
          })
        "
      />
    </div>
  </div>
</template>

<script setup>
// One student in the side panel: class, age, Telegram; attendance, average (with its trend) and
// risk; the last 30 school days as dots; why the risk score is what it is; averages by subject;
// the parent with a phone to call; birth date and address (the table leaves the address out);
// behaviour records; and a message to the parent.
import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '@/boot/axios'
import AppButton from '@/components/app/AppButton.vue'
import AvatarInitials from '@/components/pages/AvatarInitials.vue'
import { avgColor } from '@/components/journal/journal'
import { guardianRelations } from '@/utils/guardian'
import { formatPhone } from '@/utils/tashkent'
import { formatDay, ageOn } from '../crud'

const props = defineProps({
  row: { type: Object, required: true },
  today: { type: String, required: true }
})
const router = useRouter()

const STATUS_LABEL = {
  PRESENT: 'Keldi',
  LATE: 'Kechikdi',
  ABSENT: 'Kelmadi',
  EXCUSED: 'Sababli',
  NONE: 'Belgilanmagan'
}
const LEGEND = {
  PRESENT: 'Keldi',
  LATE: 'Kechikdi',
  ABSENT: 'Kelmadi',
  EXCUSED: 'Sababli'
}

const extra = ref(null)
const profile = ref(null)
watch(
  () => props.row.id,
  async id => {
    extra.value = null
    profile.value = null
    const [e, p] = await Promise.allSettled([
      api.get(`/api/pages/lists/students/${id}/extra`),
      api.get(`/api/profiles/students/${id}`)
    ])
    extra.value =
      e.status === 'fulfilled' ? e.value.data : { days: [], telegram: false }
    profile.value =
      p.status === 'fulfilled' ? p.value.data : { subjectAverages: [] }
  },
  { immediate: true }
)

const age = computed(() => ageOn(props.row.birthDate, props.today))
const relation = computed(
  () => guardianRelations[props.row.guardianRelation]?.label || 'Ota-ona'
)
const subjects = computed(() =>
  [...(profile.value?.subjectAverages || [])]
    .filter(s => s.averageScore != null)
    .sort((a, b) => b.averageScore - a.averageScore)
)
const trend = computed(() => {
  const a = props.row.average30
  const b = props.row.averagePrev
  if (a == null || b == null || Math.abs(a - b) < 0.15) return null
  return a > b ? 'up' : 'down'
})
const riskTone = computed(() => {
  const r = extra.value?.risk
  if (r == null) return 'var(--brand-text-muted)'
  return r >= 70
    ? 'var(--st-absent)'
    : r >= 40
      ? 'var(--st-late)'
      : 'var(--st-present)'
})
const pct = v => (v == null ? '—' : `${Math.round(v)}%`)
const pctTone = v =>
  v == null
    ? 'var(--brand-text-muted)'
    : v >= 95
      ? 'var(--st-present)'
      : v >= 85
        ? 'var(--gr-3)'
        : 'var(--st-absent)'
</script>

<style scoped>
.tnum {
  font-variant-numeric: tabular-nums;
}

.std {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}

.std__hero {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}

.std__titles h2 {
  margin: 0 0 var(--space-1);
  font-size: var(--text-lg);
  font-weight: 800;
}

.std__chips {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-1);
}

.std__chip {
  display: inline-flex;
  align-items: center;
  gap: var(--hairline);
  padding: var(--hairline) var(--space-2);
  border: var(--hairline) solid var(--brand-border);
  border-radius: var(--radius-pill);
  background: var(--surface-2);
  font-size: var(--text-xs);
  font-weight: 600;
}

.std__chip--tg {
  border-color: color-mix(in srgb, var(--st-excused) 40%, transparent);
  color: var(--st-excused);
}

.std__kpis {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--space-2);
}

.std__kpi {
  display: flex;
  flex-direction: column;
  padding: var(--space-3);
  border-radius: var(--radius-md);
  background: var(--surface-2);
}

.std__kpi small,
.std__muted {
  color: var(--brand-text-muted);
  font-size: var(--text-xs);
}

.std__kpi b {
  font-size: var(--text-lg);
  font-weight: 800;
  font-variant-numeric: tabular-nums;
}

.std__kpi em {
  color: var(--brand-text-muted);
  font-size: var(--text-xs);
  font-style: normal;
  font-weight: 500;
}

.std__trend--up {
  color: var(--gr-5);
}

.std__trend--down {
  color: var(--gr-2);
}

h4 {
  margin: 0 0 var(--space-2);
  font-size: var(--text-xs);
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--brand-text-muted);
}

.std__skel-dots {
  height: var(--space-5);
  border-radius: var(--radius-sm);
}

.std__skel-bars {
  height: calc(var(--crud-row-h) * 3);
  border-radius: var(--radius-sm);
}

.std__dots {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-1);
}

.std__dot {
  display: inline-block;
  width: var(--space-4);
  height: var(--space-4);
  border-radius: var(--radius-sm);
  background: var(--tile-none);
}

.std__dot--present {
  background: var(--st-present);
}

.std__dot--late {
  background: var(--st-late);
}

.std__dot--absent {
  background: var(--st-absent);
}

.std__dot--excused {
  background: var(--st-excused);
}

.std__legend {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
  margin-top: var(--space-2);
  color: var(--brand-text-muted);
  font-size: var(--text-xs);
}

.std__legend span {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
}

.std__legend .std__dot {
  width: var(--space-2);
  height: var(--space-2);
}

.std__reasons {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  margin: 0;
  padding: 0;
  list-style: none;
  font-size: var(--text-sm);
}

.std__reasons li {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2);
  border-radius: var(--radius-md);
  background: color-mix(in srgb, var(--st-late) 9%, transparent);
}

.std__reasons b {
  margin-left: auto;
  color: var(--st-late);
}

.std__bar-row {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(0, 1.5fr) auto;
  align-items: center;
  gap: var(--space-2);
  padding: var(--hairline) 0;
  font-size: var(--text-sm);
}

.std__subj {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.std__bar {
  position: relative;
  height: var(--space-2);
  border-radius: var(--radius-pill);
  background: var(--ring-track);
  overflow: hidden;
}

.std__bar i {
  position: absolute;
  inset: 0 auto 0 0;
  border-radius: inherit;
}

.std__guardian {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.std__who {
  display: flex;
  flex-direction: column;
}

.std__who small {
  color: var(--brand-text-muted);
}

.std__phone {
  color: var(--c-primary);
  font-weight: 600;
  text-decoration: none;
}

.std__list {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(0, 3fr);
  margin: 0;
  border: var(--hairline) solid var(--dash-edge);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.std__list dt,
.std__list dd {
  margin: 0;
  padding: var(--space-2) var(--space-3);
  border-bottom: var(--hairline) solid var(--dash-edge);
  font-size: var(--text-sm);
}

.std__list dt {
  background: var(--surface-2);
  color: var(--brand-text-muted);
}

.std__list dt:last-of-type,
.std__list dd:last-of-type {
  border-bottom: 0;
}

.std__behavior {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-1) 0;
  font-size: var(--text-sm);
}

.std__behavior small {
  margin-left: auto;
  color: var(--brand-text-muted);
}

.std__behavior--reward .q-icon {
  color: var(--st-present);
}

.std__behavior--warning .q-icon {
  color: var(--st-late);
}

.std__actions {
  display: flex;
  gap: var(--space-2);
}
</style>

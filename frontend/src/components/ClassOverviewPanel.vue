<template>
  <div class="cov">
    <div v-if="!overview" class="cov-card row q-col-gutter-md">
      <div class="col-12 col-md-4"><q-skeleton height="72px" /></div>
      <div class="col-12 col-md-8"><q-skeleton height="72px" /></div>
    </div>

    <template v-else>
      <div class="cov-card cov-grid">
        <!-- 👤 homeroom teacher -->
        <section class="cov-part cov-teacher" aria-label="Sinf rahbari">
          <div class="cov-caption"
            >👤 Sinf rahbari · {{ overview.className }}</div
          >
          <div
            v-if="overview.homeroomTeacher"
            class="row items-center no-wrap q-gutter-x-sm"
          >
            <q-avatar size="42px" class="cov-avatar">{{
              initials(overview.homeroomTeacher.fullName)
            }}</q-avatar>
            <div class="col" style="min-width: 0">
              <div class="cov-name ellipsis">
                {{ overview.homeroomTeacher.fullName }}
              </div>
              <div
                v-if="overview.homeroomTeacher.phone"
                class="row items-center no-wrap"
              >
                <a
                  class="cov-phone"
                  :href="`tel:${overview.homeroomTeacher.phone}`"
                  >📞 {{ formatPhone(overview.homeroomTeacher.phone) }}</a
                >
                <q-btn
                  flat
                  round
                  dense
                  size="sm"
                  icon="content_copy"
                  class="q-ml-xs cov-copy"
                  aria-label="Telefonni nusxalash"
                  @click="copy(overview.homeroomTeacher.phone)"
                >
                  <q-tooltip>Nusxalash</q-tooltip>
                </q-btn>
              </div>
              <div v-else class="cov-muted">Telefon kiritilmagan</div>
            </div>
          </div>
          <div v-else class="cov-muted">
            Sinf rahbari biriktirilmagan
            <router-link
              v-if="isAdmin"
              :to="`/app/school-classes?edit=${overview.classId}`"
              class="cov-link"
              >Biriktirish</router-link
            >
          </div>
        </section>

        <!-- 🕐 what is on now -->
        <section
          class="cov-part cov-now"
          :class="`cov-now--${state?.kind}`"
          aria-live="polite"
          aria-label="Hozirgi dars"
        >
          <div class="cov-caption">🕐 Hozirgi dars</div>
          <template v-if="state.kind === 'lesson'">
            <div class="cov-now-title">
              🟢 Hozir: {{ state.current.lessonNo }}-dars ·
              <b>{{ state.current.subjectName }}</b> ·
              {{ hhmm(state.current.start) }}–{{ hhmm(state.current.end) }}
            </div>
            <div
              v-if="state.current.teacher"
              class="cov-sub row items-center q-gutter-x-sm"
            >
              <span>{{ state.current.teacher.fullName }}</span>
              <a
                v-if="state.current.teacher.phone"
                class="cov-phone"
                :href="`tel:${state.current.teacher.phone}`"
                >📞 {{ formatPhone(state.current.teacher.phone) }}</a
              >
            </div>
            <div class="row items-center no-wrap q-mt-xs q-gutter-x-sm">
              <q-linear-progress
                :value="state.progress"
                rounded
                size="6px"
                color="positive"
                :track-color="$q.dark.isActive ? 'grey-9' : 'grey-3'"
                class="col"
              />
              <span class="cov-left">yana {{ state.leftMinutes }} daq</span>
            </div>
          </template>
          <div v-else-if="state.kind === 'break'" class="cov-now-title">
            ☕ Tanaffus · keyingi: {{ state.next.lessonNo }}-dars ·
            <b>{{ state.next.subjectName }}</b> ·
            {{ hhmm(state.next.start) }} da
            <span class="cov-muted">({{ state.inMinutes }} daq dan keyin)</span>
          </div>
          <div v-else-if="state.kind === 'before'" class="cov-now-title">
            🌅 Darslar {{ hhmm(state.next.start) }} da boshlanadi · 1-dars:
            <b>{{ state.next.subjectName }}</b>
          </div>
          <div v-else-if="state.kind === 'after'" class="cov-now-title">
            ✅ Bugungi darslar tugadi ({{ state.count }} ta dars)
          </div>
          <div v-else-if="state.kind === 'off'" class="cov-now-title">
            😴 Bugun dam olish kuni<template
              v-if="state.reason && state.reason !== 'Yakshanba'"
            >
              · {{ state.reason }}</template
            >
          </div>
          <div v-else class="cov-now-title cov-muted">
            Bugun bu sinfda dars yo'q
          </div>
        </section>

        <section v-if="canGrade && state.gradable" class="cov-part cov-action">
          <q-btn
            unelevated
            no-caps
            color="primary"
            icon="edit_note"
            label="Shu darsga baho qo'yish"
            class="cov-grade-btn"
            @click="emit('grade-lesson', state.gradable)"
          >
            <q-tooltip
              >{{ state.gradable.lessonNo }}-dars ·
              {{ state.gradable.subjectName }}</q-tooltip
            >
          </q-btn>
        </section>
      </div>

      <!-- today's timetable: past lessons dim, the current one highlighted -->
      <div
        v-if="overview.todayLessons?.length"
        class="cov-strip row items-center no-wrap"
      >
        <span class="cov-strip-label">Bugun:</span>
        <button
          v-for="l in overview.todayLessons"
          :key="l.lessonNo + '-' + l.subjectId"
          type="button"
          class="cov-chip"
          :class="{
            'cov-chip--now': state.kind === 'lesson' && state.current === l,
            'cov-chip--past': toMinutes(l.end) <= nowMinutes,
            'cov-chip--picked': l.subjectId === subjectId
          }"
          :title="`${hhmm(l.start)}–${hhmm(l.end)}${l.teacher ? ' · ' + l.teacher.fullName : ''}`"
          @click="emit('pick-subject', l.subjectId)"
        >
          <span class="cov-chip-no">{{ l.lessonNo }}</span>
          {{ l.subjectName }}
        </button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useQuasar, copyToClipboard } from 'quasar'
import {
  lessonState,
  formatPhone,
  hhmm,
  initials,
  toMinutes
} from '@/utils/tashkent'

const props = defineProps({
  overview: { type: Object, default: null },
  /** minutes since midnight, Tashkent time — the page ticks it every 30 s */
  nowMinutes: { type: Number, default: 0 },
  subjectId: { type: Number, default: null },
  canGrade: { type: Boolean, default: false },
  isAdmin: { type: Boolean, default: false }
})
const emit = defineEmits(['pick-subject', 'grade-lesson'])
const $q = useQuasar()

const state = computed(
  () => lessonState(props.overview, props.nowMinutes) || { kind: 'none' }
)

async function copy(phone) {
  try {
    await copyToClipboard(phone)
    $q.notify({ type: 'positive', message: 'Nusxalandi ✓', timeout: 1500 })
  } catch {
    $q.notify({ type: 'warning', message: "Nusxalab bo'lmadi" })
  }
}
</script>

<style scoped>
.cov-card {
  position: relative;
  background: var(--card-bg);
  border-radius: var(--radius-lg);
  padding: 14px 16px;
  border: 1px solid transparent;
  background:
    linear-gradient(var(--card-bg), var(--card-bg)) padding-box,
    linear-gradient(120deg, rgba(79, 70, 229, 0.45), rgba(16, 185, 129, 0.35))
      border-box;
}

.cov-grid {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) minmax(320px, 2fr) auto;
  gap: 16px;
  align-items: center;
}

@media (max-width: 1023px) {
  .cov-grid {
    grid-template-columns: 1fr;
  }
}

.cov-part + .cov-part {
  border-left: 1px solid var(--brand-border);
  padding-left: 16px;
}

@media (max-width: 1023px) {
  .cov-part + .cov-part {
    border-left: none;
    padding-left: 0;
    border-top: 1px solid var(--brand-border);
    padding-top: 12px;
  }
}

.cov-caption {
  font-size: var(--text-xs, 12px);
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: var(--brand-text-muted);
  margin-bottom: 6px;
}

.cov-avatar {
  background: var(--color-brand);
  color: #fff;
  font-weight: 700;
  font-size: 15px;
}

.cov-name {
  font-weight: 700;
  color: var(--text-primary);
}

.cov-phone {
  color: var(--color-brand);
  font-weight: 600;
  text-decoration: none;
  white-space: nowrap;
}

.body--dark .cov-phone,
.body--dark .cov-link {
  color: #a5b4fc;
}

.cov-phone:hover {
  text-decoration: underline;
}

.cov-link {
  margin-left: 8px;
  font-weight: 600;
  color: var(--color-brand);
}

.cov-muted {
  color: var(--brand-text-muted);
}

.cov-now {
  border-radius: 12px;
}

.cov-now-title {
  font-size: 16px;
  line-height: 1.4;
  color: var(--text-primary);
}

.cov-now--lesson .cov-now-title {
  font-size: 17px;
}

.cov-sub {
  font-size: var(--text-sm, 13px);
  color: var(--brand-text-muted);
  margin-top: 2px;
}

.cov-left {
  font-size: var(--text-xs, 12px);
  font-weight: 600;
  color: var(--color-success);
  white-space: nowrap;
}

.cov-grade-btn {
  white-space: nowrap;
}

.cov-strip {
  gap: 6px;
  margin-top: 8px;
  overflow-x: auto;
  padding: 2px 2px 4px;
  scrollbar-width: thin;
}

.cov-strip-label {
  font-size: var(--text-xs, 12px);
  font-weight: 600;
  color: var(--brand-text-muted);
  margin-right: 2px;
  flex: none;
}

.cov-chip {
  flex: none;
  font: inherit;
  font-size: var(--text-sm, 13px);
  border: 1px solid var(--brand-border);
  background: var(--card-bg);
  color: var(--text-primary);
  border-radius: 999px;
  padding: 3px 10px 3px 4px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.cov-chip:hover {
  border-color: var(--color-brand);
}

.cov-chip-no {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: var(--surface-2);
  font-size: 11px;
  font-weight: 700;
}

.cov-chip--past {
  opacity: 0.55;
}

.cov-chip--picked {
  border-color: var(--color-brand);
}

.cov-chip--now {
  opacity: 1;
  background: var(--color-brand);
  border-color: var(--color-brand);
  color: #fff;
  font-weight: 600;
}

.cov-chip--now .cov-chip-no {
  background: rgba(255, 255, 255, 0.25);
  color: #fff;
}
</style>

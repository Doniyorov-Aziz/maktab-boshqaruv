<template>
  <SideDrawer
    :model-value="modelValue"
    :title="lesson ? lesson.subject : ''"
    :subtitle="subtitle"
    icon="menu_book"
    :color="color"
    @update:model-value="v => emit('update:modelValue', v)"
  >
    <div v-if="lesson" class="ld" data-lesson-drawer>
      <dl class="ld__facts">
        <div>
          <dt>Sinf</dt>
          <dd>
            <router-link :to="`/classes/${lesson.classId}`">{{
              lesson.className
            }}</router-link>
          </dd>
        </div>
        <div>
          <dt>Vaqt</dt>
          <dd>
            {{ lesson.weekday }}, {{ periodNo }}-dars ·
            {{ hm(lesson.start) }}–{{ hm(lesson.end) }}
          </dd>
        </div>
        <div>
          <dt>O'qituvchi</dt>
          <dd class="ld__teacher">
            <AvatarInitials
              size="sm"
              :name="lesson.teacher"
              :id="lesson.teacherId"
            />
            <span>
              <b>{{ lesson.teacher }}</b>
              <a v-if="phone" :href="`tel:${phone}`" class="ld__phone">{{
                formatPhone(phone)
              }}</a>
            </span>
          </dd>
        </div>
        <div>
          <dt>Xona</dt>
          <dd>{{ roomLabel(lesson.room) }}</dd>
        </div>
        <div>
          <dt>{{ isToday ? 'Bugungi davomat' : 'Sana' }}</dt>
          <dd>
            <span v-if="isToday" class="ld__state" :data-state="state">
              {{ STATE_TEXT[state || 'NONE'] }}
            </span>
            <span v-else>{{ dateLabel }} (keyingi dars)</span>
          </dd>
        </div>
      </dl>

      <div v-if="sub" class="ld__subbox" data-substitution>
        <q-icon name="swap_horiz" />
        <span
          ><b>{{ sub.substitute }}</b> — o'rinbosar ({{ dateLabel }})<template
            v-if="sub.reason"
          >
            · {{ sub.reason }}</template
          ></span
        >
        <AppButton
          v-if="canEdit"
          variant="ghost"
          size="sm"
          icon="close"
          aria-label="O'rinbosarni bekor qilish"
          :loading="busy"
          @click="removeSub"
        />
      </div>

      <div class="ld__actions">
        <AppButton
          icon="fact_check"
          label="Davomat olish"
          size="sm"
          :to="{
            path: '/attendance',
            query: { lessonSlotId: lesson.id, date }
          }"
        />
        <AppButton
          variant="secondary"
          icon="grade"
          label="Jurnal"
          size="sm"
          :to="{
            path: '/gradebook',
            query: { class: lesson.classId, subject: lesson.subjectId }
          }"
        />
        <AppButton
          v-if="canEdit"
          variant="secondary"
          icon="edit"
          label="Tahrirlash"
          size="sm"
          :to="{ path: '/app/lesson-slots', query: { edit: lesson.id } }"
        />
      </div>

      <section v-if="canEdit" class="ld__subst">
        <AppButton
          v-if="!candidates"
          variant="secondary"
          icon="person_off"
          :label="`O'qituvchi kelmadi → o'rinbosar (${dateLabel})`"
          :loading="loadingCandidates"
          data-find-substitute
          @click="loadCandidates"
        />
        <template v-else>
          <h4>
            Bo'sh o'qituvchilar · {{ hm(lesson.start) }}–{{ hm(lesson.end) }},
            {{ dateLabel }}
          </h4>
          <p class="ld__hint">
            Faqat shu sana uchun almashtiriladi — doimiy jadval o'zgarmaydi.
          </p>
          <EmptyState
            v-if="!candidates.candidates.length"
            title="Bu vaqtda bo'sh o'qituvchi yo'q"
            icon="person_search"
          />
          <ul class="ld__cands">
            <li
              v-for="c in candidates.candidates"
              :key="c.id"
              :data-candidate="c.id"
            >
              <AvatarInitials size="sm" :name="c.name" :id="c.id" />
              <span class="ld__cand">
                <b>{{ c.name }}</b>
                <small
                  ><span :class="['ld__match', `ld__match--${c.match}`]">{{
                    MATCH[c.match]
                  }}</span>
                  · {{ c.hours }} soat/hafta · bugun
                  {{ c.lessonsThatDay }} dars</small
                >
              </span>
              <AppButton
                size="sm"
                label="Tanlash"
                :loading="busy"
                @click="choose(c)"
              />
            </li>
          </ul>
        </template>
      </section>
    </div>
  </SideDrawer>
</template>

<script setup>
// A lesson in full: class, time, teacher (the phone can be called), room and today's
// attendance (✓ taken, 🤖 automatic, ⏳ not yet), with Davomat olish / Jurnal / Tahrirlash.
// "O'qituvchi kelmadi → o'rinbosar" lists the teachers free at that time — the same
// subject first, then a close one, the lighter load first — and saves the choice for that
// one date only.
import { computed, ref, watch } from 'vue'
import { useQuasar } from 'quasar'
import SideDrawer from '@/components/dashboard/SideDrawer.vue'
import AppButton from '@/components/app/AppButton.vue'
import AvatarInitials from '@/components/pages/AvatarInitials.vue'
import EmptyState from '@/components/pages/EmptyState.vue'
import { api } from '@/boot/axios'
import { useSchoolStore } from '@/stores/school'
import { subjectColor } from '@/utils/subjectColor'
import { formatPhone, tashkentNow } from '@/utils/tashkent'
import { hm, nextDateOf, roomLabel } from './timetable'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  lesson: { type: Object, default: null },
  periods: { type: Array, default: () => [] },
  phone: { type: String, default: null },
  canEdit: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue', 'changed'])
const $q = useQuasar()
const school = useSchoolStore()

const STATE_TEXT = {
  MANUAL: '✓ Davomat olingan',
  AUTO: '🤖 Avtomatik belgilangan',
  NONE: '⏳ Hali olinmagan'
}
const MATCH = { SAME: 'Shu fan', RELATED: 'Yaqin fan', OTHER: 'Boshqa fan' }

const color = computed(() =>
  props.lesson
    ? subjectColor(props.lesson.subjectId, props.lesson.subjectColor)
    : 'var(--chart-primary)'
)
const periodNo = computed(
  () =>
    props.periods.find(p => hm(p.start) === hm(props.lesson?.start))?.n || '—'
)
const date = computed(() =>
  props.lesson ? nextDateOf(props.lesson.weekday) : null
)
const isToday = computed(() => date.value === tashkentNow().date)
const dateLabel = computed(() => {
  if (!date.value) return ''
  const [y, m, d] = date.value.split('-')
  return `${d}.${m}.${y}`
})
const subtitle = computed(() =>
  props.lesson
    ? `${props.lesson.className} · ${props.lesson.weekday}, ${hm(props.lesson.start)}–${hm(props.lesson.end)}`
    : ''
)

// the chosen date's state and substitution
const day = ref(null)
const state = computed(() => day.value?.states?.[props.lesson?.id] || null)
const sub = computed(
  () =>
    day.value?.substitutions?.find(s => s.lessonSlotId === props.lesson?.id) ||
    null
)

async function loadDay() {
  if (!props.lesson) return
  const res = await api.get('/api/pages/timetable/day', {
    params: { schoolId: school.activeSchoolId, date: date.value }
  })
  day.value = res.data
}

const candidates = ref(null)
const loadingCandidates = ref(false)
const busy = ref(false)

watch(
  () => [props.modelValue, props.lesson?.id],
  ([open]) => {
    candidates.value = null
    day.value = null
    if (open) loadDay().catch(() => {})
  },
  { immediate: true }
)

async function loadCandidates() {
  loadingCandidates.value = true
  try {
    const res = await api.get('/api/pages/timetable/substitutes', {
      params: {
        schoolId: school.activeSchoolId,
        lessonSlotId: props.lesson.id,
        date: date.value
      }
    })
    candidates.value = res.data
  } catch (e) {
    $q.notify({ type: 'negative', message: e.friendlyMessage || 'Xatolik' })
  } finally {
    loadingCandidates.value = false
  }
}

async function choose(c) {
  busy.value = true
  try {
    await api.post(
      '/api/pages/timetable/substitutions',
      { lessonSlotId: props.lesson.id, date: date.value, substituteId: c.id },
      { params: { schoolId: school.activeSchoolId } }
    )
    $q.notify({
      type: 'positive',
      message: `${c.name} — ${dateLabel.value} uchun o'rinbosar`
    })
    candidates.value = null
    await loadDay()
    emit('changed')
  } catch (e) {
    $q.notify({ type: 'negative', message: e.friendlyMessage || 'Xatolik' })
  } finally {
    busy.value = false
  }
}

async function removeSub() {
  busy.value = true
  try {
    await api.delete(`/api/pages/timetable/substitutions/${sub.value.id}`, {
      params: { schoolId: school.activeSchoolId }
    })
    await loadDay()
    emit('changed')
  } finally {
    busy.value = false
  }
}
</script>

<style scoped>
.ld {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  padding-top: var(--space-3);
}

.ld__facts {
  margin: 0;
  display: grid;
  gap: var(--space-3);
}

.ld__facts > div {
  display: grid;
  grid-template-columns: var(--name-col-sm) minmax(0, 1fr);
  gap: var(--space-3);
  align-items: center;
}

.ld__facts dt {
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
}

.ld__facts dd {
  margin: 0;
  font-size: var(--text-sm);
  font-weight: 600;
}

.ld__teacher {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.ld__teacher span {
  display: flex;
  flex-direction: column;
}

.ld__phone {
  font-weight: 500;
  font-size: var(--text-xs);
  color: var(--c-primary);
}

.ld__state[data-state='MANUAL'] {
  color: var(--st-present);
}

.ld__state[data-state='NONE'] {
  color: var(--st-late);
}

.ld__subbox {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-md);
  background: color-mix(in srgb, var(--st-late) 12%, transparent);
  color: var(--text-primary);
  font-size: var(--text-sm);
}

.ld__subbox span {
  flex: 1 1 auto;
}

.ld__actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.ld__subst h4 {
  margin: 0;
  font-size: var(--text-sm);
  font-weight: 700;
}

.ld__hint {
  margin: var(--space-1) 0 var(--space-2);
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
}

.ld__cands {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.ld__cands li {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-2);
  border-radius: var(--radius-md);
}

.ld__cands li:hover {
  background: var(--surface-2);
}

.ld__cand {
  flex: 1 1 auto;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.ld__cand b {
  font-size: var(--text-sm);
}

.ld__cand small {
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
}

.ld__match {
  font-weight: 700;
}

.ld__match--SAME {
  color: var(--st-present);
}

.ld__match--RELATED {
  color: var(--chart-secondary);
}
</style>

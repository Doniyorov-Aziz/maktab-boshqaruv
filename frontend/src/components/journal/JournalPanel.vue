<template>
  <section class="jp dash-card" data-journal-panel>
    <div class="jp__people">
      <div
        v-for="p in people"
        :key="p.role"
        class="jp__person"
        :data-person="p.role"
      >
        <AvatarInitials
          :name="p.name || '?'"
          :id="p.id || 0"
          size="sm"
          gradient
        />
        <div class="jp__who">
          <span class="jp__role">{{ p.role }}</span>
          <b class="ellipsis">{{ p.name || 'Biriktirilmagan' }}</b>
          <span v-if="p.phone" class="jp__phone">
            {{ formatPhone(p.phone) }}
            <button
              type="button"
              class="jp__copy"
              :aria-label="`${p.role} telefonini nusxalash`"
              data-copy-phone
              @click="copy(p.phone)"
            >
              <q-icon name="content_copy" />
            </button>
          </span>
        </div>
      </div>
    </div>

    <div
      class="jp__lesson"
      :class="`jp__lesson--${lessonKind}`"
      data-lesson-state
    >
      <span class="jp__role">{{ lessonCaption }}</span>
      <b class="ellipsis">{{ lessonText }}</b>
      <span v-if="lessonNote" class="jp__note">{{ lessonNote }}</span>
      <AppButton
        v-if="canGrade && gradeNow"
        size="sm"
        icon="edit_note"
        label="Shu darsga baho qo'yish"
        data-grade-lesson
        @click="emit('grade-lesson', gradeNow)"
      />
      <AppButton
        v-else
        size="sm"
        variant="secondary"
        icon="today"
        label="Bugungi ustunga o'tish"
        data-go-today
        @click="emit('go-today')"
      />
    </div>

    <div class="jp__kpis">
      <div v-for="k in kpis" :key="k.key" class="jp__kpi" :data-kpi="k.key">
        <span class="jp__kpi-label">{{ k.label }}</span>
        <span class="jp__kpi-row">
          <b class="jp__kpi-value dash-num" :style="{ color: k.color }">{{
            k.text
          }}</b>
          <DeltaPill
            :value="k.delta"
            :percent="k.percent"
            :digits="k.digits"
            :title="k.deltaTitle"
          />
        </span>
        <Sparkline
          :values="k.spark"
          :color="k.color"
          :label="`${k.label}: oy davomida`"
          class="jp__spark"
        />
      </div>
    </div>
  </section>
</template>

<script setup>
// Above the gradebook, one low band: the class teacher and the subject teacher (phone with a
// copy button), where today's lessons stand with the way into the grading (during a lesson of
// this subject: "Shu darsga baho qo'yish", otherwise "Bugungi ustunga o'tish"), and the month's
// four numbers — average, mastery, quality, count — each against last month with its run.
import { computed } from 'vue'
import { useQuasar, copyToClipboard } from 'quasar'
import AvatarInitials from '@/components/pages/AvatarInitials.vue'
import DeltaPill from '@/components/dashboard/DeltaPill.vue'
import Sparkline from '@/components/dashboard/Sparkline.vue'
import AppButton from '@/components/app/AppButton.vue'
import { formatPhone, lessonState, hhmm } from '@/utils/tashkent'
import { avgColor } from './journal'

const props = defineProps({
  overview: { type: Object, default: null },
  subjectId: { type: Number, default: null },
  teacher: { type: Object, default: null },
  // stats() of the month
  current: { type: Object, required: true },
  previous: { type: Object, default: null },
  series: { type: Object, required: true },
  nowMinutes: { type: Number, default: 0 },
  isToday: { type: Boolean, default: true },
  canGrade: { type: Boolean, default: false }
})
const emit = defineEmits(['grade-lesson', 'go-today'])
const $q = useQuasar()

const people = computed(() => [
  {
    role: 'Sinf rahbari',
    id: props.overview?.homeroomTeacher?.id,
    name: props.overview?.homeroomTeacher?.fullName,
    phone: props.overview?.homeroomTeacher?.phone
  },
  {
    role: "Fan o'qituvchisi",
    id: props.teacher?.id,
    name: props.teacher?.fullName,
    phone: props.teacher?.phone
  }
])

const state = computed(() =>
  props.isToday ? lessonState(props.overview, props.nowMinutes) : null
)
const lessonKind = computed(() => state.value?.kind || 'none')
const lessonCaption = computed(() =>
  state.value?.kind === 'lesson' ? '🟢 Hozir' : '🕐 Bugun'
)
const lessonText = computed(() => {
  const s = state.value
  if (!s) return "Bugungi darslar ma'lumoti yo'q"
  if (s.kind === 'off')
    return `Dam olish kuni${s.reason ? ` — ${s.reason}` : ''}`
  if (s.kind === 'none') return "Bugun dars yo'q"
  if (s.kind === 'lesson')
    return `${s.current.lessonNo}-dars · ${s.current.subjectName}`
  if (s.kind === 'before') return `Darslar ${hhmm(s.next.start)} da boshlanadi`
  if (s.kind === 'break') return `Tanaffus · keyin ${s.next.subjectName}`
  return `Darslar tugadi (${s.count} ta)`
})
const lessonNote = computed(() => {
  const s = state.value
  if (s?.kind === 'lesson')
    return `yana ${s.leftMinutes} daq · ${hhmm(s.current.start)}–${hhmm(s.current.end)}`
  if (s?.kind === 'break' || s?.kind === 'before')
    return `${s.inMinutes} daqiqadan keyin`
  return ''
})
// grading "this lesson" only while a lesson of the class is going on
const gradeNow = computed(() =>
  state.value?.kind === 'lesson' ? state.value.current : null
)

// one decimal, as the pill shows it (a change under 0.05 reads as 0, not as −0.0)
const delta = (now, before) =>
  now == null || before == null ? null : Math.round((now - before) * 10) / 10
const kpis = computed(() => {
  const c = props.current
  const p = props.previous
  const title = p?.count ? "O'tgan oyga nisbatan" : "O'tgan oyda baho yo'q"
  return [
    {
      key: 'average',
      label: "O'rtacha",
      text: c.average == null ? '—' : c.average.toFixed(2),
      color: avgColor(c.average),
      delta: p?.count ? delta(c.average, p.average) : null,
      digits: 2,
      percent: false,
      deltaTitle: title,
      spark: props.series.average
    },
    {
      key: 'mastery',
      label: "O'zlashtirish",
      text: c.mastery == null ? '—' : `${Math.round(c.mastery)}%`,
      color: 'var(--chart-primary)',
      delta: p?.count ? delta(c.mastery, p.mastery) : null,
      digits: 1,
      percent: true,
      deltaTitle: title,
      spark: props.series.mastery
    },
    {
      key: 'quality',
      label: 'Sifat',
      text: c.quality == null ? '—' : `${Math.round(c.quality)}%`,
      color: 'var(--gr-4)',
      delta: p?.count ? delta(c.quality, p.quality) : null,
      digits: 1,
      percent: true,
      deltaTitle: title,
      spark: props.series.quality
    },
    {
      key: 'count',
      label: 'Baholar',
      text: String(c.count),
      color: 'var(--st-excused)',
      delta: p?.count ? c.count - p.count : null,
      digits: 0,
      percent: false,
      deltaTitle: title,
      spark: props.series.count
    }
  ]
})

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
.jp {
  display: grid;
  grid-template-columns: auto minmax(var(--field-w-sm), 1fr) minmax(0, 2.2fr);
  align-items: center;
  gap: var(--space-5);
  max-height: var(--gb-panel-max-h);
  padding: var(--space-3) var(--space-5);
}

.jp:hover {
  transform: none;
}

.jp__people {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.jp__person {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  min-width: 0;
}

.jp__who {
  display: grid;
  grid-template-columns: auto auto;
  column-gap: var(--space-2);
  align-items: baseline;
  min-width: 0;
  font-size: var(--text-sm);
}

.jp__role {
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
}

.jp__who .jp__role {
  grid-column: 1 / -1;
}

.jp__phone {
  display: inline-flex;
  align-items: center;
  gap: var(--hairline);
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

.jp__copy {
  display: inline-flex;
  padding: var(--hairline);
  border: 0;
  border-radius: var(--radius-sm);
  background: transparent;
  color: inherit;
  cursor: pointer;
}

.jp__copy:hover {
  color: var(--c-primary);
}

.jp__lesson {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: var(--hairline);
  min-width: 0;
  padding-left: var(--space-4);
  border-left: var(--row-stripe) solid var(--st-unmarked);
}

.jp__lesson--lesson {
  border-left-color: var(--st-present);
}

.jp__lesson--break,
.jp__lesson--before {
  border-left-color: var(--st-late);
}

.jp__lesson b {
  max-width: 100%;
}

.jp__lesson .app-btn,
.jp__lesson :deep(.q-btn) {
  margin-top: var(--space-1);
}

.jp__note {
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
}

.jp__kpis {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--space-3);
}

.jp__kpi {
  display: flex;
  flex-direction: column;
  gap: var(--hairline);
  min-width: 0;
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-md);
  background: var(--surface-2);
}

.jp__kpi-label {
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
  white-space: nowrap;
}

.jp__kpi-row {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.jp__kpi-value {
  font-size: var(--text-lg);
  font-variant-numeric: tabular-nums;
}

.jp__spark {
  --spark-h: var(--space-5);
}

@media (max-width: 1023px) {
  .jp {
    grid-template-columns: auto minmax(0, 1fr);
    max-height: none;
  }

  .jp__kpis {
    grid-column: 1 / -1;
  }
}

@media (max-width: 599px) {
  .jp {
    grid-template-columns: minmax(0, 1fr);
    padding: var(--space-3);
  }

  .jp__kpis {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>

<template>
  <div
    class="lc"
    :class="{
      'lc--now': now,
      'lc--past': past,
      'lc--dim': dim,
      'lc--clash': clash,
      'lc--drag': draggable
    }"
    :style="{ '--c': color }"
    :draggable="draggable"
    role="button"
    tabindex="0"
    :aria-label="`${lesson.subject}, ${lesson.className}, ${lesson.teacher}, ${roomText}`"
    :data-lesson="lesson.id"
    :data-subject="lesson.subjectId"
    @click="emit('open', lesson)"
    @keydown.enter="emit('open', lesson)"
    @dragstart="onDragStart"
    @dragend="emit('dragend')"
  >
    <div class="lc__top">
      <b class="lc__subject">{{ lesson.subject }}</b>
      <span v-if="state" class="lc__state" :title="STATE_TITLE[state]">{{
        STATE_ICON[state]
      }}</span>
    </div>
    <div class="lc__meta">
      <template v-if="mode !== 'teacher'">
        <AvatarInitials
          size="xs"
          :name="lesson.teacher"
          :id="lesson.teacherId"
          class="lc__avatar"
        />
        <span class="lc__who">{{ substitute || lastName }}</span>
      </template>
      <span v-if="mode !== 'class'" class="lc__class">{{
        lesson.className
      }}</span>
      <span v-if="mode !== 'room'" class="lc__room">{{ roomText }}</span>
    </div>
    <div v-if="now" class="lc__now">Hozir · {{ left }} daq qoldi</div>
    <div v-else-if="substitute" class="lc__sub">o'rinbosar</div>
  </div>
</template>

<script setup>
// One lesson in the week grid: the subject in bold over a light tint of its colour with a
// 4 px stripe on the left, the teacher's initials and surname, the room. The lesson going
// on now glows and says how long is left; earlier lessons of today are faded. In edit
// mode the card can be dragged to another cell.
import { computed } from 'vue'
import AvatarInitials from '@/components/pages/AvatarInitials.vue'
import { subjectColor } from '@/utils/subjectColor'
import { minutes, roomLabel } from './timetable'

const props = defineProps({
  lesson: { type: Object, required: true },
  // which view: the view's own entity is not repeated on the card
  mode: { type: String, default: 'class' },
  // minutes since midnight when the card is in today's column, else null
  nowMinutes: { type: Number, default: null },
  dim: { type: Boolean, default: false },
  clash: { type: Boolean, default: false },
  draggable: { type: Boolean, default: false },
  // today's attendance: MANUAL / AUTO / NONE (null when not today)
  state: { type: String, default: null },
  // the substitute's name on this date, if any
  substitute: { type: String, default: null }
})
const emit = defineEmits(['open', 'dragstart', 'dragend'])

const STATE_ICON = { MANUAL: '✓', AUTO: '🤖', NONE: '⏳' }
const STATE_TITLE = {
  MANUAL: 'Davomat olingan',
  AUTO: 'Davomat avtomatik',
  NONE: 'Davomat hali olinmagan'
}

const color = computed(() =>
  subjectColor(props.lesson.subjectId, props.lesson.subjectColor)
)
const roomText = computed(() => roomLabel(props.lesson.room))
const lastName = computed(
  () => props.lesson.teacherShort || props.lesson.teacher
)
const now = computed(
  () =>
    props.nowMinutes !== null &&
    props.nowMinutes >= minutes(props.lesson.start) &&
    props.nowMinutes < minutes(props.lesson.end)
)
const past = computed(
  () =>
    props.nowMinutes !== null && props.nowMinutes >= minutes(props.lesson.end)
)
const left = computed(() =>
  Math.max(1, Math.ceil(minutes(props.lesson.end) - props.nowMinutes))
)

function onDragStart(e) {
  if (!props.draggable) return
  e.dataTransfer.effectAllowed = 'move'
  e.dataTransfer.setData('text/plain', String(props.lesson.id))
  emit('dragstart', props.lesson)
}
</script>

<style scoped>
.lc {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: var(--hairline);
  height: 100%;
  min-width: 0;
  padding: var(--space-1) var(--space-2) var(--space-1) var(--space-3);
  border-radius: var(--radius-sm);
  background: color-mix(in srgb, var(--c) var(--lesson-tint), var(--card-bg));
  border: 1px solid color-mix(in srgb, var(--c) 18%, transparent);
  cursor: pointer;
  overflow: hidden;
  transition:
    background var(--dur-fast) var(--ease-out),
    opacity var(--dur-fast) var(--ease-out),
    box-shadow var(--dur-fast) var(--ease-out),
    transform var(--dur-fast) var(--ease-out);
}

.lc::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: var(--tt-stripe);
  background: var(--c);
}

.lc:hover,
.lc:focus-visible {
  background: color-mix(in srgb, var(--c) var(--lesson-tint-hover), var(--card-bg));
  outline: none;
  box-shadow: var(--dash-shadow);
}

.lc--drag {
  cursor: grab;
}

.lc--past {
  opacity: 0.55;
}

.lc--dim {
  opacity: 0.18;
}

.lc--now {
  box-shadow:
    0 0 0 2px var(--c),
    0 0 18px var(--now-glow);
  z-index: 1;
}

.lc--clash {
  border-color: var(--st-absent);
  box-shadow: inset 0 0 0 1px var(--st-absent);
}

.lc__top {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  min-width: 0;
}

.lc__subject {
  flex: 1 1 auto;
  min-width: 0;
  font-size: var(--text-xs);
  font-weight: 700;
  line-height: 1.25;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.lc__state {
  font-size: var(--text-xs);
  line-height: 1;
}

.lc__meta {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  min-width: 0;
  color: var(--brand-text-muted);
  font-size: var(--text-xs);
  line-height: 1.2;
  white-space: nowrap;
  overflow: hidden;
}

.lc__who {
  overflow: hidden;
  text-overflow: ellipsis;
}

.lc__room,
.lc__class {
  flex-shrink: 0;
  padding: 0 var(--space-1);
  border-radius: var(--radius-sm);
  background: var(--surface-2);
  color: var(--text-primary);
  font-weight: 600;
}

.lc__now,
.lc__sub {
  font-size: var(--text-xs);
  font-weight: 700;
  color: var(--c);
  white-space: nowrap;
}

.lc__sub {
  color: var(--st-late);
}

@media (prefers-reduced-motion: reduce) {
  .lc {
    transition: none;
  }
}
</style>

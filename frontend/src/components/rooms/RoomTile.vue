<template>
  <button
    type="button"
    class="rt"
    :class="`rt--${r.state.toLowerCase()}`"
    :style="{ '--s': STATES[r.state].color, '--c': r.now?.subjectId ? subjectColor(r.now.subjectId, r.now.subjectColor) : 'var(--s)' }"
    :data-room="r.room.id"
    :data-state="r.state"
    :aria-label="aria"
    @click="emit('open', r)"
  >
    <span class="rt__head">
      <q-icon :name="ROOM_TYPES[r.room.type]?.icon || 'meeting_room'" class="rt__type" />
      <b class="rt__num">{{ r.room.number }}</b>
      <span class="rt__cap" title="Sig'imi"><q-icon name="person" />{{ r.room.capacity }}</span>
    </span>

    <template v-if="r.state === 'BUSY'">
      <span class="rt__line"><b>{{ r.now.className }}</b> · {{ r.now.subject }}</span>
      <span class="rt__sub">{{ r.now.teacher }}</span>
      <span class="rt__until">{{ hm(r.until) }} gacha</span>
      <span class="rt__progress"><span :style="{ width: `${r.progress || 0}%` }" /></span>
    </template>
    <template v-else-if="r.state === 'BOOKED'">
      <span class="rt__line"><b>{{ r.now.purpose }}</b></span>
      <span class="rt__sub">{{ r.now.teacher || 'band qilingan' }}</span>
      <span class="rt__until">{{ hm(r.until) }} gacha</span>
      <span class="rt__progress"><span :style="{ width: `${r.progress || 0}%` }" /></span>
    </template>
    <template v-else-if="r.state === 'FREE'">
      <span class="rt__line rt__free">Bo'sh</span>
      <span class="rt__until">{{ r.until ? `${hm(r.until)} gacha bo'sh` : 'kun oxirigacha bo\'sh' }}</span>
    </template>
    <template v-else>
      <span class="rt__line">Ta'mirda</span>
    </template>
  </button>
</template>

<script setup>
// A room on the live map: number, type, seats and what is happening now — busy (class,
// subject, teacher, until when, how much of the lesson is gone), booked, free (until the
// next lesson) or under repair (striped).
import { computed } from 'vue'
import { subjectColor } from '@/utils/subjectColor'
import { ROOM_TYPES, STATES, hm } from './rooms'

const props = defineProps({ r: { type: Object, required: true } })
const emit = defineEmits(['open'])

const aria = computed(() => {
  const n = props.r.room.number
  if (props.r.state === 'BUSY') return `${n}: band, ${props.r.now.className} ${props.r.now.subject}, ${hm(props.r.until)} gacha`
  if (props.r.state === 'FREE') return `${n}: bo'sh${props.r.until ? `, ${hm(props.r.until)} gacha` : ''}`
  return `${n}: ${STATES[props.r.state].label}`
})
</script>

<style scoped>
.rt {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: var(--hairline);
  width: var(--room-tile-w);
  min-height: var(--room-tile-h);
  padding: var(--space-2) var(--space-3) var(--space-3);
  border: 1px solid color-mix(in srgb, var(--s) 35%, transparent);
  border-radius: var(--radius-md);
  background: color-mix(in srgb, var(--s) 10%, var(--card-bg));
  color: var(--text-primary);
  font: inherit;
  text-align: left;
  cursor: pointer;
  overflow: hidden;
  transition:
    transform var(--dur-fast) var(--ease-out),
    box-shadow var(--dur-fast) var(--ease-out);
}

.rt::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: var(--accent-bar);
  background: var(--s);
}

.rt:hover,
.rt:focus-visible {
  transform: translateY(var(--lift));
  box-shadow: var(--dash-shadow-hover);
  outline: none;
}

.rt--busy {
  background: color-mix(in srgb, var(--c) 13%, var(--card-bg));
}

.rt--repair {
  background: repeating-linear-gradient(
    135deg,
    color-mix(in srgb, var(--s) 12%, var(--card-bg)) 0 8px,
    color-mix(in srgb, var(--s) 24%, var(--card-bg)) 8px 16px
  );
  color: var(--brand-text-muted);
}

.rt__head {
  display: flex;
  align-items: center;
  gap: var(--space-1);
}

.rt__type {
  color: var(--s);
  font-size: var(--text-md);
}

.rt__num {
  flex: 1 1 auto;
  min-width: 0;
  font-size: var(--text-md);
  font-weight: 800;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.rt__cap {
  display: inline-flex;
  align-items: center;
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
}

.rt__line {
  font-size: var(--text-xs);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.rt__sub {
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.rt__free {
  color: var(--room-free);
  font-weight: 700;
}

.rt__until {
  margin-top: auto;
  font-size: var(--text-xs);
  font-weight: 700;
  color: var(--s);
  font-variant-numeric: tabular-nums;
}

.rt__progress {
  height: var(--bar-h);
  border-radius: var(--radius-pill);
  background: var(--tile-none);
  overflow: hidden;
}

.rt__progress span {
  display: block;
  height: 100%;
  border-radius: var(--radius-pill);
  background: var(--c);
  transition: width var(--dur) linear;
}

@media (max-width: 599px) {
  .rt {
    width: 100%;
  }
}

@media (prefers-reduced-motion: reduce) {
  .rt,
  .rt__progress span {
    transition: none;
  }
}
</style>

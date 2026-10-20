<template>
  <article
    class="ec dash-card"
    :class="{ 'ec--away': row.status && row.status !== 'ACTIVE' }"
    tabindex="0"
    :data-employee-card="row.id"
    @click="emit('open')"
    @keydown.enter.self="emit('open')"
  >
    <header class="ec__head">
      <AvatarInitials :name="row.fullName" :id="row.id" size="md" gradient />
      <div class="ec__titles">
        <b class="ec__name">{{ row.fullName }}</b>
        <small>{{ row.positionTitle }}</small>
      </div>
      <!-- lessons a week against the norm, as a small ring -->
      <span
        v-if="row.weeklyLessons"
        class="ec__ring"
        :style="{ '--p': Math.min(100, (row.weeklyLessons / NORM) * 100) }"
        :title="`${row.weeklyLessons} soat / hafta (norma ${NORM})`"
        data-load-ring
      >
        <b class="tnum">{{ row.weeklyLessons }}</b>
      </span>
    </header>
    <div v-if="status" class="ec__status" :style="{ '--c': status.color }">
      <i />{{ status.text }}
    </div>
    <div v-if="subjects.length" class="ec__chips">
      <span
        v-for="s in subjects.slice(0, 3)"
        :key="s"
        class="ec__chip"
        :style="{ '--c': subjectColor(hash(s)) }"
        >{{ s }}</span
      >
      <span v-if="subjects.length > 3" class="ec__chip ec__chip--more"
        >+{{ subjects.length - 3 }}</span
      >
    </div>
    <footer class="ec__foot">
      <a
        v-if="row.phone"
        class="ec__phone tnum"
        :href="`tel:${String(row.phone).replace(/[^\d+]/g, '')}`"
        @click.stop
        ><q-icon name="call" /> {{ formatPhone(row.phone) }}</a
      >
      <span v-if="leads.length" class="ec__lead"
        ><q-icon name="groups" />
        {{ leads.length > 2 ? `${leads.length} ta sinf` : leads.join(', ') }}
        rahbari</span
      >
    </footer>
  </article>
</template>

<script setup>
// An employee as a card: avatar, name and position, lessons a week as a ring against the norm,
// leave or dismissal when not at work, the subjects (three, then "+N"), the phone to call and the
// class they lead.
import { computed } from 'vue'
import AvatarInitials from '@/components/pages/AvatarInitials.vue'
import { subjectColor } from '@/utils/subjectColor'
import { formatPhone } from '@/utils/tashkent'
import { listItems, formatDay } from '../crud'

const props = defineProps({ row: { type: Object, required: true } })
const emit = defineEmits(['open', 'edit', 'delete'])

const NORM = 18
const subjects = computed(() => listItems(props.row.subjects))
const leads = computed(() => listItems(props.row.classTeacherOf))
const status = computed(() => {
  const r = props.row
  if (r.status === 'ON_LEAVE')
    return {
      text: r.leaveTo ? `Ta'tilda · ${formatDay(r.leaveTo)} gacha` : "Ta'tilda",
      color: 'var(--st-late)'
    }
  if (r.status === 'DISMISSED')
    return { text: 'Ishdan ketgan', color: 'var(--st-unmarked)' }
  return null
})

function hash(name) {
  let h = 0
  for (const ch of String(name)) h = (h * 31 + ch.charCodeAt(0)) >>> 0
  return h
}
</script>

<style scoped>
.tnum {
  font-variant-numeric: tabular-nums;
}

.ec {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  padding: var(--space-4);
  cursor: pointer;
}

.ec:focus-visible {
  outline: var(--border-focus);
  outline-offset: var(--hairline);
}

.ec--away {
  opacity: 0.7;
}

.ec__head {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  min-width: 0;
}

.ec__titles {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-width: 0;
}

.ec__name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: var(--text-md);
}

.ec__titles small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--brand-text-muted);
}

.ec__ring {
  display: inline-flex;
  flex: none;
  align-items: center;
  justify-content: center;
  width: var(--space-8);
  height: var(--space-8);
  border-radius: var(--radius-pill);
  background:
    radial-gradient(circle, var(--card-bg) 58%, transparent 60%),
    conic-gradient(
      var(--chart-primary) calc(var(--p) * 1%),
      var(--ring-track) 0
    );
}

.ec__ring b {
  font-size: var(--text-sm);
  font-weight: 800;
}

.ec__status {
  display: inline-flex;
  align-items: center;
  align-self: flex-start;
  gap: var(--space-1);
  padding: var(--hairline) var(--space-2);
  border-radius: var(--radius-pill);
  background: color-mix(in srgb, var(--c) 14%, transparent);
  color: var(--c);
  font-size: var(--text-xs);
  font-weight: 700;
}

.ec__status i {
  width: var(--space-1);
  height: var(--space-1);
  border-radius: var(--radius-pill);
  background: var(--c);
}

.ec__chips {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-1);
}

.ec__chip {
  padding: var(--hairline) var(--space-2);
  border: var(--hairline) solid color-mix(in srgb, var(--c) 30%, transparent);
  border-radius: var(--radius-pill);
  background: color-mix(in srgb, var(--c) 12%, transparent);
  color: color-mix(in srgb, var(--c) 75%, var(--text-primary));
  font-size: var(--text-xs);
  font-weight: 600;
}

.ec__chip--more {
  --c: var(--c-primary);
}

.ec__foot {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-1) var(--space-3);
  margin-top: auto;
  padding-top: var(--space-3);
  border-top: var(--hairline) solid var(--dash-edge);
  font-size: var(--text-sm);
}

.ec__phone {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  color: var(--c-primary);
  font-weight: 600;
  text-decoration: none;
}

.ec__lead {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  color: var(--brand-text-muted);
}
</style>

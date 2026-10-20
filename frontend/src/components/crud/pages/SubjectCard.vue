<template>
  <article
    class="sc dash-card"
    :class="{ 'sc--off': row.active === false }"
    :style="{ '--c': color }"
    tabindex="0"
    :data-subject-card="row.id"
    @click="emit('open')"
    @keydown.enter.self="emit('open')"
  >
    <header class="sc__head">
      <IconTile
        :icon="subjectIcon(row)"
        size="48"
        :color="`color-mix(in srgb, ${color} 16%, transparent)`"
        :text-color="color"
      />
      <div class="sc__titles">
        <b class="sc__name"
          >{{ row.name
          }}<q-tooltip v-if="String(row.name || '').length > 28">{{
            row.name
          }}</q-tooltip></b
        >
        <small
          >{{ row.weeklyLessons ?? 0 }} soat / hafta ·
          {{ row.classCount ?? 0 }} sinf</small
        >
      </div>
      <span v-if="row.active === false" class="sc__off" data-inactive
        >Nofaol</span
      >
      <span class="sc__actions" @click.stop>
        <button
          v-if="canEdit"
          type="button"
          class="sc__btn"
          aria-label="Tahrirlash"
          data-action="edit"
          @click="emit('edit')"
        >
          <q-icon name="edit" />
        </button>
        <button
          v-if="canDelete && row.active !== false"
          type="button"
          class="sc__btn sc__btn--danger"
          aria-label="Nofaol qilish"
          data-action="delete"
          @click="emit('delete')"
        >
          <q-icon name="block" />
        </button>
      </span>
    </header>

    <div class="sc__people">
      <span class="sc__stack">
        <AvatarInitials
          v-for="t in (row.teachers || []).slice(0, 4)"
          :key="t.id"
          :name="t.name"
          :id="t.id"
          size="sm"
          :title="`${t.name} · ${t.lessons} soat`"
        />
      </span>
      <small class="sc__muted">{{
        row.teacherCount
          ? `${row.teacherCount} o'qituvchi`
          : "O'qituvchi biriktirilmagan"
      }}</small>
    </div>

    <footer class="sc__foot">
      <div class="sc__quality">
        <small class="sc__muted">Sifat (shu chorak)</small>
        <span class="sc__q">
          <b class="tnum" :style="{ color: qualityTone }">{{
            row.qualityNow == null ? '—' : `${Math.round(row.qualityNow)}%`
          }}</b>
          <DeltaPill
            :value="delta"
            percent
            :digits="0"
            title="O'tgan chorakka nisbatan"
          />
        </span>
      </div>
      <div class="sc__spark">
        <small class="sc__muted">O'rtacha baho, 30 kun</small>
        <Sparkline
          :values="row.averageSpark || []"
          :color="color"
          :min="2"
          :max="5"
          :label="`${row.name}: 30 kunlik o'rtacha`"
        />
      </div>
    </footer>
  </article>
</template>

<script setup>
// A subject as a card: its icon in its colour, name, hours a week and classes, the teachers as an
// avatar stack, quality this quarter against the last one (DeltaPill) and the 30-day average run.
// A retired subject is faded and says "Nofaol".
import { computed } from 'vue'
import IconTile from '@/components/app/IconTile.vue'
import AvatarInitials from '@/components/pages/AvatarInitials.vue'
import DeltaPill from '@/components/dashboard/DeltaPill.vue'
import Sparkline from '@/components/dashboard/Sparkline.vue'
import { subjectColor } from '@/utils/subjectColor'
import { subjectIcon } from '@/utils/subjectIcon'

const props = defineProps({
  row: { type: Object, required: true },
  canEdit: { type: Boolean, default: false },
  canDelete: { type: Boolean, default: false }
})
const emit = defineEmits(['open', 'edit', 'delete'])

const color = computed(() => subjectColor(props.row.id, props.row.color))
const delta = computed(() =>
  props.row.qualityNow == null || props.row.qualityPrev == null
    ? null
    : Math.round(props.row.qualityNow - props.row.qualityPrev)
)
const qualityTone = computed(() => {
  const q = props.row.qualityNow
  if (q == null) return 'var(--brand-text-muted)'
  return q >= 60
    ? 'var(--st-present)'
    : q >= 40
      ? 'var(--gr-3)'
      : 'var(--st-absent)'
})
</script>

<style scoped>
.tnum {
  font-variant-numeric: tabular-nums;
}

.sc {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  padding: var(--space-4);
  cursor: pointer;
  border-top: var(--space-1) solid var(--c);
}

.sc:focus-visible {
  outline: var(--border-focus);
  outline-offset: var(--hairline);
}

.sc--off {
  opacity: 0.55;
}

.sc__head {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  min-width: 0;
}

.sc__titles {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-width: 0;
}

.sc__name {
  display: -webkit-box;
  overflow: hidden;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  font-size: var(--text-md);
  line-height: 1.25;
  overflow-wrap: anywhere;
}

.sc__titles small,
.sc__muted {
  color: var(--brand-text-muted);
  font-size: var(--text-xs);
}

.sc__off {
  flex: none;
  padding: var(--hairline) var(--space-2);
  border-radius: var(--radius-pill);
  background: var(--surface-2);
  color: var(--brand-text-muted);
  font-size: var(--text-xs);
  font-weight: 700;
}

.sc__actions {
  display: inline-flex;
  flex: none;
  opacity: 0;
  transition: opacity var(--dur-fast) var(--ease-out);
}

.sc:hover .sc__actions,
.sc:focus-within .sc__actions {
  opacity: 1;
}

@media (hover: none) {
  .sc__actions {
    opacity: 1;
  }
}

.sc__btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: var(--control-h-sm);
  height: var(--control-h-sm);
  border: 0;
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--brand-text-muted);
  font-size: var(--text-md);
  cursor: pointer;
}

.sc__btn:hover {
  background: color-mix(in srgb, var(--c-primary) 10%, transparent);
  color: var(--c-primary);
}

.sc__btn--danger:hover {
  background: color-mix(in srgb, var(--st-absent) 10%, transparent);
  color: var(--st-absent);
}

.sc__people {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.sc__stack {
  display: inline-flex;
}

.sc__stack > * + * {
  margin-left: calc(var(--space-2) * -1);
}

/* a ring in the card's colour keeps overlapping avatars apart */
.sc__stack > * {
  box-shadow: 0 0 0 2px var(--card-bg);
}

.sc__foot {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: end;
  gap: var(--space-4);
  padding-top: var(--space-3);
  border-top: var(--hairline) solid var(--dash-edge);
}

.sc__quality,
.sc__spark {
  display: flex;
  flex-direction: column;
  gap: var(--hairline);
  min-width: 0;
}

.sc__q {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
}

.sc__q b {
  font-size: var(--text-lg);
  font-weight: 800;
}

.sc__spark :deep(.sparkline) {
  --spark-h: var(--space-6);
}
</style>

<template>
  <div class="ccards" data-crud-cards>
    <article
      v-for="(row, i) in rows"
      :key="row.id ?? i"
      class="ccard dash-card"
      :class="{
        'ccard--dim': card.dim?.(row),
        'ccard--sel': selected.has(row.id)
      }"
      :style="{ '--i': Math.min(i, 12) }"
      tabindex="0"
      :data-card-id="row.id"
      @click="emit('open', row)"
      @keydown.enter.self="emit('open', row)"
    >
      <header class="ccard__head">
        <AvatarInitials
          v-if="card.avatar"
          :name="String(card.title(row) || '')"
          :id="Number(row.id) || 0"
          size="md"
          gradient
        />
        <IconTile
          v-else
          :icon="card.icon?.(row) || icon"
          size="40"
          :color="`color-mix(in srgb, ${card.color?.(row) || color} 14%, transparent)`"
          :text-color="card.color?.(row) || color"
        />
        <div class="ccard__titles">
          <b class="ccard__title" :style="card.titleStyle?.(row)">{{
            card.title(row)
          }}</b>
          <small v-if="card.subtitle?.(row)" class="ccard__sub">{{
            card.subtitle(row)
          }}</small>
        </div>
        <span
          v-if="card.badge?.(row)"
          class="ccard__badge"
          :style="{ '--c': card.badge(row).color || 'var(--c-primary)' }"
          >{{ card.badge(row).label }}</span
        >
        <span class="ccard__actions" @click.stop>
          <button
            v-if="canEdit"
            type="button"
            class="ccard__btn"
            aria-label="Tahrirlash"
            data-action="edit"
            @click="emit('edit', row)"
          >
            <q-icon name="edit" />
            <q-tooltip>Tahrirlash</q-tooltip>
          </button>
          <button
            v-if="canDelete && row.active !== false"
            type="button"
            class="ccard__btn ccard__btn--danger"
            :aria-label="softDelete ? 'Nofaol qilish' : 'O\'chirish'"
            data-action="delete"
            @click="emit('delete', row)"
          >
            <q-icon :name="softDelete ? 'block' : 'delete_outline'" />
            <q-tooltip>{{
              softDelete ? 'Nofaol qilish' : "O'chirish"
            }}</q-tooltip>
          </button>
        </span>
      </header>
      <p v-if="card.text?.(row)" class="ccard__text">{{ card.text(row) }}</p>
      <div v-if="stats(row).length" class="ccard__stats">
        <div v-for="s in stats(row)" :key="s.label" class="ccard__stat">
          <b class="dash-num" :style="s.color ? { color: s.color } : null">{{
            s.value ?? '—'
          }}</b>
          <small>{{ s.label }}</small>
        </div>
      </div>
      <footer v-if="card.footer?.(row)" class="ccard__foot">
        <q-icon v-if="card.footerIcon" :name="card.footerIcon" />
        {{ card.footer(row) }}
      </footer>
    </article>
  </div>
</template>

<script setup>
// A list as cards (dashboard style: the faint edge and top light line, a lift on hover) in a grid
// that fills the width (minmax(280px, 1fr)). The module's `cards` config says what a card shows:
// title / subtitle / avatar or icon / badge / text / up to four numbers / a footer line.
import AvatarInitials from '@/components/pages/AvatarInitials.vue'
import IconTile from '@/components/app/IconTile.vue'

const props = defineProps({
  rows: { type: Array, required: true },
  card: { type: Object, required: true },
  icon: { type: String, default: 'description' },
  color: { type: String, default: 'var(--chart-primary)' },
  canEdit: { type: Boolean, default: false },
  canDelete: { type: Boolean, default: false },
  softDelete: { type: Boolean, default: false },
  selected: { type: Set, default: () => new Set() }
})
const emit = defineEmits(['open', 'edit', 'delete'])

const stats = row => (props.card.stats ? props.card.stats(row) : []).slice(0, 4)
</script>

<style scoped>
.ccards {
  display: grid;
  grid-template-columns: repeat(
    auto-fill,
    minmax(min(var(--crud-card-min), 100%), 1fr)
  );
  gap: var(--space-4);
}

.ccard {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  padding: var(--space-4);
  cursor: pointer;
}

.ccard:focus-visible {
  outline: var(--border-focus);
  outline-offset: var(--hairline);
}

.ccard--dim {
  opacity: 0.6;
}

.ccard--sel {
  border-color: var(--c-primary);
}

.ccard__head {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  min-width: 0;
}

.ccard__titles {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-width: 0;
}

.ccard__title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: var(--text-md);
}

.ccard__sub {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--brand-text-muted);
}

.ccard__badge {
  flex: none;
  padding: var(--hairline) var(--space-2);
  border-radius: var(--radius-pill);
  background: color-mix(in srgb, var(--c) 14%, transparent);
  color: var(--c);
  font-size: var(--text-xs);
  font-weight: 700;
}

.ccard__actions {
  display: inline-flex;
  flex: none;
  gap: var(--hairline);
  opacity: 0;
  transition: opacity var(--dur-fast) var(--ease-out);
}

.ccard:hover .ccard__actions,
.ccard:focus-within .ccard__actions {
  opacity: 1;
}

@media (hover: none) {
  .ccard__actions {
    opacity: 1;
  }
}

.ccard__btn {
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

.ccard__btn:hover {
  background: color-mix(in srgb, var(--c-primary) 10%, transparent);
  color: var(--c-primary);
}

.ccard__btn--danger:hover {
  background: color-mix(in srgb, var(--st-absent) 10%, transparent);
  color: var(--st-absent);
}

.ccard__text {
  margin: 0;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  color: var(--brand-text-muted);
  font-size: var(--text-sm);
}

.ccard__stats {
  display: grid;
  grid-auto-flow: column;
  grid-auto-columns: minmax(0, 1fr);
  gap: var(--space-2);
  padding: var(--space-3) 0 0;
  border-top: var(--hairline) solid var(--dash-edge);
}

.ccard__stat {
  display: flex;
  flex-direction: column;
  min-width: 0;
  text-align: center;
}

.ccard__stat b {
  font-size: var(--text-lg);
  font-variant-numeric: tabular-nums;
}

.ccard__stat small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--brand-text-muted);
  font-size: var(--text-xs);
}

.ccard__foot {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  margin-top: auto;
  color: var(--c-primary);
  font-size: var(--text-xs);
  font-weight: 600;
}
</style>

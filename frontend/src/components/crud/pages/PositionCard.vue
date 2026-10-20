<template>
  <article
    class="pc dash-card"
    :style="{ '--c': look.color }"
    tabindex="0"
    role="link"
    :aria-label="`${row.title}: xodimlar ro'yxati`"
    :data-position-card="row.id"
    @click="emit('open')"
    @keydown.enter.self="emit('open')"
  >
    <header class="pc__head">
      <IconTile
        :icon="look.icon"
        size="48"
        :color="`color-mix(in srgb, ${look.color} 16%, transparent)`"
        :text-color="look.color"
      />
      <div class="pc__titles">
        <b class="pc__name"
          >{{ row.title
          }}<q-tooltip v-if="String(row.title || '').length > 28">{{
            row.title
          }}</q-tooltip></b
        >
        <span
          v-if="row.defaultRole"
          class="pc__role"
          :class="`pc__role--${row.defaultRole.toLowerCase()}`"
          :title="`Yangi foydalanuvchiga taklif qilinadigan rol: ${ROLE[row.defaultRole]}`"
          data-role
          >{{ row.defaultRole }}</span
        >
      </div>
      <span class="pc__actions" @click.stop>
        <button
          v-if="canEdit"
          type="button"
          class="pc__btn"
          aria-label="Tahrirlash"
          data-action="edit"
          @click="emit('edit')"
        >
          <q-icon name="edit" />
        </button>
        <button
          v-if="canDelete"
          type="button"
          class="pc__btn pc__btn--danger"
          aria-label="O'chirish"
          data-action="delete"
          @click="emit('delete')"
        >
          <q-icon name="delete_outline" />
        </button>
      </span>
    </header>
    <p class="pc__desc">{{ row.description || 'Tavsif kiritilmagan' }}</p>
    <footer class="pc__foot">
      <span class="pc__stack">
        <AvatarInitials
          v-for="e in (row.employees || []).slice(0, 5)"
          :key="e.id"
          :name="e.name"
          :id="e.id"
          size="sm"
          :title="e.name"
        />
      </span>
      <b class="pc__count tnum" data-employee-count>{{
        row.employeeCount ?? 0
      }}</b>
      <small>xodim</small>
      <q-icon name="arrow_forward" class="pc__go" />
    </footer>
  </article>
</template>

<script setup>
// A position as a card: an icon and colour that fit the title, the name and the system role a new
// user of such an employee is offered (ADMIN / EDITOR / VIEWER), its description, how many people
// hold it with their avatars. The card opens Xodimlar filtered by this position.
import { computed } from 'vue'
import IconTile from '@/components/app/IconTile.vue'
import AvatarInitials from '@/components/pages/AvatarInitials.vue'
import { positionLook } from '@/utils/subjectIcon'

const props = defineProps({
  row: { type: Object, required: true },
  canEdit: { type: Boolean, default: false },
  canDelete: { type: Boolean, default: false }
})
const emit = defineEmits(['open', 'edit', 'delete'])

const ROLE = { ADMIN: 'administrator', EDITOR: 'muharrir', VIEWER: "ko'ruvchi" }
const look = computed(() => positionLook(props.row.title))
</script>

<style scoped>
.tnum {
  font-variant-numeric: tabular-nums;
}

.pc {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  padding: var(--space-4);
  cursor: pointer;
}

.pc:focus-visible {
  outline: var(--border-focus);
  outline-offset: var(--hairline);
}

.pc__head {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  min-width: 0;
}

.pc__titles {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  align-items: flex-start;
  gap: var(--hairline);
  min-width: 0;
}

.pc__name {
  max-width: 100%;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  font-size: var(--text-md);
  line-height: 1.25;
}

.pc__role {
  padding: 0 var(--space-2);
  border-radius: var(--radius-pill);
  background: var(--surface-2);
  color: var(--brand-text-muted);
  font-size: var(--text-xs);
  font-weight: 700;
  letter-spacing: 0.04em;
}

.pc__role--admin {
  background: color-mix(in srgb, var(--st-absent) 14%, transparent);
  color: var(--st-absent);
}

.pc__role--editor {
  background: color-mix(in srgb, var(--c-primary) 14%, transparent);
  color: var(--c-primary);
}

.pc__actions {
  display: inline-flex;
  flex: none;
  opacity: 0;
  transition: opacity var(--dur-fast) var(--ease-out);
}

.pc:hover .pc__actions,
.pc:focus-within .pc__actions {
  opacity: 1;
}

@media (hover: none) {
  .pc__actions {
    opacity: 1;
  }
}

.pc__btn {
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

.pc__btn:hover {
  background: color-mix(in srgb, var(--c-primary) 10%, transparent);
  color: var(--c-primary);
}

.pc__btn--danger:hover {
  background: color-mix(in srgb, var(--st-absent) 10%, transparent);
  color: var(--st-absent);
}

.pc__desc {
  margin: 0;
  min-height: calc(var(--text-sm) * 2.8);
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  color: var(--brand-text-muted);
  font-size: var(--text-sm);
}

.pc__foot {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding-top: var(--space-3);
  border-top: var(--hairline) solid var(--dash-edge);
}

.pc__stack {
  display: inline-flex;
}

.pc__stack > * + * {
  margin-left: calc(var(--space-2) * -1);
}

/* a ring in the card's colour keeps overlapping avatars apart */
.pc__stack > * {
  box-shadow: 0 0 0 2px var(--card-bg);
}

.pc__count {
  margin-left: auto;
  font-size: var(--text-lg);
  font-weight: 800;
  color: var(--c);
}

.pc__foot small {
  color: var(--brand-text-muted);
}

.pc__go {
  color: var(--brand-text-muted);
  transition: transform var(--dur-fast) var(--ease-out);
}

.pc:hover .pc__go {
  color: var(--c-primary);
  transform: translateX(var(--space-1));
}
</style>

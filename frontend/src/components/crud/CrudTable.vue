<template>
  <div
    class="ct"
    :class="{ 'ct--compact': density === 'compact', 'ct--loading': loading }"
    data-crud-table
  >
    <table class="ct__table" :aria-label="label" :aria-busy="loading">
      <thead>
        <tr>
          <th v-if="selectable" class="ct__check" scope="col">
            <input
              type="checkbox"
              :checked="allChecked"
              :indeterminate.prop="someChecked && !allChecked"
              aria-label="Sahifadagi hammasini tanlash"
              data-check-all
              @change="emit('select-all', $event.target.checked)"
            />
          </th>
          <th
            v-for="c in columns"
            :key="c.name"
            scope="col"
            :class="[
              `ct__th--${c.align || 'left'}`,
              { 'ct__th--sort': c.sortable }
            ]"
            :style="c.width ? { width: c.width } : null"
            :aria-sort="
              sortBy === (c.sortKey || c.name)
                ? descending
                  ? 'descending'
                  : 'ascending'
                : undefined
            "
            :data-col="c.name"
            @click="c.sortable && emit('sort', c.sortKey || c.name)"
          >
            <span class="ct__th-inner">
              {{ c.label }}
              <q-icon
                v-if="c.sortable"
                class="ct__sort"
                :class="{
                  'ct__sort--on': sortBy === (c.sortKey || c.name)
                }"
                :name="
                  sortBy === (c.sortKey || c.name) && descending
                    ? 'arrow_downward'
                    : 'arrow_upward'
                "
              />
            </span>
          </th>
          <th class="ct__actions" scope="col" aria-label="Amallar" />
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="(row, i) in rows"
          :key="row.id ?? i"
          :class="{ 'ct__row--sel': selected.has(row.id) }"
          :data-row-id="row.id"
          tabindex="0"
          @click="emit('row', row)"
          @keydown.enter.self="emit('row', row)"
        >
          <td v-if="selectable" class="ct__check" @click.stop>
            <input
              type="checkbox"
              :checked="selected.has(row.id)"
              :aria-label="`Tanlash: ${rowTitle(row)}`"
              data-check
              @change="
                emit('select', {
                  row,
                  on: $event.target.checked,
                  shift: lastShift
                })
              "
              @click="lastShift = $event.shiftKey"
            />
          </td>
          <td
            v-for="(c, ci) in columns"
            :key="c.name"
            :class="[
              `ct__td--${c.align || 'left'}`,
              { 'ct__td--first': ci === 0 }
            ]"
            :style="c.width ? { maxWidth: c.width } : null"
          >
            <CrudCell
              :col="c"
              :row="row"
              :today="today"
              @profile="(link, r) => emit('profile', link, r)"
            />
          </td>
          <td class="ct__actions" @click.stop>
            <span class="ct__btns">
              <button
                v-if="canEdit"
                type="button"
                class="ct__btn"
                :class="{ 'ct__btn--off': auth.isGuest }"
                aria-label="Tahrirlash"
                :aria-disabled="auth.isGuest || undefined"
                data-action="edit"
                @click="act('edit', row)"
              >
                <q-icon name="edit" />
                <q-tooltip>{{
                  auth.isGuest ? GUEST_READ_ONLY : 'Tahrirlash'
                }}</q-tooltip>
              </button>
              <button
                v-if="
                  !menuItems(row).length && canDelete && row.active !== false
                "
                type="button"
                class="ct__btn ct__btn--danger"
                :class="{ 'ct__btn--off': auth.isGuest }"
                :aria-label="deleteLabel"
                :aria-disabled="auth.isGuest || undefined"
                data-action="delete"
                @click="act('delete', row)"
              >
                <q-icon :name="softDelete ? 'block' : 'delete_outline'" />
                <q-tooltip>{{
                  auth.isGuest ? GUEST_READ_ONLY : deleteLabel
                }}</q-tooltip>
              </button>
              <button
                v-if="menuItems(row).length"
                type="button"
                class="ct__btn"
                aria-label="Boshqa amallar"
                data-action="more"
              >
                <q-icon name="more_horiz" />
                <q-tooltip>Boshqa amallar</q-tooltip>
                <q-menu anchor="bottom right" self="top right" auto-close>
                  <q-list dense class="ct__menu">
                    <q-item
                      v-for="m in menuItems(row)"
                      :key="m.key"
                      clickable
                      :class="{ 'text-negative': m.danger }"
                      :data-menu="m.key"
                      @click="emit('action', { key: m.key, row })"
                    >
                      <q-item-section avatar
                        ><q-icon :name="m.icon"
                      /></q-item-section>
                      <q-item-section>{{ m.label }}</q-item-section>
                    </q-item>
                  </q-list>
                </q-menu>
              </button>
            </span>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
// A list as a table: a sticky header (click a sortable one to sort), check boxes for a bulk
// action, rows that open the record (Enter too), cells by type (CrudCell), and on the right a
// sticky actions column no wider than 88 px: edit and delete, or edit and "⋯" when the module has
// more (delete then goes into the menu). Comfortable or compact rows.
import { ref, computed } from 'vue'
import CrudCell from './CrudCell.vue'
import { useAuthStore } from '@/stores/auth'
import { guestBlocked, GUEST_READ_ONLY } from '@/utils/guest'

const props = defineProps({
  columns: { type: Array, required: true },
  rows: { type: Array, required: true },
  loading: { type: Boolean, default: false },
  selectable: { type: Boolean, default: false },
  selected: { type: Set, default: () => new Set() },
  sortBy: { type: String, default: null },
  descending: { type: Boolean, default: false },
  density: { type: String, default: 'comfortable' },
  canEdit: { type: Boolean, default: false },
  canDelete: { type: Boolean, default: false },
  softDelete: { type: Boolean, default: false },
  // row → [{ key, label, icon, danger }] — the module's own row actions
  extraActions: { type: Function, default: () => [] },
  today: { type: String, required: true },
  label: { type: String, default: '' },
  titleOf: { type: Function, default: null }
})
// the demo visitor sees the row buttons, but they only say why nothing happens
const auth = useAuthStore()
function act(kind, row) {
  if (guestBlocked()) return
  emit(kind, row)
}

const emit = defineEmits([
  'sort',
  'row',
  'edit',
  'delete',
  'action',
  'select',
  'select-all',
  'profile'
])
const lastShift = ref(false)

const deleteLabel = computed(() =>
  props.softDelete ? 'Nofaol qilish' : "O'chirish"
)
const allChecked = computed(
  () => props.rows.length > 0 && props.rows.every(r => props.selected.has(r.id))
)
const someChecked = computed(() =>
  props.rows.some(r => props.selected.has(r.id))
)

function menuItems(row) {
  const extra = props.extraActions(row) || []
  if (!extra.length) return []
  const del =
    props.canDelete && row.active !== false
      ? [
          {
            key: 'delete',
            label: deleteLabel.value,
            icon: props.softDelete ? 'block' : 'delete_outline',
            danger: true
          }
        ]
      : []
  return [...extra, ...del]
}

function rowTitle(row) {
  return props.titleOf ? props.titleOf(row) : String(row.id)
}
</script>

<style scoped>
.ct {
  position: relative;
  max-height: var(--crud-table-max-h);
  overflow: auto;
  scrollbar-width: thin;
}

/* a phone scrolls the page, not a box inside it */
@media (max-width: 599px) {
  .ct {
    max-height: none;
  }
}

.ct--loading tbody {
  opacity: 0.55;
  transition: opacity var(--dur-fast) var(--ease-out);
}

.ct__table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
  font-size: var(--text-sm);
}

th,
td {
  padding: 0 var(--space-3);
  height: var(--crud-row-h);
  border-bottom: var(--hairline) solid var(--dash-edge);
  text-align: left;
  vertical-align: middle;
  white-space: nowrap;
}

.ct--compact th,
.ct--compact td {
  height: var(--crud-row-h-compact);
  padding: 0 var(--space-2);
}

thead th {
  position: sticky;
  top: 0;
  z-index: 2;
  height: var(--control-h);
  background: var(--card-bg);
  color: var(--brand-text-muted);
  font-size: var(--text-xs);
  font-weight: 600;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  box-shadow: inset 0 calc(var(--hairline) * -1) 0 var(--brand-border);
}

.ct__th--sort {
  cursor: pointer;
  user-select: none;
}

.ct__th--sort:hover {
  color: var(--text-primary);
}

.ct__th-inner {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
}

.ct__th--right .ct__th-inner {
  justify-content: flex-end;
}

.ct__sort {
  font-size: var(--text-sm);
  opacity: 0;
  transition: opacity var(--dur-fast) var(--ease-out);
}

.ct__th--sort:hover .ct__sort,
.ct__sort--on {
  opacity: 1;
  color: var(--c-primary);
}

.ct__td--first {
  font-weight: 600;
}

.ct__th--right,
.ct__td--right {
  text-align: right;
}

.ct__th--center,
.ct__td--center {
  text-align: center;
}

td {
  overflow: hidden;
  text-overflow: ellipsis;
}

tbody tr {
  cursor: pointer;
  transition: background var(--dur-fast) var(--ease-out);
}

/* a very light zebra; the hovered and the checked rows lit */
tbody tr:nth-child(even) td {
  background: var(--crud-zebra);
}

tbody tr:hover td {
  background: var(--crud-row-hover);
}

tbody tr.ct__row--sel td {
  background: color-mix(in srgb, var(--c-primary) 9%, var(--card-bg));
}

tbody tr:focus-visible {
  outline: var(--border-focus);
  outline-offset: calc(var(--hairline) * -2);
}

.ct__check {
  width: var(--space-7);
  padding-right: 0;
}

.ct__check input {
  width: var(--space-4);
  height: var(--space-4);
  accent-color: var(--c-primary);
  cursor: pointer;
}

/* native check boxes follow the theme (no white squares in dark) */
:global(.body--dark) .ct__check input {
  color-scheme: dark;
}

/* the actions: sticky on the right, never wider than 88 px */
.ct__actions {
  position: sticky;
  right: 0;
  z-index: 1;
  width: var(--crud-actions-w);
  min-width: var(--crud-actions-w);
  max-width: var(--crud-actions-w);
  padding: 0 var(--space-2);
  background: var(--card-bg);
  box-shadow: inset var(--hairline) 0 0 var(--dash-edge);
  text-align: right;
}

thead .ct__actions {
  z-index: 3;
}

.ct__btns {
  display: inline-flex;
  gap: var(--space-1);
}

.ct__btn--off {
  opacity: 0.4;
  cursor: not-allowed;
}

.ct__btn {
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
  transition:
    background var(--dur-fast) var(--ease-out),
    color var(--dur-fast) var(--ease-out);
}

.ct__btn:hover,
.ct__btn:focus-visible {
  background: color-mix(in srgb, var(--c-primary) 10%, transparent);
  color: var(--c-primary);
  outline: none;
}

.ct__btn--danger:hover,
.ct__btn--danger:focus-visible {
  background: color-mix(in srgb, var(--st-absent) 10%, transparent);
  color: var(--st-absent);
}

.ct__menu {
  min-width: calc(var(--space-8) * 4);
}
</style>

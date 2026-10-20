<template>
  <div class="cf" data-crud-filters>
    <template v-for="f in filters" :key="f.key">
      <div v-if="f.picker === 'class'" class="cf-row cf-row--picker">
        <ClassPicker
          compact
          allow-all
          :classes="classes"
          :model-value="numberOrNull(values[f.key])"
          @update:model-value="v => set(f.key, v)"
        />
      </div>
      <div
        v-else-if="optionsOf(f).length"
        class="cf-row"
        role="group"
        :aria-label="f.label"
        :data-filter="f.key"
      >
        <span class="cf-label">{{ f.label }}</span>
        <div class="cf-chips">
          <button
            v-for="o in optionsOf(f)"
            :key="o.value"
            type="button"
            class="cf-chip"
            :class="{
              'cf-chip--on': String(values[f.key]) === String(o.value)
            }"
            :aria-pressed="String(values[f.key]) === String(o.value)"
            :data-chip="o.value"
            @click="toggle(f.key, o.value)"
          >
            {{ o.label }}
            <b v-if="o.count != null" class="cf-count">{{ o.count }}</b>
          </button>
        </div>
      </div>
    </template>
    <div
      v-if="toggles.length"
      class="cf-row"
      role="group"
      aria-label="Ko'rinish"
    >
      <button
        v-for="t in toggles"
        :key="t.key"
        type="button"
        class="cf-chip"
        :class="{ 'cf-chip--on': !!values[t.key] }"
        :aria-pressed="!!values[t.key]"
        :data-toggle="t.key"
        @click="set(t.key, values[t.key] ? null : 'true')"
      >
        <q-icon
          :name="values[t.key] ? 'check_box' : 'check_box_outline_blank'"
        />
        {{ t.label }}
      </button>
    </div>
    <button
      v-if="active"
      type="button"
      class="cf-clear"
      data-filters-clear
      @click="emit('clear')"
    >
      <q-icon name="close" /> tozalash
    </button>
  </div>
</template>

<script setup>
// The filters of a list as chips: a row per filter ("Holat: Ishda 36 · Ta'tilda 2"), one chip
// on at a time, the numbers from the module's facets endpoint; a class filter is the class picker
// with "Barchasi"; toggles (e.g. "Nofaollarni ko'rsatish") are chips too; "× tozalash" when any
// is on. The values live in the page (and its URL); this only shows and changes them.
import { computed } from 'vue'
import ClassPicker from '@/components/ClassPicker.vue'

const props = defineProps({
  filters: { type: Array, default: () => [] },
  toggles: { type: Array, default: () => [] },
  // key → value (strings, as in the URL)
  values: { type: Object, required: true },
  // key → [{ value, label }] for filters whose options come from an endpoint
  options: { type: Object, default: () => ({}) },
  // key → [{ value, label, count }] from the facets endpoint
  facets: { type: Object, default: () => ({}) },
  classes: { type: Array, default: () => [] }
})
const emit = defineEmits(['change', 'clear'])

const active = computed(() =>
  Object.values(props.values).some(v => v != null && v !== '')
)

const numberOrNull = v => (v == null || v === '' ? null : Number(v))

/** A filter's chips: its options with the facet numbers (an option with 0 rows is left out). */
function optionsOf(f) {
  const facet = props.facets[f.key]
  const base = f.options || props.options[f.key] || []
  if (!facet) return base
  const counts = Object.fromEntries(facet.map(x => [String(x.value), x.count]))
  if (!base.length) return facet.filter(x => x.count > 0)
  return base
    .map(o => ({ ...o, count: counts[String(o.value)] ?? 0 }))
    .filter(o => o.count > 0 || String(props.values[f.key]) === String(o.value))
}

function set(key, value) {
  emit('change', { key, value: value == null ? null : String(value) })
}

function toggle(key, value) {
  set(key, String(props.values[key]) === String(value) ? null : value)
}
</script>

<style scoped>
.cf {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-2) var(--space-4);
}

.cf-row {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  min-width: 0;
  max-width: 100%;
}

/* a long row of chips scrolls sideways instead of wrapping into many lines */
.cf-row[data-filter] {
  flex: 0 1 auto;
}

.cf-row--picker {
  flex: 1 1 100%;
}

.cf-label {
  flex: none;
  font-size: var(--text-xs);
  font-weight: 600;
  color: var(--brand-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.cf-chips {
  display: flex;
  gap: var(--space-1);
  min-width: 0;
  overflow-x: auto;
  scrollbar-width: thin;
  padding-bottom: var(--hairline);
}

.cf-chip {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  flex: none;
  height: var(--control-h-sm);
  padding: 0 var(--space-3);
  border: 1px solid var(--c-border-control);
  border-radius: var(--radius-pill);
  background: transparent;
  color: var(--text-primary);
  font: inherit;
  font-size: var(--text-sm);
  font-weight: 500;
  cursor: pointer;
  transition:
    background var(--dur-fast) var(--ease-out),
    border-color var(--dur-fast) var(--ease-out),
    color var(--dur-fast) var(--ease-out);
}

.cf-chip:hover {
  border-color: var(--c-primary);
  background: color-mix(in srgb, var(--c-primary) 8%, transparent);
}

.cf-chip--on,
.cf-chip--on:hover {
  border-color: var(--c-primary);
  background: var(--c-primary);
  color: white;
}

.cf-count {
  min-width: var(--space-4);
  padding: 0 var(--space-1);
  border-radius: var(--radius-pill);
  background: color-mix(in srgb, currentColor 14%, transparent);
  font-size: var(--text-xs);
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  text-align: center;
}

.cf-clear {
  display: inline-flex;
  align-items: center;
  gap: var(--hairline);
  height: var(--control-h-sm);
  padding: 0 var(--space-2);
  border: 0;
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--brand-text-muted);
  font: inherit;
  font-size: var(--text-sm);
  cursor: pointer;
}

.cf-clear:hover {
  color: var(--st-absent);
  background: color-mix(in srgb, var(--st-absent) 8%, transparent);
}
</style>

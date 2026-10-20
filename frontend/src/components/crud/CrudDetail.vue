<template>
  <div class="cdet" data-crud-detail>
    <div class="cdet__hero">
      <AvatarInitials
        v-if="avatar"
        :name="title"
        :id="Number(row.id) || 0"
        size="xl"
        gradient
      />
      <IconTile v-else :icon="icon" size="48" :color="color" />
      <div class="cdet__titles">
        <h2>{{ title }}</h2>
        <small v-if="subtitle">{{ subtitle }}</small>
      </div>
    </div>
    <section
      v-for="s in sections"
      :key="s.title"
      class="cdet__section"
      :data-detail-section="s.title"
    >
      <h4>{{ s.title }}</h4>
      <dl class="cdet__list">
        <template v-for="item in s.items" :key="item.key">
          <dt>{{ item.label }}</dt>
          <dd :data-detail="item.key">
            <CrudCell
              v-if="item.col"
              :col="item.col"
              :row="row"
              :today="today"
              @profile="(link, r) => emit('profile', link, r)"
            />
            <span v-else-if="item.text">{{ item.text }}</span>
            <span v-else class="cdet__empty">—</span>
          </dd>
        </template>
      </dl>
    </section>
    <slot />
  </div>
</template>

<script setup>
// One record read in the side panel (a row of a list was clicked): its name, then sections of
// label → value. The module's `detail` config gives the sections; without it, every column of
// the table (also the hidden ones, e.g. a student's address) and then the form's other fields.
import { computed } from 'vue'
import AvatarInitials from '@/components/pages/AvatarInitials.vue'
import IconTile from '@/components/app/IconTile.vue'
import CrudCell from './CrudCell.vue'
import { formatDay } from './crud'
import { formatPhone } from '@/utils/tashkent'

const props = defineProps({
  module: { type: Object, required: true },
  row: { type: Object, required: true },
  // field key → [{ value, label }] (the form's select options, for their labels)
  options: { type: Object, default: () => ({}) },
  today: { type: String, required: true }
})
const emit = defineEmits(['profile'])

const icon = computed(() => props.module.icon)
const color = computed(() => props.module.color)
const avatar = computed(
  () =>
    !!props.module.cards?.avatar ||
    props.module.columns.some(c => c.type === 'person')
)
const title = computed(
  () =>
    props.module.titleOf?.(props.row) ??
    String(props.row.name ?? props.row.title ?? props.row.fullName ?? '')
)
const subtitle = computed(() => props.module.detailSubtitle?.(props.row) || '')

function fieldText(f) {
  const v = props.row[f.key]
  if (v == null || v === '') return ''
  if (f.type === 'date') return formatDay(v)
  if (f.type === 'phone') return formatPhone(v)
  if (f.type === 'password') return ''
  if (f.type === 'select') {
    const opts = props.options[f.key] || f.options || []
    const label = vv => {
      const o = opts.find(o => (typeof o === 'object' ? o.value : o) === vv)
      return o == null ? String(vv) : typeof o === 'object' ? o.label : o
    }
    return Array.isArray(v) ? v.map(label).join(', ') : label(v)
  }
  return String(v)
}

const sections = computed(() => {
  if (props.module.detail) {
    return props.module.detail.map(s => ({
      title: s.title,
      items: s.items.map(it => ({
        key: it.key || it.label,
        label: it.label,
        col: it.col ? { name: it.key, ...it.col } : null,
        text: it.value ? it.value(props.row) : ''
      }))
    }))
  }
  const cols = props.module.columns.filter(
    c => c.name !== 'id' && c.type !== 'person'
  )
  // a form field already shown by a column (same key or same label) is not repeated, nor are
  // first / last name when the name is the title
  const covered = new Set(
    cols.flatMap(c => [c.formKey || c.field || c.name, c.label])
  )
  const named = props.module.columns.some(c => c.type === 'person')
  const fields = props.module.fields.filter(
    f =>
      !f.autoSchool &&
      f.type !== 'password' &&
      !covered.has(f.key) &&
      !covered.has(f.label) &&
      !(named && ['firstName', 'lastName'].includes(f.key)) &&
      !String(f.key).endsWith('RegionId')
  )
  const out = [
    {
      title: "Ma'lumotlar",
      items: cols.map(c => ({ key: c.name, label: c.label, col: c }))
    }
  ]
  const extra = fields
    .map(f => ({ key: f.key, label: f.label, text: fieldText(f) }))
    .filter(it => it.text)
  if (extra.length) out.push({ title: "Qo'shimcha", items: extra })
  return out
})
</script>

<style scoped>
.cdet {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}

.cdet__hero {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}

.cdet__titles {
  min-width: 0;
}

.cdet__titles h2 {
  margin: 0;
  font-size: var(--text-lg);
  font-weight: 700;
  line-height: 1.25;
}

.cdet__titles small {
  color: var(--brand-text-muted);
}

.cdet__section h4 {
  margin: 0 0 var(--space-2);
  font-size: var(--text-xs);
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--brand-text-muted);
}

.cdet__list {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(0, 3fr);
  margin: 0;
  border: var(--hairline) solid var(--dash-edge);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.cdet__list dt,
.cdet__list dd {
  margin: 0;
  padding: var(--space-2) var(--space-3);
  border-bottom: var(--hairline) solid var(--dash-edge);
  font-size: var(--text-sm);
  min-width: 0;
  overflow-wrap: anywhere;
}

.cdet__list dt {
  background: var(--surface-2);
  color: var(--brand-text-muted);
}

.cdet__list dt:nth-last-of-type(1),
.cdet__list dd:nth-last-of-type(1) {
  border-bottom: 0;
}

.cdet__empty {
  color: var(--brand-text-muted);
}
</style>

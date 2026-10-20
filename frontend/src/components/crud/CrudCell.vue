<template>
  <!-- one cell of a list: plain elements, one small component at most (a table of 100 rows stays light) -->
  <span v-if="col.type === 'person'" class="cc-person">
    <AvatarInitials
      :name="String(value || '')"
      :id="Number(row.id) || 0"
      size="sm"
    />
    <span class="cc-person__text">
      <a
        v-if="col.link"
        class="cc-person__name cc-link"
        :href="profileHref"
        data-person-link
        @click.stop.prevent="emit('profile', col.link, row)"
        >{{ value }}</a
      >
      <b v-else class="cc-person__name">{{ value }}</b>
      <small v-if="sub" class="cc-person__sub">{{ sub }}</small>
    </span>
  </span>

  <span
    v-else-if="col.type === 'badge' && value != null && value !== ''"
    class="cc-badge"
    :style="{ '--c': col.badgeColors?.[value] || 'var(--st-unmarked)' }"
    :data-badge="value"
  >
    <i class="cc-badge__dot" />{{ text }}
    <small v-if="hint" class="cc-badge__hint">{{ hint }}</small>
  </span>

  <span v-else-if="col.type === 'chip' && text" class="cc-chip">{{
    text
  }}</span>

  <span v-else-if="col.type === 'phone' && value" class="cc-phone">
    <a
      :href="`tel:${String(value).replace(/[^\d+]/g, '')}`"
      class="cc-link tnum"
      data-phone
      @click.stop
      >{{ text }}</a
    >
    <button
      type="button"
      class="cc-copy"
      :aria-label="`${text} — nusxalash`"
      data-copy
      @click.stop="copy"
    >
      <q-icon name="content_copy" />
      <q-tooltip>Nusxalash</q-tooltip>
    </button>
  </span>

  <span
    v-else-if="col.type === 'percent' && value != null"
    class="cc-pct"
    :style="{ '--c': pctColor }"
  >
    <span class="cc-pct__bar"
      ><i :style="{ width: `${Math.max(0, Math.min(100, value))}%` }"
    /></span>
    <b class="tnum">{{ text }}</b>
  </span>

  <span
    v-else-if="col.type === 'grade' && value != null"
    class="cc-grade"
    :style="{ '--c': avgColor(value) }"
  >
    <b class="cc-grade__pill tnum">{{ text }}</b>
    <span
      v-if="trend"
      class="cc-grade__trend"
      :class="`cc-grade__trend--${trend}`"
      :title="trend === 'up' ? 'Yaxshilanmoqda' : 'Pasaymoqda'"
      >{{ trend === 'up' ? '↗' : '↘' }}</span
    >
  </span>

  <span
    v-else-if="(col.type === 'subjects' || col.type === 'list') && items.length"
    class="cc-list"
  >
    <template v-if="items.length <= max">
      <span
        v-for="it in items"
        :key="it"
        class="cc-chip"
        :class="{ 'cc-chip--subject': col.type === 'subjects' }"
        :style="
          col.type === 'subjects' ? { '--c': subjectColor(hash(it)) } : null
        "
        >{{ it }}</span
      >
    </template>
    <template v-else>
      <template v-if="col.type === 'subjects'">
        <span
          v-for="it in items.slice(0, max)"
          :key="it"
          class="cc-chip cc-chip--subject"
          :style="{ '--c': subjectColor(hash(it)) }"
          >{{ it }}</span
        >
      </template>
      <button
        type="button"
        class="cc-chip cc-chip--more"
        :data-more="items.length"
        @click.stop
      >
        {{
          col.type === 'subjects'
            ? `+${items.length - max}`
            : `${items.length} ta ${col.listLabel || ''}`.trim()
        }}
        <q-menu anchor="bottom left" self="top left" class="cc-pop">
          <div class="cc-pop__body">
            <div class="cc-pop__title"
              >{{ col.label }} ({{ items.length }})</div
            >
            <div class="cc-pop__items">
              <span
                v-for="it in items"
                :key="it"
                class="cc-chip"
                :class="{ 'cc-chip--subject': col.type === 'subjects' }"
                :style="
                  col.type === 'subjects'
                    ? { '--c': subjectColor(hash(it)) }
                    : null
                "
                >{{ it }}</span
              >
            </div>
          </div>
        </q-menu>
      </button>
    </template>
  </span>

  <Sparkline
    v-else-if="col.type === 'spark' && Array.isArray(value) && value.length > 1"
    class="cc-spark"
    :values="value"
    :color="col.color || 'var(--chart-primary)'"
    :label="col.label"
  />

  <span v-else-if="col.type === 'avatars' && avatars.length" class="cc-avatars">
    <AvatarInitials
      v-for="a in avatars.slice(0, 4)"
      :key="a.id ?? a.name"
      :name="a.name"
      :id="Number(a.id) || 0"
      size="xs"
      :title="a.name"
    />
    <small v-if="avatars.length > 4" class="cc-avatars__more"
      >+{{ avatars.length - 4 }}</small
    >
  </span>

  <span
    v-else-if="col.type === 'bool' && value != null"
    class="cc-bool"
    :class="value ? 'cc-bool--yes' : 'cc-bool--no'"
    :aria-label="value ? 'Ha' : 'Yo\'q'"
    ><q-icon :name="value ? 'check_circle' : 'cancel'"
  /></span>

  <a
    v-else-if="col.link && text"
    class="cc-link"
    :href="profileHref"
    @click.stop.prevent="emit('profile', col.link, row)"
    >{{ text }}</a
  >

  <span
    v-else-if="text"
    :class="{ tnum: ['date', 'age', 'number'].includes(col.type) }"
    >{{ text }}</span
  >
  <span v-else class="cc-empty">—</span>
</template>

<script setup>
// One cell by its column's type (config/modules.js): person (avatar, name, a small line under
// it), badge, chip, phone (tel: link, copy), percent (bar + number), grade (pill + trend),
// subjects (chips, at most `max` then "+N"), list ("18 ta sinf" with the whole list in a popover),
// spark, avatars, bool, date / age / number / text. The words come from crud.js#cellText.
import { computed } from 'vue'
import { useQuasar, copyToClipboard } from 'quasar'
import AvatarInitials from '@/components/pages/AvatarInitials.vue'
import Sparkline from '@/components/dashboard/Sparkline.vue'
import { avgColor } from '@/components/journal/journal'
import { subjectColor } from '@/utils/subjectColor'
import { cellText, listItems, rawValue } from './crud'

const props = defineProps({
  col: { type: Object, required: true },
  row: { type: Object, required: true },
  today: { type: String, required: true }
})
const emit = defineEmits(['profile'])
const $q = useQuasar()

const value = computed(() => rawValue(props.col, props.row))
const text = computed(() => cellText(props.col, props.row, props.today))
const sub = computed(() => props.col.sub?.(props.row) || '')
const hint = computed(() => props.col.badgeHint?.(props.row) || '')
const max = computed(() => props.col.max ?? 3)
const items = computed(() => listItems(value.value))
const trend = computed(() => props.col.trend?.(props.row) || null)
const avatars = computed(() =>
  (Array.isArray(value.value) ? value.value : []).map(a =>
    typeof a === 'string' ? { name: a } : a
  )
)
const profileHref = computed(() =>
  props.col.link
    ? `#/profiles/${props.col.link.type}/${props.row[props.col.link.idField]}`
    : undefined
)
const pctColor = computed(() => {
  const v = value.value
  const [good, warn] = props.col.thresholds || []
  if (good == null) return 'var(--chart-primary)'
  return v >= good
    ? 'var(--st-present)'
    : v >= warn
      ? 'var(--gr-3)'
      : 'var(--st-absent)'
})

// a subject without an id in the row gets a steady colour from its name
function hash(name) {
  let h = 0
  for (const ch of String(name)) h = (h * 31 + ch.charCodeAt(0)) >>> 0
  return h
}

async function copy() {
  try {
    await copyToClipboard(text.value)
    $q.notify({ type: 'positive', message: 'Nusxalandi ✓', timeout: 1200 })
  } catch {
    $q.notify({ type: 'warning', message: "Nusxalab bo'lmadi" })
  }
}
</script>

<style scoped>
.tnum {
  font-variant-numeric: tabular-nums;
}

.cc-empty {
  color: var(--brand-text-muted);
}

.cc-link {
  color: inherit;
  text-decoration: none;
}

.cc-link:hover {
  color: var(--c-primary);
  text-decoration: underline;
}

.cc-person {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  min-width: 0;
  max-width: 100%;
}

.cc-person__text {
  display: flex;
  flex-direction: column;
  min-width: 0;
  line-height: 1.25;
}

.cc-person__name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 600;
  color: var(--text-primary);
}

.cc-person__sub {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
}

.cc-badge {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  padding: var(--hairline) var(--space-2);
  border-radius: var(--radius-pill);
  background: color-mix(in srgb, var(--c) 14%, transparent);
  color: color-mix(in srgb, var(--c) 82%, var(--text-primary));
  font-size: var(--text-xs);
  font-weight: 600;
  white-space: nowrap;
}

.cc-badge__dot {
  width: var(--space-1);
  height: var(--space-1);
  border-radius: var(--radius-pill);
  background: var(--c);
}

.cc-badge__hint {
  font-weight: 500;
  opacity: 0.8;
}

.cc-chip {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  max-width: 100%;
  padding: var(--hairline) var(--space-2);
  border: var(--hairline) solid var(--brand-border);
  border-radius: var(--radius-pill);
  background: var(--surface-2);
  color: var(--text-primary);
  font: inherit;
  font-size: var(--text-xs);
  font-weight: 600;
  line-height: 1.5;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.cc-chip--subject {
  border-color: color-mix(in srgb, var(--c) 30%, transparent);
  background: color-mix(in srgb, var(--c) 12%, transparent);
  color: color-mix(in srgb, var(--c) 75%, var(--text-primary));
}

.cc-chip--more {
  cursor: pointer;
  color: var(--c-primary);
  border-color: color-mix(in srgb, var(--c-primary) 30%, transparent);
  background: color-mix(in srgb, var(--c-primary) 8%, transparent);
}

.cc-chip--more:hover {
  background: color-mix(in srgb, var(--c-primary) 16%, transparent);
}

.cc-list {
  display: inline-flex;
  flex-wrap: wrap;
  gap: var(--space-1);
  max-width: 100%;
}

.cc-pop__body {
  max-width: var(--side-panel-w);
  padding: var(--space-3);
}

.cc-pop__title {
  margin-bottom: var(--space-2);
  font-size: var(--text-xs);
  font-weight: 600;
  color: var(--brand-text-muted);
}

.cc-pop__items {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-1);
}

.cc-phone {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  white-space: nowrap;
}

.cc-copy {
  display: inline-flex;
  padding: var(--hairline);
  border: 0;
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--brand-text-muted);
  cursor: pointer;
  opacity: 0;
  transition: opacity var(--dur-fast) var(--ease-out);
}

.cc-phone:hover .cc-copy,
.cc-copy:focus-visible {
  opacity: 1;
}

.cc-copy:hover {
  color: var(--c-primary);
}

.cc-pct {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
}

.cc-pct__bar {
  position: relative;
  width: var(--space-8);
  height: var(--space-1);
  border-radius: var(--radius-pill);
  background: var(--ring-track);
  overflow: hidden;
}

.cc-pct__bar i {
  position: absolute;
  inset: 0 auto 0 0;
  border-radius: inherit;
  background: var(--c);
}

.cc-pct b {
  min-width: calc(var(--space-6) + var(--space-1));
  font-weight: 600;
}

.cc-grade {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
}

.cc-grade__pill {
  padding: var(--hairline) var(--space-2);
  border-radius: var(--radius-pill);
  background: color-mix(in srgb, var(--c) 16%, transparent);
  color: var(--c);
  font-size: var(--text-xs);
}

.cc-grade__trend {
  font-weight: 800;
}

.cc-grade__trend--up {
  color: var(--gr-5);
}

.cc-grade__trend--down {
  color: var(--gr-2);
}

.cc-spark {
  --spark-h: var(--space-5);
  width: calc(var(--space-8) * 2);
}

.cc-avatars {
  display: inline-flex;
  align-items: center;
}

.cc-avatars > * + * {
  margin-left: calc(var(--space-2) * -1);
}

.cc-avatars__more {
  margin-left: var(--space-1);
  color: var(--brand-text-muted);
}

.cc-bool {
  display: inline-flex;
  font-size: var(--text-md);
}

.cc-bool--yes {
  color: var(--st-present);
}

.cc-bool--no {
  color: var(--st-unmarked);
}
</style>

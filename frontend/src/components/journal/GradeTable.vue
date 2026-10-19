<template>
  <div
    ref="scrollEl"
    class="gb"
    :class="{ 'gb--heat': heat, 'gb--readonly': !canGrade }"
    data-gradebook
    @mouseover="onHover"
    @mouseleave="clearHover"
  >
    <table
      ref="tableEl"
      class="gb__table"
      role="grid"
      :aria-label="label"
      @click="onClick"
      @keydown="onKeydown"
    >
      <thead>
        <tr>
          <th class="gb__name gb__corner" scope="col">
            <span>O'quvchi</span>
            <span class="gb__corner-count">{{ students.length }} ta</span>
          </th>
          <th
            v-for="(d, ci) in days"
            :key="d.date"
            scope="col"
            class="gb__day"
            :class="dayClass(d)"
            :data-col="ci"
            :title="dayTitle(d)"
            :aria-label="dayTitle(d)"
          >
            <span class="gb__dnum">{{ Number(d.date.slice(8)) }}</span>
            <span class="gb__dwd">{{ d.weekday }}</span>
            <span v-if="d.hasLesson && !d.isFuture" class="gb__dlesson" />
          </th>
          <th class="gb__stat gb__r3" scope="col" title="O'rtacha baho"
            >O'rt.</th
          >
          <th class="gb__stat gb__r2" scope="col" title="Baholar soni">Soni</th>
          <th
            class="gb__dist gb__r1"
            scope="col"
            title="5 / 4 / 3 / 2 taqsimoti"
            >Taqsimot</th
          >
        </tr>
      </thead>
      <tbody>
        <tr v-for="(s, ri) in students" :key="s.id" :data-student="s.id">
          <th class="gb__name" scope="row">
            <div class="gb__namebox">
              <button
                type="button"
                class="gb__who"
                :data-open-student="s.id"
                :aria-label="`${s.fullName}: kartochka`"
                @click.stop="emit('student', s)"
              >
                <span class="gb__n">{{ ri + 1 }}</span>
                <AvatarInitials :name="s.fullName" :id="s.id" size="xs" />
                <span class="gb__sname">{{ s.fullName }}</span>
              </button>
              <span
                v-if="rowStats[s.id]?.trend"
                class="gb__trend"
                :class="`gb__trend--${rowStats[s.id].trend}`"
                :title="TREND_TITLE[rowStats[s.id].trend]"
                :data-trend="rowStats[s.id].trend"
                >{{ TREND_ICON[rowStats[s.id].trend] }}</span
              >
              <span
                class="gb__pill"
                :style="{ '--c': avgColor(rowStats[s.id]?.average) }"
                data-row-avg
                >{{ fmt(rowStats[s.id]?.average) }}</span
              >
            </div>
          </th>
          <td
            v-for="(d, ci) in days"
            :key="d.date"
            class="gb__cell"
            :class="[dayClass(d), cellClass(s.id, d, ri, ci)]"
            :style="heat ? heatStyle(s.id, d) : null"
            :data-row="ri"
            :data-col="ci"
            tabindex="-1"
          >
            <template
              v-for="(g, gi) in cells[s.id + '|' + d.date] || []"
              :key="g.id"
            >
              <span
                v-if="gi < 2"
                class="gb__chip"
                :class="[
                  `gb__chip--${g.value}`,
                  {
                    'gb__chip--exam': g.type !== 'CURRENT',
                    'gb__chip--demo': g.source === 'DEMO_SIM'
                  }
                ]"
                :title="gradeTitle(g)"
                :data-grade="g.id"
                :data-value="g.value"
                >{{ g.value }}</span
              >
            </template>
            <span
              v-if="(cells[s.id + '|' + d.date] || []).length > 2"
              class="gb__more"
            >
              +{{ cells[s.id + '|' + d.date].length - 2 }}
            </span>
            <span
              v-else-if="
                canGrade &&
                !(cells[s.id + '|' + d.date] || []).length &&
                !blockedReason(d)
              "
              class="gb__plus"
              aria-hidden="true"
              >+</span
            >
          </td>
          <td class="gb__stat gb__r3">
            <span
              class="gb__avg"
              :style="{ color: avgColor(rowStats[s.id]?.average) }"
            >
              {{ fmt(rowStats[s.id]?.average) }}
            </span>
          </td>
          <td class="gb__stat gb__r2">{{ rowStats[s.id]?.count || 0 }}</td>
          <td class="gb__dist gb__r1">
            <DistBar
              :dist="rowStats[s.id]?.dist"
              :count="rowStats[s.id]?.count || 0"
            />
          </td>
        </tr>
      </tbody>
      <tfoot>
        <tr>
          <th class="gb__name" scope="row">
            <span class="gb__foot-label">Sinf o'rtachasi</span>
            <span
              class="gb__pill"
              :style="{ '--c': avgColor(total.average) }"
              data-class-avg
              >{{ fmt(total.average) }}</span
            >
          </th>
          <td
            v-for="(d, ci) in days"
            :key="d.date"
            class="gb__foot"
            :class="dayClass(d)"
            :data-col="ci"
          >
            <span
              v-if="dayAvg[d.date] != null"
              :style="{ color: avgColor(dayAvg[d.date]) }"
              >{{ dayAvg[d.date].toFixed(1) }}</span
            >
          </td>
          <td class="gb__stat gb__r3">
            <span class="gb__avg" :style="{ color: avgColor(total.average) }">{{
              fmt(total.average)
            }}</span>
          </td>
          <td class="gb__stat gb__r2">{{ total.count }}</td>
          <td class="gb__dist gb__r1"
            ><DistBar :dist="total.dist" :count="total.count"
          /></td>
        </tr>
      </tfoot>
    </table>
  </div>
</template>

<script setup>
// The month as a table: students down (name sticky on the left with the average and where it
// is heading), days across (today lit, Sundays striped, future faded), the average / count /
// spread sticky on the right and the class's averages in a sticky last row. Grades are coloured
// chips (a ring for tests, a purple dot when the demo put them there); an empty gradable cell
// shows a faint "+". Heat mode paints each cell by its grade instead.
//
// Keyboard: arrows move; 2–5 grade the cell and go DOWN a row; Delete removes (5 s undo, in the
// page); Enter opens the grade window. Shift+arrows / Shift+click select a block and 2–5 grade
// all of it; Ctrl+click adds single cells.
import {
  ref,
  reactive,
  computed,
  nextTick,
  h,
  watch,
  onMounted,
  onBeforeUnmount
} from 'vue'
import AvatarInitials from '@/components/pages/AvatarInitials.vue'
import {
  avgColor,
  blockedReason,
  nextRow,
  rectCells,
  TYPE_LABEL
} from './journal'

const props = defineProps({
  days: { type: Array, required: true },
  students: { type: Array, required: true },
  // "studentId|date" → grades
  cells: { type: Object, required: true },
  // studentId → { average, count, dist, trend }
  rowStats: { type: Object, required: true },
  dayAvg: { type: Object, required: true },
  total: { type: Object, required: true },
  nowDate: { type: String, required: true },
  canGrade: { type: Boolean, default: false },
  heat: { type: Boolean, default: false },
  pulseDate: { type: String, default: null },
  openCell: { type: Object, default: null },
  label: { type: String, default: 'Baholar jurnali' }
})
const emit = defineEmits([
  'quick',
  'open',
  'remove',
  'column',
  'student',
  'blocked'
])

const TREND_ICON = { up: '↗', down: '↘', flat: '→' }
const TREND_TITLE = {
  up: "Baholari ko'tarilmoqda",
  down: 'Baholari tushmoqda',
  flat: "Baholari o'zgarmayapti"
}

// a small spread bar: 5 / 4 / 3 / 2 in their colours
const DistBar = props_ =>
  h(
    'span',
    {
      class: 'gb__bar',
      title: props_.count
        ? `5: ${props_.dist[5]} · 4: ${props_.dist[4]} · 3: ${props_.dist[3]} · 2: ${props_.dist[2]}`
        : ''
    },
    props_.count
      ? [5, 4, 3, 2].map(v =>
          h('span', {
            style: {
              width: `${(props_.dist[v] / props_.count) * 100}%`,
              background: `var(--gr-${v})`
            }
          })
        )
      : []
  )
DistBar.props = ['dist', 'count']

const scrollEl = ref(null)
const tableEl = ref(null)
const fmt = v => (v == null ? '—' : v.toFixed(2))

function dayClass(d) {
  return {
    'gb--sunday': d.isSunday,
    'gb--holiday': d.isHoliday && !d.isSunday,
    'gb--future': d.isFuture && !d.isSunday,
    'gb--today': d.date === props.nowDate,
    'gb--pulse': d.date === props.pulseDate
  }
}

function cellClass(sid, d, ri, ci) {
  return {
    'gb__cell--sel': sel.has(`${ri}|${ci}`),
    'gb__cell--open': props.openCell?.r === ri && props.openCell?.c === ci,
    'gb__cell--multi': (props.cells[sid + '|' + d.date] || []).length > 1
  }
}

function heatStyle(sid, d) {
  const list = props.cells[sid + '|' + d.date]
  if (!list?.length) return null
  const avg = list.reduce((s, g) => s + g.value, 0) / list.length
  return { '--heat': avgColor(avg) }
}

function dayTitle(d) {
  const dm = `${d.date.slice(8, 10)}.${d.date.slice(5, 7)}`
  if (d.isSunday) return `${dm} — yakshanba, dam olish kuni`
  if (d.isHoliday) return `${dm} — bayram: ${d.holidayName}`
  if (d.isFuture) return `${dm} — hali kelmagan kun`
  return `${dm}${d.hasLesson ? ' — shu fandan dars bor' : ''} · bosing: butun sinfga baho`
}

function gradeTitle(g) {
  const parts = [`${g.value} · ${TYPE_LABEL[g.type] || ''}`]
  if (g.source === 'DEMO_SIM')
    parts.push('🤖 Demo: avtomatik qo‘yilgan (tahrirlansa — qo‘lda bo‘ladi)')
  if (g.createdBy) parts.push(`Qo'ydi: ${g.createdBy}`)
  if (g.comment) parts.push(`Izoh: ${g.comment}`)
  return parts.join('\n')
}

// ---------------------------------------------------------------- cross hover (no re-render)
const hover = reactive({ r: -1, c: -1 })
const hoverCss = computed(() => {
  const out = []
  if (hover.c >= 0)
    out.push(
      `.gb [data-col="${hover.c}"]{box-shadow:inset 0 0 0 9999px var(--gb-cross)}`
    )
  if (hover.r >= 0)
    out.push(
      `.gb tbody tr:nth-child(${hover.r + 1}) > *{box-shadow:inset 0 0 0 9999px var(--gb-cross)}`
    )
  return out.join('')
})

// one <style> in the head, rewritten as the pointer moves: the table itself never re-renders
let hoverStyle = null
onMounted(() => {
  hoverStyle = document.createElement('style')
  document.head.appendChild(hoverStyle)
})
onBeforeUnmount(() => hoverStyle?.remove())
watch(hoverCss, css => {
  if (hoverStyle) hoverStyle.textContent = css
})

function onHover(e) {
  const td = e.target.closest?.('[data-col]')
  const c = td ? Number(td.dataset.col) : -1
  const r = td?.dataset.row != null ? Number(td.dataset.row) : -1
  if (c !== hover.c) hover.c = c
  if (r !== hover.r) hover.r = r
}

function clearHover() {
  hover.r = -1
  hover.c = -1
}

// ---------------------------------------------------------------- selection & focus
const sel = reactive(new Set())
const cur = { r: 0, c: 0 }
let anchor = { r: 0, c: 0 }

function focusCell(r, c, { scroll = true } = {}) {
  cur.r = Math.max(0, Math.min(props.students.length - 1, r))
  cur.c = Math.max(0, Math.min(props.days.length - 1, c))
  nextTick(() => {
    const td = tableEl.value?.querySelector(
      `td[data-row="${cur.r}"][data-col="${cur.c}"]`
    )
    td?.focus({ preventScroll: !scroll })
    if (scroll) td?.scrollIntoView({ block: 'nearest', inline: 'nearest' })
  })
}

function selectRect(to) {
  sel.clear()
  for (const k of rectCells(anchor, to)) sel.add(k)
}

/** Cells to grade: the selection when the cursor is in it, else the cursor's cell. */
function targets(r, c) {
  if (sel.size > 1 && sel.has(`${r}|${c}`))
    return [...sel]
      .map(k => k.split('|').map(Number))
      .map(([r_, c_]) => ({ r: r_, c: c_ }))
  return [{ r, c }]
}

function onClick(e) {
  const th = e.target.closest('th[data-col]')
  if (th) {
    emit('column', Number(th.dataset.col))
    return
  }
  const td = e.target.closest('td[data-row]')
  if (!td) return
  const r = Number(td.dataset.row)
  const c = Number(td.dataset.col)
  if (e.shiftKey) {
    selectRect({ r, c })
    focusCell(r, c, { scroll: false })
    return
  }
  if (e.ctrlKey || e.metaKey) {
    const k = `${r}|${c}`
    if (sel.has(k)) sel.delete(k)
    else sel.add(k)
    anchor = { r, c }
    focusCell(r, c, { scroll: false })
    return
  }
  sel.clear()
  anchor = { r, c }
  focusCell(r, c, { scroll: false })
  const gradeEl = e.target.closest('[data-grade]')
  emit('open', {
    r,
    c,
    el: td,
    gradeId: gradeEl ? gradeEl.dataset.grade : null
  })
}

function onKeydown(e) {
  const td = e.target.closest?.('td[data-row]')
  if (!td) return
  const r = Number(td.dataset.row)
  const c = Number(td.dataset.col)
  const move = {
    ArrowUp: [-1, 0],
    ArrowDown: [1, 0],
    ArrowLeft: [0, -1],
    ArrowRight: [0, 1]
  }[e.key]
  if (move) {
    e.preventDefault()
    const to = {
      r: Math.max(0, Math.min(props.students.length - 1, r + move[0])),
      c: Math.max(0, Math.min(props.days.length - 1, c + move[1]))
    }
    if (e.shiftKey) {
      // a block starts where the cursor is when nothing is selected yet
      if (!sel.size) anchor = { r, c }
      selectRect(to)
    } else {
      sel.clear()
      anchor = to
    }
    focusCell(to.r, to.c)
    return
  }
  if (
    ['2', '3', '4', '5'].includes(e.key) &&
    !e.ctrlKey &&
    !e.metaKey &&
    !e.altKey
  ) {
    e.preventDefault()
    if (!props.canGrade) return
    const list = targets(r, c)
    emit('quick', { cells: list, value: Number(e.key) })
    if (list.length === 1) {
      const n = nextRow(r, props.students.length)
      anchor = { r: n, c }
      focusCell(n, c)
    } else sel.clear()
    return
  }
  if (e.key === 'Delete' || e.key === 'Backspace') {
    e.preventDefault()
    if (props.canGrade) emit('remove', { r, c })
    return
  }
  if (e.key === 'Enter' || e.key === ' ') {
    e.preventDefault()
    emit('open', { r, c, el: td, gradeId: null })
    return
  }
  if (e.key === 'Escape') sel.clear()
}

/** Puts the day in the middle of the visible days (the name column stays on the left). */
function scrollToDate(date) {
  const el = scrollEl.value
  const ci = props.days.findIndex(d => d.date === date)
  if (!el || ci < 0) return
  const th = el.querySelector(`th[data-col="${ci}"]`)
  const name = el.querySelector('thead .gb__name')
  if (!th || !name) return
  const left = name.offsetWidth
  el.scrollLeft = Math.max(
    0,
    th.offsetLeft - left - (el.clientWidth - left - th.offsetWidth * 4) / 2
  )
}

defineExpose({
  focusCell,
  scrollToDate,
  clearSelection: () => sel.clear(),
  selection: sel
})
</script>

<style scoped>
.gb {
  --gb-cross: color-mix(in srgb, var(--c-primary) 6%, transparent);
  --gb-line: var(--dash-edge);
  --gb-sticky-bg: var(--card-bg);
  position: relative;
  overflow: auto;
  max-height: calc(100dvh - var(--gb-chrome-h));
  scrollbar-width: thin;
  overscroll-behavior: contain;
}

.gb__table {
  border-collapse: separate;
  border-spacing: 0;
  min-width: 100%;
  font-variant-numeric: tabular-nums;
}

.gb__table th,
.gb__table td {
  border-bottom: 1px solid var(--gb-line);
  border-right: 1px solid var(--gb-line);
  padding: 0;
  text-align: center;
  vertical-align: middle;
}

/* ---- header ---- */
thead th {
  position: sticky;
  top: 0;
  z-index: 3;
  height: var(--gb-head-h);
  background: var(--gb-sticky-bg);
  font-size: var(--text-xs);
  font-weight: 600;
  color: var(--brand-text-muted);
}

.gb__day {
  position: relative;
  width: var(--gb-day-w);
  min-width: var(--gb-day-w);
  cursor: pointer;
}

.gb__day:hover {
  color: var(--c-primary);
}

.gb__dnum {
  display: block;
  font-size: var(--text-sm);
  font-weight: 700;
  color: var(--text-primary);
}

.gb__dwd {
  display: block;
  font-size: var(--text-xs);
  font-weight: 500;
}

.gb__dlesson {
  position: absolute;
  left: 50%;
  bottom: var(--hairline);
  width: var(--space-1);
  height: var(--space-1);
  margin-left: calc(var(--space-1) / -2);
  border-radius: var(--radius-pill);
  background: var(--c-primary);
}

thead .gb--today {
  background: var(--c-primary);
  color: white;
}

thead .gb--today .gb__dnum {
  color: white;
}

thead .gb--today .gb__dlesson {
  background: white;
}

/* ---- name column ---- */
.gb__name {
  position: sticky;
  left: 0;
  z-index: 2;
  width: var(--gb-name-w);
  min-width: var(--gb-name-w);
  max-width: var(--gb-name-w);
  padding: 0 var(--space-2) 0 var(--space-1) !important;
  background: var(--gb-sticky-bg);
  text-align: left !important;
  white-space: nowrap;
  box-shadow: 1px 0 0 var(--gb-line);
}

.gb__namebox {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  height: var(--gb-row-h);
  min-width: 0;
}

thead .gb__name,
tfoot .gb__name {
  z-index: 4;
}

.gb__corner {
  font-size: var(--text-xs);
}

.gb__corner span:first-child {
  margin-left: var(--space-2);
  font-weight: 700;
  color: var(--text-primary);
}

.gb__corner-count {
  margin-left: var(--space-2);
}

.gb__who {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  flex: 1 1 auto;
  min-width: 0;
  height: 100%;
  padding: 0 var(--space-1);
  border: 0;
  background: transparent;
  color: var(--text-primary);
  font: inherit;
  font-size: var(--text-sm);
  text-align: left;
  cursor: pointer;
}

.gb__who:hover .gb__sname {
  color: var(--c-primary);
  text-decoration: underline;
}

.gb__n {
  flex: none;
  width: var(--space-5);
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
  text-align: right;
}

.gb__sname {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  font-weight: 600;
}

.gb__trend {
  flex: none;
  font-size: var(--text-sm);
  font-weight: 800;
}

.gb__trend--up {
  color: var(--gr-5);
}

.gb__trend--down {
  color: var(--gr-2);
}

.gb__trend--flat {
  color: var(--brand-text-muted);
}

.gb__pill {
  flex: none;
  min-width: var(--space-7);
  padding: var(--hairline) var(--space-1);
  border-radius: var(--radius-pill);
  background: color-mix(in srgb, var(--c) 16%, transparent);
  color: var(--c);
  font-size: var(--text-xs);
  font-weight: 800;
  text-align: center;
}

/* ---- cells ---- */
.gb__cell {
  position: relative;
  height: var(--gb-row-h);
  cursor: cell;
  outline: none;
}

.gb--readonly .gb__cell {
  cursor: default;
}

.gb__cell:focus {
  box-shadow: inset 0 0 0 2px var(--c-primary) !important;
  z-index: 1;
}

.gb__cell--sel {
  background: color-mix(in srgb, var(--c-primary) 16%, transparent) !important;
}

.gb__cell--open {
  box-shadow: inset 0 0 0 2px var(--c-primary) !important;
}

.gb--today:not(th) {
  background: color-mix(in srgb, var(--c-primary) 7%, transparent);
}

.gb--sunday:not(th),
thead .gb--sunday {
  background: repeating-linear-gradient(
    135deg,
    var(--tile-stripe) 0 var(--space-1),
    transparent var(--space-1) var(--space-2)
  );
  cursor: not-allowed;
}

.gb--holiday:not(th) {
  background: color-mix(in srgb, var(--year-vacation) 7%, transparent);
}

.gb--future:not(th) {
  opacity: 0.45;
}

thead .gb--future {
  opacity: 0.55;
}

.gb--pulse:not(th) {
  animation: gb-pulse 1s var(--ease-out) 2;
}

@keyframes gb-pulse {
  50% {
    background: color-mix(in srgb, var(--c-primary) 22%, transparent);
  }
}

.gb__chip {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: var(--gb-chip);
  height: var(--gb-chip);
  border-radius: var(--radius-sm);
  background: color-mix(in srgb, var(--g) 18%, transparent);
  color: var(--g);
  font-size: var(--text-sm);
  font-weight: 800;
  cursor: pointer;
}

.gb__chip--5 {
  --g: var(--gr-5);
}

.gb__chip--4 {
  --g: var(--gr-4);
}

.gb__chip--3 {
  --g: var(--gr-3);
}

.gb__chip--2 {
  --g: var(--gr-2);
}

/* a test / term grade: a ring in its colour */
.gb__chip--exam {
  box-shadow: inset 0 0 0 2px var(--g);
}

/* put there by the demo: a small purple dot */
.gb__chip--demo::after {
  content: '';
  position: absolute;
  top: calc(var(--hairline) * -2);
  right: calc(var(--hairline) * -2);
  width: var(--space-1);
  height: var(--space-1);
  border-radius: var(--radius-pill);
  background: var(--chart-primary);
}

.gb__cell--multi .gb__chip {
  width: calc(var(--gb-chip) * 0.66);
  height: calc(var(--gb-chip) * 0.8);
  font-size: var(--text-xs);
}

.gb__cell--multi .gb__chip + .gb__chip {
  margin-left: var(--hairline);
}

.gb__more {
  position: absolute;
  right: var(--hairline);
  bottom: 0;
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
}

.gb__plus {
  font-size: var(--text-md);
  color: var(--brand-text-muted);
  opacity: 0.18;
}

.gb__cell:hover .gb__plus,
.gb__cell:focus .gb__plus {
  opacity: 0.7;
  color: var(--c-primary);
}

/* heat: the cell itself carries the colour */
.gb--heat .gb__cell[style] {
  background: color-mix(in srgb, var(--heat) 55%, transparent);
}

.gb--heat .gb__chip {
  background: transparent;
  color: var(--text-primary);
  box-shadow: none;
}

.gb--heat .gb__plus {
  opacity: 0;
}

/* ---- right stats ---- */
.gb__stat,
.gb__dist {
  position: sticky;
  z-index: 2;
  background: var(--gb-sticky-bg);
  font-size: var(--text-sm);
  font-weight: 600;
}

.gb__stat {
  width: var(--gb-stat-w);
  min-width: var(--gb-stat-w);
}

.gb__dist {
  width: var(--gb-dist-w);
  min-width: var(--gb-dist-w);
  padding: 0 var(--space-2) !important;
}

thead .gb__stat,
thead .gb__dist,
tfoot .gb__stat,
tfoot .gb__dist {
  z-index: 4;
}

.gb__r1 {
  right: 0;
}

.gb__r2 {
  right: var(--gb-dist-w);
}

.gb__r3 {
  right: calc(var(--gb-dist-w) + var(--gb-stat-w));
  box-shadow: -1px 0 0 var(--gb-line);
}

.gb__avg {
  font-weight: 800;
}

.gb :deep(.gb__bar) {
  display: flex;
  height: var(--bar-h);
  border-radius: var(--radius-pill);
  background: var(--tile-none);
  overflow: hidden;
}

/* ---- footer ---- */
tfoot th,
tfoot td {
  position: sticky;
  bottom: 0;
  z-index: 3;
  height: var(--gb-row-h);
  background: var(--surface-2);
  border-top: 1px solid var(--gb-line);
  font-size: var(--text-xs);
  font-weight: 800;
}

tfoot .gb__name {
  background: var(--surface-2);
}

.gb__foot-label {
  margin: 0 var(--space-2);
  font-weight: 700;
  color: var(--text-primary);
}

@media (max-width: 599px) {
  .gb {
    --gb-name-w: 150px;
    max-height: none;
  }

  .gb__n,
  .gb__trend,
  .gb__name :deep(.avatar) {
    display: none;
  }

  .gb__stat,
  .gb__dist {
    position: static;
  }
}

@media (prefers-reduced-motion: reduce) {
  .gb--pulse:not(th) {
    animation: none;
  }
}
</style>

// The list pages' rules in plain functions (tests/ui/crud-logic.spec.js runs them without a
// browser): what a column shows as text (the table, the cards, the export use the same words),
// the page state in the URL, the phone mask, the pager's label and age from a birth date.
import { formatPhone } from '../../utils/tashkent.js'

export const PAGE_SIZES = [10, 25, 50, 100]
export const DEFAULT_SIZE = 25

const pad = n => String(n).padStart(2, '0')

/** '2019-02-03' → '03.02.2019' */
export function formatDay(v) {
  if (!v) return ''
  const [y, m, d] = String(v).slice(0, 10).split('-')
  return d ? `${d}.${m}.${y}` : String(v)
}

/** Full years on `today` (YYYY-MM-DD) of someone born on `birth`. */
export function ageOn(birth, today) {
  if (!birth) return null
  const [by, bm, bd] = String(birth).slice(0, 10).split('-').map(Number)
  const [ty, tm, td] = String(today).slice(0, 10).split('-').map(Number)
  let age = ty - by
  if (tm < bm || (tm === bm && td < bd)) age--
  return age
}

export function todayIso(d = new Date()) {
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

/** The raw value of a column for a row: field as a key or a function. */
export function rawValue(col, row) {
  if (typeof col.field === 'function') return col.field(row)
  return row?.[col.field ?? col.name]
}

/** A list column's items: an array, or "1-A, 1-B" split at the commas; never repeated. */
export function listItems(v) {
  const items = Array.isArray(v)
    ? v
    : String(v ?? '')
        .split(',')
        .map(s => s.trim())
  return [...new Set(items.filter(Boolean))]
}

/** The column's value as plain text — what the cell says, what Excel / PDF get. */
export function cellText(col, row, today = todayIso()) {
  const v = rawValue(col, row)
  switch (col.type) {
    case 'date':
      return formatDay(v)
    case 'age': {
      const a = ageOn(v, today)
      return a == null ? '' : `${a} yosh`
    }
    case 'phone':
      return v ? formatPhone(v) : ''
    case 'percent':
      return v == null ? '' : `${Math.round(v)}%`
    case 'grade':
      return v == null ? '' : Number(v).toFixed(2)
    case 'badge':
      return v == null ? '' : col.badgeLabels?.[v] || String(v)
    case 'subjects':
    case 'list':
      return listItems(v).join(', ')
    case 'bool':
      return v == null ? '' : v ? 'Ha' : "Yo'q"
    case 'load':
      return v ? `${v} soat` : ''
    case 'risk':
      return v == null ? '' : String(v)
    case 'contact': {
      const ph = col.phoneField ? row?.[col.phoneField] : null
      return [v, ph ? formatPhone(ph) : ''].filter(Boolean).join(' · ')
    }
    default:
      if (col.format) return col.format(v, row) ?? ''
      return v == null ? '' : String(v)
  }
}

/** "1–25 / 521" */
export function rangeLabel(page, size, total) {
  if (!total) return '0 / 0'
  const from = (page - 1) * size + 1
  const to = Math.min(page * size, total)
  return `${from}–${to} / ${total}`
}

/**
 * The page state from the URL query: view, search, page, size, sort and the module's filters
 * (only the keys it declares, so ?create=1 or ?edit=5 stay out).
 */
export function readQuery(query, filterKeys, defaults = {}) {
  const num = (v, d) =>
    v != null && v !== '' && !isNaN(Number(v)) ? Number(v) : d
  const [sortBy, dir] = String(query.sort || defaults.sort || '').split(',')
  const filters = {}
  for (const k of filterKeys) {
    if (query[k] != null && query[k] !== '') filters[k] = String(query[k])
  }
  return {
    view: query.view || defaults.view || 'table',
    q: query.q ? String(query.q) : '',
    page: Math.max(1, num(query.page, 1)),
    size: PAGE_SIZES.includes(num(query.size, 0))
      ? num(query.size, 0)
      : defaults.size || DEFAULT_SIZE,
    sortBy: sortBy || null,
    descending: dir === 'desc',
    filters
  }
}

/** The URL query for a page state (defaults left out, so a plain page keeps a plain URL). */
export function writeQuery(state, defaults = {}) {
  const q = {}
  if (state.view && state.view !== (defaults.view || 'table'))
    q.view = state.view
  if (state.q) q.q = state.q
  if (state.page > 1) q.page = String(state.page)
  if (state.size && state.size !== (defaults.size || DEFAULT_SIZE))
    q.size = String(state.size)
  if (state.sortBy) {
    const s = `${state.sortBy},${state.descending ? 'desc' : 'asc'}`
    if (s !== defaults.sort) q.sort = s
  }
  for (const [k, v] of Object.entries(state.filters || {})) {
    if (v != null && v !== '') q[k] = String(v)
  }
  return q
}

/** "+998 90 111 22 33" as it is being typed (digits only are taken; 998 is always there). */
export function maskPhone(input) {
  let d = String(input ?? '').replace(/\D/g, '')
  if (d.startsWith('998')) d = d.slice(3)
  d = d.slice(0, 9)
  const parts = [d.slice(0, 2), d.slice(2, 5), d.slice(5, 7), d.slice(7, 9)]
  return ['+998', ...parts.filter(Boolean)].join(' ')
}

/** A phone is complete: 998 and nine more digits. */
export const phoneComplete = v =>
  /^998\d{9}$/.test(String(v ?? '').replace(/\D/g, ''))

/** Columns shown by default: all but those marked hidden, unless the viewer chose. */
export function visibleColumnNames(columns, saved) {
  const names = columns.filter(c => c.name !== 'id').map(c => c.name)
  if (Array.isArray(saved)) {
    const kept = saved.filter(n => names.includes(n))
    if (kept.length) return kept
  }
  return columns.filter(c => c.name !== 'id' && !c.hidden).map(c => c.name)
}

/** Bright text on a colour: the first letters of a name for an avatar ("Ali Valiyev" → "AV"). */
export function initials(name) {
  return String(name || '?')
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map(w => w[0].toUpperCase())
    .join('')
}

// O'quv yillari in plain functions (tests/ui/crud-logic.spec.js runs them): where a date sits on a
// year's bar, the status words, the line under the bar.

const day = iso =>
  Date.UTC(
    ...String(iso)
      .slice(0, 10)
      .split('-')
      .map((v, i) => Number(v) - (i === 1 ? 1 : 0))
  )

/** 0–100: how far `date` is along [start, end] (clamped). */
export function along(start, end, date) {
  const a = day(start)
  const b = day(end)
  if (b <= a) return 0
  return Math.max(0, Math.min(100, ((day(date) - a) / (b - a)) * 100))
}

/** A term as a segment of its year's bar: left and width in %. */
export function segment(year, term) {
  const left = along(year.startDate, year.endDate, term.startDate)
  const right = along(year.startDate, year.endDate, term.endDate)
  return { left, width: Math.max(right - left, 0.6) }
}

export const STATUS = {
  CURRENT: { label: 'Joriy', tone: 'var(--c-primary)' },
  ACTIVE: { label: 'Davom etmoqda', tone: 'var(--st-present)' },
  FINISHED: { label: 'Yakunlangan', tone: 'var(--st-unmarked)' },
  PLANNED: { label: 'Rejalashtirilgan', tone: 'var(--st-excused)' }
}

/** The line under the bar: "62% o'tdi · 74 ish kuni qoldi" / "Yakunlangan · 210 ish kuni" / "… gacha N kun". */
export function progressLine(y, today) {
  if (y.status === 'FINISHED') return `Yakunlangan · ${y.workDays} ish kuni`
  if (today < y.startDate) {
    const days = Math.round((day(y.startDate) - day(today)) / 86400000)
    return `Boshlanishiga ${days} kun · ${y.workDays} ish kuni`
  }
  if (today > y.endDate) return `Tugagan · ${y.workDays} ish kuni`
  return `${Math.round(y.progress ?? 0)}% o'tdi · ${y.leftDays} ish kuni qoldi`
}

/** The term today falls in ("2-chorak"), or null. */
export function termOn(terms, today) {
  return (
    (terms || []).find(t => t.startDate <= today && today <= t.endDate) || null
  )
}

/** Whole days from `a` to `b` (both YYYY-MM-DD). */
export function daysBetween(a, b) {
  return Math.round((day(b) - day(a)) / 86400000)
}

/** The date `n` days after `iso` (n may be negative). */
export function isoPlus(iso, n) {
  return new Date(day(iso) + n * 86400000).toISOString().slice(0, 10)
}

/**
 * Where one edge of term `k` may go when it is dragged to `iso`: inside the year, not before the
 * previous term's end (+1 day) nor after the next one's start (−1 day), never past its own other
 * edge. Returns the date it lands on.
 */
export function moveEdge(terms, k, edge, iso, year) {
  const order = terms
    .map((_, i) => i)
    .sort((a, b) =>
      String(terms[a].startDate).localeCompare(String(terms[b].startDate))
    )
  const pos = order.indexOf(k)
  const prev = pos > 0 ? terms[order[pos - 1]] : null
  const next = pos < order.length - 1 ? terms[order[pos + 1]] : null
  const t = terms[k]
  const [lo, hi] =
    edge === 'startDate'
      ? [prev?.endDate ? isoPlus(prev.endDate, 1) : year.startDate, t.endDate]
      : [
          t.startDate,
          next?.startDate ? isoPlus(next.startDate, -1) : year.endDate
        ]
  if (iso < lo) return lo
  if (iso > hi) return hi
  return iso
}

/** A quarter's colour on the bar: the n-th quarter of the year (1-chorak, 2-chorak, …). */
export const QUARTER_COLORS = [
  'var(--subj-6)',
  'var(--subj-2)',
  'var(--subj-3)',
  'var(--subj-4)'
]

/** "kuzgi ta'til" → "kuzgi ta'tilgacha"; "2-chorak" → "2-chorakkacha". */
function until(name) {
  const n = String(name || '').toLowerCase()
  return n.endsWith('k') ? `${n}kacha` : `${n}gacha`
}

/**
 * The line under the current year's bar: "1-chorak · 68% o'tdi · kuzgi ta'tilgacha 12 kun"; in a
 * vacation "Kuzgi ta'til · 2-chorakkacha 5 kun"; null outside the terms.
 */
export function termLine(terms, today) {
  const sorted = [...(terms || [])].sort((a, b) =>
    String(a.startDate).localeCompare(String(b.startDate))
  )
  const now = sorted.find(t => t.startDate <= today && today <= t.endDate)
  const after = sorted.filter(t => t.startDate > today)
  if (now) {
    const passed = Math.round(along(now.startDate, now.endDate, today))
    const nextOther = after.find(t => t.kind !== now.kind) || after[0]
    const parts = [now.name]
    if (now.kind === 'QUARTER') parts.push(`${passed}% o'tdi`)
    if (nextOther)
      parts.push(
        `${until(nextOther.name)} ${daysBetween(today, nextOther.startDate)} kun`
      )
    else parts.push(`yil oxirigacha ${daysBetween(today, now.endDate)} kun`)
    return parts.join(' · ')
  }
  if (after.length)
    return `${after[0].name} boshlanishiga ${daysBetween(today, after[0].startDate)} kun`
  return null
}

/** Quarters in date order get 1…4, vacations null: which colour each term has. */
export function quarterIndex(terms) {
  let q = 0
  return [...(terms || [])]
    .map((t, i) => ({ t, i }))
    .sort((a, b) => String(a.t.startDate).localeCompare(String(b.t.startDate)))
    .reduce((m, { t, i }) => {
      m[i] = t.kind === 'QUARTER' ? q++ : null
      return m
    }, {})
}

/**
 * What is wrong with a year's terms, the same rules the server checks (409): a name and both dates,
 * the end not before the start, inside the year, no two overlapping. Empty — fine to save.
 */
export function termProblems(terms, year) {
  const out = []
  const list = (terms || []).filter(Boolean)
  for (const t of list) {
    const name = t.name?.trim() || 'Nomsiz'
    if (!t.name?.trim() || !t.startDate || !t.endDate) {
      out.push(`«${name}»: nomi va ikkala sanasi bo'lishi kerak`)
      continue
    }
    if (t.endDate < t.startDate)
      out.push(`«${name}»: tugash sanasi boshlanishidan oldin`)
    else if (t.startDate < year.startDate || t.endDate > year.endDate)
      out.push(`«${name}» o'quv yili ichida bo'lishi kerak`)
  }
  const dated = list
    .filter(t => t.startDate && t.endDate)
    .sort((a, b) => a.startDate.localeCompare(b.startDate))
  for (let i = 1; i < dated.length; i++) {
    if (dated[i].startDate <= dated[i - 1].endDate)
      out.push(`«${dated[i - 1].name}» va «${dated[i].name}» ustma-ust tushadi`)
  }
  return out
}

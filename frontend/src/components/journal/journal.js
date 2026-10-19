// The journal's rules in plain functions (tests/ui/aj-logic.spec.js runs them without a browser):
// a list of grades → average / mastery (3+) / quality (4+) / count / spread, a student's trend,
// the running daily values for the panel's sparklines, why a day cannot take a grade and how
// the cursor moves after a quick grade.

export const GRADE_VALUES = [5, 4, 3, 2]
export const GRADE_COLOR = {
  5: 'var(--gr-5)',
  4: 'var(--gr-4)',
  3: 'var(--gr-3)',
  2: 'var(--gr-2)'
}
export const TYPES = [
  { value: 'CURRENT', label: 'Joriy' },
  { value: 'EXAM', label: 'Nazorat' },
  { value: 'QUARTERLY', label: 'Chorak' }
]
export const TYPE_LABEL = Object.fromEntries(TYPES.map(t => [t.value, t.label]))

const round = (v, d = 2) => Math.round(v * 10 ** d) / 10 ** d

/** { average, mastery %, quality %, count, dist {5,4,3,2} } of a list of grades (values). */
export function stats(values) {
  const dist = { 5: 0, 4: 0, 3: 0, 2: 0 }
  let sum = 0
  for (const v of values) {
    dist[v] = (dist[v] || 0) + 1
    sum += v
  }
  const n = values.length
  if (!n) return { average: null, mastery: null, quality: null, count: 0, dist }
  return {
    average: round(sum / n),
    mastery: round(((n - dist[2]) / n) * 100, 1),
    quality: round(((dist[5] + dist[4]) / n) * 100, 1),
    count: n,
    dist
  }
}

/** The colour of an average (or a grade): 4.5+ green, 3.5+ blue, 2.5+ amber, else red. */
export function avgColor(v) {
  if (v == null) return 'var(--st-unmarked)'
  if (v >= 4.5) return GRADE_COLOR[5]
  if (v >= 3.5) return GRADE_COLOR[4]
  if (v >= 2.5) return GRADE_COLOR[3]
  return GRADE_COLOR[2]
}

/**
 * Where a student is heading: the average of the later half of the month's grades against the
 * earlier half. 'up' / 'down' when it moved by 0.3 or more, else 'flat'; null under 4 grades.
 */
export function trend(grades) {
  if (grades.length < 4) return null
  const sorted = [...grades].sort((a, b) => a.date.localeCompare(b.date))
  const half = Math.floor(sorted.length / 2)
  const avg = list => list.reduce((s, g) => s + g.value, 0) / list.length
  const d = avg(sorted.slice(half)) - avg(sorted.slice(0, half))
  return d >= 0.3 ? 'up' : d <= -0.3 ? 'down' : 'flat'
}

/**
 * The panel's sparklines: for each day with grades (in order) the running average, mastery,
 * quality and count of the month so far.
 */
export function runningSeries(grades) {
  const byDay = {}
  for (const g of grades) (byDay[g.date] ||= []).push(g.value)
  const out = { average: [], mastery: [], quality: [], count: [] }
  const seen = []
  for (const day of Object.keys(byDay).sort()) {
    seen.push(...byDay[day])
    const s = stats(seen)
    out.average.push(s.average)
    out.mastery.push(s.mastery)
    out.quality.push(s.quality)
    out.count.push(s.count)
  }
  return out
}

/** Why a journal day cannot take a grade (null when it can). */
export function blockedReason(d) {
  if (!d) return null
  if (d.isSunday)
    return "😴 Uzr, bu kun yakshanba — maktab ishlamaydi. Baho qo'yib bo'lmaydi."
  if (d.isHoliday) return `🎉 Bu kun bayram — dam olish kuni: ${d.holidayName}`
  if (d.isFuture) return "⏳ Kelajak sanaga baho qo'yib bo'lmaydi."
  return null
}

/** After a quick grade the cursor goes down one row (stays on the last row). */
export function nextRow(r, rows) {
  return Math.min(rows - 1, r + 1)
}

/** Every cell of the rectangle between two cells, as "r|c". */
export function rectCells(a, b) {
  const out = []
  for (let r = Math.min(a.r, b.r); r <= Math.max(a.r, b.r); r++)
    for (let c = Math.min(a.c, b.c); c <= Math.max(a.c, b.c); c++)
      out.push(`${r}|${c}`)
  return out
}

/** "Oktabr 2026" */
export const MONTHS = [
  'Yanvar',
  'Fevral',
  'Mart',
  'Aprel',
  'May',
  'Iyun',
  'Iyul',
  'Avgust',
  'Sentyabr',
  'Oktabr',
  'Noyabr',
  'Dekabr'
]
export function monthTitle(month) {
  const [y, m] = month.split('-').map(Number)
  return `${MONTHS[m - 1]} ${y}`
}

export function shiftMonth(month, delta) {
  const [y, m] = month.split('-').map(Number)
  const d = new Date(y, m - 1 + delta, 1)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
}

/**
 * The subject a class opens on: (a) today's lesson going on now or the last one that started,
 * (b) the subject chosen last time, (c) the first subject of the class.
 * overview = /api/classes/{id}/overview; nowDate / nowMinutes in Tashkent.
 */
export function pickSubject(overview, nowDate, nowMinutes, last) {
  const subjects = overview?.subjects || []
  if (!subjects.length) return null
  const has = id => subjects.some(s => s.id === id)
  if (overview.today === nowDate) {
    const started = (overview.todayLessons || []).filter(
      l => toMin(l.start) <= nowMinutes && has(l.subjectId)
    )
    if (started.length) return started[started.length - 1].subjectId
  }
  if (last && has(last)) return last
  return subjects[0].id
}

function toMin(t) {
  const [h, m] = String(t).split(':').map(Number)
  return h * 60 + m
}

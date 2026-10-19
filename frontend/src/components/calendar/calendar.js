// The calendar page's rules in plain functions: event types (name, icon, colour), dates as
// "YYYY-MM-DD" strings (no time zone surprises), the weeks of a month (Monday first), and
// how multi-day events lie across a week as bands in lanes. tests/ui/calendar-logic.spec.js
// runs them without a browser.

export const EVENT_TYPES = {
  PARENT_MEETING: {
    label: "Ota-onalar yig'ilishi",
    icon: 'family_restroom',
    color: 'var(--subj-2)'
  },
  EXAM: { label: 'Imtihon / nazorat', icon: 'quiz', color: 'var(--st-absent)' },
  VACATION: {
    label: "Ta'til",
    icon: 'beach_access',
    color: 'var(--year-vacation)'
  },
  HOLIDAY: { label: 'Bayram', icon: 'celebration', color: 'var(--subj-4)' },
  SCHOOL_EVENT: {
    label: 'Maktab tadbiri',
    icon: 'emoji_events',
    color: 'var(--subj-1)'
  },
  OTHER: { label: 'Boshqa', icon: 'event', color: 'var(--st-unmarked)' }
}
export const TYPE_ORDER = [
  'PARENT_MEETING',
  'EXAM',
  'VACATION',
  'HOLIDAY',
  'SCHOOL_EVENT',
  'OTHER'
]

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
  'Oktyabr',
  'Noyabr',
  'Dekabr'
]
export const DAYS_SHORT = ['Du', 'Se', 'Ch', 'Pa', 'Ju', 'Sh', 'Ya']

const pad = n => String(n).padStart(2, '0')

/** Date → "YYYY-MM-DD" (local calendar date, no UTC shift). */
export function iso(d) {
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

/** "YYYY-MM-DD" → Date at noon (safe from DST and zone shifts). */
export function parse(s) {
  return new Date(`${s}T12:00:00`)
}

export function addDays(s, n) {
  const d = parse(s)
  d.setDate(d.getDate() + n)
  return iso(d)
}

export function daysBetween(a, b) {
  return Math.round((parse(b) - parse(a)) / 86_400_000)
}

/** 0 = Monday … 6 = Sunday */
export function weekdayIndex(s) {
  return (parse(s).getDay() + 6) % 7
}

/** The weeks (Monday first) that cover the month: [[7 dates], …]. */
export function monthWeeks(year, month) {
  const first = iso(new Date(year, month, 1))
  let start = addDays(first, -weekdayIndex(first))
  const last = iso(new Date(year, month + 1, 0))
  const weeks = []
  while (start <= last) {
    weeks.push(Array.from({ length: 7 }, (_, k) => addDays(start, k)))
    start = addDays(start, 7)
  }
  return weeks
}

export const isMultiDay = e => e.startDate !== e.endDate

/**
 * One week's multi-day events as bands: { event, col (0–6), span, lane, cutStart, cutEnd }.
 * Lanes are filled from the top: an event takes the first lane free over all its days.
 */
export function weekBands(week, events) {
  const from = week[0]
  const to = week[6]
  const list = events
    .filter(e => isMultiDay(e) && e.startDate <= to && e.endDate >= from)
    .sort(
      (a, b) =>
        a.startDate.localeCompare(b.startDate) ||
        b.endDate.localeCompare(a.endDate)
    )
  const lanes = []
  const out = []
  for (const e of list) {
    const s = e.startDate < from ? from : e.startDate
    const t = e.endDate > to ? to : e.endDate
    const col = daysBetween(from, s)
    const span = daysBetween(s, t) + 1
    let lane = 0
    while (lanes[lane] && lanes[lane].some(c => c >= col && c < col + span))
      lane++
    lanes[lane] = [
      ...(lanes[lane] || []),
      ...Array.from({ length: span }, (_, k) => col + k)
    ]
    out.push({
      event: e,
      col,
      span,
      lane,
      cutStart: e.startDate < from,
      cutEnd: e.endDate > to
    })
  }
  return { bands: out, lanes: lanes.length }
}

/** Single-day events of a date, timed ones by their time, whole-day ones first. */
export function dayEvents(date, events) {
  return events
    .filter(e => !isMultiDay(e) && e.startDate === date)
    .sort(
      (a, b) =>
        (a.startTime || '').localeCompare(b.startTime || '') ||
        a.title.localeCompare(b.title)
    )
}

/** Holidays and vacations covering the date (they dim the day and name it). */
export function daysOff(date, events) {
  return events.filter(
    e =>
      (e.type === 'HOLIDAY' || e.type === 'VACATION') &&
      e.startDate <= date &&
      e.endDate >= date
  )
}

/** The home page's attendance steps: ≥95 / 90–95 / 85–90 / <85. */
export function heatStep(rate) {
  if (rate === null || rate === undefined) return null
  return rate >= 95 ? 1 : rate >= 90 ? 2 : rate >= 85 ? 3 : 4
}

/** An event moved to a new first day keeps its length. */
export function moved(e, newStart) {
  const len = daysBetween(e.startDate, e.endDate)
  return { startDate: newStart, endDate: addDays(newStart, len) }
}

/** "6 kun qoldi", "ertaga", "bugun". */
export function countdown(days) {
  if (days === null || days === undefined) return ''
  if (days <= 0) return 'bugun'
  if (days === 1) return 'ertaga'
  return `${days} kun qoldi`
}

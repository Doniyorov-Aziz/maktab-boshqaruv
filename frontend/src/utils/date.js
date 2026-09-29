// Local-timezone-safe date helpers for the whole app.
//
// The app is built for a single-timezone deployment (Asia/Tashkent, UTC+5)
// and everything here works off the browser's local clock rather than any
// IANA timezone conversion. The one hard rule: never round-trip a calendar
// date through `Date#toISOString()` or `new Date('YYYY-MM-DD')` and back —
// both convert through UTC, and in any positive-UTC-offset zone that silently
// shifts a local midnight back to the previous day. That mismatch is what
// made the calendar mark the wrong cell as "today".

const UZ_MONTHS = [
  'Yanvar',
  'Fevral',
  'Mart',
  'Aprel',
  'May',
  'Iyun',
  'Iyul',
  'Avgust',
  'Sentabr',
  'Oktabr',
  'Noyabr',
  'Dekabr'
]
const UZ_MONTHS_SHORT = [
  'yan',
  'fev',
  'mar',
  'apr',
  'may',
  'iyun',
  'iyul',
  'avg',
  'sen',
  'okt',
  'noy',
  'dek'
]
const UZ_DAYS = [
  'Yakshanba',
  'Dushanba',
  'Seshanba',
  'Chorshanba',
  'Payshanba',
  'Juma',
  'Shanba'
]
const UZ_DAYS_SHORT = ['Yak', 'Dush', 'Sesh', 'Chor', 'Pay', 'Juma', 'Shan']

/** "YYYY-MM-DD" built from local Y/M/D components — no UTC conversion. */
export function toLocalDateStr(date) {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

/** Today's date as "YYYY-MM-DD", in the browser's local time. */
export function todayStr() {
  return toLocalDateStr(new Date())
}

/** Parses a "YYYY-MM-DD" string as a local-midnight Date (not UTC). */
export function parseLocalDate(dateStr) {
  const [y, m, d] = dateStr.split('-').map(Number)
  return new Date(y, m - 1, d)
}

/** DD.MM.YYYY, from a Date object or a "YYYY-MM-DD" string. */
export function formatDate(input) {
  if (!input) return ''
  const d = typeof input === 'string' ? parseLocalDate(input) : input
  const dd = String(d.getDate()).padStart(2, '0')
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  return `${dd}.${mm}.${d.getFullYear()}`
}

/** "29-sen" style short form, from a Date or "YYYY-MM-DD" string. */
export function formatShortDate(input) {
  const d = typeof input === 'string' ? parseLocalDate(input) : input
  return `${d.getDate()}-${UZ_MONTHS_SHORT[d.getMonth()]}`
}

export function weekdayName(date) {
  return UZ_DAYS[date.getDay()]
}

export function weekdayShort(date) {
  return UZ_DAYS_SHORT[date.getDay()]
}

export function monthName(monthIndex) {
  return UZ_MONTHS[monthIndex]
}

/** True for Sunday — the only non-working day in the seeded school week. */
export function isWeekend(date) {
  return date.getDay() === 0
}

/**
 * Human-relative label for a "YYYY-MM-DD" date compared to today, e.g.
 * "Bugun", "Ertaga", "3 kundan keyin", "2 kun oldin". Safe to diff via
 * `new Date(str)` here since both operands are plain date-only strings
 * parsed identically (both anchor to UTC midnight), so the day-count
 * difference is exact regardless of the local UTC offset.
 */
export function relativeDay(dateStr) {
  const diffDays = Math.round(
    (new Date(dateStr) - new Date(todayStr())) / 86400000
  )
  if (diffDays === 0) return 'Bugun'
  if (diffDays === 1) return 'Ertaga'
  if (diffDays === -1) return 'Kecha'
  if (diffDays > 1) return `${diffDays} kundan keyin`
  return `${Math.abs(diffDays)} kun oldin`
}

/** "09:42" style exact time, or "kecha 16:10" / "3 kun oldin, 09:12" for older timestamps. */
export function formatActivityTime(isoTimestamp) {
  const d = new Date(isoTimestamp)
  const hh = String(d.getHours()).padStart(2, '0')
  const mm = String(d.getMinutes()).padStart(2, '0')
  const time = `${hh}:${mm}`
  const dateStr = toLocalDateStr(d)
  const diffDays = Math.round(
    (new Date(dateStr) - new Date(todayStr())) / 86400000
  )
  if (diffDays === 0) return time
  if (diffDays === -1) return `kecha ${time}`
  if (diffDays < -1) return `${Math.abs(diffDays)} kun oldin, ${time}`
  return `${formatDate(dateStr)} ${time}`
}

// The attendance page's rules in plain functions (tests/ui/aj-logic.spec.js runs them without a
// browser): the four statuses (key, icon, colour), the click cycle of the cards, the school-day
// stepping of the date control (no Sundays, nothing after today) and the counters.

export const STATUSES = [
  {
    value: 'PRESENT',
    label: 'Keldi',
    short: '✓',
    icon: 'check',
    key: '1',
    color: 'var(--st-present)'
  },
  {
    value: 'ABSENT',
    label: 'Kelmadi',
    short: '✕',
    icon: 'close',
    key: '2',
    color: 'var(--st-absent)'
  },
  {
    value: 'LATE',
    label: 'Kechikdi',
    short: '⏱',
    icon: 'schedule',
    key: '3',
    color: 'var(--st-late)'
  },
  {
    value: 'EXCUSED',
    label: 'Sababli',
    short: '📄',
    icon: 'description',
    key: '4',
    color: 'var(--st-excused)'
  }
]
export const STATUS = Object.fromEntries(STATUSES.map(s => [s.value, s]))
export const BY_KEY = Object.fromEntries(STATUSES.map(s => [s.key, s.value]))

/** Cards: a tap goes Keldi → Kelmadi → Kechikdi → Sababli → Keldi (unmarked starts at Keldi). */
export function nextStatus(status) {
  const order = ['PRESENT', 'ABSENT', 'LATE', 'EXCUSED']
  const k = order.indexOf(status)
  return k < 0 ? 'PRESENT' : order[(k + 1) % order.length]
}

const pad = n => String(n).padStart(2, '0')
const iso = d =>
  `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`

/** One school day before / after (Sundays skipped); never after `today`. */
export function stepSchoolDay(date, delta, today) {
  const d = new Date(`${date}T12:00:00`)
  do d.setDate(d.getDate() + delta)
  while (d.getDay() === 0)
  const s = iso(d)
  return s > today ? date : s
}

/** The day to open: today, or the Saturday before when today is Sunday. */
export function openingDay(today) {
  const d = new Date(`${today}T12:00:00`)
  return d.getDay() === 0 ? stepSchoolDay(today, -1, today) : today
}

export const MONTHS_GEN = [
  'yanvar',
  'fevral',
  'mart',
  'aprel',
  'may',
  'iyun',
  'iyul',
  'avgust',
  'sentyabr',
  'oktabr',
  'noyabr',
  'dekabr'
]
export const WEEKDAYS_LONG = [
  'Yakshanba',
  'Dushanba',
  'Seshanba',
  'Chorshanba',
  'Payshanba',
  'Juma',
  'Shanba'
]

/** "Dushanba, 19-oktabr" */
export function dayLabel(date) {
  const d = new Date(`${date}T12:00:00`)
  return `${WEEKDAYS_LONG[d.getDay()]}, ${d.getDate()}-${MONTHS_GEN[d.getMonth()]}`
}

/** "19-okt, Dushanba" for the date control */
export function shortDayLabel(date) {
  const d = new Date(`${date}T12:00:00`)
  return `${d.getDate()}-${MONTHS_GEN[d.getMonth()].slice(0, 3)}, ${WEEKDAYS_LONG[d.getDay()]}`
}

/** { PRESENT, ABSENT, LATE, EXCUSED, marked, total } of a list of statuses (null = unmarked). */
export function counts(statuses) {
  const c = {
    PRESENT: 0,
    ABSENT: 0,
    LATE: 0,
    EXCUSED: 0,
    marked: 0,
    total: statuses.length
  }
  for (const s of statuses) {
    if (!s) continue
    c[s]++
    c.marked++
  }
  return c
}

/** "Keldi" share for the ring: present + late of those counted (excused left out, as on the home page). */
export function presentRate(c) {
  const base = c.PRESENT + c.LATE + c.ABSENT
  return base ? ((c.PRESENT + c.LATE) / base) * 100 : null
}

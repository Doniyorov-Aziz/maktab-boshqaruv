// The timetable page's rules in plain functions (no Vue): what a period is, where the breaks
// are, whether a lesson may go to a cell (the same checks and words as the backend's 409),
// which teachers and rooms are free, a teacher's empty "windows", and the next date of a
// weekday. tests/ui/timetable-logic.spec.js runs them without a browser.
import { tashkentNow } from '../../utils/tashkent.js'

export const WEEKDAYS = [
  'Dushanba',
  'Seshanba',
  'Chorshanba',
  'Payshanba',
  'Juma',
  'Shanba'
]
export const WEEKDAY_SHORT = ['Du', 'Se', 'Ch', 'Pa', 'Ju', 'Sh']

/** '10:20:00' → 620 */
export function minutes(t) {
  if (!t) return 0
  const [h, m] = String(t).split(':').map(Number)
  return h * 60 + m
}

export const hm = t => String(t || '').slice(0, 5)

/** "204-xona", a named room as it is. */
export function roomLabel(room) {
  if (!room) return ''
  return /^\d/.test(String(room)) ? `${room}-xona` : room
}

/** Today's weekday name in Tashkent, null on Sunday. */
export function todayWeekday(now = new Date()) {
  const d = new Date(`${tashkentNow(now).date}T12:00:00`)
  return [null, ...WEEKDAYS][d.getDay()] || null
}

/** The gaps between periods: [{ after: n, minutes, long }] (a gap of 20+ minutes is the big break). */
export function breaks(periods) {
  const out = []
  for (let k = 0; k + 1 < periods.length; k++) {
    const gap = minutes(periods[k + 1].start) - minutes(periods[k].end)
    if (gap > 0)
      out.push({ after: periods[k].n, minutes: gap, long: gap >= 20 })
  }
  return out
}

const overlaps = (aStart, aEnd, bStart, bEnd) =>
  minutes(aStart) < minutes(bEnd) && minutes(aEnd) > minutes(bStart)

/**
 * Why `lesson` may not move to `day` at `period` (null = it may): its class, its teacher or
 * its room already has a lesson then. The words are the backend's 409 messages.
 */
export function conflictReason(all, lesson, day, period) {
  const others = all.filter(
    l =>
      l.id !== lesson.id &&
      l.weekday === day &&
      overlaps(l.start, l.end, period.start, period.end)
  )
  const c = others.find(l => l.classId === lesson.classId)
  if (c) return `${lesson.className} sinfida bu vaqtda dars bor (${c.subject})`
  const t = others.find(l => l.teacherId === lesson.teacherId)
  if (t) return `${lesson.teacherShort} bu vaqtda ${t.className} da`
  const r = others.find(l => l.roomId === lesson.roomId)
  if (r)
    return `${roomLabel(lesson.room)} bu vaqtda band (${r.className}, ${r.subject})`
  return null
}

/** Ids of teachers / rooms / classes busy on `day` between start and end (except one lesson). */
export function busy(all, day, start, end, exceptId = null) {
  const teachers = new Set()
  const rooms = new Set()
  const classes = new Set()
  for (const l of all) {
    if (l.id === exceptId || l.weekday !== day) continue
    if (!overlaps(l.start, l.end, start, end)) continue
    teachers.add(l.teacherId)
    rooms.add(l.roomId)
    classes.add(l.classId)
  }
  return { teachers, rooms, classes }
}

/** Per weekday: the periods between a teacher's first and last lesson that are empty ("oyna"). */
export function teacherWindows(teacherLessons, periods) {
  const out = []
  for (const day of WEEKDAYS) {
    const taken = new Set(
      teacherLessons.filter(l => l.weekday === day).map(l => hm(l.start))
    )
    if (!taken.size) continue
    const idx = periods
      .map((p, k) => (taken.has(hm(p.start)) ? k : -1))
      .filter(k => k >= 0)
    const first = Math.min(...idx)
    const last = Math.max(...idx)
    const empty = []
    for (let k = first + 1; k < last; k++)
      if (!taken.has(hm(periods[k].start))) empty.push(periods[k].n)
    if (empty.length) out.push({ day, periods: empty })
  }
  return out
}

/** Hours a week of each teacher: Map(teacherId → hours). */
export function teacherHours(all) {
  const m = new Map()
  for (const l of all)
    m.set(
      l.teacherId,
      (m.get(l.teacherId) || 0) + (minutes(l.end) - minutes(l.start)) / 60
    )
  return m
}

/** The date (YYYY-MM-DD) of the weekday this week from today on (today if it is that day). */
export function nextDateOf(weekday, from = tashkentNow().date) {
  const k = WEEKDAYS.indexOf(weekday)
  if (k < 0) return from
  const d = new Date(`${from}T12:00:00`)
  const now = (d.getDay() + 6) % 7 // Monday 0
  const add = (k - now + 7) % 7
  d.setDate(d.getDate() + add)
  const p = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}

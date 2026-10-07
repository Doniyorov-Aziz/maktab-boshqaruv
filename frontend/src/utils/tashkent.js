// School time is Tashkent time, whatever the computer's own time zone is.

const FMT = new Intl.DateTimeFormat('en-CA', {
  timeZone: 'Asia/Tashkent',
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
  second: '2-digit',
  hourCycle: 'h23'
})

/** { date: 'YYYY-MM-DD', month: 'YYYY-MM', minutes: minutes since midnight (with fraction) } */
export function tashkentNow(now = new Date()) {
  const p = Object.fromEntries(
    FMT.formatToParts(now).map(x => [x.type, x.value])
  )
  const date = `${p.year}-${p.month}-${p.day}`
  return {
    date,
    month: date.slice(0, 7),
    minutes: Number(p.hour) * 60 + Number(p.minute) + Number(p.second) / 60
  }
}

/** '10:20:00' → 620 */
export function toMinutes(time) {
  if (!time) return 0
  const [h, m] = String(time).split(':').map(Number)
  return h * 60 + m
}

/** '10:20:00' → '10:20' */
export function hhmm(time) {
  return String(time || '').slice(0, 5)
}

/** '+998901234567' → '+998 90 123 45 67' (other formats are shown as they are) */
export function formatPhone(phone) {
  if (!phone) return ''
  const digits = String(phone).replace(/\D/g, '')
  if (digits.length === 12 && digits.startsWith('998')) {
    return `+998 ${digits.slice(3, 5)} ${digits.slice(5, 8)} ${digits.slice(8, 10)} ${digits.slice(10)}`
  }
  return phone
}

/** 'Karimova Dilnoza' → 'Karimova D.' */
export function shortName(fullName) {
  if (!fullName) return ''
  const [last, first] = fullName.split(' ')
  return first ? `${last} ${first.charAt(0)}.` : last
}

/** 'Karimova Dilnoza' → 'KD' */
export function initials(fullName) {
  return (fullName || '?')
    .split(' ')
    .slice(0, 2)
    .map(s => s.charAt(0))
    .join('')
    .toUpperCase()
}

/**
 * Where the school day stands for a list of today's lessons (sorted, with start/end 'HH:mm:ss'):
 * { kind: 'off' | 'none' | 'before' | 'lesson' | 'break' | 'after', current, next, last, … }
 */
export function lessonState(overview, minutes) {
  if (!overview) return null
  if (overview.isDayOff) return { kind: 'off', reason: overview.dayOffReason }
  const lessons = overview.todayLessons || []
  if (!lessons.length) return { kind: 'none' }
  const first = lessons[0]
  const lastLesson = lessons[lessons.length - 1]
  if (minutes < toMinutes(first.start)) {
    return {
      kind: 'before',
      next: first,
      inMinutes: Math.ceil(toMinutes(first.start) - minutes)
    }
  }
  for (let i = 0; i < lessons.length; i++) {
    const l = lessons[i]
    const s = toMinutes(l.start)
    const e = toMinutes(l.end)
    if (minutes >= s && minutes < e) {
      return {
        kind: 'lesson',
        current: l,
        progress: (minutes - s) / (e - s),
        leftMinutes: Math.ceil(e - minutes),
        gradable: l
      }
    }
    const next = lessons[i + 1]
    if (next && minutes >= e && minutes < toMinutes(next.start)) {
      return {
        kind: 'break',
        next,
        inMinutes: Math.ceil(toMinutes(next.start) - minutes),
        gradable: l
      }
    }
  }
  return { kind: 'after', count: lessons.length, gradable: lastLesson }
}

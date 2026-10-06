import { ref, onMounted, onBeforeUnmount } from 'vue'

/** The current time, ticking every `ms` — for live "now" cards. */
export function useNow(ms = 20000) {
  const now = ref(new Date())
  let timer = null
  onMounted(() => {
    timer = setInterval(() => (now.value = new Date()), ms)
  })
  onBeforeUnmount(() => clearInterval(timer))
  return now
}

/** "08:30:00" → 510 (minutes since midnight). */
export function minutesOf(time) {
  const [h, m] = (time || '0:0').split(':').map(Number)
  return h * 60 + m
}

/** The device's date as "2026-10-05". */
export function isoDate(date) {
  const p = n => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${p(date.getMonth() + 1)}-${p(date.getDate())}`
}

/**
 * Where the day is right now: { phase: 'before' | 'lesson' | 'break' | 'over',
 * current, next, progress (0..1 of the lesson or break), left (minutes) }.
 */
export function dayPhase(lessons, now) {
  if (!lessons?.length) return null
  const minute = now.getHours() * 60 + now.getMinutes() + now.getSeconds() / 60
  const first = lessons[0]
  if (minute < minutesOf(first.start)) {
    return {
      phase: 'before',
      current: null,
      next: first,
      progress: 0,
      left: minutesOf(first.start) - minute
    }
  }
  for (let i = 0; i < lessons.length; i++) {
    const l = lessons[i]
    const start = minutesOf(l.start)
    const end = minutesOf(l.end)
    if (minute >= start && minute < end) {
      return {
        phase: 'lesson',
        current: l,
        next: lessons[i + 1] || null,
        progress: (minute - start) / (end - start),
        left: Math.ceil(end - minute)
      }
    }
    const next = lessons[i + 1]
    if (next && minute >= end && minute < minutesOf(next.start)) {
      const nextStart = minutesOf(next.start)
      return {
        phase: 'break',
        current: null,
        next,
        progress: (minute - end) / (nextStart - end),
        left: Math.ceil(nextStart - minute)
      }
    }
  }
  return { phase: 'over', current: null, next: null, progress: 1, left: 0 }
}

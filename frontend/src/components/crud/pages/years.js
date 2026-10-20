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

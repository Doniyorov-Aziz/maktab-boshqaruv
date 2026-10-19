// The attendance and journal rules without a browser: status cycle, school-day stepping,
// counters, journal stats / trend / subject auto-pick / cursor moves.
//   npx playwright test tests/ui/aj-logic.spec.js
import { test, expect } from '@playwright/test'
import {
  nextStatus,
  stepSchoolDay,
  openingDay,
  counts,
  presentRate,
  dayLabel,
  shortDayLabel,
  BY_KEY
} from '../../src/components/attendance/attendance.js'
import {
  stats,
  trend,
  runningSeries,
  blockedReason,
  nextRow,
  rectCells,
  monthTitle,
  shiftMonth,
  pickSubject,
  avgColor
} from '../../src/components/journal/journal.js'

test('cards cycle Keldi → Kelmadi → Kechikdi → Sababli → Keldi; unmarked starts at Keldi', () => {
  expect(nextStatus(null)).toBe('PRESENT')
  expect(nextStatus('PRESENT')).toBe('ABSENT')
  expect(nextStatus('ABSENT')).toBe('LATE')
  expect(nextStatus('LATE')).toBe('EXCUSED')
  expect(nextStatus('EXCUSED')).toBe('PRESENT')
})

test('keys 1–4 are the four statuses', () => {
  expect(BY_KEY).toEqual({ 1: 'PRESENT', 2: 'ABSENT', 3: 'LATE', 4: 'EXCUSED' })
})

test('date control skips Sundays and never goes past today', () => {
  // 2026-10-19 is a Monday
  expect(stepSchoolDay('2026-10-19', -1, '2026-10-19')).toBe('2026-10-17')
  expect(stepSchoolDay('2026-10-17', 1, '2026-10-19')).toBe('2026-10-19')
  expect(stepSchoolDay('2026-10-19', 1, '2026-10-19')).toBe('2026-10-19')
  expect(openingDay('2026-10-18')).toBe('2026-10-17')
  expect(openingDay('2026-10-19')).toBe('2026-10-19')
})

test('day labels', () => {
  expect(dayLabel('2026-10-19')).toBe('Dushanba, 19-oktabr')
  expect(shortDayLabel('2026-10-19')).toBe('19-okt, Dushanba')
})

test('counters and the ring leave excused out', () => {
  const c = counts(['PRESENT', 'PRESENT', 'ABSENT', 'LATE', 'EXCUSED', null])
  expect(c).toEqual({
    PRESENT: 2,
    ABSENT: 1,
    LATE: 1,
    EXCUSED: 1,
    marked: 5,
    total: 6
  })
  expect(presentRate(c)).toBe(75)
  expect(presentRate(counts([null]))).toBeNull()
})

test('journal stats: average, mastery 3+, quality 4+, count, spread', () => {
  expect(stats([5, 4, 3, 2])).toEqual({
    average: 3.5,
    mastery: 75,
    quality: 50,
    count: 4,
    dist: { 5: 1, 4: 1, 3: 1, 2: 1 }
  })
  expect(stats([]).average).toBeNull()
})

test('trend compares the later half with the earlier one', () => {
  const g = (date, value) => ({ date, value })
  expect(
    trend([
      g('2026-10-01', 3),
      g('2026-10-02', 3),
      g('2026-10-10', 5),
      g('2026-10-11', 5)
    ])
  ).toBe('up')
  expect(
    trend([
      g('2026-10-01', 5),
      g('2026-10-02', 5),
      g('2026-10-10', 3),
      g('2026-10-11', 3)
    ])
  ).toBe('down')
  expect(
    trend([
      g('2026-10-01', 4),
      g('2026-10-02', 4),
      g('2026-10-10', 4),
      g('2026-10-11', 4)
    ])
  ).toBe('flat')
  expect(trend([g('2026-10-01', 4)])).toBeNull()
})

test('running series per day', () => {
  const s = runningSeries([
    { date: '2026-10-02', value: 3 },
    { date: '2026-10-01', value: 5 },
    { date: '2026-10-02', value: 4 }
  ])
  expect(s.average).toEqual([5, 4])
  expect(s.count).toEqual([1, 3])
})

test('Sundays, holidays and future days cannot be graded', () => {
  expect(blockedReason({ isSunday: true })).toMatch(/yakshanba/)
  expect(
    blockedReason({ isHoliday: true, holidayName: 'Mustaqillik' })
  ).toMatch(/Mustaqillik/)
  expect(blockedReason({ isFuture: true })).toMatch(/Kelajak/)
  expect(blockedReason({})).toBeNull()
})

test('quick entry moves down and stays on the last row; a block is a rectangle', () => {
  expect(nextRow(0, 40)).toBe(1)
  expect(nextRow(39, 40)).toBe(39)
  expect(rectCells({ r: 1, c: 2 }, { r: 0, c: 3 })).toEqual([
    '0|2',
    '0|3',
    '1|2',
    '1|3'
  ])
})

test('months', () => {
  expect(monthTitle('2026-10')).toBe('Oktabr 2026')
  expect(shiftMonth('2026-12', 1)).toBe('2027-01')
  expect(shiftMonth('2026-01', -1)).toBe('2025-12')
})

test('subject auto-pick: lesson now / last started → last chosen → first', () => {
  const overview = {
    today: '2026-10-19',
    subjects: [{ id: 3 }, { id: 12 }, { id: 16 }],
    todayLessons: [
      { start: '08:30:00', subjectId: 3 },
      { start: '11:15:00', subjectId: 12 },
      { start: '12:40:00', subjectId: 16 }
    ]
  }
  expect(pickSubject(overview, '2026-10-19', 11 * 60 + 30, 16)).toBe(12)
  expect(pickSubject(overview, '2026-10-19', 12 * 60, null)).toBe(12)
  // before the first lesson: the last chosen one, else the first subject
  expect(pickSubject(overview, '2026-10-19', 7 * 60, 16)).toBe(16)
  expect(pickSubject(overview, '2026-10-19', 7 * 60, 99)).toBe(3)
  // another day's overview: lessons do not count
  expect(pickSubject(overview, '2026-10-20', 11 * 60 + 30, null)).toBe(3)
  expect(pickSubject({ subjects: [] }, '2026-10-19', 0, null)).toBeNull()
})

test('average colours', () => {
  expect(avgColor(4.6)).toBe('var(--gr-5)')
  expect(avgColor(3.6)).toBe('var(--gr-4)')
  expect(avgColor(2.6)).toBe('var(--gr-3)')
  expect(avgColor(2)).toBe('var(--gr-2)')
})

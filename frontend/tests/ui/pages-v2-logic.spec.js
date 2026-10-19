// The rules behind the v2 pages, without a browser or a backend: the drag-and-drop check
// (the backend's words), free teachers / rooms, a teacher's empty windows, breaks, the month
// grid and how a vacation lies across weeks, moving an event keeps its length.
import { test, expect } from '@playwright/test'
import {
  breaks,
  busy,
  conflictReason,
  nextDateOf,
  teacherWindows
} from '../../src/components/timetable/timetable.js'
import {
  daysBetween,
  heatStep,
  monthWeeks,
  moved,
  weekBands
} from '../../src/components/calendar/calendar.js'

const P = [
  { n: 1, start: '08:30:00', end: '09:15:00' },
  { n: 2, start: '09:25:00', end: '10:10:00' },
  { n: 3, start: '10:20:00', end: '11:05:00' },
  { n: 4, start: '11:30:00', end: '12:15:00' }
]
const L = (
  id,
  classId,
  className,
  teacherId,
  roomId,
  weekday,
  k,
  subject = 'Fizika'
) => ({
  id,
  classId,
  className,
  teacherId,
  teacherShort: teacherId === 1 ? 'Karimova D.' : 'Aliyev B.',
  roomId,
  room: roomId === 10 ? '204' : '301',
  weekday,
  start: P[k].start,
  end: P[k].end,
  subject
})
const all = [
  L(1, 100, '5-A', 1, 10, 'Dushanba', 0, 'Algebra'),
  L(2, 200, '7-B', 1, 20, 'Dushanba', 1, 'Fizika'),
  L(3, 200, '7-B', 2, 10, 'Dushanba', 2, 'Kimyo'),
  L(4, 100, '5-A', 2, 20, 'Dushanba', 3, 'Ona tili')
]

test('drag check: busy teacher, room, class — with the backend words', () => {
  const lesson = all[0] // 5-A, Karimova, room 204
  expect(conflictReason(all, lesson, 'Dushanba', P[1])).toBe(
    'Karimova D. bu vaqtda 7-B da'
  )
  expect(conflictReason(all, lesson, 'Dushanba', P[2])).toBe(
    '204-xona bu vaqtda band (7-B, Kimyo)'
  )
  expect(conflictReason(all, lesson, 'Dushanba', P[3])).toBe(
    '5-A sinfida bu vaqtda dars bor (Ona tili)'
  )
  expect(conflictReason(all, lesson, 'Seshanba', P[1])).toBeNull()
  expect(
    conflictReason(all, lesson, 'Dushanba', P[0]),
    'its own cell is free for itself'
  ).toBeNull()
})

test('free teachers and rooms at a time', () => {
  const b = busy(all, 'Dushanba', '09:25:00', '10:10:00')
  expect([...b.teachers]).toEqual([1])
  expect([...b.rooms]).toEqual([20])
  expect([
    ...busy(all, 'Dushanba', '09:25:00', '10:10:00', 2).teachers
  ]).toEqual([])
})

test("breaks and a teacher's empty windows", () => {
  expect(breaks(P)).toEqual([
    { after: 1, minutes: 10, long: false },
    { after: 2, minutes: 10, long: false },
    { after: 3, minutes: 25, long: true }
  ])
  const karimova = [all[0], { ...all[1], start: P[3].start, end: P[3].end }]
  expect(teacherWindows(karimova, P)).toEqual([
    { day: 'Dushanba', periods: [2, 3] }
  ])
})

test('next date of a weekday', () => {
  expect(nextDateOf('Dushanba', '2026-10-19')).toBe('2026-10-19')
  expect(nextDateOf('Seshanba', '2026-10-19')).toBe('2026-10-20')
  expect(nextDateOf('Dushanba', '2026-10-20')).toBe('2026-10-26')
})

test('month grid: Monday first, whole weeks', () => {
  const w = monthWeeks(2026, 9) // October 2026
  expect(w[0][0]).toBe('2026-09-28')
  expect(w.at(-1)[6]).toBe('2026-11-01')
  expect(w.every(x => x.length === 7)).toBe(true)
})

test('a vacation lies across weeks as bands, in lanes', () => {
  const ev = [
    { id: 1, startDate: '2026-10-26', endDate: '2026-11-04', type: 'VACATION' },
    { id: 2, startDate: '2026-10-27', endDate: '2026-10-28', type: 'EXAM' },
    { id: 3, startDate: '2026-10-27', endDate: '2026-10-27', type: 'OTHER' }
  ]
  const weeks = monthWeeks(2026, 9)
  const last = weekBands(weeks.at(-1), ev)
  expect(last.lanes).toBe(2)
  expect(last.bands.find(b => b.event.id === 1)).toMatchObject({
    col: 0,
    span: 7,
    lane: 0,
    cutEnd: true
  })
  expect(last.bands.find(b => b.event.id === 2)).toMatchObject({
    col: 1,
    span: 2,
    lane: 1
  })
  expect(
    last.bands.some(b => b.event.id === 3),
    'one-day events are pills'
  ).toBe(false)
})

test('moving an event keeps its length; attendance steps', () => {
  const m = moved(
    { startDate: '2026-11-02', endDate: '2026-11-09' },
    '2026-11-05'
  )
  expect(m).toEqual({ startDate: '2026-11-05', endDate: '2026-11-12' })
  expect(daysBetween(m.startDate, m.endDate)).toBe(7)
  expect([96, 92, 87, 70, null].map(heatStep)).toEqual([1, 2, 3, 4, null])
})

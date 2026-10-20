// The list pages' rules without a browser (src/components/crud/crud.js): cell words, phone
// formatting and mask, age, the pager's label, the page state in the URL and back.
//   npx playwright test tests/ui/crud-logic.spec.js
import { test, expect } from '@playwright/test'
import {
  cellText,
  formatDay,
  ageOn,
  rangeLabel,
  readQuery,
  writeQuery,
  maskPhone,
  phoneComplete,
  listItems,
  visibleColumnNames,
  initials
} from '../../src/components/crud/crud.js'
import { formatPhone } from '../../src/utils/tashkent.js'
import {
  along,
  segment,
  progressLine,
  termOn,
  moveEdge,
  isoPlus,
  daysBetween
} from '../../src/components/crud/pages/years.js'
import {
  suggestSubjectIcon,
  positionLook
} from '../../src/utils/subjectIcon.js'

test('a phone reads "+998 90 111 22 33" however it was typed', () => {
  for (const typed of [
    '998901112233',
    '+998901112233',
    '+998 (90) 111-22-33',
    '901112233',
    '90 111 22 33'
  ])
    expect(formatPhone(typed), typed).toBe('+998 90 111 22 33')
  expect(formatPhone('+7 495 123 45 67')).toBe('+7 495 123 45 67')
  expect(formatPhone('')).toBe('')
  expect(cellText({ type: 'phone', field: 'p' }, { p: '998901112233' })).toBe(
    '+998 90 111 22 33'
  )
})

test('the phone field masks as it is typed and knows when it is complete', () => {
  expect(maskPhone('')).toBe('+998')
  expect(maskPhone('9')).toBe('+998 9')
  expect(maskPhone('90111')).toBe('+998 90 111')
  expect(maskPhone('+998901112233')).toBe('+998 90 111 22 33')
  expect(maskPhone('+998 90 111 22 334')).toBe('+998 90 111 22 33')
  expect(phoneComplete('+998 90 111 22 33')).toBe(true)
  expect(phoneComplete('+998 90 111')).toBe(false)
})

test('dates, ages and numbers as the cells say them', () => {
  expect(formatDay('2019-02-03')).toBe('03.02.2019')
  expect(ageOn('2019-10-21', '2026-10-20')).toBe(6)
  expect(ageOn('2019-10-20', '2026-10-20')).toBe(7)
  expect(
    cellText({ type: 'age', field: 'b' }, { b: '2019-10-20' }, '2026-10-20')
  ).toBe('7 yosh')
  expect(cellText({ type: 'percent', field: 'v' }, { v: 87.6 })).toBe('88%')
  expect(cellText({ type: 'grade', field: 'v' }, { v: 4.256 })).toBe('4.26')
  expect(
    cellText(
      { type: 'badge', field: 's', badgeLabels: { ACTIVE: 'Ishda' } },
      { s: 'ACTIVE' }
    )
  ).toBe('Ishda')
  expect(cellText({ field: 'n', format: v => `${v} ta` }, { n: 3 })).toBe(
    '3 ta'
  )
  expect(cellText({ field: r => r.a + r.b }, { a: 'x', b: 'y' })).toBe('xy')
})

test('a list cell: split at commas, never repeated', () => {
  expect(listItems('1-A, 1-B, 1-A')).toEqual(['1-A', '1-B'])
  expect(listItems(['Matematika', 'Matematika', 'Fizika'])).toEqual([
    'Matematika',
    'Fizika'
  ])
  expect(listItems(null)).toEqual([])
  expect(cellText({ type: 'list', field: 'c' }, { c: '5-A, 5-B' })).toBe(
    '5-A, 5-B'
  )
})

test('the pager says "1–25 / 521"', () => {
  expect(rangeLabel(1, 25, 521)).toBe('1–25 / 521')
  expect(rangeLabel(21, 25, 521)).toBe('501–521 / 521')
  expect(rangeLabel(1, 25, 0)).toBe('0 / 0')
})

test('page state: URL → state → URL, defaults left out, unknown keys ignored', () => {
  const s = readQuery(
    {
      view: 'cards',
      q: 'ali',
      page: '3',
      size: '50',
      sort: 'fullName,desc',
      status: 'ACTIVE',
      create: '1'
    },
    ['status', 'positionId'],
    { view: 'table', size: 25 }
  )
  expect(s).toEqual({
    view: 'cards',
    q: 'ali',
    page: 3,
    size: 50,
    sortBy: 'fullName',
    descending: true,
    filters: { status: 'ACTIVE' }
  })
  expect(writeQuery(s, { view: 'table', size: 25 })).toEqual({
    view: 'cards',
    q: 'ali',
    page: '3',
    size: '50',
    sort: 'fullName,desc',
    status: 'ACTIVE'
  })
  // a plain page has a plain URL; a bad size falls back to the default
  expect(
    writeQuery(readQuery({}, [], { view: 'table', size: 25 }), {
      view: 'table',
      size: 25
    })
  ).toEqual({})
  expect(readQuery({ size: '7' }, [], { size: 25 }).size).toBe(25)
})

test('columns shown: the module default, or what this browser chose', () => {
  const cols = [
    { name: 'id' },
    { name: 'a' },
    { name: 'b', hidden: true },
    { name: 'c' }
  ]
  expect(visibleColumnNames(cols, null)).toEqual(['a', 'c'])
  expect(visibleColumnNames(cols, ['b', 'gone'])).toEqual(['b'])
  expect(visibleColumnNames(cols, ['gone'])).toEqual(['a', 'c'])
  expect(initials('Ali Valiyev')).toBe('AV')
})

test('a school year: where a day sits on its bar, the line under it, the term of today', () => {
  const y = {
    startDate: '2026-09-02',
    endDate: '2027-05-25',
    status: 'CURRENT',
    progress: 21.4,
    leftDays: 158,
    workDays: 200
  }
  expect(along(y.startDate, y.endDate, '2026-09-02')).toBe(0)
  expect(along(y.startDate, y.endDate, '2027-05-25')).toBe(100)
  expect(along(y.startDate, y.endDate, '2030-01-01')).toBe(100)
  const s = segment(y, { startDate: '2026-11-03', endDate: '2026-11-09' })
  expect(s.left).toBeGreaterThan(20)
  expect(s.width).toBeGreaterThan(0.5)
  expect(progressLine(y, '2026-10-20')).toBe("21% o'tdi · 158 ish kuni qoldi")
  expect(progressLine({ ...y, status: 'FINISHED' }, '2027-06-01')).toBe(
    'Yakunlangan · 200 ish kuni'
  )
  expect(progressLine({ ...y, status: 'PLANNED' }, '2026-08-31')).toBe(
    'Boshlanishiga 2 kun · 200 ish kuni'
  )
  const terms = [
    { name: '1-chorak', startDate: '2026-09-02', endDate: '2026-11-02' },
    { name: "Kuzgi ta'til", startDate: '2026-11-03', endDate: '2026-11-09' }
  ]
  expect(termOn(terms, '2026-10-20').name).toBe('1-chorak')
  expect(termOn(terms, '2026-11-05').name).toBe("Kuzgi ta'til")
  expect(termOn(terms, '2026-12-01')).toBe(null)
})

test('icons: a subject from its name, a position from its title', () => {
  expect(suggestSubjectIcon('Matematika')).toBe('calculate')
  expect(suggestSubjectIcon('Fizika')).toBe('science')
  expect(suggestSubjectIcon('Ona tili')).toBe('menu_book')
  expect(suggestSubjectIcon('Ingliz tili')).toBe('translate')
  expect(suggestSubjectIcon('Noma’lum fan')).toBe('menu_book')
  expect(positionLook('Direktor').icon).toBe('admin_panel_settings')
  expect(positionLook("O'quv ishlari bo'yicha direktor o'rinbosari").icon).toBe(
    'supervisor_account'
  )
  expect(positionLook('Qorovul').icon).toBe('security')
})

test('dragging a term edge stays inside the year, off its neighbours, before its other edge', () => {
  const year = { startDate: '2026-09-02', endDate: '2027-05-25' }
  const terms = [
    { name: 'Q1', startDate: '2026-09-02', endDate: '2026-11-02' },
    { name: 'V1', startDate: '2026-11-03', endDate: '2026-11-09' },
    { name: 'Q2', startDate: '2026-11-10', endDate: '2026-12-28' }
  ]
  expect(isoPlus('2026-11-02', 1)).toBe('2026-11-03')
  expect(isoPlus('2026-03-01', -1)).toBe('2026-02-28')
  expect(daysBetween('2026-09-02', '2026-09-12')).toBe(10)
  // Q1's end cannot pass V1's start, nor its own start
  expect(moveEdge(terms, 0, 'endDate', '2026-11-20', year)).toBe('2026-11-02')
  expect(moveEdge(terms, 0, 'endDate', '2026-10-15', year)).toBe('2026-10-15')
  expect(moveEdge(terms, 0, 'endDate', '2026-08-01', year)).toBe('2026-09-02')
  // Q1's start cannot leave the year
  expect(moveEdge(terms, 0, 'startDate', '2026-08-20', year)).toBe('2026-09-02')
  // Q2's start stops after V1's end; the last end stops at the year's end
  expect(moveEdge(terms, 2, 'startDate', '2026-11-05', year)).toBe('2026-11-10')
  expect(moveEdge(terms, 2, 'endDate', '2027-07-01', year)).toBe('2027-05-25')
  // the order of the list does not matter, the dates do
  const shuffled = [terms[2], terms[0], terms[1]]
  expect(moveEdge(shuffled, 1, 'endDate', '2026-12-01', year)).toBe(
    '2026-11-02'
  )
})

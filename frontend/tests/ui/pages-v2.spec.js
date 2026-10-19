// The v2 pages in a real browser with the test school's real numbers (fixtures written by the
// backend — see pages-v2-mock.js; no backend, no sign-in). 1366 / 1920 / 390, dark and light.
//   PAGES_FIXTURES=/path/fixtures.json UI_BASE=http://localhost:9101 npx playwright test tests/ui/pages-v2.spec.js
import { test, expect } from '@playwright/test'
import { fixtures, hasFixtures, mockContext, open } from './pages-v2-mock.js'

test.skip(!hasFixtures(), 'PAGES_FIXTURES is not set')

const SIZES = [
  { width: 1366, height: 768 },
  { width: 1920, height: 1080 },
  { width: 390, height: 844 }
]

// ---------------------------------------------------------------- every page, every size
for (const path of [
  '/timetable',
  '/calendar',
  '/classes',
  '/classes/1',
  '/rooms'
]) {
  for (const size of SIZES) {
    for (const dark of [true, false]) {
      test(`${path} ${size.width} ${dark ? 'dark' : 'light'}: no sideways scroll, no console errors`, async ({
        browser
      }) => {
        const context = await mockContext(browser, size, { dark })
        const page = await context.newPage()
        const errors = []
        page.on('pageerror', e => errors.push(e.message))
        page.on('console', m => m.type() === 'error' && errors.push(m.text()))
        await open(page, path)
        const over = await page.evaluate(
          () => document.documentElement.scrollWidth - window.innerWidth
        )
        expect(over, 'no horizontal page scroll').toBeLessThanOrEqual(1)
        expect(errors).toEqual([])
        await context.close()
      })
    }
  }
}

// ---------------------------------------------------------------- Dars jadvali
test('timetable: a class week fits 1366x768 with no scroll inside', async ({
  browser
}) => {
  const context = await mockContext(browser, { width: 1366, height: 768 })
  const page = await context.newPage()
  await open(page, '/timetable?view=class')
  const box = page.locator('[data-tt-box]')
  const r = await box.evaluate(el => ({
    bottom: el.getBoundingClientRect().bottom,
    inner: el.scrollHeight - el.clientHeight
  }))
  expect(r.bottom, 'the whole week is on the first screen').toBeLessThanOrEqual(
    768
  )
  expect(r.inner, 'the grid does not scroll inside').toBeLessThanOrEqual(1)
  const periods = fixtures()['/api/pages/timetable/week'].periods.length
  await expect(page.locator('[data-week-grid] .wg__period')).toHaveCount(
    periods
  )
  await context.close()
})

test('timetable: "Bugun" has the red now line; a subject chip dims the others', async ({
  browser
}) => {
  const context = await mockContext(
    browser,
    { width: 1920, height: 1080 },
    { dark: true }
  )
  const page = await context.newPage()
  await open(page, '/timetable?view=today', { at: '10:40' })
  await expect(
    page.locator('[data-today-timeline] [data-now-line]')
  ).toBeVisible()
  await expect(page.locator('[data-now-line]')).toContainText('10:40')
  expect(await page.locator('.tl__chip--now').count()).toBeGreaterThan(0)

  const chip = page.locator('[data-tt-legend] [data-subject]').first()
  await chip.click()
  const dimmed = await page.$$eval('.tl__chip', els => ({
    bright: els.filter(e => !e.classList.contains('tl__chip--dim')).length,
    others: els.filter(e => e.classList.contains('tl__chip--dim')).length
  }))
  expect(dimmed.bright, 'the chosen subject stays bright').toBeGreaterThan(0)
  expect(dimmed.others).toBeGreaterThan(0)
  await chip.click()
  await expect(page.locator('.tl__chip--dim')).toHaveCount(0)
  await context.close()
})

test('timetable: dragging marks busy cells red with the reason and refuses the drop', async ({
  browser
}) => {
  const context = await mockContext(browser, { width: 1920, height: 1080 })
  const page = await context.newPage()
  await open(page, '/timetable?view=class')
  const card = page.locator('[data-week-grid] [data-lesson]').first()
  const bad = page.locator('.wg__cell--bad').first()
  const dt = await page.evaluateHandle(() => new DataTransfer())
  await card.dispatchEvent('dragstart', { dataTransfer: dt })
  await expect(bad).toBeVisible()
  await expect(bad.locator('[data-drop-reason]')).toContainText(/bu vaqtda/)
  await bad.dispatchEvent('dragover', { dataTransfer: dt })
  await bad.dispatchEvent('drop', { dataTransfer: dt })
  await page.waitForTimeout(300)
  expect(
    context.requests.filter(r => r.method === 'PUT'),
    'nothing is saved'
  ).toEqual([])
  await context.close()
})

test('timetable: a drop on a free cell saves and "Bekor qilish" puts it back', async ({
  browser
}) => {
  const context = await mockContext(browser, { width: 1920, height: 1080 })
  const page = await context.newPage()
  await open(page, '/timetable?view=class')
  const card = page.locator('[data-week-grid] [data-lesson]').first()
  const id = await card.getAttribute('data-lesson')
  const dt = await page.evaluateHandle(() => new DataTransfer())
  await card.dispatchEvent('dragstart', { dataTransfer: dt })
  const ok = page.locator('.wg__cell--ok').first()
  await expect(ok).toBeVisible()
  await ok.dispatchEvent('dragover', { dataTransfer: dt })
  await ok.dispatchEvent('drop', { dataTransfer: dt })
  await expect(
    page.locator('.q-notification', { hasText: 'Bekor qilish' })
  ).toBeVisible()
  const puts = () =>
    context.requests.filter(
      r => r.method === 'PUT' && r.path === `/api/lesson-slots/${id}`
    )
  expect(puts()).toHaveLength(1)
  await page
    .locator('.q-notification')
    .getByRole('button', { name: 'Bekor qilish' })
    .click()
  await expect.poll(() => puts().length).toBe(2)
  expect(JSON.parse(puts()[1].body).weekday).toBeTruthy()
  await context.close()
})

test('timetable: substitute flow — free teachers, choose one, saved for that date', async ({
  browser
}) => {
  const context = await mockContext(browser, { width: 1920, height: 1080 })
  const page = await context.newPage()
  const subs = fixtures()['/api/pages/timetable/substitutes']
  await open(page, '/timetable?view=class')
  await page
    .locator(`[data-week-grid] [data-lesson="${subs.lessonSlotId}"]`)
    .first()
    .click()
  await expect(page.locator('[data-lesson-drawer]')).toBeVisible()
  await page.click('[data-find-substitute]')
  const first = page.locator('[data-candidate]').first()
  await expect(first).toBeVisible()
  await first.getByRole('button', { name: 'Tanlash' }).click()
  await expect
    .poll(
      () =>
        context.requests.filter(
          r =>
            r.method === 'POST' &&
            r.path === '/api/pages/timetable/substitutions'
        ).length
    )
    .toBe(1)
  const body = JSON.parse(
    context.requests.find(r => r.path === '/api/pages/timetable/substitutions')
      .body
  )
  expect(body.lessonSlotId).toBe(subs.lessonSlotId)
  expect(body.substituteId).toBe(subs.candidates[0].id)
  expect(body.date).toMatch(/^\d{4}-\d{2}-\d{2}$/)
  await context.close()
})

test('timetable: the conflict KPI opens the list', async ({ browser }) => {
  const context = await mockContext(browser, { width: 1920, height: 1080 })
  const page = await context.newPage()
  await open(page, '/timetable')
  const n = fixtures()['/api/pages/timetable/kpis'].data.conflicts.length
  await page.click('[data-kpi="conflicts"]')
  await expect(page.locator('[data-side-panel] [data-conflict]')).toHaveCount(n)
  await context.close()
})

// ---------------------------------------------------------------- Taqvim
test('calendar: month, week, list and year views; the vacation band; the year line', async ({
  browser
}) => {
  const context = await mockContext(
    browser,
    { width: 1920, height: 1080 },
    { dark: true }
  )
  const page = await context.newPage()
  await open(page, '/calendar?view=month&month=2026-11')
  await expect(page.locator('[data-month-view]')).toBeVisible()
  // the vacation crosses two weeks: a band in each
  await expect(
    page.locator('[data-band]').filter({ hasText: "Kuzgi ta'til" })
  ).toHaveCount(2)
  await expect(page.locator('[data-you-are-here]')).toBeVisible()
  for (const [view, sel] of [
    ['week', '[data-week-view]'],
    ['agenda', '[data-agenda-view]'],
    ['year', '[data-year-view]']
  ]) {
    await page
      .locator('[data-cal-views] button', {
        hasText: { week: 'Hafta', agenda: "Ro'yxat", year: 'Yil' }[view]
      })
      .click()
    await expect(page.locator(sel)).toBeVisible()
  }
  await expect(page.locator('[data-year-view] [data-month]')).toHaveCount(12)
  await context.close()
})

test('calendar: a click on an empty day opens a new event for that day', async ({
  browser
}) => {
  const context = await mockContext(browser, { width: 1920, height: 1080 })
  const page = await context.newPage()
  await open(page, '/calendar?view=month&month=2026-10')
  // the empty part of the day (not its number, not an event)
  await page.locator('[data-date="2026-10-21"]').dispatchEvent('click')
  await expect(page.locator('[data-event-dialog]')).toBeVisible()
  await page.getByRole('textbox', { name: 'Nomi' }).fill('Ochiq dars')
  await page.click('[data-ed-save]')
  await expect
    .poll(
      () =>
        context.requests.filter(
          r => r.method === 'POST' && r.path === '/api/calendar-events'
        ).length
    )
    .toBe(1)
  const body = JSON.parse(
    context.requests.find(
      r => r.path === '/api/calendar-events' && r.method === 'POST'
    ).body
  )
  expect(body).toMatchObject({
    title: 'Ochiq dars',
    startDate: '2026-10-21',
    endDate: '2026-10-21'
  })
  await context.close()
})

test('calendar: dragging an event to another day saves it and can be undone', async ({
  browser
}) => {
  const context = await mockContext(browser, { width: 1920, height: 1080 })
  const page = await context.newPage()
  await open(page, '/calendar?view=month&month=2026-10')
  const ev = fixtures()['/api/calendar-events/range'].find(
    e => e.startDate === '2026-10-25'
  )
  const pill = page.locator(`.mv__pill[data-event="${ev.id}"]`)
  const target = page.locator('[data-date="2026-10-22"]')
  const dt = await page.evaluateHandle(() => new DataTransfer())
  await pill.dispatchEvent('dragstart', { dataTransfer: dt })
  const box = await target.boundingBox()
  const week = target.locator('xpath=..')
  await week.dispatchEvent('dragover', {
    dataTransfer: dt,
    clientX: box.x + box.width / 2,
    clientY: box.y + 20
  })
  await week.dispatchEvent('drop', {
    dataTransfer: dt,
    clientX: box.x + box.width / 2,
    clientY: box.y + 20
  })
  const puts = () =>
    context.requests.filter(
      r => r.method === 'PUT' && r.path === `/api/calendar-events/${ev.id}`
    )
  await expect.poll(() => puts().length).toBe(1)
  expect(JSON.parse(puts()[0].body)).toMatchObject({
    startDate: '2026-10-22',
    endDate: '2026-10-22'
  })
  await page
    .locator('.q-notification')
    .getByRole('button', { name: 'Bekor qilish' })
    .click()
  await expect.poll(() => puts().length).toBe(2)
  expect(JSON.parse(puts()[1].body)).toMatchObject({ startDate: '2026-10-25' })
  await context.close()
})

// ---------------------------------------------------------------- Sinflar
test('classes: grouped by grade, sorted by quality, two compared', async ({
  browser
}) => {
  const context = await mockContext(browser, { width: 1920, height: 1080 })
  const page = await context.newPage()
  await open(page, '/classes')
  const list = fixtures()['/api/pages/classes/overview'].data.classes
  await expect(page.locator('[data-grade-group]')).toHaveCount(
    new Set(list.map(c => c.grade)).size
  )

  await page.locator('[data-classes-sort] button', { hasText: 'Sifat' }).click()
  const best = [...list]
    .filter(c => c.grade === list[0].grade)
    .sort((a, b) => (b.quality ?? -1) - (a.quality ?? -1))[0]
  await expect(
    page
      .locator(`[data-grade-group="${list[0].grade}"] [data-class-card]`)
      .first()
  ).toHaveAttribute('data-class-card', String(best.id))

  const cards = page.locator('[data-class-card]')
  await cards.nth(0).locator('[data-compare-pick]').click()
  await cards.nth(1).locator('[data-compare-pick]').click()
  await page.click('[data-compare-open]')
  await expect(page.locator('[data-compare] [data-metric]')).toHaveCount(3)
  await context.close()
})

test('class page: every tab opens', async ({ browser }) => {
  const context = await mockContext(
    browser,
    { width: 1920, height: 1080 },
    { dark: true }
  )
  const page = await context.newPage()
  await open(page, '/classes/1')
  await expect(page.locator('[data-class-hero]')).toBeVisible()
  for (const tab of [
    'students',
    'schedule',
    'attendance',
    'parents',
    'behavior'
  ]) {
    await page.click(`[data-class-tabs] [data-tab="${tab}"]`)
    await expect(page.locator(`[data-tab-body="${tab}"]`)).toBeVisible()
  }
  await context.close()
})

// ---------------------------------------------------------------- Xonalar
test("rooms: the live map shows the server's states; the finder lists free rooms", async ({
  browser
}) => {
  const context = await mockContext(browser, { width: 1920, height: 1080 })
  const page = await context.newPage()
  await open(page, '/rooms')
  const live = fixtures()['/api/pages/rooms/live@lesson']
  await expect(page.locator('[data-room][data-state="BUSY"]')).toHaveCount(
    live.buildings
      .flatMap(b => b.floors)
      .flatMap(f => f.rooms)
      .filter(r => r.state === 'BUSY').length
  )
  await expect(page.locator('[data-room][data-state="REPAIR"]')).toHaveCount(
    live.kpis.repair
  )
  await page.click('[data-open-finder]')
  await expect(page.locator('[data-finder-room]')).toHaveCount(
    fixtures()['/api/pages/rooms/find'].rooms.length
  )
  await context.close()
})

test("rooms: a booking that clashes shows the server's reason", async ({
  browser
}) => {
  const context = await mockContext(browser, { width: 1920, height: 1080 })
  context.overrides['POST /api/pages/rooms/bookings'] = {
    status: 409,
    body: '204-xona bu vaqtda band: 7-B, Fizika (10:20–11:05)'
  }
  const page = await context.newPage()
  await open(page, '/rooms')
  await page.click('[data-open-finder]')
  await page.locator('[data-finder-book]').first().click()
  await page
    .getByRole('textbox', { name: /Maqsad/ })
    .fill("Ota-onalar yig'ilishi")
  await page.click('[data-booking-save]')
  await expect(page.locator('[data-booking-error]')).toContainText(
    'bu vaqtda band'
  )
  await context.close()
})

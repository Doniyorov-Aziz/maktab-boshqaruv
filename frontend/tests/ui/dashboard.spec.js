// The home page v3 (npm test): equal-height rows with no holes, nothing drawn outside its
// card, today's strip ≤ 180 px with grades 1…11 in one row, 20+ class bubbles, 3+ students at
// risk, a signal opens the side panel, no green check next to "no data", count-up and rings
// switched off under reduced motion, links with the right filter, no NaN in sparklines,
// chart colours that follow the theme, the demo banner, a manual mark the automation leaves
// alone, and the teacher's own page. Admin and teacher, light and dark, 1366 / 1920 / 2560 / 390.
import { test, expect } from '@playwright/test'
import { openContext, visit, login, API } from './session.js'

const TEACHER = process.env.UI_TEACHER_USER || 'ui_teacher'

/** Rows of the grid (items with the same top), each with heights and the space it leaves. */
function gridRows() {
  const grid = document.querySelector('.dash')
  const style = getComputedStyle(grid)
  const gap = parseFloat(style.columnGap)
  const width = grid.getBoundingClientRect().width
  const items = [...grid.querySelectorAll(':scope > div')].filter(el => {
    const r = el.getBoundingClientRect()
    return r.width > 0 && r.height > 0
  })
  const rows = new Map()
  for (const el of items) {
    const r = el.getBoundingClientRect()
    const key = Math.round(r.top)
    if (!rows.has(key)) rows.set(key, [])
    rows.get(key).push({
      w: r.width,
      h: r.height,
      name: el.className.split(' ')[0] || el.tagName
    })
  }
  return [...rows.values()].map(row => ({
    names: row.map(x => x.name),
    heights: row.map(x => Math.round(x.h)),
    leftover: Math.round(
      width - row.reduce((a, x) => a + x.w, 0) - gap * (row.length - 1)
    )
  }))
}

/** Every canvas / svg inside a card must stay inside it. */
function overflowing() {
  const out = []
  for (const g of document.querySelectorAll('.dash canvas, .dash svg')) {
    const card = g.closest('.dash-card')
    if (!card) continue
    const a = g.getBoundingClientRect()
    const c = card.getBoundingClientRect()
    if (a.width === 0) continue
    if (
      a.left < c.left - 1 ||
      a.right > c.right + 1 ||
      a.top < c.top - 1 ||
      a.bottom > c.bottom + 1
    ) {
      out.push(
        `${card.querySelector('.chart-card__title, .kpi__name, .today__title')?.textContent?.trim()}: ${g.tagName}`
      )
    }
  }
  return out
}

async function loaded(page) {
  await page.waitForSelector('.dash .chart-card')
  await page.waitForSelector('[data-widget="class-map"] canvas')
  await page.waitForTimeout(1800)
}

for (const { width, height, dark } of [
  { width: 1366, height: 768, dark: true },
  { width: 1920, height: 1080, dark: true },
  { width: 1920, height: 1080, dark: false },
  { width: 2560, height: 1440, dark: true }
]) {
  test(`admin ${width}${dark ? ' dark' : ' light'}: equal rows, no holes, nothing overflows, today strip`, async ({
    browser
  }) => {
    const context = await openContext(browser, { width, height }, { dark })
    const page = await context.newPage()
    const errors = []
    page.on('pageerror', e => errors.push(e.message))
    page.on('console', m => m.type() === 'error' && errors.push(m.text()))
    await visit(page, '/')
    await loaded(page)
    for (const row of await page.evaluate(gridRows)) {
      const max = Math.max(...row.heights)
      const min = Math.min(...row.heights)
      expect(
        max - min,
        `row ${row.names.join(', ')} heights ${row.heights.join('/')}`
      ).toBeLessThanOrEqual(2)
      expect(
        row.leftover,
        `row ${row.names.join(', ')} leaves ${row.leftover}px`
      ).toBeLessThanOrEqual(2)
    }
    expect(await page.evaluate(overflowing)).toEqual([])

    // 6.2: at most 180 px, grades 1…11 side by side in one row
    const strip = await page.$eval('[data-widget="today"]', el => {
      const cols = [...el.querySelectorAll('.today__col')]
      return {
        h: el.getBoundingClientRect().height,
        cols: cols.length,
        tops: new Set(cols.map(c => Math.round(c.getBoundingClientRect().top)))
          .size
      }
    })
    expect(strip.h).toBeLessThanOrEqual(180)
    expect(strip.cols).toBe(11)
    expect(strip.tops, 'all 11 grade columns in one row').toBe(1)

    expect(errors, errors.join('\n')).toEqual([])
    await context.close()
  })
}

test('admin 390: one column, no sideways scroll, charts drawn', async ({
  browser
}) => {
  const context = await openContext(
    browser,
    { width: 390, height: 844 },
    { dark: true }
  )
  const page = await context.newPage()
  const errors = []
  page.on('pageerror', e => errors.push(e.message))
  await visit(page, '/')
  await loaded(page)
  const w = await page.evaluate(() => document.documentElement.scrollWidth)
  expect(w).toBeLessThanOrEqual(390)
  const columns = await page.evaluate(
    () =>
      new Set(
        [...document.querySelectorAll('.dash > div')].map(el =>
          Math.round(el.getBoundingClientRect().left)
        )
      ).size
  )
  expect(columns, 'one column').toBe(1)
  expect(errors, errors.join('\n')).toEqual([])
  await context.close()
})

test('20+ class bubbles, 3+ students at risk, a signal opens its side panel', async ({
  browser
}) => {
  const context = await openContext(
    browser,
    { width: 1920, height: 1080 },
    { dark: true }
  )
  const page = await context.newPage()
  await visit(page, '/')
  await loaded(page)
  const bubbles = Number(
    await page.getAttribute(
      '[data-widget="class-map"] [data-points]',
      'data-points'
    )
  )
  expect(bubbles).toBeGreaterThanOrEqual(20)
  expect(await page.locator('[data-risk-row]').count()).toBeGreaterThanOrEqual(
    3
  )

  const signal = page.locator('[data-signal]:not([disabled])').first()
  await expect(signal).toBeVisible()
  await signal.click()
  const panel = page.locator('[data-side-panel]')
  await expect(panel).toBeVisible()
  expect(await panel.locator('.panel-item').count()).toBeGreaterThan(0)
  await page.keyboard.press('Escape')
  await expect(panel).toHaveCount(0)
  await context.close()
})

test('no green check next to "no data"; a zero signal is a quiet check', async ({
  browser
}) => {
  const context = await openContext(browser, { width: 1920, height: 1080 })
  const page = await context.newPage()
  await visit(page, '/')
  await loaded(page)
  const bad = await page.evaluate(() => {
    const out = []
    for (const el of document.querySelectorAll('.dash *')) {
      if (el.children.length) continue
      if (!/ma'lumot yo'q/i.test(el.textContent || '')) continue
      const box =
        el.closest('.chart-card__state, .chart-card, .dash-card') ||
        el.parentElement
      if (
        /✅/.test(box.textContent) ||
        box.querySelector('.chart-card__state-icon--ok')
      )
        out.push(box.textContent.trim().slice(0, 80))
    }
    return out
  })
  expect(bad).toEqual([])
  expect(await page.locator('.dash').innerText()).not.toContain('✅')
  await context.close()
})

test('count-up and rings stop under prefers-reduced-motion', async ({
  browser
}) => {
  const reduced = await openContext(
    browser,
    { width: 1920, height: 1080 },
    { reducedMotion: 'reduce' }
  )
  const page = await reduced.newPage()
  await visit(page, '/')
  await page.waitForSelector('[data-ring="multi"] .mring__arc')
  const r = await page.evaluate(() => ({
    ring: getComputedStyle(document.querySelector('.mring__arc'))
      .transitionDuration,
    card: getComputedStyle(document.querySelector('.dash-card')).animationName
  }))
  expect(r.ring).toBe('0s')
  expect(r.card).toBe('none')
  // the number is final at once: it does not change after a moment
  const first = await page
    .locator('[data-widget="health"] .dash-num')
    .first()
    .innerText()
  await page.waitForTimeout(700)
  expect(
    await page.locator('[data-widget="health"] .dash-num').first().innerText()
  ).toBe(first)
  await reduced.close()

  const normal = await openContext(browser, { width: 1920, height: 1080 })
  const p2 = await normal.newPage()
  await visit(p2, '/')
  await p2.waitForSelector('[data-ring="multi"] .mring__arc')
  expect(
    await p2.evaluate(
      () =>
        getComputedStyle(document.querySelector('.mring__arc'))
          .transitionDuration
    )
  ).toBe('0.8s')
  await normal.close()
})

test('KPI and tiles open the right page with the right filter', async ({
  browser
}) => {
  const context = await openContext(browser, { width: 1920, height: 1080 })
  const page = await context.newPage()
  await visit(page, '/')
  await page.waitForSelector('[data-kpi="attendance"]')
  expect(await page.getAttribute('[data-kpi="attendance"]', 'href')).toContain(
    '/attendance/calendar'
  )
  expect(await page.getAttribute('[data-kpi="mastery"]', 'href')).toContain(
    '/gradebook'
  )
  const tile = page.locator('.tile').first()
  const classId = await tile.getAttribute('data-class-id')
  expect(await tile.getAttribute('href')).toMatch(
    new RegExp(`/attendance/calendar\\?.*class=${classId}`)
  )
  await page.locator('[data-kpi="mastery"]').click()
  await expect(page).toHaveURL(/gradebook/)
  await context.close()
})

test('sparklines have no NaN, the time left is said once', async ({
  browser
}) => {
  const context = await openContext(browser, { width: 1920, height: 1080 })
  const page = await context.newPage()
  await visit(page, '/')
  await page.waitForSelector('.kpi .sparkline')
  await page.waitForTimeout(1000)
  const paths = await page.$$eval('.sparkline path, .sparkline circle', els =>
    els.map(
      e =>
        (e.getAttribute('d') || '') +
        (e.getAttribute('cx') || '') +
        (e.getAttribute('cy') || '')
    )
  )
  expect(paths.filter(p => p.includes('NaN'))).toEqual([])
  const text = await page.locator('.dash').innerText()
  expect((text.match(/tugashiga/g) || []).length).toBeLessThanOrEqual(1)
  expect(
    (text.match(/daq qoldi/g) || []).length,
    'no "N daq qoldi" on every row'
  ).toBe(0)
  await context.close()
})

test('chart colours follow the theme', async ({ browser }) => {
  const context = await openContext(
    browser,
    { width: 1920, height: 1080 },
    { dark: false }
  )
  const page = await context.newPage()
  await visit(page, '/')
  await page.waitForSelector('.dyn canvas')
  await page.waitForTimeout(1200)
  const before = await page.$eval('.dyn canvas', c => c.toDataURL())
  const tokenBefore = await page.evaluate(() =>
    getComputedStyle(document.body).getPropertyValue('--chart-primary').trim()
  )
  await page.locator('header button').filter({ hasText: 'dark_mode' }).click()
  await page.waitForTimeout(1200)
  const tokenAfter = await page.evaluate(() =>
    getComputedStyle(document.body).getPropertyValue('--chart-primary').trim()
  )
  const after = await page.$eval('.dyn canvas', c => c.toDataURL())
  expect(tokenAfter).not.toEqual(tokenBefore)
  expect(after).not.toEqual(before)
  await context.close()
})

test('demo mode: the yellow banner and 🤖 marks are shown', async ({
  browser
}) => {
  const info = await (
    await fetch(API + '/api/system/info', {
      headers: { Authorization: 'Bearer ' + (await login()) }
    })
  ).json()
  test.skip(!info.demoSimulator, 'the backend runs without demo mode')
  const context = await openContext(browser, { width: 1920, height: 1080 })
  const page = await context.newPage()
  await visit(page, '/')
  await expect(page.getByTestId('demo-banner')).toContainText('baholar')
  await page.waitForSelector('[data-widget="kpis"] .kpi__note')
  expect(await page.locator('[data-widget="kpis"]').innerText()).toContain('🤖')
  await context.close()
})

test('a manual mark is still manual after the automation ran', async () => {
  test.setTimeout(150_000)
  const token = await login()
  const h = {
    Authorization: 'Bearer ' + token,
    'Content-Type': 'application/json'
  }
  const lessons = await (
    await fetch(API + '/api/attendance/today-lessons?schoolId=1', {
      headers: h
    })
  ).json()
  test.skip(!lessons.length, 'no lessons today')
  const lesson = lessons[lessons.length - 1]
  const date = new Date().toISOString().slice(0, 10)
  const roster = await (
    await fetch(
      `${API}/api/attendance/roster?lessonSlotId=${lesson.lessonSlotId}&date=${date}`,
      { headers: h }
    )
  ).json()
  const student = roster[0]
  await fetch(API + '/api/attendance/bulk', {
    method: 'POST',
    headers: h,
    body: JSON.stringify({
      lessonSlotId: lesson.lessonSlotId,
      recordDate: date,
      entries: [{ studentId: student.studentId, status: 'ABSENT' }]
    })
  })
  // the scheduler runs every minute
  await new Promise(r => setTimeout(r, 65_000))
  const after = await (
    await fetch(
      `${API}/api/attendance/roster?lessonSlotId=${lesson.lessonSlotId}&date=${date}`,
      { headers: h }
    )
  ).json()
  const mine = after.find(r => r.studentId === student.studentId)
  expect(mine.status).toBe('ABSENT')
  expect(mine.source).toBe('MANUAL')
})

test('teacher gets the teacher page; the school page is not shown', async ({
  browser
}) => {
  const r = await fetch(API + '/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: TEACHER, password: process.env.UI_PASS })
  })
  test.skip(!r.ok, `no ${TEACHER} user`)
  const t = (await r.text()).trim()
  const context = await browser.newContext({
    viewport: { width: 1920, height: 1080 }
  })
  await context.addInitScript(tok => {
    const c = JSON.parse(
      atob(tok.split('.')[1].replace(/-/g, '+').replace(/_/g, '/'))
    )
    localStorage.setItem('token', tok)
    localStorage.setItem('username', c.sub)
    localStorage.setItem('role', c.role)
    if (c.employeeId) localStorage.setItem('employeeId', String(c.employeeId))
    localStorage.setItem('activeSchoolId', '1')
  }, t)
  const page = await context.newPage()
  await visit(page, '/')
  await expect(page.locator('[data-dash="teacher"]')).toBeVisible()
  await expect(page.locator('[data-dash="school"]')).toHaveCount(0)
  await expect(page.getByText('Bugungi darslarim')).toBeVisible()
  await expect(page.locator('[data-widget="my-index"]')).toBeVisible()
  await expect(
    page.getByText("Sinflarimdagi xavf ostidagi o'quvchilar")
  ).toBeVisible()
  for (const row of await page.evaluate(gridRows)) {
    expect(
      Math.max(...row.heights) - Math.min(...row.heights)
    ).toBeLessThanOrEqual(2)
  }
  await context.close()
})

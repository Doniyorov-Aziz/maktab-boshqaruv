// The home page (npm test): equal-height rows with no holes, nothing drawn outside its card,
// links that land on the right page with the right filter, no NaN in sparklines, the time left
// said once, chart colours that follow the theme, the demo banner and 🤖 marks, and a manual
// mark that the automation leaves alone. Admin and teacher, light and dark, 1366 / 1920 / 2560 / 390.
import { test, expect } from '@playwright/test'
import { openContext, visit, login, API } from './session.js'

const TEACHER = process.env.UI_TEACHER_USER || 'ui_teacher'

/** Rows of the grid (items with the same top), each with heights and the space it leaves. */
function gridRows() {
  const grid = document.querySelector('.dash')
  const style = getComputedStyle(grid)
  const gap = parseFloat(style.columnGap)
  const width = grid.getBoundingClientRect().width
  const items = [...grid.querySelectorAll(':scope > div, :scope .kpi')].filter(
    el => {
      const r = el.getBoundingClientRect()
      return r.width > 0 && r.height > 0
    }
  )
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
    const card = g.closest('.chart-card, .kpi')
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
        `${card.querySelector('.chart-card__title, .kpi__name')?.textContent?.trim()}: ${g.tagName}`
      )
    }
  }
  return out
}

for (const { width, height, dark } of [
  { width: 1366, height: 768, dark: false },
  { width: 1920, height: 1080, dark: true },
  { width: 2560, height: 1440, dark: true }
]) {
  test(`admin layout ${width}${dark ? ' dark' : ''}: equal rows, no holes, nothing overflows`, async ({
    browser
  }) => {
    const context = await openContext(browser, { width, height }, { dark })
    const page = await context.newPage()
    const errors = []
    page.on('pageerror', e => errors.push(e.message))
    page.on('console', m => m.type() === 'error' && errors.push(m.text()))
    await visit(page, '/')
    await page.waitForSelector('.dash .chart-card')
    await page.waitForTimeout(1500)
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
    expect(errors, errors.join('\n')).toEqual([])
    await context.close()
  })
}

test('admin 390: one column, no sideways scroll', async ({ browser }) => {
  const context = await openContext(browser, { width: 390, height: 844 })
  const page = await context.newPage()
  await visit(page, '/')
  await page.waitForSelector('.dash .chart-card')
  await page.waitForTimeout(1500)
  const w = await page.evaluate(() => document.documentElement.scrollWidth)
  expect(w).toBeLessThanOrEqual(390)
  await context.close()
})

test('KPI, attention and map tiles open the right page with the right filter', async ({
  browser
}) => {
  const context = await openContext(browser, { width: 1920, height: 1080 })
  const page = await context.newPage()
  await visit(page, '/')
  await page.waitForSelector('[data-kpi="attendance"]')
  expect(await page.getAttribute('[data-kpi="attendance"]', 'href')).toContain(
    '/attendance/calendar'
  )
  expect(await page.getAttribute('[data-kpi="appeals"]', 'href')).toContain(
    '/parent-messages'
  )
  expect(await page.getAttribute('[data-kpi="mastery"]', 'href')).toContain(
    '/gradebook'
  )

  const tile = page.locator('.heat').first()
  const classId = await tile.getAttribute('data-class-id')
  expect(await tile.getAttribute('href')).toMatch(
    new RegExp(`/attendance/calendar\\?.*class=${classId}`)
  )

  const actions = await page.$$eval('.att__group', groups =>
    groups.flatMap(g =>
      [...g.querySelectorAll('.att__action')].map(a => ({
        kind: g.dataset.kind,
        href: a.getAttribute('href')
      }))
    )
  )
  for (const a of actions) {
    if (a.kind === 'UNMARKED_LESSON')
      expect(a.href).toMatch(
        /\/attendance\?lessonSlotId=\d+&date=\d{4}-\d{2}-\d{2}/
      )
    if (a.kind === 'ABSENT_STREAK')
      expect(a.href).toMatch(/\/profiles\/student\/\d+/)
    if (a.kind === 'APPEAL_OVERDUE')
      expect(a.href).toMatch(/\/parent-messages\?id=\d+/)
  }
  // a click really lands there
  await page.locator('[data-kpi="appeals"]').click()
  await expect(page).toHaveURL(/parent-messages/)
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
  const said = (text.match(/tugashiga/g) || []).length
  expect(said).toBeLessThanOrEqual(1)
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
  await page.waitForSelector('.trend canvas')
  await page.waitForTimeout(1200)
  const before = await page.$eval('.trend canvas', c => c.toDataURL())
  const tokenBefore = await page.evaluate(() =>
    getComputedStyle(document.body).getPropertyValue('--chart-primary').trim()
  )
  await page.locator('header button').filter({ hasText: 'dark_mode' }).click()
  await page.waitForTimeout(1200)
  const tokenAfter = await page.evaluate(() =>
    getComputedStyle(document.body).getPropertyValue('--chart-primary').trim()
  )
  const after = await page.$eval('.trend canvas', c => c.toDataURL())
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
  test.skip(!info.demoSimulator, 'the backend runs without the demo simulator')
  const context = await openContext(browser, { width: 1920, height: 1080 })
  const page = await context.newPage()
  await visit(page, '/')
  await expect(page.getByTestId('demo-banner')).toBeVisible()
  await page.waitForSelector('.heat')
  expect(await page.locator('.heat__auto').count()).toBeGreaterThan(0)
  await context.close()
})

test('a manual mark is still manual after the automation ran', async ({
  browser
}) => {
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
  // a teacher marks this student absent in a lesson the simulator has not filled yet
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

  const context = await openContext(browser, { width: 1366, height: 900 })
  const page = await context.newPage()
  await visit(
    page,
    `/attendance?lessonSlotId=${lesson.lessonSlotId}&date=${date}`
  )
  await page.waitForSelector('.roster-item')
  const firstRow = page.locator('.roster-item').first()
  await expect(firstRow.locator('.source-mark')).toHaveCount(0)
  await context.close()
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
  await context.close()
})

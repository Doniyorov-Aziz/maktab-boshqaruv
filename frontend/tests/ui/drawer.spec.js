// The side panel (SideDrawer) on the home page: the list scrolls to its very last row with
// the wheel (and with a finger on a phone), the page behind never moves, the header and the
// filters stay put, search and the class filter change the count, Esc closes the panel and
// the focus goes back to the button that opened it — and every other side panel scrolls too.
// 1440 × 900 and 390 × 844, dark and light.
import { fileURLToPath } from 'node:url'
import { test, expect } from '@playwright/test'
import { openContext, visit, login, API } from './session.js'

async function riskRows() {
  const token = await login()
  const r = await fetch(`${API}/api/dashboard/risk?schoolId=1&limit=2000`, {
    headers: { Authorization: 'Bearer ' + token }
  })
  const rows = (await r.json()).data.rows
  // the panel's default order: risk score, then name
  return [...rows].sort(
    (a, b) => b.score - a.score || a.name.localeCompare(b.name)
  )
}

async function openRisk(page) {
  await visit(page, '/')
  await page.waitForSelector('[data-risk-all]')
  await page.click('[data-risk-all]')
  await page.waitForSelector('[data-side-panel] [data-risk-panel-row]')
  await page.waitForTimeout(400)
}

const pageScroll = page =>
  page.evaluate(() => ({
    win: window.scrollY,
    doc: document.scrollingElement.scrollTop
  }))

const atBottom = page =>
  page.$eval(
    '[data-drawer-body]',
    b => b.scrollTop + b.clientHeight >= b.scrollHeight - 2
  )

for (const { width, height, dark } of [
  { width: 1440, height: 900, dark: true },
  { width: 1440, height: 900, dark: false },
  { width: 390, height: 844, dark: true },
  { width: 390, height: 844, dark: false }
]) {
  test(`risk panel ${width}${dark ? ' dark' : ' light'}: the wheel reaches the last student, the page stays`, async ({
    browser
  }) => {
    const rows = await riskRows()
    test.skip(rows.length < 20, 'too few students at risk to need a scroll')
    const last = rows[rows.length - 1]
    const context = await openContext(browser, { width, height }, { dark })
    const page = await context.newPage()
    await openRisk(page)
    const before = await pageScroll(page)
    const box = await page.locator('[data-drawer-body]').boundingBox()
    await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2)
    for (let i = 0; i < 60 && !(await atBottom(page)); i++) {
      await page.mouse.wheel(0, 600)
      await page.waitForTimeout(60)
    }
    expect(await atBottom(page), 'the list scrolled to its end').toBe(true)
    await expect(
      page.locator('[data-risk-panel-row]', { hasText: last.name }).last()
    ).toBeVisible()
    expect(await pageScroll(page), 'the page behind did not move').toEqual(
      before
    )
    // the header and the filters are still on screen
    await expect(page.locator('[data-side-panel] h3')).toBeInViewport()
    await expect(page.locator('[data-drawer-toolbar]')).toBeInViewport()
    await context.close()
  })
}

test('risk panel 390: a finger swipe reaches the last student', async ({
  browser
}) => {
  const rows = await riskRows()
  test.skip(rows.length < 20, 'too few students at risk to need a scroll')
  const last = rows[rows.length - 1]
  const context = await openContext(
    browser,
    { width: 390, height: 844 },
    { dark: true, touch: true }
  )
  const page = await context.newPage()
  await openRisk(page)
  const before = await pageScroll(page)
  const box = await page.locator('[data-drawer-body]').boundingBox()
  const cdp = await context.newCDPSession(page)
  for (let i = 0; i < 40 && !(await atBottom(page)); i++) {
    await cdp.send('Input.synthesizeScrollGesture', {
      x: Math.round(box.x + box.width / 2),
      y: Math.round(box.y + box.height * 0.8),
      yDistance: -Math.round(box.height * 0.7),
      gestureSourceType: 'touch',
      speed: 3000
    })
  }
  expect(await atBottom(page)).toBe(true)
  await expect(
    page.locator('[data-risk-panel-row]', { hasText: last.name }).last()
  ).toBeVisible()
  expect(await pageScroll(page)).toEqual(before)
  await context.close()
})

test('risk panel: search and class filter change the count; levels have their colours', async ({
  browser
}) => {
  const rows = await riskRows()
  test.skip(!rows.length, 'nobody at risk')
  const context = await openContext(
    browser,
    { width: 1440, height: 900 },
    { dark: true }
  )
  const page = await context.newPage()
  await openRisk(page)
  const count = page.locator('[data-risk-count]')
  await expect(count).toContainText(`${rows.length} tadan ${rows.length} tasi`)

  // search by part of a surname (250 ms debounce)
  const part = rows[0].name.split(' ')[1].slice(0, 4).toLowerCase()
  const expected = rows.filter(r => r.name.toLowerCase().includes(part)).length
  await page.locator('[data-risk-search] input').fill(part)
  await expect(count).toContainText(`${rows.length} tadan ${expected} tasi`)
  await page.locator('[data-risk-search] input').fill('')
  await expect(count).toContainText(`${rows.length} tadan ${rows.length} tasi`)

  // one class
  const cls = rows[0]
  const inClass = rows.filter(r => r.classId === cls.classId).length
  await page.locator('[data-risk-class]').click()
  await page
    .locator('.q-menu .q-item', { hasText: new RegExp(`^${cls.className}$`) })
    .click()
  await expect(count).toContainText(`${rows.length} tadan ${inClass} tasi`)

  // nothing found → a way back
  await page.locator('[data-risk-search] input').fill('zzzzzz')
  await expect(page.locator('[data-risk-empty]')).toContainText('topilmadi')
  await page.getByRole('button', { name: 'Filtrni tozalash' }).click()
  await expect(count).toContainText(`${rows.length} tadan ${rows.length} tasi`)

  // ring colour by level
  const levels = await page.$$eval('[data-side-panel] .score', els => [
    ...new Set(els.map(e => e.dataset.level))
  ])
  for (const l of levels) expect(['high', 'mid', 'low']).toContain(l)
  if (rows.some(r => r.score >= 70)) expect(levels).toContain('high')
  if (rows.some(r => r.score >= 50 && r.score < 70))
    expect(levels).toContain('mid')
  await context.close()
})

test('risk panel: Esc closes it and the focus returns to the button', async ({
  browser
}) => {
  const context = await openContext(browser, { width: 1440, height: 900 })
  const page = await context.newPage()
  await openRisk(page)
  // the list has the focus: PageDown scrolls it
  const top = await page.$eval('[data-drawer-body]', b => b.scrollTop)
  await page.keyboard.press('PageDown')
  await page.waitForTimeout(300)
  expect(
    await page.$eval('[data-drawer-body]', b => b.scrollTop)
  ).toBeGreaterThan(top)
  await page.keyboard.press('Escape')
  await expect(page.locator('[data-side-panel]')).toHaveCount(0)
  await expect(page.locator('[data-risk-all]')).toBeFocused()
  await context.close()
})

test('every signal panel scrolls to its last row', async ({ browser }) => {
  const context = await openContext(
    browser,
    { width: 1440, height: 900 },
    { dark: true }
  )
  const page = await context.newPage()
  await visit(page, '/')
  await page.waitForSelector('[data-signal]')
  const kinds = await page.$$eval('[data-signal]:not([disabled])', els =>
    els.map(e => e.dataset.signal)
  )
  expect(kinds.length).toBeGreaterThan(0)
  for (const kind of kinds) {
    await page.click(`[data-signal="${kind}"]`)
    await page.waitForSelector('[data-side-panel] .panel-item')
    const box = await page.locator('[data-drawer-body]').boundingBox()
    await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2)
    for (let i = 0; i < 40 && !(await atBottom(page)); i++) {
      await page.mouse.wheel(0, 600)
      await page.waitForTimeout(50)
    }
    expect(await atBottom(page), `${kind}: scrolled to the end`).toBe(true)
    await expect(
      page.locator('[data-side-panel] .panel-item').last(),
      kind
    ).toBeInViewport()
    await page.keyboard.press('Escape')
    await expect(page.locator('[data-side-panel]')).toHaveCount(0)
  }
  await context.close()
})

// the form panels (CrudPage) are SideDrawers too: on a short screen the form scrolls to its
// last field and the "Saqlash" footer stays on screen
const FORMS = [
  { key: 'lesson-slots', name: 'Dars jadvali' },
  { key: 'calendar-events', name: 'Taqvim' },
  { key: 'rooms', name: 'Xonalar' },
  { key: 'students', name: "O'quvchilar" },
  { key: 'employees', name: 'Xodimlar' }
]
for (const { key, name } of FORMS) {
  test(`form panel "${name}" scrolls to its last field`, async ({
    browser
  }) => {
    const context = await openContext(
      browser,
      { width: 1440, height: 480 },
      { dark: true }
    )
    const page = await context.newPage()
    await visit(page, `/app/${key}`)
    await page.getByRole('button', { name: "Yangi qo'shish" }).first().click()
    await page.waitForSelector('[data-side-panel] [data-crud-form]')
    await page.waitForTimeout(400)
    const before = await pageScroll(page)
    const box = await page.locator('[data-drawer-body]').boundingBox()
    await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2)
    for (let i = 0; i < 30 && !(await atBottom(page)); i++) {
      await page.mouse.wheel(0, 400)
      await page.waitForTimeout(50)
    }
    expect(await atBottom(page), `${name}: scrolled to the end`).toBe(true)
    await expect(
      page.locator('[data-crud-form] .q-field').last(),
      name
    ).toBeInViewport()
    await expect(
      page.locator('[data-side-panel]').getByRole('button', { name: 'Saqlash' })
    ).toBeInViewport()
    await expect(page.locator('[data-side-panel] h3')).toBeInViewport()
    expect(await pageScroll(page)).toEqual(before)
    await context.close()
  })
}

test('library parent panel scrolls to its last download', async ({
  browser
}) => {
  const context = await openContext(
    browser,
    { width: 1440, height: 600 },
    { dark: true }
  )
  const page = await context.newPage()
  await visit(page, '/library/stats')
  // the journal's parent cell (the page has two tables)
  const row = page.locator('.q-table tbody td.cursor-pointer').first()
  await row.waitFor({ timeout: 15000 }).catch(() => {})
  test.skip(!(await row.count()), 'no downloads in the journal')
  await row.click()
  await page.waitForSelector('[data-side-panel] [data-parent-panel]')
  await page.waitForTimeout(400)
  const box = await page.locator('[data-drawer-body]').boundingBox()
  await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2)
  for (let i = 0; i < 40 && !(await atBottom(page)); i++) {
    await page.mouse.wheel(0, 600)
    await page.waitForTimeout(50)
  }
  expect(await atBottom(page)).toBe(true)
  const items = page.locator('[data-parent-panel] .q-item')
  if (await items.count()) await expect(items.last()).toBeInViewport()
  await page.keyboard.press('Escape')
  await expect(page.locator('[data-side-panel]')).toHaveCount(0)
  await context.close()
})

// docs/images/dashboard/risk-drawer-{top,bottom,mobile}.png
const SHOTS = new URL('../../../docs/images/dashboard/', import.meta.url)
test('risk panel screenshots', async ({ browser }) => {
  for (const { size, file, bottom } of [
    { size: { width: 1440, height: 900 }, file: 'risk-drawer-top.png' },
    {
      size: { width: 1440, height: 900 },
      file: 'risk-drawer-bottom.png',
      bottom: true
    },
    { size: { width: 390, height: 844 }, file: 'risk-drawer-mobile.png' }
  ]) {
    const context = await openContext(browser, size, {
      dark: true,
      reducedMotion: 'reduce'
    })
    const page = await context.newPage()
    await openRisk(page)
    if (bottom) {
      const box = await page.locator('[data-drawer-body]').boundingBox()
      await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2)
      for (let i = 0; i < 60 && !(await atBottom(page)); i++) {
        await page.mouse.wheel(0, 600)
        await page.waitForTimeout(60)
      }
      await page.waitForTimeout(300)
    }
    await page.screenshot({ path: fileURLToPath(new URL(file, SHOTS)) })
    await context.close()
  }
})

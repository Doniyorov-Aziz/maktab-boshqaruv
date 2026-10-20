// Maktablar, Binolar and O'quv yillari at the home page's level: big cards for a short list that
// fill the row, names that are not cut, no empty filter panel, no pager for one page, the table /
// cards switch in the header, buildings busy above 0%, the current year 2026–2027 as a hero with
// its terms, "Bugun", attendance and quality; the missing year, the term checks. Each at
// 1920 light / 1920 dark / 1366 / 390, with the screenshots docs/images/crud-v2/fix-*.png.
//   CRUD_FIXTURES=…/crud.json UI_BASE=http://localhost:9103 npx playwright test tests/ui/crud-fix.spec.js
import { test, expect } from '@playwright/test'
import { hasFixtures, fixtures, mockContext, open } from './crud-mock.js'

test.skip(!hasFixtures(), 'CRUD_FIXTURES is not set')

const SIZES = [
  ['1920 light', { width: 1920, height: 1080 }, false],
  ['1920 dark', { width: 1920, height: 1080 }, true],
  ['1366', { width: 1366, height: 768 }, false],
  ['390', { width: 390, height: 844 }, true]
]
const WIDE = SIZES[0][1]
const SHOTS = '../docs/images/crud-v2'

async function visit(browser, path, size = WIDE, opts = {}) {
  const context = await mockContext(browser, size, opts)
  const page = await context.newPage()
  page.errors = []
  page.on('pageerror', e => page.errors.push(String(e)))
  page.on('console', m => m.type() === 'error' && page.errors.push(m.text()))
  await open(page, path)
  return { context, page }
}

/** No horizontal scroll, and the right edge of the last card in a row is within 24px of the grid's. */
async function fillsTheRow(page, cards) {
  const over = await page.evaluate(
    () => document.documentElement.scrollWidth - innerWidth
  )
  expect(over).toBeLessThanOrEqual(0)
  const grid = await page.locator('[data-crud-cards]').boundingBox()
  const boxes = []
  for (const c of await page.locator(cards).all())
    boxes.push(await c.boundingBox())
  const right = Math.max(...boxes.map(b => b.x + b.width))
  expect(grid.x + grid.width - right).toBeLessThan(24)
  expect(Math.min(...boxes.map(b => b.x)) - grid.x).toBeLessThan(2)
}

/** Every card name shows whole (two lines at most, nothing clamped away). */
async function namesWhole(page) {
  const cut = await page.$$eval('[data-hero-title]', els =>
    els.filter(e => e.scrollHeight - e.clientHeight > 1).map(e => e.textContent)
  )
  expect(cut).toEqual([])
}

async function commonChecks(page) {
  // no filters here: no filter panel at all, and one page — no pager
  await expect(page.locator('[data-crud-bar="filters"]')).toHaveCount(0)
  await expect(page.locator('.crud-bar.dash-card')).toHaveCount(0)
  await expect(page.locator('[data-crud-pager]')).toHaveCount(0)
}

const shotName = (name, size, dark) =>
  size.width === 1920
    ? `${SHOTS}/fix-${name}-${dark ? 'dark' : 'light'}.png`
    : null

// ---------------------------------------------------------------- Maktablar

for (const [label, size, dark] of SIZES) {
  test(`Maktablar at ${label}: hero cards fill the row, whole names, no filter panel or pager`, async ({
    browser
  }) => {
    const { context, page } = await visit(browser, '/app/schools', size, {
      dark
    })
    const schools = fixtures()['/api/schools'].content
    await expect(page.locator('[data-school-card]')).toHaveCount(schools.length)
    await fillsTheRow(page, '[data-school-card]')
    await namesWhole(page)
    await commonChecks(page)
    // the card: banner, initials, director, six numbers, the run; "Joriy" on the active school
    const first = page.locator(`[data-school-card="${schools[0].id}"]`)
    await expect(first.locator('[data-hero-kpis] [data-kpi]')).toHaveCount(6)
    await expect(first.locator('[data-hero-badge]')).toHaveText('Joriy')
    await expect(first.locator('[data-hero-primary]')).toContainText(
      'Bosh sahifaga'
    )
    if (schools[0].card?.director)
      await expect(first.locator('[data-director]')).toContainText(
        schools[0].card.director.name
      )
    if (schools.length >= 2) {
      await expect(
        page.locator(
          `[data-school-card="${schools[1].id}"] [data-hero-primary]`
        )
      ).toContainText("Shu maktabga o'tish")
      const cmp = page.locator('[data-schools-compare]')
      await cmp.scrollIntoViewIfNeeded()
      await expect(cmp.locator('[data-metric]')).toHaveCount(4)
    }
    // the switch sits in the page header, next to the search
    if (size.width >= 600)
      await expect(
        page.locator('.page-layout__actions [data-view-switch]')
      ).toBeVisible()
    const shot = shotName('schools', size, dark)
    if (shot) await page.screenshot({ path: shot, fullPage: true })
    expect(page.errors).toEqual([])
    await context.close()
  })
}

test('Maktablar: edit and delete are in the "⋯" menu, the other school is switched to', async ({
  browser
}) => {
  const { context, page } = await visit(browser, '/app/schools')
  const schools = fixtures()['/api/schools'].content
  test.skip(schools.length < 2, 'one school only')
  const card = page.locator(`[data-school-card="${schools[0].id}"]`)
  await card.locator('[data-hero-menu]').click()
  await expect(page.locator('[data-menu-item="edit"]')).toBeVisible()
  await expect(page.locator('[data-menu-item="delete"]')).toBeVisible()
  await page.keyboard.press('Escape')
  await page
    .locator(`[data-school-card="${schools[1].id}"] [data-hero-primary]`)
    .click()
  await expect(
    page.locator(`[data-school-card="${schools[1].id}"] [data-hero-badge]`)
  ).toHaveText('Joriy')
  expect(
    await page.evaluate(() => localStorage.getItem('activeSchoolId'))
  ).toBe(String(schools[1].id))
  await context.close()
})

// ---------------------------------------------------------------- Binolar

for (const [label, size, dark] of SIZES) {
  test(`Binolar at ${label}: drawn buildings fill the row, busy above 0%, whole names`, async ({
    browser
  }) => {
    const { context, page } = await visit(browser, '/app/buildings', size, {
      dark
    })
    const buildings = fixtures()['/api/buildings'].content
    await expect(page.locator('[data-building-card]')).toHaveCount(
      buildings.length
    )
    await fillsTheRow(page, '[data-building-card]')
    await namesWhole(page)
    await commonChecks(page)
    for (const b of buildings) {
      const card = page.locator(`[data-building-card="${b.id}"]`)
      // the weekly occupancy is a real share of the week's lesson slots, not "0% band"
      const text = await card.locator('[data-kpi="weekly"] b').innerText()
      expect(parseFloat(text.replace(/[^\d.]/g, ''))).toBeGreaterThan(0)
      // the drawing: one cell a room
      await expect(card.locator('[data-room-cell]')).toHaveCount(
        b.stats.floors.reduce((n, f) => n + f.rooms.length, 0)
      )
      await expect(card.locator('[data-room-type]')).toHaveCount(
        Object.keys(b.stats.roomTypes).length
      )
    }
    const shot = shotName('buildings', size, dark)
    if (shot) await page.screenshot({ path: shot, fullPage: true })
    expect(page.errors).toEqual([])
    await context.close()
  })
}

test('Binolar: a card opens Xonalar for that building, a type chip for that type too', async ({
  browser
}) => {
  const { context, page } = await visit(browser, '/app/buildings')
  const b = fixtures()['/api/buildings'].content[0]
  const card = page.locator(`[data-building-card="${b.id}"]`)
  const type = Object.keys(b.stats.roomTypes)[0]
  await card.locator(`[data-room-type="${type}"]`).click()
  await expect(page).toHaveURL(
    new RegExp(`/app/rooms\\?.*buildingId=${b.id}.*type=${type}`)
  )
  await page.goBack()
  await page.locator(`[data-building-card="${b.id}"] [data-hero-title]`).click()
  await expect(page).toHaveURL(new RegExp(`/app/rooms\\?buildingId=${b.id}$`))
  // the rooms list is filtered to that building
  const rooms = fixtures()['/api/rooms'].content.filter(
    r => r.buildingId === b.id
  )
  await expect(page.locator('tbody tr, [data-card-id]')).toHaveCount(
    Math.min(rooms.length, 20)
  )
  await context.close()
})

// ---------------------------------------------------------------- O'quv yillari

for (const [label, size, dark] of SIZES) {
  test(`O'quv yillari at ${label}: the current year 2026–2027 as a hero with terms, today and numbers`, async ({
    browser
  }) => {
    const { context, page } = await visit(
      browser,
      '/app/academic-years',
      size,
      {
        dark
      }
    )
    const hero = page.locator('[data-year-hero]')
    await expect(hero).toHaveCount(1)
    await expect(hero.locator('[data-year-title]')).toHaveText(/2026.2027/)
    await expect(hero.locator('[data-year-status]')).toHaveText('Joriy')
    await expect(hero.locator('[data-term]')).toHaveCount(7)
    await expect(hero.locator('[data-today-marker]')).toBeVisible()
    await expect(hero.locator('[data-term-line]')).toContainText('1-chorak')
    await expect(hero.locator('[data-year-progress]')).toContainText("% o'tdi")
    await expect(hero.locator('[data-year-attendance]')).toHaveText(
      /\d+(\.\d)?%/
    )
    await expect(hero.locator('[data-year-quality]')).toHaveText(/\d+(\.\d)?%/)
    // the past years: compact rows, their numbers or "no data"; no big buttons on them
    const past = page.locator('[data-past-years] [data-year-id]')
    await expect(past).toHaveCount(2)
    await expect(page.locator('[data-past-years]')).toContainText(
      "Bu yil bo'yicha ma'lumot yo'q"
    )
    await expect(page.locator('[data-make-current]')).toHaveCount(0)
    await expect(page.locator('[data-missing-year]')).toHaveCount(0)
    // "Keyingi yilni yaratish" is the header's secondary button
    await expect(page.locator('[data-header-action="openNext"]')).toBeVisible()
    const over = await page.evaluate(
      () => document.documentElement.scrollWidth - innerWidth
    )
    expect(over).toBeLessThanOrEqual(0)
    const shot = shotName('academic-years', size, dark)
    if (shot) await page.screenshot({ path: shot, fullPage: true })
    expect(page.errors).toEqual([])
    await context.close()
  })
}

test("O'quv yillari: a past year's row opens the panel; its ⋯ has «Joriy yil qilib belgilash»", async ({
  browser
}) => {
  const { context, page } = await visit(browser, '/app/academic-years')
  const row = page.locator('[data-past-years] [data-year-id]').first()
  await row.locator('[data-year-menu]').click()
  await expect(page.locator('[data-menu-item="current"]')).toBeVisible()
  await expect(page.locator('[data-menu-item="edit"]')).toBeVisible()
  await page.keyboard.press('Escape')
  await row.click()
  await expect(
    page.locator('[data-side-panel] [data-year-detail]')
  ).toBeVisible()
  await context.close()
})

test("O'quv yillari: no year of today — the warning, and 2026–2027 offered with its terms", async ({
  browser
}) => {
  const context = await mockContext(browser, WIDE)
  const f = fixtures()['/api/pages/lists/years-page']
  context.overrides['GET /api/pages/lists/years-page'] = {
    body: {
      years: f.years.filter(y => y.status !== 'CURRENT'),
      missing: {
        title: '2026–2027',
        startDate: '2026-09-02',
        endDate: '2027-05-31',
        terms: [
          {
            name: '1-chorak',
            kind: 'QUARTER',
            startDate: '2026-09-02',
            endDate: '2026-11-02'
          },
          {
            name: "Kuzgi ta'til",
            kind: 'VACATION',
            startDate: '2026-11-03',
            endDate: '2026-11-09'
          }
        ]
      }
    }
  }
  const page = await context.newPage()
  await open(page, '/app/academic-years')
  const warn = page.locator('[data-missing-year]')
  await expect(warn).toContainText("Joriy o'quv yili kiritilmagan")
  await warn.locator('[data-create-missing]').click()
  const dialog = page.locator('[data-next-dialog]')
  await expect(dialog.getByRole('textbox', { name: 'Sarlavha' })).toHaveValue(
    '2026–2027'
  )
  await expect(dialog.locator('li')).toHaveCount(2)
  await dialog.locator('[data-next-create]').click()
  await expect
    .poll(
      () =>
        context.requests.filter(
          r => r.method === 'POST' && r.path === '/api/academic-years/next'
        ).length
    )
    .toBe(1)
  const body = JSON.parse(
    context.requests.find(
      r => r.path === '/api/academic-years/next' && r.method === 'POST'
    ).body
  )
  expect(body).toMatchObject({
    title: '2026–2027',
    startDate: '2026-09-02',
    endDate: '2027-05-31'
  })
  await page.screenshot({ path: `${SHOTS}/fix-academic-years-missing.png` })
  await context.close()
})

test("O'quv yillari: overlapping terms are named in the panel and cannot be saved", async ({
  browser
}) => {
  const context = await mockContext(browser, WIDE)
  const y = fixtures()['/api/pages/lists/years-page'].years.find(
    x => x.status === 'CURRENT'
  )
  context.overrides[`GET /api/academic-years/${y.id}/terms`] = {
    body: [
      {
        name: '1-chorak',
        kind: 'QUARTER',
        startDate: '2026-09-02',
        endDate: '2026-11-05'
      },
      {
        name: "Kuzgi ta'til",
        kind: 'VACATION',
        startDate: '2026-11-03',
        endDate: '2026-11-09'
      }
    ]
  }
  const page = await context.newPage()
  await open(page, '/app/academic-years')
  await page.locator('[data-year-hero] [data-open-terms]').click()
  const panel = page.locator('[data-side-panel]')
  await expect(panel.locator('[data-terms-problems]')).toContainText(
    'ustma-ust'
  )
  await expect(panel.locator('[data-save-terms]')).toBeDisabled()
  await context.close()
})

// ---------------------------------------------------------------- Bosh sahifa: the missing year

test('Bosh sahifa: "Joriy o\'quv yili kiritilmagan" comes first in "E\'tibor talab qiladi"', async ({
  browser
}) => {
  const context = await mockContext(browser, WIDE)
  context.overrides['GET /api/dashboard/signals'] = {
    body: {
      asOf: '2026-10-20T10:00:00',
      data: [
        {
          kind: 'NO_CURRENT_YEAR',
          level: 'HIGH',
          title: "Joriy o'quv yili kiritilmagan",
          count: 1,
          items: [
            {
              kind: 'NO_CURRENT_YEAR',
              level: 'HIGH',
              title: "2026–2027 o'quv yili kiritilmagan",
              subtitle: "Bugungi sana hech qaysi o'quv yiliga tushmaydi",
              detail: null,
              ref: { title: '2026–2027' }
            }
          ]
        }
      ]
    }
  }
  const page = await context.newPage()
  await open(page, '/')
  const s = page.locator('[data-signal="NO_CURRENT_YEAR"]')
  await expect(s).toContainText("Joriy o'quv yili kiritilmagan")
  await s.click()
  await page.locator('[data-action]').filter({ hasText: 'Yaratish' }).click()
  await expect(page).toHaveURL(/\/app\/academic-years/)
  await context.close()
})

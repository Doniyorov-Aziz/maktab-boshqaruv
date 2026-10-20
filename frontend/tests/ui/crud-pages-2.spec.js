// What the first round of the five pages left untested: dragging a term on the year bar, the
// subject panel's teacher → class hours, a risk ring on every student, the users form taking the
// position's role, and each page at 1920 light / 1920 dark / 1366 / 390 with its side panel.
//   CRUD_FIXTURES=…/crud.json UI_BASE=http://localhost:9103 npx playwright test tests/ui/crud-pages-2.spec.js
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

async function visit(browser, path, size = WIDE, opts = {}) {
  const context = await mockContext(browser, size, opts)
  const page = await context.newPage()
  page.errors = []
  page.on('pageerror', e => page.errors.push(String(e)))
  page.on('console', m => m.type() === 'error' && page.errors.push(m.text()))
  await open(page, path)
  return { context, page }
}

const sent = (context, method, path) =>
  context.requests.filter(r => r.method === method && r.path === path)

// ---------------------------------------------------------------- O'quv yillari: drag

test("O'quv yillari: a term's edge is dragged on the bar, kept off its neighbour, saved", async ({
  browser
}) => {
  const { context, page } = await visit(browser, '/app/academic-years')
  await page.locator('[data-open-terms]').first().click()
  const panel = page.locator('[data-side-panel]')
  await panel.locator('[data-usual-terms]').click()
  const bar = panel.locator('[data-terms-bar]')
  await expect(bar).toBeVisible()
  // the bar and the editable list are both there (the list once vanished behind the bar)
  await expect(panel.locator('[data-term-row]')).toHaveCount(7)
  await expect(bar.locator('[data-seg]')).toHaveCount(7)
  const last = bar.locator('[data-seg]').last()
  const lastEnd = last.locator('[data-handle="end"]')
  // the handle names its date ("4-chorak: tugashi 25.05.2026 …")
  const before = await lastEnd.getAttribute('aria-label')

  // the last quarter's end, dragged left by a fifth of the bar
  await last.hover()
  const hb = await lastEnd.boundingBox()
  const bb = await bar.boundingBox()
  await page.mouse.move(hb.x + hb.width / 2, hb.y + hb.height / 2)
  await page.mouse.down()
  await page.mouse.move(hb.x - bb.width / 5, hb.y + hb.height / 2, { steps: 6 })
  await expect(panel.locator('[data-drag-note]')).toBeVisible()
  await page.mouse.up()
  expect(await lastEnd.getAttribute('aria-label')).not.toBe(before)

  // the first quarter's end cannot pass the vacation after it (keyboard: → is refused, ← works)
  const firstEnd = bar
    .locator('[data-seg]')
    .first()
    .locator('[data-handle="end"]')
  const q1 = await firstEnd.getAttribute('aria-label')
  await firstEnd.focus()
  await page.keyboard.press('ArrowRight')
  expect(await firstEnd.getAttribute('aria-label')).toBe(q1)
  await page.keyboard.press('ArrowLeft')
  expect(await firstEnd.getAttribute('aria-label')).not.toBe(q1)

  await panel.locator('[data-save-terms]').click()
  await expect
    .poll(
      () =>
        context.requests.filter(
          r => r.method === 'PUT' && r.path.endsWith('/terms')
        ).length
    )
    .toBe(1)
  const body = JSON.parse(context.requests.find(r => r.method === 'PUT').body)
  const q4 = body.find(t => t.name === '4-chorak')
  expect(q4.endDate < `${q4.endDate.slice(0, 4)}-05-25`).toBe(true)
  expect(q4.endDate >= q4.startDate).toBe(true)
  expect(page.errors).toEqual([])
  await context.close()
})

// ---------------------------------------------------------------- Fanlar: teacher → classes

test('Fanlar: the panel says in which classes each teacher has the subject, hours a week', async ({
  browser
}) => {
  const { context, page } = await visit(browser, '/app/subjects')
  const key = Object.keys(fixtures()).find(
    k => k.startsWith('/api/pages/lists/subjects/') && k.endsWith('/extra')
  )
  const extra = fixtures()[key]
  const id = key.split('/')[5]
  await page.locator(`[data-subject-card="${id}"]`).click()
  const [tid, classes] = Object.entries(extra.byTeacher)[0]
  const row = page.locator(`[data-subject-detail] [data-teacher-row="${tid}"]`)
  await expect(row.locator('[data-teacher-class]')).toHaveCount(classes.length)
  await expect(row.locator('[data-teacher-class]').first()).toHaveText(
    `${classes[0].name} · ${classes[0].lessons} soat`
  )
  await context.close()
})

// ---------------------------------------------------------------- O'quvchilar: risk for everyone

test("O'quvchilar: every row has its risk ring (not only 40+); the panel says why or that there is none", async ({
  browser
}) => {
  const { context, page } = await visit(browser, '/app/students')
  const rows = page.locator('tbody tr')
  await expect(rows).toHaveCount(25)
  await expect(page.locator('tbody [data-risk]')).toHaveCount(25)
  const low = fixtures()['/api/students'].content.find(s => s.risk < 40)
  const high = fixtures()['/api/students'].content.find(s => s.risk >= 40)
  if (low) {
    await page
      .locator('[data-crud-search]')
      .fill(low.firstName + ' ' + low.lastName)
    await expect(
      page.locator(`tr[data-row-id="${low.id}"] [data-risk]`)
    ).toHaveText(String(low.risk))
  }
  if (high) {
    await page
      .locator('[data-crud-search]')
      .fill(high.firstName + ' ' + high.lastName)
    await page.locator(`tr[data-row-id="${high.id}"]`).click()
    await expect(
      page.locator('[data-student-detail] [data-risk-reasons] li').first()
    ).toBeVisible()
  }
  await context.close()
})

// ---------------------------------------------------------------- Lavozimlar → Foydalanuvchilar: the role

test("Lavozimlar: a new user of an employee is offered their position's role", async ({
  browser
}) => {
  const context = await mockContext(browser, WIDE)
  context.overrides['GET /api/employees'] = {
    body: {
      content: [
        { id: 7001, fullName: 'Ali Valiyev', positionDefaultRole: 'EDITOR' },
        { id: 7002, fullName: 'Vali Aliyev', positionDefaultRole: null }
      ],
      totalElements: 2
    }
  }
  const page = await context.newPage()
  await open(page, '/app/users')
  await page.getByRole('button', { name: "Yangi qo'shish" }).first().click()
  const form = page.locator('[data-crud-form]')
  await form.locator('[data-field="username"] input').fill('ali')
  await form.locator('[data-field="password"] input').fill('parol123')
  await form.locator('[data-field="employeeId"] .q-field').click()
  await page.getByRole('option', { name: 'Ali Valiyev' }).click()
  await expect(form.locator('[data-field="role"]')).toContainText('EDITOR')
  await page.locator('[data-form-save]').click()
  await expect.poll(() => sent(context, 'POST', '/api/users').length).toBe(1)
  expect(JSON.parse(sent(context, 'POST', '/api/users')[0].body)).toMatchObject(
    {
      username: 'ali',
      role: 'EDITOR',
      employeeId: 7001
    }
  )
  await context.close()
})

// ---------------------------------------------------------------- the five pages at four sizes

const PAGES = [
  {
    name: "O'quv yillari",
    path: '/app/academic-years',
    opts: { currentYear: true },
    ready: '[data-year-id]',
    open: page => page.locator('[data-open-terms]').first().click(),
    panel: '[data-year-detail]',
    extra: async page => {
      await expect(page.locator('[data-today-marker]')).toBeVisible()
    }
  },
  {
    name: 'Fanlar',
    path: '/app/subjects',
    ready: '[data-subject-card]',
    open: page => page.locator('[data-subject-card]').first().click(),
    panel: '[data-subject-detail]'
  },
  {
    name: "O'quvchilar",
    path: '/app/students',
    ready: '[data-crud-hero]',
    open: page => page.locator('[data-card-id], tbody tr').first().click(),
    panel: '[data-student-detail]',
    extra: async (page, size) => {
      // a phone opens on cards, a wider screen on the table without an address column
      if (size.width < 600)
        await expect(page.locator('[data-crud-cards]')).toBeVisible()
      else
        await expect(page.locator('th[data-col="fullAddress"]')).toHaveCount(0)
    }
  },
  {
    name: 'Lavozimlar',
    path: '/app/positions',
    ready: '[data-position-card]',
    open: null,
    panel: null
  },
  {
    name: 'Xodimlar',
    path: '/app/employees',
    ready: '[data-crud-hero]',
    open: page =>
      page.locator('[data-employee-card], tbody tr').first().click(),
    panel: '[data-employee-detail]',
    extra: async (page, size) => {
      if (size.width < 600)
        await expect(page.locator('[data-employee-card]').first()).toBeVisible()
      else
        await expect(page.locator('th[data-col="weeklyLessons"]')).toBeVisible()
    }
  }
]

for (const p of PAGES) {
  for (const [label, size, dark] of SIZES) {
    test(`${p.name} at ${label}: nothing sideways, its panel fits, no console errors`, async ({
      browser
    }) => {
      const { context, page } = await visit(browser, p.path, size, {
        dark,
        ...p.opts
      })
      await expect(page.locator(p.ready).first()).toBeVisible()
      const over = await page.evaluate(
        () => document.documentElement.scrollWidth - innerWidth
      )
      expect(over).toBeLessThanOrEqual(0)
      if (p.extra) await p.extra(page, size)
      // on a phone the hero numbers sit two a row
      if (
        size.width < 600 &&
        (await page.locator('[data-crud-hero]').count())
      ) {
        const kpis = page.locator('[data-crud-hero] [data-kpi]')
        const [a, b] = [
          await kpis.nth(0).boundingBox(),
          await kpis.nth(1).boundingBox()
        ]
        expect(Math.abs(a.y - b.y)).toBeLessThan(2)
      }
      if (p.open) {
        await p.open(page)
        const panel = page.locator('[data-side-panel]')
        await expect(panel.locator(p.panel)).toBeVisible()
        await page.waitForTimeout(600) // the slide-in is over
        const box = await panel.boundingBox()
        expect(box.x).toBeGreaterThanOrEqual(-1)
        expect(box.x + box.width).toBeLessThanOrEqual(size.width + 1)
      }
      expect(page.errors).toEqual([])
      await context.close()
    })
  }
}

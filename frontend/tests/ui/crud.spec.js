// The common list page (CrudPage) in a browser, on fixtures from the TEST database
// (crud-mock.js pages / sorts / searches / filters like the server): 1920 light and dark, 1366,
// 390; ADMIN and VIEWER.
//   CRUD_FIXTURES=…/crud.json UI_BASE=http://localhost:9103 npx playwright test tests/ui/crud.spec.js
import { test, expect } from '@playwright/test'
import { hasFixtures, fixtures, mockContext, open } from './crud-mock.js'

test.skip(!hasFixtures(), 'CRUD_FIXTURES is not set')

const WIDE = { width: 1920, height: 1080 }
const LAPTOP = { width: 1366, height: 768 }
const PHONE = { width: 390, height: 844 }

/** A page that fails the test on any console error or uncaught exception. */
async function watched(context) {
  const page = await context.newPage()
  page.errors = []
  page.on('pageerror', e => page.errors.push(String(e)))
  page.on('console', m => m.type() === 'error' && page.errors.push(m.text()))
  return page
}

async function visit(browser, path, size = WIDE, opts = {}) {
  const context = await mockContext(browser, size, opts)
  const page = await watched(context)
  await open(page, path)
  return { context, page }
}

const listCalls = (context, path) =>
  context.requests.filter(r => r.method === 'GET' && r.path === path)
const lastQuery = (context, path) =>
  new URLSearchParams(listCalls(context, path).at(-1)?.search || '')

// ---------------------------------------------------------------- table basics

for (const [label, size, dark] of [
  ['1920 light', WIDE, false],
  ['1920 dark', WIDE, true],
  ['1366', LAPTOP, false],
  ['390', PHONE, true]
]) {
  test(`${label}: the actions column is at most 88 px wide, always shown`, async ({
    browser
  }) => {
    const { context, page } = await visit(
      browser,
      '/app/employees?view=table',
      size,
      {
        dark
      }
    )
    const cell = page.locator('tbody td.ct__actions').first()
    await expect(cell).toBeVisible()
    const box = await cell.boundingBox()
    expect(box.width).toBeLessThanOrEqual(88)
    await expect(
      page.locator('tbody tr').first().locator('[data-action="edit"]')
    ).toBeVisible()
    // nothing sticks out of the page sideways
    const over = await page.evaluate(
      () => document.documentElement.scrollWidth - innerWidth
    )
    expect(over).toBeLessThanOrEqual(0)
    expect(page.errors).toEqual([])
    await context.close()
  })
}

test('employees: more than 3 classes is one "N ta sinf" chip with the list in a popover', async ({
  browser
}) => {
  const { context, page } = await visit(browser, '/app/employees?size=50')
  const many = fixtures()['/api/employees'].content.find(
    e => (e.classes || '').split(',').length > 3
  )
  const n = many.classes.split(',').length
  const row = page.locator(`tr[data-row-id="${many.id}"]`)
  const chip = row.locator(`[data-more="${n}"]`)
  await expect(chip).toHaveText(`${n} ta sinf`)
  // the long comma list is not in the table
  await expect(row).not.toContainText(many.classes)
  await chip.click()
  await expect(page.locator('.cc-pop__items .cc-chip')).toHaveCount(n)
  expect(page.errors).toEqual([])
  await context.close()
})

test('phones read "+998 90 111 22 33" and call on click', async ({
  browser
}) => {
  const { context, page } = await visit(browser, '/app/employees')
  const phones = page.locator('[data-phone]')
  await expect(phones.first()).toBeVisible()
  for (const t of await phones.allTextContents())
    expect(t.trim()).toMatch(/^\+998 \d{2} \d{3} \d{2} \d{2}$/)
  expect(await phones.first().getAttribute('href')).toMatch(/^tel:\+998\d{9}$/)
  await context.close()
})

test('25 a page by default; "1–25 / N"; size and page go to the server and the URL', async ({
  browser
}) => {
  const { context, page } = await visit(browser, '/app/students')
  const total = fixtures()['/api/students'].totalElements
  await expect(page.locator('tbody tr')).toHaveCount(25)
  await expect(page.locator('[data-page-range]')).toHaveText(`1–25 / ${total}`)
  expect(lastQuery(context, '/api/students').get('size')).toBe('25')
  await page.locator('[data-page-next]').click()
  await expect(page.locator('[data-page-range]')).toHaveText(`26–50 / ${total}`)
  expect(lastQuery(context, '/api/students').get('page')).toBe('1')
  expect(page.url()).toContain('page=2')
  await page.locator('[data-page-size]').click()
  await page.getByRole('option', { name: '100' }).click()
  await expect(page.locator('tbody tr')).toHaveCount(100)
  expect(page.url()).toContain('size=100')
  await context.close()
})

test('a sortable header sorts on the server (name → last, first name)', async ({
  browser
}) => {
  const { context, page } = await visit(browser, '/app/students')
  await page.locator('th[data-col="fullName"]').click()
  await expect
    .poll(() => lastQuery(context, '/api/students').get('sort'))
    .toBe('fullName,asc')
  await page.locator('th[data-col="fullName"]').click()
  await expect
    .poll(() => lastQuery(context, '/api/students').get('sort'))
    .toBe('fullName,desc')
  await expect(page.locator('th[data-col="fullName"]')).toHaveAttribute(
    'aria-sort',
    'descending'
  )
  await context.close()
})

test('"/" jumps into the search; the server searches (q)', async ({
  browser
}) => {
  const { context, page } = await visit(browser, '/app/students')
  await page.locator('body').press('/')
  await expect(page.locator('input[data-crud-search]')).toBeFocused()
  await page.keyboard.type('madina')
  await expect
    .poll(() => lastQuery(context, '/api/students').get('q'))
    .toBe('madina')
  await expect(page.locator('tbody tr').first()).toContainText('Madina')
  expect(page.url()).toContain('q=madina')
  await context.close()
})

test('nothing found: "Filtrni tozalash" brings the list back', async ({
  browser
}) => {
  const { context, page } = await visit(browser, '/app/students?q=zzzzzz')
  await expect(page.locator('[data-empty="filtered"]')).toBeVisible()
  await page.getByRole('button', { name: 'Filtrni tozalash' }).click()
  await expect(page.locator('tbody tr')).toHaveCount(25)
  await context.close()
})

test('a failed list says so and can try again', async ({ browser }) => {
  const context = await mockContext(browser, WIDE)
  context.overrides['GET /api/positions'] = {
    status: 500,
    body: 'Server xatosi'
  }
  const page = await context.newPage()
  await open(page, '/app/positions')
  await expect(page.locator('[data-crud-error]')).toContainText('Server xatosi')
  delete context.overrides['GET /api/positions']
  await page.getByRole('button', { name: 'Qayta urinish' }).click()
  await expect(page.locator('[data-card-id]').first()).toBeVisible()
  await context.close()
})

// ---------------------------------------------------------------- filters, view, URL

test('class picker filter: a class → only its students, the number matches the facet', async ({
  browser
}) => {
  const { context, page } = await visit(browser, '/app/students')
  const f = fixtures()
  const facet = f['/api/students/facets'].schoolClassId.find(x => x.count > 0)
  const cls = f['/api/school-classes'].content.find(
    c => String(c.id) === facet.value
  )
  await page
    .locator(`[data-class-picker] [data-grade="${cls.gradeNumber}"]`)
    .click()
  await page.locator(`[data-class-picker] [data-class="${cls.id}"]`).click()
  await expect
    .poll(() => lastQuery(context, '/api/students').get('schoolClassId'))
    .toBe(String(cls.id))
  await expect(page.locator('[data-page-range]')).toContainText(
    `/ ${facet.count}`
  )
  for (const t of await page
    .locator('tbody .cc-person .cc-person__sub')
    .allTextContents())
    expect(t.trim()).toContain(`${cls.gradeNumber}-${cls.sectionLetter} sinf`)
  // "Barchasi" clears it
  await page.locator('[data-class-all]').click()
  await expect
    .poll(() => lastQuery(context, '/api/students').get('schoolClassId'))
    .toBe(null)
  await context.close()
})

test('chip filters show their numbers; × tozalash clears them', async ({
  browser
}) => {
  const { context, page } = await visit(browser, '/app/employees')
  const pos = fixtures()['/api/employees/facets'].positionId[0]
  const chip = page.locator(
    `[data-filter="positionId"] [data-chip="${pos.value}"]`
  )
  await expect(chip).toContainText(String(pos.count))
  await chip.click()
  await expect(chip).toHaveAttribute('aria-pressed', 'true')
  await expect(page.locator('[data-page-range]')).toContainText(
    `/ ${pos.count}`
  )
  await page.locator('[data-filters-clear]').click()
  await expect(chip).toHaveAttribute('aria-pressed', 'false')
  await context.close()
})

test('the view (cards) and the filters live in the URL: a reload keeps them', async ({
  browser
}) => {
  const { context, page } = await visit(browser, '/app/employees')
  await page
    .locator('[data-view-switch]')
    .getByRole('tab', { name: 'Kartochkalar' })
    .click()
  await expect(page.locator('[data-crud-cards]')).toBeVisible()
  const pos = fixtures()['/api/employees/facets'].positionId[0]
  await page
    .locator(`[data-filter="positionId"] [data-chip="${pos.value}"]`)
    .click()
  await expect(page).toHaveURL(new RegExp(`positionId=${pos.value}`))
  await expect(page).toHaveURL(/view=cards/)
  await page.reload({ waitUntil: 'networkidle' })
  await expect(page.locator('[data-crud-cards]')).toBeVisible()
  await expect(page.locator('[data-card-id]')).toHaveCount(pos.count)
  await expect(
    page.locator(`[data-filter="positionId"] [data-chip="${pos.value}"]`)
  ).toHaveAttribute('aria-pressed', 'true')
  await context.close()
})

test('density and the chosen columns stay in this browser', async ({
  browser
}) => {
  const { context, page } = await visit(browser, '/app/employees')
  await page
    .locator('[data-density]')
    .getByRole('tab', { name: 'Ixcham' })
    .click()
  await expect(page.locator('[data-crud-table]')).toHaveClass(/ct--compact/)
  await page.locator('[data-columns]').click()
  await page.locator('[data-column-toggle="phone"]').click()
  await page.keyboard.press('Escape')
  await expect(page.locator('th[data-col="phone"]')).toHaveCount(0)
  await page.reload({ waitUntil: 'networkidle' })
  await expect(page.locator('[data-crud-table]')).toHaveClass(/ct--compact/)
  await expect(page.locator('th[data-col="phone"]')).toHaveCount(0)
  await context.close()
})

// ---------------------------------------------------------------- the side panel

test('a row opens the record in the panel, which scrolls to its end', async ({
  browser
}) => {
  const { context, page } = await visit(browser, '/app/students', {
    width: 1440,
    height: 520
  })
  const first = fixtures()['/api/students'].content[0]
  await page.locator('tbody tr').first().click()
  const panel = page.locator('[data-side-panel]')
  await expect(panel.locator('[data-student-detail]')).toBeVisible()
  await expect(panel.locator('[data-detail="fullAddress"]')).toBeVisible()
  const body = panel.locator('[data-drawer-body]')
  await body.evaluate(e => (e.scrollTop = e.scrollHeight))
  const atEnd = await body.evaluate(
    e => e.scrollTop + e.clientHeight >= e.scrollHeight - 2
  )
  expect(atEnd).toBe(true)
  await expect(
    panel.locator('[data-student-detail] section').last()
  ).toBeInViewport()
  await expect(panel.locator('[data-student-detail] h2')).toHaveText(
    first.fullName
  )
  await expect(panel.locator('h3')).toHaveText("O'quvchilar")
  expect(page.errors).toEqual([])
  await context.close()
})

test('edit in the same panel: wide form in sections, phone mask, inline errors, unsaved warning', async ({
  browser
}) => {
  const { context, page } = await visit(browser, '/app/employees')
  await page.locator('tbody tr').first().click()
  await page.locator('[data-detail-edit]').click()
  const panel = page.locator('[data-side-panel]')
  await expect(panel.locator('[data-crud-form]')).toBeVisible()
  expect((await panel.boundingBox()).width).toBeGreaterThanOrEqual(550)
  await expect(panel.locator('[data-section="Xodim"]')).toBeVisible()
  await expect(panel.locator('[data-section="Holat"]')).toBeVisible()
  const phone = panel.locator('input[data-phone-input]')
  await expect(phone).toHaveValue(/^\+998 \d{2} \d{3} \d{2} \d{2}$/)
  await phone.fill('90111')
  await expect(phone).toHaveValue('+998 90 111')
  await phone.blur()
  await expect(panel).toContainText("Telefon to'liq emas")
  await expect(panel.locator('[data-dirty]')).toBeVisible()
  await panel.locator('[data-form-cancel]').click()
  await expect(page.locator('.q-dialog').last()).toContainText(
    "Saqlanmagan o'zgarishlar"
  )
  await page.getByRole('button', { name: 'Qolish' }).click()
  await phone.fill('901112233')
  await panel.locator('[data-form-save]').click()
  await expect
    .poll(() => context.requests.filter(r => r.method === 'PUT').length)
    .toBe(1)
  const sent = JSON.parse(context.requests.find(r => r.method === 'PUT').body)
  expect(sent.phone).toBe('+998901112233')
  await context.close()
})

test('"Yangi qo\'shish" opens an empty wide form; required fields say so', async ({
  browser
}) => {
  const { context, page } = await visit(browser, '/app/students')
  await page.getByRole('button', { name: "Yangi qo'shish" }).first().click()
  const panel = page.locator('[data-side-panel]')
  await expect(panel.locator('[data-crud-form]')).toBeVisible()
  await expect(panel.locator('[data-section="Ota-ona"]')).toBeVisible()
  await panel.locator('[data-form-save]').click()
  await expect(panel.locator('.q-field--error').first()).toBeVisible()
  expect(context.requests.filter(r => r.method === 'POST')).toHaveLength(0)
  await context.close()
})

// ---------------------------------------------------------------- delete, bulk, export, roles

test('a position with employees cannot be deleted: the dialog says how many', async ({
  browser
}) => {
  const { context, page } = await visit(browser, '/app/positions')
  const pos = fixtures()['/api/positions'].content.find(
    p => p.employeeCount > 0
  )
  await page
    .locator(`[data-position-card="${pos.id}"] [data-action="delete"]`)
    .click()
  await expect(page.locator('.q-dialog').last()).toContainText(
    `${pos.employeeCount} ta xodim bor`
  )
  expect(context.requests.filter(r => r.method === 'DELETE')).toHaveLength(0)
  await context.close()
})

test("the server's 409 is shown as it is", async ({ browser }) => {
  const context = await mockContext(browser, WIDE)
  context.overrides[
    'DELETE /api/subjects/' +
      fixtures()['/api/subjects'].content.find(s => s.active !== false).id
  ] = {
    status: 409,
    body: 'Bu fanda baholar bor'
  }
  const page = await context.newPage()
  await open(page, '/app/subjects?view=table')
  const id = fixtures()['/api/subjects'].content.find(
    s => s.active !== false
  ).id
  await page.locator(`tr[data-row-id="${id}"] [data-action="delete"]`).click()
  await page
    .locator('.q-dialog')
    .last()
    .getByRole('button', { name: 'Nofaol qilish' })
    .click()
  await expect(page.locator('.q-dialog').last()).toContainText(
    'Bu fanda baholar bor'
  )
  await context.close()
})

test('an employee: "Ishdan ketgan" is offered instead of deleting', async ({
  browser
}) => {
  const { context, page } = await visit(browser, '/app/employees')
  await page
    .locator('tbody tr')
    .first()
    .locator('[data-action="delete"]')
    .click()
  const dlg = page.locator('.q-dialog').last()
  await expect(dlg).toContainText('Ishdan ketgan deb belgilash')
  await page.getByRole('button', { name: 'Davom etish' }).click()
  await expect
    .poll(() => context.requests.filter(r => r.method === 'PUT').length)
    .toBe(1)
  expect(
    JSON.parse(context.requests.find(r => r.method === 'PUT').body).status
  ).toBe('DISMISSED')
  expect(context.requests.filter(r => r.method === 'DELETE')).toHaveLength(0)
  await context.close()
})

test('check boxes → bulk bar; Excel of the selected goes to the export endpoint', async ({
  browser
}) => {
  const { context, page } = await visit(browser, '/app/employees')
  const boxes = page.locator('tbody [data-check]')
  await boxes.nth(0).check()
  await boxes.nth(2).click({ modifiers: ['Shift'] })
  await expect(page.locator('[data-bulk-bar]')).toContainText('3 ta tanlandi')
  await page.locator('[data-bulk-xlsx]').click()
  await expect
    .poll(
      () => context.requests.filter(r => r.path === '/api/export/table').length
    )
    .toBe(1)
  const req = context.requests.find(r => r.path === '/api/export/table')
  expect(req.search).toContain('format=xlsx')
  const body = JSON.parse(req.body)
  expect(body.rows).toHaveLength(3)
  expect(body.columns.find(c => c.label === 'Telefon').type).toBe('phone')
  await page.locator('[data-bulk-clear]').click()
  await expect(page.locator('[data-bulk-bar]')).toHaveCount(0)
  await context.close()
})

test('Eksport ▾ PDF sends every row of the current filter', async ({
  browser
}) => {
  const { context, page } = await visit(browser, '/app/students')
  await page.locator('[data-export]').click()
  await page.locator('[data-export-pdf]').click()
  await expect
    .poll(
      () => context.requests.filter(r => r.path === '/api/export/table').length
    )
    .toBe(1)
  const req = context.requests.find(r => r.path === '/api/export/table')
  expect(req.search).toContain('format=pdf')
  expect(JSON.parse(req.body).rows).toHaveLength(
    fixtures()['/api/students'].totalElements
  )
  await context.close()
})

test('VIEWER: no create, no edit, no delete', async ({ browser }) => {
  const { context, page } = await visit(browser, '/app/employees', WIDE, {
    role: 'VIEWER'
  })
  await expect(page.locator('tbody tr').first()).toBeVisible()
  await expect(
    page.getByRole('button', { name: "Yangi qo'shish" })
  ).toHaveCount(0)
  await expect(page.locator('[data-action="edit"]')).toHaveCount(0)
  await expect(page.locator('[data-action="delete"]')).toHaveCount(0)
  await page.locator('tbody tr').first().click()
  await expect(page.locator('[data-employee-detail]')).toBeVisible()
  await expect(page.locator('[data-detail-edit]')).toHaveCount(0)
  await context.close()
})

test('other modules still work: schools and classes as cards, rooms with its own action', async ({
  browser
}) => {
  const { context, page } = await visit(browser, '/app/schools')
  await expect(page.locator('[data-card-id]')).toHaveCount(
    fixtures()['/api/schools'].totalElements
  )
  await expect(page.locator('[data-crud-cards]')).toContainText("O'quvchilar")
  await open(page, '/app/school-classes')
  await expect(page.locator('[data-card-id]').first()).toContainText('sinf')
  await open(page, '/app/academic-years')
  await expect(page.locator('[data-year-id]').first()).toBeVisible()
  expect(page.errors).toEqual([])
  await context.close()
})

test('a phone opens on cards where the module has them; no page scroll sideways', async ({
  browser
}) => {
  const { context, page } = await visit(browser, '/app/students', PHONE, {
    dark: true
  })
  await expect(page.locator('[data-crud-cards]')).toBeVisible()
  await expect(page.locator('[data-card-id]')).toHaveCount(25)
  const over = await page.evaluate(
    () => document.documentElement.scrollWidth - innerWidth
  )
  expect(over).toBeLessThanOrEqual(0)
  await context.close()
})

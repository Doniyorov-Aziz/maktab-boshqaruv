// The five pages' own designs in a browser (part 2), on fixtures from the TEST database
// (crud-mock.js): O'quv yillari, Fanlar, O'quvchilar, Lavozimlar, Xodimlar.
//   CRUD_FIXTURES=…/crud.json UI_BASE=http://localhost:9103 npx playwright test tests/ui/crud-pages.spec.js
import { test, expect } from '@playwright/test'
import { hasFixtures, fixtures, mockContext, open } from './crud-mock.js'

test.skip(!hasFixtures(), 'CRUD_FIXTURES is not set')

const WIDE = { width: 1920, height: 1080 }

async function visit(browser, path, opts = {}) {
  const context = await mockContext(browser, WIDE, opts)
  const page = await context.newPage()
  page.errors = []
  page.on('pageerror', e => page.errors.push(String(e)))
  page.on('console', m => m.type() === 'error' && page.errors.push(m.text()))
  await open(page, path)
  return { context, page }
}

const lastQuery = (context, path) =>
  new URLSearchParams(
    context.requests.filter(r => r.method === 'GET' && r.path === path).at(-1)
      ?.search || ''
  )
const sent = (context, method, path) =>
  context.requests.filter(r => r.method === method && r.path === path)

test.describe("O'quv yillari", () => {
  // the test database has no year around today: crud-mock adds a synthetic current one
  test('the current year glows, its bar has a "Bugun" marker and "% o\'tdi"', async ({
    browser
  }) => {
    const { context, page } = await visit(browser, '/app/academic-years', {
      currentYear: true
    })
    const card = page.locator('[data-status="CURRENT"]')
    await expect(card).toHaveCount(1)
    await expect(card.locator('[data-year-status]')).toHaveText('Joriy')
    await expect(card.locator('[data-today-marker]')).toHaveText('Bugun')
    await expect(card.locator('[data-year-progress]')).toContainText("o'tdi")
    await expect(card.locator('[data-term]')).toHaveCount(7)
    await expect(
      page.locator('[data-status="FINISHED"] [data-today-marker]')
    ).toHaveCount(0)
    expect(page.errors).toEqual([])
    await context.close()
  })

  test('"Joriy yil qilib belgilash" and "Keyingi yilni yaratish"', async ({
    browser
  }) => {
    const { context, page } = await visit(browser, '/app/academic-years')
    const y = fixtures()['/api/pages/lists/years'][0]
    await page.locator(`[data-year-id="${y.id}"] [data-make-current]`).click()
    await expect
      .poll(
        () =>
          sent(context, 'POST', `/api/academic-years/${y.id}/current`).length
      )
      .toBe(1)
    await page.locator('[data-next-year]').click()
    const dlg = page.locator('[data-next-dialog]')
    await expect(dlg.locator('input').first()).toHaveValue(
      fixtures()['/api/academic-years/next'].title
    )
    await dlg.locator('[data-next-create]').click()
    await expect
      .poll(() => sent(context, 'POST', '/api/academic-years/next').length)
      .toBe(1)
    const body = JSON.parse(
      sent(context, 'POST', '/api/academic-years/next')[0].body
    )
    expect(body.terms.length).toBeGreaterThan(0)
    await context.close()
  })

  test('terms in the panel: the usual calendar, saved at once', async ({
    browser
  }) => {
    const { context, page } = await visit(browser, '/app/academic-years')
    await page.locator('[data-open-terms]').first().click()
    const panel = page.locator('[data-side-panel]')
    await expect(panel.locator('[data-year-detail]')).toBeVisible()
    await panel.locator('[data-usual-terms]').click()
    await expect(panel.locator('[data-term-row]')).not.toHaveCount(0)
    await panel.locator('[data-save-terms]').click()
    await expect
      .poll(
        () =>
          context.requests.filter(
            r => r.method === 'PUT' && r.path.endsWith('/terms')
          ).length
      )
      .toBe(1)
    await context.close()
  })
})

test.describe('Fanlar', () => {
  test('cards by default; the Faol | Nofaol tabs go to the server', async ({
    browser
  }) => {
    const { context, page } = await visit(browser, '/app/subjects')
    await expect(page.locator('[data-subject-card]').first()).toBeVisible()
    expect(lastQuery(context, '/api/subjects').get('status')).toBe('ACTIVE')
    const inactive = fixtures()['/api/subjects'].content.filter(
      s => s.active === false
    ).length
    await page
      .locator('[data-crud-tabs]')
      .getByRole('tab', { name: /Nofaol/ })
      .click()
    await expect
      .poll(() => lastQuery(context, '/api/subjects').get('status'))
      .toBe('INACTIVE')
    await expect(page.locator('[data-page-range]')).toContainText(
      `/ ${inactive}`
    )
    await expect(page.locator('[data-inactive]').first()).toBeVisible()
    await context.close()
  })

  test('a card: teachers, hours, quality; the panel: spread and classes', async ({
    browser
  }) => {
    const { context, page } = await visit(browser, '/app/subjects')
    const sub = fixtures()['/api/subjects'].content.find(
      s => s.teacherCount > 1 && s.active !== false && s.qualityNow != null
    )
    const card = page.locator(`[data-subject-card="${sub.id}"]`)
    await expect(card).toContainText(`${sub.teacherCount} o'qituvchi`)
    await expect(card).toContainText(`${sub.weeklyLessons} soat`)
    await expect(card).toContainText(`${Math.round(sub.qualityNow)}%`)
    await card.click()
    const panel = page.locator('[data-subject-detail]')
    await expect(panel.locator('[data-distribution]')).toBeVisible()
    await expect(panel.locator('[data-class-row]').first()).toBeVisible()
    await context.close()
  })

  test('the form: a colour from the palette, an icon (suggested from the name)', async ({
    browser
  }) => {
    const { context, page } = await visit(browser, '/app/subjects')
    await page.getByRole('button', { name: "Yangi qo'shish" }).first().click()
    const panel = page.locator('[data-side-panel]')
    await panel.locator('[data-crud-form] input').first().fill('Fizika')
    await expect(panel.locator('[data-icon="science"]')).toHaveClass(
      /cform__icon--suggested/
    )
    await panel.locator('[data-swatch="#0284c7"]').click()
    await panel.locator('[data-icon="calculate"]').click()
    await panel.locator('[data-form-save]').click()
    await expect
      .poll(() => sent(context, 'POST', '/api/subjects').length)
      .toBe(1)
    expect(
      JSON.parse(sent(context, 'POST', '/api/subjects')[0].body)
    ).toMatchObject({
      name: 'Fizika',
      color: '#0284c7',
      icon: 'calculate'
    })
    await context.close()
  })
})

test.describe("O'quvchilar", () => {
  test('no address column in the table (nor in the column menu); the panel has it', async ({
    browser
  }) => {
    const { context, page } = await visit(browser, '/app/students')
    await expect(page.locator('th[data-col="fullAddress"]')).toHaveCount(0)
    await page.locator('[data-columns]').click()
    await expect(
      page.locator('[data-column-toggle="fullAddress"]')
    ).toHaveCount(0)
    await page.keyboard.press('Escape')
    await page.locator('tbody tr').first().click()
    const panel = page.locator('[data-student-detail]')
    await expect(panel.locator('[data-detail="fullAddress"]')).toBeVisible()
    await expect(
      panel.locator('[data-attendance-dots] [data-status]').first()
    ).toBeVisible()
    await context.close()
  })

  test('hero; "Xavf ostida" filters the list; the Telegram chip', async ({
    browser
  }) => {
    const { context, page } = await visit(browser, '/app/students')
    const hero = page.locator('[data-crud-hero]')
    await expect(hero.locator('[data-kpi="students"]')).toContainText(
      String(fixtures()['/api/students'].totalElements)
    )
    await hero.locator('[data-kpi="risk"]').click()
    await expect
      .poll(() => lastQuery(context, '/api/students').get('atRisk'))
      .toBe('true')
    await page.locator('[data-filters-clear]').click()
    await page.locator('[data-filter="telegram"] [data-chip="false"]').click()
    await expect
      .poll(() => lastQuery(context, '/api/students').get('telegram'))
      .toBe('false')
    await expect(
      page.locator('tbody tr').first().locator('.cc-bool--no')
    ).toBeVisible()
    await context.close()
  })

  test('bulk: move to another class; message the parents', async ({
    browser
  }) => {
    const { context, page } = await visit(browser, '/app/students')
    await page.locator('tbody [data-check]').nth(0).check()
    await page.locator('tbody [data-check]').nth(1).check()
    await page
      .locator('[data-bulk-bar]')
      .getByRole('button', { name: 'Boshqa sinfga' })
      .click()
    const dlg = page.locator('.q-dialog').last()
    await dlg.getByRole('radio').nth(1).click()
    await dlg.getByRole('button', { name: "Ko'chirish" }).click()
    await expect
      .poll(
        () => sent(context, 'POST', '/api/pages/lists/students/move').length
      )
      .toBe(1)
    expect(
      JSON.parse(
        sent(context, 'POST', '/api/pages/lists/students/move')[0].body
      ).studentIds
    ).toHaveLength(2)
    // the list reloads (and the choice clears) once the move is done
    await expect(
      page.locator('.q-notification', { hasText: "ko'chirildi" })
    ).toBeVisible()
    await page.locator('tbody [data-check]').nth(0).check()
    await page
      .locator('[data-bulk-bar]')
      .getByRole('button', { name: 'Ota-onalarga xabar' })
      .click()
    await expect(page).toHaveURL(/broadcasts\?students=\d+/)
    await context.close()
  })
})

test.describe('Lavozimlar', () => {
  test('a card shows its people and opens Xodimlar filtered by it', async ({
    browser
  }) => {
    const { context, page } = await visit(browser, '/app/positions')
    const pos = fixtures()['/api/positions'].content.find(
      p => p.employeeCount > 1
    )
    const card = page.locator(`[data-position-card="${pos.id}"]`)
    await expect(card.locator('[data-employee-count]')).toHaveText(
      String(pos.employeeCount)
    )
    await card.click()
    await expect(page).toHaveURL(
      new RegExp(`/app/employees\\?positionId=${pos.id}`)
    )
    await expect
      .poll(() => lastQuery(context, '/api/employees').get('positionId'))
      .toBe(String(pos.id))
    await expect(
      page.locator(`[data-filter="positionId"] [data-chip="${pos.id}"]`)
    ).toHaveAttribute('aria-pressed', 'true')
    await context.close()
  })
})

test.describe('Xodimlar', () => {
  test('hero, the "Sinf rahbarlari" chip, load and journal columns', async ({
    browser
  }) => {
    const { context, page } = await visit(browser, '/app/employees')
    await expect(
      page.locator('[data-crud-hero] [data-kpi="load"]')
    ).toContainText('soat')
    await page
      .locator('[data-filter="classTeacher"] [data-chip="true"]')
      .click()
    await expect
      .poll(() => lastQuery(context, '/api/employees').get('classTeacher'))
      .toBe('true')
    const leads = fixtures()['/api/employees'].content.filter(
      e => e.classTeacher
    ).length
    await expect(page.locator('[data-page-range]')).toContainText(`/ ${leads}`)
    await expect(page.locator('th[data-col="weeklyLessons"]')).toBeVisible()
    await expect(page.locator('th[data-col="journal7"]')).toBeVisible()
    await context.close()
  })

  test('the panel: heat map, this week, the way to the timetable', async ({
    browser
  }) => {
    const { context, page } = await visit(browser, '/app/employees')
    const t = fixtures()['/api/employees'].content.find(
      e => e.weeklyLessons > 0
    )
    await page.locator(`tr[data-row-id="${t.id}"]`).click()
    const panel = page.locator('[data-employee-detail]')
    await expect(
      panel.locator('[data-load-heat] .emd__cell--on')
    ).not.toHaveCount(0)
    await expect(panel.locator('[data-week-grid]')).toBeVisible()
    await panel.locator('[data-open-timetable]').click()
    await expect(page).toHaveURL(
      new RegExp(`/timetable\\?view=teacher&teacher=${t.id}`)
    )
    await context.close()
  })
})

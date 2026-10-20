// "Before" / "after" pictures of the five CRUD pages (docs/images/crud-v2/), on fixtures from the
// TEST database (crud-mock.js): 1920 light and dark, 390 — and the extra views once they exist.
//   CRUD_FIXTURES=…/crud.json CRUD_SHOTS=before UI_BASE=http://localhost:9103 \
//     npx playwright test tests/ui/crud-shots.spec.js
import { test } from '@playwright/test'
import { join } from 'node:path'
import { hasFixtures, mockContext, open } from './crud-mock.js'

test.skip(!hasFixtures(), 'CRUD_FIXTURES is not set')

const KIND = process.env.CRUD_SHOTS || 'after'
const DIR = join(process.cwd(), '..', 'docs', 'images', 'crud-v2')
const PAGES = {
  'academic-years': '/app/academic-years',
  subjects: '/app/subjects',
  students: '/app/students',
  positions: '/app/positions',
  employees: '/app/employees'
}
const SIZES = [
  ['1920-light', { width: 1920, height: 1080 }, false],
  ['1920-dark', { width: 1920, height: 1080 }, true],
  ['390', { width: 390, height: 844 }, true]
]

for (const [name, path] of Object.entries(PAGES)) {
  for (const [label, viewport, dark] of SIZES) {
    test(`${KIND} ${name} ${label}`, async ({ browser }) => {
      const context = await mockContext(browser, viewport, { dark })
      const page = await context.newPage()
      await open(page, path)
      await page.screenshot({
        path: join(DIR, `${KIND}-${name}-${label}.png`),
        fullPage: viewport.width < 600
      })
      await context.close()
    })
  }
}

// the record in the side panel and the wide form (light and dark)
for (const dark of [false, true]) {
  const theme = dark ? 'dark' : 'light'
  test(`${KIND} detail and form ${theme}`, async ({ browser }) => {
    test.skip(KIND === 'before', 'the old page has no record panel')
    const context = await mockContext(
      browser,
      { width: 1920, height: 1080 },
      { dark }
    )
    const page = await context.newPage()
    await open(page, '/app/employees')
    await page.locator('tbody tr').nth(9).click()
    await page.waitForSelector('[data-crud-detail]')
    await page.waitForTimeout(400)
    await page.screenshot({
      path: join(DIR, `${KIND}-crud-detail-${theme}.png`)
    })
    await page.locator('[data-detail-edit]').click()
    await page.waitForSelector('[data-crud-form]')
    await page.waitForTimeout(400)
    await page.screenshot({ path: join(DIR, `${KIND}-crud-form-${theme}.png`) })
    await context.close()
  })
}

test(`${KIND} cards and bulk bar`, async ({ browser }) => {
  test.skip(KIND === 'before', 'the old page has neither')
  const context = await mockContext(
    browser,
    { width: 1920, height: 1080 },
    { dark: true }
  )
  const page = await context.newPage()
  await open(page, '/app/employees?view=cards')
  await page.screenshot({ path: join(DIR, `${KIND}-crud-cards-dark.png`) })
  await open(page, '/app/students')
  const boxes = page.locator('tbody [data-check]')
  await boxes.nth(1).check()
  await boxes.nth(4).click({ modifiers: ['Shift'] })
  await page.waitForTimeout(300)
  await page.screenshot({ path: join(DIR, `${KIND}-crud-bulk-dark.png`) })
  await context.close()
})

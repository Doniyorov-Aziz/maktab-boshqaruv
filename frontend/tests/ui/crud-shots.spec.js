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

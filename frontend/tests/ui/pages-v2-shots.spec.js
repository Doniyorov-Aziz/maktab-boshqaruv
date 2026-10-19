// Screenshots of the v2 pages for docs/images/pages-v2/ (or SHOTS_DIR): each in dark and
// light at 1920, and one per page at 390. Data from the fixtures (see pages-v2-mock.js).
import { test } from '@playwright/test'
import { fileURLToPath } from 'node:url'
import { mkdirSync } from 'node:fs'
import { join } from 'node:path'
import { hasFixtures, mockContext, open } from './pages-v2-mock.js'

const DIR =
  process.env.SHOTS_DIR ||
  fileURLToPath(new URL('../../../docs/images/pages-v2/', import.meta.url))

test.skip(!hasFixtures(), 'PAGES_FIXTURES is not set')

async function startDrag(page) {
  const dt = await page.evaluateHandle(() => new DataTransfer())
  const card = page.locator('[data-week-grid] [data-lesson]').first()
  await card.dispatchEvent('dragstart', { dataTransfer: dt })
  await page.waitForTimeout(150)
  const bad = page.locator('.wg__cell--bad').first()
  if (await bad.count())
    await bad.dispatchEvent('dragover', { dataTransfer: dt })
  await page.waitForTimeout(150)
}

const SHOTS = [
  { name: 'timetable-class', path: '/timetable?view=class' },
  { name: 'timetable-today', path: '/timetable?view=today' },
  { name: 'timetable-drag', path: '/timetable?view=class', act: startDrag },
  { name: 'calendar-month', path: '/calendar?view=month' },
  { name: 'calendar-year', path: '/calendar?view=year' },
  { name: 'classes-grid', path: '/classes' },
  { name: 'class-detail', path: '/classes/1' },
  { name: 'rooms-live', path: '/rooms' },
  {
    name: 'rooms-finder',
    path: '/rooms',
    act: async page => {
      await page.click('[data-open-finder]')
      await page.waitForSelector('[data-finder-results]')
      await page.waitForTimeout(400)
    }
  }
]

for (const s of SHOTS) {
  for (const dark of [true, false]) {
    test(`${s.name} 1920 ${dark ? 'dark' : 'light'}`, async ({ browser }) => {
      mkdirSync(DIR, { recursive: true })
      const context = await mockContext(
        browser,
        { width: 1920, height: 1080 },
        { dark }
      )
      const page = await context.newPage()
      await open(page, s.path)
      if (s.act) await s.act(page)
      await page.screenshot({
        path: join(DIR, `${s.name}-${dark ? 'dark' : 'light'}.png`),
        fullPage: !s.act
      })
      await context.close()
    })
  }
}

for (const p of ['timetable', 'calendar', 'classes', 'rooms']) {
  test(`${p} 390 mobile`, async ({ browser }) => {
    mkdirSync(DIR, { recursive: true })
    const context = await mockContext(
      browser,
      { width: 390, height: 844 },
      { dark: true }
    )
    const page = await context.newPage()
    await open(page, `/${p}`)
    await page.screenshot({
      path: join(DIR, `${p}-mobile.png`),
      fullPage: true
    })
    await context.close()
  })
}

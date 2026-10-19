// "Before" screenshots of the old attendance and journal pages (docs/images/attendance-journal/
// before-*.png): 1920 dark and light, 1366, 390. Data from the fixtures (aj-mock.js).
import { test } from '@playwright/test'
import { fileURLToPath } from 'node:url'
import { mkdirSync } from 'node:fs'
import { join } from 'node:path'
import { hasFixtures, mockContext, open } from './aj-mock.js'

const DIR = process.env.SHOTS_DIR || fileURLToPath(new URL('../../../docs/images/attendance-journal/', import.meta.url))

test.skip(!hasFixtures(), 'AJ_FIXTURES is not set')
test.skip(!process.env.AJ_BEFORE, 'only when AJ_BEFORE=1 (the old pages)')

/** The old attendance page: class 1-B from the select, then the first lesson of the list. */
async function oldAttendance(page) {
  await open(page, '/attendance')
  await page.getByLabel('Sinf').first().click()
  await page.locator('.q-menu .q-item', { hasText: '1-B' }).first().click()
  await page.waitForTimeout(300)
  await page.getByLabel('Dars').first().click()
  await page.locator('.q-menu .q-item').first().click()
  await page.waitForTimeout(600)
}

const SIZES = [
  { tag: '1920-dark', size: { width: 1920, height: 1080 }, dark: true },
  { tag: '1920-light', size: { width: 1920, height: 1080 }, dark: false },
  { tag: '1366', size: { width: 1366, height: 768 }, dark: true },
  { tag: '390', size: { width: 390, height: 844 }, dark: true }
]

for (const s of SIZES) {
  test(`before attendance ${s.tag}`, async ({ browser }) => {
    mkdirSync(DIR, { recursive: true })
    const context = await mockContext(browser, s.size, { dark: s.dark })
    const page = await context.newPage()
    await oldAttendance(page)
    await page.screenshot({ path: join(DIR, `before-attendance-${s.tag}.png`), fullPage: true })
    await context.close()
  })
  test(`before journal ${s.tag}`, async ({ browser }, info) => {
    void info
    mkdirSync(DIR, { recursive: true })
    const context = await mockContext(browser, s.size, { dark: s.dark })
    const page = await context.newPage()
    const { fixtures } = await import('./aj-mock.js')
    await open(page, `/gradebook?class=${fixtures().classA}`)
    await page.screenshot({ path: join(DIR, `before-journal-${s.tag}.png`), fullPage: true })
    await context.close()
  })
}

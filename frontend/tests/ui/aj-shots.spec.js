// "After" screenshots of the new attendance and journal pages (docs/images/attendance-journal/
// after-*.png). Data from the fixtures (aj-mock.js); a few students are given warnings and
// statuses so every state of a row shows. SHOTS_DIR sends them elsewhere (a review pass).
import { test } from '@playwright/test'
import { fileURLToPath } from 'node:url'
import { mkdirSync } from 'node:fs'
import { join } from 'node:path'
import { hasFixtures, fixtures, mockContext, open } from './aj-mock.js'

const DIR =
  process.env.SHOTS_DIR ||
  fileURLToPath(
    new URL('../../../docs/images/attendance-journal/', import.meta.url)
  )

test.skip(!hasFixtures(), 'AJ_FIXTURES is not set')

const WIDE = { width: 1920, height: 1080 }
const LAPTOP = { width: 1366, height: 768 }
const PHONE = { width: 390, height: 844 }

/** Class A today: the lesson at 10:40 with a few absent / late / excused students and warnings. */
export function spiceRoster() {
  const f = fixtures()
  for (const [key, data] of Object.entries(f)) {
    if (
      !key.startsWith('/api/pages/attendance/roster') ||
      !data?.rows ||
      data.spiced
    )
      continue
    data.spiced = true
    const [a, b, c, d] = data.rows
    if (a)
      Object.assign(a, { status: 'ABSENT', source: 'MANUAL', absentStreak: 3 })
    if (b)
      Object.assign(b, {
        status: 'LATE',
        source: 'MANUAL',
        lateMinutes: 10,
        lateThisWeek: 2
      })
    if (c)
      Object.assign(c, {
        status: 'EXCUSED',
        source: 'MANUAL',
        comment: 'Kasallik'
      })
    if (d)
      d.history = d.history.map((h, i) =>
        i % 4 === 1 ? { ...h, status: 'ABSENT' } : h
      )
  }
}

async function attendance(browser, size, opts = {}) {
  spiceRoster()
  const context = await mockContext(browser, size, opts)
  await context.addInitScript(
    id => localStorage.setItem('attendance.classId', String(id)),
    fixtures().classA
  )
  const page = await context.newPage()
  await open(page, '/attendance', { at: opts.at || '10:40' })
  return { context, page }
}

test('after attendance list dark', async ({ browser }) => {
  mkdirSync(DIR, { recursive: true })
  const { context, page } = await attendance(browser, WIDE, { dark: true })
  await page.screenshot({
    path: join(DIR, 'after-attendance-list-dark.png'),
    fullPage: true
  })
  await context.close()
})

test('after attendance list light', async ({ browser }) => {
  const { context, page } = await attendance(browser, WIDE, { dark: false })
  await page.screenshot({
    path: join(DIR, 'after-attendance-list-light.png'),
    fullPage: true
  })
  await context.close()
})

test('after attendance 1366', async ({ browser }) => {
  const { context, page } = await attendance(browser, LAPTOP, { dark: true })
  await page.screenshot({ path: join(DIR, 'after-attendance-1366.png') })
  await context.close()
})

test('after attendance cards', async ({ browser }) => {
  const { context, page } = await attendance(browser, WIDE, { dark: true })
  await page.getByRole('tab', { name: 'Kartochkalar' }).click()
  await page.waitForTimeout(300)
  await page.screenshot({
    path: join(DIR, 'after-attendance-cards.png'),
    fullPage: true
  })
  await context.close()
})

test('after attendance mobile', async ({ browser }) => {
  const { context, page } = await attendance(browser, PHONE, { dark: true })
  await page.screenshot({
    path: join(DIR, 'after-attendance-mobile.png'),
    fullPage: true
  })
  await context.close()
})

// ---------------------------------------------------------------- journal

async function journal(browser, size, opts = {}) {
  const context = await mockContext(browser, size, opts)
  const page = await context.newPage()
  const f = fixtures()
  await open(
    page,
    `/gradebook?class=${f.classA}&subject=${f.subjectA}&month=${f.today.slice(0, 7)}`,
    {
      at: opts.at || '11:30'
    }
  )
  return { context, page }
}

test('after journal dark', async ({ browser }) => {
  mkdirSync(DIR, { recursive: true })
  const { context, page } = await journal(browser, WIDE, { dark: true })
  await page.screenshot({ path: join(DIR, 'after-journal-dark.png') })
  await context.close()
})

test('after journal light', async ({ browser }) => {
  const { context, page } = await journal(browser, WIDE, { dark: false })
  await page.screenshot({ path: join(DIR, 'after-journal-light.png') })
  await context.close()
})

test('after journal 1366', async ({ browser }) => {
  const { context, page } = await journal(browser, LAPTOP, { dark: true })
  await page.screenshot({ path: join(DIR, 'after-journal-1366.png') })
  await context.close()
})

test('after journal heat', async ({ browser }) => {
  const { context, page } = await journal(browser, WIDE, { dark: true })
  await page.locator('[data-heat-toggle]').click()
  await page.waitForTimeout(200)
  await page.screenshot({ path: join(DIR, 'after-journal-heat.png') })
  await context.close()
})

test('after journal popover', async ({ browser }) => {
  const { context, page } = await journal(browser, WIDE, { dark: true })
  const ci = await page.locator('thead .gb--today').getAttribute('data-col')
  await page.locator(`td[data-row="2"][data-col="${ci}"]`).click()
  await page.waitForSelector('[data-grade-popover]')
  await page.waitForTimeout(300)
  await page.screenshot({ path: join(DIR, 'after-journal-popover.png') })
  await context.close()
})

test('after journal mobile', async ({ browser }) => {
  const { context, page } = await journal(browser, PHONE, { dark: true })
  await page.screenshot({
    path: join(DIR, 'after-journal-mobile.png'),
    fullPage: true
  })
  await context.close()
})

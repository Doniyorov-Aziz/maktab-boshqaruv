// The public demo (#/demo?ref=linkedin): the visitor becomes the read-only "Mehmon", sees the
// banner and every page, gets the "ruxsat yo'q" page for parents' messages, finds the editing
// buttons disabled — and no POST / PUT / PATCH / DELETE ever leaves the browser. Then the
// administrator sees that one visitor (from LinkedIn) on "Tashriflar" and the home page.
// Screenshots: docs/images/guest/*.png. Needs a backend with app.guest.login-enabled=true
// (UI_API) and an administrator (UI_USER / UI_PASS) — see docs/guest-demo-progress.md.
import { fileURLToPath } from 'node:url'
import { test, expect } from '@playwright/test'
import { BASE, API, openContext, visit } from './session.js'
import { pages } from './pages.js'

const SHOTS = fileURLToPath(
  new URL('../../../docs/images/guest/', import.meta.url)
)
// a real browser's User-Agent: "HeadlessChrome" is a crawler for the visit counter
const UA =
  'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36'
const VIEWPORT = { width: 1440, height: 900 }
const ALLOWED_WRITES =
  /\/api\/(auth\/(guest-login|login|logout)|demo\/visit\/ping)(\?|$)/
const LOCKED_ROUTES = new Set([
  '/parent-messages',
  '/absence-requests',
  '/notifications',
  '/broadcasts'
])

test.describe.configure({ mode: 'serial' })

let context
let page
const writes = []
const serverErrors = []
// 403 answers while the guest only looks around (no clicks on closed things)
const forbidden = []

test.beforeAll(async ({ browser }) => {
  context = await browser.newContext({ viewport: VIEWPORT, userAgent: UA })
  context.on('request', r => {
    if (!r.url().startsWith(API)) return
    if (['GET', 'HEAD', 'OPTIONS'].includes(r.method())) return
    if (ALLOWED_WRITES.test(r.url())) return
    writes.push(`${r.method()} ${r.url()}`)
  })
  context.on('response', r => {
    // the same for the administrator, not the guest's doing: book covers whose files are not in
    // this test backend's storage, and /api/rooms on the test data (two "current" lessons in a room)
    if (/\/api\/library\/books\/\d+\/cover|\/api\/rooms\?/.test(r.url())) return
    if (r.url().startsWith(API) && r.status() === 403) forbidden.push(r.url())
    if (r.url().startsWith(API) && r.status() >= 500) {
      serverErrors.push(`${r.status()} ${r.url()}`)
    }
  })
  page = await context.newPage()
})

test.afterAll(async () => {
  await context?.close()
})

test('the demo link signs in as Mehmon and opens the home page with the banner', async () => {
  await page.goto(BASE + '/#/demo?ref=linkedin')
  await page.waitForURL(/#\/$/, { timeout: 15_000 })
  await expect(page.getByTestId('guest-banner')).toContainText(
    "Demo rejimi — barcha bo'limlarni ko'rishingiz mumkin, o'zgartirish o'chirilgan"
  )
  const role = await page.evaluate(() => localStorage.getItem('role'))
  expect(role).toBe('GUEST')
  await page.waitForLoadState('networkidle')
  await page.waitForTimeout(800)
  await page.screenshot({ path: SHOTS + '01-dashboard-banner.png' })
})

test('closed sections keep their place in the menu, with a lock', async () => {
  const locks = page.getByTestId('nav-lock')
  await expect(locks).toHaveCount(5)
  await page
    .locator('.q-drawer')
    .getByText('Murojaatlar')
    .scrollIntoViewIfNeeded()
  await page.waitForTimeout(300)
  await page
    .locator('.q-drawer')
    .screenshot({ path: SHOTS + '02-sidebar-locks.png' })
})

test('"Murojaatlar" (Maktabga xat) opens the "ruxsat yo\'q" page', async () => {
  await page.locator('.q-drawer').getByText('Murojaatlar').click()
  await expect(page.getByTestId('guest-locked')).toContainText(
    "Uzr, sizga bu bo'limga kirishga ruxsat yo'q. Ota-onalar yozishmalari maxfiy ma'lumot hisoblanadi."
  )
  await page.waitForTimeout(400)
  await page.screenshot({ path: SHOTS + '03-locked-page.png' })
  // by address too
  await visit(page, '/broadcasts')
  await expect(page.getByTestId('guest-locked')).toBeVisible()
})

test('every page opens; editing buttons are disabled and send nothing', async () => {
  const report = []
  forbidden.length = 0
  for (const p of pages) {
    if (p.public || p.bare) continue
    await visit(page, p.path)
    if (LOCKED_ROUTES.has(p.path)) {
      await expect(page.getByTestId('guest-locked'), p.path).toBeVisible()
      continue
    }
    await expect(page.getByTestId('guest-locked'), p.path).toHaveCount(0)
    await expect(page.getByTestId('guest-banner'), p.path).toBeVisible()
    // a disabled button does nothing even when clicked "through" the wrapper
    const off = page.locator('[data-guest-disabled] button')
    const n = await off.count()
    for (let i = 0; i < Math.min(n, 4); i++) {
      const b = off.nth(i)
      if (!(await b.isVisible())) continue
      await expect(b).toBeDisabled()
      await b.click({ force: true, timeout: 2000 }).catch(() => {})
    }
    report.push(`${p.path}: ${n} disabled`)
    await page.keyboard.press('Escape')
  }
  console.log(report.join('\n'))
  expect(writes).toEqual([])
  expect(serverErrors).toEqual([])
  // no open page asks for a closed section behind the scenes
  expect(forbidden).toEqual([])
})

test('a disabled button says why on hover', async () => {
  await visit(page, '/app/students')
  // a row's pencil is aria-disabled; clicked anyway: no form opens, a notice says why
  const pencil = page.locator('[data-action="edit"]').first()
  await expect(pencil).toBeDisabled()
  await pencil.click({ force: true })
  await expect(page.locator('.q-notification').first()).toContainText(
    "Demo rejimida o'zgartirish o'chirilgan"
  )
  await expect(page.locator('.q-dialog form')).toHaveCount(0)
  const wrap = page.locator('[data-guest-disabled]', {
    hasText: "Yangi qo'shish"
  })
  await wrap.hover()
  await expect(page.locator('.q-tooltip').last()).toContainText(
    "Demo rejimida o'zgartirish o'chirilgan"
  )
  await page.waitForTimeout(500)
  await page.screenshot({ path: SHOTS + '04-disabled-button-tooltip.png' })
})

test('marking attendance, a grade or a calendar event sends nothing', async () => {
  await visit(page, '/calendar')
  // a day cell: creating an event is refused in the browser
  const day = page.locator('[data-day], .mv-day, .month-day').first()
  if (await day.count()) await day.dblclick({ force: true }).catch(() => {})
  await visit(page, '/attendance')
  await page.keyboard.press('1')
  await visit(page, '/gradebook')
  const cell = page.locator('[data-cell], .gt-cell, td').nth(5)
  if (await cell.count()) await cell.click({ force: true }).catch(() => {})
  await page.waitForTimeout(500)
  await page.screenshot({ path: SHOTS + '05-gradebook-readonly.png' })
  expect(writes).toEqual([])
})

test('the administrator sees one visitor from LinkedIn', async ({
  browser
}) => {
  const admin = await openContext(browser, VIEWPORT)
  const a = await admin.newPage()
  await visit(a, '/visits')
  await expect(a.getByTestId('visits-page')).toBeVisible()
  await expect(a.locator('[data-kpi="visitors"]')).toContainText('1 kishi', {
    timeout: 10_000
  })
  await expect(a.getByTestId('visits-sources')).toContainText('LinkedIn')
  await a.waitForTimeout(800)
  await a.screenshot({ path: SHOTS + '06-visits-page.png', fullPage: true })

  await visit(a, '/')
  await expect(a.getByTestId('demo-visits-widget')).toContainText(
    'Demo tashriflari: 1 kishi'
  )
  await a
    .locator('.page-layout__header')
    .screenshot({ path: SHOTS + '07-home-widget.png' })
  await admin.close()
})

test('the login page offers the demo', async ({ browser }) => {
  const anon = await openContext(browser, VIEWPORT, { signedIn: false })
  const l = await anon.newPage()
  await visit(l, '/login')
  await expect(l.getByTestId('login-demo')).toContainText(
    "Demo sifatida ko'rish"
  )
  await l.screenshot({ path: SHOTS + '08-login-demo-button.png' })
  await anon.close()
})

test('on a phone: banner and locked page', async ({ browser }) => {
  const phone = await browser.newContext({
    viewport: { width: 390, height: 844 },
    userAgent: UA,
    isMobile: true,
    hasTouch: true
  })
  const m = await phone.newPage()
  await m.goto(BASE + '/#/demo?ref=telegram')
  await m.waitForURL(/#\/$/, { timeout: 15_000 })
  await m.waitForLoadState('networkidle')
  await m.waitForTimeout(600)
  await m.screenshot({ path: SHOTS + '09-phone-dashboard.png' })
  await m.goto(BASE + '/#/parent-messages')
  await expect(m.getByTestId('guest-locked')).toBeVisible()
  await m.waitForTimeout(600)
  await m.screenshot({ path: SHOTS + '10-phone-locked.png' })
  await phone.close()
})

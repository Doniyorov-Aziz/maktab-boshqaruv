// Dashboard screenshots for docs/images/dashboard/:
//   node tests/ui/dashboard-shots.mjs before            → before-<width>-<theme>.png
//   node tests/ui/dashboard-shots.mjs after             → after-admin-<width>-<theme>.png (+ teacher with UI_TEACHER_USER)
import { chromium } from 'playwright'
import { openContext, visit, BASE, API } from './session.js'

const phase = process.argv[2] || 'before'
const dir = new URL('../../../docs/images/dashboard/', import.meta.url)
const file = name => new URL(name, dir).pathname.replace(/^\/([A-Z]:)/, '$1')
const shots = [
  { width: 1366, height: 768, dark: false },
  { width: 1366, height: 768, dark: true },
  { width: 1920, height: 1080, dark: false },
  { width: 1920, height: 1080, dark: true },
  { width: 2560, height: 1440, dark: true },
  { width: 390, height: 844, dark: false },
  { width: 390, height: 844, dark: true }
]

const AFTER = [
  { name: 'after-admin-1920-dark.png', width: 1920, height: 1080, dark: true },
  {
    name: 'after-admin-1920-light.png',
    width: 1920,
    height: 1080,
    dark: false
  },
  { name: 'after-admin-1366-dark.png', width: 1366, height: 768, dark: true },
  { name: 'after-admin-2560-dark.png', width: 2560, height: 1440, dark: true },
  { name: 'after-admin-mobile.png', width: 390, height: 844, dark: false },
  {
    name: 'after-teacher-1920-dark.png',
    width: 1920,
    height: 1080,
    dark: true,
    user: 'ui_teacher'
  }
]

async function teacherContext(browser, s) {
  const r = await fetch(API + '/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: s.user, password: process.env.UI_PASS })
  })
  const t = (await r.text()).trim()
  const context = await browser.newContext({
    viewport: { width: s.width, height: s.height }
  })
  await context.addInitScript(
    ({ tok, dark }) => {
      const c = JSON.parse(
        atob(tok.split('.')[1].replace(/-/g, '+').replace(/_/g, '/'))
      )
      localStorage.setItem('token', tok)
      localStorage.setItem('username', c.sub)
      localStorage.setItem('role', c.role)
      if (c.employeeId) localStorage.setItem('employeeId', String(c.employeeId))
      localStorage.setItem('activeSchoolId', '1')
      localStorage.setItem('darkMode', String(dark))
    },
    { tok: t, dark: s.dark }
  )
  return context
}

const browser = await chromium.launch()
if (phase === 'after') {
  for (const s of AFTER) {
    const context = s.user
      ? await teacherContext(browser, s)
      : await openContext(
          browser,
          { width: s.width, height: s.height },
          { dark: s.dark }
        )
    const page = await context.newPage()
    const errors = []
    page.on('pageerror', e => errors.push(e.message))
    page.on('console', m => m.type() === 'error' && errors.push(m.text()))
    await visit(page, '/')
    await page.waitForTimeout(2500)
    await page.screenshot({ path: file(s.name), fullPage: true })
    console.log(
      s.name,
      errors.length ? 'errors: ' + errors.join(' | ').slice(0, 300) : 'ok'
    )
    await context.close()
  }
  await browser.close()
  process.exit(0)
}

for (const s of shots) {
  if (phase === 'before' && s.width === 2560) continue
  const context = await openContext(
    browser,
    { width: s.width, height: s.height },
    { dark: s.dark }
  )
  const page = await context.newPage()
  const errors = []
  page.on('pageerror', e => errors.push(e.message))
  page.on('console', m => m.type() === 'error' && errors.push(m.text()))
  await visit(page, '/')
  await page.waitForTimeout(1200)
  const tag = s.width === 390 ? 'mobile' : String(s.width)
  const name =
    phase === 'before'
      ? `before-${s.width}-${s.dark ? 'dark' : 'light'}.png`
      : `after-admin-${tag}-${s.dark ? 'dark' : 'light'}.png`
  await page.screenshot({ path: file(name), fullPage: true })
  console.log(
    name,
    errors.length ? 'errors: ' + errors.join(' | ').slice(0, 300) : 'ok'
  )
  await context.close()
}
await browser.close()
void BASE

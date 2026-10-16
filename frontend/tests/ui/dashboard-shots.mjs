// Dashboard screenshots for docs/images/dashboard/:
//   node tests/ui/dashboard-shots.mjs v3-before   → v3-before-<width>-<theme>.png (the page before v3)
//   node tests/ui/dashboard-shots.mjs v3          → v3-admin-*.png, v3-teacher-1920-dark.png and the
//                                                   10 s loading video v3-intro.webm
// The teacher shot signs in as UI_TEACHER_USER (default ui_teacher) with UI_PASS.
import { chromium } from 'playwright'
import { copyFileSync, mkdtempSync, readdirSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { openContext, visit, BASE } from './session.js'
import { API } from './session.js'

const phase = process.argv[2] || 'v3'
const dir = new URL('../../../docs/images/dashboard/', import.meta.url)
const file = name => new URL(name, dir).pathname.replace(/^\/([A-Z]:)/, '$1')
const TEACHER = process.env.UI_TEACHER_USER || 'ui_teacher'

const BEFORE = [
  { width: 1366, height: 768, dark: true },
  { width: 1920, height: 1080, dark: false },
  { width: 1920, height: 1080, dark: true },
  { width: 390, height: 844, dark: false }
]

const V3 = [
  { name: 'v3-admin-1920-dark.png', width: 1920, height: 1080, dark: true },
  { name: 'v3-admin-1920-light.png', width: 1920, height: 1080, dark: false },
  { name: 'v3-admin-1366-dark.png', width: 1366, height: 768, dark: true },
  { name: 'v3-admin-2560-dark.png', width: 2560, height: 1440, dark: true },
  { name: 'v3-admin-mobile.png', width: 390, height: 844, dark: true },
  {
    name: 'v3-teacher-1920-dark.png',
    width: 1920,
    height: 1080,
    dark: true,
    teacher: true
  }
]

async function teacherContext(browser, s, extra = {}) {
  const r = await fetch(API + '/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: TEACHER, password: process.env.UI_PASS })
  })
  const t = (await r.text()).trim()
  const context = await browser.newContext({
    viewport: { width: s.width, height: s.height },
    ...extra
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

async function shoot(browser, s, name) {
  const context = s.teacher
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
  await page.waitForTimeout(3000)
  await page.screenshot({ path: file(name), fullPage: true })
  console.log(
    name,
    errors.length ? 'errors: ' + errors.join(' | ').slice(0, 300) : 'ok'
  )
  await context.close()
}

const browser = await chromium.launch()
if (phase === 'v3-before') {
  for (const s of BEFORE) {
    await shoot(
      browser,
      s,
      `v3-before-${s.width}-${s.dark ? 'dark' : 'light'}.png`
    )
  }
} else {
  for (const s of V3) await shoot(browser, s, s.name)

  // the loading animation, 10 s at 1920 dark
  const videoDir = mkdtempSync(join(tmpdir(), 'dash-video-'))
  const context = await openContext(
    browser,
    { width: 1920, height: 1080 },
    {
      dark: true,
      recordVideo: { dir: videoDir, size: { width: 1920, height: 1080 } }
    }
  )
  const page = await context.newPage()
  await visit(page, '/')
  await page.waitForTimeout(10_000)
  await context.close()
  const webm = readdirSync(videoDir).find(f => f.endsWith('.webm'))
  copyFileSync(join(videoDir, webm), file('v3-intro.webm'))
  rmSync(videoDir, { recursive: true, force: true })
  console.log('v3-intro.webm ok')
}
await browser.close()
void BASE

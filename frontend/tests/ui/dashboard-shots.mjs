// Dashboard screenshots for docs/images/dashboard/:
//   node tests/ui/dashboard-shots.mjs before            → before-<width>-<theme>.png
//   node tests/ui/dashboard-shots.mjs after             → after-admin-<width>-<theme>.png (+ teacher with UI_TEACHER_USER)
import { chromium } from 'playwright'
import { openContext, visit, BASE } from './session.js'

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

const browser = await chromium.launch()
for (const s of shots) {
  if (phase === 'before' && s.width === 2560) continue
  const context = await openContext(browser, { width: s.width, height: s.height }, { dark: s.dark })
  const page = await context.newPage()
  const errors = []
  page.on('pageerror', e => errors.push(e.message))
  page.on('console', m => m.type() === 'error' && errors.push(m.text()))
  await visit(page, '/')
  await page.waitForTimeout(1200)
  const tag = s.width === 390 ? 'mobile' : String(s.width)
  const name = phase === 'before'
    ? `before-${s.width}-${s.dark ? 'dark' : 'light'}.png`
    : `after-admin-${tag}-${s.dark ? 'dark' : 'light'}.png`
  await page.screenshot({ path: file(name), fullPage: true })
  console.log(name, errors.length ? 'errors: ' + errors.join(' | ').slice(0, 300) : 'ok')
  await context.close()
}
await browser.close()
void BASE

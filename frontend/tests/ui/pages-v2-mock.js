// The v2 pages without a backend or a sign-in: every /api request is answered from a JSON
// file the backend itself wrote (PagesFixtureDump: real numbers of the test school), so the
// pages can be checked and photographed anywhere. Nothing here reaches a server — the token
// is a placeholder the app only reads its role from.
//   PAGES_FIXTURES=/path/fixtures.json npx playwright test tests/ui/pages-v2.spec.js
import { readFileSync, existsSync } from 'node:fs'
import { BASE } from './session.js'

export const FIXTURES = process.env.PAGES_FIXTURES || ''
export const hasFixtures = () => !!FIXTURES && existsSync(FIXTURES)

let cache = null
export function fixtures() {
  if (!cache) cache = JSON.parse(readFileSync(FIXTURES, 'utf8'))
  return cache
}

const b64 = o => Buffer.from(JSON.stringify(o)).toString('base64url')
// a placeholder: the app reads the role from it, no server ever sees it
const TOKEN = `${b64({ alg: 'none' })}.${b64({ sub: 'ui_mock', role: 'ADMIN', exp: 4102444800 })}.mock`

function answer(path, method, search) {
  const f = fixtures()
  if (method !== 'GET') return { status: 200, body: {} }
  if (path === '/api/schools')
    return {
      body: {
        content: [{ id: 1, name: '1-maktab', address: 'Toshkent' }],
        totalElements: 1
      }
    }
  if (path === '/api/pages/rooms/live')
    return { body: f['/api/pages/rooms/live@lesson'] }
  let m = path.match(/^\/api\/pages\/classes\/(\d+)\/month$/)
  if (m) return { body: f['/api/pages/classes/month'] }
  m = path.match(/^\/api\/pages\/classes\/(\d+)$/)
  if (m) return { body: f['/api/pages/classes/detail'] }
  m = path.match(/^\/api\/pages\/rooms\/(\d+)\/detail$/)
  if (m) return { body: f['/api/pages/rooms/detail'] }
  if (f[path] !== undefined) return { body: f[path] }
  if (path.startsWith('/api/dashboard/'))
    return { body: { asOf: null, data: null } }
  // anything else the layout asks for (badges, search lists): empty
  return {
    body:
      search.includes('size') || search.includes('page')
        ? { content: [], totalElements: 0, totalPages: 0 }
        : []
  }
}

/** A browser context signed in as a placeholder admin, every /api call answered from the fixtures. */
export async function mockContext(
  browser,
  viewport,
  { dark = false, reducedMotion = 'reduce' } = {}
) {
  const context = await browser.newContext({
    viewport,
    deviceScaleFactor: 1,
    reducedMotion
  })
  await context.addInitScript(
    ({ token, dark }) => {
      localStorage.setItem('darkMode', String(dark))
      localStorage.setItem('token', token)
      localStorage.setItem('username', 'ui_mock')
      localStorage.setItem('role', 'ADMIN')
      localStorage.setItem('activeSchoolId', '1')
      localStorage.setItem('activeSchoolName', '1-maktab')
    },
    { token: TOKEN, dark }
  )
  context.requests = []
  // "METHOD /path" → { status, body }: a test's own answer (a 409, say)
  context.overrides = {}
  await context.route('**/api/**', async route => {
    const url = new URL(route.request().url())
    const method = route.request().method()
    context.requests.push({
      method,
      path: url.pathname,
      body: route.request().postData()
    })
    const { status = 200, body } =
      context.overrides[`${method} ${url.pathname}`] ||
      answer(url.pathname, method, url.search)
    await route.fulfill({
      status,
      contentType: 'application/json',
      body: JSON.stringify(body ?? null)
    })
  })
  return context
}

/** The fixtures' day at hh:mm in Tashkent (UTC+5). */
export function tashkentTime(hhmm) {
  return new Date(`${fixtures().today}T${hhmm}:00+05:00`)
}

/** Opens a page; `at` ("10:40") fixes the browser clock to that minute of the fixtures' day. */
export async function open(page, path, { at = '10:40' } = {}) {
  if (at) await page.clock.install({ time: tashkentTime(at) })
  await page.goto(BASE + '/#' + path, { waitUntil: 'networkidle' })
  await page.waitForTimeout(500)
}

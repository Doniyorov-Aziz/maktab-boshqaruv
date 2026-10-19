// The attendance and journal pages without a backend or a sign-in: every /api call is answered
// from a JSON file the backend wrote from the TEST database (AttendanceJournalFixtureDump).
// Saves are remembered for the context (a reload shows them). Nothing reaches a server — the
// token is a placeholder the app only reads its role from.
//   AJ_FIXTURES=/path/aj.json UI_BASE=http://localhost:9102 npx playwright test tests/ui/aj*.spec.js
import { readFileSync, existsSync } from 'node:fs'
import { BASE } from './session.js'

export const FIXTURES = process.env.AJ_FIXTURES || ''
export const hasFixtures = () => !!FIXTURES && existsSync(FIXTURES)

let cache = null
export function fixtures() {
  if (!cache) cache = JSON.parse(readFileSync(FIXTURES, 'utf8'))
  return cache
}

const b64 = o => Buffer.from(JSON.stringify(o)).toString('base64url')
const token = role => `${b64({ alg: 'none' })}.${b64({ sub: 'ui_mock', role, exp: 4102444800 })}.mock`

/** The fixture for a request: "path?param=value" for the keys that tell rosters apart, else "path". */
function lookup(path, search) {
  const f = fixtures()
  const q = new URLSearchParams(search)
  for (const p of ['lessonSlotId', 'studentId', 'date']) {
    const v = q.get(p)
    if (v && f[`${path}?${p}=${v}`] !== undefined) return f[`${path}?${p}=${v}`]
  }
  return f[path]
}

/**
 * A browser context signed in as a placeholder user (ADMIN by default), /api answered from the
 * fixtures. context.requests lists what the page sent; context.state holds what it saved.
 */
export async function mockContext(browser, viewport, { dark = false, role = 'ADMIN', reducedMotion = 'reduce' } = {}) {
  const context = await browser.newContext({ viewport, deviceScaleFactor: 1, reducedMotion })
  await context.addInitScript(
    ({ t, dark, role }) => {
      localStorage.setItem('darkMode', String(dark))
      localStorage.setItem('token', t)
      localStorage.setItem('username', 'ui_mock')
      localStorage.setItem('role', role)
      localStorage.setItem('activeSchoolId', '1')
      localStorage.setItem('activeSchoolName', '1-maktab')
    },
    { t: token(role), dark, role }
  )
  context.requests = []
  context.overrides = {}
  context.state = { attendance: {}, grades: [], nextId: 900000 }
  await context.route('**/api/**', async route => {
    const req = route.request()
    const url = new URL(req.url())
    const method = req.method()
    const body = req.postData()
    context.requests.push({ method, path: url.pathname, search: url.search, body })
    const reply = (status, data) =>
      route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(data ?? null) })
    const o = context.overrides[`${method} ${url.pathname}`]
    if (o) return reply(o.status || 200, o.body)
    if (method !== 'GET') return reply(200, write(context.state, method, url.pathname, body))
    if (url.pathname === '/api/me/current-lesson') return route.fulfill({ status: 204, body: '' })
    if (url.pathname === '/api/schools')
      return reply(200, { content: [{ id: 1, name: '1-maktab', address: 'Toshkent' }], totalElements: 1 })
    const found = lookup(url.pathname, url.search)
    if (found !== undefined) return reply(200, read(context.state, url.pathname, url.search, found))
    if (url.pathname.startsWith('/api/dashboard/')) return reply(200, { asOf: null, data: null })
    return reply(200, url.search.includes('size') ? { content: [], totalElements: 0, totalPages: 0 } : [])
  })
  return context
}

// ---------------------------------------------------------------- remembered saves

function write(state, method, path, body) {
  const data = body ? JSON.parse(body) : {}
  if (path === '/api/attendance/bulk' || path === '/api/pages/attendance/save') {
    for (const e of data.entries || [])
      state.attendance[`${data.lessonSlotId}|${data.recordDate || data.date}|${e.studentId}`] = e
    return { saved: (data.entries || []).length, savedAt: new Date().toISOString() }
  }
  if (path === '/api/grades' && method === 'POST') {
    const g = { id: ++state.nextId, ...data }
    state.grades.push(g)
    return { id: g.id, createdBy: 'ui_mock', createdAt: new Date().toISOString() }
  }
  return {}
}

/** A roster with the remembered saves on top; a journal with the remembered grades. */
function read(state, path, search, data) {
  const q = new URLSearchParams(search)
  if (/roster$/.test(path) && Array.isArray(data?.rows || data)) {
    const rows = (data.rows || data).map(r => {
      const saved = state.attendance[`${q.get('lessonSlotId')}|${q.get('date')}|${r.studentId}`]
      return saved ? { ...r, status: saved.status, source: 'MANUAL', lateMinutes: saved.lateMinutes ?? null } : r
    })
    return data.rows ? { ...data, rows } : rows
  }
  if (path === '/api/grades/journal' && data?.grades) {
    const extra = state.grades.map(g => ({
      id: g.id,
      studentId: g.studentId,
      date: g.gradeDate,
      value: g.score,
      type: g.type || 'CURRENT',
      comment: g.comment || null,
      createdBy: 'ui_mock',
      createdAt: null,
      source: 'MANUAL'
    }))
    return { ...data, grades: [...data.grades, ...extra] }
  }
  return data
}

/** The fixtures' day at hh:mm in Tashkent (UTC+5). */
export function tashkentTime(hhmm) {
  return new Date(`${fixtures().today}T${hhmm}:00+05:00`)
}

/** Opens a page; `at` fixes the browser clock to that minute of the fixtures' day. */
export async function open(page, path, { at = '10:40' } = {}) {
  if (at) await page.clock.install({ time: tashkentTime(at) })
  await page.goto(BASE + '/#' + path, { waitUntil: 'networkidle' })
  await page.waitForTimeout(500)
}

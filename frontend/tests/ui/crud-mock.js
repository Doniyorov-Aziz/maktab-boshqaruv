// The CRUD pages without a backend or a sign-in: every /api call is answered from a JSON file
// the backend wrote from the TEST database (CrudFixtureDump). A list endpoint holds the WHOLE
// list; this file pages, sorts, searches (q) and filters it the way the server does, so the
// filters, the class picker and the pager can be checked for real. Saves are remembered for
// the context. Nothing reaches a server — the token is a placeholder the app reads its role from.
//   CRUD_FIXTURES=/path/crud.json UI_BASE=http://localhost:9103 npx playwright test tests/ui/crud*.spec.js
import { readFileSync, existsSync } from 'node:fs'
import { BASE } from './session.js'

export const FIXTURES = process.env.CRUD_FIXTURES || ''
export const hasFixtures = () => !!FIXTURES && existsSync(FIXTURES)

let cache = null
export function fixtures() {
  if (!cache) cache = JSON.parse(readFileSync(FIXTURES, 'utf8'))
  return cache
}

const b64 = o => Buffer.from(JSON.stringify(o)).toString('base64url')
const token = role =>
  `${b64({ alg: 'none' })}.${b64({ sub: 'ui_mock', role, exp: 4102444800 })}.mock`

// what "q" looks in, per list
const SEARCH = {
  '/api/students': r =>
    `${r.firstName} ${r.lastName} ${r.lastName} ${r.firstName}`,
  '/api/employees': r =>
    `${r.firstName} ${r.lastName} ${r.lastName} ${r.firstName} ${r.phone || ''}`,
  '/api/subjects': r => r.name,
  '/api/positions': r => r.title,
  '/api/academic-years': r => r.title
}
// a sort the server maps to real columns
const SORT_KEY = {
  fullName: r => `${r.lastName} ${r.firstName}`
}
// query parameters that are not filters
const NOT_FILTERS = new Set(['schoolId', 'page', 'size', 'sort', 'q'])

/** One page of a whole list, as the server would answer the query. */
export function serveList(path, all, search) {
  const q = new URLSearchParams(search)
  let rows = all
  const text = (q.get('q') || '').trim().toLowerCase().replace(/\s+/g, ' ')
  if (text && SEARCH[path])
    rows = rows.filter(r => SEARCH[path](r).toLowerCase().includes(text))
  if (path === '/api/subjects') {
    const st = q.get('status')
    if (st === 'INACTIVE') rows = rows.filter(r => r.active === false)
    else if (st === 'ACTIVE' || q.get('includeInactive') !== 'true')
      rows = rows.filter(r => r.active !== false)
  }
  for (const [k, v] of q) {
    if (NOT_FILTERS.has(k) || k === 'includeInactive' || v === '') continue
    if (k === 'status' && path === '/api/subjects') continue
    if (k === 'birthday') {
      const md = String(fixtures().today).slice(5, 10)
      rows = rows.filter(
        r => v !== 'true' || String(r.birthDate).slice(5, 10) === md
      )
      continue
    }
    if (k === 'classTeacher') {
      rows = rows.filter(r => v !== 'true' || !!r.classTeacher)
      continue
    }
    if (k === 'status' && path === '/api/employees') {
      rows = rows.filter(r => (r.status || 'ACTIVE') === v)
      continue
    }
    if (k === 'telegram') {
      rows = rows.filter(r => String(!!r.telegram) === v)
      continue
    }
    if (k === 'atRisk') {
      rows = rows.filter(r => v !== 'true' || (r.risk ?? 0) >= 40)
      continue
    }
    if (rows.length && k in rows[0]) rows = rows.filter(r => String(r[k]) === v)
  }
  const sort = q.get('sort')
  if (sort) {
    const [field, dir] = sort.split(',')
    const key = SORT_KEY[field] || (r => r[field])
    rows = [...rows].sort((a, b) => {
      const x = key(a)
      const y = key(b)
      const c =
        typeof x === 'number' && typeof y === 'number'
          ? x - y
          : String(x ?? '').localeCompare(String(y ?? ''), 'uz', {
              numeric: true
            })
      return dir === 'desc' ? -c : c
    })
  }
  const size = Number(q.get('size') || 20)
  const page = Number(q.get('page') || 0)
  return {
    content: rows.slice(page * size, page * size + size),
    totalElements: rows.length,
    totalPages: Math.ceil(rows.length / size),
    number: page,
    size
  }
}

/**
 * A browser context signed in as a placeholder user (ADMIN by default), /api answered from the
 * fixtures. context.requests lists what the page sent.
 */
export async function mockContext(
  browser,
  viewport,
  {
    dark = false,
    role = 'ADMIN',
    reducedMotion = 'reduce',
    storage = {},
    currentYear = false
  } = {}
) {
  const context = await browser.newContext({
    viewport,
    deviceScaleFactor: 1,
    reducedMotion
  })
  await context.addInitScript(
    ({ t, dark, role, storage }) => {
      localStorage.setItem('darkMode', String(dark))
      localStorage.setItem('token', t)
      localStorage.setItem('username', 'ui_mock')
      localStorage.setItem('role', role)
      localStorage.setItem('activeSchoolId', '1')
      localStorage.setItem('activeSchoolName', '1-maktab')
      for (const [k, v] of Object.entries(storage)) localStorage.setItem(k, v)
    },
    { t: token(role), dark, role, storage }
  )
  context.requests = []
  context.overrides = {}
  // the test database has no school year around today: a synthetic one (the usual calendar)
  if (currentYear)
    context.overrides['GET /api/pages/lists/years'] = {
      body: [syntheticYear(), ...(fixtures()['/api/pages/lists/years'] || [])]
    }
  await context.route('**/api/**', async route => {
    const req = route.request()
    const url = new URL(req.url())
    const method = req.method()
    context.requests.push({
      method,
      path: url.pathname,
      search: url.search,
      body: req.postData()
    })
    const reply = (status, data) =>
      route.fulfill({
        status,
        contentType: 'application/json',
        body: JSON.stringify(data ?? null)
      })
    const o = context.overrides[`${method} ${url.pathname}`]
    if (o) return reply(o.status || 200, o.body)
    if (method !== 'GET') {
      const body = req.postData()
      // the terms come back as saved; a move says how many moved
      if (method === 'PUT' && url.pathname.endsWith('/terms'))
        return reply(200, JSON.parse(body || '[]'))
      if (url.pathname === '/api/pages/lists/students/move') {
        const b = JSON.parse(body || '{}')
        return reply(200, {
          moved: (b.studentIds || []).length,
          className: '1-B'
        })
      }
      return reply(200, {})
    }
    if (url.pathname === '/api/me/current-lesson')
      return route.fulfill({ status: 204, body: '' })
    const f = fixtures()
    const found = f[url.pathname]
    if (found?.content)
      return reply(200, serveList(url.pathname, found.content, url.search))
    if (found !== undefined) return reply(200, found)
    // one record: /api/students/12
    const m = /^(\/api\/[a-z-]+)\/(\d+)$/.exec(url.pathname)
    if (m && f[m[1]]?.content) {
      const row = f[m[1]].content.find(r => String(r.id) === m[2])
      if (row) return reply(200, row)
      return reply(404, 'topilmadi')
    }
    if (url.pathname.startsWith('/api/dashboard/'))
      return reply(200, { asOf: null, data: null })
    return reply(
      200,
      url.search.includes('size')
        ? { content: [], totalElements: 0, totalPages: 0 }
        : []
    )
  })
  return context
}

/** Opens a page of the app (hash routing) and waits for it to settle. */
export async function open(page, path) {
  await page.goto(BASE + '/#' + path, { waitUntil: 'networkidle' })
  await page.waitForTimeout(400)
}

/** A current year around the fixtures' day, as the backend would answer (usual calendar). */
export function syntheticYear() {
  const today = fixtures().today
  const y0 =
    Number(today.slice(0, 4)) - (Number(today.slice(5, 7)) >= 8 ? 0 : 1)
  const y1 = y0 + 1
  const t = (name, kind, a, b) => ({
    id: null,
    name,
    kind,
    startDate: a,
    endDate: b
  })
  return {
    id: 999001,
    title: `${y0}–${y1}`,
    startDate: `${y0}-09-02`,
    endDate: `${y1}-05-25`,
    current: true,
    status: 'CURRENT',
    workDays: 200,
    passedDays: 42,
    leftDays: 158,
    progress: 21,
    classes: 22,
    students: 511,
    attendance: 96.2,
    quality: 68.4,
    terms: [
      t('1-chorak', 'QUARTER', `${y0}-09-02`, `${y0}-11-02`),
      t("Kuzgi ta'til", 'VACATION', `${y0}-11-03`, `${y0}-11-09`),
      t('2-chorak', 'QUARTER', `${y0}-11-10`, `${y0}-12-28`),
      t("Qishki ta'til", 'VACATION', `${y0}-12-29`, `${y1}-01-11`),
      t('3-chorak', 'QUARTER', `${y1}-01-12`, `${y1}-03-20`),
      t("Bahorgi ta'til", 'VACATION', `${y1}-03-21`, `${y1}-03-29`),
      t('4-chorak', 'QUARTER', `${y1}-03-30`, `${y1}-05-25`)
    ]
  }
}

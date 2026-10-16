// Signs in once through the API and opens pages with that token already in
// localStorage, so every check starts on the real page instead of the login form.
export const BASE = process.env.UI_BASE || 'http://localhost:9100'
export const API = process.env.UI_API || 'http://localhost:8081'
const USER = process.env.UI_USER || 'ui_audit'
const PASS = process.env.UI_PASS || ''

let token = null

export async function login() {
  if (token) return token
  const r = await fetch(API + '/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: USER, password: PASS })
  })
  if (!r.ok)
    throw new Error('Login failed: ' + r.status + ' (set UI_USER / UI_PASS)')
  token = (await r.text()).trim()
  return token
}

/** A fresh context: signed in (unless the page is public), school 1, the given theme. */
export async function openContext(
  browser,
  viewport,
  { dark = false, signedIn = true, reducedMotion, recordVideo } = {}
) {
  const t = signedIn ? await login() : null
  const context = await browser.newContext({
    viewport,
    deviceScaleFactor: 1,
    ...(reducedMotion ? { reducedMotion } : {}),
    ...(recordVideo ? { recordVideo } : {})
  })
  await context.addInitScript(
    ({ t, dark }) => {
      try {
        localStorage.setItem('darkMode', String(dark))
        if (t) {
          const claims = JSON.parse(
            atob(t.split('.')[1].replace(/-/g, '+').replace(/_/g, '/'))
          )
          localStorage.setItem('token', t)
          localStorage.setItem('username', claims.sub)
          localStorage.setItem('role', claims.role)
          if (claims.employeeId)
            localStorage.setItem('employeeId', String(claims.employeeId))
          localStorage.setItem('activeSchoolId', '1')
        }
      } catch {
        // storage blocked: the page shows the login form and the check reports it
      }
    },
    { t, dark }
  )
  return context
}

/** Opens a page and waits until its data has loaded (network quiet, no loading bar). */
export async function visit(page, path) {
  await page.goto(BASE + '/#' + path, { waitUntil: 'networkidle' })
  await page.waitForTimeout(600)
}

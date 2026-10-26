// The public demo's visitor id: a random UUID this browser keeps for a year (localStorage and a
// cookie, so clearing one does not make a returning visitor "new"). Sent with the demo's page
// pings, and with a staff sign-in so the staff member's own demo visits are not counted.

const KEY = 'demoVisitorId'
const REF_KEY = 'demoRef'
const YEAR = 365 * 24 * 3600

function readCookie() {
  const m = document.cookie.match(/(?:^|; )demoVisitorId=([0-9a-f-]{36})/)
  return m ? m[1] : null
}

function uuid() {
  if (globalThis.crypto?.randomUUID) return crypto.randomUUID()
  const b = crypto.getRandomValues(new Uint8Array(16))
  b[6] = (b[6] & 0x0f) | 0x40
  b[8] = (b[8] & 0x3f) | 0x80
  const h = [...b].map(x => x.toString(16).padStart(2, '0')).join('')
  return `${h.slice(0, 8)}-${h.slice(8, 12)}-${h.slice(12, 16)}-${h.slice(16, 20)}-${h.slice(20)}`
}

export function visitorId() {
  let id = null
  try {
    id = localStorage.getItem(KEY)
  } catch {
    // storage blocked: the cookie still works
  }
  if (!id || !/^[0-9a-f-]{36}$/.test(id)) id = readCookie() || uuid()
  try {
    localStorage.setItem(KEY, id)
  } catch {
    // ignore
  }
  document.cookie = `${KEY}=${id}; max-age=${YEAR}; path=/; SameSite=Lax`
  return id
}

/** Where the demo link came from (?ref=linkedin), for this tab's visit. */
export function setDemoRef(ref) {
  try {
    sessionStorage.setItem(REF_KEY, ref || 'direct')
  } catch {
    // ignore
  }
}

export function demoRef() {
  try {
    return sessionStorage.getItem(REF_KEY) || 'direct'
  } catch {
    return 'direct'
  }
}

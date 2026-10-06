import axios from 'axios'
import { LoadingBar } from 'quasar'

const api = axios.create({
  baseURL: import.meta.env.QCLI_API_BASE_URL || 'http://localhost:8080',
  timeout: 15000
})

// Normalizes every rejection into a plain, user-facing Uzbek message so
// callers never have to re-derive "network vs timeout vs server" logic
// themselves; the original error is still attached for callers that need it.
function friendlyMessage(error) {
  if (error.code === 'ECONNABORTED') {
    return "So'rov vaqti tugadi. Internet aloqasini tekshirib, qayta urinib ko'ring."
  }
  if (!error.response) {
    return "Server bilan bog'lanib bo'lmadi. Backend ishlayotganini tekshiring."
  }
  const status = error.response.status
  if (status === 401) {
    return "Login yoki parol noto'g'ri"
  }
  if (status >= 500) {
    return "Serverda xatolik yuz berdi. Birozdan so'ng qayta urinib ko'ring."
  }
  return typeof error.response.data === 'string' && error.response.data
    ? error.response.data
    : 'Xatolik yuz berdi'
}

// One thin bar at the top for every API call (driven here, not by Quasar's XHR hijack).
// Background polls pass { background: true } so the bar does not blink every 30 s.
let inFlight = 0

function barStart(config) {
  if (config.background) return
  config.__bar = true
  if (inFlight++ === 0) LoadingBar.start()
}

function barStop(config) {
  if (!config?.__bar) return
  config.__bar = false
  inFlight = Math.max(0, inFlight - 1)
  if (inFlight === 0) LoadingBar.stop()
}

export default ({ router }) => {
  LoadingBar.setDefaults({
    color: 'primary',
    size: '3px',
    position: 'top',
    // the interceptors below drive the bar; never auto-start on raw XHR
    hijackFilter: () => false
  })

  api.interceptors.request.use(config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    barStart(config)
    return config
  })

  api.interceptors.response.use(
    response => {
      barStop(response.config)
      return response
    },
    error => {
      barStop(error.config)
      error.friendlyMessage = friendlyMessage(error)
      if (error.response?.status === 401) {
        localStorage.removeItem('token')
        localStorage.removeItem('username')
        localStorage.removeItem('role')
        // the account was put on leave while signed in: say why on the login page
        const blocked = error.response.headers?.['x-account-blocked'] === '1'
        if (router.currentRoute.value.path !== '/login') {
          router.push(blocked ? '/login?blocked=1' : '/login')
        }
      }
      return Promise.reject(error)
    }
  )
}

export { api }

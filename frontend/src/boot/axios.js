import axios from 'axios'

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

export default ({ router }) => {
  api.interceptors.request.use(config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  })

  api.interceptors.response.use(
    response => response,
    error => {
      error.friendlyMessage = friendlyMessage(error)
      if (error.response?.status === 401) {
        localStorage.removeItem('token')
        localStorage.removeItem('username')
        localStorage.removeItem('role')
        if (router.currentRoute.value.path !== '/login') {
          router.push('/login')
        }
      }
      return Promise.reject(error)
    }
  )
}

export { api }

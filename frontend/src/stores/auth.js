import { defineStore } from 'pinia'
import { decodeJwt } from '@/utils/jwt'

// a token whose "exp" has passed (the demo visitor's lasts 8 hours) counts as signed out
export function expired(token) {
  const exp = token ? decodeJwt(token)?.exp : null
  return !!exp && exp * 1000 < Date.now()
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') || null,
    username: localStorage.getItem('username') || null,
    role: localStorage.getItem('role') || null,
    employeeId: localStorage.getItem('employeeId')
      ? Number(localStorage.getItem('employeeId'))
      : null
  }),

  getters: {
    isAuthenticated: state => !!state.token && !expired(state.token),
    // the public demo visitor: sees every page an admin sees, changes nothing
    isGuest: state => state.role === 'GUEST',
    isAdmin: state => state.role === 'ADMIN' || state.role === 'GUEST',
    isEditor: state =>
      state.role === 'EDITOR' ||
      state.role === 'ADMIN' ||
      state.role === 'GUEST',
    // a real administrator (not the demo visitor): visit statistics
    isRealAdmin: state => state.role === 'ADMIN'
  },

  actions: {
    setToken(token) {
      const claims = decodeJwt(token)
      this.token = token
      this.username = claims?.sub || null
      this.role = claims?.role || null
      this.employeeId = claims?.employeeId ?? null

      localStorage.setItem('token', token)
      if (this.username) localStorage.setItem('username', this.username)
      if (this.role) localStorage.setItem('role', this.role)
      if (this.employeeId != null) {
        localStorage.setItem('employeeId', String(this.employeeId))
      } else {
        localStorage.removeItem('employeeId')
      }
    },

    logout() {
      this.token = null
      this.username = null
      this.role = null
      this.employeeId = null
      localStorage.removeItem('token')
      localStorage.removeItem('username')
      localStorage.removeItem('role')
      localStorage.removeItem('employeeId')
    }
  }
})

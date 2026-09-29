import { defineStore } from 'pinia'
import { decodeJwt } from '@/utils/jwt'

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
    isAuthenticated: state => !!state.token,
    isAdmin: state => state.role === 'ADMIN',
    isEditor: state => state.role === 'EDITOR' || state.role === 'ADMIN'
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

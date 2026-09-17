import { defineStore } from 'pinia'
import { decodeJwt } from '@/utils/jwt'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') || null,
    username: localStorage.getItem('username') || null,
    role: localStorage.getItem('role') || null,
  }),

  getters: {
    isAuthenticated: (state) => !!state.token,
    isAdmin: (state) => state.role === 'ADMIN',
    isEditor: (state) => state.role === 'EDITOR' || state.role === 'ADMIN',
  },

  actions: {
    setToken(token) {
      const claims = decodeJwt(token)
      this.token = token
      this.username = claims?.sub || null
      this.role = claims?.role || null

      localStorage.setItem('token', token)
      if (this.username) localStorage.setItem('username', this.username)
      if (this.role) localStorage.setItem('role', this.role)
    },

    logout() {
      this.token = null
      this.username = null
      this.role = null
      localStorage.removeItem('token')
      localStorage.removeItem('username')
      localStorage.removeItem('role')
    },
  },
})

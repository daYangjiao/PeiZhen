import { defineStore } from 'pinia'
import { isJwtExpired } from '../utils/admin-auth-session'

const TOKEN_KEY = 'yuanban_admin_token'
const USER_KEY = 'yuanban_admin_user'

export const useAuthStore = defineStore('admin-auth', {
  state: () => ({
    token: '',
    user: null
  }),
  getters: {
    isSuperAdmin: (state) => state.user?.role === 'SUPER_ADMIN'
  },
  actions: {
    restore() {
      if (this.token) return
      const storedToken = window.localStorage.getItem(TOKEN_KEY) || ''
      if (storedToken && isJwtExpired(storedToken)) {
        this.clearSession()
        return
      }
      this.token = storedToken
      const rawUser = window.localStorage.getItem(USER_KEY)
      this.user = rawUser ? JSON.parse(rawUser) : null
    },
    setSession(token, user) {
      this.token = token
      this.user = user
      window.localStorage.setItem(TOKEN_KEY, token)
      window.localStorage.setItem(USER_KEY, JSON.stringify(user))
    },
    clearSession() {
      this.token = ''
      this.user = null
      window.localStorage.removeItem(TOKEN_KEY)
      window.localStorage.removeItem(USER_KEY)
    }
  }
})

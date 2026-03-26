import { defineStore } from 'pinia'

const TOKEN_KEY = 'yuanban_admin_token'
const USER_KEY = 'yuanban_admin_user'

export const useAuthStore = defineStore('admin-auth', {
  state: () => ({
    token: '',
    user: null
  }),
  actions: {
    restore() {
      if (this.token) return
      this.token = window.localStorage.getItem(TOKEN_KEY) || ''
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

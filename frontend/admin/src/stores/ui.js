import { defineStore } from 'pinia'
import { ADMIN_AUTH_REDIRECT_FLAG } from '../utils/admin-auth-session'

let nextToastId = 1

export const useUiStore = defineStore('admin-ui', {
  state: () => ({
    toasts: []
  }),
  actions: {
    toast(message, type = 'info') {
      if (window[ADMIN_AUTH_REDIRECT_FLAG]) return
      const id = nextToastId++
      this.toasts.push({ id, message, type })
      window.setTimeout(() => {
        this.toasts = this.toasts.filter((item) => item.id !== id)
      }, 2600)
    }
  }
})

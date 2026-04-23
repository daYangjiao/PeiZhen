import { defineStore } from 'pinia'

let nextToastId = 1

export const useUiStore = defineStore('admin-ui', {
  state: () => ({
    toasts: []
  }),
  actions: {
    toast(message, type = 'info') {
      const id = nextToastId++
      this.toasts.push({ id, message, type })
      window.setTimeout(() => {
        this.toasts = this.toasts.filter((item) => item.id !== id)
      }, 2600)
    }
  }
})

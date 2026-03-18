import { defineStore } from 'pinia'
import { useMessageStore } from '@/stores/message'

const STORAGE_KEYS = {
  role: 'role',
  token: 'token',
  userInfo: 'userInfo',
  isLoggedIn: 'isLoggedIn'
}

export const useSessionStore = defineStore('session', {
  state: () => ({
    role: uni.getStorageSync(STORAGE_KEYS.role) || '',
    token: uni.getStorageSync(STORAGE_KEYS.token) || '',
    userInfo: uni.getStorageSync(STORAGE_KEYS.userInfo) || null,
    isLoggedIn: !!uni.getStorageSync(STORAGE_KEYS.isLoggedIn)
  }),
  actions: {
    restoreFromStorage() {
      this.role = uni.getStorageSync(STORAGE_KEYS.role) || ''
      this.token = uni.getStorageSync(STORAGE_KEYS.token) || ''
      this.userInfo = uni.getStorageSync(STORAGE_KEYS.userInfo) || null
      this.isLoggedIn = !!uni.getStorageSync(STORAGE_KEYS.isLoggedIn)
    },
    setRole(role) {
      this.role = role
      uni.setStorageSync(STORAGE_KEYS.role, role)
    },
    setSession({ role, token, userInfo }) {
      if (role) this.setRole(role)
      if (token) {
        this.token = token
        uni.setStorageSync(STORAGE_KEYS.token, token)
      }
      if (userInfo) {
        this.userInfo = userInfo
        uni.setStorageSync(STORAGE_KEYS.userInfo, userInfo)
      }
      this.isLoggedIn = true
      uni.setStorageSync(STORAGE_KEYS.isLoggedIn, true)
      uni.$emit('session:changed')
    },
    logout() {
      const messageStore = useMessageStore()
      this.token = ''
      this.userInfo = null
      this.isLoggedIn = false
      this.role = ''
      uni.removeStorageSync(STORAGE_KEYS.token)
      uni.removeStorageSync(STORAGE_KEYS.userInfo)
      uni.removeStorageSync(STORAGE_KEYS.isLoggedIn)
      uni.removeStorageSync(STORAGE_KEYS.role)
      messageStore.clearAllUnread()
      messageStore.updateTabBarBadge()
      uni.$emit('session:changed')
    }
  }
})

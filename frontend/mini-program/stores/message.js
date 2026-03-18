import { defineStore } from 'pinia'
import { get } from '@/utils/api.js'

const TABBAR_MESSAGE_BADGE_KEY = 'tabbar_message_badge'

export const useMessageStore = defineStore('message', {
  state: () => ({
    unreadTotal: 0,
    systemUnreadCount: 0,
    contactUnreadMap: {},
    contactIdsMarkedRead: new Set(),
    lastUpdateTime: null,
    loading: false,
    refreshTimer: null
  }),
  getters: {
    totalUnreadCount: (state) => state.systemUnreadCount + state.unreadTotal,
    getContactUnreadCount: (state) => (contactId) => state.contactUnreadMap[contactId] || 0,
    hasUnreadMessages: (state) => (state.systemUnreadCount + state.unreadTotal) > 0
  },
  actions: {
    async initMessageStatus() {
      await this.refreshUnreadCounts()
    },
    scheduleRefreshUnreadCounts(delay = 200) {
      if (this.refreshTimer) clearTimeout(this.refreshTimer)
      this.refreshTimer = setTimeout(() => {
        this.refreshTimer = null
        this.refreshUnreadCounts()
      }, delay)
    },
    async refreshUnreadCounts() {
      if (this.loading) return
      this.loading = true
      try {
        const userInfo = uni.getStorageSync('userInfo') || {}
        const currentUserId = Number(userInfo.id || userInfo.userId || 0)
        const token = uni.getStorageSync('token')
        const isLoggedIn = !!uni.getStorageSync('isLoggedIn')
        if (!currentUserId || !token || !isLoggedIn) {
          this.clearAllUnread()
          this.updateTabBarBadge()
          return
        }

        const res = await get('/api/chat/contacts')
        if (res.code === 200) {
          const allContacts = res.data || []
          let totalUnread = 0
          let systemUnread = 0
          const contactUnreadMap = {}
          allContacts.forEach(contact => {
            if (contact.senderId === 0 || contact.receiverId === 0) {
              systemUnread = contact.unreadCount || 0
            } else {
              const contactId = Number(contact.senderId) === currentUserId
                ? Number(contact.receiverId)
                : Number(contact.senderId)
              if (!contactId) return
              const count = this.contactIdsMarkedRead.has(contactId) ? 0 : (contact.unreadCount || 0)
              contactUnreadMap[contactId] = count
              totalUnread += count
              if (count === 0) this.contactIdsMarkedRead.delete(contactId)
            }
          })
          this.unreadTotal = totalUnread
          this.systemUnreadCount = systemUnread
          this.contactUnreadMap = contactUnreadMap
          this.lastUpdateTime = Date.now()
          this.updateTabBarBadge()
        }
      } finally {
        this.loading = false
      }
    },
    incrementUnread(contactId, count = 1) {
      if (!contactId || contactId <= 0) return
      const currentCount = this.contactUnreadMap[contactId] || 0
      this.contactUnreadMap[contactId] = currentCount + count
      this.recalculateTotal()
    },
    decrementUnread(contactId, count = 1) {
      const currentCount = this.contactUnreadMap[contactId] || 0
      this.contactUnreadMap[contactId] = Math.max(0, currentCount - count)
      this.recalculateTotal()
    },
    incrementSystemUnread(count = 1) {
      this.systemUnreadCount += count
    },
    recalculateTotal() {
      this.unreadTotal = Object.values(this.contactUnreadMap).reduce((sum, c) => sum + c, 0)
    },
    clearAllUnread() {
      this.unreadTotal = 0
      this.systemUnreadCount = 0
      this.contactUnreadMap = {}
      this.contactIdsMarkedRead.clear()
      this.lastUpdateTime = null
      if (this.refreshTimer) {
        clearTimeout(this.refreshTimer)
        this.refreshTimer = null
      }
    },
    resetContactUnread(contactId) {
      if (!contactId) return
      this.contactIdsMarkedRead.add(contactId)
      this.contactUnreadMap[contactId] = 0
      this.recalculateTotal()
    },
    resetSystemUnread() {
      this.systemUnreadCount = 0
    },
    updateContactUnread(contactId, count) {
      if (!contactId && contactId !== 0) return
      if (this.contactIdsMarkedRead.has(contactId)) {
        this.contactUnreadMap[contactId] = 0
        if (Number(count) === 0) this.contactIdsMarkedRead.delete(contactId)
      } else {
        this.contactUnreadMap[contactId] = Number(count) || 0
      }
      this.recalculateTotal()
    },
    updateTabBarBadge() {
      const total = this.systemUnreadCount + this.unreadTotal
      try {
        uni.setStorageSync(TABBAR_MESSAGE_BADGE_KEY, total)
        const role = uni.getStorageSync('role')
        if (role && role !== 'user') return
        if (total > 0) {
          uni.setTabBarBadge({ index: 3, text: total > 99 ? '99+' : String(total) })
        } else {
          uni.removeTabBarBadge({ index: 3 })
        }
      } catch (e) {}
    }
  }
})

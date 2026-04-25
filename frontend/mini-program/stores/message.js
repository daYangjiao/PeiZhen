import { defineStore } from 'pinia'
import { get } from '@/utils/api.js'
import {
  MESSAGE_BADGE_STORAGE_KEY,
  MESSAGE_BADGE_UPDATED_EVENT,
  SYSTEM_MESSAGE_READ_EVENT,
  USER_MESSAGE_TAB_INDEX,
  formatBadgeText,
  normalizeBadgeCount,
  resolveRealtimeUnreadTarget
} from '@/utils/message-badge.mjs'

export const useMessageStore = defineStore('message', {
  state: () => ({
    unreadTotal: 0,
    systemUnreadCount: 0,
    contactUnreadMap: {},
    lastUpdateTime: null,
    loading: false,
    refreshTimer: null,
    pendingRefresh: false
  }),
  getters: {
    totalUnreadCount: (state) => normalizeBadgeCount(state.systemUnreadCount + state.unreadTotal),
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
      if (this.loading) {
        this.pendingRefresh = true
        return
      }
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
              const count = normalizeBadgeCount(contact.unreadCount)
              contactUnreadMap[contactId] = count
              totalUnread += count
            }
          })
          this.unreadTotal = normalizeBadgeCount(totalUnread)
          this.systemUnreadCount = normalizeBadgeCount(systemUnread)
          this.contactUnreadMap = contactUnreadMap
          this.lastUpdateTime = Date.now()
          this.updateTabBarBadge()
        }
      } catch (error) {
        console.error('刷新未读数失败:', error)
      } finally {
        this.loading = false
        if (this.pendingRefresh) {
          this.pendingRefresh = false
          this.scheduleRefreshUnreadCounts(80)
        }
      }
    },
    applyRealtimeUnreadFromMessage(message) {
      const userInfo = uni.getStorageSync('userInfo') || {}
      const target = resolveRealtimeUnreadTarget(message, {
        currentUserId: Number(userInfo.id || userInfo.userId || 0),
        token: uni.getStorageSync('token'),
        isLoggedIn: !!uni.getStorageSync('isLoggedIn')
      })
      if (!target) return

      if (target.type === 'system') {
        this.incrementSystemUnread(1)
        this.updateTabBarBadge()
        return
      }

      this.incrementUnread(target.contactId, 1)
      this.updateTabBarBadge()
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
      this.systemUnreadCount = normalizeBadgeCount(this.systemUnreadCount + count)
    },
    recalculateTotal() {
      this.unreadTotal = Object.values(this.contactUnreadMap).reduce((sum, c) => sum + normalizeBadgeCount(c), 0)
    },
    clearAllUnread() {
      this.unreadTotal = 0
      this.systemUnreadCount = 0
      this.contactUnreadMap = {}
      this.lastUpdateTime = null
      this.pendingRefresh = false
      if (this.refreshTimer) {
        clearTimeout(this.refreshTimer)
        this.refreshTimer = null
      }
    },
    setSystemUnreadCount(count) {
      this.systemUnreadCount = normalizeBadgeCount(count)
      this.updateTabBarBadge()
    },
    resetSystemUnread() {
      this.setSystemUnreadCount(0)
    },
    clearSystemUnread(options = {}) {
      this.systemUnreadCount = 0
      this.updateTabBarBadge()
      if (options.emitReadEvent !== false) {
        try {
          uni.$emit(SYSTEM_MESSAGE_READ_EVENT, { messageId: 0 })
        } catch (e) {}
      }
    },
    updateContactUnread(contactId, count) {
      if (!contactId && contactId !== 0) return
      this.contactUnreadMap[contactId] = normalizeBadgeCount(count)
      this.recalculateTotal()
    },
    updateTabBarBadge() {
      const total = normalizeBadgeCount(this.systemUnreadCount + this.unreadTotal)
      try {
        uni.setStorageSync(MESSAGE_BADGE_STORAGE_KEY, total)
        uni.$emit(MESSAGE_BADGE_UPDATED_EVENT, total)
      } catch (e) {}
      try {
        if (typeof uni.setTabBarBadge !== 'function' || typeof uni.removeTabBarBadge !== 'function') return
        if (total > 0) {
          uni.setTabBarBadge({
            index: USER_MESSAGE_TAB_INDEX,
            text: formatBadgeText(total)
          })
        } else {
          uni.removeTabBarBadge({ index: USER_MESSAGE_TAB_INDEX })
        }
      } catch (e) {}
    }
  }
})

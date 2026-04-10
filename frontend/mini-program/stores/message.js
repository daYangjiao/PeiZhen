import { defineStore } from 'pinia'
import { get } from '@/utils/api.js'

const TABBAR_MESSAGE_BADGE_KEY = 'tabbar_message_badge'
const TABBAR_BADGE_UPDATED_EVENT = 'message:badge-updated'
const isReadReceiptMessage = (message = {}, payload = {}) => {
  const type = String(message.type || payload.type || '')
  const msgType = Number(message.msgType || payload.msgType || 0)
  return type === 'READ_RECEIPT' || msgType === 99
}

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
              const count = contact.unreadCount || 0
              contactUnreadMap[contactId] = count
              totalUnread += count
            }
          })
          this.unreadTotal = totalUnread
          this.systemUnreadCount = systemUnread
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
      if (!message) return

      let payload = {}
      if (message.data && typeof message.data === 'object') {
        payload = message.data
      } else if (typeof message.data === 'string') {
        try {
          payload = JSON.parse(message.data)
        } catch (e) {
          payload = {}
        }
      }

      const msgType = Number(message.msgType || payload.msgType || 0)
      if (isReadReceiptMessage(message, payload)) return

      const userInfo = uni.getStorageSync('userInfo') || {}
      const currentUserId = Number(userInfo.id || userInfo.userId || 0)
      const token = uni.getStorageSync('token')
      const isLoggedIn = !!uni.getStorageSync('isLoggedIn')
      if (!currentUserId || !token || !isLoggedIn) return

      const senderId = Number(message.senderId || payload.senderId || 0)
      const receiverId = Number(message.receiverId || payload.receiverId || 0)
      const isNormalChatMsg = [1, 2, 3, 4].includes(msgType)

      // 自己发出的消息不计未读
      if (senderId && senderId === currentUserId) return

      // 系统消息（sender/receiver 包含 0）计入系统未读
      if (senderId === 0 || receiverId === 0) {
        this.incrementSystemUnread(1)
        this.updateTabBarBadge()
        return
      }

      if (!isNormalChatMsg) return

      // 普通会话消息计入对应联系人
      const contactId = senderId && senderId !== currentUserId
        ? senderId
        : (receiverId && receiverId !== currentUserId ? receiverId : 0)

      if (!contactId) return

      this.incrementUnread(contactId, 1)
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
      this.systemUnreadCount += count
    },
    recalculateTotal() {
      this.unreadTotal = Object.values(this.contactUnreadMap).reduce((sum, c) => sum + c, 0)
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
    resetSystemUnread() {
      this.systemUnreadCount = 0
    },
    updateContactUnread(contactId, count) {
      if (!contactId && contactId !== 0) return
      this.contactUnreadMap[contactId] = Number(count) || 0
      this.recalculateTotal()
    },
    updateTabBarBadge() {
      const total = this.systemUnreadCount + this.unreadTotal
      try {
        uni.setStorageSync(TABBAR_MESSAGE_BADGE_KEY, total)
        uni.$emit(TABBAR_BADGE_UPDATED_EVENT, total)
      } catch (e) {}
    }
  }
})

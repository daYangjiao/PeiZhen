import { defineStore } from 'pinia'
import { get } from '@/utils/api.js'

// 消息状态管理
export const useMessageStore = defineStore('message', {
  state: () => ({
    // 未读消息总数
    unreadTotal: 0,
    // 系统未读消息数
    systemUnreadCount: 0,
    // 联系人未读消息映射 {contactId: count}
    contactUnreadMap: {},
    // 上次更新时间
    lastUpdateTime: null,
    // 是否正在加载
    loading: false
  }),

  getters: {
    // 获取总未读数（包括系统消息和其他消息）
    totalUnreadCount: (state) => {
      return state.systemUnreadCount + state.unreadTotal
    },
    
    // 获取指定联系人的未读数
    getContactUnreadCount: (state) => (contactId) => {
      return state.contactUnreadMap[contactId] || 0
    },
    
    // 检查是否有未读消息
    hasUnreadMessages: (state) => {
      return state.totalUnreadCount > 0
    }
  },

  actions: {
    // 初始化消息状态
    async initMessageStatus() {
      await this.refreshUnreadCounts()
    },

    // 刷新所有未读消息计数
    async refreshUnreadCounts() {
      if (this.loading) return
      
      this.loading = true
      try {
        const res = await get('/api/chat/contacts')
        if (res.code === 200) {
          const allContacts = res.data
          
          // 计算总未读数
          let totalUnread = 0
          let systemUnread = 0
          const contactUnreadMap = {}

          allContacts.forEach(contact => {
            if (contact.senderId === 0 || contact.receiverId === 0) {
              // 系统消息
              systemUnread = contact.unreadCount || 0
            } else {
              // 普通联系人消息
              totalUnread += contact.unreadCount || 0
              // 存储每个联系人的未读数
              const contactId = contact.senderId === 0 ? contact.receiverId : contact.senderId
              contactUnreadMap[contactId] = contact.unreadCount || 0
            }
          })

          this.unreadTotal = totalUnread
          this.systemUnreadCount = systemUnread
          this.contactUnreadMap = contactUnreadMap
          this.lastUpdateTime = Date.now()
          
          // 更新tabBar红点
          this.updateTabBarBadge()
        }
      } catch (error) {
        console.error('刷新未读消息计数失败:', error)
      } finally {
        this.loading = false
      }
    },

    // 更新特定联系人的未读数
    updateContactUnread(contactId, unreadCount) {
      this.contactUnreadMap[contactId] = unreadCount
      this.recalculateTotal()
      this.updateTabBarBadge()
    },

    // 增加未读消息数
    incrementUnread(contactId, count = 1) {
      // 验证contactId是否合理
      if (!contactId || contactId <= 0) {
        return
      }
      
      const currentCount = this.contactUnreadMap[contactId] || 0
      const newCount = currentCount + count
      this.contactUnreadMap[contactId] = newCount
      this.recalculateTotal()
      this.updateTabBarBadge()
    },

    // 减少未读消息数（标记为已读）
    decrementUnread(contactId, count = 1) {
      const currentCount = this.contactUnreadMap[contactId] || 0
      this.contactUnreadMap[contactId] = Math.max(0, currentCount - count)
      this.recalculateTotal()
      this.updateTabBarBadge()
    },

    // 重置特定联系人的未读数为0
    resetContactUnread(contactId) {
      this.contactUnreadMap[contactId] = 0
      this.recalculateTotal()
      this.updateTabBarBadge()
    },

    // 增加系统未读消息数
    incrementSystemUnread(count = 1) {
      this.systemUnreadCount += count
      this.updateTabBarBadge()
    },
    
    // 重置系统未读消息数
    resetSystemUnread() {
      this.systemUnreadCount = 0
      this.updateTabBarBadge()
    },

    // 重新计算总未读数
    recalculateTotal() {
      const contactUnreadSum = Object.values(this.contactUnreadMap).reduce((sum, count) => sum + count, 0)
      this.unreadTotal = contactUnreadSum
    },

    // 更新tabBar徽章
    updateTabBarBadge() {
      const total = this.totalUnreadCount
      
      if (total > 0) {
        // 显示红点数字，最多显示99+
        const badgeText = total > 99 ? '99+' : total.toString()
        uni.setTabBarBadge({
          index: 3, // 消息页面在tabBar中的索引
          text: badgeText
        }).catch(() => {})
      } else {
        // 清除徽章
        uni.removeTabBarBadge({
          index: 3
        }).catch(() => {})
      }
    },
    
    // 强制刷新tabBar显示
    forceRefreshTabBar() {
      // 先清除再重新设置
      uni.removeTabBarBadge({
        index: 3
      }).finally(() => {
        this.updateTabBarBadge()
      })
    },

    // 清除所有未读状态
    clearAllUnread() {
      this.unreadTotal = 0
      this.systemUnreadCount = 0
      this.contactUnreadMap = {}
      this.lastUpdateTime = null
      this.updateTabBarBadge()
    },

    // 从本地存储恢复状态
    restoreFromStorage() {
      try {
        const storedData = uni.getStorageSync('messageStore')
        if (storedData) {
          Object.assign(this.$state, storedData)
          this.updateTabBarBadge()
        }
      } catch (error) {
        console.error('恢复消息状态失败:', error)
      }
    },

    // 持久化存储状态
    saveToStorage() {
      try {
        uni.setStorageSync('messageStore', {
          unreadTotal: this.unreadTotal,
          systemUnreadCount: this.systemUnreadCount,
          contactUnreadMap: this.contactUnreadMap,
          lastUpdateTime: this.lastUpdateTime
        })
      } catch (error) {
        console.error('保存消息状态失败:', error)
      }
    }
  }
})
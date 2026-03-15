"use strict";
const common_vendor = require("../common/vendor.js");
const utils_api = require("../utils/api.js");
const useMessageStore = common_vendor.defineStore("message", {
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
      return state.systemUnreadCount + state.unreadTotal;
    },
    // 获取指定联系人的未读数
    getContactUnreadCount: (state) => (contactId) => {
      return state.contactUnreadMap[contactId] || 0;
    },
    // 检查是否有未读消息
    hasUnreadMessages: (state) => {
      return state.totalUnreadCount > 0;
    }
  },
  actions: {
    // 初始化消息状态
    async initMessageStatus() {
      await this.refreshUnreadCounts();
    },
    // 刷新所有未读消息计数
    async refreshUnreadCounts() {
      if (this.loading)
        return;
      this.loading = true;
      try {
        const res = await utils_api.get("/api/chat/contacts");
        if (res.code === 200) {
          const allContacts = res.data;
          let totalUnread = 0;
          let systemUnread = 0;
          const contactUnreadMap = {};
          allContacts.forEach((contact) => {
            if (contact.senderId === 0 || contact.receiverId === 0) {
              systemUnread = contact.unreadCount || 0;
            } else {
              totalUnread += contact.unreadCount || 0;
              const contactId = contact.senderId === 0 ? contact.receiverId : contact.senderId;
              contactUnreadMap[contactId] = contact.unreadCount || 0;
            }
          });
          this.unreadTotal = totalUnread;
          this.systemUnreadCount = systemUnread;
          this.contactUnreadMap = contactUnreadMap;
          this.lastUpdateTime = Date.now();
          this.updateTabBarBadge();
        }
      } catch (error) {
        common_vendor.index.__f__("error", "at stores/message.js:79", "刷新未读消息计数失败:", error);
      } finally {
        this.loading = false;
      }
    },
    // 更新特定联系人的未读数
    updateContactUnread(contactId, unreadCount) {
      this.contactUnreadMap[contactId] = unreadCount;
      this.recalculateTotal();
      this.updateTabBarBadge();
    },
    // 增加未读消息数
    incrementUnread(contactId, count = 1) {
      if (!contactId || contactId <= 0) {
        return;
      }
      const currentCount = this.contactUnreadMap[contactId] || 0;
      const newCount = currentCount + count;
      this.contactUnreadMap[contactId] = newCount;
      this.recalculateTotal();
      this.updateTabBarBadge();
    },
    // 减少未读消息数（标记为已读）
    decrementUnread(contactId, count = 1) {
      const currentCount = this.contactUnreadMap[contactId] || 0;
      this.contactUnreadMap[contactId] = Math.max(0, currentCount - count);
      this.recalculateTotal();
      this.updateTabBarBadge();
    },
    // 重置特定联系人的未读数为0
    resetContactUnread(contactId) {
      this.contactUnreadMap[contactId] = 0;
      this.recalculateTotal();
      this.updateTabBarBadge();
    },
    // 增加系统未读消息数
    incrementSystemUnread(count = 1) {
      this.systemUnreadCount += count;
      this.updateTabBarBadge();
    },
    // 重置系统未读消息数
    resetSystemUnread() {
      this.systemUnreadCount = 0;
      this.updateTabBarBadge();
    },
    // 重新计算总未读数
    recalculateTotal() {
      const contactUnreadSum = Object.values(this.contactUnreadMap).reduce((sum, count) => sum + count, 0);
      this.unreadTotal = contactUnreadSum;
    },
    // 更新tabBar徽章
    updateTabBarBadge() {
      const total = this.totalUnreadCount;
      if (total > 0) {
        const badgeText = total > 99 ? "99+" : total.toString();
        common_vendor.index.setTabBarBadge({
          index: 3,
          // 消息页面在tabBar中的索引
          text: badgeText
        }).catch(() => {
        });
      } else {
        common_vendor.index.removeTabBarBadge({
          index: 3
        }).catch(() => {
        });
      }
    },
    // 强制刷新tabBar显示
    forceRefreshTabBar() {
      common_vendor.index.removeTabBarBadge({
        index: 3
      }).finally(() => {
        this.updateTabBarBadge();
      });
    },
    // 清除所有未读状态
    clearAllUnread() {
      this.unreadTotal = 0;
      this.systemUnreadCount = 0;
      this.contactUnreadMap = {};
      this.lastUpdateTime = null;
      this.updateTabBarBadge();
    },
    // 从本地存储恢复状态
    restoreFromStorage() {
      try {
        const storedData = common_vendor.index.getStorageSync("messageStore");
        if (storedData) {
          Object.assign(this.$state, storedData);
          this.updateTabBarBadge();
        }
      } catch (error) {
        common_vendor.index.__f__("error", "at stores/message.js:186", "恢复消息状态失败:", error);
      }
    },
    // 持久化存储状态
    saveToStorage() {
      try {
        common_vendor.index.setStorageSync("messageStore", {
          unreadTotal: this.unreadTotal,
          systemUnreadCount: this.systemUnreadCount,
          contactUnreadMap: this.contactUnreadMap,
          lastUpdateTime: this.lastUpdateTime
        });
      } catch (error) {
        common_vendor.index.__f__("error", "at stores/message.js:200", "保存消息状态失败:", error);
      }
    }
  }
});
exports.useMessageStore = useMessageStore;
//# sourceMappingURL=../../.sourcemap/mp-weixin/stores/message.js.map

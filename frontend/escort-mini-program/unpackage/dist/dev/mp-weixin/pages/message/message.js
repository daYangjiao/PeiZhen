"use strict";
const common_vendor = require("../../common/vendor.js");
const common_assets = require("../../common/assets.js");
const utils_api = require("../../utils/api.js");
const stores_message = require("../../stores/message.js");
const utils_chatWebsocket = require("../../utils/chat-websocket.js");
const _sfc_main = {
  __name: "message",
  setup(__props) {
    const contacts = common_vendor.ref([]);
    const lastSystemMsg = common_vendor.ref({});
    const messageStore = stores_message.useMessageStore();
    const systemUnreadCount = common_vendor.computed(() => messageStore.systemUnreadCount);
    common_vendor.onMounted(async () => {
      common_vendor.index.__f__("log", "at pages/message/message.vue:83", "消息页面 mounted");
      await messageStore.initMessageStatus();
      common_vendor.index.__f__("log", "at pages/message/message.vue:87", "初始化后store状态:", {
        systemUnreadCount: messageStore.systemUnreadCount,
        unreadTotal: messageStore.unreadTotal,
        totalUnreadCount: messageStore.totalUnreadCount,
        contactUnreadMap: messageStore.contactUnreadMap
      });
      await loadContacts();
      utils_chatWebsocket.connectChatSocket();
      utils_chatWebsocket.addChatListener(handleNewMessage);
      common_vendor.index.$on("chat:return", handleChatReturn);
      common_vendor.index.__f__("log", "at pages/message/message.vue:104", "加载完成后最终状态:", {
        systemUnreadCount: messageStore.systemUnreadCount,
        unreadTotal: messageStore.unreadTotal,
        totalUnreadCount: messageStore.totalUnreadCount,
        contactUnreadMap: messageStore.contactUnreadMap
      });
    });
    common_vendor.onUnmounted(() => {
      utils_chatWebsocket.removeChatListener(handleNewMessage);
      common_vendor.index.$off("chat:return", handleChatReturn);
    });
    const processedMessages = /* @__PURE__ */ new Set();
    const handleNewMessage = (msg) => {
      if (!msg)
        return;
      const msgKey = msg.id || `${msg.senderId}-${msg.receiverId}-${msg.createTime}`;
      if (processedMessages.has(msgKey))
        return;
      processedMessages.add(msgKey);
      if (processedMessages.size > 100) {
        const arr = Array.from(processedMessages).slice(-50);
        processedMessages.clear();
        arr.forEach((k) => processedMessages.add(k));
      }
      if ((msg.senderId === 0 || msg.senderId && msg.receiverId) && !(msg.msgType == 3 || msg.content === "READ_RECEIPT")) {
        loadContacts();
      }
    };
    const loadContacts = async () => {
      try {
        common_vendor.index.__f__("log", "at pages/message/message.vue:138", "开始加载联系人...");
        const res = await utils_api.get("/api/chat/contacts");
        common_vendor.index.__f__("log", "at pages/message/message.vue:140", "API响应:", res);
        if (res.code === 200) {
          const allContacts = res.data;
          common_vendor.index.__f__("log", "at pages/message/message.vue:144", "获取到的联系人列表:", allContacts);
          const sysMsg = allContacts.find((c) => c.senderId === 0 || c.receiverId === 0);
          const normalContacts = allContacts.filter((c) => c.senderId !== 0 && c.receiverId !== 0);
          common_vendor.index.__f__("log", "at pages/message/message.vue:150", "系统消息:", sysMsg);
          common_vendor.index.__f__("log", "at pages/message/message.vue:151", "普通联系人:", normalContacts);
          if (sysMsg) {
            lastSystemMsg.value = sysMsg;
            const systemUnread = sysMsg.unreadCount || 0;
            messageStore.systemUnreadCount = systemUnread;
            common_vendor.index.__f__("log", "at pages/message/message.vue:158", "设置系统未读数:", systemUnread, "原始数据:", sysMsg);
          } else {
            lastSystemMsg.value = {};
            messageStore.systemUnreadCount = 0;
            common_vendor.index.__f__("log", "at pages/message/message.vue:162", "未找到系统消息");
          }
          contacts.value = normalContacts;
          updateContactUnreadMap(normalContacts);
          messageStore.updateTabBarBadge();
          common_vendor.index.__f__("log", "at pages/message/message.vue:172", "更新tabBar，当前总未读数:", messageStore.totalUnreadCount);
        }
      } catch (e) {
        common_vendor.index.__f__("error", "at pages/message/message.vue:175", "加载联系人失败", e);
        common_vendor.index.showToast({
          title: "加载消息失败",
          icon: "none"
        });
      }
    };
    const updateContactUnreadMap = (contactList) => {
      common_vendor.index.__f__("log", "at pages/message/message.vue:185", "更新联系人未读映射，联系人列表:", contactList);
      contactList.forEach((contact) => {
        const currentUserId = common_vendor.index.getStorageSync("userInfo").id;
        const contactId = contact.senderId === currentUserId ? contact.receiverId : contact.senderId;
        common_vendor.index.__f__("log", "at pages/message/message.vue:191", `联系人映射 - 当前用户:${currentUserId}, 发送者:${contact.senderId}, 接收者:${contact.receiverId}, 计算出的联系人ID:${contactId}`);
        messageStore.updateContactUnread(contactId, contact.unreadCount || 0);
      });
    };
    const getContactUnreadCount = (contact) => {
      const currentUserId = common_vendor.index.getStorageSync("userInfo").id;
      const contactId = contact.senderId === currentUserId ? contact.receiverId : contact.senderId;
      const unreadCount = messageStore.getContactUnreadCount(contactId);
      common_vendor.index.__f__("log", "at pages/message/message.vue:203", "计算联系人未读数:", {
        contactId,
        senderId: contact.senderId,
        receiverId: contact.receiverId,
        currentUserId,
        storeUnread: unreadCount,
        contactInfo: contact
      });
      return unreadCount;
    };
    const openSystemChat = async () => {
      try {
        await utils_api.post("/api/chat/read?senderId=0");
      } catch (e) {
        common_vendor.index.__f__("error", "at pages/message/message.vue:219", "标记系统消息已读失败", e);
      }
      messageStore.resetSystemUnread();
      messageStore.updateTabBarBadge();
      common_vendor.index.navigateTo({
        url: `/subpkg/system-message/system-message`
      });
    };
    const openChat = async (contact) => {
      const currentUserId = common_vendor.index.getStorageSync("userInfo").id;
      let targetId;
      if (contact.senderId === currentUserId) {
        targetId = contact.receiverId;
      } else {
        targetId = contact.senderId;
      }
      if (!targetId)
        return;
      const targetName = contact.senderName === "我" ? "用户" : contact.senderName || "用户";
      try {
        await utils_api.post(`/api/chat/read?senderId=${targetId}`);
        common_vendor.index.__f__("log", "at pages/message/message.vue:246", "已调用后端标记消息为已读，senderId:", targetId);
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/message/message.vue:248", "标记消息为已读失败:", error);
      }
      messageStore.resetContactUnread(targetId);
      updateContactUnreadMap(contacts.value);
      messageStore.updateTabBarBadge();
      common_vendor.index.navigateTo({
        url: `/subpkg/chat/chat?userId=${targetId}&name=${targetName}`
      });
    };
    const getAvatarUrl = (url) => {
      if (!url)
        return "/static/user-placeholder.png";
      if (url.startsWith("http") || url.startsWith("https")) {
        return url;
      }
      let baseUrl = utils_api.config.baseURL;
      if (baseUrl.endsWith("/")) {
        baseUrl = baseUrl.slice(0, -1);
      }
      let path = url;
      if (!path.startsWith("/")) {
        path = "/" + path;
      }
      return baseUrl + path;
    };
    const handleImageError = (e) => {
      common_vendor.index.__f__("error", "at pages/message/message.vue:284", "头像加载失败:", e.detail.errMsg);
    };
    const handleChatReturn = (data) => {
      common_vendor.index.__f__("log", "at pages/message/message.vue:289", "收到聊天页面返回事件:", data);
      if (data && data.targetUserId) {
        messageStore.resetContactUnread(data.targetUserId);
        updateContactUnreadMap(contacts.value);
        messageStore.updateTabBarBadge();
        loadContacts();
      }
    };
    const formatTime = (timeStr) => {
      if (!timeStr)
        return "";
      const date = new Date(timeStr);
      const now = /* @__PURE__ */ new Date();
      if (date.toDateString() === now.toDateString()) {
        return `${String(date.getHours()).padStart(2, "0")}:${String(date.getMinutes()).padStart(2, "0")}`;
      }
      if (date.getFullYear() === now.getFullYear()) {
        return `${date.getMonth() + 1}/${date.getDate()}`;
      }
      return `${date.getFullYear()}/${date.getMonth() + 1}/${date.getDate()}`;
    };
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_assets._imports_0$2,
        b: systemUnreadCount.value > 0
      }, systemUnreadCount.value > 0 ? {
        c: common_vendor.t(systemUnreadCount.value > 99 ? "99+" : systemUnreadCount.value)
      } : {}, {
        d: common_vendor.t(formatTime(lastSystemMsg.value.createTime)),
        e: common_vendor.t(lastSystemMsg.value.content || "暂无新通知"),
        f: common_vendor.o(openSystemChat),
        g: common_vendor.f(contacts.value, (contact, k0, i0) => {
          return common_vendor.e({
            a: getAvatarUrl(contact.senderAvatar),
            b: common_vendor.o(handleImageError, contact.id),
            c: getContactUnreadCount(contact) > 0
          }, getContactUnreadCount(contact) > 0 ? {
            d: common_vendor.t(getContactUnreadCount(contact) > 99 ? "99+" : getContactUnreadCount(contact))
          } : {}, {
            e: common_vendor.t(contact.senderName || "未知用户"),
            f: common_vendor.t(formatTime(contact.createTime)),
            g: common_vendor.t(contact.msgType === 2 ? "[图片]" : contact.content),
            h: contact.id,
            i: common_vendor.o(($event) => openChat(contact), contact.id)
          });
        }),
        h: contacts.value.length === 0 && !lastSystemMsg.value.content
      }, contacts.value.length === 0 && !lastSystemMsg.value.content ? {
        i: common_assets._imports_0$1
      } : {});
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-4c1b26cf"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/message/message.js.map

"use strict";
Object.defineProperty(exports, Symbol.toStringTag, { value: "Module" });
const common_vendor = require("./common/vendor.js");
const utils_chatWebsocket = require("./utils/chat-websocket.js");
const utils_tokenValidator = require("./utils/token-validator.js");
const stores_message = require("./stores/message.js");
if (!Math) {
  "./pages/index/index.js";
  "./pages/order/order.js";
  "./pages/message/message.js";
  "./pages/profile/profile.js";
  "./subpkg/auth/login.js";
  "./subpkg/auth/register.js";
  "./subpkg/order/detail.js";
  "./subpkg/order/prepare.js";
  "./subpkg/order/submit-time-fee.js";
  "./subpkg/chat/chat.js";
  "./subpkg/system-message/system-message.js";
}
function setupGlobalChatListener() {
  utils_chatWebsocket.addChatListener((msg) => {
    if (!msg)
      return;
    const userInfo = common_vendor.index.getStorageSync("userInfo");
    const currentUserId = userInfo == null ? void 0 : userInfo.id;
    if (!currentUserId)
      return;
    const isReadReceipt = (msg.type === "READ_RECEIPT" || msg.content === "READ_RECEIPT" || msg.msgType == 3) && msg.senderId && msg.receiverId;
    if (isReadReceipt) {
      if (msg.receiverId === currentUserId) {
        stores_message.useMessageStore().decrementUnread(msg.senderId);
      }
      return;
    }
    if (msg.senderId === 0 && msg.receiverId === currentUserId) {
      stores_message.useMessageStore().incrementSystemUnread();
      return;
    }
    if (msg.senderId && msg.receiverId) {
      const contactId = msg.senderId === currentUserId ? msg.receiverId : msg.senderId;
      if (contactId && contactId > 0) {
        stores_message.useMessageStore().incrementUnread(contactId);
      }
    }
  });
}
const _sfc_main = {
  onLaunch: async function() {
    common_vendor.index.__f__("log", "at App.vue:35", "陪诊师端App Launch");
    try {
      await utils_tokenValidator.startupTokenCheck();
    } catch (error) {
      common_vendor.index.__f__("error", "at App.vue:40", "启动时token检测失败:", error);
    }
    const messageStore = stores_message.useMessageStore();
    await messageStore.initMessageStatus();
    utils_chatWebsocket.connectChatSocket();
    setupGlobalChatListener();
    utils_tokenValidator.startPeriodicTokenCheck(30 * 60 * 1e3);
  },
  onShow: async function() {
    common_vendor.index.__f__("log", "at App.vue:53", "陪诊师端App Show");
    utils_chatWebsocket.connectChatSocket();
    try {
      const messageStore = stores_message.useMessageStore();
      await messageStore.refreshUnreadCounts();
    } catch (e) {
      common_vendor.index.__f__("error", "at App.vue:59", "刷新未读消息失败", e);
    }
  },
  onHide: function() {
    common_vendor.index.__f__("log", "at App.vue:63", "陪诊师端App Hide");
  }
};
function createApp() {
  const app = common_vendor.createSSRApp(_sfc_main);
  const pinia = common_vendor.createPinia();
  app.use(pinia);
  return {
    app
  };
}
createApp().app.mount("#app");
exports.createApp = createApp;
//# sourceMappingURL=../.sourcemap/mp-weixin/app.js.map

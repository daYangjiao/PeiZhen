"use strict";
Object.defineProperty(exports, Symbol.toStringTag, { value: "Module" });
const common_vendor = require("./common/vendor.js");
const utils_chatWebsocket = require("./utils/chat-websocket.js");
const utils_websocket = require("./utils/websocket.js");
const utils_tokenValidator = require("./utils/token-validator.js");
const stores_message = require("./stores/message.js");
if (!Math) {
  "./pages/index/index.js";
  "./pages/order/order.js";
  "./pages/AItriage/01_AppointmentSelection.js";
  "./pages/message/message.js";
  "./pages/profile/profile.js";
  "./pages/OrderDetailPage/OrderDetailPage.js";
  "./subpkg/auth/login.js";
  "./subpkg/auth/register.js";
  "./subpkg/appointment/appointment-time.js";
  "./subpkg/profile/edit-profile.js";
  "./subpkg/system-message/system-message.js";
  "./subpkg/chat/chat.js";
  "./subpkg/appointment-flow/02_AppointmentForm.js";
  "./subpkg/appointment-flow/03_AICareMatchPage.js";
  "./subpkg/appointment-flow/04_OrderConfirmPage.js";
  "./subpkg/appointment-flow/05_PaymentSuccessPage.js";
  "./subpkg/appointment-flow/PaymentFailedPage.js";
  "./subpkg/ai/AIaks.js";
  "./subpkg/evaluate/Evaluate.js";
  "./subpkg/attendant/InviteAttendant.js";
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
    common_vendor.index.__f__("log", "at App.vue:38", "App Launch");
    try {
      await utils_tokenValidator.startupTokenCheck();
    } catch (error) {
      common_vendor.index.__f__("error", "at App.vue:44", "启动时token检测失败:", error);
    }
    const messageStore = stores_message.useMessageStore();
    await messageStore.initMessageStatus();
    utils_websocket.connectSocket();
    utils_chatWebsocket.connectChatSocket();
    setupGlobalChatListener();
    utils_tokenValidator.startPeriodicTokenCheck(30 * 60 * 1e3);
  },
  onShow: async function() {
    common_vendor.index.__f__("log", "at App.vue:60", "App Show");
    utils_websocket.connectSocket();
    utils_chatWebsocket.connectChatSocket();
    try {
      const messageStore = stores_message.useMessageStore();
      await messageStore.refreshUnreadCounts();
    } catch (e) {
      common_vendor.index.__f__("error", "at App.vue:67", "刷新未读消息失败", e);
    }
  },
  onHide: function() {
    common_vendor.index.__f__("log", "at App.vue:71", "App Hide");
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

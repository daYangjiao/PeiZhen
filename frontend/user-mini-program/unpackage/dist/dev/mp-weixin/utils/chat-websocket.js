"use strict";
const common_vendor = require("../common/vendor.js");
const utils_api = require("./api.js");
let socketTask = null;
let reconnectTimer = null;
const listeners = [];
const connectChatSocket = () => {
  utils_api.getToken();
  const userInfo = common_vendor.index.getStorageSync("userInfo");
  if (!userInfo || !userInfo.id)
    return;
  const wsUrl = utils_api.config.baseURL.replace("http", "ws") + `/ws/chat?userId=${userInfo.id}`;
  socketTask = common_vendor.index.connectSocket({
    url: wsUrl,
    success: () => common_vendor.index.__f__("log", "at utils/chat-websocket.js:18", "Chat WebSocket 连接请求发送成功")
  });
  socketTask.onOpen(() => {
    common_vendor.index.__f__("log", "at utils/chat-websocket.js:22", "Chat WebSocket 连接已打开");
    if (reconnectTimer) {
      clearInterval(reconnectTimer);
      reconnectTimer = null;
    }
    common_vendor.index.__f__("log", "at utils/chat-websocket.js:30", "WebSocket连接成功，保持现有未读状态");
  });
  socketTask.onMessage((res) => {
    try {
      const message = JSON.parse(res.data);
      common_vendor.index.__f__("log", "at utils/chat-websocket.js:36", "Chat WebSocket收到消息:", message);
      listeners.forEach((listener) => listener(message));
      common_vendor.index.$emit("chat:message", message);
      common_vendor.index.$emit("websocket:message", {
        type: "new_message",
        data: message
      });
    } catch (e) {
      common_vendor.index.__f__("error", "at utils/chat-websocket.js:49", "消息解析失败", e);
    }
  });
  socketTask.onClose(() => {
    common_vendor.index.__f__("log", "at utils/chat-websocket.js:54", "Chat WebSocket 连接已关闭");
    if (!reconnectTimer) {
      reconnectTimer = setInterval(() => {
        connectChatSocket();
      }, 5e3);
    }
  });
};
const addChatListener = (callback) => {
  listeners.push(callback);
};
const removeChatListener = (callback) => {
  const index = listeners.indexOf(callback);
  if (index > -1) {
    listeners.splice(index, 1);
  }
};
exports.addChatListener = addChatListener;
exports.connectChatSocket = connectChatSocket;
exports.removeChatListener = removeChatListener;
//# sourceMappingURL=../../.sourcemap/mp-weixin/utils/chat-websocket.js.map

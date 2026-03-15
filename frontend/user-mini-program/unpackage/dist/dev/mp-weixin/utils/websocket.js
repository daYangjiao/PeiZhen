"use strict";
const common_vendor = require("../common/vendor.js");
const utils_api = require("./api.js");
let socketTask = null;
let reconnectTimer = null;
const listeners = [];
const connectSocket = () => {
  utils_api.getToken();
  const userInfo = common_vendor.index.getStorageSync("userInfo");
  if (!userInfo || !userInfo.id) {
    common_vendor.index.__f__("log", "at utils/websocket.js:14", "WebSocket: 用户未登录，跳过连接");
    return;
  }
  const wsUrl = utils_api.config.baseURL.replace("http", "ws") + `/ws/orders?userId=${userInfo.id}`;
  common_vendor.index.__f__("log", "at utils/websocket.js:23", "WebSocket: 开始连接", wsUrl);
  socketTask = common_vendor.index.connectSocket({
    url: wsUrl,
    success: () => {
      common_vendor.index.__f__("log", "at utils/websocket.js:28", "WebSocket: 连接请求发送成功");
    },
    fail: (err) => {
      common_vendor.index.__f__("error", "at utils/websocket.js:31", "WebSocket: 连接请求发送失败", err);
    }
  });
  socketTask.onOpen(() => {
    common_vendor.index.__f__("log", "at utils/websocket.js:36", "WebSocket: 连接已打开");
    if (reconnectTimer) {
      clearInterval(reconnectTimer);
      reconnectTimer = null;
    }
  });
  socketTask.onMessage((res) => {
    common_vendor.index.__f__("log", "at utils/websocket.js:45", "WebSocket: 收到消息", res.data);
    try {
      const message = JSON.parse(res.data);
      listeners.forEach((listener) => listener(message));
      if (message.type === "ORDER_ACCEPTED" && message.attendantName && message.orderId) {
        const currentOrders = common_vendor.index.getStorageSync("userOrders") || [];
        const orderExists = currentOrders.some((order) => order.orderId === message.orderId);
        if (orderExists) {
          common_vendor.index.showToast({
            title: `陪诊师 ${message.attendantName} 已接单`,
            icon: "none",
            duration: 3e3
          });
          const updatedOrders = currentOrders.map((order) => {
            if (order.orderId === message.orderId) {
              return {
                ...order,
                orderStatus: 2,
                // 已接单
                attendantName: message.attendantName
              };
            }
            return order;
          });
          common_vendor.index.setStorageSync("userOrders", updatedOrders);
          common_vendor.index.$emit("orderStatusChanged", {
            orderId: message.orderId,
            status: 2,
            attendantName: message.attendantName
          });
        } else {
          common_vendor.index.__f__("log", "at utils/websocket.js:85", "WebSocket: 收到其他用户的订单通知，忽略");
        }
      }
      if (message.type === "SYSTEM_MESSAGE" || message.type === "NOTIFICATION") {
        common_vendor.index.__f__("log", "at utils/websocket.js:91", "收到系统消息:", message);
        common_vendor.index.$emit("system:message", message);
        common_vendor.index.$emit("websocket:message", {
          type: "system_message",
          data: message
        });
        if (message.action === "NEW_MESSAGE") {
          common_vendor.index.$emit("newSystemMessage");
        }
      }
    } catch (e) {
      common_vendor.index.__f__("error", "at utils/websocket.js:105", "WebSocket: 消息解析失败", e);
    }
  });
  socketTask.onClose(() => {
    common_vendor.index.__f__("log", "at utils/websocket.js:110", "WebSocket: 连接已关闭");
    if (!reconnectTimer) {
      reconnectTimer = setInterval(() => {
        common_vendor.index.__f__("log", "at utils/websocket.js:115", "WebSocket: 尝试重连...");
        connectSocket();
      }, 5e3);
    }
  });
  socketTask.onError((err) => {
    common_vendor.index.__f__("error", "at utils/websocket.js:122", "WebSocket: 连接错误", err);
  });
};
const addSocketListener = (callback) => {
  listeners.push(callback);
};
const removeSocketListener = (callback) => {
  const index = listeners.indexOf(callback);
  if (index > -1) {
    listeners.splice(index, 1);
  }
};
exports.addSocketListener = addSocketListener;
exports.connectSocket = connectSocket;
exports.removeSocketListener = removeSocketListener;
//# sourceMappingURL=../../.sourcemap/mp-weixin/utils/websocket.js.map

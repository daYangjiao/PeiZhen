"use strict";
const common_vendor = require("../../common/vendor.js");
const common_assets = require("../../common/assets.js");
const _sfc_main = {
  __name: "message",
  setup(__props) {
    const messageList = common_vendor.ref([
      {
        id: 1,
        name: "张先生",
        avatar: "/static/user-placeholder.png",
        lastMessage: "谢谢您的帮助，陪诊服务很满意",
        lastTime: "2023-11-15 16:30",
        unreadCount: 0,
        orderId: "PZ202311150001"
      },
      {
        id: 2,
        name: "李女士",
        avatar: "/static/user-placeholder.png",
        lastMessage: "请问您现在到医院了吗？",
        lastTime: "2023-11-16 09:15",
        unreadCount: 2,
        orderId: "PZ202311160002"
      },
      {
        id: 3,
        name: "系统消息",
        avatar: "/static/logo.png",
        lastMessage: "您有新的订单待处理",
        lastTime: "2023-11-16 08:00",
        unreadCount: 1,
        type: "system"
      }
    ]);
    common_vendor.onMounted(() => {
      loadMessages();
    });
    const loadMessages = () => {
      common_vendor.index.__f__("log", "at pages/message/message.vue:80", "加载消息列表");
    };
    const formatTime = (timeStr) => {
      const now = /* @__PURE__ */ new Date();
      const compatibleTimeStr = timeStr.replace(/-/g, "/");
      const time = new Date(compatibleTimeStr);
      if (isNaN(time.getTime())) {
        common_vendor.index.__f__("error", "at pages/message/message.vue:92", "Invalid date format:", timeStr);
        return timeStr;
      }
      const diff = now.getTime() - time.getTime();
      if (diff < 24 * 60 * 60 * 1e3 && now.getDate() === time.getDate()) {
        return time.toLocaleTimeString("zh-CN", { hour: "2-digit", minute: "2-digit" });
      }
      if (diff < 48 * 60 * 60 * 1e3) {
        return "昨天";
      }
      return time.toLocaleDateString("zh-CN", { month: "2-digit", day: "2-digit" });
    };
    const openChat = (message) => {
      if (message.type === "system") {
        common_vendor.index.showToast({
          title: "系统消息详情",
          icon: "none"
        });
        return;
      }
      common_vendor.index.navigateTo({
        url: `/subpkg/chat/chat?userId=${message.id}&name=${message.name}`
      });
      message.unreadCount = 0;
    };
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_vendor.f(messageList.value, (message, k0, i0) => {
          return common_vendor.e({
            a: message.avatar,
            b: message.unreadCount > 0
          }, message.unreadCount > 0 ? {
            c: common_vendor.t(message.unreadCount > 99 ? "99+" : message.unreadCount)
          } : {}, {
            d: common_vendor.t(message.name),
            e: common_vendor.t(formatTime(message.lastTime)),
            f: common_vendor.t(message.lastMessage),
            g: message.unreadCount > 0 ? 1 : "",
            h: message.id,
            i: common_vendor.o(($event) => openChat(message), message.id)
          });
        }),
        b: messageList.value.length === 0
      }, messageList.value.length === 0 ? {
        c: common_assets._imports_0$1
      } : {});
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-4c1b26cf"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/message/message.js.map

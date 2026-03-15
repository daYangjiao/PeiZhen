"use strict";
const common_vendor = require("../../common/vendor.js");
const common_assets = require("../../common/assets.js");
const utils_api = require("../../utils/api.js");
const utils_chatWebsocket = require("../../utils/chat-websocket.js");
const _sfc_main = {
  __name: "system-message",
  setup(__props) {
    const systemMessages = common_vendor.ref([]);
    const scrollTop = common_vendor.ref(0);
    const scrollIntoView = common_vendor.ref("");
    const currentTab = common_vendor.ref(0);
    const tabs = ["全部", "订单状态", "服务提醒", "平台公告", "账户相关"];
    common_vendor.onLoad(() => {
      loadSystemMessages();
      utils_chatWebsocket.addChatListener(handleNewMessage);
    });
    common_vendor.onUnmounted(() => {
      utils_chatWebsocket.removeChatListener(handleNewMessage);
    });
    const handleNewMessage = (msg) => {
      loadSystemMessages();
    };
    const loadSystemMessages = async () => {
      try {
        const res = await utils_api.get("/api/chat/history?targetUserId=0");
        if (res.code === 200 && res.data) {
          const apiData = res.data.map((msg) => ({
            ...msg,
            type: inferType(msg.content),
            title: inferTitle(msg.content),
            action: inferAction(msg.content)
          }));
          systemMessages.value = apiData.sort((a, b) => new Date(b.createTime) - new Date(a.createTime));
        } else {
          systemMessages.value = [];
        }
      } catch (e) {
        common_vendor.index.__f__("error", "at subpkg/system-message/system-message.vue:94", "加载系统消息失败", e);
        systemMessages.value = [];
      }
    };
    const inferType = (content) => {
      if (!content)
        return "平台公告";
      if (content.includes("订单") || content.includes("支付"))
        return "订单状态";
      if (content.includes("就诊") || content.includes("服务") || content.includes("评价"))
        return "服务提醒";
      if (content.includes("公告") || content.includes("维护") || content.includes("升级"))
        return "平台公告";
      if (content.includes("余额") || content.includes("充值") || content.includes("退款"))
        return "账户相关";
      return "平台公告";
    };
    const inferTitle = (content) => {
      if (!content)
        return "系统通知";
      if (content.includes("支付完成") || content.includes("支付成功"))
        return "订单支付成功";
      if (content.includes("就诊安排") || content.includes("就诊提醒"))
        return "就诊温馨提醒";
      if (content.includes("服务已完成") || content.includes("评价"))
        return "服务完成评价";
      if (content.includes("余额不足"))
        return "账户余额提醒";
      if (content.includes("维护") || content.includes("升级"))
        return "平台公告";
      return "系统通知";
    };
    const inferAction = (content) => {
      if (!content)
        return "";
      if (content.includes("支付完成") || content.includes("支付成功"))
        return "查看订单";
      if (content.includes("服务已完成") || content.includes("评价"))
        return "去评价";
      if (content.includes("余额不足"))
        return "去充值";
      return "";
    };
    const extractOrderNo = (content) => {
      if (!content)
        return null;
      const match = content.match(/订单(?:No\.|号)[:：]?\s*([A-Za-z0-9*]+)/);
      return match ? match[1] : null;
    };
    const filteredMessages = common_vendor.computed(() => {
      if (currentTab.value === 0)
        return systemMessages.value;
      const type = tabs[currentTab.value];
      return systemMessages.value.filter((msg) => msg.type === type);
    });
    const switchTab = (index) => {
      currentTab.value = index;
      scrollTop.value = 0;
    };
    const getMessageTitle = (msg) => {
      return msg.title || "系统通知";
    };
    const getActionText = (msg) => {
      return msg.action;
    };
    const handleAction = (msg) => {
      if (msg.action === "查看订单") {
        common_vendor.index.switchTab({ url: "/pages/order/order" });
      } else if (msg.action === "去评价") {
        const orderNo = extractOrderNo(msg.content);
        if (orderNo && !orderNo.includes("*")) {
          common_vendor.index.navigateTo({ url: `/pages/Evaluate/Evaluate?orderId=${orderNo}` });
        } else {
          common_vendor.index.switchTab({ url: "/pages/order/order" });
        }
      } else if (msg.action === "去充值") {
        common_vendor.index.showToast({ title: "功能开发中", icon: "none" });
      }
    };
    const formatTime = (timeStr) => {
      if (!timeStr)
        return "";
      const date = new Date(timeStr);
      const now = /* @__PURE__ */ new Date();
      if (date.toDateString() === now.toDateString()) {
        return `今天 ${String(date.getHours()).padStart(2, "0")}:${String(date.getMinutes()).padStart(2, "0")}`;
      }
      const yesterday = new Date(now);
      yesterday.setDate(now.getDate() - 1);
      if (date.toDateString() === yesterday.toDateString()) {
        return `昨天 ${String(date.getHours()).padStart(2, "0")}:${String(date.getMinutes()).padStart(2, "0")}`;
      }
      if (date.getFullYear() === now.getFullYear()) {
        return `${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
      }
      return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
    };
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_vendor.f(tabs, (tab, index, i0) => {
          return common_vendor.e({
            a: common_vendor.t(tab),
            b: currentTab.value === index
          }, currentTab.value === index ? {} : {}, {
            c: index,
            d: currentTab.value === index ? 1 : "",
            e: common_vendor.o(($event) => switchTab(index), index)
          });
        }),
        b: common_vendor.f(filteredMessages.value, (msg, index, i0) => {
          return common_vendor.e({
            a: common_vendor.t(getMessageTitle(msg)),
            b: common_vendor.t(formatTime(msg.createTime)),
            c: common_vendor.t(msg.content),
            d: getActionText(msg)
          }, getActionText(msg) ? {
            e: common_vendor.t(getActionText(msg)),
            f: common_vendor.o(($event) => handleAction(msg), index)
          } : {}, {
            g: index,
            h: "msg-" + index
          });
        }),
        c: filteredMessages.value.length === 0
      }, filteredMessages.value.length === 0 ? {
        d: common_assets._imports_0$1,
        e: common_vendor.t(tabs[currentTab.value])
      } : {}, {
        f: scrollTop.value,
        g: scrollIntoView.value
      });
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-a1145bb7"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/subpkg/system-message/system-message.js.map

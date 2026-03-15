"use strict";
const common_vendor = require("../../common/vendor.js");
const common_assets = require("../../common/assets.js");
const utils_api = require("../../utils/api.js");
const utils_chatWebsocket = require("../../utils/chat-websocket.js");
const _sfc_main = {
  __name: "order",
  setup(__props) {
    const statusBarHeight = common_vendor.ref(0);
    const searchKeyword = common_vendor.ref("");
    const statusTabs = common_vendor.ref([
      { name: "全部", value: null },
      { name: "待核销", value: 2 },
      { name: "服务中", value: 3 },
      { name: "待确认", value: 4 },
      { name: "已完成", value: 6 },
      { name: "已取消", value: 7 }
    ]);
    const activeStatus = common_vendor.ref(null);
    const orders = common_vendor.ref([]);
    const loading = common_vendor.ref(false);
    let socketListener = null;
    let pollTimer = null;
    common_vendor.onMounted(() => {
      const systemInfo = common_vendor.index.getSystemInfoSync();
      statusBarHeight.value = systemInfo.statusBarHeight || 0;
      loadOrders();
      setupWebSocketListener();
      startPolling();
    });
    common_vendor.onShow(() => {
      loadOrders();
    });
    const switchTab = (status) => {
      activeStatus.value = status;
      loadOrders();
    };
    const handleSearch = () => {
      loadOrders();
    };
    const loadOrders = async () => {
      const userInfo = common_vendor.index.getStorageSync("userInfo");
      if (!userInfo || !userInfo.id) {
        orders.value = [];
        return;
      }
      loading.value = true;
      try {
        const params = {
          attendantId: userInfo.id,
          page: 0,
          size: 50
        };
        if (activeStatus.value !== null) {
          params.orderStatus = activeStatus.value;
        }
        const res = await utils_api.get("/attendant/orders", params);
        if (res.code === 200 && res.data && res.data.content) {
          orders.value = res.data.content;
        } else {
          orders.value = [];
        }
      } catch (e) {
        common_vendor.index.__f__("error", "at pages/order/order.vue:182", "获取陪诊师订单失败:", e);
        orders.value = [];
      } finally {
        loading.value = false;
      }
    };
    const filteredOrders = common_vendor.computed(() => {
      if (!searchKeyword.value)
        return orders.value;
      const kw = searchKeyword.value.trim();
      if (!kw)
        return orders.value;
      return orders.value.filter(
        (o) => o.patientName && o.patientName.includes(kw) || o.hospital && o.hospital.includes(kw) || o.serviceContent && o.serviceContent.includes(kw) || o.serviceTypeName && o.serviceTypeName.includes(kw)
      );
    });
    const formatAmount = (amount) => {
      if (!amount)
        return "0.00";
      return Number(amount).toFixed(2);
    };
    const getAttendantIncome = (orderAmount) => {
      const raw = Number(orderAmount || 0);
      return raw * 0.9;
    };
    const getStatusText = (status) => {
      const map = {
        1: "待接单",
        2: "待核销",
        3: "服务中",
        4: "待确认",
        5: "待补款",
        6: "已完成",
        7: "已取消"
      };
      return map[status] || "未知";
    };
    const getStatusClass = (status) => {
      const map = {
        1: "status-waiting",
        2: "status-accepted",
        3: "status-service",
        4: "status-confirm",
        6: "status-completed",
        7: "status-cancelled"
      };
      return map[status] || "status-default";
    };
    const getPatientAvatar = (order) => {
      const placeholder = "/static/user-placeholder.png";
      if (!order || !order.userAvatar)
        return placeholder;
      const path = order.userAvatar;
      if (path.startsWith("http"))
        return path;
      const base = utils_api.config.baseURL.replace(/\/$/, "");
      if (path.startsWith("/"))
        return base + path;
      return base + "/" + path;
    };
    const goToDetail = (order) => {
      common_vendor.index.navigateTo({
        url: `/subpkg/order/detail?orderId=${order.orderId}`
      });
    };
    const handleSocketMessage = (message) => {
      common_vendor.index.__f__("log", "at pages/order/order.vue:255", "陪诊师订单页收到WebSocket消息:", message);
      if (message.type === "NEW_ORDER" || message.type === "ORDER_ACCEPTED" || message.type === "SERVICE_STARTED" || message.type === "SERVICE_COMPLETED" || message.type === "ORDER_STATUS_CHANGED") {
        setTimeout(() => {
          loadOrders();
        }, 1e3);
      }
    };
    const setupWebSocketListener = () => {
      if (socketListener) {
        utils_chatWebsocket.removeChatListener(socketListener);
      }
      socketListener = handleSocketMessage;
      utils_chatWebsocket.addChatListener(socketListener);
    };
    const startPolling = () => {
      if (pollTimer) {
        clearInterval(pollTimer);
      }
      pollTimer = setInterval(() => {
        loadOrders();
      }, 15e3);
    };
    common_vendor.onUnmounted(() => {
      if (socketListener) {
        utils_chatWebsocket.removeChatListener(socketListener);
        socketListener = null;
      }
      if (pollTimer) {
        clearInterval(pollTimer);
        pollTimer = null;
      }
    });
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_assets._imports_0,
        b: common_vendor.o(handleSearch),
        c: searchKeyword.value,
        d: common_vendor.o(($event) => searchKeyword.value = $event.detail.value),
        e: common_vendor.f(statusTabs.value, (tab, index, i0) => {
          return common_vendor.e({
            a: common_vendor.t(tab.name),
            b: activeStatus.value === tab.value
          }, activeStatus.value === tab.value ? {} : {}, {
            c: index,
            d: activeStatus.value === tab.value ? 1 : "",
            e: common_vendor.o(($event) => switchTab(tab.value), index)
          });
        }),
        f: loading.value
      }, loading.value ? {} : {}, {
        g: !loading.value && filteredOrders.value.length > 0
      }, !loading.value && filteredOrders.value.length > 0 ? {
        h: common_vendor.f(filteredOrders.value, (order, k0, i0) => {
          return {
            a: common_vendor.t(order.orderNo),
            b: common_vendor.t(getStatusText(order.orderStatus)),
            c: common_vendor.n(getStatusClass(order.orderStatus)),
            d: common_vendor.t(order.serviceContent || order.serviceTypeName),
            e: common_vendor.t(order.hospital),
            f: common_vendor.t(order.serviceDate),
            g: common_vendor.t(order.serviceTimeSlot || ""),
            h: common_vendor.t(formatAmount(getAttendantIncome(order.orderAmount))),
            i: getPatientAvatar(order),
            j: common_vendor.t(order.patientName || order.contactPerson),
            k: common_vendor.o(($event) => goToDetail(order), order.orderId),
            l: order.orderId,
            m: common_vendor.o(($event) => goToDetail(order), order.orderId)
          };
        }),
        i: common_assets._imports_2$2
      } : !loading.value ? {
        k: common_assets._imports_2
      } : {}, {
        j: !loading.value,
        l: statusBarHeight.value + "px"
      });
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-93207a4f"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/order/order.js.map

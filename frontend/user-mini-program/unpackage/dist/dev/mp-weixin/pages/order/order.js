"use strict";
const common_vendor = require("../../common/vendor.js");
const common_assets = require("../../common/assets.js");
const utils_api = require("../../utils/api.js");
const stores_user = require("../../stores/user.js");
const utils_websocket = require("../../utils/websocket.js");
const _sfc_main = {
  __name: "order",
  setup(__props) {
    const statusBarHeight = common_vendor.ref(0);
    const searchKeyword = common_vendor.ref("");
    const statusTabs = common_vendor.ref([
      { name: "全部", value: null },
      { name: "待支付", value: 0 },
      { name: "待接单", value: 1 },
      { name: "待服务", value: 2 },
      { name: "服务中", value: 3 },
      { name: "待确认", value: 4 },
      { name: "待补款", value: 5 },
      { name: "已完成", value: 6 },
      { name: "已取消", value: 7 }
    ]);
    const activeStatus = common_vendor.ref(null);
    const orders = common_vendor.ref([]);
    const loading = common_vendor.ref(false);
    const userStore = stores_user.useUserStore();
    let socketListener = null;
    let pollTimer = null;
    common_vendor.onMounted(async () => {
      const systemInfo = common_vendor.index.getSystemInfoSync();
      statusBarHeight.value = systemInfo.statusBarHeight || 0;
    });
    common_vendor.onShow(() => {
      common_vendor.index.__f__("log", "at pages/order/order.vue:155", "Order page onShow - refreshing data");
      userStore.restoreFromStorage();
      loadOrders();
      setupWebSocketListener();
      startPolling();
    });
    const switchTab = (status) => {
      activeStatus.value = status;
    };
    async function loadOrders() {
      if (!userStore.isLoggedIn) {
        orders.value = [];
        return;
      }
      loading.value = true;
      try {
        const response = await utils_api.get("/api/orders/user-orders", {
          page: 0,
          pageSize: 100,
          keyword: searchKeyword.value || void 0
        });
        if (response.code === 200 && response.data && response.data.content) {
          orders.value = response.data.content;
          common_vendor.index.__f__("log", "at pages/order/order.vue:187", "从后端获取的订单列表:", orders.value);
        } else {
          orders.value = [];
        }
      } catch (error) {
        orders.value = [];
        common_vendor.index.__f__("error", "at pages/order/order.vue:193", "加载订单数据出错:", error);
      } finally {
        loading.value = false;
      }
    }
    const filteredOrders = common_vendor.computed(() => {
      if (activeStatus.value === null) {
        return orders.value;
      }
      return orders.value.filter((order) => {
        if (activeStatus.value === 0) {
          return order.paymentStatus === 0 && order.orderStatus !== 7;
        }
        return order.orderStatus === activeStatus.value;
      });
    });
    const getStatusText = (status) => {
      const map = {
        0: "待支付",
        1: "待接单",
        2: "待服务",
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
        0: "status-pending",
        1: "status-waiting",
        2: "status-accepted",
        3: "status-service",
        4: "status-confirm",
        5: "status-balance",
        6: "status-completed",
        7: "status-cancelled"
      };
      return map[status] || "status-default";
    };
    const getAvatarUrl = (avatarPath) => {
      if (!avatarPath)
        return "/static/default-avatar.jpg";
      if (avatarPath.startsWith("http"))
        return avatarPath;
      if (avatarPath.startsWith("/uploads/"))
        return utils_api.config.baseURL + avatarPath;
      if (avatarPath.startsWith("/"))
        return utils_api.config.baseURL + avatarPath;
      return utils_api.config.baseURL + "/uploads/" + avatarPath;
    };
    const handleOrderClick = (order) => {
      common_vendor.index.navigateTo({
        url: `/pages/OrderDetailPage/OrderDetailPage?orderNo=${order.orderNo}`
      });
    };
    const handleDetailClick = (order) => {
      handleOrderClick(order);
    };
    const handlePay = (order) => {
      common_vendor.index.navigateTo({
        url: `/subpkg/appointment-flow/04_OrderConfirmPage?orderNo=${order.orderNo}`
      });
    };
    const handleSearch = () => {
      loadOrders();
    };
    const handleSocketMessage = (message) => {
      common_vendor.index.__f__("log", "at pages/order/order.vue:271", "订单页收到WebSocket消息:", message);
      if (message.type === "ORDER_ACCEPTED" || message.type === "SERVICE_STARTED" || message.type === "SERVICE_COMPLETED" || message.type === "ORDER_STATUS_CHANGED" || message.type === "SERVICE_PROGRESS_UPDATED") {
        common_vendor.index.__f__("log", "at pages/order/order.vue:280", "检测到订单状态变更，刷新订单列表");
        setTimeout(() => {
          loadOrders();
        }, 1e3);
      }
    };
    const setupWebSocketListener = () => {
      if (socketListener) {
        utils_websocket.removeSocketListener(socketListener);
      }
      socketListener = handleSocketMessage;
      utils_websocket.addSocketListener(socketListener);
    };
    const startPolling = () => {
      if (pollTimer) {
        clearInterval(pollTimer);
      }
      pollTimer = setInterval(() => {
        const shouldPoll = orders.value.some(
          (order) => order.paymentStatus === 0 && order.orderStatus !== 7 || [1, 2, 3, 4, 5].includes(order.orderStatus)
        );
        if (shouldPoll) {
          loadOrders();
        }
      }, 1e4);
    };
    common_vendor.onUnmounted(() => {
      if (socketListener) {
        utils_websocket.removeSocketListener(socketListener);
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
          return common_vendor.e({
            a: common_vendor.t(order.orderNo),
            b: common_vendor.t(getStatusText(order.orderStatus)),
            c: common_vendor.n(getStatusClass(order.orderStatus)),
            d: common_vendor.t(order.serviceTypeName || order.serviceContent),
            e: common_vendor.t(order.hospital),
            f: common_vendor.t(order.serviceDate),
            g: common_vendor.t(order.serviceTimeSlot),
            h: common_vendor.t(order.orderAmount.toFixed(2)),
            i: order.attendantName
          }, order.attendantName ? {
            j: getAvatarUrl(order.attendantAvatar),
            k: common_vendor.t(order.attendantName)
          } : {
            l: common_assets._imports_2,
            m: common_vendor.t(order.orderStatus === 7 ? "已取消" : order.paymentStatus === 0 ? "待支付" : "待分配")
          }, {
            n: common_vendor.o(($event) => handleDetailClick(order), order.orderNo),
            o: order.paymentStatus === 0 && order.orderStatus !== 7
          }, order.paymentStatus === 0 && order.orderStatus !== 7 ? {
            p: common_vendor.o(($event) => handlePay(order), order.orderNo)
          } : {}, {
            q: order.orderNo,
            r: common_vendor.o(($event) => handleOrderClick(order), order.orderNo)
          });
        }),
        i: common_assets._imports_1
      } : !loading.value ? {
        k: common_assets._imports_1$1
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

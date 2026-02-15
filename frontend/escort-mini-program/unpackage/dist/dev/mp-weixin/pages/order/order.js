"use strict";
const common_vendor = require("../../common/vendor.js");
const common_assets = require("../../common/assets.js");
const _sfc_main = {
  __name: "order",
  setup(__props) {
    const currentTab = common_vendor.ref(0);
    const tabs = common_vendor.ref([
      { name: "全部", status: "all", count: 0 },
      { name: "已接单", status: "accepted", count: 0 },
      { name: "进行中", status: "in_progress", count: 0 },
      { name: "已完成", status: "completed", count: 0 },
      { name: "已取消", status: "cancelled", count: 0 }
    ]);
    const orderList = common_vendor.ref([]);
    const filteredOrders = common_vendor.computed(() => {
      if (currentTab.value === 0) {
        return orderList.value;
      }
      return orderList.value.filter((order) => order.status === tabs.value[currentTab.value].status);
    });
    const updateOrderCounts = () => {
      tabs.value[0].count = orderList.value.length;
      tabs.value[1].count = orderList.value.filter((order) => order.status === "accepted").length;
      tabs.value[2].count = orderList.value.filter((order) => order.status === "in_progress").length;
      tabs.value[3].count = orderList.value.filter((order) => order.status === "completed").length;
      tabs.value[4].count = orderList.value.filter((order) => order.status === "cancelled").length;
    };
    const loadAcceptedOrders = () => {
      const acceptedOrders = common_vendor.index.getStorageSync("acceptedOrders") || [];
      orderList.value = acceptedOrders.map((order) => {
        let createTime = order.createTime || order.acceptTime;
        if (!createTime) {
          const now = /* @__PURE__ */ new Date();
          const year = now.getFullYear();
          const month = String(now.getMonth() + 1).padStart(2, "0");
          const day = String(now.getDate()).padStart(2, "0");
          const hours = String(now.getHours()).padStart(2, "0");
          const minutes = String(now.getMinutes()).padStart(2, "0");
          createTime = `${year}/${month}/${day} ${hours}:${minutes}`;
        }
        return {
          ...order,
          orderNo: order.orderNo || `PZ${Date.now()}`,
          createTime,
          distance: order.distance || "未知"
        };
      });
      updateOrderCounts();
    };
    const switchTab = (index) => {
      currentTab.value = index;
    };
    const getStatusText = (status) => {
      switch (status) {
        case "accepted":
          return "已接单";
        case "in_progress":
          return "进行中";
        case "completed":
          return "已完成";
        case "cancelled":
          return "已取消";
        default:
          return "未知状态";
      }
    };
    const goToDetail = (orderId) => {
      common_vendor.index.navigateTo({
        url: `/subpkg/order/detail?orderId=${orderId}`
      });
    };
    common_vendor.onMounted(() => {
      common_vendor.index.$on("orderStatusUpdated", (data) => {
        const { orderId, status } = data;
        const order = orderList.value.find((item) => item.id === orderId);
        if (order) {
          order.status = status;
          updateOrderCounts();
        }
      });
      common_vendor.index.$on("orderAccepted", (acceptedOrder) => {
        orderList.value.unshift(acceptedOrder);
        updateOrderCounts();
      });
      loadAcceptedOrders();
    });
    common_vendor.onUnmounted(() => {
      common_vendor.index.$off("orderStatusUpdated");
      common_vendor.index.$off("orderAccepted");
    });
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_vendor.f(tabs.value, (tab, index, i0) => {
          return common_vendor.e({
            a: common_vendor.t(tab.name),
            b: tab.count > 0
          }, tab.count > 0 ? {
            c: common_vendor.t(tab.count)
          } : {}, {
            d: currentTab.value === index ? 1 : "",
            e: index,
            f: common_vendor.o(($event) => switchTab(index), index)
          });
        }),
        b: common_vendor.f(filteredOrders.value, (order, k0, i0) => {
          return common_vendor.e({
            a: common_vendor.t(order.orderNo),
            b: common_vendor.t(order.createTime),
            c: common_vendor.t(getStatusText(order.status)),
            d: common_vendor.n(order.status),
            e: order.userAvatar,
            f: common_vendor.t(order.userName),
            g: common_vendor.t(order.serviceType),
            h: common_vendor.t(order.hospitalName),
            i: common_vendor.t(order.distance),
            j: common_vendor.t(order.price),
            k: order.status !== "cancelled"
          }, order.status !== "cancelled" ? {
            l: common_vendor.t(order.appointmentTime)
          } : {}, {
            m: order.status === "cancelled" && order.cancelReason
          }, order.status === "cancelled" && order.cancelReason ? {
            n: common_vendor.t(order.cancelReason)
          } : {}, {
            o: order.id,
            p: common_vendor.o(($event) => goToDetail(order.id), order.id)
          });
        }),
        c: filteredOrders.value.length === 0
      }, filteredOrders.value.length === 0 ? {
        d: common_assets._imports_0,
        e: common_vendor.t(tabs.value[currentTab.value].name)
      } : {});
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-93207a4f"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/order/order.js.map

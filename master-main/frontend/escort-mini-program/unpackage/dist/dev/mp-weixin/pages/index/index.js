"use strict";
const common_vendor = require("../../common/vendor.js");
if (!Math) {
  OrderCard();
}
const OrderCard = () => "../../components/OrderCard.js";
const _sfc_main = {
  __name: "index",
  setup(__props, { expose: __expose }) {
    const originalOrderList = common_vendor.ref([
      {
        id: 1,
        userName: "王大爷",
        userAge: 72,
        userGender: "男",
        userAvatar: "/static/user-placeholder.png",
        serviceType: "糖尿病复查",
        hospitalName: "北京协和医院",
        distance: "1.8km",
        appointmentTime: "2023-11-15 09:30",
        appointmentEndTime: "2023-11-15 11:30",
        duration: "2小时",
        serviceDetail: "代取化验报告",
        specialNote: "需要轮椅协助",
        price: 180,
        distanceValue: 1.8,
        timeValue: (/* @__PURE__ */ new Date("2023/11/15 09:30")).getTime()
      },
      {
        id: 2,
        userName: "陈女士",
        userAge: 28,
        userGender: "女",
        userAvatar: "/static/user-placeholder.png",
        serviceType: "孕期检查",
        hospitalName: "上海妇产科医院",
        distance: "3.2km",
        appointmentTime: "2023-11-16 14:00",
        appointmentEndTime: "2023-11-16 16:30",
        duration: "2.5小时",
        serviceDetail: "陪同产检",
        specialNote: "怀孕6个月，行动不便",
        price: 220,
        distanceValue: 3.2,
        timeValue: (/* @__PURE__ */ new Date("2023/11/16 14:00")).getTime()
      }
    ]);
    const showFilter = common_vendor.ref(false);
    const filterData = common_vendor.ref({
      distance: "",
      price: "",
      time: ""
    });
    const distanceOptions = common_vendor.ref([
      { label: "全部", value: "" },
      { label: "1km内", value: "1" },
      { label: "3km内", value: "3" },
      { label: "5km内", value: "5" }
    ]);
    const priceOptions = common_vendor.ref([
      { label: "全部", value: "" },
      { label: "价格升序", value: "asc" },
      { label: "价格降序", value: "desc" }
    ]);
    const timeOptions = common_vendor.ref([
      { label: "全部", value: "" },
      { label: "时间最近", value: "nearest" },
      { label: "时间最远", value: "farthest" }
    ]);
    const orderList = common_vendor.computed(() => {
      let filtered = [...originalOrderList.value];
      if (filterData.value.distance) {
        const maxDistance = parseFloat(filterData.value.distance);
        filtered = filtered.filter((order) => order.distanceValue <= maxDistance);
      }
      if (filterData.value.price === "asc") {
        filtered.sort((a, b) => a.price - b.price);
      } else if (filterData.value.price === "desc") {
        filtered.sort((a, b) => b.price - a.price);
      }
      if (filterData.value.time === "nearest") {
        filtered.sort((a, b) => a.timeValue - b.timeValue);
      } else if (filterData.value.time === "farthest") {
        filtered.sort((a, b) => b.timeValue - a.timeValue);
      }
      return filtered;
    });
    const isLoading = common_vendor.ref(false);
    let autoRefreshTimer = null;
    const startAutoRefresh = () => {
      stopAutoRefresh();
      autoRefreshTimer = setInterval(() => {
        loadOrders(true);
      }, 3e4);
    };
    const stopAutoRefresh = () => {
      if (autoRefreshTimer) {
        clearInterval(autoRefreshTimer);
        autoRefreshTimer = null;
      }
    };
    common_vendor.onMounted(() => {
      loadOrders();
      startAutoRefresh();
    });
    common_vendor.onUnmounted(() => {
      stopAutoRefresh();
    });
    const onShow = () => {
      startAutoRefresh();
    };
    const onHide = () => {
      stopAutoRefresh();
    };
    __expose({
      onShow,
      onHide
    });
    const loadOrders = (isRefresh = false) => {
      common_vendor.index.__f__("log", "at pages/index/index.vue:245", "加载订单列表", isRefresh ? "(刷新模式)" : "");
      if (isRefresh) {
        originalOrderList.value = [...originalOrderList.value];
      }
    };
    const viewOrderDetail = (order) => {
      common_vendor.index.navigateTo({
        url: `/subpkg/order/detail?orderId=${order.id}`
      });
    };
    const refreshOrders = () => {
      common_vendor.index.showToast({
        title: "正在刷新订单...",
        icon: "loading",
        duration: 1500
      });
      setTimeout(() => {
        loadOrders(true);
        common_vendor.index.showToast({
          title: "刷新完成",
          icon: "success",
          duration: 1e3
        });
      }, 1500);
    };
    const toggleFilter = () => {
      showFilter.value = !showFilter.value;
    };
    const setFilter = (type, value) => {
      filterData.value[type] = filterData.value[type] === value ? "" : value;
    };
    const resetFilter = () => {
      filterData.value = {
        distance: "",
        price: "",
        time: ""
      };
    };
    const applyFilter = () => {
      showFilter.value = false;
      common_vendor.index.showToast({
        title: "筛选已应用",
        icon: "success",
        duration: 1500
      });
    };
    const loadMore = () => {
      if (isLoading.value)
        return;
      isLoading.value = true;
      setTimeout(() => {
        isLoading.value = false;
      }, 1e3);
    };
    const contactPatient = (order) => {
      common_vendor.index.showActionSheet({
        itemList: ["拨打电话", "发送消息"],
        success: (res) => {
          if (res.tapIndex === 0) {
            common_vendor.index.makePhoneCall({
              phoneNumber: order.phone
            });
          } else if (res.tapIndex === 1) {
            common_vendor.index.navigateTo({
              url: `/subpkg/chat/chat?userId=${order.userId}&name=${order.userName}`
            });
          }
        }
      });
    };
    const acceptOrder = (actionData) => {
      const order = actionData.data;
      common_vendor.index.showModal({
        title: "确认接单",
        content: `确定要接受${order.userName}的陪诊订单吗？`,
        success: (res) => {
          if (res.confirm) {
            const now = /* @__PURE__ */ new Date();
            const year = now.getFullYear();
            const month = String(now.getMonth() + 1).padStart(2, "0");
            const day = String(now.getDate()).padStart(2, "0");
            const hours = String(now.getHours()).padStart(2, "0");
            const minutes = String(now.getMinutes()).padStart(2, "0");
            const createTime = `${year}/${month}/${day} ${hours}:${minutes}`;
            const acceptedOrder = {
              ...order,
              status: "accepted",
              acceptTime: (/* @__PURE__ */ new Date()).toISOString(),
              orderNo: order.orderNo || `PZ${Date.now()}${Math.floor(Math.random() * 1e3)}`,
              createTime: order.createTime || createTime
            };
            let acceptedOrders = common_vendor.index.getStorageSync("acceptedOrders") || [];
            acceptedOrders.push(acceptedOrder);
            common_vendor.index.setStorageSync("acceptedOrders", acceptedOrders);
            common_vendor.index.$emit("orderAccepted", acceptedOrder);
            common_vendor.index.showToast({
              title: "接单成功",
              icon: "success"
            });
            const index = originalOrderList.value.findIndex((item) => item.id === order.id);
            if (index > -1) {
              originalOrderList.value.splice(index, 1);
            }
          }
        }
      });
    };
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_vendor.o(toggleFilter),
        b: common_vendor.o(refreshOrders),
        c: common_vendor.f(distanceOptions.value, (option, k0, i0) => {
          return {
            a: common_vendor.t(option.label),
            b: option.value,
            c: common_vendor.n({
              active: filterData.value.distance === option.value
            }),
            d: common_vendor.o(($event) => setFilter("distance", option.value), option.value)
          };
        }),
        d: common_vendor.f(priceOptions.value, (option, k0, i0) => {
          return {
            a: common_vendor.t(option.label),
            b: option.value,
            c: common_vendor.n({
              active: filterData.value.price === option.value
            }),
            d: common_vendor.o(($event) => setFilter("price", option.value), option.value)
          };
        }),
        e: common_vendor.f(timeOptions.value, (option, k0, i0) => {
          return {
            a: common_vendor.t(option.label),
            b: option.value,
            c: common_vendor.n({
              active: filterData.value.time === option.value
            }),
            d: common_vendor.o(($event) => setFilter("time", option.value), option.value)
          };
        }),
        f: common_vendor.o(resetFilter),
        g: common_vendor.o(applyFilter),
        h: showFilter.value,
        i: common_vendor.f(orderList.value, (order, index, i0) => {
          return {
            a: order.id,
            b: common_vendor.o(contactPatient, order.id),
            c: common_vendor.o(acceptOrder, order.id),
            d: common_vendor.o(($event) => viewOrderDetail(order), order.id),
            e: common_vendor.o(($event) => acceptOrder(order), order.id),
            f: "1cf27b2a-0-" + i0,
            g: common_vendor.p({
              ["order-data"]: order,
              ["show-actions"]: true,
              ["action-type"]: "accept"
            })
          };
        }),
        j: showFilter.value ? 1 : "",
        k: common_vendor.o(loadMore),
        l: isLoading.value
      }, isLoading.value ? {} : {});
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-1cf27b2a"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/index/index.js.map

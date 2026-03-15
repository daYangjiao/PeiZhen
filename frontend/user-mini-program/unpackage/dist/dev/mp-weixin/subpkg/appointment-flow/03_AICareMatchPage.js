"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_api = require("../../utils/api.js");
const _sfc_main = {
  __name: "03_AICareMatchPage",
  setup(__props) {
    const loading = common_vendor.ref(true);
    const orders = common_vendor.ref([]);
    const showOrderDetail = common_vendor.ref(false);
    const currentOrder = common_vendor.ref({});
    const serviceTypeFilter = common_vendor.ref("");
    const sortOption = common_vendor.ref("time");
    const currentPage = common_vendor.ref(0);
    const pageSize = common_vendor.ref(10);
    const hasMore = common_vendor.ref(true);
    const serviceTypes = ["全部", "普通陪诊", "术后护理", "急诊陪同", "上门陪诊"];
    const sortOptions = ["时间优先", "距离优先", "价格优先"];
    const selectedServiceTypeName = common_vendor.computed(() => {
      return serviceTypes[serviceTypeFilter.value] || "全部";
    });
    const selectedSortName = common_vendor.computed(() => {
      return sortOptions[sortOption.value] || "时间优先";
    });
    const displayedOrders = common_vendor.computed(() => {
      let filtered = [...orders.value];
      if (serviceTypeFilter.value > 0) {
        filtered = filtered.filter((order) => order.clinicType === serviceTypeFilter.value);
      }
      if (sortOption.value === 0) {
        filtered.sort((a, b) => new Date(a.appointmentTime) - new Date(b.appointmentTime));
      } else if (sortOption.value === 1) {
        filtered.sort((a, b) => a.hospital.localeCompare(b.hospital));
      } else if (sortOption.value === 2) {
        filtered.sort((a, b) => a.orderAmount - b.orderAmount);
      }
      return filtered;
    });
    common_vendor.onLoad(() => {
      loadOrders();
    });
    const loadOrders = async () => {
      try {
        loading.value = true;
        const response = await utils_api.get("/attendant/orders/waiting", {
          page: currentPage.value,
          size: pageSize.value
        });
        if (response.data && response.data.content) {
          orders.value = [...orders.value, ...response.data.content];
          hasMore.value = response.data.content.length === pageSize.value;
        }
      } catch (error) {
        common_vendor.index.__f__("error", "at subpkg/appointment-flow/03_AICareMatchPage.vue:260", "加载订单失败:", error);
        common_vendor.index.showToast({ title: "加载失败", icon: "none" });
      } finally {
        loading.value = false;
      }
    };
    const loadMoreOrders = () => {
      if (hasMore.value) {
        currentPage.value++;
        loadOrders();
      }
    };
    const onServiceTypeChange = (e) => {
      serviceTypeFilter.value = e.detail.value;
    };
    const onSortChange = (e) => {
      sortOption.value = e.detail.value;
    };
    const viewOrderDetail = (order) => {
      currentOrder.value = order;
      showOrderDetail.value = true;
    };
    const closeOrderDetail = () => {
      showOrderDetail.value = false;
    };
    const canAcceptOrder = (order) => {
      return order.orderStatus === 1;
    };
    const acceptOrder = async (order) => {
      try {
        const response = await utils_api.post(`/attendant/orders/${order.orderId}/accept`);
        if (response.data === "接单成功") {
          common_vendor.index.showToast({ title: "接单成功", icon: "success" });
          order.orderStatus = 2;
          closeOrderDetail();
        } else {
          common_vendor.index.showToast({ title: response.data || "接单失败", icon: "none" });
        }
      } catch (error) {
        common_vendor.index.__f__("error", "at subpkg/appointment-flow/03_AICareMatchPage.vue:313", "接单失败:", error);
        common_vendor.index.showToast({ title: "接单失败", icon: "none" });
      }
    };
    const acceptOrderFromDetail = () => {
      acceptOrder(currentOrder.value);
    };
    const formatDate = (date) => {
      if (!date)
        return "";
      return new Date(date).toLocaleString("zh-CN");
    };
    const getServiceTypeName = (type) => {
      const types = {
        1: "普通陪诊",
        2: "术后护理",
        3: "急诊陪同",
        4: "上门陪诊"
      };
      return types[type] || "未知类型";
    };
    const getOptionText = (option) => {
      const options = {
        1: "代取药",
        2: "代取报告",
        3: "需轮椅协助",
        4: "复查",
        5: "药物过敏"
      };
      return options[option] || "其他";
    };
    const getOrderStatusText = (status) => {
      const statuses = {
        1: "待接单",
        2: "已接单",
        3: "服务中",
        4: "待确认时长",
        5: "待支付差价",
        6: "已完成",
        7: "已取消"
      };
      return statuses[status] || "未知状态";
    };
    const getStatusClass = (status) => {
      const classes = {
        1: "status-waiting",
        2: "status-accepted",
        3: "status-service",
        4: "status-confirm",
        5: "status-balance",
        6: "status-completed",
        7: "status-cancelled"
      };
      return classes[status] || "";
    };
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_vendor.t(selectedServiceTypeName.value),
        b: serviceTypes,
        c: common_vendor.o(onServiceTypeChange),
        d: common_vendor.t(selectedSortName.value),
        e: sortOptions,
        f: common_vendor.o(onSortChange),
        g: loading.value
      }, loading.value ? {} : common_vendor.e({
        h: common_vendor.f(displayedOrders.value, (order, index, i0) => {
          return {
            a: common_vendor.t(order.orderNo),
            b: common_vendor.t(getOrderStatusText(order.orderStatus)),
            c: common_vendor.n(getStatusClass(order.orderStatus)),
            d: common_vendor.t(getServiceTypeName(order.clinicType)),
            e: common_vendor.t(order.hospital),
            f: common_vendor.t(formatDate(order.appointmentTime)),
            g: common_vendor.t(order.patientName),
            h: common_vendor.t(order.consultationDuration),
            i: common_vendor.t(order.orderAmount),
            j: common_vendor.o(($event) => viewOrderDetail(order), order.orderId),
            k: common_vendor.t(canAcceptOrder(order) ? "立即接单" : "不可接单"),
            l: !canAcceptOrder(order),
            m: common_vendor.o(($event) => acceptOrder(order), order.orderId),
            n: order.orderId
          };
        }),
        i: orders.value.length === 0 && !loading.value
      }, orders.value.length === 0 && !loading.value ? {} : {}, {
        j: hasMore.value && orders.value.length > 0
      }, hasMore.value && orders.value.length > 0 ? {
        k: common_vendor.o(loadMoreOrders)
      } : {}), {
        l: showOrderDetail.value
      }, showOrderDetail.value ? common_vendor.e({
        m: common_vendor.o(closeOrderDetail),
        n: common_vendor.t(currentOrder.value.orderNo),
        o: common_vendor.t(getServiceTypeName(currentOrder.value.clinicType)),
        p: common_vendor.t(currentOrder.value.hospital),
        q: common_vendor.t(formatDate(currentOrder.value.appointmentTime)),
        r: common_vendor.t(currentOrder.value.patientName),
        s: common_vendor.t(currentOrder.value.contactPhone),
        t: common_vendor.t(currentOrder.value.consultationDuration),
        v: common_vendor.t(currentOrder.value.unitPrice),
        w: common_vendor.t(currentOrder.value.orderAmount),
        x: common_vendor.t(currentOrder.value.depositAmount),
        y: currentOrder.value.selectedOptions && currentOrder.value.selectedOptions.length > 0
      }, currentOrder.value.selectedOptions && currentOrder.value.selectedOptions.length > 0 ? {
        z: common_vendor.f(currentOrder.value.selectedOptions, (option, index, i0) => {
          return {
            a: common_vendor.t(getOptionText(option)),
            b: index
          };
        })
      } : {}, {
        A: currentOrder.value.customRequirement
      }, currentOrder.value.customRequirement ? {
        B: common_vendor.t(currentOrder.value.customRequirement)
      } : {}, {
        C: common_vendor.t(canAcceptOrder(currentOrder.value) ? "立即接单" : "不可接单"),
        D: !canAcceptOrder(currentOrder.value),
        E: common_vendor.o(acceptOrderFromDetail)
      }) : {});
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-384c8980"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/subpkg/appointment-flow/03_AICareMatchPage.js.map

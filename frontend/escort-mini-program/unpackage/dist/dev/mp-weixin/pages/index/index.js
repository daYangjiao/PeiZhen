"use strict";
const common_vendor = require("../../common/vendor.js");
const common_assets = require("../../common/assets.js");
const utils_api = require("../../utils/api.js");
if (!Math) {
  OrderCard();
}
const OrderCard = () => "../../components/OrderCard.js";
const _sfc_main = {
  __name: "index",
  setup(__props) {
    const orderList = common_vendor.ref([]);
    const searchKeyword = common_vendor.ref("");
    const isLoading = common_vendor.ref(false);
    const isRefreshing = common_vendor.ref(false);
    const page = common_vendor.ref(0);
    const showFilterPopup = common_vendor.ref(false);
    const serviceTypeOptions = [
      { label: "全部", value: null },
      { label: "普通陪诊", value: 1 },
      { label: "术后护理", value: 2 },
      { label: "急诊陪同", value: 3 },
      { label: "上门陪诊", value: 4 }
    ];
    const durationOptions = [
      { label: "不限时长", value: null },
      { label: "大于两小时", value: 2 },
      { label: "大于三小时", value: 3 },
      { label: "大于四小时", value: 4 }
    ];
    const feeOptions = [
      { label: "不限价格", value: null },
      { label: "小于80元", value: 80 },
      { label: "小于100元", value: 100 },
      { label: "小于150元", value: 150 }
    ];
    const serviceTypeLabels = serviceTypeOptions.map((o) => o.label);
    const durationLabels = durationOptions.map((o) => o.label);
    const feeLabels = feeOptions.map((o) => o.label);
    const filterServiceTypeIndex = common_vendor.ref(0);
    const filterDurationIndex = common_vendor.ref(0);
    const filterFeeIndex = common_vendor.ref(0);
    const expandWhich = common_vendor.ref(null);
    const filterParams = common_vendor.ref({
      serviceType: null,
      expectedDurationMinHours: null,
      orderAmountMax: null
    });
    const filteredOrderList = common_vendor.computed(() => {
      const kw = (searchKeyword.value || "").trim().toLowerCase();
      if (!kw)
        return orderList.value;
      return orderList.value.filter((o) => {
        const hospital = (o.hospital || "").toLowerCase();
        const patient = (o.patientName || o.contactPerson || "").toLowerCase();
        const service = (o.serviceTypeName || o.serviceContent || "").toString().toLowerCase();
        const req = (o.specialRequirements || "").toLowerCase();
        const custom = (o.customRequirement || "").toLowerCase();
        return hospital.includes(kw) || patient.includes(kw) || service.includes(kw) || req.includes(kw) || custom.includes(kw);
      });
    });
    const handleSearch = () => {
    };
    const formatOrderData = (raw) => {
      const symptomDescription = raw.specialRequirements || "";
      const otherRequirement = raw.customRequirement && raw.customRequirement !== "无" ? raw.customRequirement : "";
      return {
        id: raw.orderId,
        orderId: raw.orderId,
        userName: raw.patientName || raw.contactPerson || "匿名患者",
        userAge: raw.patientAge || "--",
        userGender: raw.patientSex || "未知",
        userAvatar: "/static/user-placeholder.png",
        serviceType: raw.serviceContent || raw.serviceTypeName || "陪诊服务",
        hospitalName: raw.hospital || "未知医院",
        appointmentTime: (raw.serviceDate || "") + " " + (raw.serviceTimeSlot || ""),
        price: ((raw.orderAmount || 0) * 0.9).toFixed(2),
        symptomDescription,
        otherRequirement,
        phone: raw.contactPhone || raw.userPhone,
        orderStatus: raw.orderStatus
      };
    };
    const loadOrders = async (reset = false) => {
      if (isLoading.value)
        return;
      if (reset) {
        page.value = 0;
        orderList.value = [];
      }
      isLoading.value = true;
      try {
        const params = {
          page: page.value,
          size: 10
        };
        if (filterParams.value.serviceType != null)
          params.serviceType = filterParams.value.serviceType;
        if (filterParams.value.expectedDurationMinHours != null)
          params.expectedDurationMinHours = filterParams.value.expectedDurationMinHours;
        if (filterParams.value.orderAmountMax != null)
          params.orderAmountMax = filterParams.value.orderAmountMax;
        const res = await utils_api.get("/attendant/orders/waiting", params);
        if (res.code === 200 && res.data) {
          const newOrders = res.data.content || [];
          if (reset) {
            orderList.value = newOrders;
          } else {
            orderList.value = [...orderList.value, ...newOrders];
          }
          if (newOrders.length > 0)
            page.value++;
        }
      } catch (e) {
        common_vendor.index.__f__("error", "at pages/index/index.vue:225", "加载订单失败:", e);
      } finally {
        isLoading.value = false;
        isRefreshing.value = false;
      }
    };
    const onRefresh = () => {
      isRefreshing.value = true;
      loadOrders(true);
    };
    const refreshOrders = () => {
      onRefresh();
    };
    const loadMore = () => {
      loadOrders();
    };
    const handleAccept = (actionData) => {
      const order = actionData.data || actionData;
      const attendantInfo = common_vendor.index.getStorageSync("userInfo");
      if (!attendantInfo || !attendantInfo.id) {
        common_vendor.index.showToast({ title: "请先登录", icon: "none" });
        return;
      }
      common_vendor.index.showModal({
        title: "确认接单",
        content: `确定要接受该订单吗？`,
        success: async (res) => {
          if (res.confirm) {
            try {
              const response = await utils_api.post(`/attendant/orders/${order.id}/accept?attendantId=${attendantInfo.id}`);
              if (response.code === 200) {
                common_vendor.index.showToast({ title: "接单成功", icon: "success" });
                loadOrders(true);
                setTimeout(() => {
                  common_vendor.index.switchTab({ url: "/pages/order/order" });
                }, 1500);
              } else {
                let errorMsg = response.message || "接单失败";
                if (errorMsg.includes("订单不存在")) {
                  errorMsg = "订单已失效，请刷新页面";
                } else if (errorMsg.includes("无效的陪诊师ID")) {
                  errorMsg = "账号信息异常，请重新登录";
                } else if (errorMsg.includes("订单当前状态无法接单")) {
                  errorMsg = "订单状态异常，请稍后再试";
                } else if (errorMsg.includes("用户类型不支持接单")) {
                  errorMsg = "账号权限不足，请联系管理员";
                }
                common_vendor.index.showToast({ title: errorMsg, icon: "none" });
              }
            } catch (e) {
              common_vendor.index.__f__("error", "at pages/index/index.vue:290", "接单失败:", e);
              const msg = e.message || "接单失败，请稍后重试";
              common_vendor.index.showToast({ title: msg, icon: "none" });
            }
          }
        }
      });
    };
    const contactPatient = (order) => {
      if (order.phone) {
        common_vendor.index.makePhoneCall({ phoneNumber: order.phone });
      } else {
        common_vendor.index.showToast({ title: "暂无联系电话", icon: "none" });
      }
    };
    const closeFilterPopup = () => {
      showFilterPopup.value = false;
      expandWhich.value = null;
    };
    const toggleExpand = (which) => {
      expandWhich.value = expandWhich.value === which ? null : which;
    };
    const selectServiceType = (idx) => {
      filterServiceTypeIndex.value = idx;
      expandWhich.value = null;
    };
    const selectDuration = (idx) => {
      filterDurationIndex.value = idx;
      expandWhich.value = null;
    };
    const selectFee = (idx) => {
      filterFeeIndex.value = idx;
      expandWhich.value = null;
    };
    const resetFilter = () => {
      filterServiceTypeIndex.value = 0;
      filterDurationIndex.value = 0;
      filterFeeIndex.value = 0;
      expandWhich.value = null;
    };
    const confirmFilter = () => {
      filterParams.value = {
        serviceType: serviceTypeOptions[filterServiceTypeIndex.value].value,
        expectedDurationMinHours: durationOptions[filterDurationIndex.value].value,
        orderAmountMax: feeOptions[filterFeeIndex.value].value
      };
      showFilterPopup.value = false;
      expandWhich.value = null;
      loadOrders(true);
    };
    common_vendor.onMounted(() => {
      loadOrders(true);
    });
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_assets._imports_0,
        b: common_vendor.o(handleSearch),
        c: searchKeyword.value,
        d: common_vendor.o(($event) => searchKeyword.value = $event.detail.value),
        e: common_assets._imports_1,
        f: common_vendor.o(($event) => showFilterPopup.value = true),
        g: common_assets._imports_2$1,
        h: common_vendor.o(refreshOrders),
        i: isRefreshing.value ? 1 : "",
        j: showFilterPopup.value
      }, showFilterPopup.value ? common_vendor.e({
        k: common_vendor.t(common_vendor.unref(serviceTypeLabels)[filterServiceTypeIndex.value]),
        l: common_vendor.o(($event) => toggleExpand("service")),
        m: expandWhich.value === "service"
      }, expandWhich.value === "service" ? {
        n: common_vendor.f(serviceTypeOptions, (opt, idx, i0) => {
          return {
            a: common_vendor.t(opt.label),
            b: "s" + idx,
            c: filterServiceTypeIndex.value === idx ? 1 : "",
            d: common_vendor.o(($event) => selectServiceType(idx), "s" + idx)
          };
        })
      } : {}, {
        o: common_vendor.t(common_vendor.unref(durationLabels)[filterDurationIndex.value]),
        p: common_vendor.o(($event) => toggleExpand("duration")),
        q: expandWhich.value === "duration"
      }, expandWhich.value === "duration" ? {
        r: common_vendor.f(durationOptions, (opt, idx, i0) => {
          return {
            a: common_vendor.t(opt.label),
            b: "d" + idx,
            c: filterDurationIndex.value === idx ? 1 : "",
            d: common_vendor.o(($event) => selectDuration(idx), "d" + idx)
          };
        })
      } : {}, {
        s: common_vendor.t(common_vendor.unref(feeLabels)[filterFeeIndex.value]),
        t: common_vendor.o(($event) => toggleExpand("fee")),
        v: expandWhich.value === "fee"
      }, expandWhich.value === "fee" ? {
        w: common_vendor.f(feeOptions, (opt, idx, i0) => {
          return {
            a: common_vendor.t(opt.label),
            b: "f" + idx,
            c: filterFeeIndex.value === idx ? 1 : "",
            d: common_vendor.o(($event) => selectFee(idx), "f" + idx)
          };
        })
      } : {}, {
        x: common_vendor.o(resetFilter),
        y: common_vendor.o(confirmFilter),
        z: common_vendor.o(() => {
        }),
        A: common_vendor.o(closeFilterPopup)
      }) : {}, {
        B: !isLoading.value
      }, !isLoading.value ? common_vendor.e({
        C: filteredOrderList.value.length > 0
      }, filteredOrderList.value.length > 0 ? {
        D: common_vendor.f(filteredOrderList.value, (order, index, i0) => {
          return {
            a: order.orderId,
            b: common_vendor.o(contactPatient, order.orderId),
            c: common_vendor.o(handleAccept, order.orderId),
            d: "1cf27b2a-0-" + i0,
            e: common_vendor.p({
              ["order-data"]: formatOrderData(order),
              ["show-actions"]: true,
              ["action-type"]: "accept"
            })
          };
        })
      } : {
        E: common_assets._imports_2
      }) : {}, {
        F: isLoading.value
      }, isLoading.value ? {} : {}, {
        G: common_vendor.o(loadMore),
        H: isRefreshing.value,
        I: common_vendor.o(onRefresh)
      });
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-1cf27b2a"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/index/index.js.map

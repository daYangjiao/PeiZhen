"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_api = require("../../utils/api.js");
const _sfc_main = {
  __name: "05_PaymentSuccessPage",
  setup(__props) {
    const orderData = common_vendor.ref({});
    const payTime = common_vendor.ref("");
    const fetchOrderDetail = async (orderNo) => {
      try {
        common_vendor.index.__f__("log", "at subpkg/appointment-flow/05_PaymentSuccessPage.vue:93", "【PaymentSuccessPage】正在调用后端接口获取订单完整信息:", orderNo);
        const response = await utils_api.get(`/ai/guide/orders/${orderNo}/complete-info`);
        common_vendor.index.__f__("log", "at subpkg/appointment-flow/05_PaymentSuccessPage.vue:96", "【PaymentSuccessPage】后端返回的订单完整数据:", response);
        if (response && response.code === 200 && response.data) {
          orderData.value = response.data;
        } else {
          common_vendor.index.__f__("warn", "at subpkg/appointment-flow/05_PaymentSuccessPage.vue:101", "【PaymentSuccessPage】后端返回数据为空或格式不正确:", response);
          common_vendor.index.showToast({
            title: "获取订单信息失败",
            icon: "none"
          });
        }
      } catch (error) {
        common_vendor.index.__f__("error", "at subpkg/appointment-flow/05_PaymentSuccessPage.vue:108", "【PaymentSuccessPage】获取订单完整信息失败:", error);
        common_vendor.index.showToast({
          title: "网络错误，获取订单信息失败",
          icon: "none"
        });
      }
    };
    common_vendor.onLoad(async (options) => {
      common_vendor.index.__f__("log", "at subpkg/appointment-flow/05_PaymentSuccessPage.vue:118", "【PaymentSuccessPage】页面加载参数:", options);
      const now = /* @__PURE__ */ new Date();
      payTime.value = now.getFullYear() + "-" + String(now.getMonth() + 1).padStart(2, "0") + "-" + String(now.getDate()).padStart(2, "0") + " " + String(now.getHours()).padStart(2, "0") + ":" + String(now.getMinutes()).padStart(2, "0") + ":" + String(now.getSeconds()).padStart(2, "0");
      const orderNo = options.orderNo || "";
      if (!orderNo) {
        common_vendor.index.showToast({ title: "订单号错误", icon: "none" });
        common_vendor.index.redirectTo({ url: "/pages/index/index" });
        return;
      }
      await fetchOrderDetail(orderNo);
    });
    const formatAmount = (amount) => {
      const num = Number(amount);
      if (isNaN(num))
        return "0.00";
      return num.toFixed(2);
    };
    const getSymptomDescription = () => {
      const { symptoms } = orderData.value;
      if (!symptoms) {
        return "无";
      }
      if (Array.isArray(symptoms)) {
        const validSymptoms = symptoms.filter((s) => s && s.trim() && s !== "无" && s !== "null");
        return validSymptoms.length > 0 ? validSymptoms.join(", ") : "无";
      }
      return symptoms;
    };
    const goToOrderDetail = () => {
      if (!orderData.value.orderNo) {
        common_vendor.index.showToast({ title: "无法跳转：缺少订单号", icon: "none" });
        return;
      }
      const targetOrderNo = encodeURIComponent(orderData.value.orderNo);
      common_vendor.index.reLaunch({
        url: "/pages/order/order",
        success() {
          setTimeout(() => {
            common_vendor.index.navigateTo({
              url: `/pages/OrderDetailPage/OrderDetailPage?orderNo=${targetOrderNo}`
            });
          }, 150);
        }
      });
    };
    return (_ctx, _cache) => {
      return {
        a: common_vendor.o(goToOrderDetail),
        b: common_vendor.t(orderData.value.orderNo),
        c: common_vendor.t(formatAmount(orderData.value.totalPrice)),
        d: common_vendor.t(orderData.value.paymentTime || payTime.value),
        e: common_vendor.t(orderData.value.orderStatusDesc),
        f: common_vendor.t(orderData.value.paymentStatusDesc),
        g: common_vendor.t(orderData.value.serviceDate),
        h: common_vendor.t(orderData.value.serviceTime),
        i: common_vendor.t(orderData.value.hospital),
        j: common_vendor.t(orderData.value.patientName),
        k: common_vendor.t(orderData.value.patientPhone),
        l: common_vendor.t(orderData.value.serviceTypeName),
        m: common_vendor.t(getSymptomDescription()),
        n: common_vendor.t(orderData.value.otherRequirement || "无")
      };
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-2d299dfd"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/subpkg/appointment-flow/05_PaymentSuccessPage.js.map

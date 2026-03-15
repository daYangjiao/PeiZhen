"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_api = require("../../utils/api.js");
const _sfc_main = {
  __name: "04_OrderConfirmPage",
  setup(__props) {
    const orderData = common_vendor.ref({});
    common_vendor.ref(false);
    const payMethod = common_vendor.ref("wechat");
    const isLoading = common_vendor.ref(true);
    const showBillingRules = common_vendor.ref(false);
    const showPaymentModal = common_vendor.ref(false);
    const orderNo = common_vendor.ref("");
    common_vendor.onLoad((options) => {
      common_vendor.index.__f__("log", "at subpkg/appointment-flow/04_OrderConfirmPage.vue:244", "【OrderConfirmPage】页面加载参数:", options);
      if (options && options.orderNo) {
        orderNo.value = options.orderNo;
        common_vendor.index.__f__("log", "at subpkg/appointment-flow/04_OrderConfirmPage.vue:247", "【OrderConfirmPage】订单号:", orderNo.value);
        fetchOrderDetail(orderNo.value);
      } else {
        common_vendor.index.__f__("error", "at subpkg/appointment-flow/04_OrderConfirmPage.vue:250", "【OrderConfirmPage】缺少订单号参数");
        common_vendor.index.showToast({
          title: "订单号错误",
          icon: "none"
        });
        common_vendor.index.redirectTo({
          url: "/pages/index/index"
        });
      }
    });
    const fetchOrderDetail = async (orderNo2) => {
      isLoading.value = true;
      try {
        common_vendor.index.__f__("log", "at subpkg/appointment-flow/04_OrderConfirmPage.vue:268", "【OrderConfirmPage】正在调用后端接口获取订单详情:", orderNo2);
        const response = await utils_api.get(`/ai/guide/orders/${orderNo2}/complete-info`);
        common_vendor.index.__f__("log", "at subpkg/appointment-flow/04_OrderConfirmPage.vue:271", "【OrderConfirmPage】后端返回的订单数据:", response);
        if (response && response.code === 200 && response.data) {
          orderData.value = response.data;
        } else {
          common_vendor.index.__f__("warn", "at subpkg/appointment-flow/04_OrderConfirmPage.vue:276", "【OrderConfirmPage】后端返回数据为空或格式不正确:", response);
          common_vendor.index.showToast({
            title: "获取订单信息失败",
            icon: "none"
          });
        }
      } catch (error) {
        common_vendor.index.__f__("error", "at subpkg/appointment-flow/04_OrderConfirmPage.vue:283", "【OrderConfirmPage】获取订单详情失败:", error);
        common_vendor.index.showToast({
          title: "网络错误，获取订单信息失败",
          icon: "none"
        });
      } finally {
        isLoading.value = false;
      }
    };
    const onPaymentChange = (e) => {
      payMethod.value = e.detail.value;
    };
    const confirmPay = () => {
      showPaymentModal.value = true;
    };
    const handlePaymentResult = async (isPaid) => {
      showPaymentModal.value = false;
      const paymentStatus = isPaid ? 1 : 0;
      try {
        const response = await utils_api.post("/ai/guide/payments/status", {
          orderNo: orderNo.value,
          paymentStatus
        });
        common_vendor.index.__f__("log", "at subpkg/appointment-flow/04_OrderConfirmPage.vue:322", "【OrderConfirmPage】支付状态更新响应:", response);
        if (isPaid) {
          common_vendor.index.showToast({ title: "支付成功，跳转中...", icon: "none" });
          setTimeout(() => {
            const o = encodeURIComponent(orderNo.value);
            common_vendor.index.reLaunch({
              url: "/pages/order/order",
              success() {
                setTimeout(() => {
                  common_vendor.index.navigateTo({
                    url: `/subpkg/appointment-flow/05_PaymentSuccessPage?orderNo=${o}`
                  });
                }, 150);
              }
            });
          }, 1e3);
        } else {
          common_vendor.index.showToast({ title: "支付未完成", icon: "none" });
          setTimeout(() => {
            common_vendor.index.navigateTo({
              url: `/subpkg/appointment-flow/PaymentFailedPage?orderNo=${encodeURIComponent(orderNo.value)}`
            });
          }, 1e3);
        }
      } catch (error) {
        common_vendor.index.__f__("error", "at subpkg/appointment-flow/04_OrderConfirmPage.vue:349", "【调用支付状态接口失败】", error);
        if (isPaid) {
          const o = encodeURIComponent(orderNo.value);
          common_vendor.index.reLaunch({
            url: "/pages/order/order",
            success() {
              setTimeout(() => {
                common_vendor.index.navigateTo({
                  url: `/subpkg/appointment-flow/05_PaymentSuccessPage?orderNo=${o}`
                });
              }, 150);
            }
          });
        } else {
          common_vendor.index.navigateTo({
            url: `/subpkg/appointment-flow/PaymentFailedPage?orderNo=${encodeURIComponent(orderNo.value)}`
          });
        }
      }
    };
    const formatAmount = (amount) => {
      const num = Number(amount);
      if (isNaN(num)) {
        return "0.00";
      }
      return num.toFixed(2);
    };
    const getSymptomDescription = () => {
      const { symptoms } = orderData.value;
      if (!symptoms) {
        return "未提供症状信息";
      }
      if (Array.isArray(symptoms)) {
        const validSymptoms = symptoms.filter((s) => s && s.trim() && s !== "无" && s !== "null");
        return validSymptoms.length > 0 ? validSymptoms.join(", ") : "未提供症状信息";
      }
      return symptoms;
    };
    const formatDate = (dateStr) => {
      if (!dateStr || typeof dateStr !== "string")
        return "未知";
      return dateStr;
    };
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: isLoading.value
      }, isLoading.value ? {} : {
        b: common_vendor.t(orderData.value.hospital || "未知"),
        c: common_vendor.t(formatDate(orderData.value.serviceDate)),
        d: common_vendor.t(orderData.value.serviceTime || "未知"),
        e: common_vendor.t(orderData.value.patientName || "未知"),
        f: common_vendor.t(getSymptomDescription()),
        g: common_vendor.t(orderData.value.otherRequirement || "无")
      }, {
        h: !isLoading.value
      }, !isLoading.value ? {
        i: common_vendor.o(($event) => showBillingRules.value = true),
        j: common_vendor.t(formatAmount(orderData.value.totalPrice)),
        k: common_vendor.t(formatAmount(orderData.value.totalPrice))
      } : {}, {
        l: !isLoading.value
      }, !isLoading.value ? {
        m: payMethod.value === "wechat",
        n: payMethod.value === "alipay",
        o: payMethod.value === "unionpay",
        p: common_vendor.o(onPaymentChange),
        q: payMethod.value
      } : {}, {
        r: !isLoading.value
      }, !isLoading.value ? {
        s: common_vendor.t(formatAmount(orderData.value.totalPrice)),
        t: common_vendor.o(confirmPay)
      } : {}, {
        v: showBillingRules.value
      }, showBillingRules.value ? {
        w: common_vendor.o(($event) => showBillingRules.value = false),
        x: common_vendor.o(($event) => showBillingRules.value = false),
        y: common_vendor.o(() => {
        }),
        z: common_vendor.o(($event) => showBillingRules.value = false)
      } : {}, {
        A: showPaymentModal.value
      }, showPaymentModal.value ? {
        B: common_vendor.o(($event) => handlePaymentResult(true)),
        C: common_vendor.o(($event) => handlePaymentResult(false)),
        D: common_vendor.o(() => {
        }),
        E: common_vendor.o(($event) => showPaymentModal.value = false)
      } : {});
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-b4ebde95"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/subpkg/appointment-flow/04_OrderConfirmPage.js.map

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
      common_vendor.index.__f__("log", "at pages/AItriage/04_OrderConfirmPage.vue:297", "【OrderConfirmPage】页面加载参数:", options);
      if (options && options.orderNo) {
        orderNo.value = options.orderNo;
        common_vendor.index.__f__("log", "at pages/AItriage/04_OrderConfirmPage.vue:300", "【OrderConfirmPage】订单号:", orderNo.value);
        fetchOrderDetail(orderNo.value);
      } else {
        common_vendor.index.__f__("error", "at pages/AItriage/04_OrderConfirmPage.vue:303", "【OrderConfirmPage】缺少订单号参数");
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
        common_vendor.index.__f__("log", "at pages/AItriage/04_OrderConfirmPage.vue:322", "【OrderConfirmPage】正在调用后端接口获取订单详情:", orderNo2);
        const response = await utils_api.get(`/ai/guide/orders/${orderNo2}`);
        common_vendor.index.__f__("log", "at pages/AItriage/04_OrderConfirmPage.vue:325", "【OrderConfirmPage】后端返回的订单数据:", response.data);
        if (response && response.data) {
          orderData.value = response.data;
          if (response.data.payMethod) {
            payMethod.value = response.data.payMethod;
          }
        } else {
          common_vendor.index.__f__("warn", "at pages/AItriage/04_OrderConfirmPage.vue:337", "【OrderConfirmPage】后端返回数据为空或格式不正确:", response);
          common_vendor.index.showToast({
            title: "获取订单信息失败",
            icon: "none"
          });
        }
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/AItriage/04_OrderConfirmPage.vue:344", "【OrderConfirmPage】获取订单详情失败:", error);
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
      common_vendor.index.__f__("log", "at pages/AItriage/04_OrderConfirmPage.vue:360", "【支付方式变更】", payMethod.value);
    };
    const confirmPay = () => {
      common_vendor.index.__f__("log", "at pages/AItriage/04_OrderConfirmPage.vue:367", "【开始支付流程】", {
        orderNo: orderNo.value,
        paymentMethod: payMethod.value
      });
      showPaymentModal.value = true;
    };
    const handlePaymentResult = async (isPaid) => {
      showPaymentModal.value = false;
      const paymentStatus = isPaid ? 1 : 0;
      common_vendor.index.__f__("log", "at pages/AItriage/04_OrderConfirmPage.vue:386", `【调用支付状态接口】${isPaid ? "支付成功" : "支付失败"}，参数:`, { orderNo: orderNo.value, paymentStatus });
      try {
        const response = await utils_api.post("/ai/guide/payments/status", {
          orderNo: orderNo.value,
          paymentStatus
        });
        common_vendor.index.__f__("log", "at pages/AItriage/04_OrderConfirmPage.vue:394", "【支付状态接口返回】", response);
        common_vendor.index.__f__("log", "at pages/AItriage/04_OrderConfirmPage.vue:396", "【支付状态】已支付成功 (API 调用成功，或错误处理已判断为成功)");
        common_vendor.index.showToast({
          title: "支付成功，跳转中...",
          icon: "none"
        });
        setTimeout(() => {
          common_vendor.index.navigateTo({
            url: `/pages/AItriage/05_PaymentSuccessPage?orderNo=${encodeURIComponent(orderNo.value)}`
          });
        }, 1e3);
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/AItriage/04_OrderConfirmPage.vue:410", "【调用支付状态接口失败】", error);
        if (error === 1) {
          common_vendor.index.__f__("log", "at pages/AItriage/04_OrderConfirmPage.vue:414", "【检测到后端返回 1 作为成功标志】");
          common_vendor.index.__f__("log", "at pages/AItriage/04_OrderConfirmPage.vue:415", "【支付状态】已支付成功 (API 返回成功)");
          common_vendor.index.showToast({
            title: "支付成功，跳转中...",
            icon: "none"
          });
          setTimeout(() => {
            common_vendor.index.navigateTo({
              url: `/pages/AItriage/05_PaymentSuccessPage?orderNo=${encodeURIComponent(orderNo.value)}`
            });
          }, 1e3);
        } else {
          common_vendor.index.__f__("error", "at pages/AItriage/04_OrderConfirmPage.vue:428", "【实际错误】", error);
          common_vendor.index.showToast({
            title: "网络错误，请稍后再试",
            icon: "none"
          });
        }
      }
    };
    const formatAmount = (amountInCents) => {
      const amount = Number(amountInCents);
      if (isNaN(amount)) {
        return "0.00";
      }
      return amount.toFixed(2);
    };
    const getImagePath = (avatarFileName) => {
      if (!avatarFileName || typeof avatarFileName !== "string") {
        common_vendor.index.__f__("warn", "at pages/AItriage/04_OrderConfirmPage.vue:464", "【getImagePath】无效的头像路径:", avatarFileName);
        const normalizedBaseUrl2 = utils_api.config.baseURL.endsWith("/") ? utils_api.config.baseURL : utils_api.config.baseURL + "/";
        return normalizedBaseUrl2 + "static/default_avatar.png";
      }
      if (avatarFileName.startsWith("http://") || avatarFileName.startsWith("https://")) {
        common_vendor.index.__f__("log", "at pages/AItriage/04_OrderConfirmPage.vue:472", "【getImagePath】返回绝对 URL:", avatarFileName);
        return avatarFileName;
      }
      let photoUrl = avatarFileName;
      if (photoUrl.startsWith("/")) {
        photoUrl = photoUrl.substring(1);
      }
      const normalizedBaseUrl = utils_api.config.baseURL.endsWith("/") ? utils_api.config.baseURL : utils_api.config.baseURL + "/";
      const fullPath = normalizedBaseUrl + photoUrl;
      common_vendor.index.__f__("log", "at pages/AItriage/04_OrderConfirmPage.vue:487", "【getImagePath】拼接后的完整路径:", fullPath);
      return fullPath;
    };
    const getSymptomDescription = () => {
      const { symptoms, otherRequirement } = orderData.value;
      const parts = [];
      if (symptoms && Array.isArray(symptoms) && symptoms.length > 0) {
        parts.push(...symptoms);
      }
      return parts.join(", ");
    };
    const formatDate = (dateStr) => {
      if (!dateStr || typeof dateStr !== "string")
        return "未知";
      try {
        const date = new Date(dateStr);
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, "0");
        const day = String(date.getDate()).padStart(2, "0");
        return `${year}年${month}月${day}日`;
      } catch (e) {
        common_vendor.index.__f__("error", "at pages/AItriage/04_OrderConfirmPage.vue:525", "【格式化日期错误】", e);
        return "未知";
      }
    };
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: isLoading.value
      }, isLoading.value ? {} : common_vendor.e({
        b: common_vendor.t(orderData.value.hospital || "未知"),
        c: common_vendor.t(formatDate(orderData.value.serviceDate)),
        d: common_vendor.t(orderData.value.serviceTime || "未知"),
        e: common_vendor.t(orderData.value.patientName || "未知"),
        f: common_vendor.t(getSymptomDescription() || "无"),
        g: orderData.value.otherRequirement
      }, orderData.value.otherRequirement ? {
        h: common_vendor.t(orderData.value.otherRequirement)
      } : {}), {
        i: !isLoading.value
      }, !isLoading.value ? {
        j: getImagePath(orderData.value.attendantAvatar),
        k: common_vendor.t(orderData.value.attendantName || "未知陪诊师"),
        l: common_vendor.t(orderData.value.attendantIntroduction || "暂无简介")
      } : {}, {
        m: !isLoading.value
      }, !isLoading.value ? {
        n: common_vendor.o(($event) => showBillingRules.value = true),
        o: common_vendor.t(formatAmount(orderData.value.totalPrice)),
        p: common_vendor.t(formatAmount(orderData.value.totalPrice))
      } : {}, {
        q: !isLoading.value
      }, !isLoading.value ? {
        r: payMethod.value === "wechat",
        s: payMethod.value === "alipay",
        t: payMethod.value === "unionpay",
        v: common_vendor.o(onPaymentChange),
        w: payMethod.value
      } : {}, {
        x: !isLoading.value
      }, !isLoading.value ? {
        y: common_vendor.t(formatAmount(orderData.value.totalPrice)),
        z: common_vendor.o(confirmPay)
      } : {}, {
        A: showBillingRules.value
      }, showBillingRules.value ? {
        B: common_vendor.o(($event) => showBillingRules.value = false),
        C: common_vendor.o(($event) => showBillingRules.value = false),
        D: common_vendor.o(() => {
        }),
        E: common_vendor.o(($event) => showBillingRules.value = false)
      } : {}, {
        F: showPaymentModal.value
      }, showPaymentModal.value ? {
        G: common_vendor.o(($event) => handlePaymentResult(true)),
        H: common_vendor.o(($event) => handlePaymentResult(false)),
        I: common_vendor.o(() => {
        }),
        J: common_vendor.o(($event) => showPaymentModal.value = false)
      } : {});
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-fa941edb"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/AItriage/04_OrderConfirmPage.js.map

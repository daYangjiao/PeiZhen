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
        common_vendor.index.__f__("log", "at pages/AItriage/05_PaymentSuccessPage.vue:100", "【PaymentSuccessPage】正在调用后端接口获取订单完整信息:", orderNo);
        const response = await utils_api.get(`/ai/guide/orders/${orderNo}/complete-info`);
        common_vendor.index.__f__("log", "at pages/AItriage/05_PaymentSuccessPage.vue:103", "【PaymentSuccessPage】后端返回的订单完整数据:", response.data);
        if (response && response.data) {
          orderData.value = response.data;
        } else {
          common_vendor.index.__f__("warn", "at pages/AItriage/05_PaymentSuccessPage.vue:109", "【PaymentSuccessPage】后端返回数据为空或格式不正确:", response);
          common_vendor.index.showToast({
            title: "获取订单信息失败",
            icon: "none"
          });
        }
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/AItriage/05_PaymentSuccessPage.vue:116", "【PaymentSuccessPage】获取订单完整信息失败:", error);
        common_vendor.index.showToast({
          title: "网络错误，获取订单信息失败",
          icon: "none"
        });
      }
    };
    common_vendor.onLoad(async (options) => {
      common_vendor.index.__f__("log", "at pages/AItriage/05_PaymentSuccessPage.vue:126", "【PaymentSuccessPage】页面加载参数:", options);
      const now = /* @__PURE__ */ new Date();
      payTime.value = now.getFullYear() + "-" + String(now.getMonth() + 1).padStart(2, "0") + "-" + String(now.getDate()).padStart(2, "0") + " " + String(now.getHours()).padStart(2, "0") + ":" + String(now.getMinutes()).padStart(2, "0") + ":" + String(now.getSeconds()).padStart(2, "0");
      const orderNo = options.orderNo || "";
      common_vendor.index.__f__("log", "at pages/AItriage/05_PaymentSuccessPage.vue:138", "【PaymentSuccessPage】订单号:", orderNo);
      if (!orderNo) {
        common_vendor.index.__f__("error", "at pages/AItriage/05_PaymentSuccessPage.vue:141", "【PaymentSuccessPage】缺少订单号参数");
        common_vendor.index.showToast({
          title: "订单号错误",
          icon: "none"
        });
        common_vendor.index.redirectTo({
          url: "/pages/index/index"
        });
        return;
      }
      await fetchOrderDetail(orderNo);
      let existingOrders = common_vendor.index.getStorageSync("all_orders") || [];
      common_vendor.index.__f__("log", "at pages/AItriage/05_PaymentSuccessPage.vue:159", "【调试】当前已存在的订单列表:", existingOrders);
      const serviceTimeParts = orderData.value.serviceTime ? orderData.value.serviceTime.split(" ") : [];
      const serviceDate = serviceTimeParts[0] || "未知日期";
      const startTime = serviceTimeParts[1] || "未知时间";
      const formattedPrice = `¥${formatAmount(orderData.value.totalPrice)}`;
      const attendantAvatarToUse = orderData.value.attendantAvatar || "/static/default-avatar.jpg";
      const orderForList = {
        id: orderData.value.orderNo,
        // 使用订单号作为唯一ID
        orderId: orderData.value.orderNo,
        // 与 OrderDetailPage 兼容
        serviceType: orderData.value.serviceTypeName || "普通陪诊",
        hospitalName: orderData.value.hospital || "未知医院",
        serviceDate,
        // 提取日期部分
        startTime,
        // 提取时间部分
        status: orderData.value.orderStatusDesc === "待接单" ? "assigned" : "pending",
        // <--- 修改：根据订单状态设置
        price: formattedPrice,
        // 确保格式正确
        attendantAvatar: attendantAvatarToUse,
        // <--- 关键：使用确定的头像路径
        doctorAvatar: attendantAvatarToUse,
        // 兼容旧字段，如果 order.vue 仍在使用
        attendantName: orderData.value.attendantName,
        // 新增：陪诊师姓名
        doctorName: orderData.value.attendantName || "未知陪诊师",
        // 兼容旧字段
        rating: orderData.value.attendantScore || 5,
        // 假设默认评分
        // 添加其他可能需要的字段
        amount: orderData.value.totalPrice,
        // 以分为单位的金额
        attendantPhone: orderData.value.attendantPhone,
        // 陪诊师电话
        contactPerson: orderData.value.patientName,
        // 就诊人
        payTime: orderData.value.createTime || payTime.value,
        // 支付时间 (使用创建时间)
        paymentMethod: "微信支付"
        // 支付方式
      };
      const exists = existingOrders.some((order) => order.id === orderForList.id);
      if (!exists) {
        existingOrders.unshift(orderForList);
        common_vendor.index.__f__("log", "at pages/AItriage/05_PaymentSuccessPage.vue:201", "【调试】添加新订单到列表:", orderForList);
      } else {
        common_vendor.index.__f__("log", "at pages/AItriage/05_PaymentSuccessPage.vue:203", "【调试】订单已存在，未添加:", orderForList.id);
        const index = existingOrders.findIndex((order) => order.id === orderForList.id);
        if (index !== -1) {
          const currentStatus = existingOrders[index].status;
          if (currentStatus === "pending" || currentStatus === "completed") {
            existingOrders[index] = {
              ...existingOrders[index],
              ...orderForList
            };
            common_vendor.index.__f__("log", "at pages/AItriage/05_PaymentSuccessPage.vue:217", "【调试】已更新现有订单状态:", orderForList.id);
          } else {
            const { status: _, ...orderForListWithoutStatus } = orderForList;
            existingOrders[index] = {
              ...existingOrders[index],
              ...orderForListWithoutStatus
            };
            common_vendor.index.__f__("log", "at pages/AItriage/05_PaymentSuccessPage.vue:225", "【调试】已更新现有订单其他信息，保留原状态:", orderForList.id, currentStatus);
          }
        }
      }
      common_vendor.index.setStorageSync("all_orders", existingOrders);
      common_vendor.index.__f__("log", "at pages/AItriage/05_PaymentSuccessPage.vue:232", "【调试】已保存更新后的订单列表:", existingOrders);
    });
    const formatAmount = (amountInCents) => {
      const amount = Number(amountInCents);
      if (isNaN(amount)) {
        return "0.00";
      }
      return amount.toFixed(2);
    };
    const goToOrderDetail = () => {
      if (orderData.value.orderNo) {
        common_vendor.index.redirectTo({
          url: `/pages/OrderDetailPage/OrderDetailPage?orderNo=${encodeURIComponent(orderData.value.orderNo)}`
        });
      } else {
        common_vendor.index.showToast({ title: "无法跳转：缺少订单号", icon: "none" });
      }
    };
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_vendor.o(goToOrderDetail),
        b: common_vendor.t(orderData.value.orderNo),
        c: common_vendor.t(formatAmount(orderData.value.totalPrice)),
        d: common_vendor.t(payTime.value),
        e: common_vendor.t(orderData.value.orderStatusDesc),
        f: common_vendor.t(orderData.value.paymentStatusDesc),
        g: common_vendor.t(orderData.value.attendantName),
        h: common_vendor.t(orderData.value.attendantPhone),
        i: common_vendor.t(orderData.value.serviceDate),
        j: common_vendor.t(orderData.value.serviceTime),
        k: common_vendor.t(orderData.value.hospital),
        l: common_vendor.t(orderData.value.patientName),
        m: common_vendor.t(orderData.value.patientPhone),
        n: common_vendor.t(orderData.value.serviceTypeName),
        o: orderData.value.symptoms && orderData.value.symptoms.length
      }, orderData.value.symptoms && orderData.value.symptoms.length ? {
        p: common_vendor.t(orderData.value.symptoms.join(", "))
      } : {}, {
        q: common_vendor.t(orderData.value.otherRequirement || "无")
      });
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-1e90e525"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/AItriage/05_PaymentSuccessPage.js.map

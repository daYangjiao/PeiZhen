"use strict";
const common_vendor = require("../../common/vendor.js");
const _sfc_main = {
  data() {
    return {
      orderData: {
        amount: 0,
        attendantName: "",
        attendantPhone: "",
        // 新增：陪诊师电话
        attendantAvatar: "",
        // 新增：陪诊师头像 (如果需要保存)
        hospital: "",
        orderNo: "",
        qrCodeBase64: "",
        serviceTime: "",
        contactPerson: "",
        contactPhone: "",
        selectedServices: [],
        specialRequirements: "",
        serviceType: "普通陪诊",
        payTime: (/* @__PURE__ */ new Date()).toLocaleString(),
        paymentMethod: "微信支付",
        // 从 OrderConfirmPage 传递过来的 avatarUrl
        avatarUrl: ""
      },
      payTime: "",
      // 动态支付时间
      qrCodeSrc: ""
      // 二维码图片 src
    };
  },
  onLoad(options) {
    const now = /* @__PURE__ */ new Date();
    this.payTime = now.getFullYear() + "-" + String(now.getMonth() + 1).padStart(2, "0") + "-" + String(now.getDate()).padStart(2, "0") + " " + String(now.getHours()).padStart(2, "0") + ":" + String(now.getMinutes()).padStart(2, "0") + ":" + String(now.getSeconds()).padStart(2, "0");
    let storedData = null;
    try {
      storedData = common_vendor.index.getStorageSync("orderConfirmData");
    } catch (e) {
      common_vendor.index.__f__("warn", "at pages/PaymentSuccessPage/PaymentSuccessPage.vue:131", "从 Storage 读取订单数据失败", e);
    }
    if (!storedData || !storedData.orderNo) {
      const orderNo = options.orderNo || "PZ202311150001";
      storedData = {
        orderNo,
        amount: 19800,
        // 示例值（单位：分）
        attendantName: "王丽",
        attendantPhone: "13800138000",
        // 示例值：陪诊师电话
        attendantAvatar: "/static/default-avatar.jpg",
        // 示例值：陪诊师头像
        avatarUrl: "/static/default-avatar.jpg",
        // 示例值：avatarUrl
        hospital: "北京协和医院",
        serviceTime: "2023年11月15日 上午9:30",
        contactPerson: "张三",
        contactPhone: "13800138001",
        selectedServices: ["consult", "exam"],
        specialRequirements: "请协助取药",
        qrCodeBase64: "",
        serviceType: "普通陪诊",
        payTime: this.payTime,
        paymentMethod: "微信支付"
      };
    }
    this.orderData = { ...storedData };
    const orderKey = `order_${storedData.orderNo}`;
    const updatedStoredData = { ...storedData };
    common_vendor.index.setStorageSync(orderKey, updatedStoredData);
    common_vendor.index.setStorageSync("orderConfirmData", updatedStoredData);
    this.updateQrCode();
    let existingOrders = common_vendor.index.getStorageSync("all_orders") || [];
    common_vendor.index.__f__("log", "at pages/PaymentSuccessPage/PaymentSuccessPage.vue:176", "【调试】当前已存在的订单列表:", existingOrders);
    const serviceTimeParts = storedData.serviceTime ? storedData.serviceTime.split(" ") : [];
    const serviceDate = serviceTimeParts[0] || "未知日期";
    const startTime = serviceTimeParts[1] || "未知时间";
    const formattedPrice = `¥${this.formatAmount(storedData.amount)}`;
    const attendantAvatarToUse = storedData.avatarUrl || storedData.attendantAvatar || "/static/default-avatar.jpg";
    const orderForList = {
      id: storedData.orderNo,
      // 使用订单号作为唯一ID
      orderId: storedData.orderNo,
      // 与 OrderDetailPage 兼容
      serviceType: storedData.serviceType || "普通陪诊",
      hospitalName: storedData.hospital || "未知医院",
      serviceDate,
      // 提取日期部分
      startTime,
      // 提取时间部分
      status: "assigned",
      // <--- 修改：支付成功后，订单状态应为 'assigned' (待接单)
      price: formattedPrice,
      // 确保格式正确
      attendantAvatar: attendantAvatarToUse,
      // <--- 关键：使用确定的头像路径
      doctorAvatar: attendantAvatarToUse,
      // 兼容旧字段，如果 order.vue 仍在使用
      attendantName: storedData.attendantName,
      // 新增：陪诊师姓名
      doctorName: storedData.attendantName || storedData.doctorName || "未知陪诊师",
      // 兼容旧字段
      rating: 5,
      // 假设默认评分
      // 添加其他可能需要的字段
      amount: storedData.amount,
      // 以分为单位的金额
      attendantPhone: storedData.attendantPhone,
      // 陪诊师电话
      contactPerson: storedData.contactPerson,
      // 就诊人
      payTime: storedData.payTime || this.payTime,
      // 支付时间
      paymentMethod: storedData.paymentMethod || "微信支付"
      // 支付方式
    };
    const exists = existingOrders.some((order) => order.id === orderForList.id);
    if (!exists) {
      existingOrders.unshift(orderForList);
      common_vendor.index.__f__("log", "at pages/PaymentSuccessPage/PaymentSuccessPage.vue:218", "【调试】添加新订单到列表:", orderForList);
    } else {
      common_vendor.index.__f__("log", "at pages/PaymentSuccessPage/PaymentSuccessPage.vue:220", "【调试】订单已存在，未添加:", orderForList.id);
      const index = existingOrders.findIndex((order) => order.id === orderForList.id);
      if (index !== -1) {
        const currentStatus = existingOrders[index].status;
        if (currentStatus === "pending" || currentStatus === "completed") {
          existingOrders[index] = {
            ...existingOrders[index],
            ...orderForList
          };
          common_vendor.index.__f__("log", "at pages/PaymentSuccessPage/PaymentSuccessPage.vue:234", "【调试】已更新现有订单状态:", orderForList.id);
        } else {
          const { status: _, ...orderForListWithoutStatus } = orderForList;
          existingOrders[index] = {
            ...existingOrders[index],
            ...orderForListWithoutStatus
          };
          common_vendor.index.__f__("log", "at pages/PaymentSuccessPage/PaymentSuccessPage.vue:242", "【调试】已更新现有订单其他信息，保留原状态:", orderForList.id, currentStatus);
        }
      }
    }
    common_vendor.index.setStorageSync("all_orders", existingOrders);
    common_vendor.index.__f__("log", "at pages/PaymentSuccessPage/PaymentSuccessPage.vue:249", "【调试】已保存更新后的订单列表:", existingOrders);
  },
  methods: {
    formatAmount(amount) {
      if (typeof amount !== "number" || isNaN(amount)) {
        return "0.00";
      }
      return (amount / 100).toFixed(2);
    },
    updateQrCode() {
      const qrContent = JSON.stringify({
        orderNo: this.orderData.orderNo,
        attendantName: this.orderData.attendantName,
        attendantPhone: this.orderData.attendantPhone,
        hospital: this.orderData.hospital,
        serviceTime: this.orderData.serviceTime,
        contactPerson: this.orderData.contactPerson,
        contactPhone: this.orderData.contactPhone,
        serviceType: "普通陪诊",
        specialRequirements: this.orderData.specialRequirements,
        amount: this.formatAmount(this.orderData.amount)
      });
      this.qrCodeSrc = `https://api.qrserver.com/v1/create-qr-code/?data=${encodeURIComponent(qrContent)}&size=200x200`;
      common_vendor.index.__f__("log", "at pages/PaymentSuccessPage/PaymentSuccessPage.vue:279", "【调试】生成的二维码内容:", qrContent);
    },
    handleQrError() {
      common_vendor.index.__f__("warn", "at pages/PaymentSuccessPage/PaymentSuccessPage.vue:282", "二维码加载失败，尝试刷新");
    },
    saveQRCode() {
      common_vendor.index.downloadFile({
        url: this.qrCodeSrc,
        success: (res) => {
          if (res.statusCode === 200) {
            common_vendor.index.saveImageToPhotosAlbum({
              filePath: res.tempFilePath,
              success: () => {
                common_vendor.index.showToast({ title: "已保存到相册" });
              },
              fail: () => {
                common_vendor.index.showToast({ title: "保存失败，请允许相册权限", icon: "none" });
              }
            });
          } else {
            common_vendor.index.showToast({ title: "下载失败", icon: "none" });
          }
        },
        fail: () => {
          common_vendor.index.showToast({ title: "下载二维码失败", icon: "none" });
        }
      });
    },
    refreshQRCode() {
      this.updateQrCode();
      common_vendor.index.showToast({ title: "二维码已刷新", icon: "success" });
    },
    // 新增：跳转到订单详情页的方法 (修改：使用 redirectTo)
    goToOrderDetail() {
      if (this.orderData.orderNo) {
        common_vendor.index.redirectTo({
          url: `/pages/OrderDetailPage/OrderDetailPage?orderNo=${encodeURIComponent(this.orderData.orderNo)}`
        });
      } else {
        common_vendor.index.showToast({ title: "无法跳转：缺少订单号", icon: "none" });
      }
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: common_vendor.o((...args) => $options.goToOrderDetail && $options.goToOrderDetail(...args)),
    b: common_vendor.t($data.orderData.orderNo || "PZ202311150001"),
    c: common_vendor.t($options.formatAmount($data.orderData.amount)),
    d: common_vendor.t($data.payTime),
    e: common_vendor.t($data.orderData.attendantName || "王丽"),
    f: common_vendor.t($data.orderData.attendantPhone || "13800138000"),
    g: common_vendor.t($data.orderData.serviceTime || "2023年11月15日 上午9:30"),
    h: common_vendor.t($data.orderData.hospital || "北京协和医院"),
    i: common_vendor.t($data.orderData.contactPerson || "张三"),
    j: $data.orderData.contactPhone
  }, $data.orderData.contactPhone ? {
    k: common_vendor.t($data.orderData.contactPhone)
  } : {}, {
    l: $data.orderData.specialRequirements
  }, $data.orderData.specialRequirements ? {
    m: common_vendor.t($data.orderData.specialRequirements)
  } : {}, {
    n: $data.qrCodeSrc,
    o: common_vendor.o((...args) => $options.handleQrError && $options.handleQrError(...args)),
    p: common_vendor.o((...args) => $options.saveQRCode && $options.saveQRCode(...args)),
    q: common_vendor.o((...args) => $options.refreshQRCode && $options.refreshQRCode(...args))
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-d6d97d31"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/PaymentSuccessPage/PaymentSuccessPage.js.map

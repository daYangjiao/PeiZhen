"use strict";
const common_vendor = require("../../common/vendor.js");
const common_assets = require("../../common/assets.js");
const utils_api = require("../../utils/api.js");
const _sfc_main = {
  __name: "OrderDetailPage",
  setup(__props) {
    const order = common_vendor.ref({});
    const companion = common_vendor.ref({ name: "", phone: "", rating: "4.9" });
    const companionAvatar = common_vendor.ref(null);
    const serviceSteps = common_vendor.computed(() => {
      if (!order.value)
        return [];
      const steps = [];
      switch (order.value.status) {
        case "pending":
          steps.push(
            { title: "订单创建", desc: "您已成功提交订单", time: "" },
            { title: "待支付", desc: "请在规定时间内完成支付", time: "" }
          );
          break;
        case "assigned":
          steps.push(
            { title: "订单创建", desc: "您已成功提交订单", time: "" },
            { title: "已支付", desc: "订单已支付，等待陪诊师接单", time: "" }
            // 修改此处
          );
          break;
        case "in_progress":
          steps.push(
            { title: "订单创建", desc: "您已成功提交订单", time: "" },
            { title: "陪诊师已接单", desc: "陪诊师已接单，准备为您服务", time: "" },
            { title: "陪诊师预计到达", desc: "陪诊师预计于 14:30 到达医院", time: "14:30" },
            { title: "陪诊师已到达", desc: "陪诊师已抵达医院，等待就诊", time: "15:00" },
            { title: "服务进行中", desc: "陪诊师正在陪同就诊", time: "15:15" }
          );
          break;
        case "completed":
          steps.push(
            { title: "订单创建", desc: "您已成功提交订单", time: "" },
            { title: "陪诊师已接单", desc: "陪诊师已接单，准备为您服务", time: "" },
            { title: "陪诊师预计到达", desc: "陪诊师预计于 09:00 到达医院", time: "09:00" },
            { title: "陪诊师已到达", desc: "陪诊师已抵达医院，等待就诊", time: "09:30" },
            { title: "服务进行中", desc: "陪诊师正在陪同就诊", time: "10:00" },
            { title: "服务已完成", desc: "陪诊服务已结束", time: "10:30" }
          );
          break;
        case "cancelled":
          steps.push(
            { title: "订单创建", desc: "您已成功提交订单", time: "" },
            { title: "订单已取消", desc: "订单已被取消", time: "" }
          );
          break;
        default:
          steps.push({ title: "订单创建", desc: "您已成功提交订单", time: "" });
      }
      return steps;
    });
    const getStatusText = (status) => {
      switch (status) {
        case "assigned":
          return "待陪诊师接单";
        case "pending":
          return "待支付";
        case "cancelled":
          return "已取消";
        case "in_progress":
          return "进行中";
        case "completed":
          return "已完成";
        default:
          return "未知";
      }
    };
    const getStatusClass = (status) => {
      return `status-${status}`;
    };
    const showCancelButton = common_vendor.computed(() => {
      return order.value && order.value.status === "assigned";
    });
    const cancelOrder = () => {
      common_vendor.index.showModal({
        title: "确认取消",
        content: "您确定要取消此订单吗？",
        success: (res) => {
          var _a, _b, _c;
          if (res.confirm) {
            common_vendor.index.__f__("log", "at pages/OrderDetailPage/OrderDetailPage.vue:236", "用户确认取消订单，订单号:", (_a = order.value) == null ? void 0 : _a.orderId);
            if ((_b = order.value) == null ? void 0 : _b.orderId) {
              let allOrders = common_vendor.index.getStorageSync("all_orders") || [];
              const index = allOrders.findIndex((o) => o.id === order.value.orderId);
              if (index !== -1) {
                allOrders[index].status = "cancelled";
                common_vendor.index.setStorageSync("all_orders", allOrders);
              }
              const specificOrderKey = `order_${order.value.orderId}`;
              const specificOrderData = common_vendor.index.getStorageSync(specificOrderKey);
              if (specificOrderData) {
                specificOrderData.status = "cancelled";
                common_vendor.index.setStorageSync(specificOrderKey, specificOrderData);
                if (((_c = common_vendor.index.getStorageSync("orderConfirmData")) == null ? void 0 : _c.orderNo) === order.value.orderId) {
                  common_vendor.index.setStorageSync("orderConfirmData", specificOrderData);
                }
              }
              order.value.status = "cancelled";
              common_vendor.index.showToast({
                title: "订单已取消",
                icon: "success"
              });
            }
          } else if (res.cancel) {
            common_vendor.index.__f__("log", "at pages/OrderDetailPage/OrderDetailPage.vue:276", "用户取消取消操作");
          }
        }
      });
    };
    const returnToOrders = () => {
      common_vendor.index.reLaunch({
        url: "/pages/order/order"
      });
    };
    const fetchOrderDetail = async (orderNo) => {
      var _a, _b;
      try {
        common_vendor.index.__f__("log", "at pages/OrderDetailPage/OrderDetailPage.vue:293", "【OrderDetailPage】正在调用后端接口获取订单完整信息:", orderNo);
        const response = await utils_api.get(`/ai/guide/orders/${orderNo}/complete-info`);
        common_vendor.index.__f__("log", "at pages/OrderDetailPage/OrderDetailPage.vue:296", "【OrderDetailPage】后端返回的订单完整数据:", response.data);
        if (response && response.data) {
          const backendData = response.data;
          order.value = {
            orderId: backendData.orderNo || "未知订单号",
            status: mapOrderStatus(backendData.orderStatus),
            // 将后端状态码映射为前端状态
            serviceType: backendData.serviceTypeName || "普通陪诊",
            serviceDate: backendData.serviceDate || "未知日期",
            startTime: ((_a = backendData.serviceTime) == null ? void 0 : _a.split("-")[0]) || "未知时间",
            // 假设 serviceTime 是 "08:00:00-10:00:00"
            hospitalName: backendData.hospital || "未知医院",
            contactPerson: backendData.patientName || "未知对象",
            // 优先使用患者姓名
            price: `¥${backendData.totalPrice.toFixed(2)}`,
            // 假设 totalPrice 以分为单位
            paymentMethod: "微信支付",
            // 固定值
            payTime: backendData.createTime || (/* @__PURE__ */ new Date()).toLocaleString(),
            // 使用创建时间
            otherRequirement: backendData.otherRequirement,
            // 新增字段
            // --- 陪诊师信息 ---
            attendantName: backendData.attendantName || "未知陪诊师",
            attendantPhone: backendData.attendantPhone || "未知电话",
            attendantAvatar: backendData.attendantAvatar,
            // 后端返回的头像路径
            rating: ((_b = backendData.attendantScore) == null ? void 0 : _b.toString()) || "4.9"
          };
          companion.value = {
            name: order.value.attendantName,
            phone: order.value.attendantPhone,
            rating: order.value.rating
          };
          let avatarPath = null;
          if (order.value.attendantAvatar) {
            avatarPath = order.value.attendantAvatar;
            common_vendor.index.__f__("log", "at pages/OrderDetailPage/OrderDetailPage.vue:336", "【OrderDetailPage】从 attendantAvatar 获取头像路径:", avatarPath);
          }
          if (!avatarPath && order.value.attendantName) {
            avatarPath = `/static/avatar_${order.value.attendantName}.jpg`;
            common_vendor.index.__f__("log", "at pages/OrderDetailPage/OrderDetailPage.vue:342", "【OrderDetailPage】根据陪诊师姓名获取头像路径:", avatarPath);
          }
          if (avatarPath) {
            if (avatarPath && !avatarPath.startsWith("http")) {
              if (avatarPath.startsWith("/")) {
                avatarPath = avatarPath.substring(1);
              }
              const normalizedBaseUrl = utils_api.config.baseURL.endsWith("/") ? utils_api.config.baseURL : utils_api.config.baseURL + "/";
              avatarPath = normalizedBaseUrl + avatarPath;
            }
            common_vendor.index.__f__("log", "at pages/OrderDetailPage/OrderDetailPage.vue:358", "【OrderDetailPage】拼接后的完整头像路径:", avatarPath);
            companionAvatar.value = avatarPath;
          } else {
            common_vendor.index.__f__("log", "at pages/OrderDetailPage/OrderDetailPage.vue:362", "【OrderDetailPage】无头像路径，使用默认头像");
            const normalizedBaseUrl = utils_api.config.baseURL.endsWith("/") ? utils_api.config.baseURL : utils_api.config.baseURL + "/";
            companionAvatar.value = normalizedBaseUrl + "static/default-avatar.jpg";
          }
          common_vendor.index.__f__("log", "at pages/OrderDetailPage/OrderDetailPage.vue:367", "【OrderDetailPage】最终设置的头像路径:", companionAvatar.value);
        } else {
          common_vendor.index.__f__("warn", "at pages/OrderDetailPage/OrderDetailPage.vue:370", "【OrderDetailPage】后端返回数据为空或格式不正确:", response);
          common_vendor.index.showToast({
            title: "获取订单信息失败",
            icon: "none"
          });
        }
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/OrderDetailPage/OrderDetailPage.vue:377", "【OrderDetailPage】获取订单完整信息失败:", error);
        common_vendor.index.showToast({
          title: "网络错误，获取订单信息失败",
          icon: "none"
        });
      }
    };
    const mapOrderStatus = (backendStatus) => {
      switch (backendStatus) {
        case 1:
          return "assigned";
        case 2:
          return "pending";
        case 3:
          return "in_progress";
        case 4:
          return "completed";
        case 5:
          return "cancelled";
        default:
          return "assigned";
      }
    };
    const handleImageError = (e) => {
      common_vendor.index.__f__("error", "at pages/OrderDetailPage/OrderDetailPage.vue:399", "【OrderDetailPage】头像图片加载失败:", e);
      common_vendor.index.__f__("log", "at pages/OrderDetailPage/OrderDetailPage.vue:400", "当前尝试加载的头像路径:", companionAvatar.value);
    };
    const callCompanion = () => {
      if (companion.value.phone && order.value.status !== "assigned") {
        common_vendor.index.makePhoneCall({
          phoneNumber: companion.value.phone
        });
      } else {
        common_vendor.index.showToast({
          title: order.value.status === "assigned" ? "待陪诊师接单，暂不可联系" : "未获取到陪诊师电话",
          icon: "none"
        });
      }
    };
    const consult = () => {
      common_vendor.index.showToast({
        title: "暂未开放咨询功能"
      });
    };
    const share = () => {
      common_vendor.index.showToast({
        title: "暂未开放分享功能"
      });
    };
    const evaluate = () => {
      var _a;
      if (!((_a = order.value) == null ? void 0 : _a.orderId)) {
        common_vendor.index.showToast({
          title: "订单ID缺失，无法评价",
          icon: "none"
        });
        return;
      }
      common_vendor.index.navigateTo({
        url: "/pages/Evaluate/Evaluate"
      });
    };
    common_vendor.onLoad(async (options) => {
      const passedOrderNo = options.orderNo || options.orderId;
      common_vendor.index.__f__("log", "at pages/OrderDetailPage/OrderDetailPage.vue:446", "【OrderDetailPage】onLoad 接收到的参数:", options);
      common_vendor.index.__f__("log", "at pages/OrderDetailPage/OrderDetailPage.vue:447", "【OrderDetailPage】提取的订单号 (passedOrderNo):", passedOrderNo);
      if (!passedOrderNo) {
        common_vendor.index.__f__("error", "at pages/OrderDetailPage/OrderDetailPage.vue:450", "【OrderDetailPage】缺少订单号参数");
        common_vendor.index.showToast({
          title: "订单号错误",
          icon: "none"
        });
        common_vendor.index.redirectTo({
          url: "/pages/index/index"
        });
        return;
      }
      await fetchOrderDetail(passedOrderNo);
    });
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_vendor.o(returnToOrders),
        b: common_vendor.t(getStatusText(order.value.status)),
        c: common_vendor.n(getStatusClass(order.value.status)),
        d: common_vendor.t(order.value.orderId),
        e: common_vendor.t(order.value.serviceType),
        f: common_vendor.t(order.value.serviceDate),
        g: common_vendor.t(order.value.startTime),
        h: common_vendor.t(order.value.hospitalName),
        i: common_vendor.t(order.value.contactPerson || order.value.serviceTarget),
        j: common_vendor.t(order.value.otherRequirement || "无"),
        k: companionAvatar.value
      }, companionAvatar.value ? {
        l: companionAvatar.value,
        m: common_vendor.o(handleImageError)
      } : {
        n: common_assets._imports_0$5
      }, {
        o: common_vendor.t(companion.value.name),
        p: order.value.status !== "assigned"
      }, order.value.status !== "assigned" ? {
        q: common_assets._imports_1$4,
        r: common_vendor.t(companion.value.phone)
      } : {}, {
        s: common_vendor.t(companion.value.rating),
        t: common_vendor.t(order.value.price),
        v: common_vendor.t(order.value.paymentMethod),
        w: common_vendor.t(order.value.payTime),
        x: common_vendor.f(serviceSteps.value, (item, index, i0) => {
          return {
            a: common_vendor.t(item.title),
            b: common_vendor.t(item.desc),
            c: common_vendor.t(item.time),
            d: index
          };
        }),
        y: showCancelButton.value
      }, showCancelButton.value ? {
        z: common_vendor.o(cancelOrder)
      } : {}, {
        A: common_assets._imports_1$4,
        B: common_vendor.o(callCompanion),
        C: common_assets._imports_2$2,
        D: common_vendor.o(consult),
        E: common_assets._imports_3$2,
        F: common_vendor.o(evaluate),
        G: common_assets._imports_4$1,
        H: common_vendor.o(share)
      });
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-2e88df03"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/OrderDetailPage/OrderDetailPage.js.map

"use strict";
const common_vendor = require("../../common/vendor.js");
const common_assets = require("../../common/assets.js");
const utils_api = require("../../utils/api.js");
const utils_websocket = require("../../utils/websocket.js");
const _sfc_main = {
  __name: "OrderDetailPage",
  setup(__props) {
    const order = common_vendor.ref({});
    const userEvaluation = common_vendor.ref(null);
    const balancePayMethod = common_vendor.ref("wechat");
    const showBalancePayResultModal = common_vendor.ref(false);
    const showContactModal = common_vendor.ref(false);
    let pollTimer = null;
    let payCountdownTimer = null;
    const payCountdown = common_vendor.ref("");
    const showCancelModal = common_vendor.ref(false);
    const cancelReasons = [
      "计划有变，暂不就诊",
      "信息填写有误，重新下单",
      "价格原因，暂不接受",
      "通过其他渠道已就诊",
      "其他原因"
    ];
    const selectedCancelReason = common_vendor.ref(cancelReasons[0]);
    const cancelRemark = common_vendor.ref("");
    const showPayCountdown = common_vendor.computed(() => {
      return order.value && order.value.paymentStatus === 0 && order.value.orderStatus !== 7;
    });
    const showQRCode = common_vendor.computed(() => {
      return order.value.orderStatus === 2 && order.value.qrCodeUrl;
    });
    const showDurationConfirm = common_vendor.computed(() => {
      return order.value.orderStatus === 4;
    });
    const diffDesc = common_vendor.computed(() => {
      if (!order.value || order.value.balanceAmount == null) {
        return "无费用差异";
      }
      const b = Number(order.value.balanceAmount);
      if (b === 0)
        return "无费用差异";
      if (b > 0)
        return `需补付¥${formatAmount(b)}`;
      return `自动退款¥${formatAmount(Math.abs(b))}`;
    });
    const confirmBtnText = common_vendor.computed(() => {
      if (!order.value || order.value.balanceAmount == null) {
        return "确认时长，完成订单";
      }
      const b = Number(order.value.balanceAmount);
      if (b === 0)
        return "确认时长，完成订单";
      if (b > 0)
        return `立即补付¥${formatAmount(b)}`;
      return "确认并自动退款";
    });
    const hasEvaluated = common_vendor.computed(() => {
      if (!order.value || !order.value.orderNo)
        return false;
      const key = `order_evaluated_${order.value.orderNo}`;
      return common_vendor.index.getStorageSync(key) === "1";
    });
    const primaryActionText = common_vendor.computed(() => {
      if (!order.value || order.value.orderStatus === void 0 || order.value.orderStatus === null) {
        return "";
      }
      const status = order.value.orderStatus;
      const paymentStatus = order.value.paymentStatus;
      if (paymentStatus === 0 && status !== 7) {
        return "去支付";
      }
      if (status === 1) {
        return "取消订单";
      }
      if (status === 3) {
        return "查看服务进度";
      }
      if (status === 5) {
        return "查看申诉进度";
      }
      if (status === 6) {
        return hasEvaluated.value ? "再次下单" : "去评价";
      }
      if (status === 7) {
        return "再次下单";
      }
      return "";
    });
    const onBalancePayMethodChange = (e) => {
      balancePayMethod.value = e.detail.value;
    };
    const finalAmount = common_vendor.computed(() => {
      if (!order.value)
        return 0;
      const base = order.value.totalPrice || order.value.orderAmount;
      return Number(base || 0);
    });
    const diffAmount = common_vendor.computed(() => {
      if (!order.value || order.value.balanceAmount == null)
        return 0;
      return Number(order.value.balanceAmount || 0);
    });
    const hasDiff = common_vendor.computed(() => {
      return diffAmount.value !== 0;
    });
    const prepayAmount = common_vendor.computed(() => {
      if (!order.value)
        return 0;
      if (order.value.balanceAmount != null && diffAmount.value !== 0) {
        return finalAmount.value - diffAmount.value;
      }
      return finalAmount.value;
    });
    const diffLabel = common_vendor.computed(() => {
      if (!hasDiff.value)
        return "无差额";
      if (diffAmount.value > 0)
        return "实际补付";
      return "实际退款";
    });
    const userServiceFlowSteps = [
      { key: "arrived", label: "已到院" },
      { key: "waiting", label: "候诊中" },
      { key: "exam", label: "检查中" },
      { key: "finished", label: "就诊完成" }
    ];
    const currentServiceProgressStep = common_vendor.computed(() => {
      const step = order.value.serviceProgressStep;
      if (!step || step < 1)
        return 1;
      if (step > 4)
        return 4;
      return step;
    });
    const setupPayCountdown = () => {
      if (payCountdownTimer) {
        clearInterval(payCountdownTimer);
        payCountdownTimer = null;
      }
      if (!showPayCountdown.value) {
        payCountdown.value = "";
        return;
      }
      const createTimeStr = order.value.createTime || order.value.orderDate;
      if (!createTimeStr) {
        payCountdown.value = "";
        return;
      }
      const baseTime = Date.parse(createTimeStr.replace(/-/g, "/"));
      if (isNaN(baseTime)) {
        payCountdown.value = "";
        return;
      }
      const deadline = baseTime + 15 * 60 * 1e3;
      const tick = async () => {
        const now = Date.now();
        const diff = deadline - now;
        if (diff <= 0) {
          payCountdown.value = "00:00";
          if (payCountdownTimer) {
            clearInterval(payCountdownTimer);
            payCountdownTimer = null;
          }
          if (order.value && order.value.paymentStatus === 0 && order.value.orderStatus !== 7) {
            try {
              await utils_api.put(`/api/orders/${order.value.orderId}/cancel?reason=${encodeURIComponent("超时未支付自动取消")}`);
              common_vendor.index.showToast({ title: "超时未支付，订单已自动取消", icon: "none" });
              await fetchOrderDetail(order.value.orderNo);
            } catch (e) {
              common_vendor.index.__f__("error", "at pages/OrderDetailPage/OrderDetailPage.vue:686", "自动取消超时未支付订单失败:", e);
            }
          }
          return;
        }
        const minutes = Math.floor(diff / 6e4);
        const seconds = Math.floor(diff % 6e4 / 1e3);
        const mm = minutes.toString().padStart(2, "0");
        const ss = seconds.toString().padStart(2, "0");
        payCountdown.value = `${mm}:${ss}`;
      };
      tick();
      payCountdownTimer = setInterval(tick, 1e3);
    };
    const serviceSteps = common_vendor.computed(() => {
      if (!order.value || !order.value.orderNo)
        return [];
      const steps = [];
      const status = order.value.orderStatus;
      const paymentStatus = order.value.paymentStatus;
      steps.push({ title: "订单创建", desc: "您已成功提交订单", time: order.value.createTime || formatDate(order.value.orderDate) });
      if (status === 7) {
        if (paymentStatus === 1) {
          steps.push({ title: "已支付", desc: "订单费用已支付", time: order.value.paymentTime || "" });
        }
        steps.push({
          title: "订单取消",
          desc: order.value.cancelReason || "订单已被取消",
          time: order.value.cancelTime || ""
        });
        return steps;
      }
      if (paymentStatus === 1) {
        steps.push({ title: "已支付", desc: "订单费用已支付", time: order.value.paymentTime || "" });
      }
      if (status >= 2) {
        const acceptTime = formatDate(order.value.serviceStartTime) || (order.value.paymentTime || order.value.createTime || "");
        steps.push({ title: "陪诊师已接单", desc: "陪诊师已接单，准备为您服务", time: acceptTime });
      }
      if (status >= 3) {
        steps.push({ title: "服务开始", desc: "陪诊师已开始服务", time: formatDate(order.value.serviceStartTime) });
      }
      if (status >= 4) {
        const endTime = formatDate(order.value.serviceEndTime);
        steps.push({ title: "服务结束", desc: "陪诊师已结束服务", time: endTime });
        steps.push({ title: "待确认时长", desc: "请确认实际服务时长与费用", time: endTime });
      }
      if (status >= 6) {
        const finishTime = formatDate(order.value.updateTime || order.value.serviceEndTime);
        steps.push({ title: "订单完成", desc: "订单已完成", time: finishTime });
      }
      if (status === 7) {
        steps.push({ title: "订单取消", desc: "订单已被取消", time: "" });
      }
      return steps;
    });
    const getOrderStatusText = (order2) => {
      if (!order2)
        return "未知状态";
      if (order2.paymentStatus === 0 && order2.orderStatus !== 7) {
        return "待支付";
      }
      const statusMap = {
        0: "待接单",
        1: "待接单",
        2: "待服务",
        // 修改：将“已接单”改为“待服务”
        3: "服务中",
        4: "待确认时长",
        5: "待支付差价",
        6: "已完成",
        7: "已取消"
      };
      return statusMap[order2.orderStatus] || "未知状态";
    };
    const getStatusClass = (order2) => {
      if (!order2)
        return "";
      if (order2.paymentStatus === 0 && order2.orderStatus !== 7) {
        return "status-deposit";
      }
      const classMap = {
        0: "status-waiting",
        1: "status-waiting",
        2: "status-accepted",
        3: "status-service",
        4: "status-confirm",
        5: "status-balance",
        6: "status-completed",
        7: "status-cancelled"
      };
      return classMap[order2.orderStatus] || "status-default";
    };
    const getServiceTypeName = (type) => {
      const typeMap = {
        1: "普通陪诊",
        2: "术后护理",
        3: "急诊陪同",
        4: "上门陪诊"
      };
      return typeMap[type] || "未知类型";
    };
    const formatDate = (date) => {
      if (!date)
        return "";
      return new Date(date).toLocaleString("zh-CN");
    };
    const formatAmount = (amount) => {
      if (!amount)
        return "0.00";
      return Number(amount).toFixed(2);
    };
    const getCancelByText = (cancelBy) => {
      if (cancelBy === 1)
        return "陪诊师";
      if (cancelBy === 0)
        return "用户";
      return "系统";
    };
    const openCancelModal = () => {
      if (!order.value)
        return;
      const status = order.value.orderStatus;
      if (status >= 3) {
        return;
      }
      showCancelModal.value = true;
    };
    const submitCancelOrder = async () => {
      if (!order.value || !order.value.orderId) {
        common_vendor.index.showToast({ title: "订单信息有误", icon: "none" });
        return;
      }
      const reasonText = selectedCancelReason.value || "用户主动取消";
      const fullReason = cancelRemark.value ? `${reasonText}（${cancelRemark.value}）` : reasonText;
      try {
        common_vendor.index.showLoading({ title: "正在取消...", mask: true });
        await utils_api.put(`/api/orders/${order.value.orderId}/cancel?reason=${encodeURIComponent(fullReason)}`);
        common_vendor.index.hideLoading();
        common_vendor.index.showToast({ title: "订单已取消", icon: "none" });
        showCancelModal.value = false;
        await fetchOrderDetail(order.value.orderNo);
      } catch (e) {
        common_vendor.index.hideLoading();
        common_vendor.index.__f__("error", "at pages/OrderDetailPage/OrderDetailPage.vue:860", "取消订单失败:", e);
        common_vendor.index.showToast({ title: "取消失败，请稍后重试", icon: "none" });
      }
    };
    const getAvatarUrl = (avatarPath) => {
      if (!avatarPath)
        return "/static/default-avatar.jpg";
      if (avatarPath.startsWith("http"))
        return avatarPath;
      const baseUrl = utils_api.config.baseURL.endsWith("/") ? utils_api.config.baseURL : utils_api.config.baseURL + "/";
      const cleanUrl = avatarPath.startsWith("/") ? avatarPath.substring(1) : avatarPath;
      return baseUrl + cleanUrl;
    };
    const handleImageError = (e) => {
      common_vendor.index.__f__("error", "at pages/OrderDetailPage/OrderDetailPage.vue:876", "头像加载失败:", e);
    };
    const callCompanion = () => {
      if (!order.value) {
        common_vendor.index.showToast({ title: "订单信息有误", icon: "none" });
        return;
      }
      const phone = order.value.attendantPhone;
      const targetId = order.value.attendantId;
      if (!phone && !targetId) {
        common_vendor.index.showToast({ title: "当前暂无陪诊师联系方式", icon: "none" });
        return;
      }
      showContactModal.value = true;
    };
    const handleContactChoice = (type) => {
      showContactModal.value = false;
      if (!order.value)
        return;
      const phone = order.value.attendantPhone;
      const targetId = order.value.attendantId;
      const targetName = order.value.attendantName || "陪诊师";
      if (type === "phone") {
        if (phone) {
          common_vendor.index.makePhoneCall({ phoneNumber: phone });
        } else {
          common_vendor.index.showToast({ title: "暂未提供陪诊师电话", icon: "none" });
        }
      } else if (type === "chat") {
        if (targetId) {
          common_vendor.index.navigateTo({
            url: `/subpkg/chat/chat?attendantId=${encodeURIComponent(targetId)}&name=${encodeURIComponent(targetName)}&avatar=${encodeURIComponent(order.value.attendantAvatar || "")}`
          });
        } else {
          common_vendor.index.showToast({ title: "暂未提供在线联系方式", icon: "none" });
        }
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
    const showDisputeModal = common_vendor.ref(false);
    const disputeDuration = common_vendor.ref("");
    const disputeReason = common_vendor.ref("");
    const formatDuration = (val) => {
      if (val == null)
        return "--";
      const num = Number(val);
      if (Number.isNaN(num))
        return "--";
      return num.toFixed(1).replace(/\.0$/, "");
    };
    const openDisputeModal = () => {
      disputeDuration.value = formatDuration(order.value.actualDuration);
      disputeReason.value = "";
      showDisputeModal.value = true;
    };
    const submitDispute = async () => {
      try {
        const dur = disputeDuration.value ? Number(disputeDuration.value) : null;
        const query = [];
        if (!Number.isNaN(dur) && dur > 0) {
          query.push(`userDuration=${dur}`);
        }
        if (disputeReason.value) {
          query.push(`reason=${encodeURIComponent(disputeReason.value)}`);
        }
        const qs = query.length ? `?${query.join("&")}` : "";
        const response = await utils_api.post(`/api/orders/${order.value.orderId}/dispute-time-fee${qs}`);
        if (response && response.code === 200) {
          common_vendor.index.showToast({ title: "申诉已提交", icon: "success" });
          showDisputeModal.value = false;
          await fetchOrderDetail(order.value.orderNo);
        } else {
          common_vendor.index.showToast({ title: response.message || "提交申诉失败", icon: "none" });
        }
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/OrderDetailPage/OrderDetailPage.vue:991", "提交申诉失败:", error);
        common_vendor.index.showToast({ title: "提交申诉失败", icon: "none" });
      }
    };
    const doConfirmTimeAndFee = async () => {
      try {
        common_vendor.index.showLoading({ title: "提交中..." });
        const response = await utils_api.post(`/api/orders/${order.value.orderId}/confirm-time-fee`);
        common_vendor.index.hideLoading();
        if (response && response.code === 200) {
          common_vendor.index.showToast({ title: "确认成功", icon: "success" });
          await fetchOrderDetail(order.value.orderNo);
        } else {
          common_vendor.index.showToast({ title: response.message || "确认失败", icon: "none" });
        }
      } catch (error) {
        common_vendor.index.hideLoading();
        common_vendor.index.__f__("error", "at pages/OrderDetailPage/OrderDetailPage.vue:1010", "确认时长失败:", error);
        common_vendor.index.showToast({ title: "确认失败", icon: "none" });
      }
    };
    const confirmDuration = async () => {
      if (!order.value)
        return;
      const b = Number(order.value.balanceAmount || 0);
      if (!Number.isNaN(b) && b > 0) {
        if (!balancePayMethod.value) {
          common_vendor.index.showToast({ title: "请选择支付方式", icon: "none" });
          return;
        }
        showBalancePayResultModal.value = true;
        return;
      }
      await doConfirmTimeAndFee();
    };
    const handleBalancePayResult = async (isPaid) => {
      showBalancePayResultModal.value = false;
      if (!order.value)
        return;
      if (isPaid) {
        await doConfirmTimeAndFee();
      } else {
        common_vendor.index.navigateTo({
          url: `/subpkg/appointment-flow/PaymentFailedPage?orderNo=${encodeURIComponent(order.value.orderNo)}&scene=balance`
        });
      }
    };
    const handlePrimaryAction = () => {
      if (!order.value)
        return;
      const status = order.value.orderStatus;
      const paymentStatus = order.value.paymentStatus;
      const orderNo = order.value.orderNo;
      if (status === 1) {
        openCancelModal();
        return;
      }
      if (paymentStatus === 0 && status !== 7) {
        if (orderNo) {
          common_vendor.index.navigateTo({
            url: `/subpkg/appointment-flow/04_OrderConfirmPage?orderNo=${encodeURIComponent(orderNo)}`
          });
        }
        return;
      }
      if (status === 3) {
        common_vendor.index.pageScrollTo({
          selector: ".service-progress-card",
          duration: 300
        });
        return;
      }
      if (status === 5) {
        common_vendor.index.pageScrollTo({
          selector: ".record-section",
          duration: 300
        });
        return;
      }
      if (status === 6) {
        if (!hasEvaluated.value) {
          if (orderNo) {
            common_vendor.index.navigateTo({
              url: `/subpkg/evaluate/Evaluate?orderNo=${encodeURIComponent(orderNo)}`
            });
          }
        } else {
          common_vendor.index.navigateTo({
            url: "/subpkg/appointment-flow/01_AppointmentSelection"
          });
        }
        return;
      }
      if (status === 7) {
        common_vendor.index.navigateTo({
          url: "/subpkg/appointment-flow/01_AppointmentSelection"
        });
      }
    };
    const getSymptomDescription = () => {
      const { symptoms } = order.value;
      if (!symptoms) {
        return "无";
      }
      if (Array.isArray(symptoms)) {
        const validSymptoms = symptoms.filter((s) => s && s.trim() && s !== "无" && s !== "null");
        return validSymptoms.length > 0 ? validSymptoms.join(", ") : "无";
      }
      return symptoms;
    };
    const loadUserEvaluation = async (orderId) => {
      if (!orderId)
        return;
      try {
        const res = await utils_api.get(`/api/orders/${orderId}/evaluation`);
        if (res && res.code === 200 && res.data) {
          userEvaluation.value = res.data;
        } else {
          userEvaluation.value = null;
        }
      } catch (e) {
        common_vendor.index.__f__("error", "at pages/OrderDetailPage/OrderDetailPage.vue:1154", "加载评价失败", e);
        userEvaluation.value = null;
      }
    };
    const fetchOrderDetail = async (orderNo) => {
      try {
        common_vendor.index.__f__("log", "at pages/OrderDetailPage/OrderDetailPage.vue:1162", "正在获取订单详情，订单号:", orderNo);
        const response = await utils_api.get(`/ai/guide/orders/${orderNo}/complete-info`);
        common_vendor.index.__f__("log", "at pages/OrderDetailPage/OrderDetailPage.vue:1165", "订单详情响应:", response);
        if (response && response.code === 200 && response.data) {
          order.value = response.data;
          if (order.value.orderStatus >= 2 && pollTimer) {
            clearInterval(pollTimer);
            pollTimer = null;
          }
          setupPayCountdown();
          if (order.value.orderStatus === 6 && order.value.orderId) {
            loadUserEvaluation(order.value.orderId);
          } else {
            userEvaluation.value = null;
          }
        } else {
          common_vendor.index.showToast({ title: "获取订单详情失败", icon: "none" });
        }
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/OrderDetailPage/OrderDetailPage.vue:1189", "获取订单详情失败:", error);
        common_vendor.index.showToast({ title: "网络错误", icon: "none" });
      }
    };
    const handleSocketMessage = (message) => {
      common_vendor.index.__f__("log", "at pages/OrderDetailPage/OrderDetailPage.vue:1196", "收到 WebSocket 消息:", message);
      const isCurrentOrder = message.orderId === order.value.orderId || message.orderNo === order.value.orderNo || message.data && (message.data.orderId === order.value.orderId || message.data.orderNo === order.value.orderNo);
      if (isCurrentOrder && (message.type === "ORDER_ACCEPTED" || message.type === "SERVICE_STARTED" || message.type === "SERVICE_COMPLETED" || message.type === "ORDER_STATUS_CHANGED" || message.type === "ORDER_RELEASED_BY_ATTENDANT" || message.type === "SERVICE_PROGRESS_UPDATED")) {
        common_vendor.index.__f__("log", "at pages/OrderDetailPage/OrderDetailPage.vue:1211", "收到当前订单状态更新消息，刷新详情");
        if (message.type === "ORDER_RELEASED_BY_ATTENDANT") {
          common_vendor.index.showToast({ title: "订单已重新进入接单大厅，将为您匹配合诊师", icon: "none", duration: 2500 });
        }
        setTimeout(() => {
          fetchOrderDetail(order.value.orderNo);
        }, 1e3);
      }
    };
    common_vendor.onLoad(async (options) => {
      const orderNo = options.orderNo || options.orderId;
      common_vendor.index.__f__("log", "at pages/OrderDetailPage/OrderDetailPage.vue:1226", "页面加载参数:", options);
      common_vendor.index.__f__("log", "at pages/OrderDetailPage/OrderDetailPage.vue:1227", "解析出的订单号:", orderNo);
      if (!orderNo) {
        common_vendor.index.showToast({ title: "订单号错误", icon: "none" });
        setTimeout(() => {
          common_vendor.index.reLaunch({ url: "/pages/order/order" });
        }, 1500);
        return;
      }
      await fetchOrderDetail(orderNo);
      utils_websocket.addSocketListener(handleSocketMessage);
      if (order.value.orderStatus === 1) {
        pollTimer = setInterval(() => {
          fetchOrderDetail(orderNo);
        }, 5e3);
      }
    });
    common_vendor.onUnmounted(() => {
      utils_websocket.removeSocketListener(handleSocketMessage);
      if (pollTimer) {
        clearInterval(pollTimer);
        pollTimer = null;
      }
      if (payCountdownTimer) {
        clearInterval(payCountdownTimer);
        payCountdownTimer = null;
      }
    });
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_vendor.t(getOrderStatusText(order.value)),
        b: common_vendor.n(getStatusClass(order.value)),
        c: common_vendor.t(order.value.orderNo),
        d: showPayCountdown.value
      }, showPayCountdown.value ? {
        e: common_vendor.t(payCountdown.value)
      } : {}, {
        f: order.value.orderStatus === 3 || order.value.orderStatus === 4 || order.value.orderStatus === 6
      }, order.value.orderStatus === 3 || order.value.orderStatus === 4 || order.value.orderStatus === 6 ? {
        g: common_vendor.t(userServiceFlowSteps[0].label),
        h: currentServiceProgressStep.value >= 1 ? 1 : "",
        i: currentServiceProgressStep.value === 1 ? 1 : "",
        j: currentServiceProgressStep.value >= 2 ? 1 : "",
        k: common_vendor.t(userServiceFlowSteps[1].label),
        l: currentServiceProgressStep.value >= 2 ? 1 : "",
        m: currentServiceProgressStep.value === 2 ? 1 : "",
        n: currentServiceProgressStep.value >= 3 ? 1 : "",
        o: common_vendor.t(userServiceFlowSteps[2].label),
        p: currentServiceProgressStep.value >= 3 ? 1 : "",
        q: currentServiceProgressStep.value === 3 ? 1 : "",
        r: currentServiceProgressStep.value >= 4 ? 1 : "",
        s: common_vendor.t(userServiceFlowSteps[3].label),
        t: currentServiceProgressStep.value >= 4 ? 1 : "",
        v: currentServiceProgressStep.value === 4 ? 1 : ""
      } : {}, {
        w: common_vendor.t(order.value.serviceTypeName || getServiceTypeName(order.value.clinicType)),
        x: common_vendor.t(order.value.serviceDate),
        y: common_vendor.t(order.value.serviceTime),
        z: common_vendor.t(order.value.hospital),
        A: common_vendor.t(order.value.patientName),
        B: common_vendor.t(order.value.patientPhone || order.value.contactPhone),
        C: common_vendor.t(getSymptomDescription()),
        D: common_vendor.t(order.value.otherRequirement || "无"),
        E: order.value.attendantName && order.value.attendantName !== "待分配陪诊师"
      }, order.value.attendantName && order.value.attendantName !== "待分配陪诊师" ? common_vendor.e({
        F: getAvatarUrl(order.value.attendantAvatar),
        G: common_vendor.o(handleImageError),
        H: common_vendor.t(order.value.attendantName),
        I: order.value.attendantPhone
      }, order.value.attendantPhone ? {
        J: common_assets._imports_0$4,
        K: common_vendor.t(order.value.attendantPhone)
      } : {}, {
        L: common_vendor.t(order.value.attendantScore || 5)
      }) : (order.value.orderStatus === 0 || order.value.orderStatus === 1) && order.value.paymentStatus === 1 ? common_vendor.e({
        N: order.value.orderStatus === 1 && order.value.cancelReason
      }, order.value.orderStatus === 1 && order.value.cancelReason ? {
        O: common_vendor.t(order.value.cancelReason)
      } : {}) : order.value.paymentStatus === 0 && order.value.orderStatus !== 7 ? {} : order.value.orderStatus === 7 ? {} : {}, {
        M: (order.value.orderStatus === 0 || order.value.orderStatus === 1) && order.value.paymentStatus === 1,
        P: order.value.paymentStatus === 0 && order.value.orderStatus !== 7,
        Q: order.value.orderStatus === 7,
        R: common_vendor.t(formatAmount(finalAmount.value)),
        S: common_vendor.t(formatAmount(prepayAmount.value)),
        T: hasDiff.value
      }, hasDiff.value ? {
        U: common_vendor.t(diffLabel.value),
        V: common_vendor.t(formatAmount(Math.abs(diffAmount.value)))
      } : {}, {
        W: order.value.priceCalculation
      }, order.value.priceCalculation ? {
        X: common_vendor.t(order.value.priceCalculation)
      } : {}, {
        Y: order.value.orderStatus === 7
      }, order.value.orderStatus === 7 ? {
        Z: common_vendor.t(order.value.cancelReason || "未填写"),
        aa: common_vendor.t(getCancelByText(order.value.cancelBy)),
        ab: common_vendor.t(order.value.cancelTime || "未知"),
        ac: common_vendor.t(formatAmount(order.value.penaltyAmount)),
        ad: common_vendor.t(formatAmount(order.value.refundAmount))
      } : {}, {
        ae: showQRCode.value
      }, showQRCode.value ? {
        af: order.value.qrCodeUrl
      } : {}, {
        ag: showDurationConfirm.value
      }, showDurationConfirm.value ? common_vendor.e({
        ah: common_vendor.t(formatDuration(order.value.estimatedDuration)),
        ai: common_vendor.t(order.value.serviceTypeName || getServiceTypeName(order.value.clinicType)),
        aj: common_vendor.t(formatDuration(order.value.actualDuration)),
        ak: common_vendor.t(diffDesc.value),
        al: order.value.balanceAmount && order.value.balanceAmount > 0 ? 1 : "",
        am: order.value.balanceAmount && order.value.balanceAmount < 0 ? 1 : "",
        an: order.value.balanceAmount && order.value.balanceAmount > 0
      }, order.value.balanceAmount && order.value.balanceAmount > 0 ? {
        ao: common_vendor.t(formatAmount(order.value.balanceAmount)),
        ap: balancePayMethod.value === "wechat",
        aq: balancePayMethod.value === "alipay",
        ar: common_vendor.o(onBalancePayMethodChange),
        as: balancePayMethod.value
      } : {}, {
        at: common_vendor.o(openDisputeModal),
        av: common_vendor.t(confirmBtnText.value),
        aw: common_vendor.o(confirmDuration)
      }) : {}, {
        ax: showBalancePayResultModal.value
      }, showBalancePayResultModal.value ? {
        ay: common_vendor.o(($event) => handleBalancePayResult(true)),
        az: common_vendor.o(($event) => handleBalancePayResult(false)),
        aA: common_vendor.o(() => {
        }),
        aB: common_vendor.o(($event) => showBalancePayResultModal.value = false)
      } : {}, {
        aC: showContactModal.value
      }, showContactModal.value ? {
        aD: common_vendor.o(($event) => showContactModal.value = false),
        aE: common_vendor.t(order.value.attendantPhone || "未提供电话"),
        aF: common_vendor.o(($event) => handleContactChoice("phone")),
        aG: common_vendor.o(($event) => handleContactChoice("chat")),
        aH: common_vendor.o(($event) => showContactModal.value = false),
        aI: common_vendor.o(() => {
        }),
        aJ: common_vendor.o(($event) => showContactModal.value = false)
      } : {}, {
        aK: order.value.orderStatus === 6 && userEvaluation.value
      }, order.value.orderStatus === 6 && userEvaluation.value ? common_vendor.e({
        aL: common_vendor.f(5, (i, k0, i0) => {
          return {
            a: i,
            b: i <= (userEvaluation.value.rating || 0) ? 1 : ""
          };
        }),
        aM: common_vendor.t((userEvaluation.value.rating || 0).toFixed(1)),
        aN: userEvaluation.value.content
      }, userEvaluation.value.content ? {
        aO: common_vendor.t(userEvaluation.value.content)
      } : {}, {
        aP: userEvaluation.value.tags
      }, userEvaluation.value.tags ? {
        aQ: common_vendor.f((userEvaluation.value.tags || "").split(","), (tag, i, i0) => {
          return {
            a: common_vendor.t(tag.trim()),
            b: i,
            c: tag
          };
        })
      } : {}, {
        aR: common_vendor.t(userEvaluation.value.attendantReply || "暂未回复"),
        aS: userEvaluation.value.attendantReply ? 1 : ""
      }) : {}, {
        aT: common_vendor.f(serviceSteps.value, (item, index, i0) => {
          return common_vendor.e({
            a: index !== serviceSteps.value.length - 1
          }, index !== serviceSteps.value.length - 1 ? {} : {}, {
            b: common_vendor.t(item.title),
            c: common_vendor.t(item.time || "--"),
            d: common_vendor.t(item.desc),
            e: index,
            f: common_vendor.n({
              "step-item-last": index === serviceSteps.value.length - 1
            })
          });
        }),
        aU: order.value.attendantPhone
      }, order.value.attendantPhone ? {
        aV: common_assets._imports_0$4,
        aW: common_vendor.o(callCompanion)
      } : {}, {
        aX: common_assets._imports_1$3,
        aY: common_vendor.o(consult),
        aZ: common_assets._imports_2$3,
        ba: common_vendor.o(share),
        bb: order.value.paymentStatus === 0 && order.value.orderStatus !== 7
      }, order.value.paymentStatus === 0 && order.value.orderStatus !== 7 ? {
        bc: common_assets._imports_3$2,
        bd: common_vendor.o(openCancelModal)
      } : {}, {
        be: primaryActionText.value
      }, primaryActionText.value ? {
        bf: common_vendor.t(primaryActionText.value),
        bg: common_vendor.o(handlePrimaryAction)
      } : {}, {
        bh: showDisputeModal.value
      }, showDisputeModal.value ? {
        bi: disputeDuration.value,
        bj: common_vendor.o(($event) => disputeDuration.value = $event.detail.value),
        bk: disputeReason.value,
        bl: common_vendor.o(($event) => disputeReason.value = $event.detail.value),
        bm: common_vendor.o(($event) => showDisputeModal.value = false),
        bn: common_vendor.o(submitDispute)
      } : {}, {
        bo: showCancelModal.value
      }, showCancelModal.value ? {
        bp: common_vendor.f(cancelReasons, (reason, k0, i0) => {
          return {
            a: common_vendor.t(reason),
            b: reason,
            c: selectedCancelReason.value === reason ? 1 : "",
            d: common_vendor.o(($event) => selectedCancelReason.value = reason, reason)
          };
        }),
        bq: cancelRemark.value,
        br: common_vendor.o(($event) => cancelRemark.value = $event.detail.value),
        bs: common_vendor.o(($event) => showCancelModal.value = false),
        bt: common_vendor.o(submitCancelOrder)
      } : {});
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-2e88df03"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/OrderDetailPage/OrderDetailPage.js.map

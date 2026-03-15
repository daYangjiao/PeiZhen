"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_api = require("../../utils/api.js");
const utils_chatWebsocket = require("../../utils/chat-websocket.js");
const common_assets = require("../../common/assets.js");
function fullAvatarUrl(path) {
  if (!path || path.startsWith("http") || path.startsWith("/static"))
    return path;
  const base = utils_api.config.baseURL.replace(/\/$/, "");
  return base + (path.startsWith("/") ? path : "/" + path);
}
const _sfc_main = {
  data() {
    return {
      isLoading: true,
      showSimulateModal: false,
      showCancelModal: false,
      showEndServiceModal: false,
      showPrepareModal: false,
      showContactPatientModal: false,
      simulateQrContent: "",
      cancelReason: "",
      cancelPenaltyRate: 0,
      cancelPenaltyAmount: "0.00",
      cancelRefundAmount: "0.00",
      cancelLeadTimeText: "",
      cancelPenaltyRateText: "0%",
      cancelSubmitting: false,
      socketListener: null,
      isPrepared: false,
      orderInfo: {
        id: "",
        orderNo: "",
        status: "accepted",
        patientAvatar: "/static/user-placeholder.png",
        patientName: "",
        patientAge: 0,
        patientGender: "",
        patientPhone: "",
        serviceType: "",
        hospital: "",
        appointmentTime: "",
        appointmentEndTime: "",
        duration: "",
        specialRequests: "",
        symptomDescription: "",
        otherRequirement: "",
        serviceFee: 0,
        platformFee: 0,
        totalFee: 0,
        cancelReason: "",
        cancelTime: "",
        cancelBy: null,
        penaltyAmount: 0,
        refundAmount: 0,
        serviceRecords: []
      },
      serviceFlowSteps: [
        { key: "arrived", label: "已到院" },
        { key: "waiting", label: "候诊中" },
        { key: "exam", label: "检查中" },
        { key: "finished", label: "就诊完成" }
      ],
      currentFlowStep: 1,
      evaluation: null,
      replyInput: ""
    };
  },
  computed: {
    statusIcon() {
      const icons = {
        pending: "/static/clock.svg",
        accepted: "/static/check.svg",
        in_progress: "/static/progress.svg",
        completed: "/static/success.svg",
        cancelled: "/static/cancel.svg"
      };
      return icons[this.orderInfo.status] || "/static/clock.svg";
    },
    canViewPatientContact() {
      const status = this.orderInfo.status;
      return status && status !== "pending";
    },
    statusText() {
      const texts = {
        pending: "待接单",
        accepted: "待服务",
        in_progress: "服务中",
        waiting_confirm: "待患者确认",
        disputed: "时长有争议",
        completed: "已完成",
        cancelled: "已取消"
      };
      return texts[this.orderInfo.status] || "未知状态";
    },
    statusDesc() {
      const descs = {
        pending: "等待陪诊师接单",
        accepted: "请按时到达指定地点",
        in_progress: "正在为患者提供陪诊服务",
        waiting_confirm: "已提交服务时长与费用，等待患者确认",
        disputed: "患者对本次时长与费用有异议，等待平台处理",
        completed: "服务已完成，感谢您的专业服务",
        cancelled: "订单已取消"
      };
      return descs[this.orderInfo.status] || "";
    },
    showActions() {
      return ["accepted", "in_progress"].includes(this.orderInfo.status);
    },
    mainActionText() {
      const texts = {
        accepted: "开始服务",
        in_progress: "结束服务"
      };
      return texts[this.orderInfo.status] || "";
    },
    serviceProgressStep() {
      if (this.orderInfo.status === "accepted")
        return 1;
      if (this.orderInfo.status === "in_progress")
        return 2;
      if (this.orderInfo.status === "waiting_confirm")
        return 3;
      if (this.orderInfo.status === "completed")
        return 4;
      return 1;
    },
    // 结算展示用数值（患者支付总额 / 平台服务费10% / 陪诊师实收90%）
    settlementTotal() {
      const raw = Number(this.orderInfo.totalFee || this.orderInfo.serviceFee || 0);
      return isNaN(raw) ? 0 : raw;
    },
    settlementTotalText() {
      return this.settlementTotal.toFixed(2);
    },
    platformServiceFeeText() {
      const fee = this.settlementTotal * 0.1;
      return fee.toFixed(2);
    },
    attendantIncomeText() {
      const income = this.settlementTotal * 0.9;
      return income.toFixed(2);
    },
    // 费用信息区块的陪诊师实收（未完成订单）
    feeInfoAttendantIncomeText() {
      const raw = Number(this.orderInfo.totalFee || this.orderInfo.serviceFee || 0);
      return (raw * 0.9).toFixed(2);
    }
  },
  onLoad(options) {
    if (options.orderId) {
      this.loadOrderDetail(options.orderId);
    }
    this.setupWebSocketListener();
  },
  onShow() {
    this.refreshPreparedState();
    if (this.orderInfo && this.orderInfo.id) {
      this.loadOrderDetail(this.orderInfo.id);
    }
  },
  beforeDestroy() {
    if (this.socketListener) {
      utils_chatWebsocket.removeChatListener(this.socketListener);
    }
  },
  methods: {
    refreshPreparedState() {
      if (!this.orderInfo.id)
        return;
      this.isPrepared = common_vendor.index.getStorageSync(`order_prepared_${this.orderInfo.id}`) === "1";
    },
    async loadEvaluation(orderId) {
      if (!orderId)
        return;
      try {
        const res = await utils_api.get(`/attendant/orders/${orderId}/evaluation`);
        if (res.code === 200 && res.data) {
          this.evaluation = res.data;
          this.replyInput = res.data.attendantReply || "";
        } else {
          this.evaluation = null;
        }
      } catch (e) {
        common_vendor.index.__f__("error", "at subpkg/order/detail.vue:659", "加载评价失败", e);
        this.evaluation = null;
      }
    },
    async submitReply() {
      const content = (this.replyInput || "").trim();
      if (!content) {
        common_vendor.index.showToast({ title: "请输入回复内容", icon: "none" });
        return;
      }
      if (!this.orderInfo.id)
        return;
      try {
        common_vendor.index.showLoading({ title: "提交中..." });
        const res = await utils_api.post(`/attendant/orders/${this.orderInfo.id}/evaluation/reply`, { reply: content });
        common_vendor.index.hideLoading();
        if (res.code === 200) {
          common_vendor.index.showToast({ title: "回复成功", icon: "success" });
          this.evaluation = { ...this.evaluation, attendantReply: content, replyTime: (/* @__PURE__ */ new Date()).toISOString() };
          this.replyInput = "";
        } else {
          common_vendor.index.showToast({ title: res.message || "回复失败", icon: "none" });
        }
      } catch (e) {
        common_vendor.index.hideLoading();
        common_vendor.index.__f__("error", "at subpkg/order/detail.vue:683", "回复失败", e);
        common_vendor.index.showToast({ title: "回复失败", icon: "none" });
      }
    },
    flowItemClass(stepIndex) {
      if (this.orderInfo.status === "completed")
        return "completed";
      if (this.currentFlowStep === stepIndex)
        return "current";
      if (this.currentFlowStep > stepIndex)
        return "past";
      return "future";
    },
    updateFlowStep(stepIndex) {
      if (this.orderInfo.status !== "in_progress")
        return;
      this.currentFlowStep = stepIndex;
      if (!this.orderInfo.id)
        return;
      common_vendor.index.setStorageSync(`order_flow_step_${this.orderInfo.id}`, stepIndex);
      utils_api.post(`/attendant/orders/${this.orderInfo.id}/service-progress?step=${stepIndex}`).then((res) => {
        if (res.code !== 200) {
          common_vendor.index.__f__("warn", "at subpkg/order/detail.vue:703", "更新服务进度失败:", res.message);
        }
      }).catch((err) => {
        common_vendor.index.__f__("error", "at subpkg/order/detail.vue:707", "更新服务进度异常:", err);
      });
    },
    applyIntervention() {
      common_vendor.index.showToast({
        title: "申请介入功能暂未开通，可先联系客服",
        icon: "none"
      });
    },
    // 返回
    goBack() {
      common_vendor.index.navigateBack();
    },
    // 加载订单详情
    async loadOrderDetail(orderId) {
      this.isLoading = true;
      try {
        common_vendor.index.__f__("log", "at subpkg/order/detail.vue:725", "正在加载订单详情, ID:", orderId);
        const res = await utils_api.get(`/attendant/orders/${orderId}`);
        common_vendor.index.__f__("log", "at subpkg/order/detail.vue:727", "订单详情响应:", res);
        if (res.code === 200 && res.data) {
          const order = res.data;
          let status = "pending";
          if (order.orderStatus === 2)
            status = "accepted";
          else if (order.orderStatus === 3)
            status = "in_progress";
          else if (order.orderStatus === 4)
            status = "waiting_confirm";
          else if (order.orderStatus === 5)
            status = "disputed";
          else if (order.orderStatus === 6)
            status = "completed";
          else if (order.orderStatus === 7)
            status = "cancelled";
          const placeholder = "/static/user-placeholder.png";
          const showUserAvatar = order.orderStatus !== 1 && order.userAvatar;
          this.orderInfo = {
            id: order.orderId,
            orderNo: order.orderNo,
            userId: order.userId,
            status,
            patientName: order.patientName || order.contactPerson,
            patientAge: order.patientAge || "--",
            patientGender: order.patientSex || "未知",
            patientPhone: order.contactPhone || order.userPhone,
            patientAvatar: showUserAvatar ? fullAvatarUrl(order.userAvatar) : placeholder,
            serviceType: order.serviceContent || order.serviceTypeName,
            hospital: order.hospital,
            appointmentTime: (order.serviceDate || "") + " " + (order.serviceTimeSlot || ""),
            duration: order.consultationDuration ? order.consultationDuration + "小时" : "2小时",
            symptomDescription: order.specialRequirements || "",
            otherRequirement: order.customRequirement && order.customRequirement !== "无" ? order.customRequirement : "",
            specialRequests: !order.specialRequirements && (!order.customRequirement || order.customRequirement === "无") ? "无特殊要求" : "",
            serviceFee: order.orderAmount,
            totalFee: order.orderAmount,
            cancelReason: order.cancelReason || "",
            cancelTime: order.cancelTime || "",
            cancelBy: order.cancelBy,
            penaltyAmount: order.penaltyAmount || 0,
            refundAmount: order.refundAmount || 0,
            estimatedDuration: order.estimatedDuration,
            actualDuration: order.actualDuration,
            balanceAmount: order.balanceAmount,
            serviceRecords: []
            // 暂时为空，后续可从后端获取
          };
          const stored = common_vendor.index.getStorageSync(`order_flow_step_${order.orderId}`);
          if (stored) {
            this.currentFlowStep = Number(stored) || 1;
          } else {
            if (order.orderStatus === 3)
              this.currentFlowStep = 2;
            else if (order.orderStatus === 6)
              this.currentFlowStep = 4;
            else
              this.currentFlowStep = 1;
          }
          this.simulateQrContent = `SERVICE_CONFIRM_${order.orderId}`;
          this.refreshPreparedState();
          if (status === "completed") {
            this.loadEvaluation(order.orderId);
          } else {
            this.evaluation = null;
          }
        } else {
          const msg = res.message || "订单不存在";
          common_vendor.index.__f__("error", "at subpkg/order/detail.vue:792", "加载失败:", msg);
          common_vendor.index.showToast({ title: msg, icon: "none" });
          setTimeout(() => common_vendor.index.navigateBack(), 1500);
        }
      } catch (error) {
        common_vendor.index.__f__("error", "at subpkg/order/detail.vue:797", "加载订单详情异常:", error);
        common_vendor.index.showToast({ title: "网络错误", icon: "none" });
      } finally {
        this.isLoading = false;
      }
    },
    goToPrepare() {
      common_vendor.index.navigateTo({
        url: `/subpkg/order/prepare?orderId=${this.orderInfo.id}&hospital=${encodeURIComponent(this.orderInfo.hospital || "")}&symptom=${encodeURIComponent(this.orderInfo.symptomDescription || "")}&other=${encodeURIComponent(this.orderInfo.otherRequirement || "")}`
      });
    },
    openContactPatientModal() {
      this.showContactPatientModal = true;
    },
    handleContactPatientChoice(type) {
      this.showContactPatientModal = false;
      if (type === "phone") {
        if (this.orderInfo.patientPhone) {
          common_vendor.index.makePhoneCall({ phoneNumber: this.orderInfo.patientPhone });
        } else {
          common_vendor.index.showToast({ title: "暂无患者电话", icon: "none" });
        }
      } else if (type === "chat") {
        common_vendor.index.navigateTo({
          url: `/subpkg/chat/chat?userId=${this.orderInfo.userId}&name=${encodeURIComponent(this.orderInfo.patientName || "患者")}`
        });
      }
    },
    contactPatient() {
      this.openContactPatientModal();
    },
    onScanCodeClick() {
      if (this.orderInfo.status !== "accepted")
        return;
      if (!this.isPrepared) {
        this.showPrepareModal = true;
        return;
      }
      this.scanCode();
    },
    onSimulateScanClick() {
      if (this.orderInfo.status !== "accepted")
        return;
      if (!this.isPrepared) {
        this.showPrepareModal = true;
        return;
      }
      this.showSimulateModal = true;
    },
    scanCode() {
      common_vendor.index.scanCode({
        success: (res) => {
          this.verifyQrCode(res.result);
        },
        fail: (err) => {
          common_vendor.index.__f__("error", "at subpkg/order/detail.vue:851", "扫码失败", err);
          common_vendor.index.showToast({ title: "扫码失败", icon: "none" });
        }
      });
    },
    // 模拟扫码
    handleSimulateScan() {
      if (!this.simulateQrContent) {
        common_vendor.index.showToast({ title: "请输入内容", icon: "none" });
        return;
      }
      this.showSimulateModal = false;
      this.verifyQrCode(this.simulateQrContent);
    },
    // 验证二维码并开始服务
    async verifyQrCode(content) {
      common_vendor.index.showLoading({ title: "核销中..." });
      try {
        const res = await utils_api.post(`/attendant/orders/${this.orderInfo.id}/scan-qr?qrCodeContent=${encodeURIComponent(content)}`);
        common_vendor.index.hideLoading();
        if (res.code === 200) {
          common_vendor.index.showToast({ title: "核销成功", icon: "success" });
          this.orderInfo.status = "in_progress";
          this.loadOrderDetail(this.orderInfo.id);
        } else {
          common_vendor.index.showToast({ title: res.message || "核销失败", icon: "none" });
        }
      } catch (e) {
        common_vendor.index.hideLoading();
        common_vendor.index.__f__("error", "at subpkg/order/detail.vue:885", "核销异常:", e);
        common_vendor.index.showToast({ title: "核销异常", icon: "none" });
      }
    },
    // 主要操作 (保留兼容性)
    handleMainAction() {
      if (this.orderInfo.status === "accepted") {
        common_vendor.index.showToast({ title: "请点击扫码核销", icon: "none" });
      } else if (this.orderInfo.status === "in_progress") {
        this.completeService();
      }
    },
    // 打开取消订单弹窗并计算违约金
    openCancelModal() {
      this.cancelReason = "";
      this.calculateCancelPenalty();
      this.showCancelModal = true;
    },
    // 计算取消违约金（按规则）
    calculateCancelPenalty() {
      const amount = Number(this.orderInfo.totalFee || this.orderInfo.serviceFee || 0);
      const startAt = this.parseServiceStartTime();
      const now = /* @__PURE__ */ new Date();
      let rate = 1;
      let diffMinutes = -1;
      if (startAt) {
        diffMinutes = Math.floor((startAt.getTime() - now.getTime()) / 6e4);
        if (diffMinutes > 120) {
          rate = 0;
        } else if (diffMinutes > 60) {
          rate = 0.2;
        } else if (diffMinutes > 30) {
          rate = 0.3;
        } else if (diffMinutes >= 0) {
          rate = 0.5;
        } else {
          rate = 1;
        }
      }
      const penalty = amount * rate;
      this.cancelPenaltyRate = rate;
      this.cancelPenaltyRateText = `${Math.round(rate * 100)}%`;
      this.cancelPenaltyAmount = penalty.toFixed(2);
      this.cancelRefundAmount = amount.toFixed(2);
      this.cancelLeadTimeText = this.formatLeadTime(diffMinutes);
    },
    parseServiceStartTime() {
      const text = this.orderInfo.appointmentTime || "";
      const match = text.match(/(\d{4}-\d{2}-\d{2})\s+(\d{2}:\d{2})/);
      if (!match)
        return null;
      return /* @__PURE__ */ new Date(`${match[1]}T${match[2]}:00`);
    },
    formatLeadTime(diffMinutes) {
      if (diffMinutes < 0)
        return "已超过开始时间";
      const h = Math.floor(diffMinutes / 60);
      const m = diffMinutes % 60;
      if (h > 0)
        return `${h}小时${m}分钟`;
      return `${m}分钟`;
    },
    getCancelByText(cancelBy) {
      if (cancelBy === 1)
        return "陪诊师";
      if (cancelBy === 0)
        return "用户";
      return "系统";
    },
    formatCancelTime(val) {
      if (!val)
        return "";
      if (typeof val === "string" && val.length >= 16) {
        if (/^\d{4}-\d{2}-\d{2}/.test(val))
          return val.substring(0, 19).replace("T", " ");
        const i = val.indexOf("T");
        if (i !== -1)
          return val.substring(0, 19).replace("T", " ");
      }
      if (typeof val === "number" && val > 0) {
        const d = new Date(val);
        return d.getFullYear() + "-" + String(d.getMonth() + 1).padStart(2, "0") + "-" + String(d.getDate()).padStart(2, "0") + " " + String(d.getHours()).padStart(2, "0") + ":" + String(d.getMinutes()).padStart(2, "0") + ":" + String(d.getSeconds()).padStart(2, "0");
      }
      return val;
    },
    // 取消订单（待核销/待服务）
    handleCancelOrder() {
      if (this.cancelSubmitting)
        return;
      if (!this.cancelReason || !this.cancelReason.trim()) {
        common_vendor.index.showToast({ title: "请输入取消原因", icon: "none" });
        return;
      }
      this.cancelSubmitting = true;
      cancelAttendantOrder(this.orderInfo.id, {
        reason: this.cancelReason.trim(),
        penaltyAmount: this.cancelPenaltyAmount,
        refundAmount: this.cancelRefundAmount,
        penaltyRate: this.cancelPenaltyRate
      }).then((apiRes) => {
        if (apiRes.code === 200) {
          this.showCancelModal = false;
          let msg = "您已取消接单";
          const data = apiRes.data;
          if (data && typeof data === "string" && data.includes("释放回接单大厅")) {
            msg = "取消成功，订单已重新开放给其他陪诊师";
          } else if (data && typeof data === "string") {
            msg = "订单已取消";
          }
          common_vendor.index.showToast({ title: msg, icon: "success" });
          setTimeout(() => common_vendor.index.navigateBack(), 1500);
        } else {
          common_vendor.index.showToast({ title: apiRes.message || "取消失败", icon: "none" });
        }
      }).catch((e) => {
        common_vendor.index.__f__("error", "at subpkg/order/detail.vue:1006", "取消订单异常:", e);
        common_vendor.index.showToast({ title: "取消订单失败", icon: "none" });
      }).finally(() => {
        this.cancelSubmitting = false;
      });
    },
    // 完成服务
    goToSubmitTime() {
      common_vendor.index.navigateTo({
        url: `/subpkg/order/submit-time-fee?orderId=${this.orderInfo.id}`
      });
    },
    // 点击结束服务：先校验服务进度
    handleEndServiceClick() {
      if (this.orderInfo.status !== "in_progress") {
        return;
      }
      if (this.currentFlowStep >= 4) {
        this.goToSubmitTime();
        return;
      }
      this.showEndServiceModal = true;
    },
    // 在弹窗中确认：自动把服务进度更新为「就诊完成」，再进入提交时长页面
    async confirmEndServiceWithProgress() {
      this.showEndServiceModal = false;
      const finalStep = 4;
      this.currentFlowStep = finalStep;
      if (this.orderInfo && this.orderInfo.id) {
        common_vendor.index.setStorageSync(`order_flow_step_${this.orderInfo.id}`, finalStep);
        try {
          const res = await utils_api.post(`/attendant/orders/${this.orderInfo.id}/service-progress?step=${finalStep}`);
          if (res && res.code !== 200) {
            common_vendor.index.__f__("warn", "at subpkg/order/detail.vue:1046", "一键更新服务进度为就诊完成失败:", res.message);
          }
        } catch (e) {
          common_vendor.index.__f__("error", "at subpkg/order/detail.vue:1049", "一键更新服务进度为就诊完成异常:", e);
        }
      }
      this.goToSubmitTime();
    },
    // 格式化预约时间为时间段
    formatAppointmentTime() {
      return this.orderInfo.appointmentTime || "时间待定";
    },
    // WebSocket消息处理
    handleSocketMessage(message) {
      common_vendor.index.__f__("log", "at subpkg/order/detail.vue:1063", "订单详情页收到WebSocket消息:", message);
      const isCurrentOrder = message.orderId === this.orderInfo.id || message.orderNo === this.orderInfo.orderNo || message.data && (message.data.orderId === this.orderInfo.id || message.data.orderNo === this.orderInfo.orderNo);
      if (isCurrentOrder && (message.type === "SERVICE_STARTED" || message.type === "SERVICE_COMPLETED" || message.type === "ORDER_STATUS_CHANGED")) {
        common_vendor.index.__f__("log", "at subpkg/order/detail.vue:1075", "收到当前订单状态更新，刷新详情");
        setTimeout(() => {
          this.loadOrderDetail(this.orderInfo.id);
        }, 1e3);
      }
    },
    // 设置WebSocket监听
    setupWebSocketListener() {
      this.socketListener = this.handleSocketMessage.bind(this);
      utils_chatWebsocket.addChatListener(this.socketListener);
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: $data.isLoading
  }, $data.isLoading ? {} : common_vendor.e({
    b: ["accepted", "in_progress", "waiting_confirm"].includes($data.orderInfo.status)
  }, ["accepted", "in_progress", "waiting_confirm"].includes($data.orderInfo.status) ? common_vendor.e({
    c: common_vendor.t($data.orderInfo.orderNo),
    d: $options.serviceProgressStep >= 1 ? 1 : "",
    e: $options.serviceProgressStep === 1 ? 1 : "",
    f: $options.serviceProgressStep >= 2 ? 1 : "",
    g: $options.serviceProgressStep >= 2 ? 1 : "",
    h: $options.serviceProgressStep === 2 ? 1 : "",
    i: $options.serviceProgressStep >= 3 ? 1 : "",
    j: $options.serviceProgressStep >= 3 ? 1 : "",
    k: $options.serviceProgressStep === 3 ? 1 : "",
    l: $data.orderInfo.status === "accepted"
  }, $data.orderInfo.status === "accepted" ? common_vendor.e({
    m: common_vendor.t($data.isPrepared ? "已准备" : "未完成"),
    n: $data.isPrepared ? 1 : "",
    o: !$data.isPrepared
  }, !$data.isPrepared ? {
    p: common_vendor.o((...args) => $options.goToPrepare && $options.goToPrepare(...args))
  } : {}) : {}) : $data.orderInfo.status === "completed" ? common_vendor.e({
    r: common_vendor.t($options.attendantIncomeText),
    s: common_vendor.t($options.attendantIncomeText),
    t: common_vendor.t($options.settlementTotalText),
    v: common_vendor.t($options.platformServiceFeeText),
    w: common_vendor.t($data.orderInfo.orderNo),
    x: $data.evaluation
  }, $data.evaluation ? {
    y: common_vendor.f(5, (i, k0, i0) => {
      return {
        a: i,
        b: i <= ($data.evaluation.rating || 0) ? 1 : ""
      };
    }),
    z: common_vendor.t(($data.evaluation.rating || 0).toFixed(1))
  } : {}, {
    A: !$data.evaluation
  }, !$data.evaluation ? {} : common_vendor.e({
    B: $data.evaluation.content
  }, $data.evaluation.content ? {
    C: common_vendor.t($data.evaluation.content)
  } : {}, {
    D: $data.evaluation.tags
  }, $data.evaluation.tags ? {
    E: common_vendor.f(($data.evaluation.tags || "").split(","), (tag, i, i0) => {
      return {
        a: common_vendor.t(tag.trim()),
        b: i,
        c: tag
      };
    })
  } : {}, {
    F: common_vendor.t($data.evaluation.attendantReply || "暂无回复"),
    G: $data.evaluation.attendantReply ? 1 : "",
    H: $data.replyInput,
    I: common_vendor.o(($event) => $data.replyInput = $event.detail.value),
    J: common_vendor.o((...args) => $options.submitReply && $options.submitReply(...args))
  })) : {
    K: $options.statusIcon,
    L: common_vendor.t($options.statusText),
    M: common_vendor.t($options.statusDesc),
    N: common_vendor.t($data.orderInfo.orderNo)
  }, {
    q: $data.orderInfo.status === "completed",
    O: common_assets._imports_0$3,
    P: $data.orderInfo.patientAvatar,
    Q: common_vendor.o(($event) => $data.orderInfo.patientAvatar = "/static/user-placeholder.png"),
    R: common_vendor.t($data.orderInfo.patientName),
    S: common_vendor.t($data.orderInfo.patientAge),
    T: common_vendor.t($data.orderInfo.patientGender),
    U: $options.canViewPatientContact
  }, $options.canViewPatientContact ? {
    V: common_vendor.t($data.orderInfo.patientPhone)
  } : {}, {
    W: $options.canViewPatientContact
  }, $options.canViewPatientContact ? {
    X: common_assets._imports_1$2,
    Y: common_vendor.o((...args) => $options.openContactPatientModal && $options.openContactPatientModal(...args))
  } : {}, {
    Z: common_assets._imports_2$3,
    aa: common_vendor.t($data.orderInfo.serviceType),
    ab: common_vendor.t($data.orderInfo.hospital),
    ac: common_vendor.t($options.formatAppointmentTime()),
    ad: $data.orderInfo.appointmentEndTime
  }, $data.orderInfo.appointmentEndTime ? {
    ae: common_vendor.t($data.orderInfo.appointmentEndTime)
  } : {}, {
    af: $data.orderInfo.duration
  }, $data.orderInfo.duration ? {
    ag: common_vendor.t($data.orderInfo.duration)
  } : {}, {
    ah: $data.orderInfo.status === "in_progress"
  }, $data.orderInfo.status === "in_progress" ? {
    ai: common_assets._imports_3$1,
    aj: common_vendor.f($data.serviceFlowSteps, (step, index, i0) => {
      return common_vendor.e({
        a: common_vendor.t(step.label)
      }, $data.orderInfo.status === "completed" ? {} : $data.currentFlowStep === index + 1 && index + 1 !== $data.serviceFlowSteps.length ? {} : $data.currentFlowStep === index + 1 && index + 1 === $data.serviceFlowSteps.length ? {} : $data.currentFlowStep < index + 1 ? {
        e: common_vendor.o(($event) => $options.updateFlowStep(index + 1), step.key)
      } : {}, {
        b: $data.currentFlowStep === index + 1 && index + 1 !== $data.serviceFlowSteps.length,
        c: $data.currentFlowStep === index + 1 && index + 1 === $data.serviceFlowSteps.length,
        d: $data.currentFlowStep < index + 1,
        f: step.key,
        g: common_vendor.n($options.flowItemClass(index + 1))
      });
    }),
    ak: $data.orderInfo.status === "completed"
  } : {}, {
    al: $data.orderInfo.symptomDescription
  }, $data.orderInfo.symptomDescription ? {
    am: common_assets._imports_3$2,
    an: common_vendor.t($data.orderInfo.symptomDescription)
  } : {}, {
    ao: $data.orderInfo.otherRequirement
  }, $data.orderInfo.otherRequirement ? {
    ap: common_assets._imports_4$1,
    aq: common_vendor.t($data.orderInfo.otherRequirement)
  } : $data.orderInfo.specialRequests ? {
    as: common_assets._imports_6,
    at: common_vendor.t($data.orderInfo.specialRequests)
  } : {}, {
    ar: $data.orderInfo.specialRequests,
    av: $data.orderInfo.status !== "completed"
  }, $data.orderInfo.status !== "completed" ? {
    aw: common_assets._imports_7,
    ax: common_vendor.t($options.feeInfoAttendantIncomeText)
  } : {}, {
    ay: $data.orderInfo.status === "waiting_confirm"
  }, $data.orderInfo.status === "waiting_confirm" ? {
    az: common_vendor.t($data.orderInfo.duration || "—"),
    aA: common_vendor.t($data.orderInfo.actualDuration ? $data.orderInfo.actualDuration + "小时" : "—"),
    aB: common_vendor.t($data.orderInfo.balanceAmount == null ? "—" : $data.orderInfo.balanceAmount >= 0 ? "需补付¥" + Number($data.orderInfo.balanceAmount).toFixed(2) : "自动退款¥" + Number(-$data.orderInfo.balanceAmount).toFixed(2)),
    aC: common_vendor.o((...args) => $options.contactPatient && $options.contactPatient(...args)),
    aD: common_vendor.o((...args) => $options.applyIntervention && $options.applyIntervention(...args)),
    aE: common_vendor.o((...args) => $options.goToSubmitTime && $options.goToSubmitTime(...args))
  } : {}, {
    aF: $data.orderInfo.status === "cancelled"
  }, $data.orderInfo.status === "cancelled" ? {
    aG: common_assets._imports_8,
    aH: common_vendor.t($data.orderInfo.cancelReason || "未填写"),
    aI: common_vendor.t($options.getCancelByText($data.orderInfo.cancelBy)),
    aJ: common_vendor.t($options.formatCancelTime($data.orderInfo.cancelTime) || "未知"),
    aK: common_vendor.t(Number($data.orderInfo.penaltyAmount || 0).toFixed(2))
  } : {}, {
    aL: $data.orderInfo.serviceRecords && $data.orderInfo.serviceRecords.length > 0
  }, $data.orderInfo.serviceRecords && $data.orderInfo.serviceRecords.length > 0 ? {
    aM: common_assets._imports_9,
    aN: common_vendor.f($data.orderInfo.serviceRecords, (record, index, i0) => {
      return {
        a: common_vendor.t(record.time),
        b: common_vendor.t(record.content),
        c: index
      };
    })
  } : {}, {
    aO: $data.orderInfo.status === "accepted"
  }, $data.orderInfo.status === "accepted" ? {
    aP: $data.cancelSubmitting,
    aQ: common_vendor.o((...args) => $options.openCancelModal && $options.openCancelModal(...args))
  } : {}), {
    aR: $options.showActions
  }, $options.showActions ? common_vendor.e({
    aS: common_vendor.o((...args) => $options.contactPatient && $options.contactPatient(...args)),
    aT: $data.orderInfo.status === "accepted"
  }, $data.orderInfo.status === "accepted" ? {
    aU: common_vendor.n($data.isPrepared ? "primary" : "disabled"),
    aV: common_vendor.o((...args) => $options.onScanCodeClick && $options.onScanCodeClick(...args)),
    aW: common_vendor.n($data.isPrepared ? "warning" : "disabled"),
    aX: common_vendor.o((...args) => $options.onSimulateScanClick && $options.onSimulateScanClick(...args))
  } : $data.orderInfo.status === "in_progress" ? {
    aZ: common_vendor.o((...args) => $options.handleEndServiceClick && $options.handleEndServiceClick(...args))
  } : {}, {
    aY: $data.orderInfo.status === "in_progress"
  }) : {}, {
    ba: $data.showPrepareModal
  }, $data.showPrepareModal ? {
    bb: common_vendor.o(($event) => $data.showPrepareModal = false),
    bc: common_vendor.o(($event) => {
      $data.showPrepareModal = false;
      $options.goToPrepare();
    }),
    bd: common_vendor.o(() => {
    }),
    be: common_vendor.o(($event) => $data.showPrepareModal = false)
  } : {}, {
    bf: $data.showSimulateModal
  }, $data.showSimulateModal ? {
    bg: $data.simulateQrContent,
    bh: common_vendor.o(($event) => $data.simulateQrContent = $event.detail.value),
    bi: common_vendor.t($data.orderInfo.id),
    bj: common_vendor.o(($event) => $data.showSimulateModal = false),
    bk: common_vendor.o((...args) => $options.handleSimulateScan && $options.handleSimulateScan(...args)),
    bl: common_vendor.o(() => {
    }),
    bm: common_vendor.o(($event) => $data.showSimulateModal = false)
  } : {}, {
    bn: $data.showCancelModal
  }, $data.showCancelModal ? common_vendor.e({
    bo: $data.cancelReason,
    bp: common_vendor.o(($event) => $data.cancelReason = $event.detail.value),
    bq: common_vendor.t($data.cancelLeadTimeText),
    br: $data.cancelPenaltyRate > 0
  }, $data.cancelPenaltyRate > 0 ? {
    bs: common_vendor.t($data.cancelPenaltyRateText)
  } : {}, {
    bt: $data.cancelPenaltyRate > 0
  }, $data.cancelPenaltyRate > 0 ? {
    bv: common_vendor.t($data.cancelPenaltyAmount)
  } : {}, {
    bw: $data.cancelPenaltyRate > 0
  }, $data.cancelPenaltyRate > 0 ? {
    bx: common_vendor.t($data.cancelRefundAmount)
  } : {}, {
    by: $data.cancelPenaltyRate > 0
  }, $data.cancelPenaltyRate > 0 ? {} : {}, {
    bz: common_vendor.o(($event) => $data.showCancelModal = false),
    bA: $data.cancelSubmitting,
    bB: common_vendor.o((...args) => $options.handleCancelOrder && $options.handleCancelOrder(...args)),
    bC: common_vendor.o(() => {
    }),
    bD: common_vendor.o(($event) => $data.showCancelModal = false)
  }) : {}, {
    bE: $data.showContactPatientModal
  }, $data.showContactPatientModal ? {
    bF: common_vendor.o(($event) => $data.showContactPatientModal = false),
    bG: common_vendor.t($data.orderInfo.patientPhone || "未提供电话"),
    bH: common_vendor.o(($event) => $options.handleContactPatientChoice("phone")),
    bI: common_vendor.o(($event) => $options.handleContactPatientChoice("chat")),
    bJ: common_vendor.o(($event) => $data.showContactPatientModal = false),
    bK: common_vendor.o(() => {
    }),
    bL: common_vendor.o(($event) => $data.showContactPatientModal = false)
  } : {}, {
    bM: $data.showEndServiceModal
  }, $data.showEndServiceModal ? {
    bN: common_vendor.o(($event) => $data.showEndServiceModal = false),
    bO: common_vendor.o((...args) => $options.confirmEndServiceWithProgress && $options.confirmEndServiceWithProgress(...args)),
    bP: common_vendor.o(() => {
    }),
    bQ: common_vendor.o(($event) => $data.showEndServiceModal = false)
  } : {});
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/subpkg/order/detail.js.map

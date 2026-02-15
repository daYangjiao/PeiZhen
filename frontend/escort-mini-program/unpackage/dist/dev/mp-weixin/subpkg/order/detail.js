"use strict";
const common_vendor = require("../../common/vendor.js");
const common_assets = require("../../common/assets.js");
const _sfc_main = {
  data() {
    return {
      isLoading: true,
      orderInfo: {
        id: "",
        orderNo: "PZ202311150001",
        status: "accepted",
        // pending, accepted, in_progress, completed, cancelled
        patientAvatar: "/static/avatar1.png",
        patientName: "患者",
        patientAge: 0,
        patientGender: "",
        patientPhone: "",
        serviceType: "",
        hospital: "",
        appointmentTime: "",
        appointmentEndTime: "",
        duration: "",
        specialRequests: "",
        serviceFee: 0,
        platformFee: 0,
        totalFee: 0,
        serviceRecords: []
      },
      processSteps: [
        {
          title: "前往医院",
          time: "09:00",
          desc: "到达指定地点与患者会合",
          completed: true,
          current: false
        },
        {
          title: "协助挂号",
          time: "09:30",
          desc: "帮助患者完成挂号手续",
          completed: true,
          current: false
        },
        {
          title: "陪同就诊",
          time: "10:00",
          desc: "陪同患者前往科室就诊",
          completed: true,
          current: true
        },
        {
          title: "协助检查",
          desc: "陪同患者完成各项检查",
          completed: false,
          current: false
        },
        {
          title: "取药/报告",
          desc: "协助患者取药或取检查报告",
          completed: false,
          current: false
        },
        {
          title: "服务完成",
          desc: "确认患者安全离开医院",
          completed: false,
          current: false
        }
      ]
    };
  },
  computed: {
    statusIcon() {
      const icons = {
        pending: "/static/clock.png",
        accepted: "/static/check.png",
        in_progress: "/static/progress.png",
        completed: "/static/success.png",
        cancelled: "/static/cancel.png"
      };
      return icons[this.orderInfo.status] || "/static/clock.png";
    },
    statusText() {
      const texts = {
        pending: "待接单",
        accepted: "已接单",
        in_progress: "服务中",
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
        in_progress: "完成服务"
      };
      return texts[this.orderInfo.status] || "";
    }
  },
  onLoad(options) {
    if (options.orderId) {
      this.loadOrderDetail(options.orderId);
    }
  },
  methods: {
    // 返回
    goBack() {
      common_vendor.index.navigateBack();
    },
    // 加载订单详情
    loadOrderDetail(orderId) {
      try {
        let acceptedOrders = common_vendor.index.getStorageSync("acceptedOrders") || [];
        const order = acceptedOrders.find((item) => {
          return String(item.id) === String(orderId);
        });
        if (order) {
          this.orderInfo = {
            ...this.orderInfo,
            orderNo: order.orderNo || `PZ${Date.now()}`,
            id: order.id,
            patientName: order.userName || "患者",
            patientAge: order.userAge || 0,
            patientGender: order.userGender || "",
            patientPhone: order.phone || "",
            patientAvatar: order.userAvatar || "/static/avatar1.png",
            serviceType: order.serviceType || "",
            hospital: order.hospitalName || "",
            appointmentTime: order.appointmentTime || "",
            appointmentEndTime: order.appointmentEndTime || "",
            duration: order.duration || "2小时",
            specialRequests: order.specialNote || "",
            serviceFee: order.price || 0,
            platformFee: order.platformFee || 0,
            totalFee: order.price || 0,
            status: order.status || "accepted",
            serviceRecords: order.serviceRecords || []
          };
          this.isLoading = false;
        } else {
          common_vendor.index.showToast({
            title: "订单不存在",
            icon: "none"
          });
          setTimeout(() => {
            common_vendor.index.navigateBack();
          }, 1500);
        }
      } catch (error) {
        common_vendor.index.__f__("error", "at subpkg/order/detail.vue:333", "加载订单详情失败:", error);
        this.isLoading = false;
        common_vendor.index.showToast({
          title: "加载失败",
          icon: "none"
        });
        setTimeout(() => {
          common_vendor.index.navigateBack();
        }, 1500);
      }
    },
    // 联系患者
    contactPatient() {
      common_vendor.index.showActionSheet({
        itemList: ["拨打电话", "发送消息"],
        success: (res) => {
          if (res.tapIndex === 0) {
            common_vendor.index.makePhoneCall({
              phoneNumber: this.orderInfo.patientPhone.replace(/\*/g, "")
            });
          } else if (res.tapIndex === 1) {
            common_vendor.index.navigateTo({
              url: `/pages/chat/chat?userId=${this.orderInfo.patientId}&name=${this.orderInfo.patientName}`
            });
          }
        }
      });
    },
    // 主要操作
    handleMainAction() {
      if (this.orderInfo.status === "accepted") {
        this.startService();
      } else if (this.orderInfo.status === "in_progress") {
        this.completeService();
      }
    },
    // 开始服务
    startService() {
      common_vendor.index.showModal({
        title: "确认开始服务",
        content: `确认已到达指定地点，开始为${this.orderInfo.patientName}提供陪诊服务？`,
        success: (res) => {
          if (res.confirm) {
            this.orderInfo.status = "in_progress";
            let acceptedOrders = common_vendor.index.getStorageSync("acceptedOrders") || [];
            const orderIndex = acceptedOrders.findIndex((order) => order.id == this.orderInfo.id);
            if (orderIndex > -1) {
              acceptedOrders[orderIndex].status = "in_progress";
              common_vendor.index.setStorageSync("acceptedOrders", acceptedOrders);
            }
            common_vendor.index.$emit("orderStatusUpdated", {
              orderId: this.orderInfo.id,
              status: "in_progress"
            });
            common_vendor.index.showToast({
              title: "服务已开始",
              icon: "success"
            });
          }
        }
      });
    },
    // 完成服务
    completeService() {
      common_vendor.index.showModal({
        title: "确认完成服务",
        content: `确认已完成${this.orderInfo.patientName}的所有陪诊服务？`,
        success: (res) => {
          if (res.confirm) {
            this.orderInfo.status = "completed";
            let acceptedOrders = common_vendor.index.getStorageSync("acceptedOrders") || [];
            const orderIndex = acceptedOrders.findIndex((order) => order.id == this.orderInfo.id);
            if (orderIndex > -1) {
              acceptedOrders[orderIndex].status = "completed";
              common_vendor.index.setStorageSync("acceptedOrders", acceptedOrders);
            }
            common_vendor.index.$emit("orderStatusUpdated", {
              orderId: this.orderInfo.id,
              status: "completed"
            });
            common_vendor.index.showToast({
              title: "服务已完成",
              icon: "success",
              duration: 2e3
            });
            setTimeout(() => {
              common_vendor.index.switchTab({
                url: "/pages/order/order"
              });
            }, 2e3);
          }
        }
      });
    },
    // 格式化预约时间为时间段
    formatAppointmentTime() {
      if (!this.orderInfo.appointmentTime || !this.orderInfo.appointmentEndTime) {
        return this.orderInfo.appointmentTime || "";
      }
      const startTimeStr = this.orderInfo.appointmentTime.replace(/-/g, "/");
      const endTimeStr = this.orderInfo.appointmentEndTime.replace(/-/g, "/");
      const startDate = new Date(startTimeStr);
      const endDate = new Date(endTimeStr);
      const year = startDate.getFullYear();
      const month = String(startDate.getMonth() + 1).padStart(2, "0");
      const day = String(startDate.getDate()).padStart(2, "0");
      const startTime = `${String(startDate.getHours()).padStart(2, "0")}:${String(startDate.getMinutes()).padStart(2, "0")}`;
      const endTime = `${String(endDate.getHours()).padStart(2, "0")}:${String(endDate.getMinutes()).padStart(2, "0")}`;
      return `${year}-${month}-${day} ${startTime}-${endTime}`;
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: common_assets._imports_0$3,
    b: common_vendor.o((...args) => $options.goBack && $options.goBack(...args)),
    c: $data.isLoading
  }, $data.isLoading ? {} : common_vendor.e({
    d: $options.statusIcon,
    e: common_vendor.t($options.statusText),
    f: common_vendor.t($options.statusDesc),
    g: common_vendor.t($data.orderInfo.orderNo),
    h: common_assets._imports_1$1,
    i: $data.orderInfo.patientAvatar,
    j: common_vendor.t($data.orderInfo.patientName),
    k: common_vendor.t($data.orderInfo.patientAge),
    l: common_vendor.t($data.orderInfo.patientGender),
    m: common_vendor.t($data.orderInfo.patientPhone),
    n: common_assets._imports_1$2,
    o: common_vendor.o((...args) => $options.contactPatient && $options.contactPatient(...args)),
    p: common_assets._imports_3$2,
    q: common_vendor.t($data.orderInfo.serviceType),
    r: common_vendor.t($data.orderInfo.hospital),
    s: common_vendor.t($options.formatAppointmentTime()),
    t: $data.orderInfo.appointmentEndTime
  }, $data.orderInfo.appointmentEndTime ? {
    v: common_vendor.t($data.orderInfo.appointmentEndTime)
  } : {}, {
    w: $data.orderInfo.duration
  }, $data.orderInfo.duration ? {
    x: common_vendor.t($data.orderInfo.duration)
  } : {}, {
    y: $data.orderInfo.specialRequests
  }, $data.orderInfo.specialRequests ? {
    z: common_assets._imports_4$1,
    A: common_vendor.t($data.orderInfo.specialRequests)
  } : {}, {
    B: common_assets._imports_5$1,
    C: common_vendor.t($data.orderInfo.serviceFee),
    D: $data.orderInfo.platformFee
  }, $data.orderInfo.platformFee ? {
    E: common_vendor.t($data.orderInfo.platformFee)
  } : {}, {
    F: common_vendor.t($data.orderInfo.totalFee),
    G: $data.orderInfo.status === "in_progress" || $data.orderInfo.status === "completed"
  }, $data.orderInfo.status === "in_progress" || $data.orderInfo.status === "completed" ? {
    H: common_assets._imports_6,
    I: common_vendor.f($data.processSteps, (step, index, i0) => {
      return common_vendor.e({
        a: common_vendor.t(step.title),
        b: step.time
      }, step.time ? {
        c: common_vendor.t(step.time)
      } : {}, {
        d: step.desc
      }, step.desc ? {
        e: common_vendor.t(step.desc)
      } : {}, {
        f: index,
        g: step.completed ? 1 : "",
        h: step.current ? 1 : ""
      });
    })
  } : {}, {
    J: $data.orderInfo.serviceRecords && $data.orderInfo.serviceRecords.length > 0
  }, $data.orderInfo.serviceRecords && $data.orderInfo.serviceRecords.length > 0 ? {
    K: common_assets._imports_7,
    L: common_vendor.f($data.orderInfo.serviceRecords, (record, index, i0) => {
      return {
        a: common_vendor.t(record.time),
        b: common_vendor.t(record.content),
        c: index
      };
    })
  } : {}), {
    M: $options.showActions
  }, $options.showActions ? {
    N: common_vendor.o((...args) => $options.contactPatient && $options.contactPatient(...args)),
    O: common_vendor.t($options.mainActionText),
    P: common_vendor.o((...args) => $options.handleMainAction && $options.handleMainAction(...args))
  } : {});
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/subpkg/order/detail.js.map

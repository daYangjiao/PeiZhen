"use strict";
const common_vendor = require("../common/vendor.js");
const common_assets = require("../common/assets.js");
const _sfc_main = {
  __name: "OrderCard",
  props: {
    orderData: {
      type: Object,
      required: true
    },
    showActions: {
      type: Boolean,
      default: true
    },
    showStatus: {
      type: Boolean,
      default: false
    },
    actionType: {
      type: String,
      default: "accept"
      // accept, start, complete, view
    }
  },
  emits: ["card-click", "view-detail", "main-action"],
  setup(__props, { emit: __emit }) {
    const props = __props;
    const emit = __emit;
    const displayAvatar = common_vendor.computed(() => {
      const placeholder = "/static/user-placeholder.png";
      return props.orderData.userAvatar || placeholder;
    });
    const statusClass = common_vendor.computed(() => {
      const statusMap = {
        1: "status-pending",
        2: "status-accepted",
        3: "status-progress",
        6: "status-completed",
        7: "status-cancelled"
      };
      if (typeof props.orderData.status === "string") {
        const strMap = {
          pending: "status-pending",
          accepted: "status-accepted",
          in_progress: "status-progress",
          completed: "status-completed",
          cancelled: "status-cancelled"
        };
        return strMap[props.orderData.status] || "status-pending";
      }
      return statusMap[props.orderData.orderStatus] || "status-pending";
    });
    const statusText = common_vendor.computed(() => {
      const statusMap = {
        1: "待接单",
        2: "待服务",
        3: "服务中",
        6: "已完成",
        7: "已取消"
      };
      if (typeof props.orderData.status === "string") {
        const strMap = {
          pending: "待接单",
          accepted: "待服务",
          in_progress: "服务中",
          completed: "已完成",
          cancelled: "已取消"
        };
        return strMap[props.orderData.status] || "待接单";
      }
      return statusMap[props.orderData.orderStatus] || "待接单";
    });
    const mainActionText = common_vendor.computed(() => {
      const actionMap = {
        accept: "接单",
        start: "开始服务",
        complete: "完成服务",
        view: "查看详情"
      };
      return actionMap[props.actionType] || "接单";
    });
    const serviceTypeColorClass = common_vendor.computed(() => {
      const t = (props.orderData.serviceType || "").trim();
      if (t.includes("普通"))
        return "type-normal";
      if (t.includes("术后"))
        return "type-postop";
      if (t.includes("急诊"))
        return "type-emergency";
      if (t.includes("上门"))
        return "type-home";
      return "type-default";
    });
    const handleCardClick = () => {
      emit("card-click", props.orderData);
    };
    const handleViewDetail = () => {
      common_vendor.index.navigateTo({
        url: `/subpkg/order/detail?orderId=${props.orderData.orderId || props.orderData.id}`
      });
      emit("view-detail", props.orderData);
    };
    const handleMainAction = () => {
      emit("main-action", {
        type: props.actionType,
        data: props.orderData
      });
    };
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: displayAvatar.value,
        b: common_vendor.t(__props.orderData.userName),
        c: __props.orderData.userAge
      }, __props.orderData.userAge ? {
        d: common_vendor.t(__props.orderData.userAge)
      } : {}, {
        e: __props.orderData.userGender
      }, __props.orderData.userGender ? {
        f: common_vendor.t(__props.orderData.userGender)
      } : {}, {
        g: common_vendor.t(__props.orderData.serviceType),
        h: common_vendor.n(serviceTypeColorClass.value),
        i: common_vendor.t(__props.orderData.price),
        j: common_assets._imports_0$6,
        k: common_vendor.t(__props.orderData.hospitalName || "未知医院"),
        l: common_assets._imports_1$4,
        m: common_vendor.t(__props.orderData.appointmentTime || "时间待定"),
        n: __props.orderData.phone
      }, __props.orderData.phone ? {
        o: common_assets._imports_2$5,
        p: common_vendor.t(__props.orderData.phone)
      } : {}, {
        q: __props.orderData.symptomDescription || __props.orderData.otherRequirement
      }, __props.orderData.symptomDescription || __props.orderData.otherRequirement ? common_vendor.e({
        r: __props.orderData.symptomDescription
      }, __props.orderData.symptomDescription ? {
        s: common_assets._imports_3$2,
        t: common_vendor.t(__props.orderData.symptomDescription)
      } : {}, {
        v: __props.orderData.otherRequirement
      }, __props.orderData.otherRequirement ? {
        w: common_assets._imports_4$1,
        x: common_vendor.t(__props.orderData.otherRequirement)
      } : {}) : {}, {
        y: __props.showStatus
      }, __props.showStatus ? {
        z: common_vendor.t(statusText.value),
        A: common_vendor.n(statusClass.value)
      } : {}, {
        B: __props.showActions
      }, __props.showActions ? {
        C: common_assets._imports_5$2,
        D: common_vendor.o(handleViewDetail),
        E: common_assets._imports_6$2,
        F: common_vendor.t(mainActionText.value),
        G: common_vendor.o(handleMainAction)
      } : {}, {
        H: common_vendor.o(handleCardClick)
      });
    };
  }
};
const Component = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-e55d4433"]]);
wx.createComponent(Component);
//# sourceMappingURL=../../.sourcemap/mp-weixin/components/OrderCard.js.map

"use strict";
const common_vendor = require("../common/vendor.js");
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
  emits: ["card-click", "contact", "main-action"],
  setup(__props, { emit: __emit }) {
    const props = __props;
    const emit = __emit;
    const statusClass = common_vendor.computed(() => {
      const statusMap = {
        pending: "status-pending",
        accepted: "status-accepted",
        in_progress: "status-progress",
        completed: "status-completed",
        cancelled: "status-cancelled"
      };
      return statusMap[props.orderData.status] || "status-pending";
    });
    const statusText = common_vendor.computed(() => {
      const statusMap = {
        pending: "待接单",
        accepted: "已接单",
        in_progress: "进行中",
        completed: "已完成",
        cancelled: "已取消"
      };
      return statusMap[props.orderData.status] || "待接单";
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
    const handleCardClick = () => {
      emit("card-click", props.orderData);
    };
    const handleContact = () => {
      emit("contact", props.orderData);
    };
    const handleMainAction = () => {
      emit("main-action", {
        type: props.actionType,
        data: props.orderData
      });
    };
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: __props.orderData.userAvatar,
        b: common_vendor.t(__props.orderData.userName),
        c: common_vendor.t(__props.orderData.userAge),
        d: common_vendor.t(__props.orderData.userGender),
        e: common_vendor.t(__props.orderData.serviceType),
        f: common_vendor.t(__props.orderData.price),
        g: common_vendor.t(__props.orderData.hospitalName || __props.orderData.hospital),
        h: __props.orderData.distance
      }, __props.orderData.distance ? {
        i: common_vendor.t(__props.orderData.distance)
      } : {}, {
        j: common_vendor.t(__props.orderData.appointmentTime),
        k: __props.orderData.duration
      }, __props.orderData.duration ? {
        l: common_vendor.t(__props.orderData.duration)
      } : {}, {
        m: common_vendor.t(__props.orderData.serviceDetail),
        n: __props.orderData.specialNote
      }, __props.orderData.specialNote ? {
        o: common_vendor.t(__props.orderData.specialNote)
      } : {}, {
        p: __props.showStatus
      }, __props.showStatus ? {
        q: common_vendor.t(statusText.value),
        r: common_vendor.n(statusClass.value)
      } : {}, {
        s: __props.showActions
      }, __props.showActions ? {
        t: common_vendor.o(handleContact),
        v: common_vendor.t(mainActionText.value),
        w: common_vendor.o(handleMainAction)
      } : {}, {
        x: common_vendor.o(handleCardClick)
      });
    };
  }
};
const Component = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-e55d4433"]]);
wx.createComponent(Component);
//# sourceMappingURL=../../.sourcemap/mp-weixin/components/OrderCard.js.map

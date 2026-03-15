"use strict";
const common_vendor = require("../../common/vendor.js");
const _sfc_main = {
  data() {
    return {
      orderNo: "",
      scene: "normal"
    };
  },
  onLoad(options) {
    if (options.orderNo) {
      this.orderNo = decodeURIComponent(options.orderNo);
    }
    if (options.scene) {
      this.scene = options.scene;
    }
  },
  methods: {
    // 重新支付
    retryPayment() {
      if (!this.orderNo) {
        common_vendor.index.showToast({
          title: "缺少订单号",
          icon: "none"
        });
        return;
      }
      if (this.scene === "balance") {
        common_vendor.index.redirectTo({
          url: `/pages/OrderDetailPage/OrderDetailPage?orderNo=${encodeURIComponent(this.orderNo)}`
        });
      } else {
        common_vendor.index.redirectTo({
          url: `/subpkg/appointment-flow/04_OrderConfirmPage?orderNo=${encodeURIComponent(this.orderNo)}`
        });
      }
    },
    // 跳转到订单详情页
    goToOrderDetail() {
      if (this.orderNo) {
        common_vendor.index.redirectTo({
          url: `/pages/OrderDetailPage/OrderDetailPage?orderNo=${encodeURIComponent(this.orderNo)}`
        });
      } else {
        common_vendor.index.showToast({
          title: "无法跳转：缺少订单号",
          icon: "none"
        });
      }
    },
    // 返回首页
    goHome() {
      common_vendor.index.reLaunch({
        url: "/pages/index/index"
      });
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return {
    a: common_vendor.o((...args) => $options.retryPayment && $options.retryPayment(...args)),
    b: common_vendor.o((...args) => $options.goToOrderDetail && $options.goToOrderDetail(...args)),
    c: common_vendor.o((...args) => $options.goHome && $options.goHome(...args))
  };
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-67b9ed84"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/subpkg/appointment-flow/PaymentFailedPage.js.map

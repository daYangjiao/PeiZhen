"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_api = require("../../utils/api.js");
const common_assets = require("../../common/assets.js");
const _sfc_main = {
  data() {
    return {
      orderId: null,
      serviceTypeNumber: 1,
      serviceTypeName: "普通陪诊",
      estimatedDuration: 2,
      orderAmount: 0,
      actualDuration: 2,
      feeDiff: 0,
      attendantRemark: "",
      submitting: false
    };
  },
  computed: {
    estimatedDurationDisplay() {
      return this.estimatedDuration ? this.estimatedDuration.toFixed(1) : "--";
    },
    diffText() {
      if (!this.estimatedDuration || this.feeDiff === 0) {
        return "无费用差异";
      }
      if (this.feeDiff > 0) {
        return `需补付¥${this.feeDiff.toFixed(2)}`;
      }
      return `自动退款¥${Math.abs(this.feeDiff).toFixed(2)}`;
    }
  },
  onLoad(options) {
    if (options.orderId) {
      this.orderId = options.orderId;
      this.loadOrder();
    }
  },
  methods: {
    goBack() {
      common_vendor.index.navigateBack();
    },
    async loadOrder() {
      try {
        const res = await utils_api.get(`/attendant/orders/${this.orderId}`);
        if (res.code === 200 && res.data) {
          const o = res.data;
          this.orderAmount = o.orderAmount || 0;
          this.serviceTypeNumber = o.clinicType || 1;
          this.serviceTypeName = o.serviceContent || "普通陪诊";
          let est = o.consultationDuration || o.estimatedDuration;
          if (!est && o.serviceTimeSlot) {
            const slot = o.serviceTimeSlot;
            const parts = slot.split("-");
            if (parts.length === 2) {
              const start = this.parseTime(parts[0]);
              const end = this.parseTime(parts[1]);
              const minutes = (end - start) / 6e4;
              if (minutes > 0) {
                est = Math.round(minutes / 60 * 10) / 10;
              }
            }
          }
          this.estimatedDuration = est || 2;
          this.actualDuration = this.estimatedDuration;
          this.recalcFeeDiff();
        }
      } catch (e) {
        common_vendor.index.__f__("error", "at subpkg/order/submit-time-fee.vue:139", "加载订单失败", e);
      }
    },
    parseTime(t) {
      const [h, m] = t.trim().split(":").map(Number);
      const d = /* @__PURE__ */ new Date();
      d.setHours(h || 0, m || 0, 0, 0);
      return d;
    },
    changeDuration(delta) {
      let v = this.actualDuration + delta;
      if (v < 0.5)
        v = 0.5;
      if (v > 24)
        v = 24;
      this.actualDuration = Math.round(v * 2) / 2;
      this.recalcFeeDiff();
    },
    recalcFeeDiff() {
      const expectedFee = this.calculateFee(this.serviceTypeNumber, this.actualDuration);
      this.feeDiff = expectedFee - (this.orderAmount || 0);
    },
    calculateFee(serviceType, hours) {
      const BASE_PRICE = 50;
      const EXTEND_PRICE = 30;
      const POST_CARE_PRICE = 45;
      const h = Math.max(hours, 0);
      if (serviceType === 2) {
        const rounded = Math.ceil(h);
        return rounded * POST_CARE_PRICE;
      }
      const calcNormal = () => {
        const actual = Math.max(h, 2);
        let total = BASE_PRICE;
        if (actual > 2) {
          const extra = actual - 2;
          const roundedExtra = Math.ceil(extra);
          total += roundedExtra * EXTEND_PRICE;
        }
        return total;
      };
      if (serviceType === 1) {
        return calcNormal();
      }
      if (serviceType === 3) {
        return calcNormal() + 100;
      }
      if (serviceType === 4) {
        return calcNormal() + 30;
      }
      return calcNormal();
    },
    async submit() {
      if (!this.orderId)
        return;
      if (this.submitting)
        return;
      this.submitting = true;
      try {
        const res = await utils_api.post(`/attendant/orders/${this.orderId}/end?actualDuration=${this.actualDuration}`);
        if (res.code === 200) {
          common_vendor.index.showToast({ title: "已提交，等待用户确认", icon: "success" });
          setTimeout(() => {
            common_vendor.index.navigateBack();
          }, 1200);
        } else {
          common_vendor.index.showToast({ title: res.message || "提交失败", icon: "none" });
        }
      } catch (e) {
        common_vendor.index.__f__("error", "at subpkg/order/submit-time-fee.vue:212", "提交时长失败", e);
        common_vendor.index.showToast({ title: "提交失败", icon: "none" });
      } finally {
        this.submitting = false;
      }
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return {
    a: common_assets._imports_0$4,
    b: common_vendor.o((...args) => $options.goBack && $options.goBack(...args)),
    c: common_vendor.t($options.estimatedDurationDisplay),
    d: common_vendor.t($data.serviceTypeName),
    e: common_vendor.o(($event) => $options.changeDuration(-0.5)),
    f: common_vendor.t($data.actualDuration.toFixed(1)),
    g: common_vendor.o(($event) => $options.changeDuration(0.5)),
    h: common_vendor.t($options.diffText),
    i: $data.feeDiff > 0 ? 1 : "",
    j: $data.feeDiff < 0 ? 1 : "",
    k: $data.attendantRemark,
    l: common_vendor.o(($event) => $data.attendantRemark = $event.detail.value),
    m: common_vendor.t($data.attendantRemark.length),
    n: $data.submitting,
    o: common_vendor.o((...args) => $options.submit && $options.submit(...args))
  };
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-2be58cfe"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/subpkg/order/submit-time-fee.js.map

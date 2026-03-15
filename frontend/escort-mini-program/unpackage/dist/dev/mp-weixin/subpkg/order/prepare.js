"use strict";
const common_vendor = require("../../common/vendor.js");
const common_assets = require("../../common/assets.js");
const _sfc_main = {
  data() {
    return {
      orderId: "",
      generalItems: [
        { label: "工作证/资质证明", checked: false },
        { label: "手机(扫码功能)", checked: false },
        { label: "口罩/手套", checked: false },
        { label: "签字笔/便签本", checked: false }
      ],
      orderItems: []
    };
  },
  onLoad(options) {
    this.orderId = options.orderId || "";
    const symptom = decodeURIComponent(options.symptom || "");
    const other = decodeURIComponent(options.other || "");
    decodeURIComponent(options.hospital || "");
    const items = [];
    if (symptom && symptom !== "无") {
      if (symptom.includes("青霉素") || symptom.toLowerCase().includes("过敏")) {
        items.push({ label: "对青霉素过敏", checked: false });
      }
      items.push({ label: symptom, checked: false });
    }
    items.push({ label: "医院科室导航图", checked: false });
    items.push({ label: "取药袋/病历夹", checked: false });
    items.push({ label: "轮椅(根据需求)", checked: false });
    if (other && other !== "无") {
      items.push({ label: other, checked: false });
    }
    const seen = /* @__PURE__ */ new Set();
    this.orderItems = items.filter((i) => {
      if (seen.has(i.label))
        return false;
      seen.add(i.label);
      return true;
    });
    if (this.orderItems.length === 0) {
      this.orderItems = [
        { label: "医院科室导航图", checked: false },
        { label: "取药袋/病历夹", checked: false },
        { label: "轮椅(根据需求)", checked: false }
      ];
    }
  },
  methods: {
    toggleCheck(type, idx) {
      const arr = type === "general" ? this.generalItems : this.orderItems;
      arr[idx].checked = !arr[idx].checked;
      this.$forceUpdate();
    },
    markPrepared() {
      if (!this.orderId) {
        common_vendor.index.showToast({ title: "订单信息异常", icon: "none" });
        return;
      }
      common_vendor.index.setStorageSync(`order_prepared_${this.orderId}`, "1");
      common_vendor.index.showToast({ title: "已标记准备完成", icon: "success" });
      setTimeout(() => {
        common_vendor.index.navigateBack();
      }, 800);
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return {
    a: common_assets._imports_4$1,
    b: common_vendor.f($data.generalItems, (item, idx, i0) => {
      return {
        a: common_vendor.t(item.checked ? "✓" : ""),
        b: item.checked ? 1 : "",
        c: common_vendor.t(item.label),
        d: "g" + idx,
        e: common_vendor.o(($event) => $options.toggleCheck("general", idx), "g" + idx)
      };
    }),
    c: common_assets._imports_3$2,
    d: common_vendor.f($data.orderItems, (item, idx, i0) => {
      return {
        a: common_vendor.t(item.checked ? "✓" : ""),
        b: item.checked ? 1 : "",
        c: common_vendor.t(item.label),
        d: "o" + idx,
        e: common_vendor.o(($event) => $options.toggleCheck("order", idx), "o" + idx)
      };
    }),
    e: common_vendor.o((...args) => $options.markPrepared && $options.markPrepared(...args))
  };
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-e873feab"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/subpkg/order/prepare.js.map

"use strict";
const common_vendor = require("../../common/vendor.js");
const common_assets = require("../../common/assets.js");
if (!Array) {
  const _component_uni_icons = common_vendor.resolveComponent("uni-icons");
  _component_uni_icons();
}
const _sfc_main = {
  __name: "Evaluate",
  setup(__props) {
    const orderId = common_vendor.ref("");
    const loading = common_vendor.ref(true);
    const error = common_vendor.ref("");
    const orderData = common_vendor.ref(null);
    common_vendor.onLoad((options) => {
      orderId.value = options.orderId || "";
      common_vendor.index.__f__("log", "at pages/Evaluate/Evaluate.vue:113", "【evaluate】收到 orderId:", orderId.value);
      if (!orderId.value) {
        error.value = "无效订单ID";
        loading.value = false;
        return;
      }
      loadOrderData();
    });
    const loadOrderData = async () => {
      loading.value = true;
      error.value = "";
      try {
        await new Promise((resolve) => setTimeout(resolve, 800));
        const mockData = {
          id: orderId.value,
          serviceName: "专业陪诊服务",
          hospital: "北京协和医院",
          serviceTime: "2023-08-15 09:00-12:00",
          provider: "张医生",
          rating: 5,
          comment: ""
        };
        orderData.value = mockData;
      } catch (err) {
        error.value = "加载失败，请稍后重试";
        common_vendor.index.__f__("error", "at pages/Evaluate/Evaluate.vue:146", err);
      } finally {
        loading.value = false;
      }
    };
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: _ctx.doctor.avatar,
        b: common_vendor.t(_ctx.doctor.name),
        c: common_vendor.t(_ctx.doctor.service),
        d: common_vendor.t(_ctx.doctor.time),
        e: common_vendor.f(5, (i, k0, i0) => {
          return {
            a: i,
            b: "650f982a-0-" + i0
          };
        }),
        f: common_vendor.p({
          type: "star-filled",
          color: "#FFB300",
          size: "24"
        }),
        g: common_vendor.f(5, (i, k0, i0) => {
          return {
            a: i,
            b: common_vendor.o(($event) => _ctx.setRating(i), i),
            c: _ctx.rating >= i ? 1 : "",
            d: "650f982a-1-" + i0
          };
        }),
        h: common_vendor.p({
          type: "star-filled",
          color: "#149DE4",
          size: "40"
        }),
        i: _ctx.content,
        j: common_vendor.o(($event) => _ctx.content = $event.detail.value),
        k: common_vendor.t(_ctx.content.length),
        l: common_vendor.f(_ctx.tags, (tag, k0, i0) => {
          return {
            a: common_vendor.t(tag),
            b: tag,
            c: _ctx.selectedTags.includes(tag) ? 1 : "",
            d: common_vendor.o(($event) => _ctx.toggleTag(tag), tag)
          };
        }),
        m: common_vendor.o((...args) => _ctx.cancel && _ctx.cancel(...args)),
        n: common_vendor.o((...args) => _ctx.submit && _ctx.submit(...args)),
        o: loading.value
      }, loading.value ? {} : error.value ? {
        q: common_vendor.t(error.value)
      } : {
        r: common_vendor.t(orderData.value.serviceName),
        s: common_vendor.t(orderData.value.hospital),
        t: common_vendor.t(orderData.value.serviceTime),
        v: common_vendor.t(orderData.value.provider),
        w: common_assets._imports_0$6,
        x: common_vendor.t(orderData.value.rating),
        y: orderData.value.comment,
        z: common_vendor.o(($event) => orderData.value.comment = $event.detail.value),
        A: common_vendor.o((...args) => _ctx.submit && _ctx.submit(...args))
      }, {
        p: error.value
      });
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-650f982a"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/Evaluate/Evaluate.js.map

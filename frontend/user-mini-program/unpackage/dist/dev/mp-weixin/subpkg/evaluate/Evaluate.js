"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_api = require("../../utils/api.js");
const _sfc_main = {
  __name: "Evaluate",
  setup(__props) {
    const orderNo = common_vendor.ref("");
    const orderId = common_vendor.ref(null);
    const loading = common_vendor.ref(true);
    const error = common_vendor.ref("");
    const orderInfo = common_vendor.ref({});
    const rating = common_vendor.ref(0);
    const content = common_vendor.ref("");
    const tags = common_vendor.ref(["服务专业", "沟通耐心", "时间准时", "人很亲切", "路线熟悉"]);
    const selectedTags = common_vendor.ref([]);
    common_vendor.onLoad((options) => {
      if (options && options.orderNo) {
        orderNo.value = decodeURIComponent(options.orderNo);
      }
      if (!orderNo.value) {
        error.value = "缺少订单编号";
        loading.value = false;
        return;
      }
      loadOrderInfo();
    });
    const loadOrderInfo = async () => {
      loading.value = true;
      error.value = "";
      try {
        const res = await utils_api.get(`/ai/guide/orders/${orderNo.value}/complete-info`);
        if (res && res.code === 200 && res.data) {
          orderInfo.value = res.data;
          orderId.value = res.data.orderId;
          await loadEvaluation();
        } else {
          error.value = "获取订单信息失败";
        }
      } catch (e) {
        common_vendor.index.__f__("error", "at subpkg/evaluate/Evaluate.vue:133", "加载订单信息失败", e);
        error.value = "网络错误，请稍后重试";
      } finally {
        loading.value = false;
      }
    };
    const loadEvaluation = async () => {
      if (!orderId.value)
        return;
      try {
        const res = await utils_api.get(`/api/orders/${orderId.value}/evaluation`);
        if (res && res.code === 200 && res.data) {
          const eva = res.data;
          if (eva.rating) {
            rating.value = eva.rating;
          }
          if (eva.tags) {
            selectedTags.value = eva.tags.split(",").filter(Boolean);
          }
          if (eva.content) {
            content.value = eva.content;
          }
        }
      } catch (e) {
        common_vendor.index.__f__("error", "at subpkg/evaluate/Evaluate.vue:157", "加载评价信息失败", e);
      }
    };
    const setRating = (val) => {
      rating.value = val;
    };
    const toggleTag = (tag) => {
      const list = selectedTags.value;
      const idx = list.indexOf(tag);
      if (idx >= 0) {
        list.splice(idx, 1);
      } else {
        list.push(tag);
      }
    };
    const submit = () => {
      if (rating.value === 0) {
        common_vendor.index.showToast({ title: "请先打个星级评分", icon: "none" });
        return;
      }
      if (!orderId.value) {
        common_vendor.index.showToast({ title: "订单信息有误", icon: "none" });
        return;
      }
      const payload = {
        rating: rating.value,
        tags: selectedTags.value.join(","),
        content: content.value
      };
      utils_api.post(`/api/orders/${orderId.value}/evaluation`, payload).then((res) => {
        if (res && res.code === 200) {
          if (orderNo.value) {
            common_vendor.index.setStorageSync(`order_evaluated_${orderNo.value}`, "1");
          }
          common_vendor.index.showToast({ title: "评价已提交", icon: "success" });
          setTimeout(() => {
            if (orderNo.value) {
              common_vendor.index.redirectTo({
                url: `/pages/OrderDetailPage/OrderDetailPage?orderNo=${encodeURIComponent(orderNo.value)}`
              });
            } else {
              common_vendor.index.navigateBack();
            }
          }, 800);
        } else {
          common_vendor.index.showToast({ title: res && res.message || "提交失败", icon: "none" });
        }
      }).catch((e) => {
        common_vendor.index.__f__("error", "at subpkg/evaluate/Evaluate.vue:214", "提交评价失败", e);
        common_vendor.index.showToast({ title: "提交失败，请稍后重试", icon: "none" });
      });
    };
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: loading.value
      }, loading.value ? {} : error.value ? {
        c: common_vendor.t(error.value)
      } : {
        d: common_vendor.t(orderNo.value),
        e: common_vendor.t(orderInfo.value.serviceTypeName || "陪诊服务"),
        f: common_vendor.t(orderInfo.value.serviceDate),
        g: common_vendor.t(orderInfo.value.serviceTime),
        h: common_vendor.t(orderInfo.value.hospital || "-"),
        i: common_vendor.t(orderInfo.value.attendantName || "待分配"),
        j: common_vendor.f(5, (i, k0, i0) => {
          return {
            a: i,
            b: rating.value >= i ? 1 : "",
            c: common_vendor.o(($event) => setRating(i), i)
          };
        }),
        k: common_vendor.f(tags.value, (tag, k0, i0) => {
          return {
            a: common_vendor.t(tag),
            b: tag,
            c: selectedTags.value.includes(tag) ? 1 : "",
            d: common_vendor.o(($event) => toggleTag(tag), tag)
          };
        }),
        l: content.value,
        m: common_vendor.o(($event) => content.value = $event.detail.value),
        n: common_vendor.t(content.value.length)
      }, {
        b: error.value,
        o: common_vendor.o(submit)
      });
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-956883ca"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/subpkg/evaluate/Evaluate.js.map

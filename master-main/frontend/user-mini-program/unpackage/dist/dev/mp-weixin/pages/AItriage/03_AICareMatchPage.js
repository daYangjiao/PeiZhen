"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_api = require("../../utils/api.js");
const _sfc_main = {
  __name: "03_AICareMatchPage",
  setup(__props) {
    const appointmentFormData = common_vendor.ref({});
    const appointmentNo = common_vendor.ref("");
    common_vendor.onLoad((options) => {
      if (options && options.appointmentNo) {
        appointmentNo.value = options.appointmentNo;
      }
      try {
        const cachedData = common_vendor.index.getStorageSync("appointmentFormData");
        if (cachedData) {
          appointmentFormData.value = cachedData;
          if (!appointmentNo.value && cachedData.appointmentNo) {
            appointmentNo.value = cachedData.appointmentNo;
          }
        }
      } catch (e) {
        common_vendor.index.__f__("error", "at pages/AItriage/03_AICareMatchPage.vue:156", "读取预约表单缓存失败:", e);
      }
      if (appointmentNo.value) {
        fetchAttendants();
      } else {
        common_vendor.index.__f__("error", "at pages/AItriage/03_AICareMatchPage.vue:163", "缺少 appointmentNo，无法加载数据");
      }
    });
    const loading = common_vendor.ref(true);
    const rawCompanions = common_vendor.ref([]);
    const fetchAttendants = () => {
      if (!appointmentNo.value) {
        common_vendor.index.__f__("error", "at pages/AItriage/03_AICareMatchPage.vue:173", "缺少 appointmentNo，无法请求 API");
        loading.value = false;
        return;
      }
      utils_api.get(`/ai/guide/attendants/match?appointmentNo=${encodeURIComponent(appointmentNo.value)}`).then((res) => {
        if (res.data && Array.isArray(res.data.attendants)) {
          rawCompanions.value = res.data.attendants.map((attendant) => {
            let photoUrl = attendant.photo;
            if (photoUrl && !photoUrl.startsWith("http")) {
              if (photoUrl.startsWith("/")) {
                photoUrl = photoUrl.substring(1);
              }
              const normalizedBaseUrl = utils_api.config.baseURL.endsWith("/") ? utils_api.config.baseURL : utils_api.config.baseURL + "/";
              photoUrl = normalizedBaseUrl + photoUrl;
            }
            common_vendor.index.__f__("log", "at pages/AItriage/03_AICareMatchPage.vue:198", `Constructed photo URL for ${attendant.name}:`, photoUrl);
            return {
              id: attendant.id,
              name: attendant.name,
              photo: photoUrl,
              // 确保是完整 URL
              score: attendant.score,
              professionalField: attendant.professionalField,
              experienceYears: attendant.experienceYears,
              // 保留经验年限
              reviewCount: attendant.reviewCount,
              introduction: attendant.introduction,
              qualifications: attendant.qualifications,
              services: attendant.services,
              recommended: attendant.recommended,
              phone: attendant.phone
              // --- 添加电话字段 ---
            };
          });
        } else {
          common_vendor.index.__f__("error", "at pages/AItriage/03_AICareMatchPage.vue:216", "API 返回数据格式错误:", res.data);
        }
      }).catch((err) => {
        common_vendor.index.__f__("error", "at pages/AItriage/03_AICareMatchPage.vue:220", "API 请求失败:", err);
      }).finally(() => {
        loading.value = false;
      });
    };
    const sortKey = common_vendor.ref("ai");
    const displayedCount = common_vendor.ref(3);
    const displayedCompanions = common_vendor.computed(() => {
      return companions.value.slice(0, displayedCount.value);
    });
    const companions = common_vendor.computed(() => {
      let list = [...rawCompanions.value];
      if (sortKey.value === "ai") {
        list.sort((a, b) => b.recommended - a.recommended);
      } else if (sortKey.value === "rating") {
        list.sort((a, b) => b.score - a.score);
      } else if (sortKey.value === "experience") {
        list.sort((a, b) => b.experienceYears - a.experienceYears);
      }
      return list;
    });
    const setSort = (key) => {
      sortKey.value = key;
    };
    const showDetail = common_vendor.ref(false);
    const currentCompanion = common_vendor.ref({});
    const viewDetail = (item) => {
      currentCompanion.value = item;
      showDetail.value = true;
    };
    const closeDetail = () => {
      showDetail.value = false;
    };
    const loadAllCompanions = () => {
      displayedCount.value = companions.value.length;
    };
    const sendSelectionRequest = async (companion) => {
      common_vendor.index.__f__("log", "at pages/AItriage/03_AICareMatchPage.vue:285", "用户选择的陪诊师对象:", companion);
      const orderRequestBody = {
        appointmentNo: appointmentNo.value,
        // --- 使用从 URL 或缓存获取的 appointmentNo ---
        attendantId: companion.id
        // --- 使用选中的陪诊师 ID ---
      };
      try {
        const orderResponse = await utils_api.post("/ai/guide/orders", orderRequestBody);
        if (orderResponse && orderResponse.data && orderResponse.data.orderNo) {
          const orderNo = orderResponse.data.orderNo;
          common_vendor.index.__f__("log", "at pages/AItriage/03_AICareMatchPage.vue:300", "成功创建订单，订单号:", orderNo);
          common_vendor.index.navigateTo({
            url: `/pages/AItriage/04_OrderConfirmPage?orderNo=${encodeURIComponent(orderNo)}`
          });
        } else {
          common_vendor.index.__f__("error", "at pages/AItriage/03_AICareMatchPage.vue:308", "API 创建订单返回数据格式错误:", orderResponse);
          common_vendor.index.showToast({ title: "创建订单失败", icon: "none" });
        }
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/AItriage/03_AICareMatchPage.vue:312", "创建订单请求失败:", error);
        common_vendor.index.showToast({ title: "创建订单失败", icon: "none" });
      }
    };
    const selectCompanion = (item) => {
      sendSelectionRequest(item);
    };
    const selectFromDetail = () => {
      closeDetail();
      sendSelectionRequest(currentCompanion.value);
    };
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: sortKey.value === "ai" ? 1 : "",
        b: common_vendor.o(($event) => setSort("ai")),
        c: sortKey.value === "rating" ? 1 : "",
        d: common_vendor.o(($event) => setSort("rating")),
        e: sortKey.value === "experience" ? 1 : "",
        f: common_vendor.o(($event) => setSort("experience")),
        g: loading.value
      }, loading.value ? {} : common_vendor.e({
        h: common_vendor.f(displayedCompanions.value, (item, index, i0) => {
          return common_vendor.e({
            a: item.recommended
          }, item.recommended ? {} : {}, {
            b: item.photo,
            c: common_vendor.t(item.name),
            d: common_vendor.t(item.score),
            e: common_vendor.t(item.reviewCount || 0),
            f: common_vendor.t(item.professionalField),
            g: common_vendor.t(item.experienceYears),
            h: common_vendor.o(($event) => viewDetail(item), item.id),
            i: common_vendor.o(($event) => selectCompanion(item), item.id),
            j: item.id
          });
        }),
        i: displayedCount.value < companions.value.length
      }, displayedCount.value < companions.value.length ? {
        j: common_vendor.o(loadAllCompanions)
      } : {}), {
        k: showDetail.value
      }, showDetail.value ? common_vendor.e({
        l: common_vendor.o(closeDetail),
        m: currentCompanion.value.photo,
        n: common_vendor.t(currentCompanion.value.name),
        o: common_vendor.t(currentCompanion.value.score),
        p: common_vendor.t(currentCompanion.value.reviewCount || 0),
        q: common_vendor.f(currentCompanion.value.qualifications, (item, index, i0) => {
          return {
            a: common_vendor.t(item),
            b: index
          };
        }),
        r: currentCompanion.value.services.includes("consult")
      }, currentCompanion.value.services.includes("consult") ? {} : {}, {
        s: currentCompanion.value.services.includes("exam")
      }, currentCompanion.value.services.includes("exam") ? {} : {}, {
        t: currentCompanion.value.services.includes("medication")
      }, currentCompanion.value.services.includes("medication") ? {} : {}, {
        v: currentCompanion.value.services.includes("record")
      }, currentCompanion.value.services.includes("record") ? {} : {}, {
        w: currentCompanion.value.services.includes("rehab")
      }, currentCompanion.value.services.includes("rehab") ? {} : {}, {
        x: currentCompanion.value.services.includes("diet")
      }, currentCompanion.value.services.includes("diet") ? {} : {}, {
        y: common_vendor.t(currentCompanion.value.introduction),
        z: common_vendor.o(selectFromDetail)
      }) : {});
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-81a9308a"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/AItriage/03_AICareMatchPage.js.map

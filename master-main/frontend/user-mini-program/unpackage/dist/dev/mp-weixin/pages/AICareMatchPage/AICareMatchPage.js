"use strict";
const common_vendor = require("../../common/vendor.js");
const _sfc_main = {
  __name: "AICareMatchPage",
  setup(__props) {
    const operationName = common_vendor.ref("");
    const symptoms = common_vendor.ref(["胸痛", "头痛", "发热", "呼吸困难", "恶心呕吐", "腹痛", "头晕"]);
    const selectedSymptoms = common_vendor.ref([]);
    const urgency = common_vendor.ref("normal");
    const otherNeeds = common_vendor.ref("");
    let appointmentFormData = common_vendor.ref({});
    const isMatching = common_vendor.ref(false);
    common_vendor.onMounted(() => {
      try {
        appointmentFormData.value = common_vendor.index.getStorageSync("appointmentFormData") || {};
        common_vendor.index.__f__("log", "at pages/AICareMatchPage/AICareMatchPage.vue:108", "【AICareMatchPage】从缓存读取预约表单数据:", appointmentFormData.value);
      } catch (e) {
        common_vendor.index.__f__("error", "at pages/AICareMatchPage/AICareMatchPage.vue:110", "【AICareMatchPage】读取预约表单缓存失败:", e);
      }
    });
    const toggleSymptom = (index) => {
      if (selectedSymptoms.value.includes(index)) {
        selectedSymptoms.value = selectedSymptoms.value.filter((i) => i !== index);
      } else {
        selectedSymptoms.value.push(index);
      }
    };
    const addOtherSymptom = () => {
      common_vendor.index.showToast({
        title: "添加其他症状功能未实现"
      });
    };
    const setUrgency = (level) => {
      urgency.value = level;
    };
    const getEmergencyLevel = (level) => {
      switch (level) {
        case "normal":
          return 0;
        case "urgent":
          return 1;
        case "emergency":
          return 2;
        default:
          return 0;
      }
    };
    const getRecommendation = async () => {
      if (!operationName.value && selectedSymptoms.value.length === 0 && !otherNeeds.value) {
        common_vendor.index.showToast({
          title: "请填写必要信息",
          icon: "none"
        });
        return;
      }
      const localMatchData = {
        demandType: "陪诊",
        emergencyLevel: getEmergencyLevel(urgency.value),
        surgeryName: operationName.value,
        symptoms: selectedSymptoms.value.map((i) => symptoms.value[i]),
        otherRequirement: otherNeeds.value,
        // --- 新增：从缓存添加医院和时间 ---
        hospital: appointmentFormData.value.hospitalName || "",
        serviceDate: appointmentFormData.value.serviceDate || "",
        serviceStartTime: appointmentFormData.value.startTime || "",
        serviceEndTime: appointmentFormData.value.endTime || ""
        // --- 结束新增 ---
      };
      common_vendor.index.__f__("log", "at pages/AICareMatchPage/AICareMatchPage.vue:173", "【AICareMatchPage】本地匹配数据 (模拟):", localMatchData);
      isMatching.value = true;
      const updatedFormData = {
        ...appointmentFormData.value,
        // 包含 hospitalName, serviceDate, startTime, endTime 等
        operationName: operationName.value,
        selectedSymptoms: selectedSymptoms.value,
        urgency: urgency.value,
        otherNeeds: otherNeeds.value
      };
      common_vendor.index.__f__("log", "at pages/AICareMatchPage/AICareMatchPage.vue:213", "【AICareMatchPage】准备跳转，更新缓存数据:", updatedFormData);
      common_vendor.index.setStorageSync("appointmentFormData", updatedFormData);
      setTimeout(() => {
        isMatching.value = false;
        common_vendor.index.navigateTo({
          url: `/pages/ServiceCompanionSelect/ServiceCompanionSelect`
          // 移除 appointmentId 参数
        });
      }, 1500);
    };
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: operationName.value,
        b: common_vendor.o(($event) => operationName.value = $event.detail.value),
        c: common_vendor.f(symptoms.value, (item, index, i0) => {
          return {
            a: selectedSymptoms.value.includes(index),
            b: common_vendor.t(item),
            c: index,
            d: common_vendor.o(($event) => toggleSymptom(index), index)
          };
        }),
        d: common_vendor.o(addOtherSymptom),
        e: otherNeeds.value,
        f: common_vendor.o(($event) => otherNeeds.value = $event.detail.value),
        g: urgency.value === "normal" ? 1 : "",
        h: common_vendor.o(($event) => setUrgency("normal")),
        i: urgency.value === "urgent" ? 1 : "",
        j: common_vendor.o(($event) => setUrgency("urgent")),
        k: urgency.value === "emergency" ? 1 : "",
        l: common_vendor.o(($event) => setUrgency("emergency")),
        m: common_vendor.o(getRecommendation),
        n: isMatching.value,
        o: isMatching.value
      }, isMatching.value ? {} : {});
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-1314d952"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/AICareMatchPage/AICareMatchPage.js.map

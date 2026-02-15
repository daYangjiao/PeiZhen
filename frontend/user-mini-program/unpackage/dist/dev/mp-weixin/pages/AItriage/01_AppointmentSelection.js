"use strict";
const common_vendor = require("../../common/vendor.js");
const common_assets = require("../../common/assets.js");
const _sfc_main = {
  __name: "01_AppointmentSelection",
  setup(__props) {
    const selectedService = common_vendor.ref(null);
    const serviceDetails = common_vendor.ref({
      general: {
        title: "普通陪诊服务详情",
        details: [
          { icon: "/static/time.png", text: "2小时起约，可根据需要延长" },
          { icon: "/static/ren_1.png", text: "¥50/起步价，1小时可延长活动" },
          { icon: "/static/help.png", text: "医院内陪同，包括挂号、缴费、取药等" }
        ]
      },
      postop: {
        title: "术后护理服务详情",
        details: [
          { icon: "/static/time.png", text: "24小时专业护理，术后恢复指导" },
          { icon: "/static/ren_1.png", text: "¥80/起步价，专业护理人员" },
          { icon: "/static/help.png", text: "术后康复指导、伤口护理、用药提醒" }
        ]
      },
      emergency: {
        title: "急诊陪同服务详情",
        details: [
          { icon: "/static/time.png", text: "24小时随时响应，紧急情况优先" },
          { icon: "/static/ren_1.png", text: "¥100/起步价，急诊专业陪护" },
          { icon: "/static/help.png", text: "急诊科陪同、协助医生沟通、家属联系" }
        ]
      },
      home: {
        title: "上门陪诊服务详情",
        details: [
          { icon: "/static/time.png", text: "预约上门，专业陪诊师到家服务" },
          { icon: "/static/ren_1.png", text: "¥120/起步价，包含交通费用" },
          { icon: "/static/help.png", text: "上门接送、全程陪同、专业护理" }
        ]
      }
    });
    common_vendor.onMounted(() => {
      restoreUserSelections();
    });
    common_vendor.onShow(() => {
      restoreUserSelections();
    });
    const saveUserSelections = () => {
      let serviceTypeNumber = 0;
      switch (selectedService.value) {
        case "general":
          serviceTypeNumber = 1;
          break;
        case "postop":
          serviceTypeNumber = 2;
          break;
        case "emergency":
          serviceTypeNumber = 3;
          break;
        case "home":
          serviceTypeNumber = 4;
          break;
        default:
          serviceTypeNumber = 0;
      }
      const selections = {
        appointment_type: serviceTypeNumber
        // 保存为数字 1,2,3,4
      };
      common_vendor.index.setStorageSync("appointmentSelections", selections);
    };
    const restoreUserSelections = () => {
      try {
        const selections = common_vendor.index.getStorageSync("appointmentSelections");
        if (selections && selections.appointment_type) {
          switch (selections.appointment_type) {
            case 1:
              selectedService.value = "general";
              break;
            case 2:
              selectedService.value = "postop";
              break;
            case 3:
              selectedService.value = "emergency";
              break;
            case 4:
              selectedService.value = "home";
              break;
            default:
              selectedService.value = null;
          }
        }
      } catch (e) {
        common_vendor.index.__f__("log", "at pages/AItriage/01_AppointmentSelection.vue:147", "恢复用户选择失败:", e);
      }
    };
    const selectService = (type) => {
      if (selectedService.value === type) {
        selectedService.value = null;
      } else {
        selectedService.value = type;
      }
      saveUserSelections();
    };
    const goToNext = () => {
      if (!selectedService.value) {
        common_vendor.index.showToast({
          title: "请先选择服务类型",
          icon: "none"
        });
        return;
      }
      let serviceTypeNumber = 0;
      let serviceTypeName = "";
      switch (selectedService.value) {
        case "general":
          serviceTypeNumber = 1;
          serviceTypeName = "普通陪诊";
          break;
        case "postop":
          serviceTypeNumber = 2;
          serviceTypeName = "术后护理";
          break;
        case "emergency":
          serviceTypeNumber = 3;
          serviceTypeName = "急诊陪同";
          break;
        case "home":
          serviceTypeNumber = 4;
          serviceTypeName = "上门陪诊";
          break;
      }
      common_vendor.index.navigateTo({
        url: "/pages/AItriage/02_AppointmentForm?serviceTypeNumber=" + serviceTypeNumber + "&serviceTypeName=" + encodeURIComponent(serviceTypeName)
      });
    };
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_assets._imports_0$3,
        b: selectedService.value === "general" ? 1 : "",
        c: common_vendor.o(($event) => selectService("general")),
        d: common_assets._imports_1$2,
        e: selectedService.value === "postop" ? 1 : "",
        f: common_vendor.o(($event) => selectService("postop")),
        g: common_assets._imports_2$1,
        h: selectedService.value === "emergency" ? 1 : "",
        i: common_vendor.o(($event) => selectService("emergency")),
        j: common_assets._imports_3$1,
        k: selectedService.value === "home" ? 1 : "",
        l: common_vendor.o(($event) => selectService("home")),
        m: selectedService.value
      }, selectedService.value ? {
        n: common_vendor.t(serviceDetails.value[selectedService.value].title),
        o: common_vendor.f(serviceDetails.value[selectedService.value].details, (detail, index, i0) => {
          return {
            a: detail.icon,
            b: common_vendor.t(detail.text),
            c: index
          };
        })
      } : {}, {
        p: common_vendor.o(goToNext)
      });
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-c92abd07"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/AItriage/01_AppointmentSelection.js.map

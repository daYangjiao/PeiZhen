"use strict";
const common_vendor = require("../../common/vendor.js");
const common_assets = require("../../common/assets.js");
const _sfc_main = {
  __name: "appointment",
  setup(__props) {
    const selectedService = common_vendor.ref(null);
    const selectedCompanion = common_vendor.ref(null);
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
    const companions = common_vendor.ref([
      {
        id: 1,
        name: "林雅婷",
        avatar: "/static/provider1.jpg",
        rating: 4.9,
        reviewCount: 260,
        experience: "3年经验",
        tags: ["专业陪诊", "细致入微", "有医学背景"],
        price: 50
      },
      {
        id: 2,
        name: "王建国",
        avatar: "/static/provider2.jpg",
        rating: 4.2,
        reviewCount: 178,
        experience: "2年经验",
        tags: ["耐心细致", "熟悉流程"],
        price: 55
      },
      {
        id: 3,
        name: "张丽华",
        avatar: "/static/doctor1.jpg",
        rating: 5,
        reviewCount: 312,
        experience: "5年经验",
        tags: ["护士背景", "专业素养", "服务贴心"],
        price: 60
      }
    ]);
    common_vendor.onMounted(() => {
      restoreUserSelections();
    });
    common_vendor.onShow(() => {
      restoreUserSelections();
    });
    const saveUserSelections = () => {
      const selections = {
        selectedService: selectedService.value,
        selectedCompanion: selectedCompanion.value
      };
      common_vendor.index.setStorageSync("appointmentSelections", selections);
    };
    const restoreUserSelections = () => {
      try {
        const selections = common_vendor.index.getStorageSync("appointmentSelections");
        if (selections) {
          selectedService.value = selections.selectedService;
          selectedCompanion.value = selections.selectedCompanion;
        }
      } catch (e) {
        common_vendor.index.__f__("log", "at pages/appointment/appointment.vue:186", "恢复用户选择失败:", e);
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
    const selectCompanion = (companion) => {
      if (selectedCompanion.value && selectedCompanion.value.id === companion.id) {
        selectedCompanion.value = null;
      } else {
        selectedCompanion.value = companion;
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
      if (!selectedCompanion.value) {
        common_vendor.index.showToast({
          title: "请先选择陪诊员",
          icon: "none"
        });
        return;
      }
      common_vendor.index.navigateTo({
        url: "/subpkg/appointment/appointment-time?service=" + encodeURIComponent(JSON.stringify({
          type: selectedService.value,
          companion: selectedCompanion.value
        }))
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
        p: common_vendor.f(companions.value, (companion, index, i0) => {
          return {
            a: companion.avatar,
            b: common_vendor.t(companion.name),
            c: common_vendor.t(companion.rating),
            d: common_vendor.t(companion.reviewCount),
            e: common_vendor.t(companion.experience),
            f: common_vendor.f(companion.tags, (tag, tagIndex, i1) => {
              return {
                a: common_vendor.t(tag),
                b: tagIndex
              };
            }),
            g: common_vendor.t(companion.price),
            h: selectedCompanion.value && selectedCompanion.value.id === companion.id ? 1 : "",
            i: index,
            j: common_vendor.o(($event) => selectCompanion(companion), index)
          };
        }),
        q: common_vendor.o(goToNext)
      });
    };
  }
};
wx.createPage(_sfc_main);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/appointment/appointment.js.map

"use strict";
const common_vendor = require("../../common/vendor.js");
const _sfc_main = {
  __name: "ServiceCompanionSelect",
  setup(__props) {
    let appointmentFormData = common_vendor.ref({});
    common_vendor.onMounted(() => {
      try {
        appointmentFormData.value = common_vendor.index.getStorageSync("appointmentFormData") || {};
        common_vendor.index.__f__("log", "at pages/ServiceCompanionSelect/ServiceCompanionSelect.vue:153", "【ServiceCompanionSelect】从缓存读取预约表单数据:", appointmentFormData.value);
      } catch (e) {
        common_vendor.index.__f__("error", "at pages/ServiceCompanionSelect/ServiceCompanionSelect.vue:155", "【ServiceCompanionSelect】读取预约表单缓存失败:", e);
      }
    });
    const selectedServices = common_vendor.ref(["consult", "exam"]);
    const rawCompanions = common_vendor.computed(() => {
      const startTime = appointmentFormData.value.startTime;
      const endTime = appointmentFormData.value.endTime;
      let durationHours = 1;
      let baseHourlyRate = 49;
      if (startTime && endTime) {
        const startMinutes = timeToMinutes(startTime);
        const endMinutes = timeToMinutes(endTime);
        if (endMinutes > startMinutes) {
          durationHours = (endMinutes - startMinutes) / 60;
          common_vendor.index.__f__("log", "at pages/ServiceCompanionSelect/ServiceCompanionSelect.vue:185", "【ServiceCompanionSelect】计算时长:", durationHours, "小时");
        }
      }
      const basePrice = Math.ceil(durationHours * baseHourlyRate);
      common_vendor.index.__f__("log", "at pages/ServiceCompanionSelect/ServiceCompanionSelect.vue:190", "【ServiceCompanionSelect】计算基础价格:", basePrice, "元");
      const priceCoefficients = [1, 1.2, 0.8];
      return [
        {
          id: 1,
          name: "王丽",
          avatar: "./provider1.jpg",
          rating: 4.8,
          reviews: 126,
          hospital: "北京协和医院内分泌科",
          specialty: "糖尿病患者陪诊经验",
          experience: 5,
          basePrice,
          // 基础价格
          priceCoefficient: priceCoefficients[0],
          // 价格系数 1.0
          price: Math.ceil(basePrice * priceCoefficients[0]),
          // 最终价格
          recommended: true,
          qualifications: ["8年临床护理经验", "擅长术后重症专业护理", "能提供康复训练建议", "精通糖尿病患者日常管理", "熟悉北京协和医院就诊流程"],
          services: ["consult", "exam", "medication", "record"],
          reviewSummary: "非常专业的陪诊师，对糖尿病患者的护理很有经验，就诊过程很顺利，详细记录了医生的嘱咐，还提供了很多有用的饮食建议。"
        },
        {
          id: 2,
          name: "张伟",
          avatar: "./38da0ad384c9103b9b6de1a987e16191.jpg",
          rating: 4.9,
          reviews: 218,
          hospital: "北京301医院心内科",
          specialty: "心脏病患者陪诊、老年病护理",
          experience: 10,
          basePrice,
          // 基础价格
          priceCoefficient: priceCoefficients[1],
          // 价格系数 1.2
          price: Math.ceil(basePrice * priceCoefficients[1]),
          // 最终价格
          recommended: false,
          qualifications: ["10年心血管护理经验", "老年病专科护理认证", "心脏术后康复指导专家", "熟悉各类心脏检查流程", "急救技能认证"],
          services: ["consult", "exam", "record", "rehab"],
          reviewSummary: "张陪诊师非常有耐心，对老年人特别照顾，整个就诊过程安排得很合理，解释病情也很专业，让人非常放心。"
        },
        {
          id: 3,
          name: "刘芳",
          avatar: "./1a13f15c8114696a50ebd020c621cc8f.jpg",
          rating: 4.7,
          reviews: 156,
          hospital: "北京儿童医院儿科",
          specialty: "儿童患者陪诊、疫苗接种陪同",
          experience: 7,
          basePrice,
          // 基础价格
          priceCoefficient: priceCoefficients[2],
          // 价格系数 0.8
          price: Math.ceil(basePrice * priceCoefficients[2]),
          // 最终价格
          recommended: false,
          qualifications: ["7年儿科护理经验", "儿童心理安抚专家", "熟悉各类疫苗接种流程", "儿童常见疾病护理经验", "儿科检查引导技巧"],
          services: ["consult", "exam", "medication", "diet"],
          reviewSummary: "特别会和孩子沟通，原本害怕看医生的宝宝全程很配合，还详细讲解了注意事项，非常专业的儿科陪诊师。"
        }
      ];
    });
    const timeToMinutes = (timeStr) => {
      if (!timeStr)
        return -1;
      const [h, m] = timeStr.split(":").map(Number);
      return h * 60 + m;
    };
    const sortKey = common_vendor.ref("ai");
    const priceOrder = common_vendor.ref("desc");
    const companions = common_vendor.computed(() => {
      let list = [...rawCompanions.value];
      if (sortKey.value === "ai") {
        list.sort((a, b) => b.recommended - a.recommended);
      } else if (sortKey.value === "rating") {
        list.sort((a, b) => b.rating - a.rating);
      } else if (sortKey.value === "price") {
        if (priceOrder.value === "asc") {
          list.sort((a, b) => a.price - b.price);
        } else {
          list.sort((a, b) => b.price - a.price);
        }
      } else if (sortKey.value === "experience") {
        list.sort((a, b) => b.reviews - a.reviews);
      }
      return list;
    });
    const setSort = (key) => {
      if (key === "price") {
        togglePriceOrder();
      } else {
        sortKey.value = key;
        if (key !== "price") {
          priceOrder.value = "desc";
        }
      }
    };
    const togglePriceOrder = () => {
      sortKey.value = "price";
      priceOrder.value = priceOrder.value === "asc" ? "desc" : "asc";
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
    const sendSelectionRequest = async (companion) => {
      common_vendor.index.__f__("log", "at pages/ServiceCompanionSelect/ServiceCompanionSelect.vue:332", "【调试】用户选择的陪诊师对象:", companion);
      const startMinutes = timeToMinutes(appointmentFormData.value.startTime);
      const endMinutes = timeToMinutes(appointmentFormData.value.endTime);
      let amount = 0;
      if (startMinutes !== -1 && endMinutes !== -1 && endMinutes > startMinutes) {
        const durationHours = (endMinutes - startMinutes) / 60;
        const baseHourlyRate = 49;
        const finalPricePerHour = baseHourlyRate * companion.priceCoefficient;
        amount = Math.ceil(durationHours * finalPricePerHour * 100);
      } else {
        amount = companion.price * 100;
      }
      const mockOrderNo = `ORD${Date.now()}`;
      const orderConfirmData = {
        // 从缓存获取的表单数据
        ...appointmentFormData.value,
        // 当前页面选择的数据
        attendantId: companion.id,
        attendantName: companion.name,
        // --- 修改点：添加头像信息 ---
        avatarUrl: companion.avatar,
        // 将选中陪诊师的头像路径存储
        // ---
        attendantPhone: companion.phone || "",
        // 假设伴诊师对象有电话
        selectedServices: selectedServices.value,
        // 模拟后端返回的数据
        orderNo: mockOrderNo,
        amount,
        // 使用计算后的金额（分）
        // 临时状态
        payMethod: "wechat",
        // 默认支付方式
        remark: ""
        // 默认备注
      };
      orderConfirmData.hospital = orderConfirmData.hospitalName;
      orderConfirmData.serviceTime = `${orderConfirmData.serviceDate} ${orderConfirmData.startTime}`;
      delete orderConfirmData.hospitalName;
      delete orderConfirmData.startTime;
      delete orderConfirmData.endTime;
      common_vendor.index.__f__("log", "at pages/ServiceCompanionSelect/ServiceCompanionSelect.vue:380", "【调试】准备存储到 orderConfirmData 的数据:", orderConfirmData);
      common_vendor.index.setStorageSync("orderConfirmData", orderConfirmData);
      common_vendor.index.navigateTo({
        url: "/pages/OrderConfirmPage/OrderConfirmPage"
      });
    };
    const selectCompanion = (item) => {
      sendSelectionRequest(item);
    };
    const selectFromDetail = () => {
      closeDetail();
      sendSelectionRequest(currentCompanion.value);
    };
    const getImagePath = (fileName) => `/static/${fileName}`;
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: sortKey.value === "ai" ? 1 : "",
        b: common_vendor.o(($event) => setSort("ai")),
        c: sortKey.value === "rating" ? 1 : "",
        d: common_vendor.o(($event) => setSort("rating")),
        e: priceOrder.value === "asc"
      }, priceOrder.value === "asc" ? {} : {}, {
        f: sortKey.value === "price" ? 1 : "",
        g: common_vendor.o(togglePriceOrder),
        h: sortKey.value === "experience" ? 1 : "",
        i: common_vendor.o(($event) => setSort("experience")),
        j: common_vendor.f(companions.value, (item, index, i0) => {
          return common_vendor.e({
            a: item.recommended
          }, item.recommended ? {} : {}, {
            b: getImagePath(item.avatar),
            c: common_vendor.t(item.name),
            d: common_vendor.t(item.rating),
            e: common_vendor.t(item.reviews),
            f: common_vendor.t(item.hospital),
            g: common_vendor.t(item.specialty),
            h: common_vendor.t(item.experience),
            i: common_vendor.t(item.price),
            j: common_vendor.o(($event) => viewDetail(item), item.id),
            k: common_vendor.o(($event) => selectCompanion(item), item.id),
            l: item.id
          });
        }),
        k: showDetail.value
      }, showDetail.value ? common_vendor.e({
        l: common_vendor.o(closeDetail),
        m: getImagePath(currentCompanion.value.avatar),
        n: common_vendor.t(currentCompanion.value.name),
        o: common_vendor.t(currentCompanion.value.rating),
        p: common_vendor.t(currentCompanion.value.reviews),
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
        y: common_vendor.t(currentCompanion.value.reviewSummary),
        z: common_vendor.o(selectFromDetail)
      }) : {});
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-4d871800"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/ServiceCompanionSelect/ServiceCompanionSelect.js.map

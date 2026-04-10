"use strict";
const common_vendor = require("../../common/vendor.js");
const _sfc_main = {
  data() {
    return {
      // --- 保持原有的 orderData 字段结构不变 ---
      orderData: {
        attendantId: 0,
        attendantName: "",
        attendantPhone: "",
        contactPerson: "",
        contactPhone: "",
        orderNo: "",
        orderStatus: "",
        payMethod: "wechat",
        // 默认支付方式 (注意：这里现在是 orderData.payMethod)
        qrCode: "",
        serviceTypes: [],
        // --- 保留前端原有的字段 ---
        amount: 0,
        // 从缓存或计算填充
        hospital: "",
        // 从缓存填充
        serviceTime: "",
        // 从缓存填充
        specialRequirements: "",
        // 从缓存填充
        avatarUrl: "",
        // 新增：陪诊师头像URL
        selectedServices: [],
        // 新增：从上一页传来的服务类型
        // --- 新增：用于显示症状描述的字段 ---
        operationName: "",
        // 手术名称
        selectedSymptoms: [],
        // 选中的症状索引
        symptomsList: ["胸痛", "头痛", "发热", "呼吸困难", "恶心呕吐", "腹痛", "头晕"],
        // 症状列表
        otherNeeds: ""
        // 其他需求
        // ---
      },
      remark: "",
      // 用户输入的补充备注
      showInvoice: false,
      // 发票展开状态
      // --- 修改点：移除 agree 字段 ---
      // agree: false, // 协议勾选状态 (不再需要)
      // --- 修改点：定义根级别的 payMethod 并初始化 ---
      payMethod: "wechat",
      // 根级别的支付方式，默认值
      isLoading: false
      // 页面加载状态
      // isFetchingDetail: false // 标志位，不再需要
    };
  },
  // computed: {
  //     // 计算属性：根据 selectedServices 和 serviceTime 动态计算金额 (已移除，直接使用缓存的金额)
  //     // computedAmount() { ... } // 删除此计算属性
  // },
  onLoad(options) {
    common_vendor.index.__f__("log", "at pages/OrderConfirmPage/OrderConfirmPage.vue:188", "【OrderConfirmPage】页面加载参数:", options);
    let storedData = null;
    try {
      storedData = common_vendor.index.getStorageSync("orderConfirmData");
      common_vendor.index.__f__("log", "at pages/OrderConfirmPage/OrderConfirmPage.vue:193", "【OrderConfirmPage】从 Storage 读取到完整订单数据:", storedData);
    } catch (e) {
      common_vendor.index.__f__("warn", "at pages/OrderConfirmPage/OrderConfirmPage.vue:195", "【OrderConfirmPage】从 Storage 读取完整订单数据失败", e);
    }
    if (storedData) {
      common_vendor.index.__f__("log", "at pages/OrderConfirmPage/OrderConfirmPage.vue:200", "【OrderConfirmPage】使用缓存中的订单数据填充页面");
      this.orderData = {
        ...this.orderData,
        ...storedData
      };
      this.remark = storedData.remark || "";
      this.payMethod = storedData.payMethod || "wechat";
      this.orderData.payMethod = this.payMethod;
      if (storedData.selectedServices) {
        this.orderData.selectedServices = storedData.selectedServices;
      }
    } else {
      common_vendor.index.__f__("warn", "at pages/OrderConfirmPage/OrderConfirmPage.vue:216", "【OrderConfirmPage】未找到缓存的订单数据，使用示例数据");
      const exampleData = {
        orderNo: "PZ202512250001_EXAMPLE",
        amount: 19800,
        // 示例值（单位：分）
        attendantName: "示例陪诊师",
        hospital: "示例医院",
        // 示例医院
        serviceTime: "2025-12-25 09:00",
        // 示例时间 (就诊时间)
        contactPerson: "示例就诊人",
        payMethod: "wechat",
        // 示例支付方式
        qrCode: "",
        // 示例数据可能没有二维码
        avatarUrl: "/static/default_avatar.png",
        // 示例头像
        selectedServices: ["consult", "exam"],
        // 示例服务类型
        // --- 示例：症状相关数据 ---
        operationName: "阑尾切除术",
        selectedSymptoms: [2],
        // ['发热']
        otherNeeds: "希望陪诊师有经验"
        // ---
      };
      this.orderData = {
        ...this.orderData,
        ...exampleData
      };
      this.payMethod = exampleData.payMethod;
    }
  },
  methods: {
    // /** * 调用后端接口获取订单详情 * @param {string} orderNo - 订单号 */
    // async fetchOrderDetail(orderNo) { // ... 原来的 fetchOrderDetail 逻辑被移除 ... },
    /** * 处理支付方式变更 * @param {Object} e - 事件对象 */
    onPaymentChange(e) {
      this.payMethod = e.detail.value;
      this.orderData.payMethod = e.detail.value;
      common_vendor.index.__f__("log", "at pages/OrderConfirmPage/OrderConfirmPage.vue:250", "【支付方式变更】", this.payMethod);
    },
    // /** // * 移除协议勾选处理函数 // * @param {Object} event - 事件对象 // */
    // onAgreeChange(event) { // ... 之前的逻辑 ... },
    /** * 确认支付 (修改版 - 移除协议检查) */
    confirmPay() {
      common_vendor.index.__f__("log", "at pages/OrderConfirmPage/OrderConfirmPage.vue:257", "【开始支付流程】", {
        orderNo: this.orderData.orderNo,
        // 使用根级别的 payMethod
        paymentMethod: this.payMethod,
        remark: this.remark
      });
      const currentOrderData = {
        ...this.orderData,
        // amount: this.computedAmount, // 删除此行，使用缓存中的 amount
        payMethod: this.payMethod,
        remark: this.remark
      };
      common_vendor.index.setStorageSync("orderConfirmData", currentOrderData);
      common_vendor.index.navigateTo({
        url: "/pages/PaymentSuccessPage/PaymentSuccessPage"
      });
    },
    /** * 切换发票信息显示 */
    toggleInvoice() {
      this.showInvoice = !this.showInvoice;
    },
    /** * 获取头像URL (示例) */
    getAvatarUrl() {
      return "/static/default_avatar.png";
    },
    /** * 跳转到协议页面 (示例) */
    goToAgreement() {
      common_vendor.index.navigateTo({ url: "/pages/agreement/service" });
    },
    /** * 跳转到隐私政策页面 (示例) */
    goToPrivacy() {
      common_vendor.index.navigateTo({ url: "/pages/agreement/privacy" });
    },
    /** * 格式化金额 (分 -> 元) * @param {number} amountInCents - 以分为单位的金额 * @returns {string} 格式化后的金额字符串 */
    formatAmount(amountInCents) {
      if (typeof amountInCents !== "number" || isNaN(amountInCents)) {
        return "0.00";
      }
      return (amountInCents / 100).toFixed(2);
    },
    /** * 编辑医院 */
    editHospital() {
      common_vendor.index.showModal({
        title: "修改医院",
        editable: true,
        placeholderText: this.orderData.hospital || "请输入医院名称",
        success: (res) => {
          if (res.confirm && res.content.trim() !== "") {
            this.orderData.hospital = res.content.trim();
            this.updateCache();
          }
        }
      });
    },
    /** * 编辑时间 */
    editTime() {
      let initialDate = "2025-01-01";
      let initialTime = "09:00";
      if (this.orderData.serviceTime) {
        const parts = this.orderData.serviceTime.split(" ");
        if (parts.length >= 2) {
          initialDate = parts[0];
          initialTime = parts[1];
        }
      }
      common_vendor.index.showActionSheet({
        itemList: ["修改日期", "修改时间"],
        success: (res) => {
          if (res.tapIndex === 0) {
            common_vendor.index.showDatePicker({
              value: initialDate,
              success: (dateRes) => {
                const newDate = dateRes.value;
                const timePart = this.orderData.serviceTime ? this.orderData.serviceTime.split(" ")[1] : "09:00";
                this.orderData.serviceTime = `${newDate} ${timePart}`;
                this.updateCache();
              }
            });
          } else if (res.tapIndex === 1) {
            common_vendor.index.showTimePicker({
              value: initialTime,
              success: (timeRes) => {
                const newTime = timeRes.value;
                const datePart = this.orderData.serviceTime ? this.orderData.serviceTime.split(" ")[0] : "2025-01-01";
                this.orderData.serviceTime = `${datePart} ${newTime}`;
                this.updateCache();
              }
            });
          }
        }
      });
    },
    /** * 编辑就诊人 */
    editPatient() {
      common_vendor.index.showModal({
        title: "修改就诊人",
        editable: true,
        placeholderText: this.orderData.contactPerson || "请输入就诊人姓名",
        success: (res) => {
          if (res.confirm && res.content.trim() !== "") {
            this.orderData.contactPerson = res.content.trim();
            this.updateCache();
          }
        }
      });
    },
    /** * 编辑症状 (示例) */
    editSymptom() {
      common_vendor.index.showModal({
        title: "修改症状描述",
        editable: true,
        placeholderText: this.getSymptomDescription() || "请输入症状描述",
        success: (res) => {
          if (res.confirm && res.content.trim() !== "") {
            this.orderData.otherNeeds = res.content.trim();
            this.updateCache();
          }
        }
      });
    },
    /** * 更新缓存 */
    updateCache() {
      const currentOrderData = {
        ...this.orderData,
        payMethod: this.payMethod,
        remark: this.remark
      };
      common_vendor.index.setStorageSync("orderConfirmData", currentOrderData);
      common_vendor.index.__f__("log", "at pages/OrderConfirmPage/OrderConfirmPage.vue:404", "【OrderConfirmPage】数据已更新到缓存:", currentOrderData);
    },
    // --- 新增：获取头像完整路径 ---
    getImagePath(avatarFileName) {
      if (avatarFileName && (avatarFileName.startsWith("http://") || avatarFileName.startsWith("https://"))) {
        return avatarFileName;
      }
      return avatarFileName ? `/static/${avatarFileName}` : this.getAvatarUrl();
    },
    // ---
    // --- 新增：获取症状描述文本 ---
    getSymptomDescription() {
      const { operationName, selectedSymptoms, otherNeeds, symptomsList } = this.orderData;
      const parts = [];
      if (operationName) {
        parts.push(operationName);
      }
      if (selectedSymptoms && selectedSymptoms.length > 0) {
        const symptomNames = selectedSymptoms.map((index) => symptomsList[index]).filter((name) => name);
        if (symptomNames.length > 0) {
          parts.push(...symptomNames);
        }
      }
      if (otherNeeds) {
        parts.push(otherNeeds);
      }
      return parts.join(", ");
    }
    // ---
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: $data.isLoading
  }, $data.isLoading ? {} : {
    b: common_vendor.t($data.orderData.hospital || "未知"),
    c: common_vendor.o((...args) => $options.editHospital && $options.editHospital(...args)),
    d: common_vendor.t($data.orderData.serviceTime || "未知"),
    e: common_vendor.o((...args) => $options.editTime && $options.editTime(...args)),
    f: common_vendor.t($data.orderData.contactPerson || "未知"),
    g: common_vendor.o((...args) => $options.editPatient && $options.editPatient(...args)),
    h: common_vendor.t($options.getSymptomDescription() || "无"),
    i: common_vendor.o((...args) => $options.editSymptom && $options.editSymptom(...args))
  }, {
    j: !$data.isLoading
  }, !$data.isLoading ? {
    k: $options.getImagePath($data.orderData.avatarUrl),
    l: common_vendor.t($data.orderData.attendantName || "未知陪诊师")
  } : {}, {
    m: !$data.isLoading
  }, !$data.isLoading ? {
    n: $data.remark,
    o: common_vendor.o(($event) => $data.remark = $event.detail.value)
  } : {}, {
    p: !$data.isLoading
  }, !$data.isLoading ? common_vendor.e({
    q: common_vendor.o((...args) => $options.toggleInvoice && $options.toggleInvoice(...args)),
    r: $data.showInvoice
  }, $data.showInvoice ? {} : {}) : {}, {
    s: !$data.isLoading
  }, !$data.isLoading ? {
    t: common_vendor.t($options.formatAmount($data.orderData.amount)),
    v: common_vendor.t($options.formatAmount($data.orderData.amount))
  } : {}, {
    w: !$data.isLoading
  }, !$data.isLoading ? {
    x: $data.payMethod === "wechat",
    y: $data.payMethod === "alipay",
    z: $data.payMethod === "unionpay",
    A: common_vendor.o((...args) => $options.onPaymentChange && $options.onPaymentChange(...args)),
    B: $data.payMethod
  } : {}, {
    C: !$data.isLoading
  }, !$data.isLoading ? {
    D: common_vendor.o((...args) => $options.goToAgreement && $options.goToAgreement(...args)),
    E: common_vendor.o((...args) => $options.goToPrivacy && $options.goToPrivacy(...args))
  } : {}, {
    F: !$data.isLoading
  }, !$data.isLoading ? {
    G: common_vendor.t($options.formatAmount($data.orderData.amount)),
    H: common_vendor.o((...args) => $options.confirmPay && $options.confirmPay(...args))
  } : {});
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-393f027f"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/OrderConfirmPage/OrderConfirmPage.js.map

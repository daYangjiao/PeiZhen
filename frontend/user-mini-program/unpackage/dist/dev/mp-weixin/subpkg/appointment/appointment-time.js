"use strict";
const common_vendor = require("../../common/vendor.js");
const _sfc_main = {
  __name: "appointment-time",
  setup(__props) {
    const serviceInfo = common_vendor.ref({});
    const statusBarHeight = common_vendor.ref(0);
    const selectedDate = common_vendor.ref("");
    const startTime = common_vendor.ref("");
    const endTime = common_vendor.ref("");
    const hospitalAddress = common_vendor.ref("");
    const patientName = common_vendor.ref("");
    const phoneNumber = common_vendor.ref("");
    const phoneError = common_vendor.ref("");
    const isPhoneValid = common_vendor.ref(false);
    const showTimeModal = common_vendor.ref(false);
    const timePickerType = common_vendor.ref("");
    const selectedTime = common_vendor.ref("");
    const minDate = common_vendor.computed(() => {
      const today = /* @__PURE__ */ new Date();
      return today.toISOString().split("T")[0];
    });
    const timePickerTitle = common_vendor.computed(() => {
      return timePickerType.value === "start" ? "选择开始时间" : "选择结束时间";
    });
    const timeOptions = common_vendor.computed(() => {
      const options = [];
      for (let hour = 8; hour <= 18; hour++) {
        for (let minute = 0; minute < 60; minute += 30) {
          const timeStr = `${hour.toString().padStart(2, "0")}:${minute.toString().padStart(2, "0")}`;
          options.push(timeStr);
        }
      }
      return options;
    });
    const goBack = () => {
      const pages = getCurrentPages();
      if (pages.length >= 2) {
        const prevPage = pages[pages.length - 2];
        if (prevPage.route === "pages/appointment/appointment")
          ;
      }
      common_vendor.index.navigateBack();
    };
    const onDateChange = (e) => {
      selectedDate.value = e.detail.value;
    };
    const showStartTimePicker = () => {
      timePickerType.value = "start";
      selectedTime.value = startTime.value;
      showTimeModal.value = true;
    };
    const showEndTimePicker = () => {
      timePickerType.value = "end";
      selectedTime.value = endTime.value;
      showTimeModal.value = true;
    };
    const hideTimePicker = () => {
      showTimeModal.value = false;
      selectedTime.value = "";
    };
    const selectTime = (time) => {
      selectedTime.value = time;
    };
    const confirmTime = () => {
      if (timePickerType.value === "start") {
        if (endTime.value && selectedTime.value >= endTime.value) {
          common_vendor.index.showToast({
            title: "开始时间必须小于结束时间",
            icon: "none"
          });
          return;
        }
        startTime.value = selectedTime.value;
      } else {
        if (startTime.value && selectedTime.value <= startTime.value) {
          common_vendor.index.showToast({
            title: "结束时间必须大于开始时间",
            icon: "none"
          });
          return;
        }
        endTime.value = selectedTime.value;
      }
      hideTimePicker();
    };
    const formatDate = (dateStr) => {
      if (!dateStr)
        return "";
      const date = new Date(dateStr);
      const year = date.getFullYear();
      const month = date.getMonth() + 1;
      const day = date.getDate();
      return `${year}年${month}月${day}日`;
    };
    const validatePhone = () => {
      const phoneRegex = /^1[3-9]\d{9}$/;
      if (!phoneNumber.value) {
        phoneError.value = "";
        isPhoneValid.value = false;
        return;
      }
      if (phoneRegex.test(phoneNumber.value)) {
        phoneError.value = "";
        isPhoneValid.value = true;
      } else {
        phoneError.value = "请输入正确的手机号格式（11位数字，以1开头）";
        isPhoneValid.value = false;
      }
    };
    const confirmAppointment = () => {
      var _a, _b, _c, _d;
      if (!selectedDate.value) {
        common_vendor.index.showToast({
          title: "请选择服务日期",
          icon: "none"
        });
        return;
      }
      if (!startTime.value || !endTime.value) {
        common_vendor.index.showToast({
          title: "请选择服务时间",
          icon: "none"
        });
        return;
      }
      if (!hospitalAddress.value) {
        common_vendor.index.showToast({
          title: "请输入医院地址",
          icon: "none"
        });
        return;
      }
      if (!patientName.value) {
        common_vendor.index.showToast({
          title: "请输入患者姓名",
          icon: "none"
        });
        return;
      }
      if (!phoneNumber.value) {
        common_vendor.index.showToast({
          title: "请输入联系电话",
          icon: "none"
        });
        return;
      }
      validatePhone();
      if (!isPhoneValid.value) {
        common_vendor.index.showToast({
          title: phoneError.value || "请输入正确的手机号格式",
          icon: "none"
        });
        return;
      }
      common_vendor.index.showToast({
        title: "预约成功",
        icon: "success"
      });
      const orderData = {
        hospitalName: hospitalAddress.value,
        serviceDate: selectedDate.value,
        startTime: startTime.value,
        endTime: endTime.value,
        doctorName: ((_b = (_a = serviceInfo.value) == null ? void 0 : _a.companion) == null ? void 0 : _b.name) || "陪诊员",
        doctorAvatar: ((_d = (_c = serviceInfo.value) == null ? void 0 : _c.companion) == null ? void 0 : _d.avatar) || "/static/doctor1.jpg",
        patientName: patientName.value,
        patientPhone: phoneNumber.value,
        serviceType: "普通陪诊",
        serviceDetails: "医院内陪诊，包括挂号、取药、检查等"
      };
      common_vendor.index.removeStorageSync("appointmentSelections");
      setTimeout(() => {
        try {
          common_vendor.index.setStorageSync("newOrderData", JSON.stringify(orderData));
          common_vendor.index.switchTab({
            url: "/pages/order/order"
          });
        } catch (e) {
          common_vendor.index.__f__("error", "at subpkg/appointment/appointment-time.vue:360", "保存订单数据失败:", e);
          common_vendor.index.showToast({
            title: "跳转失败，请重试",
            icon: "none"
          });
        }
      }, 1500);
    };
    common_vendor.onMounted(() => {
      common_vendor.index.getSystemInfo({
        success: (res) => {
          statusBarHeight.value = res.statusBarHeight;
        }
      });
      const pages = getCurrentPages();
      const currentPage = pages[pages.length - 1];
      const options = currentPage.options;
      if (options.service) {
        try {
          serviceInfo.value = JSON.parse(decodeURIComponent(options.service));
          common_vendor.index.__f__("log", "at subpkg/appointment/appointment-time.vue:385", "接收到的服务信息:", serviceInfo.value);
        } catch (e) {
          common_vendor.index.__f__("error", "at subpkg/appointment/appointment-time.vue:387", "解析服务信息失败:", e);
        }
      }
    });
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: statusBarHeight.value + "px",
        b: common_vendor.o(goBack),
        c: selectedDate.value,
        d: minDate.value,
        e: common_vendor.o(onDateChange),
        f: !selectedDate.value
      }, !selectedDate.value ? {} : {
        g: common_vendor.t(formatDate(selectedDate.value))
      }, {
        h: startTime.value
      }, startTime.value ? {
        i: common_vendor.t(startTime.value)
      } : {}, {
        j: common_vendor.o(showStartTimePicker),
        k: endTime.value
      }, endTime.value ? {
        l: common_vendor.t(endTime.value)
      } : {}, {
        m: common_vendor.o(showEndTimePicker),
        n: hospitalAddress.value,
        o: common_vendor.o(($event) => hospitalAddress.value = $event.detail.value),
        p: patientName.value,
        q: common_vendor.o(($event) => patientName.value = $event.detail.value),
        r: common_vendor.o(validatePhone),
        s: phoneNumber.value,
        t: common_vendor.o(($event) => phoneNumber.value = $event.detail.value),
        v: phoneError.value
      }, phoneError.value ? {
        w: common_vendor.t(phoneError.value)
      } : {}, {
        x: common_vendor.o(confirmAppointment),
        y: showTimeModal.value
      }, showTimeModal.value ? {
        z: common_vendor.t(timePickerTitle.value),
        A: common_vendor.o(hideTimePicker),
        B: common_vendor.f(timeOptions.value, (time, k0, i0) => {
          return {
            a: common_vendor.t(time),
            b: time,
            c: common_vendor.n({
              "selected": selectedTime.value === time
            }),
            d: common_vendor.o(($event) => selectTime(time), time)
          };
        }),
        C: common_vendor.o(hideTimePicker),
        D: common_vendor.o(confirmTime),
        E: common_vendor.o(() => {
        }),
        F: common_vendor.o(hideTimePicker)
      } : {});
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-8067b228"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/subpkg/appointment/appointment-time.js.map

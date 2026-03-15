"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_api = require("../../utils/api.js");
const _sfc_main = {
  __name: "02_AppointmentForm",
  setup(__props) {
    const serviceTypeNumber = common_vendor.ref(0);
    const serviceTypeName = common_vendor.ref("");
    common_vendor.onLoad((options) => {
      if (options.serviceTypeNumber) {
        serviceTypeNumber.value = parseInt(options.serviceTypeNumber);
      }
      if (options.serviceTypeName) {
        serviceTypeName.value = decodeURIComponent(options.serviceTypeName);
      }
    });
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
    const symptoms = common_vendor.ref(["胸痛", "头痛", "发热", "呼吸困难", "恶心呕吐", "腹痛", "头晕"]);
    const selectedSymptoms = common_vendor.ref([]);
    const otherRequirements = common_vendor.ref("");
    const showAddSymptomModalFlag = common_vendor.ref(false);
    const newSymptomInput = common_vendor.ref("");
    const newSymptomError = common_vendor.ref("");
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
    const isValidPhone = common_vendor.computed(() => {
      const phoneRegex = /^1[3-9]\d{9}$/;
      return !!phoneNumber.value && phoneRegex.test(String(phoneNumber.value).trim());
    });
    const isFormComplete = common_vendor.computed(() => {
      return !!selectedDate.value && !!startTime.value && !!endTime.value && !!String(hospitalAddress.value || "").trim() && !!String(patientName.value || "").trim() && isValidPhone.value && Array.isArray(selectedSymptoms.value) && selectedSymptoms.value.length > 0;
    });
    const getServiceIcon = (typeNumber) => {
      switch (typeNumber) {
        case 1:
          return "/static/logo_1.png";
        case 2:
          return "/static/logo_2.png";
        case 3:
          return "/static/logo_3.jpg";
        case 4:
          return "/static/logo_4.jpg";
        default:
          return "/static/default_icon.png";
      }
    };
    const toggleSymptom = (index) => {
      if (selectedSymptoms.value.includes(index)) {
        selectedSymptoms.value = selectedSymptoms.value.filter((i) => i !== index);
      } else {
        selectedSymptoms.value.push(index);
      }
    };
    const showAddSymptomModal = () => {
      newSymptomInput.value = "";
      newSymptomError.value = "";
      showAddSymptomModalFlag.value = true;
    };
    const hideAddSymptomModal = () => {
      showAddSymptomModalFlag.value = false;
    };
    const validateNewSymptom = () => {
      if (!newSymptomInput.value.trim()) {
        newSymptomError.value = "请输入症状描述";
        return false;
      }
      if (newSymptomInput.value.trim().length > 50) {
        newSymptomError.value = "症状描述不能超过50个字符";
        return false;
      }
      if (symptoms.value.includes(newSymptomInput.value.trim())) {
        newSymptomError.value = "该症状已存在";
        return false;
      }
      newSymptomError.value = "";
      return true;
    };
    const confirmAddSymptom = () => {
      if (!validateNewSymptom()) {
        return;
      }
      symptoms.value.push(newSymptomInput.value.trim());
      hideAddSymptomModal();
      common_vendor.index.showToast({
        title: "症状添加成功",
        icon: "success"
      });
    };
    const confirmAppointment = async () => {
      const missing = [];
      if (!selectedDate.value)
        missing.push("服务日期");
      if (!startTime.value || !endTime.value)
        missing.push("服务时段");
      if (!String(hospitalAddress.value || "").trim())
        missing.push("医院地址");
      if (!selectedSymptoms.value || selectedSymptoms.value.length === 0)
        missing.push("症状（至少选择1项）");
      if (!String(patientName.value || "").trim())
        missing.push("姓名");
      if (!String(phoneNumber.value || "").trim())
        missing.push("手机号");
      if (missing.length > 0) {
        common_vendor.index.showModal({
          title: "提示",
          content: `请先完善：${missing.join("、")}，全部输入完成后才能点击确认预约`,
          showCancel: false
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
      const toMinutes = (t) => {
        const [h, m] = String(t).split(":").map(Number);
        return (h || 0) * 60 + (m || 0);
      };
      if (toMinutes(endTime.value) <= toMinutes(startTime.value)) {
        common_vendor.index.showToast({ title: "结束时间需晚于开始时间", icon: "none" });
        return;
      }
      const demandData = {
        // 服务类型信息
        serviceTypeNumber: serviceTypeNumber.value,
        // 确保是数字
        // serviceTypeName: serviceTypeName.value, // ⚠️ 移除这行，因为后端不识别
        // 服务时间信息
        serviceDate: selectedDate.value,
        // YYYY-MM-DD
        serviceStartTime: startTime.value,
        // HH:mm
        serviceEndTime: endTime.value,
        // HH:mm
        // 医院信息
        hospital: String(hospitalAddress.value || "").trim(),
        // 联系人信息
        patientName: String(patientName.value || "").trim(),
        patientPhone: String(phoneNumber.value || "").trim(),
        // --- 新增：症状和需求 ---
        // 症状 (确保是一个数组)
        symptoms: selectedSymptoms.value.map((i) => symptoms.value[i]),
        // 将索引转换为实际症状文本
        // 其他需求 (确保是一个字符串)
        otherRequirement: otherRequirements.value
        // --- 结束新增 ---
      };
      common_vendor.index.__f__("log", "at subpkg/appointment-flow/02_AppointmentForm.vue:522", "【02页面】即将发送的请求数据 (JSON.stringify):", JSON.stringify(demandData, null, 2));
      common_vendor.index.__f__("log", "at subpkg/appointment-flow/02_AppointmentForm.vue:523", "【02页面】即将发送的请求数据 (JS Object):", demandData);
      common_vendor.index.__f__("log", "at subpkg/appointment-flow/02_AppointmentForm.vue:526", "【02页面】字段类型检查:");
      common_vendor.index.__f__("log", "at subpkg/appointment-flow/02_AppointmentForm.vue:527", "  serviceTypeNumber:", typeof demandData.serviceTypeNumber, demandData.serviceTypeNumber);
      common_vendor.index.__f__("log", "at subpkg/appointment-flow/02_AppointmentForm.vue:528", "  serviceDate:", typeof demandData.serviceDate, demandData.serviceDate);
      common_vendor.index.__f__("log", "at subpkg/appointment-flow/02_AppointmentForm.vue:529", "  serviceStartTime:", typeof demandData.serviceStartTime, demandData.serviceStartTime);
      common_vendor.index.__f__("log", "at subpkg/appointment-flow/02_AppointmentForm.vue:530", "  serviceEndTime:", typeof demandData.serviceEndTime, demandData.serviceEndTime);
      common_vendor.index.__f__("log", "at subpkg/appointment-flow/02_AppointmentForm.vue:531", "  hospital:", typeof demandData.hospital, demandData.hospital);
      common_vendor.index.__f__("log", "at subpkg/appointment-flow/02_AppointmentForm.vue:532", "  patientName:", typeof demandData.patientName, demandData.patientName);
      common_vendor.index.__f__("log", "at subpkg/appointment-flow/02_AppointmentForm.vue:533", "  patientPhone:", typeof demandData.patientPhone, demandData.patientPhone);
      common_vendor.index.__f__("log", "at subpkg/appointment-flow/02_AppointmentForm.vue:534", "  otherRequirement:", typeof demandData.otherRequirement, demandData.otherRequirement);
      common_vendor.index.__f__("log", "at subpkg/appointment-flow/02_AppointmentForm.vue:535", "  symptoms:", typeof demandData.symptoms, demandData.symptoms, Array.isArray(demandData.symptoms));
      try {
        common_vendor.index.showLoading({ title: "提交中..." });
        const submitResponse = await utils_api.post("/ai/guide/appointments", demandData);
        common_vendor.index.__f__("log", "at subpkg/appointment-flow/02_AppointmentForm.vue:543", "【02页面】提交预约响应:", submitResponse);
        if (submitResponse && submitResponse.code === 200) {
          const appointmentNo = submitResponse.data.appointmentNo;
          common_vendor.index.__f__("log", "at subpkg/appointment-flow/02_AppointmentForm.vue:547", "【02页面】获取到预约编号:", appointmentNo);
          if (!appointmentNo) {
            throw new Error("未能获取到预约编号");
          }
          const matchResponse = await utils_api.get(`/ai/guide/attendants/match?appointmentNo=${appointmentNo}`);
          common_vendor.index.__f__("log", "at subpkg/appointment-flow/02_AppointmentForm.vue:555", "【02页面】匹配陪诊师响应:", matchResponse);
          if (matchResponse && matchResponse.code === 200 && matchResponse.data.attendants.length > 0) {
            const firstAttendant = matchResponse.data.attendants[0];
            const attendantId = firstAttendant.id;
            const orderRequest = {
              appointmentNo,
              attendantId: attendantId.toString()
            };
            const orderResponse = await utils_api.post("/ai/guide/orders", orderRequest);
            common_vendor.index.__f__("log", "at subpkg/appointment-flow/02_AppointmentForm.vue:569", "【02页面】创建订单响应:", orderResponse);
            if (orderResponse && orderResponse.code === 200) {
              const orderNo = orderResponse.data.orderNo;
              common_vendor.index.__f__("log", "at subpkg/appointment-flow/02_AppointmentForm.vue:573", "【02页面】获取到订单编号:", orderNo);
              if (!orderNo) {
                throw new Error("未能获取到订单编号");
              }
              common_vendor.index.setStorageSync("orderNo", orderNo);
              common_vendor.index.redirectTo({
                url: "/subpkg/appointment-flow/04_OrderConfirmPage?orderNo=" + orderNo
              });
            } else {
              throw new Error(orderResponse.message || "创建订单失败");
            }
          } else {
            throw new Error("未找到可用陪诊师");
          }
        } else {
          const errorMsg = submitResponse.message || "提交预约失败";
          throw new Error(errorMsg);
        }
      } catch (error) {
        common_vendor.index.__f__("error", "at subpkg/appointment-flow/02_AppointmentForm.vue:597", "【02页面】提交预约失败:", error);
        let errorMessage = error.message || "提交预约失败，请稍后重试";
        if (error.statusCode && error.data) {
          errorMessage = `HTTP ${error.statusCode}: ${error.data.message || "请求失败"}`;
          common_vendor.index.__f__("error", "at subpkg/appointment-flow/02_AppointmentForm.vue:602", "【02页面】API 响应详情:", error.data);
        } else if (error.errMsg) {
          errorMessage = `请求失败: ${error.errMsg}`;
        }
        common_vendor.index.showToast({
          title: errorMessage,
          icon: "none"
        });
      } finally {
        common_vendor.index.hideLoading();
      }
    };
    common_vendor.onMounted(async () => {
      common_vendor.index.getSystemInfo({
        success: (res) => {
          statusBarHeight.value = res.statusBarHeight;
          document.documentElement.style.setProperty("--status-bar-height", `${res.statusBarHeight}px`);
        }
      });
      try {
        const response = await utils_api.get("/api/users/current");
        common_vendor.index.__f__("log", "at subpkg/appointment-flow/02_AppointmentForm.vue:629", "获取用户信息响应:", response);
        if (response && response.code === 200 && response.data) {
          const userData = response.data;
          common_vendor.index.__f__("log", "at subpkg/appointment-flow/02_AppointmentForm.vue:633", "用户数据:", userData);
          if (userData.name && !patientName.value) {
            patientName.value = userData.name;
          }
          if (userData.phone && !phoneNumber.value) {
            phoneNumber.value = userData.phone;
          }
          common_vendor.index.__f__("log", "at subpkg/appointment-flow/02_AppointmentForm.vue:645", "自动填充完成 - 姓名:", patientName.value, "电话:", phoneNumber.value);
        } else {
          common_vendor.index.__f__("log", "at subpkg/appointment-flow/02_AppointmentForm.vue:647", "未获取到用户信息或用户未登录");
        }
      } catch (error) {
        common_vendor.index.__f__("log", "at subpkg/appointment-flow/02_AppointmentForm.vue:650", "获取用户信息失败:", error);
      }
    });
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: getServiceIcon(serviceTypeNumber.value),
        b: common_vendor.t(serviceTypeName.value),
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
        p: common_vendor.f(symptoms.value, (item, index, i0) => {
          return {
            a: selectedSymptoms.value.includes(index),
            b: common_vendor.t(item),
            c: index,
            d: common_vendor.o(($event) => toggleSymptom(index), index)
          };
        }),
        q: common_vendor.o(showAddSymptomModal),
        r: patientName.value,
        s: common_vendor.o(($event) => patientName.value = $event.detail.value),
        t: common_vendor.o(validatePhone),
        v: phoneNumber.value,
        w: common_vendor.o(($event) => phoneNumber.value = $event.detail.value),
        x: phoneError.value
      }, phoneError.value ? {
        y: common_vendor.t(phoneError.value)
      } : {}, {
        z: {
          minHeight: 100
        },
        A: otherRequirements.value,
        B: common_vendor.o(($event) => otherRequirements.value = $event.detail.value),
        C: !isFormComplete.value ? 1 : "",
        D: common_vendor.o(confirmAppointment),
        E: showTimeModal.value
      }, showTimeModal.value ? {
        F: common_vendor.t(timePickerTitle.value),
        G: common_vendor.o(hideTimePicker),
        H: common_vendor.f(timeOptions.value, (time, k0, i0) => {
          return {
            a: common_vendor.t(time),
            b: time,
            c: common_vendor.n({
              "selected": selectedTime.value === time
            }),
            d: common_vendor.o(($event) => selectTime(time), time)
          };
        }),
        I: common_vendor.o(hideTimePicker),
        J: common_vendor.o(confirmTime),
        K: common_vendor.o(() => {
        }),
        L: common_vendor.o(hideTimePicker)
      } : {}, {
        M: showAddSymptomModalFlag.value
      }, showAddSymptomModalFlag.value ? common_vendor.e({
        N: common_vendor.o(hideAddSymptomModal),
        O: common_vendor.o(confirmAddSymptom),
        P: newSymptomInput.value,
        Q: common_vendor.o(($event) => newSymptomInput.value = $event.detail.value),
        R: newSymptomError.value
      }, newSymptomError.value ? {
        S: common_vendor.t(newSymptomError.value)
      } : {}, {
        T: common_vendor.o(hideAddSymptomModal),
        U: common_vendor.o(confirmAddSymptom),
        V: common_vendor.o(() => {
        }),
        W: common_vendor.o(hideAddSymptomModal)
      }) : {});
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-7ae4f2b4"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/subpkg/appointment-flow/02_AppointmentForm.js.map

"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_api = require("../../utils/api.js");
const _sfc_main = {
  __name: "AppointmentForm",
  setup(__props) {
    const getCurrentDate = () => {
      const now = /* @__PURE__ */ new Date();
      const year = now.getFullYear();
      const month = String(now.getMonth() + 1).padStart(2, "0");
      const day = String(now.getDate()).padStart(2, "0");
      return `${year}-${month}-${day}`;
    };
    const serviceDate = common_vendor.ref(getCurrentDate());
    const startTime = common_vendor.ref("14:00");
    const endTime = common_vendor.ref("16:00");
    const hospitalName = common_vendor.ref("");
    const contactPerson = common_vendor.ref("");
    const contactPhone = common_vendor.ref("");
    const notes = common_vendor.ref("");
    const onDateChange = (e) => {
      serviceDate.value = e.detail.value;
    };
    const onStartTimeChange = (e) => {
      startTime.value = e.detail.value;
    };
    const onEndTimeChange = (e) => {
      endTime.value = e.detail.value;
    };
    const timeToMinutes = (timeStr) => {
      if (!timeStr)
        return -1;
      const [h, m] = timeStr.split(":").map(Number);
      return h * 60 + m;
    };
    const isValidPhone = (phone) => {
      const reg = /^1[3-9]\d{9}$/;
      return reg.test(phone.trim());
    };
    const confirmAppointment = async () => {
      if (!serviceDate.value || !startTime.value || !endTime.value || !hospitalName.value.trim() || !contactPerson.value.trim() || !contactPhone.value.trim()) {
        common_vendor.index.showToast({
          title: "请填写必要信息",
          icon: "none"
        });
        return;
      }
      if (!isValidPhone(contactPhone.value)) {
        common_vendor.index.showToast({
          title: "请输入正确的11位手机号",
          icon: "none"
        });
        return;
      }
      if (timeToMinutes(endTime.value) <= timeToMinutes(startTime.value)) {
        common_vendor.index.showToast({
          title: "结束时间需晚于开始时间",
          icon: "none"
        });
        return;
      }
      const formData = {
        serviceDate: serviceDate.value,
        startTime: startTime.value,
        endTime: endTime.value,
        hospitalName: hospitalName.value.trim(),
        contactPerson: contactPerson.value.trim(),
        contactPhone: contactPhone.value.trim(),
        notes: notes.value.trim()
      };
      try {
        const res = await utils_api.post("/utils/api.js", formData);
        common_vendor.index.showToast({
          title: "预约提交成功",
          icon: "success"
        });
        const appointmentNo = res.data.appointmentNo;
        setTimeout(() => {
          common_vendor.index.navigateTo({
            url: `/pages/AICareMatchPage/AICareMatchPage?appointmentNo=${encodeURIComponent(appointmentNo)}`
          });
        }, 1e3);
      } catch (err) {
        common_vendor.index.__f__("error", "at pages/AppointmentForm/AppointmentForm.vue:179", "预约提交失败：", err);
      }
    };
    return (_ctx, _cache) => {
      return {
        a: common_vendor.t(serviceDate.value ? serviceDate.value.replace(/-/g, "/") : "请选择日期"),
        b: serviceDate.value,
        c: common_vendor.o(onDateChange),
        d: common_vendor.t(startTime.value || "开始"),
        e: startTime.value,
        f: common_vendor.o(onStartTimeChange),
        g: common_vendor.t(endTime.value || "结束"),
        h: endTime.value,
        i: common_vendor.o(onEndTimeChange),
        j: hospitalName.value,
        k: common_vendor.o(($event) => hospitalName.value = $event.detail.value),
        l: contactPerson.value,
        m: common_vendor.o(($event) => contactPerson.value = $event.detail.value),
        n: common_vendor.o([($event) => contactPhone.value = $event.detail.value, ($event) => contactPhone.value = contactPhone.value.replace(/\D/g, "").slice(0, 11)]),
        o: contactPhone.value,
        p: notes.value,
        q: common_vendor.o(($event) => notes.value = $event.detail.value),
        r: common_vendor.o(confirmAppointment)
      };
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-76c3d40c"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/AppointmentForm/AppointmentForm.js.map

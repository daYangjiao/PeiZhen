"use strict";
const common_vendor = require("../../common/vendor.js");
const common_assets = require("../../common/assets.js");
const utils_api = require("../../utils/api.js");
const _sfc_main = {
  __name: "InviteAttendant",
  setup(__props) {
    const orderInfo = common_vendor.ref({});
    const attendants = common_vendor.ref([]);
    const loading = common_vendor.ref(true);
    const showInviteModal = common_vendor.ref(false);
    const selectedAttendant = common_vendor.ref({});
    const selectedDate = common_vendor.ref("");
    const selectedTimeSlot = common_vendor.ref("");
    const professionalField = common_vendor.ref("");
    const timeSlots = ["09:00-12:00", "14:00-17:00", "19:00-21:00"];
    common_vendor.onLoad((options) => {
      common_vendor.index.__f__("log", "at subpkg/attendant/InviteAttendant.vue:167", "邀请陪诊师页面加载参数:", options);
      if (options && options.orderNo) {
        loadOrderInfo(options.orderNo);
      } else {
        common_vendor.index.showToast({
          title: "缺少订单信息",
          icon: "none"
        });
        setTimeout(() => {
          common_vendor.index.navigateBack();
        }, 1500);
      }
    });
    const loadOrderInfo = async (orderNo) => {
      try {
        const response = await utils_api.get(`/ai/guide/orders/${orderNo}`);
        if (response && response.data) {
          orderInfo.value = response.data;
          if (orderInfo.value.appointmentTime) {
            const date = new Date(orderInfo.value.appointmentTime);
            selectedDate.value = date.toISOString().split("T")[0];
          }
          searchAttendants();
        }
      } catch (error) {
        common_vendor.index.__f__("error", "at subpkg/attendant/InviteAttendant.vue:195", "加载订单信息失败:", error);
        common_vendor.index.showToast({
          title: "加载订单信息失败",
          icon: "none"
        });
      }
    };
    const searchAttendants = async () => {
      loading.value = true;
      try {
        const params = {
          hospitalName: orderInfo.value.hospital || "",
          serviceDate: selectedDate.value,
          serviceTimeSlot: selectedTimeSlot.value,
          professionalField: professionalField.value
        };
        const response = await utils_api.get("/user/attendants/available", params);
        if (response && response.data) {
          attendants.value = response.data.map((attendant) => ({
            ...attendant,
            isInvited: false,
            availableTimes: attendant.availableTimeSlots ? attendant.availableTimeSlots.split(",") : ["09:00-12:00"]
          }));
        }
      } catch (error) {
        common_vendor.index.__f__("error", "at subpkg/attendant/InviteAttendant.vue:225", "搜索陪诊师失败:", error);
        common_vendor.index.showToast({
          title: "搜索失败",
          icon: "none"
        });
      } finally {
        loading.value = false;
      }
    };
    const onDateChange = (e) => {
      selectedDate.value = e.detail.value;
    };
    const onTimeSlotChange = (e) => {
      selectedTimeSlot.value = timeSlots[e.detail.value];
    };
    const selectAttendant = (attendant) => {
      common_vendor.index.__f__("log", "at subpkg/attendant/InviteAttendant.vue:247", "选择陪诊师:", attendant);
    };
    const inviteAttendant = (attendant) => {
      if (attendant.isInvited)
        return;
      selectedAttendant.value = attendant;
      showInviteModal.value = true;
    };
    const confirmInvite = async () => {
      try {
        const response = await utils_api.post("/user/invitations", {
          orderId: orderInfo.value.orderId,
          attendantId: selectedAttendant.value.id,
          invitationMessage: `您好，邀请您接单：${orderInfo.value.hospital} ${formatDate(orderInfo.value.appointmentTime)}`
        });
        if (response && response.code === 200) {
          common_vendor.index.showToast({
            title: "邀请已发送",
            icon: "success"
          });
          const index = attendants.value.findIndex((a) => a.id === selectedAttendant.value.id);
          if (index !== -1) {
            attendants.value[index].isInvited = true;
          }
          closeInviteModal();
          setTimeout(() => {
            common_vendor.index.navigateBack();
          }, 3e3);
        }
      } catch (error) {
        common_vendor.index.__f__("error", "at subpkg/attendant/InviteAttendant.vue:288", "邀请失败:", error);
        common_vendor.index.showToast({
          title: "邀请失败",
          icon: "none"
        });
      }
    };
    const closeInviteModal = () => {
      showInviteModal.value = false;
      selectedAttendant.value = {};
    };
    const getAvatarUrl = (avatarPath) => {
      if (!avatarPath)
        return "/static/default-avatar.jpg";
      if (avatarPath.startsWith("http"))
        return avatarPath;
      return `http://localhost:8080${avatarPath.startsWith("/") ? avatarPath : "/" + avatarPath}`;
    };
    const handleImageError = (e) => {
      common_vendor.index.__f__("log", "at subpkg/attendant/InviteAttendant.vue:311", "图片加载失败:", e);
    };
    const formatDate = (dateString) => {
      if (!dateString)
        return "";
      const date = new Date(dateString);
      return date.toLocaleString("zh-CN", {
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit"
      }).replace(/\//g, "-");
    };
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_vendor.t(orderInfo.value.orderNo),
        b: common_vendor.t(formatDate(orderInfo.value.appointmentTime)),
        c: common_vendor.t(orderInfo.value.hospital),
        d: common_vendor.t(selectedDate.value || "请选择日期"),
        e: selectedDate.value,
        f: common_vendor.o(onDateChange),
        g: common_vendor.t(selectedTimeSlot.value || "请选择时段"),
        h: timeSlots,
        i: common_vendor.o(onTimeSlotChange),
        j: common_vendor.o(searchAttendants),
        k: professionalField.value,
        l: common_vendor.o(($event) => professionalField.value = $event.detail.value),
        m: common_vendor.o(searchAttendants),
        n: loading.value
      }, loading.value ? {} : common_vendor.e({
        o: common_vendor.t(attendants.value.length),
        p: common_vendor.f(attendants.value, (attendant, k0, i0) => {
          return {
            a: getAvatarUrl(attendant.avatarUrl),
            b: common_vendor.o(handleImageError, attendant.id),
            c: common_vendor.t(attendant.name),
            d: common_vendor.t(attendant.score || 5),
            e: common_vendor.t(attendant.introduction || "暂无简介"),
            f: common_vendor.t(attendant.professionalField || "通用陪诊"),
            g: common_vendor.t(attendant.experienceYears || 0),
            h: common_vendor.t(attendant.price || 0),
            i: common_vendor.f(attendant.availableTimes, (time, k1, i1) => {
              return {
                a: common_vendor.t(time),
                b: time
              };
            }),
            j: common_vendor.t(attendant.isInvited ? "已邀请" : "邀请接单"),
            k: attendant.isInvited ? 1 : "",
            l: common_vendor.o(($event) => inviteAttendant(attendant), attendant.id),
            m: attendant.isInvited,
            n: attendant.id,
            o: common_vendor.o(($event) => selectAttendant(attendant), attendant.id)
          };
        }),
        q: attendants.value.length === 0 && !loading.value
      }, attendants.value.length === 0 && !loading.value ? {
        r: common_assets._imports_0$7
      } : {}), {
        s: showInviteModal.value
      }, showInviteModal.value ? {
        t: common_vendor.t(selectedAttendant.value.name),
        v: common_vendor.o(closeInviteModal),
        w: common_vendor.o(confirmInvite),
        x: common_vendor.o(() => {
        }),
        y: common_vendor.o(closeInviteModal)
      } : {});
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-139efa28"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/subpkg/attendant/InviteAttendant.js.map

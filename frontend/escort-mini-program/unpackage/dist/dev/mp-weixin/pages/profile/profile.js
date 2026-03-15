"use strict";
const common_vendor = require("../../common/vendor.js");
const common_assets = require("../../common/assets.js");
const utils_api = require("../../utils/api.js");
const _sfc_main = {
  __name: "profile",
  setup(__props) {
    const attendantInfo = common_vendor.ref({});
    common_vendor.onMounted(() => {
      loadAttendantInfo();
    });
    const getFullAvatarUrl = (relativePath) => {
      if (!relativePath) {
        return "/static/default-avatar.jpg";
      }
      if (relativePath.startsWith("http")) {
        return relativePath;
      }
      const baseUrl = utils_api.config.baseURL.endsWith("/") ? utils_api.config.baseURL : utils_api.config.baseURL + "/";
      const avatarPath = relativePath.startsWith("/") ? relativePath.substring(1) : relativePath;
      return baseUrl + avatarPath;
    };
    const loadAttendantInfo = async () => {
      const userInfo = common_vendor.index.getStorageSync("userInfo");
      if (!userInfo || !userInfo.id) {
        common_vendor.index.showToast({ title: "请先登录", icon: "none" });
        setTimeout(() => {
          common_vendor.index.reLaunch({ url: "/subpkg/auth/login" });
        }, 1500);
        return;
      }
      try {
        const res = await utils_api.get(`/attendant/profile/${userInfo.id}`);
        if (res.code === 200 && res.data) {
          attendantInfo.value = res.data;
        } else {
          attendantInfo.value = userInfo;
        }
      } catch (e) {
        common_vendor.index.__f__("error", "at pages/profile/profile.vue:137", "获取陪诊师信息失败:", e);
        attendantInfo.value = userInfo;
      }
    };
    const editProfile = () => {
      common_vendor.index.showToast({ title: "编辑功能开发中", icon: "none" });
    };
    const navigateTo = (url) => {
      common_vendor.index.showToast({ title: "功能开发中", icon: "none" });
    };
    const logout = () => {
      common_vendor.index.showModal({
        title: "确认退出",
        content: "确定要退出登录吗？",
        success: (res) => {
          if (res.confirm) {
            utils_api.clearToken();
            common_vendor.index.removeStorageSync("userInfo");
            common_vendor.index.removeStorageSync("isLoggedIn");
            common_vendor.index.showToast({ title: "已退出登录", icon: "success" });
            setTimeout(() => {
              common_vendor.index.reLaunch({ url: "/subpkg/auth/login" });
            }, 1500);
          }
        }
      });
    };
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: getFullAvatarUrl(attendantInfo.value.avatarUrl || attendantInfo.value.avatar),
        b: common_vendor.t(attendantInfo.value.name || attendantInfo.value.username || "陪诊师"),
        c: common_vendor.t(attendantInfo.value.phone || "暂无电话"),
        d: common_vendor.o(editProfile),
        e: common_vendor.t(attendantInfo.value.totalOrders || 0),
        f: common_vendor.t(attendantInfo.value.completedOrders || 0),
        g: common_vendor.t(attendantInfo.value.score || "5.0"),
        h: common_vendor.t(attendantInfo.value.totalEarnings || "0.00"),
        i: common_assets._imports_0$3,
        j: attendantInfo.value.certificate
      }, attendantInfo.value.certificate ? {} : {}, {
        k: common_vendor.o(($event) => navigateTo()),
        l: common_assets._imports_1$1,
        m: common_vendor.t(attendantInfo.value.balance || "0.00"),
        n: common_vendor.o(($event) => navigateTo()),
        o: common_assets._imports_2$2,
        p: common_vendor.o(($event) => navigateTo()),
        q: common_assets._imports_3,
        r: common_vendor.o(($event) => navigateTo()),
        s: common_assets._imports_4,
        t: common_vendor.o(($event) => navigateTo()),
        v: common_assets._imports_5,
        w: common_vendor.o(($event) => navigateTo()),
        x: common_vendor.o(logout)
      });
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-dd383ca2"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/profile/profile.js.map

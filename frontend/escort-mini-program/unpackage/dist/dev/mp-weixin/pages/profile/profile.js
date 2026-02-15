"use strict";
const common_vendor = require("../../common/vendor.js");
const common_assets = require("../../common/assets.js");
const _sfc_main = {
  __name: "profile",
  setup(__props) {
    const userInfo = common_vendor.ref({
      name: "陪诊师小王",
      phone: "138****8888",
      avatar: "/static/provider1.jpg",
      status: "online",
      // online, offline, busy
      totalOrders: 156,
      completedOrders: 148,
      rating: 4.9,
      totalEarnings: 28560,
      balance: 1250.5,
      isVerified: true
    });
    common_vendor.onMounted(() => {
      loadUserInfo();
    });
    const loadUserInfo = () => {
      common_vendor.index.__f__("log", "at pages/profile/profile.vue:122", "加载用户信息");
    };
    const getStatusText = (status) => {
      const statusMap = {
        online: "在线接单",
        offline: "离线",
        busy: "忙碌中"
      };
      return statusMap[status] || "未知状态";
    };
    const editProfile = () => {
      common_vendor.index.navigateTo({
        url: "/subpkg/profile/edit-profile"
      });
    };
    const navigateTo = (url) => {
      common_vendor.index.showToast({
        title: "功能开发中",
        icon: "none"
      });
    };
    const logout = () => {
      common_vendor.index.showModal({
        title: "确认退出",
        content: "确定要退出登录吗？",
        success: (res) => {
          if (res.confirm) {
            common_vendor.index.showToast({
              title: "已退出登录",
              icon: "success"
            });
            setTimeout(() => {
              common_vendor.index.reLaunch({
                url: "/subpkg/auth/login"
              });
            }, 1500);
          }
        }
      });
    };
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: userInfo.value.avatar,
        b: common_vendor.t(userInfo.value.name),
        c: common_vendor.t(userInfo.value.phone),
        d: common_vendor.t(getStatusText(userInfo.value.status)),
        e: common_vendor.n(userInfo.value.status),
        f: common_vendor.o(editProfile),
        g: common_vendor.t(userInfo.value.totalOrders),
        h: common_vendor.t(userInfo.value.completedOrders),
        i: common_vendor.t(userInfo.value.rating),
        j: common_vendor.t(userInfo.value.totalEarnings),
        k: common_assets._imports_1$1,
        l: userInfo.value.isVerified
      }, userInfo.value.isVerified ? {} : {}, {
        m: common_vendor.o(($event) => navigateTo()),
        n: common_assets._imports_1,
        o: common_vendor.t(userInfo.value.balance),
        p: common_vendor.o(($event) => navigateTo()),
        q: common_assets._imports_2,
        r: common_vendor.o(($event) => navigateTo()),
        s: common_assets._imports_3,
        t: common_vendor.o(($event) => navigateTo()),
        v: common_assets._imports_4,
        w: common_vendor.o(($event) => navigateTo()),
        x: common_assets._imports_5,
        y: common_vendor.o(($event) => navigateTo()),
        z: common_vendor.o(logout)
      });
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-dd383ca2"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/profile/profile.js.map

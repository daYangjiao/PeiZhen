"use strict";
const common_vendor = require("../../common/vendor.js");
const common_assets = require("../../common/assets.js");
const stores_user = require("../../stores/user.js");
const api_user = require("../../api/user.js");
const _sfc_main = {
  __name: "profile",
  setup(__props) {
    const userStore = common_vendor.ref(null);
    const goToLogin = () => {
      common_vendor.index.navigateTo({
        url: "/subpkg/auth/login?redirect=" + encodeURIComponent("/pages/profile/profile")
      });
    };
    const editProfile = () => {
      common_vendor.index.__f__("log", "at pages/profile/profile.vue:114", "🚀🚀🚀 点击了编辑按钮，准备跳转到编辑页面 🚀🚀🚀");
      common_vendor.index.navigateTo({
        url: "/subpkg/profile/edit-profile"
      });
    };
    const goToOrders = () => {
      if (!userStore.value.checkLoginStatus("/pages/order/order")) {
        return;
      }
      common_vendor.index.switchTab({
        url: "/pages/order/order"
      });
    };
    const goToAppointments = () => {
      if (!userStore.value.checkLoginStatus("/pages/appointment/appointment")) {
        return;
      }
      common_vendor.index.switchTab({
        url: "/pages/appointment/appointment"
      });
    };
    const goToMessages = () => {
      if (!userStore.value.checkLoginStatus("/pages/message/message")) {
        return;
      }
      common_vendor.index.switchTab({
        url: "/pages/message/message"
      });
    };
    const goToSettings = () => {
      common_vendor.index.showToast({
        title: "功能开发中",
        icon: "none"
      });
    };
    const goToHelp = () => {
      common_vendor.index.showToast({
        title: "功能开发中",
        icon: "none"
      });
    };
    const handleLogout = () => {
      common_vendor.index.showModal({
        title: "提示",
        content: "确定要退出登录吗？",
        success: (res) => {
          if (res.confirm) {
            userStore.value.logout();
          }
        }
      });
    };
    const fetchUserDetail = async () => {
      if (userStore.value && userStore.value.isLoggedIn && userStore.value.userInfo && userStore.value.userInfo.userId) {
        try {
          const userDetailResponse = await api_user.getUserById(userStore.value.userInfo.userId);
          if (userDetailResponse.data) {
            const updatedUserInfo = {
              ...userStore.value.userInfo,
              name: userDetailResponse.data.name,
              username: userDetailResponse.data.username
            };
            userStore.value.setUserInfo(updatedUserInfo);
          }
        } catch (error) {
          common_vendor.index.__f__("error", "at pages/profile/profile.vue:202", "获取用户详细信息失败:", error);
        }
      }
    };
    common_vendor.onMounted(() => {
      userStore.value = stores_user.useUserStore();
      userStore.value.restoreFromStorage();
      fetchUserDetail();
    });
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: userStore.value && userStore.value.isLoggedIn
      }, userStore.value && userStore.value.isLoggedIn ? {
        b: userStore.value.avatar || "/static/user-placeholder.png",
        c: common_vendor.t(userStore.value.displayName),
        d: common_vendor.t(userStore.value.userInfo && userStore.value.userInfo.gender === 1 ? "先生" : userStore.value.userInfo && userStore.value.userInfo.gender === 2 ? "女士" : ""),
        e: common_vendor.o(editProfile)
      } : {
        f: common_assets._imports_0$2,
        g: common_vendor.o(goToLogin)
      }, {
        h: common_assets._imports_1$1,
        i: common_vendor.o(goToOrders),
        j: common_assets._imports_2,
        k: common_vendor.o(goToAppointments),
        l: common_assets._imports_3,
        m: common_vendor.o(goToMessages),
        n: common_assets._imports_4,
        o: common_vendor.o(goToSettings),
        p: common_assets._imports_5,
        q: common_vendor.o(goToHelp),
        r: userStore.value && userStore.value.isLoggedIn
      }, userStore.value && userStore.value.isLoggedIn ? {
        s: common_assets._imports_6,
        t: common_vendor.o(handleLogout)
      } : {});
    };
  }
};
wx.createPage(_sfc_main);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/profile/profile.js.map

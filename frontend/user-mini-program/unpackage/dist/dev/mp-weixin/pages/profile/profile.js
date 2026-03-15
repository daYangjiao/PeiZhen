"use strict";
const common_vendor = require("../../common/vendor.js");
const common_assets = require("../../common/assets.js");
const stores_user = require("../../stores/user.js");
const utils_api = require("../../utils/api.js");
const api_user = require("../../api/user.js");
const PLACEHOLDER_AVATAR = "/static/user-placeholder.png";
const _sfc_main = {
  __name: "profile",
  setup(__props) {
    const userStore = stores_user.useUserStore();
    const avatarLoadFailed = common_vendor.ref(false);
    const getFullAvatarUrl = (relativePath) => {
      if (!relativePath || typeof relativePath !== "string")
        return PLACEHOLDER_AVATAR;
      const path = String(relativePath).trim();
      if (!path)
        return PLACEHOLDER_AVATAR;
      if (path.startsWith("http"))
        return path;
      const baseUrl = utils_api.config.baseURL.endsWith("/") ? utils_api.config.baseURL : utils_api.config.baseURL + "/";
      const normalized = path.startsWith("/") ? path.substring(1) : path;
      return baseUrl + normalized;
    };
    const avatarDisplayUrl = common_vendor.computed(() => {
      var _a;
      if (avatarLoadFailed.value)
        return PLACEHOLDER_AVATAR;
      const avatar = userStore.avatar || ((_a = userStore.userInfo) == null ? void 0 : _a.avatar) || "";
      return getFullAvatarUrl(avatar);
    });
    const onAvatarError = () => {
      avatarLoadFailed.value = true;
    };
    common_vendor.watch(() => userStore.avatar, () => {
      avatarLoadFailed.value = false;
    });
    const goToLogin = () => {
      common_vendor.index.navigateTo({
        url: "/subpkg/auth/login?redirect=" + encodeURIComponent("/pages/profile/profile")
      });
    };
    const editProfile = () => {
      common_vendor.index.__f__("log", "at pages/profile/profile.vue:149", "🚀🚀🚀 点击了编辑按钮，准备跳转到编辑页面 🚀🚀🚀");
      common_vendor.index.navigateTo({
        url: "/subpkg/profile/edit-profile"
      });
    };
    const goToOrders = () => {
      if (!userStore.checkLoginStatus("/pages/order/order")) {
        return;
      }
      common_vendor.index.switchTab({
        url: "/pages/order/order"
      });
    };
    const goToAppointments = () => {
      if (!userStore.checkLoginStatus("/pages/appointment/appointment")) {
        return;
      }
      common_vendor.index.switchTab({
        url: "/pages/appointment/appointment"
      });
    };
    const goToMessages = () => {
      if (!userStore.checkLoginStatus("/pages/message/message")) {
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
            userStore.logout();
          }
        }
      });
    };
    const fetchUserDetail = async () => {
      var _a, _b;
      const uid = ((_a = userStore.userInfo) == null ? void 0 : _a.id) ?? ((_b = userStore.userInfo) == null ? void 0 : _b.userId);
      if (!userStore.isLoggedIn || !uid)
        return;
      try {
        const userDetailResponse = await api_user.getUserById(uid);
        if (userDetailResponse.data) {
          const updatedUserInfo = {
            ...userStore.userInfo,
            name: userDetailResponse.data.name,
            username: userDetailResponse.data.username,
            avatar: userDetailResponse.data.avatar
          };
          userStore.setUserInfo(updatedUserInfo);
        }
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/profile/profile.vue:230", "获取用户详细信息失败:", error);
      }
    };
    common_vendor.onMounted(() => {
      userStore.restoreFromStorage();
      fetchUserDetail();
    });
    common_vendor.onShow(() => {
      userStore.restoreFromStorage();
      avatarLoadFailed.value = false;
      fetchUserDetail();
    });
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_vendor.unref(userStore).isLoggedIn
      }, common_vendor.unref(userStore).isLoggedIn ? {
        b: avatarDisplayUrl.value,
        c: common_vendor.o(onAvatarError),
        d: common_vendor.t(common_vendor.unref(userStore).displayName),
        e: common_vendor.t(common_vendor.unref(userStore).userInfo && common_vendor.unref(userStore).userInfo.gender === 1 ? "先生" : common_vendor.unref(userStore).userInfo && common_vendor.unref(userStore).userInfo.gender === 2 ? "女士" : ""),
        f: common_vendor.o(editProfile)
      } : {
        g: common_assets._imports_0$3,
        h: common_vendor.o(goToLogin)
      }, {
        i: common_assets._imports_1$1,
        j: common_vendor.o(goToOrders),
        k: common_assets._imports_2$2,
        l: common_vendor.o(goToAppointments),
        m: common_assets._imports_3$1,
        n: common_vendor.o(goToMessages),
        o: common_assets._imports_4,
        p: common_vendor.o(goToSettings),
        q: common_assets._imports_5,
        r: common_vendor.o(goToHelp),
        s: common_vendor.unref(userStore).isLoggedIn
      }, common_vendor.unref(userStore).isLoggedIn ? {
        t: common_assets._imports_6,
        v: common_vendor.o(handleLogout)
      } : {});
    };
  }
};
wx.createPage(_sfc_main);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/profile/profile.js.map

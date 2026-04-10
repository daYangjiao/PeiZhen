"use strict";
const common_vendor = require("../common/vendor.js");
const useUserStore = common_vendor.defineStore("user", {
  state: () => ({
    // 用户信息
    userInfo: null,
    // 是否已登录
    isLoggedIn: false,
    // 是否为游客模式
    isGuestMode: false,
    // 登录时间戳
    loginTime: null
  }),
  getters: {
    // 获取用户显示名称
    displayName: (state) => {
      if (!state.userInfo)
        return "";
      return state.userInfo.nickName || state.userInfo.name || "用户";
    },
    // 获取用户头像
    avatar: (state) => {
      if (!state.userInfo)
        return "";
      return state.userInfo.avatarUrl || state.userInfo.avatar || "";
    },
    // 检查是否需要更新用户信息（超过24小时）
    needUpdateUserInfo: (state) => {
      if (!state.loginTime)
        return true;
      const now = Date.now();
      const dayInMs = 24 * 60 * 60 * 1e3;
      return now - state.loginTime > dayInMs;
    }
  },
  actions: {
    // 设置用户信息
    setUserInfo(userInfo) {
      common_vendor.index.__f__("log", "at stores/user.js:41", "保存前的用户信息:", JSON.stringify(userInfo));
      this.userInfo = userInfo;
      this.isLoggedIn = true;
      this.isGuestMode = false;
      this.loginTime = Date.now();
      if (!this.userInfo.nickName && this.userInfo.username) {
        this.userInfo.nickName = this.userInfo.username;
        common_vendor.index.__f__("log", "at stores/user.js:50", "在store中设置nickName:", this.userInfo.nickName);
      }
      common_vendor.index.setStorageSync("userInfo", this.userInfo);
      common_vendor.index.setStorageSync("isLoggedIn", true);
      common_vendor.index.setStorageSync("loginTime", this.loginTime);
      common_vendor.index.removeStorageSync("isGuestMode");
      common_vendor.index.__f__("log", "at stores/user.js:59", "保存后的用户信息:", JSON.stringify(this.userInfo));
      common_vendor.index.__f__("log", "at stores/user.js:60", "displayName:", this.displayName);
    },
    // 设置游客模式
    setGuestMode() {
      this.isGuestMode = true;
      this.isLoggedIn = false;
      this.userInfo = null;
      this.loginTime = null;
      common_vendor.index.setStorageSync("isGuestMode", true);
      common_vendor.index.removeStorageSync("userInfo");
      common_vendor.index.removeStorageSync("isLoggedIn");
      common_vendor.index.removeStorageSync("loginTime");
    },
    // 清除用户信息（退出登录）
    clearUserInfo() {
      this.userInfo = null;
      this.isLoggedIn = false;
      this.isGuestMode = false;
      this.loginTime = null;
      common_vendor.index.removeStorageSync("userInfo");
      common_vendor.index.removeStorageSync("isLoggedIn");
      common_vendor.index.removeStorageSync("isGuestMode");
      common_vendor.index.removeStorageSync("loginTime");
    },
    // 从本地存储恢复状态
    restoreFromStorage() {
      try {
        const userInfo = common_vendor.index.getStorageSync("userInfo");
        const isLoggedIn = common_vendor.index.getStorageSync("isLoggedIn");
        const isGuestMode = common_vendor.index.getStorageSync("isGuestMode");
        const loginTime = common_vendor.index.getStorageSync("loginTime");
        common_vendor.index.__f__("log", "at stores/user.js:99", "从存储恢复的用户信息:", JSON.stringify(userInfo));
        if (userInfo && isLoggedIn) {
          this.userInfo = userInfo;
          if (!this.userInfo.nickName && this.userInfo.username) {
            this.userInfo.nickName = this.userInfo.username;
            common_vendor.index.__f__("log", "at stores/user.js:107", "恢复时设置nickName:", this.userInfo.nickName);
            common_vendor.index.setStorageSync("userInfo", this.userInfo);
          }
          this.isLoggedIn = true;
          this.loginTime = loginTime || Date.now();
          common_vendor.index.__f__("log", "at stores/user.js:114", "恢复后的displayName:", this.displayName);
        } else if (isGuestMode) {
          this.isGuestMode = true;
        }
      } catch (error) {
        common_vendor.index.__f__("error", "at stores/user.js:119", "恢复用户状态失败:", error);
      }
    },
    // 微信登录
    async wxLogin() {
      try {
        const loginRes = await common_vendor.index.login({
          provider: "weixin"
        });
        if (!loginRes[1] || !loginRes[1].code) {
          throw new Error("获取登录凭证失败");
        }
        const mockUserInfo = {
          openid: "mock_openid_" + Date.now(),
          nickName: "微信用户",
          avatarUrl: "/static/logo.png"
        };
        this.setUserInfo(mockUserInfo);
        common_vendor.index.showToast({
          title: "登录成功",
          icon: "success"
        });
        return true;
      } catch (error) {
        common_vendor.index.__f__("error", "at stores/user.js:152", "微信登录失败:", error);
        common_vendor.index.showToast({
          title: "登录失败，请重试",
          icon: "none"
        });
        return false;
      }
    },
    // 获取用户详细信息
    async getUserProfile() {
      try {
        const profileRes = await common_vendor.index.getUserProfile({
          desc: "用于完善用户资料"
        });
        if (profileRes[1] && profileRes[1].userInfo) {
          const userInfo = {
            ...this.userInfo,
            ...profileRes[1].userInfo
          };
          this.setUserInfo(userInfo);
          return userInfo;
        }
        return null;
      } catch (error) {
        common_vendor.index.__f__("error", "at stores/user.js:179", "获取用户信息失败:", error);
        return null;
      }
    },
    // 检查登录状态并跳转
    checkLoginStatus(redirectUrl = "/pages/index/index") {
      if (!this.isLoggedIn && !this.isGuestMode) {
        common_vendor.index.navigateTo({
          url: "/subpkg/auth/login?redirect=" + encodeURIComponent(redirectUrl)
        });
        return false;
      }
      return true;
    },
    // 退出登录
    logout() {
      this.clearUserInfo();
      common_vendor.index.showToast({
        title: "已退出登录",
        icon: "success"
      });
      common_vendor.index.reLaunch({
        url: "/subpkg/auth/login"
      });
    }
  }
});
exports.useUserStore = useUserStore;
//# sourceMappingURL=../../.sourcemap/mp-weixin/stores/user.js.map

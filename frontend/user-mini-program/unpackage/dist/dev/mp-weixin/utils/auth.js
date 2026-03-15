"use strict";
const common_vendor = require("../common/vendor.js");
const stores_user = require("../stores/user.js");
const STORAGE_KEYS = {
  USER_INFO: "userInfo",
  IS_LOGIN: "isLogin",
  IS_GUEST: "isGuest",
  TOKEN: "token"
};
function getUserStore() {
  try {
    return stores_user.useUserStore();
  } catch (error) {
    common_vendor.index.__f__("warn", "at utils/auth.js:24", "无法获取用户store，使用本地存储作为备用方案");
    return null;
  }
}
function getUserInfo() {
  const userStore = getUserStore();
  if (userStore) {
    return userStore.userInfo;
  }
  try {
    const userInfo = common_vendor.index.getStorageSync(STORAGE_KEYS.USER_INFO);
    return userInfo || null;
  } catch (error) {
    common_vendor.index.__f__("error", "at utils/auth.js:74", "获取用户信息失败:", error);
    return null;
  }
}
function setUserInfo(userInfo) {
  const userStore = getUserStore();
  if (userStore) {
    userStore.setUserInfo(userInfo);
    return;
  }
  try {
    common_vendor.index.setStorageSync(STORAGE_KEYS.USER_INFO, userInfo);
    common_vendor.index.setStorageSync(STORAGE_KEYS.IS_LOGIN, true);
    common_vendor.index.removeStorageSync(STORAGE_KEYS.IS_GUEST);
  } catch (error) {
    common_vendor.index.__f__("error", "at utils/auth.js:97", "保存用户信息失败:", error);
  }
}
function clearUserInfo() {
  const userStore = getUserStore();
  if (userStore) {
    userStore.clearUserInfo();
    return;
  }
  try {
    common_vendor.index.removeStorageSync(STORAGE_KEYS.USER_INFO);
    common_vendor.index.removeStorageSync(STORAGE_KEYS.IS_LOGIN);
    common_vendor.index.removeStorageSync(STORAGE_KEYS.IS_GUEST);
    common_vendor.index.removeStorageSync(STORAGE_KEYS.TOKEN);
  } catch (error) {
    common_vendor.index.__f__("error", "at utils/auth.js:118", "清除用户信息失败:", error);
  }
}
function navigateToLogin() {
  common_vendor.index.navigateTo({
    url: "/subpkg/auth/login"
  });
}
exports.clearUserInfo = clearUserInfo;
exports.getUserInfo = getUserInfo;
exports.navigateToLogin = navigateToLogin;
exports.setUserInfo = setUserInfo;
//# sourceMappingURL=../../.sourcemap/mp-weixin/utils/auth.js.map

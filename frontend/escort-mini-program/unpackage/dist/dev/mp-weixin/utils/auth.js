"use strict";
const common_vendor = require("../common/vendor.js");
const STORAGE_KEYS = {
  USER_INFO: "userInfo",
  IS_LOGIN: "isLogin",
  IS_GUEST: "isGuest",
  TOKEN: "token"
};
function clearUserInfo() {
  try {
    common_vendor.index.removeStorageSync(STORAGE_KEYS.USER_INFO);
    common_vendor.index.removeStorageSync(STORAGE_KEYS.IS_LOGIN);
    common_vendor.index.removeStorageSync(STORAGE_KEYS.IS_GUEST);
    common_vendor.index.removeStorageSync(STORAGE_KEYS.TOKEN);
  } catch (error) {
    common_vendor.index.__f__("error", "at utils/auth.js:67", "清除用户信息失败:", error);
  }
}
function navigateToLogin() {
  common_vendor.index.navigateTo({
    url: "/subpkg/auth/login"
  });
}
exports.clearUserInfo = clearUserInfo;
exports.navigateToLogin = navigateToLogin;
//# sourceMappingURL=../../.sourcemap/mp-weixin/utils/auth.js.map

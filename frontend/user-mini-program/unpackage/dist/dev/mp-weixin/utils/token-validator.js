"use strict";
const common_vendor = require("../common/vendor.js");
const utils_auth = require("./auth.js");
const stores_user = require("../stores/user.js");
const utils_api = require("./api.js");
async function validateToken(token) {
  if (!token) {
    return false;
  }
  try {
    const response = await utils_api.get("/api/users/current");
    return response && response.code === 200;
  } catch (error) {
    common_vendor.index.__f__("log", "at utils/token-validator.js:25", "Token验证失败:", error.message);
    if (error.statusCode === 401) {
      return false;
    }
    return true;
  }
}
function isTokenExpiringSoon(token) {
  if (!token)
    return true;
  try {
    const payload = JSON.parse(atob(token.split(".")[1]));
    const exp = payload.exp;
    const now = Math.floor(Date.now() / 1e3);
    return exp - now < 3600;
  } catch (error) {
    common_vendor.index.__f__("error", "at utils/token-validator.js:52", "解析token失败:", error);
    return true;
  }
}
async function startupTokenCheck() {
  common_vendor.index.__f__("log", "at utils/token-validator.js:62", "开始启动时token检测...");
  const userStore = stores_user.useUserStore();
  let token = null;
  let isLoggedIn = false;
  if (userStore) {
    isLoggedIn = userStore.isLoggedIn;
    const userInfo = userStore.userInfo;
    if (userInfo && userInfo.token) {
      token = userInfo.token;
    }
  }
  if (!token) {
    token = common_vendor.index.getStorageSync("token");
    isLoggedIn = !!common_vendor.index.getStorageSync("isLogin");
  }
  common_vendor.index.__f__("log", "at utils/token-validator.js:83", "当前登录状态:", isLoggedIn);
  common_vendor.index.__f__("log", "at utils/token-validator.js:84", "Token存在:", !!token);
  if (!isLoggedIn) {
    common_vendor.index.__f__("log", "at utils/token-validator.js:88", "用户未登录，跳过token检测");
    return;
  }
  if (!token) {
    common_vendor.index.__f__("log", "at utils/token-validator.js:94", "检测到登录状态但无token，清理登录状态");
    if (userStore) {
      userStore.clearUserInfo();
    } else {
      utils_auth.clearUserInfo();
    }
    return;
  }
  if (isTokenExpiringSoon(token)) {
    common_vendor.index.__f__("log", "at utils/token-validator.js:105", "Token即将过期，建议刷新");
  }
  const isValid = await validateToken(token);
  common_vendor.index.__f__("log", "at utils/token-validator.js:111", "Token有效性验证结果:", isValid);
  if (!isValid) {
    common_vendor.index.__f__("log", "at utils/token-validator.js:114", "Token已失效，执行自动登出");
    if (userStore) {
      userStore.clearUserInfo();
    } else {
      utils_auth.clearUserInfo();
    }
    common_vendor.index.showToast({
      title: "登录已过期，请重新登录",
      icon: "none",
      duration: 2e3
    });
    setTimeout(() => {
      utils_auth.navigateToLogin();
    }, 2e3);
  } else {
    common_vendor.index.__f__("log", "at utils/token-validator.js:135", "Token验证通过，用户已登录");
  }
}
function startPeriodicTokenCheck(interval = 30 * 60 * 1e3) {
  if (window.tokenCheckTimer) {
    clearInterval(window.tokenCheckTimer);
  }
  window.tokenCheckTimer = setInterval(async () => {
    const userStore = stores_user.useUserStore();
    if (userStore && userStore.isLoggedIn) {
      const userInfo = userStore.userInfo;
      const token = userInfo == null ? void 0 : userInfo.token;
      if (token && !await validateToken(token)) {
        common_vendor.index.__f__("log", "at utils/token-validator.js:156", "定期检查发现token失效");
        if (userStore) {
          userStore.clearUserInfo();
        } else {
          utils_auth.clearUserInfo();
        }
        common_vendor.index.showToast({
          title: "登录已过期，请重新登录",
          icon: "none"
        });
        utils_auth.navigateToLogin();
      }
    }
  }, interval);
}
exports.startPeriodicTokenCheck = startPeriodicTokenCheck;
exports.startupTokenCheck = startupTokenCheck;
//# sourceMappingURL=../../.sourcemap/mp-weixin/utils/token-validator.js.map

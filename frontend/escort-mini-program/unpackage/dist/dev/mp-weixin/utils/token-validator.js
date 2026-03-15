"use strict";
const common_vendor = require("../common/vendor.js");
const utils_auth = require("./auth.js");
const utils_api = require("./api.js");
async function validateToken(token) {
  if (!token) {
    return false;
  }
  try {
    const response = await utils_api.get("/api/users/current");
    return response && response.code === 200;
  } catch (error) {
    common_vendor.index.__f__("log", "at utils/token-validator.js:24", "Token验证失败:", error.message);
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
    common_vendor.index.__f__("error", "at utils/token-validator.js:51", "解析token失败:", error);
    return true;
  }
}
async function startupTokenCheck() {
  common_vendor.index.__f__("log", "at utils/token-validator.js:61", "开始启动时token检测...");
  let token = null;
  let isLoggedIn = false;
  token = common_vendor.index.getStorageSync("token");
  isLoggedIn = !!common_vendor.index.getStorageSync("isLogin");
  common_vendor.index.__f__("log", "at utils/token-validator.js:70", "当前登录状态:", isLoggedIn);
  common_vendor.index.__f__("log", "at utils/token-validator.js:71", "Token存在:", !!token);
  if (!isLoggedIn) {
    common_vendor.index.__f__("log", "at utils/token-validator.js:75", "陪诊师未登录，跳过token检测");
    return;
  }
  if (!token) {
    common_vendor.index.__f__("log", "at utils/token-validator.js:81", "检测到登录状态但无token，清理登录状态");
    utils_auth.clearUserInfo();
    return;
  }
  if (isTokenExpiringSoon(token)) {
    common_vendor.index.__f__("log", "at utils/token-validator.js:88", "Token即将过期，建议刷新");
  }
  const isValid = await validateToken(token);
  common_vendor.index.__f__("log", "at utils/token-validator.js:94", "Token有效性验证结果:", isValid);
  if (!isValid) {
    common_vendor.index.__f__("log", "at utils/token-validator.js:97", "Token已失效，执行自动登出");
    utils_auth.clearUserInfo();
    common_vendor.index.showToast({
      title: "登录已过期，请重新登录",
      icon: "none",
      duration: 2e3
    });
    setTimeout(() => {
      utils_auth.navigateToLogin();
    }, 2e3);
  } else {
    common_vendor.index.__f__("log", "at utils/token-validator.js:114", "Token验证通过，陪诊师已登录");
  }
}
function startPeriodicTokenCheck(interval = 30 * 60 * 1e3) {
  if (window.tokenCheckTimer) {
    clearInterval(window.tokenCheckTimer);
  }
  window.tokenCheckTimer = setInterval(async () => {
    const token = common_vendor.index.getStorageSync("token");
    const isLoggedIn = !!common_vendor.index.getStorageSync("isLogin");
    if (isLoggedIn && token && !await validateToken(token)) {
      common_vendor.index.__f__("log", "at utils/token-validator.js:133", "定期检查发现token失效");
      utils_auth.clearUserInfo();
      common_vendor.index.showToast({
        title: "登录已过期，请重新登录",
        icon: "none"
      });
      utils_auth.navigateToLogin();
    }
  }, interval);
}
exports.startPeriodicTokenCheck = startPeriodicTokenCheck;
exports.startupTokenCheck = startupTokenCheck;
//# sourceMappingURL=../../.sourcemap/mp-weixin/utils/token-validator.js.map

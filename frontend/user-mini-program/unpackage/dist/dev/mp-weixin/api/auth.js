"use strict";
const common_vendor = require("../common/vendor.js");
const utils_api = require("../utils/api.js");
const utils_auth = require("../utils/auth.js");
const login = (data) => {
  common_vendor.index.__f__("log", "at api/auth.js:7", "开始登录请求，用户名:", data.username);
  return utils_api.post("/api/users/login", {
    username: data.username,
    password: data.password
  }).then((response) => {
    common_vendor.index.__f__("log", "at api/auth.js:13", "登录接口原始响应:", JSON.stringify(response));
    if (!response) {
      throw new Error("服务器无响应");
    }
    if (response.data && response.data.token) {
      const backendUserInfo = response.data.userInfo || {};
      const userInfo = {
        // 基本信息
        id: backendUserInfo.id || response.data.id,
        userId: backendUserInfo.id || response.data.id,
        username: backendUserInfo.username || data.username,
        userType: backendUserInfo.userType !== void 0 ? backendUserInfo.userType : 0,
        token: response.data.token,
        // 昵称处理 - 确保有显示名称
        nickName: backendUserInfo.username || data.username || "用户",
        name: backendUserInfo.username || data.username,
        // 时间戳
        loginTime: Date.now()
      };
      common_vendor.index.__f__("log", "at api/auth.js:41", "登录成功，构造的用户信息:", JSON.stringify(userInfo));
      utils_auth.setUserInfo(userInfo);
      return {
        ...response,
        data: {
          ...response.data,
          userInfo
        }
      };
    } else {
      let errorMsg = "登录失败";
      if (response.message) {
        errorMsg = response.message;
      } else if (response.data && response.data.message) {
        errorMsg = response.data.message;
      } else if (response.code === 401) {
        errorMsg = "用户名或密码错误";
      }
      common_vendor.index.__f__("log", "at api/auth.js:65", "登录失败，错误信息:", errorMsg);
      throw new Error(errorMsg);
    }
  }).catch((error) => {
    common_vendor.index.__f__("error", "at api/auth.js:69", "登录请求捕获到错误:", error);
    if (error instanceof Error) {
      throw error;
    }
    let errorMsg = "登录失败";
    if (error.message) {
      errorMsg = error.message;
    } else if (error.statusCode === 401) {
      errorMsg = "用户名或密码错误";
    } else if (error.statusCode >= 500) {
      errorMsg = "服务器错误";
    } else if (error.statusCode) {
      errorMsg = `请求失败 (${error.statusCode})`;
    }
    throw new Error(errorMsg);
  });
};
const register = (data) => {
  return utils_api.post("/api/users/register", {
    username: data.username,
    password: data.password,
    name: data.name,
    openid: data.openid || ""
    // 添加openid字段，后端要求不能为null
  });
};
exports.login = login;
exports.register = register;
//# sourceMappingURL=../../.sourcemap/mp-weixin/api/auth.js.map

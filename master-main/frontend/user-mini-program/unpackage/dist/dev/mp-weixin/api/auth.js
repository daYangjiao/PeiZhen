"use strict";
const common_vendor = require("../common/vendor.js");
const utils_api = require("../utils/api.js");
const utils_auth = require("../utils/auth.js");
const login = (data) => {
  return utils_api.post("/api/users/login", {
    username: data.username,
    password: data.password
  }).then((response) => {
    if (response.data.token) {
      const userInfo = {
        // 优先使用后端直接返回的字段
        id: response.data.id || response.data.userId,
        userId: response.data.id || response.data.userId,
        username: response.data.username,
        userType: response.data.userType,
        token: response.data.token,
        // 如果有userInfo对象，则合并其字段
        ...response.data.userInfo || {},
        // 确保设置昵称
        nickName: response.data.username || response.data.userInfo && response.data.userInfo.nickName || "用户"
      };
      common_vendor.index.__f__("log", "at api/auth.js:25", "登录成功，构造的用户信息:", JSON.stringify(userInfo));
      utils_auth.setUserInfo(userInfo);
    }
    return response;
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

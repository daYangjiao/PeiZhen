"use strict";
const common_vendor = require("../common/vendor.js");
const utils_api = require("../utils/api.js");
const utils_auth = require("../utils/auth.js");
const stores_user = require("../stores/user.js");
const getUserInfo = (userId) => {
  return utils_api.get(`/api/users/${userId}`);
};
const updateUserInfo = (data) => {
  const userId = data.userId || data.id || data.yonghuid || (stores_user.useUserStore().userInfo ? stores_user.useUserStore().userInfo.userId || stores_user.useUserStore().userInfo.id || stores_user.useUserStore().userInfo.yonghuid : null);
  if (!userId) {
    return Promise.reject(new Error("用户ID不存在"));
  }
  common_vendor.index.__f__("log", "at api/user.js:30", "更新用户信息，用户ID:", userId, "请求数据:", JSON.stringify(data));
  const requestData = {
    // 确保请求体中包含id字段
    id: userId,
    yonghuid: userId
  };
  if (data.name !== void 0)
    requestData.name = data.name;
  if (data.username !== void 0)
    requestData.username = data.username;
  if (data.sex !== void 0)
    requestData.sex = data.sex;
  if (data.age !== void 0)
    requestData.age = data.age;
  if (data.phone !== void 0)
    requestData.phone = data.phone;
  if (data.password !== void 0)
    requestData.password = data.password;
  if (data.nickname !== void 0)
    requestData.nickname = data.nickname;
  if (data.gender !== void 0)
    requestData.gender = data.gender;
  if (data.birthday !== void 0)
    requestData.birthday = data.birthday;
  if (data.email !== void 0)
    requestData.email = data.email;
  if (data.address !== void 0)
    requestData.address = data.address;
  if (data.emergencyContact !== void 0)
    requestData.emergencyContact = data.emergencyContact;
  if (data.medicalHistory !== void 0)
    requestData.medicalHistory = data.medicalHistory;
  if (data.allergies !== void 0)
    requestData.allergies = data.allergies;
  if (data.currentMedications !== void 0)
    requestData.currentMedications = data.currentMedications;
  if (data.bio !== void 0)
    requestData.bio = data.bio;
  return utils_api.put(`/api/users/${userId}`, requestData).then((response) => {
    if (response.data) {
      utils_auth.setUserInfo(response.data);
    }
    return response;
  });
};
const uploadAvatar = (filePath) => {
  return utils_api.upload("/customer/user/avatar", filePath).then((response) => {
    var _a;
    if ((_a = response.data) == null ? void 0 : _a.avatarUrl) {
      const userInfo = getUserInfo();
      if (userInfo) {
        utils_auth.setUserInfo({
          ...userInfo,
          avatar: response.data.avatarUrl
        });
      }
    }
    return response;
  });
};
exports.getUserInfo = getUserInfo;
exports.updateUserInfo = updateUserInfo;
exports.uploadAvatar = uploadAvatar;
//# sourceMappingURL=../../.sourcemap/mp-weixin/api/user.js.map

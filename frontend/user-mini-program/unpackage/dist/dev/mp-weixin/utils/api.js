"use strict";
const common_vendor = require("../common/vendor.js");
const utils_auth = require("./auth.js");
const config = {
  // 开发环境API地址
  baseURL: "http://localhost:8080",
  // 请求超时时间
  timeout: 1e4,
  // 请求头配置
  headers: {
    "Content-Type": "application/json",
    "User-Type": "customer"
  }
};
const getToken = () => {
  const userInfo = utils_auth.getUserInfo();
  return (userInfo == null ? void 0 : userInfo.token) || common_vendor.index.getStorageSync("token") || "";
};
const clearToken = () => {
  common_vendor.index.removeStorageSync("token");
};
const forceLogout = () => {
  common_vendor.index.__f__("log", "at utils/api.js:43", "执行强制登出逻辑");
  utils_auth.clearUserInfo();
  clearToken();
  common_vendor.index.showToast({
    title: "登录已过期，请重新登录",
    icon: "none",
    duration: 2e3
  });
  setTimeout(() => {
    common_vendor.index.reLaunch({
      url: "/subpkg/auth/login"
    });
  }, 1500);
};
const requestInterceptor = (options) => {
  const token = getToken();
  const headers = {
    "Content-Type": "application/json",
    "User-Type": "customer"
  };
  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }
  options.url = config.baseURL + options.url;
  options.timeout = config.timeout;
  options.header = headers;
  return options;
};
const responseInterceptor = (response) => {
  const { statusCode, data } = response;
  common_vendor.index.__f__("log", "at utils/api.js:82", "响应拦截器 - 状态码:", statusCode, "数据:", JSON.stringify(data));
  if (statusCode === 401) {
    common_vendor.index.__f__("log", "at utils/api.js:86", "检测到 HTTP 401，准备强制登出");
    forceLogout();
    return Promise.reject(data);
  }
  if (data && data.code === 401) {
    common_vendor.index.__f__("log", "at utils/api.js:93", "检测到业务 401，准备强制登出");
    forceLogout();
    return Promise.reject(data);
  }
  if (statusCode >= 200 && statusCode < 300) {
    if (data.code !== void 0) {
      if (data.code === 200) {
        return Promise.resolve(data);
      } else {
        common_vendor.index.showToast({ title: data.message || "请求失败", icon: "none" });
        return Promise.reject(data);
      }
    }
    return Promise.resolve({ code: 200, data });
  } else {
    common_vendor.index.showToast({ title: "网络请求失败", icon: "none" });
    return Promise.reject(response);
  }
};
const request = (options) => {
  return new Promise((resolve, reject) => {
    const interceptedOptions = requestInterceptor(options);
    common_vendor.index.request({
      ...interceptedOptions,
      success: (response) => {
        responseInterceptor(response).then(resolve).catch(reject);
      },
      fail: (error) => {
        common_vendor.index.showToast({ title: "网络连接失败", icon: "none" });
        reject(error);
      }
    });
  });
};
const get = (url, params = {}) => request({ url, method: "GET", data: params });
const post = (url, data = {}) => request({ url, method: "POST", data });
const put = (url, data = {}) => request({ url, method: "PUT", data });
const getBackendImageUrl = (imageName) => {
  const baseUrl = config.baseURL.endsWith("/") ? config.baseURL.slice(0, -1) : config.baseURL;
  return `${baseUrl}/uploads/frontend-images/${imageName}`;
};
const upload = (url, filePath, formData = {}, name = "file") => {
  return new Promise((resolve, reject) => {
    const token = getToken();
    const headers = {
      "Authorization": token ? `Bearer ${token}` : ""
    };
    common_vendor.index.uploadFile({
      url: config.baseURL + url,
      filePath,
      name,
      formData,
      header: headers,
      success: (response) => {
        try {
          const data = typeof response.data === "string" ? JSON.parse(response.data) : response.data;
          if (data.code === 200 || data.code === 0) {
            resolve(data);
          } else {
            common_vendor.index.showToast({ title: data.message || "上传失败", icon: "none" });
            reject(data);
          }
        } catch (e) {
          common_vendor.index.__f__("error", "at utils/api.js:169", "上传响应解析失败:", e, response.data);
          common_vendor.index.showToast({ title: "上传失败", icon: "none" });
          reject(response);
        }
      },
      fail: (error) => {
        common_vendor.index.__f__("error", "at utils/api.js:175", "文件上传失败:", error);
        common_vendor.index.showToast({ title: "上传失败", icon: "none" });
        reject(error);
      }
    });
  });
};
exports.config = config;
exports.get = get;
exports.getBackendImageUrl = getBackendImageUrl;
exports.getToken = getToken;
exports.post = post;
exports.put = put;
exports.upload = upload;
//# sourceMappingURL=../../.sourcemap/mp-weixin/utils/api.js.map

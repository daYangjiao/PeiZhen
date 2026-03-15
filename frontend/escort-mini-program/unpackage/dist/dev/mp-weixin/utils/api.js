"use strict";
const common_vendor = require("../common/vendor.js");
const config = {
  baseURL: "http://localhost:8080",
  timeout: 1e4,
  headers: {
    "Content-Type": "application/json"
  }
};
const getToken = () => {
  return common_vendor.index.getStorageSync("token") || "";
};
const setToken = (token) => {
  common_vendor.index.setStorageSync("token", token);
};
const clearToken = () => {
  common_vendor.index.removeStorageSync("token");
};
const forceLogout = () => {
  common_vendor.index.__f__("log", "at utils/api.js:29", "执行强制登出逻辑");
  common_vendor.index.removeStorageSync("userInfo");
  common_vendor.index.removeStorageSync("isLoggedIn");
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
    ...config.headers,
    ...options.header
  };
  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }
  if (!options.url.startsWith("http")) {
    options.url = config.baseURL + options.url;
  }
  options.timeout = config.timeout;
  options.header = headers;
  common_vendor.index.__f__("log", "at utils/api.js:66", "🚀 请求发送:", options.method, options.url);
  return options;
};
const responseInterceptor = (response) => {
  const { statusCode, data } = response;
  common_vendor.index.__f__("log", "at utils/api.js:73", "✅ 响应接收:", statusCode, data);
  if (statusCode === 401) {
    common_vendor.index.__f__("log", "at utils/api.js:77", "检测到 HTTP 401，准备强制登出");
    forceLogout();
    return Promise.reject(data);
  }
  if (data && data.code === 401) {
    common_vendor.index.__f__("log", "at utils/api.js:84", "检测到业务 401，准备强制登出");
    forceLogout();
    return Promise.reject(data);
  }
  if (statusCode >= 200 && statusCode < 300) {
    if (data.code !== void 0) {
      if (data.code === 200 || data.code === 0) {
        return Promise.resolve(data);
      } else {
        common_vendor.index.showToast({ title: data.message || data.msg || "请求失败", icon: "none" });
        return Promise.reject(data);
      }
    }
    return Promise.resolve({ code: 200, data });
  } else {
    common_vendor.index.showToast({ title: "网络请求失败(" + statusCode + ")", icon: "none" });
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
        common_vendor.index.__f__("error", "at utils/api.js:116", "❌ 请求失败:", error);
        common_vendor.index.showToast({ title: "无法连接到服务器", icon: "none" });
        reject(error);
      }
    });
  });
};
const get = (url, params = {}) => request({ url, method: "GET", data: params });
const post = (url, data = {}) => request({ url, method: "POST", data });
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
          common_vendor.index.__f__("error", "at utils/api.js:152", "上传响应解析失败:", e, response.data);
          common_vendor.index.showToast({ title: "上传失败", icon: "none" });
          reject(response);
        }
      },
      fail: (error) => {
        common_vendor.index.__f__("error", "at utils/api.js:158", "文件上传失败:", error);
        common_vendor.index.showToast({ title: "上传失败", icon: "none" });
        reject(error);
      }
    });
  });
};
exports.clearToken = clearToken;
exports.config = config;
exports.get = get;
exports.getToken = getToken;
exports.post = post;
exports.setToken = setToken;
exports.upload = upload;
//# sourceMappingURL=../../.sourcemap/mp-weixin/utils/api.js.map

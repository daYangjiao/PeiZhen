"use strict";
const common_vendor = require("../common/vendor.js");
const utils_auth = require("./auth.js");
const config = {
  // 开发环境API地址 - 微信开发工具中需要在「详情」-「本地设置」-「不校验合法域名」选项打勾
  baseURL: "http://localhost:8080",
  // 生产环境API地址
  // baseURL: 'https://your-api-domain.com',
  // baseURL:'http://172.16.3.77:8080' ,
  // 请求超时时间
  timeout: 1e4,
  // 请求头配置
  headers: {
    "Content-Type": "application/json",
    "User-Type": "customer"
    // 标识为用户端
  }
};
const getToken = () => {
  const userInfo = utils_auth.getUserInfo();
  return (userInfo == null ? void 0 : userInfo.token) || common_vendor.index.getStorageSync("token") || "";
};
const clearToken = () => {
  common_vendor.index.removeStorageSync("token");
};
const requestInterceptor = (options) => {
  const token = getToken();
  if (token) {
    const safeToken = encodeURIComponent(token);
    options.header = {
      ...options.header,
      "Authorization": `Bearer ${safeToken}`
    };
  }
  options.url = config.baseURL + options.url;
  options.timeout = config.timeout;
  const safeHeaders = {};
  {
    safeHeaders["Content-Type"] = config.headers["Content-Type"];
  }
  for (const key in config.headers) {
    if (config.headers.hasOwnProperty(key) && key !== "Content-Type") {
      const value = config.headers[key];
      safeHeaders[key] = typeof value === "string" ? encodeURIComponent(value) : value;
    }
  }
  for (const key in options.header) {
    if (options.header.hasOwnProperty(key)) {
      const value = options.header[key];
      if (key === "Content-Type") {
        safeHeaders[key] = value;
      } else {
        safeHeaders[key] = typeof value === "string" ? encodeURIComponent(value) : value;
      }
    }
  }
  options.header = safeHeaders;
  return options;
};
const responseInterceptor = (response) => {
  const { statusCode, data } = response;
  if (statusCode >= 200 && statusCode < 300) {
    if (data.code === 200) {
      return Promise.resolve(data);
    } else if (data && typeof data === "object" && !data.hasOwnProperty("code")) {
      return Promise.resolve({ code: 200, data });
    } else if (data.code === 401) {
      utils_auth.clearUserInfo();
      clearToken();
      common_vendor.index.showToast({
        title: "登录已过期，请重新登录",
        icon: "none"
      });
      setTimeout(() => {
        common_vendor.index.navigateTo({
          url: "/subpkg/auth/login"
        });
      }, 1500);
      return Promise.reject(data);
    } else {
      common_vendor.index.showToast({
        title: data.message || "请求失败",
        icon: "none"
      });
      return Promise.reject(data);
    }
  } else {
    common_vendor.index.showToast({
      title: "网络请求失败",
      icon: "none"
    });
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
        common_vendor.index.showToast({
          title: "网络连接失败",
          icon: "none"
        });
        reject(error);
      }
    });
  });
};
const get = (url, params = {}) => {
  return request({
    url,
    method: "GET",
    data: params
  });
};
const post = (url, data = {}) => {
  return request({
    url,
    method: "POST",
    data,
    header: {
      "Content-Type": "application/json"
    }
  });
};
const put = (url, data = {}) => {
  return request({
    url,
    method: "PUT",
    data
  });
};
const upload = (url, filePath, formData = {}) => {
  return new Promise((resolve, reject) => {
    const token = getToken();
    const safeToken = token ? encodeURIComponent(token) : "";
    common_vendor.index.uploadFile({
      url: config.baseURL + url,
      filePath,
      name: "file",
      formData,
      header: {
        "Authorization": safeToken ? `Bearer ${safeToken}` : "",
        "User-Type": encodeURIComponent("customer"),
        // 文件上传需要设置正确的Content-Type
        "Content-Type": "multipart/form-data"
      },
      success: (response) => {
        try {
          const data = JSON.parse(response.data);
          if (data.code === 200) {
            resolve(data);
          } else {
            common_vendor.index.showToast({
              title: data.message || "上传失败",
              icon: "none"
            });
            reject(data);
          }
        } catch (error) {
          reject(error);
        }
      },
      fail: (error) => {
        common_vendor.index.showToast({
          title: "上传失败",
          icon: "none"
        });
        reject(error);
      }
    });
  });
};
exports.config = config;
exports.get = get;
exports.post = post;
exports.put = put;
exports.upload = upload;
//# sourceMappingURL=../../.sourcemap/mp-weixin/utils/api.js.map

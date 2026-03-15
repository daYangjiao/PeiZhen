"use strict";
const common_vendor = require("../../common/vendor.js");
const stores_user = require("../../stores/user.js");
const api_auth = require("../../api/auth.js");
const utils_api = require("../../utils/api.js");
const _sfc_main = {
  data() {
    return {
      redirectUrl: "/pages/index/index",
      formData: {
        username: "",
        password: ""
      },
      isSubmitting: false,
      // 是否同意条款
      agreedToTerms: false,
      // 错误信息
      errors: {
        agreement: ""
      }
    };
  },
  onLoad(options) {
    if (options.redirect) {
      this.redirectUrl = decodeURIComponent(options.redirect);
    }
    this.userStore = stores_user.useUserStore();
    this.userStore.restoreFromStorage();
    if (this.userStore.isLoggedIn) {
      this.navigateToHome();
    }
  },
  methods: {
    // 获取后端图片 URL
    getBackendImageUrl: utils_api.getBackendImageUrl,
    // 账号密码登录
    async handleLogin() {
      if (!this.formData.username) {
        common_vendor.index.showToast({
          title: "请输入用户名",
          icon: "none"
        });
        return;
      }
      if (!this.formData.password) {
        common_vendor.index.showToast({
          title: "请输入密码",
          icon: "none"
        });
        return;
      }
      if (!this.agreedToTerms) {
        this.errors.agreement = "请阅读并同意隐私政策和服务条款";
        common_vendor.index.showToast({
          title: "请阅读并同意隐私政策和服务条款",
          icon: "none"
        });
        return;
      }
      this.isSubmitting = true;
      try {
        const response = await api_auth.login(this.formData);
        common_vendor.index.__f__("log", "at subpkg/auth/login.vue:153", "登录响应数据:", JSON.stringify(response));
        if (response.data && response.data.token) {
          const userInfo = response.data.userInfo;
          if (userInfo.userType === 1) {
            common_vendor.index.showToast({
              title: "陪诊师请使用陪诊师端登录",
              icon: "none",
              duration: 2e3
            });
            return;
          }
          common_vendor.index.showToast({
            title: "登录成功",
            icon: "success"
          });
          common_vendor.index.setStorageSync("token", response.data.token);
          if (userInfo) {
            userInfo.token = response.data.token;
          }
          common_vendor.index.__f__("log", "at subpkg/auth/login.vue:182", "准备保存的用户信息:", JSON.stringify(userInfo));
          this.userStore.setUserInfo(userInfo);
          setTimeout(() => {
            this.navigateToHome();
          }, 1500);
        } else {
          throw new Error(response.message || "登录失败");
        }
      } catch (error) {
        common_vendor.index.__f__("error", "at subpkg/auth/login.vue:195", "登录失败:", error);
        let errorMsg = "登录失败";
        if (error.message) {
          errorMsg = error.message;
        } else if (error.statusCode === 401 || error.data && error.data.code === 401) {
          errorMsg = "用户名或密码错误";
        } else if (error.statusCode >= 500 || error.data && error.data.code >= 500) {
          errorMsg = "服务器错误，请稍后重试";
        } else if (error.statusCode) {
          errorMsg = `请求失败 (${error.statusCode})`;
        }
        common_vendor.index.__f__("log", "at subpkg/auth/login.vue:209", "显示错误信息:", errorMsg);
        common_vendor.index.showToast({
          title: errorMsg,
          icon: "none"
        });
      } finally {
        this.isSubmitting = false;
      }
    },
    // 跳转到首页
    navigateToHome() {
      common_vendor.index.reLaunch({
        url: this.redirectUrl
      });
    },
    // 查看隐私政策
    viewPrivacyPolicy() {
      common_vendor.index.showModal({
        title: "隐私政策",
        content: "这里是隐私政策的内容...",
        showCancel: false
      });
    },
    // 查看服务条款
    viewTermsOfService() {
      common_vendor.index.showModal({
        title: "服务条款",
        content: "这里是服务条款的内容...",
        showCancel: false
      });
    },
    // 跳转到注册页面
    navigateToRegister() {
      common_vendor.index.navigateTo({
        url: "/subpkg/auth/register"
      });
    },
    // 切换同意条款状态
    toggleAgreement() {
      this.agreedToTerms = !this.agreedToTerms;
      if (this.agreedToTerms) {
        this.errors.agreement = "";
      }
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: $options.getBackendImageUrl("mynewlogo.png"),
    b: $data.formData.username,
    c: common_vendor.o(($event) => $data.formData.username = $event.detail.value),
    d: $data.formData.password,
    e: common_vendor.o(($event) => $data.formData.password = $event.detail.value),
    f: common_vendor.o((...args) => $options.handleLogin && $options.handleLogin(...args)),
    g: $data.isSubmitting,
    h: common_vendor.o((...args) => $options.navigateToRegister && $options.navigateToRegister(...args)),
    i: $data.agreedToTerms,
    j: common_vendor.o((...args) => $options.toggleAgreement && $options.toggleAgreement(...args)),
    k: common_vendor.o((...args) => $options.viewPrivacyPolicy && $options.viewPrivacyPolicy(...args)),
    l: common_vendor.o((...args) => $options.viewTermsOfService && $options.viewTermsOfService(...args)),
    m: $data.errors.agreement
  }, $data.errors.agreement ? {
    n: common_vendor.t($data.errors.agreement)
  } : {});
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/subpkg/auth/login.js.map

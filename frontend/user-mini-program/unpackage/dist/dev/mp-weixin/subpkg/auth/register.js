"use strict";
const common_vendor = require("../../common/vendor.js");
const api_auth = require("../../api/auth.js");
const common_assets = require("../../common/assets.js");
const _sfc_main = {
  data() {
    return {
      // 表单数据
      formData: {
        username: "",
        password: "",
        confirmPassword: "",
        name: ""
      },
      // 错误信息
      errors: {
        username: "",
        password: "",
        confirmPassword: "",
        name: "",
        agreement: ""
      },
      // 是否同意条款
      agreedToTerms: false,
      // 提交状态
      isSubmitting: false,
      // 重定向URL
      redirectUrl: "/pages/index/index"
    };
  },
  onLoad(options) {
    if (options.redirect) {
      this.redirectUrl = decodeURIComponent(options.redirect);
    }
  },
  methods: {
    // 验证用户名
    validateUsername() {
      if (!this.formData.username) {
        this.errors.username = "用户名不能为空";
        return false;
      }
      if (this.formData.username.length < 3) {
        this.errors.username = "用户名长度不能少于3个字符";
        return false;
      }
      this.errors.username = "";
      return true;
    },
    // 验证密码
    validatePassword() {
      if (!this.formData.password) {
        this.errors.password = "密码不能为空";
        return false;
      }
      if (this.formData.password.length < 6) {
        this.errors.password = "密码长度不能少于6个字符";
        return false;
      }
      this.errors.password = "";
      return true;
    },
    // 验证确认密码
    validateConfirmPassword() {
      if (!this.formData.confirmPassword) {
        this.errors.confirmPassword = "请确认密码";
        return false;
      }
      if (this.formData.confirmPassword !== this.formData.password) {
        this.errors.confirmPassword = "两次输入的密码不一致";
        return false;
      }
      this.errors.confirmPassword = "";
      return true;
    },
    // 验证姓名
    validateName() {
      if (!this.formData.name) {
        this.errors.name = "姓名不能为空";
        return false;
      }
      this.errors.name = "";
      return true;
    },
    // 验证所有表单项
    validateForm() {
      const usernameValid = this.validateUsername();
      const passwordValid = this.validatePassword();
      const confirmPasswordValid = this.validateConfirmPassword();
      const nameValid = this.validateName();
      if (!this.agreedToTerms) {
        this.errors.agreement = "请阅读并同意隐私政策和服务条款";
        return false;
      } else {
        this.errors.agreement = "";
      }
      return usernameValid && passwordValid && confirmPasswordValid && nameValid && this.agreedToTerms;
    },
    // 切换同意条款状态
    toggleAgreement() {
      this.agreedToTerms = !this.agreedToTerms;
      if (this.agreedToTerms) {
        this.errors.agreement = "";
      }
    },
    // 处理注册
    async handleRegister() {
      if (!this.validateForm()) {
        common_vendor.index.showToast({
          title: "请完善表单信息",
          icon: "none"
        });
        return;
      }
      this.isSubmitting = true;
      try {
        const registerData = {
          username: this.formData.username,
          password: this.formData.password,
          name: this.formData.name,
          openid: ""
          // 添加openid字段，后端要求不能为null
        };
        const response = await api_auth.register(registerData);
        common_vendor.index.showToast({
          title: "注册成功",
          icon: "success"
        });
        if (response.data && response.data.userId) {
          try {
            const { getUserById } = require("@/api/user.js");
            const userDetailResponse = await getUserById(response.data.userId);
            if (userDetailResponse.data) {
              common_vendor.index.__f__("log", "at subpkg/auth/register.vue:261", "获取到用户详细信息:", userDetailResponse.data);
              const userInfo = {
                userId: response.data.userId,
                name: userDetailResponse.data.name,
                username: userDetailResponse.data.username,
                nickName: userDetailResponse.data.username
              };
              common_vendor.index.setStorageSync("lastRegisteredUser", userInfo);
            }
          } catch (detailError) {
            common_vendor.index.__f__("error", "at subpkg/auth/register.vue:275", "获取用户详细信息失败:", detailError);
          }
        }
        setTimeout(() => {
          common_vendor.index.redirectTo({
            url: "/subpkg/auth/login"
          });
        }, 1500);
      } catch (error) {
        common_vendor.index.__f__("error", "at subpkg/auth/register.vue:286", "注册失败:", error);
        common_vendor.index.showToast({
          title: error.message || "注册失败，请重试",
          icon: "none"
        });
      } finally {
        this.isSubmitting = false;
      }
    },
    // 跳转到登录页
    goToLogin() {
      common_vendor.index.redirectTo({
        url: "/subpkg/auth/login"
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
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: common_assets._imports_0$1,
    b: common_vendor.o((...args) => $options.validateUsername && $options.validateUsername(...args)),
    c: $data.formData.username,
    d: common_vendor.o(($event) => $data.formData.username = $event.detail.value),
    e: $data.errors.username
  }, $data.errors.username ? {
    f: common_vendor.t($data.errors.username)
  } : {}, {
    g: common_vendor.o((...args) => $options.validatePassword && $options.validatePassword(...args)),
    h: $data.formData.password,
    i: common_vendor.o(($event) => $data.formData.password = $event.detail.value),
    j: $data.errors.password
  }, $data.errors.password ? {
    k: common_vendor.t($data.errors.password)
  } : {}, {
    l: common_vendor.o((...args) => $options.validateConfirmPassword && $options.validateConfirmPassword(...args)),
    m: $data.formData.confirmPassword,
    n: common_vendor.o(($event) => $data.formData.confirmPassword = $event.detail.value),
    o: $data.errors.confirmPassword
  }, $data.errors.confirmPassword ? {
    p: common_vendor.t($data.errors.confirmPassword)
  } : {}, {
    q: common_vendor.o((...args) => $options.validateName && $options.validateName(...args)),
    r: $data.formData.name,
    s: common_vendor.o(($event) => $data.formData.name = $event.detail.value),
    t: $data.errors.name
  }, $data.errors.name ? {
    v: common_vendor.t($data.errors.name)
  } : {}, {
    w: common_vendor.t($data.isSubmitting ? "注册中..." : "立即注册"),
    x: common_vendor.o((...args) => $options.handleRegister && $options.handleRegister(...args)),
    y: $data.isSubmitting,
    z: common_vendor.o((...args) => $options.goToLogin && $options.goToLogin(...args)),
    A: $data.agreedToTerms,
    B: common_vendor.o((...args) => $options.toggleAgreement && $options.toggleAgreement(...args)),
    C: common_vendor.o((...args) => $options.viewPrivacyPolicy && $options.viewPrivacyPolicy(...args)),
    D: common_vendor.o((...args) => $options.viewTermsOfService && $options.viewTermsOfService(...args)),
    E: $data.errors.agreement
  }, $data.errors.agreement ? {
    F: common_vendor.t($data.errors.agreement)
  } : {});
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/subpkg/auth/register.js.map

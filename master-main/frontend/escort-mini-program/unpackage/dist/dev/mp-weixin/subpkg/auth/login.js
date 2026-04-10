"use strict";
const common_vendor = require("../../common/vendor.js");
const common_assets = require("../../common/assets.js");
const _sfc_main = {
  __name: "login",
  setup(__props) {
    const loginForm = common_vendor.ref({
      phone: "",
      code: ""
    });
    const agreed = common_vendor.ref(false);
    const codeDisabled = common_vendor.ref(false);
    const codeCountdown = common_vendor.ref(0);
    const codeText = common_vendor.ref("获取验证码");
    const isWechatEnv = common_vendor.ref(false);
    const canLogin = common_vendor.computed(() => {
      return loginForm.value.phone.length === 11 && loginForm.value.code.length === 6;
    });
    const showWechatLogin = common_vendor.computed(() => {
      return isWechatEnv.value;
    });
    common_vendor.onMounted(() => {
      checkEnvironment();
      checkLoginStatus();
    });
    const checkEnvironment = () => {
      isWechatEnv.value = true;
      common_vendor.index.__f__("log", "at subpkg/auth/login.vue:127", "当前环境：微信小程序");
    };
    const checkLoginStatus = () => {
      const isLoggedIn = common_vendor.index.getStorageSync("isLoggedIn");
      const userInfo = common_vendor.index.getStorageSync("userInfo");
      if (isLoggedIn && userInfo) {
        common_vendor.index.__f__("log", "at subpkg/auth/login.vue:142", "用户已登录，跳转到首页");
        common_vendor.index.reLaunch({
          url: "/pages/index/index"
        });
      }
    };
    const sendCode = () => {
      if (loginForm.value.phone.length !== 11) {
        common_vendor.index.showToast({
          title: "请输入正确的手机号",
          icon: "none"
        });
        return;
      }
      common_vendor.index.showToast({
        title: "验证码已发送",
        icon: "success"
      });
      startCountdown();
    };
    const startCountdown = () => {
      codeDisabled.value = true;
      codeCountdown.value = 60;
      const timer = setInterval(() => {
        codeCountdown.value--;
        codeText.value = `${codeCountdown.value}s后重发`;
        if (codeCountdown.value <= 0) {
          clearInterval(timer);
          codeDisabled.value = false;
          codeText.value = "获取验证码";
        }
      }, 1e3);
    };
    const login = () => {
      if (!agreed.value) {
        common_vendor.index.showModal({
          title: "提示",
          content: "请先阅读并同意《用户协议》和《隐私政策》",
          showCancel: false,
          confirmText: "确定"
        });
        return;
      }
      if (loginForm.value.phone.length !== 11) {
        common_vendor.index.showToast({
          title: "请输入正确的手机号",
          icon: "none"
        });
        return;
      }
      if (loginForm.value.code.length !== 6) {
        common_vendor.index.showToast({
          title: "请输入6位验证码",
          icon: "none"
        });
        return;
      }
      common_vendor.index.showLoading({
        title: "登录中..."
      });
      setTimeout(() => {
        common_vendor.index.hideLoading();
        common_vendor.index.showToast({
          title: "登录成功",
          icon: "success"
        });
        setTimeout(() => {
          common_vendor.index.reLaunch({
            url: "/pages/index/index"
          });
        }, 1500);
      }, 2e3);
    };
    const onGetPhoneNumber = (e) => {
      common_vendor.index.__f__("log", "at subpkg/auth/login.vue:240", "获取手机号回调:", e.detail);
      if (!agreed.value) {
        common_vendor.index.showToast({
          title: "请先同意用户协议和隐私政策",
          icon: "none"
        });
        return;
      }
      if (e.detail.errMsg === "getPhoneNumber:ok") {
        const { encryptedData, iv } = e.detail;
        if (!encryptedData || !iv) {
          common_vendor.index.showModal({
            title: "授权失败",
            content: "获取授权信息失败，请重试",
            showCancel: false,
            confirmText: "确定"
          });
          return;
        }
        common_vendor.index.login({
          provider: "weixin",
          success: (loginRes) => {
            common_vendor.index.__f__("log", "at subpkg/auth/login.vue:269", "微信登录结果:", loginRes);
            if (loginRes.code) {
              processWechatLogin(loginRes.code);
            } else {
              common_vendor.index.showModal({
                title: "登录失败",
                content: "获取登录凭证失败，请重试",
                showCancel: false,
                confirmText: "确定"
              });
            }
          },
          fail: (err) => {
            common_vendor.index.__f__("error", "at subpkg/auth/login.vue:283", "微信登录失败:", err);
            common_vendor.index.showModal({
              title: "登录失败",
              content: "微信登录失败，请检查网络后重试",
              showCancel: true,
              cancelText: "取消",
              confirmText: "重试",
              success: (res) => {
                if (res.confirm) {
                  onGetPhoneNumber(e);
                }
              }
            });
          }
        });
      } else if (e.detail.errMsg === "getPhoneNumber:fail user deny") {
        common_vendor.index.showModal({
          title: "授权提示",
          content: "需要授权手机号才能使用微信一键登录功能，您也可以选择手机号验证码登录",
          showCancel: true,
          cancelText: "取消",
          confirmText: "重新授权",
          success: (res) => {
            if (res.confirm) {
              common_vendor.index.showToast({
                title: "请点击微信一键登录按钮重新授权",
                icon: "none",
                duration: 3e3
              });
            }
          }
        });
      } else {
        common_vendor.index.__f__("error", "at subpkg/auth/login.vue:320", "获取手机号失败:", e.detail);
        common_vendor.index.showModal({
          title: "授权失败",
          content: "授权过程中出现错误，请重试",
          showCancel: true,
          cancelText: "取消",
          confirmText: "重试",
          success: (res) => {
            if (res.confirm) {
              common_vendor.index.showToast({
                title: "请重新点击微信一键登录",
                icon: "none"
              });
            }
          }
        });
      }
    };
    const processWechatLogin = async (code, encryptedData, iv) => {
      common_vendor.index.showLoading({
        title: "登录中..."
      });
      try {
        await new Promise((resolve) => setTimeout(resolve, 2e3));
        const isSuccess = Math.random() > 0.1;
        if (!isSuccess) {
          throw new Error("服务器处理失败");
        }
        common_vendor.index.hideLoading();
        common_vendor.index.showToast({
          title: "登录成功",
          icon: "success"
        });
        const userInfo = {
          nickName: "微信用户",
          avatarUrl: "/static/user-placeholder.png",
          phone: "138****8888",
          // 实际应该是解密后的真实手机号
          loginType: "wechat",
          token: "mock_token_" + Date.now(),
          loginTime: (/* @__PURE__ */ new Date()).toISOString()
        };
        common_vendor.index.setStorageSync("userInfo", userInfo);
        common_vendor.index.setStorageSync("isLoggedIn", true);
        setTimeout(() => {
          common_vendor.index.reLaunch({
            url: "/pages/index/index"
          });
        }, 1500);
      } catch (error) {
        common_vendor.index.hideLoading();
        common_vendor.index.__f__("error", "at subpkg/auth/login.vue:398", "微信登录处理失败:", error);
        let errorMessage = "登录失败，请重试";
        if (error.message.includes("网络")) {
          errorMessage = "网络连接失败，请检查网络后重试";
        } else if (error.message.includes("服务器")) {
          errorMessage = "服务器繁忙，请稍后重试";
        } else if (error.message.includes("授权")) {
          errorMessage = "授权信息无效，请重新授权";
        }
        common_vendor.index.showModal({
          title: "登录失败",
          content: errorMessage,
          showCancel: true,
          cancelText: "取消",
          confirmText: "重试",
          success: (res) => {
            if (res.confirm) {
              common_vendor.index.__f__("log", "at subpkg/auth/login.vue:419", "用户选择重试登录");
            }
          }
        });
      }
    };
    const onAgreementChange = (e) => {
      agreed.value = e.detail.value.length > 0;
    };
    const showAgreement = (type) => {
      const title = type === "user" ? "用户协议" : "隐私政策";
      common_vendor.index.showToast({
        title: `${title}页面开发中`,
        icon: "none"
      });
    };
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_assets._imports_0$2,
        b: common_assets._imports_1$1,
        c: loginForm.value.phone,
        d: common_vendor.o(($event) => loginForm.value.phone = $event.detail.value),
        e: common_assets._imports_2$1,
        f: loginForm.value.code,
        g: common_vendor.o(($event) => loginForm.value.code = $event.detail.value),
        h: common_vendor.t(codeText.value),
        i: codeDisabled.value,
        j: common_vendor.o(sendCode),
        k: common_vendor.o(login),
        l: !canLogin.value,
        m: agreed.value,
        n: common_vendor.o(($event) => showAgreement("user")),
        o: common_vendor.o(($event) => showAgreement("privacy")),
        p: common_vendor.o(onAgreementChange),
        q: showWechatLogin.value
      }, showWechatLogin.value ? common_vendor.e({
        r: common_assets._imports_3$1,
        s: common_vendor.o(onGetPhoneNumber),
        t: !agreed.value,
        v: !agreed.value
      }, !agreed.value ? {} : {}) : {}, {
        w: !showWechatLogin.value
      }, !showWechatLogin.value ? {} : {});
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-781fbec3"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/subpkg/auth/login.js.map

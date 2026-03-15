"use strict";
const common_vendor = require("../../common/vendor.js");
const common_assets = require("../../common/assets.js");
const utils_api = require("../../utils/api.js");
const _sfc_main = {
  __name: "login",
  setup(__props) {
    const form = common_vendor.ref({
      username: "",
      password: ""
    });
    const agreed = common_vendor.ref(false);
    const onCheckChange = (e) => {
      agreed.value = e.detail.value.length > 0;
    };
    const goRegister = () => {
      common_vendor.index.navigateTo({ url: "/subpkg/auth/register" });
    };
    const handleLogin = async () => {
      if (!agreed.value) {
        common_vendor.index.showToast({ title: "请先同意协议", icon: "none" });
        return;
      }
      if (!form.value.username || !form.value.password) {
        common_vendor.index.showToast({ title: "请输入账号密码", icon: "none" });
        return;
      }
      common_vendor.index.showLoading({ title: "登录中..." });
      try {
        const res = await utils_api.post("/api/users/login", form.value);
        common_vendor.index.hideLoading();
        if (res.code === 200 && res.data) {
          const { token, userInfo } = res.data;
          if (userInfo.userType !== 1) {
            common_vendor.index.showToast({ title: "非陪诊师账号", icon: "none" });
            return;
          }
          utils_api.setToken(token);
          common_vendor.index.setStorageSync("userInfo", userInfo);
          common_vendor.index.setStorageSync("isLoggedIn", true);
          common_vendor.index.showToast({ title: "登录成功", icon: "success" });
          setTimeout(() => {
            common_vendor.index.reLaunch({ url: "/pages/index/index" });
          }, 1500);
        } else {
          common_vendor.index.showToast({ title: res.message || "登录失败", icon: "none" });
        }
      } catch (e) {
        common_vendor.index.hideLoading();
        common_vendor.index.showToast({ title: e.message || "登录失败", icon: "none" });
      }
    };
    return (_ctx, _cache) => {
      return {
        a: common_assets._imports_0$2,
        b: form.value.username,
        c: common_vendor.o(($event) => form.value.username = $event.detail.value),
        d: form.value.password,
        e: common_vendor.o(($event) => form.value.password = $event.detail.value),
        f: agreed.value,
        g: common_vendor.o(onCheckChange),
        h: common_vendor.o(handleLogin),
        i: common_vendor.o(goRegister)
      };
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-781fbec3"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/subpkg/auth/login.js.map

"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_api = require("../../utils/api.js");
const _sfc_main = {
  __name: "register",
  setup(__props) {
    const form = common_vendor.ref({
      username: "",
      password: "",
      name: "",
      phone: "",
      professionalField: "",
      introduction: "",
      userType: 1
      // 强制为陪诊师类型
    });
    const goBack = () => {
      common_vendor.index.navigateBack();
    };
    const goLogin = () => {
      common_vendor.index.redirectTo({ url: "/subpkg/auth/login" });
    };
    const handleRegister = async () => {
      if (!form.value.username || !form.value.password || !form.value.name || !form.value.phone) {
        common_vendor.index.showToast({ title: "请填写必填项", icon: "none" });
        return;
      }
      common_vendor.index.showLoading({ title: "提交中..." });
      try {
        const res = await utils_api.post("/api/users/register", form.value);
        common_vendor.index.hideLoading();
        if (res.code === 200) {
          common_vendor.index.showModal({
            title: "注册成功",
            content: "您的入驻申请已提交，请使用账号登录",
            showCancel: false,
            success: () => {
              goLogin();
            }
          });
        }
      } catch (e) {
        common_vendor.index.hideLoading();
      }
    };
    return (_ctx, _cache) => {
      return {
        a: common_vendor.o(goBack),
        b: form.value.username,
        c: common_vendor.o(($event) => form.value.username = $event.detail.value),
        d: form.value.password,
        e: common_vendor.o(($event) => form.value.password = $event.detail.value),
        f: form.value.name,
        g: common_vendor.o(($event) => form.value.name = $event.detail.value),
        h: form.value.phone,
        i: common_vendor.o(($event) => form.value.phone = $event.detail.value),
        j: form.value.professionalField,
        k: common_vendor.o(($event) => form.value.professionalField = $event.detail.value),
        l: form.value.introduction,
        m: common_vendor.o(($event) => form.value.introduction = $event.detail.value),
        n: common_vendor.o(handleRegister),
        o: common_vendor.o(goLogin)
      };
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-2f0856ca"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/subpkg/auth/register.js.map

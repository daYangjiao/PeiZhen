"use strict";
Object.defineProperty(exports, Symbol.toStringTag, { value: "Module" });
const common_vendor = require("./common/vendor.js");
if (!Math) {
  "./pages/index/index.js";
  "./pages/order/order.js";
  "./pages/message/message.js";
  "./pages/profile/profile.js";
  "./pages/appointment/appointment.js";
  "./pages/AIaks/AIaks.js";
  "./pages/AppointmentForm/AppointmentForm.js";
  "./pages/AICareMatchPage/AICareMatchPage.js";
  "./pages/ServiceCompanionSelect/ServiceCompanionSelect.js";
  "./pages/OrderConfirmPage/OrderConfirmPage.js";
  "./pages/PaymentSuccessPage/PaymentSuccessPage.js";
  "./pages/OrderDetailPage/OrderDetailPage.js";
  "./pages/Evaluate/Evaluate.js";
  "./pages/AItriage/01_AppointmentSelection.js";
  "./pages/AItriage/02_AppointmentForm.js";
  "./pages/AItriage/03_AICareMatchPage.js";
  "./pages/AItriage/04_OrderConfirmPage.js";
  "./pages/AItriage/05_PaymentSuccessPage.js";
  "./subpkg/auth/login.js";
  "./subpkg/auth/register.js";
  "./subpkg/appointment/appointment-time.js";
  "./subpkg/profile/edit-profile.js";
}
const _sfc_main = {
  onLaunch: function() {
    common_vendor.index.__f__("log", "at App.vue:4", "🌟🌟🌟 App Launch - 代码已更新 - 测试时间:", (/* @__PURE__ */ new Date()).toLocaleTimeString(), "🌟🌟🌟");
    common_vendor.index.__f__("log", "at App.vue:5", "App Launch");
  },
  onShow: function() {
    common_vendor.index.__f__("log", "at App.vue:8", "App Show");
  },
  onHide: function() {
    common_vendor.index.__f__("log", "at App.vue:11", "App Hide");
  }
};
function createApp() {
  const app = common_vendor.createSSRApp(_sfc_main);
  const pinia = common_vendor.createPinia();
  app.use(pinia);
  return {
    app
  };
}
createApp().app.mount("#app");
exports.createApp = createApp;
//# sourceMappingURL=../.sourcemap/mp-weixin/app.js.map

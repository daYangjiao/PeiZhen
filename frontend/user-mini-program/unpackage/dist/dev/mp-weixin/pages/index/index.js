"use strict";
const common_vendor = require("../../common/vendor.js");
const common_assets = require("../../common/assets.js");
const api_attendant = require("../../api/attendant.js");
const utils_api = require("../../utils/api.js");
const _sfc_main = {
  __name: "index",
  setup(__props) {
    const searchKeyword = common_vendor.ref("");
    const categories = common_vendor.ref([
      { name: "门诊陪诊", icon: utils_api.getBackendImageUrl("category1.jpg") },
      { name: "住院陪护", icon: utils_api.getBackendImageUrl("category2.jpg") },
      { name: "专家会诊", icon: utils_api.getBackendImageUrl("category3.jpg") },
      { name: "检查陪同", icon: utils_api.getBackendImageUrl("category4.jpg") }
    ]);
    const services = common_vendor.ref([
      { name: "预约服务", icon: "/static/yvyue_2.png" },
      { name: "匹配陪诊员", icon: "/static/ren_1.png" },
      { name: "专业陪诊", icon: "/static/xin.png" },
      { name: "评价反馈", icon: "/static/wujiaoxin.png" }
    ]);
    const companions = common_vendor.ref([]);
    const btnLeft = common_vendor.ref(0);
    const btnTop = common_vendor.ref(0);
    const isDragging = common_vendor.ref(false);
    const startX = common_vendor.ref(0);
    const startY = common_vendor.ref(0);
    const startLeft = common_vendor.ref(0);
    const startTop = common_vendor.ref(0);
    const handleTouchStart = (e) => {
      const touch = e.touches[0];
      startX.value = touch.clientX;
      startY.value = touch.clientY;
      startLeft.value = btnLeft.value;
      startTop.value = btnTop.value;
      isDragging.value = true;
    };
    const handleTouchMove = (e) => {
      if (!isDragging.value)
        return;
      const touch = e.touches[0];
      const deltaX = touch.clientX - startX.value;
      const deltaY = touch.clientY - startY.value;
      updatePosition(startLeft.value + deltaX, startTop.value + deltaY);
    };
    const updatePosition = (x, y) => {
      const sysInfo = common_vendor.index.getSystemInfoSync();
      const windowWidth = sysInfo.windowWidth;
      const windowHeight = sysInfo.windowHeight;
      const btnSizePx = 60 * (windowWidth / 750);
      x = Math.max(0, Math.min(windowWidth - btnSizePx, x));
      y = Math.max(0, Math.min(windowHeight - btnSizePx, y));
      btnLeft.value = x;
      btnTop.value = y;
    };
    const setInitialPosition = () => {
      const sysInfo = common_vendor.index.getSystemInfoSync();
      const windowWidth = sysInfo.windowWidth;
      const windowHeight = sysInfo.windowHeight;
      const btnSizePx = 60 * (windowWidth / 750);
      btnLeft.value = windowWidth - btnSizePx;
      btnTop.value = windowHeight - btnSizePx;
    };
    const navigateToAIaks = () => {
      common_vendor.index.navigateTo({ url: "/pages/AIaks/AIaks" });
    };
    const navigateToAppointmentForm = () => {
      common_vendor.index.switchTab({
        url: "/pages/AItriage/01_AppointmentSelection",
        // tabbar页面路径
        success: () => {
          common_vendor.index.__f__("log", "at pages/index/index.vue:191", "跳转到tabbar页面成功");
        },
        fail: (err) => {
          common_vendor.index.__f__("error", "at pages/index/index.vue:194", "跳转失败", err);
        }
      });
    };
    const navigateToCompanion = (companion) => {
      common_vendor.index.showToast({ title: "陪诊师详情功能暂未开放", icon: "none" });
    };
    const handleSearch = () => {
      if (searchKeyword.value.trim()) {
        common_vendor.index.showToast({ title: `搜索"${searchKeyword.value}"`, icon: "none" });
      } else {
        common_vendor.index.showToast({ title: "请输入搜索内容", icon: "none" });
      }
    };
    const getFullAvatarUrl = (relativePath) => {
      if (!relativePath)
        return utils_api.getBackendImageUrl("default-avatar.jpg");
      if (relativePath.startsWith("http"))
        return relativePath;
      const baseUrl = utils_api.config.baseURL.endsWith("/") ? utils_api.config.baseURL : utils_api.config.baseURL + "/";
      const avatarPath = relativePath.startsWith("/") ? relativePath.substring(1) : relativePath;
      return baseUrl + avatarPath;
    };
    const fetchAttendants = async () => {
      try {
        const res = await api_attendant.getRecommendedAttendants();
        if (res.code === 200 && res.data) {
          companions.value = res.data;
        } else {
          common_vendor.index.showToast({ title: "获取陪诊师列表失败", icon: "none" });
        }
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/index/index.vue:232", "获取陪诊师列表出错:", error);
      }
    };
    common_vendor.onMounted(() => {
      setInitialPosition();
      fetchAttendants();
    });
    return (_ctx, _cache) => {
      return {
        a: common_assets._imports_0,
        b: common_vendor.o(handleSearch),
        c: searchKeyword.value,
        d: common_vendor.o(($event) => searchKeyword.value = $event.detail.value),
        e: common_vendor.unref(utils_api.getBackendImageUrl)("banner.jpg"),
        f: common_vendor.o(navigateToAppointmentForm),
        g: common_vendor.f(categories.value, (item, index, i0) => {
          return {
            a: item.icon,
            b: common_vendor.t(item.name),
            c: index,
            d: common_vendor.o(($event) => _ctx.navigateToCategory(item), index)
          };
        }),
        h: common_vendor.f(services.value, (item, index, i0) => {
          return {
            a: item.icon,
            b: common_vendor.t(item.name),
            c: index
          };
        }),
        i: common_vendor.f(companions.value, (companion, index, i0) => {
          return {
            a: getFullAvatarUrl(companion.avatar),
            b: common_vendor.t(companion.name),
            c: common_vendor.t(companion.professionalField),
            d: common_vendor.t(companion.experienceYears),
            e: common_vendor.t(companion.score),
            f: common_vendor.t(companion.serviceCount || 0),
            g: index,
            h: common_vendor.o(($event) => navigateToCompanion(), index)
          };
        }),
        j: common_vendor.unref(utils_api.getBackendImageUrl)("mynewlogo.png"),
        k: btnLeft.value + "px",
        l: btnTop.value + "px",
        m: common_vendor.o(handleTouchStart),
        n: common_vendor.o(handleTouchMove),
        o: common_vendor.o(($event) => isDragging.value = false),
        p: common_vendor.o((...args) => _ctx.handleMouseDown && _ctx.handleMouseDown(...args)),
        q: common_vendor.o((...args) => _ctx.handleMouseMove && _ctx.handleMouseMove(...args)),
        r: common_vendor.o(($event) => isDragging.value = false),
        s: common_vendor.o(($event) => isDragging.value = false),
        t: common_vendor.o(navigateToAIaks)
      };
    };
  }
};
wx.createPage(_sfc_main);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/index/index.js.map

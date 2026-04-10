"use strict";
const common_vendor = require("../../common/vendor.js");
const common_assets = require("../../common/assets.js");
const api_attendant = require("../../api/attendant.js");
const _sfc_main = {
  __name: "index",
  setup(__props) {
    const searchKeyword = common_vendor.ref("");
    const categories = common_vendor.ref([
      { name: "门诊陪诊", icon: "/static/category1.jpg" },
      { name: "住院陪护", icon: "/static/category2.jpg" },
      { name: "专家会诊", icon: "/static/category3.jpg" },
      { name: "检查陪同", icon: "/static/category4.jpg" }
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
    const handleMouseDown = (e) => {
      startX.value = e.clientX;
      startY.value = e.clientY;
      startLeft.value = btnLeft.value;
      startTop.value = btnTop.value;
      isDragging.value = true;
    };
    const handleMouseMove = (e) => {
      if (!isDragging.value)
        return;
      const deltaX = e.clientX - startX.value;
      const deltaY = e.clientY - startY.value;
      updatePosition(startLeft.value + deltaX, startTop.value + deltaY);
    };
    const updatePosition = (x, y) => {
      const sysInfo = common_vendor.index.getSystemInfoSync();
      const windowWidth = sysInfo.windowWidth;
      const windowHeight = sysInfo.windowHeight;
      const rpxToPx = windowWidth / 750;
      const btnSizePx = 60 * rpxToPx;
      x = Math.max(0, Math.min(windowWidth - btnSizePx, x));
      y = Math.max(0, Math.min(windowHeight - btnSizePx, y));
      btnLeft.value = x;
      btnTop.value = y;
    };
    const setInitialPosition = () => {
      const sysInfo = common_vendor.index.getSystemInfoSync();
      const windowWidth = sysInfo.windowWidth;
      const windowHeight = sysInfo.windowHeight;
      const rpxToPx = windowWidth / 750;
      const btnSizePx = 60 * rpxToPx;
      const initialLeft = windowWidth - btnSizePx;
      const initialTop = windowHeight - btnSizePx;
      btnLeft.value = initialLeft;
      btnTop.value = initialTop;
    };
    const navigateToAIaks = () => {
      common_vendor.index.navigateTo({
        url: "/pages/AIaks/AIaks"
      });
    };
    const navigateToAppointmentForm = () => {
      common_vendor.index.navigateTo({
        url: "/pages/AItriage/01_AppointmentSelection"
      });
    };
    const navigateToCategory = (category) => {
      common_vendor.index.__f__("log", "at pages/index/index.vue:224", "点击分类:", category.name);
      common_vendor.index.switchTab({ url: "/pages/appointment/appointment" });
    };
    const navigateToCompanion = (companion) => {
      common_vendor.index.__f__("log", "at pages/index/index.vue:229", "点击陪诊员:", companion.name, "陪诊员ID:", companion.id);
      common_vendor.index.showToast({ title: "陪诊师详情功能暂未开放", icon: "none" });
    };
    const handleSearch = () => {
      if (searchKeyword.value.trim()) {
        common_vendor.index.__f__("log", "at pages/index/index.vue:235", "搜索关键词:", searchKeyword.value);
        common_vendor.index.showToast({ title: `搜索"${searchKeyword.value}"`, icon: "none" });
      } else {
        common_vendor.index.showToast({ title: "请输入搜索内容", icon: "none" });
      }
    };
    const fetchAttendants = async () => {
      try {
        const res = await api_attendant.getAllAttendants();
        common_vendor.index.__f__("log", "at pages/index/index.vue:251", "陪诊师列表:", res);
        if (res.code === 200 && res.data) {
          companions.value = res.data.map((item) => ({
            id: item.id,
            name: item.name || "陪诊师" + item.id,
            specialty: item.certificate || "专业陪诊",
            experience: item.introduction || "专业陪诊服务",
            rating: 4.8,
            serviceCount: 100,
            avatar: "/static/provider" + (item.id % 2 + 1) + ".jpg"
          }));
        } else {
          common_vendor.index.showToast({ title: "获取陪诊师列表失败", icon: "none" });
        }
      } catch (error) {
        common_vendor.index.__f__("error", "at pages/index/index.vue:266", "获取陪诊师列表出错:", error);
        common_vendor.index.showToast({ title: "网络错误，请稍后重试", icon: "none" });
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
        e: common_assets._imports_1,
        f: common_vendor.o(navigateToAppointmentForm),
        g: common_vendor.f(categories.value, (item, index, i0) => {
          return {
            a: item.icon,
            b: common_vendor.t(item.name),
            c: index,
            d: common_vendor.o(($event) => navigateToCategory(item), index)
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
            a: companion.avatar,
            b: common_vendor.t(companion.name),
            c: common_vendor.t(companion.specialty),
            d: common_vendor.t(companion.experience),
            e: common_vendor.t(companion.rating),
            f: common_vendor.t(companion.serviceCount),
            g: index,
            h: common_vendor.o(($event) => navigateToCompanion(companion), index)
          };
        }),
        j: common_assets._imports_0$1,
        k: btnLeft.value + "px",
        l: btnTop.value + "px",
        m: common_vendor.o(handleTouchStart),
        n: common_vendor.o(handleTouchMove),
        o: common_vendor.o(($event) => isDragging.value = false),
        p: common_vendor.o(handleMouseDown),
        q: common_vendor.o(handleMouseMove),
        r: common_vendor.o(($event) => isDragging.value = false),
        s: common_vendor.o(($event) => isDragging.value = false),
        t: common_vendor.o(navigateToAIaks)
      };
    };
  }
};
wx.createPage(_sfc_main);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/index/index.js.map

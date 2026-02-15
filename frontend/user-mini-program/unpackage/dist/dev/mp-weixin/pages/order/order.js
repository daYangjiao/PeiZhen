"use strict";
const common_vendor = require("../../common/vendor.js");
const common_assets = require("../../common/assets.js");
const ORDERS_STORAGE_KEY = "all_orders";
const _sfc_main = {
  __name: "order",
  setup(__props) {
    const statusBarHeight = common_vendor.ref(0);
    const searchKeyword = common_vendor.ref("");
    const statusTabs = common_vendor.ref([
      { name: "全部", value: "all" },
      { name: "待接单", value: "assigned" },
      { name: "进行中", value: "in_progress" },
      // 新增
      { name: "已完成", value: "completed" },
      // 修改：值为 'completed'
      { name: "待支付", value: "pending" },
      { name: "已取消", value: "cancelled" }
    ]);
    const activeStatus = common_vendor.ref("all");
    const selectedOrders = common_vendor.ref([]);
    const orders = common_vendor.ref([]);
    function generateDemoOrders() {
      const demoOrders = [
        {
          id: "1001",
          orderId: "ORD202512270001",
          status: "pending",
          // 待支付
          serviceType: "普通陪诊",
          serviceTime: "2025-12-28 10:00",
          serviceDate: "2025-12-28",
          startTime: "10:00",
          hospitalName: "中山大学附属第一医院",
          contactPerson: "张三",
          price: "¥150.00",
          paymentMethod: "微信支付",
          payTime: "2025-12-27 09:30",
          attendantName: "王丽",
          // 示例陪诊师名
          attendantPhone: "13800138000",
          // attendantAvatar: '/static/provider1.jpg', // 注释掉，使用新方法
          rating: 4.8
        },
        {
          id: "1002",
          orderId: "ORD202512270002",
          status: "assigned",
          // 待陪诊师接单
          serviceType: "专家陪诊",
          serviceTime: "2025-12-28 14:00",
          serviceDate: "2025-12-28",
          startTime: "14:00",
          hospitalName: "南方医科大学珠江医院",
          contactPerson: "李四",
          price: "¥280.00",
          paymentMethod: "支付宝",
          payTime: "2025-12-27 11:15",
          attendantName: "张伟",
          // 示例陪诊师名
          attendantPhone: "13800138001",
          // attendantAvatar: '/static/38da0ad384c9103b9b6de1a987e16191.jpg', // 注释掉，使用新方法
          rating: 4.9
        },
        {
          id: "1003",
          orderId: "ORD202512270003",
          status: "in_progress",
          // 进行中
          serviceType: "普通陪诊",
          serviceTime: "2025-12-27 15:00",
          serviceDate: "2025-12-27",
          startTime: "15:00",
          hospitalName: "广州医科大学附属第三医院",
          contactPerson: "王五",
          price: "¥150.00",
          paymentMethod: "微信支付",
          payTime: "2025-12-27 08:00",
          attendantName: "刘芳",
          // 示例陪诊师名
          attendantPhone: "13800138002",
          // attendantAvatar: '/static/1a13f15c8114696a50ebd020c621cc8f.jpg', // 注释掉，使用新方法
          rating: 4.7
        },
        {
          id: "1004",
          orderId: "ORD202512270004",
          status: "completed",
          // 已完成
          serviceType: "陪诊+报告解读",
          serviceTime: "2025-12-26 09:30",
          serviceDate: "2025-12-26",
          startTime: "09:30",
          hospitalName: "广东省人民医院",
          contactPerson: "孙六",
          price: "¥320.00",
          paymentMethod: "银联卡",
          payTime: "2025-12-26 08:45",
          attendantName: "王丽",
          // 示例陪诊师名
          attendantPhone: "13800138000",
          // attendantAvatar: '/static/provider1.jpg', // 注释掉，使用新方法
          rating: 4.95
        },
        {
          id: "1005",
          orderId: "ORD202512270005",
          status: "cancelled",
          // 已取消
          serviceType: "普通陪诊",
          serviceTime: "2025-12-29 11:00",
          serviceDate: "2025-12-29",
          startTime: "11:00",
          hospitalName: "中山大学附属第三医院",
          contactPerson: "周七",
          price: "¥150.00",
          paymentMethod: "微信支付",
          payTime: "2025-12-27 07:00",
          attendantName: "张伟",
          // 示例陪诊师名
          attendantPhone: "13800138001",
          // attendantAvatar: '/static/38da0ad384c9103b9b6de1a987e16191.jpg', // 注释掉，使用新方法
          rating: 4.6
        }
      ];
      return demoOrders;
    }
    common_vendor.onMounted(async () => {
      const systemInfo = common_vendor.index.getSystemInfoSync();
      statusBarHeight.value = systemInfo.statusBarHeight || 0;
      loadOrdersFromStorage();
    });
    common_vendor.onShow(() => {
      loadOrdersFromStorage();
    });
    function loadOrdersFromStorage() {
      try {
        const storedOrders = common_vendor.index.getStorageSync(ORDERS_STORAGE_KEY);
        if (storedOrders && Array.isArray(storedOrders) && storedOrders.length > 0) {
          orders.value = storedOrders;
          common_vendor.index.__f__("log", "at pages/order/order.vue:274", "【order.vue】从缓存加载的订单列表:", orders.value);
        } else {
          const demoOrders = generateDemoOrders();
          orders.value = demoOrders;
          common_vendor.index.setStorageSync(ORDERS_STORAGE_KEY, orders.value);
          demoOrders.forEach((order) => {
            const orderKey = `order_${order.id}`;
            common_vendor.index.setStorageSync(orderKey, order);
            common_vendor.index.__f__("log", "at pages/order/order.vue:285", `【order.vue】为订单 ${order.id} 创建缓存项: ${orderKey}`);
          });
          common_vendor.index.__f__("log", "at pages/order/order.vue:287", "【order.vue】演示数据已加载并缓存到 all_orders 和各自的 order_XXX 项中");
        }
      } catch (e) {
        common_vendor.index.__f__("error", "at pages/order/order.vue:290", "读取本地存储订单数据失败:", e);
        orders.value = generateDemoOrders();
      }
    }
    const getAvatarByAttendantName = (name) => {
      const avatarMap = {
        "王丽": "/static/provider1.jpg",
        "张伟": "/static/38da0ad384c9103b9b6de1a987e16191.jpg",
        "刘芳": "/static/1a13f15c8114696a50ebd020c621cc8f.jpg"
        // 你可以根据需要添加更多陪诊师及其对应的头像路径
        // '李四': '/static/lisi.jpg',
        // '赵六': '/static/zhaoliu.jpg',
      };
      if (name && avatarMap[name]) {
        return avatarMap[name];
      }
      return "/static/default-avatar.jpg";
    };
    const filteredOrders = common_vendor.computed(() => {
      let result = [...orders.value];
      if (activeStatus.value !== "all") {
        result = result.filter((order) => order.status === activeStatus.value);
      }
      if (searchKeyword.value.trim()) {
        const keyword = searchKeyword.value.toLowerCase();
        result = result.filter(
          (order) => order.orderId.toLowerCase().includes(keyword) || order.hospitalName.toLowerCase().includes(keyword) || order.serviceType.toLowerCase().includes(keyword)
        );
      }
      return result;
    });
    const getStatusClass = (status) => {
      switch (status) {
        case "assigned":
          return "status-assigned";
        case "in_progress":
          return "status-in-progress";
        case "completed":
          return "status-completed";
        case "pending":
          return "status-pending";
        case "cancelled":
          return "status-cancelled";
        default:
          return "status-default";
      }
    };
    const getStatusText = (status) => {
      switch (status) {
        case "assigned":
          return "待陪诊师接单";
        case "in_progress":
          return "进行中";
        case "completed":
          return "已完成";
        case "pending":
          return "待支付";
        case "cancelled":
          return "已取消";
        default:
          return "未知状态";
      }
    };
    const handleCheckboxChange = (e, orderId) => {
      const checked = e.detail.value.length > 0;
      if (checked) {
        if (!selectedOrders.value.includes(orderId)) {
          selectedOrders.value.push(orderId);
        }
      } else {
        selectedOrders.value = selectedOrders.value.filter((id) => id !== orderId);
      }
    };
    const downloadSelectedOrders = () => {
      if (selectedOrders.value.length === 0)
        return;
      common_vendor.index.showToast({
        title: `已选择 ${selectedOrders.value.length} 个订单，即将批量下载`
      });
    };
    const handleOrderClick = (order) => {
      if (!(order == null ? void 0 : order.id)) {
        common_vendor.index.showToast({ title: "无效订单", icon: "none" });
        return;
      }
      common_vendor.index.navigateTo({
        url: `/pages/OrderDetailPage/OrderDetailPage?orderNo=${order.id}`
      }).catch((err) => {
        common_vendor.index.__f__("error", "at pages/order/order.vue:408", "跳转失败:", err);
        common_vendor.index.showToast({ title: "页面不存在", icon: "none" });
      });
    };
    const handleDetailClick = (order) => {
      handleOrderClick(order);
    };
    const handleDeleteClick = (order) => {
      common_vendor.index.showModal({
        title: "确认删除",
        content: `确定要删除订单 ${order.orderId} 吗？此操作不可撤销。`,
        success: (res) => {
          if (res.confirm) {
            const updatedOrders = orders.value.filter((o) => o.id !== order.id);
            orders.value = updatedOrders;
            common_vendor.index.setStorageSync(ORDERS_STORAGE_KEY, updatedOrders);
            common_vendor.index.removeStorageSync(`order_${order.id}`);
            common_vendor.index.__f__("log", "at pages/order/order.vue:430", `【order.vue】删除订单 ${order.id} 的缓存项`);
            common_vendor.index.showToast({ title: "删除成功", icon: "success" });
          }
        },
        fail: (err) => {
          common_vendor.index.__f__("error", "at pages/order/order.vue:435", "弹窗确认失败:", err);
        }
      });
    };
    const handleSearch = () => {
      common_vendor.index.__f__("log", "at pages/order/order.vue:442", "搜索:", searchKeyword.value);
    };
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_assets._imports_0,
        b: common_vendor.o(handleSearch),
        c: searchKeyword.value,
        d: common_vendor.o(($event) => searchKeyword.value = $event.detail.value),
        e: common_vendor.f(statusTabs.value, (tab, index, i0) => {
          return {
            a: common_vendor.t(tab.name),
            b: index,
            c: activeStatus.value === tab.value ? 1 : "",
            d: common_vendor.o(($event) => activeStatus.value = tab.value, index)
          };
        }),
        f: selectedOrders.value.length > 0
      }, selectedOrders.value.length > 0 ? {
        g: common_vendor.t(selectedOrders.value.length),
        h: common_vendor.o(downloadSelectedOrders)
      } : {}, {
        i: filteredOrders.value.length > 0
      }, filteredOrders.value.length > 0 ? {
        j: common_vendor.f(filteredOrders.value, (order, k0, i0) => {
          return common_vendor.e({
            a: common_vendor.t(order.orderId.slice(-4)),
            b: common_vendor.t(getStatusText(order.status)),
            c: common_vendor.n(getStatusClass(order.status)),
            d: common_vendor.t(order.serviceType),
            e: common_vendor.t(order.hospitalName),
            f: common_vendor.t(order.serviceDate),
            g: common_vendor.t(order.startTime),
            h: common_vendor.t(order.status === "pending" ? "待支付" : order.price),
            i: order.status === "pending" ? 1 : "",
            j: getAvatarByAttendantName(order.attendantName),
            k: common_vendor.t(order.attendantName),
            l: common_vendor.f(5, (i, k1, i1) => {
              return {
                a: common_vendor.t(i <= order.rating ? "★" : "☆"),
                b: i
              };
            }),
            m: common_vendor.o(($event) => handleDetailClick(order), order.id),
            n: order.status === "completed" || order.status === "cancelled"
          }, order.status === "completed" || order.status === "cancelled" ? {
            o: common_vendor.o(($event) => handleDeleteClick(order), order.id)
          } : {}, {
            p: order.id,
            q: selectedOrders.value.includes(order.id),
            r: common_vendor.o(($event) => handleCheckboxChange($event, order.id), order.id),
            s: order.id,
            t: common_vendor.o(($event) => handleOrderClick(order), order.id)
          });
        })
      } : {
        k: common_assets._imports_1$1
      }, {
        l: statusBarHeight.value + "px"
      });
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-93207a4f"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/order/order.js.map

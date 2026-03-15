const userTabs = [
  { pagePath: 'pages/role-user/home', text: '首页', iconPath: '/static/shouye.png', selectedIconPath: '/static/shouye_active.png' },
  { pagePath: 'pages/role-user/order', text: '订单', iconPath: '/static/order.png', selectedIconPath: '/static/order_active.png' },
  { pagePath: 'pages/AItriage/01_AppointmentSelection', text: '预约', iconPath: '/static/yvyue_2.png', selectedIconPath: '/static/yvyue_2_active.png' },
  { pagePath: 'pages/role-user/message', text: '消息', iconPath: '/static/xiaoxi_2.png', selectedIconPath: '/static/xiaoxi_2_active.png' },
  { pagePath: 'pages/role-user/profile', text: '我的', iconPath: '/static/wode_2.png', selectedIconPath: '/static/wode_2_active.png' }
]

const escortTabs = [
  { pagePath: 'pages/role-escort/hall', text: '接单厅', iconPath: '/static/shouye.png', selectedIconPath: '/static/shouye_active.png' },
  { pagePath: 'pages/role-escort/order', text: '订单', iconPath: '/static/order.png', selectedIconPath: '/static/order_active.png' },
  { pagePath: 'pages/role-escort/message', text: '消息', iconPath: '/static/xiaoxi_2.png', selectedIconPath: '/static/xiaoxi_2_active.png' },
  { pagePath: 'pages/role-escort/profile', text: '我的', iconPath: '/static/wode_2.png', selectedIconPath: '/static/wode_2_active.png' }
]

Component({
  data: {
    tabs: userTabs,
    selectedIndex: 0
  },
  attached() {
    this.updateTabs()
  },
  methods: {
    updateTabs() {
      const role = wx.getStorageSync('role') || 'user'
      const pages = getCurrentPages()
      const currentRoute = pages[pages.length - 1].route
      const tabs = role === 'escort' ? escortTabs : userTabs
      const index = tabs.findIndex(t => t.pagePath === currentRoute)
      this.setData({
        tabs,
        selectedIndex: index >= 0 ? index : 0
      })
    },
    onTabTap(e) {
      const index = e.currentTarget.dataset.index
      const item = this.data.tabs[index]
      const url = '/' + item.pagePath
      this.setData({ selectedIndex: index })
      wx.switchTab({ url })
    }
  },
  pageLifetimes: {
    show() {
      this.updateTabs()
    }
  }
})


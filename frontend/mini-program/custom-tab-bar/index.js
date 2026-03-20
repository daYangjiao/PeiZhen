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

const MESSAGE_BADGE_STORAGE_KEY = 'tabbar_message_badge'
const platformApi = typeof uni !== 'undefined' ? uni : wx

const isMessageTab = (pagePath) => /\/message$/.test(pagePath || '')

const withMessageBadge = (tabs, badgeCount) => {
  return tabs.map(tab => Object.assign({}, tab, {
    badge: isMessageTab(tab.pagePath) ? badgeCount : 0
  }))
}

Component({
  data: {
    tabs: withMessageBadge(userTabs, 0),
    selectedIndex: 0,
    badgeCount: 0
  },
  attached() {
    this.updateTabs()
    this.badgeTimer = setInterval(() => {
      this.syncBadge()
    }, 800)
  },
  detached() {
    if (this.badgeTimer) {
      clearInterval(this.badgeTimer)
      this.badgeTimer = null
    }
  },
  methods: {
    updateTabs() {
      const role = platformApi.getStorageSync('role') || 'user'
      const pages = typeof getCurrentPages === 'function' ? getCurrentPages() : []
      const currentRoute = pages.length ? pages[pages.length - 1].route : ''
      const badgeCount = Number(platformApi.getStorageSync(MESSAGE_BADGE_STORAGE_KEY) || 0)
      const sourceTabs = role === 'escort' ? escortTabs : userTabs
      const tabs = withMessageBadge(sourceTabs, badgeCount)
      const index = tabs.findIndex(t => t.pagePath === currentRoute)
      this.setData({
        tabs,
        selectedIndex: index >= 0 ? index : 0,
        badgeCount
      })
    },
    syncBadge() {
      const nextBadgeCount = Number(platformApi.getStorageSync(MESSAGE_BADGE_STORAGE_KEY) || 0)
      if (nextBadgeCount === this.data.badgeCount) return
      const tabs = (this.data.tabs || []).map(tab => Object.assign({}, tab, {
        badge: isMessageTab(tab.pagePath) ? nextBadgeCount : 0
      }))
      this.setData({
        tabs,
        badgeCount: nextBadgeCount
      })
    },
    onTabTap(e) {
      const index = e.currentTarget.dataset.index
      const item = this.data.tabs[index]
      const url = '/' + item.pagePath
      this.setData({ selectedIndex: index })
      platformApi.switchTab({ url })
    }
  },
  pageLifetimes: {
    show() {
      this.updateTabs()
    }
  }
})

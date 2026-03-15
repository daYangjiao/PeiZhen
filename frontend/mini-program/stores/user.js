import { defineStore } from 'pinia'

export const useUserStore = defineStore('user', {
  state: () => ({
    userInfo: null,
    isLoggedIn: false,
    isGuestMode: false,
    loginTime: null
  }),
  getters: {
    displayName: (state) => {
      if (!state.userInfo) return ''
      return state.userInfo.nickName || state.userInfo.name || '用户'
    },
    avatar: (state) => {
      if (!state.userInfo) return ''
      return state.userInfo.avatarUrl || state.userInfo.avatar || ''
    },
    needUpdateUserInfo: (state) => {
      if (!state.loginTime) return true
      const now = Date.now()
      const dayInMs = 24 * 60 * 60 * 1000
      return (now - state.loginTime) > dayInMs
    }
  },
  actions: {
    setUserInfo(userInfo) {
      this.userInfo = userInfo
      this.isLoggedIn = true
      this.isGuestMode = false
      this.loginTime = Date.now()
      if (!this.userInfo.nickName && this.userInfo.username) {
        this.userInfo.nickName = this.userInfo.username
      }
      if (userInfo.token) {
        uni.setStorageSync('token', userInfo.token)
      }
      uni.setStorageSync('userInfo', this.userInfo)
      uni.setStorageSync('isLoggedIn', true)
      uni.setStorageSync('loginTime', this.loginTime)
      uni.removeStorageSync('isGuestMode')
    },
    setGuestMode() {
      this.isGuestMode = true
      this.isLoggedIn = false
      this.userInfo = null
      this.loginTime = null
      uni.setStorageSync('isGuestMode', true)
      uni.removeStorageSync('userInfo')
      uni.removeStorageSync('isLoggedIn')
      uni.removeStorageSync('loginTime')
    },
    clearUserInfo() {
      this.userInfo = null
      this.isLoggedIn = false
      this.isGuestMode = false
      this.loginTime = null
      uni.removeStorageSync('userInfo')
      uni.removeStorageSync('isLoggedIn')
      uni.removeStorageSync('isGuestMode')
      uni.removeStorageSync('loginTime')
      uni.removeStorageSync('token')
    },
    restoreFromStorage() {
      try {
        const userInfo = uni.getStorageSync('userInfo')
        const isLoggedIn = uni.getStorageSync('isLoggedIn')
        const isGuestMode = uni.getStorageSync('isGuestMode')
        const loginTime = uni.getStorageSync('loginTime')
        if (userInfo && isLoggedIn) {
          this.userInfo = userInfo
          if (!this.userInfo.nickName && this.userInfo.username) {
            this.userInfo.nickName = this.userInfo.username
            uni.setStorageSync('userInfo', this.userInfo)
          }
          this.isLoggedIn = true
          this.loginTime = loginTime || Date.now()
        } else if (isGuestMode) {
          this.isGuestMode = true
        }
      } catch (e) {
        console.error('恢复用户状态失败:', e)
      }
    },
    checkLoginStatus(redirectUrl = '/pages/role-user/home') {
      if (!this.isLoggedIn && !this.isGuestMode) {
        uni.navigateTo({
          url: '/pages/auth/login?role=user'
        })
        return false
      }
      return true
    },
    logout() {
      this.clearUserInfo()
      uni.showToast({ title: '已退出登录', icon: 'success' })
      setTimeout(() => {
        uni.switchTab({ url: '/pages/role-user/home' })
      }, 300)
    }
  }
})


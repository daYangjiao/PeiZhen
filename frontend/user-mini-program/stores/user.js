import { defineStore } from 'pinia'

// 用户状态管理
export const useUserStore = defineStore('user', {
  state: () => ({
    // 用户信息
    userInfo: null,
    // 是否已登录
    isLoggedIn: false,
    // 是否为游客模式
    isGuestMode: false,
    // 登录时间戳
    loginTime: null
  }),

  getters: {
    // 获取用户显示名称
    displayName: (state) => {
      if (!state.userInfo) return ''
      return state.userInfo.nickName || state.userInfo.name || '用户'
    },
    
    // 获取用户头像
    avatar: (state) => {
      if (!state.userInfo) return ''
      return state.userInfo.avatarUrl || state.userInfo.avatar || ''
    },
    
    // 检查是否需要更新用户信息（超过24小时）
    needUpdateUserInfo: (state) => {
      if (!state.loginTime) return true
      const now = Date.now()
      const dayInMs = 24 * 60 * 60 * 1000
      return (now - state.loginTime) > dayInMs
    }
  },

  actions: {
    // 设置用户信息
    setUserInfo(userInfo) {
      console.log('保存前的用户信息:', JSON.stringify(userInfo))
      this.userInfo = userInfo
      this.isLoggedIn = true
      this.isGuestMode = false
      this.loginTime = Date.now()
      
      // 确保nickName字段存在
      if (!this.userInfo.nickName && this.userInfo.username) {
        this.userInfo.nickName = this.userInfo.username
        console.log('在store中设置nickName:', this.userInfo.nickName)
      }
      
      // 单独存储token（如果存在）
      if (userInfo.token) {
        uni.setStorageSync('token', userInfo.token)
        console.log('Token已存储')
      }
      
      // 持久化存储
      uni.setStorageSync('userInfo', this.userInfo)
      uni.setStorageSync('isLoggedIn', true)
      uni.setStorageSync('loginTime', this.loginTime)
      uni.removeStorageSync('isGuestMode')
      
      console.log('保存后的用户信息:', JSON.stringify(this.userInfo))
      console.log('displayName:', this.displayName)
    },

    // 设置游客模式
    setGuestMode() {
      this.isGuestMode = true
      this.isLoggedIn = false
      this.userInfo = null
      this.loginTime = null
      
      // 持久化存储
      uni.setStorageSync('isGuestMode', true)
      uni.removeStorageSync('userInfo')
      uni.removeStorageSync('isLoggedIn')
      uni.removeStorageSync('loginTime')
    },

    // 清除用户信息（退出登录）
    clearUserInfo() {
      this.userInfo = null
      this.isLoggedIn = false
      this.isGuestMode = false
      this.loginTime = null
      
      // 清除持久化存储
      uni.removeStorageSync('userInfo')
      uni.removeStorageSync('isLoggedIn')
      uni.removeStorageSync('isGuestMode')
      uni.removeStorageSync('loginTime')
      uni.removeStorageSync('token') // 同时清除token
    },

    // 从本地存储恢复状态
    restoreFromStorage() {
      try {
        const userInfo = uni.getStorageSync('userInfo')
        const isLoggedIn = uni.getStorageSync('isLoggedIn')
        const isGuestMode = uni.getStorageSync('isGuestMode')
        const loginTime = uni.getStorageSync('loginTime')
        
        console.log('从存储恢复的用户信息:', JSON.stringify(userInfo))
        
        if (userInfo && isLoggedIn) {
          this.userInfo = userInfo
          
          // 确保nickName字段存在
          if (!this.userInfo.nickName && this.userInfo.username) {
            this.userInfo.nickName = this.userInfo.username
            console.log('恢复时设置nickName:', this.userInfo.nickName)
            // 更新存储
            uni.setStorageSync('userInfo', this.userInfo)
          }
          
          this.isLoggedIn = true
          this.loginTime = loginTime || Date.now()
          console.log('恢复后的displayName:', this.displayName)
        } else if (isGuestMode) {
          this.isGuestMode = true
        }
      } catch (error) {
        console.error('恢复用户状态失败:', error)
      }
    },

    // 微信登录
    async wxLogin() {
      try {
        // 检查登录状态
        const loginRes = await uni.login({
          provider: 'weixin'
        })
        
        if (!loginRes[1] || !loginRes[1].code) {
          throw new Error('获取登录凭证失败')
        }
        
        // 这里应该调用后端API验证code并获取用户信息
        // 暂时模拟登录成功
        const mockUserInfo = {
          openid: 'mock_openid_' + Date.now(),
          nickName: '微信用户',
          avatarUrl: '/static/logo.png'
        }
        
        this.setUserInfo(mockUserInfo)
        
        uni.showToast({
          title: '登录成功',
          icon: 'success'
        })
        
        return true
      } catch (error) {
        console.error('微信登录失败:', error)
        uni.showToast({
          title: '登录失败，请重试',
          icon: 'none'
        })
        return false
      }
    },

    // 获取用户详细信息
    async getUserProfile() {
      try {
        const profileRes = await uni.getUserProfile({
          desc: '用于完善用户资料'
        })
        
        if (profileRes[1] && profileRes[1].userInfo) {
          const userInfo = {
            ...this.userInfo,
            ...profileRes[1].userInfo
          }
          this.setUserInfo(userInfo)
          return userInfo
        }
        
        return null
      } catch (error) {
        console.error('获取用户信息失败:', error)
        return null
      }
    },

    // 检查登录状态并跳转
    checkLoginStatus(redirectUrl = '/pages/index/index') {
      if (!this.isLoggedIn && !this.isGuestMode) {
        uni.navigateTo({
          url: '/subpkg/auth/login?redirect=' + encodeURIComponent(redirectUrl)
        })
        return false
      }
      return true
    },

    // 退出登录
    logout() {
      this.clearUserInfo()
      uni.showToast({
        title: '已退出登录',
        icon: 'success'
      })
      
      // 跳转到登录页
      uni.reLaunch({
        url: '/subpkg/auth/login'
      })
    }
  }
})
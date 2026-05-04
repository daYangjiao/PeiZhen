import { defineStore } from 'pinia'
import { get } from '@/utils/api.js'
import { useMessageStore } from '@/stores/message'
import { normalizeQualificationStatus } from '@/utils/qualification.mjs'

const createDefaultAttendantInfo = () => ({
  id: null,
  name: '',
  phone: '',
  avatarUrl: '',
  avatar: '',
  certificate: '',
  score: null,
  introduction: '',
  professionalField: '',
  experienceYears: 0,
  hospitalName: '',
  qualificationStatusCode: 3,
  qualificationStatusText: '待补充',
  qualificationFailReason: '',
  idCardUploaded: false,
  practiceCertUploaded: false,
  healthCertUploaded: false,
  idCardFileUrl: '',
  idCardFrontFileUrl: '',
  idCardFrontScanFileUrl: '',
  idCardBackFileUrl: '',
  idCardBackScanFileUrl: '',
  practiceCertFileUrl: '',
  practiceCertScanFileUrl: '',
  healthCertFileUrl: '',
  healthCertScanFileUrl: '',
  practiceCertExpireDate: '',
  healthCertExpireDate: '',
  practiceCertExpired: false,
  healthCertExpired: false,
  qualificationCompleteness: 0,
  canAcceptOrders: false,
  qualificationBlockReason: '',
  qualificationPopupRequired: false,
  recentQualificationLogs: [],
  todayService: 0,
  monthService: 0,
  totalIncome: 0,
  totalEarnings: 0,
  praiseRate: 0,
  evaluationCount: 0,
  balance: 0
})

export const useUserStore = defineStore('user', {
  state: () => ({
    userInfo: null,
    isLoggedIn: false,
    isGuestMode: false,
    loginTime: null,
    attendantInfo: createDefaultAttendantInfo(),
    attendantLoading: false
  }),

  getters: {
    displayName: (state) => {
      if (!state.userInfo) return ''
      return state.userInfo.nickName || state.userInfo.name || state.userInfo.phone || '用户'
    },
    avatar: (state) => {
      if (!state.userInfo) return ''
      return state.userInfo.avatar || state.userInfo.avatarUrl || ''
    },
    needUpdateUserInfo: (state) => {
      if (!state.loginTime) return true
      const now = Date.now()
      const dayInMs = 24 * 60 * 60 * 1000
      return (now - state.loginTime) > dayInMs
    },
    todayService: (state) => Number(state.attendantInfo.todayService || 0),
    monthService: (state) => Number(state.attendantInfo.monthService || 0),
    totalIncome: (state) => Number(state.attendantInfo.totalIncome || 0),
    praiseRate: (state) => Number(state.attendantInfo.praiseRate || 0),
    balance: (state) => Number(state.attendantInfo.balance || 0),
    qualificationStatusText: (state) => state.attendantInfo.qualificationStatusText || '待补充'
  },

  actions: {
    setUserInfo(userInfo) {
      const previousUserInfo = this.userInfo || uni.getStorageSync('userInfo') || {}
      this.userInfo = { ...userInfo }
      this.isLoggedIn = true
      this.isGuestMode = false
      this.loginTime = Date.now()
      const previousName = previousUserInfo.name || ''
      const nextName = this.userInfo.name || ''
      const isEscortUser = Number(this.userInfo.userType || previousUserInfo.userType || 0) === 1
      if (!isEscortUser && nextName && nextName !== previousName) {
        this.userInfo.nickName = nextName
      } else if (!this.userInfo.nickName) {
        this.userInfo.nickName = this.userInfo.name || this.userInfo.phone || '用户'
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
      this.clearAttendantInfo()
      uni.setStorageSync('isGuestMode', true)
      uni.removeStorageSync('userInfo')
      uni.removeStorageSync('isLoggedIn')
      uni.removeStorageSync('loginTime')
    },

    clearUserInfo() {
      const messageStore = useMessageStore()
      this.userInfo = null
      this.isLoggedIn = false
      this.isGuestMode = false
      this.loginTime = null
      this.clearAttendantInfo()
      uni.removeStorageSync('userInfo')
      uni.removeStorageSync('isLoggedIn')
      uni.removeStorageSync('isGuestMode')
      uni.removeStorageSync('loginTime')
      uni.removeStorageSync('token')
      messageStore.clearAllUnread()
      messageStore.updateTabBarBadge()
      uni.$emit('session:changed')
    },

    restoreFromStorage() {
      try {
        const userInfo = uni.getStorageSync('userInfo')
        const isLoggedIn = uni.getStorageSync('isLoggedIn')
        const isGuestMode = uni.getStorageSync('isGuestMode')
        const loginTime = uni.getStorageSync('loginTime')
        if (userInfo && isLoggedIn) {
          this.userInfo = userInfo
          if (!this.userInfo.nickName) {
            this.userInfo.nickName = this.userInfo.name || this.userInfo.phone || '用户'
            uni.setStorageSync('userInfo', this.userInfo)
          }
          this.isLoggedIn = true
          this.loginTime = loginTime || Date.now()
          this.restoreAttendantInfo()
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
    },

    normalizeAttendantInfo(raw = {}) {
      const base = createDefaultAttendantInfo()
      const next = { ...base, ...raw }

      const income = Number(next.totalIncome ?? next.totalEarnings ?? 0)
      next.totalIncome = Number.isFinite(income) ? income : 0
      next.totalEarnings = Number.isFinite(income) ? income : 0
      next.todayService = Number(next.todayService || 0)
      next.monthService = Number(next.monthService || 0)
      next.praiseRate = Number(next.praiseRate || 0)
      next.evaluationCount = Number(next.evaluationCount || 0)
      next.balance = Number(next.balance || 0)
      next.score = next.score === null || next.score === undefined || next.score === '' ? null : Number(next.score)
      next.experienceYears = Number(next.experienceYears || 0)
      next.qualificationStatusCode = Number(next.qualificationStatusCode ?? 3)
      next.qualificationStatusText = next.qualificationStatusText || '待补充'
      next.qualificationFailReason = next.qualificationFailReason || ''
      Object.assign(next, normalizeQualificationStatus(next))
      next.practiceCertExpireDate = next.practiceCertExpireDate || ''
      next.healthCertExpireDate = next.healthCertExpireDate || ''
      next.practiceCertExpired = next.practiceCertExpired === true || Number(next.practiceCertExpired || 0) === 1
      next.healthCertExpired = next.healthCertExpired === true || Number(next.healthCertExpired || 0) === 1
      next.qualificationCompleteness = Number(next.qualificationCompleteness || 0)
      next.canAcceptOrders = next.canAcceptOrders === true || Number(next.canAcceptOrders || 0) === 1
      next.qualificationBlockReason = next.qualificationBlockReason || ''
      next.qualificationPopupRequired = next.qualificationPopupRequired === true || Number(next.qualificationPopupRequired || 0) === 1
      next.recentQualificationLogs = Array.isArray(next.recentQualificationLogs) ? next.recentQualificationLogs : []
      return next
    },

    setAttendantInfo(payload = {}) {
      this.attendantInfo = this.normalizeAttendantInfo(payload)
      uni.setStorageSync('attendantInfo', this.attendantInfo)
      this.syncAttendantSessionProfile(this.attendantInfo)
    },

    syncAttendantSessionProfile(payload = {}) {
      const current = this.userInfo || uni.getStorageSync('userInfo') || {}
      if (!current || !current.id) return null

      const next = {
        ...current,
        name: payload.name || current.name || '',
        phone: payload.phone || current.phone || '',
        avatar: payload.avatarUrl || payload.avatar || current.avatar || '',
        avatarUrl: payload.avatarUrl || payload.avatar || current.avatarUrl || current.avatar || ''
      }

      if (!next.nickName) {
        next.nickName = next.name || next.phone || '用户'
      }

      this.userInfo = next
      uni.setStorageSync('userInfo', next)
      uni.$emit('user:profile-updated', next)
      return next
    },

    restoreAttendantInfo() {
      const cached = uni.getStorageSync('attendantInfo')
      if (cached) {
        this.setAttendantInfo(cached)
        return true
      }
      return false
    },

    async fetchAttendantProfile(userId) {
      if (!userId) return null
      if (this.attendantLoading) return this.attendantInfo

      this.attendantLoading = true
      try {
        const res = await get(`/attendant/profile/${userId}`)
        if (res.code === 200 && res.data) {
          this.setAttendantInfo(res.data)
          return this.attendantInfo
        }
      } catch (error) {
        console.error('获取陪诊师资料失败:', error)
      } finally {
        this.attendantLoading = false
      }
      return this.attendantInfo
    },

    clearAttendantInfo() {
      this.attendantInfo = createDefaultAttendantInfo()
      this.attendantLoading = false
      uni.removeStorageSync('attendantInfo')
    }
  }
})

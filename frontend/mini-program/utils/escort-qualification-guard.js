import { useUserStore } from '@/stores/user'

const userIdFromStorage = () => {
  const userInfo = uni.getStorageSync('userInfo') || {}
  return userInfo.id || null
}

export const resolveQualificationGate = (profile = {}) => {
  const status = Number(profile.qualificationStatusCode || 0)
  const reason = profile.qualificationBlockReason || profile.qualificationFailReason || ''
  if (profile.canAcceptOrders === true) {
    return { allowed: true, state: 'passed', title: '资质已通过', message: '' }
  }
  if (status === 0) {
    return {
      allowed: false,
      state: 'pending',
      title: '资质审核中',
      message: reason || '平台正在审核你的入驻资料，审核通过后即可查看接单大厅。'
    }
  }
  if (status === 2) {
    return {
      allowed: false,
      state: 'blocked',
      title: '账号暂不可接单',
      message: reason || '账号已封禁，请联系平台客服处理。'
    }
  }
  if (profile.practiceCertExpired || profile.healthCertExpired) {
    return {
      allowed: false,
      state: 'expired',
      title: '证件已过期',
      message: reason || '证件已过期，请更新资质后重新提交审核。'
    }
  }
  if (status === 3) {
    return {
      allowed: false,
      state: 'rejected',
      title: '资质审核未通过',
      message: reason || '资质审核未通过，请修改后重新提交。'
    }
  }
  return {
    allowed: false,
    state: 'incomplete',
    title: '资质待补充',
    message: reason || '请补全身份证、执业证书、健康证和证件有效期后提交审核。'
  }
}

export const refreshEscortQualificationGate = async () => {
  const userStore = useUserStore()
  const uid = userIdFromStorage()
  if (!uid) {
    return { allowed: false, state: 'login', title: '请先登录', message: '当前操作需要登录。' }
  }
  const profile = await userStore.fetchAttendantProfile(uid)
  return resolveQualificationGate(profile || userStore.attendantInfo)
}

export const guardEscortHallAccess = async ({ showPopup = true, redirectOnConfirm = true } = {}) => {
  const gate = await refreshEscortQualificationGate()
  if (gate.allowed || gate.state === 'pending') {
    return gate
  }
  if (showPopup) {
    uni.$emit('escort-qualification-gate:show', { gate, redirectOnConfirm })
  }
  return gate
}

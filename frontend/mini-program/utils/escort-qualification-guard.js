import { useUserStore } from '@/stores/user'
import {
  createQualificationPromptTracker,
  resolveQualificationGate,
  shouldPromptQualificationGate,
} from '@/utils/escort-qualification-gate.mjs'

const promptTracker = createQualificationPromptTracker()

const userIdFromStorage = () => {
  const userInfo = uni.getStorageSync('userInfo') || {}
  return userInfo.id || null
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
  if (gate.allowed) {
    return gate
  }

  if (showPopup && shouldPromptQualificationGate({ userId: userIdFromStorage(), gate, tracker: promptTracker })) {
    uni.$emit('escort-qualification-gate:show', { gate, redirectOnConfirm })
  }
  return gate
}

import { useSessionStore } from '@/stores/session'
import { useUserStore } from '@/stores/user'
import { useMessageStore } from '@/stores/message'
import { ensureChatConnected } from '@/utils/ws-manager.js'

const syncMessageStatus = async (messageStore) => {
  try {
    ensureChatConnected()
    await messageStore.initMessageStatus()
    messageStore.updateTabBarBadge()
    messageStore.scheduleRefreshUnreadCounts(600)
    uni.$emit('session:changed')
  } catch {}
}

export const completeLoginSession = async ({ role, token, userInfo }) => {
  const session = useSessionStore()
  const userStore = useUserStore()
  const messageStore = useMessageStore()

  if (role === 'escort') {
    if (userInfo.userType !== 1) {
      throw new Error('该账号不是陪诊师')
    }
    session.setSession({ role: 'escort', token, userInfo })
    uni.setStorageSync('userInfo', userInfo)
    await syncMessageStatus(messageStore)
    return '/pages/role-escort/hall'
  }

  if (userInfo.userType === 1) {
    throw new Error('该账号是陪诊师，请切换到陪诊师登录')
  }

  session.setSession({ role: 'user', token, userInfo })
  userStore.setUserInfo({ ...userInfo, token })
  await syncMessageStatus(messageStore)
  return '/pages/role-user/home'
}

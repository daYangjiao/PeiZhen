<script>
import { useSessionStore } from '@/stores/session'
import { initWebSockets, ensureChatConnected } from '@/utils/ws-manager.js'
import { useMessageStore } from '@/stores/message'
import { checkAppUpgrade } from '@/utils/app-upgrade'

const bindGlobalMessageSync = () => {
  if (uni.__globalMessageSyncBound) return
  uni.__globalMessageSyncBound = true

  const messageStore = useMessageStore()
  const session = useSessionStore()
  const refresh = (force = false) => {
    session.restoreFromStorage()
    if (!session.isLoggedIn) {
      messageStore.clearAllUnread()
      messageStore.updateTabBarBadge()
      return
    }
    if (force) {
      messageStore.refreshUnreadCounts()
      messageStore.scheduleRefreshUnreadCounts(600)
      messageStore.scheduleRefreshUnreadCounts(1800)
      return
    }
    messageStore.scheduleRefreshUnreadCounts(120)
  }

  uni.$on('chat:message', (message) => {
    if (!message) return
    if (message.msgType === 3 || message.content === 'READ_RECEIPT' || message.type === 'READ_RECEIPT') return
    messageStore.applyRealtimeUnreadFromMessage(message)
    refresh()
  })

  uni.$on('session:changed', () => refresh(true))
  refresh(true)
}

export default {
  async onLaunch() {
    console.log('合并小程序 App Launch')
    try {
      const session = useSessionStore()
      session.restoreFromStorage()
      const messageStore = useMessageStore()
      await messageStore.initMessageStatus()
      initWebSockets()
      bindGlobalMessageSync()
      checkAppUpgrade()
    } catch (e) {
      console.warn('app init failed', e)
    }
  },
  onShow() {
    ensureChatConnected()
    const messageStore = useMessageStore()
    messageStore.scheduleRefreshUnreadCounts(120)
  }
}
</script>

<style>
/* 全局样式占位 */
</style>

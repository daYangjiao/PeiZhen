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
    if (String(message.type || '') === 'READ_RECEIPT' || Number(message.msgType || 0) === 99) return
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
    checkAppUpgrade()
  }
}
</script>

<style>
/* 全局样式占位 */

/* #ifdef H5 */
html,
body,
#app,
uni-app,
uni-page,
uni-page-wrapper,
uni-page-body {
  width: 100%;
  max-width: 100%;
  overflow-x: hidden !important;
}

*,
*::before,
*::after {
  box-sizing: border-box;
}

uni-page-body {
  position: relative;
}

html.platform-ios {
  --ios-tabbar-base-height: 50px;
  --ios-tabbar-safe-bottom: max(env(safe-area-inset-bottom), 16px);
}

html.platform-ios .uni-tabbar-bottom .uni-tabbar {
  padding-bottom: var(--ios-tabbar-safe-bottom) !important;
  min-height: calc(var(--ios-tabbar-base-height) + var(--ios-tabbar-safe-bottom));
}

html.platform-ios .uni-app--showtabbar uni-page-wrapper {
  height: calc(100% - var(--ios-tabbar-base-height) - var(--ios-tabbar-safe-bottom)) !important;
}

html.platform-ios .uni-app--showtabbar uni-page-wrapper:after {
  height: calc(var(--ios-tabbar-base-height) + var(--ios-tabbar-safe-bottom)) !important;
}
/* #endif */
</style>

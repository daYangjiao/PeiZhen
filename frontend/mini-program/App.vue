<script>
import { useSessionStore } from '@/stores/session'
import { initWebSockets, ensureChatConnected } from '@/utils/ws-manager.js'
import { useMessageStore } from '@/stores/message'

export default {
  async onLaunch() {
    console.log('合并小程序 App Launch')
    try {
      const session = useSessionStore()
      session.restoreFromStorage()
      const messageStore = useMessageStore()
      await messageStore.initMessageStatus()
      initWebSockets()
    } catch (e) {
      console.warn('app init failed', e)
    }
  },
  onShow() {
    ensureChatConnected()
  }
}
</script>

<style>
/* 全局样式占位 */
</style>


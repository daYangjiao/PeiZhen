<script>
	import { connectChatSocket } from '@/utils/chat-websocket.js'
	import { startupTokenCheck, startPeriodicTokenCheck } from '@/utils/token-validator.js'
	import { useMessageStore } from '@/stores/message.js'

	export default {
		onLaunch: async function() {
			console.log('陪诊师端App Launch')
			
			// 启动时进行token检测
			try {
				await startupTokenCheck()
			} catch (error) {
				console.error('启动时token检测失败:', error)
			}
			
			// 初始化消息状态
			const messageStore = useMessageStore()
			await messageStore.initMessageStatus()
			
			// 连接WebSocket
			connectChatSocket()
			
			// 启动定期token检查（每30分钟检查一次）
			startPeriodicTokenCheck(30 * 60 * 1000)
		},
		onShow: function() {
			console.log('陪诊师端App Show')
			connectChatSocket()
			// 不在app显示时自动刷新消息状态，避免红点消失
			console.log('陪诊师端App显示，保持当前消息状态')
		},
		onHide: function() {
			console.log('陪诊师端App Hide')
		}
	}
</script>

<style lang="scss">
	/*每个页面公共css */
	@import "@/common/common.scss";
</style>

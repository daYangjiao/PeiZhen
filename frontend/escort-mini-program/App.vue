<script>
	import { connectChatSocket, addChatListener } from '@/utils/chat-websocket.js'
	import { startupTokenCheck, startPeriodicTokenCheck } from '@/utils/token-validator.js'
	import { useMessageStore } from '@/stores/message.js'

	function setupGlobalChatListener() {
		addChatListener((msg) => {
			if (!msg) return
			const userInfo = uni.getStorageSync('userInfo')
			const currentUserId = userInfo?.id
			if (!currentUserId) return

			const isReadReceipt = (msg.type === 'READ_RECEIPT' || msg.content === 'READ_RECEIPT' || msg.msgType == 3) && msg.senderId && msg.receiverId
			if (isReadReceipt) {
				if (msg.receiverId === currentUserId) {
					useMessageStore().decrementUnread(msg.senderId)
				}
				return
			}
			if (msg.senderId === 0 && msg.receiverId === currentUserId) {
				useMessageStore().incrementSystemUnread()
				return
			}
			if (msg.senderId && msg.receiverId) {
				const contactId = msg.senderId === currentUserId ? msg.receiverId : msg.senderId
				if (contactId && contactId > 0) {
					useMessageStore().incrementUnread(contactId)
				}
			}
		})
	}

	export default {
		onLaunch: async function() {
			console.log('陪诊师端App Launch')
			
			try {
				await startupTokenCheck()
			} catch (error) {
				console.error('启动时token检测失败:', error)
			}
			
			const messageStore = useMessageStore()
			await messageStore.initMessageStatus()
			
			connectChatSocket()
			setupGlobalChatListener()
			
			// 启动定期token检查（每30分钟检查一次）
			startPeriodicTokenCheck(30 * 60 * 1000)
		},
		onShow: async function() {
			console.log('陪诊师端App Show')
			connectChatSocket()
			try {
				const messageStore = useMessageStore()
				await messageStore.refreshUnreadCounts()
			} catch (e) {
				console.error('刷新未读消息失败', e)
			}
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

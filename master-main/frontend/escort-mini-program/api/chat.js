// 聊天消息相关API
import { get, post, put } from '../utils/api.js'

// 发送消息
export const sendMessage = (data) => {
	return post('/chat/send', {
		toUserId: data.toUserId, // 接收者ID
		messageType: data.messageType, // 消息类型：text, image, voice, location
		content: data.content, // 消息内容
		orderId: data.orderId, // 关联订单ID
		extData: data.extData // 扩展数据（如位置信息、图片URL等）
	})
}

// 获取聊天历史消息
export const getChatHistory = (params) => {
	return get('/chat/history', {
		userId: params.userId, // 对方用户ID
		orderId: params.orderId, // 订单ID
		page: params.page || 1,
		pageSize: params.pageSize || 20,
		lastMessageId: params.lastMessageId // 最后一条消息ID，用于分页
	})
}

// 获取会话列表
export const getConversationList = (params = {}) => {
	return get('/chat/conversations', {
		page: params.page || 1,
		pageSize: params.pageSize || 20
	})
}

// 标记消息为已读
export const markMessageAsRead = (messageIds) => {
	return put('/chat/read', {
		messageIds: Array.isArray(messageIds) ? messageIds : [messageIds]
	})
}

// 标记会话为已读
export const markConversationAsRead = (userId, orderId) => {
	return put('/chat/conversation/read', {
		userId,
		orderId
	})
}

// 获取未读消息数量
export const getUnreadCount = () => {
	return get('/chat/unread-count')
}

// 删除消息
export const deleteMessage = (messageId) => {
	return put(`/chat/message/${messageId}/delete`)
}

// 撤回消息
export const recallMessage = (messageId) => {
	return put(`/chat/message/${messageId}/recall`)
}

// 搜索聊天记录
export const searchMessages = (params) => {
	return get('/chat/search', {
		keyword: params.keyword,
		userId: params.userId,
		orderId: params.orderId,
		messageType: params.messageType,
		startTime: params.startTime,
		endTime: params.endTime,
		page: params.page || 1,
		pageSize: params.pageSize || 20
	})
}

// 上传聊天文件（图片、语音等）
export const uploadChatFile = (filePath, fileType) => {
	return new Promise((resolve, reject) => {
		uni.uploadFile({
			url: '/chat/upload',
			filePath,
			name: 'file',
			formData: {
				fileType
			},
			success: (response) => {
				try {
					const data = JSON.parse(response.data)
					if (data.code === 200) {
						resolve(data)
					} else {
						reject(data)
					}
				} catch (error) {
					reject(error)
				}
			},
			fail: reject
		})
	})
}

// 获取在线状态
export const getOnlineStatus = (userIds) => {
	return post('/chat/online-status', {
		userIds: Array.isArray(userIds) ? userIds : [userIds]
	})
}

// 更新在线状态
export const updateOnlineStatus = (status) => {
	return put('/chat/online-status', {
		status // online, offline, busy
	})
}

// 发送正在输入状态
export const sendTypingStatus = (toUserId, isTyping) => {
	return post('/chat/typing', {
		toUserId,
		isTyping
	})
}
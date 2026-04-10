// 用户端消息通信相关API
import request from '@/utils/api'

// 获取消息列表
export function getMessageList() {
  return request({
    url: '/api/messages/list',
    method: 'GET'
  })
}

// 获取聊天记录
export function getChatHistory(userId) {
  return request({
    url: `/api/messages/history/${userId}`,
    method: 'GET'
  })
}

// 发送消息
export function sendMessage(data) {
  return request({
    url: '/api/messages/send',
    method: 'POST',
    data
  })
}

// 标记消息已读
export function markAsRead(messageId) {
  return request({
    url: `/api/messages/read/${messageId}`,
    method: 'PUT'
  })
}

// 删除消息
export function deleteMessage(messageId) {
  return request({
    url: `/api/messages/${messageId}`,
    method: 'DELETE'
  })
}

// 获取会话列表
export const getConversationList = (params = {}) => {
	return get('/customer/messages/conversations', {
		page: params.page || 1,
		pageSize: params.pageSize || 20,
		status: params.status // 会话状态筛选
	})
}

// 标记消息为已读
export const markMessageAsRead = (messageIds) => {
	return put('/customer/messages/read', {
		messageIds: Array.isArray(messageIds) ? messageIds : [messageIds]
	})
}

// 标记会话为已读
export const markConversationAsRead = (companionId, orderId) => {
	return put('/customer/messages/conversation/read', {
		companionId,
		orderId
	})
}

// 获取未读消息数量
export const getUnreadCount = () => {
	return get('/customer/messages/unread-count')
}

// 撤回消息
export const recallMessage = (messageId) => {
	return put(`/customer/messages/${messageId}/recall`)
}

// 搜索聊天记录
export const searchMessages = (params) => {
	return get('/customer/messages/search', {
		keyword: params.keyword,
		companionId: params.companionId,
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
			url: '/customer/messages/upload',
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

// 获取陪诊师在线状态
export const getCompanionOnlineStatus = (companionIds) => {
	return post('/customer/messages/online-status', {
		companionIds: Array.isArray(companionIds) ? companionIds : [companionIds]
	})
}

// 发送正在输入状态
export const sendTypingStatus = (companionId, isTyping) => {
	return post('/customer/messages/typing', {
		companionId,
		isTyping
	})
}

// 获取系统消息列表
export const getSystemMessages = (params = {}) => {
	return get('/customer/messages/system', {
		type: params.type, // 消息类型筛选
		status: params.status, // 读取状态筛选
		page: params.page || 1,
		pageSize: params.pageSize || 20
	})
}

// 标记系统消息为已读
export const markSystemMessageAsRead = (messageIds) => {
	return put('/customer/messages/system/read', {
		messageIds: Array.isArray(messageIds) ? messageIds : [messageIds]
	})
}

// 删除系统消息
export const deleteSystemMessage = (messageIds) => {
	return put('/customer/messages/system/delete', {
		messageIds: Array.isArray(messageIds) ? messageIds : [messageIds]
	})
}

// 获取消息设置
export const getMessageSettings = () => {
	return get('/customer/messages/settings')
}

// 更新消息设置
export const updateMessageSettings = (settings) => {
	return put('/customer/messages/settings', {
		pushEnabled: settings.pushEnabled, // 是否开启推送
		soundEnabled: settings.soundEnabled, // 是否开启声音
		vibrationEnabled: settings.vibrationEnabled, // 是否开启震动
		quietHours: settings.quietHours, // 免打扰时间段
		showPreview: settings.showPreview // 是否显示消息预览
	})
}

// 举报消息
export const reportMessage = (messageId, data) => {
	return post(`/customer/messages/${messageId}/report`, {
		reason: data.reason,
		description: data.description,
		evidence: data.evidence
	})
}

// 拉黑用户
export const blockUser = (companionId) => {
	return post(`/customer/messages/block`, {
		companionId
	})
}

// 取消拉黑用户
export const unblockUser = (companionId) => {
	return post(`/customer/messages/unblock`, {
		companionId
	})
}

// 获取黑名单列表
export const getBlockList = (params = {}) => {
	return get('/customer/messages/blocks', {
		page: params.page || 1,
		pageSize: params.pageSize || 20
	})
}

// 清空聊天记录
export const clearChatHistory = (companionId, orderId) => {
	return put('/customer/messages/clear', {
		companionId,
		orderId
	})
}

// 导出聊天记录
export const exportChatHistory = (companionId, orderId, format = 'txt') => {
	return post('/customer/messages/export', {
		companionId,
		orderId,
		format // txt, pdf, excel
	})
}
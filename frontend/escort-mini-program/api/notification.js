// 系统通知相关API
import { get, post, put } from '../utils/api.js'

// 获取通知列表
export const getNotificationList = (params = {}) => {
	return get('/notifications', {
		page: params.page || 1,
		pageSize: params.pageSize || 20,
		type: params.type, // 通知类型筛选
		status: params.status // 读取状态筛选
	})
}

// 获取通知详情
export const getNotificationDetail = (notificationId) => {
	return get(`/notifications/${notificationId}`)
}

// 标记通知为已读
export const markNotificationAsRead = (notificationIds) => {
	return put('/notifications/read', {
		notificationIds: Array.isArray(notificationIds) ? notificationIds : [notificationIds]
	})
}

// 标记所有通知为已读
export const markAllNotificationsAsRead = () => {
	return put('/notifications/read-all')
}

// 删除通知
export const deleteNotification = (notificationIds) => {
	return put('/notifications/delete', {
		notificationIds: Array.isArray(notificationIds) ? notificationIds : [notificationIds]
	})
}

// 清空所有通知
export const clearAllNotifications = () => {
	return put('/notifications/clear-all')
}

// 获取未读通知数量
export const getUnreadNotificationCount = () => {
	return get('/notifications/unread-count')
}

// 获取通知设置
export const getNotificationSettings = () => {
	return get('/notifications/settings')
}

// 更新通知设置
export const updateNotificationSettings = (settings) => {
	return put('/notifications/settings', {
		orderNotification: settings.orderNotification, // 订单通知
		chatNotification: settings.chatNotification, // 聊天消息通知
		systemNotification: settings.systemNotification, // 系统通知
		promotionNotification: settings.promotionNotification, // 推广通知
		soundEnabled: settings.soundEnabled, // 声音提醒
		vibrationEnabled: settings.vibrationEnabled, // 震动提醒
		quietHours: settings.quietHours // 免打扰时间段
	})
}

// 发送系统通知（管理员功能）
export const sendSystemNotification = (data) => {
	return post('/notifications/send', {
		title: data.title,
		content: data.content,
		type: data.type,
		targetUsers: data.targetUsers, // 目标用户ID数组
		scheduledTime: data.scheduledTime, // 定时发送时间
		extData: data.extData // 扩展数据
	})
}

// 获取系统公告
export const getSystemAnnouncements = (params = {}) => {
	return get('/notifications/announcements', {
		page: params.page || 1,
		pageSize: params.pageSize || 10,
		status: params.status // 公告状态
	})
}

// 获取推送token（用于推送服务）
export const updatePushToken = (token, platform) => {
	return put('/notifications/push-token', {
		token,
		platform // ios, android, web
	})
}

// 测试推送
export const testPush = () => {
	return post('/notifications/test-push')
}
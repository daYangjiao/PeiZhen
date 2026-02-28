// 订单管理相关API
import { get, post, put } from '../utils/api.js'

// 获取订单列表
export const getOrderList = (params = {}) => {
	return get('/orders', {
		page: params.page || 1,
		pageSize: params.pageSize || 10,
		status: params.status, // 订单状态筛选
		keyword: params.keyword // 搜索关键词
	})
}

// 获取订单详情
export const getOrderDetail = (orderId) => {
	return get(`/orders/${orderId}`)
}

// 接单
export const acceptOrder = (orderId) => {
	return post(`/orders/${orderId}/accept`)
}

// 拒绝订单
export const rejectOrder = (orderId, reason) => {
	return post(`/orders/${orderId}/reject`, {
		reason
	})
}

// 开始服务
export const startService = (orderId) => {
	return put(`/orders/${orderId}/start`)
}

// 完成订单
export const completeOrder = (orderId, data = {}) => {
	return put(`/orders/${orderId}/complete`, {
		summary: data.summary, // 服务总结
		images: data.images, // 服务照片
		notes: data.notes // 备注信息
	})
}

// 取消订单
export const cancelOrder = (orderId, reason) => {
	return put(`/orders/${orderId}/cancel`, {
		reason
	})
}

// 陪诊师端取消订单（待核销/待服务状态）
export const cancelAttendantOrder = (orderId, payload = {}) => {
	const params = []
	if (payload.reason) params.push(`reason=${encodeURIComponent(payload.reason)}`)
	if (payload.penaltyAmount !== undefined) params.push(`penaltyAmount=${encodeURIComponent(payload.penaltyAmount)}`)
	if (payload.refundAmount !== undefined) params.push(`refundAmount=${encodeURIComponent(payload.refundAmount)}`)
	if (payload.penaltyRate !== undefined) params.push(`penaltyRate=${encodeURIComponent(payload.penaltyRate)}`)
	const query = params.length ? `?${params.join('&')}` : ''
	return post(`/attendant/orders/${orderId}/cancel${query}`)
}

// 更新订单状态
export const updateOrderStatus = (orderId, status, data = {}) => {
	return put(`/orders/${orderId}/status`, {
		status,
		...data
	})
}

// 获取可接订单列表（待接单状态）
export const getAvailableOrders = (params = {}) => {
	return get('/orders/available', {
		page: params.page || 1,
		pageSize: params.pageSize || 10,
		location: params.location, // 地理位置筛选
		serviceType: params.serviceType // 服务类型筛选
	})
}

// 获取我的订单统计
export const getOrderStats = () => {
	return get('/orders/stats')
}

// 上传服务进度
export const uploadServiceProgress = (orderId, data) => {
	return post(`/orders/${orderId}/progress`, {
		step: data.step, // 当前步骤
		description: data.description, // 步骤描述
		images: data.images, // 相关图片
		location: data.location, // 当前位置
		timestamp: data.timestamp || Date.now()
	})
}

// 获取服务进度
export const getServiceProgress = (orderId) => {
	return get(`/orders/${orderId}/progress`)
}

// 评价订单
export const rateOrder = (orderId, data) => {
	return post(`/orders/${orderId}/rate`, {
		rating: data.rating, // 评分
		comment: data.comment, // 评价内容
		tags: data.tags // 评价标签
	})
}
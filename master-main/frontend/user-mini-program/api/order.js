// 用户端订单相关API
import { get, post, put } from '../utils/api.js'
import { wxPay } from '../utils/api.js'

// 获取订单列表
export const getOrderList = (params = {}) => {
	return get('/api/orders/user-orders', {
		status: params.status, // 订单状态筛选
		startTime: params.startTime,
		endTime: params.endTime,
		keyword: params.keyword, // 搜索关键词
		page: params.page || 1,
		pageSize: params.pageSize || 10
	})
}

// 获取订单详情
export const getOrderDetail = (orderId) => {
	return get(`/api/orders/${orderId}`)
}

// 创建订单（从预约转换）
export const createOrder = (appointmentId, paymentMethod = 'wxpay') => {
	return post('/api/orders', {
		appointmentId,
		paymentMethod
	})
}

// 取消订单
export const cancelOrder = (orderId, reason) => {
	return put(`/api/orders/${orderId}/cancel`, {
		reason
	})
}

// 删除订单
export const deleteOrder = (orderId) => {
	return del(`/api/orders/${orderId}`)
}

// 确认订单完成
export const confirmOrderComplete = (orderId) => {
	return put(`/api/orders/${orderId}/confirm-complete`)
}

// 申请退款
export const requestRefund = (orderId, data) => {
	return post(`/api/orders/${orderId}/refund`, {
		reason: data.reason,
		description: data.description,
		amount: data.amount, // 退款金额
		evidence: data.evidence // 退款证据
	})
}

// 获取订单支付信息
export const getOrderPayment = (orderId) => {
	return get(`/api/orders/${orderId}/payment`)
}

// 支付订单
export const payOrder = (orderId, paymentMethod = 'wxpay') => {
	return post(`/api/orders/${orderId}/pay`, {
		paymentMethod
	}).then(response => {
		if (response.data.paymentData && paymentMethod === 'wxpay') {
			// 调用微信支付
			return wxPay(response.data.paymentData)
		}
		return response
	})
}

// 查询支付状态
export const checkPaymentStatus = (orderId) => {
	return get(`/api/orders/${orderId}/payment-status`)
}

// 获取订单进度
export const getOrderProgress = (orderId) => {
	return get(`/api/orders/${orderId}/progress`)
}

// 评价订单
export const rateOrder = (orderId, data) => {
	return post(`/api/orders/${orderId}/rate`, {
		serviceRating: data.serviceRating, // 服务评分
		attitudeRating: data.attitudeRating, // 态度评分
		professionalRating: data.professionalRating, // 专业评分
		overallRating: data.overallRating, // 总体评分
		comment: data.comment, // 评价内容
		tags: data.tags, // 评价标签
		images: data.images, // 评价图片
		isAnonymous: data.isAnonymous || false // 是否匿名评价
	})
}

// 获取订单统计
export const getOrderStats = () => {
	return get('/api/orders/stats')
}

// 获取退款记录
export const getRefundRecords = (params = {}) => {
	return get('/api/refunds', {
		status: params.status,
		startTime: params.startTime,
		endTime: params.endTime,
		page: params.page || 1,
		pageSize: params.pageSize || 10
	})
}

// 获取退款详情
export const getRefundDetail = (refundId) => {
	return get(`/api/refunds/${refundId}`)
}

// 撤销退款申请
export const cancelRefund = (refundId) => {
	return put(`/api/refunds/${refundId}/cancel`)
}

// 获取发票信息
export const getInvoiceInfo = (orderId) => {
	return get(`/api/orders/${orderId}/invoice`)
}

// 申请发票
export const requestInvoice = (orderId, data) => {
	return post(`/api/orders/${orderId}/invoice`, {
		type: data.type, // personal: 个人, company: 企业
		title: data.title, // 发票抬头
		taxNumber: data.taxNumber, // 税号（企业发票必填）
		email: data.email, // 接收邮箱
		address: data.address, // 邮寄地址
		phone: data.phone // 联系电话
	})
}

// 获取优惠券列表
export const getCoupons = (params = {}) => {
	return get('/api/coupons', {
		status: params.status, // available: 可用, used: 已使用, expired: 已过期
		serviceType: params.serviceType,
		page: params.page || 1,
		pageSize: params.pageSize || 20
	})
}

// 使用优惠券
export const useCoupon = (orderId, couponId) => {
	return post(`/api/orders/${orderId}/use-coupon`, {
		couponId
	})
}

// 获取可用优惠券
export const getAvailableCoupons = (orderId) => {
	return get(`/api/orders/${orderId}/available-coupons`)
}

// 计算订单价格
export const calculateOrderPrice = (data) => {
	return post('/api/orders/calculate-price', {
		serviceType: data.serviceType,
		duration: data.duration,
		companionId: data.companionId,
		couponId: data.couponId,
		appointmentTime: data.appointmentTime
	})
}

// 获取订单收据
export const getOrderReceipt = (orderId) => {
	return get(`/api/orders/${orderId}/receipt`)
}

// 投诉订单
export const complainOrder = (orderId, data) => {
	return post(`/api/orders/${orderId}/complain`, {
		reason: data.reason,
		description: data.description,
		evidence: data.evidence,
		contactPhone: data.contactPhone
	})
}
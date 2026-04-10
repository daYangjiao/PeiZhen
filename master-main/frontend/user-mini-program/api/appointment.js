// 预约服务相关API
import { get, post, put, del } from '../utils/api.js'

// 搜索陪诊师
export const searchCompanions = (params) => {
	return get('/customer/companions/search', {
		keyword: params.keyword, // 搜索关键词
		latitude: params.latitude, // 纬度
		longitude: params.longitude, // 经度
		radius: params.radius || 5000, // 搜索半径（米）
		serviceType: params.serviceType, // 服务类型
		gender: params.gender, // 性别偏好
		minRating: params.minRating, // 最低评分
		priceRange: params.priceRange, // 价格区间
		availableTime: params.availableTime, // 可用时间
		sortBy: params.sortBy || 'distance', // 排序方式：distance, rating, price
		page: params.page || 1,
		pageSize: params.pageSize || 20
	})
}

// 获取陪诊师详情
export const getCompanionDetail = (companionId) => {
	return get(`/customer/companions/${companionId}`)
}

// 获取陪诊师可用时间
export const getCompanionAvailableTime = (companionId, date) => {
	return get(`/customer/companions/${companionId}/available-time`, {
		date
	})
}

// 获取陪诊师评价列表
export const getCompanionReviews = (companionId, params = {}) => {
	return get(`/customer/companions/${companionId}/reviews`, {
		page: params.page || 1,
		pageSize: params.pageSize || 10,
		rating: params.rating // 评分筛选
	})
}

// 创建预约
export const createAppointment = (data) => {
	return post('/customer/appointments', {
		companionId: data.companionId, // 陪诊师ID
		serviceType: data.serviceType, // 服务类型
		appointmentTime: data.appointmentTime, // 预约时间
		duration: data.duration, // 服务时长（小时）
		hospital: data.hospital, // 医院信息
		department: data.department, // 科室
		patientInfo: data.patientInfo, // 患者信息
		specialRequirements: data.specialRequirements, // 特殊要求
		contactPhone: data.contactPhone, // 联系电话
		emergencyContact: data.emergencyContact, // 紧急联系人
		address: data.address, // 详细地址
		location: data.location, // 位置坐标
		notes: data.notes // 备注
	})
}

// 获取预约列表
export const getAppointmentList = (params = {}) => {
	return get('/customer/appointments', {
		status: params.status, // 预约状态筛选
		startTime: params.startTime, // 开始时间
		endTime: params.endTime, // 结束时间
		page: params.page || 1,
		pageSize: params.pageSize || 10
	})
}

// 获取预约详情
export const getAppointmentDetail = (appointmentId) => {
	return get(`/customer/appointments/${appointmentId}`)
}

// 修改预约
export const updateAppointment = (appointmentId, data) => {
	return put(`/customer/appointments/${appointmentId}`, {
		appointmentTime: data.appointmentTime,
		duration: data.duration,
		specialRequirements: data.specialRequirements,
		contactPhone: data.contactPhone,
		notes: data.notes
	})
}

// 取消预约
export const cancelAppointment = (appointmentId, reason) => {
	return put(`/customer/appointments/${appointmentId}/cancel`, {
		reason
	})
}

// 确认预约
export const confirmAppointment = (appointmentId) => {
	return put(`/customer/appointments/${appointmentId}/confirm`)
}

// 完成预约
export const completeAppointment = (appointmentId) => {
	return put(`/customer/appointments/${appointmentId}/complete`)
}

// 评价预约
export const rateAppointment = (appointmentId, data) => {
	return post(`/customer/appointments/${appointmentId}/rate`, {
		rating: data.rating, // 评分 1-5
		comment: data.comment, // 评价内容
		tags: data.tags, // 评价标签
		images: data.images // 评价图片
	})
}

// 获取服务类型列表
export const getServiceTypes = () => {
	return get('/customer/service-types')
}

// 获取医院列表
export const getHospitals = (params = {}) => {
	return get('/customer/hospitals', {
		city: params.city,
		district: params.district,
		keyword: params.keyword,
		level: params.level, // 医院等级
		latitude: params.latitude,
		longitude: params.longitude,
		radius: params.radius,
		page: params.page || 1,
		pageSize: params.pageSize || 20
	})
}

// 获取医院详情
export const getHospitalDetail = (hospitalId) => {
	return get(`/customer/hospitals/${hospitalId}`)
}

// 获取医院科室列表
export const getHospitalDepartments = (hospitalId) => {
	return get(`/customer/hospitals/${hospitalId}/departments`)
}

// 收藏陪诊师
export const favoriteCompanion = (companionId) => {
	return post(`/customer/companions/${companionId}/favorite`)
}

// 取消收藏陪诊师
export const unfavoriteCompanion = (companionId) => {
	return del(`/customer/companions/${companionId}/favorite`)
}

// 获取收藏的陪诊师列表
export const getFavoriteCompanions = (params = {}) => {
	return get('/customer/favorites/companions', {
		page: params.page || 1,
		pageSize: params.pageSize || 20
	})
}

// 获取预约统计
export const getAppointmentStats = () => {
	return get('/customer/appointments/stats')
}

// 获取推荐陪诊师
export const getRecommendedCompanions = (params = {}) => {
	return get('/customer/companions/recommended', {
		latitude: params.latitude,
		longitude: params.longitude,
		serviceType: params.serviceType,
		limit: params.limit || 10
	})
}

// 举报陪诊师
export const reportCompanion = (companionId, data) => {
	return post(`/customer/companions/${companionId}/report`, {
		reason: data.reason,
		description: data.description,
		evidence: data.evidence
	})
}
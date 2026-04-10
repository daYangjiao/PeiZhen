// 用户信息相关API
import { get, post, put, upload } from '../utils/api.js'

// 获取个人信息
export const getUserInfo = () => {
	return get('/user/profile')
}

// 更新个人信息
export const updateUserInfo = (data) => {
	return put('/user/profile', {
		name: data.name,
		gender: data.gender,
		birthday: data.birthday,
		phone: data.phone,
		email: data.email,
		address: data.address,
		bio: data.bio, // 个人简介
		specialty: data.specialty, // 专业特长
		experience: data.experience, // 工作经验
		certificates: data.certificates // 资质证书
	})
}

// 上传头像
export const uploadAvatar = (filePath) => {
	return upload('/user/avatar', filePath)
}

// 获取用户详情（查看其他用户）
export const getUserDetail = (userId) => {
	return get(`/user/${userId}`)
}

// 获取用户评价列表
export const getUserRatings = (userId, params = {}) => {
	return get(`/user/${userId}/ratings`, {
		page: params.page || 1,
		pageSize: params.pageSize || 10
	})
}

// 获取用户统计信息
export const getUserStats = (userId) => {
	return get(`/user/${userId}/stats`)
}

// 更新用户位置
export const updateUserLocation = (location) => {
	return put('/user/location', {
		latitude: location.latitude,
		longitude: location.longitude,
		address: location.address,
		city: location.city,
		province: location.province
	})
}

// 获取附近的陪诊师
export const getNearbyCompanions = (params) => {
	return get('/user/nearby', {
		latitude: params.latitude,
		longitude: params.longitude,
		radius: params.radius || 5000, // 搜索半径（米）
		serviceType: params.serviceType,
		page: params.page || 1,
		pageSize: params.pageSize || 20
	})
}

// 关注/取消关注用户
export const toggleFollow = (userId, action) => {
	return post(`/user/${userId}/${action}`) // action: follow 或 unfollow
}

// 获取关注列表
export const getFollowList = (params = {}) => {
	return get('/user/follows', {
		type: params.type || 'following', // following: 关注的, followers: 粉丝
		page: params.page || 1,
		pageSize: params.pageSize || 20
	})
}

// 举报用户
export const reportUser = (userId, data) => {
	return post(`/user/${userId}/report`, {
		reason: data.reason,
		description: data.description,
		evidence: data.evidence // 举报证据（图片等）
	})
}

// 拉黑/取消拉黑用户
export const toggleBlock = (userId, action) => {
	return post(`/user/${userId}/${action}`) // action: block 或 unblock
}

// 获取黑名单
export const getBlockList = (params = {}) => {
	return get('/user/blocks', {
		page: params.page || 1,
		pageSize: params.pageSize || 20
	})
}

// 实名认证
export const submitVerification = (data) => {
	return post('/user/verification', {
		realName: data.realName,
		idCard: data.idCard,
		idCardFront: data.idCardFront, // 身份证正面照
		idCardBack: data.idCardBack, // 身份证反面照
		facePhoto: data.facePhoto // 手持身份证照片
	})
}

// 获取认证状态
export const getVerificationStatus = () => {
	return get('/user/verification/status')
}

// 上传资质证书
export const uploadCertificate = (filePath, certificateType) => {
	return upload('/user/certificate', filePath, {
		certificateType
	})
}

// 获取用户钱包信息
export const getWalletInfo = () => {
	return get('/user/wallet')
}

// 获取收入记录
export const getIncomeRecords = (params = {}) => {
	return get('/user/income', {
		page: params.page || 1,
		pageSize: params.pageSize || 20,
		startTime: params.startTime,
		endTime: params.endTime
	})
}
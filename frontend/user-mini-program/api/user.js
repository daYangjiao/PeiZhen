// 用户信息管理相关API
import { get, post, put, upload } from '../utils/api.js'
import { setUserInfo, getUserInfo as getAuthUserInfo } from '../utils/auth.js'
import { useUserStore } from '../stores/user'

// 获取个人信息
export const getUserInfo = (userId) => {
	// 使用正确的后端接口路径
	return get(`/api/users/${userId}`)
}

// 通过用户ID获取用户详细信息
export const getUserById = (userId) => {
	// 添加调试日志
	console.log('获取用户详细信息，用户ID:', userId)
	// 使用RESTful风格的路径参数
	return get(`/api/users/${userId}`)
}

// 获取当前登录用户信息
export const getCurrentUserInfo = () => {
	console.log('获取当前登录用户信息')
	return get('/api/users/current')
}

// 更新个人信息
export const updateUserInfo = (data) => {
	// 尝试从不同的字段名获取用户ID
	const userId = data.userId || data.id || data.yonghuid || (useUserStore().userInfo ? (useUserStore().userInfo.userId || useUserStore().userInfo.id || useUserStore().userInfo.yonghuid) : null)
	
	if (!userId) {
		return Promise.reject(new Error('用户ID不存在'))
	}
	
	// 添加调试日志
	console.log('更新用户信息，用户ID:', userId, '请求数据:', JSON.stringify(data))
	
	// 构建请求数据，只包含非空字段
	const requestData = {
		// 确保请求体中包含id字段
		id: userId,
		yonghuid: userId
	}
	
	// 根据后端API要求设置字段
	if (data.name !== undefined) requestData.name = data.name
	if (data.username !== undefined) requestData.username = data.username
	if (data.sex !== undefined) requestData.sex = data.sex
	if (data.age !== undefined) requestData.age = data.age
	if (data.phone !== undefined) requestData.phone = data.phone
	if (data.password !== undefined) requestData.password = data.password
	if (data.avatar !== undefined) requestData.avatar = data.avatar
	
	// 保留原有字段（如果后端支持）
	if (data.nickname !== undefined) requestData.nickname = data.nickname
	if (data.gender !== undefined) requestData.gender = data.gender
	if (data.birthday !== undefined) requestData.birthday = data.birthday
	if (data.email !== undefined) requestData.email = data.email
	if (data.address !== undefined) requestData.address = data.address
	if (data.emergencyContact !== undefined) requestData.emergencyContact = data.emergencyContact
	if (data.medicalHistory !== undefined) requestData.medicalHistory = data.medicalHistory
	if (data.allergies !== undefined) requestData.allergies = data.allergies
	if (data.currentMedications !== undefined) requestData.currentMedications = data.currentMedications
	if (data.bio !== undefined) requestData.bio = data.bio
	
	// 使用RESTful风格的路径参数，不添加查询参数
	return put(`/api/users/${userId}`, requestData).then(response => {
		// 更新本地用户信息
		if (response.data) {
			setUserInfo(response.data)
		}
		return response
	})
}

// 上传头像（与陪诊师端一致：先上传图片得到路径，再更新用户 avatar 字段）
export const uploadAvatar = async (filePath) => {
	const userInfo = getAuthUserInfo()
	const userId = userInfo?.id || userInfo?.userId
	if (!userId) {
		return Promise.reject(new Error('请先登录'))
	}
	// 1. 上传图片到通用接口，后端返回 /uploads/xxx
	const uploadRes = await upload('/api/common/upload-image', filePath, {}, 'file')
	const avatarPath = uploadRes.data
	if (!avatarPath) {
		return Promise.reject(new Error('上传失败'))
	}
	// 2. 更新用户头像字段
	await put(`/api/users/${userId}`, { avatar: avatarPath })
	// 3. 同步到本地 store 和 storage
	if (userInfo) {
		setUserInfo({ ...userInfo, avatar: avatarPath })
	}
	return { code: 200, data: { avatarUrl: avatarPath } }
}

// 更新用户位置
export const updateUserLocation = (location) => {
	return put('/customer/user/location', {
		latitude: location.latitude,
		longitude: location.longitude,
		address: location.address,
		city: location.city,
		province: location.province,
		district: location.district
	})
}

// 获取用户地址列表
export const getUserAddresses = () => {
	return get('/customer/user/addresses')
}

// 添加用户地址
export const addUserAddress = (data) => {
	return post('/customer/user/addresses', {
		name: data.name, // 地址名称
		contact: data.contact, // 联系人
		phone: data.phone, // 联系电话
		province: data.province,
		city: data.city,
		district: data.district,
		address: data.address, // 详细地址
		latitude: data.latitude,
		longitude: data.longitude,
		isDefault: data.isDefault || false // 是否默认地址
	})
}

// 更新用户地址
export const updateUserAddress = (addressId, data) => {
	return put(`/customer/user/addresses/${addressId}`, {
		name: data.name,
		contact: data.contact,
		phone: data.phone,
		province: data.province,
		city: data.city,
		district: data.district,
		address: data.address,
		latitude: data.latitude,
		longitude: data.longitude,
		isDefault: data.isDefault
	})
}

// 删除用户地址
export const deleteUserAddress = (addressId) => {
	return put(`/customer/user/addresses/${addressId}/delete`)
}

// 设置默认地址
export const setDefaultAddress = (addressId) => {
	return put(`/customer/user/addresses/${addressId}/default`)
}

// 获取用户统计信息
export const getUserStats = () => {
	return get('/customer/user/stats')
}

// 获取用户钱包信息
export const getWalletInfo = () => {
	return get('/customer/user/wallet')
}

// 获取消费记录
export const getExpenseRecords = (params = {}) => {
	return get('/customer/user/expenses', {
		type: params.type, // 消费类型
		startTime: params.startTime,
		endTime: params.endTime,
		page: params.page || 1,
		pageSize: params.pageSize || 20
	})
}

// 获取积分记录
export const getPointRecords = (params = {}) => {
	return get('/customer/user/points', {
		type: params.type, // 积分类型：earn, spend
		startTime: params.startTime,
		endTime: params.endTime,
		page: params.page || 1,
		pageSize: params.pageSize || 20
	})
}

// 实名认证
export const submitVerification = (data) => {
	return post('/customer/user/verification', {
		realName: data.realName,
		idCard: data.idCard,
		idCardFront: data.idCardFront, // 身份证正面照
		idCardBack: data.idCardBack, // 身份证反面照
		facePhoto: data.facePhoto // 手持身份证照片
	})
}

// 获取认证状态
export const getVerificationStatus = () => {
	return get('/customer/user/verification/status')
}

// 获取用户设置
export const getUserSettings = () => {
	return get('/customer/user/settings')
}

// 更新用户设置
export const updateUserSettings = (settings) => {
	return put('/customer/user/settings', {
		privacySettings: settings.privacySettings, // 隐私设置
		notificationSettings: settings.notificationSettings, // 通知设置
		language: settings.language, // 语言设置
		theme: settings.theme, // 主题设置
		autoLocation: settings.autoLocation, // 自动定位
		showOnlineStatus: settings.showOnlineStatus // 显示在线状态
	})
}

// 获取收藏列表
export const getFavorites = (type, params = {}) => {
	return get(`/customer/user/favorites/${type}`, {
		page: params.page || 1,
		pageSize: params.pageSize || 20
	})
}

// 添加收藏
export const addFavorite = (type, targetId) => {
	return post(`/customer/user/favorites/${type}`, {
		targetId
	})
}

// 取消收藏
export const removeFavorite = (type, targetId) => {
	return put(`/customer/user/favorites/${type}/${targetId}/remove`)
}

// 获取浏览历史
export const getBrowseHistory = (type, params = {}) => {
	return get(`/customer/user/history/${type}`, {
		page: params.page || 1,
		pageSize: params.pageSize || 20
	})
}

// 清空浏览历史
export const clearBrowseHistory = (type) => {
	return put(`/customer/user/history/${type}/clear`)
}

// 反馈建议
export const submitFeedback = (data) => {
	return post('/customer/user/feedback', {
		type: data.type, // 反馈类型
		title: data.title,
		content: data.content,
		contact: data.contact, // 联系方式
		images: data.images, // 截图
		deviceInfo: data.deviceInfo // 设备信息
	})
}

// 获取反馈列表
export const getFeedbackList = (params = {}) => {
	return get('/customer/user/feedback', {
		status: params.status,
		page: params.page || 1,
		pageSize: params.pageSize || 10
	})
}

// 获取反馈详情
export const getFeedbackDetail = (feedbackId) => {
	return get(`/customer/user/feedback/${feedbackId}`)
}

// 签到
export const checkIn = () => {
	return post('/customer/user/checkin')
}

// 获取签到记录
export const getCheckInRecords = (params = {}) => {
	return get('/customer/user/checkin', {
		year: params.year,
		month: params.month
	})
}

// 邀请好友
export const inviteFriend = (data) => {
	return post('/customer/user/invite', {
		phone: data.phone,
		name: data.name,
		message: data.message
	})
}

// 获取邀请记录
export const getInviteRecords = (params = {}) => {
	return get('/customer/user/invites', {
		page: params.page || 1,
		pageSize: params.pageSize || 20
	})
}
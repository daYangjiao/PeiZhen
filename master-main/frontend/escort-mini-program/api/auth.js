// 用户认证相关API
import { post, get } from '../utils/api.js'

// 用户登录
export const login = (data) => {
	return post('/auth/login', {
		phone: data.phone,
		password: data.password
	})
}

// 用户注册
export const register = (data) => {
	return post('/auth/register', {
		phone: data.phone,
		password: data.password,
		name: data.name,
		verifyCode: data.verifyCode
	})
}

// 发送验证码
export const sendVerifyCode = (phone) => {
	return post('/auth/send-code', {
		phone
	})
}

// 刷新token
export const refreshToken = () => {
	return post('/auth/refresh-token')
}

// 退出登录
export const logout = () => {
	return post('/auth/logout')
}

// 检查登录状态
export const checkLoginStatus = () => {
	return get('/auth/check-status')
}

// 修改密码
export const changePassword = (data) => {
	return post('/auth/change-password', {
		oldPassword: data.oldPassword,
		newPassword: data.newPassword
	})
}

// 忘记密码
export const resetPassword = (data) => {
	return post('/auth/reset-password', {
		phone: data.phone,
		newPassword: data.newPassword,
		verifyCode: data.verifyCode
	})
}
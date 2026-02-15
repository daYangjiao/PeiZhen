// 用户端认证相关API
import { post, get } from '../utils/api.js'
import { setUserInfo, clearUserInfo } from '../utils/auth.js'

// 用户登录
export const login = (data) => {
	console.log('开始登录请求，用户名:', data.username);
	
	return post('/api/users/login', {
			username: data.username,
			password: data.password
		}).then(response => {
			console.log('登录接口原始响应:', JSON.stringify(response));
			
			// 检查响应结构
			if (!response) {
				throw new Error('服务器无响应');
			}
			
			if (response.data && response.data.token) {
				// 从后端响应中提取用户信息
				const backendUserInfo = response.data.userInfo || {};
				
				// 构造完整的用户信息对象
				const userInfo = {
					// 基本信息
					id: backendUserInfo.id || response.data.id,
					userId: backendUserInfo.id || response.data.id,
					username: backendUserInfo.username || data.username,
					userType: backendUserInfo.userType !== undefined ? backendUserInfo.userType : 0,
					token: response.data.token,
					
					// 昵称处理 - 确保有显示名称
					nickName: backendUserInfo.username || data.username || '用户',
					name: backendUserInfo.username || data.username,
					
					// 时间戳
					loginTime: Date.now()
				};
				
				console.log('登录成功，构造的用户信息:', JSON.stringify(userInfo));
				
				// 保存用户信息到本地
				setUserInfo(userInfo);
				
				// 返回处理后的响应
				return {
					...response,
					data: {
						...response.data,
						userInfo: userInfo
					}
				};
			} else {
				// 检查是否有错误信息
				let errorMsg = '登录失败';
				if (response.message) {
					errorMsg = response.message;
				} else if (response.data && response.data.message) {
					errorMsg = response.data.message;
				} else if (response.code === 401) {
					errorMsg = '用户名或密码错误';
				}
				
				console.log('登录失败，错误信息:', errorMsg);
				throw new Error(errorMsg);
			}
		}).catch(error => {
			console.error('登录请求捕获到错误:', error);
			
			// 如果是Error对象，直接抛出
			if (error instanceof Error) {
				throw error;
			}
			
			// 处理其他类型的错误
			let errorMsg = '登录失败';
			if (error.message) {
				errorMsg = error.message;
			} else if (error.statusCode === 401) {
				errorMsg = '用户名或密码错误';
			} else if (error.statusCode >= 500) {
				errorMsg = '服务器错误';
			} else if (error.statusCode) {
				errorMsg = `请求失败 (${error.statusCode})`;
			}
			
			throw new Error(errorMsg);
		})
}

// 用户注册
export const register = (data) => {
	return post('/api/users/register', {
		username: data.username,
		password: data.password,
		name: data.name,
		openid: data.openid || '' // 添加openid字段，后端要求不能为null
	})
}

// 微信登录
export const wxLogin = (code) => {
	return post('/customer/auth/wx-login', {
		code
	}).then(response => {
		if (response.data.token) {
			// 构造用户信息对象
			const userInfo = {
				...response.data.userInfo,
				token: response.data.token,
				// 确保设置昵称
				nickName: response.data.userInfo.username || response.data.userInfo.nickName || '用户'
			}
			// 保存用户信息到本地
			setUserInfo(userInfo)
		}
		return response
	})
}

// 微信手机号授权登录
// 通过微信加密数据获取手机号并登录
export const wechatPhoneLogin = (params) => {
	return post('/api/users/wechat/login/phone', {
		code: params.code,           // 微信临时code
		encryptedData: params.encryptedData,  // 加密的手机号数据
		iv: params.iv,               // 解密向量
		signature: params.signature  // 签名，用于数据校验
	}).then(response => {
		if (response.data.token) {
			// 构造用户信息对象
			const userInfo = {
				...response.data.userInfo,
				token: response.data.token,
				// 确保设置昵称
				nickName: response.data.userInfo.username || response.data.userInfo.nickName || '用户'
			}
			// 保存用户信息到本地
			setUserInfo(userInfo)
		}
		return response
	})
}

// 微信手机号解密
export const wechatPhoneDecrypt = (data) => {
	return post('/api/users/wechat/phone', {
		encryptedData: data.encryptedData,
		iv: data.iv,
		signature: data.signature, // 签名，用于数据校验
		sessionKey: data.sessionKey,
		openid: data.openid
	})
}

// 发送验证码
export const sendVerifyCode = (phone, type = 'login') => {
	return post('/customer/auth/send-code', {
		phone,
		type // login: 登录, register: 注册, reset: 重置密码
	})
}

// 验证码登录
export const codeLogin = (data) => {
	return post('/customer/auth/code-login', {
		phone: data.phone,
		verifyCode: data.verifyCode
	}).then(response => {
		if (response.data.token) {
			// 构造用户信息对象
			const userInfo = {
				...response.data.userInfo,
				token: response.data.token,
				// 确保设置昵称
				nickName: response.data.userInfo.username || response.data.userInfo.nickName || '用户'
			}
			// 保存用户信息到本地
			setUserInfo(userInfo)
		}
		return response
	})
}

// 刷新token
export const refreshToken = () => {
	return post('/customer/auth/refresh-token')
}

// 退出登录
export const logout = () => {
	return post('/customer/auth/logout').finally(() => {
		// 无论成功失败都清除本地用户信息
		clearUserInfo()
	})
}

// 检查登录状态
export const checkLoginStatus = () => {
	return get('/customer/auth/check-status')
}

// 修改密码
export const changePassword = (data) => {
	return post('/customer/auth/change-password', {
		oldPassword: data.oldPassword,
		newPassword: data.newPassword
	})
}

// 忘记密码
export const resetPassword = (data) => {
	return post('/customer/auth/reset-password', {
		phone: data.phone,
		newPassword: data.newPassword,
		verifyCode: data.verifyCode
	})
}

// 绑定手机号
export const bindPhone = (data) => {
	return post('/customer/auth/bind-phone', {
		phone: data.phone,
		verifyCode: data.verifyCode
	})
}

// 解绑手机号
export const unbindPhone = (data) => {
	return post('/customer/auth/unbind-phone', {
		verifyCode: data.verifyCode
	})
}

// 注销账户
export const deleteAccount = (data) => {
	return post('/customer/auth/delete-account', {
		password: data.password,
		verifyCode: data.verifyCode,
		reason: data.reason
	}).then(response => {
		// 注销成功后清除本地信息
		clearUserInfo()
		return response
	})
}
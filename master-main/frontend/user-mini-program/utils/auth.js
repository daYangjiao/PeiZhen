/**
 * 用户认证相关工具函数
 * 注意：此文件已重构为与Pinia store配合使用，保持向后兼容性
 */

import { useUserStore } from '@/stores/user'

// 存储键名常量
const STORAGE_KEYS = {
	USER_INFO: 'userInfo',
	IS_LOGIN: 'isLogin',
	IS_GUEST: 'isGuest',
	TOKEN: 'token'
};

/**
 * 获取用户store实例
 * @returns {Object} 用户store实例
 */
function getUserStore() {
	try {
		return useUserStore()
	} catch (error) {
		console.warn('无法获取用户store，使用本地存储作为备用方案')
		return null
	}
}

/**
 * 检查用户是否已登录
 * @returns {boolean} 是否已登录
 */
export function isLoggedIn() {
	const userStore = getUserStore()
	if (userStore) {
		return userStore.isLoggedIn
	}
	
	// 备用方案：直接从本地存储读取
	const isLogin = uni.getStorageSync(STORAGE_KEYS.IS_LOGIN);
	const userInfo = uni.getStorageSync(STORAGE_KEYS.USER_INFO);
	return !!(isLogin && userInfo);
}

/**
 * 检查是否为游客模式
 * @returns {boolean} 是否为游客模式
 */
export function isGuestMode() {
	const userStore = getUserStore()
	if (userStore) {
		return userStore.isGuestMode
	}
	
	// 备用方案：直接从本地存储读取
	return !!uni.getStorageSync(STORAGE_KEYS.IS_GUEST);
}

/**
 * 获取用户信息
 * @returns {object|null} 用户信息对象或null
 */
export function getUserInfo() {
	const userStore = getUserStore()
	if (userStore) {
		return userStore.userInfo
	}
	
	// 备用方案：直接从本地存储读取
	try {
		const userInfo = uni.getStorageSync(STORAGE_KEYS.USER_INFO);
		return userInfo || null;
	} catch (error) {
		console.error('获取用户信息失败:', error);
		return null;
	}
}

/**
 * 保存用户信息
 * @param {object} userInfo 用户信息
 */
export function setUserInfo(userInfo) {
	const userStore = getUserStore()
	if (userStore) {
		userStore.setUserInfo(userInfo)
		return
	}
	
	// 备用方案：直接保存到本地存储
	try {
		uni.setStorageSync(STORAGE_KEYS.USER_INFO, userInfo);
		uni.setStorageSync(STORAGE_KEYS.IS_LOGIN, true);
		// 清除游客模式标记
		uni.removeStorageSync(STORAGE_KEYS.IS_GUEST);
	} catch (error) {
		console.error('保存用户信息失败:', error);
	}
}

/**
 * 清除用户信息（退出登录）
 */
export function clearUserInfo() {
	const userStore = getUserStore()
	if (userStore) {
		userStore.clearUserInfo()
		return
	}
	
	// 备用方案：直接清除本地存储
	try {
		uni.removeStorageSync(STORAGE_KEYS.USER_INFO);
		uni.removeStorageSync(STORAGE_KEYS.IS_LOGIN);
		uni.removeStorageSync(STORAGE_KEYS.IS_GUEST);
		uni.removeStorageSync(STORAGE_KEYS.TOKEN);
	} catch (error) {
		console.error('清除用户信息失败:', error);
	}
}

/**
 * 设置游客模式
 */
export function setGuestMode() {
	const userStore = getUserStore()
	if (userStore) {
		userStore.setGuestMode()
		return
	}
	
	// 备用方案：直接保存到本地存储
	try {
		uni.setStorageSync(STORAGE_KEYS.IS_GUEST, true);
		// 清除登录状态
		uni.removeStorageSync(STORAGE_KEYS.IS_LOGIN);
		uni.removeStorageSync(STORAGE_KEYS.USER_INFO);
	} catch (error) {
		console.error('设置游客模式失败:', error);
	}
}

/**
 * 检查登录状态，未登录则跳转到登录页
 * @param {boolean} showModal 是否显示提示弹窗
 * @returns {boolean} 是否已登录
 */
export function checkLoginStatus(showModal = true) {
	const userStore = getUserStore()
	if (userStore) {
		return userStore.checkLoginStatus(showModal)
	}
	
	// 备用方案：使用原有逻辑
	if (isLoggedIn()) {
		return true;
	}
	
	if (showModal) {
		uni.showModal({
			title: '提示',
			content: '请先登录后再使用此功能',
			confirmText: '去登录',
			cancelText: '取消',
			success: (res) => {
				if (res.confirm) {
					navigateToLogin();
				}
			}
		});
	} else {
		navigateToLogin();
	}
	
	return false;
}

/**
 * 跳转到登录页面
 */
export function navigateToLogin() {
	uni.navigateTo({
		url: '/subpkg/auth/login'
	});
}

/**
 * 微信登录
 * @returns {Promise} 登录结果
 */
export function wxLogin() {
	const userStore = getUserStore()
	if (userStore) {
		return userStore.wxLogin()
	}
	
	// 备用方案：使用原有逻辑
	return new Promise((resolve, reject) => {
		uni.login({
			provider: 'weixin',
			success: (res) => {
				if (res.code) {
					resolve(res);
				} else {
					reject(new Error('获取登录凭证失败'));
				}
			},
			fail: (err) => {
				reject(err);
			}
		});
	});
}

/**
 * 获取用户信息（微信授权）
 * @returns {Promise} 用户信息
 */
export function getUserProfile() {
	const userStore = getUserStore()
	if (userStore) {
		return userStore.getUserProfile()
	}
	
	// 备用方案：使用原有逻辑
	return new Promise((resolve, reject) => {
		uni.getUserProfile({
			desc: '用于完善用户资料',
			success: (res) => {
				resolve(res.userInfo);
			},
			fail: (err) => {
				reject(err);
			}
		});
	});
}

/**
 * 退出登录
 */
export function logout() {
	const userStore = getUserStore()
	if (userStore) {
		userStore.logout()
		return
	}
	
	// 备用方案：使用原有逻辑
	uni.showModal({
		title: '确认退出',
		content: '确定要退出登录吗？',
		success: (res) => {
			if (res.confirm) {
				clearUserInfo();
				uni.showToast({
					title: '已退出登录',
					icon: 'success'
				});
				
				// 跳转到首页
				setTimeout(() => {
					uni.switchTab({
						url: '/pages/index/index'
					});
				}, 1500);
			}
		}
	});
}

/**
 * 格式化用户显示名称
 * @param {object} userInfo 用户信息
 * @returns {string} 显示名称
 */
export function getDisplayName(userInfo) {
	const userStore = getUserStore()
	if (userStore) {
		return userStore.displayName
	}
	
	// 备用方案：使用原有逻辑
	if (!userInfo) return '未登录';
	return userInfo.nickName || userInfo.name || '用户';
}

/**
 * 检查是否需要更新用户信息
 * @returns {boolean} 是否需要更新
 */
export function needUpdateUserInfo() {
	const userStore = getUserStore()
	if (userStore) {
		return userStore.needUpdateUserInfo
	}
	
	// 备用方案：使用原有逻辑
	const userInfo = getUserInfo();
	if (!userInfo) return false;
	
	// 检查登录时间，超过7天需要更新
	const loginTime = userInfo.loginTime || 0;
	const now = Date.now();
	const sevenDays = 7 * 24 * 60 * 60 * 1000;
	
	return (now - loginTime) > sevenDays;
}

export default {
	isLoggedIn,
	isGuestMode,
	getUserInfo,
	setUserInfo,
	clearUserInfo,
	setGuestMode,
	checkLoginStatus,
	navigateToLogin,
	wxLogin,
	getUserProfile,
	logout,
	getDisplayName,
	needUpdateUserInfo
};
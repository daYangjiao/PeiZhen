/**
 * 陪诊师端用户认证相关工具函数
 */

// 存储键名常量
const STORAGE_KEYS = {
	USER_INFO: 'userInfo',
	IS_LOGIN: 'isLogin',
	IS_GUEST: 'isGuest',
	TOKEN: 'token'
};

/**
 * 检查用户是否已登录
 * @returns {boolean} 是否已登录
 */
export function isLoggedIn() {
	const isLogin = uni.getStorageSync(STORAGE_KEYS.IS_LOGIN);
	const userInfo = uni.getStorageSync(STORAGE_KEYS.USER_INFO);
	return !!(isLogin && userInfo);
}

/**
 * 获取用户信息
 * @returns {object|null} 用户信息对象或null
 */
export function getUserInfo() {
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
	try {
		uni.setStorageSync(STORAGE_KEYS.USER_INFO, userInfo);
		uni.setStorageSync(STORAGE_KEYS.IS_LOGIN, true);
		// 清除游客模式标记
		uni.removeStorageSync(STORAGE_KEYS.IS_GUEST);
		
		// 单独存储token
		if (userInfo.token) {
			uni.setStorageSync(STORAGE_KEYS.TOKEN, userInfo.token);
		}
	} catch (error) {
		console.error('保存用户信息失败:', error);
	}
}

/**
 * 清除用户信息（退出登录）
 */
export function clearUserInfo() {
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
 * 跳转到登录页面
 */
export function navigateToLogin() {
	uni.navigateTo({
		url: '/subpkg/auth/login'
	});
}

/**
 * 退出登录
 */
export function logout() {
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

export default {
	isLoggedIn,
	getUserInfo,
	setUserInfo,
	clearUserInfo,
	navigateToLogin,
	logout
};
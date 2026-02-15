// 微信授权登录工具函数
import { wxLogin, wechatPhoneLogin, wechatPhoneDecrypt } from '@/api/auth'

// 微信手机号登录 - 一键完成授权和手机号获取
export const handleWechatPhoneLogin = async (phoneEvent = null) => {
	try {
		// #ifdef MP-WEIXIN
		// 如果传入了手机号事件，说明是从按钮回调触发的
		if (phoneEvent) {
			// 获取微信登录凭证
		const loginRes = await new Promise((resolve, reject) => {
			uni.login({
				provider: 'weixin',
				success: (res) => {
					resolve(res);
				},
				fail: (err) => {
					reject(err);
				}
			});
		});
		
		if (!loginRes || !loginRes.code) {
			throw new Error('获取微信登录凭证失败');
		}
		
		const code = loginRes.code;
			console.log('获取到微信code:', code);
			
			// 调用手机号授权处理函数
			return await handlePhoneAuth(phoneEvent, code);
		}
		
		// 否则是初始化授权流程（保留向后兼容）
		// 获取微信登录凭证
		const loginRes = await new Promise((resolve, reject) => {
			uni.login({
				provider: 'weixin',
				success: (res) => {
					resolve(res);
				},
				fail: (err) => {
					reject(err);
				}
			});
		});
		
		if (!loginRes || !loginRes.code) {
			throw new Error('获取微信登录凭证失败');
		}
		
		const code = loginRes.code;
		console.log('获取到微信code:', code);
		
		// 调用后端接口获取 openid 和 session_key
		const authResult = await wechatPhoneLogin({ code });
		
		if (authResult.success) {
			console.log('微信授权成功，获取到session_key');
			// 存储session_key等信息供后续使用
			uni.setStorageSync('wechat_session', {
				openid: authResult.data.openid,
				session_key: authResult.data.session_key,
				timestamp: Date.now()
			});
			return authResult.data;
		} else {
			throw new Error(authResult.message || '微信授权失败');
		}
		// #endif
		
		// #ifndef MP-WEIXIN
		// 非微信小程序环境的处理
		throw new Error('当前环境不支持微信登录');
		// #endif
	} catch (error) {
		console.error('微信授权失败:', error);
		throw error;
	}
};

// 处理手机号授权回调（保持向后兼容）
export const handleGetPhoneNumber = async (e) => {
	try {
		console.log('手机号授权回调:', e);
		
		// #ifdef MP-WEIXIN
		if (e.detail.errMsg === 'getPhoneNumber:ok') {
			return await handlePhoneAuth(e);
		} else {
			throw new Error('用户拒绝授权手机号');
		}
		// #endif
		
		// #ifndef MP-WEIXIN
		// 非微信小程序环境
		throw new Error('当前环境不支持获取手机号');
		// #endif
	} catch (error) {
		console.error('获取手机号失败:', error);
		throw error;
	}
};

/**
 * 处理微信手机号授权回调
 * @param {Object} e 微信授权回调事件对象
 * @param {string} code 微信登录code
 * @returns {Promise} 登录结果
 */
export const handlePhoneAuth = async (e, code) => {
  try {
    if (e.detail.errMsg !== 'getPhoneNumber:ok') {
      throw new Error('用户拒绝授权或授权失败')
    }
    
    const { encryptedData, iv, signature } = e.detail
    
    if (!encryptedData || !iv) {
      throw new Error('获取手机号加密数据失败')
    }
    
    // 获取存储的session信息
    const sessionData = uni.getStorageSync('wechat_session');
    if (!sessionData || !sessionData.session_key) {
      throw new Error('微信授权信息已过期，请重新登录');
    }
    
    // 调用后端接口解密手机号
    const loginResult = await wechatPhoneDecrypt({
      encryptedData,
      iv,
      signature,
      sessionKey: sessionData.session_key,
      openid: sessionData.openid
    })
    
    if (loginResult.success) {
      // 保存用户信息和token
      uni.setStorageSync('token', loginResult.data.token);
      uni.setStorageSync('userInfo', loginResult.data.userInfo);
      
      uni.showToast({
        title: '登录成功',
        icon: 'success'
      });
      
      // 登录成功后的处理
      return loginResult.data;
    } else {
      throw new Error(loginResult.message || '登录失败');
    }
  } catch (error) {
    console.error('手机号授权登录失败:', error)
    uni.showToast({
      title: error.message || '登录失败',
      icon: 'none'
    })
    throw error
  }
}

/**
 * 微信普通登录（不获取手机号）
 * @returns {Promise} 登录结果
 */
export const handleWechatLogin = async () => {
  try {
    // #ifdef MP-WEIXIN
    // 1. 获取微信登录code
    const loginRes = await new Promise((resolve, reject) => {
      uni.login({
        provider: 'weixin',
        success: (res) => {
          resolve(res);
        },
        fail: (err) => {
          reject(err);
        }
      });
    });
    
    if (!loginRes || !loginRes.code) {
      throw new Error('获取微信登录code失败')
    }
    
    // 2. 获取用户信息授权
    const userInfoRes = await new Promise((resolve, reject) => {
      uni.getUserProfile({
        desc: '用于完善用户资料',
        success: (res) => {
          resolve(res);
        },
        fail: (err) => {
          reject(err);
        }
      });
    });
    
    if (!userInfoRes) {
      throw new Error('获取用户信息失败')
    }
    
    // 3. 调用后端登录接口
    const loginResult = await wxLogin(loginRes.code)
    
    if (loginResult.data.success) {
      uni.showToast({
        title: '登录成功',
        icon: 'success'
      })
      return loginResult.data
    } else {
      throw new Error(loginResult.data.message || '登录失败')
    }
    // #endif
    
    // #ifndef MP-WEIXIN
    // 开发环境或非微信环境的处理
    uni.showModal({
      title: '提示',
      content: '微信登录功能需要在微信小程序环境中使用，当前为开发调试模式',
      showCancel: false
    })
    throw new Error('当前环境不支持微信登录')
    // #endif
  } catch (error) {
    console.error('微信登录失败:', error)
    uni.showToast({
      title: error.message || '登录失败',
      icon: 'none'
    })
    throw error
  }
}
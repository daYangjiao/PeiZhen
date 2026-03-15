// 用户端API配置文件
import { getUserInfo, clearUserInfo } from './auth.js'

// 基础配置
export const config = {
	// 开发环境API地址
	baseURL: 'http://localhost:8080',
	// 请求超时时间
	timeout: 10000,
	// 请求头配置
	headers: {
		'Content-Type': 'application/json',
		'User-Type': 'customer'
	}
}

// 获取token
export const getToken = () => {
	const userInfo = getUserInfo()
	return userInfo?.token || uni.getStorageSync('token') || ''
}

// 设置token
export const setToken = (token) => {
	uni.setStorageSync('token', token)
}

// 清除token
export const clearToken = () => {
	uni.removeStorageSync('token')
}

// 获取当前页面URL
const getCurrentPageUrl = () => {
	const pages = getCurrentPages()
	if (pages.length === 0) return '/pages/index/index'
	const currentPage = pages[pages.length - 1]
	return '/' + currentPage.route + (currentPage.options ? '?' + Object.keys(currentPage.options).map(key => key + '=' + currentPage.options[key]).join('&') : '')
}

// 强制登出并跳转
const forceLogout = () => {
    console.log('执行强制登出逻辑');
    clearUserInfo()
    clearToken()
    uni.showToast({
        title: '登录已过期，请重新登录',
        icon: 'none',
        duration: 2000
    })

    // 延迟跳转，确保用户看到提示
    setTimeout(() => {
        uni.reLaunch({
            url: '/subpkg/auth/login'
        })
    }, 1500)
}

// 请求拦截器
const requestInterceptor = (options) => {
	const token = getToken()
	const headers = {
		'Content-Type': 'application/json',
		'User-Type': 'customer'
	}

	if (token) {
		headers['Authorization'] = `Bearer ${token}`
	}

	options.url = config.baseURL + options.url
	options.timeout = config.timeout
	options.header = headers

	return options
}

// 响应拦截器
const responseInterceptor = (response) => {
	const { statusCode, data } = response
	console.log('响应拦截器 - 状态码:', statusCode, '数据:', JSON.stringify(data));

	// 1. 优先处理 HTTP 401 状态码 (后端拦截器直接返回 401)
	if (statusCode === 401) {
		console.log('检测到 HTTP 401，准备强制登出');
		forceLogout();
		return Promise.reject(data);
	}

	// 2. 处理业务状态码 401
	if (data && data.code === 401) {
		console.log('检测到业务 401，准备强制登出');
		forceLogout();
		return Promise.reject(data);
	}

	// 3. 处理正常响应
	if (statusCode >= 200 && statusCode < 300) {
		if (data.code !== undefined) {
			if (data.code === 200) {
				return Promise.resolve(data)
			} else {
				uni.showToast({ title: data.message || '请求失败', icon: 'none' })
				return Promise.reject(data)
			}
		}
		return Promise.resolve({ code: 200, data: data })
	} else {
		uni.showToast({ title: '网络请求失败', icon: 'none' })
		return Promise.reject(response)
	}
}

// 通用请求方法
export const request = (options) => {
	return new Promise((resolve, reject) => {
		const interceptedOptions = requestInterceptor(options)
		uni.request({
			...interceptedOptions,
			success: (response) => {
				responseInterceptor(response)
					.then(resolve)
					.catch(reject)
			},
			fail: (error) => {
				uni.showToast({ title: '网络连接失败', icon: 'none' })
				reject(error)
			}
		})
	})
}

export const get = (url, params = {}) => request({ url, method: 'GET', data: params })
export const post = (url, data = {}) => request({ url, method: 'POST', data })
export const put = (url, data = {}) => request({ url, method: 'PUT', data })
export const del = (url, data = {}) => request({ url, method: 'DELETE', data })

// 获取后端图片 URL
export const getBackendImageUrl= (imageName) => {
	const baseUrl= config.baseURL.endsWith('/') ? config.baseURL.slice(0, -1) : config.baseURL
	return `${baseUrl}/uploads/frontend-images/${imageName}`
}

// 文件上传方法
export const upload = (url, filePath, formData = {}, name = 'file') => {
    return new Promise((resolve, reject) => {
        const token = getToken()
        const headers = {
            'Authorization': token ? `Bearer ${token}` : ''
        }

        uni.uploadFile({
            url: config.baseURL + url,
            filePath: filePath,
            name: name,
            formData: formData,
            header: headers,
            success: (response) => {
                try {
                    const data = typeof response.data === 'string' ? JSON.parse(response.data) : response.data
                    if (data.code === 200 || data.code === 0) {
                        resolve(data)
                    } else {
                        uni.showToast({ title: data.message || '上传失败', icon: 'none' })
                        reject(data)
                    }
                } catch (e) {
                    console.error('上传响应解析失败:', e, response.data)
                    uni.showToast({ title: '上传失败', icon: 'none' })
                    reject(response)
                }
            },
            fail: (error) => {
                console.error('文件上传失败:', error)
                uni.showToast({ title: '上传失败', icon: 'none' })
                reject(error)
            }
        })
    })
}

export default {
	config, getToken, setToken, clearToken, request, get, post, put, del, upload
}
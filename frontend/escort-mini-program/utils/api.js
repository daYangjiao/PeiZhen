// 陪诊师端API配置文件

// 基础配置
export const config = {
	baseURL: 'http://localhost:8080', 
	timeout: 10000,
	headers: {
		'Content-Type': 'application/json'
	}
}

// 获取token
export const getToken = () => {
	return uni.getStorageSync('token') || ''
}

// 设置token
export const setToken = (token) => {
	uni.setStorageSync('token', token)
}

// 清除token
export const clearToken = () => {
	uni.removeStorageSync('token')
}

// 强制登出并跳转
const forceLogout = () => {
    console.log('执行强制登出逻辑');
    uni.removeStorageSync('userInfo');
    uni.removeStorageSync('isLoggedIn');
    clearToken();

    uni.showToast({
        title: '登录已过期，请重新登录',
        icon: 'none',
        duration: 2000
    });

    setTimeout(() => {
        uni.reLaunch({
            url: '/subpkg/auth/login'
        });
    }, 1500);
}

// 请求拦截器
const requestInterceptor = (options) => {
	const token = getToken()
	const headers = {
		...config.headers,
		...options.header
	}

	if (token) {
		headers['Authorization'] = `Bearer ${token}`
	}

	if (!options.url.startsWith('http')) {
		options.url = config.baseURL + options.url
	}

	options.timeout = config.timeout
	options.header = headers

	console.log('🚀 请求发送:', options.method, options.url)
	return options
}

// 响应拦截器
const responseInterceptor = (response) => {
	const { statusCode, data } = response
	console.log('✅ 响应接收:', statusCode, data)

	// 1. 优先处理 HTTP 401 状态码
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
			if (data.code === 200 || data.code === 0) {
				return Promise.resolve(data)
			} else {
				uni.showToast({ title: data.message || data.msg || '请求失败', icon: 'none' })
				return Promise.reject(data)
			}
		}
		return Promise.resolve({ code: 200, data: data })
	} else {
		uni.showToast({ title: '网络请求失败(' + statusCode + ')', icon: 'none' })
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
				responseInterceptor(response).then(resolve).catch(reject)
			},
			fail: (error) => {
				console.error('❌ 请求失败:', error)
				uni.showToast({ title: '无法连接到服务器', icon: 'none' })
				reject(error)
			}
		})
	})
}

export const get = (url, params = {}) => request({ url, method: 'GET', data: params })
export const post = (url, data = {}) => request({ url, method: 'POST', data })

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

export default { config, getToken, setToken, clearToken, request, get, post, upload }

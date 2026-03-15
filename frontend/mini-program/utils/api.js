import { useSessionStore } from '@/stores/session'

export const config = {
  baseURL: 'http://localhost:8080',
  timeout: 10000
}

export const getToken = () => {
  const session = useSessionStore()
  return session.token || uni.getStorageSync('token') || ''
}

export const setToken = (token) => {
  const session = useSessionStore()
  session.token = token || ''
  if (token) uni.setStorageSync('token', token)
  else uni.removeStorageSync('token')
}

export const clearToken = () => setToken('')

const buildUrl = (url) => {
  if (url.startsWith('http')) return url
  return config.baseURL + url
}

const forceLogout = (message = '登录状态已失效，请重新登录～') => {
  try {
    uni.showToast({ title: message, icon: 'none', duration: 1500 })
  } finally {
    setTimeout(() => {
      const session = useSessionStore()
      const role = session.role || uni.getStorageSync('role') || 'user'
      session.logout()
      uni.navigateTo({
        url: `/pages/auth/login?role=${role}&from=guard`
      })
    }, 400)
  }
}

export const request = (options) => {
  const session = useSessionStore()
  const token = getToken()

  const header = {
    'Content-Type': 'application/json',
    ...(options.header || {})
  }
  if (token) header.Authorization = `Bearer ${token}`
  if (session.role) header['X-Role'] = session.role

  const reqOptions = {
    ...options,
    url: buildUrl(options.url),
    timeout: config.timeout,
    header
  }

  return new Promise((resolve, reject) => {
    uni.request({
      ...reqOptions,
      success: (res) => {
        const { statusCode, data } = res
        if (statusCode === 401 || (data && data.code === 401)) {
          forceLogout()
          reject(data || res)
          return
        }
        if (statusCode >= 200 && statusCode < 300) {
          if (data && typeof data === 'object' && 'code' in data) {
            if (data.code === 200 || data.code === 0) resolve(data)
            else reject(data)
          } else {
            resolve({ code: 200, data })
          }
          return
        }
        reject(res)
      },
      fail: (err) => reject(err)
    })
  })
}

export const get = (url, params = {}) => request({ url, method: 'GET', data: params })
export const post = (url, data = {}) => request({ url, method: 'POST', data })
export const put = (url, data = {}) => request({ url, method: 'PUT', data })
export const del = (url, data = {}) => request({ url, method: 'DELETE', data })

// 文件上传（聊天图片等）
export const upload = (url, filePath, formData = {}, name = 'file') => {
  const token = getToken()
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: buildUrl(url),
      filePath,
      name,
      formData,
      header: { Authorization: token ? `Bearer ${token}` : '' },
      success: (res) => {
        try {
          const data = typeof res.data === 'string' ? JSON.parse(res.data) : res.data
          if (data.code === 200 || data.code === 0) {
            resolve({ ...data, url: data.data || data.url })
          } else {
            uni.showToast({ title: data.message || '上传失败', icon: 'none' })
            reject(data)
          }
        } catch (e) {
          reject(res)
        }
      },
      fail: reject
    })
  })
}

// 供前端展示静态资源（如头像、banner）使用
// 简单文件名(banner.jpg等) -> /uploads/frontend-images/xxx；接口返回的 /uploads/xxx 直接拼接 baseURL
export const getBackendImageUrl = (path) => {
  if (!path) return ''
  if (path.startsWith('http')) return path
  let base = config.baseURL
  if (base.endsWith('/')) base = base.slice(0, -1)
  // 已是完整相对路径（如 /uploads/avatar.jpg）
  if (path.startsWith('/uploads/')) return base + path
  // 简单文件名 -> 后端 frontend-images 目录
  const name = path.startsWith('/') ? path.slice(1) : path
  return `${base}/uploads/frontend-images/${name}`
}


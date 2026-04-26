import { useAuthStore } from '../stores/auth'
import { createAuthExpiredError, redirectToAdminLogin } from './admin-auth-session'

const buildQuery = (params = {}) => {
  const search = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') return
    search.set(key, value)
  })
  const output = search.toString()
  return output ? `?${output}` : ''
}

export const request = async (url, options = {}) => {
  const authStore = useAuthStore()
  authStore.restore()
  const isLoginRequest = String(url || '').includes('/api/admin/auth/login')

  const response = await fetch(url, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(authStore.token ? { Authorization: `Bearer ${authStore.token}` } : {}),
      ...(options.headers || {})
    }
  })

  const payload = await response.json().catch(() => ({ code: response.status, message: '请求失败' }))
  if (!isLoginRequest && (response.status === 401 || payload.code === 401)) {
    authStore.clearSession()
    redirectToAdminLogin(window)
    throw createAuthExpiredError()
  }
  if (!response.ok || payload.code >= 400) {
    throw new Error(payload.message || '请求失败')
  }
  return payload.data
}

export const get = (url, params) => request(`${url}${buildQuery(params)}`)

export const post = (url, body) =>
  request(url, {
    method: 'POST',
    body: JSON.stringify(body || {})
  })

export const patch = (url, body) =>
  request(url, {
    method: 'PATCH',
    body: JSON.stringify(body || {})
  })

export const del = (url, body) =>
  request(url, {
    method: 'DELETE',
    body: body === undefined ? undefined : JSON.stringify(body || {})
  })

export const ADMIN_AUTH_EXPIRED_CODE = 'ADMIN_AUTH_EXPIRED'
export const ADMIN_AUTH_REDIRECT_FLAG = '__YUANBAN_ADMIN_AUTH_REDIRECTING__'
export const ADMIN_AUTH_EXPIRED_MESSAGE = '登录已过期，请重新登录。'

const ADMIN_BASE = '/admin'

const decodeBase64Url = (value = '') => {
  const normalized = String(value).replace(/-/g, '+').replace(/_/g, '/')
  const padded = normalized.padEnd(normalized.length + ((4 - (normalized.length % 4)) % 4), '=')
  if (typeof atob === 'function') return atob(padded)
  return Buffer.from(padded, 'base64').toString('utf8')
}

export const decodeJwtPayload = (token = '') => {
  const parts = String(token || '').split('.')
  if (parts.length < 2 || !parts[1]) return null
  try {
    return JSON.parse(decodeBase64Url(parts[1]))
  } catch {
    return null
  }
}

export const isJwtExpired = (token = '', nowSeconds = Math.floor(Date.now() / 1000)) => {
  const payload = decodeJwtPayload(token)
  if (!payload || typeof payload.exp !== 'number') return false
  return payload.exp <= nowSeconds
}

export const toAdminRoutePath = (pathname = '/', search = '') => {
  const current = `${pathname || '/'}${search || ''}`
  if (!current.startsWith(ADMIN_BASE)) return '/dashboard'
  const routePath = current.slice(ADMIN_BASE.length) || '/'
  return routePath.startsWith('/login') ? '' : routePath
}

export const buildAdminLoginUrl = (pathname = '/', search = '') => {
  const routePath = toAdminRoutePath(pathname, search)
  return routePath ? `${ADMIN_BASE}/login?redirect=${encodeURIComponent(routePath)}` : `${ADMIN_BASE}/login`
}

export const createAuthExpiredError = () => {
  const error = new Error(ADMIN_AUTH_EXPIRED_MESSAGE)
  error.code = ADMIN_AUTH_EXPIRED_CODE
  error.silent = true
  return error
}

export const isAuthExpiredError = (error) =>
  Boolean(error && (error.code === ADMIN_AUTH_EXPIRED_CODE || error.silent === true))

export const redirectToAdminLogin = (win = window) => {
  if (!win?.location) return
  win[ADMIN_AUTH_REDIRECT_FLAG] = true
  const target = buildAdminLoginUrl(win.location.pathname, win.location.search)
  if (`${win.location.pathname}${win.location.search}` === target) return
  if (typeof win.location.replace === 'function') {
    win.location.replace(target)
  } else {
    win.location.href = target
  }
}

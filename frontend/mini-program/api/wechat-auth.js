import { get, post } from '@/utils/api.js'

export const getWechatConfigStatus = (role = 'user') => get('/api/users/wechat/config-status', { role })

export const getWechatOAuthUrl = (role = 'user', platform = 'WECHAT_H5', redirectUrl = '') =>
  get('/api/users/wechat/oauth-url', { role, platform, redirectUrl })

export const loginByWechat = (code, role = 'user', platform = 'MINI_PROGRAM') =>
  post('/api/users/wechat/login', { code, role, platform })

export const bindWechatPhone = (payload = {}) => post('/api/users/wechat/bind-phone', payload)

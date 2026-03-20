import { get, post } from '@/utils/api.js'

export const getWechatConfigStatus = () => get('/api/users/wechat/config-status')

export const loginByWechat = (code, role = 'user') => post('/api/users/wechat/login', { code, role })

export const bindWechatPhone = (payload = {}) => post('/api/users/wechat/bind-phone', payload)

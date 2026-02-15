// 陪诊师相关API
import { get } from '../utils/api.js'

// 获取推荐陪诊师列表 (用于首页)
export const getRecommendedAttendants = () => {
	return get('/attendant/recommended')
}

// 获取陪诊师详情
export const getAttendantById = (id) => {
	return get(`/attendant/profile/${id}`)
}

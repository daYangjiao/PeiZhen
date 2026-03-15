import { get } from '@/utils/api.js'

export const getRecommendedAttendants = () => {
	return get('/attendant/recommended')
}

export const getAttendantById = (id) => {
	return get(`/attendant/profile/${id}`)
}


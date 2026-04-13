import { get, post } from '@/utils/api.js'

export async function submitMedicalQuestion(question, conversationId = '') {
  const payload = { question }
  if (conversationId) payload.conversationId = conversationId
  const res = await post('/ai/medical/qa', payload)
  return res?.data || null
}

export async function getMedicalQaRecord(recordId) {
  const res = await get(`/ai/medical/qa/${recordId}`)
  return res?.data || null
}

export async function getMedicalConversation(conversationId) {
  const res = await get(`/ai/medical/qa/conversation/${encodeURIComponent(conversationId)}`)
  return Array.isArray(res?.data) ? res.data : []
}

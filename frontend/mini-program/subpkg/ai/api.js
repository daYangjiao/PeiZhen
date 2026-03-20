import { post } from '@/utils/api.js'

export async function askMedicalQuestion(question) {
  try {
    const res = await post('/ai/medical/qa', { question })
    if (res && (res.code === 200 || res.code === 0) && res.data) return res.data
    if (res && res.answer) return res
    return null
  } catch (error) {
    console.error('AI 医疗问答请求失败:', error)
    return null
  }
}

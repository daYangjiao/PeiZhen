import { get, post } from '@/utils/api.js'

export async function createAiAppointmentSession(demandText) {
  const res = await post('/ai/guide/ai-appointment/session', { demandText })
  return res?.data || null
}

export async function getAiAppointmentSession(sessionId) {
  const res = await get(`/ai/guide/ai-appointment/session/${encodeURIComponent(sessionId)}`)
  return res?.data || null
}

export async function getLatestAiAppointmentSession() {
  const res = await get('/ai/guide/ai-appointment/session/latest')
  return res?.data || null
}

export async function replyAiAppointmentSession(sessionId, payload = {}) {
  const res = await post(`/ai/guide/ai-appointment/session/${encodeURIComponent(sessionId)}/reply`, payload)
  return res?.data || null
}

export async function startAiAppointmentMatch(sessionId) {
  const res = await post(`/ai/guide/ai-appointment/session/${encodeURIComponent(sessionId)}/match`)
  return res?.data || null
}

export async function createAiAppointmentOrder(appointmentNo, designatedAttendantId = null) {
  const payload = { appointmentNo }
  if (designatedAttendantId !== null && designatedAttendantId !== undefined) {
    payload.designatedAttendantId = Number(designatedAttendantId)
  }
  const res = await post('/ai/guide/orders', payload)
  return res?.data || null
}

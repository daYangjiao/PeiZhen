// src/api/aiaks.js

/**
 * 调用 AI 医疗问答接口
 * @param {string} question - 用户提问内容
 * @returns {Promise<{ answer: string, question: string } | null>}
 */
export async function askMedicalQuestion(question) {
  try {
    // 使用 uView 的 http 模块发起 POST 请求
    // 注意：这里的 URL 是相对于 api.js 中配置的 baseURL 的路径
    const res = await $u.http.post('/ai/medical/qa', { question });
    
    // 如果没有异常抛出，res.data 是后端返回的 JSON 对象
    // 假设后端返回格式是 { code: 200, message: "Success", data: { answer: "...", question: "..." } }
    // 根据你的实际返回结构，可能需要调整
    return res.data; // 返回整个响应数据，或者只返回 res.data.data
  } catch (error) {
    console.error('AI 医疗问答请求失败:', error);
    return null;
  }
}
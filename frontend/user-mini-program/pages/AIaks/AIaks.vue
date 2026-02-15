<template>
   <div class="chat-container">
      <!-- 聊天区域 -->
      <div class="chat-messages" ref="messagesContainer">
        <div v-for="(msg, index) in messages" :key="index" class="message-wrapper">
        <!-- AI 消息：靠左 -->
        <div v-if="msg.type === 'ai'" class="message ai">
                  <img src="/static/ai-avatar.png" alt="AI" class="ai-avatar" />
                  <div class="bubble ai-bubble">
                    <div class="name">智能医疗助手</div>
                    <div v-html="msg.text"></div>
                  </div>
                </div>

        <!-- 用户消息：靠右 -->
        <div v-else class="message user">
                  <div class="bubble user-bubble">
                    {{ msg.text }}
                  </div>
				   <img src="/static/user-avatar.jpg" alt="User" class="user-avatar" />
                </div>
              </div>
            </div>

    <!-- 输入区域 -->
    <div class="input-area">
      <!-- 医疗术语联想按钮组 -->
      <div class="suggestions-bottom">
        <div class="suggestion-title">💡 医疗术语联想</div>
        <div class="suggestion-buttons-bottom">
          <button
            v-for="(tag, index) in suggestions"
            :key="index"
            @click="selectTag(tag)"
            :class="['suggestion-btn-bottom', selectedTag === tag ? 'selected' : '']"
          >
            {{ tag }}
          </button>
        </div>
      </div>

      <!-- 输入框 -->
      <div class="input-wrapper">
        <input
          v-model="userInput"
          @keypress.enter="sendMessage"
          placeholder="请输入您的健康问题..."
          class="input-box"
        />
        <button @click="toggleMic" class="mic-btn">
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"></path>
            <path d="M19 10v2a7 7 0 0 1-14 0v-2"></path>
            <line x1="12" y1="19" x2="12" y2="23"></line>
            <line x1="8" y1="23" x2="16" y2="23"></line>
          </svg>
        </button>
      </div>
      <div class="input-actions">
        <button @click="addImage" class="action-btn">
          <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect>
            <circle cx="8.5" cy="8.5" r="1.5"></circle>
            <polyline points="21 15 16 10 5 21"></polyline>
          </svg>
        </button>
        <button @click="attachFile" class="action-btn">
          <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
            <polyline points="7 10 12 15 17 10"></polyline>
            <line x1="12" y1="15" x2="12" y2="3"></line>
          </svg>
        </button>
        <button @click="sendMessage" class="send-btn">发送</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import UserAvatar from '@/static/user-avatar.jpg'
import AIAvatar from '@/static/ai-avatar.png'
import { askMedicalQuestion } from '@/api/aiaks.js'

// 消息列表
const messages = ref([
  {
    type: 'ai',
    text: '您好！我是您的智能医疗助手，很高兴为您提供帮助。请问有什么健康问题需要咨询吗？'
  }
])

// 输入框
const userInput = ref('')
const selectedTag = ref('')

// 关键词建议
const suggestions = [
  '紧张性头痛',
  '过敏性咳嗽',
  '上呼吸道感染',
  '支气管炎',
  '胃炎',
  '鼻炎',
  '咽炎',
  '结膜炎'
]

// 滚动到底部
const messagesContainer = ref(null)
const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

// 点击术语按钮：仅填入输入框，不发送
const selectTag = (tag) => {
  selectedTag.value = tag
  userInput.value = tag
  // ❌ 不再自动触发 AI 回复！
}


  // 模拟 AI 回复（延迟后）
// const sendMessage = () => {
//   if (!userInput.value.trim()) return;

//   // ✅ 添加用户消息到聊天记录
//   messages.value.push({
//     type: 'user',
//     text: userInput.value
//   });

//   const userMsg = userInput.value;
//   userInput.value = '';

//   // 模拟 AI 回复
//   setTimeout(() => {
//     messages.value.push({
//       type: 'ai',
//       text: `您提到的是 <strong>${userMsg}</strong>，这是常见的健康问题。以下是建议：<br><strong>建议措施：</strong><ul><li>注意休息，避免过度劳累</li><li>保持饮食清淡，多吃蔬菜水果</li><li>如症状持续或加重，请及时就医</li></ul>`
//     });
//     scrollToBottom();
//   }, 800);
// }
const sendMessage = async () => {
  if (!userInput.value.trim()) return

  // 添加用户消息
  messages.value.push({
    type: 'user',
    text: userInput.value
  })

  const userMsg = userInput.value
  userInput.value = ''
  scrollToBottom()

//   // 显示“AI 正在思考...”
  messages.value.push({
    type: 'ai',
    text: '正在思考...'
  })
  scrollToBottom()

  // 调用真实 API
  const result = await askMedicalQuestion(userMsg)

  // 移除“正在思考”消息
  messages.value.pop()

  if (result) {
    messages.value.push({
      type: 'ai',
      text: result.answer // 注意：这里只取 answer
    })
  } else {
    messages.value.push({
      type: 'ai',
      text: '抱歉，当前无法回答您的问题，请稍后再试。'
    })
  }

  scrollToBottom()
}

// 其他功能（可选）
const toggleMic = () => alert('语音输入')
const addImage = () => alert('上传图片')
const attachFile = () => alert('上传文件')

onMounted(() => {
  scrollToBottom()
})


</script>

<style scoped>
.chat-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f5f7fa;
  overflow: hidden;
}

.chat-messages {
  flex: 1;
  padding: 5px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.message-wrapper {
  width: 100%;
  display: flex;
  flex-direction: column;
}

.message {
  display: flex;
    align-items: flex-start;
    max-width: 85%;
    margin: 0 auto;
}

/* AI 消息：靠左 */
.message.ai {
  justify-content: flex-start;
  margin-left: 10px;
}

.message.user {
  justify-content: flex-end;
  margin-right: 10px;
}

.ai-avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  margin-right: 10px;
  object-fit: cover;
  flex-shrink: 0;
}
.user-avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
    object-fit: cover;
    margin-left: 8px; /* 与气泡间距 */
  flex-shrink: 0;
}

/* 气泡样式*/
.bubble {
  padding: 12px 16px;
  border-radius: 18px;
  word-break: break-word;
  line-height: 1.5;
  font-size: 14px;
  /* position: relative; */
}

.ai-bubble {
  background-color: #ffffff;
  border: 1px solid #e0e0e0;
  color: #333;
  border-radius: 4px 18px 18px 18px;
   box-shadow: 0 1px 2px rgba(0,0,0,0.05);
}

.user-bubble {
  background-color: #007AFF;
  color: white;
  border-radius: 18px 4px 18px 18px; /* 右下角平一点，更像微信 */
  box-shadow: 0 1px 2px rgba(0,0,0,0.1);
}

.name {
  font-size: 12px;
  color: #333;
  margin-bottom: 4px;
}

.text {
  font-size: 14px;
  color: #333;
  line-height: 1.5;
}

.images {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
  margin-top: 12px;
}

.images img {
  width: 100%;
  height: 80px;
  object-fit: cover;
  border-radius: 8px;
}

.image-item {
  position: relative;
  overflow: hidden;
  border-radius: 8px;
}

.play-icon {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: rgba(0,0,0,0.5);
  border-radius: 50%;
  padding: 6px;
  color: white;
}

.list ul {
  margin: 8px 0 0;
  padding-left: 20px;
  font-size: 14px;
  color: #333;
}

.list li {
  margin: 4px 0;
}

/* 医疗术语联想按钮组（底部） */
.suggestions-bottom {
  max-width: 95%;
  padding: 10px;
  background-color: #f0f7ff;
  border-radius: 12px;
  border: 1px solid #b0d8ff;
  align-self: flex-start;
  /* margin-top: 0px; */
}

.suggestion-title {
  font-size: 12px;
  color: #007AFF;
  margin-bottom: 5px;
  font-weight: bold;
  display: flex;
  align-items: center;
  gap: 4px;
}

.suggestion-buttons-bottom {
  display: flex;
  flex-wrap: nowrap; /* 不换行 */
  gap: 8px;
  overflow-x: auto; /* 支持水平滚动 */
  padding: 4px 0;
  scrollbar-width: thin;
  scrollbar-color: #b0d8ff #f0f7ff;
}

.suggestion-buttons-bottom::-webkit-scrollbar {
  height: 4px;
}

.suggestion-buttons-bottom::-webkit-scrollbar-track {
  background: #f0f7ff;
  border-radius: 2px;
}

.suggestion-buttons-bottom::-webkit-scrollbar-thumb {
  background: #b0d8ff;
  border-radius: 2px;
}

.suggestion-btn-bottom {
  padding: 6px 12px;           /* 左右留足空间 */
  background-color: #e0eefc;
  border: 1px solid #b0d8ff;
  border-radius: 20px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
  color: #007AFF;
  white-space: nowrap;         /* 禁止换行 */
  text-align: center;
  flex-shrink: 0;              /* 关键！不让按钮被压缩 */
}

.suggestion-btn-bottom:hover {
  background-color: #d0e7ff;
}

.suggestion-btn-bottom.selected {
  background-color: #007AFF;
  color: white;
  border-color: #007AFF;
}

/* 输入区域 */
.input-area {
  padding: 16px;
  background-color: white;
  border-top: 1px solid #ddd;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.input-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  border: 1px solid #ccc;
  border-radius: 20px;
  padding: 8px 12px;
  background-color: #f8f9fa;
}

.input-box {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-size: 14px;
  padding: 4px 0;
}

.mic-btn {
  background: none;
  border: none;
  cursor: pointer;
  color: #666;
}

.input-actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 12px;
}

.action-btn {
  background: none;
  border: none;
  cursor: pointer;
  color: #666;
}

.send-btn {
  padding: 10px 20px;
  background-color: #007AFF;
  color: white;
  border: none;
  border-radius: 20px;
  font-size: 14px;
  cursor: pointer;
}

.send-btn:hover {
  background-color: #0056b3;
}

.highlight {
  background-color: #e8f4ff;
  padding: 12px;
  border-left: 4px solid #007AFF;
  border-radius: 8px;
  margin: 12px 0;
}
</style>
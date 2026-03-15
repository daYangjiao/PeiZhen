<template>
  <view class="container">
    <view class="chat-header">
      <view class="header-content">
        <view class="header-back" @click="navigateBack">
          <text class="back-text">←</text>
        </view>
        <view class="header-title">
          <view class="title-row">
            <text class="title-text">{{ targetName }}</text>
            <view class="online-dot"></view>
          </view>
          <text class="subtitle-text">{{ headerSubtitle }}</text>
        </view>
        <view class="header-placeholder-right"></view>
      </view>
    </view>
    <view class="header-placeholder"></view>

    <scroll-view 
      class="chat-list" 
      scroll-y 
      :scroll-top="scrollTop" 
      :scroll-into-view="scrollIntoView" 
      @scrolltolower="loadMoreHistory"
      upper-threshold="50"
      @click="closePanel"
    >
      <view v-if="loadingMore" class="loading-more"><text>加载中...</text></view>

      <view v-for="(msg, index) in messages" :key="msg.id || index" :id="'msg-' + index">
        
        <view v-if="shouldShowTime(index)" class="time-divider">
          <text>{{ formatTimeCenter(msg.createTime) }}</text>
        </view>

        <view class="message-item" :class="{ 'self': msg.senderId === currentUserId }">
          <image class="avatar" :src="getAvatar(msg)" @error="handleAvatarError" mode="aspectFill"></image>

          <view class="message-content">
            <text v-if="msg.senderId !== currentUserId" class="sender-name">{{ msg.senderName || targetName }}</text>
            
            <view class="bubble-container">
              <text v-if="msg.senderId === currentUserId && msg.status !== 'sending' && msg.status !== 'failed'" 
                class="read-status" :class="{ 'read': msg.isRead }">
                {{ msg.isRead ? '已读' : '未读' }}
              </text>

              <view v-if="msg.senderId === currentUserId && msg.status === 'sending'" class="loading-spinner"></view>
              <view v-if="msg.senderId === currentUserId && msg.status === 'failed'" class="fail-icon">!</view>

              <view class="content-bubble" :class="{'voice-bubble': msg.msgType === 3}">
                <!-- 文本 -->
                <text v-if="msg.msgType === 1" class="text">{{ msg.content }}</text>
                <!-- 图片 -->
                <image v-else-if="msg.msgType === 2" class="image" :src="getImageUrl(msg.content)" mode="widthFix" @click="previewImage(getImageUrl(msg.content))"></image>
                <!-- 语音 -->
                <view v-else-if="msg.msgType === 3" class="voice-content" @click="playVoice(msg.content)">
                  <image class="voice-icon-img" src="/static/icons/voice.png" mode="aspectFit"></image>
                  <text class="voice-text">语音消息</text>
                </view>
                <!-- 位置 -->
                <view v-else-if="msg.msgType === 4" class="location-content" @click="openLocation(msg.content)">
                  <image class="location-icon-img" src="/static/icons/location.png" mode="aspectFit"></image>
                  <view class="location-text-wrap">
                    <text class="location-name">{{ parseLocation(msg.content).name || '位置信息' }}</text>
                  </view>
                </view>
                <!-- 其他 -->
                <text v-else class="text">[未知消息类型]</text>
              </view>
            </view>
          </view>
        </view>
      </view>
      
      <view class="bottom-placeholder" :style="{ height: (showPanel ? 500 : 0) + 140 + 'rpx' }"></view>
    </scroll-view>

    <!-- 底部输入区域 -->
    <view class="footer-area" :style="{ bottom: keyboardHeight + 'px' }">
        <view class="input-toolbar">
            <!-- 语音切换 -->
            <view class="icon-btn" @click="switchVoiceMode">
                <image class="icon-img" :src="isVoiceMode ? '/static/icons/keyboard.png' : '/static/icons/voice.png'" mode="aspectFit"></image>
            </view>

            <!-- 输入框/按住说话 -->
            <view class="input-wrapper">
                <view v-if="isVoiceMode" class="voice-record-btn" :class="{ 'recording': recording }"
                    @touchstart="startRecord" @touchend="stopRecord" @touchcancel="cancelRecord">
                    <text>{{ recording ? '松开 结束' : '按住 说话' }}</text>
                </view>
                <input v-else class="input" v-model="inputText" confirm-type="send" @confirm="sendText"
                    :adjust-position="false" @focus="onInputFocus" @blur="onInputBlur" cursor-spacing="20" />
            </view>

            <!-- 表情 -->
            <view class="icon-btn" @click="toggleEmoji">
                <image class="icon-img" src="/static/icons/emoji.png" mode="aspectFit"></image>
            </view>

            <!-- 发送/更多 -->
            <view class="action-btn">
                <view v-if="inputText.trim() && !isVoiceMode" class="send-btn" :class="{ 'sending': isSending }" @click="sendText">
                    <text>{{ isSending ? '发送中...' : '发送' }}</text>
                </view>
                <view v-else class="icon-btn" @click="toggleMore">
                    <image class="icon-img" src="/static/icons/plus.png" mode="aspectFit"></image>
                </view>
            </view>
        </view>

        <!-- 功能面板 -->
        <view class="panel-area" v-if="showPanel">
            <!-- 表情面板 -->
            <scroll-view scroll-y v-if="panelType === 'emoji'" class="emoji-panel">
                <view class="emoji-grid">
                    <view v-for="(emoji, index) in emojiList" :key="index" class="emoji-item" @click="addEmoji(emoji)">
                        {{ emoji }}
                    </view>
                </view>
            </scroll-view>

            <!-- 更多功能面板 -->
            <view v-if="panelType === 'more'" class="more-panel">
                <view class="more-item" @click="chooseImage('album')">
                    <view class="more-icon-box">
                        <image class="more-icon-img" src="/static/icons/album.png" mode="aspectFit"></image>
                    </view>
                    <text class="more-text">相册</text>
                </view>
                <view class="more-item" @click="chooseImage('camera')">
                    <view class="more-icon-box">
                        <image class="more-icon-img" src="/static/icons/camera.png" mode="aspectFit"></image>
                    </view>
                    <text class="more-text">拍摄</text>
                </view>
                <view class="more-item" @click="chooseLocation">
                    <view class="more-icon-box">
                        <image class="more-icon-img" src="/static/icons/location.png" mode="aspectFit"></image>
                    </view>
                    <text class="more-text">位置</text>
                </view>
                <view class="more-item" @click="sendEmergency">
                    <view class="more-icon-box">
                        <image class="more-icon-img" src="/static/icons/emergency.png" mode="aspectFit"></image>
                    </view>
                    <text class="more-text">紧急</text>
                </view>
                <view class="more-item" @click="videoCall">
                    <view class="more-icon-box">
                        <image class="more-icon-img" src="/static/icons/video.png" mode="aspectFit"></image>
                    </view>
                    <text class="more-text">视频</text>
                </view>
                 <view class="more-item" @click="voiceCall">
                    <view class="more-icon-box">
                        <image class="more-icon-img" src="/static/icons/call.png" mode="aspectFit"></image>
                    </view>
                    <text class="more-text">通话</text>
                </view>
            </view>
        </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { get, post, upload, config } from '@/utils/api.js'
import { addChatListener, removeChatListener, connectChatSocket } from '@/utils/chat-websocket.js'

const currentUserId = ref(uni.getStorageSync('userInfo')?.id || 0)
const targetUserId = ref(null)
const targetName = ref('陪诊师')
const targetAvatar = ref('/static/doctor-avatar.png')
const messages = ref([])
const inputText = ref('')
const scrollTop = ref(0)
const scrollIntoView = ref('')
const loadingMore = ref(false)
const hasMoreHistory = ref(true)
const pageSize = 20
const currentPage = ref(1)
const isSending = ref(false)

// 新增状态
const isVoiceMode = ref(false)
const showPanel = ref(false)
const panelType = ref('') // 'emoji' | 'more'
const keyboardHeight = ref(0)
const recording = ref(false)
const recorderManager = uni.getRecorderManager()
const innerAudioContext = uni.createInnerAudioContext()
// 消息去重缓存
const processedMessages = new Set()

const emojiList = ['😀','😁','😂','🤣','😃','😄','😅','😆','😉','😊','😋','😎','😍','😘','🥰','😗','😙','😚','🙂','🤗','🤩','🤔','🤨','😐','😑','😶','🙄','😏','😣','😥','😮','🤐','😯','😪','😫','😴','😌','😛','😜','😝','🤤','😒','😓','😔','😕','🙃','🤑','😲','☹️','🙁','😖','😞','😟','😤','😢','😭','😦','😧','😨','😩','🤯','😬','😰','😱','🥵','🥶','😳','🤪','😵','😡','😠','🤬','😷','🤒','🤕','🤢','🤮','🤧','😇','🤠','🤡','🥳','🥴','🥺','🤥','🤫','🤭','🧐','🤓','😈','👿']

const headerSubtitle = computed(() => '患者 · 在线沟通中')

onLoad((options) => {
  uni.stopPullDownRefresh()

  if (!options.userId && !options.attendantId) { uni.navigateBack(); return; }
  targetUserId.value = parseInt(options.userId || options.attendantId)
  targetName.value = options.name || '陪诊师'
  if (options.avatar) targetAvatar.value = getImageUrl(options.avatar)

  connectChatSocket()
  loadHistory()

  // 监听键盘高度
  uni.onKeyboardHeightChange(res => {
      if (res.height > 0) {
          keyboardHeight.value = res.height
          showPanel.value = false // 键盘弹起时隐藏面板
          scrollToBottom()
      } else {
          keyboardHeight.value = 0
      }
  })

  // 录音监听
  recorderManager.onStop((res) => {
      if (recording.value) {
          sendVoice(res.tempFilePath)
          recording.value = false
      }
  })
})

onMounted(() => addChatListener(handleNewMessage))
onUnmounted(() => removeChatListener(handleNewMessage))

const shouldShowTime = (index) => {
  if (index === 0) return true
  const prevTime = new Date(messages.value[index - 1].createTime).getTime()
  const currTime = new Date(messages.value[index].createTime).getTime()
  return (currTime - prevTime) > 5 * 60 * 1000
}

const loadHistory = async () => {
  try {
    const res = await get(`/api/chat/history?targetUserId=${targetUserId.value}&page=1&pageSize=${pageSize}`)
    if (res.code === 200) {
      messages.value = res.data
      hasMoreHistory.value = res.data.length === pageSize
      setTimeout(() => scrollToBottom(), 100)
    }
  } catch (e) {}
}

const loadMoreHistory = async () => {
  if (loadingMore.value || !hasMoreHistory.value) return
  loadingMore.value = true
  try {
    currentPage.value++
    const res = await get(`/api/chat/history?targetUserId=${targetUserId.value}&page=${currentPage.value}&pageSize=${pageSize}`)
    if (res.code === 200 && res.data.length > 0) {
      messages.value = [...res.data.reverse(), ...messages.value]
      hasMoreHistory.value = res.data.length === pageSize
    } else {
      hasMoreHistory.value = false
    }
  } catch (e) { currentPage.value-- } finally { loadingMore.value = false }
}

const handleNewMessage = (msg) => {
  console.log('处理新消息:', msg)
  
  // 消息去重处理
  const msgKey = `${msg.senderId}-${msg.receiverId}-${msg.createTime}-${msg.content}`;
  if (processedMessages.has(msgKey)) {
    console.log('消息已处理，跳过:', msgKey);
    return;
  }
  processedMessages.add(msgKey);
  
  // 严格判断普通聊天消息类型，排除已读回执和其他系统消息
  const isNormalChatMsg = [1, 2, 3, 4].includes(Number(msg.msgType)) && 
                         msg.content !== 'READ_RECEIPT' && 
                         msg.type !== 'READ_RECEIPT' && 
                         msg.type !== 'MESSAGE_STATUS_UPDATE';

  if (isNormalChatMsg && (msg.senderId == targetUserId.value || msg.receiverId == targetUserId.value)) {
    if (msg.senderId == targetUserId.value) {
        // 来自对方的消息
        const newMsg = { ...msg };
        
        // 关键修复：确保消息包含发送者头像
        if (!newMsg.senderAvatar || newMsg.senderAvatar === '/static/user-placeholder.png') {
            // 使用预加载的目标用户头像
            newMsg.senderAvatar = targetAvatar.value || '/static/user-placeholder.png';
            console.log('设置发送者头像:', newMsg.senderAvatar);
        }
        
        // 避免重复添加相同ID的消息
        if (!messages.value.some(m => m.id == newMsg.id)) {
            messages.value.push(newMsg);
            scrollToBottom();
            markAsRead();
        }
    } else if (msg.receiverId == targetUserId.value) {
        // 自己发送的消息回显
        if (!messages.value.some(m => m.id == msg.id)) {
            messages.value.push(msg);
            scrollToBottom();
        }
    }
  }

  // 单独处理已读回执消息 - 只更新状态，不显示在聊天列表中
  if ((msg.type === 'READ_RECEIPT' || msg.content === 'READ_RECEIPT') && msg.msgType == 3) {
    console.log('收到已读回执，更新消息状态');
    const updatedMessages = messages.value.map(m => {
      // 只更新自己发送且未读的消息
      if (m.senderId == currentUserId.value && m.isRead !== 1) {
        console.log('标记消息为已读:', m.id);
        return { ...m, isRead: 1 };
      }
      return m;
    });
    
    // 使用响应式更新
    messages.value = updatedMessages;
    
    // 触发视图更新
    nextTick(() => {
      console.log('已读状态更新完成');
    });
    return; // 已读回执不添加到消息列表
  }
  
  // 处理其他类型的状态更新消息
  if (msg.type === 'MESSAGE_STATUS_UPDATE') {
    console.log('收到消息状态更新:', msg);
    const messageId = msg.messageId || msg.id;
    if (messageId) {
      messages.value = messages.value.map(m => {
        if (m.id == messageId) {
          return { ...m, ...msg.updates };
        }
        return m;
      });
    }
    return; // 状态更新消息不添加到列表
  }
}

// 发送逻辑封装
const sendMessage = async (content, type) => {
    if (isSending.value) return
    isSending.value = true
    
    const userInfo = uni.getStorageSync('userInfo')
    const tempMsg = {
      id: 'temp-' + Date.now(), senderId: currentUserId.value, receiverId: targetUserId.value, content: content, msgType: type,
      senderName: userInfo?.name || '我', senderAvatar: userInfo?.avatar, createTime: new Date(), status: 'sending', isRead: 0
    }
    messages.value.push(tempMsg)
    scrollToBottom()
    const tempIndex = messages.value.length - 1

    try {
      const res = await post('/api/chat/send', { receiverId: targetUserId.value, content: content, msgType: type })
      if (res.code === 200) messages.value[tempIndex] = { ...res.data, status: 'sent' }
      else throw new Error('Failed')
    } catch (e) { messages.value[tempIndex].status = 'failed' }
    finally {
      isSending.value = false
    }
}

const sendText = () => {
  if (!inputText.value.trim() || isSending.value) return
  sendMessage(inputText.value, 1)
  inputText.value = ''
}

const chooseImage = (sourceType) => {
  uni.chooseImage({
    count: 1,
    sourceType: sourceType ? [sourceType] : ['album', 'camera'],
    success: async (res) => {
      const path = res.tempFilePaths[0]
      // 先上传
      try {
        const uploadRes = await upload('/api/common/upload', path)
        if (uploadRes.code === 200) {
            sendMessage(uploadRes.url, 2)
        }
      } catch (e) { console.error(e) }
    }
  })
}

const sendVoice = async (path) => {
    try {
        const uploadRes = await upload('/api/common/upload', path)
        if (uploadRes.code === 200) {
            sendMessage(uploadRes.url, 3)
        }
    } catch (e) { console.error(e) }
}

const chooseLocation = () => {
    uni.chooseLocation({
        success: (res) => {
            const locationData = JSON.stringify({
                name: res.name,
                address: res.address,
                latitude: res.latitude,
                longitude: res.longitude
            })
            sendMessage(locationData, 4)
        }
    })
}

const sendEmergency = () => {
    sendMessage("【紧急求助】请立即联系我！", 1)
}

const videoCall = () => {
    uni.showToast({ title: '视频通话功能开发中', icon: 'none' })
}

const voiceCall = () => {
    uni.showToast({ title: '语音通话功能开发中', icon: 'none' })
}

// 交互逻辑
const switchVoiceMode = () => {
    isVoiceMode.value = !isVoiceMode.value
    if (isVoiceMode.value) {
        showPanel.value = false
        uni.hideKeyboard()
    } else {
        // 切换回键盘，自动聚焦
        nextTick(() => {
            // 实际开发中可能需要手动 focus
        })
    }
}

const toggleEmoji = () => {
    if (panelType.value === 'emoji' && showPanel.value) {
        showPanel.value = false
        // 切换回键盘
    } else {
        panelType.value = 'emoji'
        showPanel.value = true
        isVoiceMode.value = false
        uni.hideKeyboard()
        scrollToBottom()
    }
}

const toggleMore = () => {
    if (panelType.value === 'more' && showPanel.value) {
        showPanel.value = false
    } else {
        panelType.value = 'more'
        showPanel.value = true
        isVoiceMode.value = false
        uni.hideKeyboard()
        scrollToBottom()
    }
}

const closePanel = () => {
    showPanel.value = false
    uni.hideKeyboard()
}

const onInputFocus = (e) => {
    showPanel.value = false
    keyboardHeight.value = e.detail.height
    scrollToBottom()
}

const onInputBlur = () => {
    keyboardHeight.value = 0
}

const addEmoji = (emoji) => {
    inputText.value += emoji
}

const startRecord = () => {
    recording.value = true
    recorderManager.start()
}

const stopRecord = () => {
    // 录音结束在 onStop 中处理
    recorderManager.stop()
}

const cancelRecord = () => {
    recording.value = false
    recorderManager.stop() // 需要标记不发送
}

const playVoice = (url) => {
    innerAudioContext.src = getImageUrl(url)
    innerAudioContext.play()
}

const openLocation = (content) => {
    try {
        const loc = JSON.parse(content)
        uni.openLocation({
            latitude: loc.latitude,
            longitude: loc.longitude,
            name: loc.name,
            address: loc.address
        })
    } catch (e) {}
}

const parseLocation = (content) => {
    try { return JSON.parse(content) } catch(e) { return {} }
}

const markAsRead = () => { post(`/api/chat/read?senderId=${targetUserId.value}`) }
const navigateBack = () => {
  // 返回前通知消息页面更新状态
  uni.$emit('chat:return', { targetUserId: targetUserId.value })
  uni.navigateBack()
}
const scrollToBottom = () => { nextTick(() => { scrollIntoView.value = 'msg-' + (messages.value.length - 1) }) }
const getAvatar = (msg) => {
    if (msg.senderId === currentUserId.value) {
        // 当前用户头像
        const currentUserAvatar = uni.getStorageSync('userInfo')?.avatar
        const avatarUrl = currentUserAvatar || '/static/user-placeholder.png'
        console.log('当前陪诊师头像:', avatarUrl)
        return getImageUrl(avatarUrl)
    }
    
    // 对方用户头像
    if (msg.senderAvatar && msg.senderAvatar !== '/static/user-placeholder.png' && msg.senderAvatar !== '/static/doctor-avatar.png') {
        console.log('使用消息中的用户头像:', msg.senderAvatar)
        return getImageUrl(msg.senderAvatar)
    }
    
    // 使用预加载的目标用户头像
    if (targetAvatar.value && targetAvatar.value !== '/static/user-placeholder.png' && targetAvatar.value !== '/static/doctor-avatar.png') {
        console.log('使用预加载用户头像:', targetAvatar.value)
        return targetAvatar.value
    }
    
    // 默认使用用户占位头像
    console.log('使用默认用户头像')
    return '/static/user-placeholder.png'
}
const getImageUrl = (url) => {
  if (!url) return '/static/user-placeholder.png'; if (url.startsWith('http') || url.startsWith('wxfile')) return url;
  const baseUrl = config.baseURL.endsWith('/') ? config.baseURL.slice(0, -1) : config.baseURL; return baseUrl + (url.startsWith('/') ? url : '/' + url)
}
const previewImage = (url) => uni.previewImage({ urls: [url], current: url })

const handleAvatarError = (e) => {
    console.error('头像加载失败:', e.detail.errMsg);
    // 可以在这里设置默认头像或者重新加载
}

const formatTimeCenter = (time) => {
  if (!time) return ''; const d = new Date(time); const now = new Date();
  if (d.toDateString() === now.toDateString()) return `${d.getHours().toString().padStart(2,'0')}:${d.getMinutes().toString().padStart(2,'0')}`
  return `${d.getMonth()+1}月${d.getDate()}日 ${d.getHours().toString().padStart(2,'0')}:${d.getMinutes().toString().padStart(2,'0')}`
}
</script>

<style lang="scss" scoped>
$primary-color: #4A90E2; $bg-color: #F5F7FA; $text-main: #333; $bubble-other: #FFF; $bubble-self: $primary-color;

.container { height: 100vh; background-color: $bg-color; display: flex; flex-direction: column; }
.chat-header { position: fixed; top: 0; left: 0; width: 100%; z-index: 100; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding-top: var(--status-bar-height); box-shadow: 0 2rpx 12rpx rgba(74,144,226,0.15); }
.header-content { height: 88rpx; display: flex; align-items: center; justify-content: space-between; padding: 0 24rpx; }
.header-back { width: 60rpx; height: 60rpx; display: flex; align-items: center; .back-text { font-size: 44rpx; color: #fff; font-weight: 300; } }
.header-title { display: flex; flex-direction: column; align-items: flex-start; gap: 4rpx;
  .title-row { display: flex; align-items: center; gap: 10rpx; }
  .title-text { font-size: 32rpx; font-weight: 600; color: #fff; }
  .online-dot { width: 12rpx; height: 12rpx; background: #95f4b7; border-radius: 50%; box-shadow: 0 0 8rpx rgba(149,244,183,0.6); }
  .subtitle-text { font-size: 22rpx; color: rgba(255,255,255,0.85); }
}
.header-placeholder-right { width: 60rpx; }
.header-placeholder { width: 100%; height: calc(88rpx + var(--status-bar-height)); flex-shrink: 0; }

.chat-list { flex: 1; width: 100%; box-sizing: border-box; padding: 24rpx; overflow-y: scroll; -webkit-overflow-scrolling: touch; }
.time-divider { display: flex; justify-content: center; margin: 32rpx 0; text { font-size: 22rpx; color: #999; background: rgba(0,0,0,0.05); padding: 4rpx 16rpx; border-radius: 8rpx; } }
.loading-more { text-align: center; padding: 10rpx; font-size: 22rpx; color: #999; }

.message-item { display: flex; margin-bottom: 30rpx; align-items: flex-start; &.self { flex-direction: row-reverse; } }
.avatar { width: 80rpx; height: 80rpx; border-radius: 12rpx; flex-shrink: 0; box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.05); background: #fff; }
.message-content { display: flex; flex-direction: column; max-width: 70%; margin: 0 20rpx; .self & { align-items: flex-end; } }
.sender-name { font-size: 22rpx; color: #999; margin-bottom: 4rpx; margin-left: 8rpx; }

.bubble-container { display: flex; align-items: flex-end; gap: 10rpx; .self & { flex-direction: row-reverse; } }
.read-status { font-size: 20rpx; color: #999; margin-bottom: 10rpx; white-space: nowrap; &.read { color: #999; } }
.loading-spinner { width: 24rpx; height: 24rpx; border: 2rpx solid rgba(74,144,226,0.3); border-top-color: $primary-color; border-radius: 50%; animation: spin 1s linear infinite; margin-bottom: 10rpx; }
.fail-icon { width: 30rpx; height: 30rpx; background: #ff4d4f; color: #fff; border-radius: 50%; font-size: 20rpx; display: flex; align-items: center; justify-content: center; margin-bottom: 10rpx; }
@keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }

.content-bubble {
  position: relative; padding: 20rpx 24rpx; border-radius: 12rpx; font-size: 30rpx; line-height: 1.5; box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.06);
  background: $bubble-other; color: $text-main;
  .self & { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: #fff; }
  .image { max-width: 300rpx; border-radius: 8rpx; display: block; }
}

.voice-content {
  display: flex;
  align-items: center;
  gap: 12rpx;
}
.voice-icon-img {
  width: 32rpx;
  height: 32rpx;
}
.location-content {
  display: flex;
  align-items: center;
  gap: 12rpx;
}
.location-icon-img {
  width: 32rpx;
  height: 32rpx;
}
.location-name {
  font-size: 28rpx;
  color: $text-main;
}

.bottom-placeholder { height: 140rpx; transition: height 0.3s; }

/* 底部区域样式 */
.footer-area {
    position: fixed; left: 0; width: 100%; z-index: 100;
    background: #fff; border-top: 1rpx solid #E5E5E5;
    padding-bottom: constant(safe-area-inset-bottom);
    padding-bottom: env(safe-area-inset-bottom);
    transition: bottom 0.1s;
    box-shadow: 0 -2rpx 12rpx rgba(0,0,0,0.05);
}

.input-toolbar {
    display: flex; align-items: center; padding: 16rpx 20rpx;
    min-height: 100rpx; box-sizing: border-box;
}

.input-wrapper {
    flex: 1; margin: 0 20rpx;
}

.input {
    width: 100%; height: 72rpx; background: #F5F7FA; border-radius: 12rpx;
    padding: 0 20rpx; font-size: 30rpx; box-sizing: border-box; border: 1rpx solid transparent;
    &:focus { background: #fff; border-color: $primary-color; }
}

.voice-record-btn {
    width: 100%; height: 72rpx; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); border-radius: 12rpx;
    display: flex; align-items: center; justify-content: center;
    font-size: 30rpx; color: #fff; font-weight: 500;
    &.recording { background: linear-gradient(135deg, #764ba2 0%, #667eea 100%); }
}

.icon-btn {
    width: 60rpx; height: 60rpx; display: flex; align-items: center; justify-content: center;
}
.icon-img {
    width: 56rpx; height: 56rpx;
}

.action-btn {
    width: 100rpx; display: flex; align-items: center; justify-content: center; margin-left: 10rpx;
}

.send-btn {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: #fff; padding: 10rpx 24rpx; border-radius: 12rpx; font-size: 26rpx; font-weight: 500;
    box-shadow: 0 4rpx 12rpx rgba(102,126,234,0.3);
    &.sending { background: linear-gradient(135deg, #a8b5e8 0%, #b59fc9 100%); box-shadow: 0 2rpx 8rpx rgba(102,126,234,0.2); }
}

/* 面板区域 */
.panel-area {
    height: 500rpx; background: #fff; border-top: 1rpx solid #E5E5E5;
    overflow: hidden;
}

.emoji-panel {
    height: 100%; padding: 20rpx; box-sizing: border-box;
}
.emoji-grid {
    display: flex; flex-wrap: wrap;
}
.emoji-item {
    width: 12.5%; height: 80rpx; display: flex; align-items: center; justify-content: center; font-size: 40rpx;
}

.more-panel {
    height: 100%; display: flex; flex-wrap: wrap; padding: 40rpx 30rpx; box-sizing: border-box;
}
.more-item {
    width: 25%; display: flex; flex-direction: column; align-items: center; margin-bottom: 40rpx;
}
.more-icon-box {
    width: 110rpx; height: 110rpx; background: linear-gradient(135deg, #f5f7fa 0%, #e4e8ed 100%); border-radius: 24rpx;
    display: flex; align-items: center; justify-content: center; margin-bottom: 10rpx;
    box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.05);
}
.more-icon-img {
    width: 64rpx; height: 64rpx;
}
.more-text {
    font-size: 24rpx; color: #666;
}

.voice-content {
    display: flex; align-items: center; gap: 10rpx; min-width: 120rpx;
}
.location-content {
    display: flex; align-items: center; gap: 10rpx;
}
</style>
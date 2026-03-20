<template>
  <view class="container" :class="roleClass">
    <view class="chat-header">
      <view class="header-content">
        <view class="header-back header-side" @click="navigateBack">
          <image class="back-icon" src="/static/back.svg" mode="aspectFit"></image>
        </view>
        <view class="header-middle">
          <view class="header-title">
            <view class="title-row">
              <text class="title-text">{{ targetName }}</text>
              <view class="online-dot"></view>
            </view>
            <text class="subtitle-text">{{ headerSubtitle }}</text>
          </view>
        </view>
        <view class="header-side header-placeholder-right"></view>
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
                <text v-if="msg.msgType === 1" class="text">{{ msg.content }}</text>
                <image v-else-if="msg.msgType === 2" class="image" :src="getImageUrl(msg.content)" mode="widthFix" @click="previewImage(getImageUrl(msg.content))"></image>
                <view v-else-if="msg.msgType === 3" class="voice-content" @click="playVoice(msg.content)">
                  <image class="voice-icon-img" :src="voice" mode="aspectFit"></image>
                  <text class="voice-text">语音消息</text>
                </view>
                <view v-else-if="msg.msgType === 4" class="location-content" @click="openLocation(msg.content)">
                  <image class="location-icon-img" :src="location" mode="aspectFit"></image>
                  <view class="location-text-wrap">
                    <text class="location-name">{{ parseLocation(msg.content).name || '位置信息' }}</text>
                  </view>
                </view>
                <text v-else class="text">[未知消息类型]</text>
              </view>
            </view>
          </view>
        </view>
      </view>
      <view class="bottom-placeholder" :style="{ height: (showPanel ? 500 : 0) + 140 + 'rpx' }"></view>
    </scroll-view>

    <view class="footer-area" :style="{ bottom: keyboardHeight + 'px' }">
      <view class="input-toolbar">
        <view class="tool-slot">
          <view class="icon-btn" @click="switchVoiceMode">
            <image class="icon-img" :src="isVoiceMode ? keyboard : voice" mode="aspectFit"></image>
          </view>
        </view>
        <view class="input-wrapper">
          <view v-if="isVoiceMode" class="voice-record-btn" :class="{ 'recording': recording }"
            @touchstart="startRecord" @touchend="stopRecord" @touchcancel="cancelRecord">
            <text>{{ recording ? '松开 结束' : '按住 说话' }}</text>
          </view>
          <input v-else class="input" v-model="inputText" confirm-type="send" @confirm="sendText"
            :adjust-position="false" @focus="onInputFocus" @blur="onInputBlur" cursor-spacing="20" />
        </view>
        <view class="tool-slot">
          <view class="icon-btn" @click="toggleEmoji">
            <image class="icon-img" :src="emoji" mode="aspectFit"></image>
          </view>
        </view>
        <view class="action-btn">
          <view v-if="inputText.trim() && !isVoiceMode" class="send-btn" @click="sendText">
            <text>发送</text>
          </view>
          <view v-else class="icon-btn more-trigger" @click="toggleMore">
            <image class="icon-img" :src="plus" mode="aspectFit"></image>
          </view>
        </view>
      </view>

      <view class="panel-area" v-if="showPanel">
        <scroll-view scroll-y v-if="panelType === 'emoji'" class="emoji-panel">
          <view class="emoji-grid">
            <view v-for="(emoji, index) in emojiList" :key="index" class="emoji-item" @click="addEmoji(emoji)">
              {{ emoji }}
            </view>
          </view>
        </scroll-view>
        <view v-if="panelType === 'more'" class="more-panel">
          <view class="more-item" @click="chooseImage('album')">
            <view class="more-icon-box">
              <image class="more-icon-img" :src="album" mode="aspectFit"></image>
            </view>
            <text class="more-text">相册</text>
          </view>
          <view class="more-item" @click="chooseImage('camera')">
            <view class="more-icon-box">
              <image class="more-icon-img" :src="camera" mode="aspectFit"></image>
            </view>
            <text class="more-text">拍摄</text>
          </view>
          <view class="more-item" @click="chooseLocation">
            <view class="more-icon-box">
              <image class="more-icon-img" :src="location" mode="aspectFit"></image>
            </view>
            <text class="more-text">位置</text>
          </view>
          <view class="more-item" @click="sendEmergency">
            <view class="more-icon-box">
              <image class="more-icon-img" :src="emergency" mode="aspectFit"></image>
            </view>
            <text class="more-text">紧急</text>
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
import {
  chooseLocationWithGuard,
  createInnerAudioContext,
  createRecorderManager,
  openLocationWithGuard,
  showUnsupportedFeature
} from '@/subpkg/common/runtime.js'
import { album, camera, emoji, emergency, keyboard, location, plus, userPlaceholder, voice } from '@/utils/assets.js'

const currentUserId = ref(uni.getStorageSync('userInfo')?.id || 0)
const targetUserId = ref(null)
const role = ref(uni.getStorageSync('role') || '')
const targetName = ref(role.value === 'escort' ? '用户' : '陪诊师')
const targetAvatar = ref(userPlaceholder)
const messages = ref([])
const inputText = ref('')
const scrollTop = ref(0)
const scrollIntoView = ref('')
const loadingMore = ref(false)
const hasMoreHistory = ref(true)
const pageSize = 20
const currentPage = ref(1)

const isVoiceMode = ref(false)
const showPanel = ref(false)
const panelType = ref('')
const keyboardHeight = ref(0)
const recording = ref(false)
const recorderManager = createRecorderManager()
const innerAudioContext = createInnerAudioContext()
const processedMessages = new Set()

const emojiList = ['😀','😁','😂','🤣','😃','😄','😅','😆','😉','😊','😋','😎','😍','😘','🥰','😗','😙','😚','🙂','🤗','🤩','🤔','🤨','😐','😑','😶','🙄','😏','😣','😥','😮','🤐','😯','😪','😫','😴','😌','😛','😜','😝','🤤','😒','😓','😔','😕','🙃','🤑','😲','☹️','🙁','😖','😞','😟','😤','😢','😭','😦','😧','😨','😩','🤯','😬','😰','😱','🥵','🥶','😳','🤪','😵','😡','😠','🤬','😷','🤒','🤕','🤢','🤮','🤧','😇','🤠','🤡','🥳','🥴','🥺','🤥','🤫','🤭','🧐','🤓','😈','👿']

const headerSubtitle = computed(() => (role.value === 'escort' ? '患者 · 在线沟通中' : '陪诊师 · 在线沟通中'))
const roleClass = computed(() => (role.value === 'escort' ? 'role-escort' : 'role-user'))

onLoad((options) => {
  uni.stopPullDownRefresh()
  if (!options.userId && !options.attendantId) { uni.navigateBack(); return }
  targetUserId.value = parseInt(options.userId || options.attendantId)
  targetName.value = options.name || (role.value === 'escort' ? '用户' : '陪诊师')
  if (options.avatar) targetAvatar.value = getImageUrl(options.avatar)
  connectChatSocket()
  loadHistory()
  uni.onKeyboardHeightChange(res => {
    if (res.height > 0) {
      keyboardHeight.value = res.height
      showPanel.value = false
      scrollToBottom()
    } else {
      keyboardHeight.value = 0
    }
  })
  if (recorderManager) {
    recorderManager.onStop((res) => {
      if (recording.value) {
        sendVoice(res.tempFilePath)
        recording.value = false
      }
    })
  }
})

onMounted(() => {
  addChatListener(handleNewMessage)
})

onUnmounted(() => {
  removeChatListener(handleNewMessage)
  if (innerAudioContext && typeof innerAudioContext.destroy === 'function') {
    innerAudioContext.destroy()
  }
})

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
  } catch {}
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
  } catch {
    currentPage.value--
  } finally {
    loadingMore.value = false
  }
}

const handleNewMessage = (msg) => {
  const msgKey = `${msg.senderId}-${msg.receiverId}-${msg.createTime}-${msg.content}`
  if (processedMessages.has(msgKey)) return
  processedMessages.add(msgKey)
  const isNormalChatMsg = [1, 2, 3, 4].includes(Number(msg.msgType)) && 
    msg.content !== 'READ_RECEIPT' && 
    msg.type !== 'READ_RECEIPT' && 
    msg.type !== 'MESSAGE_STATUS_UPDATE'
  if (isNormalChatMsg && (msg.senderId == targetUserId.value || msg.receiverId == targetUserId.value)) {
    if (msg.senderId == targetUserId.value) {
      const newMsg = { ...msg }
      if (!newMsg.senderAvatar) newMsg.senderAvatar = targetAvatar.value || userPlaceholder
      if (!messages.value.some(m => m.id == newMsg.id)) {
        messages.value.push(newMsg)
        scrollToBottom()
        markAsRead()
      }
    } else if (msg.receiverId == targetUserId.value) {
      if (!messages.value.some(m => m.id == msg.id)) {
        messages.value.push(msg)
        scrollToBottom()
      }
    }
  }
}

const sendMessage = async (content, type) => {
  const userInfo = uni.getStorageSync('userInfo')
  const tempMsg = {
    id: 'temp-' + Date.now(),
    senderId: currentUserId.value,
    receiverId: targetUserId.value,
    content,
    msgType: type,
    senderName: userInfo?.name || '我',
    senderAvatar: userInfo?.avatar,
    createTime: new Date(),
    status: 'sending',
    isRead: 0
  }
  messages.value.push(tempMsg)
  scrollToBottom()
  const tempIndex = messages.value.length - 1
  try {
    const res = await post('/api/chat/send', { receiverId: targetUserId.value, content, msgType: type })
    if (res.code === 200) messages.value[tempIndex] = { ...res.data, status: 'sent' }
    else throw new Error('Failed')
  } catch {
    messages.value[tempIndex].status = 'failed'
  }
}

const sendingText = ref(false)
const sendText = () => {
  const text = (inputText.value || '').trim()
  if (!text) return
  if (sendingText.value) return
  sendingText.value = true
  inputText.value = ''
  sendMessage(text, 1)
  setTimeout(() => { sendingText.value = false }, 500)
}

const chooseImage = (sourceType) => {
  uni.chooseImage({
    count: 1,
    sourceType: sourceType ? [sourceType] : ['album', 'camera'],
    success: async (res) => {
      const path = res.tempFilePaths[0]
      try {
        const uploadRes = await upload('/api/common/upload', path)
        if (uploadRes.code === 200) sendMessage(uploadRes.url, 2)
      } catch {}
    }
  })
}

const sendVoice = async (path) => {
  try {
    const uploadRes = await upload('/api/common/upload', path)
    if (uploadRes.code === 200) sendMessage(uploadRes.url, 3)
  } catch {}
}

const chooseLocation = () => {
  chooseLocationWithGuard({
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
  sendMessage('【紧急求助】请立即联系我！', 1)
}

const switchVoiceMode = () => {
  if (!isVoiceMode.value && !recorderManager) {
    showUnsupportedFeature('录音', '公网 IP 的 HTTP 页面不支持录音，请改用小程序或 HTTPS')
    return
  }
  isVoiceMode.value = !isVoiceMode.value
  if (isVoiceMode.value) {
    showPanel.value = false
    uni.hideKeyboard()
  }
}

const toggleEmoji = () => {
  if (panelType.value === 'emoji' && showPanel.value) {
    showPanel.value = false
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
  if (!recorderManager) {
    showUnsupportedFeature('录音', '公网 IP 的 HTTP 页面不支持录音，请改用小程序或 HTTPS')
    return
  }
  recording.value = true
  recorderManager.start()
}

const stopRecord = () => {
  if (!recorderManager) return
  recorderManager.stop()
}

const cancelRecord = () => {
  recording.value = false
  if (!recorderManager) return
  recorderManager.stop()
}

const playVoice = (url) => {
  if (!innerAudioContext) {
    showUnsupportedFeature('语音播放', '当前环境不支持语音播放')
    return
  }
  innerAudioContext.src = getImageUrl(url)
  innerAudioContext.play()
}

const openLocation = (content) => {
  try {
    const loc = JSON.parse(content)
    openLocationWithGuard({
      latitude: loc.latitude,
      longitude: loc.longitude,
      name: loc.name,
      address: loc.address
    })
  } catch {}
}

const parseLocation = (content) => {
  try { return JSON.parse(content) } catch { return {} }
}

const markAsRead = () => {
  post(`/api/chat/read?senderId=${targetUserId.value}`)
}

const navigateBack = () => {
  uni.$emit('chat:return', { targetUserId: targetUserId.value })
  const pages = getCurrentPages()
  if (pages && pages.length > 1) {
    uni.navigateBack({
      fail: () => {
        if (role.value === 'escort') {
          uni.reLaunch({ url: '/pages/role-escort/message' })
        } else {
          uni.switchTab({ url: '/pages/role-user/message' })
        }
      }
    })
    return
  }
  if (role.value === 'escort') {
    uni.reLaunch({ url: '/pages/role-escort/message' })
  } else {
    uni.switchTab({ url: '/pages/role-user/message' })
  }
}

const scrollToBottom = () => {
  nextTick(() => {
    scrollIntoView.value = 'msg-' + (messages.value.length - 1)
  })
}

const getAvatar = (msg) => {
  if (msg.senderId === currentUserId.value) {
    const currentUserAvatar = uni.getStorageSync('userInfo')?.avatar
    const avatarUrl = currentUserAvatar || userPlaceholder
    return getImageUrl(avatarUrl)
  }
  if (msg.senderAvatar) return getImageUrl(msg.senderAvatar)
  if (targetAvatar.value) return targetAvatar.value
  return userPlaceholder
}

const getImageUrl = (url) => {
  if (!url) return userPlaceholder
  if (url.startsWith('http') || url.startsWith('wxfile')) return url
  const baseUrl = config.baseURL.endsWith('/') ? config.baseURL.slice(0, -1) : config.baseURL
  return baseUrl + (url.startsWith('/') ? url : '/' + url)
}

const previewImage = (url) => uni.previewImage({ urls: [url], current: url })

const handleAvatarError = () => {}

const formatTimeCenter = (time) => {
  if (!time) return ''
  const d = new Date(time)
  const now = new Date()
  if (d.toDateString() === now.toDateString()) return `${d.getHours().toString().padStart(2,'0')}:${d.getMinutes().toString().padStart(2,'0')}`
  return `${d.getMonth()+1}月${d.getDate()}日 ${d.getHours().toString().padStart(2,'0')}:${d.getMinutes().toString().padStart(2,'0')}`
}
</script>

<style lang="scss" scoped>
@import '@/styles/user-ui.scss';
@import '@/styles/escort-ui.scss';
$text-main: #1f2937;

.container {
  --chat-primary: #{$user-color-primary};
  --chat-primary-deep: #{$user-color-primary-deep};
  height: 100vh;
  background: linear-gradient(180deg, #eef5ff 0%, #f6f8fc 140rpx, #f5f7fa 100%);
  display: flex;
  flex-direction: column;
}

.container.role-escort {
  --chat-primary: #{$escort-color-primary};
  --chat-primary-deep: #{$escort-color-primary-deep};
}

.chat-header {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  z-index: 100;
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(8rpx);
  padding-top: var(--status-bar-height);
  border-bottom: 1rpx solid #e8eef6;
}

.header-content {
  height: 88rpx;
  display: grid;
  grid-template-columns: 72rpx 1fr 72rpx;
  align-items: center;
  column-gap: 8rpx;
  padding: 0 22rpx;
}

.header-side {
  width: 72rpx;
  min-width: 72rpx;
  max-width: 72rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.header-back {
  height: 72rpx;
  border-radius: 36rpx;
  justify-self: start;
  display: flex;
  align-items: center;
  justify-content: center;
}

.back-icon {
  width: 38rpx;
  height: 38rpx;
}

.header-title {
  min-width: 0;
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4rpx;
  text-align: center;
}

.header-middle {
  min-width: 0;
  width: 100%;
  display: flex;
  justify-content: center;
}

.title-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10rpx;
  max-width: 100%;
  width: 100%;
}

.title-text {
  font-size: 31rpx;
  font-weight: 700;
  color: $text-main;
  max-width: 460rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.online-dot {
  width: 12rpx;
  height: 12rpx;
  background: var(--chat-primary);
  border-radius: 50%;
  box-shadow: 0 0 0 6rpx rgba(102, 166, 255, 0.2);
}

.subtitle-text {
  font-size: 22rpx;
  color: #8a94a6;
  max-width: 520rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.header-placeholder-right {
  visibility: hidden;
}

.header-placeholder {
  width: 100%;
  height: calc(88rpx + var(--status-bar-height));
  flex-shrink: 0;
}

.chat-list {
  flex: 1;
  width: 100%;
  box-sizing: border-box;
  padding: 20rpx 22rpx 0;
  overflow-y: scroll;
  -webkit-overflow-scrolling: touch;
}

.time-divider {
  display: flex;
  justify-content: center;
  margin: 22rpx 0;
}

.time-divider text {
  font-size: 22rpx;
  color: #7f8a9b;
  background: rgba(255, 255, 255, 0.75);
  border: 1rpx solid #e5ebf3;
  padding: 6rpx 18rpx;
  border-radius: 999rpx;
}

.loading-more {
  text-align: center;
  padding: 10rpx;
  font-size: 22rpx;
  color: #8a94a6;
}

.message-item {
  display: flex;
  align-items: flex-start;
  margin-bottom: 22rpx;
}

.message-item.self {
  flex-direction: row-reverse;
}

.avatar {
  width: 74rpx;
  height: 74rpx;
  border-radius: 18rpx;
  flex-shrink: 0;
  background: #fff;
  border: 1rpx solid #e8edf5;
}

.message-content {
  display: flex;
  flex-direction: column;
  max-width: 72%;
  margin: 0 14rpx;
}

.message-item.self .message-content {
  align-items: flex-end;
}

.sender-name {
  font-size: 22rpx;
  color: #8a94a6;
  margin-bottom: 6rpx;
  margin-left: 8rpx;
}

.bubble-container {
  display: flex;
  align-items: flex-end;
  gap: 8rpx;
}

.message-item.self .bubble-container {
  flex-direction: row-reverse;
}

.read-status {
  font-size: 20rpx;
  color: #8a94a6;
  margin-bottom: 6rpx;
  white-space: nowrap;
}

.loading-spinner {
  width: 22rpx;
  height: 22rpx;
  border: 2rpx solid #c8d1df;
  border-top-color: var(--chat-primary);
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 8rpx;
}

.fail-icon {
  width: 28rpx;
  height: 28rpx;
  background: #ff4d4f;
  color: #fff;
  border-radius: 50%;
  font-size: 20rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 8rpx;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.content-bubble {
  position: relative;
  min-height: 40rpx;
  padding: 18rpx 22rpx;
  border-radius: 22rpx;
  font-size: 29rpx;
  line-height: 1.5;
  border: 1rpx solid #e7edf5;
  background: #fff;
  color: $text-main;
  box-shadow: 0 6rpx 18rpx rgba(31, 41, 55, 0.06);
}

.message-item.self .content-bubble {
  background: linear-gradient(135deg, var(--chat-primary), var(--chat-primary-deep));
  border-color: transparent;
  color: #fff;
}

.content-bubble .image {
  max-width: 300rpx;
  border-radius: 14rpx;
  display: block;
}

.voice-content,
.location-content {
  display: flex;
  align-items: center;
  gap: 10rpx;
}

.voice-icon-img,
.location-icon-img {
  width: 32rpx;
  height: 32rpx;
}

.location-name {
  font-size: 28rpx;
  color: inherit;
}

.bottom-placeholder {
  height: 160rpx;
  transition: height 0.3s;
}

.footer-area {
  position: fixed;
  left: 0;
  width: 100%;
  z-index: 100;
  background: #fff;
  border-top: 1rpx solid #e6edf5;
  padding-bottom: constant(safe-area-inset-bottom);
  padding-bottom: env(safe-area-inset-bottom);
  transition: bottom 0.1s;
}

.input-toolbar {
  display: flex;
  align-items: center;
  gap: 10rpx;
  padding: 14rpx 16rpx;
  min-height: 100rpx;
  box-sizing: border-box;
}

.tool-slot {
  width: 72rpx;
  min-width: 72rpx;
  max-width: 72rpx;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.icon-btn {
  width: 72rpx;
  height: 72rpx;
  border-radius: 36rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.icon-img {
  width: 46rpx;
  height: 46rpx;
}

.input-wrapper {
  flex: 1;
  min-width: 0;
  margin: 0;
}

.input,
.voice-record-btn {
  width: 100%;
  height: 74rpx;
  border-radius: 18rpx;
  border: 1rpx solid #e2e8f2;
  background: #f7f9fc;
  box-sizing: border-box;
}

.input {
  padding: 0 20rpx;
  font-size: 29rpx;
  color: #1f2937;
}

.voice-record-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28rpx;
  color: #425066;
}

.voice-record-btn.recording {
  background: #e8f1ff;
  border-color: #c7dbff;
}

.action-btn {
  width: 128rpx;
  min-width: 128rpx;
  max-width: 128rpx;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.send-btn {
  width: 100%;
  height: 72rpx;
  min-width: 0;
  padding: 0;
  border-radius: 36rpx;
  background: linear-gradient(135deg, var(--chat-primary), var(--chat-primary-deep));
  color: #fff;
  font-size: 28rpx;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}

.more-trigger {
  width: 72rpx;
}

.panel-area {
  height: 500rpx;
  background: #f7f9fc;
  border-top: 1rpx solid #e5ebf3;
  overflow: hidden;
}

.emoji-panel {
  height: 100%;
  padding: 20rpx;
  box-sizing: border-box;
}

.emoji-grid {
  display: flex;
  flex-wrap: wrap;
}

.emoji-item {
  width: 12.5%;
  height: 80rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 38rpx;
}

.more-panel {
  height: 100%;
  display: flex;
  flex-wrap: wrap;
  padding: 36rpx 20rpx;
  box-sizing: border-box;
}

.more-item {
  width: 25%;
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 28rpx;
}

.more-icon-box {
  width: 112rpx;
  height: 112rpx;
  border-radius: 26rpx;
  background: #fff;
  border: 1rpx solid #e5ebf3;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 10rpx;
}

.more-icon-img {
  width: 60rpx;
  height: 60rpx;
}

.more-text {
  font-size: 24rpx;
  color: #5f6b7b;
}
</style>

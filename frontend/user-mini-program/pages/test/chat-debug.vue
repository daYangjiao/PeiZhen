<template>
  <view class="container">
    <view class="header">
      <text class="title">聊天功能调试工具</text>
    </view>
    
    <view class="content">
      <!-- Token状态检查 -->
      <view class="section">
        <text class="section-title">认证状态</text>
        <view class="status-item">
          <text>Storage Token: {{ tokenStatus.storageToken ? '✓' : '✗' }}</text>
        </view>
        <view class="status-item">
          <text>UserInfo Token: {{ tokenStatus.userInfoToken ? '✓' : '✗' }}</text>
        </view>
        <view class="status-item">
          <text>API Token: {{ tokenStatus.apiToken ? '✓' : '✗' }}</text>
        </view>
        <view class="status-item">
          <text>认证状态: {{ isAuthenticated ? '已认证' : '未认证' }}</text>
        </view>
      </view>
      
      <!-- 接口测试 -->
      <view class="section">
        <text class="section-title">接口测试</text>
        <button class="test-btn" @click="testContacts">测试联系人接口</button>
        <button class="test-btn" @click="testHistory">测试聊天记录接口</button>
        <button class="test-btn" @click="testLogin">模拟登录</button>
        <button class="test-btn" @click="clearAuth">清除认证信息</button>
      </view>
      
      <!-- 测试结果 -->
      <view class="section">
        <text class="section-title">测试结果</text>
        <scroll-view class="result-container" scroll-y="true">
          <view v-for="(result, index) in testResults" :key="index" class="result-item">
            <text class="result-time">[{{ result.time }}]</text>
            <text class="result-message">{{ result.message }}</text>
          </view>
        </scroll-view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { get, post } from '@/utils/api.js'
import { getToken, setToken } from '@/utils/api.js'
import { useUserStore } from '@/stores/user.js'

const userStore = useUserStore()
const tokenStatus = ref({
  storageToken: false,
  userInfoToken: false,
  apiToken: false
})
const isAuthenticated = ref(false)
const testResults = ref([])

// 添加测试结果
const addResult = (message) => {
  testResults.value.unshift({
    time: new Date().toLocaleTimeString(),
    message: message
  })
  // 限制显示最近20条
  if (testResults.value.length > 20) {
    testResults.value.pop()
  }
}

// 检查认证状态
const checkAuthStatus = () => {
  const storageToken = uni.getStorageSync('token')
  const userInfo = uni.getStorageSync('userInfo')
  const apiToken = getToken()
  
  tokenStatus.value = {
    storageToken: !!storageToken,
    userInfoToken: !!(userInfo && userInfo.token),
    apiToken: !!apiToken
  }
  
  isAuthenticated.value = !!(storageToken || (userInfo && userInfo.token) || apiToken)
  
  addResult(`认证检查: Storage(${!!storageToken}) UserInfo(${!!(userInfo && userInfo.token)}) API(${!!apiToken})`)
}

// 测试联系人接口
const testContacts = async () => {
  try {
    addResult('开始测试联系人接口...')
    const response = await get('/api/chat/contacts')
    addResult(`联系人接口成功: ${JSON.stringify(response)}`)
  } catch (error) {
    addResult(`联系人接口失败: ${error.message || JSON.stringify(error)}`)
  }
}

// 测试聊天记录接口
const testHistory = async () => {
  try {
    addResult('开始测试聊天记录接口...')
    const response = await get('/api/chat/history?targetUserId=1')
    addResult(`聊天记录接口成功: ${JSON.stringify(response)}`)
  } catch (error) {
    addResult(`聊天记录接口失败: ${error.message || JSON.stringify(error)}`)
  }
}

// 模拟登录（使用测试账户）
const testLogin = async () => {
  try {
    addResult('开始模拟登录...')
    // 这里可以调用真实的登录接口或者使用测试token
    const fakeToken = 'eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjE1LCJzdWIiOiIxNSIsImlhdCI6MTczOTQ0NTIyMywiZXhwIjoxNzM5NTMxNjIzfQ.test-token'
    const fakeUserInfo = {
      id: 15,
      username: 'testuser',
      name: '测试用户',
      token: fakeToken
    }
    
    // 保存认证信息
    uni.setStorageSync('token', fakeToken)
    uni.setStorageSync('userInfo', fakeUserInfo)
    userStore.setUserInfo(fakeUserInfo)
    
    setToken(fakeToken)
    addResult('模拟登录成功')
    checkAuthStatus()
  } catch (error) {
    addResult(`模拟登录失败: ${error.message}`)
  }
}

// 清除认证信息
const clearAuth = () => {
  uni.removeStorageSync('token')
  uni.removeStorageSync('userInfo')
  userStore.clearUserInfo()
  addResult('已清除认证信息')
  checkAuthStatus()
}

onMounted(() => {
  addResult('调试工具初始化完成')
  checkAuthStatus()
})
</script>

<style scoped>
.container {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding: 20rpx;
}

.header {
  background: linear-gradient(135deg, #4A90E2, #357ABD);
  padding: 30rpx;
  border-radius: 16rpx;
  margin-bottom: 20rpx;
  text-align: center;
}

.title {
  color: white;
  font-size: 32rpx;
  font-weight: bold;
}

.section {
  background: white;
  border-radius: 16rpx;
  padding: 20rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.1);
}

.section-title {
  font-size: 28rpx;
  font-weight: bold;
  color: #333;
  display: block;
  margin-bottom: 16rpx;
  padding-bottom: 8rpx;
  border-bottom: 2rpx solid #4A90E2;
}

.status-item {
  padding: 12rpx 0;
  border-bottom: 1rpx solid #f0f0f0;
}

.status-item:last-child {
  border-bottom: none;
}

.test-btn {
  background: #4A90E2;
  color: white;
  border: none;
  border-radius: 8rpx;
  padding: 16rpx;
  margin: 10rpx 0;
  font-size: 26rpx;
  width: 100%;
}

.result-container {
  height: 300rpx;
  background: #f8f9fa;
  border-radius: 8rpx;
  padding: 16rpx;
}

.result-item {
  padding: 8rpx 0;
  border-bottom: 1rpx solid #eee;
}

.result-time {
  color: #666;
  font-size: 20rpx;
  margin-right: 10rpx;
}

.result-message {
  color: #333;
  font-size: 24rpx;
}
</style>
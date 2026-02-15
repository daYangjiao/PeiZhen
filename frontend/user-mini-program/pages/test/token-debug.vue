<template>
  <view class="container">
    <view class="header">
      <text class="title">Token调试工具</text>
    </view>
    
    <view class="content">
      <view class="info-section">
        <text class="section-title">Token状态</text>
        <view class="status-item">
          <text>Storage Token: {{ tokenStatus.storageToken ? '✓' : '✗' }}</text>
        </view>
        <view class="status-item">
          <text>User Info Token: {{ tokenStatus.userInfoToken ? '✓' : '✗' }}</text>
        </view>
        <view class="status-item">
          <text>API Token: {{ tokenStatus.apiToken ? '✓' : '✗' }}</text>
        </view>
        <view class="status-item">
          <text>Token有效: {{ tokenStatus.isValid ? '是' : '否' }}</text>
        </view>
      </view>
      
      <view class="action-section">
        <button class="test-btn" @click="checkToken">检查Token状态</button>
        <button class="test-btn" @click="testAPI">测试API请求</button>
        <button class="test-btn" @click="clearTokens">清除所有Token</button>
      </view>
      
      <view class="log-section">
        <text class="section-title">调试日志</text>
        <scroll-view class="log-container" scroll-y="true">
          <view v-for="(log, index) in logs" :key="index" class="log-item">
            <text>[{{ log.time }}] {{ log.message }}</text>
          </view>
        </scroll-view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { testTokenStatus, testAPIRequest } from '../../utils/token-test.js'
import { clearUserInfo, clearToken } from '../../utils/auth.js'

const tokenStatus = ref({
  storageToken: '',
  userInfoToken: '',
  apiToken: '',
  isValid: false
})

const logs = ref([])

const addLog = (message) => {
  const time = new Date().toLocaleTimeString()
  logs.value.push({
    time,
    message
  })
  console.log(`[${time}] ${message}`)
}

const checkToken = () => {
  addLog('开始检查Token状态...')
  const status = testTokenStatus()
  tokenStatus.value = status
  addLog(`检查完成 - Token${status.isValid ? '有效' : '无效'}`)
}

const testAPI = async () => {
  addLog('开始测试API请求...')
  try {
    const result = await testAPIRequest()
    addLog(`API测试结果: ${result.code === 200 ? '成功' : '失败'}`)
    if (result.data) {
      addLog(`用户信息: ${JSON.stringify(result.data)}`)
    }
  } catch (error) {
    addLog(`API测试失败: ${error.message}`)
  }
}

const clearTokens = () => {
  addLog('清除所有Token...')
  clearUserInfo()
  clearToken()
  uni.removeStorageSync('userInfo')
  uni.removeStorageSync('isLoggedIn')
  uni.removeStorageSync('token')
  addLog('Token清除完成')
  checkToken() // 重新检查状态
}

onMounted(() => {
  addLog('页面加载完成')
  checkToken()
})
</script>

<style scoped>
.container {
  padding: 20rpx;
}

.header {
  text-align: center;
  margin-bottom: 40rpx;
}

.title {
  font-size: 36rpx;
  font-weight: bold;
  color: #333;
}

.content {
}

.info-section, .action-section, .log-section {
  background: #fff;
  border-radius: 20rpx;
  padding: 30rpx;
  margin-bottom: 30rpx;
  box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.1);
}

.section-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
  margin-bottom: 20rpx;
  display: block;
}

.status-item {
  padding: 15rpx 0;
  border-bottom: 1rpx solid #eee;
}

.status-item:last-child {
  border-bottom: none;
}

.test-btn {
  width: 100%;
  height: 80rpx;
  background: #007aff;
  color: #fff;
  border: none;
  border-radius: 12rpx;
  font-size: 30rpx;
  margin-bottom: 20rpx;
}

.test-btn:active {
  background: #0062cc;
}

.log-container {
  height: 400rpx;
  background: #f5f5f5;
  border-radius: 12rpx;
  padding: 20rpx;
}

.log-item {
  padding: 10rpx 0;
  font-size: 24rpx;
  color: #666;
  border-bottom: 1rpx solid #eee;
}

.log-item:last-child {
  border-bottom: none;
}
</style>
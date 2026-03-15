<template>
  <view class="container">
    <!-- 未登录提示 -->
    <view v-if="!session.isLoggedIn" class="login-prompt-section">
      <view class="prompt-card">
        <image class="prompt-avatar" src="/static/doctor-avatar.png" mode="aspectFit"></image>
        <text class="prompt-title">您还未登录</text>
        <text class="prompt-desc">登录后可接单、查看订单与收入</text>
        <button class="login-btn" @click="goToLogin">立即登录</button>
      </view>
    </view>

    <!-- 已登录：陪诊师信息 -->
    <view v-else>
      <view class="user-card">
        <view class="user-info">
          <image class="avatar" :src="getFullAvatarUrl(attendantInfo.avatarUrl || attendantInfo.avatar)" mode="aspectFill"></image>
          <view class="user-details">
            <text class="name">{{ attendantInfo.name || attendantInfo.username || '陪诊师' }}</text>
            <text class="phone">{{ attendantInfo.phone || '暂无电话' }}</text>
            <view class="status">
              <text class="status-text online">在线接单</text>
            </view>
          </view>
          <view class="edit-btn" @click="editProfile">
            <text>编辑</text>
          </view>
        </view>
        <view class="stats">
          <view class="stat-item">
            <text class="stat-number">{{ attendantInfo.totalOrders || 0 }}</text>
            <text class="stat-label">总订单</text>
          </view>
          <view class="stat-item">
            <text class="stat-number">{{ attendantInfo.completedOrders || 0 }}</text>
            <text class="stat-label">已完成</text>
          </view>
          <view class="stat-item">
            <text class="stat-number">{{ attendantInfo.score || '5.0' }}</text>
            <text class="stat-label">评分</text>
          </view>
          <view class="stat-item">
            <text class="stat-number">¥{{ attendantInfo.totalEarnings || '0.00' }}</text>
            <text class="stat-label">总收入</text>
          </view>
        </view>
      </view>

      <view class="menu-section">
        <view class="menu-group">
          <view class="menu-item" @click="navigateTo">
            <image class="menu-icon" src="/static/ren_1.svg" mode="aspectFit"></image>
            <text class="menu-text">实名认证</text>
            <text class="menu-arrow">›</text>
          </view>
          <view class="menu-item" @click="navigateTo">
            <image class="menu-icon" src="/static/wujiaoxin.png" mode="aspectFit"></image>
            <text class="menu-text">我的钱包</text>
            <text class="menu-balance">¥{{ attendantInfo.balance || '0.00' }}</text>
            <text class="menu-arrow">›</text>
          </view>
          <view class="menu-item" @click="navigateTo">
            <image class="menu-icon" src="/static/time.png" mode="aspectFit"></image>
            <text class="menu-text">工作时间</text>
            <text class="menu-arrow">›</text>
          </view>
        </view>
        <view class="menu-group">
          <view class="menu-item" @click="navigateTo">
            <image class="menu-icon" src="/static/xin.png" mode="aspectFit"></image>
            <text class="menu-text">用户评价</text>
            <text class="menu-arrow">›</text>
          </view>
          <view class="menu-item" @click="navigateTo">
            <image class="menu-icon" src="/static/help.png" mode="aspectFit"></image>
            <text class="menu-text">帮助中心</text>
            <text class="menu-arrow">›</text>
          </view>
          <view class="menu-item" @click="navigateTo">
            <image class="menu-icon" src="/static/settings.png" mode="aspectFit"></image>
            <text class="menu-text">设置</text>
            <text class="menu-arrow">›</text>
          </view>
        </view>
      </view>

      <view class="logout-section">
        <button class="logout-btn" @click="logout">退出登录</button>
      </view>
    </view>

    <EscortBottomBar active="profile" />
  </view>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { get, config } from '@/utils/api.js'
import { useSessionStore } from '@/stores/session'
import { ensureRole } from '@/utils/auth-guard.js'
import EscortBottomBar from '@/components/EscortBottomBar.vue'

const session = useSessionStore()
const attendantInfo = ref({})

const getFullAvatarUrl = (relativePath) => {
  if (!relativePath) return '/static/doctor-avatar.png'
  if (relativePath.startsWith('http')) return relativePath
  const base = config.baseURL.endsWith('/') ? config.baseURL.slice(0, -1) : config.baseURL
  const path = relativePath.startsWith('/') ? relativePath : '/' + relativePath
  return base + path
}

const goToLogin = () => {
  uni.navigateTo({ url: '/pages/auth/login?role=escort' })
}

const loadAttendantInfo = async () => {
  const userInfo = uni.getStorageSync('userInfo')
  if (!userInfo || !userInfo.id) return
  try {
    const res = await get(`/attendant/profile/${userInfo.id}`)
    if (res.code === 200 && res.data) {
      attendantInfo.value = res.data
    } else {
      attendantInfo.value = userInfo
    }
  } catch (e) {
    attendantInfo.value = userInfo || {}
  }
}

const editProfile = () => {
  uni.showToast({ title: '编辑功能开发中', icon: 'none' })
}

const navigateTo = () => {
  uni.showToast({ title: '功能开发中', icon: 'none' })
}

const logout = () => {
  uni.showModal({
    title: '确认退出',
    content: '确定要退出登录吗？',
    success: (res) => {
      if (res.confirm) session.logout()
    }
  })
}

onMounted(() => {
  session.restoreFromStorage()
  if (session.isLoggedIn) loadAttendantInfo()
})

onShow(() => {
  if (!ensureRole('escort')) return
  session.restoreFromStorage()
  if (session.isLoggedIn) loadAttendantInfo()
})
</script>

<style lang="scss" scoped>
.container {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding: 20rpx;
  padding-bottom: calc(48rpx + env(safe-area-inset-bottom));
}

.login-prompt-section {
  padding: 80rpx 32rpx;
}
.prompt-card {
  background: linear-gradient(135deg, #4A90E2 0%, #357ABD 100%);
  border-radius: 24rpx;
  padding: 80rpx 48rpx;
  text-align: center;
}
.prompt-avatar {
  width: 140rpx;
  height: 140rpx;
  margin-bottom: 32rpx;
  opacity: 0.9;
}
.prompt-title {
  font-size: 36rpx;
  font-weight: bold;
  color: white;
  display: block;
  margin-bottom: 16rpx;
}
.prompt-desc {
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.9);
  display: block;
  margin-bottom: 48rpx;
}
.login-btn {
  background: white;
  color: #4A90E2;
  height: 88rpx;
  line-height: 88rpx;
  border-radius: 44rpx;
  font-size: 32rpx;
  font-weight: 600;
  border: none;
}
.login-btn::after { border: none; }

.user-card {
  background: #fff;
  border-radius: 20rpx;
  padding: 40rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.06);
}
.user-info {
  display: flex;
  align-items: center;
  margin-bottom: 36rpx;
}
.avatar {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  margin-right: 28rpx;
  border: 4rpx solid #4A90E2;
}
.user-details { flex: 1; }
.name { font-size: 34rpx; font-weight: 600; color: #333; display: block; margin-bottom: 8rpx; }
.phone { font-size: 26rpx; color: #666; display: block; margin-bottom: 12rpx; }
.status-text {
  font-size: 22rpx;
  padding: 8rpx 16rpx;
  border-radius: 8rpx;
  background: rgba(82, 196, 26, 0.1);
  color: #52C41A;
}
.edit-btn {
  padding: 16rpx 28rpx;
  background: #f5f7fa;
  border-radius: 32rpx;
  font-size: 26rpx;
  color: #666;
}
.stats {
  display: flex;
  justify-content: space-around;
  padding-top: 24rpx;
  border-top: 1rpx solid #f0f0f0;
}
.stat-item { text-align: center; }
.stat-number { font-size: 32rpx; font-weight: 700; color: #4A90E2; display: block; margin-bottom: 8rpx; }
.stat-label { font-size: 24rpx; color: #999; }

.menu-section { margin-top: 24rpx; }
.menu-group {
  background: #fff;
  border-radius: 20rpx;
  margin-bottom: 24rpx;
  overflow: hidden;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
}
.menu-item {
  display: flex;
  align-items: center;
  padding: 32rpx 28rpx;
  border-bottom: 1rpx solid #f5f5f5;
}
.menu-item:last-child { border-bottom: none; }
.menu-icon { width: 44rpx; height: 44rpx; margin-right: 24rpx; }
.menu-text { flex: 1; font-size: 28rpx; color: #333; }
.menu-balance { font-size: 28rpx; color: #4A90E2; font-weight: 600; margin-right: 16rpx; }
.menu-arrow { font-size: 28rpx; color: #999; }

.logout-section { margin-top: 32rpx; }
.logout-btn {
  width: 100%;
  height: 96rpx;
  background: linear-gradient(135deg, #FF4D4F, #ff3838);
  color: white;
  border: none;
  border-radius: 48rpx;
  font-size: 32rpx;
  font-weight: 600;
}
.logout-btn::after { border: none; }
</style>

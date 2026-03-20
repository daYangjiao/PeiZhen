<template>
  <view class="profile-page">
    <view v-if="!session.isLoggedIn" class="login-prompt-section">
      <view class="prompt-card">
        <image class="prompt-avatar" src="/static/doctor-avatar.png" mode="aspectFit"></image>
        <text class="prompt-title">您还未登录</text>
        <text class="prompt-desc">登录后可接单、查看订单与收入</text>
        <view class="login-btn" @click="goToLogin">
          <text>立即登录</text>
        </view>
      </view>
    </view>

    <view v-else>
      <view class="header-card">
        <view class="header-main">
          <view class="identity-block">
            <image
              class="avatar"
              :src="getFullAvatarUrl(attendantInfo.avatarUrl || attendantInfo.avatar)"
              mode="aspectFill"
            ></image>
            <view class="identity-content">
              <text class="name">{{ displayName }}</text>
              <text class="sub-info">工号 {{ attendantInfo.id || '--' }} | {{ attendantInfo.phone || '暂无电话' }}</text>
              <view class="status-wrap">
                <text class="online-tag">在线接单</text>
              </view>
            </view>
          </view>
          <view class="edit-btn" @click="editProfile">
            <text>编辑</text>
          </view>
        </view>
      </view>

      <view class="dashboard-grid">
        <view
          class="data-card"
          v-for="card in dashboardCards"
          :key="card.key"
          @click="tapCard(card)"
        >
          <text class="data-value">{{ card.value }}</text>
          <text class="data-label">{{ card.label }}</text>
        </view>
      </view>

      <view class="menu-section">
        <view class="menu-group" v-for="(group, groupIndex) in menuGroups" :key="`group-${groupIndex}`">
          <view class="menu-row" v-for="(item, itemIndex) in group" :key="item.key">
            <EscortMenuCell
              :icon="item.icon"
              :title="item.title"
              :balance="item.balance"
              :status-text="item.statusText"
              :status-type="item.statusType"
              @click="onMenuTap(item)"
            />
            <view class="cell-divider" v-if="itemIndex !== group.length - 1"></view>
          </view>
        </view>
      </view>

      <view class="logout-wrap">
        <view class="logout-btn" @click="logout">
          <text>退出登录</text>
        </view>
      </view>
    </view>

    <EscortBottomBar active="profile" />
  </view>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { onShow } from '@dcloudio/uni-app'
import EscortBottomBar from '@/components/EscortBottomBar.vue'
import EscortMenuCell from '@/components/EscortMenuCell.vue'
import { useSessionStore } from '@/stores/session'
import { useUserStore } from '@/stores/user'
import { clearToken, config } from '@/utils/api.js'
import { ensureRole } from '@/utils/auth-guard.js'
import {
  escortRules,
  escortServiceCenter,
  escortStats,
  escortWallet,
  help,
  order,
  ren1,
  settings,
  userPlaceholder,
  xin
} from '@/utils/assets.js'

const session = useSessionStore()
const userStore = useUserStore()
const { attendantInfo, todayService, monthService, totalIncome, praiseRate, balance } = storeToRefs(userStore)

const formatAmount = (value) => {
  const num = Number(value || 0)
  return Number.isFinite(num) ? num.toFixed(2) : '0.00'
}

const displayName = computed(() => attendantInfo.value.name || attendantInfo.value.username || '陪诊师')

const dashboardCards = computed(() => [
  { key: 'todayService', label: '今日服务', value: `${todayService.value}` },
  { key: 'monthService', label: '本月服务', value: `${monthService.value}` },
  { key: 'totalIncome', label: '累计收入', value: `¥${formatAmount(totalIncome.value)}` },
  { key: 'praiseRate', label: '好评率', value: `${praiseRate.value}%` }
])

const qualificationStatusType = computed(() => {
  const code = Number(attendantInfo.value.qualificationStatusCode || 0)
  if (code === 1) return 'verified'
  if (code === 0) return 'pending'
  if (code === 3) return 'failed'
  if (code === 2) return 'blocked'
  return 'default'
})

const menuGroups = computed(() => [
  [
    {
      key: 'walletDetail',
      title: '钱包明细',
      icon: escortWallet,
      balance: balance.value,
      route: '/subpkg/profile/wallet-detail'
    },
    {
      key: 'qualification',
      title: '资质管理',
      icon: ren1,
      statusText: attendantInfo.value.qualificationStatusText || '待审核',
      statusType: qualificationStatusType.value,
      route: '/subpkg/profile/qualification'
    }
  ],
  [
    {
      key: 'reviews',
      title: '我的评价',
      icon: xin,
      route: '/subpkg/profile/reviews'
    },
    {
      key: 'historyOrders',
      title: '历史订单',
      icon: order,
      route: '/pages/role-escort/order',
      mode: 'relaunch'
    },
    {
      key: 'serviceStats',
      title: '服务统计',
      icon: escortStats,
      route: '/subpkg/profile/service-stats'
    }
  ],
  [
    {
      key: 'platformRules',
      title: '平台规则',
      icon: escortRules,
      route: '/subpkg/profile/platform-rules'
    },
    {
      key: 'serviceCenter',
      title: '客服中心',
      icon: escortServiceCenter,
      route: '/subpkg/profile/service-center'
    },
    {
      key: 'helpCenter',
      title: '帮助中心',
      icon: help,
      route: '/subpkg/profile/help'
    },
    {
      key: 'settings',
      title: '设置',
      icon: settings,
      route: '/subpkg/profile/settings'
    }
  ]
])

const getFullAvatarUrl = (relativePath) => {
  if (!relativePath) {
    return userPlaceholder
  }
  if (relativePath.startsWith('http')) {
    return relativePath
  }
  const baseUrl = config.baseURL.endsWith('/') ? config.baseURL : `${config.baseURL}/`
  const avatarPath = relativePath.startsWith('/') ? relativePath.slice(1) : relativePath
  return `${baseUrl}${avatarPath}`
}

const loadProfile = async () => {
  session.restoreFromStorage()
  if (!session.isLoggedIn) return

  const userInfo = session.userInfo || uni.getStorageSync('userInfo')
  if (!userInfo || !userInfo.id) return

  if (!userStore.restoreAttendantInfo()) {
    userStore.setAttendantInfo(userInfo)
  }
  await userStore.fetchAttendantProfile(userInfo.id)
}

const goToLogin = () => {
  uni.navigateTo({ url: '/pages/auth/login?role=escort' })
}

const editProfile = () => {
  uni.navigateTo({ url: '/subpkg/profile/edit-escort' })
}

const tapCard = (card) => {
  if (card.key === 'totalIncome') {
    onMenuTap({ title: '钱包明细', route: '/subpkg/profile/wallet-detail' })
    return
  }
  if (card.key === 'praiseRate') {
    onMenuTap({ title: '我的评价', route: '/subpkg/profile/reviews' })
    return
  }
  onMenuTap({ title: '服务统计', route: '/subpkg/profile/service-stats' })
}

const onMenuTap = (item) => {
  if (!item.route) {
    uni.showToast({ title: `${item.title}开发中`, icon: 'none' })
    return
  }

  if (item.mode === 'relaunch') {
    uni.reLaunch({ url: item.route })
    return
  }

  uni.navigateTo({
    url: item.route,
    fail: () => {
      uni.showToast({ title: `${item.title}开发中`, icon: 'none' })
    }
  })
}

const logout = () => {
  uni.showModal({
    title: '确认退出',
    content: '确定要退出登录吗？',
    success: (res) => {
      if (!res.confirm) return
      clearToken()
      session.logout()
      userStore.clearAttendantInfo()
      uni.showToast({ title: '已退出登录', icon: 'success' })
      setTimeout(() => {
        uni.reLaunch({ url: '/pages/auth/login?role=escort' })
      }, 700)
    }
  })
}

onMounted(() => {
  session.restoreFromStorage()
  if (session.isLoggedIn) {
    loadProfile()
  }
})

onShow(() => {
  if (!ensureRole('escort')) return
  loadProfile()
})
</script>

<style lang="scss" scoped>
@import '@/styles/escort-ui.scss';
.profile-page {
  --escort-primary: #{$escort-color-primary};
  min-height: 100vh;
  background: $escort-color-bg;
  padding: 24rpx 24rpx calc(148rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
}

.login-prompt-section {
  padding-top: 60rpx;
}

.prompt-card {
  background: linear-gradient(135deg, $escort-color-primary 0%, $escort-color-primary-deep 100%);
  border-radius: $escort-radius-card;
  padding: 68rpx 40rpx;
  text-align: center;
}

.prompt-avatar {
  width: 130rpx;
  height: 130rpx;
  margin-bottom: 26rpx;
  opacity: 0.94;
}

.prompt-title {
  display: block;
  font-size: 34rpx;
  font-weight: 700;
  color: #ffffff;
  margin-bottom: 14rpx;
}

.prompt-desc {
  display: block;
  font-size: 25rpx;
  color: rgba(255, 255, 255, 0.92);
  margin-bottom: 36rpx;
}

.login-btn {
  height: 84rpx;
  border-radius: 42rpx;
  background: #ffffff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 52rpx;

  text {
    color: #66a6ff;
    font-size: 30rpx;
    font-weight: 600;
  }
}

.header-card {
  background: $escort-color-surface;
  border-radius: $escort-radius-card;
  padding: 28rpx;
  box-shadow: $escort-shadow-card;
  position: relative;
  overflow: hidden;

  &::after {
    content: '';
    position: absolute;
    right: -60rpx;
    top: -80rpx;
    width: 240rpx;
    height: 240rpx;
    border-radius: 50%;
    background: rgba(74, 144, 226, 0.08);
  }
}

.header-main {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.identity-block {
  display: flex;
  align-items: center;
}

.avatar {
  width: 120rpx;
  height: 120rpx;
  border-radius: 999rpx;
  border: 4rpx solid var(--escort-primary);
  box-shadow: 0 12rpx 26rpx rgba(74, 144, 226, 0.28);
}

.identity-content {
  margin-left: 20rpx;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.name {
  font-size: 36rpx;
  font-weight: 700;
  color: #1f2937;
}

.sub-info {
  font-size: 24rpx;
  color: #6b7280;
}

.status-wrap {
  margin-top: 4rpx;
}

.online-tag {
  position: relative;
  display: inline-block;
  padding: 8rpx 16rpx 8rpx 30rpx;
  border-radius: 999rpx;
  background: rgba(82, 196, 26, 0.14);
  color: #2e8b1f;
  font-size: 22rpx;
  border: 1rpx solid rgba(82, 196, 26, 0.28);

  &::before {
    content: '';
    position: absolute;
    left: 12rpx;
    top: 50%;
    transform: translateY(-50%);
    width: 10rpx;
    height: 10rpx;
    border-radius: 50%;
    background: #52c41a;
    animation: pulse 1.8s infinite ease-in-out;
  }
}

.edit-btn {
  min-width: 80rpx;
  height: 56rpx;
  padding: 0 16rpx;
  border-radius: 999rpx;
  background: #eff6ff;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1rpx solid rgba(74, 144, 226, 0.2);
  transition: all 0.2s ease;

  text {
    color: var(--escort-primary);
    font-size: 24rpx;
    font-weight: 600;
  }

  &:active {
    transform: scale(0.96);
    background: rgba(74, 144, 226, 0.14);
  }
}

.dashboard-grid {
  margin-top: 20rpx;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18rpx;
}

.data-card {
  background: $escort-color-surface;
  border-radius: $escort-radius-card;
  padding: 24rpx;
  box-shadow: $escort-shadow-card;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
  transition: transform 0.2s ease, box-shadow 0.2s ease;

  &:active {
    transform: scale(0.98);
    box-shadow: 0 5rpx 14rpx rgba(31, 41, 55, 0.1);
  }
}

.data-value {
  font-size: 38rpx;
  font-weight: 700;
  color: var(--escort-primary);
}

.data-label {
  font-size: 24rpx;
  color: #6b7280;
}

.menu-section {
  margin-top: 20rpx;
}

.menu-group {
  background: $escort-color-surface;
  border-radius: $escort-radius-card;
  overflow: hidden;
  margin-bottom: 18rpx;
  box-shadow: $escort-shadow-card;
  animation: slideUp 0.45s ease both;

  &:nth-child(2) {
    animation-delay: 0.08s;
  }

  &:nth-child(3) {
    animation-delay: 0.16s;
  }
}

.menu-row {
  background: #ffffff;
}

.cell-divider {
  height: 1rpx;
  margin-left: 24rpx;
  margin-right: 24rpx;
  background: #eef2f7;
}

.logout-wrap {
  margin-top: 8rpx;
}

.logout-btn {
  height: $escort-btn-height;
  border-radius: $escort-radius-card;
  background: linear-gradient(135deg, var(--escort-primary), #{$escort-color-primary-deep});
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: $escort-shadow-primary;
  transition: transform 0.2s ease;

  text {
    color: #ffffff;
    font-size: 30rpx;
    font-weight: 600;
  }

  &:active {
    transform: scale(0.985);
  }
}

@keyframes pulse {
  0% {
    transform: translateY(-50%) scale(1);
    opacity: 1;
  }
  50% {
    transform: translateY(-50%) scale(1.35);
    opacity: 0.6;
  }
  100% {
    transform: translateY(-50%) scale(1);
    opacity: 1;
  }
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20rpx);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>

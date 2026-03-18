<template>
  <view class="page">
    <view class="hero-card slide-up delay-1">
      <view class="hero-left">
        <image class="hero-icon" src="/static/escort-stats.svg" mode="aspectFit"></image>
        <view>
          <text class="hero-title">服务统计</text>
          <text class="hero-desc">数据看板帮助你快速了解服务表现</text>
        </view>
      </view>
    </view>

    <view class="grid slide-up delay-1">
      <view class="card"><text class="k">今日服务</text><text class="v">{{ todayService }}</text></view>
      <view class="card"><text class="k">本月服务</text><text class="v">{{ monthService }}</text></view>
      <view class="card"><text class="k">累计收入</text><text class="v">¥{{ formatMoney(totalIncome) }}</text></view>
      <view class="card"><text class="k">好评率</text><text class="v">{{ praiseRate }}%</text></view>
    </view>

    <view class="panel slide-up delay-2">
      <view class="panel-head">
        <text class="title">近7日服务趋势</text>
        <text class="refresh" @click="loadStats">刷新</text>
      </view>
      <view class="trend-wrap" v-if="trend.length">
        <view class="bar-col" v-for="item in trend" :key="item.date">
          <view class="bar-bg">
            <view class="bar" :style="{ height: item.height + '%' }"></view>
          </view>
          <text class="count">{{ item.count }}</text>
          <text class="date">{{ item.date.slice(5) }}</text>
        </view>
      </view>
      <view class="empty" v-else><text>暂无服务数据</text></view>
    </view>

    <view class="panel slide-up delay-3">
      <text class="title">服务类型分布</text>
      <view v-if="typeStats.length === 0" class="empty"><text>暂无数据</text></view>
      <view v-else>
        <view class="type-row" v-for="item in typeStats" :key="item.name">
          <view class="row-top">
            <text class="name">{{ item.name }}</text>
            <text class="num">{{ item.count }} 单</text>
          </view>
          <view class="line-bg"><view class="line" :style="{ width: item.percent + '%' }"></view></view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { get } from '@/utils/api.js'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const { todayService, monthService, totalIncome, praiseRate } = storeToRefs(userStore)

const trend = ref([])
const typeStats = ref([])

const formatMoney = (value) => {
  const num = Number(value || 0)
  return Number.isFinite(num) ? num.toFixed(2) : '0.00'
}

const loadStats = async () => {
  const userInfo = uni.getStorageSync('userInfo')
  if (!userInfo || !userInfo.id) return

  try {
    await userStore.fetchAttendantProfile(userInfo.id)

    const res = await get('/attendant/orders', {
      attendantId: userInfo.id,
      orderStatus: 6,
      page: 0,
      size: 300
    })

    const rows = res.code === 200 && res.data && Array.isArray(res.data.content)
      ? res.data.content
      : []

    buildTrend(rows)
    buildTypeStats(rows)
  } catch (error) {
    console.error('加载服务统计失败:', error)
    trend.value = []
    typeStats.value = []
  }
}

const buildTrend = (rows) => {
  const days = []
  const map = {}
  const now = new Date()

  for (let i = 6; i >= 0; i -= 1) {
    const d = new Date(now)
    d.setDate(now.getDate() - i)
    const key = d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0')
    days.push(key)
    map[key] = 0
  }

  rows.forEach((row) => {
    const serviceDate = row.serviceDate
    if (serviceDate && map[serviceDate] !== undefined) {
      map[serviceDate] += 1
    }
  })

  const max = Math.max(...days.map((d) => map[d]), 1)
  trend.value = days.map((d) => ({
    date: d,
    count: map[d],
    height: Math.round((map[d] / max) * 100)
  }))
}

const buildTypeStats = (rows) => {
  const map = {}
  rows.forEach((row) => {
    const key = row.serviceTypeName || row.serviceContent || '其他服务'
    map[key] = (map[key] || 0) + 1
  })

  const list = Object.keys(map).map((name) => ({ name, count: map[name] }))
  list.sort((a, b) => b.count - a.count)
  const max = list.length ? list[0].count : 1

  typeStats.value = list.map((item) => ({
    name: item.name,
    count: item.count,
    percent: Math.max(8, Math.round((item.count / max) * 100))
  }))
}

onMounted(() => {
  loadStats()
})
</script>

<style lang="scss" scoped>
@import '@/styles/escort-ui.scss';
.page {
  @include escort-page;
  min-height: 100vh;
  padding: 24rpx;
  box-sizing: border-box;
}

.hero-card {
  @include escort-card(26rpx);
  margin-bottom: 18rpx;
  border: 1rpx solid #e5eefb;
  background: linear-gradient(135deg, #ffffff 0%, #f3f8ff 100%);
}

.hero-left {
  display: flex;
  align-items: center;
}

.hero-icon {
  width: 56rpx;
  height: 56rpx;
  margin-right: 14rpx;
}

.hero-title {
  display: block;
  font-size: 30rpx;
  font-weight: 700;
  color: #1f2937;
}

.hero-desc {
  display: block;
  margin-top: 6rpx;
  font-size: 24rpx;
  color: #6b7280;
}

.grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16rpx;
  margin-bottom: 18rpx;
}

.card {
  background: $escort-color-surface;
  border-radius: $escort-radius-card;
  padding: 20rpx;
  box-shadow: $escort-shadow-card;
}

.k {
  display: block;
  color: #6b7280;
  font-size: 24rpx;
}

.v {
  display: block;
  margin-top: 10rpx;
  color: $escort-color-primary;
  font-size: 36rpx;
  font-weight: 700;
}

.panel {
  background: $escort-color-surface;
  border-radius: $escort-radius-card;
  padding: 24rpx;
  margin-bottom: 18rpx;
  box-shadow: $escort-shadow-card;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.title {
  font-size: 30rpx;
  font-weight: 700;
  color: #1f2937;
}

.refresh {
  font-size: 24rpx;
  color: $escort-color-primary;
}

.trend-wrap {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-top: 16rpx;
}

.bar-col {
  width: 13%;
  text-align: center;
}

.bar-bg {
  height: 180rpx;
  border-radius: 10rpx;
  background: #f1f5f9;
  display: flex;
  align-items: flex-end;
}

.bar {
  width: 100%;
  background: linear-gradient(180deg, #7cb7ff, $escort-color-primary-deep);
  border-radius: 10rpx;
}

.count {
  display: block;
  margin-top: 8rpx;
  font-size: 23rpx;
  color: #4b5563;
}

.date {
  display: block;
  margin-top: 2rpx;
  font-size: 20rpx;
  color: #9ca3af;
}

.type-row {
  margin-top: 14rpx;
}

.row-top {
  display: flex;
  justify-content: space-between;
  margin-bottom: 6rpx;
}

.name {
  font-size: 25rpx;
  color: #374151;
}

.num {
  font-size: 24rpx;
  color: #6b7280;
}

.line-bg {
  height: 14rpx;
  border-radius: 8rpx;
  background: #eef2f7;
  overflow: hidden;
}

.line {
  height: 100%;
  background: linear-gradient(90deg, #66a6ff, #66a6ff);
}

.empty {
  height: 120rpx;
  display: flex;
  align-items: center;
  justify-content: center;

  text {
    font-size: 24rpx;
    color: #9ca3af;
  }
}

.slide-up {
  opacity: 0;
  transform: translateY(20rpx);
  animation: slideUp 0.42s ease forwards;
}

.delay-1 {
  animation-delay: 0.02s;
}

.delay-2 {
  animation-delay: 0.08s;
}

.delay-3 {
  animation-delay: 0.14s;
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

<template>
  <view class="page">
    <view class="hero-card slide-up delay-1">
      <view class="hero-left">
        <image class="hero-icon" src="/static/escort-stats.svg" mode="aspectFit"></image>
        <view>
          <text class="hero-title">服务统计</text>
          <text class="hero-desc">按今日、本月或全部日期查看服务完成情况</text>
        </view>
      </view>
    </view>

    <view class="filter-bar slide-up delay-1">
      <view
        v-for="item in rangeOptions"
        :key="item.value"
        class="filter-chip"
        :class="{ active: activeRange === item.value }"
        @click="changeRange(item.value)"
      >
        <text>{{ item.label }}</text>
      </view>
    </view>

    <view class="grid slide-up delay-1">
      <view class="card"><text class="k">今日服务</text><text class="v">{{ todayService }}</text></view>
      <view class="card"><text class="k">本月服务</text><text class="v">{{ monthService }}</text></view>
      <view class="card"><text class="k">累计收入</text><text class="v">{{ formatMoney(totalIncome) }}</text></view>
      <view class="card"><text class="k">好评率</text><text class="v">{{ displayPraiseRate }}</text></view>
    </view>

    <view class="panel slide-up delay-2">
      <view class="panel-head">
        <text class="title">{{ selectedRangeLabel }}趋势</text>
        <text class="refresh" @click="loadStats">刷新</text>
      </view>
      <text class="panel-meta">共 {{ filteredRows.length }} 单服务记录</text>
      <scroll-view class="trend-scroll" scroll-x :show-scrollbar="false" v-if="trend.length">
        <view class="trend-wrap" :style="trendWrapStyle">
          <view class="bar-col" v-for="item in trend" :key="item.date">
            <view class="bar-bg">
              <view class="bar" :style="{ height: item.height + '%' }"></view>
            </view>
            <text class="count">{{ item.count }}</text>
            <text class="date">{{ formatDateShort(item.date) }}</text>
          </view>
        </view>
      </scroll-view>
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

    <view class="panel slide-up delay-3">
      <view class="panel-head">
        <text class="title">按日期查看</text>
        <text class="panel-meta">{{ selectedRangeLabel }}</text>
      </view>
      <view v-if="dateStats.length === 0" class="empty"><text>暂无数据</text></view>
      <view v-else class="date-list">
        <view class="date-row" v-for="item in dateStats" :key="item.date">
          <text class="date-label">{{ formatDateLabel(item.date) }}</text>
          <text class="date-num">{{ item.count }} 单</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { storeToRefs } from 'pinia'
import { get } from '@/utils/api.js'
import { useUserStore } from '@/stores/user'
import { formatPraiseRate } from '@/utils/rating.js'

const userStore = useUserStore()
const { attendantInfo, todayService, monthService, totalIncome, praiseRate } = storeToRefs(userStore)

const rawRows = ref([])
const activeRange = ref('all')
const rangeOptions = [
  { label: '今日服务', value: 'today' },
  { label: '本月服务', value: 'month' },
  { label: '全部日期', value: 'all' }
]

const formatMoney = (value) => {
  const num = Number(value || 0)
  return Number.isFinite(num) ? num.toFixed(2) : '0.00'
}

const pad = (value) => String(value).padStart(2, '0')

const normalizeDate = (value) => {
  if (!value) return ''
  if (/^\d{4}-\d{2}-\d{2}$/.test(value)) return value
  const parsed = new Date(typeof value === 'string' ? value.replace(/-/g, '/') : value)
  if (Number.isNaN(parsed.getTime())) return ''
  return `${parsed.getFullYear()}-${pad(parsed.getMonth() + 1)}-${pad(parsed.getDate())}`
}

const getCurrentMonthKey = () => {
  const now = new Date()
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}`
}

const filteredRows = computed(() => {
  if (activeRange.value === 'today') {
    const today = normalizeDate(new Date())
    return rawRows.value.filter((row) => normalizeDate(row.serviceDate) === today)
  }

  if (activeRange.value === 'month') {
    const currentMonthKey = getCurrentMonthKey()
    return rawRows.value.filter((row) => normalizeDate(row.serviceDate).startsWith(currentMonthKey))
  }

  return rawRows.value
})

const selectedRangeLabel = computed(() => {
  const current = rangeOptions.find((item) => item.value === activeRange.value)
  return current?.label || '全部日期'
})

const buildTrend = (rows) => {
  const map = {}

  rows.forEach((row) => {
    const serviceDate = normalizeDate(row.serviceDate)
    if (serviceDate) {
      map[serviceDate] = (map[serviceDate] || 0) + 1
    }
  })

  if (activeRange.value === 'today') {
    const today = normalizeDate(new Date())
    if (!(today in map)) {
      map[today] = 0
    }
  }

  if (activeRange.value === 'month') {
    const now = new Date()
    const lastDay = new Date(now.getFullYear(), now.getMonth() + 1, 0).getDate()
    for (let day = 1; day <= lastDay; day += 1) {
      const key = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(day)}`
      if (!(key in map)) {
        map[key] = 0
      }
    }
  }

  const dates = Object.keys(map).sort((a, b) => a.localeCompare(b))
  if (dates.length === 0) return []

  const max = Math.max(...dates.map((date) => map[date]), 1)
  return dates.map((date) => ({
    date,
    count: map[date],
    height: Math.max(map[date] > 0 ? Math.round((map[date] / max) * 100) : 6, 6)
  }))
}

const buildTypeStats = (rows) => {
  const map = {}

  rows.forEach((row) => {
    const key = row.serviceTypeName || row.serviceContent || '其他服务'
    map[key] = (map[key] || 0) + 1
  })

  const list = Object.keys(map)
    .map((name) => ({ name, count: map[name] }))
    .sort((a, b) => b.count - a.count)

  const max = list.length ? list[0].count : 1
  return list.map((item) => ({
    name: item.name,
    count: item.count,
    percent: Math.max(8, Math.round((item.count / max) * 100))
  }))
}

const buildDateStats = (rows) => {
  const map = {}

  rows.forEach((row) => {
    const serviceDate = normalizeDate(row.serviceDate)
    if (serviceDate) {
      map[serviceDate] = (map[serviceDate] || 0) + 1
    }
  })

  return Object.keys(map)
    .sort((a, b) => b.localeCompare(a))
    .map((date) => ({ date, count: map[date] }))
}

const trend = computed(() => buildTrend(filteredRows.value))
const typeStats = computed(() => buildTypeStats(filteredRows.value))
const dateStats = computed(() => buildDateStats(filteredRows.value))
const trendWrapStyle = computed(() => ({
  minWidth: `${Math.max(trend.value.length, 1) * 110}rpx`
}))
const displayPraiseRate = computed(() => formatPraiseRate(praiseRate.value, attendantInfo.value.evaluationCount))

const formatDateShort = (value) => value.slice(5)

const formatDateLabel = (value) => {
  const [year = '', month = '', day = ''] = String(value).split('-')
  return `${year}-${month}-${day}`
}

const changeRange = (range) => {
  activeRange.value = range
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

    rawRows.value = res.code === 200 && res.data && Array.isArray(res.data.content)
      ? res.data.content
      : []
  } catch (error) {
    console.error('加载服务统计失败:', error)
    rawRows.value = []
  }
}

onLoad((options) => {
  if (['today', 'month', 'all'].includes(options?.range)) {
    activeRange.value = options.range
  }
})

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

.filter-bar {
  display: flex;
  gap: 12rpx;
  margin-bottom: 18rpx;
}

.filter-chip {
  flex: 1;
  min-width: 0;
  height: 72rpx;
  border-radius: 999rpx;
  border: 1rpx solid #d9e5f4;
  background: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;

  text {
    font-size: 25rpx;
    color: #5b6678;
    font-weight: 600;
  }
}

.filter-chip.active {
  background: linear-gradient(135deg, $escort-color-primary, $escort-color-primary-deep);
  border-color: transparent;
  box-shadow: $escort-shadow-primary;

  text {
    color: #ffffff;
  }
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
  gap: 12rpx;
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

.panel-meta {
  display: block;
  margin-top: 10rpx;
  font-size: 23rpx;
  color: #8a94a6;
}

.trend-scroll {
  margin-top: 16rpx;
  white-space: nowrap;
}

.trend-wrap {
  display: flex;
  align-items: flex-end;
  gap: 16rpx;
}

.bar-col {
  width: 94rpx;
  text-align: center;
  flex-shrink: 0;
}

.bar-bg {
  height: 180rpx;
  border-radius: 10rpx;
  background: #f1f5f9;
  display: flex;
  align-items: flex-end;
  overflow: hidden;
}

.bar {
  width: 100%;
  background: linear-gradient(180deg, #69b2ff, $escort-color-primary-deep);
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
  background: linear-gradient(90deg, #007aff, #007aff);
}

.date-list {
  margin-top: 16rpx;
}

.date-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18rpx 0;
  border-bottom: 1rpx solid #eef2f7;
}

.date-row:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.date-label {
  font-size: 26rpx;
  color: #334155;
}

.date-num {
  font-size: 24rpx;
  color: $escort-color-primary;
  font-weight: 600;
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

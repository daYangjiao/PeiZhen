<template>
  <view class="page">
    <view class="card slide-up delay-1">
      <view class="overview-row">
        <text class="overview-label">账户总余额</text>
        <text class="overview-amount">¥{{ formatMoney(balance) }}</text>
      </view>
      <text class="overview-sub">可提现 ¥{{ formatMoney(balance) }}</text>
    </view>

    <view class="card slide-up delay-2">
      <text class="section-title">收支明细</text>

      <view class="tag-bar">
        <view
          v-for="tag in tags"
          :key="tag.key"
          class="tag"
          :class="{ active: activeTag === tag.key }"
          @click="activeTag = tag.key"
        >
          <text>{{ tag.label }}</text>
        </view>
      </view>

      <view v-if="loading" class="empty-wrap"><text>加载中...</text></view>
      <view v-else-if="showNotIntegrated" class="empty-wrap"><text>暂未接入</text></view>
      <view v-else-if="filteredRecords.length === 0" class="empty-wrap"><text>暂无收入明细</text></view>
      <view v-else>
        <view class="record-row" v-for="item in filteredRecords" :key="item.id">
          <view class="left-col">
            <text class="title">{{ item.title }}</text>
            <text class="sub">{{ item.sub }}</text>
          </view>
          <text class="amount" :class="item.amount >= 0 ? 'income' : 'expense'">
            {{ item.amount >= 0 ? '+' : '' }}{{ formatMoney(item.amount) }}
          </text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { storeToRefs } from 'pinia'
import { get } from '@/utils/api.js'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const { balance } = storeToRefs(userStore)

const loading = ref(false)
const activeTag = ref('all')
const incomeRecords = ref([])

const tags = [
  { key: 'all', label: '全部' },
  { key: 'income', label: '收入' },
  { key: 'withdraw', label: '提现' },
  { key: 'compensation', label: '赔付' }
]

const formatMoney = (value) => {
  const num = Number(value || 0)
  return Number.isFinite(num) ? num.toFixed(2) : '0.00'
}

const userId = () => {
  const userInfo = uni.getStorageSync('userInfo')
  return userInfo && userInfo.id ? userInfo.id : null
}

const normalizedIncome = computed(() => {
  return incomeRecords.value.map((item) => {
    const orderNo = item.orderNo || '--'
    const serviceName = item.serviceTypeName || item.serviceContent || '服务收入'
    const serviceDate = item.serviceDate || '--'
    const timeText = item.createTime ? String(item.createTime).slice(0, 16).replace('T', ' ') : serviceDate
    return {
      id: item.orderId || orderNo,
      type: 'income',
      title: `${serviceName} (订单 ${orderNo})`,
      sub: `${timeText} · 基础费+超时费`,
      amount: Number(item.orderAmount || 0)
    }
  })
})

const showNotIntegrated = computed(() => {
  return ['withdraw', 'compensation'].includes(activeTag.value)
})

const filteredRecords = computed(() => {
  if (activeTag.value === 'all' || activeTag.value === 'income') {
    return normalizedIncome.value
  }
  return []
})

const loadIncome = async () => {
  const uid = userId()
  if (!uid) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    return
  }

  loading.value = true
  try {
    await userStore.fetchAttendantProfile(uid)
    const res = await get('/attendant/orders', {
      attendantId: uid,
      orderStatus: '6',
      page: 0,
      size: 200
    })

    if (res.code === 200 && res.data && Array.isArray(res.data.content)) {
      incomeRecords.value = res.data.content
        .slice()
        .sort((a, b) => new Date(b.createTime || 0).getTime() - new Date(a.createTime || 0).getTime())
    } else {
      incomeRecords.value = []
    }
  } catch (error) {
    incomeRecords.value = []
    uni.showToast({ title: '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadIncome()
})
</script>

<style lang="scss" scoped>
@import '@/styles/escort-ui.scss';
.page {
  @include escort-page;
  min-height: 100vh;
  padding-bottom: 24rpx;
}

.card {
  background: $escort-color-surface;
  border-radius: $escort-radius-card;
  padding: 32rpx;
  margin: 24rpx;
  box-shadow: $escort-shadow-card;
}

.overview-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.overview-label {
  font-size: 30rpx;
  color: #1f2937;
  font-weight: 600;
}

.overview-amount {
  font-size: 46rpx;
  color: $escort-color-primary;
  font-weight: 700;
}

.overview-sub {
  display: block;
  margin-top: 10rpx;
  font-size: 24rpx;
  color: #666666;
}

.section-title {
  display: block;
  font-size: 30rpx;
  color: #1f2937;
  font-weight: 700;
  margin-bottom: 14rpx;
}

.tag-bar {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 16rpx;
  overflow-x: auto;
  white-space: nowrap;
}

.tag {
  height: 56rpx;
  padding: 0 22rpx;
  border-radius: 30rpx;
  background: #f0f2f5;
  display: inline-flex;
  align-items: center;
  justify-content: center;

  text {
    color: #111111;
    font-size: 24rpx;
  }
}

.active {
  background: linear-gradient(135deg, $escort-color-primary, $escort-color-primary-deep);

  text {
    color: #ffffff;
  }
}

.record-row {
  padding: 18rpx 0;
  border-bottom: 1rpx solid #eef2f7;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.left-col {
  flex: 1;
  min-width: 0;
  margin-right: 14rpx;
}

.title {
  display: block;
  font-size: 27rpx;
  color: #1f2937;
  font-weight: 700;
}

.sub {
  display: block;
  margin-top: 8rpx;
  font-size: 23rpx;
  color: #666666;
}

.amount {
  font-size: 30rpx;
  font-weight: 700;
}

.income {
  color: #52c41a;
}

.expense {
  color: #ff4d4f;
}

.empty-wrap {
  height: 180rpx;
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
  animation-delay: 0.1s;
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

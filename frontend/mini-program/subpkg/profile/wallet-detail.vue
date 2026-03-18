<template>
  <view class="page">
    <view class="hero-card slide-up delay-1">
      <view class="hero-left">
        <image class="hero-icon" src="/static/escort-wallet.svg" mode="aspectFit"></image>
        <view>
          <text class="hero-title">钱包明细</text>
          <text class="hero-desc">收支与提现统一管理，资金流向更清晰</text>
        </view>
      </view>
    </view>

    <view class="card slide-up delay-1">
      <view class="overview-row">
        <view class="overview-left">
          <text class="overview-label">账户总余额</text>
          <text class="overview-amount">¥{{ formatMoney(balance) }}</text>
        </view>
        <view class="withdraw-entry" @click="openWithdraw">
          <text>立即提现</text>
        </view>
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
          @click="onTagChange(tag.key)"
        >
          <text>{{ tag.label }}</text>
        </view>
      </view>
    </view>

    <view v-if="activeTag === 'withdraw'" class="card slide-up delay-2">
      <view class="withdraw-head">
        <text class="section-title">提现操作</text>
        <text class="head-tip">随时可提现</text>
      </view>

      <view class="method-row">
        <view class="method-left">
          <image class="wechat-icon" src="/static/wechat-icon.png" mode="aspectFit"></image>
          <text class="method-name">微信提现</text>
        </view>
        <text class="method-status">已绑定 · 尾号8912</text>
      </view>

      <view class="input-row">
        <view class="money-input-wrap">
          <text class="prefix">¥</text>
          <input
            class="money-input"
            type="digit"
            v-model="amount"
            placeholder="请输入提现金额"
            @input="onInput"
          />
        </view>
        <view class="ghost-btn" @click="setAll"><text>全部提现</text></view>
      </view>

      <text v-if="showEstimate" class="estimate">预计到账：¥{{ amountText }}</text>

      <view class="submit-btn" :class="{ disabled: submitDisabled }" @click="submitWithdraw">
        <text>{{ submitting ? '提交中...' : '确认提现' }}</text>
      </view>
    </view>

    <view class="card slide-up delay-3">
      <view class="record-head">
        <text class="section-title">{{ activeTag === 'withdraw' ? '近期提现记录' : '收支记录' }}</text>
      </view>

      <view v-if="showNotIntegrated" class="empty-wrap"><text>暂未接入</text></view>
      <view v-else-if="recordsLoading" class="empty-wrap"><text>加载中...</text></view>
      <view v-else-if="filteredRecords.length === 0" class="empty-wrap"><text>暂无记录</text></view>
      <view v-else>
        <view class="record-row" v-for="item in filteredRecords" :key="item.id">
          <view class="left-col">
            <text class="title">{{ item.title }}</text>
            <text class="sub">{{ item.sub }}</text>
          </view>
          <view class="right-col">
            <text class="amount" :class="item.amount >= 0 ? 'income' : 'expense'">
              {{ item.amount >= 0 ? '+' : '' }}{{ formatMoney(item.amount) }}
            </text>
            <text v-if="item.statusText" class="status" :class="item.statusClass">{{ item.statusText }}</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { storeToRefs } from 'pinia'
import { get, post } from '@/utils/api.js'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const { balance } = storeToRefs(userStore)

const activeTag = ref('all')
const loadingIncome = ref(false)
const loadingWithdraw = ref(false)
const incomeRecords = ref([])
const withdrawRecords = ref([])
const amount = ref('')
const submitting = ref(false)

const tags = [
  { key: 'all', label: '全部' },
  { key: 'income', label: '收入' },
  { key: 'withdraw', label: '提现' },
  { key: 'compensation', label: '赔付' }
]

const userId = () => {
  const userInfo = uni.getStorageSync('userInfo')
  return userInfo && userInfo.id ? userInfo.id : null
}

const formatMoney = (value) => {
  const num = Number(value || 0)
  return Number.isFinite(num) ? num.toFixed(2) : '0.00'
}

const parseTime = (value) => {
  if (!value) return 0
  const t = new Date(String(value).replace(' ', 'T')).getTime()
  return Number.isFinite(t) ? t : 0
}

const getStatusText = (status) => {
  if (status === 'SUCCESS' || status === 'success') return '已到账'
  if (status === 'PROCESSING' || status === 'processing') return '处理中'
  return '失败'
}

const getStatusClass = (status) => {
  if (status === 'SUCCESS' || status === 'success') return 'status-success'
  if (status === 'PROCESSING' || status === 'processing') return 'status-processing'
  return 'status-failed'
}

const normalizedIncome = computed(() => {
  return incomeRecords.value.map((item) => {
    const orderNo = item.orderNo || '--'
    const serviceName = item.serviceTypeName || item.serviceContent || '服务收入'
    const serviceDate = item.serviceDate || '--'
    const timeText = item.createTime ? String(item.createTime).slice(0, 16).replace('T', ' ') : serviceDate
    const amountValue = Number(item.orderAmount || 0) + Number(item.balanceAmount || 0)
    return {
      id: `income-${item.orderId || orderNo}`,
      title: `${serviceName} (订单 ${orderNo})`,
      sub: `${timeText} · 基础费+超时费`,
      amount: amountValue,
      sortTime: parseTime(item.createTime) || parseTime(serviceDate)
    }
  })
})

const normalizedWithdraw = computed(() => {
  return withdrawRecords.value.map((item, idx) => {
    const timeText = item.applyTime || item.createTime || '--'
    const status = item.status || ''
    return {
      id: `withdraw-${item.id || idx}`,
      title: '微信提现',
      sub: `${timeText} · 提现申请`,
      amount: -Math.abs(Number(item.amount || 0)),
      statusText: getStatusText(status),
      statusClass: getStatusClass(status),
      sortTime: parseTime(item.applyTime || item.createTime)
    }
  })
})

const recordsLoading = computed(() => {
  if (activeTag.value === 'income') return loadingIncome.value
  if (activeTag.value === 'withdraw') return loadingWithdraw.value
  if (activeTag.value === 'all') return loadingIncome.value || loadingWithdraw.value
  return false
})

const showNotIntegrated = computed(() => activeTag.value === 'compensation')

const allRecords = computed(() => {
  return [...normalizedIncome.value, ...normalizedWithdraw.value]
    .slice()
    .sort((a, b) => b.sortTime - a.sortTime)
})

const filteredRecords = computed(() => {
  if (activeTag.value === 'all') return allRecords.value
  if (activeTag.value === 'income') return normalizedIncome.value
  if (activeTag.value === 'withdraw') return normalizedWithdraw.value
  return []
})

const parsedAmount = computed(() => Number(amount.value || 0))
const amountText = computed(() => formatMoney(parsedAmount.value))
const showEstimate = computed(() => parsedAmount.value > 0)
const submitDisabled = computed(() => {
  return submitting.value || parsedAmount.value <= 0 || parsedAmount.value > Number(balance.value || 0)
})

const onInput = () => {
  if (parsedAmount.value > Number(balance.value || 0)) {
    uni.showToast({ title: '金额超过可提现余额', icon: 'none' })
  }
}

const setAll = () => {
  amount.value = formatMoney(balance.value)
}

const onTagChange = (tag) => {
  activeTag.value = tag
}

const openWithdraw = () => {
  activeTag.value = 'withdraw'
}

const loadIncome = async (uid) => {
  loadingIncome.value = true
  try {
    const res = await get('/attendant/orders', {
      attendantId: uid,
      orderStatus: '6',
      page: 0,
      size: 200
    })

    if (res.code === 200 && res.data && Array.isArray(res.data.content)) {
      incomeRecords.value = res.data.content
    } else {
      incomeRecords.value = []
    }
  } catch (error) {
    incomeRecords.value = []
  } finally {
    loadingIncome.value = false
  }
}

const loadWithdrawRecords = async (uid) => {
  loadingWithdraw.value = true
  try {
    const res = await get('/attendant/withdraw/records', { userId: uid, page: 0, size: 20 })
    if (res.code === 200 && Array.isArray(res.data)) {
      withdrawRecords.value = res.data
    } else if (res.code === 200 && res.data && Array.isArray(res.data.content)) {
      withdrawRecords.value = res.data.content
    } else {
      withdrawRecords.value = []
    }
  } catch (error) {
    withdrawRecords.value = []
  } finally {
    loadingWithdraw.value = false
  }
}

const submitWithdraw = async () => {
  if (submitDisabled.value) return
  const uid = userId()
  if (!uid) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    return
  }

  submitting.value = true
  try {
    await post('/attendant/withdraw/apply', {
      userId: uid,
      amount: parsedAmount.value
    })
    uni.showToast({ title: '提现申请已提交', icon: 'success' })
    amount.value = ''
    await loadWithdrawRecords(uid)
  } catch (error) {
    uni.showToast({ title: '提现接口暂未接入', icon: 'none' })
  } finally {
    submitting.value = false
  }
}

const initPageData = async () => {
  const uid = userId()
  if (!uid) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    return
  }
  await userStore.fetchAttendantProfile(uid)
  await Promise.all([loadIncome(uid), loadWithdrawRecords(uid)])
}

onLoad((options) => {
  const tab = options && options.tab ? String(options.tab) : ''
  if (['all', 'income', 'withdraw', 'compensation'].includes(tab)) {
    activeTag.value = tab
  }
})

onMounted(() => {
  initPageData()
})
</script>

<style lang="scss" scoped>
@import '@/styles/escort-ui.scss';

.page {
  @include escort-page;
  min-height: 100vh;
  padding-bottom: 24rpx;
}

.hero-card {
  @include escort-card(26rpx);
  margin: 24rpx 24rpx 0;
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

.overview-left {
  flex: 1;
  min-width: 0;
}

.overview-label {
  font-size: 30rpx;
  color: #1f2937;
  font-weight: 600;
}

.overview-amount {
  display: block;
  margin-top: 6rpx;
  font-size: 46rpx;
  color: $escort-color-primary;
  font-weight: 700;
}

.withdraw-entry {
  min-width: 134rpx;
  height: 64rpx;
  border-radius: 32rpx;
  background: linear-gradient(135deg, $escort-color-primary, $escort-color-primary-deep);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: $escort-shadow-primary;

  text {
    color: #fff;
    font-size: 24rpx;
    font-weight: 600;
  }
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
}

.withdraw-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.head-tip {
  font-size: 24rpx;
  color: #6b7280;
}

.tag-bar {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-top: 16rpx;
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

.method-row {
  margin-top: 16rpx;
  min-height: 66rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.method-left {
  display: flex;
  align-items: center;
}

.wechat-icon {
  width: 34rpx;
  height: 34rpx;
  margin-right: 12rpx;
}

.method-name {
  font-size: 27rpx;
  color: #1f2937;
}

.method-status {
  font-size: 24rpx;
  color: $escort-color-primary;
}

.input-row {
  margin-top: 14rpx;
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.money-input-wrap {
  flex: 1;
  min-width: 0;
  height: 84rpx;
  border-radius: 14rpx;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  padding: 0 18rpx;
  box-sizing: border-box;
}

.prefix {
  color: #1f2937;
  font-size: 30rpx;
  margin-right: 8rpx;
}

.money-input {
  flex: 1;
  font-size: 28rpx;
  color: #111827;
}

.ghost-btn {
  min-width: 128rpx;
  height: 70rpx;
  border-radius: 12rpx;
  border: 1rpx solid $escort-color-primary;
  display: flex;
  align-items: center;
  justify-content: center;

  text {
    color: $escort-color-primary;
    font-size: 24rpx;
  }
}

.estimate {
  display: block;
  margin-top: 10rpx;
  font-size: 24rpx;
  color: $escort-color-primary;
}

.submit-btn {
  margin-top: 16rpx;
  height: 82rpx;
  border-radius: 44rpx;
  background: linear-gradient(135deg, $escort-color-primary, $escort-color-primary-deep);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: $escort-shadow-primary;

  text {
    color: #ffffff;
    font-size: 28rpx;
    font-weight: 700;
  }
}

.submit-btn.disabled {
  background: #c0c4cc;
  box-shadow: none;
}

.record-head {
  margin-bottom: 10rpx;
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

.right-col {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
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

.status {
  margin-top: 6rpx;
  font-size: 22rpx;
}

.status-success {
  color: #52c41a;
}

.status-processing {
  color: #faad14;
}

.status-failed {
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

.delay-3 {
  animation-delay: 0.18s;
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

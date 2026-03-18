<template>
  <view class="page">
    <view class="hero-card slide-up delay-1">
      <view class="hero-left">
        <image class="hero-icon" src="/static/money.png" mode="aspectFit"></image>
        <view>
          <text class="hero-title">提现中心</text>
          <text class="hero-desc">余额实时更新，到账更透明</text>
        </view>
      </view>
    </view>

    <view class="card slide-up delay-1">
      <view class="row-between">
        <text class="title">可提现余额</text>
        <text class="amount">¥{{ formatMoney(balance) }}</text>
      </view>
      <text class="tip">佣金满100元可提现</text>
    </view>

    <view class="card slide-up delay-2">
      <text class="section-title">提现方式</text>
      <view class="method-row no-border">
        <view class="method-left">
          <view class="icon-wrap icon-wechat">
            <svg viewBox="0 0 64 64" class="svg-icon" xmlns="http://www.w3.org/2000/svg">
              <circle cx="32" cy="32" r="30" fill="#52C41A" />
              <circle cx="26" cy="28" r="10" fill="#FFFFFF" />
              <circle cx="40" cy="34" r="9" fill="#FFFFFF" opacity="0.95" />
              <circle cx="23" cy="28" r="1.6" fill="#52C41A" />
              <circle cx="29" cy="28" r="1.6" fill="#52C41A" />
              <circle cx="37" cy="34" r="1.4" fill="#52C41A" />
              <circle cx="43" cy="34" r="1.4" fill="#52C41A" />
            </svg>
          </view>
          <text class="method-name">微信支付</text>
        </view>
        <view class="method-right">
          <text class="method-status bound">已绑定 · 尾号8912</text>
          <view class="ghost-btn" @click="onChangeWechat"><text>更换</text></view>
        </view>
      </view>
    </view>

    <view class="card slide-up delay-3">
      <text class="section-title">提现金额</text>
      <view class="input-row">
        <view class="money-input-wrap">
          <text class="prefix">¥</text>
          <input
            class="money-input"
            type="digit"
            v-model="amount"
            placeholder="请输入金额"
            @input="onInput"
          />
        </view>
        <view class="ghost-btn all-btn" @click="setAll"><text>全部提现</text></view>
      </view>
      <text class="warning">最低提现100元</text>
      <text class="estimate" v-if="showEstimate">预计到账：¥{{ amountText }}</text>
    </view>

    <view
      class="submit-btn slide-up delay-4"
      :class="{ disabled: submitDisabled }"
      @click="submitWithdraw"
    >
      <text>{{ submitting ? '提交中...' : '确认提现' }}</text>
    </view>

    <view class="card slide-up delay-4">
      <view class="record-head">
        <text class="section-title">近期提现记录</text>
      </view>

      <view v-if="loadingRecords" class="empty"><text>加载中...</text></view>
      <view v-else-if="records.length === 0" class="empty"><text>暂无提现记录</text></view>
      <view v-else>
        <view class="record-row" v-for="item in records" :key="item.id">
          <view class="left-col">
            <text class="record-time">{{ item.applyTime || '--' }}</text>
            <text class="record-amount">提现金额 ¥{{ formatMoney(item.amount) }}</text>
          </view>
          <text class="record-status" :class="statusClass(item.status)">
            {{ statusText(item.status) }}
          </text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { storeToRefs } from 'pinia'
import { get, post } from '@/utils/api.js'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const { balance } = storeToRefs(userStore)

const amount = ref('')
const loadingRecords = ref(false)
const submitting = ref(false)
const records = ref([])

const userId = () => {
  const userInfo = uni.getStorageSync('userInfo')
  return userInfo && userInfo.id ? userInfo.id : null
}

const formatMoney = (value) => {
  const num = Number(value || 0)
  return Number.isFinite(num) ? num.toFixed(2) : '0.00'
}

const parsedAmount = computed(() => Number(amount.value || 0))
const showEstimate = computed(() => parsedAmount.value > 0)
const amountText = computed(() => formatMoney(parsedAmount.value))

const submitDisabled = computed(() => {
  return submitting.value || parsedAmount.value < 100 || parsedAmount.value > Number(balance.value || 0)
})

const onInput = () => {
  if (parsedAmount.value > Number(balance.value || 0)) {
    uni.showToast({ title: '金额超过可提现余额', icon: 'none' })
  }
}

const setAll = () => {
  amount.value = formatMoney(balance.value)
}

const onChangeWechat = () => {
  uni.showToast({ title: '更换微信方式开发中', icon: 'none' })
}

const statusText = (status) => {
  if (status === 'SUCCESS' || status === 'success') return '已到账'
  if (status === 'PROCESSING' || status === 'processing') return '处理中'
  return '失败'
}

const statusClass = (status) => {
  if (status === 'SUCCESS' || status === 'success') return 'success'
  if (status === 'PROCESSING' || status === 'processing') return 'processing'
  return 'failed'
}

const loadRecords = async () => {
  const uid = userId()
  if (!uid) return

  loadingRecords.value = true
  try {
    const res = await get('/attendant/withdraw/records', { userId: uid, page: 0, size: 5 })
    if (res.code === 200 && Array.isArray(res.data)) {
      records.value = res.data
    } else if (res.code === 200 && res.data && Array.isArray(res.data.content)) {
      records.value = res.data.content
    } else {
      records.value = []
    }
  } catch (error) {
    records.value = []
  } finally {
    loadingRecords.value = false
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
    const payload = {
      userId: uid,
      amount: parsedAmount.value
    }
    await post('/attendant/withdraw/apply', payload)
    uni.showToast({ title: '提现申请已提交', icon: 'success' })
    amount.value = ''
    loadRecords()
  } catch (error) {
    uni.showToast({ title: '提现接口暂未接入', icon: 'none' })
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  const uid = userId()
  if (uid) {
    await userStore.fetchAttendantProfile(uid)
  }
  loadRecords()
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

.row-between {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title {
  font-size: 30rpx;
  color: #1f2937;
  font-weight: 600;
}

.amount {
  font-size: 52rpx;
  color: $escort-color-primary;
  font-weight: 700;
}

.tip {
  display: block;
  margin-top: 12rpx;
  color: #666666;
  font-size: 26rpx;
}

.section-title {
  display: block;
  font-size: 30rpx;
  color: #1f2937;
  font-weight: 700;
  margin-bottom: 16rpx;
}

.method-row {
  padding: 14rpx 0;
  border-bottom: 1rpx solid #eef2f7;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.no-border {
  border-bottom: none;
}

.method-left {
  display: flex;
  align-items: center;
}

.icon-wrap {
  width: 54rpx;
  height: 54rpx;
  border-radius: 999rpx;
  overflow: hidden;
  margin-right: 14rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.svg-icon {
  width: 54rpx;
  height: 54rpx;
}

.method-name {
  font-size: 28rpx;
  color: #1f2937;
}

.method-right {
  display: flex;
  align-items: center;
  gap: 10rpx;
}

.method-status {
  font-size: 24rpx;
}

.bound {
  color: $escort-color-primary;
}

.ghost-btn {
  min-width: 86rpx;
  height: 52rpx;
  border: 1rpx solid $escort-color-primary;
  border-radius: 28rpx;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 12rpx;

  text {
    color: $escort-color-primary;
    font-size: 23rpx;
    font-weight: 600;
  }
}

.input-row {
  display: flex;
  align-items: center;
}

.money-input-wrap {
  flex: 1;
  height: 84rpx;
  background: #f8fafc;
  border-radius: 14rpx;
  display: flex;
  align-items: center;
  padding: 0 18rpx;
  box-sizing: border-box;
  margin-right: 12rpx;
}

.prefix {
  color: #1f2937;
  font-size: 32rpx;
  margin-right: 8rpx;
}

.money-input {
  flex: 1;
  font-size: 30rpx;
  color: #111827;
}

.warning {
  display: block;
  margin-top: 12rpx;
  color: #ff4d4f;
  font-size: 24rpx;
}

.estimate {
  display: block;
  margin-top: 8rpx;
  color: $escort-color-primary;
  font-size: 25rpx;
}

.submit-btn {
  height: 90rpx;
  border-radius: 60rpx;
  margin: 24rpx;
  background: linear-gradient(135deg, $escort-color-primary, $escort-color-primary-deep);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: $escort-shadow-primary;

  text {
    color: #ffffff;
    font-size: 32rpx;
    font-weight: 700;
  }
}

.disabled {
  background: #c0c4cc;
  box-shadow: none;
}

.record-head {
  margin-bottom: 8rpx;
}

.record-row {
  min-height: 94rpx;
  border-bottom: 1rpx solid #eef2f7;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10rpx 0;
}

.record-row:last-child {
  border-bottom: none;
}

.left-col {
  display: flex;
  flex-direction: column;
}

.record-time {
  font-size: 24rpx;
  color: #111827;
}

.record-amount {
  margin-top: 6rpx;
  font-size: 22rpx;
  color: #6b7280;
}

.record-status {
  font-size: 24rpx;
  font-weight: 600;
}

.success {
  color: #52c41a;
}

.processing {
  color: $escort-color-primary;
}

.failed {
  color: #ff4d4f;
}

.empty {
  height: 120rpx;
  display: flex;
  align-items: center;
  justify-content: center;

  text {
    color: #9ca3af;
    font-size: 24rpx;
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

.delay-4 {
  animation-delay: 0.2s;
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

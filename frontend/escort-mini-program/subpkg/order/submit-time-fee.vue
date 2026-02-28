<template>
  <view class="container">
    <view class="custom-navbar">
      <view class="navbar-content">
        <view class="nav-left" @click="goBack">
          <image class="back-icon" src="/static/back.svg" mode="aspectFit"></image>
        </view>
        <view class="nav-title">提交时长与费用</view>
        <view class="nav-right"></view>
      </view>
    </view>

    <scroll-view class="content" scroll-y>
      <view class="card">
        <view class="card-title">时长对比</view>
        <view class="time-row">
          <text class="label">预估时长</text>
          <text class="value">
            {{ estimatedDurationDisplay }}小时（{{ serviceTypeName }}）
          </text>
        </view>
        <view class="time-row">
          <text class="label">实际时长</text>
          <view class="duration-input">
            <button class="step-btn" @click="changeDuration(-0.5)">-</button>
            <text class="duration-text">{{ actualDuration.toFixed(1) }} 小时</text>
            <button class="step-btn" @click="changeDuration(0.5)">+</button>
          </view>
        </view>
        <view class="time-row">
          <text class="label">费用差异</text>
          <text
            class="value diff"
            :class="{
              more: feeDiff > 0,
              less: feeDiff < 0
            }"
          >
            {{ diffText }}
          </text>
        </view>
        <view class="rule-text">
          计费规则：普通陪诊起步价50元（含2小时），超出部分30元/小时；不足1小时按1小时计。
        </view>
      </view>

      <view class="card">
        <view class="card-title">陪诊师说明</view>
        <textarea
          class="desc-input"
          v-model="attendantRemark"
          placeholder="可简单说明就诊过程，例如：检查项目较多，排队时间较长等"
          maxlength="200"
          auto-height
        />
        <view class="desc-count">{{ attendantRemark.length }}/200</view>
      </view>
    </scroll-view>

    <view class="bottom-actions">
      <button class="main-btn" :loading="submitting" @click="submit">
        提交时长与费用
      </button>
    </view>
  </view>
</template>

<script>
import { get, post } from '@/utils/api.js'

export default {
  data() {
    return {
      orderId: null,
      serviceTypeNumber: 1,
      serviceTypeName: '普通陪诊',
      estimatedDuration: 2.0,
      orderAmount: 0,
      actualDuration: 2.0,
      feeDiff: 0,
      attendantRemark: '',
      submitting: false
    }
  },
  computed: {
    estimatedDurationDisplay() {
      return this.estimatedDuration ? this.estimatedDuration.toFixed(1) : '--'
    },
    diffText() {
      if (!this.estimatedDuration || this.feeDiff === 0) {
        return '无费用差异'
      }
      if (this.feeDiff > 0) {
        return `需补付¥${this.feeDiff.toFixed(2)}`
      }
      return `自动退款¥${Math.abs(this.feeDiff).toFixed(2)}`
    }
  },
  onLoad(options) {
    if (options.orderId) {
      this.orderId = options.orderId
      this.loadOrder()
    }
  },
  methods: {
    goBack() {
      uni.navigateBack()
    },
    async loadOrder() {
      try {
        const res = await get(`/attendant/orders/${this.orderId}`)
        if (res.code === 200 && res.data) {
          const o = res.data
          this.orderAmount = o.orderAmount || 0
          this.serviceTypeNumber = o.clinicType || 1
          this.serviceTypeName = o.serviceContent || '普通陪诊'

          // 预估时长：优先用 consultationDuration，其次 estimatedDuration，再次按时间段估算
          let est = o.consultationDuration || o.estimatedDuration
          if (!est && o.serviceTimeSlot) {
            const slot = o.serviceTimeSlot
            const parts = slot.split('-')
            if (parts.length === 2) {
              const start = this.parseTime(parts[0])
              const end = this.parseTime(parts[1])
              const minutes = (end - start) / 60000
              if (minutes > 0) {
                est = Math.round((minutes / 60) * 10) / 10
              }
            }
          }
          this.estimatedDuration = est || 2.0

          // 默认实际时长 = 预估时长
          this.actualDuration = this.estimatedDuration
          this.recalcFeeDiff()
        }
      } catch (e) {
        console.error('加载订单失败', e)
      }
    },
    parseTime(t) {
      const [h, m] = t.trim().split(':').map(Number)
      const d = new Date()
      d.setHours(h || 0, m || 0, 0, 0)
      return d
    },
    changeDuration(delta) {
      let v = this.actualDuration + delta
      if (v < 0.5) v = 0.5
      if (v > 24) v = 24
      // 保留一位小数（0.5 步长）
      this.actualDuration = Math.round(v * 2) / 2
      this.recalcFeeDiff()
    },
    recalcFeeDiff() {
      const expectedFee = this.calculateFee(this.serviceTypeNumber, this.actualDuration)
      this.feeDiff = expectedFee - (this.orderAmount || 0)
    },
    calculateFee(serviceType, hours) {
      // 与后端 ServiceFeeCalculator 保持一致的简化版
      const BASE_PRICE = 50
      const EXTEND_PRICE = 30
      const POST_CARE_PRICE = 45

      const h = Math.max(hours, 0)
      if (serviceType === 2) {
        // 术后护理：45元/小时，不足1小时按1小时
        const rounded = Math.ceil(h)
        return rounded * POST_CARE_PRICE
      }

      const calcNormal = () => {
        const actual = Math.max(h, 2)
        let total = BASE_PRICE
        if (actual > 2) {
          const extra = actual - 2
          const roundedExtra = Math.ceil(extra)
          total += roundedExtra * EXTEND_PRICE
        }
        return total
      }

      if (serviceType === 1) {
        return calcNormal()
      }
      if (serviceType === 3) {
        // 急诊陪同：普通陪诊 + 100
        return calcNormal() + 100
      }
      if (serviceType === 4) {
        // 上门陪诊：普通陪诊 + 30
        return calcNormal() + 30
      }
      return calcNormal()
    },
    async submit() {
      if (!this.orderId) return
      if (this.submitting) return
      this.submitting = true
      try {
        const res = await post(`/attendant/orders/${this.orderId}/end?actualDuration=${this.actualDuration}`)
        if (res.code === 200) {
          uni.showToast({ title: '已提交，等待用户确认', icon: 'success' })
          setTimeout(() => {
            uni.navigateBack()
          }, 1200)
        } else {
          uni.showToast({ title: res.message || '提交失败', icon: 'none' })
        }
      } catch (e) {
        console.error('提交时长失败', e)
        uni.showToast({ title: '提交失败', icon: 'none' })
      } finally {
        this.submitting = false
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.container {
  min-height: 100vh;
  background: #f5f5f5;
}
.custom-navbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
  padding-top: var(--status-bar-height);
  background-color: #4A90E2;
  .navbar-content {
    height: 44px;
    display: flex;
    align-items: center;
    padding: 0 15px;
    .nav-left, .nav-right {
      width: 60px;
      height: 44px;
      display: flex;
      align-items: center;
    }
    .back-icon {
      width: 24px;
      height: 24px;
      filter: brightness(0) invert(1);
    }
    .nav-title {
      flex: 1;
      text-align: center;
      font-size: 18px;
      font-weight: 600;
      color: #ffffff;
    }
  }
}
.content {
  padding-top: calc(var(--status-bar-height) + 44px);
  padding-bottom: 90px;
}
.card {
  background: #fff;
  margin: 12px 15px;
  padding: 16px;
  border-radius: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}
.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 12px;
}
.time-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
  .label {
    font-size: 14px;
    color: #666;
  }
  .value {
    font-size: 14px;
    color: #333;
  }
  .value.diff {
    font-weight: 600;
  }
  .value.more {
    color: #ff4d4f;
  }
  .value.less {
    color: #52c41a;
  }
}
.duration-input {
  display: flex;
  align-items: center;
  .step-btn {
    width: 32px;
    height: 32px;
    border-radius: 16px;
    border: 1px solid #4A90E2;
    color: #4A90E2;
    font-size: 18px;
    line-height: 30px;
    text-align: center;
    background: #fff;
  }
  .duration-text {
    margin: 0 10px;
    font-size: 14px;
    color: #333;
  }
}
.rule-text {
  margin-top: 8px;
  font-size: 12px;
  color: #999;
}
.desc-input {
  width: 100%;
  min-height: 80px;
  background: #f8f9fa;
  border-radius: 8px;
  padding: 8px;
  font-size: 14px;
  box-sizing: border-box;
}
.desc-count {
  text-align: right;
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}
.bottom-actions {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 10px 15px;
  padding-bottom: calc(10px + env(safe-area-inset-bottom));
  background: #fff;
  border-top: 1px solid #f0f0f0;
}
.main-btn {
  width: 100%;
  height: 44px;
  line-height: 44px;
  background: #4A90E2;
  color: #fff;
  border-radius: 22px;
  font-size: 16px;
  font-weight: 600;
  border: none;
}
</style>


<template>
  <view class="escort-menu-cell" @click="handleClick">
    <view class="left">
      <slot name="icon">
        <image v-if="icon" class="icon" :src="icon" mode="aspectFit"></image>
      </slot>
      <text class="title">{{ title }}</text>
    </view>
    <view class="right">
      <text v-if="balanceText" class="balance">{{ balanceText }}</text>
      <text v-else-if="statusText" class="status" :class="statusClass">{{ statusText }}</text>
      <text class="arrow">›</text>
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  icon: {
    type: String,
    default: ''
  },
  title: {
    type: String,
    default: ''
  },
  balance: {
    type: [Number, String],
    default: null
  },
  statusText: {
    type: String,
    default: ''
  },
  statusType: {
    type: String,
    default: 'default'
  }
})

const emit = defineEmits(['click'])

const balanceText = computed(() => {
  if (props.balance === null || props.balance === undefined || props.balance === '') {
    return ''
  }
  const amount = Number(props.balance)
  return Number.isFinite(amount) ? `¥${amount.toFixed(2)}` : `¥${props.balance}`
})

const statusClass = computed(() => `status--${props.statusType}`)

const handleClick = () => {
  emit('click')
}
</script>

<style lang="scss" scoped>
@import '@/styles/escort-ui.scss';
.escort-menu-cell {
  height: 96rpx;
  padding: 0 24rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #ffffff;
  transition: background-color 0.2s ease;

  &:active {
    background-color: #f2f8ff;
  }
}

.left {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
}

.icon {
  width: 36rpx;
  height: 36rpx;
  margin-right: 18rpx;
}

.title {
  font-size: 30rpx;
  color: #1f2937;
}

.right {
  display: flex;
  align-items: center;
  gap: 10rpx;
}

.balance {
  font-size: 26rpx;
  color: $escort-color-primary;
  font-weight: 600;
}

.status {
  font-size: 22rpx;
  line-height: 1;
  padding: 8rpx 14rpx;
  border-radius: 999rpx;
  border: 1rpx solid transparent;
}

.status--verified {
  color: #2f7d32;
  background: #edf8ef;
  border-color: #ccead1;
}

.status--pending {
  color: #8b5e00;
  background: #fff6e0;
  border-color: #ffe6a3;
}

.status--failed {
  color: #b42318;
  background: #ffecec;
  border-color: #ffcbcb;
}

.status--blocked {
  color: #ffffff;
  background: #9ca3af;
  border-color: #9ca3af;
}

.status--default {
  color: #6b7280;
  background: #f3f4f6;
  border-color: #e5e7eb;
}

.arrow {
  font-size: 34rpx;
  color: #c0c4cc;
  margin-left: 4rpx;
}
</style>

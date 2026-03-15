<template>
  <view></view>
</template>

<script setup>
import { onLoad } from '@dcloudio/uni-app'

onLoad((options) => {
  const orderNo = options.orderNo || ''
  const orderId = options.orderId || ''
  const query = orderNo ? `orderNo=${encodeURIComponent(orderNo)}` : (orderId ? `orderId=${encodeURIComponent(orderId)}` : '')
  if (!query) {
    uni.showToast({ title: '订单号错误', icon: 'none' })
    uni.navigateBack()
    return
  }
  uni.redirectTo({ url: `/subpkg/order/order-detail?${query}` })
})
</script>

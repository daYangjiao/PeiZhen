<template>
  <view class="page">
    <view class="faq-card" v-for="item in faqs" :key="item.q">
      <text class="q">Q：{{ item.q }}</text>
      <text class="a">A：{{ item.a }}</text>
    </view>
  </view>
</template>

<script setup>
const faqs = [
  {
    q: '为什么接单后看不到用户联系方式？',
    a: '订单进入待服务状态后可在订单详情查看完整联系信息。'
  },
  {
    q: '服务中如何更新进度？',
    a: '进入订单详情，按“已到院/候诊中/检查中/就诊完成”步骤更新。'
  },
  {
    q: '用户未确认时长费用怎么办？',
    a: '系统会提醒用户确认，若存在争议可由平台介入处理。'
  },
  {
    q: '如何修改个人资料？',
    a: '在“我的”页面点击右上角编辑，修改后保存即可生效。'
  }
]
</script>

<style lang="scss" scoped>
@import '@/styles/escort-ui.scss';
.page {
  @include escort-page;
  min-height: 100vh;
  padding: 24rpx;
  box-sizing: border-box;
}

.faq-card {
  background: $escort-color-surface;
  border-radius: $escort-radius-card;
  padding: 20rpx;
  margin-bottom: 14rpx;
  box-shadow: $escort-shadow-card;
}

.q {
  display: block;
  font-size: 27rpx;
  color: #111827;
  font-weight: 600;
  margin-bottom: 8rpx;
}

.a {
  display: block;
  font-size: 24rpx;
  line-height: 1.65;
  color: #4b5563;
}
</style>

<template>
  <view class="rules-container">
    <view class="card">
      <view class="card-title">📋 定价规则</view>
      <view class="rule-item">
        <text>普通陪诊：50元/2小时，超时30元/小时</text>
      </view>
      <view class="rule-item">
        <text>术后护理：白天45元/小时，夜间60元/小时</text>
      </view>
      <view class="rule-item">
        <text>急诊陪同：白天加急100元，夜间加急150元 + 时长费</text>
      </view>
      <view class="rule-item">
        <text>上门陪诊：3公里内免费，超出5元/公里</text>
      </view>
    </view>
    <view class="card">
      <view class="card-title">⚖️ 定金扣除规则</view>
      <view class="rule-item">
        <text>• 开始前2小时以上取消：全额退款</text>
      </view>
      <view class="rule-item">
        <text>• 开始前1-2小时取消：扣除20%定金</text>
      </view>
      <view class="rule-item">
        <text>• 开始前30-60分钟取消：扣除30%定金</text>
      </view>
      <view class="rule-item">
        <text>• 开始前30分钟内取消：扣除50%定金</text>
      </view>
      <view class="rule-item">
        <text>• 服务开始后取消：扣除100%定金</text>
      </view>
    </view>
  </view>
</template>

<style>
.rules-container {
  padding: 30rpx;
  background-color: #f5f7fb;
  min-height: 100vh;
}
.card {
  background-color: white;
  border-radius: 20rpx;
  padding: 30rpx;
  margin-bottom: 30rpx;
  box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.05);
}
.card-title {
  font-size: 36rpx;
  font-weight: bold;
  margin-bottom: 30rpx;
  color: #1e2a3e;
  border-left: 8rpx solid #4A90E2;
  padding-left: 20rpx;
}
.rule-item {
  font-size: 28rpx;
  color: #333;
  padding: 16rpx 0;
  border-bottom: 1rpx solid #eee;
}
.rule-item:last-child {
  border-bottom: none;
}
</style>
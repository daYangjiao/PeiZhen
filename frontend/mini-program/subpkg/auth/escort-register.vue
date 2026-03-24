<template>
  <view class="container">
    <view class="hero">
      <view class="hero-deco hero-deco-left"></view>
      <view class="hero-deco hero-deco-right"></view>

      <view class="nav-bar">
        <view class="back-btn" @click="goBack">
          <text class="back-icon">←</text>
        </view>
      </view>

      <view class="hero-content">
        <view class="hero-badge">
          <text class="hero-badge-text">Escort Partner</text>
        </view>
        <text class="title">陪诊师入驻</text>
        <text class="subtitle">提交基础信息后即可进入审核流程，审核通过后开始接单服务</text>

        <view class="hero-tags">
          <view class="hero-tag">
            <text class="hero-tag-text">实名认证</text>
          </view>
          <view class="hero-tag">
            <text class="hero-tag-text">资质审核</text>
          </view>
          <view class="hero-tag">
            <text class="hero-tag-text">平台派单</text>
          </view>
        </view>
      </view>
    </view>

    <view class="content">
      <view class="section-card form-card">
        <view class="section-head">
          <text class="section-title">基础信息</text>
          <text class="section-desc">请填写真实资料，后续资质审核将以此为准</text>
        </view>

        <view class="field-grid">
          <view class="field-item">
            <text class="field-label">登录密码</text>
            <view class="field-box">
              <text class="field-prefix">密码</text>
              <input class="field-input" v-model="form.password" type="password" placeholder="请输入登录密码" password />
            </view>
          </view>

          <view class="field-item">
            <text class="field-label">真实姓名</text>
            <view class="field-box">
              <text class="field-prefix">姓名</text>
              <input class="field-input" v-model="form.name" placeholder="请输入您的真实姓名" />
            </view>
          </view>

          <view class="field-item">
            <text class="field-label">手机号码</text>
            <view class="field-box">
              <text class="field-prefix">手机</text>
              <input class="field-input" v-model="form.phone" type="number" maxlength="11" placeholder="请输入手机号" />
            </view>
          </view>

          <view class="field-item">
            <text class="field-label">专业领域</text>
            <view class="field-box">
              <text class="field-prefix">方向</text>
              <input class="field-input" v-model="form.professionalField" placeholder="如：术后护理、挂号引导" />
            </view>
          </view>
        </view>

        <view class="field-item field-item-full">
          <view class="field-head">
            <text class="field-label">个人简介</text>
            <text class="field-tip">建议填写擅长服务、经验年限、沟通特点</text>
          </view>
          <view class="textarea-box">
            <textarea
              class="textarea"
              v-model="form.introduction"
              maxlength="200"
              placeholder="例如：熟悉三甲医院就诊流程，擅长术后陪护、急诊陪同、检查引导等服务"
            />
          </view>
        </view>
      </view>

      <view class="section-card guide-card">
        <view class="section-head">
          <text class="section-title">入驻说明</text>
          <text class="section-desc">注册完成后，可在个人中心继续补充资质并提交审核</text>
        </view>

        <view class="guide-list">
          <view class="guide-item">
            <view class="guide-index"><text class="guide-index-text">1</text></view>
            <view class="guide-body">
              <text class="guide-title">使用手机号登录</text>
              <text class="guide-text">提交成功后请使用当前手机号和设置的密码登录陪诊师端。</text>
            </view>
          </view>
          <view class="guide-item">
            <view class="guide-index"><text class="guide-index-text">2</text></view>
            <view class="guide-body">
              <text class="guide-title">补充资质材料</text>
              <text class="guide-text">登录后在个人中心上传身份证、执业证书、健康证等资料。</text>
            </view>
          </view>
          <view class="guide-item">
            <view class="guide-index"><text class="guide-index-text">3</text></view>
            <view class="guide-body">
              <text class="guide-title">审核通过后接单</text>
              <text class="guide-text">资质审核通过后即可在接单大厅查看订单并开始服务。</text>
            </view>
          </view>
        </view>
      </view>

      <view class="action-area">
        <button class="submit-btn" @click="handleRegister">提交入驻申请</button>
        <view class="login-link" @click="goLogin">
          已有账号？<text class="link-text">立即登录</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { post } from '@/utils/api.js'

const form = ref({
  password: '',
  name: '',
  phone: '',
  professionalField: '',
  introduction: '',
  userType: 1
})

const goBack = () => {
  uni.navigateBack()
}

const goLogin = () => {
  uni.redirectTo({ url: '/pages/auth/login?role=escort' })
}

const handleRegister = async () => {
  if (!form.value.password || !form.value.name || !form.value.phone) {
    uni.showToast({ title: '请填写必填项', icon: 'none' })
    return
  }
  if (!/^1\\d{10}$/.test(form.value.phone)) {
    uni.showToast({ title: '请输入正确手机号', icon: 'none' })
    return
  }
  uni.showLoading({ title: '提交中...' })
  try {
    const res = await post('/api/users/register', form.value)
    uni.hideLoading()
    if (res.code === 200) {
      uni.showModal({
        title: '注册成功',
        content: '您的入驻申请已提交，请使用手机号和密码登录',
        showCancel: false,
        success: () => {
          goLogin()
        }
      })
    } else {
      uni.showToast({ title: res.message || '提交失败', icon: 'none' })
    }
  } catch (e) {
    uni.hideLoading()
    uni.showToast({ title: e.message || '提交失败', icon: 'none' })
  }
}
</script>

<style lang="scss" scoped>
@import '@/styles/escort-ui.scss';

.container {
  @include escort-page;
  position: relative;
  padding-bottom: 40rpx;
  overflow: hidden;
}

.hero {
  position: relative;
  padding: calc(var(--status-bar-height) + 16rpx) 24rpx 88rpx;
  background:
    radial-gradient(circle at top left, rgba(0, 122, 255, 0.18), transparent 34%),
    linear-gradient(180deg, #eef5ff 0%, #f6f9ff 100%);
}

.hero-deco {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.4);
  filter: blur(2rpx);
}

.hero-deco-left {
  width: 220rpx;
  height: 220rpx;
  top: 36rpx;
  right: -40rpx;
}

.hero-deco-right {
  width: 140rpx;
  height: 140rpx;
  top: 188rpx;
  left: -30rpx;
}

.nav-bar {
  display: flex;
  align-items: center;
  margin-bottom: 28rpx;
}

.back-btn {
  width: 64rpx;
  height: 64rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.88);
  border: 1rpx solid rgba(0, 122, 255, 0.08);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8rpx 20rpx rgba(37, 99, 235, 0.08);
}

.back-icon {
  font-size: 38rpx;
  line-height: 1;
  color: $escort-color-primary;
}

.hero-content {
  position: relative;
  z-index: 1;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  padding: 10rpx 18rpx;
  border-radius: $escort-radius-pill;
  background: rgba(255, 255, 255, 0.7);
  border: 1rpx solid rgba(0, 122, 255, 0.08);
  margin-bottom: 20rpx;
}

.hero-badge-text {
  font-size: 22rpx;
  font-weight: 600;
  letter-spacing: 1rpx;
  color: $escort-color-primary-deep;
}

.title {
  display: block;
  font-size: 46rpx;
  font-weight: 700;
  line-height: 1.2;
  color: $escort-color-text-main;
}

.subtitle {
  display: block;
  margin-top: 12rpx;
  font-size: 26rpx;
  line-height: 1.7;
  color: $escort-color-text-sub;
}

.hero-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 14rpx;
  margin-top: 26rpx;
}

.hero-tag {
  padding: 10rpx 18rpx;
  border-radius: $escort-radius-pill;
  background: rgba(255, 255, 255, 0.9);
  border: 1rpx solid rgba(0, 122, 255, 0.08);
}

.hero-tag-text {
  font-size: 22rpx;
  color: $escort-color-text-sub;
}

.content {
  position: relative;
  margin-top: -34rpx;
  padding: 0 24rpx;
  z-index: 2;
}

.section-card {
  @include escort-card(28rpx);
  margin-bottom: 24rpx;
  border: 1rpx solid rgba(220, 232, 248, 0.9);
}

.section-head {
  margin-bottom: 24rpx;
}

.section-title {
  display: block;
  font-size: 32rpx;
  font-weight: 700;
  color: $escort-color-text-main;
}

.section-desc {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  line-height: 1.7;
  color: $escort-color-text-sub;
}

.field-grid {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.field-item {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.field-label {
  font-size: 25rpx;
  font-weight: 600;
  color: $escort-color-text-main;
}

.field-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}

.field-tip {
  font-size: 22rpx;
  color: $escort-color-text-sub;
}

.field-box,
.textarea-box {
  display: flex;
  align-items: center;
  width: 100%;
  box-sizing: border-box;
  border-radius: 18rpx;
  border: 1rpx solid $escort-color-border;
  background: linear-gradient(180deg, #fbfdff 0%, #f4f8ff 100%);
}

.field-box {
  height: 92rpx;
  padding: 0 20rpx;
}

.field-prefix {
  flex-shrink: 0;
  width: 72rpx;
  font-size: 24rpx;
  font-weight: 600;
  color: $escort-color-primary-deep;
}

.field-input {
  flex: 1;
  height: 92rpx;
  font-size: 28rpx;
  color: $escort-color-text-main;
}

.textarea-box {
  padding: 18rpx 20rpx;
}

.textarea {
  width: 100%;
  min-height: 184rpx;
  font-size: 27rpx;
  line-height: 1.7;
  color: $escort-color-text-main;
}

.guide-card {
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98) 0%, rgba(246, 249, 255, 0.98) 100%);
}

.guide-list {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.guide-item {
  display: flex;
  gap: 18rpx;
  align-items: flex-start;
}

.guide-index {
  width: 40rpx;
  height: 40rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(0, 122, 255, 0.12), rgba(37, 99, 235, 0.2));
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-top: 4rpx;
}

.guide-index-text {
  font-size: 22rpx;
  font-weight: 700;
  color: $escort-color-primary-deep;
}

.guide-body {
  flex: 1;
}

.guide-title {
  display: block;
  font-size: 26rpx;
  font-weight: 600;
  color: $escort-color-text-main;
}

.guide-text {
  display: block;
  margin-top: 6rpx;
  font-size: 24rpx;
  line-height: 1.7;
  color: $escort-color-text-sub;
}

.action-area {
  padding: 8rpx 0 16rpx;
}

.submit-btn {
  @include escort-primary-btn;
  font-size: 30rpx;
  font-weight: 700;
  border: 0;
}

.submit-btn::after {
  border: 0;
}

.login-link {
  text-align: center;
  margin-top: 24rpx;
  font-size: 24rpx;
  color: $escort-color-text-sub;
}

.link-text {
  color: $escort-color-primary;
  font-weight: 600;
}
</style>

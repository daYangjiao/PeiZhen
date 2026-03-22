<template>
  <view class="container">
    <view class="header-bg">
      <view class="back-btn" @click="goBack">
        <text class="back-icon">←</text>
      </view>
      <text class="title">陪诊师入驻</text>
      <text class="subtitle">加入我们，开启专业陪诊之旅</text>
    </view>

    <view class="form-card">
      <view class="input-item">
        <text class="label">登录密码</text>
        <input class="input" v-model="form.password" type="password" placeholder="设置登录密码" />
      </view>
      <view class="input-item">
        <text class="label">真实姓名</text>
        <input class="input" v-model="form.name" placeholder="请输入您的真实姓名" />
      </view>
      <view class="input-item">
        <text class="label">手机号码</text>
        <input class="input" v-model="form.phone" type="number" maxlength="11" placeholder="请输入手机号" />
      </view>
      <view class="input-item">
        <text class="label">专业领域</text>
        <input class="input" v-model="form.professionalField" placeholder="如：术后护理, 挂号引导" />
      </view>
      <view class="input-item">
        <text class="label">个人简介</text>
        <textarea class="textarea" v-model="form.introduction" placeholder="简单介绍您的服务经验..." />
      </view>

      <button class="submit-btn" @click="handleRegister">提交入驻申请</button>

      <view class="login-link" @click="goLogin">
        已有账号？<text class="link-text">立即登录</text>
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
  uni.showLoading({ title: '提交中...' })
  try {
    const res = await post('/api/users/register', form.value)
    uni.hideLoading()
    if (res.code === 200) {
      uni.showModal({
        title: '注册成功',
        content: '您的入驻申请已提交，请使用账号登录',
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
  min-height: 100vh;
  background:
    radial-gradient(circle at top left, rgba(0, 122, 255, 0.12), transparent 34%),
    linear-gradient(180deg, #f2f8ff 0%, #f7fbff 240rpx, #ffffff 100%);
  --primary: #{$escort-color-primary};
  --primary-deep: #{$escort-color-primary-deep};
  --text-main: #{$escort-color-text-main};
  --text-sub: #{$escort-color-text-sub};
}
.header-bg {
  padding: calc(var(--status-bar-height) + 24rpx) 30rpx 120rpx;
  background: transparent;
  color: var(--text-main);
}
.back-btn {
  width: 64rpx;
  height: 64rpx;
  border-radius: 50%;
  background: rgba(0, 122, 255, 0.08);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20rpx;
}
.back-icon {
  font-size: 38rpx;
  line-height: 1;
  color: var(--primary);
}
.title {
  font-size: 44rpx;
  font-weight: 700;
  display: block;
  margin-bottom: 10rpx;
}
.subtitle {
  font-size: 24rpx;
  color: var(--text-sub);
}
.form-card {
  margin: -36rpx 24rpx 24rpx;
  background: #fff;
  border-radius: 28rpx;
  padding: 30rpx 24rpx;
  box-shadow: 0 18rpx 42rpx rgba(18, 56, 109, 0.1);
  border: 1rpx solid rgba(0, 122, 255, 0.08);
}
.input-item {
  margin-bottom: 20rpx;
}
.label {
  font-size: 24rpx;
  color: var(--text-main);
  font-weight: 600;
  margin-bottom: 10rpx;
  display: block;
}
.input {
  height: 84rpx;
  background: #f8fbff;
  border-radius: 16rpx;
  padding: 0 18rpx;
  font-size: 27rpx;
  color: var(--text-main);
  border: 1rpx solid #dce8f8;
}
.textarea {
  min-height: 160rpx;
  background: #f8fbff;
  border-radius: 16rpx;
  padding: 16rpx 18rpx;
  font-size: 27rpx;
  color: var(--text-main);
  width: 100%;
  box-sizing: border-box;
  border: 1rpx solid #dce8f8;
}
.submit-btn {
  background: linear-gradient(135deg, var(--primary), var(--primary-deep));
  color: #fff;
  height: 88rpx;
  border-radius: 44rpx;
  font-size: 30rpx;
  font-weight: 700;
  margin-top: 10rpx;
  box-shadow: 0 12rpx 28rpx rgba(0, 122, 255, 0.22);
}
.login-link {
  text-align: center;
  margin-top: 22rpx;
  font-size: 24rpx;
  color: var(--text-sub);
}
.link-text {
  color: var(--primary);
  font-weight: 600;
}
</style>

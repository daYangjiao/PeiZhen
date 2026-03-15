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
        <text class="label">用户名</text>
        <input class="input" v-model="form.username" placeholder="设置登录用户名" />
      </view>
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
  username: '',
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
  if (!form.value.username || !form.value.password || !form.value.name || !form.value.phone) {
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

<style scoped>
.container {
  min-height: 100vh;
  background-color: #f8f9fa;
}
.header-bg {
  height: 220px;
  background: linear-gradient(135deg, #4A90E2, #357ABD);
  padding: 60px 20px 0;
  color: #fff;
}
.back-btn {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  margin-bottom: 10px;
}
.title {
  font-size: 28px;
  font-weight: bold;
  display: block;
  margin-bottom: 8px;
}
.subtitle {
  font-size: 14px;
  opacity: 0.8;
}
.form-card {
  margin: -40px 20px 20px;
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
}
.input-item {
  margin-bottom: 20px;
}
.label {
  font-size: 14px;
  color: #333;
  font-weight: 500;
  margin-bottom: 8px;
  display: block;
}
.input {
  height: 44px;
  background: #f8f9fa;
  border-radius: 8px;
  padding: 0 12px;
  font-size: 15px;
}
.textarea {
  height: 80px;
  background: #f8f9fa;
  border-radius: 8px;
  padding: 12px;
  font-size: 15px;
  width: 100%;
  box-sizing: border-box;
}
.submit-btn {
  background: linear-gradient(135deg, #4A90E2, #357ABD);
  color: #fff;
  height: 48px;
  border-radius: 24px;
  font-size: 16px;
  font-weight: bold;
  margin-top: 10px;
}
.login-link {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
  color: #666;
}
.link-text {
  color: #4A90E2;
  font-weight: 500;
}
</style>


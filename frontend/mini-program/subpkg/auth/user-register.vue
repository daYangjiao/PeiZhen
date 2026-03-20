<template>
  <view class="container">
    <view class="header-section">
      <view class="circle-1"></view>
      <view class="circle-2"></view>
      <image class="logo" src="/static/mynewlogo.png" mode="aspectFit"></image>
      <text class="welcome-text">创建用户账号</text>
      <text class="header-tip">注册后可直接预约陪诊、查看订单并与陪诊师沟通</text>
    </view>

    <view class="register-card">
      <view class="card-title">手机号注册</view>
      <view class="card-subtitle">请填写基础信息，完成后回到登录页即可使用手机号密码登录</view>

      <view class="input-group">
        <view class="input-item">
          <text class="iconfont">📱</text>
          <input class="input" v-model="form.phone" type="number" maxlength="11" placeholder="请输入手机号" />
        </view>
        <view class="input-item">
          <text class="iconfont">👤</text>
          <input class="input" v-model="form.name" maxlength="20" placeholder="请输入姓名" />
        </view>
        <view class="input-item">
          <text class="iconfont">🔒</text>
          <input class="input" v-model="form.password" type="password" maxlength="20" placeholder="请设置登录密码" />
        </view>
        <view class="input-item">
          <text class="iconfont">✅</text>
          <input class="input" v-model="confirmPassword" type="password" maxlength="20" placeholder="请再次输入密码" />
        </view>
      </view>

      <view class="agreement-row">
        <checkbox-group @change="onCheckChange">
          <label class="checkbox-label">
            <checkbox :checked="agreed" color="#4A90E2" style="transform:scale(0.7)" />
            <text class="agreement-text">我已阅读并同意<text class="link">《服务协议》</text></text>
          </label>
        </checkbox-group>
      </view>

      <button class="submit-btn" :disabled="loading" @click="handleRegister">
        {{ loading ? '注册中...' : '注册并去登录' }}
      </button>

      <view class="login-link" @click="goLogin">
        已有账号？<text class="link-text">立即登录</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { post } from '@/utils/api.js'

const phonePattern = /^1\d{10}$/

const form = ref({
  phone: '',
  name: '',
  password: '',
  userType: 0
})
const confirmPassword = ref('')
const agreed = ref(false)
const loading = ref(false)

const onCheckChange = (e) => {
  agreed.value = e.detail.value.length > 0
}

const goLogin = () => {
  uni.redirectTo({ url: '/pages/auth/login?role=user' })
}

const validateForm = () => {
  if (!agreed.value) {
    uni.showToast({ title: '请先同意协议', icon: 'none' })
    return false
  }
  if (!phonePattern.test(form.value.phone.trim())) {
    uni.showToast({ title: '请输入正确的手机号', icon: 'none' })
    return false
  }
  if (form.value.name.trim().length < 2) {
    uni.showToast({ title: '姓名至少输入 2 个字', icon: 'none' })
    return false
  }
  if (form.value.password.length < 6) {
    uni.showToast({ title: '密码至少输入 6 位', icon: 'none' })
    return false
  }
  if (form.value.password !== confirmPassword.value) {
    uni.showToast({ title: '两次输入的密码不一致', icon: 'none' })
    return false
  }
  return true
}

const handleRegister = async () => {
  if (!validateForm()) return

  loading.value = true
  uni.showLoading({ title: '注册中...' })
  try {
    const res = await post('/api/users/register', {
      phone: form.value.phone.trim(),
      name: form.value.name.trim(),
      password: form.value.password,
      userType: 0
    })
    uni.hideLoading()
    if (res.code === 200) {
      uni.showModal({
        title: '注册成功',
        content: '账号已创建成功，请使用手机号和密码登录。',
        showCancel: false,
        success: goLogin
      })
      return
    }
    uni.showToast({ title: res.message || '注册失败', icon: 'none' })
  } catch (error) {
    uni.hideLoading()
    uni.showToast({ title: error?.message || '注册失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.container {
  min-height: 100vh;
  background-color: #f5f7fa;
  position: relative;
  overflow: hidden;
}

.header-section {
  height: 320px;
  background: linear-gradient(135deg, #4A90E2, #357ABD);
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 80px;
  position: relative;

  .circle-1 {
    position: absolute;
    width: 200px;
    height: 200px;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.1);
    top: -50px;
    right: -50px;
  }

  .circle-2 {
    position: absolute;
    width: 150px;
    height: 150px;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.08);
    bottom: 20px;
    left: -30px;
  }
}

.logo {
  width: 120px;
  height: 120px;
  margin-bottom: 20px;
}

.welcome-text {
  font-size: 28px;
  color: #fff;
  font-weight: bold;
  margin-bottom: 10px;
}

.header-tip {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.88);
}

.register-card {
  margin: -60px 30px 30px;
  background: #fff;
  border-radius: 24px;
  padding: 32px 24px 28px;
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.08);
  position: relative;
  z-index: 10;
}

.card-title {
  font-size: 22px;
  font-weight: 700;
  color: #333;
  margin-bottom: 10px;
}

.card-subtitle {
  font-size: 14px;
  color: #999;
  line-height: 1.6;
  margin-bottom: 24px;
}

.input-group {
  margin-bottom: 20px;
}

.input-item {
  display: flex;
  align-items: center;
  background: #f8f9fa;
  border-radius: 12px;
  padding: 0 16px;
  margin-bottom: 16px;
  border: 1px solid #e9ecef;
}

.iconfont {
  font-size: 18px;
  margin-right: 12px;
}

.input {
  flex: 1;
  height: 52px;
  font-size: 16px;
  color: #333;
}

.agreement-row {
  margin-bottom: 20px;
}

.checkbox-label {
  display: flex;
  align-items: center;
}

.agreement-text {
  font-size: 14px;
  color: #666;
}

.link {
  color: #4A90E2;
}

.submit-btn {
  width: 100%;
  height: 52px;
  background: linear-gradient(135deg, #4A90E2, #357ABD);
  color: #fff;
  border-radius: 26px;
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 18px;
}

.login-link {
  text-align: center;
  font-size: 14px;
  color: #666;
}

.link-text {
  color: #4A90E2;
  font-weight: 600;
}
</style>

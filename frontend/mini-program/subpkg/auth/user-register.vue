<template>
  <view class="container">
    <view class="header-section">
      <view class="circle-1"></view>
      <view class="circle-2"></view>
      <image class="logo" :src="brandLogo" mode="aspectFit"></image>
      <text class="welcome-text">创建用户账号</text>
      <text class="header-tip">注册后可直接预约陪诊、查看订单并与陪诊师沟通</text>
    </view>

    <view class="register-card">
      <view class="card-title">手机号注册</view>
      <view class="card-subtitle">请填写基础信息，完成后回到登录页即可使用手机号密码登录</view>

      <view class="avatar-section">
        <text class="avatar-section-title">头像设置</text>
        <text class="avatar-section-tip">可上传自定义头像；如果不上传，将使用默认用户头像</text>
        <view class="avatar-preview-wrap">
          <image class="avatar-preview" :src="getAvatarPreview(activeAvatar)" mode="aspectFill"></image>
          <button class="avatar-upload-btn" :disabled="uploadingAvatar" @click="chooseAvatar">
            {{ uploadingAvatar ? '上传中...' : '上传头像（可选）' }}
          </button>
        </view>
        <view class="avatar-grid">
          <view
            v-for="item in userDefaultAvatarOptions"
            :key="item.value"
            class="avatar-option"
            :class="{ active: activeAvatar === item.value }"
            @click="selectDefaultAvatar(item.value)"
          >
            <image class="avatar-option-image" :src="getAvatarPreview(item.value)" mode="aspectFill"></image>
          </view>
        </view>
      </view>

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
            <checkbox :checked="agreed" color="#007AFF" style="transform:scale(0.7)" />
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
import { config } from '@/utils/api.js'
import { uploadPublicAvatarImage } from '@/api/user.js'
import { brandLogo, userDefaultAvatarOptions } from '@/utils/assets.js'

const phonePattern = /^1\d{10}$/
const defaultUserAvatar = userDefaultAvatarOptions[Math.floor(Math.random() * userDefaultAvatarOptions.length)].value

const form = ref({
  phone: '',
  name: '',
  password: '',
  avatar: defaultUserAvatar,
  userType: 0
})
const confirmPassword = ref('')
const agreed = ref(false)
const loading = ref(false)
const uploadingAvatar = ref(false)
const activeAvatar = ref(defaultUserAvatar)

const onCheckChange = (e) => {
  agreed.value = e.detail.value.length > 0
}

const goLogin = () => {
  uni.redirectTo({ url: '/pages/auth/login?role=user' })
}

const getAvatarPreview = (value) => {
  if (!value) return ''
  if (value.startsWith('http')) return value
  return `${config.baseURL}${value}`
}

const selectDefaultAvatar = (value) => {
  activeAvatar.value = value
  form.value.avatar = value
}

const chooseAvatar = () => {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: async (res) => {
      const filePath = res.tempFilePaths?.[0]
      if (!filePath) return
      uploadingAvatar.value = true
      uni.showLoading({ title: '上传中...' })
      try {
        const response = await uploadPublicAvatarImage(filePath)
        const avatarUrl = response?.data?.avatarUrl || response?.data
        if (avatarUrl) {
          activeAvatar.value = avatarUrl
          form.value.avatar = avatarUrl
          uni.showToast({ title: '头像已上传', icon: 'success' })
        }
      } catch (error) {
        uni.showToast({ title: error?.message || '头像上传失败', icon: 'none' })
      } finally {
        uni.hideLoading()
        uploadingAvatar.value = false
      }
    }
  })
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
      avatar: form.value.avatar || activeAvatar.value,
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
  display: flex;
  flex-direction: column;
  background:
    radial-gradient(circle at top left, rgba(0, 122, 255, 0.12), transparent 34%),
    linear-gradient(180deg, #f2f8ff 0%, #f7fbff 240rpx, #ffffff 100%);
  position: relative;
  overflow: hidden;
}

.header-section {
  min-height: 220px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
  padding: 34px 20px 28px;
  position: relative;
  text-align: center;

  .circle-1 {
    position: absolute;
    width: 240px;
    height: 240px;
    border-radius: 50%;
    background: rgba(0, 122, 255, 0.08);
    top: -40px;
    right: -60px;
  }

  .circle-2 {
    position: absolute;
    width: 180px;
    height: 180px;
    border-radius: 50%;
    background: rgba(37, 99, 235, 0.06);
    bottom: 20px;
    left: -40px;
  }
}

.logo {
  width: 92px;
  height: 92px;
  margin-bottom: 12px;
}

.welcome-text {
  font-size: 24px;
  color: #16324f;
  font-weight: bold;
  margin-bottom: 6px;
}

.header-tip {
  font-size: 13px;
  color: #6a7f94;
}

.register-card {
  margin: 0 18px 18px;
  background: #fff;
  border-radius: 28px;
  padding: 22px 18px 18px;
  box-shadow: 0 18px 42px rgba(18, 56, 109, 0.1);
  position: relative;
  z-index: 10;
  border: 1px solid rgba(0, 122, 255, 0.08);
  flex: 1;
  min-height: 0;
}

.card-title {
  font-size: 19px;
  font-weight: 700;
  color: #333;
  margin-bottom: 6px;
}

.card-subtitle {
  font-size: 12px;
  color: #6a7f94;
  line-height: 1.6;
  margin-bottom: 14px;
}

.avatar-section {
  margin-bottom: 14px;
}

.avatar-section-title {
  display: block;
  font-size: 15px;
  font-weight: 600;
  color: #16324f;
  margin-bottom: 4px;
}

.avatar-section-tip {
  display: block;
  font-size: 12px;
  color: #6a7f94;
  margin-bottom: 12px;
}

.avatar-preview-wrap {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.avatar-preview {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  border: 3px solid rgba(0, 122, 255, 0.12);
  background: #eef5ff;
}

.avatar-upload-btn {
  flex: 1;
  height: 40px;
  border-radius: 999px;
  background: #eef5ff;
  color: #007aff;
  font-size: 13px;
  font-weight: 600;
  border: 1px solid rgba(0, 122, 255, 0.12);
}

.avatar-grid {
  display: flex;
  gap: 10px;
}

.avatar-option {
  width: 54px;
  height: 54px;
  border-radius: 50%;
  padding: 3px;
  background: transparent;
  border: 2px solid transparent;
  box-sizing: border-box;
}

.avatar-option.active {
  border-color: #007aff;
}

.avatar-option-image {
  width: 100%;
  height: 100%;
  border-radius: 50%;
}

.input-group {
  margin-bottom: 14px;
}

.input-item {
  display: flex;
  align-items: center;
  background: #f8fbff;
  border-radius: 16px;
  padding: 0 14px;
  margin-bottom: 10px;
  border: 1px solid #dce8f8;
}

.iconfont {
  font-size: 18px;
  margin-right: 12px;
  color: #7d8ea2;
}

.input {
  flex: 1;
  height: 44px;
  font-size: 14px;
  color: #16324f;
}

.agreement-row {
  margin-bottom: 12px;
}

.checkbox-label {
  display: flex;
  align-items: center;
}

.agreement-text {
  font-size: 14px;
  color: #6a7f94;
}

.link {
  color: #007aff;
}

.submit-btn {
  width: 100%;
  height: 44px;
  background: linear-gradient(135deg, #007aff, #2563eb);
  color: #fff;
  border-radius: 22px;
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 12px;
  box-shadow: 0 10px 24px rgba(0, 122, 255, 0.24);
}

.login-link {
  text-align: center;
  font-size: 14px;
  color: #6a7f94;
}

.link-text {
  color: #007aff;
  font-weight: 600;
}
</style>

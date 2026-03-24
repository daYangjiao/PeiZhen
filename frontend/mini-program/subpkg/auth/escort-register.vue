<template>
  <view class="container">
    <view class="header-section">
      <view class="circle-1"></view>
      <view class="circle-2"></view>
      <image class="logo" :src="brandLogo" mode="aspectFit"></image>
      <text class="welcome-text">提交陪诊师入驻</text>
      <text class="header-tip">填写基础资料后进入审核流程，审核通过后即可开始接单服务</text>
    </view>

    <view class="register-card">
      <view class="card-title">陪诊师注册</view>
      <view class="card-subtitle">请使用真实资料注册，提交成功后可使用手机号和密码登录陪诊师端并补充资质信息。</view>

      <AvatarPickerField
        v-model="form.avatar"
        :options="escortDefaultAvatarOptions"
        title="头像设置"
        tip="支持上传本人头像，系统会自动裁成方形并压缩；不上传时会随机给一张陪诊师头像。"
        preview-title="陪诊形象"
        preview-desc="注册、资料编辑和小程序端都使用同一套头像处理逻辑。"
        upload-label="上传本人头像（可选）"
      />

      <view class="input-group">
        <view class="input-item">
          <text class="iconfont">📱</text>
          <input class="input" v-model="form.phone" type="number" maxlength="11" placeholder="请输入手机号" />
        </view>
        <view class="input-item">
          <text class="iconfont">👤</text>
          <input class="input" v-model="form.name" maxlength="20" placeholder="请输入真实姓名" />
        </view>
        <view class="input-item">
          <text class="iconfont">🔒</text>
          <input class="input" v-model="form.password" type="password" maxlength="20" placeholder="请设置登录密码" />
        </view>
        <view class="input-item">
          <text class="iconfont">✅</text>
          <input class="input" v-model="confirmPassword" type="password" maxlength="20" placeholder="请再次输入密码" />
        </view>
        <view class="input-item">
          <text class="iconfont">🩺</text>
          <input class="input" v-model="form.professionalField" maxlength="30" placeholder="请输入擅长领域，如术后护理、急诊陪同" />
        </view>
      </view>

      <view class="textarea-section">
        <view class="textarea-label">个人简介</view>
        <view class="textarea-tip">建议填写擅长服务、服务经验和沟通风格，帮助平台后续审核。</view>
        <view class="textarea-box">
          <textarea
            class="textarea"
            v-model="form.introduction"
            maxlength="200"
            placeholder="例如：熟悉三甲医院就诊流程，擅长术后护理、检查引导和急诊陪同等服务。"
          />
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
        {{ loading ? '提交中...' : '提交入驻申请' }}
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
import { brandLogo, escortDefaultAvatarOptions } from '@/utils/assets.js'
import AvatarPickerField from '@/components/AvatarPickerField.vue'

const phonePattern = /^1\d{10}$/
const defaultEscortAvatar = escortDefaultAvatarOptions[Math.floor(Math.random() * escortDefaultAvatarOptions.length)].value

const form = ref({
  phone: '',
  name: '',
  password: '',
  avatar: defaultEscortAvatar,
  professionalField: '',
  introduction: '',
  userType: 1
})
const confirmPassword = ref('')
const agreed = ref(false)
const loading = ref(false)

const onCheckChange = (e) => {
  agreed.value = e.detail.value.length > 0
}

const goLogin = () => {
  uni.redirectTo({ url: '/pages/auth/login?role=escort' })
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
  if (form.value.professionalField.trim().length < 2) {
    uni.showToast({ title: '请填写擅长领域', icon: 'none' })
    return false
  }
  if (form.value.introduction.trim().length < 10) {
    uni.showToast({ title: '个人简介至少输入 10 个字', icon: 'none' })
    return false
  }
  return true
}

const handleRegister = async () => {
  if (!validateForm()) return

  loading.value = true
  uni.showLoading({ title: '提交中...' })
  try {
    const res = await post('/api/users/register', {
      phone: form.value.phone.trim(),
      name: form.value.name.trim(),
      password: form.value.password,
      avatar: form.value.avatar,
      professionalField: form.value.professionalField.trim(),
      introduction: form.value.introduction.trim(),
      userType: 1
    })
    uni.hideLoading()
    if (res.code === 200) {
      uni.showModal({
        title: '提交成功',
        content: '入驻申请已提交，请使用手机号和密码登录陪诊师端。',
        showCancel: false,
        success: goLogin
      })
      return
    }
    uni.showToast({ title: res.message || '提交失败', icon: 'none' })
  } catch (error) {
    uni.hideLoading()
    uni.showToast({ title: error?.message || '提交失败', icon: 'none' })
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
  min-height: 190px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
  padding: 24px 20px 18px;
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
  width: 78px;
  height: 78px;
  margin-bottom: 10px;
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
  margin: 0 18px 16px;
  background: #fff;
  border-radius: 28px;
  padding: 18px 18px 16px;
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
  margin-bottom: 18px;
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

.textarea-section {
  margin-bottom: 14px;
}

.textarea-label {
  font-size: 14px;
  font-weight: 600;
  color: #16324f;
  margin-bottom: 6px;
}

.textarea-tip {
  font-size: 12px;
  line-height: 1.6;
  color: #6a7f94;
  margin-bottom: 8px;
}

.textarea-box {
  background: #f8fbff;
  border-radius: 16px;
  border: 1px solid #dce8f8;
  padding: 12px 14px;
}

.textarea {
  width: 100%;
  min-height: 92px;
  font-size: 14px;
  color: #16324f;
  line-height: 1.6;
}

.agreement-row {
  margin-bottom: 18px;
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
  margin-bottom: 18px;
  box-shadow: 0 10px 24px rgba(0, 122, 255, 0.24);
}

.login-link {
  text-align: center;
  font-size: 14px;
  color: #6a7f94;
  margin-top: 6px;
}

.link-text {
  color: #007aff;
  font-weight: 600;
}
</style>

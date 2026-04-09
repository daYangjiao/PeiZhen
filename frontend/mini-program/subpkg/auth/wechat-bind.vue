<template>
  <view class="container">
    <view class="hero-card">
      <view class="wechat-badge">
        <image class="wechat-icon" src="/static/wechat-icon.png" mode="aspectFit"></image>
        <text class="wechat-badge-text">微信快捷登录</text>
      </view>
      <view class="title">绑定手机号后即可一键进入</view>
      <view class="desc">
        我们会把当前微信身份和你的手机号账号关联起来。下次使用微信登录时，就不用再重复输入密码了。
      </view>
    </view>

    <view class="bind-card">
      <view class="section-title">绑定规则</view>
      <view class="rule-list">
        <view class="rule-item">
          <view class="rule-dot"></view>
          <text class="rule-text">如果这个手机号已经注册，请输入原来的登录密码完成绑定。</text>
        </view>
        <view class="rule-item">
          <view class="rule-dot"></view>
          <text class="rule-text">如果这个手机号还没注册，我们会直接为你创建一个{{ roleLabel }}账号。</text>
        </view>
        <view class="rule-item">
          <view class="rule-dot"></view>
          <text class="rule-text">同一个微信账号只能绑定一个账号，请确认当前入口角色正确。</text>
        </view>
      </view>

      <view class="field-label">手机号</view>
      <view class="input-item">
        <text class="iconfont">📱</text>
        <input
          class="input"
          v-model="phone"
          type="number"
          maxlength="11"
          placeholder="请输入常用手机号"
        />
      </view>

      <view class="field-label">姓名</view>
      <view class="input-item">
        <text class="iconfont">👤</text>
        <input class="input" v-model="name" maxlength="20" placeholder="请输入真实姓名或常用昵称" />
      </view>

      <view class="field-label">密码</view>
      <view class="input-item">
        <text class="iconfont">🔒</text>
        <input
          class="input"
          v-model="password"
          type="password"
          maxlength="20"
          placeholder="已有账号请输入原密码，新账号请输入 6 位以上密码"
        />
      </view>

      <view class="helper-text">
        绑定成功后，你仍然可以继续使用“手机号 + 密码”登录；微信登录只是多了一个更方便的入口。
      </view>

      <view class="agreement-row">
        <checkbox-group @change="onCheckChange">
          <label class="checkbox-label">
            <checkbox :checked="agreed" color="#007AFF" style="transform:scale(0.7)" />
            <text class="agreement-text">我已阅读并同意<text class="link">《服务协议》</text></text>
          </label>
        </checkbox-group>
      </view>

      <button class="submit-btn" :disabled="loading || !formReady" @click="handleBind">
        {{ loading ? '绑定中...' : '确认绑定并登录' }}
      </button>

      <view class="bottom-note">如果返回登录页重新进入，需要重新点击一次微信登录。</view>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad, onUnload } from '@dcloudio/uni-app'
import { bindWechatPhone } from '@/api/wechat-auth.js'
import { completeLoginSession } from '@/utils/auth-session.js'

const role = ref('user')
const phone = ref('')
const name = ref('')
const password = ref('')
const loading = ref(false)
const wechatBindToken = ref('')
const agreed = ref(false)
const bindCompleted = ref(false)

const phonePattern = /^1\d{10}$/
const roleLabel = computed(() => role.value === 'escort' ? '陪诊师' : '普通用户')

const formReady = computed(() => (
  agreed.value &&
  phonePattern.test(phone.value.trim()) &&
  name.value.trim().length >= 2 &&
  password.value.length >= 6
))

onLoad((options) => {
  role.value = options?.role || uni.getStorageSync('wechatBindRolePending') || 'user'
  wechatBindToken.value = uni.getStorageSync('wechatBindTokenPending') || ''
  if (!['user', 'escort'].includes(role.value)) {
    uni.showToast({ title: '当前角色暂不支持微信绑定', icon: 'none' })
    setTimeout(() => {
      uni.navigateBack({ delta: 1 })
    }, 500)
    return
  }
  if (!wechatBindToken.value) {
    uni.showToast({ title: '微信绑定凭证已失效，请重新发起微信登录', icon: 'none' })
    setTimeout(() => {
      uni.navigateBack({ delta: 1 })
    }, 500)
  }
})

onUnload(() => {
  if (bindCompleted.value) {
    uni.removeStorageSync('wechatBindTokenPending')
    uni.removeStorageSync('wechatBindRolePending')
  }
})

const onCheckChange = (e) => {
  agreed.value = e.detail.value.length > 0
}

const validateForm = () => {
  if (!wechatBindToken.value) {
    uni.showToast({ title: '微信绑定凭证已失效，请重新发起微信登录', icon: 'none' })
    return false
  }
  if (!agreed.value) {
    uni.showToast({ title: '请先同意协议', icon: 'none' })
    return false
  }
  if (!phonePattern.test(phone.value.trim())) {
    uni.showToast({ title: '请输入正确的手机号', icon: 'none' })
    return false
  }
  if (name.value.trim().length < 2) {
    uni.showToast({ title: '姓名至少输入 2 个字', icon: 'none' })
    return false
  }
  if (password.value.length < 6) {
    uni.showToast({ title: '密码至少输入 6 位', icon: 'none' })
    return false
  }
  return true
}

const handleBind = async () => {
  if (!validateForm()) return

  loading.value = true
  uni.showLoading({ title: '绑定中...' })
  try {
    const res = await bindWechatPhone({
      wechatBindToken: wechatBindToken.value,
      phone: phone.value.trim(),
      name: name.value.trim(),
      password: password.value,
      role: role.value
    })
    uni.hideLoading()
    if (res.code !== 200 || !res.data) {
      uni.showToast({ title: res.message || '绑定失败', icon: 'none' })
      return
    }

    const targetUrl = await completeLoginSession({
      role: role.value,
      token: res.data.token,
      userInfo: res.data.userInfo
    })

    bindCompleted.value = true
    uni.removeStorageSync('wechatBindTokenPending')
    uni.removeStorageSync('wechatBindRolePending')
    wechatBindToken.value = ''

    uni.showToast({ title: '绑定成功，正在登录', icon: 'success' })
    setTimeout(() => {
      if (role.value === 'escort') {
        uni.reLaunch({ url: targetUrl })
      } else {
        uni.switchTab({ url: targetUrl })
      }
    }, 450)
  } catch (error) {
    uni.hideLoading()
    uni.showToast({ title: error?.message || '绑定失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.container {
  min-height: 100vh;
  background:
    radial-gradient(circle at top left, rgba(0, 122, 255, 0.14), transparent 36%),
    linear-gradient(180deg, #f2f8ff 0%, #ffffff 100%);
  padding: 36rpx 28rpx 56rpx;
}

.hero-card,
.bind-card {
  background: rgba(255, 255, 255, 0.96);
  border-radius: 28rpx;
  box-shadow: 0 18rpx 42rpx rgba(18, 56, 109, 0.1);
  border: 1rpx solid rgba(0, 122, 255, 0.08);
}

.hero-card {
  padding: 34rpx 30rpx 28rpx;
  margin-bottom: 22rpx;
}

.wechat-badge {
  display: inline-flex;
  align-items: center;
  gap: 10rpx;
  padding: 10rpx 18rpx;
  border-radius: 999rpx;
  background: rgba(7, 193, 96, 0.1);
  margin-bottom: 18rpx;
}

.wechat-icon {
  width: 34rpx;
  height: 34rpx;
}

.wechat-badge-text {
  font-size: 24rpx;
  color: #07a35e;
  font-weight: 600;
}

.title {
  font-size: 36rpx;
  font-weight: 700;
  color: #1f2937;
  margin-bottom: 14rpx;
}

.desc {
  font-size: 26rpx;
  line-height: 1.7;
  color: #5b6473;
}

.bind-card {
  padding: 34rpx 30rpx 36rpx;
}

.section-title {
  font-size: 28rpx;
  font-weight: 700;
  color: #1f2937;
  margin-bottom: 18rpx;
}

.rule-list {
  display: flex;
  flex-direction: column;
  gap: 14rpx;
  margin-bottom: 28rpx;
}

.rule-item {
  display: flex;
  align-items: flex-start;
  gap: 12rpx;
}

.rule-dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
  background: #007aff;
  margin-top: 12rpx;
  flex-shrink: 0;
}

.rule-text {
  font-size: 25rpx;
  color: #5b6473;
  line-height: 1.7;
}

.field-label {
  font-size: 24rpx;
  color: #556070;
  margin-bottom: 10rpx;
  margin-top: 10rpx;
}

.input-item {
  display: flex;
  align-items: center;
  background: #f7f9fc;
  border: 2rpx solid #e6ebf2;
  border-radius: 18rpx;
  padding: 0 24rpx;
  height: 92rpx;
  margin-bottom: 18rpx;
}

.iconfont {
  margin-right: 16rpx;
}

.input {
  flex: 1;
  font-size: 28rpx;
}

.helper-text {
  font-size: 24rpx;
  line-height: 1.7;
  color: #6b7280;
  margin: 6rpx 0 18rpx;
}

.agreement-row {
  margin-bottom: 24rpx;
}

.checkbox-label {
  display: flex;
  align-items: center;
}

.agreement-text {
  font-size: 24rpx;
  color: #5b6473;
}

.link {
  color: #007aff;
}

.submit-btn {
  width: 100%;
  height: 92rpx;
  line-height: 92rpx;
  border-radius: 18rpx;
  background: linear-gradient(135deg, #007aff, #2563eb);
  color: #fff;
  font-size: 30rpx;
  font-weight: 600;
}

.submit-btn[disabled] {
  opacity: 0.6;
}

.bottom-note {
  margin-top: 22rpx;
  text-align: center;
  font-size: 23rpx;
  color: #8b95a5;
  line-height: 1.6;
}
</style>

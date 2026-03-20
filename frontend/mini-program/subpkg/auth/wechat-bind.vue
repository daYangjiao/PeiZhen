<template>
  <view class="container">
    <view class="bind-card">
      <view class="title">完成手机号绑定</view>
      <view class="desc">首次使用微信登录时，需要绑定手机号并补充姓名，后续可直接一键登录。</view>

      <view class="input-group">
        <view class="input-item">
          <text class="iconfont">📱</text>
          <input class="input" v-model="phone" type="number" maxlength="11" placeholder="请输入手机号" />
        </view>
        <view class="input-item">
          <text class="iconfont">👤</text>
          <input class="input" v-model="name" placeholder="请输入姓名或昵称" />
        </view>
        <view class="input-item">
          <text class="iconfont">🔒</text>
          <input class="input" v-model="password" type="password" placeholder="请输入密码" />
        </view>
      </view>

      <button class="submit-btn" :disabled="loading" @click="handleBind">
        {{ loading ? '绑定中...' : '确认绑定并登录' }}
      </button>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad, onUnload } from '@dcloudio/uni-app'
import { bindWechatPhone } from '@/api/wechat-auth.js'
import { completeLoginSession } from '@/utils/auth-session.js'

const role = ref('user')
const phone = ref('')
const name = ref('')
const password = ref('')
const loading = ref(false)
const wechatBindToken = ref('')

onLoad((options) => {
  role.value = options?.role || uni.getStorageSync('wechatBindRolePending') || 'user'
  wechatBindToken.value = uni.getStorageSync('wechatBindTokenPending') || ''
  if (!wechatBindToken.value) {
    uni.showToast({ title: '微信绑定凭证已失效', icon: 'none' })
    setTimeout(() => {
      uni.navigateBack({ delta: 1 })
    }, 500)
  }
})

onUnload(() => {
  if (!wechatBindToken.value) {
    uni.removeStorageSync('wechatBindTokenPending')
    uni.removeStorageSync('wechatBindRolePending')
  }
})

const handleBind = async () => {
  if (!wechatBindToken.value) {
    uni.showToast({ title: '微信绑定凭证已失效', icon: 'none' })
    return
  }
  if (!phone.value || !name.value || !password.value) {
    uni.showToast({ title: '请先填写完整信息', icon: 'none' })
    return
  }
  loading.value = true
  uni.showLoading({ title: '绑定中...' })
  try {
    const res = await bindWechatPhone({
      wechatBindToken: wechatBindToken.value,
      phone: phone.value,
      password: password.value,
      name: name.value,
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
    uni.removeStorageSync('wechatBindTokenPending')
    uni.removeStorageSync('wechatBindRolePending')
    wechatBindToken.value = ''
    uni.showToast({ title: '登录成功', icon: 'success' })
    setTimeout(() => {
      uni.switchTab({ url: targetUrl })
    }, 400)
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
  background: linear-gradient(180deg, #f3f7fb 0%, #ffffff 100%);
  padding: 80rpx 28rpx;
}

.bind-card {
  background: #fff;
  border-radius: 28rpx;
  padding: 40rpx 30rpx 36rpx;
  box-shadow: 0 20rpx 48rpx rgba(31, 41, 55, 0.08);
}

.title {
  font-size: 36rpx;
  font-weight: 700;
  color: #1f2937;
  margin-bottom: 12rpx;
}

.desc {
  font-size: 26rpx;
  line-height: 1.7;
  color: #6b7280;
  margin-bottom: 28rpx;
}

.input-group {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.input-item {
  display: flex;
  align-items: center;
  background: #f7f9fc;
  border: 2rpx solid #e6ebf2;
  border-radius: 18rpx;
  padding: 0 24rpx;
  height: 92rpx;
}

.iconfont {
  margin-right: 16rpx;
}

.input {
  flex: 1;
  font-size: 28rpx;
}

.submit-btn {
  margin-top: 32rpx;
  width: 100%;
  height: 92rpx;
  line-height: 92rpx;
  border-radius: 18rpx;
  background: linear-gradient(135deg, #4a90e2, #357abd);
  color: #fff;
  font-size: 30rpx;
  font-weight: 600;
}
</style>

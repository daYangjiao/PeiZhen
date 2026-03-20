<template>
  <view class="container">
    <!-- 顶部装饰（参照陪诊师登录页） -->
    <view class="header-section">
      <view class="circle-1"></view>
      <view class="circle-2"></view>
      <image class="logo" src="/static/mynewlogo.png" mode="aspectFit"></image>
      <text class="welcome-text">{{ currentRole === 'user' ? '欢迎回来，用户' : '欢迎回来，陪诊师' }}</text>
    </view>

    <!-- 登录卡片 -->
    <view class="login-card">
      <view class="tab-header">
        <view
          class="tab-item"
          :class="{ active: currentRole === 'user' }"
          @click="currentRole = 'user'"
        >
          <text>用户端</text>
        </view>
        <view
          class="tab-item"
          :class="{ active: currentRole === 'escort' }"
          @click="currentRole = 'escort'"
        >
          <text>陪诊师端</text>
        </view>
      </view>

      <button class="wechat-login-btn" :disabled="wechatLoading" @click="handleWechatLogin">
        {{ wechatLoading ? '处理中...' : '微信登录' }}
      </button>

      <view class="wechat-tip">{{ wechatTip }}</view>

      <view class="input-group">
        <view class="input-item">
          <text class="iconfont">👤</text>
          <input class="input" v-model="phone" type="number" maxlength="11" placeholder="请输入手机号" />
        </view>
        <view class="input-item">
          <text class="iconfont">🔒</text>
          <input class="input" v-model="password" type="password" placeholder="请输入密码" />
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

      <button class="login-btn" :disabled="loading" @click="handleLogin">
        {{ loading ? '登录中...' : '立即登录' }}
      </button>

      <view class="footer-links">
        <text v-if="currentRole === 'user'" @click="goUserRegister">立即注册</text>
        <text v-if="currentRole === 'user'" class="divider">|</text>
        <text v-if="currentRole === 'escort'" @click="goEscortRegister">陪诊师入驻</text>
        <text v-if="currentRole === 'escort'" class="divider">|</text>
        <text>找回密码</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad, onUnload } from '@dcloudio/uni-app'
import { post } from '@/utils/api.js'
import { getWechatConfigStatus, loginByWechat } from '@/api/wechat-auth.js'
import { completeLoginSession } from '@/utils/auth-session.js'
import { useSessionStore } from '@/stores/session'

const currentRole = ref('user')
const session = useSessionStore()
const phone = ref('')
const password = ref('')
const agreed = ref(false)
const loading = ref(false)
const wechatLoading = ref(false)
const wechatEnabled = ref(false)
const wechatStatusReason = ref('微信登录暂未开通')
const fromGuard = ref(false)
const wechatTip = ref('当前支持手机号密码登录，微信登录开通后这里会直接一键进入')

onLoad((options) => {
  if (options?.role === 'user' || options?.role === 'escort') {
    currentRole.value = options.role
  }
  if (options?.from === 'guard') {
    fromGuard.value = true
  }
  loadWechatConfigStatus()
})

onUnload(() => {
  // 仅针对“被守卫/401 自动跳转过来且仍未登录就返回”的情况，主动送回首页，避免回到受限页死循环
  if (!fromGuard.value) return
  if (session.isLoggedIn && session.token) return
  if (currentRole.value === 'escort') {
    uni.reLaunch({ url: '/pages/role-escort/hall' })
  } else {
    uni.switchTab({ url: '/pages/role-user/home' })
  }
})

const onCheckChange = (e) => {
  agreed.value = e.detail.value.length > 0
}

const goEscortRegister = () => {
  uni.navigateTo({ url: '/subpkg/auth/escort-register' })
}

const goUserRegister = () => {
  uni.navigateTo({ url: '/subpkg/auth/user-register' })
}

const handleLogin = async () => {
  if (!agreed.value) {
    uni.showToast({ title: '请先同意协议', icon: 'none' })
    return
  }
  if (!phone.value || !password.value) {
    uni.showToast({ title: '请输入账号密码', icon: 'none' })
    return
  }

  loading.value = true
  uni.showLoading({ title: '登录中...' })
  try {
    const res = await post('/api/users/login', {
      phone: phone.value,
      password: password.value
    })
    uni.hideLoading()

    if (res.code === 200 && res.data) {
      const { token, userInfo } = res.data
      const targetUrl = await completeLoginSession({ role: currentRole.value, token, userInfo })
      uni.showToast({ title: '登录成功', icon: 'success' })
      setTimeout(() => {
        if (currentRole.value === 'escort') {
          uni.reLaunch({ url: targetUrl })
        } else {
          uni.switchTab({ url: targetUrl })
        }
      }, 400)
    } else {
      uni.showToast({ title: res.message || '登录失败', icon: 'none' })
    }
  } catch (e) {
    uni.hideLoading()
    uni.showToast({ title: e.message || '登录失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

const loadWechatConfigStatus = async () => {
  try {
    const res = await getWechatConfigStatus()
    wechatEnabled.value = !!res?.data?.enabled
    wechatStatusReason.value = res?.data?.reason || '微信登录暂未开通'
    wechatTip.value = wechatEnabled.value
      ? '微信登录已可用，点击上方按钮即可一键进入'
      : `${wechatStatusReason.value}，当前可继续使用手机号密码登录`
  } catch {
    wechatEnabled.value = false
    wechatStatusReason.value = '微信登录暂未开通'
    wechatTip.value = '当前支持手机号密码登录，微信登录开通后这里会直接一键进入'
  }
}

const showWechatUnavailable = (message = wechatStatusReason.value || '微信登录暂未开通') => {
  uni.showToast({ title: message, icon: 'none' })
}

const loginWithUniWechat = () => new Promise((resolve, reject) => {
  uni.login({
    provider: 'weixin',
    success: resolve,
    fail: reject
  })
})

const handleWechatLogin = async () => {
  if (!agreed.value) {
    uni.showToast({ title: '请先同意协议', icon: 'none' })
    return
  }
  if (currentRole.value !== 'user') {
    showWechatUnavailable('陪诊师微信登录暂未开通')
    return
  }
  // #ifndef MP-WEIXIN
  showWechatUnavailable()
  return
  // #endif
  if (!wechatEnabled.value) {
    showWechatUnavailable()
    return
  }
  wechatLoading.value = true
  uni.showLoading({ title: '登录中...' })
  try {
    const loginRes = await loginWithUniWechat()
    const res = await loginByWechat(loginRes.code, currentRole.value)
    uni.hideLoading()
    if (res.code !== 200 || !res.data) {
      showWechatUnavailable(res.message || '微信登录失败')
      return
    }
    if (res.data.bindStatus === 'UNBOUND') {
      uni.setStorageSync('wechatBindTokenPending', res.data.wechatBindToken)
      uni.setStorageSync('wechatBindRolePending', currentRole.value)
      uni.navigateTo({ url: `/subpkg/auth/wechat-bind?role=${currentRole.value}` })
      return
    }
    const targetUrl = await completeLoginSession({
      role: currentRole.value,
      token: res.data.token,
      userInfo: res.data.userInfo
    })
    uni.showToast({ title: '登录成功', icon: 'success' })
    setTimeout(() => {
      uni.switchTab({ url: targetUrl })
    }, 400)
  } catch (error) {
    uni.hideLoading()
    showWechatUnavailable(error?.message || '微信登录失败')
  } finally {
    wechatLoading.value = false
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

.back-btn-wrap {
  position: absolute;
  top: 0;
  left: 0;
  z-index: 20;
  padding: 48rpx 24rpx 0;
}
.back-btn {
  font-size: 32rpx;
  color: rgba(255, 255, 255, 0.95);
  padding: 16rpx 24rpx;
  display: inline-block;
}
.header-section {
  height: 300px;
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
    background: rgba(255, 255, 255, 0.1);
    border-radius: 50%;
    top: -50px;
    right: -50px;
  }

  .circle-2 {
    position: absolute;
    width: 120px;
    height: 120px;
    background: rgba(255, 255, 255, 0.08);
    border-radius: 50%;
    bottom: 20px;
    left: -30px;
  }

  .logo {
    width: 80px;
    height: 80px;
    background: #fff;
    border-radius: 20px;
    margin-bottom: 16px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  }

  .welcome-text {
    color: #fff;
    font-size: 20px;
    font-weight: 500;
  }
}

.login-card {
  margin: -60px 24px 0;
  background: #fff;
  border-radius: 24px;
  padding: 30px 24px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  position: relative;
  z-index: 10;

  .tab-header {
    display: flex;
    margin-bottom: 30px;
    background: #f5f7fa;
    border-radius: 12px;
    padding: 4px;

    .tab-item {
      flex: 1;
      text-align: center;
      padding: 12px 0;
      font-size: 16px;
      color: #666;
      border-radius: 10px;
      transition: all 0.2s;

      &.active {
        background: linear-gradient(135deg, #4A90E2, #357ABD);
        color: #fff;
        font-weight: 600;
      }
    }
  }

  .input-item {
    display: flex;
    align-items: center;
    height: 54px;
    background: #f8f9fa;
    border-radius: 12px;
    margin-bottom: 16px;
    padding: 0 16px;

    .iconfont {
      font-size: 20px;
      color: #999;
      margin-right: 12px;
    }

    .input {
      flex: 1;
      font-size: 16px;
    }
  }
}

.wechat-login-btn {
  width: 100%;
  height: 92rpx;
  line-height: 92rpx;
  border-radius: 16rpx;
  background: linear-gradient(135deg, #29c261, #17a34a);
  color: #fff;
  font-size: 30rpx;
  font-weight: 600;
  margin: 20rpx 0 12rpx;
  box-shadow: 0 10rpx 24rpx rgba(41, 194, 97, 0.24);
}

.wechat-tip {
  text-align: center;
  color: #7b8794;
  font-size: 24rpx;
  margin-bottom: 18rpx;
}

.agreement-row {
  margin: 20px 0;

  .agreement-text {
    font-size: 13px;
    color: #999;

    .link {
      color: #4A90E2;
    }
  }
}

.login-btn {
  background: linear-gradient(135deg, #4A90E2, #357ABD);
  color: #fff;
  height: 50px;
  border-radius: 25px;
  font-size: 17px;
  font-weight: bold;
  box-shadow: 0 4px 12px rgba(74, 144, 226, 0.3);
}

.login-btn[disabled] {
  opacity: 0.7;
}

.footer-links {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 24px;
  font-size: 14px;
  color: #666;

  .divider {
    margin: 0 15px;
    color: #eee;
  }
}
</style>

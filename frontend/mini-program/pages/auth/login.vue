<template>
  <view class="container">
    <!-- 顶部装饰（参照陪诊师登录页） -->
    <view class="header-section">
      <view class="circle-1"></view>
      <view class="circle-2"></view>
      <image class="logo" :src="brandLogo" mode="aspectFit"></image>
      <text class="welcome-text">{{ currentRole === 'user' ? '欢迎回来，用户' : '欢迎回来，陪诊师' }}</text>
    </view>

    <!-- 登录卡片 -->
    <view class="login-card">
      <view v-if="!publicSafeMode" class="tab-header">
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

      <button v-if="showWechatEntry" class="wechat-login-btn" :disabled="wechatLoading" @click="handleWechatLogin">
        {{ wechatLoading ? '处理中...' : '微信登录' }}
      </button>

      <view v-if="showWechatEntry" class="wechat-tip">{{ wechatTip }}</view>
      <view v-else-if="!publicSafeMode" class="wechat-tip">微信登录仅支持微信小程序，当前可继续使用手机号密码登录</view>

      <view v-if="!publicSafeMode" class="input-group">
        <view class="input-item">
          <text class="iconfont">👤</text>
          <input class="input" v-model="phone" type="number" maxlength="11" placeholder="请输入手机号" />
        </view>
        <view class="input-item">
          <text class="iconfont">🔒</text>
          <input class="input" v-model="password" type="password" placeholder="请输入密码" />
        </view>
      </view>

      <view v-if="!publicSafeMode" class="agreement-row">
        <checkbox-group @change="onCheckChange">
          <label class="checkbox-label">
            <checkbox :checked="agreed" color="#007AFF" style="transform:scale(0.7)" />
            <text class="agreement-text">我已阅读并同意<text class="link">《服务协议》</text></text>
          </label>
        </checkbox-group>
      </view>

      <button v-if="!publicSafeMode" class="login-btn" :disabled="loading" @click="handleLogin">
        {{ loading ? '登录中...' : '立即登录' }}
      </button>

      <view v-if="publicSafeMode" class="public-safe-card">
        <text class="public-safe-title">服务与流程介绍</text>
        <text class="public-safe-desc">当前网站主要提供服务介绍、就医流程参考与健康管理信息展示。</text>
        <text class="public-safe-tip">如需了解更多内容，请持续关注后续更新。</text>
      </view>

      <view v-if="!publicSafeMode" class="footer-links">
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
import { computed, ref, watch } from 'vue'
import { onLoad, onUnload } from '@dcloudio/uni-app'
import { isWeixinMiniProgramRuntime, post } from '@/utils/api.js'
import { getWechatConfigStatus, loginByWechat } from '@/api/wechat-auth.js'
import { completeLoginSession, getSessionLandingUrl } from '@/utils/auth-session.js'
import { useSessionStore } from '@/stores/session'
import { brandLogo } from '@/utils/assets.js'
import { PUBLIC_SAFE_LANDING_URL, PUBLIC_SAFE_NOTICE, isPublicSafeMode, showPublicSafeNotice } from '@/utils/site-mode.js'

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
const publicSafeMode = computed(() => isPublicSafeMode())
const showWechatEntry = computed(() => !publicSafeMode.value && isWeixinMiniProgramRuntime())

onLoad((options) => {
  if (!publicSafeMode.value && (options?.role === 'user' || options?.role === 'escort')) {
    currentRole.value = options.role
  }
  if (publicSafeMode.value) currentRole.value = 'user'
  if (options?.from === 'guard') {
    fromGuard.value = true
  }
  if (publicSafeMode.value) {
    uni.reLaunch({ url: PUBLIC_SAFE_LANDING_URL })
    return
  }
  session.restoreFromStorage()
  if (session.isLoggedIn && session.token) {
    const targetUrl = getSessionLandingUrl({ role: session.role, userInfo: session.userInfo })
    currentRole.value = session.role || currentRole.value
    if (session.role === 'escort') {
      uni.reLaunch({ url: targetUrl })
    } else {
      uni.switchTab({ url: targetUrl })
    }
    return
  }
  if (showWechatEntry.value) {
    loadWechatConfigStatus()
  }
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
  if (publicSafeMode.value) return showPublicSafeNotice()
  uni.navigateTo({ url: '/subpkg/auth/escort-register' })
}

const goUserRegister = () => {
  if (publicSafeMode.value) return showPublicSafeNotice()
  uni.navigateTo({ url: '/subpkg/auth/user-register' })
}

const handleLogin = async () => {
  if (publicSafeMode.value) {
    showPublicSafeNotice()
    return
  }
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
    const res = await getWechatConfigStatus(currentRole.value)
    wechatEnabled.value = !!res?.data?.enabled
    wechatStatusReason.value = res?.data?.reason || '微信登录暂未开通'
    wechatTip.value = wechatEnabled.value
      ? `微信登录已可用，点击上方按钮即可一键进入${currentRole.value === 'escort' ? '陪诊师端' : ''}`
      : `${wechatStatusReason.value}，当前可继续使用手机号密码登录`
  } catch {
    wechatEnabled.value = false
    wechatStatusReason.value = '微信登录暂未开通'
    wechatTip.value = '当前支持手机号密码登录，微信登录开通后这里会直接一键进入'
  }
}

watch(currentRole, () => {
  if (showWechatEntry.value) {
    loadWechatConfigStatus()
  }
})

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
  if (publicSafeMode.value) {
    showWechatUnavailable(PUBLIC_SAFE_NOTICE)
    return
  }
  if (!agreed.value) {
    uni.showToast({ title: '请先同意协议', icon: 'none' })
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
      if (currentRole.value === 'escort') {
        uni.reLaunch({ url: targetUrl })
      } else {
        uni.switchTab({ url: targetUrl })
      }
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
  display: flex;
  flex-direction: column;
  padding-bottom: 24px;
  background:
    radial-gradient(circle at top left, rgba(0, 122, 255, 0.12), transparent 34%),
    linear-gradient(180deg, #f2f8ff 0%, #f7fbff 240rpx, #ffffff 100%);
  position: relative;
  overflow-x: hidden;
  overflow-y: auto;
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
  min-height: 176px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
  padding: 30px 20px 22px;
  position: relative;
  text-align: center;

  .circle-1 {
    position: absolute;
    width: 170px;
    height: 170px;
    background: rgba(0, 122, 255, 0.08);
    border-radius: 50%;
    top: -32px;
    right: -42px;
  }

  .circle-2 {
    position: absolute;
    width: 116px;
    height: 116px;
    background: rgba(37, 99, 235, 0.06);
    border-radius: 50%;
    bottom: 16px;
    left: -16px;
  }

  .logo {
    width: 78px;
    height: 78px;
    margin-bottom: 12px;
  }

  .welcome-text {
    color: #16324f;
    font-size: 18px;
    font-weight: 700;
    letter-spacing: 0.2px;
  }
}

.login-card {
  margin: 0 18px 18px;
  background: #fff;
  border-radius: 20px;
  padding: 18px 18px 20px;
  box-shadow: 0 18px 42px rgba(18, 56, 109, 0.1);
  position: relative;
  z-index: 10;
  border: 1px solid rgba(0, 122, 255, 0.08);

  .tab-header {
    display: flex;
    margin-bottom: 14px;
    background: #eef5ff;
    border-radius: 14px;
    padding: 5px;

    .tab-item {
      flex: 1;
      text-align: center;
      padding: 10px 0;
      font-size: 14px;
      color: #6a7f94;
      border-radius: 12px;
      transition: all 0.2s;

      &.active {
        background: linear-gradient(135deg, #007aff, #2563eb);
        color: #fff;
        font-weight: 600;
        box-shadow: 0 8px 18px rgba(0, 122, 255, 0.2);
      }
    }
  }

  .input-item {
    display: flex;
    align-items: center;
    height: 46px;
    background: #f8fbff;
    border-radius: 14px;
    margin-bottom: 12px;
    padding: 0 14px;
    border: 1px solid #dce8f8;

    .iconfont {
      font-size: 15px;
      color: #7d8ea2;
      margin-right: 8px;
    }

    .input {
      flex: 1;
      font-size: 13px;
      color: #16324f;
    }
  }
}

.wechat-login-btn {
  width: 100%;
  height: 46px;
  line-height: 46px;
  border-radius: 16px;
  background: linear-gradient(135deg, #29c261, #17a34a);
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  margin: 0 0 10px;
  box-shadow: 0 8rpx 18rpx rgba(41, 194, 97, 0.22);
}

.wechat-tip {
  text-align: center;
  color: #6a7f94;
  font-size: 12px;
  margin-bottom: 12px;
}

.agreement-row {
  margin: 10px 0 12px;

  .agreement-text {
    font-size: 12px;
    color: #7d8ea2;

    .link {
      color: #007aff;
    }
  }
}

.login-btn {
  background: linear-gradient(135deg, #007aff, #2563eb);
  color: #fff;
  height: 46px;
  border-radius: 23px;
  font-size: 15px;
  font-weight: bold;
  margin-top: 6px;
  box-shadow: 0 10px 24px rgba(0, 122, 255, 0.24);
}

.login-btn[disabled] {
  opacity: 0.7;
}

.public-safe-card {
  margin-top: 14px;
  padding: 14px 12px;
  border-radius: 16px;
  background: #f7fbff;
  border: 1px solid #dce8f8;
}

.public-safe-title {
  display: block;
  font-size: 15px;
  font-weight: 700;
  color: #16324f;
  margin-bottom: 6px;
}

.public-safe-desc,
.public-safe-tip {
  display: block;
  font-size: 12px;
  line-height: 1.6;
  color: #6a7f94;
}

.public-safe-tip {
  margin-top: 18px;
}

.footer-links {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 18px;
  font-size: 13px;
  color: #6a7f94;

  .divider {
    margin: 0 15px;
    color: #d5e2f1;
  }
}
</style>

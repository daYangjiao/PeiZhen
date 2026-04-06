<template>
  <view class="page">
    <view class="form-card">
      <view class="avatar-row">
        <image class="avatar" :src="displayAvatarUrl" mode="aspectFill"></image>
        <view class="avatar-btn" @click.stop="chooseAvatar">
          <text>{{ uploading ? '上传中...' : '更换头像' }}</text>
        </view>
      </view>

      <view class="field-row">
        <text class="label">姓名</text>
        <input class="input" v-model="form.name" placeholder="请输入姓名" />
      </view>

      <view class="field-row">
        <text class="label">手机号</text>
        <input class="input" type="number" maxlength="11" v-model="form.phone" placeholder="请输入手机号" />
      </view>

      <view class="field-row">
        <text class="label">证书编号</text>
        <input class="input" v-model="form.certificate" placeholder="请输入证书编号" />
      </view>

      <view class="field-row">
        <text class="label">擅长领域</text>
        <input class="input" v-model="form.professionalField" placeholder="如：全科陪诊,术后护理" />
      </view>

      <view class="field-row">
        <text class="label">从业年限</text>
        <input class="input" type="number" v-model="form.experienceYears" placeholder="请输入年限" />
      </view>

      <view class="field-row">
        <text class="label">常驻医院</text>
        <input class="input" v-model="form.hospitalName" placeholder="请输入常驻医院" />
      </view>

      <view class="field-row column">
        <text class="label">个人简介</text>
        <textarea class="textarea" v-model="form.introduction" placeholder="请输入个人简介" />
      </view>
    </view>

    <view class="save-btn" @click="saveProfile">
      <text>{{ saving ? '保存中...' : '保存资料' }}</text>
    </view>
  </view>
</template>

<script setup>
import { reactive, ref, onMounted, computed } from 'vue'
import { useUserStore } from '@/stores/user'
import { get, put } from '@/utils/api.js'
import { uploadPublicAvatarImage } from '@/api/user.js'
import { userPlaceholder } from '@/utils/assets.js'
import { pickCropAndUploadAvatar } from '@/utils/avatar-upload.js'
import { resolveAvatarUrl } from '@/utils/media.js'

const userStore = useUserStore()
const saving = ref(false)
const uploading = ref(false)
const localAvatarPreview = ref('')

const form = reactive({
  id: null,
  name: '',
  phone: '',
  avatar: '',
  avatarUrl: '',
  introduction: '',
  professionalField: '',
  experienceYears: '',
  hospitalName: '',
  certificate: ''
})

const displayAvatarUrl = computed(() =>
  resolveAvatarUrl(localAvatarPreview.value || form.avatarUrl || form.avatar, userPlaceholder)
)

const fillForm = (source = {}) => {
  form.id = source.id || null
  form.name = source.name || ''
  form.phone = source.phone || ''
  form.avatar = source.avatar || ''
  form.avatarUrl = source.avatarUrl || source.avatar || ''
  form.introduction = source.introduction || ''
  form.professionalField = source.professionalField || ''
  form.experienceYears = source.experienceYears ?? ''
  form.hospitalName = source.hospitalName || ''
  form.certificate = source.certificate || ''
}

const loadProfile = async () => {
  const userInfo = uni.getStorageSync('userInfo')
  if (!userInfo || !userInfo.id) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    setTimeout(() => uni.reLaunch({ url: '/pages/auth/login?role=escort' }), 1000)
    return
  }

  const cached = userStore.restoreAttendantInfo() ? userStore.attendantInfo : userInfo
  fillForm(cached)

  try {
    const res = await get(`/attendant/profile/${userInfo.id}`)
    if (res.code === 200 && res.data) {
      userStore.setAttendantInfo(res.data)
      fillForm(res.data)
    }
  } catch (error) {
    console.error('加载资料失败:', error)
  }
}

const chooseAvatar = async () => {
  if (uploading.value) return
  uploading.value = true
  try {
    const { avatarUrl, localFilePath } = await pickCropAndUploadAvatar(uploadPublicAvatarImage)
    if (avatarUrl) {
      localAvatarPreview.value = localFilePath || ''
      form.avatarUrl = avatarUrl
      form.avatar = avatarUrl
      uni.showToast({ title: '头像上传成功', icon: 'success' })
    } else {
      uni.showToast({ title: '头像上传失败', icon: 'none' })
    }
  } catch (error) {
    if (!/cancel/i.test(error?.message || error?.errMsg || '')) {
      console.error('上传头像失败:', error)
      uni.showToast({ title: '头像上传失败', icon: 'none' })
    }
  } finally {
    uploading.value = false
  }
}

const saveProfile = async () => {
  if (saving.value) return

  if (!form.name) {
    uni.showToast({ title: '请输入姓名', icon: 'none' })
    return
  }

  if (form.phone && form.phone.length !== 11) {
    uni.showToast({ title: '手机号格式有误', icon: 'none' })
    return
  }

  const userInfo = uni.getStorageSync('userInfo')
  if (!userInfo || !userInfo.id) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    return
  }

  saving.value = true
  try {
    const payload = {
      name: form.name,
      phone: form.phone,
      avatarUrl: form.avatarUrl,
      introduction: form.introduction,
      professionalField: form.professionalField,
      experienceYears: form.experienceYears === '' ? null : Number(form.experienceYears),
      hospitalName: form.hospitalName,
      certificate: form.certificate
    }

    const res = await put(`/attendant/profile/${userInfo.id}`, payload)
    if (res.code === 200) {
      await userStore.fetchAttendantProfile(userInfo.id)
      const latest = userStore.attendantInfo
      localAvatarPreview.value = ''
      uni.setStorageSync('userInfo', {
        ...userInfo,
        name: latest.name || userInfo.name,
        phone: latest.phone || userInfo.phone,
        avatar: latest.avatarUrl || latest.avatar || userInfo.avatar
      })

      uni.showToast({ title: '保存成功', icon: 'success' })
      setTimeout(() => {
        uni.navigateBack()
      }, 500)
    }
  } catch (error) {
    console.error('保存资料失败:', error)
    uni.showToast({ title: '保存失败', icon: 'none' })
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadProfile()
})
</script>

<style lang="scss" scoped>
@import '@/styles/escort-ui.scss';
.page {
  @include escort-page;
  min-height: 100vh;
  padding: 24rpx;
  box-sizing: border-box;
}

.form-card {
  @include escort-card(28rpx);
}

.avatar-row {
  display: flex;
  align-items: center;
  margin-bottom: 28rpx;
}

.avatar {
  width: 128rpx;
  height: 128rpx;
  border-radius: 34rpx;
  border: 4rpx solid rgba(0, 122, 255, 0.1);
  background: linear-gradient(180deg, #f8fbff 0%, #eef5ff 100%);
  box-shadow: 0 16rpx 34rpx rgba(0, 122, 255, 0.12);
}

.avatar-btn {
  margin-left: 20rpx;
  height: 68rpx;
  padding: 0 26rpx;
  border-radius: 999rpx;
  background: rgba(0, 122, 255, 0.08);
  display: flex;
  align-items: center;
  justify-content: center;

  text {
    color: #007AFF;
    font-size: 24rpx;
    font-weight: 600;
  }
}

.field-row {
  margin-bottom: 20rpx;
}

.column {
  display: flex;
  flex-direction: column;
}

.label {
  display: block;
  margin-bottom: 10rpx;
  font-size: 24rpx;
  color: #6b7280;
}

.input {
  height: 82rpx;
  padding: 0 20rpx;
  background: #f8fafc;
  border-radius: 12rpx;
  font-size: 28rpx;
  color: #1f2937;
}

.textarea {
  width: 100%;
  min-height: 190rpx;
  padding: 16rpx 20rpx;
  background: #f8fafc;
  border-radius: 12rpx;
  font-size: 28rpx;
  color: #1f2937;
  box-sizing: border-box;
}

.save-btn {
  margin-top: 24rpx;
  @include escort-primary-btn;
  border-radius: $escort-radius-card;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: $escort-shadow-primary;

  text {
    color: #ffffff;
    font-size: 30rpx;
    font-weight: 600;
  }
}
</style>

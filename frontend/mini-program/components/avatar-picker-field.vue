<template>
  <view class="avatar-panel">
    <view class="avatar-panel-head">
      <view>
        <text class="avatar-panel-title">{{ title }}</text>
        <text class="avatar-panel-tip">{{ tip }}</text>
      </view>
    </view>

    <view class="avatar-shell">
      <view class="avatar-preview-card">
        <image class="avatar-preview" :src="previewUrl" mode="aspectFill"></image>
        <view class="avatar-copy">
          <text class="avatar-copy-title">{{ previewTitle }}</text>
          <text class="avatar-copy-desc">{{ previewDesc }}</text>
        </view>
      </view>

      <button class="avatar-upload-btn" :disabled="uploading" @click="handleChooseAvatar">
        {{ uploading ? '处理中...' : uploadLabel }}
      </button>
    </view>

    <view class="avatar-grid">
      <view
        v-for="item in normalizedOptions"
        :key="item.value"
        class="avatar-option"
        :class="{ active: modelValue === item.value }"
        @click="selectDefaultAvatar(item.value)"
      >
        <image class="avatar-option-image" :src="item.previewUrl" mode="aspectFit"></image>
        <text class="avatar-option-label">{{ item.label }}</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { pickCropAndUploadAvatar } from '@/utils/avatar-upload.js'
import { uploadPublicAvatarImage } from '@/api/user.js'
import { resolveAvatarUrl } from '@/utils/media.js'

const props = defineProps({
  modelValue: {
    type: String,
    default: '',
  },
  options: {
    type: Array,
    default: () => [],
  },
  title: {
    type: String,
    default: '头像设置',
  },
  tip: {
    type: String,
    default: '可上传头像，也可直接选择系统头像。',
  },
  previewTitle: {
    type: String,
    default: '当前头像',
  },
  previewDesc: {
    type: String,
    default: '注册后可在个人资料中继续更换。',
  },
  uploadLabel: {
    type: String,
    default: '上传头像（可选）',
  },
})

const emit = defineEmits(['update:modelValue', 'uploaded'])
const uploading = ref(false)
const localPreviewPath = ref('')

const previewUrl = computed(() => resolveAvatarUrl(localPreviewPath.value || props.modelValue, ''))
const normalizedOptions = computed(() =>
  (props.options || []).map((item) => ({
    ...item,
    previewUrl: resolveAvatarUrl(item.value, '')
  }))
)

watch(
  () => props.modelValue,
  (value) => {
    if (value && value === localPreviewPath.value) return
    if (value && !/^(data:|blob:|wxfile:|file:|content:\/\/|\/var\/|\/private\/|\/storage\/|\/data\/|\/sdcard\/|\/Users\/|\/Volumes\/|_doc\/|_downloads\/)/i.test(value)) {
      localPreviewPath.value = ''
    }
  }
)

const selectDefaultAvatar = (value) => {
  localPreviewPath.value = ''
  emit('update:modelValue', value)
}

const handleChooseAvatar = async () => {
  if (uploading.value) return
  uploading.value = true
  try {
    const { avatarUrl, localFilePath } = await pickCropAndUploadAvatar(uploadPublicAvatarImage)
    localPreviewPath.value = localFilePath || ''
    emit('update:modelValue', avatarUrl)
    emit('uploaded', avatarUrl)
    uni.showToast({ title: '头像已更新', icon: 'success' })
  } catch (error) {
    const message = error?.message || error?.errMsg || ''
    if (!/cancel/i.test(message)) {
      uni.showToast({ title: '头像上传失败', icon: 'none' })
    }
  } finally {
    uploading.value = false
  }
}
</script>

<style scoped lang="scss">
.avatar-panel {
  border-radius: 22px;
  padding: 16px;
  background: linear-gradient(180deg, rgba(247, 251, 255, 0.98) 0%, rgba(239, 246, 255, 0.98) 100%);
  border: 1px solid rgba(0, 122, 255, 0.08);
  margin-bottom: 16px;
}

.avatar-panel-head {
  margin-bottom: 14px;
}

.avatar-panel-title {
  display: block;
  font-size: 15px;
  line-height: 1.2;
  font-weight: 700;
  color: #16324f;
  margin-bottom: 4px;
}

.avatar-panel-tip {
  display: block;
  font-size: 12px;
  line-height: 1.6;
  color: #6a7f94;
}

.avatar-shell {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 12px;
  margin-bottom: 14px;
}

.avatar-preview-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.88);
}

.avatar-preview {
  width: 76px;
  height: 76px;
  flex-shrink: 0;
  border-radius: 24px;
  border: 3px solid rgba(0, 122, 255, 0.12);
  background: #eef5ff;
}

.avatar-copy-title {
  display: block;
  font-size: 14px;
  font-weight: 700;
  color: #16324f;
  margin-bottom: 4px;
}

.avatar-copy-desc {
  display: block;
  font-size: 12px;
  line-height: 1.6;
  color: #6a7f94;
}

.avatar-upload-btn {
  height: 44px;
  border-radius: 999px;
  background: linear-gradient(135deg, #007aff, #2563eb);
  color: #ffffff;
  font-size: 13px;
  font-weight: 700;
  box-shadow: 0 10px 24px rgba(0, 122, 255, 0.2);
}

.avatar-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.avatar-option {
  padding: 8px 6px 10px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.9);
  border: 1.5px solid transparent;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.avatar-option.active {
  border-color: #007aff;
  box-shadow: 0 10px 20px rgba(0, 122, 255, 0.14);
}

.avatar-option-image {
  width: 52px;
  height: 52px;
  border-radius: 16px;
}

.avatar-option-label {
  font-size: 11px;
  color: #5d738b;
  text-align: center;
}
</style>

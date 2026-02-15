# 微信手机号登录功能使用说明

## 概述

本文档介绍了PzUni用户端微信手机号登录功能的实现和使用方法。该功能允许用户通过微信授权获取手机号进行一键登录，提升用户体验。

## 功能特点

- **一键登录**：用户无需手动输入手机号，通过微信授权即可完成登录
- **安全可靠**：使用微信官方加密数据传输，保证用户隐私安全
- **快速便捷**：减少用户操作步骤，提高登录效率
- **兼容性好**：支持微信小程序和App端

## 技术实现

### 1. 数据库设计

用户表(`user`)包含以下关键字段：
- `openid`: 微信用户唯一标识
- `phone`: 用户手机号
- `wechat_nickname`: 微信昵称
- `wechat_avatar`: 微信头像

### 2. 实现步骤

#### 前端实现

**页面组件**
集成到现有登录页面 `subpkg/auth/login.vue`，包含两个步骤：
1. 获取微信授权（获取 openid 和 session_key）
2. 获取手机号授权（解密手机号完成登录）

**核心功能**
- 微信临时登录凭证获取
- 手机号授权和解密
- 用户信息存储
- 登录状态管理

#### 后端接口

需要后端提供以下接口：

**微信授权接口**
```
POST /api/users/wechat/login
```

**请求参数：**
```json
{
  "code": "微信登录code"
}
```

**响应数据：**
```json
{
  "success": true,
  "data": {
    "openid": "用户openid",
    "session_key": "会话密钥"
  },
  "message": "授权成功"
}
```

**微信手机号解密接口**
```
POST /api/users/wechat/phone
```

**请求参数：**
```json
{
  "encryptedData": "加密的手机号数据",
  "iv": "加密算法的初始向量",
  "signature": "数据签名，用于校验数据完整性",
  "sessionKey": "会话密钥",
  "openid": "用户openid"
}
```

**响应数据：**
```json
{
  "success": true,
  "data": {
    "token": "用户token",
    "userInfo": {
      "id": 1,
      "phone": "13800138000",
      "nickname": "用户昵称",
      "avatar": "头像URL"
    }
  },
  "message": "登录成功"
}
```

### 3. API接口

**接口地址**: `/api/users/wechat/login/phone`

**请求方法**: POST

**请求参数**:
- `code` (string, required): 微信临时code
- `encryptedData` (string, required): 加密的手机号数据
- `iv` (string, required): 解密向量

**响应格式**:
```json
{
  "success": true,
  "message": "登录成功",
  "data": {
    "token": "jwt_token_here",
    "user": {
      "id": 1,
      "phone": "13800138000",
      "nickname": "微信用户",
      "avatar": "https://..."
    }
  }
}
```

### 3. 前端实现

#### 工具函数 (`utils/wechat-auth.js`)

提供了以下核心函数：
- `handleWechatPhoneLogin()`: 处理微信手机号登录流程
- `handlePhoneAuth()`: 处理微信手机号授权回调
- `handleWechatLogin()`: 微信普通登录

#### 页面组件 (`subpkg/auth/login.vue`)

集成到现有登录页面，包含：
- 微信手机号一键登录按钮
- 微信普通登录按钮
- 手机号登录入口
- 用户协议确认

## 使用方法

### 1. 页面跳转
```javascript
uni.navigateTo({
  url: '/subpkg/auth/login'
})
```

### 2. 登录流程
1. 用户进入登录页面
2. 点击"微信手机号登录"按钮
3. 系统自动获取微信登录code并换取openid和session_key
4. 同时触发手机号授权，用户确认授权手机号访问权限
5. 系统将加密数据发送到后端解密
6. 登录成功后跳转到首页

### 3. 在页面中使用微信手机号登录

```vue
<template>
  <button 
    open-type="getPhoneNumber"
    @getphonenumber="handleWechatPhoneLogin"
  >
    微信手机号登录
  </button>
</template>

<script>
import { handlePhoneAuth } from '@/utils/wechat-auth'

export default {
  data() {
    return {
      wechatCode: ''
    }
  },
  
  async onLoad() {
    // 获取微信登录code
    const res = await uni.login({ provider: 'weixin' })
    this.wechatCode = res[1].code
  },
  
  methods: {
    async handleWechatPhoneLogin(e) {
      try {
        const result = await handlePhoneAuth(e, this.wechatCode)
        if (result.success) {
          // 登录成功处理
          uni.reLaunch({ url: '/pages/index/index' })
        }
      } catch (error) {
        console.error('登录失败:', error)
      }
    }
  }
}
</script>
```

### 4. 直接调用API接口

```javascript
import { wechatPhoneLogin } from '@/api/auth'

// 调用微信手机号登录接口
const loginResult = await wechatPhoneLogin({
  code: 'wx_code_here',
  encryptedData: 'encrypted_data_here',
  iv: 'iv_here'
})
```

## 注意事项

### 1. 微信小程序配置

需要在微信小程序后台配置以下权限：
- 获取用户手机号权限
- 服务器域名配置

### 2. 开发环境兼容

在非微信环境下会显示提示信息，并提供模拟登录选项用于开发调试

### 3. 权限配置

需要在微信开发者平台启用"获取用户手机号"权限

### 4. 数据安全

session_key和解密逻辑应仅在后端完成，避免泄露敏感信息

### 5. 用户体验

用户只需点击一个按钮即可完成完整的登录流程

**重要提醒**: 必须先获取 code 之后再获取手机号，否则会出现解密用的 session_key 对不上的问题 <mcreference link="https://blog.csdn.net/qq_39303936/article/details/116269471" index="0">0</mcreference>

### 6. 参数说明

微信小程序登录涉及的关键参数：
- **code**: 通过 wx.login 获取，主要用来校验
- **session_key**: 拿到 code 通过小程序的 auth.code2Session 可以获得，主要用来解密
- **iv**: 在有需要解密的数据都会返回这个，加密算法的初始向量，主要用来解密
- **encryptedData**: 需要解密的数据
- **signature**: 签名，主要用来解密和数据校验

### 7. 错误处理

常见错误及处理方式：

- **用户拒绝授权**: 提示用户授权的必要性
- **网络请求失败**: 提供重试机制
- **code过期**: 重新获取微信登录code
- **signature校验失败**: 检查数据完整性，重新获取授权

### 8. 安全考虑

- 加密数据传输过程中不要在前端解密
- 及时清理敏感数据
- 实现token过期自动刷新机制

## 测试流程

### 开发环境测试
1. **本地调试**
   - 在浏览器或HBuilderX中运行项目
   - 跳转到登录页面 `/subpkg/auth/login`
   - 点击"微信手机号登录"按钮
   - 选择"确定"进行模拟登录
   - 验证页面跳转是否正常

### 微信小程序环境测试
1. **准备工作**
   - 确保后端接口 `/api/users/wechat/login/phone` 已实现
   - 配置微信小程序的 AppID 和 AppSecret
   - 在微信公众平台配置服务器域名

2. **功能测试**
   - 在微信开发者工具中打开项目
   - 跳转到登录页面 `/subpkg/auth/login`
   - 点击"获取微信授权"按钮
   - 等待授权完成提示
   - 点击"获取手机号"按钮
   - 授权手机号访问权限
   - 验证登录成功后的页面跳转

3. **异常测试**
   - 测试用户拒绝授权的情况
   - 测试网络异常的处理
   - 测试后端接口返回错误的处理

## 常见问题

**Q: 为什么获取不到手机号？**
A: 检查是否正确配置了微信小程序权限，确保用户已授权。

**Q: 登录后token如何管理？**
A: 使用`utils/api.js`中的token管理功能，自动处理token存储和刷新。

**Q: 如何处理用户取消授权？**
A: 在`handlePhoneAuth`函数中已包含错误处理，会提示用户重新授权。

## 更新日志

- **v1.0.0** (2025-01-09)
  - 初始版本发布
  - 支持微信手机号一键登录
  - 完善错误处理机制
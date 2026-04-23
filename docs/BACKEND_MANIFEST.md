# Backend Manifest

更新时间: 2026-04-23

本清单由 Spring Boot 控制器注解、Swagger 注解和拦截器配置整理。权限列按 `WebMvcConfig`、`AuthInterceptor`、`AdminAuthInterceptor` 推导。

## 摘要

- 控制器数量: 17
- HTTP 映射数量: 87
- 普通用户鉴权: `Authorization: Bearer <user-jwt>`
- 管理员鉴权: `Authorization: Bearer <admin-jwt>`，且 JWT principalType 为 `admin`
- WebSocket: `/ws/orders`、`/ws/chat` 通过 `WebSocketAuthHandshakeInterceptor` 鉴权。

## 接口清单

| 控制器 | 方法 | 路径 | 功能 | 权限 | 源码 |
| --- | --- | --- | --- | --- | --- |
| AiGuideController | POST | `/ai/guide/appointments` | 创建导诊预约 | 公开 | `src/main/java/org/example/controller/AiGuideController.java:52` |
| AiGuideController | GET | `/ai/guide/attendants/match` | 匹配陪诊师 | 公开 | `src/main/java/org/example/controller/AiGuideController.java:77` |
| AiGuideController | POST | `/ai/guide/ai-appointment/session` | 创建 AI 预约会话 | 用户 JWT | `src/main/java/org/example/controller/AiGuideController.java:96` |
| AiGuideController | GET | `/ai/guide/ai-appointment/session/{sessionId}` | 获取 AI 预约会话状态 | 用户 JWT | `src/main/java/org/example/controller/AiGuideController.java:105` |
| AiGuideController | GET | `/ai/guide/ai-appointment/session/latest` | 获取最近一次可恢复 AI 预约会话 | 用户 JWT | `src/main/java/org/example/controller/AiGuideController.java:118` |
| AiGuideController | GET | `/ai/guide/ai-appointment/session/latest-overview` | 获取最近一次 AI导诊会话概览 | 用户 JWT | `src/main/java/org/example/controller/AiGuideController.java:126` |
| AiGuideController | POST | `/ai/guide/ai-appointment/session/{sessionId}/reply` | 回复 AI 预约追问 | 用户 JWT | `src/main/java/org/example/controller/AiGuideController.java:133` |
| AiGuideController | POST | `/ai/guide/ai-appointment/session/{sessionId}/match` | 开始 AI 预约匹配 | 用户 JWT | `src/main/java/org/example/controller/AiGuideController.java:143` |
| AiGuideController | POST | `/ai/guide/attendants/ai-match` | AI 智能匹配陪诊师 | 用户 JWT | `src/main/java/org/example/controller/AiGuideController.java:152` |
| AiGuideController | POST | `/ai/guide/orders` | 创建订单 | 公开 | `src/main/java/org/example/controller/AiGuideController.java:159` |
| AiGuideController | POST | `/ai/guide/payments/status` | 更新支付状态 | 公开 | `src/main/java/org/example/controller/AiGuideController.java:187` |
| AiGuideController | GET | `/ai/guide/orders/{orderNo}` | 查询订单详情 | 公开 | `src/main/java/org/example/controller/AiGuideController.java:214` |
| AiGuideController | GET | `/ai/guide/orders/{orderNo}/payment-status` | 查询订单支付状态 | 公开 | `src/main/java/org/example/controller/AiGuideController.java:228` |
| AiGuideController | GET | `/ai/guide/orders/{orderNo}/complete-info` | 查询订单完整信息 | 公开 | `src/main/java/org/example/controller/AiGuideController.java:242` |
| AiGuideController | POST | `/ai/guide/test/create-order-with-appointment` | 测试创建带预约订单 | 公开 | `src/main/java/org/example/controller/AiGuideController.java:256` |
| AiGuideController | GET | `/ai/guide/test/latest-appointment` | 查询最新预约 | 公开 | `src/main/java/org/example/controller/AiGuideController.java:275` |
| AiMedicalController | POST | `/ai/medical/qa` | 提交 AI 医疗问答 | 用户 JWT | `src/main/java/org/example/controller/AiMedicalController.java:30` |
| AiMedicalController | GET | `/ai/medical/qa/{recordId}` | 查询问答记录 | 用户 JWT | `src/main/java/org/example/controller/AiMedicalController.java:45` |
| AiMedicalController | GET | `/ai/medical/qa/conversation/{conversationId}` | 查询问答会话 | 用户 JWT | `src/main/java/org/example/controller/AiMedicalController.java:63` |
| AiMedicalController | GET | `/ai/medical/qa/latest` | 获取最近一次 AI 导诊会话 | 用户 JWT | `src/main/java/org/example/controller/AiMedicalController.java:76` |
| AiMedicalController | GET | `/ai/medical/qa/thinking/{recordId}` | 兼容旧版思考过程查询 | 用户 JWT | `src/main/java/org/example/controller/AiMedicalController.java:83` |
| AppUpgradeController | POST | `/api/app-upgrade/check` | 检查 App 更新 | 公开 | `src/main/java/org/example/controller/AppUpgradeController.java:39` |
| AttendantController | GET | `/attendant/profile/{userId}` | 查询陪诊师资料 | 用户 JWT | `src/main/java/org/example/controller/AttendantController.java:66` |
| AttendantController | GET | `/attendant/profile/{userId}/reviews` | 查询公开评价 | 用户 JWT | `src/main/java/org/example/controller/AttendantController.java:89` |
| AttendantController | PUT | `/attendant/profile/{userId}` | 更新资料 | 用户 JWT | `src/main/java/org/example/controller/AttendantController.java:123` |
| AttendantController | POST | `/attendant/profile/avatar` | 上传头像 | 用户 JWT | `src/main/java/org/example/controller/AttendantController.java:185` |
| AttendantController | PUT | `/attendant/qualification/{userId}` | 更新资质 | 用户 JWT | `src/main/java/org/example/controller/AttendantController.java:222` |
| AttendantController | POST | `/attendant/qualification/{userId}/submit` | 提交资质审核 | 用户 JWT | `src/main/java/org/example/controller/AttendantController.java:266` |
| AttendantController | GET | `/attendant/orders/waiting` | 查询待接订单 | 用户 JWT | `src/main/java/org/example/controller/AttendantController.java:295` |
| AttendantController | POST | `/attendant/orders/{orderId}/accept` | 接单 | 用户 JWT | `src/main/java/org/example/controller/AttendantController.java:327` |
| AttendantController | POST | `/attendant/orders/{orderId}/reject-assigned` | 拒绝专属派单 | 用户 JWT | `src/main/java/org/example/controller/AttendantController.java:353` |
| AttendantController | POST | `/attendant/orders/{orderId}/start` | 开始服务 | 用户 JWT | `src/main/java/org/example/controller/AttendantController.java:375` |
| AttendantController | POST | `/attendant/orders/{orderId}/end` | 结束服务 | 用户 JWT | `src/main/java/org/example/controller/AttendantController.java:400` |
| AttendantController | POST | `/attendant/orders/{orderId}/service-progress` | 更新服务进度 | 用户 JWT | `src/main/java/org/example/controller/AttendantController.java:427` |
| AttendantController | GET | `/attendant/orders` | 查询陪诊师订单 | 用户 JWT | `src/main/java/org/example/controller/AttendantController.java:454` |
| AttendantController | GET | `/attendant/orders/{orderId}` | 查询订单详情 | 用户 JWT | `src/main/java/org/example/controller/AttendantController.java:559` |
| AttendantController | POST|PUT | `/attendant/orders/{orderId}/cancel` | 取消订单 | 用户 JWT | `src/main/java/org/example/controller/AttendantController.java:598` |
| AttendantController | POST | `/attendant/orders/{orderId}/scan-qr` | 扫码核销 | 用户 JWT | `src/main/java/org/example/controller/AttendantController.java:650` |
| AttendantController | GET | `/attendant/orders/{orderId}/evaluation` | 查询订单评价 | 用户 JWT | `src/main/java/org/example/controller/AttendantController.java:681` |
| AttendantController | POST | `/attendant/orders/{orderId}/evaluation/reply` | 回复评价 | 用户 JWT | `src/main/java/org/example/controller/AttendantController.java:716` |
| AttendantController | GET | `/attendant/recommended` | 查询推荐陪诊师 | 公开 | `src/main/java/org/example/controller/AttendantController.java:759` |
| ChatController | POST | `/api/chat/send` | 发送聊天消息 | 用户 JWT | `src/main/java/org/example/controller/ChatController.java:37` |
| ChatController | GET | `/api/chat/history` | 查询聊天历史 | 用户 JWT | `src/main/java/org/example/controller/ChatController.java:53` |
| ChatController | GET | `/api/chat/system` | 查询系统消息 | 用户 JWT | `src/main/java/org/example/controller/ChatController.java:68` |
| ChatController | GET | `/api/chat/system/{messageId}` | 查询系统消息详情 | 用户 JWT | `src/main/java/org/example/controller/ChatController.java:80` |
| ChatController | GET | `/api/chat/contacts` | 查询最近联系人 | 用户 JWT | `src/main/java/org/example/controller/ChatController.java:116` |
| ChatController | POST | `/api/chat/read` | 标记已读 | 用户 JWT | `src/main/java/org/example/controller/ChatController.java:127` |
| FileUploadController | POST | `/api/common/upload` | 上传文件 | 公开 | `src/main/java/org/example/controller/FileUploadController.java:26` |
| FileUploadController | POST | `/api/common/upload-image` | 上传图片 | 公开 | `src/main/java/org/example/controller/FileUploadController.java:45` |
| FileUploadController | POST | `/api/common/upload-avatar` | 上传头像 | 用户 JWT | `src/main/java/org/example/controller/FileUploadController.java:64` |
| OrderController | POST | `/api/orders` | 创建订单 | 用户 JWT | `src/main/java/org/example/controller/OrderController.java:32` |
| OrderController | GET | `/api/orders/{orderId}` | 按 ID 查询订单 | 用户 JWT | `src/main/java/org/example/controller/OrderController.java:55` |
| OrderController | PUT | `/api/orders/{orderId}` | 更新订单 | 用户 JWT | `src/main/java/org/example/controller/OrderController.java:76` |
| OrderController | POST | `/api/orders/{orderId}/confirm-time-fee` | 确认时长费用 | 用户 JWT | `src/main/java/org/example/controller/OrderController.java:93` |
| OrderController | POST | `/api/orders/{orderId}/dispute-time-fee` | 申诉时长费用 | 用户 JWT | `src/main/java/org/example/controller/OrderController.java:115` |
| OrderController | GET | `/api/orders/user-orders` | 查询当前用户订单 | 用户 JWT | `src/main/java/org/example/controller/OrderController.java:141` |
| OrderController | PUT | `/api/orders/{orderId}/cancel` | 取消订单 | 用户 JWT | `src/main/java/org/example/controller/OrderController.java:171` |
| OrderEvaluationController | GET | `/api/orders/{orderId}/evaluation` | 查询评价 | 用户 JWT | `src/main/java/org/example/controller/OrderEvaluationController.java:31` |
| OrderEvaluationController | POST | `/api/orders/{orderId}/evaluation` | 提交评价 | 用户 JWT | `src/main/java/org/example/controller/OrderEvaluationController.java:60` |
| PublicQrCodeController | GET | `/order-qr/{orderId}.png` | 获取订单服务核销二维码图片 | 公开 | `src/main/java/org/example/controller/PublicQrCodeController.java:28` |
| UserAttendantController | GET | `/user/attendants/{attendantId}` | 查询陪诊师详情 | 公开 | `src/main/java/org/example/controller/UserAttendantController.java:36` |
| UserController | POST | `/api/users/register` | 注册用户 | 公开 | `src/main/java/org/example/controller/UserController.java:44` |
| UserController | POST | `/api/users/login` | 登录 | 公开 | `src/main/java/org/example/controller/UserController.java:121` |
| UserController | GET | `/api/users/wechat/config-status` | 获取微信登录配置状态 | 公开 | `src/main/java/org/example/controller/UserController.java:150` |
| UserController | POST | `/api/users/wechat/login` | 微信登录 | 公开 | `src/main/java/org/example/controller/UserController.java:158` |
| UserController | POST | `/api/users/wechat/bind-phone` | 绑定微信手机号 | 公开 | `src/main/java/org/example/controller/UserController.java:178` |
| UserController | GET | `/api/users/current` | 查询当前用户 | 用户 JWT | `src/main/java/org/example/controller/UserController.java:198` |
| UserController | GET | `/api/users` | 查询用户列表 | 用户 JWT | `src/main/java/org/example/controller/UserController.java:223` |
| UserController | GET | `/api/users/{id}` | 查询用户详情 | 用户 JWT | `src/main/java/org/example/controller/UserController.java:233` |
| UserController | PUT | `/api/users/{id}` | 更新用户 | 用户 JWT | `src/main/java/org/example/controller/UserController.java:249` |
| UserController | DELETE | `/api/users/{id}` | 删除用户 | 用户 JWT | `src/main/java/org/example/controller/UserController.java:274` |
| AdminAdminUserController | GET | `/api/admin/admin-users` | 列表查询 | 管理员 JWT | `src/main/java/org/example/controller/admin/AdminAdminUserController.java:23` |
| AdminAdminUserController | POST | `/api/admin/admin-users` | 创建 | 管理员 JWT | `src/main/java/org/example/controller/admin/AdminAdminUserController.java:32` |
| AdminAdminUserController | PATCH | `/api/admin/admin-users/{adminId}/status` | 更新状态 | 管理员 JWT | `src/main/java/org/example/controller/admin/AdminAdminUserController.java:42` |
| AdminAttendantController | GET | `/api/admin/attendants` | 列表查询 | 管理员 JWT | `src/main/java/org/example/controller/admin/AdminAttendantController.java:23` |
| AdminAttendantController | GET | `/api/admin/attendants/{userId}` | 查询详情 | 管理员 JWT | `src/main/java/org/example/controller/admin/AdminAttendantController.java:32` |
| AdminAttendantController | PATCH | `/api/admin/attendants/{userId}/status` | 更新状态 | 管理员 JWT | `src/main/java/org/example/controller/admin/AdminAttendantController.java:41` |
| AdminAttendantController | PATCH | `/api/admin/attendants/{userId}/qualification-review` | 审核资质 | 管理员 JWT | `src/main/java/org/example/controller/admin/AdminAttendantController.java:53` |
| AdminAuthController | POST | `/api/admin/auth/login` | 登录 | 公开 | `src/main/java/org/example/controller/admin/AdminAuthController.java:19` |
| AdminDashboardController | GET | `/api/admin/dashboard/overview` | 查询概览 | 管理员 JWT | `src/main/java/org/example/controller/admin/AdminDashboardController.java:18` |
| AdminOrderController | GET | `/api/admin/orders` | 列表查询 | 管理员 JWT | `src/main/java/org/example/controller/admin/AdminOrderController.java:23` |
| AdminOrderController | GET | `/api/admin/orders/{orderId}` | 查询详情 | 管理员 JWT | `src/main/java/org/example/controller/admin/AdminOrderController.java:35` |
| AdminOrderController | PATCH | `/api/admin/orders/{orderId}/cancel` | cancel | 管理员 JWT | `src/main/java/org/example/controller/admin/AdminOrderController.java:44` |
| AdminOrderController | PATCH | `/api/admin/orders/{orderId}/dispute-resolution` | 处理争议 | 管理员 JWT | `src/main/java/org/example/controller/admin/AdminOrderController.java:56` |
| AdminUserController | GET | `/api/admin/users` | 列表查询 | 管理员 JWT | `src/main/java/org/example/controller/admin/AdminUserController.java:22` |
| AdminUserController | GET | `/api/admin/users/{userId}` | 查询详情 | 管理员 JWT | `src/main/java/org/example/controller/admin/AdminUserController.java:32` |
| AdminUserController | PATCH | `/api/admin/users/{userId}/status` | 更新状态 | 管理员 JWT | `src/main/java/org/example/controller/admin/AdminUserController.java:41` |

## 权限规则

- `/api/admin/**` 默认需要管理员 JWT，`/api/admin/auth/login` 公开。
- `/api/**`、`/attendant/**`、`/ai/medical/**` 默认需要用户 JWT，登录注册、微信登录绑定、部分上传、App 升级检查和 `/attendant/recommended` 公开。
- `/ai/guide/**` 默认需要用户 JWT；预约创建、陪诊师匹配、订单创建/查询/支付状态、测试预约接口按当前拦截器配置公开。
- `/user/attendants/**` 与 `/order-qr/**` 当前未被 MVC 鉴权拦截器覆盖，按公开接口记录。

## 维护规则

- 新增、删除或改名任意控制器路径时，必须同步更新本文件。
- 修改 `WebMvcConfig` 或鉴权拦截器白名单时，必须复核权限列。

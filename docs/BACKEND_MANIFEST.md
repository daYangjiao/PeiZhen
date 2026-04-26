# Backend Manifest

更新时间: 2026-04-25

本清单由 Spring Boot 控制器注解、Swagger 注解和拦截器配置整理。权限列按 `WebMvcConfig`、`AuthInterceptor`、`AdminAuthInterceptor` 推导。

## 摘要

- 控制器数量: 19
- HTTP 映射数量: 97
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
| UserAttendantController | GET | `/user/attendants/{attendantId}` | 用户端查询陪诊师详情，评分、评价数、好评率按 `order_evaluation.rating` 聚合返回，兼容旧入口 | 公开 | `src/main/java/org/example/controller/UserAttendantController.java:36` |
| AttendantController | GET | `/attendant/profile/{userId}` | 查询陪诊师资料，收入/余额按已完成订单最终金额扣除平台服务费后的口径返回；评分、评价数、好评率只按 `order_evaluation.rating` 聚合；资质上传状态以可展示原图或扫描预览图为准 | 用户 JWT | `src/main/java/org/example/controller/AttendantController.java:66` |
| AttendantController | GET | `/attendant/profile/{userId}/reviews` | 查询公开评价 | 用户 JWT | `src/main/java/org/example/controller/AttendantController.java:89` |
| AttendantController | PUT | `/attendant/profile/{userId}` | 更新资料 | 用户 JWT | `src/main/java/org/example/controller/AttendantController.java:123` |
| AttendantController | POST | `/attendant/profile/avatar` | 上传头像 | 用户 JWT | `src/main/java/org/example/controller/AttendantController.java:185` |
| AttendantController | PUT | `/attendant/qualification/{userId}` | 更新资质和证件有效期 | 用户 JWT，仅本人 | `src/main/java/org/example/controller/AttendantController.java:222` |
| AttendantController | POST | `/attendant/qualification/{userId}/submit` | 提交资质审核，校验证件完整和有效期 | 用户 JWT，仅本人 | `src/main/java/org/example/controller/AttendantController.java:277` |
| AttendantController | GET | `/attendant/orders/waiting` | 查询待接订单，需资质通过且证件有效 | 用户 JWT | `src/main/java/org/example/controller/AttendantController.java:311` |
| AttendantController | POST | `/attendant/orders/{orderId}/accept` | 接单；专属派单超过15分钟会服务端释放回公共接单大厅 | 用户 JWT | `src/main/java/org/example/controller/AttendantController.java:347` |
| AttendantController | POST | `/attendant/orders/{orderId}/reject-assigned` | 拒绝专属派单 | 用户 JWT | `src/main/java/org/example/controller/AttendantController.java:378` |
| AttendantController | POST | `/attendant/orders/{orderId}/start` | 开始服务 | 用户 JWT | `src/main/java/org/example/controller/AttendantController.java:400` |
| AttendantController | POST | `/attendant/orders/{orderId}/end` | 结束服务 | 用户 JWT | `src/main/java/org/example/controller/AttendantController.java:425` |
| AttendantController | POST | `/attendant/orders/{orderId}/service-progress` | 更新服务进度 | 用户 JWT | `src/main/java/org/example/controller/AttendantController.java:452` |
| AttendantController | GET | `/attendant/orders` | 查询陪诊师订单，返回统一结算字段 `settlementAmount`、`platformFeeAmount`、`attendantIncomeAmount`；列表项返回 `paymentTime` 供专属派单确认窗口判断 | 用户 JWT | `src/main/java/org/example/controller/AttendantController.java:479` |
| AttendantController | GET | `/attendant/orders/{orderId}` | 查询订单详情，返回统一结算字段 `settlementAmount`、`platformFeeAmount`、`attendantIncomeAmount` | 用户 JWT | `src/main/java/org/example/controller/AttendantController.java:585` |
| AttendantController | POST|PUT | `/attendant/orders/{orderId}/cancel` | 取消订单 | 用户 JWT | `src/main/java/org/example/controller/AttendantController.java:624` |
| AttendantController | POST | `/attendant/orders/{orderId}/scan-qr` | 扫码核销 | 用户 JWT | `src/main/java/org/example/controller/AttendantController.java:676` |
| AttendantController | GET | `/attendant/orders/{orderId}/evaluation` | 查询订单评价 | 用户 JWT | `src/main/java/org/example/controller/AttendantController.java:707` |
| AttendantController | POST | `/attendant/orders/{orderId}/evaluation/reply` | 回复评价 | 用户 JWT | `src/main/java/org/example/controller/AttendantController.java:742` |
| AttendantController | GET | `/attendant/recommended` | 查询推荐陪诊师，评分、评价数、好评率只按 `order_evaluation.rating` 聚合 | 公开 | `src/main/java/org/example/controller/AttendantController.java:785` |
| ChatController | POST | `/api/chat/send` | 发送聊天消息 | 用户 JWT | `src/main/java/org/example/controller/ChatController.java:37` |
| ChatController | GET | `/api/chat/history` | 查询聊天历史 | 用户 JWT | `src/main/java/org/example/controller/ChatController.java:53` |
| ChatController | GET | `/api/chat/system` | 查询系统消息 | 用户 JWT | `src/main/java/org/example/controller/ChatController.java:68` |
| ChatController | GET | `/api/chat/system/{messageId}` | 查询系统消息详情 | 用户 JWT | `src/main/java/org/example/controller/ChatController.java:80` |
| ChatController | GET | `/api/chat/contacts` | 查询最近联系人 | 用户 JWT | `src/main/java/org/example/controller/ChatController.java:116` |
| ChatController | POST | `/api/chat/read` | 标记已读 | 用户 JWT | `src/main/java/org/example/controller/ChatController.java:127` |
| FileUploadController | POST | `/api/common/upload` | 上传文件 | 公开 | `src/main/java/org/example/controller/FileUploadController.java:27` |
| FileUploadController | POST | `/api/common/upload-image` | 上传图片并生成文稿扫描预览图，返回原图和扫描图地址 | 公开 | `src/main/java/org/example/controller/FileUploadController.java:46` |
| FileUploadController | POST | `/api/common/upload-avatar` | 上传头像 | 用户 JWT | `src/main/java/org/example/controller/FileUploadController.java:65` |
| OrderController | POST | `/api/orders` | 创建订单 | 用户 JWT | `src/main/java/org/example/controller/OrderController.java:32` |
| OrderController | GET | `/api/orders/{orderId}` | 按 ID 查询订单 | 用户 JWT | `src/main/java/org/example/controller/OrderController.java:55` |
| OrderController | PUT | `/api/orders/{orderId}` | 更新订单 | 用户 JWT | `src/main/java/org/example/controller/OrderController.java:76` |
| OrderController | POST | `/api/orders/{orderId}/confirm-time-fee` | 确认时长费用 | 用户 JWT | `src/main/java/org/example/controller/OrderController.java:93` |
| OrderController | POST | `/api/orders/{orderId}/dispute-time-fee` | 申诉时长费用 | 用户 JWT | `src/main/java/org/example/controller/OrderController.java:118` |
| OrderController | POST | `/api/orders/{orderId}/pay-balance` | 支付时长费用差额，成功后完成订单 | 用户 JWT | `src/main/java/org/example/controller/OrderController.java:147` |
| OrderController | GET | `/api/orders/user-orders` | 查询当前用户订单 | 用户 JWT | `src/main/java/org/example/controller/OrderController.java:163` |
| OrderController | PUT | `/api/orders/{orderId}/cancel` | 取消订单 | 用户 JWT | `src/main/java/org/example/controller/OrderController.java:193` |
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
| AdminAdminUserController | POST | `/api/admin/admin-users` | 创建 | 管理员 JWT | `src/main/java/org/example/controller/admin/AdminAdminUserController.java:37` |
| AdminAdminUserController | PATCH | `/api/admin/admin-users/{adminId}/status` | 更新状态 | 管理员 JWT | `src/main/java/org/example/controller/admin/AdminAdminUserController.java:49` |
| AdminAdminUserController | DELETE | `/api/admin/admin-users/{adminId}` | 删除管理员账号 | 管理员 JWT，仅超级管理员可操作 | `src/main/java/org/example/controller/admin/AdminAdminUserController.java:63` |
| AdminAttendantController | GET | `/api/admin/attendants` | 列表查询 | 管理员 JWT | `src/main/java/org/example/controller/admin/AdminAttendantController.java:25` |
| AdminAttendantController | GET | `/api/admin/attendants/next-pending` | 查询下一条待审核陪诊师 | 管理员 JWT | `src/main/java/org/example/controller/admin/AdminAttendantController.java:34` |
| AdminAttendantController | GET | `/api/admin/attendants/{userId}` | 查询详情 | 管理员 JWT | `src/main/java/org/example/controller/admin/AdminAttendantController.java:40` |
| AdminAttendantController | GET | `/api/admin/attendants/{userId}/qualification-logs` | 查询资质审核记录，超级管理员返回审核人字段 | 管理员 JWT | `src/main/java/org/example/controller/admin/AdminAttendantController.java:50` |
| AdminAttendantController | PATCH | `/api/admin/attendants/{userId}/status` | 更新状态 | 管理员 JWT | `src/main/java/org/example/controller/admin/AdminAttendantController.java:61` |
| AdminAttendantController | PATCH | `/api/admin/attendants/{userId}/qualification-review` | 审核资质 | 管理员 JWT | `src/main/java/org/example/controller/admin/AdminAttendantController.java:73` |
| AdminAuthController | POST | `/api/admin/auth/login` | 登录 | 公开 | `src/main/java/org/example/controller/admin/AdminAuthController.java:19` |
| AdminDashboardController | GET | `/api/admin/dashboard/overview` | 查询概览，超级管理员额外返回今日处理量和最近操作日志摘要 | 管理员 JWT | `src/main/java/org/example/controller/admin/AdminDashboardController.java:21` |
| AdminOrderController | GET | `/api/admin/orders` | 列表查询 | 管理员 JWT | `src/main/java/org/example/controller/admin/AdminOrderController.java:23` |
| AdminOrderController | GET | `/api/admin/orders/{orderId}` | 查询详情 | 管理员 JWT | `src/main/java/org/example/controller/admin/AdminOrderController.java:35` |
| AdminOrderController | PATCH | `/api/admin/orders/{orderId}/cancel` | cancel | 管理员 JWT | `src/main/java/org/example/controller/admin/AdminOrderController.java:45` |
| AdminOrderController | PATCH | `/api/admin/orders/{orderId}/dispute-resolution` | 处理争议 | 管理员 JWT | `src/main/java/org/example/controller/admin/AdminOrderController.java:57` |
| AdminOperationLogController | GET | `/api/admin/operation-logs` | 查询后台操作日志，支持模块、动作、角色、关键词和时间筛选 | 管理员 JWT，仅超级管理员可用 | `src/main/java/org/example/controller/admin/AdminOperationLogController.java:23` |
| AdminUserController | GET | `/api/admin/users` | 列表查询 | 管理员 JWT | `src/main/java/org/example/controller/admin/AdminUserController.java:22` |
| AdminUserController | GET | `/api/admin/users/{userId}` | 查询详情 | 管理员 JWT | `src/main/java/org/example/controller/admin/AdminUserController.java:32` |
| AdminUserController | PATCH | `/api/admin/users/{userId}/status` | 更新状态 | 管理员 JWT | `src/main/java/org/example/controller/admin/AdminUserController.java:41` |
| AdminWorkbenchController | GET | `/api/admin/workbench/summary` | 查询处理工作台争议、待审和当前管理员领取数 | 管理员 JWT | `src/main/java/org/example/controller/admin/AdminWorkbenchController.java:23` |
| AdminWorkbenchController | GET | `/api/admin/workbench/tasks` | 查询处理工作台待办队列，超级管理员可见领取人身份 | 管理员 JWT | `src/main/java/org/example/controller/admin/AdminWorkbenchController.java:28` |
| AdminWorkbenchController | POST | `/api/admin/workbench/tasks/{type}/{targetId}/claim` | 领取或续期处理工作台任务，未过期他人领取返回冲突 | 管理员 JWT | `src/main/java/org/example/controller/admin/AdminWorkbenchController.java:42` |
| AdminWorkbenchController | POST | `/api/admin/workbench/tasks/{type}/{targetId}/complete` | 提交工作台任务处理，校验领取锁和业务状态 | 管理员 JWT | `src/main/java/org/example/controller/admin/AdminWorkbenchController.java:57` |
| AdminWorkbenchController | DELETE | `/api/admin/workbench/tasks/{type}/{targetId}/claim` | 释放当前管理员自己的任务领取锁 | 管理员 JWT | `src/main/java/org/example/controller/admin/AdminWorkbenchController.java:74` |

## 权限规则

- `/api/admin/**` 默认需要管理员 JWT，`/api/admin/auth/login` 公开。
- `/api/**`、`/attendant/**`、`/ai/medical/**` 默认需要用户 JWT，登录注册、微信登录绑定、部分上传、App 升级检查和 `/attendant/recommended` 公开。
- `/ai/guide/**` 默认需要用户 JWT；预约创建、陪诊师匹配、订单创建/查询/支付状态、测试预约接口按当前拦截器配置公开。
- `/user/attendants/**` 与 `/order-qr/**` 当前未被 MVC 鉴权拦截器覆盖，按公开接口记录。

## 上传响应约定

- `/api/common/upload-image` 返回 `ImageUploadResponse`：`url`、`originalUrl`、`scanUrl`、`scanGenerated`。
- 资质材料上传保存 `originalUrl` 为原始文件地址，保存 `scanUrl` 为后台审核默认预览图；扫描失败时 `scanUrl` 回退为 `originalUrl`。陪诊师资料响应中的上传状态必须与可展示图片一致：存在原图或扫描预览图才返回已上传。

## 评价评分口径

- 陪诊师评分、评价总数和好评率统一以 `order_evaluation.rating` 实时聚合为准；`rating >= 4` 计入好评。
- 用户订单详情中的陪诊师星级展示平均分，用户端陪诊师主页和陪诊师个人首页中的好评率展示好评占比；二者不是同一个数值，但都必须使用同一组 `order_evaluation.rating` 和评价总数。
- 没有评价时，接口返回 `score = null`、`evaluationCount = 0`、`praiseRate = 0`，前端展示为“暂无评分/暂无评价”。
- `attendant.score` 仅作为兼容缓存字段保留，不作为用户端、陪诊师端或管理端展示依据。

## 订单结算口径

- 陪诊师实际收入统一由后端 `OrderSettlementCalculator` 计算，订单列表和详情返回 `settlementAmount`、`platformFeeAmount`、`attendantIncomeAmount`。
- 只有 `orderStatus = 6` 的已完成订单产生钱包收入；`orderStatus = 7` 的取消、退款或超时关闭订单结算金额和陪诊师收入均为 0。
- 新争议/退款流程中 `order_amount` 已保存最终结算金额；若历史订单仅有 `refund_amount` 且没有负数 `balance_amount`，后端兼容为 `order_amount - refund_amount` 后再扣平台服务费。
- 平台服务费率当前为 10%，陪诊师收入为实际结算金额扣除平台服务费后的 90%；前端不得直接用原始订单金额作为钱包收入。

## 维护规则

- 本文件是可推送的后端接口清单；真实连接信息和密码只允许留在本地 `SECRET_VAULT.md`。
- 接口数量按方法级 HTTP 映射统计，不包含控制器类上的基础 `@RequestMapping`。
- 新增、删除或改名任意控制器路径时，必须同步更新本文件。
- 修改 `WebMvcConfig` 或鉴权拦截器白名单时，必须复核权限列。

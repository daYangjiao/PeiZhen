# Database Map

更新时间: 2026-04-26

本文件记录当前数据库结构、迁移脚本位置和以后数据库变更规则。当前基线主要来自 `database/student.sql`，历史增量来自 `database/*.sql`，后续新增变更统一写入根目录 `db/`。

## 变更规则

- 以后只要要求修改数据库，必须新增一个日期命名脚本：`db/YYYYMMDD_descriptive_name.sql`，例如 `db/20260423_add_table.sql`。
- 每个脚本必须可重复审阅：包含明确的 `ALTER TABLE`、`CREATE TABLE`、`INSERT` 或回填语句，不把业务解释只写在聊天里。
- 每次新增或修改数据库脚本后，必须同步更新本文件的“脚本清单”和相关表说明。
- 不把数据库密码、SSH 密码、API Key 写入本文件；真实连接信息只放本地 `SECRET_VAULT.md`。

## 脚本清单

| 路径 | 用途 |
| --- | --- |
| `database/admin_identity_isolation.sql` | 管理端管理员身份隔离增量 |
| `database/ai_appointment_persistence.sql` | AI 预约导诊持久化相关增量 |
| `database/ai_medical_qa_user_history.sql` | AI 医疗问答用户历史增量 |
| `database/chat_message_add_order_id.sql` | 聊天消息关联订单增量 |
| `database/sys_admin_role.sql` | 历史管理员角色字段增量；当前完整基线和后续自动部署脚本已包含同等语义 |
| `database/student.sql` | 当前完整建表和演示数据基线 |
| `db/20260424_attendant_qualification_audit_gate.sql` | 陪诊师资质有效期、审核日志和 18650680037 超级管理员回填；列新增采用幂等检查，重复执行不应中断部署 |
| `db/20260424_admin_operation_log_and_order_balance.sql` | 全局后台操作日志表；订单状态 5/9 争议与补差额语义同步 |
| `db/20260425_admin_workbench_task_claim.sql` | 管理端处理工作台任务领取锁，防止多人同时处理争议订单或待审陪诊师 |
| `db/20260425_attendant_qualification_scan_urls.sql` | 陪诊师资质材料扫描预览图字段；保留原图并为后台审核生成裁切增强图 |
| `db/20260426_third_party_account_wechat_login.sql` | 微信多端登录第三方身份表；回填历史 `user.openid` 为小程序微信身份 |
| `db/20260426_split_attendant_qualification_status.sql` | 拆分陪诊师资质状态与账号封禁状态；旧 `attendant.status=2` 迁移为 `user.status=0` 且 `qualification_status=2` |
| `db/.gitkeep` | 保留后续日期命名数据库脚本目录 |

## 表结构地图

| 表 | 用途 | 关键字段 |
| --- | --- | --- |
| `user` | 用户基础账号，区分普通用户和陪诊师登录身份。 | `id int`<br>`password varchar(100)`<br>`name varchar(50)`<br>`phone varchar(20)`<br>`sex varchar(10)`<br>`age int`<br>`avatar varchar(255)`<br>`user_type int`<br>`status tinyint`<br>`openid varchar(64)`<br>`create_time datetime` |
| `sys_admin` | 管理端管理员账号，独立于普通用户体系。 | `id int`<br>`name varchar(50)`<br>`phone varchar(20)`<br>`password varchar(100)`<br>`status tinyint`<br>`role varchar(20)`<br>`create_time datetime`<br>`update_time datetime`<br>`last_login_time datetime` |
| `third_party_account` | 用户和管理员的第三方登录身份绑定，当前用于微信小程序、App、H5 和管理端网页扫码登录。 | `id bigint`<br>`principal_type varchar(20)`<br>`principal_id int`<br>`provider varchar(30)`<br>`platform varchar(30)`<br>`openid varchar(128)`<br>`unionid varchar(128)`<br>`create_time datetime`<br>`update_time datetime` |
| `attendant` | 陪诊师业务资料、资质审核状态、评分兼容缓存、医院和服务能力；账号封禁只看 `user.status`，展示评分以 `order_evaluation.rating` 聚合为准。 | `user_id int`<br>`certificate varchar(100)`<br>`status int` 历史兼容字段<br>`qualification_status int` 资质状态，0=待审核、1=已通过、2=未通过<br>`qualification_fail_reason varchar(255)`<br>`introduction text`<br>`professional_field varchar(255)`<br>`score decimal(2,1)`<br>`experience_years int`<br>`hospital_name varchar(100)`<br>`service_count int`<br>`create_time datetime`<br>`update_time datetime` |
| `attendant_qualification` | 陪诊师身份证、执业证、健康证等资质材料上传状态、原图地址、扫描预览图地址和证件有效期。 | `user_id int`<br>`id_card_uploaded tinyint(1)`<br>`practice_cert_uploaded tinyint(1)`<br>`health_cert_uploaded tinyint(1)`<br>`id_card_file_url varchar(255)`<br>`id_card_front_file_url varchar(255)`<br>`id_card_front_scan_file_url varchar(255)`<br>`id_card_back_file_url varchar(255)`<br>`id_card_back_scan_file_url varchar(255)`<br>`practice_cert_file_url varchar(255)`<br>`practice_cert_scan_file_url varchar(255)`<br>`health_cert_file_url varchar(255)`<br>`health_cert_scan_file_url varchar(255)`<br>`practice_cert_expire_date varchar(20)`<br>`health_cert_expire_date varchar(20)`<br>`create_time datetime`<br>`update_time datetime` |
| `attendant_qualification_audit_log` | 陪诊师资质上传、提交、审核、驳回、封禁和恢复操作日志。 | `id bigint`<br>`user_id int`<br>`actor_type varchar(20)`<br>`actor_id int`<br>`actor_name varchar(50)`<br>`actor_phone varchar(20)`<br>`actor_role varchar(20)`<br>`action varchar(30)`<br>`from_status int`<br>`to_status int`<br>`reason varchar(255)`<br>`snapshot_json json`<br>`create_time datetime` |
| `admin_operation_log` | 全局管理端操作日志，供超级管理员按模块、动作、管理员角色、对象和时间审计。 | `id bigint`<br>`operator_id int`<br>`operator_name varchar(50)`<br>`operator_phone varchar(20)`<br>`operator_role varchar(20)`<br>`module varchar(40)`<br>`action varchar(50)`<br>`target_type varchar(40)`<br>`target_id int`<br>`target_label varchar(100)`<br>`from_status int`<br>`to_status int`<br>`remark varchar(255)`<br>`snapshot_json json`<br>`create_time datetime` |
| `admin_task_claim` | 管理端处理工作台任务领取锁，按任务类型和目标 ID 保证同一任务同一时间只由一个管理员处理。 | `task_type varchar(40)`<br>`target_id int`<br>`operator_id int`<br>`operator_name varchar(50)`<br>`operator_role varchar(20)`<br>`lock_token varchar(64)`<br>`claimed_at datetime`<br>`expires_at datetime`<br>`updated_at datetime` |
| `service_type_mapping` | 服务类型、基础价、小时价和启用状态。 | `id int`<br>`service_type_number int`<br>`service_type_name varchar(100)`<br>`description varchar(255)`<br>`price_base decimal(10,2)`<br>`price_per_hour decimal(10,2)`<br>`is_active tinyint(1)`<br>`create_time datetime`<br>`update_time datetime` |
| `guide_appointment` | AI/普通导诊预约需求，保存患者、医院、症状、服务时间等。 | `id int`<br>`appointment_no varchar(64)`<br>`user_id int`<br>`patient_name varchar(50)`<br>`patient_phone varchar(20)`<br>`symptoms json`<br>`hospital_name varchar(100)`<br>`service_type_number int`<br>`service_date varchar(20)`<br>`service_start_time varchar(20)`<br>`service_end_time varchar(20)`<br>`other_requirement text`<br>`create_time datetime` |
| `order` | 陪诊订单主表，保存用户、陪诊师、服务、支付、取消、争议、核销和结算字段；状态 5 表示平台争议处理中，9 表示待用户补差额。 | `order_id int`<br>`order_no varchar(64)`<br>`user_id int`<br>`attendant_id int`<br>`attendant_name varchar(50)`<br>`patient_name varchar(50)`<br>`patient_age int`<br>`patient_sex varchar(10)`<br>`contact_person varchar(50)`<br>`contact_phone varchar(20)`<br>`hospital varchar(100)`<br>`service_content varchar(100)`<br>`clinic_type int`<br>`service_date varchar(20)`<br>`service_time_slot varchar(50)`<br>`special_requirements text`<br>`custom_requirement text`<br>`order_amount decimal(10,2)`<br>`balance_amount decimal(10,2)`<br>`refund_amount decimal(10,2)`<br>`dispute_resolved_by int`<br>`dispute_resolved_time datetime`<br>... |
| `order_evaluation` | 订单评价和陪诊师回复。 | `id int`<br>`order_id int`<br>`order_no varchar(64)`<br>`user_id int`<br>`attendant_id int`<br>`rating int`<br>`tags varchar(255)`<br>`content text`<br>`attendant_reply text`<br>`reply_time datetime`<br>`create_time datetime`<br>`update_time datetime` |
| `chat_message` | 用户与陪诊师聊天/系统消息记录，可关联订单。 | `id bigint`<br>`sender_id int`<br>`receiver_id int`<br>`content text`<br>`order_id int`<br>`msg_type int`<br>`is_read tinyint(1)`<br>`create_time datetime` |
| `ai_medical_qa` | AI 医疗问答记录、会话、回答状态和思考过程。 | `id bigint`<br>`user_id int`<br>`conversation_id varchar(64)`<br>`question text`<br>`answer longtext`<br>`qa_status int`<br>`create_time datetime`<br>`update_time datetime`<br>`deleted tinyint(1)`<br>`thinking_process longtext` |
| `ai_appointment_session` | AI 预约导诊会话状态、结构化需求、匹配结果和预约号。 | `id bigint`<br>`session_id varchar(64)`<br>`user_id int`<br>`status varchar(32)`<br>`processing_phase varchar(32)`<br>`thinking_process varchar(255)`<br>`assistant_reply text`<br>`assistant_intent varchar(32)`<br>`need_more_info tinyint(1)`<br>`missing_fields_json json`<br>`question_type varchar(64)`<br>`question_key varchar(64)`<br>`follow_up_type varchar(64)`<br>`time_proposal_json json`<br>`options_json json`<br>`can_match tinyint(1)`<br>`ready_for_confirm tinyint(1)`<br>`follow_up_round int`<br>... |
| `ai_appointment_message` | AI 预约导诊会话消息明细和字段补丁。 | `id bigint`<br>`session_id varchar(64)`<br>`role varchar(16)`<br>`content text`<br>`field_patch_json json`<br>`deleted tinyint(1)`<br>`create_time datetime`<br>`update_time datetime` |

## 关键关系

- `user.id` 是普通用户和陪诊师登录主体；陪诊师扩展资料使用 `attendant.user_id`、`attendant_qualification.user_id` 和 `attendant_qualification_audit_log.user_id`。
- `third_party_account.principal_type + principal_id` 关联 `user.id` 或 `sys_admin.id`；微信身份按 `provider=WECHAT`、`platform`、`openid` 唯一，`unionid` 用于同一微信开放平台主体下跨 App/H5/网站扫码识别同一微信用户。
- `order.user_id` 关联下单用户，`order.attendant_id` 关联接单陪诊师，`order.guide_appointment_id` 关联导诊预约号/记录；后台订单取消和争议处理写入 `admin_operation_log`，工作台处理前用 `admin_task_claim` 对 `ORDER_DISPUTE` 任务加领取锁。
- 陪诊师钱包收入按后端统一结算口径读取：仅 `order_status = 6` 产生收入，取消/退款/超时关闭的 `order_status = 7` 不产生收入；新流程 `order_amount` 是最终结算金额，历史退款数据在没有负数 `balance_amount` 时按 `order_amount - refund_amount` 兼容后再扣 10% 平台服务费。
- 陪诊师待审核任务使用 `admin_task_claim` 对 `ATTENDANT_REVIEW` 加领取锁；审核结果只更新 `attendant.qualification_status` 并继续写入 `attendant_qualification_audit_log` 和 `admin_operation_log`；封禁/恢复账号只更新 `user.status`。
- `order_evaluation.order_id`、`order_evaluation.order_no` 关联订单评价；陪诊师评分、评价总数和好评率实时从 `order_evaluation.rating` 聚合，`rating >= 4` 算好评；`chat_message.order_id` 可将消息绑定到订单上下文。
- `ai_appointment_session.session_id` 与 `ai_appointment_message.session_id` 组成 AI 预约导诊会话和消息明细。
- `ai_medical_qa.conversation_id` 用于 AI 医疗问答会话聚合。

## 维护检查

- `database/` 保留历史增量和完整初始化基线；新数据库变更优先写入 `db/YYYYMMDD_descriptive_name.sql`。
- `SECRET_VAULT.md` 是唯一允许留在本地且不推送的密码/连接信息文件。
- 新增表时：补充用途、关键字段、与现有表关系。
- 新增列时：在对应表关键字段中补充；如果影响接口返回，也同步更新 `docs/BACKEND_MANIFEST.md`。
- 数据库变更影响前端页面展示或表单时，同步更新 `docs/FRONTEND_MANIFEST.md`。

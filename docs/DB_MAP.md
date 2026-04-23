# Database Map

更新时间: 2026-04-23

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
| `database/student.sql` | 当前完整建表和演示数据基线 |
| `db/.gitkeep` | 保留后续日期命名数据库脚本目录 |

## 表结构地图

| 表 | 用途 | 关键字段 |
| --- | --- | --- |
| `user` | 用户基础账号，区分普通用户和陪诊师登录身份。 | `id int`<br>`password varchar(100)`<br>`name varchar(50)`<br>`phone varchar(20)`<br>`sex varchar(10)`<br>`age int`<br>`avatar varchar(255)`<br>`user_type int`<br>`status tinyint`<br>`openid varchar(64)`<br>`create_time datetime` |
| `sys_admin` | 管理端管理员账号，独立于普通用户体系。 | `id int`<br>`name varchar(50)`<br>`phone varchar(20)`<br>`password varchar(100)`<br>`status tinyint`<br>`create_time datetime`<br>`update_time datetime`<br>`last_login_time datetime` |
| `attendant` | 陪诊师业务资料、状态、评分、医院和服务能力。 | `user_id int`<br>`certificate varchar(100)`<br>`status int`<br>`qualification_fail_reason varchar(255)`<br>`introduction text`<br>`professional_field varchar(255)`<br>`score decimal(2,1)`<br>`experience_years int`<br>`hospital_name varchar(100)`<br>`service_count int`<br>`create_time datetime`<br>`update_time datetime` |
| `attendant_qualification` | 陪诊师身份证、执业证、健康证等资质材料上传状态和文件地址。 | `user_id int`<br>`id_card_uploaded tinyint(1)`<br>`practice_cert_uploaded tinyint(1)`<br>`health_cert_uploaded tinyint(1)`<br>`id_card_file_url varchar(255)`<br>`id_card_front_file_url varchar(255)`<br>`id_card_back_file_url varchar(255)`<br>`practice_cert_file_url varchar(255)`<br>`health_cert_file_url varchar(255)`<br>`create_time datetime`<br>`update_time datetime` |
| `service_type_mapping` | 服务类型、基础价、小时价和启用状态。 | `id int`<br>`service_type_number int`<br>`service_type_name varchar(100)`<br>`description varchar(255)`<br>`price_base decimal(10,2)`<br>`price_per_hour decimal(10,2)`<br>`is_active tinyint(1)`<br>`create_time datetime`<br>`update_time datetime` |
| `guide_appointment` | AI/普通导诊预约需求，保存患者、医院、症状、服务时间等。 | `id int`<br>`appointment_no varchar(64)`<br>`user_id int`<br>`patient_name varchar(50)`<br>`patient_phone varchar(20)`<br>`symptoms json`<br>`hospital_name varchar(100)`<br>`service_type_number int`<br>`service_date varchar(20)`<br>`service_start_time varchar(20)`<br>`service_end_time varchar(20)`<br>`other_requirement text`<br>`create_time datetime` |
| `order` | 陪诊订单主表，保存用户、陪诊师、服务、支付、取消、争议、核销和结算字段。 | `order_id int`<br>`order_no varchar(64)`<br>`user_id int`<br>`attendant_id int`<br>`attendant_name varchar(50)`<br>`patient_name varchar(50)`<br>`patient_age int`<br>`patient_sex varchar(10)`<br>`contact_person varchar(50)`<br>`contact_phone varchar(20)`<br>`hospital varchar(100)`<br>`service_content varchar(100)`<br>`clinic_type int`<br>`service_date varchar(20)`<br>`service_time_slot varchar(50)`<br>`special_requirements text`<br>`custom_requirement text`<br>`order_amount decimal(10,2)`<br>... |
| `order_evaluation` | 订单评价和陪诊师回复。 | `id int`<br>`order_id int`<br>`order_no varchar(64)`<br>`user_id int`<br>`attendant_id int`<br>`rating int`<br>`tags varchar(255)`<br>`content text`<br>`attendant_reply text`<br>`reply_time datetime`<br>`create_time datetime`<br>`update_time datetime` |
| `chat_message` | 用户与陪诊师聊天/系统消息记录，可关联订单。 | `id bigint`<br>`sender_id int`<br>`receiver_id int`<br>`content text`<br>`order_id int`<br>`msg_type int`<br>`is_read tinyint(1)`<br>`create_time datetime` |
| `ai_medical_qa` | AI 医疗问答记录、会话、回答状态和思考过程。 | `id bigint`<br>`user_id int`<br>`conversation_id varchar(64)`<br>`question text`<br>`answer longtext`<br>`qa_status int`<br>`create_time datetime`<br>`update_time datetime`<br>`deleted tinyint(1)`<br>`thinking_process longtext` |
| `ai_appointment_session` | AI 预约导诊会话状态、结构化需求、匹配结果和预约号。 | `id bigint`<br>`session_id varchar(64)`<br>`user_id int`<br>`status varchar(32)`<br>`processing_phase varchar(32)`<br>`thinking_process varchar(255)`<br>`assistant_reply text`<br>`assistant_intent varchar(32)`<br>`need_more_info tinyint(1)`<br>`missing_fields_json json`<br>`question_type varchar(64)`<br>`question_key varchar(64)`<br>`follow_up_type varchar(64)`<br>`time_proposal_json json`<br>`options_json json`<br>`can_match tinyint(1)`<br>`ready_for_confirm tinyint(1)`<br>`follow_up_round int`<br>... |
| `ai_appointment_message` | AI 预约导诊会话消息明细和字段补丁。 | `id bigint`<br>`session_id varchar(64)`<br>`role varchar(16)`<br>`content text`<br>`field_patch_json json`<br>`deleted tinyint(1)`<br>`create_time datetime`<br>`update_time datetime` |

## 关键关系

- `user.id` 是普通用户和陪诊师登录主体；陪诊师扩展资料使用 `attendant.user_id` 和 `attendant_qualification.user_id`。
- `order.user_id` 关联下单用户，`order.attendant_id` 关联接单陪诊师，`order.guide_appointment_id` 关联导诊预约号/记录。
- `order_evaluation.order_id`、`order_evaluation.order_no` 关联订单评价；`chat_message.order_id` 可将消息绑定到订单上下文。
- `ai_appointment_session.session_id` 与 `ai_appointment_message.session_id` 组成 AI 预约导诊会话和消息明细。
- `ai_medical_qa.conversation_id` 用于 AI 医疗问答会话聚合。

## 维护检查

- 新增表时：补充用途、关键字段、与现有表关系。
- 新增列时：在对应表关键字段中补充；如果影响接口返回，也同步更新 `docs/BACKEND_MANIFEST.md`。
- 数据库变更影响前端页面展示或表单时，同步更新 `docs/FRONTEND_MANIFEST.md`。

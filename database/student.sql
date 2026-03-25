/*
 Demo database baseline rebuilt on 2026-03-20
*/
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;


DROP TABLE IF EXISTS `order_evaluation`;
DROP TABLE IF EXISTS `chat_message`;
DROP TABLE IF EXISTS `guide_appointment`;
DROP TABLE IF EXISTS `ai_medical_qa`;
DROP TABLE IF EXISTS `attendant_qualification`;
DROP TABLE IF EXISTS `attendant`;
DROP TABLE IF EXISTS `service_type_mapping`;
DROP TABLE IF EXISTS `user`;

CREATE TABLE `user` (
  `id` int NOT NULL AUTO_INCREMENT,
  `password` varchar(100) NOT NULL COMMENT '登录密码',
  `name` varchar(50) DEFAULT NULL COMMENT '真实姓名/昵称',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `sex` varchar(10) DEFAULT '未知' COMMENT '性别',
  `age` int DEFAULT '0' COMMENT '年龄',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像URL',
  `user_type` int NOT NULL DEFAULT '0' COMMENT '角色：0=普通用户,1=陪诊师,2=管理员',
  `openid` varchar(64) DEFAULT NULL COMMENT '微信OpenID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_phone` (`phone`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='用户表';

CREATE TABLE `attendant` (
  `user_id` int NOT NULL COMMENT '关联user表ID',
  `certificate` varchar(100) DEFAULT NULL COMMENT '资格证书编号',
  `status` int DEFAULT '1' COMMENT '状态：0=审核中,1=正常,2=封禁,3=审核失败',
  `qualification_fail_reason` varchar(255) DEFAULT NULL COMMENT '资质审核失败原因',
  `introduction` text COMMENT '个人简介',
  `professional_field` varchar(255) DEFAULT NULL COMMENT '擅长领域',
  `score` decimal(2,1) DEFAULT '5.0' COMMENT '评分',
  `experience_years` int DEFAULT '0' COMMENT '从业年限',
  `hospital_name` varchar(100) DEFAULT NULL COMMENT '常驻医院',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`) USING BTREE,
  CONSTRAINT `attendant_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='陪诊师扩展信息表';

CREATE TABLE `attendant_qualification` (
  `user_id` int NOT NULL COMMENT '关联user表ID',
  `id_card_uploaded` tinyint(1) DEFAULT '0' COMMENT '身份证是否上传：0=未上传,1=已上传',
  `practice_cert_uploaded` tinyint(1) DEFAULT '0' COMMENT '执业证书是否上传：0=未上传,1=已上传',
  `health_cert_uploaded` tinyint(1) DEFAULT '0' COMMENT '健康证是否上传：0=未上传,1=已上传',
  `id_card_file_url` varchar(255) DEFAULT NULL COMMENT '身份证文件地址',
  `id_card_front_file_url` varchar(255) DEFAULT NULL COMMENT '身份证正面文件地址',
  `id_card_back_file_url` varchar(255) DEFAULT NULL COMMENT '身份证背面文件地址',
  `practice_cert_file_url` varchar(255) DEFAULT NULL COMMENT '执业证书文件地址',
  `health_cert_file_url` varchar(255) DEFAULT NULL COMMENT '健康证文件地址',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`) USING BTREE,
  CONSTRAINT `attendant_qualification_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='陪诊师资质信息表';

CREATE TABLE `service_type_mapping` (
  `id` int NOT NULL AUTO_INCREMENT,
  `service_type_number` int NOT NULL,
  `service_type_name` varchar(100) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `price_base` decimal(10,2) NOT NULL DEFAULT '0.00',
  `price_per_hour` decimal(10,2) NOT NULL DEFAULT '0.00',
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_service_type_number` (`service_type_number`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='服务类型映射表';

CREATE TABLE `order` (
  `order_id` int NOT NULL AUTO_INCREMENT,
  `order_no` varchar(64) NOT NULL COMMENT '订单号',
  `user_id` int NOT NULL COMMENT '下单用户ID',
  `attendant_id` int DEFAULT NULL COMMENT '接单陪诊师ID',
  `attendant_name` varchar(50) DEFAULT NULL COMMENT '陪诊师姓名',
  `attendant_phone` varchar(20) DEFAULT NULL COMMENT '陪诊师电话',
  `patient_name` varchar(50) NOT NULL COMMENT '就诊人姓名',
  `patient_age` int DEFAULT '0' COMMENT '就诊人年龄',
  `patient_sex` varchar(10) DEFAULT '未知' COMMENT '就诊人性别',
  `contact_person` varchar(50) NOT NULL,
  `contact_phone` varchar(20) NOT NULL,
  `hospital` varchar(100) NOT NULL,
  `service_content` varchar(100) DEFAULT NULL,
  `clinic_type` int DEFAULT '1',
  `service_date` varchar(20) DEFAULT NULL,
  `service_time_slot` varchar(50) DEFAULT NULL,
  `special_requirements` text COMMENT '特殊需求',
  `custom_requirement` text COMMENT '自定义需求',
  `order_amount` decimal(10,2) NOT NULL DEFAULT '0.00',
  `payment_status` int DEFAULT '0' COMMENT '0=待支付,1=已支付',
  `payment_time` datetime DEFAULT NULL,
  `order_status` int DEFAULT '0' COMMENT '0=待支付,1=待接单,2=待服务,3=服务中,4=待确认时长费用,5=时长费用有争议,6=已完成,7=已取消',
  `cancel_reason` text COMMENT '取消原因',
  `cancel_time` datetime DEFAULT NULL COMMENT '取消时间',
  `cancel_by` tinyint DEFAULT NULL COMMENT '取消方：0用户 1陪诊师 2系统',
  `penalty_rate` decimal(5,2) DEFAULT NULL COMMENT '违约金比例(0-1)',
  `penalty_amount` decimal(10,2) DEFAULT NULL COMMENT '违约金金额',
  `refund_amount` decimal(10,2) DEFAULT NULL COMMENT '退款金额',
  `qr_code_url` varchar(255) DEFAULT NULL COMMENT '核销二维码URL',
  `accept_time` datetime DEFAULT NULL COMMENT '接单时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `service_start_time` datetime DEFAULT NULL COMMENT '服务开始时间',
  `service_end_time` datetime DEFAULT NULL COMMENT '服务结束时间',
  `service_progress_step` tinyint DEFAULT NULL COMMENT '服务进度：1=已到院,2=候诊中,3=检查中,4=就诊完成',
  `estimated_duration` decimal(5,2) DEFAULT NULL COMMENT '预估服务时长(小时)',
  `actual_duration` decimal(5,2) DEFAULT NULL COMMENT '实际服务时长(小时)',
  `balance_amount` decimal(10,2) DEFAULT NULL COMMENT '差价金额（正数需补付，负数自动退款）',
  `time_dispute_user_duration` decimal(5,2) DEFAULT NULL COMMENT '用户申诉的实际时长(小时)',
  `time_dispute_reason` text COMMENT '用户申诉说明',
  PRIMARY KEY (`order_id`) USING BTREE,
  UNIQUE KEY `uk_order_no` (`order_no`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='订单表';

CREATE TABLE `order_evaluation` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_id` int NOT NULL COMMENT '订单ID',
  `order_no` varchar(64) NOT NULL COMMENT '订单号',
  `user_id` int NOT NULL COMMENT '评价用户ID',
  `attendant_id` int DEFAULT NULL COMMENT '陪诊师用户ID',
  `rating` int NOT NULL COMMENT '总体评分（1-5星）',
  `tags` varchar(255) DEFAULT NULL COMMENT '服务亮点标签，逗号分隔',
  `content` text COMMENT '评价内容',
  `attendant_reply` text COMMENT '陪诊师回复内容',
  `reply_time` datetime DEFAULT NULL COMMENT '陪诊师回复时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_order_id` (`order_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='订单评价表';

CREATE TABLE `chat_message` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `sender_id` int NOT NULL COMMENT '发送者ID',
  `receiver_id` int NOT NULL COMMENT '接收者ID',
  `content` text COMMENT '消息内容',
  `msg_type` int DEFAULT '1' COMMENT '消息类型：1=文本, 2=图片',
  `is_read` tinyint(1) DEFAULT '0' COMMENT '是否已读：0=未读,1=已读',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_sender_receiver` (`sender_id`,`receiver_id`) USING BTREE,
  KEY `idx_receiver_read` (`receiver_id`,`is_read`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='聊天消息表';

CREATE TABLE `guide_appointment` (
  `id` int NOT NULL AUTO_INCREMENT,
  `appointment_no` varchar(64) NOT NULL COMMENT '预约编号',
  `user_id` int DEFAULT NULL COMMENT '提交人ID',
  `patient_name` varchar(50) DEFAULT NULL COMMENT '就诊人',
  `patient_phone` varchar(20) DEFAULT NULL,
  `symptoms` json DEFAULT NULL COMMENT '症状描述',
  `hospital_name` varchar(100) DEFAULT NULL,
  `service_type_number` int DEFAULT '1' COMMENT '服务类型编号',
  `service_date` varchar(20) DEFAULT NULL,
  `service_start_time` varchar(20) DEFAULT NULL,
  `service_end_time` varchar(20) DEFAULT NULL,
  `other_requirement` text COMMENT '其他需求',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_appt_no` (`appointment_no`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='AI导诊预约记录表';

CREATE TABLE `ai_medical_qa` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `conversation_id` varchar(64) NOT NULL COMMENT '对话会话ID',
  `question` text NOT NULL COMMENT '用户提问内容',
  `answer` text COMMENT 'AI回答内容',
  `qa_status` tinyint NOT NULL DEFAULT '0' COMMENT '问答状态：0-处理中，1-完成，2-失败',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
  `thinking_process` varchar(500) DEFAULT '' COMMENT 'AI思考过程',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_conversation_id` (`conversation_id`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='AI医疗问答记录表';

-- Data for user
BEGIN;
INSERT INTO `user` (`id`, `password`, `name`, `phone`, `sex`, `age`, `avatar`, `user_type`, `openid`, `create_time`) VALUES (1, '$2b$10$irw4AvTW.r0OgNJC62XF.efa1RlSSA/kBTZYd0hXx3bv1t4I5dJuy', '平台管理员', '18800000001', '男', 38, '/uploads/brand-logo.png', 2, NULL, '2026-03-01 09:00:00');
INSERT INTO `user` (`id`, `password`, `name`, `phone`, `sex`, `age`, `avatar`, `user_type`, `openid`, `create_time`) VALUES (15, '$2b$10$1fVqCCDaY4nWsrwW2zlnneDk2B1cgNN94lXsx4.O1HBug/87UppJm', '范涵伶', '18600010001', '女', 26, '/uploads/user1.jpg', 0, 'wx_demo_user_18600010001', '2026-03-02 10:10:00');
INSERT INTO `user` (`id`, `password`, `name`, `phone`, `sex`, `age`, `avatar`, `user_type`, `openid`, `create_time`) VALUES (16, '$2b$10$1fVqCCDaY4nWsrwW2zlnneDk2B1cgNN94lXsx4.O1HBug/87UppJm', '柳清禾', '13800138016', '女', 31, '/uploads/user-avatar.jpg', 0, 'wx_demo_user_13800138016', '2026-03-02 10:20:00');
INSERT INTO `user` (`id`, `password`, `name`, `phone`, `sex`, `age`, `avatar`, `user_type`, `openid`, `create_time`) VALUES (17, '$2b$10$1fVqCCDaY4nWsrwW2zlnneDk2B1cgNN94lXsx4.O1HBug/87UppJm', '周铭', '13800138017', '男', 35, '/uploads/user3.jpg', 0, NULL, '2026-03-02 10:30:00');
INSERT INTO `user` (`id`, `password`, `name`, `phone`, `sex`, `age`, `avatar`, `user_type`, `openid`, `create_time`) VALUES (18, '$2b$10$1fVqCCDaY4nWsrwW2zlnneDk2B1cgNN94lXsx4.O1HBug/87UppJm', '陈雨桐', '13800138018', '女', 29, '/uploads/user2.jpg', 0, NULL, '2026-03-02 10:40:00');
INSERT INTO `user` (`id`, `password`, `name`, `phone`, `sex`, `age`, `avatar`, `user_type`, `openid`, `create_time`) VALUES (19, '$2b$10$1fVqCCDaY4nWsrwW2zlnneDk2B1cgNN94lXsx4.O1HBug/87UppJm', '宋嘉宁', '13800138019', '女', 42, '/uploads/user2.jpg', 0, NULL, '2026-03-02 10:50:00');
INSERT INTO `user` (`id`, `password`, `name`, `phone`, `sex`, `age`, `avatar`, `user_type`, `openid`, `create_time`) VALUES (20, '$2b$10$1fVqCCDaY4nWsrwW2zlnneDk2B1cgNN94lXsx4.O1HBug/87UppJm', '何子安', '13800138020', '男', 33, '/uploads/user3.jpg', 0, NULL, '2026-03-02 11:00:00');
INSERT INTO `user` (`id`, `password`, `name`, `phone`, `sex`, `age`, `avatar`, `user_type`, `openid`, `create_time`) VALUES (21, '$2b$10$1fVqCCDaY4nWsrwW2zlnneDk2B1cgNN94lXsx4.O1HBug/87UppJm', '肖阳', '18600000001', '男', 34, '/uploads/03_Medicalcompanion.jpg', 1, 'wx_demo_attendant_18600000001', '2026-03-01 11:00:00');
INSERT INTO `user` (`id`, `password`, `name`, `phone`, `sex`, `age`, `avatar`, `user_type`, `openid`, `create_time`) VALUES (22, '$2b$10$1fVqCCDaY4nWsrwW2zlnneDk2B1cgNN94lXsx4.O1HBug/87UppJm', '林知夏', '13900139022', '女', 30, '/uploads/05_Medicalcompanion.jpg', 1, NULL, '2026-03-01 11:10:00');
INSERT INTO `user` (`id`, `password`, `name`, `phone`, `sex`, `age`, `avatar`, `user_type`, `openid`, `create_time`) VALUES (23, '$2b$10$1fVqCCDaY4nWsrwW2zlnneDk2B1cgNN94lXsx4.O1HBug/87UppJm', '周可宁', '13900139023', '女', 32, '/uploads/06_Medicalcompanion.jpg', 1, NULL, '2026-03-01 11:20:00');
INSERT INTO `user` (`id`, `password`, `name`, `phone`, `sex`, `age`, `avatar`, `user_type`, `openid`, `create_time`) VALUES (24, '$2b$10$1fVqCCDaY4nWsrwW2zlnneDk2B1cgNN94lXsx4.O1HBug/87UppJm', '唐思雨', '13900139024', '女', 29, '/uploads/02_Medicalcompanion.jpg', 1, NULL, '2026-03-01 11:30:00');
INSERT INTO `user` (`id`, `password`, `name`, `phone`, `sex`, `age`, `avatar`, `user_type`, `openid`, `create_time`) VALUES (25, '$2b$10$1fVqCCDaY4nWsrwW2zlnneDk2B1cgNN94lXsx4.O1HBug/87UppJm', '许嘉辰', '13900139025', '男', 27, '/uploads/04_Medicalcompanion.jpg', 1, NULL, '2026-03-01 11:40:00');
INSERT INTO `user` (`id`, `password`, `name`, `phone`, `sex`, `age`, `avatar`, `user_type`, `openid`, `create_time`) VALUES (26, '$2b$10$1fVqCCDaY4nWsrwW2zlnneDk2B1cgNN94lXsx4.O1HBug/87UppJm', '顾念安', '13900139026', '女', 36, '/uploads/01_Medicalcompanion.jpg', 1, NULL, '2026-03-01 11:50:00');
COMMIT;

-- Data for attendant
BEGIN;
INSERT INTO `attendant` (`user_id`, `certificate`, `status`, `qualification_fail_reason`, `introduction`, `professional_field`, `score`, `experience_years`, `hospital_name`, `create_time`, `update_time`) VALUES (21, 'CD-PZ-2026-021', 1, NULL, '熟悉华西医院、四川省人民医院就诊流程，擅长普通陪诊与急诊陪同，处理临时加号和院内协调经验丰富。', '普通陪诊,急诊陪同,老年陪诊', '4.9', 8, '四川大学华西医院', '2026-03-01 11:00:00', '2026-03-20 09:00:00');
INSERT INTO `attendant` (`user_id`, `certificate`, `status`, `qualification_fail_reason`, `introduction`, `professional_field`, `score`, `experience_years`, `hospital_name`, `create_time`, `update_time`) VALUES (22, 'CD-PZ-2026-022', 1, NULL, '擅长术后护理与普通陪诊，熟悉复查拆线、报告领取和女性患者全流程照护，沟通细致耐心。', '术后护理,普通陪诊,女性陪诊', '4.8', 6, '四川省人民医院', '2026-03-01 11:10:00', '2026-03-20 09:05:00');
INSERT INTO `attendant` (`user_id`, `certificate`, `status`, `qualification_fail_reason`, `introduction`, `professional_field`, `score`, `experience_years`, `hospital_name`, `create_time`, `update_time`) VALUES (23, 'CD-PZ-2026-023', 1, NULL, '熟悉儿童医院与综合医院接送流程，擅长普通陪诊与上门陪诊，沟通耐心细致。', '普通陪诊,上门陪诊,儿科陪诊', '4.7', 5, '成都市妇女儿童中心医院', '2026-03-01 11:20:00', '2026-03-20 09:10:00');
INSERT INTO `attendant` (`user_id`, `certificate`, `status`, `qualification_fail_reason`, `introduction`, `professional_field`, `score`, `experience_years`, `hospital_name`, `create_time`, `update_time`) VALUES (24, 'CD-PZ-2026-024', 1, NULL, '擅长上门陪诊与术后护理，能够协助住院前准备、返院复查和出院后随访。', '上门陪诊,术后护理,术后复查', '4.6', 4, '成都市第三人民医院', '2026-03-01 11:30:00', '2026-03-20 09:15:00');
INSERT INTO `attendant` (`user_id`, `certificate`, `status`, `qualification_fail_reason`, `introduction`, `professional_field`, `score`, `experience_years`, `hospital_name`, `create_time`, `update_time`) VALUES (25, 'CD-PZ-2026-025', 0, NULL, '正在完善资质材料，已具备普通陪诊与上门陪诊经验。', '普通陪诊,上门陪诊', '4.5', 2, '成都中医药大学附属医院', '2026-03-01 11:40:00', '2026-03-18 14:00:00');
INSERT INTO `attendant` (`user_id`, `certificate`, `status`, `qualification_fail_reason`, `introduction`, `professional_field`, `score`, `experience_years`, `hospital_name`, `create_time`, `update_time`) VALUES (26, 'CD-PZ-2026-026', 3, '健康证已过期，请更新后重新提交。', '擅长术后护理与急诊陪同，待补齐资质后重新开放接单。', '术后护理,急诊陪同', '4.4', 7, '成都市第一人民医院', '2026-03-01 11:50:00', '2026-03-17 18:00:00');
COMMIT;

-- Data for attendant_qualification
BEGIN;
INSERT INTO `attendant_qualification` (`user_id`, `id_card_uploaded`, `practice_cert_uploaded`, `health_cert_uploaded`, `id_card_file_url`, `id_card_front_file_url`, `id_card_back_file_url`, `practice_cert_file_url`, `health_cert_file_url`, `create_time`, `update_time`) VALUES (21, 1, 1, 1, '/uploads/qualification/idcard-front.jpg', '/uploads/qualification/idcard-front.jpg', '/uploads/qualification/idcard-back.jpg', '/uploads/qualification/practice-cert.jpg', '/uploads/qualification/health-cert.jpg', '2026-03-01 12:00:00', '2026-03-20 09:30:00');
INSERT INTO `attendant_qualification` (`user_id`, `id_card_uploaded`, `practice_cert_uploaded`, `health_cert_uploaded`, `id_card_file_url`, `id_card_front_file_url`, `id_card_back_file_url`, `practice_cert_file_url`, `health_cert_file_url`, `create_time`, `update_time`) VALUES (22, 1, 1, 1, '/uploads/qualification/idcard-front.jpg', '/uploads/qualification/idcard-front.jpg', '/uploads/qualification/idcard-back.jpg', '/uploads/qualification/practice-cert.jpg', '/uploads/qualification/health-cert.jpg', '2026-03-01 12:00:00', '2026-03-20 09:30:00');
INSERT INTO `attendant_qualification` (`user_id`, `id_card_uploaded`, `practice_cert_uploaded`, `health_cert_uploaded`, `id_card_file_url`, `id_card_front_file_url`, `id_card_back_file_url`, `practice_cert_file_url`, `health_cert_file_url`, `create_time`, `update_time`) VALUES (23, 1, 1, 1, '/uploads/qualification/idcard-front.jpg', '/uploads/qualification/idcard-front.jpg', '/uploads/qualification/idcard-back.jpg', '/uploads/qualification/practice-cert.jpg', '/uploads/qualification/health-cert.jpg', '2026-03-01 12:00:00', '2026-03-20 09:30:00');
INSERT INTO `attendant_qualification` (`user_id`, `id_card_uploaded`, `practice_cert_uploaded`, `health_cert_uploaded`, `id_card_file_url`, `id_card_front_file_url`, `id_card_back_file_url`, `practice_cert_file_url`, `health_cert_file_url`, `create_time`, `update_time`) VALUES (24, 1, 1, 1, '/uploads/qualification/idcard-front.jpg', '/uploads/qualification/idcard-front.jpg', '/uploads/qualification/idcard-back.jpg', '/uploads/qualification/practice-cert.jpg', '/uploads/qualification/health-cert.jpg', '2026-03-01 12:00:00', '2026-03-20 09:30:00');
INSERT INTO `attendant_qualification` (`user_id`, `id_card_uploaded`, `practice_cert_uploaded`, `health_cert_uploaded`, `id_card_file_url`, `id_card_front_file_url`, `id_card_back_file_url`, `practice_cert_file_url`, `health_cert_file_url`, `create_time`, `update_time`) VALUES (25, 1, 1, 0, '/uploads/qualification/idcard-front.jpg', '/uploads/qualification/idcard-front.jpg', '/uploads/qualification/idcard-back.jpg', '/uploads/qualification/practice-cert.jpg', NULL, '2026-03-01 12:00:00', '2026-03-20 09:30:00');
INSERT INTO `attendant_qualification` (`user_id`, `id_card_uploaded`, `practice_cert_uploaded`, `health_cert_uploaded`, `id_card_file_url`, `id_card_front_file_url`, `id_card_back_file_url`, `practice_cert_file_url`, `health_cert_file_url`, `create_time`, `update_time`) VALUES (26, 1, 1, 1, '/uploads/qualification/idcard-front.jpg', '/uploads/qualification/idcard-front.jpg', '/uploads/qualification/idcard-back.jpg', '/uploads/qualification/practice-cert.jpg', '/uploads/qualification/health-cert.jpg', '2026-03-01 12:00:00', '2026-03-20 09:30:00');
COMMIT;

-- Data for service_type_mapping
BEGIN;
INSERT INTO `service_type_mapping` (`id`, `service_type_number`, `service_type_name`, `description`, `price_base`, `price_per_hour`, `is_active`, `create_time`, `update_time`) VALUES (1, 1, '普通陪诊', '基础普通陪诊服务，适用于门诊就诊、问诊记录、缴费取药等场景。', '50.00', '30.00', 1, '2026-03-01 08:00:00', '2026-03-01 08:00:00');
INSERT INTO `service_type_mapping` (`id`, `service_type_number`, `service_type_name`, `description`, `price_base`, `price_per_hour`, `is_active`, `create_time`, `update_time`) VALUES (2, 2, '术后护理', '术后护理服务，适用于术后复查、换药、拆线、恢复期陪护等场景。', '45.00', '45.00', 1, '2026-03-01 08:00:00', '2026-03-01 08:00:00');
INSERT INTO `service_type_mapping` (`id`, `service_type_number`, `service_type_name`, `description`, `price_base`, `price_per_hour`, `is_active`, `create_time`, `update_time`) VALUES (3, 3, '急诊陪同', '急诊陪同服务，适用于突发不适、急诊分诊、缴费检查与快速协调。', '150.00', '30.00', 1, '2026-03-01 08:00:00', '2026-03-01 08:00:00');
INSERT INTO `service_type_mapping` (`id`, `service_type_number`, `service_type_name`, `description`, `price_base`, `price_per_hour`, `is_active`, `create_time`, `update_time`) VALUES (4, 4, '上门陪诊', '上门陪诊服务，适用于从家中接送患者到院、返程陪同及行动不便场景。', '80.00', '30.00', 1, '2026-03-01 08:00:00', '2026-03-01 08:00:00');
COMMIT;

-- Data for order
BEGIN;
INSERT INTO `order` (`order_id`, `order_no`, `user_id`, `attendant_id`, `attendant_name`, `attendant_phone`, `patient_name`, `patient_age`, `patient_sex`, `contact_person`, `contact_phone`, `hospital`, `service_content`, `clinic_type`, `service_date`, `service_time_slot`, `special_requirements`, `custom_requirement`, `order_amount`, `payment_status`, `payment_time`, `order_status`, `cancel_reason`, `cancel_time`, `cancel_by`, `penalty_rate`, `penalty_amount`, `refund_amount`, `qr_code_url`, `create_time`, `service_start_time`, `service_end_time`, `service_progress_step`, `estimated_duration`, `actual_duration`, `balance_amount`, `time_dispute_user_duration`, `time_dispute_reason`) VALUES (1001, 'ORD20260310001', 15, 21, '肖阳', '18600000001', '范涵伶', 26, '女', '范涵伶', '18600010001', '四川大学华西医院', '普通陪诊', 1, '2026-03-10', '08:30-11:30', '父亲复诊，需要陪同排队取号。', '携带既往CT报告', '185.00', 1, '2026-03-10 07:30:00', 6, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2026-03-10 07:00:00', '2026-03-10 08:25:00', '2026-03-10 11:40:00', 4, '3.00', '3.20', '12.00', NULL, NULL);
INSERT INTO `order` (`order_id`, `order_no`, `user_id`, `attendant_id`, `attendant_name`, `attendant_phone`, `patient_name`, `patient_age`, `patient_sex`, `contact_person`, `contact_phone`, `hospital`, `service_content`, `clinic_type`, `service_date`, `service_time_slot`, `special_requirements`, `custom_requirement`, `order_amount`, `payment_status`, `payment_time`, `order_status`, `cancel_reason`, `cancel_time`, `cancel_by`, `penalty_rate`, `penalty_amount`, `refund_amount`, `qr_code_url`, `create_time`, `service_start_time`, `service_end_time`, `service_progress_step`, `estimated_duration`, `actual_duration`, `balance_amount`, `time_dispute_user_duration`, `time_dispute_reason`) VALUES (1002, 'ORD20260310002', 16, 21, '肖阳', '18600000001', '柳清禾', 31, '女', '柳清禾', '13800138016', '四川大学华西医院', '术后护理', 2, '2026-03-10', '13:00-16:00', '胃镜检查后需要有人陪同观察。', '检查结束后协助取药回休息区', '220.00', 1, '2026-03-10 07:30:00', 6, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2026-03-10 07:00:00', '2026-03-10 12:55:00', '2026-03-10 16:10:00', 4, '3.00', '3.10', '0.00', NULL, NULL);
INSERT INTO `order` (`order_id`, `order_no`, `user_id`, `attendant_id`, `attendant_name`, `attendant_phone`, `patient_name`, `patient_age`, `patient_sex`, `contact_person`, `contact_phone`, `hospital`, `service_content`, `clinic_type`, `service_date`, `service_time_slot`, `special_requirements`, `custom_requirement`, `order_amount`, `payment_status`, `payment_time`, `order_status`, `cancel_reason`, `cancel_time`, `cancel_by`, `penalty_rate`, `penalty_amount`, `refund_amount`, `qr_code_url`, `create_time`, `service_start_time`, `service_end_time`, `service_progress_step`, `estimated_duration`, `actual_duration`, `balance_amount`, `time_dispute_user_duration`, `time_dispute_reason`) VALUES (1003, 'ORD20260311001', 17, 21, '肖阳', '18600000001', '周铭', 35, '男', '周铭', '13800138017', '四川省人民医院', '普通陪诊', 1, '2026-03-11', '09:00-12:00', '高血压复诊，希望协助问诊记录。', '需要帮助整理用药清单', '190.00', 1, '2026-03-11 07:30:00', 6, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2026-03-11 07:00:00', '2026-03-11 08:50:00', '2026-03-11 12:05:00', 4, '3.00', '3.00', '5.00', NULL, NULL);
INSERT INTO `order` (`order_id`, `order_no`, `user_id`, `attendant_id`, `attendant_name`, `attendant_phone`, `patient_name`, `patient_age`, `patient_sex`, `contact_person`, `contact_phone`, `hospital`, `service_content`, `clinic_type`, `service_date`, `service_time_slot`, `special_requirements`, `custom_requirement`, `order_amount`, `payment_status`, `payment_time`, `order_status`, `cancel_reason`, `cancel_time`, `cancel_by`, `penalty_rate`, `penalty_amount`, `refund_amount`, `qr_code_url`, `create_time`, `service_start_time`, `service_end_time`, `service_progress_step`, `estimated_duration`, `actual_duration`, `balance_amount`, `time_dispute_user_duration`, `time_dispute_reason`) VALUES (1004, 'ORD20260312001', 18, 21, '肖阳', '18600000001', '陈雨桐', 29, '女', '陈雨桐', '13800138018', '四川大学华西医院', '急诊陪同', 3, '2026-03-12', '10:00-15:00', '母亲突发腹痛，需要尽快就诊。', '协助完成急诊分诊和缴费', '320.00', 1, '2026-03-12 07:30:00', 6, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2026-03-12 07:00:00', '2026-03-12 09:45:00', '2026-03-12 15:10:00', 4, '5.00', '5.20', '25.00', NULL, NULL);
INSERT INTO `order` (`order_id`, `order_no`, `user_id`, `attendant_id`, `attendant_name`, `attendant_phone`, `patient_name`, `patient_age`, `patient_sex`, `contact_person`, `contact_phone`, `hospital`, `service_content`, `clinic_type`, `service_date`, `service_time_slot`, `special_requirements`, `custom_requirement`, `order_amount`, `payment_status`, `payment_time`, `order_status`, `cancel_reason`, `cancel_time`, `cancel_by`, `penalty_rate`, `penalty_amount`, `refund_amount`, `qr_code_url`, `create_time`, `service_start_time`, `service_end_time`, `service_progress_step`, `estimated_duration`, `actual_duration`, `balance_amount`, `time_dispute_user_duration`, `time_dispute_reason`) VALUES (1005, 'ORD20260313001', 19, 21, '肖阳', '18600000001', '宋嘉宁', 42, '女', '宋嘉宁', '13800138019', '成都市第三人民医院', '上门陪诊', 4, '2026-03-13', '08:00-12:00', '长辈行动不便，希望从住处接到医院复诊。', '需协助打印发票并陪同返程', '260.00', 1, '2026-03-13 07:30:00', 6, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2026-03-13 07:00:00', '2026-03-13 07:55:00', '2026-03-13 12:00:00', 4, '4.00', '4.00', '0.00', NULL, NULL);
INSERT INTO `order` (`order_id`, `order_no`, `user_id`, `attendant_id`, `attendant_name`, `attendant_phone`, `patient_name`, `patient_age`, `patient_sex`, `contact_person`, `contact_phone`, `hospital`, `service_content`, `clinic_type`, `service_date`, `service_time_slot`, `special_requirements`, `custom_requirement`, `order_amount`, `payment_status`, `payment_time`, `order_status`, `cancel_reason`, `cancel_time`, `cancel_by`, `penalty_rate`, `penalty_amount`, `refund_amount`, `qr_code_url`, `create_time`, `service_start_time`, `service_end_time`, `service_progress_step`, `estimated_duration`, `actual_duration`, `balance_amount`, `time_dispute_user_duration`, `time_dispute_reason`) VALUES (1006, 'ORD20260314001', 20, 22, '林知夏', '13900139022', '何子安', 33, '男', '何子安', '13800138020', '四川省人民医院', '术后护理', 2, '2026-03-14', '09:00-11:30', '术后拆线复查，需要协助轮椅转运。', '行动缓慢，需要陪同轮椅', '180.00', 1, '2026-03-14 07:30:00', 6, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2026-03-14 07:00:00', '2026-03-14 08:50:00', '2026-03-14 11:35:00', 4, '2.50', '2.80', '10.00', NULL, NULL);
INSERT INTO `order` (`order_id`, `order_no`, `user_id`, `attendant_id`, `attendant_name`, `attendant_phone`, `patient_name`, `patient_age`, `patient_sex`, `contact_person`, `contact_phone`, `hospital`, `service_content`, `clinic_type`, `service_date`, `service_time_slot`, `special_requirements`, `custom_requirement`, `order_amount`, `payment_status`, `payment_time`, `order_status`, `cancel_reason`, `cancel_time`, `cancel_by`, `penalty_rate`, `penalty_amount`, `refund_amount`, `qr_code_url`, `create_time`, `service_start_time`, `service_end_time`, `service_progress_step`, `estimated_duration`, `actual_duration`, `balance_amount`, `time_dispute_user_duration`, `time_dispute_reason`) VALUES (1007, 'ORD20260314002', 15, 22, '林知夏', '13900139022', '范涵伶', 26, '女', '范涵伶', '18600010001', '成都中医药大学附属医院', '普通陪诊', 1, '2026-03-14', '14:00-17:00', '乳腺复查，希望女性陪诊师陪同。', '希望协助候诊与取号', '210.00', 1, '2026-03-14 07:30:00', 6, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2026-03-14 07:00:00', '2026-03-14 13:55:00', '2026-03-14 17:10:00', 4, '3.00', '3.10', '8.00', NULL, NULL);
INSERT INTO `order` (`order_id`, `order_no`, `user_id`, `attendant_id`, `attendant_name`, `attendant_phone`, `patient_name`, `patient_age`, `patient_sex`, `contact_person`, `contact_phone`, `hospital`, `service_content`, `clinic_type`, `service_date`, `service_time_slot`, `special_requirements`, `custom_requirement`, `order_amount`, `payment_status`, `payment_time`, `order_status`, `cancel_reason`, `cancel_time`, `cancel_by`, `penalty_rate`, `penalty_amount`, `refund_amount`, `qr_code_url`, `create_time`, `service_start_time`, `service_end_time`, `service_progress_step`, `estimated_duration`, `actual_duration`, `balance_amount`, `time_dispute_user_duration`, `time_dispute_reason`) VALUES (1008, 'ORD20260315001', 16, 22, '林知夏', '13900139022', '柳清禾', 31, '女', '柳清禾', '13800138016', '四川省人民医院', '术后护理', 2, '2026-03-15', '08:30-10:30', '术后复查，需要陪同复诊和拿药。', '需要提醒术后禁忌和复诊流程', '170.00', 1, '2026-03-15 07:30:00', 6, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2026-03-15 07:00:00', '2026-03-15 08:25:00', '2026-03-15 10:40:00', 4, '2.00', '2.20', '5.00', NULL, NULL);
INSERT INTO `order` (`order_id`, `order_no`, `user_id`, `attendant_id`, `attendant_name`, `attendant_phone`, `patient_name`, `patient_age`, `patient_sex`, `contact_person`, `contact_phone`, `hospital`, `service_content`, `clinic_type`, `service_date`, `service_time_slot`, `special_requirements`, `custom_requirement`, `order_amount`, `payment_status`, `payment_time`, `order_status`, `cancel_reason`, `cancel_time`, `cancel_by`, `penalty_rate`, `penalty_amount`, `refund_amount`, `qr_code_url`, `create_time`, `service_start_time`, `service_end_time`, `service_progress_step`, `estimated_duration`, `actual_duration`, `balance_amount`, `time_dispute_user_duration`, `time_dispute_reason`) VALUES (1009, 'ORD20260315002', 17, 23, '周可宁', '13900139023', '周铭', 35, '男', '周铭', '13800138017', '成都市妇女儿童中心医院', '急诊陪同', 3, '2026-03-15', '13:30-16:30', '孩子高热反复，需要尽快到院就诊。', '儿科急诊流程需要熟悉', '230.00', 1, '2026-03-15 07:30:00', 6, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2026-03-15 07:00:00', '2026-03-15 13:20:00', '2026-03-15 16:40:00', 4, '3.00', '3.30', '15.00', NULL, NULL);
INSERT INTO `order` (`order_id`, `order_no`, `user_id`, `attendant_id`, `attendant_name`, `attendant_phone`, `patient_name`, `patient_age`, `patient_sex`, `contact_person`, `contact_phone`, `hospital`, `service_content`, `clinic_type`, `service_date`, `service_time_slot`, `special_requirements`, `custom_requirement`, `order_amount`, `payment_status`, `payment_time`, `order_status`, `cancel_reason`, `cancel_time`, `cancel_by`, `penalty_rate`, `penalty_amount`, `refund_amount`, `qr_code_url`, `create_time`, `service_start_time`, `service_end_time`, `service_progress_step`, `estimated_duration`, `actual_duration`, `balance_amount`, `time_dispute_user_duration`, `time_dispute_reason`) VALUES (1010, 'ORD20260316001', 18, 23, '周可宁', '13900139023', '陈雨桐', 29, '女', '陈雨桐', '13800138018', '成都市妇女儿童中心医院', '上门陪诊', 4, '2026-03-16', '09:00-11:00', '孩子就诊当天家属抽不开身，需要上门接送陪同。', '孩子比较怕生', '165.00', 1, '2026-03-16 07:30:00', 6, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2026-03-16 07:00:00', '2026-03-16 08:55:00', '2026-03-16 11:10:00', 4, '2.00', '2.10', '0.00', NULL, NULL);
INSERT INTO `order` (`order_id`, `order_no`, `user_id`, `attendant_id`, `attendant_name`, `attendant_phone`, `patient_name`, `patient_age`, `patient_sex`, `contact_person`, `contact_phone`, `hospital`, `service_content`, `clinic_type`, `service_date`, `service_time_slot`, `special_requirements`, `custom_requirement`, `order_amount`, `payment_status`, `payment_time`, `order_status`, `cancel_reason`, `cancel_time`, `cancel_by`, `penalty_rate`, `penalty_amount`, `refund_amount`, `qr_code_url`, `create_time`, `service_start_time`, `service_end_time`, `service_progress_step`, `estimated_duration`, `actual_duration`, `balance_amount`, `time_dispute_user_duration`, `time_dispute_reason`) VALUES (1011, 'ORD20260316002', 19, 24, '唐思雨', '13900139024', '宋嘉宁', 42, '女', '宋嘉宁', '13800138019', '成都市第三人民医院', '上门陪诊', 4, '2026-03-16', '10:00-14:00', '家属白天无法陪同，需要从家到院全程协助。', '需提醒术前禁食', '280.00', 1, '2026-03-16 07:30:00', 6, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2026-03-16 07:00:00', '2026-03-16 09:50:00', '2026-03-16 14:20:00', 4, '4.00', '4.30', '18.00', NULL, NULL);
INSERT INTO `order` (`order_id`, `order_no`, `user_id`, `attendant_id`, `attendant_name`, `attendant_phone`, `patient_name`, `patient_age`, `patient_sex`, `contact_person`, `contact_phone`, `hospital`, `service_content`, `clinic_type`, `service_date`, `service_time_slot`, `special_requirements`, `custom_requirement`, `order_amount`, `payment_status`, `payment_time`, `order_status`, `cancel_reason`, `cancel_time`, `cancel_by`, `penalty_rate`, `penalty_amount`, `refund_amount`, `qr_code_url`, `create_time`, `service_start_time`, `service_end_time`, `service_progress_step`, `estimated_duration`, `actual_duration`, `balance_amount`, `time_dispute_user_duration`, `time_dispute_reason`) VALUES (1012, 'ORD20260317001', 20, 24, '唐思雨', '13900139024', '何子安', 33, '男', '何子安', '13800138020', '成都市第一人民医院', '术后护理', 2, '2026-03-17', '08:30-12:30', '术后复查和伤口换药，需要全程陪同。', '需要协助打印病历', '300.00', 1, '2026-03-17 07:30:00', 6, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2026-03-17 07:00:00', '2026-03-17 08:20:00', '2026-03-17 12:45:00', 4, '4.00', '4.20', '20.00', NULL, NULL);
INSERT INTO `order` (`order_id`, `order_no`, `user_id`, `attendant_id`, `attendant_name`, `attendant_phone`, `patient_name`, `patient_age`, `patient_sex`, `contact_person`, `contact_phone`, `hospital`, `service_content`, `clinic_type`, `service_date`, `service_time_slot`, `special_requirements`, `custom_requirement`, `order_amount`, `payment_status`, `payment_time`, `order_status`, `cancel_reason`, `cancel_time`, `cancel_by`, `penalty_rate`, `penalty_amount`, `refund_amount`, `qr_code_url`, `create_time`, `service_start_time`, `service_end_time`, `service_progress_step`, `estimated_duration`, `actual_duration`, `balance_amount`, `time_dispute_user_duration`, `time_dispute_reason`) VALUES (1013, 'ORD20260321001', 15, NULL, NULL, NULL, '范涵伶', 26, '女', '范涵伶', '18600010001', '四川大学华西医院', '普通陪诊', 1, '2026-03-21', '08:30-11:30', '明天复诊，希望有熟悉流程的陪诊员。', '优先熟悉内分泌科', '185.00', 1, '2026-03-21 07:30:00', 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2026-03-21 07:00:00', NULL, NULL, NULL, '3.00', NULL, NULL, NULL, NULL);
INSERT INTO `order` (`order_id`, `order_no`, `user_id`, `attendant_id`, `attendant_name`, `attendant_phone`, `patient_name`, `patient_age`, `patient_sex`, `contact_person`, `contact_phone`, `hospital`, `service_content`, `clinic_type`, `service_date`, `service_time_slot`, `special_requirements`, `custom_requirement`, `order_amount`, `payment_status`, `payment_time`, `order_status`, `cancel_reason`, `cancel_time`, `cancel_by`, `penalty_rate`, `penalty_amount`, `refund_amount`, `qr_code_url`, `create_time`, `service_start_time`, `service_end_time`, `service_progress_step`, `estimated_duration`, `actual_duration`, `balance_amount`, `time_dispute_user_duration`, `time_dispute_reason`) VALUES (1014, 'ORD20260321002', 16, NULL, NULL, NULL, '柳清禾', 31, '女', '柳清禾', '13800138016', '四川省人民医院', '术后护理', 2, '2026-03-21', '13:30-15:30', '核磁复查后需要协助回休息区。', '需要提醒检查注意事项', '205.00', 1, '2026-03-21 07:30:00', 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2026-03-21 07:00:00', NULL, NULL, NULL, '2.00', NULL, NULL, NULL, NULL);
INSERT INTO `order` (`order_id`, `order_no`, `user_id`, `attendant_id`, `attendant_name`, `attendant_phone`, `patient_name`, `patient_age`, `patient_sex`, `contact_person`, `contact_phone`, `hospital`, `service_content`, `clinic_type`, `service_date`, `service_time_slot`, `special_requirements`, `custom_requirement`, `order_amount`, `payment_status`, `payment_time`, `order_status`, `cancel_reason`, `cancel_time`, `cancel_by`, `penalty_rate`, `penalty_amount`, `refund_amount`, `qr_code_url`, `create_time`, `service_start_time`, `service_end_time`, `service_progress_step`, `estimated_duration`, `actual_duration`, `balance_amount`, `time_dispute_user_duration`, `time_dispute_reason`) VALUES (1015, 'ORD20260320001', 17, 21, '肖阳', '18600000001', '周铭', 35, '男', '周铭', '13800138017', '四川大学华西医院', '普通陪诊', 1, '2026-03-20', '14:00-17:00', '今日下午复诊。', '已与陪诊师电话确认', '190.00', 1, '2026-03-20 07:30:00', 2, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2026-03-20 07:00:00', NULL, NULL, NULL, '3.00', NULL, NULL, NULL, NULL);
INSERT INTO `order` (`order_id`, `order_no`, `user_id`, `attendant_id`, `attendant_name`, `attendant_phone`, `patient_name`, `patient_age`, `patient_sex`, `contact_person`, `contact_phone`, `hospital`, `service_content`, `clinic_type`, `service_date`, `service_time_slot`, `special_requirements`, `custom_requirement`, `order_amount`, `payment_status`, `payment_time`, `order_status`, `cancel_reason`, `cancel_time`, `cancel_by`, `penalty_rate`, `penalty_amount`, `refund_amount`, `qr_code_url`, `create_time`, `service_start_time`, `service_end_time`, `service_progress_step`, `estimated_duration`, `actual_duration`, `balance_amount`, `time_dispute_user_duration`, `time_dispute_reason`) VALUES (1016, 'ORD20260320002', 18, 22, '林知夏', '13900139022', '陈雨桐', 29, '女', '陈雨桐', '13800138018', '四川省人民医院', '术后护理', 2, '2026-03-20', '15:00-18:00', '术后抽血和B超复查。', '需帮忙取报告', '215.00', 1, '2026-03-20 07:30:00', 2, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2026-03-20 07:00:00', NULL, NULL, NULL, '3.00', NULL, NULL, NULL, NULL);
INSERT INTO `order` (`order_id`, `order_no`, `user_id`, `attendant_id`, `attendant_name`, `attendant_phone`, `patient_name`, `patient_age`, `patient_sex`, `contact_person`, `contact_phone`, `hospital`, `service_content`, `clinic_type`, `service_date`, `service_time_slot`, `special_requirements`, `custom_requirement`, `order_amount`, `payment_status`, `payment_time`, `order_status`, `cancel_reason`, `cancel_time`, `cancel_by`, `penalty_rate`, `penalty_amount`, `refund_amount`, `qr_code_url`, `create_time`, `service_start_time`, `service_end_time`, `service_progress_step`, `estimated_duration`, `actual_duration`, `balance_amount`, `time_dispute_user_duration`, `time_dispute_reason`) VALUES (1017, 'ORD20260320003', 19, 23, '周可宁', '13900139023', '宋嘉宁', 42, '女', '宋嘉宁', '13800138019', '成都市妇女儿童中心医院', '急诊陪同', 3, '2026-03-20', '09:00-12:00', '孩子反复高烧，正在医院等待叫号。', '保持手机畅通', '225.00', 1, '2026-03-20 07:30:00', 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2026-03-20 07:00:00', '2026-03-20 09:05:00', NULL, 2, '3.00', NULL, NULL, NULL, NULL);
INSERT INTO `order` (`order_id`, `order_no`, `user_id`, `attendant_id`, `attendant_name`, `attendant_phone`, `patient_name`, `patient_age`, `patient_sex`, `contact_person`, `contact_phone`, `hospital`, `service_content`, `clinic_type`, `service_date`, `service_time_slot`, `special_requirements`, `custom_requirement`, `order_amount`, `payment_status`, `payment_time`, `order_status`, `cancel_reason`, `cancel_time`, `cancel_by`, `penalty_rate`, `penalty_amount`, `refund_amount`, `qr_code_url`, `create_time`, `service_start_time`, `service_end_time`, `service_progress_step`, `estimated_duration`, `actual_duration`, `balance_amount`, `time_dispute_user_duration`, `time_dispute_reason`) VALUES (1018, 'ORD20260320004', 20, 24, '唐思雨', '13900139024', '何子安', 33, '男', '何子安', '13800138020', '成都市第三人民医院', '上门陪诊', 4, '2026-03-20', '08:30-13:30', '患者行动不便，需要上门接送并协助办理入院。', '需要协助办理押金', '295.00', 1, '2026-03-20 07:30:00', 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2026-03-20 07:00:00', '2026-03-20 08:35:00', NULL, 2, '5.00', NULL, NULL, NULL, NULL);
INSERT INTO `order` (`order_id`, `order_no`, `user_id`, `attendant_id`, `attendant_name`, `attendant_phone`, `patient_name`, `patient_age`, `patient_sex`, `contact_person`, `contact_phone`, `hospital`, `service_content`, `clinic_type`, `service_date`, `service_time_slot`, `special_requirements`, `custom_requirement`, `order_amount`, `payment_status`, `payment_time`, `order_status`, `cancel_reason`, `cancel_time`, `cancel_by`, `penalty_rate`, `penalty_amount`, `refund_amount`, `qr_code_url`, `create_time`, `service_start_time`, `service_end_time`, `service_progress_step`, `estimated_duration`, `actual_duration`, `balance_amount`, `time_dispute_user_duration`, `time_dispute_reason`) VALUES (1019, 'ORD20260320005', 15, 21, '肖阳', '18600000001', '范涵伶', 26, '女', '范涵伶', '18600010001', '四川大学华西医院', '普通陪诊', 1, '2026-03-20', '10:00-13:00', '门诊就诊进行中。', '需要陪同取药', '205.00', 1, '2026-03-20 07:30:00', 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2026-03-20 07:00:00', '2026-03-20 10:05:00', NULL, 2, '3.00', NULL, NULL, NULL, NULL);
INSERT INTO `order` (`order_id`, `order_no`, `user_id`, `attendant_id`, `attendant_name`, `attendant_phone`, `patient_name`, `patient_age`, `patient_sex`, `contact_person`, `contact_phone`, `hospital`, `service_content`, `clinic_type`, `service_date`, `service_time_slot`, `special_requirements`, `custom_requirement`, `order_amount`, `payment_status`, `payment_time`, `order_status`, `cancel_reason`, `cancel_time`, `cancel_by`, `penalty_rate`, `penalty_amount`, `refund_amount`, `qr_code_url`, `create_time`, `service_start_time`, `service_end_time`, `service_progress_step`, `estimated_duration`, `actual_duration`, `balance_amount`, `time_dispute_user_duration`, `time_dispute_reason`) VALUES (1020, 'ORD20260319001', 16, 22, '林知夏', '13900139022', '柳清禾', 31, '女', '柳清禾', '13800138016', '四川省人民医院', '普通陪诊', 1, '2026-03-19', '14:00-17:00', '昨日普通陪诊已结束，待确认时长。', '用户对时长无异议', '200.00', 1, '2026-03-19 07:30:00', 4, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2026-03-19 07:00:00', '2026-03-19 14:05:00', '2026-03-19 17:05:00', 4, '3.00', '3.10', '10.00', NULL, NULL);
INSERT INTO `order` (`order_id`, `order_no`, `user_id`, `attendant_id`, `attendant_name`, `attendant_phone`, `patient_name`, `patient_age`, `patient_sex`, `contact_person`, `contact_phone`, `hospital`, `service_content`, `clinic_type`, `service_date`, `service_time_slot`, `special_requirements`, `custom_requirement`, `order_amount`, `payment_status`, `payment_time`, `order_status`, `cancel_reason`, `cancel_time`, `cancel_by`, `penalty_rate`, `penalty_amount`, `refund_amount`, `qr_code_url`, `create_time`, `service_start_time`, `service_end_time`, `service_progress_step`, `estimated_duration`, `actual_duration`, `balance_amount`, `time_dispute_user_duration`, `time_dispute_reason`) VALUES (1021, 'ORD20260319002', 17, 21, '肖阳', '18600000001', '周铭', 35, '男', '周铭', '13800138017', '四川大学华西医院', '术后护理', 2, '2026-03-19', '09:00-12:00', '待用户确认本次术后护理费用。', '需确认补缴护理材料费', '230.00', 1, '2026-03-19 07:30:00', 4, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2026-03-19 07:00:00', '2026-03-19 09:05:00', '2026-03-19 12:15:00', 4, '3.00', '3.20', '18.00', NULL, NULL);
INSERT INTO `order` (`order_id`, `order_no`, `user_id`, `attendant_id`, `attendant_name`, `attendant_phone`, `patient_name`, `patient_age`, `patient_sex`, `contact_person`, `contact_phone`, `hospital`, `service_content`, `clinic_type`, `service_date`, `service_time_slot`, `special_requirements`, `custom_requirement`, `order_amount`, `payment_status`, `payment_time`, `order_status`, `cancel_reason`, `cancel_time`, `cancel_by`, `penalty_rate`, `penalty_amount`, `refund_amount`, `qr_code_url`, `create_time`, `service_start_time`, `service_end_time`, `service_progress_step`, `estimated_duration`, `actual_duration`, `balance_amount`, `time_dispute_user_duration`, `time_dispute_reason`) VALUES (1022, 'ORD20260318001', 18, 24, '唐思雨', '13900139024', '陈雨桐', 29, '女', '陈雨桐', '13800138018', '成都市第一人民医院', '上门陪诊', 4, '2026-03-18', '08:00-12:00', '用户对本次上门陪诊时长有异议。', '用户认为返程途中等待时间不应计费', '280.00', 1, '2026-03-18 07:30:00', 5, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2026-03-18 07:00:00', '2026-03-18 08:10:00', '2026-03-18 12:30:00', 4, '4.00', '4.50', '25.00', '3.50', '用户在返程途中自行停留一小时');
INSERT INTO `order` (`order_id`, `order_no`, `user_id`, `attendant_id`, `attendant_name`, `attendant_phone`, `patient_name`, `patient_age`, `patient_sex`, `contact_person`, `contact_phone`, `hospital`, `service_content`, `clinic_type`, `service_date`, `service_time_slot`, `special_requirements`, `custom_requirement`, `order_amount`, `payment_status`, `payment_time`, `order_status`, `cancel_reason`, `cancel_time`, `cancel_by`, `penalty_rate`, `penalty_amount`, `refund_amount`, `qr_code_url`, `create_time`, `service_start_time`, `service_end_time`, `service_progress_step`, `estimated_duration`, `actual_duration`, `balance_amount`, `time_dispute_user_duration`, `time_dispute_reason`) VALUES (1023, 'ORD20260322001', 19, NULL, NULL, NULL, '宋嘉宁', 42, '女', '宋嘉宁', '13800138019', '成都市第三人民医院', '普通陪诊', 1, '2026-03-22', '08:30-11:30', '已创建订单，等待支付。', '需要短信提醒支付', '185.00', 0, NULL, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2026-03-22 07:00:00', NULL, NULL, NULL, '3.00', NULL, NULL, NULL, NULL);
INSERT INTO `order` (`order_id`, `order_no`, `user_id`, `attendant_id`, `attendant_name`, `attendant_phone`, `patient_name`, `patient_age`, `patient_sex`, `contact_person`, `contact_phone`, `hospital`, `service_content`, `clinic_type`, `service_date`, `service_time_slot`, `special_requirements`, `custom_requirement`, `order_amount`, `payment_status`, `payment_time`, `order_status`, `cancel_reason`, `cancel_time`, `cancel_by`, `penalty_rate`, `penalty_amount`, `refund_amount`, `qr_code_url`, `create_time`, `service_start_time`, `service_end_time`, `service_progress_step`, `estimated_duration`, `actual_duration`, `balance_amount`, `time_dispute_user_duration`, `time_dispute_reason`) VALUES (1024, 'ORD20260318002', 20, 23, '周可宁', '13900139023', '何子安', 33, '男', '何子安', '13800138020', '成都市妇女儿童中心医院', '急诊陪同', 3, '2026-03-18', '13:00-16:00', '因家属临时有事取消。', '系统已释放陪诊师', '210.00', 1, '2026-03-18 07:30:00', 7, '家属临时无法到院', '2026-03-18 11:20:00', 0, NULL, NULL, NULL, NULL, '2026-03-18 07:00:00', NULL, NULL, NULL, '3.00', NULL, NULL, NULL, NULL);
UPDATE `order`
SET `accept_time` = CASE
  WHEN `order_status` >= 2 THEN COALESCE(DATE_SUB(`service_start_time`, INTERVAL 30 MINUTE), `payment_time`, `create_time`)
  ELSE NULL
END;
COMMIT;

-- Data for order_evaluation
BEGIN;
INSERT INTO `order_evaluation` (`id`, `order_id`, `order_no`, `user_id`, `attendant_id`, `rating`, `tags`, `content`, `attendant_reply`, `reply_time`, `create_time`, `update_time`) VALUES (1, 1001, 'ORD20260310001', 15, 21, 5, '沟通耐心,时间准时', '沟通特别清晰，整个就诊流程都安排得很稳。', '感谢认可，祝您早日康复。', '2026-03-10 14:40:00', '2026-03-10 12:40:00', '2026-03-10 14:40:00');
INSERT INTO `order_evaluation` (`id`, `order_id`, `order_no`, `user_id`, `attendant_id`, `rating`, `tags`, `content`, `attendant_reply`, `reply_time`, `create_time`, `update_time`) VALUES (2, 1002, 'ORD20260310002', 16, 21, 5, '路线熟悉,协助取号', '陪同检查很细致，取报告也很及时。', '谢谢信任，有需要随时联系。', '2026-03-10 19:10:00', '2026-03-10 17:10:00', '2026-03-10 19:10:00');
INSERT INTO `order_evaluation` (`id`, `order_id`, `order_no`, `user_id`, `attendant_id`, `rating`, `tags`, `content`, `attendant_reply`, `reply_time`, `create_time`, `update_time`) VALUES (3, 1003, 'ORD20260311001', 17, 21, 5, '服务细致,流程熟悉', '对医院路线很熟，节省了很多时间。', '很高兴帮到您，后续复诊也可以联系我。', '2026-03-11 15:05:00', '2026-03-11 13:05:00', '2026-03-11 15:05:00');
INSERT INTO `order_evaluation` (`id`, `order_id`, `order_no`, `user_id`, `attendant_id`, `rating`, `tags`, `content`, `attendant_reply`, `reply_time`, `create_time`, `update_time`) VALUES (4, 1004, 'ORD20260312001', 18, 21, 4, '态度温和,解释清楚', '服务很认真，态度耐心。', NULL, NULL, '2026-03-12 16:10:00', '2026-03-12 16:10:00');
INSERT INTO `order_evaluation` (`id`, `order_id`, `order_no`, `user_id`, `attendant_id`, `rating`, `tags`, `content`, `attendant_reply`, `reply_time`, `create_time`, `update_time`) VALUES (5, 1005, 'ORD20260313001', 19, 21, 5, '照顾周到,医院熟悉', '长辈反馈很好，整个过程都很安心。', '感谢认可，祝您早日康复。', '2026-03-13 15:00:00', '2026-03-13 13:00:00', '2026-03-13 15:00:00');
INSERT INTO `order_evaluation` (`id`, `order_id`, `order_no`, `user_id`, `attendant_id`, `rating`, `tags`, `content`, `attendant_reply`, `reply_time`, `create_time`, `update_time`) VALUES (6, 1006, 'ORD20260314001', 20, 22, 5, '主动提醒,效率高', '陪诊师提醒事项很到位，体验不错。', '谢谢信任，有需要随时联系。', '2026-03-14 14:35:00', '2026-03-14 12:35:00', '2026-03-14 14:35:00');
INSERT INTO `order_evaluation` (`id`, `order_id`, `order_no`, `user_id`, `attendant_id`, `rating`, `tags`, `content`, `attendant_reply`, `reply_time`, `create_time`, `update_time`) VALUES (7, 1007, 'ORD20260314002', 15, 22, 4, '陪同细致,耐心负责', '排队和缴费都帮忙处理得很顺畅。', '很高兴帮到您，后续复诊也可以联系我。', '2026-03-14 20:10:00', '2026-03-14 18:10:00', '2026-03-14 20:10:00');
INSERT INTO `order_evaluation` (`id`, `order_id`, `order_no`, `user_id`, `attendant_id`, `rating`, `tags`, `content`, `attendant_reply`, `reply_time`, `create_time`, `update_time`) VALUES (8, 1008, 'ORD20260315001', 16, 22, 5, '报告领取及时,讲解清楚', '对检查流程很熟，省心很多。', NULL, NULL, '2026-03-15 11:40:00', '2026-03-15 11:40:00');
INSERT INTO `order_evaluation` (`id`, `order_id`, `order_no`, `user_id`, `attendant_id`, `rating`, `tags`, `content`, `attendant_reply`, `reply_time`, `create_time`, `update_time`) VALUES (9, 1009, 'ORD20260315002', 17, 23, 5, '排队高效,态度温柔', '全程耐心陪同，沟通很舒服。', '感谢认可，祝您早日康复。', '2026-03-15 19:40:00', '2026-03-15 17:40:00', '2026-03-15 19:40:00');
INSERT INTO `order_evaluation` (`id`, `order_id`, `order_no`, `user_id`, `attendant_id`, `rating`, `tags`, `content`, `attendant_reply`, `reply_time`, `create_time`, `update_time`) VALUES (10, 1010, 'ORD20260316001', 18, 23, 4, '服务稳定,响应迅速', '整体服务不错，后续提醒也及时。', '谢谢信任，有需要随时联系。', '2026-03-16 14:10:00', '2026-03-16 12:10:00', '2026-03-16 14:10:00');
INSERT INTO `order_evaluation` (`id`, `order_id`, `order_no`, `user_id`, `attendant_id`, `rating`, `tags`, `content`, `attendant_reply`, `reply_time`, `create_time`, `update_time`) VALUES (11, 1011, 'ORD20260316002', 19, 24, 5, '专业可靠,沟通顺畅', '照顾患者情绪很到位，专业度高。', '很高兴帮到您，后续复诊也可以联系我。', '2026-03-16 17:20:00', '2026-03-16 15:20:00', '2026-03-16 17:20:00');
INSERT INTO `order_evaluation` (`id`, `order_id`, `order_no`, `user_id`, `attendant_id`, `rating`, `tags`, `content`, `attendant_reply`, `reply_time`, `create_time`, `update_time`) VALUES (12, 1012, 'ORD20260317001', 20, 24, 5, '就诊协助到位,很安心', '复诊流程安排合理，体验很好。', NULL, NULL, '2026-03-17 13:45:00', '2026-03-17 13:45:00');
COMMIT;

-- Data for chat_message
BEGIN;
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (1, 0, 15, '您已成功支付订单 ORD20260310001，平台已为您锁定陪诊服务。', 1, 1, '2026-03-10 07:30:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (2, 0, 15, '您预约的2026-03-10 08:30-11:30 四川大学华西医院 服务已由陪诊师肖阳接单，请保持电话畅通。', 1, 1, '2026-03-10 07:40:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (3, 21, 15, '您好，我是陪诊师肖阳，已经看到您的订单，我会在服务前与您再次确认细节。', 1, 1, '2026-03-10 07:45:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (4, 0, 15, '您的订单 ORD20260310001 已完成，如对本次服务满意，欢迎提交评价。', 1, 1, '2026-03-10 11:50:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (5, 0, 21, '订单 ORD20260310001 已由用户确认完成，辛苦了。', 1, 1, '2026-03-10 11:50:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (6, 0, 16, '您已成功支付订单 ORD20260310002，平台已为您锁定陪诊服务。', 1, 1, '2026-03-10 07:30:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (7, 0, 16, '您预约的2026-03-10 13:00-16:00 四川大学华西医院 服务已由陪诊师肖阳接单，请保持电话畅通。', 1, 1, '2026-03-10 07:40:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (8, 21, 16, '您好，我是陪诊师肖阳，已经看到您的订单，我会在服务前与您再次确认细节。', 1, 1, '2026-03-10 07:45:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (9, 0, 16, '您的订单 ORD20260310002 已完成，如对本次服务满意，欢迎提交评价。', 1, 1, '2026-03-10 16:20:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (10, 0, 21, '订单 ORD20260310002 已由用户确认完成，辛苦了。', 1, 1, '2026-03-10 16:20:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (11, 0, 17, '您已成功支付订单 ORD20260311001，平台已为您锁定陪诊服务。', 1, 1, '2026-03-11 07:30:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (12, 0, 17, '您预约的2026-03-11 09:00-12:00 四川省人民医院 服务已由陪诊师肖阳接单，请保持电话畅通。', 1, 1, '2026-03-11 07:40:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (13, 21, 17, '您好，我是陪诊师肖阳，已经看到您的订单，我会在服务前与您再次确认细节。', 1, 1, '2026-03-11 07:45:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (14, 0, 17, '您的订单 ORD20260311001 已完成，如对本次服务满意，欢迎提交评价。', 1, 1, '2026-03-11 12:15:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (15, 0, 21, '订单 ORD20260311001 已由用户确认完成，辛苦了。', 1, 1, '2026-03-11 12:15:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (16, 0, 18, '您已成功支付订单 ORD20260312001，平台已为您锁定陪诊服务。', 1, 1, '2026-03-12 07:30:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (17, 0, 18, '您预约的2026-03-12 10:00-15:00 四川大学华西医院 服务已由陪诊师肖阳接单，请保持电话畅通。', 1, 1, '2026-03-12 07:40:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (18, 21, 18, '您好，我是陪诊师肖阳，已经看到您的订单，我会在服务前与您再次确认细节。', 1, 1, '2026-03-12 07:45:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (19, 0, 18, '您的订单 ORD20260312001 已完成，如对本次服务满意，欢迎提交评价。', 1, 1, '2026-03-12 15:20:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (20, 0, 21, '订单 ORD20260312001 已由用户确认完成，辛苦了。', 1, 1, '2026-03-12 15:20:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (21, 0, 19, '您已成功支付订单 ORD20260313001，平台已为您锁定陪诊服务。', 1, 1, '2026-03-13 07:30:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (22, 0, 19, '您预约的2026-03-13 08:00-12:00 成都市第三人民医院 服务已由陪诊师肖阳接单，请保持电话畅通。', 1, 1, '2026-03-13 07:40:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (23, 21, 19, '您好，我是陪诊师肖阳，已经看到您的订单，我会在服务前与您再次确认细节。', 1, 1, '2026-03-13 07:45:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (24, 0, 19, '您的订单 ORD20260313001 已完成，如对本次服务满意，欢迎提交评价。', 1, 1, '2026-03-13 12:10:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (25, 0, 21, '订单 ORD20260313001 已由用户确认完成，辛苦了。', 1, 1, '2026-03-13 12:10:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (26, 0, 20, '您已成功支付订单 ORD20260314001，平台已为您锁定陪诊服务。', 1, 1, '2026-03-14 07:30:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (27, 0, 20, '您预约的2026-03-14 09:00-11:30 四川省人民医院 服务已由陪诊师林知夏接单，请保持电话畅通。', 1, 1, '2026-03-14 07:40:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (28, 22, 20, '您好，我是陪诊师林知夏，已经看到您的订单，我会在服务前与您再次确认细节。', 1, 1, '2026-03-14 07:45:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (29, 0, 20, '您的订单 ORD20260314001 已完成，如对本次服务满意，欢迎提交评价。', 1, 1, '2026-03-14 11:45:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (30, 0, 22, '订单 ORD20260314001 已由用户确认完成，辛苦了。', 1, 1, '2026-03-14 11:45:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (31, 0, 15, '您已成功支付订单 ORD20260314002，平台已为您锁定陪诊服务。', 1, 1, '2026-03-14 07:30:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (32, 0, 15, '您预约的2026-03-14 14:00-17:00 成都中医药大学附属医院 服务已由陪诊师林知夏接单，请保持电话畅通。', 1, 1, '2026-03-14 07:40:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (33, 22, 15, '您好，我是陪诊师林知夏，已经看到您的订单，我会在服务前与您再次确认细节。', 1, 1, '2026-03-14 07:45:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (34, 0, 15, '您的订单 ORD20260314002 已完成，如对本次服务满意，欢迎提交评价。', 1, 1, '2026-03-14 17:20:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (35, 0, 22, '订单 ORD20260314002 已由用户确认完成，辛苦了。', 1, 1, '2026-03-14 17:20:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (36, 0, 16, '您已成功支付订单 ORD20260315001，平台已为您锁定陪诊服务。', 1, 1, '2026-03-15 07:30:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (37, 0, 16, '您预约的2026-03-15 08:30-10:30 四川省人民医院 服务已由陪诊师林知夏接单，请保持电话畅通。', 1, 1, '2026-03-15 07:40:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (38, 22, 16, '您好，我是陪诊师林知夏，已经看到您的订单，我会在服务前与您再次确认细节。', 1, 1, '2026-03-15 07:45:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (39, 0, 16, '您的订单 ORD20260315001 已完成，如对本次服务满意，欢迎提交评价。', 1, 1, '2026-03-15 10:50:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (40, 0, 22, '订单 ORD20260315001 已由用户确认完成，辛苦了。', 1, 1, '2026-03-15 10:50:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (41, 0, 17, '您已成功支付订单 ORD20260315002，平台已为您锁定陪诊服务。', 1, 1, '2026-03-15 07:30:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (42, 0, 17, '您预约的2026-03-15 13:30-16:30 成都市妇女儿童中心医院 服务已由陪诊师周可宁接单，请保持电话畅通。', 1, 1, '2026-03-15 07:40:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (43, 23, 17, '您好，我是陪诊师周可宁，已经看到您的订单，我会在服务前与您再次确认细节。', 1, 1, '2026-03-15 07:45:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (44, 0, 17, '您的订单 ORD20260315002 已完成，如对本次服务满意，欢迎提交评价。', 1, 1, '2026-03-15 16:50:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (45, 0, 23, '订单 ORD20260315002 已由用户确认完成，辛苦了。', 1, 1, '2026-03-15 16:50:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (46, 0, 18, '您已成功支付订单 ORD20260316001，平台已为您锁定陪诊服务。', 1, 1, '2026-03-16 07:30:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (47, 0, 18, '您预约的2026-03-16 09:00-11:00 成都市妇女儿童中心医院 服务已由陪诊师周可宁接单，请保持电话畅通。', 1, 1, '2026-03-16 07:40:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (48, 23, 18, '您好，我是陪诊师周可宁，已经看到您的订单，我会在服务前与您再次确认细节。', 1, 1, '2026-03-16 07:45:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (49, 0, 18, '您的订单 ORD20260316001 已完成，如对本次服务满意，欢迎提交评价。', 1, 1, '2026-03-16 11:20:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (50, 0, 23, '订单 ORD20260316001 已由用户确认完成，辛苦了。', 1, 1, '2026-03-16 11:20:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (51, 0, 19, '您已成功支付订单 ORD20260316002，平台已为您锁定陪诊服务。', 1, 1, '2026-03-16 07:30:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (52, 0, 19, '您预约的2026-03-16 10:00-14:00 成都市第三人民医院 服务已由陪诊师唐思雨接单，请保持电话畅通。', 1, 1, '2026-03-16 07:40:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (53, 24, 19, '您好，我是陪诊师唐思雨，已经看到您的订单，我会在服务前与您再次确认细节。', 1, 1, '2026-03-16 07:45:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (54, 0, 19, '您的订单 ORD20260316002 已完成，如对本次服务满意，欢迎提交评价。', 1, 1, '2026-03-16 14:30:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (55, 0, 24, '订单 ORD20260316002 已由用户确认完成，辛苦了。', 1, 1, '2026-03-16 14:30:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (56, 0, 20, '您已成功支付订单 ORD20260317001，平台已为您锁定陪诊服务。', 1, 1, '2026-03-17 07:30:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (57, 0, 20, '您预约的2026-03-17 08:30-12:30 成都市第一人民医院 服务已由陪诊师唐思雨接单，请保持电话畅通。', 1, 1, '2026-03-17 07:40:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (58, 24, 20, '您好，我是陪诊师唐思雨，已经看到您的订单，我会在服务前与您再次确认细节。', 1, 1, '2026-03-17 07:45:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (59, 0, 20, '您的订单 ORD20260317001 已完成，如对本次服务满意，欢迎提交评价。', 1, 1, '2026-03-17 12:55:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (60, 0, 24, '订单 ORD20260317001 已由用户确认完成，辛苦了。', 1, 1, '2026-03-17 12:55:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (61, 0, 15, '您的订单 ORD20260321001 已创建成功，请在 15 分钟内完成支付。', 1, 0, '2026-03-20 12:30:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (62, 0, 15, '您有一条新的平台提醒：若需改期，请至少提前 12 小时操作。', 1, 0, '2026-03-20 12:40:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (63, 21, 15, '明天的复诊我已经提前帮您看好路线了，您有新增需求可以直接告诉我。', 1, 0, '2026-03-20 12:45:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (64, 0, 16, '您的订单 ORD20260321002 已完成支付，陪诊师将在今晚与您联系。', 1, 1, '2026-03-20 12:50:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (65, 22, 16, '您好，我是林知夏，明天下午我会提前 20 分钟到医院门口等您。', 1, 0, '2026-03-20 12:55:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (66, 0, 21, '您有新的待服务订单 ORD20260320005，请留意服务时间。', 1, 1, '2026-03-20 09:50:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (67, 0, 22, '订单 ORD20260319001 正等待用户确认时长，请您稍后关注结果。', 1, 1, '2026-03-19 17:20:00');
COMMIT;

-- Data for guide_appointment
BEGIN;
INSERT INTO `guide_appointment` (`id`, `appointment_no`, `user_id`, `patient_name`, `patient_phone`, `symptoms`, `hospital_name`, `service_type_number`, `service_date`, `service_start_time`, `service_end_time`, `other_requirement`, `create_time`) VALUES (301, 'APT2026032001', 15, '范涵伶', '18600010001', '["头晕", "高血压复诊"]', '四川大学华西医院', 1, '2026-03-21', '08:30', '11:30', '希望优先安排熟悉华西医院的陪诊师', '2026-03-20 10:00:00');
INSERT INTO `guide_appointment` (`id`, `appointment_no`, `user_id`, `patient_name`, `patient_phone`, `symptoms`, `hospital_name`, `service_type_number`, `service_date`, `service_start_time`, `service_end_time`, `other_requirement`, `create_time`) VALUES (302, 'APT2026032002', 16, '柳清禾', '13800138016', '["胃镜术后恢复", "复查取药"]', '四川省人民医院', 2, '2026-03-22', '13:00', '16:00', '检查后需要陪同休息和取药', '2026-03-20 10:00:00');
INSERT INTO `guide_appointment` (`id`, `appointment_no`, `user_id`, `patient_name`, `patient_phone`, `symptoms`, `hospital_name`, `service_type_number`, `service_date`, `service_start_time`, `service_end_time`, `other_requirement`, `create_time`) VALUES (303, 'APT2026032003', 17, '周铭', '13800138017', '["高血压", "心悸"]', '四川省人民医院', 1, '2026-03-23', '09:00', '12:00', '希望帮助记录医嘱', '2026-03-20 10:00:00');
INSERT INTO `guide_appointment` (`id`, `appointment_no`, `user_id`, `patient_name`, `patient_phone`, `symptoms`, `hospital_name`, `service_type_number`, `service_date`, `service_start_time`, `service_end_time`, `other_requirement`, `create_time`) VALUES (304, 'APT2026032004', 18, '陈雨桐', '13800138018', '["儿童高热", "咳嗽"]', '成都市妇女儿童中心医院', 3, '2026-03-23', '14:00', '17:00', '希望能快速协助急诊分诊', '2026-03-20 10:00:00');
INSERT INTO `guide_appointment` (`id`, `appointment_no`, `user_id`, `patient_name`, `patient_phone`, `symptoms`, `hospital_name`, `service_type_number`, `service_date`, `service_start_time`, `service_end_time`, `other_requirement`, `create_time`) VALUES (305, 'APT2026032005', 19, '宋嘉宁', '13800138019', '["术后返院复查", "行动不便"]', '成都市第三人民医院', 4, '2026-03-24', '08:00', '12:00', '需从家中接送到院并协助缴费', '2026-03-20 10:00:00');
INSERT INTO `guide_appointment` (`id`, `appointment_no`, `user_id`, `patient_name`, `patient_phone`, `symptoms`, `hospital_name`, `service_type_number`, `service_date`, `service_start_time`, `service_end_time`, `other_requirement`, `create_time`) VALUES (306, 'APT2026032006', 20, '何子安', '13800138020', '["膝关节损伤复查", "行动受限"]', '成都市第一人民医院', 4, '2026-03-24', '13:00', '17:00', '希望从家中接送并陪同打印病历', '2026-03-20 10:00:00');
INSERT INTO `guide_appointment` (`id`, `appointment_no`, `user_id`, `patient_name`, `patient_phone`, `symptoms`, `hospital_name`, `service_type_number`, `service_date`, `service_start_time`, `service_end_time`, `other_requirement`, `create_time`) VALUES (307, 'APT2026032007', 15, '范涵伶', '18600010001', '["甲状腺复诊"]', '四川大学华西医院', 1, '2026-03-25', '09:00', '11:00', '最好能熟悉内分泌门诊', '2026-03-20 10:00:00');
INSERT INTO `guide_appointment` (`id`, `appointment_no`, `user_id`, `patient_name`, `patient_phone`, `symptoms`, `hospital_name`, `service_type_number`, `service_date`, `service_start_time`, `service_end_time`, `other_requirement`, `create_time`) VALUES (308, 'APT2026032008', 16, '柳清禾', '13800138016', '["术后彩超复查"]', '成都中医药大学附属医院', 2, '2026-03-25', '14:00', '16:00', '女性陪诊优先', '2026-03-20 10:00:00');
INSERT INTO `guide_appointment` (`id`, `appointment_no`, `user_id`, `patient_name`, `patient_phone`, `symptoms`, `hospital_name`, `service_type_number`, `service_date`, `service_start_time`, `service_end_time`, `other_requirement`, `create_time`) VALUES (309, 'APT2026032009', 17, '周铭', '13800138017', '["取药复诊"]', '四川省人民医院', 1, '2026-03-26', '08:30', '10:30', '希望节省排队时间', '2026-03-20 10:00:00');
INSERT INTO `guide_appointment` (`id`, `appointment_no`, `user_id`, `patient_name`, `patient_phone`, `symptoms`, `hospital_name`, `service_type_number`, `service_date`, `service_start_time`, `service_end_time`, `other_requirement`, `create_time`) VALUES (310, 'APT2026032010', 18, '陈雨桐', '13800138018', '["儿童夜间高热"]', '成都市妇女儿童中心医院', 3, '2026-03-26', '13:30', '16:30', '需协助安抚孩子情绪并尽快就诊', '2026-03-20 10:00:00');
INSERT INTO `guide_appointment` (`id`, `appointment_no`, `user_id`, `patient_name`, `patient_phone`, `symptoms`, `hospital_name`, `service_type_number`, `service_date`, `service_start_time`, `service_end_time`, `other_requirement`, `create_time`) VALUES (311, 'APT2026032011', 19, '宋嘉宁', '13800138019', '["术后拆线"]', '成都市第三人民医院', 2, '2026-03-27', '09:00', '12:00', '家属白天无法陪同，需要护理提醒', '2026-03-20 10:00:00');
INSERT INTO `guide_appointment` (`id`, `appointment_no`, `user_id`, `patient_name`, `patient_phone`, `symptoms`, `hospital_name`, `service_type_number`, `service_date`, `service_start_time`, `service_end_time`, `other_requirement`, `create_time`) VALUES (312, 'APT2026032012', 20, '何子安', '13800138020', '["复诊取药", "行动不便"]', '成都市第一人民医院', 4, '2026-03-27', '14:00', '17:30', '需要从家中出发全程陪同', '2026-03-20 10:00:00');
COMMIT;

-- Data for ai_medical_qa
BEGIN;
INSERT INTO `ai_medical_qa` (`id`, `conversation_id`, `question`, `answer`, `qa_status`, `create_time`, `update_time`, `deleted`, `thinking_process`) VALUES (1, 'qa_demo_001', '高血压患者复诊前要注意什么？', '建议提前整理近期血压记录、既往用药清单，并携带最近的化验和影像资料。', 1, '2026-03-18 09:00:00', '2026-03-18 09:00:10', 0, '根据复诊准备场景，优先提醒病历与用药记录。');
INSERT INTO `ai_medical_qa` (`id`, `conversation_id`, `question`, `answer`, `qa_status`, `create_time`, `update_time`, `deleted`, `thinking_process`) VALUES (2, 'qa_demo_002', '做胃镜前需要空腹多久？', '通常需要空腹 6-8 小时，具体请以医院检查单和医生要求为准。', 1, '2026-03-18 09:05:00', '2026-03-18 09:05:08', 0, '结合常规检查准备要求进行回答。');
INSERT INTO `ai_medical_qa` (`id`, `conversation_id`, `question`, `answer`, `qa_status`, `create_time`, `update_time`, `deleted`, `thinking_process`) VALUES (3, 'qa_demo_003', '术后复查前要准备哪些材料？', '建议准备出院小结、近期检查报告、当前用药清单以及医生要求复带的影像资料。', 1, '2026-03-18 09:10:00', '2026-03-18 09:10:12', 0, '围绕术后护理场景，优先提醒复查资料和用药记录。');
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;

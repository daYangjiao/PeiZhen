/*
 Navicat Premium Dump SQL

 Source Server         : FF_Mysql
 Source Server Type    : MySQL
 Source Server Version : 80043 (8.0.43)
 Source Host           : localhost:3306
 Source Schema         : student

 Target Server Type    : MySQL
 Target Server Version : 80043 (8.0.43)
 File Encoding         : 65001

 Date: 14/02/2026 15:45:07
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for ai_medical_qa
-- ----------------------------
DROP TABLE IF EXISTS `ai_medical_qa`;
CREATE TABLE `ai_medical_qa`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `conversation_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '对话会话ID，用于关联同一场对话的所有消息',
  `question` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户提问内容',
  `answer` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT 'AI回答内容',
  `qa_status` tinyint NOT NULL DEFAULT 0 COMMENT '问答状态：0-处理中，1-完成，2-失败',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `thinking_process` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT 'AI思考过程',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_conversation_id`(`conversation_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI医疗问答记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of ai_medical_qa
-- ----------------------------

-- ----------------------------
-- Table structure for attendant
-- ----------------------------
DROP TABLE IF EXISTS `attendant`;
CREATE TABLE `attendant`  (
  `user_id` int NOT NULL COMMENT '关联user表ID',
  `certificate` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '资格证书编号',
  `status` int NULL DEFAULT 1 COMMENT '状态：0=审核中, 1=正常, 2=封禁',
  `introduction` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '个人简介',
  `professional_field` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '擅长领域',
  `score` decimal(2, 1) NULL DEFAULT 5.0 COMMENT '评分',
  `experience_years` int NULL DEFAULT 0 COMMENT '从业年限',
  `hospital_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '常驻医院',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`) USING BTREE,
  CONSTRAINT `attendant_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '陪诊师扩展信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of attendant
-- ----------------------------
INSERT INTO `attendant` VALUES (1, 'CP20230802', 1, '多年陪诊经验，擅长老年陪护', '老年患者陪诊', 4.7, 6, NULL, '2025-07-01 10:22:35', '2026-02-09 14:51:23');
INSERT INTO `attendant` VALUES (21, NULL, 1, '资深陪诊师，熟悉各大医院流程，服务态度好。', '全科,术后护理,挂号引导', 5.0, 5, NULL, '2026-02-12 00:09:40', '2026-02-12 00:09:40');
INSERT INTO `attendant` VALUES (22, 'CP20230802', 0, '新手陪诊师，正在学习', '新手陪诊', 4.0, 0, NULL, '2025-07-01 10:06:05', '2025-09-09 15:58:15');
INSERT INTO `attendant` VALUES (23, 'CP20230802', 1, '多年陪诊经验，擅长老年陪护', '老年陪护', 5.0, 5, NULL, '2025-07-01 10:22:35', '2025-09-09 15:58:38');
INSERT INTO `attendant` VALUES (24, 'MED2023001', 1, '资深陪诊师', '普通陪诊', 5.0, 3, NULL, '2025-07-02 11:06:37', '2025-09-09 15:59:26');
INSERT INTO `attendant` VALUES (25, NULL, 1, '膝关节手术,术后护理,综合陪诊服务 (参考价格: 198.00)', '术后护理,综合陪诊服务', 4.8, 5, '北京协和医院', '2025-11-29 22:09:24', '2025-12-01 21:27:49');
INSERT INTO `attendant` VALUES (26, NULL, 0, '膝关节手术,术后护理,综合陪诊 (参考价格: 198.00)', '术后护理,综合陪诊', 4.8, 5, NULL, '2025-12-15 21:24:59', '2025-12-15 21:24:59');

-- ----------------------------
-- Table structure for chat_message
-- ----------------------------
DROP TABLE IF EXISTS `chat_message`;
CREATE TABLE `chat_message`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `sender_id` int NOT NULL COMMENT '发送者ID',
  `receiver_id` int NOT NULL COMMENT '接收者ID',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '消息内容',
  `msg_type` int NULL DEFAULT 1 COMMENT '消息类型：1=文本, 2=图片',
  `is_read` tinyint(1) NULL DEFAULT 0 COMMENT '是否已读：0=未读, 1=已读',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_sender_receiver`(`sender_id` ASC, `receiver_id` ASC) USING BTREE,
  INDEX `idx_receiver_read`(`receiver_id` ASC, `is_read` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 43 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '聊天消息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of chat_message
-- ----------------------------
INSERT INTO `chat_message` VALUES (1, 0, 15, '您的订单 ORD1770966931301a7ae31 已被陪诊师 李怀 接单，请保持电话畅通。', 1, 0, '2026-02-13 15:16:00');
INSERT INTO `chat_message` VALUES (2, 0, 15, '您的订单 ORD1770966931301a7ae31 服务已开始。', 1, 0, '2026-02-13 15:17:18');
INSERT INTO `chat_message` VALUES (3, 0, 15, '您的订单 ORD1770966931301a7ae31 服务已结束，请确认服务时长并评价。', 1, 0, '2026-02-13 15:21:58');
INSERT INTO `chat_message` VALUES (4, 0, 15, '您的订单 ORD17709675931199747d2 已被陪诊师 李怀 接单，请保持电话畅通。', 1, 0, '2026-02-13 15:26:45');
INSERT INTO `chat_message` VALUES (5, 0, 15, '您的订单 ORD17709675931199747d2 服务已开始。', 1, 0, '2026-02-13 15:26:52');
INSERT INTO `chat_message` VALUES (6, 0, 15, '您的订单 ORD177096940247987dfd3 支付成功，等待接单中。', 1, 0, '2026-02-13 15:56:43');
INSERT INTO `chat_message` VALUES (7, 0, 15, '您的订单 ORD177096940247987dfd3 已被陪诊师 李怀 接单，请保持电话畅通。', 1, 0, '2026-02-13 15:56:48');
INSERT INTO `chat_message` VALUES (8, 0, 15, '您的订单 ORD177096940247987dfd3 服务已开始。', 1, 0, '2026-02-13 16:16:38');
INSERT INTO `chat_message` VALUES (9, 0, 21, '您已开始为订单 ORD177096940247987dfd3 提供服务，请按时完成服务。', 1, 0, '2026-02-13 16:16:38');
INSERT INTO `chat_message` VALUES (10, 0, 15, '您的订单 ORD17709706212501c860c 支付成功，等待接单中。', 1, 0, '2026-02-13 16:17:02');
INSERT INTO `chat_message` VALUES (11, 0, 15, '您的订单 ORD17709706212501c860c 已被陪诊师 李怀 接单，请保持电话畅通。', 1, 0, '2026-02-13 16:17:29');
INSERT INTO `chat_message` VALUES (12, 21, 15, '您好！我是陪诊师李怀，很高兴为您服务。我会尽快与您联系确认服务细节。', 1, 1, '2026-02-13 16:17:29');
INSERT INTO `chat_message` VALUES (13, 21, 15, '你好', 1, 1, '2026-02-13 16:52:43');
INSERT INTO `chat_message` VALUES (14, 15, 21, '你好', 1, 1, '2026-02-13 19:08:48');
INSERT INTO `chat_message` VALUES (15, 21, 15, '你好', 1, 1, '2026-02-13 20:51:31');
INSERT INTO `chat_message` VALUES (16, 15, 21, '你好', 1, 1, '2026-02-13 20:51:39');
INSERT INTO `chat_message` VALUES (17, 21, 15, '你好1', 1, 1, '2026-02-13 20:51:55');
INSERT INTO `chat_message` VALUES (18, 21, 15, '你好', 1, 1, '2026-02-13 21:06:18');
INSERT INTO `chat_message` VALUES (19, 15, 21, '我不好', 1, 1, '2026-02-13 21:06:21');
INSERT INTO `chat_message` VALUES (20, 21, 15, '11', 1, 1, '2026-02-13 21:09:41');
INSERT INTO `chat_message` VALUES (21, 15, 21, '22', 1, 1, '2026-02-13 21:09:47');
INSERT INTO `chat_message` VALUES (22, 21, 15, '11', 1, 1, '2026-02-13 21:12:03');
INSERT INTO `chat_message` VALUES (23, 15, 21, '11', 1, 1, '2026-02-13 21:12:06');
INSERT INTO `chat_message` VALUES (24, 21, 15, '2', 1, 1, '2026-02-13 21:13:19');
INSERT INTO `chat_message` VALUES (25, 21, 15, '1', 1, 1, '2026-02-13 21:16:23');
INSERT INTO `chat_message` VALUES (26, 21, 15, '2', 1, 1, '2026-02-13 21:16:36');
INSERT INTO `chat_message` VALUES (27, 21, 15, 'q', 1, 1, '2026-02-13 21:19:08');
INSERT INTO `chat_message` VALUES (28, 21, 15, '1', 1, 1, '2026-02-13 21:20:05');
INSERT INTO `chat_message` VALUES (29, 15, 21, '22', 1, 1, '2026-02-13 21:20:12');
INSERT INTO `chat_message` VALUES (30, 21, 15, '2', 1, 1, '2026-02-13 21:20:17');
INSERT INTO `chat_message` VALUES (31, 15, 21, '1', 1, 1, '2026-02-14 15:37:24');
INSERT INTO `chat_message` VALUES (32, 21, 15, '2', 1, 1, '2026-02-14 15:37:30');
INSERT INTO `chat_message` VALUES (33, 15, 21, '1', 1, 1, '2026-02-14 15:38:05');
INSERT INTO `chat_message` VALUES (34, 21, 15, '2', 1, 1, '2026-02-14 15:38:09');
INSERT INTO `chat_message` VALUES (35, 21, 15, '1', 1, 1, '2026-02-14 15:38:33');
INSERT INTO `chat_message` VALUES (36, 21, 15, '22', 1, 1, '2026-02-14 15:38:37');
INSERT INTO `chat_message` VALUES (37, 15, 21, '2', 1, 1, '2026-02-14 15:38:39');
INSERT INTO `chat_message` VALUES (38, 15, 21, '2', 1, 1, '2026-02-14 15:38:41');
INSERT INTO `chat_message` VALUES (39, 21, 15, '3', 1, 1, '2026-02-14 15:38:43');
INSERT INTO `chat_message` VALUES (40, 15, 21, '2', 1, 1, '2026-02-14 15:43:14');
INSERT INTO `chat_message` VALUES (41, 21, 15, '2', 1, 1, '2026-02-14 15:43:20');
INSERT INTO `chat_message` VALUES (42, 15, 21, '3', 1, 1, '2026-02-14 15:43:33');

-- ----------------------------
-- Table structure for guide_appointment
-- ----------------------------
DROP TABLE IF EXISTS `guide_appointment`;
CREATE TABLE `guide_appointment`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `appointment_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '预约编号',
  `user_id` int NULL DEFAULT NULL COMMENT '提交人ID',
  `patient_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '就诊人',
  `patient_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `symptoms` json NULL COMMENT '症状描述',
  `hospital_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `service_type_number` int NULL DEFAULT 1 COMMENT '服务类型编号',
  `service_date` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `service_start_time` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `service_end_time` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `other_requirement` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '其他需求',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_appt_no`(`appointment_no` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 27 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI导诊需求单' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of guide_appointment
-- ----------------------------
INSERT INTO `guide_appointment` VALUES (20, 'APP1770922235116a8fb22', NULL, '范涵伶', '15520765697', '[\"胸痛\", \"发热\"]', '成都市中医院', 1, '2026-02-13', '08:00', '11:30', '', '2026-02-13 02:50:35');
INSERT INTO `guide_appointment` VALUES (21, 'APP1770922278998e189ef', NULL, '范涵伶', '15520765697', '[\"发热\", \"呼吸困难\"]', '成都市中医院', 2, '2026-02-13', '08:00', '12:30', '', '2026-02-13 02:51:19');
INSERT INTO `guide_appointment` VALUES (22, 'APP1770922397654d36ae2', NULL, '范涵伶', '15520765697', '[\"发热\", \"呼吸困难\"]', '成都市中医院', 2, '2026-02-13', '08:00', '11:30', '', '2026-02-13 02:53:18');
INSERT INTO `guide_appointment` VALUES (23, 'APP17709669312087f92fe', NULL, '范涵伶', '15520765697', '[\"胸痛\"]', '成都市', 1, '2026-02-13', '08:00', '12:30', '', '2026-02-13 15:15:31');
INSERT INTO `guide_appointment` VALUES (24, 'APP1770967593071c3a251', NULL, '范涵伶', '15520765697', '[\"胸痛\"]', '成都市', 1, '2026-02-13', '08:00', '12:00', '', '2026-02-13 15:26:33');
INSERT INTO `guide_appointment` VALUES (25, 'APP177096940242960a46b', NULL, '范涵伶', '15520765697', '[\"头晕\"]', '成都', 1, '2026-02-13', '08:00', '09:30', '', '2026-02-13 15:56:42');
INSERT INTO `guide_appointment` VALUES (26, 'APP1770970621187010b5b', NULL, '范涵伶', '15520765697', '[\"胸痛\"]', '成都市', 1, '2026-02-13', '08:00', '12:30', '', '2026-02-13 16:17:01');

-- ----------------------------
-- Table structure for order
-- ----------------------------
DROP TABLE IF EXISTS `order`;
CREATE TABLE `order`  (
  `order_id` int NOT NULL AUTO_INCREMENT,
  `order_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '订单号',
  `user_id` int NOT NULL COMMENT '下单用户ID',
  `attendant_id` int NULL DEFAULT NULL COMMENT '接单陪诊师ID',
  `attendant_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '陪诊师姓名',
  `attendant_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '陪诊师电话',
  `patient_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '就诊人姓名',
  `patient_age` int NULL DEFAULT 0 COMMENT '就诊人年龄',
  `patient_sex` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '未知' COMMENT '就诊人性别',
  `contact_person` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `contact_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `hospital` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `service_content` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `clinic_type` int NULL DEFAULT 1,
  `service_date` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `service_time_slot` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `special_requirements` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '特殊需求',
  `custom_requirement` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '自定义需求',
  `order_amount` decimal(10, 2) NOT NULL DEFAULT 0.00,
  `payment_status` int NULL DEFAULT 0 COMMENT '0=待支付, 1=已支付',
  `payment_time` datetime NULL DEFAULT NULL,
  `order_status` int NULL DEFAULT 0 COMMENT '0=待支付, 1=待接单, 2=待服务, 3=服务中, 6=已完成, 7=已取消',
  `qr_code_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '核销二维码URL',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `service_start_time` datetime NULL DEFAULT NULL COMMENT '服务开始时间',
  `service_end_time` datetime NULL DEFAULT NULL COMMENT '服务结束时间',
  `actual_duration` decimal(5, 2) NULL DEFAULT NULL COMMENT '实际服务时长',
  `balance_amount` decimal(10, 2) NULL DEFAULT NULL COMMENT '差价金额',
  PRIMARY KEY (`order_id`) USING BTREE,
  UNIQUE INDEX `uk_order_no`(`order_no` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 25 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '正式订单表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of order
-- ----------------------------
INSERT INTO `order` VALUES (20, 'ORD1770922397749e14950', 15, 21, '李怀', NULL, '范涵伶', 22, '男', '范涵伶', '15520765697', '成都市中医院', '术后护理', 2, '2026-02-13', '08:00-11:30', '发热,呼吸困难', '无', 180.00, 1, '2026-02-13 02:53:20', 6, 'https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=SERVICE_CONFIRM_20', '2026-02-13 02:53:18', '2026-02-13 02:54:49', '2026-02-13 02:55:22', 2.00, NULL);
INSERT INTO `order` VALUES (21, 'ORD1770966931301a7ae31', 15, 21, '李怀', NULL, '范涵伶', 22, '男', '范涵伶', '15520765697', '成都市', '普通陪诊', 1, '2026-02-13', '08:00-12:30', '胸痛', '无', 140.00, 1, '2026-02-13 15:15:33', 6, 'https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=SERVICE_CONFIRM_21', '2026-02-13 15:15:31', '2026-02-13 15:17:18', '2026-02-13 15:21:59', 2.00, NULL);
INSERT INTO `order` VALUES (22, 'ORD17709675931199747d2', 15, 21, '李怀', NULL, '范涵伶', 22, '男', '范涵伶', '15520765697', '成都市', '普通陪诊', 1, '2026-02-13', '08:00-12:00', '胸痛', '无', 110.00, 1, '2026-02-13 15:26:35', 3, 'https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=SERVICE_CONFIRM_22', '2026-02-13 15:26:33', '2026-02-13 15:26:52', NULL, NULL, NULL);
INSERT INTO `order` VALUES (23, 'ORD177096940247987dfd3', 15, 21, '李怀', NULL, '范涵伶', 22, '男', '范涵伶', '15520765697', '成都', '普通陪诊', 1, '2026-02-13', '08:00-09:30', '头晕', '无', 50.00, 1, '2026-02-13 15:56:44', 3, 'https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=SERVICE_CONFIRM_23', '2026-02-13 15:56:42', '2026-02-13 16:16:38', NULL, NULL, NULL);
INSERT INTO `order` VALUES (24, 'ORD17709706212501c860c', 15, 21, '李怀', NULL, '范涵伶', 22, '男', '范涵伶', '15520765697', '成都市', '普通陪诊', 1, '2026-02-13', '08:00-12:30', '胸痛', '无', 140.00, 1, '2026-02-13 16:17:03', 2, 'https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=SERVICE_CONFIRM_24', '2026-02-13 16:17:01', NULL, NULL, NULL, NULL);

-- ----------------------------
-- Table structure for service_type_mapping
-- ----------------------------
DROP TABLE IF EXISTS `service_type_mapping`;
CREATE TABLE `service_type_mapping`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `service_type_number` int NOT NULL,
  `service_type_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `price_base` decimal(10, 2) NULL DEFAULT 0.00,
  `price_per_hour` decimal(10, 2) NULL DEFAULT 0.00,
  `is_active` tinyint(1) NULL DEFAULT 1,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `service_type_number`(`service_type_number` ASC) USING BTREE,
  INDEX `idx_service_type_number`(`service_type_number` ASC) USING BTREE,
  INDEX `idx_is_active`(`is_active` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '服务类型映射表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of service_type_mapping
-- ----------------------------
INSERT INTO `service_type_mapping` VALUES (1, 1, '普通陪诊', '基础陪诊服务，包括陪同就诊、取药等', 50.00, 30.00, 1, '2026-02-09 18:36:02', '2026-02-09 18:36:02');
INSERT INTO `service_type_mapping` VALUES (2, 2, '术后护理', '专业术后护理服务', 0.00, 45.00, 1, '2026-02-09 18:36:02', '2026-02-09 18:36:02');
INSERT INTO `service_type_mapping` VALUES (3, 3, '急诊陪同', '急诊情况下的陪诊服务', 50.00, 30.00, 1, '2026-02-09 18:36:02', '2026-02-09 18:36:02');
INSERT INTO `service_type_mapping` VALUES (4, 4, '上门陪诊', '上门提供陪诊服务', 50.00, 30.00, 1, '2026-02-09 18:36:02', '2026-02-09 18:36:02');

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '登录用户名',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '登录密码',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '真实姓名/昵称',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '手机号',
  `sex` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '未知' COMMENT '性别',
  `age` int NULL DEFAULT 0 COMMENT '年龄',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '头像URL',
  `user_type` int NOT NULL DEFAULT 0 COMMENT '角色：0=普通用户, 1=陪诊师, 2=管理员',
  `openid` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '微信OpenID',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_username`(`username` ASC) USING BTREE,
  UNIQUE INDEX `uk_phone`(`phone` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 30 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '基础用户表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of user
-- ----------------------------
INSERT INTO `user` VALUES (1, 'sasa', 'testpassword', 'sara', '13812345678', '女', 25, NULL, 0, 'o123abc456def', '2026-02-12 01:55:08');
INSERT INTO `user` VALUES (9, '小明', 'testpassword', '小明', '13987654321', '男', 25, NULL, 0, 'o789xyz012uvw', '2026-02-12 01:55:08');
INSERT INTO `user` VALUES (10, '小小', '123456', '小小', '13765432109', '女', 36, NULL, 1, 'oabc123def456', '2026-02-12 01:55:08');
INSERT INTO `user` VALUES (11, '小王', '123456', '小王', '13698765432', '男', 25, '', 1, 'odef789ghi012', '2026-02-12 01:55:08');
INSERT INTO `user` VALUES (13, 'liufang', 'liufang', '刘芳', '13806580001', '女', 32, NULL, 1, NULL, '2026-02-12 01:55:08');
INSERT INTO `user` VALUES (15, 'fanfan', '123456', '范涵伶', '15520765697', '男', 22, NULL, 0, NULL, '2026-02-12 01:55:08');
INSERT INTO `user` VALUES (20, 'patient_test', '123456', '张三', '13800138001', '男', 30, NULL, 0, NULL, '2026-02-12 01:55:08');
INSERT INTO `user` VALUES (21, 'lihuai', '123456', '李怀', '13900139002', '男', 35, '/uploads/03_Medicalcompanion.jpg', 1, NULL, '2026-02-12 01:55:08');
INSERT INTO `user` VALUES (22, 'attendant002', '加密后的密码2', '李四', '13800138002', '女', 28, '/uploads/01_Medicalcompanion.jpg', 1, NULL, '2025-07-01 10:06:05');
INSERT INTO `user` VALUES (23, 'attendant003', '加密后的密码3', '王五', '13800138003', '男', 35, '/uploads/04_Medicalcompanion.jpg', 1, NULL, '2025-07-01 10:22:35');
INSERT INTO `user` VALUES (24, 'attendant123', '加密后的密码', '小张', '13800138000', '女', 30, '/uploads/02_Medicalcompanion.jpg', 1, NULL, '2025-07-02 11:06:37');
INSERT INTO `user` VALUES (25, 'youning', '123456', '优宁', '13899990001', '女', 30, '/uploads/03_Medicalcompanion.jpg', 1, NULL, '2025-11-29 22:09:24');
INSERT INTO `user` VALUES (26, 'attendant_temp_02', '123456', '张三', '13899990002', '女', 30, '/uploads/04_Medicalcompanion.jpg', 1, NULL, '2025-12-15 21:24:59');

-- ----------------------------
-- Procedure structure for AddColumnsToGuideAppointment
-- ----------------------------
DROP PROCEDURE IF EXISTS `AddColumnsToGuideAppointment`;
delimiter ;;
CREATE PROCEDURE `AddColumnsToGuideAppointment`()
BEGIN
    -- 检查并添加 hospital_name
    IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'guide_appointment' AND COLUMN_NAME = 'hospital_name') THEN
        ALTER TABLE guide_appointment ADD COLUMN hospital_name VARCHAR(100) DEFAULT '' NOT NULL COMMENT '医院名称' AFTER id;
    END IF;
    
    -- 检查并添加 service_date（给默认值，避免已有数据报错）
    IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'guide_appointment' AND COLUMN_NAME = 'service_date') THEN
        ALTER TABLE guide_appointment ADD COLUMN service_date DATE DEFAULT '1970-01-01' NOT NULL COMMENT '服务日期' AFTER hospital_name;
    END IF;
    
    -- 检查并添加 service_start_time
    IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'guide_appointment' AND COLUMN_NAME = 'service_start_time') THEN
        ALTER TABLE guide_appointment ADD COLUMN service_start_time TIME DEFAULT '00:00:00' NOT NULL COMMENT '服务开始时间' AFTER service_date;
    END IF;
    
    -- 检查并添加 service_end_time
    IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'guide_appointment' AND COLUMN_NAME = 'service_end_time') THEN
        ALTER TABLE guide_appointment ADD COLUMN service_end_time TIME DEFAULT '00:00:00' NOT NULL COMMENT '服务结束时间' AFTER service_start_time;
    END IF;
    
    -- 检查并添加 service_type_number
    IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'guide_appointment' AND COLUMN_NAME = 'service_type_number') THEN
        ALTER TABLE guide_appointment ADD COLUMN service_type_number INT DEFAULT 0 NOT NULL COMMENT '服务类型编号' AFTER service_end_time;
    END IF;
    
    -- 检查并添加 symptoms（JSON类型，允许为空）
    IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'guide_appointment' AND COLUMN_NAME = 'symptoms') THEN
        ALTER TABLE guide_appointment ADD COLUMN symptoms JSON COMMENT '症状列表' AFTER service_type_number;
    END IF;
    
    -- 检查并添加 other_requirement
    IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'guide_appointment' AND COLUMN_NAME = 'other_requirement') THEN
        ALTER TABLE guide_appointment ADD COLUMN other_requirement TEXT COMMENT '其他需求' AFTER symptoms;
    END IF;
END
;;
delimiter ;

SET FOREIGN_KEY_CHECKS = 1;

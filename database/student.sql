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

 Date: 28/02/2026 16:20:55
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
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI医疗问答记录表' ROW_FORMAT = DYNAMIC;

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
) ENGINE = InnoDB AUTO_INCREMENT = 367 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '聊天消息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of chat_message
-- ----------------------------
INSERT INTO `chat_message` VALUES (266, 0, 15, '恭喜您!订单No.ORD17720940252105599f3支付完成，我们已通知陪诊师为您服务。陪诊师将在30分钟内与您联系，请保持电话畅通。', 1, 1, '2026-02-26 16:20:27');
INSERT INTO `chat_message` VALUES (267, 0, 15, '您预约的(02月26日)12:30-18:00有成都市人民医院的就诊安排，陪诊师李怀已接单。请携带身份证、医保卡及相关检查报告。', 1, 1, '2026-02-26 16:20:36');
INSERT INTO `chat_message` VALUES (268, 21, 15, '您好！我是陪诊师李怀，很高兴为您服务。我会尽快与您联系确认服务细节。', 1, 1, '2026-02-26 16:20:36');
INSERT INTO `chat_message` VALUES (269, 0, 15, '您的订单No.ORD17720940252105599f3服务已开始。陪诊师已到达指定位置，请准备就诊。', 1, 1, '2026-02-26 16:21:00');
INSERT INTO `chat_message` VALUES (270, 0, 21, '您已开始为订单 ORD17720940252105599f3 提供服务，请按时完成服务。', 1, 1, '2026-02-26 16:21:00');
INSERT INTO `chat_message` VALUES (271, 0, 15, '您的陪诊服务(订单No.ORD17720940252105599f3)已结束，请确认本次服务时长和费用（多退少补）。', 1, 1, '2026-02-26 16:21:35');
INSERT INTO `chat_message` VALUES (272, 0, 21, '您已结束订单 ORD17720940252105599f3 的服务，请提醒用户确认时长与费用。', 1, 1, '2026-02-26 16:21:35');
INSERT INTO `chat_message` VALUES (273, 0, 15, '您已确认本次陪诊服务时长与费用，订单已完成。', 1, 1, '2026-02-26 16:21:41');
INSERT INTO `chat_message` VALUES (274, 0, 21, '用户已确认订单 ORD17720940252105599f3 的时长与费用，订单已完成。', 1, 1, '2026-02-26 16:21:41');
INSERT INTO `chat_message` VALUES (275, 0, 15, '恭喜您!订单No.ORD17720947028516784c9支付完成，我们已通知陪诊师为您服务。陪诊师将在30分钟内与您联系，请保持电话畅通。', 1, 1, '2026-02-26 16:31:44');
INSERT INTO `chat_message` VALUES (276, 0, 15, '您预约的(02月27日)08:00-14:00有成都市中医院的就诊安排，陪诊师李怀已接单。请携带身份证、医保卡及相关检查报告。', 1, 1, '2026-02-26 16:32:08');
INSERT INTO `chat_message` VALUES (277, 21, 15, '您好！我是陪诊师李怀，很高兴为您服务。我会尽快与您联系确认服务细节。', 1, 1, '2026-02-26 16:32:08');
INSERT INTO `chat_message` VALUES (278, 0, 15, '您的订单No.ORD17720947028516784c9服务已开始。陪诊师已到达指定位置，请准备就诊。', 1, 1, '2026-02-26 16:32:35');
INSERT INTO `chat_message` VALUES (279, 0, 21, '您已开始为订单 ORD17720947028516784c9 提供服务，请按时完成服务。', 1, 1, '2026-02-26 16:32:35');
INSERT INTO `chat_message` VALUES (280, 0, 15, '您的陪诊服务(订单No.ORD17720947028516784c9)已结束，请确认本次服务时长和费用（多退少补）。', 1, 1, '2026-02-26 16:32:59');
INSERT INTO `chat_message` VALUES (281, 0, 21, '您已结束订单 ORD17720947028516784c9 的服务，请提醒用户确认时长与费用。', 1, 1, '2026-02-26 16:32:59');
INSERT INTO `chat_message` VALUES (282, 0, 15, '您已确认本次陪诊服务时长与费用，订单已完成。', 1, 1, '2026-02-26 16:33:07');
INSERT INTO `chat_message` VALUES (283, 0, 21, '用户已确认订单 ORD17720947028516784c9 的时长与费用，订单已完成。', 1, 1, '2026-02-26 16:33:07');
INSERT INTO `chat_message` VALUES (284, 0, 15, '恭喜您!订单No.ORD1772167293352229bec支付完成，我们已通知陪诊师为您服务。陪诊师将在30分钟内与您联系，请保持电话畅通。', 1, 1, '2026-02-27 12:43:16');
INSERT INTO `chat_message` VALUES (285, 0, 15, '您已成功创建订单 ORD1772168983757104052，请在15分钟内完成支付，逾期系统将自动取消订单。', 1, 1, '2026-02-27 13:09:43');
INSERT INTO `chat_message` VALUES (286, 0, 15, '您已成功创建订单 ORD17721696183899bc4ab，请在15分钟内完成支付，逾期系统将自动取消订单。', 1, 1, '2026-02-27 13:20:18');
INSERT INTO `chat_message` VALUES (287, 0, 15, '您已成功创建订单 ORD177216971527054773c，请在15分钟内完成支付，逾期系统将自动取消订单。', 1, 1, '2026-02-27 13:21:55');
INSERT INTO `chat_message` VALUES (288, 0, 15, '您已成功创建订单 ORD1772170307910028ccb，请在15分钟内完成支付，逾期系统将自动取消订单。', 1, 1, '2026-02-27 13:31:47');
INSERT INTO `chat_message` VALUES (289, 0, 15, '您的订单ORD17721684470500a92ae已取消。取消原因：超时未支付自动取消', 1, 1, '2026-02-27 14:55:56');
INSERT INTO `chat_message` VALUES (290, 0, 15, '您已成功创建订单 ORD177217538128245f6c9，请在15分钟内完成支付，逾期系统将自动取消订单。', 1, 1, '2026-02-27 14:56:21');
INSERT INTO `chat_message` VALUES (291, 0, 15, '恭喜您!订单No.ORD177217538128245f6c9支付完成，我们已通知陪诊师为您服务。陪诊师将在30分钟内与您联系，请保持电话畅通。', 1, 1, '2026-02-27 14:56:36');
INSERT INTO `chat_message` VALUES (292, 0, 15, '您已成功创建订单 ORD17721767672500e0d4f，请在15分钟内完成支付，逾期系统将自动取消订单。', 1, 1, '2026-02-27 15:19:27');
INSERT INTO `chat_message` VALUES (293, 0, 15, '恭喜您!订单No.ORD17721767672500e0d4f支付完成，我们已通知陪诊师为您服务。陪诊师将在30分钟内与您联系，请保持电话畅通。', 1, 1, '2026-02-27 15:19:29');
INSERT INTO `chat_message` VALUES (294, 0, 15, '您已成功创建订单 ORD17721772386768f50f7，请在15分钟内完成支付，逾期系统将自动取消订单。', 1, 1, '2026-02-27 15:27:18');
INSERT INTO `chat_message` VALUES (295, 0, 15, '恭喜您!订单No.ORD17721772386768f50f7支付完成，我们已通知陪诊师为您服务。陪诊师将在30分钟内与您联系，请保持电话畅通。', 1, 1, '2026-02-27 15:27:20');
INSERT INTO `chat_message` VALUES (296, 0, 15, '您已成功创建订单 ORD1772177339845b88d27，请在15分钟内完成支付，逾期系统将自动取消订单。', 1, 1, '2026-02-27 15:28:59');
INSERT INTO `chat_message` VALUES (297, 0, 15, '恭喜您!订单No.ORD1772177339845b88d27支付完成，我们已通知陪诊师为您服务。陪诊师将在30分钟内与您联系，请保持电话畅通。', 1, 1, '2026-02-27 15:29:02');
INSERT INTO `chat_message` VALUES (298, 0, 15, '您已成功创建订单 ORD1772184911870ee26b8，请在15分钟内完成支付，逾期系统将自动取消订单。', 1, 1, '2026-02-27 17:35:11');
INSERT INTO `chat_message` VALUES (299, 0, 15, '恭喜您!订单No.ORD1772184911870ee26b8支付完成，我们已通知陪诊师为您服务。陪诊师将在30分钟内与您联系，请保持电话畅通。', 1, 1, '2026-02-27 17:35:13');
INSERT INTO `chat_message` VALUES (300, 0, 15, '您预约的(02月27日)08:00-12:30有cda的就诊安排，陪诊师李怀已接单。请携带身份证、医保卡及相关检查报告。', 1, 1, '2026-02-27 17:36:37');
INSERT INTO `chat_message` VALUES (301, 21, 15, '您好！我是陪诊师李怀，很高兴为您服务。我会尽快与您联系确认服务细节。', 1, 1, '2026-02-27 17:36:37');
INSERT INTO `chat_message` VALUES (302, 0, 15, '您的订单No.ORD1772184911870ee26b8服务已开始。陪诊师已到达指定位置，请准备就诊。', 1, 1, '2026-02-27 17:36:52');
INSERT INTO `chat_message` VALUES (303, 0, 21, '您已开始为订单 ORD1772184911870ee26b8 提供服务，请按时完成服务。', 1, 1, '2026-02-27 17:36:52');
INSERT INTO `chat_message` VALUES (304, 0, 15, '您的陪诊服务(订单No.ORD1772184911870ee26b8)已结束，请确认本次服务时长和费用（多退少补）。', 1, 1, '2026-02-27 17:37:02');
INSERT INTO `chat_message` VALUES (305, 0, 21, '您已结束订单 ORD1772184911870ee26b8 的服务，请提醒用户确认时长与费用。', 1, 1, '2026-02-27 17:37:02');
INSERT INTO `chat_message` VALUES (306, 0, 15, '您已确认本次陪诊服务时长与费用，订单已完成。', 1, 1, '2026-02-27 17:37:39');
INSERT INTO `chat_message` VALUES (307, 0, 21, '用户已确认订单 ORD1772184911870ee26b8 的时长与费用，订单已完成。', 1, 1, '2026-02-27 17:37:39');
INSERT INTO `chat_message` VALUES (308, 0, 15, '您已成功创建订单 ORD17721851897289aebd7，请在15分钟内完成支付，逾期系统将自动取消订单。', 1, 1, '2026-02-27 17:39:49');
INSERT INTO `chat_message` VALUES (309, 0, 15, '恭喜您!订单No.ORD17721851897289aebd7支付完成，我们已通知陪诊师为您服务。陪诊师将在30分钟内与您联系，请保持电话畅通。', 1, 1, '2026-02-27 17:39:52');
INSERT INTO `chat_message` VALUES (310, 0, 15, '您已成功创建订单 ORD1772185891805b1f712，请在15分钟内完成支付，逾期系统将自动取消订单。', 1, 1, '2026-02-27 17:51:31');
INSERT INTO `chat_message` VALUES (311, 0, 15, '恭喜您!订单No.ORD1772185891805b1f712支付完成，我们已通知陪诊师为您服务。陪诊师将在30分钟内与您联系，请保持电话畅通。', 1, 1, '2026-02-27 17:51:32');
INSERT INTO `chat_message` VALUES (312, 0, 15, '您预约的(02月27日)08:00-12:00有成都市的就诊安排，陪诊师李怀已接单。请携带身份证、医保卡及相关检查报告。', 1, 1, '2026-02-27 17:54:55');
INSERT INTO `chat_message` VALUES (313, 21, 15, '您好！我是陪诊师李怀，很高兴为您服务。我会尽快与您联系确认服务细节。', 1, 1, '2026-02-27 17:54:55');
INSERT INTO `chat_message` VALUES (314, 0, 15, '您的订单No.ORD17721851897289aebd7服务已开始。陪诊师已到达指定位置，请准备就诊。', 1, 1, '2026-02-27 17:55:17');
INSERT INTO `chat_message` VALUES (315, 0, 21, '您已开始为订单 ORD17721851897289aebd7 提供服务，请按时完成服务。', 1, 0, '2026-02-27 17:55:17');
INSERT INTO `chat_message` VALUES (316, 0, 15, '您的陪诊服务(订单No.ORD17721851897289aebd7)已结束，请确认本次服务时长和费用（多退少补）。', 1, 1, '2026-02-27 17:55:22');
INSERT INTO `chat_message` VALUES (317, 0, 21, '您已结束订单 ORD17721851897289aebd7 的服务，请提醒用户确认时长与费用。', 1, 0, '2026-02-27 17:55:22');
INSERT INTO `chat_message` VALUES (318, 0, 15, '您已成功创建订单 ORD17721864203592d5f4c，请在15分钟内完成支付，逾期系统将自动取消订单。', 1, 1, '2026-02-27 18:00:20');
INSERT INTO `chat_message` VALUES (319, 0, 15, '恭喜您!订单No.ORD17721864203592d5f4c支付完成，我们已通知陪诊师为您服务。陪诊师将在30分钟内与您联系，请保持电话畅通。', 1, 1, '2026-02-27 18:00:21');
INSERT INTO `chat_message` VALUES (320, 0, 15, '您已成功创建订单 ORD17721864910689bda5e，请在15分钟内完成支付，逾期系统将自动取消订单。', 1, 1, '2026-02-27 18:01:31');
INSERT INTO `chat_message` VALUES (321, 0, 15, '您已成功创建订单 ORD1772186820885b73960，请在15分钟内完成支付，逾期系统将自动取消订单。', 1, 1, '2026-02-27 18:07:00');
INSERT INTO `chat_message` VALUES (322, 0, 15, '您已成功创建订单 ORD17721883993729bc4e8，请在15分钟内完成预付款，逾期系统将自动取消订单。', 1, 1, '2026-02-27 18:33:19');
INSERT INTO `chat_message` VALUES (323, 0, 15, '恭喜您!订单No.ORD17721883993729bc4e8支付完成，我们已通知陪诊师为您服务。陪诊师将在30分钟内与您联系，请保持电话畅通。', 1, 1, '2026-02-27 18:33:26');
INSERT INTO `chat_message` VALUES (324, 0, 15, '您预约的(02月27日)08:00-11:00有成都市的就诊安排，陪诊师李怀已接单。请携带身份证、医保卡及相关检查报告。', 1, 1, '2026-02-27 18:34:08');
INSERT INTO `chat_message` VALUES (325, 21, 15, '您好！我是陪诊师李怀，很高兴为您服务。我会尽快与您联系确认服务细节。', 1, 1, '2026-02-27 18:34:08');
INSERT INTO `chat_message` VALUES (326, 0, 15, '您的订单No.ORD17721883993729bc4e8服务已开始。陪诊师已到达指定位置，请准备就诊。', 1, 1, '2026-02-27 18:34:14');
INSERT INTO `chat_message` VALUES (327, 0, 21, '您已开始为订单 ORD17721883993729bc4e8 提供服务，请按时完成服务。', 1, 0, '2026-02-27 18:34:14');
INSERT INTO `chat_message` VALUES (328, 0, 15, '您的陪诊服务(订单No.ORD17721883993729bc4e8)已结束，请确认本次服务时长和费用（多退少补）。', 1, 1, '2026-02-27 18:34:17');
INSERT INTO `chat_message` VALUES (329, 0, 21, '您已结束订单 ORD17721883993729bc4e8 的服务，请提醒用户确认时长与费用。', 1, 0, '2026-02-27 18:34:17');
INSERT INTO `chat_message` VALUES (330, 0, 15, '您已确认本次陪诊服务时长与费用，订单已完成。', 1, 1, '2026-02-27 18:34:33');
INSERT INTO `chat_message` VALUES (331, 0, 21, '用户已确认订单 ORD17721883993729bc4e8 的时长与费用，订单已完成。', 1, 0, '2026-02-27 18:34:33');
INSERT INTO `chat_message` VALUES (332, 0, 15, '您预约的(02月27日)08:00-11:00有成都市的就诊安排，陪诊师李怀已接单。请携带身份证、医保卡及相关检查报告。', 1, 1, '2026-02-27 18:35:52');
INSERT INTO `chat_message` VALUES (333, 21, 15, '您好！我是陪诊师李怀，很高兴为您服务。我会尽快与您联系确认服务细节。', 1, 1, '2026-02-27 18:35:52');
INSERT INTO `chat_message` VALUES (334, 0, 15, '您的订单No.ORD17721864203592d5f4c服务已开始。陪诊师已到达指定位置，请准备就诊。', 1, 1, '2026-02-27 18:36:00');
INSERT INTO `chat_message` VALUES (335, 0, 21, '您已开始为订单 ORD17721864203592d5f4c 提供服务，请按时完成服务。', 1, 0, '2026-02-27 18:36:00');
INSERT INTO `chat_message` VALUES (336, 0, 15, '您预约的(02月27日)08:00-12:00有成都市的就诊安排，陪诊师李怀已接单。请携带身份证、医保卡及相关检查报告。', 1, 1, '2026-02-27 18:36:35');
INSERT INTO `chat_message` VALUES (337, 21, 15, '您好！我是陪诊师李怀，很高兴为您服务。我会尽快与您联系确认服务细节。', 1, 1, '2026-02-27 18:36:35');
INSERT INTO `chat_message` VALUES (338, 0, 15, '您的订单No.ORD1772185891805b1f712服务已开始。陪诊师已到达指定位置，请准备就诊。', 1, 1, '2026-02-27 18:36:44');
INSERT INTO `chat_message` VALUES (339, 0, 21, '您已开始为订单 ORD1772185891805b1f712 提供服务，请按时完成服务。', 1, 0, '2026-02-27 18:36:44');
INSERT INTO `chat_message` VALUES (340, 0, 15, '您的陪诊服务(订单No.ORD1772185891805b1f712)已结束，请确认本次服务时长和费用（多退少补）。', 1, 1, '2026-02-27 18:41:33');
INSERT INTO `chat_message` VALUES (341, 0, 21, '您已结束订单 ORD1772185891805b1f712 的服务，请提醒用户确认时长与费用。', 1, 0, '2026-02-27 18:41:33');
INSERT INTO `chat_message` VALUES (342, 0, 15, '您的订单ORD1772186820885b73960已取消。取消原因：超时未支付自动取消', 1, 1, '2026-02-27 18:41:39');
INSERT INTO `chat_message` VALUES (343, 0, 15, '您的订单ORD17721864910689bda5e已取消。取消原因：超时未支付自动取消', 1, 1, '2026-02-27 18:41:42');
INSERT INTO `chat_message` VALUES (344, 0, 15, '您已成功创建订单 ORD1772191228942f306f5，请在15分钟内完成预付款，逾期系统将自动取消订单。', 1, 1, '2026-02-27 19:20:28');
INSERT INTO `chat_message` VALUES (345, 0, 15, '恭喜您!订单No.ORD1772191228942f306f5支付完成，我们已通知陪诊师为您服务。陪诊师将在30分钟内与您联系，请保持电话畅通。', 1, 1, '2026-02-27 19:20:32');
INSERT INTO `chat_message` VALUES (346, 0, 15, '您预约的(02月27日)08:00-11:00有成都市的就诊安排，陪诊师李怀已接单。请携带身份证、医保卡及相关检查报告。', 1, 1, '2026-02-27 19:21:31');
INSERT INTO `chat_message` VALUES (347, 21, 15, '您好！我是陪诊师李怀，很高兴为您服务。我会尽快与您联系确认服务细节。', 1, 1, '2026-02-27 19:21:31');
INSERT INTO `chat_message` VALUES (348, 0, 15, '您的订单No.ORD1772191228942f306f5服务已开始。陪诊师已到达指定位置，请准备就诊。', 1, 1, '2026-02-27 19:21:38');
INSERT INTO `chat_message` VALUES (349, 0, 21, '您已开始为订单 ORD1772191228942f306f5 提供服务，请按时完成服务。', 1, 0, '2026-02-27 19:21:38');
INSERT INTO `chat_message` VALUES (350, 0, 15, '您的陪诊服务(订单No.ORD1772191228942f306f5)已结束，请确认本次服务时长和费用（多退少补）。', 1, 1, '2026-02-27 19:26:06');
INSERT INTO `chat_message` VALUES (351, 0, 21, '您已结束订单 ORD1772191228942f306f5 的服务，请提醒用户确认时长与费用。', 1, 0, '2026-02-27 19:26:06');
INSERT INTO `chat_message` VALUES (352, 0, 15, '您已确认本次陪诊服务时长与费用，订单已完成。', 1, 1, '2026-02-27 19:29:33');
INSERT INTO `chat_message` VALUES (353, 0, 21, '用户已确认订单 ORD1772191228942f306f5 的时长与费用，订单已完成。', 1, 0, '2026-02-27 19:29:33');
INSERT INTO `chat_message` VALUES (354, 0, 15, '您已确认本次陪诊服务时长与费用，订单已完成。', 1, 1, '2026-02-28 01:06:12');
INSERT INTO `chat_message` VALUES (355, 0, 21, '用户已确认订单 ORD1772185891805b1f712 的时长与费用，订单已完成。', 1, 0, '2026-02-28 01:06:12');
INSERT INTO `chat_message` VALUES (356, 0, 15, '您预约的(02月27日)08:00-10:30有成都市的就诊安排，陪诊师李怀已接单。请携带身份证、医保卡及相关检查报告。', 1, 1, '2026-02-28 01:08:44');
INSERT INTO `chat_message` VALUES (357, 21, 15, '您好！我是陪诊师李怀，很高兴为您服务。我会尽快与您联系确认服务细节。', 1, 1, '2026-02-28 01:08:44');
INSERT INTO `chat_message` VALUES (358, 0, 15, '您的陪诊服务(订单No.ORD17721864203592d5f4c)已结束，请确认本次服务时长和费用（多退少补）。', 1, 1, '2026-02-28 01:08:57');
INSERT INTO `chat_message` VALUES (359, 0, 21, '您已结束订单 ORD17721864203592d5f4c 的服务，请提醒用户确认时长与费用。', 1, 0, '2026-02-28 01:08:57');
INSERT INTO `chat_message` VALUES (360, 0, 15, '您已确认本次陪诊服务时长与费用，订单已完成。', 1, 1, '2026-02-28 01:19:10');
INSERT INTO `chat_message` VALUES (361, 0, 21, '用户已确认订单 ORD17721864203592d5f4c 的时长与费用，订单已完成。', 1, 0, '2026-02-28 01:19:10');
INSERT INTO `chat_message` VALUES (362, 0, 15, '您已成功创建订单 ORD1772262390131755423，请在15分钟内完成预付款，逾期系统将自动取消订单。', 1, 1, '2026-02-28 15:06:30');
INSERT INTO `chat_message` VALUES (363, 0, 15, '恭喜您!订单No.ORD1772262390131755423支付完成，我们已通知陪诊师为您服务。陪诊师将在30分钟内与您联系，请保持电话畅通。', 1, 1, '2026-02-28 15:06:41');
INSERT INTO `chat_message` VALUES (364, 0, 15, '您的订单ORD1772262390131755423已取消。取消原因：计划有变，暂不就诊', 1, 1, '2026-02-28 15:52:07');
INSERT INTO `chat_message` VALUES (365, 0, 15, '您的订单No.ORD1772177339845b88d27服务已开始。陪诊师已到达指定位置，请准备就诊。', 1, 1, '2026-02-28 15:52:58');
INSERT INTO `chat_message` VALUES (366, 0, 21, '您已开始为订单 ORD1772177339845b88d27 提供服务，请按时完成服务。', 1, 0, '2026-02-28 15:52:58');

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
) ENGINE = InnoDB AUTO_INCREMENT = 60 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI导诊需求单' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of guide_appointment
-- ----------------------------
INSERT INTO `guide_appointment` VALUES (47, 'APP177217538122848db2c', NULL, '范涵伶', '15520765697', '[\"发热\"]', '成都市', 3, '2026-02-27', '08:00', '10:30', '', '2026-02-27 14:56:21');
INSERT INTO `guide_appointment` VALUES (48, 'APP1772176767195ecf6f7', NULL, '范涵伶', '15520765697', '[\"胸痛\"]', '成都市', 1, '2026-02-27', '08:00', '10:00', '', '2026-02-27 15:19:27');
INSERT INTO `guide_appointment` VALUES (49, 'APP17721772386522ac459', NULL, '范涵伶', '15520765697', '[]', '成都', 1, '2026-02-27', '08:00', '09:30', '', '2026-02-27 15:27:19');
INSERT INTO `guide_appointment` VALUES (50, 'APP1772177339818d6120c', NULL, '范涵伶', '15520765697', '[\"胸痛\"]', '成都市', 1, '2026-02-27', '08:00', '10:30', '', '2026-02-27 15:29:00');
INSERT INTO `guide_appointment` VALUES (51, 'APP1772184911807e9d59e', NULL, '范涵伶', '15520765697', '[\"恶心呕吐\"]', 'cda', 1, '2026-02-27', '08:00', '12:30', '', '2026-02-27 17:35:12');
INSERT INTO `guide_appointment` VALUES (52, 'APP17721851896979a985d', NULL, '范涵伶', '15520765697', '[]', '成都市', 1, '2026-02-27', '08:00', '12:00', '', '2026-02-27 17:39:50');
INSERT INTO `guide_appointment` VALUES (53, 'APP1772185891746594f49', NULL, '范涵伶', '15520765697', '[\"胸痛\"]', '成都市', 1, '2026-02-27', '08:00', '12:00', '', '2026-02-27 17:51:32');
INSERT INTO `guide_appointment` VALUES (54, 'APP177218642031426bb39', NULL, '范涵伶', '15520765697', '[\"腹痛\"]', '成都市', 1, '2026-02-27', '08:00', '11:00', '', '2026-02-27 18:00:20');
INSERT INTO `guide_appointment` VALUES (55, 'APP1772186491023186247', NULL, '范涵伶', '15520765697', '[\"胸痛\"]', '成都市', 1, '2026-02-27', '08:00', '10:00', '', '2026-02-27 18:01:31');
INSERT INTO `guide_appointment` VALUES (56, 'APP1772186820807f55fd5', NULL, '范涵伶', '15520765697', '[\"胸痛\"]', '成都市', 1, '2026-02-27', '08:00', '10:30', '', '2026-02-27 18:07:01');
INSERT INTO `guide_appointment` VALUES (57, 'APP1772188399298b3f3bb', NULL, '范涵伶', '15520765697', '[\"胸痛\"]', '成都市', 3, '2026-02-27', '08:00', '11:00', '', '2026-02-27 18:33:19');
INSERT INTO `guide_appointment` VALUES (58, 'APP17721912288718621a7', NULL, '范涵伶', '15520765697', '[\"胸痛\"]', '成都市', 3, '2026-02-27', '08:00', '11:00', '', '2026-02-27 19:20:29');
INSERT INTO `guide_appointment` VALUES (59, 'APP17722623900267c0721', NULL, '范涵伶', '15520765697', '[\"发热\"]', '成都市', 4, '2026-02-28', '08:00', '11:00', '', '2026-02-28 15:06:30');

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
  `order_status` int NULL DEFAULT 0 COMMENT '0=待支付, 1=待接单, 2=待服务, 3=服务中, 4=待确认时长费用, 5=时长费用有争议, 6=已完成, 7=已取消',
  `cancel_reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '取消原因',
  `cancel_time` datetime NULL DEFAULT NULL COMMENT '取消时间',
  `cancel_by` tinyint NULL DEFAULT NULL COMMENT '取消方：0用户 1陪诊师 2系统',
  `penalty_rate` decimal(5, 2) NULL DEFAULT NULL COMMENT '违约金比例(0-1)',
  `penalty_amount` decimal(10, 2) NULL DEFAULT NULL COMMENT '违约金金额',
  `refund_amount` decimal(10, 2) NULL DEFAULT NULL COMMENT '退款金额',
  `qr_code_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '核销二维码URL',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `service_start_time` datetime NULL DEFAULT NULL COMMENT '服务开始时间',
  `service_end_time` datetime NULL DEFAULT NULL COMMENT '服务结束时间',
  `service_progress_step` tinyint NULL DEFAULT NULL COMMENT '服务进度：1=已到院, 2=候诊中, 3=检查中, 4=就诊完成',
  `estimated_duration` decimal(5, 2) NULL DEFAULT NULL COMMENT '预估服务时长(小时)',
  `actual_duration` decimal(5, 2) NULL DEFAULT NULL COMMENT '实际服务时长(小时)',
  `balance_amount` decimal(10, 2) NULL DEFAULT NULL COMMENT '差价金额（正数需补付，负数自动退款）',
  `time_dispute_user_duration` decimal(5, 2) NULL DEFAULT NULL COMMENT '用户申诉的实际时长(小时)',
  `time_dispute_reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '用户申诉说明',
  PRIMARY KEY (`order_id`) USING BTREE,
  UNIQUE INDEX `uk_order_no`(`order_no` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 58 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '正式订单表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of order
-- ----------------------------
INSERT INTO `order` VALUES (45, 'ORD177217538128245f6c9', 15, NULL, NULL, NULL, '范涵伶', 22, '男', '范涵伶', '15520765697', '成都市', '急诊陪同', 3, '2026-02-27', '08:00-10:30', '发热', '无', 180.00, 1, '2026-02-27 14:56:37', 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2026-02-27 14:56:21', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `order` VALUES (46, 'ORD17721767672500e0d4f', 15, NULL, NULL, NULL, '范涵伶', 22, '男', '范涵伶', '15520765697', '成都市', '普通陪诊', 1, '2026-02-27', '08:00-10:00', '胸痛', '无', 50.00, 1, '2026-02-27 15:19:29', 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2026-02-27 15:19:27', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `order` VALUES (47, 'ORD17721772386768f50f7', 15, NULL, NULL, NULL, '范涵伶', 22, '男', '范涵伶', '15520765697', '成都', '普通陪诊', 1, '2026-02-27', '08:00-09:30', NULL, '无', 50.00, 1, '2026-02-27 15:27:20', 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2026-02-27 15:27:19', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `order` VALUES (48, 'ORD1772177339845b88d27', 15, 21, '李怀', NULL, '范涵伶', 22, '男', '范涵伶', '15520765697', '成都市', '普通陪诊', 1, '2026-02-27', '08:00-10:30', '胸痛', '无', 80.00, 1, '2026-02-27 15:29:02', 3, NULL, NULL, NULL, NULL, NULL, NULL, 'https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=SERVICE_CONFIRM_48', '2026-02-27 15:29:00', '2026-02-28 15:52:59', NULL, 4, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `order` VALUES (49, 'ORD1772184911870ee26b8', 15, 21, '李怀', NULL, '范涵伶', 22, '男', '范涵伶', '15520765697', 'cda', '普通陪诊', 1, '2026-02-27', '08:00-12:30', '恶心呕吐', '无', 170.00, 1, '2026-02-27 17:35:13', 6, NULL, NULL, NULL, NULL, NULL, NULL, 'https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=SERVICE_CONFIRM_49', '2026-02-27 17:35:12', '2026-02-27 17:36:53', '2026-02-27 17:37:02', 4, 4.50, 5.50, 30.00, NULL, NULL);
INSERT INTO `order` VALUES (50, 'ORD17721851897289aebd7', 15, 21, '李怀', NULL, '范涵伶', 22, '男', '范涵伶', '15520765697', '成都市', '普通陪诊', 1, '2026-02-27', '08:00-12:00', NULL, '无', 110.00, 1, '2026-02-27 17:39:52', 4, NULL, NULL, NULL, NULL, NULL, NULL, 'https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=SERVICE_CONFIRM_50', '2026-02-27 17:39:50', '2026-02-27 17:55:17', '2026-02-27 17:55:23', 4, 4.00, 4.50, 30.00, NULL, NULL);
INSERT INTO `order` VALUES (51, 'ORD1772185891805b1f712', 15, 21, '李怀', NULL, '范涵伶', 22, '男', '范涵伶', '15520765697', '成都市', '普通陪诊', 1, '2026-02-27', '08:00-12:00', '胸痛', '无', 140.00, 1, '2026-02-27 17:51:40', 6, NULL, NULL, NULL, NULL, NULL, NULL, 'https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=SERVICE_CONFIRM_51', '2026-02-27 17:51:32', '2026-02-27 18:36:45', '2026-02-27 18:41:34', 4, 4.00, 4.50, 30.00, NULL, NULL);
INSERT INTO `order` VALUES (52, 'ORD17721864203592d5f4c', 15, 21, '李怀', NULL, '范涵伶', 22, '男', '范涵伶', '15520765697', '成都市', '普通陪诊', 1, '2026-02-27', '08:00-11:00', '腹痛', '无', 140.00, 1, '2026-02-27 18:00:22', 6, NULL, NULL, NULL, NULL, NULL, NULL, 'https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=SERVICE_CONFIRM_52', '2026-02-27 18:00:20', '2026-02-27 18:36:01', '2026-02-28 01:08:57', 4, 3.00, 4.50, 60.00, NULL, NULL);
INSERT INTO `order` VALUES (53, 'ORD17721864910689bda5e', 15, NULL, NULL, NULL, '范涵伶', 22, '男', '范涵伶', '15520765697', '成都市', '普通陪诊', 1, '2026-02-27', '08:00-10:00', '胸痛', '无', 50.00, 0, NULL, 7, '超时未支付自动取消', '2026-02-27 18:41:43', 0, 0.00, 0.00, 50.00, NULL, '2026-02-27 18:01:31', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `order` VALUES (54, 'ORD1772186820885b73960', 15, NULL, NULL, NULL, '范涵伶', 22, '男', '范涵伶', '15520765697', '成都市', '普通陪诊', 1, '2026-02-27', '08:00-10:30', '胸痛', '无', 80.00, 0, NULL, 7, '超时未支付自动取消', '2026-02-27 18:41:40', 0, 0.00, 0.00, 80.00, NULL, '2026-02-27 18:07:01', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `order` VALUES (55, 'ORD17721883993729bc4e8', 15, 21, '李怀', NULL, '范涵伶', 22, '男', '范涵伶', '15520765697', '成都市', '急诊陪同', 3, '2026-02-27', '08:00-11:00', '胸痛', '无', 210.00, 1, '2026-02-27 18:33:27', 6, NULL, NULL, NULL, NULL, NULL, NULL, 'https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=SERVICE_CONFIRM_55', '2026-02-27 18:33:19', '2026-02-27 18:34:14', '2026-02-27 18:34:18', NULL, 3.00, 4.00, 30.00, NULL, NULL);
INSERT INTO `order` VALUES (56, 'ORD1772191228942f306f5', 15, 21, '李怀', NULL, '范涵伶', 22, '男', '范涵伶', '15520765697', '成都市', '急诊陪同', 3, '2026-02-27', '08:00-11:00', '胸痛', '无', 150.00, 1, '2026-02-27 19:20:33', 6, NULL, NULL, NULL, NULL, NULL, 30.00, 'https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=SERVICE_CONFIRM_56', '2026-02-27 19:20:29', '2026-02-27 19:21:38', '2026-02-27 19:26:06', 4, 3.00, 2.00, -30.00, NULL, NULL);
INSERT INTO `order` VALUES (57, 'ORD1772262390131755423', 15, NULL, NULL, NULL, '范涵伶', 22, '男', '范涵伶', '15520765697', '成都市', '上门陪诊', 4, '2026-02-28', '08:00-11:00', '发热', '无', 110.00, 1, '2026-02-28 15:06:41', 7, '计划有变，暂不就诊', '2026-02-28 15:52:07', 0, 0.00, 0.00, 110.00, NULL, '2026-02-28 15:06:30', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);

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
INSERT INTO `user` VALUES (15, 'fanfan', '123456', '范涵伶', '15520765697', '男', 22, '/uploads/user1.jpg', 0, NULL, '2026-02-12 01:55:08');
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

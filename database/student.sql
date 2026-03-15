/*
 Navicat Premium Dump SQL

 Source Server         : localhost_3306
 Source Server Type    : MySQL
 Source Server Version : 90600 (9.6.0)
 Source Host           : localhost:3306
 Source Schema         : student

 Target Server Type    : MySQL
 Target Server Version : 90600 (9.6.0)
 File Encoding         : 65001

 Date: 13/03/2026 11:56:13
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for ai_medical_qa
-- ----------------------------
DROP TABLE IF EXISTS `ai_medical_qa`;
CREATE TABLE `ai_medical_qa` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `conversation_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '对话会话ID，用于关联同一场对话的所有消息',
  `question` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户提问内容',
  `answer` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT 'AI回答内容',
  `qa_status` tinyint NOT NULL DEFAULT '0' COMMENT '问答状态：0-处理中，1-完成，2-失败',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
  `thinking_process` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT 'AI思考过程',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_conversation_id` (`conversation_id`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='AI医疗问答记录表';

-- ----------------------------
-- Records of ai_medical_qa
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for attendant
-- ----------------------------
DROP TABLE IF EXISTS `attendant`;
CREATE TABLE `attendant` (
  `user_id` int NOT NULL COMMENT '关联user表ID',
  `certificate` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '资格证书编号',
  `status` int DEFAULT '1' COMMENT '状态：0=审核中, 1=正常, 2=封禁',
  `introduction` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '个人简介',
  `professional_field` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '擅长领域',
  `score` decimal(2,1) DEFAULT '5.0' COMMENT '评分',
  `experience_years` int DEFAULT '0' COMMENT '从业年限',
  `hospital_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '常驻医院',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`) USING BTREE,
  CONSTRAINT `attendant_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='陪诊师扩展信息表';

-- ----------------------------
-- Records of attendant
-- ----------------------------
BEGIN;
INSERT INTO `attendant` (`user_id`, `certificate`, `status`, `introduction`, `professional_field`, `score`, `experience_years`, `hospital_name`, `create_time`, `update_time`) VALUES (1, 'CP20230802', 1, '多年陪诊经验，擅长老年陪护', '老年患者陪诊', 4.7, 6, NULL, '2025-07-01 10:22:35', '2026-02-09 14:51:23');
INSERT INTO `attendant` (`user_id`, `certificate`, `status`, `introduction`, `professional_field`, `score`, `experience_years`, `hospital_name`, `create_time`, `update_time`) VALUES (21, NULL, 1, '资深陪诊师，熟悉各大医院流程，服务态度好。', '全科,术后护理,挂号引导', 5.0, 5, NULL, '2026-02-12 00:09:40', '2026-02-12 00:09:40');
INSERT INTO `attendant` (`user_id`, `certificate`, `status`, `introduction`, `professional_field`, `score`, `experience_years`, `hospital_name`, `create_time`, `update_time`) VALUES (22, 'CP20230802', 0, '新手陪诊师，正在学习', '新手陪诊', 4.0, 0, NULL, '2025-07-01 10:06:05', '2025-09-09 15:58:15');
INSERT INTO `attendant` (`user_id`, `certificate`, `status`, `introduction`, `professional_field`, `score`, `experience_years`, `hospital_name`, `create_time`, `update_time`) VALUES (23, 'CP20230802', 1, '多年陪诊经验，擅长老年陪护', '老年陪护', 5.0, 5, NULL, '2025-07-01 10:22:35', '2025-09-09 15:58:38');
INSERT INTO `attendant` (`user_id`, `certificate`, `status`, `introduction`, `professional_field`, `score`, `experience_years`, `hospital_name`, `create_time`, `update_time`) VALUES (24, 'MED2023001', 1, '资深陪诊师', '普通陪诊', 5.0, 3, NULL, '2025-07-02 11:06:37', '2025-09-09 15:59:26');
INSERT INTO `attendant` (`user_id`, `certificate`, `status`, `introduction`, `professional_field`, `score`, `experience_years`, `hospital_name`, `create_time`, `update_time`) VALUES (25, NULL, 1, '膝关节手术,术后护理,综合陪诊服务 (参考价格: 198.00)', '术后护理,综合陪诊服务', 4.8, 5, '北京协和医院', '2025-11-29 22:09:24', '2025-12-01 21:27:49');
INSERT INTO `attendant` (`user_id`, `certificate`, `status`, `introduction`, `professional_field`, `score`, `experience_years`, `hospital_name`, `create_time`, `update_time`) VALUES (26, NULL, 0, '膝关节手术,术后护理,综合陪诊 (参考价格: 198.00)', '术后护理,综合陪诊', 4.8, 5, NULL, '2025-12-15 21:24:59', '2025-12-15 21:24:59');
COMMIT;

-- ----------------------------
-- Table structure for chat_message
-- ----------------------------
DROP TABLE IF EXISTS `chat_message`;
CREATE TABLE `chat_message` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `sender_id` int NOT NULL COMMENT '发送者ID',
  `receiver_id` int NOT NULL COMMENT '接收者ID',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '消息内容',
  `msg_type` int DEFAULT '1' COMMENT '消息类型：1=文本, 2=图片',
  `is_read` tinyint(1) DEFAULT '0' COMMENT '是否已读：0=未读, 1=已读',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_sender_receiver` (`sender_id`,`receiver_id`) USING BTREE,
  KEY `idx_receiver_read` (`receiver_id`,`is_read`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=420 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='聊天消息表';

-- ----------------------------
-- Records of chat_message
-- ----------------------------
BEGIN;
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (266, 0, 15, '恭喜您!订单No.ORD17720940252105599f3支付完成，我们已通知陪诊师为您服务。陪诊师将在30分钟内与您联系，请保持电话畅通。', 1, 1, '2026-02-26 16:20:27');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (267, 0, 15, '您预约的(02月26日)12:30-18:00有成都市人民医院的就诊安排，陪诊师李怀已接单。请携带身份证、医保卡及相关检查报告。', 1, 1, '2026-02-26 16:20:36');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (268, 21, 15, '您好！我是陪诊师李怀，很高兴为您服务。我会尽快与您联系确认服务细节。', 1, 1, '2026-02-26 16:20:36');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (269, 0, 15, '您的订单No.ORD17720940252105599f3服务已开始。陪诊师已到达指定位置，请准备就诊。', 1, 1, '2026-02-26 16:21:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (270, 0, 21, '您已开始为订单 ORD17720940252105599f3 提供服务，请按时完成服务。', 1, 1, '2026-02-26 16:21:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (271, 0, 15, '您的陪诊服务(订单No.ORD17720940252105599f3)已结束，请确认本次服务时长和费用（多退少补）。', 1, 1, '2026-02-26 16:21:35');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (272, 0, 21, '您已结束订单 ORD17720940252105599f3 的服务，请提醒用户确认时长与费用。', 1, 1, '2026-02-26 16:21:35');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (273, 0, 15, '您已确认本次陪诊服务时长与费用，订单已完成。', 1, 1, '2026-02-26 16:21:41');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (274, 0, 21, '用户已确认订单 ORD17720940252105599f3 的时长与费用，订单已完成。', 1, 1, '2026-02-26 16:21:41');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (275, 0, 15, '恭喜您!订单No.ORD17720947028516784c9支付完成，我们已通知陪诊师为您服务。陪诊师将在30分钟内与您联系，请保持电话畅通。', 1, 1, '2026-02-26 16:31:44');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (276, 0, 15, '您预约的(02月27日)08:00-14:00有成都市中医院的就诊安排，陪诊师李怀已接单。请携带身份证、医保卡及相关检查报告。', 1, 1, '2026-02-26 16:32:08');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (277, 21, 15, '您好！我是陪诊师李怀，很高兴为您服务。我会尽快与您联系确认服务细节。', 1, 1, '2026-02-26 16:32:08');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (278, 0, 15, '您的订单No.ORD17720947028516784c9服务已开始。陪诊师已到达指定位置，请准备就诊。', 1, 1, '2026-02-26 16:32:35');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (279, 0, 21, '您已开始为订单 ORD17720947028516784c9 提供服务，请按时完成服务。', 1, 1, '2026-02-26 16:32:35');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (280, 0, 15, '您的陪诊服务(订单No.ORD17720947028516784c9)已结束，请确认本次服务时长和费用（多退少补）。', 1, 1, '2026-02-26 16:32:59');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (281, 0, 21, '您已结束订单 ORD17720947028516784c9 的服务，请提醒用户确认时长与费用。', 1, 1, '2026-02-26 16:32:59');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (282, 0, 15, '您已确认本次陪诊服务时长与费用，订单已完成。', 1, 1, '2026-02-26 16:33:07');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (283, 0, 21, '用户已确认订单 ORD17720947028516784c9 的时长与费用，订单已完成。', 1, 1, '2026-02-26 16:33:07');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (284, 0, 15, '恭喜您!订单No.ORD1772167293352229bec支付完成，我们已通知陪诊师为您服务。陪诊师将在30分钟内与您联系，请保持电话畅通。', 1, 1, '2026-02-27 12:43:16');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (285, 0, 15, '您已成功创建订单 ORD1772168983757104052，请在15分钟内完成支付，逾期系统将自动取消订单。', 1, 1, '2026-02-27 13:09:43');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (286, 0, 15, '您已成功创建订单 ORD17721696183899bc4ab，请在15分钟内完成支付，逾期系统将自动取消订单。', 1, 1, '2026-02-27 13:20:18');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (287, 0, 15, '您已成功创建订单 ORD177216971527054773c，请在15分钟内完成支付，逾期系统将自动取消订单。', 1, 1, '2026-02-27 13:21:55');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (288, 0, 15, '您已成功创建订单 ORD1772170307910028ccb，请在15分钟内完成支付，逾期系统将自动取消订单。', 1, 1, '2026-02-27 13:31:47');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (289, 0, 15, '您的订单ORD17721684470500a92ae已取消。取消原因：超时未支付自动取消', 1, 1, '2026-02-27 14:55:56');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (290, 0, 15, '您已成功创建订单 ORD177217538128245f6c9，请在15分钟内完成支付，逾期系统将自动取消订单。', 1, 1, '2026-02-27 14:56:21');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (291, 0, 15, '恭喜您!订单No.ORD177217538128245f6c9支付完成，我们已通知陪诊师为您服务。陪诊师将在30分钟内与您联系，请保持电话畅通。', 1, 1, '2026-02-27 14:56:36');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (292, 0, 15, '您已成功创建订单 ORD17721767672500e0d4f，请在15分钟内完成支付，逾期系统将自动取消订单。', 1, 1, '2026-02-27 15:19:27');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (293, 0, 15, '恭喜您!订单No.ORD17721767672500e0d4f支付完成，我们已通知陪诊师为您服务。陪诊师将在30分钟内与您联系，请保持电话畅通。', 1, 1, '2026-02-27 15:19:29');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (294, 0, 15, '您已成功创建订单 ORD17721772386768f50f7，请在15分钟内完成支付，逾期系统将自动取消订单。', 1, 1, '2026-02-27 15:27:18');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (295, 0, 15, '恭喜您!订单No.ORD17721772386768f50f7支付完成，我们已通知陪诊师为您服务。陪诊师将在30分钟内与您联系，请保持电话畅通。', 1, 1, '2026-02-27 15:27:20');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (296, 0, 15, '您已成功创建订单 ORD1772177339845b88d27，请在15分钟内完成支付，逾期系统将自动取消订单。', 1, 1, '2026-02-27 15:28:59');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (297, 0, 15, '恭喜您!订单No.ORD1772177339845b88d27支付完成，我们已通知陪诊师为您服务。陪诊师将在30分钟内与您联系，请保持电话畅通。', 1, 1, '2026-02-27 15:29:02');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (298, 0, 15, '您已成功创建订单 ORD1772184911870ee26b8，请在15分钟内完成支付，逾期系统将自动取消订单。', 1, 1, '2026-02-27 17:35:11');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (299, 0, 15, '恭喜您!订单No.ORD1772184911870ee26b8支付完成，我们已通知陪诊师为您服务。陪诊师将在30分钟内与您联系，请保持电话畅通。', 1, 1, '2026-02-27 17:35:13');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (300, 0, 15, '您预约的(02月27日)08:00-12:30有cda的就诊安排，陪诊师李怀已接单。请携带身份证、医保卡及相关检查报告。', 1, 1, '2026-02-27 17:36:37');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (301, 21, 15, '您好！我是陪诊师李怀，很高兴为您服务。我会尽快与您联系确认服务细节。', 1, 1, '2026-02-27 17:36:37');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (302, 0, 15, '您的订单No.ORD1772184911870ee26b8服务已开始。陪诊师已到达指定位置，请准备就诊。', 1, 1, '2026-02-27 17:36:52');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (303, 0, 21, '您已开始为订单 ORD1772184911870ee26b8 提供服务，请按时完成服务。', 1, 1, '2026-02-27 17:36:52');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (304, 0, 15, '您的陪诊服务(订单No.ORD1772184911870ee26b8)已结束，请确认本次服务时长和费用（多退少补）。', 1, 1, '2026-02-27 17:37:02');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (305, 0, 21, '您已结束订单 ORD1772184911870ee26b8 的服务，请提醒用户确认时长与费用。', 1, 1, '2026-02-27 17:37:02');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (306, 0, 15, '您已确认本次陪诊服务时长与费用，订单已完成。', 1, 1, '2026-02-27 17:37:39');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (307, 0, 21, '用户已确认订单 ORD1772184911870ee26b8 的时长与费用，订单已完成。', 1, 1, '2026-02-27 17:37:39');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (308, 0, 15, '您已成功创建订单 ORD17721851897289aebd7，请在15分钟内完成支付，逾期系统将自动取消订单。', 1, 1, '2026-02-27 17:39:49');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (309, 0, 15, '恭喜您!订单No.ORD17721851897289aebd7支付完成，我们已通知陪诊师为您服务。陪诊师将在30分钟内与您联系，请保持电话畅通。', 1, 1, '2026-02-27 17:39:52');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (310, 0, 15, '您已成功创建订单 ORD1772185891805b1f712，请在15分钟内完成支付，逾期系统将自动取消订单。', 1, 1, '2026-02-27 17:51:31');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (311, 0, 15, '恭喜您!订单No.ORD1772185891805b1f712支付完成，我们已通知陪诊师为您服务。陪诊师将在30分钟内与您联系，请保持电话畅通。', 1, 1, '2026-02-27 17:51:32');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (312, 0, 15, '您预约的(02月27日)08:00-12:00有成都市的就诊安排，陪诊师李怀已接单。请携带身份证、医保卡及相关检查报告。', 1, 1, '2026-02-27 17:54:55');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (313, 21, 15, '您好！我是陪诊师李怀，很高兴为您服务。我会尽快与您联系确认服务细节。', 1, 1, '2026-02-27 17:54:55');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (314, 0, 15, '您的订单No.ORD17721851897289aebd7服务已开始。陪诊师已到达指定位置，请准备就诊。', 1, 1, '2026-02-27 17:55:17');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (315, 0, 21, '您已开始为订单 ORD17721851897289aebd7 提供服务，请按时完成服务。', 1, 1, '2026-02-27 17:55:17');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (316, 0, 15, '您的陪诊服务(订单No.ORD17721851897289aebd7)已结束，请确认本次服务时长和费用（多退少补）。', 1, 1, '2026-02-27 17:55:22');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (317, 0, 21, '您已结束订单 ORD17721851897289aebd7 的服务，请提醒用户确认时长与费用。', 1, 1, '2026-02-27 17:55:22');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (318, 0, 15, '您已成功创建订单 ORD17721864203592d5f4c，请在15分钟内完成支付，逾期系统将自动取消订单。', 1, 1, '2026-02-27 18:00:20');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (319, 0, 15, '恭喜您!订单No.ORD17721864203592d5f4c支付完成，我们已通知陪诊师为您服务。陪诊师将在30分钟内与您联系，请保持电话畅通。', 1, 1, '2026-02-27 18:00:21');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (320, 0, 15, '您已成功创建订单 ORD17721864910689bda5e，请在15分钟内完成支付，逾期系统将自动取消订单。', 1, 1, '2026-02-27 18:01:31');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (321, 0, 15, '您已成功创建订单 ORD1772186820885b73960，请在15分钟内完成支付，逾期系统将自动取消订单。', 1, 1, '2026-02-27 18:07:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (322, 0, 15, '您已成功创建订单 ORD17721883993729bc4e8，请在15分钟内完成预付款，逾期系统将自动取消订单。', 1, 1, '2026-02-27 18:33:19');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (323, 0, 15, '恭喜您!订单No.ORD17721883993729bc4e8支付完成，我们已通知陪诊师为您服务。陪诊师将在30分钟内与您联系，请保持电话畅通。', 1, 1, '2026-02-27 18:33:26');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (324, 0, 15, '您预约的(02月27日)08:00-11:00有成都市的就诊安排，陪诊师李怀已接单。请携带身份证、医保卡及相关检查报告。', 1, 1, '2026-02-27 18:34:08');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (325, 21, 15, '您好！我是陪诊师李怀，很高兴为您服务。我会尽快与您联系确认服务细节。', 1, 1, '2026-02-27 18:34:08');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (326, 0, 15, '您的订单No.ORD17721883993729bc4e8服务已开始。陪诊师已到达指定位置，请准备就诊。', 1, 1, '2026-02-27 18:34:14');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (327, 0, 21, '您已开始为订单 ORD17721883993729bc4e8 提供服务，请按时完成服务。', 1, 1, '2026-02-27 18:34:14');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (328, 0, 15, '您的陪诊服务(订单No.ORD17721883993729bc4e8)已结束，请确认本次服务时长和费用（多退少补）。', 1, 1, '2026-02-27 18:34:17');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (329, 0, 21, '您已结束订单 ORD17721883993729bc4e8 的服务，请提醒用户确认时长与费用。', 1, 1, '2026-02-27 18:34:17');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (330, 0, 15, '您已确认本次陪诊服务时长与费用，订单已完成。', 1, 1, '2026-02-27 18:34:33');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (331, 0, 21, '用户已确认订单 ORD17721883993729bc4e8 的时长与费用，订单已完成。', 1, 1, '2026-02-27 18:34:33');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (332, 0, 15, '您预约的(02月27日)08:00-11:00有成都市的就诊安排，陪诊师李怀已接单。请携带身份证、医保卡及相关检查报告。', 1, 1, '2026-02-27 18:35:52');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (333, 21, 15, '您好！我是陪诊师李怀，很高兴为您服务。我会尽快与您联系确认服务细节。', 1, 1, '2026-02-27 18:35:52');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (334, 0, 15, '您的订单No.ORD17721864203592d5f4c服务已开始。陪诊师已到达指定位置，请准备就诊。', 1, 1, '2026-02-27 18:36:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (335, 0, 21, '您已开始为订单 ORD17721864203592d5f4c 提供服务，请按时完成服务。', 1, 1, '2026-02-27 18:36:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (336, 0, 15, '您预约的(02月27日)08:00-12:00有成都市的就诊安排，陪诊师李怀已接单。请携带身份证、医保卡及相关检查报告。', 1, 1, '2026-02-27 18:36:35');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (337, 21, 15, '您好！我是陪诊师李怀，很高兴为您服务。我会尽快与您联系确认服务细节。', 1, 1, '2026-02-27 18:36:35');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (338, 0, 15, '您的订单No.ORD1772185891805b1f712服务已开始。陪诊师已到达指定位置，请准备就诊。', 1, 1, '2026-02-27 18:36:44');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (339, 0, 21, '您已开始为订单 ORD1772185891805b1f712 提供服务，请按时完成服务。', 1, 1, '2026-02-27 18:36:44');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (340, 0, 15, '您的陪诊服务(订单No.ORD1772185891805b1f712)已结束，请确认本次服务时长和费用（多退少补）。', 1, 1, '2026-02-27 18:41:33');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (341, 0, 21, '您已结束订单 ORD1772185891805b1f712 的服务，请提醒用户确认时长与费用。', 1, 1, '2026-02-27 18:41:33');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (342, 0, 15, '您的订单ORD1772186820885b73960已取消。取消原因：超时未支付自动取消', 1, 1, '2026-02-27 18:41:39');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (343, 0, 15, '您的订单ORD17721864910689bda5e已取消。取消原因：超时未支付自动取消', 1, 1, '2026-02-27 18:41:42');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (344, 0, 15, '您已成功创建订单 ORD1772191228942f306f5，请在15分钟内完成预付款，逾期系统将自动取消订单。', 1, 1, '2026-02-27 19:20:28');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (345, 0, 15, '恭喜您!订单No.ORD1772191228942f306f5支付完成，我们已通知陪诊师为您服务。陪诊师将在30分钟内与您联系，请保持电话畅通。', 1, 1, '2026-02-27 19:20:32');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (346, 0, 15, '您预约的(02月27日)08:00-11:00有成都市的就诊安排，陪诊师李怀已接单。请携带身份证、医保卡及相关检查报告。', 1, 1, '2026-02-27 19:21:31');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (347, 21, 15, '您好！我是陪诊师李怀，很高兴为您服务。我会尽快与您联系确认服务细节。', 1, 1, '2026-02-27 19:21:31');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (348, 0, 15, '您的订单No.ORD1772191228942f306f5服务已开始。陪诊师已到达指定位置，请准备就诊。', 1, 1, '2026-02-27 19:21:38');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (349, 0, 21, '您已开始为订单 ORD1772191228942f306f5 提供服务，请按时完成服务。', 1, 1, '2026-02-27 19:21:38');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (350, 0, 15, '您的陪诊服务(订单No.ORD1772191228942f306f5)已结束，请确认本次服务时长和费用（多退少补）。', 1, 1, '2026-02-27 19:26:06');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (351, 0, 21, '您已结束订单 ORD1772191228942f306f5 的服务，请提醒用户确认时长与费用。', 1, 1, '2026-02-27 19:26:06');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (352, 0, 15, '您已确认本次陪诊服务时长与费用，订单已完成。', 1, 1, '2026-02-27 19:29:33');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (353, 0, 21, '用户已确认订单 ORD1772191228942f306f5 的时长与费用，订单已完成。', 1, 1, '2026-02-27 19:29:33');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (354, 0, 15, '您已确认本次陪诊服务时长与费用，订单已完成。', 1, 1, '2026-02-28 01:06:12');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (355, 0, 21, '用户已确认订单 ORD1772185891805b1f712 的时长与费用，订单已完成。', 1, 1, '2026-02-28 01:06:12');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (356, 0, 15, '您预约的(02月27日)08:00-10:30有成都市的就诊安排，陪诊师李怀已接单。请携带身份证、医保卡及相关检查报告。', 1, 1, '2026-02-28 01:08:44');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (357, 21, 15, '您好！我是陪诊师李怀，很高兴为您服务。我会尽快与您联系确认服务细节。', 1, 1, '2026-02-28 01:08:44');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (358, 0, 15, '您的陪诊服务(订单No.ORD17721864203592d5f4c)已结束，请确认本次服务时长和费用（多退少补）。', 1, 1, '2026-02-28 01:08:57');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (359, 0, 21, '您已结束订单 ORD17721864203592d5f4c 的服务，请提醒用户确认时长与费用。', 1, 1, '2026-02-28 01:08:57');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (360, 0, 15, '您已确认本次陪诊服务时长与费用，订单已完成。', 1, 1, '2026-02-28 01:19:10');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (361, 0, 21, '用户已确认订单 ORD17721864203592d5f4c 的时长与费用，订单已完成。', 1, 1, '2026-02-28 01:19:10');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (362, 0, 15, '您已成功创建订单 ORD1772262390131755423，请在15分钟内完成预付款，逾期系统将自动取消订单。', 1, 1, '2026-02-28 15:06:30');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (363, 0, 15, '恭喜您!订单No.ORD1772262390131755423支付完成，我们已通知陪诊师为您服务。陪诊师将在30分钟内与您联系，请保持电话畅通。', 1, 1, '2026-02-28 15:06:41');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (364, 0, 15, '您的订单ORD1772262390131755423已取消。取消原因：计划有变，暂不就诊', 1, 1, '2026-02-28 15:52:07');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (365, 0, 15, '您的订单No.ORD1772177339845b88d27服务已开始。陪诊师已到达指定位置，请准备就诊。', 1, 1, '2026-02-28 15:52:58');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (366, 0, 21, '您已开始为订单 ORD1772177339845b88d27 提供服务，请按时完成服务。', 1, 1, '2026-02-28 15:52:58');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (367, 0, 15, '您预约的(02月27日)08:00-09:30有成都的就诊安排，陪诊师李怀已接单。请携带身份证、医保卡及相关检查报告。', 1, 1, '2026-02-28 21:51:29');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (368, 21, 15, '您好！我是陪诊师李怀，很高兴为您服务。我会尽快与您联系确认服务细节。', 1, 1, '2026-02-28 21:51:29');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (369, 0, 15, '您已成功创建订单 ORD1772287600519548ca7，请在15分钟内完成预付款，逾期系统将自动取消订单。', 1, 1, '2026-02-28 22:06:40');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (370, 0, 15, '恭喜您!订单No.ORD1772287600519548ca7支付完成，我们已通知陪诊师为您服务。陪诊师将在30分钟内与您联系，请保持电话畅通。', 1, 1, '2026-02-28 22:06:41');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (371, 0, 15, '您预约的(02月27日)08:00-10:30有成都市的就诊安排，陪诊师李怀已接单。请携带身份证、医保卡及相关检查报告。', 1, 1, '2026-02-28 22:08:25');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (372, 21, 15, '您好！我是陪诊师李怀，很高兴为您服务。我会尽快与您联系确认服务细节。', 1, 1, '2026-02-28 22:08:25');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (373, 0, 15, '您的订单No.ORD17721772386768f50f7服务已开始。陪诊师已到达指定位置，请准备就诊。', 1, 1, '2026-02-28 22:08:45');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (374, 0, 21, '您已开始为订单 ORD17721772386768f50f7 提供服务，请按时完成服务。', 1, 1, '2026-02-28 22:08:45');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (375, 15, 21, '11', 1, 1, '2026-02-28 22:27:55');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (376, 21, 15, '22', 1, 1, '2026-02-28 22:30:32');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (377, 15, 21, '11', 1, 1, '2026-02-28 22:30:35');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (378, 15, 21, '22', 1, 1, '2026-03-03 09:56:01');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (379, 21, 15, 'nih ', 1, 1, '2026-03-03 09:56:11');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (380, 15, 21, '22', 1, 1, '2026-03-03 09:56:16');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (381, 21, 15, '22', 1, 1, '2026-03-03 09:56:21');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (382, 21, 15, '【紧急求助】请立即联系我！', 1, 1, '2026-03-03 09:56:38');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (383, 15, 21, '😂😂', 1, 1, '2026-03-03 09:57:06');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (384, 21, 15, '😃😃', 1, 1, '2026-03-03 09:57:12');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (385, 21, 15, '11', 1, 1, '2026-03-10 20:08:00');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (386, 15, 21, '22', 1, 1, '2026-03-10 20:08:12');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (387, 0, 15, '您已成功创建订单 ORD177315249372547cc64，请在15分钟内完成预付款，逾期系统将自动取消订单。', 1, 1, '2026-03-10 22:21:33');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (388, 0, 15, '恭喜您!订单No.ORD177315249372547cc64支付完成，我们已通知陪诊师为您服务。陪诊师将在30分钟内与您联系，请保持电话畅通。', 1, 1, '2026-03-10 22:21:44');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (389, 0, 15, '您预约的(03月11日)08:00-11:00有成都市中医院的就诊安排，陪诊师李怀已接单。请携带身份证、医保卡及相关检查报告。', 1, 1, '2026-03-10 22:22:01');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (390, 21, 15, '您好！我是陪诊师李怀，很高兴为您服务。我会尽快与您联系确认服务细节。', 1, 1, '2026-03-10 22:22:01');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (391, 0, 15, '您的订单No.ORD177315249372547cc64服务已开始。陪诊师已到达指定位置，请准备就诊。', 1, 1, '2026-03-10 22:22:18');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (392, 0, 21, '您已开始为订单 ORD177315249372547cc64 提供服务，请按时完成服务。', 1, 1, '2026-03-10 22:22:18');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (393, 21, 15, '11', 1, 1, '2026-03-10 22:22:37');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (394, 0, 15, '您的陪诊服务(订单No.ORD177315249372547cc64)已结束，请确认本次服务时长和费用（多退少补）。', 1, 1, '2026-03-10 22:22:52');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (395, 0, 21, '您已结束订单 ORD177315249372547cc64 的服务，请提醒用户确认时长与费用。', 1, 1, '2026-03-10 22:22:52');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (396, 0, 15, '您已确认本次陪诊服务时长与费用，订单已完成。', 1, 1, '2026-03-10 22:23:06');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (397, 0, 21, '用户已确认订单 ORD177315249372547cc64 的时长与费用，订单已完成。', 1, 1, '2026-03-10 22:23:06');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (398, 0, 15, '您已成功创建订单 ORD1773298044420e4b1ab，请在15分钟内完成预付款，逾期系统将自动取消订单。', 1, 1, '2026-03-12 14:47:24');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (399, 0, 15, '恭喜您!订单No.ORD1773298044420e4b1ab支付完成，我们已通知陪诊师为您服务。陪诊师将在30分钟内与您联系，请保持电话畅通。', 1, 1, '2026-03-12 14:47:37');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (400, 0, 15, '您预约的(03月13日)08:00-10:00有都江堰市医院的就诊安排，陪诊师李怀已接单。请携带身份证、医保卡及相关检查报告。', 1, 1, '2026-03-12 14:47:57');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (401, 21, 15, '您好！我是陪诊师李怀，很高兴为您服务。我会尽快与您联系确认服务细节。', 1, 1, '2026-03-12 14:47:57');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (402, 0, 15, '您的订单No.ORD1773298044420e4b1ab服务已开始。陪诊师已到达指定位置，请准备就诊。', 1, 1, '2026-03-12 14:48:25');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (403, 0, 21, '您已开始为订单 ORD1773298044420e4b1ab 提供服务，请按时完成服务。', 1, 1, '2026-03-12 14:48:25');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (404, 0, 15, '您的陪诊服务(订单No.ORD1773298044420e4b1ab)已结束，请确认本次服务时长和费用（多退少补）。', 1, 1, '2026-03-12 14:48:47');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (405, 0, 21, '您已结束订单 ORD1773298044420e4b1ab 的服务，请提醒用户确认时长与费用。', 1, 1, '2026-03-12 14:48:47');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (406, 0, 15, '您已确认本次陪诊服务时长与费用，订单已完成。', 1, 1, '2026-03-12 14:48:52');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (407, 0, 21, '用户已确认订单 ORD1773298044420e4b1ab 的时长与费用，订单已完成。', 1, 1, '2026-03-12 14:48:52');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (408, 0, 15, '您已成功创建订单 ORD1773310486563a96d40，请在15分钟内完成预付款，逾期系统将自动取消订单。', 1, 1, '2026-03-12 18:14:46');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (409, 0, 15, '恭喜您!订单No.ORD1773310486563a96d40支付完成，我们已通知陪诊师为您服务。陪诊师将在30分钟内与您联系，请保持电话畅通。', 1, 1, '2026-03-12 18:15:06');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (410, 0, 15, '您预约的(03月12日)08:00-11:30有都江堰医院的就诊安排，陪诊师李怀已接单。请携带身份证、医保卡及相关检查报告。', 1, 1, '2026-03-12 18:15:34');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (411, 21, 15, '您好！我是陪诊师李怀，很高兴为您服务。我会尽快与您联系确认服务细节。', 1, 1, '2026-03-12 18:15:34');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (412, 0, 15, '您的订单No.ORD1773310486563a96d40服务已开始。陪诊师已到达指定位置，请准备就诊。', 1, 1, '2026-03-12 18:15:56');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (413, 0, 21, '您已开始为订单 ORD1773310486563a96d40 提供服务，请按时完成服务。', 1, 1, '2026-03-12 18:15:56');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (414, 0, 15, '您的陪诊服务(订单No.ORD1773310486563a96d40)已结束，请确认本次服务时长和费用（多退少补）。', 1, 1, '2026-03-12 18:16:23');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (415, 0, 21, '您已结束订单 ORD1773310486563a96d40 的服务，请提醒用户确认时长与费用。', 1, 1, '2026-03-12 18:16:23');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (416, 0, 15, '您已确认本次陪诊服务时长与费用，订单已完成。', 1, 1, '2026-03-12 18:16:35');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (417, 0, 21, '用户已确认订单 ORD1773310486563a96d40 的时长与费用，订单已完成。', 1, 1, '2026-03-12 18:16:35');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (418, 21, 15, '22', 1, 1, '2026-03-12 18:17:22');
INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES (419, 15, 21, '11', 1, 1, '2026-03-12 18:17:24');
COMMIT;

-- ----------------------------
-- Table structure for guide_appointment
-- ----------------------------
DROP TABLE IF EXISTS `guide_appointment`;
CREATE TABLE `guide_appointment` (
  `id` int NOT NULL AUTO_INCREMENT,
  `appointment_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '预约编号',
  `user_id` int DEFAULT NULL COMMENT '提交人ID',
  `patient_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '就诊人',
  `patient_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `symptoms` json DEFAULT NULL COMMENT '症状描述',
  `hospital_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `service_type_number` int DEFAULT '1' COMMENT '服务类型编号',
  `service_date` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `service_start_time` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `service_end_time` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `other_requirement` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '其他需求',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_appt_no` (`appointment_no`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=64 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='AI导诊需求单';

-- ----------------------------
-- Records of guide_appointment
-- ----------------------------
BEGIN;
INSERT INTO `guide_appointment` (`id`, `appointment_no`, `user_id`, `patient_name`, `patient_phone`, `symptoms`, `hospital_name`, `service_type_number`, `service_date`, `service_start_time`, `service_end_time`, `other_requirement`, `create_time`) VALUES (47, 'APP177217538122848db2c', NULL, '范涵伶', '15520765697', '[\"发热\"]', '成都市', 3, '2026-02-27', '08:00', '10:30', '', '2026-02-27 14:56:21');
INSERT INTO `guide_appointment` (`id`, `appointment_no`, `user_id`, `patient_name`, `patient_phone`, `symptoms`, `hospital_name`, `service_type_number`, `service_date`, `service_start_time`, `service_end_time`, `other_requirement`, `create_time`) VALUES (48, 'APP1772176767195ecf6f7', NULL, '范涵伶', '15520765697', '[\"胸痛\"]', '成都市', 1, '2026-02-27', '08:00', '10:00', '', '2026-02-27 15:19:27');
INSERT INTO `guide_appointment` (`id`, `appointment_no`, `user_id`, `patient_name`, `patient_phone`, `symptoms`, `hospital_name`, `service_type_number`, `service_date`, `service_start_time`, `service_end_time`, `other_requirement`, `create_time`) VALUES (49, 'APP17721772386522ac459', NULL, '范涵伶', '15520765697', '[]', '成都', 1, '2026-02-27', '08:00', '09:30', '', '2026-02-27 15:27:19');
INSERT INTO `guide_appointment` (`id`, `appointment_no`, `user_id`, `patient_name`, `patient_phone`, `symptoms`, `hospital_name`, `service_type_number`, `service_date`, `service_start_time`, `service_end_time`, `other_requirement`, `create_time`) VALUES (50, 'APP1772177339818d6120c', NULL, '范涵伶', '15520765697', '[\"胸痛\"]', '成都市', 1, '2026-02-27', '08:00', '10:30', '', '2026-02-27 15:29:00');
INSERT INTO `guide_appointment` (`id`, `appointment_no`, `user_id`, `patient_name`, `patient_phone`, `symptoms`, `hospital_name`, `service_type_number`, `service_date`, `service_start_time`, `service_end_time`, `other_requirement`, `create_time`) VALUES (51, 'APP1772184911807e9d59e', NULL, '范涵伶', '15520765697', '[\"恶心呕吐\"]', 'cda', 1, '2026-02-27', '08:00', '12:30', '', '2026-02-27 17:35:12');
INSERT INTO `guide_appointment` (`id`, `appointment_no`, `user_id`, `patient_name`, `patient_phone`, `symptoms`, `hospital_name`, `service_type_number`, `service_date`, `service_start_time`, `service_end_time`, `other_requirement`, `create_time`) VALUES (52, 'APP17721851896979a985d', NULL, '范涵伶', '15520765697', '[]', '成都市', 1, '2026-02-27', '08:00', '12:00', '', '2026-02-27 17:39:50');
INSERT INTO `guide_appointment` (`id`, `appointment_no`, `user_id`, `patient_name`, `patient_phone`, `symptoms`, `hospital_name`, `service_type_number`, `service_date`, `service_start_time`, `service_end_time`, `other_requirement`, `create_time`) VALUES (53, 'APP1772185891746594f49', NULL, '范涵伶', '15520765697', '[\"胸痛\"]', '成都市', 1, '2026-02-27', '08:00', '12:00', '', '2026-02-27 17:51:32');
INSERT INTO `guide_appointment` (`id`, `appointment_no`, `user_id`, `patient_name`, `patient_phone`, `symptoms`, `hospital_name`, `service_type_number`, `service_date`, `service_start_time`, `service_end_time`, `other_requirement`, `create_time`) VALUES (54, 'APP177218642031426bb39', NULL, '范涵伶', '15520765697', '[\"腹痛\"]', '成都市', 1, '2026-02-27', '08:00', '11:00', '', '2026-02-27 18:00:20');
INSERT INTO `guide_appointment` (`id`, `appointment_no`, `user_id`, `patient_name`, `patient_phone`, `symptoms`, `hospital_name`, `service_type_number`, `service_date`, `service_start_time`, `service_end_time`, `other_requirement`, `create_time`) VALUES (55, 'APP1772186491023186247', NULL, '范涵伶', '15520765697', '[\"胸痛\"]', '成都市', 1, '2026-02-27', '08:00', '10:00', '', '2026-02-27 18:01:31');
INSERT INTO `guide_appointment` (`id`, `appointment_no`, `user_id`, `patient_name`, `patient_phone`, `symptoms`, `hospital_name`, `service_type_number`, `service_date`, `service_start_time`, `service_end_time`, `other_requirement`, `create_time`) VALUES (56, 'APP1772186820807f55fd5', NULL, '范涵伶', '15520765697', '[\"胸痛\"]', '成都市', 1, '2026-02-27', '08:00', '10:30', '', '2026-02-27 18:07:01');
INSERT INTO `guide_appointment` (`id`, `appointment_no`, `user_id`, `patient_name`, `patient_phone`, `symptoms`, `hospital_name`, `service_type_number`, `service_date`, `service_start_time`, `service_end_time`, `other_requirement`, `create_time`) VALUES (57, 'APP1772188399298b3f3bb', NULL, '范涵伶', '15520765697', '[\"胸痛\"]', '成都市', 3, '2026-02-27', '08:00', '11:00', '', '2026-02-27 18:33:19');
INSERT INTO `guide_appointment` (`id`, `appointment_no`, `user_id`, `patient_name`, `patient_phone`, `symptoms`, `hospital_name`, `service_type_number`, `service_date`, `service_start_time`, `service_end_time`, `other_requirement`, `create_time`) VALUES (58, 'APP17721912288718621a7', NULL, '范涵伶', '15520765697', '[\"胸痛\"]', '成都市', 3, '2026-02-27', '08:00', '11:00', '', '2026-02-27 19:20:29');
INSERT INTO `guide_appointment` (`id`, `appointment_no`, `user_id`, `patient_name`, `patient_phone`, `symptoms`, `hospital_name`, `service_type_number`, `service_date`, `service_start_time`, `service_end_time`, `other_requirement`, `create_time`) VALUES (59, 'APP17722623900267c0721', NULL, '范涵伶', '15520765697', '[\"发热\"]', '成都市', 4, '2026-02-28', '08:00', '11:00', '', '2026-02-28 15:06:30');
INSERT INTO `guide_appointment` (`id`, `appointment_no`, `user_id`, `patient_name`, `patient_phone`, `symptoms`, `hospital_name`, `service_type_number`, `service_date`, `service_start_time`, `service_end_time`, `other_requirement`, `create_time`) VALUES (60, 'APP1772287600469d5925e', NULL, '范涵伶', '15520765697', '[\"恶心呕吐\", \"腹痛\"]', '成都市医院', 2, '2026-02-28', '08:00', '11:30', '', '2026-02-28 22:06:40');
INSERT INTO `guide_appointment` (`id`, `appointment_no`, `user_id`, `patient_name`, `patient_phone`, `symptoms`, `hospital_name`, `service_type_number`, `service_date`, `service_start_time`, `service_end_time`, `other_requirement`, `create_time`) VALUES (61, 'APP1773152493668c50d89', NULL, '范涵伶', '15520765697', '[\"胸痛\", \"头痛\"]', '成都市中医院', 1, '2026-03-11', '08:00', '11:00', '', '2026-03-10 22:21:34');
INSERT INTO `guide_appointment` (`id`, `appointment_no`, `user_id`, `patient_name`, `patient_phone`, `symptoms`, `hospital_name`, `service_type_number`, `service_date`, `service_start_time`, `service_end_time`, `other_requirement`, `create_time`) VALUES (62, 'APP177329804436434b23f', NULL, '范涵伶', '15520765697', '[\"发热\"]', '都江堰市医院', 3, '2026-03-13', '08:00', '10:00', '', '2026-03-12 14:47:24');
INSERT INTO `guide_appointment` (`id`, `appointment_no`, `user_id`, `patient_name`, `patient_phone`, `symptoms`, `hospital_name`, `service_type_number`, `service_date`, `service_start_time`, `service_end_time`, `other_requirement`, `create_time`) VALUES (63, 'APP1773310486529195c5c', NULL, '范涵伶', '15520765697', '[\"发热\"]', '都江堰医院', 1, '2026-03-12', '08:00', '11:30', '', '2026-03-12 18:14:47');
COMMIT;

-- ----------------------------
-- Table structure for order
-- ----------------------------
DROP TABLE IF EXISTS `order`;
CREATE TABLE `order` (
  `order_id` int NOT NULL AUTO_INCREMENT,
  `order_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '订单号',
  `user_id` int NOT NULL COMMENT '下单用户ID',
  `attendant_id` int DEFAULT NULL COMMENT '接单陪诊师ID',
  `attendant_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '陪诊师姓名',
  `attendant_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '陪诊师电话',
  `patient_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '就诊人姓名',
  `patient_age` int DEFAULT '0' COMMENT '就诊人年龄',
  `patient_sex` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '未知' COMMENT '就诊人性别',
  `contact_person` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `contact_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `hospital` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `service_content` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `clinic_type` int DEFAULT '1',
  `service_date` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `service_time_slot` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `special_requirements` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '特殊需求',
  `custom_requirement` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '自定义需求',
  `order_amount` decimal(10,2) NOT NULL DEFAULT '0.00',
  `payment_status` int DEFAULT '0' COMMENT '0=待支付, 1=已支付',
  `payment_time` datetime DEFAULT NULL,
  `order_status` int DEFAULT '0' COMMENT '0=待支付, 1=待接单, 2=待服务, 3=服务中, 4=待确认时长费用, 5=时长费用有争议, 6=已完成, 7=已取消',
  `cancel_reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '取消原因',
  `cancel_time` datetime DEFAULT NULL COMMENT '取消时间',
  `cancel_by` tinyint DEFAULT NULL COMMENT '取消方：0用户 1陪诊师 2系统',
  `penalty_rate` decimal(5,2) DEFAULT NULL COMMENT '违约金比例(0-1)',
  `penalty_amount` decimal(10,2) DEFAULT NULL COMMENT '违约金金额',
  `refund_amount` decimal(10,2) DEFAULT NULL COMMENT '退款金额',
  `qr_code_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '核销二维码URL',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `service_start_time` datetime DEFAULT NULL COMMENT '服务开始时间',
  `service_end_time` datetime DEFAULT NULL COMMENT '服务结束时间',
  `service_progress_step` tinyint DEFAULT NULL COMMENT '服务进度：1=已到院, 2=候诊中, 3=检查中, 4=就诊完成',
  `estimated_duration` decimal(5,2) DEFAULT NULL COMMENT '预估服务时长(小时)',
  `actual_duration` decimal(5,2) DEFAULT NULL COMMENT '实际服务时长(小时)',
  `balance_amount` decimal(10,2) DEFAULT NULL COMMENT '差价金额（正数需补付，负数自动退款）',
  `time_dispute_user_duration` decimal(5,2) DEFAULT NULL COMMENT '用户申诉的实际时长(小时)',
  `time_dispute_reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '用户申诉说明',
  PRIMARY KEY (`order_id`) USING BTREE,
  UNIQUE KEY `uk_order_no` (`order_no`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=62 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='正式订单表';

-- ----------------------------
-- Records of order
-- ----------------------------
BEGIN;
INSERT INTO `order` (`order_id`, `order_no`, `user_id`, `attendant_id`, `attendant_name`, `attendant_phone`, `patient_name`, `patient_age`, `patient_sex`, `contact_person`, `contact_phone`, `hospital`, `service_content`, `clinic_type`, `service_date`, `service_time_slot`, `special_requirements`, `custom_requirement`, `order_amount`, `payment_status`, `payment_time`, `order_status`, `cancel_reason`, `cancel_time`, `cancel_by`, `penalty_rate`, `penalty_amount`, `refund_amount`, `qr_code_url`, `create_time`, `service_start_time`, `service_end_time`, `service_progress_step`, `estimated_duration`, `actual_duration`, `balance_amount`, `time_dispute_user_duration`, `time_dispute_reason`) VALUES (45, 'ORD177217538128245f6c9', 15, 21, '李怀', NULL, '范涵伶', 22, '男', '范涵伶', '15520765697', '成都市', '急诊陪同', 3, '2026-02-27', '08:00-10:30', '发热', '无', 180.00, 1, '2026-02-27 14:56:37', 2, NULL, NULL, NULL, NULL, NULL, NULL, 'https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=SERVICE_CONFIRM_45', '2026-02-27 14:56:21', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `order` (`order_id`, `order_no`, `user_id`, `attendant_id`, `attendant_name`, `attendant_phone`, `patient_name`, `patient_age`, `patient_sex`, `contact_person`, `contact_phone`, `hospital`, `service_content`, `clinic_type`, `service_date`, `service_time_slot`, `special_requirements`, `custom_requirement`, `order_amount`, `payment_status`, `payment_time`, `order_status`, `cancel_reason`, `cancel_time`, `cancel_by`, `penalty_rate`, `penalty_amount`, `refund_amount`, `qr_code_url`, `create_time`, `service_start_time`, `service_end_time`, `service_progress_step`, `estimated_duration`, `actual_duration`, `balance_amount`, `time_dispute_user_duration`, `time_dispute_reason`) VALUES (46, 'ORD17721767672500e0d4f', 15, NULL, NULL, NULL, '范涵伶', 22, '男', '范涵伶', '15520765697', '成都市', '普通陪诊', 1, '2026-02-27', '08:00-10:00', '胸痛', '无', 50.00, 1, '2026-02-27 15:19:29', 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2026-02-27 15:19:27', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `order` (`order_id`, `order_no`, `user_id`, `attendant_id`, `attendant_name`, `attendant_phone`, `patient_name`, `patient_age`, `patient_sex`, `contact_person`, `contact_phone`, `hospital`, `service_content`, `clinic_type`, `service_date`, `service_time_slot`, `special_requirements`, `custom_requirement`, `order_amount`, `payment_status`, `payment_time`, `order_status`, `cancel_reason`, `cancel_time`, `cancel_by`, `penalty_rate`, `penalty_amount`, `refund_amount`, `qr_code_url`, `create_time`, `service_start_time`, `service_end_time`, `service_progress_step`, `estimated_duration`, `actual_duration`, `balance_amount`, `time_dispute_user_duration`, `time_dispute_reason`) VALUES (47, 'ORD17721772386768f50f7', 15, 21, '李怀', NULL, '范涵伶', 22, '男', '范涵伶', '15520765697', '成都', '普通陪诊', 1, '2026-02-27', '08:00-09:30', NULL, '无', 50.00, 1, '2026-02-27 15:27:20', 3, NULL, NULL, NULL, NULL, NULL, NULL, 'https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=SERVICE_CONFIRM_47', '2026-02-27 15:27:19', '2026-02-28 22:08:45', NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `order` (`order_id`, `order_no`, `user_id`, `attendant_id`, `attendant_name`, `attendant_phone`, `patient_name`, `patient_age`, `patient_sex`, `contact_person`, `contact_phone`, `hospital`, `service_content`, `clinic_type`, `service_date`, `service_time_slot`, `special_requirements`, `custom_requirement`, `order_amount`, `payment_status`, `payment_time`, `order_status`, `cancel_reason`, `cancel_time`, `cancel_by`, `penalty_rate`, `penalty_amount`, `refund_amount`, `qr_code_url`, `create_time`, `service_start_time`, `service_end_time`, `service_progress_step`, `estimated_duration`, `actual_duration`, `balance_amount`, `time_dispute_user_duration`, `time_dispute_reason`) VALUES (48, 'ORD1772177339845b88d27', 15, 21, '李怀', NULL, '范涵伶', 22, '男', '范涵伶', '15520765697', '成都市', '普通陪诊', 1, '2026-02-27', '08:00-10:30', '胸痛', '无', 80.00, 1, '2026-02-27 15:29:02', 3, NULL, NULL, NULL, NULL, NULL, NULL, 'https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=SERVICE_CONFIRM_48', '2026-02-27 15:29:00', '2026-02-28 15:52:59', NULL, 4, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `order` (`order_id`, `order_no`, `user_id`, `attendant_id`, `attendant_name`, `attendant_phone`, `patient_name`, `patient_age`, `patient_sex`, `contact_person`, `contact_phone`, `hospital`, `service_content`, `clinic_type`, `service_date`, `service_time_slot`, `special_requirements`, `custom_requirement`, `order_amount`, `payment_status`, `payment_time`, `order_status`, `cancel_reason`, `cancel_time`, `cancel_by`, `penalty_rate`, `penalty_amount`, `refund_amount`, `qr_code_url`, `create_time`, `service_start_time`, `service_end_time`, `service_progress_step`, `estimated_duration`, `actual_duration`, `balance_amount`, `time_dispute_user_duration`, `time_dispute_reason`) VALUES (49, 'ORD1772184911870ee26b8', 15, 21, '李怀', NULL, '范涵伶', 22, '男', '范涵伶', '15520765697', 'cda', '普通陪诊', 1, '2026-02-27', '08:00-12:30', '恶心呕吐', '无', 170.00, 1, '2026-02-27 17:35:13', 6, NULL, NULL, NULL, NULL, NULL, NULL, 'https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=SERVICE_CONFIRM_49', '2026-02-27 17:35:12', '2026-02-27 17:36:53', '2026-02-27 17:37:02', 4, 4.50, 5.50, 30.00, NULL, NULL);
INSERT INTO `order` (`order_id`, `order_no`, `user_id`, `attendant_id`, `attendant_name`, `attendant_phone`, `patient_name`, `patient_age`, `patient_sex`, `contact_person`, `contact_phone`, `hospital`, `service_content`, `clinic_type`, `service_date`, `service_time_slot`, `special_requirements`, `custom_requirement`, `order_amount`, `payment_status`, `payment_time`, `order_status`, `cancel_reason`, `cancel_time`, `cancel_by`, `penalty_rate`, `penalty_amount`, `refund_amount`, `qr_code_url`, `create_time`, `service_start_time`, `service_end_time`, `service_progress_step`, `estimated_duration`, `actual_duration`, `balance_amount`, `time_dispute_user_duration`, `time_dispute_reason`) VALUES (50, 'ORD17721851897289aebd7', 15, 21, '李怀', NULL, '范涵伶', 22, '男', '范涵伶', '15520765697', '成都市', '普通陪诊', 1, '2026-02-27', '08:00-12:00', NULL, '无', 110.00, 1, '2026-02-27 17:39:52', 4, NULL, NULL, NULL, NULL, NULL, NULL, 'https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=SERVICE_CONFIRM_50', '2026-02-27 17:39:50', '2026-02-27 17:55:17', '2026-02-27 17:55:23', 4, 4.00, 4.50, 30.00, NULL, NULL);
INSERT INTO `order` (`order_id`, `order_no`, `user_id`, `attendant_id`, `attendant_name`, `attendant_phone`, `patient_name`, `patient_age`, `patient_sex`, `contact_person`, `contact_phone`, `hospital`, `service_content`, `clinic_type`, `service_date`, `service_time_slot`, `special_requirements`, `custom_requirement`, `order_amount`, `payment_status`, `payment_time`, `order_status`, `cancel_reason`, `cancel_time`, `cancel_by`, `penalty_rate`, `penalty_amount`, `refund_amount`, `qr_code_url`, `create_time`, `service_start_time`, `service_end_time`, `service_progress_step`, `estimated_duration`, `actual_duration`, `balance_amount`, `time_dispute_user_duration`, `time_dispute_reason`) VALUES (51, 'ORD1772185891805b1f712', 15, 21, '李怀', NULL, '范涵伶', 22, '男', '范涵伶', '15520765697', '成都市', '普通陪诊', 1, '2026-02-27', '08:00-12:00', '胸痛', '无', 140.00, 1, '2026-02-27 17:51:40', 6, NULL, NULL, NULL, NULL, NULL, NULL, 'https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=SERVICE_CONFIRM_51', '2026-02-27 17:51:32', '2026-02-27 18:36:45', '2026-02-27 18:41:34', 4, 4.00, 4.50, 30.00, NULL, NULL);
INSERT INTO `order` (`order_id`, `order_no`, `user_id`, `attendant_id`, `attendant_name`, `attendant_phone`, `patient_name`, `patient_age`, `patient_sex`, `contact_person`, `contact_phone`, `hospital`, `service_content`, `clinic_type`, `service_date`, `service_time_slot`, `special_requirements`, `custom_requirement`, `order_amount`, `payment_status`, `payment_time`, `order_status`, `cancel_reason`, `cancel_time`, `cancel_by`, `penalty_rate`, `penalty_amount`, `refund_amount`, `qr_code_url`, `create_time`, `service_start_time`, `service_end_time`, `service_progress_step`, `estimated_duration`, `actual_duration`, `balance_amount`, `time_dispute_user_duration`, `time_dispute_reason`) VALUES (52, 'ORD17721864203592d5f4c', 15, 21, '李怀', NULL, '范涵伶', 22, '男', '范涵伶', '15520765697', '成都市', '普通陪诊', 1, '2026-02-27', '08:00-11:00', '腹痛', '无', 140.00, 1, '2026-02-27 18:00:22', 6, NULL, NULL, NULL, NULL, NULL, NULL, 'https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=SERVICE_CONFIRM_52', '2026-02-27 18:00:20', '2026-02-27 18:36:01', '2026-02-28 01:08:57', 4, 3.00, 4.50, 60.00, NULL, NULL);
INSERT INTO `order` (`order_id`, `order_no`, `user_id`, `attendant_id`, `attendant_name`, `attendant_phone`, `patient_name`, `patient_age`, `patient_sex`, `contact_person`, `contact_phone`, `hospital`, `service_content`, `clinic_type`, `service_date`, `service_time_slot`, `special_requirements`, `custom_requirement`, `order_amount`, `payment_status`, `payment_time`, `order_status`, `cancel_reason`, `cancel_time`, `cancel_by`, `penalty_rate`, `penalty_amount`, `refund_amount`, `qr_code_url`, `create_time`, `service_start_time`, `service_end_time`, `service_progress_step`, `estimated_duration`, `actual_duration`, `balance_amount`, `time_dispute_user_duration`, `time_dispute_reason`) VALUES (53, 'ORD17721864910689bda5e', 15, NULL, NULL, NULL, '范涵伶', 22, '男', '范涵伶', '15520765697', '成都市', '普通陪诊', 1, '2026-02-27', '08:00-10:00', '胸痛', '无', 50.00, 0, NULL, 7, '超时未支付自动取消', '2026-02-27 18:41:43', 0, 0.00, 0.00, 50.00, NULL, '2026-02-27 18:01:31', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `order` (`order_id`, `order_no`, `user_id`, `attendant_id`, `attendant_name`, `attendant_phone`, `patient_name`, `patient_age`, `patient_sex`, `contact_person`, `contact_phone`, `hospital`, `service_content`, `clinic_type`, `service_date`, `service_time_slot`, `special_requirements`, `custom_requirement`, `order_amount`, `payment_status`, `payment_time`, `order_status`, `cancel_reason`, `cancel_time`, `cancel_by`, `penalty_rate`, `penalty_amount`, `refund_amount`, `qr_code_url`, `create_time`, `service_start_time`, `service_end_time`, `service_progress_step`, `estimated_duration`, `actual_duration`, `balance_amount`, `time_dispute_user_duration`, `time_dispute_reason`) VALUES (54, 'ORD1772186820885b73960', 15, NULL, NULL, NULL, '范涵伶', 22, '男', '范涵伶', '15520765697', '成都市', '普通陪诊', 1, '2026-02-27', '08:00-10:30', '胸痛', '无', 80.00, 0, NULL, 7, '超时未支付自动取消', '2026-02-27 18:41:40', 0, 0.00, 0.00, 80.00, NULL, '2026-02-27 18:07:01', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `order` (`order_id`, `order_no`, `user_id`, `attendant_id`, `attendant_name`, `attendant_phone`, `patient_name`, `patient_age`, `patient_sex`, `contact_person`, `contact_phone`, `hospital`, `service_content`, `clinic_type`, `service_date`, `service_time_slot`, `special_requirements`, `custom_requirement`, `order_amount`, `payment_status`, `payment_time`, `order_status`, `cancel_reason`, `cancel_time`, `cancel_by`, `penalty_rate`, `penalty_amount`, `refund_amount`, `qr_code_url`, `create_time`, `service_start_time`, `service_end_time`, `service_progress_step`, `estimated_duration`, `actual_duration`, `balance_amount`, `time_dispute_user_duration`, `time_dispute_reason`) VALUES (55, 'ORD17721883993729bc4e8', 15, 21, '李怀', NULL, '范涵伶', 22, '男', '范涵伶', '15520765697', '成都市', '急诊陪同', 3, '2026-02-27', '08:00-11:00', '胸痛', '无', 210.00, 1, '2026-02-27 18:33:27', 6, NULL, NULL, NULL, NULL, NULL, NULL, 'https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=SERVICE_CONFIRM_55', '2026-02-27 18:33:19', '2026-02-27 18:34:14', '2026-02-27 18:34:18', NULL, 3.00, 4.00, 30.00, NULL, NULL);
INSERT INTO `order` (`order_id`, `order_no`, `user_id`, `attendant_id`, `attendant_name`, `attendant_phone`, `patient_name`, `patient_age`, `patient_sex`, `contact_person`, `contact_phone`, `hospital`, `service_content`, `clinic_type`, `service_date`, `service_time_slot`, `special_requirements`, `custom_requirement`, `order_amount`, `payment_status`, `payment_time`, `order_status`, `cancel_reason`, `cancel_time`, `cancel_by`, `penalty_rate`, `penalty_amount`, `refund_amount`, `qr_code_url`, `create_time`, `service_start_time`, `service_end_time`, `service_progress_step`, `estimated_duration`, `actual_duration`, `balance_amount`, `time_dispute_user_duration`, `time_dispute_reason`) VALUES (56, 'ORD1772191228942f306f5', 15, 21, '李怀', NULL, '范涵伶', 22, '男', '范涵伶', '15520765697', '成都市', '急诊陪同', 3, '2026-02-27', '08:00-11:00', '胸痛', '无', 150.00, 1, '2026-02-27 19:20:33', 6, NULL, NULL, NULL, NULL, NULL, 30.00, 'https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=SERVICE_CONFIRM_56', '2026-02-27 19:20:29', '2026-02-27 19:21:38', '2026-02-27 19:26:06', 4, 3.00, 2.00, -30.00, NULL, NULL);
INSERT INTO `order` (`order_id`, `order_no`, `user_id`, `attendant_id`, `attendant_name`, `attendant_phone`, `patient_name`, `patient_age`, `patient_sex`, `contact_person`, `contact_phone`, `hospital`, `service_content`, `clinic_type`, `service_date`, `service_time_slot`, `special_requirements`, `custom_requirement`, `order_amount`, `payment_status`, `payment_time`, `order_status`, `cancel_reason`, `cancel_time`, `cancel_by`, `penalty_rate`, `penalty_amount`, `refund_amount`, `qr_code_url`, `create_time`, `service_start_time`, `service_end_time`, `service_progress_step`, `estimated_duration`, `actual_duration`, `balance_amount`, `time_dispute_user_duration`, `time_dispute_reason`) VALUES (57, 'ORD1772262390131755423', 15, NULL, NULL, NULL, '范涵伶', 22, '男', '范涵伶', '15520765697', '成都市', '上门陪诊', 4, '2026-02-28', '08:00-11:00', '发热', '无', 110.00, 1, '2026-02-28 15:06:41', 7, '计划有变，暂不就诊', '2026-02-28 15:52:07', 0, 0.00, 0.00, 110.00, NULL, '2026-02-28 15:06:30', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `order` (`order_id`, `order_no`, `user_id`, `attendant_id`, `attendant_name`, `attendant_phone`, `patient_name`, `patient_age`, `patient_sex`, `contact_person`, `contact_phone`, `hospital`, `service_content`, `clinic_type`, `service_date`, `service_time_slot`, `special_requirements`, `custom_requirement`, `order_amount`, `payment_status`, `payment_time`, `order_status`, `cancel_reason`, `cancel_time`, `cancel_by`, `penalty_rate`, `penalty_amount`, `refund_amount`, `qr_code_url`, `create_time`, `service_start_time`, `service_end_time`, `service_progress_step`, `estimated_duration`, `actual_duration`, `balance_amount`, `time_dispute_user_duration`, `time_dispute_reason`) VALUES (58, 'ORD1772287600519548ca7', 15, NULL, NULL, NULL, '范涵伶', 22, '男', '范涵伶', '15520765697', '成都市医院', '术后护理', 2, '2026-02-28', '08:00-11:30', '恶心呕吐,腹痛', '无', 180.00, 1, '2026-02-28 22:06:42', 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2026-02-28 22:06:41', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `order` (`order_id`, `order_no`, `user_id`, `attendant_id`, `attendant_name`, `attendant_phone`, `patient_name`, `patient_age`, `patient_sex`, `contact_person`, `contact_phone`, `hospital`, `service_content`, `clinic_type`, `service_date`, `service_time_slot`, `special_requirements`, `custom_requirement`, `order_amount`, `payment_status`, `payment_time`, `order_status`, `cancel_reason`, `cancel_time`, `cancel_by`, `penalty_rate`, `penalty_amount`, `refund_amount`, `qr_code_url`, `create_time`, `service_start_time`, `service_end_time`, `service_progress_step`, `estimated_duration`, `actual_duration`, `balance_amount`, `time_dispute_user_duration`, `time_dispute_reason`) VALUES (59, 'ORD177315249372547cc64', 15, 21, '李怀', NULL, '范涵伶', 22, '男', '范涵伶', '15520765697', '成都市中医院', '普通陪诊', 1, '2026-03-11', '08:00-11:00', '胸痛,头痛', '无', 110.00, 1, '2026-03-10 22:21:45', 6, NULL, NULL, NULL, NULL, NULL, NULL, 'https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=SERVICE_CONFIRM_59', '2026-03-10 22:21:34', '2026-03-10 22:22:19', '2026-03-10 22:22:52', 4, 3.00, 4.00, 30.00, NULL, NULL);
INSERT INTO `order` (`order_id`, `order_no`, `user_id`, `attendant_id`, `attendant_name`, `attendant_phone`, `patient_name`, `patient_age`, `patient_sex`, `contact_person`, `contact_phone`, `hospital`, `service_content`, `clinic_type`, `service_date`, `service_time_slot`, `special_requirements`, `custom_requirement`, `order_amount`, `payment_status`, `payment_time`, `order_status`, `cancel_reason`, `cancel_time`, `cancel_by`, `penalty_rate`, `penalty_amount`, `refund_amount`, `qr_code_url`, `create_time`, `service_start_time`, `service_end_time`, `service_progress_step`, `estimated_duration`, `actual_duration`, `balance_amount`, `time_dispute_user_duration`, `time_dispute_reason`) VALUES (60, 'ORD1773298044420e4b1ab', 15, 21, '李怀', NULL, '范涵伶', 22, '男', '范涵伶', '15520765697', '都江堰市医院', '急诊陪同', 3, '2026-03-13', '08:00-10:00', '发热', '无', 210.00, 1, '2026-03-12 14:47:38', 6, NULL, NULL, NULL, NULL, NULL, NULL, 'https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=SERVICE_CONFIRM_60', '2026-03-12 14:47:24', '2026-03-12 14:48:25', '2026-03-12 14:48:48', 4, 2.00, 3.50, 60.00, NULL, NULL);
INSERT INTO `order` (`order_id`, `order_no`, `user_id`, `attendant_id`, `attendant_name`, `attendant_phone`, `patient_name`, `patient_age`, `patient_sex`, `contact_person`, `contact_phone`, `hospital`, `service_content`, `clinic_type`, `service_date`, `service_time_slot`, `special_requirements`, `custom_requirement`, `order_amount`, `payment_status`, `payment_time`, `order_status`, `cancel_reason`, `cancel_time`, `cancel_by`, `penalty_rate`, `penalty_amount`, `refund_amount`, `qr_code_url`, `create_time`, `service_start_time`, `service_end_time`, `service_progress_step`, `estimated_duration`, `actual_duration`, `balance_amount`, `time_dispute_user_duration`, `time_dispute_reason`) VALUES (61, 'ORD1773310486563a96d40', 15, 21, '李怀', NULL, '范涵伶', 22, '男', '范涵伶', '15520765697', '都江堰医院', '普通陪诊', 1, '2026-03-12', '08:00-11:30', '发热', '无', 140.00, 1, '2026-03-12 18:15:07', 6, NULL, NULL, NULL, NULL, NULL, NULL, 'https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=SERVICE_CONFIRM_61', '2026-03-12 18:14:47', '2026-03-12 18:15:57', '2026-03-12 18:16:24', 4, 3.50, 4.50, 30.00, NULL, NULL);
COMMIT;

-- ----------------------------
-- Table structure for order_evaluation
-- ----------------------------
DROP TABLE IF EXISTS `order_evaluation`;
CREATE TABLE `order_evaluation` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_id` int NOT NULL COMMENT '订单ID',
  `order_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '订单号',
  `user_id` int NOT NULL COMMENT '评价用户ID',
  `attendant_id` int DEFAULT NULL COMMENT '陪诊师用户ID',
  `rating` int NOT NULL COMMENT '总体评分（1-5星）',
  `tags` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '服务亮点标签，逗号分隔',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '评价内容',
  `attendant_reply` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '陪诊师回复内容',
  `reply_time` datetime DEFAULT NULL COMMENT '陪诊师回复时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_order_id` (`order_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='订单评价表';

-- ----------------------------
-- Records of order_evaluation
-- ----------------------------
BEGIN;
INSERT INTO `order_evaluation` (`id`, `order_id`, `order_no`, `user_id`, `attendant_id`, `rating`, `tags`, `content`, `attendant_reply`, `reply_time`, `create_time`, `update_time`) VALUES (1, 55, 'ORD17721883993729bc4e8', 15, 21, 5, '沟通耐心', '很耐心', '谢谢', '2026-02-28 21:17:24', '2026-02-28 16:35:59', '2026-02-28 21:17:23');
INSERT INTO `order_evaluation` (`id`, `order_id`, `order_no`, `user_id`, `attendant_id`, `rating`, `tags`, `content`, `attendant_reply`, `reply_time`, `create_time`, `update_time`) VALUES (2, 59, 'ORD177315249372547cc64', 15, 21, 5, '沟通耐心', '很好', '谢谢', '2026-03-10 22:23:32', '2026-03-10 22:23:23', '2026-03-10 22:23:31');
INSERT INTO `order_evaluation` (`id`, `order_id`, `order_no`, `user_id`, `attendant_id`, `rating`, `tags`, `content`, `attendant_reply`, `reply_time`, `create_time`, `update_time`) VALUES (3, 60, 'ORD1773298044420e4b1ab', 15, 21, 5, '沟通耐心', '', '谢谢', '2026-03-12 14:49:16', '2026-03-12 14:49:06', '2026-03-12 14:49:15');
INSERT INTO `order_evaluation` (`id`, `order_id`, `order_no`, `user_id`, `attendant_id`, `rating`, `tags`, `content`, `attendant_reply`, `reply_time`, `create_time`, `update_time`) VALUES (4, 61, 'ORD1773310486563a96d40', 15, 21, 5, '沟通耐心', '很好', '谢谢', '2026-03-12 18:16:57', '2026-03-12 18:16:47', '2026-03-12 18:16:56');
COMMIT;

-- ----------------------------
-- Table structure for service_type_mapping
-- ----------------------------
DROP TABLE IF EXISTS `service_type_mapping`;
CREATE TABLE `service_type_mapping` (
  `id` int NOT NULL AUTO_INCREMENT,
  `service_type_number` int NOT NULL,
  `service_type_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `price_base` decimal(10,2) DEFAULT '0.00',
  `price_per_hour` decimal(10,2) DEFAULT '0.00',
  `is_active` tinyint(1) DEFAULT '1',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `service_type_number` (`service_type_number`) USING BTREE,
  KEY `idx_service_type_number` (`service_type_number`) USING BTREE,
  KEY `idx_is_active` (`is_active`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='服务类型映射表';

-- ----------------------------
-- Records of service_type_mapping
-- ----------------------------
BEGIN;
INSERT INTO `service_type_mapping` (`id`, `service_type_number`, `service_type_name`, `description`, `price_base`, `price_per_hour`, `is_active`, `create_time`, `update_time`) VALUES (1, 1, '普通陪诊', '基础陪诊服务，包括陪同就诊、取药等', 50.00, 30.00, 1, '2026-02-09 18:36:02', '2026-02-09 18:36:02');
INSERT INTO `service_type_mapping` (`id`, `service_type_number`, `service_type_name`, `description`, `price_base`, `price_per_hour`, `is_active`, `create_time`, `update_time`) VALUES (2, 2, '术后护理', '专业术后护理服务', 0.00, 45.00, 1, '2026-02-09 18:36:02', '2026-02-09 18:36:02');
INSERT INTO `service_type_mapping` (`id`, `service_type_number`, `service_type_name`, `description`, `price_base`, `price_per_hour`, `is_active`, `create_time`, `update_time`) VALUES (3, 3, '急诊陪同', '急诊情况下的陪诊服务', 50.00, 30.00, 1, '2026-02-09 18:36:02', '2026-02-09 18:36:02');
INSERT INTO `service_type_mapping` (`id`, `service_type_number`, `service_type_name`, `description`, `price_base`, `price_per_hour`, `is_active`, `create_time`, `update_time`) VALUES (4, 4, '上门陪诊', '上门提供陪诊服务', 50.00, 30.00, 1, '2026-02-09 18:36:02', '2026-02-09 18:36:02');
COMMIT;

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '登录用户名',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '登录密码',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '真实姓名/昵称',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '手机号',
  `sex` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '未知' COMMENT '性别',
  `age` int DEFAULT '0' COMMENT '年龄',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '头像URL',
  `user_type` int NOT NULL DEFAULT '0' COMMENT '角色：0=普通用户, 1=陪诊师, 2=管理员',
  `openid` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '微信OpenID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_username` (`username`) USING BTREE,
  UNIQUE KEY `uk_phone` (`phone`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=30 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='基础用户表';

-- ----------------------------
-- Records of user
-- ----------------------------
BEGIN;
INSERT INTO `user` (`id`, `username`, `password`, `name`, `phone`, `sex`, `age`, `avatar`, `user_type`, `openid`, `create_time`) VALUES (1, 'sasa', 'testpassword', 'sara', '13812345678', '女', 25, NULL, 0, 'o123abc456def', '2026-02-12 01:55:08');
INSERT INTO `user` (`id`, `username`, `password`, `name`, `phone`, `sex`, `age`, `avatar`, `user_type`, `openid`, `create_time`) VALUES (9, '小明', 'testpassword', '小明', '13987654321', '男', 25, NULL, 0, 'o789xyz012uvw', '2026-02-12 01:55:08');
INSERT INTO `user` (`id`, `username`, `password`, `name`, `phone`, `sex`, `age`, `avatar`, `user_type`, `openid`, `create_time`) VALUES (10, '小小', '123456', '小小', '13765432109', '女', 36, NULL, 1, 'oabc123def456', '2026-02-12 01:55:08');
INSERT INTO `user` (`id`, `username`, `password`, `name`, `phone`, `sex`, `age`, `avatar`, `user_type`, `openid`, `create_time`) VALUES (11, '小王', '123456', '小王', '13698765432', '男', 25, '', 1, 'odef789ghi012', '2026-02-12 01:55:08');
INSERT INTO `user` (`id`, `username`, `password`, `name`, `phone`, `sex`, `age`, `avatar`, `user_type`, `openid`, `create_time`) VALUES (13, 'liufang', 'liufang', '刘芳', '13806580001', '女', 32, NULL, 1, NULL, '2026-02-12 01:55:08');
INSERT INTO `user` (`id`, `username`, `password`, `name`, `phone`, `sex`, `age`, `avatar`, `user_type`, `openid`, `create_time`) VALUES (15, 'fanfan', '123456', '范涵伶', '15520765697', '男', 22, '/uploads/user1.jpg', 0, NULL, '2026-02-12 01:55:08');
INSERT INTO `user` (`id`, `username`, `password`, `name`, `phone`, `sex`, `age`, `avatar`, `user_type`, `openid`, `create_time`) VALUES (20, 'patient_test', '123456', '张三', '13800138001', '男', 30, NULL, 0, NULL, '2026-02-12 01:55:08');
INSERT INTO `user` (`id`, `username`, `password`, `name`, `phone`, `sex`, `age`, `avatar`, `user_type`, `openid`, `create_time`) VALUES (21, 'lihuai', '123456', '李怀', '13900139002', '男', 35, '/uploads/03_Medicalcompanion.jpg', 1, NULL, '2026-02-12 01:55:08');
INSERT INTO `user` (`id`, `username`, `password`, `name`, `phone`, `sex`, `age`, `avatar`, `user_type`, `openid`, `create_time`) VALUES (22, 'attendant002', '加密后的密码2', '李四', '13800138002', '女', 28, '/uploads/01_Medicalcompanion.jpg', 1, NULL, '2025-07-01 10:06:05');
INSERT INTO `user` (`id`, `username`, `password`, `name`, `phone`, `sex`, `age`, `avatar`, `user_type`, `openid`, `create_time`) VALUES (23, 'attendant003', '加密后的密码3', '王五', '13800138003', '男', 35, '/uploads/04_Medicalcompanion.jpg', 1, NULL, '2025-07-01 10:22:35');
INSERT INTO `user` (`id`, `username`, `password`, `name`, `phone`, `sex`, `age`, `avatar`, `user_type`, `openid`, `create_time`) VALUES (24, 'attendant123', '加密后的密码', '小张', '13800138000', '女', 30, '/uploads/02_Medicalcompanion.jpg', 1, NULL, '2025-07-02 11:06:37');
INSERT INTO `user` (`id`, `username`, `password`, `name`, `phone`, `sex`, `age`, `avatar`, `user_type`, `openid`, `create_time`) VALUES (25, 'youning', '123456', '优宁', '13899990001', '女', 30, '/uploads/03_Medicalcompanion.jpg', 1, NULL, '2025-11-29 22:09:24');
INSERT INTO `user` (`id`, `username`, `password`, `name`, `phone`, `sex`, `age`, `avatar`, `user_type`, `openid`, `create_time`) VALUES (26, 'attendant_temp_02', '123456', '张三', '13899990002', '女', 30, '/uploads/04_Medicalcompanion.jpg', 1, NULL, '2025-12-15 21:24:59');
COMMIT;

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

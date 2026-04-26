SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `order_evaluation`;
DROP TABLE IF EXISTS `chat_message`;
DROP TABLE IF EXISTS `guide_appointment`;
DROP TABLE IF EXISTS `ai_medical_qa`;
DROP TABLE IF EXISTS `admin_task_claim`;
DROP TABLE IF EXISTS `admin_operation_log`;
DROP TABLE IF EXISTS `attendant_qualification_audit_log`;
DROP TABLE IF EXISTS `attendant_qualification`;
DROP TABLE IF EXISTS `attendant`;
DROP TABLE IF EXISTS `service_type_mapping`;
DROP TABLE IF EXISTS `order`;
DROP TABLE IF EXISTS `sys_admin`;
DROP TABLE IF EXISTS `user`;

CREATE TABLE `user` (
  `id` int NOT NULL AUTO_INCREMENT,
  `password` varchar(100) NOT NULL,
  `name` varchar(50) DEFAULT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `sex` varchar(10) DEFAULT 'unknown',
  `age` int DEFAULT 0,
  `avatar` varchar(255) DEFAULT NULL,
  `user_type` int NOT NULL DEFAULT 0,
  `status` tinyint NOT NULL DEFAULT 1,
  `openid` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `sys_admin` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL,
  `phone` varchar(20) NOT NULL,
  `password` varchar(100) NOT NULL,
  `status` tinyint NOT NULL DEFAULT 1,
  `role` varchar(20) NOT NULL DEFAULT 'ADMIN',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `last_login_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_admin_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `admin_operation_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `operator_id` int DEFAULT NULL,
  `operator_name` varchar(50) DEFAULT NULL,
  `operator_phone` varchar(20) DEFAULT NULL,
  `operator_role` varchar(20) NOT NULL DEFAULT 'ADMIN',
  `module` varchar(40) NOT NULL,
  `action` varchar(50) NOT NULL,
  `target_type` varchar(40) DEFAULT NULL,
  `target_id` int DEFAULT NULL,
  `target_label` varchar(100) DEFAULT NULL,
  `from_status` int DEFAULT NULL,
  `to_status` int DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `snapshot_json` json DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_admin_operation_log_create_time` (`create_time`),
  KEY `idx_admin_operation_log_operator` (`operator_id`, `operator_role`, `create_time`),
  KEY `idx_admin_operation_log_module_action` (`module`, `action`, `create_time`),
  KEY `idx_admin_operation_log_target` (`target_type`, `target_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `admin_task_claim` (
  `task_type` varchar(40) NOT NULL,
  `target_id` int NOT NULL,
  `operator_id` int NOT NULL,
  `operator_name` varchar(50) DEFAULT NULL,
  `operator_role` varchar(20) DEFAULT NULL,
  `lock_token` varchar(64) NOT NULL,
  `claimed_at` datetime NOT NULL,
  `expires_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`task_type`, `target_id`),
  KEY `idx_admin_task_claim_operator` (`operator_id`, `expires_at`),
  KEY `idx_admin_task_claim_expires` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `attendant` (
  `user_id` int NOT NULL,
  `certificate` varchar(100) DEFAULT NULL,
  `status` int NOT NULL DEFAULT 0,
  `qualification_status` int NOT NULL DEFAULT 0,
  `qualification_fail_reason` varchar(255) DEFAULT NULL,
  `introduction` text,
  `professional_field` varchar(255) DEFAULT NULL,
  `score` decimal(2,1) DEFAULT 5.0,
  `experience_years` int DEFAULT 0,
  `hospital_name` varchar(100) DEFAULT NULL,
  `service_count` int DEFAULT 0,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`),
  CONSTRAINT `fk_attendant_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `attendant_qualification` (
  `user_id` int NOT NULL,
  `id_card_uploaded` tinyint(1) DEFAULT 0,
  `practice_cert_uploaded` tinyint(1) DEFAULT 0,
  `health_cert_uploaded` tinyint(1) DEFAULT 0,
  `id_card_file_url` varchar(255) DEFAULT NULL,
  `id_card_front_file_url` varchar(255) DEFAULT NULL,
  `id_card_front_scan_file_url` varchar(255) DEFAULT NULL,
  `id_card_back_file_url` varchar(255) DEFAULT NULL,
  `id_card_back_scan_file_url` varchar(255) DEFAULT NULL,
  `practice_cert_file_url` varchar(255) DEFAULT NULL,
  `practice_cert_scan_file_url` varchar(255) DEFAULT NULL,
  `health_cert_file_url` varchar(255) DEFAULT NULL,
  `health_cert_scan_file_url` varchar(255) DEFAULT NULL,
  `practice_cert_expire_date` varchar(20) DEFAULT NULL,
  `health_cert_expire_date` varchar(20) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`),
  CONSTRAINT `fk_attendant_qualification_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `attendant_qualification_audit_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `actor_type` varchar(20) NOT NULL,
  `actor_id` int DEFAULT NULL,
  `actor_name` varchar(50) DEFAULT NULL,
  `actor_phone` varchar(20) DEFAULT NULL,
  `actor_role` varchar(20) DEFAULT NULL,
  `action` varchar(30) NOT NULL,
  `from_status` int DEFAULT NULL,
  `to_status` int DEFAULT NULL,
  `reason` varchar(255) DEFAULT NULL,
  `snapshot_json` json DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_attendant_qualification_audit_user` (`user_id`, `create_time`),
  CONSTRAINT `fk_attendant_qualification_audit_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `service_type_mapping` (
  `id` int NOT NULL AUTO_INCREMENT,
  `service_type_number` int NOT NULL,
  `service_type_name` varchar(100) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `price_base` decimal(10,2) NOT NULL DEFAULT 0.00,
  `price_per_hour` decimal(10,2) NOT NULL DEFAULT 0.00,
  `is_active` tinyint(1) NOT NULL DEFAULT 1,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_service_type_number` (`service_type_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `guide_appointment` (
  `id` int NOT NULL AUTO_INCREMENT,
  `appointment_no` varchar(64) NOT NULL,
  `user_id` int NOT NULL,
  `patient_name` varchar(50) NOT NULL,
  `patient_phone` varchar(20) NOT NULL,
  `symptoms` json DEFAULT NULL,
  `hospital_name` varchar(100) DEFAULT NULL,
  `service_type_number` int DEFAULT NULL,
  `service_date` varchar(20) DEFAULT NULL,
  `service_start_time` varchar(20) DEFAULT NULL,
  `service_end_time` varchar(20) DEFAULT NULL,
  `other_requirement` text,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_guide_appointment_no` (`appointment_no`),
  KEY `idx_guide_appointment_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `order` (
  `order_id` int NOT NULL AUTO_INCREMENT,
  `order_no` varchar(64) NOT NULL,
  `user_id` int NOT NULL,
  `attendant_id` int DEFAULT NULL,
  `attendant_name` varchar(50) DEFAULT NULL,
  `patient_name` varchar(50) DEFAULT NULL,
  `patient_age` int DEFAULT NULL,
  `patient_sex` varchar(10) DEFAULT NULL,
  `contact_person` varchar(50) DEFAULT NULL,
  `contact_phone` varchar(20) DEFAULT NULL,
  `hospital` varchar(100) DEFAULT NULL,
  `service_content` varchar(100) DEFAULT NULL,
  `clinic_type` int DEFAULT NULL,
  `service_date` varchar(20) DEFAULT NULL,
  `service_time_slot` varchar(50) DEFAULT NULL,
  `special_requirements` text,
  `custom_requirement` text,
  `order_amount` decimal(10,2) DEFAULT 0.00,
  `payment_status` int DEFAULT 0,
  `payment_time` datetime DEFAULT NULL,
  `order_status` int DEFAULT 0,
  `cancel_reason` varchar(255) DEFAULT NULL,
  `cancel_time` datetime DEFAULT NULL,
  `cancel_by` int DEFAULT NULL,
  `penalty_rate` decimal(10,2) DEFAULT NULL,
  `penalty_amount` decimal(10,2) DEFAULT NULL,
  `refund_amount` decimal(10,2) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `qr_code_url` varchar(255) DEFAULT NULL,
  `accept_time` datetime DEFAULT NULL,
  `service_start_time` datetime DEFAULT NULL,
  `service_end_time` datetime DEFAULT NULL,
  `service_progress_step` int DEFAULT NULL,
  `estimated_duration` decimal(10,2) DEFAULT NULL,
  `actual_duration` decimal(10,2) DEFAULT NULL,
  `balance_amount` decimal(10,2) DEFAULT NULL,
  `time_dispute_user_duration` decimal(10,2) DEFAULT NULL,
  `time_dispute_reason` varchar(255) DEFAULT NULL,
  `consultation_duration` decimal(10,2) DEFAULT NULL,
  `unit_price` decimal(10,2) DEFAULT NULL,
  `deposit_amount` decimal(10,2) DEFAULT NULL,
  `appointment_time` datetime DEFAULT NULL,
  `electronic_medical_record` varchar(255) DEFAULT NULL,
  `order_date` datetime DEFAULT NULL,
  `verification_code` varchar(64) DEFAULT NULL,
  `guide_appointment_id` varchar(64) DEFAULT NULL,
  `admin_remark` varchar(255) DEFAULT NULL,
  `dispute_resolved_by` int DEFAULT NULL,
  `dispute_resolved_time` datetime DEFAULT NULL,
  PRIMARY KEY (`order_id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_order_user_id` (`user_id`),
  KEY `idx_order_attendant_id` (`attendant_id`),
  KEY `idx_order_status` (`order_status`),
  KEY `idx_order_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `order_evaluation` (
  `id` int NOT NULL AUTO_INCREMENT,
  `order_id` int NOT NULL,
  `order_no` varchar(64) DEFAULT NULL,
  `user_id` int NOT NULL,
  `attendant_id` int NOT NULL,
  `rating` int NOT NULL,
  `tags` varchar(255) DEFAULT NULL,
  `content` text,
  `attendant_reply` text,
  `reply_time` datetime DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_evaluation_order_id` (`order_id`),
  KEY `idx_order_evaluation_attendant_id` (`attendant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `chat_message` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sender_id` int NOT NULL,
  `receiver_id` int NOT NULL,
  `content` text NOT NULL,
  `order_id` int DEFAULT NULL,
  `msg_type` int NOT NULL DEFAULT 1,
  `is_read` tinyint(1) NOT NULL DEFAULT 0,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_chat_receiver_read` (`receiver_id`, `is_read`),
  KEY `idx_chat_pair_time` (`sender_id`, `receiver_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `ai_medical_qa` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` int DEFAULT NULL,
  `conversation_id` varchar(64) DEFAULT NULL,
  `question` text,
  `answer` longtext,
  `qa_status` int DEFAULT 0,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) DEFAULT 0,
  `thinking_process` longtext,
  PRIMARY KEY (`id`),
  KEY `idx_ai_medical_qa_conversation_id` (`conversation_id`),
  KEY `idx_ai_medical_qa_user_time` (`user_id`, `create_time`),
  KEY `idx_ai_medical_qa_user_conversation` (`user_id`, `conversation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `ai_appointment_session` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` varchar(64) NOT NULL,
  `user_id` int DEFAULT NULL,
  `status` varchar(32) NOT NULL,
  `processing_phase` varchar(32) NOT NULL,
  `thinking_process` varchar(255) DEFAULT NULL,
  `assistant_reply` text,
  `assistant_intent` varchar(32) DEFAULT NULL,
  `need_more_info` tinyint(1) NOT NULL DEFAULT 1,
  `missing_fields_json` json DEFAULT NULL,
  `question_type` varchar(64) DEFAULT NULL,
  `question_key` varchar(64) DEFAULT NULL,
  `follow_up_type` varchar(64) DEFAULT NULL,
  `time_proposal_json` json DEFAULT NULL,
  `options_json` json DEFAULT NULL,
  `can_match` tinyint(1) NOT NULL DEFAULT 0,
  `ready_for_confirm` tinyint(1) NOT NULL DEFAULT 0,
  `follow_up_round` int NOT NULL DEFAULT 0,
  `raw_demand_text` text,
  `structured_demand_json` json DEFAULT NULL,
  `field_patch_json` json DEFAULT NULL,
  `confirm_summary_json` json DEFAULT NULL,
  `matched_list_json` json DEFAULT NULL,
  `appointment_no` varchar(64) DEFAULT NULL,
  `degraded` tinyint(1) NOT NULL DEFAULT 0,
  `deleted` tinyint(1) NOT NULL DEFAULT 0,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_appointment_session_id` (`session_id`),
  KEY `idx_ai_appointment_session_user` (`user_id`),
  KEY `idx_ai_appointment_session_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `ai_appointment_message` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` varchar(64) NOT NULL,
  `role` varchar(16) NOT NULL,
  `content` text NOT NULL,
  `field_patch_json` json DEFAULT NULL,
  `deleted` tinyint(1) NOT NULL DEFAULT 0,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ai_appointment_message_session` (`session_id`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO `user` (`id`, `password`, `name`, `phone`, `sex`, `age`, `avatar`, `user_type`, `status`, `openid`, `create_time`) VALUES
  (1, 'admin123', 'Admin', '13800000000', 'male', 30, '/static/uploads/user-avatar.jpg', 2, 1, NULL, NOW()),
  (2, '123456', 'Test User', '13800000001', 'female', 26, '/static/uploads/user-avatar.jpg', 0, 1, NULL, NOW()),
  (3, '123456', 'Test Attendant', '13800000002', 'female', 32, '/static/uploads/user-avatar.jpg', 1, 1, NULL, NOW());

INSERT INTO `sys_admin` (`id`, `name`, `phone`, `password`, `status`, `role`, `create_time`, `update_time`, `last_login_time`) VALUES
  (1, '本地管理员', '13800000000', 'admin123', 1, 'SUPER_ADMIN', NOW(), NOW(), NULL);

INSERT INTO `attendant` (`user_id`, `certificate`, `status`, `qualification_status`, `qualification_fail_reason`, `introduction`, `professional_field`, `score`, `experience_years`, `hospital_name`, `service_count`, `create_time`, `update_time`) VALUES
  (3, 'CERT-20260325-001', 1, 1, '', 'Experienced hospital escort with qualification review completed.', 'Registration, consultation, examination', 5.0, 6, 'Fujian Union Hospital', 12, NOW(), NOW());

INSERT INTO `attendant_qualification` (`user_id`, `id_card_uploaded`, `practice_cert_uploaded`, `health_cert_uploaded`, `id_card_file_url`, `id_card_front_file_url`, `id_card_front_scan_file_url`, `id_card_back_file_url`, `id_card_back_scan_file_url`, `practice_cert_file_url`, `practice_cert_scan_file_url`, `health_cert_file_url`, `health_cert_scan_file_url`, `practice_cert_expire_date`, `health_cert_expire_date`, `create_time`, `update_time`) VALUES
  (3, 1, 1, 1, '/static/uploads/qualification/idcard-front.jpg', '/static/uploads/qualification/idcard-front.jpg', '/static/uploads/qualification/idcard-front.jpg', '/static/uploads/qualification/idcard-back.jpg', '/static/uploads/qualification/idcard-back.jpg', '/static/uploads/qualification/practice-cert.jpg', '/static/uploads/qualification/practice-cert.jpg', '/static/uploads/qualification/health-cert.jpg', '/static/uploads/qualification/health-cert.jpg', DATE_FORMAT(DATE_ADD(CURDATE(), INTERVAL 1 YEAR), '%Y-%m-%d'), DATE_FORMAT(DATE_ADD(CURDATE(), INTERVAL 1 YEAR), '%Y-%m-%d'), NOW(), NOW());

INSERT INTO `service_type_mapping` (`id`, `service_type_number`, `service_type_name`, `description`, `price_base`, `price_per_hour`, `is_active`, `create_time`, `update_time`) VALUES
  (1, 1, 'Hospital Escort', 'Basic escort service', 80.00, 50.00, 1, NOW(), NOW()),
  (2, 2, 'Registration Help', 'Registration and queue support', 50.00, 30.00, 1, NOW(), NOW());

INSERT INTO `guide_appointment` (`id`, `appointment_no`, `user_id`, `patient_name`, `patient_phone`, `symptoms`, `hospital_name`, `service_type_number`, `service_date`, `service_start_time`, `service_end_time`, `other_requirement`, `create_time`) VALUES
  (1, 'GA202603250001', 2, 'Test User', '13800000001', JSON_ARRAY('fever', 'cough'), 'Fujian Union Hospital', 1, DATE_FORMAT(CURDATE(), '%Y-%m-%d'), '09:00', '11:00', 'Need on-site support.', NOW());

INSERT INTO `order` (`order_id`, `order_no`, `user_id`, `attendant_id`, `attendant_name`, `patient_name`, `patient_age`, `patient_sex`, `contact_person`, `contact_phone`, `hospital`, `service_content`, `clinic_type`, `service_date`, `service_time_slot`, `special_requirements`, `custom_requirement`, `order_amount`, `payment_status`, `payment_time`, `order_status`, `create_time`, `service_start_time`, `service_end_time`, `estimated_duration`, `actual_duration`, `balance_amount`, `unit_price`, `deposit_amount`, `verification_code`, `guide_appointment_id`) VALUES
  (1, 'ORD202603250001', 2, 3, 'Test Attendant', 'Test User', 26, 'female', 'Test User', '13800000001', 'Fujian Union Hospital', 'Hospital Escort', 1, DATE_FORMAT(CURDATE(), '%Y-%m-%d'), '09:00-11:00', 'Wheelchair support', 'Need faster registration', 180.00, 1, NOW(), 6, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), 2.00, 2.00, 0.00, 50.00, 100.00, 'VERIFY001', 'GA202603250001'),
  (2, 'ORD202603250002', 2, NULL, NULL, 'Test User', 26, 'female', 'Test User', '13800000001', 'Fujian Union Hospital', 'Registration Help', 2, DATE_FORMAT(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '%Y-%m-%d'), '14:00-15:00', 'No special requirements', '', 50.00, 0, NULL, 0, NOW(), NULL, NULL, 1.00, NULL, NULL, 30.00, 20.00, 'VERIFY002', NULL);

INSERT INTO `order_evaluation` (`id`, `order_id`, `order_no`, `user_id`, `attendant_id`, `rating`, `tags`, `content`, `attendant_reply`, `reply_time`, `create_time`, `update_time`) VALUES
  (1, 1, 'ORD202603250001', 2, 3, 5, 'careful,punctual', 'Service was smooth and reassuring.', 'Thank you for your trust.', NOW(), NOW(), NOW());

INSERT INTO `chat_message` (`id`, `sender_id`, `receiver_id`, `content`, `msg_type`, `is_read`, `create_time`) VALUES
  (1, 2, 3, 'Hello, I have arrived at the hospital.', 1, 1, NOW()),
  (2, 3, 2, 'I am at gate 2 and will meet you there.', 1, 0, NOW());

INSERT INTO `ai_medical_qa` (`id`, `user_id`, `conversation_id`, `question`, `answer`, `qa_status`, `create_time`, `update_time`, `deleted`, `thinking_process`) VALUES
  (1, NULL, 'conv-demo-001', 'I have had a fever for three days, should I go to hospital?', 'If the fever persists or worsens, please seek in-person care promptly.', 1, NOW(), NOW(), 0, 'Reviewed symptom duration and recommended timely evaluation.');

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE IF NOT EXISTS `ai_appointment_session` (
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

CREATE TABLE IF NOT EXISTS `ai_appointment_message` (
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

SET @stmt := IF (
  EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_appointment_session'
      AND COLUMN_NAME = 'assistant_intent'
  ),
  'SELECT 1',
  'ALTER TABLE ai_appointment_session ADD COLUMN assistant_intent varchar(32) DEFAULT NULL AFTER assistant_reply'
);
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @stmt := IF (
  EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_appointment_session'
      AND COLUMN_NAME = 'question_key'
  ),
  'SELECT 1',
  'ALTER TABLE ai_appointment_session ADD COLUMN question_key varchar(64) DEFAULT NULL AFTER question_type'
);
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @stmt := IF (
  EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_appointment_session'
      AND COLUMN_NAME = 'time_proposal_json'
  ),
  'SELECT 1',
  'ALTER TABLE ai_appointment_session ADD COLUMN time_proposal_json json DEFAULT NULL AFTER follow_up_type'
);
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

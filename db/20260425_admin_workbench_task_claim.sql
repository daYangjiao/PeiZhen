SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `admin_task_claim` (
  `task_type` varchar(40) NOT NULL,
  `target_id` int NOT NULL,
  `operator_id` int NOT NULL,
  `operator_name` varchar(50) DEFAULT NULL,
  `operator_role` varchar(20) NOT NULL DEFAULT 'ADMIN',
  `lock_token` varchar(64) NOT NULL,
  `claimed_at` datetime NOT NULL,
  `expires_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`task_type`, `target_id`),
  KEY `idx_admin_task_claim_operator` (`operator_id`, `expires_at`),
  KEY `idx_admin_task_claim_expires` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

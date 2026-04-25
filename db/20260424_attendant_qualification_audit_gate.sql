SET @schema_name = DATABASE();

SET @sql = IF(
  NOT EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'attendant_qualification'
      AND COLUMN_NAME = 'practice_cert_expire_date'
  ),
  'ALTER TABLE `attendant_qualification` ADD COLUMN `practice_cert_expire_date` varchar(20) DEFAULT NULL AFTER `health_cert_file_url`',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  NOT EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'attendant_qualification'
      AND COLUMN_NAME = 'health_cert_expire_date'
  ),
  'ALTER TABLE `attendant_qualification` ADD COLUMN `health_cert_expire_date` varchar(20) DEFAULT NULL AFTER `practice_cert_expire_date`',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS `attendant_qualification_audit_log` (
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

UPDATE `attendant_qualification`
SET
  `practice_cert_expire_date` = COALESCE(`practice_cert_expire_date`, DATE_FORMAT(DATE_ADD(CURDATE(), INTERVAL 1 YEAR), '%Y-%m-%d')),
  `health_cert_expire_date` = COALESCE(`health_cert_expire_date`, DATE_FORMAT(DATE_ADD(CURDATE(), INTERVAL 1 YEAR), '%Y-%m-%d'))
WHERE `practice_cert_uploaded` = 1
  AND `health_cert_uploaded` = 1;

UPDATE `sys_admin`
SET `role` = 'SUPER_ADMIN'
WHERE `phone` = '18650680037';

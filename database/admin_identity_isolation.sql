CREATE TABLE IF NOT EXISTS `sys_admin` (
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

SET @stmt := IF(
  EXISTS(
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_admin'
      AND COLUMN_NAME = 'role'
  ),
  'SELECT 1',
  'ALTER TABLE sys_admin ADD COLUMN `role` varchar(20) NOT NULL DEFAULT ''ADMIN'' AFTER status'
);
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

INSERT INTO `sys_admin` (`name`, `phone`, `password`, `status`, `role`)
SELECT '本地管理员', '13800000000', 'admin123', 1, 'SUPER_ADMIN'
WHERE NOT EXISTS (
  SELECT 1
  FROM `sys_admin`
  WHERE `phone` = '13800000000'
);

UPDATE `sys_admin`
SET `role` = 'SUPER_ADMIN'
WHERE `phone` = '13800000000';

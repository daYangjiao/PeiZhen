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

UPDATE `sys_admin`
SET `role` = 'SUPER_ADMIN'
WHERE `role` IS NULL
   OR `role` = ''
   OR `phone` = '13800000000';

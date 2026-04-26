SET @schema_name = DATABASE();
SET @qualification_status_exists = (
  SELECT COUNT(*) > 0
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'attendant'
    AND COLUMN_NAME = 'qualification_status'
);

SET @sql = (
  SELECT IF(
    @qualification_status_exists = 0,
    'ALTER TABLE `attendant` ADD COLUMN `qualification_status` int NOT NULL DEFAULT 0 AFTER `status`',
    'SELECT 1'
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `attendant`
SET `qualification_status` = CASE
  WHEN `status` = 1 THEN 1
  WHEN `status` IN (2, 3) THEN 2
  ELSE 0
END
WHERE @qualification_status_exists = 0;

UPDATE `user` u
INNER JOIN `attendant` a ON a.user_id = u.id
SET u.status = 0
WHERE a.status = 2
  AND @qualification_status_exists = 0;

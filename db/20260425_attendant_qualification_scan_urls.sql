SET @schema_name = DATABASE();

SET @sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `attendant_qualification` ADD COLUMN `id_card_front_scan_file_url` varchar(255) DEFAULT NULL AFTER `id_card_front_file_url`',
    'SELECT 1'
  )
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'attendant_qualification'
    AND COLUMN_NAME = 'id_card_front_scan_file_url'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `attendant_qualification` ADD COLUMN `id_card_back_scan_file_url` varchar(255) DEFAULT NULL AFTER `id_card_back_file_url`',
    'SELECT 1'
  )
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'attendant_qualification'
    AND COLUMN_NAME = 'id_card_back_scan_file_url'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `attendant_qualification` ADD COLUMN `practice_cert_scan_file_url` varchar(255) DEFAULT NULL AFTER `practice_cert_file_url`',
    'SELECT 1'
  )
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'attendant_qualification'
    AND COLUMN_NAME = 'practice_cert_scan_file_url'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `attendant_qualification` ADD COLUMN `health_cert_scan_file_url` varchar(255) DEFAULT NULL AFTER `health_cert_file_url`',
    'SELECT 1'
  )
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'attendant_qualification'
    AND COLUMN_NAME = 'health_cert_scan_file_url'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `attendant_qualification`
SET
  `id_card_front_scan_file_url` = COALESCE(`id_card_front_scan_file_url`, `id_card_front_file_url`, `id_card_file_url`),
  `id_card_back_scan_file_url` = COALESCE(`id_card_back_scan_file_url`, `id_card_back_file_url`),
  `practice_cert_scan_file_url` = COALESCE(`practice_cert_scan_file_url`, `practice_cert_file_url`),
  `health_cert_scan_file_url` = COALESCE(`health_cert_scan_file_url`, `health_cert_file_url`);

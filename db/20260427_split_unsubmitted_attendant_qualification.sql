ALTER TABLE `attendant`
  MODIFY COLUMN `qualification_status` int NOT NULL DEFAULT 3;

UPDATE `attendant` a
LEFT JOIN `attendant_qualification` q ON q.user_id = a.user_id
SET a.`qualification_status` = 3,
    a.`update_time` = NOW()
WHERE a.`qualification_status` = 0
  AND (
    q.user_id IS NULL
    OR NULLIF(TRIM(COALESCE(q.id_card_front_file_url, q.id_card_file_url, '')), '') IS NULL
    OR NULLIF(TRIM(COALESCE(q.id_card_back_file_url, '')), '') IS NULL
    OR NULLIF(TRIM(COALESCE(q.practice_cert_file_url, '')), '') IS NULL
    OR NULLIF(TRIM(COALESCE(q.health_cert_file_url, '')), '') IS NULL
    OR NULLIF(TRIM(COALESCE(q.practice_cert_expire_date, '')), '') IS NULL
    OR NULLIF(TRIM(COALESCE(q.health_cert_expire_date, '')), '') IS NULL
    OR STR_TO_DATE(q.practice_cert_expire_date, '%Y-%m-%d') < CURDATE()
    OR STR_TO_DATE(q.health_cert_expire_date, '%Y-%m-%d') < CURDATE()
  );

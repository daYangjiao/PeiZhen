-- AI 导诊聊天记录用户隔离迁移脚本。
-- 旧数据保持 user_id=NULL，不自动归属任何用户，避免历史 conversationId 越权恢复。

SET @stmt := IF(
  EXISTS(
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_medical_qa'
      AND COLUMN_NAME = 'user_id'
  ),
  'SELECT 1',
  'ALTER TABLE ai_medical_qa ADD COLUMN user_id int DEFAULT NULL AFTER id'
);
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @stmt := IF(
  EXISTS(
    SELECT 1
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_medical_qa'
      AND INDEX_NAME = 'idx_ai_medical_qa_user_time'
  ),
  'SELECT 1',
  'ALTER TABLE ai_medical_qa ADD INDEX idx_ai_medical_qa_user_time (user_id, create_time)'
);
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @stmt := IF(
  EXISTS(
    SELECT 1
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_medical_qa'
      AND INDEX_NAME = 'idx_ai_medical_qa_user_conversation'
  ),
  'SELECT 1',
  'ALTER TABLE ai_medical_qa ADD INDEX idx_ai_medical_qa_user_conversation (user_id, conversation_id)'
);
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

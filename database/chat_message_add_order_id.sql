ALTER TABLE `chat_message`
ADD COLUMN `order_id` int DEFAULT NULL COMMENT '关联订单ID' AFTER `content`;

CREATE INDEX `idx_chat_message_order_id` ON `chat_message` (`order_id`);

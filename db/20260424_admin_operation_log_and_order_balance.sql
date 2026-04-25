SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `admin_operation_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `operator_id` int DEFAULT NULL,
  `operator_name` varchar(50) DEFAULT NULL,
  `operator_phone` varchar(20) DEFAULT NULL,
  `operator_role` varchar(20) DEFAULT NULL,
  `module` varchar(40) NOT NULL,
  `action` varchar(50) NOT NULL,
  `target_type` varchar(40) DEFAULT NULL,
  `target_id` int DEFAULT NULL,
  `target_label` varchar(100) DEFAULT NULL,
  `from_status` int DEFAULT NULL,
  `to_status` int DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `snapshot_json` json DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_admin_operation_log_create_time` (`create_time`),
  KEY `idx_admin_operation_log_operator` (`operator_id`, `operator_role`, `create_time`),
  KEY `idx_admin_operation_log_module_action` (`module`, `action`, `create_time`),
  KEY `idx_admin_operation_log_target` (`target_type`, `target_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 订单状态本次新增语义：5=平台争议处理中，9=待用户补差额。
-- 相关金额字段沿用 `order.balance_amount`、`order.refund_amount`、`order.payment_time`，无需新增列。

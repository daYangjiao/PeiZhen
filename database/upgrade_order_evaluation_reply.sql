-- 升级脚本：订单评价表新增陪诊师回复字段
-- 执行前请确保 order_evaluation 表已存在（由 upgrade_order_evaluation.sql 创建）

ALTER TABLE `order_evaluation`
  ADD COLUMN `attendant_reply` TEXT NULL DEFAULT NULL COMMENT '陪诊师回复内容' AFTER `content`,
  ADD COLUMN `reply_time` DATETIME NULL DEFAULT NULL COMMENT '陪诊师回复时间' AFTER `attendant_reply`;

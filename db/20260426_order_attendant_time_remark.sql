ALTER TABLE `order`
  ADD COLUMN `attendant_time_remark` varchar(255) DEFAULT NULL AFTER `actual_duration`;

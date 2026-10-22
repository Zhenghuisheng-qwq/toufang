-- R3 report core only. Review/back up before execution; NOT run by this task.
-- No historical snapshot backfill; no changes to payment facts or existing reports.
DELIMITER $$
CREATE PROCEDURE acq_upgrade_r3()
BEGIN
  IF DATABASE() IS NULL OR DATABASE() <> 'toufang' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Select authorized toufang database';
  END IF;
  IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='trade_report' AND column_name='content_text') THEN
    ALTER TABLE `trade_report` ADD COLUMN `content_text` mediumtext;
  END IF;
  IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='trade_report' AND column_name='generation_mode') THEN
    ALTER TABLE `trade_report` ADD COLUMN `generation_mode` varchar(32) DEFAULT NULL;
  END IF;
  IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='trade_report' AND column_name='generated_time') THEN
    ALTER TABLE `trade_report` ADD COLUMN `generated_time` datetime DEFAULT NULL;
  END IF;
  IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='trade_order' AND column_name='report_template_id') THEN
    ALTER TABLE `trade_order` ADD COLUMN `report_template_id` bigint DEFAULT NULL;
  END IF;
  IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='trade_order' AND column_name='report_template_version_id') THEN
    ALTER TABLE `trade_order` ADD COLUMN `report_template_version_id` bigint DEFAULT NULL;
  END IF;
  IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='trade_order' AND column_name='report_input_version') THEN
    ALTER TABLE `trade_order` ADD COLUMN `report_input_version` int DEFAULT NULL;
  END IF;
  IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='trade_order' AND column_name='report_name') THEN
    ALTER TABLE `trade_order` ADD COLUMN `report_name` varchar(64) DEFAULT NULL;
  END IF;
  IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='trade_order' AND column_name='report_gender') THEN
    ALTER TABLE `trade_order` ADD COLUMN `report_gender` tinyint DEFAULT NULL;
  END IF;
  IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='trade_order' AND column_name='report_birth_date') THEN
    ALTER TABLE `trade_order` ADD COLUMN `report_birth_date` date DEFAULT NULL;
  END IF;
  IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='trade_order' AND column_name='report_birth_time') THEN
    ALTER TABLE `trade_order` ADD COLUMN `report_birth_time` time DEFAULT NULL;
  END IF;
  IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='post_pay_task' AND column_name='lease_version') THEN
    ALTER TABLE `post_pay_task` ADD COLUMN `lease_version` bigint NOT NULL DEFAULT 0;
  END IF;
END$$
DELIMITER ;
CALL acq_upgrade_r3();
DROP PROCEDURE acq_upgrade_r3;

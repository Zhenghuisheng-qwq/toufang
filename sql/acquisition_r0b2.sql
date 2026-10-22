-- r0b2: review schema and back up before running. NOT automatically executed.
-- Run only on the explicitly authorized toufang database; DDL auto-commits.
-- Existing columns are preserved: compare their type/default manually before deployment.
DELIMITER $$
CREATE PROCEDURE acq_upgrade_r0b2()
BEGIN
  IF DATABASE() IS NULL OR DATABASE() <> 'toufang' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Select authorized toufang database first';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='pay_merchant' AND column_name='remark') THEN
    ALTER TABLE `pay_merchant` ADD COLUMN `remark` varchar(255) DEFAULT NULL;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='pay_merchant' AND column_name='current_version_id') THEN
    ALTER TABLE `pay_merchant` ADD COLUMN `current_version_id` bigint DEFAULT NULL;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='pay_merchant_version' AND column_name='merchant_serial_no') THEN
    ALTER TABLE `pay_merchant_version` ADD COLUMN `merchant_serial_no` varchar(128) DEFAULT NULL;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='pay_merchant_version' AND column_name='verify_mode') THEN
    ALTER TABLE `pay_merchant_version` ADD COLUMN `verify_mode` varchar(32) NOT NULL DEFAULT 'UNCONFIRMED';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='pay_merchant_version' AND column_name='platform_key_id') THEN
    ALTER TABLE `pay_merchant_version` ADD COLUMN `platform_key_id` varchar(128) DEFAULT NULL;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='pay_merchant_version' AND column_name='platform_public_key_ref') THEN
    ALTER TABLE `pay_merchant_version` ADD COLUMN `platform_public_key_ref` varchar(128) DEFAULT NULL;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='wecom_corp' AND column_name='sid') THEN
    ALTER TABLE `wecom_corp` ADD COLUMN `sid` varchar(64) DEFAULT NULL;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='wecom_corp' AND column_name='auth_mode') THEN
    ALTER TABLE `wecom_corp` ADD COLUMN `auth_mode` varchar(32) NOT NULL DEFAULT 'UNCONFIRMED';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='wecom_corp' AND column_name='remark') THEN
    ALTER TABLE `wecom_corp` ADD COLUMN `remark` varchar(255) DEFAULT NULL;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='wecom_route' AND column_name='entry_type') THEN
    ALTER TABLE `wecom_route` ADD COLUMN `entry_type` tinyint NOT NULL DEFAULT 1;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='wecom_route' AND column_name='link_id') THEN
    ALTER TABLE `wecom_route` ADD COLUMN `link_id` varchar(128) DEFAULT NULL;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='wecom_route' AND column_name='link_url') THEN
    ALTER TABLE `wecom_route` ADD COLUMN `link_url` varchar(512) DEFAULT NULL;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='wecom_route' AND column_name='capability_note') THEN
    ALTER TABLE `wecom_route` ADD COLUMN `capability_note` varchar(255) DEFAULT NULL;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='wecom_route' AND column_name='remark') THEN
    ALTER TABLE `wecom_route` ADD COLUMN `remark` varchar(255) DEFAULT NULL;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='ad_account' AND column_name='interface_mode') THEN
    ALTER TABLE `ad_account` ADD COLUMN `interface_mode` varchar(32) NOT NULL DEFAULT 'UNCONFIRMED';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='ad_account' AND column_name='data_source_id') THEN
    ALTER TABLE `ad_account` ADD COLUMN `data_source_id` varchar(128) DEFAULT NULL;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='ad_account' AND column_name='pay_event') THEN
    ALTER TABLE `ad_account` ADD COLUMN `pay_event` varchar(128) DEFAULT NULL;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='ad_account' AND column_name='amount_unit') THEN
    ALTER TABLE `ad_account` ADD COLUMN `amount_unit` varchar(16) NOT NULL DEFAULT 'UNCONFIRMED';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='ad_account' AND column_name='remark') THEN
    ALTER TABLE `ad_account` ADD COLUMN `remark` varchar(255) DEFAULT NULL;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='wecom_corp' AND index_name='uk_wecom_sid') THEN
    ALTER TABLE wecom_corp ADD UNIQUE KEY uk_wecom_sid (sid);
  END IF;
END$$
DELIMITER ;
CALL acq_upgrade_r0b2();
DROP PROCEDURE acq_upgrade_r0b2;

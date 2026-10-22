-- R4 execution record, 2026-10-22. NOT an idempotent migration runner.
-- Applied once to authorized toufang after schema snapshot and local row backup.
-- Do not replay; inspect information_schema and generate only remaining differences.
-- DDL auto-commits. Enum conversion was guarded by exact backed-up IDs in JDBC.
ALTER TABLE `trade_report` ADD COLUMN `content_text` mediumtext;
ALTER TABLE `trade_report` ADD COLUMN `generation_mode` varchar(32) DEFAULT NULL;
ALTER TABLE `trade_report` ADD COLUMN `generated_time` datetime DEFAULT NULL;
ALTER TABLE `pay_merchant_version` MODIFY COLUMN `merchant_serial_no` varchar(128) DEFAULT NULL;
ALTER TABLE `pay_merchant_version` ADD COLUMN `verify_mode` varchar(32) NOT NULL DEFAULT 'UNCONFIRMED';
ALTER TABLE `pay_merchant_version` ADD COLUMN `platform_key_id` varchar(128) DEFAULT NULL;
ALTER TABLE `pay_merchant_version` ADD COLUMN `platform_public_key_ref` varchar(128) DEFAULT NULL;
ALTER TABLE `trade_order` ADD COLUMN `report_template_id` bigint DEFAULT NULL;
ALTER TABLE `trade_order` ADD COLUMN `report_template_version_id` bigint DEFAULT NULL;
ALTER TABLE `trade_order` ADD COLUMN `report_input_version` int DEFAULT NULL;
ALTER TABLE `trade_order` ADD COLUMN `report_name` varchar(64) DEFAULT NULL;
ALTER TABLE `trade_order` ADD COLUMN `report_gender` tinyint DEFAULT NULL;
ALTER TABLE `trade_order` ADD COLUMN `report_birth_date` date DEFAULT NULL;
ALTER TABLE `trade_order` ADD COLUMN `report_birth_time` time DEFAULT NULL;
ALTER TABLE `wecom_corp` ADD COLUMN `sid` varchar(64) DEFAULT NULL;
ALTER TABLE `wecom_corp` MODIFY COLUMN `auth_mode` varchar(32) NOT NULL DEFAULT 'UNCONFIRMED';
ALTER TABLE `wecom_corp` ADD UNIQUE KEY `uk_wecom_sid` (`sid`);
ALTER TABLE `wecom_route` MODIFY COLUMN `link_id` varchar(128) DEFAULT NULL;
ALTER TABLE `wecom_route` ADD COLUMN `capability_note` varchar(255) DEFAULT NULL;
ALTER TABLE `wecom_route` ADD COLUMN `remark` varchar(255) DEFAULT NULL;
ALTER TABLE `ad_account` ADD COLUMN `interface_mode` varchar(32) NOT NULL DEFAULT 'UNCONFIRMED';
ALTER TABLE `ad_account` ADD COLUMN `data_source_id` varchar(128) DEFAULT NULL;
ALTER TABLE `ad_account` ADD COLUMN `pay_event` varchar(128) DEFAULT NULL;
ALTER TABLE `ad_account` MODIFY COLUMN `amount_unit` varchar(16) NOT NULL DEFAULT 'UNCONFIRMED';
ALTER TABLE `post_pay_task` MODIFY COLUMN `lease_version` bigint NOT NULL DEFAULT 0;

-- Exact guarded enum statements applied in a separate transaction after the DDL.
-- Preserves documented old meanings; not an authorization or platform validation.
START TRANSACTION;
UPDATE `wecom_corp` SET `auth_mode`='SELF_BUILT' WHERE id=1 AND `auth_mode`='1';
UPDATE `wecom_corp` SET `auth_mode`='UNCONFIRMED' WHERE id=2 AND `auth_mode`='0';
UPDATE `ad_account` SET `amount_unit`='UNCONFIRMED' WHERE id=1 AND `amount_unit`='0';
UPDATE `ad_account` SET `amount_unit`='FEN' WHERE id=2 AND `amount_unit`='1';
UPDATE `ad_account` SET `amount_unit`='YUAN' WHERE id=3 AND `amount_unit`='2';
COMMIT;

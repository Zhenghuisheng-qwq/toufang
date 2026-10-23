-- Business-flow increment. Back up affected schema/data before applying. DDL auto-commits.
-- Re-run detects completed objects; never drops business rows.
DELIMITER $$
CREATE PROCEDURE acq_business_upgrade()
BEGIN
IF DATABASE() IS NULL OR DATABASE()<>'toufang' THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Authorized toufang only'; END IF;
CREATE TABLE IF NOT EXISTS `acq_h5_session` (id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID', token_hash char(64) NOT NULL, domain_name varchar(255) NOT NULL, expires_at datetime NOT NULL, revoked tinyint NOT NULL DEFAULT 0, create_time datetime DEFAULT CURRENT_TIMESTAMP, update_time datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, is_delete tinyint NOT NULL DEFAULT 0, PRIMARY KEY(id), UNIQUE KEY uk_h5_token(token_hash)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务链路 acq_h5_session';
CREATE TABLE IF NOT EXISTS `acq_h5_request` (id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID', session_id bigint NOT NULL, request_kind varchar(16) NOT NULL, request_key varchar(64) NOT NULL, request_hash char(64) NOT NULL, result_id varchar(64) NOT NULL, create_time datetime DEFAULT CURRENT_TIMESTAMP, update_time datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, is_delete tinyint NOT NULL DEFAULT 0, PRIMARY KEY(id), UNIQUE KEY uk_h5_request(session_id,request_kind,request_key)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务链路 acq_h5_request';
CREATE TABLE IF NOT EXISTS `acq_pay_evidence` (id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID', order_no varchar(32) NOT NULL, source varchar(16) NOT NULL, evidence_key varchar(128) NOT NULL, evidence_hash char(64) NOT NULL, transaction_id varchar(64) NOT NULL, create_time datetime DEFAULT CURRENT_TIMESTAMP, update_time datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, is_delete tinyint NOT NULL DEFAULT 0, PRIMARY KEY(id), UNIQUE KEY uk_pay_evidence(source,evidence_key)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务链路 acq_pay_evidence';
CREATE TABLE IF NOT EXISTS `wecom_customer_relation` (id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID', corp_id varchar(64) NOT NULL, external_userid varchar(128) NOT NULL, teacher_userid varchar(64) NOT NULL, status tinyint NOT NULL DEFAULT 0, event_time bigint NOT NULL DEFAULT 0, create_time datetime DEFAULT CURRENT_TIMESTAMP, update_time datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, is_delete tinyint NOT NULL DEFAULT 0, PRIMARY KEY(id), UNIQUE KEY uk_wecom_relation(corp_id,external_userid,teacher_userid)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务链路 wecom_customer_relation';
CREATE TABLE IF NOT EXISTS `acq_oauth_state` (id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID', state_hash char(64) NOT NULL, session_id bigint NOT NULL, order_no varchar(32) NOT NULL, app_id varchar(64) NOT NULL, expires_at datetime NOT NULL, consumed tinyint NOT NULL DEFAULT 0, create_time datetime DEFAULT CURRENT_TIMESTAMP, update_time datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, is_delete tinyint NOT NULL DEFAULT 0, PRIMARY KEY(id), UNIQUE KEY uk_oauth_state(state_hash)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务链路 acq_oauth_state';
IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='trade_order' AND COLUMN_NAME='session_id') THEN ALTER TABLE `trade_order` ADD COLUMN `session_id` bigint DEFAULT NULL; END IF;
IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='trade_order' AND COLUMN_NAME='entry_type') THEN ALTER TABLE `trade_order` ADD COLUMN `entry_type` tinyint DEFAULT NULL; END IF;
IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='trade_order' AND COLUMN_NAME='entry_link_id') THEN ALTER TABLE `trade_order` ADD COLUMN `entry_link_id` varchar(128) DEFAULT NULL; END IF;
IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='trade_order' AND COLUMN_NAME='entry_link_url') THEN ALTER TABLE `trade_order` ADD COLUMN `entry_link_url` varchar(512) DEFAULT NULL; END IF;
IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='trade_order' AND COLUMN_NAME='page_title') THEN ALTER TABLE `trade_order` ADD COLUMN `page_title` varchar(128) DEFAULT NULL; END IF;
IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='trade_order' AND COLUMN_NAME='return_path') THEN ALTER TABLE `trade_order` ADD COLUMN `return_path` varchar(255) DEFAULT NULL; END IF;
IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='h5_visit' AND COLUMN_NAME='session_id') THEN ALTER TABLE `h5_visit` ADD COLUMN `session_id` bigint DEFAULT NULL; END IF;
IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='h5_lead' AND COLUMN_NAME='session_id') THEN ALTER TABLE `h5_lead` ADD COLUMN `session_id` bigint DEFAULT NULL; END IF;
IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='landing_page' AND COLUMN_NAME='ad_account_id') THEN ALTER TABLE `landing_page` ADD COLUMN `ad_account_id` bigint DEFAULT NULL; END IF;
IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='pay_record' AND COLUMN_NAME='confirmation_source') THEN ALTER TABLE `pay_record` ADD COLUMN `confirmation_source` varchar(16) DEFAULT NULL; END IF;
IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='pay_record' AND COLUMN_NAME='evidence_hash') THEN ALTER TABLE `pay_record` ADD COLUMN `evidence_hash` char(64) DEFAULT NULL; END IF;
IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='pay_record' AND COLUMN_NAME='payment_url') THEN ALTER TABLE `pay_record` ADD COLUMN `payment_url` varchar(2048) DEFAULT NULL; END IF;
IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='pay_record' AND COLUMN_NAME='prepay_id') THEN ALTER TABLE `pay_record` ADD COLUMN `prepay_id` varchar(256) DEFAULT NULL; END IF;
IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='pay_record' AND COLUMN_NAME='attempt_state') THEN ALTER TABLE `pay_record` ADD COLUMN `attempt_state` varchar(24) NOT NULL DEFAULT 'NEW'; END IF;
IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='pay_record' AND COLUMN_NAME='attempt_owner') THEN ALTER TABLE `pay_record` ADD COLUMN `attempt_owner` varchar(64) DEFAULT NULL; END IF;
IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='pay_record' AND COLUMN_NAME='attempt_until') THEN ALTER TABLE `pay_record` ADD COLUMN `attempt_until` datetime DEFAULT NULL; END IF;
IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='ad_conversion' AND COLUMN_NAME='event_key') THEN ALTER TABLE `ad_conversion` ADD COLUMN `event_key` varchar(128) DEFAULT NULL; END IF;
IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='ad_conversion' AND COLUMN_NAME='source_type') THEN ALTER TABLE `ad_conversion` ADD COLUMN `source_type` varchar(24) DEFAULT NULL; END IF;
IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='ad_conversion' AND COLUMN_NAME='source_id') THEN ALTER TABLE `ad_conversion` ADD COLUMN `source_id` bigint DEFAULT NULL; END IF;
IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='ad_conversion' AND COLUMN_NAME='event_time') THEN ALTER TABLE `ad_conversion` ADD COLUMN `event_time` datetime DEFAULT NULL; END IF;
IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='ad_conversion' AND COLUMN_NAME='delivery_snapshot') THEN ALTER TABLE `ad_conversion` ADD COLUMN `delivery_snapshot` text; END IF;
IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='ad_conversion' AND COLUMN_NAME='next_run_at') THEN ALTER TABLE `ad_conversion` ADD COLUMN `next_run_at` datetime DEFAULT NULL; END IF;
IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='ad_conversion' AND COLUMN_NAME='lease_owner') THEN ALTER TABLE `ad_conversion` ADD COLUMN `lease_owner` varchar(64) DEFAULT NULL; END IF;
IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='ad_conversion' AND COLUMN_NAME='lease_until') THEN ALTER TABLE `ad_conversion` ADD COLUMN `lease_until` datetime DEFAULT NULL; END IF;
IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='ad_conversion' AND COLUMN_NAME='lease_version') THEN ALTER TABLE `ad_conversion` ADD COLUMN `lease_version` bigint NOT NULL DEFAULT 0; END IF;
IF EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='ad_conversion' AND COLUMN_NAME='order_no' AND IS_NULLABLE='NO') THEN ALTER TABLE ad_conversion MODIFY order_no varchar(32) DEFAULT NULL; END IF;
IF NOT EXISTS(SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='ad_conversion' AND INDEX_NAME='uk_conversion_event') THEN ALTER TABLE ad_conversion ADD UNIQUE KEY uk_conversion_event(event_key); END IF;
IF EXISTS(SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='wecom_contact_entry' AND INDEX_NAME='uk_link_id') THEN ALTER TABLE wecom_contact_entry DROP INDEX uk_link_id, ADD KEY idx_shared_link(link_id); END IF;
IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='landing_page' AND column_name='pay_merchant_id') THEN
ALTER TABLE landing_page ADD COLUMN pay_merchant_id BIGINT NULL COMMENT '页面指定支付商户，仍必须满足当前域名授权';
END IF;
IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='trade_order' AND column_name='ad_snapshot') THEN
ALTER TABLE trade_order ADD COLUMN ad_snapshot TEXT NULL COMMENT '下单时冻结广告账户接口和三类事件映射，不含凭据';
END IF;
CREATE TABLE IF NOT EXISTS acq_wecom_action (
 id bigint NOT NULL AUTO_INCREMENT COMMENT '主键', event_id bigint NOT NULL COMMENT '来源已验签事件',
 corp_row_id bigint NOT NULL COMMENT '企业配置ID', corp_id varchar(64) NOT NULL COMMENT '企业身份快照',
 external_userid varchar(64) NOT NULL COMMENT '本企业客户', teacher_userid varchar(64) NOT NULL COMMENT '跟进老师',
 action_type varchar(16) NOT NULL COMMENT 'REMARK或TAG', payload text NOT NULL COMMENT '无凭据规则快照',
 status tinyint NOT NULL DEFAULT 0 COMMENT '0待执行1成功2跳过4未知5阻断6执行中',
 attempt_count int NOT NULL DEFAULT 0 COMMENT '外呼尝试次数', lease_owner varchar(64) NULL COMMENT '租约持有者',
 lease_until datetime NULL COMMENT '租约截止', lease_version bigint NOT NULL DEFAULT 0 COMMENT '租约版本',
 last_error varchar(128) NULL COMMENT '脱敏结果分类', create_time datetime DEFAULT CURRENT_TIMESTAMP,
 update_time datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, is_delete tinyint NOT NULL DEFAULT 0,
 PRIMARY KEY(id), UNIQUE KEY uk_customer_action(corp_id,external_userid,teacher_userid,action_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企微备注及标签独立任务';
END$$
DELIMITER ;
CALL acq_business_upgrade();
DROP PROCEDURE acq_business_upgrade;

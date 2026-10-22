-- r0b1: review schema and back up before running. NOT automatically executed.
-- Run only on the explicitly authorized toufang database; DDL auto-commits.
-- Existing columns are preserved: compare their type/default manually before deployment.
DELIMITER $$
CREATE PROCEDURE acq_upgrade_r0b1()
BEGIN
  IF DATABASE() IS NULL OR DATABASE() <> 'toufang' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Select authorized toufang database first';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='landing_page' AND column_name='report_template_id') THEN
    ALTER TABLE `landing_page` ADD COLUMN `report_template_id` bigint DEFAULT NULL;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='report_template' AND column_name='current_version_id') THEN
    ALTER TABLE `report_template` ADD COLUMN `current_version_id` bigint DEFAULT NULL;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='report_template_version' AND column_name='content_type') THEN
    ALTER TABLE `report_template_version` ADD COLUMN `content_type` tinyint NOT NULL DEFAULT 1;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='report_template_version' AND column_name='content_text') THEN
    ALTER TABLE `report_template_version` ADD COLUMN `content_text` text;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='report_template_version' AND column_name='published_time') THEN
    ALTER TABLE `report_template_version` ADD COLUMN `published_time` datetime DEFAULT NULL;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='site_domain' AND column_name='review_status') THEN
    ALTER TABLE `site_domain` ADD COLUMN `review_status` tinyint NOT NULL DEFAULT 0;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='site_domain' AND column_name='icp_status') THEN
    ALTER TABLE `site_domain` ADD COLUMN `icp_status` tinyint NOT NULL DEFAULT 0;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='site_domain' AND column_name='https_status') THEN
    ALTER TABLE `site_domain` ADD COLUMN `https_status` tinyint NOT NULL DEFAULT 0;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='site_domain' AND column_name='verify_status') THEN
    ALTER TABLE `site_domain` ADD COLUMN `verify_status` tinyint NOT NULL DEFAULT 0;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='site_domain' AND column_name='remark') THEN
    ALTER TABLE `site_domain` ADD COLUMN `remark` varchar(255) DEFAULT NULL;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='domain_merchant' AND column_name='verify_status') THEN
    ALTER TABLE `domain_merchant` ADD COLUMN `verify_status` tinyint NOT NULL DEFAULT 0;
  END IF;
END$$
DELIMITER ;
CALL acq_upgrade_r0b1();
DROP PROCEDURE acq_upgrade_r0b1;

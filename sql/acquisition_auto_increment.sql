-- 投放业务表主键增量迁移。
-- 仅用于已核对结构并获准迁移的 106.55.61.223:3306/toufang。先备份和比对差异，不因本机无执行记录而整份重跑。
-- 本脚本不包含 USE、DROP DATABASE、TRUNCATE。
-- 已是 AUTO_INCREMENT 的表会跳过。已有主键值不修改。
-- 执行前请另行保留结构备份。DDL 不能当作普通业务事务回滚。

-- 若同名过程已存在，先核对归属与先前执行结果；禁止先删除它来强行重跑。

DELIMITER $$

CREATE PROCEDURE acq_r0b1_migrate()
BEGIN
    DECLARE done INT DEFAULT 0;
    DECLARE tbl VARCHAR(64);
    DECLARE extra_value VARCHAR(128);
    DECLARE cur CURSOR FOR
        SELECT table_name
          FROM information_schema.tables
         WHERE table_schema = DATABASE()
           AND table_name IN (
                'landing_page', 'landing_asset', 'report_template', 'report_template_version', 'trade_report',
                'site_domain', 'landing_domain', 'domain_merchant',
                'pay_merchant', 'pay_merchant_version', 'trade_order', 'pay_record', 'pay_refund',
                'wecom_corp', 'wecom_teacher', 'wecom_route', 'wecom_contact_entry', 'wecom_tag',
                'wecom_external_contact', 'wecom_customer_bind', 'wecom_contact_event',
                'ad_account', 'ad_click', 'ad_conversion',
                'h5_visit', 'h5_lead', 'post_pay_task'
           )
         ORDER BY table_name;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

    IF DATABASE() IS NULL OR DATABASE() <> 'toufang' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'refuse: current database must be toufang';
    END IF;

    IF (SELECT COUNT(*) FROM information_schema.tables
         WHERE table_schema = DATABASE()
           AND table_name IN (
                'landing_page', 'landing_asset', 'report_template', 'report_template_version', 'trade_report',
                'site_domain', 'landing_domain', 'domain_merchant',
                'pay_merchant', 'pay_merchant_version', 'trade_order', 'pay_record', 'pay_refund',
                'wecom_corp', 'wecom_teacher', 'wecom_route', 'wecom_contact_entry', 'wecom_tag',
                'wecom_external_contact', 'wecom_customer_bind', 'wecom_contact_event',
                'ad_account', 'ad_click', 'ad_conversion',
                'h5_visit', 'h5_lead', 'post_pay_task'
           )) <> 27 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'refuse: expected 27 acquisition tables';
    END IF;

    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO tbl;
        IF done = 1 THEN
            LEAVE read_loop;
        END IF;
        SELECT EXTRA INTO extra_value
          FROM information_schema.columns
         WHERE table_schema = DATABASE()
           AND table_name = tbl
           AND column_name = 'id';
        IF extra_value IS NULL OR extra_value NOT LIKE '%auto_increment%' THEN
            SET @ddl = CONCAT(
                'ALTER TABLE `', tbl,
                '` MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT ''主键ID'''
            );
            PREPARE stmt FROM @ddl;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
        END IF;
    END LOOP;
    CLOSE cur;

    IF (SELECT COUNT(*) FROM information_schema.columns
         WHERE table_schema = DATABASE() AND table_name = 'report_template' AND column_name = 'current_version_id') = 0 THEN
        ALTER TABLE report_template
            ADD COLUMN current_version_id bigint DEFAULT NULL COMMENT '当前已发布版本ID，空表示尚未发布' AFTER status;
    END IF;

    IF (SELECT COUNT(*) FROM information_schema.columns
         WHERE table_schema = DATABASE() AND table_name = 'report_template_version' AND column_name = 'content_type') = 0 THEN
        ALTER TABLE report_template_version
            ADD COLUMN content_type tinyint NOT NULL DEFAULT 1 COMMENT '正文类型：1=纯文本' AFTER change_note;
    END IF;
    IF (SELECT COUNT(*) FROM information_schema.columns
         WHERE table_schema = DATABASE() AND table_name = 'report_template_version' AND column_name = 'content_text') = 0 THEN
        ALTER TABLE report_template_version
            ADD COLUMN content_text text COMMENT '模板正文。开发配置内容，不是正式算法输出' AFTER content_type;
    END IF;
    IF (SELECT COUNT(*) FROM information_schema.columns
         WHERE table_schema = DATABASE() AND table_name = 'report_template_version' AND column_name = 'published_time') = 0 THEN
        ALTER TABLE report_template_version
            ADD COLUMN published_time datetime DEFAULT NULL COMMENT '发布时间' AFTER content_text;
    END IF;

    IF (SELECT COUNT(*) FROM information_schema.columns
         WHERE table_schema = DATABASE() AND table_name = 'site_domain' AND column_name = 'review_status') = 0 THEN
        ALTER TABLE site_domain
            ADD COLUMN review_status tinyint NOT NULL DEFAULT 0 COMMENT '人工审核登记：0=未登记 1=已登记通过 2=已登记未通过' AFTER status,
            ADD COLUMN icp_status tinyint NOT NULL DEFAULT 0 COMMENT '备案登记：0=未登记 1=已登记备案 2=已登记未备案' AFTER review_status,
            ADD COLUMN https_status tinyint NOT NULL DEFAULT 0 COMMENT 'HTTPS登记：0=未登记 1=已登记启用 2=已登记未启用' AFTER icp_status,
            ADD COLUMN verify_status tinyint NOT NULL DEFAULT 0 COMMENT '实际核验：0=未验证 1=已验证。人工登记不等于核验' AFTER https_status;
    END IF;

    IF (SELECT COUNT(*) FROM information_schema.columns
         WHERE table_schema = DATABASE() AND table_name = 'domain_merchant' AND column_name = 'verify_status') = 0 THEN
        ALTER TABLE domain_merchant
            ADD COLUMN verify_status tinyint NOT NULL DEFAULT 0 COMMENT '支付授权核验：0=仅人工登记 1=已通过平台核验' AFTER status;
    END IF;

    ALTER TABLE domain_merchant
        MODIFY COLUMN status tinyint NOT NULL DEFAULT 1 COMMENT '人工授权登记：0=停用 1=已登记。不等于平台核验通过';
END$$

DELIMITER ;

CALL acq_r0b1_migrate();

DROP PROCEDURE IF EXISTS acq_r0b1_migrate;

SELECT table_name, column_type, extra, column_comment
  FROM information_schema.columns
 WHERE table_schema = DATABASE()
   AND column_name = 'id'
 ORDER BY table_name;

SELECT table_name, column_name, column_type, column_comment
  FROM information_schema.columns
 WHERE table_schema = DATABASE()
   AND (
        (table_name = 'report_template' AND column_name = 'current_version_id')
        OR (table_name = 'report_template_version' AND column_name IN ('content_type', 'content_text', 'published_time'))
        OR (table_name = 'site_domain' AND column_name IN ('review_status', 'icp_status', 'https_status', 'verify_status'))
        OR (table_name = 'domain_merchant' AND column_name = 'verify_status')
   )
 ORDER BY table_name, ordinal_position;

SELECT table_name, auto_increment
  FROM information_schema.tables
 WHERE table_schema = DATABASE()
   AND table_name IN (
        'landing_page', 'landing_asset', 'report_template', 'report_template_version', 'trade_report',
        'site_domain', 'landing_domain', 'domain_merchant',
        'pay_merchant', 'pay_merchant_version', 'trade_order', 'pay_record', 'pay_refund',
        'wecom_corp', 'wecom_teacher', 'wecom_route', 'wecom_contact_entry', 'wecom_tag',
        'wecom_external_contact', 'wecom_customer_bind', 'wecom_contact_event',
        'ad_account', 'ad_click', 'ad_conversion',
        'h5_visit', 'h5_lead', 'post_pay_task'
   )
 ORDER BY table_name;

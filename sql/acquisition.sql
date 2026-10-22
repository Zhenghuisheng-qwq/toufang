-- 投放获客业务表（jsj-mini 规范）
-- 数据库自增主键、is_delete、InnoDB、utf8mb4。不使用外键，不使用 JSON。
-- 仅 CREATE TABLE IF NOT EXISTS。不包含 DROP、TRUNCATE、USE，也不写入密钥或业务种子。
-- 目标库由执行人显式选择。不要在未确认的生产库上执行。
--
-- 共 27 张表：
-- 落地页：landing_page、landing_asset、report_template、report_template_version、trade_report
-- 域名：site_domain、landing_domain、domain_merchant
-- 支付：pay_merchant、pay_merchant_version、trade_order、pay_record、pay_refund
-- 企微：wecom_corp、wecom_teacher、wecom_route、wecom_contact_entry、wecom_tag、
--       wecom_external_contact、wecom_customer_bind、wecom_contact_event
-- 广告：ad_account、ad_click、ad_conversion
-- 线索：h5_visit、h5_lead
-- 任务：post_pay_task
--
-- 关联（逻辑，无外键）：
-- landing_page 1—n site_domain，经由 landing_domain
-- site_domain n—n pay_merchant，经由 domain_merchant
-- trade_order 创建时复制域名、商户版本、企微企业、老师、广告点击
-- trade_report.order_no、wecom_contact_entry.order_no、ad_conversion.order_no、post_pay_task.order_no 指向订单
-- wecom_external_contact 以 corp_id + external_userid 隔离
-- wecom_contact_entry.entry_type：1=联系我二维码（state_token），2=获客助手链接（link_id/customer_channel）

-- ========================================
-- 落地页
-- ========================================
CREATE TABLE IF NOT EXISTS `landing_page` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` varchar(128) NOT NULL COMMENT '页面标题',
  `headline` varchar(255) DEFAULT NULL COMMENT '主宣传文案',
  `selling_text` varchar(500) DEFAULT NULL COMMENT '补充文案',
  `button_text` varchar(64) NOT NULL DEFAULT '立即测试' COMMENT '按钮文案',
  `price_fen` int NOT NULL DEFAULT 0 COMMENT '展示价格，单位分',
  `publish_status` tinyint NOT NULL DEFAULT 0 COMMENT '发布状态：0=草稿 1=已发布 2=已下线',
  `sort_no` int NOT NULL DEFAULT 0 COMMENT '展示顺序，小的在前',
  `report_template_id` bigint DEFAULT NULL COMMENT '报告模板ID，算法未定时可空',
  `name_enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否采集姓名：0=否 1=是',
  `gender_enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否采集性别：0=否 1=是',
  `birth_date_enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否采集出生日期：0=否 1=是',
  `birth_time_enabled` tinyint NOT NULL DEFAULT 0 COMMENT '是否采集出生时间：0=否 1=是',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志：0=未删除 1=已删除',
  PRIMARY KEY (`id`),
  KEY `idx_publish_sort` (`publish_status`, `sort_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='落地页配置';

CREATE TABLE IF NOT EXISTS `landing_asset` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `landing_page_id` bigint NOT NULL COMMENT '落地页ID',
  `asset_type` tinyint NOT NULL DEFAULT 1 COMMENT '素材类型：1=图片',
  `asset_url` varchar(512) NOT NULL COMMENT '素材地址',
  `sort_no` int NOT NULL DEFAULT 0 COMMENT '展示顺序，小的在前',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0=停用 1=启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志：0=未删除 1=已删除',
  PRIMARY KEY (`id`),
  KEY `idx_landing_sort` (`landing_page_id`, `sort_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='落地页素材';

CREATE TABLE IF NOT EXISTS `report_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `template_name` varchar(128) NOT NULL COMMENT '模板名称',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0=草稿 1=启用 2=停用',
  `current_version_id` bigint DEFAULT NULL COMMENT '当前已发布版本ID，空表示尚未发布',
  `remark` varchar(255) DEFAULT NULL COMMENT '说明，不包含未确认的计分规则',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志：0=未删除 1=已删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报告模板';

CREATE TABLE IF NOT EXISTS `report_template_version` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `template_id` bigint NOT NULL COMMENT '报告模板ID',
  `version_no` int NOT NULL COMMENT '版本号，从1递增',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0=草稿 1=已发布 2=停用',
  `change_note` varchar(255) DEFAULT NULL COMMENT '版本说明，不写入未确认的测试规则',
  `content_type` tinyint NOT NULL DEFAULT 1 COMMENT '正文类型：1=纯文本',
  `content_text` text COMMENT '模板正文。开发配置内容，不是正式算法输出',
  `published_time` datetime DEFAULT NULL COMMENT '发布时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志：0=未删除 1=已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_version` (`template_id`, `version_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报告模板版本';

CREATE TABLE IF NOT EXISTS `trade_report` (
  `content_text` mediumtext,
  `generation_mode` varchar(32) DEFAULT NULL,
  `generated_time` datetime DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_no` varchar(32) NOT NULL COMMENT '订单号',
  `template_id` bigint DEFAULT NULL COMMENT '生成时使用的模板ID',
  `template_version_id` bigint DEFAULT NULL COMMENT '生成时锁定的模板版本ID',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0=待生成 1=已生成 2=生成失败',
  `voucher_hash` char(64) DEFAULT NULL COMMENT '访问凭证哈希，不存明文凭证',
  `fail_reason` varchar(255) DEFAULT NULL COMMENT '最近一次失败说明',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志：0=未删除 1=已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_report_order_no` (`order_no`),
  UNIQUE KEY `uk_voucher_hash` (`voucher_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单报告';

-- ========================================
-- 域名与商户授权
-- ========================================
CREATE TABLE IF NOT EXISTS `site_domain` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `domain_name` varchar(255) NOT NULL COMMENT '域名，不含协议和路径',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0=停用 1=启用',
  `review_status` tinyint NOT NULL DEFAULT 0 COMMENT '人工审核登记：0=未登记 1=已登记通过 2=已登记未通过',
  `icp_status` tinyint NOT NULL DEFAULT 0 COMMENT '备案登记：0=未登记 1=已登记备案 2=已登记未备案',
  `https_status` tinyint NOT NULL DEFAULT 0 COMMENT 'HTTPS登记：0=未登记 1=已登记启用 2=已登记未启用',
  `verify_status` tinyint NOT NULL DEFAULT 0 COMMENT '实际核验：0=未验证 1=已验证。人工登记不等于核验',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志：0=未删除 1=已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_domain_name` (`domain_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='投放域名';

CREATE TABLE IF NOT EXISTS `landing_domain` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `landing_page_id` bigint NOT NULL COMMENT '落地页ID',
  `domain_id` bigint NOT NULL COMMENT '域名ID',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0=停用 1=启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志：0=未删除 1=已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_landing_domain` (`landing_page_id`, `domain_id`),
  KEY `idx_domain_id` (`domain_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='落地页与域名关联';

CREATE TABLE IF NOT EXISTS `domain_merchant` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `domain_id` bigint NOT NULL COMMENT '域名ID',
  `pay_merchant_id` bigint NOT NULL COMMENT '支付商户ID',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '人工授权登记：0=停用 1=已登记。不等于平台核验通过',
  `verify_status` tinyint NOT NULL DEFAULT 0 COMMENT '支付授权核验：0=仅人工登记 1=已通过平台核验',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志：0=未删除 1=已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_domain_merchant` (`domain_id`, `pay_merchant_id`),
  KEY `idx_pay_merchant_id` (`pay_merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='域名与支付商户授权';

-- ========================================
-- 微信支付。密钥列只保存引用，禁止明文。
-- ========================================
CREATE TABLE IF NOT EXISTS `pay_merchant` (
  `remark` varchar(255) DEFAULT NULL,
  `current_version_id` bigint DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `mch_id` varchar(32) NOT NULL COMMENT '微信支付商户号',
  `app_id` varchar(32) NOT NULL COMMENT '关联AppID',
  `mch_name` varchar(64) NOT NULL COMMENT '商户名称',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0=停用 1=启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志：0=未删除 1=已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mch_app` (`mch_id`, `app_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='微信支付商户';

CREATE TABLE IF NOT EXISTS `pay_merchant_version` (
  `merchant_serial_no` varchar(128) DEFAULT NULL,
  `verify_mode` varchar(32) NOT NULL DEFAULT 'UNCONFIRMED',
  `platform_key_id` varchar(128) DEFAULT NULL,
  `platform_public_key_ref` varchar(128) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `pay_merchant_id` bigint NOT NULL COMMENT '支付商户ID',
  `version_no` int NOT NULL COMMENT '配置版本号，从1递增',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0=草稿 1=启用 2=停用',
  `api_v3_key_ref` varchar(128) DEFAULT NULL COMMENT 'APIv3密钥引用，格式ref:，不存明文',
  `private_key_ref` varchar(128) DEFAULT NULL COMMENT '商户私钥引用，格式ref:，不存明文',
  `platform_cert_ref` varchar(128) DEFAULT NULL COMMENT '平台证书引用，格式ref:；历史公钥引用须人工确认后迁移，不自动判定模式',
  `remark` varchar(255) DEFAULT NULL COMMENT '版本说明',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志：0=未删除 1=已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_merchant_version` (`pay_merchant_id`, `version_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='微信支付配置版本';

CREATE TABLE IF NOT EXISTS `trade_order` (
  `report_template_id` bigint DEFAULT NULL,
  `report_template_version_id` bigint DEFAULT NULL,
  `report_input_version` int DEFAULT NULL,
  `report_name` varchar(64) DEFAULT NULL,
  `report_gender` tinyint DEFAULT NULL,
  `report_birth_date` date DEFAULT NULL,
  `report_birth_time` time DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_no` varchar(32) NOT NULL COMMENT '订单号',
  `lead_id` bigint DEFAULT NULL COMMENT '线索ID',
  `visit_id` bigint DEFAULT NULL COMMENT '访问记录ID',
  `landing_page_id` bigint DEFAULT NULL COMMENT '下单时落地页ID',
  `ad_click_id` bigint DEFAULT NULL COMMENT '下单时锁定的广告点击ID',
  `domain_id` bigint DEFAULT NULL COMMENT '下单时访问域名ID',
  `domain_name` varchar(255) DEFAULT NULL COMMENT '下单时访问域名快照',
  `amount_fen` int NOT NULL COMMENT '订单金额，单位分',
  `trade_type` tinyint NOT NULL COMMENT '支付类型：1=H5 2=JSAPI',
  `pay_status` tinyint NOT NULL DEFAULT 0 COMMENT '支付状态：0=待支付 1=已支付 2=已退款 3=已关闭',
  `pay_merchant_id` bigint NOT NULL COMMENT '锁定的商户ID',
  `merchant_version_id` bigint NOT NULL COMMENT '锁定的商户配置版本ID',
  `merchant_version_no` int NOT NULL COMMENT '锁定的商户配置版本号',
  `mch_id` varchar(32) NOT NULL COMMENT '锁定的商户号快照',
  `app_id` varchar(32) NOT NULL COMMENT '锁定的AppID快照',
  `route_id` bigint DEFAULT NULL COMMENT '下单时命中的企微路由ID',
  `corp_row_id` bigint DEFAULT NULL COMMENT '下单时锁定的企业行ID',
  `teacher_row_id` bigint DEFAULT NULL COMMENT '下单时锁定的老师行ID',
  `corp_id` varchar(64) DEFAULT NULL COMMENT '下单时锁定的企业CorpID快照',
  `teacher_userid` varchar(64) DEFAULT NULL COMMENT '下单时锁定的老师UserID快照',
  `paid_time` datetime DEFAULT NULL COMMENT '支付成功时间',
  `close_time` datetime DEFAULT NULL COMMENT '关闭时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志：0=未删除 1=已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_lead_id` (`lead_id`),
  KEY `idx_ad_click_id` (`ad_click_id`),
  KEY `idx_pay_status_create_time` (`pay_status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='投放订单。快照列创建后不可因配置切换而改写';

CREATE TABLE IF NOT EXISTS `pay_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_no` varchar(32) NOT NULL COMMENT '订单号',
  `out_trade_no` varchar(32) NOT NULL COMMENT '商户订单号，首期与订单号相同',
  `transaction_id` varchar(64) DEFAULT NULL COMMENT '微信支付订单号',
  `success_order_no` varchar(32) DEFAULT NULL COMMENT '支付成功时写入订单号；为空表示尚未成功。用于保证一笔订单一条成功支付',
  `notify_id` varchar(64) DEFAULT NULL COMMENT '微信通知ID，重复通知靠唯一约束拦截',
  `mch_id` varchar(32) NOT NULL COMMENT '商户号，须与订单快照一致',
  `app_id` varchar(32) NOT NULL COMMENT 'AppID，须与订单快照一致',
  `amount_fen` int NOT NULL COMMENT '支付金额，单位分',
  `trade_type` tinyint NOT NULL COMMENT '支付类型：1=H5 2=JSAPI',
  `pay_status` tinyint NOT NULL DEFAULT 0 COMMENT '支付状态：0=待支付 1=支付成功 2=已关闭 3=转入退款',
  `wx_trade_state` varchar(32) DEFAULT NULL COMMENT '微信原始交易状态',
  `prepay_id` varchar(64) DEFAULT NULL COMMENT '预支付标识',
  `payer_openid` varchar(64) DEFAULT NULL COMMENT 'JSAPI付款人openid',
  `success_time` datetime DEFAULT NULL COMMENT '支付成功时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志：0=未删除 1=已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pay_order_no` (`order_no`),
  UNIQUE KEY `uk_out_trade_no` (`out_trade_no`),
  UNIQUE KEY `uk_transaction_id` (`transaction_id`),
  UNIQUE KEY `uk_success_order_no` (`success_order_no`),
  UNIQUE KEY `uk_notify_id` (`notify_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='微信支付记录';

CREATE TABLE IF NOT EXISTS `pay_refund` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `refund_no` varchar(32) NOT NULL COMMENT '商户退款单号',
  `order_no` varchar(32) NOT NULL COMMENT '原订单号',
  `pay_record_id` bigint NOT NULL COMMENT '原支付记录ID',
  `refund_fen` int NOT NULL COMMENT '退款金额，单位分',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0=处理中 1=成功 2=关闭 3=异常',
  `wx_refund_id` varchar(64) DEFAULT NULL COMMENT '微信退款单号',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志：0=未删除 1=已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_refund_no` (`refund_no`),
  UNIQUE KEY `uk_wx_refund_id` (`wx_refund_id`),
  KEY `idx_refund_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付退款记录';

-- ========================================
-- 企业微信。Token、Secret、EncodingAESKey 只存引用。
-- ========================================
CREATE TABLE IF NOT EXISTS `wecom_corp` (
  `sid` varchar(64) DEFAULT NULL,
  `auth_mode` varchar(32) NOT NULL DEFAULT 'UNCONFIRMED',
  `remark` varchar(255) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `corp_id` varchar(64) NOT NULL COMMENT '企业微信CorpID',
  `corp_name` varchar(128) NOT NULL COMMENT '企业名称',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0=停用 1=启用',
  `token_ref` varchar(128) DEFAULT NULL COMMENT '回调Token引用，格式ref:',
  `encoding_aes_key_ref` varchar(128) DEFAULT NULL COMMENT 'EncodingAESKey引用，格式ref:',
  `contact_secret_ref` varchar(128) DEFAULT NULL COMMENT '客户联系Secret引用，格式ref:',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志：0=未删除 1=已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_corp_id` (`corp_id`),
  UNIQUE KEY `uk_wecom_sid` (`sid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业微信主体';

CREATE TABLE IF NOT EXISTS `wecom_teacher` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `corp_row_id` bigint NOT NULL COMMENT '企业行ID',
  `corp_id` varchar(64) NOT NULL COMMENT '企业CorpID',
  `teacher_userid` varchar(64) NOT NULL COMMENT '老师UserID',
  `teacher_name` varchar(64) NOT NULL COMMENT '老师名称',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0=停用 1=启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志：0=未删除 1=已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_corp_teacher` (`corp_id`, `teacher_userid`),
  KEY `idx_corp_row_id` (`corp_row_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业微信老师';

CREATE TABLE IF NOT EXISTS `wecom_route` (
  `entry_type` tinyint NOT NULL DEFAULT 1,
  `link_id` varchar(128) DEFAULT NULL,
  `link_url` varchar(512) DEFAULT NULL,
  `capability_note` varchar(255) DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `scope_type` tinyint NOT NULL COMMENT '范围：1=全局当前 2=落地页指定',
  `landing_page_id` bigint DEFAULT NULL COMMENT '落地页ID，全局路由为空',
  `corp_row_id` bigint NOT NULL COMMENT '企业行ID',
  `teacher_row_id` bigint NOT NULL COMMENT '老师行ID',
  `corp_id` varchar(64) NOT NULL COMMENT '企业CorpID',
  `teacher_userid` varchar(64) NOT NULL COMMENT '老师UserID',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0=停用 1=启用',
  `active_key` varchar(40) DEFAULT NULL COMMENT '当前启用占位：G=全局，L:{落地页ID}=落地页。停用必须为NULL',
  `sort_no` int NOT NULL DEFAULT 0 COMMENT '同范围排序',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志：0=未删除 1=已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_active_key` (`active_key`),
  KEY `idx_landing_status` (`landing_page_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企微承接路由';

CREATE TABLE IF NOT EXISTS `wecom_contact_entry` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `entry_type` tinyint NOT NULL COMMENT '入口类型：1=联系我二维码 2=获客助手链接',
  `order_no` varchar(32) NOT NULL COMMENT '订单号，一订单一入口',
  `corp_row_id` bigint NOT NULL COMMENT '企业行ID',
  `corp_id` varchar(64) NOT NULL COMMENT '企业CorpID快照',
  `teacher_userid` varchar(64) NOT NULL COMMENT '老师UserID快照',
  `state_token` varchar(30) DEFAULT NULL COMMENT '联系我state，仅类型1使用，最长30',
  `config_id` varchar(64) DEFAULT NULL COMMENT '联系我配置ID，仅类型1',
  `qr_code_url` varchar(512) DEFAULT NULL COMMENT '联系我二维码地址，仅类型1',
  `link_id` varchar(64) DEFAULT NULL COMMENT '获客助手链接ID，仅类型2',
  `link_url` varchar(512) DEFAULT NULL COMMENT '获客助手链接地址，仅类型2',
  `customer_channel` varchar(64) DEFAULT NULL COMMENT '获客助手自定义渠道参数，仅类型2',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0=待创建 1=已创建 2=创建失败 3=已失效',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志：0=未删除 1=已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_entry_order_no` (`order_no`),
  UNIQUE KEY `uk_state_token` (`state_token`),
  UNIQUE KEY `uk_link_id` (`link_id`),
  KEY `idx_entry_corp` (`corp_id`, `entry_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企微获客入口。联系我与获客助手字段互斥';

CREATE TABLE IF NOT EXISTS `wecom_tag` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `corp_id` varchar(64) NOT NULL COMMENT '企业CorpID',
  `tag_id` varchar(64) NOT NULL COMMENT '企业标签ID',
  `tag_name` varchar(64) NOT NULL COMMENT '标签名称',
  `group_id` varchar(64) DEFAULT NULL COMMENT '标签组ID',
  `group_name` varchar(64) DEFAULT NULL COMMENT '标签组名称',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0=停用 1=启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志：0=未删除 1=已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_corp_tag` (`corp_id`, `tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业微信标签';

CREATE TABLE IF NOT EXISTS `wecom_external_contact` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `corp_id` varchar(64) NOT NULL COMMENT '企业CorpID',
  `external_userid` varchar(64) NOT NULL COMMENT '外部联系人ID，只在本企业内有效',
  `union_id` varchar(64) DEFAULT NULL COMMENT '微信UnionID，老客户匹配时使用',
  `contact_name` varchar(64) DEFAULT NULL COMMENT '外部联系人名称',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0=已删除 1=跟进中',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志：0=未删除 1=已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_corp_external` (`corp_id`, `external_userid`),
  KEY `idx_union_id` (`union_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企微外部联系人，按企业隔离';

CREATE TABLE IF NOT EXISTS `wecom_customer_bind` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_no` varchar(32) NOT NULL COMMENT '订单号',
  `corp_id` varchar(64) NOT NULL COMMENT '企业CorpID',
  `external_userid` varchar(64) NOT NULL COMMENT '外部联系人ID',
  `teacher_userid` varchar(64) NOT NULL COMMENT '老师UserID',
  `match_type` tinyint NOT NULL COMMENT '关联方式：1=联系我state 2=UnionID',
  `state_token` varchar(30) DEFAULT NULL COMMENT '命中的联系我state',
  `contact_entry_id` bigint DEFAULT NULL COMMENT '获客入口ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志：0=未删除 1=已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bind_order_no` (`order_no`),
  KEY `idx_corp_external` (`corp_id`, `external_userid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企微客户与订单关联';

CREATE TABLE IF NOT EXISTS `wecom_contact_event` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `dedup_key` varchar(128) NOT NULL COMMENT '回调幂等键',
  `corp_id` varchar(64) NOT NULL COMMENT '企业CorpID',
  `change_type` tinyint NOT NULL COMMENT '变更类型：1=添加好友 2=删除好友',
  `external_userid` varchar(64) NOT NULL COMMENT '外部联系人ID',
  `teacher_userid` varchar(64) DEFAULT NULL COMMENT '老师UserID',
  `state_token` varchar(30) DEFAULT NULL COMMENT '回调中的state',
  `order_no` varchar(32) DEFAULT NULL COMMENT '匹配到的订单号',
  `event_time` datetime DEFAULT NULL COMMENT '回调事件时间',
  `remark_status` tinyint NOT NULL DEFAULT 0 COMMENT '备注执行：0=待处理 1=成功 2=跳过 3=失败',
  `tag_status` tinyint NOT NULL DEFAULT 0 COMMENT '打标签执行：0=待处理 1=成功 2=跳过 3=失败',
  `last_error` varchar(255) DEFAULT NULL COMMENT '最近一次失败说明',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志：0=未删除 1=已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dedup_key` (`dedup_key`),
  KEY `idx_event_order_no` (`order_no`),
  KEY `idx_event_corp_external` (`corp_id`, `external_userid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企微添加与删除好友事件';

-- ========================================
-- 广告归因。三平台点击列分开，未确认参数放在原始查询串。
-- ========================================
CREATE TABLE IF NOT EXISTS `ad_account` (
  `interface_mode` varchar(32) NOT NULL DEFAULT 'UNCONFIRMED',
  `data_source_id` varchar(128) DEFAULT NULL,
  `pay_event` varchar(128) DEFAULT NULL,
  `amount_unit` varchar(16) NOT NULL DEFAULT 'UNCONFIRMED',
  `remark` varchar(255) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `platform` tinyint NOT NULL COMMENT '平台：1=TENCENT 2=BAIDU 3=KUAISHOU',
  `external_account_id` varchar(64) NOT NULL COMMENT '平台广告账户ID',
  `account_name` varchar(128) NOT NULL COMMENT '账户名称',
  `credential_ref` varchar(128) DEFAULT NULL COMMENT 'API授权引用，格式ref:，不存明文令牌',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0=停用 1=启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志：0=未删除 1=已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_platform_account` (`platform`, `external_account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='广告账户';

CREATE TABLE IF NOT EXISTS `ad_click` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `platform` tinyint NOT NULL COMMENT '平台：1=TENCENT 2=BAIDU 3=KUAISHOU',
  `ad_account_id` bigint DEFAULT NULL COMMENT '广告账户ID',
  `landing_page_id` bigint DEFAULT NULL COMMENT '落地页ID',
  `domain_id` bigint DEFAULT NULL COMMENT '访问域名ID',
  `domain_name` varchar(255) DEFAULT NULL COMMENT '访问域名',
  `visit_id` bigint DEFAULT NULL COMMENT '访问记录ID',
  `lead_id` bigint DEFAULT NULL COMMENT '线索ID',
  `order_no` varchar(32) DEFAULT NULL COMMENT '随后绑定的订单号',
  `tencent_click_id` varchar(128) DEFAULT NULL COMMENT '腾讯点击标识',
  `tencent_impression_id` varchar(128) DEFAULT NULL COMMENT '腾讯曝光标识',
  `tencent_request_id` varchar(128) DEFAULT NULL COMMENT '腾讯请求标识',
  `baidu_bd_vid` varchar(128) DEFAULT NULL COMMENT '百度bd_vid',
  `baidu_logid_url` varchar(512) DEFAULT NULL COMMENT '百度logidUrl',
  `kuaishou_callback` varchar(512) DEFAULT NULL COMMENT '快手callback',
  `raw_query` varchar(1024) DEFAULT NULL COMMENT '原始查询串，容纳尚未确认的点击参数',
  `click_dedup_key` varchar(160) DEFAULT NULL COMMENT '平台加主点击标识。无稳定标识时为空',
  `click_time` datetime DEFAULT NULL COMMENT '点击进入时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志：0=未删除 1=已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_click_dedup` (`click_dedup_key`),
  KEY `idx_click_order_no` (`order_no`),
  KEY `idx_click_lead_id` (`lead_id`),
  KEY `idx_tencent_click` (`tencent_click_id`),
  KEY `idx_baidu_bd_vid` (`baidu_bd_vid`),
  KEY `idx_kuaishou_callback` (`kuaishou_callback`(64))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='广告点击记录';

CREATE TABLE IF NOT EXISTS `ad_conversion` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_no` varchar(32) NOT NULL COMMENT '订单号',
  `platform` tinyint NOT NULL COMMENT '平台：1=TENCENT 2=BAIDU 3=KUAISHOU',
  `event_type` tinyint NOT NULL COMMENT '转化类型：1=支付',
  `ad_account_id` bigint DEFAULT NULL COMMENT '广告账户ID',
  `ad_click_id` bigint DEFAULT NULL COMMENT '广告点击ID',
  `amount_fen` int DEFAULT NULL COMMENT '转化金额，单位分',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '回传状态：0=待回传 1=已接收 2=跳过 3=失败 4=结果未知',
  `retry_count` int NOT NULL DEFAULT 0 COMMENT '已尝试次数',
  `request_time` datetime DEFAULT NULL COMMENT '最近一次请求时间',
  `response_code` varchar(32) DEFAULT NULL COMMENT '平台响应码',
  `response_body` varchar(500) DEFAULT NULL COMMENT '平台响应摘要',
  `last_error` varchar(255) DEFAULT NULL COMMENT '最近一次失败说明',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志：0=未删除 1=已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_platform_event` (`order_no`, `platform`, `event_type`),
  KEY `idx_conversion_click` (`ad_click_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='广告转化回传';

-- ========================================
-- H5 访问与线索
-- ========================================
CREATE TABLE IF NOT EXISTS `h5_visit` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `visit_no` varchar(32) NOT NULL COMMENT '访问号',
  `landing_page_id` bigint DEFAULT NULL COMMENT '落地页ID',
  `domain_id` bigint DEFAULT NULL COMMENT '域名ID',
  `domain_name` varchar(255) DEFAULT NULL COMMENT '原始访问域名',
  `platform` tinyint NOT NULL DEFAULT 0 COMMENT '来源平台：0=未知 1=TENCENT 2=BAIDU 3=KUAISHOU',
  `ad_click_id` bigint DEFAULT NULL COMMENT '广告点击ID',
  `visit_time` datetime NOT NULL COMMENT '访问时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志：0=未删除 1=已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_visit_no` (`visit_no`),
  KEY `idx_visit_click` (`ad_click_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='H5访问记录';

CREATE TABLE IF NOT EXISTS `h5_lead` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `visit_id` bigint DEFAULT NULL COMMENT '访问记录ID',
  `landing_page_id` bigint DEFAULT NULL COMMENT '落地页ID',
  `domain_id` bigint DEFAULT NULL COMMENT '域名ID',
  `domain_name` varchar(255) DEFAULT NULL COMMENT '提交时的原始访问域名',
  `platform` tinyint NOT NULL DEFAULT 0 COMMENT '来源平台：0=未知 1=TENCENT 2=BAIDU 3=KUAISHOU',
  `ad_click_id` bigint DEFAULT NULL COMMENT '广告点击ID',
  `real_name` varchar(32) DEFAULT NULL COMMENT '姓名',
  `gender` tinyint DEFAULT NULL COMMENT '性别：0=未知 1=男 2=女',
  `birth_date` date DEFAULT NULL COMMENT '出生日期',
  `birth_time` time DEFAULT NULL COMMENT '出生时间，未采集时为空',
  `submit_time` datetime NOT NULL COMMENT '用户提交时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志：0=未删除 1=已删除',
  PRIMARY KEY (`id`),
  KEY `idx_lead_visit` (`visit_id`),
  KEY `idx_lead_click` (`ad_click_id`),
  KEY `idx_lead_submit_time` (`submit_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='H5用户线索';

-- ========================================
-- 支付后任务。本脚本不包含调度器。
-- ========================================
CREATE TABLE IF NOT EXISTS `post_pay_task` (
  `lease_version` bigint NOT NULL DEFAULT 0,
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_no` varchar(32) NOT NULL COMMENT '订单号',
  `task_type` tinyint NOT NULL COMMENT '任务类型：1=REPORT_GENERATE 2=WECOM_CONTACT_CREATE 3=AD_CONVERSION_REPORT',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '任务状态：0=待执行 1=执行中 2=成功 3=失败待重试 4=结果未知',
  `attempt_count` int NOT NULL DEFAULT 0 COMMENT '已执行次数',
  `next_run_at` datetime NOT NULL COMMENT '下次执行时间',
  `lease_owner` varchar(64) DEFAULT NULL COMMENT '租约持有者',
  `lease_until` datetime DEFAULT NULL COMMENT '租约截止时间',
  `last_error` varchar(255) DEFAULT NULL COMMENT '最后错误',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志：0=未删除 1=已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_task` (`order_no`, `task_type`),
  KEY `idx_status_next_run` (`status`, `next_run_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付后异步任务';

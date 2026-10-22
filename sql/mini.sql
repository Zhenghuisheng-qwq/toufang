-- ========================================
-- 用户表
-- ========================================
CREATE TABLE IF NOT EXISTS `user` (
  `id` bigint NOT NULL COMMENT '用户id（雪花ID）',
  `nickname` varchar(64) DEFAULT NULL COMMENT '昵称',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像URL',
  `bio` varchar(255) DEFAULT NULL COMMENT '个人简介',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0=禁用 1=正常',
  `source_type` tinyint DEFAULT NULL COMMENT '来源：1=后台创建 2=手机号注册 3=微信小程序 4=微信公众号 5=邮箱注册',
  `register_time` datetime DEFAULT NULL COMMENT '注册时间',
  `last_active_time` datetime DEFAULT NULL COMMENT '最后活跃时间',
  `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志：0=未删除 1=已删除',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_status_is_delete` (`status`, `is_delete`),
  KEY `idx_last_active_time` (`last_active_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ========================================
-- 用户敏感信息表
-- ========================================
CREATE TABLE IF NOT EXISTS `user_sensitive_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID（关联 user.id）',
  `open_id` varchar(128) DEFAULT NULL COMMENT '微信openId',
  `union_id` varchar(128) DEFAULT NULL COMMENT '微信unionId',
  `mini_app_id` varchar(64) DEFAULT NULL COMMENT '小程序appId',
  `official_app_id` varchar(64) DEFAULT NULL COMMENT '公众号appId',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号（uni-app / H5 登录使用）',
  `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志：0=未删除 1=已删除',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  KEY `idx_open_id` (`open_id`),
  KEY `idx_union_id` (`union_id`),
  KEY `idx_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户敏感信息表';

-- ========================================
-- 用户登录会话表（审计/踢线/多端登录记录）
-- ========================================
CREATE TABLE IF NOT EXISTS `user_login_session` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `token` varchar(128) NOT NULL COMMENT '登录 token',
  `login_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
  `expire_time` datetime DEFAULT NULL COMMENT '过期时间',
  `device_type` varchar(32) DEFAULT NULL COMMENT '设备类型：mini/uni/h5/admin',
  `client_ip` varchar(64) DEFAULT NULL COMMENT '客户端IP',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0=失效 1=有效',
  `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '删除标志：0=未删除 1=已删除',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_token` (`token`),
  KEY `idx_user_status` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户登录会话表';


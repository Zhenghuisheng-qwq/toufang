# AGENTS.md

## 当前模块归属（用户确认，覆盖旧指令）

- jsj-mini 是 H5 后端，保留已有移动端/IM 能力。
- jsj-mini-app 是唯一 H5 前端，使用 Vue 3 + TypeScript + Vite；允许修改，不再按不可修改原生小程序保护。
- jsj-h5、jsj-uni-app 和根目录 generator 已由用户删除，保持删除，不创建平行 H5 工程。
- 后台 jsj-admin / jsj-admin-ui、共享领域 jsj-service、公用 jsj-common 保留现有实现。后台内置代码生成保留。
- 不依赖或读取工作区外 fmqx/zaq，不恢复旧架构。历史验收报告保留事实，不能改路径后冒充新工程验收。

## 技能

项目技能位于 .agents/skills/*/SKILL.md。前端实现、响应式、组件和视觉验收使用 senior-frontend；数据库和后端工作按相应项目技能执行。项目技能优先于全局同名技能，用户当前授权优先。

## 数据流与边界

jsj-mini-app → 同源 /api/h5 → jsj-mini（默认8881）→ jsj-service 投放领域。
jsj-admin-ui → /dev-api → jsj-admin（默认8080）→ 系统权限与投放配置/查询。
Admin 系统层使用 MyBatis XML；投放共享层使用 MyBatis-Plus。投放多步写用 acqTransactionManager，不能混用系统事务。

广告进入 H5 → 动态页面 → 访问/线索 → 服务端订单快照 → 微信支付证据确认 → 持久化报告/企微/广告任务。报告不依赖添加老师。保留多域名、多商户、多企业老师、版本和历史快照保护。

投放主键 BIGINT AUTO_INCREMENT / IdType.AUTO，前端 ID 使用字符串。凭据保存 ref: 引用，真实秘密由已忽略的本地 YAML 或环境变量注入，不输出秘密原文。

## 环境与命令

JDK21；Node18+；无根 Maven 聚合 POM。依次 install jsj-common、jsj-service，再构建 Admin/Mini，避免旧缓存掩盖缺失源码。README 提供后端离线测试白名单与构建命令。

H5 在 jsj-mini-app：npm ci --ignore-scripts、npm run dev（5174）、npm run typecheck、npm run build（dist）、npm run preview（4174）。开发代理配置 H5_API_TARGET 默认 http://127.0.0.1:8881；H5_ALLOWED_HOSTS 显式列出已授权开发域名。生产同源 HTTPS，禁止绕过域名/订单所有权校验。

后台在 jsj-admin-ui：npm run dev、npx --no-install vue-tsc --noEmit、npm run build:prod。保持原权限、菜单与 API 契约。

投放业务库唯一 106.55.61.223:3306/toufang；系统库独立配置，不假定旧电脑目录或 localhost 数据库存在。应用从根目录或模块目录导入 .local/application-business.yml，Admin 另导入 application-business-admin.yml。

新增字段同步初始化 SQL 和独立增量脚本；执行前确认目标库、结构和备份。禁止因前端目录纠正重装数据库、清库、重建表或重置自增。

## 验证与运行安全

- AcquisitionConstraintJdbcTest 会写库，未经单独审查不能运行。离线类名单见 README。
- ConfigurationPermissionTest 仅启动最小方法安全上下文。未修改后台时不重复整轮 CRUD 验收。
- 支付/OAuth/退款、企微外发及各 Worker 默认关闭；Worker 仅注册在 Mini 且要求 since。持有凭据不等于真实第三方调用授权。
- 不为编译启动应用或调度器。启动验收实例前核对插件、数据源、Quartz/Netty/Worker，使用独立端口，结束只关闭自有进程。
- 前端必须实际执行依赖安装、类型检查、生产构建和必要浏览器/API 联通；区分模拟响应、真实数据库、真实平台验证，不能复用历史数字充当本轮结果。
- 保留用户未提交改动，不自动提交、推送、部署或重写锁文件升级依赖。

当前导航 doc/project-index.md；本次目录纠正记录 docs/codex-h5-module-correction.md；历史业务链路验收 docs/codex-business-flow-review.md。

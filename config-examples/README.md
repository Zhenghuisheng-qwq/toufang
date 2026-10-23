# 本地配置和启动

2026-10-23：原“数据库未连接、未执行 SQL、H5 待实现”描述已失效。已通过 JDBC 验证指定 toufang 库与同服务器 Redis，详见 [本轮报告](../docs/codex-business-flow-review.md)。未修改服务器密码、授权或防火墙。

本机可用配置在已忽略的 `.local/application-business.yml` 与 `.local/application-business-admin.yml`。当前可直接使用，不要求再次提供密码。应用从项目根或模块目录启动均导入这些文件。新机器复制同名 .example 的结构后在本地填值或注入环境变量，不提交真实凭据。

| 变量 | 用途 |
| --- | --- |
| BOOT_DB_URL / BOOT_DB_USERNAME / BOOT_DB_PASSWORD | Mini 默认数据源，指定 toufang |
| ADMIN_DB_URL / ADMIN_DB_USERNAME / ADMIN_DB_PASSWORD | Admin 系统登录、角色和菜单数据源 |
| ACQ_DB_URL / ACQ_DB_USERNAME / ACQ_DB_PASSWORD | Admin 投放事务数据源，指定 toufang |
| REDIS_HOST / REDIS_PORT / REDIS_PASSWORD / REDIS_DATABASE | 实际 Redis；本机授权服务器当前无密码 |
| JWT_SECRET | Admin HS512 随机签名密钥，至少 64 字节 |
| UPLOAD_DIR | Admin 可写上传目录；本机使用 .local/uploads 的绝对路径 |
| WX_APP_ID / WX_APP_SECRET | 原小程序授权，不替代公众号或支付商户授权 |

当前系统表实际位于 toufang，配置仍支持系统/投放库分开；其他环境不能推定同库。网络不可达时核对来源白名单，不自行修改远程授权。

数据源初始化 SQL 将 MySQL session time_zone 设为 +08:00，与应用时间一致。仅 JDBC serverTimezone 不会改变 MySQL NOW() 时区。本轮没有改写历史记录时间，旧任务须审核后按 since 划定范围。

## 平台配置

后台仅保存 ref:alias；PlatformSecrets 只解析 acq.secrets.alias，不读取任意路径或其他属性。私钥、平台公钥/证书、APIv3 密钥、企微 Token/AES/客户联系 Secret、广告 Token 从本地密钥源注入。

公众号另需 acq.oauth.{appId}.secret-ref，appId 与订单支付快照一致。备注/标签规则为 acq.wecom.automation.{corpId}，仅支持明确确认的 EMPTY_ONLY 备注与同企业标签追加。共享获客链接不推断订单。

开关见 [业务配置示例](application-business.yml.example)。每个 Worker 需 enabled=true 和审查后的 ISO 本地时间 since；支付、退款及外发另有独立开关。退款只支持显式 FULL_REVOKE 策略下的全额退款，未确认时关闭。

Admin 示例使用内存 Quartz 且关闭自动启动，启动前仍需核对 SysJob 初始化逻辑。不要在未经审查的数据库上使用 JDBC Quartz 初始化。

## 同源部署与 SQL

生产 HTTPS 静态资源与 /api 同域。参考 [Nginx 示例](h5-nginx.conf.example) 设置可信转发头，并在 Mini 显式开启 forwarded-headers 处理；不能将信任转发头的后端直接暴露公网。各域名必须分别完成 H5 支付、公众号网页授权和平台回调的授权配置。

H5 唯一工程为 jsj-mini-app，进入该目录使用 npm ci --ignore-scripts、npm run dev、npm run typecheck、npm run build。产物为 jsj-mini-app/dist。

H5 开发端口 5174，默认代理 Mini 8881。业务 Host 仍需已授权域名配置；后台 /dev-api 代理 Admin 8080。生产不使用测试端口 63400–63404。

历史业务轮次已执行 acquisition_business_flow.sql、acq_menu_business_flow.sql；本次前端归属纠正未执行 SQL。acquisition.sql 已同步字段，但没有在已有库执行；新环境先核对初始化及增量依赖。菜单不分配真实角色。

DDL 自动提交，执行前备份、比对字段类型和唯一索引。IF NOT EXISTS 不能证明其他环境已有结构正确。不得盲目执行旧脚本固定 ry-vue 的语句。

构建命令及离线白名单见 README。AcquisitionConstraintJdbcTest 不在白名单。真实库验收仅用独立配置和精确 ID 清理，未增加生产模拟支付接口。

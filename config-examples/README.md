# 本机配置与构建

示例不自动加载，不含账号、密码或私钥。不要在聊天中粘贴秘密。

| 变量 | 用途 |
| --- | --- |
| ADMIN_DB_URL / ADMIN_DB_USERNAME / ADMIN_DB_PASSWORD | Admin 系统登录、角色、菜单数据源。实际实例需本机确认；业务 SQL 不在此执行 |
| ACQ_DB_URL / ACQ_DB_USERNAME / ACQ_DB_PASSWORD | Admin 投放数据源，仅允许已授权的 106.55.61.223:3306/toufang |
| BOOT_DB_URL / BOOT_DB_USERNAME / BOOT_DB_PASSWORD | Mini 默认业务数据源；投放同样指向指定 toufang。Admin 使用示例中的 ADMIN_DB_* 覆盖公共默认项 |
| REDIS_HOST / REDIS_PORT / REDIS_PASSWORD / REDIS_DATABASE | 实际可用的 Redis，不能假定旧电脑本机服务已迁移 |
| JWT_SECRET | 管理端随机签名密钥，符合现有 HS512 长度要求；不再使用仓库默认值 |
| UPLOAD_DIR | Admin 实际可写的上传目录；已上传图片文件需另行确认来源与备份 |
| WX_APP_ID / WX_APP_SECRET | 原小程序授权配置；不等于公众号或微信支付授权 |

JDBC URL 的 TLS 与证书参数按已授权环境设置，不以关闭安全校验解决网络问题。远程库连接可能需要防火墙、白名单或 VPN 配置；本轮没有修改网络规则。系统库需要已有 sys_user、sys_role、sys_menu 等数据及登录账号，SQL 文件存在不代表数据已导入。

将对应 .example 内容复制到被忽略的 application-local.yml，或使用明确的外部配置路径。Admin 默认 local；Mini 选择 local 才加载其本地文件。不要把秘密添加到受跟踪文件。

本轮没有启动应用。Admin 的 SysJobServiceImpl 初始化会清理并重新登记 Quartz 任务；示例禁用 Quartz 自动运行，仍需检查任务存储方式及启动生命周期后再启动。Mini 启动还会拉起 Netty。报告 Worker 默认 false，仅在完成迁移和运行验收后显式开启；支付 live 保持 false。

无根 POM。使用 Java 21，先构建当前依赖，避免旧缓存：

```powershell
mvn -f jsj-common/pom.xml install
mvn -f jsj-service/pom.xml '-Dtest=AcquisitionRuleTest,AcquisitionConfigRuleTest,AcquisitionSchemaTest,PlainTextReportRendererTest,ConfigurationSafetyTest,ReportGenerationTest,OrderReportSnapshotTest,AuthDeliverySafetyTest,ReportAdminServiceTest' install
mvn -f jsj-admin/pom.xml '-Dtest=ConfigurationPermissionTest' package
mvn -f jsj-mini/pom.xml package -DskipTests
```

本机 Maven 未加入 PATH 时使用已安装 Maven 的绝对路径。不要无筛选运行服务层全部测试：AcquisitionConstraintJdbcTest 会写数据库，且保留了旧本地库假设。

Admin UI 使用现有 package-lock.json；已有依赖可直接执行 npm run build:prod 和 npx --no-install vue-tsc --noEmit。本轮没有升级依赖、重写锁文件。本轮完整类型检查与生产构建分别通过；两项命令仍须独立执行。

数据库脚本均未执行：

1. 空库初始化使用 sql/acquisition.sql（27 张表，含 B2/R3 字段）；不用于给已有表升级。
2. 现有 R0-A/B1 库先核对结构，再依次审查 acquisition_r0b1.sql、acquisition_r0b2.sql、acquisition_r3.sql。
3. 已有字段脚本不会覆盖其类型，必须核对实际定义；sid 重复等约束冲突需要人工调查，不能靠删数据解决。
4. 旧主键若不是 AUTO_INCREMENT，另行审查既有 acquisition_auto_increment.sql，不能盲目连跑。
5. 核验实际系统库与父菜单后审查 acq_menu_r0b2.sql、acq_menu_r3.sql。B2 旧脚本仍固定 ry-vue，实际库名不符时不得直接运行；R3 需明确设置已核验的 @approved_system_database，默认 NULL 不插入。检查 ID/权限冲突，不自动分配角色权限。
6. DDL 自动提交，先备份与确定执行窗口；错误可能留下已完成的字段及迁移过程。检查失败位置并重新比对差异；本轮不执行 DROP、过程清理或全量重跑。

jsj-uni-app 与根目录 generator 是用户主动删除，保持删除；无需寻找旧 R1–R2 或搬回 fmqx/zaq。H5、支付及订单凭证后续按新基线实现，本轮不决定新前端方案。

## 2026-10-22 本机诊断

- MySQL 工具 SELECT DATABASE() 报 Transport closed；这是工具传输故障。
- 沙箱内 TCP 探测失败，获准在沙箱外对唯一指定地址探测后，106.55.61.223:3306 连接成功。未扫描其他地址/端口。
- 未发现 Admin/Mini application-local.yml；进程环境的 ADMIN_DB_*、ACQ_DB_*、BOOT_DB_*、REDIS_HOST、JWT_SECRET、UPLOAD_DIR 未设置。没有进行 MySQL 认证或 JDBC 查询，因此不推断账号错误、表权限或数据源实际绑定。
- 凭据通过已授权本地配置文件或环境注入；不要在对话中发送密码或私钥。连接目标可达不代表已获数据库权限。
- 两个开关继续关闭，无应用启动、真实支付/企微/广告请求、数据库写入或迁移。
- 报告管理页面路径 acq/report/index，权限 acq:report:query、acq:report:content、acq:report:retry；权限需系统库负责人审查后分配，不能用编辑权限代替正文/补生成权限。

# Toufang / Scaffolding-v2

广告投放获客系统。2026-10-23 已补齐独立 H5、匿名订单凭证、支付确认事务、持久化报告与异步任务、企微回调和广告回传适配代码，并完成指定数据库与 Chrome 隔离验收。真实微信支付、公众号授权、企微和广告接收尚未联调，不能作为生产上线完成结论。

当前模块归属和本轮前端验证见 [H5 目录纠正记录](docs/codex-h5-module-correction.md)。旧 jsj-h5 验收不等于 jsj-mini-app 验收。

历史业务交付、边界和遗留事项见 [业务链路交付报告](docs/codex-business-flow-review.md)。旧 R3 文档的认证失败、尚无 H5、未执行 SQL 等描述已被本报告替代，旧报告保留溯源。

## 当前工程

| 目录 | 职责 |
| --- | --- |
| jsj-mini-app | 唯一 H5 前端（Vue 3 / TypeScript / Vite），对接 jsj-mini |
| jsj-mini | H5 API、平台回调、Mini 专属 Worker，保留移动端功能 |
| jsj-service | 配置、订单、支付、报告、企微、广告领域事务 |
| jsj-admin / jsj-admin-ui | 系统权限、配置与报告管理、业务查询、受控全额退款 |
| jsj-common | 公共能力 |
| sql / config-examples / docs | 数据库脚本、脱敏配置与验收文档 |

用户主动删除的 jsj-h5、jsj-uni-app、根 generator 保持删除；不需要 fmqx、zaq 或内部框架仓库。后台内置生成工具仍保留。主键保持 BIGINT AUTO_INCREMENT / IdType.AUTO，浏览器业务 ID 使用字符串。

## 构建

需要 JDK 21、Maven、Node/npm，没有根聚合 POM。使用明确离线测试白名单：

```powershell
mvn -f jsj-common/pom.xml install -DskipTests
mvn -f jsj-service/pom.xml '-Dtest=AcquisitionRuleTest,AcquisitionConfigRuleTest,AcquisitionSchemaTest,PlainTextReportRendererTest,ConfigurationSafetyTest,ReportGenerationTest,OrderReportSnapshotTest,AuthDeliverySafetyTest,ReportAdminServiceTest,FlowSecurityTest,PaymentConfirmationTest,PlatformCryptoTest,AdDeliveryAdapterTest' install
mvn -f jsj-admin/pom.xml '-Dtest=ConfigurationPermissionTest' package
mvn -f jsj-mini/pom.xml package -DskipTests
```

不要无筛选执行 AcquisitionConstraintJdbcTest：它会写数据库且仍有旧环境假设。

后台：进入 jsj-admin-ui，按既有锁文件安装依赖，执行 `npx --no-install vue-tsc --noEmit`、`npm run build:prod`。H5：进入 jsj-mini-app，执行 `npm ci --ignore-scripts`、`npx --no-install vue-tsc --noEmit`、`npm run build`。没有为通过检查升级既有依赖。

## 本地启动与数据库

本机已保留被 Git 忽略的 `.local/application-business.yml`、`.local/application-business-admin.yml`，从项目根或模块目录启动均自动导入。其他电脑按 [配置说明](config-examples/README.md) 准备本地值，示例不会自动生效。

投放库唯一为 `106.55.61.223:3306/toufang`。本次 JDBC 核实当前系统登录、角色、菜单表也在该库；其他环境仍须确认系统库，不能假定 localhost/ry-vue 存在。历史业务轮次已执行 `sql/acquisition_business_flow.sql` 和 `sql/acq_menu_business_flow.sql`；未执行全量初始化、迁移旧系统表或给真实角色授权。不要导入全量 SQL 覆盖已有数据。

```powershell
# 从项目根运行，先确认端口和本地配置。
java -jar jsj-admin/target/jsj-admin.jar --spring.quartz.job-store-type=memory --spring.quartz.auto-startup=false
java -jar jsj-mini/target/jsj-mini-1.0.0.jar
# 分别在前端模块运行 npm run dev。
```

Admin 默认 8080、Mini HTTP 8881；后台沿用现有 Vite 配置，H5 开发端口 5174。H5 页面 `/pages/{已发布页面ID}`、订单 `/orders/{订单号}`，后台 `/acq/business`。H5 Host 必须对应已批准且启用的域名和页面关联，localhost 不等于已配置业务域名。Cookie 与 API 同源，生产 HTTPS 代理参考 [Nginx 示例](config-examples/h5-nginx.conf.example)。

支付、OAuth、退款、企微外发/回调和各 Worker 默认关闭。Worker 还需显式 `since`，避免处理历史任务。历史业务验收端口 63400–63404 已释放，临时数据、账号和登录会话已清理。

## 历史业务验收（前端目录纠正前）

服务层离线测试 53 项、管理权限测试 4 项通过；两个后端包、后台与 H5 完整类型检查和生产构建通过。指定数据库验证了并发下单、支付/任务整体回滚、入账幂等、租约保护、报告持久化和多老师关系隔离。Chrome 验证了表单、订单恢复、报告及后台菜单权限。未进行微信实机或真实平台外发，未覆盖项见交付报告。

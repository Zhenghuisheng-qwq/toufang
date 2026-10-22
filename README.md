# Toufang / Scaffolding-v2

广告投放获客系统的当前开发基线：管理后台、独立 Mini 后端、共享领域服务及原生小程序。配置后台、模板渲染和报告持久化已实现；真实支付、H5 订单凭证、企微回调和三平台转化回传仍需后续开发与联调。

用户已明确主动删除 `jsj-uni-app` 与根目录 `generator`，保持删除，不寻找旧 R1–R2、不搬回 fmqx/zaq。H5 产品需求保留，新的前端实现方式另行决定。后台内置代码/表单生成工具与报告生成业务继续保留。

## 当前目录

| 目录 | 职责 |
| --- | --- |
| jsj-admin | Spring Boot 3 管理后端，系统权限、运营配置和报告管理 |
| jsj-admin-ui | Vue 3、TypeScript、Element Plus 管理前端 |
| jsj-mini | Mini HTTP/Netty 后端；报告 Worker 仅在此注册，默认关闭 |
| jsj-common | 共享异常和基础能力 |
| jsj-service | 领域 Entity、Mapper、Service、报告渲染与持久化 |
| jsj-mini-app | 原生微信小程序，本轮保持不变 |
| sql | 初始化与增量脚本、菜单；执行前先比对实际结构 |
| config-examples | 无秘密的本机配置示例和安全构建命令 |
| docs | 当前接手审核和验收结果 |
| poc | 内存规则实验，不代表生产 API 已接通 |

## 构建与运行条件

使用 JDK 21、Maven、Node/npm 和现有 package-lock.json。无根 Maven 聚合 POM。先安装 common/service 当前产物，再构建两个应用：

```powershell
mvn -f jsj-common/pom.xml install -DskipTests
mvn -f jsj-service/pom.xml '-Dtest=AcquisitionRuleTest,AcquisitionConfigRuleTest,AcquisitionSchemaTest,PlainTextReportRendererTest,ConfigurationSafetyTest,ReportGenerationTest,OrderReportSnapshotTest,AuthDeliverySafetyTest,ReportAdminServiceTest' install
mvn -f jsj-admin/pom.xml '-Dtest=ConfigurationPermissionTest' package
mvn -f jsj-mini/pom.xml package -DskipTests
cd jsj-admin-ui
npx --no-install vue-tsc --noEmit
npm run build:prod
```

不要无筛选运行 `AcquisitionConstraintJdbcTest`，它会写数据库。构建不需要启动应用、调度器或连接业务库。

投放库唯一指定为 `106.55.61.223:3306/toufang`；系统登录/角色/菜单库独立配置，不能假定本机有 ry-vue。Redis、JWT 签名密钥、上传目录和数据库认证需要本地配置。示例文件不会自动加载，详见 [配置说明](config-examples/README.md)。不得把密码/私钥提交或粘贴到聊天。

本轮没有执行 SQL。不要直接导入历史全量初始化脚本覆盖现有库，也不要因本机缺少执行记录而重复升级。报告菜单增量 `sql/acq_menu_r3.sql` 默认禁止写入，必须先确认实际系统库、父菜单和 ID/权限冲突；不会自动授权角色。

应用默认端口 Admin 8080、Mini HTTP 8881 / WS 9001。实际启动前检查系统库和 Redis、Quartz 初始化行为及端口归属；支付和报告 Worker 开关保持关闭。本轮未启动应用、未调用真实平台。

## 当前验收

完整前端类型检查与生产构建通过；Java 离线测试 47 项通过，POC 7 项通过。数据库工具故障与认证配置缺失仍导致实际数据库、四条后台 CRUD 和浏览器验收未完成，状态 `CURRENT_BASE_PARTIAL`。不能将这些构建/测试结果当作整个产品交付。

详细范围、问题编号和修改清单见 [当前审核报告](docs/codex-r3-review.md)。[原项目索引](doc/project-index.md) 保留历史参考，其中旧模块或环境描述以当前基线为准。

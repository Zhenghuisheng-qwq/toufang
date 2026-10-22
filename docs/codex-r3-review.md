# 当前源码续作验收：类型修复 / 报告后台 / 数据库诊断

状态：**CURRENT_BASE_PARTIAL**。日期：2026-10-22。以下为最新结论；后半部分保留上一轮测试记录，不篡改历史结果。历史“缺失待恢复”“找回 R1–R2”“必须恢复 UniApp”的解释已被用户决定取代。当前源码与历史提示词不同不构成接手阻断。

## 1. 当前基线与保留范围

当前仓库 `D:/code/dz/toufang/scaffolding-v2`，`master`，HEAD `f6b6f95`。开始时已有上一轮大量未提交成果；本轮在其上继续，未 reset、clean、恢复、提交、推送或部署。

`jsj-uni-app`、根目录 `generator` 是主动删除，仍不存在，不恢复、不寻找旧分支或 fmqx/zaq。原生 `jsj-mini-app` 未修改。H5 产品需求保留，实现方案不在本轮决定；H5、订单凭证、支付入口后续按新基线开发。

没有根聚合 POM；common → service → admin/mini 依次使用当前源码产物。四个 POM 未发现 zhenai-framework、systemPath 或 fmqx/zaq 引用。当前构建不依赖旧仓库；使用已有 Maven 第三方缓存离线构建，未声称已用空缓存重新下载所有依赖。

## 2. 修改范围与文件清单

- 前端：修正分页 API 泛型、Vue 全局实例扩展、Element Plus/axios/Quill 官方声明、表单/上传/树/表格引用、路由元数据、空值和返回模型；添加报告后台页面和 API。
- 后端：新增 ReportAdminService / ReportAdminMapper / ReportAdminController / 显式状态 DTO，复用已有生成器；加强支付身份一致性；增加离线补生成与权限测试。
- 文档与交接：修正主动删除解释、配置说明、运行边界；增加报告菜单增量；修复 .gitignore 对真实源码目录的误忽略。
- 保留前轮配置后台、模板、订单快照、报告 Worker、SQL 等成果。完整当前工作区相对 HEAD 文件清单见文末，包含继承改动，不能把全部条目都算成本轮新开发。

## 3. 完整类型检查

命令：在 `jsj-admin-ui` 执行 `npx --no-install vue-tsc --noEmit`。实际版本：Node 22.18.0、TypeScript 5.6.3、vue-tsc 2.1.10、Vue 3.5.26。

本轮重新取得的原始诊断为 **250 项 / 55 个文件**，不是沿用旧报告数字。原始日志：`jsj-admin-ui/target/typecheck-current.txt`。主要类型：TS2339 119、TS2322 44、TS18048 23、TS18047 15、TS2367 13；其余为类型参数、索引、可空值、导入和重载等错误。

主要文件：cache/index.vue 29、DraggableItem.vue 26、job/detail.vue 16、lock.vue 12、SidebarItem.vue 10、user/index.vue 10、genInfoForm.vue 9、role/authUser.vue 7。不能把全部错误未经证据归因于脚手架历史。

已修根因：

1. 分页返回 `TableDataInfo<T>` 本身包含 `rows: T[]`，API 误传 `T[]` 导致二维数组，已纠正。
2. 以真正的 Vue 模块增强声明全局插件方法，增加只允许 setup 内使用的实例检查，不再覆盖 Vue 包声明。
3. 移除 Element Plus/axios/Quill 的空模块声明，使用安装包的真实类型。新增暴露的组件契约错误同样修正，过程中诊断数曾升至 267，并未掩盖。
4. 改用类型明确的 template ref、子组件 expose 与可空值判断；修复树节点字段、字典/日志状态类型、角色授权的逗号分隔参数、单对象/列表模型。
5. 修复实际行为：Vue 2 `$set` 残留、按钮将 MouseEvent 当成业务行、定时器立即调用、错误导入路径、表单设计器事件名不一致。未删除后台内置生成工具。

**最终 0 项，退出 0**。`strict` 和检查范围未降低，没有新增批量 any、ts-ignore、排除 acq 或转 JavaScript。既有少量宽类型不是本轮全面清理目标；类型通过不等于所有页面运行验收通过。package-lock.json 和依赖版本未修改。

## 4. 数据库连接与运行时诊断

| 阶段 | 实际结果 |
| --- | --- |
| MySQL 工具 | 只读 SELECT DATABASE() 返回 Transport closed，工具传输层失败 |
| 指定目标 TCP | 沙箱内探测失败；获准在沙箱外单次连接 106.55.61.223:3306 成功。未扫描其他主机或端口 |
| 应用认证配置 | Admin/Mini application-local.yml 均不存在；Process/User/Machine 环境均未设置下列核心变量 |
| MySQL 认证 / 表权限 | NOT_RUN，没有取得供本轮使用的授权认证配置，不能报告认证失败或权限不足 |
| JDBC 驱动与参数 | Maven 编译依赖可用；实际 JDBC 查询 NOT_RUN，不能判断参数匹配或服务器身份 |
| Profile | 源码 Admin 默认 local，Mini 示例需选 local；未启动，实际 profile 加载 NOT_RUN |
| SqlSessionFactory / Tx | 源码 acq MapperScan 指向 acqSqlSessionFactory，工厂和 acqTransactionManager 通过同一 selectDataSource 选择；Admin 没有 acq 配置时拒绝回落系统库。运行时绑定 NOT_RUN |
| SELECT DATABASE()/结构与索引 | NOT_RUN，未确认真实 schema、AUTO_INCREMENT 回填、B1/B2/R3 是否已安装 |

缺少的配置项及用途见 `config-examples/README.md`：`ADMIN_DB_URL/USERNAME/PASSWORD`（系统登录、菜单、角色），`ACQ_DB_URL/USERNAME/PASSWORD`（Admin 投放库），`BOOT_DB_URL/USERNAME/PASSWORD`（Mini 默认库），`REDIS_HOST`（Redis）、`JWT_SECRET`（管理鉴权）、`UPLOAD_DIR`（本地上传）。仅报告名称，不输出秘密，不要求聊天粘贴密码。

系统库名称不能假定为本机 ry-vue。TCP 可达不代表数据库授权或 JDBC 认证通过；当前证据也不要求擅自开放防火墙。凭据、数据库权限和 TLS 参数由负责人通过授权本地配置提供。

## 5. SQL 与菜单实际执行

**业务和系统库写入 0 次；SQL/菜单执行 0 次。** 未创建数据库、迁移系统表、清表、重置自增、修改远程密码或分配角色。

已有 B1/B2/R3 字段和唯一约束尚未对比实际库，不能声称已升级。新增 `sql/acq_menu_r3.sql` 对应实际报告页面及 query/content/retry 权限，默认 `@approved_system_database=NULL` 不插入；需要核对实际系统库、父菜单、ID 2008/2802/2803 与权限冲突，备份后再执行差异。不会自动授权普通角色。

B2 旧菜单脚本仍固定 ry-vue：实际库名不同不得直接执行。旧增量脚本存在创建/清理存储过程语句，不能在本轮“禁止 DROP”要求下整份盲跑；后续应按已核对差异形成获准的执行集合。

## 6. 四条配置后台验收

| 链路 | 已实现/离线证据 | 实际 DB/API CRUD | 浏览器 |
| --- | --- | --- | --- |
| 商户及版本 | 配置 CRUD、身份/历史保护、版本/敏感权限、引用完整性，相关现有测试回归通过 | NOT_RUN | NOT_RUN |
| 企微企业/老师/标签 | 企业归属、关联保护、独立凭据权限、模式与引用配置；所有权等离线规则通过 | NOT_RUN | NOT_RUN |
| 承接路由 | 专属页面作用域、企业/老师资格、无失效跨企业兜底、活动唯一键及事务代码保留 | NOT_RUN；未切真实全局路由 | NOT_RUN |
| 腾讯/百度/快手账户 | 平台筛选、多账户配置、身份保护、引用权限、待确认模式；未假称 connected | NOT_RUN | NOT_RUN |

离线测试与源码检查不能替代数据库锁、唯一约束或真实页面操作。没有测试数据 ID 可交付，因为本轮没有执行实际 CRUD。

## 7. 报告后端和受控补生成

- `jsj-service/.../acq/report/ReportAdminServiceImpl.java:42`：acq 事务内锁定唯一 REPORT_GENERATE 任务 → 订单 → 支付 → 报告 → 冻结版本，与 Worker 的关键锁顺序一致；无效订单/资格抛错，事务回滚新增任务。
- 支付资格复用 ReportGenerationServiceImpl.paid，并加强非空商户/AppID及支付记录订单号一致性。要求订单/支付均为成功状态、真实记录的交易号与时间、同金额/身份，不信任请求传入“已支付”。这仍不是微信通知验签；真实支付确认入口尚待开发，不能把数据库状态检查称作平台联调通过。
- 缺失冻结快照、非发布版本、不支持的模板内容、有效或异常租约拒绝；已有成功正文返回原报告，失败/删除的既有报告禁止覆盖。条件领取带 lease_version，重用同一 ReportGenerationService 完成正文持久化和任务完成。
- `jsj-admin/.../acq/ReportAdminController.java:14`：精确订单号状态查询、独立正文读取、显式补生成。`acq:report:query/content/retry` 三种权限分别执行；响应 no-store，补生成审计不记录请求/响应正文。状态 DTO 不含姓名生辰、凭据、票据或报告正文，未知错误映射为固定分类。
- `jsj-admin-ui/src/views/acq/report/index.vue`：精确查询、状态/冻结模板版本/时间/错误/任务展示、正文按权限单独获取、补生成确认及重复点击保护；正文纯文本插值，无 v-html。不含随机分数、测算或伪 AI。
- 新增 10 个 Service 离线测试，调用真实共享渲染/生成服务与内存 Mapper；新增 1 个真实 Spring 方法安全代理测试，检查查询权限不隐含正文/补生成权限。均通过。仍缺真实 MySQL 事务、并发和浏览器证据。

## 8. 稳定问题编号与回归

未确认新的未修复 P0，不将待验证项当作已发生缺陷。

| 编号 | 本轮结论 |
| --- | --- |
| ACQ-001 P1 | 源码仍使用环境注入，未重新引入弱默认凭据；历史真实凭据由所有者确认并轮换，未改远程密码/历史 |
| ACQ-002 P1 | 新建草稿/停用、编辑不写启停状态和独立权限保留；离线回归通过，真实接口验收待补 |
| ACQ-003 P1 | 发布父锁 + version 当前锁定读保留；已发布保护离线测试通过，MySQL 并发未执行 |
| ACQ-004 P1 | 页面 reportTemplateId 显式提交保留，模板选项权限与绑定代码回归；浏览器未执行 |
| ACQ-005 P1 | 非开发环境无供应商仍明确拒绝发送，负向测试通过，无验证码输出 |
| ACQ-006 P1 | 系统/投放事务绑定源码保留，编译回归通过；运行时双库证据待补 |
| ACQ-007 P2 | 链接长度 512 与 SQL 一致，离线规则测试通过 |
| ACQ-008 P2 | 完整类型检查由本轮实测 250 降为 0，声明/API/组件根因修复；不是靠跳过检查通过 |
| ACQ-009 P2 | 仍待处理：jsj-admin/src/main/resources/logback.xml:4 固定 /home/jsj/logs，正式本机启动前须确认可写目录；未为测试启动该文件日志 |
| ACQ-010 P2 | 仍待验证：生产构建有 vendor/vue 循环 chunk 与大包警告；未在浏览器证明无启动问题 |
| ACQ-011 待验证 | 真实 schema、索引、回填、隔离、并发、故障恢复全部 NOT_RUN；TCP 可达不能替代 |
| ACQ-012 P1 已修复 | .gitignore 原 **/build/ 误忽略 views/tool/build 的 6 个业务源码文件，干净交接会遗漏；增加精确例外（.gitignore:277），当前变为可审查的未跟踪源码，尚未提交 |
| ACQ-013 P1 已修复 | ReportGenerationServiceImpl.java:65 原 paid 比较允许双方缺失商户/AppID相等；新增非空及支付记录订单号校验，并有拒绝路径测试。无已发生资金事故证据 |

## 9. 最终构建和测试

| 验证 | 当前实际结果 |
| --- | --- |
| common 离线 install -DskipTests | 退出 0，无测试源码 |
| service 离线显式安全名单 install | **43/43**，0 失败/错误/跳过，退出 0 |
| admin ConfigurationPermissionTest package | **4/4**，0 失败/错误/跳过，退出 0 |
| mini package -DskipTests | 退出 0，无测试源码，不启动应用 |
| Java 合计 | **47 项**，保留旧 36 项 + 新增 11 项；不是沿用旧数量 |
| POC node --test poc/p0-exec/run-tests.mjs | **7/7**，纯内存实验，不算生产 H5/支付闭环 |
| 完整 vue-tsc | **0 错误、退出 0** |
| npm run build:prod | **退出 0**，保留 chunk 警告 |
| Admin/Mini 嵌入 service JAR | SHA256 与本轮 service 产物一致，排除旧缓存产物 |

日志位于各模块 target/current-*-build.log、jsj-admin-ui/target/typecheck-final.txt。首次新增补生成测试 43 项中出现 2 个错误：内存替身原地改变领取版本导致后续读取错位；已显式固定领取前版本，再运行全部测试通过。未删除断言、跳过失败用例或运行会写数据库的 AcquisitionConstraintJdbcTest。前端解除空类型声明后出现的额外错误亦已修复，不隐藏失败历史。

## 10. 浏览器验证

**NOT_RUN**：未发现可用浏览器工具，且当前认证配置/运行版本未确认。商户、企微、路由、广告、模板预览、报告后台均没有浏览器验收记录。没有把离线代理测试、接口定义或 Vite 成功称作浏览器通过。

## 11. 测试数据残留

无数据库测试记录、无 CODEX_VERIFY_* ID、无清理或逻辑删除残留。仅产生正常 target/dist 构建产物与测试日志；未清除审计日志。

## 12. 开关与外部副作用

`acq.pay.live=false`、`acq.report.worker.enabled=false` 保持。Worker 仍仅 Mini 注册，Admin 仅在获权限的显式请求内处理单个订单，不启动自动消费。真实支付、退款、企微、广告调用均 **0 次**。

## 13. 当前运行实例

本轮未启动、停止或重启应用。只读端口检查发现 8080/PID 51076、8881/PID 59380 已有 Java 监听；其来源、profile 和运行产物未经验证，不能作为本次运行验收。发现 redis-server 进程 PID 9872，但未确认本地 6379 监听或供本项目使用的授权配置；不据此声称 Redis 已就绪。未读取/输出其他进程秘密或接管未知实例。

## 14. 剩余明确工作与交接结论

1. 通过授权本地配置补充系统库、投放库、Redis、JWT、上传/日志目录；连接后核对实际 acq 数据源、SELECT DATABASE()、schema 和权限。
2. 备份并比对 B1/B2/R3/菜单差异，只执行必要且获准的增量；使用独立 CODEX_VERIFY_* 记录完成四条后台真实 CRUD、并发与回滚验证。不得操作真实全局路由。
3. 确认运行的就是本轮产物，再完成浏览器和无权限/错误反馈验收；验证 chunk 警告影响。
4. H5、订单访问凭证、微信支付确认/退款/公众号授权、企微回调关联及三平台真实适配器，后续按当前基线原生实现；不寻找或恢复被放弃的旧工程。

**当前源码足以在本机独立继续开发，不需要再带 fmqx/zaq 或恢复主动删除模块。** 编译、类型检查和离线规则已通过；缺少的是明确运行配置与实际数据库/浏览器验收，故 CURRENT_BASE_PARTIAL，不是 BLOCKED，也不表示整个广告投放系统已交付。

---

# 上一轮记录（历史，不作为当前执行指令）

状态：**R0B2_PARTIAL / R3_PARTIAL / AUDIT_PARTIAL / HANDOFF_PARTIAL**。

本次继续工作的起点为 D:/code/dz/toufang/scaffolding-v2，master，f6b6f95，工作区干净。该提交已包含前一段新增的配置后端、报告渲染器、事务修复；本轮保留并继续实现。未提交、推送或部署。

历史报告的 R1R2_CODE_READY 与当前源码不一致：当前 Mini 业务入口主要是原认证接口，没有所述 H5 下单、支付 SDK/通知、公众号授权、订单凭证和一次性票据链路；jsj-uni-app 已在 bb988d7 中删除，本轮没有恢复用户已提交的删除。POC 中同名能力不是生产调用链。

## 本轮实现与实际范围

| 功能 | 实现位置（仓库相对路径） | 已实现 | 未完成或未验证 |
| --- | --- | --- | --- |
| 商户与版本 | jsj-service/.../acq/impl/MerchantAdminServiceImpl.java；jsj-admin/.../acq/PayMerchantController.java；jsj-admin-ui/src/views/acq/merchant/index.vue | 列表、基础信息、启停、历史身份保护、草稿版本、完整性、激活、域名展示；敏感引用独立权限 | 引用解析、微信授权、真实支付 NOT_RUN；真实数据库并发版本测试未执行 |
| 企微企业/老师/标签 | WecomAdminServiceImpl.java、WecomConfigController.java；views/acq/wecom/index.vue | 企业标识、模式、引用；老师/标签维护、筛选、归属和引用保护 | 不调用 Token、回调、出码、备注或标签接口；子列表目前按企业加载后筛选，尚无服务端分页 |
| 承接路由 | WecomRouteServiceImpl.java、WecomRouteController.java；views/acq/route/index.vue | 全局/指定页优先、企业老师选择、入口类型、切换与解析预览；指定失效不换企业；原事务与 active_key 唯一约束保留 | 数据库锁等待、死锁重试体验、并发切换回滚待真实隔离测试；不出码、不建 state |
| 三平台账户配置 | AdAccountAdminServiceImpl.java、AdAccountController.java；views/acq/ad/index.vue | 腾讯/百度/快手，多账户、接口模式、事件和金额单位待确认项、引用管理、历史身份保护 | 没有真实平台回传适配器；配置启用不表示联调成功 |
| 模板绑定和预览 | LandingPageAdminServiceImpl.java、ReportTemplateAdminServiceImpl.java；views/acq/landing/index.vue、template/index.vue | 保存模板关联、远程选择已发布配置、同一纯文本渲染器预览、未知变量拒绝发布；发布版本不覆盖 | 客户最终正文/算法未提供；纯文本变量替换不是测算算法 |
| 订单输入快照 | jsj-service/src/main/java/com/zhs/service/acq/report/OrderReportSnapshot.java；TradeOrderServiceImpl.java | 从实际页面、线索、发布版本冻结 ID 和输入；覆盖不可信的传入快照，出生时间空值保持空；缺模板拒绝创建 | 下单公开 API 和订单凭证基线缺失；真实创建订单链路未验证 |
| 报告持久化 | acq/report/ReportGenerationServiceImpl.java；mapper/acq/ReportWorkMapper.java | 再校验支付记录与订单金额/商户/AppID、一订单一报告、保存正文和生成时间、已有成功报告复用、稳定失败分类 | 支付真实性仍依赖尚缺的 R1–R2 验签入口；MySQL 事务/唯一冲突/进程崩溃恢复 NOT_RUN |
| 报告任务 | jsj-mini/src/main/java/com/zhs/config/ReportWorker.java；ReportWorkMapper.java | Mini 专属、默认关闭，仅 REPORT_GENERATE；DB 时间领取，owner+lease_version 拒绝旧 Worker；报告及完成同一事务；临时错误有限重试，配置错误留待处理 | 受控修复/后台补生成入口、完整任务运营界面未实现；未启动调度器 |
| 支付后任务创建 | PayRecordServiceImpl.java | 本地成功状态转换事务内幂等登记报告任务，不消费企微/广告任务 | 此方法不是微信通知验签/真实支付客户端；不应直接作为公开支付成功接口 |
| H5 报告与访问 | 当前 jsj-uni-app 缺失 | 未新建第二套工程或凭证系统，未开放只凭订单号查正文接口 | 报告页、支付结果页衔接、一次性票据/跨订单凭证校验、访问退款规则、后台正文/补生成均未完成 |

配置前端统一使用现有 Element Plus、权限 Store、请求封装及后台风格；包含加载、保存防重复点击、分页、筛选、空态及危险操作确认。新增写命令不绑定系统字段。商户版本写操作同时要求版本和敏感权限。敏感读取响应 no-store，写操作日志不保存请求/响应正文；前端敏感配置与报告预览不再进入通用重复提交 sessionStorage 缓存。

报告生成不读取最新模板替换旧订单、不修改支付事实、不生成假二维码、不增加 fakePaid/forceSuccess 等入口。退款访问语义未擅自决定。报告核心依赖已持久化支付事实，但真实平台联调仍为 0 次。

## 独立性、环境与数据

- Maven 无根 POM/Wrapper；四个 Java 模块已按当前源码依赖构建。未发现 zhenai-framework-*、旧 fmqx/zaq 路径、systemPath 或内部父 POM。定向检查后端目录未发现外部源码软链接。不需要搬回旧公司仓库。
- JDK 21.0.11 可用；Maven 3.9.11 在 D:/env/maven/apache-maven-3.9.11/bin/mvn.cmd，未进入 PATH。Node 22.18.0、npm 10.9.3，后台 package-lock.json 与 node_modules 存在。未安装/升级依赖或改写锁文件。
- application-local.yml 被忽略且当前缺失。新增 config-examples/*.example 与说明，不自动加载。缺少系统库、Redis、上传目录和本机授权网络配置时需在本地补齐，不在聊天中提供秘密。
- 本机未发现 6379 监听；系统库登录、菜单和角色数据未确认。没有启动或终止任何原有应用实例；归属不明的端口和进程未操作。
- 指定业务库仍是 106.55.61.223:3306/toufang。MySQL 工具的只读 SELECT DATABASE()/hostname/version 请求返回 Transport closed；**没有成功确认实际运行时数据源**，不能将配置地址当成连接验证。
- 业务、系统数据库写入 0 次；没有测试记录 ID 或清理残留；SQL、菜单脚本均未执行。acq.pay.live=false，acq.report.worker.enabled=false。
- 本轮修改了受跟踪的公共配置：DB 凭据/URL、Redis host、JWT Secret 改为环境注入，Admin 上传目录改为 UPLOAD_DIR；关闭默认 Druid 监控页与慢 SQL 正文日志。此前进入 Git 的凭据是否仍有效未验证；移除当前值不清除历史，所有者应检查并轮换真实有效凭据。

## SQL 与菜单

- acquisition.sql：仍为 27 张表；同步 B2 配置字段、sid 唯一索引与 R3 快照/正文/领取版本字段。
- acquisition_r0b1.sql：补齐上一轮模板正文/版本指针、域名登记核验、页面模板关联等增量。
- acquisition_r0b2.sql：新增配置字段及 sid 唯一索引，按 information_schema 检查存在性。
- acquisition_r3.sql：只新增实际使用的订单快照、报告正文/生成模式/时间、任务 lease_version；不回填历史订单、不伪造付款、不覆盖旧报告。
- acq_menu_r0b2.sql：新增四组菜单及权限；检查系统库、R0-B1 父菜单、ID/权限冲突；不覆盖既有 23 条菜单、不自动授权角色。
- 全部脚本仍需实际结构核对、备份、执行窗口及 SQL 执行验收。DDL 自动提交；既有字段类型不会被脚本偷偷改写。没有为尚未实现的后台报告补生成创建假菜单。

## 稳定问题清单

未确认 P0，不将待验证问题当作已发生事故。

| 编号 | 风险/状态 | 证据与处理 |
| --- | --- | --- |
| ACQ-001 | P1 已修复（源码） | 公共数据源配置曾携带固定账号密码，Admin JWT 有默认值：jsj-service/src/main/resources/application-common.yml:10；jsj-admin/src/main/resources/application.yml:85。改为环境变量，历史有效性和轮换待所有者处理 |
| ACQ-002 | P1 已修复 | 落地页和域名 add/edit 接收状态，绕过独立启停权限。LandingPageAdminServiceImpl.java:80、SiteDomainAdminServiceImpl.java:81 新建强制草稿/停用，更新不写状态；UI 状态在编辑中只读 |
| ACQ-003 | P1 已修复 | 模板发布先普通读取后加父锁，RR 下再次普通读取仍可能是旧正文。ReportTemplateAdminServiceImpl.java:219 改为当前锁定读；编辑/删除/发布共用父锁 |
| ACQ-004 | P1 已修复 | 落地页前端 payload 原来遗漏 reportTemplateId，会清除已有绑定；现已显式保存并显示绑定配置 |
| ACQ-005 | P1 已修复 | AuthServiceImpl.java:65 无短信供应商却在非开发环境正常返回，开发日志还打印验证码。非开发模式现明确拒绝，不写验证码；开发验证码不再输出日志，随机源改为 SecureRandom；增加负向回归 |
| ACQ-006 | P1 继承修复并编译回归 | DruidConfig.java:33 显式系统事务管理器，投放写操作使用 acqTransactionManager；本轮未做真实双库事务验证 |
| ACQ-007 | P2 已修复 | ConfigRules.java 获客链接长度原 1024 与 SQL 512 不一致；统一 512，离线边界测试通过 |
| ACQ-008 | P2 部分修复 | 全局 declare module vue 覆盖包真实类型已移除；完整 vue-tsc 暴露 250 项脚手架错误，例如 components/Editor/index.vue:36、layout/components/Navbar.vue:83。未通过 any/关闭检查来掩盖；本轮 acq 页面/API 无类型诊断 |
| ACQ-009 | P2 待修复 | jsj-admin/src/main/resources/logback.xml:4 固定 /home/jsj/logs，新电脑需确认目录/权限或后续改为环境配置；测试使用 logback-test.xml，避免继承文件日志写入 |
| ACQ-010 | P2 待验证 | Vite 提示 vendor/vue 循环 chunk；构建成功不代表浏览器启动链路已通过 |
| ACQ-011 | 待验证 | 真实 schema、索引、AUTO_INCREMENT 回填、跨库事务隔离、路由并发和报告唯一冲突/回滚都没有数据库证据；规则/DDL 文本测试不等同数据库测试 |

## 审核覆盖与未覆盖

| 模块 | 已检查 | 验证/边界 |
| --- | --- | --- |
| jsj-common | POM、业务异常、共享基础依赖 | 构建通过；没有声称逐行审查所有工具 |
| jsj-service/acq | 27 表实体/Mapper、配置服务、支付/订单/任务骨架、模板与报告新调用链、引用和事务 | 离线测试通过；数据库集成未执行 |
| jsj-service/auth | 原登录链路与短信发送分支 | 修复已证实的假发送成功和日志问题；微信登录/IM 底层未完整审计 |
| jsj-admin | 配置 Controller、方法权限、Druid/系统事务、模板接口、Quartz 启动逻辑 | 最小 Spring 方法安全上下文测试；系统所有 CRUD、导出、上传、代码生成未逐项 E2E |
| jsj-admin-ui | 新配置页面、原模板/落地页/域名、请求/权限封装、类型声明、构建 | 构建通过、完整类型检查失败；无浏览器视觉验收 |
| jsj-mini | POM、原认证入口、LoginInterceptor、报告 Worker 注册范围 | 编译通过；未启动 HTTP/Netty/调度器，无真实 API 联调 |
| jsj-mini-app | 保留范围与 Git diff | 未修改、未做原生端功能验收 |
| jsj-uni-app / generator | 当前目录和 Git 删除历史 | 源码缺失，不能声称已审查或构建 |
| SQL / POC | 结构、迁移文本、菜单权限及既有 POC | POC 是内存规则实验，不是已接通生产接口 |

## 实际验证证据

Maven 使用上述本机绝对路径和 -o。先 common install，再 service install，再两个应用 package；未运行 spring-boot:run。

| 命令 | 结果 |
| --- | --- |
| mvn -o -f jsj-common/pom.xml install -DskipTests | 退出 0（Common 无测试源码） |
| mvn -o -f jsj-service/pom.xml -Dtest=AcquisitionRuleTest,AcquisitionConfigRuleTest,AcquisitionSchemaTest,PlainTextReportRendererTest,ConfigurationSafetyTest,ReportGenerationTest,OrderReportSnapshotTest,AuthDeliverySafetyTest install | 33 项，0 失败/错误/跳过，退出 0 |
| mvn -o -f jsj-admin/pom.xml -Dtest=ConfigurationPermissionTest package | 3 项，0 失败，退出 0；真实方法权限代理+内存 Service 替身，不是 HTTP 登录验收 |
| mvn -o -f jsj-mini/pom.xml package -DskipTests | 退出 0；Mini 无测试源码 |
| npm run build:prod（jsj-admin-ui） | 退出 0，有 chunk 警告 |
| npx --no-install vue-tsc --noEmit | 退出 2，250 项错误；记录位于 jsj-admin-ui/target/typecheck-result.txt |
| node --test poc/p0-exec/run-tests.mjs | 7/7，通过；纯内存 POC，不计为真实支付/票据实现 |
| 后端包嵌入 Service JAR SHA256 对比 | 使用当前源码产物；最终再次校验 |

报告核心测试覆盖冻结输入/版本、缺失时间、模板语法/输出边界、未支付/金额不符拒绝、重复报告复用、旧 owner/旧 lease_version/过期租约拒绝、结束时失租抛错、配置失败不修改支付。Mapper 替身测试**不证明真实 MySQL 事务回滚**。

保留失败过程：首次 Maven install 因沙箱无缓存写权限失败，获授权后成功；新增权限测试初次存在测试语法和请求上下文缺失，已修复并最终 3/3 通过。没有删除断言或跳过关键测试换通过。旧 AcquisitionConstraintJdbcTest 会写旧假定库，未运行。

四条浏览器链路（商户、企微、路由、广告）全部 **NOT_RUN**；模板预览与绑定浏览器链路、H5 未支付/无凭证拒绝、成功报告、真实付款闭环也为 **NOT_RUN**。原因：数据库只读连接工具不可用、本机系统库/Redis/登录尚未确认、缺少浏览器工具及 H5/R1–R2 代码。真实支付、退款、企微、广告接口调用次数：**0**。

## 当时的后续建议（已被用户最新决定及本轮结论取代，不执行）

1. 【判断已撤回】当时建议寻找旧 R1–R2/恢复 UniApp；用户现已明确主动放弃这些旧实现，保持删除，不再寻找或恢复。
2. 在同一订单权限链上完成报告公开 DTO、访问票据、H5 状态页/支付结果页衔接、后台正文查看与受控补生成。现有报告生成核心可复用。
3. 本地补配置，恢复获授权的只读数据库访问；核对 schema/备份后执行已审查迁移和菜单，完成隔离并发及四条浏览器验收。
4. 将剩余前端类型错误和未覆盖的系统/IM 范围作为明确审核尾项，不标整个项目已全面通过。

结论：**不需要再带 fmqx/zaq 即可独立继续后端和后台开发**。当前源码可以独立构建，但环境/数据库验收和缺失的 H5/R1–R2 代码需要补齐；这不等于必须迁回旧系统整仓。R3 仍有页面、凭证、后台恢复和验证缺口，不能宣称 R3_CODE_READY 或整个产品完成。

## 本轮修改文件清单（相对起点 f6b6f95）

- AGENTS.md
- config-examples/admin-local.yml.example
- config-examples/mini-local.yml.example
- config-examples/README.md
- docs/codex-r3-review.md
- jsj-admin-ui/src/api/acq/config.ts
- jsj-admin-ui/src/api/acq/template.ts
- jsj-admin-ui/src/types/global.d.ts
- jsj-admin-ui/src/views/acq/ad/index.vue
- jsj-admin-ui/src/views/acq/config-fields.ts
- jsj-admin-ui/src/views/acq/ConfigManager.vue
- jsj-admin-ui/src/views/acq/domain/index.vue
- jsj-admin-ui/src/views/acq/landing/index.vue
- jsj-admin-ui/src/views/acq/merchant/index.vue
- jsj-admin-ui/src/views/acq/route/index.vue
- jsj-admin-ui/src/views/acq/template/index.vue
- jsj-admin-ui/src/views/acq/wecom/index.vue
- jsj-admin/src/main/java/com/zhs/web/controller/acq/AdAccountController.java
- jsj-admin/src/main/java/com/zhs/web/controller/acq/PayMerchantController.java
- jsj-admin/src/main/java/com/zhs/web/controller/acq/ReportTemplateController.java
- jsj-admin/src/main/java/com/zhs/web/controller/acq/WecomConfigController.java
- jsj-admin/src/main/java/com/zhs/web/controller/acq/WecomRouteController.java
- jsj-admin/src/main/resources/application.yml
- jsj-admin/src/test/java/com/zhs/web/controller/acq/ConfigurationPermissionTest.java
- jsj-admin/src/test/resources/logback-test.xml
- jsj-mini/src/main/java/com/zhs/config/ReportWorker.java
- jsj-service/src/main/java/com/zhs/service/acq/ConfigRules.java
- jsj-service/src/main/java/com/zhs/service/acq/impl/LandingPageAdminServiceImpl.java
- jsj-service/src/main/java/com/zhs/service/acq/impl/PayRecordServiceImpl.java
- jsj-service/src/main/java/com/zhs/service/acq/impl/PostPayTaskServiceImpl.java
- jsj-service/src/main/java/com/zhs/service/acq/impl/ReportTemplateAdminServiceImpl.java
- jsj-service/src/main/java/com/zhs/service/acq/impl/SiteDomainAdminServiceImpl.java
- jsj-service/src/main/java/com/zhs/service/acq/impl/TradeOrderServiceImpl.java
- jsj-service/src/main/java/com/zhs/service/acq/impl/WecomAdminServiceImpl.java
- jsj-service/src/main/java/com/zhs/service/acq/report/OrderReportSnapshot.java
- jsj-service/src/main/java/com/zhs/service/acq/report/ReportGenerationService.java
- jsj-service/src/main/java/com/zhs/service/acq/report/ReportGenerationServiceImpl.java
- jsj-service/src/main/java/com/zhs/service/acq/ReportTemplateAdminService.java
- jsj-service/src/main/java/com/zhs/service/acq/RouteEligibility.java
- jsj-service/src/main/java/com/zhs/service/acq/WecomAdminService.java
- jsj-service/src/main/java/com/zhs/service/auth/impl/AuthServiceImpl.java
- jsj-service/src/main/java/com/zhs/service/mapper/acq/ReportTemplateVersionMapper.java
- jsj-service/src/main/java/com/zhs/service/mapper/acq/ReportWorkMapper.java
- jsj-service/src/main/java/com/zhs/service/model/acq/PostPayTask.java
- jsj-service/src/main/java/com/zhs/service/model/acq/TradeOrder.java
- jsj-service/src/main/java/com/zhs/service/model/acq/TradeReport.java
- jsj-service/src/main/resources/application-common-admin.yml
- jsj-service/src/main/resources/application-common.yml
- jsj-service/src/test/java/com/zhs/service/acq/AcquisitionSchemaTest.java
- jsj-service/src/test/java/com/zhs/service/acq/AuthDeliverySafetyTest.java
- jsj-service/src/test/java/com/zhs/service/acq/ConfigurationSafetyTest.java
- jsj-service/src/test/java/com/zhs/service/acq/report/OrderReportSnapshotTest.java
- jsj-service/src/test/java/com/zhs/service/acq/report/ReportGenerationTest.java
- sql/acq_menu_r0b2.sql
- sql/acquisition_r0b1.sql
- sql/acquisition_r0b2.sql
- sql/acquisition_r3.sql
- sql/acquisition.sql


## 当前工作区完整修改清单（含前轮继承成果）

以下为相对 HEAD f6b6f95 的当前完整清单；未提交，包含前轮继承成果。本轮也将原被忽略的 6 个后台表单设计器文件显式保留，不等于恢复根目录 generator。

- `.gitignore`
- `AGENTS.md`
- `README.md`
- `config-examples/README.md`
- `config-examples/admin-local.yml.example`
- `config-examples/mini-local.yml.example`
- `docs/codex-r3-review.md`
- `jsj-admin-ui/src/api/acq/config.ts`
- `jsj-admin-ui/src/api/acq/report.ts`
- `jsj-admin-ui/src/api/acq/template.ts`
- `jsj-admin-ui/src/api/monitor/job.ts`
- `jsj-admin-ui/src/api/monitor/jobLog.ts`
- `jsj-admin-ui/src/api/monitor/logininfor.ts`
- `jsj-admin-ui/src/api/monitor/online.ts`
- `jsj-admin-ui/src/api/monitor/operlog.ts`
- `jsj-admin-ui/src/api/system/config.ts`
- `jsj-admin-ui/src/api/system/dict/data.ts`
- `jsj-admin-ui/src/api/system/dict/type.ts`
- `jsj-admin-ui/src/api/system/notice.ts`
- `jsj-admin-ui/src/api/system/post.ts`
- `jsj-admin-ui/src/api/system/role.ts`
- `jsj-admin-ui/src/api/system/user.ts`
- `jsj-admin-ui/src/components/Crontab/index.vue`
- `jsj-admin-ui/src/components/Editor/index.vue`
- `jsj-admin-ui/src/components/FileUpload/index.vue`
- `jsj-admin-ui/src/components/ImageUpload/index.vue`
- `jsj-admin-ui/src/components/RightToolbar/index.vue`
- `jsj-admin-ui/src/components/SizeSelect/index.vue`
- `jsj-admin-ui/src/components/TopNav/index.vue`
- `jsj-admin-ui/src/layout/components/HeaderNotice/index.vue`
- `jsj-admin-ui/src/layout/components/IframeToggle/index.vue`
- `jsj-admin-ui/src/layout/components/Navbar.vue`
- `jsj-admin-ui/src/layout/components/Settings/index.vue`
- `jsj-admin-ui/src/layout/components/Sidebar/Link.vue`
- `jsj-admin-ui/src/layout/components/Sidebar/SidebarItem.vue`
- `jsj-admin-ui/src/layout/components/Sidebar/index.vue`
- `jsj-admin-ui/src/layout/components/TagsView/ScrollPane.vue`
- `jsj-admin-ui/src/layout/components/TagsView/index.vue`
- `jsj-admin-ui/src/layout/components/TopBar/index.vue`
- `jsj-admin-ui/src/layout/index.vue`
- `jsj-admin-ui/src/types/api/monitor/job.ts`
- `jsj-admin-ui/src/types/api/monitor/jobLog.ts`
- `jsj-admin-ui/src/types/api/monitor/operlog.ts`
- `jsj-admin-ui/src/types/api/system/role.ts`
- `jsj-admin-ui/src/types/global.d.ts`
- `jsj-admin-ui/src/types/vue-instance.d.ts`
- `jsj-admin-ui/src/utils/component-proxy.ts`
- `jsj-admin-ui/src/views/acq/ConfigManager.vue`
- `jsj-admin-ui/src/views/acq/ad/index.vue`
- `jsj-admin-ui/src/views/acq/config-fields.ts`
- `jsj-admin-ui/src/views/acq/domain/index.vue`
- `jsj-admin-ui/src/views/acq/landing/index.vue`
- `jsj-admin-ui/src/views/acq/merchant/index.vue`
- `jsj-admin-ui/src/views/acq/report/index.vue`
- `jsj-admin-ui/src/views/acq/route/index.vue`
- `jsj-admin-ui/src/views/acq/template/index.vue`
- `jsj-admin-ui/src/views/acq/wecom/index.vue`
- `jsj-admin-ui/src/views/error/401.vue`
- `jsj-admin-ui/src/views/lock.vue`
- `jsj-admin-ui/src/views/login.vue`
- `jsj-admin-ui/src/views/monitor/cache/index.vue`
- `jsj-admin-ui/src/views/monitor/cache/list.vue`
- `jsj-admin-ui/src/views/monitor/job/detail.vue`
- `jsj-admin-ui/src/views/monitor/job/index.vue`
- `jsj-admin-ui/src/views/monitor/job/log.vue`
- `jsj-admin-ui/src/views/monitor/logininfor/index.vue`
- `jsj-admin-ui/src/views/monitor/online/index.vue`
- `jsj-admin-ui/src/views/monitor/operlog/detail.vue`
- `jsj-admin-ui/src/views/monitor/operlog/index.vue`
- `jsj-admin-ui/src/views/monitor/server/index.vue`
- `jsj-admin-ui/src/views/register.vue`
- `jsj-admin-ui/src/views/system/config/index.vue`
- `jsj-admin-ui/src/views/system/dept/index.vue`
- `jsj-admin-ui/src/views/system/dict/data.vue`
- `jsj-admin-ui/src/views/system/dict/detail.vue`
- `jsj-admin-ui/src/views/system/dict/index.vue`
- `jsj-admin-ui/src/views/system/menu/index.vue`
- `jsj-admin-ui/src/views/system/notice/index.vue`
- `jsj-admin-ui/src/views/system/post/index.vue`
- `jsj-admin-ui/src/views/system/role/authUser.vue`
- `jsj-admin-ui/src/views/system/role/index.vue`
- `jsj-admin-ui/src/views/system/role/selectUser.vue`
- `jsj-admin-ui/src/views/system/user/authRole.vue`
- `jsj-admin-ui/src/views/system/user/index.vue`
- `jsj-admin-ui/src/views/system/user/profile/resetPwd.vue`
- `jsj-admin-ui/src/views/system/user/profile/userAvatar.vue`
- `jsj-admin-ui/src/views/system/user/profile/userInfo.vue`
- `jsj-admin-ui/src/views/tool/build/CodeTypeDialog.vue`
- `jsj-admin-ui/src/views/tool/build/DraggableItem.vue`
- `jsj-admin-ui/src/views/tool/build/IconsDialog.vue`
- `jsj-admin-ui/src/views/tool/build/RightPanel.vue`
- `jsj-admin-ui/src/views/tool/build/TreeNodeDialog.vue`
- `jsj-admin-ui/src/views/tool/build/index.vue`
- `jsj-admin-ui/src/views/tool/gen/basicInfoForm.vue`
- `jsj-admin-ui/src/views/tool/gen/createTable.vue`
- `jsj-admin-ui/src/views/tool/gen/editTable.vue`
- `jsj-admin-ui/src/views/tool/gen/genInfoForm.vue`
- `jsj-admin-ui/src/views/tool/gen/importTable.vue`
- `jsj-admin-ui/src/views/tool/gen/index.vue`
- `jsj-admin/src/main/java/com/zhs/web/controller/acq/AdAccountController.java`
- `jsj-admin/src/main/java/com/zhs/web/controller/acq/PayMerchantController.java`
- `jsj-admin/src/main/java/com/zhs/web/controller/acq/ReportAdminController.java`
- `jsj-admin/src/main/java/com/zhs/web/controller/acq/ReportTemplateController.java`
- `jsj-admin/src/main/java/com/zhs/web/controller/acq/WecomConfigController.java`
- `jsj-admin/src/main/java/com/zhs/web/controller/acq/WecomRouteController.java`
- `jsj-admin/src/main/resources/application.yml`
- `jsj-admin/src/test/java/com/zhs/web/controller/acq/ConfigurationPermissionTest.java`
- `jsj-admin/src/test/resources/logback-test.xml`
- `jsj-mini/src/main/java/com/zhs/config/ReportWorker.java`
- `jsj-service/src/main/java/com/zhs/service/acq/ConfigRules.java`
- `jsj-service/src/main/java/com/zhs/service/acq/ReportTemplateAdminService.java`
- `jsj-service/src/main/java/com/zhs/service/acq/RouteEligibility.java`
- `jsj-service/src/main/java/com/zhs/service/acq/WecomAdminService.java`
- `jsj-service/src/main/java/com/zhs/service/acq/impl/LandingPageAdminServiceImpl.java`
- `jsj-service/src/main/java/com/zhs/service/acq/impl/PayRecordServiceImpl.java`
- `jsj-service/src/main/java/com/zhs/service/acq/impl/PostPayTaskServiceImpl.java`
- `jsj-service/src/main/java/com/zhs/service/acq/impl/ReportTemplateAdminServiceImpl.java`
- `jsj-service/src/main/java/com/zhs/service/acq/impl/SiteDomainAdminServiceImpl.java`
- `jsj-service/src/main/java/com/zhs/service/acq/impl/TradeOrderServiceImpl.java`
- `jsj-service/src/main/java/com/zhs/service/acq/impl/WecomAdminServiceImpl.java`
- `jsj-service/src/main/java/com/zhs/service/acq/report/OrderReportSnapshot.java`
- `jsj-service/src/main/java/com/zhs/service/acq/report/ReportAdminService.java`
- `jsj-service/src/main/java/com/zhs/service/acq/report/ReportAdminServiceImpl.java`
- `jsj-service/src/main/java/com/zhs/service/acq/report/ReportAdminView.java`
- `jsj-service/src/main/java/com/zhs/service/acq/report/ReportGenerationService.java`
- `jsj-service/src/main/java/com/zhs/service/acq/report/ReportGenerationServiceImpl.java`
- `jsj-service/src/main/java/com/zhs/service/auth/impl/AuthServiceImpl.java`
- `jsj-service/src/main/java/com/zhs/service/mapper/acq/ReportAdminMapper.java`
- `jsj-service/src/main/java/com/zhs/service/mapper/acq/ReportTemplateVersionMapper.java`
- `jsj-service/src/main/java/com/zhs/service/mapper/acq/ReportWorkMapper.java`
- `jsj-service/src/main/java/com/zhs/service/model/acq/PostPayTask.java`
- `jsj-service/src/main/java/com/zhs/service/model/acq/TradeOrder.java`
- `jsj-service/src/main/java/com/zhs/service/model/acq/TradeReport.java`
- `jsj-service/src/main/resources/application-common-admin.yml`
- `jsj-service/src/main/resources/application-common.yml`
- `jsj-service/src/test/java/com/zhs/service/acq/AcquisitionSchemaTest.java`
- `jsj-service/src/test/java/com/zhs/service/acq/AuthDeliverySafetyTest.java`
- `jsj-service/src/test/java/com/zhs/service/acq/ConfigurationSafetyTest.java`
- `jsj-service/src/test/java/com/zhs/service/acq/report/OrderReportSnapshotTest.java`
- `jsj-service/src/test/java/com/zhs/service/acq/report/ReportAdminServiceTest.java`
- `jsj-service/src/test/java/com/zhs/service/acq/report/ReportGenerationTest.java`
- `sql/acq_menu_r0b2.sql`
- `sql/acq_menu_r3.sql`
- `sql/acquisition.sql`
- `sql/acquisition_r0b1.sql`
- `sql/acquisition_r0b2.sql`
- `sql/acquisition_r3.sql`
- `sql/admin.sql`

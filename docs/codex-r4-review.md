# R4：真实数据库联调与后台浏览器验收

## 1. RESULT 与范围

**CURRENT_ADMIN_ACCEPTANCE_READY**（2026-10-22，仅当前四块配置后台与报告管理范围）。

已完成：当前 yml → 指定 MySQL/JDBC → 真实结构增量 → 本轮 Admin 产物 → 真实登录、菜单、配置操作与报告管理 → 精确清理。

不代表完整投放系统可上线。H5、真实支付确认、订单访问凭证、企微回调与广告真实回传仍属于后续阶段。没有恢复主动删除的 jsj-uni-app、根 generator，没有修改 jsj-mini-app，没有读取 fmqx/zaq，没有提交、推送或部署。

## 2. 工作区基线及本轮文件

工作目录：`D:/code/dz/toufang/scaffolding-v2`。继承上一轮累计成果；本轮期间检测到用户提交，HEAD 从历史记录的 f6b6f95 变为 **0fde08a**。用户同步更新的两个 application-common*.yml 被保留，并以其真实配置重新连接成功。不能将该提交或历史 148 项变更记为本轮开发。

本轮相对当前 HEAD 的交付：

| 文件 | 变化 |
| --- | --- |
| jsj-admin-ui/vite.config.ts | 移除产生运行时循环依赖的强制 manualChunks 分组，保留 Rollup 自动分包与页面动态导入 |
| sql/acq_menu_r0b2.sql | 固定 ry-vue 改为调用方明确指定、经核验的 @approved_system_database；未设置则拒绝 |
| sql/acq_menu_r3.sql | 允许经明确核验的系统表与业务表同库；保留显式目标、父菜单及碰撞保护 |
| sql/acquisition_auto_increment.sql | 移除目标检查之前的 DROP PROCEDURE IF EXISTS，避免删除未知同名过程；toufang 目标修正已包含在用户期间提交中 |
| sql/acquisition_r4_verified_increment.sql | 本次实际执行的 25 条 DDL 记录，非可盲目重放的迁移器 |
| docs/codex-r4-review.md | 本报告 |

本地连接、备份、验收脚本、截图、日志、产物在已忽略的 `jsj-admin/target/r4/`。没有全量暂存或覆盖用户配置。git ls-files 已确认配置组件、报告页面、Controller、Service 和生成器源码被跟踪；git check-ignore 已确认本地连接、数据备份和浏览器证据被忽略。

## 3. 配置及实际来源

遵照“直接连接 yml”，读取当前 `jsj-service/src/main/resources/application-common.yml` 中有效的数据源。未继续使用此前认证失败的聊天配置。

本轮实例使用外部、已忽略的 `jsj-admin/target/r4/application-r4.yml`，显式通过 `spring.config.additional-location` 加载：

| 配置 | 本轮用途及结论 |
| --- | --- |
| spring.datasource.url / username / password | 系统库；采用已验证 yml 连接，实际为指定 toufang |
| acq.datasource.url / username / password | 投放库；同一指定 toufang，不同命名的数据源/事务管理器 |
| spring.data.redis.host / port / password | 用户指定服务器；无密码，PING 成功 |
| spring.data.redis.database | 使用 15；启动前 SELECT 15 + DBSIZE 确认空库，未 FLUSHDB |
| token.secret | 本轮生成的本地随机签名密钥；不输出、不提交 |
| ruoyi.profile | 本工作区下 target/r4/uploads；未依赖旧电脑目录 |
| cors.allowed-origins | 仅本地验收前端 http://127.0.0.1:18181 |
| spring.quartz.auto-startup / job-store-type | false / memory；实际日志证明 RAMJobStore、NOT STARTED、执行数 0 |
| spring.sql.init.mode | never |
| acq.pay.live / acq.report.worker.enabled | false / false |
| logging.config | 本地 console 配置，日志由启动器重定向到 target/r4，未使用旧 /home/jsj/logs |

原后台登录信息未猜测或重置。创建两个临时非管理员账号及独立角色进行真实登录，使用现有验证码、密码验证和 JWT/Redis 流程。验收后已删除临时账号和角色。

第三方 Secret、支付私钥、广告 Token 无需补齐；没有为本轮后台验收启用第三方。

## 4. 数据库分层验证

| 层级 | 结果 |
| --- | --- |
| TCP | 指定 106.55.61.223:3306 可达 |
| 协议/认证 | 当前 yml 认证成功；早期旧连接 1045 已被当前配置成功结果取代 |
| 实际库/版本 | SELECT DATABASE(), VERSION()：toufang / MySQL 8.0.46 |
| 项目 JDBC | 使用本项目 Admin jar 内现有 mysql-connector-j；查询、事务和增量均实际成功 |
| 应用绑定 | 本轮浏览器写入经 Admin Controller → Service → Acq Mapper，在独立 JDBC 连接 toufang 中读到相同 ID/关联；系统登录、菜单同样实际读取该库 |

MySQL MCP 的 Transport closed 是工具故障；改用项目现有 JDBC 后完成验证，没有反复重试该工具。

系统与业务表本次确实同库，不再假设 ry-vue 或本机 jsj_toufang_r0a。AcqPersistenceConfiguration 的投放 Mapper/SQL session/事务绑定保持不变；Admin 主事务仍使用 dynamicDataSource。

## 5. 真实结构差异

实际 **47 张表：27 张投放业务表及 20 张系统表**。业务表数量与 acquisition.sql 的真实 CREATE TABLE 数一致。

迁移前逐列比对类型、默认值、空值、主键自增、普通/唯一索引及索引前缀：

- 19 个字段缺失：报告正文/生成模式/时间 3 个，商户验签配置 3 个，订单报告快照 7 个，企业 sid 1 个，路由说明 2 个，广告接口配置 3 个。
- 5 个类型差异：merchant_serial_no 64→128；wecom_route.link_id 64→128；lease_version int→bigint；auth_mode、amount_unit 旧 tinyint→当前字符串枚举。
- 缺少 wecom_corp.uk_wecom_sid 唯一索引。
- 所有 27 表主键均为 id，BIGINT AUTO_INCREMENT；无需执行自增迁移脚本。

现有数字枚举的数据库注释给出了含义：授权 0=待确认、1=自建应用；金额 0=待确认、1=分、2=元。已先备份，再按精确现有 ID 保义转换：wecom_corp 1、2；ad_account 1、2、3。没有猜测授权结果或把配置登记当作平台联调。

迁移后重新查询 information_schema：字段/默认值/空值/索引差异为 **0**；27 表主键检查无差异。逻辑删除列仍按原规则，未修改历史删除状态。新增订单快照没有回填历史事实。

## 6. SQL 与菜单执行

- 未执行 acquisition.sql 初始化、全表重建或 acquisition_auto_increment.sql。
- 备份受影响七表数据及迁移前完整字段/索引快照后，执行 **25 条 DDL，各 1 次**。DDL 自动提交，逐条记录成功，不声称 DDL 可整体事务回滚。
- 旧枚举 **5 行**在单独事务内按备份 ID 和旧值条件转换；其他原有配置值未主动改写。
- sid 新增时原有行均为 NULL，唯一约束无冲突；未删除冲突数据。
- 原 sys_menu 有 85 条、没有投放菜单。核验 ID、权限和父路由无冲突后，仅安装 R4 必要的父菜单 1 条、B2 29 条、报告 3 条，合计 **33 条**。
- 菜单安装路径执行 **2 次**：第二次记录数量仍为 33，父/报告新增数为 0；没有重复菜单。R3 使用已核验会话变量 toufang 执行 INSERT 部分，未盲目执行脚本的默认 NULL 设置。
- 没有修改原有两角色的授权。测试时只给新建测试角色分别授予 33/10 个菜单节点；清理时删除这 43 条临时角色菜单关联。
- 其余 B1 页面菜单不是本轮四块配置/报告范围，未扩大安装范围。

## 7. ID、关联、事务及唯一约束

实际浏览器创建记录并经真实 Service/Mapper 回填字符串 ID：商户 2、企业 3、老师 3、标签 2、路由 3、广告 4。商户版本真实接口创建 3、4（版本号 1、2）。后续接口能通过回填 ID 查询、更新或关联。

独立 JDBC 复核：版本 3/4→商户 2；老师 3→企业 3；标签 2→对应 CorpID；路由 3→测试页面 3、企业 3、老师 3。报告 1/2 使用冻结模板版本 1。

- 重复商户身份提交被真实唯一约束转换为业务错误，无第二条商户记录。
- 未支付/缺快照补生成均失败，实际 post_pay_task 中这两订单记录数为 0，证明 ensureReport 的先写入被事务回滚。
- 对已生成订单并发提交 3 次补生成，均返回同一 reportId=1、taskId=3、attemptCount=1；正文不变。
- 有效租约保持 status=1、leaseVersion=7；过期租约恢复后成功，leaseVersion=8。
- 广告及路由经页面删除后 is_delete=1，详情拒绝、列表不可见；没有用物理删除冒充页面删除成功。最终清理另按测试标记执行精确物理删除。

## 8. 实例及产物对应

| 项目 | 本次实例 |
| --- | --- |
| 后端 | 本轮构建 jsj-admin/target/jsj-admin.jar，监听 127.0.0.1:18180 |
| SHA-256 | 77BEE87B21F70D9630908230E7D9D4173CD5C53FDD247D6BF36F20C96DFA0BC7 |
| 后端 PID | 初始 52888，配置精确 CORS 后仅重启自己的实例为 42100 |
| 前端 | 本轮生产构建 jsj-admin-ui/dist，127.0.0.1:18181；本地代理 /prod-api → 18180 |
| 前端 PID | 27224 |
| 浏览器 | 独立无头 Chrome，专用 profile；CDP 仅监听 127.0.0.1:19224，PID 43324 |
| 现有实例 | 未把 8080/8881/占用的 18080 当作验收实例；未停止它们 |

结束后核对端口所属 PID，仅停止 42100、27224、43324。不存在需要用户接管的本轮后台进程。

## 9. 四块配置后台实际验收

| 模块/页面 | API / 权限 | 实际结果 |
| --- | --- | --- |
| 支付商户 /acq/merchant | /acq/merchant；list/query/add/edit/status/remove/version/secret | 页面新增、三项必填提示、编辑、启用、关键词/状态筛选、详情与刷新后重新读取通过；真实 API 创建两版本并启用版本 4；存在版本/订单引用时删除拒绝；普通详情无密钥引用 |
| 企业微信 /acq/wecom | /acq/wecom；list/query/add/edit/status/remove/secret | 页面新增、必填提示、编辑授权模式、启用；页面新增并启用老师，新增标签；详情正确显示企业隔离关联和路由引用；企业/老师被引用时删除拒绝 |
| 承接路由 /acq/route | /acq/route；list/query/add/edit/switch/remove/preview | 页面选择独立测试落地页、企业、老师，新增/编辑/启用；页面解析预览；停用/删除通过；空企业提交被后端拒绝；未切换已有全局承接 |
| 广告账户 /acq/ad | /acq/ad；list/query/add/edit/status/remove/secret | 页面创建腾讯配置、编辑金额单位、启用/停用、名称筛选、删除通过；空名称/账户 ID 提示；没有真实广告 API 调用 |

四页均从后端 getRouters 返回的菜单进入。列表分页 total/首屏及 pageNum=2 空页边界经真实接口核对；本次每模块测试数据不足一整页，**没有声称验证大量数据跨页 UI**。低权限账号刷新后逐页重新读取，新增按钮不存在，直接 POST 四类写接口均返回业务 code=403。

关联表：merchant→pay_merchant/pay_merchant_version/domain_merchant；wecom→wecom_corp/wecom_teacher/wecom_tag；route→wecom_route/landing_page/corp/teacher；ad→ad_account（历史引用 ad_click/ad_conversion）。关联域名表在商户详情读取，未额外新增本轮范围之外的域名业务。

## 10. 报告管理实际验收

入口 `/acq/report`，接口 `/acq/report/{orderNo}`、`/content`、`/retry`。

“受控补生成”是**同步调用本地纯文本生成器**，不是仅入队；本轮已实际生成并在页面查看保存的正文，未启用后台 Worker。

| 用例 | 结果与证据 |
| --- | --- |
| 状态/版本/时间/任务 | 页面和接口显示订单、冻结 1/1、生成时间、任务 ID/状态、次数；与 JDBC 一致 |
| 普通详情不泄漏正文 | 低权限状态接口成功，无 content_text 或渲染正文 |
| 独立正文权限 | 仅 query 账号直接 GET content：code=403 |
| 独立补生成权限 | 同账号直接 POST retry：code=403；页面两按钮均移除 |
| 未支付 | CODEX_VERIFY_R4_unpaid：拒绝，未遗留任务 |
| 缺快照 | CODEX_VERIFY_R4_missing：拒绝，不借用当前模板，未遗留任务 |
| 成功生成/冻结版本 | CODEX_VERIFY_R4_paid 生成 report=1；模板当前版本为 2，正文仍来自冻结版本 1 的 OLD 文案 |
| 成功报告保护/重复 | 三次并发重复均返回同一报告和任务，正文、生成时间、执行次数不变 |
| 有效租约 | CODEX_VERIFY_R4_live 被拒绝；未知错误只显示 REQUIRES_REVIEW |
| 过期租约恢复 | CODEX_VERIFY_R4_expired 生成 report=2，task=2，租约版本 7→8 |

合成文本只用于验证确定性模板替换，不能解释为已完成测算算法、正式报告内容或外部生成器联调。第三方生成执行 NOT_RUN。

## 11. 测试数据清理及保留

所有测试数据采用 CODEX_VERIFY_R4 标记。实例停止后，事务内锁定精确 ID、逐条验证标记、扫描业务关联确认外部引用为 0，并保留清理前本地快照后删除：

| 表 | 精确 ID |
| --- | --- |
| trade_order / pay_record | 各 1,2,3,4,5 |
| trade_report | 1,2 |
| post_pay_task | 1,2,3 |
| pay_merchant | 2 |
| pay_merchant_version | 3,4 |
| wecom_corp / wecom_teacher / wecom_route | 各 3 |
| wecom_tag | 2 |
| ad_account | 4 |
| landing_page | 3 |
| report_template | 1 |
| report_template_version | 1,2 |
| sys_user / sys_role | 各 100,101 |
| sys_user_role / sys_role_menu | 上述测试账号/角色的 2/43 条关联 |

新 JDBC 连接提交后复查，以上业务与账号记录全部为 0；原有用户 2、角色 2 保留。保留新增的 33 条正式菜单、35 条测试操作审计及 6 条登录审计，没有删除审计日志。没有按模糊前缀清库或重置 AUTO_INCREMENT，测试造成的 ID 空洞正常保留。

保留本地忽略的验收证据/备份/配置；Redis 15 的系统配置和字典缓存不执行全库清理。测试登录已退出；验证码按原 TTL 失效。

## 12. 测试、类型检查、构建与发现的问题

| 检查 | 本轮结果 |
| --- | --- |
| service 离线指定单元测试 | 43/43，通过；未运行会写库的 AcquisitionConstraintJdbcTest |
| admin ConfigurationPermissionTest | 4/4，通过 |
| POC | 7/7，通过；不当作生产 H5/支付实现 |
| 完整 vue-tsc --noEmit | 修改后通过，0 错误；未放宽 tsconfig |
| npm run build:prod | 修改前后均可构建，修正后生产产物通过真实浏览器 |
| service install / admin package | 当前源码产物构建通过 |
| common / mini | 继承上一轮构建通过；本轮未改这两个模块，不伪造本轮重跑结果 |
| 浏览器异常复查 | 清空历史事件后重载：Runtime.exceptionThrown / Log.entryAdded / Network.loadingFailed 收集结果 [] |

初次 service install 因沙箱不能写已有 Maven 缓存失败，获准后成功；属于本地文件权限，不是源码错误。最初 yml 前的 1045、临时测试密码长度不符合既有 20 字符限制、验收新端口未列入 CORS 均已针对本轮配置/脚本解决，未改认证规则。

有证据的问题：

- **P1，已修复**：jsj-admin-ui/vite.config.ts:26 原 manualChunks 的 Vue/vendor 强制分组导致生产登录页 `Cannot access 'tm' before initialization`，页面停在资源加载。移除强制分组，生产重建、真实登录/四页/报告及重载均通过。
- **P1，已修复**：sql/acq_menu_r0b2.sql:8、sql/acq_menu_r3.sql:21 错误假定系统库必须 ry-vue、不能是 toufang，阻止实际合法部署。改用明确核验的系统库变量，保留默认拒绝/碰撞保护；重复安装通过。
- **P2，已修复**：sql/acquisition_auto_increment.sql:7 在目标核验前删除同名过程；移除前置 DROP，本轮未执行该迁移。
- **P2，保留性能观察项**：自动分包后仍有大 chunk 构建提示；当前页面运行正常，本轮未做无关体积优化。

本轮未发现需要重写架构的 P0。并发测试覆盖报告重复提交；不声称已完成整个系统的压力、所有并发切换组合或空 Maven 缓存构建验证。

## 13. 开关与外部调用

本轮 Admin 外部配置：acq.pay.live=false，acq.report.worker.enabled=false；报告 Worker 位于 Mini，本轮未启动 Mini Worker。Quartz 实际 RAM 存储、暂停，任务执行数 0。没有自动 schema 初始化。

本轮支付、企微、广告及收费生成接口调用次数：**0**。只有指定 MySQL/Redis 访问和本机浏览器/API 验收。原有 8080/8881 等其他实例没有被当成证据，不推断那些既有实例的开关状态。

## 14. 剩余事项与接续位置

本轮范围无剩余配置阻塞，**不需要用户再次提供密码或补旧仓库**。本机可以基于当前源码独立继续开发。

后续优先接真实业务闭环：H5、访问/订单凭证、微信支付创建/通知验签/查单与退款、企微回调与客户关联、三平台真实回传、受控启用任务调度。按现有领域服务和快照边界继续，不恢复用户删除的工程、不将配置后台验收等同于这些能力完成。

开发时继续使用有效 yml。需要重启本轮形式的实例时，使用独立端口与本地外部配置；临时测试账号已清理，不再可登录。原有系统账号及角色保持原样。

证据位置：`jsj-admin/target/r4/schema-diff.json`、`pre-migration-schema.yml`、`migration-ledger.log`、`browser-evidence.jsonl`、`data-verification.tsv`、`after-cleanup.tsv`、`cleanup-complete.txt`、构建/测试日志及 generated-report.png。备份与连接文件仅本机保留，不作为可提交资源。

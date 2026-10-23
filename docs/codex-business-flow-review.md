# 投放获客业务链路实施与验收

日期：2026-10-23。工作目录：`D:/code/dz/toufang/scaffolding-v2`。起始 HEAD：`5bf91ae6990a0af510566b4a9cb43bbf1be37cc8`，起始工作区干净。本轮未提交、推送或部署；所有修改留在工作区。

## 交付结论

**本地业务链路与持久化验收通过；真实平台联调未执行；产品仍有明确未完成范围。** 不能将此结论写成整个产品 READY。

已形成：动态已发布页面 → 匿名访问/资料提交 → 服务端定价和配置快照订单 → 统一支付证据确认 → 三类独立持久化任务 → 报告读取、老师入口、广告事件。付款后的报告不依赖加老师。真实数据库、生产前端包和浏览器均参与了本轮验证。

根据本轮最新授权新增 `jsj-h5`，未恢复已主动删除的 jsj-uni-app 或根 generator；原生 jsj-mini-app 未修改。没有引用 fmqx/zaq 源码或内部框架；独立继续开发不需要搬回旧仓库。没有根 Maven 聚合 POM，沿用 common/service → admin/mini 的构建方式。

## 实现范围与文件

表中的“实现”指源码能力，“验证”仅指本轮列明的验证，不代表第三方接收成功。

| 功能 | 主要文件（相对项目根） | 本轮范围与边界 |
| --- | --- | --- |
| 独立 H5 | jsj-h5/src/Landing.vue、Order.vue、api.ts | Vue3/TS/Vite；动态图片文案和表单、可选出生时间；支付等待、报告、独立老师入口；当前标签页 sessionStorage 仅存流程 ID/幂等键，不存访问令牌或资料正文 |
| 同源匿名访问 | jsj-mini/.../controller/H5Controller.java；jsj-service/.../acq/flow/FlowSecurity.java | HttpOnly/SameSite Cookie、数据库只存随机令牌哈希、域名绑定、POST Origin 与请求头校验；不改原移动端登录 |
| 配置解析 | jsj-service/.../acq/flow/H5CatalogService.java | 校验发布状态、域名、页面关联、商户授权和配置版本、模板及承接路由；指定商户仍受当前域名授权限制；多商户歧义拒绝静默选择 |
| 访问/点击/线索/订单 | jsj-service/.../acq/flow/H5FlowService.java、AdEventService.java | 锁匿名会话串行处理同一凭证请求；幂等键和请求摘要持久化；忽略前端金额/状态/商户事实；冻结商户、模板、企业、老师、入口、广告接口/事件映射；线索无需先下单即可登记提交事件 |
| 微信支付 | jsj-service/.../acq/flow/WechatPaymentAdapter.java、PaymentApplicationService.java | 官方 Java SDK H5/JSAPI 预支付、签名参数、签名响应查单、通知验签解密；平台调用在短事务之外；结果不明保留 UNKNOWN，不盲目再次下单 |
| 公众号 OAuth | jsj-service/.../acq/flow/OfficialAccountOAuthService.java | WxJava MP；state 哈希、绑定会话/订单/appId、10 分钟有效且单次消费；openid 绑定订单对应 appId；服务器控制返回地址 |
| 支付入账 | jsj-service/.../acq/flow/PaymentConfirmationService.java；jsj-mini/.../controller/PaymentNotificationController.java | 回调与查单共用锁和事务；核验金额、币种、商户、appId、交易号；支付证据与三类任务原子登记；查单不伪造通知 ID；退款后迟到成功通知不会恢复权益 |
| 持久化任务 | jsj-service/.../mapper/acq/BusinessFlowMapper.java、AdDeliveryMapper.java、WecomActionMapper.java；jsj-mini/.../config/*Worker.java | 报告、企微、广告任务独立；租约/版本条件完成；Mini 专属调度，默认关闭且必须指定 since；广告最多 5 次，未知结果不自动重发 |
| 报告 | 既有 acq/report/ReportGenerationServiceImpl.java；H5FlowService.report | 保留已实现模板版本与持久化逻辑；仅有效已支付所有者可读；没有出生时间显示“未提供”；纯文本渲染；不是正式测算算法 |
| 退款 | jsj-service/.../acq/flow/RefundApplicationService.java；jsj-admin/.../BusinessQueryController.java | 受权限和业务策略双重控制的全额退款/查单；稳定退款号，已有意图只查单；成功后保留报告但撤销读取；不支持部分退款或退款通知端点 |
| 企微入口 | jsj-service/.../acq/flow/WecomEntryJob.java、PostPaymentPreparationService.java | SELF_BUILT 联系我二维码及唯一 state；共享获客助手链接允许多订单显示；远程结果未知保留人工核对状态，不重复创建 |
| 企微回调/归属 | jsj-mini/.../controller/WecomCallbackController.java；jsj-service/.../acq/flow/WecomCallbackAdapter.java、CorpBoundWecomCrypt.java、WecomCallbackService.java | SDK 验签解密，补校验密文尾部接收企业；安全 XML 解析；企业/客户/老师三元关系，重复去重、按事件时间处理；只有精确 state 和老师匹配才关联，歧义保留未关联 |
| 备注/标签 | jsj-service/.../acq/flow/WecomAutomationService.java | 独立持久化动作；明确 EMPTY_ONLY 策略下仅填空备注；仅追加同企业有效标签；规则目前从本地配置读取，无后台规则编辑页面 |
| 三平台适配 | jsj-service/.../acq/flow/AdDeliveryAdapter.java | 腾讯 Marketing、百度 oCPC、快手 H5 接口；固定 HTTPS 主机、映射和金额单位校验，检查业务成功码，不以 HTTP200 作为成功；腾讯另一个 TENCENT_CONVERSION 模式仍明确阻断 |
| 后台 | jsj-admin/.../BusinessQueryController.java；jsj-service/.../mapper/acq/BusinessQueryMapper.java；jsj-admin-ui/src/views/acq/business/index.vue | 订单/线索/广告事件/汇总查询，列表分页；全额退款确认与查单按钮、后端权限、审计；查询响应不含资料正文、凭据或平台报文 |
| 配置增强 | ConfigCommands、LandingViews、LandingPageAdminServiceImpl、AdAccountAdminServiceImpl 及配置 UI | 页面可指定已授权商户/广告账户；广告支付/提交/加老师三类映射；前端 ID 字符串；保留版本发布保护及已有配置能力 |

`...` 展开分别是模块的 `src/main/java/com/zhs/service`、`src/main/java/com/zhs` 或 `src/main/java/com/zhs/web/controller/acq`；完整文件清单以本轮 Git diff 和新增目录为准。

## 数据库、菜单与配置

唯一连接目标：`106.55.61.223:3306/toufang`。使用现有 YAML 中已授权认证，通过 JDBC 连通；本机实际系统表也在 toufang。没有创建数据库、搬迁系统表、改变服务器授权或执行全量初始化。

- 已执行 `sql/acquisition_business_flow.sql`：新增 acq_h5_session、acq_h5_request、acq_pay_evidence、acq_oauth_state、wecom_customer_relation、acq_wecom_action 六张表；补订单/访问/线索绑定、支付尝试状态、广告事件与租约/快照字段；广告订单号允许空以支持提交事件；共享 link_id 改为普通索引。
- 保持 BIGINT AUTO_INCREMENT / IdType.AUTO。验收生成主键、再建关联和并发请求均成功；没有恢复雪花 ID。
- 修改 `sql/acquisition.sql` 同步原 27 张表的新字段，**未将该全量脚本导入现有库**。新增六表由增量脚本提供。
- 已执行 `sql/acq_menu_business_flow.sql`：补齐缺失的落地页/模板/域名菜单权限及业务查询菜单；共 27 条受保护插入。业务菜单 2009，新增权限子项 2901–2904。未给真实角色授权。
- 迁移前受影响表结构备份及执行辅助文件在被忽略的 `jsj-admin/target/business/`。DDL 自动提交；其他环境须重新比对真实字段和索引，不能盲目照搬执行。
- 发现 MySQL 会话原为 UTC、应用为 +08:00，造成租约时间判断不一致；现两个实际数据源都初始化 session time_zone=+08:00。未改历史时间字段，因此 Worker 开启前必须审核 since 截止点。

已将受跟踪公共 YAML 的明文数据库认证改为环境变量；实际可用值保存在已忽略的 `.local/application-business.yml` 和 `.local/application-business-admin.yml`。这些文件保留，可以继续本机开发。Admin 上传目录位于本机 `.local/uploads`，不假设旧电脑上传资源已迁移。

新增脱敏业务/Admin YAML 示例及 HTTPS 同源 Nginx 示例；README 与 config-examples/README 已更新。外部私钥、平台 Token 等只解析受限 `ref:alias`；支付 SDK/企微 SDK 的原始报文日志关闭，异常仅返回分类信息。

## 本轮实际验证

### 构建与离线测试

| 项目 | 实际结果 |
| --- | --- |
| service 离线白名单 | 53 项，0 失败、0 错误、0 跳过；最后包含跨企业密文校验修复 |
| admin ConfigurationPermissionTest | 4 项通过 |
| service install、admin package、mini package | 成功；使用当前 shared 产物，不依赖旧项目本地 JAR |
| Admin UI | 全量 vue-tsc 与 build:prod 通过；仍有既有大 chunk 提示，未以忽略类型或升级依赖规避 |
| H5 | 全量 vue-tsc 与 Vite 生产构建通过；新增自己的 package-lock.json |
| 禁用测试 | 未执行 AcquisitionConstraintJdbcTest；没有运行任意共享库全量测试 |

新增离线测试位于 `jsj-service/src/test/java/com/zhs/service/acq/flow/`。使用独立 RSA/AES 密钥生成可验签微信通知，验证篡改拒绝；企微验证官方 SDK 生成密文往返、错误签名及错误接收企业拒绝。三平台传输由测试替身返回契约响应，验证成功码、空响应 UNKNOWN 和金额换算，没有实际发送请求。

强化企微测试曾真实失败：WxJava 4.8.0 解密得到接收企业但未比较；本轮补充 `CorpBoundWecomCrypt` 使用 JCA 校验已验签密文尾部接收方，保留 SDK 验签及解密入口。修复后同组测试通过。构建失败没有通过删除断言或跳过验签处理。

### 指定真实数据库与 HTTP

使用 FLOW_2aceae7434f4 合成配置与专用临时后台账号，平台开关全关。辅助 Java/Python 脚本及日志在本机忽略目录 `jsj-admin/target/business/`，这些临时脚本不是自动运行的生产代码，也不属于提交后的默认测试入口。

已验证：

1. 已发布页面、HttpOnly 会话、访问和线索重复请求幂等；两线程相同下单请求只得到同一订单。
2. 前端篡改金额/支付状态不改变服务器 1990 分、未支付事实；未支付报告、跨会话订单、跨 Origin、幂等键内容变化均被拒绝。
3. 模拟已认证支付证据由隔离 Java 探针调用内部领域服务，无生产“标记支付成功”接口；金额不符回滚。
4. 在真实 Spring 事务中人为使任务后续事件登记抛异常，支付状态、任务及证据整体回滚；随后两线程合法证据只有一个入账赢家。
5. 查单 notifyId 为 NULL，随后通知幂等；每单准确三类独立持久化任务。任务在前一进程退出后仍可由新进程查询和处理；旧租约版本不能完成任务。
6. 真正持久化报告包含可选时间“未提供”，无需添加老师即可读取；静态老师入口独立准备。
7. 企微回调领域逻辑真实落库：重复事件去重、同客户两名老师、删除一名老师保留另一关系、跨企业拒绝、无精确 state 的事件不误关联；最后一个关系删除后客户状态变更。
8. 真实后台登录成功，四类查询允许，未获退款权限账号调用退款返回 403。

以下没有伪称数据库实测通过：真实退款全流程、所有配置同时切换的端到端矩阵、所有乱序企微事件组合、广告发送结果未知后的运营对账流程。相关部分只有领域代码、离线测试或仍未实现的操作入口。

### 浏览器

独立 Chrome/CDP，后端 Mini 63400、H5 63401、CDP 63402、Admin 63403、后台静态代理 63404；没有占用或停止原有 8881/18881 服务。

- H5 在 390px 与 1440px 无横向溢出；真实填写姓名/性别/日期，出生时间留空。
- 双击提交、订单刷新、重新返回落地页均恢复同一订单；测试页面无 JS 异常。
- 内部测试证据确认并持久化生成后，浏览器显示报告，不要求先加老师。
- 后台通过真实令牌进入新增菜单，订单表和统计可读；关闭临时账号首次登录提示后操作；只读账号不显示退款按钮，HTTP 仍做独立权限检查。
- 未运行第二浏览器引擎；没有将普通 Chrome 写成微信实机 JSAPI/H5 支付通过。
- 页面不存在/停用/配置不足、支付取消、OAuth 授权拒绝、全部异步失败状态具有代码分支，但本轮没有完成每个分支的浏览器矩阵；应列为上线前补验。

截图： [H5 移动页](evidence/business-flow/h5-mobile.png)、[持久化报告](evidence/business-flow/h5-report.png)、[后台查询](evidence/business-flow/admin-business.png)。均为合成测试资料；后台截图截在首次登录提示关闭动画期间，后续统计切换与权限断言已通过。

## 真实平台边界和未完成代码

**本轮真实支付、退款、公众号授权、企微联系人操作、广告外发均未执行。** HTTP mock 成功、官方 SDK 验签成功和数据库持久化成功不能代替平台联调。

| 平台/能力 | 已实现 | 仍需提供或继续实现 |
| --- | --- | --- |
| 微信支付 | H5/JSAPI、查单、通知验签统一入账、全额退款/查单 | 有效商户/appId 绑定、产品权限、私钥/APIv3/平台验签引用、授权域与 HTTPS 通知环境、微信实机；UNKNOWN 支付尝试的运营对账及安全重新发起入口尚未实现 |
| 公众号 | OAuth state/会话/appId 绑定及 JSAPI 参数 | 网页授权能力、Secret 引用、域名和微信内验证 |
| 退款 | 显式 FULL_REVOKE 的全额模型；迟到支付通知不复活权益 | 部分退款权益政策未确定，未实现；退款通知端点未实现，当前靠受控查单确认；不应开启未经业务确认的策略 |
| 企微联系我 | SELF_BUILT 二维码、state、验签与接收方校验、多老师关系、备注/标签动作 | 企业客户联系权限、可见老师、回调 Token/AES、真实 state 样本、实际备注/标签权限；外部结果未知的运营核对入口尚未实现 |
| 获客助手 | 共享授权链接展示，歧义事件保留未关联 | customer_channel 等账户特定能力及可唯一关联标识未接入，不能将共享 link_id 当订单唯一键 |
| 腾讯 | TENCENT_MARKETING user_actions/add，稳定 external_action_id | Token、账户/数据源、实际事件和金额口径确认；TENCENT_CONVERSION 另一协议模式尚未实现，明确返回 PLATFORM_PROTOCOL_NOT_VERIFIED |
| 百度/快手 | oCPC uploadConvertData / H5 activate，固定主机和业务码判断 | 账户资格、百度 Token、真实点击参数、事件映射、金额单位及平台接收验证 |
| 后台运营 | 配置页、订单/线索/事件/汇总、退款权限确认 | 综合任务状态和 UNKNOWN 人工对账/重试操作页、细筛选和更多运营指标未完成；备注标签规则仍在本地配置 |
| 正式报告 | 版本保护、快照、模板纯文本和持久化读取 | 正式内容、算法、结论审核与展示规则仍需业务提供，测试文本不作为正式测算结果 |

浏览器订单恢复依赖当前标签页 sessionStorage，读取权限依赖 HttpOnly Cookie；清空 Cookie、换设备或跨域不会自动恢复访问，没有另设跨设备找回机制。备注 EMPTY_ONLY 是读取后再写入，企微 API 无本地事务 CAS；人工同时编辑时仍有竞态，启用前需确认运营规则。

协议参考：[微信支付 Java SDK](https://github.com/wechatpay-apiv3/wechatpay-java)、[WxJava](https://github.com/binarywang/WxJava)、[腾讯官方 Marketing SDK](https://github.com/tencentad/marketing-api-go-sdk)、[百度 oCPC 文档](https://ocpx.baidu.com/developer/ocpc-doc/api/api-doc/api-interface/)、[快手 H5 回传文档](https://docs.qingque.cn/d/home/eZQCZzmIhUFUUpOOBlfg5kbYI?identityId=1nEnN8wgrHn)。SDK/文档并不能证明当前账号具有对应产品权限。

## 清理、开关与后续操作

本轮精确清理已提交：4 笔合成订单、4 条支付、4 份报告、12 条支付后任务、8 条广告事件、6 条支付证据、4 条线索/访问、6 个匿名会话及其幂等记录；企微合成事件/老师关系/联系人、11 条配置记录、临时用户/角色及5条权限关系均已删除。删除前校验精确 ID、配置 tag、商户、企业、域名和账号 create_by；没有模糊匹配真实订单。保留增量表结构、菜单和真实数据。

临时登录通过正常 logout 撤销；临时密码/令牌/验证码文件已删除。本轮自启的 Java/Node 实例和专用 Chrome 已关闭，63400–63404 不再监听。本地有用 YAML 与正常构建产物、测试日志保留；没有关闭用户原有服务。

所有支付/OAuth/退款、企微回调/外发及 Worker 开关最终默认关闭，未在本地配置打开；Admin 验收 Quartz 使用内存且禁自动启动。任何 Worker 开启都要检查 since，不能自动补发历史转化。

仍需用户处理：轮换之前在聊天或仓库中出现过的数据库凭据，更新本地配置；评估使用最小权限应用账号和 Redis 网络访问范围。本轮没有擅自改远程密码、账户授权、网络规则或重写 Git 历史。

下一步应优先补运营对账入口和未覆盖浏览器/配置切换矩阵，再按已授权账号逐平台联调。现有源码、本机依赖和本地配置足以独立继续开发；新功能和真实平台验证未完成不意味着必须搬回旧系统。

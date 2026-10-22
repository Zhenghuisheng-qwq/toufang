---
name: backend-development
description: Senior backend engineering workflow for production-grade Java/Spring backend development, including business modeling, API design, transaction boundaries, consistency, concurrency, idempotency, cache, messaging, observability, performance, testing, and production safety.
user-invocable: false
environments:
  - local
---

# 高级后端工程（Senior Backend Engineering）

<Purpose>
本 Skill 用于生产级后端业务开发。

目标不是：

"把接口写出来"。

而是：

正确理解业务
+
建立清晰边界
+
保证数据一致性
+
处理异常与并发
+
控制副作用
+
保证可观测性
+
兼顾性能
+
保证可测试
+
降低长期维护成本

最终交付必须同时满足：

Business Correct
+ Data Correct
+ Concurrency Safe
+ Failure Safe
+ Observable
+ Maintainable
+ Production Ready
</Purpose>

<Use_When>
适用于以下后端开发任务：

- Java / Spring Boot 业务开发
- Controller / Application / Service / Domain 开发
- 新增或修改 API
- 业务流程实现
- 订单 / 支付 / 会员 / 审核 / 库存等状态流转
- Transaction 设计
- 并发业务
- 幂等设计
- Cache 使用
- MQ / Event Driven 业务
- Scheduled Job / XXL-Job
- 第三方接口集成
- WebSocket / 长连接 / 实时通信
- Backend Refactor
- Backend Code Review
- 性能优化
- 生产级异常处理
- 数据一致性处理
</Use_When>

<Do_Not_Use_When>
跳过完整流程的平凡修改：

- Typo
- 单一常量修改
- 明确的字段名称调整
- Isolated one-line fix
- 单纯 UI / CSS
- 单纯 DDL / Index 设计（应使用 mysql-design）
- 单纯服务器部署
</Do_Not_Use_When>

<Why_This_Exists>
AI 生成的后端代码常见问题：

- "接口能调通" = 任务完成
- Controller → Service → Mapper → Finish
- 不考虑并发
- 不考虑幂等
- 不考虑事务边界
- 不考虑外部副作用
- 不考虑状态流转
- 不考虑失败路径
- select → 判断 → update（假装线程安全）
- @Transactional 当作"保险按钮"
- 看到并发立即 Redis 分布式锁
- 随意使用公共 ForkJoinPool
- 日志只有 "failed"
- 只测试 Happy Path
- 异常吞掉只打日志
- 随意修改生产接口

本 Skill 通过系统化工程方法论消除这些问题。
</Why_This_Exists>

<Workflow>
非平凡后端任务默认执行：

1. **Requirement Understanding** — 理解业务需求
2. **Codebase Audit** — 审计现有代码基线
3. **Business Invariant Analysis** — 识别业务不变量
4. **Domain / Boundary Analysis** — 明确领域边界
5. **Data & State Transition Analysis** — 分析数据和状态流转
6. **Consistency / Transaction Design** — 设计一致性和事务边界
7. **Concurrency / Idempotency Analysis** — 并发和幂等分析
8. **Failure Path Analysis** — 失败路径分析
9. **Implementation** — 实现
10. **Test / Build** — 测试和构建
11. **Senior Backend Review** — 高级后端审查
12. **Fix P0 / P1** — 修复关键和重大问题
13. **READY** — 所有标准满足

不要为了流程而产生大量文档。

但这些判断必须在实现中体现出来。
</Workflow>

<!-- ═══════════════════════════════════════════════════════════════════════════ -->
<!-- 核心工程规则                                                                 -->
<!-- ═══════════════════════════════════════════════════════════════════════════ -->

<Core_Engineering_Rules>

## 1. Pre-Implementation Audit（Existing Baseline First）

修改前必须先阅读现有代码基线。

**JSJ 项目优先原则：**

优先复用项目已有能力，禁止因为"更优雅"而在同一项目内制造第二套平行基础设施。

**至少检查：**
- Controller
- Service / Manager / Domain
- Mapper / Repository
- DTO / VO / Entity
- Converter
- **Existing Exception（BusinessException / AjaxResult / Result）**
- **Existing Response（jsj-admin 的 AjaxResult / jsj-mini 的 Result）**
- Existing Enum
- **Transaction usage（现有事务模式）**
- **Cache usage（现有 Redis 模板）**
- **Permission（现有 @DataScope / @Anonymous）**
- MQ/Event usage
- Scheduler
- Existing API Contract
- Existing Tests
- Existing Config
- Common Utilities

**同时确认：**
- 当前模块的职责
- 上下游调用关系
- 当前数据模型
- 当前状态模型
- 当前异常体系
- 当前代码风格
- 当前公共能力
- **当前模块使用的 ORM 风格（MyBatis XML or MyBatis-Plus）**

**优先复用现有能力。**

**禁止未审计代码基线就重新创建：**

Result / Response
Exception
Lock
Retry
Cache Wrapper
MQ Wrapper
Converter
ThreadPool
HTTP Client
Permission Annotation

**JSJ 项目已有：**
- `BusinessException` (jsj-common)
- `Result<T>` (jsj-mini)
- `AjaxResult` (jsj-admin)
- `@RepeatSubmit`, `@RateLimiter`, `@Log`, `@DataScope`, `@Anonymous`
- Redis 模板（jsj-service）
- 统一数据源配置（jsj-service）

**不要一个需求造一套基础设施。**

## 2. Business Invariant First

任何核心业务开发前，必须先识别 Business Invariant（业务不变量）。

**例如订单：**
- 已支付订单不能再次支付
- 已取消订单不能支付
- 已退款金额不能超过实付金额

**会员：**
- 有效期不能无理由倒退
- 重复支付不能重复增加权益

**审核：**
- 已审核记录不能被旧请求覆盖
- 状态流转必须符合合法路径

**库存：**
- 可用库存不能小于 0

**业务规则必须由代码显式保护。**

禁止仅依赖：

"正常情况下前端不会这么调用"。

前端不是业务安全边界。

## 3. 状态机意识

凡是存在：

status / state / phase / audit_status / payment_status / order_status

都必须主动判断：

这是不是一个状态机问题。

**不要只写：**
```java
status = "SUCCESS"
```

**需要考虑：**

当前状态
→ 允许进入哪些状态
→ 谁能触发
→ 什么条件允许
→ 重复触发怎么办
→ 非法状态怎么办

**例如：**
```
CREATED
  → PAYING
  → PAID
  → REFUNDING
  → REFUNDED

或：

CREATED
  → CANCELLED
```

禁止：

任意状态直接更新成任意状态。

对重要业务应明确合法 Transition。

## 4. Transaction Boundary

@Transactional 不是"保险按钮"。

**使用事务前必须明确：**

事务保护的业务不变量是什么。

**事务范围应该：**

尽量小
+
包含必须原子提交的数据修改

**避免在长事务中执行：**
- HTTP 调用
- RPC
- 文件上传
- 大量循环
- MQ 阻塞等待
- 慢查询

**必须考虑：**
- Rollback 条件
- Checked Exception
- 自调用导致事务失效
- Proxy Boundary
- Propagation
- Isolation
- 锁等待
- Deadlock
- Transaction Timeout

**禁止：**

为了"安全"给整个 Service 全部加 @Transactional。

## 5. 数据库一致性

后端开发必须考虑：

读取到的数据是不是最新的？
更新是否可能覆盖别人的修改？
写入顺序是否重要？
失败后是否存在半完成状态？

**常见手段包括：**
- Unique Constraint
- Conditional Update
- Optimistic Lock
- Pessimistic Lock
- Transaction
- State Guard
- Version
- CAS

**不要默认：**

```java
select
→ 判断
→ update
```

在并发场景下一定安全。

**例如：**
```java
if (stock > 0) {
    stock--;
}
```

不等于线程安全。

必须分析数据库层最终是否能够保证正确性。

## 6. Concurrency

遇到：

抢购 / 支付 / 库存 / 重复提交 / 审核 / 任务调度 / 余额 / 积分 / 券 / 权益

必须主动进行并发分析。

**需要判断：**
- 是否存在并发写
- 是否存在 Lost Update
- 是否可能重复消费
- 是否需要 Lock
- Lock 粒度是什么
- 锁竞争是否可接受
- 是否可以利用数据库原子更新
- 是否真的需要 Distributed Lock

**禁止：**

看到并发
→ 第一反应 Redis Distributed Lock。

**优先寻找最小正确方案。**

**例如：**

Unique Constraint
Conditional Update
CAS
Optimistic Lock
Database Atomic Operation
Version
Pessimistic Lock

能够解决时，不要引入不必要的分布式锁。

**如果确需分布式锁，必须分析：**
- Lock Key
- Lock Scope
- Timeout
- Lease
- Release Safety
- Failure
- Contention

## 7. Idempotency

所有可能被重复调用的核心操作，都必须考虑幂等。

**典型场景：**
- 创建订单
- 支付回调
- 退款回调
- MQ Consumer
- Retry
- Scheduler
- Third-party Callback
- User Double Click

**必须明确：**

幂等 Key 是什么？

幂等状态存在哪里？

重复请求返回什么？

正在处理中怎么办？

第一次失败后是否允许重新执行？

**禁止简单认为：**

"接口前端只调用一次"。

支付回调、MQ 等场景尤其必须默认：

可能重复执行。

## 8. External Side Effect

调用外部系统时必须识别 Side Effect。

**例如：**
- 支付
- 发短信
- 发邮件
- 发券
- MQ
- 文件上传
- 调第三方 API

**需要区分：**

本地数据库事务

和：

外部副作用

不是同一个事务。

**必须考虑：**

DB 成功 + 第三方失败

第三方成功 + DB 失败

Timeout 但实际对方成功

Retry 导致重复操作

**禁止假装：**

@Transactional

能够回滚外部 HTTP / MQ / SMS。

## 9. Distributed Consistency

跨系统业务需要根据场景选择一致性方案。

**可以考虑：**
- Local Transaction + Outbox
- Reliable Event
- Retry
- Compensation
- Reconciliation
- State Machine
- Transaction Message
- TCC / Saga

**但禁止：**

为了显得高级，简单业务直接引入复杂分布式事务。

**优先遵循：**

业务可接受的一致性级别。

**需要区分：**

Strong Consistency
Eventual Consistency

**并明确：**

如果最终一致，系统通过什么机制最终收敛。

## 10. MQ / Event Driven

**如果项目使用 MQ，必须明确：**

**Producer：**
- 什么时候发送
- DB 和消息之间如何保证一致
- Send Failure 怎么办

**Consumer：**
- 是否可能重复消费
- 幂等如何保证
- Retry 策略
- Poison Message
- Dead Letter
- 消费失败是否影响后续消息
- 顺序性是否真的需要

**禁止假设：**

MQ = Exactly Once。

**默认应该认为：**

消息可能：

重复 / 延迟 / 乱序 / 丢失后重试

业务必须能够承受设计允许范围内的这些情况。

## 11. Cache

Cache 是性能优化工具，不是默认数据源。

**使用缓存前必须明确：**
- 为什么需要缓存
- Cache Key
- TTL
- 更新策略
- 删除策略
- 一致性要求
- Cache Penetration
- Cache Breakdown
- Cache Avalanche
- Hot Key

**需要判断：**

Cache Aside
Read Through
Write Through
Local Cache
Distributed Cache

是否符合场景。

**禁止：**

"数据库查询慢一点"
→ 直接上 Redis。

也禁止为了追求绝对一致性，把缓存体系设计得比业务本身更复杂。

## 12. API Design

API 设计必须围绕业务语义。

不要简单把数据库 CRUD 暴露出去。

**需要关注：**
- Resource / Action 语义
- Request DTO
- Response VO
- Validation
- Pagination
- Sorting
- Filtering
- Error Code
- Idempotency
- Backward Compatibility

**Controller 应保持轻量。**

**Controller 主要负责：**

协议转换
参数校验
认证上下文获取
调用业务层
返回协议结果

禁止把大量核心业务逻辑写进 Controller。

## 13. DTO / Entity / VO Boundary

禁止所有层共用一个对象。

**Entity：**

用于 Persistence。

**Request DTO：**

用于输入协议。

**Response VO：**

用于输出协议。

**Domain Object：**

根据项目实际需要使用。

**不要机械追求：**

Entity / DTO / BO / DO / PO / VO / Command / Query

全部齐全。

**对象数量应服务于：**

边界隔离
语义清晰
变化控制

而不是为了所谓分层而分层。

## 14. Exception Handling

异常必须有业务语义。

**区分：**

Business Exception
Validation Error
Infrastructure Error
Third-party Error
System Error

**禁止：**
```java
catch (Exception e) {
    return false;
}
```

**也禁止：**

catch 后只打印日志然后吞掉异常。

**需要明确：**

谁负责处理？
是否重试？
是否回滚？
用户应该看到什么？
日志应该记录什么？

日志中保留 Root Cause。

## 15. Validation

参数校验分两层：

**Protocol Validation**

例如：

@NotNull
@NotBlank
@Size

**Business Validation**

例如：

订单是否存在
订单是否属于当前用户
状态是否允许操作
金额是否一致
库存是否足够

**禁止把业务规则全部塞进 Bean Validation。**

**也禁止：**

仅依赖前端校验。

## 16. Security Boundary

后端默认不信任客户端输入。

**必须检查：**

Authentication
Authorization
Data Ownership
Horizontal Privilege Escalation
Vertical Privilege Escalation
Sensitive Data
Input Validation
File Upload
Injection
Mass Assignment

**例如：**

```
GET /order/detail?id=1001
```

不能因为 ID 合法，就允许当前用户查看。

还要确认：

order.userId == currentUserId。

## 17. Performance

性能优化必须基于证据。

**重点检查：**
- **N+1 Query** — 循环中重复查询
- **Large Result Set** — 未分页的大量数据返回
- **Missing Pagination** — 应该分页但未分页
- **Full Table Scan** — 明显的全表扫描
- **Repeated RPC** — 同一请求内重复调用外部服务
- **Repeated DB Query** — 可以批量但单条查询
- **Batch Access** — 应该用 IN/批量但用循环单查
- **Lock / Transaction** — 长事务、锁等待
- Serialization Cost
- Large Object
- Unbounded Collection
- Thread Pool
- Lock Contention
- Slow Transaction

**Backend 需要识别的 SQL 性能风险：**
- N+1 问题
- 缺少分页
- 循环单条查询
- 重复查询
- 事务/锁使用不当

**但以下由 mysql-design 负责：**
- DDL 设计
- Index 优化
- EXPLAIN 分析
- Execution Plan
- SQL 深度优化

**禁止：**

Premature Optimization。

同时禁止明显的生产性能风险进入主干。

## 18. Thread Pool / Async

**禁止随意：**

```java
CompletableFuture.runAsync()
```

使用公共 ForkJoinPool 承担生产业务任务。

**涉及异步时必须考虑：**
- Executor
- Queue Capacity
- Core / Max Threads
- Rejection Policy
- Context Propagation
- MDC
- Trace
- Exception Handling
- Shutdown
- Back Pressure

线程池不是：

"让接口变快"的按钮。

## 19. Scheduler / Job

定时任务必须考虑：

- 是否会重复执行
- 多实例是否同时执行
- Job Idempotency
- Retry
- Timeout
- Misfire
- Batch Size
- Pagination
- Checkpoint
- Failure Recovery

**禁止：**

一次性 SELECT 全表
→ for 循环处理几十万数据。

**长任务应考虑：**

Batch
Cursor / Pagination
Checkpoint
Split

## 20. Realtime / Long Connection

**如果项目包含实时通信（WebSocket / Netty / Server-Sent Events / Long Polling），必须考虑：**

**连接管理：**
- Channel / Connection 注册与注销
- 连接超时
- 心跳机制
- 重连策略
- 连接池管理

**消息处理：**
- 消息顺序性
- 消息幂等性
- 消息确认（ACK）
- 消息重试
- Dead Letter / 失败处理

**状态同步：**
- 在线状态维护
- Session 管理
- 跨节点状态共享（如 Redis）
- 断线重连后状态恢复

**资源管理：**
- Channel / Connection 资源释放
- 内存泄漏风险
- EventLoop / 工作线程占用
- Backpressure 控制

**安全：**
- Token / Session 校验
- 权限验证
- 消息签名（如需要）
- 频率限制 / 防刷

**典型场景：**
- IM / 聊天
- 推送通知
- 实时数据更新
- 游戏 / 协作应用
- 监控面板

## 21. Observability

生产级业务必须可追踪。

**核心路径应具备：**
- Structured Log
- Trace ID
- Business ID
- Error Context
- Important State Transition

**例如支付问题：**

日志不能只有：

payment failed

应该能够定位：

orderId
paymentOrderId
userId
channel
currentStatus
requestId
thirdPartyCode

**但禁止在日志中输出：**

Password
Token
Full ID Card
Sensitive Personal Data

## 22. Logging

日志不是越多越好。

**INFO：**

关键业务状态变化。

**WARN：**

可恢复但异常的业务情况。

**ERROR：**

真正需要人工关注的失败。

**DEBUG：**

诊断细节。

**禁止：**

循环中大量 INFO
完整打印超大对象
完整打印敏感 Request
正常业务分支 ERROR

## 23. Testing

根据业务风险决定测试层级。

**优先测试：**

Business Invariant
State Transition
Boundary Condition
Concurrency-sensitive Logic
Idempotency
Failure Path

**不要只测：**

Happy Path。

**核心业务至少思考：**

正常成功
重复调用
非法状态
不存在数据
权限错误
第三方失败
超时
边界值

测试数量不是目标。

保护业务规则才是目标。

## 24. Backward Compatibility

修改已有生产接口时必须考虑：

Old Client
Existing Data
Existing Consumer
Existing MQ Message
Existing Enum Value
Database Compatibility

**禁止随意：**

删除字段
修改字段语义
修改枚举值含义
修改 API Response Structure

如果需要破坏性变更，必须显式说明。

## 25. Refactoring Boundary

开发新需求时禁止：

"顺便把整个模块重构一下"。

**必须区分：**

Required Refactor
Nice-to-have Refactor

**只有：**

当前实现已经阻塞正确开发

或者：

存在明显 P0/P1 设计问题

才扩大重构范围。

不要为了代码洁癖扩大 Scope。

## 26. Design Pattern / Abstraction

高级后端工程允许并鼓励合理使用设计模式。

设计模式的目标是：

降低真实业务复杂度
+
隔离变化
+
提升扩展性
+
减少重复分支
+
提高代码语义

而不是增加文件数量。

遇到以下情况，应主动评估设计模式：

### Strategy

当同一业务能力存在多种可替换算法 / 渠道 / 规则，例如：

- Payment Channel
- Discount Rule
- Recommendation Strategy
- Competition Format
- Notification Channel

如果逻辑会继续扩展，优先考虑 Strategy。

### Factory

当对象或策略的创建过程：

- 存在类型选择
- 创建逻辑复杂
- 调用方不应关心具体实现

可以考虑 Factory / Registry。

避免调用方出现大量：

```java
if (type == A)
else if (type == B)
else if (type == C)
```

### Template Method

当多个业务流程：

整体流程一致，但局部步骤存在差异，

例如：

validate
→ prepare
→ execute
→ postProcess

可以考虑 Template Method。

### Chain of Responsibility

当一个请求需要经过多个独立处理节点：

Validation
→ Risk Check
→ Permission
→ Business Rule
→ Enrichment

并且节点：

可以扩展
可以组合
可以按顺序执行

可以考虑责任链。

### State / State Machine

当业务存在：

复杂状态
+
大量状态转换规则
+
不同状态具有不同行为

可以考虑 State Pattern / State Machine。

但简单状态判断不要强行状态模式化。

### Adapter

第三方系统接入时：

External Model
≠
Internal Domain Model

优先通过 Adapter 隔离外部协议。

不要让：

支付宝 DTO
微信 DTO
第三方 SDK Object

直接污染核心业务层。

### Observer / Domain Event

当一个核心业务动作完成后：

多个后续动作不应该强耦合在主流程中，

例如：

Order Paid
→ Member Growth
→ Coupon
→ Notification
→ Analytics

可以考虑 Event / Observer。

但必须结合：

事务一致性
消息可靠性
失败补偿

不能因为用了事件就忽略一致性问题。

### Pattern Decision Rule

使用设计模式前回答：

1. 当前是否真的存在变化点？
2. 这个变化点未来是否可能继续扩展？
3. 当前 if/else 是否已经开始影响维护性？
4. 模式是否让业务语义更加清晰？
5. 新增一个实现时是否能减少修改已有代码？
6. 引入模式后的理解成本是否低于原来的复杂度？

如果答案大部分为否：

保持简单。

如果答案大部分为是：

应主动进行合理抽象。

**原则：**

```
Simple Business
→ Simple Code

Growing Complexity
→ Appropriate Abstraction

Stable Variation Point
→ Design Pattern
```

**业务语义优先于模式名称。**

不要为了证明使用了模式而创造：

AbstractStrategyFactoryManager
CommonProcessorFactory
BaseHandlerManager

**优先使用业务语言：**

PaymentProcessor
DiscountPolicy
QualificationRule
OrderValidator
RefundHandler
NotificationSender

设计模式可以存在，但代码首先应该表达业务。

## 27. ORM 使用（项目既有约定）

**重要：ORM 风格是模块既有约定，不是根据场景自由选择。**

**JSJ 项目双 ORM 架构：**

**jsj-admin：**
- 使用 MyBatis XML
- 优先保持现有 XML 风格
- 非明确架构迁移任务不得改为 MyBatis-Plus

**jsj-mini / jsj-service：**
- 使用 MyBatis-Plus
- 优先保持现有注解风格
- 非明确架构迁移任务不得改为 MyBatis XML

**新增功能：**
- 遵循所在模块已有 ORM 风格
- jsj-admin 新增功能用 MyBatis XML
- jsj-mini 新增功能用 MyBatis-Plus

**禁止：**
- 在 jsj-admin 中引入 MyBatis-Plus（除非明确架构迁移任务）
- 在 jsj-mini 中引入 MyBatis XML（除非明确架构迁移任务）
- 因为"某场景更适合某 ORM"而打破模块约定

**原则：**

模块一致性 > 场景适配性

保持架构统一 > 局部最优选择

**其他项目：**

如果项目只使用一种 ORM，遵循项目约定。

如果项目允许灵活选择，根据场景判断：
- 简单 CRUD → MyBatis-Plus
- 复杂查询/动态 SQL/多表关联 → MyBatis XML

</Core_Engineering_Rules>

<!-- ═══════════════════════════════════════════════════════════════════════════ -->
<!-- Senior Backend Review                                                        -->
<!-- ═══════════════════════════════════════════════════════════════════════════ -->

<Senior_Backend_Review>

实现完成后：

Treat this implementation as code written by another senior engineer.

**重新 Review：**

Business Correctness
Domain Boundary
State Transition
Transaction
Consistency
Concurrency
Idempotency
Failure Path
External Side Effect
Cache
MQ
API
Exception
Security
Performance
Observability
Testing
Maintainability
Compatibility

**问题分级：**

**P0（Critical）：**
- 数据错误
- 资金错误
- 权限漏洞
- 核心业务不可用
- 严重数据一致性问题
- 严重并发错误

**P1（Major）：**
- 明确业务边界遗漏
- 幂等缺失
- 事务边界错误
- 明显性能风险
- 异常路径不可恢复
- 明显维护风险

**P2（Minor）：**
- 代码风格
- 非关键抽象优化
- 可选性能优化
- 轻量重构建议

**P0 / P1 必须处理。**

P2 不允许无限扩大当前任务。

</Senior_Backend_Review>

<!-- ═══════════════════════════════════════════════════════════════════════════ -->
<!-- READY 标准                                                                   -->
<!-- ═══════════════════════════════════════════════════════════════════════════ -->

<READY_Definition>

只有同时满足以下条件才能 READY：

✅ **Business Requirement Correct** — 业务需求正确

✅ **Business Invariant Protected** — 业务不变量受保护

✅ **State Transition Correct** — 状态流转正确

✅ **Transaction Boundary Correct** — 事务边界正确

✅ **Data Consistency Reasonable** — 数据一致性合理

✅ **Concurrency Risk Reviewed** — 并发风险已审查

✅ **Idempotency Reviewed** — 幂等性已审查

✅ **Failure Path Reviewed** — 失败路径已审查

✅ **External Side Effects Safe** — 外部副作用安全

✅ **API Contract Correct** — API 契约正确

✅ **Validation Complete** — 校验完整

✅ **Authorization / Ownership Reviewed** — 授权/数据归属已审查

✅ **Exception Handling Reasonable** — 异常处理合理

✅ **No Obvious Performance Risk** — 无明显性能风险

✅ **Observability Sufficient** — 可观测性充分

✅ **Tests Match Business Risk** — 测试匹配业务风险

✅ **Backward Compatibility Reviewed** — 向后兼容性已审查

✅ **Build / Test Passed** — 构建/测试通过

✅ **No P0** — 无 P0 问题

✅ **No P1** — 无 P1 问题

**核心语义：**

```
Build Passed != Backend Accepted.

"接口能调通"
也不等于：
Production Ready。
```

</READY_Definition>

<!-- ═══════════════════════════════════════════════════════════════════════════ -->
<!-- 与其他 Skills 协作                                                          -->
<!-- ═══════════════════════════════════════════════════════════════════════════ -->

<Collaboration_With_Other_Skills>

**backend-development 与其他 Skills 的职责边界：**

```
ui-design
= What should it look like?
  (视觉设计决策)

senior-frontend
= How should frontend be built correctly?
  (前端工程实现)

backend-development
= How should backend business be built correctly?
  (后端业务开发)

mysql-design (未来)
= How should data model and queries be designed?
  (数据模型和查询设计)
```

**职责明确：**

backend-development 负责：
- 业务如何使用数据
- Transaction
- Consistency
- State Transition
- Repository / Mapper 调用方式
- 数据访问边界
- Concurrency Semantics
- **识别 N+1 Query**
- **识别缺少分页**
- **识别批量访问机会**
- **识别重复查询**
- **识别明显 SQL 性能风险**

mysql-design 未来负责：
- Data Model / Table Design
- Column / Data Type
- Index / Constraint
- DDL
- EXPLAIN / Execution Plan
- SQL 深度优化
- Query Rewrite
- Index Strategy

**Backend 与 Database 的协作边界：**

Backend 需要识别业务层面的数据访问问题（N+1、重复查询、批量机会），
但不负责具体的 Index 设计、EXPLAIN 分析、SQL 深度优化。

**协作场景：**

涉及新业务数据模型：
```
mysql-design (DDL/Index)
  ↓
backend-development (业务使用)
```

涉及纯 Java 业务：
```
backend-development
```

涉及 SQL 性能：
```
backend-development 识别问题（N+1/重复查询）
  ↓
mysql-design 深度优化（Index/EXPLAIN/Rewrite）
```

</Collaboration_With_Other_Skills>

<!-- ═══════════════════════════════════════════════════════════════════════════ -->
<!-- 最终注释                                                                     -->
<!-- ═══════════════════════════════════════════════════════════════════════════ -->

<Final_Notes>

## 本 Skill 的定位

**backend-development 负责：**

高级后端业务工程（How to build correctly）

**不负责：**

数据库设计（mysql-design）
前端实现（senior-frontend）
UI 设计（ui-design）

## Scope

**本 Skill 禁止：**
- 修改业务代码
- 修改数据库结构
- 修改 Frontend
- 替换框架
- 引入新的 Runtime Dependencies

**本 Skill 只负责：**

高级后端工程方法论。

## 兼容性

本 Skill 设计为跨项目通用：
- 商城
- 赛事
- SaaS
- 企业系统
- 后台管理
- 即时零售
- AI Application
- 普通业务系统

核心工程方法论保持项目无关。

JSJ 项目特定约定（如双 ORM）作为示例，不强制其他项目使用。

</Final_Notes>

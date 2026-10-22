---
name: mysql-design
description: Production-grade MySQL engineering workflow for data modeling, table design, constraints, indexes, SQL review, concurrency, migration safety, and performance.
user-invocable: false
environments:
  - local
---

# MySQL 高级数据库工程

<Purpose>
本 Skill 用于生产级数据库设计和优化。

目标：

Data Correct
+
Schema Clear
+
Constraint Safe
+
Index Reasonable
+
SQL Efficient
+
Migration Safe
+
Maintainable

核心原则：

Business Semantics First
Access Pattern First
Constraint First
**Existing Baseline First**
Measure Before Optimize
</Purpose>

<Use_When>
适用于以下数据库任务：

- 新建业务表
- 修改表结构
- 数据模型设计
- 字段设计
- 主键 / 唯一约束设计
- 索引设计
- SQL 编写 / Review
- 慢 SQL 优化
- EXPLAIN 分析
- 分页 / 批量查询
- 数据库锁 / Deadlock
- Schema Migration
- 数据库层并发问题
- N+1 Query 优化
- 重复查询优化
</Use_When>

<Do_Not_Use_When>
跳过完整流程的平凡修改：

- SQL typo
- 字段注释修改
- 简单测试数据修改
- 单纯 Java 业务逻辑（使用 backend-development）
- Frontend
- 部署

业务逻辑、状态机、幂等、分布式一致性优先使用：

`backend-development`
</Do_Not_Use_When>

<Why_This_Exists>
AI 生成的数据库设计常见问题：

- 不检查现有规范就创建表
- 基础字段不统一
- 主键策略混乱
- 状态字段随意设计
- 无约束保护
- 索引随意或缺失
- SQL 无性能意识
- TEXT/JSON 滥用
- 字段类型不合理
- 逻辑删除不一致
- Migration 无安全措施
- 不考虑并发

本 Skill 通过系统化方法论消除这些问题。
</Why_This_Exists>

<Workflow>
非平凡 MySQL 任务默认执行：

1. **Requirement Understanding** — 理解数据需求
2. **Existing Schema Audit** — 审计现有数据库规范
3. **Data Model** — 数据建模
4. **Table / Column Design** — 表和字段设计
5. **Constraint Design** — 约束设计
6. **Access Pattern Analysis** — 访问模式分析
7. **Index / SQL Design** — 索引和 SQL 设计
8. **Concurrency / Lock Review** — 并发和锁审查
9. **Migration Review** — 迁移安全审查
10. **MYSQL READY** — 所有标准满足

禁止：

Requirement
→ CREATE TABLE
→ Finish
</Workflow>

<!-- ═══════════════════════════════════════════════════════════════════════════ -->
<!-- 核心工程规则                                                                 -->
<!-- ═══════════════════════════════════════════════════════════════════════════ -->

<Core_Engineering_Rules>

## 1. Existing Baseline First

**修改数据库前必须先审计现有规范。**

**至少检查：**
- Existing Tables / DDL
- Entity 类
- Mapper / Mapper XML
- Existing SQL
- Existing Index
- **Naming Convention（字段命名约定）**
- **ID Strategy（主键策略）**
- **Status Convention（状态字段约定）**
- **Logical Delete（逻辑删除字段）**
- **Audit Fields（审计字段）**
- **Time Fields（时间字段）**
- **Character Set / Collation**
- **Engine（InnoDB）**

**JSJ 项目当前存在两套独立数据库规范：**

### jsj-admin（若依系统 - ry-vue 库）

**基础字段：**
```sql
id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
create_by     VARCHAR(64)  DEFAULT ''  COMMENT '创建者',
create_time   DATETIME     COMMENT '创建时间',
update_by     VARCHAR(64)  DEFAULT ''  COMMENT '更新者',
update_time   DATETIME     COMMENT '更新时间',
del_flag      CHAR(1)      DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）'
```

**特点：**
- 有 `create_by` / `update_by`（操作审计）
- 逻辑删除使用 `del_flag`（CHAR(1), '0'/'2'）
- ID 使用 AUTO_INCREMENT

### jsj-mini（小程序系统 - jsj_mini 库）

**基础字段：**
```sql
id            BIGINT       NOT NULL COMMENT '主键ID（雪花ID）',
create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
update_time   DATETIME     DEFAULT CURRENT_TIMESTAMP
                           ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
is_delete     TINYINT      NOT NULL DEFAULT 0 COMMENT '删除标志：0=未删除 1=已删除'
```

**特点：**
- 无 `create_by` / `update_by`（除非业务需要）
- 逻辑删除使用 `is_delete`（TINYINT, 0/1）
- ID 使用雪花 ID

**原则：**

新建表必须遵循所在模块的既有规范。

**禁止：**
- 在 jsj-admin 中使用 `is_delete`
- 在 jsj-mini 中使用 `del_flag`
- 在 jsj-mini 中机械增加 `create_by`/`update_by`（除非业务需要）
- 跨两个独立数据库强推统一字段命名
- 混用多套 ID 策略
- 混用多套逻辑删除字段

**模块一致性 > 跨模块统一。**

## 2. 表基础字段（Mandatory）

**所有业务表必须具备当前所属模块规定的基础字段。**

**jsj-admin 新建表：**
```sql
CREATE TABLE sys_xxx (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'xxx ID',
  -- 业务字段...
  create_by     VARCHAR(64)  DEFAULT ''  COMMENT '创建者',
  create_time   DATETIME     COMMENT '创建时间',
  update_by     VARCHAR(64)  DEFAULT ''  COMMENT '更新者',
  update_time   DATETIME     COMMENT '更新时间',
  del_flag      CHAR(1)      DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  remark        VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='xxx表';
```

**jsj-mini 新建表：**
```sql
CREATE TABLE xxx (
  id            BIGINT       NOT NULL COMMENT 'xxx ID（雪花ID）',
  -- 业务字段...
  create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time   DATETIME     DEFAULT CURRENT_TIMESTAMP
                             ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  is_delete     TINYINT      NOT NULL DEFAULT 0 COMMENT '删除标志：0=未删除 1=已删除',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='xxx表';
```

**create_by / update_by 使用场景：**

jsj-admin：
- 默认所有业务表都有

jsj-mini：
- 存在人工操作（后台审核、运营修改、管理员操作）
- 需要操作审计
- 需要区分操作主体

**重要区分：**

不要混淆 `user_id` 和 `create_by` / `update_by`：

- `user_id` — **业务归属**（这条数据属于谁）
- `create_by` — **操作主体**（谁创建的这条数据）
- `update_by` — **操作主体**（谁最后修改的这条数据）

**示例：**

```sql
-- 用户订单表
CREATE TABLE `order` (
  id            BIGINT NOT NULL,
  user_id       BIGINT NOT NULL COMMENT '订单所属用户（业务归属）',
  create_by     VARCHAR(64) COMMENT '创建人（操作主体，如客服代下单）',
  ...
);
```

在这个例子中：
- `user_id` = 1001（订单属于用户 1001）
- `create_by` = 'admin'（管理员代客户下单）

两者语义不同，不可混用。

## 3. 主键设计

**JSJ 项目主键策略：**

### jsj-admin

```sql
id BIGINT NOT NULL AUTO_INCREMENT
```

继续使用 MySQL AUTO_INCREMENT。

### jsj-mini

```sql
id BIGINT NOT NULL COMMENT '主键ID（雪花ID）'
```

使用雪花 ID（分布式唯一 ID）。

**不要改变所在模块的主键策略。**

**通用原则：**
- 禁止使用业务字段作为主键
- 禁止使用 VARCHAR 主键
- 禁止复合主键（除非明确的关联表）
- 主键必须 NOT NULL
- 主键必须有 COMMENT

## 4. 字段设计

### 类型选择

**整数：**
- TINYINT：状态、标志（0-255）
- INT：一般整数
- BIGINT：ID、大数值、时间戳

**字符串：**
- VARCHAR(N)：变长字符串，N 根据业务合理设置
- CHAR(N)：固定长度（如 del_flag CHAR(1)）

**时间：**
- DATETIME：标准时间类型
- DATETIME(3)：需要毫秒精度时（如 IM 消息）

**禁止：**
- TEXT / LONGTEXT 存储结构化数据
- JSON 类型字段
- VARCHAR 存储 JSON 字符串

**Structured Business Data → Structured Schema**

### 字段约束

**必须明确：**
- NOT NULL 或 DEFAULT NULL
- DEFAULT 值
- COMMENT（必须有注释）

**示例：**
```sql
status        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0=禁用 1=正常',
nickname      VARCHAR(64)  DEFAULT NULL COMMENT '昵称',
source_type   TINYINT      DEFAULT NULL COMMENT '来源：1=后台 2=注册 3=小程序'
```

### 命名约定

**遵循当前模块约定：**

jsj-admin（若依风格）：
- 表名：`sys_user`, `sys_role`
- 字段：下划线命名 `user_name`, `create_time`

jsj-mini：
- 表名：单数，`user`, `im_message`
- 字段：下划线命名 `user_id`, `create_time`

**通用原则：**
- 小写
- 下划线分隔
- 语义清晰
- 避免保留字

## 5. 状态字段设计

**状态字段必须：**
- 使用 TINYINT
- 定义清晰语义
- COMMENT 中说明所有可能值
- 有合理 DEFAULT

**好的示例：**
```sql
status        TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0=禁用 1=正常 2=锁定',
order_status  TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态：0=待支付 1=已支付 2=已发货 3=已完成 4=已取消',
audit_status  TINYINT NOT NULL DEFAULT 0 COMMENT '审核状态：0=待审核 1=审核通过 2=审核拒绝'
```

**避免：**
```sql
-- 反模式：无语义说明
status TINYINT,

-- 反模式：VARCHAR 存储状态
status VARCHAR(20) DEFAULT 'pending'
```

## 6. 逻辑删除

**JSJ 项目逻辑删除：**

### jsj-admin
```sql
del_flag CHAR(1) DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）'
```

### jsj-mini
```sql
is_delete TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志：0=未删除 1=已删除'
```

**所有业务表必须有逻辑删除字段。**

**查询时必须：**
```sql
WHERE del_flag = '0'  -- jsj-admin
WHERE is_delete = 0   -- jsj-mini
```

**禁止物理删除业务数据。**

## 7. 唯一约束

**业务唯一性必须通过数据库约束保护。**

**示例：**
```sql
UNIQUE KEY uk_user_id (user_id),
UNIQUE KEY uk_token (token),
UNIQUE KEY uk_phone (phone),
UNIQUE KEY uk_email (email)
```

**复合唯一约束：**
```sql
UNIQUE KEY uk_user_conversation (user_id, conversation_id)
```

**原则：**
- 业务不变量必须有约束
- 不要仅依赖 Java 层校验
- 约束名称清晰（uk_ 前缀表示 unique key）

## 8. 外键

**JSJ 项目：不使用数据库外键。**

**原因：**
- 影响性能
- 增加迁移复杂度
- 分库分表困难

**通过以下方式保证引用完整性：**
- Java 业务层校验
- 逻辑删除而非物理删除
- 后台定时对账任务

## 9. 索引设计

### 索引原则

**必须建索引：**
- 主键（自动）
- 唯一约束（自动）
- WHERE 条件高频字段
- JOIN 条件字段
- ORDER BY 字段
- 外键关联字段

**索引命名：**
```sql
PRIMARY KEY (id)                              -- 主键
UNIQUE KEY uk_token (token)                   -- 唯一索引
INDEX idx_user_id (user_id)                   -- 普通索引
INDEX idx_status_create_time (status, create_time)  -- 复合索引
```

### 复合索引

**遵循最左前缀原则：**

```sql
INDEX idx_status_create_time (status, create_time)
```

**可以使用：**
- `WHERE status = 1`
- `WHERE status = 1 AND create_time > xxx`

**不能使用：**
- `WHERE create_time > xxx`（跳过了最左列）

**字段顺序：**
1. 等值查询字段在前
2. 范围查询字段在后
3. 区分度高的字段在前

### 索引覆盖

**如果查询只需要索引字段，可以避免回表：**

```sql
-- 查询只需要 user_id 和 status
INDEX idx_user_id_status (user_id, status)

SELECT user_id, status FROM xxx WHERE user_id = 1;  -- 索引覆盖
```

### 索引注意事项

**避免：**
- 过多索引（影响写入性能）
- 区分度低的字段单独索引（如性别）
- VARCHAR 字段过长索引
- 函数作用于索引字段（如 `WHERE DATE(create_time) = xxx`）

## 10. SQL 性能

### N+1 Query

**禁止：**
```java
// 反模式：N+1 Query
List<Order> orders = orderMapper.selectAll();
for (Order order : orders) {
    User user = userMapper.selectById(order.getUserId());  // N 次查询
}
```

**正确：**
```java
// 方案 1：JOIN
List<OrderVO> orders = orderMapper.selectWithUser();

// 方案 2：IN 批量查询
List<Order> orders = orderMapper.selectAll();
List<Long> userIds = orders.stream().map(Order::getUserId).distinct().collect(toList());
List<User> users = userMapper.selectByIds(userIds);  // 1 次批量查询
```

### 分页

**必须分页：**
```sql
SELECT * FROM user
WHERE status = 1
ORDER BY create_time DESC
LIMIT 20 OFFSET 0;
```

**大表深度分页优化：**
```sql
-- 反模式：OFFSET 过大
SELECT * FROM user ORDER BY id LIMIT 20 OFFSET 100000;  -- 慢

-- 优化：使用游标
SELECT * FROM user WHERE id > 上次最大ID ORDER BY id LIMIT 20;
```

### 批量操作

**批量 INSERT：**
```sql
INSERT INTO user (id, name, status) VALUES
(1, 'a', 1),
(2, 'b', 1),
(3, 'c', 1);
```

**批量 UPDATE（谨慎使用）：**
```sql
UPDATE user SET status = 1 WHERE id IN (1, 2, 3, ...前100个);
```

**避免：**
- 一次批量操作过多数据（建议 <1000 条）
- 长事务中批量操作

### EXPLAIN

**慢查询必须 EXPLAIN：**
```sql
EXPLAIN SELECT * FROM user WHERE status = 1 AND create_time > '2024-01-01';
```

**关注：**
- type（ALL = 全表扫描，需要优化）
- key（使用的索引）
- rows（扫描行数）
- Extra（Using filesort / Using temporary 需要优化）

## 11. 并发与锁

### 乐观锁

**使用 Version 字段：**
```sql
version INT NOT NULL DEFAULT 0 COMMENT '版本号（乐观锁）'

-- 更新时
UPDATE user SET balance = balance - 100, version = version + 1
WHERE id = 1 AND version = 当前version;
```

### 悲观锁

**FOR UPDATE：**
```sql
SELECT * FROM user WHERE id = 1 FOR UPDATE;  -- 行锁

UPDATE user SET balance = balance - 100 WHERE id = 1;
```

**注意：**
- 必须在事务中
- 锁的粒度（行锁 vs 表锁）
- 避免长事务持有锁

### 死锁

**常见死锁场景：**
- 多个事务以不同顺序锁定资源
- 间隙锁

**避免：**
- 事务尽量小
- 按相同顺序访问资源
- 使用唯一索引（避免间隙锁）

## 12. Schema Migration

### Migration 安全原则

**必须：**
- 可回滚
- 分阶段执行
- 先加后删
- 避免锁表

**危险操作：**
- ALTER TABLE（大表会锁表）
- DROP COLUMN（不可回滚）
- CHANGE COLUMN（类型变更）

**安全实践：**

**新增字段：**
```sql
-- Phase 1: 新增字段（允许 NULL）
ALTER TABLE user ADD COLUMN new_field VARCHAR(64) DEFAULT NULL;

-- Phase 2: 数据迁移
UPDATE user SET new_field = xxx;

-- Phase 3: 改为 NOT NULL（如需要）
ALTER TABLE user MODIFY new_field VARCHAR(64) NOT NULL;
```

**删除字段：**
```sql
-- Phase 1: 停止使用
-- Phase 2: 等待观察期
-- Phase 3: 真正删除
ALTER TABLE user DROP COLUMN old_field;
```

**字段重命名：**
```sql
-- 不要用 RENAME
-- Phase 1: 新增字段
ALTER TABLE user ADD COLUMN new_name VARCHAR(64);

-- Phase 2: 数据迁移
UPDATE user SET new_name = old_name;

-- Phase 3: 切换代码
-- Phase 4: 删除旧字段
ALTER TABLE user DROP COLUMN old_name;
```

## 13. 字符集

**JSJ 项目统一：**
```sql
ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
```

**原则：**
- 统一使用 utf8mb4（支持 emoji）
- 不要使用 utf8（实际是 utf8mb3，不完整）

## 14. 引擎

**统一使用：**
```sql
ENGINE=InnoDB
```

**不要使用：**
- MyISAM（不支持事务）
- MEMORY（数据易丢失）

## 15. 表注释

**所有表必须有清晰注释：**
```sql
CREATE TABLE user (
  ...
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
```

## 16. 与 backend-development 协作

**mysql-design 负责：**
- Data Model / Table Design
- Column / Data Type
- Index / Constraint
- DDL
- EXPLAIN / Execution Plan
- SQL 深度优化
- Query Rewrite
- Index Strategy

**backend-development 负责识别（然后由 mysql-design 优化）：**
- N+1 Query
- 缺少分页
- 批量访问机会
- 重复查询
- 明显 SQL 性能风险

**协作场景：**

```
backend-development 识别问题（N+1/重复查询）
  ↓
mysql-design 深度优化（Index/EXPLAIN/Rewrite）
```

</Core_Engineering_Rules>

<!-- ═══════════════════════════════════════════════════════════════════════════ -->
<!-- MySQL Review                                                                 -->
<!-- ═══════════════════════════════════════════════════════════════════════════ -->

<MySQL_Review>

DDL / SQL 完成后重新 Review：

**检查维度：**
- Existing Baseline Respected（遵循现有规范）
- Base Fields Complete（基础字段完整）
- Primary Key Correct（主键正确）
- Logical Delete Correct（逻辑删除正确）
- Constraint Sufficient（约束充分）
- Index Reasonable（索引合理）
- Type Appropriate（类型合理）
- Comment Clear（注释清晰）
- Naming Consistent（命名一致）
- Migration Safe（迁移安全）
- SQL Efficient（SQL 高效）
- No JSON Fields（无 JSON 字段）
- No TEXT for Structured Data（无 TEXT 存储结构化数据）

**问题分级：**

**P0（Critical）：**
- 破坏现有规范
- 缺少基础字段
- 主键错误
- 无逻辑删除
- 关键业务无约束
- 明显 SQL 性能风险

**P1（Major）：**
- 索引缺失
- 类型不合理
- 命名不一致
- 注释缺失
- Migration 不安全

**P2（Minor）：**
- 索引可优化
- 注释可完善
- 命名可改进

**P0 / P1 必须修复。**

</MySQL_Review>

<!-- ═══════════════════════════════════════════════════════════════════════════ -->
<!-- READY 标准                                                                   -->
<!-- ═══════════════════════════════════════════════════════════════════════════ -->

<MYSQL_READY_Definition>

只有同时满足以下条件才能 MYSQL READY：

✅ **Existing Baseline Respected** — 遵循现有规范

✅ **Base Fields Complete** — 基础字段完整

✅ **Primary Key Correct** — 主键设计正确

✅ **Logical Delete Correct** — 逻辑删除正确

✅ **Constraint Sufficient** — 业务约束充分

✅ **Index Reasonable** — 索引合理

✅ **Type Appropriate** — 字段类型合理

✅ **Comment Clear** — 所有表和字段有清晰注释

✅ **Naming Consistent** — 命名遵循模块约定

✅ **Character Set Correct** — utf8mb4

✅ **Engine Correct** — InnoDB

✅ **No JSON Fields** — 无 JSON 类型字段

✅ **No TEXT for Structured Data** — 无 TEXT 存储结构化数据

✅ **SQL Reviewed** — SQL 经过性能审查

✅ **Migration Safe** — Schema 变更安全

✅ **No P0** — 无 P0 问题

✅ **No P1** — 无 P1 问题

</MYSQL_READY_Definition>

<!-- ═══════════════════════════════════════════════════════════════════════════ -->
<!-- 最终注释                                                                     -->
<!-- ═══════════════════════════════════════════════════════════════════════════ -->

<Final_Notes>

## 本 Skill 的定位

**mysql-design 负责：**

数据库设计和优化（Data Model & Query Design）

**不负责：**

业务逻辑（backend-development）
前端实现（senior-frontend）
UI 设计（ui-design）

## Scope

**本 Skill 禁止：**
- 修改 Java 业务代码
- 修改 Frontend
- 擅自统一两个独立数据库的字段命名

**本 Skill 只负责：**

MySQL 数据库工程方法论。

## JSJ 双数据库架构

JSJ 项目有两个独立数据库：

```
jsj-admin → ry-vue 库（若依规范）
jsj-mini  → jsj_mini 库（小程序规范）
```

**两套规范共存，模块一致性优先。**

## 兼容性

本 Skill 核心方法论保持项目无关。

JSJ 项目特定约定作为示例，不强制其他项目使用。

</Final_Notes>

# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## AI Agent Skills

本项目包含专用的 AI Agent Skills，用于提升代码质量和开发效率。

### Project-Level Skills

**Skills 位置：** `.agents/skills/`

**Available Skills:**
- `senior-frontend` - Senior Frontend Engineering workflow for production-grade UI implementation

**Skill Discovery:**
- Claude Code: 自动发现 `.agents/skills/*/SKILL.md`
- Cursor: 通过 `.cursorrules` 引导

**Priority:**
项目级 Skills 优先于全局 Skills (`~/.claude/skills/`, `~/.cursor/skills-cursor/`)

**Usage:**
当执行前端任务（UI 实现、响应式设计、组件开发、CSS 优化、Visual Review）时，自动应用 `senior-frontend` 工程标准。

---

## Project Overview

Scaffolding-v2 是一个全栈多平台脚手架项目，包含后台管理系统、小程序后端、移动端前端和代码生成器。

**核心特点：**
- 双后端独立部署：jsj-admin（管理端）与 jsj-mini（小程序/IM）完全解耦
- 双库设计：ry-vue（管理端）与 jsj_mini（移动端/IM）数据库隔离
- 双 ORM 风格：jsj-admin 使用 MyBatis XML，jsj-mini 使用 MyBatis-Plus 注解
- 共享基础模块：jsj-common（通用工具）、jsj-service（数据访问层）

## Project Structure

```
scaffolding-v2/
├── generator/        # JavaFX 桌面代码生成器 (Java 21 + Freemarker)
├── jsj-admin/        # 后台管理后端 (Spring Boot 3.5, :8080)
├── jsj-admin-ui/     # 后台管理前端 (Vue 3 + TypeScript + Vite, dev :80)
├── jsj-mini/         # 小程序/IM 后端 (Spring Boot 3.5, HTTP :8881, WS :9001)
├── jsj-mini-app/     # 微信原生小程序
├── jsj-uni-app/      # UniApp 跨端 App
├── jsj-common/       # 公共基础能力（通用异常、注解等）
├── jsj-service/      # 统一数据访问与核心业务 Service（MyBatis-Plus + Redis）
└── sql/              # 数据库初始化脚本
    ├── admin.sql     # 管理端主表 → ry-vue 库
    └── mini.sql      # 小程序/IM → jsj_mini 库
```

**重要：** 各 Java 模块独立构建，无根级 Maven 聚合 POM。

## Architecture

### Data Flow

**管理端：**
```
jsj-admin-ui (:80)
  → Vite proxy /dev-api
  → jsj-admin (:8080)
  → MyBatis XML
  → MySQL (ry-vue)
```

**移动端 HTTP：**
```
jsj-mini-app / jsj-uni-app
  → jsj-mini (:8881) /api/*
  → MyBatis-Plus
  → MySQL (jsj_mini)
```

**移动端 WebSocket (IM)：**
```
客户端
  → ws://host:9001/ws
  → Netty
  → Redis token 校验
  → IM Handler (单聊/群聊/ACK/心跳)
```

### Module Dependencies

**jsj-admin** 依赖：
- jsj-common (v1.0.0)
- jsj-service (v1.0.0)
- Spring Security + JWT
- MyBatis + PageHelper
- Quartz

**jsj-mini** 依赖：
- jsj-common (v1.0.0)
- jsj-service (v1.0.0)
- Netty 4.1 (WebSocket IM)
- Hutool

**jsj-service** 提供：
- MyBatis-Plus 3.5.9 + 分页拦截器
- Druid 连接池
- Redis 配置与模板
- 通过 `application-common.yml` 统一配置数据源和 Redis

**jsj-common** 提供：
- 业务异常基类
- 通用注解（如需扩展）
- 无业务逻辑，仅工具类

## Build & Run Commands

### Backend (Java)

**前置条件：**
- JDK 21+
- MySQL 8.0+，Redis 6+
- 先导入数据库脚本：`sql/admin.sql` → ry-vue 库，`sql/mini.sql` → jsj_mini 库

**jsj-admin (管理后端)：**
```bash
cd jsj-admin
mvn clean package              # 打包
mvn spring-boot:run            # 运行 :8080
```
Swagger: http://localhost:8080/swagger-ui.html

**jsj-mini (小程序/IM 后端)：**
```bash
cd jsj-mini
mvn clean package
mvn spring-boot:run            # HTTP :8881, WS :9001
```

**注意端口冲突：** jsj-mini 默认配置 `server.port=8881`（不是 8081），若需修改端口，编辑 `jsj-mini/src/main/resources/application.yml`。

### Frontend (Node.js)

**前置条件：** Node.js 18+

**jsj-admin-ui (管理前端)：**
```bash
cd jsj-admin-ui
npm install                    # 安装依赖
npm run dev                    # 开发 :80，代理 /dev-api → :8080
npm run build:prod             # 生产构建
```

**jsj-uni-app (跨端 App)：**
```bash
cd jsj-uni-app
npm install
npm run dev:h5                 # H5 开发
npm run dev:mp-weixin          # 微信小程序开发
npm run build:h5               # H5 构建
```

**jsj-mini-app (微信原生小程序)：**
- 用微信开发者工具打开 `jsj-mini-app/` 目录
- 配置 `config.js` 中 `baseUrl` 为 `http://localhost:8881`（默认错误写为 :8080）

### Code Generator

**generator (JavaFX 桌面生成器)：**
```bash
cd generator
mvn javafx:run                 # 启动 GUI
```
- 连接数据库，选择表，生成 CRUD 代码（Controller/Service/Dao/Mapper/DTO）
- 模板位置：`generator/template/*.ftl` (24 个 Freemarker 模板)

## Configuration

### Database & Redis

**jsj-service 统一配置：** `jsj-service/src/main/resources/application-common.yml`

```yaml
spring:
  datasource:
    url: jdbc:mysql://192.168.31.48:3306/pupu?...
    username: root
    password: 123456
  data:
    redis:
      host: ${REDIS_HOST:192.168.31.48}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
```

**环境变量覆盖（生产部署必须）：**
- `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`, `REDIS_DATABASE`
- `WX_APP_ID`, `WX_APP_SECRET` (jsj-mini 微信登录)
- `IM_ALLOWED_ORIGINS` (WebSocket Origin 白名单)
- `JWT_SECRET` (jsj-admin JWT 签名密钥，生产必须替换)

**数据库切换：**
- jsj-admin 连接 ry-vue 库（由 admin.sql 创建）
- jsj-mini 连接 jsj_mini 库（由 mini.sql 创建）
- 需修改 `application-common.yml` 中的 `url` 参数

### Frontend Proxy

**jsj-admin-ui 开发代理：**
- `.env.development` 中 `VITE_APP_PROXY_TARGET=http://localhost:8080`
- Vite 将 `/dev-api` 请求代理到后端
- 若后端端口改变，需同步修改此配置

**移动端配置：**
- jsj-mini-app: `config.js` → `baseUrl`, `wsUrl`
- jsj-uni-app: `src/config/index.js` → 按环境切换

## Key Implementation Details

### Authentication

**jsj-admin (JWT):**
- Spring Security + JWT Token
- Token 存 Redis，可配置过期时间 (默认 30 分钟)
- 权限模型：RBAC（用户 → 角色 → 菜单/按钮权限 + 数据权限 `@DataScope`）
- 入口：`SysLoginController` → `SysLoginService` → `TokenService`

**jsj-mini (自定义 Token):**
- Token = UUID，存 Redis，Header `Authorization: Bearer <token>`
- 拦截器：`LoginInterceptor` 解析 token → `UserContext.setUserId()`
- 免登录接口标注 `@NoLoginRequest`
- 微信登录：`AuthController.wxLogin()` → `WeChatMiniService.code2session()` → 查找/创建用户 → 签发 token
- WebSocket 鉴权：复用 HTTP token，通过 `AUTH` 消息携带，从 Redis 校验

### IM Module (Netty WebSocket)

**连接地址：** `ws://localhost:9001/ws`

**核心文件：**
- `jsj-mini/src/main/java/com/zhs/netty/ImWebSocketServer.java` - Netty 服务器
- `config/NettyServerConfig.java` - Spring 启动后拉起 Netty
- `netty/handler/WebSocketFrameHandler.java` - 消息分发入口
- `netty/handler/ImHandlerFactory.java` - 按 MsgType 路由 Handler
- `netty/handler/impl/` - AuthHandler, ChatHandler, GroupChatHandler, AckHandler, HeartbeatHandler
- `netty/session/ImSessionManager.java` - 用户 Channel 会话管理

**协议格式（JSON）：**
```json
{
  "msgType": 1,           // 1=AUTH, 3=HEARTBEAT, 5=CHAT, 6=GROUP_CHAT, 8=ACK
  "requestId": "uuid",
  "timestamp": 1714000000000,
  "body": { }
}
```

**消息状态机（im_message.status）：**
- 0 = 未送达
- 1 = 已送达
- 2 = 已读
- 3 = 已撤回

客户端通过 `ACK` 消息确认送达/已读。

### Code Generation

项目提供两套代码生成：

**1. 桌面生成器（generator）：**
- 独立 JavaFX 应用，连接任意数据库
- 模板：`generator/template/*.ftl` (24 个)
- 支持 MySQL, PostgreSQL, SQL Server, Oracle, MongoDB
- 输出：Controller/Service/Dao/Mapper/DTO/Req/Resp

**2. 管理端内置生成（jsj-admin）：**
- 入口：管理 UI → 系统工具 → 代码生成，或 API `/tool/gen/*`
- 模板：Velocity (`jsj-admin/src/main/resources/vm/`)
- 元数据表：`gen_table`, `gen_table_column`（存于 ry-vue 库）
- 配置：`generator.yml`（作者、包名、表前缀）

### Shared Service Layer

**jsj-service 职责：**
- 统一数据访问：MyBatis-Plus Mapper（用户、IM 实体）
- 核心业务 Service：`AuthService`, `WeChatMiniService`（可被 jsj-admin/jsj-mini 复用）
- 配置自动装配：`JsjServiceAutoConfiguration`

**重要：** jsj-admin 和 jsj-mini 通过依赖 jsj-service 复用数据层，但业务逻辑（Controller/具体 Service 实现）仍独立。

## ORM Differences

| 模块 | ORM | 特点 |
|------|-----|------|
| jsj-admin | MyBatis (XML) | Mapper XML 位于 `jsj-admin/src/main/resources/mapper/`，20+ 文件 |
| jsj-mini | MyBatis-Plus (注解) | Mapper 接口继承 `BaseMapper`，无 XML，使用 Wrapper 查询 |
| jsj-service | MyBatis-Plus (注解) | 提供共享实体 Mapper（User, UserSensitiveInfo, ImMessage 等） |

**扩展 CRUD 时：**
- jsj-admin 领域：创建 Mapper.xml + Service + Controller
- jsj-mini 领域：创建 Mapper 接口 + Service + Controller（无需 XML）

## Testing

**现状：** 各模块均配置了测试依赖，但无实际测试代码（无 `src/test` 实现）。

**添加测试时：**
- jsj-admin: 使用 `spring-boot-starter-test`
- jsj-mini: 使用 JUnit Jupiter 5.10.2
- 建议先为核心业务逻辑（AuthService, IM Handler）编写单元测试

## Common Pitfalls

1. **端口配置不一致：**
   - jsj-mini 默认 :8881，但 jsj-mini-app/config.js 默认写 :8080，需手动改为 :8881

2. **数据库名称不匹配：**
   - SQL 脚本建库为 `jsj_mini`，但 `application-common.yml` 可能配置为其他库名，需对齐

3. **前端代理目标：**
   - jsj-admin-ui 代理到 :8080（jsj-admin）
   - 移动端配置应指向 :8881（jsj-mini）

4. **生产密钥：**
   - JWT secret、DB 密码、微信 AppSecret 必须通过环境变量覆盖，不要硬编码

5. **IM Origin 白名单：**
   - 生产部署时必须配置 `IM_ALLOWED_ORIGINS` 环境变量，否则 WebSocket 连接可能被拒绝

6. **虚拟线程：**
   - jsj-admin 启用了 JDK 21 虚拟线程 (`spring.threads.virtual.enabled=true`)，jsj-mini 未启用

## Documentation

详细文档位于 `doc/project-index.md`，包含：
- 完整业务域索引
- 数据库表结构说明
- API 路由清单
- IM 协议详解
- 认证与鉴权流程
- 新人导航路径

开发前建议先阅读此文档。

> 历史脚手架索引，仅供追溯。模块归属和运行指引已失效，请使用 project-index.md。

# Scaffolding-v2 项目索引

> 全栈多平台脚手架 — 业务域与实现说明  
> 根路径：`D:\code\living_code\脚手架\scaffolding`  
> 最后更新：2026-08-08

---

## 目录

- [1. 项目概览](#1-项目概览)
- [2. 顶层结构](#2-顶层结构)
- [3. 技术栈](#3-技术栈)
- [4. 系统架构](#4-系统架构)
- [5. 模块详解](#5-模块详解)
  - [5.1 generator — 桌面代码生成器](#51-generator--桌面代码生成器)
  - [5.2 jsj-admin — 后台管理后端](#52-jsj-admin--后台管理后端)
  - [5.3 jsj-admin-ui — 后台管理前端](#53-jsj-admin-ui--后台管理前端)
  - [5.4 jsj-mini — 小程序/IM 后端](#54-jsj-mini--小程序im-后端)
  - [5.5 jsj-mini-app — 微信原生小程序](#55-jsj-mini-app--微信原生小程序)
  - [5.6 jsj-uni-app — UniApp 跨端](#56-jsj-uni-app--uniapp-跨端)
- [6. 业务域索引](#6-业务域索引)
- [7. 数据库设计](#7-数据库设计)
- [8. API 路由清单](#8-api-路由清单)
- [9. IM 模块实现](#9-im-模块实现)
- [10. 认证与鉴权](#10-认证与鉴权)
- [11. 配置文件](#11-配置文件)
- [12. 启动与开发流程](#12-启动与开发流程)
- [13. 代码生成](#13-代码生成)
- [14. 测试与 CI/CD](#14-测试与-cicd)
- [15. 已知缺口与注意事项](#15-已知缺口与注意事项)

---

## 1. 项目概览

**Scaffolding-v2（脚手架 v2）** 是一个全栈多平台脚手架项目，采用 Monorepo 式目录组织，各模块独立构建（无根级 Maven 聚合 POM）。

核心能力：

- **生成器驱动开发** — 桌面 JavaFX 工具 + 管理端内置代码生成，一键产出 CRUD 代码
- **前后端分离 + 多端** — 管理端 Vue 3；移动端同时支持微信原生小程序和 UniApp
- **双后端独立部署** — `jsj-admin`（管理）与 `jsj-mini`（小程序/IM）解耦，可独立扩展
- **集成 Netty IM** — WebSocket 实时聊天（单聊/群聊/消息状态机/ACK）
- **现代化技术栈** — Java 21 虚拟线程、Spring Boot 3、Vue 3

---

## 2. 顶层结构

```
scaffolding/
├── generator/        # JavaFX 桌面代码生成器
├── jsj-admin/        # 后台管理系统 - 后端 (Spring Boot 3, :8080)
├── jsj-admin-ui/     # 后台管理系统 - 前端 (Vue 3, dev :80)
├── jsj-mini/         # 小程序/IM 后端 (HTTP :8081, WS :9001/ws)
├── jsj-mini-app/     # 微信原生小程序
├── jsj-uni-app/      # UniApp 跨端 App (H5 / 微信 / App)
├── sql/              # MySQL 初始化脚本
├── doc/              # 项目文档（本文件）
└── README.md         # 项目总览与快速开始
```

> **注意：** 根目录 `README.md` 中引用的是 `docs/` 目录，实际文档目录为 `doc/`。

---

## 3. 技术栈

| 模块 | 路径 | 技术 | 默认端口 |
|------|------|------|----------|
| **generator** | `generator/` | Java 21 + JavaFX 21 + Freemarker + SQLite + Log4j2 | 桌面 GUI |
| **jsj-admin** | `jsj-admin/` | Spring Boot 3.5.11 + MyBatis + Spring Security + JWT + Redis + Quartz + Druid + PageHelper + SpringDoc | HTTP **8080** |
| **jsj-admin-ui** | `jsj-admin-ui/` | Vue 3.5 + TypeScript 5.6 + Vite 6 + Element Plus + Pinia + ECharts + Axios | dev **80** |
| **jsj-mini** | `jsj-mini/` | Spring Boot 3.5.11 + MyBatis-Plus 3.5 + Redis + Netty 4.1 + Hutool + SpringDoc | HTTP **8081**, WS **9001/ws** |
| **jsj-mini-app** | `jsj-mini-app/` | 微信原生小程序 (WXML / WXSS / JS) | — |
| **jsj-uni-app** | `jsj-uni-app/` | Vue 3 + UniApp 3 alpha + Vite 5 + Pinia | 按平台 |

**基础设施依赖：** MySQL 8.0+、Redis 6+、JDK 21+、Node.js 18+

---

## 4. 系统架构

```
┌─────────────────┐     ┌──────────────────┐
│  jsj-admin-ui   │────>│    jsj-admin     │────> MySQL (ry-vue) + Redis
│  (Vue 3)        │     │  (Spring Boot 3) │
└─────────────────┘     └──────────────────┘

┌─────────────────┐
│  jsj-mini-app   │──┐
│  (微信原生)      │  │   ┌──────────────────┐
└─────────────────┘  ├──>│    jsj-mini      │────> MySQL (jsj_mini) + Redis
┌─────────────────┐  │   │  HTTP :8081      │
│  jsj-uni-app    │──┘   │  WS   :9001 /ws  │
│  (UniApp)       │      └──────────────────┘
└─────────────────┘

┌─────────────────┐
│    generator    │────> 连接数据库，生成 CRUD 代码
│  (JavaFX)       │
└─────────────────┘
```

### 数据流说明

| 链路 | 请求路径 |
|------|----------|
| 管理端 | `jsj-admin-ui` → Axios `/dev-api` 代理 → `jsj-admin:8080` → MyBatis XML → `ry-vue` |
| 移动端 HTTP | `jsj-mini-app` / `jsj-uni-app` → `jsj-mini:8081/api/*` → MyBatis-Plus → `jsj_mini` |
| 移动端 WS | 客户端 → `ws://host:9001/ws` → Netty → Redis token 校验 → IM Handler |
| 代码生成 | `generator` 连库读表结构 → Freemarker 模板 → 输出 Java/Mapper 等 |

---

## 5. 模块详解

### 5.1 generator — 桌面代码生成器

**入口：** `generator/src/main/java/com/szmirren/Main.java`  
**启动：** `cd generator && mvn javafx:run`

#### 目录结构

```
generator/
├── pom.xml
├── config/ConfigDB.db          # SQLite 本地配置库（数据库连接、生成历史等）
├── template/*.ftl              # Freemarker 模板
└── src/main/java/com/szmirren/
    ├── Main.java               # JavaFX 入口
    ├── controller/             # 23 个 FXML 控制器（连接、生成、设置）
    ├── common/                 # ConfigUtil、TemplateUtil
    ├── entity/ / models/ / options/ / spi/
    └── view/                   # FXML 视图
```

#### 模板清单（`generator/template/`）

| 模板 | 输出 |
|------|------|
| `zhsController.ftl` | REST Controller |
| `zhsService.ftl` / `zhsServiceImpl.ftl` | Service 接口与实现 |
| `zhsDao.ftl` | DAO 层 |
| `zhsEntity.ftl` / `zhsDto.ftl` | 实体与 DTO |
| `zhsAddReq.ftl` / `zhsUpdateReq.ftl` / `zhsQueryReq.ftl` | 请求对象 |
| `zhsResp.ftl` / `zhsDetailResp.ftl` | 响应对象 |
| `MySQLMapper.ftl` / `OracleMapper.ftl` / `PostgreSQLMapper.ftl` / `SqlServerMapper.ftl` | 各数据库 Mapper |
| `zhsMongoController.ftl` / `zhsMongoServiceImpl.ftl` / `zhsMongoDto.ftl` / `zhsMongoConfig.ftl` | MongoDB 系列 |
| `UnitTest.ftl` | 单元测试模板 |
| `zhsSwagger.ftl` / `zhsR.ftl` / `zhsMeta.ftl` | Swagger 注解、统一返回、元数据 |

#### 支持的数据库

MySQL、PostgreSQL、SQL Server、Oracle、MongoDB

---

### 5.2 jsj-admin — 后台管理后端

**入口：** `jsj-admin/src/main/java/com/zhs/JsjAdminApplication.java`  
**启动：** `cd jsj-admin && mvn spring-boot:run`  
**Swagger：** `http://localhost:8080/swagger-ui.html`

基于 **RuoYi-Vue 3.9.2** 升级到 Spring Boot 3 的后台管理后端。

#### 包结构（`com.zhs.*`）

| 包 | 职责 |
|----|------|
| `common/` | 通用工具、注解（`@Log`、`@DataScope`、`@RateLimiter` 等）、异常、Redis 封装、XSS 过滤、常量 |
| `framework/` | Spring 配置（Security/JWT/Redis/Druid/MyBatis）、AOP、拦截器、登录/权限/Token 服务 |
| `system/` | 系统管理领域：用户/角色/菜单/部门/字典/配置/通知/岗位等 Service + Mapper + Model |
| `web/controller/` | REST 控制器（system、monitor、common、tool） |
| `generator/` | 内置代码生成（Velocity 模板，与 admin-ui 的 `/tool/gen` 联动） |
| `quartz/` | 定时任务调度（SysJob、SysJobLog、RyTask 示例） |

#### 关键实现类

| 类 | 路径 | 说明 |
|----|------|------|
| `SecurityConfig` | `framework/config/SecurityConfig.java` | JWT + Spring Security 过滤器链 |
| `TokenService` | `framework/web/service/TokenService.java` | Token 签发与校验 |
| `SysLoginService` | `framework/web/service/SysLoginService.java` | 登录逻辑（验证码、密码重试锁定） |
| `SysPermissionService` | `framework/web/service/SysPermissionService.java` | 权限集合计算 |
| `DruidConfig` | `framework/config/DruidConfig.java` | 动态数据源 + Druid 监控 |
| `DataScopeAspect` | `framework/aspectj/DataScopeAspect.java` | 数据权限 AOP |
| `GenController` | `generator/controller/GenController.java` | 代码生成 REST API |

#### 认证流程（管理端）

1. 前端请求 `/captchaImage` 获取验证码
2. `POST /login` 提交用户名 + 密码 + 验证码 + uuid
3. `SysLoginService` 校验 → 生成 JWT Token
4. 前端存储 Token，后续请求 Header 携带 `Authorization: Bearer <token>`
5. `GET /getInfo` 获取用户信息与权限
6. `GET /getRouters` 获取动态菜单路由

---

### 5.3 jsj-admin-ui — 后台管理前端

**入口：** `jsj-admin-ui/index.html` → `src/main.ts`  
**启动：** `cd jsj-admin-ui && npm install && npm run dev`

#### 目录结构（`src/`）

| 目录 | 职责 |
|------|------|
| `api/` | 后端 API 封装 |
| `views/` | 页面组件 |
| `router/` | 路由配置（`router/index.ts`） |
| `store/` | Pinia 状态（user、dict、tagsView、app、lock） |
| `layout/` | 布局框架 |
| `utils/request.ts` | Axios 封装，开发代理前缀 `/dev-api` |
| `permission.ts` | 路由守卫 / 权限控制 |
| `vite/plugins/` | Vite 插件（auto-import、svg-icon、compression） |

#### API 层（`src/api/`）

| 文件 | 对应后端 |
|------|----------|
| `login.ts` | 登录、注册、getInfo、getRouters |
| `menu.ts` | 菜单路由 |
| `system/user.ts` | 用户管理 |
| `system/role.ts` | 角色管理 |
| `system/menu.ts` | 菜单管理 |
| `system/dept.ts` | 部门管理 |
| `system/post.ts` | 岗位管理 |
| `system/config.ts` | 参数配置 |
| `system/dict/type.ts` / `data.ts` | 字典管理 |
| `system/notice.ts` | 通知公告 |
| `monitor/online.ts` | 在线用户 |
| `monitor/job.ts` / `jobLog.ts` | 定时任务 |
| `monitor/operlog.ts` | 操作日志 |
| `monitor/logininfor.ts` | 登录日志 |
| `monitor/server.ts` | 服务监控 |
| `monitor/cache.ts` | 缓存监控 |
| `tool/gen.ts` | 代码生成 |

#### 页面域（`src/views/`）

| 目录 | 页面 |
|------|------|
| `system/user/` | 用户列表、授权角色、个人中心（头像/信息/改密） |
| `system/role/` | 角色列表、分配用户 |
| `system/menu/` | 菜单管理 |
| `system/dept/` | 部门管理 |
| `system/post/` | 岗位管理 |
| `system/dict/` | 字典类型与数据 |
| `system/config/` | 参数配置 |
| `system/notice/` | 通知公告 |
| `monitor/online/` | 在线用户 |
| `monitor/job/` | 定时任务及日志 |
| `monitor/operlog/` | 操作日志 |
| `monitor/logininfor/` | 登录日志 |
| `monitor/server/` | 服务监控 |
| `monitor/cache/` | 缓存监控 |
| `monitor/druid/` | Druid 监控 |
| `tool/gen/` | 代码生成（导入表、编辑、预览、下载） |
| `tool/build/` | 表单构建器 |
| `tool/swagger/` | Swagger 文档 |
| `login.vue` / `register.vue` | 登录/注册 |
| `error/404.vue` / `401.vue` | 错误页 |

---

### 5.4 jsj-mini — 小程序/IM 后端

**入口：** `jsj-mini/src/main/java/com/zhs/MiniApplication.java`  
**启动：** `cd jsj-mini && mvn spring-boot:run`

#### 包结构（`com.zhs.*`）

| 包 | 职责 |
|----|------|
| `controller/` | HTTP REST API（目前仅 `AuthController`） |
| `service/` | 认证、微信登录、用户业务 |
| `service/wechat/` | 微信 code2session 封装 |
| `mapper/` | MyBatis-Plus Mapper（用户 3 表 + IM 4 表） |
| `model/` | 实体类 |
| `netty/` | WebSocket IM 服务核心 |
| `config/` | WebMvc、Redis、MyBatis-Plus、Netty 启动、登录拦截器 |
| `common/` | `Result` 统一返回、`TokenUtil`、`UserContext`、缓存 Key |
| `dto/` / `request/` / `response/` | 请求响应 DTO |
| `exception/` | `GlobalExceptionHandler`、`ServiceException` |

#### Netty IM 核心文件

| 文件 | 说明 |
|------|------|
| `netty/ImWebSocketServer.java` | WS 服务器 Bootstrap |
| `config/NettyServerConfig.java` | Spring 启动后拉起 Netty 线程 |
| `netty/handler/WebSocketFrameHandler.java` | 帧分发入口 |
| `netty/handler/ImHandlerFactory.java` | 按 `MsgType` 路由 Handler |
| `netty/handler/OriginCheckHandler.java` | Origin 白名单校验 |
| `netty/handler/impl/AuthHandler.java` | WS 鉴权 |
| `netty/handler/impl/HeartbeatHandler.java` | 心跳 |
| `netty/handler/impl/ChatHandler.java` | 单聊 |
| `netty/handler/impl/GroupChatHandler.java` | 群聊 |
| `netty/handler/impl/AckHandler.java` | 消息确认 |
| `netty/session/ImSessionManager.java` | 用户 Channel 会话管理 |
| `netty/session/ImChannelAttrKeys.java` | Channel 属性（userId、authenticated 等） |
| `netty/protocol/ImPacket.java` | JSON 协议信封 |
| `netty/protocol/MsgType.java` | 消息类型枚举 |
| `netty/protocol/ChatBody.java` / `GroupChatBody.java` | 消息体 |

---

### 5.5 jsj-mini-app — 微信原生小程序

**入口：** `jsj-mini-app/app.js`  
**打开方式：** 微信开发者工具导入 `jsj-mini-app/` 目录

#### 目录结构

```
jsj-mini-app/
├── app.js / app.json / app.wxss
├── config.js              # baseUrl、wsUrl、testLoginEnabled
├── api/auth.js            # 认证 API
├── pages/
│   ├── login/index        # 登录页
│   └── index/index        # 首页
└── utils/
    ├── request.js         # 请求封装（自动带 token）
    ├── websocket.js       # WS 客户端
    └── im-core.js         # IM 核心逻辑
```

#### 页面路由（`app.json`）

- `pages/login/index` — 登录
- `pages/index/index` — 首页

> **配置注意：** `config.js` 中 `baseUrl` 默认为 `http://localhost:8080`，但 `jsj-mini` 实际运行在 **8081**，需手动改为 `http://localhost:8081`。

---

### 5.6 jsj-uni-app — UniApp 跨端

**入口：** `jsj-uni-app/src/main.js`  
**启动：** `npm run dev:h5` / `dev:mp-weixin` / `dev:app`

#### 目录结构

```
jsj-uni-app/src/
├── main.js / App.vue
├── pages.json / manifest.json
├── config/index.js        # baseUrl :8081, wsUrl :9001/ws
├── api/auth.js
├── pages/login/ / pages/index/
├── store/modules/user.js
└── utils/                 # request、auth、websocket、im-core（与 mini-app 同构）
```

#### 环境配置（`config/index.js`）

| 环境 | baseUrl | wsUrl |
|------|---------|-------|
| development | `http://localhost:8081` | `ws://localhost:9001/ws` |
| production | `https://api.yoursite.com` | `wss://api.yoursite.com/ws` |

#### 页面路由（`pages.json`）

- `pages/index/index` — 首页
- `pages/login/index` — 登录

---

## 6. 业务域索引

### 6.1 后台管理系统（jsj-admin + jsj-admin-ui）

| 业务域 | 说明 | 后端 Controller | 前端 Views |
|--------|------|-----------------|------------|
| **认证** | 登录、注册、验证码、Token | `SysLoginController`, `SysRegisterController`, `CaptchaController` | `login.vue`, `register.vue` |
| **用户管理** | CRUD、导入导出、重置密码、分配角色 | `SysUserController` | `system/user/` |
| **角色管理** | CRUD、菜单权限、数据权限、分配用户 | `SysRoleController` | `system/role/` |
| **菜单管理** | 树形菜单 CRUD | `SysMenuController` | `system/menu/` |
| **部门管理** | 树形部门 CRUD | `SysDeptController` | `system/dept/` |
| **岗位管理** | 岗位 CRUD | `SysPostController` | `system/post/` |
| **字典管理** | 字典类型 + 字典数据 | `SysDictTypeController`, `SysDictDataController` | `system/dict/` |
| **参数配置** | 系统参数键值对 | `SysConfigController` | `system/config/` |
| **通知公告** | 公告发布与阅读 | `SysNoticeController` | `system/notice/` |
| **个人中心** | 头像、基本信息、改密 | `SysProfileController` | `system/user/profile/` |
| **在线用户** | 查看/强退在线会话 | `SysUserOnlineController` | `monitor/online/` |
| **定时任务** | Quartz 任务 CRUD、启停、日志 | `SysJobController`, `SysJobLogController` | `monitor/job/` |
| **操作日志** | 系统操作审计 | `SysOperlogController` | `monitor/operlog/` |
| **登录日志** | 登录成功/失败记录 | `SysLogininforController` | `monitor/logininfor/` |
| **服务监控** | CPU/内存/JVM/磁盘 | `ServerController` | `monitor/server/` |
| **缓存监控** | Redis 键值查看 | `CacheController` | `monitor/cache/` |
| **文件上传下载** | 通用文件接口 | `CommonController` | — |
| **代码生成** | 导入表、配置、预览、下载 | `GenController` | `tool/gen/` |
| **表单构建** | 可视化表单设计 | — | `tool/build/` |

### 6.2 小程序/IM 系统（jsj-mini + 移动端）

| 业务域 | 说明 | 实现位置 |
|--------|------|----------|
| **微信登录** | code → openId → 查找/创建用户 → 签发 token | `AuthServiceImpl.wxLogin()` |
| **手机验证码** | 发送频率限制 + Redis 存码（开发环境日志打印） | `AuthServiceImpl.sendPhoneCode()` |
| **手机号登录** | 验证码校验 → 查找/创建用户 → 签发 token | `AuthServiceImpl.phoneLogin()` |
| **当前用户** | 从 `UserContext` 取 userId 返回用户信息 | `AuthController.currentUser()` |
| **退出登录** | 删 Redis token + 断开 WS 连接 | `AuthServiceImpl.logout()` |
| **单聊** | WS 发送/推送文本消息，持久化到 `im_message` | `ChatHandler` |
| **群聊** | WS 群消息广播，更新群成员会话 | `GroupChatHandler` |
| **消息 ACK** | 客户端确认送达/已读 | `AckHandler` |
| **心跳** | 保活 + 空闲断开 | `HeartbeatHandler` |
| **会话管理** | 用户 ↔ Channel 映射，多端登录 | `ImSessionManager` |

---

## 7. 数据库设计

### 7.1 管理端库 `ry-vue`

**脚本：** `sql/admin.sql`（主表）、`sql/quartz.sql`（定时任务）

| 表名 | 说明 |
|------|------|
| `sys_dept` | 部门（树形） |
| `sys_user` | 系统用户 |
| `sys_post` | 岗位 |
| `sys_role` | 角色 |
| `sys_menu` | 菜单/按钮权限 |
| `sys_user_role` | 用户-角色关联 |
| `sys_role_menu` | 角色-菜单关联 |
| `sys_role_dept` | 角色-部门（数据权限） |
| `sys_user_post` | 用户-岗位关联 |
| `sys_oper_log` | 操作日志 |
| `sys_dict_type` / `sys_dict_data` | 字典 |
| `sys_config` | 系统参数 |
| `sys_logininfor` | 登录日志 |
| `sys_job` / `sys_job_log` | 定时任务及日志 |
| `sys_notice` / `sys_notice_read` | 通知公告 |
| `gen_table` / `gen_table_column` | 代码生成元数据 |

**ORM：** MyBatis XML，映射文件位于 `jsj-admin/src/main/resources/mapper/`

### 7.2 小程序/IM 库 `jsj_mini`

**脚本：** `sql/mini.sql`（用户）、`sql/jsj_mini_im.sql`（IM）

#### 用户体系

| 表名 | 说明 | 关键字段 |
|------|------|----------|
| `user` | 用户主表 | `id`(雪花), `nickname`, `avatar`, `status`, `source_type`(1后台/2手机/3小程序...) |
| `user_sensitive_info` | 敏感信息 | `open_id`, `union_id`, `phone` |
| `user_login_session` | 登录会话 | `token`, `device_type`(mini/uni/h5/admin), `expire_time` |

#### IM 体系

| 表名 | 说明 | 关键字段 |
|------|------|----------|
| `im_conversation` | 会话（每用户一份） | `conversation_id`(单聊: smallId_bigId, 群聊: group_{id}), `unread_count`, `is_top`, `is_mute` |
| `im_message` | 消息 | `msg_type`(1文本/2图片/3语音...), `status`(0未送达/1已送达/2已读/3撤回), `client_msg_id`(去重) |
| `im_group` | 群组 | `owner_id`, `max_member`, `member_count` |
| `im_group_member` | 群成员 | `role`(0成员/1管理员/2群主), `is_mute` |

**ORM：** MyBatis-Plus 注解 Mapper，无 XML  
**Mapper 位置：** `jsj-mini/src/main/java/com/zhs/mapper/`

**逻辑删除：** 全局字段 `is_delete`（0=未删, 1=已删）

---

## 8. API 路由清单

### 8.1 jsj-admin（:8080）

#### 认证

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/captchaImage` | 获取验证码 |
| POST | `/login` | 登录 |
| POST | `/register` | 注册 |
| GET | `/getInfo` | 当前用户信息 + 权限 |
| GET | `/getRouters` | 动态菜单路由 |

#### 系统管理（`/system/*`）

| 路径前缀 | Controller |
|----------|------------|
| `/system/user` | `SysUserController` |
| `/system/role` | `SysRoleController` |
| `/system/menu` | `SysMenuController` |
| `/system/dept` | `SysDeptController` |
| `/system/post` | `SysPostController` |
| `/system/config` | `SysConfigController` |
| `/system/dict/type` | `SysDictTypeController` |
| `/system/dict/data` | `SysDictDataController` |
| `/system/notice` | `SysNoticeController` |
| `/system/user/profile` | `SysProfileController` |

#### 监控（`/monitor/*`）

| 路径前缀 | Controller |
|----------|------------|
| `/monitor/online` | `SysUserOnlineController` |
| `/monitor/job` | `SysJobController` |
| `/monitor/jobLog` | `SysJobLogController` |
| `/monitor/operlog` | `SysOperlogController` |
| `/monitor/logininfor` | `SysLogininforController` |
| `/monitor/server` | `ServerController` |
| `/monitor/cache` | `CacheController` |

#### 通用与工具

| 路径 | 说明 |
|------|------|
| `/common/upload` / `/common/download` | 文件上传下载 |
| `/tool/gen/*` | 代码生成 |
| `/test/user` | 测试接口 |
| `/swagger-ui.html` / `/v3/api-docs` | API 文档 |
| `/druid/*` | Druid 监控（profile: druid） |

### 8.2 jsj-mini（:8081）

| 方法 | 路径 | 需登录 | 说明 |
|------|------|--------|------|
| POST | `/api/auth/wx-login` | 否 | 微信小程序登录 |
| POST | `/api/auth/send-code` | 否 | 发送手机验证码 |
| POST | `/api/auth/phone-login` | 否 | 手机号 + 验证码登录 |
| POST | `/api/auth/current-user` | 是 | 获取当前用户 |
| POST | `/api/auth/logout` | 是 | 退出（删 token + 断 WS） |

> 需登录接口由 `LoginInterceptor` 拦截，从 Header 解析 token 注入 `UserContext`。  
> 免登录接口标注 `@NoLoginRequest`。

---

## 9. IM 模块实现

### 9.1 连接地址

```
ws://localhost:9001/ws
```

配置项（`jsj-mini/application.yml` → `im.netty.*`）：

| 配置 | 默认值 | 说明 |
|------|--------|------|
| `port` | 9001 | WS 端口 |
| `websocket-path` | `/ws` | 路径 |
| `boss-threads` | 1 | Boss 线程 |
| `worker-threads` | 4 | Worker 线程 |
| `business-threads` | 16 | 业务线程池 |
| `reader-idle-seconds` | 90 | 读空闲超时（秒） |
| `max-frame-size` | 65536 | 最大帧大小 |
| `allowed-origins` | 环境变量 `IM_ALLOWED_ORIGINS` | Origin 白名单 |

### 9.2 协议格式

所有 WebSocket 通信使用统一 JSON 信封（`ImPacket`）：

```json
{
  "msgType": 1,
  "requestId": "uuid",
  "timestamp": 1714000000000,
  "body": { }
}
```

### 9.3 消息类型（`MsgType`）

| code | 枚举 | 说明 | Handler |
|------|------|------|---------|
| 1 | AUTH | 鉴权 | `AuthHandler` |
| 2 | AUTH_RESP | 鉴权响应 | — |
| 3 | HEARTBEAT | 心跳 | `HeartbeatHandler` |
| 5 | CHAT | 单聊消息 | `ChatHandler` |
| 6 | GROUP_CHAT | 群聊消息 | `GroupChatHandler` |
| 7 | MSG_PUSH | 服务端推送 | — |
| 8 | ACK | 消息确认 | `AckHandler` |
| 9 | SYSTEM | 系统通知 | — |

### 9.4 鉴权流程

1. 客户端 HTTP 登录获取 `token`
2. 建立 WS 连接 `ws://host:9001/ws`
3. 发送 `AUTH` 消息，`body.token` = HTTP 登录 token
4. 服务端从 Redis（`TokenCacheKey`）校验 token → 取 `userId`
5. 设置 Channel 属性，注册到 `ImSessionManager`
6. 返回 `AUTH_RESP`（success / error）

### 9.5 消息状态机

`im_message.status`：

| 值 | 含义 |
|----|------|
| 0 | 未送达 |
| 1 | 已送达 |
| 2 | 已读 |
| 3 | 已撤回 |

客户端通过 `ACK` 消息类型确认送达/已读，服务端更新状态并推送 `MSG_PUSH`。

### 9.6 客户端 IM 工具

| 模块 | 文件 | 说明 |
|------|------|------|
| 微信原生 | `jsj-mini-app/utils/websocket.js` | WS 连接管理 |
| 微信原生 | `jsj-mini-app/utils/im-core.js` | IM 业务逻辑 |
| UniApp | `jsj-uni-app/src/utils/websocket.js` | 同上 |
| UniApp | `jsj-uni-app/src/utils/im-core.js` | 同上 |

---

## 10. 认证与鉴权

### 10.1 jsj-admin（JWT）

- **机制：** Spring Security + JWT Token
- **Token 存储：** Redis（可配置过期时间）
- **权限模型：** RBAC（用户 → 角色 → 菜单/按钮权限 + 数据权限 `@DataScope`）
- **密码策略：** 最大错误 5 次，锁定 10 分钟（`application.yml` → `user.password.*`）

### 10.2 jsj-mini（Token + Redis）

- **机制：** 自定义 Token（UUID），存 Redis，Header 传递
- **Token 解析：** `TokenUtil.resolveFrom(request)` → Redis `TokenCacheKey` → userId
- **拦截器：** `LoginInterceptor` 注入 `UserContext`
- **WS 复用：** IM 鉴权同样校验 Redis 中的 token

#### 微信登录流程

```
客户端 wx.login() → code
  → POST /api/auth/wx-login { code, nickname?, avatar? }
  → WeChatMiniService.code2session → openId
  → findOrCreateByOpenId → issueToken
  → 返回 { token, userId, nickname, avatar }
```

#### 手机号登录流程

```
POST /api/auth/send-code { phone }
  → Redis 存验证码 + 发送频率限制
POST /api/auth/phone-login { phone, code }
  → 校验验证码 → findOrCreateByPhone → issueToken
  → 返回 { token, userId, ... }
```

#### Redis 缓存 Key

| Key 类 | 用途 |
|--------|------|
| `TokenCacheKey` | token → userId |
| `SmsCodeCacheKey` | 手机号 → 验证码 |
| `SmsSendLimitCacheKey` | 发送频率限制 |
| `NewUserLockCacheKey` | 新用户创建分布式锁 |

---

## 11. 配置文件

### 11.1 jsj-admin

| 文件 | 说明 |
|------|------|
| `application.yml` | 端口 8080、Redis、JWT、MyBatis、SpringDoc、XSS、虚拟线程、文件路径 |
| `application-druid.yml` | Druid 数据源（profile: druid） |
| `generator.yml` | 内置代码生成作者/包名/表前缀 |
| `logback.xml` | 日志配置 |

关键配置项：

```yaml
ruoyi:
  name: JSJ-Admin
  version: 3.9.2
  profile: D:/jsj/uploadPath    # 文件上传路径
  captchaType: math             # 验证码类型

server:
  port: 8080
```

### 11.2 jsj-mini

| 文件 | 说明 |
|------|------|
| `application.yml` | 端口 8081、数据源、Redis、微信、IM Netty |

**环境变量：**

| 变量 | 说明 |
|------|------|
| `DB_HOST` / `DB_NAME` / `DB_USERNAME` / `DB_PASSWORD` | 数据库连接 |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | Redis |
| `WX_APP_ID` / `WX_APP_SECRET` | 微信小程序 |
| `IM_ALLOWED_ORIGINS` | WS Origin 白名单 |

### 11.3 jsj-admin-ui

| 文件 | 说明 |
|------|------|
| `.env.development` | `VITE_APP_BASE_API=/dev-api`，代理到 `localhost:8080` |
| `.env.production` / `.env.staging` | 生产/预发 |
| `vite.config.ts` | 开发服务器、代理、拆包 |

### 11.4 移动端

| 文件 | 说明 |
|------|------|
| `jsj-mini-app/config.js` | `baseUrl`, `wsUrl`, `testLoginEnabled` |
| `jsj-uni-app/src/config/index.js` | 按 NODE_ENV 切换 dev/prod |

---

## 12. 启动与开发流程

### 12.1 环境要求

- JDK 21+
- Node.js 18+
- MySQL 8.0+
- Redis 6+

### 12.2 数据库初始化

```bash
# 按顺序导入（脚本内已带 CREATE DATABASE / USE）
mysql -u root -p < sql/admin.sql    # admin 主表 → ry-vue
mysql -u root -p < sql/quartz.sql         # admin 定时任务 → ry-vue
mysql -u root -p < sql/mini.sql       # 小程序用户 → jsj_mini
mysql -u root -p < sql/jsj_mini_im.sql    # IM 四表 → jsj_mini
```

### 12.3 启动顺序

```bash
# 1. 管理后端
cd jsj-admin && mvn spring-boot:run          # :8080

# 2. 管理前端
cd jsj-admin-ui && npm install && npm run dev  # :80

# 3. 小程序后端
cd jsj-mini && mvn spring-boot:run           # :8081 + WS :9001

# 4. 微信原生小程序
# 微信开发者工具打开 jsj-mini-app/

# 5. UniApp
cd jsj-uni-app && npm install && npm run dev:h5

# 6. 代码生成器（可选）
cd generator && mvn javafx:run
```

### 12.4 新人导航路径

| 目标 | 阅读顺序 |
|------|----------|
| 管理端 CRUD | `jsj-admin-ui/src/api` → `jsj-admin/web/controller` → `system/service` → `mapper/*.xml` |
| 移动端登录 | `jsj-uni-app/src/api/auth.js` → `jsj-mini/controller/AuthController` → `service/impl/AuthServiceImpl` |
| IM 聊天 | `utils/im-core.js` → `jsj-mini/netty/` → `sql/jsj_mini_im.sql` |
| 扩业务表 | `generator` 桌面工具 或 admin `/tool/gen` |
| 权限体系 | `framework/config/SecurityConfig` → `SysPermissionService` → `permission.ts` |

---

## 13. 代码生成

项目提供两套代码生成能力：

### 13.1 桌面生成器（generator）

- **技术：** JavaFX + Freemarker
- **入口：** `mvn javafx:run`
- **模板：** `generator/template/*.ftl`
- **输出：** Controller、Service、Dao、Entity、DTO、Req/Resp、多数据库 Mapper、Mongo 系列

### 13.2 管理端内置生成（jsj-admin）

- **入口：** 管理 UI → 系统工具 → 代码生成，或 API `/tool/gen/*`
- **技术：** Velocity 模板
- **模板：** `jsj-admin/src/main/resources/vm/`

| 模板目录 | 输出 |
|----------|------|
| `vm/java/` | controller、service、serviceImpl、domain、mapper、sub-domain |
| `vm/xml/` | mapper.xml |
| `vm/vue/` / `vm/vue/v3/` / `vm/vue/v3ts/` | Vue 2/3 页面 |
| `vm/ts/` | TypeScript API + 类型 |
| `vm/js/` | JavaScript API |
| `vm/sql/` | DDL 脚本 |

- **元数据表：** `gen_table` / `gen_table_column`（存于 `ry-vue`）
- **配置：** `generator.yml`（作者、包名、表前缀）

---

## 14. 测试与 CI/CD

| 模块 | 测试依赖 | 实际测试代码 |
|------|----------|--------------|
| jsj-admin | `spring-boot-starter-test` | 无 `src/test` |
| jsj-mini | JUnit Jupiter 5.10.2 | 无 `src/test` |
| generator | JUnit 4.13.2 | 仅 `view/Test.java`（调试） |
| jsj-uni-app | `@dcloudio/uni-automator` | 无测试脚本 |
| jsj-admin-ui | 无 | 无 |

**CI/CD：** 无 `.github/workflows`、无 Docker / docker-compose 配置。

---

## 15. 已知缺口与注意事项

### 15.1 配置陷阱

| 问题 | 说明 |
|------|------|
| `jsj-mini-app/config.js` baseUrl | 默认为 `:8080`，应改为 `:8081` |
| `jsj-mini` 默认 DB_NAME | 配置为 `chat`，SQL 脚本建库为 `jsj_mini`，需对齐 |
| 生产密钥 | JWT secret、DB 密码、微信 AppSecret 必须通过环境变量注入 |

### 15.2 未完成功能

- 手机验证码：开发环境仅日志打印，真实短信服务商待接入（`AuthServiceImpl` TODO）
- 自动化测试：各模块均无实际测试代码
- 文档：`README.md` 引用的 `docs/im-module.md`、`docs/uniapp-architecture.md` 尚未创建

### 15.3 架构特点

- **无根级聚合 POM** — 各 Java 模块独立 Maven 工程
- **双库隔离** — `ry-vue`（管理）与 `jsj_mini`（移动端/IM）完全分离
- **双 ORM 风格** — admin 用 MyBatis XML，mini 用 MyBatis-Plus 注解
- **IM 与 HTTP 共用 Token** — Redis 统一校验，退出登录同时断 WS

---

## 附录：入口文件速查

| 模块 | 入口文件 |
|------|----------|
| generator | `generator/src/main/java/com/szmirren/Main.java` |
| jsj-admin | `jsj-admin/src/main/java/com/zhs/JsjAdminApplication.java` |
| jsj-admin-ui | `jsj-admin-ui/src/main.ts` |
| jsj-mini | `jsj-mini/src/main/java/com/zhs/MiniApplication.java` |
| jsj-mini-app | `jsj-mini-app/app.js` |
| jsj-uni-app | `jsj-uni-app/src/main.js` |

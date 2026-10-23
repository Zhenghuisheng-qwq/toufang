# jsj-mini — H5 后端（保留移动端 / IM 能力）

面向唯一 H5 前端 `jsj-mini-app`。H5 使用 `/api/h5` 同源 Cookie；既有移动端 Bearer Token 和 IM 能力独立保留。jsj-h5、jsj-uni-app、根 generator 保持删除。

H5 接口与开关说明见根 README、config-examples/README；本次仅纠正前端归属，不重建后端或数据库。

## 技术栈

- Java 21 + Spring Boot 3.5
- MyBatis-Plus 3.5（分页 / 防全表 / 乐观锁 / 逻辑删除）
- Redis（Token、验证码、群成员缓存）
- Druid 连接池
- Netty 4.1（WebSocket IM）

## 启动

```bash
# 本地开发
mvn spring-boot:run

# 或在 IDE 中运行 MiniApplication.java
```

端口：
- HTTP API：`8881`
- WebSocket：`9001`（路径 `/ws`）

## 配置文件

`application.yml` 负责端口 / 微信 / IM；数据源与 Redis 通过 `spring.config.import` 引入 `jsj-service` 的 `application-common.yml`。敏感项可用环境变量覆盖：

| 环境变量 | 含义 | 默认 |
|---|---|---|
| （见 `jsj-service` 的 `application-common.yml`） | 数据库连接 | BOOT_DB_* 或已忽略本地配置 |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | Redis | 本地授权配置，见根配置说明 |
| `WX_APP_ID` / `WX_APP_SECRET` | 微信小程序 | 占位符 |
| `IM_ALLOWED_ORIGINS` | WebSocket 允许的 Origin（逗号分隔） | 空=不校验 |

多环境可通过 `-Dspring.profiles.active=prod` + 新增 `application-prod.yml` 的方式扩展。

## 接口列表

| 方法 | 路径 | 说明 | 需登录 |
|------|------|------|--------|
| POST | `/api/auth/wx-login` | 微信小程序登录（code2Session） | 否 |
| POST | `/api/auth/send-code` | 发送手机验证码 | 否 |
| POST | `/api/auth/phone-login` | 手机号+验证码登录 | 否 |
| POST | `/api/auth/current-user` | 获取当前登录用户 | 是 |
| POST | `/api/auth/logout` | 退出登录（同时断开 WebSocket） | 是 |

## IM WebSocket

- 协议：WebSocket（`ws://localhost:9001/ws`），生产建议 `wss://` 前置 Nginx
- 鉴权：连接后首帧发送 `AUTH` 携带 token（由 `/api/auth/*` 颁发）
- 消息类型：文本 / 图片 / 语音 / 视频 / 文件 / 系统
- 能力：单聊、群聊、心跳、ACK、消息状态机（未送达→已送达→已读）
- 客户端接入与协议细节见 [`docs/im-module.md`](../docs/im-module.md)

## 认证方式

推荐请求头：`Authorization: Bearer <token>`

Token 存于 Redis，有效期 30 天；兼容老客户端还可读 `token` / `access-token` 头。

## 数据库

库名：`jsj_mini`（由 `sql/mini.sql` 和 `sql/jsj_mini_im.sql` 自动建库）

- 用户体系：`user` / `user_sensitive_info` / `user_login_session`
- IM：`im_conversation` / `im_message` / `im_group` / `im_group_member`

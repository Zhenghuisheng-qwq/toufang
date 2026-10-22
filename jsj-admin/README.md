# jsj-admin - 后台管理后端

基于 RuoYi-Vue 升级到 Spring Boot 3 + JDK 21 的后台管理系统后端。

## 技术栈

- Java 21 + Spring Boot 3.5（开启虚拟线程）
- Spring Security + JWT
- MyBatis + PageHelper
- Redis（会话、缓存）
- Druid（连接池 + 监控，监控页需登录）
- SpringDoc OpenAPI（API 文档）
- Quartz（定时任务）

## 启动

```bash
# 本地开发
mvn spring-boot:run

# 或在 IDE 中运行 JsjAdminApplication.java
```

默认端口：`8080`

## 配置文件

当前仅 `application.yml` + `application-druid.yml`（通过 `spring.profiles.active: druid` 激活）。敏感项建议通过环境变量覆盖：

| 环境变量 | 含义 |
|---|---|
| `DB_HOST` / `DB_NAME` / `DB_USERNAME` / `DB_PASSWORD` | 主库数据源 |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | Redis |
| `JWT_SECRET` | JWT 签名密钥（建议 ≥64 字节随机字符串） |
| `DRUID_USERNAME` / `DRUID_PASSWORD` | Druid 监控台账号 |

多环境部署可通过新增 `application-prod.yml` + `-Dspring.profiles.active=prod,druid` 扩展。

## API 文档

启动后访问：http://localhost:8080/swagger-ui.html

（默认 swagger 扫描包在 `application.yml` 的 `springdoc.group-configs` 中，按需调整 `packages-to-scan` 扩大范围。）

## 默认账号

| 账号 | 密码 | 角色 |
|------|------|------|
| admin | admin123 | 超级管理员 |

## 数据库

库名：`ry-vue`（由 `sql/admin.sql` 自动建库；`sql/quartz.sql` 同库建表）。

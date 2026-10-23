# 当前项目索引

| 模块 | 归属与入口 |
| --- | --- |
| jsj-mini-app | 唯一 H5 前端；src/main.ts、Landing.vue、Order.vue；/pages/:id、/orders/:no |
| jsj-mini | H5 后端；H5Controller、PaymentNotificationController、WecomCallbackController；HTTP8881 |
| jsj-service | 配置、订单、报告、支付、企微、广告领域及 Mapper |
| jsj-admin / jsj-admin-ui | 管理端权限、配置、报告、业务查询；默认8080 |
| jsj-common | 共享公共能力 |

jsj-h5、jsj-uni-app、根 generator 保持删除。原小程序入口已从 jsj-mini-app 移除；保留后端原移动端/IM API 不影响 H5 独立凭证。

H5 在 jsj-mini-app 执行 npm ci --ignore-scripts、npm run dev、npm run typecheck、npm run build。生产产物 jsj-mini-app/dist；后端与同源代理配置见根 README 和 config-examples/README.md。

本次验收见 ../docs/codex-h5-module-correction.md。历史业务实现见 ../docs/codex-business-flow-review.md；旧脚手架 API/IM 索引保存在 project-index-legacy.md，仅作历史参考，不作为当前模块、凭据、数据库或启动指引。

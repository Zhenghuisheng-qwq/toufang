# H5 工程归属纠正交接记录

日期：2026-10-23。起始提交 `695a154`，工作区起始干净。

## 当前归属与实际改动

用户确认：**jsj-mini 是 H5 后端，jsj-mini-app 是唯一 H5 前端。** 之前保护原生小程序、另建 jsj-h5 的指令撤销。jsj-h5、jsj-uni-app、根 generator 均保持删除。

当前 checkout 和可见 Git 历史没有可直接提取的 jsj-h5 源文件，因此依据上一轮业务实现和当前真实 H5 API，在正确目录落实页面，而不是声称完成了未经核实的文件搬迁。没有创建平行工程。

- jsj-mini-app 新增 Vue3 / TypeScript / Vite 入口、路由、同源 API 客户端、落地页表单、订单/支付/OAuth/报告/企微入口页面，以及自己的 package.json、锁文件和构建配置。
- 移除原 app.js/app.json/app.wxss、WXML/WXSS 页面、wx.request 登录封装、config.js、微信工程配置和仅供 IDE 识别的 POM。使用浏览器/npm，不再使用微信开发者工具运行此目录。
- 保留服务端定价、Cookie 所有权、幂等键和现有 API 契约。出生时间可留空；双击防重复；当前标签页刷新/返回恢复既有订单。已创建订单的恢复不依赖页面继续发布。
- 报告和老师入口分别读取；老师失败不遮挡报告；退款或凭证失效时清除页面中的报告正文。客户端不会将支付跳转成功当作入账。
- README、AGENTS、CLAUDE、Cursor 入口说明、模块说明和 Nginx 产物路径统一到 jsj-mini-app。旧长篇脚手架索引归档为 doc/project-index-legacy.md，明确标记为历史；当前索引改为正确模块导航。

**本轮操作未改后端 Java、POM、数据库 SQL 或运营后台源码。未执行数据库增量、重装数据库、后台 CRUD 验收、真实支付或外发。** 历史报告 `codex-business-flow-review.md` 及旧截图保持原样；其中 jsj-h5 的验证不计入本轮结果。

## 依赖与构建

初始复用原前端版本时，npm 官方审计返回 3 项高风险（包含传递关系）。只将新模块的 Vue 和 Vite 调整到同系列补丁版本，没有改动 Admin UI 或其他模块锁文件，没有执行 audit fix --force。

最终版本为 Vue 3.5.44、Vite 6.4.5，其他直接依赖沿用已有系列。版本已核对 [Vue 官方发布](https://github.com/vuejs/core/releases/tag/v3.5.44) 和 [Vite 官方发布](https://github.com/vitejs/vite/releases/tag/v6.4.5)。本机 Node 22.18.0、npm 10.9.3。

以下均在 **jsj-mini-app** 重新实际执行：

| 验证 | 结果 |
| --- | --- |
| npm install --ignore-scripts（官方 registry） | 成功，生成该模块锁文件 |
| npm ci --ignore-scripts（官方 registry） | 成功，验证锁文件可复现安装 |
| npm run typecheck | 全量 vue-tsc 通过 |
| npm run build | 类型检查和生产构建通过，产物 jsj-mini-app/dist |
| npm audit --registry=https://registry.npmjs.org | 0 vulnerabilities；仅代表本次审计结果 |
| 开发服务器 | Vite 开发入口及真实 API 代理检查通过 |
| 生产包浏览器 | Chrome 17 项断言通过，未借用旧工程结果 |

本机默认 npm 镜像的 audit API 返回 404，不被当成审计通过；切换官方审计端点后获得上述结果。

## 浏览器与 API 验证边界

测试源：`jsj-mini-app/tests/browser-check.mjs`。测试模拟逻辑仅在 tests 中由独立 Chrome CDP 拦截请求，不进入 src 或生产 dist，也没有增加后端模拟支付接口。

**真实联通部分：** 启动已保留的 Mini 包，关闭支付/OAuth/退款、企微回调/外发和所有 Worker。浏览器通过 H5 生产预览的同源 `/api` 代理访问 `/api/h5/pages/999999999`，收到现有控制器的“页面不存在”响应（HTTP200、业务码500）。这证明代理、后端路由及只读查询路径连通，不代表已有可上线的发布页，也不代表新工程真实下单验证通过。

本机已有的 8881 实例此前返回通用“系统异常”，18881 对该路径不可用；未修改或停止这两个非本轮进程。使用本轮独立 63410 Mini 实例才得到明确业务响应。不能据此声称默认端口上的旧实例已修复。

**浏览器契约部分：** 生产包来自 jsj-mini-app/dist，使用浏览器测试响应验证：

- 390、768、1024、1440 四种宽度无横向溢出；人工查看移动、桌面、报告截图。
- 空表单阻止提交；出生时间留空；百度 bd_vid 保留；下单请求不携带前端金额。
- 双击只产生一次下单请求；刷新与返回恢复同一订单。
- 支付未开放时保留订单；报告可在老师入口失败时独立显示。
- 退款状态、错误凭证不显示报告正文；页面未发布/配置不足不显示提交表单。
- 页面无浏览器运行时异常。

这里的下单、已支付、报告、退款等状态是**浏览器契约模拟**，不包装成真实数据库或微信实机验收。本轮没有写业务数据，没有复跑既有后台的整轮 CRUD。

证据：[17 项结果](evidence/h5-module-correction/result.json)、[移动落地页](evidence/h5-module-correction/landing-390.png)、[桌面落地页](evidence/h5-module-correction/landing-1440.png)、[模拟已付报告](evidence/h5-module-correction/report-390.png)。

## 启动与部署

通常先准备可用的 jsj-mini 实例，再运行：

```powershell
cd jsj-mini-app
npm ci --ignore-scripts
npm run dev
# http://127.0.0.1:5174/pages/已发布页面ID
npm run typecheck
npm run build
npm run preview
# 本机生产包预览 4174；产物 dist/
```

API 固定同源 `/api/h5`。开发/预览代理默认 `http://127.0.0.1:8881`。若默认端口被旧实例占用，可在项目根使用独立 Mini 端口（先确认本地配置和全部开关）：

```powershell
java -jar jsj-mini/target/jsj-mini-1.0.0.jar --server.address=127.0.0.1 --server.port=63410 --acq.payment.enabled=false --acq.report.worker.enabled=false --acq.post-payment.worker.enabled=false --acq.ads.worker.enabled=false --acq.wecom.automation.worker.enabled=false
# 另一个 PowerShell，在 jsj-mini-app 目录：
$env:H5_API_TARGET='http://127.0.0.1:63410'
npm run dev
```

也可复制 `.env.example` 为被忽略的 `.env.local` 设置 H5_API_TARGET；不要给前端注入秘密。开发域名通过 H5_ALLOWED_HOSTS 显式允许，并解析到本机。后端仍要求域名已核验、已关联发布页和授权配置，localhost 不会绕过校验。

生产静态目录为 jsj-mini-app/dist，Vue history 路由回退 index.html，同域 `/api` 转发 Mini。参考 config-examples/h5-nginx.conf.example。Vite preview 不是生产部署服务。

浏览器测试需专用 Chrome CDP、已启动预览和可用 Mini：设置 H5_TEST_URL、H5_TEST_CDP 后运行 `node tests/browser-check.mjs`。默认输出在 `.local/h5-module-verification`。测试会先执行真实只读接口，再在专用标签页拦截业务请求。

## 结束状态与后续业务

本次临时端口为 Mini63410、预览63411、Chrome63412、开发服务器63413。验收结束停止自有进程，原8881/18881保持不动。无业务测试数据需清理，保留本地配置和正常构建产物。未提交、推送或部署。

收尾时工作区出现本轮未操作的 `sql/`、后端测试及 `.tmp_diff2.py` 等并发变更，全部原样保留。全仓 diff 检查报告 sql/admin.sql 末尾空行，未为前端验收擅自修改。上述并发变更不包含在本轮构建/API 检查结论中。

工程归属纠正已落实。后续沿 jsj-mini-app/jsj-mini 继续既定业务，不另起架构审计。仍有真实微信/公众号/企微/广告联调、UNKNOWN 运营对账入口、部分退款规则与实现、获客助手精确关联、正式报告规则等业务缺口；这些没有因前端目录纠正而被标记完成。

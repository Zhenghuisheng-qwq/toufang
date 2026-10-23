# jsj-mini-app — 投放 H5 前端

这是 jsj-mini 的唯一 H5 前端。原生小程序入口 app.js/app.json、WXML/WXSS、wx.request 登录及 IDE 占位 POM 已移除；无需微信开发者工具，不创建平行 H5 工程。

```powershell
cd jsj-mini-app
npm ci --ignore-scripts
npm run dev
npm run typecheck
npm run build
npm run preview
```

Node 18+（本机 Node 22），依赖精确版本与 lock 文件固定。开发 5174，生产包预览 4174，均只监听 127.0.0.1；生产产物 dist/。主入口 src/main.ts，路由 /pages/:id、/orders/:no；根路径也支持 ?page=页面ID。

接口固定同源 /api/h5，对接 jsj-mini，默认代理 http://127.0.0.1:8881。需要改开发代理时将 .env.example 复制为已忽略的 .env.local，设置 H5_API_TARGET。已授权自定义域名写 H5_ALLOWED_HOSTS 并解析到本机。保留 Host/Origin；不会因本地开发自动绕过后端域名核验。localhost 没有关联已发布业务页面时显示后端拒绝原因是正常的。

使用 HttpOnly Cookie 访问订单，不沿用小程序 Bearer token 或 wx.login。请求键及访问/线索/订单 ID 只存当前标签页 sessionStorage；订单金额、商户和支付状态由后端决定。公众号 OAuth/JSAPI 和微信 H5 均调用现有后端；支付成功只等待服务端确认。

生产使用 HTTPS 静态托管和同源 /api 反代，参考 ../config-examples/h5-nginx.conf.example；Vue history 路由必须回退 index.html。Vite preview 仅用于本机验证，不作为生产服务。

本轮验证独立记录在 ../docs/codex-h5-module-correction.md；历史 jsj-h5 构建和浏览器结果不改名复用。真实支付/企微/广告联调与后台未完成项见历史业务报告，目录纠正不改变这些边界。

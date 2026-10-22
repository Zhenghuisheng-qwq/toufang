---
name: senior-frontend
description: Senior Frontend Engineering workflow - production-grade UI implementation, responsive design, CSS quality, and visual verification
user-invocable: false
environments:
  - local
---

# Senior Frontend Engineering

<Purpose>
本 Skill 定义了 Senior Frontend Engineering 的完整工作流程和质量标准，确保 AI 生成的前端代码达到生产级标准：功能正确、视觉成熟、响应式、可维护、且经过正确验收。

**职责边界：**
- **senior-frontend（本 Skill）：** How should we build it correctly?（如何正确构建）
- **ui-design（设计 Skill）：** What should it look like?（应该如何呈现）

本 Skill 负责实现设计，而非创造设计。优先消费现有设计输入（Design/Screenshot/Design System/ui-design 输出）。

这不是关于教 AI 如何写 Vue/React 语法，而是建立从设计到实现的工程流程。
</Purpose>

<Use_When>
适用于非平凡前端任务：
- 新前端页面开发
- UI 页面实现
- 截图/设计稿还原
- 页面重构或视觉优化
- Responsive 设计调整
- CSS/Layout 问题排查
- Component 设计决策
- 前端架构调整
- 页面交互状态实现
- 前端性能优化
- Visual Review
- 前端 Code Review
</Use_When>

<Do_Not_Use_When>
对于平凡修改跳过完整流程：
- 拼写错误修复
- 单行文本修改
- 常量调整
- 单个类型错误修复
- 明确孤立的单行修复

避免：改一个字符串却重新审计整个前端架构。

同样跳过：
- Backend 任务
- 纯 API 开发
- 数据库操作
- 非 UI 工作
</Do_Not_Use_When>

<Why_This_Exists>
AI 生成的前端代码常见问题：
- "能用就行"心态，缺乏视觉成熟度
- 基于截图堆砌 HTML/CSS
- 到处是魔法数字
- 过度使用绝对定位
- 随机 margin-left/margin-top/translate 调整
- CSS 补丁无限堆叠
- 桌面端可用但 Responsive 崩溃
- 只实现 Happy Path
- 随意提取 Component
- 过度抽象
- Typography 不一致
- 间距不一致
- 信息层级混乱
- 卡片堆叠无呼吸感
- 不自然的空白
- 缺乏商业产品光泽
- "Build passed = 任务完成"假设
- 没有实际 Browser Review

本 Skill 通过系统化工程规范消除这些反模式。
</Why_This_Exists>

<Workflow>
对于非平凡前端任务，执行此工作流程：

1. **实施前审计（Pre-Implementation Audit）** — 理解当前代码库基线
2. **Layout 分析（Layout Analysis）** — 编码前选择布局模型
3. **Component 与状态设计（Component & State Design）** — 规划结构和交互
4. **实施（Implementation）** — 遵循项目约定编写代码
5. **Type Check & Build** — 验证编译
6. **视觉验收（Visual Verification）** — 在浏览器中打开，测试多视口
7. **Senior Frontend Review** — 像审查别人的代码一样审查
8. **修复 P0/P1 问题** — 处理关键和重大问题
9. **重新验证（Re-verify）** — 确认修复有效
10. **READY** — 所有标准满足

不要为每个步骤生成冗长文档。内部完成它们。仅向用户传达关键发现和决策。
</Workflow>

<!-- ═══════════════════════════════════════════════════════════════════════════ -->
<!-- 核心工程规则                                                                 -->
<!-- ═══════════════════════════════════════════════════════════════════════════ -->

<Core_Rules>

## 1. 实施前审计（Pre-Implementation Audit）

在编写任何非平凡前端任务代码之前，理解当前基线。

**最小检查项：**
- 当前页面（如果修改现有页面）
- 父 Layout
- Router 配置
- Store/State 管理
- API 集成
- 全局 CSS
- Design Tokens
- 现有 Components
- 现有 Composables/Hooks
- Responsive 规则
- Typography 系统
- 间距系统
- 现有 UI Library

**原则：** 优先复用现有能力。

**绝不在未检查是否已存在的情况下创建：**
- Button, Card, Modal, Tabs, Table, Pagination
- Typography 系统
- Layout 系统
- Design tokens

**绝不为每个页面重新发明设计系统。**

## 2. Layout 工程（Layout Engineering）

在实施页面之前，确定布局模型。

**考虑：**
1. 页面主布局结构
2. 主容器最大宽度
3. 页面内边距
4. Flexbox vs CSS Grid
5. 正常文档流
6. Sticky/Fixed 定位
7. absolute 的适当使用
8. 自适应宽度 vs 固定尺寸
9. 内容换行
10. 长文本处理
11. 溢出行为
12. 列数变化（responsive）

**优先使用：**
- 正常文档流（Normal Document Flow）
- Flexbox
- CSS Grid

**仅对真正需要定位的元素使用 `position: absolute`：**
- 遮罩层（Overlays）
- 徽章（Badges）
- 装饰元素（Decorations）
- 浮动控件（Floating controls）
- 特殊定位需求

**禁止模式：**
```css
/* 禁止：用随机偏移强制元素定位 */
.element {
  margin-left: 47px;  /* 为什么是 47？ */
  margin-top: 23px;   /* 为什么是 23？ */
  position: absolute;
  left: 234px;        /* 硬编码匹配截图 */
  top: 567px;
  transform: translate(-13px, 8px);  /* 更多魔法 */
}
```

**禁止思维：**
"截图是 1440px 宽，所以我将硬编码所有宽度来匹配。"

使用响应式、基于流的布局，适应内容和视口。

## 3. UI 根因分析（Root Cause Analysis for UI Problems）

这是**核心规则**。

当发现 UI 问题时：
- 错位
- 过于拥挤
- 过于宽松
- 重叠
- 溢出
- 卡片错位
- 文本换行问题
- Responsive 崩溃
- Footer 错位
- Fixed 元素错位

**不要立即添加 CSS 补丁。**

**首先确定根本原因：**
1. Layout Model 问题？
2. Container Width 问题？
3. Grid Distribution 问题？
4. Flex Distribution 问题？
5. 父元素对齐问题？
6. Typography 问题？
7. Line Height 问题？
8. Content Flow 问题？
9. Responsive Breakpoint 问题？
10. Component Structure 问题？
11. Design Token 问题？
12. DOM Structure 问题？

**如果是结构性问题：修复根本原因。**

**不要无限堆叠补丁：**
```css
/* 反模式：无限补丁 */
.element { margin-top: 10px; }
.element { margin-top: 13px !important; }  /* 仍然错误 */
.element { margin-top: 16px !important; transform: translateY(-2px); }  /* 绝望 */
```

## 4. 视觉层级验证（Visual Hierarchy Verification）

页面实现必须保真设计的信息层级。

**验证项：**
- Page Title 视觉突出
- Section Title 明确区分
- Primary/Secondary/Supporting 内容层级清晰
- 核心数据醒目
- 状态指示器可见
- Primary/Secondary Actions 层级明确

**如果存在设计输入（Design/Screenshot/ui-design）：**
- 必须忠实还原设计的视觉层级
- 不得擅自改变信息优先级

**如果无设计输入：**
- 根据业务逻辑合理建立层级
- 避免所有内容视觉权重相同

**禁止：所有内容看起来一样**
```css
/* 反模式：无层级 */
.title, .content, .label, .value {
  font-size: 14px;
  color: #333;
  font-weight: 400;
}
```

**关键：**
本规则负责验证层级实现正确性，不负责定义层级体系（由 ui-design 负责）。

## 5. Typography 实现（Typography Implementation）

**优先消费现有 Typography System。**

**检查顺序：**
1. 项目 Design Tokens（CSS Variables / SCSS Variables）
2. UI Library 提供的 Typography
3. ui-design 定义的 Typography System
4. 设计稿标注的字体规范

**如果已有 Typography System：**
```
必须复用，禁止随意创建新字号。
```

**验证项：**
- font-size 符合体系
- font-weight 正确
- line-height 合理
- letter-spacing 正确（如有定义）
- text color 符合语义（primary/secondary/muted）
- 长文本换行正常
- Responsive 下字号合理调整

**禁止：**
```css
/* 反模式：随意创建字号 */
.element-1 { font-size: 13px; }
.element-2 { font-size: 15px; }
.element-3 { font-size: 17px; }
.element-4 { font-size: 19px; }
```

**关键：**
本规则负责正确实现 Typography，不负责定义 Typography System（由 ui-design 负责）。

## 6. 间距实现（Spacing Implementation）

**优先消费现有 Spacing System。**

**检查顺序：**
1. 项目 Design Tokens（--spacing-* / $spacing-*）
2. UI Library 提供的 spacing utilities
3. ui-design 定义的 Spacing System
4. 设计稿标注的间距规范

**如果项目已有 Spacing System：**
```
4, 8, 12, 16, 20, 24, 32, 40, 48
```

**必须复用它们。**

**如果遇到非标准尺寸（13px, 17px, 19px, 27px）：**

**首先确定来源：**
- 设计规范要求
- 视觉对齐需求
- 浏览器渲染修复
- 现有 Token
- 遗留约束

**如果无设计依据：**
收敛到统一 Spacing System。

**禁止：无理由的随机魔法数字。**

**关键：**
本规则负责正确使用 Spacing Tokens，不负责定义 Spacing System（由 ui-design 负责）。

## 7. Responsive 实现（Responsive Implementation）

**优先消费现有 Responsive 策略。**

**检查顺序：**
1. 项目现有 breakpoints 定义
2. ui-design 定义的 Responsive Information Strategy
3. 设计稿标注的响应式规范
4. UI Library 提供的 responsive utilities

**如果项目已定义 breakpoints：**
```javascript
breakpoints: {
  mobile: 768,
  tablet: 1024,
  desktop: 1440
}
```

**必须复用它们。**

**如果项目无统一标准，Visual Review 必须至少覆盖：**
- 1440 (Desktop)
- 1024 (Tablet)
- 768 (Tablet/Large Mobile)
- 390 (Mobile)

**实现职责：**

根据 ui-design 定义的信息组织策略，使用正确的 CSS 技术实现：
- Media Queries
- Flexbox (flex-wrap, flex-direction)
- CSS Grid (grid-template-columns 调整)
- Container Queries（如适用）
- min/max-width
- Overflow 处理
- Hidden utilities

**表格 Responsive 实现：**
- 横向滚动（Horizontal Scroll）
- 隐藏列（Hidden Columns）
- 紧凑布局（Compact Layout）
- 响应式列优先级

**Header Responsive 实现：**
```
Desktop Navigation → Compact Navigation → Mobile Navigation
```

**关键：**
本规则负责技术实现，Responsive Information Strategy 由 ui-design 定义。

## 8. Component 设计（Component Design）

基于以下因素提取 components：
- **可复用性（Reusability）**
- **业务语义（Business Semantics）**
- **独立职责（Independent Responsibility）**
- **复杂度（Complexity）**
- **可维护性（Maintainability）**

**好的示例：**
```
MatchCard
UserProfileCard
OrderStatus
EmptyState
Pagination
```

这些具有清晰的业务或通用语义。

**避免创建大量：**
```vue
<Box />
<Wrapper />
<Container1 />
<Container2 />
<LeftBox />
<RightBox />
<TopWrapper />
```

没有清晰职责的 components。

**同样禁止：**
"为了节省几十行代码而将所有内容提取为 components。"

**Component 设计目标：**
- 高内聚（High cohesion）
- 低耦合（Low coupling）
- 语义清晰（Clear semantics）

不是：Components 越多 = 工程越好。

**关键：**
本规则负责代码组件架构，Component Visual Language（视觉规范）由 ui-design 定义。

## 8. 交互状态（Interaction State）

页面不能只实现 Happy Path。

**根据业务场景检查：**
- Loading
- Empty
- Error
- Disabled
- Hover
- Active
- Focus
- Submitting
- Success
- Failed
- Permission Denied

**示例 - 请求流程：**
```
Request
  → Loading
  → Success / Empty / Error
```

**提交流程：**
```
Click
  → Submitting（防止重复提交）
  → Success / Failed
  → Recover / Retry
```

**不要为了形式主义给所有 components 添加所有状态。**

根据实际业务需求确定。

## 9. CSS 质量（CSS Quality）

**禁止模式：**
```css
/* 反模式：覆盖级联 */
.xxx { color: red; }
.xxx { color: blue; }
.xxx { color: green; }
.xxx { color: yellow !important; }
```

**避免：**
- 深层选择器嵌套（>3 层）
- 过度使用 !important
- 重复样式
- 无意义的魔法数字
- 重复的 media queries
- 随机 z-index 值
- 死 CSS
- CSS 补丁堆叠

**CSS 问题：优先修复结构。**

仅在真正需要时使用局部补丁。

## 10. Accessibility

根据实际页面场景考虑：
- 语义化 HTML（`<button>`, `<a>`, `<nav>` 等）
- Button vs link 语义
- 图片 Alt 文本
- 输入框 Label
- 键盘导航
- Focus 指示器
- Disabled 状态
- ARIA 属性（需要时）

**不要所有交互都用 `<div>` + click。**

但也不要追求理论上的完整 WCAG 合规性而过度复杂化普通业务页面。

**遵循合理的 Production 标准。**

## 11. Performance

根据实际问题检查：
- 图片大小
- 懒加载（Lazy Loading）
- 大列表
- 虚拟滚动（Virtual Scrolling）
- 重复请求
- 不必要的 Watchers
- 不必要的 Computed Properties
- 不必要的重新渲染
- Bundle Size
- 大型依赖

**原则：不要过早优化。**

仅处理：
- **明确存在的问题**
- **明显可能导致问题的情况**

## 12. 修改现有页面的范围边界（Scope Boundary）

**UI 任务禁止擅自修改：**
- API Contract
- Router
- Permissions
- Store
- Business Logic
- Domain Rules

除非当前任务明确要求。

**禁止：**
"为了改一个 UI 元素，我重构整个模块。"

**禁止：Scope Creep（范围蔓延）。**

</Core_Rules>

<!-- ═══════════════════════════════════════════════════════════════════════════ -->
<!-- 验证                                                                         -->
<!-- ═══════════════════════════════════════════════════════════════════════════ -->

<Visual_Verification>

**这是强制步骤。**

**Build Passed ≠ Visual Accepted。**

如果当前 Claude Code 环境具备：
- Browser
- 截图能力
- Preview
- Playwright
- Dev Server
- 其他页面检查能力

则页面实施完成后：**必须实际打开页面。**

**不能只看源码。**

**最小验证：**
- Desktop（1440 或类似）
- Tablet（1024 或类似）
- Mobile（390 或类似）

**检查：**
- Layout 完整性
- 对齐
- 间距
- Typography
- Visual Hierarchy
- 文本换行
- 长内容处理
- 溢出行为
- 横向滚动（除非有意为之，否则不应出现）
- 重叠元素
- Sticky 元素
- Fixed 元素
- Modals/Dialogs
- 表格
- Empty State
- Loading State
- Footer
- Header

**如果发现明确问题：直接修复。**

**修复后：重新验证。**

**不能假设："我认为应该没问题。"**

</Visual_Verification>

<Senior_Frontend_Review>

页面实施完成后：

**不要因为是自己刚写的代码就假设正确。**

**切换视角：**

"将此实现视为另一个工程师编写的代码。"

**重新进行 Senior Frontend Review。**

**检查维度：**
- **保真度（Fidelity）** — 是否忠实实现设计（如有设计输入）
- **Layout 正确性** — Flexbox/Grid/Flow 实现正确
- **间距实现** — 符合 Spacing System
- **Typography 实现** — 符合 Typography System
- **Visual Hierarchy 保真度** — 层级还原正确
- **Responsive 稳定性** — 各视口下正常工作
- **Component 架构合理性** — 高内聚低耦合
- **Interaction State 完整性** — Loading/Empty/Error/Hover 已处理
- **CSS 质量** — 无过度嵌套/!important/魔法数字
- **Accessibility 充分性** — 语义 HTML/键盘导航/Focus
- **Performance 关注点** — 无明显性能问题
- **可维护性** — 代码清晰易维护
- **溢出处理** — 无意外 overflow
- **业务回归** — 未破坏现有功能

**不负责审查（由 ui-design 负责）：**
- 设计美学（Aesthetic）
- 品牌一致性（Brand Consistency）
- 设计决策合理性（Design Decision Rationale）

**问题优先级：**

**P0（Critical - 关键）：**
- 功能不可用
- 页面严重崩坏
- 严重业务回归
- 严重重叠/不可操作

**P1（Major - 重大）：**
- 明显保真度问题（与设计稿偏差过大）
- Responsive 崩溃
- Layout 错误
- 明显交互缺失
- 高风险可维护性问题

**P2（Minor - 次要）：**
- 轻微视觉优化
- 非关键代码清理
- 可选体验增强

**原则：**
- **P0 / P1 必须修复。**
- **P2：不要为追求完美而无限扩大范围。**

</Senior_Frontend_Review>

<!-- ═══════════════════════════════════════════════════════════════════════════ -->
<!-- READY 标准                                                                   -->
<!-- ═══════════════════════════════════════════════════════════════════════════ -->

<READY_Definition>

**仅当满足所有条件时才声明 READY：**

✅ **功能正确（Function Correct）** — 所有必需功能正常工作

✅ **视觉成熟（Visual Mature）** — 看起来像精致的商业产品

✅ **Responsive 稳定（Responsive Stable）** — Desktop/Tablet/Mobile 均可用

✅ **无明显溢出（No Obvious Overflow）** — 内容不破坏容器

✅ **无明显重叠（No Obvious Overlap）** — 元素不会错误覆盖彼此

✅ **Typography 一致（Typography Consistent）** — 遵循统一字体系统

✅ **间距一致（Spacing Consistent）** — 遵循统一间距系统

✅ **Component 结构合理（Component Structure Reasonable）** — 高内聚，低耦合

✅ **CSS 可维护（CSS Maintainable）** — 无过度嵌套、!important、魔法数字

✅ **交互状态合理（Interaction State Reasonable）** — Loading/Empty/Error/Hover/Focus 已处理

✅ **无 P0 问题（No P0 Issues）** — 无关键阻塞

✅ **无 P1 问题（No P1 Issues）** — 无重大问题

✅ **Build Passed** — 编译成功

✅ **Type Check Passed** — 无 TypeScript 错误（如适用）

✅ **Visual Verification Passed** — 实际在浏览器中打开并验证（如环境支持）

✅ **无破坏性变更（No Breaking Changes）** — 未破坏 API/Router/Store/Permission/Business Logic

</READY_Definition>

<!-- ═══════════════════════════════════════════════════════════════════════════ -->
<!-- 与 ui-design 协作                                                            -->
<!-- ═══════════════════════════════════════════════════════════════════════════ -->

<Collaboration_With_UI_Design>

**senior-frontend 与 ui-design 的职责边界：**

```
ui-design        = What should it look like?（应该如何呈现）
senior-frontend  = How should we build it correctly?（如何正确构建）
```

**协作场景：**

**场景 1：没有设计稿的新页面**
```
需求
  ↓
ui-design（定义设计）
  ↓
DESIGN READY
  ↓
senior-frontend（实现设计）
  ↓
IMPLEMENTATION READY
```

**场景 2：已有成熟设计稿**
```
需求 + 设计稿（Design / Screenshot / Figma）
  ↓
senior-frontend（忠实实现）
  ↓
IMPLEMENTATION READY

（ui-design 默认不介入）
```

**场景 3：现有页面视觉升级**
```
现有页面
  ↓
ui-design（UI Review / Redesign）
  ↓
DESIGN READY
  ↓
senior-frontend（实现优化）
  ↓
IMPLEMENTATION READY
```

**场景 4：纯技术问题（Layout/Responsive/CSS Bug）**
```
技术问题
  ↓
senior-frontend（直接修复）
  ↓
READY

（无需 ui-design）
```

**设计输入优先级：**

senior-frontend 优先消费以下设计输入（按优先级）：
1. 用户提供的设计稿（Design / Screenshot / Figma）
2. ui-design 输出（DESIGN READY）
3. 项目现有 Design System
4. UI Library 默认规范

**禁止擅自：**
- 重新定义视觉方向
- 改变品牌色
- 重新设计整体视觉语言
- 改变信息架构
- 重新设计页面风格

除非用户明确要求或无任何设计输入。

</Collaboration_With_UI_Design>

<!-- ═══════════════════════════════════════════════════════════════════════════ -->
<!-- 最终注释                                                                     -->
<!-- ═══════════════════════════════════════════════════════════════════════════ -->

<Integration_With_Project_Design_System>

**senior-frontend 是："高级前端工程方法论"**

**senior-frontend 不是："特定项目的视觉设计"**

因此**禁止将特定业务风格硬编码到 Skill 中。**

**禁止示例内容：**
```
电商必须是红色
体育网站必须是深色
管理后台必须使用特定 Layout
Card 必须有特定圆角
```

**项目实际视觉规范应来自：**
- 当前项目 Design System
- 当前业务需求
- 当前设计稿 / 截图

**关系应该是：**

```
Senior Frontend Skill = 如何构建（How to build）
项目 Design System = 应该如何呈现（How it should look）
任务 Prompt = 构建什么（What to build）
```

**在实际项目中工作时：**
1. 阅读项目现有 Design Tokens
2. 阅读项目现有 Components
3. 阅读项目现有 Layout 模式
4. 遵循项目约定
5. 按照本 Skill 的工程标准实施

</Integration_With_Project_Design_System>

<Integration_With_Frontend_Design_Skill>

如果 `frontend-design` Skill（官方插件）可用：

**关系：**

```
frontend-design（可选）→ 视觉方向、调色板、Typography 特征
         ↓
senior-frontend（必需）→ 工程实现、质量、验证
         ↓
    生产级 UI
```

**frontend-design 提供：** 美学方向、品牌独特性、设计决策

**senior-frontend 提供：** Layout 工程、响应式实现、CSS 质量、验证工作流

**它们是互补的，而非冗余的。**

</Integration_With_Frontend_Design_Skill>

<!-- ═══════════════════════════════════════════════════════════════════════════ -->
<!-- 最终注释                                                                     -->
<!-- ═══════════════════════════════════════════════════════════════════════════ -->

<Final_Notes>

## 禁止做的事

**禁止：**
1. Audit → 立即创建重复 Skill
2. 删除现有 Skills
3. 大规模重构 AI 规则目录
4. 作为 Skill 实施的一部分修改业务代码
5. 作为 Skill 实施的一部分修改前端业务页面
6. 修改 Backend
7. 替换 UI Library
8. 替换 Build Tool
9. 引入新的 Runtime Dependencies
10. 为 Skill 系统创建复杂框架
11. 为了显得高级而过度工程化
12. 创建大量重复文档
13. 将特定业务项目视觉风格写入通用 Skill
14. 将建议的目录结构视为强制结构
15. 无依据修改现有 AI Agent 工作流

## 本轮目标

**将高级前端开发方法论沉淀为 JSJ 脚手架现有系统可以稳定复用的 Skill。**

## 兼容性

当前优先支持 Claude Code。

但避免在 Skill 内容中编写大量仅 Claude Code 才能理解的语法。

核心方法论应保持工具无关：
- Claude Code
- Codex  
- Cursor

未来都能复用。

如果当前仓库 Skill 机制是 Claude Code 专用的，则遵循仓库现有约定。

</Final_Notes>

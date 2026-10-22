---
name: ui-design
description: Production-grade UI/UX design workflow for modern commercial web applications - information architecture, visual direction, and design systems
user-invocable: false
environments:
  - local
---

# UI Design

<Purpose>
本 Skill 定义了生产级 UI/UX 设计的完整工作流程，确保页面设计达到现代商业应用标准：目标明确、信息架构清晰、视觉系统完整、避免 AI 模板化。

这不是关于"让页面漂亮一点"，而是系统化地从需求到设计方案的完整流程。
</Purpose>

<Use_When>
适用于以下设计任务：
- 新页面没有成熟设计稿（从需求直接设计）
- 新产品页面设计
- Homepage / Landing Page Design
- Dashboard Design  
- Detail Page Design
- UI Redesign
- 现有页面视觉优化
- 页面"太丑/太普通/不高级"
- Design System 建立
- Visual Review（美学/风格/一致性）
- 页面视觉方向设计
</Use_When>

<Do_Not_Use_When>
跳过设计流程：
- 已有成熟设计稿且只需忠实实现
- 单纯 CSS Bug
- 单纯 Responsive Bug
- Type Error
- Backend Task
- API Task
- Trivial frontend fix
</Do_Not_Use_When>

<Why_This_Exists>
AI 生成的 UI 常见问题：
- 直接开始画 Card，没有思考页面目标
- 所有页面都是圆角白卡堆叠
- 默认蓝紫渐变 + Glassmorphism
- 信息层级混乱，所有内容视觉权重相同
- 字号随意（13/14/15/16/17/18 无规律）
- 间距随意（magic numbers 泛滥）
- 4 个同级 Primary Button 并列
- Card inside Card inside Card
- 过度发光 Border + 无意义网格背景
- 典型 AI 模板感，缺乏品牌特征

本 Skill 通过系统化设计方法论消除这些问题。
</Why_This_Exists>

<Workflow>
设计工作流程：

1. **需求理解（Requirement）** — 理解业务目标
2. **页面目标（Page Goal）** — 明确页面核心任务
3. **用户任务（User Task）** — 用户为什么来这个页面
4. **信息架构（Information Architecture）** — 内容优先级和组织
5. **视觉方向（Visual Direction）** — 确定视觉语言
6. **Design System** — 建立或复用设计系统
7. **Component Visual Language** — 定义组件视觉规范
8. **Responsive Information Strategy** — 响应式信息策略
9. **UI Review** — 设计审查
10. **DESIGN READY** — 设计交付

不要跳过前 4 步直接画 UI。
</Workflow>

<!-- ═══════════════════════════════════════════════════════════════════════════ -->
<!-- 核心设计原则                                                                 -->
<!-- ═══════════════════════════════════════════════════════════════════════════ -->

<Core_Design_Principles>

## 1. 页面目标与用户任务（Page Goal & User Task）

**开始设计前必须回答：**

- 用户为什么进入这个页面？
- 最重要的任务是什么？
- 第一眼应该看到什么？
- 第二步应该做什么？
- 什么信息属于辅助信息？
- 哪些操作是核心 CTA？

**禁止：**
```
需求：做一个用户列表页
设计：立即开始画表格和卡片
```

**正确：**
```
1. 页面目标：管理员快速找到目标用户并执行操作
2. 核心任务：搜索 → 筛选 → 查看详情 → 操作
3. 第一优先级：搜索框 + 快速筛选器
4. 第二优先级：用户列表（状态/关键数据可见）
5. 辅助信息：分页/批量操作
```

## 2. 信息架构（Information Architecture）

**建立清晰的内容层级：**

```
Primary Information
  ↓
Secondary Information
  ↓
Supporting Details
  ↓
Metadata
```

**示例 - Dashboard：**
- Primary：核心指标（今日收入/订单数）
- Secondary：趋势图表
- Supporting：数据明细表
- Metadata：更新时间/筛选条件

**禁止：**
所有信息平铺，无优先级。

## 3. 视觉层级（Visual Hierarchy）

**明确定义：**

| 层级 | 用途 | Typography | 示例 |
|------|------|------------|------|
| **Page Title** | 页面主标题 | 28-32px, Bold | "用户管理" |
| **Section Title** | 区块标题 | 20-24px, Semibold | "最近活跃用户" |
| **Primary Content** | 主要内容 | 16px, Regular | 正文内容 |
| **Secondary Content** | 次要内容 | 14px, Regular | 辅助说明 |
| **Caption** | 标注/元数据 | 12px, Regular | 更新时间 |

**禁止：**
```css
/* 反模式：所有内容视觉权重相同 */
.title, .content, .label, .value {
  font-size: 14px;
  font-weight: 400;
  color: #333;
}
```

## 4. Typography System

**建立规律的字体系统：**

**推荐基础体系：**
```
Display: 32px / 1.2 / Bold
H1:      24px / 1.3 / Semibold
H2:      20px / 1.4 / Semibold
H3:      16px / 1.5 / Semibold
Body:    14px / 1.5 / Regular
Small:   12px / 1.5 / Regular
```

**避免：**
```
13px, 15px, 17px, 18px, 19px 无规律堆积
```

**优先：**
复用项目现有 Typography System。

如果项目无体系，提议最小化 token 集合。

## 5. Color System

**定义系统化调色板：**

**基础色（Foundation）：**
- Background: #FFFFFF / #F5F7FA
- Surface: #FFFFFF (卡片/对话框)
- Border: #E4E7EB

**文本色（Text）：**
- Primary: #1F2937 (主要文字)
- Secondary: #6B7280 (次要文字)
- Muted: #9CA3AF (辅助文字)

**功能色（Functional）：**
- Primary: #3B82F6 (主要操作)
- Success: #10B981
- Warning: #F59E0B
- Error: #EF4444

**禁止默认：**
- 蓝紫渐变作为默认主题
- 所有按钮都发光
- 无意义的霓虹色

**调色板选择应基于：**
- 品牌特征
- 产品定位
- 目标用户
- 行业惯例

## 6. Spacing System

**建立规律的间距体系：**

**推荐基础体系：**
```
4px   - 极小间距（图标与文字）
8px   - 小间距（表单元素内部）
12px  - 紧凑间距（列表项）
16px  - 标准间距（段落/卡片内边距）
24px  - 中等间距（区块间距）
32px  - 大间距（Section 间距）
48px  - 超大间距（Page Section）
```

**优先：**
复用项目现有 Spacing System。

**禁止：**
```
margin-top: 17px;
padding: 23px 19px 21px 27px;
```

无规律魔法数字。

## 7. 密度策略（Density Strategy）

**根据页面类型选择信息密度：**

**High Density（高密度）：**
- 后台管理表格
- 数据密集型 Dashboard
- 专业工具界面
- 特征：紧凑间距、信息量大、效率优先

**Medium Density（中密度）：**
- 内容管理页面
- 标准业务表单
- 列表页
- 特征：平衡信息量与可读性

**Low Density（低密度）：**
- Landing Page
- Marketing 页面
- 引导流程
- 特征：宽松间距、突出重点、体验优先

**禁止：**
所有页面使用相同密度。

## 8. Card 使用策略（Card Usage）

**Card 的合理使用：**

**适合 Card 的场景：**
- 独立内容单元（文章预览、产品卡片）
- 需要视觉分组的内容
- 可交互的内容块

**不适合 Card 的场景：**
- 简单文本信息（用 section 或 list）
- 已经在容器内的内容
- 过度嵌套（Card inside Card inside Card）

**禁止：**
```html
<!-- 反模式：所有内容都包 Card -->
<Card>
  <Card>
    <Card>
      <p>Some text</p>
    </Card>
  </Card>
</Card>
```

**合理：**
```html
<!-- 根据内容性质决定是否使用 Card -->
<div class="page">
  <section class="stats"> <!-- 统计数据不需要 Card -->
    <StatItem />
  </section>
  
  <Card> <!-- 独立内容单元使用 Card -->
    <ArticlePreview />
  </Card>
</div>
```

## 9. CTA 层级（CTA Hierarchy）

**一个区域明确 CTA 优先级：**

| 层级 | 用途 | 样式 | 示例 |
|------|------|------|------|
| **Primary** | 核心操作 | Filled, 主色 | "提交订单" |
| **Secondary** | 次要操作 | Outlined / Ghost | "保存草稿" |
| **Tertiary** | 辅助操作 | Text link | "取消" |
| **Danger** | 危险操作 | Filled, 红色 | "删除账户" |

**禁止：**
```html
<!-- 反模式：4 个同级 Primary Button -->
<div class="actions">
  <Button type="primary">提交</Button>
  <Button type="primary">保存</Button>
  <Button type="primary">导出</Button>
  <Button type="primary">分享</Button>
</div>
```

**正确：**
```html
<div class="actions">
  <Button type="primary">提交</Button>
  <Button type="secondary">保存草稿</Button>
  <Button type="text">取消</Button>
</div>
```

## 10. Visual Rhythm（视觉节奏）

**控制页面视觉节奏：**

**通过以下元素建立节奏：**
- 大区块 vs 小区块
- 密集信息 vs 留白
- 粗标题 vs 细文本
- 图片 vs 纯文本
- 数据可视化 vs 表格

**避免：**
```
Card Card Card Card Card Card Card Card
（单调重复，无节奏感）
```

**追求：**
```
Hero Section
↓
3-Column Cards
↓
Full-width Chart
↓
2-Column Content
↓
CTA Section
```

**节奏变化创造视觉兴趣。**

## 11. Anti-AI-Looking Design

**避免典型 AI 模板化设计：**

**禁止默认使用（除非有明确品牌理由）：**
- 蓝紫渐变背景
- Neon Glow 效果
- 大量 Glassmorphism
- 巨型 Hero Section（占满整屏）
- Floating Orb（浮动光球）
- 无意义光晕
- 所有模块都是圆角白卡
- 过度阴影
- 发光 Border
- 无意义网格背景
- 装饰性科技线条泛滥

**"技术感"应通过以下建立：**
- 精确的 Typography
- 清晰的 Grid System
- 适当的信息密度
- 高质量 Imagery
- Precise Spacing
- 简洁的 Motion
- 高质量内容
- 成熟的 Layout

**而不是单纯靠蓝紫发光。**

**判断标准：**
"这个设计在 2024 年看起来是否专业且非模板化？"

## 12. Component Visual Language

**定义组件视觉规范：**

**Button：**
- 高度（32px / 36px / 40px）
- 圆角（4px / 6px / 8px）
- 内边距（12px 16px / 16px 24px）
- 字重（Medium / Semibold）

**Input / Form：**
- 高度（36px / 40px）
- 圆角（6px / 8px）
- Border 颜色（default / focus / error）
- Label 位置（top / floating / inline）

**Card：**
- 圆角（8px / 12px）
- 阴影（subtle / medium）
- 边框（有 / 无）
- 内边距（16px / 20px / 24px）

**Table：**
- 行高（40px / 48px）
- Stripe（有 / 无）
- Border style（全边框 / 横线 / 无边框）
- Header style（填充 / 加粗 / 颜色区分）

**保持项目内一致性。**

## 13. Icon & Image Style

**保持视觉一致性：**

**Icon：**
- 风格统一（Line / Filled / Duotone，选其一）
- 线宽一致（1.5px / 2px）
- 尺寸规范（16px / 20px / 24px）
- 视觉重量一致

**禁止混用：**
```
一个页面同时出现：
- Line icons
- Filled icons
- Emoji
- 3D icons
```

**Image：**
- 风格一致（照片 / 插画 / 3D，选择统一风格）
- 色调一致
- 质量统一（避免混合高清与模糊）

## 14. Responsive Information Strategy

**定义响应式信息组织策略：**

**Desktop（1440px+）：**
- 信息如何布局？（3-column / 2-column / sidebar）
- 主要内容区宽度？（固定 / 流式）
- 导航如何呈现？（横向 / 侧边）

**Tablet（768px-1024px）：**
- 哪些列合并？（3 → 2）
- 哪些内容重新组织？
- 导航如何调整？（折叠 / 简化）

**Mobile（<768px）：**
- 哪些内容优先显示？
- 哪些功能折叠/隐藏？
- 哪些区块堆叠？
- 导航如何处理？（汉堡菜单 / 底部 Tab）

**注意：**
ui-design 定义信息策略，具体 CSS 实现由 senior-frontend 完成。

## 15. Design Token

**优先复用现有 Design Tokens：**

**检查项目是否已有：**
- Color tokens
- Typography tokens
- Spacing tokens
- Radius tokens
- Shadow tokens
- Breakpoint tokens

**如果已存在：**
禁止随意重新定义，必须复用。

**如果不存在：**
可以提议最小化 Design Token 集合，但不要单方面重构整个项目。

**提议格式：**
```javascript
// 颜色 tokens
--color-text-primary: #1F2937
--color-text-secondary: #6B7280
--color-primary: #3B82F6

// 间距 tokens
--spacing-xs: 4px
--spacing-sm: 8px
--spacing-md: 16px
--spacing-lg: 24px

// 字号 tokens
--text-display: 32px
--text-h1: 24px
--text-body: 14px
```

</Core_Design_Principles>

<!-- ═══════════════════════════════════════════════════════════════════════════ -->
<!-- UI REVIEW                                                                    -->
<!-- ═══════════════════════════════════════════════════════════════════════════ -->

<UI_Review>

**设计完成后进行 UI Review：**

**检查维度：**

✅ **页面目标清晰** — 用户知道这是什么页面，做什么用

✅ **信息架构清晰** — Primary/Secondary/Supporting 层级明确

✅ **视觉层级清晰** — 重要内容视觉突出

✅ **Typography 一致** — 遵循统一字体系统

✅ **Color System 一致** — 颜色使用符合定义的调色板

✅ **Spacing 一致** — 间距遵循统一体系

✅ **Density 合理** — 信息密度符合页面类型

✅ **CTA 层级清晰** — Primary/Secondary 明确

✅ **Component Visual Language 一致** — 组件风格统一

✅ **无明显 AI 模板化** — 不是典型蓝紫渐变 + 圆角卡片堆叠

✅ **Icon/Image 风格一致** — 视觉风格统一

✅ **Responsive Strategy 清晰** — Desktop/Tablet/Mobile 信息组织明确

✅ **无明显视觉冲突** — 颜色/字体/间距无明显不协调

✅ **无 P0/P1 UI 问题**

**问题优先级：**

**P0（Critical）：**
- 页面目标不清晰
- 信息架构混乱
- 核心 CTA 不明显
- 严重视觉冲突

**P1（Major）：**
- 视觉层级不清晰
- Typography/Spacing 不一致
- 明显 AI 模板化
- CTA 层级混乱

**P2（Minor）：**
- 轻微视觉优化
- 非关键细节调整

**原则：**
P0/P1 必须修复，P2 不要无限扩大范围。

</UI_Review>

<!-- ═══════════════════════════════════════════════════════════════════════════ -->
<!-- DESIGN READY                                                                 -->
<!-- ═══════════════════════════════════════════════════════════════════════════ -->

<DESIGN_READY_Definition>

**仅当满足所有条件时才声明 DESIGN READY：**

✅ **Page Goal Clear** — 页面目标明确

✅ **Information Architecture Clear** — 信息架构清晰

✅ **Visual Direction Clear** — 视觉方向明确

✅ **Hierarchy Clear** — 视觉层级清晰

✅ **Typography Consistent** — 字体系统一致

✅ **Color System Consistent** — 调色板一致

✅ **Spacing Consistent** — 间距系统一致

✅ **Density Appropriate** — 信息密度合理

✅ **CTA Hierarchy Clear** — CTA 层级清晰

✅ **Component Visual Language Consistent** — 组件视觉语言一致

✅ **Responsive Information Strategy Clear** — 响应式信息策略明确

✅ **No Obvious AI Template Pattern** — 无明显 AI 模板化

✅ **No Obvious Visual Conflict** — 无明显视觉冲突

✅ **No P0/P1 UI Issue** — 无 P0/P1 问题

**设计交付内容：**
- 视觉方向说明
- Design Tokens（如果创建）
- Component Visual Specs
- Responsive Information Strategy
- 关键设计决策说明

</DESIGN_READY_Definition>

<!-- ═══════════════════════════════════════════════════════════════════════════ -->
<!-- 与 senior-frontend 协作                                                      -->
<!-- ═══════════════════════════════════════════════════════════════════════════ -->

<Collaboration_With_Senior_Frontend>

**ui-design 与 senior-frontend 的关系：**

```
ui-design        = What should it look like?
senior-frontend  = How should we build it correctly?
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
需求 + 设计稿
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

**职责边界清晰，避免重复工作。**

</Collaboration_With_Senior_Frontend>

<!-- ═══════════════════════════════════════════════════════════════════════════ -->
<!-- 与已有设计的关系                                                             -->
<!-- ═══════════════════════════════════════════════════════════════════════════ -->

<Respect_Existing_Design>

**如果用户提供了设计输入：**

**Design / Screenshot / Figma / Prototype**

**首先判断任务类型：**

**A. Fidelity Implementation（忠实实现）**
- 用户明确要求还原设计
- 设计稿已经成熟
- → **优先交给 senior-frontend**
- → **ui-design 不擅自 Redesign**

**B. Visual Optimization（视觉优化）**
- 用户要求"优化视觉"
- 设计稿存在明显问题
- → **ui-design 可以提出改进方案**

**C. Redesign（重新设计）**
- 用户明确要求重新设计
- 现有设计不符合需求
- → **ui-design 负责 Redesign**

**原则：**
尊重已有设计，不要擅自修改用户提供的设计方案。

</Respect_Existing_Design>

<!-- ═══════════════════════════════════════════════════════════════════════════ -->
<!-- 与 frontend-design Plugin 的关系                                             -->
<!-- ═══════════════════════════════════════════════════════════════════════════ -->

<Relationship_With_Frontend_Design_Plugin>

**frontend-design Plugin（Claude 官方）：**
- 提供品牌独特性指导
- 美学方向（aesthetic direction）
- Typography signature
- 避免模板化设计
- 设计风险决策

**ui-design（JSJ 项目级）：**
- 提供系统化设计流程
- Information Architecture
- Design System 建立
- Component Visual Specs
- 跨 Agent 可携带（Claude Code / Cursor / Codex）

**关系：**
- 两者可共存
- frontend-design 提供艺术指导
- ui-design 提供工程化设计流程
- ui-design 必须能独立工作（不依赖 Plugin）

</Relationship_With_Frontend_Design_Plugin>

<!-- ═══════════════════════════════════════════════════════════════════════════ -->
<!-- 最终注释                                                                     -->
<!-- ═══════════════════════════════════════════════════════════════════════════ -->

<Final_Notes>

## 本 Skill 的定位

**ui-design 负责：**
设计决策（What should it look like?）

**不负责：**
代码实现（How to build it?）

**协作伙伴：**
senior-frontend（实现设计）

## Scope

**本 Skill 禁止：**
- 修改业务代码
- 实现 CSS
- 编写 Component 代码
- 修改 API/Router/Store
- 替换 UI Library
- 替换 Build Tool

**本 Skill 只负责：**
设计方案制定。

## 兼容性

本 Skill 设计为跨 Agent 可用：
- Claude Code
- Cursor
- Codex

核心设计方法论保持工具无关。

</Final_Notes>

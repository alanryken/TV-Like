---
name: "tvlike-html-rule-extractor"
description: "Extracts visible TV-Like YAML rules from generic HTML pages. Invoke when an AI needs to turn page HTML and URL into maintainable TV-Like DSL."
---

# TV-Like HTML Rule Extractor

你是一个把任意网站的通用 HTML 页面抽取成 TV-Like YAML DSL 的技能。

目标：

- 从 HTML 和 URL 中识别对电视端真正有价值的可见区块
- 生成当前 `tv-like-core` 已支持的 YAML 规则
- 保持 selector 稳定、可维护、尽量通用
- 不要针对某一个样例页面、某一类站点或某个垂直行业过拟合

## 什么时候调用

在这些场景调用：

- 用户给出一个网页 HTML，要求提取 TV-Like 规则
- 用户给出一个网页 URL 和源码，希望生成 YAML DSL
- 其他 AI 需要从任意站点页面中总结“可见规则”
- 需要把页面结构抽象成适合电视端展示的 section 和 items

不要在这些场景调用：

- 只是解释 HTML 语法
- 只是做 DOM 调试，不需要产出 TV-Like YAML
- 需要生成项目之外的自定义 DSL

## 当前项目约束

只使用当前项目已经实现的能力：

- 顶层：`version`
- 顶层：`paths` 或 `sections`
- path：`match`、`matches`
- section：`name`、`selector`、`limit`、`meta`、`fields`、`items`
- items：`selector`、`limit`、`meta`、`fields`
- field 只允许：
  - `text`
  - `img`
  - `link`
- field 配置只允许：
  - `selector`
  - `attr`
  - `transforms`
  - `meta`
- transforms 只允许：
  - `trim`
  - `upper`
  - `lower`
  - `digits`
  - `abs-url`

不要虚构这些能力：

- fallback
- 条件判断
- 变量引用
- 自定义函数
- 自定义字段名
- 多图、多链接、多文本并行输出

## 可见规则定义

“可见规则”只提取用户在页面上实际看得到、且电视端有展示价值的内容。

默认保留：

- 顶部导航
- 主横幅 / hero / 轮播
- 当前可见公告
- 主内容摘要区
- 通用列表 / 卡片栅格 / 表格化列表
- 标签、分类、面包屑、筛选入口
- 相关推荐、推荐入口、操作入口

默认忽略：

- `script`
- `style`
- HTML 注释里的内容
- 统计脚本、埋点、广告脚本
- 仅交互逻辑，不承载内容的按钮
- 纯版权、备案、技术性页脚
- 默认隐藏且对电视端无意义的下拉层

注意：

- 如果某个公告、弹层、横幅在源码里明确处于可见状态，并且承载内容，可以保留
- 不要因为是弹层就一律删除，要按“当前是否可见、是否有内容价值”判断

## 抽取原则

### 1. 先判断页面类型

优先判断页面更像：

- 首页 / 门户页
- 列表页 / 分类页
- 搜索结果页
- 详情页
- 文章页 / 文档页
- 商品页 / 服务页
- 表单页 / 登录页
- 仪表盘 / 工具页

### 2. 只保留电视端高价值区块

区块数量宁少勿滥。

优先保留：

- 用户会浏览和点击的内容区
- 重复出现的内容卡片区
- 页面主导航和主入口

优先舍弃：

- 纯说明文字
- 重复导航
- 只在 PC 交互里有意义的浮层
- 空列表

### 3. selector 选择策略

优先使用：

- 稳定的 `id`
- 稳定的 `class`
- 语义清楚的父容器
- 适度的组合选择器
- 必要时使用 `:has(...)`、`:contains(...)`、`:first-of-type`

避免使用：

- 过长层级链
- `nth-child`
- 强依赖顺序的深层 selector
- 明显像运行时生成的 class
- 只对当前样例页面成立的路径

### 4. section 和 items 的判断

- 单块内容：用 `fields`
- 重复卡片：用 `items`
- 同构重复块：优先合并成一个通用 `list`

如果一个区块里主要是重复卡片，就优先写成：

```yaml
items:
  selector: "> li"
  fields:
    text:
      selector: .title a
    link:
      selector: a
      attr: href
    img:
      selector: img
      attr: src
```

如果页面上出现很多结构完全相同、只有标题文案不同的块：

- 不要按文案复制多条 section
- 优先抽成一个通用 section
- 再用一个 `items` list 提取这些重复块
- 如果当前 DSL 不能同时表达“两层嵌套”，优先保留对浏览和点击更有价值的一层

错误示例：

- `电影`
- `电视剧`
- `纪录片`
- `动漫`

如果这几块 DOM 结构完全相同，只是标题不同，不要各写一条 rule。

### 5. 字段映射规则

TV-Like 当前只支持三个字段，因此必须做信息压缩：

- `text`：最主要的可读文本，通常是标题
- `link`：点击后进入详情或播放页的地址
- `img`：海报、封面、缩略图

常见映射：

- 卡片标题 / 条目名称 / 按钮文案 -> `text`
- 详情链接 / 跳转链接 / 操作入口 -> `link`
- 封面图 / 缩略图 / 图标图 -> `img`
- 导航项文案 -> `text`
- 导航项地址 -> `link`
- 公告正文 / 摘要 -> `text`
- 横幅图 -> `img`
- 横幅跳转 -> `link`

不要尝试输出当前引擎不支持的额外字段。

## 图片与链接处理

链接处理：

- `href` 一般映射到 `link`
- 相对地址优先加 `transforms: [abs-url]`

图片处理：

- 先找 `src`
- 没有再找 `data-src`
- 还没有再找 `data-original`
- 相对地址优先加 `transforms: [abs-url]`

文本处理：

- 默认加 `transforms: [trim]`

## 路径规则选择

如果已知 URL 或页面类型明确：

- 优先输出 `paths`
- 首页常见可用：
  - `/`
  - `/index.html`
- 详情页、分类页按 URL 模式写 `match`

如果没有可靠 URL：

- 直接输出顶层 `sections`

## section 命名建议

推荐命名：

- `nav`
- `hero`
- `banner`
- `notice`
- `summary`
- `list`
- `grid`
- `catalog`
- `related`
- `actions`
- `sidebar`
- `tabs`
- `filters`
- `topic`

避免：

- `section1`
- `block2`
- `tmp`
- 直接复制技术 class 名作为业务名

## 工作流程

按这个顺序执行：

1. 阅读 URL、HTML、可见文本和重复结构
2. 判断页面类型和主要区块
3. 排除不可见、空白、低价值节点
4. 给每个高价值区块选择稳定 root selector
5. 判断该区块是 `fields` 还是 `items`
6. 为每个区块映射 `text / link / img`
7. 为相对链接和图片补 `abs-url`
8. 控制 section 数量，避免把页面所有零碎块都写进 YAML
9. 检查 selector 是否明显过拟合
10. 输出 YAML 和必要说明

## 输出格式

默认输出两部分：

### 1. 页面判断

简要说明：

- 页面类型
- 保留了哪些可见区块
- 略过了哪些低价值区块
- selector 为什么这样选

### 2. YAML DSL

直接输出可复制的 YAML：

```yaml
version: 1
sections:
  - name: nav
    selector: .nav
    items:
      selector: li
      fields:
        text:
          selector: a
          transforms: [trim]
        link:
          selector: a
          attr: href
          transforms: [abs-url]
```

如果页面存在很多同构块，优先输出这种合并写法：

```yaml
version: 1
sections:
  - name: catalog
    selector: .page-body
    items:
      selector: ".block-head:has(a[href])"
      fields:
        text:
          selector: a
          transforms: [trim]
        link:
          selector: a
          attr: href
          transforms: [abs-url]
```

必要时再补充一句人工复核提示，但不要长篇解释。

## 自检清单

输出前逐项检查：

- 是否只用了项目支持的 YAML 能力
- 是否只用了 `text / img / link`
- 是否优先选择了稳定 selector
- 是否避免了注释内容和脚本内容
- 是否忽略了空列表和低价值区块
- 是否给相对链接加了 `abs-url`
- 是否把重复卡片写成了 `items`
- 是否把同构重复块合并成了一个通用 list
- 是否存在明显绑定单一样例站点的 selector

## 失败时的处理

如果 HTML 过于混乱，无法稳定抽取：

- 先给出最小可用 YAML
- 只保留最稳定的 1 到 3 个 section
- 明确指出需要人工复核的区块

优先稳定可维护，不要追求覆盖所有元素。

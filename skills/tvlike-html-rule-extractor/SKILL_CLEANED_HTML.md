---
name: "tvlike-cleaned-html-rule-extractor"
description: "Generates pure TV-Like YAML from cleanedHtml and URL. Invoke when AI needs rules closer to original HTML while still leveraging cleaned or folded repeated structures."
---

# TV-Like Cleaned HTML Rule Extractor

你是一个把 `HtmlSummarizer` 产出的 `cleanedHtml` 归纳为 TV-Like YAML DSL 的技能。

你的唯一目标是：

- 根据页面真实结构生成一份可直接使用的 TV-Like YAML
- 优先基于 `cleanedHtml` 和 URL 归纳，而不是照抄页面已有 DSL
- 在存在重复结构时，主动识别适合抽成 `items` 的列表块
- 最终只输出纯 YAML，不输出解释、分析、标题、前后说明

## 什么时候调用

在这些场景调用：

- 用户给出 `cleanedHtml` 和 URL，要求生成 TV-Like YAML
- 用户希望结果尽量接近直接分析原 HTML 的效果，但不想输入整页原始 HTML
- 页面里存在大量重复卡片、导航项、分类入口，希望 AI 识别为可复用的列表规则
- 其他 AI 已经先做了 HTML 清洗，需要再将清洗结果归纳成当前项目可执行的 YAML

不要在这些场景调用：

- 输入只有 `customTree`，没有 `cleanedHtml`
- 只是解释 HTML 结构，不需要生成 YAML
- 只是分析现有 DSL，不需要重新归纳规则
- 需要输出 JSON、Markdown 报告或自然语言总结

## `cleanedHtml` 的输入特点

`cleanedHtml` 由 `HtmlSummarizer.summarize(...)` 生成，具有这些特点：

- 已移除 `script`、`style`、`noscript`、`template` 等噪声节点
- 已移除明显隐藏元素
- 文本已做空白归一化
- 只保留较关键的属性，如 `id`、`class`、`href`、`src`、`data-src`、`data-original`、`title`、`alt`

这意味着：

- 它比 `customTree` 保留更多真实 DOM 细节
- 它比原始 HTML 更干净，更适合归纳稳定 selector
- 你可以直接利用属性值、层级和重复结构来判断页面类型和字段含义

## 当前项目真实约束

只使用当前仓库已经实现的能力，不要脑补。

### 顶层 DSL 能力

只支持：

- `version`
- `paths`
- `sections`

path 只支持：

- `match`
- `matches`

section 只支持：

- `name`
- `selector`
- `limit`
- `meta`
- `fields`
- `items`

items 只支持：

- `selector`
- `limit`
- `meta`
- `fields`

field 名只支持：

- `text`
- `img`
- `link`

field 配置只支持：

- `selector`
- `attr`
- `transforms`
- `meta`

transforms 只支持：

- `trim`
- `upper`
- `upper-case`
- `lower`
- `lower-case`
- `digits`
- `abs-url`

不要虚构这些能力：

- fallback
- 条件判断
- 变量引用
- 自定义函数
- import / include
- 自定义字段类型
- 多值并行输出
- 嵌套多层 list 容器

## 当前解析行为

生成 YAML 时，必须遵守这些真实行为：

- 路径命中时，更具体的 `match` 会优先于更宽泛的规则
- 如果没有 `paths` 命中，而顶层有 `sections`，会走默认 `/**`
- `section` 可以只用 `fields`，也可以用 `items`
- `items` 里的每个字段通过 `selectFirst()` 取第一个匹配值，不会收集多个值
- 非法 selector 会被静默忽略，不会抛给用户
- `meta` 会透传到结果，但不会参与执行逻辑
- `limit` 作用于 section 命中数或 item 数
- 最终 section 输出顺序按 DOM 出现顺序排序，而不是 YAML 书写顺序

因此：

- 不要写依赖“字段合并”“多节点拼接”的规则
- 不要假设一个字段能拿到多个匹配
- 不要用只有在理想解析器里才成立的复杂设计
- 不要因为输入里可能已有 DSL 或旧规则，就直接照抄它

## 工作方法

每次执行都按这个顺序：

1. 先结合 URL 判断页面类型
2. 再从 `cleanedHtml` 中识别高价值区块和重复结构
3. 优先寻找稳定的容器 selector
4. 对重复块抽取 `items.selector`
5. 对标题、链接、封面映射为 `text / link / img`
6. 输出最终 YAML

思考过程可以存在，但最终回复里不要输出思考过程。

## 第一步：判断页面类型

优先判断页面更像：

- 首页 / 门户页
- 分类页 / 列表页
- 搜索结果页
- 详情页
- 播放页
- 专题页
- 文档页 / 文章页

页面类型会直接影响：

- 是否使用 `paths`
- section 应该保留哪些块
- 列表是否适合抽成 `items`

## 第二步：识别重复结构和折叠线索

优先识别这些高价值重复块：

- 顶部导航项
- 主内容卡片列表
- 分类入口
- 推荐区块
- 选集列表
- 标签或筛选入口

如果 `cleanedHtml` 中出现多个同级、结构相近的子节点：

- 优先把父容器作为 `section.selector`
- 把重复子节点作为 `items.selector`
- 不要为每个重复块单独生成几乎相同的 section

如果用户同时提供了 `foldedHtml`、`customTree` 或任何“repeat / 折叠”线索：

- 把这些线索当作重复结构的强提示
- 优先用它们帮助你决定哪些区域应该抽成 `items`
- 但 selector 仍然要基于 `cleanedHtml` 中真实可执行的 DOM 结构来写

## 第三步：selector 选择策略

优先使用：

- 稳定的 `id`
- 稳定的 `class`
- 语义清楚的父容器
- 适度的组合选择器
- 在 Jsoup 可支持范围内、且能明显提升稳定性的 `:has(...)`
- 属性值可明确区分类别时的属性选择器，如 `[href*='/show/']`

避免：

- 过长层级链
- `nth-child`
- `nth-of-type`
- `first-of-type`
- `last-of-type`
- 强依赖顺序的深层路径
- 看起来像运行时生成的 class
- 只对当前样例成立的偶然位置

## 第四步：section / items 选择原则

- 单块信息：优先 `fields`
- 重复卡片：优先 `items`
- 同构重复块：优先合并，而不是按文案复制多条 section

如果某个区域同时存在块标题和块内列表：

- 父容器更稳定时，用父容器做 `section.selector`
- 块标题可放在 `fields`
- 重复卡片放在 `items`

如果当前 DSL 无法优雅表达双层结构：

- 优先保留电视端价值更高的一层
- 通常优先保留影片卡片、导航入口、分类入口、剧集入口

## 第五步：字段映射原则

当前只能输出三个字段，因此要做信息压缩：

- `text`：核心标题或文案
- `link`：详情、播放或入口链接
- `img`：封面、缩略图、海报

常见映射：

- 导航项文字 -> `text`
- 导航项地址 -> `link`
- 卡片标题 -> `text`
- 卡片详情链接 -> `link`
- 卡片封面 -> `img`
- 公告正文 -> `text`
- 横幅图片 -> `img`
- 横幅跳转 -> `link`

### attr 与 transforms

链接：

- 一般用 `attr: href`
- 相对地址优先加 `transforms: [abs-url]`

图片：

- 先看 `src`
- 没有再看 `data-src`
- 再没有再看 `data-original`
- 相对地址优先加 `transforms: [abs-url]`

文本：

- 默认优先加 `transforms: [trim]`

## 路径规则选择

如果 URL 明确、且不同页面类型差异明显：

- 优先输出 `paths`

首页常见写法：

```yaml
version: 1
paths:
  - matches:
      - /
      - /index.html
    sections: []
```

列表页、详情页、播放页常见写法：

```yaml
version: 1
paths:
  - match: /voddetail/**
    sections: []
```

如果没有可靠 URL 信息：

- 可以直接输出顶层 `sections`

## section 命名建议

推荐：

- `nav`
- `hero`
- `banner`
- `notice`
- `latest`
- `catalog`
- `list`
- `grid`
- `related`
- `filters`
- `topic`

避免：

- `section1`
- `block2`
- `tmp`
- 直接照搬技术 class 名当业务名

## 输出格式

最终回复必须只包含纯 YAML，不要输出任何别的文字。

如果用户明确要求“只要 YAML”，那就只返回 YAML 文本本身。

## 自检清单

输出前逐项检查：

- 是否是从 `cleanedHtml`、URL 和真实结构归纳出的 YAML，而不是照抄输入里的旧规则
- 是否只用了项目当前真实支持的能力
- 是否优先用了更短、更稳的 selector
- 是否严格使用了 2 空格缩进，且没有 tab
- 是否只用了 `text / img / link`
- 是否理解了 `selectFirst()` 带来的单值提取限制
- 是否识别并合并了明显重复的卡片、导航、分类或选集结构
- 是否给相对链接和图片加了 `abs-url`
- 如果用户给了明确 URL，是否判断过该页面更适合 `matches`、`match` 还是顶层 `sections`
- 最终输出是否只有纯 YAML

## 失败时的处理

如果 `cleanedHtml` 信息不够，或者稳定 selector 很难选：

- 仍然先给最小可用 YAML
- 只保留最稳定的 1 到 3 个 section
- 不要输出失败说明，仍然只输出最小 YAML

优先稳定和可维护，不要追求“把整页都抽出来”。

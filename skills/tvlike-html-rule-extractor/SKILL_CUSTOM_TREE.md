name: "tvlike-custom-tree-rule-extractor"
description: "Generates pure TV-Like YAML from customTree DOM summaries. Invoke when AI needs to convert HtmlSummarizer customTree into stable YAML."
---

# TV-Like Custom Tree Rule Extractor

你是一个把 `HtmlSummarizer` 产出的 `customTree` 结构摘要归纳为 TV-Like YAML DSL 的技能。

你的唯一目标是：

- 根据 `customTree` 表达的页面真实结构生成一份可直接使用的 TV-Like YAML
- 只基于 `customTree` 本身进行归纳
- 最终只输出纯 YAML，不输出解释、分析、标题、前后说明

## 什么时候调用

在这些场景调用：

- 用户给出 `customTree`，要求生成 TV-Like YAML
- 用户给出 `customTree` 文本，希望 AI 归纳成 TV-Like DSL
- 其他 AI 先把 HTML 压缩成结构树，再需要把它转换成当前项目可执行的 YAML
- 页面 HTML 很大，不适合整页直接喂模型，但已经有 `customTree` 可供分析

不要在这些场景调用：

- 只是解释 `customTree` 结构
- 只是分析现有 DSL，不需要重新生成 YAML
- 需要输出项目之外的自定义 DSL
- 需要输出 JSON、Markdown 报告或自然语言总结

## `customTree` 格式约定

`customTree` 由 `HtmlSummarizer.toCustomTree(...)` 生成，典型格式类似：

```text
document title="首页"
  body
    div#app.root
      ul.cards
        li.card repeat=3
          a attrs=[href] text="片名1"
          img attrs=[data-src]
```

读法规则：

- 缩进表示父子层级，每一级缩进 2 个空格
- 行首是标签名，如 `div`、`ul`、`li`、`a`、`img`
- `#id` 表示节点 id
- `.className` 表示节点 class，可连续出现多个
- `repeat=N` 表示该节点代表一组被折叠的重复块
- `attrs=[href, data-src]` 表示该节点保留下来的关键属性名
- `text="..."` 表示节点自身文本摘要，不代表完整原文

注意：

- `repeat=N` 是“重复结构线索”，不是 CSS selector 的一部分
- `attrs=[href]` 表示这个节点可能适合提取 `link`
- `attrs=[src]`、`attrs=[data-src]`、`attrs=[data-original]` 表示这个节点可能适合提取 `img`
- `text="..."` 主要用于理解业务语义，不应该被当作 selector 条件

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

1. 根据 `customTree` 判断页面类型
2. 从层级和 `repeat` 识别高价值区块
3. 从标签名、`#id`、`.class`、`attrs=[...]` 中提取 selector 线索
4. 选择更短、更稳的容器和字段 selector
5. 映射为 `sections / fields / items`
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

## 第二步：从 `customTree` 归纳真实结构

优先看用户真正会浏览或点击的内容。

优先保留：

- 顶部导航
- hero / banner / 可见公告
- 主内容卡片列表
- 分类入口
- 推荐列表
- 筛选入口 / 标签入口

优先忽略：

- `document` 根节点本身
- 只起布局作用、没有业务语义的外层容器
- 纯技术页脚
- 默认隐藏且不承载核心内容的浮层
- 没有文本、链接、图片线索的装饰节点

特殊判断：

- `repeat=N` 往往意味着该节点适合抽成 `items.selector`
- 如果一个父容器下出现多个同构子节点，优先把父容器当作 `section.selector`，把重复子节点当作 `items.selector`
- 如果只有一个明确业务块，没有重复项，优先用 `fields`

## 第三步：按真实实现生成 selector

### selector 选择策略

优先使用：

- 稳定的 `id`
- 稳定的 `class`
- 语义清楚的父容器
- 适度的组合选择器
- 在 Jsoup 可支持范围内、且能明显提升稳定性的 `:has(...)`

避免：

- 过长层级链
- `nth-child`
- `nth-of-type`
- `first-of-type`
- `last-of-type`
- 强依赖顺序的深层路径
- 看起来像运行时生成的 class
- 只对当前样例摘要成立的偶然位置

### `customTree` 到 selector 的映射原则

- `div#app.root` 可以推断为 `div#app.root` 或更短的 `#app`
- `ul.cards` 下的 `li.card repeat=8`，通常适合：
  - `section.selector: ul.cards`
  - `items.selector: li.card`
- `a attrs=[href] text="..."` 通常适合提取：
  - `text.selector: a`
  - `link.selector: a`
  - `link.attr: href`
- `img attrs=[data-src]` 通常适合提取：
  - `img.selector: img`
  - `img.attr: data-src`

不要把这些摘要标记直接写进 selector：

- `repeat=3`
- `attrs=[href]`
- `text="片名1"`

这些标记只用于理解结构，不是 CSS 语法。

### section / items 选择原则

- 单块信息：优先 `fields`
- 重复卡片：优先 `items`
- 同构重复块：优先合并，而不是按文案复制多条 section

如果树里出现多个相邻同构块：

- 不要按标题文案展开成多条几乎相同的 section
- 优先保留最有电视端价值的一层
- 如果当前 DSL 无法优雅表达双层结构，就保留“分类入口”或“影片卡片”中更重要的一层

### 字段映射原则

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

由于此技能只处理 `customTree`，默认没有可靠 URL 信息：

- 默认直接输出顶层 `sections`
- 只有当用户显式同时提供可用路径信息，并且你能从输入中稳定区分页面类型时，才谨慎输出 `paths`

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

- 是否是从 `customTree` 和真实结构归纳出的 YAML，而不是照抄输入里的旧规则
- 是否只用了项目当前真实支持的能力
- 是否优先用了更短、更稳的 selector
- 是否严格使用了 2 空格缩进，且没有 tab
- 是否只用了 `text / img / link`
- 是否理解了 `selectFirst()` 带来的单值提取限制
- 是否避免使用 `first-of-type / nth-*` 这类容易丢数据的 selector
- 是否给相对链接和图片加了 `abs-url`
- 是否避免把同构区块按文案重复展开
- 最终输出是否只有纯 YAML

## 失败时的处理

如果 `customTree` 信息不够，或者稳定 selector 很难选：

- 仍然先给最小可用 YAML
- 只保留最稳定的 1 到 3 个 section
- 不要输出失败说明，仍然只输出最小 YAML

优先稳定和可维护，不要追求“把整页都抽出来”。

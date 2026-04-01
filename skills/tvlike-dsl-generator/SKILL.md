---
name: "tvlike-dsl-generator"
description: "Generates TV-Like DSL from page HTML/URL by inferring sections, selectors, and options. Invoke when user wants to create or bootstrap extraction rules for a webpage."
---

# TV-Like DSL Generator

你是一个专门为 TV-Like 项目生成 DSL 的技能。

目标：根据页面内容、页面 URL、HTML 结构、已有项目规则约定，自动产出可直接用于 TV-Like 的 DSL 初稿，并尽量保证规则稳定、可维护、贴合电视端内容结构。

## 何时使用

在以下场景主动使用本技能：

- 用户希望“根据网页自动生成 DSL”
- 用户提供了 HTML、页面文件、页面 URL，希望快速产出 TV-Like 提取规则
- 用户希望为某个网站/页面类型初始化 TV-Like 规则
- 用户希望从现有页面结构反推 `section`、`items`、`text`、`img`、`link` 规则

## 必须遵守的项目 DSL 约束

只生成当前项目真正支持的 DSL 能力，不要虚构语法。

当前可用能力：

- `path: <AntPath> { ... }`
- `section:<name> <selector> { ... }`
- `items: <selector> { ... }`
- 字段类型仅限：
  - `text`
  - `img`
  - `link`
- 选项仅使用项目当前支持的形式：
  - `[attr: xxx]`
  - `[limit: N]`
  - `[transform: trim|upper|lower|digits|abs-url]`
  - 自定义透传元数据，例如 `[img-ratio: 2/3]`

不要输出当前项目未实现的 DSL 关键字，例如：

- `globals`
- `page-type`
- `exclude`
- 条件判断语法
- 自定义函数调用

如果用户要求这些能力，也先按当前项目可落地的 DSL 生成，并在结果后简短说明“该能力当前核心引擎未实现”。

## 生成目标

生成的 DSL 应优先覆盖电视端最重要的内容区块：

1. 顶部导航 / tabs
2. 页面主标题 / hero
3. 推荐内容列表 / 推荐卡片区
4. 演员、标签、专题等横向列表
5. 详情页中的剧集列表、相关推荐列表

生成规则时优先抽取：

- 文本标题
- 封面图
- 跳转链接

## 工作流程

### 第一步：识别页面类型

先判断页面更像哪一类：

- 首页
- 分类页
- 搜索页
- 详情页
- 播放页
- 专题页

再据此决定重点 section。

### 第二步：找稳定容器

优先选择结构稳定、语义清晰、层级适中的容器作为 section 根节点：

- 优先 class/id 稳定的块
- 避免选择易变的 hash class
- 避免依赖过长选择器
- 避免直接绑定 nth-child
- 如多个区块结构相同但语义不同，可生成多个 `section`

### 第三步：判断单块还是列表

- 如果区块只有一个核心信息块，直接在 `section` 内写字段
- 如果区块存在重复卡片，优先使用 `items`

示例思路：

- 单标题区：`section:hero .page-header { text: h1 }`
- 卡片列表区：`section:recommend .video-list { items: li.card { ... } }`

### 第四步：推断字段

字段映射规则：

- 标题、名称、文案：优先映射到 `text`
- 海报、缩略图：映射到 `img`
- 详情跳转地址：映射到 `link`

属性提取规则：

- 链接默认优先 `href`
- 图片默认优先 `src`
- 如果是懒加载图片，使用 `[attr: data-src]`、`[attr: data-original]` 等真实属性

### 第五步：补充选项

按实际情况决定是否补充：

- `[limit: N]`
  - 导航、相关推荐、横向滑动列表常常适合限制数量
- `[transform: abs-url]`
  - 当链接或图片是相对地址时推荐加上
- `[transform: trim]`
  - 文本前后空白明显时加上
- `[img-ratio: 2/3]` / `[img-ratio: 16/9]` / `[img-ratio: 4/3]`
  - 当区块明显是海报、横图、人物图时可以透传比例建议

## 命名规范

`section` 名称尽量简短直白，使用英文小写短词或中划线风格。

推荐优先使用这些名字：

- `tab`
- `hero`
- `top-title`
- `recommend`
- `episodes`
- `actor`
- `related`
- `rank`
- `category`
- `banner`

避免：

- 过长命名
- 与页面 class 完全一一绑定的技术性命名
- 模糊命名如 `section1`、`block2`

## 选择器策略

优先级从高到低：

1. `#id`
2. 稳定 class
3. 语义标签 + 稳定 class
4. 简短层级组合

避免：

- `body > div:nth-child(3) > ...`
- 明显是运行时生成的随机类名
- 对内容过拟合的文本型定位

## 输出要求

默认输出 3 部分，按这个顺序组织：

### 1. 页面判断

用 2~5 行简述：

- 页面类型判断
- 主要可提取区块
- 选择器策略

### 2. DSL

直接给出完整 DSL 代码块，确保格式可复制。

要求：

- 如果已知页面 URL，生成合适的 `path`
- 如果未知路径，优先使用 `path: /**`
- 只输出项目当前支持的 DSL 语法
- 保持尽量少但足够用的 section

### 3. 补充说明

只保留必要说明，例如：

- 哪些地方使用了 `abs-url`
- 哪些图片用了懒加载属性
- 哪些 section 还建议人工二次校对

## 生成时的决策原则

- 优先稳定性，而不是追求覆盖页面每一个元素
- 优先可维护性，而不是复杂 selector
- 优先主内容，而不是广告、角标、装饰节点
- 优先对电视端有价值的区块

## 如果页面信息不足

当 HTML 不完整、只有截图描述、或 DOM 太少时：

- 仍然生成“可作为起点”的 DSL 草稿
- 使用更保守的 selector
- 明确指出哪些部分需要用户提供更完整 HTML 再细化

## 输出示例

```txt
页面判断：
- 这是一个视频详情页
- 主要区块包括主标题、选集列表、相关推荐

DSL：
path: /detail/** {
    section:hero .video-detail {
        text: h1.title [transform: trim]
    }

    section:episodes .episode-list {
        items: li {
            text: a [transform: trim]
            link: a [attr: href] [transform: abs-url]
        } [limit: 40]
    }

    section:related .recommend-list {
        items: li.card {
            text: .title [transform: trim]
            link: a [attr: href] [transform: abs-url]
            img: img [attr: data-src] [transform: abs-url]
        } [img-ratio: 2/3]
    }
}

补充说明：
- `img` 使用 `data-src`
- 链接和图片都补了 `abs-url`
```

## 特别提醒

- 如果页面上有多个相似列表，只保留最有价值的 1~3 个
- 如果一个 section 同时包含主标题和推荐列表，应拆成多个 section
- 如果 `link` 和 `text` 指向同一个 `a`，这是正常情况
- 如果图片区块没有跳转链接，可以只保留 `img` 和 `text`
- 不要为了“看起来完整”而生成大量低质量 section

# TV-Like 中文说明

## 什么是 TV-Like

TV-Like 是一个把网页内容转换成电视端可消费结构的提取引擎。

它不负责重写网页，也不负责电视端 UI，而是通过一份清晰的 YAML DSL，告诉核心引擎：

- 哪些 URL 需要命中规则
- 页面里哪些区域是导航、主标题、推荐列表、剧集列表
- 每个区域该如何提取 `text`、`img`、`link`
- 哪些字段需要做 `trim`、`abs-url` 这类轻量变换

## 为什么改成 YAML DSL

旧版 DSL 在可读性和扩展性上有几个明显问题：

- 语法和 CSS selector 容易冲突
- `[]` 选项写法不利于继续扩展
- 块、字段、执行选项混在一行，阅读成本高
- 很难做校验、补全、格式化和自动生成

YAML DSL 的优势更直接：

- 层次清晰：`paths -> sections -> fields/items`
- selector 只是普通字符串，不再和 DSL 语法冲突
- 执行型配置显式化，如 `attr`、`limit`、`transforms`
- 元数据单独放在 `meta`，语义清楚
- 更适合后续做规则生成器、校验器和编辑器

## 当前核心能力

- 支持页面内嵌 YAML 规则
- 支持从 YAML Hub 拉取远程规则
- 支持更具体的 path 规则优先命中
- 支持 `section` 单区块抽取
- 支持 `items` 列表抽取
- 支持 `text`、`img`、`link` 三种字段
- 支持字段级 `attr`
- 支持区块级与列表级 `limit`
- 支持元数据透传，如 `img-ratio`、`badge`
- 支持 `trim`、`upper`、`lower`、`digits`、`abs-url`
- 支持通过 `RuleParser.validate()` 做 YAML DSL 结构校验

## 最小 YAML DSL 示例

```yaml
version: 1
sections:
  - name: hero
    selector: .hero-card
    meta:
      badge: featured
    fields:
      text:
        selector: .title
        transforms: [trim, upper]
      link:
        selector: .title a
        attr: href
        transforms: [abs-url]
      img:
        selector: img[data-src]
        attr: data-src
        transforms: [abs-url]
```

## 规则结构

- `version`：DSL 版本号，当前固定使用 `1`
- `paths`：按 URL path 拆分规则
- `match` / `matches`：单个或多个路径模式
- `sections`：区块列表
- `name`：输出里的区块名称
- `selector`：区块或字段的 CSS selector
- `fields`：区块字段定义
- `items`：列表项模板
- `meta`：透传到输出结构的元数据
- `limit`：限制 section 或 items 返回数量
- `transforms`：对字段值执行串联转换

## 快速开始

### 1. 引入核心模块

```xml
<dependency>
    <groupId>tv.tvai</groupId>
    <artifactId>tv-like-core</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### 2. 直接调用

```java
String html = "...网页源码...";
String url = "https://example.com/list";

List<Map<String, Object>> result = new TV(html, url).like();
```

### 3. 使用远程规则 Hub

```java
String html = "...网页源码...";
String url = "https://example.com/list";
String dslHub = "https://your-rule-host.example.com/";

List<Map<String, Object>> result = new TV(html, url, dslHub).like();
```

## 规则加载顺序

- 优先读取页面中的 `script[name="tv-like"]`
- 也支持 `script[id="tv-like"]` 与 `script[id="tv-like-rules"]`
- 页面未内嵌规则时，再请求远程 Hub
- Hub 会尝试 `${host}.yaml`、`${host}.yml` 以及域名倒序路径形式

## 输出结构

输出仍然保持为区块数组，方便现有调用方继续消费：

```json
[
  {
    "section": "hero",
    "badge": "featured",
    "text": { "value": "HELLO WORLD" },
    "link": { "value": "https://example.com/detail/1" },
    "img": { "value": "https://example.com/img/1.jpg" }
  }
]
```

## 文档导航

- 详细 YAML 教程：`TUTORIAL.zh.md`
- 英文概览：`README.en.md`
- 英文教程：`TUTORIAL.en.md`
- 官网简介：`index.html`

## 开发验证

在 `tv-like-core` 目录执行：

```bash
mvn test
```

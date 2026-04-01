![TV-Like Logo](images/1767871810.png)

# TV-Like

TV-Like 的目标很直接：把普通网页的核心内容抽出来，转换成更适合电视大屏浏览和遥控器操作的结构化数据。

它不是浏览器壳，也不是重写整站前端，而是通过一套足够轻量的 DSL 规则，从网页中提取标题、图片、链接和列表，输出统一 JSON，再交给电视端 UI 渲染层处理。

## 一句话理解

- 输入是网页 HTML 和页面 URL
- 规则来源可以是页面内嵌 DSL，也可以是远程 DSL Hub
- 解析后输出统一结构，方便电视端模板直接消费
- 核心模块是 `tv-like-core`，负责规则解析、路径匹配与内容抽取

## 仓库结构

- `tv-like-core`：核心解析引擎，负责 DSL 解析和 HTML 提取
- `tv-like-module`：示例与集成验证模块
- `tv-like-android-plugin`：Android/TV 侧接入示例
- `test/html`：一些网页样本
- `doc`：本次及后续改动记录

## 适合解决什么问题

- 老网站没有 TV 端页面，但 PC/H5 页面内容结构稳定
- 希望快速把网页站点接入电视端模板
- 希望把网页内容统一成固定 JSON，便于后续渲染、编排、缓存和推荐
- 希望不同网站的接入方式尽量一致，而不是每站写一套解析代码

## 当前核心能力

- 支持页面内嵌 DSL 提取
- 支持从 DSL Hub 按域名/路径规则回源获取 DSL
- 支持 Ant 风格路径匹配
- 支持 `section` 单区块提取
- 支持 `items` 列表提取
- 支持 `text`、`img`、`link` 三类字段
- 支持字段级 `attr`
- 支持区块级和列表级 `limit`
- 支持自定义元数据透传，例如 `img-ratio`
- 支持简单值变换 `transform`

## 本次核心优化

- 增强 DSL 解析稳健性，先统一清洗换行和块注释
- 支持一个 `path` 配置多个路径模式，例如 `/** || /detail/**`
- 修复 `items` 块解析后残留文本污染后续字段解析的问题
- `limit` 非法值不再抛异常，自动降级为不限量
- 提取器对空文档、空规则、非法选择器做了兜底
- 列表项为空时不再写入结果，减少脏数据
- 增加 `transform` 执行能力，当前支持：
  - `trim`
  - `upper`
  - `lower`
  - `digits`
  - `abs-url`
- `TV` 入口改为带 baseUri 解析 HTML，`abs-url` 可正确补全相对链接
- 网络访问改为更稳妥的连接关闭与空值处理，移除直接打印堆栈的粗暴行为
- 增加单元测试，覆盖多路径、列表限制、绝对地址补全、非法选择器容错等场景

## 快速开始

### 1. 引入核心模块

当前核心模块基于 Java 8 和 Maven。

```xml
<dependency>
    <groupId>tv.tvai</groupId>
    <artifactId>tv-like-core</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### 2. 最简单的调用方式

```java
String html = "...网页源码...";
String url = "https://example.com/list";

List<Map<String, Object>> result = new TV(html, url).like();
```

如果页面里已经内嵌了 DSL，这一步就能直接拿到结构化结果。

### 3. 自定义 DSL Hub

```java
String html = "...网页源码...";
String url = "https://example.com/list";
String dslHub = "https://your-dsl-hub.example.com/";

List<Map<String, Object>> result = new TV(html, url, dslHub).like();
```

## DSL 写法

### 最小示例

```html
<script type="text/plain" name="tv-like">
path: /list/** {
    section:hero .hero-card {
        text: .title
        link: .title a [attr: href]
        img: img [attr: data-src]
    }

    section:recommend .recommend-list {
        items: li.card {
            text: .title
            link: a [attr: href]
            img: img [attr: data-src]
        } [limit: 12] [img-ratio: 2/3]
    }
}
</script>
```

### DSL 结构说明

- `path: /xxx/** { ... }`
  - 指定规则生效的 URL 路径
  - 支持 Ant 风格通配符
  - 支持多路径写法：`/list/** || /detail/**`
- `section:<name> <selector> { ... }`
  - 定义一个区块
  - `<name>` 是输出里的区块名
  - `<selector>` 是区块根节点选择器
- `items: <selector> { ... }`
  - 定义区块内的列表项模板
- `text / img / link`
  - 当前支持的 3 种字段

## 选项说明

### 通用选项

- `[自定义key: 自定义value]`
  - 不参与执行，原样透传到输出结果
  - 适合传递 `img-ratio`、`layout`、`style` 等渲染元信息

### 字段级选项

- `[attr: href]`
  - 从属性取值，而不是读取文本
- `[transform: trim|abs-url]`
  - 对提取值做串联处理

### 区块级或列表级选项

- `[limit: 10]`
  - 限制 section 或 items 最多返回数量

## transform 说明

当前内置了几个简单但实用的变换器：

- `trim`：去掉首尾空白
- `upper`：转大写
- `lower`：转小写
- `digits`：只保留数字
- `abs-url`：把相对地址补全为绝对地址

示例：

```txt
text: .title [transform: trim|upper]
link: a [attr: href] [transform: abs-url]
img: img [attr: data-src] [transform: abs-url]
```

## 输出格式

输出是一个区块数组，每个区块至少包含 `section` 字段。

```json
[
  {
    "section": "hero",
    "text": { "value": "热门推荐" },
    "link": { "value": "https://example.com/list/top" }
  },
  {
    "section": "recommend",
    "items": {
      "img-ratio": "2/3",
      "value": [
        {
          "text": { "value": "影片 A" },
          "link": { "value": "https://example.com/detail/1" },
          "img": { "value": "https://example.com/poster/1.jpg" }
        }
      ]
    }
  }
]
```

说明：

- 普通字段会被包装成 `{ "value": xxx }`
- 自定义选项会和 `value` 同级输出
- 列表结果放在 `items.value` 中

## DSL 加载顺序

- 优先读取页面中的 `<script type="text/plain" name="tv-like">`
- 页面没有内嵌 DSL 时，再尝试请求 DSL Hub
- DSL Hub 会按 host 和域名倒序路径进行多次尝试

例如 `www.demo.example.com` 可能依次尝试：

- `www.demo.example.com.dsl`
- `demo.example.com.dsl`
- `com/example/demo/www.dsl`
- `com/example/demo.dsl`

## 健壮性设计

当前核心模块对下面这些情况做了兜底：

- HTML 为空
- URL 为空或不合法
- DSL 为空
- `limit` 写成非法值
- CSS Selector 非法
- 列表项命中但字段全部为空
- 远程 DSL 获取失败

这些情况默认都不会直接中断主流程，而是尽量返回空结果或部分有效结果。

## 开发与测试

在 `tv-like-core` 下执行：

```bash
mvn test
```

当前已覆盖的关键测试点：

- 多路径 `path` 匹配
- `transform` 串联执行
- `abs-url` 绝对地址补全
- `items limit` 截断
- 非法字段选择器容错

## 和电视端的关系

TV-Like 负责“提取”和“统一结构”。

电视端框架负责“展示”和“交互”。

这意味着：

- 网页站点接入成本更低
- 电视 UI 可以统一
- 同一个站点可以按不同模板渲染

如果你在做大屏内容聚合、影视站接入、分类导航页重构，这种模式会比直接适配网页 DOM 更稳定。

## 后续建议方向

- 增加更多字段类型，例如视频、标签、副标题
- 增加更多 `transform` 能力，例如 replace、prefix、suffix
- 增加规则调试工具和 DSL 校验工具
- 支持更细粒度的过滤、条件匹配和字段回退
- 补充更多真实站点样例和回归测试

## 相关文档

- 中文扩展文档：`README.zh.md`
- 英文说明：`README.en.md`
- 变更记录：`doc/`

## License

本项目采用 MIT License。



# TV-Like Extract Rules 规范文档

## 版本信息
- **版本**：1.3  
- **发布日期**：2025-10-16  
- **作者**：xAI Grok（基于用户需求定制）  
- **适用场景**：网页元素提取与电视端（TV-side）大屏展示适配  
- **许可证**：MIT License（自由使用、修改、传播）  

---

## 1. 介绍

### 1.1 什么是 TV-Like Extract Rules？
TV-Like Extract Rules（简称 **TV-Like**）是一个简单、易读的提取规范，用于从网页中提取主要元素（如图片、文字、视频、超链接），并输出统一的 JSON 结构，便于提取内容适配到电视端模板中重新渲染。  

**“TV-Like” 的含义**：  
- **TV**：代表“Television”（电视），强调针对大屏电视端优化——提取结果适合卡片式布局、遥控器导航和焦点高亮（如轮播列表、焦点视频卡片）。  
- **Like**：意为“类似于”或“风格”，表示提取逻辑“像电视节目一样直观、模块化、流畅”，避免网页复杂嵌套，转而优先输出易渲染的数组/对象（如限额列表用于 TV 横向滚动）。  

这个规范类似于网页的 `<meta name="keywords">` 或 `robots.txt`，开发者只需在 HTML `<head>` 中添加 `<script>` 标签（支持多行），或部署一个 `.tvlike` 文本文件，即可告诉电视端“如何提取页面元素”。  

**核心优势**：  
- **简单易读**：基于块式结构（键: 值 { 子块 }），用大括号 `{ }` 明确嵌套边界，避免纯缩进依赖，便于阅读和编辑。  
- **TV 优化**：内置布局提示（`layout`），焦点高亮由 Java 自动处理（基于超链接判断）。  
- **通用性**：适用于视频、新闻、电商、博客等任意网站，支持嵌套、循环、条件提取。  
- **输出统一**：JSON 对象/数组，使用固定键（如 `text`、`img`、`link`），自然表达嵌套结构，便于 Java 后台处理（使用 Jsoup 解析）。  

### 1.2 设计目标
- **不支持自定义 JSON 键**：输出使用固定标准键（如 `text`、`img`、`video`、`link`、`file`），开发者通过类型和块结构控制。  
- 支持元素类型：图片（`img`）、文字（`text`）、视频（`video`）、超链接（`link`）、文件（`file`）。  
- 支持嵌套：通过 `{ 子块 }` 明确控制层级（例如，链接包裹图片/文字）。  
- 支持循环/列表：用 `list: selector [options] { item-blocks }` 组装为数组模式（无需键名）。  
- 支持相对选择器（`>` 子元素）、祖先查找（`closest:`）、排除（`-` 或 `exclude`）。  
- 支持属性回退链（`src|data-src`）、后处理（`transform`）、限额/索引（`limit/index`）。  
- 选择器：CSS（默认）或 XPath（`xpath:` 前缀）。  
- TV 扩展：布局提示（`layout: grid|carousel`）；焦点由 Java 自动判断（仅检查元素是否有超链接）。  
- 保留 `globals`：用于全局配置，如 base-url 和排除。  

**适用流程**：  
1. 网站开发者添加 TV-Like 规则（script 或文件）。  
2. Java 程序从页面/文件读取规则，解析 HTML，提取元素。  
3. 输出 JSON，适配 TV 模板渲染（例如，列表 → 轮播卡片）。焦点逻辑：Java 检查元素是否包含/包裹 `<a>` 标签，即可上焦（无需规则中指定）。  

---

## 2. 集成方式

### 2.1 嵌入式方式（推荐，支持多行，类似于 keywords）
在网页 `<head>` 中添加 `<script type="text/plain" id="tv-like-rules">` 标签，直接编写多行规则内容（与 `tvlike.txt` 格式完全一致，无需转义）。这是一个非自闭合标签，支持任意多行纯文本。  

**示例**：  
```html
<head>
  <script type="text/plain" id="tv-like-rules">
        page-type: tv-video-list
        extends: base-tv-metadata
        
        globals {
          base-url: "https://video.com"
          exclude: ".ads"
        }
        
        content: .video-grid > .video-item [layout: carousel, limit: 10] {
          img: .thumb [attr: src|data-src, transform: url-join(base-url)]
          text: .title [transform: trim]
          link: closest: a
        }
  </script>
  <!-- 其他 head 内容 -->
</head>
```  
- **优势**：多行直接编写，大括号明确块边界；浏览器忽略执行，仅作为数据存储。  
- **Java 程序**：用 Jsoup 读取 `String rules = doc.getElementById("tv-like-rules").html();`（或 `.text()`，自动处理换行）。  

**注意**：如果浏览器兼容性是问题（极少见），可 fallback 到原 `<meta>` 自闭合方式，但新版优先推荐 `<script>` 以支持多行。

### 2.2 文件方式（类似于 robots.txt）
部署独立文件 `/tvlike.txt`（纯文本），Java 通过 HTTP GET 获取。  
- 示例内容：见下文语法（与 script 内容一模一样）。  
- 优势：支持继承（`extends`）和导入（`import`），适合多页面共享规则。  

### 2.3 Java 集成提示
- **解析器**：自定义简单递归解析（~200 行代码），将规则字符串拆分为树状结构（基于 `{ }` 块）。  
- **执行**：从 `<body>` 根容器开始，用 Jsoup 执行选择器。  
- **焦点处理**：在提取后，Java 自动检查每个元素（或其子元素）是否包含 `<a>` 标签（用 `element.select("a").size() > 0`），若有则标记为可焦点（输出 JSON 中添加 `"focusable": true`）。无需规则中指定 `tv-focus`。  
- **依赖**：Jsoup（HTML 解析），可选 Gson（JSON 输出）。  
- **伪代码示例**（更新以支持新块结构）：  
  ```java
  public class TVLikeExtractor {
      public JsonObject extract(String html) {
          Document doc = Jsoup.parse(html);
          // 优先从 script 读取规则
          Element script = doc.getElementById("tv-like-rules");
          String rulesContent = (script != null) ? script.html() : "";  // fallback 到文件
          // 解析 rulesContent 为 RuleTree（基于 { } 块）
          RuleTree tree = parseRulesWithBlocks(rulesContent);
          JsonObject result = tree.execute(doc);
          // 自动添加焦点标记
          addFocusable(result, doc);
          return result;
      }
      
      private void addFocusable(JsonObject obj, Document doc) {
          // 递归检查每个元素是否有 <a>，设置 "focusable": true
          // 示例：if (element.select("a").isEmpty() == false) { ... }
      }
  }
  ```

---

## 3. 语法规范

规则是一个纯文本块，使用块式结构（键: 值 [options] { 子块 }）来明确嵌套和层级，支持注释（`#` 开头）。这种设计比纯缩进更直观：大括号 `{ }` 清晰标记块边界，便于阅读复杂嵌套，而无需依赖空格对齐。  

### 3.1 基本结构
```
# 可选：页面类型（用于多页面适配，支持 URL 模式）
page-type: tv-generic | tv-article-detail | /video/.*

# 可选：继承基规则
extends: base-tv-template  # 从 /tvlike-base.txt 继承

# 全局设置（保留，用于配置）
globals {
  base-url: "https://example.com"  # 相对 URL 补全
  exclude: ".ads, .popup"  # 全局排除（CSS，逗号分隔）
  default-type: text  # 默认元素类型
  import: /common-tv-rules.txt  # 加载共享规则
}

# 标准通用字段（可选，自动添加，使用固定键）
metadata {
  text: title
  text: meta[name=description] [attr: content, transform: trim]
  text: meta[name=keywords] [attr: content]
}

# 自定义容器/元素（用 { } 控制层级）
content: .main-container [options] {
  text: .title [options]  # 固定键 text 表示文字提取
  img: .thumb [options]   # 固定键 img 表示图片
  sub: .sub-container [options] {
    link: closest: a [options]  # 嵌套层级
  }
  list: .item-list [options] {  # 循环开始，{ } 内定义 item 结构，组装为数组（list 模式，无需键）
    text: .name
    img: .photo
    link: a
  }
}
```
- **块结构**：`key: selector [options] { 子块 }` —— 键后跟选择器和选项，大括号内为子元素（固定键如 `text`、`img`）。根级用 `content:` 或类似固定根键开始。  
- **固定键**：仅支持标准类型键（`text`、`img`、`video`、`link`、`file`），无需自定义。  
- **selector**：在键后指定，CSS（`.class`、`#id`、`tag`）或 `xpath:/path`。备选用 `|` 分隔（`.title1 | .title2`）。  
- **[options]**：方括号内，逗号分隔。详见 3.2。  
- **相对选择器**：`>` 表示容器内子元素（如 `.container > .child`）。  
- **祖先查找**：`[closest: selector]`（如 `[closest: a]` 找最近链接）。  
- **嵌套**：`{ 子块 }` 表示子对象。  
- **循环列表**：用 `list: selector [options] { item-blocks }` 开始，`{ }` 内多行定义 item 结构，自动组装为数组（list 模式，无需键名）。多 item 块会重复提取为数组元素。  

### 3.2 选项详解
- **attr: chain**：属性回退，如 `attr: src|data-src|data-lazy`（按序取非空值）。  
- **transform: func**：后处理，如 `upper`（大写）、`lower`（小写）、`trim`（去空格）、`url-join(base-url)`（补全 URL）。链式：`[transform: trim,upper]`。  
- **limit: N**：提取前 N 个（数字）。  
- **index: N**：提取第 N 个（0-based）。如 `[index:0, limit:3]` 取前 3 跳过第 1。  
- **exclude: sel**：排除，如 `[exclude: .spam]` 或全局 `globals { exclude: ... }`。支持数组：`[.ads, .popup]`。  
- **if: sel**：条件，仅当元素存在时提取（如 `[if: .premium]`）。  
- **layout: type**：TV 布局提示，如 `layout: grid|carousel|stack`（输出 JSON 中添加 `"layout": "carousel"` 到容器）。  
- **alias: name sel**：定义别名，后用 `alias:name` 替换（全局或块内）。  

### 3.3 高级特性
- **继承 (extends)**：`extends: file-path` 或 `extends: page-type:base`，合并基规则（覆盖同级块）。  
- **导入 (import)**：`globals { import: /shared.txt }`，加载并合并。  
- **多规则覆盖**：同一块内多行，最后一行生效。  
- **条件块**：`if: selector { blocks... }` 只在匹配时执行。  

---

## 4. 输出格式
统一 JSON 对象：  
- 根级：`page-type`、`globals`（如果定义）、`metadata`（自动，使用固定键）。  
- 内容：固定键的对象/数组（`text: "..."`、`img: "..."`、`link: "..."`），嵌套用对象，列表用数组（list 模式）。  
- TV 扩展：容器可选 `"layout"`；每个元素自动添加 `"focusable": true/false`（由 Java 基于超链接判断）。  

**示例输出**（视频列表，组装为 list）：  
```json
{
  "page-type": "tv-video-list",
  "globals": { "base-url": "https://video.com" },
  "metadata": {
    "text": ["标题1", "标题2"]
  },
  "content": {
    "layout": "carousel",
    "list": [
      {
        "img": "https://site.com/thumb1.jpg",
        "text": "视频1",
        "link": "https://site.com/video/1",
        "focusable": true
      },
      ...
    ]
  }
}
```
- `content` 是固定根容器（可选，自定义为其他但固定）；列表直接为 `"list": [...]`（无需键）。  

---

## 5. 示例

### 5.1 视频网站 - TV 列表页（tv-video-list）
```
page-type: tv-video-list
extends: base-tv-metadata

globals {
  base-url: "https://video.com"
  exclude: ".ads"
}

content: .video-grid > .video-item [layout: carousel, limit: 10] {
  img: .thumb [attr: src|data-src, transform: url-join(base-url)]
  text: .title [transform: trim]
  link: closest: a
}
```
- **输出**：`content.list` 数组，每个 item 有固定键，焦点自动判断（有 link 即 true）。  
- **易读点**：`{ }` 清晰包裹 item 结构，无缩进歧义。  
- **嵌入式示例**：直接复制以上内容到 `<script id="tv-like-rules">` 内。

### 5.2 新闻网站 - TV 文章详情（tv-article-detail）
```
page-type: tv-article-detail

content: [layout: stack] {
  text: h1 [transform: upper]
  text: .article-body > p [limit: 300, transform: trim]
  img: .img-container [if: .caption]
}

related: [limit: 5] {
  list: .links-list li {
    text: a
    link: a [closest: .link-card]
  }
}
```
- **输出**：嵌套对象 + 数组，焦点基于 link。  
- **易读点**：每个块独立，用 `{ }` 隔离层级。

### 5.3 电商网站 - TV 商品列表（tv-product-list）
```
page-type: tv-product-list
import: /ecommerce-tv-shared.txt

products: .product-grid .card [layout: grid, index: 0, limit: 8] {
  text: .name
  text: .price [transform: trim]
  img: .photo [exclude: .out-of-stock]
  if: .expanded {
    text: .specs .color
  }
}
```
- **输出**：网格数组，条件嵌套为对象。  
- **易读点**：条件用 `if: { }`，无需额外缩进。

### 5.4 通用 TV 模板（base-tv-template.tvlike）
```
page-type: tv-generic

globals {
  default-type: text
  exclude: ".footer, .header"
}

metadata {
  text: title
}

main: body > .main [layout: stack] {
  text: .title
}
```
- **输出**：简单固定键结构。  
- **易读点**：全局块和主块用 `{ }` 自然分隔。

---

## 6. 最佳实践与故障排除
- **易读性**：用 `{ }` 明确块，避免长行；保持规则 < 50 行，从 `metadata` 开始渐进添加。  
- **多行支持**：在 `<script>` 中直接编写，块边界清晰。  
- **列表组装**：`list: { item... }` 自动转为数组，无需额外键。  
- **焦点简化**：Java 只需 `if (element.select("a").isEmpty() == false)` 即标记焦点。  
- **测试**：用 Jsoup 模拟提取，验证 JSON。  
- **常见问题**：  
  - 块不匹配：确保 `{` 和 `}` 配对。  
  - 选择器不匹配：用浏览器 DevTools 测试 CSS/XPath。  
  - 相对 URL：确保 `globals base-url`。  
  - TV 渲染：JSON 中的 `layout` 和 `focusable` 供前端模板使用。  
- **扩展**：未来可加 `priority: high`（提取优先级）。  

## 7. 联系与贡献
- **反馈**：通过 xAI Grok 反馈。  
- **贡献**：Fork 本文档，提交 PR。  
- **相关资源**：Jsoup 文档、CSS 选择器指南。  

---

**文档结束**。这个版本用块式 `{ }` 结构替换纯缩进，更直观易读（类似于简化JSON/YAML混合），边界明确，便于开发者复制/编辑。如果需要更多示例或进一步调整（如添加分号分隔），请提供反馈！